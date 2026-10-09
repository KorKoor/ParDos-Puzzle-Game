"""
ui_icons_b: rangos de prestigio, medallas, copa de platino, cofres y extras de interfaz.
Ids: rank.<RANGO>, rank.PLATINUM, trophy.<TIER>, trophy.platinum, chest.<rareza>.<closed|open>, chest.glow, ui.*
"""
import os, sys, math
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, ".."))   # artlib
sys.path.insert(0, HERE)                        # _looks, _kit
from artlib import *

INK = "#2B1B3A"
WHITE = "#FFFFFF"


# ------------------------------------------------------------------ colores (se calculan aqui: el JSON queda pequeno)

def _rgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def M(a, b, t):
    ca, cb = _rgb(a), _rgb(b)
    return "#%02X%02X%02X" % tuple(int(round(ca[i] + (cb[i] - ca[i]) * t)) for i in range(3))


def L(c, t=0.4):
    return M(c, WHITE, t)


def D(c, t=0.3):
    return M(c, INK, t)


def A(c, a):
    return c[:7] + "%02X" % int(round(max(0.0, min(1.0, a)) * 255))


def vg(top, bot, y1, y2):
    return lin(0, y1, 0, y2, [(0, top), (1, bot)])


def dg(c, x1, y1, x2, y2, hi=0.4, lo=0.22):
    """Masa de color con luz arriba-izquierda y sombra abajo-derecha."""
    return lin(x1, y1, x2, y2, [(0, L(c, hi)), (0.55, c), (1, D(c, lo))])


def tg(light, mid, dark, x1, y1, x2, y2):
    return lin(x1, y1, x2, y2, [(0, light), (0.5, mid), (1, dark)])


def ngon(cx, cy, r, n, rot=-90.0):
    return [(cx + r * math.cos(math.radians(rot + i * 360.0 / n)),
             cy + r * math.sin(math.radians(rot + i * 360.0 / n))) for i in range(n)]


def rpoly(s, pts, fill, rr=3.0, outline=None, ow=1.3):
    """Poligono con esquinas redondeadas (el trazo del mismo relleno redondea) y contorno fino opcional."""
    # se empieza a mitad de un lado para que el cierre no deje muesca en el vertice
    m = ((pts[0][0] + pts[1][0]) / 2.0, (pts[0][1] + pts[1][1]) / 2.0)
    q = [m] + list(pts[1:]) + [pts[0]]
    if outline:
        s.poly(q, fill=outline, stroke=outline, sw=2 * rr + 2 * ow, join=JOIN_ROUND)
    s.poly(q, fill=fill, stroke=fill, sw=2 * rr, join=JOIN_ROUND)


def shadow(s, cx=50, cy=91, rx=26, ry=3.6, a=0.2):
    s.ellipse(cx, cy, rx, ry, fill=A(INK, a))


def gloss(s, cx, cy, rx, ry, a=0.55):
    s.ellipse(cx, cy, rx, ry, fill=A(WHITE, a))


def feather(s, x, y, ang, ln, wd, fill, edge=None, ew=0.8):
    """Hoja o pluma: base en (x, y), apunta en la direccion `ang` (grados, 0 = derecha)."""
    with s.rotate(ang, x, y):
        p = Path().M(x, y).C(x + ln * 0.25, y - wd * 1.5, x + ln * 0.9, y - wd * 1.3, x + ln, y)
        p.C(x + ln * 0.9, y + wd * 1.3, x + ln * 0.25, y + wd * 1.5, x, y).Z()
        s.path(p, fill=fill, stroke=edge, sw=ew if edge else 0, join=JOIN_ROUND)


def leaf_fill(x, y, ln, light, mid):
    return lin(x, y, x + ln, y, [(0, mid), (1, light)])


def laurel_branch(s, cx, cy, r, a0, a1, n, ln, wd, light, mid, dark, stem=None, sw=1.6):
    """Rama de laurel sobre un arco (angulos en grados; a0 -> a1 marca el sentido del crecimiento)."""
    dirn = 1 if a1 > a0 else -1
    stem = stem or dark
    s.arc_stroke(cx, cy, r, a0, a1, stem, sw)
    for i in range(n):
        t = i / float(max(1, n - 1))
        a = a0 + (a1 - a0) * t
        px = cx + r * math.cos(math.radians(a))
        py = cy + r * math.sin(math.radians(a))
        tang = a + 90.0 * dirn
        k = 1.0 - 0.32 * t
        for side in (-1, 1):
            ang = tang + 40.0 * side * dirn * (-1)
            f = leaf_fill(px, py, ln * k, light if side == -1 else mid, mid if side == -1 else dark)
            feather(s, px, py, ang, ln * k, wd * k, f, edge=D(dark, .35), ew=0.7)
    # hoja de la punta
    a = a1
    px = cx + r * math.cos(math.radians(a))
    py = cy + r * math.sin(math.radians(a))
    feather(s, px, py, a + 90.0 * dirn, ln * 0.8, wd * 0.75, leaf_fill(px, py, ln * .8, light, mid), edge=D(dark, .35), ew=0.7)


def small_star(s, x, y, r, fill, edge=None, inner=0.46):
    s.star(x, y, r, 5, inner, fill=fill, stroke=edge, sw=0.8 if edge else 0)


