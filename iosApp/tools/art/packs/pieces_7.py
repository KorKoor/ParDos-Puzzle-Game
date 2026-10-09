import os
import sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from _pk import *  # noqa: F401,F403
import _animals

WH = ["#FFFFFF", "#C9D3E8"]


# ------------------------------------------------------------------ Futuro
def future_1():
    return _animals.head("ROBOT")


def future_2():  # laptop
    p = Pc()
    p.rect(16, 20, 68, 44, ["#4A4A68", "#1E1E32"], r=5)
    p.rect(21, 25, 58, 34, ["#8AE0F5", "#2B7AC0"], r=2, ol=False)
    p.path("M 6 70 H 94 L 88 82 Q 86 86 80 86 H 20 Q 14 86 12 82 Z", ["#D3DCEF", "#8A98B8"])
    p.rect(40, 72, 20, 4, ["#8A98B8", "#5A6888"], r=2, ol=False)
    p.path("M 30 52 L 42 38 L 52 46 L 66 30", ["#FFFFFF", "#FFFFFF"], ol=False, op=.0)
    p.curve("M 28 52 L 40 40 L 50 48 L 68 30", "#FFFFFF", 2.6)
    return p.out()


def future_3():  # telefono
    p = Pc()
    p.rect(28, 6, 44, 86, ["#4A4A68", "#1E1E32"], r=9)
    p.rect(33, 16, 34, 62, ["#8AB0F5", "#7A4FD0"], r=3, ol=False)
    p.circ(50, 85, 3.4, "#C9D3E8", ol=False)
    p.rect(43, 10, 14, 3, "#12121E", r=1.5, ol=False)
    p.circ(50, 42, 9, "#FFFFFF", ol=False, sw=0)
    p.sparkle(50, 42, 12, "#FFFFFF")
    return p.out()


def future_4():  # bateria
    p = Pc()
    p.rect(12, 28, 70, 46, ["#E8EEF8", "#9AA8C8"], r=8)
    p.rect(82, 40, 8, 22, ["#9AA8C8", "#5A6888"], r=3)
    p.rect(18, 34, 58, 34, ["#2B2B45", "#12121E"], r=4, ol=False)
    for x, c in ((22, "#7FF0A8"), (36, "#7FF0A8"), (50, "#B8F070"), (64, "#FFE86B")):
        p.rect(x, 38, 11, 26, [c, dk(c, .25)], r=3, sw=1)
    p.poly([(54, 4), (42, 26), (52, 26), (46, 44)], ["#FFE86B", "#F29A1F"], sw=1.2)
    return p.out()


def future_5():  # antena parabolica
    p = Pc()
    p.path("M 14 28 C 14 60 40 74 70 66 L 62 40 C 50 38 36 30 30 14 C 22 14 14 20 14 28 Z", ["#E8EEF8", "#8A98B8"])
    p.line(46, 50, 76, 20, "#5A6888", 3)
    p.circ(78, 18, 5, "#FF8A7A")
    p.rect(40, 66, 16, 22, ["#8A98B8", "#4A5878"], r=3)
    p.rect(26, 84, 44, 8, ["#8A98B8", "#4A5878"], r=3)
    p.sparkle(84, 40, 5, "#8AD0F5")
    return p.out()


def future_6():  # videojuego
    p = Pc()
    p.path("M 14 36 C 6 40 4 70 12 76 C 20 82 28 70 36 66 H 64 C 72 70 80 82 88 76 C 96 70 94 40 86 36 C 78 30 22 30 14 36 Z", ["#8A6FE0", "#3A1E8A"])
    p.rect(22, 44, 20, 6, ["#E8EEF8", "#9AA8C8"], r=2, ol=False)
    p.rect(29, 37, 6, 20, ["#E8EEF8", "#9AA8C8"], r=2, ol=False)
    p.circ(66, 52, 5, "#FF8A7A", sw=1)
    p.circ(78, 46, 5, "#FFE08A", sw=1)
    p.circ(72, 58, 5, "#8AF0B0", sw=1)
    p.circ(56, 56, 3, "#C9D3E8", ol=False)
    return p.out()


