"""
Escenas de fondo de avatar (portadas de drawScene / drawAvatar de AvatarArt.kt).
Se pintan sobre el degradado bg1->bg2 (lo pone la app) y debajo del animal. En Android se animan; aqui son
estaticas (la app las mece despacio). Los detalles van en los bordes: el animal ocupa el centro
(cabeza: ovalo x 17..83, y 29..87; hombros desde y=82).

Ligeras a proposito: las piezas del mismo color/estilo se agrupan en una sola forma (menos formas = menos
trabajo por fotograma cuando la app las mece).
"""
import os, sys, math, random
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, ".."))   # artlib
sys.path.insert(0, HERE)                        # _looks, _kit
from artlib import *
from artlib import _ellipse_cmds
import _looks

PAL = _looks.palette("FOX")
W = "#FFFFFF"
INK = "#3D405B"


def ha(hexcol, a):
    """Color #RRGGBB con alfa a (0..1) como #RRGGBBAA (mas corto en el JSON que alpha())."""
    return hexcol[:7] + "%02X" % int(round(a * 255))


RIM = ha(INK, .34)      # contorno fino para que lo blanco se lea sobre fondos claros
SOFT = ha(INK, .18)


def new():
    return Scene(100, 100, pal=PAL)


# ------------------------------------------------------------------ utilidades geometricas

def place(cmds, x=0.0, y=0.0, rot=0.0, sc=1.0):
    """Gira, escala y mueve una lista de comandos (para agrupar muchas piezas en una sola forma)."""
    c, s_ = math.cos(math.radians(rot)), math.sin(math.radians(rot))

    def f(px, py):
        return (x + (px * c - py * s_) * sc, y + (px * s_ + py * c) * sc)

    out = []
    for cm in cmds:
        if cm[0] == "Z":
            out.append(cm)
        else:
            pts = [f(cm[i], cm[i + 1]) for i in range(1, len(cm), 2)]
            out.append((cm[0],) + tuple(v for p in pts for v in p))
    return out


def pt(px, py, x=0.0, y=0.0, rot=0.0, sc=1.0):
    c, s_ = math.cos(math.radians(rot)), math.sin(math.radians(rot))
    return (x + (px * c - py * s_) * sc, y + (px * s_ + py * c) * sc)


def poly_cmds(pts):
    return [("M", pts[0][0], pts[0][1])] + [("L", p[0], p[1]) for p in pts[1:]] + [("Z",)]


def circles(pts, k=1.0):
    cmds = []
    for (x, y, r) in pts:
        cmds += _ellipse_cmds(x, y, r * k, r * k)
    return cmds


def star4_path(x, y, r, squeeze=.24):
    k = r * squeeze
    return Path().M(x, y - r).Q(x + k, y - k, x + r, y).Q(x + k, y + k, x, y + r).Q(x - k, y + k, x - r, y).Q(x - k, y - k, x, y - r).Z()


def arc_cmds(cx, cy, r, a0, a1):
    x0, y0 = cx + r * math.cos(math.radians(a0)), cy + r * math.sin(math.radians(a0))
    x1, y1 = cx + r * math.cos(math.radians(a1)), cy + r * math.sin(math.radians(a1))
    return Path().M(x0, y0).A(r, r, 0, 1 if abs(a1 - a0) > 180 else 0, 1 if a1 > a0 else 0, x1, y1).cmds


# ------------------------------------------------------------------ utilidades de dibujo

def glow(s, x, y, r, col=W, a=.5):
    s.circle(x, y, r, fill=rad(x, y, r, [(0, ha(col, a)), (1, ha(col, 0))]))


def glint(s, x, y, r, tint="#FFE7A8", rim=RIM, g=0, ga=.5, squeeze=.24):
    """Destello de 4 puntas (blanco que vira a un tinte calido) con contorno fino."""
    if g:
        glow(s, x, y, r * g, W, ga)
    s.path(star4_path(x, y, r, squeeze), fill=lin(x - r * .5, y - r * .6, x + r * .5, y + r * .6, [(0, W), (1, tint)]),
           stroke=rim, sw=.5, join=JOIN_ROUND)


def dots(s, pts, halo=.16, rim=ha(INK, .26)):
    """Muchos puntitos de luz en solo dos formas (halo suave + puntos)."""
    if halo:
        s.path(circles(pts, 2.4), fill=ha(W, halo))
    s.path(circles(pts), fill=W, stroke=rim, sw=.35)


