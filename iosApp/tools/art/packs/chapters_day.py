"""
Capitulos de dia del mapa de ParDos (iPhone).

  chapter.d0 .. chapter.d11   emblemas 100x100 (medallon pequeno con una escena del capitulo)
  landmark.<tipo>             monumentos 160x120 (base apoyada en y~112) de los 12 paisajes de dia:
                              dunes pines river hills clouds lavender lanterns mountains cherry gold_dunes moon bamboo
"""
import os
import sys
import math

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, ".."))   # artlib
sys.path.insert(0, HERE)
from artlib import *  # noqa: E402,F401,F403

INK = "#2B1B3A"


# ------------------------------------------------------------------ color (todo se calcula en Python: JSON pequeno)

def _rgb(h):
    h = h.lstrip("#")
    return [int(h[i:i + 2], 16) for i in (0, 2, 4)]


def M(a, b, t):
    ca, cb = _rgb(a), _rgb(b)
    return "#%02X%02X%02X" % tuple(int(round(ca[i] + (cb[i] - ca[i]) * t)) for i in range(3))


def lt(c, t=.35):
    return M(c, "#FFFFFF", t)


def dk(c, t=.2):
    return M(c, INK, t)


def AL(c, a):
    """Color hex con alfa 0..1."""
    return c + "%02X" % int(round(a * 255))


def gd(c, x, y, w, h, hi=.35, lo=.2):
    """Degradado diagonal luz arriba-izquierda / sombra abajo-derecha sobre la caja x,y,w,h."""
    return lin(x, y, x + w * .7, y + h, [(0, lt(c, hi)), (1, dk(c, lo))])


def gv(c, y1, y2, hi=.3, lo=.18):
    return lin(0, y1, 0, y2, [(0, lt(c, hi)), (1, dk(c, lo))])


def gh(c, x1, x2, hi=.3, lo=.2):
    return lin(x1, 0, x2, 0, [(0, lt(c, hi)), (1, dk(c, lo))])


def ol(c, t=.45):
    return dk(c, t)


def shadow(s, cx, cy, rx, ry=None):
    ry = ry if ry is not None else max(3.0, rx * .13)
    s.ellipse(cx, cy, rx * 1.1, ry * 1.3, fill="#2B1B3A14")
    s.ellipse(cx, cy, rx, ry, fill="#2B1B3A26")


def shine(s, cx, cy, rx, ry, a=.5, rot=-25):
    with s.rotate(rot, cx, cy):
        s.ellipse(cx, cy, rx, ry, fill="#FFFFFF" + "%02X" % int(a * 255))


# ------------------------------------------------------------------ piezas comunes

def pagoda_roof(s, cx, yb, w, h, c, ov=7, sw=1.1, hi=.32, lo=.22):
    """Tejado curvo de pagoda: bordes que suben en punta. yb = borde inferior, h = alto sobre yb."""
    hw = w / 2.0
    p = Path()
    p.M(cx - hw - ov, yb - 3)
    p.Q(cx - hw + 3, yb + 2, cx - hw + 10, yb + 1)
    p.L(cx + hw - 10, yb + 1)
    p.Q(cx + hw - 3, yb + 2, cx + hw + ov, yb - 3)
    p.Q(cx + hw * .38, yb - h * .16, cx, yb - h)
    p.Q(cx - hw * .38, yb - h * .16, cx - hw - ov, yb - 3)
    p.Z()
    s.path(p, fill=gh(c, cx - hw - ov, cx + hw + ov, hi, lo), stroke=ol(c), sw=sw, join=JOIN_ROUND)
    return p


def pine(s, x, yb, h, c, sw=0.0, trunk="#8A5A33"):
    """Pino de tres pisos con tronco. yb = pie, h = altura total."""
    s.rect(x - h * .05, yb - h * .2, h * .1, h * .2, r=h * .02, fill=gh(trunk, x - h * .05, x + h * .05, .25, .2))
    for k in range(3):
        top = yb - h + k * h * .27
        bot = top + h * .45
        hw = h * (.19 + .075 * k)
        col = M(c, "#2B6B4A", .0 + k * .0)
        s.poly([(x, top), (x + hw, bot), (x - hw, bot)],
               fill=gh(c, x - hw, x + hw, .22 - .03 * k, .26 + .02 * k),
               stroke=(ol(c, .5) if sw else None), sw=sw, join=JOIN_ROUND)


def spark_pts(cx, cy, rx, ry, a0, a1, n=8):
    out = []
    for i in range(n + 1):
        a = math.radians(a0 + (a1 - a0) * i / n)
        out.append((cx + rx * math.cos(a), cy + ry * math.sin(a)))
    return out


# ------------------------------------------------------------------ medallon de los emblemas

MC = (50.0, 50.0)
MR = 35.0


def _edge(y, sign):
    dy = y - MC[1]
    return MC[0] + sign * math.sqrt(max(MR * MR - dy * dy, 0.0))


