"""
Paquete fx: particulas y destellos sueltos de ParDos (iPhone).
Cada dibujo es una particula centrada en 100x100 (ocupa unos 70x70), ligera (1 a 8 formas),
y se tinta con las ranuras $c (color principal) y $c2 (secundario).
"""
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, ".."))   # artlib
sys.path.insert(0, HERE)                        # _looks, _kit
from artlib import *  # noqa: F401,F403

W = "#FFFFFF"
INK = "#2B1B3A"
SHADE = "#7A3367"


# ------------------------------------------------------------------ ayudas de color

def lite(c, t=0.45):
    return mix(c, W, t)


def deep(c, t=0.3):
    return mix(c, SHADE, t)


def edge(c, t=0.45):
    return mix(c, INK, t)


def g2(c, x1=30, y1=20, x2=72, y2=84, a=0.5, b=0.22):
    """Degradado de dos tonos: luz arriba-izquierda, sombra abajo-derecha."""
    return lin(x1, y1, x2, y2, [(0, lite(c, a)), (1, deep(c, b))])


def new(c, c2):
    return Scene(100, 100, pal={"c": c, "c2": c2})


# ------------------------------------------------------------------ ayudas de geometria

def _xf(cmds, rot=0.0, cx=0.0, cy=0.0, dx=0.0, dy=0.0):
    a = math.radians(rot)
    ca, sa = math.cos(a), math.sin(a)

    def p(x, y):
        return (cx + (x - cx) * ca - (y - cy) * sa + dx, cy + (x - cx) * sa + (y - cy) * ca + dy)

    out = []
    for c in cmds:
        if c[0] == "Z":
            out.append(c)
        elif c[0] in ("M", "L"):
            x, y = p(c[1], c[2])
            out.append((c[0], x, y))
        else:
            x1, y1 = p(c[1], c[2])
            x2, y2 = p(c[3], c[4])
            x, y = p(c[5], c[6])
            out.append(("C", x1, y1, x2, y2, x, y))
    return out


def ell(cx, cy, rx, ry, rot=0.0):
    k = KAPPA
    base = [
        ("M", cx + rx, cy),
        ("C", cx + rx, cy + ry * k, cx + rx * k, cy + ry, cx, cy + ry),
        ("C", cx - rx * k, cy + ry, cx - rx, cy + ry * k, cx - rx, cy),
        ("C", cx - rx, cy - ry * k, cx - rx * k, cy - ry, cx, cy - ry),
        ("C", cx + rx * k, cy - ry, cx + rx, cy - ry * k, cx + rx, cy),
        ("Z",),
    ]
    return _xf(base, rot, cx, cy) if rot else base


def circles(items):
    out = []
    for (cx, cy, r) in items:
        out += ell(cx, cy, r, r)
    return out


def polys(lst):
    out = []
    for pts in lst:
        out.append(("M", pts[0][0], pts[0][1]))
        for (x, y) in pts[1:]:
            out.append(("L", x, y))
        out.append(("Z",))
    return out


def star_pts(cx, cy, r, n=5, inner=0.5, rot=-90.0):
    pts = []
    for i in range(n * 2):
        rr = r if i % 2 == 0 else r * inner
        a = math.radians(rot + i * 180.0 / n)
        pts.append((cx + rr * math.cos(a), cy + rr * math.sin(a)))
    return pts


def ring_pts(cx, cy, r, n, rot=0.0):
    return [(cx + r * math.cos(math.radians(rot + i * 360.0 / n)),
             cy + r * math.sin(math.radians(rot + i * 360.0 / n))) for i in range(n)]


def wedge(cx, cy, ang, r0, r1, w0, w1):
    a = math.radians(ang)
    ux, uy = math.cos(a), math.sin(a)
    px, py = -uy, ux
    return [(cx + ux * r0 + px * w0 / 2, cy + uy * r0 + py * w0 / 2),
            (cx + ux * r1 + px * w1 / 2, cy + uy * r1 + py * w1 / 2),
            (cx + ux * r1 - px * w1 / 2, cy + uy * r1 - py * w1 / 2),
            (cx + ux * r0 - px * w0 / 2, cy + uy * r0 - py * w0 / 2)]


# ------------------------------------------------------------------ destellos

def spark():
    s = new("#FFC94A", "#FFF1B8")
    s.circle(50, 50, 34, fill=rad(50, 50, 34, [(0, alpha("$c", 0.55)), (1, alpha("$c", 0))]))
    s.sparkle(50, 50, 38, squeeze=0.09, fill=rad(50, 50, 38, [(0, W), (0.3, lite("$c", 0.6)), (1, "$c")]))
    with s.rotate(45, 50, 50):
        s.sparkle(50, 50, 21, squeeze=0.1, fill=rad(50, 50, 21, [(0, W), (1, "$c2")]), op=0.95)
    s.circle(50, 50, 5.5, fill=W, op=0.95)
    return s.bake()


