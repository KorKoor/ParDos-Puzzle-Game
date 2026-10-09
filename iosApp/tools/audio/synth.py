"""
Sintetizador sencillo de ParDos (numpy). Genera efectos y musica sin muestras externas.
Todo es mono float32 en [-1, 1]. SR_SFX para efectos (mp3) y SR_MUSIC para musica (wav).
"""
import math
import wave

import numpy as np

SR_SFX = 44100
SR_MUSIC = 22050

NOTE_INDEX = {"C": 0, "C#": 1, "Db": 1, "D": 2, "D#": 3, "Eb": 3, "E": 4, "F": 5, "F#": 6, "Gb": 6, "G": 7,
              "G#": 8, "Ab": 8, "A": 9, "A#": 10, "Bb": 10, "B": 11}


def note(name):
    """'C4' -> Hz."""
    if name[1] in "#b":
        key, octave = name[:2], int(name[2:])
    else:
        key, octave = name[:1], int(name[1:])
    midi = 12 * (octave + 1) + NOTE_INDEX[key]
    return 440.0 * 2 ** ((midi - 69) / 12.0)


def midi_hz(m):
    return 440.0 * 2 ** ((m - 69) / 12.0)


# ------------------------------------------------------------------ instrumentos (sumas de parciales con decaimiento)

# (razon de frecuencia, amplitud, decaimiento en 1/s)
KALIMBA = [(1.0, 1.0, 5.5), (5.4, 0.28, 12.0), (9.0, 0.08, 20.0)]
MARIMBA = [(1.0, 1.0, 6.5), (4.0, 0.35, 16.0), (10.0, 0.08, 30.0)]
GLOCK = [(1.0, 1.0, 3.2), (2.76, 0.45, 5.5), (5.4, 0.25, 8.5), (8.93, 0.12, 12.0)]
BELL = [(1.0, 1.0, 2.0), (2.0, 0.55, 2.4), (2.4, 0.4, 3.0), (3.0, 0.3, 3.6), (4.1, 0.2, 4.6), (5.2, 0.12, 6.0)]
CHIME = [(1.0, 1.0, 2.6), (3.0, 0.4, 4.0), (5.0, 0.2, 6.0)]
WOOD = [(1.0, 1.0, 38.0), (2.3, 0.4, 55.0)]
PLUCK = [(1.0, 1.0, 7.0), (2.0, 0.45, 9.0), (3.0, 0.2, 12.0)]
CELESTA = [(1.0, 1.0, 4.5), (4.0, 0.3, 9.0), (6.0, 0.12, 14.0)]


def tone(freq, dur, parts, vel=1.0, sr=SR_SFX, attack=0.003, detune=0.0):
    n = int(dur * sr)
    t = np.arange(n) / sr
    out = np.zeros(n)
    for ratio, amp, decay in parts:
        f = freq * ratio * (1.0 + detune)
        if f > sr * 0.45:
            continue
        out += amp * np.sin(2 * np.pi * f * t) * np.exp(-decay * t)
    a = max(1, int(attack * sr))
    out[:a] *= np.linspace(0.0, 1.0, a)
    return (out * vel).astype(np.float32)


def pad(freq, dur, sr=SR_MUSIC, attack=1.2, release=1.4, vel=1.0, bright=0.5):
    """Colchon calido: parciales con ligera desafinacion, ataque y relevo lentos."""
    n = int(dur * sr)
    t = np.arange(n) / sr
    out = np.zeros(n)
    for k, amp in ((1, 1.0), (2, 0.5 * bright + 0.2), (3, 0.28 * bright), (4, 0.12 * bright), (6, 0.05 * bright)):
        for d in (-0.0035, 0.0, 0.0042):
            f = freq * k * (1 + d)
            if f < sr * 0.45:
                out += amp * np.sin(2 * np.pi * f * t + 0.7 * k + d * 40) / 3.0
    env = np.ones(n)
    a = min(n, int(attack * sr))
    r = min(n, int(release * sr))
    env[:a] = 0.5 - 0.5 * np.cos(np.linspace(0, np.pi, a))
    env[n - r:] *= 0.5 + 0.5 * np.cos(np.linspace(0, np.pi, r))
    return (out * env * vel * 0.5).astype(np.float32)


