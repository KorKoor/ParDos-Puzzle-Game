"""
Accesorios de avatar (parte A): CROWN HELMET HEADBAND MOON STAR SCARF FLOWERS BOW GLASSES WIZARD HEADPHONES FLOWER_CROWN.
Lienzo 100x100, mismas coordenadas que los animales (cabeza ovalada centrada en (50,58), rx 33, ry 29).
Todos los colores son fijos (ninguna ranura): el accesorio no cambia con la variante del animal.
"""
import os, sys, math
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, ".."))   # artlib
sys.path.insert(0, HERE)                        # _looks, _kit
from artlib import *


# ------------------------------------------------------------------ utilidades de color (todo se calcula a hex fijo)

def _rgb(h):
    h = h.lstrip("#")
    return [int(h[i:i + 2], 16) for i in (0, 2, 4)]


def mx(a, b, t):
    ca, cb = _rgb(a), _rgb(b)
    return "#%02X%02X%02X" % tuple(int(round(ca[i] + (cb[i] - ca[i]) * t)) for i in range(3))


def lt(c, t=.35):
    return mx(c, "#FFFFFF", t)


def dk(c, t=.2):
    return mx(c, "#000000", t)


def ed(c, t=.45):
    """Contorno: el mismo color mezclado con tinta morada, nunca negro."""
    return mx(c, "#2B1B3A", t)


def al(c, a):
    return c + "%02X" % int(round(a * 255))


WHITE = "#FFFFFF"


def g2(c, x1, y1, x2, y2, hi=.42, lo=.22):
    return lin(x1, y1, x2, y2, [(0, lt(c, hi)), (1, dk(c, lo))])


def rg(c, cx, cy, r, hi=.5, lo=.2):
    return rad(cx, cy, r, [(0, lt(c, hi)), (1, dk(c, lo))])


def shine(s, cx, cy, rx, ry, a=.6, rot=-25):
    with s.rotate(rot, cx, cy):
        s.ellipse(cx, cy, rx, ry, fill=al(WHITE, a))


def qpt(p0, p1, p2, t):
    u = 1 - t
    return (u * u * p0[0] + 2 * u * t * p1[0] + t * t * p2[0], u * u * p0[1] + 2 * u * t * p1[1] + t * t * p2[1])


def qsub(p0, p1, p2, t0, t1):
    """Trozo [t0,t1] de una curva cuadratica: (inicio, control, fin)."""
    a = qpt(p0, p1, p2, t0)
    b = qpt(p0, p1, p2, t1)
    dx = 2 * ((1 - t0) * (p1[0] - p0[0]) + t0 * (p2[0] - p1[0]))
    dy = 2 * ((1 - t0) * (p1[1] - p0[1]) + t0 * (p2[1] - p1[1]))
    return a, (a[0] + dx * (t1 - t0) / 2, a[1] + dy * (t1 - t0) / 2), b


def star_pts(cx, cy, r, inner=.45, rot=-90, n=5):
    pts = []
    for i in range(n * 2):
        rr = r if i % 2 == 0 else r * inner
        a = math.radians(rot + i * 180.0 / n)
        pts.append((cx + rr * math.cos(a), cy + rr * math.sin(a)))
    return pts


