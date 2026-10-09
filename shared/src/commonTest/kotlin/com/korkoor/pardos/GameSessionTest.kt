package com.korkoor.pardos

import com.korkoor.pardos.domain.level.LevelCatalog
import com.korkoor.pardos.domain.level.LevelKind
import com.korkoor.pardos.domain.level.Twist
import com.korkoor.pardos.domain.session.GameSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Lector de JSON mínimo para comprobar que lo que se entrega a Swift está bien formado. */
private class MiniJson(private val s: String) {
    private var i = 0
    fun parse(): Any? { val v = value(); ws(); require(i == s.length) { "basura al final en $i" }; return v }
    private fun ws() { while (i < s.length && s[i].isWhitespace()) i++ }
    private fun value(): Any? {
        ws()
        return when (val c = s[i]) {
            '{' -> { i++; val m = LinkedHashMap<String, Any?>(); ws(); if (s[i] == '}') { i++; return m }
                while (true) { ws(); val k = str(); ws(); expect(':'); m[k] = value(); ws(); if (s[i] == ',') i++ else { expect('}'); break } }; m }
            '[' -> { i++; val l = ArrayList<Any?>(); ws(); if (s[i] == ']') { i++; return l }
                while (true) { l.add(value()); ws(); if (s[i] == ',') i++ else { expect(']'); break } }; l }
            '"' -> str()
            't' -> { i += 4; true }
            'f' -> { i += 5; false }
            'n' -> { i += 4; null }
            else -> { val st = i; while (i < s.length && (s[i].isDigit() || s[i] in "+-.eE")) i++; require(i > st) { "carácter '$c' en $i" }; s.substring(st, i).toDouble() }
        }
    }
    private fun expect(c: Char) { require(s[i] == c) { "se esperaba '$c' en $i y hay '${s[i]}'" }; i++ }
    private fun str(): String {
        expect('"'); val sb = StringBuilder()
        while (s[i] != '"') {
            if (s[i] == '\\') { i++; when (s[i]) { 'n' -> sb.append('\n'); 'r' -> sb.append('\r'); 't' -> sb.append('\t'); 'u' -> { sb.append(s.substring(i + 1, i + 5).toInt(16).toChar()); i += 4 }; else -> sb.append(s[i]) } }
            else sb.append(s[i])
            i++
        }
        i++; return sb.toString()
    }
}

@Suppress("UNCHECKED_CAST")
private fun parse(json: String): Map<String, Any?> = MiniJson(json).parse() as Map<String, Any?>

class GameSessionTest {
    private fun GameSession.state() = parse(snapshot())
    private fun Map<String, Any?>.int(k: String) = (this[k] as Double).toInt()
    private fun Map<String, Any?>.list(k: String) = this[k] as List<*>

    @Test fun everyLevelInfoIsWellFormedJson() {
        val session = GameSession(1L)
        for (level in 1..LevelCatalog.TOTAL_LEVELS) {
            val info = parse(session.levelInfo(level))
            assertEquals(level, info.int("id"))
            assertTrue((info["goal"] as String).isNotBlank(), "nivel $level sin meta")
        }
    }

    @Test fun startingLevelOneGivesAPlayableBoard() {
        val session = GameSession(7L)
        session.start(1)
        val st = session.state()
        assertEquals(1, st.int("level"))
        assertEquals("playing", st["status"])
        assertEquals(3, st.int("size"))
        assertEquals(2, st.list("tiles").size)
        assertEquals(0, st.int("moves"))
        assertNull(st["movesLeft"])
    }

    @Test fun stonesAreExposedAndTilesNeverSitOnThem() {
        val session = GameSession(3L)
        session.start(7)
        val st = session.state()
        val stones = st.list("stones").map { (it as List<*>).map { n -> (n as Double).toInt() } }
        assertEquals(listOf(listOf(0, 0)), stones)
        repeat(60) { session.move(it % 4) }
        val after = session.state()
        after.list("tiles").forEach { t ->
            val tile = t as Map<*, *>
            assertTrue(!(tile["r"] == 0.0 && tile["c"] == 0.0), "una ficha ocupa la piedra")
        }
    }

    @Test fun movingChangesTheBoardCountsMovesAndCanBeUndone() {
        val session = GameSession(11L)
        session.start(2)
        var moved = false
        for (d in 0..3) if (!moved) moved = session.move(d)
        assertTrue(moved)
        val st = session.state()
        assertEquals(1, st.int("moves"))
        assertEquals(true, st["canUndo"])
        assertTrue(session.undoMove())
        assertEquals(0, session.state().int("moves"))
        assertTrue(!session.undoMove(), "solo se deshace una jugada")
    }

