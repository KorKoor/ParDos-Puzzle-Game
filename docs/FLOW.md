# Estado de flow: por qué se juega "una más"

Principio: flow = una meta clara + respuesta inmediata + un reto a la medida de la habilidad + cero fricción entre partidas.
Todo vive en `shared/.../domain/flow/Flow.kt` (reglas puras, probadas en `FlowTest`) y se pinta en `ui/game/components/FlowUi.kt`.

| Pieza | Qué hace | Dónde se ve |
|---|---|---|
| **Aura de flow** (`FlowMeter`) | Cada jugada que fusiona suma ímpetu; una que solo desliza lo apaga. A 4 / 8 / 14 seguidas: "¡En racha!" → "¡Imparable!" → "¡FLOW!" con halo dorado → naranja → arcoíris, vibración y un sonido más agudo. Da +5 / +10 / +20 % de monedas al ganar. | Borde del tablero y letrero sobre él |
| **Racha de victorias** (`WinStreak`) | Niveles de campaña ganados seguidos: +5 % de monedas por victoria (hasta +50 %). Perder **enfría la racha a la mitad** (no a cero): duele lo justo para querer recuperarla. Hitos a 5/10/20/40/80 con gemas y deshacer. | Llama "N seguidos" en la cabecera y en el resumen |
| **Ayuda adaptativa** (`AssistPolicy`) | Si un nivel se atasca (2, 4, 6 derrotas) sube la ayuda **a la vista**: +1/+2/+3 deshacer gratis (una sola vez por nivel), hasta +25 % de movimientos/tiempo y más "evoluciones de la suerte". Se avisa con "Te echamos una mano…" y se quita al ganar. Reemplaza al antiguo "modo piedad", que estaba declarado pero no hacía nada. | Aviso al empezar el nivel |
| **Casi lo logras** (`NearMiss`) | Tras perder, una frase concreta: "¡Estabas a una fusión de la meta!", "¡Te faltó solo el peldaño 16!", "¡Te faltaron solo 12 fusiones!"… | Pantalla de derrota, sobre "Reintentar" |
| **Un nivel más** (`NextLevelTeaser`) | El resumen enseña qué viene: tipo del siguiente nivel, "¡JEFE!" o "¡NUEVA REGLA!", y la distancia al cofre del capítulo. | Resumen de victoria |
| **Siguiente nivel automático** | Tras ver las estrellas empieza una cuenta atrás de 4,5 s con barra; **cualquier toque la pausa**. No salta cuando hay hito de racha ni al cerrar el capítulo (para disfrutar el cofre). Se apaga en *Ajustes → Ritmo de juego*. | Resumen de victoria |

Lo que **no** se hace: la ayuda nunca está escondida, la racha no se pierde de golpe y el avance automático se puede desactivar.

Para ajustar: `FlowTier` (umbrales y bonos), `WinStreak.MILESTONES`, `AssistPolicy.forAttempts`, `autoNextMs` (4500) en `GameScreen.kt`.