def future_7():  # arcade (joystick)
    p = Pc()
    p.path("M 8 64 H 92 L 86 88 H 14 Z", ["#8A6FE0", "#3A1E8A"])
    p.line(36, 64, 36, 36, "#C9D3E8", 5)
    p.circ(36, 30, 12, ["#FF8A7A", "#C8203B"])
    p.shine(32, 25, 4, 2.4, -35, .65)
    for x, y, c in ((62, 56, "#FFE08A"), (76, 50, "#8AD0F5"), (74, 66, "#8AF0B0")):
        p.circ(x, y, 6.5, c, sw=1.2)
    p.circ(36, 64, 8, ["#4A4A68", "#1E1E32"])
    return p.out()


def future_8():  # mente artificial (cerebro)
    p = Pc()
    p.path("M 50 16 C 38 4 14 14 18 34 C 6 42 8 64 22 68 C 22 82 44 90 50 78 C 56 90 78 82 78 68 C 92 64 94 42 82 34 C 86 14 62 4 50 16 Z", ["#FFB8D0", "#E8586D"])
    p.curve("M 50 16 V 78", "#C8203B", 2)
    for d in ("M 50 34 C 40 30 30 36 28 46", "M 50 52 C 40 48 30 54 26 62", "M 50 40 C 60 34 70 38 74 48", "M 50 62 C 60 56 70 62 72 68"):
        p.curve(d, "#C8203B", 2, op=.7)
    for x, y in ((30, 28), (72, 28), (24, 54), (78, 56)):
        p.circ(x, y, 3, "#8AD0F5", ol=False)
    p.shine(34, 26, 6, 3, -30, .55)
    return p.out()


def future_9():  # microscopio
    p = Pc()
    with p.s.rotate(-20, 50, 40):
        p.rect(40, 8, 16, 52, ["#C9D3E8", "#7A88A8"], r=5)
        p.rect(36, 4, 24, 10, ["#4A4A68", "#1E1E32"], r=3)
        p.rect(44, 60, 8, 12, ["#4A4A68", "#1E1E32"], r=2)
    p.path("M 66 22 C 90 34 90 70 56 78", ["#8A98B8", "#4A5878"], ol=False)
    p.curve("M 66 22 C 90 34 90 70 56 78", "#5A6888", 6)
    p.rect(18, 84, 64, 8, ["#8A98B8", "#4A5878"], r=3)
    p.rect(26, 74, 36, 5, ["#E8EEF8", "#9AA8C8"], r=2)
    p.sparkle(24, 24, 5, "#8AD0F5")
    return p.out()


def future_10():  # iman cuantico
    p = Pc()
    p.path("M 14 14 H 40 V 58 C 40 66 60 66 60 58 V 14 H 86 V 58 C 86 82 66 94 50 94 C 34 94 14 82 14 58 Z", ["#FF8A7A", "#C8203B"])
    p.rect(14, 14, 26, 18, ["#FFFFFF", "#C9D3E8"], r=2)
    p.rect(60, 14, 26, 18, ["#FFFFFF", "#C9D3E8"], r=2)
    p.s.path("M 22 6 Q 27 -2 32 6", stroke="#8AD0F5", sw=2.4)
    p.s.path("M 68 6 Q 73 -2 78 6", stroke="#8AD0F5", sw=2.4)
    p.sparkle(50, 38, 7, "#FFE08A")
    return p.out()


# ------------------------------------------------------------------ Dinosaurios
def dino_1():  # huevo fosil
    p = Pc()
    p.path("M 50 8 C 76 8 88 44 82 66 C 78 84 64 92 50 92 C 36 92 22 84 18 66 C 12 44 24 8 50 8 Z", ["#F5F0DC", "#C9B88A"])
    for x, y, r in ((40, 34, 6), (60, 48, 7), (40, 64, 5), (62, 74, 4), (52, 26, 3)):
        p.circ(x, y, r, ["#8AB070", "#4E7A3A"], sw=1)
    p.shine(34, 40, 4, 12, 10, .5)
    return p.out()


