"""Adornos del borde del camino del mapa (parte B): cerezo, dunas doradas, luna, bambu, cementerio,
bosque muerto, calabazas, casa embrujada y props genericos (nubes, arbustos, flores, rocas...)."""
import os, sys, math
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, ".."))   # artlib
sys.path.insert(0, HERE)
from artlib import *
from artlib import _ellipse_cmds, _rrect_cmds

INK = "#2B1B3A"


# ------------------------------------------------------------------ utilidades de color

def _rgb(c):
    c = c.lstrip("#")
    return [int(c[i:i + 2], 16) for i in (0, 2, 4)]


def mx(a, b, t):
    ca, cb = _rgb(a), _rgb(b)
    return "#%02X%02X%02X" % tuple(int(round(ca[i] + (cb[i] - ca[i]) * t)) for i in range(3))


def L(c, t=.35):
    return mx(c, "#FFFFFF", t)


def D(c, t=.2):
    return mx(c, INK, t)


def OL(c, t=.5):
    return mx(c, INK, t)


def A(c, a):
    return c[:7] + "%02X" % int(round(a * 255))


def G(c, x1, y1, x2, y2, a=.35, b=.2):
    return lin(x1, y1, x2, y2, [(0, L(c, a)), (1, D(c, b))])


def R(c, cx, cy, r, a=.4, b=.2):
    return rad(cx, cy, r, [(0, L(c, a)), (1, D(c, b))])


def glow(s, cx, cy, r, c, a=.5):
    s.circle(cx, cy, r, fill=rad(cx, cy, r, [(0, A(c, a)), (.5, A(c, a * .38)), (1, A(c, 0))]))


def shadow(s, cx=50, y=90, rx=26, ry=4.2, col="#00000026"):
    s.ellipse(cx, y, rx, ry, fill=col)


def shine(s, x, y, rx, ry, rot=-30, a=.55):
    with s.rotate(rot, x, y):
        s.ellipse(x, y, rx, ry, fill="#FFFFFF%02X" % int(a * 255))


# ------------------------------------------------------------------ geometria como lista de comandos

def circ(cs):
    out = []
    for c in cs:
        if len(c) == 3:
            out += _ellipse_cmds(c[0], c[1], c[2], c[2])
        else:
            out += _ellipse_cmds(c[0], c[1], c[2], c[3])
    return out


def rr(x, y, w, h, r):
    return _rrect_cmds(x, y, w, h, r)


def leaf(bx, by, ang, ln, wd):
    """Hoja en forma de lente: base en (bx,by), direccion ang (grados, 0 = derecha, 90 = abajo)."""
    a = math.radians(ang)
    ux, uy = math.cos(a), math.sin(a)
    px, py = -uy, ux
    tip = (bx + ux * ln, by + uy * ln)

    def pt(t, w):
        return (bx + ux * ln * t + px * wd * w, by + uy * ln * t + py * wd * w)
    c1, c2 = pt(.28, 1), pt(.78, .8)
    d1, d2 = pt(.28, -1), pt(.78, -.8)
    return [("M", bx, by), ("C", c1[0], c1[1], c2[0], c2[1], tip[0], tip[1]),
            ("C", d2[0], d2[1], d1[0], d1[1], bx, by), ("Z",)]


def flower5(cx, cy, r, n=5, rot=-90, pr=.52):
    out = []
    for i in range(n):
        a = math.radians(rot + i * 360.0 / n)
        d = r * (1 - pr)
        out += _ellipse_cmds(cx + d * math.cos(a), cy + d * math.sin(a), r * pr, r * pr)
    return out


def sparkle_c(cx, cy, r, sq=.2):
    k = r * sq
    return Path().M(cx, cy - r).Q(cx + k, cy - k, cx + r, cy).Q(cx + k, cy + k, cx, cy + r) \
        .Q(cx - k, cy + k, cx - r, cy).Q(cx - k, cy - k, cx, cy - r).Z().cmds


def drop(cx, cy, w, h):
    """Gota / llama con la punta arriba; base redonda apoyada en y = cy."""
    return Path().M(cx, cy - h).C(cx + w * .15, cy - h * .6, cx + w, cy - w * 2.3, cx + w, cy - w) \
        .A(w, w, 0, 0, 1, cx - w, cy - w).C(cx - w, cy - w * 2.3, cx - w * .15, cy - h * .6, cx, cy - h).Z().cmds


def blades(items):
    """items: (x, y_base, inclinacion, alto). Briznas de hierba como triangulos finos."""
    out = []
    for (x, y, lean, h) in items:
        out += [("M", x - 1.9, y), ("Q", x - .4, y - h * .55, x + lean, y - h), ("Q", x + .9, y - h * .5, x + 1.9, y), ("Z",)]
    return out


def crescent(cx, cy, Ro, ri, dx):
    xi = (dx * dx + Ro * Ro - ri * ri) / (2.0 * dx)
    yi = math.sqrt(max(0.0, Ro * Ro - xi * xi))
    p1 = (cx + xi, cy - yi)
    p2 = (cx + xi, cy + yi)
    return Path().M(p1[0], p1[1]).A(Ro, Ro, 0, 1, 0, p2[0], p2[1]).A(ri, ri, 0, 1, 1, p1[0], p1[1]).Z().cmds


def bat(cx, cy, w):
    return Path().M(cx - w, cy - w * .3).Q(cx - w * .55, cy - w * .6, cx - w * .14, cy - w * .12) \
        .L(cx - w * .12, cy - w * .42).L(cx, cy - w * .2).L(cx + w * .12, cy - w * .42).L(cx + w * .14, cy - w * .12) \
        .Q(cx + w * .55, cy - w * .6, cx + w, cy - w * .3) \
        .Q(cx + w * .8, cy + w * .1, cx + w * .56, cy + w * .2).Q(cx + w * .4, cy - w * .02, cx + w * .24, cy + w * .26) \
        .L(cx, cy + w * .42).L(cx - w * .24, cy + w * .26).Q(cx - w * .4, cy - w * .02, cx - w * .56, cy + w * .2) \
        .Q(cx - w * .8, cy + w * .1, cx - w, cy - w * .3).Z().cmds


def arch(cx, top, w, h):
    r = w / 2.0
    return Path().M(cx - r, top + h).L(cx - r, top + r).A(r, r, 0, 0, 1, cx + r, top + r).L(cx + r, top + h).Z().cmds


def mass(s, cmds, fill, ol=None, ow=1.5):
    """Masa de color unida (varios subcaminos) con contorno fino opcional."""
    if ol:
        s.path(cmds, fill=ol, stroke=ol, sw=ow * 2, join=JOIN_ROUND)
    return s.path(cmds, fill=fill)


def course_lines(apex, a, b, ts):
    out = []
    for t in ts:
        p = (apex[0] + (a[0] - apex[0]) * t, apex[1] + (a[1] - apex[1]) * t)
        q = (apex[0] + (b[0] - apex[0]) * t, apex[1] + (b[1] - apex[1]) * t)
        out += [("M", p[0], p[1]), ("L", q[0], q[1])]
    return out


# ------------------------------------------------------------------ colores del paquete

BARK = "#8A5F4E"
PINK = "#F4A6C0"
PINK_D = "#D77C9F"
SAND = "#EBB851"
SAND_D = "#C98A2B"
LEAF = "#6FB57A"
NIGHT_STONE = "#9A94B8"
PURPLE = "#A874FF"
ORANGE = "#FFA23A"
WOOD = "#C98F5B"


# ================================================================== CEREZO

def cherry_a():
    s = Scene()
    shadow(s, 50, 90, 27)
    s.path("M 42 91 C 47 82 48 70 46 58 C 45 52 46 47 49 43 L 57 44 C 58 52 56 60 56 68 C 56 78 59 85 64 91 Z",
           fill=G(BARK, 40, 45, 62, 90, .28, .28))
    s.curve([(48, 64), (40, 57), (31, 51)], stroke=D(BARK, .15), sw=3.4)
    s.curve([(55, 62), (63, 55), (72, 50)], stroke=D(BARK, .15), sw=3.2)
    can = circ([(50, 33, 19), (30, 45, 14), (70, 44, 15), (39, 23, 13), (61, 22, 13), (50, 48, 14)])
    with s.translate(1.2, 2.8):
        s.path(can, fill=PINK_D)
    s.path(can, fill=lin(26, 10, 76, 60, [(0, L(PINK, .6)), (.5, PINK), (1, mx(PINK, "#C85A8A", .5))]))
    shine(s, 38, 20, 10, 4.6, -28, .6)
    shine(s, 24, 38, 4, 2.2, -50, .45)
    fl = [(34, 38, 5.4), (56, 28, 5.8), (67, 46, 5), (45, 50, 4.8), (24, 48, 4.2), (72, 34, 4.2), (46, 17, 4.4)]
    s.path(sum([flower5(x, y, r) for x, y, r in fl], []), fill="#FFF3F7")
    s.path(circ([(x, y, 1.3) for x, y, r in fl]), fill="#E26C95")
    s.path(sum([leaf(x, y, a, 6, 2.6) for x, y, a in [(21, 72, 20), (79, 80, -30), (68, 68, 60)]], []), fill="#F8B8CC")
    return s.bake()


