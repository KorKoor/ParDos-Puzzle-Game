import os
import sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from _pk import *  # noqa: F401,F403
import _animals

WH = ["#FFFFFF", "#C9D3E8"]
SKIN = ["#FFD3A8", "#E8A06B"]


def wheel(p, x, y, r=9):
    p.circ(x, y, r, ["#4A4A68", "#1E1E32"])
    p.circ(x, y, r * 0.45, ["#E8E8F4", "#9AA8C8"], ol=False)


# ------------------------------------------------------------------ Transportes
def transp_1():  # auto
    p = Pc()
    p.path("M 6 66 V 54 C 6 48 12 46 18 46 L 30 28 H 66 L 80 46 C 90 46 94 52 94 58 V 66 Z", ["#FF8A7A", "#C83A2B"])
    p.path("M 34 32 H 48 V 46 H 24 Z", ["#BFEFFF", "#4F9FD0"], ol=False)
    p.path("M 52 32 H 64 L 74 46 H 52 Z", ["#BFEFFF", "#4F9FD0"], ol=False)
    wheel(p, 28, 68)
    wheel(p, 72, 68)
    p.circ(88, 54, 3, "#FFE08A", ol=False)
    return p.out()


def transp_2():  # bicicleta
    p = Pc()
    p.s.circle(24, 66, 20, stroke="#4A4A68", sw=4)
    p.s.circle(76, 66, 20, stroke="#4A4A68", sw=4)
    p.s.path("M 24 66 L 40 38 H 64 L 76 66 M 40 38 L 50 66 H 24 M 50 66 L 64 38", stroke="#4F8FE0", sw=4, join=JOIN_ROUND, cap=CAP_ROUND)
    p.line(34, 32, 46, 32, "#2B2B45", 5)
    p.line(64, 38, 70, 24, "#2B2B45", 3.4)
    p.line(66, 24, 76, 24, "#2B2B45", 3.4)
    p.circ(50, 66, 5, "#FFE08A")
    return p.out()


def transp_3():  # autobus
    p = Pc()
    p.rect(8, 18, 84, 62, ["#FFE08A", "#E8A91F"], r=10)
    p.rect(14, 26, 72, 24, ["#BFEFFF", "#4F9FD0"], r=4, ol=False)
    for x in (34, 50, 66):
        p.line(x, 26, x, 50, "#E8A91F", 2)
    p.rect(8, 56, 84, 6, ["#E8586D", "#B8203B"], r=0, ol=False)
    wheel(p, 28, 82, 9)
    wheel(p, 72, 82, 9)
    p.circ(16, 68, 3, "#FFFFFF", ol=False)
    p.circ(84, 68, 3, "#FFE86B", ol=False)
    return p.out()


def transp_4():  # taxi
    p = Pc()
    p.path("M 6 66 V 54 C 6 48 12 46 18 46 L 30 30 H 68 L 82 46 C 90 46 94 52 94 58 V 66 Z", ["#FFE08A", "#E8A91F"])
    p.path("M 34 34 H 48 V 46 H 24 Z", ["#BFEFFF", "#4F9FD0"], ol=False)
    p.path("M 52 34 H 64 L 74 46 H 52 Z", ["#BFEFFF", "#4F9FD0"], ol=False)
    p.rect(40, 18, 20, 12, ["#FFFFFF", "#D3DCEF"], r=3)
    p.line(44, 24, 56, 24, "#2B2B45", 2)
    for k in range(5):
        p.rect(10 + k * 6, 56, 3, 3, "#2B2B45", r=0, ol=False)
    wheel(p, 28, 68)
    wheel(p, 72, 68)
    return p.out()