def gem_stud(s, x, y, r, c):
    s.circle(x, y, r, fill=dg(c, x - r, y - r, x + r, y + r, .55, .25), stroke=D(c, .5), sw=0.8)
    gloss(s, x - r * .3, y - r * .35, r * .35, r * .25, .8)


# ------------------------------------------------------------------ rangos

RANK_DEF = [
    ("NOVICE",      "#D9DDE6", "#9EA6B6", "#5E6678"),
    ("APPRENTICE",  "#F1C9A0", "#CD8A4B", "#8A4F22"),
    ("ADEPT",       "#E6EEFA", "#9DB8DC", "#557AA8"),
    ("EXPERT",      "#B8F0D3", "#4FC08D", "#1F7A55"),
    ("MASTER",      "#FFEFA8", "#F2B83B", "#B27A12"),
    ("GRANDMASTER", "#E2C8FF", "#9B6BE0", "#5B34A6"),
    ("LEGEND",      "#FFC2B8", "#E5533D", "#8E1F18"),
    ("MYTHIC",      "#FFFFFF", "#8FD3FF", "#6A4CE0"),
]


def wing(s, x, y, flip, light, mid, dark):
    """Ala de plumas que sale del hombro (x, y) hacia la izquierda; flip la pone a la derecha."""
    feats = [(165, 17), (178, 23), (191, 26), (204, 25), (217, 22), (230, 18), (243, 13)]
    ctx = s.flip_x(50) if flip else s.translate(0, 0)
    with ctx:
        for ang, ln in feats:
            f = lin(x, y, x + ln * math.cos(math.radians(ang)), y + ln * math.sin(math.radians(ang)), [(0, mid), (1, light)])
            feather(s, x, y, ang, ln, 4.3, f, edge=D(dark, .3), ew=0.8)


def rank_badge(idx):
    name, light, mid, dark = RANK_DEF[idx]
    s = Scene(100, 100)
    cx, cy, R = 50, 54, 27
    edge = D(dark, .4)
    mythic = idx == 7

    # halo
    if idx >= 4:
        s.circle(cx, cy, 45, fill=rad(cx, cy, 45, [(0.4, A(mid, .5)), (1, A(mid, 0))]))
    # rayos de la leyenda
    if idx == 6:
        for k in range(12):
            a = math.radians(k * 30 + 15)
            rl = 44 if k % 2 == 0 else 38
            hw = 0.17
            s.poly([(cx + 14 * math.cos(a - hw), cy + 14 * math.sin(a - hw)),
                    (cx + rl * math.cos(a), cy + rl * math.sin(a)),
                    (cx + 14 * math.cos(a + hw), cy + 14 * math.sin(a + hw))],
                   fill=rad(cx, cy, rl, [(0.35, A(L(mid, .3), .85)), (1, A(mid, .15))]))
    # alas del mitico
    if mythic:
        wl, wm, wd_ = "#FFFFFF", "#BFE6FF", "#6A4CE0"
        wing(s, 28, 56, False, wl, wm, wd_)
        wing(s, 28, 56, True, wl, wm, wd_)
    # laurel
    if 3 <= idx <= 6:
        span, n, ln, wd = {3: (70, 4, 9, 3.0), 4: (100, 6, 9.5, 3.2), 5: (135, 8, 10, 3.4), 6: (135, 8, 10, 3.4)}[idx]
        ll, lm, ld = L(mid, .55), mid, dark
        rr = 34
        laurel_branch(s, cx, cy, rr, 100, 100 + span, n, ln, wd, ll, lm, ld)
        laurel_branch(s, cx, cy, rr, 80, 80 - span, n, ln, wd, ll, lm, ld)
    shadow(s, cx, 90, 24, 3.6)

    # cuerpo hexagonal
    pts = ngon(cx, cy, R, 6)
    if mythic:
        body = lin(cx - R, cy - R, cx + R, cy + R, [(0, "#FFFFFF"), (0.3, "#FFD0F0"), (0.62, "#9FD8FF"), (1, "#6A4CE0")])
    else:
        body = lin(cx - R, cy - R, cx + R, cy + R, [(0, L(light, .35)), (0.4, light), (0.62, mid), (1, dark)])
    rpoly(s, [(x, y + 2.2) for x, y in pts], A(INK, .22), rr=3.2)
    rpoly(s, pts, body, rr=3.2, outline=edge, ow=1.3)
    # contorno de luz interior
    # placa hundida
    R2 = 18.5
    pts2 = ngon(cx, cy, R2, 6)
    if mythic:
        plate = lin(0, cy - R2, 0, cy + R2, [(0, "#4A34B8"), (1, "#8D7BE8")])
    else:
        plate = vg(M(dark, INK, .15), M(mid, dark, .35), cy - R2, cy + R2)
    rpoly(s, pts2, plate, rr=2.0, outline=A(WHITE, .5), ow=0.9)
    # estrella
    sr = 13.2
    star_fill = vg(WHITE, L(light, .1) if not mythic else "#CFE9FF", cy - sr, cy + sr)
    s.star(cx, cy + 2.0, sr, 5, .47, fill=A(INK, .32), stroke=A(INK, .32), sw=1.2, round_join=True)
    s.star(cx, cy + .3, sr, 5, .47, fill=star_fill, stroke=D(mid, .45), sw=1.0, round_join=True)
    gloss(s, cx - 3.2, cy - 5.6, 2.2, 1.2, .85)
    # brillo del borde
    ins = lambda p, t: (p[0] + (cx - p[0]) * t, p[1] + (cy - p[1]) * t)
    a = ins(pts[5], .13)
    b = ins(pts[0], .13)
    c = ins(pts[1], .06)
    s.curve([a, ((a[0] + b[0]) / 2, (a[1] + b[1]) / 2 - .4), b], A(WHITE, .6), 2.0)
    # tachuelas en los vertices
    if idx >= 2:
        for k in (1, 2, 4, 5):
            gem_stud(s, pts[k][0], pts[k][1], 2.7, L(mid, .5) if not mythic else "#FFFFFF")
    # peldanos: tantas estrellitas como rango
    n = idx
    if n > 0:
        step = 12.5
        for i in range(n):
            ang = -90 + (i - (n - 1) / 2.0) * step
            px = cx + 41 * math.cos(math.radians(ang))
            py = cy + 41 * math.sin(math.radians(ang))
            pf = vg(L(light, .5), mid, py - 3.7, py + 3.7) if not mythic else vg("#FFFFFF", "#9FD8FF", py - 3.7, py + 3.7)
            small_star(s, px, py, 3.9, pf, edge=D(dark, .35))
    if mythic:
        s.sparkle(88, 30, 6, fill=WHITE)
        s.sparkle(14, 74, 5, fill=WHITE)
        s.sparkle(86, 80, 4, fill="#DFF3FF")
    return s.bake()


