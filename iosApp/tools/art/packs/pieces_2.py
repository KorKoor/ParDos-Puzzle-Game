import os
import sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from _pk import *  # noqa: F401,F403
import _animals

CREAM = ["#FFF4DC", "#E8C99A"]
WOOD = ["#C9955E", "#8A5A2B"]


def steam(p, x, y, n=2):
    for k in range(n):
        p.curve("M %f %f Q %f %f %f %f T %f %f" % (x + k * 12, y, x + k * 12 + 5, y - 7, x + k * 12, y - 14, x + k * 12 + 2, y - 22), "#FFFFFF", 3, op=.8)


# ------------------------------------------------------------------ Mesa de te
def tea_1():  # matcha
    p = Pc()
    p.path("M 14 46 H 86 C 86 74 70 88 50 88 C 30 88 14 74 14 46 Z", ["#F2EBDD", "#C9B896"])
    p.ell(50, 46, 36, 8, ["#9ADB7A", "#4E9A3A"])
    p.ell(50, 45, 28, 5, ["#C9F0A8", "#7BC05A"], ol=False)
    p.rect(30, 82, 40, 6, ["#C9B896", "#8A7A5A"], r=3)
    steam(p, 38, 36)
    return p.out()


def tea_2():  # jazmin
    p = Pc()
    p.ell(50, 82, 40, 8, ["#FFFFFF", "#C9D3E8"])
    p.path("M 22 44 H 78 C 78 70 66 80 50 80 C 34 80 22 70 22 44 Z", ["#FFFFFF", "#D3DCEF"])
    p.path("M 76 50 C 94 48 94 70 74 68", ["#E8EDF8", "#B8C4DC"], ol=False)
    p.curve("M 76 50 C 94 48 94 70 74 68", "#9AA8C8", 3)
    p.ell(50, 44, 28, 6, ["#F4B860", "#C9782B"])
    p.flower(50, 42, 14, 6, "#FFFFFF", "#FFD84A", rot=0)
    steam(p, 40, 30, 1)
    return p.out()


def tea_3():  # mochi / dango
    p = Pc()
    p.line(50, 12, 50, 88, "#C9955E", 3.5)
    for y, c in ((26, ["#FFD3E2", "#F08FB0"]), (50, ["#FFFFFF", "#D3DCEF"]), (74, ["#C9F0A8", "#6FBF73"])):
        p.circ(50, y, 15, c)
        p.shine(44, y - 5, 4, 2, -35, .6)
    return p.out()


def tea_4():  # helado
    p = Pc()
    p.poly([(30, 52), (70, 52), (50, 92)], ["#F2C27A", "#B8782B"])
    for a, b in ((38, 62), (46, 56), (54, 62)):
        p.line(a, 56, b, 84 if False else 80, "#B8782B", 1.2)
    p.ell(50, 50, 24, 9, ["#FFE8F0", "#F4A8C4"])
    p.ell(50, 36, 19, 8, ["#FFF4F8", "#F7BCD2"])
    p.ell(50, 24, 13, 7, ["#FFFFFF", "#FAD0E0"])
    p.circ(50, 14, 4, "#E8586D")
    return p.out()


def tea_5():  # galleta
    p = Pc()
    p.circ(50, 52, 36, ["#E8B070", "#B8782B"])
    for x, y in ((38, 38), (58, 34), (64, 54), (44, 58), (30, 58), (52, 72), (70, 68)):
        p.ell(x, y, 5, 3.6, ["#7A4A2B", "#3A2418"], ol=False, rot=x * 3)
    p.shine(34, 32, 8, 3.4, -40, .5)
    return p.out()


def tea_6():  # galleta de la fortuna
    p = Pc()
    p.path("M 12 56 C 12 30 52 24 88 50 C 62 52 52 60 50 72 C 36 74 12 72 12 56 Z", ["#F5CE7A", "#C9902B"])
    p.path("M 50 72 C 52 60 62 52 88 50 C 84 74 62 82 50 72 Z", ["#EDB85A", "#B8782B"])
    p.rect(64, 20, 30, 10, "#FFFFFF", r=1)
    p.line(68, 25, 90, 25, "#E8586D", 1.6)
    return p.out()