def transp_5():  # tren de vapor
    p = Pc()
    p.rect(8, 40, 56, 30, ["#4A4A68", "#1E1E32"], r=4)
    p.rect(60, 24, 32, 46, ["#E8586D", "#B8203B"], r=4)
    p.rect(66, 30, 20, 18, ["#BFEFFF", "#4F9FD0"], r=3)
    p.rect(14, 24, 14, 20, ["#4A4A68", "#1E1E32"], r=2)
    p.rect(10, 20, 22, 6, ["#8A8AA8", "#4A4A68"], r=2)
    p.rect(4, 70, 90, 6, ["#8A8AA8", "#4A4A68"], r=2)
    wheel(p, 24, 76, 10)
    wheel(p, 48, 76, 10)
    wheel(p, 76, 76, 12)
    for x, y, r in ((20, 12, 6), (30, 4, 7)):
        p.circ(x, y, r, "#FFFFFF", ol=False, sw=0)
    return p.out()


def transp_6():  # helicoptero
    p = Pc()
    p.line(10, 22, 70, 22, "#4A4A68", 3)
    p.line(40, 22, 40, 32, "#4A4A68", 3)
    p.ell(44, 52, 30, 20, ["#FF8A7A", "#C83A2B"])
    p.path("M 22 52 H 8 L 4 40 H 14 Z", ["#FF8A7A", "#C83A2B"])
    p.ell(54, 48, 13, 11, ["#BFEFFF", "#4F9FD0"])
    p.line(22, 78, 70, 78, "#4A4A68", 3)
    p.line(34, 70, 32, 78, "#4A4A68", 3)
    p.line(56, 70, 58, 78, "#4A4A68", 3)
    p.line(6, 34, 14, 46, "#4A4A68", 3)
    return p.out()


def transp_7():  # avion
    p = Pc(shadow=False)
    p.path("M 8 56 C 8 44 30 38 54 40 L 84 20 L 94 24 L 76 46 C 90 48 96 52 92 58 C 88 62 70 62 64 58 L 56 84 L 44 84 L 46 60 C 30 66 8 66 8 56 Z", ["#FFFFFF", "#B8C4DC"])
    p.path("M 54 40 L 84 20 L 94 24 L 76 46 Z", ["#8AB0F5", "#3F5FD0"], ol=False)
    p.path("M 46 60 L 44 84 L 56 84 L 64 58 Z", ["#8AB0F5", "#3F5FD0"], ol=False)
    for x in (20, 28, 36):
        p.circ(x, 52, 2.4, "#4F8FE0", ol=False)
    return p.out()


def transp_8():  # tren bala
    p = Pc()
    p.path("M 4 62 V 46 C 4 34 20 28 40 28 H 92 V 62 Z", ["#FFFFFF", "#C9D3E8"])
    p.path("M 4 62 V 52 H 92 V 62 Z", ["#4F8FE0", "#2A4AA0"], ol=False)
    for x in (30, 46, 62, 78):
        p.rect(x, 34, 10, 10, ["#BFEFFF", "#4F9FD0"], r=2, sw=1)
    p.path("M 8 46 C 10 38 20 34 32 34 V 46 Z", ["#BFEFFF", "#4F9FD0"], ol=False)
    p.rect(2, 66, 96, 6, ["#8A8AA8", "#4A4A68"], r=2)
    p.circ(12, 62, 3, "#FFE86B", ol=False)
    return p.out()


def transp_9():  # scooter
    p = Pc()
    p.path("M 14 76 C 14 60 26 52 42 52 H 66 C 74 52 80 58 80 66 V 76 Z", ["#7FD0F5", "#2B7AC0"])
    p.rect(38, 36, 26, 16, ["#4A4A68", "#1E1E32"], r=6)
    p.path("M 66 52 L 74 18 H 88", ["#8A8AA8", "#4A4A68"], ol=False)
    p.curve("M 66 52 L 74 18 H 88", "#4A4A68", 4)
    p.circ(84, 70, 12, ["#4A4A68", "#1E1E32"])
    p.circ(84, 70, 5, "#FFE08A", ol=False)
    p.circ(24, 76, 10, ["#4A4A68", "#1E1E32"])
    p.circ(24, 76, 4, "#FFE08A", ol=False)
    p.circ(14, 56, 4, "#FFE86B", ol=False)
    return p.out()


