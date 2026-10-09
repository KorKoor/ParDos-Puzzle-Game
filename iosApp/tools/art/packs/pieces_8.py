import os
import sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from _pk import *  # noqa: F401,F403
import _animals

WH = ["#FFFFFF", "#C9D3E8"]
SKIN = ["#FFD3A8", "#E8A06B"]


# ------------------------------------------------------------------ Criaturas miticas
def myth_1():  # hongo magico
    p = Pc()
    p.path("M 38 92 C 36 74 38 62 40 54 H 60 C 62 62 64 74 62 92 Z", ["#FFF4DC", "#E8C99A"])
    p.path("M 8 56 C 8 26 30 8 50 8 C 70 8 92 26 92 56 C 80 62 20 62 8 56 Z", ["#FF8A7A", "#C8203B"])
    for x, y, r in ((30, 36, 7), (54, 24, 6), (68, 44, 8), (44, 48, 5)):
        p.circ(x, y, r, "#FFFFFF", sw=1)
    p.sparkle(84, 18, 6, "#FFE08A")
    p.eyes(46, 54, 74, 1.8)
    return p.out()


def myth_2():  # tridente
    p = Pc()
    p.line(50, 24, 50, 94, "#C9902B", 5)
    p.s.path("M 24 14 V 36 C 24 48 76 48 76 36 V 14", stroke="#FFE08A", sw=5, cap=CAP_ROUND, join=JOIN_ROUND)
    for x in (24, 50, 76):
        p.poly([(x - 6, 20), (x, 4), (x + 6, 20)], ["#FFE88A", "#C9902B"], sw=1.2)
    p.rect(44, 50, 12, 8, ["#8AD0F5", "#3F7FD0"], r=3)
    p.sparkle(80, 60, 6, "#8AD0F5")
    return p.out()


def myth_3():  # espadas cruzadas
    p = Pc()
    for a in (45, -45):
        with p.s.rotate(a, 50, 50):
            p.path("M 50 2 L 57 12 V 60 H 43 V 12 Z", ["#F2F6FF", "#8A9AC8"])
            p.rect(32, 60, 36, 7, ["#FFE08A", "#C9902B"], r=3)
            p.rect(45, 67, 10, 16, ["#8A5A2B", "#4A3220"], r=3)
            p.circ(50, 88, 4.4, ["#E8586D", "#B8203B"])
    p.circ(50, 50, 6, ["#FFE08A", "#C9902B"])
    return p.out()


def myth_4():  # polvo estelar
    p = Pc(shadow=False)
    for x, y, r, c in ((50, 50, 26, "#FFF1A8"), (24, 26, 12, "#B49CF5"), (78, 28, 14, "#8AD0F5"), (26, 76, 13, "#FF8AB8"), (76, 74, 12, "#FFE88A")):
        p.sparkle(x, y, r, c, 1.0)
    for x, y in ((12, 52), (88, 52), (50, 10), (50, 90), (40, 28), (62, 70)):
        p.dot(x, y, 2.2, "#FFFFFF")
    return p.out()


def myth_5():  # sirena
    p = Pc()
    p.path("M 36 60 C 24 74 34 84 14 86 C 28 92 44 90 50 78 C 56 90 72 92 86 86 C 66 84 76 74 64 60 Z", ["#8AF0D0", "#1E8F8A"])
    p.circ(50, 40, 16, SKIN)
    p.path("M 30 44 Q 24 18 50 20 Q 76 18 70 44 Q 66 32 50 32 Q 34 32 30 44 Z", ["#FF8AB8", "#C8203B"], ol=False)
    p.path("M 30 44 Q 26 62 34 70 Q 32 56 36 46 Z", ["#FF8AB8", "#C8203B"], ol=False)
    p.path("M 70 44 Q 74 62 66 70 Q 68 56 64 46 Z", ["#FF8AB8", "#C8203B"], ol=False)
    p.eyes(44, 56, 42, 2.2)
    p.curve("M 46 50 Q 50 53 54 50", "#C8203B", 1.6)
    p.path("M 36 60 H 64 L 62 64 H 38 Z", ["#8AF0D0", "#1E8F8A"], ol=False)
    p.circ(40, 74, 2.4, "#FFFFFF", ol=False)
    p.circ(58, 72, 2, "#FFFFFF", ol=False)
    p.sparkle(82, 26, 5, "#8AF0D0")
    return p.out()


