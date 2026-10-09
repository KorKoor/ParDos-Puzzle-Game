"""Avatares B (KOALA RACCOON CHICK UNICORN DRAGON AXOLOTL CAPYBARA TIGER): portado de drawAvatar de AvatarArt.kt.
Cada dibujo lleva hombros, partes de atras, cabeza y cara completa. Sin fondo ni accesorios. Lienzo 100x100."""
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, ".."))   # artlib
sys.path.insert(0, HERE)                        # _looks
from artlib import *  # noqa: F401,F403
import _looks

ANIMALS = ["KOALA", "RACCOON", "CHICK", "UNICORN", "DRAGON", "AXOLOTL", "CAPYBARA", "TIGER"]

INK = "#2B2B3A"
H, L, D, I, S = "$head", "$light", "$dark", "$inner", "$shirt"


# ------------------------------------------------------------------ colores derivados
def hi(c, t=.35):
    return mix(c, "#FFFFFF", t)


def sd(c, t=.2):
    return mix(c, D, t)


def edge(c, t=.5):
    return mix(c, D, t)


def fgrad(c, x1, y1, x2, y2, t=.28, b=.22):
    return lin(x1, y1, x2, y2, [(0, hi(c, t)), (.5, c), (1, sd(c, b))])


# ------------------------------------------------------------------ piezas comunes
def shoulders(s):
    """Hombros ($shirt), cuello del animal y cuello de la camiseta."""
    s.ellipse(50, 104, 40, 22, fill=lin(22, 82, 76, 126, [(0, hi(S, .30)), (1, sd(S, .30))]), stroke=edge(S, .5), sw=1.2)
    s.path(Path().M(37, 83).Q(50, 105, 63, 83).Z(), fill=lin(0, 84, 0, 98, [(0, sd(H, .5)), (1, sd(H, .12))]))
    s.path(Path().M(35.5, 84.5).Q(50, 107, 64.5, 84.5), stroke=hi(S, .5), sw=2.3, cap=CAP_ROUND)
    with s.rotate(-10, 28, 92):
        s.ellipse(28, 92.5, 11, 3.4, fill="#FFFFFF33")


def head(s):
    s.ellipse(50, 58, 33, 29, fill=fgrad(H, 34, 28, 68, 90), stroke=edge(H, .5), sw=1.3)
    with s.rotate(-14, 38, 38):
        s.ellipse(38, 38, 12.5, 4.8, fill="#FFFFFF4D")
    s.ellipse(27.5, 47, 1.7, 2.7, fill="#FFFFFF4D")


def blush(s):
    for cx in (27, 73):
        s.circle(cx, 68, 7.4, fill=rad(cx, 68, 7.4, [(0, "#FF8FA3B8"), (.6, "#FF8FA38A"), (1, "#FF8FA300")]))


def eye(s, cx, cy, r, white=False, lid=None, lash=None, lashes=0, side=1):
    """Ojo: abierto dentro de TAG_EYES_OPEN y cerrado (parpadeo) dentro de TAG_EYES_CLOSED."""
    if white:
        s.circle(cx, cy, r * 1.7, fill="#FFFFFF", stroke="#2B2B3A40", sw=1.2)
    with s.tag(TAG_EYES_OPEN):
        s.circle(cx, cy, r, fill=rad(cx - r * .3, cy - r * .4, r * 1.9, [(0, "#5C5C78"), (1, INK)]))
        s.circle(cx - r * .3, cy - r * .35, r * .38, fill="#FFFFFF")
        s.circle(cx + r * .4, cy + r * .42, r * .17, fill="#FFFFFFB3")
        for k in range(lashes):
            a = math.radians(-150 + k * 28) if side < 0 else math.radians(-30 - k * 28)
            x0, y0 = cx + (r * .88) * math.cos(a), cy + (r * .88) * math.sin(a)
            x1, y1 = cx + (r + 2.6) * math.cos(a), cy + (r + 2.6) * math.sin(a)
            s.line(x0, y0, x1, y1, INK, 1.3)
    with s.tag(TAG_EYES_CLOSED):
        if white and lid is not None:
            s.circle(cx, cy, r * 1.7 + .4, fill=lid)
        s.path(Path().M(cx - r, cy - r * .15).Q(cx, cy + r * .85, cx + r, cy - r * .15),
               stroke=lash or INK, sw=max(1.4, r * .42), cap=CAP_ROUND)


