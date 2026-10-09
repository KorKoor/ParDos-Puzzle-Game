package com.korkoor.pardos.domain.meta

import com.korkoor.pardos.domain.collection.AlbumBonus
import com.korkoor.pardos.domain.collection.ChestType
import com.korkoor.pardos.domain.collection.CollectibleCatalog
import com.korkoor.pardos.domain.collection.PerkKind
import com.korkoor.pardos.domain.collection.PerkRules
import com.korkoor.pardos.domain.collection.Rarity
import com.korkoor.pardos.domain.collection.Series
import com.korkoor.pardos.domain.collection.SeriesPackRules
import com.korkoor.pardos.domain.collection.ShardShop
import com.korkoor.pardos.domain.collection.Showcase
import com.korkoor.pardos.domain.collection.TokenRules
import com.korkoor.pardos.domain.economy.Economy
import com.korkoor.pardos.domain.events.EventCalendar
import com.korkoor.pardos.domain.flow.FlowMeter
import com.korkoor.pardos.domain.flow.FlowTier
import com.korkoor.pardos.domain.flow.WinStreak
import com.korkoor.pardos.domain.level.LevelCatalog
import com.korkoor.pardos.domain.level.LevelRules
import com.korkoor.pardos.domain.model.MissionType
import com.korkoor.pardos.domain.retention.DailyWheel
import com.korkoor.pardos.domain.retention.SeasonCalendar
import com.korkoor.pardos.domain.retention.SeasonPass
import com.korkoor.pardos.domain.rewards.ChapterRewards
import com.korkoor.pardos.domain.rewards.CoinRewards
import com.korkoor.pardos.domain.shop.Avatars
import com.korkoor.pardos.domain.shop.Banners
import com.korkoor.pardos.domain.shop.DailyOffer
import com.korkoor.pardos.domain.shop.DailyOffers
import com.korkoor.pardos.domain.shop.EventSkins
import com.korkoor.pardos.domain.shop.GemPacks
import com.korkoor.pardos.domain.shop.IosStore
import com.korkoor.pardos.domain.shop.MergeFx
import com.korkoor.pardos.domain.shop.MergeFxInventory
import com.korkoor.pardos.domain.shop.OfferItem
import com.korkoor.pardos.domain.shop.Price
import com.korkoor.pardos.domain.shop.ShopPrices
import com.korkoor.pardos.domain.shop.SkinInventory
import com.korkoor.pardos.domain.shop.TileSkin
import com.korkoor.pardos.domain.social.WeekCalendar
import kotlin.random.Random

/**
 * Todo lo que rodea a la partida en la app de iPhone: monedas y gemas, tienda, cofres, álbum, misiones, pase, ligas, perfil.
 * Es el mismo reglamento que usa Android (módulos de `domain`), pero con el estado guardado en un solo texto ([save]/[load]).
 *
 * Uso desde Swift: crear, `load(estado)`, y antes de cada llamada `tick(día, ms, minuto)`. Cada acción devuelve un JSON
 * `{"ok":true,...}`; después de cualquier acción hay que guardar `save()`. Los números enteros viajan como Int (sin decimales).
 */
class MetaSession {
    private val store = MetaStore()
    private val clock = Clock()
    private val wallet = Wallet(store)
    private val col = CollectionOps(store, wallet)
    private val ret = Retention(store, wallet, col, clock)
    private val modes = Modes(store, wallet, col, ret, clock)
    private val ach = Achievements(store, wallet, col)
    private val prestige = Prestige(store, wallet, col, ach, ret)

    // ------------------------------------------------------------------ guardado y reloj

    fun load(state: String) {
        store.import(state)
        com.korkoor.pardos.domain.shop.StudioSkin.config = com.korkoor.pardos.domain.shop.StudioConfig.decode(store.str("studio_cfg").ifEmpty { null })
    }
    fun save(): String = store.export()

    /** Fija el día local (desde 1970), los milisegundos y los minutos desde medianoche. Siempre antes de usar lo demás. */
    fun tick(epochDay: Int, nowMs: Long, minuteOfDay: Int) {
        clock.today = epochDay
        clock.nowMs = nowMs
        clock.minute = minuteOfDay
        ret.rolloverSeasonIfNeeded()
        ret.rolloverLeagueIfNeeded()
    }

    private fun ok(vararg pairs: Pair<String, Any?>): String = obj("ok" to true, *pairs)
    private fun no(reason: String): String = obj("ok" to false, "reason" to reason)

    // ------------------------------------------------------------------ catálogos (estáticos)

    fun skinCatalog(): String = MetaCatalogs.skins()
    fun fxCatalog(): String = MetaCatalogs.fx()
    fun avatarCatalog(): String = MetaCatalogs.avatars()
    fun bannerCatalog(): String = MetaCatalogs.banners()
    fun albumCatalog(): String = MetaCatalogs.album()
    fun economyInfo(): String = MetaCatalogs.economy()
    fun seasonTiers(): String = MetaCatalogs.seasonTiers(ret.seasonId)

    // ------------------------------------------------------------------ progreso de la campaña

    val unlockedLevel: Int get() = store.int("unlocked", 1).coerceIn(1, LevelCatalog.TOTAL_LEVELS)
    fun starsOf(level: Int): Int = store.int("stars_$level")
    fun bestMovesOf(level: Int): Int = store.int("best_$level")

    /** Para traer el progreso que la primera versión de la app guardaba en el teléfono. */
    fun importLevel(level: Int, stars: Int, bestMoves: Int) {
        if (level < 1) return
        if (stars > starsOf(level)) store.setInt("stars_$level", stars)
        if (bestMoves > 0 && (bestMovesOf(level) == 0 || bestMoves < bestMovesOf(level))) store.setInt("best_$level", bestMoves)
    }

    fun importUnlocked(level: Int) { if (level > unlockedLevel) store.setInt("unlocked", level) }

    val tutorialDone: Boolean get() = store.bool("tutorial_done")
    fun markTutorialDone() { store.setBool("tutorial_done", true) }

    fun flag(key: String): Boolean = store.bool("flag_$key")
    fun setFlag(key: String, value: Boolean) { store.setBool("flag_$key", value) }

    val totalStars: Int get() = (1..unlockedLevel).sumOf { starsOf(it) }
    val levelsWon: Int get() = (1..unlockedLevel).count { starsOf(it) > 0 }

    fun chapterStars(chapter: Int): Int {
        val first = chapter * ChapterRewards.LEVELS_PER_CHAPTER + 1
        return (first until first + ChapterRewards.LEVELS_PER_CHAPTER).sumOf { starsOf(it) }
    }

    // ------------------------------------------------------------------ fin de partida

