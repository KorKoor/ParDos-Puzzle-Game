package com.korkoor.pardos.domain.level

import com.korkoor.pardos.domain.level.LevelBuilders.boss
import com.korkoor.pardos.domain.level.LevelBuilders.clock
import com.korkoor.pardos.domain.level.LevelBuilders.combo
import com.korkoor.pardos.domain.level.LevelBuilders.fours
import com.korkoor.pardos.domain.level.LevelBuilders.doubleDrop
import com.korkoor.pardos.domain.level.LevelBuilders.harvest
import com.korkoor.pardos.domain.level.LevelBuilders.headStart
import com.korkoor.pardos.domain.level.LevelBuilders.heavy
import com.korkoor.pardos.domain.level.LevelBuilders.ladder
import com.korkoor.pardos.domain.level.LevelBuilders.marathon
import com.korkoor.pardos.domain.level.LevelBuilders.score
import com.korkoor.pardos.domain.level.LevelBuilders.sprint
import com.korkoor.pardos.domain.level.LevelBuilders.stones
import com.korkoor.pardos.domain.level.LevelBuilders.storm
import com.korkoor.pardos.domain.level.LevelBuilders.twins
import com.korkoor.pardos.domain.level.LevelBuilders.twisted
import com.korkoor.pardos.domain.level.LevelBuilders.zen
import kotlin.random.Random

/**
 * Catálogo de la campaña. `spec(n)` da el nivel n:
 *  1. si está en [HandmadeLevels], ese (los niveles diseñados a mano mandan);
 *  2. si no, lo genera [LevelGenerator] de forma determinista: el mismo número siempre da el mismo nivel.
 *
 * Para alargar la campaña basta subir [TOTAL_LEVELS]; para cambiar un nivel, escribirlo en `HandmadeLevels`.
 */
object LevelCatalog {
    const val TOTAL_LEVELS = 2400

    /** Debe coincidir con `ChapterRewards.LEVELS_PER_CHAPTER` (un cofre cada capítulo). */
    const val LEVELS_PER_CHAPTER = 20

    fun spec(level: Int): LevelSpec {
        val id = level.coerceAtLeast(1)
        return HandmadeLevels.get(id) ?: LevelGenerator.build(id)
    }

    fun all(count: Int = TOTAL_LEVELS): List<LevelSpec> = (1..count).map(::spec)
}

/**
 * Receta del generador. Cada capítulo (20 niveles) sigue un guion:
 *  - nivel 1: Zen de bienvenida · niveles 10 y 20: jefes · niveles 5 y 15: tipo "especial" (nunca Zen);
 *  - el resto se sortea con pesos, sin repetir el tipo anterior y penalizando los que ya salieron en el capítulo;
 *  - dos tipos exigentes nunca van seguidos, y nunca justo antes de un jefe.
 *
 * Calendario de novedades (capítulo = 20 niveles; una persona que juega 3-4 horas al día avanza unos 2 capítulos diarios):
 *  - cada tipo de nivel se suelta en su capítulo (ver [UNLOCK]) y en ese capítulo se debuta fácil y con peso extra;
 *  - los giros de controles van entrando uno a uno (ver [twistsFor]);
 *  - a partir del capítulo 30 aparecen niveles de doble regla;
 *  - la dificultad sube despacio durante toda la campaña (holguras de [sprintSlack] y [clockSeconds], piedras más duras).
 */
internal object LevelGenerator {
    private const val PER = LevelCatalog.LEVELS_PER_CHAPTER

    fun chapterOf(level: Int): Int = (level - 1) / PER
    fun positionOf(level: Int): Int = (level - 1) % PER + 1

    /** Capítulo en el que cada tipo entra en la campaña (los que no aparecen empiezan en el 0). */
    val UNLOCK: Map<LevelKind, Int> = mapOf(
        LevelKind.TWINS to 2, LevelKind.CLOCK to 2,
        LevelKind.HEAVY to 3, LevelKind.LADDER to 6, LevelKind.HARVEST to 8, LevelKind.TWIST to 10,
        LevelKind.MARATHON to 14, LevelKind.DOUBLE to 17, LevelKind.STORM to 20, LevelKind.COMBO to 26
    )

