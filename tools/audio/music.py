# -*- coding: utf-8 -*-
"""
Música adaptativa de ParDos: 5 piezas (zen, dusk, deep, boss, halloween), cada una en 3 capas con la misma duración:

    base   -> bajo, colchón y arpegio suave (siempre suena)
    groove -> percusión suave y acordes cortitos (entra al ganar ritmo)
    lead   -> melodía de campanitas (entra en FLOW y en los momentos grandes)

La app mezcla las capas en tiempo real (AdaptiveMusic.kt). Todo es armonía consonante (pentatónica sobre acordes sencillos),
sintetizada aquí mismo: sin derechos de autor.

    python tools/audio/music.py            # todas
    python tools/audio/music.py zen        # una sola
"""
import os
import sys
import tempfile

import numpy as np

from synth import *  # noqa: F401,F403
from sfx import INSTR, plus  # reutiliza los instrumentos

HERE = os.path.dirname(os.path.abspath(__file__))
RAW = os.path.normpath(os.path.join(HERE, '..', '..', 'app', 'src', 'main', 'res', 'raw'))

OUT_SR = 32000  # los .ogg salen a 32 kHz (música suave: sobra, y pesa menos)


# ------------------------------------------------------------------ instrumentos de la música

def bass_note(f, dur, vel=1.0):
    n = N(dur)
    t = np.arange(n) / SR
    y = np.sin(2 * np.pi * f * t) + 0.32 * np.sin(2 * np.pi * 2 * f * t) * np.exp(-t * 2.5) + 0.08 * np.sin(2 * np.pi * 3 * f * t) * np.exp(-t * 5)
    y *= env_adsr(n, 0.02, 0.12, 0.82, min(0.3, dur * 0.4))
    return lowpass(y, 650, 2) * vel


def pad_note(f, dur, vel=1.0, bright=2600.0):
    y = swell(f, dur, 0.0045, 3, (1.0, 0.35, 0.12), attack=min(0.7, dur * 0.4), release=min(0.8, dur * 0.45))
    return lowpass(y, bright, 2) * vel


def string_note(f, dur, vel=1.0, bright=2200.0):
    """Cuerdas suaves (sierra filtrada, varias voces) para Halloween y jefes."""
    n = N(dur)
    y = np.zeros(n)
    for d in (-0.005, 0.0, 0.005):
        y += saw(f * (1 + d), dur, 10)[:n]
    y *= env_adsr(n, min(0.6, dur * 0.35), 0.15, 0.8, min(0.7, dur * 0.4))
    return lowpass(y, bright, 2) * vel / 3


def plucked(voice, f, dur=0.6, vel=1.0):
    fn = INSTR[voice]
    try:
        return fn(f, dur) * vel
    except TypeError:
        return fn(f) * vel


def drum(kind, vel=1.0):
    return {'kick': lambda: kick(), 'rim': lambda: rim(), 'shaker': lambda: shaker(), 'hat': lambda: hat(), 'tom': lambda: tom(),
            'taiko': lambda: taiko(), 'snare': lambda: snare()}[kind]() * vel


# ------------------------------------------------------------------ configuración de cada pieza

class Cfg:
    def __init__(self, **kw):
        self.__dict__.update(kw)
        self.beat = 60.0 / self.bpm
        self.bar = self.beat * 4
        self.n_bars = self.cycles * len(self.chords)
        self.length = self.bar * self.n_bars
        self.N = N(self.length)
        self.tail = 3.0


