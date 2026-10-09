import os
import sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from _pk import *  # noqa: F401,F403
import _animals

WH = ["#FFFFFF", "#C9D3E8"]


# ------------------------------------------------------------------ Duleria
def candy_1():  # paleta
    p = Pc()
    p.line(50, 56, 50, 92, "#F2E8D0", 4)
    p.circ(50, 38, 30, ["#FF8AB8", "#D6336C"])
    p.s.path("M 50 38 m 0 -4 a 4 4 0 1 1 -2 0 m 0 0 a 9 9 0 1 0 8 4 a 15 15 0 1 1 -16 -6 a 22 22 0 1 0 22 8", stroke="#FFFFFF", sw=3.4, cap=CAP_ROUND)
    p.shine(36, 24, 5, 2.6, -40, .6)
    return p.out()


def candy_2():  # chocolate
    p = Pc()
    p.rect(22, 10, 56, 80, ["#9A6A4A", "#4A2A1E"], r=5)
    for r in range(4):
        for c in range(3):
            p.rect(27 + c * 17, 16 + r * 18, 14, 15, ["#B88A68", "#6A4630"], r=2, sw=1)
    p.rect(22, 10, 56, 28, ["#E8586D", "#B8203B"], r=5)
    p.rect(28, 22, 44, 8, "#FFE08A", r=2, ol=False)
    return p.out()


def candy_3():  # caramelo
    p = Pc()
    p.path("M 6 50 L 24 36 V 64 Z", ["#FF8AB8", "#D6336C"])
    p.path("M 94 50 L 76 36 V 64 Z", ["#FF8AB8", "#D6336C"])
    p.ell(50, 50, 28, 20, ["#FFB8D0", "#E8586D"])
    p.s.path("M 34 36 Q 44 50 34 64", stroke="#FFFFFF", sw=3, op=.8)
    p.s.path("M 50 32 Q 60 50 50 68", stroke="#FFFFFF", sw=3, op=.8)
    p.shine(40, 40, 6, 2.6, -30, .6)
    return p.out()


def candy_4():  # dona
    p = Pc()
    p.circ(50, 52, 38, ["#F2B866", "#B8782B"])
    p.circ(50, 50, 36, ["#FF8AB8", "#D6336C"], ol=False)
    p.path("M 14 52 Q 20 38 30 46 Q 36 36 46 44 Q 54 34 64 44 Q 74 36 80 46 Q 90 42 86 54 Q 84 66 76 62 Q 68 72 58 64 Q 46 72 38 64 Q 26 70 20 62 Q 12 64 14 52 Z", ["#FFB8D0", "#E8586D"], ol=False)
    p.circ(50, 50, 11, "#F3EFE6", ol=True)
    for x, y, c in ((30, 34, "#FFE08A"), (66, 30, "#8AD0F5"), (74, 58, "#FFFFFF"), (30, 66, "#8AF0B0"), (50, 76, "#FFE08A"), (44, 28, "#FFFFFF")):
        p.rect(x - 3, y - 1, 6, 2.6, c, r=1, ol=False)
    return p.out()


def candy_5():  # cupcake
    p = Pc()
    p.path("M 22 52 L 28 90 H 72 L 78 52 Z", ["#F2B866", "#B8782B"])
    for x in (36, 46, 56, 66):
        p.line(x, 56, x - 2, 88, "#B8782B", 1.2)
    p.ell(50, 48, 30, 12, ["#FFB8D0", "#E8586D"])
    p.ell(50, 36, 22, 11, ["#FFE8F0", "#F4A8C4"])
    p.ell(50, 24, 14, 9, ["#FFFFFF", "#FAD0E0"])
    p.circ(50, 14, 5, "#E8586D")
    return p.out()


def candy_6():  # flan
    p = Pc()
    p.path("M 14 62 Q 50 38 86 62 L 78 88 H 22 Z", ["#FFE88A", "#E8A91F"])
    p.path("M 14 62 Q 50 38 86 62 Q 86 70 70 70 Q 50 62 30 70 Q 14 70 14 62 Z", ["#C9702B", "#7A3A1B"], ol=False)
    p.path("M 28 66 Q 26 80 32 84", ["#C9702B", "#7A3A1B"], ol=False)
    p.circ(50, 34, 8, ["#FF8A9A", "#D6336C"])
    p.leaf(50, 28, 10, 3, -70, "#4E9A5A")
    p.shine(34, 70, 3, 9, 10, .5)
    return p.out()