def cherry_b():
    s = Scene()
    shadow(s, 50, 90, 24)
    s.path("M 46 91 C 50 82 52 72 49 63 C 47 56 44 50 43 44 L 52 42 C 54 49 56 55 57 62 C 59 72 57 82 61 91 Z",
           fill=G(BARK, 42, 44, 62, 90, .28, .28))
    strands = [[(24, 44), (21, 56), (24, 69)], [(33, 50), (31, 62), (34, 75)], [(67, 50), (69, 62), (66, 73)],
               [(76, 44), (79, 56), (76, 67)], [(58, 52), (60, 60), (58, 66)]]
    cm = []
    for st in strands:
        cm += catmull(st)
    s.path(cm, stroke=D(PINK_D, .1), sw=1.7)
    dots = [(p[0], p[1], 2.7) for st in strands for p in st[1:]]
    s.path(circ(dots), fill=PINK)
    can = circ([(30, 36, 12), (44, 26, 15), (60, 27, 15), (72, 37, 12), (50, 40, 14), (36, 42, 11), (64, 42, 11), (52, 32, 14)])
    with s.translate(1.2, 2.8):
        s.path(can, fill=PINK_D)
    s.path(can, fill=lin(22, 10, 80, 56, [(0, L(PINK, .6)), (.5, PINK), (1, mx(PINK, "#C85A8A", .5))]))
    shine(s, 38, 21, 10, 4.4, -22, .6)
    fl = [(30, 34, 5), (50, 22, 5.4), (66, 32, 5.2), (46, 42, 4.6), (74, 40, 4)]
    s.path(sum([flower5(x, y, r) for x, y, r in fl], []), fill="#FFF3F7")
    s.path(circ([(x, y, 1.2) for x, y, r in fl]), fill="#E26C95")
    s.path(sum([leaf(x, y, a, 6, 2.6) for x, y, a in [(30, 80, 160), (74, 84, -20)]], []), fill="#F8B8CC")
    return s.bake()


def cherry_c():
    s = Scene()
    shadow(s, 50, 90, 36)
    s.curve([(44, 90), (46, 82), (45, 74)], stroke=BARK, sw=3.4)
    s.curve([(56, 90), (55, 82), (57, 74)], stroke=BARK, sw=3.4)
    bush = circ([(28, 74, 13), (72, 74, 13), (50, 62, 19), (39, 77, 12.5), (61, 77, 12.5), (50, 78, 13)])
    with s.translate(1, 2.4):
        s.path(bush, fill=PINK_D)
    s.path(bush, fill=lin(24, 44, 76, 90, [(0, L(PINK, .6)), (.5, PINK), (1, mx(PINK, "#C85A8A", .45))]))
    shine(s, 38, 52, 9, 4, -25, .6)
    fl = [(34, 62, 5.4), (56, 54, 5.6), (66, 70, 5.2), (46, 74, 5), (24, 76, 4.4), (78, 78, 4.2), (50, 86, 4)]
    s.path(sum([flower5(x, y, r) for x, y, r in fl], []), fill="#FFF3F7")
    s.path(circ([(x, y, 1.3) for x, y, r in fl]), fill="#E26C95")
    s.path(sum([leaf(x, y, a, 5.5, 2.4) for x, y, a in [(12, 90, 10), (88, 91, 190), (18, 66, -150), (84, 64, -30)]], []), fill="#F8B8CC")
    return s.bake()


# ================================================================== DUNAS DORADAS

def gold_a():
    s = Scene()
    glow(s, 66, 42, 32, "#FFD36E", .55)
    s.circle(66, 42, 13.5, fill=R("#FFD76A", 61, 37, 18, .6, .08))
    shadow(s, 50, 91, 42, 3.4, "#8A5A1F22")
    s.path("M 46 91 C 58 74 76 66 94 76 L 94 91 Z", fill=G(SAND, 50, 66, 90, 91, .4, .15))
    main = "M 6 91 C 18 89 28 62 46 52 C 57 46 63 54 71 67 C 79 79 88 88 94 91 Z"
    s.path(main, fill=G(SAND, 12, 52, 70, 91, .45, .15))
    s.path("M 46 52 C 57 46 63 54 71 67 C 79 79 88 88 94 91 L 60 91 C 62 76 55 62 46 52 Z",
           fill=lin(48, 52, 80, 91, [(0, mx(SAND, SAND_D, .6)), (1, mx(SAND_D, INK, .25))]))
    s.path([("M", 16, 84), ("Q", 24, 77, 30, 71), ("M", 24, 87), ("Q", 34, 78, 40, 66), ("M", 66, 86), ("Q", 74, 82, 82, 84)],
           stroke="#FFFFFF66", sw=1.5)
    s.path(sum([leaf(x, y, a, 6, 1.6) for x, y, a in [(80, 66, 0), (86, 72, 20)]], []), fill="#FFFFFF00")
    shine(s, 36, 62, 7, 2.6, -48, .5)
    return s.bake()


def gold_b():
    s = Scene()
    shadow(s, 50, 91, 42, 3.8, "#8A5A1F26")
    apex2, l2, f2, r2 = (78, 56), (66, 82), (80, 88), (92, 80)
    s.poly([apex2, l2, f2], fill=G(SAND, 66, 56, 80, 88, .5, .05))
    s.poly([apex2, f2, r2], fill=lin(78, 56, 92, 88, [(0, mx(SAND, SAND_D, .5)), (1, mx(SAND_D, INK, .3))]))
    apex, lf, fr, rt = (42, 22), (9, 83), (46, 92), (84, 80)
    s.poly([apex, lf, fr], fill=G(SAND, 12, 30, 44, 92, .5, .08))
    s.poly([apex, fr, rt], fill=lin(46, 22, 80, 92, [(0, mx(SAND, SAND_D, .55)), (1, mx(SAND_D, INK, .32))]))
    s.path(course_lines(apex, lf, fr, [.26, .5, .76]), stroke=A(SAND_D, .5), sw=.9)
    s.path(course_lines(apex, fr, rt, [.26, .5, .76]), stroke=A("#5A3410", .35), sw=.9)
    s.poly([apex, (35, 36), (42, 38), (49, 36)], fill="#FFE9A8")
    s.poly([(41, 76), (41, 91), (50, 92), (50, 78)], fill=A("#3A2410", .0))
    s.path(arch(47, 77, 9, 14), fill="#4A2F18")
    return s.bake()


def gold_c():
    s = Scene()
    shadow(s, 50, 91, 32, 4, "#8A5A1F26")
    s.ellipse(50, 88, 30, 6.5, fill=G(SAND, 24, 80, 70, 95, .45, .12))
    cac = rr(41, 24, 18, 66, 9) + rr(24, 42, 9.5, 26, 4.7) + rr(24, 58, 22, 9.5, 4.7) \
        + rr(67, 32, 9.5, 28, 4.7) + rr(54, 51, 22, 9.5, 4.7)
    cg = "#6CB380"
    mass(s, cac, lin(24, 0, 76, 0, [(0, L(cg, .45)), (.45, cg), (1, D(cg, .35))]), ol=OL(cg, .55), ow=1.3)
    s.path([("M", 47, 30), ("L", 47, 86), ("M", 53, 30), ("L", 53, 86), ("M", 28.7, 46), ("L", 28.7, 62), ("M", 71.7, 36), ("L", 71.7, 56)],
           stroke=A(D(cg, .5), .28), sw=1.2)
    s.path(circ([(44, 40, .9), (56, 48, .9), (46, 62, .9), (55, 72, .9), (44, 78, .9), (72, 40, .8), (28.7, 50, .8)]), fill="#FFF5D6")
    s.path(flower5(50, 22, 6), fill=lin(44, 16, 56, 28, [(0, "#FFC0D4"), (1, "#F27FA6")]))
    s.circle(50, 22, 2, fill="#FFD36E")
    shine(s, 44, 36, 1.6, 8, 0, .5)
    return s.bake()


# ================================================================== LUNA

def moon_a():
    s = Scene()
    glow(s, 50, 46, 46, "#FFE9A0", .6)
    s.ellipse(50, 90, 30, 4, fill="#FFE9A044")
    s.circle(50, 46, 25, fill=R("#FFF3BE", 42, 36, 36, .55, .0).__class__(R("#FFF3BE", 42, 36, 36, .55, .0)) if False else
             rad(42, 36, 38, [(0, "#FFFBE0"), (.55, "#FFEFA8"), (1, "#E8C95F")]), stroke="#D9B64E", sw=1.2)
    s.path(circ([(58, 53, 6, 5.2), (40, 58, 4.2, 3.6), (61, 36, 3, 2.6), (47, 66, 2.6, 2.2)]), fill="#E3C970AA")
    shine(s, 39, 33, 8, 4, -35, .6)
    s.path(sparkle_c(18, 24, 7) + sparkle_c(82, 30, 6) + sparkle_c(78, 70, 4.6) + sparkle_c(24, 72, 3.8), fill="#FFD86B")
    return s.bake()