SETS = {
    # Do mayor pentatónico: Am - F - C - G (cálido, "jardín")
    'zen': Cfg(
        bpm=88, cycles=4, swing=0.54, seed=101,
        chords=[
            dict(bass=45, fifth=52, tones=[57, 60, 64, 67], up=[69, 72, 76]),
            dict(bass=41, fifth=48, tones=[53, 57, 60, 64], up=[65, 69, 72]),
            dict(bass=48, fifth=55, tones=[55, 60, 64, 67], up=[72, 76, 79]),
            dict(bass=43, fifth=50, tones=[55, 59, 62, 67], up=[71, 74, 79]),
        ],
        pool=[72, 74, 76, 79, 81, 84, 86],
        arp='kalimba', lead='glass', lead2='celesta', chord_inst='kalimba', pad_bright=2600,
        kick=True, shaker=True, rim=True, minor=False,
        rms=dict(base=-23.0, groove=-27.0, lead=-26.0),
    ),
    # Re menor pentatónico: Dm7 - Bbmaj7 - Fmaj7 - C (atardecer, ritmo con swing)
    'dusk': Cfg(
        bpm=94, cycles=4, swing=0.58, seed=202,
        chords=[
            dict(bass=38, fifth=45, tones=[57, 62, 65, 69], up=[72, 74, 77]),
            dict(bass=46, fifth=53, tones=[58, 62, 65, 69], up=[70, 74, 77]),
            dict(bass=41, fifth=48, tones=[57, 60, 64, 65], up=[69, 72, 77]),
            dict(bass=48, fifth=55, tones=[55, 60, 64, 67], up=[72, 74, 79]),
        ],
        pool=[74, 77, 79, 81, 84, 86, 89],
        arp='musicbox', lead='celesta', lead2='kalimba', chord_inst='marimba', pad_bright=2300,
        kick=True, shaker=True, rim=True, minor=True,
        rms=dict(base=-23.0, groove=-27.0, lead=-26.0),
    ),
    # Mi menor pentatónico: Em - C - G - D (profundo, aventura)
    'deep': Cfg(
        bpm=100, cycles=4, swing=0.52, seed=303,
        chords=[
            dict(bass=40, fifth=47, tones=[52, 55, 59, 64], up=[67, 71, 76]),
            dict(bass=36, fifth=43, tones=[55, 60, 64, 67], up=[71, 72, 76]),
            dict(bass=43, fifth=50, tones=[55, 59, 62, 67], up=[71, 74, 79]),
            dict(bass=38, fifth=45, tones=[57, 62, 66, 69], up=[74, 78, 81]),
        ],
        pool=[76, 79, 81, 83, 86, 88, 91],
        arp='kalimba', lead='glass', lead2='celesta', chord_inst='marimba', pad_bright=2100,
        kick=True, shaker=True, rim=False, minor=True,
        rms=dict(base=-23.0, groove=-27.0, lead=-26.0),
    ),
    # La menor, tenso: Am - F - G - Em (jefes)
    'boss': Cfg(
        bpm=112, cycles=4, swing=0.5, seed=404,
        chords=[
            dict(bass=33, fifth=40, tones=[57, 60, 64, 69], up=[72, 76, 81]),
            dict(bass=29, fifth=36, tones=[53, 57, 60, 65], up=[69, 72, 77]),
            dict(bass=31, fifth=38, tones=[55, 59, 62, 67], up=[71, 74, 79]),
            dict(bass=28, fifth=35, tones=[52, 55, 59, 64], up=[67, 71, 76]),
        ],
        pool=[69, 72, 74, 76, 79, 81, 84],
        arp='koto', lead='zap', lead2='glass', chord_inst='marimba', pad_bright=1900, strings=True,
        kick=True, shaker=True, rim=True, minor=True, taiko=True, driving=True,
        rms=dict(base=-22.0, groove=-25.0, lead=-27.0),
    ),
    # La menor armónico, misterioso: Am - F - E - Am (Noche de Brujas)
    'halloween': Cfg(
        bpm=84, cycles=4, swing=0.53, seed=505,
        chords=[
            dict(bass=45, fifth=52, tones=[57, 60, 64, 69], up=[72, 76, 81], pool=[69, 72, 76, 81, 84]),
            dict(bass=41, fifth=48, tones=[53, 57, 60, 65], up=[69, 72, 77], pool=[69, 72, 77, 81, 84]),
            dict(bass=40, fifth=47, tones=[52, 56, 59, 64], up=[68, 71, 76], pool=[68, 71, 76, 80, 83]),
            dict(bass=45, fifth=52, tones=[57, 60, 64, 69], up=[72, 76, 81], pool=[69, 72, 76, 81, 84]),
        ],
        pool=[69, 72, 76, 81, 84],
        arp='musicbox', lead='celesta', lead2='glass', chord_inst='musicbox', pad_bright=1700, strings=True,
        kick=True, shaker=False, rim=True, minor=True, spooky=True,
        rms=dict(base=-23.0, groove=-28.0, lead=-26.0),
    ),
}