    /**
     * Victoria. [level] es el número de nivel (o el del reto si [daily]). Entrega monedas, experiencia, puntos del pase,
     * misiones, hucha, cofre de capítulo y todo lo demás, y devuelve el desglose para enseñarlo.
     */
    fun onWin(
        level: Int, daily: Boolean, stars: Int, moves: Int, timeMs: Long, maxTile: Int, merges: Int, usedHelp: Boolean,
        kind: String, boss: Boolean, flow: Int
    ): String {
        val st = stars.coerceIn(1, 3)
        if (!daily) modes.clearLevelAttempts(level)
        val firstClear = !daily && starsOf(level) == 0
        val spec = if (daily) null else LevelCatalog.spec(level)
        val rawCoins = CoinRewards.forLevelWin(st, firstClear) + (spec?.let { LevelRules.coinBonus(it) } ?: 0)

        // racha de victorias seguidas
        val winStreak = WinStreak.afterWin(store.int("win_streak"))
        store.setInt("win_streak", winStreak)
        val streakPct = WinStreak.coinBonusPct(winStreak)
        val milestone = WinStreak.milestoneAt(winStreak)
        if (milestone != null) {
            wallet.addGems(milestone.gems)
            wallet.addUndos(milestone.undos)
        }
        val flowPct = FlowMeter.coinBonusPct(FlowTier.entries[flow.coerceIn(0, FlowTier.entries.size - 1)])
        val base = rawCoins * (100 + streakPct + flowPct) / 100
        val eventMult = EventCalendar.coinMultiplier(clock.today)
        val albumPct = AlbumBonus.coinPercent(col.owned, col.foil)
        val withEvent = EventCalendar.apply(base, AlbumBonus.combine(eventMult, col.owned, col.foil))
        val coins = wallet.applyWinBonuses(withEvent)
        wallet.addCoins(coins)

        val bonus = ret.onGameFinished(won = true, dailyChallenge = daily)
        ret.onStars(st)
        if (daily) ret.onDailyChallengeCompleted()
        ret.onTileReached(maxTile)
        ret.addWeekly(com.korkoor.pardos.domain.retention.WeeklyType.MERGE_PAIRS, merges)

        // progreso guardado
        var newBest = false
        var chapterChest: Raw? = null
        if (!daily) {
            if (st > starsOf(level)) store.setInt("stars_$level", st)
            if (bestMovesOf(level) == 0 || moves < bestMovesOf(level)) { store.setInt("best_$level", moves); newBest = true }
            val next = (level + 1).coerceAtMost(LevelCatalog.TOTAL_LEVELS)
            if (next > unlockedLevel) store.setInt("unlocked", next)
            if (level == 1) markTutorialDone()
            val chapter = ChapterRewards.chapterOf(level)
            if (level == ChapterRewards.lastLevelOf(chapter) && !store.bool("chapter_chest_$chapter")) {
                store.setBool("chapter_chest_$chapter", true)
                val r = ChapterRewards.forChapter(chapter)
                wallet.addCoins(r.coins)
                wallet.addGems(r.gems)
                val chest = if (chapter >= 3) ChestType.RARE else ChestType.COMMON
                col.addChests(chest, 1)
                chapterChest = Raw(obj("chapter" to chapter + 1, "coins" to r.coins, "gems" to r.gems, "chest" to chest.name))
            }
        }

        // experiencia y misiones
        val levelRewards = ret.addXpForVictory(st)
        ret.updateMission(MissionType.PLAY_GAMES, 1)
        ret.updateMission(MissionType.WIN_LEVELS, 1)
        ret.updateMission(MissionType.EARN_STARS, st)
        ret.updateMission(MissionType.MERGE_PAIRS, merges)
        ret.updateMission(MissionType.REACH_BLOCK, maxTile)
        val secs = (timeMs / 1000).toInt()
        if (secs > 0) ret.updateMission(MissionType.WIN_UNDER_TIME, secs)
        if (!usedHelp) ret.updateMission(MissionType.WIN_NO_POWERUPS, 1)

        prestige.onLevelWon(kind, boss, flow >= FlowTier.FLOW.ordinal, winStreak)
        if (daily) prestige.onDailyDone()
        modes.logRun(if (daily) "Reto diario" else "Campaña", (if (daily) "" else "Nivel " + level + " · ") + "★".repeat(st) + " · " + moves + " mov")
        prestige.refresh(unlockedLevel)
        val teaser = if (daily) null else com.korkoor.pardos.domain.flow.NextLevelTeaser.after(level)
        return ok(
            "coins" to coins, "rawCoins" to rawCoins, "streakPct" to streakPct, "flowPct" to flowPct, "albumPct" to albumPct,
            "eventMult" to eventMult, "vipBonus" to wallet.vip, "boostActive" to (wallet.boostWins > 0),
            "firstClear" to firstClear, "firstWinCoins" to bonus.firstWinCoins, "piggyGems" to bonus.piggyGems,
            "seasonPoints" to bonus.seasonPoints, "newBest" to newBest, "stars" to st, "winStreak" to winStreak,
            "winMilestone" to milestone?.let { Raw(obj("gems" to it.gems, "undos" to it.undos)) },
            "levelRewards" to levelRewards.map { Raw(obj("level" to it.level, "coins" to it.coins, "gems" to it.gems, "chest" to it.chest?.name)) },
            "chapterChest" to chapterChest,
            "newSkins" to bonus.newSkins.map { it.id },
            "teaser" to teaser?.let { Raw(obj("level" to it.nextLevel, "title" to it.title, "boss" to it.isBoss, "toChest" to it.levelsToChest)) },
            "nextGoal" to ret.nextGoal()?.let { Raw(obj("title" to it.title, "detail" to it.detail, "progress" to it.progress.toDouble(), "kind" to it.kind.name)) }
        )
    }

    /** Derrota o abandono con jugadas hechas. */
    fun onLoss(level: Int, daily: Boolean, maxTile: Int, merges: Int): String {
        store.setInt("win_streak", WinStreak.afterLoss(store.int("win_streak")))
        if (!daily) modes.registerLevelFailure(level)
        ret.onGameFinished(won = false, dailyChallenge = daily)
        ret.updateMission(MissionType.PLAY_GAMES, 1)
        ret.updateMission(MissionType.MERGE_PAIRS, merges)
        ret.updateMission(MissionType.REACH_BLOCK, maxTile)
        ret.onTileReached(maxTile)
        ret.addWeekly(com.korkoor.pardos.domain.retention.WeeklyType.MERGE_PAIRS, merges)
        return ok("winStreak" to store.int("win_streak"), "nextGoal" to ret.nextGoal()?.let { Raw(obj("title" to it.title, "detail" to it.detail, "progress" to it.progress.toDouble(), "kind" to it.kind.name)) })
    }


    // ------------------------------------------------------------------ ayuda, torre, carrera, duelo

    /** Ayuda por intentos fallidos: `percent` agranda movimientos y reloj; también regala los Deshacer que falten. */
    fun prepareLevel(level: Int): String {
        val a = modes.prepareLevel(level)
        return ok("percent" to a.percent, "message" to a.message, "tier" to a.tier, "undos" to a.undos)
    }

    fun towerStart(): String { modes.towerStart(); return modes.towerJson() }
    fun towerInfo(): String = modes.towerJson()
    fun towerEnd() { modes.towerEnd() }