def bass(freq, dur, sr=SR_MUSIC, vel=1.0):
    n = int(dur * sr)
    t = np.arange(n) / sr
    out = np.sin(2 * np.pi * freq * t) + 0.5 * np.sin(2 * np.pi * freq * 2 * t) + 0.2 * np.sin(2 * np.pi * freq * 3 * t)
    env = np.exp(-2.2 * t)
    a = int(0.012 * sr)
    env[:a] *= np.linspace(0, 1, a)
    return (out * env * vel * 0.7).astype(np.float32)


def noise(dur, sr=SR_SFX, seed=1):
    rng = np.random.RandomState(seed)
    return rng.uniform(-1, 1, int(dur * sr)).astype(np.float32)


def lowpass(x, hz, sr=SR_SFX):
    a = math.exp(-2 * math.pi * hz / sr)
    out = np.empty_like(x)
    acc = 0.0
    for i in range(len(x)):
        acc = (1 - a) * x[i] + a * acc
        out[i] = acc
    return out


def fast_lowpass(x, hz, sr=SR_SFX):
    """Filtro en el dominio de la frecuencia (rapido): pendiente suave."""
    spec = np.fft.rfft(x)
    freqs = np.fft.rfftfreq(len(x), 1.0 / sr)
    gain = 1.0 / np.sqrt(1.0 + (freqs / hz) ** 4)
    return np.fft.irfft(spec * gain, len(x)).astype(np.float32)


def fast_highpass(x, hz, sr=SR_SFX):
    spec = np.fft.rfft(x)
    freqs = np.fft.rfftfreq(len(x), 1.0 / sr)
    gain = 1.0 / np.sqrt(1.0 + (hz / np.maximum(freqs, 1e-3)) ** 4)
    return np.fft.irfft(spec * gain, len(x)).astype(np.float32)


def whoosh(dur, f0=300.0, f1=3000.0, sr=SR_SFX, seed=3, peak=0.5):
    """Barrido de ruido: el filtro sube (o baja) de f0 a f1 con una campana de volumen."""
    n = int(dur * sr)
    base = noise(dur, sr, seed)
    bands = 6
    freqs = np.geomspace(min(f0, f1), max(f0, f1), bands)
    pos = np.linspace(0, 1, n)
    sweep = pos if f1 > f0 else 1 - pos
    out = np.zeros(n)
    for i, f in enumerate(freqs):
        centre = i / (bands - 1)
        weight = np.exp(-((sweep - centre) ** 2) / (2 * 0.12 ** 2))
        out += fast_lowpass(base, f * 1.4, sr) * weight
    env = np.sin(np.pi * pos) ** (1.6 if peak > 0.4 else 1.0)
    return (out * env * 0.5).astype(np.float32)


def reverb(x, sr=SR_SFX, rt=0.9, mix=0.25, seed=7):
    """Reverberacion por convolucion con un ruido que se apaga (FFT)."""
    n_ir = int(rt * sr)
    rng = np.random.RandomState(seed)
    t = np.arange(n_ir) / sr
    ir = rng.randn(n_ir) * np.exp(-6.9 * t / rt)
    ir = fast_lowpass(ir.astype(np.float32), 5200, sr)
    ir[:int(0.004 * sr)] *= 0.0
    ir /= np.sqrt(np.sum(ir ** 2)) + 1e-9
    total = len(x) + n_ir
    size = 1 << (total - 1).bit_length()
    wet = np.fft.irfft(np.fft.rfft(x, size) * np.fft.rfft(ir, size), size)[:total]
    dry = np.zeros(total)
    dry[:len(x)] = x
    return ((1 - mix) * dry + mix * wet * 1.6).astype(np.float32)


