package com.korkoor.pardos.domain.tower

import com.korkoor.pardos.domain.level.LevelCatalog
import kotlin.random.Random

/**
 * Torre infinita: una partida de pisos seguidos con 3 corazones. Cada piso es un nivel de la campaña (ya probado con el bot),
 * escogido con una semilla fija según el piso: sube la dificultad con los pisos y cada 5 hay un jefe. Perder un nivel cuesta
 * un corazón y se repite el piso; los jefes devuelven un corazón. Sin corazones, la subida termina y queda el récord.
 *
 * Es el contenido sin final del juego: cuando se acaba la campaña (o se quiere variar) la torre sigue.
 */
object TowerRules {
    const val START_HEARTS = 3
    const val MAX_HEARTS = 4
    const val BOSS_EVERY = 5

    /** Capítulo de la campaña del que sale el piso: sube más rápido que la campaña (piso 10 ≈ capítulo 18, piso 60 ≈ capítulo 103) y se queda en el último. */
    fun chapterFor(floor: Int): Int {
        val f = floor.coerceAtLeast(1)
        return (1 + (f * 1.7)).toInt().coerceAtMost(LevelCatalog.TOTAL_LEVELS / LevelCatalog.LEVELS_PER_CHAPTER - 1)
    }

    fun isBossFloor(floor: Int): Boolean = floor > 0 && floor % BOSS_EVERY == 0

    /** Número de nivel de la campaña que se juega en este piso (siempre el mismo para el mismo piso). */
    fun levelIdFor(floor: Int): Int {
        val chapter = chapterFor(floor)
        val per = LevelCatalog.LEVELS_PER_CHAPTER
        val rnd = Random(floor * 2654435761L + 17)
        val pos = if (isBossFloor(floor)) {
            // El jefe pequeño del capítulo (10) o el final (20), alternando
            if ((floor / BOSS_EVERY) % 2 == 1) 10 else per
        } else {
            // Niveles normales: nunca el primero del capítulo (bienvenida) ni los jefes
            val options = (2..per - 1).filter { it != 10 }
            options[rnd.nextInt(options.size)]
        }
        return chapter * per + pos
    }

    data class FloorReward(val coins: Int, val gems: Int, val heart: Boolean)

    fun rewardFor(floor: Int): FloorReward {
        val f = floor.coerceAtLeast(1)
        val boss = isBossFloor(f)
        return FloorReward(
            coins = 20 + 8 * f + if (boss) 60 else 0,
            gems = if (boss) 2 + f / BOSS_EVERY else 0,
            heart = boss
        )
    }

    /** Corazones tras superar un piso: los jefes devuelven uno (hasta el máximo). */
    fun heartsAfterClear(floor: Int, hearts: Int): Int =
        if (isBossFloor(floor)) (hearts + 1).coerceAtMost(MAX_HEARTS) else hearts

    /** Texto corto del piso para la cabecera. */
    fun floorLabel(floor: Int): String = if (isBossFloor(floor)) "PISO $floor · JEFE" else "PISO $floor"
}
