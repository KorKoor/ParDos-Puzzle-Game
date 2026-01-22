package com.korkoor.pardos.ui.game.logic

import com.korkoor.pardos.domain.model.GameMode
import kotlinx.coroutines.*

class GameTimerManager(
    private val scope: CoroutineScope,
    private val onTick: (Long) -> Unit,
    private val onTimeUp: () -> Unit
) {
    private var timerJob: Job? = null

    /**
     * Inicia el cronómetro.
     * [onLowTime] es opcional para no romper llamadas existentes.
     */
    fun startTimer(
        mode: GameMode,
        initialTime: Long,
        onLowTime: ((Boolean) -> Unit)? = null
    ) {
        // Detenemos cualquier timer previo de forma segura
        stop()

        timerJob = scope.launch {
            var currentTime = initialTime

            while (isActive) {
                delay(1000)

                // MODO CUENTA REGRESIVA (Desafío / Rápido)
                if (mode == GameMode.DESAFIO || mode == GameMode.RAPIDO) {
                    currentTime--

                    // Feedback de tiempo bajo (ej: sacudida de pantalla o color rojo)
                    onLowTime?.invoke(currentTime in 1..10)

                    if (currentTime <= 0) {
                        onTick(0)
                        onTimeUp()
                        this.cancel() // Detener corrutina
                    } else {
                        onTick(currentTime)
                    }
                }
                // MODO CRONÓMETRO (Zen / Clásico / Tablas)
                else {
                    currentTime++
                    onTick(currentTime)
                }
            }
        }
    }

    fun stop() {
        timerJob?.cancel()
        timerJob = null
    }
}