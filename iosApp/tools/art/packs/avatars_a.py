"""Avatares vectoriales (parte A): FOX CAT PANDA BUNNY BEAR FROG OWL PENGUIN.

Portado de drawAvatar (AvatarArt.kt): mismas coordenadas, lienzo 100x100, sin fondo ni accesorios.
Ranuras: $head $light $dark $inner $bg1 $bg2 $shirt.
"""
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, ".."))   # artlib
sys.path.insert(0, HERE)                        # _looks
from artlib import *  # noqa: E402,F401,F403
import _looks  # noqa: E402

ANIMALS = ["FOX", "CAT", "PANDA", "BUNNY", "BEAR", "FROG", "OWL", "PENGUIN"]

INK = "#2B2B3A"
WHITE = "#FFFFFF"
BLACK = "#000000"
BEAK = "#F2A93B"
OL = mix("$head", "$dark", .45)          # contorno fino del cuerpo (mismo tono, mas oscuro)


# ------------------------------------------------------------------ utilidades

def lite(c, t):
    return mix(c, WHITE, t)


def shade(c, t):
    return mix(c, "$dark", t)


def vlin(y1, y2, top, bot, x=0):
    return lin(x, y1, x, y2, [(0, top), (1, bot)])


def body_paint(cx=38, cy=36, r=60, hi=.34, lo=.22, base="$head"):
    """Degradado de masa de color: luz arriba-izquierda, sombra abajo-derecha."""
    return rad(cx, cy, r, [(0, lite(base, hi)), (.45, base), (1, shade(base, lo))])


def new(animal):
    return Scene(100, 100, pal=_looks.palette(animal))


def both(s, fn):
    """Dibuja fn() a la izquierda y su espejo a la derecha."""
    fn()
    with s.flip_x(50):
        fn()


def rpoly(pts, r=1.5):
    """Poligono con esquinas redondeadas (curvas cuadraticas en cada vertice)."""
    n = len(pts)
    rs = r if isinstance(r, (list, tuple)) else [r] * n
    p = Path()
    for i in range(n):
        v, pv, nv = pts[i], pts[i - 1], pts[(i + 1) % n]
        d1 = math.hypot(pv[0] - v[0], pv[1] - v[1])
        d2 = math.hypot(nv[0] - v[0], nv[1] - v[1])
        k1 = min(rs[i], d1 / 2.0) / d1
        k2 = min(rs[i], d2 / 2.0) / d2
        a = (v[0] + (pv[0] - v[0]) * k1, v[1] + (pv[1] - v[1]) * k1)
        b = (v[0] + (nv[0] - v[0]) * k2, v[1] + (nv[1] - v[1]) * k2)
        if i == 0:
            p.M(*a)
        else:
            p.L(*a)
        p.Q(v[0], v[1], b[0], b[1])
    p.Z()
    return p


# ------------------------------------------------------------------ piezas comunes

def shoulders(s):
    """Hombros (ranura shirt): elipse (50,104) 40x22 como en Kotlin, con volumen, y sombra bajo la barbilla."""
    x2 = 40 * math.sqrt(1 - (2 / 22.0) ** 2)
    top = Path().M(50 - x2, 102).A(40, 22, 0, 0, 1, 50 + x2, 102)
    closed = Path().M(50 - x2, 102).A(40, 22, 0, 0, 1, 50 + x2, 102).Z()
    s.path(closed, fill=lin(12, 82, 88, 102, [(0, lite("$shirt", .28)), (.5, "$shirt"), (1, mix("$shirt", BLACK, .18))]))
    s.path(top, stroke=mix("$shirt", "#2B1B3A", .38), sw=1.4, join=JOIN_ROUND)
    # brillo del hombro (media luna siguiendo el borde) y sombra suave bajo la barbilla
    pts = [(50 + 38 * math.cos(math.radians(a)), 104 + 20 * math.sin(math.radians(a))) for a in (203, 222, 241)]
    s.curve(pts, "#FFFFFF59", 1.7)
    s.ellipse(50, 88.5, 25, 6.5, fill=lin(0, 83, 0, 95, [(0, "#00000040"), (1, "#00000000")]))


