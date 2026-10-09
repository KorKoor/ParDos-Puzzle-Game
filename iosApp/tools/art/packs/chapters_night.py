"""Paquete chapters_night: emblemas de los 12 capitulos de Noche de brujas y monumentos nocturnos del mapa.

Ids: chapter.n0 .. chapter.n11 (100x100, medallon nocturno) y landmark.<tipo> (160x120, base en y~112).
"""
import os, sys, math
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, ".."))   # artlib
sys.path.insert(0, HERE)                        # _looks, _kit
from artlib import *

WHITE = "#FFFFFF"
DEEP = "#1B1030"
OUTL = "#2B1B3A"
GOLD = "#FFE08A"
AMBER = "#FFB347"
R_IN = 38.6


# ------------------------------------------------------------------ utilidades de color

def hexa(c, a):
    """'#RRGGBB' + alfa 0..1 -> '#RRGGBBAA'."""
    return c + "%02X" % int(round(max(0.0, min(1.0, a)) * 255))


def lt(c, t=.35):
    return mix(c, WHITE, t)


def dk(c, t=.3):
    return mix(c, DEEP, t)


def edge(c, t=.45):
    return mix(c, OUTL, t)


def gfill(c, x1, y1, x2, y2, top=.35, bot=.22):
    return lin(x1, y1, x2, y2, [(0, lt(c, top)), (1, dk(c, bot))])


def glow(s, cx, cy, r, c, a=.5):
    return s.circle(cx, cy, r, fill=rad(cx, cy, r, [(0, hexa(c, a)), (.5, hexa(c, a * .34)), (1, hexa(c, 0))]))


# ------------------------------------------------------------------ formas reutilizables

def crescent(s, cx, cy, R, fill, k=.8, dx=.45, dy=-.25, stroke=None, sw=0.0):
    """Luna creciente: circulo de radio R menos otro desplazado."""
    r2 = R * k
    ix, iy = cx + dx * R, cy + dy * R
    d = math.hypot(ix - cx, iy - cy)
    a = (R * R - r2 * r2 + d * d) / (2 * d)
    h = math.sqrt(max(0.0, R * R - a * a))
    ux, uy = (ix - cx) / d, (iy - cy) / d
    mx, my = cx + a * ux, cy + a * uy
    p1 = (mx - uy * h, my + ux * h)
    p2 = (mx + uy * h, my - ux * h)
    p = Path().M(*p1)
    p.A(R, R, 0, 1 if a > 0 else 0, 1, *p2)
    p.A(r2, r2, 0, 1 if (a - d) > 0 else 0, 0, *p1)
    p.Z()
    return s.path(p, fill=fill, stroke=stroke, sw=sw, join=JOIN_ROUND)


def bat(s, cx, cy, w, fill="#1E1038", rim=None, flap=0.0):
    """Murcielago de frente con las alas abiertas (w = envergadura)."""
    h = w / 2.0
    up = flap * .22
    pts = [(0, -.32), (.16, -.6), (.25, -.3), (.55, -.5 - up), (1, -.2 - up * 2), (.86, .14), (.64, .04),
           (.5, .42), (.28, .2), (.12, .5), (0, .42)]
    right = [(cx + x * h, cy + y * h) for x, y in pts]
    left = [(cx - x * h, cy + y * h) for x, y in reversed(pts[1:-1])]
    return s.poly(right + left, fill=fill, stroke=rim or fill, sw=max(.6, w * .05), join=JOIN_ROUND)


def sparkles(s, items, color="#FFFFFFC8"):
    for (x, y, r) in items:
        s.sparkle(x, y, r, fill=color)


def firefly(s, x, y, r, c="#C8FF7A", a=.8):
    glow(s, x, y, r * 3.4, c, a * .7)
    s.circle(x, y, r, fill=c)
    s.circle(x - r * .2, y - r * .2, r * .45, fill="#FFFFFFD0")


def ghost(s, cx, cy, w, h=None, glowc="#B9A2FF", ga=.45, fill=None, ink="#2A1450", look=0.0, alpha=1.0, gl=True):
    """Fantasma de frente con el borde de abajo ondulado."""
    h = h or w * 1.22
    x0, x1 = cx - w / 2.0, cx + w / 2.0
    top, bot = cy - h / 2.0, cy + h / 2.0
    if gl:
        glow(s, cx, cy, w * 1.0, glowc, ga)
    seg = w / 3.0
    p = Path().M(x0, bot).L(x0, top + w / 2.0).A(w / 2.0, w / 2.0, 0, 0, 1, x1, top + w / 2.0).L(x1, bot)
    p.Q(x1 - seg * .5, bot + h * .17, x1 - seg, bot - h * .04)
    p.Q(x1 - seg * 1.5, bot + h * .17, x1 - seg * 2, bot - h * .04)
    p.Q(x1 - seg * 2.5, bot + h * .17, x0, bot)
    p.Z()
    body = fill or lin(x0, top, x1, bot, [(0, "#FFFFFF"), (.6, "#E7E3FA"), (1, "#B9B2E6")])
    s.path(p, fill=body, stroke=hexa("#5B4C9A", .75), sw=max(.7, w * .045), join=JOIN_ROUND, op=alpha)
    ey = top + w * .52
    s.ellipse(cx - w * .18 + look, ey, w * .075, w * .105, fill=ink, op=alpha)
    s.ellipse(cx + w * .18 + look, ey, w * .075, w * .105, fill=ink, op=alpha)
    s.ellipse(cx + look * .6, ey + w * .24, w * .075, w * .1, fill=ink, op=alpha)
    s.ellipse(cx - w * .26, top + w * .28, w * .09, w * .15, fill="#FFFFFFB0", op=alpha)


def tomb(s, cx, top, w, bot, c_top="#D8D0F4", c_bot="#7E6CB6", sw=1.2):
    x0, x1 = cx - w / 2.0, cx + w / 2.0
    p = Path().M(x0, bot).L(x0, top + w / 2.0).A(w / 2.0, w / 2.0, 0, 0, 1, x1, top + w / 2.0).L(x1, bot).Z()
    s.path(p, fill=lin(x0, top, x1, bot, [(0, c_top), (1, c_bot)]), stroke=edge(c_bot, .55), sw=sw, join=JOIN_ROUND)
    s.line(x0 + w * .17, top + w * .6, x0 + w * .17, bot - w * .5, "#FFFFFF66", max(.8, w * .07))


