package com.example.pardos.domain.model

import com.example.pardos.domain.model.LevelInfo

object LevelRepository {

    fun getGeneratedLevels(): List<LevelInfo> {
        val totalLevels = 2400
        val levels = mutableListOf<LevelInfo>()

        for (i in 1..totalLevels) {
            // 📈 LÓGICA DE TARGET (Sube cada 10 niveles)
            // Nivel 1-10: 64, Nivel 11-20: 128, etc.
            val exponent = (i / 10) + 6
            val calculatedTarget = Math.pow(2.0, exponent.toDouble()).toInt()
            // Capamos el target máximo a 131,072 (la ficha más alta lógica en un tablero 4x4)
            val finalTarget = calculatedTarget.coerceAtMost(131072)

            // ⏱️ LÓGICA DE TIEMPO (Modo Desafío cada 5 niveles)
            val isChallenge = i % 5 == 0
            val maxTime = if (isChallenge) {
                // El tiempo empieza en 180s y baja 2s cada 10 niveles (mínimo 45s)
                (180L - (i / 10 * 2)).coerceAtMost(600L).coerceAtLeast(45L)
            } else null

            levels.add(
                LevelInfo(
                    id = i,
                    target = finalTarget,
                    isLocked = i > 1, // Solo el 1 empieza desbloqueado
                    starsEarned = 0,
                    difficultyName = when {
                        i % 25 == 0 -> "ÉPICO"
                        isChallenge -> "Desafío"
                        i % 2 == 0 -> "Normal"
                        else -> "Zen"
                    },
                    maxTime = maxTime
                )
            )
        }
        return levels
    }
}