def crescent_d(c1, r1, c2, r2):
    """Camino de una luna creciente: circulo (c1, r1) menos circulo (c2, r2)."""
    (x1, y1), (x2, y2) = c1, c2
    d = math.hypot(x2 - x1, y2 - y1)
    a = (r1 * r1 - r2 * r2 + d * d) / (2 * d)
    h = math.sqrt(r1 * r1 - a * a)
    ux, uy = (x2 - x1) / d, (y2 - y1) / d
    px, py = x1 + a * ux, y1 + a * uy
    p1 = (px - h * uy, py + h * ux)
    p2 = (px + h * uy, py - h * ux)

    def arc(center, p_from, p_to, via_angle):
        cx, cy = center
        t1 = math.atan2(p_from[1] - cy, p_from[0] - cx)
        t2 = math.atan2(p_to[1] - cy, p_to[0] - cx)
        tv = via_angle
        tau = 2 * math.pi
        delta = (t2 - t1) % tau
        off = (tv - t1) % tau
        if off < delta:
            sweep, span = 1, delta
        else:
            sweep, span = 0, tau - delta
        return (1 if span > math.pi else 0), sweep

    big1, sw1 = arc(c1, p1, p2, math.atan2(-uy, -ux))
    big2, sw2 = arc(c2, p2, p1, math.atan2(-uy, -ux))
    return "M %.2f %.2f A %.2f %.2f 0 %d %d %.2f %.2f A %.2f %.2f 0 %d %d %.2f %.2f Z" % (
        p1[0], p1[1], r1, r1, big1, sw1, p2[0], p2[1], r2, r2, big2, sw2, p1[0], p1[1])


def leaf_d(cx, cy, ln, wd):
    return "M %.2f %.2f Q %.2f %.2f %.2f %.2f Q %.2f %.2f %.2f %.2f Z" % (
        cx - ln / 2, cy, cx, cy - wd, cx + ln / 2, cy, cx, cy + wd, cx - ln / 2, cy)


# ------------------------------------------------------------------ CROWN

def crown():
    s = Scene(100, 100)
    gold = "#FFD34A"
    line = "#9A5F18"
    body = "M 32 34 L 28.5 14.5 L 41 23.5 L 50 10 L 59 23.5 L 71.5 14.5 L 68 34 Q 50 40.5 32 34 Z"
    # sombra suave sobre la frente
    s.ellipse(50, 38.5, 19, 2.6, fill="#00000018")
    s.path(body, fill=lin(30, 9, 70, 40, [(0, "#FFEE9C"), (.5, gold), (1, "#DE9C1C")]))
    # banda inferior mas oscura
    s.path("M 31.2 29 Q 50 35.8 68.8 29 L 68 34 Q 50 40.5 32 34 Z", fill=lin(0, 29, 0, 40, [(0, "#F2BB3C"), (1, "#C78718")]))
    s.path("M 31.2 29 Q 50 35.8 68.8 29", stroke=al(line, .75), sw=1.1, cap=CAP_ROUND)
    # brillos
    s.path("M 33.2 27.5 L 31 17.5", stroke=al(WHITE, .6), sw=1.5, cap=CAP_ROUND)
    s.path("M 47.8 15.5 L 45.6 22.5", stroke=al(WHITE, .5), sw=1.3, cap=CAP_ROUND)
    # contorno
    s.path(body, stroke=line, sw=1.7, join=JOIN_ROUND)
    # gemas de las puntas
    for (x, y) in ((28.5, 14.5), (50, 10), (71.5, 14.5)):
        r = 3.1 if x == 50 else 2.8
        s.circle(x, y, r, fill=rad(x - .8, y - .9, r * 1.5, [(0, "#FFB3C4"), (.5, "#FF6B8A"), (1, "#D63A62")]), stroke=al(line, .8), sw=.9)
        s.circle(x - r * .35, y - r * .4, r * .32, fill=al(WHITE, .85))
    # gema central y perlas
    s.path("M 50 30.6 L 54.2 34.4 L 50 38.2 L 45.8 34.4 Z", fill=lin(46, 31, 54, 38, [(0, "#9BE3F5"), (1, "#4FA8D8")]), stroke=al(line, .85), sw=.9, join=JOIN_ROUND)
    s.path("M 48.2 33.6 L 50 31.8 L 51.2 33.6", stroke=al(WHITE, .8), sw=.9, cap=CAP_ROUND)
    for x in (38.6, 61.4):
        s.circle(x, 33.2, 1.7, fill=rg("#FFFFFF", x - .5, 32.6, 2.4, hi=0, lo=.18), stroke=al(line, .55), sw=.6)
    return s.bake()