def transp_10():  # crucero
    p = Pc()
    p.path("M 4 62 H 96 L 84 84 H 18 Z", ["#E8586D", "#B8203B"])
    p.rect(14, 44, 72, 20, ["#FFFFFF", "#D3DCEF"], r=3)
    p.rect(26, 28, 48, 18, ["#FFFFFF", "#D3DCEF"], r=3)
    for x in range(20, 80, 10):
        p.circ(x, 54, 3, "#4F9FD0", ol=False)
    for x in (34, 46, 58):
        p.circ(x, 37, 3, "#4F9FD0", ol=False)
    p.rect(44, 12, 14, 16, ["#FF8A7A", "#C83A2B"], r=2)
    p.rect(44, 12, 14, 5, "#2B2B45", r=1, ol=False)
    p.curve("M 4 92 Q 16 86 28 92 T 52 92 T 76 92 T 98 90", "#7FC0EB", 3)
    return p.out()


# ------------------------------------------------------------------ Reino encantado
def king_1():  # espada
    p = Pc()
    with p.s.rotate(-40, 50, 50):
        p.path("M 50 2 L 58 14 V 62 H 42 V 14 Z", ["#F2F6FF", "#8A9AC8"])
        p.line(50, 8, 50, 60, "#C9D3E8", 2)
        p.rect(30, 62, 40, 8, ["#FFE08A", "#C9902B"], r=3)
        p.rect(45, 70, 10, 18, ["#8A5A2B", "#4A3220"], r=3)
        p.circ(50, 92, 5, ["#E8586D", "#B8203B"])
    return p.out()


def king_2():  # arco
    p = Pc()
    p.s.path("M 26 8 C 70 28 70 72 26 92", stroke="#8A5A2B", sw=7, cap=CAP_ROUND)
    p.line(26, 8, 26, 92, "#F5F5FF", 1.6)
    p.line(18, 50, 90, 50, "#C9955E", 3.4)
    p.poly([(90, 50), (78, 43), (78, 57)], ["#F2F6FF", "#8A9AC8"], sw=1)
    p.poly([(18, 50), (28, 44), (24, 50), (28, 56)], ["#E8586D", "#B8203B"], sw=1)
    return p.out()


def king_3():  # jinete (casco y caballo)
    p = Pc()
    p.path("M 30 92 C 24 60 26 34 44 22 L 40 6 L 54 16 C 70 14 82 30 82 44 L 90 54 L 78 62 L 66 54 C 64 66 70 80 64 92 Z", ["#FFFFFF", "#B8C4DC"])
    p.path("M 44 22 C 34 30 28 46 30 60 C 40 44 46 34 54 16 Z", ["#8A6FE0", "#4A2FA0"], ol=False)
    p.path("M 50 26 C 58 22 70 26 72 36 L 56 36 Z", ["#E8586D", "#B8203B"])
    p.circ(66, 40, 3, "#2B1B3A", ol=False)
    p.line(56, 36, 74, 36, "#C9902B", 2.4)
    p.star(40, 62, 7, ["#FFE08A", "#C9902B"])
    return p.out()


def king_4():  # campana
    p = Pc()
    p.path("M 50 10 C 74 10 78 36 78 56 L 90 72 H 10 L 22 56 C 22 36 26 10 50 10 Z", ["#FFE08A", "#C9902B"])
    p.circ(50, 80, 8, ["#8A5A2B", "#4A3220"])
    p.line(50, 10, 50, 4, "#8A5A2B", 4)
    p.shine(36, 36, 4, 14, 10, .55)
    p.sparkle(80, 22, 6)
    return p.out()


def king_5():  # castillo
    p = Pc()
    p.rect(30, 32, 40, 58, ["#D9DCEF", "#8A8FB8"], r=2)
    for x, h in ((8, 44), (74, 44)):
        p.rect(x, 90 - h, 18, h, ["#E8EAFA", "#9AA0C8"], r=2)
        p.poly([(x - 2, 90 - h), (x + 9, 90 - h - 18), (x + 20, 90 - h)], ["#E8586D", "#B8203B"], sw=1.2)
    for x in (30, 42, 54, 66):
        p.rect(x, 24, 8, 10, ["#D9DCEF", "#8A8FB8"], r=1, sw=1)
    p.path("M 42 90 V 66 Q 50 54 58 66 V 90 Z", ["#7A4A2B", "#3A2418"])
    p.line(50, 16, 50, 4, "#8A5A2B", 2)
    p.poly([(50, 4), (62, 8), (50, 12)], ["#FFE08A", "#C9902B"], sw=1)
    p.rect(44, 40, 12, 12, ["#BFEFFF", "#4F9FD0"], r=6, sw=1)
    return p.out()