def dino_2():  # hueso
    p = Pc()
    with p.s.rotate(-35, 50, 50):
        p.rect(24, 42, 52, 16, ["#FFFFFF", "#D9D0BC"], r=4)
        for x, y in ((22, 40), (22, 60), (78, 40), (78, 60)):
            p.circ(x, y, 9, ["#FFFFFF", "#D9D0BC"])
    return p.out()


def dino_3():  # helecho
    p = Pc()
    p.curve("M 50 92 Q 48 56 66 14", "#4E9A5A", 3.4)
    for k in range(9):
        t = k / 8.0
        x = 50 + (66 - 50) * t * 0.9 - math.sin(t * 3) * 3
        y = 88 - t * 72
        ln = 24 * (1 - t * 0.7)
        for s_ in (-1, 1):
            with p.s.rotate(s_ * -40 - 10, x, y):
                p.path("M %f %f Q %f %f %f %f Q %f %f %f %f Z" % (x, y, x + s_ * ln * .5, y - 5, x + s_ * ln, y, x + s_ * ln * .5, y + 2, x, y), ["#9ADD92", "#3E8A5A"], sw=1)
    return p.out()


def dino_4():  # volcan
    p = Pc()
    p.path("M 6 90 L 36 36 L 64 36 L 94 90 Z", ["#B8886A", "#5A3A2B"])
    p.path("M 36 36 Q 50 28 64 36 Q 56 44 50 40 Q 44 46 36 36 Z", ["#FF9A4A", "#D6402B"], ol=False)
    p.path("M 46 40 Q 44 60 38 80 L 52 80 Q 54 60 50 42 Z", ["#FF9A4A", "#D6402B"], ol=False)
    p.circ(40, 22, 9, ["#FFE86B", "#F29A1F"], ol=False)
    p.circ(58, 16, 7, ["#FF9A4A", "#D6402B"], ol=False)
    p.circ(48, 8, 4, "#FFE86B", ol=False)
    return p.out()


def dino_5():  # diplodocus
    p = Pc()
    p.path("M 24 70 C 16 52 30 44 46 46 C 54 20 64 12 76 14 C 90 14 92 28 82 30 C 74 32 72 44 76 56 C 80 74 72 82 58 82 L 28 82 C 22 82 24 76 24 70 Z", ["#9ADD92", "#3E8A5A"])
    p.path("M 24 70 C 10 78 8 66 6 60 C 14 62 20 62 24 66 Z", ["#7FD08A", "#2E7A4A"])
    for x in (36, 66):
        p.rect(x - 5, 76, 10, 14, ["#7FD08A", "#2E7A4A"], r=3)
    p.circ(84, 20, 2.2, "#2B1B3A", ol=False)
    for x, y in ((40, 56), (54, 60), (62, 46)):
        p.circ(x, y, 3.6, "#E8F8A0", ol=False)
    return p.out()


def dino_6():  # cocodrilo
    p = Pc()
    p.path("M 6 70 C 6 52 20 42 36 44 L 60 40 C 76 36 94 44 96 58 C 98 70 80 74 66 72 L 30 78 C 18 80 6 80 6 70 Z", ["#8EE08A", "#2E7A4A"])
    p.path("M 60 62 L 96 58 C 98 70 80 74 66 72 Z", ["#E8F8B0", "#B8D86A"], ol=False)
    for k in range(6):
        p.poly([(18 + k * 9, 42 - (k % 2) * 2), (22 + k * 9, 34 - (k % 2) * 3), (26 + k * 9, 42)], ["#6FBF73", "#2E7A4A"], sw=1)
    p.circ(48, 38, 6, "#FFFFFF", sw=1)
    p.circ(49, 38, 2.6, "#2B1B3A", ol=False)
    for x in (70, 76, 82, 88):
        p.poly([(x, 60), (x + 2, 66), (x + 4, 60)], "#FFFFFF", ol=False)
    return p.out()