# ------------------------------------------------------------------ HELMET

def helmet():
    s = Scene(100, 100)
    cx, cy, R = 50, 53, 41
    # cristal de la burbuja: casi transparente en el centro, azulado en el borde
    s.circle(cx, cy, R, fill=rad(40, 40, 58, [(0, "#EAF8FF22"), (.62, "#C4E8FF30"), (1, "#8CCBF066")]))
    # borde
    s.circle(cx, cy, R, stroke=lin(15, 15, 85, 90, [(0, "#FFFFFFF2"), (.5, "#DDF0FFCC"), (1, "#A9CBE8E6")]), sw=3.2)
    s.circle(cx, cy, R - 2.4, stroke=al("#7FB6E0", .30), sw=.9)
    # reflejos
    s.arc_stroke(cx, cy, R - 6.5, 198, 252, al(WHITE, .85), 3.2)
    s.arc_stroke(cx, cy, R - 6.5, 262, 278, al(WHITE, .6), 3.2)
    s.arc_stroke(cx, cy, R - 7.0, 28, 52, al(WHITE, .38), 2.4)
    s.circle(cx - 25.5, cy - 20, 1.3, fill=al(WHITE, .9))
    # collarin metalico
    s.ellipse(50, 94, 29, 2.4, fill="#00000020")
    s.rect(16, 82.5, 68, 11.5, r=5.75, fill=lin(0, 82, 0, 94, [(0, "#F4F7FB"), (.45, "#D4DCE8"), (1, "#9EACC4")]), stroke="#6E7C99", sw=1.3)
    s.rect(21, 84.4, 58, 2.6, r=1.3, fill=al(WHITE, .7))
    for x in (24, 76):
        s.circle(x, 88.6, 1.5, fill=rg("#C9D3E3", x - .4, 88.1, 2.2, hi=.6, lo=.3), stroke=al("#6E7C99", .8), sw=.5)
    s.circle(50, 88.7, 2.1, fill=rad(49.5, 88.2, 2.6, [(0, "#FFC0AD"), (1, "#E07A5F")]), stroke=al("#8A3E2E", .8), sw=.7)
    return s.bake()


# ------------------------------------------------------------------ HEADBAND

def headband():
    s = Scene(100, 100)
    red = "#D94F4F"
    line = ed(red, .5)
    kx, ky = 80, 45.5
    # colas al viento
    for ang, ln, w in ((-10, 15, 4.8), (22, 14, 4.8)):
        with s.rotate(ang, kx, ky):
            d = ("M %.2f %.2f Q %.2f %.2f %.2f %.2f L %.2f %.2f L %.2f %.2f Q %.2f %.2f %.2f %.2f Z" % (
                kx, ky - w / 2, kx + ln * .5, ky - w / 2 - 1.8, kx + ln, ky - w / 2,
                kx + ln - 3.6, ky, kx + ln, ky + w / 2,
                kx + ln * .5, ky + w / 2 - 1.8, kx, ky + w / 2))
            s.path(d, fill=g2(red, kx, ky - 3, kx + ln, ky + 3, hi=.28, lo=.2), stroke=line, sw=1.1, join=JOIN_ROUND)
    # banda
    band = ("M 22 40.5 Q 50 45.6 78 40.5 Q 80.8 40.4 80.8 43 L 80.8 46.6 Q 80.8 49.1 78 49.3 "
            "Q 50 54.6 22 49.3 Q 19.2 49.1 19.2 46.6 L 19.2 43 Q 19.2 40.4 22 40.5 Z")
    s.ellipse(50, 56.4, 24, 2.2, fill="#00000016")
    s.path(band, fill=lin(0, 39, 0, 54, [(0, lt(red, .32)), (.55, red), (1, dk(red, .2))]), stroke=line, sw=1.4, join=JOIN_ROUND)
    s.path("M 24 41.8 Q 50 47.2 76 41.8", stroke=al(WHITE, .38), sw=1.3, cap=CAP_ROUND)
    # puntadas
    s.path("M 24 47.3 Q 50 52.6 76 47.3", stroke=al(WHITE, .28), sw=.8, cap=CAP_BUTT)
    # nudo
    s.circle(kx, ky, 4.1, fill=rg(red, kx - 1.2, ky - 1.4, 6, hi=.45, lo=.22), stroke=line, sw=1.2)
    s.path("M %.2f %.2f Q %.2f %.2f %.2f %.2f" % (kx - 2.2, ky - 0.5, kx - 0.4, ky - 2.6, kx + 1.6, ky - 1.5), stroke=al(WHITE, .5), sw=.9, cap=CAP_ROUND)
    # sol del centro
    s.circle(50, 47.4, 5.0, fill=rg("#FFFFFF", 48.6, 45.8, 7, hi=0, lo=.1), stroke=al(line, .6), sw=.8)
    s.circle(50, 47.4, 3.1, fill=rg(red, 49.2, 46.3, 4.5, hi=.4, lo=.22))
    s.circle(49.2, 46.4, .85, fill=al(WHITE, .7))
    return s.bake()


