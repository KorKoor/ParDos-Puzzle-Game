package com.korkoor.pardos.domain.meta

import com.korkoor.pardos.domain.collection.ChestType
import com.korkoor.pardos.domain.collection.CollectibleCatalog
import com.korkoor.pardos.domain.economy.Economy
import com.korkoor.pardos.domain.events.EventCalendar
import com.korkoor.pardos.domain.events.GameEvent
import com.korkoor.pardos.domain.model.DailyMission
import com.korkoor.pardos.domain.model.DailyMissionPlan
import com.korkoor.pardos.domain.model.MissionPool
import com.korkoor.pardos.domain.model.MissionType
import com.korkoor.pardos.domain.model.PerfectDays
import com.korkoor.pardos.domain.retention.Comeback
import com.korkoor.pardos.domain.retention.ComebackGift
import com.korkoor.pardos.domain.retention.DailyWheel
import com.korkoor.pardos.domain.retention.FreeChest
import com.korkoor.pardos.domain.retention.GoalInput
import com.korkoor.pardos.domain.retention.League
import com.korkoor.pardos.domain.retention.LeagueOutcome
import com.korkoor.pardos.domain.retention.LeagueResult
import com.korkoor.pardos.domain.retention.Leagues
import com.korkoor.pardos.domain.retention.NextGoal
import com.korkoor.pardos.domain.retention.NextGoals
import com.korkoor.pardos.domain.retention.PiggyBank
import com.korkoor.pardos.domain.retention.PlayerLevelRewards
import com.korkoor.pardos.domain.retention.SeasonCalendar
import com.korkoor.pardos.domain.retention.SeasonPass
import com.korkoor.pardos.domain.retention.SeasonPoints
import com.korkoor.pardos.domain.retention.SeasonReward
import com.korkoor.pardos.domain.retention.WeeklyMission
import com.korkoor.pardos.domain.retention.WeeklyMissions
import com.korkoor.pardos.domain.retention.WeeklyType
import com.korkoor.pardos.domain.retention.WheelKind
import com.korkoor.pardos.domain.retention.WheelSlice
import com.korkoor.pardos.domain.rewards.DailyRewards
import com.korkoor.pardos.domain.rewards.Reward
import com.korkoor.pardos.domain.rewards.StreakCalculator
import com.korkoor.pardos.domain.rewards.StreakChange
import com.korkoor.pardos.domain.rewards.StreakRepair
import com.korkoor.pardos.domain.rewards.StreakState
import com.korkoor.pardos.domain.shop.DayPart
import com.korkoor.pardos.domain.shop.EventSkins
import com.korkoor.pardos.domain.shop.HiddenSkins
import com.korkoor.pardos.domain.shop.PlayerStats
import com.korkoor.pardos.domain.shop.TileSkin
import com.korkoor.pardos.domain.social.WeekCalendar
import kotlin.random.Random

/** Reloj de la partida de meta: lo fija Swift antes de cada llamada (día local desde 1970, milisegundos y minuto del día). */
internal class Clock {
    var today: Int = 0
    var nowMs: Long = 0L
    var minute: Int = 0
}

internal class WeeklyView(val mission: WeeklyMission, val progress: Int, val claimed: Boolean) {
    val done: Boolean get() = progress >= mission.target
}

internal class GameBonus(val firstWinCoins: Int, val piggyGems: Int, val seasonPoints: Int, val newSkins: List<TileSkin>)

