package com.korkoor.pardos.domain.level

import com.korkoor.pardos.domain.level.LevelBuilders.boss
import com.korkoor.pardos.domain.level.LevelBuilders.clock
import com.korkoor.pardos.domain.level.LevelBuilders.fours
import com.korkoor.pardos.domain.level.LevelBuilders.headStart
import com.korkoor.pardos.domain.level.LevelBuilders.score
import com.korkoor.pardos.domain.level.LevelBuilders.sprint
import com.korkoor.pardos.domain.level.LevelBuilders.stones
import com.korkoor.pardos.domain.level.LevelBuilders.twins
import com.korkoor.pardos.domain.level.LevelBuilders.zen

/**
 * Niveles escritos a mano. Cualquier número de nivel que aparezca aquí sustituye al que generaría el catálogo, así que
 * es el sitio para afinar un nivel concreto o diseñar uno especial: basta una línea nueva en [specs].
 *
 * El primer capítulo es el tutorial: cada regla nueva se presenta sola y fácil antes de mezclarse con otras.
 */
internal object HandmadeLevels {

    val specs: List<LevelSpec> = listOf(
        // ---- Capítulo 1: aprender las reglas una a una ----
        zen(1, tile = 32, size = 3),
        zen(2, tile = 64, size = 3),
        score(3, tile = 64, size = 3, tip = "Aquí gana quien más puntos suma: cada fusión cuenta"),
        zen(4, tile = 64, size = 4, tip = "Un tablero de 4×4 te da más sitio"),
        fours(5, tile = 64, size = 4, tip = "Caen muchos 4: te acercas más rápido, pero hay que hacer sitio"),
        headStart(6, tile = 128, size = 3),
        stones(7, tile = 128, pattern = "4-esq-nw"),
        sprint(8, tile = 128, size = 4, slack = 1.7),
        score(9, tile = 128, size = 4),
        boss(
            sprint(10, tile = 128, size = 4, slack = 1.6, stones = StonePatterns.byId("4-esq-se")),
            "Rincón apurado", tip = "Primer jefe: una piedra y pocos movimientos"
        ),
        zen(11, tile = 256, size = 4),
        twins(12, tile = 64, size = 4),
        stones(13, tile = 256, pattern = "4-esq-ne"),
        clock(14, tile = 128, size = 4, secondsPerMove = 2.0),
        fours(15, tile = 256, size = 4),
        headStart(16, tile = 256, size = 4),
        sprint(17, tile = 256, size = 4, slack = 1.9),
        score(18, tile = 256, size = 4),
        zen(19, tile = 512, size = 5, title = "Zen amplio"),
        boss(
            clock(20, tile = 256, size = 4, secondsPerMove = 1.9, stones = StonePatterns.byId("4-cuatro")),
            "Cuatro rincones", tip = "Jefe del capítulo: cuatro piedras y el reloj en marcha"
        )
    )

    private val byId: Map<Int, LevelSpec> = specs.associateBy { it.id }

    fun get(id: Int): LevelSpec? = byId[id]
}