# ------------------------------------------------------------------ gemas (se reutilizan en medallas, copa, cofres y montones)

def gem(s, cx, cy, w, h, c, outline=True, sheen=True):
    """Gema de talla brillante con facetas. c = color base."""
    x0, x1 = cx - w / 2.0, cx + w / 2.0
    yt = cy - h / 2.0
    yg = yt + h * 0.30
    yb = cy + h / 2.0
    tl, tr = cx - w * 0.27, cx + w * 0.27      # mesa
    gl, gr = cx - w * 0.17, cx + w * 0.17      # facetas del cinturon
    ln = D(c, .5)
    sw = max(0.7, w * 0.028)
    hi, lo = L(c, .75), D(c, .22)
    # silueta con contorno
    if outline:
        s.poly([(x0, yg), (tl, yt), (tr, yt), (x1, yg), (cx, yb)], fill=ln, stroke=ln, sw=sw * 2.4 + 1.0, join=JOIN_ROUND)
    # corona
    s.poly([(tl, yt), (tr, yt), (cx + w * .2, yg), (cx - w * .2, yg)], fill=vg(L(c, .85), L(c, .45), yt, yg))
    s.poly([(x0, yg), (tl, yt), (cx - w * .2, yg)], fill=lin(x0, yt, cx, yg, [(0, L(c, .55)), (1, c)]))
    s.poly([(x1, yg), (tr, yt), (cx + w * .2, yg)], fill=lin(x1, yt, cx, yg, [(0, c), (1, M(c, lo, .55))]))
    # pabellon
    s.poly([(x0, yg), (cx - w * .2, yg), (cx, yb)], fill=lin(x0, yg, cx, yb, [(0, c), (1, M(c, lo, .35))]))
    s.poly([(cx - w * .2, yg), (cx + w * .2, yg), (cx, yb)], fill=lin(cx, yg, cx, yb, [(0, L(c, .35)), (1, c)]))
    s.poly([(cx + w * .2, yg), (x1, yg), (cx, yb)], fill=lin(cx + w * .2, yg, cx + w * .3, yb, [(0, M(c, lo, .3)), (1, M(c, lo, .75))]))
    # aristas
    k = A(WHITE, .55)
    s.poly([(x0, yg), (tl, yt), (tr, yt), (x1, yg), (cx, yb)], stroke=A(ln, .0) if False else ln, sw=sw, join=JOIN_ROUND)
    s.line(x0, yg, x1, yg, k, sw * .8)
    s.line(cx - w * .2, yg, cx, yb, A(WHITE, .35), sw * .7)
    s.line(cx + w * .2, yg, cx, yb, A(WHITE, .25), sw * .7)
    s.line(tl, yt, cx - w * .2, yg, A(WHITE, .45), sw * .7)
    s.line(tr, yt, cx + w * .2, yg, A(WHITE, .35), sw * .7)
    if sheen:
        gloss(s, cx - w * .17, yt + h * .1, w * .09, h * .045, .85)


# ------------------------------------------------------------------ medallas de trofeo

TIER_DEF = {
    "BRONZE":  ("#F4C79B", "#CD8A4B", "#8A4F22"),
    "SILVER":  ("#FFFFFF", "#C3CAD8", "#7C869B"),
    "GOLD":    ("#FFF3B0", "#F2B83B", "#B27A12"),
    "DIAMOND": ("#FFFFFF", "#8FD3FF", "#4A7FE0"),
}


