"""
Avatares vectoriales (paquete D): DEER COW DUCK BAT GHOST PUMPKIN ROBOT ALIEN DINO PHOENIX.
Portados de drawAvatar (AvatarArt.kt) en un lienzo de 100x100. Sin fondo ni accesorios.
Cada id "animal.X" lleva hombros ($shirt), partes de atras, cabeza y cara (ojos abiertos/cerrados con etiquetas).
"""
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, ".."))  # artlib
sys.path.insert(0, HERE)                       # _looks
from artlib import *  # noqa: E402,F401,F403
import _looks  # noqa: E402

OPEN, CLOSED = TAG_EYES_OPEN, TAG_EYES_CLOSED
INK = "#2B2B3A"
ANIMALS = ["DEER", "COW", "DUCK", "BAT", "GHOST", "PUMPKIN", "ROBOT", "ALIEN", "DINO", "PHOENIX"]


# ------------------------------------------------------------------ utilidades de color

def lt(c, t=.35):
    return mix(c, "#FFFFFF", t)


def sd(c, t=.3):
    return mix(c, "$dark", t)


def ol(c, t=.5):
    return mix(c, "$dark", t)


def wh(a):
    return "#FFFFFF%02X" % int(round(a * 255))


def kk(a, c="#2B2B3A"):
    return c + "%02X" % int(round(a * 255))


def new(animal):
    return Scene(100, 100, pal=_looks.palette(animal))


class _Null(object):
    def __enter__(self):
        return self

    def __exit__(self, *a):
        return False


def side(s, flip):
    """Espejo respecto al eje vertical (para el lado derecho)."""
    return s.flip_x(50) if flip else _Null()


# ------------------------------------------------------------------ arcos de elipse y formas comunes

def _pt(cx, cy, rx, ry, a):
    r = math.radians(a)
    return cx + rx * math.cos(r), cy + ry * math.sin(r)


def arc_to(p, cx, cy, rx, ry, a0, a1):
    x0, y0 = _pt(cx, cy, rx, ry, a0)
    x1, y1 = _pt(cx, cy, rx, ry, a1)
    p.M(x0, y0)
    p.A(rx, ry, 0, 1 if abs(a1 - a0) > 180 else 0, 1 if a1 > a0 else 0, x1, y1)
    return p


def smile(s, cx, cy, w, color="$dark", sw=1.9):
    """Boca de gato: dos arcos (como smile() de Android)."""
    p = Path()
    arc_to(p, cx - w / 2.0, cy + .5, w / 2.0, 3.5, 15, 165)
    arc_to(p, cx + w / 2.0, cy + .5, w / 2.0, 3.5, 15, 165)
    s.path(p, stroke=color, sw=sw, cap=CAP_ROUND, join=JOIN_ROUND)


def blush(s, r=7.4, a=.72):
    for x in (27, 73):
        hexa = "#FF8FA3"
        s.circle(x, 68, r, fill=rad(x, 68, r, [(0, hexa + "%02X" % int(255 * a)), (.55, hexa + "%02X" % int(255 * a * .62)), (1, hexa + "00")]))


def eye(s, cx, cy, r, white=False):
    """Ojo con brillos; abierto (TAG_EYES_OPEN) y cerrado (TAG_EYES_CLOSED)."""
    with s.tag(OPEN):
        if white:
            s.circle(cx, cy, r * 1.7, fill=rad(cx - r * .5, cy - r * .6, r * 2.7, [(0, "#FFFFFF"), (1, "#DADCEC")]),
                     stroke=kk(.38), sw=1.0)
        s.ellipse(cx, cy, r, r, fill=lin(cx, cy - r, cx, cy + r, [(0, "#1B1B2A"), (1, "#464662")]))
        s.circle(cx - r * .3, cy - r * .35, r * .38, fill="#FFFFFF")
        s.circle(cx + r * .4, cy + r * .42, r * .17, fill="#FFFFFFB3")
    with s.tag(CLOSED):
        w = r * 1.55 if white else r * 1.25
        s.path(Path().M(cx - w, cy - .5).Q(cx, cy + r * 1.0, cx + w, cy - .5), stroke=INK, sw=max(1.5, r * .42), cap=CAP_ROUND)


