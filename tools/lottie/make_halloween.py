"""Genera la animacion Lottie (JSON) de la mega actualizacion de Halloween de ParDos.
Uso: python tools/lottie/make_halloween.py [salida.json]
Formato para tienda: lienzo 1920x1080 (16:9), 60 fps, 180 cuadros (3 s) en bucle sin costura, menos de 200 KB. Todo es vector (sin imagenes, sin emojis, sin texto):
luna con halo que respira, tres murcielagos que vuelan en bucle aleteando, una calabaza que flota con la cara encendida,
telaranas de esquina, estrellas que titilan y una colina con lapidas."""
import json
import math
import os
import sys

FPS, END, W, H = 60, 180, 1920, 1080
TF = FPS // 30            # los tiempos se escribieron a 30 fps; se multiplican para 60 fps
SW, SH = 910.22, 512.0     # espacio de la escena (16:9); la capa "Escena" lo escala a 1920x1080
K = W / SW
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
        kf = {"t": t * TF, "s": val}
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
    return {"ddd": 0, "ind": ind, "ty": 4, "nm": name, "sr": 1, "ks": kst or ks(), "ao": 0, "shapes": list(reversed(shapes)), "ip": 0, "op": END, **({"parent": 1} if ind > 1 else {}), "st": 0, "bm": 0}


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
    idx = [1]

    # capa 1: "Escena" (capa nula) que escala el espacio de 910x512 al lienzo de 1920x1080; todas las demas cuelgan de ella
    layers.append({"ddd": 0, "ind": 1, "ty": 3, "nm": "Escena", "sr": 1, "ks": {"o": static(0), "r": static(0), "p": static([0, 0, 0]), "a": static([0, 0, 0]), "s": static([K * 100, K * 100, 100])},
                   "ao": 0, "ip": 0, "op": END, "st": 0, "bm": 0})

    def add(name, shapes, kst=None):
        idx[0] += 1
        layers.append(layer(idx[0], name, shapes, kst))

    CX = SW / 2.0

    # ---- estrellas que titilan (arriba del todo)
    stars = [(64, 70, 9), (452, 96, 7), (120, 150, 6), (304, 40, 8), (40, 210, 5), (578, 200, 6), (700, 60, 8), (840, 150, 6),
             (210, 40, 6), (520, 120, 5), (790, 250, 5), (380, 190, 5), (880, 60, 7), (160, 250, 4)]
    for n, (x, y, s) in enumerate(stars):
        base = 20 + (n % 3) * 20
        shift = (n * 13) % 45
        # titileo periodico (2 destellos por vuelta) con desfase propio; los extremos coinciden para que el bucle no se note
        kf = [(tt, base + (100 - base) * (0.5 + 0.5 * math.sin(2 * math.pi * (tt + shift) / 45.0))) for tt in range(0, 91, 5)]
        kf[-1] = (90, kf[0][1])
        add("Estrella %d" % (n + 1), [group([sparkle_shape(s), fill(GLOW)], tr(p=(x, y)))], ks(o=anim(kf)))

    # ---- murcielagos: vuelan en un recorrido cerrado y aletean
    def bat(name, path, scale):
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
            kf = {"t": t * TF, "s": [x, y, 0]}
            if n < len(path) - 1:
                kf.update(EASE)
            pos["k"].append(kf)
        add(name, [group(body, tr())], ks(p=pos, s=(scale, scale)))

    bat("Murcielago 1", [(0, 110, 120), (22, 290, 80), (45, 520, 135), (68, 330, 175), (90, 110, 120)], 78)
    bat("Murcielago 2", [(0, 840, 240), (22, 700, 195), (45, 560, 255), (68, 740, 300), (90, 840, 240)], 62)
    bat("Murcielago 3", [(0, 560, 50), (22, 700, 95), (45, 850, 62), (68, 720, 28), (90, 560, 50)], 48)
    bat("Murcielago 4", [(0, 70, 265), (22, 190, 232), (45, 300, 285), (68, 160, 310), (90, 70, 265)], 42)

    # ---- calabaza (flota y respira) con la cara encendida
    face_glow = anim([(0, 88), (10, 100), (20, 78), (30, 100), (45, 90), (60, 100), (75, 80), (90, 88)])
    face = [poly([(-62, -22), (-26, -22), (-44, -58)]), poly([(26, -22), (62, -22), (44, -58)]), poly([(-8, 4), (8, 4), (0, -12)]),
            poly([(-70, 24), (-46, 52), (-26, 34), (-8, 56), (10, 34), (28, 56), (46, 52), (70, 24), (40, 74), (0, 82), (-40, 74)])]
    pumpkin = [
        group([ell(0, 78, 236, 34), fill("#000000", 22)], name="Sombra suelo"),
        group([ell(-62, 10, 120, 150), fill(ORANGE_D)], name="Gajo izq"),
        group([ell(62, 10, 120, 150), fill(ORANGE_D)], name="Gajo der"),
        group([ell(-32, 8, 130, 160), fill(ORANGE)], name="Gajo izq medio"),
        group([ell(32, 8, 130, 160), fill(ORANGE)], name="Gajo der medio"),
        group([ell(0, 6, 112, 168), fill(ORANGE_L)], name="Gajo centro"),
        group([ell(-26, -28, 26, 44), fill("#FFFFFF", 28)], name="Brillo"),
        group([poly([(-9, -86), (-12, -112), (14, -118), (24, -100), (10, -96)]), fill(STEM)], name="Rabito"),
        group(face + [fill(GLOW, 100), stroke(DARK, 5, 100)], tr(o=face_glow), "Cara encendida"),
    ]
    bob = anim([(0, [CX, 372, 0]), (23, [CX, 360, 0]), (45, [CX, 372, 0]), (68, [CX, 360, 0]), (90, [CX, 372, 0])])
    add("Calabaza", pumpkin, ks(p=bob, r=anim([(0, -2), (23, 2), (45, -2), (68, 2), (90, -2)]), s=(100, 100)))

    # halo naranja detras de la calabaza
    add("Halo calabaza", [group([ell(0, 0, 330, 270), fill(ORANGE, 11)], tr(s=anim([(0, [100, 100]), (45, [106, 106]), (90, [100, 100])])), "Halo")], ks(p=(CX, 350)))

    # ---- lapidas y colina (a todo el ancho)
    hill = bez([[-20, 512], [-20, 436], [160, 404], [330, 428], [455, 440], [600, 410], [760, 392], [930, 430], [930, 512]], [[0, 0]] * 9, [[0, 0]] * 9)
    tomb = lambda x, y, w, h: group([rect(x, y, w, h, 14), fill("#5A3A80")], name="Lapida")
    cross = lambda x, y: group([rect(x, y, 8, 30, 2), rect(x - 10, y + 8, 28, 7, 2), fill(VIOLET, 45)], name="Cruz")
    add("Colina", [group([hill, fill("#3E2160")], name="Colina"), tomb(36, 396, 40, 60), tomb(128, 410, 32, 48), tomb(740, 384, 44, 66), tomb(838, 400, 40, 58),
                   cross(60, 410), cross(790, 402)])

    # ---- luna con halo que respira
    moon = [
        group([ell(0, 0, 150, 150), fill(GLOW, 15)], tr(s=anim([(0, [100, 100]), (45, [112, 112]), (90, [100, 100])])), "Halo luna"),
        group([ell(0, 0, 104, 104), fill(MOON)], name="Luna"),
        group([ell(-18, -10, 22, 22), ell(14, 16, 28, 28), ell(20, -24, 14, 14), fill(MOON_D)], name="Crateres"),
    ]
    add("Luna", moon, ks(p=anim([(0, [740, 112, 0]), (45, [740, 106, 0]), (90, [740, 112, 0])])))

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
    add("Telaranas", [web(0, 0, 100), web(SW, 0, -100)])

    return {"v": "5.7.4", "fr": FPS, "ip": 0, "op": END, "w": W, "h": H, "nm": "ParDos - Mega actualizacion de Halloween", "ddd": 0, "assets": [], "layers": layers,
            "meta": {"g": "ParDos", "a": "ParDos", "k": "halloween,evento,parDos", "d": "Evento de Halloween (vector, 16:9, 60 fps, 3 s en bucle)"}}


if __name__ == "__main__":
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "docs", "halloween", "halloween_mega_update.json")
    os.makedirs(os.path.dirname(os.path.abspath(out)), exist_ok=True)
    data = make()
    with open(out, "w", encoding="utf-8") as fh:
        json.dump(data, fh, ensure_ascii=False, separators=(",", ":"))
    size = os.path.getsize(out)
    assert W / H == 1920 / 1080 and 1920 <= W <= 4096 and FPS == 60, "formato 16:9, ancho 1920-4096 y 60 fps"
    assert size <= 200 * 1024, "el archivo pasa de 200 KB: %d bytes" % size
    print("listo", os.path.abspath(out), size, "bytes (%.0f KB)" % (size / 1024.0), len(data["layers"]), "capas", W, "x", H, FPS, "fps")