# ------------------------------------------------------------------ MOON

def moon():
    s = Scene(100, 100)
    gold = "#FFE08A"
    line = "#B98A28"
    # halo
    s.circle(26, 21, 17, fill=rad(26, 21, 17, [(0, "#FFF1B866"), (1, "#FFF1B800")]))
    s.circle(79, 17, 11, fill=rad(79, 17, 11, [(0, "#FFF1B866"), (1, "#FFF1B800")]))
    # luna creciente
    d = crescent_d((25.5, 21.5), 11.5, (31.5, 17.5), 9.6)
    s.path(d, fill=lin(15, 11, 33, 32, [(0, "#FFF3BE"), (.5, gold), (1, "#EFB73C")]), stroke=line, sw=1.2, join=JOIN_ROUND)
    s.path("M 16.6 17.5 Q 17.8 13.8 21.4 11.8", stroke=al(WHITE, .7), sw=1.5, cap=CAP_ROUND)
    # estrella
    s.poly(star_pts(79, 17.5, 6.6), fill=lin(73, 11, 85, 24, [(0, "#FFF3BE"), (1, "#F0B83A")]), stroke=line, sw=1.1, join=JOIN_ROUND)
    s.sparkle(79 - 1.6, 15.2, 1.6, fill=al(WHITE, .85))
    # destellos pequenos
    s.sparkle(64, 8.5, 3.0, fill="#FFF1B8")
    s.sparkle(90.5, 33, 2.4, fill="#FFF1B8")
    return s.bake()


# ------------------------------------------------------------------ STAR

def star():
    s = Scene(100, 100)
    gold = "#FFD34A"
    line = "#B87A10"
    cx, cy, r = 50, 16.5, 11.2
    s.circle(cx, cy, 17, fill=rad(cx, cy, 17, [(0, "#FFF1B855"), (1, "#FFF1B800")]))
    pts = star_pts(cx, cy, r)
    s.poly(pts, fill=lin(cx - r, cy - r, cx + r, cy + r, [(0, "#FFF0A0"), (.5, gold), (1, "#E39B1C")]), stroke=line, sw=1.6, join=JOIN_ROUND)
    # facetas: la mitad de cada punta un poco mas oscura (volumen)
    c = (cx, cy)
    for i in range(5):
        s.poly([c, pts[2 * i], pts[(2 * i + 1) % 10]], fill="#7A460014")
    for i in range(5):
        s.poly([c, pts[2 * i], pts[(2 * i - 1) % 10]], fill="#FFFFFF1C")
    s.poly(pts, stroke=line, sw=1.6, join=JOIN_ROUND)
    shine(s, cx - 3.6, cy - 4.6, 2.8, 1.3, a=.75, rot=-40)
    s.sparkle(32.5, 11.5, 3.1, fill="#FFF1B8")
    s.sparkle(68.5, 9.6, 2.4, fill="#FFF1B8")
    s.circle(72, 27, 1.1, fill=al("#FFF1B8", .9))
    return s.bake()


