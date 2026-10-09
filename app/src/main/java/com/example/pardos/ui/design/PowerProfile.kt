package com.korkoor.pardos.ui.design

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.PowerManager
import androidx.annotation.RequiresApi
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat

/** Cuánta animación puede permitirse el teléfono ahora mismo. */
enum class PowerLevel { NORMAL, SAVER, HOT }

/**
 * Ahorro de energía y calor: con el ahorro de batería del sistema o cuando el teléfono se calienta, las animaciones decorativas
 * bajan de fotogramas (30 → 10 → 4 por segundo) y las partículas se reducen. Un solo registro para toda la app.
 */
object PowerProfile {
    val level = mutableStateOf(PowerLevel.NORMAL)

    /** Reloj único (milisegundos del sistema) que mueve todas las animaciones decorativas; lo actualiza [CozyClockProvider]. */
    val nowMs = mutableLongStateOf(0L)

    private var started = false
    private var powerSave = false
    private var thermal = 0

    fun start(context: Context) {
        if (started) return
        started = true
        val app = context.applicationContext
        val pm = app.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
        powerSave = pm.isPowerSaveMode
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                powerSave = pm.isPowerSaveMode
                recompute()
            }
        }
        try {
            ContextCompat.registerReceiver(app, receiver, IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
        } catch (e: Throwable) {
            // sin ahorro automático si el sistema no deja registrar el aviso
        }
        if (Build.VERSION.SDK_INT >= 29) Thermal29.register(pm) { status ->
            thermal = status
            recompute()
        }
        recompute()
    }

    private fun recompute() {
        // estados térmicos: 2 = moderado, 3 = severo (o peor)
        level.value = when {
            thermal >= 3 -> PowerLevel.HOT
            powerSave || thermal >= 2 -> PowerLevel.SAVER
            else -> PowerLevel.NORMAL
        }
    }

    /** Milisegundos entre fotogramas de las animaciones decorativas. */
    fun frameMs(): Long = when (level.value) {
        PowerLevel.NORMAL -> 33L
        PowerLevel.SAVER -> 100L
        PowerLevel.HOT -> 250L
    }

    /** Multiplicador de cuántas partículas se dibujan. */
    fun particleScale(): Float = when (level.value) {
        PowerLevel.NORMAL -> 1f
        PowerLevel.SAVER -> 0.5f
        PowerLevel.HOT -> 0.2f
    }

    @RequiresApi(29)
    private object Thermal29 {
        fun register(pm: PowerManager, onStatus: (Int) -> Unit) {
            pm.addThermalStatusListener { status -> onStatus(status) }
            onStatus(pm.currentThermalStatus)
        }
    }
}

/** Vuelta 0..1 de un ciclo de [periodMs] milisegundos, a los fotogramas que permite [PowerProfile] (y parada si la app no se ve). */
@androidx.compose.runtime.Composable
fun rememberThrottledPhase(periodMs: Int): State<Float> =
    remember(periodMs) { derivedStateOf { (PowerProfile.nowMs.longValue % periodMs).toFloat() / periodMs } }
