"""Adornos del borde del camino del mapa (parte A): dunas, pinos, rio, colinas, nubes, lavanda, farolillos, montanas.
Cada tipo tiene 3 variantes (_a, _b, _c). Dibujos de 100x100 con la base apoyada en y~90 y sombra de contacto.
"""
import os
import sys
import math

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, ".."))   # artlib
sys.path.insert(0, HERE)                        # _looks, _kit
from artlib import *                            # noqa: F401,F403
from artlib import _ellipse_cmds, _rrect_cmds   # noqa: E402

INK = "#2B1B3A"
WHITE = "#FFFFFF"
SHADOW = "#00000024"


# ------------------------------------------------------------------ ayudas

def lite(c, t=.35):
    return mix(c, WHITE, t)


def shade(c, t=.2):
    return mix(c, "#3A2650", t)


def edge(c, t=.45):
    return mix(c, INK, t)


def ground_shadow(s, cx=50, cy=90, rx=28, ry=4.2):
    s.ellipse(cx, cy, rx, ry, fill=SHADOW)


def glint(s, cx, cy, rx, ry, rot=-30, a=.55):
    with s.rotate(rot, cx, cy):
        s.ellipse(cx, cy, rx, ry, fill=alpha(WHITE, a))


def circ(cx, cy, r):
    return _ellipse_cmds(cx, cy, r, r)


def sky_glow(s, cx, cy, r, col="#FFD27A", a0=.6):
    s.circle(cx, cy, r, fill=rad(cx, cy, r, [(0, alpha(col, a0)), (1, alpha(col, 0))]))


def ribbon(pts, widths):
    """Contorno suave de una cinta que recorre pts con el ancho indicado en cada punto."""
    left, right = [], []
    n = len(pts)
    for i, (x, y) in enumerate(pts):
        x0, y0 = pts[max(0, i - 1)]
        x1, y1 = pts[min(n - 1, i + 1)]
        dx, dy = x1 - x0, y1 - y0
        d = math.hypot(dx, dy) or 1.0
        nx, ny = -dy / d, dx / d
        w = widths[i] / 2.0
        left.append((x + nx * w, y + ny * w))
        right.append((x - nx * w, y - ny * w))
    return left + right[::-1]


def tuft(s, x, y, k=1.0, col="#6FAE74"):
    """Matita de hierba: tres hojas en una sola forma."""
    d = ("M %g %g Q %g %g %g %g Q %g %g %g %g Z "
         "M %g %g Q %g %g %g %g Q %g %g %g %g Z "
         "M %g %g Q %g %g %g %g Q %g %g %g %g Z") % (
        x - 4 * k, y, x - 5 * k, y - 5 * k, x - 7 * k, y - 9 * k, x - 2.5 * k, y - 4 * k, x - 1 * k, y,
        x - 2 * k, y, x - 1 * k, y - 7 * k, x + 0.5 * k, y - 12 * k, x + 1.5 * k, y - 6 * k, x + 2 * k, y,
        x + 1 * k, y, x + 3 * k, y - 4 * k, x + 7 * k, y - 8 * k, x + 4 * k, y - 3 * k, x + 5 * k, y)
    s.path(d, fill=lin(x - 7 * k, y - 12 * k, x + 7 * k, y, [(0, lite(col, .3)), (1, shade(col, .15))]))


# ------------------------------------------------------------------ DUNAS

CACTUS = "#6FB07F"


def cactus(s, cx, by, k=1.0):
    s.ellipse(cx, by + 1, 10 * k, 2.4 * k, fill="#0000002C")
    g = lin(cx - 7 * k, 0, cx + 7 * k, 0, [(0, lite(CACTUS, .38)), (1, shade(CACTUS, .28))])
    # brazos
    s.path("M %g %g L %g %g L %g %g" % (cx - 4 * k, by - 18 * k, cx - 13 * k, by - 18 * k, cx - 13 * k, by - 31 * k),
           stroke=g, sw=7 * k, cap=CAP_ROUND, join=JOIN_ROUND)
    s.path("M %g %g L %g %g L %g %g" % (cx + 4 * k, by - 25 * k, cx + 13 * k, by - 25 * k, cx + 13 * k, by - 37 * k),
           stroke=g, sw=7 * k, cap=CAP_ROUND, join=JOIN_ROUND)
    # tronco
    s.rect(cx - 6.5 * k, by - 50 * k, 13 * k, 52 * k, r=6.5 * k, fill=g)
    s.line(cx - 2 * k, by - 44 * k, cx - 2 * k, by - 6 * k, alpha(WHITE, .35), 1.6 * k)
    s.line(cx - 13 * k, by - 35 * k, cx - 13 * k, by - 26 * k, alpha(WHITE, .3), 1.4 * k)
    # flor
    s.circle(cx, by - 51 * k, 3.4 * k, fill=lin(cx - 3, by - 55, cx + 3, by - 48, [(0, "#FF9DB4"), (1, "#E5576B")]))


def dune_a():
    s = Scene()
    ground_shadow(s, 50, 90, 40, 3.6)
    s.path("M 3 88 C 8 72 20 58 33 61 C 44 64 49 78 54 89 Z",
           fill=lin(10, 58, 40, 90, [(0, "#F9E3B4"), (1, "#EBCB92")]))
    s.path("M 8 89 C 24 83 38 50 58 36 C 68 44 80 72 93 89 C 70 94 30 94 8 89 Z",
           fill=lin(20, 40, 70, 92, [(0, "#FCE8B8"), (1, "#EBBE7C")]))
    s.path("M 58 36 C 68 44 80 72 93 89 C 80 92 66 92.5 52 91.5 C 61 76 63 54 58 36 Z",
           fill=lin(58, 36, 82, 92, [(0, "#DBA466"), (1, "#B97F4D")]))
    s.curve([(15, 86), (25, 81), (35, 82), (45, 77)], stroke=alpha("#C58F52", .5), sw=1.5)
    s.curve([(26, 90), (36, 86), (46, 87), (52, 83)], stroke=alpha("#C58F52", .4), sw=1.4)
    s.curve([(40, 52), (47, 44), (55, 38)], stroke=alpha(WHITE, .65), sw=2.2)
    return s.bake()