def head(s, shine=True):
    s.ellipse(50, 58, 33, 29, fill=body_paint(), stroke=OL, sw=1.6)
    if shine:
        s.ellipse(41, 37, 13, 5, fill=lin(28, 32, 54, 42, [(0, "#FFFFFF99"), (1, "#FFFFFF1F")]))
        s.circle(55.5, 34.4, 1.3, fill="#FFFFFF73")


def cheeks(s, y=68, r=7.4):
    for cx in (27, 73):
        s.circle(cx, y, r, fill=rad(cx, y, r, [(0, "#FF8FA3B3"), (.6, "#FF8FA380"), (1, "#FF8FA300")]))


def smile(s, cx, cy, w, color=INK, sw=1.9):
    """Boca de dos arcos (como smile() de Kotlin: dos arcos de 15 a 165 grados)."""
    rx, ry = w / 2.0, 3.5
    a0, a1 = math.radians(15), math.radians(165)
    p = Path()
    for x0 in (cx - w, cx):
        ccx, ccy = x0 + rx, cy - 3 + ry
        p.M(ccx + rx * math.cos(a0), ccy + ry * math.sin(a0))
        p.A(rx, ry, 0, 0, 1, ccx + rx * math.cos(a1), ccy + ry * math.sin(a1))
    s.path(p, stroke=color, sw=sw, cap=CAP_ROUND, join=JOIN_ROUND)


def glossy_nose(s, cx, cy, rx, ry, color=INK, hi=.2):
    s.ellipse(cx, cy, rx, ry, fill=lin(cx, cy - ry, cx, cy + ry, [(0, mix(color, WHITE, hi)), (1, color)]))
    s.ellipse(cx - rx * .32, cy - ry * .38, rx * .36, ry * .26, fill="#FFFFFF8C")


def philtrum(s, y0, y1, color=INK):
    s.line(50, y0, 50, y1, color, 1.5)


def eye(s, cx, cy, r, white=False, line=INK):
    """Ojo: abierto (etiqueta ojos abiertos) y cerrado (parpadeo)."""
    with s.tag(TAG_EYES_OPEN):
        if white:
            R = r * 1.7
            s.circle(cx, cy, R, fill=rad(cx - R * .3, cy - R * .35, R * 1.6, [(0, WHITE), (1, "#DCDFEC")]),
                     stroke="#2B2B3A47", sw=1.2)
        s.circle(cx, cy, r, fill=rad(cx - r * .3, cy - r * .4, r * 1.7, [(0, "#53536E"), (.55, INK), (1, "#191924")]))
        s.circle(cx - r * .3, cy - r * .35, r * .38, fill=WHITE)
        s.circle(cx + r * .4, cy + r * .42, r * .17, fill="#FFFFFFB3")
    with s.tag(TAG_EYES_CLOSED):
        lw = 1.3 if white else 1.05
        p = Path().M(cx - lw * r, cy - .15 * r).Q(cx, cy + .95 * r, cx + lw * r, cy - .15 * r)
        s.path(p, stroke=line, sw=max(1.5, r * .46), cap=CAP_ROUND, join=JOIN_ROUND)


# ------------------------------------------------------------------ animales

