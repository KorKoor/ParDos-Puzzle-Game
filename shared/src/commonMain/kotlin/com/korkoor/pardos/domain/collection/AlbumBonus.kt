package com.korkoor.pardos.domain.collection

/**
 * Bono PERMANENTE de monedas por avanzar en el Álbum. Da una razón para seguir abriendo cofres
 * incluso cuando ya tienes lo que querías: cada pieza y cada serie completa suben tus ganancias.
 *
 *  - +1 % por cada 8 piezas distintas (máx. 9 %).
 *  - +3 % por cada serie completa (máx. 27 %).
 *  - +6 % por el álbum entero.
 *  - +1 % por cada 6 piezas Brillantes (ver [FoilRules]).
 * Tope total: 45 %.
 */
object AlbumBonus {
    const val PER_EIGHT_PIECES = 1
    const val PER_SERIES = 3
    const val FULL_ALBUM = 6
    const val MAX_PERCENT = 45

    data class Breakdown(val pieces: Int, val series: Int, val album: Int, val foil: Int = 0) {
        val total: Int get() = (pieces + series + album + foil).coerceAtMost(MAX_PERCENT)
    }

    fun breakdown(owned: Set<String>, foil: Set<String> = emptySet()): Breakdown {
        val valid = owned.count { CollectibleCatalog.byId(it) != null }
        val pieces = valid / 8 * PER_EIGHT_PIECES
        val series = Series.entries.count { CollectibleCatalog.isSeriesComplete(it, owned) } * PER_SERIES
        val album = if (CollectibleCatalog.isAlbumComplete(owned)) FULL_ALBUM else 0
        return Breakdown(pieces, series, album, FoilRules.coinPercent(foil))
    }

    /** Porcentaje total de monedas extra (0..30). */
    fun coinPercent(owned: Set<String>, foil: Set<String> = emptySet()): Int = breakdown(owned, foil).total

    /** Multiplicador sobre el que se SUMA al del evento, p. ej. 0.12 para +12 %. */
    fun coinBonus(owned: Set<String>, foil: Set<String> = emptySet()): Double = coinPercent(owned, foil) / 100.0

    /** Combina el multiplicador de evento con el bono del álbum de forma aditiva (x2 + 12 % = x2.12). */
    fun combine(eventMultiplier: Double, owned: Set<String>, foil: Set<String> = emptySet()): Double = eventMultiplier + coinBonus(owned, foil)
}