def dino_7():
    return _animals.head("DINO", "NORMAL")


def dino_8():  # amonita dorada
    p = Pc()
    p.circ(50, 52, 38, ["#FFE08A", "#C9902B"])
    pts = []
    for k in range(0, 181):
        ang = k * 0.12
        r = 3 + k * 0.17
        pts.append((50 + math.cos(ang) * r, 52 + math.sin(ang) * r))
    p.s.curve(pts[::6], "#8A5A2B", 3.2)
    for a in range(0, 360, 36):
        p.line(50 + math.cos(math.radians(a)) * 32, 52 + math.sin(math.radians(a)) * 32, 50 + math.cos(math.radians(a)) * 38, 52 + math.sin(math.radians(a)) * 38, "#8A5A2B", 2)
    p.sparkle(78, 22, 6)
    return p.out()


def dino_9():  # bosque prehistorico
    p = Pc()
    p.path("M 44 92 Q 40 60 50 40 Q 60 60 56 92 Z", ["#A9733B", "#5A3A26"])
    for a, c in ((-70, "#6FBF73"), (-35, "#4E9A5A"), (0, "#7FD08A"), (35, "#4E9A5A"), (70, "#6FBF73")):
        p.leaf(50, 40, 38, 9, a - 90, c)
    p.circ(46, 46, 4, "#8A5A2B", ol=False)
    p.circ(54, 48, 4, "#8A5A2B", ol=False)
    p.path("M 8 92 Q 20 78 32 92 Z", ["#7FD08A", "#2E7A4A"], ol=False)
    p.path("M 68 92 Q 80 76 92 92 Z", ["#7FD08A", "#2E7A4A"], ol=False)
    return p.out()


def dino_10():  # huella gigante
    p = Pc()
    p.path("M 50 90 C 28 90 24 70 32 54 C 40 44 60 44 68 54 C 76 70 72 90 50 90 Z", ["#B8886A", "#5A3A2B"])
    for x, y, r in ((24, 44, 8), (40, 28, 9), (60, 28, 9), (76, 44, 8)):
        p.ell(x, y, r, r * 1.3, ["#B8886A", "#5A3A2B"], rot=(x - 50) * 0.6)
    p.shine(40, 66, 4, 8, 20, .35)
    return p.out()


# ------------------------------------------------------------------ Bichos
def bugs_1():  # abeja
    p = Pc(shadow=False)
    p.ell(30, 36, 18, 12, ["#E8FAFF", "#A8DFF8"], rot=-30)
    p.ell(60, 30, 18, 12, ["#E8FAFF", "#A8DFF8"], rot=20)
    p.ell(50, 58, 28, 22, ["#FFE86B", "#E8A91F"])
    for x in (42, 54, 66):
        p.path("M %d 38 Q %d 58 %d 78" % (x, x - 4, x), "#2B1B3A", ol=False, op=0)
        p.ell(x, 58, 4, 21, "#2B1B3A", ol=False)
    p.circ(24, 58, 14, ["#FFE86B", "#E8A91F"])
    p.eyes(20, 29, 56, 3)
    p.curve("M 20 66 Q 25 70 30 66", "#2B1B3A", 1.8)
    p.line(78, 58, 90, 62, "#2B1B3A", 2.6)
    p.line(18, 46, 12, 36, "#2B1B3A", 1.6)
    p.line(28, 45, 30, 34, "#2B1B3A", 1.6)
    return p.out()


def bugs_2():  # hormiga
    p = Pc()
    for x in (34, 50, 66):
        p.line(x, 56, x - 8, 78, "#4A2A1E", 2.4)
        p.line(x, 56, x + 8, 78, "#4A2A1E", 2.4)
    p.ell(22, 56, 14, 11, ["#A9602B", "#5A2A1B"])
    p.ell(50, 56, 12, 10, ["#A9602B", "#5A2A1B"])
    p.ell(76, 54, 18, 15, ["#A9602B", "#5A2A1B"])
    p.eyes(16, 26, 52, 2.6)
    p.curve("M 14 46 Q 8 36 14 28", "#4A2A1E", 2)
    p.curve("M 26 46 Q 28 34 22 26", "#4A2A1E", 2)
    p.shine(72, 48, 6, 3, -30, .45)
    return p.out()


