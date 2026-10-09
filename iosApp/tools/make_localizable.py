# -*- coding: utf-8 -*-
"""Genera iosApp/ParDos/en.lproj/Localizable.strings a partir de translations_en.py.
Los textos con variables (\\(x)) se escriben en todas las combinaciones de %lld (numero) y %@ (texto), porque desde el codigo no se sabe
de que tipo es cada variable. Uso: python make_localizable.py"""
import io
import itertools
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from translations_en import T  # noqa: E402

OUT_DIR = os.path.normpath(os.path.join(HERE, "..", "ParDos", "en.lproj"))


def split_slots(text):
    """Parte un literal Swift en trozos de texto y variables: [('t', 'Nivel '), ('v', 'card.id'), ...]."""
    parts = []
    buf = []
    i = 0
    while i < len(text):
        if text.startswith("\\(", i):
            if buf:
                parts.append(("t", "".join(buf)))
                buf = []
            depth = 1
            j = i + 2
            while j < len(text) and depth:
                if text[j] == "(":
                    depth += 1
                elif text[j] == ")":
                    depth -= 1
                j += 1
            parts.append(("v", text[i + 2:j - 1]))
            i = j
        else:
            buf.append(text[i])
            i += 1
    if buf:
        parts.append(("t", "".join(buf)))
    return parts


def unescape(s):
    return s.replace("\\n", "\n").replace('\\"', '"').replace("\\\\", "\\")


def esc(s):
    return s.replace("\\", "\\\\").replace('"', '\\"').replace("\n", "\\n")


def main():
    lines = ["/* Generado por iosApp/tools/make_localizable.py: no editar a mano. */", ""]
    seen = set()
    count = 0
    for es, en in sorted(T.items()):
        parts = split_slots(es)
        nslots = sum(1 for k, _ in parts if k == "v")
        combos = list(itertools.product(("%lld", "%@"), repeat=nslots)) if nslots else [()]
        for combo in combos:
            it = iter(combo)
            key = ""
            for kind, value in parts:
                key += unescape(value).replace("%", "%%") if kind == "t" else next(it)
            val = en.replace("%", "%%")
            for n, spec in enumerate(combo):
                val = val.replace("{%d}" % n, "%%%d$%s" % (n + 1, spec[1:]))
            if key in seen:
                continue
            seen.add(key)
            lines.append('"%s" = "%s";' % (esc(key), esc(val)))
            count += 1
    os.makedirs(OUT_DIR, exist_ok=True)
    with io.open(os.path.join(OUT_DIR, "Localizable.strings"), "w", encoding="utf-8") as fh:
        fh.write("\n".join(lines) + "\n")
    print(count, "entradas para", len(T), "textos")


if __name__ == "__main__":
    main()