def king_6():  # hada
    p = Pc()
    p.path("M 46 50 C 22 14 6 30 20 48 C 30 58 40 56 46 50 Z", ["#D3F4FF", "#7FC0EB"])
    p.path("M 54 50 C 78 14 94 30 80 48 C 70 58 60 56 54 50 Z", ["#D3F4FF", "#7FC0EB"])
    p.path("M 46 54 C 30 62 28 80 40 84 C 44 76 46 66 48 58 Z", ["#E8FAFF", "#A8DFF8"], ol=False)
    p.path("M 40 62 H 60 L 66 90 H 34 Z", ["#FF8AB8", "#D6336C"])
    p.circ(50, 44, 12, SKIN)
    p.path("M 38 42 Q 40 28 50 30 Q 62 28 62 42 Q 56 34 50 36 Q 44 34 38 42 Z", ["#FFE08A", "#C9902B"], ol=False)
    p.eyes(46, 54, 45, 1.8)
    p.sparkle(78, 22, 6, "#FFE08A")
    p.sparkle(20, 70, 5, "#FFE08A")
    return p.out()


def king_7():  # princesa
    p = Pc()
    p.path("M 50 8 L 58 22 L 70 18 L 66 32 H 34 L 30 18 L 42 22 Z", ["#FFE08A", "#C9902B"])
    p.circ(50, 8, 3, "#FF8AB8", ol=False)
    p.circ(30, 18, 3, "#8AD0F5", ol=False)
    p.circ(70, 18, 3, "#8AD0F5", ol=False)
    p.circ(50, 52, 22, SKIN)
    p.path("M 28 52 Q 26 30 50 32 Q 74 30 72 52 Q 66 40 50 42 Q 34 40 28 52 Z", ["#FFC83D", "#C9902B"], ol=False)
    p.path("M 28 52 Q 24 80 34 90 Q 32 70 36 56 Z", ["#FFC83D", "#C9902B"], ol=False)
    p.path("M 72 52 Q 76 80 66 90 Q 68 70 64 56 Z", ["#FFC83D", "#C9902B"], ol=False)
    p.eyes(42, 58, 54, 2.4)
    p.curve("M 45 64 Q 50 68 55 64", "#C8203B", 1.8)
    p.circ(36, 62, 4, "#FF8AB8", ol=False, sw=0)
    p.circ(64, 62, 4, "#FF8AB8", ol=False, sw=0)
    return p.out()


def king_8():
    return _animals.head("DRAGON")


def king_9():  # corona
    p = Pc()
    p.path("M 10 72 L 6 26 L 30 46 L 50 14 L 70 46 L 94 26 L 90 72 Z", ["#FFE88A", "#C9902B"])
    p.rect(10, 70, 80, 14, ["#FFD36E", "#E8A91F"], r=4)
    for x, c in ((6, "#FF8AB8"), (50, "#8AD0F5"), (94, "#FF8AB8")):
        p.circ(x, 24 if x != 50 else 12, 5, c, sw=1.1)
    for x, c in ((26, "#E8586D"), (50, "#6A9FF0"), (74, "#7FD08A")):
        p.circ(x, 77, 4.4, c, sw=1)
    p.shine(30, 48, 3, 12, 15, .5)
    return p.out()


def king_10():  # principe
    p = Pc()
    p.path("M 28 30 L 36 12 L 44 26 L 50 8 L 56 26 L 64 12 L 72 30 Z", ["#FFE88A", "#C9902B"])
    p.circ(50, 54, 24, SKIN)
    p.path("M 26 52 Q 22 26 50 28 Q 78 26 74 52 Q 70 40 50 40 Q 30 40 26 52 Z", ["#8A5A2B", "#4A3220"], ol=False)
    p.eyes(42, 58, 54, 2.6)
    p.curve("M 44 66 Q 50 71 56 66", "#C8203B", 2)
    p.rect(30, 80, 40, 12, ["#4F8FE0", "#2A4AA0"], r=4)
    p.circ(50, 84, 4, "#FFE08A")
    return p.out()


