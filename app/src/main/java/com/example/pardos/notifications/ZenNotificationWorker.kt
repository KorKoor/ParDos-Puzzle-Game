package com.korkoor.pardos.notifications

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters

/** Publica un aviso programado. Todo el aspecto vive en [PardosNotifier]. */
class ZenNotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val title = inputData.getString("title") ?: "ParDos: Zen Math"
        val message = inputData.getString("message") ?: "Es hora de jugar"
        val id = inputData.getInt("id", 0)
        val key = inputData.getString("key") ?: ""
        Log.d(TAG, "doWork id=$id key=$key title=$title")
        PardosNotifier.show(context, title, message, id, key)
        return Result.success()
    }

    private companion object { const val TAG = "ZenNotificationWorker" }
}
