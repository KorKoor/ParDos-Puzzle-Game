"""
Banners de perfil de ParDos (iPhone). Lienzo 300x100 (relacion 3:1; la app lo estira para llenar la tarjeta).

Cada patron se entrega en capas:
    banner.<PATRON>       base estatica (colinas, siluetas, suelo...)
    banner.<PATRON>.fx    capa movil (nubes, burbujas, petalos, luces...)
    banner.<PATRON>.fx2   solo si el modo es "twinkle": mismas formas en otras posiciones; la app alterna fx / fx2

Ranuras: $top $bottom $accent $ink. El degradado top->bottom lo pone la app; aqui solo se dibuja encima.
Todos los tonos salen del acento (y de top/bottom) para que se vean bien sobre banners claros y oscuros.

MODES (modo de animacion de la capa fx):
    drift   se desplaza en horizontal y se repite: x=0 y x=300 son equivalentes (las formas del borde se dibujan dos veces)
    rise    sube y se repite en vertical (y=0 e y=100 equivalentes)
    fall    baja y se repite en vertical
    sway    va y viene unos pocos pixeles
    twinkle la app alterna fx y fx2
"""
import math
import os
import random
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, ".."))   # artlib
sys.path.insert(0, HERE)
from artlib import *  # noqa: E402,F401,F403
from artlib import catmull  # noqa: E402

W, H = 300, 100

MODES = {
    "CHECKER": "twinkle", "DOTS": "drift", "HILLS": "drift", "PETALS": "fall", "LEAVES": "fall",
    "WAVES": "sway", "BUBBLES": "rise", "SNOW": "fall", "DIAMONDS": "twinkle", "RAYS": "twinkle",
    "STARS": "twinkle", "AURORA": "drift", "SKYLINE": "twinkle", "EMBERS": "rise", "BATS": "drift",
    "PUMPKINS": "twinkle", "MOUNTAINS": "drift", "FOREST": "twinkle", "CLOUDS": "drift", "HEARTS": "rise",
    "FIREWORKS": "twinkle", "GALAXY": "twinkle", "ZEN": "drift", "STRIPES": "drift", "CIRCUIT": "drift",
    "LANTERNS": "sway", "CONFETTI": "fall", "RAIN": "fall", "SUNSET": "drift", "CRYSTALS": "twinkle",
}

ORDER = ["CHECKER", "DOTS", "HILLS", "PETALS", "LEAVES", "WAVES", "BUBBLES", "SNOW", "DIAMONDS", "RAYS",
         "STARS", "AURORA", "SKYLINE", "EMBERS", "BATS", "PUMPKINS", "MOUNTAINS", "FOREST", "CLOUDS", "HEARTS",
         "FIREWORKS", "GALAXY", "ZEN", "STRIPES", "CIRCUIT", "LANTERNS", "CONFETTI", "RAIN", "SUNSET", "CRYSTALS"]

PAL = {"top": "#F3EFE6", "bottom": "#E8E0D0", "accent": "#B8A58A", "ink": "#3D405B"}
WHITE = "#FFFFFF"
DEEP = "#2B1B3A"     # sombra morada profunda (nunca negro puro)
NIGHT = "#0F0A22"    # siluetas nocturnas


# ------------------------------------------------------------------ utilidades

def new():
    return Scene(W, H, pal=PAL)


def A(a, c="$accent"):
    return alpha(c, a)


def lite(t, c="$accent"):
    return mix(c, WHITE, t)


def dark(t, c="$accent"):
    return mix(c, DEEP, t)


def smooth(t):
    t = max(0.0, min(1.0, t))
    return t * t * (3 - 2 * t)


def scatter(n, seed, x0=0, x1=W, y0=0, y1=H, md=18, tries=600, wx=False, wy=False):
    """Puntos repartidos con distancia minima (opcionalmente con vuelta en x / y)."""
    r = random.Random(seed)
    pts = []
    for _ in range(tries):
        x = r.uniform(x0, x1)
        y = r.uniform(y0, y1)
        ok = True
        for (px, py) in pts:
            dx, dy = abs(x - px), abs(y - py)
            if wx:
                dx = min(dx, W - dx)
            if wy:
                dy = min(dy, H - dy)
            if dx * dx + dy * dy < md * md:
                ok = False
                break
        if ok:
            pts.append((x, y))
            if len(pts) >= n:
                break
    return pts


def wrapx(fn, x, m, period=W):
    """Llama fn(x) y la copia necesaria para que la capa se repita sin costura en horizontal."""
    fn(x)
    if x - m < 0:
        fn(x + period)
    if x + m > period:
        fn(x - period)


def wrapy(fn, y, m, period=H):
    fn(y)
    if y - m < 0:
        fn(y + period)
    if y + m > period:
        fn(y - period)


def tcmds(cmds, deg=0.0, dx=0.0, dy=0.0, k=1.0, kx=None):
    """Transforma una lista de comandos (escala, giro y traslado) para juntar varias formas en un solo camino."""
    a = math.radians(deg)
    c, s_ = math.cos(a), math.sin(a)
    sx = k if kx is None else kx
    out = []

    def ap(x, y):
        x, y = x * sx, y * k
        return (x * c - y * s_ + dx, x * s_ + y * c + dy)

    for cmd in cmds:
        t = cmd[0]
        if t in ("M", "L"):
            x, y = ap(cmd[1], cmd[2])
            out.append((t, x, y))
        elif t == "C":
            p1 = ap(cmd[1], cmd[2])
            p2 = ap(cmd[3], cmd[4])
            p3 = ap(cmd[5], cmd[6])
            out.append(("C", p1[0], p1[1], p2[0], p2[1], p3[0], p3[1]))
        else:
            out.append(cmd)
    return out


def ell_cmds(cx, cy, rx, ry):
    kx, ky = rx * KAPPA, ry * KAPPA
    return [("M", cx + rx, cy), ("C", cx + rx, cy + ky, cx + kx, cy + ry, cx, cy + ry),
            ("C", cx - kx, cy + ry, cx - rx, cy + ky, cx - rx, cy), ("C", cx - rx, cy - ky, cx - kx, cy - ry, cx, cy - ry),
            ("C", cx + kx, cy - ry, cx + rx, cy - ky, cx + rx, cy), ("Z",)]


def wave_y(base, comps, x):
    return base + sum(a * math.sin(2 * math.pi * c * x / W + ph) for (a, c, ph) in comps)


def wave_pts(base, comps, step=20, x0=-20, x1=320):
    pts = []
    x = x0
    while x <= x1 + 0.01:
        pts.append((x, wave_y(base, comps, x)))
        x += step
    return pts


def ridge_cmds(pts, bottom=H + 3):
    c = catmull(pts)
    return [("M", pts[0][0], bottom), ("L", pts[0][0], pts[0][1])] + c[1:] + [("L", pts[-1][0], bottom), ("Z",)]


def hill(s, pts, fill, rim=None, rim_sw=0.8, bottom=H + 3):
    s.path(ridge_cmds(pts, bottom), fill=fill)
    if rim is not None:
        s.path(catmull(pts), stroke=rim, sw=rim_sw, cap=CAP_ROUND)


def glow(s, cx, cy, r, color, a=0.5, mid=None):
    st = [(0, alpha(color, a)), (1, alpha(color, 0))]
    if mid is not None:
        st = [(0, alpha(color, a)), (0.45, alpha(color, a * mid)), (1, alpha(color, 0))]
    s.circle(cx, cy, r, fill=rad(cx, cy, r, st))


def orb(s, cx, cy, r, c1, c2, shine=True):
    """Esfera con volumen suave (luz arriba-izquierda)."""
    s.circle(cx, cy, r, fill=rad(cx - r * 0.35, cy - r * 0.4, r * 1.5, [(0, c1), (1, c2)]))
    if shine:
        s.ellipse(cx - r * 0.35, cy - r * 0.45, r * 0.32, r * 0.2, fill=alpha(WHITE, 0.55))


def cloud(s, cx, cy, k=1.0, a=0.95, body=WHITE, shade=None):
    """Nube mullida: sombra suave debajo y bultos blancos."""
    sh = shade if shade is not None else alpha("$ink", 0.10)
    s.ellipse(cx + 1 * k, cy + 4.2 * k, 17 * k, 4.2 * k, fill=sh)
    s.rect(cx - 17 * k, cy - 2 * k, 34 * k, 8.5 * k, r=4.2 * k, fill=alpha(body, a))
    s.circle(cx - 7 * k, cy - 2.5 * k, 6.4 * k, fill=alpha(body, a))
    s.circle(cx + 2.5 * k, cy - 6 * k, 8.6 * k, fill=alpha(body, a))
    s.circle(cx + 11 * k, cy - 1.5 * k, 6.0 * k, fill=alpha(body, a))
    s.ellipse(cx - 1 * k, cy - 8.5 * k, 4.2 * k, 2.0 * k, fill=alpha(WHITE, 0.8 * a))


def sparkle_dot(s, x, y, r, c, a=1.0, core=True):
    """Destello de 4 puntas con nucleo blanco."""
    s.sparkle(x, y, r, fill=alpha(c, a))
    if core:
        s.circle(x, y, max(0.5, r * 0.22), fill=alpha(WHITE, min(1.0, a + 0.1)))


def crescent_cmds(R, a_deg, thick):
    """Luna creciente abierta hacia la derecha, centrada en (0,0): radio R, apertura a_deg, grosor (fraccion de R)."""
    a = math.radians(a_deg)
    px, py = R * math.cos(a), R * math.sin(a)
    xin = -R + thick * R
    d = xin - px
    ri = (-(R * math.sin(a)) ** 2 - d * d) / (2 * d)
    p = Path()
    p.M(px, -py)
    p.A(R, R, 0, 1, 0, px, py)
    p.A(ri, ri, 0, 0, 1, px, -py)
    p.Z()
    return p.cmds


# ------------------------------------------------------------------ 1. CHECKER

def checker():
    b = new()
    pitch = 20
    for r in range(5):
        for c in range(15):
            x, y = c * pitch, r * pitch
            t = smooth((x + 10) / 300.0)
            if (r + c) % 2 == 0:
                b.rect(x + 1, y + 1, pitch - 2, pitch - 2, r=4, fill=A(0.12 + 0.16 * t), stroke=A(0.22 + 0.22 * t), sw=0.8)
            else:
                b.rect(x + 1.5, y + 1.5, pitch - 3, pitch - 3, r=4, fill=alpha(WHITE, 0.05 + 0.08 * t))
    # destello diagonal suave
    b.poly([(70, 0), (120, 0), (70, 100), (20, 100)], fill=lin(20, 0, 120, 0, [(0, alpha(WHITE, 0)), (0.5, alpha(WHITE, 0.16)), (1, alpha(WHITE, 0))]))
    b.poly([(200, 0), (225, 0), (175, 100), (150, 100)], fill=lin(150, 0, 225, 0, [(0, alpha(WHITE, 0)), (0.5, alpha(WHITE, 0.10)), (1, alpha(WHITE, 0))]))

    def spark_layer(seed):
        f = new()
        r = random.Random(seed)
        pts = scatter(8, seed, 12, 288, 10, 90, md=34)
        for (x, y) in pts:
            x = round(x / 20) * 20
            y = round(y / 20) * 20
            rr = r.uniform(3.5, 6.5)
            sparkle_dot(f, x, y, rr, mix("$accent", "$ink", 0.2), 0.75)
            f.circle(x, y, rr * 1.9, fill=rad(x, y, rr * 1.9, [(0, alpha(WHITE, 0.35)), (1, alpha(WHITE, 0))]))
        return f

    return b, spark_layer(11), spark_layer(29)


# ------------------------------------------------------------------ 2. DOTS

def dots():
    b = new()
    row = 0
    y = 8
    while y < 104:
        x = 10 if row % 2 == 0 else 21
        while x < 312:
            t = 0.72 * (x / 300.0) + 0.28 * (1 - y / 100.0)
            r = 0.9 + 3.9 * smooth(t) ** 1.15
            b.circle(x, y, r, fill=A(0.20 + 0.20 * smooth(t)))
            x += 22
        y += 16
        row += 1
    f = new()
    specs = [(36, 30, 9), (92, 66, 6), (150, 22, 11), (208, 72, 8), (262, 38, 10), (122, 84, 5)]
    for (x, y, r) in specs:
        def put(xx, y=y, r=r):
            f.circle(xx, y, r, fill=rad(xx - r * 0.3, y - r * 0.35, r * 1.3, [(0, alpha(WHITE, 0.50)), (0.55, A(0.10)), (1, A(0.26))]), stroke=A(0.45), sw=0.8)
            f.ellipse(xx - r * 0.35, y - r * 0.45, r * 0.3, r * 0.17, fill=alpha(WHITE, 0.75))
        wrapx(put, x, r + 2)
    return b, f


# ------------------------------------------------------------------ 3. HILLS

HILL_LAYERS = [
    (58, [(6, 1, 0.5), (3, 3, 1.2)]),
    (71, [(8, 1, 2.0), (3, 2, 0.3)]),
    (85, [(7, 1, 3.6), (2.5, 3, 2.1)]),
]


def inkd(t, c="$accent"):
    """Oscurece hacia la tinta (solo para banners claros: la tinta es un tono profundo del mismo matiz)."""
    return mix(c, "$ink", t)


