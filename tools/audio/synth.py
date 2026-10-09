# -*- coding: utf-8 -*-
"""
Sintetizador mínimo (solo numpy) con el que se generan TODOS los sonidos y la música de ParDos.
Sin muestras de terceros: todo nace aquí, así que no hay derechos de autor que cuidar.

Uso: ver sfx.py y music.py. Los .ogg salen en app/src/main/res/raw/.
"""
import os
import subprocess
import sys
import wave

import numpy as np

SR = 44100


# ------------------------------------------------------------------ utilidades básicas

def N(dur, sr=SR):
    """Muestras que ocupan 'dur' segundos (redondeado: así todas las señales miden lo mismo)."""
    return int(round(dur * sr))


def T(dur, sr=SR):
    """Eje de tiempo en segundos."""
    return np.arange(N(dur, sr)) / sr


def midi(m):
    return 440.0 * 2.0 ** ((np.asarray(m, dtype=float) - 69.0) / 12.0)


def db(x):
    return 10.0 ** (x / 20.0)


def fade(x, a_ms=2.0, r_ms=10.0, sr=SR):
    """Fundidos cortos al inicio y al final para que nada haga 'clic'."""
    x = x.copy()
    a = min(len(x), int(sr * a_ms / 1000.0))
    r = min(len(x), int(sr * r_ms / 1000.0))
    if a > 1:
        x[:a] *= np.linspace(0.0, 1.0, a)
    if r > 1:
        x[-r:] *= np.linspace(1.0, 0.0, r)
    return x


def pad_to(x, dur, sr=SR):
    n = int(round(dur * sr))
    if len(x) >= n:
        return x[:n].copy()
    return np.concatenate([x, np.zeros(n - len(x), dtype=x.dtype)])


def place(buf, sig, at, gain=1.0, sr=SR):
    """Suma 'sig' dentro de 'buf' empezando en 'at' segundos (se recorta si no cabe)."""
    i = int(round(at * sr))
    if i >= len(buf) or i + len(sig) <= 0:
        return buf
    s0 = max(0, -i)
    i0 = max(0, i)
    j = min(len(buf), i + len(sig))
    buf[i0:j] += sig[s0:s0 + (j - i0)] * gain
    return buf


def normalize(x, peak=0.9):
    m = float(np.max(np.abs(x))) if len(x) else 0.0
    return x if m < 1e-9 else x * (peak / m)


def soft_clip(x, drive=1.0):
    return np.tanh(x * drive) / np.tanh(drive)


def rms_db(x):
    r = float(np.sqrt(np.mean(np.square(x)))) if len(x) else 0.0
    return 20.0 * np.log10(max(r, 1e-9))


# ------------------------------------------------------------------ envolventes

def env_exp(n, decay, attack=0.002, sr=SR):
    """Ataque lineal corto + caída exponencial (decay en 1/s)."""
    t = np.arange(n) / sr
    return np.minimum(1.0, t / max(attack, 1e-5)) * np.exp(-t * decay)


def env_adsr(n, a, d, s, r, sr=SR):
    """ADSR (tiempos en segundos, sostenido 's' en 0..1) sobre n muestras."""
    t = np.arange(n) / sr
    dur = n / sr
    e = np.ones(n)
    e = np.where(t < a, t / max(a, 1e-5), e)
    e = np.where((t >= a) & (t < a + d), 1.0 - (1.0 - s) * (t - a) / max(d, 1e-5), e)
    e = np.where(t >= a + d, s, e)
    rel = np.clip((dur - t) / max(r, 1e-5), 0.0, 1.0)
    return e * rel


# ------------------------------------------------------------------ filtros (por FFT, sin scipy)

def fft_filter(x, fn):
    """Multiplica el espectro por fn(freqs) (respuesta suave en 0..1+)."""
    n = len(x)
    X = np.fft.rfft(x)
    f = np.fft.rfftfreq(n, 1.0 / SR)
    return np.fft.irfft(X * fn(f), n)


def lowpass(x, fc, order=4):
    return fft_filter(x, lambda f: 1.0 / np.sqrt(1.0 + (f / fc) ** (2 * order)))


