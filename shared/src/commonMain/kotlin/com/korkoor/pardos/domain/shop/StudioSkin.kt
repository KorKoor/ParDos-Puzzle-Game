package com.korkoor.pardos.domain.shop

import kotlin.math.abs

/** Conversión HSL → ARGB (Long) sin dependencias de UI, para generar paletas desde un tono. */
object Hsl {
    /** [h] en grados (0..360), [s] y [l] entre 0 y 1. Devuelve ARGB opaco. */
    fun argb(h: Float, s: Float, l: Float): Long {
        val hue = ((h % 360f) + 360f) % 360f
        val sat = s.coerceIn(0f, 1f)
        val lig = l.coerceIn(0f, 1f)
        val c = (1f - abs(2f * lig - 1f)) * sat
        val hp = hue / 60f
        val x = c * (1f - abs(hp % 2f - 1f))
        val (r1, g1, b1) = when {
            hp < 1f -> Triple(c, x, 0f)
            hp < 2f -> Triple(x, c, 0f)
            hp < 3f -> Triple(0f, c, x)
            hp < 4f -> Triple(0f, x, c)
            hp < 5f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        val m = lig - c / 2f
        fun ch(v: Float): Long = ((v + m) * 255f + 0.5f).toInt().coerceIn(0, 255).toLong()
        return 0xFF000000L or (ch(r1) shl 16) or (ch(g1) shl 8) or ch(b1)
    }

    /** Camino más corto entre dos tonos (el arcoíris es circular). */
    fun lerpHue(a: Float, b: Float, t: Float): Float {
        var d = ((b - a) % 360f + 540f) % 360f - 180f
        if (d == -180f) d = 180f
        return ((a + d * t) % 360f + 360f) % 360f
    }
}

enum class StudioTone { PASTEL, VIVID, DEEP }
enum class StudioBackground { LIGHT, TINTED, DARK }

/**
 * Lo que el jugador personaliza en la skin Studio. Todo son pocos parámetros; de ahí sale un
 * [SkinStyle] completo (12 tonos de ficha, texto, fondo, acento y partículas), así que cualquier
 * combinación se ve bien y mantiene el contraste del texto.
 */
data class StudioConfig(
    val finish: TileFinish = TileFinish.JELLY,
    /** Tono de las fichas pequeñas (0..359). */
    val hue: Int = 200,
    /** Tono de las fichas grandes: la paleta viaja de [hue] a [hue2]. */
    val hue2: Int = 285,
    val tone: StudioTone = StudioTone.VIVID,
    val background: StudioBackground = StudioBackground.TINTED,
    val particles: ParticleKind = ParticleKind.STARS
) {
    /** El neón solo se lee bien sobre fondo oscuro. */
    val effectiveBackground: StudioBackground
        get() = if (finish == TileFinish.NEON) StudioBackground.DARK else background

    fun toStyle(): SkinStyle {
        val h1 = ((hue % 360) + 360) % 360
        val h2 = ((hue2 % 360) + 360) % 360
        val bg = effectiveBackground
        val dark = bg == StudioBackground.DARK

        // Rango de saturación y luminosidad según el carácter elegido
        val (sat0, sat1, light0, light1) = when (tone) {
            StudioTone.PASTEL -> listOf(0.55f, 0.50f, 0.94f, 0.50f)
            StudioTone.VIVID -> listOf(0.70f, 0.80f, 0.90f, 0.34f)
            StudioTone.DEEP -> listOf(0.55f, 0.65f, 0.80f, 0.24f)
        }
        val lightSteps = List(12) { i -> light0 + (light1 - light0) * i / 11f }
        val palette = List(12) { i ->
            val t = i / 11f
            Hsl.argb(Hsl.lerpHue(h1.toFloat(), h2.toFloat(), t), sat0 + (sat1 - sat0) * t, lightSteps[i])
        }

        // Texto claro en cuanto la ficha se vuelve oscura (salvo vidrio, que depende del fondo)
        val firstDark = lightSteps.indexOfFirst { it < 0.58f }.let { if (it < 0) 99 else it }
        val lightFrom = when (finish) {
            TileFinish.GLASS -> if (dark) 0 else 99
            else -> firstDark
        }

        val top: Long
        val bottom: Long
        val ink: Long
        val surface: Long
        when (bg) {
            StudioBackground.LIGHT -> {
                top = Hsl.argb(h1.toFloat(), 0.28f, 0.96f); bottom = Hsl.argb(h1.toFloat(), 0.32f, 0.90f)
                ink = Hsl.argb(h1.toFloat(), 0.45f, 0.24f); surface = Hsl.argb(h1.toFloat(), 0.30f, 0.985f)
            }
            StudioBackground.TINTED -> {
                top = Hsl.argb(h1.toFloat(), 0.60f, 0.93f); bottom = Hsl.argb(h2.toFloat(), 0.50f, 0.84f)
                ink = Hsl.argb(h1.toFloat(), 0.50f, 0.22f); surface = Hsl.argb(h1.toFloat(), 0.45f, 0.975f)
            }
            StudioBackground.DARK -> {
                top = Hsl.argb(h1.toFloat(), 0.45f, 0.11f); bottom = Hsl.argb(h2.toFloat(), 0.50f, 0.22f)
                ink = Hsl.argb(h1.toFloat(), 0.30f, 0.93f); surface = Hsl.argb(h1.toFloat(), 0.35f, 0.16f)
            }
        }

        return SkinStyle(
            finish = finish,
            tilePalette = palette,
            darkText = Hsl.argb(h1.toFloat(), 0.55f, 0.20f),
            lightText = Hsl.argb(h1.toFloat(), 0.40f, 0.97f),
            lightTextFromPower = lightFrom,
            bgTop = top, bgBottom = bottom, ink = ink,
            accent = Hsl.argb(h1.toFloat(), 0.65f, 0.60f),
            surface = surface,
            particles = particles,
            particleTint = if (dark) Hsl.argb(h2.toFloat(), 0.65f, 0.78f) else Hsl.argb(h1.toFloat(), 0.55f, 0.62f)
        )
    }

    // ---- Guardado como texto (SharedPreferences hoy, UserDefaults mañana) ----
    fun encode(): String = listOf(finish.name, hue, hue2, tone.name, background.name, particles.name).joinToString("|")

    companion object {
        /** Recupera una configuración guardada; si el texto es inválido devuelve la de fábrica. */
        fun decode(text: String?): StudioConfig {
            val parts = text?.split("|") ?: return StudioConfig()
            if (parts.size != 6) return StudioConfig()
            return try {
                StudioConfig(
                    finish = TileFinish.valueOf(parts[0]),
                    hue = parts[1].toInt().mod(360),
                    hue2 = parts[2].toInt().mod(360),
                    tone = StudioTone.valueOf(parts[3]),
                    background = StudioBackground.valueOf(parts[4]),
                    particles = ParticleKind.valueOf(parts[5])
                )
            } catch (e: IllegalArgumentException) {
                StudioConfig()
            }
        }
    }
}

/** Configuración activa de la skin Studio (la app la carga al abrir y al guardar cambios). */
object StudioSkin {
    var config: StudioConfig = StudioConfig()
    val style: SkinStyle get() = config.toStyle()

