"""Genera el ZIP para "Importar logros" de Play Console a partir de AchievementCatalog.kt (82 logros, 10 puntos cada uno).
Uso: python tools/pgs/make_achievements_zip.py [carpeta_salida]"""
import csv, io, math, os, re, sys, zipfile
from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")
SRC = os.path.join(ROOT, "shared/src/commonMain/kotlin/com/korkoor/pardos/domain/achievements/AchievementCatalog.kt")
OUT = sys.argv[1] if len(sys.argv) > 1 else os.path.join(ROOT, "tools/pgs/out")
os.makedirs(OUT, exist_ok=True)

text = io.open(SRC, encoding="utf-8").read()
rows = re.findall(r'AchievementDef\("([^"]+)",\s*"([^"]+)",\s*"([^"]+)",\s*0x([0-9A-Fa-f]{8})L', text)
assert len(rows) >= 80, len(rows)


def icon(color_hex, path):
    c = tuple(int(color_hex[i:i + 2], 16) for i in (2, 4, 6))
    S = 512
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.ellipse((8, 8, S - 8, S - 8), fill=c + (255,))
    dark = tuple(int(v * 0.72) for v in c)
    d.ellipse((8, 8, S - 8, S - 8), outline=dark + (255,), width=18)
    cx = cy = S / 2
    pts = []
    for i in range(10):
        r = S * (0.30 if i % 2 == 0 else 0.13)
        a = -math.pi / 2 + i * math.pi / 5
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a) + S * 0.02))
    d.polygon(pts, fill=(255, 255, 255, 235))
    im.quantize(colors=5, dither=Image.Dither.NONE).save(path, optimize=True)


icons_dir = os.path.join(OUT, "icons")
os.makedirs(icons_dir, exist_ok=True)
from collections import Counter
# Para que el ZIP sea pequeño, los colores se acercan a los 8 más usados (el mismo ícono sirve para varios logros)
_top = [c[2:] for c, _ in Counter(r[3] for r in rows).most_common(8)]
def _snap(c):
    v = [int(c[i:i + 2], 16) for i in (2, 4, 6)]
    return min(_top, key=lambda o: sum((v[i] - int(o[2 * i:2 * i + 2], 16)) ** 2 for i in range(3)))
rows = [(a, b, c, "FF" + _snap(d)) for (a, b, c, d) in rows]
meta, loc, maps = [], [], []
for n, (ach_id, title, desc, color) in enumerate(rows, 1):
    fn = "icon_%s.png" % color[2:].lower()
    if not os.path.exists(os.path.join(icons_dir, fn)):
        icon(color, os.path.join(icons_dir, fn))
    meta.append([title, "Revealed", "FALSE", "", "10", str(n)])
    loc.append([title, "es-419", title, desc])
    maps.append([title, fn])


def write(name, header, data):
    with io.open(os.path.join(OUT, name), "w", encoding="utf-8", newline="") as fh:
        w = csv.writer(fh)
        w.writerow(header)
        w.writerows(data)


write("AchievementsMetadata.csv", ["Name", "Initial State", "Incremental", "Steps to Unlock", "Points", "List Order"], meta)
write("AchievementsLocalizations.csv", ["Name", "Language", "Title", "Description"], loc)
write("AchievementsIconsMappings.csv", ["Name", "Icon Filename"], maps)

zp = os.path.join(OUT, "logros.zip")
with zipfile.ZipFile(zp, "w", zipfile.ZIP_DEFLATED) as z:
    for name in ("AchievementsMetadata.csv", "AchievementsLocalizations.csv", "AchievementsIconsMappings.csv"):
        z.write(os.path.join(OUT, name), name)
    for fn in os.listdir(icons_dir):
        z.write(os.path.join(icons_dir, fn), fn)
print(len(rows), "logros ->", zp)