def shoulders(s):
    """Hombros y ropa (ranura $shirt) + sombra de la cabeza."""
    sh = "$shirt"
    s.oval(10, 82, 80, 44, fill=lin(22, 82, 76, 118, [(0, lt(sh, .32)), (.5, sh), (1, sd(sh, .4))]), stroke=ol(sh, .45), sw=1.3)
    s.oval(22, 84, 30, 10, fill=wh(.26))
    s.ellipse(50, 90, 25, 4.6, fill=alpha("$dark", .22))
    s.path(arc_to(Path(), 50, 104, 39, 21, 205, 335), stroke=wh(.3), sw=1.1, cap=CAP_ROUND)


HEAD_PATH = None


def head_shape(s, path=None, hi=.30, lo=.25, outline=True, fill=None, rim=True, gloss=True):
    """Cabeza ovalada (17,29)-(83,87): degradado radial con luz arriba a la izquierda, borde de luz y brillo."""
    cx, cy, rx, ry = 50.0, 58.0, 33.0, 29.0
    f = fill or rad(cx - 12, cy - 15, 56, [(0, lt("$head", hi)), (.5, "$head"), (1, sd("$head", lo))])
    st = ol("$head", .5) if outline else None
    if path is None:
        s.ellipse(cx, cy, rx, ry, fill=f, stroke=st, sw=1.3)
    else:
        s.path(path, fill=f, stroke=st, sw=1.3, join=JOIN_ROUND)
    if rim:
        s.path(arc_to(Path(), cx, cy, rx - 2.4, ry - 2.4, 195, 262), stroke=wh(.4), sw=1.2, cap=CAP_ROUND)
    if gloss:
        with s.rotate(-14, 40, 37):
            s.ellipse(40, 37, 11, 4.2, fill=wh(.42))


# ------------------------------------------------------------------ DEER

LEAF_EAR = "M 4 40 C 8 33.5 19 32 27.5 37 C 29 41.5 22 47 14 46.5 C 9 46 5.5 43.5 4 40 Z"
LEAF_EAR_IN = "M 8.5 40 C 11 36.5 18 35.5 24 38.5 C 24.5 41 20.5 43.5 15 43.5 C 11.5 43.3 9.5 42 8.5 40 Z"


def deer():
    s = new("DEER")
    shoulders(s)
    # cuernos (marron fijo) con contorno oscuro
    beam, br = Path(), Path()
    for k in (-1, 1):
        beam.M(50 + k * 13, 37).C(50 + k * 15, 30, 50 + k * 20.5, 20, 50 + k * 22, 9.5)
        br.M(50 + k * 19.2, 23).C(50 + k * 23, 21, 50 + k * 28, 18.5, 50 + k * 32, 16.5)
        br.M(50 + k * 22, 13.5).C(50 + k * 19.5, 10.5, 50 + k * 17, 8, 50 + k * 15, 5.8)
    for pth, w in ((beam, 3.4), (br, 2.7)):
        s.path(pth, stroke="#4E321D", sw=w + 1.8, cap=CAP_ROUND, join=JOIN_ROUND)
    for pth, w in ((beam, 3.4), (br, 2.7)):
        s.path(pth, stroke=lin(0, 38, 0, 5, [(0, "#6E4524"), (1, "#BC8758")]), sw=w, cap=CAP_ROUND, join=JOIN_ROUND)
    # orejas
    for flip in (False, True):
        with side(s, flip):
            with s.rotate(-38, 21, 40):
                s.path(LEAF_EAR, fill=lin(5, 34, 24, 47, [(0, lt("$head", .28)), (1, sd("$head", .22))]),
                       stroke=ol("$head", .5), sw=1.2, join=JOIN_ROUND)
                s.path(LEAF_EAR_IN, fill=lin(9, 36, 22, 44, [(0, lt("$inner", .3)), (1, "$inner")]))
    head_shape(s)
    # motas de la frente
    for (x, y, r) in ((36, 40, 2.3), (64, 40, 2.3), (50, 36, 2.6), (43, 44, 1.8), (57, 44, 1.8)):
        s.circle(x, y, r, fill=lin(x, y - r, x, y + r, [(0, lt("$light", .5)), (1, "$light")]))
    # hocico
    s.ellipse(50, 74, 16, 12, fill=lin(34, 62, 62, 86, [(0, lt("$light", .6)), (.6, "$light"), (1, mix("$light", "$head", .35))]),
              stroke=alpha(sd("$head", .3), .35), sw=1.0)
    blush(s)
    # nariz
    s.ellipse(50, 67, 6, 4, fill=lin(50, 63, 50, 71, [(0, "#4A4A60"), (1, INK)]))
    s.ellipse(47.8, 65.4, 2.4, 1.1, fill=wh(.55))
    s.line(50, 71, 50, 78, alpha(INK, .5), 1.1)
    eye(s, 37, 57, 4.4)
    eye(s, 63, 57, 4.4)
    smile(s, 50, 77, 5)
    return s.bake()