# ------------------------------------------------------------------ SCARF

def scarf():
    s = Scene(100, 100)
    red = "#E5576B"
    cream = "#FFF1DC"
    line = ed(red, .5)
    T = ((22, 75), (50, 82.5), (78, 75))
    B = ((22, 86), (50, 94.5), (78, 86))
    band = "M %.2f %.2f Q %.2f %.2f %.2f %.2f L %.2f %.2f Q %.2f %.2f %.2f %.2f Z" % (
        T[0][0], T[0][1], T[1][0], T[1][1], T[2][0], T[2][1], B[2][0], B[2][1], B[1][0], B[1][1], B[0][0], B[0][1])
    s.path(band, fill=lin(0, 75, 0, 94, [(0, lt(red, .3)), (.5, red), (1, dk(red, .2))]))
    # rayas diagonales
    for t0, t1 in ((.10, .19), (.30, .39), (.50, .59)):
        a, c, b = qsub(T[0], T[1], T[2], t0, t1)
        a2, c2, b2 = qsub(B[0], B[1], B[2], t0 + .035, t1 + .035)
        s.path("M %.2f %.2f Q %.2f %.2f %.2f %.2f L %.2f %.2f Q %.2f %.2f %.2f %.2f Z" % (
            a[0], a[1], c[0], c[1], b[0], b[1], b2[0], b2[1], c2[0], c2[1], a2[0], a2[1]), fill=al(cream, .92))
    # brillo del pliegue superior
    a, c, b = qsub((T[0][0], T[0][1] + 2.1), (T[1][0], T[1][1] + 2.1), (T[2][0], T[2][1] + 2.1), .08, .62)
    s.path("M %.2f %.2f Q %.2f %.2f %.2f %.2f" % (a[0], a[1], c[0], c[1], b[0], b[1]), stroke=al(WHITE, .35), sw=1.3, cap=CAP_ROUND)
    s.path(band, stroke=line, sw=1.4, join=JOIN_ROUND)
    # cola colgando por delante de la banda
    kx, ky = 66, 80
    with s.rotate(-8, kx, ky):
        s.ellipse(kx + 1.2, 93.6, 6.2, 1.4, fill="#00000014")
        tail = "M 61.2 80 L 70.8 80 L 71.2 92.6 Q 66 94.4 60.8 92.6 Z"
        s.path(tail, fill=lin(60, 80, 72, 93, [(0, lt(red, .22)), (1, dk(red, .22))]))
        s.path("M 60.95 87.6 Q 66 89.2 71.05 87.6 L 71.15 90 Q 66 91.6 60.85 90 Z", fill=al(cream, .92))
        s.path("M 62.6 82 L 62.9 91", stroke=al(WHITE, .3), sw=1.2, cap=CAP_ROUND)
        s.path(tail, stroke=line, sw=1.3, join=JOIN_ROUND)
        for x in (62.0, 64.3, 66.6, 68.9):
            s.line(x, 93.3, x - .3, 96.0, dk(red, .1), 1.5)
        # nudo
        s.rect(59.6, 77.6, 12.8, 5.6, r=2.6, fill=lin(0, 77, 0, 84, [(0, lt(red, .35)), (1, dk(red, .2))]), stroke=line, sw=1.3)
        s.path("M 62 79.3 Q 66 78.2 70.2 79.3", stroke=al(WHITE, .5), sw=1.0, cap=CAP_ROUND)
    return s.bake()


# ------------------------------------------------------------------ BOW