def highpass(x, fc, order=4):
    return fft_filter(x, lambda f: 1.0 / np.sqrt(1.0 + (fc / np.maximum(f, 1e-3)) ** (2 * order)))


def bandpass(x, lo, hi, order=3):
    return highpass(lowpass(x, hi, order), lo, order)


def stft_filter(x, resp, win=1024):
    """Filtro que cambia con el tiempo: resp(freqs, t_frac) devuelve la ganancia por frecuencia (t_frac = 0..1)."""
    hop = win // 2
    w = np.hanning(win)
    n = len(x)
    pad = np.concatenate([np.zeros(win), x, np.zeros(win * 2)])
    out = np.zeros(len(pad))
    f = np.fft.rfftfreq(win, 1.0 / SR)
    total = max(1, n)
    pos = 0
    while pos + win <= len(pad):
        seg = pad[pos:pos + win] * w
        S = np.fft.rfft(seg)
        center = pos + win // 2 - win
        frac = float(np.clip(center / total, 0.0, 1.0))
        out[pos:pos + win] += np.fft.irfft(S * resp(f, frac), win) * w
        pos += hop
    out = out / 1.5  # compensa el solape de ventanas Hann al 50 %
    return out[win:win + n]


def noise(n, seed=None):
    r = np.random.RandomState(seed)
    return r.uniform(-1.0, 1.0, n)


def pink(n, seed=None):
    w = noise(n, seed)
    return lowpass(w, 4000, 2) * 0.7 + w * 0.1


# ------------------------------------------------------------------ reverb y eco

_IR_CACHE = {}


def _ir(rt60, damp, seed, length=None):
    key = (round(rt60, 3), damp, seed, length)
    if key in _IR_CACHE:
        return _IR_CACHE[key]
    n = N(length if length else rt60 * 1.2)
    t = np.arange(n) / SR
    ir = noise(n, seed) * np.exp(-6.9 * t / rt60)
    ir = lowpass(ir, damp, 2)
    # unos pocos rebotes tempranos para dar 'sala'
    r = np.random.RandomState(seed + 1)
    for k in range(7):
        i = N(0.004 + 0.012 * k + 0.003 * r.rand())
        if i < n:
            ir[i] += (0.9 - 0.1 * k) * (1 if r.rand() > 0.5 else -1)
    ir /= np.sqrt(np.sum(ir ** 2)) + 1e-9
    _IR_CACHE[key] = ir
    return ir


def fftconv(x, h):
    n = len(x) + len(h) - 1
    m = 1
    while m < n:
        m *= 2
    return np.fft.irfft(np.fft.rfft(x, m) * np.fft.rfft(h, m), m)[:n]


def reverb(x, rt60=1.0, wet=0.25, damp=6000.0, predelay=0.01, seed=11, keep_tail=True):
    """Reverb mono. Devuelve la señal con cola (más larga que la de entrada) si keep_tail."""
    ir = _ir(rt60, damp, seed)
    pre = np.zeros(N(predelay))
    wetsig = fftconv(x, np.concatenate([pre, ir]))
    dry = np.concatenate([x, np.zeros(len(wetsig) - len(x))])
    out = dry * (1.0 - wet * 0.5) + wetsig * wet * 1.6
    return out if keep_tail else out[:len(x)]


def reverb_stereo(x, rt60=1.8, wet=0.3, damp=5500.0, predelay=0.015, seed=21):
    """Devuelve (L, R) con dos reverbs distintas: da ancho."""
    L = reverb(x, rt60, wet, damp, predelay, seed)
    R = reverb(x, rt60, wet, damp, predelay + 0.004, seed + 7)
    n = max(len(L), len(R))
    return pad_to(L, n / SR), pad_to(R, n / SR)


def echo(x, delay, feedback=0.35, taps=4, tone=3500.0):
    out = np.concatenate([x, np.zeros(N(delay * taps))])
    cur = x
    for k in range(1, taps + 1):
        cur = lowpass(cur, tone, 2) * feedback
        place(out, cur, delay * k)
    return out


