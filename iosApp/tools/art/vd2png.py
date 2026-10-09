"""Dibuja iconos vectoriales de Android (VectorDrawable) como PNG: python vd2png.py fondo.xml primer_plano.xml salida.png [tamano] [recorte]
Admite caminos, grupos (escala, giro, traslado), degradados lineales y radiales, trazos y colores #AARRGGBB.
recorte = lado (en unidades de 108) que se muestra desde el centro; 108 = todo, 84 = mas cerca (el icono de iOS no lleva zona segura)."""
import os
import sys
import xml.etree.ElementTree as ET

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from artlib import Scene, lin, rad, parse_path_d, CAP_BUTT, CAP_ROUND, CAP_SQUARE, JOIN_MITER, JOIN_ROUND, JOIN_BEVEL  # noqa: E402
import render  # noqa: E402

A = "{http://schemas.android.com/apk/res/android}"
APT = "{http://schemas.android.com/aapt}"


def color(c):
    c = c.strip().lstrip("#")
    if len(c) == 8:
        return "#" + c[2:] + c[:2]
    return "#" + c


def paint(el, attr):
    """Color o degradado de un atributo (directo o dentro de aapt:attr)."""
    v = el.get(A + attr)
    if v is not None:
        return color(v)
    for sub in el.findall(APT + "attr"):
        if sub.get("name") == "android:" + attr:
            g = sub.find("gradient")
            stops = [(float(i.get(A + "offset")), color(i.get(A + "color"))) for i in g.findall("item")]
            if g.get(A + "type") == "radial":
                return rad(float(g.get(A + "centerX")), float(g.get(A + "centerY")), float(g.get(A + "gradientRadius")), stops)
            return lin(float(g.get(A + "startX")), float(g.get(A + "startY")), float(g.get(A + "endX")), float(g.get(A + "endY")), stops)
    return None


def draw(el, s):
    for ch in el:
        tag = ch.tag
        if tag == "group":
            sx = float(ch.get(A + "scaleX", 1))
            sy = float(ch.get(A + "scaleY", 1))
            px = float(ch.get(A + "pivotX", 0))
            py = float(ch.get(A + "pivotY", 0))
            tx = float(ch.get(A + "translateX", 0))
            ty = float(ch.get(A + "translateY", 0))
            rot = float(ch.get(A + "rotation", 0))
            with s.translate(tx + px, ty + py):
                with s.rotate(rot, 0, 0):
                    with s.scale(sx, sy, 0, 0):
                        with s.translate(-px, -py):
                            draw(ch, s)
        elif tag == "path":
            d = ch.get(A + "pathData")
            fill = paint(ch, "fillColor")
            stroke = paint(ch, "strokeColor")
            sw = float(ch.get(A + "strokeWidth", 0))
            fa = float(ch.get(A + "fillAlpha", 1))
            sa = float(ch.get(A + "strokeAlpha", 1))
            join = {"round": JOIN_ROUND, "bevel": JOIN_BEVEL}.get(ch.get(A + "strokeLineJoin", "miter"), JOIN_MITER)
            cap = {"round": CAP_ROUND, "square": CAP_SQUARE}.get(ch.get(A + "strokeLineCap", "butt"), CAP_BUTT)
            even = ch.get(A + "fillType", "") == "evenOdd"
            if isinstance(fill, str) and fill.lower() in ("#00000000",):
                fill = None
            if fill is not None:
                s.path(d, fill=fill, evenodd=even, op=fa if fa < 1 else None)
            if stroke is not None and sw > 0:
                s.path(d, stroke=stroke, sw=sw, cap=cap, join=join, op=sa if sa < 1 else None)


def main():
    bg, fg, out = sys.argv[1], sys.argv[2], sys.argv[3]
    size = int(sys.argv[4]) if len(sys.argv) > 4 else 1024
    crop = float(sys.argv[5]) if len(sys.argv) > 5 else 108.0
    s = Scene(108, 108)
    for path in (bg, fg):
        root = ET.parse(path).getroot()
        draw(root, s)
    icon = s.bake()
    m = (108.0 - crop) / 2.0
    # recorte: se mueve y se escala todo el dibujo para que el centro llene el lienzo
    k = 108.0 / crop
    for sh in icon["s"]:
        p = sh["p"]
        i = 0
        while i < len(p):
            c = p[i]
            n = {0: 2, 1: 2, 2: 6, 3: 0}[c]
            for j in range(n):
                p[i + 1 + j] = (p[i + 1 + j] - m) * k
            i += 1 + n
        for key in ("f", "k"):
            pt = sh.get(key)
            if isinstance(pt, dict):
                q = pt["p"]
                if pt["t"] == "l":
                    pt["p"] = [(q[0] - m) * k, (q[1] - m) * k, (q[2] - m) * k, (q[3] - m) * k]
                else:
                    pt["p"] = [(q[0] - m) * k, (q[1] - m) * k, q[2] * k]
        if "w" in sh:
            sh["w"] = sh["w"] * k
    render.SS = 2
    img = render.render_icon(icon, size=size, bg="#FFFFFFFF").convert("RGB")
    img.save(out)
    print("listo", out, img.size)


if __name__ == "__main__":
    main()
