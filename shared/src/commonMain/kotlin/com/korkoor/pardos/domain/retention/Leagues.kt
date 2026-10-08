package com.korkoor.pardos.domain.retention

import com.korkoor.pardos.domain.collection.ChestType

/**
 * Ligas semanales: una escalera personal de Bronce a Diamante. Cada semana cuentan las estrellas que
 * ganaste; al cerrar la semana subes, te mantienes o bajas. No hay rivales: compites contra tu propia
 * constancia, así funciona igual sin conexión (y en iOS).
 */
enum class League(val displayName: String, val promoteStars: Int, val keepStars: Int) {
    BRONZE("Bronce", promoteStars = 20, keepStars = 0),
    SILVER("Plata", promoteStars = 30, keepStars = 8),
    GOLD("Oro", promoteStars = 40, keepStars = 12),
    SAPPHIRE("Zafiro", promoteStars = 55, keepStars = 16),
    RUBY("Rubí", promoteStars = 70, keepStars = 22),
    DIAMOND("Diamante", promoteStars = 85, keepStars = 28);

    val isTop: Boolean get() = this == DIAMOND
    val isBottom: Boolean get() = this == BRONZE

    fun next(): League? = entries.getOrNull(ordinal + 1)
    fun previous(): League? = entries.getOrNull(ordinal - 1)

    companion object {
        fun fromId(id: String?): League = entries.firstOrNull { it.name == id } ?: BRONZE
    }
}

enum class LeagueOutcome { PROMOTED, STAYED, DEMOTED, TOP_HELD }

/** Lo que pasó al cerrar una semana y lo que se cobra por ello. */
data class LeagueResult(
    val from: League,
    val to: League,
    val stars: Int,
    val outcome: LeagueOutcome,
    val coins: Int,
    val gems: Int,
    val chest: ChestType?
) {
    val hasReward: Boolean get() = coins > 0 || gems > 0 || chest != null

    /** Formato compacto para guardar un resultado pendiente de reclamar. */
    fun encode(): String = listOf(from.name, to.name, stars, outcome.name, coins, gems, chest?.name ?: "").joinToString("|")

    companion object {
        fun decode(raw: String?): LeagueResult? {
            val p = raw?.split("|") ?: return null
            if (p.size != 7) return null
            return try {
                LeagueResult(
                    from = League.valueOf(p[0]), to = League.valueOf(p[1]), stars = p[2].toInt(),
                    outcome = LeagueOutcome.valueOf(p[3]), coins = p[4].toInt(), gems = p[5].toInt(),
                    chest = p[6].takeIf { it.isNotEmpty() }?.let { ChestType.valueOf(it) }
                )
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}

object Leagues {
    /** Monedas por estrella ganada en la semana, según la liga en la que jugaste: premia la constancia. */
    private fun coinsPerStar(league: League) = 4 + league.ordinal * 2

    /** Premio extra por subir a [to]. */
    private fun promotionGems(to: League) = 2 + to.ordinal * 2
    private fun promotionChest(to: League): ChestType? = when {
        to.ordinal >= League.RUBY.ordinal -> ChestType.EPIC
        to.ordinal >= League.GOLD.ordinal -> ChestType.RARE
        else -> ChestType.COMMON
    }

    /** Estrellas que faltan esta semana para subir (0 si ya llegaste). En la cima, para defenderla. */
    fun starsToPromote(league: League, weekStars: Int): Int = (league.promoteStars - weekStars).coerceAtLeast(0)

    /** Avance de 0 a 1 hacia el ascenso. */
    fun progress(league: League, weekStars: Int): Float =
        (weekStars.toFloat() / league.promoteStars).coerceIn(0f, 1f)

    /** ¿Con las estrellas de hoy estarías en riesgo de bajar al cerrar la semana? */
    fun atRisk(league: League, weekStars: Int): Boolean = !league.isBottom && weekStars < league.keepStars

    /** Resultado de cerrar una semana en [league] con [weekStars] estrellas. */
    fun evaluate(league: League, weekStars: Int): LeagueResult {
        val stars = weekStars.coerceAtLeast(0)
        val coins = stars * coinsPerStar(league)
        return when {
            stars >= league.promoteStars && league.isTop ->
                LeagueResult(league, league, stars, LeagueOutcome.TOP_HELD, coins + 150, 5, ChestType.EPIC)
            stars >= league.promoteStars -> {
                val up = league.next()!!
                LeagueResult(league, up, stars, LeagueOutcome.PROMOTED, coins + 100 * up.ordinal, promotionGems(up), promotionChest(up))
            }
            atRisk(league, stars) ->
                LeagueResult(league, league.previous()!!, stars, LeagueOutcome.DEMOTED, coins, 0, null)
            else -> LeagueResult(league, league, stars, LeagueOutcome.STAYED, coins, 0, null)
        }
    }

    /**
     * Semanas sin jugar: cada una cuenta como 0 estrellas y solo puede hacerte bajar (sin premios).
     * Devuelve la liga final tras [missedWeeks] semanas vacías (tope de 3 para no castigar de más).
     */
    fun afterMissedWeeks(league: League, missedWeeks: Int): League {
        var current = league
        repeat(missedWeeks.coerceIn(0, 3)) {
            val r = evaluate(current, 0)
            if (r.outcome == LeagueOutcome.DEMOTED) current = r.to
        }
        return current
    }
}
