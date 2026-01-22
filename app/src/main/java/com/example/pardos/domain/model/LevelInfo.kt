package com.example.pardos.domain.model

/**
 * Representa los datos de un nivel en el selector.
 * @param id El número del nivel.
 * @param target El valor de ficha objetivo (ej: 2048, 4096).
 * @param isLocked Indica si el nivel está bloqueado.
 * @param starsEarned Cantidad de estrellas obtenidas (0 a 3).
 * @param difficultyName Nombre de la dificultad (Zen, Normal, Desafío).
 * @param maxTime Tiempo límite en segundos (null si es modo Zen).
 */

data class LevelInfo(
    val id: Int,
    val target: Int,
    val isLocked: Boolean = true,
    val starsEarned: Int = 0,
    // 🔥 AGREGAMOS ESTOS CAMPOS PARA ARREGLAR EL ERROR:
    val bestTime: Long = 0L,
    val bestMoves: Int = 0,
    // Mantenemos tus campos visuales:
    val difficultyName: String = "Normal",
    val maxTime: Long? = null
)