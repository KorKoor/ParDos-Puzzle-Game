package com.korkoor.pardos.domain.retention

enum class GoalKind { CHEST_READY, LEAGUE, WEEKLY, SEASON, DAILY_CHALLENGE, CHEST_TIMER }

/** Una meta cercana para mostrar al terminar una partida ("una más y lo consigo"). [progress] va de 0 a 1. */
data class NextGoal(val kind: GoalKind, val title: String, val detail: String, val progress: Float)

/** Lo que el selector necesita saber. Los campos opcionales van en null / 0 cuando no aplican. */
data class GoalInput(
    /** Milisegundos hasta el próximo cofre gratis; 0 = ya está listo. */
    val chestRemainingMs: Long,
    val leagueNextName: String? = null,
    val leagueStarsToPromote: Int = 0,
    val leaguePromoteTarget: Int = 1,
    val weeklyTitle: String? = null,
    val weeklyProgress: Int = 0,
    val weeklyTarget: Int = 1,
    /** Siguiente nivel del pase (0 si ya está completo) y puntos que faltan para llegar. */
    val seasonNextTier: Int = 0,
    val seasonPointsLeft: Int = 0,
    val seasonPointsPerTier: Int = 100,
    val dailyChallengeDone: Boolean = true
)

/**
 * Elige UNA meta: la más cercana y más valiosa. Orden de prioridad: cofre listo > liga a punto de subir >
 * misión semanal casi hecha > nivel del pase cerca > reto diario sin jugar > cuenta atrás del cofre.
 */
object NextGoals {
    const val LEAGUE_NEAR_STARS = 8
    const val WEEKLY_NEAR_RATIO = 0.5f
    const val SEASON_NEAR_RATIO = 0.4f

    fun pick(i: GoalInput): NextGoal? {
        if (i.chestRemainingMs <= 0L) {
            return NextGoal(GoalKind.CHEST_READY, "¡Tu cofre gratis está listo!", "Ábrelo desde el menú", 1f)
        }
        if (i.leagueNextName != null && i.leagueStarsToPromote in 1..LEAGUE_NEAR_STARS) {
            val target = i.leaguePromoteTarget.coerceAtLeast(1)
            return NextGoal(
                GoalKind.LEAGUE, "Sube a ${i.leagueNextName}",
                "Te faltan ${i.leagueStarsToPromote} ★ esta semana",
                ((target - i.leagueStarsToPromote).toFloat() / target).coerceIn(0f, 1f)
            )
        }
        if (i.weeklyTitle != null && i.weeklyTarget > 0 && i.weeklyProgress < i.weeklyTarget) {
            val ratio = i.weeklyProgress.toFloat() / i.weeklyTarget
            if (ratio >= WEEKLY_NEAR_RATIO) {
                return NextGoal(GoalKind.WEEKLY, i.weeklyTitle, "${i.weeklyProgress} de ${i.weeklyTarget}", ratio.coerceIn(0f, 1f))
            }
        }
        if (i.seasonNextTier > 0 && i.seasonPointsPerTier > 0 && i.seasonPointsLeft in 1..(i.seasonPointsPerTier * SEASON_NEAR_RATIO).toInt()) {
            return NextGoal(
                GoalKind.SEASON, "Nivel ${i.seasonNextTier} del pase",
                "Faltan ${i.seasonPointsLeft} puntos",
                (1f - i.seasonPointsLeft.toFloat() / i.seasonPointsPerTier).coerceIn(0f, 1f)
            )
        }
        if (!i.dailyChallengeDone) {
            return NextGoal(GoalKind.DAILY_CHALLENGE, "Reto diario", "Aún no lo juegas hoy", 0f)
        }
        val mins = (i.chestRemainingMs / 60_000L).coerceAtLeast(1)
        val wait = if (mins >= 60) "${mins / 60} h ${mins % 60} min" else "$mins min"
        return NextGoal(
            GoalKind.CHEST_TIMER, "Próximo cofre gratis", "En $wait",
            (1f - i.chestRemainingMs.toFloat() / com.korkoor.pardos.domain.economy.Economy.FREE_CHEST_COOLDOWN_MS).coerceIn(0f, 1f)
        )
    }
}