def sparkle():
    s = new("#FFD36E", "#FFF1B8")
    s.circle(50, 50, 36, fill=rad(50, 50, 36, [(0, alpha("$c", 0.5)), (1, alpha("$c", 0))]))
    s.sparkle(50, 50, 33, squeeze=0.3, fill=rad(50, 50, 33, [(0, W), (0.45, lite("$c", 0.5)), (1, "$c")]))
    s.sparkle(74, 26, 10, squeeze=0.24, fill=lite("$c2", 0.35))
    s.sparkle(25, 74, 7.5, squeeze=0.24, fill=lite("$c2", 0.2))
    return s.bake()


def star():
    s = new("#FFD04A", "#FFF1B8")
    cx, cy, r = 50, 53, 37
    s.star(cx, cy, r, 5, 0.5, fill=g2("$c", 28, 16, 74, 86, 0.3, 0.3), stroke=mix("$c", "#9A4A12", 0.62), sw=1.6)
    pts = star_pts(cx, cy, r, 5, 0.5)
    lights, darks = [], []
    for i in range(5):
        tip = pts[2 * i]
        lights.append([(cx, cy), tip, pts[(2 * i - 1) % 10]])
        darks.append([(cx, cy), tip, pts[(2 * i + 1) % 10]])
    s.path(polys(lights), fill=W, op=0.26)
    s.path(polys(darks), fill="#C4561A", op=0.2)
    with s.rotate(-20, 44, 36):
        s.ellipse(44, 36, 3.6, 7.5, fill=W, op=0.7)
    return s.bake()


def star_soft():
    s = new("#FFD66E", "#FFF1B8")
    s.circle(50, 50, 38, fill=rad(50, 50, 38, [(0, alpha("$c", 0.5)), (1, alpha("$c", 0))]))
    p = rad(46, 44, 40, [(0, lite("$c", 0.75)), (0.5, lite("$c", 0.25)), (1, "$c")])
    s.star(50, 53, 27, 5, 0.58, fill=p, stroke=p, sw=10)
    with s.rotate(-25, 42, 38):
        s.ellipse(42, 38, 4.2, 8, fill=W, op=0.6)
    return s.bake()


# ------------------------------------------------------------------ burbujas, petalos, corazones

def bubble():
    s = new("#7FD6F5", "#FFFFFF")
    s.circle(50, 50, 33, fill=rad(42, 40, 44, [(0, alpha(W, 0.30)), (0.7, alpha("$c", 0.2)), (1, alpha("$c", 0.5))]),
             stroke=alpha(mix("$c", INK, 0.12), 0.9), sw=2.2)
    s.arc_stroke(50, 50, 24, 195, 255, W, 4.2, op=0.85)
    s.arc_stroke(50, 50, 25, 20, 60, lite("$c", 0.55), 3.2, op=0.7)
    s.circle(67, 34, 3.2, fill=W, op=0.75)
    return s.bake()


def petal():
    s = new("#F4A8BC", "#FFE0E8")
    with s.rotate(28, 50, 50):
        with s.scale(0.94, 0.94, 50, 50):
            body = "M50 86 C30 72 20 42 38 22 C45 14 55 14 62 22 C80 42 70 72 50 86 Z"
            s.path(body, fill=lin(36, 16, 66, 86, [(0, lite("$c", 0.6)), (0.55, "$c"), (1, deep("$c", 0.2))]),
                   stroke=edge("$c"), sw=1.4, join=JOIN_ROUND)
            s.curve([(50, 80), (49, 58), (50, 32)], stroke=deep("$c", 0.3), sw=1.4, op=0.5)
            with s.rotate(-12, 42, 36):
                s.ellipse(42, 36, 4.2, 10, fill=W, op=0.55)
    return s.bake()


def petal_cherry():
    s = new("#FFB3C9", "#FFE6EE")
    with s.rotate(-16, 50, 50):
        with s.scale(0.95, 0.95, 50, 50):
            body = ("M50 86 C30 70 17 44 27 26 C33 16 45 16 50 28 C55 16 67 16 73 26 "
                    "C83 44 70 70 50 86 Z")
            s.path(body, fill=lin(50, 88, 50, 18, [(0, deep("$c", 0.12)), (0.55, "$c"), (1, lite("$c", 0.65))]),
                   stroke=edge("$c", 0.4), sw=1.4, join=JOIN_ROUND)
            s.path("M50 80 C48 66 44 52 38 38 M50 80 C50 62 50 48 50 36 M50 80 C52 66 56 52 62 38",
                   stroke=deep("$c", 0.3), sw=1.1, op=0.45, cap=CAP_ROUND)
            with s.rotate(-18, 36, 34):
                s.ellipse(36, 34, 3.6, 8, fill=W, op=0.6)
    return s.bake()