def bugs_3():  # oruga
    p = Pc()
    for k, x in enumerate((20, 34, 48, 62)):
        p.circ(x, 62 - (k % 2) * 6, 14, ["#B8F070", "#4E9A2B"])
    p.circ(78, 54, 17, ["#9ADD92", "#3E8A5A"])
    p.eyes(73, 85, 50, 3.2)
    p.curve("M 72 62 Q 78 68 84 62", "#2B1B3A", 2)
    p.curve("M 72 38 Q 70 28 64 26", "#2B1B3A", 2)
    p.curve("M 84 38 Q 86 28 92 26", "#2B1B3A", 2)
    p.circ(64, 26, 3, "#FF8AB8", ol=False)
    p.circ(92, 26, 3, "#FF8AB8", ol=False)
    for x in (20, 34, 48, 62):
        p.line(x, 76, x, 86, "#2E7A4A", 3)
    return p.out()


def bugs_4():  # caracol
    p = Pc()
    p.path("M 8 84 C 8 76 20 74 44 74 H 80 C 88 74 90 62 84 56 L 84 40 M 76 56 L 76 40", ["#E8C99A", "#B8903B"], ol=False)
    p.path("M 8 86 C 8 78 18 76 30 76 H 82 C 92 76 90 62 82 58 C 76 56 74 60 70 62 H 30 C 14 62 8 72 8 86 Z", ["#FFE0A8", "#E8A06B"])
    p.line(82, 58, 86, 44, "#E8A06B", 2.6)
    p.line(76, 60, 74, 46, "#E8A06B", 2.6)
    p.circ(86, 42, 3.4, "#2B1B3A", ol=False)
    p.circ(74, 44, 3.4, "#2B1B3A", ol=False)
    p.circ(46, 44, 28, ["#FFB8D0", "#C8503B"])
    p.s.path("M 46 44 m 0 -3 a 3 3 0 1 1 -2 3 a 8 8 0 1 1 8 8 a 14 14 0 1 1 -14 -14 a 20 20 0 1 1 20 20", stroke="#8A2A3A", sw=2.6, cap=CAP_ROUND)
    return p.out()


def bugs_5():  # mariquita
    p = Pc()
    p.circ(50, 24, 13, ["#4A4A68", "#1E1E32"])
    p.circ(50, 58, 36, ["#FF8A7A", "#C8203B"])
    p.line(50, 24, 50, 94, "#2B1B3A", 2.4)
    for x, y in ((34, 44), (66, 44), (30, 66), (70, 66), (42, 80), (58, 80)):
        p.circ(x, y, 5.4, "#2B1B3A", ol=False)
    p.eyes(45, 55, 22, 2.4, "#FFFFFF")
    p.line(44, 12, 38, 4, "#2B1B3A", 2)
    p.line(56, 12, 62, 4, "#2B1B3A", 2)
    p.shine(36, 40, 6, 3, -35, .55)
    return p.out()


def bugs_6():  # grillo
    p = Pc()
    p.ell(46, 56, 30, 18, ["#9A7A4A", "#4A3220"], rot=-8)
    p.path("M 28 56 C 44 38 70 40 82 56 C 66 52 44 54 28 56 Z", ["#B8F070", "#4E9A2B"], ol=False)
    p.circ(76, 44, 12, ["#B89A6A", "#6A4A2B"])
    p.eyes(74, 82, 42, 2.6)
    p.curve("M 80 34 Q 88 14 96 8", "#4A3220", 1.8)
    p.curve("M 74 34 Q 72 16 78 6", "#4A3220", 1.8)
    p.s.path("M 36 66 L 24 84 L 38 88 M 52 68 L 50 90 L 62 90", stroke="#4A3220", sw=3, cap=CAP_ROUND, join=JOIN_ROUND)
    return p.out()


