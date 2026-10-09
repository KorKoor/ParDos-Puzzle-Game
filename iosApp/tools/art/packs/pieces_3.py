import os
import sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from _pk import *  # noqa: F401,F403
import _animals

WH = ["#FFFFFF", "#C9D3E8"]


def fish(p, x, y, c, s_=1.0, tail=None, flip=False):
    tail = tail or c
    with p.s.flip_x(50) if flip else p.s.translate(0, 0):
        p.path("M %f %f L %f %f L %f %f Z" % (x - 22 * s_, y, x - 40 * s_, y - 14 * s_, x - 40 * s_, y + 14 * s_), tail)
        p.ell(x, y, 26 * s_, 17 * s_, c)
        p.eyes(x + 12 * s_, x + 12 * s_, y - 4 * s_, 2.6 * s_)


# ------------------------------------------------------------------ Mar profundo
def ocean_1():  # pez payaso
    p = Pc(shadow=False)
    p.path("M 22 50 L 6 34 L 8 66 Z", ["#FFB86B", "#E8642B"])
    p.ell(54, 50, 36, 24, ["#FFB86B", "#E8642B"])
    for x, w in ((40, 6), (62, 5)):
        p.ell(x, 50, w, 23, WH, ol=True, sw=1)
    p.path("M 40 28 Q 56 12 72 30 Z", ["#FF9A4A", "#C8501B"])
    p.eyes(78, 78, 46, 3)
    p.shine(46, 38, 8, 3, 0, .5)
    return p.out()


def ocean_2():  # gota
    p = Pc()
    p.path("M 50 8 C 50 8 20 44 20 62 C 20 80 34 90 50 90 C 66 90 80 80 80 62 C 80 44 50 8 50 8 Z", ["#9FE0FF", "#2B7AC0"])
    p.shine(38, 62, 6, 13, 12, .6)
    return p.out()


def ocean_3():  # pez globo
    p = Pc(shadow=False)
    for a in range(0, 360, 30):
        x, y = 50 + math.cos(math.radians(a)) * 34, 52 + math.sin(math.radians(a)) * 34
        p.line(50 + math.cos(math.radians(a)) * 26, 52 + math.sin(math.radians(a)) * 26, x, y, "#C9902B", 3)
    p.path("M 18 52 L 4 38 V 66 Z", ["#FFD36E", "#E8A91F"])
    p.circ(54, 52, 28, ["#FFE88A", "#E8A91F"])
    p.ell(56, 66, 20, 10, ["#FFFFFF", "#FFF1C8"], ol=False)
    p.eyes(66, 66, 44, 4.4)
    p.ell(78, 56, 4, 3, "#C9902B", ol=False)
    return p.out()


def ocean_4():  # sombrilla
    p = Pc()
    p.line(50, 30, 44, 90, "#8A5A2B", 3.4)
    p.path("M 8 44 C 10 20 40 10 52 12 C 66 14 92 24 92 44 Q 80 36 70 44 Q 60 36 50 44 Q 40 36 30 44 Q 20 36 8 44 Z", ["#FF8A7A", "#D6402B"])
    p.path("M 52 12 C 44 20 40 34 42 42 Q 50 36 52 44 C 52 30 54 20 52 12 Z", WH, ol=False)
    p.path("M 52 12 C 66 14 80 22 84 34 Q 74 28 66 40 Q 62 30 58 24 Z", WH, ol=False)
    return p.out()


def ocean_5():  # cangrejo
    p = Pc()
    for s_ in (-1, 1):
        x = 50 + s_ * 34
        p.line(50 + s_ * 20, 66, x, 78, "#C83A2B", 3)
        p.line(50 + s_ * 16, 58, 50 + s_ * 40, 62, "#C83A2B", 3)
        p.circ(50 + s_ * 34, 30, 12, ["#FF8A7A", "#C83A2B"])
        p.line(50 + s_ * 22, 52, 50 + s_ * 32, 38, "#C83A2B", 3.4)
        p.poly([(50 + s_ * 34, 18), (50 + s_ * 30, 30), (50 + s_ * 38, 30)], "#E8F4FF", ol=False)
    p.ell(50, 62, 28, 18, ["#FF8A7A", "#C83A2B"])
    p.line(42, 48, 42, 40, "#C83A2B", 2.4)
    p.line(58, 48, 58, 40, "#C83A2B", 2.4)
    p.eyes(42, 58, 38, 3.4)
    return p.out()


