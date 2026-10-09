import os
import sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from _pk import *  # noqa: F401,F403
import _animals

WH = ["#FFFFFF", "#C9D3E8"]


# ------------------------------------------------------------------ Selva
def jungle_1():
    return _animals.head("MONKEY")


def jungle_2():  # loro
    p = Pc()
    p.path("M 34 90 C 22 60 26 20 52 12 C 76 8 84 34 74 54 C 70 66 62 74 58 90 Z", ["#FF8A7A", "#C83A2B"])
    p.path("M 52 12 C 76 8 84 34 74 54 C 66 40 60 26 52 12 Z", ["#7FD0F5", "#2B7AC0"], ol=False)
    p.path("M 34 90 C 40 80 44 70 48 60 C 52 76 54 84 58 90 Z", ["#FFE86B", "#E8A91F"], ol=False)
    p.path("M 60 24 C 80 22 92 34 86 44 C 78 42 68 38 60 34 Z", ["#FFE08A", "#C9902B"])
    p.circ(56, 26, 4.4, "#FFFFFF")
    p.circ(56, 26, 2, "#2B1B3A", ol=False)
    return p.out()


def jungle_3():  # serpiente
    p = Pc()
    p.s.path("M 20 86 C 4 70 30 60 50 70 C 70 80 92 66 78 46 C 68 34 40 40 46 24", stroke="#4E9A5A", sw=14, cap=CAP_ROUND)
    p.s.path("M 20 86 C 4 70 30 60 50 70 C 70 80 92 66 78 46 C 68 34 40 40 46 24", stroke=lin(0, 20, 100, 90, [(0, "#9ADD92"), (1, "#3E8A5A")]), sw=11, cap=CAP_ROUND)
    p.ell(48, 22, 12, 9, ["#9ADD92", "#3E8A5A"])
    p.eyes(44, 54, 19, 2.4)
    p.curve("M 56 26 L 66 30 M 66 30 L 70 26 M 66 30 L 70 34", "#E8586D", 1.6)
    for x, y in ((30, 70), (60, 76), (80, 56)):
        p.circ(x, y, 3, "#FFE86B", ol=False)
    return p.out()


def jungle_4():
    return _animals.head("FROG")


def jungle_5():  # elefante
    p = Pc()
    p.circ(22, 46, 20, ["#B8C4D8", "#6A7A9A"])
    p.circ(78, 46, 20, ["#B8C4D8", "#6A7A9A"])
    p.circ(50, 46, 26, ["#C9D3E8", "#7A88A8"])
    p.path("M 40 56 C 36 78 38 92 52 90 C 60 88 56 80 52 78 C 50 70 56 62 60 56 Z", ["#C9D3E8", "#7A88A8"])
    p.eyes(40, 60, 42, 2.8)
    p.poly([(30, 64), (22, 78), (36, 70)], "#FFFFFF", sw=1)
    p.poly([(70, 64), (78, 78), (64, 70)], "#FFFFFF", sw=1)
    return p.out()


def jungle_6():  # gorila
    p = Pc()
    p.circ(50, 46, 30, ["#6A6A82", "#2B2B3E"])
    p.ell(50, 60, 20, 16, ["#A8A0B0", "#5A5468"])
    p.ell(50, 38, 22, 12, ["#8A8AA2", "#3E3E55"], ol=False)
    p.eyes(40, 60, 44, 3)
    p.ell(44, 60, 2.4, 3.2, "#2B1B3A", ol=False)
    p.ell(56, 60, 2.4, 3.2, "#2B1B3A", ol=False)
    p.curve("M 42 70 Q 50 74 58 70", "#2B1B3A", 2)
    p.circ(20, 50, 7, ["#8A8AA2", "#3E3E55"])
    p.circ(80, 50, 7, ["#8A8AA2", "#3E3E55"])
    return p.out()


def jungle_7():  # rinoceronte
    p = Pc()
    p.path("M 8 70 C 8 40 30 26 56 30 C 78 32 94 44 94 66 C 94 84 80 90 66 88 L 24 88 C 12 88 8 80 8 70 Z", ["#B8C4D8", "#6A7A9A"])
    p.path("M 14 52 L 4 36 L 24 44 Z", ["#B8C4D8", "#6A7A9A"])
    p.path("M 82 54 C 92 36 100 48 94 58 Z", ["#FFFFFF", "#C9D3E8"])
    p.poly([(84, 54), (96, 54), (92, 28)], ["#FFFFFF", "#C9D3E8"])
    p.eyes(70, 70, 56, 2.8)
    p.poly([(36, 34), (46, 30), (42, 42)], ["#B8C4D8", "#6A7A9A"], sw=1)
    return p.out()


