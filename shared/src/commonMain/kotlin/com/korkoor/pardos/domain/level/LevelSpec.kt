package com.korkoor.pardos.domain.level

import com.korkoor.pardos.domain.logic.Direction

/** Casilla del tablero como (fila, columna). */
typealias Cell = Pair<Int, Int>

/** Qué hay que conseguir para ganar. */
enum class LevelGoal {
    /** Tener [LevelSpec.goalCount] fichas de [LevelSpec.goalValue] o más a la vez. */
    REACH_TILE,

    /** Sumar [LevelSpec.goalValue] puntos. */
    SCORE,

    /**
     * Escalera: tener a la vez [LevelSpec.goalCount] fichas distintas y consecutivas, la mayor de [LevelSpec.goalValue]
     * (por ejemplo 16-32-64-128 con valor 128 y cantidad 4).
     */
    LADDER,

    /** Maratón: hacer [LevelSpec.goalValue] fusiones en total. */
    MERGES,

    /** Combo: lograr [LevelSpec.goalCount] veces un movimiento que fusione [LevelSpec.goalValue] pares a la vez. */
    COMBO
}

/** Cómo caen las fichas nuevas. */
enum class SpawnStyle {
    NORMAL,

    /** Caen muchos 4. */
    FOURS,

    /** Solo fichas grandes (casi todo 4 y 8): el tablero se llena rápido pero se avanza el doble. */
    HEAVY
}

/**
 * Giro de los controles: el dedo no empuja hacia donde apunta. Es una regla de habilidad (el tablero y las probabilidades
 * son los de siempre), así que no cambia la dificultad matemática del nivel.
 */
enum class Twist(val label: String, val hint: String) {
    NONE("", ""),
    MIRROR_H("Izquierda ↔ derecha", "Izquierda y derecha están cambiadas"),
    MIRROR_V("Arriba ↔ abajo", "Arriba y abajo están cambiados"),
    FLIP("Todo al revés", "Cada deslizamiento va en la dirección contraria"),
    SPIN("Giro de 90°", "Cada deslizamiento gira un cuarto de vuelta a la derecha");

    /** Dirección real del tablero para el deslizamiento hecho por el jugador. */
    fun apply(d: Direction): Direction = when (this) {
        NONE -> d
        MIRROR_H -> when (d) { Direction.LEFT -> Direction.RIGHT; Direction.RIGHT -> Direction.LEFT; else -> d }
        MIRROR_V -> when (d) { Direction.UP -> Direction.DOWN; Direction.DOWN -> Direction.UP; else -> d }
        FLIP -> when (d) { Direction.UP -> Direction.DOWN; Direction.DOWN -> Direction.UP; Direction.LEFT -> Direction.RIGHT; Direction.RIGHT -> Direction.LEFT }
        SPIN -> when (d) { Direction.UP -> Direction.RIGHT; Direction.RIGHT -> Direction.DOWN; Direction.DOWN -> Direction.LEFT; Direction.LEFT -> Direction.UP }
    }
}

/**
 * Tormenta: cada [everyMoves] movimientos cae una piedra temporal en una casilla libre y se va [lifeMoves] movimientos
 * después. Nunca hay más de [maxStones] a la vez ni caen si el tablero está casi lleno.
 */
data class Storm(val everyMoves: Int, val lifeMoves: Int, val maxStones: Int)

/**
 * Tipo de nivel: es lo que el jugador ve en el mapa y le dice, de un vistazo, qué le espera.
 * Para añadir un tipo nuevo: un valor aquí, su constructor en `LevelBuilders.kt` y (si debe salir solo) su peso en
 * `LevelCatalog`. La interfaz solo necesita un icono y un color por tipo.
 */
enum class LevelKind(val label: String, val rule: String) {
    ZEN("Zen", "Sin presión: juega a tu ritmo"),
    SCORE("Puntos", "Suma puntos fusionando, sin buscar una sola ficha"),
    FOURS("Lluvia de 4", "Caen muchos más 4: todo va más rápido"),
    HEADSTART("Con ventaja", "Empiezas con fichas altas ya en el tablero"),
    STONES("Piedras", "Las piedras no se mueven ni se pueden fusionar"),
    SPRINT("Sprint", "Tienes un número limitado de movimientos"),
    TWINS("Gemelas", "Necesitas dos fichas iguales a la vez"),
    CLOCK("Contrarreloj", "Las combinaciones te regalan tiempo"),
    BOSS("Jefe", "Un reto con varias reglas a la vez"),
    HEAVY("Pesadas", "Solo caen fichas grandes: el tablero se llena enseguida"),
    LADDER("Escalera", "Reúne fichas consecutivas a la vez, de menor a mayor"),
    MARATHON("Maratón", "Cuenta las fusiones: cada deslizamiento tiene que valer"),
    COMBO("Combo", "Un solo deslizamiento tiene que fusionar varios pares"),
    TWIST("Del revés", "Los controles no van hacia donde deslizas"),
    STORM("Tormenta", "Caen piedras temporales que te cierran el paso");