def ocean_6():  # surfista (tabla y ola)
    p = Pc()
    p.ell(50, 54, 12, 40, ["#FFD84A", "#E8A91F"], rot=-28)
    p.line(36, 22, 66, 88, "#FF8A7A", 2.6)
    p.path("M 6 82 Q 24 66 40 82 T 74 82 T 98 80 V 94 H 6 Z", ["#7FD0F5", "#2B7AC0"])
    p.circ(66, 40, 8, ["#FFD3A8", "#E8A06B"])
    p.path("M 60 48 L 74 48 L 70 68 L 62 68 Z", ["#FF8AB8", "#D6336C"])
    return p.out()


def ocean_7():  # tiburon
    p = Pc(shadow=False)
    p.path("M 6 66 C 20 30 62 24 94 54 C 80 54 74 64 62 70 C 42 82 18 82 6 66 Z", ["#9AB0CC", "#4A6A8A"])
    p.path("M 50 36 L 60 8 L 70 40 Z", ["#9AB0CC", "#4A6A8A"])
    p.path("M 8 66 C 24 72 54 76 64 70 C 58 80 30 86 8 66 Z", WH, ol=False)
    p.path("M 6 66 L 0 50 L 18 58 Z", ["#7A90B0", "#3A5070"])
    p.eyes(68, 68, 50, 3)
    p.curve("M 72 62 Q 84 64 90 58", "#2B3A5A", 2)
    for x in (76, 80, 84):
        p.poly([(x, 61), (x + 2, 66), (x + 4, 60)], "#FFFFFF", ol=False)
    return p.out()


def ocean_8():  # ballena
    p = Pc(shadow=False)
    p.path("M 8 62 C 8 28 56 14 86 40 C 94 48 96 36 94 28 C 100 34 100 46 94 56 C 86 80 40 92 8 62 Z", ["#7FB8F0", "#2B5AB0"])
    p.path("M 14 66 C 36 80 70 78 84 62 C 70 90 30 92 14 66 Z", ["#EAF4FF", "#C0D8F5"], ol=False)
    p.eyes(30, 30, 52, 3.2)
    p.curve("M 20 62 Q 30 68 40 64", "#1E3A70", 2)
    for dx in (-8, 0, 8):
        p.curve("M 46 24 Q %d 10 %d 4" % (46 + dx, 46 + dx * 2), "#BDEBFA", 3)
    return p.out()


def ocean_9():  # pulpo
    p = Pc()
    for k in range(6):
        x = 22 + k * 11.2
        p.curve("M %f 56 Q %f 76 %f 88" % (x, x + (k - 2.5) * 5, x + (k - 2.5) * 4), "#9A6FE0", 7)
    p.ell(50, 38, 28, 28, ["#C9A8F5", "#7A4FD0"])
    p.eyes(40, 60, 40, 4.6)
    p.curve("M 44 52 Q 50 57 56 52", "#4A2FA0", 2)
    p.shine(38, 24, 7, 3, -30, .55)
    return p.out()


def ocean_10():  # calamar luminoso
    p = Pc(shadow=False)
    p.circ(50, 50, 40, "#6A8AF5", ol=False, sw=0)
    p.s.circle(50, 50, 42, fill=rad(50, 50, 42, [(0, "#7FFFE8CC"), (1, "#7FFFE800")]))
    for k in range(5):
        x = 36 + k * 7
        p.curve("M %f 58 Q %f 74 %f 88" % (x, x + (k - 2) * 6, x + (k - 2) * 3), "#FFB8F0", 4)
    p.path("M 50 8 Q 74 24 66 58 H 34 Q 26 24 50 8 Z", ["#FFC8F5", "#B84FD0"])
    p.eyes(42, 58, 58, 3.4)
    for x, y in ((50, 26), (44, 38), (56, 38), (50, 48)):
        p.circ(x, y, 2.4, "#8AFFF0", ol=False)
    return p.out()


