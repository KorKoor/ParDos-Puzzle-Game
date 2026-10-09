package com.korkoor.pardos.domain.meta

import com.korkoor.pardos.domain.achievements.AchContext
import com.korkoor.pardos.domain.achievements.AchievementCatalog
import com.korkoor.pardos.domain.collection.ChestType
import com.korkoor.pardos.domain.collection.CollectibleCatalog
import com.korkoor.pardos.domain.collection.Rarity
import com.korkoor.pardos.domain.collection.Series
import com.korkoor.pardos.domain.collection.TokenRules
import com.korkoor.pardos.domain.economy.Economy
import com.korkoor.pardos.domain.model.GameMode
import com.korkoor.pardos.domain.model.TileModel
import com.korkoor.pardos.domain.prestige.Milestone
import com.korkoor.pardos.domain.prestige.Platinum
import com.korkoor.pardos.domain.prestige.PrestigeMilestones
import com.korkoor.pardos.domain.prestige.PrestigeRank
import com.korkoor.pardos.domain.prestige.PrestigeScore
import com.korkoor.pardos.domain.prestige.PrestigeStats
import com.korkoor.pardos.domain.prestige.ProfileTitle
import com.korkoor.pardos.domain.prestige.ProfileTitles
import com.korkoor.pardos.domain.prestige.TitleUnlock
import com.korkoor.pardos.domain.prestige.TrophyTier
import com.korkoor.pardos.domain.prestige.trophyTierOf
import com.korkoor.pardos.domain.shop.Avatars
import com.korkoor.pardos.domain.shop.Banners

/** Logros (82, los mismos que en Android): se miran con el estado del tablero y pagan según su rareza. */
internal class Achievements(private val s: MetaStore, private val wallet: Wallet, private val col: CollectionOps) {

    val unlocked: Set<String> get() = s.strSet("ach_unlocked")

    fun rarityOf(id: String): Rarity = AchievementCatalog.rarity[id] ?: Rarity.COMMON

    class Unlock(val id: String, val title: String, val description: String, val rarity: Rarity, val coins: Int, val gems: Int, val chest: ChestType?)

    /** Mira todos los logros pendientes con el estado actual y entrega el premio de los nuevos. */
    fun check(ctx: AchContext): List<Unlock> {
        val done = unlocked
        val out = mutableListOf<Unlock>()
        for (def in AchievementCatalog.all) {
            if (def.id in done) continue
            if (!def.condition(ctx)) continue
            out += pay(def.id, def.title, def.description)
        }
        return out
    }

    private fun pay(id: String, title: String, description: String): Unlock {
        s.addToStrSet("ach_unlocked", id)
        val rarity = rarityOf(id)
        val reward = Economy.achievementReward(rarity)
        wallet.addCoins(reward.coins)
        wallet.addGems(reward.gems)
        val chest = when (rarity) {
            Rarity.EPIC -> ChestType.COMMON
            Rarity.LEGENDARY -> ChestType.RARE
            else -> null
        }
        chest?.let { col.addChests(it, 1) }
        return Unlock(id, title, description, rarity, reward.coins, reward.gems, chest)
    }

    fun listJson(): List<Raw> = AchievementCatalog.all.map { d ->
        val rarity = rarityOf(d.id)
        val reward = Economy.achievementReward(rarity)
        robj(
            "id" to d.id, "title" to d.title, "desc" to d.description, "color" to rgb(d.color), "rarity" to rarity.name,
            "tier" to trophyTierOf(rarity).name, "unlocked" to (d.id in unlocked), "coins" to reward.coins, "gems" to reward.gems,
            "category" to AchievementCatalog.categoryOf(d.id)
        )
    }
}

