package com.korkoor.pardos.domain.session

import com.korkoor.pardos.domain.level.GoalStats
import com.korkoor.pardos.domain.level.LevelCatalog
import com.korkoor.pardos.domain.level.LevelGoal
import com.korkoor.pardos.domain.level.LevelKind
import com.korkoor.pardos.domain.level.LevelRules
import com.korkoor.pardos.domain.level.LevelSpec
import com.korkoor.pardos.domain.level.SpawnRules
import com.korkoor.pardos.domain.level.SpawnStyle
import com.korkoor.pardos.domain.level.Storm
import com.korkoor.pardos.domain.level.StormRules
import com.korkoor.pardos.domain.level.StormStone
import com.korkoor.pardos.domain.level.Twist
import com.korkoor.pardos.domain.level.goalText
import com.korkoor.pardos.domain.flow.FlowMeter
import com.korkoor.pardos.domain.flow.FlowTier
import com.korkoor.pardos.domain.logic.CoachStep
import com.korkoor.pardos.domain.logic.DailyChallenge
import com.korkoor.pardos.domain.logic.Direction
import com.korkoor.pardos.domain.logic.GameEngine
import com.korkoor.pardos.domain.logic.MoveAdvisor
import com.korkoor.pardos.domain.logic.TutorialCoach
import com.korkoor.pardos.domain.model.TileModel
import kotlin.math.ln
import kotlin.random.Random

/**
 * Una partida de campaña completa sin interfaz: motor, reglas del nivel, tormentas, giros de control, fases de jefe, metas,
 * estrellas, deshacer, pistas y tutorial. La usa la app de iOS (SwiftUI solo dibuja) y tiene pruebas, así que lo que juega
 * iOS es lo mismo que está probado. Todo lo que cruza a Swift son enteros y texto (JSON): así no depende de cómo Kotlin/Native
 * traduzca listas o pares.
 *
 * Direcciones: 0 = arriba, 1 = abajo, 2 = izquierda, 3 = derecha.
 */
class GameSession(seed: Long) {
    constructor() : this(Random.nextLong())

    private var rng: Random = Random(seed)
    private var spec: LevelSpec = LevelCatalog.spec(1)
    private var engine = GameEngine(3, rng)
    private var tiles: List<TileModel> = emptyList()
    private var score = 0
    private var moves = 0
    private var stats = GoalStats()
    private var storm: Storm? = null
    private var stormStones: List<StormStone> = emptyList()
    private var twist: Twist = Twist.NONE
    private var spawn: SpawnStyle = SpawnStyle.NORMAL
    private var phase = 1
    private var phaseTitle = ""
    private var status = PLAYING
    private var lostReason = ""
    private var stars = 0
    private var timeLeftMs: Long? = null
    private var elapsedMs = 0L
    private var started = false
    private var blockedMessage = ""
    private var undo: Snapshot? = null
    private var daily = false
    private var mergePairs = 0
    private var peakTile = 0
    private var extraTimeMs = 0L
    private var comboTimeBonus = true
    private var flowStreak = 0
    private var reviveUsed = false
    private var peakFlowStreak = 0
    private var flowCallout = ""
    private var powersAllowed = true
    private var customLabel = ""
    private var assistPercent = 100

    /** Si es `true`, el nivel 1 enseña con el guion del tutorial (la app lo apaga cuando ya se completó). */
    var tutorialEnabled: Boolean = false

    private class Snapshot(
        val tiles: List<TileModel>, val score: Int, val moves: Int, val stats: GoalStats,
        val stormStones: List<StormStone>, val timeLeftMs: Long?
    )

    // ------------------------------------------------------------------ catálogo

    fun levelCount(): Int = LevelCatalog.TOTAL_LEVELS

    /** Datos de un nivel para el mapa (sin empezarlo). */
    fun levelInfo(level: Int): String = cardJson(LevelCatalog.spec(level))

    /** Tarjeta del reto de un día (días desde 1970): cada día de la semana trae un tipo de reto distinto. */
    fun dailyInfo(epochDay: Int): String = cardJson(DailyChallenge.forDay(epochDay).spec)

