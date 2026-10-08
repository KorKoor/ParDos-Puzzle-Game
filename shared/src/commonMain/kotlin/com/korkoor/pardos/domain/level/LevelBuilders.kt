package com.korkoor.pardos.domain.level

import kotlin.random.Random

/**
 * Constructores de niveles: una línea por nivel. Calculan solos los límites (movimientos, tiempo, puntos) a partir de la
 * meta, así que un nivel nuevo se escribe diciendo solo "qué" y "dónde", por ejemplo:
 *
 *     stones(23, tile = 256, pattern = "4-pilar-a")
 *     sprint(24, tile = 128, slack = 1.35)
 *
 * Todos devuelven un [LevelSpec] ya completo (el catálogo y las pruebas lo validan).
 */
object LevelBuilders {

    /** Tablero por defecto: 3×3 hasta 128 (como siempre) y 4×4 a partir de ahí. */
    fun defaultSize(tile: Int): Int = if (tile <= 128) 3 else 4

    /** Nivel sin reglas especiales: llega a la ficha. */
    fun zen(id: Int, tile: Int, size: Int = defaultSize(tile), title: String = LevelKind.ZEN.label, tip: String? = null) =
        LevelSpec(id, LevelKind.ZEN, title, size, goalValue = tile, tip = tip)

    /** Suma puntos en vez de buscar una sola ficha: sirve cualquier forma de jugar mientras se fusione mucho. */
    fun score(id: Int, tile: Int, size: Int = defaultSize(tile), title: String = LevelKind.SCORE.label, tip: String? = null) =
        LevelSpec(
            id, LevelKind.SCORE, title, size,
            goal = LevelGoal.SCORE,
            goalValue = roundTo(LevelMath.tileScore(tile) * 1.15, 50),
            scaleTile = tile, tip = tip
        )

    /** Caen muchos más 4: llegar es más rápido, pero el tablero se llena antes. */
    fun fours(id: Int, tile: Int, size: Int = defaultSize(tile), title: String = LevelKind.FOURS.label, tip: String? = null) =
        LevelSpec(id, LevelKind.FOURS, title, size, goalValue = tile, spawn = SpawnStyle.FOURS, tip = tip)

    /** Empiezas con fichas altas ya puestas (su suma es algo más de la mitad de la meta). */
    fun headStart(id: Int, tile: Int, size: Int = defaultSize(tile), stones: StonePattern? = null, title: String = LevelKind.HEADSTART.label): LevelSpec {
        val blocked = stones?.cells.orEmpty()
        return LevelSpec(
            id, LevelKind.HEADSTART, title, size, goalValue = tile,
            stones = blocked, startTiles = startTilesFor(id, tile, size, blocked)
        )
    }

    /** Piedras fijas en el tablero. */
    fun stones(id: Int, tile: Int, pattern: String, title: String? = null): LevelSpec = stones(id, tile, StonePatterns.byId(pattern), title)

    fun stones(id: Int, tile: Int, pattern: StonePattern, title: String? = null) =
        LevelSpec(
            id, LevelKind.STONES, title ?: pattern.name, pattern.size, goalValue = tile, stones = pattern.cells,
            tip = "Las fichas se frenan contra ellas y no pueden saltarlas"
        )

    /**
     * Movimientos limitados. [slack] es cuántas veces el mínimo posible puedes gastar:
     * 1,7 generoso · 1,5 normal · 1,35 justo.
     */
    fun sprint(id: Int, tile: Int, size: Int = defaultSize(tile).coerceAtLeast(4), slack: Double = 1.5, stones: StonePattern? = null, title: String = LevelKind.SPRINT.label): LevelSpec {
        val base = LevelSpec(id, LevelKind.SPRINT, title, size, goalValue = tile, stones = stones?.cells.orEmpty())
        return base.copy(moveLimit = LevelMath.moveLimit(base, slack), tip = "Cada movimiento cuenta: planifica antes de deslizar")
    }

    /** Dos fichas de [tile] a la vez: hay que construir una segunda sin fusionarla con la primera. */
    fun twins(id: Int, tile: Int, size: Int = defaultSize(tile).coerceAtLeast(4), title: String = LevelKind.TWINS.label) =
        LevelSpec(id, LevelKind.TWINS, title, size, goalValue = tile, goalCount = 2, tip = "Guarda una ficha y construye otra igual")

    /** Contrarreloj: [secondsPerMove] segundos por cada movimiento mínimo (las combinaciones suman tiempo). */
    fun clock(id: Int, tile: Int, size: Int = defaultSize(tile).coerceAtLeast(4), secondsPerMove: Double = 1.6, stones: StonePattern? = null, title: String = LevelKind.CLOCK.label): LevelSpec {
        val base = LevelSpec(id, LevelKind.CLOCK, title, size, goalValue = tile, stones = stones?.cells.orEmpty())
        return base.copy(timeLimitMs = LevelMath.timeLimitMs(base, secondsPerMove), tip = "Encadena fusiones para ganar segundos")
    }

