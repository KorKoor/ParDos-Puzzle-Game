"""
ui_icons_a: iconos de interfaz de ParDos para iPhone.
  cozy.<tipo>   16 pegatinas 3D suaves (CozyKind de Android), 100x100, a todo color.
  power.<glifo> 5 poderes de Android + power.freeze + power.extratime, con ranuras $base $light $dark
                (paleta por defecto = color base de cada poder; la app puede tintarlos o ponerlos en gris).
"""
import os
import sys
import math

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, ".."))   # artlib
sys.path.insert(0, HERE)                        # _looks, _kit
from artlib import *  # noqa: F401,F403

INK = "#2B1B3A"


# ------------------------------------------------------------------ utilidades

def _rgb(h):
    h = h.lstrip("#")
    return int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16)


def blend(a, b, t):
    ra, rb = _rgb(a), _rgb(b)
    return "#%02X%02X%02X" % tuple(int(round(ra[i] + (rb[i] - ra[i]) * t)) for i in range(3))


def lighter(c, t=.4):
    return blend(c, "#FFFFFF", t)


def darker(c, t=.35):
    return blend(c, INK, t)


def pcmds(pts, closed=True):
    c = [("M", pts[0][0], pts[0][1])] + [("L", x, y) for (x, y) in pts[1:]]
    if closed:
        c.append(("Z",))
    return c


def spts(cx, cy, ro, ri, n=5, rot=-90):
    out = []
    for i in range(n * 2):
        r = ro if i % 2 == 0 else ri
        a = math.radians(rot + i * 180.0 / n)
        out.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    return out


def sparkle_cmds(cx, cy, r, squeeze=.2):
    k = r * squeeze
    p = Path().M(cx, cy - r).Q(cx + k, cy - k, cx + r, cy).Q(cx + k, cy + k, cx, cy + r)
    p.Q(cx - k, cy + k, cx - r, cy).Q(cx - k, cy - k, cx, cy - r).Z()
    return p.cmds


def body(s, cmds, fill, edge, grow=4.0, ring=2.0):
    """Masa de color con esquinas suaves: anillo oscuro por fuera y relleno degradado engordado."""
    s.path(cmds, stroke=edge, sw=grow + 2 * ring, join=JOIN_ROUND)
    s.path(cmds, fill=fill, stroke=fill, sw=grow, join=JOIN_ROUND)


def shine(s, x, y, w, h, rot=0, a=.6):
    with s.rotate(rot, x + w / 2.0, y + h / 2.0):
        s.oval(x, y, w, h, fill="#FFFFFF", op=a)


def gold_spark(s, cx, cy, r, squeeze=.22, top="#FFF6C8", bot="#FFD25A", edge="#C98A14", grow=.8, ring=1.1):
    body(s, sparkle_cmds(cx, cy, r, squeeze), lin(cx - r, cy - r, cx + r, cy + r, [(0, top), (1, bot)]), edge, grow, ring)


def face(s, cx, cy, k=1.0, ink="#7A4A12", blush="#FF8FA3", blush_op=.55):
    for dx in (-9, 9):
        s.circle(cx + dx * k, cy, 2.7 * k, fill=ink)
        s.circle(cx + dx * k - .85 * k, cy - 1.0 * k, .95 * k, fill="#FFFFFF", op=.9)
    s.path(Path().M(cx - 4.6 * k, cy + 3.6 * k).Q(cx, cy + 9.2 * k, cx + 4.6 * k, cy + 3.6 * k),
           stroke=ink, sw=1.9 * k, cap=CAP_ROUND)
    s.circle(cx - 15.5 * k, cy + 5 * k, 3.5 * k, fill=blush, op=blush_op)
    s.circle(cx + 15.5 * k, cy + 5 * k, 3.5 * k, fill=blush, op=blush_op)


def ground(s, cx, rx, y=92, ry=3.6):
    s.ellipse(cx, y, rx, ry, fill="#00000024")


# ================================================================== COZY (16)

GOLD_L, GOLD_B, GOLD_D = "#FFE680", "#FFC83D", "#D9961A"
GOLD_EDGE = blend(GOLD_D, INK, .42)


def gold_grad(x1=24, y1=14, x2=76, y2=92):
    return lin(x1, y1, x2, y2, [(0, "#FFEE9C"), (.5, GOLD_B), (1, "#EAA222")])


def cozy_star():
    s = Scene()
    ground(s, 50, 25)
    body(s, pcmds(spts(50, 55, 39, 19.5)), gold_grad(), GOLD_EDGE, grow=5.5, ring=2.0)
    shine(s, 32, 28, 14, 7, -38, .7)
    s.circle(27, 37, 1.8, fill="#FFFFFF", op=.7)
    face(s, 50, 58, .95)
    gold_spark(s, 85, 18, 8)
    return s.bake()


def cozy_coin():
    s = Scene()
    ground(s, 50, 30)
    edge = GOLD_EDGE
    s.circle(50, 55, 38, fill=lin(0, 17, 0, 93, [(0, "#E3A62B"), (1, "#B87510")]), stroke=edge, sw=2)
    s.circle(50, 50, 38, fill=lin(20, 12, 80, 90, [(0, "#FFEE9C"), (.5, GOLD_B), (1, "#EBA121")]), stroke=edge, sw=2)
    # plato interior un poco hundido
    s.circle(50, 50, 29.5, fill=lin(0, 21, 0, 79, [(0, "#F4B72F"), (1, "#FFD968")]))
    s.circle(50, 50, 29.5, stroke="#C98A14", sw=2.2, op=.6)
    # emblema en relieve
    sp = sparkle_cmds(50, 50, 19, .24)
    s.path(sp, fill="#B87510", op=.45)
    with s.translate(-1.2, -1.6):
        body(s, sp, lin(32, 32, 68, 68, [(0, "#FFFBE0"), (1, "#FFD968")]), "#C98A14", grow=.8, ring=1.1)
    s.arc_stroke(50, 50, 34, 198, 252, "#FFFFFF", 3.8, op=.75)
    s.circle(21, 62, 1.7, fill="#FFFFFF", op=.6)
    gold_spark(s, 84, 17, 7)
    return s.bake()