# ------------------------------------------------------------------ COW

HORN = "M 30 37 C 26.5 30 24.5 22 25.5 13.5 C 33 19 38 26 41 31.5 Z"


def _clamp_head(pts, k=.965):
    out = []
    for (x, y) in pts:
        u = math.sqrt(((x - 50) / 33.0) ** 2 + ((y - 58) / 29.0) ** 2)
        if u > k:
            x, y = 50 + (x - 50) * k / u, 58 + (y - 58) * k / u
        out.append((x, y))
    return out


def cow():
    s = new("COW")
    shoulders(s)
    rim_ear = mix("$dark", "$light", .32)
    for flip in (False, True):
        with side(s, flip):
            with s.rotate(-14, 15, 40):
                s.oval(2, 33, 26, 13, fill=lin(2, 33, 2, 46, [(0, lt("$dark", .22)), (1, "$dark")]), stroke=rim_ear, sw=1.0)
                s.oval(7, 36, 15, 7, fill=lin(7, 36, 7, 43, [(0, lt("$inner", .3)), (1, "$inner")]))
            s.path(HORN, fill=lin(30, 36, 26, 13, [(0, "#D9BE8A"), (1, "#FFF3D6")]), stroke="#A98B58", sw=1.0, join=JOIN_ROUND)
    head_shape(s, hi=.0, lo=.2, outline=False)
    # manchas (recortadas a la cabeza) y contorno encima
    patch1 = [(20.5, 38), (25, 31), (33, 31.5), (40, 36), (41.5, 43.5), (36, 50), (28, 51), (21, 47)]
    s.blob(_clamp_head(patch1), fill=lin(20, 30, 40, 52, [(0, lt("$dark", .18)), (1, "$dark")]))
    patch2 = [(62, 53), (68, 50), (75, 52.5), (78, 59), (74, 66), (67, 67.5), (61.5, 62)]
    s.blob(_clamp_head(patch2), fill=lin(60, 50, 78, 68, [(0, lt("$dark", .18)), (1, "$dark")]))
    s.ellipse(50, 58, 33, 29, stroke=ol("$head", .5), sw=1.3)
    with s.rotate(-14, 40, 37):
        s.ellipse(40, 37, 11, 4.2, fill=wh(.5))
    blush(s)
    # morro rosa
    s.ellipse(50, 74.5, 23, 12.5, fill=lin(27, 62, 73, 87, [(0, lt("$inner", .5)), (.55, "$inner"), (1, mix("$inner", "#E07A8E", .4))]),
              stroke=alpha(ol("$inner", .45), .6), sw=1.0)
    s.ellipse(39.5, 72.5, 2.5, 3.5, fill=alpha(INK, .55))
    s.ellipse(60.5, 72.5, 2.5, 3.5, fill=alpha(INK, .55))
    s.ellipse(40, 66.8, 9, 2.4, fill=wh(.4))
    eye(s, 37, 57, 4.4)
    eye(s, 63, 57, 4.4)
    smile(s, 50, 80, 6)
    return s.bake()


