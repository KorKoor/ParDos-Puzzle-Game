package com.korkoor.pardos.domain.logic

import com.korkoor.pardos.domain.level.LevelBuilders
import com.korkoor.pardos.domain.level.LevelKind
import com.korkoor.pardos.domain.level.LevelMath
import com.korkoor.pardos.domain.level.LevelSpec
import com.korkoor.pardos.domain.level.StonePatterns
import com.korkoor.pardos.domain.level.Storm
import com.korkoor.pardos.domain.level.Twist
import kotlin.random.Random

/** Configuración del reto diario. Es la misma para cualquier jugador que comparta el día. */
data class DailyConfig(
    val day: Int,
    val seed: Long,
    val boardSize: Int,
    /** Ficha de referencia del día (1.024 o 2.048); la meta real de este reto está en [spec]. */
    val target: Int,
    val themeIndex: Int,
    /** Reglas del día (cada día de la semana tiene su tipo de reto). */
    val spec: LevelSpec
)

object DailyChallenge {
    /** Número de temas disponibles (índices 0..THEME_COUNT-1). */
    const val THEME_COUNT = 5

    /**
     * Un tipo de reto por día (lunes = 0) y tres semanas distintas que se turnan: hay motivo para volver cada día,
     * los domingos se descansa y en tres semanas casi todos los tipos de nivel salen una vez.
     */
    val WEEKS: List<List<LevelKind>> = listOf(
        listOf(LevelKind.STONES, LevelKind.SPRINT, LevelKind.FOURS, LevelKind.SCORE, LevelKind.TWINS, LevelKind.HEADSTART, LevelKind.ZEN),
        listOf(LevelKind.HEAVY, LevelKind.LADDER, LevelKind.HARVEST, LevelKind.TWIST, LevelKind.DOUBLE, LevelKind.STORM, LevelKind.ZEN),
        listOf(LevelKind.MARATHON, LevelKind.COMBO, LevelKind.STORM, LevelKind.DOUBLE, LevelKind.HEAVY, LevelKind.LADDER, LevelKind.ZEN)
    )

    /** La primera semana (se conserva por compatibilidad). */
    val WEEK: List<LevelKind> get() = WEEKS.first()

    /** Número de semana (con la semana empezando en lunes) de un día de época. */
    fun weekIndex(epochDay: Int): Int = (epochDay + 3).floorDiv(7)

    /** Tipo de reto de un día. */
    fun kindFor(epochDay: Int): LevelKind = WEEKS[weekIndex(epochDay).mod(WEEKS.size)][weekday(epochDay)]

    /** Día de la semana de un día de época (0 = lunes). El día 0 (1-1-1970) fue jueves. */
    fun weekday(epochDay: Int): Int = ((epochDay + 3) % 7 + 7) % 7

    fun forDay(epochDay: Int): DailyConfig {
        // La semilla del tablero y la de la configuración salen del día, pero son independientes
        val seed = epochDay.toLong() * 7919L + 104729L
        val config = Random(seed xor 0x5DEECE66DL)
        val size = if (config.nextBoolean()) 4 else 5
        val target = if (config.nextBoolean()) 1024 else 2048
        val theme = config.nextInt(THEME_COUNT)
        val spec = specFor(kindFor(epochDay), size, target, config)
        return DailyConfig(epochDay, seed, size, target, theme, spec)
    }

    /** Reto del tipo [kind] sobre un tablero de [size] con la ficha de referencia [target] (1.024 o 2.048). */
    internal fun specFor(kind: LevelKind, size: Int, target: Int, random: Random): LevelSpec = when (kind) {
        LevelKind.STONES -> {
            val pattern = StonePatterns.pick(size, 2, random) ?: StonePatterns.pick(size, 1, random)!!
            val tile = 1 shl StonePatterns.exponentFor(LevelMath.log2(target), pattern)
            LevelBuilders.stones(1, tile, pattern)
        }
        LevelKind.SPRINT -> LevelBuilders.sprint(1, target / 2, size, slack = 1.5)
        LevelKind.FOURS -> LevelBuilders.fours(1, target, size)
        LevelKind.SCORE -> LevelBuilders.score(1, target / 4, size)
        LevelKind.TWINS -> LevelBuilders.twins(1, target / 4, size)
        LevelKind.HEADSTART -> LevelBuilders.headStart(1, if (size == 4) 512 else target, size)
        LevelKind.HEAVY -> LevelBuilders.heavy(1, if (size == 4) 256 else target / 2, size)
        LevelKind.LADDER -> if (size == 4) LevelBuilders.ladder(1, 256, 4, 4) else LevelBuilders.ladder(1, 512, 5, 5)
        LevelKind.HARVEST -> if (size == 4) LevelBuilders.harvest(1, 64, 8, 4) else LevelBuilders.harvest(1, 64, 14, 5)
        LevelKind.DOUBLE -> LevelBuilders.doubleDrop(1, if (size == 4) 256 else 512, size)
        LevelKind.STORM -> LevelBuilders.storm(1, if (size == 4) 256 else 512, size, Storm(everyMoves = 5, lifeMoves = 5, maxStones = 3))
        LevelKind.MARATHON -> LevelBuilders.marathon(1, if (size == 4) 200 else 260, size, slack = 1.2)
        LevelKind.COMBO -> LevelBuilders.combo(1, 3, 2, size, slack = 2.2)
        LevelKind.TWIST -> {
            val twist = listOf(Twist.MIRROR_H, Twist.MIRROR_V, Twist.NO_UP, Twist.NO_DOWN, Twist.NO_LEFT, Twist.NO_RIGHT, Twist.FLIP, Twist.SPIN).random(random)
            LevelBuilders.twisted(LevelBuilders.zen(1, 256, size), twist)
        }
        // En 4×4 el 2048 es demasiado para un reto de un día (ni un bot de pruebas lo gana): se queda en 1.024
        else -> LevelBuilders.zen(1, if (size == 4) minOf(target, 1024) else target, size)
    }
}