    fun unlockOf(kind: LevelKind): Int = UNLOCK[kind] ?: 0

    /** Ficha base según el avance: sube hasta 1024 y ahí se queda; la dificultad pasa a venir de las reglas. */
    fun baseExp(level: Int): Int = when {
        level <= 2 -> 5
        level <= 5 -> 6
        level <= 10 -> 7
        level <= 18 -> 8
        level <= 28 -> 9
        else -> 10
    }

    private fun tileOf(exp: Int) = 1 shl exp

    // ---------------------------------------------------------------- guion del capítulo

    /** Pesos de sorteo según el capítulo: solo entran los tipos ya soltados y los recién llegados pesan más unos capítulos. */
    private fun weights(chapter: Int): Map<LevelKind, Double> {
        val base = mapOf(
            LevelKind.ZEN to (24.0 - (chapter / 4.0).coerceAtMost(10.0)),
            LevelKind.SCORE to 14.0, LevelKind.FOURS to 12.0, LevelKind.HEADSTART to 12.0, LevelKind.STONES to 16.0,
            LevelKind.SPRINT to 10.0, LevelKind.TWINS to 7.0, LevelKind.CLOCK to 7.0,
            LevelKind.HEAVY to 9.0, LevelKind.LADDER to 9.0, LevelKind.TWIST to 9.0,
            LevelKind.MARATHON to 8.0, LevelKind.STORM to 10.0, LevelKind.COMBO to 10.0,
            LevelKind.HARVEST to 9.0, LevelKind.DOUBLE to 8.0
        )
        return base.filterKeys { chapter >= unlockOf(it) }.mapValues { (k, w) ->
            val age = chapter - unlockOf(k)
            if (unlockOf(k) > 0 && age in 0..2) w * 1.8 else w
        }
    }

    private val specialKinds = listOf(
        LevelKind.STONES, LevelKind.SPRINT, LevelKind.TWINS, LevelKind.CLOCK, LevelKind.FOURS,
        LevelKind.HEAVY, LevelKind.LADDER, LevelKind.TWIST, LevelKind.MARATHON, LevelKind.STORM, LevelKind.COMBO,
        LevelKind.HARVEST, LevelKind.DOUBLE
    )

    internal fun plan(chapter: Int): List<LevelKind> {
        val rnd = Random(chapter * 104729L + 11)
        val w = weights(chapter)
        val plan = arrayOfNulls<LevelKind>(PER)
        plan[0] = LevelKind.ZEN
        plan[9] = LevelKind.BOSS
        plan[PER - 1] = LevelKind.BOSS
        plan[10] = listOf(LevelKind.HEADSTART, LevelKind.ZEN, LevelKind.SCORE).random(rnd)   // respiro tras el jefe pequeño

        // Los tipos que debutan este capítulo salen seguro, repartidos por el capítulo (el primero, en el nivel 5)
        val debuts = w.keys.filter { chapter > 0 && unlockOf(it) == chapter }.shuffled(rnd)
        listOf(4, 14, 7, 17).zip(debuts).forEach { (i, kind) -> plan[i] = kind }

        val used = HashMap<LevelKind, Int>()
        plan.filterNotNull().forEach { used[it] = (used[it] ?: 0) + 1 }

        for (i in 1 until PER - 1) {
            if (plan[i] != null) continue
            val prev = plan[i - 1]
            val next = plan[i + 1]
            val nextDemanding = next?.isDemanding == true
            val special = i == 4 || i == 14
            val candidates = w.keys.filter { k ->
                k != prev && k != next &&
                    !(special && k !in specialKinds) &&
                    !(k.isDemanding && (prev?.isDemanding == true || nextDemanding))
            }.ifEmpty { w.keys.filter { it != prev && !it.isDemanding } }
            val weighted = candidates.map { k -> k to (w.getValue(k) * decay(used[k] ?: 0)) }
            var roll = rnd.nextDouble() * weighted.sumOf { it.second }
            var chosen = weighted.last().first
            for ((k, weight) in weighted) {
                roll -= weight
                if (roll <= 0) { chosen = k; break }
            }
            plan[i] = chosen
            used[chosen] = (used[chosen] ?: 0) + 1
        }
        return plan.map { it!! }
    }