def heart():
    s = new("#E8586D", "#FFB3C1")
    body = ("M50 86 C20 66 10 46 17 32 C24 18 42 18 50 34 C58 18 76 18 83 32 "
            "C90 46 80 66 50 86 Z")
    s.path(body, fill=g2("$c", 22, 18, 80, 88, 0.45, 0.22), stroke=edge("$c"), sw=1.5, join=JOIN_ROUND)
    with s.rotate(-35, 30, 34):
        s.ellipse(30, 34, 4.4, 9, fill=W, op=0.65)
    s.circle(42, 27, 2.4, fill=W, op=0.6)
    return s.bake()


def heart_small():
    s = new("#F07A93", "#FFD3DD")
    with s.rotate(12, 50, 50):
        body = ("M50 82 C18 62 14 36 30 28 C41 23 48 31 50 38 C52 31 59 23 70 28 "
                "C86 36 82 62 50 82 Z")
        s.path(body, fill=lite("$c", 0.7), stroke=lite("$c", 0.7), sw=7, join=JOIN_ROUND)
        s.path(body, fill=g2("$c", 22, 24, 78, 84, 0.45, 0.2), stroke=edge("$c", 0.35), sw=1.2, join=JOIN_ROUND)
        with s.rotate(-30, 32, 40):
            s.ellipse(32, 40, 3.6, 7.5, fill=W, op=0.7)
    return s.bake()


# ------------------------------------------------------------------ confeti y serpentina

def confetti_a():
    s = new("#E07A5F", "#F2C14E")
    with s.rotate(32, 50, 50):
        s.rect(35, 15, 30, 70, r=4.5, fill=lin(35, 15, 65, 85, [(0, lite("$c", 0.38)), (1, deep("$c", 0.12))]),
               stroke=edge("$c", 0.4), sw=1.3)
        s.path(polys([[(35, 57), (65, 44), (65, 80), (60.5, 85), (39.5, 85), (35, 80)]]),
               fill=lin(35, 44, 65, 85, [(0, lite("$c2", 0.3)), (1, deep("$c2", 0.15))]),
               stroke=edge("$c2", 0.4), sw=1.3, join=JOIN_ROUND)
        s.line(40, 22, 40, 44, W, 3.2, op=0.55)
    return s.bake()


def confetti_b():
    s = new("#F2B544", "#D9822B")
    with s.rotate(-24, 50, 50):
        s.ellipse(50, 57, 31, 22, fill=mix("$c", "$c2", 0.8), stroke=edge("$c2", 0.4), sw=1.2)
        s.ellipse(50, 50, 31, 22, fill=lin(22, 30, 78, 72, [(0, lite("$c", 0.45)), (1, "$c")]),
                  stroke=edge("$c", 0.4), sw=1.3)
        s.ellipse(50, 50, 21, 13, fill=None, stroke=lite("$c", 0.65), sw=1.5, op=0.7)
        s.ellipse(39, 43, 8, 3.2, fill=W, op=0.65)
    return s.bake()


def confetti_c():
    s = new("#4DB6A0", "#B6E8D8")
    with s.rotate(16, 50, 50):
        pts = [(50, 18), (84, 76), (16, 76)]
        p = lin(24, 18, 76, 82, [(0, lite("$c", 0.45)), (1, deep("$c", 0.12))])
        s.poly(pts, fill=p, stroke=p, sw=7, join=JOIN_ROUND)
        s.poly([(50, 22), (80, 74), (50, 74)], fill=SHADE, op=0.13)
        s.poly(pts, fill=None, stroke=edge("$c", 0.35), sw=1.2, join=JOIN_ROUND, op=0.0001) if False else None
        s.line(44, 38, 30, 66, W, 3.2, op=0.55)
    return s.bake()


def ribbon():
    s = new("#E8708F", "#F7B6C8")

    def centre(u):
        return (14 + 72 * u, 50 + 17 * math.sin(2 * math.pi * 1.25 * u + 0.35))

    def half(u):
        return 1.2 + 8.5 * abs(math.cos(4 * math.pi * u)) ** 0.8

    def lobe(ua, ub, steps=14):
        left, right = [], []
        for i in range(steps + 1):
            u = ua + (ub - ua) * i / steps
            x, y = centre(u)
            x2, y2 = centre(min(1.0, u + 0.002))
            x1, y1 = centre(max(0.0, u - 0.002))
            dx, dy = x2 - x1, y2 - y1
            ln = math.hypot(dx, dy) or 1.0
            nx, ny = -dy / ln, dx / ln
            h = half(u)
            left.append((x + nx * h, y + ny * h))
            right.append((x - nx * h, y - ny * h))
        return left + right[::-1]

    cuts = [0.0, 0.125, 0.375, 0.625, 0.875, 1.0]
    face_a = lin(14, 30, 86, 72, [(0, lite("$c", 0.35)), (1, deep("$c", 0.12))])
    face_b = lin(14, 30, 86, 72, [(0, lite("$c2", 0.2)), (1, mix("$c2", "$c", 0.45))])
    with s.rotate(-10, 50, 50):
        for i in range(5):
            s.poly(lobe(cuts[i], cuts[i + 1]), fill=face_a if i % 2 == 0 else face_b,
                   stroke=edge("$c", 0.4), sw=1.2, join=JOIN_ROUND)
    return s.bake()