def scatter(sizes, seed, gap=1.5, head_pad=1.5, edge=49.0, a0=125.0, a1=415.0, tries=400):
    """Reparte (x, y, r) en el anillo libre alrededor de la cabeza, repartido por angulos para que no se amontone.
    Determinista. Los angulos van de a0 a a1 pasando por arriba (abajo quedan los hombros)."""
    rnd = random.Random(seed)
    n = len(sizes)
    slots = list(range(n))
    rnd.shuffle(slots)
    step = (a1 - a0) / float(n)
    pts = []
    for i, r in enumerate(sizes):
        base = a0 + (slots[i] + .5) * step
        ok = None
        for t in range(tries * 2):
            if t < tries:
                th = math.radians(base + rnd.uniform(-.5, .5) * step)
                rr = rnd.uniform(22, 50)
                x, y = 50 + rr * math.cos(th), 50 + rr * math.sin(th)
            else:                                  # plan B: en cualquier sitio libre
                x, y = rnd.uniform(2, 98), rnd.uniform(2, 98)
            if math.hypot(x - 50, y - 50) > edge - r * .7:
                continue
            if ((x - 50) / (33 + r + head_pad)) ** 2 + ((y - 58) / (29 + r + head_pad)) ** 2 < 1:
                continue
            if ((x - 50) / (40 + r)) ** 2 + ((y - 104) / (22 + r)) ** 2 < 1:
                continue
            if any(math.hypot(x - px, y - py) < r + pr + gap for (px, py, pr) in pts):
                continue
            ok = (x, y, r)
            break
        if ok:
            pts.append(ok)
    return pts


# ------------------------------------------------------------------ SPARKLES

def scene_sparkles():
    s = new()
    big = [(17, 20, 8.4), (83, 25, 6.8), (91, 57, 6.0), (10, 47, 4.8), (73, 10, 4.0), (12, 71, 3.4), (88, 79, 3.4)]
    for (x, y, r) in big[:3]:
        glow(s, x, y, r * 1.9, W, .5)
    for (x, y, r) in big:
        glint(s, x, y, r)
    dots(s, [(30, 9, 1.4), (93, 40, 1.5), (6, 31, 1.3), (24, 27, 1.1), (62, 6, 1.2), (96, 70, 1.1)])
    return s.bake()


# ------------------------------------------------------------------ RAYS

def scene_rays():
    s = new()
    cx, cy = 50, 62
    warm = "#FFF1CC"
    s.circle(cx, cy, 47, fill=rad(cx, cy, 47, [(0, ha(warm, .45)), (.72, ha(warm, .33)), (1, ha(warm, .1))]))
    wedges = []
    for i in range(12):
        a0 = math.radians(-97.5 + i * 30)
        a1 = a0 + math.radians(15)
        wedges += poly_cmds([(cx, cy), (cx + math.cos(a0) * 96, cy + math.sin(a0) * 96), (cx + math.cos(a1) * 96, cy + math.sin(a1) * 96)])
    s.path(wedges, fill=rad(cx, cy, 96, [(0, ha(warm, .5)), (.5, ha(warm, .3)), (1, ha(warm, .08))]))
    s.circle(cx, cy, 44, stroke=ha(W, .6), sw=.9)
    s.circle(cx, cy, 48.5, stroke=ha(W, .32), sw=.6)
    for (x, y, r) in [(16, 22, 4.4), (86, 17, 3.6), (92, 70, 3.0), (9, 62, 2.6)]:
        glint(s, x, y, r)
    return s.bake()


# ------------------------------------------------------------------ STARS

def scene_stars():
    s = new()
    # constelacion arriba a la derecha
    const = [(74, 9), (86, 17), (89, 31), (80, 39)]
    s.curve(const[:1] + const[1:], ha(W, .45), .6, cap=CAP_ROUND) if False else None
    s.path(poly_cmds(const)[:-1], stroke=ha(W, .5), sw=.55, cap=CAP_ROUND, join=JOIN_ROUND)
    for (x, y, r) in [(86, 17, 2.4), (89, 31, 1.9)]:
        glow(s, x, y, r * 3, W, .5)
    dots(s, [(74, 9, 2.2), (86, 17, 2.0), (89, 31, 1.7), (80, 39, 1.4),
             (14, 17, 1.7), (8, 43, 1.5), (93, 53, 1.8), (31, 8, 1.4), (60, 5, 1.2), (7, 66, 1.9), (94, 70, 1.1), (21, 36, 1.1)])
    glow(s, 17, 29, 10, W, .5)
    glint(s, 17, 29, 5.2)
    glint(s, 12, 53, 3.2, tint="#DCCBFF")
    for (x, y, r) in [(47, 6.5, 3.4), (96, 44, 2.2)]:
        s.star(x, y, r, fill=lin(x - r, y - r, x + r, y + r, [(0, W), (1, "#FFE9A0")]), stroke=RIM, sw=.45)
    return s.bake()