def moon_b():
    s = Scene()
    glow(s, 48, 42, 42, "#FFE9A0", .55)
    cres = crescent(46, 40, 25, 21, 11)
    with s.rotate(-22, 46, 40):
        s.path(cres, fill=lin(26, 18, 62, 62, [(0, "#FFFBE0"), (.5, "#FFEFA8"), (1, "#E3C255")]), stroke="#D9B64E", sw=1.2, join=JOIN_ROUND)
    cl = circ([(32, 77, 10), (46, 70, 13), (62, 74, 11), (72, 80, 8)]) + rr(20, 78, 62, 12, 6)
    mass(s, cl, lin(0, 58, 0, 91, [(0, "#FFFFFF"), (.55, "#EEF0FF"), (1, "#C4CAEE")]), ol="#AEB6E3", ow=1.1)
    s.path(sparkle_c(78, 20, 6) + sparkle_c(18, 30, 4.5) + sparkle_c(84, 50, 3.6), fill="#FFD86B")
    shine(s, 40, 68, 7, 2.6, -15, .75)
    return s.bake()


def moon_c():
    s = Scene()
    glow(s, 28, 30, 30, "#FFE9A0", .55)
    s.circle(28, 30, 13, fill=rad(24, 26, 20, [(0, "#FFFBE0"), (.6, "#FFEFA8"), (1, "#E8C95F")]), stroke="#D9B64E", sw=1)
    glow(s, 50, 76, 26, ORANGE, .22)
    s.path("M 6 85 Q 16 79 26 85 T 46 85 T 66 85 T 86 85 L 94 85 L 94 92 L 6 92 Z", fill=lin(0, 80, 0, 92, [(0, "#7C7FD6"), (1, "#4B4F9E")]))
    s.path("M 22 70 L 78 70 C 76 79 70 85 60 85 L 40 85 C 30 85 24 79 22 70 Z", fill=G("#7A5670", 22, 70, 78, 86, .25, .35),
           stroke=OL("#7A5670"), sw=1.2, join=JOIN_ROUND)
    s.path("M 23 74 L 77 74", stroke=A("#FFD7A0", .5), sw=1.1)
    s.rect(49, 24, 3, 47, r=1.5, fill="#4A3358")
    s.path("M 51 27 C 68 36 70 52 67 64 L 63 61 L 59 65 L 55 61 L 51 64 Z", fill=lin(51, 27, 68, 65, [(0, "#FFF8DE"), (1, "#DCCFB4")]))
    s.path("M 49 34 C 38 42 35 54 37 63 L 41 60 L 45 64 L 49 61 Z", fill=lin(37, 34, 49, 64, [(0, "#FFF2D0"), (1, "#D6C7AC")]))
    s.poly([(52, 24), (62, 27.5), (52, 31)], fill="#9C6BE8")
    s.circle(72, 69, 3.4, fill="#FFC857")
    s.path("M 8 87 Q 18 82 28 87 T 48 87 T 68 87 T 88 87", stroke="#FFFFFF77", sw=1.4)
    return s.bake()


# ================================================================== BAMBU

BG1, BG2, BG3 = "#86C77F", "#72B572", "#63A56A"


def stalk(s, x, top, c):
    s.rect(x - 4.5, top, 9, 91 - top, r=4.5, fill=lin(x - 4.5, 0, x + 4.5, 0, [(0, L(c, .45)), (.5, c), (1, D(c, .32))]))


def bamboo_a():
    s = Scene()
    shadow(s, 50, 90, 28)
    st = [(36, 24, BG1), (50, 12, BG2), (64, 30, BG3)]
    for x, top, c in st:
        stalk(s, x, top, c)
    nodes = []
    for x, top, c in st:
        y = top + 18
        while y < 86:
            nodes += rr(x - 5.2, y - 1.3, 10.4, 2.6, 1.3)
            y += 19
    s.path(nodes, fill="#3F7D4C")
    lv1, lv2 = [], []
    for x, top, c in st:
        lv1 += leaf(x, top + 18, -150, 18, 4.2) + leaf(x, top + 36, -25, 17, 4)
        lv2 += leaf(x, top + 4, -35, 15, 3.6) + leaf(x, top + 18, -20, 16, 3.8) + leaf(x, top + 36, -158, 15, 3.6)
    s.path(lv2, fill=lin(14, 10, 86, 60, [(0, "#5DA56B"), (1, "#3F8458")]))
    s.path(lv1, fill=lin(14, 10, 86, 60, [(0, "#9BD98D"), (1, "#5FB070")]))
    s.path(blades([(26, 90, -3, 9), (30, 90, 2, 6), (44, 90, -2, 7), (58, 90, 3, 7), (72, 90, -3, 9), (76, 90, 3, 6)]), fill="#5AA56A")
    return s.bake()


def bamboo_b():
    s = Scene()
    shadow(s, 50, 91, 34)
    s.ellipse(50, 89, 36, 7, fill=G("#9A7458", 20, 80, 80, 96, .2, .35))
    # tallos jovenes detras
    for x, top, c in [(24, 38, BG1), (78, 46, BG3)]:
        s.rect(x - 3.2, top, 6.4, 90 - top, r=3.2, fill=lin(x - 3.2, 0, x + 3.2, 0, [(0, L(c, .45)), (.5, c), (1, D(c, .32))]))
    s.path(sum([leaf(24, 44, -140, 14, 3.6), leaf(24, 52, -30, 13, 3.2), leaf(78, 52, -35, 13, 3.4), leaf(78, 60, -150, 12, 3)], []),
           fill=lin(10, 30, 90, 60, [(0, "#9BD98D"), (1, "#5FB070")]))
    sh = [(38, 88, 8.5, 48), (58, 88, 10, 36), (50, 88, 7, 24)]
    for (x, base, w, h) in sh:
        d = "M %s %s C %s %s %s %s %s %s C %s %s %s %s %s %s Z" % (
            x - w, base, x - w - 1, base - h * .45, x - w * .45, base - h * .85, x, base - h,
            x + w * .45, base - h * .85, x + w + 1, base - h * .45, x + w, base)
        s.path(d, fill=lin(x - w, base - h, x + w, base, [(0, "#E1E48D"), (.5, "#B7CE78"), (1, "#8DA55E")]),
               stroke=OL("#8DA55E", .5), sw=1.2, join=JOIN_ROUND)
    br = []
    for (x, base, w, h) in sh:
        for k in (.28, .5, .7):
            yy = base - h * k
            ww = w * (1 - k * .55)
            br += [("M", x - ww, yy + 3), ("L", x, yy - 2), ("L", x + ww, yy + 3)]
    s.path(br, stroke=A("#6D5530", .55), sw=1.1)
    s.path(blades([(24, 90, -3, 8), (68, 90, 3, 8), (82, 90, -2, 7), (12, 90, 2, 6)]), fill="#5AA56A")
    return s.bake()


def bamboo_c():
    s = Scene()
    ST = "#B8B2CC"
    shadow(s, 46, 91, 28)
    stalk(s, 76, 30, BG2)
    s.path([("M", 71, 55), ("L", 81, 55), ("M", 71, 71), ("L", 81, 71)], stroke="#3F7D4C", sw=2)
    s.path(sum([leaf(76, 36, -150, 16, 3.8), leaf(76, 44, -30, 15, 3.6), leaf(76, 58, -155, 14, 3.4)], []),
           fill=lin(60, 30, 92, 60, [(0, "#9BD98D"), (1, "#5FB070")]))
    glow(s, 42, 52, 24, ORANGE, .3)
    ol = OL(ST, .5)
    s.rect(24, 80, 36, 10, r=2.5, fill=G(ST, 24, 80, 60, 90, .35, .3), stroke=ol, sw=1.2)
    s.rect(34, 62, 16, 19, r=2.5, fill=G(ST, 34, 62, 50, 81, .35, .3), stroke=ol, sw=1.2)
    s.rect(24, 56, 36, 8, r=2.5, fill=G(ST, 24, 56, 60, 64, .4, .25), stroke=ol, sw=1.2)
    s.rect(29, 38, 26, 19, r=3, fill=G(ST, 29, 38, 55, 57, .35, .3), stroke=ol, sw=1.2)
    s.rect(34, 42, 16, 11, r=2, fill=lin(34, 42, 50, 53, [(0, "#FFE9A0"), (1, "#FFA23A")]))
    s.path("M 20 40 C 30 40 36 30 42 22 C 48 30 54 40 64 40 C 54 44 30 44 20 40 Z", fill=G("#8E88AB", 20, 22, 64, 44, .3, .35), stroke=ol, sw=1.2, join=JOIN_ROUND)
    s.circle(42, 20, 3.4, fill=G(ST, 39, 17, 45, 23, .4, .2), stroke=ol, sw=1)
    s.path(blades([(26, 90, -3, 6), (60, 90, 3, 7)]), fill="#5AA56A")
    return s.bake()


# ================================================================== CEMENTERIO

