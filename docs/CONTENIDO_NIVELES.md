# Contenido de niveles (campaña de 2.400 niveles + torre + retos diarios)

Objetivo: que quien juega 3-4 horas al día tenga contenido nuevo y variado durante **más de un mes**, sin tocar la campaña.

## Cuánto dura
Estimación con los movimientos mínimos de cada nivel (`CatalogReportTest`, `CAL=report`): 0,9 s por movimiento, +25 % por reintentos y +12 s por nivel (menús, cofres, anuncios).

| Tramo | Horas | Minutos por nivel |
|---|---|---|
| 1-400 | 26,3 | 3,9 |
| 401-800 | 28,2 | 4,2 |
| 801-1200 | 29,5 | 4,4 |
| 1201-1600 | 31,1 | 4,7 |
| 1601-2000 | 30,9 | 4,6 |
| 2001-2400 | 33,5 | 5,0 |
| **Campaña** | **179,5 h** | |

- A 3,5 h/día: **~51 días**. A 4 h/día: ~45 días. Muy rápido (0,6 s por movimiento, 4 h/día): ~34 días.
- Y **después**: la **Torre infinita** (sin final), los **retos diarios** (3 semanas distintas que se turnan) y todo lo social.

## Tipos de nivel (17)
Los 9 de antes (Zen, Puntos, Lluvia de 4, Con ventaja, Piedras, Sprint, Gemelas, Contrarreloj, Jefe) y **8 nuevos**, cada uno con su color, su icono y su tarjeta de "NUEVA REGLA" la primera vez:

| Tipo | Qué cambia | Cómo se gana |
|---|---|---|
| **Pesadas** | Solo caen 4 y 8 (casi nunca un 2): el tablero se llena enseguida | Llegar a la ficha |
| **Escalera** | Fichas consecutivas a la vez (16-32-64-128…) | Tener todos los peldaños en el tablero |
| **Cosecha** | Cada fusión que da la ficha pedida cuenta | Crear N fichas de 16/32/64/128 |
| **Del revés** | Los controles no van hacia donde deslizas (izquierda↔derecha, arriba↔abajo, todo al revés, giro de 90°) | Llegar a la ficha (a veces con movimientos o reloj) |
| **Callejón** | Una dirección está prohibida (sin arriba, sin abajo, sin izquierda o sin derecha) | Llegar a la ficha |
| **Maratón** | Cuentan las fusiones | Hacer N fusiones con un tope de movimientos |
| **Doble caída** | Caen dos fichas nuevas por jugada | Llegar a la ficha |
| **Tormenta** | Cada pocos movimientos cae una **piedra temporal** que se va sola | Llegar a la ficha |
| **Combo** | Un solo deslizamiento tiene que fusionar varios pares | 3-4 pares de golpe, 1-3 veces |

También: tableros 6×6, **71 distribuciones de piedras** y dificultad creciente hasta el nivel 2.400.

## Jefes (16 recetas, con segunda fase)
Cada jefe (uno cada 10 niveles) tiene nombre propio y, **desde el capítulo 2, una segunda fase**: pasados unos movimientos suena un aviso ("FASE 2") y cambian las reglas a media partida.

| Jefe | Regla base | Fase 2 |
|---|---|---|
| El Muro, La Cantera, Rompecabezas, Espejismo, Cosecha de oro | Sprint/Gemelas/Con ventaja/Del revés/Cosecha | ¡Cae una tormenta! |
| Tormenta de 4, Duelo de puntos, Reacción en cadena | Lluvia de 4 / Puntos / Combo | ¡Llueven los 4! |
| Doble o nada, Ojo del huracán, Lluvia de meteoros | Gemelas / Tormenta / Doble caída | ¡Controles cambiados! |
| Relojería, Pesadilla, Gran maratón, Escalera real | Contrarreloj / Pesadas / Maratón / Escalera | Una dirección se bloquea / ¡Arriba y abajo se cambian! |
| Callejón sin salida | Una dirección prohibida | ¡Todo al revés! |

Los jefes con límite de movimientos reciben un 12 % más de margen para compensar la fase 2. Los jefes que salen en cada capítulo dependen de lo ya desbloqueado.