# ------------------------------------------------------------------ DUCK

def duck():
    s = new("DUCK")
    shoulders(s)
    # mechon de plumas
    for (ang, cx, cy, ry) in ((-9, 46.5, 28, 8.4), (10, 53.5, 26.5, 8.8)):
        with s.rotate(ang, cx, cy + 6):
            s.path("M %s %s C %s %s %s %s %s %s C %s %s %s %s %s %s Z" % (
                cx, cy - ry - 1, cx + 5.4, cy - ry + 3, cx + 5, cy + ry - 2, cx, cy + ry,
                cx - 5, cy + ry - 2, cx - 5.4, cy - ry + 3, cx, cy - ry - 1),
                fill=lin(cx, cy - ry, cx, cy + ry, [(0, lt("$head", .3)), (1, "$head")]), stroke=ol("$head", .5), sw=1.1, join=JOIN_ROUND)
    head_shape(s)
    blush(s)
    # pico ancho
    s.ellipse(50, 72, 21, 10, fill=lin(30, 62, 66, 82, [(0, lt("$inner", .4)), (.55, "$inner"), (1, sd("$inner", .3))]),
              stroke=ol("$inner", .42), sw=1.2)
    s.ellipse(40, 66.6, 9.5, 2.4, fill=wh(.5))
    s.line(32, 72, 68, 72, alpha("$dark", .45), 1.2)
    s.circle(44.5, 67, 1.3, fill=alpha("$dark", .65))
    s.circle(55.5, 67, 1.3, fill=alpha("$dark", .65))
    eye(s, 37, 57, 4.4)
    eye(s, 63, 57, 4.4)
    return s.bake()


# ------------------------------------------------------------------ BAT

WING = ("M 68 47 C 74 38 82 30 91 27 Q 83 44 96 52 Q 85 62 88 76 Q 79 70 69 73 Z")


def bat():
    s = new("BAT")
    shoulders(s)
    wing_f = lin(68, 50, 96, 50, [(0, mix("$head", "$dark", .32)), (1, mix("$head", "$dark", .62))])
    rim = mix("$head", "$light", .45)
    for flip in (False, True):
        with side(s, flip):
            s.path(WING, fill=wing_f, stroke=rim, sw=1.3, join=JOIN_ROUND)
            # dedos del ala
            p = Path()
            for (x, y) in ((91, 27), (96, 52), (88, 76)):
                p.M(72, 58).Q((72 + x) / 2.0 + 1, (58 + y) / 2.0 - 2, x, y)
            s.path(p, stroke=alpha(rim, .6), sw=1.0, cap=CAP_ROUND)
    # orejas largas
    for flip in (False, True):
        with side(s, flip):
            s.path("M 20 47 C 19.5 31 22 17 28 9 C 34 16 40 24 44 32 Z",
                   fill=lin(22, 12, 38, 40, [(0, lt("$head", .22)), (1, sd("$head", .22))]), stroke=ol("$head", .5), sw=1.3, join=JOIN_ROUND)
            s.path("M 25 40 C 25 30 26.5 22 28.5 18 C 32 23 35 28 38 33 Z",
                   fill=lin(26, 20, 36, 38, [(0, lt("$inner", .3)), (1, mix("$inner", "$head", .25))]))
    head_shape(s)
    # hocico claro
    s.ellipse(50, 73, 15, 10, fill=lin(35, 63, 62, 83, [(0, lt("$light", .35)), (1, mix("$light", "$head", .35))]))
    blush(s)
    s.ellipse(50, 64.6, 3.4, 2.2, fill=sd("$head", .6))
    eye(s, 37, 57, 3.8, white=True)
    eye(s, 63, 57, 3.8, white=True)
    smile(s, 50, 71, 6)
    for pts in (((44, 73.5), (48.4, 73.5), (46.2, 80)), ((51.6, 73.5), (56, 73.5), (53.8, 80))):
        s.tri(*pts, fill="#FFFFFF", stroke=kk(.4, "#3A2A5E"), sw=.7, join=JOIN_ROUND)
    return s.bake()


