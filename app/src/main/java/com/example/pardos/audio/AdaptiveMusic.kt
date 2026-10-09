package com.korkoor.pardos.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Process
import android.util.Log
import java.nio.ByteOrder
import kotlin.math.exp
import kotlin.math.max

/**
 * Música adaptativa por capas.
 *
 * Cada pieza son 3 bucles estéreo de la MISMA duración (base, groove, lead; ver tools/audio/music.py). Aquí se decodifican a
 * memoria y un hilo los mezcla sample a sample con un solo AudioTrack: así las capas entran y salen siempre en compás, el
 * bucle no tiene huecos y el cambio de una pieza a otra es un fundido suave (las dos suenan un momento).
 */
class AdaptiveMusic(private val context: Context) {

    private class Loop(val name: String, val frames: Int, val layers: Array<ShortArray>)

    private class Voice(val loop: Loop, startGains: FloatArray) {
        var pos = 0
        val g = FloatArray(LAYERS).also { startGains.copyInto(it) }
        var gain = 0f
        @Volatile var target = 1f
        @Volatile var tau = 1.5f
    }

    private val lock = Object()
    private val voices = ArrayList<Voice>()
    /** Piezas ya decodificadas (como mucho 2): volver a una pieza no vuelve a decodificar. */
    private val cache = LinkedHashMap<String, Loop>()

    @Volatile private var layerTarget = floatArrayOf(1f, 0f, 0f)
    @Volatile private var layerTau = 1.2f
    @Volatile private var master = 0.7f
    @Volatile private var duck = 1f
    @Volatile private var paused = true
    @Volatile private var running = false
    @Volatile private var releasing = false

    private var track: AudioTrack? = null
    private var thread: Thread? = null
    @Volatile var currentSet: String? = null
        private set
    @Volatile private var loadingSet: String? = null

    /** Suena algo (o se está cargando)? */
    val isActive: Boolean get() = synchronized(lock) { voices.isNotEmpty() } || loadingSet != null

    // ---------------------------------------------------------------- control

    /**
     * Empieza la pieza [set] con las capas [layers] (0..1 cada una). Si ya es la que suena solo cambia las capas.
     * La carga se hace en segundo plano; mientras tanto sigue lo anterior.
     */
    fun play(set: String, layers: FloatArray, fadeSec: Float = 1.6f) {
        if (releasing) return
        layerTarget = layers.copyOf()
        if (set == currentSet || set == loadingSet) { resumeIfNeeded(); return }
        loadingSet = set
        Thread({
            val loop = loadCached(set)
            if (loop == null || releasing) { if (loadingSet == set) loadingSet = null; return@Thread }
            synchronized(lock) {
                // si mientras cargaba se pidió otra pieza, esta ya no hace falta
                if (loadingSet != set) return@Thread
                for (v in voices) { v.target = 0f; v.tau = max(0.2f, fadeSec / 3f) }
                val v = Voice(loop, layerTarget)
                v.tau = max(0.2f, fadeSec / 3f)
                v.target = 1f
                voices.add(v)
                currentSet = set
                loadingSet = null
                paused = false
                lock.notifyAll()
            }
            ensureThread()
        }, "music-load").start()
    }

    /** Decodifica una pieza en segundo plano para que luego arranque al instante. */
    fun preload(set: String) {
        if (releasing || synchronized(cache) { cache.containsKey(set) } || set == currentSet) return
        Thread({ loadCached(set) }, "music-preload").start()
    }

    private fun loadCached(set: String): Loop? {
        synchronized(cache) { cache[set]?.let { return it } }
        val loop = loadSet(set) ?: return null
        synchronized(cache) {
            cache[set] = loop
            while (cache.size > 2) { val k = cache.keys.first(); cache.remove(k) }
        }
        return loop
    }

    fun setLayers(layers: FloatArray, tauSec: Float = 1.2f) {
        layerTau = tauSec
        layerTarget = layers.copyOf()
    }

    fun setMaster(v: Float) { master = v.coerceIn(0f, 1f) }

    fun setDuck(v: Float) { duck = v.coerceIn(0f, 1f) }