def tomb(s, x, base, w, h, c=NIGHT_STONE):
    r = w / 2.0
    d = "M %s %s L %s %s A %s %s 0 0 1 %s %s L %s %s Z" % (x - r, base, x - r, base - h + r, r, r, x + r, base - h + r, x + r, base)
    s.path(d, fill=G(c, x - r, base - h, x + r, base, .4, .3), stroke=OL(c), sw=1.4, join=JOIN_ROUND)


def graveyard_a():
    s = Scene()
    glow(s, 50, 56, 44, PURPLE, .42)
    shadow(s, 50, 91, 30, 4, "#1B103038")
    s.rect(26, 83, 48, 8, r=3, fill=G("#7E789E", 26, 83, 74, 91, .3, .3), stroke=OL("#7E789E"), sw=1.2)
    tomb(s, 50, 85, 38, 56)
    s.arc_stroke(50, 50, 14.5, 195, 252, "#FFFFFF99", 2)
    s.path([("M", 50, 46), ("L", 50, 69), ("M", 42, 54), ("L", 58, 54)], stroke=A(D(NIGHT_STONE, .6), .55), sw=3.4)
    s.path("M 33 78 C 36 70 42 72 44 78 C 46 84 38 86 33 84 Z", fill=A("#5E8F68", .8))
    s.path(blades([(22, 90, -4, 9), (26, 90, 1, 7), (76, 90, 4, 9), (80, 90, -1, 7), (72, 90, 2, 6)]), fill="#4F7A60")
    glow(s, 78, 40, 11, PURPLE, .6)
    s.path(drop(78, 48, 3.4, 12), fill="#E6D6FF")
    s.path(drop(22, 38, 2.4, 8), fill="#E6D6FF")
    return s.bake()


def graveyard_b():
    s = Scene()
    glow(s, 50, 58, 42, PURPLE, .38)
    shadow(s, 50, 91, 32, 4, "#1B103038")
    s.ellipse(50, 88, 31, 6.5, fill=G("#6B4E5E", 20, 80, 80, 96, .15, .45))
    wd = "#8A6A58"
    with s.rotate(7, 50, 88):
        s.rect(45, 30, 10, 60, r=2, fill=lin(45, 0, 55, 0, [(0, L(wd, .25)), (1, D(wd, .35))]), stroke=OL(wd), sw=1.2)
        s.rect(30, 44, 40, 9, r=2, fill=lin(0, 44, 0, 53, [(0, L(wd, .25)), (1, D(wd, .35))]), stroke=OL(wd), sw=1.2)
        s.path([("M", 47, 36), ("L", 47, 60), ("M", 36, 48.5), ("L", 62, 48.5)], stroke="#FFFFFF2E", sw=1)
    s.path(circ([(30, 85, 5, 3), (70, 85, 6, 3.5)]), fill="#4A3358")
    s.path(blades([(26, 90, -4, 9), (31, 90, 1, 6), (72, 90, 4, 9), (77, 90, -2, 7)]), fill="#4F7A60")
    s.ellipse(48, 84, 36, 5, fill="#CDBBFF3A")
    glow(s, 22, 44, 11, PURPLE, .6)
    s.path(drop(22, 51, 3, 10), fill="#E6D6FF")
    return s.bake()


def graveyard_c():
    s = Scene()
    glow(s, 50, 60, 44, ORANGE, .3)
    shadow(s, 50, 91, 34, 4, "#1B103038")
    tomb(s, 36, 88, 28, 54)
    tomb(s, 66, 88, 30, 38, c="#8B85AE")
    s.path([("M", 34, 52), ("L", 38, 58), ("L", 35, 64), ("M", 66, 66), ("L", 63, 72)], stroke=A(D(NIGHT_STONE, .6), .6), sw=1.3)
    s.arc_stroke(36, 50, 10, 195, 252, "#FFFFFF88", 1.8)
    glow(s, 52, 66, 20, "#FFC24A", .6)
    s.rect(46, 72, 11, 16, r=2.4, fill=lin(46, 0, 57, 0, [(0, "#FFF7DE"), (1, "#E8D8B2")]), stroke=OL("#E8D8B2"), sw=1)
    s.path(drop(51.5, 71, 3.5, 11), fill=lin(48, 60, 55, 72, [(0, "#FFE066"), (1, "#FF8A2E")]))
    s.path(drop(51.5, 70.5, 1.5, 5), fill="#FFF6C4")
    s.path(blades([(18, 90, -4, 9), (23, 90, 1, 6), (82, 90, 3, 9), (77, 90, -1, 6), (56, 90, 3, 5)]), fill="#4F7A60")
    return s.bake()


# ================================================================== BOSQUE MUERTO

BK = "#6A5078"


def dead_tree_a():
    s = Scene()
    glow(s, 50, 52, 46, PURPLE, .30)
    glow(s, 50, 58, 22, ORANGE, .38)
    shadow(s, 50, 91, 28, 4, "#1B103038")
    br = []
    for pts in [[(47, 44), (38, 34), (26, 24)], [(53, 40), (62, 28), (74, 17)], [(45, 58), (34, 53), (21, 55)], [(56, 54), (68, 48), (81, 50)]]:
        br += catmull(pts)
    s.path(br, stroke=BK, sw=3.8)
    tw = [("M", 38, 34), ("L", 33, 24), ("M", 33, 30), ("L", 22, 33), ("M", 62, 28), ("L", 62, 17), ("M", 68, 24), ("L", 80, 24),
          ("M", 34, 53), ("L", 30, 44), ("M", 68, 48), ("L", 70, 38)]
    s.path(tw, stroke=BK, sw=2.2)
    s.path("M 36 91 C 41 84 43 72 42 60 C 41 50 44 42 47 36 L 54 35 C 58 44 60 52 58 62 C 57 74 59 84 65 91 Z",
           fill=lin(38, 0, 64, 0, [(0, L(BK, .22)), (.55, BK), (1, D(BK, .4))]), stroke=OL(BK, .55), sw=1.3, join=JOIN_ROUND)
    s.path([("M", 46, 40), ("Q", 44, 52, 47, 64), ("M", 56, 46), ("Q", 57, 58, 55, 72)], stroke=A(D(BK, .6), .5), sw=1.2)
    glow(s, 46.5, 57, 7, ORANGE, .6)
    glow(s, 55, 57, 7, ORANGE, .6)
    with s.rotate(18, 46.5, 57):
        s.ellipse(46.5, 57, 3.4, 2.3, fill="#FFD25C")
    with s.rotate(-18, 55, 57):
        s.ellipse(55, 57, 3.4, 2.3, fill="#FFD25C")
    s.poly([(45, 67), (48, 65), (50.5, 68), (53, 65), (56, 67), (53.5, 71), (47.5, 71)], fill="#2A1A33")
    return s.bake()


def dead_tree_b():
    s = Scene()
    glow(s, 68, 30, 32, ORANGE, .38)
    s.circle(68, 30, 14, fill=rad(63, 25, 22, [(0, "#FFF6D2"), (1, "#F4D88A")]), stroke="#D9B64E", sw=1)
    shadow(s, 48, 91, 27, 4, "#1B103038")
    br = []
    for pts in [[(51, 40), (40, 30), (28, 26)], [(54, 54), (42, 50), (30, 52)], [(57, 44), (68, 40), (80, 41)]]:
        br += catmull(pts)
    s.path(br, stroke=BK, sw=3.4)
    s.path([("M", 40, 30), ("L", 38, 20), ("M", 33, 28), ("L", 24, 33), ("M", 68, 40), ("L", 72, 50), ("M", 76, 41), ("L", 79, 33),
            ("M", 42, 50), ("L", 40, 60)], stroke=BK, sw=2)
    s.path("M 36 91 C 42 84 46 72 46 58 C 46 48 49 38 52 30 L 60 33 C 57 42 56 50 56 60 C 56 72 58 82 65 91 Z",
           fill=lin(38, 0, 64, 0, [(0, L(BK, .22)), (.55, BK), (1, D(BK, .4))]), stroke=OL(BK, .55), sw=1.3, join=JOIN_ROUND)
    s.path([("M", 50, 44), ("Q", 49, 58, 51, 70)], stroke=A(D(BK, .6), .5), sw=1.2)
    cr = "#2F2340"
    with s.rotate(-12, 69, 33):
        s.ellipse(69, 33, 7.4, 4.8, fill=lin(62, 28, 76, 38, [(0, "#5B4A78"), (1, cr)]))
    s.path([("M", 63, 32), ("L", 55, 37), ("L", 64, 36), ("Z",)], fill=cr)
    s.circle(75, 28, 3.7, fill=cr)
    s.poly([(78, 27), (83, 29.5), (78, 30.5)], fill="#E9A23A")
    s.circle(75.6, 27.4, .9, fill="#FFD25C")
    s.path([("M", 67, 37), ("L", 67, 40.5), ("M", 71, 37), ("L", 71, 40.5)], stroke="#E9A23A", sw=1)
    return s.bake()


