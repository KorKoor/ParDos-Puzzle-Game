package com.korkoor.pardos

import com.korkoor.pardos.domain.collection.ChestRules
import com.korkoor.pardos.domain.collection.ChestType
import com.korkoor.pardos.domain.collection.CollectibleCatalog
import com.korkoor.pardos.domain.collection.CraftRules
import com.korkoor.pardos.domain.collection.PityState
import com.korkoor.pardos.domain.collection.Rarity
import com.korkoor.pardos.domain.collection.Series
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CollectionTest {
    @Test fun catalogHas32SeriesOfTenWithFixedRarityShape() {
        assertEquals(32, Series.entries.size)
        assertEquals(320, CollectibleCatalog.all.size)
        assertEquals(320, CollectibleCatalog.all.map { it.id }.toSet().size)
        Series.entries.forEach { s ->
            val items = CollectibleCatalog.inSeries(s)
            assertEquals(10, items.size, s.id)
            assertEquals(4, items.count { it.rarity == Rarity.COMMON })
            assertEquals(3, items.count { it.rarity == Rarity.RARE })
            assertEquals(2, items.count { it.rarity == Rarity.EPIC })
            assertEquals(1, items.count { it.rarity == Rarity.LEGENDARY })
        }
    }

    @Test fun rarerCardsAreWorthMoreAndCostMoreToCraft() {
        val r = Rarity.entries
        for (i in 1 until r.size) {
            assertTrue(r[i].shardValue > r[i - 1].shardValue)
            assertTrue(r[i].craftCost > r[i - 1].craftCost)
        }
    }

    @Test fun chestsDropTheRightNumberOfCards() {
        ChestType.entries.forEach { t ->
            val res = ChestRules.open(t, emptySet(), PityState(), Random(3))
            assertEquals(t.cards, res.drops.size)
        }
    }

    @Test fun noRepeatedCardInsideTheSameChest() {
        repeat(300) { seed ->
            val ids = ChestRules.open(ChestType.EPIC, emptySet(), PityState(), Random(seed)).drops.map { it.collectible.id }
            assertEquals(ids.size, ids.toSet().size)
        }
    }

    @Test fun rareChestAlwaysHasARareOrBetter() {
        repeat(500) { seed ->
            val res = ChestRules.open(ChestType.RARE, emptySet(), PityState(), Random(seed))
            assertTrue(res.drops.any { it.collectible.rarity.ordinal >= Rarity.RARE.ordinal }, "seed $seed")
        }
    }

    @Test fun epicChestAlwaysHasAnEpicOrBetter() {
        repeat(500) { seed ->
            val res = ChestRules.open(ChestType.EPIC, emptySet(), PityState(), Random(seed))
            assertTrue(res.drops.any { it.collectible.rarity.ordinal >= Rarity.EPIC.ordinal }, "seed $seed")
        }
    }

    @Test fun pityForcesAnEpicEvenFromACommonChest() {
        val almost = PityState(sinceEpic = ChestRules.PITY_EPIC - 1)
        repeat(300) { seed ->
            val res = ChestRules.open(ChestType.COMMON, emptySet(), almost, Random(seed))
            assertTrue(res.drops.any { it.collectible.rarity.ordinal >= Rarity.EPIC.ordinal }, "seed $seed")
            assertEquals(0, res.pity.sinceEpic)
        }
    }

    @Test fun legendaryPityForcesALegendary() {
        val almost = PityState(sinceLegendary = ChestRules.PITY_LEGENDARY - 1)
        repeat(200) { seed ->
            val res = ChestRules.open(ChestType.COMMON, emptySet(), almost, Random(seed))
            assertTrue(res.drops.any { it.collectible.rarity == Rarity.LEGENDARY }, "seed $seed")
            assertEquals(0, res.pity.sinceLegendary)
        }
    }

    @Test fun pityCountersGrowWithoutGoodDrops() {
        // Cofre común con semilla donde no cae épica: los contadores suben en 1
        var found = false
        for (seed in 0 until 200) {
            val res = ChestRules.open(ChestType.COMMON, emptySet(), PityState(2, 3), Random(seed))
            if (res.drops.none { it.collectible.rarity.ordinal >= Rarity.EPIC.ordinal }) {
                assertEquals(3, res.pity.sinceEpic)
                assertEquals(4, res.pity.sinceLegendary)
                found = true
                break
            }
        }
        assertTrue(found)
    }

    @Test fun duplicatesTurnIntoShardsAndNewOnesDoNot() {
        val ownedAll = CollectibleCatalog.all.map { it.id }.toSet()
        val dup = ChestRules.open(ChestType.RARE, ownedAll, PityState(), Random(1))
        assertTrue(dup.drops.all { !it.isNew && it.shards == it.collectible.rarity.shardValue })

        val fresh = ChestRules.open(ChestType.RARE, emptySet(), PityState(), Random(1))
        assertTrue(fresh.drops.all { it.isNew && it.shards == 0 })
    }

    @Test fun openingChestsEventuallyCompletesTheAlbum() {
        // Con garantías y preferencia por piezas nuevas, el álbum de 320 se completa en unos cientos de cofres
        val rng = Random(2026)
        val owned = mutableSetOf<String>()
        var pity = PityState()
        var chests = 0
        while (!CollectibleCatalog.isAlbumComplete(owned) && chests < 3000) {
            val res = ChestRules.open(ChestType.RARE, owned, pity, rng)
            res.drops.forEach { owned += it.collectible.id }
            pity = res.pity
            chests++
        }
        assertTrue(CollectibleCatalog.isAlbumComplete(owned), "no se completó tras $chests cofres")
        assertTrue(chests in 100..1500, "cofres necesarios: $chests")
    }

    @Test fun craftingRules() {
        val c = CollectibleCatalog.all.first { it.rarity == Rarity.RARE }
        assertFalse(CraftRules.canCraft(c, emptySet(), shards = CraftRules.cost(c) - 1))
        assertTrue(CraftRules.canCraft(c, emptySet(), shards = CraftRules.cost(c)))
        assertFalse(CraftRules.canCraft(c, setOf(c.id), shards = 9999)) // ya la tiene
    }

    @Test fun seriesProgressAndCompletion() {
        val garden = CollectibleCatalog.inSeries(Series.GARDEN).map { it.id }
        assertEquals(3 to 10, CollectibleCatalog.progress(Series.GARDEN, garden.take(3).toSet()))
        assertTrue(CollectibleCatalog.isSeriesComplete(Series.GARDEN, garden.toSet()))
        assertFalse(CollectibleCatalog.isSeriesComplete(Series.SKY, garden.toSet()))
    }
}
