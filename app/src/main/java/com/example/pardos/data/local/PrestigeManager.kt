package com.korkoor.pardos.data.local

import android.content.Context
import android.content.SharedPreferences
import com.korkoor.pardos.domain.achievements.gameAchievements
import com.korkoor.pardos.domain.collection.CollectibleCatalog
import com.korkoor.pardos.domain.collection.Rarity
import com.korkoor.pardos.domain.collection.Series
import com.korkoor.pardos.domain.flow.FlowTier
import com.korkoor.pardos.domain.level.LevelCatalog
import com.korkoor.pardos.domain.level.LevelKind
import com.korkoor.pardos.domain.prestige.Metric
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Lo que pasa en el prestigio y merece una celebración en pantalla. */
sealed interface PrestigeEvent {
    data class MilestoneDone(val milestone: Milestone) : PrestigeEvent
    data class RankUp(val rank: PrestigeRank) : PrestigeEvent
    data class TitleUnlocked(val title: ProfileTitle) : PrestigeEvent
    data object PlatinumEarned : PrestigeEvent

    /** Primera vez con el sistema: se registran de golpe los hitos que ya tenías (sin una lluvia de letreros). */
    data class Backfill(val count: Int, val coins: Int, val gems: Int) : PrestigeEvent
}

/** Canal de avisos de prestigio: lo escucha la pantalla principal para mostrar los letreros. */
object PrestigeBus {
    private val _events = MutableSharedFlow<PrestigeEvent>(extraBufferCapacity = 32)
    val events: SharedFlow<PrestigeEvent> = _events.asSharedFlow()
    fun emit(e: PrestigeEvent) { _events.tryEmit(e) }
}

/**
 * Prestigio del jugador: junta logros, colección, campaña y torre en una sola puntuación, reparte los premios de los
 * hitos y de los rangos, guarda el título que luces y el Platino. Las reglas están en `domain/prestige`.
 */