# ------------------------------------------------------------------ CLOUDS

CLOUD = Path().M(0, 7).A(7, 7, 0, 0, 1, -.062, -6.997).A(9, 9, 0, 0, 1, 16.062, -6.997).A(7, 7, 0, 0, 1, 16, 7).Z()


def cloud(s, x, y, sc):
    s.ellipse(x + 8 * sc, y + 9.4 * sc, 12.5 * sc, 1.8 * sc, fill=ha(INK, .13))
    with s.translate(x, y):
        with s.scale(sc):
            s.path(CLOUD, fill=lin(0, -12, 0, 7, [(0, W), (.55, mix(W, "$bg2", .1)), (1, mix(W, "$bg2", .5))]),
                   stroke=RIM, sw=1.1, join=JOIN_ROUND)
            s.arc_stroke(8, -3, 6.9, 208, 252, ha(W, .95), 1.1)


def scene_clouds():
    s = new()
    cloud(s, 8, 20, .98)
    cloud(s, 62, 10, .8)
    cloud(s, 76, 43, .6)
    cloud(s, 6, 63, .5)
    return s.bake()


# ------------------------------------------------------------------ HEARTS

HEART = [("M", 0, .95), ("C", -1.35, .05, -1.0, -1.05, 0, -.35), ("C", 1.0, -1.05, 1.35, .05, 0, .95), ("Z",)]


def scene_hearts():
    s = new()
    hl = []
    for (x, y, r, c1, c2) in [(16, 20, 6.0, "#FFA5C2", "#F2527F"), (84, 27, 6.8, "#FFB0C8", "#F7608C"), (73, 9, 3.6, "#FFC2D6", "#F7608C"),
                              (9, 54, 4.2, "#FFB0C8", "#F7608C"), (91, 63, 5.0, "#FFA5C2", "#F2527F"), (13, 77, 3.0, "#FFC2D6", "#F7608C"),
                              (35, 9, 2.6, "#FFC2D6", "#F7608C")]:
        s.path(place(HEART, x, y, 0, r), fill=lin(x - r, y - r, x + r * .9, y + r, [(0, c1), (1, c2)]),
               stroke=ha(mix_hex(c2, "#5A1030", .35), .55), sw=.6, join=JOIN_ROUND)
        hl += place(_ellipse_cmds(0, 0, .3, .15), x - r * .5, y - r * .3, -35, r)
    s.path(hl, fill=ha(W, .82))
    for (x, y, r) in [(25, 12, 2.6), (92, 44, 2.8), (6, 36, 2.4)]:
        glint(s, x, y, r, tint="#FFD0E0")
    dots(s, [(60, 5, 1.1), (96, 56, 1.1)], halo=.12)
    return s.bake()


def mix_hex(a, b, t):
    ca = [int(a[i:i + 2], 16) for i in (1, 3, 5)]
    cb = [int(b[i:i + 2], 16) for i in (1, 3, 5)]
    return "#%02X%02X%02X" % tuple(int(round(ca[i] + (cb[i] - ca[i]) * t)) for i in range(3))


# ------------------------------------------------------------------ SNOW

def flake_cmds(x, y, r, rot, barbs):
    d = []
    for k in range(3):
        a = math.radians(k * 60 + rot)
        dx, dy = math.cos(a) * r, math.sin(a) * r
        d += [("M", x - dx, y - dy), ("L", x + dx, y + dy)]
    if barbs:
        for k in range(6):
            a = math.radians(k * 60 + rot)
            px, py = x + math.cos(a) * r * .62, y + math.sin(a) * r * .62
            for sg in (-1, 1):
                b = a + sg * math.radians(48)
                d += [("M", px, py), ("L", px + math.cos(b) * r * .32, py + math.sin(b) * r * .32)]
    return d


