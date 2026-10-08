package com.korkoor.pardos.ui.design

import com.korkoor.pardos.data.local.LocalDay
import com.korkoor.pardos.domain.retention.SeasonalCopy

/**
 * Temporada activa. Durante la Noche de brujas (todo octubre y hasta el 2 de noviembre) la app entera se viste de Halloween:
 * paleta naranja calabaza y morado, fondo con calabazas y fantasmas, mapa embrujado, textos de temporada y adornos.
 * Se calcula una vez por arranque (el día no cambia mientras la app está abierta de forma relevante).
 */
object Season {
    val halloween: Boolean by lazy { SeasonalCopy.isHalloweenWindow(LocalDay.today()) }

    /** Elige entre el texto normal y el de temporada. */
    fun text(normal: String, spooky: String): String = if (halloween) spooky else normal
}