def candy_7():  # pastel de cumple
    p = Pc()
    p.rect(14, 54, 72, 34, ["#FFE8F0", "#F4A8C4"], r=5)
    p.rect(22, 34, 56, 24, ["#FFFFFF", "#E8D0E0"], r=5)
    p.path("M 14 58 Q 20 70 28 58 Q 36 72 44 58 Q 52 72 60 58 Q 68 72 76 58 Q 80 66 86 58 V 54 H 14 Z", ["#FFFFFF", "#E8D0E0"], ol=False)
    for x, c in ((34, "#FF8A9A"), (50, "#8AD0F5"), (66, "#FFE08A")):
        p.rect(x - 2.5, 18, 5, 18, c, r=1.5, sw=1)
        p.path("M %f 8 C %f 14 %f 18 %f 18 C %f 18 %f 14 %f 8 Z" % (x, x + 4, x + 3, x, x, x - 3, x - 4), ["#FFE86B", "#E8502B"], sw=0.8)
    return p.out()


def candy_8():  # gran helado
    p = Pc()
    p.path("M 22 46 H 78 L 68 86 Q 50 92 32 86 Z", ["#E8F4FF", "#B8D0F0"])
    for x, y, c in ((38, 36, ["#FFB8D0", "#E8586D"]), (54, 32, ["#FFE88A", "#E8A91F"]), (68, 38, ["#C9F0A8", "#6FBF73"])):
        p.circ(x, y, 14, c)
    p.circ(46, 20, 8, ["#FFFFFF", "#E8D0E0"])
    p.circ(46, 10, 5, "#E8586D")
    p.line(70, 12, 84, 4, "#FF8A7A", 3)
    p.rect(20, 46, 60, 7, ["#FFFFFF", "#C9D3E8"], r=3)
    return p.out()


def candy_9():  # pay de manzana
    p = Pc()
    p.path("M 10 62 Q 50 28 90 62 L 82 84 H 18 Z", ["#F2B866", "#B8782B"])
    for k in range(4):
        p.line(26 + k * 16, 48 + (0 if k in (1, 2) else 6), 30 + k * 14, 66, "#E8D08A", 3)
        p.line(18 + k * 18, 60, 36 + k * 16, 46, "#E8D08A", 3)
    p.path("M 8 62 H 92 Q 92 72 84 72 H 16 Q 8 72 8 62 Z", ["#F5D79A", "#C9955E"])
    p.circ(50, 44, 3, "#C9955E", ol=False)
    p.circ(50, 34, 0.1, "#FFFFFF", ol=False)
    return p.out()


def candy_10():  # palomitas
    p = Pc()
    for x, y, r in ((36, 30, 11), (52, 22, 12), (66, 32, 11), (28, 44, 10), (74, 46, 10), (50, 40, 11)):
        p.circ(x, y, r, ["#FFFFFF", "#F2E2B0"])
    p.path("M 22 50 H 78 L 72 90 H 28 Z", ["#FF8A9A", "#D6336C"])
    for k in range(3):
        p.line(34 + k * 16, 56, 36 + k * 14, 86, "#FFFFFF", 4)
    return p.out()


# ------------------------------------------------------------------ Huerto
def fruit_1():  # manzana
    p = Pc()
    p.path("M 50 28 C 70 14 94 30 90 56 C 86 80 66 92 50 86 C 34 92 14 80 10 56 C 6 30 30 14 50 28 Z", ["#FF8A8A", "#C8203B"])
    p.line(50, 28, 52, 12, "#6A4630", 3)
    p.leaf(52, 18, 24, 7, -20, "#6FBF73")
    p.shine(30, 44, 5, 10, 25, .6)
    return p.out()


def fruit_2():  # platano
    p = Pc()
    p.path("M 22 20 C 12 56 30 88 74 84 C 88 82 94 68 90 62 C 70 74 44 66 36 44 C 32 34 34 24 30 18 Z", ["#FFE88A", "#E8A91F"])
    p.path("M 22 20 L 30 18 L 32 8 L 22 10 Z", ["#8A6A3B", "#4A3A22"])
    p.curve("M 34 40 C 44 62 62 70 84 70", "#C9902B", 1.6)
    p.shine(28, 40, 3, 12, 15, .5)
    return p.out()