def pumpkin_body(s, cx, cy, rx, ry, body="#E8772E", lit=True):
    """Calabaza con gajos: devuelve nada; la cara va aparte."""
    dark, mid, light = "#B84F14", "#E1651E", "#FF9A3E"
    # gajos exteriores
    for sgn in (-1, 1):
        s.ellipse(cx + sgn * rx * .56, cy + ry * .02, rx * .50, ry * .94,
                  fill=lin(cx - rx, cy - ry, cx + rx, cy + ry, [(0, mid if sgn < 0 else dark), (1, dark if sgn < 0 else "#8F3A0E")]))
    for sgn in (-1, 1):
        s.ellipse(cx + sgn * rx * .30, cy, rx * .50, ry,
                  fill=lin(cx - rx, cy - ry, cx + rx, cy + ry, [(0, light if sgn < 0 else mid), (1, mid if sgn < 0 else dark)]))
    s.ellipse(cx, cy, rx * .44, ry * 1.02, fill=lin(cx - rx * .4, cy - ry, cx + rx * .4, cy + ry, [(0, "#FFB25A"), (1, "#E1651E")]))
    # surcos
    for sgn in (-1, 1):
        s.curve([(cx + sgn * rx * .44, cy - ry * .86), (cx + sgn * rx * .5, cy), (cx + sgn * rx * .44, cy + ry * .86)],
                hexa("#7A2E08", .55), max(.8, rx * .05))


def pumpkin_face(s, cx, cy, r, c1="#FFF0A0", c2="#FFAE3A"):
    """Cara de calabaza encendida (r = radio horizontal de la calabaza)."""
    f = lin(cx, cy - r * .5, cx, cy + r * .7, [(0, c1), (1, c2)])
    s.tri((cx - r * .55, cy - r * .02), (cx - r * .17, cy - r * .02), (cx - r * .36, cy - r * .4), fill=f, join=JOIN_ROUND)
    s.tri((cx + r * .55, cy - r * .02), (cx + r * .17, cy - r * .02), (cx + r * .36, cy - r * .4), fill=f, join=JOIN_ROUND)
    s.poly([(cx - r * .6, cy + r * .2), (cx - r * .32, cy + r * .47), (cx - r * .12, cy + r * .27),
            (cx + r * .12, cy + r * .47), (cx + r * .32, cy + r * .27), (cx + r * .6, cy + r * .2),
            (cx + r * .36, cy + r * .66), (cx - r * .36, cy + r * .66)], fill=f, join=JOIN_ROUND)


def stem(s, x, y, w, h, lean=.25):
    p = Path().M(x - w / 2, y).C(x - w * .55, y - h * .5, x - w * .2 + lean * h, y - h * .8, x + lean * h - w * .35, y - h)
    p.L(x + lean * h + w * .45, y - h * .96).C(x + w * .3 + lean * h * .6, y - h * .5, x + w * .6, y - h * .2, x + w / 2, y).Z()
    s.path(p, fill=lin(x - w, y, x + w, y - h, [(0, "#8DB04A"), (1, "#3F6B2A")]), stroke=hexa("#2D4A1E", .8), sw=.9, join=JOIN_ROUND)


# ------------------------------------------------------------------ medallon

def circ_x(y, side, R=R_IN):
    return 50 + side * math.sqrt(max(0.0, R * R - (y - 50) ** 2))


def terrain(s, pts, fill, smooth=True, R=R_IN + .2, **kw):
    """Suelo recortado por el medallon. pts[0] y pts[-1] solo usan la y (la x sale del circulo)."""
    yl, yr = pts[0][1], pts[-1][1]
    xl, xr = circ_x(yl, -1, R), circ_x(yr, 1, R)
    seq = [(xl, yl)] + list(pts[1:-1]) + [(xr, yr)]
    cmds = catmull(seq) if smooth else [("M", seq[0][0], seq[0][1])] + [("L", x, y) for (x, y) in seq[1:]]
    p = Path()
    p.cmds = list(cmds)
    p._cx, p._cy = xr, yr
    p.A(R, R, 0, 1 if (yl + yr) / 2.0 < 50 else 0, 1, xl, yl)
    p.Z()
    return s.path(p, fill=fill, **kw)


def badge(accent, sky_top, sky_bot, gcol, gpos=(50, 46), gr=30, ga=.5):
    s = Scene(100, 100)
    s.ellipse(50, 94, 27, 3, fill="#00000026")
    s.circle(50, 50, 42, fill=lin(18, 10, 82, 92, [(0, lt(accent, .66)), (.5, accent), (1, mix(accent, DEEP, .62))]))
    s.circle(50, 50, 38.6, fill=lin(50, 12, 50, 88, [(0, sky_top), (1, sky_bot)]))
    glow(s, gpos[0], gpos[1], gr, gcol, ga)
    return s


def finish(s, accent):
    s.circle(50, 50, 38.6, stroke=mix(accent, DEEP, .72), sw=1.3)
    s.circle(50, 50, 42, stroke=hexa("#1B1030", .55), sw=.9)
    s.arc_stroke(50, 50, 40.3, 198, 262, "#FFFFFFA6", 1.5)
    s.arc_stroke(50, 50, 40.3, 20, 40, "#FFFFFF40", 1.2)
    return s.bake()


# ================================================================== EMBLEMAS

def n0():
    """Cementerio Sereno: lapidas bajo la luna."""
    A = "#7A56C0"
    s = badge(A, "#46338F", "#150B2C", "#B196FF", (46, 46), 31, .55)
    sparkles(s, [(24, 27, 2.8), (77, 47, 2.2), (35, 20, 1.8), (19, 47, 1.8)])
    glow(s, 69, 27, 17, "#E6D8FF", .4)
    crescent(s, 69, 27, 9.5, lin(60, 18, 78, 38, [(0, "#FFFBE6"), (1, "#D8C6FF")]))
    bat(s, 27, 37, 14, "#241347", rim="#6E58B8")
    terrain(s, [(0, 65), (30, 59), (62, 61), (0, 63)], vgrad("#5E4AA8", "#33256B", 58, 90))
    tomb(s, 66, 49, 19, 78, "#C5BAEC", "#5E4D98")
    s.line(60.5, 59, 71.5, 59, hexa("#2B1B5A", .5), 1.5)
    s.line(62, 63.5, 70, 63.5, hexa("#2B1B5A", .5), 1.5)
    tomb(s, 40, 33, 27, 79)
    s.rect(38.3, 41, 3.4, 18, 1.2, fill=hexa("#2B1B5A", .5))
    s.rect(32.5, 46, 15, 3.4, 1.2, fill=hexa("#2B1B5A", .5))
    terrain(s, [(0, 72), (28, 67), (60, 70), (0, 69)], vgrad("#3F2D78", "#150B2C", 66, 90))
    s.ellipse(50, 73, 34, 4.2, fill="#FFFFFF1C")
    for (x, y) in [(26, 72), (30, 71.5), (55, 73), (80, 69)]:
        s.curve([(x, y + 2), (x - .6, y - 1.5), (x - 2, y - 3.5)], "#8F7AD6", 1.1)
    return finish(s, A)


