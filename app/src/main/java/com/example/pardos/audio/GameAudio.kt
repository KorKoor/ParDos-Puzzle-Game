package com.korkoor.pardos.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.korkoor.pardos.R
import com.korkoor.pardos.data.local.EconomyManager
import com.korkoor.pardos.data.local.SettingsManager
import com.korkoor.pardos.domain.logic.Direction
import com.korkoor.pardos.domain.shop.MergeFx
import com.korkoor.pardos.ui.design.Season
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Todo el sonido de ParDos en un solo lugar.
 *
 *  - Efectos (SoundPool): interfaz, tablero, combos, flow, jefes, recompensas… Las fusiones son notas de una escala pentatónica
 *    (una por nivel de ficha) y suenan con la "voz" del efecto de fusión que lleve el jugador.
 *  - Música ([music]): la melodía del menú y música adaptativa por capas durante el juego.
 *
 * Se usa desde cualquier pantalla: `GameAudio.play(Sfx.COIN)`.
 */
object GameAudio {
    private const val TAG = "GameAudio"

    private var app: Context? = null
    private var pool: SoundPool? = null
    private val ids = HashMap<String, Int>()
    private val ready = HashSet<Int>()
    private val lastPlayed = HashMap<Any, Long>()
    /** Cuándo sonó por última vez algo que no sea un tic de fondo (para que el toque genérico no se duplique). */
    @Volatile private var lastAny = 0L
    private val main = Handler(Looper.getMainLooper())
    private var settings: SettingsManager? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Volatile private var voice = "marimba"
    private val loadedVoices = HashSet<String>()

    /** Música del menú y del juego. */
    val music: MusicDirector get() = MusicDirector

    /** Se llama una vez al abrir la app (es seguro repetirlo). */
    fun init(context: Context) {
        if (pool != null) return
        val ctx = context.applicationContext
        app = ctx
        val st = SettingsManager(ctx)
        settings = st
        val p = SoundPool.Builder()
            .setMaxStreams(16)
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
            )
            .build()
        p.setOnLoadCompleteListener { _, id, status ->
            if (status == 0) synchronized(ready) { ready.add(id) } else Log.w(TAG, "No cargó el sonido $id (estado $status)")
        }
        pool = p
        Sfx.entries.forEach { load(it.file) }
        MusicDirector.init(ctx, st)