## Cuándo aparece cada cosa (jugando ~50 niveles/día)
| Día aprox. | Nivel | Novedad |
|---|---|---|
| 1 | 1-20 | Tutorial a mano: una regla por nivel |
| 1-2 | 41 | Gemelas y Contrarreloj entran al sorteo |
| 2 | 61 | **Pesadas** |
| 3 | 121 | **Escalera** (3 peldaños) |
| 4 | 161 | **Cosecha** |
| 5 | 201 | **Del revés** (izquierda ↔ derecha) |
| 5-6 | 261 | **Callejones** (sin arriba / sin abajo) |
| 6 | 281 | **Maratón** |
| 7 | 321 | **Doble caída** |
| 9 | 401 | **Tormenta** |
| 10 | 481 | Callejones sin izquierda / derecha |
| 11 | 521 | **Combo** (3 pares) |
| 12 | 581+ | Niveles de doble regla (del revés + Sprint/Contrarreloj/Piedras) |
| 14 | 681 | Giro de 90°, "todo al revés" más adelante |
| 19 | 901 | Escaleras de 5 peldaños |
| 20 | 961 | Combos de 4 pares en 5×5 y combos repetidos |
| 29 | 1401 | Escaleras hasta 1.024 y combos 4×2 |
| 30-51 | 1500-2400 | Todo mezclado con la dificultad al máximo |

Cada tipo debuta en su capítulo **más fácil** y con peso extra los primeros 3 capítulos, y siempre aparece en el nivel 5 de su capítulo de estreno.

## Torre infinita (modo nuevo)
*Elige tu ritmo → Torre infinita.* Pisos seguidos con **3 corazones**: cada piso es un nivel de la campaña (ya probado con el bot) escogido con semilla fija, **cada 5 pisos hay un jefe** (con su fase 2) que devuelve un corazón. Perder cuesta un corazón y se repite el piso; sin corazones la subida termina. Los premios (monedas, gemas en los jefes) se cobran al momento, así que bajar a cobrar no pierde nada. El piso 10 equivale al capítulo 18 de la campaña y el 60 al 103: nunca se acaba. Guarda el récord de piso.

## Retos diarios: 3 semanas que se turnan
Un tipo de reto por día (domingo siempre Zen):
- Semana A: Piedras, Sprint, Lluvia de 4, Puntos, Gemelas, Con ventaja.
- Semana B: Pesadas, Escalera, Cosecha, Del revés/Callejón, Doble caída, Tormenta.
- Semana C: Maratón, Combo, Tormenta, Doble caída, Pesadas, Escalera.
El bot comprueba que todos los retos diarios se pueden ganar (en 4×4 la meta máxima es 1.024).

## Dificultad
- Holgura de los Sprint: 1,72 → 1,30 hacia el capítulo 105. Reloj: 1,9 → 1,4 s por movimiento mínimo.
- Piedras: gravedad 1 → 3 según el capítulo; las pesadas y las tormentas suman reglas poco a poco.
- Tormenta: de una piedra cada 8 movimientos (máx. 2) a una cada 3 (máx. 4).
- Maratón: de 80 a 320 fusiones; Combo: de 3 pares a 4 pares repetidos; Escalera: de 3 a 5 peldaños; Cosecha: más fichas de más valor.
- Las estrellas premian la eficiencia (menos movimientos) y siguen dando motivo para repetir niveles.

## Cómo está probado (353 pruebas)
- `LevelCatalogTest` / `NewMechanicsTest`: los 2.400 niveles son válidos, el guion de cada capítulo se cumple, cada tipo llega en su capítulo y no antes, los giros entran uno a uno, las tormentas no ahogan el tablero, todos los jefes (menos los del primer capítulo) tienen fase 2 y esta llega a verse.
- `LevelFeasibilityTest`: un bot juega **todos** los niveles distintos (~720) y los retos diarios con las reglas reales (tormentas, combos, cosecha, callejones y fases de jefe incluidas) y debe ganar al menos 3 de 6 partidas; una persona con deshacer lo hace mejor.
- `TowerTest`: cada piso es un nivel real y estable, los pisos de jefe son jefes, la dificultad sube y se queda en el último capítulo.
- `CalibrationTest` (con `CAL=heavy|ladder|marathon|combo|storm|sprint|pat|mm|new2`) imprime lo que gana el bot con cada ajuste.
- Para probar en el teléfono (build debug): `adb shell am start -n com.korkoor.pardos.debug/com.korkoor.pardos.MainActivity --ei debug_level 405`; torre: `--ei debug_tower 5 --ei debug_hearts 2 --es debug_tower_end win|lose`.

## Dónde tocar
- `LevelGenerator.UNLOCK` (capítulo de estreno de cada tipo), `weights()` (pesos), `plan()` (guion del capítulo), `twistArrival()` (cuándo entra cada giro).
- Parámetros de cada tipo: `heavyLevel`, `ladderLevel`, `harvestLevel`, `marathonLevel`, `doubleLevel`, `comboLevel`, `twistLevel`, `stormFor`, `bossLevel` y `withBossPhase`.
- Torre: `domain/tower/TowerRules.kt`. Diarios: `DailyChallenge.WEEKS`. Piedras: `StonePatterns.all`. Niveles a mano: `HandmadeLevels`.
- Para alargar la campaña: `LevelCatalog.TOTAL_LEVELS` (hay que repetir `CAL=report` para ver las horas).