def bolt():
    s = new("#B27BFF", "#E6D2FF")
    s.circle(50, 50, 36, fill=rad(50, 50, 36, [(0, alpha("$c", 0.28)), (1, alpha("$c", 0))]))
    P = [(53, 10), (76, 10), (62, 38), (82, 38), (34, 92), (45, 58), (22, 58)]
    with s.scale(0.86, 0.86, 50, 50):
        s.poly(P, fill=lin(30, 12, 74, 90, [(0, lite("$c", 0.55)), (0.5, "$c"), (1, deep("$c", 0.25))]),
               stroke=edge("$c", 0.4), sw=1.6, join=JOIN_ROUND)
        s.poly([(53, 10), (62, 10), (46, 46), (33, 56), (22, 58)], fill=W, op=0.4)
        s.poly([(45, 58), (62, 38), (82, 38), (34, 92)], fill=SHADE, op=0.16)
    return s.bake()


def firework():
    s = new("#FF9E5E", "#FFD36E")
    cx = cy = 50
    longs = [wedge(cx, cy, a, 12, 38, 6.5, 2.4) for a in range(0, 360, 45)]
    shorts = [wedge(cx, cy, a, 14, 28, 4.5, 1.8) for a in range(22, 382, 45)]
    s.circle(50, 50, 14, fill=rad(50, 50, 14, [(0, alpha("$c2", 0.8)), (1, alpha("$c2", 0))]))
    s.path(polys(longs), fill=lin(20, 20, 80, 80, [(0, lite("$c", 0.3)), (1, deep("$c", 0.1))]),
           stroke=lite("$c", 0.1), sw=1.6, join=JOIN_ROUND)
    s.path(polys(shorts), fill="$c2", stroke="$c2", sw=1.4, join=JOIN_ROUND)
    tips = [(cx + 41 * math.cos(math.radians(a)), cy + 41 * math.sin(math.radians(a))) for a in range(0, 360, 45)]
    s.path(circles([(x, y, 2.4) for (x, y) in tips]), fill=lite("$c", 0.4))
    s.circle(50, 50, 6, fill=W, op=0.95)
    return s.bake()


# ------------------------------------------------------------------ hojas, nieve y flores

def leaf():
    s = new("#7FBF6A", "#D6F0B8")
    with s.rotate(38, 50, 50):
        with s.scale(0.92, 0.92, 50, 50):
            body = "M50 88 C22 72 20 36 50 12 C80 36 78 72 50 88 Z"
            s.path(body, fill=lin(26, 16, 76, 86, [(0, lite("$c", 0.45)), (1, deep("$c", 0.25))]),
                   stroke=edge("$c", 0.4), sw=1.4, join=JOIN_ROUND)
            s.path("M50 88 C78 72 80 36 50 12 L50 88 Z", fill=INK, op=0.12)
            s.path("M50 90 L50 24 M50 70 L36 58 M50 56 L34 42 M50 70 L64 58 M50 56 L66 42",
                   stroke=lite("$c", 0.6), sw=1.6, op=0.85, cap=CAP_ROUND)
            with s.rotate(-12, 38, 40):
                s.ellipse(38, 40, 3.4, 9, fill=W, op=0.5)
    return s.bake()


def leaf_round():
    s = new("#8CC46A", "#E2F5C4")
    with s.rotate(-20, 50, 50):
        s.curve([(50, 66), (49, 80), (52, 90)], stroke=deep("$c", 0.35), sw=3.6)
        body = "M50 80 C36 70 20 62 20 44 C20 28 34 17 50 17 C66 17 80 28 80 44 C80 62 64 70 50 80 Z"
        s.path(body, fill=lin(26, 18, 74, 78, [(0, lite("$c", 0.5)), (1, deep("$c", 0.2))]),
               stroke=edge("$c", 0.4), sw=1.4, join=JOIN_ROUND)
        s.path("M50 78 L50 28 M50 62 L36 50 M50 62 L64 50 M50 46 L40 36 M50 46 L60 36",
               stroke=lite("$c", 0.6), sw=1.5, op=0.85, cap=CAP_ROUND)
        with s.rotate(-30, 33, 33):
            s.ellipse(33, 33, 3.6, 8, fill=W, op=0.5)
    return s.bake()