def myth_6():  # elfo
    p = Pc()
    p.path("M 50 4 Q 76 20 76 42 H 24 Q 26 20 50 4 Z", ["#6FCF7A", "#2E7A4A"])
    p.rect(20, 40, 60, 8, ["#8A5A2B", "#4A3220"], r=3)
    p.circ(50, 62, 20, SKIN)
    p.poly([(30, 62), (12, 52), (32, 70)], SKIN, sw=1.2)
    p.poly([(70, 62), (88, 52), (68, 70)], SKIN, sw=1.2)
    p.eyes(42, 58, 60, 2.6)
    p.curve("M 44 72 Q 50 77 56 72", "#C8203B", 2)
    p.circ(34, 68, 3.4, "#FF8AB8", ol=False, sw=0)
    p.circ(66, 68, 3.4, "#FF8AB8", ol=False, sw=0)
    p.star(50, 24, 6, ["#FFE08A", "#C9902B"])
    return p.out()


def myth_7():
    return _animals.head("UNICORN")


def myth_8():  # grifo real
    p = Pc()
    p.path("M 10 66 C 4 36 20 14 40 12 C 36 28 40 40 50 46 C 60 40 64 28 60 12 C 80 14 96 36 90 66 C 80 54 66 56 50 56 C 34 56 20 54 10 66 Z", ["#FFFFFF", "#9AA8C8"])
    p.path("M 14 56 C 6 40 14 24 28 20 C 24 34 28 46 36 52 Z", ["#F2F6FF", "#8A9AC8"], ol=False)
    p.circ(50, 62, 24, ["#F2C36B", "#B8782B"])
    for a in range(0, 360, 30):
        p.circ(50 + math.cos(math.radians(a)) * 26, 62 + math.sin(math.radians(a)) * 24, 6, ["#D98A33", "#8A4A16"], sw=1)
    p.circ(50, 62, 20, ["#F2C36B", "#B8782B"])
    p.eyes(42, 58, 58, 2.6)
    p.poly([(44, 66), (56, 66), (50, 76)], ["#FFB86B", "#E8642B"], sw=1)
    return p.out()


def myth_9():  # amuleto (ojo)
    p = Pc()
    p.circ(50, 52, 40, ["#6FA8F5", "#1E3A8A"])
    p.circ(50, 52, 31, ["#FFFFFF", "#D3DCEF"])
    p.circ(50, 52, 22, ["#6FA8F5", "#1E3A8A"])
    p.circ(50, 52, 12, ["#2B2B45", "#12121E"])
    p.circ(46, 48, 3.6, "#FFFFFF", ol=False)
    p.circ(50, 8, 4, ["#FFE08A", "#C9902B"])
    return p.out()


def myth_10():  # emblema ancestral (flor de lis)
    p = Pc()
    p.path("M 50 6 C 66 22 66 44 50 62 C 34 44 34 22 50 6 Z", ["#FFE88A", "#C9902B"])
    p.path("M 44 52 C 28 34 10 40 12 58 C 14 70 30 72 40 64 C 30 62 26 54 30 48 C 34 52 40 58 44 60 Z", ["#FFE88A", "#C9902B"])
    p.path("M 56 52 C 72 34 90 40 88 58 C 86 70 70 72 60 64 C 70 62 74 54 70 48 C 66 52 60 58 56 60 Z", ["#FFE88A", "#C9902B"])
    p.rect(30, 68, 40, 9, ["#FFE08A", "#C9902B"], r=3)
    p.path("M 40 77 L 36 92 L 50 86 L 64 92 L 60 77 Z", ["#FFE88A", "#C9902B"])
    p.circ(50, 72, 3, "#E8586D", ol=False)
    return p.out()