def n1():
    """Bosque Encantado: arbol retorcido con ojos que brillan, luciernagas y setas magicas."""
    A = "#2F8A7A"
    s = badge(A, "#276A7C", "#0B1A2A", "#6FF0C8", (60, 42), 30, .5)
    glow(s, 64, 33, 19, "#CFFFF0", .5)
    s.circle(64, 33, 11.5, fill=lin(55, 22, 74, 45, [(0, "#F6FFFB"), (1, "#B2EADB")]))
    s.circle(60.5, 36, 2.4, fill="#8FCFC0", op=.5)
    s.circle(67.5, 29.5, 1.7, fill="#8FCFC0", op=.5)
    tc = "#2C2058"
    # ramas
    s.path("M 38 50 Q 30 41 23 28 M 29 38 Q 21 36 14 27 M 25.5 31 Q 28 23 25 15 "
           "M 44 50 Q 42 36 46 21 M 44.5 36 Q 52 32 55 24 "
           "M 51 50 Q 61 43 70 30 M 63 40.5 Q 71 43 79 36 M 67 34 Q 67 27 71 20",
           stroke=tc, sw=4.2, cap=CAP_ROUND, join=JOIN_ROUND)
    s.path("M 29 38 Q 21 36 14 27 M 25.5 31 Q 28 23 25 15 M 44.5 36 Q 52 32 55 24 M 63 40.5 Q 71 43 79 36 M 67 34 Q 67 27 71 20",
           stroke=tc, sw=-1, cap=CAP_ROUND) if False else None
    p = Path().M(31, 89).C(40, 85, 41, 77, 40, 67).C(39, 60, 37, 54, 34, 47).L(55, 47).C(52, 54, 51, 60, 51, 67).C(51, 77, 53, 85, 62, 89).Z()
    s.path(p, fill=lin(34, 47, 60, 89, [(0, "#4A3B86"), (1, "#1A1033")]), stroke=hexa("#8E7DD0", .55), sw=.9, join=JOIN_ROUND)
    s.curve([(37, 50), (41, 62), (41, 74), (36, 84)], "#9A8AE0", 1.2, op=.45)
    for ex in (41.4, 50.2):
        glow(s, ex, 63, 6.5, "#FFC94D", .6)
    with s.rotate(-16, 41.4, 63):
        s.ellipse(41.4, 63, 3.4, 2.1, fill=rad(41.4, 63, 3.4, [(0, "#FFFBD0"), (1, "#FFC94D")]))
    with s.rotate(16, 50.2, 63):
        s.ellipse(50.2, 63, 3.4, 2.1, fill=rad(50.2, 63, 3.4, [(0, "#FFFBD0"), (1, "#FFC94D")]))
    terrain(s, [(0, 76), (28, 72), (60, 75), (0, 73)], vgrad("#2C7A74", "#0F2D33", 70, 90))
    for (x, y, c) in [(70, 76, 7), (78, 79, 5)]:
        glow(s, x, y - 2, c * 1.5, "#7CF5D0", .5)
        s.rect(x - c * .17, y - 1, c * .34, c * .55, 1, fill="#CDEFE6")
        s.path(Path().M(x - c * .52, y - 1).A(c * .52, c * .46, 0, 0, 1, x + c * .52, y - 1).Z(),
               fill=lin(x - c, y - c * .5, x + c, y, [(0, "#B8FFE8"), (1, "#2FB596")]), stroke=edge("#2FA58A", .5), sw=.8, join=JOIN_ROUND)
    for (x, y) in [(24, 56), (72, 55), (30, 71), (62, 50)]:
        firefly(s, x, y, 1.5)
    return finish(s, A)