    fun towerWin(maxTile: Int, merges: Int, kind: String, boss: Boolean, flow: Int): String {
        val c = modes.towerWin(maxTile, merges)
        prestige.onLevelWon(kind, boss, flow >= FlowTier.FLOW.ordinal, store.int("win_streak"))
        prestige.refresh(unlockedLevel)
        return ok("coins" to c.coins, "gems" to c.gems, "heart" to c.heart, "hearts" to c.hearts, "floor" to modes.towerFloor)
    }

    fun towerNext(): String { modes.towerNext(); return modes.towerJson() }

    fun towerLose(): String {
        val l = modes.towerLose()
        return ok("hearts" to l.hearts, "over" to l.over, "newRecord" to l.newRecord, "floor" to modes.towerFloor, "best" to modes.towerBest)
    }

    fun raceStage(n: Int): String = modes.raceStageJson(n)
    fun raceNextTime(n: Int, remainingMs: Long): Long = modes.raceAfterStage(n, remainingMs)
    fun raceStageCleared() { modes.raceStageCleared() }

    fun raceFinish(stages: Int, merges: Int, maxTile: Int): String {
        val r = modes.raceFinish(stages, merges, maxTile)
        return ok("coins" to r.coins, "best" to r.best, "newRecord" to r.newRecord, "stages" to stages)
    }

    fun remoteNewSeed(): Long = modes.remoteNewSeed()
    fun remoteDecode(text: String): String = modes.remoteDecodeJson(text)
    fun remoteFinishCreator(seed: Long, score: Int, name: String): String = modes.remoteFinishCreator(seed, score, name)
    fun remoteFinishChallenged(seed: Long, theirScore: Int, name: String, myScore: Int): String = modes.remoteFinishChallenged(seed, theirScore, name, myScore)
    fun remoteHistory(): String = modes.remoteHistoryJson()
    fun tablesInfo(): String = modes.tablesInfoJson()
    fun tablesWon() { modes.tablesWon() }
    fun duelConfig(): String = modes.duelConfigJson()
    fun duelResult(score1: Int, score2: Int): String = modes.duelResultJson(score1, score2)
    fun customFinished(score: Int, won: Boolean, merges: Int, maxTile: Int) { modes.customFinished(score, won, merges, maxTile) }
    fun records(): String = modes.recordsJson()
    fun recentRuns(): String = modes.runsJson()


    // ------------------------------------------------------------------ logros y prestigio

    /** Mira los logros con el estado del tablero (se llama después de cada jugada). Devuelve los que se desbloquearon. */
    fun checkAchievements(
        completed: Boolean, level: Int, moves: Int, elapsedMs: Long, score: Int, combo: Int, empty: Int, hasMoves: Boolean,
        size: Int, mode: String, tiles: String
    ): String {
        val ctx = parseAchContext(completed, level, moves, elapsedMs, score, combo, empty, hasMoves, size, mode, tiles)
        val fresh = ach.check(ctx)
        if (fresh.isNotEmpty()) prestige.refresh(unlockedLevel)
        return obj("unlocked" to fresh.map {
            robj("id" to it.id, "title" to it.title, "desc" to it.description, "rarity" to it.rarity.name, "coins" to it.coins, "gems" to it.gems, "chest" to it.chest?.name)
        })
    }

    fun achievementsList(): String = obj("list" to ach.listJson(), "done" to ach.unlocked.size, "total" to com.korkoor.pardos.domain.achievements.AchievementCatalog.all.size)

    fun prestigeState(): String {
        prestige.refresh(unlockedLevel)
        return prestige.stateJson(unlockedLevel)
    }

    /** Avisos de prestigio pendientes (hitos, rangos, títulos, Platino); se entregan una sola vez. */
    fun takePrestigeEvents(): String = arr(prestige.takeEvents())

    fun equipTitle(id: String): String = if (prestige.equip(id, unlockedLevel)) ok() else no("Aún no tienes ese título")
    fun buyTitle(id: String): String = if (prestige.buy(id)) ok() else no("No alcanzan las gemas")

    private fun afterChange() { prestige.refresh(unlockedLevel) }


    // ------------------------------------------------------------------ avisos (notificaciones locales)

    /** Los avisos que hay que programar ahora (el mismo planificador que Android; Swift los pasa a notificaciones locales). */
    fun reminders(): String {
        val today = clock.today
        val owned = wallet.ownedSkins
        val activeEvent = ret.activeSkinEvents().firstOrNull { e -> EventSkins.skinFor(e.type)?.let { it.id !in owned } == true }
        val missions = ret.todayMissions()
        val league = ret.league
        val input = com.korkoor.pardos.domain.retention.ReminderInput(
            nowMs = clock.nowMs, minuteOfDay = clock.minute, streak = ret.streak,
            freeChestLastMs = store.long("free_chest_last"), seasonDaysLeft = SeasonCalendar.daysLeft(today),
            seasonClaimable = ret.claimableTierCount(), seasonTierReached = SeasonPass.tierFor(ret.seasonPoints),
            weeklyOpen = ret.weekly().count { !it.claimed }, daysLeftInWeek = WeekCalendar.daysLeft(today),
            wheelFreeLeft = ret.wheelAllowance().freeLeft, piggyGems = ret.piggy,
            leagueName = league.displayName,
            leagueStarsToPromote = com.korkoor.pardos.domain.retention.Leagues.starsToPromote(league, ret.leagueWeekStars()),
            leagueAtRisk = com.korkoor.pardos.domain.retention.Leagues.atRisk(league, ret.leagueWeekStars()),
            eventName = activeEvent?.let { EventSkins.eventName(it.type) } ?: "",
            eventSkinName = activeEvent?.let { EventSkins.skinFor(it.type)?.displayName } ?: "",
            eventDaysLeft = activeEvent?.daysLeft(today) ?: Int.MAX_VALUE,
            eventWinsLeft = activeEvent?.let { EventSkins.winsLeft(ret.eventWins(it)) } ?: 0,
            missionsClaimable = missions.count { it.isCompleted && !ret.missionClaimed(it.id) },
            missionsOpen = missions.count { !ret.missionClaimed(it.id) },
            perfectDays = ret.perfectDays(), perfectToday = ret.perfectToday(), unopenedChests = col.totalChests,
            dailyChallengeOpen = !ret.dailyChallengeDoneToday(),
            upcomingEvents = EventCalendar.upcoming(today, 14).mapNotNull { (type, days) ->
                val skin = EventSkins.skinFor(type) ?: return@mapNotNull null
                if (skin.id in owned) null
                else com.korkoor.pardos.domain.retention.UpcomingEvent(type.ordinal, EventSkins.eventName(type), skin.displayName, days)
            }
        )
        val plan = com.korkoor.pardos.domain.retention.ReminderPlanner.plan(input)
        val halloween = com.korkoor.pardos.domain.retention.SeasonalCopy.isHalloweenWindow(today)
        val finalPlan = com.korkoor.pardos.domain.retention.SeasonalCopy.apply(plan, halloween)
        return arr(finalPlan.map { robj("key" to it.key, "id" to it.id, "title" to it.title, "body" to it.body, "delayMs" to it.delayMs) })
    }

