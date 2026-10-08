package com.korkoor.pardos.ui.design

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Stairs
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Filter4
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Terrain
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.korkoor.pardos.domain.level.LevelKind

/**
 * Cada tipo de nivel tiene un color y un icono propios: se reconocen en el mapa, en la cabecera del juego y en la
 * tarjeta de reglas. Para un tipo nuevo basta añadir su rama aquí.
 */
fun LevelKind.accent(): Color = when (this) {
    LevelKind.ZEN -> Sage
    LevelKind.SCORE -> Gold
    LevelKind.FOURS -> Terracotta
    LevelKind.HEADSTART -> GemBlue
    LevelKind.STONES -> Color(0xFF7C869B)
    LevelKind.SPRINT -> Color(0xFFD9694C)
    LevelKind.TWINS -> Color(0xFF8E6BD6)
    LevelKind.CLOCK -> Color(0xFFE0782F)
    LevelKind.BOSS -> Color(0xFFB4413C)
    LevelKind.HEAVY -> Color(0xFF8D6E63)
    LevelKind.LADDER -> Color(0xFF2E9E8F)
    LevelKind.MARATHON -> Color(0xFF4F7CAC)
    LevelKind.COMBO -> Color(0xFFE0568B)
    LevelKind.TWIST -> Color(0xFF5C6BC0)
    LevelKind.STORM -> Color(0xFF3C8DAD)
}

fun LevelKind.symbol(): ImageVector = when (this) {
    LevelKind.ZEN -> Icons.Rounded.Spa
    LevelKind.SCORE -> Icons.Rounded.Star
    LevelKind.FOURS -> Icons.Rounded.Filter4
    LevelKind.HEADSTART -> Icons.Rounded.Bolt
    LevelKind.STONES -> Icons.Rounded.Terrain
    LevelKind.SPRINT -> Icons.Rounded.Speed
    LevelKind.TWINS -> Icons.Rounded.ContentCopy
    LevelKind.CLOCK -> Icons.Rounded.Timer
    LevelKind.BOSS -> Icons.Rounded.Shield
    LevelKind.HEAVY -> Icons.Rounded.FitnessCenter
    LevelKind.LADDER -> Icons.Rounded.Stairs
    LevelKind.MARATHON -> Icons.Rounded.DirectionsRun
    LevelKind.COMBO -> Icons.Rounded.Whatshot
    LevelKind.TWIST -> Icons.Rounded.SwapHoriz
    LevelKind.STORM -> Icons.Rounded.Cloud
}
