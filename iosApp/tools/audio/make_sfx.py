"""
Efectos de sonido de ParDos. Uso:  python make_sfx.py        (escribe iosApp/ParDos/Sounds/sfx_<nombre>.mp3 e informe)
Calido, redondo y suave: marimba, kalimba, glockenspiel, campanitas, madera y agua.
"""
import io
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from synth import *  # noqa: E402,F403

OUT = os.path.normpath(os.path.join(HERE, "..", "..", "ParDos", "Sounds"))
SR = SR_SFX
RNG = np.random.RandomState(11)
S = {}


def sfx(fn):
    S[fn.__name__] = fn
    return fn


def K(n, d=0.5, v=1.0):
    return tone(note(n), d, KALIMBA, v)


def M(n, d=0.5, v=1.0):
    return tone(note(n), d, MARIMBA, v)


def G(n, d=0.8, v=1.0):
    return tone(note(n), d, GLOCK, v)


def B(n, d=1.4, v=1.0):
    return tone(note(n), d, BELL, v)


def W(hz, d=0.08, v=1.0):
    return tone(hz, d, WOOD, v)


def ping(hz, d=0.5, v=1.0):
    return tone(hz, d, [(1.0, 1.0, 9.0), (2.0, 0.5, 12.0), (3.0, 0.3, 16.0)], v)


def run(inst, names, step, d, v=1.0, at=0.0, gain=1.0):
    return [(at + i * step, inst(n, d, v), gain) for i, n in enumerate(names)]


def chord(inst, names, d, v=1.0, at=0.0, spread=0.012):
    return [(at + i * spread, inst(n, d, v / (1 + 0.15 * len(names))), 1.0) for i, n in enumerate(names)]


def sparkle(at=0.0, count=5, span=0.3, lo=2400, hi=5200, v=0.35):
    out = []
    for i in range(count):
        f = RNG.uniform(lo, hi)
        out.append((at + i * span / max(1, count) + RNG.uniform(0, 0.03), tone(f, 0.25, [(1, 1, 14.0), (2, 0.3, 20.0)], v), 1.0))
    return out


def done(items, rt=None, mix=0.25, tail=0.3):
    x = seq(items, SR, tail)
    if rt:
        x = reverb(x, SR, rt, mix)
    return x


# ------------------------------------------------------------------ interfaz

@sfx
def tap(): return done([(0, W(1250, 0.07, 0.9), 1), (0.0, K("C6", 0.1, 0.18), 1)])
@sfx
def tap_soft(): return done([(0, W(1700, 0.05, 0.5), 1)])
@sfx
def back(): return done(run(K, ["G5", "D5"], 0.06, 0.25, 0.8))
@sfx
def toggle_on(): return done(run(K, ["E5", "B5"], 0.07, 0.3, 0.8))
@sfx
def toggle_off(): return done(run(K, ["B5", "E5"], 0.07, 0.25, 0.7))
@sfx
def tab(): return done([(0, M("C5", 0.18, 0.8), 1), (0, W(900, 0.05, 0.4), 1)])
@sfx
def select(): return done([(0, G("E6", 0.35, 0.55), 1)])
@sfx
def deselect(): return done([(0, G("C6", 0.25, 0.4), 1)])
@sfx
def popup(): return done([(0, whoosh(0.16, 500, 2500, SR, 2), 0.35), (0.05, K("C5", 0.3, 0.8), 1), (0.09, K("G5", 0.35, 0.7), 1)])
@sfx
def dismiss(): return done([(0, whoosh(0.15, 2500, 500, SR, 4), 0.3), (0.02, K("G4", 0.25, 0.6), 1)])
@sfx
def error(): return done([(0, M("A3", 0.3, 0.8), 1), (0.11, M("F3", 0.4, 0.8), 1)])
@sfx
def locked(): return done([(0, W(170, 0.12, 1.0), 1), (0, M("D3", 0.25, 0.7), 1)])
@sfx
def swoosh(): return done([(0, whoosh(0.3, 400, 3500, SR, 5), 0.9)])
@sfx
def swoosh_big(): return done([(0, whoosh(0.5, 250, 5000, SR, 6), 1.0)], 0.7, 0.18)
@sfx
def notification(): return done([(0, B("E6", 1.0, 0.7), 1), (0.17, B("A6", 1.2, 0.6), 1)], 0.8, 0.22)
@sfx
def sheet_open(): return done([(0, whoosh(0.22, 400, 3000, SR, 7), 0.5), (0.12, G("C6", 0.5, 0.5), 1)])
@sfx
def sheet_close(): return done([(0, whoosh(0.2, 3000, 400, SR, 8), 0.45)])
@sfx
def success_small(): return done(run(K, ["E5", "G5"], 0.08, 0.35, 0.8) + [(0.16, G("C6", 0.6, 0.5), 1)], 0.5, 0.15)
@sfx
def typing(): return done([(0, W(2000, 0.03, 0.5), 1)])
@sfx
def slider_tick(): return done([(0, W(1800, 0.02, 0.45), 1)])
@sfx
def page_turn(): return done([(0, whoosh(0.2, 2000, 6000, SR, 9), 0.4)])
@sfx
def warning_soft(): return done(run(M, ["E4", "E4"], 0.14, 0.25, 0.7))


