"""Saca de los archivos Swift los textos de pantalla (en espanol) para traducirlos. Uso: python extract_strings.py [salida.json]"""
import glob
import io
import json
import os
import re
import sys

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "ParDos"))
LIT = r'"((?:[^"\\]|\\.)*)"'
PATTERNS = [
    re.compile(r'\b(?:Text|Button|Label|Toggle|Alert|PopupFrame|SectionTitle)\(\s*(?:title:\s*)?' + LIT),
    re.compile(r'\b(?:title|message|subtitle|label|detail|text|name)\s*:\s*' + LIT),
    re.compile(r'\bBigButton\(title:\s*' + LIT),
    re.compile(r'\.(?:default|cancel|destructive)\(Text\(' + LIT),
]
HAS_WORDS = re.compile(r'[A-Za-záéíóúñÁÉÍÓÚÑ]{3}')


def main():
    found = {}
    for path in sorted(glob.glob(os.path.join(ROOT, "*.swift"))):
        name = os.path.basename(path)
        text = io.open(path, encoding="utf-8").read()
        for pat in PATTERNS:
            for m in pat.finditer(text):
                s = m.group(1)
                if not HAS_WORDS.search(s):
                    continue
                if s.startswith(("fx.", "cozy.", "prop.", "chapter.", "animal.", "acc.", "scene.", "banner.", "chest.", "rank.", "power.", "sfx_", "mus_", "ico_")):
                    continue
                if "." in s and " " not in s and s.islower():
                    continue
                found.setdefault(s, set()).add(name)
    out = [{"es": k, "files": sorted(v), "interp": "\\(" in k} for k, v in sorted(found.items())]
    dest = sys.argv[1] if len(sys.argv) > 1 else None
    if dest:
        with io.open(dest, "w", encoding="utf-8") as fh:
            json.dump(out, fh, ensure_ascii=False, indent=0)
    print(len(out), "textos,", sum(1 for o in out if o["interp"]), "con variables,", sum(len(o["es"]) for o in out), "caracteres")


if __name__ == "__main__":
    main()