def scene_snow():
    s = new()
    sizes = [6.0, 5.0, 4.2, 3.6] + [1.0, 1.7, 2.2, 1.3, 1.9, 1.1, 1.5, 2.0, 1.2, 1.7, 1.4, 2.1, 1.3, 1.8]
    pts = scatter(sizes, 11, gap=2.2)
    rnd = random.Random(5)
    flakes = [p for p in pts if p[2] >= 3.5]
    small = [p for p in pts if p[2] < 3.5]
    for (x, y, r) in flakes:
        glow(s, x, y, r * 1.8, W, .32)
        c = flake_cmds(x, y, r, rnd.uniform(0, 60), r >= 4.5)
        s.path(c, stroke=ha(INK, .3), sw=max(1.3, r * .34), cap=CAP_ROUND, join=JOIN_ROUND)
        s.path(c, stroke=W, sw=max(.7, r * .2), cap=CAP_ROUND, join=JOIN_ROUND)
    dots(s, small, halo=.18)
    return s.bake()


# ------------------------------------------------------------------ SPOOKY

BAT = (Path().M(0, -1.2).L(-1.7, -4.2).L(-2.7, -1.5).Q(-6.5, -7, -12, -2.6).Q(-10.4, -.4, -9.4, 2.4).Q(-7.2, .6, -5.6, 3.2)
       .Q(-3.6, 1, -1.9, 3.8).Q(-1, 4.6, 0, 4.4).Q(1, 4.6, 1.9, 3.8).Q(3.6, 1, 5.6, 3.2).Q(7.2, .6, 9.4, 2.4)
       .Q(10.4, -.4, 12, -2.6).Q(6.5, -7, 2.7, -1.5).L(1.7, -4.2).Z())


def bat(s, cx, cy, sc, rot=0):
    with s.translate(cx, cy):
        with s.rotate(rot):
            with s.scale(sc):
                s.path(BAT, fill=lin(0, -7, 0, 5, [(0, "#42297A"), (1, "#160C2E")]), stroke=ha("#CDB8FF", .5), sw=.55, join=JOIN_ROUND)


def scene_spooky():
    s = new()
    mx, my = 76, 24
    s.circle(mx, my, 25, fill=rad(mx, my, 25, [(0, "#FFE9A83C"), (1, "#FFE9A800")]))
    s.circle(mx, my, 12.8, fill=rad(mx - 4, my - 4, 20, [(0, "#FFF8DA"), (.55, "#FFE9A8"), (1, "#F2D283")]),
             stroke=ha("#C9A24E", .45), sw=.6)
    s.path(circles([(mx - 3.6, my - 2.6, 2.6), (mx + 3.6, my + 3.6, 1.9), (mx + 2.8, my - 4.6, 1.2), (mx - 4.8, my + 4.4, 1.0)]), fill=ha("#D9B664", .5))
    # un jiron de nube cruza la luna
    with s.translate(63, 33):
        with s.scale(.62):
            s.path(CLOUD, fill=lin(0, -12, 0, 7, [(0, "#50388F"), (1, "#2A1A55")]), stroke=ha("#CDB8FF", .3), sw=.8, join=JOIN_ROUND)
    bat(s, 19, 23, 1.0, -8)
    bat(s, 46, 9, .7, 6)
    bat(s, 11, 52, .5, -14)
    dots(s, [(9, 40, 1.1), (92, 62, 1.3), (30, 6, 1.0), (96, 44, .9)], halo=.2)
    return s.bake()


# ------------------------------------------------------------------ PETALS

def petal_cmds(L):
    return (Path().M(-L, 0).C(-.55 * L, -.8 * L, .6 * L, -.92 * L, L, -.24 * L).L(.76 * L, 0).L(L, .24 * L)
            .C(.6 * L, .92 * L, -.55 * L, .8 * L, -L, 0).Z()).cmds


def scene_petals():
    s = new()
    rnd = random.Random(21)
    sizes = [5.6, 5.0, 4.6, 5.2, 4.2, 4.8, 3.8, 4.4, 5.4, 4.0, 4.6, 3.6, 4.2]
    cols = [("#FF86AA", "#FFC6D8"), ("#FF93B3", "#FFD0DF"), ("#FF7FA6", "#FFBDD2")]
    veins, shines = [], []
    for i, (x, y, L) in enumerate(scatter(sizes, 33, gap=.8)):
        c1, c2 = cols[i % 3]
        rot = rnd.uniform(-180, 180)
        a, b = pt(-L, 0, x, y, rot), pt(L, 0, x, y, rot)
        s.path(place(petal_cmds(L), x, y, rot), fill=lin(a[0], a[1], b[0], b[1], [(0, c1), (1, c2)]),
               stroke=ha("#9A2E5C", .4), sw=.45, join=JOIN_ROUND)
        veins += place([("M", -L * .85, 0), ("L", L * .35, 0)], x, y, rot)
        shines += place(_ellipse_cmds(-.1 * L, -.36 * L, .45 * L, .13 * L), x, y, rot)
    s.path(veins, stroke=ha("#D9487C", .45), sw=.35, cap=CAP_ROUND)
    s.path(shines, fill=ha(W, .55))
    return s.bake()