    @Test fun moveLimitEndsTheGameWhenSpent() {
        val level = LevelCatalog.all().first { it.moveLimit != null && it.kind == LevelKind.SPRINT }
        val session = GameSession(5L)
        session.start(level.id)
        var guard = 0
        while (session.state()["status"] == "playing" && guard++ < 5000) session.move(guard % 4)
        val st = session.state()
        assertTrue(st["status"] != "playing", "la partida debía terminar")
        if (st["status"] == "lost") assertTrue(st["lostReason"] in listOf("out_of_moves", "board_full"))
    }

    @Test fun clockLevelsLoseWhenTheTimeRunsOut() {
        val level = LevelCatalog.all().first { it.timeLimitMs != null }
        val session = GameSession(9L)
        session.start(level.id)
        for (d in 0..3) if (session.move(d)) break      // el reloj empieza con la primera jugada
        session.tick(level.timeLimitMs!!.toInt() + 5_000)
        val st = session.state()
        assertEquals("lost", st["status"])
        assertEquals("time_up", st["lostReason"])
    }

    @Test fun blockedDirectionsDoNotSpendAMove() {
        val level = LevelCatalog.all().first { it.twist == Twist.NO_UP }
        val session = GameSession(2L)
        session.start(level.id)
        assertTrue(!session.move(0), "arriba está prohibido")
        val st = session.state()
        assertEquals(0, st.int("moves"))
        assertTrue((st["blocked"] as String).isNotBlank())
    }

    @Test fun hintsPointToAValidMoveAndFollowTheTutorial() {
        val session = GameSession(4L)
        session.tutorialEnabled = true
        session.start(1)
        val st = session.state()
        assertNotNull(st["coach"], "el tutorial debe hablar al empezar")
        val hint = parse(session.hint())
        assertTrue(hint.int("dir") in 0..3)
        assertTrue(session.move(hint.int("dir")), "la pista debe ser una jugada que mueva")
    }

    @Test fun playingWithHintsWinsLevelOneAndGivesStars() {
        var wins = 0
        for (seed in 1L..40L) {
            val session = GameSession(seed)
            session.start(1)
            var guard = 0
            while (session.state()["status"] == "playing" && guard++ < 600) {
                val h = session.hint()
                val dir = if (h.isEmpty()) guard % 4 else parse(h).int("dir")
                if (!session.move(dir)) session.move((dir + 1) % 4)
            }
            val st = session.state()
            if (st["status"] == "won") {
                wins++
                assertTrue(st.int("stars") in 1..3)
                assertEquals(1.0, st["progress"])
            }
        }
        assertTrue(wins >= 10, "con pistas se gana el nivel 1 casi siempre (ganó $wins de 40)")
    }

    @Test fun everyKindOfLevelCanBeStartedAndPlayedForAWhile() {
        val session = GameSession(13L)
        val firstOfKind = LevelCatalog.all(400).distinctBy { it.kind }
        for (spec in firstOfKind) {
            session.start(spec.id)
            repeat(40) { session.move(it % 4); session.tick(250) }
            val st = session.state()                    // no debe fallar ni producir JSON roto
            assertTrue(st.int("level") == spec.id)
            assertTrue(st.list("tiles").isNotEmpty() || st["status"] != "playing")
        }
    }

    /** Las claves que lee la app de iPhone (iosApp/ParDos/Models.swift): si falta una, Swift no puede decodificar el estado. */
    @Test fun snapshotAndLevelInfoCarryEveryKeyTheSwiftAppReads() {
        val session = GameSession(21L)
        session.start(1)
        val snapshotKeys = listOf(
            "level", "daily", "title", "kind", "kindLabel", "rule", "tip", "goal", "size", "progress", "score", "moves", "movesLeft", "timeLeftMs",
            "status", "lostReason", "stars", "tiles", "stones", "storm", "twist", "twistHint", "blocked", "phase", "phaseTitle", "chips",
            "coach", "coachKind", "coachDir", "coachCells", "coachDone", "coachNeeded", "tutorialDone", "canUndo", "merges", "maxTile", "elapsedMs", "powers", "label", "assist"
        )
        val st = session.state()
        snapshotKeys.forEach { assertTrue(it in st.keys, "falta '$it' en el estado") }
        (st.list("tiles").first() as Map<*, *>).let { t -> listOf("id", "v", "r", "c", "new", "merged").forEach { assertTrue(it in t.keys, "falta '$it' en la ficha") } }
        val info = parse(session.levelInfo(1))
        listOf("id", "title", "kind", "kindLabel", "rule", "goal", "size", "boss", "moveLimit", "timeMs", "chips", "tip", "threeStars")
            .forEach { assertTrue(it in info.keys, "falta '$it' en la tarjeta del nivel") }
    }

