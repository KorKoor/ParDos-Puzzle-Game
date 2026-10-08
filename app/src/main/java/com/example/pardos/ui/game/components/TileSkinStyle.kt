package com.korkoor.pardos.ui.game.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.korkoor.pardos.domain.shop.TileSkin
import com.korkoor.pardos.ui.theme.GameTheme
import kotlin.math.log2

/** Cómo se dibuja una ficha con una skin concreta. */
data class TileLook(
    val background: Color,
    val text: Color,
    /** Brillo superior tipo gelatina. */
    val gloss: Boolean = true,
    val border: Color? = null,
    /** Elevación de la sombra en dp (0 = plano). */
    val elevation: Float = 2f,
    /** Halo de color alrededor de la ficha (solo neón). */
    val glow: Color? = null
)

private fun power(value: Int): Int = (log2(value.coerceAtLeast(2).toFloat()).toInt() - 1).coerceIn(0, 11)

private val woodTones = listOf(
    Color(0xFFF0E4D0), Color(0xFFEBD9BD), Color(0xFFE3C9A0), Color(0xFFD9B98A),
    Color(0xFFCFA574), Color(0xFFC29360), Color(0xFFB58250), Color(0xFFA6713F),
    Color(0xFF96612F), Color(0xFF85521F), Color(0xFF734517), Color(0xFF5F3811)
)

private val neonTones = listOf(
    Color(0xFF7BE0D3), Color(0xFF6CC3F0), Color(0xFF7B8CFF), Color(0xFFA27BFF),
    Color(0xFFD27BFF), Color(0xFFFF7BD0), Color(0xFFFF7B91), Color(0xFFFF9C6B),
    Color(0xFFFFC96B), Color(0xFFE6F06B), Color(0xFF9CF06B), Color(0xFF6BF0A5)
)

/** Resuelve el aspecto de una ficha para la skin equipada. */
@Composable
fun tileLook(skin: TileSkin, value: Int, theme: GameTheme): TileLook {
    val jelly = getTileColor(value, theme)
    return when (skin) {
        TileSkin.JELLY -> TileLook(
            background = jelly,
            text = getTileTextColor(value),
            elevation = if (value >= 128) 4f else 2f
        )

        // Sin brillo ni sombra: colores mates, más sobrios
        TileSkin.FLAT -> TileLook(
            background = jelly,
            text = getTileTextColor(value),
            gloss = false,
            elevation = 0f
        )

        // Madera: ecos del ícono de la app. Números marrones grabados sobre tonos de madera.
        TileSkin.WOOD -> {
            val p = power(value)
            TileLook(
                background = woodTones[p],
                text = if (p <= 3) Color(0xFF6B4A34) else Color(0xFFFFF4E3),
                border = Color(0xFFFFFFFF).copy(alpha = 0.35f),
                elevation = 3f
            )
        }

        // Cristal: translúcido con borde claro
        TileSkin.GLASS -> TileLook(
            background = jelly.copy(alpha = 0.55f),
            text = Color(0xFF3D405B),
            gloss = true,
            border = Color.White.copy(alpha = 0.85f),
            elevation = 0f
        )

        // Neón: oscuro, borde y número del color de la ficha
        TileSkin.NEON -> {
            val c = neonTones[power(value)]
            TileLook(
                background = Color(0xFF24273B),
                text = c,
                gloss = false,
                border = c,
                elevation = 0f,
                glow = c
            )
        }
    }
}
