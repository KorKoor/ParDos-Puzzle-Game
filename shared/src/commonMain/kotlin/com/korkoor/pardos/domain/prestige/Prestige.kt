package com.korkoor.pardos.domain.prestige

import com.korkoor.pardos.domain.collection.ChestType
import com.korkoor.pardos.domain.collection.Rarity

/**
 * Prestigio: lo que te hace sentir (y que vean) que avanzas. Todo es lógica pura, probada en `PrestigeTest`:
 *  - [TrophyTier]: los 82 logros del juego se muestran como trofeos de bronce, plata, oro y diamante según su rareza.
 *  - [PrestigeStats] → [PrestigeScore]: una sola cifra de **puntos de prestigio** que suma trofeos, colección, campaña,
 *    torre, nivel y hitos. Con ella se sube de [PrestigeRank] (con premio) y se ordena el ranking de amigos.
 *  - [PrestigeMilestones]: ~75 hitos a largo plazo (piezas, series, brillantes, niveles, estrellas, torre, jefes, rachas…).
 *  - [ProfileTitles]: títulos que se ganan (por hitos, rangos o el Platino) o se compran, y que se lucen bajo el nombre.
 *  - [Platinum]: el trofeo máximo por tener TODOS los logros y TODOS los hitos.
 * Nada aquí castiga ni presiona: solo enseña lo cerca que estás de lo siguiente y deja presumir lo ya ganado.
 */

// ============================== TROFEOS ==============================

enum class TrophyTier(val label: String, val points: Int) {
    BRONZE("Bronce", 1),
    SILVER("Plata", 3),
    GOLD("Oro", 7),
    DIAMOND("Diamante", 20)
}

fun trophyTierOf(rarity: Rarity): TrophyTier = when (rarity) {
    Rarity.COMMON -> TrophyTier.BRONZE
    Rarity.RARE -> TrophyTier.SILVER
    Rarity.EPIC -> TrophyTier.GOLD
    Rarity.LEGENDARY -> TrophyTier.DIAMOND
}

// ============================== ESTADÍSTICAS ==============================

data class PrestigeStats(
    val trophiesByTier: Map<TrophyTier, Int> = emptyMap(),
    /** Cuántos logros existen en total (los que se pueden lograr). */
    val trophiesTotal: Int = 0,
    val milestonesDone: Int = 0,
    val piecesByRarity: Map<Rarity, Int> = emptyMap(),
    val foil: Int = 0,
    val seriesComplete: Int = 0,
    val albumComplete: Boolean = false,
    /** Nivel de campaña más alto superado. */
    val campaignLevel: Int = 0,
    val campaignStars: Int = 0,
    val towerBest: Int = 0,
    val playerLevel: Int = 1,
    /** Niveles ganados seguidos (mejor racha) y días seguidos jugando (mejor racha). */
    val bestWinStreak: Int = 0,
    val bestDayStreak: Int = 0,
    val bossesDefeated: Int = 0,
    /** Cuántos tipos de nivel distintos has ganado. */
    val kindsWon: Int = 0,
    val dailyDone: Int = 0,
    val friends: Int = 0,
    /** Niveles en los que llegaste a "¡FLOW!". */
    val flowPeaks: Int = 0,
    val platinum: Boolean = false
) {
    val trophiesUnlocked: Int get() = trophiesByTier.values.sum()
    val pieces: Int get() = piecesByRarity.values.sum()
    val epicPlus: Int get() = (piecesByRarity[Rarity.EPIC] ?: 0) + (piecesByRarity[Rarity.LEGENDARY] ?: 0)

    fun value(m: Metric): Int = when (m) {
        Metric.PIECES -> pieces
        Metric.EPIC_PLUS -> epicPlus
        Metric.FOIL -> foil
        Metric.SERIES -> seriesComplete
        Metric.ALBUM -> if (albumComplete) 1 else 0
        Metric.CAMPAIGN_LEVEL -> campaignLevel
        Metric.CAMPAIGN_STARS -> campaignStars
        Metric.TOWER -> towerBest
        Metric.WIN_STREAK -> bestWinStreak
        Metric.DAY_STREAK -> bestDayStreak
        Metric.BOSSES -> bossesDefeated
        Metric.KINDS -> kindsWon
        Metric.DAILY -> dailyDone
        Metric.FRIENDS -> friends
        Metric.FLOW -> flowPeaks
        Metric.TROPHIES -> trophiesUnlocked
    }
}

