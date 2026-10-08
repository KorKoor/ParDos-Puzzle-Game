package com.korkoor.pardos.ui.design

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import kotlinx.coroutines.delay

/** ¿El degradado de fondo es oscuro? (para decidir el color de los iconos de las barras del sistema). */
fun isDarkBackground(colors: List<Color>): Boolean =
    colors.isNotEmpty() && colors.map { it.luminance() }.average() < 0.35

/**
 * Sobre un fondo oscuro los iconos de la barra de estado y de navegación pasan a claros mientras esta pantalla está abierta.
 * Al salir se vuelve al valor de la app (iconos oscuros) en lugar de "lo anterior": el splash puede haberlo dejado en claro.
 */
@Composable
fun DarkSystemBars(dark: Boolean) {
    val view = LocalView.current
    fun apply(lightIcons: Boolean) {
        val window = (view.context as? Activity)?.window ?: return
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = lightIcons
            isAppearanceLightNavigationBars = lightIcons
        }
    }
    DisposableEffect(dark) {
        if (dark) apply(false)
        onDispose { apply(true) }
    }
    // El splash restaura los iconos al cerrarse, justo después de que la pantalla los puso claros: se reaplican
    LaunchedEffect(dark) {
        if (dark) {
            delay(900)
            apply(false)
        }
    }
}