def smile(s, cx, cy, w, color=D, sw=1.9):
    """Sonrisa de dos arcos, igual que smile() de Kotlin."""
    a0, a1 = math.radians(15), math.radians(165)
    for x0 in (cx - w, cx):
        ex, ey, rx, ry = x0 + w / 2.0, cy + .5, w / 2.0, 3.5
        p = Path().M(ex + rx * math.cos(a0), ey + ry * math.sin(a0)).A(rx, ry, 0, 0, 1, ex + rx * math.cos(a1), ey + ry * math.sin(a1))
        s.path(p, stroke=color, sw=sw, cap=CAP_ROUND, join=JOIN_ROUND)


def wedge(s, x0, y0, x1, y1, w, fill, w1=0.0):
    """Mancha afilada (rayas) de la base (x0,y0) a la punta (x1,y1)."""
    dx, dy = x1 - x0, y1 - y0
    ln = math.hypot(dx, dy)
    nx, ny = -dy / ln, dx / ln
    xm, ym = x0 + dx * .4, y0 + dy * .4
    p = Path().M(x0 + nx * w, y0 + ny * w)
    p.Q(xm + nx * w * .62, ym + ny * w * .62, x1 + nx * w1, y1 + ny * w1)
    p.Q(xm - nx * w * .62, ym - ny * w * .62, x0 - nx * w, y0 - ny * w).Z()
    s.path(p, fill=fill, join=JOIN_ROUND)


def nose_shine(s, x, y, rx=2.2, ry=1.2, a="73"):
    with s.rotate(-18, x, y):
        s.ellipse(x, y, rx, ry, fill="#FFFFFF" + a)


# ------------------------------------------------------------------ KOALA
def koala():
    s = Scene(100, 100, pal=_looks.palette("KOALA"))
    shoulders(s)
    for cx in (19, 81):
        s.circle(cx, 38, 16, fill=rad(cx - 5, 31, 27, [(0, hi(H, .34)), (.6, H), (1, sd(H, .26))]), stroke=edge(H, .5), sw=1.3)
        f = rad(cx - 2, 36, 12.5, [(0, hi(I, .6)), (1, I)])
        for k in range(8):
            a = math.radians(k * 45 + 22)
            s.circle(cx + 7.0 * math.cos(a), 39 + 7.0 * math.sin(a), 3.3, fill=f)
        s.circle(cx, 39, 8.6, fill=f)
    head(s)
    # pelusa clara bajo la barbilla
    s.ellipse(50, 79, 17, 5.5, fill=alpha(L, .55))
    blush(s)
    eye(s, 37, 57, 4.4)
    eye(s, 63, 57, 4.4, side=-1)
    # nariz grande
    nose = Path().M(41.3, 63.5).C(41.3, 56.5, 58.7, 56.5, 58.7, 63.5).L(58, 72).C(58, 78.5, 54, 81.5, 50, 81.5).C(46, 81.5, 42, 78.5, 42, 72).Z()
    s.path(nose, fill=lin(44, 57, 56, 82, [(0, "#5A5A70"), (.5, "#3B3B4A"), (1, "#2A2A38")]), stroke="#2A2A3880", sw=.8, join=JOIN_ROUND)
    nose_shine(s, 46.2, 62.2, 3.4, 1.7, "8C")
    s.ellipse(54.5, 77, 2.2, 1.0, fill="#FFFFFF33")
    return s.bake()


