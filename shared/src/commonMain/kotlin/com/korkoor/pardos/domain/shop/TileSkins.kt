package com.korkoor.pardos.domain.shop

/** Partículas ambientales que flotan sobre el fondo. */
enum class ParticleKind {
    NONE, PETALS, SNOW, LEAVES, FIREFLIES, BUBBLES, STARS, EMBERS, SPRINKLES, RAIN, SAND,
    // Fiestas y skins secretas (siempre al final: el orden fija la semilla de cada efecto)
    HEARTS, BATS, MARIGOLD, FLOWERS, CONFETTI, FLAG_CONFETTI, FIREWORKS, SNOWFLAKE, SPARKLES, METEORS
}

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

/** De dónde sale una skin: la tienda la vende, o solo se consigue por otro camino. */
enum class SkinSource {
    SHOP, ALBUM, SEASON, PURCHASE,
    /** Skin de una fiesta del calendario: se gana jugando durante el evento (ver `EventSkins`). */
    EVENT,
    /** Skin secreta: se descubre cumpliendo una condición que no se anuncia (ver `HiddenSkins`). */
    HIDDEN
}

/** Estilos visuales (fichas + temática). El dibujo vive en la app; aquí datos, reglas y precios. */
enum class TileSkin(
    val id: String,
    val displayName: String,
    val rarity: SkinRarity,
    val coinPrice: Int = 0,
    val gemPrice: Int = 0,
    /** Exclusiva: no se vende por monedas ni gemas (álbum, pase de temporada o compra real). */
    val source: SkinSource = SkinSource.SHOP,
    private val baseStyle: SkinStyle
) {
    JELLY(
        "jelly", "Gelatina", SkinRarity.COMMON,
        baseStyle = SkinStyle(TileFinish.JELLY, particles = ParticleKind.PETALS)
    ),
    FLAT(
        "flat", "Papel", SkinRarity.COMMON, coinPrice = 450,
        baseStyle = SkinStyle(
            TileFinish.FLAT,
            tilePalette = ColorRamp.ramp(12, 0xFFF3EBDD, 0xFFE8D5B5, 0xFFD9A66A, 0xFFC4704F, 0xFF8E4A3A, 0xFF4F3B33),
            bgTop = 0xFFF8F3E8, bgBottom = 0xFFEFE6D6, ink = 0xFF4F3B33, accent = 0xFFB8A58A,
            surface = 0xFFFFFDF8, particles = ParticleKind.NONE
        )
    ),
    MINT(
        "mint", "Menta", SkinRarity.COMMON, coinPrice = 600,
        baseStyle = SkinStyle(
            TileFinish.PORCELAIN,
            tilePalette = ColorRamp.ramp(12, 0xFFF0FBF6, 0xFFC8EEDD, 0xFF8FDCC0, 0xFF52C2A2, 0xFF2E9A86, 0xFF1D6B66),
            darkText = 0xFF1F5C55, lightTextFromPower = 4,
            bgTop = 0xFFEFFBF6, bgBottom = 0xFFCFEFE4, ink = 0xFF1F5C55, accent = 0xFF52C2A2,
            surface = 0xFFF8FFFC, particles = ParticleKind.BUBBLES, particleTint = 0xFF7FD6BE
        )
    ),
    WOOD(
        "wood", "Madera", SkinRarity.RARE, coinPrice = 1000,
        baseStyle = SkinStyle(
            TileFinish.WOOD,
            tilePalette = ColorRamp.ramp(12, 0xFFF0E4D0, 0xFFD9B98A, 0xFFB58250, 0xFF85521F, 0xFF5F3811),
            darkText = 0xFF6B4A34, lightText = 0xFFFFF4E3, lightTextFromPower = 4,
            bgTop = 0xFFF3E6D0, bgBottom = 0xFFE2CBA3, ink = 0xFF5B3F2A, accent = 0xFFB58250,
            surface = 0xFFFFF8EC, particles = ParticleKind.NONE
        )
    ),
    SAKURA(
        "sakura", "Cerezo", SkinRarity.RARE, coinPrice = 1200,
        baseStyle = SkinStyle(
            TileFinish.PORCELAIN,
            tilePalette = ColorRamp.ramp(12, 0xFFFFF4F6, 0xFFF8CAD5, 0xFFE99AB1, 0xFFC76B8A, 0xFF8E3E66, 0xFF5B2547),
            darkText = 0xFF8E4A62, lightTextFromPower = 3,
            bgTop = 0xFFFFF1F4, bgBottom = 0xFFFBD5DE, ink = 0xFF7A3D55, accent = 0xFFE89AB0,
            surface = 0xFFFFFAFB, particles = ParticleKind.PETALS, particleTint = 0xFFF4A8BC
        )
    ),
    FOREST(
        "forest", "Bosque", SkinRarity.RARE, coinPrice = 1200,
        baseStyle = SkinStyle(
            TileFinish.JELLY,
            tilePalette = ColorRamp.ramp(12, 0xFFEFF7E6, 0xFFCFE8B9, 0xFF9CCB84, 0xFF5FA264, 0xFF2F7A52, 0xFF1B4D3A),
            darkText = 0xFF3C5A3A, lightTextFromPower = 3,
            bgTop = 0xFFEAF5E4, bgBottom = 0xFFCFE8C4, ink = 0xFF2E5D3A, accent = 0xFF6BAA75,
            surface = 0xFFF8FDF5, particles = ParticleKind.LEAVES, particleTint = 0xFF7DB86A
        )
    ),
    AUTUMN(
        "autumn", "Otoño", SkinRarity.RARE, coinPrice = 1200,
        baseStyle = SkinStyle(
            TileFinish.JELLY,
            tilePalette = ColorRamp.ramp(12, 0xFFFFF3E0, 0xFFFAD7A0, 0xFFF0A860, 0xFFD97B3A, 0xFFA8502A, 0xFF6B3320),
            darkText = 0xFF7A4A28, lightTextFromPower = 3,
            bgTop = 0xFFFFF1DC, bgBottom = 0xFFF6CFA0, ink = 0xFF6B3F1D, accent = 0xFFE08A3C,
            surface = 0xFFFFF9EE, particles = ParticleKind.LEAVES, particleTint = 0xFFE08A3C
        )
    ),
    LAVENDER(
        "lavender", "Lavanda", SkinRarity.RARE, coinPrice = 1300,
        baseStyle = SkinStyle(
            TileFinish.PORCELAIN,
            tilePalette = ColorRamp.ramp(12, 0xFFF7F2FF, 0xFFE0D2FA, 0xFFC4A9F0, 0xFFA283E0, 0xFF7C5CC4, 0xFF4F3A8F),
            darkText = 0xFF5A3F8F, lightTextFromPower = 3,
            bgTop = 0xFFF6F0FF, bgBottom = 0xFFDDD0F5, ink = 0xFF55397F, accent = 0xFFA283E0,
            surface = 0xFFFCFAFF, particles = ParticleKind.FIREFLIES, particleTint = 0xFFB79BEA
        )
    ),
    DESERT(
        "desert", "Dunas", SkinRarity.RARE, coinPrice = 1400,
        baseStyle = SkinStyle(
            TileFinish.JELLY,
            tilePalette = ColorRamp.ramp(12, 0xFFFFF4DC, 0xFFF6D9A0, 0xFFE9B96A, 0xFFD4904A, 0xFFB5663A, 0xFF7A3E2A),
            darkText = 0xFF7A4A24, lightTextFromPower = 3,
            bgTop = 0xFFFFF0D2, bgBottom = 0xFFF1CE96, ink = 0xFF6E4120, accent = 0xFFD4904A,
            surface = 0xFFFFF8EA, particles = ParticleKind.SAND, particleTint = 0xFFD9A55E
        )
    ),
    GLASS(
        "glass", "Hielo", SkinRarity.EPIC, coinPrice = 1500,
        baseStyle = SkinStyle(
            TileFinish.GLASS,
            tilePalette = ColorRamp.ramp(12, 0xFFE9F6FF, 0xFFBEE0F7, 0xFF8EC5EC, 0xFF5E9FD8, 0xFF4B6FC4, 0xFF3A47A0),
            darkText = 0xFF2F4B6B, lightTextFromPower = 99,
            bgTop = 0xFFEAF6FF, bgBottom = 0xFFC9E6FA, ink = 0xFF2F4B6B, accent = 0xFF7FB6E6,
            surface = 0xFFF6FBFF, particles = ParticleKind.SNOW, particleTint = 0xFFFFFFFF
        )
    ),
    CANDY(
        "candy", "Dulces", SkinRarity.EPIC, coinPrice = 1800,
        baseStyle = SkinStyle(
            TileFinish.JELLY,
            tilePalette = ColorRamp.ramp(12, 0xFFFFE3F1, 0xFFFFB8DA, 0xFFFF8CC0, 0xFFE86FD0, 0xFF9B7BEA, 0xFF5BB8E8, 0xFF4FD1B8),
            darkText = 0xFF8A3E6E, lightTextFromPower = 3,
            bgTop = 0xFFFFEEF8, bgBottom = 0xFFD9F5EA, ink = 0xFF8A3E6E, accent = 0xFFFF8CC0,
            surface = 0xFFFFFAFD, particles = ParticleKind.SPRINKLES, particleTint = 0xFFFF8CC0
        )
    ),
    SUNSET(
        "sunset", "Atardecer", SkinRarity.EPIC, coinPrice = 1700,
        baseStyle = SkinStyle(
            TileFinish.JELLY,
            tilePalette = ColorRamp.ramp(12, 0xFFFFF0D6, 0xFFFFD08A, 0xFFFFA36B, 0xFFF5707A, 0xFFD64F8F, 0xFF8E3FA8, 0xFF5B3A9E),
            darkText = 0xFF7A3340, lightTextFromPower = 3,
            bgTop = 0xFFFFE3B8, bgBottom = 0xFFF2A3B4, ink = 0xFF6E2C45, accent = 0xFFF5707A,
            surface = 0xFFFFF6EC, particles = ParticleKind.EMBERS, particleTint = 0xFFFFB27A
        )
    ),
    ARCADE(
        "arcade", "Arcade", SkinRarity.RARE, coinPrice = 1500,
        baseStyle = SkinStyle(
            TileFinish.FLAT,
            tilePalette = listOf(
                0xFFFFE066, 0xFFFFB347, 0xFFFF7A59, 0xFFFF4D6D, 0xFFE24DD0, 0xFF9B5DE5,
                0xFF5C7CFA, 0xFF3FB6F5, 0xFF2DD4BF, 0xFF4ADE80, 0xFFB5E548, 0xFFFFFFFF
            ),
            darkText = 0xFF14142B, lightTextFromPower = 99,
            bgTop = 0xFF14142B, bgBottom = 0xFF222247, ink = 0xFFF4F4FF, accent = 0xFFFF4D6D,
            surface = 0xFF2A2A52, particles = ParticleKind.SPRINKLES, particleTint = 0xFFFF4D6D
        )
    ),
    NEON(
        "neon", "Neón", SkinRarity.EPIC, gemPrice = 80,
        baseStyle = SkinStyle(
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
        "ocean", "Océano", SkinRarity.EPIC, gemPrice = 70,
        baseStyle = SkinStyle(
            TileFinish.GLASS,
            tilePalette = ColorRamp.ramp(12, 0xFFD9F5F2, 0xFF9AE0DC, 0xFF52C3CF, 0xFF2E9BC8, 0xFF2F6FB8, 0xFF3F4FA0),
            darkText = 0xFF0E3B5C, lightTextFromPower = 99,
            bgTop = 0xFF0E3B5C, bgBottom = 0xFF1B7F8C, ink = 0xFFE6FBFF, accent = 0xFF52C3CF,
            surface = 0xFF12506F, particles = ParticleKind.BUBBLES, particleTint = 0xFFBDF1F5
        )
    ),
    STORM(
        "storm", "Tormenta", SkinRarity.EPIC, gemPrice = 75,
        baseStyle = SkinStyle(
            TileFinish.GLASS,
            tilePalette = ColorRamp.ramp(12, 0xFFEEF3FA, 0xFFD3DEEE, 0xFFB5C7E3, 0xFF97AEDB, 0xFF8E9FDD, 0xFFA99CE6),
            darkText = 0xFF0F1B2D, lightTextFromPower = 99,
            bgTop = 0xFF18212F, bgBottom = 0xFF31405A, ink = 0xFFE3ECF8, accent = 0xFF8FA7D8,
            surface = 0xFF222E42, particles = ParticleKind.RAIN, particleTint = 0xFFAEC6E8
        )
    ),
    FIREFLY(
        "firefly", "Luciérnagas", SkinRarity.EPIC, gemPrice = 70,
        baseStyle = SkinStyle(
            TileFinish.JELLY,
            tilePalette = ColorRamp.ramp(12, 0xFFE4F8E6, 0xFFB7E8C2, 0xFF7ED3A0, 0xFF48B884, 0xFF2F9A74, 0xFF2D7A63),
            darkText = 0xFF0F3B2E, lightTextFromPower = 6,
            bgTop = 0xFF0E2923, bgBottom = 0xFF184238, ink = 0xFFE3F7E8, accent = 0xFFD6F26B,
            surface = 0xFF15372F, particles = ParticleKind.FIREFLIES, particleTint = 0xFFD6F26B
        )
    ),
    LAVA(
        "lava", "Volcán", SkinRarity.EPIC, gemPrice = 90,
        baseStyle = SkinStyle(
            TileFinish.JELLY,
            tilePalette = ColorRamp.ramp(12, 0xFFFFEBCF, 0xFFFFC970, 0xFFFF9A3C, 0xFFFF5E3A, 0xFFD62F3B, 0xFF8E1B2E),
            darkText = 0xFF6B2410, lightTextFromPower = 4,
            bgTop = 0xFF1B0B0A, bgBottom = 0xFF42130F, ink = 0xFFFFE1C6, accent = 0xFFFF7B3A,
            surface = 0xFF2E100D, particles = ParticleKind.EMBERS, particleTint = 0xFFFF7B3A
        )
    ),
    SPACE(
        "space", "Galaxia", SkinRarity.LEGENDARY, gemPrice = 150,
        baseStyle = SkinStyle(
            TileFinish.NEON,
            tilePalette = ColorRamp.ramp(12, 0xFF8FD3FF, 0xFFA9A0FF, 0xFFD18BFF, 0xFFFF8BD0, 0xFFFFB48B, 0xFFFFE08B),
            bgTop = 0xFF0B0B2A, bgBottom = 0xFF3A1C71, ink = 0xFFF1EEFF, accent = 0xFFA9A0FF,
            surface = 0xFF1B1745, particles = ParticleKind.STARS, particleTint = 0xFFFFFFFF
        )
    ),
    AURORA(
        "aurora", "Aurora", SkinRarity.LEGENDARY, gemPrice = 160,
        baseStyle = SkinStyle(
            TileFinish.GLASS,
            tilePalette = ColorRamp.ramp(12, 0xFFD9FCF2, 0xFFA6F2D8, 0xFF78E6C6, 0xFF7ED8E8, 0xFF8EC5FF, 0xFFB69CFF, 0xFFE59CFF),
            darkText = 0xFF0B2536, lightTextFromPower = 99,
            bgTop = 0xFF07182B, bgBottom = 0xFF14465A, ink = 0xFFD9FFF2, accent = 0xFF78E6C6,
            surface = 0xFF0F2D44, particles = ParticleKind.STARS, particleTint = 0xFFB8FFE0
        )
    ),
    ROSEGOLD(
        "rosegold", "Oro Rosa", SkinRarity.LEGENDARY, gemPrice = 180,
        baseStyle = SkinStyle(
            TileFinish.METAL,
            tilePalette = ColorRamp.ramp(12, 0xFFFFEDE8, 0xFFF8CFC2, 0xFFEBAA98, 0xFFD58472, 0xFFB45F5C, 0xFF7C3A44),
            darkText = 0xFF6B3236, lightText = 0xFFFFEDE6, lightTextFromPower = 5,
            bgTop = 0xFF2B1B22, bgBottom = 0xFF4A2F38, ink = 0xFFFFD9CF, accent = 0xFFEBAA98,
            surface = 0xFF3A2530, particles = ParticleKind.STARS, particleTint = 0xFFF8CFC2
        )
    ),
    GOLD(
        "gold", "Oro Real", SkinRarity.LEGENDARY, source = SkinSource.ALBUM,
        baseStyle = SkinStyle(
            TileFinish.METAL,
            tilePalette = ColorRamp.ramp(12, 0xFFFFF2C2, 0xFFF6D77A, 0xFFE8B84A, 0xFFC99428, 0xFF9E6F1B, 0xFF6F4A12),
            darkText = 0xFF5A3C0E, lightText = 0xFFFFF3CF, lightTextFromPower = 5,
            bgTop = 0xFF2A2118, bgBottom = 0xFF4A3A1F, ink = 0xFFFFE9A8, accent = 0xFFE8B84A,
            surface = 0xFF3A2E1C, particles = ParticleKind.EMBERS, particleTint = 0xFFFFC95A
        )
    ),

    /** Skin de pago con editor propio: el jugador elige acabado, colores, fondo y partículas. */
    STUDIO(
        "studio", "Studio", SkinRarity.LEGENDARY, source = SkinSource.PURCHASE,
        baseStyle = SkinStyle(TileFinish.JELLY)
    ),

    // --- Exclusivas de temporada: solo se consiguen con el Pase de temporada ---
    ECLIPSE(
        "eclipse", "Eclipse", SkinRarity.LEGENDARY, source = SkinSource.SEASON,
        baseStyle = SkinStyle(
            TileFinish.METAL,
            tilePalette = ColorRamp.ramp(12, 0xFFF4F0FF, 0xFFD6CCF4, 0xFFAE9DE6, 0xFF8B74CF, 0xFF65489F, 0xFF3C2B75),
            darkText = 0xFF2E2060, lightText = 0xFFF4F0FF, lightTextFromPower = 4,
            bgTop = 0xFF08050F, bgBottom = 0xFF2A0F3D, ink = 0xFFF2E9FF, accent = 0xFFFFD36E,
            surface = 0xFF1B1030, particles = ParticleKind.EMBERS, particleTint = 0xFFFFD36E
        )
    ),
    SOLSTICE(
        "solstice", "Solsticio", SkinRarity.LEGENDARY, source = SkinSource.SEASON,
        baseStyle = SkinStyle(
            TileFinish.METAL,
            tilePalette = ColorRamp.ramp(12, 0xFFFFFAE3, 0xFFFFE9A0, 0xFFFFCB5A, 0xFFFFA03A, 0xFFF2693A, 0xFFB8392F),
            darkText = 0xFF6B3410, lightText = 0xFFFFF7E6, lightTextFromPower = 5,
            bgTop = 0xFFFFEFC2, bgBottom = 0xFFFFB873, ink = 0xFF6B3410, accent = 0xFFF2693A,
            surface = 0xFFFFF8E6, particles = ParticleKind.SAND, particleTint = 0xFFFFC95A
        )
    ),
    CRYSTAL(
        "crystal", "Cristal", SkinRarity.LEGENDARY, source = SkinSource.SEASON,
        baseStyle = SkinStyle(
            TileFinish.GLASS,
            tilePalette = ColorRamp.ramp(12, 0xFFFFFFFF, 0xFFE3F5FF, 0xFFC4E6FB, 0xFFA3D2F5, 0xFF9CBBF0, 0xFFB3A8F0),
            darkText = 0xFF23405E, lightTextFromPower = 99,
            bgTop = 0xFFEFFAFF, bgBottom = 0xFFC7E4F6, ink = 0xFF23405E, accent = 0xFF7FB6E6,
            surface = 0xFFF8FDFF, particles = ParticleKind.SNOW, particleTint = 0xFFFFFFFF
        )
    ),

    // ============ Skins de fiestas del calendario (se ganan jugando durante el evento) ============
    VALENTINE(
        "valentine", "Corazón", SkinRarity.EPIC, source = SkinSource.EVENT,
        baseStyle = SkinStyle(
            TileFinish.PORCELAIN,
            tilePalette = ColorRamp.ramp(12, 0xFFFFF0F3, 0xFFFFC2D1, 0xFFFF8FAB, 0xFFE5446D, 0xFFB5173F, 0xFF6D0F2B),
            darkText = 0xFF8A2D4A, lightTextFromPower = 3,
            bgTop = 0xFFFFE5EC, bgBottom = 0xFFFFB3C6, ink = 0xFF7A1F3D, accent = 0xFFE5446D,
            surface = 0xFFFFF7F9, particles = ParticleKind.HEARTS, particleTint = 0xFFFF5C8A
        )
    ),
    SPRING(
        "spring", "Primavera", SkinRarity.EPIC, source = SkinSource.EVENT,
        baseStyle = SkinStyle(
            TileFinish.JELLY,
            tilePalette = ColorRamp.ramp(12, 0xFFFFFBE0, 0xFFFFE680, 0xFFC6EE8A, 0xFF6ED3A0, 0xFFF7A6C8, 0xFFC77DFF, 0xFF8E6BE8),
            darkText = 0xFF3F5E3A, lightTextFromPower = 5,
            bgTop = 0xFFF3FFE6, bgBottom = 0xFFD2F3DA, ink = 0xFF2F6B3A, accent = 0xFFF783B0,
            surface = 0xFFFBFFF5, particles = ParticleKind.FLOWERS, particleTint = 0xFFF783B0
        )
    ),
    SUMMER(
        "summer", "Verano", SkinRarity.EPIC, source = SkinSource.EVENT,
        baseStyle = SkinStyle(
            TileFinish.GLASS,
            tilePalette = ColorRamp.ramp(12, 0xFFE0FBFF, 0xFF9BEBF2, 0xFF4FD0E0, 0xFF2EB5C9, 0xFFFFC857, 0xFFFF8A5B, 0xFFFF5A5F),
            darkText = 0xFF0E4F63, lightTextFromPower = 99,
            bgTop = 0xFF7EDCF2, bgBottom = 0xFFFFE9A8, ink = 0xFF0E4F63, accent = 0xFFFF8A5B,
            surface = 0xFFF2FDFF, particles = ParticleKind.BUBBLES, particleTint = 0xFFFFFFFF
        )
    ),
    INDEPENDENCE(
        "independence", "Viva México", SkinRarity.LEGENDARY, source = SkinSource.EVENT,
        baseStyle = SkinStyle(
            TileFinish.JELLY,
            tilePalette = ColorRamp.ramp(12, 0xFFFFFDF5, 0xFFC6EBC9, 0xFF52B788, 0xFF1B8A5A, 0xFFE9C46A, 0xFFD7263D, 0xFF8E1230),
            darkText = 0xFF0F3D2A, lightText = 0xFFFFFDF5, lightTextFromPower = 4,
            bgTop = 0xFF063D2A, bgBottom = 0xFF1F5A44, ink = 0xFFFFF4DC, accent = 0xFFE9C46A,
            surface = 0xFF0E4A35, particles = ParticleKind.FLAG_CONFETTI, particleTint = 0xFFFFFFFF
        )
    ),
    HALLOWEEN(
        "halloween", "Noche de Brujas", SkinRarity.LEGENDARY, source = SkinSource.EVENT,
        baseStyle = SkinStyle(
            TileFinish.JELLY,
            tilePalette = ColorRamp.ramp(12, 0xFFFFF0D6, 0xFFFFC46B, 0xFFFF8A1F, 0xFFE0560A, 0xFF9B2D6F, 0xFF5B1F8A, 0xFF2B0F4D),
            darkText = 0xFF4A1F0A, lightText = 0xFFFFF0D6, lightTextFromPower = 3,
            bgTop = 0xFF120821, bgBottom = 0xFF3B1458, ink = 0xFFFFD9A0, accent = 0xFFFF8A1F,
            surface = 0xFF241036, particles = ParticleKind.BATS, particleTint = 0xFFC9A0FF
        )
    ),
    MUERTOS(
        "muertos", "Cempasúchil", SkinRarity.LEGENDARY, source = SkinSource.EVENT,
        baseStyle = SkinStyle(
            TileFinish.JELLY,
            tilePalette = ColorRamp.ramp(12, 0xFFFFF6D6, 0xFFFFD35A, 0xFFFFA81F, 0xFFFF7A1A, 0xFFE0388C, 0xFF9B2FAE, 0xFF4B2A8F),
            darkText = 0xFF4A1F2E, lightText = 0xFFFFF6D6, lightTextFromPower = 4,
            bgTop = 0xFF1F0F3A, bgBottom = 0xFF7A2D5C, ink = 0xFFFFE7B0, accent = 0xFFFFA81F,
            surface = 0xFF331A52, particles = ParticleKind.MARIGOLD, particleTint = 0xFFFFA81F
        )
    ),
    CHRISTMAS(
        "christmas", "Navidad", SkinRarity.LEGENDARY, source = SkinSource.EVENT,
        baseStyle = SkinStyle(
            TileFinish.METAL,
            tilePalette = ColorRamp.ramp(12, 0xFFFFFBF2, 0xFFCDEBCB, 0xFF5CAE78, 0xFF16704A, 0xFFC1272D, 0xFF8E1B22, 0xFFE8B84A),
            darkText = 0xFF14503C, lightText = 0xFFFFFBF2, lightTextFromPower = 4,
            bgTop = 0xFF0D3B2E, bgBottom = 0xFF1F6B4F, ink = 0xFFFFF5E0, accent = 0xFFE8B84A,
            surface = 0xFF14503C, particles = ParticleKind.SNOWFLAKE, particleTint = 0xFFFFFFFF
        )
    ),
    NEWYEAR(
        "newyear", "Nochevieja", SkinRarity.LEGENDARY, source = SkinSource.EVENT,
        baseStyle = SkinStyle(
            TileFinish.METAL,
            tilePalette = ColorRamp.ramp(12, 0xFFFFF8E1, 0xFFFBE7A1, 0xFFF1C95A, 0xFFD4A02D, 0xFF8A6A1F, 0xFF3A3A66, 0xFF1F2547),
            darkText = 0xFF4A3510, lightText = 0xFFFFF4D2, lightTextFromPower = 5,
            bgTop = 0xFF050816, bgBottom = 0xFF1B2250, ink = 0xFFFFF1C2, accent = 0xFFF1C95A,
            surface = 0xFF10163A, particles = ParticleKind.FIREWORKS, particleTint = 0xFFF1C95A
        )
    ),

    // ============ Skins secretas (se descubren sin que nadie diga cómo) ============
    NIGHT_OWL(
        "nightowl", "Búho Nocturno", SkinRarity.EPIC, source = SkinSource.HIDDEN,
        baseStyle = SkinStyle(
            TileFinish.GLASS,
            tilePalette = ColorRamp.ramp(12, 0xFFE6ECFF, 0xFFB8C6F5, 0xFF8EA2E8, 0xFF6678D2, 0xFF4A55B0, 0xFF2D3485, 0xFFF2E6A8),
            darkText = 0xFF141B4A, lightTextFromPower = 99,
            bgTop = 0xFF050818, bgBottom = 0xFF16204A, ink = 0xFFE6ECFF, accent = 0xFFF2E6A8,
            surface = 0xFF0C1230, particles = ParticleKind.SPARKLES, particleTint = 0xFFF2E6A8
        )
    ),
    DAWN(
        "dawn", "Amanecer", SkinRarity.EPIC, source = SkinSource.HIDDEN,
        baseStyle = SkinStyle(
            TileFinish.PORCELAIN,
            tilePalette = ColorRamp.ramp(12, 0xFFFFF7E8, 0xFFFFE0B8, 0xFFFFC08A, 0xFFFF9A8A, 0xFFF77FA0, 0xFFC76BC8, 0xFF8E7BE8),
            darkText = 0xFF7A3A52, lightTextFromPower = 4,
            bgTop = 0xFFCFE6FF, bgBottom = 0xFFFFD6C2, ink = 0xFF6B3F5E, accent = 0xFFFF9A8A,
            surface = 0xFFFFFAF6, particles = ParticleKind.EMBERS, particleTint = 0xFFFFB27A
        )
    ),
    PHOENIX(
        "phoenix", "Fénix", SkinRarity.LEGENDARY, source = SkinSource.HIDDEN,
        baseStyle = SkinStyle(
            TileFinish.METAL,
            tilePalette = ColorRamp.ramp(12, 0xFFFFF4C2, 0xFFFFD84A, 0xFFFFA11F, 0xFFFF6A1A, 0xFFE0321A, 0xFFA3141A, 0xFF5B0F1F),
            darkText = 0xFF6B2410, lightText = 0xFFFFF1CF, lightTextFromPower = 4,
            bgTop = 0xFF1A0506, bgBottom = 0xFF5A1A0A, ink = 0xFFFFE2A8, accent = 0xFFFFA11F,
            surface = 0xFF2E0F0A, particles = ParticleKind.EMBERS, particleTint = 0xFFFFC14A
        )
    ),
    CROWN(
        "crown", "Corona", SkinRarity.LEGENDARY, source = SkinSource.HIDDEN,
        baseStyle = SkinStyle(
            TileFinish.METAL,
            tilePalette = ColorRamp.ramp(12, 0xFFFFF9E0, 0xFFFFE58A, 0xFFF6C945, 0xFFD9A21B, 0xFFA8761A, 0xFF6B1B3A, 0xFF3A0F2A),
            darkText = 0xFF5A3C0E, lightText = 0xFFFFF3CF, lightTextFromPower = 5,
            bgTop = 0xFF1E0B1C, bgBottom = 0xFF4A1230, ink = 0xFFFFE9A8, accent = 0xFFF6C945,
            surface = 0xFF331225, particles = ParticleKind.SPARKLES, particleTint = 0xFFF6C945
        )
    ),
    PRISM(
        "prism", "Prisma", SkinRarity.LEGENDARY, source = SkinSource.HIDDEN,
        baseStyle = SkinStyle(
            TileFinish.GLASS,
            tilePalette = ColorRamp.ramp(12, 0xFFFF9AA2, 0xFFFFB7B2, 0xFFFFDAC1, 0xFFFFF3A8, 0xFFB5EAD7, 0xFFA0E7E5, 0xFFA5C8FF, 0xFFC7B6FF, 0xFFF3A6FF),
            darkText = 0xFF2B2A52, lightTextFromPower = 99,
            bgTop = 0xFF1C1C3A, bgBottom = 0xFF2E2A5C, ink = 0xFFF4F0FF, accent = 0xFFC7B6FF,
            surface = 0xFF272550, particles = ParticleKind.SPARKLES, particleTint = 0xFFFFFFFF
        )
    ),
    JADE(
        "jade", "Jade Imperial", SkinRarity.EPIC, source = SkinSource.HIDDEN,
        baseStyle = SkinStyle(
            TileFinish.PORCELAIN,
            tilePalette = ColorRamp.ramp(12, 0xFFF1FBF3, 0xFFCDEED8, 0xFF9ADDB4, 0xFF5CC38E, 0xFF2E9E6C, 0xFF1B7050, 0xFFD9B25A),
            darkText = 0xFF14503C, lightTextFromPower = 4,
            bgTop = 0xFF0E2A22, bgBottom = 0xFF1C4A3A, ink = 0xFFE3F7E8, accent = 0xFFD9B25A,
            surface = 0xFF163A30, particles = ParticleKind.LEAVES, particleTint = 0xFF7ED3A0
        )
    ),
    METEOR(
        "meteor", "Estrella Fugaz", SkinRarity.LEGENDARY, source = SkinSource.HIDDEN,
        baseStyle = SkinStyle(
            TileFinish.NEON,
            tilePalette = listOf(
                0xFF7BF0E8, 0xFF6CD0FF, 0xFF7BA4FF, 0xFF8E8BFF, 0xFFB98BFF, 0xFFE58BFF,
                0xFFFF8BD8, 0xFFFF8BA8, 0xFFFFA98B, 0xFFFFD38B, 0xFFFFF08B, 0xFFFFFFFF
            ),
            bgTop = 0xFF04040F, bgBottom = 0xFF141446, ink = 0xFFEEF0FF, accent = 0xFF8E8BFF,
            surface = 0xFF14143A, particles = ParticleKind.METEORS, particleTint = 0xFFFFFFFF
        )
    ),
    OBSIDIAN(
        "obsidian", "Obsidiana", SkinRarity.LEGENDARY, source = SkinSource.HIDDEN,
        baseStyle = SkinStyle(
            TileFinish.METAL,
            tilePalette = ColorRamp.ramp(12, 0xFFE4E0F2, 0xFFB5AFD0, 0xFF857FA6, 0xFF5E5885, 0xFF433E69, 0xFF2C2950, 0xFF8E5CF0),
            darkText = 0xFF1E1B38, lightText = 0xFFEDE8FF, lightTextFromPower = 3,
            bgTop = 0xFF08070F, bgBottom = 0xFF1E1A33, ink = 0xFFE6E2F5, accent = 0xFF8E5CF0,
            surface = 0xFF14122A, particles = ParticleKind.SPARKLES, particleTint = 0xFF8E5CF0
        )
    );

    /** Estilo visual. Studio lee la configuración que armó el jugador. */
    val style: SkinStyle get() = if (this === STUDIO) StudioSkin.style else baseStyle

    /** No se compra con monedas ni gemas. */
    val exclusive: Boolean get() = source != SkinSource.SHOP

    val isFree: Boolean get() = coinPrice == 0 && gemPrice == 0 && !exclusive

    companion object {
        val DEFAULT = JELLY

        fun fromId(id: String?): TileSkin = entries.firstOrNull { it.id == id } ?: DEFAULT

        /** Skins que rotan como premio del Pase de temporada. */
        val seasonal: List<TileSkin> = listOf(ECLIPSE, SOLSTICE, CRYSTAL)

        /** Skins de fiestas del calendario y skins secretas. */
        val events: List<TileSkin> get() = entries.filter { it.source == SkinSource.EVENT }
        val hidden: List<TileSkin> get() = entries.filter { it.source == SkinSource.HIDDEN }
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
