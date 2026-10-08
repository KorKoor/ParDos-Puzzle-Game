package com.korkoor.pardos.domain.logic

import com.korkoor.pardos.domain.model.TileModel
import kotlin.random.Random

/** Una jugada recomendada: hacia dónde deslizar y qué fichas se van a fundir con ella. */
data class MoveAdvice(
    val direction: Direction,
    /** Casillas (fila, columna) de las fichas que se fusionan al deslizar así. Vacío si no se fusiona nada. */
    val mergeCells: Set<Pair<Int, Int>>,
    val merges: Int
)

/**
 * Consejero de jugadas sencillo para los tutoriales y las pistas: prefiere la dirección que más fichas funde y, a igualdad,
 * la que deja más huecos. No intenta jugar bien: solo enseña un movimiento válido y útil.
 */
object MoveAdvisor {
    /** Orden de preferencia a igualdad: es el que más se parece a un gesto natural. */
    private val order = listOf(Direction.RIGHT, Direction.LEFT, Direction.DOWN, Direction.UP)

    fun suggest(boardSize: Int, blocked: Set<Pair<Int, Int>>, tiles: List<TileModel>): MoveAdvice? {
        if (tiles.isEmpty()) return null
        val engine = GameEngine(boardSize, Random(0), blocked)
        var best: MoveAdvice? = null
        var bestScore = Int.MIN_VALUE
        for (dir in order) {
            val (moved, _) = engine.move(tiles, dir)
            if (!changed(tiles, moved)) continue
            val merges = tiles.size - moved.size
            val score = merges * 100 - moved.size
            if (score > bestScore) {
                bestScore = score
                best = MoveAdvice(dir, mergingCells(tiles, moved), merges)
            }
        }
        return best
    }

    private fun changed(before: List<TileModel>, after: List<TileModel>): Boolean {
        if (before.size != after.size) return true
        val map = before.associate { (it.row to it.col) to it.value }
        return after.any { map[it.row to it.col] != it.value }
    }

    /** Fichas que desaparecen o crecen al deslizar: son las que se funden. */
    private fun mergingCells(before: List<TileModel>, after: List<TileModel>): Set<Pair<Int, Int>> {
        val survivors = after.associateBy { it.id }
        val cells = HashSet<Pair<Int, Int>>()
        for (t in before) {
            val now = survivors[t.id]
            if (now == null || now.value != t.value) cells.add(t.row to t.col)
        }
        return cells
    }
}

/** Qué enseñar en cada momento del tutorial del primer nivel. */
enum class CoachKind { SWIPE, MERGE, PRAISE }

data class CoachStep(
    val kind: CoachKind,
    val text: String,
    /** Hacia dónde mostrar la mano (`null` = solo texto). */
    val direction: Direction?,
    /** Fichas a resaltar. */
    val cells: Set<Pair<Int, Int>>,
    /** Fusiones que ya hizo y las que hacen falta para terminar el tutorial. */
    val mergesDone: Int,
    val mergesNeeded: Int
)

/**
 * Guion del tutorial: 1) deslizar, 2) juntar dos fichas iguales, 3) repetirlo un par de veces. Termina solo cuando
 * ya sabe fusionar ([MERGES_TO_FINISH] veces) o tras unos cuantos movimientos.
 */
object TutorialCoach {
    const val MERGES_TO_FINISH = 3
    const val MAX_MOVES = 24

    fun isFinished(moveCount: Int, merges: Int): Boolean = merges >= MERGES_TO_FINISH || moveCount >= MAX_MOVES

    fun step(
        boardSize: Int,
        blocked: Set<Pair<Int, Int>>,
        tiles: List<TileModel>,
        moveCount: Int,
        merges: Int,
        goalText: String
    ): CoachStep? {
        if (isFinished(moveCount, merges)) return null
        val advice = MoveAdvisor.suggest(boardSize, blocked, tiles)
        val canMerge = advice != null && advice.merges > 0
        return when {
            merges == 0 && moveCount == 0 -> CoachStep(
                CoachKind.SWIPE, "Desliza el dedo para mover todas las fichas a la vez",
                advice?.direction, advice?.mergeCells.orEmpty(), merges, MERGES_TO_FINISH
            )
            merges == 0 && canMerge -> CoachStep(
                CoachKind.MERGE, "Cuando dos fichas iguales chocan, ¡se suman!",
                advice!!.direction, advice.mergeCells, merges, MERGES_TO_FINISH
            )
            merges == 0 -> CoachStep(
                CoachKind.SWIPE, "Sigue deslizando hasta que dos fichas iguales se toquen",
                advice?.direction, emptySet(), merges, MERGES_TO_FINISH
            )
            canMerge -> CoachStep(
                CoachKind.PRAISE, if (merges == 1) "¡Eso es! Las fichas iguales se suman. $goalText" else "¡Muy bien! Sigue juntando fichas iguales",
                advice!!.direction, advice.mergeCells, merges, MERGES_TO_FINISH
            )
            else -> CoachStep(
                CoachKind.PRAISE, "¡Muy bien! $goalText", null, emptySet(), merges, MERGES_TO_FINISH
            )
        }
    }
}
