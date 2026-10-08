package com.korkoor.pardos.domain.shop

import com.korkoor.pardos.domain.economy.Economy

/** PRESTIGE = se desbloquea con un rango de prestigio (o el Platino). */
enum class BannerSource { FREE, SHOP, SEASON, PRESTIGE }

/** Dibujo que lleva el banner encima del degradado; la app lo pinta con Canvas. */
enum class BannerPattern { CHECKER, DOTS, HILLS, PETALS, LEAVES, WAVES, BUBBLES, SNOW, DIAMONDS, RAYS, STARS, AURORA, SKYLINE, EMBERS,
    BATS, PUMPKINS, MOUNTAINS, FOREST, CLOUDS, HEARTS, FIREWORKS, GALAXY, ZEN, STRIPES, CIRCUIT, LANTERNS, CONFETTI, RAIN, SUNSET, CRYSTALS }

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
    val gemPrice: Int = 0,
    /** Solo para PRESTIGE: el rango que lo desbloquea (null = el Platino). */
    val unlockRank: com.korkoor.pardos.domain.prestige.PrestigeRank? = null
) {
    /** Rareza para mostrar: sale de cómo se consigue y de su precio. */
    val rarity: com.korkoor.pardos.domain.collection.Rarity
        get() = when {
            source == BannerSource.PRESTIGE -> com.korkoor.pardos.domain.collection.Rarity.LEGENDARY
            source == BannerSource.SEASON -> com.korkoor.pardos.domain.collection.Rarity.EPIC
            gemPrice >= 100 -> com.korkoor.pardos.domain.collection.Rarity.LEGENDARY
            gemPrice >= 85 -> com.korkoor.pardos.domain.collection.Rarity.EPIC
            gemPrice > 0 || coinPrice >= 1_100 -> com.korkoor.pardos.domain.collection.Rarity.RARE
            else -> com.korkoor.pardos.domain.collection.Rarity.COMMON
        }

    /** De la colección de Noche de brujas. */
    val isHalloween: Boolean
        get() = pattern == BannerPattern.BATS || pattern == BannerPattern.PUMPKINS

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

    private fun prestige(id: Int, name: String, rank: com.korkoor.pardos.domain.prestige.PrestigeRank?, p: BannerPattern, top: Long, bottom: Long, accent: Long, ink: Long) =
        BannerDef(id, name, BannerSource.PRESTIGE, p, top, bottom, accent, ink, unlockRank = rank)

    val free: List<BannerDef> = listOf(
        free(1, "Crema", BannerPattern.CHECKER, 0xFFF6F1E6, 0xFFEAE3D2, 0xFFD9CFB8, 0xFF3D405B),
        free(2, "Menta", BannerPattern.DOTS, 0xFFD9F2E6, 0xFFA8DDC4, 0xFF6B9E86, 0xFF2E5D4A)
    )

    /** De barato a caro (primero monedas, luego gemas). */
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
        shop(14, "Ciudad neón", BannerPattern.SKYLINE, 0xFF1B1235, 0xFF4A1F6B, 0xFFFF5FA8, 0xFFFFE6F5, gems = Economy.BANNER_GEMS_EPIC),
        // ---- monedas
        shop(30, "Cielo despejado", BannerPattern.CLOUDS, 0xFFBFE4FF, 0xFFE9F6FF, 0xFFFFFFFF, 0xFF1F4A6E, coins = Economy.BANNER_PRICE_BASIC),
        shop(31, "Montañas", BannerPattern.MOUNTAINS, 0xFFCFE0F5, 0xFFF1E4D3, 0xFF6B83A8, 0xFF22324F, coins = Economy.BANNER_PRICE_COZY),
        shop(32, "Bosque de pinos", BannerPattern.FOREST, 0xFFBFE6C9, 0xFF2F7A4A, 0xFF3E8A5A, 0xFFF0FFF3, coins = Economy.BANNER_PRICE_COZY),
        shop(33, "Día de lluvia", BannerPattern.RAIN, 0xFFD5DEE9, 0xFFAEBCCE, 0xFF5D7799, 0xFF26364E, coins = Economy.BANNER_PRICE_NICE),
        shop(34, "Corazones", BannerPattern.HEARTS, 0xFFFFE1EA, 0xFFFFC4D6, 0xFFFF6F9B, 0xFF7A2E4B, coins = Economy.BANNER_PRICE_NICE),
        shop(35, "Fiesta de confeti", BannerPattern.CONFETTI, 0xFFFFF4D6, 0xFFFFE1F0, 0xFFFF8CC0, 0xFF6B3A55, coins = Economy.BANNER_PRICE_RARE),
        shop(36, "Jardín zen", BannerPattern.ZEN, 0xFFF3ECDD, 0xFFE2D5BC, 0xFFB7A47E, 0xFF4A3F27, coins = Economy.BANNER_PRICE_RARE),
        shop(37, "Caramelo", BannerPattern.STRIPES, 0xFFFFE9F2, 0xFFFFC2DA, 0xFFFF7FB2, 0xFF7A2E55, coins = Economy.BANNER_PRICE_RARE),
        shop(38, "Farolillos", BannerPattern.LANTERNS, 0xFF2A1B3D, 0xFF5B2A4E, 0xFFFFB44A, 0xFFFFEBD0, coins = Economy.BANNER_PRICE_PRIME),
        shop(39, "Atardecer en el mar", BannerPattern.SUNSET, 0xFFF2985A, 0xFFB83A7A, 0xFFFFE08A, 0xFFFFF5E6, coins = Economy.BANNER_PRICE_PRIME),
        // ---- gemas
        shop(40, "Noche de brujas", BannerPattern.BATS, 0xFF1B0F2E, 0xFF4A1F6B, 0xFFFF9A2E, 0xFFFFEBD0, gems = Economy.BANNER_GEMS_NICE),
        shop(41, "Campo de calabazas", BannerPattern.PUMPKINS, 0xFF2B1A3D, 0xFF7A2D4F, 0xFFFF8A1F, 0xFFFFEBD0, gems = Economy.BANNER_GEMS_NICE),
        shop(42, "Fuegos artificiales", BannerPattern.FIREWORKS, 0xFF0B1030, 0xFF1F2A5E, 0xFFFFD36E, 0xFFF1F0FF, gems = Economy.BANNER_GEMS_RARE),
        shop(43, "Circuito", BannerPattern.CIRCUIT, 0xFF081F2B, 0xFF0E4A55, 0xFF5CF2D0, 0xFFD9FFF5, gems = Economy.BANNER_GEMS_RARE),
        shop(44, "Galaxia", BannerPattern.GALAXY, 0xFF0A0720, 0xFF3A1670, 0xFFC9A6FF, 0xFFF1E9FF, gems = Economy.BANNER_GEMS_EPIC),
        shop(45, "Tormenta eléctrica", BannerPattern.RAIN, 0xFF141B33, 0xFF2A3A66, 0xFF8EC9FF, 0xFFEAF3FF, gems = Economy.BANNER_GEMS_EPIC),
        shop(46, "Bosque encantado", BannerPattern.FOREST, 0xFF0B1F1A, 0xFF16423A, 0xFFD6F26B, 0xFFE3F7E8, gems = Economy.BANNER_GEMS_EPIC),
        shop(47, "Cristales", BannerPattern.CRYSTALS, 0xFF1A1440, 0xFF3C2A8F, 0xFF8EE3FF, 0xFFEAF6FF, gems = Economy.BANNER_GEMS_EPIC),
        shop(48, "Nebulosa rosa", BannerPattern.GALAXY, 0xFF1F0A2E, 0xFF6B1F5E, 0xFFFF8CC9, 0xFFFFE6F5, gems = 110),
        shop(49, "Cumbre de lava", BannerPattern.MOUNTAINS, 0xFF2A0F0C, 0xFF7A2314, 0xFFFF7B3A, 0xFFFFE1C6, gems = 110)
    ).sortedWith(compareBy({ it.gemPrice > 0 }, { it.coinPrice }, { it.gemPrice }))

    /** Exclusivos del Pase, por pares: [par*2] en la vía gratis y [par*2+1] en la premium. */
    val seasonal: List<BannerDef> = listOf(
        season(16, "Brisa de primavera", BannerPattern.PETALS, 0xFFE8F8E4, 0xFFFFD9E6, 0xFFFF9EC4, 0xFF47603F),
        season(17, "Eclipse dorado", BannerPattern.STARS, 0xFF08050F, 0xFF2A0F3D, 0xFFFFD36E, 0xFFF2E9FF),
        season(18, "Verano en la playa", BannerPattern.WAVES, 0xFFFFF0C9, 0xFF7FD3E8, 0xFF2E9BC8, 0xFF174A6B),
        season(19, "Atardecer real", BannerPattern.RAYS, 0xFFFFC27A, 0xFFE0558F, 0xFFFFE08A, 0xFFFFF5E6),
        season(20, "Bosque de luciérnagas", BannerPattern.BUBBLES, 0xFF0E2923, 0xFF184238, 0xFFD6F26B, 0xFFE3F7E8),
        season(21, "Cristal de hielo", BannerPattern.SNOW, 0xFFF4FBFF, 0xFFB5D8F2, 0xFFFFFFFF, 0xFF23405E)
    )

    /**
     * Más exclusivos del Pase (nunca se venden): cuatro por temporada (uno en la vía gratis y tres en la premium).
     * Rota entre 3 grupos de cuatro.
     */
    val passExtras: List<BannerDef> = listOf(
        season(60, "Luna de cosecha", BannerPattern.PUMPKINS, 0xFF2A1636, 0xFFB8561B, 0xFFFFD36E, 0xFFFFF1D6),
        season(61, "Noche de murciélagos", BannerPattern.BATS, 0xFF0F0A22, 0xFF3A1A5E, 0xFFC59BFF, 0xFFF1E9FF),
        season(62, "Fuegos de verano", BannerPattern.FIREWORKS, 0xFF120A2A, 0xFF4A1F6B, 0xFFFF8CC0, 0xFFFFE6F5),
        season(63, "Fiesta de farolillos", BannerPattern.LANTERNS, 0xFF1C1236, 0xFF4A2A6E, 0xFFFF6F6F, 0xFFFFE9E0),
        season(64, "Cumbres nevadas", BannerPattern.MOUNTAINS, 0xFFE3F1FF, 0xFFF8FBFF, 0xFF7FA3CF, 0xFF1E3A5F),
        season(65, "Lluvia de neón", BannerPattern.RAIN, 0xFF14102E, 0xFF2A1F5E, 0xFFFF5FA8, 0xFFFFE6F5),
        season(66, "Galaxia violeta", BannerPattern.GALAXY, 0xFF0C0828, 0xFF2A1670, 0xFF8EC9FF, 0xFFEAF3FF),
        season(67, "Jardín de cerezos", BannerPattern.ZEN, 0xFFFFE8EE, 0xFFFFC9D8, 0xFFFF8FB0, 0xFF7A3D55),
        season(68, "Corazón de cristal", BannerPattern.HEARTS, 0xFFE8EEFF, 0xFFC7B8FF, 0xFFFF8FC0, 0xFF3C2F7A),
        season(69, "Atardecer de oro", BannerPattern.SUNSET, 0xFFFFD27A, 0xFFE8743A, 0xFFFFF1B5, 0xFF4A210E),
        season(70, "Bosque de cuento", BannerPattern.FOREST, 0xFFDDF6D2, 0xFF2A6B45, 0xFF2E8B57, 0xFFF0FFF3),
        season(71, "Circuito solar", BannerPattern.CIRCUIT, 0xFF1A1405, 0xFF4A3A0A, 0xFFFFD36E, 0xFFFFF5D6)
    )

    /** Se ganan subiendo de rango de prestigio (y el último, con el Platino). Se conceden solos al llegar. */
    val prestige: List<BannerDef> = listOf(
        prestige(80, "Primeros pasos", com.korkoor.pardos.domain.prestige.PrestigeRank.APPRENTICE, BannerPattern.CONFETTI, 0xFFE3F1FF, 0xFFC7E0FA, 0xFF5D9BD9, 0xFF1F3F66),
        prestige(81, "Sendero del adepto", com.korkoor.pardos.domain.prestige.PrestigeRank.ADEPT, BannerPattern.FOREST, 0xFFD9F0DE, 0xFF2E7A50, 0xFF2E8B57, 0xFFF0FFF3),
        prestige(82, "Cumbres del experto", com.korkoor.pardos.domain.prestige.PrestigeRank.EXPERT, BannerPattern.MOUNTAINS, 0xFFC9DCF7, 0xFFEAD9C8, 0xFF5D7BAA, 0xFF1F2F4F),
        prestige(83, "Templo del maestro", com.korkoor.pardos.domain.prestige.PrestigeRank.MASTER, BannerPattern.ZEN, 0xFFFFF0C8, 0xFFF2CC7A, 0xFFB8862B, 0xFF4A3410),
        prestige(84, "Cristal imperial", com.korkoor.pardos.domain.prestige.PrestigeRank.GRANDMASTER, BannerPattern.CRYSTALS, 0xFF12204A, 0xFF2D5AA8, 0xFF8EE3FF, 0xFFEAF6FF),
        prestige(85, "Ascenso del fénix", com.korkoor.pardos.domain.prestige.PrestigeRank.LEGEND, BannerPattern.EMBERS, 0xFF2A0F0C, 0xFF8A2A10, 0xFFFFC24A, 0xFFFFF0D6),
        prestige(86, "Cosmos mítico", com.korkoor.pardos.domain.prestige.PrestigeRank.MYTHIC, BannerPattern.GALAXY, 0xFF07041A, 0xFF2D1370, 0xFFFFD36E, 0xFFF4EEFF),
        prestige(87, "Platino", null, BannerPattern.AURORA, 0xFF101A33, 0xFF3A4C7A, 0xFFBBCBF2, 0xFFF3F6FF)
    )

    val all: List<BannerDef> = free + shop.sortedBy { it.id } + seasonal + passExtras + prestige

    /** Banner extra de la vía del Pase [slot] (0..3) de [seasonId]. */
    fun seasonExtra(seasonId: Int, slot: Int): BannerDef = passExtras[(seasonId.mod(3)) * 4 + slot.mod(4)]

    /** Banners de prestigio que ya tocan con [rank] (y el del Platino si [platinum]). */
    fun prestigeUnlocked(rank: com.korkoor.pardos.domain.prestige.PrestigeRank, platinum: Boolean): List<BannerDef> =
        prestige.filter { val r = it.unlockRank; if (r == null) platinum else rank.ordinal >= r.ordinal }

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