def cozy_gem():
    s = Scene()
    ground(s, 50, 27)
    A, B, C, D, E = (14, 38), (30, 16), (70, 16), (86, 38), (50, 89)
    G1, G2 = (38, 38), (62, 38)
    base_fill = lin(20, 14, 80, 90, [(0, "#B9EEFC"), (.45, "#4FB4DC"), (1, "#2F86B5")])
    edge = blend("#2A7AA8", INK, .45)
    body(s, pcmds([A, B, C, D, E]), base_fill, edge, grow=4.4, ring=2.0)
    dk = "#1F5E85"
    s.path(pcmds([B, C, G2, G1]), fill="#FFFFFF", op=.45)
    s.path(pcmds([A, B, G1]), fill="#FFFFFF", op=.2)
    s.path(pcmds([D, C, G2]), fill=dk, op=.16)
    s.path(pcmds([A, G1, E]), fill="#FFFFFF", op=.1)
    s.path(pcmds([G2, D, E]), fill=dk, op=.3)
    ln = "#FFFFFF"
    for (p, q) in ((A, D), (B, G1), (C, G2), (G1, E), (G2, E)):
        s.line(p[0], p[1], q[0], q[1], ln, 1.8, cap=CAP_ROUND, op=.55)
    sp = sparkle_cmds(31, 27, 8, .2)
    s.path(sp, fill="#FFFFFF")
    s.circle(31, 27, 11, fill="#FFFFFF", op=.2)
    gold_spark(s, 86, 14, 6, top="#FFFFFF", bot="#BDEEFF", edge="#2A7AA8")
    return s.bake()


FLAME_PATH = ("M 50 6 C 58 26 82 36 82 62 C 82 82 67 94 50 94 C 33 94 18 82 18 62 "
              "C 18 48 27 40 33 30 C 35 41 41 46 45 44 C 40 30 44 17 50 6 Z")


def cozy_flame():
    s = Scene()
    ground(s, 50, 24, y=93, ry=3.4)
    with s.scale(.88, .88, 50, 53):
        body(s, FLAME_PATH, lin(0, 6, 0, 94, [(0, "#FFB347"), (.5, "#FF7A3C"), (1, "#D93F2B")]),
             blend("#D93F2B", INK, .4), grow=4.5, ring=2.2)
        with s.scale(.72, .72, 50, 90):
            s.path(FLAME_PATH, fill=lin(0, 6, 0, 94, [(0, "#FFE59A"), (1, "#FF9A3C")]))
            with s.scale(.55, .55, 50, 90):
                s.path(FLAME_PATH, fill=lin(0, 6, 0, 94, [(0, "#FFFBE0"), (1, "#FFE08A")]))
        shine(s, 26, 52, 8, 17, 18, .5)
        s.circle(28, 46, 1.8, fill="#FFFFFF", op=.6)
    return s.bake()


def cozy_trophy():
    s = Scene()
    ground(s, 50, 28, y=93, ry=3.4)
    edge = GOLD_EDGE
    handle = lin(0, 18, 0, 46, [(0, "#F6B930"), (1, "#D9961A")])
    with s.scale(.92, .92, 50, 50):
        for (cx, a0, a1) in ((22, 90, 270), (78, -90, 90)):
            s.arc_stroke(cx, 32, 14, a0, a1, edge, 7 + 4.4)
            s.arc_stroke(cx, 32, 14, a0, a1, handle, 7)
        # pie y base
        s.rect(43, 60, 14, 20, 3, fill=lin(43, 0, 57, 0, [(0, "#E8A62A"), (1, "#C98A14")]), stroke=edge, sw=2)
        s.rect(27, 77, 46, 15, 5.5, fill=lin(0, 77, 0, 92, [(0, "#FFE680"), (1, "#E2A020")]), stroke=edge, sw=2.2)
        s.rect(33, 80, 34, 3, 1.5, fill="#FFFFFF", op=.45)
        cup = "M 24 10 L 76 10 L 72 42 C 70 58 60 66 50 66 C 40 66 30 58 28 42 Z"
        body(s, cup, lin(24, 10, 76, 66, [(0, "#FFEE9C"), (.5, GOLD_B), (1, "#E5A024")]), edge, grow=4.5, ring=2.0)
        s.rect(21, 8, 58, 8, 4, fill=lin(0, 8, 0, 16, [(0, "#FFF4C2"), (1, "#F6B930")]), stroke=edge, sw=2)
        body(s, pcmds(spts(50, 37, 13, 6)), lin(0, 24, 0, 50, [(0, "#FFFBE0"), (1, "#FFE08A")]), "#C98A14", grow=1.2, ring=1.2)
        shine(s, 32, 21, 7, 24, 8, .55)
    gold_spark(s, 86, 12, 6)
    return s.bake()


def lid_y(x):
    t = (x - 12) / 76.0
    return 36 - 60 * t + 60 * t * t


def band_poly(x1, x2, bottom=87):
    xs = [x1 + (x2 - x1) * i / 6.0 for i in range(7)]
    return pcmds([(x, lid_y(x)) for x in xs] + [(x2, bottom), (x1, bottom)])


