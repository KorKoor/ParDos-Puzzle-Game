package com.korkoor.pardos

import com.korkoor.pardos.domain.economy.Economy
import com.korkoor.pardos.domain.shop.CoinBoost
import com.korkoor.pardos.domain.shop.GemPacks
import com.korkoor.pardos.domain.shop.ShopCatalog
import com.korkoor.pardos.domain.shop.VipPerks
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MonetizationTest {
    @Test fun biggerPacksAlwaysGiveMoreGemsPerDollar() {
        val packs = GemPacks.packs
        assertEquals(5, packs.size)
        packs.zipWithNext().forEach { (a, b) ->
            assertTrue(b.gems > a.gems, "más gemas absolutas")
            assertTrue(b.gems * a.usdCents > a.gems * b.usdCents, "mejor relación ${a.id}→${b.id}")
        }
        assertEquals(0, GemPacks.bonusPercent(packs.first()))
        assertEquals(packs.map { GemPacks.bonusPercent(it) }.sorted(), packs.map { GemPacks.bonusPercent(it) }, "el extra crece con el pack")
    }

    @Test fun bestValuePackExists() {
        assertTrue(GemPacks.packs.any { it.id == GemPacks.bestValueId })
    }

    @Test fun firstPurchaseDoublesOnlyOnce() {
        val p = ShopCatalog.byId(ShopCatalog.GEMS_SMALL)!!
        assertEquals(p.gems * Economy.FIRST_PURCHASE_MULTIPLIER, GemPacks.gemsForPurchase(p, firstTime = true))
        assertEquals(p.gems, GemPacks.gemsForPurchase(p, firstTime = false))
    }

    @Test fun vipPerks() {
        assertTrue(VipPerks.canClaimDaily(vip = true, lastClaimDay = 10, today = 11))
        assertFalse(VipPerks.canClaimDaily(true, 11, 11), "ya cobrado hoy")
        assertFalse(VipPerks.canClaimDaily(false, 0, 11), "sin VIP")
        assertEquals(100, VipPerks.coinsWithBonus(100, vip = false))
        assertEquals(120, VipPerks.coinsWithBonus(100, vip = true))
        assertEquals(0, VipPerks.coinsWithBonus(0, vip = true))
        assertEquals(2, VipPerks.coinsWithBonus(1, vip = true), "redondea hacia arriba, nunca resta")
    }

    @Test fun coinBoost() {
        assertEquals(100, CoinBoost.apply(100, winsLeft = 0))
        assertEquals(150, CoinBoost.apply(100, winsLeft = 3))
        assertEquals(CoinBoost.WINS, CoinBoost.addWins(0))
        assertEquals(CoinBoost.MAX_WINS, CoinBoost.addWins(CoinBoost.MAX_WINS - 1), "tope de acumulación")
        assertTrue(CoinBoost.canBuy(CoinBoost.PRICE_GEMS, 0))
        assertFalse(CoinBoost.canBuy(CoinBoost.PRICE_GEMS - 1, 0))
        assertFalse(CoinBoost.canBuy(999, CoinBoost.MAX_WINS), "ya está al tope")
    }

    @Test fun boostAndVipStackBelowDouble() {
        // Un jugador VIP con impulso no debe pasar de ~2x las monedas base
        val base = 100
        val total = CoinBoost.apply(VipPerks.coinsWithBonus(base, true), 5)
        assertTrue(total <= base * 2, "total=$total")
    }
}