def echo(x, sr, delay, feedback=0.35, taps=3, mix=0.35):
    out = np.zeros(len(x) + int(delay * sr * taps) + 1)
    out[:len(x)] += x
    for k in range(1, taps + 1):
        s = int(delay * sr * k)
        out[s:s + len(x)] += x * (feedback ** k) * mix * 2
    return out.astype(np.float32)


def declick(x, sr=SR_SFX, fin=0.002, fout=0.012):
    x = x.copy()
    a = max(1, int(fin * sr))
    b = max(1, int(fout * sr))
    x[:a] *= np.linspace(0, 1, a)
    x[-b:] *= np.linspace(1, 0, b)
    return x


def normalize(x, peak_db=-4.0):
    p = np.max(np.abs(x)) + 1e-9
    return (x * (10 ** (peak_db / 20.0) / p)).astype(np.float32)


def softclip(x, drive=1.0):
    return np.tanh(x * drive).astype(np.float32)


def trim(x, sr=SR_SFX, thresh_db=-60.0, tail=0.01):
    amp = np.abs(x)
    idx = np.where(amp > 10 ** (thresh_db / 20.0))[0]
    if len(idx) == 0:
        return x
    return x[:min(len(x), idx[-1] + int(tail * sr))]


class Mix:
    """Mesa de mezcla: coloca sonidos en segundos. circular=True hace que lo que se pasa del final vuelva al principio (bucles)."""

    def __init__(self, seconds, sr=SR_SFX, circular=False):
        self.sr = sr
        self.n = int(seconds * sr)
        self.buf = np.zeros(self.n, dtype=np.float64)
        self.circular = circular

    def add(self, sig, at=0.0, gain=1.0):
        start = int(at * self.sr)
        m = len(sig)
        if self.circular:
            idx = (np.arange(m) + start) % self.n
            np.add.at(self.buf, idx, sig * gain)
        else:
            if start >= self.n:
                return
            end = min(self.n, start + m)
            self.buf[start:end] += sig[:end - start] * gain

    def render(self):
        return self.buf.astype(np.float32)


def seq(items, sr=SR_SFX, tail=0.4):
    """items: [(delay_s, señal, ganancia)] -> señal con todo colocado."""
    end = max(d + len(s) / sr for d, s, g in items) + tail
    m = Mix(end, sr)
    for d, s, g in items:
        m.add(s, d, g)
    return m.render()


# ------------------------------------------------------------------ salida

def to_wav(path, x, sr):
    pcm = (np.clip(x, -1, 1) * 32767).astype("<i2")
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(sr)
        w.writeframes(pcm.tobytes())


def to_mp3(path, x, sr, bitrate=96):
    import lameenc
    enc = lameenc.Encoder()
    enc.set_bit_rate(bitrate)
    enc.set_in_sample_rate(sr)
    enc.set_channels(1)
    enc.set_quality(2)
    pcm = (np.clip(x, -1, 1) * 32767).astype("<i2")
    data = enc.encode(pcm.tobytes()) + enc.flush()
    with open(path, "wb") as fh:
        fh.write(data)


def analyze(x, sr):
    peak = float(np.max(np.abs(x)) + 1e-12)
    rms = float(np.sqrt(np.mean(x.astype(np.float64) ** 2)) + 1e-12)
    spec = np.abs(np.fft.rfft(x * np.hanning(len(x))))
    freqs = np.fft.rfftfreq(len(x), 1.0 / sr)
    centroid = float(np.sum(freqs * spec) / (np.sum(spec) + 1e-9))
    return {
        "dur": len(x) / sr,
        "peak_db": 20 * math.log10(peak),
        "rms_db": 20 * math.log10(rms),
        "clipped": int(np.sum(np.abs(x) > 0.999)),
        "dc": float(np.mean(x)),
        "centroid": centroid,
        "low": float(np.sum(spec[freqs < 120] ** 2) / (np.sum(spec ** 2) + 1e-9)),
        "head": float(abs(x[0])), "tail": float(abs(x[-1])),
    }