enum class Metric { PIECES, EPIC_PLUS, FOIL, SERIES, ALBUM, CAMPAIGN_LEVEL, CAMPAIGN_STARS, TOWER, WIN_STREAK, DAY_STREAK, BOSSES, KINDS, DAILY, FRIENDS, FLOW, TROPHIES }

// ============================== PUNTOS ==============================

object PrestigeScore {
    private val PIECE_POINTS = mapOf(Rarity.COMMON to 2, Rarity.RARE to 5, Rarity.EPIC to 12, Rarity.LEGENDARY to 30)
    const val FOIL_POINTS = 8
    const val SERIES_POINTS = 60
    const val ALBUM_POINTS = 500
    const val PLATINUM_POINTS = 1000
    const val MILESTONE_POINTS = 15

    fun score(s: PrestigeStats): Int {
        val trophies = s.trophiesByTier.entries.sumOf { (tier, n) -> tier.points * n }
        val pieces = s.piecesByRarity.entries.sumOf { (r, n) -> (PIECE_POINTS[r] ?: 0) * n }
        return trophies + pieces +
            s.foil * FOIL_POINTS + s.seriesComplete * SERIES_POINTS + (if (s.albumComplete) ALBUM_POINTS else 0) +
            s.campaignStars / 2 + s.towerBest * 10 + s.playerLevel * 5 + s.milestonesDone * MILESTONE_POINTS +
            (if (s.platinum) PLATINUM_POINTS else 0)
    }
}

// ============================== RANGOS ==============================

enum class PrestigeRank(
    val title: String,
    val minScore: Int,
    val rewardCoins: Int,
    val rewardGems: Int,
    val rewardChest: ChestType?
) {
    NOVICE("Novato", 0, 0, 0, null),
    APPRENTICE("Aprendiz", 150, 150, 3, null),
    ADEPT("Adepto", 500, 300, 6, ChestType.COMMON),
    EXPERT("Experto", 1_200, 600, 10, ChestType.RARE),
    MASTER("Maestro", 2_500, 1_000, 20, ChestType.RARE),
    GRANDMASTER("Gran Maestro", 4_500, 2_000, 40, ChestType.EPIC),
    LEGEND("Leyenda", 6_500, 4_000, 80, ChestType.EPIC),
    MYTHIC("Mítico", 9_500, 8_000, 150, ChestType.EPIC);

    val next: PrestigeRank? get() = entries.getOrNull(ordinal + 1)

    companion object {
        fun forScore(score: Int): PrestigeRank = entries.last { score >= it.minScore }

        /** Puntos que faltan para el siguiente rango (0 si ya eres Mítico). */
        fun pointsToNext(score: Int): Int = forScore(score).next?.let { (it.minScore - score).coerceAtLeast(0) } ?: 0

        /** Avance de 0 a 1 dentro del rango actual hacia el siguiente. */
        fun progress(score: Int): Float {
            val cur = forScore(score)
            val next = cur.next ?: return 1f
            return ((score - cur.minScore).toFloat() / (next.minScore - cur.minScore)).coerceIn(0f, 1f)
        }
    }
}

// ============================== HITOS ==============================

enum class MilestoneGroup(val label: String) {
    COLLECTION("Colección"), CAMPAIGN("Campaña"), TOWER("Torre"), MASTERY("Maestría"), SOCIAL("Social"), TROPHIES("Trofeos")
}

data class Milestone(
    val id: String,
    val metric: Metric,
    val target: Int,
    val group: MilestoneGroup,
    val title: String,
    val description: String,
    val coins: Int,
    val gems: Int,
    val chest: ChestType? = null
) {
    fun progress(s: PrestigeStats): Int = s.value(metric).coerceAtMost(target)
    fun isDone(s: PrestigeStats): Boolean = s.value(metric) >= target
    fun fraction(s: PrestigeStats): Float = (s.value(metric).toFloat() / target).coerceIn(0f, 1f)
}

object PrestigeMilestones {
    private fun tier(i: Int, n: Int): Triple<Int, Int, ChestType?> {
        // Premio que crece con el hito dentro de su serie: monedas, gemas desde el 2º y cofre en el último tercio
        val coins = 60 + 90 * i
        val gems = if (i >= 1) i * 2 else 0
        val chest = when {
            i == n - 1 && n >= 3 -> ChestType.RARE
            i >= (n * 2) / 3 && n >= 4 -> ChestType.COMMON
            else -> null
        }
        return Triple(coins, gems, chest)
    }