/** Prestigio: una sola cifra que junta logros, colección, campaña y torre; rangos, hitos, títulos y el Platino. */
internal class Prestige(
    private val s: MetaStore, private val wallet: Wallet, private val col: CollectionOps,
    private val ach: Achievements, private val ret: Retention
) {
    val doneMilestones: Set<String> get() = s.strSet("pm_done")
    val platinum: Boolean get() = s.bool("pm_platinum")
    val boughtTitles: Set<String> get() = s.strSet("pm_titles_bought")
    val equippedTitleId: String get() = s.str("pm_title_eq", ProfileTitles.DEFAULT_ID)

    fun onLevelWon(kind: String, boss: Boolean, flowPeak: Boolean, winStreak: Int) {
        s.addToStrSet("pm_kinds", kind)
        if (boss) s.addInt("pm_bosses", 1)
        if (flowPeak) s.addInt("pm_flow", 1)
        if (winStreak > s.int("pm_best_win_streak")) s.setInt("pm_best_win_streak", winStreak)
    }

    fun onDailyDone() { s.addInt("pm_daily", 1) }

    fun stats(unlockedLevel: Int): PrestigeStats {
        val byTier = HashMap<TrophyTier, Int>()
        ach.unlocked.forEach { id -> byTier[trophyTierOf(ach.rarityOf(id))] = (byTier[trophyTierOf(ach.rarityOf(id))] ?: 0) + 1 }
        val owned = col.owned.filter { CollectibleCatalog.byId(it) != null }.toSet()
        val byRarity = HashMap<Rarity, Int>()
        owned.forEach { id -> CollectibleCatalog.byId(id)?.let { byRarity[it.rarity] = (byRarity[it.rarity] ?: 0) + 1 } }
        var stars = 0
        var best = 0
        for (lv in 1..unlockedLevel) {
            val st = s.int("stars_$lv")
            if (st > 0) { stars += st; best = lv }
        }
        return PrestigeStats(
            trophiesByTier = byTier, trophiesTotal = AchievementCatalog.all.size, milestonesDone = doneMilestones.size,
            piecesByRarity = byRarity, foil = col.foil.count { CollectibleCatalog.byId(it) != null },
            seriesComplete = Series.entries.count { CollectibleCatalog.isSeriesComplete(it, owned) },
            albumComplete = CollectibleCatalog.isAlbumComplete(owned), campaignLevel = best, campaignStars = stars,
            towerBest = s.int("tower_best"), playerLevel = ret.playerLevel, bestWinStreak = s.int("pm_best_win_streak"),
            bestDayStreak = maxOf(ret.bestStreak, ret.streak), bossesDefeated = s.int("pm_bosses"),
            kindsWon = s.strSet("pm_kinds").size, dailyDone = s.int("pm_daily"), friends = 0, flowPeaks = s.int("pm_flow"),
            platinum = platinum
        )
    }

    fun score(unlockedLevel: Int): Int = PrestigeScore.score(stats(unlockedLevel))
    fun rank(unlockedLevel: Int): PrestigeRank = PrestigeRank.forScore(score(unlockedLevel))

    fun isOwned(t: ProfileTitle, stats: PrestigeStats): Boolean =
        t.id in boughtTitles || ProfileTitles.isEarned(t, PrestigeRank.forScore(PrestigeScore.score(stats)), doneMilestones, stats.platinum)

    fun equip(id: String, unlockedLevel: Int): Boolean {
        val t = ProfileTitles.all.firstOrNull { it.id == id } ?: return false
        if (!isOwned(t, stats(unlockedLevel))) return false
        s.setStr("pm_title_eq", t.id)
        return true
    }

    fun buy(id: String): Boolean {
        val t = ProfileTitles.all.firstOrNull { it.id == id } ?: return false
        val u = t.unlock as? TitleUnlock.Gems ?: return false
        if (t.id in boughtTitles || !wallet.spendGems(u.price)) return false
        s.addToStrSet("pm_titles_bought", t.id)
        s.setStr("pm_title_eq", t.id)
        return true
    }

    private fun event(vararg pairs: Pair<String, Any?>) { s.setStr("pm_events", s.str("pm_events") + obj(*pairs) + "\n") }

    /** Devuelve los avisos pendientes (hitos, rangos, títulos, Platino) y los borra. */
    fun takeEvents(): List<Raw> {
        val raw = s.str("pm_events")
        if (raw.isEmpty()) return emptyList()
        s.remove("pm_events")
        return raw.split('\n').filter { it.isNotBlank() }.map { Raw(it) }
    }

    /** Revisa hitos nuevos, el Platino, subidas de rango y títulos; paga los premios (una sola vez) y deja los avisos. */
    fun refresh(unlockedLevel: Int) {
        var stats = stats(unlockedLevel)
        val firstRun = !s.has("pm_done")
        val done = doneMilestones.toMutableSet()
        val fresh = mutableListOf<Milestone>()
        for (m in PrestigeMilestones.all) {
            if (m.id !in done && m.isDone(stats)) {
                done += m.id
                wallet.addCoins(m.coins)
                if (m.gems > 0) wallet.addGems(m.gems)
                m.chest?.let { col.addChests(it, 1) }
                fresh += m
            }
        }
        if (firstRun && fresh.isNotEmpty()) {
            event("type" to "backfill", "count" to fresh.size, "coins" to fresh.sumOf { it.coins }, "gems" to fresh.sumOf { it.gems })
        } else {
            fresh.forEach { event("type" to "milestone", "title" to it.title, "desc" to it.description, "coins" to it.coins, "gems" to it.gems, "chest" to it.chest?.name) }
        }
        if (firstRun || done.size != doneMilestones.size) s.setStrSet("pm_done", done)
        stats = stats.copy(milestonesDone = done.size)

        if (!platinum && Platinum.isEarned(stats.trophiesUnlocked, stats.trophiesTotal, done.size, PrestigeMilestones.all.size)) {
            s.setBool("pm_platinum", true)
            wallet.addCoins(Platinum.REWARD_COINS)
            wallet.addGems(Platinum.REWARD_GEMS)
            col.addChests(ChestType.EPIC, 2)
            col.addTokens(TokenRules.PLATINUM)
            event("type" to "platinum", "coins" to Platinum.REWARD_COINS, "gems" to Platinum.REWARD_GEMS)
            stats = stats.copy(platinum = true)
        }

        val score = PrestigeScore.score(stats)
        val rank = PrestigeRank.forScore(score)
        val seen = s.int("pm_rank_seen", -1)
        if (seen < 0) {
            s.setInt("pm_rank_seen", rank.ordinal)
        } else if (rank.ordinal > seen) {
            for (r in PrestigeRank.entries.filter { it.ordinal in (seen + 1)..rank.ordinal }) {
                wallet.addCoins(r.rewardCoins)
                wallet.addGems(r.rewardGems)
                r.rewardChest?.let { col.addChests(it, 1) }
                col.addTokens(TokenRules.RANK_UP)
                event("type" to "rank", "title" to r.title, "coins" to r.rewardCoins, "gems" to r.rewardGems, "chest" to r.rewardChest?.name)
            }
            s.setInt("pm_rank_seen", rank.ordinal)
        }

        Avatars.prestigeUnlocked(rank, stats.platinum).forEach { wallet.grantAvatar(it.id) }
        Banners.prestigeUnlocked(rank, stats.platinum).forEach { wallet.grantBanner(it.id) }

        val earnedNow = ProfileTitles.all.filter { ProfileTitles.isEarned(it, rank, done, stats.platinum) }.map { it.id }.toSet()
        if (!s.has("pm_titles_seen")) {
            s.setStrSet("pm_titles_seen", earnedNow)
        } else {
            val fresher = earnedNow - s.strSet("pm_titles_seen")
            if (fresher.isNotEmpty()) {
                fresher.mapNotNull { id -> ProfileTitles.all.firstOrNull { it.id == id } }.forEach { event("type" to "title", "title" to it.name) }
                s.setStrSet("pm_titles_seen", earnedNow)
            }
        }
    }

    fun stateJson(unlockedLevel: Int): String {
        val stats = stats(unlockedLevel)
        val score = PrestigeScore.score(stats)
        val rank = PrestigeRank.forScore(score)
        val done = doneMilestones
        val milestones = PrestigeMilestones.all.map { m ->
            robj(
                "id" to m.id, "title" to m.title, "desc" to m.description, "group" to m.group.label, "target" to m.target,
                "progress" to m.progress(stats), "done" to (m.id in done), "coins" to m.coins, "gems" to m.gems, "chest" to m.chest?.name
            )
        }
        val titles = ProfileTitles.all.map { t ->
            val price = (t.unlock as? TitleUnlock.Gems)?.price ?: 0
            robj(
                "id" to t.id, "name" to t.name, "rarity" to t.rarity.name, "how" to ProfileTitles.howTo(t),
                "owned" to isOwned(t, stats), "equipped" to (t.id == equippedTitleId), "price" to price
            )
        }
        val platProgress = Platinum.progress(stats.trophiesUnlocked, stats.trophiesTotal, done.size, PrestigeMilestones.all.size)
        val tiers = TrophyTier.entries.map { robj("tier" to it.name, "label" to it.label, "count" to (stats.trophiesByTier[it] ?: 0)) }
        return obj(
            "score" to score, "rank" to rank.title, "rankIndex" to rank.ordinal, "nextRank" to rank.next?.title,
            "pointsToNext" to PrestigeRank.pointsToNext(score), "progress" to PrestigeRank.progress(score).toDouble(),
            "ranks" to PrestigeRank.entries.map { robj("title" to it.title, "min" to it.minScore, "coins" to it.rewardCoins, "gems" to it.rewardGems, "chest" to it.rewardChest?.name) },
            "trophies" to stats.trophiesUnlocked, "trophiesTotal" to stats.trophiesTotal, "tiers" to tiers,
            "milestonesDone" to done.size, "milestonesTotal" to PrestigeMilestones.all.size,
            "platinum" to platinum, "platDone" to platProgress.first, "platTotal" to platProgress.second,
            "platCoins" to Platinum.REWARD_COINS, "platGems" to Platinum.REWARD_GEMS,
            "milestones" to milestones, "titles" to titles, "equippedTitle" to ProfileTitles.byId(equippedTitleId).name
        )
    }
}

/** Convierte lo que manda Swift (fichas como "valor:fila:columna;...") en el contexto de logros. */
internal fun parseAchContext(
    completed: Boolean, level: Int, moves: Int, elapsedMs: Long, score: Int, combo: Int, empty: Int, hasMoves: Boolean, size: Int,
    mode: String, tiles: String
): AchContext {
    val list = tiles.split(';').filter { it.isNotBlank() }.mapNotNull { part ->
        val p = part.split(':')
        if (p.size != 3) return@mapNotNull null
        val v = p[0].toIntOrNull() ?: return@mapNotNull null
        val r = p[1].toIntOrNull() ?: return@mapNotNull null
        val c = p[2].toIntOrNull() ?: return@mapNotNull null
        if (v <= 0 || r < 0 || c < 0) null else TileModel(id = "$r-$c", value = v, row = r, col = c)
    }
    val gm = GameMode.entries.firstOrNull { it.name == mode } ?: GameMode.CLASICO
    return AchContext(completed, level, moves, elapsedMs, score, combo, empty, hasMoves, size, gm, list)
}