    /** ¿Toca preguntar si quiere avisos? (después de la primera victoria, pocas veces y separadas). */
    fun shouldAskNotifications(granted: Boolean, enabled: Boolean): Boolean {
        val last = if (store.has("notif_ask_day")) store.int("notif_ask_day") else null
        return com.korkoor.pardos.domain.retention.NotificationPrimer.shouldAsk(
            permissionNeeded = true, granted = granted, enabledInSettings = enabled,
            hasWonAGame = store.int("total_wins") > 0, lastAskDay = last, askCount = store.int("notif_ask_count"), today = clock.today
        )
    }

    fun noteNotificationAsked() {
        store.addInt("notif_ask_count", 1)
        store.setInt("notif_ask_day", clock.today)
    }

    // ------------------------------------------------------------------ al abrir la app

    /** Una vez al día: racha, regalo de regreso, escudos, fichas. Devuelve lo que hay que celebrar. */
    fun openApp(): String {
        val c = ret.checkIn()
        return ok(
            "change" to c.change.name, "streak" to c.streak, "daysAway" to c.daysAway, "tokens" to c.tokens,
            "comeback" to c.comeback?.let { Raw(obj("coins" to it.coins, "chest" to it.chest.name, "freezes" to it.freezes)) },
            "milestone" to c.milestone?.let { Raw(obj("days" to it.days, "coins" to it.coins, "gems" to it.gems, "chest" to it.chest?.name)) },
            "repairLost" to c.repairLost, "repairCost" to ret.repairCost(),
            "vipGems" to wallet.claimVipDaily(clock.today),
            "reveals" to ret.takePendingReveals().map { it.id }
        )
    }

    fun repairStreak(): String = if (ret.repairStreak()) ok() else no("No alcanzan las gemas")
    fun declineRepair(): String { ret.declineRepair(); return ok() }

    fun claimDailyReward(): String {
        val r = ret.claimDailyReward() ?: return no("Ya lo reclamaste hoy")
        return ok("coins" to r.coins, "gems" to r.gems, "isChest" to r.isChest)
    }

    fun claimFreeChest(): String {
        val t = ret.claimFreeChest() ?: return no("Aún falta")
        return ok("chest" to t.name)
    }

    fun skipFreeChestWithGems(): String = if (ret.skipFreeChestWithGems()) ok() else no("No alcanzan las gemas")

    fun spinWheel(seed: Long): String {
        val i = ret.spinWheel(Random(seed)) ?: return no("No quedan giros")
        return ok("index" to i)
    }

    fun wheelSlices(): String = arr(DailyWheel.slices.map {
        robj("kind" to it.kind.name, "amount" to it.amount, "chest" to it.chest?.name, "label" to it.label, "weight" to it.weight)
    })

    // ------------------------------------------------------------------ cofres y álbum

    fun openChest(type: String, seed: Long): String {
        val t = ChestType.entries.firstOrNull { it.name == type } ?: return no("Cofre desconocido")
        val before = col.owned
        val res = col.openChest(t, Random(seed)) ?: return no("No tienes ese cofre")
        ret.onChestOpened()
        // la serie o el álbum podrían haberse completado con este cofre
        val drops = res.drops.map {
            obj(
                "id" to it.collectible.id, "new" to it.isNew, "shards" to it.shards, "bonus" to it.bonus,
                "rarity" to it.collectible.rarity.name
            )
        }
        val reveals = ret.checkHiddenSkins().map { it.id }
        afterChange()
        return ok("drops" to drops.map { Raw(it) }, "pieces" to col.owned.size, "was" to before.size, "reveals" to reveals)
    }

    fun buyChest(type: String, withGems: Boolean): String {
        val t = ChestType.entries.firstOrNull { it.name == type } ?: return no("Cofre desconocido")
        val price: Price = (if (withGems) ShopPrices.chestGems(t) else ShopPrices.chestCoins(t)) ?: return no("Ese cofre no se vende así")
        return if (col.buyChest(t, price)) ok() else no(if (withGems) "No alcanzan las gemas" else "No alcanzan las monedas")
    }

    fun openSeriesPack(seriesId: String, withGems: Boolean, seed: Long): String {
        val series = Series.entries.firstOrNull { it.id == seriesId } ?: return no("Serie desconocida")
        val drops = col.openSeriesPack(series, withGems, Random(seed)) ?: return no(if (SeriesPackRules.canOpen(series, col.owned)) "No alcanza el saldo" else "La serie ya está completa")
        return ok("drops" to drops.map { Raw(obj("id" to it.collectible.id, "new" to it.isNew, "shards" to it.shards, "bonus" to it.bonus, "rarity" to it.collectible.rarity.name)) })
    }

    fun sellPiece(id: String, qty: Int): String {
        val coins = col.sell(id, qty) ?: return no("No tienes repetidas")
        return ok("coins" to coins)
    }

    fun sellAll(maxRarity: String): String {
        val r = Rarity.entries.firstOrNull { it.name == maxRarity } ?: return no("Rareza desconocida")
        val (n, coins) = col.sellAllUpTo(r)
        return ok("count" to n, "coins" to coins)
    }

    fun recyclePiece(id: String, qty: Int): String {
        val shards = col.recycle(id, qty) ?: return no("No tienes repetidas")
        return ok("shards" to shards)
    }

    fun craftPiece(id: String): String {
        val c = CollectibleCatalog.byId(id) ?: return no("Pieza desconocida")
        return if (col.craft(c)) { afterChange(); ok() } else no("No alcanza la esencia")
    }

    fun foilPiece(id: String): String {
        val c = CollectibleCatalog.byId(id) ?: return no("Pieza desconocida")
        return if (col.upgradeFoil(c)) { afterChange(); ok() } else no("No alcanza la esencia")
    }

    fun buyShardPack(): String = if (col.buyShardPack(clock.today)) ok() else no("No alcanzan las gemas o llegaste al tope de hoy")
    fun buyTradeToken(): String = if (col.buyToken(clock.today)) ok() else no("No alcanzan las gemas o llegaste al tope de hoy")

    fun claimSeriesReward(seriesId: String): String {
        val s = Series.entries.firstOrNull { it.id == seriesId } ?: return no("Serie desconocida")
        val r = col.claimSeries(s) ?: return no("Aún no está completa")
        afterChange()
        return ok("coins" to r.first, "gems" to r.second)
    }

    fun claimAlbumReward(): String = if (col.claimAlbum()) ok("gems" to CollectionOps.ALBUM_GEMS) else no("Aún no está completo")

    fun setShowcase(ids: String) { col.setShowcase(ids.split(',').filter { it.isNotBlank() }) }
    fun unlockShowcaseSlot(): String = if (col.unlockShowcaseSlot()) ok() else no("No alcanzan las gemas")

