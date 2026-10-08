package com.korkoor.pardos.domain.model

import com.korkoor.pardos.domain.level.LevelKind
import com.korkoor.pardos.domain.level.LevelSpec

/**
 * Representa los datos de un nivel en el selector.
 * @param id El número del nivel.
 * @param target Ficha (o puntos) a alcanzar.
 * @param isLocked Indica si el nivel está bloqueado.
 * @param starsEarned Cantidad de estrellas obtenidas (0 a 3).
 * @param difficultyName Nombre del tipo de nivel (Zen, Piedras, Sprint...).
 * @param maxTime Tiempo límite en segundos (null si no hay reloj).
 * @param spec Definición completa del nivel (reglas, tablero, límites).
 */
data class LevelInfo(
    val id: Int,
    val target: Int,
    val isLocked: Boolean = true,
    val starsEarned: Int = 0,
    val bestTime: Long = 0L,
    val bestMoves: Int = 0,
    val difficultyName: String = "Zen",
    val maxTime: Long? = null,
    val spec: LevelSpec? = null
) {
    val kind: LevelKind get() = spec?.kind ?: LevelKind.ZEN
    val boardSize: Int get() = spec?.boardSize ?: 4
}