# ------------------------------------------------------------------ BUBBLES

def scene_bubbles():
    s = new()
    sizes = [7.8, 6.2, 5.0, 4.6, 3.8, 3.2, 3.0, 4.2, 5.6, 3.4]
    pts = scatter(sizes, 7, gap=1.6)
    s.path(circles([(x, y, r + .55) for (x, y, r) in pts]), stroke=RIM, sw=.6)
    for (x, y, r) in pts:
        s.circle(x, y, r, fill=rad(x - r * .25, y - r * .3, r * 1.3, [(0, ha("#FFFFFF", .06)), (.62, ha("#BFEFFF", .22)), (1, ha("#CFE9FF", .6))]),
                 stroke=ha(W, .9), sw=max(.55, r * .1))
    arcs, pink, spots = [], [], []
    for (x, y, r) in pts:
        arcs += arc_cmds(x, y, r * .7, 198, 262)
        pink += arc_cmds(x, y, r * .7, 20, 55)
        spots += [(x - r * .52, y - r * .5, max(.45, r * .11))]
    s.path(arcs, stroke=ha(W, .95), sw=.9, cap=CAP_ROUND)
    s.path(pink, stroke=ha("#FFC4E6", .9), sw=.6, cap=CAP_ROUND)
    s.path(circles(spots), fill=W)
    return s.bake()


# ------------------------------------------------------------------ CONFETTI

CONF = [("#FF6F9B", "#B8325F"), ("#FFD34A", "#B8861A"), ("#5CE1E6", "#1F8F9A"), ("#B57CF0", "#6B3FB0"), ("#8EE3B0", "#3E9A66")]


def scene_confetti():
    s = new()
    rnd = random.Random(3)
    sizes = [4.8, 4.4, 4.0, 5.0, 4.2, 4.6, 3.8, 4.8, 4.3, 4.0, 4.6, 3.8, 5.0, 4.2, 4.4, 3.8, 4.6]
    fills = [[] for _ in CONF]
    lines = [[] for _ in CONF]
    kinds = [0, 1, 2, 3, 0, 0, 1, 3]
    for i, (x, y, r) in enumerate(scatter(sizes, 17, gap=.6)):
        k = kinds[i % 8]
        ci = i % 5
        rot = rnd.uniform(0, 180)
        if k == 0:
            fills[ci] += poly_cmds([pt(a, b, x, y, rot) for (a, b) in [(-r, -r * .55), (r, -r * .55), (r, r * .55), (-r, r * .55)]])
        elif k == 1:
            fills[ci] += _ellipse_cmds(x, y, r * .62, r * .62)
        elif k == 2:
            fills[ci] += poly_cmds([pt(a, b, x, y, rot) for (a, b) in [(-r * .85, r * .65), (0, -r * .85), (r * .85, r * .65)]])
        else:
            lines[ci] += catmull([pt(a, b, x, y, rot) for (a, b) in [(-r, 0), (-r * .5, -r * .6), (0, 0), (r * .5, r * .6), (r, 0)]])
    for ci, (base, dark) in enumerate(CONF):
        if fills[ci]:
            s.path(fills[ci], fill=base, stroke=ha(dark, .55), sw=.4, join=JOIN_ROUND)
        if lines[ci]:
            s.path(lines[ci], stroke=base, sw=1.3, cap=CAP_ROUND, join=JOIN_ROUND)
    return s.bake()


# ------------------------------------------------------------------ AURORA