    /** Apaga la música con un fundido. */
    fun stop(fadeSec: Float = 1.2f) {
        synchronized(lock) {
            for (v in voices) { v.target = 0f; v.tau = max(0.15f, fadeSec / 3f) }
            currentSet = null
            loadingSet = null
        }
    }

    fun pause() {
        paused = true
    }

    fun resume() {
        if (releasing) return
        synchronized(lock) { paused = false; lock.notifyAll() }
    }

    private fun resumeIfNeeded() {
        if (paused) resume()
    }

    fun release() {
        releasing = true
        running = false
        synchronized(lock) { paused = false; lock.notifyAll() }
        thread?.let { try { it.join(400) } catch (_: Exception) { } }
        try { track?.stop() } catch (_: Exception) { }
        try { track?.release() } catch (_: Exception) { }
        track = null
        synchronized(lock) { voices.clear() }
        synchronized(cache) { cache.clear() }
    }

    // ---------------------------------------------------------------- mezcla

    private fun ensureThread() {
        if (running) return
        running = true
        val bufBytes = max(AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT), CHUNK * 4 * 4)
        val t = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
                )
                .setAudioFormat(
                    AudioFormat.Builder().setSampleRate(SAMPLE_RATE).setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build()
                )
                .setBufferSizeInBytes(bufBytes)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo crear el AudioTrack", e)
            running = false
            return
        }
        track = t
        thread = Thread({ mixLoop(t) }, "music-mix").also { it.start() }
    }

    private fun mixLoop(t: AudioTrack) {
        try { Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO) } catch (_: Exception) { }
        val out = ShortArray(CHUNK * 2)
        val acc = FloatArray(CHUNK * 2)
        val ramp = FloatArray(CHUNK) { it / CHUNK.toFloat() }
        val chunkSec = CHUNK / SAMPLE_RATE.toFloat()
        var masterCur = 0f
        val s = FloatArray(LAYERS)
        val e = FloatArray(LAYERS)
        while (running) {
            synchronized(lock) {
                while (running && (paused || voices.isEmpty())) {
                    try { if (t.playState == AudioTrack.PLAYSTATE_PLAYING) t.pause() } catch (_: Exception) { }
                    try { lock.wait(500) } catch (_: InterruptedException) { }
                }
            }
            if (!running) break
            try { if (t.playState != AudioTrack.PLAYSTATE_PLAYING) t.play() } catch (_: Exception) { }

            java.util.Arrays.fill(acc, 0f)
            val lt = layerTarget
            val aL = 1f - exp(-chunkSec / layerTau)
            val mTarget = master * duck
            val m0 = masterCur
            val m1 = m0 + (mTarget - m0) * (1f - exp(-chunkSec / 0.12f))
            masterCur = m1

            synchronized(lock) {
                for (v in voices) {
                    val aV = 1f - exp(-chunkSec / v.tau)
                    val vg0 = v.gain
                    val vg1 = vg0 + (v.target - vg0) * aV
                    v.gain = vg1
                    for (l in 0 until LAYERS) {
                        val g0 = v.g[l]
                        val g1 = g0 + (lt[l] - g0) * aL
                        v.g[l] = g1
                        s[l] = g0 * vg0
                        e[l] = g1 * vg1
                    }
                    val frames = v.loop.frames
                    var p = v.pos
                    val l0 = v.loop.layers[0]; val l1 = v.loop.layers[1]; val l2 = v.loop.layers[2]
                    for (f in 0 until CHUNK) {
                        val k = ramp[f]
                        val g0 = s[0] + (e[0] - s[0]) * k
                        val g1 = s[1] + (e[1] - s[1]) * k
                        val g2 = s[2] + (e[2] - s[2]) * k
                        val i = p * 2
                        var l = l0[i] * g0
                        var r = l0[i + 1] * g0
                        if (g1 > 0.0004f) { l += l1[i] * g1; r += l1[i + 1] * g1 }
                        if (g2 > 0.0004f) { l += l2[i] * g2; r += l2[i + 1] * g2 }
                        acc[f * 2] += l
                        acc[f * 2 + 1] += r
                        p++
                        if (p >= frames) p = 0
                    }
                    v.pos = p
                }
                // se retiran las piezas que ya se apagaron del todo
                val it = voices.iterator()
                while (it.hasNext()) {
                    val v = it.next()
                    if (v.target == 0f && v.gain < 0.003f) it.remove()
                }
            }

            for (f in 0 until CHUNK) {
                val m = m0 + (m1 - m0) * ramp[f]
                val sl = (acc[f * 2] * m).coerceIn(-32767f, 32767f)
                val sr = (acc[f * 2 + 1] * m).coerceIn(-32767f, 32767f)
                out[f * 2] = sl.toInt().toShort()
                out[f * 2 + 1] = sr.toInt().toShort()
            }
            try { t.write(out, 0, out.size) } catch (_: Exception) { }
        }
    }

    // ---------------------------------------------------------------- carga (decodifica los .ogg a memoria)

    private fun loadSet(set: String): Loop? {
        val names = listOf("base", "groove", "lead")
        val layers = ArrayList<ShortArray>()
        for (n in names) {
            val id = context.resources.getIdentifier("music_${set}_$n", "raw", context.packageName)
            if (id == 0) { Log.w(TAG, "Falta la capa music_${set}_$n"); return null }
            val pcm = try { decode(id) } catch (e: Exception) { Log.e(TAG, "Error al decodificar music_${set}_$n", e); null } ?: return null
            layers.add(pcm)
        }
        // todas deben medir lo mismo; si no, se recorta a la más corta (nunca debería pasar)
        val frames = layers.minOf { it.size / 2 }
        if (layers.any { it.size / 2 != frames }) Log.w(TAG, "Capas de $set con largos distintos: ${layers.map { it.size / 2 }}")
        return Loop(set, frames, Array(LAYERS) { layers[it] })
    }

    private fun decode(resId: Int): ShortArray? {
        val afd = context.resources.openRawResourceFd(resId) ?: return null
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            val fmt = extractor.getTrackFormat(0)
            val mime = fmt.getString(MediaFormat.KEY_MIME) ?: return null
            extractor.selectTrack(0)
            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(fmt, null, null, 0)
            codec.start()

            var channels = if (fmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT) else 2
            val durUs = if (fmt.containsKey(MediaFormat.KEY_DURATION)) fmt.getLong(MediaFormat.KEY_DURATION) else 45_000_000L
            var buf = ShortArray(((durUs / 1_000_000.0) * SAMPLE_RATE * channels).toInt() + 8192)
            var n = 0
            val info = MediaCodec.BufferInfo()
            var inEos = false
            var outEos = false
            while (!outEos) {
                if (!inEos) {
                    val ii = codec.dequeueInputBuffer(10_000)
                    if (ii >= 0) {
                        val ib = codec.getInputBuffer(ii)!!
                        val size = extractor.readSampleData(ib, 0)
                        if (size < 0) {
                            codec.queueInputBuffer(ii, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inEos = true
                        } else {
                            codec.queueInputBuffer(ii, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val oi = codec.dequeueOutputBuffer(info, 10_000)
                when {
                    oi >= 0 -> {
                        val ob = codec.getOutputBuffer(oi)!!
                        ob.position(info.offset)
                        ob.limit(info.offset + info.size)
                        val sb = ob.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                        val cnt = sb.remaining()
                        if (n + cnt > buf.size) buf = buf.copyOf(max(buf.size * 2, n + cnt))
                        sb.get(buf, n, cnt)
                        n += cnt
                        codec.releaseOutputBuffer(oi, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outEos = true
                    }
                    oi == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val nf = codec.outputFormat
                        if (nf.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) channels = nf.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    }
                }
            }
            if (channels == 2) return buf.copyOf(n - (n % 2))
            // mono -> estéreo
            val frames = n / channels
            val st = ShortArray(frames * 2)
            for (f in 0 until frames) { st[f * 2] = buf[f * channels]; st[f * 2 + 1] = buf[f * channels] }
            return st
        } finally {
            try { codec?.stop() } catch (_: Exception) { }
            try { codec?.release() } catch (_: Exception) { }
            try { extractor.release() } catch (_: Exception) { }
            try { afd.close() } catch (_: Exception) { }
        }
    }

    companion object {
        private const val TAG = "AdaptiveMusic"
        const val LAYERS = 3
        const val SAMPLE_RATE = 32000
        private const val CHUNK = 1024
    }
}
