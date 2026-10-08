# Niveles de la campaña

Un nivel ya no es solo "llega a la ficha X": es un `LevelSpec` (en `shared/.../domain/level/`) con sus reglas. El juego solo lee ese dato, así que **añadir o ajustar niveles no toca el motor ni la interfaz**.

## Tipos de nivel (`LevelKind`)

| Tipo | Qué cambia | Cómo se gana |
|---|---|---|
| **Zen** | nada, sin presión | llegar a la ficha |
| **Puntos** | meta de puntos, no de ficha | sumar los puntos |
| **Lluvia de 4** | caen muchos más 4 | llegar a la ficha (más rápido, más caótico) |
| **Con ventaja** | empiezas con fichas altas ya puestas | llegar a la ficha (nivel corto) |
| **Piedras** | casillas bloqueadas que no se mueven ni se fusionan | llegar a la ficha |
| **Sprint** | movimientos limitados | llegar a la ficha antes de quedarse sin movimientos |
| **Gemelas** | dos fichas iguales a la vez | tener 2 fichas de X |
| **Contrarreloj** | reloj (las combinaciones dan segundos) | llegar a la ficha a tiempo |
| **Jefe** | varias reglas a la vez, nombre propio | según sus reglas |

Las **estrellas** premian la eficiencia: sin reloj, los movimientos usados frente al mínimo posible (3★ ≤ 1,3×, 2★ ≤ 1,75×); con reloj, el tiempo gastado (3★ en la mitad del límite). Cada tipo tiene color e icono en `ui/design/LevelKindStyle.kt`.

## Cómo añadir o cambiar un nivel

Escribe una línea en `HandmadeLevels.specs` (`shared/.../domain/level/HandmadeLevels.kt`). Un número de nivel que esté ahí **sustituye** al generado.

```kotlin
stones(23, tile = 256, pattern = "4-pilar-a"),
sprint(24, tile = 128, size = 4, slack = 1.5),          // slack: 1,75 generoso · 1,5 normal
twins(25, tile = 64),
clock(26, tile = 128, secondsPerMove = 1.8),
score(27, tile = 256, size = 5),
headStart(28, tile = 256),
boss(sprint(30, tile = 256, stones = StonePatterns.byId("4-cuatro")), "Mi jefe", tip = "..."),
```

Los límites (movimientos, tiempo, puntos) se calculan solos desde la meta (`LevelMath`). Para un tipo de regla nuevo: un valor en `LevelKind`, su constructor en `LevelBuilders`, su color/icono en `LevelKindStyle` y (si debe salir solo) su peso en `LevelGenerator.weights`.

Para **alargar la campaña** basta subir `LevelCatalog.TOTAL_LEVELS` (el generador funciona con cualquier número).

## Cómo se generan los niveles que no están a mano

`LevelGenerator` (en `LevelCatalog.kt`) es determinista: el mismo número da siempre el mismo nivel.

- Cada capítulo (20 niveles, un cofre) sigue un guion: nivel 1 Zen · niveles 10 y 20 **jefes** · niveles 5 y 15 siempre especiales · el resto se sortea con pesos, nunca repite el tipo anterior, penaliza los ya usados en el capítulo y nunca junta dos tipos exigentes.
- Los tipos nuevos se sueltan poco a poco (Gemelas y Contrarreloj desde el capítulo 3).
- La ficha base sube hasta 1024 y ahí se queda: desde ahí la dificultad viene de las reglas (piedras más molestas, menos holgura, reloj más justo), no de partidas de media hora.
- Variantes: tablero mini (3×3, rápido), normal (4×4) y amplio (5×5); metas más cortas o largas; piedras suaves combinadas con sprint, reloj o ventaja.
- El **capítulo 1 (niveles 1–20) está escrito a mano** como tutorial: cada regla se presenta sola antes de mezclarse.

El **reto diario** usa un tipo por día de la semana (lunes piedras, martes sprint, miércoles lluvia de 4, jueves puntos, viernes gemelas, sábado ventaja, domingo Zen).

## Piedras (`StonePatterns`)

Cada patrón tiene `severity` (1 = rincones, casi no molestan · 2 = una piedra suelta · 3 = parten el tablero) y `maxExp`: la ficha más alta (potencia de 2) con la que un bot sigue ganando casi siempre. El generador nunca pide más que eso. Los medidos con `LevelFeasibilityTest`; al añadir un patrón, mídelo y apunta su `maxExp`.

## Pruebas que protegen los niveles

- `LevelCatalogTest`: los 2.400 niveles son válidos (`LevelValidator`), el guion de cada capítulo se cumple, hay variedad real (tableros, metas, piedras, tipos), Zen es minoría, el primer capítulo introduce una regla cada vez.
- `LevelFeasibilityTest`: un bot juega una muestra de ~190 niveles distintos con sus reglas (límite de movimientos y tiempo incluidos) y debe ganar casi siempre. **Si un nivel nuevo es demasiado duro, esta prueba lo dice con su número.**
- `GameEngineStonesTest`: las piedras bloquean, parten filas, no reciben fichas; sin piedras el motor es idéntico al anterior (compara contra una copia del algoritmo viejo).
- `LevelRulesTest`: meta, estrellas, progreso y errores típicos al escribir un nivel.

## Probar un nivel en el teléfono

Solo en builds de depuración:

```
adb shell am start -n com.korkoor.pardos.debug/com.korkoor.pardos.MainActivity --ei debug_level 47
```