# ------------------------------------------------------------------ GHOST

def _sheet():
    pts = [(91, 94), (81.5, 100), (71, 94), (60.5, 100), (50, 94), (39.5, 100), (29, 94), (18.5, 100), (9, 94)]
    cmds = [("M", 10, 62), ("C", 8, 34, 24, 18, 50, 18), ("C", 76, 18, 92, 34, 90, 62), ("L", 91, 94)]
    cmds += catmull(pts)[1:]
    cmds += [("L", 10, 62), ("Z",)]
    return cmds


def ghost():
    s = new("GHOST")
    shoulders(s)
    s.circle(50, 58, 48, fill=rad(50, 58, 48, [(.6, "#FFFFFF2E"), (1, "#FFFFFF00")]))
    sheet = _sheet()
    s.path(sheet, fill=lin(30, 18, 70, 100, [(0, lt("$head", .8)), (.55, mix("$light", "$head", .4)), (1, mix("$light", "$dark", .12))]),
           stroke=ol("$light", .35), sw=1.3, join=JOIN_ROUND)
    # cabeza: resplandor suave sobre la sabana (sin contorno duro)
    s.ellipse(50, 58, 33, 29, fill=rad(40, 44, 50, [(0, lt("$head", .55)), (.6, "$head"), (1, mix("$head", "$light", .55))]))
    s.path(arc_to(Path(), 50, 58, 31, 27, 195, 262), stroke=wh(.55), sw=1.2, cap=CAP_ROUND)
    with s.rotate(-14, 40, 37):
        s.ellipse(40, 37, 11, 4.2, fill=wh(.55))
    blush(s)
    eye(s, 36, 56, 5.2)
    eye(s, 64, 56, 5.2)
    # boca en o
    s.oval(44, 66, 12, 14, fill=lin(44, 66, 44, 80, [(0, "$dark"), (1, sd("$dark", .0))]))
    s.oval(46, 73, 8, 5, fill="#FF8FA399")
    return s.bake()


# ------------------------------------------------------------------ PUMPKIN

def pumpkin():
    s = new("PUMPKIN")
    shoulders(s)
    stem_c = "#6B4A2A"
    s.path("M 45 33 C 44 27 44.5 20 46.5 14.5 C 49.5 12.8 53 13.2 55.5 15 C 56 21 56 27 55 33 Z",
           fill=lin(44, 20, 56, 20, [(0, "#8A6238"), (.5, stem_c), (1, "#4A3119")]), stroke="#3A2412", sw=1.1, join=JOIN_ROUND)
    with s.rotate(-28, 56, 25):
        s.path("M 55 25 C 59 17 69 15 76 18.5 C 72 25 63 28 55 25 Z", fill=lin(55, 18, 76, 26, [(0, "#7DCB66"), (1, "#3F8F3A")]), stroke="#2F6B2C", sw=1.0, join=JOIN_ROUND)
        s.line(57, 24, 71, 20.5, "#2F6B2C", 0.9)
    # gajos: laterales, medios y central
    lobes = ((25, 17, 25.5, .0), (75, 17, 25.5, .0), (37, 17.5, 28, .0), (63, 17.5, 28, .0), (50, 17.5, 29, .0))
    for (cx, rx, ry, _) in lobes:
        s.ellipse(cx, 59, rx, ry, fill=lin(cx - rx, 0, cx + rx, 0, [(0, sd("$head", .34)), (.42, lt("$head", .22)), (1, sd("$head", .34))]),
                  stroke=ol("$head", .5), sw=1.2)
    with s.rotate(-14, 38, 38):
        s.ellipse(38, 38, 12, 4.2, fill=wh(.42))
    blush(s)
    glow = "#FFD34A"
    dk = "#2B1608"
    for cx in (37, 63):
        with s.tag(OPEN):
            s.tri((cx - 8, 58), (cx + 8, 58), (cx, 46), fill=dk, stroke=alpha(glow, .55), sw=1.0, join=JOIN_ROUND)
            s.tri((cx - 4.5, 56.5), (cx + 4.5, 56.5), (cx, 49), fill=lin(cx, 49, cx, 57, [(0, "#FFE98A"), (1, "#FFB02E")]))
        with s.tag(CLOSED):
            s.tri((cx - 8, 58), (cx + 8, 58), (cx, 55), fill=dk, stroke=alpha(glow, .5), sw=1.0, join=JOIN_ROUND)
    mouth = [(28, 70), (37, 65), (43, 73), (50, 66), (57, 73), (63, 65), (72, 70), (66, 82), (58, 78), (50, 82), (42, 78), (34, 82)]
    s.poly(mouth, fill=lin(0, 64, 0, 83, [(0, dk), (.55, dk), (1, "#8A3A0E")]), stroke=alpha(glow, .7), sw=1.2, join=JOIN_ROUND)
    return s.bake()