def seg_ground(s, pts, fill, stroke=None, sw=0.0):
    """Suelo del medallon: perfil superior (izq -> der) y por debajo todo el circulo. Los extremos van al borde."""
    y0, y1 = pts[0][1], pts[-1][1]
    p0 = (_edge(y0, -1), y0)
    p1 = (_edge(y1, +1), y1)
    prof = [p0] + list(pts[1:-1]) + [p1]
    a0 = math.degrees(math.atan2(p0[1] - MC[1], p0[0] - MC[0]))
    a1 = math.degrees(math.atan2(p1[1] - MC[1], p1[0] - MC[0]))
    large = 1 if ((a0 - a1) % 360.0) > 180.0 else 0
    p = Path()
    p.cmds = catmull(prof)
    p._cx, p._cy = p1
    p.A(MR, MR, 0, large, 1, p0[0], p0[1])
    p.Z()
    return s.path(p, fill=fill, stroke=stroke, sw=sw, join=JOIN_ROUND)


def band(s, ya, yb, fill, stroke=None, sw=0.0):
    """Franja horizontal del circulo entre ya y yb (agua, camino...)."""
    xl_a, xr_a = _edge(ya, -1), _edge(ya, +1)
    xl_b, xr_b = _edge(yb, -1), _edge(yb, +1)
    p = Path()
    p.M(xl_a, ya).L(xr_a, ya)
    p.A(MR, MR, 0, 0, 1, xr_b, yb)
    p.L(xl_b, yb)
    p.A(MR, MR, 0, 0, 1, xl_a, ya)
    p.Z()
    return s.path(p, fill=fill, stroke=stroke, sw=sw, join=JOIN_ROUND)


def medal_begin(s, c, top, bot):
    s.ellipse(50, 94, 26, 2.8, fill="#2B1B3A24")
    s.circle(50, 50, 43, fill=lin(14, 8, 86, 94, [(0, lt(c, .55)), (.45, c), (1, dk(c, .34))]), stroke=ol(c, .5), sw=1.3)
    s.circle(50, 50, 38.6, fill=lin(80, 90, 20, 12, [(0, lt(c, .5)), (1, dk(c, .3))]))
    s.circle(50, 50, 36.2, fill=dk(c, .42))
    s.circle(50, 50, MR, fill=lin(0, 15, 0, 85, [(0, top), (1, bot)]))


def medal_end(s, c):
    s.circle(50, 50, MR, stroke=AL(dk(c, .55), .55), sw=1.3)
    s.arc_stroke(50, 50, 40.6, 196, 262, "#FFFFFFB0", 2.2)
    s.arc_stroke(50, 50, 40.6, 272, 300, "#FFFFFF66", 1.6)


def bloom_sun(s, x, y, r, glow=.3, a="#FFF2BE", b="#FFD470"):
    s.circle(x, y, r * 1.9, fill=AL(b, glow * .55))
    s.circle(x, y, r * 1.45, fill=AL(b, glow))
    s.circle(x, y, r, fill=rad(x - r * .3, y - r * .3, r * 1.3, [(0, a), (1, b)]))


def leaf_spark(s, x, y, r, c="#FFFFFF", a=.9):
    s.sparkle(x, y, r, fill=AL(c, a))


# ================================================================== EMBLEMAS

def emblem_0():  # Jardin de Arena
    c = "#6B9E86"
    s = Scene(100, 100)
    medal_begin(s, c, "#DDEFE3", "#FBF2DA")
    bloom_sun(s, 66, 31, 5.2)
    sand = "#EBD7A6"
    seg_ground(s, [(15, 63), (32, 59.5), (50, 58), (68, 59.5), (85, 63)],
               fill=lin(0, 58, 0, 85, [(0, lt(sand, .4)), (1, dk(sand, .2))]))
    # arena rastrillada
    for rx, ry, w in ((32, 10.5, 1.5), (23, 7.6, 1.5), (14.5, 4.6, 1.4)):
        pth = Path().M(50 - rx, 62).A(rx, ry, 0, 0, 0, 50 + rx, 62)
        s.path(pth, stroke=AL(dk(sand, .35), .6), sw=w, cap=CAP_ROUND)
    # piedras apiladas
    grey = "#A9B0A8"
    s.ellipse(50, 61.5, 13.5, 7.6, fill=gd(grey, 36, 54, 28, 16, .4, .3), stroke=ol(grey, .5), sw=.9)
    s.ellipse(51, 52, 9.8, 5.6, fill=gd(grey, 41, 46, 20, 12, .45, .3), stroke=ol(grey, .5), sw=.9)
    s.ellipse(50, 45, 6, 4.2, fill=gd(grey, 44, 41, 12, 8, .5, .3), stroke=ol(grey, .5), sw=.9)
    shine(s, 44, 59, 5, 1.6, .55)
    shine(s, 47.5, 50.3, 3.6, 1.3, .55)
    s.ellipse(52, 66.5, 7, 1.6, fill="#6B9E8666")   # musgo
    medal_end(s, c)
    return s.bake()


