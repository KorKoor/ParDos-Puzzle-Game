"""Accesorios de avatar (segunda tanda): WITCH PIRATE CHEF SANTA DEVIL HALO NINJA TOPHAT CAP SUNGLASSES CAPE PARTY.

Portados de AvatarArt.kt (drawAvatar, bloque de accesorios). Mismo lienzo 100x100 y mismas coordenadas que los
animales: el accesorio se pinta ENCIMA del animal. Colores fijos. CAPE tiene dos capas:
  acc.CAPE.back  -> la capa que va detras del cuerpo (se pinta antes que el animal)
  acc.CAPE       -> el cuello y el broche, delante.
"""
import os, sys, math
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, ".."))   # artlib
sys.path.insert(0, HERE)                        # _looks, _kit
from artlib import *

W = "#FFFFFF"
INK = "#2B1B3A"
GOLD = "#E0A93B"


# ------------------------------------------------------------------ ayudas

def hi(c, t=.35):
    return mix(c, W, t)


def lo(c, t=.22):
    return mix(c, INK, t)


def edge(c, t=.5):
    return mix(c, INK, t)


def vfill(c, y1, y2, a=.32, b=.24):
    """Degradado vertical: luz arriba, sombra abajo."""
    return lin(0, y1, 0, y2, [(0, hi(c, a)), (1, lo(c, b))])


def dfill(c, x1, y1, x2, y2, a=.35, b=.24):
    """Degradado diagonal: luz en (x1,y1), sombra en (x2,y2)."""
    return lin(x1, y1, x2, y2, [(0, hi(c, a)), (1, lo(c, b))])


def soft_shadow(s, cx, cy, rx, ry, a=0x24):
    """Sombra suave que el accesorio echa sobre la cabeza del animal."""
    s.ellipse(cx, cy, rx, ry, fill="#2B1B3A%02X" % a)


def bez(p0, c1, c2, p1, n=60):
    pts = []
    for i in range(n + 1):
        t = i / float(n)
        u = 1 - t
        pts.append((u * u * u * p0[0] + 3 * u * u * t * c1[0] + 3 * u * t * t * c2[0] + t * t * t * p1[0],
                    u * u * u * p0[1] + 3 * u * u * t * c1[1] + 3 * u * t * t * c2[1] + t * t * t * p1[1]))
    return pts


def x_at(pts, y):
    """x del camino (lista de puntos) a la altura y."""
    for (xa, ya), (xb, yb) in zip(pts, pts[1:]):
        if ya != yb and (ya - y) * (yb - y) <= 0:
            return xa + (xb - xa) * (y - ya) / (yb - ya)
    raise ValueError("el camino no pasa por y=%s" % y)


def gem(s, cx, cy, r, color):
    """Gema redonda con brillo."""
    s.circle(cx, cy, r, fill=dfill(color, cx - r, cy - r, cx + r, cy + r, .45, .25), stroke=edge(color, .5), sw=.9)
    s.ellipse(cx - r * .3, cy - r * .35, r * .35, r * .22, fill=alpha(W, .8))


# ------------------------------------------------------------------ WITCH

