"""Avatares vectoriales (parte C): LION WOLF SHEEP HEDGEHOG TURTLE PIG MONKEY HAMSTER.

Portado de drawAvatar (AvatarArt.kt): mismas coordenadas, lienzo 100x100, sin fondo ni accesorios.
Ranuras: $head $light $dark $inner $bg1 $bg2 $shirt. Mismas piezas comunes que avatars_a (hombros, cabeza,
mejillas, ojos, boca) para que todos los animales se vean de la misma familia.
"""
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, ".."))   # artlib
sys.path.insert(0, HERE)                        # _looks
from artlib import *  # noqa: E402,F401,F403
import _looks  # noqa: E402

ANIMALS = ["LION", "WOLF", "SHEEP", "HEDGEHOG", "TURTLE", "PIG", "MONKEY", "HAMSTER"]

INK = "#2B2B3A"
WHITE = "#FFFFFF"
BLACK = "#000000"
OL = mix("$head", "$dark", .45)          # contorno fino del cuerpo (mismo tono, mas oscuro)


# ------------------------------------------------------------------ utilidades

def lite(c, t):
    return mix(c, WHITE, t)


def shade(c, t):
    return mix(c, "$dark", t)


def hexmix(a, b, t):
    """Mezcla de dos colores fijos calculada ahora (el JSON queda mas corto)."""
    return _looks._lerp_hex(a, b, t)


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
    # brillo del hombro y cuello
    s.ellipse(37, 89, 15, 4.6, fill=lin(22, 86, 52, 92, [(0, "#FFFFFF4D"), (1, "#FFFFFF0D")]))
    s.ellipse(50, 88.5, 25, 5, fill="#0000002E")


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


def muzzle(s, cx, cy, rx, ry, top=.15, bot=.3, edge=.35):
    """Hocico / mancha clara con volumen (ranura light)."""
    s.ellipse(cx, cy, rx, ry, fill=vlin(cy - ry, cy + ry, lite("$light", top), mix("$light", "$head", bot)),
              stroke=alpha(OL, edge), sw=1)


# ------------------------------------------------------------------ animales

def lion():
    s = new("LION")
    shoulders(s)

    # melena: 14 mechones alternando dos tonos fijos
    A, B = "#B5661E", "#C7782A"
    for i in range(14):
        ang = math.radians(i * 360.0 / 14.0)
        cx, cy = round(50 + math.cos(ang) * 35, 1), round(58 + math.sin(ang) * 33, 1)
        base = A if i % 2 == 0 else B
        s.circle(cx, cy, 12.5,
                 fill=rad(cx - 4, cy - 5, 18, [(0, hexmix(base, "#FFD9A0", .4)), (.55, base), (1, hexmix(base, "#5A2D08", .32))]),
                 stroke="#5A2D0859", sw=1)
    s.circle(50, 58, 36, fill=lin(30, 24, 70, 94, [(0, "#C98031"), (1, "#A55A17")]))
    # sombra de la cara sobre la melena
    s.circle(50, 58, 37, fill=rad(50, 58, 37, [(0, "#3A1A0673"), (.8, "#3A1A0673"), (1, "#3A1A0600")]))
    # pelitos de la melena
    for i in range(14):
        ang = math.radians(i * 360.0 / 14.0 + 6)
        r0, r1 = 38.5, 45.5
        s.line(round(50 + math.cos(ang) * r0, 1), round(58 + math.sin(ang) * (r0 * .94), 1),
               round(50 + math.cos(ang + .06) * r1, 1), round(58 + math.sin(ang + .06) * (r1 * .94), 1),
               "#5A2D0859", 1.3)

    def ear():
        s.circle(30, 27, 7, fill=body_paint(27, 23, 11), stroke=OL, sw=1.2)
        s.circle(30, 28, 3.4, fill=vlin(24.6, 31.4, shade("$inner", .28), "$inner"))
    both(s, ear)

    head(s)

    # mechon sobre la frente
    for (x, y, w) in ((44, 33.5, 4.2), (50, 34.5, 4.6), (56, 33.5, 4.2)):
        s.path(Path().M(x - w, y - 3).Q(x - w * .2, y + 3.2, x + w, y - 3), stroke=alpha("#8A4A16", .4), sw=1.4, cap=CAP_ROUND)

    muzzle(s, 50, 73.5, 15, 11.5)
    cheeks(s)
    eye(s, 37, 57, 4.4)
    eye(s, 63, 57, 4.4)

    # nariz triangular
    s.path(rpoly([(44, 62), (56, 62), (50, 69)], [2, 2, 2.2]), fill=vlin(62, 69, "#7A4F30", "#5C3A21"),
           stroke="#3E2412", sw=.8, join=JOIN_ROUND)
    s.ellipse(47.6, 63.3, 2.3, 1, fill="#FFFFFF80")
    philtrum(s, 68.5, 72.4, "#5C3A21")
    smile(s, 50, 72, 6, color="$dark")
    for sd in (-1, 1):
        for (dx, dy) in ((7.5, 75.5), (10.5, 72.3), (10.5, 78)):
            s.circle(50 + sd * dx, dy, .8, fill=alpha("$dark", .45))
    return s.bake()