def fruit_3():  # uvas
    p = Pc()
    cs = ["#C98AF5", "#7A2FD0"]
    pos = [(30, 38), (50, 38), (70, 38), (40, 54), (60, 54), (30, 54), (70, 54), (50, 70), (40, 86 - 8), (60, 70)]
    for x, y in pos:
        p.circ(x, y, 10, cs, sw=1.2)
        p.shine(x - 3, y - 3, 2.6, 1.4, -30, .55)
    p.line(50, 28, 52, 12, "#6A4630", 3)
    p.leaf(52, 20, 26, 9, -15, "#6FBF73")
    return p.out()


def fruit_4():  # fresa
    p = Pc()
    p.path("M 50 90 C 20 72 10 44 22 30 C 36 20 64 20 78 30 C 90 44 80 72 50 90 Z", ["#FF8A8A", "#C8203B"])
    for x, y in ((34, 40), (50, 36), (66, 40), (40, 54), (60, 54), (50, 68), (30, 56), (70, 56)):
        p.ell(x, y, 2, 3.2, "#FFE88A", ol=False)
    p.path("M 50 22 L 36 10 L 44 24 L 30 22 L 42 30 L 50 34 L 58 30 L 70 22 L 56 24 L 64 10 Z", ["#8EE08A", "#3E9A5A"])
    p.shine(30, 46, 3.6, 8, 20, .5)
    return p.out()


def fruit_5():  # durazno
    p = Pc()
    p.path("M 50 24 C 70 10 94 28 90 56 C 86 80 66 92 50 88 C 34 92 14 80 10 56 C 6 28 30 10 50 24 Z", ["#FFC0A0", "#F2704A"])
    p.curve("M 50 24 C 54 48 54 68 50 88", "#E8502B", 2, op=.5)
    p.leaf(50, 22, 26, 8, -25, "#6FBF73")
    p.shine(30, 44, 5, 10, 25, .55)
    return p.out()