def medal_ribbons(s, ytop=5, ybot=44):
    red, red2 = "#E5533D", "#B4413C"
    left = [(22, ytop), (40, ytop), (60, ybot), (42, ybot)]
    right = [(78, ytop), (60, ytop), (40, ybot), (58, ybot)]
    rpoly(s, left, lin(22, ytop, 60, ybot, [(0, L(red, .25)), (1, red2)]), rr=1.2, outline=D(red2, .45), ow=1.1)
    rpoly(s, right, lin(78, ytop, 40, ybot, [(0, red), (1, D(red2, .25))]), rr=1.2, outline=D(red2, .45), ow=1.1)
    s.line(30, ytop + 3, 49, ybot - 4, A(WHITE, .28), 1.4)
    s.line(70, ytop + 3, 51, ybot - 4, A(WHITE, .18), 1.4)


def trophy_medal(tier):
    light, mid, dark = TIER_DEF[tier]
    s = Scene(100, 100)
    cx, cy = 50, 61
    medal_ribbons(s)
    if tier == "DIAMOND":
        # anillo y gema
        s.circle(50, 33, 4.6, stroke=D(dark, .3), sw=3.0)
        s.circle(50, 33, 4.6, stroke=L(mid, .4), sw=1.6)
        s.ellipse(50, 90, 26, 3.4, fill=A(INK, .18))
        s.poly([(25, 54), (38, 39), (62, 39), (75, 54), (50, 88)], fill=A(INK, .22), stroke=A(INK, .22), sw=3, join=JOIN_ROUND)
        gem(s, 50, 63, 52, 50, "#8FD3FF")
        s.sparkle(24, 40, 6, fill=WHITE)
        s.sparkle(78, 74, 4.5, fill=WHITE)
        return s.bake()
    R = 26
    edge = D(dark, .4)
    s.circle(50, 33, 4.6, stroke=D(dark, .3), sw=3.0)
    s.circle(50, 33, 4.6, stroke=L(mid, .4), sw=1.6)
    s.ellipse(50, 91, 24, 3.4, fill=A(INK, .18))
    s.circle(cx, cy + 2.2, R, fill=A(INK, .22))
    s.circle(cx, cy, R, fill=tg(L(light, .3), mid, dark, cx - R, cy - R, cx + R, cy + R), stroke=edge, sw=1.4)
    # muescas del borde
    for k in range(16):
        a = math.radians(k * 22.5)
        s.circle(cx + 22.6 * math.cos(a), cy + 22.6 * math.sin(a), 1.05, fill=A(D(dark, .3), .45))
    # disco interior hundido
    s.circle(cx, cy, 19.5, fill=lin(cx, cy - 19.5, cx, cy + 19.5, [(0, M(mid, dark, .45)), (1, L(mid, .12))]), stroke=A(WHITE, .6), sw=1.0)
    # estrella
    sr = 12.6
    s.star(cx, cy + 1.9, sr, 5, .47, fill=A(INK, .3), stroke=A(INK, .3), sw=1.0)
    s.star(cx, cy + .3, sr, 5, .47, fill=vg(L(light, .7), light if tier != "SILVER" else mid, cy - sr, cy + sr), stroke=D(mid, .4), sw=1.0)
    gloss(s, cx - 3, cy - 5, 2.2, 1.2, .85)
    # brillo del aro
    s.arc_stroke(cx, cy, 23.2, 196, 252, A(WHITE, .7), 2.2)
    s.arc_stroke(cx, cy, 23.2, 262, 276, A(WHITE, .5), 2.2)
    return s.bake()


# ------------------------------------------------------------------ copa de platino

def crown(s, cx, y_base, w, h, body, gem_c):
    x0, x1 = cx - w / 2.0, cx + w / 2.0
    pts = [(x0, y_base), (x0 - 0.5, y_base - h * .8), (cx - w * .25, y_base - h * .45), (cx, y_base - h),
           (cx + w * .25, y_base - h * .45), (x1 + 0.5, y_base - h * .8), (x1, y_base)]
    rpoly(s, pts, lin(0, y_base - h, 0, y_base, [(0, L(body, .5)), (1, D(body, .15))]), rr=1.2, outline=D(body, .5), ow=1.0)
    for px, py in ((x0 - .5, y_base - h * .8), (cx, y_base - h), (x1 + .5, y_base - h * .8)):
        s.circle(px, py - .5, 2.2, fill=L(gem_c, .5), stroke=D(gem_c, .45), sw=.8)
    s.rect(x0 + 1, y_base - h * .22, w - 2, h * .22, r=1, fill=D(body, .12), stroke=D(body, .5), sw=.8)
    gem(s, cx, y_base - h * .38, w * .26, w * .3, gem_c, outline=False, sheen=False)