def jungle_8():  # jaguar
    p = _animals.head("TIGER", "GOLD", extra={"dark": "#3A2414"})
    return p


def jungle_9():  # jirafa
    p = Pc()
    p.poly([(34, 10), (36, 28), (28, 24)], ["#FFE08A", "#C9902B"], sw=1)
    p.poly([(66, 10), (64, 28), (72, 24)], ["#FFE08A", "#C9902B"], sw=1)
    p.circ(34, 8, 3, "#8A5A2B", ol=False)
    p.circ(66, 8, 3, "#8A5A2B", ol=False)
    p.circ(24, 36, 7, ["#FFE08A", "#C9902B"])
    p.circ(76, 36, 7, ["#FFE08A", "#C9902B"])
    p.ell(50, 40, 22, 26, ["#FFE88A", "#E8A91F"])
    p.ell(50, 56, 17, 13, ["#FFF4D0", "#E8C080"])
    for x, y in ((36, 26), (62, 30), (44, 20), (30, 44)):
        p.ell(x, y, 5, 4, ["#C98A55", "#7A4A2B"], ol=False)
    p.eyes(40, 60, 40, 2.8)
    p.ell(44, 58, 2, 2.6, "#8A5A2B", ol=False)
    p.ell(56, 58, 2, 2.6, "#8A5A2B", ol=False)
    return p.out()


def jungle_10():
    return _animals.head("TIGER")


# ------------------------------------------------------------------ Artico
def arctic_1():
    return _animals.head("PENGUIN")


def arctic_2():  # muneco de nieve
    p = Pc()
    p.circ(50, 70, 22, WH)
    p.circ(50, 38, 16, WH)
    p.rect(36, 18, 28, 5, ["#4A4A68", "#1E1E32"], r=2)
    p.rect(40, 6, 20, 14, ["#4A4A68", "#1E1E32"], r=3)
    p.poly([(50, 38), (66, 42), (50, 44)], ["#FFB86B", "#E8642B"], sw=1)
    p.eyes(44, 56, 34, 2.2)
    for y in (62, 72, 82):
        p.circ(50, y, 2.4, "#2B2B45", ol=False)
    p.rect(34, 46, 32, 6, ["#E8586D", "#B8203B"], r=3)
    p.line(28, 62, 14, 52, "#6A4630", 2.6)
    p.line(72, 62, 86, 52, "#6A4630", 2.6)
    return p.out()


def arctic_3():  # esqui
    p = Pc()
    for dx in (0, 18):
        with p.s.rotate(-20, 50, 50):
            p.rect(30 + dx, 8, 7, 84, ["#7FD0F5", "#2B7AC0"], r=3)
            p.rect(30 + dx, 8, 7, 14, ["#FF8A7A", "#C83A2B"], r=3)
    p.line(78, 20, 58, 86, "#4A4A68", 2.6)
    p.line(72, 24, 84, 18, "#4A4A68", 2.6)
    return p.out()


def arctic_4():  # trineo
    p = Pc()
    p.rect(14, 44, 72, 22, ["#E8586D", "#B8203B"], r=6)
    p.rect(20, 40, 60, 6, ["#FFE08A", "#C9902B"], r=3)
    p.s.path("M 8 78 H 78 Q 94 78 88 62", stroke="#8A8AA8", sw=4, cap=CAP_ROUND)
    p.line(24, 66, 24, 78, "#8A8AA8", 3)
    p.line(66, 66, 66, 78, "#8A8AA8", 3)
    p.curve("M 14 44 Q 4 36 14 28", "#8A5A2B", 2.6)
    return p.out()


def arctic_5():
    return _animals.head("BEAR", "FROST")


def arctic_6():
    return _animals.head("DEER")


