package com.korkoor.pardos.audio

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalViewConfiguration

/**
 * Sonido de toque en TODA la app, sin tocar cada botón: se oye un "tok" cuando un toque termina sobre algo que lo recibió
 * (un botón, una tarjeta, un interruptor…). Un deslizamiento (el tablero) o un toque sobre el fondo no suenan.
 * Si el propio botón ya reprodujo otro sonido (comprar, reclamar, volver…), el toque genérico se calla para no sonar doble.
 */
fun Modifier.uiTapSounds(): Modifier = composed {
    val slop = LocalViewConfiguration.current.touchSlop
    pointerInput(slop) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var moved = false
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Final)
                val ch = event.changes.firstOrNull { it.id == down.id } ?: return@awaitEachGesture
                if ((ch.position - down.position).getDistance() > slop) moved = true
                if (ch.changedToUpIgnoreConsumed()) {
                    // "consumida" = algo (un botón) se quedó con el toque
                    if (!moved && ch.isConsumed) GameAudio.tapIfQuiet()
                    return@awaitEachGesture
                }
                if (!ch.pressed) return@awaitEachGesture
            }
        }
    }
}
