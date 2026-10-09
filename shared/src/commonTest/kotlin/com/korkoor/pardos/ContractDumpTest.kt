package com.korkoor.pardos

import com.korkoor.pardos.domain.meta.MetaSession
import com.korkoor.pardos.domain.session.GameSession
import kotlin.test.Test

/**
 * Imprime muestras reales de cada JSON que lee Swift (con la marca CONTRACT). `iosApp/tools/check_swift_contract.py` las compara
 * con las estructuras `Decodable` de los archivos Swift de iosApp/ParDos: si falta una clave o un tipo no coincide, Swift no podría leer el estado.
 */
class ContractDumpTest {
    private fun dump(name: String, json: String) { println("CONTRACT\t$name\t$json") }

    @Test fun dumpSamples() {
        val day = 20_800
        val m = MetaSession()
        m.tick(day, day * 86_400_000L, 12 * 60)
        dump("open", m.openApp())
        dump("gift", m.claimDailyReward())
        m.claimFreeChest()
        dump("chestOpen", m.openChest("COMMON", 3L))
        dump("wheel", m.spinWheel(9L))
        for (n in 1..12) m.onWin(level = n, daily = false, stars = 3, moves = 12, timeMs = 30_000, maxTile = 128, merges = 40, usedHelp = false, kind = "ZEN", boss = false, flow = 0)
        dump("win", m.onWin(level = 20, daily = false, stars = 2, moves = 30, timeMs = 60_000, maxTile = 256, merges = 40, usedHelp = false, kind = "ZEN", boss = false, flow = 0))
        dump("loss", m.onLoss(level = 3, daily = false, maxTile = 64, merges = 12))
        val state = jsonObject(m.state())
        val mission = (state.list("missions").firstOrNull { (it as Map<*, *>)["done"] == true } as Map<*, *>?)
        dump("missionClaim", if (mission != null) m.claimMission((mission["id"] as Double).toInt()) else """{"ok":true,"coins":10,"allDone":false,"perfectGems":0}""")
        m.unlockSeasonPremium()
        dump("tierClaim", m.claimTier(1, false))
        dump("state", m.state())
        dump("albumState", m.albumState())
        dump("skinCatalog", m.skinCatalog())
        dump("fxCatalog", m.fxCatalog())
        dump("avatarCatalog", m.avatarCatalog())
        dump("bannerCatalog", m.bannerCatalog())
        dump("albumCatalog", m.albumCatalog())
        dump("seasonTiers", m.seasonTiers())
        dump("wheelSlices", m.wheelSlices())
        dump("economy", m.economyInfo())
        dump("store", m.storeProducts())
        dump("fail", m.buySkin("flat", 0))
        dump("assist", m.prepareLevel(5))
        dump("towerStart", m.towerStart())
        dump("towerWin", m.towerWin(128, 20, "ZEN", false, 0))
        dump("towerNext", m.towerNext())
        dump("towerLose", m.towerLose())
        dump("raceStage", m.raceStage(3))
        dump("raceFinish", m.raceFinish(3, 20, 128))
        dump("duelConfig", m.duelConfig())
        dump("duelResult", m.duelResult(100, 200))
        dump("records", m.records())
        dump("achCheck", m.checkAchievements(true, 1, 12, 20_000, 200, 3, 5, true, 3, "CLASICO", "2:0:0;16:0:1"))
        dump("achList", m.achievementsList())
        dump("prestige", m.prestigeState())
        dump("prestigeEvents", m.takePrestigeEvents())
        dump("reminders", m.reminders())
        dump("runs", m.recentRuns())
        val friend = MetaSession()
        friend.tick(day, day * 86_400_000L, 600)
        friend.setProfileName("Sol")
        m.addFriend(friend.friendCode())
        dump("friends", m.friendsJson())
        dump("friendAdd", m.addFriend(friend.friendCode()))
        dump("tables", m.tablesInfo())
        val seed = m.remoteNewSeed()
        val created = jsonObject(m.remoteFinishCreator(seed, 321, "Luna"))
        dump("remoteCreate", m.remoteFinishCreator(seed, 321, "Luna"))
        dump("remoteDecode", m.remoteDecode(created["text"] as String))
        dump("remoteChallenged", m.remoteFinishChallenged(seed, 321, "LUNA", 400))
        dump("remoteHistory", m.remoteHistory())
        dump("studioState", m.studioState())
        dump("studioPreview", m.studioPreview("GLASS", 100, 200, "PASTEL", "LIGHT", "SNOW"))

        // regreso tras ausencia larga y racha perdida por poco
        val m2 = MetaSession()
        m2.tick(day, day * 86_400_000L, 600)
        for (i in 0 until 5) { m2.tick(day + i, (day + i) * 86_400_000L, 600); m2.openApp() }
        m2.tick(day + 8, (day + 8) * 86_400_000L, 600)
        dump("openLate", m2.openApp())
        dump("stateLate", m2.state())
        val m3 = MetaSession()
        m3.tick(day + 20, (day + 20) * 86_400_000L, 600)
        dump("openComeback", m3.openApp())
        m3.tick(day + 40, (day + 40) * 86_400_000L, 600)
        dump("openComeback2", m3.openApp())

        // racha perdida por poco: ofrece recuperarla
        val m4 = MetaSession()
        m4.tick(day, day * 86_400_000L, 600)
        for (i in 0 until 5) { m4.tick(day + i, (day + i) * 86_400_000L, 600); m4.openApp() }
        m4.tick(day + 7, (day + 7) * 86_400_000L, 600)
        dump("openRepair", m4.openApp())
        dump("stateRepair", m4.state())

        // fiesta de Halloween activa (eventos, skin de evento, voz de temporada)
        val halloween = com.korkoor.pardos.domain.retention.Civil.toEpochDay(2026, 10, 28)
        val m5 = MetaSession()
        m5.tick(halloween, halloween * 86_400_000L, 700)
        m5.openApp()
        m5.onWin(level = 1, daily = false, stars = 3, moves = 10, timeMs = 1000, maxTile = 64, merges = 5, usedHelp = false, kind = "ZEN", boss = false, flow = 2)
        dump("stateHalloween", m5.state())
        dump("remindersHalloween", m5.reminders())

        // liga: una semana buena y cierre de semana
        val m6 = MetaSession()
        m6.tick(day, day * 86_400_000L, 600)
        m6.openApp()
        for (n in 1..10) m6.onWin(level = n, daily = false, stars = 3, moves = 10, timeMs = 1000, maxTile = 64, merges = 5, usedHelp = false, kind = "ZEN", boss = false, flow = 0)
        m6.tick(day + 8, (day + 8) * 86_400_000L, 600)
        m6.openApp()
        dump("stateLeague", m6.state())
        dump("prestigeLate", m6.prestigeState())
        dump("achListLate", m6.achievementsList())

        val g = GameSession(5L)
        g.tutorialEnabled = true
        g.start(1)
        dump("board", g.snapshot())
        g.move(3)
        dump("board2", g.snapshot())
        dump("levelCard", g.levelInfo(8))
        dump("dailyCard", g.dailyInfo(day))
        dump("hint", g.hint())
    }
}
