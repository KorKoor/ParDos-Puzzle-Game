# Guía de arte vectorial de ParDos (iPhone)

La app de iPhone **no usa emojis como arte**. Todo dibujo (avatares, piezas del álbum, mapa, adornos, partículas) se dibuja
en Python con `artlib.py`, se mira como PNG con `render.py` y se guarda como JSON en `iosApp/ParDos/Art/art_<paquete>.json`.
`iosApp/ParDos/Art.swift` lee ese JSON y lo pinta con `Canvas`. **Lo que ves en el PNG es lo que sale en el iPhone.**

## Cómo se trabaja

1. Crea o edita **solo tu paquete**: `iosApp/tools/art/packs/<paquete>.py` (no toques `artlib.py`, `render.py`, `build.py` ni paquetes ajenos).
2. Un paquete define `def build(): return {"icons": {id: escena.bake(), ...}, "palettes": {nombre: {ranura: "#RRGGBB"}}}` (`palettes` es opcional).
3. Compila y genera hojas de vista previa:
   `cd C:/Users/carlo/Documents/Android/ParDos-Puzzle-Game/iosApp/tools/art && python build.py <paquete> --preview`
   Las hojas quedan en `_preview/<paquete>_NN.png` (48 dibujos por hoja, 8 por fila). **Ábrelas con la herramienta Read y mira de verdad cada dibujo.**
4. Itera: reconocible a 128 px **y** a 48 px (así se ven en listas), sin formas rotas, sin partes cortadas, colores armoniosos. Mínimo dos rondas de revisión visual.
5. Para ver un dibujo grande: `python -c "import sys; sys.path.insert(0,'.'); ..."` o usa `render.contact_sheet([(nombre, dibujo)], 'ruta.png', cols=1, cell=512)`.
6. Escribe archivos con la herramienta Write (no con heredocs de bash: se rompen las barras y las comillas triples). Python 3 en Windows, comando `python`.
7. Prohibido: emojis o letras/texto dibujado como arte, imágenes externas, descargar nada, tocar archivos fuera de tu paquete, inventar ids fuera de tu lista.

Plantilla de paquete:

```python
import os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, ".."))   # artlib
sys.path.insert(0, HERE)                        # _looks, _kit
from artlib import *

def mi_dibujo():
    s = Scene(100, 100, pal={"main": "#E0A93B"})
    s.ellipse(50, 92, 28, 5, fill="#00000022")                 # sombra de contacto
    s.circle(50, 50, 34, fill=rad(40, 38, 50, [(0, mix("$main", "#FFFFFF", .45)), (1, "$main")]))
    return s.bake()

def build():
    return {"icons": {"piece.ejemplo_1": mi_dibujo()}}
```

## API de `artlib` (todo se importa con `from artlib import *`)

Lienzo `Scene(w=100, h=100, pal={})`: guarda las formas en el orden en que las dibujas (lo último queda encima).

| Llamada | Qué hace |
|---|---|
| `s.circle(cx, cy, r, fill=, stroke=, sw=, op=)` | círculo |
| `s.ellipse(cx, cy, rx, ry, ...)` / `s.oval(x, y, w, h, ...)` | elipse por centro y radios / por esquina y tamaño (como `drawOval` de Compose) |
| `s.rect(x, y, w, h, r=0, ...)` | rectángulo con esquinas redondeadas |
| `s.poly([(x,y),...], fill=, stroke=, sw=, closed=True, join=)` / `s.tri(a, b, c, ...)` | polígono / triángulo |
| `s.path("M 10 10 C ... Z", fill=, stroke=, sw=, cap=, join=, evenodd=)` | datos de camino SVG (M L H V C S Q T A Z, mayúsculas y minúsculas) o un `Path()` o lista de comandos |
| `s.line(x1,y1,x2,y2, stroke, sw)` / `s.curve([(x,y),...], stroke, sw)` / `s.blob([(x,y),...], fill=)` | línea / curva suave por puntos / forma cerrada suave por puntos |
| `s.star(cx, cy, r, points=5, inner=.45, rot=-90, ...)`, `s.heart(cx, cy, r, ...)`, `s.sparkle(cx, cy, r, fill=)` | estrella, corazón, destello de 4 puntas |
| `s.arc_stroke(cx, cy, r, grados_ini, grados_fin, stroke, sw)` | arco (0° = derecha, positivo = hacia abajo) |
| `with s.translate(dx,dy):` `with s.rotate(grados, cx, cy):` `with s.scale(sx, sy, cx, cy):` `with s.flip_x(cx):` | transformaciones (se pueden anidar) |
| `with s.opacity(0.5):` | opacidad de grupo (multiplica la de cada forma) |
| `with s.tag(TAG_EYES_OPEN):` / `TAG_EYES_CLOSED` | formas que solo se ven con los ojos abiertos / al parpadear |

Parámetros comunes: `fill` y `stroke` aceptan una *pintura*; `sw` = grosor del trazo; `cap` = `CAP_BUTT/CAP_ROUND/CAP_SQUARE`;
`join` = `JOIN_MITER/JOIN_ROUND/JOIN_BEVEL`; `op` = opacidad 0..1; `evenodd=True` para formas con agujeros (la unión por defecto no hace agujeros).

