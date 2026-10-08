package com.korkoor.pardos.domain.shop

import com.korkoor.pardos.domain.economy.Economy

enum class BannerSource { FREE, SHOP, SEASON }

/** Dibujo que lleva el banner encima del degradado; la app lo pinta con Canvas. */
enum class BannerPattern { CHECKER, DOTS, HILLS, PETALS, LEAVES, WAVES, BUBBLES, SNOW, DIAMONDS, RAYS, STARS, AURORA, SKYLINE, EMBERS }

/**
 * Banner de perfil: la imagen de fondo de tu tarjeta de jugador (perfil, menú, amigos).
 * Todo son datos: añadir uno = añadir una entrada.
 */
data class BannerDef(
    val id: Int,
    val name: String,
    val source: BannerSource,
    val pattern: BannerPattern,
    val top: Long,
    val bottom: Long,
    /** Color del dibujo (colinas, olas, estrellas...). */
    val accent: Long,
    /** Color del texto que va encima. */
    val ink: Long,
    val coinPrice: Int = 0,
    val gemPrice: Int = 0
) {
    val isDark: Boolean
        get() {
            fun l(c: Long): Double = (((c shr 16) and 0xFF) * 0.299 + ((c shr 8) and 0xFF) * 0.587 + (c and 0xFF) * 0.114) / 255.0
            return (l(top) + l(bottom)) / 2 < 0.4
        }
}

object Banners {
    const val DEFAULT_ID = 1

    private fun free(id: Int, name: String, p: BannerPattern, top: Long, bottom: Long, accent: Long, ink: Long) =
        BannerDef(id, name, BannerSource.FREE, p, top, bottom, accent, ink)

    private fun shop(id: Int, name: String, p: BannerPattern, top: Long, bottom: Long, accent: Long, ink: Long, coins: Int = 0, gems: Int = 0) =
        BannerDef(id, name, BannerSource.SHOP, p, top, bottom, accent, ink, coins, gems)

    private fun season(id: Int, name: String, p: BannerPattern, top: Long, bottom: Long, accent: Long, ink: Long) =
        BannerDef(id, name, BannerSource.SEASON, p, top, bottom, accent, ink)

    val free: List<BannerDef> = listOf(
        free(1, "Crema", BannerPattern.CHECKER, 0xFFF6F1E6, 0xFFEAE3D2, 0xFFD9CFB8, 0xFF3D405B),
        free(2, "Menta", BannerPattern.DOTS, 0xFFD9F2E6, 0xFFA8DDC4, 0xFF6B9E86, 0xFF2E5D4A)
    )

    /** De barato a caro. */
    val shop: List<BannerDef> = listOf(
        shop(3, "Colinas", BannerPattern.HILLS, 0xFFCFE9D6, 0xFFE8F5DC, 0xFF7FB69C, 0xFF2E5D3A, coins = Economy.BANNER_PRICE_BASIC),
        shop(4, "Cerezo", BannerPattern.PETALS, 0xFFFFE3EA, 0xFFFBC4D4, 0xFFE78FA8, 0xFF7A3D55, coins = Economy.BANNER_PRICE_COZY),
        shop(5, "Hojas de otoño", BannerPattern.LEAVES, 0xFFFFE9C9, 0xFFF4B773, 0xFFD97B3A, 0xFF6B3A1A, coins = Economy.BANNER_PRICE_COZY),
        shop(6, "Mar tranquilo", BannerPattern.WAVES, 0xFFCDEBFA, 0xFF8EC9E8, 0xFF3F8FB8, 0xFF174A6B, coins = Economy.BANNER_PRICE_NICE),
        shop(7, "Burbujas", BannerPattern.BUBBLES, 0xFFE0F7F4, 0xFF9BE0D6, 0xFF3FB5A5, 0xFF1B5E57, coins = Economy.BANNER_PRICE_NICE),
        shop(8, "Nevada", BannerPattern.SNOW, 0xFFEAF4FF, 0xFFC3DCF5, 0xFF7FB0E0, 0xFF2B4B6F, coins = Economy.BANNER_PRICE_RARE),
        shop(9, "Dulces", BannerPattern.DIAMONDS, 0xFFFFE0F1, 0xFFD9F5EA, 0xFFFF8CC0, 0xFF8A3E6E, coins = Economy.BANNER_PRICE_RARE),
        shop(10, "Dunas", BannerPattern.HILLS, 0xFFFFE7B8, 0xFFF1C27A, 0xFFD99C55, 0xFF6B3F18, coins = Economy.BANNER_PRICE_PRIME),
        shop(11, "Soleado", BannerPattern.RAYS, 0xFFFFF3C4, 0xFFFFD36E, 0xFFF2A93B, 0xFF6B4A0E, coins = Economy.BANNER_PRICE_PRIME),
        shop(12, "Noche estrellada", BannerPattern.STARS, 0xFF121B3A, 0xFF2D2A6E, 0xFFFFE08A, 0xFFF1F0FF, gems = Economy.BANNER_GEMS_NICE),
        shop(15, "Volcán", BannerPattern.EMBERS, 0xFF2A0F0C, 0xFF6B1F14, 0xFFFF7B3A, 0xFFFFE1C6, gems = Economy.BANNER_GEMS_RARE),
        shop(13, "Aurora", BannerPattern.AURORA, 0xFF081B2E, 0xFF14465A, 0xFF78E6C6, 0xFFD9FFF2, gems = Economy.BANNER_GEMS_EPIC),
        shop(14, "Ciudad neón", BannerPattern.SKYLINE, 0xFF1B1235, 0xFF4A1F6B, 0xFFFF5FA8, 0xFFFFE6F5, gems = Economy.BANNER_GEMS_EPIC)
    )

