package com.korkoor.pardos.data.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.korkoor.pardos.data.local.EconomyManager
import com.korkoor.pardos.data.local.RetentionManager
import com.korkoor.pardos.domain.shop.ProductKind
import com.korkoor.pardos.domain.shop.ShopCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Compras dentro de la app con Google Play Billing.
 *
 * Los productos (ver [ShopCatalog]) deben existir en Play Console como "Productos administrados".
 * Mientras no existan, [prices] queda vacío y la tienda muestra "No disponible" en vez de fallar.
 *
 * Reglas de entrega:
 *  - Gemas (consumibles): se entregan SOLO cuando el consumo en Play termina bien, así no se
 *    pueden duplicar ni perder si la app se cierra a media compra (se recuperan al reconectar).
 *  - VIP (no consumible): se reconoce (acknowledge) y se activa; es idempotente.
 */
class BillingManager(context: Context) : PurchasesUpdatedListener {

    private val appContext = context.applicationContext
    private val economy = EconomyManager(appContext)
    private val retention = RetentionManager(appContext)

    private val _prices = MutableStateFlow<Map<String, String>>(emptyMap())
    /** productId -> precio con formato local (ej. "$0.99"). */
    val prices: StateFlow<Map<String, String>> = _prices.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    /** Último resultado para mostrar al jugador (compra lista, cancelada, error). */
    val message: StateFlow<String?> = _message.asStateFlow()

    private val details = mutableMapOf<String, ProductDetails>()

    private val client: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    fun connect() {
        if (client.isReady) return
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    loadProducts()
                    restorePurchases()
                } else {
                    Log.w(TAG, "Billing setup failed: ${result.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing disconnected")
            }
        })
    }

    fun disconnect() {
        if (client.isReady) client.endConnection()
    }

    fun clearMessage() { _message.value = null }

    private fun loadProducts() {
        val products = ShopCatalog.products.map {
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(it.id)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder().setProductList(products).build()
        client.queryProductDetailsAsync(params) { result, queryResult ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.w(TAG, "queryProductDetails failed: ${result.debugMessage}")
                return@queryProductDetailsAsync
            }
            val list = queryResult.productDetailsList
            details.clear()
            list.forEach { details[it.productId] = it }
            _prices.value = list.mapNotNull { pd ->
                pd.oneTimePurchaseOfferDetails?.formattedPrice?.let { pd.productId to it }
            }.toMap()
        }
    }

    /** Recupera compras que quedaron pendientes de consumir/reconocer (cierre a media compra, reinstalación). */
    private fun restorePurchases() {
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        client.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases.forEach { handlePurchase(it, silent = true) }
            }
        }
    }

    fun purchase(activity: Activity, productId: String) {
        val pd = details[productId]
        if (pd == null) {
            _message.value = "Producto no disponible todavía"
            return
        }
        val flow = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(pd).build())
            )
            .build()
        val result = client.launchBillingFlow(activity, flow)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _message.value = "No se pudo iniciar la compra"
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> purchases?.forEach { handlePurchase(it, silent = false) }
            BillingClient.BillingResponseCode.USER_CANCELED -> _message.value = "Compra cancelada"
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> restorePurchases()
            else -> {
                Log.w(TAG, "Purchase error ${result.responseCode}: ${result.debugMessage}")
                _message.value = "No se pudo completar la compra"
            }
        }
    }

    private fun handlePurchase(purchase: Purchase, silent: Boolean) {
        if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            if (!silent) _message.value = "Pago pendiente; se entregará al confirmarse"
            return
        }
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (!silent) com.korkoor.pardos.audio.GameAudio.play(com.korkoor.pardos.audio.Sfx.PURCHASE)

        purchase.products.forEach { productId ->
            val product = ShopCatalog.byId(productId) ?: return@forEach
            when (product.kind) {
                ProductKind.GEMS -> {
                    val params = ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
                    client.consumeAsync(params) { consumeResult, _ ->
                        if (consumeResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            val first = economy.isFirstPurchase(product.id)
                            val gems = com.korkoor.pardos.domain.shop.GemPacks.gemsForPurchase(product, first)
                            economy.markPurchased(product.id)
                            economy.addGems(gems)
                            _message.value = if (first) "+$gems gemas (¡primera compra x2!)" else "+$gems gemas"
                        }
                    }
                }
                ProductKind.STARTER -> {
                    // Compra única (no consumible): se entrega una sola vez por cuenta de Play
                    if (!economy.isStarterClaimed()) {
                        economy.claimStarterPack()
                        if (!silent) _message.value = "¡Pack inicial entregado!"
                    }
                    if (!purchase.isAcknowledged) {
                        val params = AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken).build()
                        client.acknowledgePurchase(params) { }
                    }
                }
                ProductKind.STUDIO -> {
                    economy.unlockStudio()
                    if (!purchase.isAcknowledged) {
                        val params = AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken).build()
                        client.acknowledgePurchase(params) { }
                    }
                    if (!silent) _message.value = "¡Skin Studio desbloqueada! Diséñala a tu gusto"
                }
                ProductKind.SEASON_PASS -> consumeThen(purchase) {
                    retention.unlockPremium()
                    _message.value = "¡Pase premium activado!"
                }
                ProductKind.PIGGY -> consumeThen(purchase) {
                    val gems = retention.breakPiggy()
                    _message.value = if (gems > 0) "¡Hucha rota! +$gems gemas" else "La hucha estaba vacía"
                }
                ProductKind.VIP -> {
                    economy.setVip(true)
                    if (!purchase.isAcknowledged) {
                        val params = AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken).build()
                        client.acknowledgePurchase(params) { }
                    }
                    if (!silent) _message.value = "¡VIP activado!"
                }
            }
        }
    }

    /** Consume la compra y SOLO si Play confirma entrega el premio (no se duplica ni se pierde). */
    private fun consumeThen(purchase: Purchase, deliver: () -> Unit) {
        val params = ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        client.consumeAsync(params) { consumeResult, _ ->
            if (consumeResult.responseCode == BillingClient.BillingResponseCode.OK) deliver()
        }
    }

    private companion object {
        const val TAG = "BillingManager"
    }
}