def emblem_1():  # Bosque Sereno
    c = "#3F9468"
    s = Scene(100, 100)
    medal_begin(s, c, "#D9EFE2", "#F6F1DB")
    bloom_sun(s, 31, 30, 4.6)
    seg_ground(s, [(15, 64), (34, 58), (50, 57), (68, 58), (85, 64)],
               fill=lin(0, 56, 0, 80, [(0, lt("#7DBB8C", .2)), (1, "#5DA479")]))
    g = "#3F9468"
    pine(s, 28.5, 67, 28, M(g, "#FFFFFF", .12), sw=.8)
    pine(s, 72, 67, 29, M(g, "#FFFFFF", .12), sw=.8)
    pine(s, 50, 69, 46, g, sw=.9)
    seg_ground(s, [(15, 70), (32, 68), (50, 70), (68, 68), (85, 70)],
               fill=lin(0, 66, 0, 86, [(0, "#6FB07F"), (1, "#3F8A5E")]))
    for x, y in ((38, 77), (60, 75)):
        s.ellipse(x, y, 4.5, 1.4, fill="#FFFFFF44")
    medal_end(s, c)
    return s.bake()


def emblem_2():  # Orilla del Rio
    c = "#4E8FA6"
    s = Scene(100, 100)
    medal_begin(s, c, "#D4EAF2", "#F6F1DE")
    bloom_sun(s, 31, 31, 4.8)
    seg_ground(s, [(15, 62), (32, 56), (50, 58), (68, 56), (85, 62)],
               fill=lin(0, 54, 0, 80, [(0, "#9CCB86"), (1, "#69A878")]))
    band(s, 61, 76, lin(0, 61, 0, 76, [(0, "#8CC6D6"), (1, "#4E8FA6")]))
    for pts in ([(27, 66), (35, 64.5), (44, 66.5)], [(54, 70), (63, 68.5), (73, 70.5)], [(36, 73), (46, 71.5), (56, 73.5)]):
        s.curve(pts, "#FFFFFF99", 1.2)
    seg_ground(s, [(15, 76), (30, 75), (50, 78), (70, 75), (85, 76)],
               fill=lin(0, 74, 0, 86, [(0, "#8DC27D"), (1, "#559A6A")]))
    # puente en arco
    wood = "#B9824E"
    br = Path().M(23, 66).A(27, 20, 0, 0, 1, 77, 66).L(71.5, 66).A(21.5, 14, 0, 0, 0, 28.5, 66).Z()
    s.path(br, fill=lin(0, 46, 0, 66, [(0, lt(wood, .35)), (1, dk(wood, .25))]), stroke=ol(wood, .5), sw=1.0, join=JOIN_ROUND)
    s.path(Path().M(25.5, 63.5).A(24.6, 18.2, 0, 0, 1, 74.5, 63.5), stroke="#FFFFFF77", sw=1.1, cap=CAP_ROUND)
    s.path(Path().M(28.5, 66).A(21.5, 14, 0, 0, 1, 71.5, 66), stroke=AL(dk(wood, .5), .5), sw=1.0)
    s.ellipse(67, 80, 4.2, 1.6, fill="#7FC8A0")
    s.circle(67.5, 79.6, 1.1, fill="#FFB6C8")
    medal_end(s, c)
    return s.bake()


def emblem_3():  # Colinas de Te
    c = "#8DB04A"
    s = Scene(100, 100)
    medal_begin(s, c, "#EEF5D2", "#FCF5DC")
    bloom_sun(s, 69, 30, 5)
    seg_ground(s, [(15, 62), (30, 52), (50, 46), (68, 50), (85, 58)],
               fill=lin(0, 44, 0, 70, [(0, "#C9DE8C"), (1, "#A3C463")]))
    hill = "#8DB04A"
    seg_ground(s, [(15, 72), (28, 60), (42, 53), (58, 58), (72, 68), (85, 76)],
               fill=lin(20, 50, 70, 84, [(0, lt(hill, .3)), (1, dk(hill, .3))]), stroke=ol(hill, .4), sw=.8)
    # bancales de te
    for pts in ([(24, 69), (33, 61.5), (43, 57), (56, 62), (67, 70)],
                [(29, 77), (38, 69.5), (47, 66.5), (58, 71), (66, 77)]):
        s.curve(pts, AL(dk(hill, .55), .6), 2.1)
        s.curve([(x, y - 1.6) for x, y in pts], "#FFFFFF55", .9)
    # casa de te
    s.rect(38.5, 45, 11, 8.5, r=.8, fill="#FFF6E0", stroke=ol("#B98456", .3), sw=.7)
    s.rect(42.7, 48, 2.8, 5.4, r=1.2, fill="#7A4A2A")
    pagoda_roof(s, 44, 45, 11, 8, "#3E8F7A", ov=4.2, sw=.8)
    s.circle(44, 36, 1, fill="#FFD36E")
    seg_ground(s, [(17, 79), (32, 77), (50, 80), (68, 77), (83, 79)],
               fill=lin(0, 76, 0, 86, [(0, "#8DB04A"), (1, "#5E8C38")]))
    medal_end(s, c)
    return s.bake()