def witch():
    s = Scene()
    brim = "#2D1B4E"
    cone = "#4A2D82"
    soft_shadow(s, 50, 46.5, 35, 4.2)
    # ala
    s.ellipse(50, 38, 39.5, 7.6, fill=lin(0, 30, 0, 46, [(0, hi(brim, .30)), (1, lo(brim, .35))]),
              stroke=edge(brim, .6), sw=1.2)
    s.curve([(13.5, 36.2), (20, 33), (30, 31.2)], stroke=alpha(W, .30), sw=1.4)
    # cono con la punta doblada
    left = bez((27, 37), (30, 30), (43, 22), (49, 13))
    right = bez((61, 16), (60, 24), (68, 30), (73, 37))
    d = ("M 27 37 C 30 30 43 22 49 13 C 51 8 57 4.6 64 5.2 C 69 5.8 72.5 9 74 13.5 "
         "C 69 11.6 64 12.6 61 16 C 60 24 68 30 73 37 C 62 41.5 38 41.5 27 37 Z")
    s.path(d, fill=lin(30, 8, 74, 40, [(0, hi(cone, .32)), (.55, cone), (1, lo(cone, .38))]),
           stroke=edge(cone, .55), sw=1.2, join=JOIN_ROUND)
    # sombra del doblez
    s.path("M 49 13 C 53 16 58 15.5 61 16 C 58 12.5 52 11 49 13 Z", fill=alpha(lo(cone, .6), .45))
    # banda naranja que sigue la curva del cono
    yt, yb = 29.2, 36.0
    xl_t, xr_t = x_at(left, yt), x_at(right, yt)
    xl_b, xr_b = x_at(left, yb), x_at(right, yb)
    band = ("M %.2f %.2f Q 50 %.2f %.2f %.2f L %.2f %.2f Q 50 %.2f %.2f %.2f Z"
            % (xl_t, yt, yt + 7, xr_t, yt, xr_b, yb, yb + 7, xl_b, yb))
    s.path(band, fill=lin(0, 28, 0, 42, [(0, "#FFA94A"), (1, "#E26E12")]))
    s.path("M %.2f %.2f Q 50 %.2f %.2f %.2f" % (xl_t, yt, yt + 7, xr_t, yt), stroke=alpha(W, .35), sw=.9)
    # contorno del cono por encima de la banda
    s.path(d, stroke=edge(cone, .55), sw=1.2, join=JOIN_ROUND)
    # brillo del lado izquierdo
    s.curve([(x + 2.6, y) for (x, y) in left[10:34:3]], stroke=alpha(W, .30), sw=1.6)
    # hebilla dorada
    s.rect(44, 31.6, 12, 9, r=2.2, fill=dfill(GOLD, 44, 31, 56, 41, .55, .2), stroke=edge("#B87510", .3), sw=1.1)
    s.rect(47.2, 34.4, 5.6, 3.6, r=1, fill=lin(0, 34, 0, 38, [(0, "#C9560F"), (1, "#E26E12")]))
    s.ellipse(47.5, 33, 2.6, .9, fill=alpha(W, .7))
    # estrellita y puntos dorados
    s.sparkle(50.5, 23.5, 3.3, fill="#FFD34A")
    s.circle(56.2, 17.2, 1.1, fill="#FFD34A")
    s.circle(45.5, 29.6, .9, fill="#FFD34A")
    return s.bake()


# ------------------------------------------------------------------ PIRATE