# ------------------------------------------------------------------ construcción de capas

def fold(sig, n):
    """Pliega la cola (reverb y notas que se pasan del final) sobre el inicio: el bucle empalma sin corte."""
    out = np.zeros(n)
    k = 0
    while k * n < len(sig):
        seg = sig[k * n:(k + 1) * n]
        out[:len(seg)] += seg
        k += 1
    return out


def finish(dry, cfg, rt60=2.2, wet=0.32, seed=1, width=True):
    """Reverb estéreo + pliegue de la cola + filtros de limpieza. Devuelve (L, R) de largo exacto N."""
    dry = highpass(dry, 35, 2)
    if width:
        L, R = reverb_stereo(dry, rt60, wet, 5200.0, 0.02, 30 + seed)
    else:
        L = R = reverb(dry, rt60, wet, 5200.0, 0.02, 30 + seed)
    L, R = fold(L, cfg.N), fold(R, cfg.N)
    return lowpass(L, 13000, 2), lowpass(R, 13000, 2)


def swing_t(cfg, bar_start, eighth_index):
    """Tiempo en segundos de la corchea 'eighth_index' (0..7) de un compás, con swing."""
    base = eighth_index * cfg.beat / 2
    if eighth_index % 2 == 1:
        base += (cfg.swing - 0.5) * cfg.beat
    return bar_start + base


def chord_of(cfg, bar):
    return cfg.chords[bar % len(cfg.chords)]