    private fun series(
        metric: Metric, group: MilestoneGroup, prefix: String, targets: List<Int>,
        title: (Int) -> String, description: (Int) -> String
    ): List<Milestone> = targets.mapIndexed { i, t ->
        val (coins, gems, chest) = tier(i, targets.size)
        Milestone("${prefix}_$t", metric, t, group, title(t), description(t), coins, gems, chest)
    }

    val all: List<Milestone> = buildList {
        val C = MilestoneGroup.COLLECTION
        addAll(series(Metric.PIECES, C, "pieces", listOf(5, 12, 24, 36, 48, 60, 72), { "Coleccionista · $it" }, { "Consigue $it piezas del álbum" }))
        addAll(series(Metric.EPIC_PLUS, C, "epic", listOf(2, 5, 9, 14, 18), { "Ojo de águila · $it" }, { "Ten $it piezas épicas o legendarias" }))
        addAll(series(Metric.FOIL, C, "foil", listOf(3, 6, 12, 24, 48, 72), { "Brillante · $it" }, { "Convierte $it piezas en brillantes" }))
        addAll(series(Metric.SERIES, C, "series", listOf(1, 3, 5, 7, 9), { "Series completas · $it" }, { "Completa $it series del álbum" }))
        addAll(series(Metric.ALBUM, C, "album", listOf(1), { "Álbum completo" }, { "Completa las 9 series del álbum" }))

        val G = MilestoneGroup.CAMPAIGN
        addAll(series(Metric.CAMPAIGN_LEVEL, G, "level", listOf(20, 60, 120, 240, 480, 800, 1200, 1800, 2400), { "Nivel $it" }, { "Supera el nivel $it de la campaña" }))
        addAll(series(Metric.CAMPAIGN_STARS, G, "stars", listOf(60, 200, 500, 1000, 2000, 3500, 5000), { "$it estrellas" }, { "Gana $it estrellas en la campaña" }))

        val T = MilestoneGroup.TOWER
        addAll(series(Metric.TOWER, T, "tower", listOf(5, 10, 20, 30, 50, 75, 100), { "Torre · piso $it" }, { "Llega al piso $it de la Torre infinita" }))
        addAll(series(Metric.BOSSES, T, "boss", listOf(1, 10, 40, 100), { "Jefes · $it" }, { "Derrota a $it jefes" }))

        val M = MilestoneGroup.MASTERY
        addAll(series(Metric.WIN_STREAK, M, "winstreak", listOf(5, 10, 20, 40), { "Racha de $it victorias" }, { "Gana $it niveles seguidos" }))
        addAll(series(Metric.DAY_STREAK, M, "daystreak", listOf(14, 60, 150), { "$it días seguidos" }, { "Juega $it días seguidos" }))
        addAll(series(Metric.KINDS, M, "kinds", listOf(5, 10, 17), { "Maestro de reglas · $it" }, { "Gana niveles de $it tipos distintos" }))
        addAll(series(Metric.FLOW, M, "flow", listOf(1, 10, 50), { "En estado flow · $it" }, { "Llega a ¡FLOW! en $it niveles" }))
        addAll(series(Metric.DAILY, M, "daily", listOf(3, 10, 30, 100), { "Retos diarios · $it" }, { "Completa $it retos diarios" }))

        val S = MilestoneGroup.SOCIAL
        addAll(series(Metric.FRIENDS, S, "friends", listOf(1, 3, 8), { "Amigos · $it" }, { "Ten $it amigos en ParDos" }))

        val R = MilestoneGroup.TROPHIES
        addAll(series(Metric.TROPHIES, R, "trophies", listOf(10, 25, 50, 75), { "Cazatrofeos · $it" }, { "Desbloquea $it logros" }))
    }

    private val byId = all.associateBy { it.id }
    fun byId(id: String): Milestone? = byId[id]
    fun inGroup(g: MilestoneGroup): List<Milestone> = all.filter { it.group == g }

    /** Los hitos aún sin lograr, de menor a mayor lejanía: la lista de "lo siguiente". */
    fun nextUp(s: PrestigeStats, done: Set<String>, count: Int = 3): List<Milestone> =
        all.filter { it.id !in done }.sortedByDescending { it.fraction(s) }.take(count)
}