def pirate():
    s = Scene()
    hat = "#33304F"
    soft_shadow(s, 50, 39, 33, 3.4)
    d = "M 13 38 L 30 13 Q 50 3 70 13 L 87 38 Q 50 27 13 38 Z"
    s.path(d, fill=lin(14, 8, 86, 38, [(0, hi(hat, .30)), (.5, hat), (1, lo(hat, .45))]), stroke=GOLD, sw=1.7, join=JOIN_ROUND)
    # alas laterales un poco mas oscuras
    s.path("M 13 38 L 30 13 L 36.5 31.5 Q 24 34.5 13 38 Z", fill=alpha(INK, .22))
    s.path("M 87 38 L 70 13 L 63.5 31.5 Q 76 34.5 87 38 Z", fill=alpha(INK, .22))
    # pliegue de la copa y brillo
    s.curve([(36, 12.5), (44, 9.6), (52, 9)], stroke=alpha(W, .28), sw=1.6)
    # ribete dorado del borde inferior
    s.path("M 14 37.2 Q 50 26.6 86 37.2", stroke=lin(14, 0, 86, 0, [(0, "#C98A1B"), (.5, "#FFE08A"), (1, "#C98A1B")]), sw=2.6)
    s.path(d, stroke=GOLD, sw=1.1, join=JOIN_ROUND)
    # calavera
    sk = "#F6F2EA"
    s.circle(50, 17.8, 5.3, fill=rad(48.4, 15.8, 8, [(0, W), (1, "#D9D4E6")]), stroke="#9C94B5", sw=.8)
    s.rect(46.6, 21.2, 6.8, 3.6, r=1.3, fill=rad(48.4, 20.6, 8, [(0, W), (1, "#D9D4E6")]), stroke="#9C94B5", sw=.8)
    s.ellipse(48, 17.6, 1.35, 1.6, fill="#2B1B3A")
    s.ellipse(52, 17.6, 1.35, 1.6, fill="#2B1B3A")
    s.tri((49.2, 20.2), (50.8, 20.2), (50, 19.1), fill="#2B1B3A")
    for x in (48.4, 50, 51.6):
        s.line(x, 22.3, x, 24.4, "#9C94B5", .6, cap=CAP_BUTT)
    # huesos cruzados
    for (ax, ay, bx, by) in ((39.5, 27.2, 60.5, 31.6), (60.5, 27.2, 39.5, 31.6)):
        s.line(ax, ay, bx, by, "#9C94B5", 3.2)
        s.line(ax, ay, bx, by, W, 2.0)
        for (px, py) in ((ax, ay), (bx, by)):
            s.circle(px - .6, py - 1.0, 1.5, fill=W, stroke="#9C94B5", sw=.6)
            s.circle(px - .6, py + 1.3, 1.5, fill=W, stroke="#9C94B5", sw=.6)
    return s.bake()


# ------------------------------------------------------------------ CHEF

def chef():
    s = Scene()
    ln = "#B9BDD6"
    soft_shadow(s, 50, 41.6, 29, 3.0)
    for (cx, cy, r) in ((34.5, 23, 11), (65.5, 23, 11), (50, 16.2, 12.6)):
        s.circle(cx, cy, r, fill=rad(cx - r * .35, cy - r * .45, r * 1.7, [(0, W), (1, "#D9DCEC")]), stroke=ln, sw=1.1)
    # copa
    s.rect(31, 25.2, 38, 13.8, r=2.6, fill=lin(31, 0, 69, 0, [(0, "#D2D6E8"), (.32, W), (.7, "#F2F3FA"), (1, "#CDD1E4")]),
           stroke=ln, sw=1.1)
    for x in (40.5, 50, 59.5):
        s.line(x, 27.5, x, 32, alpha("#9DA3C4", .55), 1.0, cap=CAP_BUTT)
    # cinta roja
    s.rect(31.4, 33.2, 37.2, 3.5, r=.8, fill=lin(0, 33, 0, 37, [(0, "#F0707F"), (1, "#C73B52")]))
    s.line(34, 34.1, 46, 34.1, alpha(W, .5), .8)
    # brillos
    s.ellipse(44, 11.6, 4.8, 2.4, fill=alpha(W, .8))
    s.ellipse(28.6, 19.6, 2.6, 1.5, fill=alpha(W, .75))
    return s.bake()


# ------------------------------------------------------------------ SANTA