def n2():
    """Pantano Brumoso: rana de ojos brillantes sobre un nenufar, fuego fatuo y juncos."""
    A = "#5E9C45"
    s = badge(A, "#2E5A4A", "#0C1A20", "#B7F26B", (62, 40), 28, .45)
    # fuego fatuo
    glow(s, 64, 33, 16, "#C8FF7A", .75)
    p = Path().M(64, 20).C(70, 28, 73, 33, 70.5, 39).C(69, 43, 66, 44, 64, 44).C(61, 44, 58, 43, 57.5, 39).C(56, 34, 60, 30, 64, 20).Z()
    s.path(p, fill=lin(64, 20, 64, 44, [(0, "#F4FFC8"), (.5, "#C8FF7A"), (1, "#7FD04A")]), stroke=hexa("#E9FFB0", .6), sw=.8, join=JOIN_ROUND)
    s.path(Path().M(64, 31).C(67, 35, 68, 38, 66.5, 41).C(65.5, 43, 62.5, 43, 61.5, 41).C(60.5, 38, 62, 36, 64, 31).Z(), fill="#FFFFFFB8")
    # agua
    terrain(s, [(0, 62), (30, 58), (62, 60), (0, 61)], vgrad("#2E7A5A", "#0E2D2A", 56, 90))
    for (x, y, w) in [(60, 68, 20), (30, 78, 16), (70, 80, 14)]:
        s.curve([(x - w / 2, y), (x - w / 4, y - 1.2), (x, y), (x + w / 4, y + 1.2), (x + w / 2, y)], "#8FE0B0", 1.1, op=.5)
    # juncos
    s.path("M 77 72 Q 77 60 74 47 M 83 72 Q 82 58 85 44 M 71 74 Q 70 62 66 54", stroke="#3F7F46", sw=1.8, cap=CAP_ROUND)
    for (x, y, rot) in [(74, 47, -10), (85, 44, 8)]:
        with s.rotate(rot, x, y + 4):
            s.rect(x - 2.2, y - 3, 4.4, 10, 2.2, fill=lin(x - 2, y, x + 2, y + 8, [(0, "#B87A4A"), (1, "#6B3E22")]), stroke=hexa("#3B2314", .7), sw=.7)
    s.path("M 74 72 Q 66 66 60 70 M 80 72 Q 90 64 92 60", stroke="#4E9A52", sw=1.6, cap=CAP_ROUND)
    # nenufar y rana
    s.ellipse(38, 70, 18.5, 6.4, fill=lin(20, 64, 58, 76, [(0, "#8FD27A"), (1, "#2F7A44")]), stroke=edge("#2F7A44", .5), sw=.9)
    s.path(Path().M(38, 70).L(52, 66.8).L(56.5, 70.5).Z(), fill=hexa("#0E2D2A", .5))
    s.ellipse(38, 64, 12, 9.4, fill=lin(28, 55, 48, 74, [(0, "#A8EE8F"), (1, "#3C8F4A")]), stroke=edge("#3C8F4A", .55), sw=1.1)
    s.ellipse(38, 68, 7.5, 4.4, fill="#E6FFC8", op=.55)
    s.ellipse(28.4, 68.8, 3.4, 2.2, fill="#74C766", stroke=edge("#3C8F4A", .55), sw=.8)
    s.ellipse(47.6, 68.8, 3.4, 2.2, fill="#74C766", stroke=edge("#3C8F4A", .55), sw=.8)
    for ex in (31.6, 44.4):
        s.circle(ex, 55.2, 5, fill=lin(ex - 4, 50, ex + 4, 60, [(0, "#A8EE8F"), (1, "#4C9F52")]), stroke=edge("#3C8F4A", .55), sw=1.1)
        glow(s, ex, 55.2, 6.5, "#FFE680", .5)
        s.circle(ex, 55.2, 3.5, fill=rad(ex, 55.2, 3.5, [(0, "#FFFBD0"), (1, "#FFC94D")]))
        s.ellipse(ex, 55.6, 1.0, 2.5, fill="#2A1450")
    s.curve([(31, 63), (35, 65.2), (41, 65.2), (45, 63)], "#1F4A2A", 1.2, op=.8)
    s.ellipse(33, 52.6, 1.5, .9, fill="#FFFFFFB0")
    s.ellipse(50, 72, 38, 3.4, fill="#FFFFFF20")
    s.ellipse(32, 83, 22, 2.6, fill="#FFFFFF18")
    return finish(s, A)


def n3():
    """Calabazar: calabaza encendida."""
    A = "#E8772E"
    s = badge(A, "#4B2680", "#170B2C", "#FFA03A", (50, 58), 33, .62)
    sparkles(s, [(24, 27, 2.6), (75, 24, 2.2), (79, 52, 1.6)], "#FFE9B8C8")
    bat(s, 70, 29, 12, "#1E1038", rim="#6E58B8")
    s.ellipse(50, 83, 29, 3.6, fill="#00000030")
    pumpkin_body(s, 50, 58, 29, 23)
    stem(s, 49.5, 36.5, 7.5, 10)
    s.path("M 56 36 C 62 33 64 38 61 40 C 59 41 58 39 59.5 38", stroke="#7FB04A", sw=1.4, cap=CAP_ROUND)
    s.path(Path().M(58, 37).C(64, 31, 72, 33, 72, 38).C(67, 41, 61, 41, 58, 37).Z(), fill=lin(58, 33, 72, 41, [(0, "#9CC85A"), (1, "#3F7A2E")]), stroke=hexa("#2D4A1E", .7), sw=.8)
    glow(s, 50, 61, 22, "#FFD86B", .5)
    pumpkin_face(s, 50, 58.5, 25)
    s.ellipse(33, 46, 6.5, 3.6, fill="#FFFFFF70", op=.9)
    s.ellipse(29, 52, 2, 1.2, fill="#FFFFFF66")
    return finish(s, A)


def n4():
    """Faro Fantasma: faro de rayas con su haz y un fantasma."""
    A = "#8E86C0"
    s = badge(A, "#413C86", "#14123A", "#B5AAF2", (50, 46), 30, .5)
    sparkles(s, [(24, 20, 2.0), (77, 25, 2.4), (33, 38, 1.4)])
    # haz
    s.poly([(53, 33), (10, 20), (10, 45)], fill=lin(53, 33, 12, 33, [(0, "#FFF6C0C0"), (1, "#FFF6C000")]))
    s.poly([(53, 33), (88, 24), (88, 41)], fill=lin(53, 33, 88, 33, [(0, "#FFF6C080"), (1, "#FFF6C000")]))
    # roca
    terrain(s, [(0, 78), (30, 72), (60, 74), (0, 78)], vgrad("#6A6EA0", "#2B2C60", 70, 90))
    # torre
    tw = lambda y, hw_top=6.5, hw_bot=12: 53 - 0  # noqa: E731
    def hw(y):
        return 6.5 + (y - 38) / 40.0 * 5.5
    tower = [(53 - hw(38), 38), (53 + hw(38), 38), (53 + hw(78), 78), (53 - hw(78), 78)]
    s.poly(tower, fill=lin(41, 38, 65, 78, [(0, "#FFFFFF"), (1, "#C9C4EC")]), stroke=edge("#7A70C0", .5), sw=1.1, join=JOIN_ROUND)
    for (ya, yb) in [(47, 55), (63, 71)]:
        s.poly([(53 - hw(ya), ya), (53 + hw(ya), ya), (53 + hw(yb), yb), (53 - hw(yb), yb)], fill=lin(41, ya, 65, yb, [(0, "#8B7FE0"), (1, "#5A4CA8")]))
    s.rect(41.5, 34.5, 23, 4.2, 1.6, fill=gfill("#6E64B8", 41, 34, 64, 39), stroke=edge("#6E64B8", .6), sw=.9)
    glow(s, 53, 28, 13, "#FFE680", .8)
    s.rect(47, 24.5, 12, 10.5, 2.4, fill=lin(47, 24, 59, 35, [(0, "#FFFBD0"), (1, "#FFC94D")]), stroke=edge("#B98A2C", .5), sw=.9)
    s.tri((45, 24.8), (53, 15.5), (61, 24.8), fill=gfill("#8E86C0", 45, 15, 61, 25, .3, .35), stroke=edge("#6E64B8", .6), sw=1, join=JOIN_ROUND)
    s.circle(53, 14.6, 1.6, fill="#FFE680")
    s.path(Path().M(49.6, 78).L(49.6, 71).A(3.4, 3.4, 0, 0, 1, 56.4, 71).L(56.4, 78).Z(), fill="#2B2C60")
    # fantasma
    ghost(s, 75, 46, 15, glowc="#CFC6FF", ga=.6)
    # olas
    terrain(s, [(0, 82), (30, 80), (60, 83), (0, 81)], vgrad("#4F5BB0", "#1E2058", 78, 90))
    s.curve([(24, 84), (30, 82.8), (36, 84), (42, 82.8)], "#FFFFFF80", 1.2)
    s.curve([(58, 85), (64, 83.8), (70, 85), (76, 83.8)], "#FFFFFF80", 1.2)
    return finish(s, A)