# ------------------------------------------------------------------ partida

@sfx
def merge_1(): return done([(0, M("C5", 0.3, 1.0), 1), (0, tone(260, 0.05, [(1, 1, 60.0)], 0.5), 1)])
@sfx
def merge_2(): return done([(0, M("E5", 0.3, 0.9), 1), (0.01, G("E6", 0.4, 0.3), 1)])
@sfx
def merge_3(): return done([(0, G("G5", 0.6, 0.8), 1), (0.01, B("G6", 0.8, 0.35), 1)], 0.5, 0.15)
@sfx
def merge_4(): return done(chord(G, ["C6", "E6", "G6", "C7"], 1.0, 0.9, spread=0.03) + [(0, B("C6", 1.2, 0.5), 1)], 0.9, 0.25)
@sfx
def merge_max(): return done(run(G, ["C5", "E5", "G5", "C6", "E6", "G6", "C7"], 0.065, 1.0, 0.8) + chord(B, ["C6", "G6", "E7"], 1.6, 0.8, at=0.45, spread=0.04) + sparkle(0.4, 9, 0.9, 2600, 6200, 0.3), 1.5, 0.3, 0.6)


_COMBO = ["C5", "D5", "E5", "G5", "A5", "C6"]


def _combo(i):
    notes = _COMBO[:i + 1]
    if i == 5:
        return done(run(G, ["C5", "E5", "G5", "C6", "E6"], 0.05, 0.7, 0.8) + [(0.25, B("C7", 1.0, 0.5), 1)] + sparkle(0.2, 5, 0.4), 0.8, 0.2)
    return done([(0, G(notes[-1], 0.55, 0.9), 1), (0, M(notes[0], 0.25, 0.4), 1)] + ([(0.05, K(notes[-2], 0.3, 0.5), 1)] if i else []), 0.4, 0.12)


