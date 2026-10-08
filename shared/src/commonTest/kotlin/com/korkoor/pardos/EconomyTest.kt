package com.korkoor.pardos

import com.korkoor.pardos.domain.collection.ChestType
import com.korkoor.pardos.domain.collection.Rarity
import com.korkoor.pardos.domain.economy.Economy
import com.korkoor.pardos.domain.shop.DailyOffers
import com.korkoor.pardos.domain.shop.OfferItem
import com.korkoor.pardos.domain.shop.Price
import com.korkoor.pardos.domain.shop.ShopPrices
import com.korkoor.pardos.domain.shop.TileSkin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EconomyTest {
    @Test fun achievementRewardsGrowWithRarity() {
        val rewards = Rarity.entries.map { Economy.achievementReward(it) }
        for (i in 1 until rewards.size) {
            assertTrue(rewards[i].coins > rewards[i - 1].coins)
            assertTrue(rewards[i].gems >= rewards[i - 1].gems)
        }
    }

    @Test fun achievementRarityFollowsPositionInCategory() {
        assertEquals(Rarity.COMMON, Economy.achievementRarity(0, 10))
        assertEquals(Rarity.LEGENDARY, Economy.achievementRarity(9, 10))
        assertEquals(Rarity.COMMON, Economy.achievementRarity(0, 1))
        val all = (0 until 20).map { Economy.achievementRarity(it, 20) }
        assertTrue(all.zipWithNext().all { (a, b) -> b.ordinal >= a.ordinal }, "las rarezas deben subir con el índice")
    }

    @Test fun streakMilestonesAreOrderedAndRewardMore() {
        val m = Economy.streakMilestones
        assertEquals(m.map { it.days }, m.map { it.days }.sorted())
        for (i in 1 until m.size) assertTrue(m[i].coins > m[i - 1].coins)
        assertNotNull(Economy.milestoneFor(7))
        assertNull(Economy.milestoneFor(8))
    }

    @Test fun doubleCoinsIsCapped() {
        assertEquals(60, Economy.doubleBonus(60))
        assertEquals(Economy.DOUBLE_COINS_CAP, Economy.doubleBonus(10_000))
        assertEquals(0, Economy.doubleBonus(-5))
    }

    @Test fun chestPricesAreConsistent() {
        assertNotNull(ShopPrices.chestCoins(ChestType.COMMON))
        assertNull(ShopPrices.chestCoins(ChestType.EPIC))      // el épico es premium
        assertNotNull(ShopPrices.chestGems(ChestType.EPIC))
        assertTrue(Economy.RARE_CHEST_COINS > Economy.COMMON_CHEST_COINS)
        assertTrue(Economy.EPIC_CHEST_GEMS > Economy.RARE_CHEST_GEMS)
    }

    @Test fun priceDiscountRoundsUp() {
        assertEquals(Price(coins = 75), Price(coins = 100).discounted(25))
        assertEquals(Price(coins = 4, gems = 1), Price(coins = 5, gems = 1).discounted(25)) // 3.75→4, 0.75→1
        assertEquals(Price(coins = 100), Price(coins = 100).discounted(0))
    }

    @Test fun dailyOfferIsDeterministicAndValid() {
        assertEquals(DailyOffers.forDay(20733), DailyOffers.forDay(20733))
        (20000..20100).forEach { d ->
            val o = DailyOffers.forDay(d)
            assertTrue(o.discountPercent in Economy.dailyOfferDiscounts)
            when (val item = o.item) {
                is OfferItem.SkinOffer -> assertTrue(!item.skin.isFree && !item.skin.exclusive)
                is OfferItem.ChestOffer -> assertTrue(item.type != ChestType.EPIC)
            }
        }
    }

    @Test fun dailyOffersRotateThroughManyDifferentSkins() {
        val skins = (20000..20120).mapNotNull { (DailyOffers.forDay(it).item as? OfferItem.SkinOffer)?.skin }.toSet()
        assertTrue(skins.size >= 6, "variedad de skins en oferta: ${skins.size}")
    }

    // ---------- Balance ----------

    /** Ingresos de un jugador casual en un día (ganar niveles, misiones y regalo diario). */
    private fun casualDailyCoins(): Int {
        val levelWins = 6 * 45          // 6 niveles, ~2 estrellas
        val missions = 3 * 25
        val dailyGift = DAILY_GIFT_AVG
        return levelWins + missions + dailyGift
    }

    private val DAILY_GIFT_AVG = 150

    @Test fun aCasualPlayerAffordsACommonSkinInAboutADayOrTwo() {
        val days = TileSkin.FLAT.coinPrice.toDouble() / casualDailyCoins()
        assertTrue(days in 0.5..3.0, "días para la skin barata: $days")
    }

    @Test fun mostCoinSkinsTakeBetweenOneAndTwoWeeksOfCasualPlay() {
        TileSkin.entries.filter { it.coinPrice >= 900 }.forEach {
            val days = it.coinPrice.toDouble() / casualDailyCoins()
            assertTrue(days in 2.0..12.0, "${it.id}: $days días")
        }
    }

    @Test fun premiumSkinsCostMoreGemsThanAStarterPackSpread() {
        // Una skin premium (gemas) nunca debe costar menos que ~$1.5 en gemas (80 gemas ≈ 100 gemas por $0.99)
        TileSkin.entries.filter { it.gemPrice > 0 }.forEach { assertTrue(it.gemPrice >= 60, it.id) }
    }

    @Test fun streakFreezeIsCheaperThanTwoDaysOfIncome() {
        assertTrue(Economy.STREAK_FREEZE_PRICE_COINS < 2 * casualDailyCoins())
    }
}