**Pinturas**: `"#RRGGBB"`, `"#RRGGBBAA"` (con alfa), `"$ranura"` (color de la paleta del dibujo o de la paleta que ponga la app),
`mix(a, b, t)` (mezcla de dos colores, t de 0 a 1), `alpha(color, a)`, y degradados: `lin(x1,y1,x2,y2, [(0, c1), (1, c2)])`,
`rad(cx, cy, r, [(0, c1), (1, c2)])`, `vgrad(arriba, abajo, y1=0, y2=100)`. Las coordenadas del degradado van en las mismas
coordenadas locales que la forma. El radial es siempre circular (no elíptico).

**Paleta/ranuras**: `Scene(pal={"head": "#F29A4A", ...})`. Un color `"$head"` se resuelve en la app con la paleta que le toque
(avatares: variantes de color). Úsalas solo donde el color deba poder cambiar; el resto, colores fijos.

## Convención de ids (contrato con Swift: no cambiarla)

| Id | Qué | Paquete |
|---|---|---|
| `animal.<ANIMAL>` | avatar sin fondo: orejas/partes traseras, hombros (ranura `shirt`), cabeza y cara. 100×100 | `avatars_*` |
| `acc.<ACCESORIO>` | accesorio encima del animal (corona, gafas...). 100×100, mismas coordenadas que el avatar | `accessories_*` |
| `scene.<ESCENA>` | decoración de fondo del avatar (se pinta sobre el degradado bg1→bg2 y debajo del animal) | `avatar_scenes` |
| paleta `avatar.<ANIMAL>.<VARIANTE>` | `@pal:` con las 7 ranuras `head light dark inner bg1 bg2 shirt` | `avatars_*` |
| `banner.<PATRON>` | un motivo suelto centrado en 100×100 (se repite en el banner), color en la ranura `accent` | `banners` |
| `fx.<tipo>` | partícula/destello suelto, 100×100, centrado | `fx` |
| `chapter.d<0-11>` / `chapter.n<0-11>` | emblema del capítulo del mapa (día / noche de brujas), 100×100 | `chapters_*` |
| `landmark.<tipo>` | monumento grande del capítulo (alto y ancho libres: 160×120) | `map_*` |
| `prop.<tipo>` | adorno del borde del mapa (árbol, farol...), 100×100, base apoyada en y≈92 | `map_*` |
| `series.<id>` | escudo de la serie del álbum, 100×100 | `pieces_*` |
| `piece.<id>` | pieza del álbum, p. ej. `piece.garden_1`, 100×100 | `pieces_*` |

## Estilo (muy importante: tiene que verse premium y todo igual de coherente)

- **Lienzo 100×100**, dibujo centrado y dentro de 8..92 (deja aire; las sombras pueden llegar a 95).
- **Plano con volumen suave**, como las pegatinas 3D de Apple pero planas: cada masa de color lleva un degradado de 2 tonos
  (luz arriba-izquierda, sombra abajo-derecha: `lin(...)` o `rad(...)` con `mix("#color", "#FFFFFF", .35)` arriba y `mix("#color","#000000",.18)` abajo).
- **Contorno**: ninguno, o uno fino (1.2–2.0) del mismo color más oscuro (`mix(color, "#2B1B3A", .45)`), nunca negro puro. Elige uno por dibujo y sé coherente.
- **Brillos**: 1–3 manchas blancas con alfa 0.35–0.7 (óvalos o curvas finas) en la zona de luz. Sombra de contacto: `s.ellipse(50, 90, r, 4, fill="#00000024")` para objetos que se apoyan.
- **Silueta primero**: se tiene que reconocer a 48 px. Pocas formas grandes mejor que muchos detalles diminutos.
- **Detalle**: entre 8 y 40 formas por dibujo (más solo para paisajes). Mantén cada dibujo por debajo de ~6 KB de JSON.
- **Paleta**: colores cálidos y amables (como la app: crema `#F3EFE6`, salvia `#6B9E86`, coral `#E07A5F`, oro `#E0A93B`, tinta `#3D405B`).
  Evita saturación chillona; los oscuros son morados/azules profundos, no negros.
- **Piezas del álbum**: la carta ya pone su propio fondo, halo y marco; tú dibujas solo el objeto/animal/escena, sin fondo cuadrado.
  Las 10 piezas de una serie se parecen en estilo y paleta, y se distinguen por la silueta.
- Sin texto, números ni letras dentro del dibujo.

## Revisión visual (obligatoria)

Antes de dar un paquete por terminado, mira **todas** las hojas y comprueba: ¿se reconoce cada cosa? ¿hay formas cortadas por el borde?
¿los degradados van del lado correcto? ¿se parece al resto de la serie? Corrige y vuelve a mirar. Si algo no sale bien tras tres intentos,
simplifícalo (una silueta limpia y bonita vale más que un detalle roto).