# ------------------------------------------------------------------ Cocina del mundo
def wfood_1():  # taco
    p = Pc()
    p.path("M 8 70 C 8 30 92 30 92 70 Z", ["#FFE08A", "#C9902B"])
    p.path("M 14 62 Q 22 36 38 40 Q 30 56 40 62 Z", ["#7FD08A", "#2E7A4A"], ol=False)
    p.path("M 36 60 Q 46 34 62 38 Q 54 54 66 62 Z", ["#FF8A7A", "#C8203B"], ol=False)
    p.path("M 60 60 Q 70 40 86 46 Q 78 56 86 64 Z", ["#FFF4A8", "#E8C94A"], ol=False)
    p.path("M 8 70 H 92", ["#E8C27A", "#B8782B"], ol=False)
    p.curve("M 10 68 C 20 60 80 60 90 68", "#C9902B", 3)
    return p.out()


def wfood_2():  # pizza
    p = Pc()
    p.path("M 50 92 L 8 22 Q 50 2 92 22 Z", ["#FFD38A", "#E8A04A"])
    p.path("M 14 28 Q 50 10 86 28", ["#E8A04A", "#B8782B"], ol=False)
    p.curve("M 10 26 Q 50 6 90 26", "#B8782B", 6)
    p.path("M 50 82 L 18 30 Q 50 16 82 30 Z", ["#FFB86B", "#F2704A"], ol=False)
    for x, y in ((38, 38), (60, 40), (50, 56), (46, 70)):
        p.circ(x, y, 6, ["#E8586D", "#8A1B2B"], sw=1)
    p.circ(56, 30, 3, "#6FBF73", ol=False)
    p.circ(38, 52, 3, "#6FBF73", ol=False)
    return p.out()


def wfood_3():  # hamburguesa
    p = Pc()
    p.path("M 10 40 C 10 10 90 10 90 40 Z", ["#F2B866", "#B8782B"])
    for x, y in ((32, 22), (50, 16), (66, 24)):
        p.ell(x, y, 3, 1.6, "#FFFFFF", ol=False)
    p.path("M 8 44 Q 20 54 32 44 Q 44 54 56 44 Q 68 54 80 44 Q 90 50 92 44 V 48 H 8 Z", ["#7FD08A", "#2E7A4A"])
    p.rect(10, 48, 80, 8, ["#FFE86B", "#E8A91F"], r=2)
    p.rect(8, 56, 84, 12, ["#8A5A3B", "#4A2A1E"], r=6)
    p.rect(10, 66, 80, 6, ["#FF8A7A", "#C8203B"], r=3)
    p.path("M 10 74 H 90 C 90 88 10 88 10 74 Z", ["#F2B866", "#B8782B"])
    return p.out()


def wfood_4():  # papas fritas
    p = Pc()
    for x, y, h, a in ((30, 36, 44, -8), (42, 28, 50, -3), (54, 24, 54, 2), (66, 30, 48, 8), (76, 38, 40, 12), (36, 40, 40, 4)):
        with p.s.rotate(a, x, y + h):
            p.rect(x - 4, y, 8, h, ["#FFE88A", "#E8A91F"], r=2, sw=1.1)
    p.path("M 18 56 H 82 L 74 92 H 26 Z", ["#FF8A7A", "#C8203B"])
    p.path("M 24 62 H 76 L 74 70 H 26 Z", ["#FFE08A", "#C9902B"], ol=False)
    return p.out()


def wfood_5():  # sushi
    p = Pc()
    p.rect(14, 42, 72, 36, ["#2E4A3A", "#12261E"], r=8)
    p.rect(22, 38, 56, 36, ["#FFFFFF", "#E8E0D0"], r=8)
    p.path("M 14 40 C 14 22 86 22 86 40 C 86 52 70 50 50 50 C 30 50 14 52 14 40 Z", ["#FF9A7A", "#E8502B"])
    p.curve("M 26 36 C 36 28 48 34 58 28", "#FFE0D0", 2.2, op=.8)
    p.curve("M 30 42 C 40 34 52 40 66 32", "#FFE0D0", 2.2, op=.8)
    p.ell(50, 82, 38, 5, ["#7FD08A", "#2E7A4A"], ol=False)
    return p.out()