def platinum_cup():
    s = Scene(100, 100)
    pl, pm, pd = "#FFFFFF", "#CFD8F1", "#7F8BBE"
    edge = "#5A648F"
    s.circle(50, 48, 47, fill=rad(50, 48, 47, [(0.3, A("#BFD9FF", .85)), (0.65, A("#C9B8FF", .3)), (1, A("#BFD9FF", 0))]))
    # peana
    s.ellipse(50, 90.5, 28, 3.4, fill=A(INK, .2))
    rpoly(s, [(26, 83), (74, 83), (74, 90), (26, 90)], vg("#7B7FD0", "#3E3F86", 83, 90), rr=1.5, outline="#2C2D63", ow=1.0)
    s.rect(28, 84, 44, 1.8, r=.9, fill=A(WHITE, .35))
    rpoly(s, [(33, 77), (67, 77), (67, 83), (33, 83)], lin(0, 77, 0, 83, [(0, L(pm, .6)), (1, pd)]), rr=1.5, outline=edge, ow=1.0)
    # tallo
    stem = Path().M(44, 60).L(56, 60).C(56, 66, 58.5, 72, 61, 77.5).L(39, 77.5).C(41.5, 72, 44, 66, 44, 60).Z()
    s.path(stem, fill=lin(39, 0, 61, 0, [(0, pm), (0.32, pl), (1, pd)]), stroke=edge, sw=1.2, join=JOIN_ROUND)
    s.ellipse(50, 70.5, 9.2, 3.8, fill=lin(41, 0, 59, 0, [(0, pm), (0.32, pl), (1, pd)]), stroke=edge, sw=1.1)
    # asas
    for flip in (False, True):
        with (s.flip_x(50) if flip else s.translate(0, 0)):
            h = Path().M(25, 19).C(5, 17, 4, 47, 32, 46)
            s.path(h, stroke=edge, sw=6.6, cap=CAP_ROUND)
            s.path(h, stroke=lin(4, 17, 30, 47, [(0, pl), (0.5, pm), (1, pd)]), sw=4.4, cap=CAP_ROUND)
            s.path(Path().M(23, 20).C(9.5, 18.5, 8.5, 38, 18, 43), stroke=A(WHITE, .75), sw=1.1, cap=CAP_ROUND)
    # cuenco
    bowl = Path().M(21, 14).L(79, 14).C(80, 39, 68, 57, 53, 61).L(47, 61).C(32, 57, 20, 39, 21, 14).Z()
    s.path(bowl, fill=lin(21, 14, 79, 62, [(0, pl), (0.45, pm), (1, pd)]), stroke=edge, sw=1.4, join=JOIN_ROUND)
    # reflejo lateral y banda
    s.path(Path().M(26, 20).C(27, 38, 33, 48, 42, 55), stroke=A(WHITE, .75), sw=2.2, cap=CAP_ROUND)
    s.path(Path().M(23.5, 25).C(35, 31, 65, 31, 76.5, 25), stroke=A(edge, .35), sw=1.0, cap=CAP_ROUND)
    # boca de la copa
    s.ellipse(50, 14, 29, 4.4, fill=lin(0, 9, 0, 19, [(0, "#9AA6D4"), (1, "#E8EDFB")]), stroke=edge, sw=1.4)
    s.ellipse(50, 14.8, 25.5, 2.9, fill=lin(0, 12, 0, 18, [(0, "#6772A8"), (1, "#A9B4DE")]))
    s.ellipse(50, 12.6, 22, 1.0, fill=A(WHITE, .35))
    # emblema: diamante
    s.ellipse(50, 41.5, 10.5, 12.5, fill=A(INK, .12))
    gem(s, 50, 35, 17, 20, "#8FD3FF")
    small_star(s, 33, 32, 2.6, vg(WHITE, "#CFD8F1", 29, 35), edge=edge)
    small_star(s, 67, 32, 2.6, vg(WHITE, "#CFD8F1", 29, 35), edge=edge)
    # destellos
    s.sparkle(13, 11, 6.5, fill=WHITE)
    s.sparkle(89, 20, 5.2, fill="#EAF3FF")
    s.sparkle(80, 5.5, 3.6, fill=WHITE)
    s.sparkle(12, 60, 3.8, fill="#EAF3FF")
    return s.bake()


def rank_platinum():
    s = Scene(100, 100)
    pl, pm, pd = "#FFFFFF", "#CFD8F1", "#7F8BBE"
    edge = "#5A648F"
    cx, cy = 50, 57
    s.circle(cx, cy, 46, fill=rad(cx, cy, 46, [(0.4, A("#BFD9FF", .8)), (0.7, A("#C9B8FF", .25)), (1, A("#BFD9FF", 0))]))
    # laurel de plata
    ll, lm, ld = "#FFFFFF", "#C4D0F0", "#6D7AAE"
    laurel_branch(s, cx, cy, 33, 95, 245, 9, 10, 3.3, ll, lm, ld)
    laurel_branch(s, cx, cy, 33, 85, -65, 9, 10, 3.3, ll, lm, ld)
    shadow(s, cx, 91, 22, 3.2)
    # medallon
    R = 24
    s.circle(cx, cy + 2.2, R, fill=A(INK, .22))
    s.circle(cx, cy, R, fill=tg("#FFFFFF", "#C9D4F0", "#6C78AC", cx - R, cy - R, cx + R, cy + R), stroke=edge, sw=1.5)
    for k in range(20):
        a = math.radians(k * 18)
        s.circle(cx + 21 * math.cos(a), cy + 21 * math.sin(a), .95, fill=A(edge, .4))
    s.circle(cx, cy, 17.5, fill=lin(cx, cy - 18, cx, cy + 18, [(0, "#4B3FA8"), (1, "#8C84E0")]), stroke=A(WHITE, .7), sw=1.0)
    s.arc_stroke(cx, cy, 20.5, 196, 252, A(WHITE, .75), 2.0)
    # diamante
    gem(s, cx, cy + 1, 22, 25, "#9ADCFF")
    # corona
    crown(s, cx, 29, 22, 15, "#DDE5F8", "#8FD3FF")
    s.sparkle(14, 26, 5.5, fill=WHITE)
    s.sparkle(87, 36, 4.5, fill=WHITE)
    s.sparkle(80, 82, 3.6, fill="#EAF3FF")
    return s.bake()