def snowflake():
    s = new("#8FD0F2", "#F2FBFF")
    cx = cy = 50
    d = []
    for k in range(6):
        a = math.radians(-90 + 60 * k)
        ux, uy = math.cos(a), math.sin(a)
        d.append(("M", cx, cy))
        d.append(("L", cx + ux * 36, cy + uy * 36))
        for (r, ln, ang) in ((22, 11, 52), (30, 6.5, 52)):
            bx, by = cx + ux * r, cy + uy * r
            for sgn in (-1, 1):
                b = a + math.radians(sgn * ang)
                d.append(("M", bx, by))
                d.append(("L", bx + math.cos(b) * ln, by + math.sin(b) * ln))
    s.path(d, stroke=deep("$c", 0.25), sw=7, cap=CAP_ROUND, join=JOIN_ROUND)
    s.path(d, stroke="$c2", sw=3.4, cap=CAP_ROUND, join=JOIN_ROUND)
    s.poly(ring_pts(cx, cy, 8, 6, -90), fill="$c2", stroke=deep("$c", 0.25), sw=1.6, join=JOIN_ROUND)
    return s.bake()


def snow_dot():
    s = new("#CFEAFB", "#FFFFFF")
    s.circle(50, 50, 38, fill=rad(50, 50, 38, [(0, alpha("$c", 0.6)), (0.55, alpha("$c", 0.22)), (1, alpha("$c", 0))]))
    s.circle(50, 50, 26, fill=rad(42, 41, 32, [(0, "$c2"), (0.55, lite("$c", 0.35)), (1, mix("$c", "#7FA6D6", 0.3))]),
             stroke=alpha(mix("$c", "#5B7FB0", 0.6), 0.85), sw=1.6)
    s.ellipse(41, 40, 8, 5, fill=W, op=0.8)
    return s.bake()


def bat():
    s = new("#5B4A82", "#8A74B8")
    wing = "M44 45 C36 32 20 27 6 38 Q15 46 14 62 Q22 54 29 68 Q36 58 45 62 Z"
    body_col = lin(10, 30, 90, 72, [(0, mix("$c", W, 0.12)), (1, deep("$c", 0.25))])
    memb = lin(10, 30, 46, 70, [(0, lite("$c2", 0.1)), (1, mix("$c2", "$c", 0.55))])
    bones = "M42 45 L14 62 M44 47 L29 68"
    with s.scale(0.9, 0.9, 50, 52):
        s.path(wing, fill=memb, stroke=edge("$c", 0.3), sw=1.6, join=JOIN_ROUND)
        s.path(bones, stroke=deep("$c", 0.3), sw=1.4, op=0.5)
        with s.flip_x(50):
            s.path(wing, fill=memb, stroke=edge("$c", 0.3), sw=1.6, join=JOIN_ROUND)
            s.path(bones, stroke=deep("$c", 0.3), sw=1.4, op=0.5)
        s.path(polys([[(39, 36), (40, 19), (49, 31)], [(61, 36), (60, 19), (51, 31)]]),
               fill=body_col, stroke=body_col, sw=2, join=JOIN_ROUND)
        s.ellipse(50, 57, 9.5, 15, fill=body_col, stroke=edge("$c", 0.3), sw=1.4)
        s.circle(50, 42, 11.5, fill=body_col, stroke=edge("$c", 0.3), sw=1.4)
        s.path(circles([(45.5, 41, 2.3), (54.5, 41, 2.3)]), fill="#FFE066")
    return s.bake()


def flower():
    s = new("#F29BB0", "#FFD36E")
    for k in range(5):
        with s.rotate(72 * k, 50, 50):
            s.path(ell(50, 30, 13, 19), fill=lin(50, 12, 50, 48, [(0, lite("$c", 0.5)), (1, "$c")]),
                   stroke=edge("$c", 0.4), sw=1.3)
    s.circle(50, 50, 11.5, fill=rad(46, 46, 14, [(0, lite("$c2", 0.55)), (1, deep("$c2", 0.1))]),
             stroke=edge("$c2", 0.4), sw=1.3)
    s.path(circles([(46, 48, 1.6), (53, 46, 1.6), (51, 54, 1.6), (45, 54, 1.6)]), fill=deep("$c2", 0.35), op=0.7)
    return s.bake()


def marigold():
    s = new("#F2A31B", "#D9531E")
    outer = circles(ring_pts_c(50, 50, 24, 12, 11)) + ell(50, 50, 28, 28)
    s.path(outer, stroke=edge("$c", 0.45), sw=3.2, join=JOIN_ROUND)
    s.path(outer, fill=rad(44, 42, 44, [(0, lite("$c", 0.4)), (1, "$c")]))
    inner = circles(ring_pts_c(50, 50, 14, 8, 8.5, 15)) + ell(50, 50, 15, 15)
    s.path(inner, fill=rad(50, 50, 24, [(0, "$c2"), (1, mix("$c", "$c2", 0.35))]),
           stroke=deep("$c2", 0.2), sw=1.0, join=JOIN_ROUND, op=1.0)
    s.circle(50, 50, 7, fill=rad(47, 47, 9, [(0, lite("$c", 0.4)), (1, "$c")]), stroke=deep("$c2", 0.3), sw=1.0)
    s.ellipse(38, 35, 5, 2.6, fill=W, op=0.45)
    return s.bake()