    private fun cardJson(s: LevelSpec): String {
        return obj(
            "id" to s.id, "title" to s.title, "kind" to s.kind.name, "kindLabel" to s.kind.label, "rule" to s.kind.rule,
            "goal" to s.goalText(), "size" to s.boardSize, "boss" to s.isBoss,
            "moveLimit" to s.moveLimit, "timeMs" to s.timeLimitMs, "chips" to s.ruleChips(), "tip" to s.tip,
            "threeStars" to LevelRules.threeStarHint(s)
        )
    }

    // ------------------------------------------------------------------ partida

    fun start(level: Int) {
        startAssisted(level, 100)
    }

    /**
     * Empieza un nivel con la ayuda por intentos fallidos: [percent] (100 = sin ayuda) agranda el límite de movimientos y el
     * reloj, como la `AssistPolicy` de Android.
     */
    fun startAssisted(level: Int, percent: Int) {
        daily = false
        assistPercent = percent.coerceIn(100, 200)
        val base = LevelCatalog.spec(level)
        begin(if (assistPercent == 100) base else base.copy(
            moveLimit = base.moveLimit?.let { it * assistPercent / 100 },
            timeLimitMs = base.timeLimitMs?.let { it * assistPercent / 100 }
        ))
        comboTimeBonus = true
        powersAllowed = true
        customLabel = ""
    }

    /**
     * Partida suelta con tablero y meta a elección (Carrera, Duelo, Torre sin piedras, Personalizado). [timeLimitMs] = 0 no
     * pone reloj; [seed] = 0 usa una semilla al azar. [comboBonus] activa los segundos extra por combinación (solo contrarreloj).
     */
    fun startCustom(size: Int, target: Int, timeLimitMs: Long, seed: Long, levelNumber: Int, label: String, comboBonus: Boolean, powers: Boolean) {
        daily = false
        assistPercent = 100
        if (seed != 0L) rng = Random(seed)
        val limit = if (timeLimitMs > 0L) timeLimitMs else null
        begin(LevelSpec(levelNumber, LevelKind.ZEN, label.ifEmpty { "Partida libre" }, size.coerceIn(3, 6), goalValue = target, timeLimitMs = limit))
        comboTimeBonus = comboBonus
        powersAllowed = powers
        customLabel = label
    }

    /** Empieza el reto de un día (días desde 1970): el mismo tablero y las mismas fichas para todo el mundo ese día. */
    fun startDaily(epochDay: Int) {
        val config = DailyChallenge.forDay(epochDay)
        daily = true
        rng = Random(config.seed)
        assistPercent = 100
        begin(config.spec)
        comboTimeBonus = true
        powersAllowed = false
        customLabel = ""

    }

    private fun begin(level: LevelSpec) {
        spec = level
        twist = spec.twist
        storm = spec.storm
        spawn = spec.spawn
        phase = 1
        phaseTitle = ""
        score = 0
        moves = 0
        stats = GoalStats()
        stormStones = emptyList()
        status = PLAYING
        lostReason = ""
        stars = 0
        timeLeftMs = spec.timeLimitMs
        elapsedMs = 0L
        started = false
        blockedMessage = ""
        undo = null
        mergePairs = 0
        peakTile = 0
        extraTimeMs = 0L
        flowStreak = 0
        peakFlowStreak = 0
        flowCallout = ""
        reviveUsed = false
        engine = GameEngine(spec.boardSize, rng, spec.stoneSet)
        tiles = initialTiles()
    }

    private fun initialTiles(): List<TileModel> {
        if (spec.startTiles.isNotEmpty()) {
            return spec.startTiles.map { TileModel(TileModel.generateId(), it.value, it.row, it.col, isNew = true) }
        }
        val count = when {
            spec.boardSize >= 5 -> 4
            spec.boardSize == 4 -> 3
            else -> 2
        }
        val list = ArrayList<TileModel>()
        repeat(count) {
            engine.spawnTileWithSpecificValue(list, SpawnRules.pick(spawn, spec.scaleTile, rng), 1)?.let { list.add(it) }
        }
        return list
    }