def dead_tree_c():
    s = Scene()
    glow(s, 50, 56, 46, PURPLE, .30)
    shadow(s, 50, 91, 34, 4, "#1B103038")
    tall = "M 25 91 C 28 80 30 68 29 54 C 28 44 30 34 33 26 L 38 27 C 36 36 35 46 35 56 C 35 70 36 82 40 91 Z"
    short = "M 62 91 C 64 83 66 76 65 68 C 64 62 66 56 68 52 L 73 53 C 72 58 72 64 72 70 C 72 80 74 86 77 91 Z"
    br = []
    for pts in [[(32, 40), (22, 30), (14, 22)], [(34, 34), (44, 24), (52, 20)], [(31, 54), (20, 50), (12, 52)], [(35, 48), (44, 44), (52, 46)],
                [(69, 60), (80, 54), (86, 46)], [(70, 56), (60, 48), (56, 40)]]:
        br += catmull(pts)
    s.path(br, stroke=BK, sw=2.8)
    s.path([("M", 22, 30), ("L", 20, 22), ("M", 44, 24), ("L", 46, 14), ("M", 80, 54), ("L", 88, 56), ("M", 60, 48), ("L", 52, 50)], stroke=BK, sw=1.8)
    for d in (tall, short):
        s.path(d, fill=lin(24, 0, 78, 0, [(0, L(BK, .22)), (.55, BK), (1, D(BK, .4))]), stroke=OL(BK, .55), sw=1.2, join=JOIN_ROUND)
    s.path(circ([(50, 84, 38, 6), (36, 78, 22, 4.4), (70, 80, 20, 4)]), fill="#CDBBFF40")
    glow(s, 50, 72, 8, ORANGE, .55)
    glow(s, 56, 72, 8, ORANGE, .55)
    s.ellipse(48.5, 72, 2.3, 3, fill="#FFD25C")
    s.ellipse(55, 72, 2.3, 3, fill="#FFD25C")
    return s.bake()


# ================================================================== CALABAZAS

PUMP = "#F28A2E"
PUMP_D = "#C4571A"


def pumpkin(s, cx, cy, w, h, face=False, glow_a=0.0):
    lob = circ([(cx - w * .5, cy, w * .56, h), (cx + w * .5, cy, w * .56, h), (cx, cy, w * .64, h * 1.02)])
    mass(s, lob, lin(cx - w, cy - h, cx + w, cy + h, [(0, L(PUMP, .5)), (.5, PUMP), (1, PUMP_D)]), ol=A(OL(PUMP_D, .45), 1.0), ow=1.1)
    s.path([("M", cx - w * .24, cy - h * .93), ("Q", cx - w * .42, cy, cx - w * .24, cy + h * .95),
            ("M", cx + w * .24, cy - h * .93), ("Q", cx + w * .42, cy, cx + w * .24, cy + h * .95)], stroke=A(PUMP_D, .55), sw=max(1.0, w * .045))
    s.curve([(cx, cy - h * .88), (cx + w * .03, cy - h * 1.1), (cx + w * .16, cy - h * 1.32)], stroke="#5C8A3A", sw=max(2.4, w * .17))
    if face:
        fc = rad(cx, cy + h * .15, w * .7, [(0, "#FFF3B0"), (1, "#FFA928")])
        s.poly([(cx - w * .56, cy - h * .06), (cx - w * .2, cy - h * .06), (cx - w * .38, cy - h * .5)], fill=fc)
        s.poly([(cx + w * .56, cy - h * .06), (cx + w * .2, cy - h * .06), (cx + w * .38, cy - h * .5)], fill=fc)
        s.poly([(cx - w * .08, cy + h * .08), (cx + w * .08, cy + h * .08), (cx, cy - h * .06)], fill=fc)
        s.poly([(cx - w * .62, cy + h * .24), (cx - w * .4, cy + h * .2), (cx - w * .28, cy + h * .38), (cx - w * .1, cy + h * .22),
                (cx + w * .1, cy + h * .22), (cx + w * .28, cy + h * .38), (cx + w * .4, cy + h * .2), (cx + w * .62, cy + h * .24),
                (cx + w * .46, cy + h * .62), (cx, cy + h * .74), (cx - w * .46, cy + h * .62)], fill=fc)


def pumpkins_a():
    s = Scene()
    glow(s, 50, 58, 46, ORANGE, .5)
    shadow(s, 50, 91, 32, 4, "#1B103038")
    pumpkin(s, 50, 61, 34, 28, face=True)
    s.path(leaf(58, 30, -20, 12, 3.4), fill=lin(58, 26, 70, 32, [(0, "#8CC46A"), (1, "#4F8A44")]))
    s.path(leaf(54, 28, 200, 9, 2.4), fill="#6DAA55")
    shine(s, 33, 46, 8, 3.4, -50, .55)
    return s.bake()


def pumpkins_b():
    s = Scene()
    glow(s, 50, 62, 44, ORANGE, .32)
    shadow(s, 50, 91, 40, 4, "#1B103038")
    s.curve([(6, 83), (24, 88), (46, 84), (70, 88), (92, 84)], stroke="#5C8A3A", sw=2)
    pumpkin(s, 50, 58, 25, 21)
    pumpkin(s, 25, 76, 15, 12.5)
    pumpkin(s, 75, 75, 17, 14)
    s.path(sum([leaf(38, 86, -165, 12, 4.4), leaf(88, 86, -20, 10, 3.6), leaf(12, 86, -30, 9, 3.2)], []), fill=lin(0, 70, 100, 90, [(0, "#8CC46A"), (1, "#4F8A44")]))
    shine(s, 41, 48, 6, 2.6, -50, .55)
    return s.bake()


def pumpkins_c():
    s = Scene()
    glow(s, 46, 62, 44, ORANGE, .46)
    shadow(s, 52, 91, 38, 4, "#1B103038")
    pumpkin(s, 76, 78, 13, 11)
    pumpkin(s, 44, 66, 28, 23, face=True)
    HAT = "#6B49A8"
    with s.rotate(-8, 42, 44):
        s.path("M 30 44 C 33 34 40 26 52 12 C 51 24 56 34 58 44 Z", fill=lin(30, 12, 58, 44, [(0, L(HAT, .3)), (1, D(HAT, .4))]), stroke=OL(HAT, .55), sw=1.2, join=JOIN_ROUND)
        s.path("M 32 40 C 40 43 50 43 57 40 L 58 44 C 50 47 38 47 31 44 Z", fill="#F0C14B")
        s.ellipse(42, 44.5, 21, 4.8, fill=lin(22, 40, 62, 50, [(0, L(HAT, .2)), (1, D(HAT, .5))]), stroke=OL(HAT, .55), sw=1.2)
        s.rect(40, 38.5, 6, 6, r=1.2, stroke="#B88A1E", sw=1.1)
    shine(s, 28, 56, 7, 3, -50, .5)
    return s.bake()


# ================================================================== CASA EMBRUJADA

HW = "#6A4FA0"
HR = "#3B2866"


def win(s, cx, top, w, h):
    s.path(arch(cx, top, w, h), fill=lin(cx, top, cx, top + h, [(0, "#FFE9A0"), (1, "#FF9A2E")]))


def haunted_a():
    s = Scene()
    glow(s, 50, 52, 48, PURPLE, .42)
    glow(s, 56, 70, 28, ORANGE, .25)
    shadow(s, 50, 91, 38, 4, "#1B103038")
    ol = OL(HW, .55)
    s.rect(72, 30, 7, 18, r=1.2, fill=G(HW, 72, 30, 79, 48, .1, .4), stroke=ol, sw=1.1)
    s.rect(20, 34, 19, 57, r=1.5, fill=lin(20, 0, 39, 0, [(0, L(HW, .22)), (1, D(HW, .25))]), stroke=ol, sw=1.2)
    s.path("M 15 36 C 22 33 27 24 29.5 11 C 32 24 37 33 44 36 Z", fill=lin(15, 11, 44, 36, [(0, L(HR, .22)), (1, D(HR, .3))]), stroke=OL(HR, .55), sw=1.2, join=JOIN_ROUND)
    s.rect(34, 52, 51, 39, r=1.5, fill=lin(34, 0, 85, 0, [(0, L(HW, .22)), (1, D(HW, .3))]), stroke=ol, sw=1.2)
    s.path("M 27 54 C 38 51 48 40 59 26 C 70 40 80 51 92 54 Z", fill=lin(27, 26, 92, 54, [(0, L(HR, .22)), (1, D(HR, .3))]), stroke=OL(HR, .55), sw=1.2, join=JOIN_ROUND)
    win(s, 29.5, 44, 8, 13)
    win(s, 29.5, 66, 8, 13)
    win(s, 47, 58, 9, 14)
    win(s, 72, 58, 9, 14)
    s.path(arch(59.5, 74, 13, 17), fill="#2A1A44")
    s.path(arch(59.5, 77, 8, 14), fill=lin(0, 77, 0, 91, [(0, "#FF9A2E"), (1, "#FFD070")]))
    s.path(bat(14, 22, 6) + bat(80, 14, 5.4), fill="#2F2340")
    return s.bake()