def ring_pts_c(cx, cy, rr, n, r, rot=0.0):
    return [(x, y, r) for (x, y) in ring_pts(cx, cy, rr, n, rot)]


# ------------------------------------------------------------------ cielo y fuego

def meteor():
    s = new("#FFD36E", "#FF9E5E")
    hx, hy = 68, 68
    # cola ancha que se afina hacia atras (arriba a la izquierda)
    tail = "M%d %d C60 52 40 34 14 14 C38 42 52 62 %d %d Z" % (hx + 9, hy - 11, hx - 11, hy + 9)
    s.path(tail, fill=lin(hx, hy, 14, 14, [(0, alpha("$c2", 0.95)), (0.55, alpha("$c2", 0.5)), (1, alpha("$c2", 0))]))
    s.path("M%d %d C60 58 44 42 24 22 C42 46 54 60 %d %d Z" % (hx + 5, hy - 6, hx - 6, hy + 5),
           fill=lin(hx, hy, 24, 22, [(0, alpha("$c", 0.95)), (1, alpha("$c", 0))]))
    s.circle(hx, hy, 19, fill=rad(hx, hy, 19, [(0, alpha("$c", 0.55)), (1, alpha("$c", 0))]))
    s.circle(hx, hy, 11.5, fill=rad(hx - 2.5, hy - 2.5, 13, [(0, W), (0.5, lite("$c", 0.35)), (1, "$c")]),
             stroke=alpha(deep("$c", 0.3), 0.7), sw=1.3)
    s.path(circles([(47, 66, 2.6), (60, 82, 2.1), (28, 38, 2.0)]), fill=lite("$c", 0.3), op=0.95)
    return s.bake()


def ember():
    s = new("#FF8A3D", "#FFD36E")
    s.circle(50, 56, 34, fill=rad(50, 56, 34, [(0, alpha("$c", 0.5)), (1, alpha("$c", 0))]))
    flame = "M50 14 C58 30 74 42 72 62 C70 78 60 88 50 88 C40 88 30 78 28 62 C26 46 42 36 50 14 Z"
    s.path(flame, fill=lin(50, 14, 50, 88, [(0, lite("$c", 0.15)), (0.6, "$c"), (1, deep("$c", 0.3))]),
           stroke=deep("$c", 0.35), sw=1.3, join=JOIN_ROUND)
    s.path("M50 38 C56 50 63 58 62 70 C61 80 56 84 50 84 C44 84 39 80 38 70 C37 58 46 52 50 38 Z",
           fill=rad(50, 76, 28, [(0, W), (0.5, "$c2"), (1, lite("$c", 0.2))]))
    return s.bake()


def raindrop():
    s = new("#6FB8E8", "#DFF3FF")
    body = "M50 12 C58 28 74 44 74 62 C74 76 64 88 50 88 C36 88 26 76 26 62 C26 44 42 28 50 12 Z"
    s.path(body, fill=lin(30, 24, 72, 88, [(0, lite("$c", 0.5)), (0.55, "$c"), (1, deep("$c", 0.25))]),
           stroke=edge("$c", 0.4), sw=1.5, join=JOIN_ROUND)
    s.path("M38 52 C34 60 34 70 40 76", stroke=W, sw=4.2, cap=CAP_ROUND, op=0.7)
    s.circle(37, 44, 2.4, fill=W, op=0.75)
    s.arc_stroke(50, 62, 19, 40, 95, lite("$c", 0.55), 3, op=0.6)
    return s.bake()


# ------------------------------------------------------------------ gemas

def diamond():
    s = new("#7FD6F5", "#D6F3FF")
    g = [(14, 40), (32, 40), (50, 40), (68, 40), (86, 40)]
    t0, t1, tip = (31, 19), (69, 19), (50, 86)
    outline = [(t0[0], t0[1]), (t1[0], t1[1]), g[4], tip, g[0]]
    s.poly(outline, fill=lin(14, 19, 84, 86, [(0, lite("$c", 0.6)), (0.5, "$c"), (1, deep("$c", 0.25))]),
           stroke=edge("$c", 0.4), sw=1.6, join=JOIN_ROUND)
    s.path(polys([[g[0], t0, g[1]], [t0, t1, g[2]], [g[3], t1, g[4]], [g[1], g[2], tip]]), fill=W, op=0.34)
    s.path(polys([[t0, g[1], g[2]], [t1, g[2], g[3]], [g[0], g[1], tip], [g[3], g[4], tip]]), fill=INK, op=0.12)
    s.path("M14 40 L86 40 M31 19 L32 40 M69 19 L68 40 M32 40 L50 86 M68 40 L50 86 M50 40 L50 86",
           stroke=W, sw=1.1, op=0.5)
    return s.bake()