def wolf():
    s = new("WOLF")
    shoulders(s)
    earc = mix("$head", "$dark", .35)

    def ear():
        s.path(rpoly([(18, 48), (23, 3), (46, 32)], [1.5, 3, 1.5]),
               fill=lin(23, 3, 32, 46, [(0, shade("$head", .5)), (.55, earc), (1, mix("$head", "$dark", .12))]),
               stroke=OL, sw=1.5, join=JOIN_ROUND)
        s.path(rpoly([(24, 38), (26, 14), (38, 32)], [1.2, 2.6, 1.2]),
               fill=vlin(14, 38, lite("$inner", .25), mix("$inner", "$head", .25)))
        s.path(Path().M(27.2, 21).Q(27, 27, 29.5, 31), stroke="#FFFFFF38", sw=1.1, cap=CAP_ROUND)
    both(s, ear)

    head(s)

    # marca oscura de la frente
    s.ellipse(50, 37, 8, 7, fill=rad(50, 37, 8.5, [(0, alpha("$dark", .42)), (1, alpha("$dark", 0))]))

    # mechones de las mejillas
    fur = lin(8, 56, 24, 74, [(0, lite("$light", .15)), (1, mix("$light", "$head", .3))])

    def tuft():
        p = Path().M(17, 55.5).C(13.5, 58.5, 9.5, 62, 7, 67).C(12, 68, 18, 70.5, 23, 73.2).Z()
        s.path(p, fill=fur, stroke=alpha(OL, .55), sw=1, join=JOIN_ROUND)
    both(s, tuft)

    muzzle(s, 50, 73.5, 17, 13.5)
    cheeks(s)
    eye(s, 37, 57, 4.4)
    eye(s, 63, 57, 4.4)
    glossy_nose(s, 50, 65.75, 5.5, 3.75)
    philtrum(s, 69, 73.5)
    smile(s, 50, 73, 5.5)
    return s.bake()


def sheep():
    s = new("SHEEP")
    shoulders(s)

    # lana de atras
    wool = [(26, 36, 13), (40, 26, 13), (50, 23, 13), (60, 26, 13), (74, 36, 13),
            (17, 52, 13), (83, 52, 13), (17, 68, 13), (83, 68, 13)]
    for (x, y, r) in wool:
        s.circle(x, y, r, fill=rad(x - 4, y - 5, r * 1.9, [(0, WHITE), (.5, "$light"), (1, mix("$light", "$dark", .2))]),
                 stroke=alpha(mix("$light", "$dark", .55), .35), sw=1.1)

    # orejas caidas, por encima de la lana
    def ear():
        with s.rotate(-16, 25, 54):
            s.ellipse(14.5, 54, 11.5, 5.8, fill=body_paint(10, 51, 18), stroke=OL, sw=1.3)
            s.ellipse(15.5, 54.2, 7, 2.9, fill=vlin(51, 57, shade("$inner", .2), lite("$inner", .1)))
    both(s, ear)

    head(s)

    # flequillo de lana
    for (x, y, r) in ((41, 32, 7.5), (52, 30, 8), (61, 33, 7)):
        s.circle(x, y, r, stroke=alpha(mix("$light", "$dark", .55), .45), sw=2.4)
    for (x, y, r) in ((41, 32, 7.5), (52, 30, 8), (61, 33, 7)):
        s.circle(x, y, r, fill=rad(x - 2.5, y - 3, r * 2, [(0, WHITE), (.5, "$light"), (1, mix("$light", "$dark", .16))]))

    s.ellipse(50, 69.5, 13, 9.5, fill=rad(50, 66, 15, [(0, "#FFFFFF59"), (1, "#FFFFFF00")]))
    cheeks(s)
    eye(s, 37, 57, 4.4)
    eye(s, 63, 57, 4.4)
    glossy_nose(s, 50, 65.75, 4, 2.75, color="$dark", hi=.25)
    philtrum(s, 68, 70.8, "$dark")
    smile(s, 50, 70, 4.5, color="$dark")
    return s.bake()