    private fun blockedNow(): Set<Pair<Int, Int>> = StormRules.blockedNow(spec.stones, stormStones)

    /** Desliza. Devuelve `true` si el tablero cambió (y se gastó una jugada). */
    fun move(direction: Int): Boolean {
        if (status != PLAYING) return false
        val swipe = directionOf(direction)
        blockedMessage = ""
        if (!twist.allows(swipe)) {
            blockedMessage = twist.label
            return false
        }
        val before = tiles
        val (moved, gained) = engine.move(before, twist.apply(swipe), 1)
        if (!changed(before, moved)) return false

        undo = Snapshot(before, score, moves, stats, stormStones, timeLeftMs)
        started = true
        val pairs = (before.size - moved.size).coerceAtLeast(0)

        // Fichas nuevas (1 o 2 por jugada) y la evolución espontánea de siempre
        val finalTiles = moved.toMutableList()
        repeat(spec.dropsPerMove) {
            engine.spawnTileWithSpecificValue(finalTiles, SpawnRules.pick(spawn, spec.scaleTile, rng), 1)?.let { finalTiles.add(it) }
        }
        if (rng.nextInt(1, 101) <= 15) {
            val candidates = finalTiles.filter { it.value == 4 || it.value == 8 || it.value == 16 }
            if (candidates.isNotEmpty()) {
                val lucky = candidates[rng.nextInt(candidates.size)]
                val i = finalTiles.indexOf(lucky)
                if (i != -1) finalTiles[i] = lucky.copy(value = lucky.value * 2)
            }
        }

        score += gained
        moves += 1
        val harvested = if (spec.goal == LevelGoal.HARVEST) moved.count { it.isMerged && it.value == spec.goalValue } else 0
        stats = stats.after(pairs, if (spec.goal == LevelGoal.COMBO) spec.goalValue else 0, harvested)
        tiles = finalTiles
        mergePairs += pairs
        val flowBefore = flowStreak
        flowStreak = FlowMeter.next(flowStreak, pairs > 0)
        peakFlowStreak = maxOf(peakFlowStreak, flowStreak)
        flowCallout = FlowMeter.tierUp(flowBefore, flowStreak)?.callout ?: ""
        peakTile = maxOf(peakTile, finalTiles.maxOfOrNull { it.value } ?: 0)

        // El reloj de los niveles contrarreloj regala segundos con las combinaciones
        timeLeftMs?.let { left ->
            val bonus = if (!comboTimeBonus) 0L else when {
                pairs >= 4 -> 10_000L
                pairs >= 3 -> 7_000L
                pairs >= 2 -> 4_000L
                else -> 0L
            }
            timeLeftMs = (left + bonus).coerceAtMost((spec.timeLimitMs ?: left) + extraTimeMs)
        }

        val reached = LevelRules.isGoalReached(spec, tiles, score, stats)
        if (reached) {
            win()
            return true
        }

        // Tormenta: se van piedras temporales y cae otra nueva
        storm?.let { st ->
            stormStones = StormRules.step(st, moves, stormStones, tiles, spec.stoneSet, spec.boardSize, rng)
            engine = GameEngine(spec.boardSize, rng, blockedNow())
        }
        maybeStartPhase2()

        when {
            spec.moveLimit?.let { moves >= it } == true -> lose(OUT_OF_MOVES)
            engine.isGameOver(tiles) -> lose(BOARD_FULL)
        }
        return true
    }

    private fun maybeStartPhase2() {
        val ph = spec.phase2 ?: return
        if (phase >= 2 || moves < ph.atMoves) return
        phase = 2
        phaseTitle = ph.title
        ph.twist?.let { twist = it }
        ph.storm?.let { storm = it }
        ph.spawn?.let { spawn = it }
    }

    private fun win() {
        status = WON
        val used = spec.timeLimitMs?.let { limit -> (limit + extraTimeMs - (timeLeftMs ?: limit)).coerceAtLeast(0L) } ?: elapsedMs
        stars = LevelRules.stars(spec, moves, used)
    }