# ------------------------------------------------------------------ instrumentos (una nota)

def _partials(f, dur, spec, attack=0.002):
    """spec = [(razón, amplitud, caída 1/s)]. Descarta los parciales que pasen de 18 kHz."""
    t = T(dur)
    y = np.zeros(len(t))
    for ratio, amp, dec in spec:
        fr = f * ratio
        if fr > 18000:
            continue
        y += amp * np.sin(2 * np.pi * fr * t) * env_exp(len(t), dec, attack)
    return y


def marimba(f, dur=0.7):
    y = _partials(f, dur, [(1, 1.0, 5.5), (4.0, 0.5, 22), (9.2, 0.12, 48)], 0.0015)
    click = bandpass(noise(N(0.012), int(f)), 1500, 6000) * env_exp(N(0.012), 280)
    y[:len(click)] += click * 0.35
    return fade(y)


def kalimba(f, dur=0.9):
    y = _partials(f, dur, [(1, 1.0, 4.2), (5.4, 0.28, 16), (8.9, 0.1, 30), (2.0, 0.12, 9)], 0.001)
    return fade(y)


def glass(f, dur=1.4):
    y = _partials(f, dur, [(1, 1.0, 2.4), (2.76, 0.55, 3.6), (5.4, 0.28, 6.0), (8.93, 0.12, 9.5)], 0.0012)
    return fade(y)


def celesta(f, dur=1.1):
    y = _partials(f, dur, [(1, 1.0, 3.6), (2, 0.4, 5.5), (3, 0.18, 8), (4.01, 0.1, 13)], 0.001)
    return fade(y)


def musicbox(f, dur=0.8):
    y = _partials(f, dur, [(1, 1.0, 4.5), (3.01, 0.5, 8), (6.2, 0.2, 15), (9.6, 0.07, 26)], 0.0008)
    return fade(y)


def xylophone(f, dur=0.45):
    y = _partials(f, dur, [(1, 1.0, 9), (3.0, 0.5, 17), (6.0, 0.22, 34)], 0.0008)
    click = bandpass(noise(N(0.008), int(f) + 3), 2500, 9000) * env_exp(N(0.008), 400)
    y[:len(click)] += click * 0.3
    return fade(y)


def koto(f, dur=1.0):
    t = T(dur)
    y = np.zeros(len(t))
    for k in range(1, 9):
        fr = f * k * (1 + 0.0003 * k * k)
        if fr > 16000:
            break
        y += (1.0 / k) * np.sin(2 * np.pi * fr * t) * np.exp(-t * (2.6 + 1.1 * k))
    y *= np.minimum(1.0, t / 0.001)
    return fade(y)


def bloop(f, dur=0.35):
    t = T(dur)
    # sube de tono como una burbuja que revienta
    fr = f * (0.62 + 0.62 * (1 - np.exp(-t * 45)))
    ph = 2 * np.pi * np.cumsum(fr) / SR
    y = np.sin(ph) * env_exp(len(t), 11, 0.002) + 0.25 * np.sin(2 * ph) * env_exp(len(t), 25, 0.002)
    return fade(y)


def ripple(f, dur=1.5):
    t = T(dur)
    y = (np.sin(2 * np.pi * f * t) + 0.35 * np.sin(2 * np.pi * f * 2.003 * t) * np.exp(-t * 2.0)) * np.exp(-t * 2.2)
    y *= np.minimum(1.0, t / 0.008)
    return fade(y)


def zap(f, dur=0.5):
    t = T(dur)
    idx = 5.0 * np.exp(-t * 18)
    y = np.sin(2 * np.pi * f * t + idx * np.sin(2 * np.pi * f * 2.01 * t)) * env_exp(len(t), 7, 0.001)
    y += 0.15 * np.sin(2 * np.pi * f * 4.02 * t) * env_exp(len(t), 30, 0.001)
    return fade(y)