def dune_b():
    s = Scene()
    ground_shadow(s, 50, 90, 40, 3.6)
    sky_glow(s, 30, 30, 22, "#FFD27A", .55)
    s.circle(30, 30, 11, fill=rad(27, 27, 14, [(0, "#FFF0B0"), (1, "#F2B84A")]))
    glint(s, 26, 26, 3.4, 1.8, -35, .7)
    s.path("M 30 88 C 46 66 62 46 78 48 C 90 50 93 74 95 88 Z",
           fill=lin(40, 48, 90, 90, [(0, "#F7DDA6"), (1, "#E6B876")]))
    s.path("M 78 48 C 90 50 93 74 95 88 L 82 89 C 84 72 84 58 78 48 Z",
           fill=lin(78, 48, 90, 90, [(0, "#CF985A"), (1, "#B27846")]))
    s.path("M 5 90 C 13 74 26 58 42 58 C 56 58 62 78 71 90 C 50 95 25 95 5 90 Z",
           fill=lin(12, 58, 60, 92, [(0, "#FDEBC2"), (1, "#EDC27E")]))
    s.path("M 42 58 C 56 58 62 78 71 90 C 62 92 54 92 46 91.5 C 52 80 50 68 42 58 Z",
           fill=lin(42, 58, 66, 92, [(0, "#DFAA6C"), (1, "#BD834F")]))
    s.curve([(14, 82), (22, 74), (30, 67)], stroke=alpha(WHITE, .6), sw=2.2)
    s.curve([(14, 88), (24, 85), (34, 86)], stroke=alpha("#C58F52", .45), sw=1.4)
    return s.bake()


def dune_c():
    s = Scene()
    ground_shadow(s, 50, 90, 40, 3.6)
    s.path("M 4 90 C 18 72 42 58 60 60 C 78 62 88 76 95 90 C 70 95 30 95 4 90 Z",
           fill=lin(14, 60, 80, 92, [(0, "#FCE8B8"), (1, "#EBBE7C")]))
    s.path("M 60 60 C 78 62 88 76 95 90 C 80 93 68 93 56 92 C 64 80 66 70 60 60 Z",
           fill=lin(60, 60, 88, 92, [(0, "#DBA466"), (1, "#B97F4D")]))
    s.curve([(14, 85), (24, 81), (34, 82)], stroke=alpha("#C58F52", .45), sw=1.4)
    # piedrecillas
    s.ellipse(72, 83, 4, 2.2, fill=lin(68, 80, 76, 86, [(0, "#E9D7C0"), (1, "#B79B83")]))
    s.ellipse(80, 86, 2.6, 1.5, fill=lin(77, 84, 83, 88, [(0, "#E9D7C0"), (1, "#B79B83")]))
    cactus(s, 38, 82, 1.0)
    s.curve([(18, 76), (26, 70), (34, 66)], stroke=alpha(WHITE, .55), sw=2.0)
    return s.bake()


# ------------------------------------------------------------------ PINOS

PINE = "#4E9A72"
TRUNK = "#9A6A44"


def pine(s, cx, by, k, col=PINE, snow=False):
    s.rect(cx - 4 * k, by - 4 * k, 8 * k, 13 * k, r=2 * k,
           fill=lin(cx - 4 * k, 0, cx + 4 * k, 0, [(0, lite(TRUNK, .25)), (1, shade(TRUNK, .28))]))
    tiers = [
        (by - 40 * k, by, 27 * k, mix(col, INK, .12)),
        (by - 58 * k, by - 24 * k, 22 * k, col),
        (by - 76 * k, by - 46 * k, 16 * k, mix(col, WHITE, .12)),
    ]
    for top, bot, half, c in tiers:
        d = "M %g %g L %g %g Q %g %g %g %g Z" % (cx, top, cx + half, bot, cx, bot + 5 * k, cx - half, bot)
        s.path(d, fill=lin(cx - half, top, cx + half * .8, bot, [(0, lite(c, .30)), (1, shade(c, .24))]),
               stroke=edge(c), sw=1.3, join=JOIN_ROUND)
    for top, bot, half, c in tiers[:2]:
        s.line(cx - half * .18, top + (bot - top) * .22, cx - half * .62, bot - 3 * k, alpha(WHITE, .32), 1.7 * k)
    if snow:
        for top, bot, half, c in tiers:
            frac = .55
            w = half * frac
            yb = top + (bot - top) * frac
            pts = [(cx, top), (cx + w, yb), (cx + w * .5, yb - 2.2 * k), (cx + w * .05, yb + 1.8 * k),
                   (cx - w * .42, yb - 2.2 * k), (cx - w, yb)]
            s.poly(pts, fill=lin(cx - w, top, cx + w, yb, [(0, WHITE), (1, "#D5E3F6")]),
                   stroke="#E4EEFA", sw=1.8, join=JOIN_ROUND)


def pine_a():
    s = Scene()
    ground_shadow(s, 50, 90, 27, 4)
    pine(s, 50, 82, .95)
    return s.bake()


