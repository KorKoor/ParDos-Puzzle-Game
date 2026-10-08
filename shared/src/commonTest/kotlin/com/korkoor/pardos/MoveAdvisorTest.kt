package com.korkoor.pardos

import com.korkoor.pardos.domain.logic.CoachKind
import com.korkoor.pardos.domain.logic.Direction
import com.korkoor.pardos.domain.logic.MoveAdvisor
import com.korkoor.pardos.domain.logic.TutorialCoach
import com.korkoor.pardos.domain.model.TileModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MoveAdvisorTest {
    private fun t(r: Int, c: Int, v: Int) = TileModel("t${r}_$c", v, r, c)

    @Test fun prefersTheDirectionThatMergesAndReportsTheCells() {
        // dos 2 en la fila 1: solo deslizando a los lados se funden
        val advice = MoveAdvisor.suggest(4, emptySet(), listOf(t(1, 0, 2), t(1, 3, 2), t(3, 1, 8)))!!
        assertTrue(advice.direction == Direction.LEFT || advice.direction == Direction.RIGHT)
        assertEquals(1, advice.merges)
        assertEquals(setOf(1 to 0, 1 to 3), advice.mergeCells)
    }

    @Test fun fallsBackToAnyValidMoveWhenNothingMerges() {
        val advice = MoveAdvisor.suggest(4, emptySet(), listOf(t(0, 0, 2), t(2, 2, 4)))!!
        assertEquals(0, advice.merges)
        assertTrue(advice.mergeCells.isEmpty())
    }

    @Test fun skipsMovesThatChangeNothing() {
        // la ficha ya está en la esquina arriba-izquierda: no sirve ni UP ni LEFT
        val advice = MoveAdvisor.suggest(4, emptySet(), listOf(t(0, 0, 2)))!!
        assertTrue(advice.direction == Direction.RIGHT || advice.direction == Direction.DOWN)
    }

    @Test fun returnsNullWithoutMovesOrTiles() {
        assertNull(MoveAdvisor.suggest(4, emptySet(), emptyList()))
        val full = (0 until 2).flatMap { r -> (0 until 2).map { c -> t(r, c, if ((r + c) % 2 == 0) 2 else 4) } }
        assertNull(MoveAdvisor.suggest(2, emptySet(), full))
    }

    @Test fun respectsStones() {
        // una piedra entre los dos 2: no se pueden fundir en horizontal
        val advice = MoveAdvisor.suggest(4, setOf(0 to 1), listOf(t(0, 0, 2), t(0, 2, 2)))!!
        assertEquals(0, advice.merges)
    }

    @Test fun tutorialStartsWithSwipeThenTeachesMergingThenFinishes() {
        val goal = "Llega a 32"
        val start = TutorialCoach.step(3, emptySet(), listOf(t(0, 0, 2), t(2, 2, 2)), 0, 0, goal)!!
        assertEquals(CoachKind.SWIPE, start.kind)
        assertNotNull(start.direction)

        val mergeLesson = TutorialCoach.step(3, emptySet(), listOf(t(0, 0, 2), t(0, 2, 2)), 2, 0, goal)!!
        assertEquals(CoachKind.MERGE, mergeLesson.kind)
        assertEquals(setOf(0 to 0, 0 to 2), mergeLesson.cells)

        val praise = TutorialCoach.step(3, emptySet(), listOf(t(0, 0, 4)), 5, 1, goal)!!
        assertEquals(CoachKind.PRAISE, praise.kind)
        assertTrue(praise.text.contains(goal))

        assertNull(TutorialCoach.step(3, emptySet(), listOf(t(0, 0, 4)), 10, TutorialCoach.MERGES_TO_FINISH, goal))
        assertTrue(TutorialCoach.isFinished(TutorialCoach.MAX_MOVES, 0))
    }
}