# ------------------------------------------------------------------ cofres (lienzo 120 x 100)

CHEST_MAT = {
    "common": dict(wl="#E0B27A", wb="#B87A3E", wd="#7A4B1F", ml="#F1D79B", m="#C9A064", md="#8A6A33",
                   gem="#7FC8A0", glow="#FFD36E", velvet="#C0504A", velvet2="#7E2D35"),
    "rare":   dict(wl="#8DB4D8", wb="#5A86B5", wd="#38587F", ml="#FFFFFF", m="#D7DEE8", md="#8E9AAD",
                   gem="#4FB4DC", glow="#9AD9FF", velvet="#3F63A8", velvet2="#22386B"),
    "epic":   dict(wl="#B69BEA", wb="#7C5CC4", wd="#4A3490", ml="#FFEA9A", m="#FFC83D", md="#B87510",
                   gem="#FF6B8A", glow="#FFB0E0", velvet="#B0306A", velvet2="#6E1B45"),
}


def _cubic_pt(p0, p1, p2, p3, t):
    u = 1 - t
    return (u ** 3 * p0[0] + 3 * u * u * t * p1[0] + 3 * u * t * t * p2[0] + t ** 3 * p3[0],
            u ** 3 * p0[1] + 3 * u * u * t * p1[1] + 3 * u * t * t * p2[1] + t ** 3 * p3[1])


def _dome_y(halves, x):
    """y del borde de una cupula hecha de curvas cubicas a la altura x."""
    for (p0, p1, p2, p3) in halves:
        lo, hi = min(p0[0], p3[0]), max(p0[0], p3[0])
        if lo - 1e-6 <= x <= hi + 1e-6:
            a, b = 0.0, 1.0
            inc = p3[0] > p0[0]
            for _ in range(40):
                m = (a + b) / 2
                px = _cubic_pt(p0, p1, p2, p3, m)[0]
                if (px < x) == inc:
                    a = m
                else:
                    b = m
            return _cubic_pt(p0, p1, p2, p3, (a + b) / 2)[1]
    return 50


def coin(s, x, y, r, base="#FFC83D"):
    s.circle(x, y, r, fill=dg(base, x - r, y - r, x + r, y + r, .6, .2), stroke=D("#B87510", .3), sw=1.0)
    s.circle(x, y, r * .62, stroke=A("#B87510", .55), sw=0.9)
    gloss(s, x - r * .38, y - r * .45, r * .3, r * .17, .85)


def strap_h(c_l, c, c_d, x0, x1):
    return lin(x0, 0, x1, 0, [(0, c_l), (0.45, c), (1, c_d)])


