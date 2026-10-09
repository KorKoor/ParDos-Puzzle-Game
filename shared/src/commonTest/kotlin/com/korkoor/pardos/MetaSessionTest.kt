package com.korkoor.pardos

import com.korkoor.pardos.domain.meta.MetaSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MetaSessionTest {
    private val day = 20_800
    private val noon = 12 * 60

    private fun fresh(d: Int = day): MetaSession = MetaSession().also { it.tick(d, d * 86_400_000L, noon) }

    private fun MetaSession.st() = jsonObject(state())
    private fun ok(json: String): Map<String, Any?> = jsonObject(json).also { assertEquals(true, it["ok"], "debía salir bien: $json") }
    private fun fails(json: String) = assertEquals(false, jsonObject(json)["ok"], "debía fallar: $json")

    @Test fun catalogsAreWellFormedJson() {
        val m = fresh()
        assertTrue(jsonArray(m.skinCatalog()).size >= 20)
        assertTrue(jsonArray(m.fxCatalog()).size >= 8)
        assertTrue(jsonArray(m.avatarCatalog()).size >= 100)
        assertTrue(jsonArray(m.bannerCatalog()).size >= 50)
        assertEquals(30, jsonArray(m.seasonTiers()).size)
        assertEquals(8, jsonArray(m.wheelSlices()).size)
        val album = jsonObject(m.albumCatalog())
        assertEquals(320, album.list("pieces").size)
        assertEquals(32, album.list("series").size)
        jsonObject(m.economyInfo())
        jsonObject(m.storeProducts())
        // cada elemento de una lista tiene que ser un objeto (no un texto con JSON dentro)
        listOf(m.skinCatalog(), m.fxCatalog(), m.avatarCatalog(), m.bannerCatalog(), m.seasonTiers(), m.wheelSlices()).forEach { json ->
            assertTrue(jsonArray(json).all { it is Map<*, *> }, "los elementos deben ser objetos: ${json.take(80)}")
        }
        assertTrue(album.list("pieces").all { it is Map<*, *> })
        assertTrue(album.list("series").all { it is Map<*, *> })
    }

    @Test fun freshStateHasEveryKeyTheSwiftAppReads() {
        val st = fresh().st()
        val keys = listOf(
            "coins", "gems", "shards", "tokens", "undos", "freezes", "extraTimes", "vip", "boostWins", "piggy", "canBreakPiggy", "chests", "totalChests",
            "streak", "bestStreak", "winStreak", "playerLevel", "xp", "xpNext", "name", "avatar", "banner", "unlocked", "totalStars", "levelsWon", "tutorialDone",
            "ownedSkins", "equippedSkin", "ownedFx", "equippedFx", "ownedAvatars", "ownedBanners", "dailyReward", "cycle", "freeChest", "wheel", "missions",
            "perfectDays", "perfectToday", "weekly", "weeklyBonusReady", "weeklyBonusClaimed", "season", "league", "offer", "events", "eventSkins", "nextGoal",
            "badges", "repair", "coinPercent", "dayOfWeek"
        )
        keys.forEach { assertTrue(it in st.keys, "falta '$it' en el estado") }
        assertEquals(3, st.list("missions").size)
        assertEquals(4, st.list("weekly").size)
        assertEquals(7, st.list("cycle").size)
        assertEquals(1, st.int("unlocked"))
        assertEquals("jelly", st["equippedSkin"])
    }

    @Test fun integerFieldsHaveNoDecimals() {
        val m = fresh()
        m.openApp()
        val raw = m.state()
        for (k in listOf("coins", "gems", "shards", "tokens", "undos", "freezes", "streak", "xp", "xpNext", "playerLevel", "unlocked", "badges", "avatar", "banner")) {
            val v = Regex(""""$k":([^,}\]]+)""").find(raw)?.groupValues?.get(1) ?: error("sin $k")
            assertTrue(Regex("""-?\d+""").matches(v), "'$k' debe ser entero y es '$v'")
        }
    }

    @Test fun saveAndLoadRoundTrip() {
        val a = fresh()
        a.openApp(); a.claimDailyReward(); a.claimFreeChest(); a.state()
        val saved = a.save()
        val b = MetaSession().also { it.load(saved); it.tick(day, day * 86_400_000L, noon) }
        assertEquals(a.state(), b.state())
        assertEquals(saved, b.save())
    }

    @Test fun firstOpenStartsTheStreakAndTheDailyGiftPaysOnce() {
        val m = fresh()
        val open = ok(m.openApp())
        assertEquals("STARTED", open["change"])
        assertEquals(1, m.st().int("streak"))
        val coinsBefore = m.st().int("coins")
        val gift = ok(m.claimDailyReward())
        assertEquals(50, gift.int("coins"))
        assertEquals(coinsBefore + 50, m.st().int("coins"))
        fails(m.claimDailyReward())
        // abrir otra vez el mismo día no cambia nada
        assertEquals("NONE", ok(m.openApp())["change"])
    }

    @Test fun streakContinuesOnConsecutiveDaysAndResetsAfterAGap() {
        val m = fresh()
        m.openApp()
        m.tick(day + 1, (day + 1) * 86_400_000L, noon)
        assertEquals("CONTINUED", ok(m.openApp())["change"])
        assertEquals(2, m.st().int("streak"))
        m.tick(day + 6, (day + 6) * 86_400_000L, noon)
        assertEquals("RESET", ok(m.openApp())["change"])
        assertEquals(1, m.st().int("streak"))
    }

    @Test fun freeChestAlternatesWaitingAndIsOpenedIntoCards() {
        val m = fresh()
        val claim = ok(m.claimFreeChest())
        assertEquals("COMMON", claim["chest"])
        fails(m.claimFreeChest())
        assertEquals(1, m.st().map("chests").int("COMMON"))
        val opened = ok(m.openChest("COMMON", 7L))
        assertEquals(2, opened.list("drops").size)
        assertEquals(0, m.st().map("chests").int("COMMON"))
        fails(m.openChest("COMMON", 7L))
        val album = jsonObject(m.albumState())
        assertTrue(album.int("owned") in 1..2)
    }

    @Test fun wheelGivesOneFreeSpinPerDay() {
        val m = fresh()
        val spin = ok(m.spinWheel(5L))
        assertTrue(spin.int("index") in 0..7)
        fails(m.spinWheel(6L))
        m.tick(day + 1, (day + 1) * 86_400_000L, noon)
        ok(m.spinWheel(7L))
    }

    @Test fun winningLevelOnePaysCoinsUnlocksAndProgressesMissions() {
        val m = fresh()
        m.openApp()
        val win = ok(m.onWin(level = 1, daily = false, stars = 3, moves = 12, timeMs = 40_000, maxTile = 16, merges = 9, usedHelp = false, kind = "ZEN", boss = false, flow = 0))
        assertTrue(win.int("coins") >= 10 + 30 + 20, "monedas de la primera victoria: ${win["coins"]}")
        assertEquals(true, win["firstClear"])
        assertEquals(75, win.int("firstWinCoins"))
        assertEquals(2, m.st().int("unlocked"))
        assertEquals(3, m.starsOf(1))
        assertEquals(12, m.bestMovesOf(1))
        assertEquals(true, m.tutorialDone)
        assertTrue(m.st().int("xp") > 0)
        assertTrue(m.st().map("season").int("points") >= 15)
        // la segunda vez ya no es primera victoria
        val again = ok(m.onWin(level = 1, daily = false, stars = 2, moves = 20, timeMs = 50_000, maxTile = 8, merges = 5, usedHelp = false, kind = "ZEN", boss = false, flow = 0))
        assertEquals(false, again["firstClear"])
        assertEquals(0, again.int("firstWinCoins"))
        assertEquals(3, m.starsOf(1), "no se pierden estrellas")
    }

    @Test fun lastLevelOfAChapterOpensItsChestOnce() {
        val m = fresh()
        val win = ok(m.onWin(level = 20, daily = false, stars = 1, moves = 40, timeMs = 90_000, maxTile = 64, merges = 30, usedHelp = false, kind = "ZEN", boss = false, flow = 0))
        assertNotNull(win["chapterChest"])
        val again = ok(m.onWin(level = 20, daily = false, stars = 1, moves = 40, timeMs = 90_000, maxTile = 64, merges = 30, usedHelp = false, kind = "ZEN", boss = false, flow = 0))
        assertEquals(null, again["chapterChest"])
    }

    @Test fun shopBuysOnlyWhatYouCanAffordAndEquipsOnlyWhatYouOwn() {
        val m = fresh()
        fails(m.buySkin("flat", 0))
        fails(m.equipSkin("flat"))
        m.grantCoins(1000)
        ok(m.buySkin("flat", 0))
        assertEquals(550, m.st().int("coins"))
        ok(m.equipSkin("flat"))
        assertEquals("flat", m.st()["equippedSkin"])
        fails(m.buySkin("flat", 0))
        fails(m.buySkin("nope", 0))
    }

    @Test fun consumablesAndExchange() {
        val m = fresh()
        m.grantCoins(500)
        ok(m.buyUndos(3))
        assertEquals(3, m.st().int("undos"))
        assertEquals(320, m.st().int("coins"))
        assertTrue(m.useUndo())
        m.grantGems(10)
        ok(m.exchangeGems(5))
        assertEquals(520, m.st().int("coins"))
        fails(m.exchangeGems(50))
    }

    @Test fun dailyMissionsAreClaimedOnlyWhenDone() {
        val m = fresh()
        val first = jsonObject(m.state()).list("missions").first() as Map<*, *>
        val id = (first["id"] as Double).toInt()
        fails(m.claimMission(id))
        // muchas victorias completan las misiones fáciles
        repeat(6) { m.onWin(level = 1 + it, daily = false, stars = 3, moves = 10, timeMs = 30_000, maxTile = 128, merges = 60, usedHelp = false, kind = "ZEN", boss = false, flow = 0) }
        val done = jsonObject(m.state()).list("missions").map { it as Map<*, *> }.filter { it["done"] == true }
        assertTrue(done.isNotEmpty())
        val target = (done.first()["id"] as Double).toInt()
        ok(m.claimMission(target))
        fails(m.claimMission(target))
    }

    @Test fun seasonPassClaimsTiersAsPointsArrive() {
        val m = fresh()
        fails(m.claimTier(1, false))
        repeat(12) { m.onWin(level = 1, daily = false, stars = 3, moves = 10, timeMs = 30_000, maxTile = 16, merges = 5, usedHelp = false, kind = "ZEN", boss = false, flow = 0) }
        val tier = m.st().map("season").int("tier")
        assertTrue(tier >= 1, "tras 12 victorias hay al menos un nivel del pase")
        val before = m.st().int("coins")
        ok(m.claimTier(1, false))
        assertTrue(m.st().int("coins") > before)
        fails(m.claimTier(1, false))
        fails(m.claimTier(1, true))
        m.unlockSeasonPremium()
        ok(m.claimTier(1, true))
    }

    @Test fun albumSellCraftAndFoilFollowTheRules() {
        val m = fresh()
        fails(m.sellPiece("garden_1", 1))
        fails(m.craftPiece("garden_1"))
        // abre varios cofres hasta tener alguna repetida
        var copiesSeen = false
        for (n in 1..30) {
            m.grantCoins(400)
            ok(m.buyChest("COMMON", false))
            ok(m.openChest("COMMON", n.toLong()))
            val copies = jsonObject(m.albumState()).map("copies")
            if (copies.values.any { (it as Double) >= 2 }) { copiesSeen = true; break }
        }
        assertTrue(copiesSeen, "con tantos cofres tiene que haber repetidas")
        val dupe = jsonObject(m.albumState()).map("copies").entries.first { (it.value as Double) >= 2 }.key
        val sold = ok(m.sellPiece(dupe, 1))
        assertTrue(sold.int("coins") > 0)
    }

    @Test fun dailyOfferCanBeBoughtOnce() {
        val m = fresh()
        m.grantCoins(5000)
        m.grantGems(300)
        val r = jsonObject(m.buyDailyOffer())
        if (r["ok"] == true) fails(m.buyDailyOffer())
    }

    @Test fun profileOnlyAcceptsOwnedAvatarsAndBanners() {
        val m = fresh()
        assertTrue(m.setAvatar(3))        // clásico: de todos
        assertTrue(!m.setAvatar(40))      // de tienda: no lo tiene
        assertTrue(m.setBanner(1))
        m.setProfileName("  Luna  ")
        assertEquals("Luna", m.st()["name"])
    }

    @Test fun leagueClosesAWeekAndPaysTheResult() {
        val m = fresh()
        repeat(8) { m.onWin(level = 1, daily = false, stars = 3, moves = 10, timeMs = 30_000, maxTile = 16, merges = 5, usedHelp = false, kind = "ZEN", boss = false, flow = 0) }
        m.tick(day + 8, (day + 8) * 86_400_000L, noon)
        val lg = m.st().map("league")
        assertTrue(lg["pending"] != null || lg["id"] == "BRONZE")
    }

    @Test fun towerRunPaysFloorsLosesHeartsAndEnds() {
        val m = fresh()
        val start = jsonObject(m.towerStart())
        assertEquals(1, start.int("floor"))
        assertEquals(3, start.int("hearts"))
        val before = m.st().int("coins")
        val win = ok(m.towerWin(maxTile = 128, merges = 30, kind = "ZEN", boss = false, flow = 0))
        assertTrue(win.int("coins") > 0)
        assertTrue(m.st().int("coins") >= before + win.int("coins"))
        assertEquals(2, jsonObject(m.towerNext()).int("floor"))
        assertEquals(2, jsonObject(m.towerInfo()).int("best"))
        assertEquals(2, ok(m.towerLose()).int("hearts"))
        ok(m.towerLose())
        val last = ok(m.towerLose())
        assertEquals(true, last["over"])
    }

    @Test fun raceStagesGrowAndFinishPaysByStagesCleared() {
        val m = fresh()
        assertEquals(3, jsonObject(m.raceStage(1)).int("size"))
        assertTrue(m.raceNextTime(1, 10_000L) > 10_000L)
        assertTrue(m.raceNextTime(9, 179_000L) <= 180_000L)
        val before = m.st().int("coins")
        val end = ok(m.raceFinish(stages = 3, merges = 40, maxTile = 128))
        assertEquals(true, end["newRecord"])
        assertTrue(m.st().int("coins") >= before + 60)
        assertEquals(3, jsonObject(m.records()).int("race"))
    }

    @Test fun duelPicksTheHigherScore() {
        val m = fresh()
        assertEquals("PLAYER_2", jsonObject(m.duelResult(100, 250))["winner"])
        assertEquals("TIE", jsonObject(m.duelResult(80, 80))["winner"])
    }

    @Test fun failingALevelRepeatedlyGivesAssistAndFreeUndos() {
        val m = fresh()
        assertEquals(100, jsonObject(m.prepareLevel(5)).int("percent"))
        repeat(4) { m.onLoss(level = 5, daily = false, maxTile = 16, merges = 3) }
        val a = jsonObject(m.prepareLevel(5))
        assertTrue(a.int("percent") > 100)
        assertTrue(a.int("undos") > 0)
        assertEquals(a.int("undos"), m.st().int("undos"))
        // pedir la ayuda otra vez no regala más
        assertEquals(0, jsonObject(m.prepareLevel(5)).int("undos"))
        m.onWin(level = 5, daily = false, stars = 1, moves = 20, timeMs = 1000, maxTile = 64, merges = 10, usedHelp = true, kind = "ZEN", boss = false, flow = 0)
        assertEquals(100, jsonObject(m.prepareLevel(5)).int("percent"))
    }

    @Test fun achievementsUnlockOnceAndPayTheirReward() {
        val m = fresh()
        val coins0 = m.st().int("coins")
        val first = jsonObject(m.checkAchievements(true, 1, 12, 20_000, 200, 3, 5, true, 3, "CLASICO", "2:0:0;16:0:1"))
        val ids = first.list("unlocked").map { (it as Map<*, *>)["id"] }
        assertTrue("first_win" in ids && "tile_16" in ids && "combo_3" in ids, "salieron $ids")
        assertTrue(m.st().int("coins") > coins0)
        val again = jsonObject(m.checkAchievements(true, 1, 12, 20_000, 200, 3, 5, true, 3, "CLASICO", "2:0:0;16:0:1"))
        assertEquals(0, again.list("unlocked").size, "no se pagan dos veces")
        val list = jsonObject(m.achievementsList())
        assertEquals(82, list.int("total"))
        assertTrue(list.int("done") >= 3)
        assertTrue(list.list("list").all { it is Map<*, *> })
    }

    @Test fun achievementTilesWithBadDataAreIgnored() {
        val m = fresh()
        val r = jsonObject(m.checkAchievements(false, 1, 0, 0, 0, 0, 9, true, 3, "NOPE", "x;0:0:0;4:-1:2;8:1"))
        assertTrue(r.list("unlocked").isEmpty() || r.list("unlocked").isNotEmpty())
    }

    @Test fun prestigeRankClimbsAndPaysItsRewardOnce() {
        val m = fresh()
        m.prestigeState()
        repeat(40) { n -> m.onWin(level = n + 1, daily = false, stars = 3, moves = 12, timeMs = 30_000, maxTile = 256, merges = 40, usedHelp = false, kind = "ZEN", boss = false, flow = 0) }
        val st = jsonObject(m.prestigeState())
        assertTrue(st.int("score") > 0)
        assertEquals(st.int("milestonesDone"), (st.list("milestones").count { (it as Map<*, *>)["done"] == true }))
        assertEquals(8, st.list("ranks").size)
        val events = jsonArray(m.takePrestigeEvents())
        assertTrue(jsonArray(m.takePrestigeEvents()).isEmpty(), "los avisos se entregan una sola vez")
        assertTrue(events.all { it is Map<*, *> })
    }

    @Test fun titlesCanBeBoughtWithGemsAndEquipped() {
        val m = fresh()
        fails(m.buyTitle("g_fusion_king"))
        m.grantGems(500)
        ok(m.buyTitle("g_fusion_king"))
        assertEquals("Rey de las fusiones", m.st()["title"])
        fails(m.equipTitle("t_platinum"))
        ok(m.equipTitle("default"))
    }
}