@sfx
def combo_1(): return _combo(0)
@sfx
def combo_2(): return _combo(1)
@sfx
def combo_3(): return _combo(2)
@sfx
def combo_4(): return _combo(3)
@sfx
def combo_5(): return _combo(4)
@sfx
def combo_6(): return _combo(5)
@sfx
def tile_spawn(): return done([(0, K("C6", 0.15, 0.28), 1)])
@sfx
def slide_soft(): return done([(0, whoosh(0.1, 800, 2500, SR, 10), 0.3)])
@sfx
def blocked(): return done([(0, W(230, 0.1, 0.9), 1), (0, M("E3", 0.12, 0.5), 1)])
@sfx
def undo(): return done(run(K, ["G5", "E5", "C5"], 0.05, 0.3, 0.7))
@sfx
def stuck(): return done(run(M, ["E4", "C4"], 0.2, 0.7, 0.8), 0.5, 0.15)
@sfx
def near_miss(): return done(run(K, ["G4", "E4", "D4"], 0.17, 0.6, 0.7) + [(0.1, whoosh(0.5, 1800, 300, SR, 12), 0.25)], 0.6, 0.15)
@sfx
def level_start(): return done(run(K, ["C5", "E5", "G5"], 0.09, 0.5, 0.8) + [(0.27, G("C6", 0.9, 0.6), 1)], 0.7, 0.2)
@sfx
def countdown_tick(): return done([(0, W(1500, 0.05, 0.6), 1)])
@sfx
def time_low(): return done(run(M, ["A3", "A3"], 0.13, 0.25, 0.9))
@sfx
def power_clean():
    items = []
    for i in range(7):
        f = RNG.uniform(500, 1300)
        n = int(0.14 * SR)
        t = np.arange(n) / SR
        chirp = np.sin(2 * np.pi * (f * t + 2500 * t * t)) * np.exp(-t * 18)
        items.append((i * 0.045, (chirp * 0.5).astype(np.float32), 1))
    return done(items + sparkle(0.15, 5, 0.3), 0.5, 0.15)
@sfx
def power_merge(): return done(run(G, ["G5", "E5"], 0.07, 0.5, 0.7) + chord(G, ["C6", "E6", "G6"], 0.9, 0.9, at=0.18) + [(0.18, whoosh(0.2, 3000, 500, SR, 13), 0.25)], 0.6, 0.2)
@sfx
def power_broom(): return done([(0, whoosh(0.4, 600, 4200, SR, 14), 0.8)] + [(0.1 + i * 0.05, K(n, 0.3, 0.45), 1) for i, n in enumerate(["E6", "G6", "C7", "A6", "E7"])], 0.5, 0.15)
@sfx
def power_link(): return done(run(K, ["E5", "G5"], 0.09, 0.4, 0.8) + [(0.17, G("C6", 0.5, 0.6), 1), (0.17, W(2200, 0.03, 0.5), 1)], 0.4, 0.12)
@sfx
def power_extratime(): return done([(0, B("C6", 1.0, 0.6), 1)] + run(G, ["G5", "C6", "E6"], 0.08, 0.6, 0.7, at=0.1), 0.7, 0.2)
@sfx
def freeze(): return done(run(G, ["C7", "E7", "G7", "C8"], 0.04, 0.7, 0.45) + [(0, whoosh(0.5, 6000, 1200, SR, 15), 0.3)] + sparkle(0.1, 6, 0.4, 4000, 8000, 0.25), 0.8, 0.2)
@sfx
def revive():
    beat = lambda: tone(85, 0.18, [(1, 1, 18.0), (2, 0.4, 26.0)], 1.0)
    return done([(0, beat(), 1), (0.22, beat(), 1)] + run(K, ["C5", "E5", "G5", "C6"], 0.09, 0.6, 0.8, at=0.5) + [(0.8, G("E6", 1.0, 0.5), 1)], 1.0, 0.25)
@sfx
def boss_intro(): return done([(0, tone(note("C3"), 1.4, MARIMBA, 1.0), 1), (0.0, tone(note("F#3"), 1.4, MARIMBA, 0.7), 1), (0.05, W(110, 0.2, 1.0), 1), (0.4, whoosh(0.8, 200, 1500, SR, 16), 0.3)], 1.1, 0.3, 0.5)
@sfx
def boss_defeat(): return done(run(M, ["C4", "G4", "C5"], 0.1, 0.8, 0.9) + chord(G, ["C5", "E5", "G5", "C6"], 1.4, 0.9, at=0.35) + sparkle(0.4, 8, 0.8), 1.1, 0.25, 0.5)
@sfx
def goal_reached(): return done(chord(G, ["G5", "B5", "D6"], 0.9, 0.9, spread=0.04) + [(0.12, B("G6", 1.2, 0.5), 1)] + sparkle(0.1, 5, 0.4), 0.8, 0.22)
@sfx
def star_ping(): return done([(0, B("E6", 1.2, 0.9), 1)], 0.6, 0.2)
@sfx
def flow_up(): return done(run(K, ["C5", "E5", "G5", "C6", "E6"], 0.045, 0.4, 0.7), 0.4, 0.12)