def haunted_b():
    s = Scene()
    glow(s, 64, 28, 34, ORANGE, .4)
    s.circle(64, 28, 13, fill=rad(59, 23, 21, [(0, "#FFF6D2"), (1, "#F4D88A")]), stroke="#D9B64E", sw=1)
    shadow(s, 50, 91, 28, 4, "#1B103038")
    ol = OL(HW, .55)
    with s.rotate(4, 50, 91):
        s.path("M 30 91 L 35 44 L 65 44 L 70 91 Z", fill=lin(30, 0, 70, 0, [(0, L(HW, .22)), (1, D(HW, .3))]), stroke=ol, sw=1.2, join=JOIN_ROUND)
        s.rect(31, 40, 38, 7, r=2, fill=G(HW, 31, 40, 69, 47, .3, .3), stroke=ol, sw=1.1)
        s.rect(36, 24, 28, 17, r=2, fill=lin(36, 0, 64, 0, [(0, L(HW, .22)), (1, D(HW, .3))]), stroke=ol, sw=1.1)
        s.path("M 31 26 C 40 23 46 15 51 4 C 54 14 60 23 69 26 Z", fill=lin(31, 4, 69, 26, [(0, L(HR, .22)), (1, D(HR, .3))]), stroke=OL(HR, .55), sw=1.1, join=JOIN_ROUND)
        win(s, 50, 27, 10, 13)
        win(s, 50, 52, 9, 14)
        s.path(arch(50, 74, 14, 17), fill="#2A1A44")
        s.path(arch(50, 77, 8, 14), fill=lin(0, 77, 0, 91, [(0, "#FF9A2E"), (1, "#FFD070")]))
        s.path([("M", 34, 66), ("L", 38, 66), ("M", 62, 62), ("L", 66, 62), ("M", 33, 78), ("L", 38, 78)], stroke=A("#2A1A44", .4), sw=1.3)
    s.path(bat(22, 24, 6) + bat(84, 52, 5) + bat(14, 44, 4.2), fill="#2F2340")
    return s.bake()


def haunted_c():
    s = Scene()
    glow(s, 50, 52, 46, PURPLE, .36)
    glow(s, 26, 36, 18, ORANGE, .6)
    glow(s, 74, 36, 18, ORANGE, .6)
    shadow(s, 50, 91, 38, 4, "#1B103038")
    ST = "#7C6BA8"
    ol = OL(ST, .55)
    iron = "#3A2D52"
    bars = [("M", x, 90) if False else ("M", x, 88) for x in ()]
    cm = []
    for x in (40, 46, 52, 58, 64)[:0]:
        pass
    # barras
    for x in (40, 46.5, 53, 59.5):
        top = 56 - 4 * math.cos((x - 50) / 14.0 * 1.4)
        cm += [("M", x, 90), ("L", x, top)]
    s.path(cm, stroke=iron, sw=2.2, cap=CAP_BUTT)
    s.path("M 35 60 Q 50 36 65 60", stroke=iron, sw=2.6)
    s.path("M 35 78 L 65 78", stroke=iron, sw=2)
    s.path(sum([[("M", x - 1.8, y + 2), ("L", x, y - 3.5), ("L", x + 1.8, y + 2), ("Z",)] for x, y in
                [(40, 54), (46.5, 51.5), (53, 51.5), (59.5, 54)]], []), fill=iron)
    for px in (26, 74):
        s.rect(px - 7, 48, 14, 43, r=1.5, fill=lin(px - 7, 0, px + 7, 0, [(0, L(ST, .25)), (1, D(ST, .3))]), stroke=ol, sw=1.2)
        s.rect(px - 9.5, 44, 19, 6, r=2, fill=G(ST, px - 9.5, 44, px + 9.5, 50, .35, .25), stroke=ol, sw=1.2)
        s.circle(px, 37, 7, fill=rad(px - 2, 34, 9, [(0, "#FFF0B0"), (.6, "#FFA928"), (1, "#E4701A")]), stroke=OL("#C4571A", .4), sw=1)
        s.rect(px - 1, 28, 2, 4, r=1, fill="#5C8A3A")
    s.path(bat(50, 20, 7), fill="#2F2340")
    s.path(blades([(12, 90, -3, 6), (88, 90, 3, 6), (36, 90, 3, 6)]), fill="#4F7A60")
    return s.bake()


# ================================================================== NUBES

def cloud_a():
    s = Scene()
    cl = circ([(33, 56, 14), (49, 45, 19), (67, 54, 15), (53, 60, 16)]) + rr(19, 56, 66, 21, 10.5)
    mass(s, cl, lin(0, 27, 0, 78, [(0, "#FFFFFF"), (.6, "#F1F4FF"), (1, "#C9D3F2")]), ol="#B2BDE6", ow=1.1)
    shine(s, 42, 38, 9, 3.6, -30, .85)
    s.path([("M", 30, 72), ("Q", 50, 76, 72, 72)], stroke="#FFFFFF99", sw=1.4)
    return s.bake()


def cloud_b():
    s = Scene()
    cl = circ([(26, 58, 11), (42, 50, 16), (62, 54, 14), (77, 60, 10)]) + rr(14, 57, 74, 17, 8.5)
    mass(s, cl, lin(0, 34, 0, 75, [(0, "#FFFFFF"), (.55, "#F4F1FF"), (1, "#CFC8F0")]), ol="#B9B2E4", ow=1.1)
    shine(s, 38, 42, 8, 3.2, -30, .85)
    return s.bake()


def cloud_c():
    s = Scene()
    big = circ([(31, 52, 12), (46, 43, 16), (60, 52, 12)]) + rr(17, 52, 56, 19, 9.5)
    mass(s, big, lin(0, 27, 0, 72, [(0, "#FFFFFF"), (.6, "#F1F4FF"), (1, "#C9D3F2")]), ol="#B2BDE6", ow=1.1)
    sm = circ([(72, 70, 8), (82, 72, 6)]) + rr(62, 70, 28, 11, 5.5)
    mass(s, sm, lin(0, 62, 0, 82, [(0, "#FFFFFF"), (1, "#D4DCF4")]), ol="#B8C2E8", ow=1)
    shine(s, 40, 36, 7, 2.8, -30, .85)
    return s.bake()


# ================================================================== ARBUSTOS, FLORES, ROCAS, VARIOS

def bush_a():
    s = Scene()
    shadow(s, 50, 90, 36)
    bsh = circ([(26, 72, 14), (74, 72, 14), (50, 60, 21), (38, 77, 13), (62, 77, 13), (50, 78, 13)])
    mass(s, bsh, lin(20, 40, 80, 92, [(0, "#9BD98D"), (.5, "#6FB57A"), (1, "#418A5E")]), ol=OL("#418A5E", .5), ow=1.3)
    s.path([("M", 30, 62), ("Q", 36, 54, 46, 52), ("M", 58, 70), ("Q", 66, 62, 72, 64), ("M", 24, 76), ("Q", 28, 72, 34, 72)], stroke="#FFFFFF55", sw=2)
    s.path(circ([(36, 64, 2.4), (60, 56, 2.4), (68, 74, 2.4), (44, 80, 2.4), (52, 68, 2.4)]), fill="#E8575A")
    s.path(circ([(35.4, 63.4, .8), (59.4, 55.4, .8), (67.4, 73.4, .8)]), fill="#FFFFFFAA")
    return s.bake()


def bush_b():
    s = Scene()
    shadow(s, 50, 90, 34)
    back = circ([(34, 56, 15), (62, 52, 16), (48, 44, 15)])
    mass(s, back, lin(20, 30, 80, 70, [(0, "#74B88C"), (1, "#3F8460")]), ol=OL("#3F8460", .5), ow=1.2)
    front = circ([(28, 74, 14), (72, 74, 14), (50, 68, 18), (40, 78, 12.5), (60, 78, 12.5)])
    mass(s, front, lin(20, 50, 80, 92, [(0, "#A6E08E"), (.5, "#72BE78"), (1, "#4B9A66")]), ol=OL("#4B9A66", .5), ow=1.2)
    s.path(sum([flower5(x, y, r, pr=.55) for x, y, r in [(34, 62, 4.6), (62, 66, 4.8), (50, 50, 4.4), (74, 80, 4), (24, 80, 3.8)]], []), fill="#FFE9F1")
    s.path(circ([(x, y, 1.2) for x, y in [(34, 62), (62, 66), (50, 50), (74, 80), (24, 80)]]), fill="#F3B63F")
    s.path([("M", 22, 66), ("Q", 28, 58, 38, 56)], stroke="#FFFFFF55", sw=2)
    return s.bake()


def flower_a():
    s = Scene()
    shadow(s, 50, 91, 16, 3.2)
    s.path([("M", 50, 91), ("C", 51, 80, 49, 66, 50, 50)], stroke="#4F9A62", sw=3.2)
    s.path(leaf(50, 80, -150, 20, 5) + leaf(50, 72, -30, 18, 4.6), fill=lin(26, 60, 74, 82, [(0, "#8CD08A"), (1, "#3F8C5A")]))
    pet = []
    for i in range(8):
        pet += leaf(50, 38, i * 45 - 90, 20, 5.4)
    s.path(pet, fill=lin(36, 20, 64, 56, [(0, "#FFFFFF"), (1, "#F4E3EC")]), stroke="#CDB4C4", sw=1)
    s.circle(50, 38, 7, fill=R("#F7C63E", 48, 36, 9, .45, .2), stroke="#C99522", sw=1)
    return s.bake()