# ------------------------------------------------------------------ Fiesta
def fest_1():  # confeti
    p = Pc()
    p.path("M 14 90 L 36 36 L 64 64 Z", ["#FFD84A", "#E8A91F"])
    p.path("M 14 90 L 24 62 L 40 78 Z", ["#FF8AB8", "#D6336C"], ol=False)
    for x, y, c in ((56, 22, "#FF8A7A"), (74, 40, "#8AB0F5"), (46, 12, "#7FD08A"), (86, 22, "#FFD84A"), (68, 12, "#FF8AB8"), (80, 56, "#B49CF5")):
        p.rect(x - 3, y - 3, 7, 7, c, r=1.5, sw=1)
    p.curve("M 40 36 Q 54 28 56 14", "#FF8AB8", 2)
    p.curve("M 42 40 Q 66 34 82 38", "#8AB0F5", 2)
    return p.out()


def fest_2():  # entrada
    p = Pc()
    p.path("M 8 28 H 92 V 44 A 7 7 0 0 0 92 58 V 74 H 8 V 58 A 7 7 0 0 0 8 44 Z", ["#FF8A7A", "#C83A2B"])
    p.s.line(66, 30, 66, 72, stroke="#FFF1D0", sw=1.6)
    p.star(34, 51, 13, ["#FFE08A", "#C9902B"])
    for y in (40, 51, 62):
        p.line(74, y, 86, y, "#FFF1D0", 2.2)
    return p.out()


def fest_3():  # regalo
    p = Pc()
    p.rect(14, 40, 72, 48, ["#8AB0F5", "#3F5FD0"], r=4)
    p.rect(10, 28, 80, 16, ["#A8C4F8", "#4A6FE0"], r=4)
    p.rect(44, 28, 12, 60, ["#FFE08A", "#C9902B"], r=0)
    p.path("M 50 28 C 30 10 18 24 34 28 Z", ["#FFE08A", "#C9902B"])
    p.path("M 50 28 C 70 10 82 24 66 28 Z", ["#FFE08A", "#C9902B"])
    p.circ(50, 28, 5, ["#FFF1A8", "#E8A91F"])
    return p.out()


def fest_4():  # fogata
    p = Pc()
    p.rect(12, 74, 76, 10, ["#8A5A2B", "#4A3220"], r=5)
    with p.s.rotate(-18, 50, 80):
        p.rect(14, 74, 72, 10, ["#A9733B", "#5A3A26"], r=5)
    p.path("M 50 10 C 74 36 84 52 74 70 C 68 80 32 80 26 70 C 18 54 34 40 40 24 C 44 34 48 32 50 10 Z", ["#FF9A4A", "#D6402B"])
    p.path("M 50 36 C 64 52 68 62 62 72 C 58 78 42 78 38 72 C 32 62 44 54 50 36 Z", ["#FFE86B", "#FFB02B"], ol=False)
    return p.out()


def fest_5():  # mascara
    p = Pc()
    p.path("M 8 36 C 20 22 38 28 50 38 C 62 28 80 22 92 36 C 94 56 76 70 62 62 C 56 58 54 54 50 54 C 46 54 44 58 38 62 C 24 70 6 56 8 36 Z", ["#B49CF5", "#6A4FD0"])
    p.ell(30, 44, 9, 6, "#2B1B3A", ol=False, rot=14)
    p.ell(70, 44, 9, 6, "#2B1B3A", ol=False, rot=-14)
    for x in (22, 78):
        p.star(x, 28, 5, ["#FFE08A", "#C9902B"])
    p.curve("M 40 22 Q 50 12 60 22", "#FFE08A", 3)
    p.shine(28, 34, 6, 2.4, 20, .5)
    return p.out()


