package com.korkoor.pardos

import com.korkoor.pardos.domain.economy.Economy
import com.korkoor.pardos.domain.retention.SeasonPass
import com.korkoor.pardos.domain.shop.BannerSource
import com.korkoor.pardos.domain.shop.Banners
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class BannersTest {
    @Test fun idsAreUniqueAndTheDefaultIsFree() {
        val ids = Banners.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertEquals(BannerSource.FREE, Banners.byId(Banners.DEFAULT_ID).source)
        assertTrue(Banners.isOwned(Banners.DEFAULT_ID, emptySet()))
        assertEquals(Banners.DEFAULT_ID, Banners.byId(9999).id)
    }

    @Test fun shopIsOrderedByPriceAndHasASanePrice() {
        val coins = Banners.shop.filter { it.gemPrice == 0 }.map { it.coinPrice }
        assertEquals(coins, coins.sorted())
        Banners.shop.forEach { assertTrue(it.coinPrice > 0 || it.gemPrice > 0, it.name) }
        assertTrue(Banners.shop.all { it.coinPrice <= Economy.BANNER_PRICE_PRIME })
    }

    @Test fun purchasesFollowTheRules() {
        val cheap = Banners.byId(3)
        assertIs<Banners.Purchase.NotEnoughCoins>(Banners.buy(3, emptySet(), cheap.coinPrice - 1, 999))
        val ok = Banners.buy(3, emptySet(), cheap.coinPrice + 5, 0)
        assertIs<Banners.Purchase.Ok>(ok)
        assertEquals(5, ok.coinsLeft)
        assertIs<Banners.Purchase.AlreadyOwned>(Banners.buy(3, setOf(3), 99_999, 99_999))
        assertIs<Banners.Purchase.AlreadyOwned>(Banners.buy(1, emptySet(), 99_999, 99_999))
        assertIs<Banners.Purchase.NotEnoughGems>(Banners.buy(13, emptySet(), 99_999, 0))
        assertIs<Banners.Purchase.NotPurchasable>(Banners.buy(17, emptySet(), 99_999, 99_999))
        assertIs<Banners.Purchase.NotPurchasable>(Banners.buy(4242, emptySet(), 99_999, 99_999))
    }

    @Test fun seasonBannersRotateAndAreNeverSold() {
        val seen = (0 until 3).flatMap { listOf(Banners.seasonFree(it).id, Banners.seasonPremium(it).id) }.toSet()
        assertEquals(Banners.seasonal.map { it.id }.toSet(), seen)
        for (s in 0 until 9) assertTrue(Banners.seasonFree(s).id != Banners.seasonPremium(s).id)
        assertTrue(Banners.exists(Banners.seasonFree(24321).id))
        assertTrue(Banners.exists(Banners.seasonPremium(-7).id))
    }

    @Test fun seasonPassGivesOneBannerPerTrack() {
        val season = 24321
        val free = (1..SeasonPass.TIERS).filter { SeasonPass.freeReward(it, season).banner != 0 }
        val premium = (1..SeasonPass.TIERS).filter { SeasonPass.premiumReward(it, season).banner != 0 }
        assertEquals(listOf(SeasonPass.FREE_BANNER_TIER), free)
        assertEquals(listOf(SeasonPass.PREMIUM_BANNER_TIER), premium)
        assertEquals(Banners.seasonFree(season).id, SeasonPass.freeReward(SeasonPass.FREE_BANNER_TIER, season).banner)
        assertFalse(SeasonPass.premiumReward(SeasonPass.PREMIUM_BANNER_TIER, season).isEmpty)
    }

    @Test fun darkBannersAreDetected() {
        assertTrue(Banners.byId(12).isDark)
        assertFalse(Banners.byId(1).isDark)
    }
}