def pine_b():
    s = Scene()
    ground_shadow(s, 52, 90, 38, 4)
    pine(s, 70, 80, .62, col="#69B07C")
    pine(s, 37, 84, .9)
    tuft(s, 62, 89, 1.0)
    tuft(s, 84, 88, .8, "#7DB878")
    return s.bake()


def stump(s, cx, by):
    wood = "#B07E54"
    s.path("M %g %g L %g %g Q %g %g %g %g L %g %g Z" % (cx - 11, by - 14, cx - 12, by, cx, by + 5, cx + 12, by, cx + 11, by - 14),
           fill=lin(cx - 12, 0, cx + 12, 0, [(0, lite(wood, .25)), (1, shade(wood, .3))]),
           stroke=edge(wood), sw=1.2, join=JOIN_ROUND)
    s.ellipse(cx, by - 14, 11, 4.6, fill=lin(cx - 11, by - 18, cx + 11, by - 10, [(0, "#F1D4A6"), (1, "#D9A872")]),
              stroke=edge(wood), sw=1.2)
    s.ellipse(cx, by - 14, 6.5, 2.6, stroke=alpha("#9A6A44", .7), sw=1.0)
    s.ellipse(cx, by - 14, 2.4, 1.0, stroke=alpha("#9A6A44", .7), sw=0.9)


def pine_c():
    s = Scene()
    ground_shadow(s, 50, 90, 38, 4)
    stump(s, 76, 84)
    pine(s, 38, 84, .88, col="#4A8F76", snow=True)
    s.ellipse(76, 69, 9, 2.8, fill=lin(66, 66, 86, 72, [(0, WHITE), (1, "#D5E3F6")]))
    s.ellipse(58, 89, 10, 2.4, fill=alpha(WHITE, .85))
    return s.bake()


# ------------------------------------------------------------------ RIO

WATER_L = "#98DAEE"
WATER_D = "#4F97C8"
BANK = "#A5D08A"


def river_a():
    s = Scene()
    pond = [(8, 70), (18, 57), (38, 50), (62, 51), (82, 58), (93, 72), (82, 85), (58, 90), (30, 88), (14, 81)]
    s.blob(pond, fill=lin(10, 50, 90, 90, [(0, lite(BANK, .25)), (1, shade(BANK, .28))]), stroke=edge(BANK), sw=1.2)
    inner = [(14, 70), (23, 60), (40, 55), (62, 56), (78, 61), (87, 72), (78, 82), (57, 85), (32, 83), (19, 77)]
    s.blob(inner, fill=lin(0, 55, 0, 85, [(0, "#5BA5D2"), (1, WATER_L)]))
    s.curve([(26, 68), (35, 65), (46, 67)], stroke=alpha(WHITE, .6), sw=1.6)
    s.curve([(56, 79), (66, 76), (78, 78)], stroke=alpha(WHITE, .5), sw=1.5)
    # nenufares
    pad = lin(50, 62, 74, 74, [(0, "#9ADBB2"), (1, "#4E9E78")])
    s.path("M 61 69 L 70.5 67 C 70 64 66 62.5 62 63 C 56 63.5 53 66 53 69 C 53 72 57 74 62 74 C 66 74 70 72.5 71 70 Z", fill=pad)
    s.line(56, 69, 60, 68.5, alpha(WHITE, .5), 1.1)
    s.ellipse(36, 76, 6, 2.9, fill=pad)
    s.circle(61, 66.5, 3.8, fill=lin(58, 63, 64, 70, [(0, "#FFC0D2"), (1, "#E5576B")]))
    s.circle(61, 66.5, 1.4, fill="#F5C34A")
    tuft(s, 12, 74, 1.1)
    tuft(s, 90, 74, 1.0, "#7DB878")
    return s.bake()


def river_b():
    s = Scene()
    pts = [(88, 46), (72, 53), (57, 58), (43, 66), (31, 75), (21, 82)]
    s.blob(ribbon(pts, [9, 12, 16, 21, 26, 29]), fill=lin(20, 46, 90, 88, [(0, lite(BANK, .25)), (1, shade(BANK, .28))]))
    s.blob(ribbon(pts, [5, 8, 12, 16, 21, 23]), fill=lin(88, 46, 20, 84, [(0, "#5BA5D2"), (1, WATER_L)]))
    s.curve([(78, 51), (66, 56), (54, 61)], stroke=alpha(WHITE, .55), sw=1.5)
    s.curve([(44, 70), (36, 75), (30, 80)], stroke=alpha(WHITE, .55), sw=1.5)
    # piedras
    s.blob([(66, 62), (72, 59), (79, 62), (78, 67), (70, 68)],
           fill=lin(66, 58, 80, 68, [(0, "#DAD5E6"), (1, "#8E86A8")]), stroke=edge("#A9A3BF"), sw=1.1)
    s.blob([(40, 62), (44, 59), (49, 61), (48, 65), (42, 66)],
           fill=lin(40, 58, 50, 66, [(0, "#DAD5E6"), (1, "#8E86A8")]), stroke=edge("#A9A3BF"), sw=1.1)
    # juncos
    for dx, tip, hx, hy in ((0, -4, 74, 54), (7, 5, 83, 57)):
        bx = 74 + dx
        s.curve([(bx, 90), (bx + tip * .3, 74), (bx + tip, 60)], stroke="#5E9E68", sw=1.8)
        with s.rotate(tip * 2, bx + tip, 58):
            s.ellipse(bx + tip, 56, 2.5, 6, fill=lin(bx, 50, bx + 4, 62, [(0, "#B87A4E"), (1, "#7A4A2E")]))
    tuft(s, 66, 90, 1.0)
    tuft(s, 12, 62, .9, "#7DB878")
    return s.bake()