    /** Producto de Play / App Store que desbloquea el editor. */
    const val PRODUCT_ID = "skin_studio"
}

/** Puntos de partida para no empezar de cero. */
data class StudioPreset(val name: String, val config: StudioConfig)

object StudioPresets {
    val all: List<StudioPreset> = listOf(
        StudioPreset("Cielo", StudioConfig(TileFinish.JELLY, 200, 285, StudioTone.VIVID, StudioBackground.TINTED, ParticleKind.STARS)),
        StudioPreset("Mandarina", StudioConfig(TileFinish.JELLY, 28, 350, StudioTone.VIVID, StudioBackground.LIGHT, ParticleKind.EMBERS)),
        StudioPreset("Medianoche", StudioConfig(TileFinish.NEON, 265, 320, StudioTone.VIVID, StudioBackground.DARK, ParticleKind.STARS)),
        StudioPreset("Jade", StudioConfig(TileFinish.PORCELAIN, 150, 185, StudioTone.PASTEL, StudioBackground.TINTED, ParticleKind.BUBBLES)),
        StudioPreset("Cuarzo", StudioConfig(TileFinish.GLASS, 330, 285, StudioTone.PASTEL, StudioBackground.LIGHT, ParticleKind.SNOW)),
        StudioPreset("Bronce", StudioConfig(TileFinish.METAL, 32, 12, StudioTone.DEEP, StudioBackground.DARK, ParticleKind.EMBERS)),
        StudioPreset("Selva", StudioConfig(TileFinish.WOOD, 95, 140, StudioTone.DEEP, StudioBackground.TINTED, ParticleKind.LEAVES)),
        StudioPreset("Hielo", StudioConfig(TileFinish.FLAT, 195, 225, StudioTone.PASTEL, StudioBackground.LIGHT, ParticleKind.NONE))
    )
}