    private fun lose(reason: String) {
        status = LOST
        lostReason = reason
    }

    /** Vuelve a la jugada anterior (una sola, y solo mientras se juega). */
    fun undoMove(): Boolean {
        val snap = undo ?: return false
        if (status != PLAYING) return false
        tiles = snap.tiles; score = snap.score; moves = snap.moves; stats = snap.stats
        stormStones = snap.stormStones; timeLeftMs = snap.timeLeftMs
        engine = GameEngine(spec.boardSize, rng, blockedNow())
        undo = null
        return true
    }

    /** "Tiempo extra": regala milisegundos al reloj de un nivel con reloj. Devuelve `false` si no hay reloj o ya terminó. */
    fun addExtraTime(ms: Int): Boolean {
        val left = timeLeftMs ?: return false
        if (status != PLAYING || ms <= 0) return false
        timeLeftMs = left + ms
        extraTimeMs += ms
        return true
    }

    /** ¿Se puede ofrecer seguir jugando tras perder? Una vez por partida y no en retos diarios. */
    private fun canRevive(): Boolean = status == LOST && !reviveUsed && !daily && powersAllowed

    /**
     * Segunda oportunidad (una por partida): si el tablero se llenó, se queda la mitad de las fichas más grandes; si se acabaron
     * los movimientos, se regalan unos cuantos más; si se acabó el tiempo, +30 segundos. Como la de Android.
     */
    fun revive(): Boolean {
        if (!canRevive()) return false
        reviveUsed = true
        when (lostReason) {
            BOARD_FULL -> {
                val keep = (tiles.size / 2).coerceAtLeast(2)
                tiles = tiles.sortedByDescending { it.value }.take(keep).map { it.copy(isNew = false, isMerged = false) }
            }
            OUT_OF_MOVES -> {
                val limit = spec.moveLimit ?: moves
                val extra = (limit * 15 / 100).coerceAtLeast(8)
                spec = spec.copy(moveLimit = moves + extra)
            }
            TIME_UP -> {
                timeLeftMs = 30_000L
                extraTimeMs += 30_000L
            }
        }
        status = PLAYING
        lostReason = ""
        undo = null
        blockedMessage = ""
        engine = GameEngine(spec.boardSize, rng, blockedNow())
        return true
    }

    /** Reloj: la app avisa cada tanto de cuántos milisegundos pasaron (solo cuenta tras la primera jugada). */
    fun tick(deltaMs: Int) {
        if (status != PLAYING || !started) return
        elapsedMs += deltaMs
        val left = timeLeftMs ?: return
        val next = left - deltaMs
        if (next <= 0L) {
            timeLeftMs = 0L
            lose(TIME_UP)
        } else timeLeftMs = next
    }


    // ------------------------------------------------------------------ poderes

    /** Limpiar: se queda con las 3 fichas más grandes (como el poder "Limpiar" de Android). */
    fun powerClean(): Boolean {
        if (!powersAllowed || status != PLAYING || tiles.size <= 3) return false
        undo = null
        tiles = tiles.sortedByDescending { it.value }.take(3).map { it.copy(isNew = false, isMerged = false) }
        started = true
        afterPower()
        return true
    }

    /** Fusión: junta al azar una pareja de fichas iguales (la primera que encuentra). */
    fun powerMerge(): Boolean {
        if (!powersAllowed || status != PLAYING) return false
        val pair = tiles.groupBy { it.value }.values.firstOrNull { it.size >= 2 } ?: return false
        return mergeInternal(pair[0], pair[1])
    }

    /** Escoba: quita la ficha elegida. */
    fun powerBroom(tileId: String): Boolean {
        if (!powersAllowed || status != PLAYING) return false
        if (tiles.none { it.id == tileId }) return false
        undo = null
        tiles = tiles.filter { it.id != tileId }.map { it.copy(isNew = false, isMerged = false) }
        started = true
        afterPower()
        return true
    }

