"""Kit para dibujar piezas del album rapido: figuras con degradado, contorno fino y brillo. Todo en coordenadas 0..100."""
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, ".."))
from artlib import *  # noqa: F401,F403
from artlib import parse_path_d, _hex_to_rgba  # noqa: F401


def mixc(a, b, t):
    ra, rb = _hex_to_rgba(a), _hex_to_rgba(b)
    return "#%02X%02X%02X" % tuple(int(round(ra[i] + (rb[i] - ra[i]) * t)) for i in range(3))


def lt(c, t=0.38):
    return mixc(c, "#FFFFFF", t)


def dk(c, t=0.22):
    return mixc(c, "#1E1030", t)


def _bbox_d(d):
    xs, ys = [], []
    for cmd in parse_path_d(d):
        v = cmd[1:]
        xs += v[0::2]
        ys += v[1::2]
    return min(xs), min(ys), max(xs), max(ys)


class Pc:
    """Lienzo de una pieza. Cada figura lleva degradado de luz (arriba-izquierda) y contorno del mismo tono, mas oscuro."""

    def __init__(self, shadow=True, sh_y=91, sh_r=26):
        self.s = Scene(100, 100)
        if shadow:
            self.s.ellipse(50, sh_y, sh_r, 4.2, fill="#00000024")

    def _fill(self, c, box):
        x0, y0, x1, y1 = box
        if isinstance(c, (list, tuple)):  # (claro, oscuro)
            top, bot = c
        else:
            top, bot = lt(c, 0.34), dk(c, 0.14)
        return lin(x0, y0, x1, y1, [(0, top), (1, bot)])

    def path(self, d, c, ol=True, sw=1.5, op=None):
        box = _bbox_d(d)
        base = c[1] if isinstance(c, (list, tuple)) else c
        self.s.path(d, fill=self._fill(c, box), stroke=dk(base, 0.4) if ol else None, sw=sw if ol else 0, join=JOIN_ROUND, cap=CAP_ROUND, op=op)

    def circ(self, cx, cy, r, c, ol=True, sw=1.5):
        base = c[1] if isinstance(c, (list, tuple)) else c
        self.s.circle(cx, cy, r, fill=self._fill(c, (cx - r, cy - r, cx + r, cy + r)), stroke=dk(base, 0.4) if ol else None, sw=sw if ol else 0)

    def ell(self, cx, cy, rx, ry, c, ol=True, sw=1.5, rot=0):
        base = c[1] if isinstance(c, (list, tuple)) else c
        if rot:
            with self.s.rotate(rot, cx, cy):
                self.s.ellipse(cx, cy, rx, ry, fill=self._fill(c, (cx - rx, cy - ry, cx + rx, cy + ry)), stroke=dk(base, 0.4) if ol else None, sw=sw if ol else 0)
        else:
            self.s.ellipse(cx, cy, rx, ry, fill=self._fill(c, (cx - rx, cy - ry, cx + rx, cy + ry)), stroke=dk(base, 0.4) if ol else None, sw=sw if ol else 0)

    def rect(self, x, y, w, h, c, r=3, ol=True, sw=1.5):
        base = c[1] if isinstance(c, (list, tuple)) else c
        self.s.rect(x, y, w, h, r=r, fill=self._fill(c, (x, y, x + w, y + h)), stroke=dk(base, 0.4) if ol else None, sw=sw if ol else 0)

    def poly(self, pts, c, ol=True, sw=1.5):
        base = c[1] if isinstance(c, (list, tuple)) else c
        xs = [p[0] for p in pts]
        ys = [p[1] for p in pts]
        self.s.poly(pts, fill=self._fill(c, (min(xs), min(ys), max(xs), max(ys))), stroke=dk(base, 0.4) if ol else None, sw=sw if ol else 0, join=JOIN_ROUND)

    def line(self, x1, y1, x2, y2, c, w=2.5):
        self.s.line(x1, y1, x2, y2, c, w)

    def curve(self, d, c, w=2.5, op=None):
        self.s.path(d, stroke=c, sw=w, cap=CAP_ROUND, join=JOIN_ROUND, op=op)

    def flat(self, d, c, op=None):
        self.s.path(d, fill=c, op=op)

    def dot(self, cx, cy, r, c, op=None):
        self.s.circle(cx, cy, r, fill=c, op=op)

    def shine(self, cx, cy, rx=6, ry=3, rot=-30, a=0.55):
        with self.s.rotate(rot, cx, cy):
            self.s.ellipse(cx, cy, rx, ry, fill="#FFFFFF", op=a)

    def eyes(self, x1, x2, y, r=2.4, c="#2B1B3A"):
        for x in (x1, x2):
            self.s.circle(x, y, r, fill=c)
            self.s.circle(x + r * 0.35, y - r * 0.35, r * 0.38, fill="#FFFFFF")

    def star(self, cx, cy, r, c, pts=5):
        base = c[1] if isinstance(c, (list, tuple)) else c
        self.s.star(cx, cy, r, points=pts, fill=self._fill(c, (cx - r, cy - r, cx + r, cy + r)), stroke=dk(base, 0.4), sw=1.3)

    def sparkle(self, cx, cy, r, c="#FFFFFF", a=0.9):
        self.s.sparkle(cx, cy, r, fill=c, op=a)

    def flower(self, cx, cy, r, n, petal, center, rot=0):
        for k in range(n):
            a = rot + k * 360.0 / n
            px = cx + math.cos(math.radians(a)) * r * 0.55
            py = cy + math.sin(math.radians(a)) * r * 0.55
            self.ell(px, py, r * 0.46, r * 0.3, petal, sw=1.1, rot=a)
        self.circ(cx, cy, r * 0.3, center, sw=1.1)

    def leaf(self, x, y, ln, wd, ang, c):
        with self.s.rotate(ang, x, y):
            self.path("M %f %f Q %f %f %f %f Q %f %f %f %f Z" % (x, y, x + ln * 0.5, y - wd, x + ln, y, x + ln * 0.5, y + wd, x, y), c, sw=1.1)

    def out(self):
        return self.s.bake()