def tea_7():  # croissant / pan dulce
    p = Pc()
    for x, y, a, w in ((20, 62, -35, 12), (36, 50, -20, 15), (50, 46, 0, 17), (64, 50, 20, 15), (80, 62, 35, 12)):
        p.ell(x, y, w, 21 - abs(a) * 0.12, ["#F2B866", "#B8782B"], rot=a)
    p.shine(46, 36, 8, 3, 0, .45)
    return p.out()


def tea_8():  # ramen
    p = Pc()
    p.path("M 10 44 H 90 C 90 74 72 90 50 90 C 28 90 10 74 10 44 Z", ["#FF8A7A", "#C83A2B"])
    p.ell(50, 44, 40, 10, ["#F7D9A0", "#E8B060"])
    for k in range(4):
        p.curve("M %d 42 Q %d 36 %d 46 T %d 44" % (22 + k * 14, 28 + k * 14, 34 + k * 14, 44 + k * 14), "#FFF1B8", 3)
    p.ell(36, 42, 9, 6, ["#FFFFFF", "#E8E0D0"])
    p.circ(36, 42, 3.4, "#FFB02B", ol=False)
    p.rect(58, 32, 14, 10, ["#2E5A3A", "#163A22"], r=2)
    p.line(66, 8, 82, 38, "#C9955E", 3)
    p.line(74, 6, 88, 36, "#C9955E", 3)
    return p.out()


def tea_9():  # pastel
    p = Pc()
    p.poly([(16, 56), (84, 40), (84, 80), (16, 84)], ["#FFE8F0", "#F4A8C4"])
    p.poly([(16, 56), (84, 40), (88, 32), (22, 46)], ["#FFFFFF", "#FAD0E0"])
    p.line(16, 70, 84, 62, "#F8E0A0", 5)
    p.circ(36, 42, 10, ["#FF8A9A", "#D6402B"])
    p.leaf(36, 33, 10, 3, -80, "#4E9A5A")
    return p.out()


def tea_10():  # miel
    p = Pc()
    p.path("M 24 40 C 12 52 14 84 30 88 H 70 C 86 84 88 52 76 40 Z", ["#FFD36E", "#C9902B"])
    p.rect(26, 28, 48, 14, ["#C9955E", "#8A5A2B"], r=4)
    p.rect(36, 52, 28, 22, "#FFF4DC", r=5)
    p.ell(50, 63, 6, 6, ["#FFD36E", "#C9902B"], ol=False)
    p.path("M 44 28 Q 50 14 56 28 Z", "#FFD36E")
    p.shine(30, 56, 3, 9, 10, .5)
    return p.out()


# ------------------------------------------------------------------ Aventura
def adv_1():  # bota
    p = Pc()
    p.path("M 28 14 H 54 L 56 52 L 86 66 Q 92 74 86 82 H 20 Q 14 78 16 70 L 22 52 Z", ["#C9955E", "#7A4A2B"])
    p.rect(14, 80, 78, 9, ["#4A3A3A", "#241818"], r=3)
    p.path("M 28 14 H 54 L 54 24 H 28 Z", ["#E8C27A", "#B8782B"])
    for y in (34, 44, 54):
        p.line(32, y, 50, y, "#FFF4DC", 2.2)
    return p.out()


def adv_2():  # velero
    p = Pc()
    p.path("M 14 70 H 86 Q 78 88 62 88 H 38 Q 22 88 14 70 Z", ["#C9955E", "#7A4A2B"])
    p.line(50, 14, 50, 70, "#5A3A26", 3)
    p.path("M 50 16 Q 78 36 76 62 H 50 Z", ["#FFFFFF", "#C9D3E8"])
    p.path("M 46 24 Q 24 42 24 62 H 46 Z", ["#FFB8C4", "#E8586D"])
    p.poly([(50, 14), (62, 18), (50, 22)], ["#FF8A7A", "#D6402B"])
    p.curve("M 8 90 Q 20 84 32 90 T 56 90 T 80 90 T 96 88", "#7FC0EB", 3)
    return p.out()


