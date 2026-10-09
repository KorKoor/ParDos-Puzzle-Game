# Audio de ParDos

Todo el sonido del juego está **sintetizado por código** (sin muestras ni música de terceros): no hay derechos de autor que pagar ni licencias que citar, y se puede regenerar o retocar cuando se quiera.

## Qué suena

| Parte | Cuántos | Dónde vive |
|---|---|---|
| Efectos de sonido (menús, juego, premios, cartas, compras…) | **213** | `app/src/main/res/raw/*.ogg` (≈ 2,2 MB) |
| Notas de fusión (una por nivel de ficha × 10 "voces") | 120 (`mg_<voz>_01…12`) | incluidas en los 213 |
| Música adaptativa | **5 piezas × 3 capas** | `music_<pieza>_<capa>.ogg` (≈ 4 MB) |

### Notas de fusión
Cada fusión es una **nota de la escala pentatónica de Do** (suenan bien aunque se mezclen): cuanto más alta es la ficha, más aguda la nota. Las fusiones de una misma jugada forman un arpegio (55 ms entre notas). Con fichas grandes se añade un golpe grave (`merge_sub`, nivel ≥ 6) y un destello (`merge_sparkle`, nivel ≥ 8).
La **voz** (marimba, kalimba, bloop, koto, caja de música, xilófono, cristal, ondas, zap, campana de fuego) sigue al **efecto de fusión equipado** en la tienda: cambiar el efecto cambia también cómo suena el juego.

### Combos y racha (flow)
- `combo_1…4` suenan a los 3, 5, 8 y 12 de combo (`GameAudio.combo`).
- `flow_in / flow_tier / flow_out`: al entrar en racha, al subir de nivel de racha y al perderla (un soplido suave).
- Hitos de ficha (`milestone`): 128, 256, 512… con fanfarria creciente.
- Victoria: `star_1/2/3` suenan una a una con cada estrella; `win`, `win_big` (3 estrellas), `boss_win`. La música baja (duck) para que se oigan.

### Detalles que también suenan
Récord personal al repetir un nivel (`new_best`, con su aviso "¡NUEVO RÉCORD!" en el resumen), "ya casi" al cruzar el 50, 75 y 90 % de la meta (`goal_ping`, cada vez más agudo), hito de racha de victorias (`streak`), los poderes de espera **Limpiar** (`board_clear` + sacudida) y **Fusión** (nota de fusión), el corazón que se pierde en la Torre (`heart_lost`), subir de nivel de jugador (`level_up`), tocar un nivel bloqueado (`ui_locked`), cambiar de pestaña (`ui_tab`) y reclamar misiones diarias o el premio de la liga (`claim`).

### Menús, tienda y premios
Un "tok" suave en **cualquier toque sobre algo pulsable** (modificador `Modifier.uiTapSounds()` aplicado una sola vez en `MainActivity`; si el botón ya tiene su propio sonido —comprar, reclamar, volver— el genérico se calla). Sonidos propios para: abrir/cerrar, interruptor on/off, pestañas, error, bloqueado, notificación, monedas contando, gemas, cofre (sacudida + apertura), cartas por rareza (común → legendaria), carta nueva, brillante (foil), vender, intercambiar, ruleta (tic/parada/premio), hucha, compra, anuncio con premio, regalo, subida de pase, subida de nivel/rango, platino, logros y misiones.

### Música adaptativa
Cinco piezas en bucle (≈ 45 s cada una), cada una en **3 capas sincronizadas**:

| Capa | Qué es | Cuándo entra |
|---|---|---|
| `base` | bajo, colchón y arpegio suave | siempre |
| `groove` | percusión suave y acordes cortos | al ganar ritmo en la partida |
| `lead` | melodía de campanitas | en racha (flow) y momentos grandes |

| Pieza | Tono | Se usa en |
|---|---|---|
| `zen` | Do mayor pentatónico (cálido) | capítulos 1-3 y los modos sueltos |
| `dusk` | Re menor pentatónico (atardecer) | capítulos 4-6 |
| `deep` | Mi menor pentatónico (aventura) | capítulos 7-9 y la Torre |
| `boss` | La menor, más tenso | niveles jefe |
| `halloween` | oscura y juguetona | menús y partidas durante la Noche de brujas (los jefes siguen con `boss`) |

Los **menús** conservan la melodía de siempre (`theme_song.mp3`) y solo cambian a la pieza `halloween` en la Noche de brujas. Mientras estás en el menú, la pieza del juego se va decodificando en segundo plano para que al empezar un nivel la música entre sin espera.

