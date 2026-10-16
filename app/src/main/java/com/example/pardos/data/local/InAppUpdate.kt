package com.korkoor.pardos.data.local

import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

/**
 * Avisa a quien tiene una versión vieja de que hay una nueva, con la ventana oficial de Google Play (nada de pantallas propias).
 * Actualización flexible (se descarga mientras juegas) y, si en Play Console la versión se marca con prioridad alta (4 o 5), inmediata.
 * Se crea en onCreate y se comprueba en onResume; fuera de Play (instalación manual, depuración) simplemente no hace nada.
 */
class InAppUpdate(private val activity: ComponentActivity) {
    private val manager = AppUpdateManagerFactory.create(activity)
    private val launcher = activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        Log.d(TAG, "Resultado de la actualización: ${result.resultCode}")
    }
    private val prefs = activity.getSharedPreferences("pardos_update", android.content.Context.MODE_PRIVATE)

    fun check() {
        try {
            manager.appUpdateInfo.addOnSuccessListener { info ->
                when {
                    // Ya descargada mientras jugabas: se instala y la app se reinicia sola
                    info.installStatus() == InstallStatus.DOWNLOADED -> finishFlexible()
                    // Una inmediata que quedó a medias se retoma
                    info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS ->
                        start(info, AppUpdateType.IMMEDIATE)
                    info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE -> {
                        val urgent = info.updatePriority() >= URGENT_PRIORITY && info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                        if (urgent) start(info, AppUpdateType.IMMEDIATE)
                        else if (info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) && canNagToday()) start(info, AppUpdateType.FLEXIBLE)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Actualización: ${e.message}")
        }
    }

    /** La flexible se ofrece como mucho una vez al día para no cansar. */
    private fun canNagToday(): Boolean {
        val today = LocalDay.today()
        if (prefs.getInt(K_LAST_DAY, Int.MIN_VALUE) == today) return false
        prefs.edit().putInt(K_LAST_DAY, today).apply()
        return true
    }

    private fun start(info: AppUpdateInfo, type: Int) {
        try {
            if (type == AppUpdateType.FLEXIBLE) {
                manager.registerListener(object : InstallStateUpdatedListener {
                    override fun onStateUpdate(state: InstallState) {
                        if (state.installStatus() == InstallStatus.DOWNLOADED) {
                            manager.unregisterListener(this)
                            finishFlexible()
                        }
                    }
                })
            }
            manager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.newBuilder(type).build())
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo iniciar la actualización: ${e.message}")
        }
    }

    private fun finishFlexible() {
        Toast.makeText(activity, "Actualización lista: reiniciando ParDos…", Toast.LENGTH_SHORT).show()
        manager.completeUpdate()
    }

    private companion object {
        const val TAG = "InAppUpdate"
        const val K_LAST_DAY = "last_nag_day"
        const val URGENT_PRIORITY = 4
    }
}