def river_c():
    s = Scene()
    ground_shadow(s, 50, 91, 40, 3.2)
    water = [(5, 82), (15, 73), (40, 70), (62, 72), (86, 72), (95, 81), (82, 89), (56, 91), (28, 90), (12, 88)]
    s.blob(water, fill=lin(0, 70, 0, 91, [(0, "#5BA5D2"), (1, WATER_L)]))
    s.curve([(28, 85), (38, 82), (48, 84)], stroke=alpha(WHITE, .55), sw=1.5)
    s.curve([(60, 87), (70, 84), (80, 86)], stroke=alpha(WHITE, .45), sw=1.4)
    # orillas
    s.ellipse(14, 78, 14, 8, fill=lin(0, 70, 0, 86, [(0, lite(BANK, .25)), (1, shade(BANK, .2))]))
    s.ellipse(86, 78, 14, 8, fill=lin(0, 70, 0, 86, [(0, lite(BANK, .25)), (1, shade(BANK, .2))]))
    wood = "#B27A4C"

    def yd(x):
        return 52 + 24 * ((x - 50) / 38.0) ** 2

    xs = [12, 22, 31, 41, 50, 59, 69, 78, 88]
    s.curve([(x, yd(x) + 3) for x in xs], stroke=shade("#8A5A3B", .2), sw=11)
    s.curve([(x, yd(x)) for x in xs], stroke=lin(12, 50, 88, 76, [(0, lite(wood, .35)), (1, shade(wood, .15))]), sw=7)
    for x in (22, 36, 50, 64, 78):
        s.line(x, yd(x) - 13, x, yd(x) - 2, shade("#8A5A3B", .1), 2.4)
    s.curve([(x, yd(x) - 13) for x in xs], stroke=lin(12, 40, 88, 62, [(0, lite(wood, .3)), (1, shade(wood, .25))]), sw=3)
    s.curve([(26, yd(26) - 1), (38, yd(38) - 1), (50, yd(50) - 1)], stroke=alpha(WHITE, .35), sw=1.4)
    return s.bake()


# ------------------------------------------------------------------ COLINAS

def hill(s, x, y, w, h, fill, stroke=None, sw=0):
    s.path("M %g %g C %g %g %g %g %g %g Q %g %g %g %g Z" % (
        x - w / 2, y, x - w * .32, y - h * 1.3, x + w * .32, y - h * 1.3, x + w / 2, y, x, y + 6, x - w / 2, y),
        fill=fill, stroke=stroke, sw=sw, join=JOIN_ROUND)


def bush_row(s, pts, r, col="#3F8A5E"):
    cmds = []
    for (x, y) in pts:
        cmds += circ(x, y, r)
    s.path(cmds, fill=lin(pts[0][0], pts[0][1] - r, pts[-1][0], pts[-1][1] + r, [(0, lite(col, .3)), (1, shade(col, .15))]),
           stroke=alpha(shade(col, .4), .7), sw=.8)


def hills_a():
    s = Scene()
    hill(s, 68, 86, 52, 38, lin(60, 48, 70, 90, [(0, "#C8E6B2"), (1, "#A4D096")]))
    hill(s, 36, 88, 60, 38, lin(20, 52, 50, 92, [(0, "#A8D79A"), (1, "#7BBE8C")]))
    hill(s, 58, 91, 66, 26, lin(40, 66, 80, 96, [(0, "#82C28C"), (1, "#559E72")]))
    s.curve([(18, 66), (26, 60), (34, 58)], stroke=alpha(WHITE, .45), sw=2.0)
    s.curve([(62, 56), (68, 52), (74, 52)], stroke=alpha(WHITE, .4), sw=1.8)
    bush_row(s, [(42, 80), (49, 76), (57, 74.5), (65, 76), (72, 80)], 3.2)
    bush_row(s, [(36, 87), (45, 82.5), (55, 80.5), (66, 82), (76, 86.5)], 3.2)
    return s.bake()


def hills_b():
    s = Scene()
    hill(s, 50, 90, 84, 58, lin(20, 34, 70, 92, [(0, "#B5E0A0"), (1, "#7FBE8A")]))
    hill(s, 74, 91, 44, 24, lin(60, 68, 90, 94, [(0, "#82C28C"), (1, "#559E72")]))
    s.curve([(26, 88), (38, 80), (54, 74), (47, 63), (51, 53)], stroke="#F1DDA6", sw=3.4)
    s.curve([(26, 88), (38, 80), (54, 74), (47, 63), (51, 53)], stroke=alpha("#D9B56E", .45), sw=1.0)
    # arbol en la cima
    s.line(50, 43, 50, 30, "#8A5A3B", 3)
    s.circle(50, 24, 9.5, fill=rad(46, 20, 13, [(0, "#B9E49E"), (1, "#4F9A66")]), stroke=edge("#4F9A66"), sw=1.2)
    glint(s, 46, 20, 3.4, 1.9, -35, .55)
    s.curve([(20, 56), (28, 49), (36, 44)], stroke=alpha(WHITE, .5), sw=2.0)
    bush_row(s, [(66, 84), (72, 81), (79, 80.5), (86, 83)], 2.8)
    return s.bake()


