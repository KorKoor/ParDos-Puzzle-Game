package com.korkoor.pardos.domain.shop

enum class ProductKind { GEMS, VIP, STARTER, STUDIO, SEASON_PASS, PIGGY, BUNDLE }

/** Producto de la tienda (compra real). Los IDs deben crearse igual en Play Console / App Store Connect. */
data class StoreProduct(
    val id: String,
    val kind: ProductKind,
    val gems: Int = 0,
    /** Precio de referencia en centavos de dólar (solo para calcular el "% extra" que se enseña; el cobro real lo fija Play). */
    val usdCents: Int = 0,
    /** Solo para BUNDLE: cofres raros que trae el pack. */
    val rareChests: Int = 0
)

object ShopCatalog {
    const val GEMS_TINY = "gems_tiny"
    const val GEMS_SMALL = "gems_small"
    const val GEMS_MEDIUM = "gems_medium"
    const val GEMS_LARGE = "gems_large"
    const val GEMS_HUGE = "gems_huge"
    const val VIP_FOREVER = "vip_forever"
    const val STARTER_PACK = "starter_pack"
    /** Desbloquea el editor de la skin Studio (compra única). */
    const val SKIN_STUDIO = StudioSkin.PRODUCT_ID
    /** Vía premium del pase de la temporada en curso (consumible: se compra una vez por temporada). */
    const val SEASON_PASS = "season_pass"
    /** Rompe la hucha y entrega las gemas guardadas (consumible). */
    const val PIGGY_BREAK = "piggy_break"
    /** Packs de gemas + cofres (consumibles: se pueden volver a comprar). */
    const val HALLOWEEN_PACK = "halloween_pack"
    const val BOSS_PACK = "boss_pack"
    const val TREASURE_PACK = "treasure_pack"

    val products: List<StoreProduct> = listOf(
        StoreProduct(GEMS_TINY, ProductKind.GEMS, gems = 45, usdCents = 99),
        StoreProduct(GEMS_SMALL, ProductKind.GEMS, gems = 100, usdCents = 199),
        StoreProduct(GEMS_MEDIUM, ProductKind.GEMS, gems = 550, usdCents = 999),
        StoreProduct(GEMS_LARGE, ProductKind.GEMS, gems = 1200, usdCents = 1999),
        StoreProduct(GEMS_HUGE, ProductKind.GEMS, gems = 3500, usdCents = 4999),
        StoreProduct(VIP_FOREVER, ProductKind.VIP),
        StoreProduct(STARTER_PACK, ProductKind.STARTER),
        StoreProduct(SKIN_STUDIO, ProductKind.STUDIO),
        StoreProduct(SEASON_PASS, ProductKind.SEASON_PASS),
        StoreProduct(PIGGY_BREAK, ProductKind.PIGGY),
        StoreProduct(HALLOWEEN_PACK, ProductKind.BUNDLE, gems = 800, usdCents = 499, rareChests = 4),
        StoreProduct(BOSS_PACK, ProductKind.BUNDLE, gems = 200, usdCents = 199, rareChests = 1),
        StoreProduct(TREASURE_PACK, ProductKind.BUNDLE, gems = 1000, usdCents = 999, rareChests = 6)
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
