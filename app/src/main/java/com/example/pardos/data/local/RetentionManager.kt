package com.korkoor.pardos.data.local

import android.content.Context
import android.content.SharedPreferences
import com.korkoor.pardos.domain.collection.ChestType
import com.korkoor.pardos.domain.economy.Economy
import com.korkoor.pardos.domain.retention.Comeback
import com.korkoor.pardos.domain.retention.ComebackGift
import com.korkoor.pardos.domain.retention.DailyWheel
import com.korkoor.pardos.domain.retention.FreeChest
import com.korkoor.pardos.domain.retention.League
import com.korkoor.pardos.domain.retention.LeagueOutcome
import com.korkoor.pardos.domain.retention.LeagueResult
import com.korkoor.pardos.domain.retention.Leagues
import com.korkoor.pardos.domain.collection.CollectibleCatalog
import com.korkoor.pardos.domain.events.EventCalendar
import com.korkoor.pardos.domain.events.GameEvent
import com.korkoor.pardos.domain.retention.GoalInput
import com.korkoor.pardos.domain.retention.LevelReward
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
import com.korkoor.pardos.domain.shop.DayPart
import com.korkoor.pardos.domain.shop.EventSkins
import com.korkoor.pardos.domain.shop.HiddenSkins
import com.korkoor.pardos.domain.shop.PlayerStats
import com.korkoor.pardos.domain.shop.TileSkin
import com.korkoor.pardos.domain.social.WeekCalendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

/** Un progreso semanal listo para pintar. */
data class WeeklyProgress(val mission: WeeklyMission, val progress: Int, val claimed: Boolean) {
    val done: Boolean get() = progress >= mission.target
}

/** Resultado de una partida terminada, para mostrar los extras ganados. */
data class GameBonus(
    val firstWinCoins: Int = 0, val piggyGems: Int = 0, val seasonPoints: Int = 0,
    /** Skins que se desbloquearon con esta partida (de evento o secretas). */
    val newSkins: List<TileSkin> = emptyList()
)

/**
 * Todo lo que hace volver al jugador: cofre gratis, ruleta, pase de temporada, misiones semanales,
 * hucha, premios por nivel y regalo de regreso. Los números viven en `Economy` y `domain/retention`.
 * Los StateFlow son compartidos para que cualquier pantalla vea el mismo valor al instante.
 */