La mezcla se hace en tiempo real (`AdaptiveMusic.kt`: hilo con `AudioTrack`, decodificación con `MediaCodec`, cambios de capa y de pieza con fundidos). La intensidad (`MusicDirector.intensity(0..3)`) sube con las jugadas seguidas que fusionan y baja al perder la racha. Al ganar, la música baja unos segundos para que se oiga la fanfarria.

## Escucharlo en la computadora: la mesa de sonido
```bash
python tools/audio/soundboard.py          # genera tools/audio/soundboard.html y lo abre en el navegador
```
Una sola página (≈ 9 MB, lleva los audios dentro, funciona sin internet) con: los **93 efectos** agrupados (volumen que tienen en el juego y dónde suenan; los de reserva salen atenuados), las **notas de fusión** de cada voz con un **simulador de jugada** (varias fusiones + combo, con la misma lógica que `GameAudio.merges`), **escenas** que encadenan sonidos como el juego (victoria con 3 estrellas, derrota, racha hasta FLOW, cofre épico, jefe, menús, premios, ruleta) y el **mezclador de la música** (5 piezas × 3 capas, con los presets de intensidad del juego).

## Cómo se comporta con el teléfono
- **Cortesía**: si ya suena música de otra app (Spotify, YouTube…), la música del juego **no arranca** ni le roba el audio; los efectos sí suenan.
- **Segundo plano**: al salir de la app la música se suspende (Android 16 exige que las apps no suenen en segundo plano). Al volver, se reanuda sola.
- **Anuncios**: al abrirse un anuncio la app pasa a segundo plano y la música se calla sola; al cerrarlo vuelve.
- **Ajustes → Sonido y tacto**: interruptores de efectos y música, y un **deslizador de volumen** para cada uno (`SettingsManager.sfxVolume / musicVolume`).
- **En la partida**: un botón de altavoz en la barra superior silencia (o activa) efectos y música de un toque, sin salir del nivel.

## Cómo regenerar o cambiar los sonidos
Requisitos: Python 3 con `numpy`, y `ffmpeg` (para pasar a `.ogg`). Se apunta a ffmpeg con la variable `FFMPEG`.

```bash
cd tools/audio
export FFMPEG="/ruta/a/ffmpeg"
python sfx.py                 # los 213 efectos (o: python sfx.py ui_tap  → solo los que empiecen así)
python music.py               # las 5 piezas (o: python music.py zen)
python analyze.py             # hojas con espectrogramas y niveles para revisarlos
```

- `synth.py`: osciladores, envolventes, filtros, reverb y ayudantes comunes.
- `sfx.py`: cada efecto es una función decorada con `@sfx("nombre", peak)`; `peak` es su **importancia** (más alto = más fuerte). El render normaliza por nivel (RMS) y limita el pico a 0,9 para que nada sature.
- `music.py`: las piezas se definen en `SETS` (tempo, acordes, instrumentos de cada capa).
- `analyze.py`: revisa picos, RMS y la costura del bucle de la música.

Los `.ogg` generados **sí se suben al repositorio** (los necesita el build). Los intermedios (`.wav`, hojas) no (`tools/audio/.gitignore`).

## Código
| Archivo | Para qué |
|---|---|
| `audio/Sfx.kt` | Catálogo (`enum Sfx`) con archivo, ganancia, separación mínima entre repeticiones y prioridad |
| `audio/GameAudio.kt` | `GameAudio` (SoundPool, notas de fusión, combos, pausa/reanudar) y `MusicDirector` (qué pieza y qué capas suenan) |
| `audio/AdaptiveMusic.kt` | Mezclador de capas con `AudioTrack` |
| `audio/UiSounds.kt` | Sonido de toque global |
| `ui/settings/SettingsScreen.kt` | Interruptores y volúmenes |

## Para añadir un sonido nuevo
1. Añade su función en `tools/audio/sfx.py` y ejecuta `python sfx.py nombre`.
2. Añade una línea en `enum Sfx` (`audio/Sfx.kt`) con el nombre del archivo.
3. Llámalo con `GameAudio.play(Sfx.NOMBRE)` donde ocurra el evento.

> Nota: el sonido se diseñó y se verificó con espectrogramas, niveles y registros del teléfono, pero **la última palabra es el oído**: si algo suena fuerte, flojo o repetitivo, es cuestión de cambiar `peak` en `sfx.py` y regenerar ese efecto.