def santa():
    s = Scene()
    red = "#D6322B"
    soft_shadow(s, 50, 44, 34, 3.2)
    d = "M 19 38 C 19 18 33 8 54 8 C 69 8 80 17 82 38 Z"
    s.path(d, fill=lin(24, 8, 82, 40, [(0, hi(red, .34)), (.5, red), (1, lo(red, .38))]), stroke=edge(red, .55), sw=1.3, join=JOIN_ROUND)
    # pliegue y brillo
    s.curve([(58, 12), (68, 17), (74, 28)], stroke=alpha(lo(red, .55), .55), sw=1.8)
    s.curve([(26, 24), (32, 15), (43, 10.5)], stroke=alpha(W, .42), sw=2.0)
    # pompon
    for (bx, by, br) in ((70.2, 11.2, 3.4), (77.8, 10.8, 3.4), (79.4, 17.4, 3.2), (73.8, 20, 3.0), (68.6, 17.2, 3.0)):
        s.circle(bx, by, br, fill="#EDEAF5")
    s.circle(74, 14.6, 6.4, fill=rad(71.6, 12, 9, [(0, W), (1, "#DDD8EB")]), stroke="#C9C3D8", sw=.9)
    s.ellipse(71.8, 12.3, 2.4, 1.5, fill=alpha(W, .9))
    # ribete de piel
    s.rect(14, 31, 72, 11.4, r=5.7, fill=lin(0, 31, 0, 42.4, [(0, W), (1, "#E1DCEC")]), stroke="#C9C3D8", sw=1.1)
    for (fx, fy, fr) in ((24, 38.6, 1.8), (36, 39.4, 1.5), (49, 39.8, 1.7), (62, 39.4, 1.5), (75, 38.6, 1.8)):
        s.ellipse(fx, fy, fr * 1.6, fr * .8, fill="#E6E1F0", op=.9)
    s.ellipse(33, 34.4, 8, 1.4, fill=alpha(W, .9))
    return s.bake()


# ------------------------------------------------------------------ DEVIL

def devil():
    s = Scene()
    red = "#D9322B"

    def horn():
        d = "M 22 45 C 16 35 14 22 17 9 C 27 15 38 23 42 34 Z"
        s.path(d, fill=lin(31, 42, 17, 9, [(0, lo(red, .38)), (.55, red), (1, "#FF7E62")]), stroke=edge(red, .55), sw=1.3, join=JOIN_ROUND)
        # anillos de la cornamenta
        s.curve([(14.8, 33), (25, 32.2), (38.4, 31.4)], stroke=alpha(INK, .22), sw=1.2)
        s.curve([(14.6, 24), (21, 23.6), (28.6, 24.6)], stroke=alpha(INK, .20), sw=1.1)
        # brillo
        s.curve([(19.4, 40), (17.4, 30), (17.6, 19)], stroke=alpha(W, .5), sw=1.6)
        s.circle(17.7, 13.6, .9, fill=alpha(W, .8))

    horn()
    with s.flip_x(50):
        horn()
    return s.bake()


# ------------------------------------------------------------------ HALO

def halo():
    s = Scene()
    cx, cy, rx, ry = 50, 11.8, 22, 5.2
    # resplandor
    s.ellipse(cx, cy, rx + 7, ry + 5, fill="#FFE08A20")
    s.ellipse(cx, cy, rx + 4.5, ry + 3.2, fill="#FFE08A2E")
    # aro
    s.ellipse(cx, cy, rx, ry, stroke="#C98A1B", sw=4.8)
    s.ellipse(cx, cy, rx, ry, stroke=lin(0, cy - ry, 0, cy + ry, [(0, "#FFF4B8"), (.5, "#FFD34A"), (1, "#EDA82A")]), sw=3.3)
    # brillo del aro (arco de la parte izquierda y superior)
    pts = []
    for k in range(0, 9):
        a = math.radians(200 + k * 8.5)
        pts.append((cx + rx * math.cos(a), cy + ry * math.sin(a)))
    s.curve(pts, stroke=alpha(W, .85), sw=1.0)
    # destellos
    s.sparkle(80, 9, 3.2, fill=alpha(W, .95))
    s.sparkle(21.5, 17.5, 2.2, fill="#FFE9A0")
    s.circle(72, 19.4, .8, fill=alpha(W, .8))
    return s.bake()


# ------------------------------------------------------------------ NINJA