        // La voz de las fusiones sigue al efecto de fusión equipado
        val eco = EconomyManager(ctx)
        setVoice(eco.equippedFx.value)
        scope.launch { eco.equippedFx.collect { setVoice(it) } }
    }

    private fun load(file: String): Int? {
        val ctx = app ?: return null
        ids[file]?.let { return it }
        val rid = ctx.resources.getIdentifier(file, "raw", ctx.packageName)
        if (rid == 0) { Log.w(TAG, "Falta el sonido $file"); return null }
        val id = pool?.load(ctx, rid, 1) ?: return null
        ids[file] = id
        return id
    }

    private fun isReady(id: Int): Boolean = synchronized(ready) { id in ready }

    /** Cambia la voz de las fusiones (se cargan sus 12 notas la primera vez). */
    fun setVoice(fx: MergeFx) {
        val v = MergeVoices.voiceFor(fx)
        voice = v
        if (loadedVoices.add(v)) {
            for (l in 1..MergeVoices.LEVELS) load(MergeVoices.file(v, l))
        }
    }

    private val sfxOn: Boolean get() = settings?.soundEnabled?.value ?: true
    private val sfxVolume: Float get() = settings?.sfxVolume?.value ?: 0.9f

    private fun playFile(file: String, volume: Float, rate: Float, pan: Float, priority: Int): Boolean {
        if (com.korkoor.pardos.BuildConfig.DEBUG) Log.v(TAG, "sfx $file vol=${"%.2f".format(volume)} rate=${"%.2f".format(rate)} pan=${"%.2f".format(pan)}")
        val p = pool ?: return false
        val id = ids[file] ?: return false
        if (!sfxOn || !isReady(id)) return false
        val v = (volume * sfxVolume).coerceIn(0f, 1f)
        if (v <= 0.001f) return false
        val l = if (pan > 0f) v * (1f - pan) else v
        val r = if (pan < 0f) v * (1f + pan) else v
        p.play(id, l, r, priority, 0, rate.coerceIn(0.5f, 2f))
        return true
    }

    // ---------------------------------------------------------------- efectos sueltos

    /** Reproduce un efecto. [rate] cambia el tono (1 = normal) y [pan] va de -1 (izquierda) a 1 (derecha). */
    fun play(s: Sfx, volume: Float = 1f, rate: Float = 1f, pan: Float = 0f) {
        if (pool == null) return
        if (s.minGapMs > 0) {
            val now = System.currentTimeMillis()
            val last = lastPlayed[s] ?: 0L
            if (now - last < s.minGapMs) return
            lastPlayed[s] = now
        }
        if (s != Sfx.SWIPE && s != Sfx.SPAWN && s != Sfx.XP_TICK && s != Sfx.WHEEL_TICK && s != Sfx.UI_TICK) lastAny = System.currentTimeMillis()
        playFile(s.file, volume * s.gain, rate, pan, s.priority)
    }

    fun playLater(s: Sfx, delayMs: Long, volume: Float = 1f, rate: Float = 1f, pan: Float = 0f) {
        if (delayMs <= 0) play(s, volume, rate, pan) else main.postDelayed({ play(s, volume, rate, pan) }, delayMs)
    }

    /** Toque de interfaz. */
    fun tap() = play(Sfx.UI_TAP, rate = 1f + (Random.nextFloat() - 0.5f) * 0.06f)

    /** Toque genérico de la app: se calla si el propio botón acaba de reproducir otro sonido. */
    fun tapIfQuiet() {
        if (System.currentTimeMillis() - lastAny < 130) return
        tap()
    }

    // ---------------------------------------------------------------- tablero

    /** Deslizar: un soplo suave que se va hacia el lado del movimiento. */
    fun swipe(dir: Direction) {
        val pan = when (dir) { Direction.LEFT -> -0.55f; Direction.RIGHT -> 0.55f; else -> 0f }
        play(Sfx.SWIPE, 0.8f, 0.94f + Random.nextFloat() * 0.12f, pan)
    }

    /** Una nota de fusión del nivel [level] con la voz actual. */
    fun mergeNote(level: Int, volume: Float = 1f, delayMs: Long = 0L, pan: Float = 0f) {
        if (pool == null) return
        val file = MergeVoices.file(voice, level)
        if (delayMs <= 0) playFile(file, volume * 0.95f, 1f, pan, 2)
        else main.postDelayed({ playFile(file, volume * 0.95f, 1f, pan, 2) }, delayMs)
    }

    /**
     * Las fusiones de una jugada suenan como un arpegio ascendente (de la ficha más chica a la más grande), una nota cada 55 ms.
     * Las fichas grandes suman un golpe grave y destellos. [values] son los valores de las fichas que se formaron.
     */
    fun merges(values: List<Int>, combo: Int) {
        if (values.isEmpty() || pool == null) return
        val levels = values.map { MergeVoices.levelOf(it) }.sorted()
        val distinct = levels.distinct().takeLast(5)
        distinct.forEachIndexed { i, lv ->
            val count = levels.count { it == lv }
            val vol = (0.78f + 0.035f * lv + 0.04f * i + if (count > 1) 0.08f else 0f).coerceAtMost(1f)
            mergeNote(lv, vol, delayMs = i * 55L)
        }
        val top = levels.last()
        if (top >= 6) play(Sfx.MERGE_SUB, ((top - 5) / 7f + 0.35f).coerceAtMost(1f))
        if (top >= 8) play(Sfx.MERGE_SPARKLE, 0.7f)
        combo(combo)
    }

    /** Racha de fusiones seguidas (cada una dentro de 1 s de la anterior). */
    fun combo(count: Int) {
        if (count < 2) return
        play(Sfx.COMBO_SPARK, 0.55f + 0.03f * count.coerceAtMost(10), 1f + 0.035f * count.coerceAtMost(12))
        when (count) {
            3 -> playLater(Sfx.COMBO_1, 120)
            5 -> playLater(Sfx.COMBO_2, 120)
            8 -> playLater(Sfx.COMBO_3, 120)
            12 -> playLater(Sfx.COMBO_4, 120)
        }
    }

    /** Sube de nivel el ritmo del tablero (0 = calma, 1 = en racha, 2 = imparable, 3 = ¡FLOW!). */
    fun flowTier(tier: Int) {
        when (tier) {
            1 -> play(Sfx.FLOW_TIER, 0.8f, 0.92f)
            2 -> play(Sfx.FLOW_TIER, 0.95f, 1.06f)
            3 -> play(Sfx.FLOW_IN)
        }
    }

    fun flowOut() = play(Sfx.FLOW_OUT)

    /** Primera vez que aparece una ficha grande en el nivel. */
    fun milestone(value: Int) {
        when {
            value >= 2048 -> play(Sfx.MILESTONE_BIG)
            value >= 512 -> play(Sfx.MILESTONE_MID, rate = if (value >= 1024) 1.05f else 1f)
            value >= 128 -> play(Sfx.MILESTONE_SMALL, rate = if (value >= 256) 1.06f else 1f)
        }
    }

    /** Las estrellas del resumen suenan una por una, cada una más alta. */
    fun stars(count: Int, startDelayMs: Long = 0L, stepMs: Long = 420L) {
        for (i in 1..count.coerceIn(0, 3)) {
            playLater(when (i) { 1 -> Sfx.STAR_1; 2 -> Sfx.STAR_2; else -> Sfx.STAR_3 }, startDelayMs + (i - 1) * stepMs)
        }
    }

    /** Carta que se revela, según su rareza (0 común … 3 legendaria). */
    fun card(rarityOrdinal: Int, isNew: Boolean) {
        play(Sfx.CARD_FLIP)
        val s = when (rarityOrdinal) { 0 -> Sfx.CARD_COMMON; 1 -> Sfx.CARD_RARE; 2 -> Sfx.CARD_EPIC; else -> Sfx.CARD_LEGEND }
        playLater(s, 90)
        if (isNew) playLater(Sfx.CARD_NEW, 420)
    }

    // ---------------------------------------------------------------- ciclo de vida

    fun onAppPause() = MusicDirector.suspend()

    fun onAppResume() = MusicDirector.unsuspend()
}