# ------------------------------------------------------------------ RACCOON
def raccoon():
    s = Scene(100, 100, pal=_looks.palette("RACCOON"))
    shoulders(s)
    for cx in (24, 76):
        s.circle(cx, 33, 10, fill=rad(cx - 3, 28, 17, [(0, hi(H, .34)), (.6, H), (1, sd(H, .26))]), stroke=edge(H, .5), sw=1.3)
        s.circle(cx, 34, 5.5, fill=lin(cx, 29, cx, 40, [(0, hi(D, .15)), (1, sd(D, .1))]))
    head(s)
    # franja clara de la frente
    s.path("M 50 30.5 C 56 33 57.8 41 54.2 47.5 C 52.6 51 51 54 50 57 C 49 54 47.4 51 45.8 47.5 C 42.2 41 44 33 50 30.5 Z",
           fill=lin(0, 30, 0, 56, [(0, hi(L, .2)), (1, L)]))
    # antifaz
    mask = lin(0, 49, 0, 68, [(0, hi(D, .14)), (1, sd(D, .0))])
    s.path("M 19 60 C 19 53 25 49.2 34 49.6 C 41 50 45 54.5 50 54.5 C 55 54.5 59 50 66 49.6 C 75 49.2 81 53 81 60 "
           "C 81 66.5 74.5 69 67 67.6 C 61 66.4 56 63.5 50 63.5 C 44 63.5 39 66.4 33 67.6 C 25.5 69 19 66.5 19 60 Z", fill=mask)
    # hocico claro
    s.path("M 50 60.2 C 57 60.2 66 63.5 65.2 72.5 C 64.4 80.5 58.5 85 50 85 C 41.5 85 35.6 80.5 34.8 72.5 C 34 63.5 43 60.2 50 60.2 Z",
           fill=lin(0, 60, 0, 85, [(0, hi(L, .0)), (1, mix(L, H, .35))]))
    blush(s)
    eye(s, 35, 58, 3.8, white=True, lid=mask, lash=hi(L, .0))
    eye(s, 65, 58, 3.8, white=True, lid=mask, lash=hi(L, .0), side=-1)
    # nariz y boca
    s.ellipse(50, 66.2, 5.2, 3.9, fill=lin(0, 62, 0, 70, [(0, "#4A4A60"), (1, INK)]))
    nose_shine(s, 48.2, 64.8, 1.9, .9, "8C")
    smile(s, 50, 71.5, 5.5, INK)
    for sx in (-1, 1):
        for (dx, dy) in ((9.5, 2.0), (11.5, 5.0), (8.0, 6.2)):
            s.circle(50 + sx * dx, 71 + dy, .8, fill=alpha(INK, .28))
    return s.bake()


# ------------------------------------------------------------------ CHICK
def chick():
    s = Scene(100, 100, pal=_looks.palette("CHICK"))
    shoulders(s)
    # penacho (detras de la cabeza)
    for k, ang, ry, cy in ((-1, -17, 7, 27.5), (1, 17, 7, 27.5), (0, 0, 8.2, 24.2)):
        px = 50 + 6 * k
        with s.rotate(ang, px, 35):
            s.ellipse(px, cy, 4.1, ry, fill=lin(px - 4, cy - ry, px + 4, cy + ry, [(0, hi(H, .38)), (1, sd(H, .12))]), stroke=edge(H, .45), sw=1.1)
    head(s)
    # cara clara
    s.ellipse(50, 72, 19, 12, fill=alpha(L, .6))
    blush(s)
    eye(s, 37, 57, 4.4)
    eye(s, 63, 57, 4.4, side=-1)
    # pico
    beak = "#F29A2E"
    s.path("M 44.2 62.6 C 45 60.8 55 60.8 55.8 62.6 C 56.2 66.2 52.8 70.6 50 72.4 C 47.2 70.6 43.8 66.2 44.2 62.6 Z",
           fill=lin(46, 61, 54, 73, [(0, "#FFC060"), (.5, beak), (1, "#D77C16")]), stroke="#B8751066", sw=.9, join=JOIN_ROUND)
    s.path(Path().M(45.2, 66.2).Q(50, 68.2, 54.8, 66.2), stroke="#B8751099", sw=1.1, cap=CAP_ROUND)
    s.ellipse(47.4, 63.4, 2.4, .9, fill="#FFFFFF8C")
    return s.bake()