def bugs_7():  # girasol zumbon
    p = Pc()
    p.line(50, 60, 50, 92, "#4E9A5A", 4)
    p.leaf(50, 80, 22, 6, -30, "#6FBF73")
    p.leaf(50, 72, 20, 5, -150, "#6FBF73")
    for a in range(0, 360, 30):
        with p.s.rotate(a, 50, 40):
            p.ell(50, 14, 5.6, 12, ["#FFE86B", "#E8A91F"], sw=1)
    p.circ(50, 40, 18, ["#8A5A2B", "#4A3220"])
    p.eyes(44, 56, 38, 2.4, "#FFFFFF")
    p.circ(44, 38, 1.2, "#2B1B3A", ol=False)
    p.circ(56, 38, 1.2, "#2B1B3A", ol=False)
    p.curve("M 44 47 Q 50 52 56 47", "#FFE86B", 2)
    return p.out()


def bugs_8():  # escarabajo dorado
    p = Pc()
    p.circ(50, 24, 12, ["#FFE88A", "#C9902B"])
    p.ell(50, 62, 32, 32, ["#FFE88A", "#C9902B"])
    p.line(50, 32, 50, 94, "#8A5A2B", 2.2)
    for s_ in (-1, 1):
        p.s.path("M %f 48 L %f 40 L %f 30" % (50 + s_ * 28, 50 + s_ * 40, 50 + s_ * 44), stroke="#8A5A2B", sw=3, cap=CAP_ROUND)
        p.s.path("M %f 66 L %f 66" % (50 + s_ * 30, 50 + s_ * 44), stroke="#8A5A2B", sw=3, cap=CAP_ROUND)
        p.s.path("M %f 82 L %f 90" % (50 + s_ * 24, 50 + s_ * 38), stroke="#8A5A2B", sw=3, cap=CAP_ROUND)
    p.eyes(45, 55, 22, 2.2, "#2B1B3A")
    p.shine(36, 50, 5, 11, 15, .55)
    p.sparkle(78, 24, 6)
    return p.out()


def bugs_9():  # microbio simpatico
    p = Pc()
    for a in range(0, 360, 30):
        x, y = 50 + math.cos(math.radians(a)) * 36, 52 + math.sin(math.radians(a)) * 36
        p.line(50 + math.cos(math.radians(a)) * 28, 52 + math.sin(math.radians(a)) * 28, x, y, "#7A4FD0", 3)
        p.circ(x, y, 4.4, ["#C9A8F5", "#7A4FD0"], sw=1)
    p.circ(50, 52, 28, ["#C9A8F5", "#7A4FD0"])
    p.eyes(40, 60, 46, 4.2)
    p.curve("M 40 62 Q 50 72 60 62", "#4A2FA0", 2.6)
    p.circ(30, 60, 4, "#FF8AB8", ol=False, sw=0)
    p.circ(70, 60, 4, "#FF8AB8", ol=False, sw=0)
    p.shine(36, 34, 6, 3, -35, .55)
    return p.out()


def bugs_10():  # pradera florida
    p = Pc()
    for x, y, h, pc in ((24, 52, 40, "#FF8AB8"), (50, 40, 52, "#FFE86B"), (76, 50, 42, "#8AB0F5"), (38, 62, 30, "#FFFFFF"), (64, 64, 28, "#FF8A7A")):
        p.line(x, 92, x, y + 6, "#4E9A5A", 3)
        p.flower(x, y, 20, 5, pc, "#FFD84A" if pc != "#FFE86B" else "#FF8A7A", rot=-90)
    p.leaf(50, 92, 20, 5, -150, "#6FBF73")
    p.leaf(50, 92, 20, 5, -30, "#6FBF73")
    return p.out()


# ------------------------------------------------------------------ Aves
def birds_1():
    return _animals.head("CHICK")


