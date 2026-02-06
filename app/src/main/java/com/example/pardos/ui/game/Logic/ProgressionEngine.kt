package com.korkoor.pardos.domain.logic

import kotlin.math.pow
import kotlin.random.Random

object ProgressionEngine {
    const val initialSize = 3
    const val initialTarget = 16

    /** * 📈 PROGRESIÓN SUAVIZADA (NO EXPONENENCIAL)
     * Ahora los niveles no duplican la meta de golpe en cada paso.
     * Esto permite tener muchos más niveles por cada tamaño de tablero.
     */
    fun calculateTargetForLevel(level: Int): Int {
        return when {
            level <= 2  -> 32
            level <= 5  -> 64
            level <= 10 -> 128
            level <= 18 -> 256
            level <= 28 -> 512
            level <= 45 -> 1024
            level <= 70 -> 2048
            else -> 4096
        }
    }

    /**
     * 🧩 BALANCE DE ESPACIO (DENSIDAD)
     * Se mantiene el 3x3 hasta el 128 por petición.
     * El tablero 4x4 ahora es el protagonista principal durante gran parte de la campaña.
     */
    fun calculateBoardSize(target: Int): Int {
        return when {
            target <= 128  -> 3   // Niveles 1 al 10 aprox.
            target <= 4096 -> 4   // El grueso de la campaña (Niveles 11 al 70+)
            target <= 16384 -> 5
            else           -> 6
        }
    }

    /**
     * 🛡️ SISTEMA DE TIEMPO (ZERO PRESSURE)
     * Los niveles de campaña devuelven null para desactivar la derrota por tiempo.
     */
    fun calculateTimeLimitForTarget(target: Int, isCampaign: Boolean = true): Long? {
        if (isCampaign) return null // Sin límite en campaña

        val seconds = when {
            target <= 64   -> 180L  // 3 min
            target <= 128  -> 360L  // 6 min (Compensa el 3x3)
            target <= 512  -> 600L  // 10 min
            target <= 2048 -> 900L  // 15 min
            else           -> 1200L // 20 min
        }
        return seconds * 1000L
    }

    /**
     * ✨ AYUDA DIVINA BALANCEADA (ANTI-BLOQUEO)
     * Probabilidad ajustada para no regalar el juego pero evitar el atasco.
     */
    fun shouldTriggerDivineHelp(target: Int): Boolean {
        val probability = when {
            target >= 2048 -> 0.25 // 25% en niveles épicos
            target >= 512  -> 0.18 // 18% en niveles difíciles
            else           -> 0.12 // 12% base
        }
        return Random.nextDouble() < probability
    }

    /**
     * 🎲 GENERACIÓN DE FICHAS INTELIGENTE (EL "MERO" BALANCE)
     * Ayuda a que los niveles con metas altas no sean eternos.
     */
    fun getNewTileValue(target: Int): Int {
        val rand = Random.nextDouble()
        return when {
            target >= 2048 && rand < 0.04 -> 16
            target >= 1024 && rand < 0.06 -> 8
            rand < calculateFourProbabilityForTarget(target) -> 4
            else -> 2
        }
    }

    /**
     * Evita que el 3x3 se llene de valores distintos que no se pueden combinar.
     */
    private fun calculateFourProbabilityForTarget(target: Int): Double {
        return if (target <= 128) 0.06 else 0.15
    }

    /**
     * ⭐ ESTRELLAS POR EFICIENCIA
     * Basado en un tiempo "ideal" de 5 minutos para 3 estrellas.
     */
    fun calculateStars(timeElapsed: Long, target: Int): Int {
        val idealTimeMs = 300000L // 5 minutos
        return when {
            timeElapsed <= idealTimeMs * 0.7 -> 3
            timeElapsed <= idealTimeMs -> 2
            else -> 1
        }
    }

    // Funciones de utilidad
    fun calculateTimeLimitForLevel(level: Int): Long? =
        calculateTimeLimitForTarget(calculateTargetForLevel(level), true)

    fun getScoreMultiplier(level: Int): Float = 1.0f + (level - 1) * 0.2f

    fun calculateNextTarget(currentTarget: Int): Int = currentTarget * 2
}