def ninja():
    s = Scene()
    ink = "#2E2A45"
    red = "#D94F4F"
    soft_shadow(s, 50, 49.6, 31, 2.4, 0x26)
    # colas del nudo (detras de la cinta)
    s.path("M 84 44.5 C 88 46 92 49.5 95 55.5 L 91.4 57 C 88.5 52.5 86 49.5 83 48 Z",
           fill=lin(84, 44, 95, 57, [(0, hi(ink, .15)), (1, lo(ink, .3))]), stroke=edge(ink, .5), sw=.9, join=JOIN_ROUND)
    s.path("M 84 43.5 C 89 42.5 94 43.5 97.5 47 L 95.2 50.4 C 91.5 47.5 88 46.8 83.5 47.2 Z",
           fill=lin(84, 43, 97, 50, [(0, hi(ink, .15)), (1, lo(ink, .3))]), stroke=edge(ink, .5), sw=.9, join=JOIN_ROUND)
    # mascara de la parte baja de la cara
    mask = ("M 15.2 68.5 C 14.5 79 25 89 50 89 C 75 89 85.5 79 84.8 68.5 "
            "C 77 65.6 69 62.6 59 61.4 C 53 60.8 47 60.8 41 61.4 C 31 62.6 23 65.6 15.2 68.5 Z")
    s.path(mask, fill=lin(24, 60, 74, 90, [(0, hi(ink, .20)), (.5, ink), (1, lo(ink, .5))]), stroke=edge(ink, .55), sw=1.2, join=JOIN_ROUND)
    # pliegues de la tela
    s.curve([(26, 71), (36, 67.6), (46, 66.6)], stroke=alpha(W, .16), sw=1.5)
    s.curve([(54, 66.6), (64, 67.6), (74, 71)], stroke=alpha(INK, .5), sw=1.3)
    s.curve([(33, 81), (42, 84.4), (50, 85.2)], stroke=alpha(W, .09), sw=1.4)
    s.ellipse(33, 72.4, 7.4, 1.6, fill=alpha(W, .12))
    # cinta de la frente
    s.rect(15, 38.2, 70, 9.8, r=4.2, fill=lin(0, 38, 0, 48, [(0, hi(ink, .22)), (1, lo(ink, .45))]), stroke=edge(ink, .55), sw=1.2)
    s.rect(18, 43.5, 64, 2.3, r=1, fill=lin(0, 43, 0, 46, [(0, "#EE7777"), (1, "#C03D3D")]))
    s.line(20, 40.6, 43, 40.6, alpha(W, .2), 1.2)
    # chapa de acero
    s.rect(41.5, 38.8, 17, 8.4, r=2.2, fill=lin(41, 38, 59, 48, [(0, "#F1F5FB"), (.5, "#B7C0D3"), (1, "#7C879F")]), stroke="#58627C", sw=1)
    s.poly([(50, 40.6), (53, 43), (50, 45.4), (47, 43)], fill="#58627C")
    s.circle(44, 43, .8, fill="#58627C")
    s.circle(56, 43, .8, fill="#58627C")
    # nudo
    s.circle(85, 45.5, 4.6, fill=dfill(ink, 82, 41, 89, 50, .22, .35), stroke=edge(ink, .55), sw=1)
    s.ellipse(83.6, 43.8, 1.8, 1.0, fill=alpha(W, .4))
    return s.bake()


# ------------------------------------------------------------------ TOPHAT

def tophat():
    s = Scene()
    dk = "#2B2B3A"
    soft_shadow(s, 50, 38.8, 29, 2.6)
    # copa
    s.rect(31, 6.4, 38, 26.4, r=2, fill=lin(31, 0, 69, 0, [(0, lo(dk, .0)), (.28, hi(dk, .32)), (1, lo(dk, .35))]), stroke=edge(dk, .55), sw=1.2)
    s.ellipse(50, 6.4, 19, 2.8, fill=hi(dk, .22), stroke=edge(dk, .55), sw=1.1)
    # cinta granate con hebilla
    s.rect(31.6, 22.6, 36.8, 6.6, fill=lin(31, 0, 69, 0, [(0, "#9A3329"), (.3, "#D2574A"), (1, "#8C2D26")]))
    s.rect(46, 22.2, 8, 7.4, r=1.6, fill=dfill(GOLD, 46, 22, 54, 30, .55, .15), stroke="#9A5F0C", sw=.9)
    s.rect(48.2, 24.4, 3.6, 3.0, r=.6, fill="#8C2D26")
    # ala
    s.rect(19, 29.4, 62, 8.8, r=4.4, fill=lin(0, 29, 0, 38, [(0, hi("#22222E", .28)), (1, "#14141E")]), stroke=edge("#22222E", .6), sw=1.2)
    s.line(25, 31.4, 46, 31.4, alpha(W, .22), 1.2)
    # brillo de la copa
    s.rect(34, 9, 4, 12.5, r=2, fill=alpha(W, .24))
    s.rect(34, 23.4, 4, 3.4, r=1.4, fill=alpha(W, .2))
    return s.bake()


