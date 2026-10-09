package com.korkoor.pardos.domain.meta

import com.korkoor.pardos.domain.collection.AlbumBonus
import com.korkoor.pardos.domain.economy.Economy
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

    // ---------------- Duelo a distancia (por código, sin servidor) ----------------

    private fun remotePlayed(): Set<String> = s.strSet("remote_played")

    private fun remoteHistory(): List<com.korkoor.pardos.domain.logic.RemoteDuelRecord> =
        com.korkoor.pardos.domain.logic.RemoteDuel.decodeHistory(s.str("remote_history").ifEmpty { null })

    fun remoteNewSeed(): Long = com.korkoor.pardos.domain.logic.RemoteDuel.normalizeSeed(clock.nowMs * 1_000_003L + s.int("remote_seed_n"))

    /** Lee un código dentro de cualquier texto pegado. */
    fun remoteDecodeJson(text: String): String {
        val c = com.korkoor.pardos.domain.logic.RemoteDuel.decode(text) ?: return obj("ok" to false, "reason" to "No encontré un código válido")
        val code = com.korkoor.pardos.domain.logic.RemoteDuel.encode(c)
        return obj(
            "ok" to true, "seed" to c.seed, "score" to c.score, "name" to c.name,
            "display" to com.korkoor.pardos.domain.logic.RemoteDuel.displayName(c.name), "played" to (code in remotePlayed())
        )
    }

    /** Fin del reto propio: paga lo de lanzar (hasta 3 al día) y arma el mensaje para compartir. */
    fun remoteFinishCreator(seed: Long, score: Int, name: String): String {
        val today = clock.today
        val count = if (s.int("remote_created_day", -1) == today) s.int("remote_created_count") else 0
        val reward = com.korkoor.pardos.domain.logic.RemoteDuel.createReward(count)
        wallet.addCoins(reward.coins)
        s.setInt("remote_created_day", today)
        s.setInt("remote_created_count", count + 1)
        val challenge = com.korkoor.pardos.domain.logic.RemoteChallenge(seed, score, name)
        val text = com.korkoor.pardos.domain.logic.RemoteDuel.shareText(challenge, "ParDos para iPhone y Android")
        sideGame(won = false, merges = 0, maxTile = 0)
        return obj("ok" to true, "coins" to reward.coins, "code" to com.korkoor.pardos.domain.logic.RemoteDuel.encode(challenge), "text" to text)
    }

    /** Fin de un reto recibido: cada código solo paga y cuenta la primera vez que se juega. */
    fun remoteFinishChallenged(seed: Long, theirScore: Int, name: String, myScore: Int): String {
        val challenge = com.korkoor.pardos.domain.logic.RemoteChallenge(seed, theirScore, name)
        val code = com.korkoor.pardos.domain.logic.RemoteDuel.encode(challenge)
        val outcome = com.korkoor.pardos.domain.logic.RemoteDuel.outcome(myScore, theirScore)
        val first = code !in remotePlayed()
        var coins = 0
        var gems = 0
        if (first) {
            val reward = com.korkoor.pardos.domain.logic.RemoteDuel.rewardFor(outcome)
            coins = reward.coins
            gems = reward.gems
            wallet.addCoins(coins)
            wallet.addGems(gems)
            val record = com.korkoor.pardos.domain.logic.RemoteDuelRecord(
                com.korkoor.pardos.domain.logic.RemoteDuel.displayName(name), myScore, theirScore, outcome, clock.today
            )
            s.setStr("remote_history", com.korkoor.pardos.domain.logic.RemoteDuel.encodeHistory((listOf(record) + remoteHistory()).take(30)))
            s.addToStrSet("remote_played", code)
        }
        sideGame(won = outcome == com.korkoor.pardos.domain.logic.RemoteOutcome.WIN, merges = 0, maxTile = 0)
        return obj("ok" to true, "outcome" to outcome.name, "coins" to coins, "gems" to gems, "firstTime" to first, "mine" to myScore, "theirs" to theirScore)
    }

    fun remoteHistoryJson(): String {
        val history = remoteHistory()
        val st = com.korkoor.pardos.domain.logic.RemoteDuel.stats(history)
        return obj(
            "history" to history.map { robj("opponent" to it.opponent, "mine" to it.myScore, "theirs" to it.theirScore, "outcome" to it.outcome.name, "day" to it.day) },
            "wins" to st.wins, "losses" to st.losses, "ties" to st.ties, "streak" to st.currentStreak, "best" to st.bestStreak,
            "createdToday" to (if (s.int("remote_created_day", -1) == clock.today) s.int("remote_created_count") else 0),
            "createLimit" to Economy.REMOTE_DUEL_CREATE_PER_DAY
        )
    }

    fun recordsJson(): String = obj(
        "tower" to towerBest, "race" to s.int("rec_race"), "duel" to s.int("rec_duel"), "custom" to s.int("rec_custom"),
        "bestStreak" to ret.bestStreak, "bestTile" to s.int("best_tile"), "totalWins" to s.int("total_wins"), "totalStars" to s.int("total_stars"),
        "daysPlayed" to s.int("days_played")
    )
}