    private fun decay(times: Int): Double {
        var f = 1.0
        repeat(times) { f *= 0.35 }
        return f
    }

    // ---------------------------------------------------------------- construcción

    fun build(level: Int): LevelSpec {
        val chapter = chapterOf(level)
        val pos = positionOf(level)
        val kind = plan(chapter)[pos - 1]
        val rnd = Random(level * 7919L + 13)
        var exp = baseExp(level)
        if (pos == 1 || pos == 11) {
            // Respiros: el primer nivel de cada capítulo y el de después del jefe pequeño son un poco más cortos
            exp -= 1
        } else {
            // Duración variable: algunos niveles son más cortos y, pasado el 40, otros un poco más largos
            val r = rnd.nextDouble()
            if (r < 0.28 && exp >= 8) exp -= 1 else if (r > 0.82 && level > 40 && exp < MAX_EXP) exp += 1
        }
        // Un tipo en su capítulo de estreno se juega más suave
        val fresh = kind != LevelKind.BOSS && chapter == unlockOf(kind) && unlockOf(kind) > 0

        return when (kind) {
            LevelKind.ZEN -> plainVariant(level, chapter, kind, exp, rnd)
            LevelKind.SCORE -> plainVariant(level, chapter, kind, if (exp >= 9) exp - 1 else exp, rnd)
            LevelKind.FOURS -> plainVariant(level, chapter, kind, exp, rnd)
            LevelKind.HEADSTART -> {
                val wide = chapter >= 2 && rnd.nextDouble() < 0.2
                val size = if (wide) 5 else 4
                val pattern = if (chapter >= 2 && rnd.nextDouble() < 0.3) StonePatterns.pick(size, 1, rnd) else null
                val e = (exp + if (wide) 1 else 0).coerceAtMost(MAX_EXP)
                headStart(level, tileOf(pattern?.let { StonePatterns.exponentFor(e - 1, it) } ?: e), size, pattern)
            }
            LevelKind.STONES -> stonesLevel(level, chapter, exp, rnd)
            LevelKind.SPRINT -> {
                val wide = chapter >= 2 && rnd.nextDouble() < 0.2
                val size = if (wide) 5 else 4
                val pattern = if (chapter >= 1 && rnd.nextDouble() < 0.3) StonePatterns.pick(size, 1, rnd) else null
                val e = (exp - 1 + if (wide) 1 else 0).coerceIn(6, MAX_EXP)
                val slack = sprintSlack(chapter) + (rnd.nextInt(3) - 1) * 0.06
                sprint(level, tileOf(pattern?.let { StonePatterns.exponentFor(e - 1, it) } ?: e), size, slack = slack, stones = pattern)
            }
            LevelKind.TWINS -> {
                val wide = chapter >= 2 && rnd.nextDouble() < 0.2
                twins(level, tileOf((exp - 2 + if (wide) 1 else 0).coerceAtLeast(5)), if (wide) 5 else 4)
            }
            LevelKind.CLOCK -> {
                val pattern = if (chapter >= 3 && rnd.nextDouble() < 0.25) StonePatterns.pick(4, 1, rnd) else null
                val e = (exp - 1).coerceAtLeast(6)
                val seconds = clockSeconds(chapter) + (rnd.nextInt(3) - 1) * 0.1 + if (pattern != null) 0.25 else 0.0
                clock(level, tileOf(pattern?.let { StonePatterns.exponentFor(e - 1, it) } ?: e), 4, secondsPerMove = seconds, stones = pattern)
            }
            LevelKind.HEAVY -> heavyLevel(level, chapter, exp, rnd, fresh)
            LevelKind.LADDER -> ladderLevel(level, chapter, rnd, fresh)
            LevelKind.MARATHON -> marathonLevel(level, chapter, rnd, fresh)
            LevelKind.COMBO -> comboLevel(level, chapter, rnd, fresh)
            LevelKind.TWIST -> twistLevel(level, chapter, exp, rnd, fresh)
            LevelKind.STORM -> stormLevel(level, chapter, exp, rnd, fresh)
            LevelKind.BOSS -> bossLevel(level, chapter, pos, exp, rnd)
            LevelKind.HARVEST -> harvestLevel(level, chapter, exp, rnd, fresh)
            LevelKind.DOUBLE -> doubleLevel(level, chapter, exp, rnd, fresh)
        }
    }