# ------------------------------------------------------------------ CAP

def cap():
    s = Scene()
    blue = "#3E7CD9"
    dk = "#2D5CA8"
    soft_shadow(s, 50, 45.5, 33, 3.0)
    d = "M 19 41 Q 21 9 52 9 Q 80 11 82 41 Q 50 45 19 41 Z"
    s.path(d, fill=lin(24, 10, 80, 44, [(0, hi(blue, .32)), (.55, blue), (1, lo(blue, .34))]), stroke=edge(blue, .55), sw=1.3, join=JOIN_ROUND)
    # costuras de los gajos
    s.curve([(52, 10.6), (43, 22), (35, 41.4)], stroke=alpha(INK, .25), sw=1.2)
    s.curve([(52, 10.6), (62, 22), (68, 42)], stroke=alpha(INK, .25), sw=1.2)
    s.curve([(52, 10.6), (52, 24), (52, 43)], stroke=alpha(INK, .18), sw=1.0)
    # banda inferior
    s.path("M 19.2 36.8 Q 50 40.6 81.8 36.8 L 82 41 Q 50 45 19 41 Z", fill=alpha(INK, .16))
    # brillo
    s.curve([(26, 28), (28, 19), (36, 13.2)], stroke=alpha(W, .45), sw=2.4)
    # boton
    s.circle(52, 9.8, 2.9, fill=dfill(dk, 50, 8, 55, 13, .4, .2), stroke=edge(dk, .5), sw=.9)
    # emblema
    s.star(63, 26, 4.8, points=5, inner=.5, fill=alpha(W, .92))
    # visera
    with s.rotate(-4, 68, 38.5):
        s.ellipse(68, 39, 24.5, 6, fill=lin(0, 33, 0, 45, [(0, hi(dk, .22)), (1, lo(dk, .42))]), stroke=edge(dk, .6), sw=1.3)
        s.curve([(48, 37.2), (62, 34.6), (80, 35.6)], stroke=alpha(W, .38), sw=1.5)
        s.curve([(50, 41), (68, 44.2), (86, 40.6)], stroke=alpha(INK, .22), sw=1.2)
    return s.bake()


# ------------------------------------------------------------------ SUNGLASSES

def sunglasses():
    s = Scene()
    frame = "#2A2644"
    # patillas
    s.line(24.5, 54, 14.5, 50, frame, 2.8)
    s.line(75.5, 54, 85.5, 50, frame, 2.8)
    s.circle(14.5, 50, 1.9, fill=lo(frame, .1))
    s.circle(85.5, 50, 1.9, fill=lo(frame, .1))
    # puente
    s.curve([(45, 55), (50, 52.6), (55, 55)], stroke=frame, sw=3.0)
    for x in (24, 52):
        s.rect(x, 49, 24, 16, r=7.2, fill=lin(x, 49, x + 24, 65, [(0, "#5A4D8C"), (.45, "#2A2348"), (1, "#15112A")]),
               stroke=lin(x, 49, x + 24, 65, [(0, "#7C70B0"), (1, "#3A3560")]), sw=1.7)
        # reflejos
        s.poly([(x + 4.5, 62), (x + 9.5, 51.4), (x + 12.8, 51.4), (x + 7.8, 62)], fill=alpha(W, .16))
        s.line(x + 4.4, 52.6, x + 9.6, 52.6, alpha(W, .6), 1.8)
        s.circle(x + 19.4, 61, .9, fill=alpha(W, .45))
    return s.bake()