# ------------------------------------------------------------------ premios

@sfx
def stars_1(): return done([(0, B("E6", 1.2, 0.9), 1)], 0.7, 0.22)
@sfx
def stars_2(): return done([(0, B("E6", 1.0, 0.8), 1), (0.22, B("G6", 1.3, 0.9), 1)], 0.8, 0.22)
@sfx
def stars_3(): return done([(0, B("E6", 1.0, 0.7), 1), (0.2, B("G6", 1.0, 0.8), 1), (0.4, B("C7", 1.6, 0.9), 1)] + chord(G, ["C6", "E6", "G6"], 1.4, 0.7, at=0.45) + sparkle(0.4, 8, 0.8), 1.2, 0.28, 0.6)
@sfx
def result_win(): return done(run(M, ["C5", "E5", "G5", "C6"], 0.11, 0.6, 0.9) + chord(G, ["C5", "E5", "G5", "C6"], 1.6, 1.0, at=0.5) + chord(B, ["C6", "G6"], 1.8, 0.7, at=0.5) + sparkle(0.55, 7, 0.9), 1.2, 0.27, 0.6)
@sfx
def result_lose(): return done(run(M, ["G4", "E4", "C4"], 0.26, 1.0, 0.8) + [(0.6, pad(note("C3"), 1.4, SR, 0.3, 0.9, 0.5), 0.4)], 0.9, 0.2, 0.4)
@sfx
def new_record(): return done(run(G, ["C5", "E5", "G5", "C6", "E6", "G6"], 0.07, 0.8, 0.8) + [(0.45, B("C7", 1.5, 0.6), 1)] + sparkle(0.3, 8, 0.8), 1.0, 0.25, 0.5)
@sfx
def level_up(): return done(run(G, ["C5", "E5", "G5", "C6", "E6", "G6"], 0.07, 0.8, 0.8) + [(0.45, B("C7", 1.5, 0.6), 1), (0.45, G("E7", 1.2, 0.4), 1)] + sparkle(0.4, 10, 0.9, 2800, 6800), 1.1, 0.26, 0.6)
@sfx
def xp_tick(): return done([(0, K("E6", 0.06, 0.3), 1)])
@sfx
def streak_up(): return done(run(K, ["C5", "G5"], 0.09, 0.5, 0.9) + [(0.05, whoosh(0.35, 300, 2200, SR, 17), 0.3), (0.2, B("E6", 1.0, 0.6), 1)], 0.7, 0.2)
@sfx
def streak_lost(): return done([(0, M("C4", 0.9, 0.9), 1), (0.05, whoosh(0.5, 1500, 200, SR, 18), 0.3)], 0.6, 0.15)
@sfx
def streak_saved(): return done(chord(G, ["C5", "G5", "E6"], 1.2, 0.8, spread=0.07) + [(0.3, B("C6", 1.4, 0.5), 1)], 0.9, 0.25)
@sfx
def badge(): return done(run(G, ["G5", "C6"], 0.1, 0.8, 0.8) + [(0.2, B("E6", 1.2, 0.6), 1)], 0.7, 0.2)
@sfx
def achievement(): return done(run(G, ["C5", "G5", "C6", "E6"], 0.09, 0.8, 0.8) + chord(B, ["C6", "E6", "G6"], 1.8, 0.8, at=0.4) + sparkle(0.4, 8, 0.8), 1.4, 0.28, 0.6)
@sfx
def achievement_rare(): return done(run(G, ["C5", "E5", "G5", "C6", "E6", "G6"], 0.08, 0.9, 0.8) + chord(B, ["C6", "E6", "G6", "C7"], 2.4, 0.85, at=0.5) + chord(G, ["C6", "G6", "E7"], 1.8, 0.6, at=0.6) + sparkle(0.45, 14, 1.4, 2600, 7000, 0.3), 2.0, 0.32, 0.8)
@sfx
def prestige():
    p = [(0, pad(note(n), 3.2, SR, 0.5, 1.6, 0.8), 0.9) for n in ["C3", "G3", "C4", "E4", "G4"]]
    return done(p + run(B, ["C5", "G5", "C6", "E6", "G6", "C7"], 0.18, 2.0, 0.7, at=0.5) + sparkle(1.0, 14, 1.8), 2.2, 0.32, 1.0)