def cozy_chest():
    s = Scene()
    ground(s, 50, 38, y=91, ry=3.8)
    edge = blend("#7A4720", INK, .4)
    gold_edge = "#A8700F"
    s.rect(12, 44, 76, 44, 9, fill=lin(0, 44, 0, 88, [(0, "#C98A4E"), (1, "#9A5C2B")]), stroke=edge, sw=2.4)
    lid = "M 12 50 L 12 36 Q 50 6 88 36 L 88 50 Z"
    s.path(lid, fill=lin(0, 10, 0, 50, [(0, "#EDBE82"), (1, "#C27E44")]), stroke=edge, sw=2.4, join=JOIN_ROUND)
    # vetas
    for (x1, x2, y) in ((16, 25, 62), (75, 84, 70), (16, 22, 76)):
        s.line(x1, y, x2, y, "#5A3216", 1.6, op=.28)
    # bandas
    for (x1, x2) in ((26, 37), (63, 74)):
        s.path(band_poly(x1, x2), fill=lin(x1, 0, x2, 0, [(0, "#FFEBA6"), (1, "#E8AE46")]), stroke=gold_edge, sw=1.5, join=JOIN_ROUND)
    s.rect(11, 42, 78, 10, 4, fill=lin(0, 42, 0, 52, [(0, "#FFEBA6"), (1, "#E5A93F")]), stroke=gold_edge, sw=1.6)
    # cerradura
    s.rect(40, 45, 20, 22, 6, fill=lin(0, 45, 0, 67, [(0, "#FFF0B8"), (1, "#EDB448")]), stroke=gold_edge, sw=2)
    s.circle(50, 54.5, 3.6, fill="#6B3A18")
    s.path(pcmds([(48.4, 55), (51.6, 55), (52.6, 62), (47.4, 62)]), fill="#6B3A18")
    for x in (18.5, 81.5):
        s.circle(x, 81, 2.6, fill="#F4C24A", stroke=gold_edge, sw=1.2)
    s.path(Path().M(17, 37).Q(30, 19, 48, 14), stroke="#FFFFFF", sw=3, op=.4, cap=CAP_ROUND)
    gold_spark(s, 86, 18, 7)
    return s.bake()


def cozy_heart():
    s = Scene()
    ground(s, 50, 25, y=92, ry=3.4)
    with s.scale(.93, .93, 50, 55):
        d = "M 50 88 C 8 58 6 28 30 19 C 42 15 50 25 50 31 C 50 25 58 15 70 19 C 94 28 92 58 50 88 Z"
        body(s, d, lin(16, 15, 84, 90, [(0, "#FFC6D4"), (.42, "#FF6B8A"), (1, "#D93A63")]),
             blend("#D93A63", INK, .38), grow=5, ring=2.0)
        shine(s, 22, 28, 17, 9, -35, .7)
        s.circle(19, 41, 1.9, fill="#FFFFFF", op=.7)
        face(s, 50, 52, 1.0, ink="#A82447", blush="#FFFFFF", blush_op=.5)
    return s.bake()


def cozy_gift():
    s = Scene()
    ground(s, 50, 34, y=92, ry=3.8)
    edge = blend("#B83048", INK, .4)
    rbn_edge = "#B97A12"
    s.rect(16, 46, 68, 44, 8, fill=lin(16, 46, 84, 90, [(0, "#FF9AA8"), (.5, "#E5576B"), (1, "#C73A50")]), stroke=edge, sw=2.2)
    s.rect(10, 34, 80, 16, 7, fill=lin(10, 34, 90, 50, [(0, "#FFAAB6"), (.5, "#EC6377"), (1, "#C73A50")]), stroke=edge, sw=2.2)
    s.rect(43, 34, 14, 56, 3, fill=lin(43, 0, 57, 0, [(0, "#FFEBA6"), (1, "#EBAF45")]), stroke=rbn_edge, sw=1.6)
    # sombra del lazo sobre la tapa
    s.rect(19, 41, 20, 3.5, 1.7, fill="#FFFFFF", op=.45)
    for dx in (-1, 1):
        loop = "M 50 33 C %g 1 %g 34 50 33 Z" % (50 + dx * 38, 50 + dx * 42)
        body(s, loop, lin(20, 8, 80, 34, [(0, "#FFF0B8"), (1, "#EDB448")]), rbn_edge, grow=1.6, ring=1.5)
        s.path(Path().M(50 + dx * 12, 27).Q(50 + dx * 20, 15, 50 + dx * 28, 17), stroke="#FFFFFF", sw=2.2, op=.55, cap=CAP_ROUND)
    s.circle(50, 33, 7.5, fill=lin(43, 26, 57, 40, [(0, "#FFF4C2"), (1, "#EBAF45")]), stroke=rbn_edge, sw=1.8)
    gold_spark(s, 86, 20, 6, top="#FFFFFF", bot="#FFD25A")
    return s.bake()


def cozy_bolt():
    s = Scene()
    ground(s, 50, 22, y=93, ry=3.2)
    pts = [(50, 12), (73, 12), (58, 40), (79, 40), (36, 88), (45, 55), (23, 55)]
    body(s, pcmds(pts), lin(20, 12, 70, 90, [(0, "#FFF2A8"), (.45, GOLD_B), (1, "#E58A12")]),
         blend("#E08A12", INK, .45), grow=5, ring=2.0)
    s.path(Path().M(52, 20).L(40, 47), stroke="#FFFFFF", sw=3, op=.6, cap=CAP_ROUND)
    s.circle(56, 21, 1.8, fill="#FFFFFF", op=.7)
    gold_spark(s, 20, 27, 7)
    gold_spark(s, 84, 74, 5)
    return s.bake()


