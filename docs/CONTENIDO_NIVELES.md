# Contenido de niveles (campaña de 2.400 niveles)

Objetivo: que quien juega 3-4 horas al día tenga contenido nuevo y variado durante **más de un mes**, sin tocar la campaña.

## Cuánto dura
Estimación con los movimientos mínimos de cada nivel (`CatalogReportTest`, `CAL=report`): 0,9 s por movimiento, +25 % por reintentos y +12 s por nivel (menús, cofres, anuncios).

| Tramo | Horas | Minutos por nivel |
|---|---|---|
| 1-400 | 25,5 | 3,8 |
| 401-800 | 27,5 | 4,1 |
| 801-1200 | 27,9 | 4,2 |
| 1201-1600 | 30,0 | 4,5 |
| 1601-2000 | 31,1 | 4,7 |
| 2001-2400 | 31,0 | 4,6 |
| **Total** | **173 h** | |

- A 3,5 h/día: **~49 días**. A 4 h/día: ~43 días. Muy rápido (0,6 s por movimiento, 4 h/día): ~33 días.
- Cada nivel dura más con la campaña (3,8 → 4,7 min) y los jefes (uno cada 10 niveles) rompen el ritmo.

## Tipos de nivel (15)
Los 9 de antes (Zen, Puntos, Lluvia de 4, Con ventaja, Piedras, Sprint, Gemelas, Contrarreloj, Jefe) y **6 nuevos**, cada uno con su color, su icono y su tarjeta de "NUEVA REGLA" la primera vez:

| Tipo | Qué cambia | Cómo se gana |
|---|---|---|
| **Pesadas** | Solo caen 4 y 8 (casi nunca un 2): el tablero se llena enseguida | Llegar a la ficha |
| **Escalera** | Fichas consecutivas a la vez (16-32-64-128…) | Tener todos los peldaños en el tablero |
| **Del revés** | Los controles no van hacia donde deslizas (4 giros: izquierda↔derecha, arriba↔abajo, todo al revés, giro de 90°) | Llegar a la ficha (a veces con movimientos o reloj) |
| **Maratón** | Cuentan las fusiones | Hacer N fusiones con un tope de movimientos |
| **Tormenta** | Cada pocos movimientos cae una **piedra temporal** que se va sola | Llegar a la ficha |
| **Combo** | Un solo deslizamiento tiene que fusionar varios pares | 3-4 pares de golpe, 1-3 veces |

Además: 6 jefes nuevos (Escalera real, Pesadilla, Espejismo, Ojo del huracán, Gran maratón, Reacción en cadena → 13 recetas en total), **tableros 6×6** (Piedras y Pesadas) y **32 distribuciones de piedras nuevas** (71 en total, calibradas con el bot).

## Cuándo aparece cada cosa (jugando ~50 niveles/día)
| Día aprox. | Nivel | Novedad |
|---|---|---|
| 1 | 1-20 | Tutorial a mano: una regla por nivel |
| 1-2 | 41 | Gemelas y Contrarreloj entran al sorteo |
| 2 | 61 | **Pesadas** |
| 3 | 121 | **Escalera** (3 peldaños) |
| 5 | 201 | **Del revés** (izquierda ↔ derecha) |
| 6 | 281 | **Maratón** |
| 7 | 321 | Giro arriba ↔ abajo |
| 9 | 401 | **Tormenta** |
| 10 | 481 | Giro "todo al revés" |
| 11 | 521 | **Combo** (3 pares) |
| 12 | 581+ | Niveles de doble regla (del revés + Sprint/Contrarreloj/Piedras) |
| 14 | 681 | Giro de 90° |
| 19 | 901 | Escaleras de 5 peldaños |
| 20 | 961 | Combos de 4 pares en 5×5 y combos repetidos |
| 29 | 1401 | Escaleras hasta 1.024 y combos 4×2 |
| 30-49 | 1500-2400 | Todo mezclado con la dificultad al máximo |

Cada tipo debuta en su capítulo **más fácil** y con peso extra los primeros 3 capítulos, y siempre aparece en el nivel 5 de su capítulo de estreno.

## Dificultad
- Holgura de los Sprint: 1,72 → 1,30 hacia el capítulo 105. Reloj: 1,9 → 1,4 s por movimiento mínimo.
- Piedras: gravedad 1 → 3 según el capítulo; las pesadas y las tormentas suman reglas poco a poco.
- Tormenta: de una piedra cada 8 movimientos (máx. 2) a una cada 3 (máx. 4).
- Maratón: de 80 a 320 fusiones; Combo: de 3 pares a 4 pares repetidos; Escalera: de 3 a 5 peldaños.
- Las estrellas premian la eficiencia (menos movimientos) y siguen dando motivo para repetir niveles.

## Cómo está probado
- `LevelCatalogTest` / `NewMechanicsTest`: los 2.400 niveles son válidos, el guion de cada capítulo se cumple, cada tipo llega en su capítulo y no antes, los giros entran uno a uno, las tormentas no ahogan el tablero.
- `LevelFeasibilityTest`: un bot juega **todos** los niveles distintos (~730) con las reglas reales (incluidas las tormentas y los combos) y debe ganar al menos 3 de 6 partidas; una persona con deshacer lo hace mejor.
- `CalibrationTest` (con `CAL=heavy|ladder|marathon|combo|storm|sprint|pat|mm`) imprime lo que gana el bot con cada ajuste; así se calibraron los números.
- Para probar un nivel en el teléfono (build debug): `adb shell am start -n com.korkoor.pardos.debug/com.korkoor.pardos.MainActivity --ei debug_level 405`.

## Dónde tocar
- `LevelGenerator.UNLOCK` (capítulo de estreno de cada tipo), `weights()` (pesos), `plan()` (guion del capítulo).
- Parámetros de cada tipo: `heavyLevel`, `ladderLevel`, `marathonLevel`, `comboLevel`, `twistLevel`, `stormFor`, `bossLevel`.
- Piedras: `StonePatterns.all` (con su `maxExp` calibrado). Niveles a mano: `HandmadeLevels`.
- Para alargar la campaña: `LevelCatalog.TOTAL_LEVELS` (el mapa y los capítulos se adaptan; hay que repetir `CAL=report` para ver las horas).
