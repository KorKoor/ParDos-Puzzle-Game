package com.korkoor.pardos.domain.level

import com.korkoor.pardos.domain.model.TileModel

/**
 * Comprueba que un nivel tiene sentido antes de que lo juegue nadie. Lo usan las pruebas sobre todo el catálogo, así que
 * un nivel mal escrito (piedra fuera del tablero, meta imposible, límite de movimientos menor que el mínimo...) hace
 * fallar la compilación de pruebas en vez de llegar a un jugador.
 */
object LevelValidator {

    fun problems(spec: LevelSpec): List<String> = buildList {
        val tag = "Nivel ${spec.id}"
        if (spec.boardSize !in 3..6) add("$tag: tablero ${spec.boardSize} fuera de 3..6")
        if (spec.goalValue <= 0) add("$tag: meta ${spec.goalValue} no válida")
        val maxCount = when (spec.goal) { LevelGoal.LADDER -> 5; LevelGoal.COMBO -> 4; LevelGoal.HARVEST -> 40; else -> 3 }
        val minCount = if (spec.goal == LevelGoal.LADDER) 2 else if (spec.goal == LevelGoal.HARVEST) 3 else 1
        if (spec.goalCount !in minCount..maxCount) add("$tag: cantidad meta ${spec.goalCount} fuera de $minCount..$maxCount para ${spec.goal}")
        if (spec.scaleTile <= 1 || !isPowerOfTwo(spec.scaleTile)) add("$tag: ficha de escala ${spec.scaleTile} no es potencia de 2")
        if ((spec.goal == LevelGoal.REACH_TILE || spec.goal == LevelGoal.LADDER || spec.goal == LevelGoal.HARVEST) && !isPowerOfTwo(spec.goalValue)) add("$tag: la ficha meta ${spec.goalValue} no es potencia de 2")
        if ((spec.goal == LevelGoal.SCORE || spec.goal == LevelGoal.MERGES) && spec.goalCount != 1) add("$tag: una meta de ${spec.goal} no lleva cantidad")
        if (spec.goal == LevelGoal.LADDER && spec.goalValue shr (spec.goalCount - 1) < 4) add("$tag: la escalera baja de 4")
        if (spec.goal == LevelGoal.COMBO && spec.goalValue !in 2..(spec.boardSize * 2 - 2)) add("$tag: combo de ${spec.goalValue} pares imposible en ${spec.boardSize}×${spec.boardSize}")
        if (spec.goal == LevelGoal.MERGES && spec.goalValue < 20) add("$tag: maratón de ${spec.goalValue} fusiones es demasiado corto")
        if (spec.kind == LevelKind.TWIST && spec.twist == Twist.NONE) add("$tag: nivel del revés sin giro")
        if (spec.dropsPerMove !in 1..2) add("$tag: ${spec.dropsPerMove} fichas por jugada")
        if (spec.kind == LevelKind.DOUBLE && spec.dropsPerMove != 2) add("$tag: doble caída sin dos fichas por jugada")
        if (spec.goal == LevelGoal.HARVEST && spec.goalValue < 8) add("$tag: cosecha de fichas de ${spec.goalValue}")
        if (spec.kind == LevelKind.STORM && spec.storm == null) add("$tag: nivel de tormenta sin tormenta")
        spec.phase2?.let { ph ->
            if (spec.kind != LevelKind.BOSS) add("$tag: solo los jefes tienen segunda fase")
            if (ph.atMoves < 8 || ph.atMoves >= LevelMath.minMoves(spec)) add("$tag: la fase 2 empieza en ${ph.atMoves} y el nivel dura unos ${LevelMath.minMoves(spec)}")
            if (ph.storm == null && ph.twist == null && ph.spawn == null) add("$tag: la fase 2 no cambia nada")
            ph.storm?.let { st -> if (spec.freeCells - st.maxStones < 9) add("$tag: la tormenta de la fase 2 deja poco sitio") }
        }
        spec.storm?.let { st ->
            if (st.everyMoves < 3 || st.lifeMoves < 2 || st.maxStones !in 1..4) add("$tag: tormenta $st fuera de rango")
            if (spec.freeCells - st.maxStones < 9) add("$tag: la tormenta deja demasiado poco sitio")
        }
        if (spec.title.isBlank()) add("$tag: sin título")

        // Piedras
        val inside = spec.stones.all { (r, c) -> r in 0 until spec.boardSize && c in 0 until spec.boardSize }
        if (!inside) add("$tag: hay piedras fuera del tablero")
        if (spec.stones.toSet().size != spec.stones.size) add("$tag: piedras repetidas")
        if (inside && !isConnected(spec.boardSize, spec.stoneSet)) add("$tag: las piedras dejan casillas aisladas")
        if (spec.kind == LevelKind.STONES && spec.stones.isEmpty()) add("$tag: nivel de piedras sin piedras")
        if (spec.boardSize !in 3..6 && spec.stones.isNotEmpty()) add("$tag: tablero no soportado con piedras")
        if (spec.freeCells < 9) add("$tag: solo ${spec.freeCells} casillas libres")

        // Meta alcanzable
        val needed = LevelMath.neededSum(spec)
        val cap = LevelMath.practicalCap(spec.freeCells)
        if (needed > cap * 2) add("$tag: la meta $needed no cabe en ${spec.freeCells} casillas (tope $cap)")

        // Límites
        val min = LevelMath.minMoves(spec)
        // En un maratón (fusiones ≈ movimientos) basta un 5 % de margen: el límite es parte del reto
        val needFactor = if (spec.goal == LevelGoal.MERGES) 1.05 else 1.2
        spec.moveLimit?.let { if (it < (min * needFactor).toInt()) add("$tag: ${it} movimientos no bastan (mínimo posible $min)") }
        spec.timeLimitMs?.let { if (it < min * 1000L) add("$tag: el reloj ($it ms) no llega para $min movimientos") }

        // Fichas de salida
        val starts = spec.startTiles
        if (starts.any { !isPowerOfTwo(it.value) || it.value < 2 }) add("$tag: ficha de salida con valor no válido")
        if (starts.any { it.row !in 0 until spec.boardSize || it.col !in 0 until spec.boardSize }) add("$tag: ficha de salida fuera del tablero")
        if (starts.map { it.row to it.col }.toSet().size != starts.size) add("$tag: fichas de salida solapadas")
        if (starts.any { (it.row to it.col) in spec.stoneSet }) add("$tag: ficha de salida sobre una piedra")
        if (spec.kind == LevelKind.HEADSTART && starts.isEmpty()) add("$tag: nivel con ventaja sin fichas de salida")
        if (starts.isNotEmpty()) {
            val tiles = starts.mapIndexed { i, s -> TileModel("s$i", s.value, s.row, s.col) }
            if (LevelRules.isGoalReached(spec, tiles, 0)) add("$tag: ya empieza ganado")
            if (starts.size > spec.freeCells - 3) add("$tag: demasiadas fichas de salida")
        }
    }

    /** Todas las casillas libres forman una sola región (ninguna queda aislada por las piedras). */
    internal fun isConnected(size: Int, stones: Set<Cell>): Boolean {
        val free = (0 until size).flatMap { r -> (0 until size).map { c -> r to c } }.filter { it !in stones }
        if (free.isEmpty()) return false
        val seen = HashSet<Cell>()
        val stack = ArrayDeque<Cell>()
        stack.add(free.first()); seen.add(free.first())
        while (stack.isNotEmpty()) {
            val (r, c) = stack.removeLast()
            for ((dr, dc) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
                val n = (r + dr) to (c + dc)
                if (n.first in 0 until size && n.second in 0 until size && n !in stones && seen.add(n)) stack.add(n)
            }
        }
        return seen.size == free.size
    }

    private fun isPowerOfTwo(n: Int) = n > 0 && (n and (n - 1)) == 0
}
