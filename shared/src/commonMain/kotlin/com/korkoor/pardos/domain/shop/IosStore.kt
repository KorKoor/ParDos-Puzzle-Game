package com.korkoor.pardos.domain.shop

import kotlin.math.roundToInt

/**
 * Tienda de pago de iPhone (App Store). Los precios son **escalones de precio de Apple** (solo existen ciertos importes en
 * dólares: 0,99 · 1,99 · 2,99 · 4,99 · 6,99 · 9,99 · 19,99 · 49,99 · 99,99...), y la escalera de gemas por dólar sube
 * con cada pack para que el pack grande siempre compense, igual que en Android pero con los dos escalones que a Apple le
 * faltaban a la escalera de Play: **$4,99** (el precio de impulso más usado en iPhone) y **$99,99** (la "bóveda").
 *
 * En iPhone no hay anuncios con premio, así que lo que en Android se hace viendo un anuncio (poderes, giros extra, duplicar
 * monedas) aquí se hace con monedas o gemas, y la ruleta y los cofres dan un poco más de lo esencial: ver `Economy`.
 *
 * Los IDs de los cinco primeros son los mismos que en Google Play para que el perfil guardado en la nube siga valiendo.
 */
object IosStore {
    /** Importes que Apple permite (en centavos de dólar). Si algún día se cambia un precio, tiene que ser uno de estos. */
    val applePricePoints: List<Int> = listOf(
        99, 199, 299, 399, 499, 599, 699, 799, 899, 999, 1099, 1199, 1299, 1399, 1499, 1999, 2499, 2999, 3999, 4999, 5999, 6999, 7999, 8999, 9999
    )

    const val GEMS_POCKET = "gems_pocket"
    const val GEMS_VAULT = "gems_vault"

    /** Packs de gemas (consumibles). De menos a más caro. */
    val gemPacks: List<StoreProduct> = listOf(
        StoreProduct(ShopCatalog.GEMS_TINY, ProductKind.GEMS, gems = 45, usdCents = 99),
        StoreProduct(ShopCatalog.GEMS_SMALL, ProductKind.GEMS, gems = 100, usdCents = 199),
        StoreProduct(GEMS_POCKET, ProductKind.GEMS, gems = 270, usdCents = 499),
        StoreProduct(ShopCatalog.GEMS_MEDIUM, ProductKind.GEMS, gems = 580, usdCents = 999),
        StoreProduct(ShopCatalog.GEMS_LARGE, ProductKind.GEMS, gems = 1_250, usdCents = 1999),
        StoreProduct(ShopCatalog.GEMS_HUGE, ProductKind.GEMS, gems = 3_600, usdCents = 4999),
        StoreProduct(GEMS_VAULT, ProductKind.GEMS, gems = 8_000, usdCents = 9999)
    )

    /** Nombre corto de cada pack para la tienda. */
    fun packName(id: String): String = when (id) {
        ShopCatalog.GEMS_TINY -> "Chispa"
        ShopCatalog.GEMS_SMALL -> "Puñado"
        GEMS_POCKET -> "Bolsita"
        ShopCatalog.GEMS_MEDIUM -> "Bolsa"
        ShopCatalog.GEMS_LARGE -> "Baúl"
        ShopCatalog.GEMS_HUGE -> "Tesoro"
        GEMS_VAULT -> "Bóveda"
        else -> "Gemas"
    }

    /** El pack que se destaca como "mejor valor" (el escalón de $9,99: el que más se compra en iPhone). */
    const val BEST_VALUE_ID = ShopCatalog.GEMS_MEDIUM

    private fun gemsPerDollar(p: StoreProduct) = p.gems * 100.0 / p.usdCents

    /** % de gemas extra frente al pack más barato. */
    fun bonusPercent(p: StoreProduct): Int {
        val base = gemPacks.first()
        return ((gemsPerDollar(p) / gemsPerDollar(base) - 1.0) * 100).roundToInt().coerceAtLeast(0)
    }

    /** Ofertas especiales de una sola vez o por temporada. Precios = escalones de Apple. */
    data class Special(val id: String, val name: String, val blurb: String, val usdCents: Int, val repeatable: Boolean)

    val specials: List<Special> = listOf(
        Special(ShopCatalog.STARTER_PACK, "Pack inicial", "300 gemas, 3 cofres raros, skin Cerezo y efecto Corazones. Una sola vez.", 299, false),
        Special(ShopCatalog.SEASON_PASS, "Pase premium", "La vía premium de la temporada: más premios y la skin exclusiva del mes.", 499, true),
        Special(ShopCatalog.SKIN_STUDIO, "Studio", "Diseña tus propias fichas: acabado, colores, fondo y partículas. Una sola vez.", 399, false),
        Special(ShopCatalog.VIP_FOREVER, "VIP para siempre", "+20% de monedas, 5 gemas cada día y poderes sin esperas de pago. Una sola vez.", 699, false),
        Special(ShopCatalog.PIGGY_BREAK, "Romper la hucha", "Recibe de golpe todas las gemas que guardó tu hucha.", 299, true)
    )

    fun special(id: String): Special? = specials.firstOrNull { it.id == id }

    fun gemPack(id: String): StoreProduct? = gemPacks.firstOrNull { it.id == id }

    /** Texto de precio en dólares ("$4.99"), sin depender de formatos del teléfono. */
    fun priceText(usdCents: Int): String {
        val dollars = usdCents / 100
        val cents = usdCents % 100
        return "$" + dollars + "." + cents.toString().padStart(2, '0')
    }
}