# ------------------------------------------------------------------ Noche de brujas
def hall_1():
    return _animals.head("PUMPKIN")


def hall_2():
    return _animals.head("GHOST")


def hall_3():
    return _animals.head("BAT")


def hall_4():  # arana
    p = Pc()
    p.line(50, 0, 50, 36, "#C9D3E8", 1.6)
    for s_ in (-1, 1):
        for k, (dx, dy, ex, ey) in enumerate(((22, -14, 38, 6), (28, -4, 44, 22), (28, 8, 44, 38), (22, 18, 36, 52))):
            p.s.path("M %f %f L %f %f L %f %f" % (50 + s_ * 6, 52, 50 + s_ * dx, 52 + dy - 6, 50 + s_ * ex, 52 + dy + (ey - 52 + dy) * 0.5), stroke="#4A3A5A", sw=3, join=JOIN_ROUND, cap=CAP_ROUND)
    p.circ(50, 62, 18, ["#7A5AA0", "#2B1B4A"])
    p.circ(50, 44, 12, ["#7A5AA0", "#2B1B4A"])
    p.eyes(45, 55, 42, 3, "#FF5A5A")
    p.path("M 46 56 L 50 66 L 54 56 Z", ["#FF8A5A", "#C8501B"], ol=False)
    return p.out()


def hall_5():  # telarana
    p = Pc(shadow=False)
    for a in range(0, 360, 45):
        p.line(50, 50, 50 + math.cos(math.radians(a)) * 42, 50 + math.sin(math.radians(a)) * 42, "#E8EEF8", 2)
    for r in (12, 22, 32, 42):
        pts = [(50 + math.cos(math.radians(a)) * r, 50 + math.sin(math.radians(a)) * r) for a in range(0, 360, 45)]
        p.s.poly(pts, stroke="#E8EEF8", sw=1.8, closed=True, join=JOIN_ROUND)
    p.circ(76, 28, 6, ["#7A5AA0", "#2B1B4A"])
    return p.out()


def hall_6():  # calavera
    p = Pc()
    p.path("M 50 8 C 78 8 90 30 82 54 C 80 62 74 64 72 70 V 84 H 28 V 70 C 26 64 20 62 18 54 C 10 30 22 8 50 8 Z", ["#FFFFFF", "#C9D3E8"])
    p.ell(36, 46, 10, 12, "#2B1B3A", ol=False)
    p.ell(64, 46, 10, 12, "#2B1B3A", ol=False)
    p.poly([(50, 56), (45, 66), (55, 66)], "#2B1B3A", ol=False)
    for x in (36, 44, 52, 60):
        p.rect(x - 2, 72, 4, 12, "#FFFFFF", r=1, sw=1)
    p.circ(36, 44, 2.4, "#8AF0B0", ol=False)
    p.circ(64, 44, 2.4, "#8AF0B0", ol=False)
    return p.out()


def hall_7():  # vampiro
    p = Pc()
    p.path("M 14 90 L 18 40 Q 50 6 82 40 L 86 90 Z", ["#4A2A6A", "#1E1030"])
    p.circ(50, 52, 22, ["#F5F0FF", "#C9C0DC"])
    p.path("M 28 50 Q 30 28 50 30 Q 70 28 72 50 Q 62 38 50 40 Q 38 38 28 50 Z", ["#2B1B3A", "#120A1E"], ol=False)
    p.eyes(42, 58, 54, 2.6, "#C8203B")
    p.curve("M 42 66 Q 50 72 58 66", "#C8203B", 2)
    p.poly([(44, 67), (46, 74), (48, 68)], "#FFFFFF", sw=0.8)
    p.poly([(52, 68), (54, 74), (56, 67)], "#FFFFFF", sw=0.8)
    p.poly([(26, 90), (50, 80), (74, 90)], ["#C8203B", "#7A1020"], sw=1)
    return p.out()