def wfood_6():  # dumpling
    p = Pc()
    p.path("M 6 64 C 6 34 30 24 50 24 C 70 24 94 34 94 64 C 94 80 70 84 50 84 C 30 84 6 80 6 64 Z", ["#FFF4DC", "#E8C99A"])
    for k in range(7):
        p.curve("M %d 28 Q %d 36 %d 52" % (20 + k * 10, 22 + k * 10, 28 + k * 8), "#C9955E", 2)
    p.shine(30, 56, 7, 3, -10, .5)
    p.sparkle(82, 20, 5, "#FFFFFF")
    return p.out()


def wfood_7():  # curry
    p = Pc()
    p.path("M 8 48 H 92 C 92 76 74 90 50 90 C 26 90 8 76 8 48 Z", ["#F2EBDD", "#C9B896"])
    p.ell(50, 48, 42, 10, ["#FFFFFF", "#E8E0D0"])
    p.path("M 14 46 Q 28 34 50 36 Q 72 34 86 46 Q 72 54 50 52 Q 28 54 14 46 Z", ["#F2A91F", "#B8502B"], ol=False)
    for x, y in ((36, 44), (52, 42), (66, 46)):
        p.circ(x, y, 5, ["#C9702B", "#7A3A1B"], sw=1)
    p.circ(62, 38, 3, "#6FBF73", ol=False)
    return p.out()


def wfood_8():  # paella
    p = Pc()
    p.ell(50, 62, 42, 22, ["#4A4A68", "#1E1E32"])
    p.ell(50, 60, 36, 17, ["#FFD36E", "#E8A91F"], ol=False)
    p.rect(88, 56, 10, 6, ["#4A4A68", "#1E1E32"], r=2)
    p.rect(2, 56, 10, 6, ["#4A4A68", "#1E1E32"], r=2)
    for x, y, c in ((36, 56, "#FF8A7A"), (58, 52, "#7FD08A"), (68, 62, "#FF8A7A"), (46, 68, "#FFFFFF")):
        p.ell(x, y, 8, 5, c, sw=1, rot=x)
    p.path("M 28 60 Q 34 50 42 56 Q 36 66 28 60 Z", ["#FFB86B", "#E8642B"], sw=1)
    return p.out()


def wfood_9():  # pasta
    p = Pc()
    p.ell(50, 64, 42, 24, ["#FFFFFF", "#C9D3E8"])
    p.ell(50, 60, 34, 17, ["#F5F0E8", "#E8E0D0"], ol=False)
    for k in range(7):
        p.curve("M %d 48 Q %d 36 %d 52 T %d 60" % (24 + k * 8, 30 + k * 8, 36 + k * 8, 46 + k * 8), "#FFE08A", 3.4)
    p.path("M 34 52 Q 50 38 66 52 Q 50 62 34 52 Z", ["#FF8A7A", "#C8203B"], ol=False, op=.85)
    p.circ(46, 50, 4, ["#7FD08A", "#2E7A4A"], sw=1)
    p.circ(58, 54, 4, ["#7FD08A", "#2E7A4A"], sw=1)
    return p.out()


def wfood_10():  # ensalada
    p = Pc()
    p.path("M 8 44 H 92 C 92 76 74 90 50 90 C 26 90 8 76 8 44 Z", ["#FFFFFF", "#C9D3E8"])
    for x, y, a, c in ((24, 40, -30, "#6FCF7A"), (44, 32, 10, "#4E9A5A"), (66, 34, 30, "#7FD08A"), (82, 42, 50, "#6FCF7A"), (54, 40, -10, "#9ADD92")):
        p.ell(x, y, 12, 8, [lt(c, .3), c], rot=a, sw=1.1)
    p.circ(34, 44, 8, ["#FF8A7A", "#C8203B"])
    p.circ(62, 46, 7, ["#FF8A7A", "#C8203B"])
    p.ell(50, 36, 7, 5, ["#FFE88A", "#E8A91F"], sw=1)
    p.ell(74, 46, 6, 4, ["#FFFFFF", "#E8D8B0"], sw=1)
    return p.out()