def cozy_timer():
    s = Scene()
    ground(s, 50, 26, y=93, ry=3.4)
    edge = blend("#2A8576", INK, .4)
    dk = "#2A8576"
    # botones
    s.rect(41, 5, 18, 12, 4, fill=lin(0, 5, 0, 17, [(0, "#7DD3C4"), (1, dk)]), stroke=edge, sw=2)
    s.rect(44, 14, 12, 9, 2, fill=dk, stroke=edge, sw=1.6)
    with s.rotate(-42, 50, 57):
        s.rect(44, 7, 12, 9, 3.5, fill=lin(0, 7, 0, 16, [(0, "#7DD3C4"), (1, dk)]), stroke=edge, sw=2)
    c = (50, 58)
    s.circle(c[0], c[1], 35, fill=lin(20, 24, 80, 92, [(0, "#B7F1E6"), (.45, "#4FB8A8"), (1, "#2F8F80")]), stroke=edge, sw=2.2)
    s.circle(c[0], c[1], 26, fill="#FFFFFF", stroke=blend(dk, "#FFFFFF", .3), sw=2)
    # espacio ya recorrido
    a0, a1 = math.radians(-90), math.radians(25)
    wedge = Path().M(c[0], c[1]).L(c[0] + 24 * math.cos(a0), c[1] + 24 * math.sin(a0))
    wedge.A(24, 24, 0, 0, 1, c[0] + 24 * math.cos(a1), c[1] + 24 * math.sin(a1)).Z()
    s.path(wedge, fill="#E07A5F", op=.28)
    for i in range(12):
        a = math.radians(i * 30 - 90)
        r0, r1 = (20.5, 24.5) if i % 3 == 0 else (22, 24.5)
        s.line(c[0] + r0 * math.cos(a), c[1] + r0 * math.sin(a), c[0] + r1 * math.cos(a), c[1] + r1 * math.sin(a),
               dk, 2.0 if i % 3 == 0 else 1.4, op=.7)
    s.line(c[0], c[1], c[0], c[1] - 15, dk, 3.6)
    s.line(c[0], c[1], c[0] + 18 * math.cos(a1), c[1] + 18 * math.sin(a1), "#E07A5F", 3.2)
    s.circle(c[0], c[1], 3.8, fill=dk)
    s.circle(c[0], c[1], 1.4, fill="#FFFFFF", op=.8)
    s.arc_stroke(c[0], c[1], 31, 196, 246, "#FFFFFF", 3.4, op=.55)
    gold_spark(s, 85, 30, 6)
    return s.bake()


def cozy_shield():
    s = Scene()
    ground(s, 50, 24, y=94, ry=3.2)
    with s.scale(.92, .92, 50, 50):
        d = "M 50 6 L 86 20 L 86 48 C 86 72 70 86 50 94 C 30 86 14 72 14 48 L 14 20 Z"
        body(s, d, lin(14, 6, 86, 94, [(0, "#C4F0D5"), (.45, "#6BB08A"), (1, "#3E8060")]),
             blend("#3E8060", INK, .4), grow=5, ring=2.0)
        half = "M 50 6 L 86 20 L 86 48 C 86 72 70 86 50 94 Z"
        s.path(half, fill="#1F5A3E", op=.2)
        s.path("M 50 15 L 77 26 L 77 47 C 77 66 65 77 50 84 C 35 77 23 66 23 47 L 23 26 Z",
               stroke="#FFFFFF", sw=1.8, op=.5, join=JOIN_ROUND)
        body(s, pcmds(spts(50, 51, 19, 8.8)), lin(0, 32, 0, 70, [(0, "#FFF4C2"), (1, "#FFC83D")]),
             "#C98A14", grow=1.6, ring=1.4)
        shine(s, 22, 22, 7, 26, 8, .5)
    gold_spark(s, 84, 12, 6, top="#FFFFFF", bot="#FFE680")
    return s.bake()


def cozy_piggy():
    s = Scene()
    ground(s, 46, 34, y=92, ry=3.6)
    pk_l, pk_b, pk_d = "#FFCBD8", "#FF9EB5", "#D9667F"
    edge = blend(pk_d, INK, .4)
    legs = lin(0, 72, 0, 91, [(0, "#EE8CA6"), (1, "#D9667F")])
    # orejas y patas por detras
    body(s, pcmds([(56, 38), (63, 14), (80, 40)]), lin(56, 14, 80, 40, [(0, "#FFB6C8"), (1, "#E87E9A")]), edge, grow=4, ring=1.8)
    s.path(pcmds([(61, 33), (64.5, 21), (73, 34)]), fill="#FFD2DD", op=.85)
    for x in (24, 54):
        s.rect(x, 72, 14, 19, 5.5, fill=legs, stroke=edge, sw=2)
    # cola
    s.path(Path().M(10, 62).C(1, 62, 1, 50, 8, 49).C(13, 48, 13, 55, 8.5, 55), stroke=edge, sw=4.4, cap=CAP_ROUND)
    s.path(Path().M(10, 62).C(1, 62, 1, 50, 8, 49).C(13, 48, 13, 55, 8.5, 55), stroke=pk_b, sw=2, cap=CAP_ROUND)
    # cuerpo
    s.ellipse(45, 58, 36, 29, fill=lin(16, 30, 76, 86, [(0, "#FFD3DE"), (.5, pk_b), (1, "#E8738F")]), stroke=edge, sw=2.2)
    # hocico
    s.ellipse(81, 58, 10.5, 12.5, fill=lin(70, 46, 92, 70, [(0, "#FFC2D2"), (1, "#F58BA5")]), stroke=edge, sw=2.2)
    s.ellipse(78, 58.5, 1.9, 3.2, fill="#8A3A50")
    s.ellipse(85, 58.5, 1.9, 3.2, fill="#8A3A50")
    # ojo y mejilla
    s.circle(64, 48, 3.2, fill="#5A2A3A")
    s.circle(63, 47, 1.1, fill="#FFFFFF", op=.9)
    s.circle(64, 62, 5.5, fill="#FF6B8A", op=.4)
    # ranura y moneda
    s.rect(29, 31, 24, 6, 3, fill="#8A3A50")
    s.circle(41, 19, 9.5, fill=lin(32, 10, 50, 28, [(0, "#FFF0A8"), (1, "#EBA121")]), stroke="#B8731A", sw=2)
    s.circle(41, 19, 5.5, stroke="#C98A14", sw=1.4, op=.7)
    shine(s, 20, 41, 15, 7, -22, .6)
    return s.bake()


