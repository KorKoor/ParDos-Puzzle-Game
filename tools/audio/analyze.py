# -*- coding: utf-8 -*-
"""
Revisa los .ogg generados: duración, pico, volumen (RMS), recortes y un espectrograma (PNG) de los que se pidan.

    python analyze.py                 # tabla de todos
    python analyze.py sheet ui_tap combo_4 flow_in    # hoja de espectrogramas
"""
import os
import subprocess
import sys
import tempfile
import wave

import numpy as np

from synth import SR, ffmpeg_exe

HERE = os.path.dirname(os.path.abspath(__file__))
RAW = os.path.normpath(os.path.join(HERE, '..', '..', 'app', 'src', 'main', 'res', 'raw'))


def decode(path):
    with tempfile.TemporaryDirectory() as tmp:
        wav = os.path.join(tmp, 'x.wav')
        subprocess.run([ffmpeg_exe(), '-y', '-hide_banner', '-loglevel', 'error', '-i', path, '-ar', str(SR), '-ac', '1', wav], check=True)
        with wave.open(wav, 'rb') as w:
            raw = w.readframes(w.getnframes())
        return np.frombuffer(raw, dtype='<i2').astype(np.float64) / 32768.0


def spectrogram(x, win=1024, hop=256):
    w = np.hanning(win)
    frames = []
    for i in range(0, max(1, len(x) - win), hop):
        seg = x[i:i + win]
        if len(seg) < win:
            seg = np.pad(seg, (0, win - len(seg)))
        frames.append(np.abs(np.fft.rfft(seg * w)))
    S = np.array(frames).T
    S = 20 * np.log10(S + 1e-6)
    return np.clip((S - (S.max() - 80)) / 80, 0, 1)


def sheet(names, out):
    from PIL import Image
    tiles = []
    for n in names:
        x = decode(os.path.join(RAW, n + '.ogg'))
        S = spectrogram(x)[:200][::-1]  # hasta ~8,6 kHz, graves abajo
        img = (S * 255).astype(np.uint8)
        im = Image.fromarray(img, 'L').resize((min(520, max(120, S.shape[1] * 2)), 200))
        # onda abajo
        wave_h = 50
        wv = Image.new('L', (im.width, wave_h), 20)
        px = wv.load()
        step = max(1, len(x) // im.width)
        for i in range(im.width):
            seg = x[i * step:(i + 1) * step]
            if len(seg):
                a = int(np.max(np.abs(seg)) * (wave_h / 2 - 1))
                for y in range(wave_h // 2 - a, wave_h // 2 + a + 1):
                    px[i, y] = 220
        tile = Image.new('L', (im.width, 200 + wave_h + 16), 0)
        tile.paste(im, (0, 16))
        tile.paste(wv, (0, 216))
        tiles.append((n, tile))
    from PIL import ImageDraw
    W = 3
    rows = (len(tiles) + W - 1) // W
    cw = max(t.width for _, t in tiles) + 6
    ch = max(t.height for _, t in tiles) + 6
    canvas = Image.new('L', (cw * W, ch * rows), 0)
    d = ImageDraw.Draw(canvas)
    for k, (n, t) in enumerate(tiles):
        x0 = (k % W) * cw
        y0 = (k // W) * ch
        canvas.paste(t, (x0, y0))
        d.text((x0 + 4, y0 + 2), n, fill=255)
    canvas.save(out)


def table():
    rows = []
    for f in sorted(os.listdir(RAW)):
        if not f.endswith('.ogg') or f.startswith('music_'):
            continue
        x = decode(os.path.join(RAW, f))
        peak = float(np.max(np.abs(x)))
        rms = 20 * np.log10(max(float(np.sqrt(np.mean(x ** 2))), 1e-9))
        clip = int(np.sum(np.abs(x) > 0.995))
        dc = float(np.mean(x))
        rows.append((f[:-4], len(x) / SR, peak, rms, clip, dc))
    return rows


if __name__ == '__main__':
    if len(sys.argv) > 1 and sys.argv[1] == 'sheet':
        sheet(sys.argv[2:], os.path.join(HERE, '_sheet.png'))
        print('ok')
    else:
        rows = table()
        bad = [r for r in rows if r[4] > 0 or abs(r[5]) > 0.01 or r[1] > 4.5 or r[2] < 0.2]
        print(f'{len(rows)} sonidos; con problemas: {len(bad)}')
        for r in bad:
            print(f'  {r[0]:18s} dur={r[1]:.2f} peak={r[2]:.2f} rms={r[3]:.1f} clip={r[4]} dc={r[5]:.4f}')
        short = sorted(rows, key=lambda r: r[1])[:5]
        longest = sorted(rows, key=lambda r: -r[1])[:8]
        print('más cortos:', [(r[0], round(r[1], 2)) for r in short])
        print('más largos:', [(r[0], round(r[1], 2)) for r in longest])
        loud = sorted(rows, key=lambda r: -r[3])[:6]
        quiet = sorted(rows, key=lambda r: r[3])[:6]
        print('más fuertes (rms):', [(r[0], round(r[3], 1)) for r in loud])
        print('más bajos (rms):', [(r[0], round(r[3], 1)) for r in quiet])