    /** Estado del álbum para pintar la pantalla. */
    fun albumState(): String {
        val owned = col.owned
        val foil = col.foil
        val bonus = AlbumBonus.breakdown(owned, foil)
        val totals = PerkRules.totals(owned, foil)
        return obj(
            "copies" to Raw(obj(*col.copies.entries.map { it.key to it.value }.toTypedArray())),
            "foil" to foil.toList(), "owned" to owned.size, "total" to CollectibleCatalog.all.size,
            "shards" to col.shards, "tokens" to col.tokens,
            "claimedSeries" to col.claimedSeries.toList(), "albumClaimed" to col.albumClaimed, "albumClaimable" to col.isAlbumClaimable,
            "claimableSeries" to Series.entries.filter { col.isSeriesClaimable(it) }.map { it.id },
            "showcase" to col.showcase, "slots" to col.showcaseSlots, "maxSlots" to Showcase.MAX_SLOTS,
            "slotCost" to (if (col.showcaseSlots < Showcase.MAX_SLOTS) Showcase.unlockCost(col.showcaseSlots + 1) else 0),
            "shardPacksToday" to col.shardPacksToday(clock.today), "tokensToday" to col.tokensBoughtToday(clock.today),
            "coinPercent" to bonus.total,
            "perks" to PerkKind.entries.map { Raw(obj("kind" to it.name, "label" to it.labelEs, "tenths" to (totals[it] ?: 0), "cap" to PerkRules.cap(it))) },
            "chests" to Raw(obj(*ChestType.entries.map { it.name to col.chestCount(it) }.toTypedArray()))
        )
    }

    // ------------------------------------------------------------------ tienda

    fun buySkin(id: String, discount: Int): String {
        val skin = TileSkin.entries.firstOrNull { it.id == id } ?: return no("Skin desconocida")
        return when (wallet.buySkin(skin, discount)) {
            is SkinInventory.Purchase.Ok -> ok()
            SkinInventory.Purchase.AlreadyOwned -> no("Ya la tienes")
            SkinInventory.Purchase.NotEnoughCoins -> no("No alcanzan las monedas")
            SkinInventory.Purchase.NotEnoughGems -> no("No alcanzan las gemas")
            SkinInventory.Purchase.NotPurchasable -> no("Esta skin no se compra")
        }
    }

    fun equipSkin(id: String): String {
        val skin = TileSkin.entries.firstOrNull { it.id == id } ?: return no("Skin desconocida")
        return if (wallet.equipSkin(skin)) ok() else no("Aún no la tienes")
    }

    fun buyFx(id: String, discount: Int): String {
        val fx = MergeFx.entries.firstOrNull { it.id == id } ?: return no("Efecto desconocido")
        return when (wallet.buyFx(fx, discount)) {
            is MergeFxInventory.Purchase.Ok -> ok()
            MergeFxInventory.Purchase.AlreadyOwned -> no("Ya lo tienes")
            MergeFxInventory.Purchase.NotEnoughCoins -> no("No alcanzan las monedas")
            MergeFxInventory.Purchase.NotEnoughGems -> no("No alcanzan las gemas")
            MergeFxInventory.Purchase.NotPurchasable -> no("Este efecto no se compra")
        }
    }

    fun equipFx(id: String): String {
        val fx = MergeFx.entries.firstOrNull { it.id == id } ?: return no("Efecto desconocido")
        return if (wallet.equipFx(fx)) ok() else no("Aún no lo tienes")
    }

    fun buyAvatar(id: Int): String = when (wallet.buyAvatar(id)) {
        is Avatars.Purchase.Ok -> ok()
        Avatars.Purchase.AlreadyOwned -> no("Ya lo tienes")
        Avatars.Purchase.NotEnoughCoins -> no("No alcanzan las monedas")
        Avatars.Purchase.NotPurchasable -> no("Este avatar no se compra")
    }

    fun buyBanner(id: Int): String = when (wallet.buyBanner(id)) {
        is Banners.Purchase.Ok -> ok()
        Banners.Purchase.AlreadyOwned -> no("Ya lo tienes")
        Banners.Purchase.NotEnoughCoins -> no("No alcanzan las monedas")
        Banners.Purchase.NotEnoughGems -> no("No alcanzan las gemas")
        Banners.Purchase.NotPurchasable -> no("Este banner no se compra")
    }

    fun buyUndos(pack: Int): String = if (wallet.buyUndos(pack)) ok() else no("No alcanzan las monedas")
    fun buyExtraTimes(pack: Int): String = if (wallet.buyExtraTimes(pack)) ok() else no("No alcanzan las monedas")
    fun buyFreeze(): String = if (wallet.buyFreeze()) ok() else no("No alcanzan las monedas o ya tienes el máximo")
    fun buyCoinBoost(): String = if (wallet.buyCoinBoost()) ok() else no("No alcanzan las gemas o el impulso ya está al máximo")
    fun exchangeGems(gems: Int): String = if (wallet.exchangeGems(gems)) ok("coins" to com.korkoor.pardos.domain.shop.CoinShop.coinsForGems(gems)) else no("No alcanzan las gemas")

    fun buyEventSkin(eventId: String): String {
        val e = EventCalendar.seasonalOn(clock.today).firstOrNull { it.id == eventId } ?: return no("El evento ya terminó")
        return if (ret.buyEventSkin(e)) ok() else no("No alcanzan las gemas o ya la tienes")
    }

    /** Una compra de gemas simulada (la versión de prueba no cobra): sirve para ver la tienda llena. */
    fun grantGems(n: Int) { wallet.addGems(n) }
    fun grantCoins(n: Int) { wallet.addCoins(n) }

    fun useUndo(): Boolean = wallet.useUndo()

    /** Texto para compartir una victoria (el mismo formato que Android; sin enlace de Google Play). [dailyDay] = -1 si no es el reto diario. */
    fun shareVictory(modeName: String, stars: Int, targetTile: Int, moves: Int, timeMs: Long, dailyDay: Int): String {
        val text = com.korkoor.pardos.domain.social.ShareText.victory(
            modeName, stars, targetTile, moves, timeMs, ret.streak, if (dailyDay >= 0) dailyDay else null
        )
        return text.substringBeforeLast("\n") + "\nParDos para iPhone"
    }

    /** Paga la segunda oportunidad con gemas. */
    fun buyRevive(): String = if (wallet.spendGems(Economy.REVIVE_PRICE_GEMS)) ok() else no("No alcanzan las gemas")

    /** Paga un poder manual (Escoba o Unir) con monedas. */
    fun buyManualPower(): String = if (wallet.vip || wallet.spendCoins(Economy.MANUAL_POWER_PRICE_COINS)) ok() else no("No alcanzan las monedas")

    /** ¿Ya se cobró el cofre de este capítulo (0 = el primero)? */
    fun chapterChestClaimed(chapter: Int): Boolean = store.bool("chapter_chest_$chapter")
    fun useExtraTime(): Boolean = wallet.useExtraTime()