def cozy_sparkles():
    s = Scene()
    g = lin(14, 20, 76, 92, [(0, "#FFF6C8"), (.5, "#FFD046"), (1, "#EBA121")])
    body(s, sparkle_cmds(43, 57, 35, .3), g, GOLD_EDGE, grow=2.4, ring=2.0)
    body(s, sparkle_cmds(79, 22, 15, .3), lin(64, 8, 94, 36, [(0, "#FFF6C8"), (1, "#FFC83D")]), GOLD_EDGE, grow=1.8, ring=1.8)
    body(s, sparkle_cmds(80, 82, 11, .3), lin(70, 72, 92, 94, [(0, "#FFF6C8"), (1, "#FFC83D")]), GOLD_EDGE, grow=1.4, ring=1.6)
    s.path(sparkle_cmds(43, 57, 15, .28), fill="#FFFFFF", op=.8)
    s.circle(11, 22, 2.6, fill="#FFE680", stroke="#C98A14", sw=1.2)
    s.circle(20, 88, 2.2, fill="#FFE680", stroke="#C98A14", sw=1.2)
    return s.bake()


def cozy_party():
    s = Scene()
    ground(s, 40, 30, y=93, ry=3.4)
    edge = blend("#D97A1A", INK, .42)
    tip, A, B = (13, 88), (44, 31), (69, 56)

    def at(f, p):
        return (tip[0] + (p[0] - tip[0]) * f, tip[1] + (p[1] - tip[1]) * f)

    body(s, pcmds([tip, A, B]), lin(10, 88, 69, 31, [(0, "#FFC060"), (.5, "#FFA133"), (1, "#E8841F")]), edge, grow=4, ring=1.9)
    for (f1, f2, col) in ((.22, .38, "#FF6B8A"), (.52, .68, "#5BC0EB")):
        s.path(pcmds([at(f1, A), at(f2, A), at(f2, B), at(f1, B)]), fill=col)
    s.path(pcmds([at(.0, A), at(.55, A), at(.55, B), at(.0, B)]), fill="#FFFFFF", op=0.001)
    # boca del cono
    mid = ((A[0] + B[0]) / 2.0, (A[1] + B[1]) / 2.0)
    ang = math.degrees(math.atan2(B[1] - A[1], B[0] - A[0]))
    ln = math.hypot(B[0] - A[0], B[1] - A[1])
    with s.rotate(ang, mid[0], mid[1]):
        s.ellipse(mid[0], mid[1], ln / 2.0 + 1, 6.5, fill="#B9601A", stroke=edge, sw=2)
        s.ellipse(mid[0], mid[1] + .4, ln / 2.0 - 3, 4.3, fill="#7A3A12", op=.9)
    s.path(Path().M(20, 76).L(40, 52), stroke="#FFFFFF", sw=2.6, op=.5, cap=CAP_ROUND)
    # confeti
    s.path(Path().M(58, 38).C(60, 26, 70, 28, 72, 17).C(73, 12, 78, 11, 80, 14), stroke="#FF6B8A", sw=3.4, cap=CAP_ROUND)
    s.path(Path().M(66, 44).C(78, 42, 80, 34, 88, 32), stroke="#5BC0EB", sw=3.4, cap=CAP_ROUND)
    s.path(Path().M(74, 52).C(82, 56, 86, 52, 90, 58), stroke="#FFD046", sw=3.4, cap=CAP_ROUND)
    for (x, y, c) in ((50, 16, "#FFD046"), (86, 20, "#FF6B8A"), (90, 44, "#FF6B8A"), (76, 30, "#5BC0EB"), (62, 9, "#5BC0EB")):
        s.circle(x, y, 3.8, fill=c, stroke=darker(c, .35), sw=1.2)
    with s.rotate(35, 82, 74):
        s.rect(78, 71, 8, 5, 1.4, fill="#FFD046", stroke="#C98A14", sw=1)
    with s.rotate(-30, 36, 18):
        s.rect(32, 15.5, 8, 5, 1.4, fill="#FF6B8A", stroke=darker("#FF6B8A", .35), sw=1)
    gold_spark(s, 76, 12, 6.5, top="#FFFFFF", bot="#FFD25A")
    return s.bake()