def crystal():
    s = new("#B79CF0", "#E6DAFF")
    for (bx, rot, mir) in ((30, -20, 1), (70, 20, -1)):
        with s.rotate(rot, bx, 86):
            q = [(bx - 9, 86), (bx - 9, 62), (bx, 50), (bx + 9, 62), (bx + 9, 86)]
            s.poly(q, fill=lin(bx - 9, 50, bx + 9, 86, [(0, lite("$c", 0.4)), (1, deep("$c", 0.28))]),
                   stroke=edge("$c", 0.4), sw=1.3, join=JOIN_ROUND)
            s.poly([(bx, 50), (bx + 9, 62), (bx + 9, 86), (bx, 86)], fill=SHADE, op=0.14)
    main = [(50, 11), (68, 29), (68, 71), (50, 89), (32, 71), (32, 29)]
    s.poly(main, fill=lin(32, 11, 68, 89, [(0, lite("$c", 0.55)), (0.5, "$c"), (1, deep("$c", 0.3))]),
           stroke=edge("$c", 0.4), sw=1.6, join=JOIN_ROUND)
    s.poly([(50, 11), (32, 29), (32, 71), (50, 89)], fill=W, op=0.26)
    s.poly([(50, 11), (68, 29), (68, 71), (50, 89)], fill=SHADE, op=0.16)
    s.path("M32 29 L50 41 L68 29 M50 41 L50 89", stroke=W, sw=1.2, op=0.45)
    s.path("M38 36 L38 64", stroke=W, sw=3, op=0.6)
    return s.bake()


def cloud():
    s = new("#CFE0F5", "#FFFFFF")
    blobs = circles([(34, 58, 15), (52, 46, 21), (70, 57, 15)])
    body = blobs + _rrect_cmds_list(20, 56, 62, 22, 11)
    s.path(body, stroke=edge("$c", 0.4), sw=3.4, join=JOIN_ROUND)
    s.path(body, fill=lin(0, 26, 0, 80, [(0, "$c2"), (0.5, lite("$c", 0.3)), (1, deep("$c", 0.12))]))
    s.ellipse(44, 36, 11, 5, fill=W, op=0.7)
    return s.bake()


def _rrect_cmds_list(x, y, w, h, r):
    k = r * KAPPA
    return [
        ("M", x + r, y),
        ("L", x + w - r, y),
        ("C", x + w - r + k, y, x + w, y + r - k, x + w, y + r),
        ("L", x + w, y + h - r),
        ("C", x + w, y + h - r + k, x + w - r + k, y + h, x + w - r, y + h),
        ("L", x + r, y + h),
        ("C", x + r - k, y + h, x, y + h - r + k, x, y + h - r),
        ("L", x, y + r),
        ("C", x, y + r - k, x + r - k, y, x + r, y),
        ("Z",),
    ]


# ------------------------------------------------------------------ musica y fiesta

def note():
    s = new("#8E7CC3", "#D9CFF5")
    heads = ell(33, 72, 12, 8.8, -22) + ell(69, 64, 12, 8.8, -22)
    bars = polys([[(41, 70), (41, 28), (45, 28), (45, 70)], [(77, 62), (77, 20), (81, 20), (81, 62)],
                  [(41, 26), (81, 17), (81, 31), (41, 40)]])
    col = lin(24, 17, 82, 82, [(0, lite("$c", 0.35)), (1, deep("$c", 0.25))])
    s.path(bars, fill=col, stroke=col, sw=1.2, join=JOIN_ROUND)
    s.path(heads, fill=col, stroke=edge("$c", 0.3), sw=1.2)
    s.line(42, 31, 78, 22.5, W, 2.2, op=0.45)
    with s.rotate(-22, 30, 69):
        s.ellipse(30, 69, 5, 2.2, fill=W, op=0.5)
    return s.bake()


def ghost_small():
    s = new("#ECE8FF", "#B8BCE8")
    body = ("M24 82 L24 42 C24 27 36 16 50 16 C64 16 76 27 76 42 L76 82 "
            "Q67 96 58.7 82 Q50 96 41.3 82 Q33 96 24 82 Z")
    s.path(body, fill=lin(30, 16, 74, 92, [(0, W), (0.55, "$c"), (1, "$c2")]),
           stroke=edge("$c2", 0.55), sw=1.6, join=JOIN_ROUND)
    s.ellipse(40, 44, 3.4, 4.8, fill="#3A3F66")
    s.ellipse(60, 44, 3.4, 4.8, fill="#3A3F66")
    s.ellipse(50, 57, 3.2, 4, fill="#3A3F66")
    s.path(circles([(32, 53, 4.2), (68, 53, 4.2)]), fill="#FF9EBB", op=0.55)
    s.circle(41, 40.5, 1.1, fill=W, op=0.9)
    s.circle(61, 40.5, 1.1, fill=W, op=0.9)
    return s.bake()