def emblem_4():  # Faro en la Bruma
    c = "#7A8FB5"
    s = Scene(100, 100)
    medal_begin(s, c, "#B7C4E0", "#EDEFF6")
    seg_ground(s, [(15, 66), (35, 65), (50, 64.5), (65, 65), (85, 66)],
               fill=lin(0, 64, 0, 86, [(0, "#93A6C9"), (1, "#667CAA")]))
    for pts in ([(20, 72), (30, 70.5), (41, 72.5)], [(60, 74), (70, 72.5), (80, 74.5)], [(34, 79), (46, 77.5), (58, 79.5)]):
        s.curve(pts, "#FFFFFF88", 1.2)
    # haz de luz
    s.poly([(50, 29.5), (26, 25), (26, 34)], fill=lin(50, 0, 26, 0, [(0, "#FFF4B0B0"), (1, "#FFF4B000")]))
    s.poly([(50, 29.5), (72, 26), (72, 33.5)], fill=lin(50, 0, 72, 0, [(0, "#FFF4B0A0"), (1, "#FFF4B000")]))
    s.circle(50, 29.5, 9, fill="#FFF4B04D")
    # roca y torre
    rock = "#7B8294"
    s.ellipse(50, 70.5, 17, 6, fill=gd(rock, 33, 64, 34, 12, .35, .3), stroke=ol(rock, .5), sw=.9)
    tw = Path().M(43.2, 70).L(45.8, 36).L(54.2, 36).L(56.8, 70).Z()
    s.path(tw, fill=lin(43, 0, 57, 0, [(0, "#FFFFFF"), (.6, "#EEF0F8"), (1, "#B9C2DC")]), stroke=ol(c, .5), sw=.9, join=JOIN_ROUND)
    for ya, yb in ((59, 65), (46, 52)):
        def hwid(y):
            return 4.2 + (y - 36) / 34.0 * 2.6
        st = Path().M(50 - hwid(ya), ya).L(50 + hwid(ya), ya).L(50 + hwid(yb), yb).L(50 - hwid(yb), yb).Z()
        s.path(st, fill=lin(43, 0, 57, 0, [(0, "#E8605F"), (1, "#B63B48")]))
    s.rect(43.5, 33, 13, 3, r=1, fill="#55606E")
    s.rect(46, 26, 8, 7, r=1.3, fill=lin(0, 26, 0, 33, [(0, "#FFF2A0"), (1, "#FFC85A")]), stroke=ol("#E0A93B", .5), sw=.7)
    s.poly([(50, 18.5), (43.8, 26.4), (56.2, 26.4)], fill=lin(44, 0, 56, 0, [(0, "#EF6B66"), (1, "#B63B48")]), stroke=ol("#B63B48", .4), sw=.7, join=JOIN_ROUND)
    # bruma
    s.ellipse(50, 66, 29, 4.2, fill="#FFFFFF8C")
    s.ellipse(35, 51, 13, 2.6, fill="#FFFFFF66")
    s.ellipse(65, 58, 11, 2.3, fill="#FFFFFF59")
    medal_end(s, c)
    return s.bake()


def spike(s, x, ytop, ybot, w, c):
    """Espiga de lavanda: gota alargada con tres puntos de luz."""
    p = Path().M(x, ytop)
    p.C(x + w, ytop + (ybot - ytop) * .25, x + w * .9, ybot - 1, x, ybot)
    p.C(x - w * .9, ybot - 1, x - w, ytop + (ybot - ytop) * .25, x, ytop)
    p.Z()
    s.path(p, fill=gd(c, x - w, ytop, w * 2, ybot - ytop, .4, .22), stroke=ol(c, .45), sw=.6, join=JOIN_ROUND)
    s.ellipse(x - w * .3, ytop + (ybot - ytop) * .38, w * .22, (ybot - ytop) * .08, fill="#FFFFFF77")