/**
 * Decide qué música suena y con cuánta intensidad.
 *
 *  - Menú: la melodía del menú (en Noche de brujas, la pieza "halloween").
 *  - Juego: una pieza adaptativa por capas; sube de intensidad con las jugadas seguidas (FLOW) y en los jefes.
 *
 * Respeta al jugador: si ya está escuchando su propia música, no la pisa (solo suenan los efectos).
 */
object MusicDirector {
    private const val TAG = "MusicDirector"

    enum class Scene { NONE, MENU, GAME }

    private var app: Context? = null
    private var settings: SettingsManager? = null
    private val main = Handler(Looper.getMainLooper())
    private var engine: AdaptiveMusic? = null
    private var legacy: MediaPlayer? = null
    private var legacyLevel = 0f
    private var legacyTarget = 0f
    private var ticking = false

    private var scene = Scene.NONE
    private var set = "zen"
    private var level = 0
    private var suspended = false
    private var focusLost = false
    private var duckFactor = 1f
    private var duckRestore: Runnable? = null
    private var audioManager: AudioManager? = null
    private var focusListener: AudioManager.OnAudioFocusChangeListener? = null
    private var focusRequest: Any? = null
    private var haveFocus = false

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun init(ctx: Context, st: SettingsManager) {
        if (app != null) return
        app = ctx
        settings = st
        audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        scope.launch { st.musicEnabled.collect { apply() } }
        scope.launch { st.musicVolume.collect { applyVolume() } }
    }

    // ---------------------------------------------------------------- pedidos de la app

    /** Menús y pantallas fuera del tablero. */
    fun menu() {
        scene = Scene.MENU
        level = 0
        apply()
        // la pieza del juego se deja lista para que al empezar un nivel la música entre sin espera
        val st = settings
        val ctx = app
        if (ctx != null && st != null && st.musicEnabled.value && !Season.halloween) ensureEngine(ctx).preload(set)
    }

    /** Durante una partida. [set] es la pieza (zen, dusk, deep, boss, halloween). */
    fun game(set: String) {
        this.set = set
        scene = Scene.GAME
        level = 0
        apply()
    }

    /** Intensidad 0..3: 0 solo la base, 1 +ritmo, 2 +melodía, 3 todo al máximo. */
    fun intensity(l: Int) {
        val v = l.coerceIn(0, 3)
        if (v == level) return
        level = v
        engine?.setLayers(layersFor(v), if (v > 0) 1.1f else 2.0f)
    }

    /** Baja la música un momento (para que se oiga una fanfarria) y la devuelve sola. */
    fun duck(factor: Float, ms: Long) {
        duckRestore?.let { main.removeCallbacks(it) }
        duckFactor = factor
        engine?.setDuck(factor)
        applyLegacyTarget()
        val r = Runnable { duckFactor = 1f; engine?.setDuck(1f); applyLegacyTarget() }
        duckRestore = r
        main.postDelayed(r, ms)
    }

    /** Cambia la pieza del juego: dentro de una partida suena ya; fuera, se usará al entrar. */
    fun switchGameSet(newSet: String) {
        set = newSet
        if (scene == Scene.GAME) { level = 0; apply() }
    }

    fun suspend() {
        suspended = true
        engine?.pause()
        try { legacy?.takeIf { it.isPlaying }?.pause() } catch (_: Exception) { }
    }