def fest_6():  # rueda de feria
    p = Pc()
    p.path("M 30 92 L 50 52 L 70 92", ["#8A8AA8", "#4A4A68"], ol=False)
    p.curve("M 30 92 L 50 52 L 70 92", "#4A4A68", 4)
    p.s.circle(50, 46, 34, stroke="#E8586D", sw=4)
    for a in range(0, 360, 45):
        x, y = 50 + math.cos(math.radians(a)) * 34, 46 + math.sin(math.radians(a)) * 34
        p.line(50, 46, x, y, "#FFB8C4", 1.6)
        p.circ(x, y + 3, 6, ["#FFE08A", "#E8A91F"] if (a // 45) % 2 else ["#8AD0F5", "#2B7AC0"], sw=1.1)
    p.circ(50, 46, 5, "#FFFFFF")
    return p.out()


def fest_7():  # carpa
    p = Pc()
    p.rect(14, 52, 72, 38, ["#FFF4DC", "#E8C99A"], r=3)
    for k in range(4):
        p.poly([(14 + k * 18, 52), (14 + k * 18 + 18, 52), (50, 14)], ["#FF8A7A", "#C83A2B"] if k % 2 == 0 else ["#FFFFFF", "#D3DCEF"], sw=1.1)
    p.path("M 40 90 V 66 Q 50 54 60 66 V 90 Z", ["#7A4A6A", "#3A1E3A"])
    p.line(50, 14, 50, 4, "#8A5A2B", 2)
    p.poly([(50, 4), (62, 8), (50, 12)], ["#FFE08A", "#C9902B"], sw=1)
    return p.out()


def fest_8():  # fuegos artificiales
    p = Pc(shadow=False)
    for cx, cy, r, c in ((50, 42, 30, "#FFD84A"), (22, 66, 14, "#FF8AB8"), (80, 66, 15, "#8AD0F5")):
        for a in range(0, 360, 30):
            ca, sa = math.cos(math.radians(a)), math.sin(math.radians(a))
            p.line(cx + ca * r * 0.35, cy + sa * r * 0.35, cx + ca * r, cy + sa * r, c, 3 if r > 20 else 2.2)
            p.dot(cx + ca * r * 1.1, cy + sa * r * 1.1, 2, "#FFFFFF")
        p.circ(cx, cy, 4, "#FFFFFF", ol=False)
    return p.out()


def fest_9():  # globo
    p = Pc()
    p.path("M 50 8 C 82 8 90 44 70 64 C 62 72 56 74 54 80 H 46 C 44 74 38 72 30 64 C 10 44 18 8 50 8 Z", ["#FF8A9A", "#D6336C"])
    p.poly([(46, 80), (54, 80), (56, 86), (44, 86)], "#D6336C")
    p.curve("M 50 86 Q 40 92 52 96", "#9A8AA8", 1.6)
    p.shine(34, 28, 6, 12, 25, .55)
    return p.out()


def fest_10():  # carrusel
    p = Pc()
    p.line(50, 8, 50, 90, "#FFE08A", 4)
    p.poly([(14, 22), (50, 8), (86, 22), (78, 30), (22, 30)], ["#FF8A9A", "#D6336C"])
    p.rect(22, 30, 56, 6, ["#FFE08A", "#C9902B"], r=2)
    p.path("M 28 78 C 24 54 38 40 56 42 L 66 34 L 72 44 L 62 48 C 70 60 70 74 64 80 L 56 80 L 56 66 L 40 66 L 36 80 Z", ["#FFFFFF", "#C9D3E8"])
    p.circ(64, 38, 2, "#2B1B3A", ol=False)
    p.path("M 46 42 Q 40 52 34 58", ["#FFB8D0", "#E8586D"], ol=False)
    p.rect(14, 86, 72, 6, ["#FFE08A", "#C9902B"], r=3)
    return p.out()


# ------------------------------------------------------------------ Noche magica
def mag_1():  # farol
    p = Pc()
    p.rect(40, 6, 20, 8, ["#8A5A2B", "#4A3220"], r=3)
    p.path("M 28 20 C 18 36 18 62 30 78 H 70 C 82 62 82 36 72 20 Z", ["#FF8A5A", "#D6402B"])
    for x in (36, 50, 64):
        p.line(x, 22, x, 78, "#8A2A1B", 1.4)
    p.ell(50, 50, 12, 18, ["#FFF1A8", "#FFB02B"], ol=False)
    p.rect(34, 78, 32, 8, ["#8A5A2B", "#4A3220"], r=3)
    p.line(50, 86, 50, 96, "#FFE08A", 2.6)
    return p.out()


def mag_2():  # lampara
    p = Pc()
    p.path("M 50 8 C 78 8 86 36 70 54 C 64 60 64 66 62 72 H 38 C 36 66 36 60 30 54 C 14 36 22 8 50 8 Z", ["#FFF8C0", "#FFC83D"])
    p.rect(38, 72, 24, 16, ["#C9D3E8", "#7A88A8"], r=3)
    p.line(40, 78, 60, 78, "#4A5A8A", 1.6)
    p.line(40, 83, 60, 83, "#4A5A8A", 1.6)
    p.s.path("M 42 52 Q 50 36 58 52", stroke="#C9902B", sw=1.8)
    p.shine(36, 26, 5, 10, 25, .6)
    return p.out()


def mag_3():  # vela
    p = Pc()
    p.rect(36, 40, 28, 48, ["#FFF4DC", "#E8C99A"], r=5)
    p.path("M 36 44 Q 42 54 46 44 Q 52 60 58 44 Q 60 50 64 46 V 40 H 36 Z", ["#FFFFFF", "#F2E2C0"], ol=False)
    p.line(50, 40, 50, 30, "#4A3A3A", 2.2)
    p.path("M 50 8 C 62 20 62 32 50 34 C 38 32 38 20 50 8 Z", ["#FFE86B", "#E8502B"])
    p.ell(50, 28, 3.6, 5.5, "#FFFFFF", ol=False)
    p.ell(50, 90, 30, 4, ["#E8C99A", "#B8903B"], ol=False)
    return p.out()


def mag_4():  # constelacion
    p = Pc(shadow=False)
    pts = [(18, 70), (36, 40), (56, 52), (72, 22), (86, 58)]
    for a, b in zip(pts, pts[1:]):
        p.line(a[0], a[1], b[0], b[1], "#B8C8F0", 1.8)
    for i, (x, y) in enumerate(pts):
        p.star(x, y, 9 if i % 2 == 0 else 7, ["#FFF1A8", "#F2A91F"])
    p.sparkle(14, 24, 5)
    p.sparkle(60, 80, 4)
    return p.out()


def mag_5():  # libro de hechizos
    p = Pc()
    p.path("M 8 22 Q 30 14 50 24 Q 70 14 92 22 V 80 Q 70 72 50 82 Q 30 72 8 80 Z", ["#8A6FE0", "#3A1E8A"])
    p.path("M 14 26 Q 32 20 48 28 V 76 Q 32 70 14 76 Z", ["#FFF8E6", "#E8D8B0"])
    p.path("M 52 28 Q 68 20 86 26 V 76 Q 68 70 52 76 Z", ["#FFF8E6", "#E8D8B0"])
    p.star(32, 50, 11, ["#FFE08A", "#C9902B"])
    p.sparkle(70, 40, 7, "#8A6FE0")
    p.line(60, 54, 80, 54, "#B49CF5", 1.8)
    p.line(60, 62, 76, 62, "#B49CF5", 1.8)
    return p.out()


def mag_6():  # pocion
    p = Pc()
    p.path("M 40 8 H 60 V 34 C 86 54 88 88 50 88 C 12 88 14 54 40 34 Z", ["#E8F4FF", "#B8D0F0"])
    p.path("M 28 62 C 22 78 34 88 50 88 C 66 88 78 78 72 62 Q 50 54 28 62 Z", ["#C98AF5", "#7A2FD0"], ol=False)
    p.rect(38, 4, 24, 10, ["#C9955E", "#7A4A2B"], r=3)
    p.circ(40, 72, 3.4, "#E8C8FF", ol=False)
    p.circ(56, 66, 2.4, "#E8C8FF", ol=False)
    p.sparkle(70, 30, 6, "#C98AF5")
    return p.out()


def mag_7():  # bola de cristal
    p = Pc()
    p.path("M 22 90 L 30 70 H 70 L 78 90 Z", ["#8A6FE0", "#3A1E8A"])
    p.circ(50, 42, 30, ["#E8D8FF", "#7A4FD0"])
    p.circ(50, 42, 24, ["#C8A8FF", "#4A2FA0"], ol=False)
    p.sparkle(50, 40, 12, "#FFFFFF", .95)
    p.sparkle(38, 52, 5, "#FFE08A")
    p.sparkle(62, 30, 4)
    p.shine(38, 26, 8, 4, -35, .7)
    return p.out()


def mag_8():  # varita estelar
    p = Pc()
    with p.s.rotate(-45, 50, 50):
        p.rect(44, 36, 9, 56, ["#6A4FD0", "#2B1B6A"], r=3)
        p.rect(44, 36, 9, 7, ["#FFE08A", "#C9902B"], r=2)
    p.star(72, 26, 22, ["#FFF1A8", "#F2A91F"])
    p.sparkle(26, 22, 6)
    p.sparkle(84, 60, 5)
    p.sparkle(18, 62, 4)
    return p.out()


def mag_9():
    return _animals.head("OWL")


def mag_10():  # mago de la noche
    p = Pc()
    p.path("M 50 4 Q 66 30 84 46 H 16 Q 34 28 50 4 Z", ["#6A4FD0", "#2B1B6A"])
    p.rect(12, 42, 76, 9, ["#8A6FE0", "#3A1E8A"], r=4)
    p.star(50, 26, 7, ["#FFE08A", "#C9902B"])
    p.circ(50, 62, 20, ["#FFD3A8", "#E8A06B"])
    p.path("M 30 62 Q 34 92 50 94 Q 66 92 70 62 Q 60 74 50 72 Q 40 74 30 62 Z", ["#FFFFFF", "#C9D3E8"])
    p.eyes(43, 57, 58, 2.6)
    p.ell(50, 64, 4, 3, "#E8A06B", ol=False)
    return p.out()


# ------------------------------------------------------------------ Granja
def farm_1():
    return _animals.head("CHICK", "PLATINUM", extra={"inner": "#E8586D"})


def farm_2():
    return _animals.head("COW")


def farm_3():
    return _animals.head("PIG")


def farm_4():
    return _animals.head("SHEEP")


def farm_5():  # caballo
    p = Pc()
    p.path("M 30 92 C 24 60 26 34 44 22 L 40 6 L 54 16 C 70 14 82 30 82 44 L 90 54 L 78 62 L 66 54 C 64 66 70 80 64 92 Z", ["#C98A55", "#7A4A2B"])
    p.path("M 40 6 L 54 16 L 44 22 Z", ["#7A4A2B", "#3A2418"])
    p.path("M 44 22 C 34 30 28 46 30 60 C 40 44 46 34 54 16 Z", ["#4A2A1E", "#1E1010"], ol=False)
    p.circ(66, 36, 3, "#2B1B3A", ol=False)
    p.ell(86, 54, 3, 2, "#4A2A1E", ol=False)
    p.shine(56, 28, 6, 3, 40, .4)
    return p.out()


def farm_6():  # tractor
    p = Pc()
    p.rect(52, 36, 36, 34, ["#7FD08A", "#2E7A4A"], r=4)
    p.rect(18, 46, 40, 24, ["#7FD08A", "#2E7A4A"], r=4)
    p.rect(56, 40, 24, 16, ["#BFEFFF", "#7FC0EB"], r=2)
    p.rect(20, 34, 5, 14, "#4A4A68", r=1.5)
    p.circ(70, 74, 17, ["#4A4A68", "#1E1E32"])
    p.circ(70, 74, 8, ["#FFE08A", "#C9902B"])
    p.circ(30, 80, 10, ["#4A4A68", "#1E1E32"])
    p.circ(30, 80, 4.5, ["#FFE08A", "#C9902B"])
    return p.out()


def farm_7():  # maiz de oro
    p = Pc()
    p.leaf(50, 90, 52, 14, -125, "#6FBF73")
    p.leaf(50, 90, 52, 14, -55, "#4E9A5A")
    p.ell(50, 44, 15, 36, ["#FFE88A", "#E8A91F"])
    for yy in range(16, 74, 9):
        for xx in (-7, 0, 7):
            p.circ(50 + xx, yy, 3.2, ["#FFF4B8", "#F2B02B"], sw=0.8)
    p.shine(45, 28, 3, 10, 5, .6)
    return p.out()


def farm_8():  # gallo de oro
    return _animals.head("CHICK", "GOLD", extra={"inner": "#E8586D"})


def farm_9():  # pollito
    return _animals.head("CHICK")


def farm_10():  # campo de trigo
    p = Pc()
    for x, h, a in ((30, 64, -8), (50, 72, 0), (70, 64, 8)):
        p.line(x, 90, x + a, 90 - h, "#C9902B", 2.4)
        for k in range(6):
            y = 90 - h + k * 9
            for s_ in (-1, 1):
                with p.s.rotate(s_ * 35, x + a, y):
                    p.ell(x + a + s_ * 4, y, 3.4, 8, ["#FFE88A", "#E8A91F"], sw=1)
        p.ell(x + a, 90 - h - 6, 3.2, 8, ["#FFE88A", "#E8A91F"], sw=1)
    return p.out()


def build():
    icons = {}
    for pre, ser in (("ocean", "ocean"), ("fest", "festival"), ("mag", "magic"), ("farm", "farm")):
        for i in range(1, 11):
            icons["piece.%s_%d" % (ser, i)] = globals()["%s_%d" % (pre, i)]()
    return {"icons": icons}