    /** Unir: fusiona las dos fichas elegidas si valen lo mismo. */
    fun powerLink(firstId: String, secondId: String): Boolean {
        if (!powersAllowed || status != PLAYING || firstId == secondId) return false
        val a = tiles.firstOrNull { it.id == firstId } ?: return false
        val b = tiles.firstOrNull { it.id == secondId } ?: return false
        if (a.value != b.value) return false
        return mergeInternal(a, b)
    }

    private fun mergeInternal(a: TileModel, b: TileModel): Boolean {
        undo = null
        val value = b.value * 2
        tiles = tiles.filter { it.id != a.id && it.id != b.id }.map { it.copy(isNew = false, isMerged = false) } +
            b.copy(value = value, isMerged = true, isNew = false)
        score += value
        mergePairs += 1
        peakTile = maxOf(peakTile, value)
        started = true
        afterPower()
        return true
    }

    private fun afterPower() {
        if (LevelRules.isGoalReached(spec, tiles, score, stats)) {
            win()
            return
        }
        if (engine.isGameOver(tiles)) lose(BOARD_FULL)
    }

    // ------------------------------------------------------------------ pistas y tutorial

    /** Hacia dónde deslizar para la jugada recomendada (teniendo en cuenta los giros de control). */
    private fun adviceSwipe(): Pair<Int, Set<Pair<Int, Int>>>? {
        val advice = MoveAdvisor.suggest(spec.boardSize, blockedNow(), tiles) ?: return null
        val swipe = Direction.entries.firstOrNull { twist.allows(it) && twist.apply(it) == advice.direction } ?: return null
        return directionInt(swipe) to advice.mergeCells
    }

    /** Pista suelta (para cuando el jugador se queda parado): `{"dir":3,"cells":[[0,1],[0,2]]}` o texto vacío. */
    fun hint(): String {
        if (status != PLAYING) return ""
        val (dir, cells) = adviceSwipe() ?: return ""
        return obj("dir" to dir, "cells" to cells.map { listOf(it.first, it.second) })
    }

    private fun coachStep(): CoachStep? {
        if (!tutorialEnabled || daily || spec.id != 1 || status != PLAYING) return null
        return TutorialCoach.step(spec.boardSize, blockedNow(), tiles, moves, stats.merges, goalText(spec.goal, spec.goalValue, spec.goalCount))
    }

    // ------------------------------------------------------------------ estado para la interfaz

    fun snapshot(): String {
        val coach = coachStep()
        val coachDir = coach?.direction?.let { d -> Direction.entries.firstOrNull { twist.allows(it) && twist.apply(it) == d } }
        val tutorialDone = tutorialEnabled && !daily && spec.id == 1 && TutorialCoach.isFinished(moves, stats.merges)
        val limit = spec.moveLimit
        return obj(
            "level" to spec.id, "daily" to daily, "title" to spec.title, "kind" to spec.kind.name, "kindLabel" to spec.kind.label,
            "rule" to spec.kind.rule, "tip" to spec.tip, "goal" to goalText(spec.goal, spec.goalValue, spec.goalCount),
            "size" to spec.boardSize, "progress" to progress(),
            "score" to score, "moves" to moves, "movesLeft" to limit?.let { (it - moves).coerceAtLeast(0) },
            "timeLeftMs" to timeLeftMs, "status" to status, "lostReason" to lostReason, "stars" to stars,
            "tiles" to tiles.map { t ->
                obj("id" to t.id, "v" to t.value, "r" to t.row, "c" to t.col, "new" to t.isNew, "merged" to t.isMerged)
            }.let { Raw("[" + it.joinToString(",") + "]") },
            "stones" to spec.stones.map { listOf(it.first, it.second) },
            "storm" to stormStones.map { s -> listOf(s.cell.first, s.cell.second, (storm?.lifeMoves ?: 0) - (moves - s.born)) },
            "twist" to twist.label, "twistHint" to twist.hint, "blocked" to blockedMessage,
            "phase" to phase, "phaseTitle" to phaseTitle,
            "chips" to spec.ruleChips(),
            "coach" to coach?.text, "coachKind" to coach?.kind?.name,
            "coachDir" to coachDir?.let { directionInt(it) }, "coachCells" to coach?.cells?.map { listOf(it.first, it.second) },
            "coachDone" to coach?.mergesDone, "coachNeeded" to coach?.mergesNeeded,
            "tutorialDone" to tutorialDone, "canUndo" to (undo != null && status == PLAYING),
            "merges" to mergePairs, "maxTile" to peakTile, "elapsedMs" to elapsedMs,
            "powers" to powersAllowed, "label" to customLabel, "assist" to assistPercent,
            "canRevive" to canRevive(), "combo" to peakFlowStreak, "flow" to FlowMeter.tierOf(peakFlowStreak).ordinal, "callout" to flowCallout,
            "empty" to (spec.freeCells - tiles.size).coerceAtLeast(0), "stuck" to engine.isGameOver(tiles)
        )
    }