    // ------------------------------------------------------------------ misiones, pase, liga

    fun claimMission(id: Int): String {
        val c = ret.claimMission(id) ?: return no("No se puede cobrar")
        return ok("coins" to c.coins, "allDone" to c.bonusAll, "perfectGems" to c.perfectMilestoneGems)
    }

    fun claimWeekly(id: String): String {
        val m = ret.claimWeekly(id) ?: return no("No se puede cobrar")
        return ok("coins" to m.coins)
    }

    fun claimWeeklyBonus(): String = if (ret.claimWeeklyBonus()) ok("gems" to com.korkoor.pardos.domain.retention.WeeklyMissions.COMPLETION_GEMS) else no("Aún no")

    fun claimTier(tier: Int, premium: Boolean): String {
        val r = ret.claimTier(tier, premium) ?: return no("No se puede cobrar")
        return ok("coins" to r.coins, "gems" to r.gems, "chest" to r.chest?.name, "skin" to r.skin?.id, "avatar" to r.avatar, "banner" to r.banner, "fx" to r.fx?.id)
    }

    fun claimAllTiers(): String {
        val list = ret.claimAllTiers()
        return ok("count" to list.size, "coins" to list.sumOf { it.coins }, "gems" to list.sumOf { it.gems })
    }

    fun buySeasonTier(): String = if (ret.buyTier()) ok() else no("No alcanzan las gemas")

    /** Compra del pase premium: en la versión de prueba no se cobra (no hay tienda de pago). */
    fun unlockSeasonPremium() { ret.unlockPremium() }

    fun claimLeague(): String {
        val r = ret.claimLeagueResult() ?: return no("Nada pendiente")
        return ok("outcome" to r.outcome.name, "from" to r.from.displayName, "to" to r.to.displayName, "coins" to r.coins, "gems" to r.gems, "chest" to r.chest?.name)
    }

    // ------------------------------------------------------------------ perfil

    val profileName: String get() = store.str("name", "Jugador Zen")
    fun setProfileName(name: String) { val n = name.trim(); if (n.isNotEmpty()) store.setStr("name", n.take(18)) }

    fun setAvatar(id: Int): Boolean {
        if (!Avatars.isOwned(id, wallet.ownedAvatars)) return false
        wallet.avatarId = id
        return true
    }

    fun setBanner(id: Int): Boolean {
        if (!Banners.isOwned(id, wallet.ownedBanners)) return false
        wallet.bannerId = id
        return true
    }

    // ------------------------------------------------------------------ estado general para pintar

    /** Lo necesario para el menú, la barra de monedas y las tarjetas de hoy. Se pide después de cada acción. */
    fun state(): String {
        val today = clock.today
        val dr = ret.dailyRewardPending()
        val points = ret.seasonPoints
        val tier = SeasonPass.tierFor(points)
        val lg = ret.league
        val pending = ret.pendingLeagueResult()
        val offer: DailyOffer = DailyOffers.forDay(today)
        val goal = ret.nextGoal()
        val events = EventCalendar.activeOn(today).map {
            Raw(obj("id" to it.id, "name" to EventSkins.eventName(it.type), "coinMult" to it.coinMultiplier, "xpMult" to it.xpMultiplier, "daysLeft" to it.daysLeft(today)))
        }
        val missions = ret.todayMissions().map {
            Raw(obj(
                "id" to it.id, "desc" to it.description, "target" to it.targetValue, "progress" to minOf(it.currentProgress, it.targetValue),
                "done" to it.isCompleted, "claimed" to ret.missionClaimed(it.id), "coins" to CoinRewards.forMission(it.xpReward), "type" to it.type.name
            ))
        }
        val weekly = ret.weekly().map {
            Raw(obj("id" to it.mission.id, "title" to it.mission.title, "target" to it.mission.target, "progress" to minOf(it.progress, it.mission.target), "done" to it.done, "claimed" to it.claimed, "coins" to it.mission.coins))
        }
        val activeEventSkins = ret.activeSkinEvents().map {
            val skin = EventSkins.skinFor(it.type)
            Raw(obj("id" to it.id, "skin" to skin?.id, "name" to EventSkins.eventName(it.type), "wins" to ret.eventWins(it), "need" to EventSkins.WINS_REQUIRED, "daysLeft" to it.daysLeft(today), "owned" to (skin != null && skin.id in wallet.ownedSkins)))
        }
        return obj(
            "coins" to wallet.coins, "gems" to wallet.gems, "shards" to col.shards, "tokens" to col.tokens,
            "undos" to wallet.undos, "freezes" to wallet.freezes, "extraTimes" to wallet.extraTimes,
            "vip" to wallet.vip, "boostWins" to wallet.boostWins, "piggy" to ret.piggy, "canBreakPiggy" to ret.canBreakPiggy(),
            "chests" to Raw(obj(*ChestType.entries.map { it.name to col.chestCount(it) }.toTypedArray())), "totalChests" to col.totalChests,
            "streak" to ret.streak, "bestStreak" to ret.bestStreak, "winStreak" to store.int("win_streak"),
            "playerLevel" to ret.playerLevel, "xp" to ret.xp, "xpNext" to ret.xpToNext,
            "name" to profileName, "avatar" to wallet.avatarId, "banner" to wallet.bannerId,
            "unlocked" to unlockedLevel, "totalStars" to totalStars, "levelsWon" to levelsWon, "tutorialDone" to tutorialDone,
            "ownedSkins" to wallet.ownedSkins.toList(), "equippedSkin" to wallet.equippedSkin.id,
            "ownedFx" to wallet.ownedFx.toList(), "equippedFx" to wallet.equippedFx.id,
            "ownedAvatars" to wallet.ownedAvatars.toList(), "ownedBanners" to wallet.ownedBanners.toList(),
            "dailyReward" to Raw(obj(
                "claimable" to (dr != null), "day" to com.korkoor.pardos.domain.rewards.DailyRewards.dayInCycle(ret.dailyRewardStreak()),
                "coins" to (dr?.coins ?: 0), "gems" to (dr?.gems ?: 0), "isChest" to (dr?.isChest ?: false)
            )),
            "cycle" to com.korkoor.pardos.domain.rewards.DailyRewards.cycle.map { Raw(obj("coins" to it.coins, "gems" to it.gems, "isChest" to it.isChest)) },
            "freeChest" to Raw(obj("ready" to ret.freeChestReady(), "remainingMs" to ret.freeChestRemainingMs(), "type" to ret.nextFreeChestType().name,
                "skipCost" to com.korkoor.pardos.domain.retention.FreeChest.skipCostGems(ret.freeChestRemainingMs()))),
            "wheel" to Raw(obj("freeLeft" to ret.wheelAllowance().freeLeft, "adLeft" to ret.wheelAllowance().adLeft)),
            "missions" to missions, "perfectDays" to ret.perfectDays(), "perfectToday" to ret.perfectToday(),
            "weekly" to weekly, "weeklyBonusReady" to ret.canClaimWeeklyBonus(), "weeklyBonusClaimed" to ret.weeklyBonusClaimed(),
            "season" to Raw(obj(
                "id" to ret.seasonId, "name" to SeasonCalendar.name(ret.seasonId), "daysLeft" to SeasonCalendar.daysLeft(today),
                "points" to points, "tier" to tier, "pointsInTier" to SeasonPass.pointsInTier(points), "premium" to ret.seasonPremium,
                "claimable" to ret.claimableTierCount(), "skipCost" to SeasonPass.skipCostGems(points),
                "claimed" to (1..SeasonPass.TIERS).flatMap { t -> listOf(if (ret.isTierClaimed(t, false)) "f$t" else null, if (ret.isTierClaimed(t, true)) "p$t" else null) }.filterNotNull(),
                "skin" to SeasonCalendar.skinFor(ret.seasonId).id
            )),
            "league" to Raw(obj(
                "id" to lg.name, "name" to lg.displayName, "next" to lg.next()?.displayName, "weekStars" to ret.leagueWeekStars(),
                "promote" to lg.promoteStars, "keep" to lg.keepStars, "weekDaysLeft" to WeekCalendar.daysLeft(today),
                "pending" to pending?.let { Raw(obj("outcome" to it.outcome.name, "from" to it.from.displayName, "to" to it.to.displayName, "stars" to it.stars, "coins" to it.coins, "gems" to it.gems, "chest" to it.chest?.name)) }
            )),
            "offer" to Raw(offerJson(offer)),
            "events" to events, "eventSkins" to activeEventSkins,
            "nextGoal" to goal?.let { Raw(obj("title" to it.title, "detail" to it.detail, "progress" to it.progress.toDouble(), "kind" to it.kind.name)) },
            "badges" to ret.pendingBadges() + (if (dr != null) 1 else 0),
            "repair" to (if (ret.pendingRepair() > 0) Raw(obj("lost" to ret.pendingRepair(), "cost" to ret.repairCost())) else null),
            "coinPercent" to AlbumBonus.coinPercent(col.owned, col.foil),
            "dayOfWeek" to EventCalendar.dayOfWeek(today),
            "title" to com.korkoor.pardos.domain.prestige.ProfileTitles.byId(prestige.equippedTitleId).name,
            "rank" to prestige.rank(unlockedLevel).title, "prestige" to prestige.score(unlockedLevel)
        )
    }