def fruit_6():  # pina
    p = Pc()
    for a, c in ((-45, "#6FBF73"), (-15, "#4E9A5A"), (15, "#6FBF73"), (45, "#4E9A5A"), (0, "#7FD08A")):
        p.leaf(50, 34, 28, 6, a - 90, c)
    p.ell(50, 64, 26, 30, ["#FFE88A", "#E8A91F"])
    for yy in range(46, 90, 11):
        for xx in range(36, 68, 11):
            p.dot(xx + (5 if (yy // 11) % 2 else 0), yy, 2.2, "#C9902B")
    p.shine(38, 54, 3, 10, 10, .5)
    return p.out()


def fruit_7():  # sandia
    p = Pc()
    p.path("M 6 36 H 94 C 94 74 74 92 50 92 C 26 92 6 74 6 36 Z", ["#6FCF7A", "#2E7A4A"])
    p.path("M 12 36 H 88 C 88 70 70 84 50 84 C 30 84 12 70 12 36 Z", ["#FFFFFF", "#E8F4E0"], ol=False)
    p.path("M 17 36 H 83 C 83 66 66 80 50 80 C 34 80 17 66 17 36 Z", ["#FF8A9A", "#D6203B"], ol=False)
    for x, y in ((34, 48), (50, 58), (66, 48), (42, 68), (58, 70)):
        p.ell(x, y, 2.4, 4, "#2B1B3A", ol=False, rot=15)
    return p.out()


def fruit_8():  # manzana verde / dorada
    p = Pc()
    p.path("M 50 28 C 70 14 94 30 90 56 C 86 80 66 92 50 86 C 34 92 14 80 10 56 C 6 30 30 14 50 28 Z", ["#D8F58A", "#6FAF2B"])
    p.line(50, 28, 52, 12, "#6A4630", 3)
    p.leaf(52, 18, 24, 7, -20, "#4E9A5A")
    p.shine(30, 44, 5, 10, 25, .6)
    p.sparkle(78, 30, 6, "#FFFFFF")
    return p.out()


def fruit_9():  # kiwi
    p = Pc()
    p.circ(50, 52, 38, ["#B88A5E", "#6A4630"])
    p.circ(50, 52, 33, ["#B8F070", "#4E9A2B"], ol=False)
    p.circ(50, 52, 12, ["#FFF8D0", "#E8E0A0"], ol=False)
    for a in range(0, 360, 20):
        p.line(50 + math.cos(math.radians(a)) * 15, 52 + math.sin(math.radians(a)) * 15, 50 + math.cos(math.radians(a)) * 27, 52 + math.sin(math.radians(a)) * 27, "#E8F8B0", 1.2)
        p.dot(50 + math.cos(math.radians(a)) * 18, 52 + math.sin(math.radians(a)) * 18, 1.5, "#2B1B3A")
    return p.out()


def fruit_10():  # aguacate
    p = Pc()
    p.path("M 50 8 C 70 8 72 34 82 50 C 96 76 78 94 50 94 C 22 94 4 76 18 50 C 28 34 30 8 50 8 Z", ["#6FBF73", "#2E5A3A"])
    p.path("M 50 16 C 66 16 66 38 76 52 C 88 74 72 88 50 88 C 28 88 12 74 24 52 C 34 38 34 16 50 16 Z", ["#E8F8B0", "#B8D86A"], ol=False)
    p.circ(50, 62, 13, ["#C98A55", "#7A4A2B"])
    p.shine(44, 56, 4, 2.6, -30, .5)
    return p.out()


# ------------------------------------------------------------------ Orquesta
def orch_1():  # guitarra
    p = Pc()
    with p.s.rotate(-35, 50, 50):
        p.rect(46, 6, 8, 46, ["#8A5A2B", "#4A3220"], r=2)
        p.rect(42, 2, 16, 10, ["#4A3220", "#241810"], r=2)
        p.path("M 50 44 C 74 44 78 62 68 70 C 80 78 76 94 50 94 C 24 94 20 78 32 70 C 22 62 26 44 50 44 Z", ["#F2B866", "#B8782B"])
        p.circ(50, 70, 8, ["#4A2A1E", "#1E1010"])
        p.rect(40, 84, 20, 4, ["#4A3220", "#241810"], r=1.5)
        for x in (47, 50, 53):
            p.line(x, 12, x, 84, "#F5F5FF", 0.9)
    return p.out()


def orch_2():  # tambor
    p = Pc()
    p.ell(50, 42, 38, 12, ["#FFFFFF", "#D3DCEF"])
    p.path("M 12 42 V 70 C 12 80 88 80 88 70 V 42 C 88 52 12 52 12 42 Z", ["#FF8A7A", "#C83A2B"])
    for k in range(6):
        p.line(14 + k * 14.4, 50, 24 + k * 14.4, 76, "#FFE08A", 2)
        p.line(24 + k * 14.4, 50, 14 + k * 14.4, 76, "#FFE08A", 2)
    p.line(20, 6, 52, 40, "#E8C27A", 3.4)
    p.line(80, 6, 48, 40, "#E8C27A", 3.4)
    p.circ(20, 6, 4, "#E8C27A")
    p.circ(80, 6, 4, "#E8C27A")
    return p.out()


def orch_3():  # trompeta
    p = Pc()
    p.path("M 6 42 H 52 C 66 42 74 30 84 30 V 22 L 96 14 V 66 L 84 58 V 50 C 74 50 66 58 52 58 H 6 Z", ["#FFE08A", "#C9902B"])
    p.s.path("M 24 42 V 30 C 24 22 40 22 40 30 V 42", stroke="#C9902B", sw=3.4)
    for x in (32, 44, 56):
        p.rect(x - 3, 52, 6, 14, ["#FFF1A8", "#C9902B"], r=2, sw=1)
    p.shine(40, 46, 10, 2.4, 0, .6)
    return p.out()


def orch_4():  # violin
    p = Pc()
    with p.s.rotate(25, 50, 50):
        p.rect(46, 4, 8, 36, ["#4A3220", "#241810"], r=2)
        p.path("M 50 36 C 70 34 70 52 62 56 C 76 60 76 90 50 92 C 24 90 24 60 38 56 C 30 52 30 34 50 36 Z", ["#C9702B", "#7A3A1B"])
        p.rect(44, 62, 12, 4, ["#4A3220", "#241810"], r=1.5, ol=False)
        p.line(40, 66, 46, 66, "#2B1B3A", 1.6)
        p.line(54, 66, 60, 66, "#2B1B3A", 1.6)
    p.line(14, 18, 86, 74, "#E8C27A", 2.4)
    return p.out()


def orch_5():  # saxofon
    p = Pc()
    p.path("M 62 6 H 74 V 56 C 74 82 60 94 42 94 C 24 94 14 84 14 70 C 14 62 20 58 28 58 C 36 58 36 70 42 70 C 50 70 62 66 62 52 Z", ["#FFE08A", "#C9902B"])
    p.path("M 14 70 C 14 84 24 94 42 94 C 24 90 20 80 24 70 Z", ["#FFF1A8", "#E8A91F"], ol=False)
    p.rect(58, 2, 22, 8, ["#8A5A2B", "#4A3220"], r=3)
    for y in (22, 34, 46):
        p.circ(68, y, 3.6, "#C9902B", ol=False)
    return p.out()


def orch_6():  # audifonos
    p = Pc()
    p.s.path("M 16 56 C 16 8 84 8 84 56", stroke=lin(0, 10, 0, 56, [(0, "#B49CF5"), (1, "#6A4FD0")]), sw=8, cap=CAP_ROUND)
    p.rect(8, 52, 24, 34, ["#8A6FE0", "#3A1E8A"], r=9)
    p.rect(68, 52, 24, 34, ["#8A6FE0", "#3A1E8A"], r=9)
    p.rect(12, 58, 12, 22, ["#FFE08A", "#C9902B"], r=5, ol=False)
    p.rect(76, 58, 12, 22, ["#FFE08A", "#C9902B"], r=5, ol=False)
    return p.out()


def orch_7():  # partitura
    p = Pc()
    p.rect(12, 12, 76, 76, ["#FFFFFF", "#E8E0D0"], r=5)
    for y in (30, 40, 50, 60, 70):
        p.line(20, y, 80, y, "#8A7A8A", 1.2)
    p.ell(36, 62, 6, 4.4, "#2B1B3A", ol=False, rot=-20)
    p.line(42, 62, 42, 36, "#2B1B3A", 1.8)
    p.ell(60, 52, 6, 4.4, "#2B1B3A", ol=False, rot=-20)
    p.line(66, 52, 66, 26, "#2B1B3A", 1.8)
    p.line(42, 36, 66, 26, "#2B1B3A", 3)
    return p.out()


def orch_8():  # disco de oro
    p = Pc()
    p.circ(50, 50, 40, ["#FFE08A", "#C9902B"])
    for r in (32, 24, 16):
        p.s.circle(50, 50, r, stroke="#C9902B", sw=1, op=.6)
    p.circ(50, 50, 12, ["#FFFFFF", "#F2E2B0"])
    p.circ(50, 50, 3, "#C9902B", ol=False)
    p.s.path("M 20 36 A 34 34 0 0 1 50 16", stroke="#FFFFFF", sw=4, cap=CAP_ROUND, op=.7)
    p.sparkle(82, 18, 6)
    return p.out()


def orch_9():  # radio antigua
    p = Pc()
    p.rect(8, 28, 84, 58, ["#C9955E", "#7A4A2B"], r=8)
    p.circ(32, 58, 18, ["#4A3220", "#241810"])
    for k in range(3):
        p.s.circle(32, 58, 5 + k * 5, stroke="#8A6A4A", sw=1)
    p.rect(58, 40, 28, 14, ["#FFF4DC", "#E8C99A"], r=3)
    p.line(62, 47, 82, 47, "#C83A2B", 2)
    p.circ(66, 70, 6, ["#FFE08A", "#C9902B"])
    p.circ(82, 70, 6, ["#FFE08A", "#C9902B"])
    p.line(70, 28, 86, 8, "#8A8AA8", 2.4)
    return p.out()


def orch_10():  # coro celestial
    p = Pc(shadow=False)
    for x, y, r, c in ((34, 60, 1.0, ["#FFE08A", "#C9902B"]), (66, 44, 1.0, ["#8AD0F5", "#3F7FD0"])):
        p.path("M %f %f L %f %f V %f" % (x + 10, y - 40, x + 10, y, y + 2), c)
        p.ell(x, y, 12, 8.4, c, rot=-20)
        p.line(x + 10, y - 2, x + 10, y - 38, dk(c[1], .1), 3)
    p.line(44, 22, 76, 6, "#C9902B", 5)
    p.sparkle(20, 24, 6)
    p.sparkle(84, 74, 5)
    return p.out()


# ------------------------------------------------------------------ Deportes
def ball(p, cx, cy, r, c1, c2):
    p.circ(cx, cy, r, [c1, c2])


def sports_1():  # balon
    p = Pc()
    p.circ(50, 50, 40, ["#FFFFFF", "#C9D3E8"])
    p.poly([(50, 34), (64, 44), (58, 62), (42, 62), (36, 44)], ["#4A4A68", "#1E1E32"], sw=1)
    for a in range(0, 360, 72):
        x, y = 50 + math.cos(math.radians(a - 90)) * 40, 50 + math.sin(math.radians(a - 90)) * 40
        p.line(50 + math.cos(math.radians(a - 90)) * 22, 50 + math.sin(math.radians(a - 90)) * 22, x, y, "#4A4A68", 1.8)
    p.shine(34, 30, 6, 3, -35, .55)
    return p.out()


def sports_2():  # baloncesto
    p = Pc()
    p.circ(50, 50, 40, ["#FFB86B", "#E8642B"])
    p.s.circle(50, 50, 40, stroke="#7A3A1B", sw=1.5)
    p.line(50, 10, 50, 90, "#7A3A1B", 2)
    p.line(10, 50, 90, 50, "#7A3A1B", 2)
    p.s.path("M 20 18 C 38 34 38 66 20 82", stroke="#7A3A1B", sw=2)
    p.s.path("M 80 18 C 62 34 62 66 80 82", stroke="#7A3A1B", sw=2)
    p.shine(34, 30, 6, 3, -35, .45)
    return p.out()


def sports_3():  # tenis
    p = Pc()
    with p.s.rotate(-35, 50, 50):
        p.ell(50, 34, 24, 30, ["#7FD0F5", "#2B7AC0"], sw=3)
        p.ell(50, 34, 18, 24, "#FFFFFF", ol=False)
        p.s.ellipse(50, 34, 18, 24, fill="#FFFFFF00")
        for k in range(-3, 4):
            p.line(50 + k * 5, 14, 50 + k * 5, 56, "#C9D3E8", 1)
            p.line(34, 34 + k * 6, 66, 34 + k * 6, "#C9D3E8", 1)
        p.rect(46, 60, 8, 32, ["#E8586D", "#B8203B"], r=3)
    p.circ(78, 74, 12, ["#E8F870", "#9AC82B"])
    p.s.path("M 70 68 Q 78 74 70 80", stroke="#FFFFFF", sw=1.6)
    return p.out()


def sports_4():  # beisbol
    p = Pc()
    p.circ(50, 50, 40, ["#FFFFFF", "#D3DCEF"])
    p.s.path("M 24 22 C 40 34 40 66 24 78", stroke="#E8586D", sw=2.2)
    p.s.path("M 76 22 C 60 34 60 66 76 78", stroke="#E8586D", sw=2.2)
    for y in (32, 42, 58, 68):
        p.line(24 + (6 if y in (32, 68) else 10), y, 24 + (14 if y in (32, 68) else 18), y + 1, "#E8586D", 2)
        p.line(76 - (6 if y in (32, 68) else 10), y, 76 - (14 if y in (32, 68) else 18), y + 1, "#E8586D", 2)
    p.shine(34, 28, 6, 3, -35, .55)
    return p.out()


def sports_5():  # futbol americano
    p = Pc()
    with p.s.rotate(-30, 50, 50):
        p.ell(50, 50, 44, 26, ["#C98A55", "#7A4A2B"])
        p.line(30, 50, 70, 50, "#FFFFFF", 2.6)
        for x in (38, 46, 54, 62):
            p.line(x, 44, x, 56, "#FFFFFF", 2)
        p.s.path("M 14 42 Q 16 50 14 58", stroke="#FFFFFF", sw=2.4)
        p.s.path("M 86 42 Q 84 50 86 58", stroke="#FFFFFF", sw=2.4)
    return p.out()


def sports_6():  # voleibol
    p = Pc()
    p.circ(50, 50, 40, ["#FFF8E0", "#E8D8A0"])
    p.s.path("M 14 36 C 40 30 60 44 86 66", stroke="#4F8FE0", sw=8, cap=CAP_BUTT)
    p.s.path("M 20 74 C 40 54 56 20 60 10", stroke="#FFD84A", sw=8)
    p.s.path("M 90 40 C 70 44 46 40 26 14", stroke="#4F8FE0", sw=8)
    p.s.circle(50, 50, 40, stroke="#9A8A5A", sw=1.5)
    p.shine(34, 30, 6, 3, -35, .55)
    return p.out()


def sports_7():  # natacion
    p = Pc()
    p.path("M 4 62 Q 20 50 36 62 T 68 62 T 98 60 V 92 H 4 Z", ["#7FD0F5", "#2B7AC0"])
    p.circ(46, 40, 13, ["#FFD3A8", "#E8A06B"])
    p.path("M 33 40 A 13 13 0 0 1 59 40 Z", ["#FF8A7A", "#C83A2B"])
    p.rect(36, 36, 22, 7, ["#8AD0F5", "#3F7FD0"], r=3)
    p.path("M 28 60 Q 46 50 64 60", ["#FFD3A8", "#E8A06B"], ol=False)
    p.curve("M 60 56 Q 76 48 88 58", "#FFD3A8", 6)
    p.curve("M 10 78 Q 20 72 30 78 T 50 78", "#FFFFFF", 2.4, op=.8)
    return p.out()


def sports_8():  # medalla de oro 1er lugar
    p = Pc()
    p.poly([(30, 4), (48, 4), (58, 40), (42, 40)], ["#E8586D", "#B8203B"], sw=1.2)
    p.poly([(70, 4), (52, 4), (42, 40), (58, 40)], ["#FFFFFF", "#C9D3E8"], sw=1.2)
    p.circ(50, 64, 28, ["#FFE88A", "#C9902B"])
    p.circ(50, 64, 21, ["#FFD36E", "#E8A91F"], sw=1.1)
    p.rect(46, 50, 8, 26, ["#8A5A2B", "#4A3220"], r=2, ol=False)
    p.poly([(46, 50), (54, 50), (46, 56)], "#8A5A2B", ol=False)
    p.sparkle(78, 38, 5)
    return p.out()


def sports_9():  # guante de boxeo
    p = Pc()
    p.path("M 22 50 C 12 22 36 10 56 12 C 80 12 92 32 86 54 C 84 62 78 66 74 66 V 90 H 38 V 70 C 28 66 24 58 22 50 Z", ["#FF8A9A", "#C8203B"])
    p.rect(36, 72, 40, 16, ["#FFFFFF", "#D3DCEF"], r=4)
    p.s.path("M 46 36 C 54 30 74 32 78 46", stroke="#E8A0B0", sw=2.4, cap=CAP_ROUND)
    p.shine(44, 24, 8, 3, -20, .55)
    return p.out()


def sports_10():  # ciclismo
    p = Pc()
    p.s.circle(26, 66, 20, stroke="#4A4A68", sw=4)
    p.s.circle(74, 66, 20, stroke="#4A4A68", sw=4)
    p.s.path("M 26 66 L 42 36 H 66 L 74 66 M 42 36 L 52 66 H 26 M 52 66 L 66 36", stroke="#E8586D", sw=4, join=JOIN_ROUND, cap=CAP_ROUND)
    p.line(38, 30, 48, 30, "#4A4A68", 4)
    p.line(66, 36, 70, 24, "#4A4A68", 3.4)
    p.circ(26, 66, 3, "#FFE08A")
    p.circ(74, 66, 3, "#FFE08A")
    p.path("M 40 18 A 12 10 0 0 1 64 18 Z", ["#8AD0F5", "#2B7AC0"])
    return p.out()


def build():
    icons = {}
    for pre, ser in (("candy", "candy"), ("fruit", "fruit"), ("orch", "orchestra"), ("sports", "sports")):
        for i in range(1, 11):
            icons["piece.%s_%d" % (ser, i)] = globals()["%s_%d" % (pre, i)]()
    return {"icons": icons}