def arp_pattern(rng, bar):
    pats = [[0, 1, 2, 3, 2, 1, 2, 1], [0, 2, 1, 3, 2, 3, 1, 2], [0, 1, 2, 4, 3, 2, 1, 0], [0, 3, 2, 1, 2, 3, 4, 3]]
    return pats[(bar + rng.randint(0, 2)) % len(pats)] if bar % 2 == 0 else pats[(bar // 2 * 3 + 1) % len(pats)]


def build_base(cfg):
    rng = np.random.RandomState(cfg.seed)
    total = cfg.length + cfg.tail
    dry = np.zeros(N(total))
    padl = np.zeros(N(total))
    padr = np.zeros(N(total))
    for b in range(cfg.n_bars):
        ch = chord_of(cfg, b)
        t0 = b * cfg.bar
        # bajo: la fundamental dura el compás; a veces la quinta en el tiempo 3
        if getattr(cfg, 'driving', False):
            for k in range(8):
                f = midi(ch['bass'] + (12 if k in (3, 7) else 0))
                place(dry, bass_note(f, cfg.beat * 0.42, 0.85 if k % 2 == 0 else 0.55), t0 + k * cfg.beat / 2, 0.42)
        else:
            place(dry, bass_note(midi(ch['bass']), cfg.bar * 0.96), t0, 0.5)
            if b % 2 == 1:
                place(dry, bass_note(midi(ch['fifth']), cfg.beat * 1.6, 0.8), t0 + cfg.beat * 2, 0.22)
        # colchón: notas del acorde, una por voz, con atención a no estorbar al bajo
        for i, m in enumerate(ch['tones'][:3]):
            f = midi(m - 12 if m > 62 else m)
            note = (string_note if getattr(cfg, 'strings', False) else pad_note)(f, cfg.bar + 0.9, 1.0, cfg.pad_bright)
            # estéreo: cada nota se reparte distinto
            gl, gr = (0.8, 0.45) if i % 2 == 0 else (0.45, 0.8)
            place(padl, note, t0, 0.12 * gl)
            place(padr, note, t0, 0.12 * gr)
        # arpegio suave (corcheas, con notas salteadas para respirar)
        notes = ch['tones'] + ch['up']
        pat = arp_pattern(rng, b)
        for k, idx in enumerate(pat):
            if (k % 2 == 1 and rng.rand() < 0.35) or (k == 0 and b % 4 == 3 and rng.rand() < 0.5):
                continue
            m = notes[idx % len(notes)] + (12 if idx >= 4 and rng.rand() < 0.3 else 0)
            vel = 0.2 if k % 2 == 0 else 0.13
            place(dry, plucked(cfg.arp, midi(m), 0.55, 1.0), swing_t(cfg, t0, k), vel)
        # polvito de campanitas cada cuatro compases
        if b % 4 == 3:
            place(dry, sparkle(1.4, 5, 3000, 7500, int(cfg.seed + b), 0.18), t0 + cfg.beat * 1.0, 1.0)
        if getattr(cfg, 'spooky', False) and b % 8 == 0:
            place(dry, gong(midi(ch['bass'] + 12), 3.5), t0, 0.05)
    # los colchones van directo estéreo (sin reverb larga), el resto con reverb
    L, R = finish(dry, cfg, 2.2, 0.3, 1)
    pl, pr = fold(padl, cfg.N), fold(padr, cfg.N)
    pl, pr = reverb(pl, 2.6, 0.3, 4500.0, 0.03, 51, keep_tail=False), reverb(pr, 2.6, 0.3, 4500.0, 0.03, 52, keep_tail=False)
    return L + pl, R + pr


def build_groove(cfg):
    rng = np.random.RandomState(cfg.seed + 1)
    total = cfg.length + cfg.tail
    dry = np.zeros(N(total))
    drums = np.zeros(N(total))
    for b in range(cfg.n_bars):
        ch = chord_of(cfg, b)
        t0 = b * cfg.bar
        taiko_bar = getattr(cfg, 'taiko', False)
        if cfg.kick:
            place(drums, drum('kick'), t0, 0.5)
            place(drums, drum('kick'), t0 + cfg.beat * 2, 0.42)
            if b % 2 == 1:
                place(drums, drum('kick'), swing_t(cfg, t0, 5), 0.28)
        if cfg.rim:
            place(drums, drum('rim'), t0 + cfg.beat, 0.34)
            place(drums, drum('rim'), t0 + cfg.beat * 3, 0.34)
        if cfg.shaker:
            steps = 16 if getattr(cfg, 'driving', False) else 8
            for k in range(steps):
                tt = t0 + (swing_t(cfg, 0, k) if steps == 8 else k * cfg.beat / 4)
                place(drums, drum('shaker') if rng.rand() > 0.12 else drum('hat'), tt, (0.3 if k % 2 == 0 else 0.2) * (0.6 if steps == 16 else 1.0))
        if taiko_bar:
            place(drums, drum('taiko'), t0, 0.35)
            if b % 2 == 1:
                place(drums, drum('tom'), t0 + cfg.beat * 3.5, 0.28)
                place(drums, drum('tom') * 0.9, t0 + cfg.beat * 3.75, 0.22)
        # acordes cortitos a contratiempo
        for k in (3, 7):
            for i, m in enumerate(ch['tones'][:3]):
                place(dry, plucked(cfg.chord_inst, midi(m + 12), 0.4, 1.0), swing_t(cfg, t0, k) + i * 0.012, 0.075)
        # pulso del bajo (corcheas suaves en los compases pares)
        if b % 2 == 0 and not getattr(cfg, 'driving', False):
            for k in (0, 2, 4, 6):
                place(dry, bass_note(midi(ch['bass'] + 12), cfg.beat * 0.4, 0.5), swing_t(cfg, t0, k), 0.12)
    dl, dr = finish(dry, cfg, 1.4, 0.2, 2)
    # percusión casi seca (con un poquito de aire)
    pl = reverb(drums, 0.8, 0.12, 6000.0, 0.01, 61, keep_tail=False)
    pr = reverb(drums, 0.8, 0.12, 6000.0, 0.012, 62, keep_tail=False)
    return dl + fold(pl, cfg.N), dr + fold(pr, cfg.N)


def make_melody(cfg, rng):
    """Devuelve [(compás, tiempo_en_compás_en_tiempos, nota_midi, duración_en_tiempos, velocidad)]."""
    motifs = [
        [(0, 1.0), (1.0, 0.5), (1.5, 0.5), (2.0, 1.5), (3.5, 0.5)],
        [(0, 0.5), (0.5, 0.5), (1.0, 1.0), (2.5, 0.5), (3.0, 1.0)],
        [(0.5, 1.0), (1.5, 0.5), (2.0, 0.5), (3.0, 1.0)],
        [(0, 1.5), (2.0, 0.5), (2.5, 0.5), (3.0, 0.5), (3.5, 0.5)],
    ]
    out = []
    idx = 3
    for phrase in range(cfg.n_bars // 2):
        motif = motifs[(phrase * 3 + rng.randint(0, 2)) % len(motifs)]
        for bar_in in range(2):
            b = phrase * 2 + bar_in
            ch = chord_of(cfg, b)
            pool = ch.get('pool', cfg.pool)
            # el segundo compás de la frase responde: más suave o más grave
            for (beat, dur) in motif:
                if bar_in == 1 and rng.rand() < 0.3:
                    continue
                if phrase % 2 == 1 and bar_in == 0 and beat == 0 and rng.rand() < 0.5:
                    continue
                step = int(rng.choice([-2, -1, -1, 0, 1, 1, 2]))
                idx = int(np.clip(idx + step, 0, len(pool) - 1))
                out.append((b, beat, pool[idx], dur, 0.8 if beat == 0 else 0.6))
        # cada ocho compases la melodía descansa un compás para respirar
    return out


def build_lead(cfg):
    rng = np.random.RandomState(cfg.seed + 2)
    total = cfg.length + cfg.tail
    dry = np.zeros(N(total))
    sh = np.zeros(N(total))
    for (b, beat, m, dur, vel) in make_melody(cfg, rng):
        if b % 8 == 7 and beat >= 2.0:
            continue
        t = b * cfg.bar + beat * cfg.beat
        if (beat * 2) % 2 == 1:
            t += (cfg.swing - 0.5) * cfg.beat
        inst = cfg.lead if rng.rand() < 0.7 else cfg.lead2
        place(dry, plucked(inst, midi(m), 0.9, 1.0), t, 0.16 * vel)
    # un hilo de brillo agudo sostenido sobre cada acorde (octava arriba), muy suave
    for b in range(cfg.n_bars):
        ch = chord_of(cfg, b)
        place(sh, pad_note(midi(ch['up'][0] + 12), cfg.bar + 0.8, 1.0, 5200.0), b * cfg.bar, 0.035)
        if b % 4 == 2:
            place(dry, sparkle(1.6, 6, 3200, 9000, cfg.seed + b, 0.16), b * cfg.bar + cfg.beat * 2, 1.0)
    dry = plus(dry, sh)
    dry = echo(dry, cfg.beat * 0.75, 0.38, 3, 3200.0)
    L, R = finish(dry, cfg, 2.4, 0.38, 3)
    return L, R


BUILDERS = {'base': build_base, 'groove': build_groove, 'lead': build_lead}


def render_layer(name, layer, tmp):
    cfg = SETS[name]
    L, R = BUILDERS[layer](cfg)
    # volumen de la capa: se fija por RMS (distinto por capa) y se evita que pase de 0.8 de pico
    stereo = np.stack([L, R], axis=1)
    rms = 20 * np.log10(max(float(np.sqrt(np.mean(stereo ** 2))), 1e-9))
    stereo = stereo * db(cfg.rms[layer] - rms)
    peak = float(np.max(np.abs(stereo)))
    if peak > 0.8:
        stereo = soft_clip(stereo / 0.8, 1.2) * 0.8
    # empalme del bucle: 2 ms de fundido en los extremos nunca estorba (la cola ya está plegada)
    wav = os.path.join(tmp, f'music_{name}_{layer}.wav')
    to_wav(wav, stereo)
    ogg = os.path.join(RAW, f'music_{name}_{layer}.ogg')
    to_ogg(wav, ogg, float(os.environ.get('MUSIC_Q', '1.0')), sr=OUT_SR, channels=2)
    return cfg.length, os.path.getsize(ogg), peak, rms


def main(argv):
    only = argv[1] if len(argv) > 1 else ''
    os.makedirs(RAW, exist_ok=True)
    total = 0
    with tempfile.TemporaryDirectory() as tmp:
        for name in SETS:
            if only and name != only:
                continue
            for layer in BUILDERS:
                dur, size, peak, rms = render_layer(name, layer, tmp)
                total += size
                print(f'{name:10s} {layer:7s} {dur:5.1f}s {size / 1024:6.0f} KB  pico={peak:.2f} rms={rms:.1f}dB')
    print(f'\nmúsica: {total / 1024:.0f} KB')


if __name__ == '__main__':
    main(sys.argv)