def hedgehog():
    s = new("HEDGEHOG")
    shoulders(s)

    def crown(r_out, r_in, n, phase, fill, stroke, sw):
        pts = []
        for i in range(n + 1):
            r = r_out if (i + phase) % 2 == 0 else r_in
            a = math.pi + i * math.pi / n
            pts.append((round(50 + r * math.cos(a), 1), round(66 + r * math.sin(a), 1)))
        s.poly(pts, fill=fill, stroke=stroke, sw=sw, join=JOIN_ROUND)

    tip = mix("$dark", "$head", .5)
    crown(49, 38, 20, 0,
          rad(50, 66, 49, [(0, "$dark"), (.6, "$dark"), (1, tip)]),
          alpha(mix("$dark", "$light", .5), .55), 1)
    crown(44, 34, 20, 1,
          rad(50, 66, 44, [(0, mix("$dark", "$head", .12)), (.6, mix("$dark", "$head", .3)), (1, mix("$dark", "$head", .62))]),
          alpha(mix("$dark", "$light", .45), .5), .9)
    # brillo en las puntas
    for i in range(1, 20, 2):
        a = math.pi + i * math.pi / 20
        s.line(round(50 + 38 * math.cos(a), 1), round(66 + 38 * math.sin(a), 1),
               round(50 + 42.5 * math.cos(a), 1), round(66 + 42.5 * math.sin(a), 1), "#FFFFFF38", .9)

    def ear():
        s.circle(25, 40, 6.5, fill=body_paint(23, 37, 10), stroke=OL, sw=1.1)
        s.circle(25, 41, 3.2, fill=vlin(38, 44, shade("$inner", .25), "$inner"))
    both(s, ear)

    head(s)

    # careta clara
    s.ellipse(50, 67.6, 24, 19.4, fill=rad(50, 58, 28, [(0, lite("$light", .35)), (1, mix("$light", "$head", .3))]),
              stroke=alpha(OL, .35), sw=1)
    cheeks(s)
    eye(s, 37, 57, 4.4)
    eye(s, 63, 57, 4.4)
    glossy_nose(s, 50, 66, 4.2, 4.0, hi=.25)
    philtrum(s, 69.5, 73.5)
    smile(s, 50, 74, 4)
    return s.bake()


def turtle():
    s = new("TURTLE")
    shoulders(s)
    G1, G2, GL = "#6B8E4E", "#86A862", "#4F6E36"

    # caparazon
    s.ellipse(50, 57, 44, 37, fill=lin(12, 22, 88, 94, [(0, hexmix(G1, WHITE, .2)), (.5, G1), (1, hexmix(G1, "#2B3A1A", .32))]),
              stroke=alpha(GL, .9), sw=1.4)
    for k in range(20):
        a = math.radians(k * 18 + 9)
        s.line(round(50 + 33 * math.cos(a), 1), round(54 + 26 * math.sin(a), 1),
               round(50 + 44 * math.cos(a), 1), round(57 + 37 * math.sin(a), 1), alpha(GL, .85), 1.5, cap=CAP_BUTT)
    s.ellipse(50, 54, 35, 28, fill=rad(38, 36, 52, [(0, hexmix(G2, WHITE, .28)), (.55, G2), (1, hexmix(G2, GL, .4))]),
              stroke=alpha(GL, .8), sw=1.2)
    for (x1, y1, x2, y2) in ((50, 26, 50, 40), (24, 40, 36, 34), (76, 40, 64, 34)):
        s.line(x1, y1, x2, y2, GL, 2.2)
    # brillo del borde del caparazon
    s.curve([(round(50 + 40 * math.cos(math.radians(a)), 1), round(57 + 33 * math.sin(math.radians(a)), 1))
             for a in range(196, 262, 11)], "#FFFFFF59", 2)

    head(s)
    for x in (29, 71):
        s.circle(x, 42, 3.4, fill=vlin(38.6, 45.4, shade("$head", .3), shade("$head", .16)))
    muzzle(s, 50, 74.5, 14, 8.5, top=.2)
    cheeks(s)
    eye(s, 37, 57, 4.4)
    eye(s, 63, 57, 4.4)
    for x in (46, 54):
        s.ellipse(x, 67, 1.5, 1.2, fill=alpha("$dark", .7))
    smile(s, 50, 73, 7, color="$dark")
    return s.bake()


def pig():
    s = new("PIG")
    shoulders(s)

    def ear():
        s.path(rpoly([(20, 44), (18, 14), (44, 32)], [1.5, 3.4, 1.5]),
               fill=vlin(14, 44, lite("$head", .22), shade("$head", .14)), stroke=OL, sw=1.5, join=JOIN_ROUND)
        s.path(rpoly([(24, 38), (23, 21), (38, 32)], [1.2, 2.6, 1.2]),
               fill=vlin(21, 38, lite("$inner", .3), shade("$inner", .08)))
        s.path(Path().M(22.6, 27).Q(22.4, 32, 24.4, 35), stroke="#FFFFFF40", sw=1.1, cap=CAP_ROUND)
    both(s, ear)

    head(s)

    # hocico
    s.ellipse(50, 70.5, 15, 10.5, fill=vlin(60, 81, lite("$inner", .38), "$inner"),
              stroke=alpha(mix("$inner", "$dark", .5), .6), sw=1.2)
    s.ellipse(46, 64.8, 6, 2, fill="#FFFFFF4D")
    for x in (44.2, 55.8):
        s.ellipse(x, 69.6, 2.3, 3.5, fill=alpha("$dark", .7))
        s.ellipse(x - .6, 68.2, .7, 1.1, fill="#FFFFFF59")
    cheeks(s)
    eye(s, 37, 57, 4.4)
    eye(s, 63, 57, 4.4)
    smile(s, 50, 77, 5, color="$dark")
    return s.bake()