// ============================== TÍTULOS ==============================

sealed interface TitleUnlock {
    data object Free : TitleUnlock
    data class Rank(val rank: PrestigeRank) : TitleUnlock
    data class Milestone(val id: String) : TitleUnlock
    data object PlatinumTrophy : TitleUnlock
    data class Gems(val price: Int) : TitleUnlock
}

data class ProfileTitle(val id: String, val name: String, val rarity: Rarity, val unlock: TitleUnlock)

object ProfileTitles {
    const val DEFAULT_ID = "default"

    val all: List<ProfileTitle> = buildList {
        add(ProfileTitle(DEFAULT_ID, "Jugador Zen", Rarity.COMMON, TitleUnlock.Free))
        // por rango
        add(ProfileTitle("rank_apprentice", "Aprendiz del tablero", Rarity.COMMON, TitleUnlock.Rank(PrestigeRank.APPRENTICE)))
        add(ProfileTitle("rank_adept", "Adepto de las fichas", Rarity.COMMON, TitleUnlock.Rank(PrestigeRank.ADEPT)))
        add(ProfileTitle("rank_expert", "Experto del 2048", Rarity.RARE, TitleUnlock.Rank(PrestigeRank.EXPERT)))
        add(ProfileTitle("rank_master", "Maestro zen", Rarity.RARE, TitleUnlock.Rank(PrestigeRank.MASTER)))
        add(ProfileTitle("rank_grandmaster", "Gran Maestro", Rarity.EPIC, TitleUnlock.Rank(PrestigeRank.GRANDMASTER)))
        add(ProfileTitle("rank_legend", "Leyenda viva", Rarity.LEGENDARY, TitleUnlock.Rank(PrestigeRank.LEGEND)))
        add(ProfileTitle("rank_mythic", "Mítico", Rarity.LEGENDARY, TitleUnlock.Rank(PrestigeRank.MYTHIC)))
        // por hitos
        add(ProfileTitle("t_collector", "Coleccionista", Rarity.RARE, TitleUnlock.Milestone("pieces_24")))
        add(ProfileTitle("t_curator", "Curador del álbum", Rarity.EPIC, TitleUnlock.Milestone("pieces_60")))
        add(ProfileTitle("t_album", "Dueño del álbum", Rarity.LEGENDARY, TitleUnlock.Milestone("album_1")))
        add(ProfileTitle("t_shiny", "Cazador de brillos", Rarity.EPIC, TitleUnlock.Milestone("foil_24")))
        add(ProfileTitle("t_eagle", "Ojo de águila", Rarity.RARE, TitleUnlock.Milestone("epic_9")))
        add(ProfileTitle("t_explorer", "Explorador", Rarity.COMMON, TitleUnlock.Milestone("level_120")))
        add(ProfileTitle("t_conqueror", "Conquistador de los 2.400", Rarity.LEGENDARY, TitleUnlock.Milestone("level_2400")))
        add(ProfileTitle("t_stargazer", "Estrella fugaz", Rarity.EPIC, TitleUnlock.Milestone("stars_2000")))
        add(ProfileTitle("t_climber", "Escalador de torres", Rarity.RARE, TitleUnlock.Milestone("tower_20")))
        add(ProfileTitle("t_titan", "Titán de la torre", Rarity.LEGENDARY, TitleUnlock.Milestone("tower_75")))
        add(ProfileTitle("t_hunter", "Cazador de jefes", Rarity.EPIC, TitleUnlock.Milestone("boss_40")))
        add(ProfileTitle("t_rules", "Maestro de reglas", Rarity.EPIC, TitleUnlock.Milestone("kinds_17")))
        add(ProfileTitle("t_unstoppable", "Imparable", Rarity.EPIC, TitleUnlock.Milestone("winstreak_20")))
        add(ProfileTitle("t_steady", "Constante", Rarity.RARE, TitleUnlock.Milestone("daystreak_60")))
        add(ProfileTitle("t_flow", "En llamas", Rarity.RARE, TitleUnlock.Milestone("flow_10")))
        add(ProfileTitle("t_friend", "Alma de la fiesta", Rarity.RARE, TitleUnlock.Milestone("friends_8")))
        add(ProfileTitle("t_hunter_trophy", "Cazatrofeos", Rarity.EPIC, TitleUnlock.Milestone("trophies_50")))
        add(ProfileTitle("t_daily", "Fiel del reto diario", Rarity.RARE, TitleUnlock.Milestone("daily_30")))
        // el Platino
        add(ProfileTitle("t_platinum", "PLATINO", Rarity.LEGENDARY, TitleUnlock.PlatinumTrophy))
        // a la venta (sumidero de gemas, solo cosmético)
        add(ProfileTitle("g_fusion_king", "Rey de las fusiones", Rarity.RARE, TitleUnlock.Gems(200)))
        add(ProfileTitle("g_night", "Noche de brujas", Rarity.RARE, TitleUnlock.Gems(250)))
        add(ProfileTitle("g_mind", "Mente maestra", Rarity.EPIC, TitleUnlock.Gems(400)))
        add(ProfileTitle("g_supreme", "El Zen supremo", Rarity.LEGENDARY, TitleUnlock.Gems(800)))
    }

