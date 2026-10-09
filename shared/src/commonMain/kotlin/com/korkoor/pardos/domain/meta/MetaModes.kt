package com.korkoor.pardos.domain.meta

import com.korkoor.pardos.domain.collection.AlbumBonus
import com.korkoor.pardos.domain.events.EventCalendar
import com.korkoor.pardos.domain.flow.AssistPolicy
import com.korkoor.pardos.domain.logic.DuelRules
import com.korkoor.pardos.domain.logic.RaceRules
import com.korkoor.pardos.domain.model.MissionType
import com.korkoor.pardos.domain.retention.WeeklyType
import com.korkoor.pardos.domain.tower.TowerRules

/** Modos de juego fuera de la campaña (Torre, Carrera, Duelo, Personalizado) y la ayuda por intentos. Mismas reglas que Android. */
internal class Modes(
    private val s: MetaStore,
    private val wallet: Wallet,
    private val col: CollectionOps,
    private val ret: Retention,
    private val clock: Clock
) {
    // ---------------- ayuda por intentos fallidos ----------------

    class Assist(val percent: Int, val message: String?, val tier: Int, val undos: Int)

    /** Antes de empezar un nivel: cuánta ayuda toca según los intentos fallidos, y regala los "Deshacer" que falten. */
    fun prepareLevel(level: Int): Assist {
        val assist = AssistPolicy.forAttempts(s.int("attempts_$level"))
        val grantedKey = "assist_granted_$level"
        val extra = AssistPolicy.newUndos(s.int(grantedKey), assist)
        if (extra > 0) {
            wallet.addUndos(extra)
            s.setInt(grantedKey, assist.totalFreeUndos)
        }
        return Assist((assist.limitFactor * 100).toInt(), assist.message, assist.tier, extra)
    }

    fun registerLevelFailure(level: Int) { s.addInt("attempts_$level", 1) }

    fun clearLevelAttempts(level: Int) {
        s.remove("attempts_$level")
        s.remove("assist_granted_$level")
    }

    // ---------------- Torre infinita ----------------

    val towerFloor: Int get() = s.int("tower_floor", 1)
    val towerHearts: Int get() = s.int("tower_hearts")
    val towerBest: Int get() = s.int("tower_best")
    val towerActive: Boolean get() = s.bool("tower_active")

    fun towerStart() {
        s.setInt("tower_floor", 1)
        s.setInt("tower_hearts", TowerRules.START_HEARTS)
        s.setInt("tower_run_coins", 0)
        s.setInt("tower_run_gems", 0)
        s.setInt("tower_record_start", towerBest)
        s.setBool("tower_active", true)
        touchTowerBest()
    }

    private fun touchTowerBest() {
        if (towerFloor > towerBest) s.setInt("tower_best", towerFloor)
    }

    fun towerLevelId(): Int = TowerRules.levelIdFor(towerFloor)

    class TowerClear(val coins: Int, val gems: Int, val heart: Boolean, val hearts: Int)

    /** Piso superado: se cobra al momento. El piso siguiente se abre con [towerNext]. */
    fun towerWin(maxTile: Int, merges: Int): TowerClear {
        val floor = towerFloor
        val reward = TowerRules.rewardFor(floor)
        wallet.addCoins(reward.coins)
        wallet.addGems(reward.gems)
        s.addInt("tower_run_coins", reward.coins)
        s.addInt("tower_run_gems", reward.gems)
        val before = towerHearts
        s.setInt("tower_hearts", TowerRules.heartsAfterClear(floor, before))
        sideGame(won = true, merges = merges, maxTile = maxTile)
        return TowerClear(reward.coins, reward.gems, reward.heart, towerHearts)
    }

    fun towerNext() {
        s.setInt("tower_floor", towerFloor + 1)
        touchTowerBest()
    }

    class TowerLoss(val hearts: Int, val over: Boolean, val newRecord: Boolean)

    /** En la torre perder cuesta un corazón (el piso se repite). Sin corazones la subida termina. */
    fun towerLose(): TowerLoss {
        val hearts = (towerHearts - 1).coerceAtLeast(0)
        s.setInt("tower_hearts", hearts)
        val over = hearts == 0
        val record = over && towerFloor > s.int("tower_record_start") && towerFloor > 1
        if (over) s.setBool("tower_active", false)
        return TowerLoss(hearts, over, record)
    }

    fun towerEnd() { s.setBool("tower_active", false) }

    fun towerJson(): String = obj(
        "floor" to towerFloor, "hearts" to towerHearts, "best" to towerBest, "levelId" to towerLevelId(),
        "boss" to TowerRules.isBossFloor(towerFloor), "label" to TowerRules.floorLabel(towerFloor),
        "runCoins" to s.int("tower_run_coins"), "runGems" to s.int("tower_run_gems"), "maxHearts" to TowerRules.MAX_HEARTS,
        "active" to towerActive
    )

    // ---------------- Carrera ----------------

    fun raceStageJson(n: Int): String {
        val st = RaceRules.stage(n)
        return obj("n" to st.number, "size" to st.boardSize, "target" to st.target, "bonusMs" to RaceRules.timeBonusMs(n),
            "startMs" to RaceRules.START_TIME_MS, "maxMs" to RaceRules.MAX_TIME_MS)
    }

    fun raceAfterStage(n: Int, remainingMs: Long): Long = RaceRules.timeAfterStage(remainingMs, n)

    class RaceEnd(val coins: Int, val best: Int, val newRecord: Boolean)

    fun raceStageCleared() {
        ret.updateMission(MissionType.WIN_LEVELS, 1)
        ret.addWeekly(WeeklyType.WIN_LEVELS, 1)
    }

    fun raceFinish(stages: Int, merges: Int, maxTile: Int): RaceEnd {
        val base = RaceRules.coinsFor(stages)
        var coins = EventCalendar.apply(base, AlbumBonus.combine(EventCalendar.coinMultiplier(clock.today), col.owned, col.foil))
        coins = wallet.applyWinBonuses(coins)
        wallet.addCoins(coins)
        val best = s.int("rec_race")
        val record = stages > best
        if (record) s.setInt("rec_race", stages)
        sideGame(won = stages > 0, merges = merges, maxTile = maxTile)
        return RaceEnd(coins, maxOf(best, stages), record)
    }

    // ---------------- Duelo local y partidas libres ----------------

    fun duelResultJson(score1: Int, score2: Int): String {
        val winner = DuelRules.winner(score1, score2)
        val best = s.int("rec_duel")
        val top = maxOf(score1, score2)
        if (top > best) s.setInt("rec_duel", top)
        return obj("ok" to true, "winner" to winner.name, "margin" to DuelRules.margin(score1, score2), "best" to maxOf(best, top))
    }

    fun duelConfigJson(): String = obj("size" to DuelRules.BOARD_SIZE, "target" to DuelRules.TARGET, "roundMs" to DuelRules.ROUND_MS)

    fun customFinished(score: Int, won: Boolean, merges: Int, maxTile: Int) {
        if (score > s.int("rec_custom")) s.setInt("rec_custom", score)
        sideGame(won, merges, maxTile)
    }

    /** Partidas que no son de campaña también cuentan para misiones y el pase (sin monedas por nivel). */
    fun sideGame(won: Boolean, merges: Int, maxTile: Int) {
        ret.onGameFinished(won = won, dailyChallenge = false)
        ret.updateMission(MissionType.PLAY_GAMES, 1)
        if (won) ret.updateMission(MissionType.WIN_LEVELS, 1)
        ret.updateMission(MissionType.MERGE_PAIRS, merges)
        ret.updateMission(MissionType.REACH_BLOCK, maxTile)
        ret.onTileReached(maxTile)
        ret.addWeekly(WeeklyType.MERGE_PAIRS, merges)
    }

    fun recordsJson(): String = obj(
        "tower" to towerBest, "race" to s.int("rec_race"), "duel" to s.int("rec_duel"), "custom" to s.int("rec_custom"),
        "bestStreak" to ret.bestStreak, "bestTile" to s.int("best_tile"), "totalWins" to s.int("total_wins"), "totalStars" to s.int("total_stars"),
        "daysPlayed" to s.int("days_played")
    )
}
