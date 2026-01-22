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
    val isLocked: Boolean,
    val starsEarned: Int = 0,
    val difficultyName: String = "Normal",
    val maxTime: Long? = null
)