# ------------------------------------------------------------------ ROBOT

def robot():
    s = new("ROBOT")
    shoulders(s)
    metal = mix("$head", "$dark", .35)
    # antena
    s.line(50, 33, 50, 12, "$dark", 3)
    s.circle(50, 10, 8.4, fill=rad(50, 10, 8.4, [(0, "#FF5A5A66"), (1, "#FF5A5A00")]))
    s.circle(50, 10, 4.6, fill=rad(48.6, 8.4, 6.4, [(0, "#FFC2B8"), (.45, "#FF5A5A"), (1, "#C42E3A")]), stroke="#7A1C2A", sw=.8)
    s.circle(48.6, 8.8, 1.4, fill=wh(.8))
    # orejeras
    for x in (7, 80):
        s.rect(x, 46, 13, 24, r=5, fill=lin(x, 46, x + 13, 70, [(0, lt(metal, .3)), (1, sd(metal, .3))]), stroke=ol(metal, .5), sw=1.2)
        s.rect(x + 3.5, 52, 6, 12, r=3, fill=alpha("$dark", .38))
    # cabeza metalica
    s.rect(17, 29, 66, 58, r=18, fill=lin(22, 29, 80, 87, [(0, lt("$head", .38)), (.5, "$head"), (1, sd("$head", .3))]), stroke=ol("$head", .55), sw=1.5, join=JOIN_ROUND)
    s.path(arc_to(Path(), 50, 58, 30.6, 26.6, 188, 252), stroke=wh(.45), sw=1.2, cap=CAP_ROUND)
    for (x, y) in ((24, 35), (76, 35), (24, 82), (76, 82)):
        s.circle(x, y, 2.3, fill=lin(x - 2, y - 2, x + 2, y + 2, [(0, lt("$head", .2)), (1, sd("$head", .55))]), stroke=alpha("$dark", .5), sw=.6)
        s.line(x - 1.1, y - 0.2, x + 1.1, y + 0.2, alpha("$dark", .7), .6, cap=CAP_BUTT)
    with s.rotate(-8, 40, 37):
        s.ellipse(40, 37, 11, 3.6, fill=wh(.38))
    # pantalla
    s.rect(23, 44, 54, 38, r=11, fill=lin(23, 44, 23, 82, [(0, "#27345C"), (1, "#111A33")]), stroke=alpha(lt("$inner", .2), .5), sw=1.2)
    s.rect(26.5, 46.2, 30, 4.8, r=2.4, fill=wh(.2))
    blush(s, r=7, a=.55)
    for (x, wid) in ((31, 16), (53, 16)):
        with s.tag(OPEN):
            s.rect(x, 48.5, wid, 15, r=5, fill=alpha("$inner", .32))
            s.rect(x + 2, 52, 12, 11, r=3, fill=lin(0, 52, 0, 63, [(0, lt("$inner", .55)), (1, "$inner")]))
            s.rect(x + 4, 53.2, 4.5, 2, r=1, fill=wh(.7))
        with s.tag(CLOSED):
            s.rect(x + 2, 52.8, 12, 2.4, r=1.2, fill="$inner")
    # boca de rejilla
    s.rect(37, 70, 26, 4.2, r=2, fill=lin(0, 70, 0, 74, [(0, lt("$inner", .35)), (1, "$inner")]))
    for x in (41, 47, 53, 59):
        s.rect(x - .8, 69.5, 1.6, 5.2, fill="#1B2540")
    return s.bake()