def scene_aurora():
    s = new()
    bands = [("#4FD9B4", "#C4FFEE"), ("#68AEFF", "#D8EBFF"), ("#9C80FF", "#E0D6FF")]
    for k, (col, lite) in enumerate(bands):
        base = 18 + k * 11
        pts = []
        x = -4
        while x <= 104:
            pts.append((x, base + math.sin(x / 22.0 + k * 1.3) * 7))
            x += 8
        cmds = [("M", 0, 0), ("L", pts[0][0], pts[0][1])] + catmull(pts)[1:] + [("L", 104, 0), ("Z",)]
        hem = base + 9
        s.path(cmds, fill=lin(0, 0, 0, hem, [(0, ha(col, 0)), (.55, ha(col, .34)), (1, ha(col, .8))]))
        rays = []
        for j in range(8):
            xx = 6 + j * 13 + k * 4
            yy = base + math.sin(xx / 22.0 + k * 1.3) * 7
            rays += [("M", xx, yy - 1), ("L", xx, yy - 13 - (j % 3) * 3)]
        s.path(rays, stroke=lin(0, 0, 0, hem, [(0, ha(lite, 0)), (1, ha(lite, .55))]), sw=1.2)
        s.curve(pts, ha(lite, .7), .9)
    dots(s, [(14, 56, 1.3), (88, 62, 1.5), (8, 31, 1.2), (92, 38, 1.1), (30, 7, 1.0), (95, 22, 1.0)], halo=.2)
    glint(s, 85, 48, 3.6)
    return s.bake()


# ------------------------------------------------------------------ FLAMES

def flame(s, x, base, h, w, lean, c_bot, c_mid, c_top):
    p = (Path().M(x - w, base).C(x - w * 1.3, base - h * .45, x - w * .35, base - h * .62, x + lean, base - h)
         .C(x + w * .3, base - h * .6, x + w * 1.4, base - h * .5, x + w, base).Z())
    s.path(p, fill=lin(0, base, 0, base - h, [(0, c_bot), (.5, c_mid), (1, c_top)]))


def scene_flames():
    s = new()
    for gx in (6, 94):
        s.circle(gx, 86, 42, fill=rad(gx, 86, 42, [(0, "#FFB34735"), (1, "#FF8A1F00")]))
    base = 99
    left = [(7.5, 38, 5.2, -1.5), (13.5, 52, 6.2, 2), (20, 40, 5.4, -2)]
    right = [(92.5, 40, 5.2, 1.5), (86, 50, 6.2, -2), (79.5, 36, 5.2, 2)]
    for (x, h, w, lean) in left + right:
        flame(s, x, base, h, w, lean, "#E8432A", "#FF8A1F", "#FFB347")
        flame(s, x, base, h * .72, w * .66, lean * .6, "#FF8A1F", "#FFB02E", "#FFD34A")
        flame(s, x, base, h * .42, w * .36, lean * .3, "#FFC23A", "#FFE27A", "#FFF6C0")
    yel = [(10, 36, 1.5), (91, 32, 1.4), (95, 48, 1.2), (72, 16, 1.2)]
    ora = [(19, 30, 1.1), (5, 50, 1.2), (84, 26, 1.1), (27, 20, 1.0)]
    s.path(circles(yel + ora, 3.0), fill="#FFB34730")
    s.path(circles(yel), fill="#FFD34A")
    s.path(circles(ora), fill="#FFB347")
    return s.bake()


# ------------------------------------------------------------------ adornos de AUTO

def scene_auto_midnight():
    s = new()
    glow(s, 15, 17, 9, W, .45)
    glow(s, 91, 54, 8, W, .4)
    dots(s, [(15, 17, 1.9), (84, 22, 1.5), (75, 10, 1.2), (10, 44, 1.3), (91, 54, 1.6)], halo=.2)
    glint(s, 15, 17, 4.8)
    glint(s, 91, 54, 3.8, tint="#D8DCFF")
    return s.bake()


def scene_auto_gold():
    s = new()
    rim = ha("#9A5F0C", .55)
    for (x, y, r) in [(15, 19, 5.4), (87, 30, 4.2), (79, 9, 3.2)]:
        glow(s, x, y, r * 2, "#FFF3C4", .6)
        glint(s, x, y, r, tint="#FFD25A", rim=rim)
    dots(s, [(24, 9, 1.2), (93, 19, 1.1)], rim=ha("#9A5F0C", .4))
    return s.bake()


def build():
    icons = {
        "scene.SPARKLES": scene_sparkles(),
        "scene.RAYS": scene_rays(),
        "scene.STARS": scene_stars(),
        "scene.CLOUDS": scene_clouds(),
        "scene.HEARTS": scene_hearts(),
        "scene.SNOW": scene_snow(),
        "scene.SPOOKY": scene_spooky(),
        "scene.PETALS": scene_petals(),
        "scene.BUBBLES": scene_bubbles(),
        "scene.CONFETTI": scene_confetti(),
        "scene.AURORA": scene_aurora(),
        "scene.FLAMES": scene_flames(),
        "scene.AUTO_MIDNIGHT": scene_auto_midnight(),
        "scene.AUTO_GOLD": scene_auto_gold(),
    }
    return {"icons": icons}