def n5():
    """Mansion Embrujada: casona con ventanas encendidas bajo la luna."""
    A = "#6A3FA0"
    s = badge(A, "#4F2D90", "#160B2C", "#C29BFF", (58, 36), 30, .5)
    glow(s, 66, 27, 19, "#FFE9B0", .5)
    s.circle(66, 27, 12, fill=lin(56, 16, 76, 40, [(0, "#FFFDEB"), (1, "#F2D9A0")]))
    s.circle(62.5, 30, 2.6, fill="#E3C98A", op=.6)
    s.circle(70, 23, 1.8, fill="#E3C98A", op=.6)
    bat(s, 22, 32, 13, "#1E1038", rim="#7A62C0")
    bat(s, 78, 50, 11, "#1E1038", rim="#7A62C0")
    ink = lin(24, 36, 76, 80, [(0, "#4A3585"), (1, "#1A0F36")])
    rim = hexa("#8D74D6", .7)
    # torre izquierda
    s.rect(24, 40, 13, 40, 0, fill=ink, stroke=rim, sw=.9)
    s.tri((22, 41), (30.5, 22), (39, 41), fill=ink, stroke=rim, sw=.9, join=JOIN_ROUND)
    # chimenea
    s.rect(64, 36, 6, 14, 0, fill=ink, stroke=rim, sw=.9)
    # cuerpo
    s.rect(34, 52, 44, 28, 0, fill=ink, stroke=rim, sw=.9)
    s.poly([(30, 53), (56, 34), (82, 53)], fill=ink, stroke=rim, sw=.9, join=JOIN_ROUND)
    s.rect(52.4, 33, 6, 6, 0, fill=ink) if False else None
    # ventanas
    wf = lin(0, 55, 0, 72, [(0, "#FFE08A"), (1, "#FF9A3C")])
    for (x, y) in [(41, 61), (53, 61), (65, 61)]:
        glow(s, x + 2.5, y + 4, 7, "#FFB347", .5)
        s.rect(x, y, 5, 8, 2.4, fill=wf)
    glow(s, 56, 47, 6, "#FFB347", .5)
    s.circle(56, 47, 3, fill=wf)
    glow(s, 30.5, 49, 7, "#FFB347", .55)
    s.path(Path().M(28, 56).L(28, 50).A(2.5, 2.5, 0, 0, 1, 33, 50).L(33, 56).Z(), fill=wf)
    s.path(Path().M(51.5, 80).L(51.5, 71.5).A(3.5, 3.5, 0, 0, 1, 58.5, 71.5).L(58.5, 80).Z(), fill="#FFB347")
    # suelo y verja
    terrain(s, [(0, 78), (28, 75), (62, 77), (0, 76)], vgrad("#2D1F5A", "#120A26", 72, 90))
    s.path("M 18 80 L 18 70 M 24 79 L 24 70 M 76 79 L 76 70 M 82 80 L 82 70 M 17 74 L 25 74 M 75 74 L 83 74",
           stroke="#150B2C", sw=1.6, cap=CAP_ROUND)
    # arbol seco
    s.path("M 84 78 Q 83 66 86 58 M 85 66 Q 80 62 78 56 M 85 62 Q 90 60 91 54", stroke="#150B2C", sw=2.2, cap=CAP_ROUND)
    return finish(s, A)


def n6():
    """Mercado de Brujas: sombrero de bruja con hebilla y pocion."""
    A = "#E0782F"
    s = badge(A, "#44256F", "#170A2B", "#FF9A3C", (50, 64), 31, .55)
    sparkles(s, [(24, 22, 2.4), (74, 20, 2.8), (80, 40, 1.8)], "#FFE9B8D0")
    crescent(s, 30, 30, 6, lin(24, 24, 36, 36, [(0, "#FFFBE6"), (1, "#E4D6FF")]))
    # ala
    s.ellipse(50, 69, 32, 9, fill=lin(20, 62, 80, 78, [(0, "#7A4CC0"), (1, "#2A1559")]), stroke=edge("#4A2A8A", .5), sw=1.2)
    s.ellipse(50, 68.4, 25, 6, fill=hexa("#150B2C", .55))
    # cono
    cone = Path().M(31, 67).C(36, 50, 42, 34, 52, 24).C(58, 19, 66, 20, 71, 28).C(66, 27, 63, 30, 63, 36)
    cone.C(64, 48, 66, 58, 69, 67).C(60, 73, 40, 73, 31, 67).Z()
    s.path(cone, fill=lin(30, 24, 70, 72, [(0, "#8C5CD8"), (.55, "#5A32A0"), (1, "#2A1559")]), stroke=edge("#4A2A8A", .55), sw=1.3, join=JOIN_ROUND)
    # cinta
    band = Path().M(33.2, 62).C(42, 66, 58, 66, 66.8, 62).L(68.2, 67.4).C(60, 72.4, 40, 72.4, 31.8, 67.4).Z()
    s.path(band, fill=lin(32, 62, 68, 72, [(0, "#FFB05A"), (1, "#D05A18")]), stroke=edge("#B04A10", .5), sw=.9, join=JOIN_ROUND)
    s.rect(45, 63.4, 10.4, 9, 2, fill=lin(45, 63, 56, 73, [(0, "#FFF0A0"), (1, "#E0A93B")]), stroke=edge("#B98A2C", .5), sw=.9)
    s.rect(47.8, 65.8, 4.8, 4.2, 1, fill="#5A32A0")
    s.curve([(38, 52), (42, 41), (48, 31)], "#FFFFFF66", 1.7)
    # pocion flotante
    glow(s, 78, 55, 11, "#FF9A3C", .55)
    with s.rotate(14, 78, 55):
        s.rect(75.6, 41.5, 4.8, 6.5, 1.4, fill=lin(75, 41, 81, 48, [(0, "#E8D6FF"), (1, "#B7A2E8")]))
        s.rect(75, 40, 6, 3.2, 1.4, fill="#B87A4A")
        s.circle(78, 54, 8, fill=lin(70, 46, 86, 62, [(0, "#F0E6FF"), (1, "#B8A2F0")]), stroke=hexa("#7A5CC0", .8), sw=1)
        s.path(Path().M(70.4, 55).A(7.6, 7, 0, 0, 0, 85.6, 55).Z(), fill=lin(70, 50, 86, 62, [(0, "#FFC94D"), (1, "#E8601A")]))
        s.circle(76.5, 58, 1.3, fill="#FFFFFFB0")
        s.circle(80.5, 55.5, 1, fill="#FFFFFFB0")
    return finish(s, A)


