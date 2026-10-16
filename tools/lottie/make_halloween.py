"""Genera la animacion Lottie (JSON) de la mega actualizacion de Halloween de ParDos.
Uso: python tools/lottie/make_halloween.py [salida.json]
Lienzo 512x512, 30 fps, 90 cuadros (3 s) en bucle sin costura. Todo es vector (sin imagenes, sin emojis, sin texto):
luna con halo que respira, tres murcielagos que vuelan en bucle aleteando, una calabaza que flota con la cara encendida,
telaranas de esquina, estrellas que titilan y una colina con lapidas."""
import json
import math
import os
import sys

FPS, END, W, H = 30, 90, 512, 512
EASE = {"i": {"x": [0.667], "y": [1]}, "o": {"x": [0.333], "y": [0]}}
LIN = {"i": {"x": [1], "y": [1]}, "o": {"x": [0], "y": [0]}}


def rgb(h):
    h = h.lstrip("#")
    return [int(h[i:i + 2], 16) / 255.0 for i in (0, 2, 4)] + [1]


def static(v):
    return {"a": 0, "k": v}


def anim(frames, ease=EASE):
    """frames: lista de (t, valor). Valor escalar o lista."""
    ks = []
    for n, (t, v) in enumerate(frames):
        val = v if isinstance(v, list) else [v]
        kf = {"t": t, "s": val}
        if n < len(frames) - 1:
            kf.update(ease)
        ks.append(kf)
    return {"a": 1, "k": ks}


def ks(o=100, r=0, p=(0, 0), a=(0, 0), s=(100, 100)):
    def f(v, three=False):
        if isinstance(v, dict):
            return v
        return static(list(v) + ([0] if three else []) if isinstance(v, (tuple, list)) else v)
    sc = s if isinstance(s, dict) else static(list(s) + [100])
    return {"o": f(o), "r": f(r), "p": f(p, True), "a": f(a, True), "s": sc}


def tr(p=(0, 0), a=(0, 0), s=(100, 100), r=0, o=100):
    return {"ty": "tr", "p": static(list(p)) if not isinstance(p, dict) else p, "a": static(list(a)), "s": static(list(s)) if not isinstance(s, dict) else s,
            "r": static(r) if not isinstance(r, dict) else r, "o": static(o) if not isinstance(o, dict) else o, "sk": static(0), "sa": static(0)}


def fill(color, op=100):
    return {"ty": "fl", "c": static(rgb(color)), "o": static(op) if not isinstance(op, dict) else op, "r": 1, "nm": "Relleno"}


def stroke(color, w, op=100):
    return {"ty": "st", "c": static(rgb(color)), "o": static(op), "w": static(w), "lc": 2, "lj": 2, "nm": "Trazo"}


def ell(cx, cy, w, h):
    return {"ty": "el", "p": static([cx, cy]), "s": static([w, h]), "d": 1, "nm": "Elipse"}


def rect(x, y, w, h, r=0):
    return {"ty": "rc", "p": static([x + w / 2, y + h / 2]), "s": static([w, h]), "r": static(r), "d": 1, "nm": "Rect"}


def poly(points, closed=True):
    return {"ty": "sh", "ks": static({"i": [[0, 0]] * len(points), "o": [[0, 0]] * len(points), "v": [list(p) for p in points], "c": closed}), "nm": "Poligono"}


def bez(v, i, o, closed=True):
    return {"ty": "sh", "ks": static({"i": i, "o": o, "v": v, "c": closed}), "nm": "Curva"}


def group(items, transform=None, name="Grupo"):
    return {"ty": "gr", "it": items + [transform or tr()], "nm": name}


def layer(ind, name, shapes, kst=None):
    # En Lottie el primer elemento de la lista queda ENCIMA: aqui se escriben de abajo hacia arriba y se invierten al guardar
    return {"ddd": 0, "ind": ind, "ty": 4, "nm": name, "sr": 1, "ks": kst or ks(), "ao": 0, "shapes": list(reversed(shapes)), "ip": 0, "op": END, "st": 0, "bm": 0}


# ---------------------------------------------------------------- paleta (la del evento de Halloween del juego)
NIGHT = "#1B0F2E"
PURPLE = "#4A1F6B"
VIOLET = "#7B4FB3"
ORANGE = "#FF8A1F"
ORANGE_D = "#C95F0E"
ORANGE_L = "#FFB45A"
GLOW = "#FFD27A"
MOON = "#FFEFC6"
MOON_D = "#F3D99A"
DARK = "#2A1236"
BAT = "#150821"
STEM = "#5C7A34"
WEB = "#E6DDF5"


def sparkle_shape(size):
    s = size
    return bez([[0, -s], [s * 0.22, -s * 0.22], [s, 0], [s * 0.22, s * 0.22], [0, s], [-s * 0.22, s * 0.22], [-s, 0], [-s * 0.22, -s * 0.22]],
               [[0, 0]] * 8, [[0, 0]] * 8)