def emblem_5():  # Valle de Lavanda
    c = "#8E6BD6"
    s = Scene(100, 100)
    medal_begin(s, c, "#E4DAF7", "#FBF0E3")
    bloom_sun(s, 29, 30, 4.8)
    seg_ground(s, [(15, 62), (30, 55), (50, 52), (70, 55), (85, 62)],
               fill=lin(0, 50, 0, 70, [(0, "#CDBBF0"), (1, "#B49BE4")]))
    # molino
    cream = "#FFF6E0"
    tw = Path().M(43.3, 67).L(45.6, 44).L(54.4, 44).L(56.7, 67).Z()
    s.path(tw, fill=lin(43, 0, 57, 0, [(0, "#FFFFFF"), (1, "#E3D5C0")]), stroke=ol("#B98456", .4), sw=.8, join=JOIN_ROUND)
    s.rect(47.4, 58, 5.2, 9, r=2.2, fill="#7A4A2A")
    pagoda_roof(s, 50, 45, 9, 8.5, "#7A5C9E", ov=2.6, sw=.7)
    hub = (50.0, 41.5)
    with s.rotate(18, hub[0], hub[1]):
        for i in range(4):
            with s.rotate(i * 90, hub[0], hub[1]):
                s.rect(hub[0] - .8, hub[1] - 17, 1.6, 17, r=.5, fill="#8A5A33")
                s.rect(hub[0] + .8, hub[1] - 16, 5.4, 11, r=.7, fill=lin(hub[0], 0, hub[0] + 6, 0, [(0, "#FFFFFF"), (1, "#E3DAEE")]), stroke="#8A5A3366", sw=.5)
    s.circle(hub[0], hub[1], 2.1, fill="#8A5A33")
    # campo de lavanda con surcos
    seg_ground(s, [(15, 66), (32, 64), (50, 66), (68, 64), (85, 66)],
               fill=lin(0, 62, 0, 86, [(0, "#B79BEA"), (1, "#7F5CC4")]))
    for x0, x1 in ((19, 42), (26, 45), (38, 48), (62, 52), (74, 55), (81, 58)):
        s.curve([(x0, 82 if x0 > 24 and x0 < 76 else 72), ((x0 + x1) / 2, 72), (x1, 66.5)], AL("#E8DBFF", .45), 1.1)
    for x, y in ((27, 70), (36, 75), (44, 70), (56, 71), (64, 76), (72, 70)):
        spike(s, x, y - 8, y + 2, 2.3, "#9A74E0")
    medal_end(s, c)
    return s.bake()


def emblem_6():  # Mercado Nocturno
    c = "#E0782F"
    s = Scene(100, 100)
    medal_begin(s, c, "#2C285E", "#8A507A")
    for x, y, r in ((30, 24, 1.5), (68, 22, 1.3), (78, 36, 1.1), (22, 40, 1.0), (54, 20, 1.2)):
        s.sparkle(x, y, r * 1.6, fill="#FFF4D0CC")
    s.curve([(21, 31), (35, 37), (50, 39), (65, 37), (79, 31)], "#3B2A3E", 1.1)
    for x, yy, rx, ry, col in ((34, 40.5, 5.4, 6.4, "#FF8A3C"), (66, 40.5, 5.4, 6.4, "#FF8A3C"), (50, 49, 7.4, 8.8, "#E5576B")):
        s.circle(x, yy, rx * 2.4, fill=AL("#FFB45A", .2))
        s.circle(x, yy, rx * 1.7, fill=AL("#FFB45A", .24))
        s.rect(x - 2.2, yy - ry - 2.2, 4.4, 2.6, r=.8, fill="#4A2E3A")
        s.ellipse(x, yy, rx, ry, fill=rad(x - rx * .3, yy - ry * .3, rx * 1.6, [(0, lt(col, .55)), (.6, col), (1, dk(col, .25))]),
                  stroke=ol(col, .45), sw=.8)
        s.ellipse(x - rx * .3, yy - ry * .35, rx * .22, ry * .3, fill="#FFFFFF88")
    # puesto con toldo
    s.rect(31, 68, 38, 12, r=1.5, fill=lin(0, 68, 0, 80, [(0, "#B98456"), (1, "#7A4A2A")]))
    s.rect(33, 59, 2.4, 10, fill="#7A4A2A")
    s.rect(64.6, 59, 2.4, 10, fill="#7A4A2A")
    for x, col in ((38, "#FFD36E"), (46, "#E5576B"), (54, "#9BD7A6"), (62, "#FFB15A")):
        s.circle(x, 66.6, 2.2, fill=gd(col, x - 2.2, 64.4, 4.4, 4.4, .5, .2))
    aw = Path().M(28, 56)
    aw.L(72, 56).L(72, 62)
    n = 8
    wdt = 44.0 / n
    for k in range(n):
        aw.A(wdt / 2, wdt / 2, 0, 0, 1, 72 - (k + 1) * wdt, 62)
    aw.Z()
    s.path(aw, fill=lin(0, 56, 0, 66, [(0, "#FFFFFF"), (1, "#EADFCB")]), stroke=ol("#E5576B", .3), sw=.7, join=JOIN_ROUND)
    st = Path()
    for k in range(0, n, 2):
        x0 = 28 + k * wdt
        st.M(x0, 56).L(x0 + wdt, 56).L(x0 + wdt, 62).A(wdt / 2, wdt / 2, 0, 0, 1, x0, 62).Z()
    s.path(st, fill=lin(0, 56, 0, 66, [(0, "#F0717F"), (1, "#C2394D")]))
    s.rect(27, 53.5, 46, 3.4, r=1.4, fill=gv("#E5576B", 53, 57, .3, .3))
    seg_ground(s, [(15, 78), (35, 80), (50, 81), (65, 80), (85, 78)], fill=lin(0, 76, 0, 86, [(0, "#4A2E52"), (1, "#2C1B3A")]))
    medal_end(s, c)
    return s.bake()


