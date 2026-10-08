package com.korkoor.pardos

import com.korkoor.pardos.domain.collection.AlbumBonus
import com.korkoor.pardos.domain.collection.CollectibleCatalog
import com.korkoor.pardos.domain.collection.CraftRules
import com.korkoor.pardos.domain.collection.FoilRules
import com.korkoor.pardos.domain.collection.Rarity
import com.korkoor.pardos.domain.collection.ShardShop
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FoilAndShardsTest {
    private val all = CollectibleCatalog.all.map { it.id }.toSet()

    @Test fun foilCostsDoubleTheCraftCostAndGrowsWithRarity() {
        Rarity.entries.forEach { r ->
            val c = CollectibleCatalog.ofRarity(r).first()
            assertEquals(CraftRules.cost(c) * 2, FoilRules.cost(c))
        }
        assertTrue(FoilRules.cost(CollectibleCatalog.ofRarity(Rarity.LEGENDARY).first()) > FoilRules.cost(CollectibleCatalog.ofRarity(Rarity.COMMON).first()))
    }

    @Test fun onlyOwnedNotYetFoilPiecesWithEnoughEssenceCanUpgrade() {
        val c = CollectibleCatalog.ofRarity(Rarity.COMMON).first()
        val cost = FoilRules.cost(c)
        assertTrue(FoilRules.canUpgrade(c, setOf(c.id), emptySet(), cost))
        assertFalse(FoilRules.canUpgrade(c, setOf(c.id), emptySet(), cost - 1), "falta esencia")
        assertFalse(FoilRules.canUpgrade(c, emptySet(), emptySet(), 9999), "no la tienes")
        assertFalse(FoilRules.canUpgrade(c, setOf(c.id), setOf(c.id), 9999), "ya es brillante")
    }

    @Test fun foilBonusIsOnePercentPerSixPieces() {
        assertEquals(0, FoilRules.coinPercent(emptySet()))
        assertEquals(0, FoilRules.coinPercent(all.take(5).toSet()))
        assertEquals(1, FoilRules.coinPercent(all.take(6).toSet()))
        assertEquals(12, FoilRules.coinPercent(all))
        assertEquals(0, FoilRules.coinPercent((1..30).map { "zz$it" }.toSet()), "ids desconocidos no cuentan")
    }

    @Test fun totalAlbumBonusStaysUnderTheCap() {
        assertEquals(AlbumBonus.MAX_PERCENT, AlbumBonus.coinPercent(all, all))
        assertTrue(AlbumBonus.MAX_PERCENT <= 45)
        assertTrue(AlbumBonus.coinPercent(all) < AlbumBonus.MAX_PERCENT, "sin brillantes no llega al tope: hay algo que perseguir")
    }

    @Test fun shardShopHasADailyLimit() {
        assertTrue(ShardShop.canBuy(ShardShop.GEMS_PER_PACK, 0))
        assertFalse(ShardShop.canBuy(ShardShop.GEMS_PER_PACK - 1, 0))
        assertFalse(ShardShop.canBuy(9999, ShardShop.MAX_PACKS_PER_DAY))
    }

    @Test fun gemsToShardsRateIsReasonable() {
        // 25 gemas = 100 esencia: crear una legendaria (600) sale en ~150 gemas
        val legendary = CollectibleCatalog.ofRarity(Rarity.LEGENDARY).first()
        val gemsForLegendary = CraftRules.cost(legendary) * ShardShop.GEMS_PER_PACK / ShardShop.SHARDS_PER_PACK
        assertTrue(gemsForLegendary in 100..200, "gemas=$gemsForLegendary")
    }
}