# ------------------------------------------------------------------ ALIEN

ALIEN_HEAD = "M 17 57 C 17 40 31 29 50 29 C 69 29 83 40 83 57 C 83 72 67 87 50 87 C 33 87 17 72 17 57 Z"


def alien():
    s = new("ALIEN")
    shoulders(s)
    for k in (-1, 1):
        x0, y0, x1, y1 = 50 + k * 12, 34, 50 + k * 19, 13
        s.curve([(x0, y0), (50 + k * 15, 24), (x1, y1)], "$dark", 2.6)
        c = "$inner" if k < 0 else "#FF8FC0"
        s.circle(50 + k * 19.5, 12, 8, fill=rad(50 + k * 19.5, 12, 8, [(0, alpha(c, .45)), (1, alpha(c, 0))]))
        s.circle(50 + k * 19.5, 12, 4.8, fill=rad(50 + k * 19.5 - 1.5, 10.2, 6.6, [(0, lt(c, .7)), (.55, c), (1, mix(c, "$dark", .35))]),
                 stroke=ol(c, .45), sw=.9)
        s.circle(50 + k * 19.5 - 1.5, 10.2, 1.3, fill=wh(.85))
    head_shape(s, path=ALIEN_HEAD, hi=.34, lo=.28, gloss=False)
    s.ellipse(50, 35, 10, 4, fill=wh(.4))
    blush(s)
    ek = "#14142B"
    for (cx, rot) in ((33, -20), (67, 20)):
        with s.tag(OPEN):
            with s.rotate(rot, cx, 56):
                s.ellipse(cx, 56, 10, 8.6, fill=rad(cx - 2, 51, 14, [(0, "#3E3F7A"), (.45, ek), (1, "#0B0B1A")]))
            s.circle(cx - 3, 52, 2.4, fill=wh(.88))
            s.circle(cx + 3.2, 59, 1.1, fill=wh(.5))
        with s.tag(CLOSED):
            with s.rotate(rot, cx, 56):
                s.path(Path().M(cx - 10, 55.6).Q(cx, 60.5, cx + 10, 55.6), stroke=ek, sw=2.2, cap=CAP_ROUND)
    s.circle(48.3, 66.6, .8, fill=alpha("$dark", .6))
    s.circle(51.7, 66.6, .8, fill=alpha("$dark", .6))
    smile(s, 50, 73, 5, "#2A6B3E")
    return s.bake()


# ------------------------------------------------------------------ DINO

def dino():
    s = new("DINO")
    shoulders(s)
    for k in range(-2, 3):
        x = 50 + k * 12
        yb = 36 - (0 if k == 0 else 1)
        yt = 17 + abs(k) * 4
        s.tri((x - 6.5, yb), (x, yt), (x + 6.5, yb), fill=lin(x - 6, yb, x + 3, yt, [(0, sd("$inner", .25)), (1, lt("$inner", .38))]),
              stroke=ol("$inner", .5), sw=1.2, join=JOIN_ROUND)
    head_shape(s)
    # vientre/hocico claro
    s.ellipse(50, 73.5, 20, 13.5, fill=lin(30, 60, 70, 87, [(0, lt("$light", .55)), (.55, "$light"), (1, mix("$light", "$head", .45))]),
              stroke=alpha(sd("$head", .3), .35), sw=1.0)
    for (x, y, r) in ((27, 42, 3.2), (73, 44, 3.2), (35, 36, 2.6), (66, 36, 2.2)):
        s.circle(x, y, r, fill=alpha(mix("$head", "$dark", .3), .85))
    blush(s)
    eye(s, 35, 53, 4.2, white=True)
    eye(s, 65, 53, 4.2, white=True)
    s.circle(43, 66, 1.7, fill=alpha("$dark", .72))
    s.circle(57, 66, 1.7, fill=alpha("$dark", .72))
    smile(s, 50, 77, 7)
    for pts in (((40, 78.5), (44, 78.5), (42, 83)), ((56, 78.5), (60, 78.5), (58, 83))):
        s.tri(*pts, fill="#FFFFFF", stroke=kk(.4, "#3F6B22"), sw=.7, join=JOIN_ROUND)
    return s.bake()