def firebell(f, dur=1.3):
    y = glass(f, dur)
    t = T(dur)
    boom = np.sin(2 * np.pi * np.maximum(45, 95 * np.exp(-t * 7)) * t) * np.exp(-t * 9) * 0.55
    crack = bandpass(noise(len(t), int(f) + 5), 2500, 9000) * np.exp(-t * 55) * 0.25
    return fade(y + boom + crack)


VOICES = {
    'marimba': marimba, 'kalimba': kalimba, 'bloop': bloop, 'koto': koto, 'musicbox': musicbox,
    'xylophone': xylophone, 'glass': glass, 'ripple': ripple, 'zap': zap, 'firebell': firebell,
}


# ------------------------------------------------------------------ percusión

def kick(dur=0.35, f0=130.0, f1=42.0):
    t = T(dur)
    fr = f1 + (f0 - f1) * np.exp(-t * 28)
    ph = 2 * np.pi * np.cumsum(fr) / SR
    y = np.sin(ph) * np.exp(-t * 9)
    y += 0.3 * bandpass(noise(len(t), 3), 800, 4000) * np.exp(-t * 120)
    return fade(y, 1, 8)


def rim(dur=0.12):
    t = T(dur)
    y = np.sin(2 * np.pi * 820 * t) * np.exp(-t * 60) * 0.5
    y += bandpass(noise(len(t), 5), 1500, 7000) * np.exp(-t * 90) * 0.7
    return fade(y, 1, 6)


def shaker(dur=0.09, seed=1):
    t = T(dur)
    y = highpass(noise(len(t), seed), 4500, 2) * np.minimum(1.0, t / 0.012) * np.exp(-t * 38)
    return fade(y, 1, 8)


def hat(dur=0.07, seed=2):
    t = T(dur)
    y = highpass(noise(len(t), seed), 7000, 3) * np.exp(-t * 70)
    return fade(y, 0.5, 5)


def tom(f=110.0, dur=0.5):
    t = T(dur)
    fr = f * (1 + 0.55 * np.exp(-t * 18))
    ph = 2 * np.pi * np.cumsum(fr) / SR
    y = np.sin(ph) * np.exp(-t * 7.5) + 0.25 * bandpass(noise(len(t), 9), 300, 2500) * np.exp(-t * 45)
    return fade(y, 1, 12)


def taiko(f=70.0, dur=1.0):
    t = T(dur)
    fr = f * (1 + 0.9 * np.exp(-t * 14))
    ph = 2 * np.pi * np.cumsum(fr) / SR
    y = np.sin(ph) * np.exp(-t * 3.6) + 0.4 * lowpass(noise(len(t), 12), 1800, 2) * np.exp(-t * 30)
    return fade(y, 1, 20)


def snare(dur=0.25):
    t = T(dur)
    y = bandpass(noise(len(t), 14), 1200, 7500) * np.exp(-t * 22) * 0.7
    y += np.sin(2 * np.pi * 190 * t) * np.exp(-t * 28) * 0.5
    return fade(y, 1, 12)


# ------------------------------------------------------------------ texturas

def whoosh(dur=0.3, f0=500.0, f1=3500.0, seed=4, peak_at=0.5, width=2200.0):
    """Ruido que 'pasa': un filtro de banda que sube (f1 > f0) o baja (f1 < f0)."""
    n = N(dur)
    x = noise(n, seed)

    def resp(f, tf):
        c = f0 * (f1 / f0) ** tf
        return np.exp(-0.5 * ((f - c) / (c * 0.55 + 200)) ** 2)

    y = stft_filter(x, resp, 1024)
    t = np.arange(n) / SR / dur
    env = np.sin(np.pi * np.clip(t, 0, 1)) ** (1.4 if peak_at == 0.5 else 1.0)
    if peak_at != 0.5:
        env = np.where(t < peak_at, (t / peak_at) ** 1.5, ((1 - t) / (1 - peak_at)) ** 1.2)
    return fade(y * env)


