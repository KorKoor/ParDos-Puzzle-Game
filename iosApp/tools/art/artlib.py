"""
Libreria de dibujo vectorial de ParDos para iPhone.

Se dibuja en Python (con un lienzo de 100 x 100 por defecto), se previsualiza como PNG con render.py
y se guarda como JSON en iosApp/ParDos/Art/*.json. La app lee ese JSON (Art.swift) y lo pinta con Canvas.
Asi se pueden ver los dibujos antes de compilar y no hay emojis en ninguna pantalla.

Formato de un dibujo ya "cocido" (lo que se guarda):
    {"w": 100, "h": 100, "pal": {"ranura": "#RRGGBB", ...}, "s": [forma, ...]}
Forma:
    "p": [codigo, numeros...]  0=mover(x,y) 1=linea(x,y) 2=curva(x1,y1,x2,y2,x,y) 3=cerrar
    "f": pintura de relleno (opcional)   "k": pintura del trazo (opcional)   "w": grosor del trazo
    "c": remate 0 recto 1 redondo 2 cuadrado    "j": union 0 punta 1 redonda 2 biselada
    "o": opacidad 0..1 (opcional)   "e": 1 = relleno par/impar (agujeros)   "g": etiqueta (0 siempre, 1 ojos abiertos, 2 ojos cerrados)
Pintura:
    "#RRGGBB" | "#RRGGBBAA" | "$ranura" (color de la paleta) | ["m", pintura, pintura, t] (mezcla) | ["a", pintura, alfa]
    {"t":"l","p":[x1,y1,x2,y2],"st":[[desplazamiento, color], ...]}  degradado lineal
    {"t":"r","p":[cx,cy,r],"st":[...]}                                 degradado radial
"""
import json
import math
import re
from contextlib import contextmanager

KAPPA = 0.5522847498307936

CAP_BUTT, CAP_ROUND, CAP_SQUARE = 0, 1, 2
JOIN_MITER, JOIN_ROUND, JOIN_BEVEL = 0, 1, 2

TAG_ALWAYS, TAG_EYES_OPEN, TAG_EYES_CLOSED = 0, 1, 2


# ------------------------------------------------------------------ colores

def rgb(r, g, b, a=1.0):
    """Color a partir de numeros 0..255 (alfa 0..1)."""
    base = "#%02X%02X%02X" % (int(round(r)), int(round(g)), int(round(b)))
    if a < 0.999:
        base += "%02X" % int(round(a * 255))
    return base


def mix(a, b, t):
    """Mezcla dos pinturas de color: t=0 es a, t=1 es b."""
    return ["m", a, b, round(float(t), 3)]


def alpha(c, a):
    """Mismo color con otra opacidad (0..1)."""
    return ["a", c, round(float(a), 3)]


def slot(name):
    return "$" + name


def _hex_to_rgba(h):
    h = h.lstrip("#")
    if len(h) == 3:
        h = "".join(ch * 2 for ch in h)
    r, g, b = int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16)
    a = int(h[6:8], 16) / 255.0 if len(h) >= 8 else 1.0
    return (r, g, b, a)


def resolve_color(spec, pal):
    """Pintura de color -> (r, g, b, a) con r,g,b en 0..255."""
    if isinstance(spec, str):
        if spec.startswith("$"):
            name = spec[1:]
            if name not in pal:
                raise KeyError("ranura sin color en la paleta: " + name)
            return resolve_color(pal[name], pal)
        return _hex_to_rgba(spec)
    if isinstance(spec, (list, tuple)):
        kind = spec[0]
        if kind == "m":
            c1 = resolve_color(spec[1], pal)
            c2 = resolve_color(spec[2], pal)
            t = spec[3]
            return tuple(c1[i] + (c2[i] - c1[i]) * t for i in range(4))
        if kind == "a":
            c = resolve_color(spec[1], pal)
            return (c[0], c[1], c[2], c[3] * spec[2])
    raise ValueError("pintura de color no valida: %r" % (spec,))


# ------------------------------------------------------------------ matrices (a b c d e f)

def _mul(m, n):
    a1, b1, c1, d1, e1, f1 = m
    a2, b2, c2, d2, e2, f2 = n
    return (
        a1 * a2 + c1 * b2, b1 * a2 + d1 * b2,
        a1 * c2 + c1 * d2, b1 * c2 + d1 * d2,
        a1 * e2 + c1 * f2 + e1, b1 * e2 + d1 * f2 + f1,
    )