def hills_c():
    s = Scene()
    hill(s, 34, 90, 66, 44, lin(14, 48, 50, 92, [(0, "#B5E0A0"), (1, "#7FBE8A")]))
    hill(s, 72, 91, 54, 32, lin(52, 60, 92, 96, [(0, "#86C690"), (1, "#559E72")]))
    # molino
    s.path("M 27 51 L 29 36 L 35 36 L 37 51 Z", fill=lin(27, 0, 37, 0, [(0, "#FFF6E6"), (1, "#E6D3B8")]), stroke=edge("#D8C3A4"), sw=1.1, join=JOIN_ROUND)
    s.tri((26.5, 36.5), (37.5, 36.5), (32, 29), fill=lin(26, 29, 38, 37, [(0, "#EE8D6E"), (1, "#C9573F")]), stroke=edge("#C9573F"), sw=1.1, join=JOIN_ROUND)
    for a in (22, 112, 202, 292):
        with s.rotate(a, 32, 35):
            s.rect(30.4, 18, 3.2, 17, r=1.2, fill=lin(30, 0, 34, 0, [(0, "#E6C79A"), (1, "#A9794E")]))
    s.circle(32, 35, 2.2, fill="#8A5A3B")
    s.curve([(14, 66), (20, 60), (26, 57)], stroke=alpha(WHITE, .5), sw=2.0)
    # flores
    for (x, y, c) in ((58, 76, "#E5576B"), (70, 71, "#F5C34A"), (79, 78, WHITE), (66, 83, "#F5C34A"), (85, 72, "#E5576B"), (74, 85, WHITE)):
        s.circle(x, y, 2.3, fill=c, stroke=alpha(INK, .25), sw=.6)
    return s.bake()


# ------------------------------------------------------------------ NUBES

CLOUD_EDGE = "#C9D4EE"


def cloud_cmds(cx, by, w):
    k = w / 64.0
    return (_rrect_cmds(cx - 32 * k, by - 22 * k, 64 * k, 22 * k, 11 * k)
            + _ellipse_cmds(cx - 14 * k, by - 24 * k, 13 * k, 13 * k)
            + _ellipse_cmds(cx + 4 * k, by - 31 * k, 17 * k, 17 * k)
            + _ellipse_cmds(cx + 22 * k, by - 22 * k, 11.5 * k, 11.5 * k))


def cloud(s, cx, by, w, tint=None):
    k = w / 64.0
    cmds = cloud_cmds(cx, by, w)
    s.path(cmds, fill=CLOUD_EDGE, stroke=CLOUD_EDGE, sw=2.4, join=JOIN_ROUND)
    s.path(cmds, fill=lin(0, by - 48 * k, 0, by, [(0, WHITE), (.55, "#F2F6FF"), (1, "#D1DCF4")]))
    glint(s, cx - 4 * k, by - 40 * k, 6 * k, 2.6 * k, -35, .85)
    s.curve([(cx - 24 * k, by - 7 * k), (cx - 12 * k, by - 4 * k), (cx, by - 5 * k)], stroke=alpha("#9FB0DC", .35), sw=1.6 * k)


def cloud_a():
    s = Scene()
    s.ellipse(50, 88, 27, 3.6, fill="#0000001A")
    cloud(s, 50, 70, 74)
    return s.bake()


def cloud_b():
    s = Scene()
    s.ellipse(52, 88, 32, 3.6, fill="#0000001A")
    cloud(s, 62, 58, 56)
    cloud(s, 36, 78, 52)
    return s.bake()


def cloud_c():
    s = Scene()
    s.ellipse(50, 88, 28, 3.6, fill="#0000001A")
    sky_glow(s, 66, 36, 30, "#FFD27A", .5)
    for i in range(8):
        a = math.radians(i * 45 + 22)
        s.line(66 + 19 * math.cos(a), 36 + 19 * math.sin(a), 66 + 25 * math.cos(a), 36 + 25 * math.sin(a), alpha("#F2B84A", .9), 2.6)
    s.circle(66, 36, 14, fill=rad(62, 32, 19, [(0, "#FFF0B0"), (1, "#F2B84A")]))
    glint(s, 61, 31, 4, 2, -35, .7)
    cloud(s, 44, 74, 66)
    return s.bake()


# ------------------------------------------------------------------ LAVANDA

LAV = "#9D7FD4"
LEAF = "#6FAE74"


def spike(s, x0, y0, x1, y1, n=5, r0=3.4, r1=1.9, col=LAV):
    cmds = []
    for i in range(n):
        t = i / float(n - 1)
        cmds += _ellipse_cmds(x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, (r0 + (r1 - r0) * t) * .9, (r0 + (r1 - r0) * t) * 1.15)
    xs = [x0, x1]
    s.path(cmds, fill=lin(min(xs) - 4, min(y0, y1), max(xs) + 4, max(y0, y1), [(0, lite(col, .4)), (1, shade(col, .22))]),
           stroke=alpha(edge(col), .7), sw=.8)


def stem(s, pts, col=LEAF, sw=1.8):
    s.curve(pts, stroke=col, sw=sw)


def leaves(s, cx, by, k=1.0):
    d = ("M %g %g Q %g %g %g %g Q %g %g %g %g Z "
         "M %g %g Q %g %g %g %g Q %g %g %g %g Z "
         "M %g %g Q %g %g %g %g Q %g %g %g %g Z "
         "M %g %g Q %g %g %g %g Q %g %g %g %g Z") % (
        cx - 3 * k, by, cx - 11 * k, by - 4 * k, cx - 21 * k, by - 9 * k, cx - 12 * k, by + 1 * k, cx - 7 * k, by + 2 * k,
        cx - 2 * k, by, cx - 7 * k, by - 9 * k, cx - 12 * k, by - 18 * k, cx - 5 * k, by - 8 * k, cx - 1 * k, by,
        cx + 3 * k, by, cx + 11 * k, by - 4 * k, cx + 21 * k, by - 9 * k, cx + 12 * k, by + 1 * k, cx + 7 * k, by + 2 * k,
        cx + 2 * k, by, cx + 7 * k, by - 9 * k, cx + 12 * k, by - 18 * k, cx + 5 * k, by - 8 * k, cx + 1 * k, by)
    s.path(d, fill=lin(cx - 20 * k, by - 18 * k, cx + 20 * k, by + 2 * k, [(0, lite(LEAF, .3)), (1, shade(LEAF, .22))]))