def adv_3():  # kayak
    p = Pc(shadow=False)
    p.path("M 6 58 Q 50 38 94 58 Q 50 74 6 58 Z", ["#FFB86B", "#E8642B"])
    p.ell(50, 54, 12, 5, ["#2B3A5A", "#162036"])
    p.line(22, 36, 78, 76, "#C9955E", 3.4)
    p.ell(20, 34, 10, 5, ["#FFFFFF", "#C9D3E8"], rot=40)
    p.ell(80, 78, 10, 5, ["#FFFFFF", "#C9D3E8"], rot=40)
    p.curve("M 6 82 Q 20 76 34 82 T 62 82 T 90 82", "#7FC0EB", 3.4)
    return p.out()


def adv_4():  # ancla
    p = Pc()
    p.curve("M 50 24 V 82", "#4A6A8A", 5)
    p.s.circle(50, 16, 8, stroke="#4A6A8A", sw=4.4)
    p.curve("M 36 38 H 64", "#4A6A8A", 5)
    p.s.path("M 14 62 Q 20 90 50 90 Q 80 90 86 62", stroke="#4A6A8A", sw=5, cap=CAP_ROUND)
    p.path("M 6 58 L 20 58 L 14 72 Z", ["#8AB0D0", "#4A6A8A"])
    p.path("M 94 58 L 80 58 L 86 72 Z", ["#8AB0D0", "#4A6A8A"])
    p.circ(50, 16, 3, "#FFFFFF", ol=False)
    return p.out()


def adv_5():  # montana
    p = Pc()
    p.poly([(4, 86), (36, 28), (62, 70), (72, 56), (96, 86)], ["#9AA8C8", "#4A5A8A"])
    p.poly([(36, 28), (46, 46), (40, 42), (36, 50), (30, 42), (26, 46)], ["#FFFFFF", "#D3E4F8"])
    p.poly([(72, 56), (80, 68), (74, 64), (70, 70), (66, 64)], ["#FFFFFF", "#D3E4F8"], sw=1)
    p.line(36, 28, 36, 12, "#5A3A26", 2)
    p.poly([(36, 12), (52, 17), (36, 22)], ["#FF8A7A", "#D6402B"], sw=1)
    return p.out()


def adv_6():  # ola
    p = Pc()
    p.path("M 6 78 C 6 40 34 14 66 18 C 56 28 62 42 76 44 C 86 46 92 56 94 78 Z", ["#7FD0F5", "#2B7AC0"])
    p.path("M 28 40 C 38 22 54 20 62 24 C 52 28 46 36 48 44 C 40 40 34 40 28 40 Z", ["#FFFFFF", "#CFE8FA"], ol=False)
    p.curve("M 10 82 Q 30 72 50 82 T 90 82", "#FFFFFF", 3, op=.9)
    p.circ(76, 28, 3, "#FFFFFF", ol=False)
    p.circ(84, 38, 2, "#FFFFFF", ol=False)
    return p.out()


def adv_7():  # brujula
    p = Pc()
    p.circ(50, 50, 40, ["#E8C27A", "#8A5A2B"])
    p.circ(50, 50, 32, ["#FFF8E6", "#E8D8B0"])
    for a in range(0, 360, 45):
        p.line(50 + math.cos(math.radians(a)) * 28, 50 + math.sin(math.radians(a)) * 28, 50 + math.cos(math.radians(a)) * 32, 50 + math.sin(math.radians(a)) * 32, "#8A5A2B", 1.6)
    with p.s.rotate(35, 50, 50):
        p.poly([(50, 18), (58, 50), (42, 50)], ["#FF8A7A", "#C83A2B"], sw=1.2)
        p.poly([(50, 82), (58, 50), (42, 50)], ["#FFFFFF", "#9AA8C8"], sw=1.2)
    p.circ(50, 50, 4, "#FFD36E")
    return p.out()


