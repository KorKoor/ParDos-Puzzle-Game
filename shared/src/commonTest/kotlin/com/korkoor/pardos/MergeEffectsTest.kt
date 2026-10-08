package com.korkoor.pardos

import com.korkoor.pardos.domain.collection.Rarity
import com.korkoor.pardos.domain.shop.FxSource
import com.korkoor.pardos.domain.shop.MergeFx
import com.korkoor.pardos.domain.shop.MergeFxInventory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MergeEffectsTest {
    @Test fun idsAreUniqueAndLookupWorks() {
        assertEquals(MergeFx.entries.size, MergeFx.entries.map { it.id }.toSet().size)
        MergeFx.entries.forEach { assertEquals(it, MergeFx.fromId(it.id)) }
        assertEquals(MergeFx.DEFAULT, MergeFx.fromId("no-existe"))
        assertEquals(MergeFx.DEFAULT, MergeFx.fromId(null))
    }

    @Test fun onlyClassicIsFree() {
        assertEquals(listOf(MergeFx.CLASSIC), MergeFx.entries.filter { it.isFree })
    }

    @Test fun commonAndRareCostCoinsEpicAndLegendaryCostGems() {
        MergeFx.entries.filter { it.buyable }.forEach {
            when (it.rarity) {
                Rarity.COMMON, Rarity.RARE -> assertTrue(it.coinPrice > 0 && it.gemPrice == 0, it.id)
                Rarity.EPIC, Rarity.LEGENDARY -> assertTrue(it.gemPrice > 0 && it.coinPrice == 0, it.id)
            }
        }
    }

    @Test fun priceGrowsWithRarity() {
        assertTrue(MergeFx.PETALS.coinPrice > MergeFx.SPARKS.coinPrice)
        assertTrue(MergeFx.FIREWORKS.gemPrice > MergeFx.STARS.gemPrice)
    }

    @Test fun catalogHasEnoughVarietyToSell() {
        assertTrue(MergeFx.entries.count { it.buyable } >= 6)
        assertTrue(MergeFx.entries.any { it.source == FxSource.STARTER }, "el pack inicial regala uno")
        assertTrue(MergeFx.entries.any { it.source == FxSource.SEASON }, "el pase regala uno")
    }

    @Test fun buyWithCoins() {
        val r = MergeFxInventory().buy(MergeFx.SPARKS, coins = 1000, gems = 0)
        assertIs<MergeFxInventory.Purchase.Ok>(r)
        assertEquals(1000 - MergeFx.SPARKS.coinPrice, r.coinsLeft)
        assertTrue(MergeFx.SPARKS.id in r.inventory.owned)
        assertIs<MergeFxInventory.Purchase.AlreadyOwned>(r.inventory.buy(MergeFx.SPARKS, 9999, 9999))
    }

    @Test fun notEnoughMoneyAndNotPurchasable() {
        val inv = MergeFxInventory()
        assertIs<MergeFxInventory.Purchase.NotEnoughCoins>(inv.buy(MergeFx.SPARKS, 100, 999))
        assertIs<MergeFxInventory.Purchase.NotEnoughGems>(inv.buy(MergeFx.STARS, 9999, 10))
        assertIs<MergeFxInventory.Purchase.NotPurchasable>(inv.buy(MergeFx.HEARTS, 9999, 9999))
        assertIs<MergeFxInventory.Purchase.NotPurchasable>(inv.buy(MergeFx.LIGHTNING, 9999, 9999))
    }

    @Test fun discountAppliesAndRoundsUp() {
        val r = MergeFxInventory().buy(MergeFx.PETALS, 5000, 0, discountPercent = 30)
        assertIs<MergeFxInventory.Purchase.Ok>(r)
        assertEquals(5000 - 1050, r.coinsLeft)
    }

    @Test fun equipOnlyWhatYouOwn() {
        var inv = MergeFxInventory()
        assertEquals(MergeFx.CLASSIC.id, inv.equip(MergeFx.SPARKS).equipped, "no lo tiene")
        inv = inv.grant(MergeFx.HEARTS)
        assertEquals(MergeFx.HEARTS.id, inv.equip(MergeFx.HEARTS).equipped)
        assertTrue(inv.owns(MergeFx.CLASSIC))
        assertFalse(inv.owns(MergeFx.FIREWORKS))
    }

    @Test fun particleCountStaysInRange() {
        assertEquals(8, MergeFx.particleCount(2))
        assertTrue(MergeFx.particleCount(2048) in 8..26)
        assertTrue(MergeFx.particleCount(64) > MergeFx.particleCount(4))
        assertEquals(26, MergeFx.particleCount(1 shl 30))
    }

    @Test fun seasonPassGivesTheSeasonEffectsOnTheRightTiers() {
        val season = 2026 * 12 + 9
        val free = com.korkoor.pardos.domain.retention.SeasonPass.freeReward(com.korkoor.pardos.domain.retention.SeasonPass.FREE_FX_TIER, season)
        val premium = com.korkoor.pardos.domain.retention.SeasonPass.premiumReward(com.korkoor.pardos.domain.retention.SeasonPass.PREMIUM_FX_TIER, season)
        assertEquals(MergeFx.SPARKS, free.fx)
        assertEquals(MergeFx.LIGHTNING, premium.fx)
        assertFalse(free.isEmpty)
        // el resto de niveles no traen efecto
        val others = (1..30).filter { it != 25 }.count { com.korkoor.pardos.domain.retention.SeasonPass.freeReward(it, season).fx != null }
        assertEquals(0, others)
        assertEquals(MergeFx.entries.count { it.source == FxSource.SEASON }, 1, "solo Rayo es exclusivo del pase")
    }
}