def cozy_dice():
    s = Scene()
    ground(s, 50, 31, y=93, ry=3.4)
    edge = blend("#6C63FF", INK, .45)
    with s.rotate(-8, 50, 50):
        s.rect(16, 18, 68, 68, 19, fill=lin(0, 18, 0, 86, [(0, "#A9A3FA"), (1, "#6C63FF")]), stroke=edge, sw=2.2)
        s.rect(16, 14, 68, 68, 19, fill=lin(18, 14, 84, 82, [(0, "#FFFFFF"), (.6, "#F2F0FC"), (1, "#D9D5F5")]), stroke=edge, sw=2.2)
        for (x, y) in ((34, 32), (66, 32), (50, 48), (34, 64), (66, 64)):
            s.circle(x, y + 1, 6.8, fill="#4A42C9", op=.3)
            s.circle(x, y, 6.4, fill=lin(x - 6, y - 6, x + 6, y + 6, [(0, "#8F88FF"), (1, "#5048E0")]))
            s.circle(x - 2.2, y - 2.4, 1.8, fill="#FFFFFF", op=.6)
        s.path(Path().M(24, 26).Q(26, 20, 34, 19), stroke="#FFFFFF", sw=2.6, op=.9, cap=CAP_ROUND)
    gold_spark(s, 87, 16, 6, top="#FFFFFF", bot="#FFD25A")
    return s.bake()


def cozy_crown():
    s = Scene()
    ground(s, 50, 34, y=91, ry=3.4)
    with s.translate(0, 3):
        with s.scale(.86, .86, 50, 54):
            d = "M 12 78 L 6 30 L 32 54 L 50 18 L 68 54 L 94 30 L 88 78 Z"
            body(s, d, lin(10, 18, 90, 80, [(0, "#FFEE9C"), (.5, GOLD_B), (1, "#E5A024")]), GOLD_EDGE, grow=5, ring=2.0)
            s.rect(10, 76, 80, 14, 6, fill=lin(0, 76, 0, 90, [(0, "#FFE680"), (1, "#E2A020")]), stroke=GOLD_EDGE, sw=2.2)
            for (x, c) in ((30, "#FF6B8A"), (50, "#5BC0EB"), (70, "#FF6B8A")):
                s.circle(x, 83, 5.4, fill=lin(x - 5, 78, x + 5, 89, [(0, lighter(c, .5)), (1, c)]), stroke=darker(c, .4), sw=1.4)
                s.circle(x - 1.6, 81.2, 1.3, fill="#FFFFFF", op=.8)
            for (x, y) in ((6, 28), (50, 15), (94, 28)):
                s.circle(x, y, 5.6, fill=lin(x - 5, y - 5, x + 5, y + 5, [(0, "#FFFFFF"), (1, "#FFE9A0")]), stroke=GOLD_EDGE, sw=1.8)
            shine(s, 26, 42, 8, 22, 12, .5)
    gold_spark(s, 82, 40, 5, top="#FFFFFF", bot="#FFF0B0")
    return s.bake()


# ================================================================== POWER (7)

def pal_for(base):
    r, g, b = _rgb(base)
    return {"base": base, "light": blend(base, "#FFFFFF", .5),
            "dark": "#%02X%02X%02X" % (int(r * .68), int(g * .68), int(b * .68))}


EDGE = mix("$dark", INK, .38)
PGOLD_G = lin(0, 0, 100, 100, [(0, "#FFF6C8"), (1, "#FFD25A")])


def tri_grad(x1, y1, x2, y2):
    return lin(x1, y1, x2, y2, [(0, "$light"), (.5, "$base"), (1, "$dark")])


def pbody(s, cmds, fill, grow=3.0, ring=2.0):
    s.path(cmds, stroke=EDGE, sw=grow + 2 * ring, join=JOIN_ROUND)
    s.path(cmds, fill=fill, stroke=fill, sw=grow, join=JOIN_ROUND)


def pspark(s, cx, cy, r, squeeze=.24):
    gold_spark(s, cx, cy, r, squeeze=squeeze, grow=.8, ring=1.1)


def power_undo():
    s = Scene(pal=pal_for("#E07A5F"))
    cx, cy, r = 50, 55, 28
    s.circle(cx, cy, 41, fill="$base", op=.13)
    g = tri_grad(18, 20, 82, 92)
    a0, a1 = 74, -140
    ex, ey = cx + r * math.cos(math.radians(a1)), cy + r * math.sin(math.radians(a1))
    d = (math.sin(math.radians(a1)), -math.cos(math.radians(a1)))
    n = (-d[1], d[0])
    tip = (ex + d[0] * 17, ey + d[1] * 17)
    back = (ex - d[0] * 5, ey - d[1] * 5)
    hw = 16
    head = pcmds([tip, (back[0] + n[0] * hw, back[1] + n[1] * hw), (back[0] - n[0] * hw, back[1] - n[1] * hw)])
    s.arc_stroke(cx, cy, r, a0, a1, EDGE, 13 + 4.4)
    s.path(head, stroke=EDGE, sw=3 + 4.4, join=JOIN_ROUND)
    s.arc_stroke(cx, cy, r, a0, a1, g, 13)
    s.path(head, fill=g, stroke=g, sw=3, join=JOIN_ROUND)
    s.arc_stroke(cx, cy, r - 2, -75, -35, "#FFFFFF", 3, op=.6)
    s.arc_stroke(cx, cy, r - 2, 10, 55, "#FFFFFF", 3, op=.35)
    # reloj
    s.circle(cx, cy, 16, fill="#FFFFFF", op=.97)
    s.circle(cx, cy, 16, stroke="$dark", sw=2.2, op=.6)
    s.line(cx, cy, cx - 4, cy - 10.5, "$dark", 3.3)
    s.line(cx, cy, cx, cy - 7, "$dark", 3.3)
    s.line(cx, cy, cx + 7, cy + 3, "$dark", 3.3)
    s.circle(cx, cy, 2.6, fill="$dark")
    pspark(s, 82, 22, 7)
    return s.bake()