    /** Ficha máxima (potencia de 2) que pide la campaña generada: 1.024. Las partidas más largas cansan. */
    private const val MAX_EXP = 10

    /** Holgura de los sprints: 1,72 en el capítulo 1 y baja hasta 1,3 hacia el capítulo 105 (los últimos tramos piden precisión). */
    fun sprintSlack(chapter: Int): Double = (1.72 - 0.004 * chapter).coerceAtLeast(1.3)
    fun clockSeconds(chapter: Int): Double = (1.9 - 0.0045 * chapter).coerceAtLeast(1.4)

    /** Zen, Puntos y Lluvia de 4 comparten variantes de tablero: normal, mini (3×3, rápido) y amplio (5×5). */
    private fun plainVariant(level: Int, chapter: Int, kind: LevelKind, exp: Int, rnd: Random): LevelSpec {
        val roll = if (chapter >= 1) rnd.nextDouble() else 1.0
        val (size, e, suffix) = when {
            roll < 0.18 -> Triple(3, 6, " mini")
            roll < 0.42 -> Triple(5, (exp + 1).coerceAtMost(MAX_EXP), " amplio")
            else -> Triple(4, exp, "")
        }
        val title = kind.label + suffix
        return when (kind) {
            LevelKind.SCORE -> score(level, tileOf(e), size, title)
            LevelKind.FOURS -> fours(level, tileOf(e), size, title)
            else -> zen(level, tileOf(e), size, title)
        }
    }

    /** Piedras: la gravedad permitida y la ficha de la meta suben con los capítulos y bajan con lo que estorban. */
    private fun stonesLevel(level: Int, chapter: Int, exp: Int, rnd: Random): LevelSpec {
        val maxSeverity = when {
            chapter <= 1 -> 1
            chapter <= 3 -> 2
            else -> 3
        }
        val giant = chapter >= 12 && rnd.nextDouble() < 0.12
        val wide = !giant && chapter >= 2 && rnd.nextDouble() < 0.3
        val size = if (giant) 6 else if (wide) 5 else 4
        val severity = 1 + rnd.nextInt(maxSeverity)
        val pattern = StonePatterns.pick(size, severity, rnd) ?: StonePatterns.pick(4, 1, rnd)!!
        return stones(level, tileOf(StonePatterns.exponentFor(exp + if (wide || giant) 1 else 0, pattern)), pattern)
    }

    // ---------------------------------------------------------------- tipos nuevos

    /** Pesadas: pocas fichas hacen falta, pero el tablero se llena enseguida. En 5×5 y 6×6 la meta sube. */
    private fun heavyLevel(level: Int, chapter: Int, exp: Int, rnd: Random, fresh: Boolean): LevelSpec {
        val size = when {
            chapter >= 16 && rnd.nextDouble() < 0.12 -> 6
            chapter >= 6 && rnd.nextDouble() < 0.35 -> 5
            else -> 4
        }
        val pattern = if (chapter >= 10 && rnd.nextDouble() < 0.3) StonePatterns.pick(size, if (chapter >= 24 && rnd.nextDouble() < 0.4) 2 else 1, rnd) else null
        var e = when (size) {
            4 -> (exp - 1).coerceIn(7, if (chapter >= 30) 9 else 8)
            5 -> exp.coerceIn(8, 10)
            else -> 11
        }
        if (pattern != null) e = StonePatterns.exponentFor(e - 2, pattern)
        if (fresh) e = (e - 1).coerceAtLeast(6)
        return heavy(level, tileOf(e), size, pattern)
    }

