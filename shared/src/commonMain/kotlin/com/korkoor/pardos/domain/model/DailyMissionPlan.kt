package com.korkoor.pardos.domain.model

import kotlin.random.Random

/**
 * Qué misiones toca hoy. En lugar de 3 al azar (que podían salir las tres difíciles o las tres iguales), cada día lleva
 * **una fácil, una media y una difícil, de tipos distintos**: siempre hay algo que se cobra en la primera partida, algo que
 * pide un poco más y un reto para quien se queda a jugar. Es determinista por día (el mismo día sale lo mismo).
 */
object DailyMissionPlan {
    const val PER_DAY = 3
    private const val LIGHT_MAX_XP = 30
    private const val MEDIUM_MAX_XP = 60

    enum class Tier { LIGHT, MEDIUM, HARD }

    fun tierOf(m: DailyMission): Tier = when {
        m.xpReward <= LIGHT_MAX_XP -> Tier.LIGHT
        m.xpReward <= MEDIUM_MAX_XP -> Tier.MEDIUM
        else -> Tier.HARD
    }

    fun forDay(day: Int, pool: List<DailyMission> = MissionPool.allMissions): List<DailyMission> {
        val rnd = Random(day.toLong() * 7_919L + 13L)
        val used = mutableSetOf<MissionType>()
        val picked = mutableListOf<DailyMission>()
        for (tier in Tier.entries) {
            val candidates = pool.filter { tierOf(it) == tier }
            val preferred = candidates.filter { it.type !in used }.ifEmpty { candidates }
            if (preferred.isEmpty()) continue
            val m = preferred[rnd.nextInt(preferred.size)]
            picked += m
            used += m.type
        }
        // Si faltó algún nivel en el catálogo, se rellena con lo que haya (sin repetir)
        if (picked.size < PER_DAY) {
            pool.filter { it !in picked }.shuffled(rnd).take(PER_DAY - picked.size).forEach { picked += it }
        }
        return picked
    }
}

/**
 * Días perfectos: cobrar las tres misiones del día. Encadenarlos da premios de racha (el motivo para volver mañana).
 */
object PerfectDays {
    data class Milestone(val days: Int, val gems: Int)

    val milestones = listOf(Milestone(3, 3), Milestone(7, 8), Milestone(14, 15), Milestone(30, 40))

    /** Racha de días perfectos tras cobrar las tres de hoy: sigue si el último día perfecto fue ayer, se reinicia si no. */
    fun next(lastPerfectDay: Int?, streak: Int, today: Int): Int = when {
        lastPerfectDay == today -> streak              // ya contado hoy
        lastPerfectDay == today - 1 -> streak + 1
        else -> 1
    }

    /** Premio exacto de hoy (null si no cae en un hito). */
    fun milestoneFor(streak: Int): Milestone? = milestones.firstOrNull { it.days == streak }

    /** Siguiente hito por alcanzar, para enseñarlo ("faltan 2 días para 8 gemas"). */
    fun nextMilestone(streak: Int): Milestone? = milestones.firstOrNull { it.days > streak }

    /** La racha que se muestra: si el último día perfecto no fue hoy ni ayer, ya está rota. */
    fun alive(lastPerfectDay: Int?, streak: Int, today: Int): Int =
        if (lastPerfectDay != null && lastPerfectDay >= today - 1) streak else 0
}
