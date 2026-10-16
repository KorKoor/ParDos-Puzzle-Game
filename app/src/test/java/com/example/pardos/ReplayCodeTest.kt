package com.example.pardos

import com.korkoor.pardos.data.local.ReplayCode
import com.korkoor.pardos.data.local.ReplayFrame
import com.korkoor.pardos.data.local.ReplayTile
import com.korkoor.pardos.data.local.SavedGameReplay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ReplayCodeTest {
    private fun sample(frames: Int) = SavedGameReplay(
        boardSize = 4, target = 2048, finalScore = 1234,
        frames = (0 until frames).map { ReplayFrame(it, if (it == 0) "Inicio" else "Izquierda", it * 4, listOf(ReplayTile(0, 0, 2), ReplayTile(3, 2, 8 + it))) },
        savedAt = 1_700_000_000_000L
    )

    @Test
    fun roundTripKeepsEverything() {
        val r = sample(40)
        val back = ReplayCode.decode(ReplayCode.encode(r))
        assertNotNull(back)
        assertEquals(r, back)
    }

    @Test
    fun longReplaysAreTrimmedToTheLastFrames() {
        val back = ReplayCode.decode(ReplayCode.encode(sample(400)))!!
        assertEquals(150, back.frames.size)
        assertEquals(399, back.frames.last().move)
    }

    @Test
    fun badCodesAreRejected() {
        assertNull(ReplayCode.decode(""))
        assertNull(ReplayCode.decode("hola"))
        assertNull(ReplayCode.decode("PDR1-@@@@"))
        assertNull(ReplayCode.decode(ReplayCode.encode(sample(5)).dropLast(9)))
    }

    @Test
    fun codeSurvivesSpacesAndLineBreaks() {
        val code = ReplayCode.encode(sample(10))
        val broken = code.chunked(30).joinToString("\n ")
        assertEquals(sample(10), ReplayCode.decode(broken))
    }
}