def bow():
    s = Scene(100, 100)
    pink = "#FF6B8A"
    line = ed(pink, .45)
    cx, cy = 72, 32
    wing = "M 71 32 C 66 24 60.5 20.5 57.2 23 C 54.2 26.5 54.2 37.5 57.2 41 C 60.5 43.5 66 40 71 32 Z"
    fold = "M 71 32 C 67 29.5 62 29.5 58.5 31 M 71 32 C 67 34.5 62 34.5 58.5 33"
    tail = "M 70 35 L 74 36 L 69.6 50.5 L 66.6 46.4 L 62.2 48.4 Z"
    with s.rotate(-6, cx, cy):
        s.path(tail, fill=lin(62, 36, 74, 50, [(0, lt(pink, .1)), (1, dk(pink, .22))]), stroke=line, sw=1.3, join=JOIN_ROUND)
    with s.flip_x(cx):
        with s.rotate(-6, cx, cy):
            s.path(tail, fill=lin(62, 36, 74, 50, [(0, lt(pink, .1)), (1, dk(pink, .22))]), stroke=line, sw=1.3, join=JOIN_ROUND)
    for flip in (False, True):
        def draw():
            s.path(wing, fill=lin(55, 22, 71, 42, [(0, lt(pink, .42)), (.6, pink), (1, dk(pink, .2))]))
            s.path(fold, stroke=al(dk(pink, .45), .35), sw=1.0, cap=CAP_ROUND)
            s.path("M 59.5 25.2 C 57.6 28.4 57.2 31.6 57.4 34", stroke=al(WHITE, .6), sw=1.5, cap=CAP_ROUND)
            s.path(wing, stroke=line, sw=1.4, join=JOIN_ROUND)
        if flip:
            with s.flip_x(cx):
                draw()
        else:
            draw()
    s.circle(cx, cy + .3, 4.7, fill=rg(pink, cx - 1.5, cy - 1.6, 7, hi=.45, lo=.22), stroke=line, sw=1.3)
    s.circle(cx - 1.4, cy - 1.4, 1.5, fill=al(WHITE, .7))
    return s.bake()


# ------------------------------------------------------------------ GLASSES

def glasses():
    s = Scene(100, 100)
    ink = "#2B2B3A"
    frame = lin(26, 46, 74, 68, [(0, "#5A5A78"), (1, "#1E1E2C")])
    # patillas
    s.line(27.3, 55.8, 19.6, 52.4, ink, 2.2)
    s.line(72.7, 55.8, 80.4, 52.4, ink, 2.2)
    for gx in (37, 63):
        s.circle(gx, 57, 9.8, fill=lin(gx - 9, 48, gx + 9, 66, [(0, "#FFFFFF3A"), (1, "#BFE3FF12")]))
        s.circle(gx, 57, 9.8, stroke=frame, sw=2.5)
        s.arc_stroke(gx, 57, 6.6, 202, 252, al(WHITE, .9), 1.5)
        s.circle(gx + 4.4, 62.6, .85, fill=al(WHITE, .6))
    s.path("M 46.3 55.6 Q 50 52 53.7 55.6", stroke=ink, sw=2.4, cap=CAP_ROUND)
    return s.bake()


# ------------------------------------------------------------------ WIZARD