def arctic_7():  # glaciar
    p = Pc()
    p.poly([(6, 86), (30, 34), (46, 56), (64, 22), (94, 86)], ["#E8F8FF", "#7FC0EB"])
    p.poly([(64, 22), (76, 60), (58, 86), (46, 56)], ["#BFEFFF", "#4F9FD0"], ol=False)
    p.poly([(30, 34), (46, 56), (34, 86), (22, 62)], ["#D3F4FF", "#8AD0F5"], ol=False)
    p.path("M 6 86 Q 28 78 50 86 T 94 86 V 92 H 6 Z", ["#7FD0F5", "#2B7AC0"], ol=False)
    p.sparkle(70, 30, 5)
    return p.out()


def arctic_8():
    return _animals.head("DRAGON", "FROST")


def arctic_9():  # guantes
    p = Pc()
    p.path("M 26 90 V 52 C 14 44 20 24 30 30 L 34 38 L 36 14 C 38 6 46 8 46 14 L 48 36 L 52 8 C 54 2 62 4 62 12 L 62 38 C 66 26 80 28 76 42 L 74 70 C 74 80 66 90 56 90 Z", ["#FF8A9A", "#C8203B"])
    p.rect(24, 78, 52, 12, ["#FFFFFF", "#D3DCEF"], r=4)
    p.curve("M 28 60 H 70", "#FFFFFF", 2.4, op=.7)
    return p.out()


def arctic_10():  # cristal de escarcha
    p = Pc()
    p.poly([(50, 6), (70, 40), (62, 90), (38, 90), (30, 40)], ["#BFEFFF", "#4F9FD0"])
    p.poly([(50, 6), (50, 90), (38, 90), (30, 40)], ["#E8FAFF", "#8AD0F5"], ol=False)
    p.poly([(14, 52), (30, 40), (34, 80), (20, 74)], ["#BFEFFF", "#4F9FD0"])
    p.poly([(86, 52), (70, 40), (66, 80), (80, 74)], ["#BFEFFF", "#4F9FD0"])
    p.sparkle(60, 24, 6)
    return p.out()


# ------------------------------------------------------------------ Desierto
def desert_1():  # cactus
    p = Pc()
    p.rect(38, 14, 24, 76, ["#7FD08A", "#2E7A4A"], r=12)
    p.path("M 38 56 H 28 C 18 56 18 36 26 36 V 50 H 38 Z", ["#7FD08A", "#2E7A4A"])
    p.path("M 62 66 H 72 C 82 66 82 44 74 44 V 60 H 62 Z", ["#7FD08A", "#2E7A4A"])
    for x in (44, 50, 56):
        p.line(x, 22, x, 82, "#2E7A4A", 1.1)
    p.flower(50, 12, 9, 5, "#FF8AB8", "#FFE08A")
    return p.out()


def desert_2():  # camello
    p = Pc()
    for x in (28, 38, 62, 72):
        p.rect(x - 3.5, 66, 7, 24, ["#E8B880", "#9A6A38"], r=3)
    p.ell(50, 58, 34, 17, ["#F0C490", "#B8782B"])
    p.ell(38, 40, 10, 16, ["#F0C490", "#B8782B"])
    p.ell(62, 40, 10, 16, ["#F0C490", "#B8782B"])
    p.path("M 76 56 C 84 46 84 30 82 22 L 94 22 C 98 26 96 34 92 36 C 92 46 90 54 88 62 Z", ["#F0C490", "#B8782B"])
    p.circ(90, 28, 2, "#2B1B3A", ol=False)
    p.path("M 28 52 H 72 V 60 H 28 Z", ["#E8586D", "#B8203B"], ol=False)
    p.curve("M 20 54 Q 12 58 14 70", "#9A6A38", 3)
    return p.out()


def desert_3():  # escorpion
    p = Pc()
    p.s.path("M 70 74 C 94 70 96 36 70 24", stroke="#C83A2B", sw=7, cap=CAP_ROUND)
    p.poly([(70, 24), (62, 14), (76, 16)], ["#FF8A7A", "#C83A2B"])
    p.ell(46, 66, 24, 14, ["#FF8A5A", "#C8501B"])
    for s_ in (-1, 1):
        p.s.path("M %f 52 C %f 34 %f 34 %f 40" % (36 + s_ * 4, 24 + s_ * 10, 12 + s_ * 20 + (0 if s_ < 0 else 20), 18 + (s_ + 1) * 22), stroke="#C8501B", sw=5, cap=CAP_ROUND)
        p.circ(14 + (s_ + 1) * 22, 40 - (0), 8, ["#FF9A6A", "#C8501B"])
    for k in range(3):
        p.line(30 + k * 10, 76, 26 + k * 10, 90, "#C8501B", 2.4)
    p.eyes(36, 46, 60, 2.2)
    return p.out()


