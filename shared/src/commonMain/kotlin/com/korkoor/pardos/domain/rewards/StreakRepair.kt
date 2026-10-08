package com.korkoor.pardos.domain.rewards

/**
 * Recuperar una racha perdida. Solo se ofrece si la racha valía la pena (3+ días) y se perdió por poco
 * (faltaron 1 o 2 días); la oferta dura el día en que se detecta. Cuesta gemas o un anuncio (uno por semana).
 */
object StreakRepair {
    const val MIN_STREAK = 3
    /** Diferencia máxima entre hoy y el último día jugado: 3 = faltaron dos días. */
    const val MAX_GAP_DAYS = 3
    const val MIN_GEMS = 10
    const val MAX_GEMS = 60
    /** Cada cuántos días se puede recuperar gratis con un anuncio. */
    const val AD_COOLDOWN_DAYS = 7

    /** ¿Se debe ofrecer recuperar? [before] es el estado ANTES de procesar el día de hoy. */
    fun canOffer(before: StreakState, today: Int): Boolean =
        before.streak >= MIN_STREAK && before.lastDay > 0 && (today - before.lastDay) in 2..MAX_GAP_DAYS

    /** Gemas: más racha, más cuesta (2 por día), entre [MIN_GEMS] y [MAX_GEMS]. */
    fun gemCost(lostStreak: Int): Int = (lostStreak * 2).coerceIn(MIN_GEMS, MAX_GEMS)

    /** ¿Ya pasó el enfriamiento del anuncio? [lastAdDay] = 0 si nunca se usó. */
    fun adAvailable(lastAdDay: Int, today: Int): Boolean = lastAdDay <= 0 || today - lastAdDay >= AD_COOLDOWN_DAYS

    /** Estado tras recuperar: la racha continúa y hoy cuenta como un día más. */
    fun repaired(before: StreakState, today: Int): StreakState {
        val s = before.streak + 1
        return StreakState(streak = s, best = maxOf(before.best, s), lastDay = today)
    }
}
