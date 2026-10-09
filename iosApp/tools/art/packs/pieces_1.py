import os
import sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from _pk import *  # noqa: F401,F403
import _animals

G = "#6FBF73"
GD = "#3E8A5A"


def stem(p, x1, y1, x2, y2, w=3.2):
    p.line(x1, y1, x2, y2, "#4E9A5A", w)


# ------------------------------------------------------------------ Jardin Zen
def garden_1():  # margarita
    p = Pc()
    stem(p, 50, 52, 50, 88)
    p.leaf(50, 80, 22, 6, -30, G)
    p.leaf(50, 72, 20, 5, -150, G)
    p.flower(50, 38, 30, 11, "#FFFFFF", "#FFC83D", rot=-90)
    p.shine(44, 30, 5, 2.6, -40, .6)
    return p.out()


def garden_2():  # loto
    p = Pc()
    p.ell(50, 80, 38, 9, ["#9FDCF0", "#4FA9CF"])
    p.ell(26, 80, 15, 5, G, sw=1)
    for a, c in ((-72, "#F4A3C0"), (72, "#F4A3C0"), (-38, "#F8B7CE"), (38, "#F8B7CE"), (0, "#FFD3E2")):
        p.ell(50 + math.sin(math.radians(a)) * 20, 62 - math.cos(math.radians(a)) * 13 - (4 if a == 0 else 0), 9, 21, c, rot=a, sw=1.2)
    p.circ(50, 62, 5, "#FFC83D", sw=1)
    return p.out()


def garden_3():  # brote
    p = Pc()
    p.path("M 22 86 Q 50 62 78 86 Z", ["#B88A5E", "#7A5230"])
    stem(p, 50, 76, 50, 52, 3.4)
    p.path("M 50 56 Q 22 52 18 28 Q 44 26 50 56 Z", ["#A8E6A0", "#4E9A5A"])
    p.path("M 50 52 Q 78 48 84 24 Q 56 22 50 52 Z", ["#9ADD92", "#3E8A5A"])
    p.shine(30, 38, 5, 2, 20)
    return p.out()


def garden_4():  # trebol
    p = Pc()
    stem(p, 50, 56, 56, 90, 3)
    for a in (-135, -45, 45, 135):
        x = 50 + math.cos(math.radians(a)) * 17
        y = 48 + math.sin(math.radians(a)) * 17
        with p.s.rotate(a + 90, x, y):
            p.path("M %f %f C %f %f %f %f %f %f C %f %f %f %f %f %f Z" % (x, y + 14, x - 26, y - 2, x - 12, y - 24, x, y - 8, x + 12, y - 24, x + 26, y - 2, x, y + 14), ["#8EE08A", "#3E9A5A"], sw=1.2)
    p.circ(50, 48, 3, "#2E7A4A", ol=False)
    return p.out()


def garden_5():  # cerezo
    p = Pc()
    p.path("M 14 90 Q 34 62 44 48 Q 56 30 84 22", ["#9A6B4A", "#5A3A26"], ol=False)
    p.curve("M 14 90 Q 34 62 44 48 Q 56 30 84 22", "#6A4630", 5)
    for x, y, r in ((30, 44, 12), (52, 30, 15), (72, 40, 12), (60, 58, 11), (82, 22, 9)):
        p.flower(x, y, r * 1.5, 5, "#FFC3D6", "#FF8FB0", rot=-90)
    return p.out()


def garden_6():  # bosque
    p = Pc()
    for x, y, s_ in ((30, 70, 0.8), (68, 72, 0.86), (50, 62, 1.1)):
        p.rect(x - 3 * s_, y + 8 * s_, 6 * s_, 14 * s_, "#8A5A3A", r=1)
        for k, (w, h) in enumerate(((22, 20), (17, 18), (12, 16))):
            yy = y + 10 * s_ - k * 14 * s_
            p.poly([(x, yy - h * s_), (x + w * s_, yy), (x - w * s_, yy)], ["#7BD08B", "#2E7A4A"], sw=1.1)
    return p.out()