    /** Escalera: más peldaños y más altos según el capítulo; desde el 30, a veces con tope de movimientos. */
    private fun ladderLevel(level: Int, chapter: Int, rnd: Random, fresh: Boolean): LevelSpec {
        val steps = when {
            fresh -> 3
            chapter >= 45 -> 5
            chapter >= 18 -> 4
            else -> 3
        }
        val topExp = when (steps) {
            3 -> if (chapter >= 12) 8 else 7
            4 -> if (chapter >= 30) 9 else 8
            else -> if (chapter >= 70 && rnd.nextBoolean()) 10 else 9
        }
        val size = if (steps == 5 && topExp >= 10) 5 else if (steps >= 4 && rnd.nextDouble() < 0.2) 5 else 4
        val pattern = if (chapter >= 24 && size == 5 && topExp <= 9 && rnd.nextDouble() < 0.4) StonePatterns.pick(5, 1, rnd) else null
        val slack = if (chapter >= 30 && !fresh && rnd.nextBoolean()) 1.7 - (chapter - 30).coerceAtMost(60) * 0.003 else null
        return ladder(level, tileOf(topExp), steps, size, pattern, slack)
    }

    /** Maratón: las fusiones pedidas suben con los capítulos; más adelante se juega con piedras y fichas pesadas. */
    private fun marathonLevel(level: Int, chapter: Int, rnd: Random, fresh: Boolean): LevelSpec {
        // Con fichas pesadas el tablero de 4×4 no aguanta tantas jugadas: esas van en 5×5
        val heavy = chapter >= 34 && rnd.nextDouble() < 0.25
        val size = if (heavy || (chapter >= 18 && rnd.nextDouble() < 0.3)) 5 else 4
        val merges = ((70 + chapter * 4).coerceAtMost(if (size == 5) 300 else 220) - if (fresh) 40 else 0) / 10 * 10
        val severity = 1
        val pattern = if (!heavy && chapter >= 22 && rnd.nextDouble() < 0.45) StonePatterns.pick(size, severity, rnd) else null
        val spawn = if (heavy) SpawnStyle.HEAVY else SpawnStyle.NORMAL
        val slack = (if (fresh) 1.4 else 1.22 - 0.0015 * chapter).coerceAtLeast(1.1)
        return marathon(level, merges, size, slack, pattern, spawn)
    }

    /**
     * Combo: tres pares de golpe al principio; con los capítulos hay que repetirlo o buscar cuatro. La holgura es amplia
     * porque preparar el tablero lleva su tiempo.
     */
    private fun comboLevel(level: Int, chapter: Int, rnd: Random, fresh: Boolean): LevelSpec {
        val (pairs, times, size) = when {
            fresh || chapter < 34 -> Triple(3, 1, 4)
            chapter < 48 -> Triple(3, 2, if (rnd.nextBoolean()) 5 else 4)
            chapter < 70 -> if (rnd.nextDouble() < 0.5) Triple(4, 1, 5) else Triple(3, 3, 4)
            else -> if (rnd.nextDouble() < 0.5) Triple(4, 2, 5) else Triple(4, 3, 5)
        }
        val slack = when {
            pairs >= 4 -> 3.5
            times >= 2 -> 2.2
            else -> 2.0
        }
        return combo(level, pairs, times, size, slack)
    }

    /** Capítulo en el que entra cada giro de controles (los callejones prohíben una dirección). */
    fun twistArrival(twist: Twist): Int = when (twist) {
        Twist.NONE -> 0
        Twist.MIRROR_H -> 10
        Twist.NO_UP, Twist.NO_DOWN -> 13
        Twist.MIRROR_V -> 16
        Twist.NO_LEFT, Twist.NO_RIGHT -> 20
        Twist.FLIP -> 24
        Twist.SPIN -> 34
    }

