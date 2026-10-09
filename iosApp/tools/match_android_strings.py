"""Cruza los textos de la app de iPhone con los de Android (res/values y res/values-en) y muestra los que ya tienen traduccion.
Uso: python match_android_strings.py strings_es.json salida_coincidencias.json"""
import io
import json
import os
import re
import sys
import xml.etree.ElementTree as ET

RES = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "app", "src", "main", "res"))


def load(path):
    out = {}
    for el in ET.parse(path).getroot().findall("string"):
        text = "".join(el.itertext())
        text = text.replace("\\'", "'").replace("\\n", "\n").replace('\\"', '"')
        out[el.get("name")] = text
    return out


def main():
    es = load(os.path.join(RES, "values", "strings.xml"))
    en = load(os.path.join(RES, "values-en", "strings.xml"))
    by_text = {}
    for key, text in es.items():
        if key in en:
            by_text[text.strip()] = en[key]
    items = json.load(io.open(sys.argv[1], encoding="utf-8"))
    matched = {}
    missing = []
    for it in items:
        s = it["es"]
        if s.strip() in by_text:
            matched[s] = by_text[s.strip()]
        else:
            missing.append(s)
    io.open(sys.argv[2], "w", encoding="utf-8").write(json.dumps({"matched": matched, "missing": missing}, ensure_ascii=False, indent=0))
    print(len(matched), "con traduccion de Android;", len(missing), "faltan")


if __name__ == "__main__":
    main()