def wolf_pts():
    return [(0, 44), (2, 39), (8, 36), (10, 28), (14, 22), (18, 16), (21, 10), (24, 4), (24.5, -3), (26, -8), (29, -3.5),
            (31, -5), (36, -11), (41, -14), (40.5, -9.5), (35.5, -5), (31, 0), (30, 6), (31, 13), (33, 19), (32, 26),
            (33, 44), (25, 44), (25, 36), (24, 31), (21, 31), (19, 38), (18, 44)]


def n7():
    """Pico Aullante: lobo aullando a la luna sobre la cima."""
    A = "#5668A8"
    s = badge(A, "#324388", "#101736", "#9DB2FF", (60, 38), 31, .5)
    sparkles(s, [(24, 24, 2.4), (30, 40, 1.6), (80, 56, 1.8)])
    glow(s, 66, 30, 22, "#E8EEFF", .6)
    s.circle(66, 30, 14.5, fill=lin(53, 17, 79, 45, [(0, "#FFFFFF"), (1, "#CAD6FF")]))
    for (x, y, r) in [(61, 34, 3.0), (71, 26, 2.2), (69, 36, 1.6)]:
        s.circle(x, y, r, fill="#A9B9EE", op=.55)
    # montana
    mt = [(0, 80), (20, 70), (32, 60), (42, 58), (48, 63), (56, 66), (66, 62), (74, 64), (0, 80)]
    terrain(s, mt, lin(10, 56, 80, 90, [(0, "#6A7DC0"), (1, "#202A5C")]), smooth=False, stroke=hexa("#A5B6F2", .5), sw=.9, join=JOIN_ROUND)
    s.poly([(32, 60), (42, 58), (48, 63), (39, 62), (36, 66)], fill="#EAF0FF", op=.9, join=JOIN_ROUND)
    s.poly([(66, 62), (74, 64), (72.5, 67), (68, 65)], fill="#EAF0FF", op=.9, join=JOIN_ROUND)
    s.poly([(60, 76), (70, 66), (86, 80)], fill=hexa("#101736", .35))
    # lobo
    with s.translate(34, 58.5):
        with s.scale(.62, .62, 0, 44):
            s.poly(wolf_pts(), fill=lin(0, -14, 40, 44, [(0, "#3C4A92"), (1, "#0F1330")]), stroke="#0F1330", sw=2, join=JOIN_ROUND)
            s.path("M 24.5 -3 L 26 -8 L 29 -3.5", stroke="#7C8CD8", sw=1.2, cap=CAP_ROUND, join=JOIN_ROUND, op=.7)
            s.curve([(11, 29), (15, 22), (19, 15), (23, 8)], "#8FA0EE", 1.8, op=.55)
    terrain(s, [(0, 82), (30, 78), (62, 80), (0, 80)], vgrad("#2C3880", "#0E1233", 76, 90))
    return finish(s, A)


def soul(s, x, y, r, a=1.0, tail=1.0, look=0.0):
    """Alma pequena: gota de luz con ojos y cola."""
    glow(s, x, y, r * 2.6, "#E2CCFF", .55 * a)
    p = Path().M(x, y - r * 1.5).C(x + r * 1.3, y - r * .3, x + r * 1.2, y + r * .9, x + r * .6, y + r * 1.1)
    p.C(x + r * .4, y + r * 1.6 * tail, x + r * .1, y + r * 1.4 * tail, x, y + r * 1.9 * tail)
    p.C(x - r * .1, y + r * 1.4 * tail, x - r * .4, y + r * 1.6 * tail, x - r * .6, y + r * 1.1)
    p.C(x - r * 1.2, y + r * .9, x - r * 1.3, y - r * .3, x, y - r * 1.5).Z()
    s.path(p, fill=lin(x, y - r * 1.5, x, y + r * 1.9, [(0, "#FFFFFF"), (1, "#D5BFFF")]), op=a, stroke=hexa("#8E64D6", .7), sw=.7, join=JOIN_ROUND)
    s.circle(x - r * .38 + look, y - r * .2, r * .2, fill="#3A1F6E", op=a)
    s.circle(x + r * .38 + look, y - r * .2, r * .2, fill="#3A1F6E", op=a)


def n8():
    """Arbol de las Almas: copa violeta de la que cuelgan almas."""
    A = "#9B5FD0"
    s = badge(A, "#41247A", "#150A2E", "#C9A0FF", (50, 40), 31, .55)
    sparkles(s, [(22, 30, 2.2), (79, 34, 2.4), (30, 20, 1.5)], "#F1E0FFD0")
    # tronco
    tr = Path().M(37, 88).C(44, 84, 46, 76, 46, 66).C(46, 60, 45, 56, 43, 52).L(58, 52).C(55, 57, 54, 62, 54, 68).C(54, 78, 56, 84, 64, 88).Z()
    s.path(tr, fill=lin(38, 52, 62, 88, [(0, "#5C3F96"), (1, "#1E1236")]), stroke=hexa("#9A7AE0", .5), sw=.9, join=JOIN_ROUND)
    s.curve([(46, 54), (46.5, 66), (45, 78)], "#B79AF0", 1.1, op=.4)
    # copa
    crown = [(24, 44), (22, 33), (30, 22), (42, 17), (56, 17), (68, 22), (77, 32), (76, 44), (66, 54), (50, 56), (34, 54)]
    s.blob(crown, fill=lin(24, 17, 76, 56, [(0, "#C79BFF"), (.5, "#9255D8"), (1, "#5A2E9E")]), stroke=hexa("#4A2A8A", .8), sw=1.2)
    for (x, y, r) in [(38, 28, 9), (58, 27, 8), (49, 40, 10), (31, 42, 6), (68, 40, 6.5)]:
        s.circle(x, y, r, fill=rad(x - r * .3, y - r * .3, r * 1.2, [(0, "#E3C9FF66"), (1, "#E3C9FF00")]))
    s.curve([(30, 30), (37, 22), (46, 20)], "#FFFFFF80", 1.8)
    # almas colgando
    for (x, y, r, lk) in [(30, 60, 4.3, 0.4), (70, 62, 4.8, -0.4), (50, 69, 3.6, 0)]:
        s.line(x, y - r * 1.5 - 5, x, y - r * 1.4, hexa("#E2CCFF", .55), .9)
        soul(s, x, y, r, look=lk)
    s.ellipse(50, 89, 24, 2.5, fill="#00000030")
    terrain(s, [(0, 82), (30, 80), (62, 82), (0, 81)], vgrad("#3F2878", "#140A2A", 78, 90))
    return finish(s, A)