    /** Controles girados: los giros nuevos entran uno a uno y, desde el 30, se mezclan con otras reglas. */
    fun twistsFor(chapter: Int): List<Twist> = Twist.entries.filter { it != Twist.NONE && chapter >= twistArrival(it) }

    private fun twistLevel(level: Int, chapter: Int, exp: Int, rnd: Random, fresh: Boolean): LevelSpec {
        val available = twistsFor(chapter)
        // Los giros que acaban de llegar salen más a menudo
        val arrivals = available.filter { twistArrival(it) == chapter }
        val twist = if (arrivals.isNotEmpty() && rnd.nextDouble() < 0.6) arrivals.random(rnd) else available.random(rnd)
        val e = (exp - if (twist == Twist.SPIN || twist == Twist.FLIP) 3 else 2).coerceIn(6, 8) - if (fresh) 1 else 0
        val base = when {
            chapter >= 30 && rnd.nextDouble() < 0.35 -> sprint(level, tileOf(e), 4, slack = sprintSlack(chapter) + 0.3)
            chapter >= 30 && rnd.nextDouble() < 0.25 -> clock(level, tileOf((e - 1).coerceAtLeast(6)), 4, secondsPerMove = clockSeconds(chapter) + 0.4)
            chapter >= 30 && rnd.nextDouble() < 0.2 -> StonePatterns.pick(4, 1, rnd)!!.let { stones(level, tileOf(StonePatterns.exponentFor(e, it)), it) }
            chapter >= 18 && rnd.nextDouble() < 0.3 -> score(level, tileOf(e), 4)
            else -> zen(level, tileOf(e), 4)
        }
        return twisted(base, twist)
    }

    /**
     * Cosecha: la cantidad se calcula para que la suma de fichas creadas sea parecida a la de un nivel normal de ese tramo
     * (más o menos 1,3 veces la ficha base) y la ficha cosechada sube con los capítulos.
     */
    private fun harvestLevel(level: Int, chapter: Int, exp: Int, rnd: Random, fresh: Boolean): LevelSpec {
        val size = if (chapter >= 14 && rnd.nextDouble() < 0.3) 5 else 4
        val values = when {
            chapter < 12 -> listOf(16, 32)
            chapter < 30 -> listOf(32, 64)
            else -> listOf(64, 128)
        }
        val value = values.random(rnd)
        val sum = (1.3 * tileOf(exp)).toInt() * (if (fresh) 5 else 10) / 10
        val maxCount = when (value) { 16 -> 30; 32 -> 22; 64 -> if (size == 5) 16 else 12; else -> if (size == 5) 10 else 6 }
        val count = (sum / value).coerceIn(4, maxCount)
        val pattern = if (size == 5 && chapter >= 20 && rnd.nextDouble() < 0.4) StonePatterns.pick(5, 1, rnd) else null
        return harvest(level, value, if (pattern != null) (count * 4 / 5).coerceAtLeast(4) else count, size, pattern)
    }

    /** Doble caída: en 4×4 la meta se queda en 256; en 5×5 puede subir hasta 1.024. */
    private fun doubleLevel(level: Int, chapter: Int, exp: Int, rnd: Random, fresh: Boolean): LevelSpec {
        val wide = chapter >= 10 && rnd.nextDouble() < 0.35
        val size = if (wide) 5 else 4
        var e = if (wide) exp.coerceIn(8, 10) else (exp - 1).coerceIn(6, 8)
        val pattern = if (wide && chapter >= 25 && rnd.nextDouble() < 0.3) StonePatterns.pick(5, 1, rnd) else null
        if (pattern != null) e = StonePatterns.exponentFor(e - 1, pattern)
        if (fresh) e = (e - 1).coerceAtLeast(6)
        return doubleDrop(level, tileOf(e), size, pattern)
    }