def adv_8():  # mapa
    p = Pc()
    p.poly([(10, 24), (36, 18), (62, 26), (90, 18), (90, 78), (62, 86), (36, 78), (10, 86)], ["#F5E3B8", "#D6B070"])
    p.line(36, 18, 36, 78, "#B8903B", 1.2)
    p.line(62, 26, 62, 86, "#B8903B", 1.2)
    p.curve("M 20 70 Q 30 40 50 56 T 78 36", "#E8586D", 2.4)
    p.line(70, 28, 84, 42, "#C83A2B", 4)
    p.line(84, 28, 70, 42, "#C83A2B", 4)
    p.ell(26, 38, 9, 6, "#9ADB7A", ol=False)
    return p.out()


def adv_9():  # campamento
    p = Pc()
    p.poly([(50, 14), (92, 84), (8, 84)], ["#FF9A5A", "#C8501B"])
    p.poly([(50, 14), (66, 84), (34, 84)], ["#FFC27A", "#E8782B"], sw=1.2)
    p.poly([(50, 44), (60, 84), (40, 84)], ["#4A2A2A", "#1E1010"], ol=False)
    p.path("M 72 90 Q 66 78 76 66 Q 78 78 86 72 Q 90 84 82 90 Z", ["#FFE86B", "#E8502B"])
    return p.out()


def adv_10():  # mochila
    p = Pc()
    p.path("M 24 36 Q 24 12 50 12 Q 76 12 76 36 V 82 Q 76 90 68 90 H 32 Q 24 90 24 82 Z", ["#7FD08A", "#3E8A5A"])
    p.rect(32, 52, 36, 26, ["#5AAA6A", "#2E7A4A"], r=5)
    p.rect(40, 62, 20, 5, ["#FFD36E", "#C9902B"], r=2)
    p.path("M 34 18 Q 50 6 66 18", ["#FFD36E", "#C9902B"], ol=False)
    p.curve("M 34 18 Q 50 6 66 18", "#C9902B", 4)
    p.rect(14, 44, 10, 28, ["#5AAA6A", "#2E7A4A"], r=4)
    p.rect(76, 44, 10, 28, ["#5AAA6A", "#2E7A4A"], r=4)
    return p.out()


# ------------------------------------------------------------------ Estudio
def stu_1():  # pincel
    p = Pc()
    with p.s.rotate(-40, 50, 50):
        p.rect(44, 6, 12, 54, ["#E8B070", "#B8782B"], r=5)
        p.rect(42, 54, 16, 12, ["#E8E8F0", "#9AA8C8"], r=2)
        p.path("M 42 66 H 58 Q 64 80 50 94 Q 36 80 42 66 Z", ["#FF8AB8", "#D6336C"])
    p.circ(20, 78, 8, ["#8AD0F5", "#3F7FD0"])
    p.circ(30, 88, 5, ["#FFD84A", "#E8A91F"])
    return p.out()


def stu_2():  # paleta
    p = Pc()
    p.path("M 50 12 C 82 12 94 38 86 58 C 80 72 66 62 62 72 C 58 86 36 92 22 78 C 4 60 10 12 50 12 Z", ["#F2D3A0", "#C9955E"])
    p.circ(66, 70, 6, "#FFFFFF", sw=1.2)
    for x, y, c in ((32, 34, ["#FF8A8A", "#D6402B"]), (52, 26, ["#FFE060", "#E8A91F"]), (72, 36, ["#7FD08A", "#3E8A5A"]), (26, 56, ["#8AB0F5", "#3F5FD0"])):
        p.circ(x, y, 7.5, c)
    return p.out()


def stu_3():  # nota
    p = Pc()
    p.path("M 40 18 L 82 8 V 66 H 74 V 26 L 48 32 V 74 H 40 Z", ["#8A6FE0", "#4A2FA0"])
    p.ell(30, 76, 14, 10, ["#B49CF5", "#6A4FD0"], rot=-20)
    p.ell(66, 68, 14, 10, ["#B49CF5", "#6A4FD0"], rot=-20)
    p.sparkle(82, 34, 5)
    return p.out()