def birds_2():
    return _animals.head("DUCK")


def birds_3():  # paloma
    p = Pc()
    p.path("M 22 60 C 14 40 34 26 54 34 C 72 40 82 58 76 70 C 66 84 36 84 22 60 Z", ["#FFFFFF", "#B8C4DC"])
    p.path("M 40 46 C 28 20 58 8 74 26 C 62 30 52 38 46 50 Z", ["#F2F6FF", "#8A9AC8"])
    p.circ(70, 38, 12, ["#FFFFFF", "#B8C4DC"])
    p.path("M 80 38 L 94 42 L 80 46 Z", ["#FFB86B", "#E8642B"])
    p.circ(72, 35, 2.2, "#2B1B3A", ol=False)
    p.path("M 22 60 L 8 72 L 24 70 Z", ["#B8C4DC", "#6A7A9A"])
    p.leaf(86, 80, 18, 5, -130, "#6FBF73")
    return p.out()


def birds_4():  # pavo
    p = Pc()
    for a in range(-60, 61, 20):
        with p.s.rotate(a, 50, 70):
            p.ell(50, 26, 9, 24, ["#E8A06B", "#8A4A2B"] if a % 40 == 0 else ["#FFB86B", "#B8502B"], sw=1.2)
    p.ell(50, 72, 22, 20, ["#A9733B", "#5A3A26"])
    p.circ(50, 52, 12, ["#A9733B", "#5A3A26"])
    p.poly([(50, 54), (58, 58), (50, 60)], ["#FFB86B", "#E8642B"], sw=1)
    p.path("M 50 60 Q 56 70 50 72 Q 44 70 50 60 Z", ["#FF8A7A", "#C8203B"], ol=False)
    p.eyes(46, 54, 48, 2.2)
    return p.out()


def birds_5():  # pavo real
    p = Pc()
    for a in range(-70, 71, 14):
        with p.s.rotate(a, 50, 74):
            p.ell(50, 32, 7, 30, ["#7FE0C8", "#1E8F8A"], sw=1)
            p.circ(50, 14, 5, ["#FFE88A", "#3F7FD0"], sw=1)
    p.ell(50, 76, 16, 16, ["#4F8FE0", "#1E3A8A"])
    p.circ(50, 56, 9, ["#4F8FE0", "#1E3A8A"])
    p.line(50, 48, 50, 40, "#4A3A8A", 1.6)
    p.circ(50, 38, 2, "#6FCF9A", ol=False)
    p.poly([(52, 56), (58, 58), (52, 60)], ["#FFE08A", "#C9902B"], sw=1)
    p.circ(48, 55, 1.6, "#2B1B3A", ol=False)
    return p.out()


def birds_6():  # flamenco
    p = Pc()
    p.s.path("M 46 92 L 46 62", stroke="#FF8AB8", sw=3.2, cap=CAP_ROUND)
    p.s.path("M 56 92 L 56 66 L 52 62", stroke="#FF8AB8", sw=3.2, cap=CAP_ROUND)
    p.ell(48, 56, 24, 14, ["#FFB8D0", "#E8586D"])
    p.path("M 56 52 C 72 44 72 26 62 16 C 56 10 48 14 50 22 C 56 18 60 26 56 32 C 52 38 44 44 36 48 Z", ["#FFB8D0", "#E8586D"])
    p.circ(58, 14, 8, ["#FFC8DC", "#E8586D"])
    p.path("M 62 12 Q 76 12 74 24 Q 70 18 64 18 Z", ["#2B2B45", "#12121E"])
    p.circ(58, 12, 2, "#2B1B3A", ol=False)
    p.path("M 30 52 C 24 60 30 68 40 62 Z", ["#FF8AB8", "#C8203B"], ol=False)
    return p.out()