    /** Tormenta: cae una piedra cada vez más a menudo, dura algo más y puede haber más a la vez. */
    fun stormFor(chapter: Int, fresh: Boolean): Storm {
        if (fresh) return Storm(everyMoves = 8, lifeMoves = 5, maxStones = 2)
        val age = (chapter - 20).coerceAtLeast(0)
        return Storm(
            everyMoves = (7 - age / 12).coerceIn(3, 7),
            lifeMoves = (5 + age / 40).coerceIn(5, 6),
            maxStones = (2 + age / 25).coerceIn(2, 4)
        )
    }

    private fun stormLevel(level: Int, chapter: Int, exp: Int, rnd: Random, fresh: Boolean): LevelSpec {
        val e = (exp - 1).coerceIn(7, 9) - if (fresh) 1 else 0
        val pattern = if (chapter >= 36 && rnd.nextDouble() < 0.3) StonePatterns.pick(4, 1, rnd) else null
        val tile = tileOf(if (pattern != null) StonePatterns.exponentFor(e - 1, pattern) else e)
        return storm(level, tile, 4, stormFor(chapter, fresh), pattern)
    }

    // ---------------------------------------------------------------- jefes

    private data class Recipe(val name: String, val unlock: Int)

    private val recipes = listOf(
        Recipe("El Muro", 0), Recipe("Tormenta de 4", 0), Recipe("Doble o nada", 0), Recipe("La Cantera", 0),
        Recipe("Relojería", 0), Recipe("Duelo de puntos", 0), Recipe("Rompecabezas", 0),
        Recipe("Escalera real", 6), Recipe("Pesadilla", 3), Recipe("Espejismo", 10),
        Recipe("Ojo del huracán", 20), Recipe("Gran maratón", 14), Recipe("Reacción en cadena", 26),
        Recipe("Cosecha de oro", 8), Recipe("Lluvia de meteoros", 17), Recipe("Callejón sin salida", 13)
    )

    /** Nombres de todos los jefes (para las pruebas y la interfaz). */
    val bossNames: List<String> get() = recipes.map { it.name }

    private fun bossLevel(level: Int, chapter: Int, pos: Int, exp: Int, rnd: Random): LevelSpec {
        val isFinal = pos == PER
        val e = (exp - if (isFinal) 1 else 2).coerceIn(if (level > 60) 8 else 6, 10)   // el jefe pequeño es más corto que el final
        val available = recipes.withIndex().filter { chapter >= it.value.unlock }.map { it.index }
        val recipe = available[(chapter * 3 + if (isFinal) 2 else 0) % available.size]
        val corner = StonePatterns.pick(4, 1, rnd)!!
        val pillar = StonePatterns.all.filter { it.size == 4 && it.cells.size == 1 && it.severity == 2 }.random(rnd)
        val goal = tileOf(e)
        val name = recipes[recipe].name
        val base = bossBase(recipe, level, chapter, e, goal, corner, pillar, rnd, name)
        // El primer jefe de todos (capítulo 1) se queda de una sola fase; los demás cambian las reglas a media partida
        return if (chapter >= 1) withBossPhase(recipe, base, chapter) else base
    }

    /** Fase 2 de cada receta de jefe: una regla que entra a media partida. */
    private fun withBossPhase(recipe: Int, base: LevelSpec, chapter: Int): LevelSpec {
        val calmStorm = Storm(everyMoves = 9, lifeMoves = 5, maxStones = 2)
        val storm = if (chapter >= 20) stormFor(chapter, false).let { it.copy(everyMoves = (it.everyMoves + 2).coerceAtMost(8)) } else calmStorm
        val B = LevelBuilders
        // Con una segunda fase el jefe pide más: se compensa con un poco más de margen de movimientos
        @Suppress("NAME_SHADOWING")
        val base = base.copy(moveLimit = base.moveLimit?.let { (it * 1.12).toInt() })
        return when (recipe) {
            0, 3, 6, 9, 13 -> B.withPhase(base, "¡Cae una tormenta!", storm = storm)
            1, 5, 12 -> B.withPhase(base, "¡Llueven los 4!", spawn = SpawnStyle.FOURS)
            2, 10, 14 -> B.withPhase(base, "¡Controles cambiados!", twist = Twist.MIRROR_H)
            4 -> B.withPhase(base, "¡Una dirección se bloquea!", twist = Twist.NO_UP)
            7 -> B.withPhase(base, "¡Arriba y abajo se cambian!", twist = Twist.MIRROR_V)
            8 -> B.withPhase(base, "¡Una dirección se bloquea!", twist = Twist.NO_DOWN)
            11 -> B.withPhase(base, "¡Una dirección se bloquea!", twist = Twist.NO_LEFT)
            else -> B.withPhase(base, "¡Todo al revés!", twist = Twist.FLIP)
        }
    }

