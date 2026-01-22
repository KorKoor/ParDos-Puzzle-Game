package com.korkoor.pardos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFFBBADA0),
    secondary = Color(0xFF776E65),
    background = Color(0xFFFAF8EF)
)

@Composable
fun PardosTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) darkColorScheme() else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}