def birds_7():  # cuervo
    p = Pc()
    p.path("M 14 62 C 10 40 30 22 54 28 C 76 32 86 52 78 68 C 66 86 24 84 14 62 Z", ["#6A6A8A", "#12121E"])
    p.path("M 40 46 C 28 20 58 6 76 24 C 62 28 54 38 48 50 Z", ["#4A4A68", "#12121E"])
    p.circ(72, 38, 13, ["#6A6A8A", "#12121E"])
    p.path("M 82 36 L 98 42 L 82 48 Z", ["#8A8AA8", "#3A3A55"])
    p.circ(74, 34, 3.4, "#FFFFFF", sw=1)
    p.circ(75, 34, 1.6, "#2B1B3A", ol=False)
    p.path("M 14 62 L 2 76 L 20 72 Z", ["#4A4A68", "#12121E"])
    p.line(40, 82, 38, 92, "#8A8AA8", 2.6)
    p.line(54, 82, 56, 92, "#8A8AA8", 2.6)
    return p.out()


def birds_8():  # cisne
    p = Pc()
    p.path("M 6 70 C 6 56 24 50 50 52 C 74 54 94 56 94 70 C 90 88 20 90 6 70 Z", ["#FFFFFF", "#B8C4DC"])
    p.path("M 24 62 C 40 50 62 54 76 64 C 60 60 40 62 24 62 Z", ["#F2F6FF", "#C9D3E8"], ol=False)
    p.s.path("M 70 56 C 94 54 82 30 66 28 C 56 28 56 12 70 10", stroke="#9AA8C8", sw=11.5, cap=CAP_ROUND)
    p.s.path("M 70 56 C 94 54 82 30 66 28 C 56 28 56 12 70 10", stroke="#FFFFFF", sw=8.5, cap=CAP_ROUND)
    p.s.path("M 70 56 C 94 54 82 30 66 28 C 56 28 56 12 70 10", stroke="#C9D3E8", sw=1.5, cap=CAP_ROUND, op=.0)
    p.circ(72, 12, 8, ["#FFFFFF", "#C9D3E8"])
    p.poly([(78, 11), (92, 14), (78, 17)], ["#FFB86B", "#E8642B"], sw=1)
    p.circ(73, 10, 1.8, "#2B1B3A", ol=False)
    return p.out()


def birds_9():  # pluma
    p = Pc()
    with p.s.rotate(35, 50, 50):
        p.path("M 50 4 C 80 22 80 66 50 90 C 20 66 20 22 50 4 Z", ["#8AD0F5", "#3F7FD0"])
        p.line(50, 4, 50, 94, "#2A4AA0", 2.4)
        for y in range(18, 78, 10):
            p.line(50, y + 6, 66, y - 6, "#4F8FE0", 1.2)
            p.line(50, y + 6, 34, y - 6, "#4F8FE0", 1.2)
    p.sparkle(78, 20, 6)
    return p.out()


def birds_10():  # dodo
    p = Pc()
    p.ell(50, 62, 30, 24, ["#A9A0B8", "#5A5468"])
    p.circ(66, 36, 16, ["#B8B0C8", "#6A6478"])
    p.path("M 74 28 C 96 26 100 48 80 52 C 76 44 74 36 74 28 Z", ["#FFE08A", "#C9902B"])
    p.circ(66, 32, 4.2, "#FFFFFF", sw=1)
    p.circ(67, 32, 2, "#2B1B3A", ol=False)
    p.curve("M 60 22 Q 56 8 62 4", "#5A5468", 2)
    p.curve("M 66 20 Q 66 6 74 4", "#5A5468", 2)
    p.path("M 22 60 C 12 66 14 80 28 78 Z", ["#8A8098", "#4A4458"])
    p.line(42, 84, 40, 94, "#E8A06B", 3)
    p.line(58, 84, 60, 94, "#E8A06B", 3)
    return p.out()


def build():
    icons = {}
    for pre, ser in (("future", "future"), ("dino", "dino"), ("bugs", "bugs"), ("birds", "birds")):
        for i in range(1, 11):
            icons["piece.%s_%d" % (ser, i)] = globals()["%s_%d" % (pre, i)]()
    return {"icons": icons}