def flower_b():
    s = Scene()
    shadow(s, 50, 91, 18, 3.2)
    s.path([("M", 50, 91), ("C", 49, 80, 51, 66, 50, 52)], stroke="#4F9A62", sw=3.4)
    s.path(leaf(50, 90, -160, 34, 8) + leaf(50, 90, -22, 32, 7.5), fill=lin(20, 56, 80, 92, [(0, "#9BD98D"), (1, "#3F8C5A")]))
    cup = "M 33 32 L 38 24 L 44 32 L 50 20 L 56 32 L 62 24 L 67 32 C 69 48 60 58 50 58 C 40 58 31 48 33 32 Z"
    s.path(cup, fill=lin(32, 20, 68, 58, [(0, "#FF9E8C"), (.55, "#E8604E"), (1, "#B63A4A")]), stroke=OL("#B63A4A", .45), sw=1.2, join=JOIN_ROUND)
    s.path([("M", 50, 26), ("Q", 47, 42, 50, 56), ("M", 38, 34), ("Q", 40, 46, 46, 55)], stroke="#FFFFFF33", sw=1.2)
    shine(s, 41, 40, 3, 7, 12, .55)
    return s.bake()


def flower_c():
    s = Scene()
    shadow(s, 50, 91, 24, 3.2)
    s.path([("M", 50, 91), ("C", 49, 76, 51, 64, 50, 44), ("M", 36, 91), ("C", 36, 80, 34, 70, 32, 58), ("M", 66, 91), ("C", 66, 82, 69, 74, 70, 64)],
           stroke="#4F9A62", sw=2.6)
    s.path(leaf(50, 88, -150, 16, 4.2) + leaf(50, 80, -30, 14, 3.8) + leaf(66, 88, -30, 12, 3.4) + leaf(36, 88, -150, 12, 3.4),
           fill=lin(20, 60, 80, 92, [(0, "#9BD98D"), (1, "#3F8C5A")]))
    s.path(flower5(50, 38, 11), fill=lin(40, 26, 62, 50, [(0, "#C9B2FF"), (1, "#8E66D9")]))
    s.path(flower5(32, 52, 8.5), fill=lin(24, 44, 40, 62, [(0, "#FFB0A0"), (1, "#E8604E")]))
    s.path(flower5(70, 58, 8), fill=lin(62, 50, 78, 66, [(0, "#FFE58A"), (1, "#EBB22E")]))
    s.path(circ([(50, 38, 3.2), (32, 52, 2.6), (70, 58, 2.5)]), fill="#FFF6D6")
    return s.bake()


def rock_a():
    s = Scene()
    shadow(s, 50, 91, 38, 4.2)
    body = "M 12 90 C 9 72 20 52 40 45 C 56 40 74 46 84 62 C 91 73 91 84 87 90 Z"
    gc = "#A7A2BC"
    s.path(body, fill=lin(14, 42, 86, 92, [(0, L(gc, .5)), (.5, gc), (1, D(gc, .4))]), stroke=OL(gc, .5), sw=1.4, join=JOIN_ROUND)
    s.path("M 56 44 C 74 46 84 58 86 70 C 90 80 90 86 87 90 L 62 90 C 70 78 68 58 56 44 Z", fill=A(D(gc, .55), .35))
    s.path("M 24 62 C 28 50 40 46 52 46 C 48 50 44 54 40 60 C 34 58 28 60 24 62 Z", fill="#6FAE70")
    s.path(circ([(34, 52, 3), (46, 49, 2.4)]), fill="#9BD98D")
    s.path([("M", 56, 62), ("L", 50, 72), ("L", 54, 82)], stroke=A(D(gc, .6), .5), sw=1.3)
    shine(s, 30, 70, 7, 3, -60, .5)
    s.path(blades([(14, 90, -3, 7), (19, 90, 2, 5), (86, 90, 3, 7)]), fill="#5AA56A")
    return s.bake()


def rock_b():
    s = Scene()
    shadow(s, 50, 91, 40, 4)
    gc = "#B3AEC6"
    s.path("M 8 90 C 6 78 14 66 28 62 C 40 59 52 64 58 74 C 62 80 62 86 60 90 Z", fill=lin(8, 60, 60, 90, [(0, L(gc, .5)), (1, D(gc, .4))]), stroke=OL(gc, .5), sw=1.3, join=JOIN_ROUND)
    s.path("M 40 90 C 38 70 48 52 64 46 C 76 42 88 50 90 66 C 91 76 88 84 86 90 Z", fill=lin(40, 44, 90, 90, [(0, L(gc, .35)), (.5, gc), (1, D(gc, .45))]), stroke=OL(gc, .5), sw=1.4, join=JOIN_ROUND)
    s.path("M 74 44 C 84 48 90 56 90 66 C 91 76 88 84 86 90 L 70 90 C 80 78 80 58 74 44 Z", fill=A(D(gc, .55), .33))
    s.path("M 56 52 C 62 46 72 44 80 46 C 76 50 70 52 66 56 C 62 54 58 54 56 52 Z", fill="#6FAE70")
    shine(s, 60, 62, 5, 2.4, -60, .5)
    shine(s, 18, 72, 4, 1.8, -50, .5)
    s.path(circ([(34, 90, 4, 2.4), (44, 91, 3, 1.8)]), fill=lin(30, 88, 48, 93, [(0, "#CFCADD"), (1, "#8F8AA6")]))
    s.path(blades([(8, 90, -3, 7), (14, 90, 2, 5), (92, 90, 3, 6)]), fill="#5AA56A")
    return s.bake()


def signpost():
    s = Scene()
    shadow(s, 50, 91, 22, 3.6)
    wd = "#A9744A"
    s.rect(45, 18, 10, 73, r=3, fill=lin(45, 0, 55, 0, [(0, L(wd, .3)), (1, D(wd, .35))]), stroke=OL(wd, .5), sw=1.2)
    s.path("M 30 26 L 66 26 L 79 34.5 L 66 43 L 30 43 Z", fill=lin(0, 26, 0, 43, [(0, L("#E3B27A", .3)), (1, D("#E3B27A", .2))]), stroke=OL("#B07A48", .5), sw=1.2, join=JOIN_ROUND)
    s.path("M 70 49 L 34 49 L 21 57.5 L 34 66 L 70 66 Z", fill=lin(0, 49, 0, 66, [(0, L("#D79E62", .3)), (1, D("#D79E62", .22))]), stroke=OL("#B07A48", .5), sw=1.2, join=JOIN_ROUND)
    s.path([("M", 35, 31), ("L", 62, 31), ("M", 35, 38), ("L", 56, 38), ("M", 63, 54), ("L", 40, 54), ("M", 63, 61), ("L", 44, 61)], stroke=A("#7A4A28", .3), sw=1)
    s.path(circ([(34, 34.5, 1.3), (47, 34.5, 1.3), (66, 57.5, 1.3), (53, 57.5, 1.3)]), fill="#5E3A22")
    s.circle(50, 17, 3.6, fill=G(wd, 46, 13, 54, 21, .35, .25), stroke=OL(wd, .5), sw=1)
    s.path(blades([(40, 90, -3, 7), (45, 90, 2, 5), (58, 90, 3, 7), (62, 90, -1, 5)]), fill="#5AA56A")
    return s.bake()


def fence():
    s = Scene()
    shadow(s, 50, 91, 42, 3.6)
    wd = "#D9A56C"
    pk = []
    for x in (13, 28.5, 44, 59.5, 75):
        pk += [("M", x, 90), ("L", x, 52), ("L", x + 5.5, 44), ("L", x + 11, 52), ("L", x + 11, 90), ("Z",)]
    mass(s, pk, lin(0, 44, 0, 90, [(0, L(wd, .4)), (1, D(wd, .22))]), ol=OL("#B07A48", .5), ow=1.2)
    s.path(sum([[("M", x + 8, 50), ("L", x + 11, 52), ("L", x + 11, 90), ("L", x + 8, 90), ("Z",)] for x in (13, 28.5, 44, 59.5, 75)], []), fill=A(D(wd, .6), .22))
    for y in (58, 76):
        s.rect(8, y, 84, 6.5, r=2.4, fill=lin(0, y, 0, y + 6.5, [(0, L("#C98F5B", .35)), (1, D("#C98F5B", .25))]), stroke=OL("#B07A48", .5), sw=1.1)
    s.path(circ([(x + 5.5, 61, 1) for x in (13, 28.5, 44, 59.5, 75)] + [(x + 5.5, 79, 1) for x in (13, 28.5, 44, 59.5, 75)]), fill="#6B4528")
    s.path(blades([(10, 90, -3, 7), (24, 90, 2, 6), (55, 90, -2, 7), (70, 90, 3, 6), (90, 90, 3, 7)]), fill="#5AA56A")
    return s.bake()


