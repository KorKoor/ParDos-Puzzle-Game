package com.korkoor.pardos.domain.logic

import kotlin.math.pow
import kotlin.random.Random

object ProgressionEngine {
    const val initialSize = 3
    const val initialTarget = 16

    /** * 📈 PROGRESIÓN SUAVIZADA (NO EXPONENENCIAL)
     * Ahora los niveles no duplican la meta de golpe en cada paso.
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
     */
    fun calculateBoardSize(target: Int): Int {
        return when {
            target <= 128  -> 3
            target <= 4096 -> 4
            target <= 16384 -> 5
            else           -> 6
        }
    }

    /**
     * 🛡️ SISTEMA DE TIEMPO (ZERO PRESSURE)
     * Los niveles de campaña devuelven null para desactivar la derrota por tiempo.
     */
    fun calculateTimeLimitForTarget(target: Int, isCampaign: Boolean = true): Long? {
        if (isCampaign) return null

        val seconds = when {
            target <= 64   -> 180L
            target <= 128  -> 360L
            target <= 512  -> 600L
            target <= 2048 -> 900L
            else           -> 1200L
        }
        return seconds * 1000L
    }

    /**
     * ✨ AYUDA DIVINA BALANCEADA (ANTI-BLOQUEO)
     * Reducido drásticamente para que sea "de vez en cuando".
     */
    fun shouldTriggerDivineHelp(target: Int): Boolean {
        val probability = when {
            target >= 2048 -> 0.08 // 8% en niveles épicos (antes 25%)
            target >= 512  -> 0.05 // 5% en niveles difíciles
            else           -> 0.03 // 3% base (Muy ocasional)
        }
        return Random.nextDouble() < probability
    }

    /**
     * 🎯 FILTRO DE VALORES ELEGIBLES
     * Solo permite que 4, 8 y 16 evolucionen solos.
     */
    fun isValueEligibleForDivineHelp(value: Int): Boolean {
        return value == 4 || value == 8 || value == 16
    }

    /**
     * 🎲 GENERACIÓN DE FICHAS INTELIGENTE
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
     * Evita que el 3x3 se llene de valores distintos.
     */
    private fun calculateFourProbabilityForTarget(target: Int): Double {
        return if (target <= 128) 0.06 else 0.15
    }

    /**
     * ⭐ ESTRELLAS POR EFICIENCIA
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