package com.example.pardos.domain.logic

import kotlin.math.pow

object ProgressionEngine {
    const val initialSize = 3
    const val initialTarget = 16

    fun calculateTargetForLevel(level: Int): Int {
        val exponent = ((level - 1) / 2) + 6

        return 2.0.pow(exponent.toDouble()).toInt().coerceAtMost(131072)
    }

    fun calculateBoardSize(target: Int): Int {
        return when {
            target <= 64 -> 3
            target <= 256 -> 3
            target <= 2048 -> 4
            target <= 8192 -> 5
            else -> 6
        }
    }
    fun calculateTimeLimitForTarget(target: Int): Long {
        return when {
            target <= 64   -> 60L  // 1 min (Rápido, es fácil)
            target <= 128  -> 90L  // 1.5 min
            target <= 256  -> 120L // 2 min
            target <= 512  -> 180L // 3 min
            target <= 1024 -> 300L // 5 min
            target <= 2048 -> 420L // 7 min
            else           -> 600L // 10 min
        }
    }
    fun calculateStars(timeRemaining: Long, totalTime: Long): Int {
        if (totalTime <= 0L) return 3

        val percentage = timeRemaining.toFloat() / totalTime.toFloat()

        return when {
            percentage >= 0.50f -> 3 // Bajé un poco la exigencia para las 3 estrellas
            percentage >= 0.20f -> 2
            else -> 1
        }
    }

    fun calculateTimeLimitForLevel(level: Int): Long {
        val target = calculateTargetForLevel(level)
        return calculateTimeLimitForTarget(target)
    }

    fun calculateFourProbability(level: Int): Double {
        val baseProb = 0.10
        val maxProb = 0.30
        return (baseProb + (level * 0.02)).coerceAtMost(maxProb)
    }

    fun getScoreMultiplier(level: Int): Float {
        return 1.0f + (level - 1) * 0.5f
    }

    fun calculateNextTarget(currentTarget: Int): Int {
        return currentTarget * 2
    }
}