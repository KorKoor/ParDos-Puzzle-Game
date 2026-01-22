package com.example.pardos.domain.achievements

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.pardos.domain.model.BoardState

/**
 * Representa un logro dentro del juego.
 *
 * @param id Identificador único del logro (ej. "time_15").
 * @param titleResId Nombre visible del logro.
 * @param descriptionResId Texto descriptivo de la condición.
 * @param icon Ícono representativo (ImageVector de Material Icons).
 * @param color Color principal del logro.
 * @param isUnlocked Estado actual del logro (por defecto false).
 * @param condition Función que evalúa si el logro se cumple dado el estado del tablero.
 */
data class Achievement(
    val id: String,
    val titleResId: Int,       // 🔥 Cambiado de String a Int
    val descriptionResId: Int, // 🔥 Cambiado de String a Int
    val icon: ImageVector,
    val color: Color,
    val isUnlocked: Boolean = false,
    val condition: (BoardState) -> Boolean
)
