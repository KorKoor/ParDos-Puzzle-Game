# Prestigio, Platino y rivalidad

Sistema de estatus a largo plazo: lo que haces en ParDos se convierte en **puntos de prestigio**, un **rango**, **títulos**
que lucir y un **Platino** por completarlo todo. Todo se calcula en `shared` (reglas puras, con tests) y la app solo lo pinta.

## Puntos de prestigio
| Fuente | Puntos |
|---|---|
| Pieza común / rara / épica / legendaria | 2 / 5 / 12 / 30 |
| Versión foil | +8 |
| Serie completa | 60 |
| Álbum completo | 500 |
| Hito logrado | 15 |
| Estrellas de campaña | ½ por estrella |
| Piso de la Torre | 10 por piso |
| Nivel de campaña | 5 por nivel |
| Platino | 1000 |

## Rangos (con premio al subir)
Novato 0 · Aprendiz 150 · Adepto 500 · Experto 1200 · Maestro 2500 · Gran Maestro 4500 · Leyenda 6500 · Mítico 9500.

## Hitos (~76)
Contadores acumulativos con meta visible y barra de progreso (piezas, épicas+, foil, series, álbum, nivel y estrellas de campaña,
Torre, racha de victorias y de días, jefes, tipos de nivel, retos diarios, amigos, nivel de flow, trofeos). Cada hito paga monedas,
a veces gemas o un cofre. La primera vez se registran de golpe los que ya tenías, con un único aviso.

## Títulos
Se muestran bajo el nombre en Inicio, Perfil y Amigos. Se consiguen por rango, por hitos, por el Platino o comprándolos con gemas
(`PrestigeManager.buy`). Equipa el que quieras en Prestigio → Títulos.

## Platino
Todos los logros + todos los hitos. Premio: 5000 monedas, 300 gemas, 2 cofres épicos y el título PLATINO. En el perfil y en
amigos aparece el icono de trofeo de platino.

## Rivalidad
- El podio de amigos se ordena por prestigio.
- La pantalla Prestigio muestra a quién estás a punto de pasar, o quién va justo por delante, con los puntos que faltan.
- El perfil se sincroniza (prestigio, título, platino, mejor piso de la Torre, piezas) con el resto de datos.

## Principios
- Sin escasez falsa, sin temporizadores inventados ni cobros ocultos: el progreso se gana jugando.
- La rivalidad es entre amigos, no un ranking global público.

## Código
- `shared/.../domain/prestige/Prestige.kt` + `PrestigeTest`
- `app/.../data/local/PrestigeManager.kt` (lee las fuentes, paga premios, emite eventos)
- `app/.../ui/prestige/` (`PrestigeScreen`, `PrestigeArt`, `PrestigeToasts`)
- Ganchos: `GameViewModel` (nivel ganado, piso de Torre, reto diario) y `MainActivity.onResume`.

## Arreglos de paso
- Icono de lanzador de Halloween restaurado (válido hasta el 2 de noviembre; luego vuelve el clásico según `docs/play-store`).
- `ToyButton`: el reflejo ya no ensancha el botón (en Misiones el texto se apilaba letra por letra).