@sfx
def tier_up(): return done(run(G, ["G5", "C6", "E6"], 0.08, 0.7, 0.8) + [(0.2, B("G6", 1.2, 0.5), 1)], 0.7, 0.2)
@sfx
def league_promote(): return done(run(M, ["C5", "G5", "C6"], 0.1, 0.6, 0.9) + chord(G, ["C6", "E6", "G6", "C7"], 1.4, 0.9, at=0.35) + sparkle(0.4, 7, 0.8), 1.1, 0.25, 0.5)
@sfx
def league_demote(): return done(run(M, ["E4", "C4"], 0.2, 0.8, 0.7), 0.5, 0.15)
@sfx
def claim(): return done([(0, ping(1760, 0.4, 0.8), 1), (0.06, ping(2349, 0.5, 0.7), 1)] + sparkle(0.05, 3, 0.2), 0.5, 0.15)
@sfx
def claim_all(): return done([(i * 0.06, ping(RNG.choice([1568, 1760, 2093, 2349]), 0.4, 0.7), 1) for i in range(8)] + chord(G, ["C6", "E6", "G6"], 1.0, 0.7, at=0.5), 0.8, 0.22)
@sfx
def mission_done(): return done(run(K, ["E5", "G5", "C6"], 0.08, 0.5, 0.85) + [(0.16, G("E6", 0.8, 0.5), 1)], 0.5, 0.15)
@sfx
def daily_gift(): return done([(0, W(300, 0.1, 0.9), 1), (0.05, whoosh(0.25, 400, 3000, SR, 19), 0.4)] + run(G, ["C6", "E6", "G6", "C7"], 0.07, 0.7, 0.7, at=0.2) + sparkle(0.25, 8, 0.6), 0.9, 0.24, 0.5)
@sfx
def welcome_back(): return done(run(K, ["C5", "G5", "E6", "G6"], 0.14, 0.9, 0.8) + chord(B, ["C6", "G6"], 1.8, 0.6, at=0.5), 1.2, 0.3, 0.5)
@sfx
def purchase(): return done([(0, W(500, 0.06, 0.9), 1), (0.04, ping(2093, 0.8, 0.8), 1), (0.1, ping(3136, 1.0, 0.7), 1)] + sparkle(0.12, 5, 0.3), 0.6, 0.16)
@sfx
def purchase_fail(): return done(run(M, ["D4", "A3"], 0.14, 0.4, 0.8))
@sfx
def unlock(): return done([(0, W(2400, 0.04, 0.8), 1), (0.05, M("C4", 0.3, 0.6), 1), (0.1, G("C6", 0.6, 0.6), 1), (0.17, G("E6", 0.8, 0.6), 1)], 0.5, 0.15)
@sfx
def friend_added(): return done(run(K, ["E5", "A5", "E6"], 0.08, 0.5, 0.8), 0.5, 0.15)
@sfx
def message_in(): return done([(0, W(1400, 0.05, 0.6), 1), (0.04, tone(note("A6"), 0.5, CHIME, 0.6), 1)], 0.5, 0.15)
@sfx
def countdown_ready(): return done(run(tone_chime := (lambda n, d, v: tone(note(n), d, CHIME, v)), ["E6", "E6", "A6"], 0.16, 0.7, 0.6), 0.6, 0.18)


# ------------------------------------------------------------------ monedas, cofres, cartas, ruleta

def tings(count, span, lo=1400, hi=2600, v=0.7, at=0.0):
    return [(at + i * span / max(1, count) + RNG.uniform(0, 0.015), ping(RNG.uniform(lo, hi), 0.35, v * RNG.uniform(0.7, 1.0)), 1) for i in range(count)]


