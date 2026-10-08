package com.korkoor.pardos.ui.game.menu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.korkoor.pardos.ui.design.pardosBackdrop

/**
 * Capa de ambiente que va ENCIMA del degradado del tema (menú, juego, modos): luces suaves, manchas de color que respiran y
 * fichas fantasma del juego que suben despacio. Es el mismo fondo `pardosBackdrop()` del resto de pantallas, pero sin base propia,
 * así respeta el color de la skin equipada. (Antes dibujaba un mantel de cuadros inclinado, que ya se veía anticuado.)
 */
@Composable
@Suppress("UNUSED_PARAMETER")
fun PicnicBackgroundOptimized(color: Color) {
    Box(Modifier.fillMaxSize().pardosBackdrop(Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))))
}