def lavender_a():
    s = Scene()
    ground_shadow(s, 50, 90, 26, 3.8)
    tips = [(22, 34), (36, 22), (50, 16), (64, 24), (78, 36)]
    for (tx, ty) in tips:
        stem(s, [(50 + (tx - 50) * .1, 86), (50 + (tx - 50) * .55, 60), (tx, ty + 14)])
    for (tx, ty) in tips:
        spike(s, tx - (tx - 50) * .02, ty + 22, tx, ty, n=5)
    leaves(s, 50, 89, 1.1)
    return s.bake()


def lavender_b():
    s = Scene()
    ground_shadow(s, 50, 90, 38, 4)
    # arbusto de flor
    s.blob([(10, 80), (14, 64), (28, 52), (50, 46), (72, 52), (86, 64), (90, 80), (70, 88), (50, 90), (30, 88)],
           fill=lin(14, 46, 86, 90, [(0, lite("#6FAE74", .15)), (1, shade("#6FAE74", .3))]))
    dots = [(26, 62), (38, 56), (52, 52), (66, 56), (78, 63), (30, 72), (44, 66), (58, 66), (72, 72), (36, 80), (52, 77), (68, 81)]
    cmds = []
    for (x, y) in dots:
        cmds += circ(x, y, 6.2)
    s.path(cmds, fill=lin(14, 46, 86, 88, [(0, lite(LAV, .45)), (1, shade(LAV, .25))]), stroke=alpha(edge(LAV), .65), sw=.9)
    cmds = []
    for (x, y) in [(34, 60), (48, 57), (62, 60), (40, 70), (56, 71)]:
        cmds += circ(x, y, 2.2)
    s.path(cmds, fill=alpha(WHITE, .45))
    # mariposa
    with s.rotate(-18, 76, 36):
        s.ellipse(71, 33, 6, 4.2, fill=lin(66, 30, 77, 38, [(0, "#FFC0A0"), (1, "#E5576B")]))
        s.ellipse(81, 33, 6, 4.2, fill=lin(76, 30, 87, 38, [(0, "#FFC0A0"), (1, "#E5576B")]))
        s.ellipse(72, 39, 4, 3, fill=lin(68, 36, 76, 42, [(0, "#FFE0A0"), (1, "#F2A93B")]))
        s.ellipse(80, 39, 4, 3, fill=lin(76, 36, 84, 42, [(0, "#FFE0A0"), (1, "#F2A93B")]))
        s.line(76, 30, 76, 42, "#5C3A2E", 1.8)
    s.line(76, 30, 73, 25, "#5C3A2E", 1.0)
    s.line(76, 30, 79, 25, "#5C3A2E", 1.0)
    return s.bake()


def lavender_c():
    s = Scene()
    ground_shadow(s, 50, 90, 24, 3.8)
    tips = [(30, 30), (42, 18), (56, 15), (68, 26)]
    for (tx, ty) in tips:
        stem(s, [(50 + (tx - 50) * .15, 62), (50 + (tx - 50) * .6, 46), (tx, ty + 14)])
    for (tx, ty) in tips:
        spike(s, tx, ty + 20, tx, ty, n=5)
    leaves(s, 50, 63, .9)
    clay = "#E08A62"
    s.path("M 31 64 L 69 64 L 63 89 Q 50 93 37 89 Z",
           fill=lin(31, 64, 69, 90, [(0, lite(clay, .35)), (1, shade(clay, .22))]), stroke=edge(clay), sw=1.3, join=JOIN_ROUND)
    s.rect(28, 58, 44, 9, r=3.5, fill=lin(28, 58, 72, 67, [(0, lite(clay, .45)), (1, shade(clay, .1))]), stroke=edge(clay), sw=1.3)
    s.line(38, 72, 40, 84, alpha(WHITE, .4), 2.2)
    s.line(36, 62, 52, 62, alpha(WHITE, .45), 1.6)
    return s.bake()


# ------------------------------------------------------------------ FAROLILLOS

ORANGE = "#F2893C"
CAPC = "#A5502A"


def paper_lantern(s, cx, cy, rx=12, ry=14, col=ORANGE, glow=True, tassel=True):
    if glow:
        sky_glow(s, cx, cy, rx * 2.3, "#FFD27A", .55)
    s.ellipse(cx, cy, rx, ry, fill=rad(cx - rx * .3, cy - ry * .35, rx * 1.9, [(0, lite(col, .55)), (.55, col), (1, shade(col, .28))]),
              stroke=edge(col), sw=1.2)
    s.curve([(cx - rx * .55, cy - ry * .88), (cx - rx * .64, cy), (cx - rx * .55, cy + ry * .88)], stroke=alpha(shade(col, .45), .55), sw=1.1)
    s.curve([(cx + rx * .55, cy - ry * .88), (cx + rx * .64, cy), (cx + rx * .55, cy + ry * .88)], stroke=alpha(shade(col, .45), .55), sw=1.1)
    s.line(cx, cy - ry * .98, cx, cy + ry * .98, alpha(shade(col, .45), .5), 1.0)
    s.rect(cx - rx * .55, cy - ry - 2.2, rx * 1.1, 4.4, r=1.8, fill=lin(cx - rx, 0, cx + rx, 0, [(0, lite(CAPC, .3)), (1, shade(CAPC, .2))]))
    s.rect(cx - rx * .55, cy + ry - 2.2, rx * 1.1, 4.4, r=1.8, fill=lin(cx - rx, 0, cx + rx, 0, [(0, lite(CAPC, .3)), (1, shade(CAPC, .2))]))
    if tassel:
        s.line(cx, cy + ry + 2, cx, cy + ry + 7, "#F2C14E", 1.4)
        s.ellipse(cx, cy + ry + 9, 1.8, 3, fill="#F2C14E")
    glint(s, cx - rx * .38, cy - ry * .35, rx * .22, ry * .4, -10, .6)