def pumpkin_small():
    s = new("#F28A1F", "#3E7C3A")
    lobes = ell(30, 57, 19, 25) + ell(70, 57, 19, 25) + ell(50, 57, 22, 27)
    s.path(lobes, stroke=edge("$c", 0.55), sw=3.2, join=JOIN_ROUND)
    s.path(lobes, fill=lin(0, 28, 0, 86, [(0, lite("$c", 0.35)), (0.55, "$c"), (1, deep("$c", 0.22))]))
    s.path("M40 32 C34 46 34 68 40 83 M60 32 C66 46 66 68 60 83",
           stroke=deep("$c", 0.4), sw=1.8, op=0.5, cap=CAP_ROUND)
    with s.rotate(10, 50, 28):
        s.rect(45, 15, 10, 17, r=3, fill=lin(45, 15, 55, 32, [(0, lite("$c2", 0.3)), (1, deep("$c2", 0.2))]),
               stroke=edge("$c2", 0.4), sw=1.2)
    s.path(polys([[(36, 52), (44, 52), (40, 45)], [(56, 52), (64, 52), (60, 45)]]),
           fill="#3A1F5C", stroke="#3A1F5C", sw=1.4, join=JOIN_ROUND)
    s.path("M38 64 L43 69 L47 64 L53 69 L57 64 L62 66", stroke="#3A1F5C", sw=2.4, join=JOIN_ROUND)
    s.ellipse(27, 44, 4, 7, fill=W, op=0.4)
    return s.bake()


def coin_spin():
    s = new("#F2C14E", "#B87510")
    s.ellipse(56, 52, 27, 35, fill=lin(40, 20, 80, 84, [(0, deep("$c", 0.15)), (1, "$c2")]), stroke=edge("$c2", 0.4), sw=1.3)
    s.ellipse(50, 50, 27, 35, fill=g2("$c", 28, 18, 72, 84, 0.55, 0.12), stroke=edge("$c2", 0.45), sw=1.5)
    s.ellipse(50, 50, 20.5, 28, fill=None, stroke=deep("$c", 0.3), sw=2, op=0.55)
    s.ellipse(48, 48, 20.5, 28, fill=None, stroke=lite("$c", 0.6), sw=1.2, op=0.6)
    with s.scale(0.75, 1.0, 50, 50):
        s.sparkle(50, 50, 17, squeeze=0.22, fill=lin(40, 36, 60, 64, [(0, deep("$c", 0.28)), (1, "$c2")]), op=0.85)
    s.path("M33 30 C28 38 26 48 28 58", stroke=W, sw=3.4, cap=CAP_ROUND, op=0.6)
    return s.bake()


def ring():
    s = new("#FFD36E", "#FFF3C4")
    s.circle(50, 50, 28, stroke=alpha("$c", 0.22), sw=17)
    s.circle(50, 50, 28, stroke=lin(24, 24, 76, 76, [(0, lite("$c", 0.5)), (1, "$c")]), sw=7)
    s.circle(50, 50, 28, stroke=lite("$c", 0.8), sw=1.8, op=0.9)
    s.arc_stroke(50, 50, 28, 200, 258, W, 2.4, op=0.9)
    s.sparkle(77, 23, 9, squeeze=0.22, fill="$c2")
    return s.bake()


def glow():
    s = new("#FFC96B", "#FFFFFF")
    s.circle(50, 50, 38, fill=rad(50, 50, 38, [(0, alpha("$c", 0.95)), (0.3, alpha("$c", 0.55)),
                                                (0.65, alpha("$c", 0.18)), (1, alpha("$c", 0))]))
    s.circle(50, 50, 13, fill=rad(50, 50, 13, [(0, alpha("$c2", 0.95)), (1, alpha("$c2", 0))]))
    return s.bake()


def build():
    icons = {
        "fx.spark": spark(),
        "fx.sparkle": sparkle(),
        "fx.star": star(),
        "fx.star_soft": star_soft(),
        "fx.bubble": bubble(),
        "fx.petal": petal(),
        "fx.petal_cherry": petal_cherry(),
        "fx.heart": heart(),
        "fx.heart_small": heart_small(),
        "fx.confetti_a": confetti_a(),
        "fx.confetti_b": confetti_b(),
        "fx.confetti_c": confetti_c(),
        "fx.ribbon": ribbon(),
        "fx.bolt": bolt(),
        "fx.firework": firework(),
        "fx.leaf": leaf(),
        "fx.leaf_round": leaf_round(),
        "fx.snowflake": snowflake(),
        "fx.snow_dot": snow_dot(),
        "fx.bat": bat(),
        "fx.flower": flower(),
        "fx.marigold": marigold(),
        "fx.meteor": meteor(),
        "fx.ember": ember(),
        "fx.raindrop": raindrop(),
        "fx.diamond": diamond(),
        "fx.crystal": crystal(),
        "fx.cloud": cloud(),
        "fx.note": note(),
        "fx.ghost_small": ghost_small(),
        "fx.pumpkin_small": pumpkin_small(),
        "fx.coin_spin": coin_spin(),
        "fx.ring": ring(),
        "fx.glow": glow(),
    }
    return {"icons": icons}