def power_wand():
    s = Scene(pal=pal_for("#6B9E86"))
    A, B = (20, 85), (62, 43)

    def P(t):
        return (A[0] + (B[0] - A[0]) * t, A[1] + (B[1] - A[1]) * t)

    w = 13
    s.line(A[0], A[1], B[0], B[1], EDGE, w + 4.4)
    s.line(A[0], A[1], B[0], B[1],
           lin(A[0], A[1], B[0], B[1], [(0, "$dark"), (.5, "$base"), (.8, "$light"), (.82, "#FFFFFF"), (1, "#EDE8F5")]), w)
    # anillo dorado
    p0, p1 = P(.2), P(.31)
    s.line(p0[0], p0[1], p1[0], p1[1], PGOLD_G, w + 3, cap=CAP_BUTT)
    nx, ny = .707, .707
    for p in (p0, p1):
        s.line(p[0] - nx * 8.4, p[1] - ny * 8.4, p[0] + nx * 8.4, p[1] + ny * 8.4, "#B8861A", 1.6, cap=CAP_BUTT)
    q0, q1 = P(.06), P(.74)
    s.line(q0[0] - 2.4, q0[1] - 2.4, q1[0] - 2.4, q1[1] - 2.4, "#FFFFFF", 2.6, op=.5)
    # estrella de la punta
    sp = sparkle_cmds(71, 31, 22, .26)
    s.circle(71, 31, 20, fill="#FFD25A", op=.2)
    body(s, sp, lin(50, 9, 92, 53, [(0, "#FFF6C8"), (1, "#FFC83D")]), "#C98A14", grow=1.2, ring=1.4)
    s.path(sparkle_cmds(71, 31, 9, .3), fill="#FFFFFF", op=.85)
    pspark(s, 87, 62, 6)
    pspark(s, 44, 20, 6)
    pspark(s, 12, 62, 4.5)
    for (x, y, r, o) in ((28, 66, 2.4, .55), (36, 74, 1.8, .45), (12, 76, 1.8, .5)):
        s.circle(x, y, r, fill="$light", op=o)
    return s.bake()


def power_merge():
    s = Scene(pal=pal_for("#E0A93B"))
    lc, rc, r = (36, 63), (64, 63), 22
    s.circle(50, 58, 42, fill="$base", op=.17)
    for (c, mid) in ((lc, "$base"), (rc, mix("$base", "$dark", .3))):
        s.circle(c[0], c[1], r, fill=rad(c[0] - 7, c[1] - 9, 36, [(0, "$light"), (.5, mid), (1, "$dark")]), stroke=EDGE, sw=2.2)
    # lente de union
    h = math.sqrt(r * r - 14 * 14)
    lens = Path().M(50, 63 - h).Q(62, 63, 50, 63 + h).Q(38, 63, 50, 63 - h).Z().cmds
    s.path(lens, fill="#FFFFFF", op=.96)
    s.path(lens, fill="#FFD25A", op=.55)
    s.circle(lc[0] - 9, lc[1] - 10, 4.2, fill="#FFFFFF", op=.75)
    s.circle(rc[0] - 7, rc[1] - 10, 3.4, fill="#FFFFFF", op=.6)
    # chispa de fusion
    s.line(50, 37, 50, 41, "$dark", 2, op=.0001)
    s.circle(50, 24, 14, fill="#FFD25A", op=.22)
    pspark(s, 50, 24, 15, .24)
    pspark(s, 82, 38, 5.5)
    pspark(s, 19, 40, 5)
    return s.bake()


def power_broom():
    s = Scene(pal=pal_for("#E07A5F"))
    with s.translate(7, -2):
        with s.scale(.8, .8, 50, 50):
            with s.rotate(36, 50, 50):
                s.line(50, 6, 50, 58, EDGE, 8 + 4.4)
                s.line(50, 6, 50, 58, lin(0, 6, 0, 58, [(0, "#E3BB8C"), (1, "#A9754A")]), 8)
                head = pcmds([(36, 56), (64, 56), (76, 92), (24, 92)])
                pbody(s, head, lin(0, 56, 0, 92, [(0, "$light"), (.45, "$base"), (1, "$dark")]), grow=4, ring=2.2)
                for i in range(7):
                    x0 = 33 + i * 5.6
                    x1 = 27 + i * 7.7
                    s.line(x0, 68, x1, 90, "$dark", 1.8, op=.45)
                s.rect(33, 49, 34, 12, 5, fill=lin(0, 49, 0, 61, [(0, "#FFF0B8"), (1, "#E8B04A")]), stroke="#B8861A", sw=1.8)
                s.rect(37, 52, 26, 2.6, 1.3, fill="#FFFFFF", op=.65)
    for (x, y, r, o) in ((14, 84, 4.2, .5), (7, 75, 3, .4), (22, 90, 2.4, .45)):
        s.circle(x, y, r, fill="$base", op=o)
    pspark(s, 80, 26, 7)
    pspark(s, 88, 56, 4.5)
    return s.bake()


def power_link():
    s = Scene(pal=pal_for("#6C63FF"))
    tile = lin(0, 40, 0, 78, [(0, "$light"), (1, "$base")])
    # puente
    bridge = Path().M(25, 44).Q(50, 6, 75, 44)
    s.path(bridge, stroke=EDGE, sw=6.5 + 4, cap=CAP_ROUND)
    s.path(bridge, stroke=lin(20, 0, 80, 0, [(0, "$base"), (.5, "$light"), (1, "$base")]), sw=6.5, cap=CAP_ROUND)
    for left in (True, False):
        x = 9 if left else 61
        s.rect(x, 42, 30, 36, 10, fill=tile, stroke=EDGE, sw=2.2)
        s.rect(x + 4, 46, 22, 5, 2.5, fill="#FFFFFF", op=.45)
        cx = x + 15
        dr = 1 if left else -1
        w = 4.2
        s.line(cx - 6 * dr, 61, cx + 6 * dr, 61, "#FFFFFF", w)
        s.line(cx + 6 * dr, 61, cx + 1.5 * dr, 55.5, "#FFFFFF", w)
        s.line(cx + 6 * dr, 61, cx + 1.5 * dr, 66.5, "#FFFFFF", w)
    s.circle(50, 61, 13, fill="#FFD25A", op=.25)
    pspark(s, 50, 61, 11, .25)
    pspark(s, 50, 17, 5)
    pspark(s, 88, 86, 5.5)
    return s.bake()