# ------------------------------------------------------------------ Maravillas del mundo
def land_1():  # torre de Tokio
    p = Pc()
    p.poly([(50, 4), (58, 40), (66, 64), (86, 90), (14, 90), (34, 64), (42, 40)], ["#FF9A5A", "#C8203B"])
    p.rect(36, 36, 28, 7, ["#FFFFFF", "#D3DCEF"], r=1.5)
    p.rect(30, 56, 40, 7, ["#FFFFFF", "#D3DCEF"], r=1.5)
    p.line(50, 4, 50, 0, "#C8203B", 2)
    p.s.path("M 34 90 Q 50 64 66 90", stroke="#FFFFFF", sw=2.4)
    for x in (44, 50, 56):
        p.line(x, 44, x, 54, "#FFFFFF", 1.2)
    return p.out()


def land_2():  # estatua de la libertad
    p = Pc()
    p.poly([(30, 92), (36, 66), (64, 66), (70, 92)], ["#B8F0D8", "#4E9A8A"])
    p.path("M 36 66 C 32 50 38 40 50 38 C 62 40 68 50 64 66 Z", ["#9AE8CC", "#3E8A7A"])
    p.circ(50, 30, 10, ["#B8F0D8", "#4E9A8A"])
    for a in (-60, -30, 0, 30, 60):
        p.line(50 + math.sin(math.radians(a)) * 9, 22 - math.cos(math.radians(a)) * 2, 50 + math.sin(math.radians(a)) * 20, 14 - math.cos(math.radians(a)) * 6, "#9AE8CC", 3)
    p.line(64, 46, 76, 24, "#9AE8CC", 5)
    p.rect(72, 12, 8, 14, ["#9AE8CC", "#3E8A7A"], r=2)
    p.path("M 72 12 Q 76 0 80 12 Z", ["#FFE86B", "#F29A1F"])
    p.rect(22, 90, 56, 6, ["#C9D3E8", "#7A88A8"], r=2)
    return p.out()


def land_3():  # castillo japones
    p = Pc()
    p.rect(22, 62, 56, 28, ["#F2EBDD", "#C9B896"], r=2)
    for y, w in ((46, 36), (30, 26), (16, 16)):
        p.rect(50 - w / 2 + 2, y + 8, w - 4, 16, ["#F2EBDD", "#C9B896"], r=1)
        p.path("M %f %f Q %f %f %f %f L %f %f Q %f %f %f %f Z" % (50 - w, y + 10, 50 - w + 4, y + 2, 50, y, 50, y + 4, 50 + w - 4, y + 2, 50 + w, y + 10), ["#4A6A8A", "#1E3A52"])
    p.path("M 40 90 V 72 Q 50 62 60 72 V 90 Z", ["#8A5A2B", "#4A3220"])
    p.poly([(46, 12), (50, 2), (54, 12)], ["#FFE08A", "#C9902B"], sw=1)
    return p.out()


def land_4():  # mezquita
    p = Pc()
    p.rect(22, 54, 56, 36, ["#F5EBD2", "#C9B896"], r=2)
    p.path("M 26 54 C 26 24 74 24 74 54 Z", ["#8AD0F5", "#3F7FD0"])
    p.line(50, 24, 50, 14, "#C9902B", 2.4)
    p.poly([(50, 8), (55, 16), (45, 16)], ["#FFE08A", "#C9902B"], sw=1)
    for x in (12, 88):
        p.rect(x - 5, 28, 10, 62, ["#FFFFFF", "#D3DCEF"], r=2)
        p.poly([(x - 6, 28), (x, 12), (x + 6, 28)], ["#8AD0F5", "#3F7FD0"], sw=1.1)
        p.rect(x - 6, 44, 12, 4, ["#C9902B", "#8A5A2B"], r=1, ol=False)
    p.path("M 42 90 V 70 Q 50 58 58 70 V 90 Z", ["#4A5A8A", "#162036"])
    return p.out()


def land_5():  # partenon
    p = Pc()
    p.poly([(8, 34), (50, 10), (92, 34)], ["#F5EBD2", "#C9B896"])
    p.rect(12, 34, 76, 8, ["#FFFFFF", "#E0D8C0"], r=1)
    for x in range(16, 90, 14):
        p.rect(x, 42, 9, 38, ["#F5EBD2", "#C9B896"], r=2, sw=1.2)
    p.rect(8, 80, 84, 7, ["#E0D8C0", "#A89868"], r=2)
    p.rect(4, 86, 92, 6, ["#D0C8A8", "#988858"], r=2)
    return p.out()