def _apply(m, x, y):
    return (m[0] * x + m[2] * y + m[4], m[1] * x + m[3] * y + m[5])


def _num(v):
    v = round(float(v), 2)
    if v == int(v):
        return int(v)
    return v


# ------------------------------------------------------------------ arcos SVG -> curvas

def _arc_to_cubics(x1, y1, rx, ry, phi_deg, large, sweep, x2, y2):
    if rx == 0 or ry == 0 or (x1 == x2 and y1 == y2):
        return [("L", x2, y2)]
    rx, ry = abs(rx), abs(ry)
    phi = math.radians(phi_deg)
    cp, sp = math.cos(phi), math.sin(phi)
    dx, dy = (x1 - x2) / 2.0, (y1 - y2) / 2.0
    x1p = cp * dx + sp * dy
    y1p = -sp * dx + cp * dy
    lam = (x1p ** 2) / (rx ** 2) + (y1p ** 2) / (ry ** 2)
    if lam > 1:
        s = math.sqrt(lam)
        rx *= s
        ry *= s
    num = rx ** 2 * ry ** 2 - rx ** 2 * y1p ** 2 - ry ** 2 * x1p ** 2
    den = rx ** 2 * y1p ** 2 + ry ** 2 * x1p ** 2
    co = math.sqrt(max(0.0, num / den)) if den != 0 else 0.0
    if bool(large) == bool(sweep):
        co = -co
    cxp = co * rx * y1p / ry
    cyp = -co * ry * x1p / rx
    cx = cp * cxp - sp * cyp + (x1 + x2) / 2.0
    cy = sp * cxp + cp * cyp + (y1 + y2) / 2.0

    def ang(ux, uy, vx, vy):
        dot = ux * vx + uy * vy
        ln = math.hypot(ux, uy) * math.hypot(vx, vy)
        a = math.acos(max(-1.0, min(1.0, dot / ln)))
        if ux * vy - uy * vx < 0:
            a = -a
        return a

    th1 = ang(1, 0, (x1p - cxp) / rx, (y1p - cyp) / ry)
    dth = ang((x1p - cxp) / rx, (y1p - cyp) / ry, (-x1p - cxp) / rx, (-y1p - cyp) / ry)
    if not sweep and dth > 0:
        dth -= 2 * math.pi
    elif sweep and dth < 0:
        dth += 2 * math.pi
    segs = max(1, int(math.ceil(abs(dth) / (math.pi / 2) - 1e-9)))
    delta = dth / segs
    t = 4.0 / 3.0 * math.tan(delta / 4.0)
    out = []
    a1 = th1
    for _ in range(segs):
        a2 = a1 + delta
        c1, s1 = math.cos(a1), math.sin(a1)
        c2, s2 = math.cos(a2), math.sin(a2)
        pts = [
            (c1 - t * s1, s1 + t * c1),
            (c2 + t * s2, s2 - t * c2),
            (c2, s2),
        ]
        mapped = []
        for (ux, uy) in pts:
            ux *= rx
            uy *= ry
            mapped.append((cp * ux - sp * uy + cx, sp * ux + cp * uy + cy))
        out.append(("C", mapped[0][0], mapped[0][1], mapped[1][0], mapped[1][1], mapped[2][0], mapped[2][1]))
        a1 = a2
    return out


_TOKEN = re.compile(r"([MmLlHhVvCcSsQqTtAaZz])|(-?(?:\d+\.?\d*|\.\d+)(?:[eE][-+]?\d+)?)")