def fox():
    s = new("FOX")
    shoulders(s)

    def ear():
        s.path(rpoly([(18, 42), (26, 6), (46, 32)], [1.2, 3, 1.2]),
               fill=vlin(6, 42, lite("$head", .2), shade("$head", .14)), stroke=OL, sw=1.5, join=JOIN_ROUND)
        s.path(rpoly([(24, 34), (27, 14), (38, 31)], [1, 2.2, 1]),
               fill=vlin(14, 34, mix("$dark", "$head", .22), "$dark"))
        s.path(Path().M(28.4, 22).Q(28, 27, 30, 31), stroke="#FFFFFF2E", sw=1.1, cap=CAP_ROUND)
    both(s, ear)

    head(s)

    # mejillas claras y peludas (sobresalen un poco de la cabeza, como en Kotlin)
    patches = [(34, 72, 20, 14), (66, 72, 20, 14), (50, 74, 12, 10)]
    fur = lin(50, 58, 50, 87, [(0, "$light"), (1, mix("$light", "$head", .32))])
    tuft_a = rpoly([(22, 65), (9, 69.5), (16.5, 75)], [1.2, 1.4, 1.2])
    tuft_b = rpoly([(16.5, 72), (10, 82), (22, 80.5)], [1.2, 1.4, 1.2])

    def tufts(paint, stroke_only):
        for t in (tuft_a, tuft_b):
            if stroke_only:
                s.path(t, stroke=OL, sw=2.6, join=JOIN_ROUND)
            else:
                s.path(t, fill=paint)
    for (cx, cy, rx, ry) in patches:
        s.ellipse(cx, cy, rx, ry, stroke=OL, sw=2.6)
    tufts(fur, True)
    with s.flip_x(50):
        tufts(fur, True)
    for (cx, cy, rx, ry) in patches:
        s.ellipse(cx, cy, rx, ry, fill=fur)
    tufts(fur, False)
    with s.flip_x(50):
        tufts(fur, False)

    cheeks(s)
    eye(s, 37, 57, 4.4)
    eye(s, 63, 57, 4.4)
    glossy_nose(s, 50, 65.75, 5, 3.75)
    philtrum(s, 69, 72.2)
    smile(s, 50, 71, 5.5)
    return s.bake()


def cat():
    s = new("CAT")
    shoulders(s)

    def ear():
        s.path(rpoly([(18, 44), (24, 8), (46, 32)], [1.2, 3, 1.2]),
               fill=vlin(8, 44, lite("$head", .2), shade("$head", .14)), stroke=OL, sw=1.5, join=JOIN_ROUND)
        s.path(rpoly([(24, 36), (26, 16), (38, 32)], [1, 2.4, 1]),
               fill=vlin(16, 36, lite("$inner", .35), "$inner"))
    both(s, ear)

    head(s)

    stripe = mix("$head", "$dark", .35)
    for dx in (-7, 0, 7):
        s.line(50 + dx, 33, 50 + dx * .9, 41, stripe, 2.6)

    # hocico claro
    s.ellipse(50, 75, 14, 9, fill=lin(50, 66, 50, 84, [(0, "$light"), (1, mix("$light", "$head", .3))]),
              stroke=alpha(OL, .45), sw=1)
    cheeks(s)
    eye(s, 37, 57, 4.4)
    eye(s, 63, 57, 4.4)
    # bigotes
    for sd in (-1, 1):
        for k in range(3):
            s.line(50 + sd * 15, 66 + k * 3, 50 + sd * 30, 62 + k * 6, alpha("$dark", .5), 1.2)
    glossy_nose(s, 50, 65.75, 3.5, 2.75, color="$inner", hi=.3)
    s.line(50, 68.4, 50, 71.2, INK, 1.4)
    smile(s, 50, 70, 5.5, color="$dark")
    return s.bake()


def panda():
    s = new("PANDA")
    shoulders(s)

    def ear():
        s.circle(24, 34, 12, fill=rad(20, 30, 17, [(0, mix("$dark", WHITE, .22)), (.5, "$dark"), (1, mix("$dark", BLACK, .2))]))
        s.ellipse(21.5, 29.5, 5, 2.5, fill="#FFFFFF2E")
    both(s, ear)

    head(s)

    # manchas de los ojos (ovalos girados 22 grados)
    def patch(cx, rot, px, py):
        with s.rotate(rot, px, py):
            s.ellipse(cx, 58, 9.5, 12, fill=lin(cx, 46, cx, 70, [(0, mix("$dark", WHITE, .16)), (1, mix("$dark", BLACK, .15))]))
    patch(36.5, 22, 36, 57)
    patch(63.5, -22, 64, 57)

    cheeks(s)
    eye(s, 36, 57, 3.6, white=True, line="$light")
    eye(s, 64, 57, 3.6, white=True, line="$light")
    glossy_nose(s, 50, 65.5, 5, 3.5, hi=.25)
    philtrum(s, 68.5, 71.2)
    smile(s, 50, 70, 5.5)
    return s.bake()


