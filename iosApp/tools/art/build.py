"""
Construye los dibujos de ParDos para iPhone.

    python build.py                 # todos los paquetes de packs/*.py
    python build.py avatars map     # solo esos paquetes
    python build.py --preview       # ademas guarda hojas PNG en _preview/ para revisarlas con los ojos

Cada paquete (packs/<nombre>.py) define:
    def build():
        return {"icons": {"id": escena.bake(), ...}, "palettes": {"nombre": {"ranura": "#RRGGBB", ...}}}   # palettes es opcional
y genera ../../ParDos/Art/art_<nombre>.json (el que lee la app) tras validarlo.
"""
import importlib.util
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from artlib import resolve_color  # noqa: E402

OUT = os.path.normpath(os.path.join(HERE, "..", "..", "ParDos", "Art"))
PREVIEW = os.path.join(HERE, "_preview")
MAX_FILE_BYTES = 6 * 1024 * 1024


def _check_paint(paint, pal, where, problems):
    try:
        if isinstance(paint, dict):
            if paint.get("t") not in ("l", "r"):
                problems.append("%s: degradado desconocido" % where)
                return
            if not paint.get("st"):
                problems.append("%s: degradado sin paradas" % where)
                return
            for off, col in paint["st"]:
                resolve_color(col, pal)
        else:
            resolve_color(paint, pal)
    except Exception as exc:  # noqa: BLE001
        problems.append("%s: %s" % (where, exc))


def validate_icon(name, icon, extra_pals=None):
    problems = []
    if not isinstance(icon, dict) or "s" not in icon or "w" not in icon or "h" not in icon:
        return ["%s: formato incorrecto" % name]
    if not icon["s"]:
        problems.append("%s: no tiene formas" % name)
    pal = dict(icon.get("pal", {}))
    if extra_pals:
        for p in extra_pals:
            pal.update(p)
    for i, sh in enumerate(icon["s"]):
        where = "%s[forma %d]" % (name, i)
        flat = sh.get("p")
        if not flat or flat[0] != 0:
            problems.append("%s: el camino debe empezar con mover" % where)
            continue
        k = 0
        while k < len(flat):
            code = flat[k]
            k += {0: 3, 1: 3, 2: 7, 3: 1}.get(code, 10 ** 9)
        if k != len(flat):
            problems.append("%s: camino mal formado" % where)
        nums = [v for v in flat]
        if any(abs(v) > 5000 for v in nums):
            problems.append("%s: coordenadas fuera de rango" % where)
        if "f" not in sh and "k" not in sh:
            problems.append("%s: sin relleno ni trazo" % where)
        if "f" in sh:
            _check_paint(sh["f"], pal, where + ".f", problems)
        if "k" in sh:
            _check_paint(sh["k"], pal, where + ".k", problems)
            if sh.get("w", 0) <= 0:
                problems.append("%s: trazo sin grosor" % where)
    return problems


def load_pack(name):
    path = os.path.join(HERE, "packs", name + ".py")
    spec = importlib.util.spec_from_file_location("pack_" + name, path)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


def build_pack(name, preview=False):
    mod = load_pack(name)
    data = mod.build()
    icons = data["icons"]
    palettes = data.get("palettes", {})
    problems = []
    pal_dicts = list(palettes.values())
    for icon_id, icon in icons.items():
        # una ranura puede venir de la paleta del dibujo o de cualquier paleta del paquete: se comprueba contra la propia primero
        issues = validate_icon(icon_id, icon)
        if issues and pal_dicts:
            issues = validate_icon(icon_id, icon, [pal_dicts[0]])
        problems.extend(issues)
    for pname, p in palettes.items():
        for slot_name, col in p.items():
            try:
                resolve_color(col, p)
            except Exception as exc:  # noqa: BLE001
                problems.append("paleta %s.%s: %s" % (pname, slot_name, exc))
    if problems:
        for p in problems[:40]:
            print("  ERROR", p)
        raise SystemExit("%s: %d problemas" % (name, len(problems)))
    out = {}
    out.update(icons)
    for pname, p in palettes.items():
        out["@pal:" + pname] = p
    os.makedirs(OUT, exist_ok=True)
    target = os.path.join(OUT, "art_%s.json" % name)
    with open(target, "w", encoding="utf-8") as fh:
        json.dump(out, fh, ensure_ascii=False, separators=(",", ":"), sort_keys=True)
    size = os.path.getsize(target)
    if size > MAX_FILE_BYTES:
        raise SystemExit("%s: el archivo pesa %d bytes (limite %d)" % (name, size, MAX_FILE_BYTES))
    print("%-18s %4d dibujos %3d paletas %8.1f KB" % (name, len(icons), len(palettes), size / 1024.0))
    if preview:
        from render import contact_sheet
        os.makedirs(PREVIEW, exist_ok=True)
        items = sorted(icons.items())
        per = 48
        for page in range(0, len(items), per):
            chunk = items[page:page + per]
            path = os.path.join(PREVIEW, "%s_%02d.png" % (name, page // per + 1))
            contact_sheet(chunk, path, cols=8, cell=128)
    return len(icons)


def main(argv):
    preview = "--preview" in argv
    names = [a for a in argv if not a.startswith("--")]
    packs_dir = os.path.join(HERE, "packs")
    if not names:
        names = sorted(f[:-3] for f in os.listdir(packs_dir) if f.endswith(".py") and not f.startswith("_"))
    total = 0
    for n in names:
        total += build_pack(n, preview)
    print("total:", total, "dibujos")
    # ids repetidos entre paquetes
    seen = {}
    for f in sorted(os.listdir(OUT)):
        if not (f.startswith("art_") and f.endswith(".json")):
            continue
        try:
            with open(os.path.join(OUT, f), encoding="utf-8") as fh:
                keys = list(json.load(fh))
        except Exception:  # otro agente puede estar escribiendo ese archivo ahora mismo
            continue
        for key in keys:
            if key in seen and seen[key] != f:
                raise SystemExit("id repetido %s en %s y %s" % (key, seen[key], f))
            seen[key] = f


if __name__ == "__main__":
    main(sys.argv[1:])
