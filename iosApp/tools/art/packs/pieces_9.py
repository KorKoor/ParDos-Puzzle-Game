import os
import sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from _pk import *  # noqa: F401,F403
import _animals

WH = ["#FFFFFF", "#C9D3E8"]
SKIN = ["#FFD3A8", "#E8A06B"]


# ------------------------------------------------------------------ Templo zen
def temple_1():  # arbol de deseos
    p = Pc()
    p.path("M 46 92 Q 44 60 50 12 Q 56 60 54 92 Z", ["#8AC87A", "#3E8A5A"])
    for a, c in ((-60, "#6FBF73"), (-30, "#4E9A5A"), (30, "#6FBF73"), (60, "#4E9A5A")):
        p.leaf(50, 30 + abs(a) * 0.3, 34, 7, a - 90, c)
    for x, y, c in ((26, 50, "#FF8AB8"), (70, 44, "#FFE08A"), (30, 70, "#8AD0F5"), (72, 66, "#FFB86B"), (50, 58, "#B49CF5")):
        p.line(x + (50 - x) * 0.8, y - 10, x, y - 4, "#8A5A2B", 1.2)
        p.rect(x - 5, y - 4, 10, 22, c, r=2, sw=1)
        p.line(x - 2, y + 2, x - 2, y + 12, "#FFFFFF", 1, ) if False else None
    p.sparkle(84, 20, 5, "#FFE08A")
    return p.out()


def temple_2():  # bambu
    p = Pc()
    for x, h, c in ((26, 78, ["#9ADD92", "#3E8A5A"]), (50, 86, ["#B8F0A0", "#4E9A2B"]), (74, 74, ["#9ADD92", "#3E8A5A"])):
        p.rect(x - 7, 92 - h, 14, h, c, r=4)
        for y in range(92 - h + 16, 90, 18):
            p.rect(x - 8, y, 16, 4, [lt(c[1], .2), c[1]], r=2, sw=1)
    p.leaf(33, 38, 24, 5, -20, "#6FBF73")
    p.leaf(57, 26, 24, 5, -160, "#6FBF73")
    p.leaf(81, 42, 20, 5, -30, "#6FBF73")
    return p.out()


def temple_3():  # campanilla de viento
    p = Pc()
    p.line(50, 6, 50, 24, "#C9902B", 2.4)
    p.path("M 24 36 C 24 18 76 18 76 36 Z", ["#8AD0F5", "#3F7FD0"])
    p.rect(22, 34, 56, 6, ["#C9902B", "#8A5A2B"], r=2)
    for x, h in ((34, 38), (50, 46), (66, 34)):
        p.line(x, 40, x, 40 + h - 12, "#C9D3E8", 1.6)
        p.rect(x - 3, 28 + h, 6, 18, ["#E8F8FF", "#8AD0F5"], r=2, sw=1)
    p.line(50, 40, 50, 66, "#C9D3E8", 1.4)
    p.path("M 40 68 H 60 L 56 90 L 44 90 Z", ["#FF8AB8", "#D6336C"])
    p.sparkle(82, 40, 5)
    return p.out()


def temple_4():  # munecas tradicionales
    p = Pc()
    for x, c, h in ((32, ["#FF8A7A", "#C8203B"], 70), (68, ["#8AB0F5", "#3F5FD0"], 64)):
        p.path("M %d 92 L %d 56 H %d L %d 92 Z" % (x - 14, x - 10, x + 10, x + 14), c)
        p.circ(x, 38, 16, ["#FFF4DC", "#F2D8B0"])
        p.path("M %d 38 Q %d 14 %d 20 Q %d 14 %d 38 Q %d 28 %d 30 Q %d 28 %d 38 Z" % (x - 17, x - 18, x, x + 18, x + 17, x + 8, x, x - 8, x - 17), ["#2B2B45", "#12121E"], ol=False)
        p.eyes(x - 6, x + 6, 40, 1.6)
        p.circ(x, 47, 1.8, "#C8203B", ol=False)
        p.line(x - 10, 62, x + 10, 62, "#FFE08A", 4)
    p.flower(50, 20, 8, 5, "#FFC3D6", "#FF8FB0")
    return p.out()


def temple_5():  # meditacion
    p = Pc()
    p.circ(50, 24, 14, SKIN)
    p.path("M 36 22 Q 38 6 50 8 Q 62 6 64 22 Q 56 14 50 16 Q 44 14 36 22 Z", ["#2B2B45", "#12121E"], ol=False)
    p.path("M 36 42 H 64 L 70 64 H 30 Z", ["#FF9A5A", "#C8501B"])
    p.path("M 18 78 C 14 66 28 62 40 70 H 60 C 72 62 86 66 82 78 C 76 88 24 88 18 78 Z", ["#FF9A5A", "#C8501B"])
    p.ell(30, 64, 9, 5, SKIN, rot=-20)
    p.ell(70, 64, 9, 5, SKIN, rot=20)
    p.eyes(45, 55, 24, 1.4)
    p.curve("M 46 30 Q 50 33 54 30", "#C8203B", 1.6)
    p.s.circle(50, 24, 26, stroke="#FFE08A", sw=1.6, op=.5)
    return p.out()