def parse_path_d(d):
    """Datos de camino SVG -> lista de comandos absolutos ('M','L','C','Z')."""
    tokens = []
    for m in _TOKEN.finditer(d):
        if m.group(1):
            tokens.append(m.group(1))
        else:
            tokens.append(float(m.group(2)))
    out = []
    i = 0
    cmd = None
    cx = cy = sx = sy = 0.0
    last_c2 = None  # ultimo punto de control cubico (para S)
    last_q = None   # ultimo punto de control cuadratico (para T)

    def take(n):
        nonlocal i
        vals = tokens[i:i + n]
        if len(vals) < n or any(isinstance(v, str) for v in vals):
            raise ValueError("camino SVG incompleto: " + d)
        i += n
        return vals

    while i < len(tokens):
        t = tokens[i]
        if isinstance(t, str):
            cmd = t
            i += 1
            if cmd in "Zz":
                out.append(("Z",))
                cx, cy = sx, sy
                last_c2 = last_q = None
                continue
        elif cmd is None:
            raise ValueError("camino SVG sin comando: " + d)
        rel = cmd.islower()
        c = cmd.upper()
        if c == "M":
            x, y = take(2)
            if rel:
                x += cx
                y += cy
            out.append(("M", x, y))
            cx, cy = x, y
            sx, sy = x, y
            cmd = "l" if rel else "L"
            last_c2 = last_q = None
        elif c == "L":
            x, y = take(2)
            if rel:
                x += cx
                y += cy
            out.append(("L", x, y))
            cx, cy = x, y
            last_c2 = last_q = None
        elif c == "H":
            (x,) = take(1)
            if rel:
                x += cx
            out.append(("L", x, cy))
            cx = x
            last_c2 = last_q = None
        elif c == "V":
            (y,) = take(1)
            if rel:
                y += cy
            out.append(("L", cx, y))
            cy = y
            last_c2 = last_q = None
        elif c == "C":
            x1, y1, x2, y2, x, y = take(6)
            if rel:
                x1 += cx; y1 += cy; x2 += cx; y2 += cy; x += cx; y += cy
            out.append(("C", x1, y1, x2, y2, x, y))
            last_c2 = (x2, y2)
            last_q = None
            cx, cy = x, y
        elif c == "S":
            x2, y2, x, y = take(4)
            if rel:
                x2 += cx; y2 += cy; x += cx; y += cy
            if last_c2 is not None:
                x1, y1 = 2 * cx - last_c2[0], 2 * cy - last_c2[1]
            else:
                x1, y1 = cx, cy
            out.append(("C", x1, y1, x2, y2, x, y))
            last_c2 = (x2, y2)
            last_q = None
            cx, cy = x, y
        elif c == "Q":
            qx, qy, x, y = take(4)
            if rel:
                qx += cx; qy += cy; x += cx; y += cy
            out.append(_quad_to_cubic(cx, cy, qx, qy, x, y))
            last_q = (qx, qy)
            last_c2 = None
            cx, cy = x, y
        elif c == "T":
            x, y = take(2)
            if rel:
                x += cx; y += cy
            if last_q is not None:
                qx, qy = 2 * cx - last_q[0], 2 * cy - last_q[1]
            else:
                qx, qy = cx, cy
            out.append(_quad_to_cubic(cx, cy, qx, qy, x, y))
            last_q = (qx, qy)
            last_c2 = None
            cx, cy = x, y
        elif c == "A":
            rx, ry, rot, large, sweep, x, y = take(7)
            if rel:
                x += cx; y += cy
            out.extend(_arc_to_cubics(cx, cy, rx, ry, rot, int(large), int(sweep), x, y))
            cx, cy = x, y
            last_c2 = last_q = None
        else:
            raise ValueError("comando de camino desconocido: " + cmd)
    return out


def _quad_to_cubic(x0, y0, qx, qy, x, y):
    return ("C", x0 + 2.0 / 3.0 * (qx - x0), y0 + 2.0 / 3.0 * (qy - y0),
            x + 2.0 / 3.0 * (qx - x), y + 2.0 / 3.0 * (qy - y), x, y)


# ------------------------------------------------------------------ constructor de caminos

class Path:
    """Camino en coordenadas locales. Metodos encadenables."""

    def __init__(self):
        self.cmds = []
        self._cx = 0.0
        self._cy = 0.0

    def M(self, x, y):
        self.cmds.append(("M", x, y)); self._cx, self._cy = x, y; return self

    def L(self, x, y):
        self.cmds.append(("L", x, y)); self._cx, self._cy = x, y; return self

    def C(self, x1, y1, x2, y2, x, y):
        self.cmds.append(("C", x1, y1, x2, y2, x, y)); self._cx, self._cy = x, y; return self

    def Q(self, qx, qy, x, y):
        self.cmds.append(_quad_to_cubic(self._cx, self._cy, qx, qy, x, y)); self._cx, self._cy = x, y; return self

    def A(self, rx, ry, rot, large, sweep, x, y):
        self.cmds.extend(_arc_to_cubics(self._cx, self._cy, rx, ry, rot, large, sweep, x, y))
        self._cx, self._cy = x, y
        return self

    def Z(self):
        self.cmds.append(("Z",)); return self

    @staticmethod
    def from_d(d):
        p = Path()
        p.cmds = parse_path_d(d)
        return p


