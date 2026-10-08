package com.korkoor.pardos.domain.shop

enum class ProductKind { GEMS, VIP, STARTER }

/** Producto de la tienda (compra real). Los IDs deben crearse igual en Play Console / App Store Connect. */
data class StoreProduct(
    val id: String,
    val kind: ProductKind,
    val gems: Int = 0
)

object ShopCatalog {
    const val GEMS_SMALL = "gems_small"
    const val GEMS_MEDIUM = "gems_medium"
    const val GEMS_LARGE = "gems_large"
    const val VIP_FOREVER = "vip_forever"
    const val STARTER_PACK = "starter_pack"

    val products: List<StoreProduct> = listOf(
        StoreProduct(GEMS_SMALL, ProductKind.GEMS, gems = 100),
        StoreProduct(GEMS_MEDIUM, ProductKind.GEMS, gems = 550),
        StoreProduct(GEMS_LARGE, ProductKind.GEMS, gems = 1200),
        StoreProduct(VIP_FOREVER, ProductKind.VIP),
        StoreProduct(STARTER_PACK, ProductKind.STARTER)
    )

    fun byId(id: String): StoreProduct? = products.firstOrNull { it.id == id }
}

/** Precios y límites de lo que se compra con monedas/gemas dentro del juego. */
object CoinShop {
    const val STREAK_FREEZE_PRICE_COINS = com.korkoor.pardos.domain.economy.Economy.STREAK_FREEZE_PRICE_COINS
    const val MAX_STREAK_FREEZES = com.korkoor.pardos.domain.economy.Economy.MAX_STREAK_FREEZES

    /** Cuántas monedas da una gema al cambiarla. */
    const val COINS_PER_GEM = 40

    fun coinsForGems(gems: Int): Int = gems.coerceAtLeast(0) * COINS_PER_GEM

    fun canBuyStreakFreeze(coins: Int, owned: Int): Boolean =
        owned < MAX_STREAK_FREEZES && coins >= STREAK_FREEZE_PRICE_COINS
}