    fun unsuspend() {
        if (!suspended) return
        suspended = false
        apply()
    }

    // ---------------------------------------------------------------- aplicar el estado deseado

    private fun layersFor(l: Int): FloatArray = when (l) {
        0 -> floatArrayOf(1f, 0f, 0f)
        1 -> floatArrayOf(1f, 0.6f, 0f)
        2 -> floatArrayOf(1f, 0.9f, 0.55f)
        else -> floatArrayOf(1f, 1f, 0.9f)
    }

    private fun volume(): Float = (settings?.musicVolume?.value ?: 0.7f)

    private fun applyVolume() {
        engine?.setMaster(volume() * 0.9f)
        applyLegacyTarget()
    }

    private fun apply() {
        val st = settings ?: return
        val ctx = app ?: return
        if (suspended || focusLost) return
        if (!st.musicEnabled.value || scene == Scene.NONE) {
            engine?.stop(0.8f)
            setLegacyTarget(0f)
            return
        }
        // Si el jugador ya está escuchando su propia música, se la respetamos
        val ours = engine?.isActive == true || legacy?.isPlaying == true
        if (!ours && audioManager?.isMusicActive == true) {
            Log.d(TAG, "Hay música de otra app: solo efectos")
            return
        }
        requestFocus()
        if (com.korkoor.pardos.BuildConfig.DEBUG) Log.d(TAG, "música: escena=$scene pieza=$set intensidad=$level")
        when (scene) {
            Scene.MENU -> {
                if (Season.halloween) {
                    ensureEngine(ctx).play("halloween", floatArrayOf(1f, 0.35f, 0.2f))
                    setLegacyTarget(0f)
                } else {
                    engine?.stop(1.2f)
                    ensureLegacy(ctx)
                    setLegacyTarget(1f)
                }
            }
            Scene.GAME -> {
                ensureEngine(ctx).play(set, layersFor(level))
                setLegacyTarget(0f)
            }
            Scene.NONE -> Unit
        }
        applyVolume()
    }

    private fun ensureEngine(ctx: Context): AdaptiveMusic {
        engine?.let { return it }
        return AdaptiveMusic(ctx).also {
            it.setMaster(volume() * 0.9f)
            it.setDuck(duckFactor)
            engine = it
        }
    }

    // ---------------------------------------------------------------- melodía clásica del menú (MediaPlayer)

    private fun ensureLegacy(ctx: Context) {
        if (legacy != null) return
        try {
            legacy = MediaPlayer.create(ctx, R.raw.theme_song)?.apply {
                isLooping = true
                setVolume(0f, 0f)
            }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo abrir la melodía del menú", e)
        }
    }

    private fun setLegacyTarget(t: Float) {
        legacyTarget = t
        applyLegacyTarget()
    }

    private fun applyLegacyTarget() {
        val p = legacy ?: return
        if (legacyTarget > 0f && !suspended) {
            try { if (!p.isPlaying) p.start() } catch (_: Exception) { }
        }
        if (!ticking) {
            ticking = true
            main.post(object : Runnable {
                override fun run() {
                    val goal = legacyTarget * duckFactor * volume() * 0.5f
                    legacyLevel += (goal - legacyLevel) * 0.12f
                    if (kotlin.math.abs(goal - legacyLevel) < 0.002f) legacyLevel = goal
                    try { p.setVolume(legacyLevel, legacyLevel) } catch (_: Exception) { }
                    if (legacyLevel == goal) {
                        ticking = false
                        if (goal == 0f) try { if (p.isPlaying) p.pause() } catch (_: Exception) { }
                        return
                    }
                    main.postDelayed(this, 40)
                }
            })
        }
    }

    // ---------------------------------------------------------------- foco de audio (cortesía con otras apps)

    private fun requestFocus() {
        val am = audioManager ?: return
        if (haveFocus) return
        val listener = focusListener ?: AudioManager.OnAudioFocusChangeListener { change ->
            when (change) {
                AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    focusLost = true
                    haveFocus = false
                    engine?.pause()
                    try { legacy?.takeIf { it.isPlaying }?.pause() } catch (_: Exception) { }
                }
                AudioManager.AUDIOFOCUS_GAIN -> {
                    if (focusLost) { focusLost = false; apply(); engine?.resume() }
                }
            }
        }.also { focusListener = it }
        val result = if (Build.VERSION.SDK_INT >= 26) {
            val req = android.media.AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setOnAudioFocusChangeListener(listener)
                .build()
            focusRequest = req
            am.requestAudioFocus(req)
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(listener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
        haveFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }
}
