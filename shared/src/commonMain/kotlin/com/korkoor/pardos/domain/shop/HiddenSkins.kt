package com.korkoor.pardos.domain.shop

/** Lo que el juego sabe de ti para decidir si descubriste una skin secreta. Todo son contadores acumulados. */
data class PlayerStats(
    /** Victorias entre las 00:00 y las 04:59 (hora local). */
    val nightWins: Int = 0,
    /** Victorias entre las 05:00 y las 07:59. */
    val dawnWins: Int = 0,
    val bestStreak: Int = 0,
    /** Ficha más alta creada alguna vez. */
    val bestTile: Int = 0,
    val albumComplete: Boolean = false,
    /** Días distintos en los que entraste. */
    val daysPlayed: Int = 0,
    val totalStars: Int = 0,
    val totalWins: Int = 0
)

/** Franja del día para las skins de madrugada. */
enum class DayPart { NIGHT, DAWN, OTHER;
    companion object {
        /** [minuteOfDay]: minutos desde medianoche (0..1439). */
        fun of(minuteOfDay: Int): DayPart = when (minuteOfDay) {
            in 0 until 5 * 60 -> NIGHT
            in 5 * 60 until 8 * 60 -> DAWN
            else -> OTHER
        }
    }
}

/**
 * Skins ocultas. Cada una tiene una pista poética (nunca dice el número exacto) y una condición que se evalúa con
 * [PlayerStats]. Al cumplirla se entrega sola. Añadir una = una fila aquí + la skin en `TileSkin`.
 */
object HiddenSkins {
    const val NIGHT_WINS = 3
    const val DAWN_WINS = 3
    const val STREAK_DAYS = 30
    const val TILE = 2048
    const val DAYS_PLAYED = 30
    const val STARS = 300
    const val WINS = 200

    data class Def(val skin: TileSkin, val hint: String, val isUnlocked: (PlayerStats) -> Boolean)

    val defs: List<Def> = listOf(
        Def(TileSkin.NIGHT_OWL, "Hay quien juega mientras el mundo duerme…") { it.nightWins >= NIGHT_WINS },
        Def(TileSkin.DAWN, "El primer rayo de sol premia a los madrugadores.") { it.dawnWins >= DAWN_WINS },
        Def(TileSkin.PHOENIX, "Renace de las cenizas… si no dejas que el fuego se apague.") { it.bestStreak >= STREAK_DAYS },
        Def(TileSkin.CROWN, "Solo un rey alcanza la ficha más alta.") { it.bestTile >= TILE },
        Def(TileSkin.PRISM, "Reúne todos los colores del Álbum.") { it.albumComplete },
        Def(TileSkin.JADE, "La constancia se vuelve piedra preciosa.") { it.daysPlayed >= DAYS_PLAYED },
        Def(TileSkin.METEOR, "Cada estrella que ganes te acerca a una fugaz.") { it.totalStars >= STARS },
        Def(TileSkin.OBSIDIAN, "Nace del fuego tras cientos de victorias.") { it.totalWins >= WINS }
    )

    fun defFor(skin: TileSkin): Def? = defs.firstOrNull { it.skin == skin }

    fun hint(skin: TileSkin): String = defFor(skin)?.hint ?: ""

    /** Skins ocultas que [stats] ya merece. */
    fun unlocked(stats: PlayerStats): List<TileSkin> = defs.filter { it.isUnlocked(stats) }.map { it.skin }

    /** Las que merece y todavía no tiene. */
    fun newlyUnlocked(stats: PlayerStats, owned: Set<String>): List<TileSkin> =
        unlocked(stats).filter { it.id !in owned }
}
