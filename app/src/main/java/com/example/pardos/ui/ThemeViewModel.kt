@file:Suppress("COMPOSE_APPLIER_CALL_MISMATCH")

package com.korkoor.pardos.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel

class ThemeViewModel : ViewModel() {
    // El tema que se está mostrando actualmente
    // Tema por nivel (se desbloquea al llegar a ciertas metas) y, encima, el de la skin equipada
    private var baseTheme by mutableStateOf<GameTheme>(GameTheme.Zen)
    private var skinTheme by mutableStateOf<GameTheme?>(null)

    /** Tema activo: el de la skin si cambia la temática; si no, el de nivel. */
    val currentTheme: GameTheme get() = skinTheme ?: baseTheme

    /**
     * Tema para pantallas con texto oscuro sobre el fondo (mapa, listas): una skin clara se aplica,
     * una oscura no, para no perder legibilidad.
     */
    val uiTheme: GameTheme
        get() = skinTheme?.takeIf { !(it is GameTheme.Skinned && it.isDark) } ?: baseTheme

    fun applySkin(skin: com.korkoor.pardos.domain.shop.TileSkin) {
        skinTheme = GameTheme.fromSkin(skin)
    }

    // El objetivo del nivel actual (para lógica de desbloqueo)
    var currentLevelTarget by mutableIntStateOf(0)
        private set

    /**
     * Se llama al iniciar cualquier nivel.
     * Actualiza el objetivo actual y ajusta el tema automáticamente si es necesario.
     */
    fun updateLevel(targetValue: Int) {
        currentLevelTarget = targetValue

        // LÓGICA ORIGINAL: Al entrar a un nivel, se pone el tema que le toca automáticamente
        // basado en el nivel más alto desbloqueado para ese target.
        val targetTheme = GameTheme.allThemes
            .filterNotNull()
            .filter { theme -> targetValue >= theme.minLevel }
            .lastOrNull()

        targetTheme?.let { selected ->
            baseTheme = selected
        }
    }

    /**
     * Permite cambiar el tema desde el menú de selección manual.
     */
    fun selectThemeManual(theme: GameTheme?) {
        // 🛡️ SEGURIDAD ORIGINAL: Solo permite cambiar manualmente si el nivel actual es suficiente
        if (theme != null && currentLevelTarget >= theme.minLevel) {
            baseTheme = theme
        }
    }

    /**
     * 🔥 NUEVA FUNCIÓN: selectThemeByIndex
     * Usada por el MainActivity para el Reto Diario.
     * Permite forzar un tema específico por su posición en la lista.
     */
    fun selectThemeByIndex(index: Int) {
        val themes = GameTheme.allThemes.filterNotNull()
        themes.getOrNull(index)?.let { selected ->
            baseTheme = selected
        }
    }
}

@Composable
fun ThemeSelector(viewModel: ThemeViewModel) {
    val themes = GameTheme.allThemes.filterNotNull()

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(themes) { theme ->
            // 🔒 LÓGICA DE BLOQUEO:
            // Un tema está "desbloqueado" si la meta del nivel actual es >= minLevel del tema
            val isUnlocked = viewModel.currentLevelTarget >= theme.minLevel
            val isSelected = viewModel.currentTheme == theme

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(64.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(theme.colors))
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) theme.accentColor else Color(0xFF3D405B).copy(alpha = 0.1f),
                            shape = CircleShape
                        )
                        // Si está bloqueado, se ve más opaco y no se puede clickear
                        .alpha(if (isUnlocked) 1f else 0.3f)
                        .clickable(enabled = isUnlocked) {
                            viewModel.selectThemeManual(theme)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Si está bloqueado, mostramos un candado muy pequeño y aesthetic
                    if (!isUnlocked) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF3D405B).copy(alpha = 0.5f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = theme.name,
                    color = Color(0xFF3D405B).copy(alpha = if (isUnlocked) 1f else 0.4f),
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}