def wizard():
    s = Scene(100, 100)
    purple = "#6C4FD6"
    deep = "#4F38A8"
    gold = "#FFD34A"
    line = ed(purple, .5)
    # ala
    s.ellipse(50, 40, 37, 7.2, fill=lin(13, 33, 87, 47, [(0, lt(deep, .3)), (1, dk(deep, .25))]), stroke=line, sw=1.4)
    s.ellipse(50, 41.2, 27, 3.4, fill="#00000024")
    # cono con la punta doblada
    cone = "M 26 40 C 36 37 49 24 56 8 C 57.6 4.2 61 2.4 65 3.4 C 63.6 6 63.4 9.4 65.2 14.4 C 68 22 72 31 75 40 Q 50 47.5 26 40 Z"
    s.path(cone, fill=lin(24, 10, 78, 42, [(0, lt(purple, .38)), (.55, purple), (1, dk(purple, .22))]))
    # banda dorada
    band = "M 30.4 30.4 Q 50 37.2 71.6 30.4 L 73.4 36.4 Q 50 43.8 28.6 36.4 Z"
    s.path(band, fill=lin(0, 30, 0, 42, [(0, "#FFE680"), (1, "#E0A52A")]), stroke=al("#9A5F18", .8), sw=.9, join=JOIN_ROUND)
    s.path("M 33.2 32.3 Q 50 37.8 68.6 32.3", stroke=al(WHITE, .55), sw=1.0, cap=CAP_ROUND)
    # hebilla
    s.rect(46, 33.4, 8, 6.4, r=1.4, fill=None, stroke="#9A5F18", sw=1.2)
    s.path(cone, stroke=line, sw=1.5, join=JOIN_ROUND)
    # brillo del cono
    s.path("M 36.5 31 C 43 27.5 50 20 54 11.5", stroke=al(WHITE, .38), sw=1.7, cap=CAP_ROUND)
    # estrella y puntitos
    s.poly(star_pts(54.5, 22, 5.0), fill=lin(50, 17, 59, 27, [(0, "#FFF0A0"), (1, "#F0B83A")]), stroke=al("#9A5F18", .8), sw=.7, join=JOIN_ROUND)
    s.sparkle(63.5, 28.5, 2.1, fill="#FFF1B8")
    s.circle(45, 28.2, 1.15, fill=al("#FFF1B8", .95))
    s.circle(62.6, 15.4, .9, fill=al("#FFF1B8", .9))
    return s.bake()


# ------------------------------------------------------------------ HEADPHONES

def headphones():
    s = Scene(100, 100)
    slate = "#3D405B"
    coral = "#E07A5F"
    line = ed(slate, .35)
    cx, cy, rx, ry = 50, 56, 35, 36
    # arco de la diadema (elipse): del extremo izquierdo al derecho por arriba
    def pt(deg):
        a = math.radians(deg)
        return cx + rx * math.cos(a), cy + ry * math.sin(a)
    p0, p1 = pt(184), pt(356)
    arc = "M %.2f %.2f A %d %d 0 0 1 %.2f %.2f" % (p0[0], p0[1], rx, ry, p1[0], p1[1])
    s.path(arc, stroke=line, sw=6.6, cap=CAP_ROUND)
    s.path(arc, stroke=lin(15, 20, 85, 56, [(0, "#6A6E92"), (1, "#34374F")]), sw=5.0, cap=CAP_ROUND)
    # almohadilla superior
    q0, q1 = pt(236), pt(304)
    s.path("M %.2f %.2f A %d %d 0 0 1 %.2f %.2f" % (q0[0], q0[1], rx, ry, q1[0], q1[1]), stroke=lin(30, 18, 70, 24, [(0, lt(coral, .25)), (1, dk(coral, .12))]), sw=5.0, cap=CAP_ROUND)
    s.path("M %.2f %.2f A %d %d 0 0 1 %.2f %.2f" % (pt(222)[0], pt(222)[1], rx, ry, pt(256)[0], pt(256)[1]), stroke=al(WHITE, .35), sw=1.3, cap=CAP_ROUND)
    for side in (0, 1):
        def cup():
            s.rect(8, 45, 13.5, 26.5, r=6.7, fill=lin(8, 45, 22, 72, [(0, "#6A6E92"), (1, "#2E3048")]), stroke=line, sw=1.2)
            s.rect(10.8, 49, 7.6, 18.5, r=3.8, fill=lin(11, 49, 18, 68, [(0, lt(coral, .3)), (1, dk(coral, .2))]), stroke=al(ed(coral, .5), .8), sw=.8)
            shine(s, 13.2, 49.2, 2.4, 1.1, a=.6, rot=-30)
            s.line(11.2, 53.5, 11.2, 63, al(WHITE, .35), 1.1)
        if side == 0:
            cup()
        else:
            with s.flip_x(50):
                cup()
    return s.bake()