@sfx
def coin(): return done([(0, ping(1760, 0.5, 0.85), 1), (0.05, ping(2349, 0.6, 0.6), 1)], 0.4, 0.12)
@sfx
def coins_small(): return done(tings(4, 0.25), 0.4, 0.12)
@sfx
def coins_big(): return done(tings(14, 0.75, v=0.55) + chord(G, ["C6", "E6", "G6"], 1.0, 0.6, at=0.75), 0.8, 0.22, 0.5)
@sfx
def gem(): return done([(0, G("C7", 0.7, 0.7), 1), (0.04, tone(3136, 0.4, [(1, 1, 10.0), (2.0, 0.4, 14.0)], 0.4), 1)] + sparkle(0.04, 3, 0.2), 0.5, 0.15)
@sfx
def gems(): return done([(i * 0.07, G(n, 0.7, 0.55), 1) for i, n in enumerate(["C7", "E7", "G6", "C7", "E7", "G7"])] + sparkle(0.1, 8, 0.5), 0.8, 0.22)
@sfx
def shard(): return done([(0, G("G6", 0.5, 0.7), 1)] + sparkle(0.02, 4, 0.25, 3000, 7000, 0.3), 0.5, 0.15)
@sfx
def token(): return done([(0, W(800, 0.06, 0.8), 1), (0.03, G("E6", 0.5, 0.6), 1)], 0.4, 0.12)
@sfx
def chest_shake(): return done([(i * 0.1, W(RNG.uniform(160, 260), 0.1, 0.9), 1) for i in range(5)] + [(0.05 + i * 0.12, tone(RNG.uniform(2500, 4200), 0.15, [(1, 1, 22.0)], 0.2), 1) for i in range(4)], 0.4, 0.1)
@sfx
def chest_unlock(): return done([(0, W(2600, 0.04, 0.8), 1), (0.06, W(1900, 0.05, 0.8), 1), (0.13, M("C3", 0.4, 0.7), 1)])


def _creak(dur=0.4):
    n = int(dur * SR)
    t = np.arange(n) / SR
    f = 170 + 120 * (t / dur)
    sig = np.sin(2 * np.pi * np.cumsum(f) / SR + 4 * np.sin(2 * np.pi * 18 * t)) * np.sin(np.pi * t / dur)
    return (sig * 0.3).astype(np.float32)


@sfx
def chest_open_common(): return done([(0, _creak(0.35), 1), (0.3, whoosh(0.3, 500, 3500, SR, 20), 0.4)] + run(G, ["C6", "G6"], 0.09, 0.8, 0.7, at=0.33) + tings(3, 0.25, at=0.4), 0.7, 0.2, 0.4)
@sfx
def chest_open_rare(): return done([(0, _creak(0.35), 1), (0.3, whoosh(0.35, 500, 4500, SR, 21), 0.45)] + chord(G, ["E6", "G6", "B6"], 1.2, 0.8, at=0.33, spread=0.05) + [(0.4, B("E6", 1.4, 0.5), 1)] + tings(4, 0.3, at=0.45) + sparkle(0.4, 6, 0.7), 1.0, 0.25, 0.6)
@sfx
def chest_open_epic(): return done([(0, _creak(0.4), 1), (0.3, whoosh(0.45, 400, 5500, SR, 22), 0.5)] + [(0.3, pad(note(n), 2.0, SR, 0.3, 1.2, 0.7), 0.6) for n in ["A3", "E4", "A4", "C5"]] + run(G, ["A5", "C6", "E6", "A6", "C7"], 0.08, 0.9, 0.8, at=0.4) + tings(5, 0.4, at=0.5) + sparkle(0.4, 10, 1.0), 1.4, 0.3, 0.8)
@sfx
def chest_open_legendary():
    swell = [(0.2, pad(note(n), 3.0, SR, 1.4, 1.2, 0.9), 0.7) for n in ["C3", "G3", "C4", "E4", "G4"]]
    return done([(0, W(70, 0.3, 1.0), 1), (0, tone(65, 0.5, [(1, 1, 5.0)], 0.8), 1), (0.15, _creak(0.5), 1), (0.5, whoosh(0.8, 300, 6500, SR, 23), 0.6)]
                + swell + run(B, ["C6", "E6", "G6", "C7", "E7", "G7"], 0.12, 1.8, 0.6, at=0.8) + tings(10, 0.9, at=0.9) + sparkle(0.8, 18, 1.8, 2600, 7600), 2.0, 0.34, 1.2)