    /** Solo caen fichas grandes (4 y 8): el tablero se llena enseguida, pero cada fusión vale el doble. */
    fun heavy(id: Int, tile: Int, size: Int = 4, stones: StonePattern? = null, title: String = LevelKind.HEAVY.label) =
        LevelSpec(
            id, LevelKind.HEAVY, title, size, goalValue = tile, spawn = SpawnStyle.HEAVY, stones = stones?.cells.orEmpty(),
            tip = "Casi no caen doses: cuida cada hueco"
        )

    /** Escalera: [steps] fichas consecutivas a la vez, la mayor de [top] (16-32-64-128 con top = 128 y steps = 4). */
    fun ladder(id: Int, top: Int, steps: Int, size: Int = 4, stones: StonePattern? = null, slack: Double? = null, title: String = LevelKind.LADDER.label): LevelSpec {
        val base = LevelSpec(
            id, LevelKind.LADDER, title, size, goal = LevelGoal.LADDER, goalValue = top, goalCount = steps, stones = stones?.cells.orEmpty(),
            scaleTile = top, tip = "Guarda cada peldaño: si fusionas uno, tendrás que volver a hacerlo"
        )
        return if (slack == null) base else base.copy(moveLimit = LevelMath.moveLimit(base, slack))
    }

    /** Maratón: [merges] fusiones en total, con un tope de movimientos ([slack] veces las fusiones pedidas). */
    fun marathon(
        id: Int, merges: Int, size: Int = 4, slack: Double = 1.4, stones: StonePattern? = null,
        spawn: SpawnStyle = SpawnStyle.NORMAL, title: String = LevelKind.MARATHON.label
    ): LevelSpec {
        val base = LevelSpec(
            id, LevelKind.MARATHON, title, size, goal = LevelGoal.MERGES, goalValue = merges, stones = stones?.cells.orEmpty(),
            scaleTile = 128, spawn = spawn
        )
        return base.copy(moveLimit = LevelMath.moveLimit(base, slack), tip = "Cuenta cada fusión: los movimientos que no fusionan se pagan")
    }

    /** Combo: un solo deslizamiento que fusione [pairs] pares a la vez, [times] veces, en un tope de movimientos. */
    fun combo(id: Int, pairs: Int, times: Int = 1, size: Int = 4, slack: Double = 1.6, stones: StonePattern? = null, title: String = LevelKind.COMBO.label): LevelSpec {
        val base = LevelSpec(
            id, LevelKind.COMBO, title, size, goal = LevelGoal.COMBO, goalValue = pairs, goalCount = times, stones = stones?.cells.orEmpty(),
            scaleTile = 64
        )
        return base.copy(moveLimit = LevelMath.moveLimit(base, slack), tip = "Prepara filas con pares iguales y deslízalas todas a la vez")
    }

    /** Un nivel normal con los controles girados. */
    fun twisted(base: LevelSpec, twist: Twist, title: String = LevelKind.TWIST.label): LevelSpec =
        base.copy(kind = LevelKind.TWIST, title = title, twist = twist, tip = twist.hint)

    /** Piedras temporales que caen durante la partida. */
    fun storm(id: Int, tile: Int, size: Int = 4, storm: Storm = Storm(everyMoves = 8, lifeMoves = 6, maxStones = 2), stones: StonePattern? = null, title: String = LevelKind.STORM.label) =
        LevelSpec(
            id, LevelKind.STORM, title, size, goalValue = tile, stones = stones?.cells.orEmpty(), storm = storm,
            tip = "Las piedras que caen se van solas pasados ${storm.lifeMoves} movimientos"
        )

    /** Convierte un nivel en jefe de capítulo: se queda con todas sus reglas y gana nombre propio. */
    fun boss(base: LevelSpec, title: String, tip: String? = null): LevelSpec =
        base.copy(kind = LevelKind.BOSS, title = title, tip = tip ?: base.tip)

    // ---------------------------------------------------------------------------------------------------------------

    private fun roundTo(value: Double, step: Int): Int = ((value / step).toInt().coerceAtLeast(1)) * step

    /**
     * Fichas de salida de un nivel con ventaja: una mayor, dos medianas y una pequeña (algo más de la mitad de la meta).
     * Se reparten por casillas libres de forma determinista según el número de nivel.
     */
    internal fun startTilesFor(id: Int, tile: Int, size: Int, blocked: List<Cell>): List<StartTile> {
        val values = buildList {
            add(tile / 4); add(tile / 8); add(tile / 8); add(tile / 16)
            if (size >= 5) { add(tile / 16); add(tile / 32) }
        }.filter { it >= 2 }
        val rnd = Random(id * 31L + tile)
        val free = (0 until size).flatMap { r -> (0 until size).map { c -> r to c } }.filter { it !in blocked }.shuffled(rnd)
        return values.mapIndexed { i, v -> StartTile(free[i].first, free[i].second, v) }
    }
}
