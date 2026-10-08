package com.korkoor.pardos.domain.shop

/** Partículas ambientales que flotan sobre el fondo. */
enum class ParticleKind { NONE, PETALS, SNOW, LEAVES, FIREFLIES, BUBBLES, STARS, EMBERS, SPRINKLES }

/** Acabado físico de la ficha; el dibujo lo resuelve la app. */
enum class TileFinish { JELLY, FLAT, GLASS, WOOD, NEON, PORCELAIN, METAL }

/** Interpolación de colores ARGB (Long) para generar paletas sin repetir 12 constantes por skin. */
object ColorRamp {
    private fun channel(c: Long, shift: Int): Int = ((c shr shift) and 0xFF).toInt()

    fun lerp(a: Long, b: Long, t: Float): Long {
        fun mix(shift: Int): Long =
            (channel(a, shift) * (1 - t) + channel(b, shift) * t).toInt().coerceIn(0, 255).toLong()
        return (mix(24) shl 24) or (mix(16) shl 16) or (mix(8) shl 8) or mix(0)
    }

    /** [steps] colores repartidos a lo largo de los [anchors] (mínimo 2). */
    fun ramp(steps: Int, vararg anchors: Long): List<Long> {
        require(anchors.size >= 2 && steps >= 2)
        return List(steps) { i ->
            val pos = i.toFloat() / (steps - 1) * (anchors.size - 1)
            val idx = pos.toInt().coerceAtMost(anchors.size - 2)
            lerp(anchors[idx], anchors[idx + 1], pos - idx)
        }
    }
}

/**
 * Aspecto completo de una skin: cómo se ven las fichas y también la temática del juego
 * (fondo, color de texto, acento y partículas). Todo son datos: añadir una skin = añadir una entrada.
 */
data class SkinStyle(
    val finish: TileFinish,
    /** 12 tonos (2, 4, 8 … 4096+). `null` = usar la paleta clásica según el tema de nivel. */
    val tilePalette: List<Long>? = null,
    val darkText: Long = 0xFF5C4F44,
    val lightText: Long = 0xFFFFFBF5,
    /** Desde qué potencia (0 = ficha 2) el texto pasa a ser claro. */
    val lightTextFromPower: Int = 3,
    /** Fondo del juego: si es `null` la skin no cambia el tema. */
    val bgTop: Long? = null,
    val bgBottom: Long? = null,
    /** Texto principal sobre el fondo (títulos). */
    val ink: Long? = null,
    val accent: Long? = null,
    val surface: Long? = null,
    val particles: ParticleKind = ParticleKind.PETALS,
    val particleTint: Long = 0xFFF4B6C2
) {
    val changesTheme: Boolean get() = bgTop != null && bgBottom != null
}

enum class SkinRarity { COMMON, RARE, EPIC, LEGENDARY }