def chest(kind, is_open):
    m = CHEST_MAT[kind]
    s = Scene(120, 100)
    wl, wb, wd = m["wl"], m["wb"], m["wd"]
    ml, mt, md = m["ml"], m["m"], m["md"]
    edge = D(wd, .35)
    medge = D(md, .45)
    cx = 60
    s.ellipse(cx, 91.5, 43, 4.4, fill=A(INK, .22))

    def metal_v(y0, y1):
        return vg(ml, md, y0, y1) if False else lin(0, y0, 0, y1, [(0, ml), (0.5, mt), (1, md)])

    def rivet(x, y, r=1.7):
        s.circle(x, y, r, fill=md, stroke=A(INK, .25), sw=.5)
        s.circle(x - r * .3, y - r * .35, r * .42, fill=A(WHITE, .85))

    # ----- parte de atras (solo abierto): tapa vista por dentro, interior, tesoro
    if is_open:
        # tapa abierta: marco de madera y forro
        outer = Path().M(23, 52).L(23, 31).C(23, 15, 40, 8, 60, 8).C(80, 8, 97, 15, 97, 31).L(97, 52).Z()
        s.path(outer, fill=lin(0, 8, 0, 52, [(0, wb), (1, wd)]), stroke=edge, sw=2.0, join=JOIN_ROUND)
        s.path(Path().M(25.5, 33).C(26.5, 18, 42, 11, 60, 11).C(78, 11, 93.5, 18, 94.5, 33), stroke=A(WHITE, .35), sw=1.4, cap=CAP_ROUND)
        inner = Path().M(29, 51).L(29, 32).C(29, 20, 42, 14, 60, 14).C(78, 14, 91, 20, 91, 32).L(91, 51).Z()
        s.path(inner, fill=lin(0, 14, 0, 51, [(0, m["velvet"]), (1, m["velvet2"])]), stroke=D(m["velvet2"], .4), sw=1.2, join=JOIN_ROUND)
        # costuras del forro
        for dx in (-17, 17):
            s.path(Path().M(60 + dx, 51).L(60 + dx * 0.9, 19), stroke=A(WHITE, .1), sw=1.1)
        s.path(Path().M(31.5, 31).C(32, 22, 44, 17, 60, 17).C(76, 17, 88, 22, 88.5, 31), stroke=A(L(mt, .3), .7), sw=1.2, cap=CAP_ROUND)
        # remaches del marco
        for px, py in ((26, 36), (94, 36), (31, 18), (89, 18)):
            rivet(px, py, 1.6)
        # emblema de la tapa
        s.star(cx, 32, 7.2, 5, .46, fill=A(INK, .25), stroke=A(INK, .25), sw=1.0)
        s.star(cx, 31, 7.2, 5, .46, fill=lin(0, 24, 0, 38, [(0, ml), (1, mt)]), stroke=medge, sw=.9)
        # haces de luz
        for ang, wid, ln in ((-38, 10, 50), (-13, 11, 54), (13, 11, 54), (38, 10, 50)):
            a0, a1 = math.radians(-90 + ang - wid / 2.0), math.radians(-90 + ang + wid / 2.0)
            s.poly([(cx, 52), (cx + ln * math.cos(a0), 52 + ln * math.sin(a0)), (cx + ln * math.cos(a1), 52 + ln * math.sin(a1))],
                   fill=rad(cx, 52, ln, [(0.1, A(L(m["glow"], .5), .55)), (1, A(m["glow"], 0))]))
        # interior del cuerpo
        s.poly([(24, 56), (27, 46.5), (93, 46.5), (96, 56)], fill=rad(cx, 54, 42, [(0, "#FFF6C8"), (0.55, L(m["glow"], .2)), (1, M(m["glow"], "#B87510", .55))]),
               stroke=edge, sw=1.8, join=JOIN_ROUND)
        # montón de monedas y gemas
        mound = Path().M(28, 56).C(28, 46, 36, 40, 44, 41).C(50, 34, 70, 34, 76, 41).C(84, 40, 92, 46, 92, 56).Z()
        s.path(mound, fill=lin(0, 34, 0, 56, [(0, "#FFE48A"), (1, "#E0A12A")]), stroke=D("#B87510", .3), sw=1.2, join=JOIN_ROUND)
        for (x, y, r) in ((34, 50, 6.2), (86, 50, 6.2), (45, 44, 6.6), (75, 44, 6.6), (60, 41, 7.0), (52, 51, 6.0), (69, 51, 6.0)):
            coin(s, x, y, r)
        gem(s, 47, 36, 10, 12, m["gem"])
        gem(s, 72, 37, 9, 11, L(m["gem"], .1) if kind != "common" else "#F08AA8")
        gem(s, 60, 29, 13, 16, L(m["glow"], .15) if kind == "rare" else (m["gem"] if kind == "epic" else "#7FB8F0"))
        s.ellipse(cx, 53, 38, 6, fill=rad(cx, 53, 38, [(0, A("#FFFFFF", .45)), (1, A("#FFFFFF", 0))]))
        s.sparkle(30, 22, 5.5, fill="#FFF4C2")
        s.sparkle(92, 16, 6.5, fill="#FFF4C2")
        s.sparkle(60, 7, 4.2, fill=WHITE)
        s.sparkle(102, 44, 4, fill="#FFF4C2")
        s.sparkle(16, 40, 3.6, fill=WHITE)

    # ----- cuerpo (frente)
    body = Path().M(22, 56).L(98, 56).L(95, 86).Q(94, 90.5, 90, 90.5).L(30, 90.5).Q(26, 90.5, 25, 86).Z()
    s.path(body, fill=lin(0, 56, 0, 90, [(0, wl), (0.35, wb), (1, wd)]), stroke=edge, sw=2.0, join=JOIN_ROUND)
    for y in (68, 79):
        s.line(24.5, y, 95.5, y, A(wd, .5), 1.5, cap=CAP_BUTT)
        s.line(24.5, y + 1.4, 95.5, y + 1.4, A(WHITE, .16), 1.0, cap=CAP_BUTT)
    s.path(Path().M(48, 73).C(54, 70.5, 64, 70.5, 71, 74), stroke=A(wd, .25), sw=1.1, cap=CAP_ROUND)
    s.path(Path().M(34, 84).C(40, 82, 46, 82, 50, 85), stroke=A(wd, .25), sw=1.1, cap=CAP_ROUND)
    s.rect(26.5, 60, 3.4, 26, r=1.7, fill=A(WHITE, .2))
    # correas
    for x0 in (31, 77):
        s.rect(x0, 56, 12, 34, r=2.2, fill=strap_h(ml, mt, md, x0, x0 + 12), stroke=medge, sw=1.2)
        s.rect(x0 + 1.6, 57, 2.2, 31, r=1.1, fill=A(WHITE, .35))
        for y in (62, 75, 85.5):
            rivet(x0 + 6.4, y)

    # ----- tapa cerrada
    if not is_open:
        halves = [((19, 47), (19, 31), (38, 24), (60, 24)), ((60, 24), (82, 24), (101, 31), (101, 47))]
        lid = Path().M(19, 57).L(19, 47).C(19, 31, 38, 24, 60, 24).C(82, 24, 101, 31, 101, 47).L(101, 57).Z()
        s.path(lid, fill=lin(0, 24, 0, 57, [(0, wl), (0.45, wb), (1, wd)]), stroke=edge, sw=2.0, join=JOIN_ROUND)
        # vetas de la tapa
        s.path(Path().M(25, 45).C(27, 35, 42, 30, 60, 30).C(78, 30, 93, 35, 95, 45), stroke=A(wd, .28), sw=1.3, cap=CAP_ROUND)
        s.path(Path().M(31, 46).C(33, 40, 45, 36, 60, 36).C(75, 36, 87, 40, 89, 46), stroke=A(wd, .2), sw=1.1, cap=CAP_ROUND)
        # correas de la tapa
        for x0 in (31, 77):
            pts = [(x0, 56)]
            for k in range(7):
                x = x0 + 12 * k / 6.0
                pts.append((x, _dome_y(halves, x)))
            pts.append((x0 + 12, 56))
            s.poly(pts, fill=strap_h(ml, mt, md, x0, x0 + 12), stroke=medge, sw=1.2, join=JOIN_ROUND)
            s.line(x0 + 2.6, 52, x0 + 2.6, _dome_y(halves, x0 + 2.6) + 3, A(WHITE, .38), 1.8)
        # brillo de la cupula
        s.curve([(27, 41), (31, 33), (42, 28.5), (52, 26.6)], A(WHITE, .55), 3.0)
        s.sparkle(24, 31, 0.01, fill=WHITE) if False else None

    # ----- aro metalico
    s.rect(16.5, 52, 87, 8, r=3, fill=metal_v(52, 60), stroke=medge, sw=1.3)
    s.rect(20, 53.3, 80, 1.6, r=.8, fill=A(WHITE, .5))
    for x in (22, 38, 82, 98):
        rivet(x, 57.2, 1.4)

    # ----- cerradura con la gema de la rareza
    plate = Path().M(50.5, 51).L(69.5, 51).L(67.5, 68).Q(60, 75, 52.5, 68).Z()
    s.path(plate, fill=lin(0, 51, 0, 74, [(0, ml), (0.5, mt), (1, md)]), stroke=medge, sw=1.4, join=JOIN_ROUND)
    s.path(Path().M(52.5, 53).L(52.5, 63), stroke=A(WHITE, .45), sw=1.4, cap=CAP_ROUND)
    gem(s, cx, 61.5, 11, 13.5, m["gem"])

    if not is_open:
        if kind == "epic":
            s.star(cx, 38, 7.4, 5, .46, fill=A(INK, .25), stroke=A(INK, .25), sw=1.0)
            s.star(cx, 37, 7.4, 5, .46, fill=lin(0, 30, 0, 44, [(0, ml), (1, mt)]), stroke=medge, sw=.9)
            s.sparkle(100, 22, 6, fill="#FFF4C2")
            s.sparkle(16, 36, 4, fill=WHITE)
        elif kind == "rare":
            gem(s, cx, 38, 10, 12, "#BDEBFA")
            s.sparkle(98, 26, 5, fill=WHITE)
    return s.bake()


