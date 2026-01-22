package com.example.pardos.domain.model

import com.example.pardos.domain.logic.ProgressionEngine
import com.example.pardos.domain.model.LevelInfo

object LevelRepository {
        fun getGeneratedLevels(): List<LevelInfo> {
            val totalLevels = 2400
            val levels = mutableListOf<LevelInfo>()

            for (i in 1..totalLevels) {
                // ✅ FIX: Usamos el Engine para que la meta coincida con la del juego real.
                // (Si el Engine dice que Nivel 7 es 512, aquí también será 512).
                val finalTarget = ProgressionEngine.calculateTargetForLevel(i)

                // ⏱️ TU LÓGICA DE TIEMPO (Se mantiene igual, estaba bien)
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
                        // ✅ Inicializamos los nuevos campos en 0
                        bestTime = 0L,
                        bestMoves = 0,
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