def make():
    layers = []
    idx = [0]

    def add(name, shapes, kst=None):
        idx[0] += 1
        layers.append(layer(idx[0], name, shapes, kst))

    # ---- estrellas que titilan (arriba del todo)
    stars = [(64, 70, 9, 0), (452, 96, 7, 20), (120, 150, 6, 40), (404, 40, 8, 60), (40, 210, 5, 10), (478, 200, 6, 50)]
    for n, (x, y, s, ph) in enumerate(stars):
        fr = [(0, 30), (15, 100), (30, 30), (45, 100), (60, 30), (75, 100), (90, 30)]
        shift = ph % 30
        fr = [((t + shift) % END if False else t, v) for t, v in fr]
        add("Estrella %d" % (n + 1), [group([sparkle_shape(s), fill(GLOW)], tr(p=(x, y)))],
            ks(o=anim([(0, 20 + (ph % 3) * 20), (22, 100), (45, 25), (68, 100), (90, 20 + (ph % 3) * 20)])))

    # ---- murcielagos: vuelan en un recorrido cerrado y aletean
    def bat(ind_name, path, scale, flap_offset):
        wing_l = bez([[-6, 4], [-22, -22], [-48, -26], [-62, -4], [-50, -2], [-44, 8], [-34, 2], [-26, 12], [-14, 6]],
                     [[0, 0], [8, 6], [10, 2], [0, 10], [6, -2], [4, 6], [0, -4], [4, 4], [0, 0]], [[0, 0], [-8, -6], [-10, -2], [0, -10], [-6, 2], [-4, -6], [0, 4], [-4, -4], [0, 0]])
        wing_r = bez([[6, 4], [22, -22], [48, -26], [62, -4], [50, -2], [44, 8], [34, 2], [26, 12], [14, 6]],
                     [[0, 0], [-8, 6], [-10, 2], [0, 10], [-6, -2], [-4, 6], [0, -4], [-4, 4], [0, 0]], [[0, 0], [8, -6], [10, -2], [0, -10], [6, 2], [4, -6], [0, 4], [4, -4], [0, 0]])
        flap = lambda: anim([(0, [100, 100]), (6, [100, 35]), (12, [100, 100]), (18, [100, 35]), (24, [100, 100]), (30, [100, 35]), (36, [100, 100]),
                             (42, [100, 35]), (48, [100, 100]), (54, [100, 35]), (60, [100, 100]), (66, [100, 35]), (72, [100, 100]), (78, [100, 35]), (84, [100, 100]), (90, [100, 100])])
        body = [
            group([wing_l, fill(BAT), stroke(VIOLET, 1.8, 90)], tr(a=(-6, 4), p=(-6, 4), s=flap()), "Ala izq"),
            group([wing_r, fill(BAT), stroke(VIOLET, 1.8, 90)], tr(a=(6, 4), p=(6, 4), s=flap()), "Ala der"),
            group([ell(0, 6, 18, 26), fill(BAT), stroke(VIOLET, 1.8, 90)], name="Cuerpo"),
            group([ell(0, -10, 16, 16), fill(BAT), stroke(VIOLET, 1.8, 90)], name="Cabeza"),
            group([poly([(-7, -16), (-4, -30), (0, -16)]), poly([(7, -16), (4, -30), (0, -16)]), fill(BAT), stroke(VIOLET, 1.8, 90)], name="Orejas"),
            group([ell(-3.5, -11, 3.4, 3.4), ell(3.5, -11, 3.4, 3.4), fill(GLOW)], name="Ojos"),
        ]
        pos = {"a": 1, "k": []}
        for n, (t, x, y) in enumerate(path):
            kf = {"t": t, "s": [x, y, 0]}
            if n < len(path) - 1:
                kf.update(EASE)
            pos["k"].append(kf)
        add(ind_name, [group(body, tr())], ks(p=pos, s=(scale, scale)))

    bat("Murcielago 1", [(0, 70, 120), (22, 190, 80), (45, 330, 130), (68, 200, 170), (90, 70, 120)], 78, 0)
    bat("Murcielago 2", [(0, 440, 230), (22, 330, 190), (45, 200, 250), (68, 360, 290), (90, 440, 230)], 62, 8)
    bat("Murcielago 3", [(0, 300, 50), (22, 400, 90), (45, 470, 60), (68, 380, 30), (90, 300, 50)], 48, 16)

    # ---- calabaza (flota y respira) con la cara encendida
    face_glow = anim([(0, 88), (10, 100), (20, 78), (30, 100), (45, 90), (60, 100), (75, 80), (90, 88)])
    pumpkin = [
        group([ell(0, 78, 236, 34), fill("#000000", 22)], name="Sombra suelo"),
        group([ell(-62, 10, 120, 150), fill(ORANGE_D)], name="Gajo izq"),
        group([ell(62, 10, 120, 150), fill(ORANGE_D)], name="Gajo der"),
        group([ell(-32, 8, 130, 160), fill(ORANGE)], name="Gajo izq medio"),
        group([ell(32, 8, 130, 160), fill(ORANGE)], name="Gajo der medio"),
        group([ell(0, 6, 112, 168), fill(ORANGE_L)], name="Gajo centro"),
        group([ell(-26, -28, 26, 44), fill("#FFFFFF", 28)], name="Brillo"),
        group([poly([(-9, -86), (-12, -112), (14, -118), (24, -100), (10, -96)]), fill(STEM)], name="Rabito"),
        group([poly([(-62, -22), (-26, -22), (-44, -58)]), poly([(26, -22), (62, -22), (44, -58)]), poly([(-8, 4), (8, 4), (0, -12)]),
               poly([(-70, 24), (-46, 52), (-26, 34), (-8, 56), (10, 34), (28, 56), (46, 52), (70, 24), (40, 74), (0, 82), (-40, 74)]),
               fill(GLOW, 100), stroke(DARK, 5, 100)], tr(o=face_glow), "Cara encendida"),
    ]
    bob = anim([(0, [256, 372, 0]), (23, [256, 360, 0]), (45, [256, 372, 0]), (68, [256, 360, 0]), (90, [256, 372, 0])])
    # la posicion necesita 3 componentes
    add("Calabaza", pumpkin, ks(p=bob, r=anim([(0, -2), (23, 2), (45, -2), (68, 2), (90, -2)]), s=(100, 100)))

    # halo naranja detras de la calabaza
    add("Halo calabaza", [group([ell(0, 0, 330, 270), fill(ORANGE, 11)], tr(s=anim([(0, [100, 100]), (45, [106, 106]), (90, [100, 100])])), "Halo")], ks(p=(256, 350)))

    # ---- lapidas y colina
    hill = bez([[-20, 512], [-20, 430], [120, 400], [256, 420], [400, 396], [532, 430], [532, 512]], [[0, 0]] * 7, [[0, 0]] * 7)
    tomb = lambda x, y, w, h: group([rect(x, y, w, h, 14), fill("#5A3A80")], name="Lapida")
    add("Colina", [group([hill, fill("#3E2160")], name="Colina"), tomb(36, 396, 40, 60), tomb(430, 392, 44, 66),
                   group([rect(60, 410, 8, 30, 2), rect(50, 418, 28, 7, 2), fill(VIOLET, 45)], name="Cruz")])

    # ---- luna con halo que respira
    moon = [
        group([ell(0, 0, 150, 150), fill(GLOW, 15)], tr(s=anim([(0, [100, 100]), (45, [112, 112]), (90, [100, 100])])), "Halo luna"),
        group([ell(0, 0, 104, 104), fill(MOON)], name="Luna"),
        group([ell(-18, -10, 22, 22), ell(14, 16, 28, 28), ell(20, -24, 14, 14), fill(MOON_D)], name="Crateres"),
    ]
    add("Luna", moon, ks(p=anim([(0, [392, 112, 0]), (45, [392, 106, 0]), (90, [392, 112, 0])])))

    # ---- telaranas de las esquinas
    def web(x, y, sx):
        lines = []
        for k in range(5):
            ang = math.radians(k * 22.5)
            lines.append(poly([(0, 0), (math.cos(ang) * 110, math.sin(ang) * 110)], closed=False))
        for r in (36, 66, 96):
            pts = [(math.cos(math.radians(a)) * r * (0.92 if i % 2 else 1.0), math.sin(math.radians(a)) * r * (0.92 if i % 2 else 1.0)) for i, a in enumerate([0, 22.5, 45, 67.5, 90])]
            lines.append(poly(pts, closed=False))
        return group(lines + [stroke(WEB, 2.2, 70)], tr(p=(x, y), s=(sx, 100)), "Telarana")
    add("Telaranas", [web(0, 0, 100), web(512, 0, -100)])

    return {"v": "5.7.4", "fr": FPS, "ip": 0, "op": END, "w": W, "h": H, "nm": "ParDos - Mega actualizacion de Halloween", "ddd": 0, "assets": [], "layers": layers,
            "meta": {"g": "ParDos", "a": "ParDos", "k": "halloween,evento,parDos", "d": "Evento de Halloween (vector, 3 s en bucle)"}}


if __name__ == "__main__":
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "docs", "halloween", "halloween_mega_update.json")
    os.makedirs(os.path.dirname(os.path.abspath(out)), exist_ok=True)
    data = make()
    with open(out, "w", encoding="utf-8") as fh:
        json.dump(data, fh, ensure_ascii=False, separators=(",", ":"))
    print("listo", os.path.abspath(out), os.path.getsize(out), "bytes", len(data["layers"]), "capas")