def firefly():
    s = Scene()
    glow(s, 52, 52, 42, "#D8F25A", .55)
    glow(s, 60, 58, 20, "#FFF7A0", .75)
    with s.rotate(-30, 48, 38):
        s.ellipse(48, 38, 11, 5, fill="#FFFFFFB3", stroke="#9DB0D8", sw=1)
    with s.rotate(-62, 52, 36):
        s.ellipse(52, 36, 10, 4.2, fill="#FFFFFF99", stroke="#9DB0D8", sw=1)
    s.ellipse(50, 54, 12, 6.4, fill=lin(38, 48, 62, 60, [(0, "#6A5A4A"), (1, "#352A33")]))
    s.circle(36, 53, 5, fill=lin(32, 48, 40, 58, [(0, "#7A6A58"), (1, "#352A33")]))
    s.circle(63, 57, 6.5, fill=rad(62, 55, 8, [(0, "#FFFFE0"), (.5, "#F4F57C"), (1, "#BEE83A")]))
    s.path([("M", 33, 49), ("Q", 28, 42, 24, 41), ("M", 35, 48), ("Q", 34, 40, 31, 37)], stroke="#4A3A3A", sw=1.1)
    s.path([("M", 44, 60), ("L", 42, 66), ("M", 50, 60), ("L", 50, 67), ("M", 56, 60), ("L", 58, 66)], stroke="#4A3A3A", sw=1.1)
    s.path(sparkle_c(76, 34, 5) + sparkle_c(24, 70, 3.6), fill="#FFF7A0")
    return s.bake()


def mushroom():
    s = Scene()
    shadow(s, 50, 91, 36, 3.8)
    cr = "#E4574F"
    # chica
    s.path("M 62 90 C 63 84 62 78 61 73 L 76 73 C 75 78 74 84 75 90 Z", fill=lin(60, 0, 78, 0, [(0, "#FFFBF0"), (1, "#E8DCC4")]), stroke=OL("#C9B896", .5), sw=1.1, join=JOIN_ROUND)
    s.path("M 56 74 C 56 58 82 58 82 74 C 76 78 62 78 56 74 Z", fill=lin(56, 58, 82, 78, [(0, L(cr, .3)), (1, D(cr, .35))]), stroke=OL(cr, .5), sw=1.1, join=JOIN_ROUND)
    s.path(circ([(64, 67, 2), (73, 66, 1.6)]), fill="#FFFFFFE6")
    # grande
    s.path("M 33 90 C 35 80 35 70 33 60 L 56 60 C 54 70 54 80 56 90 Z", fill=lin(32, 0, 58, 0, [(0, "#FFFBF0"), (1, "#E8DCC4")]), stroke=OL("#C9B896", .5), sw=1.2, join=JOIN_ROUND)
    s.path("M 17 62 C 15 30 75 30 73 62 C 62 68 28 68 17 62 Z", fill=lin(16, 30, 74, 68, [(0, L(cr, .3)), (.55, cr), (1, D(cr, .38))]), stroke=OL(cr, .5), sw=1.3, join=JOIN_ROUND)
    s.path(circ([(30, 46, 4.4), (46, 40, 5), (60, 49, 3.8), (40, 55, 3.2), (53, 58, 2.6)]), fill="#FFFFFFEB")
    shine(s, 28, 38, 7, 2.8, -40, .55)
    return s.bake()


def stump():
    s = Scene()
    shadow(s, 50, 91, 38, 4)
    bk = "#9A6A50"
    s.path("M 22 56 C 22 68 24 80 18 91 C 30 94 42 90 50 91 C 58 90 70 94 82 91 C 76 80 78 68 78 56 Z",
           fill=lin(18, 0, 82, 0, [(0, L(bk, .3)), (.5, bk), (1, D(bk, .42))]), stroke=OL(bk, .5), sw=1.3, join=JOIN_ROUND)
    s.path([("M", 32, 62), ("Q", 30, 74, 32, 86), ("M", 44, 64), ("Q", 43, 76, 45, 88), ("M", 60, 64), ("Q", 62, 76, 60, 88), ("M", 70, 62), ("Q", 69, 72, 71, 82)],
           stroke=A(D(bk, .6), .4), sw=1.4)
    s.ellipse(50, 56, 29, 10, fill=lin(22, 46, 78, 66, [(0, "#F2D4A4"), (1, "#D9AC74")]), stroke=OL(bk, .5), sw=1.3)
    s.path(circ([(50, 56, 20, 6.6), (50, 56, 11, 3.6)]), fill="#FFFFFF00", stroke=A("#A9744A", .6), sw=1) if False else None
    s.ellipse(50, 56, 20, 6.6, stroke=A("#A9744A", .6), sw=1.1)
    s.ellipse(50, 56, 11, 3.6, stroke=A("#A9744A", .6), sw=1.1)
    s.circle(50, 56, 1.3, fill="#A9744A")
    s.path("M 22 62 C 24 70 28 74 34 72 C 32 68 34 64 30 60 Z", fill="#6FAE70")
    s.path(circ([(26, 66, 1.4), (30, 69, 1.2)]), fill="#9BD98D")
    s.curve([(68, 52), (68, 44), (70, 38)], stroke="#4F9A62", sw=2.2)
    s.path(leaf(70, 40, -150, 10, 3.2) + leaf(70, 40, -20, 10, 3.2), fill="#7FCB84")
    return s.bake()


def lamp_post():
    s = Scene()
    iron = "#4A3F66"
    glow(s, 50, 36, 38, "#FFD36E", .55)
    shadow(s, 50, 91, 20, 3.4)
    s.rect(40, 83, 20, 8, r=2.5, fill=G(iron, 40, 83, 60, 91, .3, .3), stroke=OL(iron, .5), sw=1.1)
    s.path("M 45 84 L 47 48 L 53 48 L 55 84 Z", fill=lin(45, 0, 55, 0, [(0, L(iron, .3)), (1, D(iron, .3))]), stroke=OL(iron, .5), sw=1.1, join=JOIN_ROUND)
    s.rect(43, 68, 14, 4, r=2, fill=G(iron, 43, 68, 57, 72, .35, .2), stroke=OL(iron, .5), sw=1)
    s.rect(39, 46, 22, 5, r=2, fill=G(iron, 39, 46, 61, 51, .3, .3), stroke=OL(iron, .5), sw=1.1)
    s.path("M 41 46 L 38 26 C 38 24 62 24 62 26 L 59 46 Z", fill=rad(50, 36, 18, [(0, "#FFF8D0"), (.6, "#FFD36E"), (1, "#F0A83A")]), stroke=OL(iron, .5), sw=1.1, join=JOIN_ROUND)
    s.path([("M", 50, 26), ("L", 50, 46), ("M", 44, 26), ("L", 45, 46), ("M", 56, 26), ("L", 55, 46)], stroke=A(iron, .55), sw=1.1)
    s.path("M 34 26 L 66 26 L 56 15 L 44 15 Z", fill=lin(34, 15, 66, 26, [(0, L(iron, .3)), (1, D(iron, .3))]), stroke=OL(iron, .5), sw=1.1, join=JOIN_ROUND)
    s.circle(50, 12.5, 3.2, fill=G(iron, 47, 9, 53, 16, .35, .2), stroke=OL(iron, .5), sw=1)
    shine(s, 46, 34, 1.8, 5, 8, .7)
    return s.bake()


# ================================================================== paquete

def build():
    icons = {
        "prop.cherry_a": cherry_a(), "prop.cherry_b": cherry_b(), "prop.cherry_c": cherry_c(),
        "prop.gold_dunes_a": gold_a(), "prop.gold_dunes_b": gold_b(), "prop.gold_dunes_c": gold_c(),
        "prop.moon_a": moon_a(), "prop.moon_b": moon_b(), "prop.moon_c": moon_c(),
        "prop.bamboo_a": bamboo_a(), "prop.bamboo_b": bamboo_b(), "prop.bamboo_c": bamboo_c(),
        "prop.graveyard_a": graveyard_a(), "prop.graveyard_b": graveyard_b(), "prop.graveyard_c": graveyard_c(),
        "prop.dead_forest_a": dead_tree_a(), "prop.dead_forest_b": dead_tree_b(), "prop.dead_forest_c": dead_tree_c(),
        "prop.pumpkins_a": pumpkins_a(), "prop.pumpkins_b": pumpkins_b(), "prop.pumpkins_c": pumpkins_c(),
        "prop.haunted_a": haunted_a(), "prop.haunted_b": haunted_b(), "prop.haunted_c": haunted_c(),
        "prop.cloud_a": cloud_a(), "prop.cloud_b": cloud_b(), "prop.cloud_c": cloud_c(),
        "prop.bush_a": bush_a(), "prop.bush_b": bush_b(),
        "prop.flower_a": flower_a(), "prop.flower_b": flower_b(), "prop.flower_c": flower_c(),
        "prop.rock_a": rock_a(), "prop.rock_b": rock_b(),
        "prop.signpost": signpost(), "prop.fence": fence(), "prop.firefly": firefly(),
        "prop.mushroom": mushroom(), "prop.stump": stump(), "prop.lamp_post": lamp_post(),
    }
    return {"icons": icons}