# ------------------------------------------------------------------ CAPE

CAPE_RED = "#D9322B"
CAPE_CRIM = "#7A1F3D"


def cape_back():
    """La capa detras del cuerpo: se asoma por los lados y por abajo."""
    s = Scene()
    outer = ("M 50 70 L 36 70 C 24 66 12 58 6 49 C 2 66 2 84 5 96 C 18 100 34 95 50 98 "
             "C 66 95 82 100 95 96 C 98 84 98 66 94 49 C 88 58 76 66 64 70 Z")
    s.path(outer, fill=lin(0, 48, 100, 100, [(0, hi(CAPE_CRIM, .12)), (1, lo(CAPE_CRIM, .35))]), stroke=edge(CAPE_CRIM, .5), sw=1.3, join=JOIN_ROUND)
    inner = ("M 50 72 L 38 72 C 27 68 17 62 11 55 C 8 68 8 82 10 93 C 22 96 36 92 50 94 "
             "C 64 92 78 96 90 93 C 92 82 92 68 89 55 C 83 62 73 68 62 72 Z")
    s.path(inner, fill=lin(0, 52, 0, 96, [(0, hi(CAPE_RED, .25)), (.5, CAPE_RED), (1, lo(CAPE_RED, .35))]))
    # pliegues
    for sd in (-1, 1):
        cx = 50 + sd * 1
        s.curve([(50 + sd * 41, 60), (50 + sd * 42.5, 74), (50 + sd * 41, 90)], stroke=alpha(INK, .20), sw=1.4)
        s.curve([(50 + sd * 36, 66), (50 + sd * 35.5, 78), (50 + sd * 34.5, 90)], stroke=alpha(W, .14), sw=1.4)
    return s.bake()


def cape():
    """Cuello de la capa y broche dorado, por delante del cuerpo."""
    s = Scene()

    def wing():
        d = "M 9 61 C 17 70 25 79 33 88 C 34 91.5 33 95 30 99 C 20 97 11 98 6 97.5 C 3 85 4 72 9 61 Z"
        s.path(d, fill=lin(6, 60, 34, 99, [(0, hi(CAPE_CRIM, .15)), (1, lo(CAPE_CRIM, .35))]), stroke=edge(CAPE_CRIM, .5), sw=1.3, join=JOIN_ROUND)
        # forro rojo
        s.path("M 11.6 67 C 17.5 73.5 24 81 30 88 C 30.6 91 30 93.5 28 96 C 20 94.8 13 95.2 8.6 95 C 7 86 8 76 11.6 67 Z",
               fill=lin(8, 66, 30, 96, [(0, hi(CAPE_RED, .25)), (1, lo(CAPE_RED, .3))]))
        # ribete dorado en el borde interior
        s.curve([(9.5, 62.5), (20, 74), (32, 87)], stroke=alpha("#FFD34A", .9), sw=1.1)
        s.curve([(13, 69), (13.5, 82), (12, 92)], stroke=alpha(W, .16), sw=1.3)

    wing()
    with s.flip_x(50):
        wing()
    # cuello que cruza bajo la barbilla
    s.path("M 28 87 C 36 91 43 93 50 93.2 C 57 93 64 91 72 87 L 71 94.6 C 64 98.4 57 99.6 50 99.6 C 43 99.6 36 98.4 29 94.6 Z",
           fill=lin(0, 87, 0, 99.6, [(0, hi(CAPE_CRIM, .22)), (1, lo(CAPE_CRIM, .4))]), stroke=edge(CAPE_CRIM, .5), sw=1.2, join=JOIN_ROUND)
    s.curve([(30, 88.4), (40, 92), (50, 93), (60, 92), (70, 88.4)], stroke=alpha("#FFD34A", .85), sw=1.0)
    # broche
    s.circle(50, 94.6, 4.9, fill=dfill(GOLD, 46, 90, 55, 99, .55, .2), stroke="#9A5F0C", sw=1)
    gem(s, 50, 94.6, 2.4, "#E5456B")
    return s.bake()


