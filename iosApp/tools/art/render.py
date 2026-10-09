"""
Previsualizador de dibujos de ParDos: pinta el mismo JSON que lee la app y lo guarda como PNG.

Uso desde Python:
    from render import render_icon, contact_sheet
    img = render_icon(icon, size=256, palette={"head": "#FF0000"}, bg="#F3EFE6")
    contact_sheet([("fox", icon), ...], cols=6, cell=160, path="hoja.png")

Aproxima a Swift: el relleno por defecto es "union de subcaminos" (usa e=1 cuando haya agujeros);
las uniones de trazos se pintan siempre redondas.
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageChops, ImageDraw, ImageFont

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from artlib import resolve_color  # noqa: E402

SS = 4  # supermuestreo


def _flatten(flat, scale, steps=14):
    """Lista plana de comandos -> lista de (puntos, cerrado) en pixeles."""
    subs = []
    cur = None
    start = (0.0, 0.0)
    cx = cy = 0.0
    i = 0
    n = len(flat)
    while i < n:
        c = flat[i]
        if c == 0:
            if cur and len(cur) > 1:
                subs.append((cur, False))
            cx, cy = flat[i + 1] * scale, flat[i + 2] * scale
            start = (cx, cy)
            cur = [(cx, cy)]
            i += 3
        elif c == 1:
            if cur is None:
                cur = [start]
            cx, cy = flat[i + 1] * scale, flat[i + 2] * scale
            cur.append((cx, cy))
            i += 3
        elif c == 2:
            if cur is None:
                cur = [start]
            x1, y1 = flat[i + 1] * scale, flat[i + 2] * scale
            x2, y2 = flat[i + 3] * scale, flat[i + 4] * scale
            x, y = flat[i + 5] * scale, flat[i + 6] * scale
            for k in range(1, steps + 1):
                t = k / steps
                u = 1 - t
                cur.append((u * u * u * cx + 3 * u * u * t * x1 + 3 * u * t * t * x2 + t * t * t * x,
                            u * u * u * cy + 3 * u * u * t * y1 + 3 * u * t * t * y2 + t * t * t * y))
            cx, cy = x, y
            i += 7
        else:
            if cur and len(cur) > 1:
                subs.append((cur, True))
            cur = None
            cx, cy = start
            i += 1
    if cur and len(cur) > 1:
        subs.append((cur, False))
    return subs


def _fill_mask(subs, size, evenodd):
    mask = None
    for pts, _closed in subs:
        if len(pts) < 3:
            continue
        layer = Image.new("L", size, 0)
        ImageDraw.Draw(layer).polygon(pts, fill=255)
        arr = np.asarray(layer, dtype=np.uint8)
        if mask is None:
            mask = arr.copy()
        elif evenodd:
            mask = mask ^ arr
        else:
            mask = np.maximum(mask, arr)
    if mask is None:
        mask = np.zeros((size[1], size[0]), dtype=np.uint8)
    return mask


def _stroke_mask(subs, size, width, cap):
    layer = Image.new("L", size, 0)
    d = ImageDraw.Draw(layer)
    w = max(1, int(round(width)))
    for pts, closed in subs:
        seq = list(pts)
        if closed and seq[0] != seq[-1]:
            seq.append(seq[0])
        if len(seq) >= 2:
            d.line(seq, fill=255, width=w, joint="curve")
        r = width / 2.0
        if cap == 1:
            for (x, y) in (seq[0], seq[-1]):
                d.ellipse([x - r, y - r, x + r, y + r], fill=255)
        else:
            for (x, y) in seq[1:-1:3]:
                d.ellipse([x - r * 0.98, y - r * 0.98, x + r * 0.98, y + r * 0.98], fill=255)
    return np.asarray(layer, dtype=np.uint8)


def _paint_array(paint, pal, size, scale, origin):
    """Arreglo RGBA float (alto, ancho, 4): colores 0..255, alfa 0..1. origin = esquina (x0, y0) de la region en pixeles."""
    w, h = size
    ox, oy = origin
    if isinstance(paint, dict):
        stops = [(s[0], resolve_color(s[1], pal)) for s in paint["st"]]
        ys, xs = np.mgrid[0:h, 0:w].astype(np.float32)
        xs += 0.5 + ox
        ys += 0.5 + oy
        if paint["t"] == "l":
            x1, y1, x2, y2 = [v * scale for v in paint["p"]]
            dx, dy = x2 - x1, y2 - y1
            den = dx * dx + dy * dy
            t = ((xs - x1) * dx + (ys - y1) * dy) / den if den > 0 else np.zeros_like(xs)
        else:
            cx, cy, r = [v * scale for v in paint["p"]]
            t = np.sqrt((xs - cx) ** 2 + (ys - cy) ** 2) / max(r, 1e-6)
        t = np.clip(t, 0, 1)
        offs = [s[0] for s in stops]
        out = np.zeros((h, w, 4), dtype=np.float32)
        for ch in range(4):
            vals = [s[1][ch] for s in stops]
            out[:, :, ch] = np.interp(t, offs, vals)
        return out
    r, g, b, a = resolve_color(paint, pal)
    out = np.zeros((h, w, 4), dtype=np.float32)
    out[:, :, 0] = r
    out[:, :, 1] = g
    out[:, :, 2] = b
    out[:, :, 3] = a
    return out


def render_icon(icon, size=256, palette=None, bg=None, blink=False):
    """Pinta un dibujo cocido y devuelve una imagen RGBA (size en el lado mayor)."""
    pal = dict(icon.get("pal", {}))
    if palette:
        pal.update(palette)
    big = size * SS
    scale = big / float(max(icon["w"], icon["h"]))
    wpx, hpx = int(round(icon["w"] * scale)), int(round(icon["h"] * scale))
    canvas = np.zeros((hpx, wpx, 4), dtype=np.float32)  # color sin premultiplicar + alfa acumulado 0..1
    for sh in icon["s"]:
        tag = sh.get("g", 0)
        if tag == 1 and blink:
            continue
        if tag == 2 and not blink:
            continue
        subs = _flatten(sh["p"], scale)
        if not subs:
            continue
        pad = (sh.get("w", 0) * scale) / 2.0 + 3 if "k" in sh else 3
        xs = [p[0] for pts, _ in subs for p in pts]
        ys = [p[1] for pts, _ in subs for p in pts]
        x0 = max(0, int(math.floor(min(xs) - pad)))
        y0 = max(0, int(math.floor(min(ys) - pad)))
        x1 = min(wpx, int(math.ceil(max(xs) + pad)))
        y1 = min(hpx, int(math.ceil(max(ys) + pad)))
        if x1 <= x0 or y1 <= y0:
            continue
        rw, rh = x1 - x0, y1 - y0
        local = [([(px - x0, py - y0) for (px, py) in pts], closed) for pts, closed in subs]
        op = sh.get("o", 1.0)
        layers = []
        if "f" in sh:
            layers.append((_fill_mask(local, (rw, rh), bool(sh.get("e", 0))), sh["f"]))
        if "k" in sh:
            layers.append((_stroke_mask(local, (rw, rh), sh["w"] * scale, sh.get("c", 0)), sh["k"]))
        for mask, paint in layers:
            arr = _paint_array(paint, pal, (rw, rh), scale, (x0, y0))
            a_src = (mask.astype(np.float32) / 255.0) * arr[:, :, 3] * op
            region = canvas[y0:y1, x0:x1]
            a_dst = region[:, :, 3]
            a_out = a_src + a_dst * (1 - a_src)
            safe = np.where(a_out > 1e-6, a_out, 1.0)
            for ch in range(3):
                region[:, :, ch] = (arr[:, :, ch] * a_src + region[:, :, ch] * a_dst * (1 - a_src)) / safe
            region[:, :, 3] = a_out
    rgba = np.zeros((hpx, wpx, 4), dtype=np.uint8)
    rgba[:, :, :3] = np.clip(canvas[:, :, :3], 0, 255).astype(np.uint8)
    rgba[:, :, 3] = np.clip(canvas[:, :, 3] * 255, 0, 255).astype(np.uint8)
    longest = float(max(icon["w"], icon["h"]))
    img = Image.fromarray(rgba, "RGBA").resize((max(1, int(round(icon["w"] / longest * size))),
                                                max(1, int(round(icon["h"] / longest * size)))), Image.LANCZOS)
    if bg is not None:
        base = Image.new("RGBA", img.size, bg)
        base.alpha_composite(img)
        return base
    return img


def _font(px):
    for name in ("arial.ttf", "segoeui.ttf", "DejaVuSans.ttf"):
        try:
            return ImageFont.truetype(name, px)
        except Exception:
            continue
    return ImageFont.load_default()


def contact_sheet(items, path, cols=6, cell=160, palette=None, bg="#F3EFE6", label=True, blink=False):
    """items: lista de (nombre, dibujo) o (nombre, dibujo, paleta). Guarda una hoja PNG."""
    rows = int(math.ceil(len(items) / float(cols)))
    pad = 10
    lab = 22 if label else 0
    sheet = Image.new("RGBA", (cols * (cell + pad) + pad, rows * (cell + lab + pad) + pad), bg)
    d = ImageDraw.Draw(sheet)
    f = _font(12)
    for idx, it in enumerate(items):
        name, icon = it[0], it[1]
        pal = it[2] if len(it) > 2 else palette
        r, c = divmod(idx, cols)
        x = pad + c * (cell + pad)
        y = pad + r * (cell + lab + pad)
        d.rectangle([x, y, x + cell, y + cell], fill="#FFFFFF" if bg != "#FFFFFF" else "#EEEEEE")
        img = render_icon(icon, size=cell, palette=pal, blink=blink)
        sheet.alpha_composite(img, (x + (cell - img.size[0]) // 2, y + (cell - img.size[1]) // 2))
        if label:
            d.text((x + 2, y + cell + 3), str(name)[:int(cell / 7)], fill="#333333", font=f)
    sheet.convert("RGB").save(path)
    return path


if __name__ == "__main__":
    import json
    if len(sys.argv) < 3:
        print("uso: python render.py dibujos.json salida.png [columnas] [tamano]")
        sys.exit(1)
    data = json.load(open(sys.argv[1], encoding="utf-8"))
    cols = int(sys.argv[3]) if len(sys.argv) > 3 else 6
    cell = int(sys.argv[4]) if len(sys.argv) > 4 else 160
    contact_sheet([(k, v) for k, v in sorted(data.items())], sys.argv[2], cols=cols, cell=cell)
    print("listo:", sys.argv[2])