@sfx
def card_flip(): return done([(0, whoosh(0.12, 2500, 5500, SR, 24), 0.5), (0.1, W(1600, 0.03, 0.5), 1)])
@sfx
def card_reveal_common(): return done([(0, G("E6", 0.5, 0.6), 1)], 0.4, 0.12)
@sfx
def card_reveal_rare(): return done(run(G, ["E6", "G6"], 0.09, 0.8, 0.7), 0.6, 0.18)
@sfx
def card_reveal_epic(): return done(run(G, ["C6", "E6", "G6"], 0.08, 0.9, 0.75) + [(0.2, B("C7", 1.2, 0.5), 1)] + sparkle(0.1, 6, 0.5), 0.9, 0.25)
@sfx
def card_reveal_legendary(): return done(run(G, ["C6", "E6", "G6", "C7"], 0.08, 1.0, 0.8) + chord(B, ["C6", "G6", "E7"], 2.0, 0.8, at=0.35) + [(0.3, pad(note("C4"), 2.0, SR, 0.3, 1.2, 0.7), 0.5)] + sparkle(0.2, 12, 1.2, 2600, 7200), 1.6, 0.3, 0.8)
@sfx
def card_new(): return done(chord(G, ["C6", "E6", "G6"], 0.9, 0.8, spread=0.05) + sparkle(0.05, 5, 0.4), 0.7, 0.2)
@sfx
def card_duplicate(): return done([(0, M("D4", 0.35, 0.6), 1)])
@sfx
def card_foil(): return done(run(G, ["C7", "E7", "G7", "E7", "C8"], 0.05, 0.6, 0.4) + [(0, whoosh(0.5, 1500, 7000, SR, 25), 0.3)] + sparkle(0.05, 8, 0.5, 3500, 8500, 0.25), 0.8, 0.22)
@sfx
def series_complete(): return done(run(M, ["C5", "E5", "G5", "C6"], 0.12, 0.6, 0.9) + chord(G, ["C6", "E6", "G6", "C7"], 2.0, 0.9, at=0.55) + chord(B, ["C6", "G6", "E7"], 2.4, 0.7, at=0.55) + tings(8, 0.8, at=0.6) + sparkle(0.6, 10, 1.4), 1.6, 0.3, 0.8)
@sfx
def album_complete():
    pads = [(0, pad(note(n), 5.0, SR, 0.8, 2.0, 0.8), 0.7) for n in ["F3", "C4", "F4", "A4"]] + [(2.0, pad(note(n), 4.0, SR, 0.5, 2.0, 0.8), 0.7) for n in ["C3", "G3", "C4", "E4", "G4"]]
    return done(pads + run(G, ["C5", "E5", "G5", "C6", "E6", "G6", "C7"], 0.11, 1.2, 0.8, at=0.4) + run(B, ["C6", "E6", "G6", "C7", "E7"], 0.25, 2.2, 0.7, at=2.2) + tings(16, 2.4, at=0.8) + sparkle(1.0, 24, 3.5, 2600, 8000, 0.3), 2.4, 0.34, 1.5)
@sfx
def perk_unlock(): return done(run(G, ["E6", "A6", "C7"], 0.08, 0.8, 0.7) + sparkle(0.1, 5, 0.4), 0.7, 0.2)
@sfx
def wheel_tick(): return done([(0, W(1100, 0.025, 0.6), 1)])
@sfx
def wheel_spin():
    items = [(0, whoosh(1.0, 300, 4000, SR, 26), 0.7)]
    t, gap = 0.0, 0.14
    while t < 0.95:
        items.append((t, W(1100, 0.025, 0.5), 1))
        t += gap
        gap = max(0.04, gap * 0.9)
    return done(items)