# ------------------------------------------------------------------ UNICORN
def unicorn():
    s = Scene(100, 100, pal=_looks.palette("UNICORN"))
    shoulders(s)
    # melena a un lado
    mane = ["#FF9EC4", "#C59BFF", "#8EC9FF", "#FFD36E"]
    for i, c in enumerate(mane):
        cx, cy, rx = 19 + i, 43 + 9 * i, 13 - i
        s.ellipse(cx, cy, rx, 13, fill=lin(cx - rx, cy - 13, cx + rx, cy + 13, [(0, mix(c, "#FFFFFF", .4)), (.55, c), (1, mix(c, "#6B3A7A", .22))]),
                  stroke=mix(c, "#6B3A7A", .4), sw=1.0)
        s.ellipse(cx - 3, cy - 6, 4.4, 1.6, fill="#FFFFFF59")
    s.sparkle(13.5, 41, 3.4, fill="#FFFFFFE6")
    s.sparkle(16.5, 66, 2.3, fill="#FFFFFFD9")
    # orejas
    s.poly([(24, 42), (26, 13.5), (44, 33)], fill=lin(24, 14, 40, 40, [(0, hi(H, .3)), (1, sd(H, .16))]), stroke=edge(H, .5), sw=1.2, join=JOIN_ROUND)
    s.poly([(76, 42), (74, 13.5), (56, 33)], fill=lin(60, 14, 76, 40, [(0, hi(H, .3)), (1, sd(H, .16))]), stroke=edge(H, .5), sw=1.2, join=JOIN_ROUND)
    s.poly([(28, 35), (29, 19.5), (38.5, 32)], fill=lin(28, 20, 38, 35, [(0, hi(I, .3)), (1, I)]), join=JOIN_ROUND)
    s.poly([(72, 35), (71, 19.5), (61.5, 32)], fill=lin(72, 20, 62, 35, [(0, hi(I, .3)), (1, I)]), join=JOIN_ROUND)
    head(s)
    # hocico
    s.ellipse(50, 74, 14.5, 10.3, fill=lin(0, 64, 0, 84, [(0, L), (1, mix(H, I, .38))]), stroke=alpha(edge(H, .5), .35), sw=.9)
    blush(s)
    eye(s, 37, 57, 4.4, lashes=2, side=-1)
    eye(s, 63, 57, 4.4, lashes=2, side=1)
    for x in (46, 54):
        s.circle(x, 71.2, 1.6, fill=mix(I, D, .38))
    smile(s, 50, 74, 4, D, 1.8)
    # mechon de la frente
    s.path("M 44.5 33.5 C 38 31.5 30.5 36 31.5 43.5 C 32 47.5 36.2 48.6 38.2 45.4 C 36.6 41.6 40.4 37.6 46 36.2 Z",
           fill=lin(31, 33, 44, 48, [(0, "#FFB8D4"), (1, "#E58CB8")]), stroke=mix("#FF9EC4", "#6B3A7A", .4), sw=1.0, join=JOIN_ROUND)
    s.path("M 41.5 31.8 C 36 32 32.5 35.8 33.4 40.2 C 35.4 36.8 38.6 35.4 43 35 Z",
           fill=lin(33, 31, 42, 40, [(0, "#E2C8FF"), (1, "#B58BF2")]), join=JOIN_ROUND)
    # cuerno
    horn = Path().M(43, 34.5).Q(47.2, 19, 50, 3.6).Q(52.8, 19, 57, 34.5).Z()
    s.path(horn, fill=lin(43, 6, 57, 34, [(0, "#FFF0B0"), (.45, "#FFD36E"), (1, "#E3A93C")]), stroke="#B9801E", sw=1.2, join=JOIN_ROUND)
    for y in (12.5, 20.5, 28):
        hw = (y - 3.6) / 30.9 * 6.6 - .5
        s.line(50 - hw, y + 1.8, 50 + hw, y - 1.8, "#D99A2A", 1.7, cap=CAP_BUTT)
    s.line(48.4, 27, 49.4, 9, "#FFFFFF99", 1.2)
    return s.bake()