    private fun bossBase(
        recipe: Int, level: Int, chapter: Int, e: Int, goal: Int, corner: StonePattern, pillar: StonePattern, rnd: Random, name: String
    ): LevelSpec {
        return when (recipe) {
            0 -> boss(sprint(level, tileOf(StonePatterns.exponentFor(if (chapter >= 4) e else e - 1, pillar)), 4, slack = sprintSlack(chapter) + 0.1, stones = pillar), name)
            1 -> boss(fours(level, goal, 4).let { it.copy(timeLimitMs = LevelMath.timeLimitMs(it, clockSeconds(chapter) + 0.6)) }, name)
            2 -> boss(twins(level, tileOf(e - 1), 4).let { it.copy(moveLimit = LevelMath.moveLimit(it, sprintSlack(chapter) + 0.15)) }, name)
            3 -> {
                val big = StonePatterns.pick(5, 2, rnd)!!
                boss(twins(level, tileOf(e - 1), 5).copy(stones = big.cells), name)
            }
            4 -> boss(clock(level, tileOf(e - 1), 4, secondsPerMove = clockSeconds(chapter) + 0.2, stones = corner), name)
            5 -> boss(score(level, tileOf(e - 1), 4).let { it.copy(moveLimit = LevelMath.moveLimit(it, sprintSlack(chapter) + 0.1)) }, name)
            6 -> {
                val h = headStart(level, tileOf(StonePatterns.exponentFor(e - 1, corner)), 4, corner)
                boss(h.copy(moveLimit = LevelMath.moveLimit(h, sprintSlack(chapter) + 0.1)), name)
            }
            7 -> boss(ladder(level, tileOf(if (chapter >= 18) 9 else 8), if (chapter >= 18) 4 else 3, 4, null, slack = 1.55), name)
            8 -> boss(heavy(level, tileOf((e - 2).coerceIn(7, 8)), 4, StonePatterns.pick(4, 1, rnd)).let { it.copy(moveLimit = LevelMath.moveLimit(it, 2.3)) }, name)
            9 -> boss(twisted(sprint(level, tileOf((e - 2).coerceAtLeast(6)), 4, slack = sprintSlack(chapter) + 0.35), twistsFor(chapter).random(rnd)), name)
            10 -> boss(storm(level, tileOf((e - 1).coerceIn(7, 9)), 4, stormFor(chapter, false).let { it.copy(everyMoves = (it.everyMoves - 1).coerceAtLeast(3)) }, corner), name)
            11 -> boss(marathon(level, ((130 + chapter * 2).coerceAtMost(260)) / 10 * 10, 5, 1.2, StonePatterns.pick(5, 1, rnd)), name)
            12 -> boss(combo(level, 3, 2, if (chapter >= 40) 5 else 4, 2.4), name)
            13 -> boss(harvest(level, 32, (24 + chapter / 4).coerceAtMost(40), 5, StonePatterns.pick(5, 1, rnd)), name)
            14 -> boss(doubleDrop(level, tileOf((e - 1).coerceIn(7, 9)), 5, StonePatterns.pick(5, 1, rnd)), name)
            else -> boss(twisted(zen(level, tileOf((e - 2).coerceIn(6, 8)), 4), listOf(Twist.NO_UP, Twist.NO_DOWN, Twist.NO_LEFT, Twist.NO_RIGHT).random(rnd)), name)
        }
    }
}