def monkey():
    s = new("MONKEY")
    shoulders(s)

    def ear():
        s.circle(15, 58, 13, fill=body_paint(10, 51, 20), stroke=OL, sw=1.4)
        s.circle(15, 59, 8, fill=vlin(51, 67, shade("$light", .2), lite("$light", .1)))
    both(s, ear)

    head(s)

    # mechon de la frente
    hair = alpha(mix("$head", "$dark", .55), .8)
    for d in ("M 52 31 Q 51 25 46 24.5", "M 49 31 Q 46 26 41.5 27", "M 55 31 Q 56 26 61 26.5"):
        s.path(d, stroke=hair, sw=2.2, cap=CAP_ROUND)

    # careta clara (union de tres ovalos con contorno comun)
    patches = [(37, 59, 15, 13), (63, 59, 15, 13), (50, 73, 17, 13)]
    fur = lin(50, 46, 50, 86, [(0, lite("$light", .22)), (1, mix("$light", "$head", .28))])
    for (cx, cy, rx, ry) in patches:
        s.ellipse(cx, cy, rx, ry, stroke=alpha(OL, .75), sw=2.4)
    for (cx, cy, rx, ry) in patches:
        s.ellipse(cx, cy, rx, ry, fill=fur)

    cheeks(s)
    eye(s, 37, 57, 4.4)
    eye(s, 63, 57, 4.4)
    for x in (46.5, 53.5):
        s.circle(x, 66, 1.4, fill=alpha("$dark", .65))
    smile(s, 50, 74, 7, color="$dark")
    return s.bake()


def hamster():
    s = new("HAMSTER")
    shoulders(s)

    def ear():
        s.circle(27, 33, 9.5, fill=body_paint(23, 29, 15), stroke=OL, sw=1.3)
        s.circle(27, 34, 5.4, fill=vlin(29, 39, shade("$inner", .22), lite("$inner", .1)))
    both(s, ear)

    head(s)

    # franja oscura de la frente
    s.path(Path().M(50, 29.6).C(54.4, 31.6, 53.6, 40, 50, 46.5).C(46.4, 40, 45.6, 31.6, 50, 29.6).Z(),
           fill=vlin(29.6, 46.5, alpha(mix("$head", "$dark", .42), .9), alpha(mix("$head", "$dark", .3), .25)))

    # mofletes (sobresalen de la cabeza)
    patches = [(29, 71.5, 16, 13.5), (71, 71.5, 16, 13.5), (50, 72, 13, 10)]
    fur = lin(50, 58, 50, 86, [(0, lite("$light", .15)), (1, mix("$light", "$head", .3))])
    for (cx, cy, rx, ry) in patches:
        s.ellipse(cx, cy, rx, ry, stroke=alpha(OL, .8), sw=2.4)
    for (cx, cy, rx, ry) in patches:
        s.ellipse(cx, cy, rx, ry, fill=fur)
    s.ellipse(24, 66, 5.5, 2.2, fill="#FFFFFF59")
    s.ellipse(66, 66, 5.5, 2.2, fill="#FFFFFF59")

    cheeks(s, y=68)
    eye(s, 37, 56, 3.7)
    eye(s, 63, 56, 3.7)
    glossy_nose(s, 50, 63.75, 3.5, 2.75, color="$inner", hi=.3)
    s.line(50, 66, 50, 69, "$dark", 1.3)
    smile(s, 50, 68, 4, color="$dark")
    for x in (47.2, 50.1):
        s.rect(x, 70.5, 2.7, 4.6, r=1, fill=WHITE, stroke=mix(WHITE, "$dark", .35), sw=.7)
    return s.bake()


# ------------------------------------------------------------------ paquete

def build():
    icons = {
        "animal.LION": lion(),
        "animal.WOLF": wolf(),
        "animal.SHEEP": sheep(),
        "animal.HEDGEHOG": hedgehog(),
        "animal.TURTLE": turtle(),
        "animal.PIG": pig(),
        "animal.MONKEY": monkey(),
        "animal.HAMSTER": hamster(),
    }
    return {"icons": icons, "palettes": _looks.all_palettes(ANIMALS)}