# ------------------------------------------------------------------ DRAGON
def dragon():
    s = Scene(100, 100, pal=_looks.palette("DRAGON"))
    shoulders(s)
    # cuernos curvos
    left = "M 26.5 39 C 21 30 19 18 20.5 6 C 31 10.5 40 20 44.5 31.5 Z"
    s.path(left, fill=lin(20, 6, 38, 36, [(0, hi(I, .55)), (.5, I), (1, sd(I, .22))]), stroke=edge(I, .5), sw=1.2, join=JOIN_ROUND)
    with s.flip_x(50):
        s.path(left, fill=lin(20, 6, 38, 36, [(0, hi(I, .55)), (.5, I), (1, sd(I, .22))]), stroke=edge(I, .5), sw=1.2, join=JOIN_ROUND)
    for sg in (-1, 1):
        with (s.flip_x(50) if sg > 0 else s.translate(0, 0)):
            s.line(23.4, 24, 29.5, 24.8, sd(I, .42), 1.3)
            s.line(27.2, 16.5, 33, 18.4, sd(I, .42), 1.3)
    # puas de la cabeza
    sp = mix(H, D, .38)
    for k in (-1, 0, 1):
        x = 50 + k * 11
        tip = 12 if k == 0 else 16
        s.path(Path().M(x - 5.8, 33).Q(x - 3.6, 22, x, tip).Q(x + 3.6, 22, x + 5.8, 33).Z(),
               fill=lin(x - 5, tip, x + 5, 33, [(0, hi(sp, .3)), (1, sd(sp, .2))]), stroke=edge(sp, .5), sw=1.1, join=JOIN_ROUND)
    head(s)
    # escamas de la frente
    for (x, y) in ((42, 40), (50, 37.5), (58, 40), (46, 44.2), (54, 44.2)):
        s.path(Path().M(x - 2.7, y - .6).Q(x, y + 3.4, x + 2.7, y - .6), stroke=alpha(edge(H, .6), .38), sw=1.1, cap=CAP_ROUND)
    # hocico
    s.ellipse(50, 74, 17, 12, fill=lin(0, 62, 0, 86, [(0, L), (1, mix(L, H, .3))]), stroke=alpha(edge(H, .5), .3), sw=.9)
    blush(s)
    eye(s, 37, 57, 4.4)
    eye(s, 63, 57, 4.4, side=-1)
    for x in (45, 55):
        s.circle(x, 70, 1.7, fill=alpha(D, .7))
    smile(s, 50, 78, 6, D, 1.9)
    return s.bake()


# ------------------------------------------------------------------ AXOLOTL
def axolotl():
    s = Scene(100, 100, pal=_looks.palette("AXOLOTL"))
    shoulders(s)
    # branquias plumosas: tres a cada lado
    for k, (ang, ln) in enumerate(((-68, 15.5), (-42, 18), (-16, 19))):
        for side in (0, 1):
            ctx = s.flip_x(50) if side else s.translate(0, 0)
            with ctx:
                px, py = 20, 56
                with s.rotate(ang, px, py):
                    s.ellipse(px, py - ln / 2, 3.9, ln / 2, fill=lin(px - 4, py - ln, px + 4, py, [(0, hi(I, .5)), (.55, I), (1, sd(I, .18))]),
                              stroke=edge(I, .5), sw=1.0)
                    s.line(px, py - 2, px, py - ln + 4, alpha(sd(I, .5), .4), 1.0)
                    s.circle(px, py - ln + 2.6, 2.0, fill="#FFFFFF99")
    head(s)
    for (x, y, r) in ((43, 38.5, 1.1), (50, 35.5, 1.3), (57, 38.5, 1.1), (47, 42.2, .9), (53.5, 42.6, .9), (37.5, 44, .8), (62.5, 44, .8)):
        s.circle(x, y, r, fill=alpha(I, .5))
    s.ellipse(50, 74.5, 15, 8.8, fill=lin(0, 66, 0, 83, [(0, L), (1, mix(L, H, .3))]), stroke=alpha(edge(H, .5), .28), sw=.9)
    blush(s)
    eye(s, 37, 57, 4.4)
    eye(s, 63, 57, 4.4, side=-1)
    for x in (46.5, 53.5):
        s.circle(x, 66.5, .8, fill=alpha(D, .45))
    smile(s, 50, 71.5, 7, D, 1.9)
    return s.bake()