def hills():
    b = new()
    glow(b, 252, 28, 52, "#FFF6D6", 0.55)
    b.circle(252, 28, 20, fill=alpha(WHITE, 0.22))
    b.circle(252, 28, 13, fill=lin(245, 18, 258, 40, [(0, "#FFFDF0"), (1, "#FFE9A8")]))
    cols = [mix("$accent", "$top", 0.35), "$accent", inkd(0.30), inkd(0.52)]
    for i, (base, comps) in enumerate(HILL_LAYERS):
        pts = wave_pts(base, comps)
        col = cols[i]
        b.path(ridge_cmds(pts), fill=lin(0, base - 12, 0, 100, [(0, lite(0.18, col)), (1, mix(col, "$ink", 0.18))]))
        b.path(catmull(pts), stroke=alpha(WHITE, 0.42), sw=0.9, cap=CAP_ROUND)
        if i >= 1:
            for k in (1, 2):
                off = [(x, y + 6 * k + i * 1.5) for (x, y) in pts]
                b.path(catmull(off), stroke=alpha("$ink", 0.10), sw=1.1, cap=CAP_ROUND)
    for (x, i) in [(26, 1), (52, 1), (188, 1), (160, 0), (95, 2), (140, 2), (272, 2), (24, 2), (120, 0)]:
        base, comps = HILL_LAYERS[i]
        y0 = wave_y(base, comps, x) + 2
        k = [0.75, 1.0, 1.25][i]
        cc = cols[min(i + 2, 3)]
        b.rect(x - 0.8 * k, y0 - 6 * k, 1.6 * k, 6.5 * k, fill=inkd(0.6))
        if (x // 7) % 2 == 0:
            b.circle(x, y0 - 9 * k, 5 * k, fill=rad(x - 1.5 * k, y0 - 11 * k, 7 * k, [(0, lite(0.22, cc)), (1, inkd(0.15, cc))]))
        else:
            b.tri((x - 4.2 * k, y0 - 3 * k), (x, y0 - 14 * k), (x + 4.2 * k, y0 - 3 * k), fill=lin(x - 4, y0 - 14, x + 4, y0, [(0, lite(0.2, cc)), (1, inkd(0.2, cc))]))
    # casita
    hx = 214
    hy = wave_y(HILL_LAYERS[1][0], HILL_LAYERS[1][1], hx) + 3
    b.rect(hx - 6, hy - 8, 12, 8.5, r=0.8, fill=lin(hx, hy - 8, hx, hy, [(0, "#FFF4DE"), (1, "#EBD3AA")]))
    b.poly([(hx - 8, hy - 7.5), (hx, hy - 15), (hx + 8, hy - 7.5)], fill=lin(hx, hy - 15, hx, hy - 7, [(0, "#E58A6B"), (1, "#C25B45")]), join=JOIN_ROUND)
    b.rect(hx - 1.4, hy - 5, 2.8, 5, r=0.6, fill="#8A5A3C")
    b.rect(hx + 3.2, hy - 6.3, 2.2, 2.2, r=0.4, fill="#FFE08A")
    b.rect(hx + 3.5, hy - 17, 2.2, 5, fill="#C25B45")
    f = new()
    for (x, y, k) in [(46, 26, 1.1), (143, 18, 0.8), (232, 44, 0.95)]:
        wrapx(lambda xx, y=y, k=k: cloud(f, xx, y, k, 0.94), x, 22 * k)
    wrapx(lambda xx: cloud(f, xx, 40, 0.6, 0.8), 290, 14)
    return b, f


# ------------------------------------------------------------------ 4. PETALS

PETAL = [("M", 0, -5.2), ("C", -2.8, -9.6, -9.6, -5.6, -8.0, 1.0), ("C", -6.4, 6.4, -1.6, 9.6, 0, 10.4),
         ("C", 1.6, 9.6, 6.4, 6.4, 8.0, 1.0), ("C", 9.6, -5.6, 2.8, -9.6, 0, -5.2), ("Z",)]


def petal_path(k=1.0, deg=0.0, dx=0.0, dy=0.0):
    return tcmds(PETAL, deg, dx, dy, k)


def blossom(s, cx, cy, k, deg=0.0, col1=None, col2=None):
    col1 = col1 or lite(0.5)
    col2 = col2 or "$accent"
    cmds = []
    for i in range(5):
        a = deg + i * 72
        ax = math.sin(math.radians(a))
        ay = -math.cos(math.radians(a))
        cmds += tcmds(PETAL, a, cx + ax * 5.2 * k, cy + ay * 5.2 * k, k * 0.62)
    s.path(cmds, fill=rad(cx, cy, 11 * k, [(0, dark(0.08, col2)), (0.45, col1), (1, mix(col1, col2, 0.35))]), stroke=dark(0.25, col2), sw=0.5, join=JOIN_ROUND)
    s.circle(cx, cy, 1.5 * k, fill=dark(0.35, col2))
    for i in range(6):
        a = math.radians(deg + i * 60 + 20)
        s.line(cx, cy, cx + math.cos(a) * 3.4 * k, cy + math.sin(a) * 3.4 * k, dark(0.3, col2), 0.35 * k + 0.2)
        s.circle(cx + math.cos(a) * 3.6 * k, cy + math.sin(a) * 3.6 * k, 0.55 * k, fill=lite(0.6, "#F4C95D"))


def petals():
    b = new()
    for (x, y, r, a) in [(40, 30, 16, 0.14), (120, 72, 22, 0.10), (176, 16, 12, 0.14), (84, 50, 8, 0.12), (270, 70, 15, 0.12), (212, 60, 10, 0.13)]:
        b.circle(x, y, r, fill=rad(x, y, r, [(0, alpha(WHITE, a * 2.3)), (1, alpha(WHITE, 0))]))
    # monton de petalos
    pts = wave_pts(95, [(3.5, 1, 0.8), (1.5, 3, 2.0)])
    hill(b, pts, lin(0, 88, 0, 100, [(0, A(0.30)), (1, A(0.55))]), rim=alpha(WHITE, 0.35))
    r = random.Random(3)
    for x in range(8, 300, 19):
        y = wave_y(95, [(3.5, 1, 0.8), (1.5, 3, 2.0)], x) + r.uniform(0, 3)
        b.path(petal_path(0.26, r.uniform(0, 360), x, y), fill=lite(0.45), op=0.85)
    # rama
    wood = mix("$ink", "$bottom", 0.30)
    main = [(308, -6), (262, 6), (226, 8), (196, 20), (160, 22)]
    b.curve(main, wood, 3.0)
    b.curve([(262, 6), (252, 24), (236, 34)], wood, 1.8)
    b.curve([(226, 8), (212, -4)], wood, 1.6)
    b.curve([(196, 20), (190, 36), (176, 42)], wood, 1.4)
    for (x, y, k, d) in [(284, 2, 1.35, 10), (250, 12, 1.15, 40), (236, 34, 1.0, 20), (222, 8, 1.2, 55), (212, -3, 0.9, 5), (196, 22, 1.25, 25),
                          (176, 42, 0.95, 60), (162, 22, 1.1, 35), (268, 24, 0.8, 15)]:
        blossom(b, x, y, k, d, lite(0.55), "$accent")
    for (x, y) in [(244, 28), (190, 30), (170, 30), (278, 14)]:
        b.circle(x, y, 1.6, fill=dark(0.1), stroke=dark(0.35), sw=0.4)
    f = new()
    r = random.Random(7)
    pts = scatter(15, 9, 4, 296, 0, 100, md=26, wy=True)
    for (x, y) in pts:
        k = r.uniform(0.5, 1.0)
        deg = r.uniform(0, 360)
        col = lite(r.choice([0.55, 0.35, 0.15]))

        def put(yy, x=x, k=k, deg=deg, col=col):
            f.path(petal_path(k * 0.9, deg, x, yy), fill=col, op=0.9)
            f.path(tcmds([("M", 0, -4), ("L", 0, 8)], deg, x, yy, k * 0.9), stroke=A(0.4, "#FFFFFF"), sw=0.5)
        wrapy(put, y, 10 * k)
    return b, f


# ------------------------------------------------------------------ 5. LEAVES

LEAF = [("M", 0, -1.7), ("C", 1.15, -0.8, 1.05, 0.9, 0, 1.7), ("C", -1.05, 0.9, -1.15, -0.8, 0, -1.7), ("Z",)]
MAPLE_R = [(0, -1.0), (0.17, -0.55), (0.46, -0.72), (0.38, -0.30), (0.96, -0.40), (0.62, -0.02), (0.74, 0.42), (0.20, 0.30), (0.0, 0.46)]


def maple_cmds(k=1.0, deg=0.0, dx=0.0, dy=0.0):
    pts = MAPLE_R + [(-x, y) for (x, y) in reversed(MAPLE_R[1:-1])]
    cmds = [("M", pts[0][0], pts[0][1])] + [("L", x, y) for (x, y) in pts[1:]] + [("Z",)]
    return tcmds(cmds, deg, dx, dy, k)


def leaf_at(s, x, y, length, deg, col, maple=False, veins=True, op=None):
    """Hoja con degradado y nervio; col es una pintura de color."""
    k = length / 3.4
    if maple:
        r = length * 0.62
        s.path(maple_cmds(r, deg, x, y), fill=lin(x - r, y - r, x + r, y + r, [(0, lite(0.28, col)), (1, mix(col, DEEP, 0.2))]),
               stroke=alpha(mix(col, DEEP, 0.5), 0.55), sw=0.4, join=JOIN_ROUND, op=op)
        if veins:
            a = math.radians(deg)
            for ang in (-90, -35, 35, 90 + 25, 90 - 25 - 50):
                aa = math.radians(ang) + a
                s.line(x, y, x + math.cos(aa) * r * 0.7, y + math.sin(aa) * r * 0.7, alpha(DEEP, 0.28), 0.4, op=op)
        return
    s.path(tcmds(LEAF, deg, x, y, k), fill=lin(x - length * 0.5, y - length * 0.5, x + length * 0.5, y + length * 0.5,
           [(0, lite(0.3, col)), (1, mix(col, DEEP, 0.18))]), op=op)
    if veins:
        s.path(tcmds([("M", 0, -1.45), ("L", 0, 1.9)], deg, x, y, k), stroke=alpha(DEEP, 0.26), sw=0.5, cap=CAP_ROUND, op=op)


AUTUMN = ["$accent", dark(0.18), lite(0.28), "#E0A93B", "#C8553D"]


def leaves():
    b = new()
    glow(b, 238, 74, 70, "#FFE9B0", 0.55)
    # lineas de arboles al fondo
    pts = wave_pts(64, [(4, 4, 0.4), (3, 9, 1.1)], step=10)
    b.path(ridge_cmds(pts), fill=lin(0, 56, 0, 100, [(0, A(0.30)), (1, A(0.55))]))
    pts = wave_pts(76, [(4, 3, 2.0), (2.5, 8, 0.2)], step=10)
    b.path(ridge_cmds(pts), fill=lin(0, 68, 0, 100, [(0, A(0.55)), (1, dark(0.25, A(0.85)))]))
    # suelo con hojas
    pts = wave_pts(90, [(3, 1, 1.0), (1.5, 4, 0.5)])
    b.path(ridge_cmds(pts), fill=lin(0, 84, 0, 100, [(0, dark(0.3)), (1, dark(0.55))]))
    b.path(catmull(pts), stroke=alpha(WHITE, 0.25), sw=0.8)
    r = random.Random(14)
    for x in range(4, 300, 13):
        y = wave_y(90, [(3, 1, 1.0), (1.5, 4, 0.5)], x) + r.uniform(1, 6)
        leaf_at(b, x, y, r.uniform(7, 10), r.uniform(0, 360), r.choice(AUTUMN), maple=(r.random() < 0.3), veins=False)
    # rama con hojas arriba a la izquierda
    wood = mix("$ink", "$bottom", 0.25)
    b.curve([(-8, -2), (22, 6), (52, 12), (86, 26), (112, 34)], wood, 3.0)
    b.curve([(52, 12), (60, -4)], wood, 1.6)
    b.curve([(86, 26), (96, 44), (92, 52)], wood, 1.5)
    b.curve([(22, 6), (30, 24), (22, 34)], wood, 1.5)
    spec = [(14, 14, 15, 200, 1, True), (38, 20, 14, 130, 2, False), (60, 4, 13, 30, 3, True), (74, 22, 15, 250, 0, False),
            (100, 18, 14, 60, 4, True), (110, 40, 15, 20, 1, False), (94, 54, 13, 180, 3, True), (22, 38, 13, 210, 2, True),
            (46, 36, 12, 100, 0, False), (126, 34, 12, 70, 2, False), (72, 42, 11, 160, 4, False)]
    for (x, y, L, d, ci, mp) in spec:
        leaf_at(b, x, y, L, d, AUTUMN[ci], maple=mp)
    f = new()
    r = random.Random(21)
    pts = scatter(13, 4, 6, 294, 0, 100, md=27, wy=True)
    for i, (x, y) in enumerate(pts):
        L = r.uniform(8, 13)
        d = r.uniform(0, 360)
        c = AUTUMN[i % len(AUTUMN)]
        mp = (i % 4 == 0)
        wrapy(lambda yy, x=x, L=L, d=d, c=c, mp=mp: leaf_at(f, x, yy, L, d, c, maple=mp, veins=True, op=0.92), y, L)
    return b, f


# ------------------------------------------------------------------ 6. WAVES

HZ = 50
WAVE_BANDS = [(HZ + 1, [(1.6, 2, 0.0), (0.8, 5, 1.0)]), (HZ + 13, [(2.4, 2, 1.7), (1.0, 6, 0.4)]),
              (HZ + 27, [(3.0, 2, 3.1), (1.2, 5, 2.2)]), (HZ + 42, [(3.4, 1, 4.3), (1.4, 4, 1.4)])]


def waves():
    b = new()
    glow(b, 250, 26, 58, "#FFF7DA", 0.5)
    b.circle(250, 26, 19, fill=alpha(WHITE, 0.25))
    b.circle(250, 26, 12, fill=lin(244, 16, 256, 38, [(0, "#FFFFFF"), (1, "#FFF0C2")]))
    # isla lejana
    isl = [(-6, HZ + 2), (14, HZ - 5), (38, HZ - 10), (62, HZ - 6), (86, HZ - 1), (100, HZ + 2)]
    b.path(ridge_cmds(isl), fill=lin(0, HZ - 10, 0, HZ + 2, [(0, mix("$accent", "$top", 0.35)), (1, mix("$accent", "$top", 0.6))]))
    b.path([("M", 20, HZ - 4), ("C", 26, HZ - 9, 30, HZ - 9, 36, HZ - 4)], stroke=alpha(WHITE, 0.3), sw=0.8)
    cols = [mix("$accent", WHITE, 0.55), mix("$accent", WHITE, 0.32), "$accent", inkd(0.28)]
    for i, (base, comps) in enumerate(WAVE_BANDS):
        pts = wave_pts(base, comps, step=15)
        b.path(ridge_cmds(pts), fill=lin(0, base - 6, 0, 100, [(0, lite(0.12, cols[i])), (1, mix(cols[i], "$ink", 0.16))]))
        if i == 0:
            # velero
            sx, sy = 172, base - 1
            b.path([("M", sx - 8, sy - 1), ("L", sx + 9, sy - 1), ("L", sx + 5.5, sy + 3), ("L", sx - 5.5, sy + 3), ("Z",)], fill=lin(sx, sy - 1, sx, sy + 3, [(0, "#FFFDF6"), (1, "#C98B5E")]), join=JOIN_ROUND)
            b.line(sx, sy - 1, sx, sy - 16, "#8A5A3C", 0.9)
            b.poly([(sx + 0.8, sy - 15), (sx + 8, sy - 3), (sx + 0.8, sy - 3)], fill=lin(sx, sy - 15, sx + 8, sy - 3, [(0, "#FFFFFF"), (1, "#F3E7D5")]))
            b.poly([(sx - 0.8, sy - 12), (sx - 7, sy - 3), (sx - 0.8, sy - 3)], fill=lin(sx - 7, sy - 12, sx, sy - 3, [(0, "#FFF6E8"), (1, "#E8D3B4")]))
            b.poly([(sx, sy - 16), (sx + 4, sy - 15), (sx, sy - 14)], fill="#E07A5F")
            b.ellipse(sx, sy + 4.5, 9, 1.6, fill=alpha(WHITE, 0.4))
    # reflejo del sol
    for k, (w, a) in enumerate([(22, 0.6), (17, 0.5), (13, 0.42), (9, 0.35), (6, 0.28)]):
        b.rect(250 - w / 2.0, HZ + 4 + k * 5.5, w, 1.8, r=0.9, fill=alpha(WHITE, a))
    f = new()
    for i, (base, comps) in enumerate(WAVE_BANDS):
        pts = wave_pts(base - 0.6, comps, step=15)
        f.path(catmull(pts), stroke=alpha(WHITE, 0.75 - i * 0.12), sw=1.3 - i * 0.12, cap=CAP_ROUND)
    r = random.Random(8)
    for (x, y) in scatter(22, 3, 8, 292, HZ + 6, 96, md=17):
        w = r.uniform(4, 9)
        f.rect(x - w / 2, y, w, 1.1, r=0.55, fill=alpha(WHITE, r.uniform(0.35, 0.7)))
    for (x, y, k) in [(128, 24, 1.0), (206, 14, 0.8), (90, 34, 0.7)]:
        f.path([("M", x - 5 * k, y), ("C", x - 3 * k, y - 4 * k, x - 1 * k, y - 3 * k, x, y - 0.5 * k), ("C", x + 1 * k, y - 3 * k, x + 3 * k, y - 4 * k, x + 5 * k, y)],
               stroke=alpha("$ink", 0.5), sw=1.1, cap=CAP_ROUND, join=JOIN_ROUND)
    return b, f


# ------------------------------------------------------------------ 7. BUBBLES

def bubbles():
    b = new()
    for (x0, wd, a, lean) in [(30, 24, 0.20, 34), (104, 34, 0.13, 40), (196, 22, 0.18, 30), (258, 30, 0.11, 36)]:
        b.poly([(x0, -2), (x0 + wd, -2), (x0 + wd * 1.9 + lean, 102), (x0 + wd * 0.5 + lean, 102)], fill=lin(0, 0, 0, 100, [(0, alpha(WHITE, a)), (1, alpha(WHITE, 0))]))
    # algas
    r = random.Random(12)
    xs = [10, 34, 58, 82, 214, 238, 262, 288, 130, 170]
    for i, x in enumerate(xs):
        h = r.uniform(24, 46) if (x < 100 or x > 200) else r.uniform(12, 22)
        s_ = 1 if i % 2 == 0 else -1
        pts = [(x, 103), (x + s_ * 4, 103 - h * 0.33), (x - s_ * 3.5, 103 - h * 0.66), (x + s_ * 2, 103 - h)]
        b.curve(pts, lin(0, 103 - h, 0, 103, [(0, lite(0.25)), (1, dark(0.45))]), r.uniform(2.2, 3.4))
        b.curve([(x + 5, 103), (x + 5 - s_ * 3, 103 - h * 0.45), (x + 5 + s_ * 2, 103 - h * 0.8)], lin(0, 103 - h, 0, 103, [(0, lite(0.1)), (1, dark(0.5))]), 1.7, op=0.8)
    # guijarros
    for i in range(17):
        x = 8 + i * 17.5 + r.uniform(-3, 3)
        w = r.uniform(7, 13)
        b.ellipse(x, 99.5, w, r.uniform(3, 4.6), fill=lin(x, 94, x, 104, [(0, lite(0.35, mix("$accent", "$bottom", 0.4))), (1, dark(0.35, mix("$accent", "$bottom", 0.4)))]))
    f = new()
    pts = scatter(14, 17, 6, 294, 0, 100, md=19, wy=True)
    r = random.Random(33)
    for (x, y) in pts:
        rr = r.uniform(2.6, 8.5)

        def put(yy, x=x, rr=rr):
            f.circle(x, yy, rr, fill=rad(x - rr * 0.3, yy - rr * 0.35, rr * 1.35, [(0, alpha(WHITE, 0.32)), (0.6, A(0.10)), (1, A(0.30))]), stroke=A(0.6), sw=0.8)
            f.ellipse(x - rr * 0.36, yy - rr * 0.42, rr * 0.3, rr * 0.17, fill=alpha(WHITE, 0.8))
            if rr > 5:
                f.circle(x + rr * 0.4, yy + rr * 0.45, rr * 0.09, fill=alpha(WHITE, 0.55))
        wrapy(put, y, rr + 1)
    return b, f


# ------------------------------------------------------------------ 8. SNOW

def snow_flake_cmds(x, y, r, deg=0.0):
    cmds = []
    for i in range(3):
        a = math.radians(deg + i * 60)
        cmds += [("M", x - math.cos(a) * r, y - math.sin(a) * r), ("L", x + math.cos(a) * r, y + math.sin(a) * r)]
    return cmds


def snowman(s, x, y, k=1.0):
    s.ellipse(x, y + 0.5 * k, 10 * k, 2.2 * k, fill=alpha("$ink", 0.14))
    sn = lambda cx, cy, r: s.circle(cx, cy, r, fill=rad(cx - r * 0.35, cy - r * 0.4, r * 1.6, [(0, "#FFFFFF"), (1, "#C9DAEE")]))
    sn(x, y - 5.2 * k, 6.2 * k)
    sn(x, y - 14.2 * k, 4.8 * k)
    s.rect(x - 4 * k, y - 18.6 * k, 8 * k, 1.1 * k, r=0.5 * k, fill="#5C4B6B")
    s.rect(x - 2.6 * k, y - 23.5 * k, 5.2 * k, 5.2 * k, r=0.9 * k, fill="#6B5A7E")
    s.rect(x - 2.6 * k, y - 20.4 * k, 5.2 * k, 1.0 * k, fill="#E07A5F")
    s.circle(x - 1.6 * k, y - 15.2 * k, 0.55 * k, fill="#3D405B")
    s.circle(x + 1.6 * k, y - 15.2 * k, 0.55 * k, fill="#3D405B")
    s.poly([(x, y - 14 * k), (x + 3.8 * k, y - 13.2 * k), (x, y - 12.8 * k)], fill="#F29A4A")
    s.path([("M", x - 4.2 * k, y - 10.4 * k), ("C", x - 1 * k, y - 8.8 * k, x + 1 * k, y - 8.8 * k, x + 4.2 * k, y - 10.4 * k)], stroke="#E07A5F", sw=1.7 * k, cap=CAP_ROUND)
    s.rect(x + 2.0 * k, y - 10 * k, 1.8 * k, 4.2 * k, r=0.8 * k, fill="#E07A5F")
    for dy in (-7.2, -4.8, -2.4):
        s.circle(x, y + dy * k, 0.5 * k, fill="#3D405B")
    s.line(x - 5.8 * k, y - 7 * k, x - 11 * k, y - 11 * k, "#6B4A33", 0.9 * k)
    s.line(x + 5.8 * k, y - 7 * k, x + 11 * k, y - 11.5 * k, "#6B4A33", 0.9 * k)


def snow_pine(s, x, y, k, shade):
    s.rect(x - 0.9 * k, y - 4 * k, 1.8 * k, 4.5 * k, fill=inkd(0.5, "#8A6A50"))
    for i, (w, h, off) in enumerate([(9, 9, 0), (7, 8, -6.5), (5, 7, -12)]):
        top = y - 4 * k + off * k
        s.tri((x - w * k, top), (x, top - h * k), (x + w * k, top), fill=lin(x - w * k, top - h * k, x + w * k, top, [(0, mix(shade, WHITE, 0.15)), (1, mix(shade, DEEP, 0.2))]), join=JOIN_ROUND)
        s.path([("M", x - w * k * 0.72, top - h * k * 0.28), ("Q", x - w * k * 0.3, top - h * k * 0.1, x, top - h * k * 0.3), ("Q", x + w * k * 0.3, top - h * k * 0.1, x + w * k * 0.72, top - h * k * 0.28),
                ("L", x, top - h * k), ("Z",)], fill=alpha(WHITE, 0.92))


def snow():
    b = new()
    glow(b, 240, 24, 56, WHITE, 0.55)
    snowc = mix("$accent", "$ink", 0.45)
    # bosque lejano
    r = random.Random(4)
    x = -4
    while x < 306:
        k = r.uniform(0.55, 0.85)
        h = 18 * k
        b.tri((x - 6 * k, 76), (x, 76 - h * 1.6), (x + 6 * k, 76), fill=alpha(mix("$accent", "$ink", 0.25), 0.26))
        x += r.uniform(8, 15)
    h1 = wave_pts(80, [(4, 1, 0.8), (2, 3, 1.5)], step=15)
    b.path(ridge_cmds(h1), fill=lin(0, 72, 0, 100, [(0, alpha(WHITE, 0.95)), (1, mix(WHITE, "$accent", 0.28))]))
    b.path(catmull(h1), stroke=alpha(WHITE, 0.9), sw=1.0)
    snow_pine(b, 36, 76, 1.2, mix("$accent", "$ink", 0.35))
    snow_pine(b, 56, 79, 0.85, mix("$accent", "$ink", 0.3))
    snow_pine(b, 196, 77, 1.0, mix("$accent", "$ink", 0.35))
    snow_pine(b, 216, 80, 1.35, mix("$accent", "$ink", 0.38))
    h2 = wave_pts(90, [(3.5, 1, 3.0), (1.6, 3, 0.6)], step=15)
    b.path(ridge_cmds(h2), fill=lin(0, 84, 0, 100, [(0, "#FFFFFF"), (1, mix(WHITE, "$accent", 0.38))]))
    b.path(catmull(h2), stroke=alpha("$accent", 0.25), sw=1.0)
    for (x, y, rr) in [(110, 94, 5), (150, 96, 4), (84, 93, 3.2), (282, 95, 4)]:
        b.ellipse(x, y, rr * 1.8, rr * 0.4, fill=alpha(mix("$accent", "$ink", 0.2), 0.18))
    snowman(b, 262, wave_y(90, [(3.5, 1, 3.0), (1.6, 3, 0.6)], 262) + 4, 1.0)
    f = new()
    r = random.Random(41)
    pts = scatter(20, 6, 4, 296, 0, 100, md=15, wy=True)
    edge = alpha(mix("$ink", "$bottom", 0.4), 0.28)
    for i, (x, y) in enumerate(pts):
        if i % 4 == 0:
            rr = r.uniform(3.2, 5.0)
            d = r.uniform(0, 60)

            def put(yy, x=x, rr=rr, d=d):
                cm = snow_flake_cmds(x, yy, rr, d)
                f.path(cm, stroke=alpha(mix("$ink", "$bottom", 0.35), 0.45), sw=1.9, cap=CAP_ROUND)
                f.path(cm, stroke=WHITE, sw=1.0, cap=CAP_ROUND)
                f.circle(x, yy, rr * 0.32, fill=WHITE)
            wrapy(put, y, rr + 1)
        else:
            rr = r.uniform(1.1, 2.9)

            def put(yy, x=x, rr=rr):
                if rr > 2.1:
                    f.circle(x, yy, rr * 2.2, fill=rad(x, yy, rr * 2.2, [(0, alpha(WHITE, 0.4)), (1, alpha(WHITE, 0))]))
                f.circle(x, yy, rr, fill=WHITE, stroke=edge, sw=0.5)
            wrapy(put, y, rr * 2.2)
    return b, f


# ------------------------------------------------------------------ utilidades de la segunda tanda

def night_stars(s, seed, n=40, y1=70, a=0.8, col="$ink", rmax=1.1):
    r = random.Random(seed)
    for (x, y) in scatter(n, seed, 4, 296, 3, y1, md=11):
        s.circle(x, y, r.uniform(0.35, rmax), fill=alpha(col, r.uniform(0.3, a)))


def twinkles(seed, n, col, y0=6, y1=94, rmin=2.5, rmax=5.5, snap=0, md=30, x0=8, x1=292, into=None):
    """Capa de destellos para los patrones 'twinkle' (cada semilla da otras posiciones)."""
    f = into if into is not None else new()
    r = random.Random(seed + 100)
    for (x, y) in scatter(n, seed, x0, x1, y0, y1, md=md):
        if snap:
            x = round(x / snap) * snap
            y = round(y / snap) * snap
        rr = r.uniform(rmin, rmax)
        f.circle(x, y, rr * 2.0, fill=rad(x, y, rr * 2.0, [(0, alpha(col, 0.4)), (1, alpha(col, 0))]))
        sparkle_dot(f, x, y, rr, col, 0.95)
    return f


def pine(s, x, y, h, col, tiers=4, rim=None):
    """Pino de tiers triangulares; (x, y) es la base del tronco."""
    w = h * 0.34
    s.rect(x - h * 0.03, y - h * 0.12, h * 0.06, h * 0.14, fill=col)
    for i in range(tiers):
        t0 = 0.12 + i * (0.80 / tiers)
        t1 = t0 + (0.88 - t0) * 0.62 + 0.10
        ww = w * (1.0 - i * 0.52 / tiers)
        s.tri((x - ww, y - h * t0), (x, y - h * min(t1 + 0.18, 1.0)), (x + ww, y - h * t0), fill=col, join=JOIN_ROUND)
    if rim is not None:
        s.line(x, y - h, x - w * 0.5, y - h * 0.62, rim, 0.5)


# ------------------------------------------------------------------ 9. DIAMONDS

def gem(s, cx, cy, w, h, col):
    """Diamante tallado con facetas."""
    top = cy - h * 0.55
    gy = cy - h * 0.12
    L, R = cx - w, cx + w
    s.poly([(L, gy), (cx - w * 0.52, top), (cx + w * 0.52, top), (R, gy), (cx, cy + h * 0.55)],
           fill=lin(cx - w, top, cx + w, cy + h * 0.55, [(0, lite(0.65, col)), (1, dark(0.1, col))]), stroke=dark(0.25, col), sw=0.7, join=JOIN_ROUND)
    s.poly([(L, gy), (cx - w * 0.52, top), (cx - w * 0.18, gy)], fill=alpha(WHITE, 0.4))
    s.poly([(cx - w * 0.52, top), (cx + w * 0.52, top), (cx + w * 0.18, gy), (cx - w * 0.18, gy)], fill=alpha(WHITE, 0.2))
    s.poly([(cx + w * 0.52, top), (R, gy), (cx + w * 0.18, gy)], fill=alpha(dark(0.4, col), 0.25))
    s.poly([(L, gy), (cx - w * 0.18, gy), (cx, cy + h * 0.55)], fill=alpha(dark(0.15, col), 0.2))
    s.poly([(R, gy), (cx + w * 0.18, gy), (cx, cy + h * 0.55)], fill=alpha(dark(0.45, col), 0.32))
    s.line(L, gy, R, gy, alpha(WHITE, 0.55), 0.5)


def diamonds():
    b = new()
    for row in range(-1, 8):
        for c in range(-1, 11):
            cx = c * 30 + (15 if row % 2 else 0)
            cy = row * 15 + 8
            t = smooth(cx / 300.0)
            pts = [(cx, cy - 15), (cx + 15, cy), (cx, cy + 15), (cx - 15, cy)]
            tone = (c * 7 + row * 3) % 3
            if tone == 0:
                b.poly(pts, fill=A(0.12 + 0.14 * t), stroke=A(0.34), sw=0.6, join=JOIN_ROUND)
            elif tone == 1:
                b.poly(pts, fill=alpha(WHITE, 0.10 + 0.08 * t), stroke=alpha(WHITE, 0.30), sw=0.6, join=JOIN_ROUND)
                b.poly([(cx, cy - 7), (cx + 7, cy), (cx, cy + 7), (cx - 7, cy)], stroke=A(0.28), sw=0.5, join=JOIN_ROUND)
            else:
                b.poly(pts, stroke=A(0.22), sw=0.5, join=JOIN_ROUND)
    for (x, y, w, h) in [(58, 62, 15, 24), (168, 36, 12, 19), (252, 66, 17, 27), (112, 24, 8, 13)]:
        b.ellipse(x, y + h * 0.62, w * 1.1, 1.6, fill=alpha("$ink", 0.14))
        gem(b, x, y, w, h, "$accent")
    return b, twinkles(11, 9, lite(0.35), snap=15, md=30), twinkles(29, 9, lite(0.35), snap=15, md=30)


# ------------------------------------------------------------------ 10. RAYS

def rays():
    b = new()
    cx, cy = 218, 86
    n = 16
    for i in range(n):
        a0 = math.pi + i * math.pi / n
        a1 = a0 + math.pi / n * 0.5
        R = 330
        b.poly([(cx, cy), (cx + R * math.cos(a0), cy + R * math.sin(a0)), (cx + R * math.cos(a1), cy + R * math.sin(a1))],
               fill=rad(cx, cy, 250, [(0, alpha(WHITE, 0.34)), (0.6, alpha(WHITE, 0.14)), (1, alpha(WHITE, 0))]))
    glow(b, cx, cy, 90, lite(0.6), 0.6, mid=0.5)
    for rr, a in [(34, 0.22), (26, 0.3)]:
        b.circle(cx, cy, rr, fill=alpha(WHITE, a))
    b.circle(cx, cy, 19, fill=rad(cx - 5, cy - 6, 26, [(0, "#FFFFFF"), (0.4, lite(0.7)), (1, "$accent")]))
    hl = wave_pts(92, [(3, 1, 0.4), (2, 3, 2.1)], step=15)
    b.path(ridge_cmds(hl), fill=lin(0, 86, 0, 100, [(0, mix("$accent", DEEP, 0.38)), (1, mix("$accent", DEEP, 0.62))]))
    b.path(catmull(hl), stroke=alpha(WHITE, 0.3), sw=0.8, cap=CAP_ROUND)

    def layer(off, seed):
        f = new()
        for i in range(n):
            a0 = math.pi + (i + off) * math.pi / n
            a1 = a0 + math.pi / n * 0.22
            R = 330
            f.poly([(cx, cy), (cx + R * math.cos(a0), cy + R * math.sin(a0)), (cx + R * math.cos(a1), cy + R * math.sin(a1))],
                   fill=rad(cx, cy, 250, [(0, alpha(WHITE, 0.4)), (1, alpha(WHITE, 0))]))
        twinkles(seed, 7, WHITE, 4, 70, 2.2, 4.8, md=34, into=f)
        return f
    return b, layer(0.45, 7), layer(0.75, 19)


# ------------------------------------------------------------------ 11. STARS

def stars():
    b = new()
    night_stars(b, 5, 48, 74, 0.8)
    glow(b, 238, 28, 50, lite(0.5), 0.5)
    b.path(tcmds(crescent_cmds(15, 62, 0.42), -22, 238, 28), fill=lin(224, 14, 252, 42, [(0, "#FFFFFF"), (1, lite(0.45))]))
    consts = [(40, 22), (70, 30), (98, 24), (120, 40), (150, 36), (176, 22)]
    for i in range(len(consts) - 1):
        b.line(consts[i][0], consts[i][1], consts[i + 1][0], consts[i + 1][1], alpha("$ink", 0.25), 0.5)
    for (x, y) in consts:
        b.circle(x, y, 4.5, fill=rad(x, y, 4.5, [(0, alpha("$accent", 0.5)), (1, alpha("$accent", 0))]))
        b.star(x, y, 2.6, 5, 0.45, fill=lite(0.45))
    hl = wave_pts(88, [(4, 1, 0.9), (2.5, 2, 2.2)], step=15)
    b.path(ridge_cmds(hl), fill=mix("$bottom", NIGHT, 0.72))
    for (x, h) in [(18, 22), (34, 16), (262, 19), (280, 26), (298, 15)]:
        pine(b, x, wave_y(88, [(4, 1, 0.9), (2.5, 2, 2.2)], x) + 2, h, mix("$bottom", NIGHT, 0.82))
    cx = 214
    cy = wave_y(88, [(4, 1, 0.9), (2.5, 2, 2.2)], cx) + 2
    b.rect(cx - 6, cy - 7, 12, 8, fill=mix("$bottom", NIGHT, 0.85))
    b.poly([(cx - 8, cy - 6.5), (cx, cy - 13), (cx + 8, cy - 6.5)], fill=mix("$bottom", NIGHT, 0.9))
    b.rect(cx - 1.6, cy - 5, 3.2, 3.4, fill=lite(0.6, "#FFD27A"))
    f1 = twinkles(3, 10, lite(0.4), 4, 66, 2.2, 5.2, md=28)
    f2 = twinkles(17, 10, lite(0.4), 4, 66, 2.2, 5.2, md=28)
    f2.line(40, 8, 78, 22, alpha(WHITE, 0.9), 1.0)
    f2.line(40, 8, 56, 14, alpha(WHITE, 0.3), 1.8)
    f2.circle(78, 22, 1.4, fill=WHITE)
    return b, f1, f2


# ------------------------------------------------------------------ 12. AURORA

def aurora_curtain(s, base, amp, cyc, ph, h, col, a, streaks=True):
    top = [(x, base + amp * math.sin(2 * math.pi * cyc * x / W + ph)) for x in range(-12, 313, 6)]
    bot = [(x, y + h + 5 * math.sin(2 * math.pi * (cyc + 1) * x / W + ph * 1.7)) for (x, y) in top]
    ymin = base - amp
    ymax = base + amp + h + 6
    s.poly(top + list(reversed(bot)), fill=lin(0, ymin, 0, ymax, [(0, alpha(col, 0)), (0.18, alpha(col, a)), (0.55, alpha(col, a * 0.45)), (1, alpha(col, 0))]))
    if streaks:
        for i in range(0, len(top), 2):
            x, y = top[i]
            s.line(x, y + 1, x, y + h * 0.8, alpha(col, a * 0.35), 0.9, cap=CAP_BUTT)


def aurora():
    b = new()
    night_stars(b, 9, 44, 58, 0.75)
    aurora_curtain(b, 24, 8, 1, 0.4, 44, "$accent", 0.85)
    aurora_curtain(b, 34, 7, 2, 1.7, 36, mix("$accent", "#B58CFF", 0.55), 0.62)
    aurora_curtain(b, 16, 5, 3, 2.6, 30, lite(0.45), 0.32, streaks=False)
    far = wave_pts(84, [(3, 1, 1.0), (2, 3, 0.2)], step=15)
    b.path(ridge_cmds(far), fill=mix("$bottom", NIGHT, 0.55))
    near = wave_pts(93, [(3, 1, 2.6), (1.8, 2, 0.8)], step=15)
    b.path(ridge_cmds(near), fill=mix("$bottom", NIGHT, 0.82))
    for (x, h) in [(24, 24), (40, 17), (74, 13), (226, 22), (246, 15), (270, 26), (288, 16)]:
        pine(b, x, wave_y(93, [(3, 1, 2.6), (1.8, 2, 0.8)], x) + 2, h, mix("$bottom", NIGHT, 0.9))
    f = new()
    aurora_curtain(f, 22, 7, 1, 2.2, 40, lite(0.5), 0.34)
    aurora_curtain(f, 32, 6, 2, 0.5, 30, mix("$accent", "#8EC9FF", 0.5), 0.28, streaks=False)
    for (x, y, rr) in [(36, 20, 4), (110, 34, 3), (190, 16, 4.5), (255, 30, 3.4)]:
        wrapx(lambda xx, y=y, rr=rr: sparkle_dot(f, xx, y, rr, lite(0.6), 0.85), x, 6)
    return b, f


# ------------------------------------------------------------------ 14. SKYLINE (ciudad)

def city_blocks(seed, x0, x1, hmin, hmax, wmin, wmax):
    r = random.Random(seed)
    out = []
    x = x0
    while x < x1:
        w = r.uniform(wmin, wmax)
        out.append((x, w, r.uniform(hmin, hmax)))
        x += w + r.uniform(0.5, 2.5)
    return out


def city_windows(blocks, base_y, seed):
    r = random.Random(seed)
    out = []
    for (x, w, h) in blocks:
        yy = base_y - h + 4
        while yy < base_y - 3:
            xx = x + 2.2
            while xx < x + w - 3:
                out.append((xx, yy, r.random()))
                xx += 4.4
            yy += 5.2
    return out


def skyline():
    b = new()
    cx, cy, R = 232, 44, 24
    glow(b, cx, cy, 52, "$accent", 0.5)
    b.circle(cx, cy, R, fill=lin(cx, cy - R, cx, cy + R, [(0, lite(0.6)), (1, "$accent")]))
    for i in range(5):
        dy = 3 + i * 4.6
        hw = math.sqrt(max(R * R - dy * dy, 1))
        b.rect(cx - hw, cy + dy, 2 * hw, 1.0 + i * 0.5, fill=mix("$top", "$bottom", 0.55))
    far = city_blocks(2, -4, 308, 22, 52, 11, 21)
    near = city_blocks(7, -6, 310, 14, 40, 14, 26)
    for (x, w, h) in far:
        b.rect(x, 88 - h, w, h + 14, fill=mix("$bottom", NIGHT, 0.45))
        b.line(x, 88 - h, x + w, 88 - h, alpha("$accent", 0.35), 0.5, cap=CAP_BUTT)
    for (x, w, h) in near:
        b.rect(x, 100 - h, w, h + 4, fill=mix("$top", NIGHT, 0.82), stroke=alpha("$accent", 0.5), sw=0.6)
    wins_far = city_windows(far, 88, 5)
    wins_near = city_windows(near, 100, 8)
    for (x, y, u) in wins_far:
        b.rect(x, y, 1.6, 2.0, fill=alpha("$accent", 0.20))
    for (x, y, u) in wins_near:
        b.rect(x, y, 1.9, 2.4, fill=alpha("$accent", 0.16))
    tallest = max(near, key=lambda t: t[2])
    b.line(tallest[0] + tallest[1] / 2, 100 - tallest[2], tallest[0] + tallest[1] / 2, 100 - tallest[2] - 9, alpha("$accent", 0.8), 0.7)
    b.rect(0, 97, 300, 3, fill=lin(0, 97, 300, 97, [(0, alpha("$accent", 0.0)), (0.5, alpha("$accent", 0.5)), (1, alpha("$accent", 0.0))]))

    def lit(lo, hi, blink):
        f = new()
        for (x, y, u) in wins_far:
            if lo <= u < hi:
                f.rect(x, y, 1.6, 2.0, fill=lite(0.45))
        for (x, y, u) in wins_near:
            if lo <= u < hi:
                f.rect(x - 0.4, y - 0.4, 2.7, 3.2, fill=alpha("$accent", 0.25))
                f.rect(x, y, 1.9, 2.4, fill=lite(0.65))
        ax, ay = tallest[0] + tallest[1] / 2, 100 - tallest[2] - 9
        if blink:
            f.circle(ax, ay, 3.4, fill=rad(ax, ay, 3.4, [(0, alpha("#FF4F6D", 0.7)), (1, alpha("#FF4F6D", 0))]))
            f.circle(ax, ay, 1.0, fill="#FF4F6D")
        return f
    return b, lit(0.0, 0.22, True), lit(0.14, 0.36, False)


# ------------------------------------------------------------------ 13. EMBERS (volcan)

def embers():
    b = new()
    night_stars(b, 4, 26, 38, 0.6)
    far = wave_pts(66, [(7, 1, 0.5), (4, 3, 1.8)], step=15)
    b.path(ridge_cmds(far), fill=mix("$bottom", NIGHT, 0.4))
    b.path(catmull(far), stroke=alpha("$accent", 0.3), sw=0.7)
    for (vx, vt, vw, glowa) in [(52, 54, 52, 0.35), (210, 26, 98, 0.8)]:
        glow(b, vx, vt + 2, 56 if vw > 60 else 30, "$accent", glowa, mid=0.5)
        top = vw * 0.12
        b.poly([(vx - vw, 101), (vx - vw * 0.6, vt + (100 - vt) * 0.38), (vx - top * 1.6, vt + 5), (vx - top, vt), (vx + top, vt + 1),
                (vx + top * 1.6, vt + 6), (vx + vw * 0.62, vt + (100 - vt) * 0.4), (vx + vw, 101)],
               fill=lin(0, vt, 0, 100, [(0, mix("$bottom", NIGHT, 0.5)), (1, mix("$bottom", NIGHT, 0.88))]), join=JOIN_ROUND)
        b.ellipse(vx, vt + 1, top * 1.05, 1.8, fill=lite(0.5))
        for sgn, off in ((-1, 0), (1, 1)):
            pts = [(vx + sgn * 2, vt + 2), (vx + sgn * (5 + off * 2), vt + 13), (vx + sgn * 3, vt + 25), (vx + sgn * (13 + off * 3), vt + 38),
                   (vx + sgn * (12 + off * 2), vt + 52), (vx + sgn * (26 + off * 6), vt + 66)]
            if vw < 60:
                pts = pts[:4]
            b.curve(pts, alpha("$accent", 0.4), 5.0)
            b.curve(pts, "$accent", 2.4)
            b.curve(pts, lite(0.7), 0.9)
    r = random.Random(3)
    for (sx, sy, sk) in [(206, 15, 1.0), (214, 6, 0.8), (202, -2, 0.7)]:
        b.circle(sx, sy, 9 * sk, fill=alpha(mix("$bottom", NIGHT, 0.3), 0.55))
        b.circle(sx - 3 * sk, sy - 2 * sk, 5 * sk, fill=alpha(lite(0.2), 0.12))
    gr = wave_pts(93, [(2.5, 1, 1.4), (1.5, 4, 0.3)], step=15)
    b.path(ridge_cmds(gr), fill=mix("$bottom", NIGHT, 0.9))
    for (x, y, l) in [(40, 96, 14), (110, 95, 20), (170, 97, 12), (270, 96, 18)]:
        b.line(x, y, x + l * 0.6, y - 1.2, alpha("$accent", 0.55), 0.9)
        b.line(x + l * 0.6, y - 1.2, x + l, y + 0.4, alpha("$accent", 0.4), 0.7)
    f = new()
    r = random.Random(12)
    pts = scatter(18, 5, 150, 270, 0, 100, md=12, wy=True) + scatter(9, 6, 4, 150, 0, 100, md=15, wy=True) + scatter(5, 7, 270, 296, 0, 100, md=15, wy=True)
    for (x, y) in pts:
        rr = r.uniform(0.7, 2.1)
        col = lite(r.uniform(0.25, 0.75))

        def put(yy, x=x, rr=rr, col=col):
            f.circle(x, yy, rr * 3.2, fill=rad(x, yy, rr * 3.2, [(0, alpha("$accent", 0.5)), (1, alpha("$accent", 0))]))
            f.circle(x, yy, rr, fill=col)
            if rr > 1.4:
                f.line(x, yy + rr, x - 0.4, yy + rr * 3.2, alpha("$accent", 0.35), 0.5)
        wrapy(put, y, rr * 3.4)
    return b, f


# ------------------------------------------------------------------ 15. BATS (noche de brujas)

BAT_WING = [("M", 0, -1), ("C", -3, -6, -9, -8, -15, -5), ("C", -13, -3, -12, -1, -11, 1), ("C", -9, -1, -8, 0, -7, 3),
            ("C", -5, 1, -3, 2, -2, 4), ("L", 0, 3), ("Z",)]


def bat(s, x, y, k, col, eyes=None):
    s.path(tcmds(BAT_WING, 0, x, y, k), fill=col, join=JOIN_ROUND)
    s.path(tcmds(BAT_WING, 0, x, y, k, kx=-k), fill=col, join=JOIN_ROUND)
    s.ellipse(x, y + 1 * k, 2.3 * k, 3.4 * k, fill=col)
    s.circle(x, y - 2.8 * k, 1.9 * k, fill=col)
    s.tri((x - 1.8 * k, y - 3.6 * k), (x - 1.2 * k, y - 6.2 * k), (x - 0.2 * k, y - 4.2 * k), fill=col)
    s.tri((x + 1.8 * k, y - 3.6 * k), (x + 1.2 * k, y - 6.2 * k), (x + 0.2 * k, y - 4.2 * k), fill=col)
    if eyes is not None:
        s.circle(x - 0.8 * k, y - 3.0 * k, 0.45 * k, fill=eyes)
        s.circle(x + 0.8 * k, y - 3.0 * k, 0.45 * k, fill=eyes)


def dead_tree(s, x, y, k, col):
    for (x0, y0, x1, y1, w) in [(0, 0, 1, -26, 4.2), (1, -15, -13, -29, 2.4), (-13, -29, -19, -37, 1.3), (-7, -23, -16, -24, 1.0),
                                (1, -19, 13, -33, 2.2), (13, -33, 20, -36, 1.1), (8, -27, 14, -22, 0.9), (1, -26, -3, -40, 1.7), (-3, -40, -7, -46, 0.9)]:
        s.line(x + x0 * k, y + y0 * k, x + x1 * k, y + y1 * k, col, w * k)


def bats():
    b = new()
    sil = mix("$bottom", NIGHT, 0.86)
    night_stars(b, 21, 36, 50, 0.7)
    mx, my = 78, 36
    glow(b, mx, my, 56, lite(0.35), 0.5, mid=0.5)
    b.circle(mx, my, 22, fill=lin(mx - 14, my - 18, mx + 14, my + 20, [(0, lite(0.82)), (1, lite(0.4))]))
    for (dx, dy, rr) in [(-7, -6, 4.5), (6, 4, 6.0), (-4, 9, 3.0), (9, -9, 2.6)]:
        b.circle(mx + dx, my + dy, rr, fill=alpha(dark(0.3), 0.2))
    hl = wave_pts(88, [(3.5, 1, 0.3), (2.2, 2, 2.4)], step=15)
    b.path(ridge_cmds(hl), fill=sil)
    dead_tree(b, 24, 92, 1.0, sil)
    hx, hy = 232, wave_y(88, [(3.5, 1, 0.3), (2.2, 2, 2.4)], 232) + 2
    b.rect(hx - 17, hy - 24, 34, 26, fill=sil)
    b.poly([(hx - 20, hy - 23), (hx - 2, hy - 38), (hx + 20, hy - 23)], fill=sil, join=JOIN_ROUND)
    b.rect(hx + 8, hy - 46, 10, 24, fill=sil)
    b.poly([(hx + 6, hy - 45), (hx + 13, hy - 58), (hx + 20, hy - 45)], fill=sil, join=JOIN_ROUND)
    b.rect(hx - 14, hy - 44, 4, 10, fill=sil)
    for (wx, wy, ww, wh) in [(hx - 12, hy - 18, 5, 7), (hx + 2, hy - 18, 5, 7), (hx + 10.5, hy - 38, 4, 6), (hx - 3, hy - 30, 3.4, 5)]:
        b.circle(wx + ww / 2, wy + wh / 2, 8, fill=rad(wx + ww / 2, wy + wh / 2, 8, [(0, alpha("$accent", 0.45)), (1, alpha("$accent", 0))]))
        b.rect(wx, wy, ww, wh, r=0.8, fill=lite(0.45, "$accent"))
    b.rect(hx - 8, hy - 7, 5, 9, r=2.5, fill=lite(0.55, "$accent"))
    for (gx, gh) in [(150, 7), (164, 5), (182, 6)]:
        gy = wave_y(88, [(3.5, 1, 0.3), (2.2, 2, 2.4)], gx) + 3
        b.rect(gx - 2.6, gy - gh, 5.2, gh, r=2.4, fill=sil)
        b.line(gx, gy - gh * 0.7, gx, gy - gh * 0.3, alpha(WHITE, 0.15), 0.5)
    f = new()
    for (x, y, k) in [(34, 26, 1.3), (118, 14, 1.0), (170, 40, 0.8), (258, 22, 1.5), (212, 58, 0.7)]:
        wrapx(lambda xx, y=y, k=k: bat(f, xx, y, k, NIGHT, eyes=lite(0.3)), x, 17 * k)
    return b, f


# ------------------------------------------------------------------ 16. PUMPKINS

def pumpkin(s, x, y, r, col):
    cy = y - r * 0.85
    dk = mix(col, DEEP, 0.45)
    for dx in (-0.46, 0.46):
        s.ellipse(x + dx * r, cy, 0.58 * r, 0.86 * r, fill=rad(x + dx * r - r * 0.2, cy - r * 0.3, r * 1.3, [(0, lite(0.12, col)), (1, mix(col, DEEP, 0.3))]), stroke=dk, sw=0.5)
    s.ellipse(x, cy, 0.62 * r, 0.92 * r, fill=rad(x - r * 0.2, cy - r * 0.35, r * 1.3, [(0, lite(0.3, col)), (1, mix(col, DEEP, 0.2))]), stroke=dk, sw=0.5)
    s.path([("M", x - r * 0.1, cy - r * 0.86), ("L", x - r * 0.14, cy - r * 1.18), ("C", x - r * 0.05, cy - r * 1.32, x + r * 0.2, cy - r * 1.32, x + r * 0.3, cy - r * 1.16),
            ("L", x + r * 0.14, cy - r * 1.1), ("L", x + r * 0.1, cy - r * 0.86), ("Z",)], fill=mix("#6E8B3D", DEEP, 0.25), join=JOIN_ROUND)
    s.ellipse(x - r * 0.3, cy - r * 0.5, r * 0.1, r * 0.22, fill=alpha(WHITE, 0.28))


def pumpkin_face(x, y, r):
    cy = y - r * 0.85
    eyes = [[(x - 0.44 * r, cy - 0.02 * r), (x - 0.12 * r, cy - 0.02 * r), (x - 0.28 * r, cy - 0.36 * r)],
            [(x + 0.12 * r, cy - 0.02 * r), (x + 0.44 * r, cy - 0.02 * r), (x + 0.28 * r, cy - 0.36 * r)],
            [(x - 0.07 * r, cy + 0.1 * r), (x + 0.07 * r, cy + 0.1 * r), (x, cy - 0.02 * r)]]
    pts = []
    n = 6
    for i in range(n + 1):
        px = x - 0.5 * r + i * r / n
        pts.append((px, cy + (0.28 if i % 2 == 0 else 0.46) * r))
    mouth = [(x - 0.5 * r, cy + 0.2 * r)] + pts + [(x + 0.5 * r, cy + 0.2 * r)]
    return eyes + [mouth]


PUMPKIN_SPOTS = [(40, 89, 13, True), (92, 94, 9, False), (150, 91, 15, True), (205, 95, 10, False), (252, 90, 14, True), (284, 95, 8, False), (118, 97, 7, False)]


def pumpkins():
    b = new()
    night_stars(b, 31, 30, 44, 0.6)
    mx, my = 74, 64
    glow(b, mx, my, 62, lite(0.3), 0.5, mid=0.5)
    b.circle(mx, my, 26, fill=lin(mx - 14, my - 20, mx + 14, my + 24, [(0, lite(0.7)), (1, lite(0.25))]))
    for (dx, dy, rr) in [(-8, -7, 5), (7, 5, 7), (-5, 11, 3.2)]:
        b.circle(mx + dx, my + dy, rr, fill=alpha(dark(0.3), 0.16))
    sil = mix("$bottom", NIGHT, 0.82)
    far = wave_pts(76, [(4, 1, 0.9), (2.5, 3, 0.3)], step=15)
    b.path(ridge_cmds(far), fill=mix("$bottom", NIGHT, 0.62))
    dead_tree(b, 270, 78, 0.8, sil)
    dead_tree(b, 196, 76, 0.55, mix("$bottom", NIGHT, 0.7))
    field = wave_pts(86, [(3, 1, 2.0), (2, 2, 0.2)], step=15)
    b.path(ridge_cmds(field), fill=lin(0, 80, 0, 100, [(0, mix("$bottom", NIGHT, 0.78)), (1, mix("$bottom", NIGHT, 0.92))]))
    col = mix("#F2761A", "$accent", 0.25)
    for (x, y, r, face) in sorted(PUMPKIN_SPOTS, key=lambda t: t[1]):
        c = col if r > 8 else mix(col, NIGHT, 0.25)
        pumpkin(b, x, y, r, c)
        if face:
            for poly in pumpkin_face(x, y, r):
                b.poly(poly, fill=mix(col, NIGHT, 0.9))

    def lit(strengths, seed):
        f = new()
        for (spot, st) in zip([s for s in PUMPKIN_SPOTS if s[3]], strengths):
            x, y, r, _ = spot
            cy = y - r * 0.85
            f.circle(x, cy, r * 2.4, fill=rad(x, cy, r * 2.4, [(0, alpha("$accent", 0.55 * st)), (1, alpha("$accent", 0))]))
            for poly in pumpkin_face(x, y, r):
                f.poly(poly, fill=lite(0.65 * st, "#FFC34A"))
        twinkles(seed, 7, lite(0.4), 8, 78, 1.8, 3.6, md=28, into=f)
        return f
    return b, lit([1.0, 0.7, 0.9], 13), lit([0.7, 1.0, 0.65], 27)


# ------------------------------------------------------------------ 17. MOUNTAINS

def mount(s, x, yb, w, h, col, snow=0.0, shade=0.28):
    ay = yb - h
    s.poly([(x - w, yb), (x, ay), (x + w, yb)], fill=col, join=JOIN_ROUND)
    s.poly([(x, ay), (x + w * 0.1, yb), (x + w, yb)], fill=alpha(DEEP, shade))
    s.poly([(x, ay), (x - w * 0.4, yb), (x - w * 0.06, yb)], fill=alpha(WHITE, 0.06))
    if snow > 0:
        ds = h * snow
        wd = w * snow
        s.poly([(x, ay), (x - wd, ay + ds), (x - wd * 0.55, ay + ds * 0.74), (x - wd * 0.22, ay + ds * 1.04), (x + wd * 0.15, ay + ds * 0.78),
                (x + wd * 0.5, ay + ds * 1.0), (x + wd, ay + ds)], fill=alpha(WHITE, 0.96), join=JOIN_ROUND)
        s.poly([(x, ay), (x + wd * 0.1, ay + ds * 0.9), (x + wd * 0.5, ay + ds * 1.0), (x + wd, ay + ds)], fill=alpha("#7C94B8", 0.2))


def mountains():
    b = new()
    glow(b, 236, 26, 50, lite(0.7), 0.55, mid=0.5)
    b.circle(236, 26, 11, fill=lin(230, 16, 242, 38, [(0, WHITE), (1, lite(0.6))]))
    r = random.Random(9)
    ranges = [(72, 0.55, (24, 40), 0.36, 0.0), (86, 0.15, (30, 50), 0.34, 0.22)]
    far_c = mix("$accent", "$top", 0.58)
    mid_c = mix("$accent", DEEP, 0.12)
    near_c = mix("$accent", DEEP, 0.55)
    x = -20
    while x < 330:
        h = r.uniform(28, 44)
        mount(b, x, 76, h * r.uniform(1.0, 1.3), h, far_c, snow=0.3, shade=0.18)
        x += r.uniform(44, 62)
    b.rect(0, 62, 300, 20, fill=lin(0, 62, 0, 82, [(0, alpha("$top", 0)), (1, alpha(mix("$top", "$bottom", 0.5), 0.55))]))
    x = -10
    while x < 330:
        h = r.uniform(38, 60)
        mount(b, x, 90, h * r.uniform(0.95, 1.2), h, mid_c, snow=0.34, shade=0.3)
        x += r.uniform(66, 90)
    b.rect(0, 76, 300, 24, fill=lin(0, 76, 0, 98, [(0, alpha("$bottom", 0)), (1, alpha(mix("$bottom", "$top", 0.3), 0.6))]))
    hl = wave_pts(93, [(3, 1, 0.9), (1.8, 3, 1.4)], step=15)
    b.path(ridge_cmds(hl), fill=lin(0, 86, 0, 100, [(0, near_c), (1, mix("$accent", DEEP, 0.75))]))
    for (x, h) in [(14, 20), (30, 14), (46, 24), (250, 18), (268, 25), (286, 15), (150, 11), (166, 15)]:
        pine(b, x, wave_y(93, [(3, 1, 0.9), (1.8, 3, 1.4)], x) + 2, h, mix("$accent", DEEP, 0.78))
    f = new()
    for (x, y, k) in [(40, 20, 0.9), (150, 34, 0.7), (245, 14, 0.8)]:
        wrapx(lambda xx, y=y, k=k: cloud(f, xx, y, k, 0.9), x, 20 * k)
    for (x, y, k) in [(90, 40, 1.0), (101, 45, 0.8), (205, 30, 0.9), (193, 34, 0.7)]:
        wrapx(lambda xx, y=y, k=k: f.path([("M", xx - 4 * k, y), ("C", xx - 2.5 * k, y - 3 * k, xx - 0.8 * k, y - 2.2 * k, xx, y), ("C", xx + 0.8 * k, y - 2.2 * k, xx + 2.5 * k, y - 3 * k, xx + 4 * k, y)],
                                          stroke=alpha("$ink", 0.6), sw=0.9, cap=CAP_ROUND, join=JOIN_ROUND), x, 6)
    return b, f


# ------------------------------------------------------------------ 18. FOREST

def pine_row(s, seed, y, h0, h1, step, col, rim, x0=-8, x1=308):
    r = random.Random(seed)
    x = x0
    while x < x1:
        pine(s, x, y + r.uniform(-1.5, 1.5), r.uniform(h0, h1), col, tiers=r.choice((3, 4, 4, 5)), rim=rim)
        x += step * r.uniform(0.7, 1.3)


def forest():
    b = new()
    for (x0, x1, w) in [(40, 74, 40), (96, 124, 34), (190, 218, 38)]:
        b.poly([(x0, 0), (x1, 0), (x1 - w, 100), (x0 - w, 100)], fill=lin(0, 0, 0, 100, [(0, alpha(WHITE, 0.16)), (1, alpha(WHITE, 0))]))
    pine_row(b, 1, 70, 26, 38, 11, mix("$bottom", "$top", 0.5), alpha(WHITE, 0.18))
    b.rect(0, 52, 300, 26, fill=lin(0, 52, 0, 78, [(0, alpha("$top", 0)), (0.7, alpha(mix("$top", "$bottom", 0.35), 0.5)), (1, alpha("$bottom", 0))]))
    pine_row(b, 2, 82, 34, 52, 17, mix("$bottom", DEEP, 0.16), alpha(WHITE, 0.14))
    b.rect(0, 70, 300, 22, fill=lin(0, 70, 0, 92, [(0, alpha("$top", 0)), (1, alpha(mix("$top", "$bottom", 0.4), 0.4))]))
    g = wave_pts(94, [(3, 1, 1.0), (1.6, 3, 0.5)], step=15)
    b.path(ridge_cmds(g), fill=mix("$bottom", DEEP, 0.55))
    for (x, h) in [(12, 62), (284, 70), (258, 46)]:
        pine(b, x, 98, h, mix("$bottom", DEEP, 0.62), tiers=5)
    for (x, y, r_) in [(60, 97, 7), (120, 98, 6), (170, 97, 8), (226, 98, 6)]:
        b.ellipse(x, y, r_ * 1.5, r_, fill=mix("$bottom", DEEP, 0.7))
        b.ellipse(x - r_ * 0.4, y - r_ * 0.35, r_ * 0.7, r_ * 0.3, fill=alpha(WHITE, 0.07))
    return b, twinkles(5, 13, lite(0.3), 18, 90, 1.6, 3.4, md=22), twinkles(23, 13, lite(0.3), 18, 90, 1.6, 3.4, md=22)


# ------------------------------------------------------------------ 19. CLOUDS

def balloon(s, x, y, k):
    cols = ["#FF8A8A", "#FFD27A", "#FFFFFF", "#8AD1FF", "#FF8A8A"]
    s.path(tcmds(ell_cmds(0, 0, 9, 11), 0, x, y, k), fill=rad(x - 3 * k, y - 4 * k, 15 * k, [(0, "#FFFFFF"), (1, "#E7C9B0")]))
    for i, c in enumerate(cols):
        x0 = -7.2 + i * 3.6
        s.path(tcmds([("M", x0 * 0.7, -9.5), ("C", x0 * 1.2, -4, x0 * 1.2, 4, x0 * 0.5, 10.2), ("L", (x0 + 3.6) * 0.5, 10.2),
                      ("C", (x0 + 3.6) * 1.2, 4, (x0 + 3.6) * 1.2, -4, (x0 + 3.6) * 0.7, -9.5), ("Z",)], 0, x, y, k),
               fill=alpha(c, 0.75))
    s.ellipse(x - 3.5 * k, y - 4 * k, 1.8 * k, 3.4 * k, fill=alpha(WHITE, 0.4))
    s.line(x - 3.2 * k, y + 10 * k, x - 2.2 * k, y + 15 * k, "#8A5A3C", 0.5 * k)
    s.line(x + 3.2 * k, y + 10 * k, x + 2.2 * k, y + 15 * k, "#8A5A3C", 0.5 * k)
    s.rect(x - 3 * k, y + 15 * k, 6 * k, 4.2 * k, r=0.8 * k, fill="#B8845A")


def bird(s, x, y, k, col, sw=0.9):
    s.path([("M", x - 4.5 * k, y), ("C", x - 2.8 * k, y - 3.2 * k, x - 0.9 * k, y - 2.4 * k, x, y), ("C", x + 0.9 * k, y - 2.4 * k, x + 2.8 * k, y - 3.2 * k, x + 4.5 * k, y)],
           stroke=col, sw=sw, cap=CAP_ROUND, join=JOIN_ROUND)


def clouds():
    b = new()
    glow(b, 250, 20, 60, "#FFFFFF", 0.8, mid=0.5)
    b.circle(250, 20, 11, fill=lin(244, 10, 256, 32, [(0, "#FFFFFF"), (1, "#FFF1B8")]))
    for (x, y, k) in [(34, 46, 0.5), (120, 30, 0.45), (176, 52, 0.4), (290, 56, 0.5)]:
        cloud(b, x, y, k, 0.7)
    balloon(b, 78, 34, 0.9)
    cloud(b, 150, 70, 1.15, 0.97)
    cloud(b, 226, 60, 0.9, 0.95)
    r = random.Random(6)
    for cx in range(-8, 322, 22):
        rr = r.uniform(11, 17)
        cy = 99 + r.uniform(-2, 2)
        b.circle(cx, cy, rr, fill=rad(cx - rr * 0.3, cy - rr * 0.6, rr * 1.5, [(0, "#FFFFFF"), (0.7, mix(WHITE, "$ink", 0.05)), (1, mix(WHITE, "$ink", 0.14))]))
    b.rect(0, 92, 300, 10, fill=alpha(WHITE, 0.9))
    f = new()
    for (x, y, k, a) in [(30, 22, 1.0, 0.95), (112, 44, 1.35, 0.97), (196, 12, 0.8, 0.9), (262, 46, 1.1, 0.95), (156, 74, 0.7, 0.8)]:
        wrapx(lambda xx, y=y, k=k, a=a: cloud(f, xx, y, k, a), x, 19 * k)
    for (x, y, k) in [(60, 18, 1.0), (72, 24, 0.8), (218, 30, 1.1), (232, 35, 0.8)]:
        wrapx(lambda xx, y=y, k=k: bird(f, xx, y, k, alpha("$ink", 0.55)), x, 6)
    return b, f


# ------------------------------------------------------------------ 20. HEARTS

def gloss_heart(s, x, y, r, col, a=1.0):
    s.heart(x, y, r, fill=lin(x - r, y - r, x + r, y + r, [(0, alpha(lite(0.45, col), a)), (1, alpha(dark(0.12, col), a))]), stroke=alpha(dark(0.3, col), 0.6 * a), sw=0.6)
    s.ellipse(x - r * 0.45, y - r * 0.38, r * 0.3, r * 0.14, fill=alpha(WHITE, 0.6 * a))
    s.circle(x - r * 0.12, y - r * 0.18, r * 0.07, fill=alpha(WHITE, 0.7 * a))


def hearts():
    b = new()
    r = random.Random(14)
    for (x, y) in scatter(10, 3, 6, 294, 6, 94, md=34):
        rad_ = r.uniform(7, 15)
        b.circle(x, y, rad_, fill=rad(x, y, rad_, [(0, alpha(WHITE, 0.34)), (0.7, alpha(WHITE, 0.14)), (1, alpha(WHITE, 0))]))
    row = 0
    y = 10
    while y < 100:
        x = 8 if row % 2 == 0 else 20
        while x < 306:
            b.heart(x, y, 2.4, fill=A(0.13))
            x += 24
        y += 15
        row += 1
    for (x, y, rr, a) in [(54, 58, 21, 1.0), (140, 36, 14, 0.9), (214, 62, 25, 1.0), (268, 26, 10, 0.85)]:
        glow(b, x, y, rr * 1.9, "$accent", 0.35)
        gloss_heart(b, x, y, rr, "$accent", a)
    for (x0, y0) in [(0, 84), (300, 12)]:
        b.curve([(x0, y0), (x0 + (40 if x0 == 0 else -40), y0 - 14), (x0 + (86 if x0 == 0 else -86), y0 + 4)], A(0.4), 0.8)
    f = new()
    r = random.Random(33)
    for (x, y) in scatter(13, 7, 6, 294, 0, 100, md=19, wy=True):
        rr = r.uniform(3.5, 8.5)
        wrapy(lambda yy, x=x, rr=rr: gloss_heart(f, x, yy, rr, "$accent", 0.9), y, rr + 2)
    return b, f


# ------------------------------------------------------------------ 21. FIREWORKS

def firework(s, cx, cy, R, col, n=18, droop=0.0, seed=1):
    r = random.Random(seed)
    s.circle(cx, cy, R * 1.5, fill=rad(cx, cy, R * 1.5, [(0, alpha(col, 0.35)), (0.5, alpha(col, 0.1)), (1, alpha(col, 0))]))
    for i in range(n):
        a = 2 * math.pi * i / n + r.uniform(-0.05, 0.05)
        ln = R * (1.0 if i % 2 == 0 else 0.72)
        pts = []
        for j in range(7):
            t = 0.25 + 0.75 * j / 6.0
            pts.append((cx + math.cos(a) * ln * t, cy + math.sin(a) * ln * t + droop * ln * t * t))
        s.curve(pts, alpha(col, 0.55), 1.3)
        s.curve(pts[2:], alpha(lite(0.5, col), 0.95), 0.7)
        ex, ey = pts[-1]
        s.circle(ex, ey, 1.25, fill=lite(0.65, col))
        s.circle(ex, ey, 3.0, fill=rad(ex, ey, 3.0, [(0, alpha(col, 0.5)), (1, alpha(col, 0))]))
    s.circle(cx, cy, R * 0.12, fill=alpha(lite(0.7, col), 0.9))


def fireworks():
    b = new()
    night_stars(b, 41, 44, 60, 0.7)
    hl = wave_pts(82, [(3, 1, 0.5), (2, 3, 1.1)], step=15)
    b.path(ridge_cmds(hl), fill=mix("$bottom", NIGHT, 0.7))
    r = random.Random(4)
    x = -4
    while x < 304:
        w = r.uniform(7, 13)
        h = r.uniform(8, 20)
        b.rect(x, 82 - h + wave_y(0, [(3, 1, 0.5), (2, 3, 1.1)], x) * 0.0, w, h + 4, fill=mix("$bottom", NIGHT, 0.82))
        x += w + r.uniform(0, 2)
    b.rect(0, 90, 300, 10, fill=lin(0, 90, 0, 100, [(0, mix("$bottom", NIGHT, 0.55)), (1, mix("$bottom", NIGHT, 0.8))]))
    for (x, y, w, a) in [(60, 93, 20, 0.5), (110, 96, 28, 0.4), (200, 94, 24, 0.5), (250, 97, 30, 0.35), (160, 92, 12, 0.4)]:
        b.rect(x - w / 2, y, w, 1.0, r=0.5, fill=alpha("$accent", a))
    cols = ["$accent", mix("$accent", "#FF6F91", 0.55), mix("$accent", "#7FD1FF", 0.6)]
    f1 = new()
    firework(f1, 74, 36, 28, cols[0], seed=1)
    firework(f1, 208, 28, 22, cols[2], n=14, droop=0.35, seed=2)
    firework(f1, 262, 54, 12, cols[1], n=12, seed=3)
    f2 = new()
    firework(f2, 120, 30, 24, cols[1], n=16, droop=0.3, seed=4)
    firework(f2, 238, 38, 27, cols[0], seed=5)
    firework(f2, 30, 52, 11, cols[2], n=12, seed=6)
    return b, f1, f2


# ------------------------------------------------------------------ 22. GALAXY

def spiral(s, cx, cy, scale, tilt, col, seed):
    r = random.Random(seed)
    ca, sa = math.cos(math.radians(tilt)), math.sin(math.radians(tilt))
    for arm in range(3):
        for i in range(90):
            rr = 3 + i * 0.62
            th = arm * 2 * math.pi / 3 + i * 0.105
            jx, jy = r.uniform(-1.6, 1.6), r.uniform(-1.2, 1.2)
            px, py = (rr * math.cos(th) + jx) * scale, (rr * math.sin(th) * 0.42 + jy) * scale
            x, y = cx + px * ca - py * sa, cy + px * sa + py * ca
            a = max(0.1, 0.95 - i / 100.0)
            sz = max(0.4, 1.5 - i / 80.0)
            s.circle(x, y, sz, fill=alpha(lite(0.55 - i / 220.0, col), a))
            if i % 9 == 0:
                s.circle(x, y, sz * 3, fill=rad(x, y, sz * 3, [(0, alpha(col, 0.28)), (1, alpha(col, 0))]))
    s.circle(cx, cy, 15 * scale, fill=rad(cx, cy, 15 * scale, [(0, alpha(WHITE, 0.95)), (0.3, alpha(lite(0.6, col), 0.7)), (1, alpha(col, 0))]))


def galaxy():
    b = new()
    for (x, y, rr, c, a) in [(60, 30, 50, "$accent", 0.28), (170, 78, 60, mix("$accent", "#6F8CFF", 0.6), 0.26), (270, 24, 46, mix("$accent", "#FF8CC0", 0.5), 0.24), (220, 50, 40, "$accent", 0.2)]:
        b.circle(x, y, rr, fill=rad(x, y, rr, [(0, alpha(c, a)), (0.6, alpha(c, a * 0.4)), (1, alpha(c, 0))]))
    night_stars(b, 51, 90, 96, 0.8, rmax=1.3)
    spiral(b, 208, 52, 1.0, -24, "$accent", 5)
    px, py = 52, 62
    ring = ell_cmds(0, 0, 21, 5.2)
    b.path(tcmds([("M", -21, 0)] + ring[3:5], -16, px, py), stroke=alpha(lite(0.5), 0.75), sw=1.6, cap=CAP_ROUND)
    b.circle(px, py, 12.5, fill=lin(px - 10, py - 12, px + 10, py + 12, [(0, lite(0.55)), (0.55, "$accent"), (1, dark(0.55))]))
    b.path(tcmds([("M", -11, -3), ("C", -4, -6, 4, -6, 11, -3), ("L", 11, -1.4), ("C", 4, -4.4, -4, -4.4, -11, -1.4), ("Z",)], 0, px, py + 2), fill=alpha(WHITE, 0.14))
    b.circle(px + 4, py + 4, 12.5, fill=alpha(NIGHT, 0.0))
    b.path(tcmds(ring[:3], -16, px, py), stroke=alpha(lite(0.65), 0.95), sw=1.8, cap=CAP_ROUND)
    b.path(tcmds(ring[:3], -16, px, py), stroke=alpha("$accent", 0.5), sw=0.5, cap=CAP_ROUND)
    f1 = twinkles(8, 11, lite(0.5), 4, 96, 2.0, 4.6, md=24)
    f2 = twinkles(26, 11, lite(0.5), 4, 96, 2.0, 4.6, md=24)
    f2.line(150, 8, 188, 20, alpha(WHITE, 0.95), 1.0)
    f2.line(150, 8, 168, 13, alpha(WHITE, 0.35), 2.0)
    f2.circle(188, 20, 1.5, fill=WHITE)
    return b, f1, f2


# ------------------------------------------------------------------ 23. ZEN

def bamboo(s, x, h, w, col, seed):
    r = random.Random(seed)
    dk = mix(col, DEEP, 0.35)
    s.rect(x - w / 2, 0, w, h, fill=lin(x - w / 2, 0, x + w / 2, 0, [(0, lite(0.15, col)), (0.6, col), (1, dk)]))
    y = h - 12
    while y > 4:
        s.rect(x - w / 2 - 0.3, y, w + 0.6, 1.1, fill=dk)
        if r.random() < 0.6:
            sgn = r.choice((-1, 1))
            lc = mix(col, "#7FB069", 0.45)
            for (dy, ang) in [(0, sgn * 24), (2, sgn * 52)]:
                s.path(tcmds([("M", 0, 0), ("C", 3, -2.2, 10, -2.4, 17, 0), ("C", 10, 2.2, 3, 2.2, 0, 0), ("Z",)], ang, x, y + dy, 0.8, kx=0.8 * sgn), fill=lc)
        y -= r.uniform(14, 20)


def zen():
    b = new()
    glow(b, 70, 30, 54, "#FFFFFF", 0.45, mid=0.5)
    b.arc_stroke(70, 30, 15, 25, 335, alpha("$ink", 0.5), 3.4)
    b.arc_stroke(70, 30, 15, 318, 335, alpha("$ink", 0.35), 2.0)
    far = wave_pts(58, [(4, 1, 0.3), (2.5, 3, 1.8)], step=15)
    b.path(ridge_cmds(far), fill=alpha(mix("$accent", "$top", 0.5), 0.55))
    mid = wave_pts(64, [(3, 1, 1.8), (1.6, 2, 0.4)], step=15)
    b.path(ridge_cmds(mid), fill=alpha(mix("$accent", "$top", 0.28), 0.6))
    b.rect(0, 52, 300, 18, fill=lin(0, 52, 0, 70, [(0, alpha("$top", 0)), (1, alpha(WHITE, 0.35))]))
    b.rect(0, 68, 300, 34, fill=lin(0, 68, 0, 100, [(0, lite(0.5, mix("$bottom", "$accent", 0.25))), (1, mix("$bottom", "$accent", 0.45))]))
    b.rect(0, 67.5, 300, 1.0, fill=alpha("$ink", 0.14))
    for i in range(6):
        y = 74 + i * 4.8
        pts = wave_pts(y, [(0.9, 8, i), (0.5, 17, i * 2)], step=6, x0=-4, x1=150)
        b.curve(pts, alpha("$ink", 0.2), 0.7)
        b.curve([(x, yy + 0.9) for (x, yy) in pts], alpha(WHITE, 0.55), 0.6)
    for (rx, ry, sc) in [(206, 88, 1.0), (262, 92, 0.6)]:
        for k in range(1, 5):
            b.ellipse(rx, ry, (9 + k * 6.5) * sc, (2.8 + k * 1.7) * sc, stroke=alpha("$ink", 0.2 - k * 0.025), sw=0.7)
            b.ellipse(rx, ry + 0.8, (9 + k * 6.5) * sc, (2.8 + k * 1.7) * sc, stroke=alpha(WHITE, 0.45), sw=0.5)
    rock = mix("$accent", "$ink", 0.45)
    for (x, y, w, h, c) in [(206, 88, 15, 15, rock), (219, 91, 9, 8, mix(rock, "$accent", 0.2)), (262, 92, 10, 9, rock), (196, 92, 7, 5.5, mix(rock, "$accent", 0.3))]:
        b.ellipse(x + 1, y + 1.5, w * 1.1, h * 0.25, fill=alpha("$ink", 0.2))
        b.path(tcmds(ell_cmds(0, 0, w, h), 0, x, y - h * 0.55), fill=rad(x - w * 0.35, y - h * 1.1, w * 2.2, [(0, lite(0.25, c)), (0.6, c), (1, mix(c, DEEP, 0.3))]))
    for (x, w, seed) in [(14, 4.5, 1), (28, 3.6, 2), (41, 3.0, 3)]:
        bamboo(b, x, 84 if w > 4 else 76, w, mix("$accent", "#8DB06A", 0.5), seed)
    f = new()
    r = random.Random(77)
    for (x, y) in scatter(9, 4, 6, 294, 8, 70, md=30, wx=True):
        k = r.uniform(0.5, 0.8)
        deg = r.uniform(0, 360)
        col = lite(r.uniform(0.3, 0.55))
        wrapx(lambda xx, y=y, k=k, deg=deg, col=col: f.path(petal_path(k, deg, xx, y), fill=alpha(col, 0.9), stroke=alpha(dark(0.2), 0.3), sw=0.4), x, 9 * k)
    for (x, y, w) in [(40, 46, 34), (150, 38, 44), (250, 52, 30)]:
        wrapx(lambda xx, y=y, w=w: f.ellipse(xx, y, w, 2.6, fill=alpha(WHITE, 0.32)), x, w)
    return b, f


# ------------------------------------------------------------------ 24. STRIPES (caramelo)

CANDY = ["#FF6F91", "#FFC75F", "#4FC3F7", "#9CCC65", "#BA68C8", "#FFFFFF"]


def rot_pts(pts, deg, cx, cy):
    a = math.radians(deg)
    c, s_ = math.cos(a), math.sin(a)
    return [(cx + x * c - y * s_, cy + x * s_ + y * c) for (x, y) in pts]


def lollipop(s, x, y, r, c1, c2, tilt=0):
    s.rect(x - 1.4, y + r * 0.7, 2.8, r * 2.0, r=1.4, fill=lin(x - 1.4, 0, x + 1.4, 0, [(0, "#FFFFFF"), (1, mix(WHITE, c1, 0.4))]), stroke=alpha(DEEP, 0.15), sw=0.4)
    s.circle(x, y, r + 1.1, fill=alpha(DEEP, 0.12))
    s.circle(x, y, r, fill=c2)
    pts = []
    for i in range(0, 101):
        th = i * 0.19
        rr = r * (0.06 + 0.94 * i / 100.0)
        pts.append((x + rr * math.cos(th + math.radians(tilt)), y + rr * math.sin(th + math.radians(tilt))))
    s.curve(pts, c1, max(1.8, r * 0.2))
    s.circle(x, y, r, stroke=mix(c1, DEEP, 0.2), sw=0.7)
    s.ellipse(x - r * 0.4, y - r * 0.5, r * 0.3, r * 0.16, fill=alpha(WHITE, 0.6))


def cane(s, x, y, h, rr, c1):
    pts = [(x, y + h - i * h / 24.0) for i in range(25)]
    pts += [(x - rr + rr * math.cos(math.pi * i / 16.0), y - rr * math.sin(math.pi * i / 16.0)) for i in range(1, 17)]
    s.curve(pts, alpha(DEEP, 0.18), 6.0)
    s.curve(pts, "#FFFFFF", 4.8)
    n = len(pts)
    i = 2
    while i < n - 2:
        seg = pts[i:min(i + 5, n)]
        if len(seg) >= 2:
            s.poly(seg, stroke=c1, sw=4.8, closed=False, cap=CAP_BUTT)
        i += 10
    s.curve([(px - 1.2, py - 0.4) for (px, py) in pts[2:-2]], alpha(WHITE, 0.55), 0.9)


def wrapped_candy(s, x, y, k, c1, c2):
    for sgn in (-1, 1):
        s.poly([(x + sgn * 7 * k, y), (x + sgn * 13 * k, y - 5 * k), (x + sgn * 13 * k, y + 5 * k)], fill=alpha(c1, 0.9), join=JOIN_ROUND)
        s.line(x + sgn * 8 * k, y, x + sgn * 12.5 * k, y - 2.4 * k, alpha(WHITE, 0.6), 0.5)
    s.ellipse(x, y, 8 * k, 5.6 * k, fill=lin(x, y - 5 * k, x, y + 5 * k, [(0, lite(0.5, c1)), (1, c1)]), stroke=mix(c1, DEEP, 0.25), sw=0.6)
    for i in (-1, 0, 1):
        s.line(x + i * 3.6 * k - 1.2 * k, y - 5 * k, x + i * 3.6 * k + 1.2 * k, y + 5 * k, alpha(c2, 0.9), 1.5 * k)
    s.ellipse(x - 2.5 * k, y - 2.6 * k, 2.6 * k, 0.9 * k, fill=alpha(WHITE, 0.6))


def stripes():
    b = new()
    for i in range(-3, 12):
        x0 = i * 44
        b.poly([(x0, 0), (x0 + 22, 0), (x0 + 22 - 100, 100), (x0 - 100, 100)], fill=A(0.22) if i % 2 == 0 else alpha(WHITE, 0.26))
    b.rect(0, 0, 300, 100, fill=lin(0, 0, 300, 0, [(0, alpha(WHITE, 0.0)), (0.5, alpha(WHITE, 0.12)), (1, alpha(WHITE, 0.0))]))
    lollipop(b, 52, 38, 17, "$accent", "#FFFFFF", 10)
    lollipop(b, 236, 30, 13, "#4FC3F7", "#FFFFFF", 70)
    cane(b, 130, 26, 52, 11, "$accent")
    cane(b, 286, 36, 40, 9, "#7C5CE0")
    wrapped_candy(b, 188, 74, 1.2, "$accent", "#FFFFFF")
    wrapped_candy(b, 92, 84, 0.85, "#FFC75F", "#FFFFFF")
    f = new()
    r = random.Random(5)
    for (x, y) in scatter(24, 8, 4, 296, 6, 94, md=18, wx=True):
        c = r.choice(CANDY[:5] + ["$accent"])
        w = r.uniform(3.4, 5.2)
        deg = r.uniform(0, 180)

        def put(xx, y=y, c=c, w=w, deg=deg):
            f.poly(rot_pts([(-w / 2, -0.8), (w / 2, -0.8), (w / 2, 0.8), (-w / 2, 0.8)], deg, xx, y), fill=c, join=JOIN_ROUND, stroke=alpha(DEEP, 0.15), sw=0.3)
        wrapx(put, x, 5)
    return b, f


# ------------------------------------------------------------------ 25. CIRCUIT

def circuit_rows(seed, spacing=15):
    r = random.Random(seed)
    rows = []
    yb = 8
    while yb < 100:
        x, y = -6.0, float(yb)
        pts = [(x, y)]
        off = 0.0
        while x < 306:
            x += r.uniform(16, 46)
            pts.append((x, y))
            if r.random() < 0.65:
                new_off = r.choice([-4.0, 0.0, 4.0])
                if new_off != off:
                    dy = (yb + new_off) - y
                    x += abs(dy)
                    y += dy
                    off = new_off
                    pts.append((x, y))
        rows.append((yb, pts))
        yb += spacing
    return rows


def circuit():
    b = new()
    rows = circuit_rows(7)
    for gx in range(0, 300, 12):
        b.line(gx, 0, gx, 100, alpha("$accent", 0.05), 0.4, cap=CAP_BUTT)
    for (yb, pts) in rows:
        b.poly(pts, stroke=alpha("$accent", 0.34), sw=1.0, closed=False, join=JOIN_ROUND, cap=CAP_ROUND)
        for i, (x, y) in enumerate(pts[1:-1]):
            if i % 2 == 0:
                b.circle(x, y, 2.0, fill=mix("$bottom", NIGHT, 0.4), stroke=alpha("$accent", 0.7), sw=0.8)
    cx, cy = 232, 50
    glow(b, cx, cy, 44, "$accent", 0.4)
    b.rect(cx - 17, cy - 13, 34, 26, r=2.2, fill=lin(cx, cy - 13, cx, cy + 13, [(0, mix("$bottom", NIGHT, 0.35)), (1, mix("$bottom", NIGHT, 0.7))]), stroke=alpha("$accent", 0.85), sw=0.9)
    for i in range(5):
        px = cx - 12 + i * 6
        b.line(px, cy - 13, px, cy - 17, alpha("$accent", 0.9), 1.0, cap=CAP_BUTT)
        b.line(px, cy + 13, px, cy + 17, alpha("$accent", 0.9), 1.0, cap=CAP_BUTT)
    for i in range(4):
        py = cy - 7 + i * 4.6
        b.line(cx - 17, py, cx - 21, py, alpha("$accent", 0.9), 1.0, cap=CAP_BUTT)
        b.line(cx + 17, py, cx + 21, py, alpha("$accent", 0.9), 1.0, cap=CAP_BUTT)
    b.rect(cx - 8, cy - 6, 16, 12, r=1.4, stroke=alpha("$accent", 0.6), sw=0.6)
    b.circle(cx - 11.5, cy - 8.5, 1.1, fill=lite(0.5))
    b.rect(cx - 5, cy - 2.5, 10, 1.1, fill=alpha("$accent", 0.7))
    b.rect(cx - 5, cy + 0.5, 6, 1.1, fill=alpha("$accent", 0.45))
    for (x, y) in [(60, 48), (130, 86), (180, 18), (20, 70)]:
        b.circle(x, y, 4.2, fill=mix("$bottom", NIGHT, 0.5), stroke=alpha("$accent", 0.75), sw=0.9)
        b.circle(x, y, 1.4, fill=lite(0.5))
    f = new()
    r = random.Random(19)
    for (yb, pts) in rows:
        y = yb + 7.5
        x = r.uniform(0, 40)
        while x < 300:
            ln = r.choice((4, 8, 14))
            col = lite(r.uniform(0.2, 0.6))
            wrapx(lambda xx, y=y, ln=ln, col=col: (f.line(xx, y, xx + ln, y, alpha("$accent", 0.35), 2.6), f.line(xx, y, xx + ln, y, col, 0.9),
                                                  f.circle(xx + ln, y, 1.1, fill=WHITE)), x, ln + 3)
            x += r.uniform(60, 120)
    return b, f


# ------------------------------------------------------------------ 26. LANTERNS

def lantern(s, x, y, k, body, glow_c, string_to=None):
    if string_to is not None:
        s.line(x, string_to, x, y - 10 * k, alpha(DEEP, 0.7), 0.6)
    s.circle(x, y, 20 * k, fill=rad(x, y, 20 * k, [(0, alpha(glow_c, 0.55)), (0.5, alpha(glow_c, 0.15)), (1, alpha(glow_c, 0))]))
    s.rect(x - 3.4 * k, y - 10.4 * k, 6.8 * k, 2.6 * k, r=0.8 * k, fill="#C9A24A")
    s.path(tcmds(ell_cmds(0, 0, 8.4, 9.4), 0, x, y, k), fill=rad(x, y - 1 * k, 11 * k, [(0, lite(0.65, body)), (0.6, body), (1, dark(0.25, body))]), stroke=dark(0.35, body), sw=0.5)
    for dx in (-5.2, -2.6, 0, 2.6, 5.2):
        s.path(tcmds([("M", dx * 0.7, -9.2), ("C", dx * 1.15, -4, dx * 1.15, 4, dx * 0.7, 9.2)], 0, x, y, k), stroke=alpha(dark(0.4, body), 0.55), sw=0.4)
    s.rect(x - 3.4 * k, y + 8 * k, 6.8 * k, 2.4 * k, r=0.8 * k, fill="#C9A24A")
    s.line(x, y + 10.4 * k, x, y + 16 * k, "#C9A24A", 0.6 * k)
    for dx in (-1.2, 0, 1.2):
        s.line(x + dx * k, y + 16 * k, x + dx * k * 1.6, y + 21 * k, alpha("#E9C46A", 0.9), 0.4 * k)
    s.ellipse(x - 3 * k, y - 3.5 * k, 1.6 * k, 3 * k, fill=alpha(WHITE, 0.35))


def pagoda(s, x, y, tiers, w, col, lit):
    cur_w = w
    cy = y
    for i in range(tiers):
        h = 7.5
        s.rect(x - cur_w * 0.42, cy - h, cur_w * 0.84, h, fill=col)
        if lit:
            for wx in (-0.2, 0.2):
                s.rect(x + wx * cur_w - 1.2, cy - h + 2, 2.4, 3.2, r=0.5, fill=lite(0.5, "$accent"))
        s.path([("M", x - cur_w * 0.7, cy - h + 1.4), ("C", x - cur_w * 0.3, cy - h - 0.4, x - cur_w * 0.12, cy - h - 4, x, cy - h - 5.6),
                ("C", x + cur_w * 0.12, cy - h - 4, x + cur_w * 0.3, cy - h - 0.4, x + cur_w * 0.7, cy - h + 1.4), ("C", x + cur_w * 0.3, cy - h + 2.6, x - cur_w * 0.3, cy - h + 2.6, x - cur_w * 0.7, cy - h + 1.4), ("Z",)],
               fill=col, join=JOIN_ROUND)
        cy -= h + 3.6
        cur_w *= 0.78
    s.line(x, cy + 2, x, cy - 6, col, 0.9)


def lanterns():
    b = new()
    night_stars(b, 61, 26, 50, 0.6)
    b.path(tcmds(crescent_cmds(8, 62, 0.45), -20, 262, 20), fill=lite(0.65))
    pts = [(-4, 8), (60, 17), (150, 21), (240, 17), (304, 8)]
    sag = catmull(pts)
    b.path(sag, stroke=alpha(DEEP, 0.7), sw=0.7)
    for i in range(1, 20):
        t = i / 20.0
        x = -4 + 308 * t
        y = 8 + 13 * math.sin(math.pi * t) ** 0.9
        b.circle(x, y + 1.6, 4.2, fill=rad(x, y + 1.6, 4.2, [(0, alpha("$accent", 0.55)), (1, alpha("$accent", 0))]))
        b.circle(x, y + 1.6, 1.3, fill=lite(0.55 + 0.2 * (i % 2)))
    for (x, y, k) in [(22, 46, 0.45), (120, 40, 0.4), (192, 48, 0.42), (276, 42, 0.45)]:
        lantern(b, x, y, k, mix("$accent", "#D83A3A", 0.5), "$accent", string_to=y - 30)
    far = mix("$bottom", NIGHT, 0.75)
    hl = wave_pts(92, [(2.5, 1, 0.5), (1.5, 3, 1.0)], step=15)
    b.path(ridge_cmds(hl), fill=mix("$bottom", NIGHT, 0.86))
    pagoda(b, 64, 94, 4, 26, far, True)
    pagoda(b, 226, 95, 3, 22, far, True)
    pagoda(b, 150, 96, 2, 14, mix("$bottom", NIGHT, 0.8), False)
    f = new()
    for (x, y, k, s_) in [(34, 36, 1.1, 0), (92, 56, 0.9, 0), (156, 40, 1.25, 0), (226, 58, 0.95, 0), (278, 34, 1.05, 0)]:
        lantern(f, x, y, k, mix("$accent", "#D83A3A", 0.5), "$accent", string_to=-4)
    for (x, y) in [(60, 20), (130, 70), (200, 22), (260, 66)]:
        sparkle_dot(f, x, y, 2.6, lite(0.6), 0.8)
    return b, f


# ------------------------------------------------------------------ 27. CONFETTI

def bunting(s, x0, x1, y0, sag, n, seed):
    r = random.Random(seed)
    pts = [(x0 + (x1 - x0) * i / 24.0, y0 + sag * math.sin(math.pi * i / 24.0)) for i in range(25)]
    s.curve(pts, alpha(DEEP, 0.45), 0.8)
    for i in range(n):
        t = (i + 0.5) / n
        x = x0 + (x1 - x0) * t
        y = y0 + sag * math.sin(math.pi * t)
        c = CANDY[i % 5] if i % 5 != 0 else "$accent"
        w = (x1 - x0) / n * 0.42
        s.poly([(x - w, y - 0.5), (x + w, y - 0.5), (x, y + w * 1.9)], fill=c, stroke=alpha(DEEP, 0.15), sw=0.4, join=JOIN_ROUND)
        s.poly([(x - w, y - 0.5), (x, y - 0.5), (x, y + w * 1.9)], fill=alpha(WHITE, 0.2))


def streamer(s, x, y, col, curls, dirn=1):
    pts = []
    for i in range(0, 60):
        t = i / 59.0
        pts.append((x + dirn * (t * 34 + 4 * math.sin(t * curls * math.pi * 2)), y + t * 48))
    s.curve(pts, alpha(col, 0.9), 2.4)
    s.curve([(px - 0.5, py - 0.3) for (px, py) in pts], alpha(WHITE, 0.35), 0.7)


def confetti():
    b = new()
    for (x, y, rr) in [(40, 60, 16), (130, 40, 12), (210, 72, 18), (270, 38, 13), (88, 80, 9)]:
        b.circle(x, y, rr, fill=rad(x, y, rr, [(0, alpha(WHITE, 0.4)), (1, alpha(WHITE, 0))]))
    bunting(b, -4, 150, 2, 12, 9, 1)
    bunting(b, 150, 304, 2, 12, 9, 2)
    streamer(b, 8, 52, "#FF6F91", 2.0, 1)
    streamer(b, 292, 46, "#4FC3F7", 2.4, -1)
    streamer(b, 30, 60, "$accent", 1.6, 1)
    streamer(b, 270, 56, "#FFC75F", 1.8, -1)
    f = new()
    r = random.Random(23)
    cols = CANDY[:5] + ["$accent"]
    for (x, y) in scatter(26, 9, 4, 296, 0, 100, md=14, wy=True):
        c = r.choice(cols)
        kind = r.randrange(5)
        w = r.uniform(3.6, 6.2)
        deg = r.uniform(0, 180)

        def put(yy, x=x, c=c, kind=kind, w=w, deg=deg):
            if kind == 0:
                f.poly(rot_pts([(-w / 2, -1.1), (w / 2, -1.1), (w / 2, 1.1), (-w / 2, 1.1)], deg, x, yy), fill=c)
                f.poly(rot_pts([(-w / 2, -1.1), (w / 2, -1.1), (w / 2, 0), (-w / 2, 0)], deg, x, yy), fill=alpha(WHITE, 0.3))
            elif kind == 1:
                f.circle(x, yy, w * 0.38, fill=c)
                f.circle(x - w * 0.12, yy - w * 0.12, w * 0.12, fill=alpha(WHITE, 0.6))
            elif kind == 2:
                f.poly(rot_pts([(0, -w * 0.55), (w * 0.5, w * 0.4), (-w * 0.5, w * 0.4)], deg, x, yy), fill=c, join=JOIN_ROUND)
            elif kind == 3:
                f.star(x, yy, w * 0.55, 5, 0.45, rot=deg, fill=c)
            else:
                pts = [(x + (-w + w * 2 * i / 8.0), yy + math.sin(i * 1.2) * 1.4) for i in range(9)]
                f.curve(rot_pts([(px - x, py - yy) for (px, py) in pts], deg, x, yy), c, 1.2)
        wrapy(put, y, 8)
    return b, f


# ------------------------------------------------------------------ 28. RAIN

def rain():
    b = new()
    cb = mix("$accent", "$top", 0.35)
    for (x, y, k, a) in [(40, 2, 2.0, 0.55), (150, -2, 2.3, 0.6), (252, 3, 2.1, 0.55), (96, 10, 1.5, 0.75), (206, 12, 1.6, 0.75), (-6, 12, 1.5, 0.8), (296, 14, 1.4, 0.8)]:
        cloud(b, x, y, k, a, body=cb, shade=alpha(DEEP, 0.14))
    hl = wave_pts(76, [(3, 1, 0.2), (2, 3, 1.1)], step=15)
    b.path(ridge_cmds(hl), fill=alpha(mix("$accent", "$bottom", 0.5), 0.55))
    x = -6
    r = random.Random(3)
    while x < 300:
        w = r.uniform(8, 14)
        h = r.uniform(8, 20)
        b.rect(x, 78 - h, w, h + 6, fill=alpha(mix("$accent", DEEP, 0.4), 0.5))
        x += w + r.uniform(1, 3)
    b.rect(0, 82, 300, 20, fill=lin(0, 82, 0, 100, [(0, mix("$bottom", "$accent", 0.2)), (1, mix("$bottom", DEEP, 0.3))]))
    lx = 252
    b.circle(lx, 40, 28, fill=rad(lx, 40, 28, [(0, alpha(lite(0.7), 0.4)), (1, alpha(lite(0.7), 0))]))
    b.rect(lx - 0.9, 38, 1.8, 50, fill=mix("$accent", DEEP, 0.6))
    b.rect(lx - 4.5, 34, 9, 4.5, r=1.4, fill=lite(0.7))
    b.poly([(lx - 6, 34), (lx, 28), (lx + 6, 34)], fill=mix("$accent", DEEP, 0.6), join=JOIN_ROUND)
    for (x, y, w) in [(60, 91, 22), (150, 95, 30), (214, 92, 20), (260, 94, 26)]:
        b.ellipse(x, y, w, 3.2, fill=alpha(lite(0.5), 0.3), stroke=alpha(WHITE, 0.4), sw=0.5)
        b.ellipse(x, y, w * 0.55, 1.6, stroke=alpha(WHITE, 0.45), sw=0.5)
    b.ellipse(lx, 92, 8, 1.8, fill=alpha(lite(0.7), 0.45))
    f = new()
    r = random.Random(11)
    col = mix("$accent", WHITE, 0.4)
    for (x, y) in scatter(34, 4, 2, 298, 0, 100, md=11, wy=True, wx=True):
        ln = r.uniform(6, 11)
        a = r.uniform(0.45, 0.8)
        wrapy(lambda yy, x=x, ln=ln, a=a: f.line(x, yy, x - ln * 0.22, yy + ln, alpha(col, a), 0.7), y, ln + 1)
    return b, f


# ------------------------------------------------------------------ 29. SUNSET

def palm(s, x, y, k, col):
    s.path([("M", x - 1.2 * k, y), ("C", x - 3.5 * k, y - 12 * k, x - 1 * k, y - 22 * k, x + 4 * k, y - 29 * k), ("L", x + 5.4 * k, y - 28 * k),
            ("C", x + 1 * k, y - 21 * k, x - 0.4 * k, y - 12 * k, x + 1.4 * k, y), ("Z",)], fill=col)
    for (deg, sgn) in [(-80, 1), (-42, 1), (-12, 1), (22, 1), (42, -1), (12, -1), (-24, -1)]:
        s.path(tcmds([("M", 0, 0), ("C", 8, -7, 17, -5, 23, 3), ("C", 17, -1, 8, -1, 0, 1.5), ("Z",)], deg, x + 4.6 * k, y - 29 * k, k, kx=k * sgn), fill=col)


def sunset():
    b = new()
    HZ = 60
    sx, sy = 150, 56
    glow(b, sx, sy, 90, "$accent", 0.55, mid=0.5)
    b.circle(sx, sy, 30, fill=alpha("$accent", 0.2))
    b.circle(sx, sy, 22, fill=lin(sx, sy - 22, sx, sy + 22, [(0, lite(0.65)), (1, "$accent")]))
    for (x, y, w, a) in [(40, 30, 50, 0.3), (230, 22, 60, 0.28), (92, 44, 44, 0.24), (270, 46, 40, 0.26), (150, 18, 70, 0.2)]:
        b.ellipse(x, y, w, 2.8, fill=alpha(lite(0.5), a))
        b.ellipse(x + 6, y + 3.6, w * 0.7, 1.8, fill=alpha("$accent", a * 0.7))
    b.rect(0, HZ, 300, 42, fill=lin(0, HZ, 0, 100, [(0, mix("$bottom", "$accent", 0.28)), (1, mix("$bottom", DEEP, 0.5))]))
    b.rect(0, HZ - 0.5, 300, 1.2, fill=alpha(lite(0.7), 0.6))
    for k, (w, a) in enumerate([(52, 0.6), (44, 0.5), (36, 0.42), (28, 0.35), (20, 0.3), (12, 0.25)]):
        b.rect(sx - w / 2, HZ + 3 + k * 6, w, 1.8, r=0.9, fill=alpha(lite(0.55), a))
    r = random.Random(12)
    for (x, y) in scatter(26, 5, 4, 296, HZ + 5, 98, md=11):
        w = r.uniform(5, 12)
        b.rect(x - w / 2, y, w, 0.9, r=0.45, fill=alpha(lite(0.5), r.uniform(0.15, 0.35)))
    sil = mix("$bottom", DEEP, 0.78)
    b.path(ridge_cmds([(204, 76), (222, 68), (244, 62), (268, 62), (292, 68), (312, 76)], bottom=HZ + 4), fill=sil)
    palm(b, 248, 62, 1.0, sil)
    palm(b, 270, 63, 0.75, sil)
    bx, by = 52, HZ + 6
    b.path([("M", bx - 8, by - 1), ("L", bx + 9, by - 1), ("L", bx + 5.5, by + 3), ("L", bx - 5.5, by + 3), ("Z",)], fill=sil)
    b.line(bx, by - 1, bx, by - 15, sil, 0.8)
    b.poly([(bx + 0.8, by - 14), (bx + 7, by - 3), (bx + 0.8, by - 3)], fill=sil)
    b.poly([(bx - 0.8, by - 11), (bx - 6, by - 3), (bx - 0.8, by - 3)], fill=sil)
    f = new()
    for (x, y, w, a) in [(30, 14, 40, 0.55), (110, 30, 52, 0.5), (190, 10, 44, 0.55), (262, 26, 50, 0.5)]:
        wrapx(lambda xx, y=y, w=w, a=a: (f.ellipse(xx, y, w, 3.0, fill=alpha(lite(0.55), a)), f.ellipse(xx + 5, y + 4, w * 0.7, 1.8, fill=alpha("$accent", a * 0.6))), x, w)
    for (x, y, k) in [(70, 40, 1.2), (84, 45, 0.9), (210, 36, 1.0), (222, 41, 0.8), (240, 33, 0.7)]:
        wrapx(lambda xx, y=y, k=k: bird(f, xx, y, k, alpha(DEEP, 0.7), 1.0), x, 6)
    return b, f


# ------------------------------------------------------------------ 30. CRYSTALS

def crystal(s, x, y, w, h, deg, col, a=1.0):
    def T(pts):
        return rot_pts(pts, deg, x, y)
    ap = -h - w * 1.25
    s.poly(T([(-w, 0), (-w, -h), (0, -h - w * 0.4), (0, 0)]), fill=alpha(lite(0.3, col), a))
    s.poly(T([(0, 0), (0, -h - w * 0.4), (w, -h), (w, 0)]), fill=alpha(dark(0.3, col), a))
    s.poly(T([(-w, -h), (0, ap), (0, -h - w * 0.4)]), fill=alpha(lite(0.72, col), a))
    s.poly(T([(0, ap), (w, -h), (0, -h - w * 0.4)]), fill=alpha(lite(0.42, col), a))
    s.poly(T([(-w, 0), (-w, -h), (0, ap), (w, -h), (w, 0)]), stroke=alpha(lite(0.6, col), 0.8 * a), sw=0.5, join=JOIN_ROUND)
    s.poly(T([(-w * 0.55, -h * 0.15), (-w * 0.55, -h * 0.7)]), stroke=alpha(WHITE, 0.55 * a), sw=0.8, closed=False, cap=CAP_ROUND)


def crystals():
    b = new()
    for (x, y, rr) in [(150, 84, 60), (60, 90, 44), (250, 88, 52)]:
        b.circle(x, y, rr, fill=rad(x, y, rr, [(0, alpha("$accent", 0.38)), (1, alpha("$accent", 0))]))
    for (x, w, h, d) in [(104, 8, 22, -12), (196, 9, 26, 10), (22, 7, 18, 8), (280, 8, 20, -10)]:
        crystal(b, x, 101, w, h, d, mix("$accent", "$bottom", 0.55), 0.8)
    for (x, w, h, d) in [(150, 12, 36, -6), (174, 9, 22, 14), (127, 8, 20, -22), (62, 10, 28, 8), (40, 7, 17, -14), (240, 11, 31, -5), (262, 8, 18, 16), (216, 7, 14, 26), (82, 6, 12, 20)]:
        crystal(b, x, 102, w, h, d, "$accent", 1.0)
    r = random.Random(8)
    for (x, y) in scatter(9, 5, 8, 292, 8, 60, md=30):
        s_ = r.uniform(2.4, 4.2)
        b.poly([(x, y - s_ * 1.5), (x + s_, y), (x, y + s_ * 1.5), (x - s_, y)], fill=alpha(lite(0.5), 0.7), stroke=alpha(WHITE, 0.6), sw=0.4, join=JOIN_ROUND)
    return b, twinkles(4, 11, lite(0.55), 6, 94, 2.2, 5.0, md=26), twinkles(21, 11, lite(0.55), 6, 94, 2.2, 5.0, md=26)


# ----- FIN PATRONES -----

def build():
    icons = {}
    for name in ORDER:
        fn = globals().get(name.lower())
        if fn is None:
            continue
        res = fn()
        icons["banner." + name] = res[0].bake()
        icons["banner.%s.fx" % name] = res[1].bake()
        if MODES[name] == "twinkle":
            icons["banner.%s.fx2" % name] = res[2].bake()
    return {"icons": icons}
