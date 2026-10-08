package com.korkoor.pardos.ui.game

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.korkoor.pardos.R
import kotlin.random.Random

class SoundManager(context: Context) {
    private val soundPool: SoundPool
    private val betterPopId: Int
    private val winId: Int
    private val gameOverId: Int

    // Música de fondo
    private var menuMusicPlayer: MediaPlayer? = null
    private val settings = com.korkoor.pardos.data.local.SettingsManager(context)

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            // 🔥 MEJORA: Aumentamos canales simultáneos para que los sonidos rápidos no se corten
            .setMaxStreams(10)
            .setAudioAttributes(audioAttributes)
            .build()

        // Cargar efectos
        betterPopId = soundPool.load(context, R.raw.better_pop, 1)
        winId = soundPool.load(context, R.raw.win, 1)
        gameOverId = soundPool.load(context, R.raw.game_over, 1)
    }

    // --- MÚSICA DE MENÚ ---
    fun playMenuMusic(context: Context) {
        if (!settings.musicEnabled.value) return
        try {
            if (menuMusicPlayer == null) {
                menuMusicPlayer = MediaPlayer.create(context, R.raw.theme_song)
                menuMusicPlayer?.isLooping = true
                menuMusicPlayer?.setVolume(0.5f, 0.5f)
            }

            if (menuMusicPlayer?.isPlaying == false) {
                menuMusicPlayer?.start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopMenuMusic() {
        try {
            if (menuMusicPlayer?.isPlaying == true) {
                menuMusicPlayer?.pause()
                // Opcional: seekTo(0) si quieres que reinicie siempre desde el principio
                // menuMusicPlayer?.seekTo(0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- EFECTOS DE SONIDO CON PITCH (JUICE 🧃) ---

    /**
     * Reproduce el sonido de pop.
     * @param combo El número de combo actual (0, 1, 2, 3...).
     * Entre más alto, más agudo el sonido.
     */
    fun playBetterPop(combo: Int = 0) {
        // 1. Variación Aleatoria: Pequeña fluctuación para que no suene robótico
        val variance = Random.nextFloat() * 0.1f - 0.05f // +/- 0.05f

        // 2. Escala de Combo: Aumenta 0.1 de pitch por cada nivel de combo
        // Limitamos el combo boost a 1.0 extra (para que no se vuelva chillón e inaudible)
        val comboBoost = (combo * 0.1f).coerceAtMost(1.0f)

        // 3. Pitch Final: Base (1.0) + Combo + Variación
        // CoerceIn asegura que no baje de 0.5 (lento) ni suba de 2.0 (doble velocidad/chipmunk)
        val finalPitch = (1.0f + comboBoost + variance).coerceIn(0.8f, 2.0f)

        if (!settings.soundEnabled.value) return
        // Reproducir con el pitch calculado
        soundPool.play(betterPopId, 1f, 1f, 1, 0, finalPitch)
    }

    fun playWin() {
        stopMenuMusic()
        if (!settings.soundEnabled.value) return
        soundPool.play(winId, 1f, 1f, 1, 0, 1.0f)
    }

    fun playGameOver() {
        stopMenuMusic()
        if (!settings.soundEnabled.value) return
        // Volumen reducido al 20% como pediste
        // Pitch bajado ligeramente (0.9f) para que suene más triste/pesado
        soundPool.play(gameOverId, 0.2f, 0.2f, 1, 0, 0.9f)
    }

    fun release() {
        try {
            menuMusicPlayer?.stop()
            menuMusicPlayer?.release()
            menuMusicPlayer = null
            soundPool.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}