# ------------------------------------------------------------------ PARTY

def party():
    s = Scene()
    purple = "#B57CF0"
    gold = "#FFD34A"
    soft_shadow(s, 50, 41.5, 20, 2.6)
    apex = (50, 9.5)
    bl, br, by = 31.5, 68.5, 38.0

    def xl(y):
        return bl + (br - bl) / 2.0 * (by - y) / (by - apex[1])

    def xr(y):
        return br - (br - bl) / 2.0 * (by - y) / (by - apex[1])

    d = "M %.1f %.1f L %.1f %.1f L %.1f %.1f Q 50 43 %.1f %.1f Z" % (bl, by, apex[0], apex[1], br, by, bl, by)
    s.path(d, fill=lin(36, 12, 66, 40, [(0, hi(purple, .38)), (.5, purple), (1, lo(purple, .36))]), stroke=edge(purple, .5), sw=1.3, join=JOIN_ROUND)
    # rayas doradas en diagonal
    for ya in (30.0, 21.5, 13.8):
        yb = ya + 3.4
        h = 2.5
        s.poly([(xl(ya), ya), (xr(yb), yb), (xr(yb + h), yb + h), (xl(ya + h), ya + h)], fill=lin(0, ya, 0, ya + 6, [(0, "#FFE58A"), (1, "#F2B83A")]))
    # lunares
    for (px, py, pr, pc) in ((43, 26.4, 1.3, W), (58, 33.6, 1.4, W), (47.5, 17.6, 1.1, "#FFB3D1"), (55.5, 25.4, 1.2, "#8EE3D0"), (40.5, 33.8, 1.2, "#FFB3D1")):
        s.circle(px, py, pr, fill=pc)
    # borde inferior
    s.path("M %.1f %.1f Q 50 42 %.1f %.1f" % (bl + .6, by - .3, br - .6, by - .3), stroke=lin(bl, 0, br, 0, [(0, "#E5A02B"), (.5, "#FFE58A"), (1, "#E5A02B")]), sw=3.0, cap=CAP_BUTT)
    # brillo
    s.curve([(40, 31), (44, 22), (48.2, 14.6)], stroke=alpha(W, .45), sw=1.7)
    # pompon
    for k in range(7):
        a = math.radians(k * 360 / 7 - 90)
        s.circle(50 + 4.2 * math.cos(a), 8.2 + 4.2 * math.sin(a), 2.0, fill=hi(gold, .2))
    s.circle(50, 8.2, 4.4, fill=rad(48.6, 6.6, 7, [(0, "#FFF1B5"), (1, "#F2B83A")]), stroke="#C98A1B", sw=.8)
    s.circle(48.6, 6.6, 1.3, fill=alpha(W, .85))
    # confeti
    s.rect(22, 22, 4.2, 2.2, r=.6, fill="#FF8FA3")
    with s.rotate(35, 76, 17):
        s.rect(73.8, 15.8, 4.6, 2.4, r=.6, fill="#8EE3D0")
    s.circle(27.5, 31, 1.4, fill=gold)
    s.circle(80.5, 28.5, 1.3, fill="#FFB3D1")
    s.sparkle(72.5, 8, 2.2, fill=alpha(W, .95))
    return s.bake()


# ------------------------------------------------------------------ paquete

def build():
    icons = {
        "acc.WITCH": witch(),
        "acc.PIRATE": pirate(),
        "acc.CHEF": chef(),
        "acc.SANTA": santa(),
        "acc.DEVIL": devil(),
        "acc.HALO": halo(),
        "acc.NINJA": ninja(),
        "acc.TOPHAT": tophat(),
        "acc.CAP": cap(),
        "acc.SUNGLASSES": sunglasses(),
        "acc.CAPE.back": cape_back(),
        "acc.CAPE": cape(),
        "acc.PARTY": party(),
    }
    return {"icons": icons}
