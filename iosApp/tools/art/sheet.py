"""Hoja de vista previa de un paquete ya construido: python sheet.py <paquete> [columnas] [tamano]"""
import json, os, sys, re
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from render import contact_sheet
name = sys.argv[1]
cols = int(sys.argv[2]) if len(sys.argv) > 2 else 10
cell = int(sys.argv[3]) if len(sys.argv) > 3 else 96
d = json.load(open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "ParDos", "Art", "art_%s.json" % name), encoding="utf-8"))
def key(k):
    m = re.match(r"(.*?)(\d+)$", k)
    return (m.group(1), int(m.group(2))) if m else (k, 0)
items = [(k, d[k]) for k in sorted((k for k in d if not k.startswith("@")), key=key)]
out = os.path.join(os.path.dirname(os.path.abspath(__file__)), "_preview", name + "_sheet.png")
os.makedirs(os.path.dirname(out), exist_ok=True)
contact_sheet(items, out, cols=cols, cell=cell, label=False)
print(out, len(items))