class RetentionManager(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = appContext.getSharedPreferences("pardos_retention", Context.MODE_PRIVATE)
    private val economy = EconomyManager(appContext)
    private val collection = CollectionManager(appContext)

    init {
        if (!loaded) {
            _freeChestLast.value = prefs.getLong(K_CHEST_LAST, 0L)
            _piggy.value = prefs.getInt(K_PIGGY, 0)
            loaded = true
        }
        rolloverSeasonIfNeeded()
        rolloverLeagueIfNeeded()
    }

    /** Sube cada vez que algo cambia; las pantallas lo observan para volver a leer. */
    val tick: StateFlow<Int> = _tick.asStateFlow()
    val seasonPoints: StateFlow<Int> = _seasonPoints.asStateFlow()
    val seasonPremium: StateFlow<Boolean> = _seasonPremium.asStateFlow()
    val seasonClaimed: StateFlow<Set<String>> = _seasonClaimed.asStateFlow()
    val piggy: StateFlow<Int> = _piggy.asStateFlow()
    val freeChestLast: StateFlow<Long> = _freeChestLast.asStateFlow()

    private fun bump() { _tick.value = _tick.value + 1 }

    private fun today() = LocalDay.today()
    private fun week() = WeekCalendar.weekId(today())

    // ============================ Entrega de premios ============================

    fun grant(r: SeasonReward) {
        economy.addCoins(r.coins)
        economy.addGems(r.gems)
        r.chest?.let { collection.addChests(it, 1) }
        economy.addStreakFreezes(r.freezes.coerceAtMost((Economy.MAX_STREAK_FREEZES - economy.streakFreezes.value).coerceAtLeast(0)))
        economy.addUndos(r.undos)
        r.skin?.let { economy.grantSkin(it) }
        if (r.avatar != 0) economy.grantAvatar(r.avatar)
        if (r.banner != 0) economy.grantBanner(r.banner)
        r.fx?.let { economy.grantFx(it) }
        if (r.tokens > 0) collection.addTokens(r.tokens)
    }

    // ============================ Al entrar / regresar ============================

    data class CheckIn(val comeback: ComebackGift?, val daysAway: Int)

    /** Se llama al abrir el menú. Una vez al día suma el login, cuenta la semana y entrega el regalo de regreso. */
    fun checkIn(): CheckIn {
        val today = today()
        val last = prefs.getInt(K_LAST_OPEN, -1)
        if (last == today) return CheckIn(null, 0)
        val away = if (last < 0) 0 else today - last
        prefs.edit().putInt(K_LAST_OPEN, today).apply()

        addSeasonPoints(SeasonPoints.DAILY_LOGIN)
        collection.onDailyCheckIn(today)
        addWeekly(WeeklyType.LOGIN_DAYS, 1)
        prefs.edit().putInt(K_DAYS_PLAYED, prefs.getInt(K_DAYS_PLAYED, 0) + 1).apply()

        val gift = Comeback.giftFor(away)
        if (gift != null) {
            economy.addCoins(gift.coins)
            collection.addChests(gift.chest, 1)
            economy.addStreakFreezes(gift.freezes.coerceAtMost((Economy.MAX_STREAK_FREEZES - economy.streakFreezes.value).coerceAtLeast(0)))
        }
        bump()
        return CheckIn(gift, away)
    }

    // ============================ Cofre gratis ============================

    fun freeChestRemainingMs(now: Long = System.currentTimeMillis()): Long =
        FreeChest.remainingMs(_freeChestLast.value, now)

    fun isFreeChestReady(now: Long = System.currentTimeMillis()): Boolean = freeChestRemainingMs(now) == 0L

    /** Reclama el cofre gratis (si ya toca) y arranca la siguiente espera. */
    fun claimFreeChest(now: Long = System.currentTimeMillis()): ChestType? {
        if (!isFreeChestReady(now)) return null
        val count = prefs.getInt(K_CHEST_COUNT, 0)
        val type = FreeChest.typeFor(count)
        collection.addChests(type, 1)
        com.korkoor.pardos.audio.GameAudio.play(com.korkoor.pardos.audio.Sfx.GIFT)
        _freeChestLast.value = now
        prefs.edit().putLong(K_CHEST_LAST, now).putInt(K_CHEST_COUNT, count + 1).apply()
        addSeasonPoints(SeasonPoints.FREE_CHEST)
        bump()
        return type
    }

    /** Tipo del próximo cofre gratis (cada quinto es raro). */
    fun nextFreeChestType(): ChestType = FreeChest.typeFor(prefs.getInt(K_CHEST_COUNT, 0))

    fun adSkipsLeftToday(): Int {
        val used = if (prefs.getInt(K_CHEST_SKIP_DAY, -1) == today()) prefs.getInt(K_CHEST_SKIPS, 0) else 0
        return (Economy.FREE_CHEST_AD_SKIPS_PER_DAY - used).coerceAtLeast(0)
    }

    /** Quita la espera tras ver un anuncio (hay un tope diario). */
    fun skipFreeChestWithAd(): Boolean {
        if (adSkipsLeftToday() <= 0) return false
        val used = Economy.FREE_CHEST_AD_SKIPS_PER_DAY - adSkipsLeftToday() + 1
        prefs.edit().putInt(K_CHEST_SKIP_DAY, today()).putInt(K_CHEST_SKIPS, used).putLong(K_CHEST_LAST, 0L).apply()
        _freeChestLast.value = 0L
        bump()
        return true
    }

    /** Quita la espera pagando gemas. */
    fun skipFreeChestWithGems(now: Long = System.currentTimeMillis()): Boolean {
        val cost = FreeChest.skipCostGems(freeChestRemainingMs(now))
        if (cost <= 0 || !economy.spendGems(cost)) return false
        prefs.edit().putLong(K_CHEST_LAST, 0L).apply()
        _freeChestLast.value = 0L
        bump()
        return true
    }

    // ============================ Ruleta ============================

    private fun wheelUsed(key: String): Int = if (prefs.getInt(K_WHEEL_DAY, -1) == today()) prefs.getInt(key, 0) else 0

    fun wheelAllowance(): DailyWheel.Allowance = DailyWheel.allowance(wheelUsed(K_WHEEL_FREE), wheelUsed(K_WHEEL_AD))

    /** Decide el premio del giro y lo marca como usado. Devuelve el índice de casilla o null si no quedan giros. */
    fun spinWheel(viaAd: Boolean, random: Random = Random.Default): Int? {
        val a = wheelAllowance()
        if (viaAd) { if (a.adLeft <= 0) return null } else if (a.freeLeft <= 0) return null
        val today = today()
        val free = wheelUsed(K_WHEEL_FREE) + if (viaAd) 0 else 1
        val ad = wheelUsed(K_WHEEL_AD) + if (viaAd) 1 else 0
        prefs.edit().putInt(K_WHEEL_DAY, today).putInt(K_WHEEL_FREE, free).putInt(K_WHEEL_AD, ad).apply()
        bump()
        return DailyWheel.pick(random)
    }

    /** Entrega el premio de una casilla (se llama cuando termina la animación). */
    fun applyWheelPrize(slice: WheelSlice) {
        when (slice.kind) {
            WheelKind.COINS -> economy.addCoins(slice.amount)
            WheelKind.GEMS -> economy.addGems(slice.amount)
            WheelKind.CHEST -> collection.addChests(slice.chest ?: ChestType.COMMON, slice.amount)
            WheelKind.SEASON_POINTS -> addSeasonPoints(slice.amount)
        }
        addSeasonPoints(SeasonPoints.WHEEL_SPIN)
        bump()
    }

    // ============================ Pase de temporada ============================

    val seasonId: Int get() = SeasonCalendar.seasonId(today())

    private fun rolloverSeasonIfNeeded() {
        val current = seasonId
        val stored = prefs.getInt(K_SEASON_ID, -1)
        if (stored != current) {
            prefs.edit()
                .putInt(K_SEASON_ID, current).putInt(K_SEASON_POINTS, 0).putBoolean(K_SEASON_PREMIUM, false)
                .putStringSet(K_SEASON_CLAIMED, emptySet()).apply()
            _seasonPoints.value = 0; _seasonPremium.value = false; _seasonClaimed.value = emptySet()
        } else if (!seasonLoaded) {
            _seasonPoints.value = prefs.getInt(K_SEASON_POINTS, 0)
            _seasonPremium.value = prefs.getBoolean(K_SEASON_PREMIUM, false)
            _seasonClaimed.value = prefs.getStringSet(K_SEASON_CLAIMED, emptySet())?.toSet() ?: emptySet()
        }
        seasonLoaded = true
        // Si la app lleva abierta un cambio de mes, el estado en memoria ya se reinició arriba
    }

    fun addSeasonPoints(points: Int) {
        if (points <= 0) return
        rolloverSeasonIfNeeded()
        val total = (_seasonPoints.value + points).coerceAtMost(SeasonPass.TOTAL_POINTS)
        _seasonPoints.value = total
        prefs.edit().putInt(K_SEASON_POINTS, total).apply()
        bump()
    }

    /** Compra real del pase: abre la vía premium de ESTA temporada. */
    fun unlockPremium() {
        rolloverSeasonIfNeeded()
        _seasonPremium.value = true
        prefs.edit().putBoolean(K_SEASON_PREMIUM, true).apply()
        bump()
    }

    private fun claimKey(tier: Int, premium: Boolean) = (if (premium) "p" else "f") + tier

    fun isTierClaimed(tier: Int, premium: Boolean): Boolean = claimKey(tier, premium) in _seasonClaimed.value

    fun canClaimTier(tier: Int, premium: Boolean): Boolean =
        tier in 1..SeasonPass.TIERS && tier <= SeasonPass.tierFor(_seasonPoints.value) &&
            (!premium || _seasonPremium.value) && !isTierClaimed(tier, premium)

    fun claimTier(tier: Int, premium: Boolean): SeasonReward? {
        if (!canClaimTier(tier, premium)) return null
        val reward = if (premium) SeasonPass.premiumReward(tier, seasonId) else SeasonPass.freeReward(tier, seasonId)
        val updated = _seasonClaimed.value + claimKey(tier, premium)
        _seasonClaimed.value = updated
        prefs.edit().putStringSet(K_SEASON_CLAIMED, updated).apply()
        grant(reward)
        // lo especial (cofre, skin, avatar, banner, efecto) suena más grande que una moneda más
        val special = reward.chest != null || reward.skin != null || reward.avatar != 0 || reward.banner != 0 || reward.fx != null
        com.korkoor.pardos.audio.GameAudio.play(if (special) com.korkoor.pardos.audio.Sfx.REWARD_BIG else com.korkoor.pardos.audio.Sfx.SEASON_TIER)
        bump()
        return reward
    }

    /** Reclama todo lo disponible de una vez. Devuelve los premios entregados. */
    fun claimAllTiers(): List<SeasonReward> {
        val out = mutableListOf<SeasonReward>()
        for (t in 1..SeasonPass.tierFor(_seasonPoints.value)) {
            claimTier(t, false)?.let { out += it }
            claimTier(t, true)?.let { out += it }
        }
        return out
    }

    fun claimableTierCount(): Int {
        var n = 0
        for (t in 1..SeasonPass.tierFor(_seasonPoints.value)) {
            if (canClaimTier(t, false)) n++
            if (canClaimTier(t, true)) n++
        }
        return n
    }

    /** Gasta gemas para subir un nivel del pase. */
    fun buyTier(): Boolean {
        val points = _seasonPoints.value
        val cost = SeasonPass.skipCostGems(points)
        if (cost <= 0 || !economy.spendGems(cost)) return false
        val next = ((SeasonPass.tierFor(points) + 1) * SeasonPass.POINTS_PER_TIER).coerceAtMost(SeasonPass.TOTAL_POINTS)
        _seasonPoints.value = next
        prefs.edit().putInt(K_SEASON_POINTS, next).apply()
        bump()
        return true
    }

    // ============================ Misiones semanales ============================

    private fun wkKey(type: WeeklyType) = "wk_${week()}_${type.name}"
    private fun wkClaimKey(id: String) = "wk_${week()}_claimed_$id"

    fun weekly(): List<WeeklyProgress> = WeeklyMissions.forWeek(week()).map {
        WeeklyProgress(it, prefs.getInt(wkKey(it.type), 0), prefs.getBoolean(wkClaimKey(it.id), false))
    }

    private fun addWeekly(type: WeeklyType, amount: Int, asMax: Boolean = false) {
        if (amount <= 0) return
        val key = wkKey(type)
        val now = prefs.getInt(key, 0)
        val next = if (asMax) maxOf(now, amount) else now + amount
        if (next != now) { prefs.edit().putInt(key, next).apply(); bump() }
    }

    fun claimWeekly(id: String): WeeklyMission? {
        val item = weekly().firstOrNull { it.mission.id == id } ?: return null
        if (!item.done || item.claimed) return null
        prefs.edit().putBoolean(wkClaimKey(id), true).apply()
        economy.addCoins(item.mission.coins)
        collection.addTokens(com.korkoor.pardos.domain.collection.TokenRules.WEEKLY_MISSION)
        addSeasonPoints(SeasonPoints.WEEKLY_MISSION)
        com.korkoor.pardos.audio.GameAudio.play(com.korkoor.pardos.audio.Sfx.MISSION_DONE)
        bump()
        return item.mission
    }

    fun isWeeklyBonusClaimed(): Boolean = prefs.getBoolean("wk_${week()}_bonus", false)

    fun canClaimWeeklyBonus(): Boolean = !isWeeklyBonusClaimed() && weekly().all { it.claimed }

    fun claimWeeklyBonus(): Boolean {
        if (!canClaimWeeklyBonus()) return false
        prefs.edit().putBoolean("wk_${week()}_bonus", true).apply()
        collection.addChests(WeeklyMissions.completionChest, 1)
        economy.addGems(WeeklyMissions.COMPLETION_GEMS)
        com.korkoor.pardos.audio.GameAudio.play(com.korkoor.pardos.audio.Sfx.REWARD_BIG)
        bump()
        return true
    }

    fun weeklyClaimableCount(): Int = weekly().count { it.done && !it.claimed } + if (canClaimWeeklyBonus()) 1 else 0

    // ---- Eventos de juego que alimentan semanales y pase ----

    /** Al terminar cualquier partida. Devuelve los extras ganados para enseñarlos. */
    fun onGameFinished(won: Boolean, dailyChallenge: Boolean): GameBonus {
        addWeekly(WeeklyType.PLAY_GAMES, 1)

        // Puntos de pase por jugar/ganar, con tope diario
        val capDay = prefs.getInt(K_GAME_PTS_DAY, -1)
        var usedToday = if (capDay == today()) prefs.getInt(K_GAME_PTS, 0) else 0
        val wanted = SeasonPoints.GAME_PLAYED + if (won) SeasonPoints.GAME_WON else 0
        val earned = wanted.coerceAtMost((SeasonPoints.GAME_DAILY_CAP - usedToday).coerceAtLeast(0))
        usedToday += earned
        prefs.edit().putInt(K_GAME_PTS_DAY, today()).putInt(K_GAME_PTS, usedToday).apply()
        addSeasonPoints(earned)

        var firstWin = 0
        var piggyGems = 0
        val newSkins = mutableListOf<TileSkin>()
        if (won) {
            addWeekly(WeeklyType.WIN_LEVELS, 1)
            prefs.edit().putInt(K_TOTAL_WINS, prefs.getInt(K_TOTAL_WINS, 0) + 1).apply()
            when (DayPart.of(minuteOfDay())) {
                DayPart.NIGHT -> prefs.edit().putInt(K_NIGHT_WINS, prefs.getInt(K_NIGHT_WINS, 0) + 1).apply()
                DayPart.DAWN -> prefs.edit().putInt(K_DAWN_WINS, prefs.getInt(K_DAWN_WINS, 0) + 1).apply()
                DayPart.OTHER -> Unit
            }
            newSkins += countEventWin()
            if (prefs.getInt(K_FIRST_WIN_DAY, -1) != today()) {
                prefs.edit().putInt(K_FIRST_WIN_DAY, today()).apply()
                firstWin = Economy.FIRST_WIN_BONUS_COINS
                economy.addCoins(firstWin)
            }
            piggyGems = PiggyBank.gemsForWin(dailyChallenge)
            val before = _piggy.value
            _piggy.value = PiggyBank.deposit(before, piggyGems)
            piggyGems = _piggy.value - before
            prefs.edit().putInt(K_PIGGY, _piggy.value).apply()
        }
        newSkins += checkHiddenSkins()
        bump()
        return GameBonus(firstWin, piggyGems, earned, newSkins.distinct())
    }

    fun onDailyChallengeCompleted() {
        addWeekly(WeeklyType.DAILY_CHALLENGES, 1)
        // Solo una vez por día suma puntos
        if (prefs.getInt(K_DAILY_CH_DAY, -1) != today()) {
            prefs.edit().putInt(K_DAILY_CH_DAY, today()).apply()
            addSeasonPoints(SeasonPoints.DAILY_CHALLENGE)
        }
    }

    fun onMerges(pairs: Int) = addWeekly(WeeklyType.MERGE_PAIRS, pairs)
    fun onStars(stars: Int) {
        addWeekly(WeeklyType.EARN_STARS, stars)
        if (stars > 0) prefs.edit().putInt(K_TOTAL_STARS, prefs.getInt(K_TOTAL_STARS, 0) + stars).apply()
    }
    fun onTileReached(value: Int) {
        addWeekly(WeeklyType.REACH_TILE, value, asMax = true)
        if (value > prefs.getInt(K_BEST_TILE, 0)) prefs.edit().putInt(K_BEST_TILE, value).apply()
    }
    fun onChestOpened() = addWeekly(WeeklyType.OPEN_CHESTS, 1)

    /** Se llama al cobrar una misión diaria. [allClaimed] = ya cobró las tres. Devuelve true si hubo bonus del día. */
    fun onDailyMissionClaimed(allClaimed: Boolean): Boolean {
        com.korkoor.pardos.audio.GameAudio.play(com.korkoor.pardos.audio.Sfx.MISSION_DONE)
        addSeasonPoints(SeasonPoints.DAILY_MISSION)
        if (allClaimed && prefs.getInt(K_ALL_MISSIONS_DAY, -1) != today()) {
            prefs.edit().putInt(K_ALL_MISSIONS_DAY, today()).apply()
            collection.addChests(ChestType.COMMON, 1)
            com.korkoor.pardos.audio.GameAudio.playLater(com.korkoor.pardos.audio.Sfx.REWARD_BIG, 350)
            collection.addTokens(com.korkoor.pardos.domain.collection.TokenRules.ALL_DAILY_MISSIONS)
            economy.addGems(Economy.DAILY_MISSIONS_BONUS_GEMS)
            addSeasonPoints(SeasonPoints.ALL_DAILY_MISSIONS)
            // Racha de días perfectos: encadenar días con las tres misiones cobradas da premios por hitos
            val last = prefs.getInt(K_PERFECT_DAY, Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE }
            val streak = com.korkoor.pardos.domain.model.PerfectDays.next(last, prefs.getInt(K_PERFECT_STREAK, 0), today())
            prefs.edit().putInt(K_PERFECT_DAY, today()).putInt(K_PERFECT_STREAK, streak).apply()
            lastPerfectMilestone = com.korkoor.pardos.domain.model.PerfectDays.milestoneFor(streak)?.also { economy.addGems(it.gems) }
            return true
        }
        return false
    }

    /** Días perfectos seguidos (0 si la racha ya se rompió). */
    fun perfectDays(): Int = com.korkoor.pardos.domain.model.PerfectDays.alive(
        prefs.getInt(K_PERFECT_DAY, Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE }, prefs.getInt(K_PERFECT_STREAK, 0), today()
    )

    /** ¿Hoy ya se cobraron las tres misiones? */
    fun perfectToday(): Boolean = prefs.getInt(K_PERFECT_DAY, Int.MIN_VALUE) == today()

    /** Premio de hito que cayó en el último cobro (la UI lo lee una vez para felicitar). */
    @Volatile var lastPerfectMilestone: com.korkoor.pardos.domain.model.PerfectDays.Milestone? = null

    // ============================ Liga semanal ============================

    val league: League get() = League.fromId(prefs.getString(K_LEAGUE, null))

    /** Estrellas ganadas esta semana (las mismas que cuentan para la misión semanal de estrellas). */
    fun leagueWeekStars(): Int = prefs.getInt(wkKey(WeeklyType.EARN_STARS), 0)

    /** Resultado de la semana pasada, a la espera de que el jugador lo reclame. */
    fun pendingLeagueResult(): LeagueResult? = LeagueResult.decode(prefs.getString(K_LEAGUE_PENDING, null))

    /** Cierra la semana anterior si ya pasó: sube/baja de liga y deja el resultado pendiente de reclamar. */
    private fun rolloverLeagueIfNeeded() {
        val current = week()
        val last = prefs.getInt(K_LEAGUE_WEEK, -1)
        if (last < 0) { prefs.edit().putInt(K_LEAGUE_WEEK, current).apply(); return }
        if (last >= current) return

        // Un resultado anterior sin reclamar se entrega ya para no perderlo
        pendingLeagueResult()?.let { grantLeagueResult(it) }

        val stars = prefs.getInt("wk_${last}_${WeeklyType.EARN_STARS.name}", 0)
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
        prefs.edit()
            .putString(K_LEAGUE, finalLeague.name)
            .putInt(K_LEAGUE_WEEK, current)
            .putString(K_LEAGUE_PENDING, if (worthShowing) result.encode() else null)
            .apply()
        bump()
    }

    private fun grantLeagueResult(r: LeagueResult) {
        economy.addCoins(r.coins)
        economy.addGems(r.gems)
        r.chest?.let { collection.addChests(it, 1) }
    }

    /** Entrega el premio de la semana pasada. Devuelve el resultado o null si no había nada pendiente. */
    fun claimLeagueResult(): LeagueResult? {
        val r = pendingLeagueResult() ?: return null
        grantLeagueResult(r)
        prefs.edit().remove(K_LEAGUE_PENDING).apply()
        bump()
        return r
    }

    // ============================ Hucha ============================

    fun canBreakPiggy(): Boolean = PiggyBank.canBreak(_piggy.value)

    /** Rompe la hucha tras la compra real: entrega las gemas guardadas y vuelve a empezar. */
    fun breakPiggy(): Int {
        val gems = _piggy.value
        if (gems <= 0) return 0
        economy.addGems(gems)
        _piggy.value = 0
        prefs.edit().putInt(K_PIGGY, 0).apply()
        com.korkoor.pardos.audio.GameAudio.play(com.korkoor.pardos.audio.Sfx.PIGGY)
        bump()
        return gems
    }

    // ============================ Nivel de jugador ============================

    /** Entrega los premios de los niveles de jugador alcanzados desde la última vez. */
    fun collectLevelRewards(playerLevel: Int): List<LevelReward> {
        val rewarded = prefs.getInt(K_REWARDED_LEVEL, 1)
        if (playerLevel <= rewarded) return emptyList()
        val rewards = PlayerLevelRewards.between(rewarded, playerLevel)
        rewards.forEach {
            economy.addCoins(it.coins)
            economy.addGems(it.gems)
            it.chest?.let { c -> collection.addChests(c, 1) }
        }
        prefs.edit().putInt(K_REWARDED_LEVEL, playerLevel).apply()
        bump()
        return rewards
    }

    // ============================ Skins de fiesta y skins secretas ============================

    private fun minuteOfDay(): Int {
        val c = java.util.Calendar.getInstance()
        return c.get(java.util.Calendar.HOUR_OF_DAY) * 60 + c.get(java.util.Calendar.MINUTE)
    }

    private fun evKey(e: GameEvent) = "ev_${e.type.name}_${e.startDay}"

    /** Victorias de esta edición del evento (cada año cuenta aparte). */
    fun eventWins(e: GameEvent): Int = prefs.getInt(evKey(e), 0)

    /** Fiestas activas hoy que traen skin (normalmente 0 o 1). */
    fun activeSkinEvents(): List<GameEvent> =
        EventCalendar.seasonalOn(today()).filter { EventSkins.skinFor(it.type) != null }

    /** Suma una victoria a cada fiesta activa y entrega la skin al llegar a la meta. Devuelve las skins nuevas. */
    private fun countEventWin(): List<TileSkin> {
        val out = mutableListOf<TileSkin>()
        activeSkinEvents().forEach { e ->
            val skin = EventSkins.skinFor(e.type) ?: return@forEach
            val wins = eventWins(e) + 1
            prefs.edit().putInt(evKey(e), wins).apply()
            if (wins >= EventSkins.WINS_REQUIRED && skin.id !in economy.ownedSkins.value) {
                economy.grantSkin(skin)
                markReveal(skin)
                out += skin
            }
        }
        return out
    }

    /** Compra la skin de la fiesta activa con gemas. */
    fun buyEventSkin(e: GameEvent): Boolean {
        val skin = EventSkins.skinFor(e.type) ?: return false
        if (skin.id in economy.ownedSkins.value) return false
        if (!economy.spendGems(EventSkins.GEM_PRICE)) return false
        economy.grantSkin(skin)
        bump()
        return true
    }

    /** Cuenta las piezas de campaña ya conseguidas (para jugadores que ya llevaban partidas antes de los contadores). */
    private fun legacyCampaignStats(): Pair<Int, Int> {
        val store = appContext.getSharedPreferences("pardos_storage", Context.MODE_PRIVATE)
        var stars = 0
        var wins = 0
        store.all.forEach { (k, v) ->
            if (k.startsWith("stars_level_") && v is Int && v > 0) { stars += v; wins += 1 }
        }
        return stars to wins
    }

    /** Todo lo que cuentan las skins secretas. Toma el mayor entre el contador y lo que ya estaba guardado. */
    fun playerStats(): PlayerStats {
        val (legacyStars, legacyWins) = legacyCampaignStats()
        val profile = appContext.getSharedPreferences("pardos_profile", Context.MODE_PRIVATE)
        return PlayerStats(
            nightWins = prefs.getInt(K_NIGHT_WINS, 0),
            dawnWins = prefs.getInt(K_DAWN_WINS, 0),
            bestStreak = profile.getInt("best_streak", 0),
            bestTile = prefs.getInt(K_BEST_TILE, 0),
            albumComplete = CollectibleCatalog.isAlbumComplete(collection.owned.value),
            daysPlayed = prefs.getInt(K_DAYS_PLAYED, 0),
            totalStars = maxOf(prefs.getInt(K_TOTAL_STARS, 0), legacyStars),
            totalWins = maxOf(prefs.getInt(K_TOTAL_WINS, 0), legacyWins)
        )
    }

    /** Entrega las skins secretas que ya se merecen y las deja pendientes de mostrar. Devuelve las nuevas. */
    fun checkHiddenSkins(): List<TileSkin> {
        val fresh = HiddenSkins.newlyUnlocked(playerStats(), economy.ownedSkins.value)
        fresh.forEach { economy.grantSkin(it); markReveal(it) }
        if (fresh.isNotEmpty()) bump()
        return fresh
    }

    private fun markReveal(skin: TileSkin) {
        val pending = (prefs.getStringSet(K_REVEAL, emptySet()) ?: emptySet()) + skin.id
        prefs.edit().putStringSet(K_REVEAL, pending).apply()
    }

    /** Skins desbloqueadas que aún no se celebraron con un diálogo. Al leerlas quedan marcadas como vistas. */
    fun takePendingReveals(): List<TileSkin> {
        val ids = prefs.getStringSet(K_REVEAL, emptySet()) ?: emptySet()
        if (ids.isEmpty()) return emptyList()
        prefs.edit().remove(K_REVEAL).apply()
        return ids.map { TileSkin.fromId(it) }
    }

    // ============================ Siguiente meta ============================

    fun dailyChallengeDoneToday(): Boolean = prefs.getInt(K_DAILY_CH_DAY, -1) == today()

    /** La meta más cercana para enseñar al terminar una partida (ver `NextGoals`). */
    fun nextGoal(now: Long = System.currentTimeMillis()): NextGoal? {
        val lg = league
        val stars = leagueWeekStars()
        val weekly = weekly().filter { !it.claimed && !it.done }.maxByOrNull { it.progress.toFloat() / it.mission.target }
        val points = _seasonPoints.value
        val tier = SeasonPass.tierFor(points)
        return NextGoals.pick(
            GoalInput(
                chestRemainingMs = freeChestRemainingMs(now),
                leagueNextName = lg.next()?.displayName,
                leagueStarsToPromote = Leagues.starsToPromote(lg, stars),
                leaguePromoteTarget = lg.promoteStars,
                weeklyTitle = weekly?.mission?.title,
                weeklyProgress = weekly?.progress ?: 0,
                weeklyTarget = weekly?.mission?.target ?: 1,
                seasonNextTier = if (tier >= SeasonPass.TIERS) 0 else tier + 1,
                seasonPointsLeft = if (tier >= SeasonPass.TIERS) 0 else SeasonPass.POINTS_PER_TIER - SeasonPass.pointsInTier(points),
                seasonPointsPerTier = SeasonPass.POINTS_PER_TIER,
                dailyChallengeDone = dailyChallengeDoneToday()
            )
        )
    }

    // ============================ Insignias (puntos rojos) ============================

    /** Cuántas cosas esperan al jugador. Sirve para el punto rojo del menú. */
    fun pendingBadges(): Int {
        var n = 0
        if (isFreeChestReady()) n++
        if (wheelAllowance().freeLeft > 0) n++
        if (claimableTierCount() > 0) n++
        if (weeklyClaimableCount() > 0) n++
        if (pendingLeagueResult() != null) n++
        return n
    }

    private companion object {
        const val K_CHEST_LAST = "free_chest_last"
        const val K_CHEST_COUNT = "free_chest_count"
        const val K_CHEST_SKIP_DAY = "free_chest_skip_day"
        const val K_CHEST_SKIPS = "free_chest_skips"
        const val K_WHEEL_DAY = "wheel_day"
        const val K_WHEEL_FREE = "wheel_free"
        const val K_WHEEL_AD = "wheel_ad"
        const val K_SEASON_ID = "season_id"
        const val K_SEASON_POINTS = "season_points"
        const val K_SEASON_PREMIUM = "season_premium"
        const val K_SEASON_CLAIMED = "season_claimed"
        const val K_GAME_PTS_DAY = "game_pts_day"
        const val K_GAME_PTS = "game_pts"
        const val K_FIRST_WIN_DAY = "first_win_day"
        const val K_DAILY_CH_DAY = "daily_ch_day"
        const val K_ALL_MISSIONS_DAY = "all_missions_day"
        const val K_PERFECT_DAY = "perfect_day"
        const val K_PERFECT_STREAK = "perfect_streak"
        const val K_PIGGY = "piggy"
        const val K_LAST_OPEN = "last_open_day"
        const val K_REWARDED_LEVEL = "rewarded_level"
        const val K_LEAGUE = "league"
        const val K_NIGHT_WINS = "night_wins"
        const val K_DAWN_WINS = "dawn_wins"
        const val K_DAYS_PLAYED = "days_played"
        const val K_TOTAL_WINS = "total_wins"
        const val K_TOTAL_STARS = "total_stars"
        const val K_BEST_TILE = "best_tile"
        const val K_REVEAL = "reveal_pending"
        const val K_LEAGUE_WEEK = "league_week"
        const val K_LEAGUE_PENDING = "league_pending"

        val _tick = MutableStateFlow(0)
        val _seasonPoints = MutableStateFlow(0)
        val _seasonPremium = MutableStateFlow(false)
        val _seasonClaimed = MutableStateFlow<Set<String>>(emptySet())
        val _piggy = MutableStateFlow(0)
        val _freeChestLast = MutableStateFlow(0L)
        var loaded = false
        var seasonLoaded = false
    }
}
