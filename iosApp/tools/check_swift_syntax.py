"""Revisa la sintaxis de los .swift sin necesitar un Mac (no comprueba tipos, pero atrapa llaves, comas y palabras mal escritas).

Uso:  pip install tree-sitter tree-sitter-swift   y luego   python iosApp/tools/check_swift_syntax.py
"""
import glob
import os
import sys

import tree_sitter_swift as tss
from tree_sitter import Language, Parser

parser = Parser(Language(tss.language()))
base = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'ParDos')
bad = 0
for path in sorted(glob.glob(os.path.join(base, '*.swift'))):
    src = open(path, 'rb').read()
    errors = []

    def walk(node):
        if node.type == 'ERROR' or node.is_missing:
            errors.append((node.start_point[0] + 1, node.start_point[1] + 1, node.type))
        for child in node.children:
            walk(child)

    walk(parser.parse(src).root_node)
    name = os.path.basename(path)
    if errors:
        bad += 1
        print(f'{name}: ERRORES en (línea, columna): {errors[:8]}')
    else:
        print(f'{name}: ok')
sys.exit(1 if bad else 0)