def n9():
    """Tumbas de Momias: momia con ojos que brillan y piramides."""
    A = "#D59A2B"
    s = badge(A, "#3B2870", "#1C0F2E", "#FFB347", (50, 70), 32, .6)
    sparkles(s, [(24, 25, 2.2), (78, 28, 2.4), (68, 18, 1.5)], "#FFF0C8D0")
    crescent(s, 27, 36, 5, lin(22, 30, 32, 42, [(0, "#FFFBE6"), (1, "#FFE9B0")]))
    # piramides
    terrain(s, [(0, 76), (14, 66), (24, 52), (40, 70), (52, 74), (0, 76)], lin(10, 52, 40, 76, [(0, "#E8B861"), (1, "#8A5A22")]), smooth=False)
    s.poly([(24, 52), (40, 70), (30, 72), (24, 52)], fill="#00000030")
    terrain(s, [(0, 74), (60, 74), (76, 56), (88, 72), (0, 74)], lin(60, 56, 90, 76, [(0, "#E8B861"), (1, "#8A5A22")]), smooth=False)
    s.poly([(76, 56), (88, 72), (80, 74), (76, 56)], fill="#00000030")
    # cabeza
    hx, hy, rx, ry = 50, 52, 21, 25
    s.ellipse(50, 86, 24, 3.4, fill="#00000030")
    s.path(Path().M(31, 88).C(31, 74, 38, 72, 50, 72).C(62, 72, 69, 74, 69, 88).Z(),
           fill=lin(31, 72, 69, 88, [(0, "#EAD9B0"), (1, "#A98A58")]), stroke=edge("#8A6A3A", .6), sw=1.2, join=JOIN_ROUND)
    s.ellipse(hx, hy, rx, ry, fill=lin(hx - rx, hy - ry, hx + rx, hy + ry, [(0, "#F4E8C8"), (1, "#B79A66")]), stroke=edge("#8A6A3A", .6), sw=1.3)
    s.rect(31, 44.5, 38, 11, 5, fill="#2B1A22")
    # vendas
    def xs(y):
        return rx * math.sqrt(max(0.0, 1 - ((y - hy) / ry) ** 2))
    bands = [(30, 34), (36.5, 41), (58, 62.5), (65, 69)]
    strips = Path()
    for (ya, yb) in bands:
        ym = (ya + yb) / 2.0
        sl = 2.6
        strips.M(hx - xs(ym) - 1.2, ym + sl).L(hx + xs(ym) + 1.2, ym - sl)
    s.path(strips, stroke=lin(30, 30, 70, 69, [(0, "#FFF7DC"), (1, "#D6BE88")]), sw=4.4, cap=CAP_ROUND)
    s.path(Path().M(hx - 18, 46).L(hx - 4, 64).M(hx + 16, 44).L(hx + 6, 62), stroke="#FFF7DC", sw=0, ) if False else None
    s.curve([(50, 27), (51, 36), (49, 44)], "#C8AE78", 1.2, op=.6)
    s.curve([(36, 36), (38, 43), (34, 52)], "#FFFFFF80", 1.5)
    for ex in (42.5, 57.5):
        glow(s, ex, 50.4, 6.5, "#FFC94D", .75)
        s.ellipse(ex, 50.4, 3.6, 2.9, fill=rad(ex, 50.4, 3.8, [(0, "#FFFBD0"), (1, "#FFC94D")]))
        s.ellipse(ex + .3, 50.6, 1.0, 1.9, fill="#2B1A22")
    s.path(Path().M(31, 78).L(69, 83), stroke=lin(30, 76, 70, 84, [(0, "#FFF7DC"), (1, "#D6BE88")]), sw=4, cap=CAP_ROUND)
    terrain(s, [(0, 84), (30, 82), (62, 84), (0, 83)], vgrad("#C98A3C", "#5A3A1E", 80, 90))
    return finish(s, A)


def n10():
    """Bahia del Barco Fantasma: barco de velas rotas con ojos de buey que brillan."""
    A = "#3F4FA0"
    s = badge(A, "#2B3A8C", "#0B1030", "#7FF0E0", (50, 52), 30, .5)
    glow(s, 28, 27, 14, "#E4EEFF", .5)
    s.circle(28, 27, 8.2, fill=lin(22, 20, 35, 35, [(0, "#FFFFFF"), (1, "#C2D2FF")]))
    sparkles(s, [(72, 22, 2.2), (82, 40, 1.6), (44, 18, 1.5)])
    # velas
    sail = lin(30, 22, 70, 56, [(0, "#F4F6FF"), (1, "#A6B4E6")])
    s.path("M 51 17 L 51 66", stroke="#3A2A52", sw=1.8, cap=CAP_ROUND)
    s.path("M 33 40 L 33 66", stroke="#3A2A52", sw=1.6, cap=CAP_ROUND)
    s.poly([(53, 22), (72, 25), (70, 36), (72, 46), (68, 53), (63, 49), (58, 54), (53, 49)], fill=sail, stroke=hexa("#6B7AC0", .8), sw=.9, join=JOIN_ROUND, op=.92)
    s.poly([(49, 24), (36, 28), (38, 37), (35, 46), (38, 52), (43, 48), (47, 53), (49, 48)], fill=sail, stroke=hexa("#6B7AC0", .8), sw=.9, join=JOIN_ROUND, op=.92)
    s.poly([(35, 43), (24, 46), (26, 53), (31, 51), (35, 55)], fill=sail, stroke=hexa("#6B7AC0", .8), sw=.9, join=JOIN_ROUND, op=.9)
    s.poly([(51, 17), (62, 20), (57, 22.5), (60, 25), (51, 24.5)], fill="#7FF0E0", op=.9, join=JOIN_ROUND)
    # casco
    hull = Path().M(17, 60).L(83, 60).C(80, 72, 72, 80, 62, 80).L(36, 80).C(27, 79, 19, 70, 17, 60).Z()
    glow(s, 50, 70, 28, "#7FF0E0", .22)
    s.path(hull, fill=lin(17, 60, 83, 80, [(0, "#4A4A92"), (1, "#18183C")]), stroke=hexa("#8A94E0", .7), sw=1.1, join=JOIN_ROUND)
    s.rect(16, 58.4, 68, 3.6, 1.6, fill=lin(16, 58, 84, 62, [(0, "#8A94E0"), (1, "#4B5BB0")]))
    for x in (32, 44, 56, 68):
        glow(s, x, 69, 5.5, "#7FF0E0", .8)
        s.circle(x, 69, 2.6, fill=rad(x, 69, 2.8, [(0, "#E8FFFB"), (1, "#5FE0D0")]), stroke="#1A1840", sw=.8)
    s.curve([(22, 63), (50, 66), (78, 63)], "#B6BEF4", .9, op=.4)
    # mar
    terrain(s, [(0, 78), (22, 76), (44, 79), (66, 77), (0, 79)], vgrad("#3E50B0", "#0B1030", 74, 90))
    for (x, y, w) in [(26, 80.5, 22), (62, 82, 24), (44, 86, 20)]:
        s.curve([(x - w / 2, y), (x - w / 4, y - 1.6), (x, y), (x + w / 4, y + 1.6), (x + w / 2, y)], "#DDE8FF", 1.2, op=.55)
    return finish(s, A)