def land_6():  # torii
    p = Pc()
    p.path("M 6 22 Q 50 10 94 22 L 90 30 Q 50 22 10 30 Z", ["#FF8A7A", "#C8203B"])
    p.rect(14, 34, 72, 8, ["#FF8A7A", "#C8203B"], r=2)
    p.rect(24, 28, 10, 62, ["#FF8A7A", "#C8203B"], r=2)
    p.rect(66, 28, 10, 62, ["#FF8A7A", "#C8203B"], r=2)
    p.rect(44, 26, 12, 14, ["#2B2B45", "#12121E"], r=2)
    p.rect(20, 86, 18, 6, "#4A4A68", r=2)
    p.rect(62, 86, 18, 6, "#4A4A68", r=2)
    return p.out()


def land_7():  # puente colgante
    p = Pc()
    p.rect(8, 56, 84, 7, ["#E8586D", "#B8203B"], r=2)
    for x in (24, 76):
        p.rect(x - 4, 14, 8, 78, ["#FF8A7A", "#C8203B"], r=2)
    p.s.path("M 4 52 Q 24 52 24 18", stroke="#FF8A7A", sw=2.4)
    p.s.path("M 24 18 Q 50 62 76 18", stroke="#FF8A7A", sw=3.4)
    p.s.path("M 76 18 Q 76 52 96 52", stroke="#FF8A7A", sw=2.4)
    for x in range(30, 74, 8):
        y = 18 + (1 - ((x - 50) / 26.0) ** 2) * 0
        p.line(x, 28 + abs(x - 50) * -0.0 + (26 - abs(x - 50)) * 0.9, x, 56, "#FFC0B0", 1.2)
    p.path("M 4 74 Q 28 66 50 74 T 96 74 V 92 H 4 Z", ["#7FD0F5", "#2B7AC0"], ol=False)
    return p.out()


def land_8():  # coliseo
    p = Pc()
    p.path("M 4 86 V 38 C 4 20 96 20 96 38 V 86 Z", ["#E8C27A", "#B8903B"])
    for row, y in enumerate((42, 62)):
        for k in range(7):
            x = 14 + k * 12
            p.path("M %d %d V %d Q %d %d %d %d V %d Z" % (x, y + 14, y + 4, x + 4, y - 2, x + 8, y + 4, y + 14), ["#8A5A2B", "#4A3220"], ol=False)
    p.rect(4, 80, 92, 8, ["#D0A860", "#8A6A2B"], r=2)
    p.curve("M 10 30 Q 50 14 90 30", "#F5D79A", 2.4, op=.7)
    return p.out()


def land_9():  # catedral
    p = Pc()
    p.rect(22, 38, 56, 52, ["#F5EBD2", "#C9B896"], r=2)
    for x in (14, 78):
        p.rect(x, 20, 14, 70, ["#E8DCC0", "#B8A878"], r=2)
        p.poly([(x - 2, 20), (x + 7, 0), (x + 16, 20)], ["#8AB0F5", "#3F5FD0"], sw=1.1)
    p.poly([(22, 38), (50, 14), (78, 38)], ["#8AB0F5", "#3F5FD0"])
    p.circ(50, 46, 9, ["#FFE08A", "#C9902B"])
    p.path("M 42 90 V 66 Q 50 54 58 66 V 90 Z", ["#8A5A2B", "#4A3220"])
    p.line(50, 14, 50, 4, "#C9902B", 2)
    p.line(46, 8, 54, 8, "#C9902B", 2)
    return p.out()


def land_10():  # isla secreta
    p = Pc(shadow=False)
    p.path("M 4 74 Q 24 66 50 74 T 96 74 V 94 H 4 Z", ["#7FD0F5", "#2B7AC0"])
    p.ell(50, 74, 34, 11, ["#FFE0A8", "#E8A06B"])
    p.path("M 50 72 Q 46 46 52 24", ["#A9733B", "#5A3A26"], ol=False)
    p.curve("M 50 72 Q 46 46 52 24", "#8A5A2B", 5)
    for a, c in ((-70, "#6FBF73"), (-30, "#4E9A5A"), (20, "#7FD08A"), (60, "#4E9A5A")):
        p.leaf(52, 24, 30, 7, a - 90 + 10, c)
    p.circ(50, 28, 3, "#8A5A2B", ol=False)
    p.sparkle(82, 30, 5)
    return p.out()