def sparkle(dur=0.8, count=14, lo=2200.0, hi=7000.0, seed=3, level=0.5):
    """Polvito mágico: campanitas diminutas en momentos al azar."""
    r = np.random.RandomState(seed)
    out = np.zeros(N(dur))
    for _ in range(count):
        f = lo * (hi / lo) ** r.rand()
        at = r.rand() * dur * 0.75
        note = _partials(f, 0.35, [(1, 1.0, 14), (2.7, 0.3, 24)], 0.0006)
        place(out, note, at, level * (0.35 + 0.65 * r.rand()))
    return out


def shimmer(dur=1.0, f=1568.0, seed=5):
    """Brillo que sube y se apaga (para el 'tercer nivel' de cualquier celebración)."""
    t = T(dur)
    y = np.zeros(len(t))
    r = np.random.RandomState(seed)
    for k in range(6):
        fr = f * (1.0 + 0.5 * k) * (1 + 0.002 * r.randn())
        if fr > 15000:
            continue
        y += np.sin(2 * np.pi * fr * t) * (0.7 ** k)
    env = np.sin(np.pi * np.clip(t / dur, 0, 1)) ** 0.8
    y *= env * (1 + 0.25 * np.sin(2 * np.pi * 7.0 * t))
    return fade(y * 0.35, 5, 60)


def swell(f, dur=1.2, detune=0.004, voices=3, harm=(1.0, 0.4, 0.15), attack=0.35, release=0.5):
    """Colchón suave (varias ondas ligeramente desafinadas)."""
    t = T(dur)
    y = np.zeros(len(t))
    for v in range(voices):
        d = (v - (voices - 1) / 2.0) * detune
        for k, a in enumerate(harm, start=1):
            y += a * np.sin(2 * np.pi * f * k * (1 + d) * t + v)
    env = env_adsr(len(t), attack, 0.1, 0.85, release)
    return y * env / voices


def saw(f, dur, harmonics=14):
    t = T(dur)
    y = np.zeros(len(t))
    for k in range(1, harmonics + 1):
        if f * k > 14000:
            break
        y += np.sin(2 * np.pi * f * k * t) / k
    return y * (2 / np.pi)


def gong(f=110.0, dur=2.6):
    t = T(dur)
    y = np.zeros(len(t))
    for ratio, amp, dec in [(1, 1.0, 1.3), (1.51, 0.6, 1.7), (2.0, 0.55, 2.0), (2.74, 0.4, 2.6), (3.76, 0.3, 3.4), (5.4, 0.18, 4.8)]:
        y += amp * np.sin(2 * np.pi * f * ratio * t + 0.3 * np.sin(2 * np.pi * 2.1 * t)) * np.exp(-t * dec)
    y *= np.minimum(1.0, t / 0.004)
    y += 0.4 * lowpass(noise(len(t), 33), 1500, 2) * np.exp(-t * 22)
    return fade(y, 1, 80)


# ------------------------------------------------------------------ salida

def to_wav(path, x, sr=SR):
    """x mono (n,) o estéreo (n,2). Se guarda en 16 bits."""
    x = np.asarray(x)
    x = np.clip(x, -1.0, 1.0)
    pcm = (x * 32767.0).astype('<i2')
    ch = 1 if pcm.ndim == 1 else pcm.shape[1]
    with wave.open(path, 'wb') as w:
        w.setnchannels(ch)
        w.setsampwidth(2)
        w.setframerate(sr)
        w.writeframes(pcm.tobytes())


def ffmpeg_exe():
    here = os.path.dirname(os.path.abspath(__file__))
    env = os.environ.get('FFMPEG')
    if env and os.path.exists(env):
        return env
    try:
        import imageio_ffmpeg  # type: ignore
        return imageio_ffmpeg.get_ffmpeg_exe()
    except Exception:
        pass
    return 'ffmpeg'


def to_ogg(wav_path, ogg_path, quality=4, sr=None, channels=None):
    cmd = [ffmpeg_exe(), '-y', '-hide_banner', '-loglevel', 'error', '-i', wav_path]
    if sr:
        cmd += ['-ar', str(sr)]
    if channels:
        cmd += ['-ac', str(channels)]
    cmd += ['-c:a', 'libvorbis', '-q:a', str(quality), ogg_path]
    subprocess.run(cmd, check=True)