def _ellipse_cmds(cx, cy, rx, ry):
    kx, ky = rx * KAPPA, ry * KAPPA
    return [
        ("M", cx + rx, cy),
        ("C", cx + rx, cy + ky, cx + kx, cy + ry, cx, cy + ry),
        ("C", cx - kx, cy + ry, cx - rx, cy + ky, cx - rx, cy),
        ("C", cx - rx, cy - ky, cx - kx, cy - ry, cx, cy - ry),
        ("C", cx + kx, cy - ry, cx + rx, cy - ky, cx + rx, cy),
        ("Z",),
    ]


def _rrect_cmds(x, y, w, h, r):
    r = max(0.0, min(r, w / 2.0, h / 2.0))
    if r <= 0:
        return [("M", x, y), ("L", x + w, y), ("L", x + w, y + h), ("L", x, y + h), ("Z",)]
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


# ------------------------------------------------------------------ pinturas con degradado

def lin(x1, y1, x2, y2, stops):
    """Degradado lineal en coordenadas locales. stops: [(desplazamiento 0..1, color), ...]."""
    return {"t": "l", "p": [x1, y1, x2, y2], "st": [[round(float(o), 3), c] for (o, c) in stops]}


def rad(cx, cy, r, stops):
    """Degradado radial en coordenadas locales."""
    return {"t": "r", "p": [cx, cy, r], "st": [[round(float(o), 3), c] for (o, c) in stops]}


def vgrad(top, bottom, y1=0.0, y2=100.0):
    """Degradado vertical corto de escribir."""
    return lin(0, y1, 0, y2, [(0, top), (1, bottom)])


# ------------------------------------------------------------------ escena