@sfx
def wheel_stop(): return done([(0, W(500, 0.08, 1.0), 1), (0.03, M("G3", 0.3, 0.8), 1)])
@sfx
def wheel_win(): return done(run(G, ["C6", "E6", "G6", "C7"], 0.07, 0.8, 0.75) + tings(5, 0.4, at=0.2) + sparkle(0.2, 6, 0.5), 0.8, 0.22, 0.5)
@sfx
def wheel_jackpot(): return done(run(G, ["C5", "E5", "G5", "C6", "E6", "G6", "C7"], 0.08, 1.2, 0.85) + chord(B, ["C6", "G6", "E7"], 2.4, 0.8, at=0.6) + tings(20, 1.4, at=0.5) + sparkle(0.5, 14, 1.6), 1.8, 0.3, 0.9)
@sfx
def piggy_break(): return done([(0, fast_highpass(noise(0.12, SR, 30), 1500, SR) * np.exp(-np.arange(int(0.12 * SR)) / SR * 30).astype(np.float32), 0.9), (0.0, W(300, 0.1, 1.0), 1)] + chord(G, ["C7", "E7", "G7"], 0.7, 0.4, at=0.02) + tings(12, 0.9, at=0.1), 0.7, 0.2, 0.5)
@sfx
def piggy_shake(): return done([(i * 0.09, W(RNG.uniform(250, 380), 0.08, 0.8), 1) for i in range(4)] + tings(4, 0.4, 1800, 3000, 0.35, 0.05), 0.3, 0.08)
@sfx
def gift_open(): return done([(0, W(300, 0.08, 0.9), 1), (0.04, whoosh(0.3, 500, 4000, SR, 27), 0.4)] + run(G, ["E6", "G6", "C7"], 0.07, 0.8, 0.7, at=0.2) + sparkle(0.2, 6, 0.5), 0.8, 0.22, 0.4)


# ------------------------------------------------------------------ escribir

FREQUENT = {"tap", "tap_soft", "tile_spawn", "xp_tick", "countdown_tick", "typing", "slider_tick", "wheel_tick", "slide_soft", "merge_1"}


def main(names):
    os.makedirs(OUT, exist_ok=True)
    chosen = names or sorted(S)
    lines = []
    bad = 0
    for name in chosen:
        x = S[name]()
        x = trim(x, SR)
        x = declick(x, SR)
        dur = len(x) / SR
        if name in FREQUENT:
            level = -15.0
        elif dur < 0.15:
            level = -12.0
        elif dur < 0.5:
            level = -9.0
        elif dur < 1.2:
            level = -6.0
        else:
            level = -4.0
        x = normalize(x, level)
        a = analyze(x, SR)
        problems = []
        if a["clipped"] > 0:
            problems.append("recorta")
        if a["dur"] < 0.025 or a["dur"] > 8.5:
            problems.append("duracion")
        if abs(a["dc"]) > 0.01:
            problems.append("dc")
        if a["low"] > 0.5:
            problems.append("mucho grave")
        if np.isnan(x).any():
            problems.append("nan")
        lines.append("%-24s %5.2fs pico %5.1f rms %6.1f centro %5.0fHz grave %.2f %s" % (name, a["dur"], a["peak_db"], a["rms_db"], a["centroid"], a["low"], " ".join(problems)))
        if problems:
            bad += 1
            continue
        to_mp3(os.path.join(OUT, "sfx_%s.mp3" % name), x, SR, 96)
    print("\n".join(lines))
    print("%d sonidos, %d con problemas" % (len(chosen), bad))
    if not names:
        swift = os.path.normpath(os.path.join(HERE, "..", "..", "ParDos", "Sfx.swift"))
        body = "\n".join("    case %s" % n for n in sorted(S))
        with open(swift, "w", encoding="utf-8") as fh:
            fh.write("// Generado por iosApp/tools/audio/make_sfx.py: no editar a mano.\n\n"
                     "/// Efectos de sonido (archivos sfx_<nombre>.mp3).\nenum Sfx: String {\n" + body + "\n}\n")


if __name__ == "__main__":
    main(sys.argv[1:])
