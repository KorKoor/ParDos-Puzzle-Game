"""Parche de una sola vez: serie destacada del dia (logica pura, operacion, cofre, estado y pruebas)."""
import io

BASE = r"C:\Users\carlo\Documents\Android\ParDos-Puzzle-Game\shared\src"
MAIN = BASE + r"\commonMain\kotlin\com\korkoor\pardos\domain"

io.open(MAIN + r"\retention\FeaturedSeries.kt", "w", encoding="utf-8").write('''package com.korkoor.pardos.domain.retention

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
''')

io.open(MAIN + r"\meta\MetaFeatured.kt", "w", encoding="utf-8").write('''package com.korkoor.pardos.domain.meta

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
''')

# CollectionOps: aceptar una carta extra
p = MAIN + r"\meta\MetaCollection.kt"
t = io.open(p, encoding="utf-8").read()
old = "    private fun applyDrops(drops: List<Drop>) {"
assert old in t
t = t.replace(old, "    /** Guarda una carta extra (por ejemplo la de la serie destacada del día). */\n    fun addBonusDrop(drop: Drop) = applyDrops(listOf(drop))\n\n" + old, 1)
io.open(p, "w", encoding="utf-8").write(t)

# fachada
p = MAIN + r"\meta\MetaSession.kt"
t = io.open(p, encoding="utf-8").read()


def rep(old, new):
    global t
    assert old in t, old[:60]
    t = t.replace(old, new, 1)


rep("    private val ads = AdOps(store, wallet, col, ret, clock)\n", "    private val ads = AdOps(store, wallet, col, ret, clock)\n    private val featured = FeaturedOps(store, col, clock)\n")
rep("""        val res = col.openChest(t, Random(seed)) ?: return no("No tienes ese cofre")
        ret.onChestOpened()""", """        val res = col.openChest(t, Random(seed)) ?: return no("No tienes ese cofre")
        val featuredDrop = featured.takeBonus(Random(seed + 17))
        ret.onChestOpened()""")
rep("""        val drops = res.drops.map {
            obj(""", """        val drops = (res.drops + listOfNotNull(featuredDrop)).map {
            obj(""")
rep("""            "happyHour" to happy.json(), "calendar" to calendar.json(),""", """            "happyHour" to happy.json(), "calendar" to calendar.json(), "featured" to featured.json(),""")
io.open(p, "w", encoding="utf-8").write(t)

# pruebas
p = BASE + r"\commonTest\kotlin\com\korkoor\pardos\DailyRetentionTest.kt"
t = io.open(p, encoding="utf-8").read().rstrip()
assert t.endswith("}")
t = t[:-1] + '''
    // ---------------------------------------------------------------- serie destacada

    @Test
    fun featuredSeriesRotatesThroughAllSeriesWithoutRepeating() {
        val seen = (20000 until 20032).map { com.korkoor.pardos.domain.retention.FeaturedSeries.seriesFor(it) }
        // fuera de Noche de brujas: 32 dias seguidos, 32 series distintas
        val day = LoginCalendar.daysFromCivil(2026, 3, 1)
        val spring = (day until day + 32).map { com.korkoor.pardos.domain.retention.FeaturedSeries.seriesFor(it) }
        assertEquals(32, spring.toSet().size)
        assertTrue(seen.isNotEmpty())
    }

    @Test
    fun firstChestOfTheDayBringsAFeaturedExtraCard() {
        val day = LoginCalendar.daysFromCivil(2026, 3, 5)
        val m = session(day)
        val st = m.state()
        assertTrue(st.contains("\\"featured\\":{"), st)
        assertTrue(st.contains("\\"bonusReady\\":true"), st)
        val claim = m.claimFreeChest()
        assertTrue(claim.contains("\\"ok\\":true"), claim)
        val type = field(claim, "chest").trim('"')
        val r = m.openChest(type, 11L)
        assertTrue(r.contains("\\"ok\\":true"), r)
        assertTrue(Regex("\\"bonus\\":true").findAll(r).count() >= 1, "el primer cofre del dia trae la carta destacada: " + r)
        assertTrue(m.state().contains("\\"bonusReady\\":false"))
    }
}
'''
io.open(p, "w", encoding="utf-8").write(t)
print("ok")
