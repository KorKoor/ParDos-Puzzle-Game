package com.korkoor.pardos

import com.korkoor.pardos.domain.shop.MenuPromo
import com.korkoor.pardos.domain.shop.PromoKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MenuPromoTest {
    private val fresh = MenuPromo.State(vip = false, starterClaimed = false, campaignLevel = 1, freeGemsLeft = 3, visit = 0)

    @Test fun theDailyOfferIsNeverSomethingTheyAlreadyOwn() {
        val allSkins = com.korkoor.pardos.domain.shop.TileSkin.entries.map { it.id }.toSet()
        for (day in 0 until 120) {
            val plain = com.korkoor.pardos.domain.shop.DailyOffers.forDay(day)
            val owned = (plain.item as? com.korkoor.pardos.domain.shop.OfferItem.SkinOffer)?.let { setOf(it.skin.id) } ?: emptySet()
            val avoided = com.korkoor.pardos.domain.shop.DailyOffers.forDayAvoiding(day, owned)
            val item = avoided.item
            if (item is com.korkoor.pardos.domain.shop.OfferItem.SkinOffer) assertFalse(item.skin.id in owned, "día $day")
            // misma oferta todo el día y mismo descuento que la normal
            assertEquals(avoided, com.korkoor.pardos.domain.shop.DailyOffers.forDayAvoiding(day, owned))
            assertEquals(plain.discountPercent, avoided.discountPercent)
            // sin tocar nada, queda igual que antes (iPhone sigue usando forDay)
            if (owned.isEmpty()) assertEquals(plain, avoided)
        }
        // con todas las skins en su poder, la oferta pasa a ser un cofre
        val everything = com.korkoor.pardos.domain.shop.DailyOffers.forDayAvoiding(1, allSkins)
        assertTrue(everything.item is com.korkoor.pardos.domain.shop.OfferItem.ChestOffer)
    }

    @Test fun aNewPlayerOnlySeesHarmlessThings() {
        val e = MenuPromo.eligible(fresh)
        assertFalse(PromoKind.STARTER_PACK in e)
        assertFalse(PromoKind.VIP in e)
        assertTrue(PromoKind.DAILY_OFFER in e && PromoKind.FREE_GEMS in e)
    }

    @Test fun theStarterPackAppearsOnceTheyKnowTheGameAndNeverAfterBuyingIt() {
        assertTrue(PromoKind.STARTER_PACK in MenuPromo.eligible(fresh.copy(campaignLevel = MenuPromo.STARTER_MIN_LEVEL)))
        assertFalse(PromoKind.STARTER_PACK in MenuPromo.eligible(fresh.copy(campaignLevel = 40, starterClaimed = true)))
    }

    @Test fun vipPlayersNeverSeeVipOrAdOffers() {
        val e = MenuPromo.eligible(fresh.copy(vip = true, campaignLevel = 60, starterClaimed = true))
        assertEquals(listOf(PromoKind.DAILY_OFFER), e)
    }

    @Test fun freeGemsDisappearWhenTheDayIsUsedUp() {
        assertFalse(PromoKind.FREE_GEMS in MenuPromo.eligible(fresh.copy(freeGemsLeft = 0)))
    }

    @Test fun theCardRotatesBetweenVisitsAndCoversEveryOption() {
        val s = fresh.copy(campaignLevel = 30)
        val seen = (0 until 8).map { MenuPromo.pick(s.copy(visit = it)) }.toSet()
        assertEquals(MenuPromo.eligible(s).toSet(), seen)
        assertEquals(MenuPromo.pick(s.copy(visit = 0)), MenuPromo.pick(s.copy(visit = MenuPromo.eligible(s).size)))
    }
}
