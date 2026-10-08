package com.korkoor.pardos.ui.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import com.korkoor.pardos.data.local.SettingsManager

/** Vibración del sistema que respeta el interruptor de Ajustes. Úsalo en lugar de `LocalHapticFeedback.current`. */
@Composable
fun rememberGameHaptics(): HapticFeedback {
    val base = LocalHapticFeedback.current
    val settings = SettingsManager(LocalContext.current)
    return remember(base) {
        object : HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                if (settings.hapticsEnabled.value) base.performHapticFeedback(hapticFeedbackType)
            }
        }
    }
}
