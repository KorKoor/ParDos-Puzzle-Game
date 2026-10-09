"""Vista previa de banners con sus colores reales: python bannerprev.py PATRON [PATRON ...]
Cada fila es una variante del catalogo; a la izquierda base + fx y a la derecha base + fx2 (o solo base si no hay fx2)."""
import json
import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import render  # noqa: E402

V = {
    "DIAMONDS": [("FFE0F1", "D9F5EA", "FF8CC0", "8A3E6E")],
    "RAYS": [("FFF3C4", "FFD36E", "F2A93B", "6B4A0E"), ("FFC27A", "E0558F", "FFE08A", "FFF5E6")],
    "STARS": [("121B3A", "2D2A6E", "FFE08A", "F1F0FF")],
    "AURORA": [("081B2E", "14465A", "78E6C6", "D9FFF2"), ("101A33", "3A4C7A", "BBCBF2", "F3F6FF")],
    "SKYLINE": [("1B1235", "4A1F6B", "FF5FA8", "FFE6F5")],
    "EMBERS": [("2A0F0C", "6B1F14", "FF7B3A", "FFE1C6")],
    "BATS": [("1B0F2E", "4A1F6B", "FF9A2E", "FFEBD0"), ("0F0A22", "3A1A5E", "C59BFF", "F1E9FF")],
    "PUMPKINS": [("2B1A3D", "7A2D4F", "FF8A1F", "FFEBD0"), ("2A1636", "B8561B", "FFD36E", "FFF1D6")],
    "MOUNTAINS": [("CFE0F5", "F1E4D3", "6B83A8", "22324F"), ("2A0F0C", "7A2314", "FF7B3A", "FFE1C6"), ("E3F1FF", "F8FBFF", "7FA3CF", "1E3A5F")],
    "FOREST": [("BFE6C9", "2F7A4A", "3E8A5A", "F0FFF3"), ("0B1F1A", "16423A", "D6F26B", "E3F7E8")],
    "CLOUDS": [("BFE4FF", "E9F6FF", "FFFFFF", "1F4A6E")],
    "HEARTS": [("FFE1EA", "FFC4D6", "FF6F9B", "7A2E4B"), ("E8EEFF", "C7B8FF", "FF8FC0", "3C2F7A")],
    "FIREWORKS": [("0B1030", "1F2A5E", "FFD36E", "F1F0FF"), ("120A2A", "4A1F6B", "FF8CC0", "FFE6F5")],
    "GALAXY": [("0A0720", "3A1670", "C9A6FF", "F1E9FF"), ("1F0A2E", "6B1F5E", "FF8CC9", "FFE6F5")],
    "ZEN": [("F3ECDD", "E2D5BC", "B7A47E", "4A3F27"), ("FFE8EE", "FFC9D8", "FF8FB0", "7A3D55")],
    "STRIPES": [("FFE9F2", "FFC2DA", "FF7FB2", "7A2E55")],
    "CIRCUIT": [("081F2B", "0E4A55", "5CF2D0", "D9FFF5"), ("1A1405", "4A3A0A", "FFD36E", "FFF5D6")],
    "LANTERNS": [("2A1B3D", "5B2A4E", "FFB44A", "FFEBD0")],
    "CONFETTI": [("FFF4D6", "FFE1F0", "FF8CC0", "6B3A55")],
    "RAIN": [("D5DEE9", "AEBCCE", "5D7799", "26364E"), ("141B33", "2A3A66", "8EC9FF", "EAF3FF"), ("14102E", "2A1F5E", "FF5FA8", "FFE6F5")],
    "SUNSET": [("F2985A", "B83A7A", "FFE08A", "FFF5E6"), ("FFD27A", "E8743A", "FFF1B5", "4A210E")],
    "CRYSTALS": [("1A1440", "3C2A8F", "8EE3FF", "EAF6FF"), ("12204A", "2D5AA8", "8EE3FF", "FFFFFF")],
}


def hx(c):
    return tuple(int(c[i:i + 2], 16) for i in (0, 2, 4))


def compose(art, name, variant, layers, w=450):
    top, bottom, accent, ink = variant
    pal = {"top": "#" + top, "bottom": "#" + bottom, "accent": "#" + accent, "ink": "#" + ink}
    h = w // 3
    img = Image.new("RGBA", (w, h))
    dr = ImageDraw.Draw(img)
    t, b = hx(top), hx(bottom)
    for y in range(h):
        k = y / float(h - 1)
        dr.line([(0, y), (w, y)], fill=tuple(int(t[i] + (b[i] - t[i]) * k) for i in range(3)) + (255,))
    for key in layers:
        if key in art:
            layer = render.render_icon(art[key], size=w, palette=pal)
            img.alpha_composite(layer.resize((w, h)))
    return img


def main():
    art = json.load(open(os.path.join(HERE, "..", "..", "ParDos", "Art", "art_banners.json"), encoding="utf-8"))
    rows = []
    for name in sys.argv[1:]:
        for v in V[name.upper()]:
            n = "banner." + name.upper()
            left = compose(art, name, v, [n, n + ".fx"])
            right = compose(art, name, v, [n, n + ".fx2"] if (n + ".fx2") in art else [n])
            rows.append((left, right))
    if not rows:
        return
    w, h = rows[0][0].size
    sheet = Image.new("RGBA", (w * 2 + 12, (h + 6) * len(rows)), (40, 40, 40, 255))
    for i, (a, b) in enumerate(rows):
        sheet.paste(a, (0, i * (h + 6)))
        sheet.paste(b, (w + 12, i * (h + 6)))
    out = os.path.join(HERE, "_preview", "banners_prev.png")
    os.makedirs(os.path.dirname(out), exist_ok=True)
    sheet.convert("RGB").save(out)
    print(out, sheet.size)


if __name__ == "__main__":
    main()
