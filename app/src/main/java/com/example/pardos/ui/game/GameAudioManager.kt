package com.korkoor.pardos.ui.game

import android.media.AudioAttributes
import android.media.SoundPool

// 🔥 OPTIMIZACIÓN GIGANTE: SoundPool en lugar de MediaPlayer para latencia 0 y menos RAM
internal class GameAudioManager(private val context: android.content.Context) {
    private var soundPool: SoundPool? = null
    private var moveSoundId: Int = 0
    private var victorySoundId: Int = 0
    private var loaded = false
    private val settings = com.korkoor.pardos.data.local.SettingsManager(context)

    fun initialize() {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(5) // Permite hasta 5 sonidos simultáneos sin crashear
                .setAudioAttributes(audioAttributes)
                .build()

            soundPool?.setOnLoadCompleteListener { _, _, status ->
                if (status == 0) loaded = true
            }

            moveSoundId = soundPool?.load(context, com.korkoor.pardos.R.raw.move_pop, 1) ?: 0
            victorySoundId = soundPool?.load(context, com.korkoor.pardos.R.raw.victory_sound, 1) ?: 0
        } catch (e: Exception) {
            android.util.Log.e("GameAudio", "Error initializing SoundPool", e)
        }
    }

    fun playMoveSound() {
        if (settings.soundEnabled.value && loaded && moveSoundId != 0) {
            soundPool?.play(moveSoundId, 0.7f, 0.7f, 1, 0, 1f)
        }
    }

    fun playVictorySound() {
        if (settings.soundEnabled.value && loaded && victorySoundId != 0) {
            soundPool?.play(victorySoundId, 0.8f, 0.8f, 1, 0, 1f)
        }
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        loaded = false
    }
}