class PrestigeManager(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = appContext.getSharedPreferences("pardos_prestige", Context.MODE_PRIVATE)
    private val storage: SharedPreferences = appContext.getSharedPreferences("pardos_storage", Context.MODE_PRIVATE)
    private val economy = EconomyManager(appContext)
    private val collection = CollectionManager(appContext)
    private val rewards = RewardsManager(appContext)

    // ---------------------------------------------------------------- lectura

    val doneMilestones: Set<String> get() = prefs.getStringSet(K_DONE, emptySet()) ?: emptySet()
    val boughtTitles: Set<String> get() = prefs.getStringSet(K_BOUGHT, emptySet()) ?: emptySet()
    val isPlatinum: Boolean get() = prefs.getBoolean(K_PLATINUM, false)

    /** Logros desbloqueados (los marca el motor del juego con `ach_<id>`). */
    fun unlockedAchievements(): Set<String> = gameAchievements.all.map { it.id }.filter { storage.getBoolean("ach_$it", false) }.toSet()

    /** Estadísticas de ahora mismo (se lee todo de las preferencias; es barato). */
    fun stats(): PrestigeStats {
        val unlocked = unlockedAchievements()
        val byTier = HashMap<TrophyTier, Int>()
        unlocked.forEach { id -> byTier.merge(trophyTierOf(rewards.rarityOf(id)), 1, Int::plus) }

        val owned = collection.owned.value.filter { CollectibleCatalog.byId(it) != null }.toSet()
        val byRarity = HashMap<Rarity, Int>()
        owned.forEach { id -> CollectibleCatalog.byId(id)?.let { byRarity.merge(it.rarity, 1, Int::plus) } }
        val seriesDone = Series.entries.count { CollectibleCatalog.isSeriesComplete(it, owned) }

        var stars = 0
        var best = 0
        for (lv in 1..LevelCatalog.TOTAL_LEVELS) {
            val s = storage.getInt("stars_level_$lv", 0)
            if (s > 0) { stars += s; best = lv }
        }
        val profile = ProfileManager(appContext).getProfile()
        return PrestigeStats(
            trophiesByTier = byTier,
            trophiesTotal = gameAchievements.all.size,
            milestonesDone = doneMilestones.size,
            piecesByRarity = byRarity,
            foil = collection.foil.value.count { CollectibleCatalog.byId(it) != null },
            seriesComplete = seriesDone,
            albumComplete = CollectibleCatalog.isAlbumComplete(owned),
            campaignLevel = best,
            campaignStars = stars,
            towerBest = storage.getInt("tower_best_floor", 0),
            playerLevel = profile.playerLevel,
            bestWinStreak = prefs.getInt(K_BEST_WIN_STREAK, 0),
            bestDayStreak = maxOf(profile.bestStreak, profile.currentStreak),
            bossesDefeated = prefs.getInt(K_BOSSES, 0),
            kindsWon = (prefs.getStringSet(K_KINDS, emptySet()) ?: emptySet()).size,
            dailyDone = prefs.getInt(K_DAILY, 0),
            friends = profile.friendsUids.size,
            flowPeaks = prefs.getInt(K_FLOW, 0),
            platinum = isPlatinum
        )
    }

    fun score(): Int = PrestigeScore.score(stats())
    fun rank(): PrestigeRank = PrestigeRank.forScore(score())

    // ---------------------------------------------------------------- títulos

    fun equippedTitle(): ProfileTitle = ProfileTitles.byId(prefs.getString(K_EQUIPPED, ProfileTitles.DEFAULT_ID))

    /** ¿Puedes lucir este título ahora mismo? (ganado o comprado) */
    fun isOwned(t: ProfileTitle, stats: PrestigeStats = stats()): Boolean =
        t.id in boughtTitles || ProfileTitles.isEarned(t, PrestigeRank.forScore(PrestigeScore.score(stats)), doneMilestones, stats.platinum)

    fun equip(t: ProfileTitle): Boolean {
        if (!isOwned(t)) return false
        prefs.edit().putString(K_EQUIPPED, t.id).apply()
        ProfileManager(appContext).requestSync()
        return true
    }

    fun buy(t: ProfileTitle): Boolean {
        val u = t.unlock as? TitleUnlock.Gems ?: return false
        if (t.id in boughtTitles || !economy.spendGems(u.price)) return false
        prefs.edit().putStringSet(K_BOUGHT, boughtTitles + t.id).putString(K_EQUIPPED, t.id).apply()
        PrestigeBus.emit(PrestigeEvent.TitleUnlocked(t))
        ProfileManager(appContext).requestSync()
        return true
    }

    // ---------------------------------------------------------------- contadores (los llama el juego)

    /** Un nivel ganado (de campaña o de la torre). */
    fun onLevelWon(kind: LevelKind, boss: Boolean, peakFlow: FlowTier, winStreak: Int) {
        val e = prefs.edit()
        e.putStringSet(K_KINDS, (prefs.getStringSet(K_KINDS, emptySet()) ?: emptySet()) + kind.name)
        if (boss) e.putInt(K_BOSSES, prefs.getInt(K_BOSSES, 0) + 1)
        if (peakFlow == FlowTier.FLOW) e.putInt(K_FLOW, prefs.getInt(K_FLOW, 0) + 1)
        if (winStreak > prefs.getInt(K_BEST_WIN_STREAK, 0)) e.putInt(K_BEST_WIN_STREAK, winStreak)
        e.apply()
        refresh()
    }

    fun onDailyDone() {
        prefs.edit().putInt(K_DAILY, prefs.getInt(K_DAILY, 0) + 1).apply()
        refresh()
    }

    // ---------------------------------------------------------------- revisión: hitos, rangos, platino y títulos

    /**
     * Mira si hay hitos nuevos, subidas de rango, el Platino o títulos desbloqueados; paga los premios (una sola vez) y avisa.
     * Devuelve lo ocurrido (también se emite por [PrestigeBus]).
     */
    fun refresh(): List<PrestigeEvent> {
        val events = mutableListOf<PrestigeEvent>()
        var stats = stats()

        // 1. Hitos (la primera vez se pagan los ya logrados y se avisa con un solo letrero)
        val firstRun = !prefs.contains(K_DONE)
        val done = doneMilestones.toMutableSet()
        val newOnes = mutableListOf<Milestone>()
        for (m in PrestigeMilestones.all) {
            if (m.id !in done && m.isDone(stats)) {
                done += m.id
                payMilestone(m)
                newOnes += m
            }
        }
        if (firstRun && newOnes.isNotEmpty()) {
            events += PrestigeEvent.Backfill(newOnes.size, newOnes.sumOf { it.coins }, newOnes.sumOf { it.gems })
        } else {
            newOnes.forEach { events += PrestigeEvent.MilestoneDone(it) }
        }
        if (firstRun) prefs.edit().putStringSet(K_DONE, done).apply()
        if (done.size != doneMilestones.size) prefs.edit().putStringSet(K_DONE, done).apply()
        stats = stats.copy(milestonesDone = done.size)

        // 2. Platino: todos los logros y todos los hitos
        if (!isPlatinum && Platinum.isEarned(stats.trophiesUnlocked, stats.trophiesTotal, done.size, PrestigeMilestones.all.size)) {
            prefs.edit().putBoolean(K_PLATINUM, true).apply()
            economy.addCoins(Platinum.REWARD_COINS)
            economy.addGems(Platinum.REWARD_GEMS)
            collection.addChests(com.korkoor.pardos.domain.collection.ChestType.EPIC, 2)
            events += PrestigeEvent.PlatinumEarned
            stats = stats.copy(platinum = true)
        }

        // 3. Rango (con la puntuación ya completa)
        val score = PrestigeScore.score(stats)
        val rank = PrestigeRank.forScore(score)
        val seen = prefs.getInt(K_RANK_SEEN, -1)
        if (seen < 0) {
            // primera vez con el sistema: se anota el rango de partida sin regalar los anteriores
            prefs.edit().putInt(K_RANK_SEEN, rank.ordinal).apply()
        } else if (rank.ordinal > seen) {
            for (r in PrestigeRank.entries.filter { it.ordinal in (seen + 1)..rank.ordinal }) {
                economy.addCoins(r.rewardCoins)
                economy.addGems(r.rewardGems)
                r.rewardChest?.let { collection.addChests(it, 1) }
                events += PrestigeEvent.RankUp(r)
            }
            prefs.edit().putInt(K_RANK_SEEN, rank.ordinal).apply()
        }

        // 4. Títulos nuevos
        val titlesSeen = (prefs.getStringSet(K_TITLES_SEEN, null))
        val earnedNow = ProfileTitles.all.filter { ProfileTitles.isEarned(it, rank, done, stats.platinum) }.map { it.id }.toSet()
        if (titlesSeen == null) {
            prefs.edit().putStringSet(K_TITLES_SEEN, earnedNow).apply()
        } else {
            val fresh = earnedNow - titlesSeen
            if (fresh.isNotEmpty()) {
                fresh.mapNotNull { id -> ProfileTitles.all.firstOrNull { it.id == id } }.forEach { events += PrestigeEvent.TitleUnlocked(it) }
                prefs.edit().putStringSet(K_TITLES_SEEN, earnedNow).apply()
            }
        }

        // Para el perfil que ven los amigos
        val changed = score != prefs.getInt(K_SCORE_CACHE, -1) || stats.platinum != prefs.getBoolean(K_PLAT_CACHE, false)
        prefs.edit()
            .putInt(K_SCORE_CACHE, score).putBoolean(K_PLAT_CACHE, stats.platinum)
            .putInt(K_PIECES_CACHE, stats.pieces).putInt(K_TOWER_CACHE, stats.towerBest)
            .apply()
        if (changed) ProfileManager(appContext).requestSync()

        events.forEach(PrestigeBus::emit)
        return events
    }

    private fun payMilestone(m: Milestone) {
        economy.addCoins(m.coins)
        if (m.gems > 0) economy.addGems(m.gems)
        m.chest?.let { collection.addChests(it, 1) }
    }

    companion object {
        private const val K_DONE = "milestones_done"
        private const val K_BOUGHT = "titles_bought"
        private const val K_EQUIPPED = "title_equipped"
        private const val K_PLATINUM = "platinum"
        private const val K_RANK_SEEN = "rank_seen"
        private const val K_TITLES_SEEN = "titles_seen"
        private const val K_KINDS = "kinds_won"
        private const val K_BOSSES = "bosses"
        private const val K_FLOW = "flow_peaks"
        private const val K_DAILY = "daily_done"
        private const val K_BEST_WIN_STREAK = "best_win_streak"
        const val K_SCORE_CACHE = "score_cache"
        const val K_PLAT_CACHE = "platinum_cache"
        const val K_PIECES_CACHE = "pieces_cache"
        const val K_TOWER_CACHE = "tower_cache"
        const val K_EQUIPPED_PUBLIC = "title_equipped"

        /** Valores guardados que el perfil público lee sin recalcular nada. */
        fun cachedScore(context: Context): Int = context.applicationContext.getSharedPreferences("pardos_prestige", Context.MODE_PRIVATE).getInt(K_SCORE_CACHE, 0)
        fun cachedPlatinum(context: Context): Boolean = context.applicationContext.getSharedPreferences("pardos_prestige", Context.MODE_PRIVATE).getBoolean(K_PLAT_CACHE, false)
        fun cachedTitleId(context: Context): String = context.applicationContext.getSharedPreferences("pardos_prestige", Context.MODE_PRIVATE).getString(K_EQUIPPED, ProfileTitles.DEFAULT_ID) ?: ProfileTitles.DEFAULT_ID
        fun cachedPieces(context: Context): Int = context.applicationContext.getSharedPreferences("pardos_prestige", Context.MODE_PRIVATE).getInt(K_PIECES_CACHE, 0)
        fun cachedTower(context: Context): Int = context.applicationContext.getSharedPreferences("pardos_prestige", Context.MODE_PRIVATE).getInt(K_TOWER_CACHE, 0)
    }
}