/** Todo lo que hace volver al jugador (mismas reglas que RetentionManager, ProfileManager y MissionManager de Android). */
internal class Retention(
    private val s: MetaStore,
    private val wallet: Wallet,
    private val col: CollectionOps,
    private val clock: Clock
) {
    private val today: Int get() = clock.today
    private val week: Int get() = WeekCalendar.weekId(clock.today)

    // ============================ Racha de días ============================

    val streak: Int get() = s.int("streak")
    val bestStreak: Int get() = s.int("best_streak")

    class CheckIn(
        val change: StreakChange, val streak: Int, val comeback: ComebackGift?, val daysAway: Int,
        val milestone: Economy.StreakMilestone?, val tokens: Int, val repairLost: Int
    )

    /** Se llama al abrir la app. Una vez al día: racha, escudos, hitos, login, ficha de intercambio y regalo de regreso. */
    fun checkIn(): CheckIn {
        rolloverSeasonIfNeeded()
        rolloverLeagueIfNeeded()
        val lastOpen = s.int("last_open_day", -1)
        val firstToday = lastOpen != today
        val before = StreakState(streak, bestStreak, s.int("streak_last"))
        val result = StreakCalculator.onOpen(before, today, wallet.freezes)
        var milestone: Economy.StreakMilestone? = null
        var repairLost = 0
        if (result.change == StreakChange.RESET && StreakRepair.canOffer(before, today)) {
            s.setInt("repair_day", today)
            s.setInt("repair_streak", before.streak)
            s.setInt("repair_best", before.best)
            repairLost = before.streak
        }
        if (result.change != StreakChange.NONE) {
            s.setInt("streak", result.state.streak)
            s.setInt("best_streak", result.state.best)
            s.setInt("streak_last", result.state.lastDay)
            wallet.setFreezes(result.freezesLeft)
            milestone = claimStreakMilestone(result.state.streak)
        }
        if (!firstToday) return CheckIn(result.change, streak, null, 0, milestone, 0, repairLost)

        val away = if (lastOpen < 0) 0 else today - lastOpen
        s.setInt("last_open_day", today)
        addSeasonPoints(SeasonPoints.DAILY_LOGIN)
        val tokens = col.onDailyCheckIn(today)
        addWeekly(WeeklyType.LOGIN_DAYS, 1)
        s.addInt("days_played", 1)
        val gift = Comeback.giftFor(away)
        if (gift != null) {
            wallet.addCoins(gift.coins)
            col.addChests(gift.chest, 1)
            wallet.addFreezes(gift.freezes)
        }
        return CheckIn(result.change, streak, gift, away, milestone, tokens, repairLost)
    }

    private fun claimStreakMilestone(streakNow: Int): Economy.StreakMilestone? {
        val m = Economy.milestoneFor(streakNow) ?: return null
        val key = "streak_milestone_${m.days}"
        if (s.bool(key)) return null
        s.setBool(key, true)
        wallet.addCoins(m.coins)
        wallet.addGems(m.gems)
        m.chest?.let { col.addChests(it, 1) }
        return m
    }

    // ---- recuperar una racha perdida por poco ----

    fun pendingRepair(): Int = if (s.int("repair_day", -1) == today) s.int("repair_streak") else 0

    fun repairCost(): Int = StreakRepair.gemCost(pendingRepair())

    fun repairStreak(): Boolean {
        val lost = pendingRepair()
        if (lost <= 0) return false
        if (!wallet.spendGems(repairCost())) return false
        val fixed = StreakRepair.repaired(StreakState(lost, s.int("repair_best", lost), today), today)
        s.setInt("streak", fixed.streak)
        s.setInt("best_streak", fixed.best)
        s.setInt("streak_last", fixed.lastDay)
        s.remove("repair_day")
        claimStreakMilestone(fixed.streak)
        return true
    }

    fun declineRepair() { s.remove("repair_day") }

    // ============================ Regalo diario ============================

    fun dailyRewardClaimable(): Boolean = s.int("daily_reward_day") != today
    fun dailyRewardStreak(): Int = streak.coerceAtLeast(1)
    fun dailyRewardPending(): Reward? = if (dailyRewardClaimable()) DailyRewards.rewardForStreak(dailyRewardStreak()) else null

    fun claimDailyReward(): Reward? {
        val r = dailyRewardPending() ?: return null
        s.setInt("daily_reward_day", today)
        wallet.addCoins(r.coins)
        wallet.addGems(r.gems)
        return r
    }

    // ============================ Cofre gratis ============================

    fun freeChestRemainingMs(): Long = FreeChest.remainingMs(s.long("free_chest_last"), clock.nowMs)
    fun freeChestReady(): Boolean = freeChestRemainingMs() == 0L
    fun nextFreeChestType(): ChestType = FreeChest.typeFor(s.int("free_chest_count"))

    fun claimFreeChest(): ChestType? {
        if (!freeChestReady()) return null
        val count = s.int("free_chest_count")
        val type = FreeChest.typeFor(count)
        col.addChests(type, 1)
        s.setLong("free_chest_last", clock.nowMs)
        s.setInt("free_chest_count", count + 1)
        addSeasonPoints(SeasonPoints.FREE_CHEST)
        return type
    }

    fun skipFreeChestWithGems(): Boolean {
        val cost = FreeChest.skipCostGems(freeChestRemainingMs())
        if (cost <= 0 || !wallet.spendGems(cost)) return false
        s.setLong("free_chest_last", 0L)
        return true
    }

    // ============================ Ruleta ============================

    private fun wheelUsed(key: String): Int = if (s.int("wheel_day", -1) == today) s.int(key) else 0

    fun wheelAllowance(): DailyWheel.Allowance = DailyWheel.allowance(wheelUsed("wheel_free"), wheelUsed("wheel_ad"))

    /** Gira, entrega el premio y devuelve la casilla ganadora (null si no quedan giros gratis). */
    fun spinWheel(random: Random): Int? {
        if (wheelAllowance().freeLeft <= 0) return null
        s.setInt("wheel_free", wheelUsed("wheel_free") + 1)
        s.setInt("wheel_ad", wheelUsed("wheel_ad"))
        s.setInt("wheel_day", today)
        val index = DailyWheel.pick(random)
        applyWheelPrize(DailyWheel.slices[index])
        return index
    }

    private fun applyWheelPrize(slice: WheelSlice) {
        when (slice.kind) {
            WheelKind.COINS -> wallet.addCoins(slice.amount)
            WheelKind.GEMS -> wallet.addGems(slice.amount)
            WheelKind.CHEST -> col.addChests(slice.chest ?: ChestType.COMMON, slice.amount)
            WheelKind.SEASON_POINTS -> addSeasonPoints(slice.amount)
        }
        addSeasonPoints(SeasonPoints.WHEEL_SPIN)
    }

    // ============================ Pase de temporada ============================

    val seasonId: Int get() = SeasonCalendar.seasonId(today)
    val seasonPoints: Int get() = s.int("season_points")
    val seasonPremium: Boolean get() = s.bool("season_premium")
    private val seasonClaimed: Set<String> get() = s.strSet("season_claimed")

    fun rolloverSeasonIfNeeded() {
        val current = seasonId
        if (s.int("season_id", -1) != current) {
            s.setInt("season_id", current)
            s.setInt("season_points", 0)
            s.setBool("season_premium", false)
            s.setStrSet("season_claimed", emptyList())
        }
    }

    fun addSeasonPoints(points: Int) {
        if (points <= 0) return
        rolloverSeasonIfNeeded()
        s.setInt("season_points", (seasonPoints + points).coerceAtMost(SeasonPass.TOTAL_POINTS))
    }

    fun unlockPremium() {
        rolloverSeasonIfNeeded()
        s.setBool("season_premium", true)
    }

    private fun claimKey(tier: Int, premium: Boolean) = (if (premium) "p" else "f") + tier

    fun isTierClaimed(tier: Int, premium: Boolean): Boolean = claimKey(tier, premium) in seasonClaimed

    fun canClaimTier(tier: Int, premium: Boolean): Boolean =
        tier in 1..SeasonPass.TIERS && tier <= SeasonPass.tierFor(seasonPoints) &&
            (!premium || seasonPremium) && !isTierClaimed(tier, premium)

    fun claimTier(tier: Int, premium: Boolean): SeasonReward? {
        if (!canClaimTier(tier, premium)) return null
        val reward = if (premium) SeasonPass.premiumReward(tier, seasonId) else SeasonPass.freeReward(tier, seasonId)
        s.addToStrSet("season_claimed", claimKey(tier, premium))
        wallet.grant(reward, col)
        return reward
    }

    fun claimAllTiers(): List<SeasonReward> {
        val out = mutableListOf<SeasonReward>()
        for (t in 1..SeasonPass.tierFor(seasonPoints)) {
            claimTier(t, false)?.let { out += it }
            claimTier(t, true)?.let { out += it }
        }
        return out
    }

    fun claimableTierCount(): Int {
        var n = 0
        for (t in 1..SeasonPass.tierFor(seasonPoints)) {
            if (canClaimTier(t, false)) n++
            if (canClaimTier(t, true)) n++
        }
        return n
    }

    fun buyTier(): Boolean {
        val points = seasonPoints
        val cost = SeasonPass.skipCostGems(points)
        if (cost <= 0 || !wallet.spendGems(cost)) return false
        s.setInt("season_points", ((SeasonPass.tierFor(points) + 1) * SeasonPass.POINTS_PER_TIER).coerceAtMost(SeasonPass.TOTAL_POINTS))
        return true
    }

    // ============================ Misiones semanales ============================

    private fun wkKey(type: WeeklyType) = "wk_${week}_${type.name}"
    private fun wkClaimKey(id: String) = "wk_${week}_claimed_$id"

    fun weekly(): List<WeeklyView> = WeeklyMissions.forWeek(week).map {
        WeeklyView(it, s.int(wkKey(it.type)), s.bool(wkClaimKey(it.id)))
    }

    fun addWeekly(type: WeeklyType, amount: Int, asMax: Boolean = false) {
        if (amount <= 0) return
        val key = wkKey(type)
        val now = s.int(key)
        s.setInt(key, if (asMax) maxOf(now, amount) else now + amount)
    }

    fun claimWeekly(id: String): WeeklyMission? {
        val item = weekly().firstOrNull { it.mission.id == id } ?: return null
        if (!item.done || item.claimed) return null
        s.setBool(wkClaimKey(id), true)
        wallet.addCoins(item.mission.coins)
        col.addTokens(com.korkoor.pardos.domain.collection.TokenRules.WEEKLY_MISSION)
        addSeasonPoints(SeasonPoints.WEEKLY_MISSION)
        return item.mission
    }

    fun weeklyBonusClaimed(): Boolean = s.bool("wk_${week}_bonus")
    fun canClaimWeeklyBonus(): Boolean = !weeklyBonusClaimed() && weekly().all { it.claimed }

    fun claimWeeklyBonus(): Boolean {
        if (!canClaimWeeklyBonus()) return false
        s.setBool("wk_${week}_bonus", true)
        col.addChests(WeeklyMissions.completionChest, 1)
        wallet.addGems(WeeklyMissions.COMPLETION_GEMS)
        return true
    }

    fun weeklyClaimableCount(): Int = weekly().count { it.done && !it.claimed } + if (canClaimWeeklyBonus()) 1 else 0

    // ============================ Misiones diarias ============================

    fun todayMissions(): List<DailyMission> {
        if (s.int("m_day", -1) != today) {
            val fresh = DailyMissionPlan.forDay(today)
            s.setInt("m_day", today)
            s.setStr("m_ids", fresh.joinToString(",") { it.id.toString() })
            MissionPool.allMissions.forEach {
                s.remove("m_${it.id}_p"); s.remove("m_${it.id}_done"); s.remove("m_${it.id}_claimed")
            }
        }
        val ids = s.str("m_ids").split(',').mapNotNull { it.toIntOrNull() }
        return ids.mapNotNull { id ->
            MissionPool.allMissions.firstOrNull { it.id == id }?.copy(
                currentProgress = s.int("m_${id}_p"), isCompleted = s.bool("m_${id}_done")
            )
        }
    }

    fun missionClaimed(id: Int): Boolean = s.bool("m_${id}_claimed")

    fun updateMission(type: MissionType, amount: Int) {
        todayMissions().filter { it.type == type && !it.isCompleted }.forEach { m ->
            val next = when (type) {
                MissionType.REACH_BLOCK -> maxOf(m.currentProgress, amount)
                MissionType.WIN_UNDER_TIME -> if (amount <= m.targetValue) m.targetValue else m.currentProgress
                else -> m.currentProgress + amount
            }
            if (next != m.currentProgress) {
                s.setInt("m_${m.id}_p", next)
                if (next >= m.targetValue) s.setBool("m_${m.id}_done", true)
            }
        }
    }

    class MissionClaim(val coins: Int, val bonusAll: Boolean, val perfectMilestoneGems: Int)

    fun claimMission(id: Int): MissionClaim? {
        val m = todayMissions().firstOrNull { it.id == id } ?: return null
        if (!m.isCompleted || missionClaimed(id)) return null
        s.setBool("m_${id}_claimed", true)
        val coins = com.korkoor.pardos.domain.rewards.CoinRewards.forMission(m.xpReward)
        wallet.addCoins(coins)
        val all = todayMissions().all { missionClaimed(it.id) }
        val bonus = onDailyMissionClaimed(all)
        return MissionClaim(coins, bonus.first, bonus.second)
    }

    private fun onDailyMissionClaimed(allClaimed: Boolean): Pair<Boolean, Int> {
        addSeasonPoints(SeasonPoints.DAILY_MISSION)
        if (allClaimed && s.int("all_missions_day", -1) != today) {
            s.setInt("all_missions_day", today)
            col.addChests(ChestType.COMMON, 1)
            col.addTokens(com.korkoor.pardos.domain.collection.TokenRules.ALL_DAILY_MISSIONS)
            wallet.addGems(Economy.DAILY_MISSIONS_BONUS_GEMS)
            addSeasonPoints(SeasonPoints.ALL_DAILY_MISSIONS)
            val last = if (s.has("perfect_day")) s.int("perfect_day") else null
            val streakNow = PerfectDays.next(last, s.int("perfect_streak"), today)
            s.setInt("perfect_day", today)
            s.setInt("perfect_streak", streakNow)
            val gems = PerfectDays.milestoneFor(streakNow)?.also { wallet.addGems(it.gems) }?.gems ?: 0
            return true to gems
        }
        return false to 0
    }

    fun perfectDays(): Int = PerfectDays.alive(if (s.has("perfect_day")) s.int("perfect_day") else null, s.int("perfect_streak"), today)
    fun perfectToday(): Boolean = s.has("perfect_day") && s.int("perfect_day") == today

    // ============================ Jugador: experiencia y nivel ============================

    val playerLevel: Int get() = s.int("player_level", 1)
    val xp: Int get() = s.int("xp")
    val xpToNext: Int get() = s.int("xp_next", 100)

    /** Suma la experiencia de una victoria y devuelve los premios de los niveles de jugador que se alcanzaron. */
    fun addXpForVictory(stars: Int): List<com.korkoor.pardos.domain.retention.LevelReward> {
        val gained = (EventCalendar.apply(15 + stars * 10, EventCalendar.xpMultiplier(today)) *
            com.korkoor.pardos.domain.collection.PerkRules.xpMultiplier(col.owned, col.foil)).toInt()
        var newXp = xp + gained
        var level = playerLevel
        var limit = xpToNext
        while (newXp >= limit) {
            newXp -= limit
            level++
            limit += 50
        }
        s.setInt("xp", newXp)
        s.setInt("player_level", level)
        s.setInt("xp_next", limit)
        return collectLevelRewards(level)
    }

    private fun collectLevelRewards(level: Int): List<com.korkoor.pardos.domain.retention.LevelReward> {
        val rewarded = s.int("rewarded_level", 1)
        if (level <= rewarded) return emptyList()
        val rewards = PlayerLevelRewards.between(rewarded, level)
        rewards.forEach {
            wallet.addCoins(it.coins)
            wallet.addGems(it.gems)
            it.chest?.let { c -> col.addChests(c, 1) }
        }
        s.setInt("rewarded_level", level)
        return rewards
    }

    // ============================ Partida terminada ============================

    private fun evKey(e: GameEvent) = "ev_${e.type.name}_${e.startDay}"

    fun eventWins(e: GameEvent): Int = s.int(evKey(e))

    fun activeSkinEvents(): List<GameEvent> = EventCalendar.seasonalOn(today).filter { EventSkins.skinFor(it.type) != null }

    private fun countEventWin(): List<TileSkin> {
        val out = mutableListOf<TileSkin>()
        activeSkinEvents().forEach { e ->
            val skin = EventSkins.skinFor(e.type) ?: return@forEach
            val wins = eventWins(e) + 1
            s.setInt(evKey(e), wins)
            if (wins >= EventSkins.WINS_REQUIRED && skin.id !in wallet.ownedSkins) {
                wallet.grantSkin(skin)
                markReveal(skin)
                out += skin
            }
        }
        return out
    }

    fun buyEventSkin(e: GameEvent): Boolean {
        val skin = EventSkins.skinFor(e.type) ?: return false
        if (skin.id in wallet.ownedSkins) return false
        if (!wallet.spendGems(EventSkins.GEM_PRICE)) return false
        wallet.grantSkin(skin)
        return true
    }

    fun playerStats(): PlayerStats = PlayerStats(
        nightWins = s.int("night_wins"), dawnWins = s.int("dawn_wins"), bestStreak = bestStreak, bestTile = s.int("best_tile"),
        albumComplete = CollectibleCatalog.isAlbumComplete(col.owned), daysPlayed = s.int("days_played"),
        totalStars = s.int("total_stars"), totalWins = s.int("total_wins")
    )

    fun checkHiddenSkins(): List<TileSkin> {
        val fresh = HiddenSkins.newlyUnlocked(playerStats(), wallet.ownedSkins)
        fresh.forEach { wallet.grantSkin(it); markReveal(it) }
        return fresh
    }

    private fun markReveal(skin: TileSkin) { s.addToStrSet("reveal_pending", skin.id) }

    fun takePendingReveals(): List<TileSkin> {
        val ids = s.strSet("reveal_pending")
        if (ids.isEmpty()) return emptyList()
        s.remove("reveal_pending")
        return ids.map { TileSkin.fromId(it) }
    }

    /** Al terminar cualquier partida. Devuelve los extras ganados para enseñarlos. */
    fun onGameFinished(won: Boolean, dailyChallenge: Boolean): GameBonus {
        addWeekly(WeeklyType.PLAY_GAMES, 1)
        val capDay = s.int("game_pts_day", -1)
        var used = if (capDay == today) s.int("game_pts") else 0
        val wanted = SeasonPoints.GAME_PLAYED + if (won) SeasonPoints.GAME_WON else 0
        val earned = wanted.coerceAtMost((SeasonPoints.GAME_DAILY_CAP - used).coerceAtLeast(0))
        used += earned
        s.setInt("game_pts_day", today)
        s.setInt("game_pts", used)
        addSeasonPoints(earned)

        var firstWin = 0
        var piggyGems = 0
        val newSkins = mutableListOf<TileSkin>()
        if (won) {
            addWeekly(WeeklyType.WIN_LEVELS, 1)
            s.addInt("total_wins", 1)
            when (DayPart.of(clock.minute)) {
                DayPart.NIGHT -> s.addInt("night_wins", 1)
                DayPart.DAWN -> s.addInt("dawn_wins", 1)
                DayPart.OTHER -> Unit
            }
            newSkins += countEventWin()
            if (s.int("first_win_day", -1) != today) {
                s.setInt("first_win_day", today)
                firstWin = Economy.FIRST_WIN_BONUS_COINS
                wallet.addCoins(firstWin)
            }
            val gain = PiggyBank.gemsForWin(dailyChallenge)
            val before = s.int("piggy")
            val after = PiggyBank.deposit(before, gain)
            s.setInt("piggy", after)
            piggyGems = after - before
        }
        newSkins += checkHiddenSkins()
        return GameBonus(firstWin, piggyGems, earned, newSkins.distinct())
    }

    fun onDailyChallengeCompleted() {
        addWeekly(WeeklyType.DAILY_CHALLENGES, 1)
        if (s.int("daily_ch_day", -1) != today) {
            s.setInt("daily_ch_day", today)
            addSeasonPoints(SeasonPoints.DAILY_CHALLENGE)
        }
    }

    fun onStars(stars: Int) {
        addWeekly(WeeklyType.EARN_STARS, stars)
        if (stars > 0) s.addInt("total_stars", stars)
    }

    fun onTileReached(value: Int) {
        addWeekly(WeeklyType.REACH_TILE, value, asMax = true)
        if (value > s.int("best_tile")) s.setInt("best_tile", value)
    }

    fun onChestOpened() { addWeekly(WeeklyType.OPEN_CHESTS, 1) }

    val piggy: Int get() = s.int("piggy")
    fun canBreakPiggy(): Boolean = PiggyBank.canBreak(piggy)
    fun dailyChallengeDoneToday(): Boolean = s.int("daily_ch_day", -1) == today

    // ============================ Liga semanal ============================

    val league: League get() = League.fromId(s.str("league"))
    fun leagueWeekStars(): Int = s.int(wkKey(WeeklyType.EARN_STARS))
    fun pendingLeagueResult(): LeagueResult? = LeagueResult.decode(s.str("league_pending").ifEmpty { null })

    fun rolloverLeagueIfNeeded() {
        val current = week
        val last = s.int("league_week", -1)
        if (last < 0) { s.setInt("league_week", current); return }
        if (last >= current) return
        pendingLeagueResult()?.let { grantLeagueResult(it) }
        val stars = s.int("wk_${last}_${WeeklyType.EARN_STARS.name}")
        val base = Leagues.evaluate(league, stars)
        val finalLeague = Leagues.afterMissedWeeks(base.to, current - last - 1)
        val outcome = when {
            base.outcome == LeagueOutcome.TOP_HELD && finalLeague == base.to -> LeagueOutcome.TOP_HELD
            finalLeague.ordinal > base.from.ordinal -> LeagueOutcome.PROMOTED
            finalLeague.ordinal < base.from.ordinal -> LeagueOutcome.DEMOTED
            else -> LeagueOutcome.STAYED
        }
        val result = base.copy(to = finalLeague, outcome = outcome)
        val worthShowing = result.hasReward || outcome != LeagueOutcome.STAYED
        s.setStr("league", finalLeague.name)
        s.setInt("league_week", current)
        if (worthShowing) s.setStr("league_pending", result.encode()) else s.remove("league_pending")
    }

    private fun grantLeagueResult(r: LeagueResult) {
        wallet.addCoins(r.coins)
        wallet.addGems(r.gems)
        r.chest?.let { col.addChests(it, 1) }
    }

    fun claimLeagueResult(): LeagueResult? {
        val r = pendingLeagueResult() ?: return null
        grantLeagueResult(r)
        s.remove("league_pending")
        return r
    }

    // ============================ Siguiente meta e insignias ============================

    fun nextGoal(): NextGoal? {
        val lg = league
        val stars = leagueWeekStars()
        val wk = weekly().filter { !it.claimed && !it.done }.maxByOrNull { it.progress.toFloat() / it.mission.target }
        val points = seasonPoints
        val tier = SeasonPass.tierFor(points)
        return NextGoals.pick(
            GoalInput(
                chestRemainingMs = freeChestRemainingMs(),
                leagueNextName = lg.next()?.displayName,
                leagueStarsToPromote = Leagues.starsToPromote(lg, stars),
                leaguePromoteTarget = lg.promoteStars,
                weeklyTitle = wk?.mission?.title,
                weeklyProgress = wk?.progress ?: 0,
                weeklyTarget = wk?.mission?.target ?: 1,
                seasonNextTier = if (tier >= SeasonPass.TIERS) 0 else tier + 1,
                seasonPointsLeft = if (tier >= SeasonPass.TIERS) 0 else SeasonPass.POINTS_PER_TIER - SeasonPass.pointsInTier(points),
                seasonPointsPerTier = SeasonPass.POINTS_PER_TIER,
                dailyChallengeDone = dailyChallengeDoneToday()
            )
        )
    }

    fun pendingBadges(): Int {
        var n = 0
        if (freeChestReady()) n++
        if (wheelAllowance().freeLeft > 0) n++
        if (claimableTierCount() > 0) n++
        if (weeklyClaimableCount() > 0) n++
        if (pendingLeagueResult() != null) n++
        return n
    }
}