    /** Avance hacia la meta de 0 a 1 (la ficha mayor en escala logarítmica, o lo que mida cada tipo de meta). */
    private fun progress(): Double {
        if (status == WON) return 1.0
        return if (spec.goal == LevelGoal.REACH_TILE) {
            if (spec.goalValue <= 2) return 0.0
            val need = spec.goalCount.coerceAtLeast(1)
            val best = tiles.map { it.value }.sortedDescending().take(need)
            if (best.isEmpty()) return 0.0
            val total = best.sumOf { v -> if (v <= 1) 0.0 else (ln(v.toDouble()) / ln(spec.goalValue.toDouble())).coerceIn(0.0, 1.0) }
            total / need
        } else {
            LevelRules.progress(spec.goal, spec.goalValue, spec.goalCount, tiles, score, stats).toDouble()
        }
    }

    // ------------------------------------------------------------------ utilidades

    private fun changed(a: List<TileModel>, b: List<TileModel>): Boolean {
        if (a.size != b.size) return true
        val m = a.associate { (it.row to it.col) to it.value }
        return b.any { m[it.row to it.col] != it.value }
    }

    companion object {
        const val PLAYING = "playing"
        const val WON = "won"
        const val LOST = "lost"
        const val BOARD_FULL = "board_full"
        const val OUT_OF_MOVES = "out_of_moves"
        const val TIME_UP = "time_up"

        fun directionOf(i: Int): Direction = when (i) {
            0 -> Direction.UP
            1 -> Direction.DOWN
            2 -> Direction.LEFT
            else -> Direction.RIGHT
        }

        fun directionInt(d: Direction): Int = when (d) {
            Direction.UP -> 0
            Direction.DOWN -> 1
            Direction.LEFT -> 2
            Direction.RIGHT -> 3
        }
    }
}

// ---------------------------------------------------------------------- JSON mínimo (sin librerías)

/** Texto que ya es JSON y no hay que volver a escapar. */
private class Raw(val json: String)

private fun obj(vararg pairs: Pair<String, Any?>): String =
    pairs.joinToString(",", "{", "}") { (k, v) -> "\"$k\":${value(v)}" }

private fun value(v: Any?): String = when (v) {
    null -> "null"
    is Raw -> v.json
    is Boolean, is Int, is Long -> v.toString()
    is Double -> if (v.isNaN() || v.isInfinite()) "0" else v.toString()
    is String -> quote(v)
    is Iterable<*> -> v.joinToString(",", "[", "]") { value(it) }
    else -> quote(v.toString())
}

private fun quote(s: String): String {
    val sb = StringBuilder("\"")
    for (ch in s) {
        when {
            ch == '"' -> sb.append("\\\"")
            ch == '\\' -> sb.append("\\\\")
            ch == '\n' -> sb.append("\\n")
            ch == '\r' -> sb.append("\\r")
            ch == '\t' -> sb.append("\\t")
            ch.code < 0x20 -> sb.append("\\u").append(ch.code.toString(16).padStart(4, '0'))
            else -> sb.append(ch)
        }
    }
    return sb.append('"').toString()
}