    @Test fun theDailyChallengeIsTheSameBoardForEveryone() {
        val day = 20_800
        fun board(): List<List<Int>> {
            val session = GameSession()          // semilla distinta en cada sesión: el reto manda la suya
            session.startDaily(day)
            val st = session.state()
            assertEquals(true, st["daily"])
            return st.list("tiles").map { t -> (t as Map<*, *>).let { listOf((it["v"] as Double).toInt(), (it["r"] as Double).toInt(), (it["c"] as Double).toInt()) } }
        }
        assertEquals(board(), board())
        // y también las fichas que van cayendo
        fun played(): String {
            val session = GameSession()
            session.startDaily(day)
            repeat(12) { session.move(it % 4) }
            return session.state().list("tiles").map { t -> (t as Map<*, *>).let { "${it["v"]}@${it["r"]},${it["c"]}" } }.sorted().joinToString()
        }
        assertEquals(played(), played())
    }

    @Test fun dailyInfoDescribesTheDaysChallengeAndNeverUsesTheTutorial() {
        val info = parse(GameSession().dailyInfo(20_801))
        assertTrue((info["goal"] as String).isNotBlank())
        val session = GameSession(3L)
        session.tutorialEnabled = true
        session.startDaily(20_801)
        assertNull(session.state()["coach"], "el reto diario no usa el tutorial aunque su número de nivel sea 1")
    }

    /** Swift decodifica con `Int`: un número entero escrito como "3.0" haría fallar toda la lectura del estado. */
    @Test fun integerFieldsAreWrittenWithoutDecimals() {
        val session = GameSession(33L)
        val intKeys = listOf("level", "size", "score", "moves", "stars", "phase", "movesLeft", "timeLeftMs", "coachDir", "coachDone", "coachNeeded", "merges", "maxTile", "elapsedMs")
        for (level in listOf(1, 8, 14, 20, 60, 100)) {
            session.tutorialEnabled = level == 1
            session.start(level)
            repeat(6) { session.move(it % 4) }
            val raw = session.snapshot()
            for (k in intKeys) {
                val m = Regex(""""$k":([^,}\]]+)""").find(raw) ?: continue
                val v = m.groupValues[1]
                assertTrue(v == "null" || Regex("""-?\d+""").matches(v), "'$k' debe ser entero y es '$v' (nivel $level)")
            }
            val tiles = Regex(""""[vrc]":([^,}]+)""").findAll(raw).map { it.groupValues[1] }.toList()
            assertTrue(tiles.all { Regex("""-?\d+""").matches(it) }, "fichas con decimales: $tiles")
        }
    }

    @Test fun powersChangeTheBoardOnlyWhileTheyAreAllowed() {
        val session = GameSession(77L)
        session.startCustom(size = 4, target = 4096, timeLimitMs = 0L, seed = 5L, levelNumber = 1, label = "Prueba", comboBonus = false, powers = true)
        repeat(30) { session.move(it % 4) }
        val before = session.state()
        if (before["status"] == "playing" && before.list("tiles").size > 3) {
            assertTrue(session.powerClean())
            assertEquals(3, session.state().list("tiles").size)
        }
        val s2 = GameSession(78L)
        s2.startDaily(20_800)          // el reto diario no deja usar poderes
        assertTrue(!s2.powerClean())
        assertTrue(!s2.powerMerge())
    }

    @Test fun broomAndLinkWorkOnChosenTiles() {
        val session = GameSession(3L)
        session.startCustom(size = 4, target = 4096, timeLimitMs = 0L, seed = 9L, levelNumber = 1, label = "", comboBonus = false, powers = true)
        val tiles = session.state().list("tiles").map { it as Map<*, *> }
        val first = tiles.first()["id"] as String
        assertTrue(session.powerBroom(first))
        assertTrue(session.state().list("tiles").none { (it as Map<*, *>)["id"] == first })
        assertTrue(!session.powerBroom("no-existe"))
        assertTrue(!session.powerLink("a", "b"))
    }

    @Test fun customGamesCarryTheirTimeAndLabel() {
        val session = GameSession(1L)
        session.startCustom(size = 5, target = 512, timeLimitMs = 90_000L, seed = 4L, levelNumber = 3, label = "Etapa 3", comboBonus = false, powers = false)
        val st = session.state()
        assertEquals(5, st.int("size"))
        assertEquals("Etapa 3", st["label"])
        assertEquals(90_000, st.int("timeLeftMs"))
        assertEquals(false, st["powers"])
        assertNull(st["coach"])
    }

    @Test fun assistedStartsGiveMoreRoomOnLimitedLevels() {
        val level = LevelCatalog.all().first { it.moveLimit != null }
        val plain = GameSession(1L)
        plain.start(level.id)
        val helped = GameSession(1L)
        helped.startAssisted(level.id, 130)
        assertTrue(helped.state().int("movesLeft") > plain.state().int("movesLeft"))
        assertEquals(130, helped.state().int("assist"))
    }
}