/** Estilos visuales (fichas + temática). El dibujo vive en la app; aquí datos, reglas y precios. */
enum class TileSkin(
    val id: String,
    val rarity: SkinRarity,
    val coinPrice: Int = 0,
    val gemPrice: Int = 0,
    /** Exclusiva: solo se consigue con un logro (no se puede comprar). */
    val exclusive: Boolean = false,
    val style: SkinStyle
) {
    JELLY(
        "jelly", SkinRarity.COMMON,
        style = SkinStyle(TileFinish.JELLY, particles = ParticleKind.PETALS)
    ),
    FLAT(
        "flat", SkinRarity.COMMON, coinPrice = 450,
        style = SkinStyle(
            TileFinish.FLAT,
            tilePalette = ColorRamp.ramp(12, 0xFFF3EBDD, 0xFFE8D5B5, 0xFFD9A66A, 0xFFC4704F, 0xFF8E4A3A, 0xFF4F3B33),
            bgTop = 0xFFF8F3E8, bgBottom = 0xFFEFE6D6, ink = 0xFF4F3B33, accent = 0xFFB8A58A,
            surface = 0xFFFFFDF8, particles = ParticleKind.NONE
        )
    ),
    WOOD(
        "wood", SkinRarity.RARE, coinPrice = 1000,
        style = SkinStyle(
            TileFinish.WOOD,
            tilePalette = ColorRamp.ramp(12, 0xFFF0E4D0, 0xFFD9B98A, 0xFFB58250, 0xFF85521F, 0xFF5F3811),
            darkText = 0xFF6B4A34, lightText = 0xFFFFF4E3, lightTextFromPower = 4,
            bgTop = 0xFFF3E6D0, bgBottom = 0xFFE2CBA3, ink = 0xFF5B3F2A, accent = 0xFFB58250,
            surface = 0xFFFFF8EC, particles = ParticleKind.NONE
        )
    ),
    SAKURA(
        "sakura", SkinRarity.RARE, coinPrice = 1200,
        style = SkinStyle(
            TileFinish.PORCELAIN,
            tilePalette = ColorRamp.ramp(12, 0xFFFFF4F6, 0xFFF8CAD5, 0xFFE99AB1, 0xFFC76B8A, 0xFF8E3E66, 0xFF5B2547),
            darkText = 0xFF8E4A62, lightTextFromPower = 3,
            bgTop = 0xFFFFF1F4, bgBottom = 0xFFFBD5DE, ink = 0xFF7A3D55, accent = 0xFFE89AB0,
            surface = 0xFFFFFAFB, particles = ParticleKind.PETALS, particleTint = 0xFFF4A8BC
        )
    ),
    FOREST(
        "forest", SkinRarity.RARE, coinPrice = 1200,
        style = SkinStyle(
            TileFinish.JELLY,
            tilePalette = ColorRamp.ramp(12, 0xFFEFF7E6, 0xFFCFE8B9, 0xFF9CCB84, 0xFF5FA264, 0xFF2F7A52, 0xFF1B4D3A),
            darkText = 0xFF3C5A3A, lightTextFromPower = 3,
            bgTop = 0xFFEAF5E4, bgBottom = 0xFFCFE8C4, ink = 0xFF2E5D3A, accent = 0xFF6BAA75,
            surface = 0xFFF8FDF5, particles = ParticleKind.LEAVES, particleTint = 0xFF7DB86A
        )
    ),
    AUTUMN(
        "autumn", SkinRarity.RARE, coinPrice = 1200,
        style = SkinStyle(
            TileFinish.JELLY,
            tilePalette = ColorRamp.ramp(12, 0xFFFFF3E0, 0xFFFAD7A0, 0xFFF0A860, 0xFFD97B3A, 0xFFA8502A, 0xFF6B3320),
            darkText = 0xFF7A4A28, lightTextFromPower = 3,
            bgTop = 0xFFFFF1DC, bgBottom = 0xFFF6CFA0, ink = 0xFF6B3F1D, accent = 0xFFE08A3C,
            surface = 0xFFFFF9EE, particles = ParticleKind.LEAVES, particleTint = 0xFFE08A3C
        )
    ),
    GLASS(
        "glass", SkinRarity.EPIC, coinPrice = 1500,
        style = SkinStyle(
            TileFinish.GLASS,
            tilePalette = ColorRamp.ramp(12, 0xFFE9F6FF, 0xFFBEE0F7, 0xFF8EC5EC, 0xFF5E9FD8, 0xFF4B6FC4, 0xFF3A47A0),
            darkText = 0xFF2F4B6B, lightTextFromPower = 99,
            bgTop = 0xFFEAF6FF, bgBottom = 0xFFC9E6FA, ink = 0xFF2F4B6B, accent = 0xFF7FB6E6,
            surface = 0xFFF6FBFF, particles = ParticleKind.SNOW, particleTint = 0xFFFFFFFF
        )
    ),
    CANDY(
        "candy", SkinRarity.EPIC, coinPrice = 1800,
        style = SkinStyle(
            TileFinish.JELLY,
            tilePalette = ColorRamp.ramp(12, 0xFFFFE3F1, 0xFFFFB8DA, 0xFFFF8CC0, 0xFFE86FD0, 0xFF9B7BEA, 0xFF5BB8E8, 0xFF4FD1B8),
            darkText = 0xFF8A3E6E, lightTextFromPower = 3,
            bgTop = 0xFFFFEEF8, bgBottom = 0xFFD9F5EA, ink = 0xFF8A3E6E, accent = 0xFFFF8CC0,
            surface = 0xFFFFFAFD, particles = ParticleKind.SPRINKLES, particleTint = 0xFFFF8CC0
        )
    ),
    NEON(
        "neon", SkinRarity.EPIC, gemPrice = 80,
        style = SkinStyle(
            TileFinish.NEON,
            tilePalette = listOf(
                0xFF7BE0D3, 0xFF6CC3F0, 0xFF7B8CFF, 0xFFA27BFF, 0xFFD27BFF, 0xFFFF7BD0,
                0xFFFF7B91, 0xFFFF9C6B, 0xFFFFC96B, 0xFFE6F06B, 0xFF9CF06B, 0xFF6BF0A5
            ),
            bgTop = 0xFF171A2B, bgBottom = 0xFF2B1F4A, ink = 0xFFEDEEFF, accent = 0xFF7B8CFF,
            surface = 0xFF24273B, particles = ParticleKind.STARS, particleTint = 0xFFA27BFF
        )
    ),
    OCEAN(
        "ocean", SkinRarity.EPIC, gemPrice = 70,
        style = SkinStyle(
            TileFinish.GLASS,
            tilePalette = ColorRamp.ramp(12, 0xFFD9F5F2, 0xFF9AE0DC, 0xFF52C3CF, 0xFF2E9BC8, 0xFF2F6FB8, 0xFF3F4FA0),
            darkText = 0xFF0E3B5C, lightTextFromPower = 99,
            bgTop = 0xFF0E3B5C, bgBottom = 0xFF1B7F8C, ink = 0xFFE6FBFF, accent = 0xFF52C3CF,
            surface = 0xFF12506F, particles = ParticleKind.BUBBLES, particleTint = 0xFFBDF1F5
        )
    ),
    SPACE(
        "space", SkinRarity.LEGENDARY, gemPrice = 150,
        style = SkinStyle(
            TileFinish.NEON,
            tilePalette = ColorRamp.ramp(12, 0xFF8FD3FF, 0xFFA9A0FF, 0xFFD18BFF, 0xFFFF8BD0, 0xFFFFB48B, 0xFFFFE08B),
            bgTop = 0xFF0B0B2A, bgBottom = 0xFF3A1C71, ink = 0xFFF1EEFF, accent = 0xFFA9A0FF,
            surface = 0xFF1B1745, particles = ParticleKind.STARS, particleTint = 0xFFFFFFFF
        )
    ),
    GOLD(
        "gold", SkinRarity.LEGENDARY, exclusive = true,
        style = SkinStyle(
            TileFinish.METAL,
            tilePalette = ColorRamp.ramp(12, 0xFFFFF2C2, 0xFFF6D77A, 0xFFE8B84A, 0xFFC99428, 0xFF9E6F1B, 0xFF6F4A12),
            darkText = 0xFF5A3C0E, lightText = 0xFFFFF3CF, lightTextFromPower = 5,
            bgTop = 0xFF2A2118, bgBottom = 0xFF4A3A1F, ink = 0xFFFFE9A8, accent = 0xFFE8B84A,
            surface = 0xFF3A2E1C, particles = ParticleKind.EMBERS, particleTint = 0xFFFFC95A
        )
    );

    val isFree: Boolean get() = coinPrice == 0 && gemPrice == 0 && !exclusive

    companion object {
        val DEFAULT = JELLY

        fun fromId(id: String?): TileSkin = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

/** Estado de skins del jugador (puro, sin guardado): sirve para razonar compras y equipado. */
data class SkinInventory(
    val owned: Set<String> = setOf(TileSkin.DEFAULT.id),
    val equipped: String = TileSkin.DEFAULT.id
) {
    fun owns(skin: TileSkin): Boolean = skin.isFree || skin.id in owned

    sealed interface Purchase {
        data class Ok(val inventory: SkinInventory, val coinsLeft: Int, val gemsLeft: Int) : Purchase
        data object AlreadyOwned : Purchase
        data object NotEnoughCoins : Purchase
        data object NotEnoughGems : Purchase
        /** Skin exclusiva: solo se obtiene con su logro. */
        data object NotPurchasable : Purchase
    }

    /** Intenta comprar [skin] con el saldo dado (con [discountPercent] de descuento sobre el precio). */
    fun buy(skin: TileSkin, coins: Int, gems: Int, discountPercent: Int = 0): Purchase {
        if (owns(skin)) return Purchase.AlreadyOwned
        if (skin.exclusive) return Purchase.NotPurchasable
        val factor = (100 - discountPercent.coerceIn(0, 90)) / 100.0
        val coinCost = kotlin.math.ceil(skin.coinPrice * factor).toInt()
        val gemCost = kotlin.math.ceil(skin.gemPrice * factor).toInt()
        return when {
            coins < coinCost -> Purchase.NotEnoughCoins
            gems < gemCost -> Purchase.NotEnoughGems
            else -> Purchase.Ok(
                inventory = copy(owned = owned + skin.id),
                coinsLeft = coins - coinCost,
                gemsLeft = gems - gemCost
            )
        }
    }

    /** Concede una skin (recompensa de logro, pack...). */
    fun grant(skin: TileSkin): SkinInventory = copy(owned = owned + skin.id)

    /** Equipa una skin solo si ya es del jugador. */
    fun equip(skin: TileSkin): SkinInventory = if (owns(skin)) copy(equipped = skin.id) else this
}