# ------------------------------------------------------------------ FLOWERS (flor en la oreja)

def flower(s, cx, cy, R, col, rot=0):
    pr = R * .6
    d = R * .62
    line = al(ed(col, .4), .65)
    for k in range(5):
        a = math.radians(rot - 90 + 72 * k)
        px, py = cx + d * math.cos(a), cy + d * math.sin(a)
        s.circle(px, py, pr, fill=rad(px - pr * .3, py - pr * .35, pr * 1.7, [(0, lt(col, .5)), (1, dk(col, .1))]), stroke=line, sw=.6)
    s.circle(cx, cy, R * .36, fill=rad(cx - .5, cy - .5, R * .5, [(0, "#FFF0A0"), (1, "#F0B83A")]), stroke=al("#9A5F18", .6), sw=.5)
    s.circle(cx - R * .12, cy - R * .14, R * .1, fill=al(WHITE, .85))


def flowers():
    s = Scene(100, 100)
    leafc = "#6FBF73"
    # hojas detras
    for (x, y, ang) in ((63.5, 40.5, 30), (84.5, 41.5, -35), (66, 18.5, -20)):
        with s.rotate(ang, x, y):
            s.path(leaf_d(x, y, 10, 3.6), fill=g2(leafc, x - 5, y - 3, x + 5, y + 3, hi=.3, lo=.2), stroke=al(ed(leafc, .5), .8), sw=.8, join=JOIN_ROUND)
    # flor grande
    flower(s, 74, 30, 8.4, "#FF9EC4", rot=-8)
    # dos pequenas
    flower(s, 86, 38.5, 4.6, "#FFFFFF", rot=14)
    flower(s, 63.5, 22.5, 4.2, "#B9A6FF", rot=30)
    return s.bake()


# ------------------------------------------------------------------ FLOWER_CROWN

def flower_crown():
    s = Scene(100, 100)
    leafc = "#5DAF68"
    cols = ["#FF9EC4", "#FFFFFF", "#FFB347", "#B9A6FF"]
    pts = []
    for i in range(9):
        ang = math.radians(198 + i * 18)
        pts.append((50 + 34 * math.cos(ang), 62 + 33 * math.sin(ang)))
    # enredadera
    s.curve(pts, ed(leafc, .35), 3.4)
    s.curve(pts, lin(15, 30, 85, 55, [(0, "#7FCB84"), (1, "#4E9A5C")]), 2.4)
    # hojas entre flores
    for i in range(8):
        ang = 198 + i * 18 + 9
        a = math.radians(ang)
        x, y = 50 + 35.5 * math.cos(a), 62 + 34.5 * math.sin(a)
        rot = ang + 90 + (22 if i % 2 else -22)
        with s.rotate(rot, x, y):
            s.path(leaf_d(x, y, 9, 3.3), fill=g2(leafc, x - 4, y - 3, x + 4, y + 3, hi=.32, lo=.2), stroke=al(ed(leafc, .5), .8), sw=.8, join=JOIN_ROUND)
    # flores (las del centro mas grandes)
    radii = [4.6, 5.2, 5.7, 6.1, 6.6, 6.1, 5.7, 5.2, 4.6]
    for i, (x, y) in enumerate(pts):
        flower(s, x, y, radii[i], cols[i % 4], rot=i * 23)
    return s.bake()


# ------------------------------------------------------------------ paquete

def build():
    icons = {
        "acc.CROWN": crown(),
        "acc.HELMET": helmet(),
        "acc.HEADBAND": headband(),
        "acc.MOON": moon(),
        "acc.STAR": star(),
        "acc.SCARF": scarf(),
        "acc.FLOWERS": flowers(),
        "acc.BOW": bow(),
        "acc.GLASSES": glasses(),
        "acc.WIZARD": wizard(),
        "acc.HEADPHONES": headphones(),
        "acc.FLOWER_CROWN": flower_crown(),
    }
    return {"icons": icons}
