package com.korkoor.pardos.domain.retention

import com.korkoor.pardos.domain.collection.Series

/**
 * Serie destacada del día: cada día una serie del álbum es la protagonista y el primer cofre que abres ese día trae una carta extra
 * de ella. La rotación recorre las 32 series sin repetir en 32 días; en Noche de brujas la serie de Halloween sale un día sí y otro no.
 */
object FeaturedSeries {
    fun seriesFor(epochDay: Int): Series {
        if (SeasonalCopy.isHalloweenWindow(epochDay) && epochDay % 2 == 0) return Series.HALLOWEEN
        val all = Series.entries
        val index = ((epochDay.toLong() * 7L) % all.size + all.size) % all.size
        return all[index.toInt()]
    }
}