class Scene:
    """Lienzo vectorial. Todas las formas se dibujan en coordenadas locales y se guardan ya transformadas."""

    def __init__(self, w=100.0, h=100.0, pal=None):
        self.w = float(w)
        self.h = float(h)
        self.pal = dict(pal or {})
        self.shapes = []
        self._m = (1.0, 0.0, 0.0, 1.0, 0.0, 0.0)
        self._stack = []
        self._op = 1.0
        self._tag = TAG_ALWAYS

    # ----- transformaciones
    @contextmanager
    def _push(self, m):
        self._stack.append(self._m)
        self._m = _mul(self._m, m)
        try:
            yield self
        finally:
            self._m = self._stack.pop()

    def translate(self, dx, dy):
        return self._push((1, 0, 0, 1, dx, dy))

    def rotate(self, deg, cx=0.0, cy=0.0):
        a = math.radians(deg)
        c, s = math.cos(a), math.sin(a)
        m = (c, s, -s, c, cx - c * cx + s * cy, cy - s * cx - c * cy)
        return self._push(m)

    def scale(self, sx, sy=None, cx=0.0, cy=0.0):
        if sy is None:
            sy = sx
        return self._push((sx, 0, 0, sy, cx - sx * cx, cy - sy * cy))

    def flip_x(self, cx=50.0):
        """Espejo horizontal respecto a la recta x = cx."""
        return self._push((-1, 0, 0, 1, 2 * cx, 0))

    @contextmanager
    def opacity(self, o):
        old = self._op
        self._op = old * o
        try:
            yield self
        finally:
            self._op = old

    @contextmanager
    def tag(self, t):
        old = self._tag
        self._tag = t
        try:
            yield self
        finally:
            self._tag = old

    # ----- pinturas
    def _paint(self, p):
        """Convierte una pintura local en pintura global (aplica la matriz a los degradados)."""
        if p is None:
            return None
        if isinstance(p, dict):
            m = self._m
            if p["t"] == "l":
                x1, y1, x2, y2 = p["p"]
                a = _apply(m, x1, y1)
                b = _apply(m, x2, y2)
                return {"t": "l", "p": [_num(a[0]), _num(a[1]), _num(b[0]), _num(b[1])], "st": p["st"]}
            cx, cy, r = p["p"]
            c = _apply(m, cx, cy)
            det = abs(m[0] * m[3] - m[1] * m[2])
            return {"t": "r", "p": [_num(c[0]), _num(c[1]), _num(r * math.sqrt(det))], "st": p["st"]}
        return p

    # ----- emision
    def _emit(self, cmds, fill, stroke, sw, cap, join, op, evenodd):
        if fill is None and stroke is None:
            return None
        m = self._m
        flat = []
        for c in cmds:
            k = c[0]
            if k == "M":
                x, y = _apply(m, c[1], c[2]); flat += [0, _num(x), _num(y)]
            elif k == "L":
                x, y = _apply(m, c[1], c[2]); flat += [1, _num(x), _num(y)]
            elif k == "C":
                x1, y1 = _apply(m, c[1], c[2])
                x2, y2 = _apply(m, c[3], c[4])
                x, y = _apply(m, c[5], c[6])
                flat += [2, _num(x1), _num(y1), _num(x2), _num(y2), _num(x), _num(y)]
            else:
                flat.append(3)
        shape = {"p": flat}
        if fill is not None:
            shape["f"] = self._paint(fill)
        if stroke is not None and sw and sw > 0:
            shape["k"] = self._paint(stroke)
            scale = math.sqrt(abs(m[0] * m[3] - m[1] * m[2]))
            shape["w"] = _num(sw * scale)
            if cap:
                shape["c"] = cap
            if join:
                shape["j"] = join
        if "f" not in shape and "k" not in shape:
            return None
        total_op = self._op * (1.0 if op is None else op)
        if total_op < 0.999:
            shape["o"] = round(total_op, 3)
        if evenodd:
            shape["e"] = 1
        if self._tag:
            shape["g"] = self._tag
        self.shapes.append(shape)
        return shape

    def _go(self, cmds, fill, stroke, sw, cap, join, op, evenodd):
        return self._emit(cmds, fill, stroke, sw, cap, join, op, evenodd)

    # ----- formas
    def path(self, p, fill=None, stroke=None, sw=0.0, cap=0, join=0, op=None, evenodd=False):
        """p es un Path, una cadena de datos SVG ('M 10 10 L ...') o una lista de comandos."""
        if isinstance(p, str):
            cmds = parse_path_d(p)
        elif isinstance(p, Path):
            cmds = p.cmds
        else:
            cmds = p
        return self._go(cmds, fill, stroke, sw, cap, join, op, evenodd)

    def circle(self, cx, cy, r, fill=None, stroke=None, sw=0.0, op=None, join=0, cap=0):
        return self._go(_ellipse_cmds(cx, cy, r, r), fill, stroke, sw, cap or 0, join or 0, op, False)

    def ellipse(self, cx, cy, rx, ry, fill=None, stroke=None, sw=0.0, op=None, join=0, cap=0):
        return self._go(_ellipse_cmds(cx, cy, rx, ry), fill, stroke, sw, cap or 0, join or 0, op, False)

    def oval(self, x, y, w, h, fill=None, stroke=None, sw=0.0, op=None, join=0, cap=0):
        """Ovalo con la esquina de arriba a la izquierda y el tamano (como drawOval de Compose)."""
        return self._go(_ellipse_cmds(x + w / 2.0, y + h / 2.0, w / 2.0, h / 2.0), fill, stroke, sw, cap or 0, join or 0, op, False)

    def rect(self, x, y, w, h, r=0.0, fill=None, stroke=None, sw=0.0, op=None, join=None, cap=0):
        return self._go(_rrect_cmds(x, y, w, h, r), fill, stroke, sw, cap or 0, (JOIN_ROUND if r else 0) if join is None else join, op, False)

    def poly(self, pts, fill=None, stroke=None, sw=0.0, closed=True, cap=0, join=0, op=None):
        cmds = [("M", pts[0][0], pts[0][1])] + [("L", x, y) for (x, y) in pts[1:]]
        if closed:
            cmds.append(("Z",))
        return self._go(cmds, fill, stroke, sw, cap, join, op, False)

    def tri(self, a, b, c, fill=None, stroke=None, sw=0.0, join=0, op=None):
        return self.poly([a, b, c], fill, stroke, sw, True, 0, join, op)

    def line(self, x1, y1, x2, y2, stroke, sw, cap=CAP_ROUND, op=None):
        return self._go([("M", x1, y1), ("L", x2, y2)], None, stroke, sw, cap, JOIN_ROUND, op, False)

    def curve(self, pts, stroke, sw, cap=CAP_ROUND, join=JOIN_ROUND, op=None):
        """Linea suave que pasa por los puntos (Catmull-Rom convertido a curvas)."""
        return self._go(catmull(pts), None, stroke, sw, cap, join, op, False)

    def blob(self, pts, fill=None, stroke=None, sw=0.0, op=None):
        """Forma cerrada y suave que pasa por los puntos."""
        return self._go(catmull(pts, closed=True), fill, stroke, sw, 0, JOIN_ROUND, op, False)

    def star(self, cx, cy, r, points=5, inner=0.45, rot=-90.0, fill=None, stroke=None, sw=0.0, op=None, round_join=True):
        pts = []
        for i in range(points * 2):
            rr = r if i % 2 == 0 else r * inner
            a = math.radians(rot + i * 180.0 / points)
            pts.append((cx + rr * math.cos(a), cy + rr * math.sin(a)))
        return self.poly(pts, fill, stroke, sw, True, 0, JOIN_ROUND if round_join else 0, op)

    def heart(self, cx, cy, r, fill=None, stroke=None, sw=0.0, op=None):
        p = Path()
        p.M(cx, cy + r * 0.95)
        p.C(cx - r * 1.35, cy + r * 0.05, cx - r * 1.0, cy - r * 1.05, cx, cy - r * 0.35)
        p.C(cx + r * 1.0, cy - r * 1.05, cx + r * 1.35, cy + r * 0.05, cx, cy + r * 0.95)
        p.Z()
        return self._go(p.cmds, fill, stroke, sw, 0, JOIN_ROUND, op, False)

    def sparkle(self, cx, cy, r, fill=None, op=None, squeeze=0.18):
        """Estrella de cuatro puntas con lados curvos."""
        p = Path()
        k = r * squeeze
        p.M(cx, cy - r)
        p.Q(cx + k, cy - k, cx + r, cy)
        p.Q(cx + k, cy + k, cx, cy + r)
        p.Q(cx - k, cy + k, cx - r, cy)
        p.Q(cx - k, cy - k, cx, cy - r)
        p.Z()
        return self._go(p.cmds, fill, None, 0, 0, 0, op, False)

    def arc_stroke(self, cx, cy, r, start_deg, end_deg, stroke, sw, cap=CAP_ROUND, op=None):
        """Arco de circunferencia (grados, 0 = derecha, positivo = hacia abajo)."""
        a0, a1 = math.radians(start_deg), math.radians(end_deg)
        x0, y0 = cx + r * math.cos(a0), cy + r * math.sin(a0)
        x1, y1 = cx + r * math.cos(a1), cy + r * math.sin(a1)
        large = 1 if abs(end_deg - start_deg) > 180 else 0
        sweep = 1 if end_deg > start_deg else 0
        p = Path().M(x0, y0).A(r, r, 0, large, sweep, x1, y1)
        return self._go(p.cmds, None, stroke, sw, cap, JOIN_ROUND, op, False)

    # ----- salida
    def bake(self):
        return {"w": _num(self.w), "h": _num(self.h), "pal": dict(self.pal), "s": list(self.shapes)}