def desert_4():  # lagartija
    p = Pc()
    p.s.path("M 16 80 C 4 60 30 50 40 40 C 56 24 76 36 84 44", stroke="#7FD08A", sw=12, cap=CAP_ROUND)
    p.s.path("M 16 80 C 4 60 30 50 40 40 C 56 24 76 36 84 44", stroke=lin(10, 40, 90, 80, [(0, "#B8F0A0"), (1, "#3E8A5A")]), sw=9, cap=CAP_ROUND)
    p.ell(86, 44, 11, 8, ["#B8F0A0", "#3E8A5A"])
    p.eyes(86, 86, 40, 2.2)
    for x, y in ((46, 36), (60, 30), (34, 56)):
        p.line(x, y, x - 6, y + 14, "#3E8A5A", 3.4)
    for x, y in ((40, 48), (54, 36), (68, 36)):
        p.circ(x, y, 3, "#FFE86B", ol=False)
    return p.out()


def desert_5():  # dunas
    p = Pc(shadow=False)
    p.path("M 4 74 C 24 40 46 56 62 70 C 74 58 88 56 96 64 V 92 H 4 Z", ["#FFD38A", "#E8A04A"])
    p.path("M 4 86 C 30 66 54 90 96 74 V 94 H 4 Z", ["#FFB86B", "#C8782B"], ol=False)
    p.circ(76, 28, 14, ["#FFF1A8", "#FFB02B"])
    p.curve("M 20 66 Q 32 60 44 66", "#FFF1D0", 2, op=.8)
    return p.out()


def desert_6():  # tienda beduina
    p = Pc()
    p.path("M 4 86 L 24 36 Q 50 20 76 36 L 96 86 Z", ["#FFB86B", "#C8782B"])
    p.path("M 36 86 L 44 50 Q 50 46 56 50 L 64 86 Z", ["#4A2A2A", "#1E1010"], ol=False)
    p.path("M 24 36 Q 50 20 76 36 L 72 44 Q 50 32 28 44 Z", ["#E8586D", "#B8203B"], ol=False)
    p.line(50, 22, 50, 8, "#8A5A2B", 2.4)
    p.poly([(50, 8), (64, 12), (50, 16)], ["#FFE08A", "#C9902B"], sw=1)
    return p.out()


def desert_7():  # anfora
    p = Pc()
    p.path("M 36 10 H 64 V 20 C 62 28 78 36 78 58 C 78 80 64 90 50 90 C 36 90 22 80 22 58 C 22 36 38 28 36 20 Z", ["#E8A06B", "#B8502B"])
    p.s.path("M 24 52 C 40 58 60 58 76 52", stroke="#FFE0B0", sw=3)
    p.s.path("M 24 66 C 40 72 60 72 76 66", stroke="#FFE0B0", sw=3)
    p.s.path("M 36 14 C 14 12 12 40 28 44", stroke="#B8502B", sw=5, cap=CAP_ROUND)
    p.s.path("M 64 14 C 86 12 88 40 72 44", stroke="#B8502B", sw=5, cap=CAP_ROUND)
    p.shine(34, 52, 4, 12, 10, .45)
    return p.out()


def desert_8():  # esfinge
    p = Pc()
    p.path("M 6 88 V 68 C 6 56 16 52 30 52 L 30 36 C 30 18 70 18 70 36 V 52 L 94 60 V 88 Z", ["#E8C27A", "#B8903B"])
    p.path("M 30 36 C 30 18 70 18 70 36 L 74 56 H 26 Z", ["#F5D79A", "#C9A24B"])
    p.poly([(30, 36), (22, 50), (30, 56)], ["#4F8FE0", "#2A4AA0"], sw=1)
    p.poly([(70, 36), (78, 50), (70, 56)], ["#4F8FE0", "#2A4AA0"], sw=1)
    p.eyes(42, 58, 40, 2.4)
    p.ell(50, 50, 3, 4, "#C9902B", ol=False)
    p.curve("M 44 58 Q 50 60 56 58", "#8A5A2B", 1.6)
    return p.out()