# ------------------------------------------------------------------ Verano
def summer_1():  # coctel
    p = Pc()
    p.path("M 14 22 H 86 L 54 56 V 82 H 66 V 90 H 34 V 82 H 46 V 56 Z", ["#FFFFFF", "#C9D3E8"])
    p.path("M 22 28 H 78 L 54 52 H 46 Z", ["#FF9A7A", "#E8502B"], ol=False)
    p.line(64, 20, 80, 2, "#E8586D", 3)
    p.circ(26, 22, 9, ["#FFE86B", "#E8A91F"])
    p.circ(26, 22, 4, "#FFFFFF", ol=False)
    p.sparkle(76, 38, 4)
    return p.out()


def summer_2():  # coco
    p = Pc()
    p.circ(50, 56, 34, ["#B8886A", "#5A3A2B"])
    p.path("M 24 36 Q 50 6 76 36 Q 50 24 24 36 Z", ["#FFFFFF", "#E8E0D0"], ol=False)
    for x, y in ((40, 54), (60, 54), (50, 66)):
        p.circ(x, y, 3.4, "#2B1B3A", ol=False)
    p.line(62, 24, 80, 6, "#FF8AB8", 4)
    p.line(70, 14, 84, 14, "#FF8AB8", 3)
    p.shine(36, 44, 5, 3, -35, .45)
    return p.out()


def summer_3():  # gafas de sol
    p = Pc()
    p.path("M 6 36 H 94 V 44 H 6 Z", ["#2B2B45", "#12121E"], ol=False)
    for x in (30, 70):
        p.path("M %d 34 H %d C %d 62 %d 70 %d 64 C %d 62 %d 40 %d 34 Z" % (x - 24, x + 24, x + 24, x + 8, x, x - 8, x - 24, x - 24), ["#4A4A68", "#12121E"])
        p.shine(x - 10, 44, 6, 2.4, -20, .6)
    p.rect(44, 40, 12, 5, ["#2B2B45", "#12121E"], r=2, ol=False)
    p.sparkle(84, 24, 6, "#FFE08A")
    return p.out()


def summer_4():  # traje de bano
    p = Pc()
    p.path("M 14 24 Q 30 40 36 56 Q 26 70 12 74 Q 18 54 14 24 Z", ["#FF8AB8", "#D6336C"], ol=False)
    p.path("M 22 20 Q 40 34 50 52 Q 36 68 14 72 Q 22 44 22 20 Z", ["#FF8AB8", "#D6336C"])
    p.path("M 78 20 Q 60 34 50 52 Q 64 68 86 72 Q 78 44 78 20 Z", ["#FF8AB8", "#D6336C"])
    p.s.path("M 22 20 Q 50 -2 78 20", stroke="#FFFFFF", sw=3)
    for x, y in ((32, 44), (68, 44), (46, 62), (54, 62)):
        p.circ(x, y, 3, "#FFFFFF", ol=False)
    return p.out()


def summer_5():  # playa
    p = Pc(shadow=False)
    p.path("M 4 62 Q 28 54 50 62 T 96 60 V 94 H 4 Z", ["#7FD0F5", "#2B7AC0"])
    p.path("M 4 78 Q 30 66 60 76 T 96 72 V 94 H 4 Z", ["#FFE0A8", "#E8A06B"], ol=False)
    p.line(36, 34, 40, 84, "#8A5A2B", 3)
    p.path("M 8 40 C 10 18 56 10 66 38 Q 56 30 46 38 Q 36 30 26 38 Q 18 30 8 40 Z", ["#FF8A7A", "#C8203B"])
    p.circ(82, 22, 11, ["#FFF1A8", "#FFB02B"], ol=False)
    p.ell(70, 84, 10, 4, "#FF8AB8", ol=False)
    return p.out()