def emblem_7():  # Pico Nevado
    c = "#5B8FC4"
    s = Scene(100, 100)
    medal_begin(s, c, "#D2E4F4", "#F5F2E8")
    bloom_sun(s, 70, 28, 4.4)
    rock = "#6F93BE"
    # montanas del fondo
    s.poly([(20, 64), (32, 44), (47, 66)], fill=lin(20, 44, 47, 66, [(0, lt(rock, .1)), (1, dk(rock, .15))]), join=JOIN_ROUND)
    s.poly([(32, 44), (47, 66), (38, 66)], fill=AL(dk(rock, .35), .35))
    s.poly([(32, 44), (35.5, 49.5), (32, 48.3), (28.5, 50.5)], fill="#FFFFFF")
    s.poly([(58, 66), (72, 45), (80, 63)], fill=lin(58, 45, 80, 66, [(0, lt(rock, .05)), (1, dk(rock, .2))]), join=JOIN_ROUND)
    s.poly([(72, 45), (80, 63), (70, 66)], fill=AL(dk(rock, .4), .35))
    s.poly([(72, 45), (75.6, 50.2), (72, 49), (68.8, 51.2)], fill="#FFFFFF")
    # pico principal
    s.poly([(52, 26), (78, 68), (22, 68)], fill=lin(30, 26, 70, 68, [(0, lt(c, .3)), (1, dk(c, .1))]), stroke=ol(c, .4), sw=.8, join=JOIN_ROUND)
    s.poly([(52, 26), (78, 68), (60, 68), (56, 54), (53, 42)], fill=AL(dk(c, .6), .5))
    s.poly([(52, 26), (62.6, 43.4), (57.5, 41.6), (53, 46.5), (49, 41.5), (43.2, 43.8)], fill=lin(44, 26, 63, 46, [(0, "#FFFFFF"), (1, "#E6EEF9")]), join=JOIN_ROUND)
    s.poly([(52, 26), (62.6, 43.4), (57.5, 41.6), (53, 46.5)], fill="#B9CBE6AA", join=JOIN_ROUND)
    s.line(52, 26, 52, 17.5, "#6B5360", 1.0)
    s.poly([(52, 17.5), (61, 20.2), (52, 23)], fill="#E5576B", join=JOIN_ROUND)
    seg_ground(s, [(15, 67), (32, 66), (50, 68), (68, 66), (85, 67)], fill=lin(0, 65, 0, 86, [(0, "#FFFFFF"), (1, "#C8D8EC")]))
    pine(s, 26.5, 75, 14, "#4A9B78", sw=.0)
    pine(s, 73, 74, 12, "#4A9B78", sw=.0)
    s.ellipse(50, 78, 20, 2.2, fill="#FFFFFF99")
    medal_end(s, c)
    return s.bake()


def emblem_8():  # Isla de Cerezos
    c = "#E57A9A"
    s = Scene(100, 100)
    medal_begin(s, c, "#FBDDE6", "#FFF5E8")
    bloom_sun(s, 69, 28, 4.6)
    seg_ground(s, [(15, 63), (35, 62), (50, 62), (65, 62), (85, 63)], fill=lin(0, 62, 0, 86, [(0, "#A9D6E2"), (1, "#74B0C6")]))
    for pts in ([(22, 71), (30, 69.5), (38, 71.5)], [(62, 74), (70, 72.5), (78, 74.5)], [(30, 80), (40, 78.5), (50, 80.5)]):
        s.curve(pts, "#FFFFFF88", 1.1)
    # isla
    s.ellipse(50, 70, 28, 8.5, fill=lin(0, 62, 0, 78, [(0, "#9CCB86"), (1, "#5E9E6E")]), stroke=ol("#5E9E6E", .45), sw=.8)
    # cerezo
    wood = "#8A5A33"
    s.path(Path().M(31, 70).C(30, 63, 36, 58, 34, 50), stroke=wood, sw=3.2, cap=CAP_ROUND)
    for cx, cy, r in ((24, 46, 7.5), (36, 40, 9.6), (46, 49, 7), (30, 54, 6.5)):
        s.circle(cx, cy, r, fill=rad(cx - r * .35, cy - r * .4, r * 1.5, [(0, "#FFC6D6"), (.6, "#F59CB8"), (1, "#D9628A")]), stroke=ol("#E57A9A", .35), sw=.6)
    s.circle(33, 36, 2.6, fill="#FFFFFF88")
    s.circle(22, 43, 1.9, fill="#FFFFFF77")
    # torii
    red = "#D94F4F"
    for x in (53, 67):
        s.rect(x - 1.8, 41, 3.6, 29, r=.8, fill=gh(red, x - 2, x + 2, .3, .25))
    s.rect(50.5, 49, 19, 3.2, r=.8, fill=gv(red, 49, 52, .3, .25))
    s.rect(51.5, 44, 17, 2.4, fill=gv(red, 44, 46.5, .2, .1))
    kas = Path().M(47, 40).Q(60, 44, 73, 40).L(71.6, 44.4).Q(60, 47.4, 48.4, 44.4).Z()
    s.path(kas, fill=gv("#4A4358", 40, 46, .2, .3), stroke="#D94F4F", sw=.7, join=JOIN_ROUND)
    for x, y, rot in ((70, 56, 30), (44, 31, -20), (58, 31, 20), (22, 62, 10)):
        with s.rotate(rot, x, y):
            s.ellipse(x, y, 1.9, 1.1, fill="#FFB6C9")
    medal_end(s, c)
    return s.bake()