def catmull(pts, closed=False, tension=0.5):
    """Comandos de curva suave (Catmull-Rom) que pasan por los puntos."""
    n = len(pts)
    cmds = [("M", pts[0][0], pts[0][1])]
    if n == 1:
        return cmds
    rng = range(n) if closed else range(n - 1)
    for i in rng:
        p0 = pts[(i - 1) % n] if (closed or i > 0) else pts[i]
        p1 = pts[i]
        p2 = pts[(i + 1) % n]
        p3 = pts[(i + 2) % n] if (closed or i + 2 < n) else pts[(i + 1) % n]
        t = tension / 3.0 * 2.0
        c1 = (p1[0] + (p2[0] - p0[0]) * t / 2.0 * 1.0, p1[1] + (p2[1] - p0[1]) * t / 2.0 * 1.0)
        c2 = (p2[0] - (p3[0] - p1[0]) * t / 2.0 * 1.0, p2[1] - (p3[1] - p1[1]) * t / 2.0 * 1.0)
        cmds.append(("C", c1[0], c1[1], c2[0], c2[1], p2[0], p2[1]))
    if closed:
        cmds.append(("Z",))
    return cmds


# ------------------------------------------------------------------ empaquetado

def pack(icons, path):
    """Guarda {id: dibujo cocido} como JSON compacto."""
    with open(path, "w", encoding="utf-8") as fh:
        json.dump(icons, fh, ensure_ascii=False, separators=(",", ":"), sort_keys=True)


def unpack(path):
    with open(path, "r", encoding="utf-8") as fh:
        return json.load(fh)