def bunny():
    s = new("BUNNY")
    shoulders(s)

    def ear():
        with s.rotate(-9, 33, 38):
            s.ellipse(33, 23.5, 9, 20.5, fill=lin(26, 4, 40, 44, [(0, lite("$head", .3)), (.5, "$head"), (1, shade("$head", .2))]),
                      stroke=OL, sw=1.5)
            s.ellipse(33, 23.5, 5.2, 14, fill=lin(33, 9, 33, 38, [(0, lite("$inner", .45)), (1, "$inner")]))
            s.ellipse(31.2, 15.5, 1.6, 4.5, fill="#FFFFFF59")
    both(s, ear)

    head(s)

    s.ellipse(50, 73, 12, 9, fill=lin(50, 64, 50, 82, [(0, "$light"), (1, mix("$light", "$head", .45))]),
              stroke=alpha(OL, .3), sw=1)
    cheeks(s)
    eye(s, 37, 57, 4.4)
    eye(s, 63, 57, 4.4)
    glossy_nose(s, 50, 65.75, 3.5, 2.75, color="$inner", hi=.3)
    s.line(50, 68.4, 50, 71, "$dark", 1.4)
    smile(s, 50, 70, 5, color="$dark")
    # dientes
    for x in (46.5, 50.3):
        s.rect(x, 73, 3.2, 5, r=1, fill=WHITE, stroke=mix(WHITE, "$dark", .35), sw=.7)
    return s.bake()


def bear():
    s = new("BEAR")
    shoulders(s)

    def ear():
        s.circle(24, 34, 12, fill=body_paint(20, 29, 20), stroke=OL, sw=1.5)
        s.circle(24, 35, 6.5, fill=lin(20, 29, 28, 41, [(0, shade("$inner", .12)), (1, lite("$inner", .15))]))
    both(s, ear)

    head(s)

    s.ellipse(50, 73.5, 15, 11.5, fill=lin(50, 62, 50, 85, [(0, lite("$light", .15)), (1, mix("$light", "$head", .3))]),
              stroke=alpha(OL, .35), sw=1)
    cheeks(s)
    eye(s, 37, 57, 4.4)
    eye(s, 63, 57, 4.4)
    glossy_nose(s, 50, 65.5, 5, 3.5)
    philtrum(s, 68.5, 71.2)
    smile(s, 50, 70, 5.5)
    return s.bake()


def frog():
    s = new("FROG")
    shoulders(s)

    # ojos saltones por encima de la cabeza
    for cx in (33, 67):
        s.circle(cx, 35, 13, fill=body_paint(cx - 4, 29, 20), stroke=OL, sw=1.6)

    head(s)
    for (x, y, r) in ((50, 37.5, 2.3), (45, 42.5, 1.7), (56, 42, 1.9)):
        s.circle(x, y, r, fill=alpha(shade("$head", .6), .16))

    # barbilla clara
    s.ellipse(50, 74.5, 20, 8.5, fill=lin(50, 66, 50, 83, [(0, "$light"), (1, mix("$light", "$head", .35))]),
              stroke=alpha(OL, .3), sw=1)
    cheeks(s)
    for cx in (33, 67):
        s.ellipse(cx - 6, 28.6, 3.6, 1.7, fill="#FFFFFF73")
    eye(s, 33, 35, 4.6, white=True)
    eye(s, 67, 35, 4.6, white=True)
    for x in (46, 54):
        s.ellipse(x, 62.6, 1.1, .8, fill=alpha("$dark", .6))
    # boca grande
    a0, a1 = math.radians(15), math.radians(165)
    mouth = Path().M(50 + 16 * math.cos(a0), 69 + 7 * math.sin(a0)).A(16, 7, 0, 0, 1, 50 + 16 * math.cos(a1), 69 + 7 * math.sin(a1))
    s.path(mouth, stroke="$dark", sw=2.2, cap=CAP_ROUND, join=JOIN_ROUND)
    return s.bake()


