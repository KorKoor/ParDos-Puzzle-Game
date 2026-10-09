package com.korkoor.pardos.domain.meta

import com.korkoor.pardos.domain.collection.Collectible
import com.korkoor.pardos.domain.collection.CollectibleCatalog
import com.korkoor.pardos.domain.collection.Drop
import com.korkoor.pardos.domain.collection.Series
import com.korkoor.pardos.domain.retention.FeaturedSeries
import kotlin.random.Random

/** Serie destacada: el primer cofre de cada día trae una carta extra de esa serie (prefiere las que aún no tienes). */
internal class FeaturedOps(private val s: MetaStore, private val col: CollectionOps, private val clock: Clock) {
    fun series(): Series = FeaturedSeries.seriesFor(clock.today)

    fun bonusReady(): Boolean = s.int("featured_bonus_day", -1) != clock.today

    /** La carta extra del primer cofre del día (y la marca como usada); null si ya se usó hoy. */
    fun takeBonus(random: Random): Drop? {
        if (!bonusReady()) return null
        s.setInt("featured_bonus_day", clock.today)
        val pool: List<Collectible> = CollectibleCatalog.all.filter { it.series == series() }
        if (pool.isEmpty()) return null
        val unowned = pool.filter { it.id !in col.owned }
        val pick = (if (unowned.isNotEmpty()) unowned else pool).random(random)
        val isNew = pick.id !in col.owned
        val drop = Drop(pick, isNew, if (isNew) 0 else pick.rarity.shardValue, bonus = true)
        col.addBonusDrop(drop)
        return drop
    }

    fun json(): Raw {
        val ser = series()
        val have = col.owned
        val total = CollectibleCatalog.all.count { it.series == ser }
        val owned = CollectibleCatalog.all.count { it.series == ser && it.id in have }
        return Raw(obj("series" to ser.id, "name" to ser.nameEs, "owned" to owned, "total" to total, "bonusReady" to bonusReady()))
    }
}
