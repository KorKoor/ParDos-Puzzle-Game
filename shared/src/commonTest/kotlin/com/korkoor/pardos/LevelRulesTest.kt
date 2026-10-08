package com.korkoor.pardos

import com.korkoor.pardos.domain.level.LevelBuilders
import com.korkoor.pardos.domain.level.LevelGoal
import com.korkoor.pardos.domain.level.LevelKind
import com.korkoor.pardos.domain.level.LevelSpec
import com.korkoor.pardos.domain.level.LevelValidator
import com.korkoor.pardos.domain.level.StartTile
import com.korkoor.pardos.domain.level.StonePatterns
import com.korkoor.pardos.domain.model.BoardState
import com.korkoor.pardos.domain.model.TileModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LevelRulesTest {
    private fun t(v: Int, i: Int) = TileModel("t$i", v, i / 4, i % 4)

    @Test fun scoreGoalProgressFollowsPoints() {
        val state = BoardState(boardSize = 4, goal = LevelGoal.SCORE, levelLimit = 400, score = 100)
        assertEquals(0.25f, state.levelProgress, 0.001f)
        assertEquals(1f, state.copy(score = 900).levelProgress)
    }

    @Test fun twinsProgressAveragesTheBestTiles() {
        val base = BoardState(boardSize = 4, goal = LevelGoal.REACH_TILE, goalCount = 2, levelLimit = 64)
        // una ficha completa (64) y otra a medias (8 = mitad del camino): 0,75
        assertEquals(0.75f, base.copy(tiles = listOf(t(64, 0), t(8, 1), t(2, 2))).levelProgress, 0.001f)
        // con una sola ficha solo se cubre la mitad
        assertEquals(0.5f, base.copy(tiles = listOf(t(64, 0))).levelProgress, 0.001f)
    }

    @Test fun movesLeftAndFreeCellsRespectTheRules() {
        val state = BoardState(boardSize = 4, moveLimit = 30, moveCount = 12, blocked = listOf(0 to 0, 3 to 3), tiles = listOf(t(2, 5)))
        assertEquals(18, state.movesLeft)
        assertEquals(13, state.emptySpaces)
        assertEquals(null, BoardState().movesLeft)
        assertTrue(state.copy(tiles = listOf(TileModel("s", 2, 0, 0))).validate().errors.any { it.contains("piedra") })
    }

    // ---- el validador atrapa los errores típicos al escribir un nivel a mano ----

    private fun base(): LevelSpec = LevelBuilders.zen(99, 128, 4)

    @Test fun validatorAcceptsTheBuilders() {
        val ok = listOf(
            LevelBuilders.zen(1, 64), LevelBuilders.score(2, 128), LevelBuilders.fours(3, 128, 4), LevelBuilders.headStart(4, 128, 4),
            LevelBuilders.stones(5, 128, "4-pilar-a"), LevelBuilders.sprint(6, 128), LevelBuilders.twins(7, 64), LevelBuilders.clock(8, 128)
        )
        ok.forEach { assertTrue(LevelValidator.problems(it).isEmpty(), "${it.kind}: ${LevelValidator.problems(it)}") }
    }

    @Test fun validatorCatchesBadLevels() {
        assertTrue(LevelValidator.problems(base().copy(goalValue = 100)).isNotEmpty(), "meta que no es potencia de 2")
        assertTrue(LevelValidator.problems(base().copy(stones = listOf(5 to 5))).isNotEmpty(), "piedra fuera del tablero")
        assertTrue(LevelValidator.problems(base().copy(stones = listOf(1 to 1, 1 to 1))).isNotEmpty(), "piedras repetidas")
        assertTrue(LevelValidator.problems(base().copy(moveLimit = 10)).isNotEmpty(), "pocos movimientos")
        assertTrue(LevelValidator.problems(base().copy(timeLimitMs = 5_000)).isNotEmpty(), "reloj imposible")
        assertTrue(LevelValidator.problems(base().copy(kind = LevelKind.STONES)).isNotEmpty(), "piedras sin piedras")
        assertTrue(LevelValidator.problems(base().copy(kind = LevelKind.HEADSTART)).isNotEmpty(), "ventaja sin fichas")
        assertTrue(
            LevelValidator.problems(base().copy(startTiles = listOf(StartTile(0, 0, 128)))).isNotEmpty(), "ya empieza ganado"
        )
        // piedras que encierran una casilla
        val walled = base().copy(stones = listOf(0 to 1, 1 to 0, 1 to 2, 2 to 1, 3 to 3).let { it })
        assertTrue(LevelValidator.problems(walled).isNotEmpty(), "casilla aislada")
        assertTrue(StonePatterns.all.none { p -> LevelValidator.problems(base().copy(boardSize = p.size, stones = p.cells)).any { it.contains("aisla") } })
    }
}