def owl():
    s = new("OWL")
    shoulders(s)

    def tuft():
        s.path(rpoly([(20, 42), (22, 10), (44, 32)], [1.2, 3, 1.2]),
               fill=vlin(10, 42, lite("$head", .2), shade("$head", .16)), stroke=OL, sw=1.5, join=JOIN_ROUND)
        s.path(rpoly([(25, 36), (25.5, 18), (38, 32)], [1, 2.2, 1]),
               fill=vlin(18, 36, lite("$inner", .2), shade("$inner", .12)))
    both(s, tuft)

    head(s)

    # plumas de la frente
    for (x, y) in ((50, 38.5), (42, 36), (58, 36), (29.5, 41), (70.5, 41)):
        s.path(Path().M(x - 3.8, y - 2.2).Q(x, y + 2.6, x + 3.8, y - 2.2), stroke=alpha("$dark", .3), sw=1.5, cap=CAP_ROUND)
    for (x, y) in ((50, 83), (42, 84.5), (58, 84.5), (34, 82.5), (66, 82.5)):
        s.path(Path().M(x - 3.4, y - 2).Q(x, y + 2.4, x + 3.4, y - 2), stroke=alpha("$dark", .26), sw=1.4, cap=CAP_ROUND)

    # discos faciales
    rim = mix("$head", "$dark", .32)
    disc = lin(50, 41, 50, 71, [(0, lite("$light", .2)), (1, mix("$light", "$head", .22))])
    for cx in (36, 64):
        s.circle(cx, 56, 15, stroke=rim, sw=4.4)
    for cx in (36, 64):
        s.circle(cx, 56, 15, fill=disc)
        s.circle(cx, 56, 15, stroke=rim, sw=2.2)

    cheeks(s, y=73, r=6)
    eye(s, 36, 56, 6.5, white=True)
    eye(s, 64, 56, 6.5, white=True)

    # pico
    beak_stroke = mix(BEAK, "#7A4A10", .5)
    s.path(rpoly([(44, 64), (56, 64), (50, 76)], [2.2, 2.2, 2.4]),
           fill=vlin(64, 76, mix(BEAK, WHITE, .35), mix(BEAK, "#B87510", .25)), stroke=beak_stroke, sw=1, join=JOIN_ROUND)
    s.ellipse(47.6, 66.2, 2.2, 1.1, fill="#FFFFFF80")
    s.line(46.5, 67.8, 53.5, 67.8, alpha(beak_stroke, .55), .9)
    return s.bake()


def penguin():
    s = new("PENGUIN")
    shoulders(s)
    head(s)

    # mascara facial blanca (dos ovalos que forman un corazon)
    mask = lin(50, 44, 50, 84, [(0, WHITE), (1, mix("$light", "$dark", .12))])
    edge = alpha(mix("$light", "$dark", .5), .45)
    for cx in (38, 62):
        s.ellipse(cx, 64, 16, 20, stroke=edge, sw=2.2)
    for cx in (38, 62):
        s.ellipse(cx, 64, 16, 20, fill=mask)

    cheeks(s)
    eye(s, 38, 58, 4, line=INK)
    eye(s, 62, 58, 4, line=INK)
    # pico
    beak_stroke = mix(BEAK, "#7A4A10", .5)
    s.path(rpoly([(43, 65), (57, 65), (50, 74)], [2.2, 2.2, 2.4]),
           fill=vlin(65, 74, mix(BEAK, WHITE, .35), mix(BEAK, "#B87510", .25)), stroke=beak_stroke, sw=1, join=JOIN_ROUND)
    s.ellipse(47.4, 67.2, 2.4, 1.1, fill="#FFFFFF80")
    s.line(45.2, 68.9, 54.8, 68.9, alpha(beak_stroke, .55), .9)
    return s.bake()


# ------------------------------------------------------------------ paquete

def build():
    icons = {
        "animal.FOX": fox(),
        "animal.CAT": cat(),
        "animal.PANDA": panda(),
        "animal.BUNNY": bunny(),
        "animal.BEAR": bear(),
        "animal.FROG": frog(),
        "animal.OWL": owl(),
        "animal.PENGUIN": penguin(),
    }
    return {"icons": icons, "palettes": _looks.all_palettes(ANIMALS)}