def temple_6():  # luna de otono
    p = Pc(shadow=False)
    p.circ(50, 38, 28, ["#FFF8C0", "#FFC83D"])
    for x, y, r in ((40, 30, 5), (58, 44, 6), (44, 50, 3)):
        p.circ(x, y, r, ["#FFE08A", "#E8B83B"], sw=0.8)
    for x, h in ((20, 34), (30, 44), (70, 40), (82, 32)):
        p.curve("M %d 92 Q %d %d %d %d" % (x, x + 2, 92 - h * 0.6, x + 8, 92 - h), "#C9902B", 2.4)
        p.ell(x + 8, 92 - h, 3, 8, ["#FFE88A", "#C9902B"], rot=30, sw=1)
    p.curve("M 4 90 H 96", "#8A7A5A", 2)
    return p.out()


def temple_7():  # Monte Fuji
    p = Pc()
    p.poly([(4, 86), (36, 40), (50, 24), (64, 40), (96, 86)], ["#8AB0F5", "#3F5FD0"])
    p.poly([(36, 40), (50, 24), (64, 40), (58, 38), (54, 46), (48, 38), (42, 46)], ["#FFFFFF", "#D3E4F8"])
    p.path("M 4 86 Q 50 74 96 86 V 92 H 4 Z", ["#7FD08A", "#2E7A4A"], ol=False)
    p.circ(80, 22, 10, ["#FF8A7A", "#C8203B"], ol=False)
    p.path("M 58 14 Q 70 8 80 16 Q 72 20 58 14 Z", "#FFFFFF", ol=False, op=.8)
    return p.out()


def temple_8():  # carpa koi dorada
    p = Pc(shadow=False)
    p.path("M 14 50 C 8 30 26 22 36 30 C 46 22 48 34 46 44 Z", ["#FFB86B", "#E8642B"])
    p.path("M 74 50 L 94 30 L 90 50 L 94 70 Z", ["#FFB86B", "#E8642B"])
    p.ell(46, 50, 34, 20, ["#FFE08A", "#E8A91F"])
    for x, y, r in ((36, 42, 7), (54, 56, 8), (60, 42, 5)):
        p.circ(x, y, r, ["#FF8A7A", "#C8203B"], ol=False)
    for k in range(4):
        p.s.path("M %d 40 Q %d 50 %d 60" % (24 + k * 8, 22 + k * 8, 24 + k * 8), stroke="#FFD36E", sw=1, op=.7)
    p.eyes(24, 24, 46, 3)
    p.curve("M 14 54 Q 20 58 26 56", "#C8501B", 1.8)
    p.s.path("M 30 66 Q 36 80 48 74", stroke="#FFB86B", sw=4, cap=CAP_ROUND)
    return p.out()


def temple_9():  # dojo (judogi)
    p = Pc()
    p.path("M 16 14 L 50 8 L 84 14 L 94 48 L 76 52 L 72 40 V 90 H 28 V 40 L 24 52 L 6 48 Z", ["#FFFFFF", "#C9D3E8"])
    p.path("M 40 8 L 50 40 L 60 8 Z", ["#E8EEF8", "#9AA8C8"], ol=False)
    p.line(40, 10, 50, 44, "#9AA8C8", 1.6)
    p.line(60, 10, 50, 44, "#9AA8C8", 1.6)
    p.rect(26, 56, 48, 9, ["#2B2B45", "#12121E"], r=2)
    p.poly([(50, 65), (44, 84), (52, 80)], ["#2B2B45", "#12121E"], sw=1)
    p.poly([(50, 65), (58, 84), (50, 80)], ["#2B2B45", "#12121E"], sw=1)
    return p.out()


def temple_10():  # naipes antiguos
    p = Pc()
    for a, x, c in ((-18, 34, ["#FFF4DC", "#E8C99A"]), (12, 62, ["#FFFFFF", "#D9E0F0"])):
        with p.s.rotate(a, x, 56):
            p.rect(x - 18, 14, 36, 66, c, r=5)
    with p.s.rotate(-18, 34, 56):
        p.flower(34, 40, 22, 5, "#FF8AB8", "#FFE08A")
        p.leaf(34, 62, 14, 4, -60, "#6FBF73")
    with p.s.rotate(12, 62, 56):
        p.circ(62, 34, 10, ["#FF8A7A", "#C8203B"])
        p.curve("M 50 56 Q 62 46 74 56", "#2E7A4A", 3)
        p.curve("M 50 64 Q 62 54 74 64", "#2E7A4A", 3)
        p.star(62, 70, 5, ["#FFE08A", "#C9902B"])
    return p.out()