def lantern_a():
    s = Scene()
    ground_shadow(s, 50, 90, 26, 3.8)
    wood = "#8A5A3B"
    s.rect(24, 84, 22, 6, r=2.5, fill=lin(24, 84, 46, 90, [(0, "#CFC8DC"), (1, "#8E86A8")]), stroke=edge("#A9A3BF"), sw=1.1)
    s.rect(32, 16, 6, 70, r=2.5, fill=lin(32, 0, 38, 0, [(0, lite(wood, .3)), (1, shade(wood, .28))]))
    s.path("M 35 18 L 63 18", stroke=lin(35, 0, 63, 0, [(0, lite(wood, .2)), (1, shade(wood, .25))]), sw=4.4, cap=CAP_ROUND)
    s.path("M 35 30 Q 42 28 47 19", stroke=shade(wood, .25), sw=2.6, cap=CAP_ROUND)
    s.line(62, 18, 62, 29, "#5C3A2E", 1.3)
    paper_lantern(s, 62, 47, 14, 16)
    s.circle(35, 14, 3.2, fill=lin(32, 11, 38, 17, [(0, lite(wood, .35)), (1, shade(wood, .2))]))
    return s.bake()


def lantern_b():
    s = Scene()
    ground_shadow(s, 50, 90, 28, 4)
    stone = "#C2BBD4"
    g = lambda x1, y1, x2, y2: lin(x1, y1, x2, y2, [(0, lite(stone, .45)), (1, shade(stone, .28))])
    ol = edge("#A9A3BF")
    sky_glow(s, 50, 46, 30, "#FFD27A", .5)
    s.rect(30, 82, 40, 8, r=3, fill=g(30, 82, 70, 90), stroke=ol, sw=1.1)
    s.rect(43, 58, 14, 26, r=2.5, fill=g(43, 58, 57, 84), stroke=ol, sw=1.1)
    s.rect(32, 52, 36, 8, r=3, fill=g(32, 52, 68, 60), stroke=ol, sw=1.1)
    s.rect(37, 36, 26, 18, r=3, fill=g(37, 36, 63, 54), stroke=ol, sw=1.1)
    s.rect(42, 40, 16, 11, r=2.5, fill=lin(42, 40, 58, 51, [(0, "#FFF0B0"), (1, "#F2A93B")]))
    s.line(50, 40, 50, 51, alpha("#8A5A2B", .5), 1.1)
    s.path("M 20 38 C 32 38 42 30 50 18 C 58 30 68 38 80 38 C 66 42 34 42 20 38 Z",
           fill=lin(20, 18, 80, 42, [(0, lite("#9E96B6", .4)), (1, shade("#9E96B6", .3))]), stroke=ol, sw=1.2, join=JOIN_ROUND)
    s.circle(50, 15.5, 3.6, fill=g(46, 12, 54, 20), stroke=ol, sw=1.1)
    s.curve([(30, 36), (38, 33), (44, 27)], stroke=alpha(WHITE, .5), sw=1.8)
    return s.bake()


def lantern_c():
    s = Scene()
    ground_shadow(s, 50, 91, 40, 3.4)
    wood = "#8A5A3B"
    for x in (12, 88):
        s.rect(x - 3, 38, 6, 53, r=2.5, fill=lin(x - 3, 0, x + 3, 0, [(0, lite(wood, .3)), (1, shade(wood, .28))]))
        s.circle(x, 37, 3.4, fill=lin(x - 3, 34, x + 3, 40, [(0, lite(wood, .35)), (1, shade(wood, .2))]))
    s.curve([(12, 38), (31, 49), (50, 53), (69, 49), (88, 38)], stroke="#5C3A2E", sw=1.4)
    paper_lantern(s, 31, 61, 9.5, 11, col="#E8664D", glow=True, tassel=False)
    paper_lantern(s, 50, 66, 11, 13, col="#F2B84A", glow=True, tassel=True)
    paper_lantern(s, 69, 61, 9.5, 11, col="#E8664D", glow=True, tassel=False)
    return s.bake()


# ------------------------------------------------------------------ MONTANAS

def snow_cap(s, pts, fill=None):
    s.poly(pts, fill=fill or lin(0, 0, 0, 40, [(0, WHITE), (1, "#D9E4F7")]), stroke="#EAF1FB", sw=1.4, join=JOIN_ROUND)