# ------------------------------------------------------------------ PHOENIX

RED, ORG, YEL = "#E5382B", "#FF8A1F", "#FFD34A"


def _flame(L, W, lean=0.0):
    return (Path().M(lean, -L)
            .C(W * .12 + lean * .5, -L * .62, W * .5, -L * .5, W * .5, -L * .2)
            .C(W * .5, -L * .04, W * .3, 0, 0, 0)
            .C(-W * .3, 0, -W * .5, -L * .04, -W * .5, -L * .2)
            .C(-W * .5, -L * .5, -W * .12 + lean * .5, -L * .62, lean, -L).Z())


def flame(s, bx, by, ang, L, W, lean=0.0):
    with s.translate(bx, by):
        with s.rotate(ang, 0, 0):
            s.path(_flame(L, W, lean), fill=lin(0, 0, 0, -L, [(0, "#B5231E"), (1, "#F2503A")]))
            s.path(_flame(L * .78, W * .72, lean * .7), fill=lin(0, 0, 0, -L * .78, [(0, "#FF7A1A"), (1, "#FFB347")]))
            s.path(_flame(L * .54, W * .45, lean * .4), fill=lin(0, 0, 0, -L * .54, [(0, "#FFC02E"), (1, "#FFEB9A")]))


def phoenix():
    s = new("PHOENIX")
    shoulders(s)
    # alas de fuego a los lados
    for flip in (False, True):
        with side(s, flip):
            flame(s, 27, 66, -52, 25, 15, lean=-1.5)
            flame(s, 27, 62, -76, 22, 13, lean=-1.5)
    # cresta
    flame(s, 38, 42, -28, 31, 16, lean=1.5)
    flame(s, 62, 42, 28, 31, 16, lean=-1.5)
    flame(s, 50, 42, 0, 37, 19)
    head_shape(s)
    # marca clara del pico y la cara
    s.ellipse(50, 71.5, 20, 15.5, fill=lin(30, 56, 70, 87, [(0, lt("$light", .5)), (.55, "$light"), (1, mix("$light", "$head", .4))]),
              stroke=alpha(sd("$head", .3), .35), sw=1.0)
    # plumas de la frente
    for (x, a, h) in ((44, -12, 8), (50, 0, 10), (56, 12, 8)):
        with s.translate(x, 36):
            with s.rotate(a, 0, 0):
                s.path(_flame(h, 5.2), fill=lin(0, 0, 0, -h, [(0, "#FFB02E"), (1, "#FFE08A")]))
    blush(s)
    eye(s, 37, 57, 4.4)
    eye(s, 63, 57, 4.4)
    # pico
    s.path("M 43 62 Q 50 59.4 57 62 Q 55.5 70 50 77 Q 44.5 70 43 62 Z", fill=lin(43, 60, 57, 77, [(0, "#FFD36E"), (.5, "#FFB02E"), (1, "#E08A10")]),
           stroke="#B87510", sw=1.0, join=JOIN_ROUND)
    s.line(44.5, 65.2, 55.5, 65.2, "#B87510", 1.3)
    s.ellipse(47.5, 63.2, 2.4, 1.0, fill=wh(.6))
    return s.bake()


# ------------------------------------------------------------------ paquete

MAKERS = {
    "DEER": deer, "COW": cow, "DUCK": duck, "BAT": bat, "GHOST": ghost,
    "PUMPKIN": pumpkin, "ROBOT": robot, "ALIEN": alien, "DINO": dino, "PHOENIX": phoenix,
}


def build():
    icons = {"animal.%s" % a: MAKERS[a]() for a in ANIMALS}
    return {"icons": icons, "palettes": _looks.all_palettes(ANIMALS)}
