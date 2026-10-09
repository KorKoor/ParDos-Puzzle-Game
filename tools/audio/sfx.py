# -*- coding: utf-8 -*-
"""
Genera todos los efectos de sonido de ParDos en app/src/main/res/raw/*.ogg

    python tools/audio/sfx.py            # todos
    python tools/audio/sfx.py ui_tap     # solo los que empiecen con ese nombre

Cada efecto se normaliza a un pico fijo (el volumen final lo reparte la app en GameAudio.kt).
"""
import json
import os
import sys
import tempfile

import numpy as np

from synth import *  # noqa: F401,F403

HERE = os.path.dirname(os.path.abspath(__file__))
RAW = os.path.normpath(os.path.join(HERE, '..', '..', 'app', 'src', 'main', 'res', 'raw'))

REGISTRY = []  # (nombre, peak, función)


def sfx(name, peak=0.8):
    def deco(fn):
        REGISTRY.append((name, peak, fn))
        return fn
    return deco


# Escala pentatónica mayor de Do: las notas de las fusiones (todas suenan bien juntas)
PENTA = [0, 2, 4, 7, 9]
MERGE_LEVELS = 12


def penta_midi(i, base=60):
    return base + 12 * (i // 5) + PENTA[i % 5]


INSTR = dict(VOICES)
INSTR['celesta'] = celesta


def seq(voice, notes, step, dur=None, gain=1.0, total=None, decay_gain=1.0):
    """Notas una detrás de otra (midi). Cada una se solapa con la siguiente."""
    fn = INSTR[voice] if isinstance(voice, str) else voice
    tot = total if total else step * len(notes) + (dur or 0.8)
    out = np.zeros(N(tot))
    for k, m in enumerate(notes):
        note = fn(midi(m), dur) if dur else fn(midi(m))
        place(out, note, k * step, gain * (decay_gain ** k))
    return out


def chord(voice, notes, dur=None, gain=1.0, stagger=0.0):
    fn = INSTR[voice] if isinstance(voice, str) else voice
    ex = len(notes) * stagger
    first = fn(midi(notes[0]), dur) if dur else fn(midi(notes[0]))
    out = np.zeros(len(first) + N(ex))
    for k, m in enumerate(notes):
        note = fn(midi(m), dur) if dur else fn(midi(m))
        place(out, note, k * stagger, gain / np.sqrt(len(notes)))
    return out


def plus(*xs):
    """Suma señales de distinto largo (la salida mide lo que la más larga)."""
    n = max(len(x) for x in xs)
    out = np.zeros(n)
    for x in xs:
        out[:len(x)] += x
    return out


def mix_at(total, parts):
    """parts = [(señal, inicio_s, ganancia)]"""
    out = np.zeros(N(total))
    for sig, at, g in parts:
        place(out, sig, at, g)
    return out


def tone_glide(f0, f1, dur, decay=8.0, shape='sine'):
    t = T(dur)
    fr = f0 + (f1 - f0) * (1 - np.exp(-t * 14))
    ph = 2 * np.pi * np.cumsum(fr) / SR
    y = np.sin(ph) if shape == 'sine' else np.tanh(2.5 * np.sin(ph))
    return fade(y * env_exp(len(t), decay, 0.002))


# ======================================================================= INTERFAZ

@sfx('ui_tap', 0.5)
def _():
    return mix_at(0.16, [(marimba(midi(76), 0.15), 0, 0.8), (kalimba(midi(88), 0.1), 0, 0.22)])


@sfx('ui_tap2', 0.5)
def _():
    return mix_at(0.16, [(marimba(midi(81), 0.15), 0, 0.8), (kalimba(midi(93), 0.1), 0, 0.2)])


@sfx('ui_back', 0.5)
def _():
    return mix_at(0.3, [(marimba(midi(76), 0.14), 0, 0.8), (marimba(midi(69), 0.2), 0.075, 0.8)])


@sfx('ui_open', 0.6)
def _():
    return mix_at(0.6, [(whoosh(0.24, 500, 3000, 21), 0, 0.9), (glass(midi(96), 0.5), 0.13, 0.28)])


@sfx('ui_close', 0.55)
def _():
    return mix_at(0.4, [(whoosh(0.2, 2800, 450, 22), 0, 0.9), (marimba(midi(72), 0.16), 0.02, 0.35)])


@sfx('ui_on', 0.5)
def _():
    return mix_at(0.35, [(kalimba(midi(76), 0.25), 0, 0.8), (kalimba(midi(83), 0.3), 0.07, 0.9)])


@sfx('ui_off', 0.45)
def _():
    return mix_at(0.35, [(kalimba(midi(83), 0.25), 0, 0.8), (kalimba(midi(76), 0.3), 0.07, 0.8)])


@sfx('ui_tab', 0.45)
def _():
    return xylophone(midi(86), 0.09)


@sfx('ui_error', 0.55)
def _():
    return mix_at(0.35, [(tone_glide(210, 150, 0.3, 9, 'soft'), 0, 0.9), (lowpass(noise(N(0.05), 4), 900, 2) * env_exp(N(0.05), 70), 0, 0.5)])


@sfx('ui_locked', 0.5)
def _():
    n = N(0.18)
    thud = lowpass(noise(n, 5), 400, 3) * env_exp(n, 40, 0.001)
    return fade(thud * 0.9 + tone_glide(110, 80, 0.18, 22)[:n] * 0.8)


@sfx('ui_whoosh', 0.5)
def _():
    return whoosh(0.38, 350, 2800, 23)


@sfx('ui_notify', 0.7)
def _():
    dry = mix_at(0.8, [(glass(midi(91), 0.6), 0, 0.8), (glass(midi(96), 0.7), 0.13, 0.9)])
    return reverb(dry, 0.8, 0.22)


@sfx('ui_confirm', 0.7)
def _():
    dry = mix_at(0.9, [(marimba(midi(72), 0.3), 0, 0.8), (marimba(midi(76), 0.3), 0.07, 0.8), (marimba(midi(79), 0.35), 0.14, 0.85),
                       (glass(midi(96), 0.7), 0.22, 0.35)])
    return reverb(dry, 0.9, 0.2)


@sfx('ui_tick', 0.35)
def _():
    return xylophone(midi(96), 0.04)


@sfx('ui_popup', 0.55)
def _():
    return mix_at(0.4, [(bloop(midi(72), 0.25), 0, 0.8), (sparkle(0.3, 5, 3000, 7000, 8, 0.3), 0.03, 0.6)])


# ======================================================================= TABLERO

@sfx('swipe', 0.32)
def _():
    n = N(0.17)
    x = pink(n, 41)
    y = bandpass(x, 450, 1700, 2) * np.sin(np.pi * np.linspace(0, 1, n)) ** 1.6
    return fade(y)


@sfx('spawn', 0.35)
def _():
    return bloop(midi(84), 0.12)


@sfx('merge_sub', 0.6)
def _():
    return fade(tone_glide(110, 58, 0.3, 9))


@sfx('merge_sparkle', 0.55)
def _():
    return sparkle(0.6, 9, 3000, 8000, 13, 0.5)


@sfx('combo_spark', 0.7)
def _():
    notes = [72, 74, 76, 79, 81, 84, 86]
    dry = seq('glass', notes, 0.045, 0.35, 1.0, 0.8, 0.96)
    return reverb(plus(dry, shimmer(0.7, 2093, 3) * 0.3), 0.7, 0.2)


@sfx('combo_1', 0.8)
def _():
    dry = seq('glass', [72, 76, 79], 0.06, 0.8, 0.9, 1.0)
    return reverb(dry, 0.9, 0.25)


@sfx('combo_2', 0.85)
def _():
    dry = plus(seq('glass', [72, 76, 79, 84], 0.055, 0.9, 0.9, 1.2), shimmer(1.0, 1568, 4) * 0.25)
    return reverb(dry, 1.0, 0.25)


@sfx('combo_3', 0.9)
def _():
    dry = plus(seq('glass', [76, 79, 84, 88], 0.05, 1.0, 0.9, 1.4), swell(midi(60), 1.3, 0.004, 3, (1.0, 0.5, 0.2), 0.25, 0.7) * 0.12,
               sparkle(1.0, 16, 2500, 8000, 6, 0.4))
    return reverb(dry, 1.2, 0.28)


@sfx('combo_4', 0.95)
def _():
    parts = [(chord('glass', [72, 79, 84, 88], 1.3, 1.0), 0.0, 0.8), (seq('glass', [84, 88, 91, 96], 0.05, 1.0, 0.8, 1.2), 0.0, 0.9),
             (swell(midi(48), 1.6, 0.004, 3, (1.0, 0.5, 0.25), 0.3, 0.8), 0.0, 0.18), (sparkle(1.3, 22, 2500, 9000, 7, 0.45), 0.0, 1.0),
             (whoosh(0.5, 400, 5000, 24), 0.0, 0.5), (fade(tone_glide(120, 60, 0.5, 6)), 0.0, 0.5)]
    return reverb(mix_at(1.8, parts), 1.5, 0.3)


@sfx('flow_in', 0.9)
def _():
    parts = [(whoosh(0.6, 250, 4800, 31, 0.8), 0, 1.0), (seq('glass', [72, 79, 84, 88], 0.09, 1.2, 0.8, 1.4), 0.2, 0.9),
             (shimmer(1.2, 1760, 8), 0.3, 0.5), (fade(tone_glide(130, 65, 0.4, 6)), 0.0, 0.5)]
    return reverb(mix_at(1.7, parts), 1.3, 0.28)


@sfx('flow_tier', 0.85)
def _():
    parts = [(whoosh(0.4, 400, 4000, 32, 0.7), 0, 0.8), (seq('glass', [76, 83, 88], 0.07, 0.9, 0.9, 1.0), 0.12, 0.9),
             (sparkle(0.8, 10, 3000, 8000, 9, 0.4), 0.2, 0.8)]
    return reverb(mix_at(1.2, parts), 1.0, 0.25)


@sfx('flow_out', 0.5)
def _():
    return mix_at(0.7, [(whoosh(0.5, 3500, 300, 33, 0.3), 0, 0.7), (glass(midi(72), 0.6), 0, 0.2)])


@sfx('ms_small', 0.9)
def _():
    dry = plus(seq('glass', [72, 76, 79, 84], 0.07, 1.0, 0.9, 1.3), shimmer(1.1, 1568, 13) * 0.3)
    return reverb(dry, 1.2, 0.28)


@sfx('ms_mid', 0.95)
def _():
    parts = [(seq('glass', [67, 72, 76, 79, 84], 0.07, 1.2, 0.9, 1.6), 0, 0.9), (chord('celesta', [60, 67, 72, 76], 1.4, 0.8), 0.28, 0.7),
             (swell(midi(48), 1.7, 0.004, 3, (1.0, 0.5, 0.2), 0.3, 0.8), 0.1, 0.16), (sparkle(1.2, 20, 2500, 9000, 14, 0.45), 0.1, 1.0)]
    return reverb(mix_at(2.0, parts), 1.4, 0.3)


@sfx('ms_big', 1.0)
def _():
    parts = [(gong(98, 2.6), 0, 0.5), (seq('glass', [60, 67, 72, 76, 79, 84, 88, 91], 0.08, 1.4, 0.85, 2.0), 0.1, 0.85),
             (chord('celesta', [60, 64, 67, 72, 76], 1.8, 0.9), 0.6, 0.7), (swell(midi(48), 2.4, 0.005, 3, (1.0, 0.55, 0.3), 0.5, 1.0), 0.0, 0.2),
             (sparkle(2.0, 34, 2200, 9500, 15, 0.5), 0.2, 1.0), (shimmer(1.8, 1568, 14), 0.5, 0.45)]
    return reverb(mix_at(3.0, parts), 1.8, 0.32)


@sfx('goal_ping', 0.7)
def _():
    return reverb(mix_at(0.7, [(glass(midi(91), 0.6), 0, 0.9)]), 0.6, 0.18)


@sfx('goal_done', 0.85)
def _():
    dry = mix_at(1.2, [(glass(midi(84), 0.7), 0, 0.8), (glass(midi(91), 0.9), 0.1, 0.9), (sparkle(0.8, 8, 3000, 8000, 17, 0.4), 0.1, 0.8)])
    return reverb(dry, 1.0, 0.25)


@sfx('star_1', 0.85)
def _():
    return reverb(mix_at(1.0, [(glass(midi(84), 0.9), 0, 0.9), (sparkle(0.6, 6, 3000, 7000, 18, 0.4), 0, 0.7)]), 0.9, 0.25)


@sfx('star_2', 0.88)
def _():
    return reverb(mix_at(1.0, [(glass(midi(88), 0.9), 0, 0.9), (sparkle(0.7, 8, 3000, 8000, 19, 0.45), 0, 0.8)]), 0.9, 0.25)


@sfx('star_3', 0.95)
def _():
    parts = [(glass(midi(91), 1.1), 0, 0.9), (glass(midi(96), 1.2), 0.0, 0.5), (sparkle(1.0, 14, 2500, 9000, 20, 0.5), 0, 0.9), (shimmer(1.0, 1976, 15), 0.1, 0.4)]
    return reverb(mix_at(1.6, parts), 1.2, 0.28)


@sfx('win', 0.95)
def _():
    arp = seq('marimba', [60, 64, 67, 72, 76, 79, 84], 0.085, 0.5, 0.9, 1.3)
    bells = chord('glass', [72, 76, 79, 84], 1.6, 0.9)
    parts = [(arp, 0, 1.0), (bells, 0.5, 0.85), (swell(midi(48), 2.0, 0.004, 3, (1.0, 0.5, 0.2), 0.25, 0.9), 0.4, 0.18),
             (sparkle(1.6, 26, 2200, 9000, 21, 0.5), 0.5, 1.0)]
    return reverb(mix_at(2.8, parts), 1.5, 0.3)


@sfx('win_big', 1.0)
def _():
    arp = seq('marimba', [60, 64, 67, 72, 76, 79, 84, 88, 91], 0.075, 0.5, 0.9, 1.4)
    bells = chord('glass', [72, 76, 79, 84, 88], 1.8, 0.9)
    parts = [(arp, 0, 1.0), (bells, 0.55, 0.9), (swell(midi(48), 2.6, 0.005, 3, (1.0, 0.55, 0.25), 0.3, 1.0), 0.4, 0.2),
             (sparkle(2.0, 36, 2200, 9500, 22, 0.5), 0.5, 1.0), (chord('celesta', [76, 79, 84, 88], 1.6, 0.8), 0.9, 0.6),
             (shimmer(1.8, 1760, 16), 0.9, 0.4), (taiko(65, 1.0), 0.55, 0.35)]
    return reverb(mix_at(3.6, parts), 1.8, 0.32)


@sfx('fail', 0.55)
def _():
    dry = plus(seq('kalimba', [64, 62, 59], 0.22, 1.0, 0.8, 1.7, 0.9), swell(midi(52), 1.5, 0.003, 3, (1.0, 0.3, 0.1), 0.3, 0.8) * 0.14)
    return reverb(dry, 1.3, 0.28)


@sfx('near_miss', 0.5)
def _():
    dry = seq('kalimba', [67, 64], 0.2, 0.9, 0.8, 1.2)
    return reverb(dry, 1.0, 0.25)


@sfx('tick_warn', 0.5)
def _():
    return mix_at(0.2, [(xylophone(midi(72), 0.1), 0, 0.8), (xylophone(midi(72), 0.1), 0.1, 0.5)])


@sfx('heartbeat', 0.6)
def _():
    return mix_at(0.55, [(fade(tone_glide(85, 55, 0.2, 14)), 0, 0.9), (fade(tone_glide(75, 50, 0.2, 14)), 0.17, 0.7)])


@sfx('undo', 0.6)
def _():
    parts = [(whoosh(0.35, 3000, 400, 34, 0.3), 0, 0.8), (seq('kalimba', [79, 76, 72], 0.07, 0.3, 0.8, 0.6), 0.02, 0.6)]
    return mix_at(0.6, parts)


@sfx('hammer', 0.85)
def _():
    n = N(0.6)
    hit = fade(tone_glide(150, 55, 0.3, 11)) * 1.0
    crack = bandpass(noise(n, 50), 1500, 9000) * env_exp(n, 22, 0.001)
    tinkle = sparkle(0.5, 12, 3000, 9000, 51, 0.5)
    return mix_at(0.7, [(hit, 0, 0.9), (crack, 0, 0.6), (tinkle, 0.05, 0.7)])


@sfx('shuffle', 0.55)
def _():
    out = np.zeros(N(0.6))
    r = np.random.RandomState(52)
    for k in range(22):
        at = k * 0.024 + 0.01 * r.rand()
        n = N(0.025)
        tick = bandpass(noise(n, 60 + k), 1500, 7000) * env_exp(n, 160, 0.0008)
        place(out, tick, at, 0.5 + 0.5 * r.rand())
    return out


@sfx('magic', 0.8)
def _():
    parts = [(zap(midi(84), 0.5), 0, 0.8), (seq('glass', [79, 84, 88, 91], 0.06, 0.6, 0.8, 1.1), 0.08, 0.7), (sparkle(0.7, 12, 3000, 9000, 53, 0.5), 0.1, 0.8)]
    return reverb(mix_at(1.1, parts), 0.9, 0.25)


@sfx('freeze', 0.7)
def _():
    parts = [(chord('glass', [88, 91, 95], 1.0, 0.9), 0, 0.8), (sparkle(0.8, 16, 4000, 10000, 54, 0.5), 0, 0.8), (whoosh(0.5, 4000, 1500, 55), 0, 0.35)]
    return reverb(mix_at(1.3, parts), 1.1, 0.28)


@sfx('revive', 0.85)
def _():
    parts = [(swell(midi(60), 1.6, 0.004, 3, (1.0, 0.5, 0.2), 0.5, 0.7), 0, 0.5), (seq('glass', [72, 76, 79, 84, 88], 0.1, 1.2, 0.8, 1.2), 0.3, 0.9),
             (sparkle(1.4, 24, 2500, 9000, 56, 0.5), 0.4, 1.0)]
    return reverb(mix_at(2.4, parts), 1.5, 0.3)


@sfx('board_clear', 0.9)
def _():
    parts = [(whoosh(0.8, 300, 5500, 57, 0.7), 0, 0.9), (sparkle(1.0, 22, 2500, 9000, 58, 0.5), 0.2, 1.0),
             (seq('glass', [72, 79, 84, 91], 0.08, 1.0, 0.8, 1.2), 0.4, 0.7)]
    return reverb(mix_at(1.8, parts), 1.2, 0.28)


@sfx('stone_bump', 0.6)
def _():
    n = N(0.16)
    return fade(lowpass(noise(n, 59), 600, 3) * env_exp(n, 38, 0.001) * 0.8 + tone_glide(95, 70, 0.16, 25)[:n] * 0.8)


@sfx('blocked', 0.5)
def _():
    return mix_at(0.35, [(marimba(midi(64), 0.18), 0, 0.8), (marimba(midi(62), 0.22), 0.1, 0.8)])


@sfx('storm_warn', 0.65)
def _():
    n = N(1.0)
    x = lowpass(noise(n, 61), 180, 2)
    env = np.sin(np.pi * np.linspace(0, 1, n)) ** 1.3
    return fade(x * env * 4.0)


@sfx('storm_hit', 0.7)
def _():
    n = N(0.3)
    return fade(lowpass(noise(n, 62), 500, 3) * env_exp(n, 22, 0.001) + tone_glide(80, 50, 0.3, 14)[:n])


@sfx('boss_intro', 1.0)
def _():
    parts = [(gong(73, 3.2), 0, 0.8), (swell(midi(36), 3.0, 0.004, 3, (1.0, 0.6, 0.3), 1.2, 1.2), 0, 0.35),
             (whoosh(1.2, 120, 2500, 63, 0.9), 0, 0.4), (taiko(60, 1.2), 0.0, 0.55)]
    return reverb(mix_at(3.6, parts), 1.8, 0.3)


@sfx('boss_phase', 0.9)
def _():
    n = N(1.0)
    t = T(1.0)
    fr = 220 + 700 * (t / 1.0) ** 1.5
    ph = 2 * np.pi * np.cumsum(fr) / SR
    sirenish = lowpass(np.tanh(2.0 * np.sin(ph)), 2400, 2) * (0.6 + 0.4 * np.sin(2 * np.pi * 7 * t)) * np.minimum(1, t / 0.2) * np.minimum(1, (1 - t) / 0.1)
    return mix_at(1.4, [(sirenish, 0, 0.5), (taiko(70, 1.0), 0.95, 0.9), (gong(98, 1.4), 0.95, 0.4)])


@sfx('boss_win', 1.0)
def _():
    brass = np.zeros(N(3.4))
    for k, (m, at) in enumerate([(60, 0.0), (67, 0.25), (72, 0.5), (76, 0.75), (79, 1.0)]):
        place(brass, lowpass(saw(midi(m), 1.4) * env_adsr(N(1.4), 0.05, 0.2, 0.7, 0.5), 2800, 2), at, 0.35)
    parts = [(brass, 0, 0.8), (seq('glass', [72, 76, 79, 84, 88, 91, 96], 0.09, 1.4, 0.8, 2.0), 0.8, 0.8), (taiko(65, 1.2), 0.0, 0.6), (taiko(80, 1.2), 0.5, 0.5),
             (sparkle(2.0, 40, 2200, 9500, 64, 0.5), 0.9, 1.0), (gong(98, 2.6), 1.0, 0.4)]
    return reverb(mix_at(4.0, parts), 1.9, 0.32)


@sfx('tower_floor', 0.8)
def _():
    dry = seq('marimba', [67, 72, 76, 79], 0.07, 0.4, 0.9, 1.0)
    return reverb(dry, 0.9, 0.22)


@sfx('heart_lost', 0.75)
def _():
    n = N(0.5)
    shatter = bandpass(noise(n, 65), 2500, 10000) * env_exp(n, 18, 0.001)
    return mix_at(0.9, [(shatter, 0, 0.6), (sparkle(0.6, 10, 3500, 9500, 66, 0.4), 0.02, 0.7), (seq('kalimba', [64, 57], 0.18, 0.7, 0.8, 0.9), 0.1, 0.6)])


@sfx('new_best', 0.9)
def _():
    parts = [(seq('glass', [72, 79, 84, 88, 91], 0.08, 1.2, 0.9, 1.5), 0, 0.9), (sparkle(1.4, 26, 2200, 9000, 67, 0.5), 0.1, 1.0), (shimmer(1.2, 1760, 17), 0.2, 0.4)]
    return reverb(mix_at(2.2, parts), 1.4, 0.3)


# ======================================================================= RECOMPENSAS Y META

def coin_ding(pitch=0):
    return mix_at(0.5, [(glass(midi(95 + pitch), 0.45), 0, 0.7), (glass(midi(100 + pitch), 0.5), 0.055, 0.6)])


@sfx('coin', 0.7)
def _():
    return reverb(coin_ding(0), 0.5, 0.15)


@sfx('coins', 0.8)
def _():
    r = np.random.RandomState(70)
    out = np.zeros(N(0.9))
    for k in range(9):
        place(out, coin_ding(int(r.randint(-2, 3))), k * 0.05 + r.rand() * 0.02, 0.5 + 0.4 * r.rand())
    return reverb(out, 0.6, 0.18)


@sfx('gem', 0.8)
def _():
    parts = [(glass(midi(100), 0.9), 0, 0.8), (glass(midi(107), 1.0), 0.05, 0.6), (sparkle(0.7, 10, 4000, 10000, 71, 0.5), 0.05, 0.8)]
    return reverb(mix_at(1.3, parts), 0.9, 0.25)


@sfx('xp_tick', 0.4)
def _():
    return xylophone(midi(88), 0.05)


@sfx('claim', 0.85)
def _():
    r = np.random.RandomState(72)
    out = np.zeros(N(1.0))
    for k in range(7):
        place(out, coin_ding(int(r.randint(-3, 4))), 0.12 + k * 0.06, 0.6)
    place(out, seq('marimba', [72, 79, 84], 0.06, 0.4, 0.9, 0.6), 0.0, 0.9)
    return reverb(out, 0.8, 0.2)


@sfx('level_up', 0.95)
def _():
    parts = [(seq('marimba', [67, 72, 76, 79, 84], 0.08, 0.5, 0.9, 1.1), 0, 1.0), (chord('glass', [72, 76, 79, 84], 1.4, 0.9), 0.45, 0.85),
             (sparkle(1.3, 22, 2400, 9000, 73, 0.5), 0.4, 1.0), (swell(midi(48), 1.8, 0.004, 3, (1.0, 0.5, 0.2), 0.25, 0.8), 0.3, 0.16)]
    return reverb(mix_at(2.4, parts), 1.4, 0.3)


@sfx('rank_up', 1.0)
def _():
    parts = [(gong(98, 2.4), 0, 0.35), (seq('marimba', [60, 64, 67, 72, 76, 79, 84], 0.08, 0.5, 0.9, 1.3), 0.1, 0.9), (chord('glass', [72, 76, 79, 84, 88], 1.8, 0.9), 0.7, 0.9),
             (sparkle(2.0, 34, 2200, 9500, 74, 0.5), 0.5, 1.0), (swell(midi(48), 2.6, 0.005, 3, (1.0, 0.55, 0.25), 0.4, 1.0), 0.3, 0.2),
             (shimmer(1.6, 1760, 18), 0.8, 0.4), (taiko(70, 1.0), 0.0, 0.4)]
    return reverb(mix_at(3.4, parts), 1.8, 0.32)


@sfx('platinum', 1.0)
def _():
    parts = [(gong(82, 3.0), 0, 0.45), (seq('glass', [72, 79, 84, 88, 91, 96, 100], 0.1, 1.6, 0.85, 1.9), 0.1, 0.9),
             (chord('celesta', [72, 76, 79, 84, 88, 91], 2.0, 0.9), 0.9, 0.8), (sparkle(2.6, 50, 2200, 10000, 75, 0.55), 0.3, 1.0),
             (swell(midi(48), 3.0, 0.005, 3, (1.0, 0.6, 0.3), 0.6, 1.2), 0.2, 0.22), (shimmer(2.4, 1976, 19), 0.6, 0.5), (taiko(65, 1.0), 0.0, 0.5)]
    return reverb(mix_at(4.4, parts), 2.0, 0.35)


@sfx('achievement', 0.9)
def _():
    parts = [(seq('glass', [79, 84, 88, 91], 0.1, 1.0, 0.9, 1.3), 0, 0.9), (sparkle(1.2, 20, 2500, 9000, 76, 0.5), 0.1, 1.0), (marimba(midi(60), 0.6), 0, 0.5)]
    return reverb(mix_at(1.9, parts), 1.3, 0.28)


@sfx('mission_done', 0.8)
def _():
    dry = mix_at(0.9, [(marimba(midi(72), 0.35), 0, 0.8), (marimba(midi(79), 0.45), 0.09, 0.85), (glass(midi(91), 0.6), 0.14, 0.4)])
    return reverb(dry, 0.8, 0.2)


@sfx('daily', 0.9)
def _():
    parts = [(seq('kalimba', [72, 76, 79, 84, 88], 0.09, 0.6, 0.9, 1.0), 0, 0.9), (sparkle(1.0, 14, 2500, 8000, 77, 0.45), 0.2, 0.9),
             (chord('glass', [79, 84, 88], 1.0, 0.6), 0.5, 0.7)]
    return reverb(mix_at(1.8, parts), 1.2, 0.28)


@sfx('streak', 0.8)
def _():
    parts = [(whoosh(0.5, 300, 3500, 78, 0.7), 0, 0.7), (marimba(midi(76), 0.5), 0.3, 0.8), (glass(midi(88), 0.9), 0.38, 0.6), (sparkle(0.7, 10, 3000, 8000, 79, 0.4), 0.35, 0.8)]
    return reverb(mix_at(1.3, parts), 1.0, 0.25)


@sfx('chest_shake', 0.7)
def _():
    out = np.zeros(N(0.7))
    r = np.random.RandomState(80)
    for k in range(5):
        at = 0.02 + k * 0.12 + 0.01 * r.rand()
        n = N(0.09)
        thump = lowpass(noise(n, 81 + k), 900, 2) * env_exp(n, 40, 0.001) + fade(tone_glide(140, 90, 0.09, 30))[:n] * 0.6
        place(out, thump, at, 0.6 + 0.4 * r.rand())
        place(out, bandpass(noise(N(0.03), 90 + k), 2000, 8000) * env_exp(N(0.03), 100), at, 0.3)
    return out


@sfx('chest_open', 0.95)
def _():
    parts = [(fade(tone_glide(90, 45, 0.25, 10)), 0, 0.6), (whoosh(0.4, 400, 5000, 82, 0.8), 0.02, 0.7), (seq('glass', [79, 84, 88, 91], 0.06, 0.9, 0.85, 1.1), 0.1, 0.8),
             (sparkle(1.2, 26, 2500, 9500, 83, 0.5), 0.1, 1.0)]
    return reverb(mix_at(1.9, parts), 1.2, 0.28)


@sfx('card_flip', 0.55)
def _():
    n = N(0.16)
    x = pink(n, 84)
    return fade(bandpass(x, 800, 6000, 2) * np.sin(np.pi * np.linspace(0, 1, n)) ** 0.8 * 1.4)


@sfx('card_common', 0.7)
def _():
    return reverb(mix_at(0.7, [(marimba(midi(84), 0.5), 0, 0.8), (glass(midi(96), 0.4), 0.02, 0.2)]), 0.6, 0.18)


@sfx('card_rare', 0.8)
def _():
    parts = [(glass(midi(88), 0.9), 0, 0.8), (glass(midi(95), 0.9), 0.08, 0.6), (sparkle(0.7, 10, 3000, 8000, 85, 0.4), 0, 0.8)]
    return reverb(mix_at(1.2, parts), 1.0, 0.25)


@sfx('card_epic', 0.9)
def _():
    parts = [(seq('glass', [79, 84, 88, 91], 0.07, 1.0, 0.85, 1.2), 0, 0.9), (swell(midi(55), 1.5, 0.004, 3, (1.0, 0.5, 0.2), 0.3, 0.6), 0, 0.2),
             (sparkle(1.1, 20, 2400, 9000, 86, 0.5), 0.05, 1.0), (whoosh(0.5, 400, 4500, 87), 0, 0.4)]
    return reverb(mix_at(1.8, parts), 1.3, 0.3)


@sfx('card_legend', 1.0)
def _():
    parts = [(gong(98, 2.2), 0, 0.3), (seq('glass', [72, 79, 84, 88, 91, 96], 0.08, 1.4, 0.85, 1.6), 0.05, 0.9),
             (chord('celesta', [72, 76, 79, 84], 1.6, 0.8), 0.5, 0.7), (swell(midi(48), 2.2, 0.005, 3, (1.0, 0.55, 0.25), 0.4, 0.9), 0.1, 0.22),
             (sparkle(1.8, 34, 2200, 9500, 88, 0.55), 0.2, 1.0), (shimmer(1.6, 1760, 20), 0.4, 0.4), (fade(tone_glide(90, 45, 0.5, 6)), 0, 0.5)]
    return reverb(mix_at(3.0, parts), 1.7, 0.32)


@sfx('card_new', 0.75)
def _():
    return reverb(seq('marimba', [84, 88, 91], 0.06, 0.4, 0.9, 0.8), 0.7, 0.2)


@sfx('foil', 0.85)
def _():
    parts = [(shimmer(1.4, 1568, 21), 0, 0.8), (seq('glass', [84, 88, 91, 96, 100], 0.09, 1.0, 0.7, 1.7), 0.1, 0.8), (sparkle(1.4, 30, 3000, 10000, 89, 0.5), 0, 0.9)]
    return reverb(mix_at(2.0, parts), 1.5, 0.3)


@sfx('sell', 0.75)
def _():
    r = np.random.RandomState(90)
    out = np.zeros(N(0.8))
    for k in range(6):
        place(out, coin_ding(int(r.randint(-1, 3))), k * 0.05, 0.5 + 0.4 * r.rand())
    place(out, whoosh(0.25, 800, 2200, 91), 0, 0.3)
    return reverb(out, 0.5, 0.15)


@sfx('trade_send', 0.7)
def _():
    parts = [(whoosh(0.35, 500, 3500, 92), 0, 0.8), (seq('kalimba', [76, 83, 88], 0.07, 0.4, 0.9, 0.7), 0.18, 0.8)]
    return reverb(mix_at(0.9, parts), 0.7, 0.2)


@sfx('trade_done', 0.85)
def _():
    parts = [(seq('glass', [79, 84], 0.12, 0.8, 0.9, 1.0), 0, 0.9), (seq('marimba', [72, 79, 84], 0.07, 0.4, 0.9, 0.7), 0.24, 0.8), (sparkle(0.9, 14, 3000, 9000, 93, 0.5), 0.2, 0.9)]
    return reverb(mix_at(1.5, parts), 1.0, 0.25)


@sfx('wheel_tick', 0.45)
def _():
    n = N(0.045)
    return fade(bandpass(noise(n, 94), 1500, 5000) * env_exp(n, 120, 0.0005) + xylophone(midi(91), 0.05)[:n] * 0.7)


@sfx('wheel_stop', 0.7)
def _():
    return mix_at(0.8, [(fade(tone_glide(130, 80, 0.2, 18)), 0, 0.8), (glass(midi(84), 0.7), 0.03, 0.5)])


@sfx('wheel_win', 0.95)
def _():
    parts = [(seq('marimba', [72, 76, 79, 84, 88, 91], 0.07, 0.4, 0.9, 1.1), 0, 0.9), (chord('glass', [79, 84, 88], 1.4, 0.8), 0.45, 0.8), (sparkle(1.4, 28, 2400, 9500, 95, 0.5), 0.3, 1.0)]
    out = mix_at(2.0, parts)
    r = np.random.RandomState(96)
    for k in range(8):
        place(out, coin_ding(int(r.randint(-2, 3))), 0.6 + k * 0.07, 0.4)
    return reverb(out, 1.3, 0.28)


@sfx('piggy', 1.0)
def _():
    n = N(0.45)
    glassbreak = bandpass(noise(n, 97), 2500, 11000) * env_exp(n, 14, 0.001)
    r = np.random.RandomState(98)
    out = mix_at(1.8, [(glassbreak, 0, 0.8), (fade(tone_glide(120, 60, 0.3, 12)), 0, 0.7), (sparkle(1.0, 18, 3000, 10000, 99, 0.5), 0, 0.8)])
    for k in range(14):
        place(out, coin_ding(int(r.randint(-3, 4))), 0.25 + k * 0.07, 0.5 + 0.3 * r.rand())
    return reverb(out, 1.0, 0.22)


@sfx('purchase', 1.0)
def _():
    r = np.random.RandomState(100)
    out = mix_at(1.8, [(bandpass(noise(N(0.05), 101), 1500, 7000) * env_exp(N(0.05), 90), 0, 0.5),
                       (glass(midi(96), 1.2), 0.04, 0.8), (glass(midi(103), 1.2), 0.09, 0.6), (seq('marimba', [72, 79, 84], 0.07, 0.4, 0.9, 0.7), 0.18, 0.7),
                       (sparkle(1.2, 20, 2500, 9000, 102, 0.5), 0.1, 0.9)])
    for k in range(10):
        place(out, coin_ding(int(r.randint(-3, 4))), 0.2 + k * 0.06, 0.4 + 0.3 * r.rand())
    return reverb(out, 1.0, 0.22)


@sfx('ad_reward', 0.85)
def _():
    parts = [(seq('glass', [79, 84, 88], 0.07, 0.9, 0.9, 1.0), 0, 0.9), (sparkle(1.0, 16, 2500, 9000, 103, 0.5), 0.05, 0.9), (shimmer(0.9, 1760, 22), 0.1, 0.35)]
    return reverb(mix_at(1.5, parts), 1.0, 0.25)


@sfx('gift', 0.8)
def _():
    parts = [(whoosh(0.25, 800, 4000, 104), 0, 0.6), (seq('kalimba', [76, 79, 84, 88], 0.07, 0.5, 0.9, 0.9), 0.15, 0.9), (sparkle(0.8, 12, 3000, 8500, 105, 0.45), 0.2, 0.8)]
    return reverb(mix_at(1.3, parts), 0.9, 0.24)


@sfx('season_tier', 0.8)
def _():
    out = mix_at(1.2, [(seq('marimba', [72, 76, 79], 0.07, 0.5, 0.9, 0.7), 0, 0.9), (glass(midi(91), 0.8), 0.16, 0.7), (sparkle(0.7, 10, 3000, 8500, 106, 0.4), 0.2, 0.8)])
    return reverb(out, 0.9, 0.25)


@sfx('reward_big', 0.95)
def _():
    parts = [(seq('marimba', [60, 64, 67, 72, 76, 79], 0.07, 0.5, 0.9, 1.0), 0, 0.9), (chord('glass', [72, 76, 79, 84], 1.5, 0.9), 0.4, 0.9),
             (sparkle(1.6, 28, 2300, 9500, 107, 0.5), 0.3, 1.0), (swell(midi(48), 2.0, 0.004, 3, (1.0, 0.5, 0.25), 0.3, 0.9), 0.2, 0.2)]
    return reverb(mix_at(2.6, parts), 1.4, 0.3)


@sfx('countdown', 0.6)
def _():
    return mix_at(0.4, [(marimba(midi(79), 0.3), 0, 0.9)])


@sfx('go', 0.8)
def _():
    return reverb(mix_at(0.9, [(marimba(midi(84), 0.4), 0, 0.8), (glass(midi(96), 0.7), 0.02, 0.5), (sparkle(0.5, 6, 3000, 8000, 108, 0.4), 0, 0.6)]), 0.8, 0.2)


# ======================================================================= VOCES DE FUSIÓN (una por efecto cosmético)

def merge_voices():
    out = []
    for voice in VOICES:
        for i in range(MERGE_LEVELS):
            m = penta_midi(i, 60)
            # las voces graves/suaves suenan mejor una octava más abajo en los niveles bajos; el glass, una abajo siempre
            if voice in ('glass', 'firebell'):
                m -= 12
                m = max(m, 55)
            if voice in ('bloop', 'koto', 'ripple'):
                m -= 5
            fn = VOICES[voice]
            # las notas de niveles altos duran más (se siente el peso)
            dur = 0.35 + 0.025 * i
            note = fn(midi(m), dur) if voice not in ('glass', 'firebell', 'ripple') else fn(midi(m), 0.6 + 0.05 * i)
            out.append((f'mg_{voice}_{i + 1:02d}', 0.8, note))
    return out


def render(name, peak, sig, tmp, quality=4):
    sig = np.asarray(sig, dtype=float)
    # recorta el silencio del final para que el sonido sea lo más corto posible
    thr = 1e-3 * (np.max(np.abs(sig)) + 1e-9)
    idx = np.where(np.abs(sig) > thr)[0]
    if len(idx):
        sig = sig[:idx[-1] + N(0.02)]
    sig = fade(sig, 1.0, 12.0)
    # Volumen parecido entre sonidos: se iguala el RMS (peak = "importancia": 1.0 fanfarria, 0.5 interfaz, 0.3 fondo)
    # y se limita el pico a 0.9 para que el codificador no recorte.
    target = -17.0 + 20.0 * np.log10(max(peak, 0.05))
    gain_rms = db(target - rms_db(sig))
    gain_peak = (0.85 * peak) / (float(np.max(np.abs(sig))) + 1e-9)  # los toquecitos cortos se notan por el pico, no por el RMS
    sig = sig * (max(gain_rms, gain_peak) if len(sig) < SR * 0.3 else gain_rms)
    m = float(np.max(np.abs(sig)))
    if m > 0.9:
        sig = sig * (0.9 / m)
    wav = os.path.join(tmp, name + '.wav')
    to_wav(wav, sig)
    ogg = os.path.join(RAW, name + '.ogg')
    to_ogg(wav, ogg, quality, channels=1)
    return len(sig) / SR, os.path.getsize(ogg)


def main(argv):
    only = argv[1] if len(argv) > 1 else ''
    os.makedirs(RAW, exist_ok=True)
    jobs = [(n, p, fn()) for n, p, fn in REGISTRY if n.startswith(only)]
    if not only or only.startswith('mg_'):
        jobs += [(n, p, s) for n, p, s in merge_voices() if n.startswith(only)]
    total = 0
    manifest = {}
    with tempfile.TemporaryDirectory() as tmp:
        for name, peak, sig in jobs:
            dur, size = render(name, peak, sig, tmp)
            total += size
            manifest[name] = round(dur, 3)
            print(f'{name:18s} {dur:5.2f}s {size / 1024:6.1f} KB')
    print(f'\n{len(jobs)} sonidos, {total / 1024:.0f} KB en {RAW}')
    with open(os.path.join(HERE, 'sfx_manifest.json'), 'w', encoding='utf-8') as f:
        json.dump(manifest, f, indent=1)


if __name__ == '__main__':
    main(sys.argv)