def desert_9():  # aguila
    p = Pc()
    p.path("M 50 90 C 24 82 10 56 20 26 C 28 12 48 8 62 18 C 74 26 78 40 76 52 C 70 70 60 82 50 90 Z", ["#C98A55", "#7A4A2B"])
    p.path("M 28 30 C 34 16 56 12 66 24 C 56 32 40 34 28 30 Z", ["#FFFFFF", "#D3DCEF"], ol=False)
    p.path("M 64 36 C 80 34 94 44 90 54 C 82 52 72 50 64 46 Z", ["#FFE08A", "#C9902B"])
    p.circ(56, 34, 4.6, "#FFFFFF", sw=1)
    p.circ(57, 34, 2.4, "#2B1B3A", ol=False)
    p.path("M 50 90 C 56 78 62 70 66 62 C 72 74 64 86 50 90 Z", ["#FFFFFF", "#D3DCEF"], ol=False)
    return p.out()


def desert_10():  # genio
    p = Pc()
    p.path("M 14 80 C 14 66 30 64 40 64 H 60 C 70 64 80 66 82 74 L 94 66 C 94 78 90 86 80 88 H 30 C 20 88 14 86 14 80 Z", ["#FFE08A", "#C9902B"])
    p.path("M 50 64 C 30 50 66 40 50 22 C 36 12 56 6 66 12", ["#8AD0F5", "#3F7FD0"], ol=False)
    p.s.path("M 50 64 C 30 50 66 40 50 24", stroke="#8AD0F5", sw=12, cap=CAP_ROUND, op=.9)
    p.circ(54, 18, 11, ["#8AD0F5", "#3F7FD0"])
    p.eyes(50, 59, 17, 2)
    p.sparkle(80, 30, 6, "#FFE08A")
    p.sparkle(22, 40, 5, "#FFE08A")
    return p.out()


# ------------------------------------------------------------------ Espacio
def space_1():  # satelite
    p = Pc(shadow=False)
    with p.s.rotate(-30, 50, 50):
        p.rect(38, 36, 24, 28, ["#C9D3E8", "#7A88A8"], r=4)
        for dx in (-1, 1):
            x = 50 + dx * 32
            p.rect(x - 14, 40, 28, 20, ["#4F8FE0", "#1E3A8A"], r=2)
            for k in (-7, 0, 7):
                p.line(x + k, 40, x + k, 60, "#8AB0F5", 1)
            p.line(50 + dx * 12, 50, x - dx * 14, 50, "#7A88A8", 2.4)
        p.line(50, 36, 50, 22, "#7A88A8", 2.4)
        p.circ(50, 20, 4, "#FF8A7A")
    p.sparkle(20, 22, 5)
    return p.out()


def space_2():  # cometa
    p = Pc(shadow=False)
    p.s.path("M 78 22 Q 40 40 10 78", stroke=lin(78, 22, 10, 78, [(0, "#8AD0F5"), (1, "#8AD0F500")]), sw=18, cap=CAP_ROUND)
    p.s.path("M 74 24 Q 44 42 18 70", stroke=lin(74, 24, 18, 70, [(0, "#E8FAFF"), (1, "#FFFFFF00")]), sw=8, cap=CAP_ROUND)
    p.circ(78, 24, 14, ["#F2F6FF", "#8A9AC8"])
    p.circ(74, 20, 3, "#C9D3E8", ol=False)
    p.circ(82, 28, 2.4, "#C9D3E8", ol=False)
    p.sparkle(24, 24, 5)
    return p.out()


def space_3():
    return _animals.head("ALIEN")


def space_4():  # telescopio
    p = Pc()
    with p.s.rotate(-25, 50, 50):
        p.rect(14, 36, 46, 22, ["#8AB0F5", "#3F5FD0"], r=4)
        p.rect(56, 32, 28, 30, ["#C9D3E8", "#7A88A8"], r=4)
        p.rect(80, 28, 10, 38, ["#4A5A8A", "#162036"], r=3)
        p.rect(24, 42, 6, 10, "#FFE08A", r=1.5, ol=False)
    p.line(50, 58, 32, 90, "#8A5A2B", 3.4)
    p.line(50, 58, 68, 90, "#8A5A2B", 3.4)
    p.line(50, 58, 50, 90, "#8A5A2B", 3.4)
    p.sparkle(20, 20, 5)
    return p.out()