def roof(s, cx, y, w, h, fill, rim, ridge=True):
    """Tejado de pagoda con aleros levantados. y = cumbrera, h = alto, w = ancho total."""
    p = Path().M(cx, y)
    p.C(cx - w * .12, y + h * .55, cx - w * .36, y + h * .7, cx - w / 2, y + h * .56)
    p.C(cx - w * .46, y + h * .98, cx - w * .4, y + h, cx - w * .3, y + h)
    p.L(cx + w * .3, y + h)
    p.C(cx + w * .4, y + h, cx + w * .46, y + h * .98, cx + w / 2, y + h * .56)
    p.C(cx + w * .36, y + h * .7, cx + w * .12, y + h * .55, cx, y).Z()
    return s.path(p, fill=fill, stroke=rim, sw=1.0, join=JOIN_ROUND)


def n11():
    """Templo Maldito: pagoda de tres pisos con farolillos y llamas errantes."""
    A = "#B33C5E"
    s = badge(A, "#4F2160", "#16081E", "#FF4D7E", (50, 52), 31, .5)
    sparkles(s, [(22, 26, 2.0), (80, 26, 2.2)], "#FFD9E6C0")
    glow(s, 50, 48, 24, "#FF7AA2", .3)
    ink = lin(30, 20, 70, 80, [(0, "#4A2A66"), (1, "#1A0B26")])
    red = lin(20, 20, 80, 50, [(0, "#E8527E"), (1, "#7A1F3E")])
    rim = hexa("#FF9AB8", .6)
    # tres pisos de abajo a arriba
    tiers = [(66, 40, 76), (50, 30, 62), (36, 21, 50)]
    for (ytop_body, bw, ybot) in tiers:
        pass
    # piso 1
    s.rect(31, 61, 38, 17, 0, fill=ink, stroke=rim, sw=.8)
    roof(s, 50, 52, 54, 11, red, edge("#B33C5E", .6))
    # piso 2
    s.rect(36, 44, 28, 8.6, 0, fill=ink, stroke=rim, sw=.8)
    roof(s, 50, 35.5, 42, 10, red, edge("#B33C5E", .6))
    # piso 3
    s.rect(41, 29.5, 18, 6.5, 0, fill=ink, stroke=rim, sw=.8)
    roof(s, 50, 20.5, 32, 9.5, red, edge("#B33C5E", .6))
    s.line(50, 12, 50, 21, "#F2C86B", 1.6)
    s.circle(50, 11.4, 2, fill="#FFE08A")
    # ventanas encendidas
    wf = lin(0, 62, 0, 74, [(0, "#FFC2D6"), (1, "#FF4D7E")])
    for x in (37, 44.5, 55.5, 63):
        pass
    for (x, y, w, h) in [(36, 64, 5, 8), (56, 64, 5, 8), (46, 63, 8, 14), (41.6, 31.5, 3.6, 4), (54.8, 31.5, 3.6, 4), (45, 46, 3.6, 4.4), (51.4, 46, 3.6, 4.4)]:
        glow(s, x + w / 2, y + h / 2, max(w, h) * .9, "#FF4D7E", .55)
        s.rect(x, y, w, h, min(w, h) / 2.2, fill=wf)
    # farolillos
    for (x, y) in [(23, 62), (77, 62)]:
        s.line(x, y - 9, x, y - 4, "#F2C86B", .9)
        glow(s, x, y, 7, "#FF4D7E", .6)
        s.ellipse(x, y, 3.2, 4.2, fill=lin(x - 3, y - 4, x + 3, y + 4, [(0, "#FFC2D6"), (1, "#E0386A")]), stroke=edge("#B33C5E", .5), sw=.7)
    # base y escalones
    terrain(s, [(0, 79), (30, 77), (62, 79), (0, 78)], vgrad("#3A1B4E", "#12071A", 74, 90))
    s.poly([(44, 90), (56, 90), (54, 78), (46, 78)], fill=lin(0, 78, 0, 90, [(0, "#8A5A9A"), (1, "#3A1B4E")]), op=.9)
    # llamas errantes
    for (x, y, r) in [(24, 44, 3.3), (77, 40, 2.8), (30, 74, 2.4)]:
        glow(s, x, y, r * 3, "#C79BFF", .6)
        p = Path().M(x, y - r * 2).C(x + r * 1.2, y - r * .6, x + r * 1.1, y + r * .8, x, y + r).C(x - r * 1.1, y + r * .8, x - r * 1.2, y - r * .6, x, y - r * 2).Z()
        s.path(p, fill=lin(x, y - r * 2, x, y + r, [(0, "#FFFFFF"), (1, "#B58CFF")]))
    return finish(s, A)


def build():
    icons = {}
    for i, fn in enumerate([n0, n1, n2, n3, n4, n5, n6, n7, n8, n9, n10, n11]):
        icons["chapter.n%d" % i] = fn()
    return {"icons": icons}