def stu_4():  # dado
    p = Pc()
    p.poly([(50, 8), (88, 28), (50, 48), (12, 28)], ["#FFFFFF", "#E0E8F8"])
    p.poly([(12, 28), (50, 48), (50, 92), (12, 72)], ["#F8F0FF", "#B8A8E0"])
    p.poly([(88, 28), (50, 48), (50, 92), (88, 72)], ["#E8E0F8", "#8A78C8"])
    for x, y in ((50, 28),):
        p.circ(x, y, 4.2, "#4A2FA0", ol=False)
    for x, y in ((24, 44), (38, 56), (24, 60), (38, 72)):
        p.circ(x, y, 3.6, "#4A2FA0", ol=False)
    for x, y in ((62, 56), (76, 44), (62, 76), (76, 64), (69, 60)):
        p.circ(x, y, 3.4, "#4A2FA0", ol=False)
    return p.out()


def stu_5():  # pieza
    p = Pc()
    p.path("M 18 30 H 38 C 30 14 56 14 48 30 H 68 V 48 C 82 40 84 64 68 56 V 78 H 18 Z", ["#FF9A7A", "#D6402B"])
    p.shine(28, 40, 6, 2.6, -40, .5)
    return p.out()


def stu_6():  # peluche
    p = Pc()
    p.circ(26, 30, 11, ["#D9A66B", "#8A5A2B"])
    p.circ(74, 30, 11, ["#D9A66B", "#8A5A2B"])
    p.ell(50, 74, 24, 18, ["#E8B880", "#9A6A38"])
    p.circ(50, 42, 26, ["#E8B880", "#9A6A38"])
    p.ell(50, 52, 12, 9, ["#FFE8C8", "#E8C080"])
    p.eyes(40, 60, 38, 2.8)
    p.ell(50, 48, 4, 3, "#4A2A1E", ol=False)
    p.path("M 62 62 L 86 56 L 86 70 Z", "#E8586D")
    p.path("M 62 62 L 40 56 L 40 70 Z", "#E8586D")
    p.circ(50, 63, 4.5, "#C83A4A")
    return p.out()


def stu_7():  # camara
    p = Pc()
    p.rect(8, 30, 84, 56, ["#6A6A8A", "#2B2B45"], r=9)
    p.rect(20, 20, 24, 14, ["#6A6A8A", "#2B2B45"], r=3)
    p.rect(8, 44, 84, 14, ["#FF8A7A", "#C83A2B"], r=0, ol=False)
    p.circ(50, 58, 22, ["#E8E8F4", "#9AA8C8"])
    p.circ(50, 58, 16, ["#4A5A8A", "#162036"])
    p.circ(50, 58, 8, ["#8AB0F5", "#3F5FD0"], ol=False)
    p.shine(44, 52, 4, 2, -40, .7)
    p.circ(80, 42, 3, "#FFE86B", ol=False)
    return p.out()


def stu_8():  # piano
    p = Pc()
    p.rect(8, 22, 84, 64, ["#4A4A68", "#1E1E32"], r=6)
    p.rect(14, 28, 72, 12, ["#2B2B45", "#12121E"], r=3, ol=False)
    for k in range(7):
        p.rect(15 + k * 10.2, 44, 9.4, 38, ["#FFFFFF", "#D3DCEF"], r=2, sw=1)
    for k in (0, 1, 3, 4, 5):
        p.rect(22 + k * 10.2, 44, 6.4, 22, ["#3A3A55", "#12121E"], r=2, sw=1)
    return p.out()


def stu_9():  # claqueta
    p = Pc()
    p.rect(10, 38, 80, 48, ["#4A4A68", "#1E1E32"], r=5)
    p.rect(10, 24, 80, 14, ["#F2F2F8", "#9AA8C8"], r=3)
    for k in range(5):
        p.poly([(14 + k * 16, 38), (24 + k * 16, 24), (32 + k * 16, 24), (22 + k * 16, 38)], ["#2B2B45", "#12121E"], ol=False)
    p.line(20, 52, 80, 52, "#FFFFFF", 2.4)
    p.line(20, 64, 66, 64, "#8A8AA8", 2.4)
    p.line(20, 74, 54, 74, "#8A8AA8", 2.4)
    return p.out()