def hall_8():  # bruja
    p = Pc()
    p.poly([(50, 4), (74, 46), (26, 46)], ["#6A4FD0", "#2B1B6A"])
    p.rect(10, 44, 80, 10, ["#8A6FE0", "#3A1E8A"], r=5)
    p.rect(34, 36, 32, 7, ["#FFE08A", "#C9902B"], r=2, ol=False)
    p.circ(50, 66, 20, ["#BFF0A8", "#6FBF73"])
    p.eyes(42, 58, 62, 2.6, "#2B1B3A")
    p.path("M 50 66 L 46 74 L 54 72 Z", ["#8FD08A", "#4E9A5A"], sw=1)
    p.curve("M 42 78 Q 50 83 58 78", "#2B1B3A", 2)
    p.circ(34, 74, 3, "#FF8AB8", ol=False, sw=0)
    p.circ(66, 74, 3, "#FF8AB8", ol=False, sw=0)
    return p.out()


def hall_9():  # caldero
    p = Pc()
    p.ell(50, 42, 34, 10, ["#2B2B45", "#12121E"])
    p.path("M 14 44 C 6 70 20 90 50 90 C 80 90 94 70 86 44 C 70 54 30 54 14 44 Z", ["#5A5A78", "#1E1E32"])
    p.ell(50, 42, 28, 6, ["#9AF07A", "#3E9A2B"], ol=False)
    for x, y, r in ((38, 34, 6), (58, 30, 8), (68, 38, 5)):
        p.circ(x, y, r, ["#C9F7A8", "#6FBF3A"], sw=1)
    p.rect(16, 86, 12, 8, "#2B2B45", r=2)
    p.rect(72, 86, 12, 8, "#2B2B45", r=2)
    p.shine(28, 62, 3, 10, 15, .35)
    return p.out()


def hall_10():  # zombi
    p = Pc()
    p.circ(50, 52, 34, ["#B8E8A0", "#5A9A4A"])
    p.path("M 18 40 Q 24 16 50 18 Q 76 16 82 40 Q 70 28 50 30 Q 30 28 18 40 Z", ["#4A3A5A", "#1E1030"], ol=False)
    p.circ(38, 50, 8, "#FFFFFF", sw=1)
    p.circ(64, 48, 6, "#FFFFFF", sw=1)
    p.circ(39, 51, 3, "#2B1B3A", ol=False)
    p.circ(65, 49, 2.4, "#2B1B3A", ol=False)
    p.line(48, 72, 66, 70, "#2B1B3A", 2.6)
    for x in (52, 58, 64):
        p.line(x, 68, x, 74, "#2B1B3A", 1.2)
    p.line(30, 26, 38, 34, "#4A2A2A", 1.6)
    return p.out()


# ------------------------------------------------------------------ Invierno magico
def xmas_1():  # arbol
    p = Pc()
    p.rect(42, 78, 16, 14, ["#8A5A2B", "#4A3220"], r=2)
    for y, w in ((60, 34), (42, 28), (24, 20)):
        p.poly([(50, y - 22), (50 + w, y + 6), (50 - w, y + 6)], ["#6FCF7A", "#1E6A3A"])
    p.star(50, 12, 9, ["#FFF1A8", "#F2A91F"])
    for x, y, c in ((40, 56, "#E8586D"), (62, 62, "#8AD0F5"), (50, 40, "#FFE08A"), (44, 28, "#FF8AB8"), (58, 50, "#E8586D")):
        p.circ(x, y, 4, c, sw=1)
    return p.out()


def xmas_2():  # santa
    p = Pc()
    p.path("M 22 44 Q 50 -4 78 44 Z", ["#FF8A7A", "#C8203B"])
    p.circ(50, 8, 7, "#FFFFFF")
    p.rect(18, 40, 64, 12, ["#FFFFFF", "#D3DCEF"], r=6)
    p.circ(50, 58, 22, SKIN)
    p.path("M 28 62 Q 34 94 50 94 Q 66 94 72 62 Q 62 74 50 72 Q 38 74 28 62 Z", ["#FFFFFF", "#D3DCEF"])
    p.eyes(42, 58, 56, 2.4)
    p.circ(50, 64, 4, ["#FFB8A8", "#E8782B"], sw=1)
    return p.out()