# ------------------------------------------------------------------ Mascotas
def dog(p, c1, c2, ear, nose="#2B1B3A", spot=None):
    p.ell(24, 48, 11, 22, ear, rot=10)
    p.ell(76, 48, 11, 22, ear, rot=-10)
    p.circ(50, 50, 30, [c1, c2])
    if spot:
        p.ell(34, 36, 12, 10, spot, ol=False, rot=-20)
    p.ell(50, 64, 17, 13, ["#FFF4DC", "#E8D0A8"], ol=False)
    p.eyes(39, 61, 46, 3)
    p.ell(50, 58, 6, 4.4, nose, ol=False)
    p.curve("M 50 62 V 67 M 44 68 Q 50 74 56 68", "#4A2A1E", 1.8)
    p.ell(50, 74, 5, 3, ["#FF8AB8", "#E8586D"], ol=False)


def pets_1():  # perrito
    p = Pc()
    dog(p, "#E8B880", "#B8782B", ["#9A6A38", "#4A3220"])
    return p.out()


def pets_2():
    return _animals.head("CAT")


def pets_3():
    return _animals.head("HAMSTER")


def pets_4():
    return _animals.head("BUNNY")


def pets_5():
    return _animals.head("TURTLE")


def pets_6():  # pez dorado
    p = Pc(shadow=False)
    p.path("M 24 50 C 6 32 10 18 22 22 C 30 26 34 40 34 50 C 34 60 30 74 22 78 C 10 82 6 68 24 50 Z", ["#FFB86B", "#E8642B"])
    p.ell(58, 50, 32, 24, ["#FFB86B", "#E8642B"])
    p.path("M 48 28 Q 60 8 74 30 Z", ["#FF9A4A", "#C8501B"])
    p.path("M 50 72 Q 56 88 70 74 Z", ["#FF9A4A", "#C8501B"])
    p.circ(76, 46, 6, "#FFFFFF", sw=1)
    p.circ(77, 46, 3, "#2B1B3A", ol=False)
    p.curve("M 86 54 Q 90 56 88 60", "#C8501B", 1.8)
    for k in range(4):
        p.s.path("M %d 40 Q %d 50 %d 60" % (44 + k * 8, 41 + k * 8, 44 + k * 8), stroke="#FFD79A", sw=1.2, op=.7)
    p.circ(18, 20, 4, "#BFEFFF", ol=False, sw=0)
    p.circ(28, 12, 3, "#BFEFFF", ol=False, sw=0)
    return p.out()


def pets_7():  # caniche
    p = Pc()
    for x, y, r in ((26, 40, 14), (74, 40, 14), (50, 20, 16), (36, 26, 12), (64, 26, 12)):
        p.circ(x, y, r, ["#FFFFFF", "#D3DCEF"])
    p.ell(22, 66, 11, 18, ["#FFFFFF", "#D3DCEF"], rot=10)
    p.ell(78, 66, 11, 18, ["#FFFFFF", "#D3DCEF"], rot=-10)
    p.circ(50, 52, 24, ["#FFFFFF", "#D3DCEF"])
    p.ell(50, 66, 12, 10, ["#FFF4F8", "#F2D0DC"], ol=False)
    p.eyes(40, 60, 50, 2.6)
    p.ell(50, 62, 4.6, 3.4, "#2B1B3A", ol=False)
    p.curve("M 44 70 Q 50 75 56 70", "#4A2A1E", 1.8)
    p.circ(50, 12, 5, ["#FF8AB8", "#D6336C"])
    return p.out()


def pets_8():  # gato de la suerte
    c = _animals.head("CAT", "GOLD")
    return c


def pets_9():  # conejo blanco
    return _animals.head("BUNNY", "PLATINUM")


def pets_10():  # perro leal
    p = Pc()
    dog(p, "#FFFFFF", "#D9D0BC", ["#E89A4A", "#8A4A1B"], spot=["#E89A4A", "#B8501B"])
    p.sparkle(84, 22, 5, "#FFE08A")
    return p.out()


def build():
    icons = {}
    for pre, ser in (("temple", "temple"), ("pets", "pets")):
        for i in range(1, 11):
            icons["piece.%s_%d" % (ser, i)] = globals()["%s_%d" % (pre, i)]()
    return {"icons": icons}
