"""
Musicas de fondo de ParDos (bucles perfectos). Uso:  python make_music.py   (escribe iosApp/ParDos/Sounds/mus_<nombre>.wav)
Cada pista es generada con tonalidad, progresion de acordes, pad, bajo, arpegio y melodia sobre la pentatonica.
El mezclador es circular: lo que se sale por el final vuelve al principio, asi el bucle no tiene costura.
"""
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from synth import *  # noqa: E402,F403

OUT = os.path.normpath(os.path.join(HERE, "..", "..", "ParDos", "Sounds"))
SR = SR_MUSIC

PENT_MAJOR = [0, 2, 4, 7, 9]
PENT_MINOR = [0, 3, 5, 7, 10]


def n(name):
    return int(round(69 + 12 * np.log2(note(name) / 440.0)))


def triad(root, kind):
    third = 4 if kind == "M" else 3
    seventh = {"M": 11, "m": 10, "7": 10}.get(kind, 10)
    return [root, root + third, root + 7, root + 12]


def compose(name, bpm, bars, chords, pent, key_root, seed, cfg, stems=False):
    rng = np.random.RandomState(seed)
    beat = 60.0 / bpm
    bar = beat * 4
    total = bar * bars
    mixes = {k: Mix(total, SR, circular=True) for k in ("base", "groove", "lead")}
    scale = [key_root + 12 * o + s for o in (0, 1, 2) for s in pent]
    motif = [int(rng.randint(2, 8)) for _ in range(4)]

    for b in range(bars):
        t0 = b * bar
        root, kind = chords[b % len(chords)]
        notes = triad(root, kind)
        # colchon
        if cfg.get("pad", 0) > 0:
            for m_ in notes[:4]:
                mixes["base"].add(pad(midi_hz(m_), bar + 1.6, SR, 1.1, 1.6, 1.0, cfg.get("bright", 0.5)), t0 - 0.2, cfg["pad"] * 0.55)
        # bajo
        if cfg.get("bass", 0) > 0:
            mixes["base"].add(bass(midi_hz(root), beat * 2.6, SR, 1.0), t0, cfg["bass"])
            if cfg.get("bass2", True):
                mixes["base"].add(bass(midi_hz(root), beat * 1.6, SR, 0.8), t0 + beat * 2, cfg["bass"] * 0.7)
        # arpegio
        inst = cfg.get("arp_inst", KALIMBA)
        prob = cfg.get("arp_prob", 0.5)
        if prob > 0:
            order = [0, 1, 2, 3, 2, 1, 2, 3]
            for i in range(cfg.get("arp_steps", 8)):
                if rng.rand() > prob and i % 4 != 0:
                    continue
                m_ = notes[order[i % len(order)]] + 12 + (12 if i % 8 == 5 else 0)
                vel = cfg.get("arp_vel", 0.5) * (1.0 if i % 4 == 0 else 0.7) * rng.uniform(0.8, 1.0)
                mixes["groove"].add(tone(midi_hz(m_), 1.2, inst, vel, SR), t0 + i * beat * 4 / cfg.get("arp_steps", 8) + rng.uniform(0, 0.01), 1.0)
        # melodia
        if cfg.get("mel", 0) > 0 and b % cfg.get("mel_every", 2) == 0:
            minst = cfg.get("mel_inst", GLOCK)
            shift = (b // 4) % 3
            pos = 0.0
            for k, deg in enumerate(motif):
                if rng.rand() < cfg.get("mel_rest", 0.25):
                    pos += beat
                    continue
                mm = scale[min(len(scale) - 1, deg + 5 + (shift if k % 2 else 0))]
                dur = beat * (1.5 if k == len(motif) - 1 else 1.0)
                mixes["lead"].add(tone(midi_hz(mm), 2.0, minst, cfg["mel"], SR), t0 + beat * 0.5 + pos, 1.0)
                pos += dur
        # escobillas / shaker
        if cfg.get("shaker", 0) > 0:
            for i in range(8):
                if i % 2 == 1 or cfg.get("shaker_all", False):
                    hit = fast_highpass(noise(0.07, SR, seed + b * 8 + i), 5000, SR) * np.exp(-np.arange(int(0.07 * SR)) / SR * 45)
                    mixes["groove"].add(hit.astype(np.float32), t0 + i * beat / 2, cfg["shaker"] * (1.0 if i % 4 == 3 else 0.6))
        # campana lejana
        if cfg.get("bell", 0) > 0 and b % cfg.get("bell_every", 4) == 2:
            mixes["base"].add(tone(midi_hz(scale[int(rng.randint(8, 13))]), 3.0, BELL, cfg["bell"], SR), t0 + beat * rng.uniform(0.5, 2.5), 1.0)
        # gota de agua
        if cfg.get("drop", 0) > 0 and b % 3 == 1:
            f = midi_hz(scale[int(rng.randint(9, 14))])
            nn = int(0.5 * SR)
            tt = np.arange(nn) / SR
            d = np.sin(2 * np.pi * (f * tt + 120 * tt * tt * 0)) * np.exp(-tt * 9) * (1 + 0.5 * np.exp(-tt * 40))
            mixes["base"].add((d * cfg["drop"]).astype(np.float32), t0 + beat * rng.uniform(1, 3), 1.0)
    parts = {k: m.render() for k, m in mixes.items()}

    def finish_layer(x):
        if cfg.get("reverb", 0) > 0:
            # reverb circular: se calcula sobre la pista repetida tres veces y se toma la central
            rep3 = np.concatenate([x, x, x])
            wet = reverb(rep3, SR, 1.6, cfg["reverb"])[:len(rep3)]
            x = wet[len(x):2 * len(x)]
        x = fast_highpass(x.astype(np.float32), 90, SR)
        return fast_lowpass(x, 7000, SR)

    parts = {k: finish_layer(v) for k, v in parts.items()}
    total_sig = parts["base"] + parts["groove"] + parts["lead"]
    peak = np.max(np.abs(total_sig)) + 1e-9
    gain = 10 ** (-3.0 / 20) / peak
    rms = np.sqrt(np.mean((total_sig * gain) ** 2))
    target = 10 ** (cfg.get("rms_db", -23.0) / 20)
    if rms > target:
        gain *= target / rms
    if stems:
        return {k: (v * gain).astype(np.float32) for k, v in parts.items()}
    return (total_sig * gain).astype(np.float32)


def C(name):
    return n(name)


TRACKS = {
    "menu": dict(bpm=76, bars=16, key=C("C4"), pent=PENT_MAJOR, seed=1,
                 chords=[(C("C3"), "M"), (C("A2"), "m"), (C("F2"), "M"), (C("G2"), "M"), (C("C3"), "M"), (C("A2"), "m"), (C("F2"), "M"), (C("G2"), "M"),
                         (C("F2"), "M"), (C("G2"), "M"), (C("E2"), "m"), (C("A2"), "m"), (C("F2"), "M"), (C("C3"), "M"), (C("G2"), "M"), (C("C3"), "M")],
                 cfg=dict(pad=1.0, bass=0.0, arp_prob=0.45, arp_vel=0.5, mel=0.45, mel_every=2, mel_rest=0.3, bell=0.3, reverb=0.28, bright=0.5)),
    "map": dict(bpm=92, bars=16, key=C("G3"), pent=PENT_MAJOR, seed=2,
                chords=[(C("G2"), "M"), (C("D3"), "M"), (C("E2"), "m"), (C("C3"), "M")] * 2 + [(C("C3"), "M"), (C("D3"), "M"), (C("B2"), "m"), (C("E2"), "m")] + [(C("C3"), "M"), (C("G2"), "M"), (C("D3"), "M"), (C("G2"), "M")],
                cfg=dict(pad=0.6, bass=0.7, arp_inst=MARIMBA, arp_prob=0.8, arp_vel=0.55, mel=0.4, mel_every=2, mel_rest=0.2, shaker=0.1, reverb=0.22, bright=0.6)),
    "game": dict(bpm=70, bars=16, key=C("A3"), pent=PENT_MINOR, seed=3,
                 chords=[(C("A2"), "m"), (C("F2"), "M"), (C("C3"), "M"), (C("G2"), "M")] * 2 + [(C("A2"), "m"), (C("F2"), "M"), (C("D3"), "m"), (C("E3"), "M")] + [(C("F2"), "M"), (C("G2"), "M"), (C("A2"), "m"), (C("A2"), "m")],
                 cfg=dict(pad=1.0, bass=0.0, arp_prob=0.7, arp_vel=0.42, mel=0.4, mel_every=2, mel_rest=0.35, drop=0.14, bell=0.22, bell_every=4, reverb=0.32, bright=0.4, rms_db=-25.0), stems=True),
    "shop": dict(bpm=104, bars=16, key=C("F3"), pent=PENT_MAJOR, seed=4,
                 chords=[(C("F2"), "M"), (C("D3"), "m"), (C("A#2"), "M"), (C("C3"), "M")] * 2 + [(C("A#2"), "M"), (C("C3"), "M"), (C("A2"), "m"), (C("D3"), "m")] + [(C("F2"), "M"), (C("A#2"), "M"), (C("C3"), "M"), (C("F2"), "M")],
                 cfg=dict(pad=0.5, bass=0.75, arp_inst=PLUCK, arp_prob=0.85, arp_vel=0.5, mel=0.5, mel_inst=MARIMBA, mel_every=2, mel_rest=0.2, shaker=0.14, shaker_all=False, reverb=0.2, bright=0.7)),
    "halloween": dict(bpm=84, bars=16, key=C("E3"), pent=PENT_MINOR, seed=5,
                      chords=[(C("E2"), "m"), (C("C3"), "M"), (C("D3"), "M"), (C("E2"), "m")] * 2 + [(C("E2"), "m"), (C("A2"), "m"), (C("B2"), "7"), (C("E2"), "m")] + [(C("C3"), "M"), (C("D3"), "M"), (C("B2"), "7"), (C("E2"), "m")],
                      cfg=dict(pad=1.0, bass=0.5, bass2=False, arp_inst=CELESTA, arp_prob=0.5, arp_vel=0.45, mel=0.42, mel_inst=CELESTA, mel_every=2, mel_rest=0.3, bell=0.4, bell_every=4, reverb=0.34, bright=0.45)),
}


def main(names):
    os.makedirs(OUT, exist_ok=True)
    for name in (names or sorted(TRACKS)):
        spec = TRACKS[name]
        stems = spec.get("stems", False)
        x = compose(name, spec["bpm"], spec["bars"], spec["chords"], spec["pent"], spec["key"], spec["seed"], spec["cfg"], stems)
        if stems:
            for layer, sig in x.items():
                to_wav(os.path.join(OUT, "mus_%s_%s.wav" % (name, layer)), sig, SR)
            mixed = x["base"] + x["groove"] + x["lead"]
            a = analyze(mixed, SR)
            print("%-10s capas %5.1fs pico %5.1f rms %6.1f grave %.3f" % (name, a["dur"], a["peak_db"], a["rms_db"], a["low"]))
            continue
        a = analyze(x, SR)
        seam = abs(float(x[0]) - float(x[-1]))
        print("%-10s %5.1fs pico %5.1f rms %6.1f centro %5.0fHz grave %.3f costura %.4f" % (name, a["dur"], a["peak_db"], a["rms_db"], a["centroid"], a["low"], seam))
        to_wav(os.path.join(OUT, "mus_%s.wav" % name), x, SR)


if __name__ == "__main__":
    main(sys.argv[1:])