def chest_glow():
    s = Scene(120, 100)
    cx, cy = 60, 50
    s.circle(cx, cy, 50, fill=rad(cx, cy, 50, [(0, A("#FFF8D6", .95)), (0.3, A("#FFE48A", .7)), (0.65, A("#FFD36E", .28)), (1, A("#FFD36E", 0))]))
    for k in range(14):
        a = math.radians(k * (360.0 / 14) + 6)
        ln = 50 if k % 2 == 0 else 41
        hw = 0.1 if k % 2 == 0 else 0.08
        s.poly([(cx + 12 * math.cos(a - hw), cy + 12 * math.sin(a - hw)),
                (cx + ln * math.cos(a), cy + ln * math.sin(a)),
                (cx + 12 * math.cos(a + hw), cy + 12 * math.sin(a + hw))],
               fill=rad(cx, cy, ln, [(0.2, A("#FFF4C2", .7)), (1, A("#FFD36E", 0))]))
    s.sparkle(22, 24, 5, fill=A(WHITE, .9))
    s.sparkle(98, 30, 6, fill=A(WHITE, .9))
    s.sparkle(88, 78, 4, fill=A(WHITE, .9))
    s.sparkle(30, 74, 3.6, fill=A(WHITE, .9))
    return s.bake()


def build():
    icons = {}
    for idx in range(8):
        icons["rank." + RANK_DEF[idx][0]] = rank_badge(idx)
    icons["rank.PLATINUM"] = rank_platinum()
    for tier in TIER_DEF:
        icons["trophy." + tier] = trophy_medal(tier)
    icons["trophy.platinum"] = platinum_cup()
    for kind in CHEST_MAT:
        icons["chest.%s.closed" % kind] = chest(kind, False)
        icons["chest.%s.open" % kind] = chest(kind, True)
    icons["chest.glow"] = chest_glow()
    return {"icons": icons}