def power_freeze():
    s = Scene(pal=pal_for("#4FA8DC"))
    cx, cy = 50, 52
    s.circle(cx, cy, 41, fill="$base", op=.14)
    L = 35
    segs = []
    for k in range(6):
        ang = math.radians(-90 + 60 * k)
        ca, sa = math.cos(ang), math.sin(ang)
        segs.append(((cx, cy), (cx + L * ca, cy + L * sa), 7.2))
        for (t, ln) in ((20, 11.5), (29, 7.5)):
            px, py = cx + t * ca, cy + t * sa
            for sg in (-1, 1):
                a2 = ang + sg * math.radians(52)
                segs.append(((px, py), (px + ln * math.cos(a2), py + ln * math.sin(a2)), 5.2))
    g = lin(14, 16, 86, 90, [(0, "$light"), (.5, "$base"), (1, "$dark")])
    for (a, b, w) in segs:
        s.line(a[0], a[1], b[0], b[1], EDGE, w + 4.2)
    for (a, b, w) in segs:
        s.line(a[0], a[1], b[0], b[1], g, w)
    hexa = spts(cx, cy, 11.5, 11.5, n=3, rot=-90)
    pbody(s, pcmds(hexa), lin(cx - 11, cy - 11, cx + 11, cy + 11, [(0, "#FFFFFF"), (1, "$light")]), grow=2, ring=2)
    s.path(pcmds(spts(cx, cy, 5.2, 5.2, n=3, rot=-90)), fill="$base", op=.55)
    for k in range(6):
        ang = math.radians(-90 + 60 * k)
        s.circle(cx + (L + 1) * math.cos(ang), cy + (L + 1) * math.sin(ang), 3.4, fill="#FFFFFF", stroke=EDGE, sw=1.4)
    for (x, y, r) in ((84, 20, 6), (14, 82, 5)):
        body(s, sparkle_cmds(x, y, r, .24), "#FFFFFF", mix("$dark", INK, .2), grow=.8, ring=1.1)
    return s.bake()


def power_extratime():
    s = Scene(pal=pal_for("#4FB8A8"))
    c = (45, 58)
    s.rect(38, 14, 14, 11, 3.5, fill=lin(0, 14, 0, 25, [(0, "$light"), (1, "$dark")]), stroke=EDGE, sw=2)
    s.rect(41, 22, 8, 7, 2, fill="$dark", stroke=EDGE, sw=1.6)
    s.circle(c[0], c[1], 31, fill=lin(15, 26, 76, 90, [(0, "$light"), (.5, "$base"), (1, "$dark")]), stroke=EDGE, sw=2.2)
    s.circle(c[0], c[1], 23.5, fill="#FFFFFF", op=.97)
    s.circle(c[0], c[1], 23.5, stroke="$dark", sw=2, op=.35)
    for i in range(4):
        a = math.radians(i * 90 - 90)
        s.line(c[0] + 18 * math.cos(a), c[1] + 18 * math.sin(a), c[0] + 21.5 * math.cos(a), c[1] + 21.5 * math.sin(a), "$dark", 2.4, op=.6)
    s.line(c[0], c[1], c[0], c[1] - 15, "$dark", 3.6)
    s.line(c[0], c[1], c[0] + 10, c[1] + 6, "$dark", 3.6)
    s.circle(c[0], c[1], 3.4, fill="$dark")
    s.arc_stroke(c[0], c[1], 27.5, 196, 246, "#FFFFFF", 3.2, op=.55)
    # insignia +
    s.circle(75, 27, 18.5, fill="#FFFFFF")
    s.circle(75, 27, 15.5, fill=lin(0, 12, 0, 42, [(0, "$base"), (1, "$dark")]), stroke=EDGE, sw=1.6)
    s.line(75, 19.5, 75, 34.5, "#FFFFFF", 5.2)
    s.line(67.5, 27, 82.5, 27, "#FFFFFF", 5.2)
    pspark(s, 87, 68, 5.5)
    pspark(s, 16, 20, 4.5)
    return s.bake()


def build():
    icons = {
        "cozy.star": cozy_star(), "cozy.coin": cozy_coin(), "cozy.gem": cozy_gem(), "cozy.flame": cozy_flame(),
        "cozy.trophy": cozy_trophy(), "cozy.chest": cozy_chest(), "cozy.heart": cozy_heart(), "cozy.gift": cozy_gift(),
        "cozy.bolt": cozy_bolt(), "cozy.timer": cozy_timer(), "cozy.shield": cozy_shield(), "cozy.piggy": cozy_piggy(),
        "cozy.sparkles": cozy_sparkles(), "cozy.party": cozy_party(), "cozy.dice": cozy_dice(), "cozy.crown": cozy_crown(),
        "power.undo": power_undo(), "power.wand": power_wand(), "power.merge": power_merge(), "power.broom": power_broom(),
        "power.link": power_link(), "power.freeze": power_freeze(), "power.extratime": power_extratime(),
    }
    return {"icons": icons}