def xmas_3():  # calcetin
    p = Pc()
    p.path("M 34 8 H 66 V 52 L 86 70 C 94 80 84 94 72 90 L 40 68 C 34 64 34 60 34 52 Z", ["#FF8A7A", "#C8203B"])
    p.rect(30, 6, 40, 14, ["#FFFFFF", "#D3DCEF"], r=5)
    p.path("M 72 90 C 84 94 94 80 86 70 L 76 60 L 62 68 Z", ["#FFFFFF", "#D3DCEF"], ol=False)
    p.star(48, 38, 8, ["#FFE08A", "#C9902B"])
    p.circ(50, 14, 3, "#8AD0F5", ol=False)
    return p.out()


def xmas_4():  # bufanda
    p = Pc()
    p.path("M 12 36 C 12 22 88 22 88 36 C 88 52 12 52 12 36 Z", ["#FF8A7A", "#C8203B"])
    p.path("M 62 44 H 82 L 84 90 H 62 Z", ["#FF8A7A", "#C8203B"])
    for y in (30, 40):
        p.line(14, y, 86, y, "#FFFFFF", 4, ) if False else None
    for k in range(4):
        p.rect(62, 52 + k * 10, 22, 4, ["#FFFFFF", "#D3DCEF"], r=0, ol=False)
    for x in range(14, 88, 14):
        p.line(x, 28, x - 4, 50, "#FFFFFF", 3)
    for x in range(64, 84, 5):
        p.line(x, 90, x, 98, "#FF8A7A", 2.4)
    return p.out()


def xmas_5():  # esquiador
    p = Pc()
    p.circ(54, 24, 12, SKIN)
    p.path("M 42 22 A 12 12 0 0 1 66 22 H 42 Z", ["#E8586D", "#B8203B"])
    p.rect(46, 20, 16, 6, ["#8AD0F5", "#3F7FD0"], r=2, ol=False)
    p.path("M 42 38 L 66 38 L 62 64 L 48 64 Z", ["#4F8FE0", "#2A4AA0"])
    p.line(46, 42, 28, 58, "#4F8FE0", 5)
    p.line(64, 42, 78, 58, "#4F8FE0", 5)
    p.line(48, 64, 40, 84, "#4A4A68", 5)
    p.line(60, 64, 68, 84, "#4A4A68", 5)
    p.line(8, 90, 52, 74, "#E8586D", 4)
    p.line(48, 90, 92, 78, "#E8586D", 4)
    p.line(28, 58, 14, 90, "#8A8AA8", 2)
    p.line(78, 58, 90, 90, "#8A8AA8", 2)
    return p.out()


def xmas_6():  # patinaje
    p = Pc()
    p.path("M 28 10 H 54 L 56 50 L 82 62 C 90 66 90 76 82 76 H 22 Q 16 76 18 68 L 24 50 Z", ["#FFFFFF", "#C9D3E8"])
    p.rect(14, 76, 78, 6, ["#C9D3E8", "#7A88A8"], r=2)
    p.s.path("M 10 90 Q 52 84 92 90", stroke="#8AD0F5", sw=3, cap=CAP_ROUND)
    for y in (24, 34, 44):
        p.line(30, y, 50, y, "#8AB0F5", 2)
    p.path("M 28 10 H 54 V 20 H 28 Z", ["#E8586D", "#B8203B"], ol=False)
    p.sparkle(78, 30, 6)
    return p.out()