def emblem_9():  # Desierto Dorado
    c = "#E0A93B"
    s = Scene(100, 100)
    medal_begin(s, c, "#FFEEBE", "#FFF8E4")
    bloom_sun(s, 68, 32, 8.5, glow=.42, a="#FFF6C8", b="#FFC95A")
    sand_l, sand_d = "#F5D58E", "#C99649"
    # piramide grande
    s.poly([(42, 33), (19, 69), (44, 75)], fill=lin(19, 33, 44, 75, [(0, "#F9E1A6"), (1, "#EDBE68")]), join=JOIN_ROUND)
    s.poly([(42, 33), (44, 75), (71, 67)], fill=lin(42, 33, 71, 75, [(0, "#D9A04A"), (1, "#A9702C")]), join=JOIN_ROUND)
    s.line(42, 33, 44, 75, AL(dk("#D9A04A", .5), .5), .8)
    s.path("M 36 44 L 46 47 L 56 44 M 31 54 L 45 58 L 60 54", stroke=AL(dk(sand_d, .4), .35), sw=.8)
    # piramide pequena
    s.poly([(65, 52), (52, 71), (64, 76)], fill=lin(52, 52, 64, 76, [(0, "#FBE6B0"), (1, "#EFC472")]), join=JOIN_ROUND)
    s.poly([(65, 52), (64, 76), (77, 70)], fill=lin(65, 52, 77, 76, [(0, "#D9A04A"), (1, "#A9702C")]), join=JOIN_ROUND)
    seg_ground(s, [(15, 70), (30, 72), (46, 76), (62, 77), (76, 71), (85, 70)],
               fill=lin(0, 70, 0, 86, [(0, lt(sand_l, .2)), (1, "#E3B462")]), stroke=AL(ol(c, .4), .5), sw=.7)
    seg_ground(s, [(19, 79), (36, 78), (54, 82), (72, 78), (81, 79)], fill=lin(0, 78, 0, 86, [(0, "#EDC579"), (1, "#C99649")]))
    s.curve([(38, 82), (46, 80.4), (54, 82.2)], "#FFFFFF77", 1.1)
    medal_end(s, c)
    return s.bake()


def crescent(s, cx, cy, R, r, d, fill, stroke=None, sw=0.0, rot=0.0):
    """Luna creciente: circulo R menos circulo r desplazado d a la derecha."""
    x = (R * R - r * r + d * d) / (2.0 * d)
    h = math.sqrt(max(R * R - x * x, 0.0))
    p = Path()
    p.M(cx + x, cy - h)
    p.A(R, R, 0, 1, 0, cx + x, cy + h)
    p.A(r, r, 0, 1, 1, cx + x, cy - h)
    p.Z()
    if rot:
        with s.rotate(rot, cx, cy):
            return s.path(p, fill=fill, stroke=stroke, sw=sw, join=JOIN_ROUND)
    return s.path(p, fill=fill, stroke=stroke, sw=sw, join=JOIN_ROUND)


def emblem_10():  # Bahia Lunar
    c = "#4B5BA8"
    s = Scene(100, 100)
    medal_begin(s, c, "#232763", "#5C66B8")
    for x, y, r in ((27, 29, 1.7), (40, 22, 1.2), (74, 46, 1.3), (22, 46, 1.1), (50, 33, 1.0)):
        s.sparkle(x, y, r * 1.7, fill="#FFF4D0DD")
    s.circle(63, 35, 17, fill="#FFF4C233")
    s.circle(63, 35, 13, fill="#FFF4C240")
    crescent(s, 62, 35, 10.5, 8.6, 5.6, rad(56, 28, 18, [(0, "#FFFBE0"), (1, "#F5D77E")]), rot=-20)
    seg_ground(s, [(15, 62), (35, 61), (50, 61.5), (65, 61), (85, 62)], fill=lin(0, 60, 0, 86, [(0, "#4A59A6"), (1, "#222C6B")]))
    for y, w in ((65, 9), (69, 12), (73.5, 8), (78, 5)):
        s.rect(63 - w / 2, y, w, 1.3, r=.65, fill="#FFE9A8" + "99")
    # velero
    s.path("M 29.5 61 L 56.5 61 Q 53.5 68.5 48 68.5 L 38 68.5 Q 32.5 68.5 29.5 61 Z",
           fill=lin(0, 61, 0, 69, [(0, "#F0717F"), (1, "#B63B48")]), stroke=ol("#E5576B", .45), sw=.7, join=JOIN_ROUND)
    s.rect(30.5, 62.3, 25, 1.2, r=.6, fill="#FFFFFFCC")
    s.rect(42.3, 36, 1.5, 25.5, r=.5, fill="#8A5A33")
    s.path("M 44.4 38 C 50 45, 55 52, 56.5 59.5 L 44.4 59.5 Z", fill=lin(44, 38, 56, 60, [(0, "#FFFAE8"), (1, "#E6DFF2")]), stroke="#B8A8D866", sw=.5)
    s.path("M 41.6 42.5 C 38 49, 34.5 54, 31.2 59.5 L 41.6 59.5 Z", fill=lin(31, 42, 42, 60, [(0, "#F1ECFA"), (1, "#C7BFE6")]), stroke="#B8A8D866", sw=.5)
    s.poly([(43, 34.5), (49, 36.5), (43, 38.5)], fill="#E5576B", join=JOIN_ROUND)
    for pts in ([(20, 71), (28, 69.6), (36, 71.4)], [(24, 77), (34, 75.6), (44, 77.4)], [(68, 71), (76, 69.8), (82, 71)]):
        s.curve(pts, "#FFFFFF66", 1.1)
    medal_end(s, c)
    return s.bake()