def garden_7():  # colibri
    p = Pc(shadow=False)
    p.path("M 28 58 Q 46 34 70 44 Q 78 60 58 70 Q 40 76 28 58 Z", ["#5CE0C8", "#1E8F8A"])
    p.path("M 40 52 Q 28 22 56 24 Q 60 40 52 54 Z", ["#8AF0E0", "#2BA8A0"])
    p.circ(72, 42, 11, ["#FF7A9A", "#C83A66"])
    p.line(80, 40, 98, 28, "#3A2A3A", 2.2)
    p.circ(75, 39, 2.2, "#2B1B3A", ol=False)
    p.path("M 30 60 L 12 80 L 26 74 L 18 90 L 36 70 Z", ["#2BA8A0", "#1E6F8A"])
    p.sparkle(20, 24, 6, "#FFC83D")
    return p.out()


def garden_8():
    return _animals.head("FOX", "JADE")


def garden_9():  # mariposa
    p = Pc(shadow=False)
    p.path("M 50 50 C 30 10 6 20 14 46 C 18 60 36 62 50 54 Z", ["#FFB86B", "#E8642B"])
    p.path("M 50 50 C 70 10 94 20 86 46 C 82 60 64 62 50 54 Z", ["#FFB86B", "#E8642B"])
    p.path("M 50 56 C 32 58 20 72 30 84 C 40 90 48 74 50 56 Z", ["#FFD79A", "#E8892B"])
    p.path("M 50 56 C 68 58 80 72 70 84 C 60 90 52 74 50 56 Z", ["#FFD79A", "#E8892B"])
    for x, y in ((24, 36), (76, 36)):
        p.circ(x, y, 5, "#FFFFFF", ol=False)
        p.circ(x, y, 2.4, "#4A2A6A", ol=False)
    p.rect(47, 38, 6, 38, ["#5A3A5A", "#2B1B3A"], r=3)
    p.curve("M 50 38 Q 44 24 38 20", "#2B1B3A", 1.6)
    p.curve("M 50 38 Q 56 24 62 20", "#2B1B3A", 1.6)
    return p.out()


def garden_10():  # fuente
    p = Pc()
    p.ell(50, 80, 40, 10, ["#E9E4F2", "#A9A0C0"])
    p.ell(50, 76, 34, 7, ["#9FDCF0", "#4FA9CF"], ol=False)
    p.rect(44, 46, 12, 30, ["#E9E4F2", "#A9A0C0"], r=3)
    p.ell(50, 46, 22, 6, ["#E9E4F2", "#A9A0C0"])
    p.ell(50, 44, 17, 3.5, ["#9FDCF0", "#4FA9CF"], ol=False)
    for dx in (-14, 0, 14):
        p.curve("M 50 42 Q %d 18 %d 40" % (50 + dx * 1.2, 50 + dx * 2), "#8FD6F2", 3)
    p.curve("M 50 42 Q 50 14 50 38", "#BDEBFA", 3)
    return p.out()


# ------------------------------------------------------------------ Cielo
def sky_1():  # amanecer
    p = Pc(shadow=False)
    p.rect(8, 58, 84, 30, ["#FFB36B", "#E8642B"], r=6)
    p.s.path("M 24 62 A 26 26 0 0 1 76 62 Z", fill=lin(0, 36, 0, 62, [(0, "#FFF1A8"), (1, "#FFB24A")]), stroke="#C96A1B", sw=1.5, join=JOIN_ROUND)
    for a in range(-70, 71, 35):
        x = 50 + math.sin(math.radians(a)) * 36
        y = 62 - math.cos(math.radians(a)) * 36
        p.line(50 + math.sin(math.radians(a)) * 30, 62 - math.cos(math.radians(a)) * 30, x, y, "#FFD36E", 3)
    p.curve("M 14 74 Q 50 66 86 74", "#FFF1D0", 2.2, op=.7)
    return p.out()


def sky_2():  # luna
    p = Pc(shadow=False)
    p.path("M 62 14 A 36 36 0 1 0 84 66 A 30 30 0 1 1 62 14 Z", ["#FFF4B8", "#E8B83B"])
    p.sparkle(78, 26, 7, "#FFFFFF")
    p.sparkle(28, 22, 4, "#FFFFFF")
    p.shine(34, 38, 6, 3, 50, .5)
    return p.out()


def sky_3():  # nube
    p = Pc()
    p.path("M 22 72 C 4 72 4 50 22 50 C 22 32 48 26 56 42 C 66 30 90 38 84 56 C 98 58 98 72 82 72 Z", ["#FFFFFF", "#B9D3F0"])
    p.shine(34, 46, 8, 3, -20, .7)
    return p.out()


