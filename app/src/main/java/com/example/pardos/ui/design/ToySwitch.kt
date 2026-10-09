package com.korkoor.pardos.ui.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * Interruptor "de juguete": canal hundido, ficha redonda con labio y brillo que rebota al cambiar.
 * Sustituye al Switch de Material para que los ajustes tengan la misma personalidad que el resto del juego.
 */
@Composable
fun ToySwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier, onColor: Color = Sage) {
    val haptic = rememberGameHaptics()
    val pos by animateFloatAsState(if (checked) 1f else 0f, spring(dampingRatio = 0.5f, stiffness = 420f), label = "toySwitchPos")
    val track by animateColorAsState(if (checked) onColor else Navy.copy(alpha = 0.22f), label = "toySwitchTrack")
    val width = 58.dp
    val height = 34.dp
    val thumb = 26.dp
    Box(
        modifier = modifier
            .size(width, height)
            .clip(RoundedCornerShape(50))
            .background(Brush.verticalGradient(listOf(track.deepen(0.18f), track)))
            .border(1.dp, Color.Black.copy(alpha = 0.08f), RoundedCornerShape(50))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Switch) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                com.korkoor.pardos.audio.GameAudio.play(if (checked) com.korkoor.pardos.audio.Sfx.UI_OFF else com.korkoor.pardos.audio.Sfx.UI_ON)
                onCheckedChange(!checked)
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // pequeños puntos del canal: se ven al lado vacío
        Box(
            Modifier
                .offset(x = (4.dp + (width - thumb - 8.dp) * pos))
                .size(thumb)
                .graphicsLayer { scaleX = 1f + 0.08f * (1f - kotlin.math.abs(pos * 2f - 1f)); scaleY = scaleX }
                .background(Color.Black.copy(alpha = 0.16f), CircleShape)
                .padding(bottom = 2.dp)
                .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFF1ECE2))), CircleShape)
        ) {
            Box(Modifier.align(Alignment.TopCenter).offset(y = 3.dp).size(thumb * 0.5f, 4.dp).background(Color.White, RoundedCornerShape(50)))
        }
    }
}