def mountains_a():
    s = Scene()
    ground_shadow(s, 50, 90, 42, 3.4)
    s.path("M 6 88 C 18 70 32 42 50 14 C 66 40 82 68 94 88 C 70 94 30 94 6 88 Z",
           fill=lin(14, 20, 80, 90, [(0, "#B3BCE8"), (1, "#7C88C4")]))
    s.path("M 50 14 C 66 40 82 68 94 88 C 82 91 68 92.5 56 92 C 60 70 57 40 50 14 Z",
           fill=lin(50, 14, 90, 92, [(0, "#8590C9"), (1, "#5A66A6")]))
    snow_cap(s, [(50, 14), (64.5, 39), (58, 36.5), (54, 43), (49, 36), (44, 43), (40, 36.5), (35, 39)],
             lin(36, 14, 64, 42, [(0, WHITE), (.6, "#EEF3FC"), (1, "#C9D6F0")]))
    s.curve([(44, 28), (36, 46), (26, 66)], stroke=alpha(WHITE, .38), sw=2.2)
    s.path("M 4 90 C 14 76 34 76 48 90 C 30 95 14 95 4 90 Z",
           fill=lin(4, 76, 40, 94, [(0, "#9CCB8A"), (1, "#5E9E72")]))
    return s.bake()


def mountains_b():
    s = Scene()
    ground_shadow(s, 50, 90, 42, 3.4)
    s.path("M 3 88 C 12 68 26 42 40 15 C 56 40 70 66 78 88 Z",
           fill=lin(8, 20, 70, 90, [(0, "#B3BCE8"), (1, "#7C88C4")]))
    s.path("M 40 15 C 56 40 70 66 78 88 L 50 90 C 52 66 48 40 40 15 Z",
           fill=lin(40, 15, 76, 90, [(0, "#8590C9"), (1, "#5A66A6")]))
    snow_cap(s, [(40, 15), (53.5, 38), (47, 36), (43, 42), (38, 35.5), (33.5, 41), (28.5, 38)],
             lin(28, 15, 54, 42, [(0, WHITE), (.6, "#EEF3FC"), (1, "#C9D6F0")]))
    s.path("M 46 90 C 52 76 62 56 74 42 C 82 58 90 74 97 89 C 84 93 62 93 46 90 Z",
           fill=lin(52, 42, 96, 92, [(0, "#C3AEE0"), (1, "#8C76BE")]))
    s.path("M 74 42 C 82 58 90 74 97 89 C 90 91 82 92 74 92 C 78 74 78 56 74 42 Z",
           fill=lin(74, 42, 96, 92, [(0, "#9E88CE"), (1, "#6E5AA8")]))
    snow_cap(s, [(74, 42), (80, 52), (76.5, 51), (74.5, 55), (72, 51), (69, 54.5), (67.5, 52)],
             lin(66, 42, 80, 56, [(0, WHITE), (1, "#D9E4F7")]))
    s.curve([(36, 30), (28, 48), (20, 66)], stroke=alpha(WHITE, .38), sw=2.2)
    return s.bake()


def mountains_c():
    s = Scene()
    ground_shadow(s, 50, 90, 42, 3.4)
    # cadena lejana
    s.path("M 4 86 C 14 70 22 52 30 40 C 40 54 46 64 52 74 C 62 58 70 48 76 42 C 84 56 90 70 96 86 C 70 92 30 92 4 86 Z",
           fill=lin(4, 40, 96, 90, [(0, "#C9D0F0"), (1, "#9AA5D8")]))
    snow_cap(s, [(30, 40), (36, 49), (32.5, 47.5), (30, 52), (27.5, 47.5), (24, 49.5)], lin(24, 40, 36, 52, [(0, WHITE), (1, "#D9E4F7")]))
    snow_cap(s, [(76, 42), (82, 52), (79, 50.5), (76, 55), (73, 50.5), (70, 52)], lin(70, 42, 82, 55, [(0, WHITE), (1, "#D9E4F7")]))
    # pico central
    s.path("M 22 90 C 32 66 44 42 54 18 C 66 42 78 68 88 90 C 66 95 44 95 22 90 Z",
           fill=lin(26, 20, 82, 90, [(0, "#AEB8E6"), (1, "#6F7BBA")]))
    s.path("M 54 18 C 66 42 78 68 88 90 C 76 93 64 93.5 54 93 C 58 70 58 44 54 18 Z",
           fill=lin(54, 18, 86, 93, [(0, "#7F8AC5"), (1, "#566299")]))
    snow_cap(s, [(54, 18), (66, 40), (60, 38), (56.5, 44), (52, 37.5), (47.5, 43.5), (43, 38.5), (41.5, 39.5)],
             lin(42, 18, 66, 44, [(0, WHITE), (.6, "#EEF3FC"), (1, "#C9D6F0")]))
    # neblina
    s.path(_rrect_cmds(8, 66, 84, 11, 5.5), fill=alpha(WHITE, .6))
    s.path(_rrect_cmds(20, 76, 66, 8, 4), fill=alpha(WHITE, .5))
    # pinitos
    for (x, k) in ((14, .5), (24, .38)):
        pine(s, x, 90, k, col="#5AA27A")
    s.curve([(46, 32), (40, 50), (34, 66)], stroke=alpha(WHITE, .35), sw=2.0)
    return s.bake()


# ------------------------------------------------------------------ paquete

def build():
    icons = {}
    makers = {
        "dunes": (dune_a, dune_b, dune_c),
        "pines": (pine_a, pine_b, pine_c),
        "river": (river_a, river_b, river_c),
        "hills": (hills_a, hills_b, hills_c),
        "clouds": (cloud_a, cloud_b, cloud_c),
        "lavender": (lavender_a, lavender_b, lavender_c),
        "lanterns": (lantern_a, lantern_b, lantern_c),
        "mountains": (mountains_a, mountains_b, mountains_c),
    }
    for kind, fns in makers.items():
        for suffix, fn in zip("abc", fns):
            icons["prop.%s_%s" % (kind, suffix)] = fn()
    return {"icons": icons}