def space_5():  # sol
    p = Pc(shadow=False)
    for a in range(0, 360, 30):
        p.poly([(50 + math.cos(math.radians(a - 8)) * 30, 50 + math.sin(math.radians(a - 8)) * 30), (50 + math.cos(math.radians(a)) * 46, 50 + math.sin(math.radians(a)) * 46), (50 + math.cos(math.radians(a + 8)) * 30, 50 + math.sin(math.radians(a + 8)) * 30)], ["#FFE86B", "#F29A1F"], sw=1)
    p.circ(50, 50, 28, ["#FFF1A8", "#FFB02B"])
    p.shine(40, 38, 8, 4, -35, .6)
    return p.out()


def space_6():  # luna llena
    p = Pc(shadow=False)
    p.circ(50, 50, 38, ["#F5F5FF", "#B8C0E0"])
    for x, y, r in ((36, 36, 8), (62, 46, 10), (44, 64, 7), (70, 70, 5), (30, 56, 4)):
        p.circ(x, y, r, ["#D3D8F0", "#9AA4CC"], sw=1)
    p.shine(34, 30, 8, 4, -35, .7)
    return p.out()


def space_7():  # ovni
    p = Pc(shadow=False)
    p.path("M 28 46 C 28 18 72 18 72 46 Z", ["#BFEFFF", "#4F9FD0"])
    p.ell(50, 54, 44, 14, ["#C9D3E8", "#7A88A8"])
    for x in (22, 38, 62, 78):
        p.circ(x, 56, 3.4, "#FFE08A", ol=False)
    p.circ(50, 62, 3.4, "#FFE08A", ol=False)
    p.s.path("M 36 66 L 22 94 H 78 L 64 66 Z", fill=lin(0, 66, 0, 94, [(0, "#FFF1A860"), (1, "#FFF1A800")]))
    p.circ(50, 34, 6, ["#9BE79A", "#3E8A5A"], sw=1)
    return p.out()


def space_8():  # astronauta
    p = Pc()
    p.circ(50, 40, 30, ["#FFFFFF", "#C9D3E8"])
    p.rect(26, 26, 48, 32, ["#4A5A8A", "#162036"], r=14)
    p.shine(38, 34, 8, 3, -25, .6)
    p.circ(50, 46, 4, "#8AB0F5", ol=False)
    p.rect(30, 66, 40, 26, ["#FFFFFF", "#C9D3E8"], r=8)
    p.rect(38, 74, 10, 8, "#E8586D", r=2)
    p.rect(52, 74, 10, 8, "#8AD0F5", r=2)
    return p.out()


def space_9():  # planeta azul
    p = Pc(shadow=False)
    p.circ(50, 50, 38, ["#7FC8F5", "#1E4AA0"])
    p.path("M 22 34 Q 38 22 52 32 Q 46 46 32 44 Q 22 46 22 34 Z", "#6FCF7A", ol=False)
    p.path("M 54 56 Q 74 44 82 60 Q 74 76 58 72 Z", "#6FCF7A", ol=False)
    p.path("M 30 70 Q 40 62 48 72 Q 40 82 30 70 Z", "#6FCF7A", ol=False)
    p.s.ellipse(50, 52, 44, 8, stroke="#FFFFFF", sw=2, op=.35)
    p.shine(36, 32, 8, 4, -35, .55)
    return p.out()


def space_10():  # agujero negro
    p = Pc(shadow=False)
    p.s.ellipse(50, 50, 44, 14, stroke=lin(6, 50, 94, 50, [(0, "#FF8AB8"), (0.5, "#FFE08A"), (1, "#8A6FE0")]), sw=7)
    p.circ(50, 50, 22, ["#2B1B4A", "#050510"])
    p.s.circle(50, 50, 24, stroke="#B49CF5", sw=1.6, op=.7)
    p.s.path("M 10 56 Q 50 74 90 56", stroke=lin(10, 56, 90, 56, [(0, "#FF8AB8"), (0.5, "#FFE08A"), (1, "#8A6FE0")]), sw=6, cap=CAP_ROUND)
    p.sparkle(16, 22, 5)
    p.sparkle(84, 78, 4)
    return p.out()


def build():
    icons = {}
    for pre, ser in (("jungle", "jungle"), ("arctic", "arctic"), ("desert", "desert"), ("space", "space")):
        for i in range(1, 11):
            icons["piece.%s_%d" % (ser, i)] = globals()["%s_%d" % (pre, i)]()
    return {"icons": icons}