def xmas_7():  # senora claus
    p = Pc()
    p.path("M 22 44 Q 50 -4 78 44 Z", ["#FF8A7A", "#C8203B"])
    p.circ(50, 8, 7, "#FFFFFF")
    p.rect(18, 40, 64, 12, ["#FFFFFF", "#D3DCEF"], r=6)
    p.circ(50, 60, 21, SKIN)
    p.path("M 28 56 Q 20 74 26 90 Q 30 76 34 62 Z", ["#E8E8F4", "#B8C4DC"], ol=False)
    p.path("M 72 56 Q 80 74 74 90 Q 70 76 66 62 Z", ["#E8E8F4", "#B8C4DC"], ol=False)
    p.eyes(42, 58, 58, 2.4)
    p.curve("M 44 68 Q 50 72 56 68", "#C8203B", 2)
    p.circ(36, 64, 3.4, "#FF8AB8", ol=False, sw=0)
    p.circ(64, 64, 3.4, "#FF8AB8", ol=False, sw=0)
    p.circ(30, 52, 5, "#FFFFFF", sw=1)
    p.leaf(24, 42, 14, 4, -150, "#6FBF73")
    return p.out()


def xmas_8():  # luces de invierno
    p = Pc(shadow=False)
    p.s.path("M 6 30 Q 28 62 50 36 Q 72 10 94 44", stroke="#4A4A68", sw=2.4, cap=CAP_ROUND)
    for x, y, c in ((16, 46, "#FF8A7A"), (34, 54, "#FFE08A"), (52, 38, "#8AD0F5"), (70, 22, "#FF8AB8"), (86, 40, "#7FD08A")):
        p.s.circle(x, y + 4, 14, fill=rad(x, y + 4, 14, [(0, c + "99"), (1, c + "00")]))
        p.line(x, y - 4, x, y + 1, "#4A4A68", 2)
        p.ell(x, y + 8, 6, 8, c)
    return p.out()


def xmas_9():  # leche tibia
    p = Pc()
    p.path("M 22 20 H 72 L 66 84 Q 66 90 58 90 H 36 Q 28 90 28 84 Z", ["#FFFFFF", "#D3DCEF"])
    p.path("M 24 34 H 70 L 66 84 Q 66 90 58 90 H 36 Q 28 90 28 84 Z", ["#FFFFFF", "#F2EBDD"], ol=False)
    p.s.path("M 72 30 C 94 30 94 62 68 62", stroke="#C9D3E8", sw=6, cap=CAP_ROUND)
    p.ell(47, 34, 23, 5, ["#FFF4DC", "#E8D8B0"], ol=False)
    p.heart = None
    p.s.heart(46, 60, 9, fill="#FF8A9A")
    steam = [(36, 10), (50, 6), (62, 10)]
    for x, y in steam:
        p.curve("M %d %d Q %d %d %d %d" % (x, y + 8, x + 6, y + 2, x, y - 6), "#FFFFFF", 2.6, op=.85)
    return p.out()


def xmas_10():  # cabana nevada
    p = Pc()
    p.rect(18, 46, 64, 42, ["#C9955E", "#7A4A2B"], r=3)
    for y in (54, 64, 74, 84):
        p.line(18, y, 82, y, "#7A4A2B", 1.2)
    p.path("M 8 50 L 50 12 L 92 50 Z", ["#FFFFFF", "#B8D0F0"])
    p.path("M 8 50 L 20 50 Q 24 58 30 50 Q 36 56 42 50 L 58 50 Q 64 58 70 50 Q 76 56 80 50 L 92 50 L 50 12 Z", ["#E8F4FF", "#9AB8E0"], ol=False)
    p.rect(40, 62, 20, 26, ["#8A3A2B", "#4A1E18"], r=3)
    p.rect(24, 58, 12, 12, ["#FFF1A8", "#FFB02B"], r=2)
    p.rect(64, 58, 12, 12, ["#FFF1A8", "#FFB02B"], r=2)
    p.rect(66, 12, 10, 22, ["#8A5A2B", "#4A3220"], r=2)
    return p.out()


def build():
    icons = {}
    for pre, ser in (("transp", "transport"), ("king", "kingdom"), ("hall", "halloween"), ("xmas", "christmas")):
        for i in range(1, 11):
            icons["piece.%s_%d" % (ser, i)] = globals()["%s_%d" % (pre, i)]()
    return {"icons": icons}