def stu_10():  # microfono de oro
    p = Pc()
    p.rect(40, 50, 20, 36, ["#8A6A3B", "#4A3A22"], r=6)
    p.circ(50, 34, 22, ["#FFE08A", "#C9902B"])
    for y in (26, 34, 42):
        p.line(34, y, 66, y, "#C9902B", 1.6)
    p.line(50, 12, 50, 56, "#C9902B", 1.6)
    p.shine(42, 26, 5, 2.6, -40, .65)
    p.sparkle(78, 20, 6, "#FFE08A")
    return p.out()


# ------------------------------------------------------------------ Tesoros
def tre_1():  # alcancia
    p = Pc()
    p.ell(50, 56, 38, 28, ["#FFB8D0", "#E8586D"])
    p.poly([(28, 34), (34, 18), (44, 32)], ["#FFB8D0", "#E8586D"])
    p.ell(86, 52, 8, 11, ["#FF9AB8", "#D6336C"])
    p.rect(40, 30, 22, 5, ["#8A2A3A", "#4A1020"], r=2)
    p.circ(70, 48, 3, "#2B1B3A", ol=False)
    p.rect(30, 78, 9, 12, ["#FFB8D0", "#E8586D"], r=3)
    p.rect(60, 78, 9, 12, ["#FFB8D0", "#E8586D"], r=3)
    p.circ(48, 14, 9, ["#FFE08A", "#C9902B"])
    return p.out()


def tre_2():  # bolsa de oro
    p = Pc()
    p.path("M 34 24 Q 50 34 66 24 L 74 40 C 94 60 90 90 50 90 C 10 90 6 60 26 40 Z", ["#E8C27A", "#8A5A2B"])
    p.path("M 34 24 L 28 10 Q 50 18 72 10 L 66 24 Q 50 32 34 24 Z", ["#C9955E", "#7A4A2B"])
    p.line(34, 28, 66, 28, "#8A2A3A", 3)
    p.circ(50, 62, 16, ["#FFE08A", "#C9902B"])
    p.line(50, 52, 50, 72, "#8A5A2B", 3)
    p.line(44, 56, 56, 56, "#8A5A2B", 2.4)
    p.line(44, 68, 56, 68, "#8A5A2B", 2.4)
    return p.out()


def tre_3():  # llave
    p = Pc()
    with p.s.rotate(-35, 50, 50):
        p.circ(28, 50, 17, ["#FFE08A", "#C9902B"])
        p.circ(28, 50, 7, "#FFF8E0", ol=True)
        p.rect(42, 46, 48, 9, ["#FFE08A", "#C9902B"], r=3)
        p.rect(70, 54, 8, 14, ["#FFE08A", "#C9902B"], r=2)
        p.rect(82, 54, 8, 10, ["#FFE08A", "#C9902B"], r=2)
    p.sparkle(78, 22, 6)
    return p.out()


def tre_4():  # escudo
    p = Pc()
    p.path("M 50 8 L 86 20 V 52 C 86 74 66 86 50 92 C 34 86 14 74 14 52 V 20 Z", ["#8AB0F5", "#3F5FD0"])
    p.path("M 50 16 L 78 26 V 52 C 78 68 62 78 50 83 C 38 78 22 68 22 52 V 26 Z", ["#F2F6FF", "#B8C8F0"], ol=False)
    p.path("M 50 16 L 78 26 V 52 C 78 68 62 78 50 83 Z", ["#E8586D", "#B8203B"], ol=False)
    p.star(50, 48, 14, ["#FFE08A", "#C9902B"])
    return p.out()


def tre_5():  # trofeo
    p = Pc()
    p.path("M 24 14 H 76 V 40 C 76 58 62 64 50 64 C 38 64 24 58 24 40 Z", ["#FFE08A", "#C9902B"])
    p.s.path("M 24 22 C 6 22 6 48 28 52", stroke="#C9902B", sw=4, cap=CAP_ROUND)
    p.s.path("M 76 22 C 94 22 94 48 72 52", stroke="#C9902B", sw=4, cap=CAP_ROUND)
    p.rect(44, 62, 12, 14, ["#FFE08A", "#C9902B"], r=2)
    p.rect(30, 76, 40, 12, ["#8A6A3B", "#4A3A22"], r=3)
    p.star(50, 38, 11, "#FFFFFF")
    p.shine(36, 26, 4, 8, 15, .5)
    return p.out()