    private fun offerJson(o: DailyOffer): String {
        val item = o.item
        return when (item) {
            is OfferItem.SkinOffer -> obj("kind" to "skin", "id" to item.skin.id, "discount" to o.discountPercent,
                "coin" to ShopPrices.skin(item.skin).discounted(o.discountPercent).coins, "gem" to ShopPrices.skin(item.skin).discounted(o.discountPercent).gems,
                "owned" to (item.skin.id in wallet.ownedSkins))
            is OfferItem.ChestOffer -> {
                val p = ShopPrices.chestCoins(item.type) ?: ShopPrices.chestGems(item.type) ?: Price()
                val d = p.discounted(o.discountPercent)
                obj("kind" to "chest", "id" to item.type.name, "discount" to o.discountPercent, "coin" to d.coins, "gem" to d.gems, "owned" to false)
            }
        }
    }

    /** Compra la oferta del día (skin o cofre) con el descuento. */
    fun buyDailyOffer(): String {
        val o = DailyOffers.forDay(clock.today)
        if (store.bool("offer_${o.day}")) return no("Ya la compraste hoy")
        val item = o.item
        val result = when (item) {
            is OfferItem.SkinOffer -> buySkin(item.skin.id, o.discountPercent)
            is OfferItem.ChestOffer -> {
                val p = (ShopPrices.chestCoins(item.type) ?: ShopPrices.chestGems(item.type) ?: Price()).discounted(o.discountPercent)
                if (col.buyChest(item.type, p)) ok() else no("No alcanza el saldo")
            }
        }
        if (result.contains("\"ok\":true")) store.setBool("offer_${o.day}", true)
        return result
    }

    /** Tienda de pago de iPhone: packs de gemas (escalones de precio de Apple) y ofertas especiales, con lo que ya tienes. */
    fun storeProducts(): String {
        val packs = IosStore.gemPacks.map { p ->
            robj(
                "id" to p.id, "name" to IosStore.packName(p.id), "gems" to p.gems, "usdCents" to p.usdCents,
                "price" to IosStore.priceText(p.usdCents), "bonus" to IosStore.bonusPercent(p),
                "first" to !store.bool("purchased_${p.id}"), "best" to (p.id == IosStore.BEST_VALUE_ID)
            )
        }
        val specials = IosStore.specials.map { sp ->
            val owned = when (sp.id) {
                com.korkoor.pardos.domain.shop.ShopCatalog.STARTER_PACK -> wallet.starterClaimed
                com.korkoor.pardos.domain.shop.ShopCatalog.VIP_FOREVER -> wallet.vip
                com.korkoor.pardos.domain.shop.ShopCatalog.SKIN_STUDIO -> TileSkin.STUDIO.id in wallet.ownedSkins
                com.korkoor.pardos.domain.shop.ShopCatalog.SEASON_PASS -> ret.seasonPremium
                else -> false
            }
            val available = when (sp.id) {
                com.korkoor.pardos.domain.shop.ShopCatalog.PIGGY_BREAK -> ret.canBreakPiggy()
                else -> true
            }
            robj(
                "id" to sp.id, "name" to sp.name, "blurb" to sp.blurb, "usdCents" to sp.usdCents,
                "price" to IosStore.priceText(sp.usdCents), "owned" to owned, "available" to available
            )
        }
        return obj("packs" to packs, "specials" to specials)
    }

    /** Simula la compra de un producto (la versión de prueba no cobra; con StoreKit esto lo confirma la App Store). */
    fun testBuyProduct(id: String): String {
        val pack = IosStore.gemPack(id)
        if (pack != null) {
            val first = !store.bool("purchased_$id")
            val gems = GemPacks.gemsForPurchase(pack, first)
            store.setBool("purchased_$id", true)
            wallet.addGems(gems)
            return ok("gems" to gems)
        }
        return when (id) {
            com.korkoor.pardos.domain.shop.ShopCatalog.STARTER_PACK ->
                if (wallet.claimStarterPack(col)) ok("gems" to Economy.STARTER_GEMS) else no("Ya reclamaste el pack inicial")
            com.korkoor.pardos.domain.shop.ShopCatalog.SKIN_STUDIO ->
                if (TileSkin.STUDIO.id in wallet.ownedSkins) no("Ya tienes Studio") else { wallet.grantSkin(TileSkin.STUDIO); ok() }
            com.korkoor.pardos.domain.shop.ShopCatalog.VIP_FOREVER ->
                if (wallet.vip) no("Ya eres VIP") else { wallet.setVip(true); ok() }
            com.korkoor.pardos.domain.shop.ShopCatalog.SEASON_PASS ->
                if (ret.seasonPremium) no("Ya tienes el pase premium de este mes") else { ret.unlockPremium(); ok() }
            com.korkoor.pardos.domain.shop.ShopCatalog.PIGGY_BREAK -> {
                val gems = ret.breakPiggy()
                if (gems > 0) ok("gems" to gems) else no("La hucha todavía está vacía")
            }
            else -> no("Producto desconocido")
        }
    }