    /** Tipos que castigan el error y no deberían ir seguidos en el mapa. */
    val isDemanding: Boolean get() = this == SPRINT || this == CLOCK || this == BOSS || this == TWINS || this == COMBO || this == STORM || this == TWIST
}

/** Ficha que ya está en el tablero al empezar. */
data class StartTile(val row: Int, val col: Int, val value: Int)

/**
 * Todo lo que define un nivel de campaña. Es un dato puro: el catálogo ([LevelCatalog]) lo produce y el juego solo lo lee,
 * así que añadir o ajustar niveles no toca el motor ni la interfaz.
 */
data class LevelSpec(
    val id: Int,
    val kind: LevelKind,
    val title: String = kind.label,
    val boardSize: Int,
    val goal: LevelGoal = LevelGoal.REACH_TILE,
    /** Ficha a alcanzar, o puntos si [goal] es [LevelGoal.SCORE]. */
    val goalValue: Int,
    val goalCount: Int = 1,
    val stones: List<Cell> = emptyList(),
    val moveLimit: Int? = null,
    /** Tiempo total en milisegundos (las combinaciones suman más); `null` = sin reloj. */
    val timeLimitMs: Long? = null,
    val spawn: SpawnStyle = SpawnStyle.NORMAL,
    val startTiles: List<StartTile> = emptyList(),
    /** Ficha equivalente al esfuerzo del nivel; decide el reparto de fichas nuevas y la dificultad percibida. */
    val scaleTile: Int = goalValue,
    /** Ayuda corta que se enseña al empezar (si hace falta explicar algo más que el tipo). */
    val tip: String? = null,
    /** Giro de los controles (solo cambia cómo se juega, no el tablero). */
    val twist: Twist = Twist.NONE,
    /** Piedras temporales que van cayendo durante la partida. */
    val storm: Storm? = null
) {
    val stoneSet: Set<Cell> get() = stones.toSet()
    val freeCells: Int get() = boardSize * boardSize - stones.size
    val isTimed: Boolean get() = timeLimitMs != null
    val hasMoveLimit: Boolean get() = moveLimit != null
    val isBoss: Boolean get() = kind == LevelKind.BOSS

    /** Meta escrita para la tarjeta del nivel: "Llega a 256", "Suma 1.800 puntos", "Ten 2 fichas de 64". */
    fun goalText(): String = goalText(goal, goalValue, goalCount)

    /** Reglas especiales en frases cortas (para las etiquetas del nivel). */
    fun ruleChips(): List<String> = buildList {
        if (stones.isNotEmpty()) add(if (stones.size == 1) "1 piedra" else "${stones.size} piedras")
        moveLimit?.let { add("$it movimientos") }
        timeLimitMs?.let { add(formatClock(it)) }
        if (spawn == SpawnStyle.FOURS) add("Lluvia de 4")
        if (spawn == SpawnStyle.HEAVY) add("Fichas pesadas")
        if (startTiles.isNotEmpty()) add("Con ventaja")
        if (twist != Twist.NONE) add(twist.label)
        storm?.let { add("Tormenta: cada ${it.everyMoves} mov.") }
    }
}

/** Meta escrita para cualquier combinación de objetivo (también la usa la interfaz con los datos del tablero). */
fun goalText(goal: LevelGoal, value: Int, count: Int): String = when {
    goal == LevelGoal.SCORE -> "Suma ${formatThousands(value)} puntos"
    goal == LevelGoal.LADDER -> "Reúne ${ladderRungs(value, count).joinToString("-")} a la vez"
    goal == LevelGoal.MERGES -> "Haz ${formatThousands(value)} fusiones"
    goal == LevelGoal.COMBO -> if (count > 1) "Fusiona $value pares de golpe, $count veces" else "Fusiona $value pares de golpe"
    count > 1 -> "Ten $count fichas de $value a la vez"
    else -> "Llega a $value"
}

/** Peldaños de una escalera de meta [top] con [steps] fichas, de menor a mayor (16-32-64-128). */
fun ladderRungs(top: Int, steps: Int): List<Int> = (steps - 1 downTo 0).map { top shr it }.filter { it >= 2 }

fun formatThousands(n: Int): String {
    val s = n.toString()
    if (s.length <= 3) return s
    val sb = StringBuilder()
    s.forEachIndexed { i, ch ->
        if (i > 0 && (s.length - i) % 3 == 0) sb.append('.')
        sb.append(ch)
    }
    return sb.toString()
}

fun formatClock(ms: Long): String {
    val total = (ms / 1000).toInt()
    return "${total / 60}:${(total % 60).toString().padStart(2, '0')}"
}
