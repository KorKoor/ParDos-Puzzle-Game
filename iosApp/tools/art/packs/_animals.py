"""Cabezas de animal para las piezas del album: se sacan de los avatares ya dibujados (sin hombros ni ojos cerrados)."""
import glob
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import _looks  # noqa: E402

ART = os.path.normpath(os.path.join(HERE, "..", "..", "..", "ParDos", "Art"))
_cache = {}


def _load():
    if _cache:
        return
    for f in glob.glob(os.path.join(ART, "art_avatars_*.json")):
        with open(f, encoding="utf-8") as fh:
            d = json.load(fh)
        for k, v in d.items():
            if k.startswith("animal."):
                _cache[k[7:]] = v


def _xf(icon, sx, tx, ty):
    out = {"w": icon["w"], "h": icon["h"], "pal": icon["pal"], "s": []}
    for sh in icon["s"]:
        n = dict(sh)
        p = list(sh["p"])
        i = 0
        while i < len(p):
            c = p[i]
            cnt = {0: 2, 1: 2, 2: 6, 3: 0}[c]
            for j in range(cnt):
                p[i + 1 + j] = round(p[i + 1 + j] * sx + (tx if j % 2 == 0 else ty), 2)
            i += 1 + cnt
        n["p"] = p
        for key in ("f", "k"):
            pt = n.get(key)
            if isinstance(pt, dict):
                q = dict(pt)
                pp = list(pt["p"])
                if pt["t"] == "l":
                    pp = [pp[0] * sx + tx, pp[1] * sx + ty, pp[2] * sx + tx, pp[3] * sx + ty]
                else:
                    pp = [pp[0] * sx + tx, pp[1] * sx + ty, pp[2] * sx]
                q["p"] = [round(v, 2) for v in pp]
                n[key] = q
        if "w" in n:
            n["w"] = round(n["w"] * sx, 2)
        out["s"].append(n)
    return out


def head(animal, variant="NORMAL", scale=1.0, dy=2, extra=None):
    """Dibujo de la cabeza del animal (id de avatar en mayusculas) con la paleta de esa variante."""
    _load()
    base = _cache[animal]
    shapes = []
    for sh in base["s"]:
        if sh.get("g") == 2:
            continue
        if "$shirt" in json.dumps(sh):
            continue
        n = dict(sh)
        n.pop("g", None)
        shapes.append(n)
    icon = {"w": base["w"], "h": base["h"], "pal": _looks.palette(animal, variant), "s": shapes}
    icon = _xf(icon, scale, 50 - 50 * scale, 50 - 50 * scale + dy)
    if extra:
        icon["pal"].update(extra)
    return icon
