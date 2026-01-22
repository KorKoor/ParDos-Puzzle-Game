package com.korkoor.pardos.ui.theme

import androidx.compose.ui.graphics.Color

sealed class GameTheme(
    val colors: List<Color>,
    val name: String,
    val mainTextColor: Color,
    val accentColor: Color,
    val surfaceColor: Color,
    val minLevel: Int
) {
    // 🍦 VANILLA CREAM (El modo por defecto, ultra limpio)
    object Zen : GameTheme(
        colors = listOf(Color(0xFFF9F9F9), Color(0xFFF3F0E9)),
        name = "Vanilla",
        mainTextColor = Color(0xFF5C574F),
        accentColor = Color(0xFFB8C1B0), // Verde eucalipto muy suave
        surfaceColor = Color(0xFFFFFFFF).copy(alpha = 0.95f),
        minLevel = 0
    )

    // 🌸 SAKURA HAZE (Rosa empolvado, nada chillón)
    object Forest : GameTheme(
        colors = listOf(Color(0xFFFFF5F5), Color(0xFFFEE2E2)),
        name = "Sakura",
        mainTextColor = Color(0xFF8B5E61),
        accentColor = Color(0xFFE5B1B1), // Rosa viejo pastel
        surfaceColor = Color(0xFFFFFFFF).copy(alpha = 0.9f),
        minLevel = 256
    )

    // ☁️ CLOUD BLUE (Azul aireado, tipo cielo nórdico)
    object Sunset : GameTheme(
        colors = listOf(Color(0xFFF0F9FF), Color(0xFFE0F2FE)),
        name = "Cloud",
        mainTextColor = Color(0xFF475569),
        accentColor = Color(0xFFBAE6FD), // Azul bebé desaturado
        surfaceColor = Color(0xFFFFFFFF).copy(alpha = 0.85f),
        minLevel = 512
    )

    // 🦄 SOFT LAVENDER (Lila místico pero muy claro)
    object Cyber : GameTheme(
        colors = listOf(Color(0xFFFAF5FF), Color(0xFFF3E8FF)),
        name = "Lavanda",
        mainTextColor = Color(0xFF6B21A8).copy(alpha = 0.7f),
        accentColor = Color(0xFFDDD6FE), // Violeta pastel
        surfaceColor = Color(0xFFFFFFFF).copy(alpha = 0.85f),
        minLevel = 1024
    )

    // 🍵 MATTE MATCHA (Verde té con base crema)
    object Midnight : GameTheme(
        colors = listOf(Color(0xFFF0FDF4), Color(0xFFDCFCE7)),
        name = "Matcha",
        mainTextColor = Color(0xFF166534).copy(alpha = 0.7f),
        accentColor = Color(0xFFBBF7D0), // Verde menta suave
        surfaceColor = Color(0xFFFFFFFF).copy(alpha = 0.9f),
        minLevel = 2048
    )

    companion object {
        val allThemes = listOf(Zen, Forest, Sunset, Cyber, Midnight)
    }
}