# ------------------------------------------------------------------ CAPYBARA
def capybara():
    s = Scene(100, 100, pal=_looks.palette("CAPYBARA"))
    shoulders(s)
    ear = mix(H, D, .25)
    for cx in (30, 70):
        s.circle(cx, 32, 7, fill=rad(cx - 2, 29, 11, [(0, hi(ear, .34)), (.6, ear), (1, sd(ear, .25))]), stroke=edge(ear, .5), sw=1.1)
        s.circle(cx, 33, 3.4, fill=lin(cx, 29, cx, 37, [(0, sd(I, .1)), (1, hi(I, .1))]))
    head(s)
    # pelillos de la frente
    for (x, y, d) in ((44, 33.6, -1), (50, 32.6, 0), (56, 33.6, 1)):
        s.path(Path().M(x, y).Q(x + d * 1.2, y + 2.6, x + d * 2.4, y + 4.4), stroke=alpha(edge(H, .65), .36), sw=1.1, cap=CAP_ROUND)
    # morro grande
    mz = mix(H, L, .4)
    s.ellipse(50, 73, 20, 15, fill=lin(30, 58, 70, 88, [(0, hi(mz, .3)), (.5, mz), (1, sd(mz, .15))]), stroke=alpha(edge(H, .5), .3), sw=.9)
    blush(s)
    eye(s, 37, 57, 4.4)
    eye(s, 63, 57, 4.4, side=-1)
    for sx in (-1, 1):
        for (dx, dy) in ((11, -3.5), (13.5, -.5), (10, 1.5)):
            s.circle(50 + sx * dx, 73 + dy, .85, fill=alpha(edge(H, .7), .5))
    s.ellipse(50, 66.6, 7.4, 4.8, fill=lin(0, 62, 0, 72, [(0, "#5A4232"), (1, "#3B2A1E")]))
    nose_shine(s, 47.2, 64.8, 2.6, 1.2, "8C")
    s.line(50, 70.8, 50, 74.2, alpha("#3B2A1E", .6), 1.2)
    smile(s, 50, 76, 5, D, 1.8)
    return s.bake()


# ------------------------------------------------------------------ TIGER
def tiger():
    s = Scene(100, 100, pal=_looks.palette("TIGER"))
    shoulders(s)
    for cx in (25, 75):
        s.circle(cx, 35, 11, fill=rad(cx - 3, 30, 19, [(0, hi(H, .34)), (.6, H), (1, sd(H, .26))]), stroke=edge(H, .5), sw=1.3)
        s.circle(cx, 36, 6, fill=lin(cx, 31, cx, 42, [(0, sd(L, .12)), (1, L)]))
    head(s)
    # parches claros sobre los ojos
    with s.rotate(-14, 36, 49.5):
        s.ellipse(36, 49.5, 5.4, 2.9, fill=alpha(L, .85))
    with s.rotate(14, 64, 49.5):
        s.ellipse(64, 49.5, 5.4, 2.9, fill=alpha(L, .85))
    # rayas
    st = lin(0, 30, 0, 66, [(0, mix(D, "#000000", .0)), (1, mix(D, H, .1))])
    wedge(s, 50, 30, 50, 45.5, 2.5, D, .0)
    wedge(s, 42.2, 30.6, 44.8, 41.5, 1.9, D, .0)
    wedge(s, 57.8, 30.6, 55.2, 41.5, 1.9, D, .0)
    for sg in (-1, 1):
        for k in range(2):
            y = 54 + k * 9
            wedge(s, 50 + sg * 32.2, y, 50 + sg * 20.2, y + 3.2, 2.1, D, .0)
    # hocico claro
    s.ellipse(50, 73.6, 16, 11.4, fill=lin(0, 62, 0, 85, [(0, hi(L, .2)), (1, mix(L, H, .25))]), stroke=alpha(edge(H, .5), .3), sw=.9)
    blush(s)
    eye(s, 37, 57, 4.4)
    eye(s, 63, 57, 4.4, side=-1)
    # nariz y boca
    s.path(Path().M(44.4, 62.6).Q(50, 60.4, 55.6, 62.6).Q(55, 66.2, 50, 69.4).Q(45, 66.2, 44.4, 62.6).Z(),
           fill=lin(46, 61, 54, 70, [(0, "#FF8AA0"), (1, "#D63F58")]), stroke="#B8324866", sw=.8, join=JOIN_ROUND)
    nose_shine(s, 47.6, 63.2, 1.9, .8, "99")
    s.line(50, 69.4, 50, 73.4, alpha(D, .55), 1.2)
    smile(s, 50, 72.4, 6, D, 1.8)
    for sg in (-1, 1):
        for k in range(2):
            s.line(50 + sg * 14, 68 + k * 3, 50 + sg * 28, 65 + k * 6, alpha(D, .42), 1.1)
    return s.bake()


def build():
    icons = {
        "animal.KOALA": koala(),
        "animal.RACCOON": raccoon(),
        "animal.CHICK": chick(),
        "animal.UNICORN": unicorn(),
        "animal.DRAGON": dragon(),
        "animal.AXOLOTL": axolotl(),
        "animal.CAPYBARA": capybara(),
        "animal.TIGER": tiger(),
    }
    return {"icons": icons, "palettes": _looks.all_palettes(ANIMALS)}