    private val byId = all.associateBy { it.id }
    fun byId(id: String?): ProfileTitle = byId[id] ?: byId.getValue(DEFAULT_ID)

    /** ¿El título está disponible ya (sin contar los que se compran)? */
    fun isEarned(t: ProfileTitle, rank: PrestigeRank, doneMilestones: Set<String>, platinum: Boolean): Boolean = when (val u = t.unlock) {
        TitleUnlock.Free -> true
        is TitleUnlock.Rank -> rank.ordinal >= u.rank.ordinal
        is TitleUnlock.Milestone -> u.id in doneMilestones
        TitleUnlock.PlatinumTrophy -> platinum
        is TitleUnlock.Gems -> false
    }

    /** Cómo se consigue, en una frase corta para el catálogo. */
    fun howTo(t: ProfileTitle): String = when (val u = t.unlock) {
        TitleUnlock.Free -> "De inicio"
        is TitleUnlock.Rank -> "Llega al rango ${u.rank.title}"
        is TitleUnlock.Milestone -> PrestigeMilestones.byId(u.id)?.description ?: "Logra un hito"
        TitleUnlock.PlatinumTrophy -> "Consigue el Platino"
        is TitleUnlock.Gems -> "${u.price} gemas"
    }
}

// ============================== PLATINO ==============================

/** El Platino: todos los logros (los trofeos) y todos los hitos. El máximo orgullo del juego. */
object Platinum {
    const val REWARD_COINS = 5_000
    const val REWARD_GEMS = 300

    /** (conseguidos, totales) entre logros e hitos. */
    fun progress(achievementsDone: Int, achievementsTotal: Int, milestonesDone: Int, milestonesTotal: Int): Pair<Int, Int> =
        (achievementsDone + milestonesDone) to (achievementsTotal + milestonesTotal)

    fun isEarned(achievementsDone: Int, achievementsTotal: Int, milestonesDone: Int, milestonesTotal: Int): Boolean =
        achievementsTotal > 0 && achievementsDone >= achievementsTotal && milestonesDone >= milestonesTotal
}

// ============================== RIVALES ==============================

/** Un jugador del ranking de amigos con su puntuación de prestigio. */
data class RankEntry(val uid: String, val name: String, val score: Int, val isMe: Boolean = false)

/** El amigo justo por encima y el justo por debajo: la comparación que anima a seguir (o a defender). */
data class Rivalry(val me: RankEntry, val position: Int, val above: RankEntry?, val below: RankEntry?) {
    val gapToAbove: Int get() = above?.let { (it.score - me.score + 1).coerceAtLeast(1) } ?: 0
    val gapToBelow: Int get() = below?.let { (me.score - it.score).coerceAtLeast(0) } ?: 0
}

object Rivals {
    /** Ordena por prestigio (empata por nombre) y devuelve la posición de [me] y sus vecinos. */
    fun rank(me: RankEntry, friends: List<RankEntry>): Pair<List<RankEntry>, Rivalry> {
        val sorted = (friends.filter { it.uid != me.uid } + me).sortedWith(compareByDescending<RankEntry> { it.score }.thenBy { it.name })
        val i = sorted.indexOfFirst { it.uid == me.uid }
        return sorted to Rivalry(me, i + 1, sorted.getOrNull(i - 1), sorted.getOrNull(i + 1))
    }
}