def sky_4():  # copo
    p = Pc(shadow=False)
    for a in (0, 60, 120):
        with p.s.rotate(a, 50, 50):
            p.line(50, 12, 50, 88, "#9BD0F5", 4)
            for y in (26, 74):
                p.line(50, y, 40, y - 9, "#9BD0F5", 3)
                p.line(50, y, 60, y - 9, "#9BD0F5", 3)
    p.circ(50, 50, 6, "#E6F4FF")
    return p.out()


def sky_5():  # brisa
    p = Pc(shadow=False)
    p.curve("M 10 34 H 62 C 82 34 82 14 66 16", "#A8D8F5", 6)
    p.curve("M 10 52 H 76 C 94 52 94 72 76 70", "#7FC0EB", 6)
    p.curve("M 20 70 H 50 C 62 70 62 84 52 84", "#C5E6FA", 5)
    p.sparkle(84, 36, 5)
    return p.out()


def sky_6():  # rayo
    p = Pc()
    p.poly([(58, 6), (24, 54), (46, 54), (36, 94), (76, 40), (54, 40)], ["#FFE86B", "#F29A1F"])
    p.shine(46, 28, 5, 2, 55, .6)
    return p.out()


def sky_7():  # planeta
    p = Pc(shadow=False)
    p.circ(50, 52, 26, ["#8AD0F5", "#3F7FD0"])
    p.path("M 34 40 Q 50 30 62 44 Q 56 54 44 50 Q 34 52 34 40 Z", "#7BD08B", ol=False)
    p.path("M 52 64 Q 70 58 74 68 Q 66 78 54 74 Z", "#7BD08B", ol=False)
    p.s.ellipse(50, 56, 42, 8, stroke="#FFD79A", sw=3.2, op=.95)
    p.shine(40, 40, 6, 3, -40, .5)
    return p.out()


def sky_8():  # cohete
    p = Pc()
    p.path("M 50 8 C 70 24 72 52 66 70 L 34 70 C 28 52 30 24 50 8 Z", ["#FFFFFF", "#C9D3E8"])
    p.path("M 50 8 C 58 16 62 26 64 34 L 36 34 C 38 26 42 16 50 8 Z", ["#FF8A7A", "#D6402B"])
    p.circ(50, 46, 8, ["#9FDCF0", "#3F7FD0"])
    p.path("M 34 56 L 18 74 L 34 70 Z", ["#FF8A7A", "#D6402B"])
    p.path("M 66 56 L 82 74 L 66 70 Z", ["#FF8A7A", "#D6402B"])
    p.path("M 40 72 Q 50 98 60 72 Z", ["#FFE86B", "#F29A1F"])
    return p.out()


def sky_9():  # arcoiris
    p = Pc(shadow=False)
    for i, c in enumerate(("#E8586D", "#FFA24A", "#FFD84A", "#6FCF7A", "#4FA9E8", "#8A6FE0")):
        r = 42 - i * 5.5
        p.s.path("M %f 78 A %f %f 0 0 1 %f 78" % (50 - r, r, r, 50 + r), stroke=c, sw=5.4, cap=CAP_BUTT)
    p.ell(22, 80, 14, 7, ["#FFFFFF", "#C5DDF5"])
    p.ell(78, 80, 14, 7, ["#FFFFFF", "#C5DDF5"])
    return p.out()


def sky_10():  # lluvia de estrellas
    p = Pc(shadow=False)
    p.path("M 86 14 L 30 62", ["#FFF1A8", "#FFD36E"], ol=False)
    p.s.path("M 90 10 Q 60 30 30 60", stroke=lin(90, 10, 30, 60, [(0, "#FFFFFF00"), (1, "#FFE08A")]), sw=7, cap=CAP_ROUND)
    p.star(30, 62, 16, ["#FFF1A8", "#F2A91F"])
    p.sparkle(70, 66, 6)
    p.sparkle(18, 28, 5)
    p.sparkle(82, 40, 4)
    return p.out()


def build():
    icons = {}
    for i in range(1, 11):
        icons["piece.garden_%d" % i] = globals()["garden_%d" % i]()
        icons["piece.sky_%d" % i] = globals()["sky_%d" % i]()
    return {"icons": icons}