def summer_6():  # palmera
    p = Pc()
    p.path("M 46 92 Q 40 60 50 34 Q 62 60 56 92 Z", ["#C98A55", "#7A4A2B"])
    for a, c in ((-75, "#6FBF73"), (-40, "#4E9A5A"), (0, "#7FD08A"), (40, "#4E9A5A"), (75, "#6FBF73")):
        p.leaf(52, 34, 40, 10, a - 90 + (6 if a > 0 else -6), c)
    p.circ(46, 40, 5, ["#8A5A2B", "#4A3220"])
    p.circ(56, 42, 5, ["#8A5A2B", "#4A3220"])
    p.path("M 8 92 Q 28 82 48 92 Z", ["#FFE0A8", "#E8A06B"], ol=False)
    return p.out()


def summer_7():  # sol sonriente
    p = Pc(shadow=False)
    for a in range(0, 360, 30):
        p.poly([(50 + math.cos(math.radians(a - 9)) * 32, 50 + math.sin(math.radians(a - 9)) * 32), (50 + math.cos(math.radians(a)) * 47, 50 + math.sin(math.radians(a)) * 47), (50 + math.cos(math.radians(a + 9)) * 32, 50 + math.sin(math.radians(a + 9)) * 32)], ["#FFE86B", "#F29A1F"], sw=1)
    p.circ(50, 50, 30, ["#FFF1A8", "#FFB02B"])
    p.eyes(40, 60, 46, 3)
    p.curve("M 38 58 Q 50 70 62 58", "#C9702B", 3)
    p.circ(32, 58, 5, "#FF8A7A", ol=False, sw=0)
    p.circ(68, 58, 5, "#FF8A7A", ol=False, sw=0)
    return p.out()


def summer_8():  # atardecer en la costa
    p = Pc(shadow=False)
    p.rect(4, 8, 92, 56, [lt("#FF8A7A", .2), "#FFC27A"], r=8, ol=False)
    p.circ(50, 56, 26, ["#FFF1A8", "#FF8A3A"], ol=False)
    p.rect(4, 56, 92, 36, ["#2B7AC0", "#1E3A8A"], r=0, ol=False)
    for y, w in ((62, 36), (70, 28), (78, 20), (86, 12)):
        p.line(50 - w, y, 50 + w, y, "#FFD79A", 2.6)
    for x, h in ((8, 26), (18, 34), (28, 22), (74, 30), (84, 38), (92, 24)):
        p.rect(x, 62 - h, 8, h, ["#4A3A5A", "#1E1030"], r=1, ol=False)
    return p.out()


def summer_9():  # pesca
    p = Pc()
    p.line(14, 8, 70, 40, "#8A5A2B", 3.4)
    p.s.path("M 70 40 Q 80 56 74 70", stroke="#F5F5FF", sw=1.4)
    p.circ(74, 40, 3, "#E8586D", ol=False)
    p.path("M 4 74 Q 24 66 50 74 T 96 74 V 94 H 4 Z", ["#7FD0F5", "#2B7AC0"])
    p.ell(70, 78, 14, 8, ["#FFB86B", "#E8642B"])
    p.poly([(56, 78), (46, 70), (46, 86)], ["#FFB86B", "#E8642B"], sw=1)
    p.circ(76, 76, 2, "#2B1B3A", ol=False)
    return p.out()


def summer_10():  # lancha
    p = Pc()
    p.path("M 6 56 H 94 L 80 80 H 24 Z", ["#FF8A7A", "#C8203B"])
    p.path("M 6 56 H 94 L 90 64 H 10 Z", ["#FFFFFF", "#C9D3E8"], ol=False)
    p.path("M 32 56 L 40 36 H 62 L 70 56 Z", ["#BFEFFF", "#4F9FD0"])
    p.rect(60, 28, 4, 10, "#4A4A68", r=1)
    p.path("M 4 86 Q 24 78 50 86 T 98 84 V 96 H 4 Z", ["#7FD0F5", "#2B7AC0"], ol=False)
    p.curve("M 4 92 Q 20 86 36 92 T 70 92", "#FFFFFF", 2.4, op=.8)
    return p.out()


def build():
    icons = {}
    for pre, ser in (("myth", "myth"), ("wfood", "worldfood"), ("land", "landmarks"), ("summer", "summer")):
        for i in range(1, 11):
            icons["piece.%s_%d" % (ser, i)] = globals()["%s_%d" % (pre, i)]()
    return {"icons": icons}