    /** Exclusivos del Pase, por pares: [par*2] en la vía gratis y [par*2+1] en la premium. */
    val seasonal: List<BannerDef> = listOf(
        season(16, "Brisa de primavera", BannerPattern.PETALS, 0xFFE8F8E4, 0xFFFFD9E6, 0xFFFF9EC4, 0xFF47603F),
        season(17, "Eclipse dorado", BannerPattern.STARS, 0xFF08050F, 0xFF2A0F3D, 0xFFFFD36E, 0xFFF2E9FF),
        season(18, "Verano en la playa", BannerPattern.WAVES, 0xFFFFF0C9, 0xFF7FD3E8, 0xFF2E9BC8, 0xFF174A6B),
        season(19, "Atardecer real", BannerPattern.RAYS, 0xFFFFC27A, 0xFFE0558F, 0xFFFFE08A, 0xFFFFF5E6),
        season(20, "Bosque de luciérnagas", BannerPattern.BUBBLES, 0xFF0E2923, 0xFF184238, 0xFFD6F26B, 0xFFE3F7E8),
        season(21, "Cristal de hielo", BannerPattern.SNOW, 0xFFF4FBFF, 0xFFB5D8F2, 0xFFFFFFFF, 0xFF23405E)
    )

    val all: List<BannerDef> = free + shop.sortedBy { it.id } + seasonal

    fun byId(id: Int): BannerDef = all.firstOrNull { it.id == id } ?: all.first()
    fun exists(id: Int): Boolean = all.any { it.id == id }

    /** Los gratis son de todos; el resto hay que tenerlos. */
    fun isOwned(id: Int, owned: Set<Int>): Boolean = free.any { it.id == id } || id in owned

    fun seasonFree(seasonId: Int): BannerDef = seasonal[(seasonId.mod(seasonal.size / 2)) * 2]
    fun seasonPremium(seasonId: Int): BannerDef = seasonal[(seasonId.mod(seasonal.size / 2)) * 2 + 1]

    sealed interface Purchase {
        data class Ok(val coinsLeft: Int, val gemsLeft: Int) : Purchase
        data object AlreadyOwned : Purchase
        data object NotEnoughCoins : Purchase
        data object NotEnoughGems : Purchase
        data object NotPurchasable : Purchase
    }

    fun buy(id: Int, owned: Set<Int>, coins: Int, gems: Int): Purchase {
        val def = all.firstOrNull { it.id == id } ?: return Purchase.NotPurchasable
        if (isOwned(id, owned)) return Purchase.AlreadyOwned
        if (def.source != BannerSource.SHOP) return Purchase.NotPurchasable
        if (coins < def.coinPrice) return Purchase.NotEnoughCoins
        if (gems < def.gemPrice) return Purchase.NotEnoughGems
        return Purchase.Ok(coins - def.coinPrice, gems - def.gemPrice)
    }
}
