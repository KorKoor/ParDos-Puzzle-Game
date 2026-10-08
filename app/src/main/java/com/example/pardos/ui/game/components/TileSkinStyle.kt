package com.korkoor.pardos.ui.game.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.korkoor.pardos.domain.shop.SkinStyle
import com.korkoor.pardos.domain.shop.TileFinish
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

/** 0 = ficha 2, 1 = ficha 4, … hasta 11. */
private fun power(value: Int): Int = (log2(value.coerceAtLeast(2).toFloat()).toInt() - 1).coerceIn(0, 11)

/**
 * Resuelve el aspecto de una ficha. Todo sale de los DATOS de la skin ([TileSkin.style]):
 * paleta de 12 tonos, colores de texto y acabado. Añadir una skin no requiere tocar este código.
 */
@Composable
fun tileLook(skin: TileSkin, value: Int, theme: GameTheme): TileLook = tileLook(skin.style, value, theme)

/** Igual que arriba pero desde un estilo suelto (la vista previa del editor de Studio). */
@Composable
fun tileLook(st: SkinStyle, value: Int, theme: GameTheme): TileLook {
    val p = power(value)

    // Sin paleta propia (Gelatina): colores clásicos según el tema de nivel
    val tone: Color = st.tilePalette?.let { Color(it[p]) } ?: getTileColor(value, theme)
    val text: Color = st.tilePalette?.let {
        if (p >= st.lightTextFromPower) Color(st.lightText) else Color(st.darkText)
    } ?: getTileTextColor(value)

    return when (st.finish) {
        TileFinish.JELLY -> TileLook(tone, text, elevation = if (value >= 128) 4f else 2f)

        TileFinish.FLAT -> TileLook(tone, text, gloss = false, elevation = 0f)

        TileFinish.PORCELAIN -> TileLook(
            tone, text, gloss = true, border = Color.White.copy(alpha = 0.65f), elevation = 3f
        )

        TileFinish.WOOD -> TileLook(
            tone, text, gloss = true, border = Color.White.copy(alpha = 0.35f), elevation = 3f
        )

        TileFinish.GLASS -> TileLook(
            tone.copy(alpha = 0.6f), text, gloss = true, border = Color.White.copy(alpha = 0.85f), elevation = 0f
        )

        TileFinish.METAL -> TileLook(
            tone, text, gloss = true, border = Color(0xFFFFF3CF).copy(alpha = 0.7f), elevation = 4f
        )

        // Neón: ficha oscura; el color de la paleta pasa al borde, el número y el halo
        TileFinish.NEON -> TileLook(
            background = Color(st.surface ?: 0xFF24273B),
            text = tone,
            gloss = false,
            border = tone,
            elevation = 0f,
            glow = tone
        )
    }
}