def tre_6():  # medalla
    p = Pc()
    p.poly([(28, 6), (46, 6), (60, 44), (42, 44)], ["#E8586D", "#B8203B"], sw=1.2)
    p.poly([(72, 6), (54, 6), (40, 44), (58, 44)], ["#4F8FE0", "#2A4AA0"], sw=1.2)
    p.circ(50, 64, 26, ["#FFE08A", "#C9902B"])
    p.circ(50, 64, 19, ["#FFD36E", "#E8A91F"], sw=1.1)
    p.star(50, 64, 13, "#FFFFFF")
    return p.out()


def tre_7():  # estrella fugaz
    p = Pc(shadow=False)
    p.star(50, 52, 34, ["#FFF1A8", "#F2A91F"])
    p.shine(40, 40, 7, 3, -45, .6)
    p.sparkle(14, 20, 6)
    p.sparkle(86, 24, 5)
    p.sparkle(82, 82, 4)
    return p.out()


def tre_8():  # diamante
    p = Pc()
    p.poly([(26, 14), (74, 14), (94, 38), (50, 90), (6, 38)], ["#9BE7FF", "#3F7FD0"])
    p.poly([(6, 38), (94, 38), (50, 90)], ["#7BCFF5", "#2A5AB0"], ol=False)
    p.poly([(26, 14), (38, 38), (6, 38)], ["#D3F4FF", "#8AD0F5"], ol=False)
    p.poly([(74, 14), (62, 38), (94, 38)], ["#BFEFFF", "#7FC0EB"], ol=False)
    p.poly([(26, 14), (74, 14), (62, 38), (38, 38)], ["#E8FAFF", "#A8DFF8"], ol=False)
    p.s.path("M 26 14 L 74 14 L 94 38 L 50 90 L 6 38 Z", stroke=dk("#3F7FD0", .3), sw=1.5, join=JOIN_ROUND)
    p.sparkle(24, 18, 5)
    return p.out()


def tre_9():  # anillo
    p = Pc()
    p.s.circle(50, 62, 26, stroke=lin(24, 36, 76, 88, [(0, "#FFF1A8"), (1, "#C9902B")]), sw=9)
    p.poly([(40, 18), (60, 18), (68, 30), (50, 42), (32, 30)], ["#FFC0E0", "#D6336C"])
    p.poly([(32, 30), (68, 30), (50, 42)], ["#FF8AB8", "#B8203B"], ol=False)
    p.sparkle(72, 14, 6)
    return p.out()


def tre_10():  # llave maestra
    p = Pc()
    with p.s.rotate(-40, 50, 50):
        p.path("M 20 50 C 20 36 30 28 40 28 C 50 28 56 36 56 44 C 56 56 48 64 38 64 C 28 64 20 58 20 50 Z", ["#B49CF5", "#4A2FA0"])
        p.circ(38, 46, 7, "#FFF8E0")
        p.rect(52, 42, 44, 8, ["#B49CF5", "#4A2FA0"], r=3)
        p.rect(78, 50, 7, 12, ["#B49CF5", "#4A2FA0"], r=2)
        p.rect(88, 50, 7, 9, ["#B49CF5", "#4A2FA0"], r=2)
        p.circ(60, 46, 3.4, "#FFE08A", ol=False)
    p.sparkle(80, 22, 6)
    p.sparkle(18, 78, 5)
    return p.out()


def build():
    icons = {}
    for pre, ser in (("tea", "tea"), ("adv", "adventure"), ("stu", "studio"), ("tre", "treasure")):
        for i in range(1, 11):
            icons["piece.%s_%d" % (ser, i)] = globals()["%s_%d" % (pre, i)]()
    return {"icons": icons}
