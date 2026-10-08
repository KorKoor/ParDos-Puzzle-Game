package com.korkoor.pardos.domain.retention

import com.korkoor.pardos.domain.collection.ChestType
import com.korkoor.pardos.domain.economy.Economy
import kotlin.random.Random

// ============================ Misiones semanales ============================

enum class WeeklyType { PLAY_GAMES, WIN_LEVELS, DAILY_CHALLENGES, MERGE_PAIRS, EARN_STARS, OPEN_CHESTS, LOGIN_DAYS, REACH_TILE }

data class WeeklyMission(
    val id: String,
    val type: WeeklyType,
    val title: String,
    val target: Int,
    val coins: Int
)

/** Metas más largas que las diarias: dan un motivo para volver varios días de la semana. */
object WeeklyMissions {
    const val PER_WEEK = 4

    val pool: List<WeeklyMission> = listOf(
        WeeklyMission("play20", WeeklyType.PLAY_GAMES, "Juega 20 partidas", 20, 150),
        WeeklyMission("play35", WeeklyType.PLAY_GAMES, "Juega 35 partidas", 35, 250),
        WeeklyMission("win10", WeeklyType.WIN_LEVELS, "Gana 10 niveles", 10, 200),
        WeeklyMission("win20", WeeklyType.WIN_LEVELS, "Gana 20 niveles", 20, 350),
        WeeklyMission("daily3", WeeklyType.DAILY_CHALLENGES, "Completa 3 retos diarios", 3, 200),
        WeeklyMission("daily5", WeeklyType.DAILY_CHALLENGES, "Completa 5 retos diarios", 5, 300),
        WeeklyMission("merge800", WeeklyType.MERGE_PAIRS, "Combina 800 pares", 800, 200),
        WeeklyMission("stars25", WeeklyType.EARN_STARS, "Gana 25 estrellas", 25, 250),
        WeeklyMission("chests3", WeeklyType.OPEN_CHESTS, "Abre 3 cofres", 3, 150),
        WeeklyMission("login5", WeeklyType.LOGIN_DAYS, "Entra 5 días distintos", 5, 200),
        WeeklyMission("tile512", WeeklyType.REACH_TILE, "Crea una ficha de 512", 512, 250)
    )

    /** Premio por completar las cuatro. */
    val completionChest = ChestType.RARE
    const val COMPLETION_GEMS = 8

    /** Las misiones de la semana: iguales para todos y de tipos distintos. */
    fun forWeek(weekId: Int): List<WeeklyMission> {
        val rng = Random(weekId.toLong() * 7919L + 13L)
        return pool.groupBy { it.type }.values
            .map { it[rng.nextInt(it.size)] }
            .shuffled(rng)
            .take(PER_WEEK)
    }
}

// ============================ Hucha ============================

/** La hucha guarda gemas mientras juegas; romperla es una compra opcional. */
object PiggyBank {
    const val CAP = Economy.PIGGY_CAP
    const val MIN_TO_BREAK = Economy.PIGGY_MIN_TO_BREAK

    fun gemsForWin(dailyChallenge: Boolean): Int = if (dailyChallenge) 3 else 1

    fun deposit(current: Int, add: Int): Int = (current + add.coerceAtLeast(0)).coerceAtMost(CAP)

    fun canBreak(current: Int): Boolean = current >= MIN_TO_BREAK
}

// ============================ Nivel de jugador ============================

data class LevelReward(val level: Int, val coins: Int, val gems: Int, val chest: ChestType?)

/** Cada nivel de jugador paga algo; los múltiplos de 5 y de 10 traen cofre. */
object PlayerLevelRewards {
    fun forLevel(level: Int): LevelReward {
        require(level >= 2)
        return LevelReward(
            level = level,
            coins = 40 + 10 * level.coerceAtMost(30),
            gems = if (level % 5 == 0) 2 + level / 10 else 0,
            chest = when {
                level % 10 == 0 -> ChestType.RARE
                level % 5 == 0 -> ChestType.COMMON
                else -> null
            }
        )
    }

    /** Premios de todos los niveles entre [from] (ya cobrado) y [to] (actual), sin incluir [from]. */
    fun between(from: Int, to: Int): List<LevelReward> =
        ((from + 1).coerceAtLeast(2)..to).map { forLevel(it) }
}

// ============================ Regreso y primera victoria ============================

data class ComebackGift(val coins: Int, val chest: ChestType, val freezes: Int)

object Comeback {
    /** Regalo para quien vuelve tras [daysAway] días sin entrar; `null` si la ausencia fue corta. */
    fun giftFor(daysAway: Int): ComebackGift? = when {
        daysAway >= 7 -> ComebackGift(300, ChestType.RARE, 1)
        daysAway >= Economy.COMEBACK_MIN_DAYS -> ComebackGift(150, ChestType.COMMON, 0)
        else -> null
    }
}