def emblem_11():  # Templo de Bambu
    c = "#3E9E7A"
    s = Scene(100, 100)
    medal_begin(s, c, "#DAF0E5", "#F7F3DB")
    bloom_sun(s, 70, 28, 4.4)
    seg_ground(s, [(15, 67), (35, 65), (50, 66), (65, 65), (85, 67)], fill=lin(0, 64, 0, 86, [(0, "#9CCB86"), (1, "#5E9E6E")]))
    bam = "#4DB38A"
    for x, y0, y1 in ((23.5, 40, 74), (31, 27, 73), (69, 27, 73), (76.5, 40, 74)):
        s.rect(x - 2.2, y0, 4.4, y1 - y0, r=2.1, fill=gh(bam, x - 2.3, x + 2.3, .38, .25), stroke=ol(bam, .45), sw=.6)
        nodes = Path()
        yy = y0 + 9
        while yy < y1 - 3:
            nodes.M(x - 2.3, yy).L(x + 2.3, yy)
            yy += 9
        s.path(nodes, stroke=AL(dk(bam, .55), .65), sw=.9)
        lf = Path().M(x, y0 + 3).Q(x + 6, y0 - 1, x + 9, y0 + 5).Q(x + 4, y0 + 5, x, y0 + 3).Z()
        s.path(lf, fill=gd("#5CC595", x, y0 - 2, 9, 8, .3, .2))
        lf2 = Path().M(x, y0 + 8).Q(x - 6, y0 + 3, x - 8.5, y0 + 9).Q(x - 4, y0 + 10, x, y0 + 8).Z()
        s.path(lf2, fill=gd("#4DB38A", x - 9, y0 + 3, 9, 8, .25, .2))
    # pagoda
    wall = "#FFF4DC"
    roofc = "#B5463A"
    s.rect(42, 68, 16, 2.2, r=.8, fill=gv("#B9B2A6", 68, 70.5, .3, .25))
    tiers = ((57, 67.5, 18, 10, 28, 7.5), (44.5, 54.5, 14, 8.6, 21, 6.6), (33, 42.5, 10, 7.4, 15, 6))
    for (wy, wb, ww, wh, rw, rh) in tiers:
        s.rect(50 - ww / 2, wb - wh, ww, wh, r=.8, fill=gh(wall, 50 - ww / 2, 50 + ww / 2, .2, .22), stroke=ol("#B98456", .35), sw=.6)
    s.rect(47.4, 59.6, 5.2, 8, r=2.4, fill="#7A4A2A")
    s.rect(47.6, 46.6, 4.8, 4.2, r=.9, fill=lin(0, 46, 0, 51, [(0, "#FFE28A"), (1, "#E0A93B")]))
    s.circle(50, 38, 1.9, fill="#7A4A2A")
    for (wy, wb, ww, wh, rw, rh) in tiers:
        pagoda_roof(s, 50, wb - wh, rw, rh, roofc, ov=3.6, sw=.7)
    s.line(50, 25.5, 50, 20, "#D9A21B", 1.1)
    s.circle(50, 19.6, 1.9, fill=rad(49.5, 19, 3, [(0, "#FFF0B0"), (1, "#E0A93B")]))
    medal_end(s, c)
    return s.bake()


EMBLEMS = [emblem_0, emblem_1, emblem_2, emblem_3, emblem_4, emblem_5, emblem_6, emblem_7, emblem_8, emblem_9, emblem_10, emblem_11]


# ================================================================== MONUMENTOS (160 x 120)
# (los monumentos se anaden mas abajo)

LANDMARKS = {}


def build():
    icons = {}
    for i, fn in enumerate(EMBLEMS):
        icons["chapter.d%d" % i] = fn()
    for name, fn in LANDMARKS.items():
        icons["landmark." + name] = fn()
    return {"icons": icons}