    // ------------------------------------------------------------------ amigos sin cuenta (tarjetas)

    private fun playerId(): String {
        if (!store.has("player_id")) store.setStr("player_id", com.korkoor.pardos.domain.social.FriendCode.generate())
        return store.str("player_id")
    }

    private fun myCard() = com.korkoor.pardos.domain.social.FriendCard(
        playerId(), profileName, wallet.avatarId, wallet.bannerId, ret.playerLevel, prestige.score(unlockedLevel),
        totalStars, col.owned.size, store.int("tower_best"), ret.league.ordinal, clock.today
    )

    /** Tu tarjeta de amigo, lista para compartir. */
    fun friendCode(): String = com.korkoor.pardos.domain.social.FriendCards.encode(myCard())

    fun friendInviteText(): String = com.korkoor.pardos.domain.social.FriendCards.inviteText(friendCode())

    /** Agrega (o actualiza) a un amigo a partir de su tarjeta pegada. */
    fun addFriend(text: String): String {
        val card = com.korkoor.pardos.domain.social.FriendCards.decode(text) ?: return no("No encontré una tarjeta de amigo válida")
        if (card.id == playerId()) return no("Esa es tu propia tarjeta")
        val ids = store.strSet("friend_ids")
        val known = card.id in ids
        if (!known && ids.size >= com.korkoor.pardos.domain.social.FriendCards.MAX_FRIENDS) return no("Ya tienes 100 amigos")
        store.addToStrSet("friend_ids", card.id)
        store.setStr("friend_" + card.id, com.korkoor.pardos.domain.social.FriendCards.encode(card))
        afterChange()
        return ok("name" to card.name, "updated" to known)
    }

    fun removeFriend(id: String): String {
        store.setStrSet("friend_ids", store.strSet("friend_ids") - id)
        store.remove("friend_$id")
        return ok()
    }

    /** Lista de amigos ordenada por prestigio, contigo dentro, y los rivales de arriba y de abajo. */
    fun friendsJson(): String {
        val me = myCard()
        val cards = store.strSet("friend_ids").mapNotNull { id ->
            com.korkoor.pardos.domain.social.FriendCards.decode(store.str("friend_$id"))
        }
        val myEntry = com.korkoor.pardos.domain.prestige.RankEntry(me.id, me.name, me.prestige, isMe = true)
        val others = cards.map { com.korkoor.pardos.domain.prestige.RankEntry(it.id, it.name, it.prestige) }
        val (sorted, rivalry) = com.korkoor.pardos.domain.prestige.Rivals.rank(myEntry, others)
        val byId = (cards + me).associateBy { it.id }
        val leagues = com.korkoor.pardos.domain.retention.League.entries
        val rows = sorted.mapIndexedNotNull { index, entry ->
            val c = byId[entry.uid] ?: return@mapIndexedNotNull null
            robj(
                "id" to c.id, "name" to c.name, "avatar" to c.avatar, "banner" to c.banner, "level" to c.level,
                "prestige" to c.prestige, "stars" to c.stars, "pieces" to c.pieces, "tower" to c.tower,
                "league" to leagues[c.league.coerceIn(0, leagues.size - 1)].displayName,
                "daysAgo" to (clock.today - c.day).coerceAtLeast(0), "me" to (c.id == me.id), "position" to index + 1
            )
        }
        return obj(
            "rows" to rows, "count" to cards.size, "max" to com.korkoor.pardos.domain.social.FriendCards.MAX_FRIENDS,
            "position" to rivalry.position,
            "above" to rivalry.above?.let { Raw(obj("name" to it.name, "gap" to rivalry.gapToAbove)) },
            "below" to rivalry.below?.let { Raw(obj("name" to it.name, "gap" to rivalry.gapToBelow)) },
            "code" to friendCode()
        )
    }

    // ------------------------------------------------------------------ Studio (tu propia skin)

    private fun studioConfigFrom(finish: String, hue: Int, hue2: Int, tone: String, background: String, particles: String) =
        com.korkoor.pardos.domain.shop.StudioConfig.decode(listOf(finish, hue.toString(), hue2.toString(), tone, background, particles).joinToString("|"))

    /** Configuración guardada y los puntos de partida. */
    fun studioState(): String {
        val c = com.korkoor.pardos.domain.shop.StudioSkin.config
        fun cfg(c: com.korkoor.pardos.domain.shop.StudioConfig) = arrayOf<Pair<String, Any?>>(
            "finish" to c.finish.name, "hue" to c.hue, "hue2" to c.hue2, "tone" to c.tone.name, "background" to c.background.name, "particles" to c.particles.name
        )
        return obj(
            "owned" to (TileSkin.STUDIO.id in wallet.ownedSkins), "config" to Raw(obj(*cfg(c))),
            "presets" to com.korkoor.pardos.domain.shop.StudioPresets.all.map { Raw(obj("name" to it.name, *cfg(it.config))) },
            "finishes" to com.korkoor.pardos.domain.shop.TileFinish.entries.map { it.name },
            "particleKinds" to com.korkoor.pardos.domain.shop.ParticleKind.entries.map { it.name }
        )
    }

    /** Vista previa de una configuración (no se guarda): devuelve la skin como en el catálogo. */
    fun studioPreview(finish: String, hue: Int, hue2: Int, tone: String, background: String, particles: String): String {
        val c = studioConfigFrom(finish, hue, hue2, tone, background, particles)
        return MetaCatalogs.skinObject(TileSkin.STUDIO, c.toStyle(), "studio_preview").json
    }

    /** Guarda la configuración de Studio (hace falta tener la skin). */
    fun saveStudio(finish: String, hue: Int, hue2: Int, tone: String, background: String, particles: String): String {
        if (TileSkin.STUDIO.id !in wallet.ownedSkins) return no("Primero consigue Studio")
        val c = studioConfigFrom(finish, hue, hue2, tone, background, particles)
        com.korkoor.pardos.domain.shop.StudioSkin.config = c
        store.setStr("studio_cfg", c.encode())
        return ok()
    }

    /** Copia de seguridad en texto (para compartirla o guardarla en Notas). */
    fun exportBackup(): String = MetaBackup.encode(store.export())

    /** Restaura una copia pegada (se busca dentro del mensaje). Reemplaza el progreso actual. */
    fun importBackup(text: String): String {
        val state = MetaBackup.decode(text) ?: return no("No encontré una copia válida en ese texto")
        if (!state.contains("\t")) return no("La copia está vacía")
        load(state)
        return ok("coins" to wallet.coins, "unlocked" to unlockedLevel)
    }

    fun resetAll() { store.clear() }
}
