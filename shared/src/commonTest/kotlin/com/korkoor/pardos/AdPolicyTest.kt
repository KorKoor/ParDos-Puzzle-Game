package com.korkoor.pardos

import com.korkoor.pardos.domain.shop.AdPolicy
import com.korkoor.pardos.domain.shop.AdRewards
import com.korkoor.pardos.domain.shop.ContinueOffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdPolicyTest {
    private val ready = AdPolicy.State(
        vip = false, campaignLevel = 20, winsSinceLastAd = 3, msSinceLastAd = 600_000, msSinceLastRewarded = 900_000,
        shownToday = 0, installAgeMs = 3_600_000, afterWin = true
    )

    @Test fun showsWhenEverythingIsFine() = assertTrue(AdPolicy.shouldShowInterstitial(ready))

    @Test fun neverForVipNeverAfterALossAndNeverInTheFirstLevels() {
        assertFalse(AdPolicy.shouldShowInterstitial(ready.copy(vip = true)))
        assertFalse(AdPolicy.shouldShowInterstitial(ready.copy(afterWin = false)))
        assertFalse(AdPolicy.shouldShowInterstitial(ready.copy(campaignLevel = AdPolicy.MIN_CAMPAIGN_LEVEL - 1)))
        assertTrue(AdPolicy.shouldShowInterstitial(ready.copy(campaignLevel = AdPolicy.MIN_CAMPAIGN_LEVEL)))
    }

    @Test fun neverOnTopOfACelebration() {
        assertFalse(AdPolicy.shouldShowInterstitial(ready.copy(bigMoment = true)))
        assertTrue(AdPolicy.shouldShowInterstitial(ready.copy(bigMoment = false)))
    }

    @Test fun newInstallsAreLeftAlone() {
        assertFalse(AdPolicy.shouldShowInterstitial(ready.copy(installAgeMs = AdPolicy.MIN_INSTALL_AGE_MS - 1)))
        assertTrue(AdPolicy.shouldShowInterstitial(ready.copy(installAgeMs = AdPolicy.MIN_INSTALL_AGE_MS)))
    }

    @Test fun spacedByWinsTimeAndADailyCap() {
        assertFalse(AdPolicy.shouldShowInterstitial(ready.copy(winsSinceLastAd = AdPolicy.WINS_BETWEEN - 1)))
        assertFalse(AdPolicy.shouldShowInterstitial(ready.copy(msSinceLastAd = AdPolicy.MIN_GAP_MS - 1)))
        assertFalse(AdPolicy.shouldShowInterstitial(ready.copy(shownToday = AdPolicy.MAX_PER_DAY)))
        assertTrue(AdPolicy.shouldShowInterstitial(ready.copy(shownToday = AdPolicy.MAX_PER_DAY - 1)))
    }

    @Test fun aRewardedAdTheyAskedForBuysThemAQuietPeriod() {
        assertFalse(AdPolicy.shouldShowInterstitial(ready.copy(msSinceLastRewarded = 30_000)))
        assertTrue(AdPolicy.shouldShowInterstitial(ready.copy(msSinceLastRewarded = AdPolicy.AFTER_REWARDED_MS)))
    }

    @Test fun rewardsHaveDailyCapsThatCountDown() {
        assertEquals(3, AdRewards.left(0, AdRewards.FREE_GEMS_PER_DAY))
        assertEquals(0, AdRewards.left(5, AdRewards.FREE_GEMS_PER_DAY))
        assertTrue(AdRewards.FREE_GEMS * AdRewards.FREE_GEMS_PER_DAY <= 20, "las gemas gratis diarias no regalan el juego")
    }

    @Test fun anUndoForAnAdIsLimitedPerLevel() {
        assertTrue(AdRewards.UNDO_ADS_PER_LEVEL in 1..3, "poco, para que no se pueda repetir cada jugada a voluntad")
    }

    @Test fun continuingCostsGemsAsAnAlternativeToTheAd() {
        assertTrue(ContinueOffer.canPayWithGems(ContinueOffer.GEMS))
        assertFalse(ContinueOffer.canPayWithGems(ContinueOffer.GEMS - 1))
        assertEquals(1, ContinueOffer.missing(ContinueOffer.GEMS - 1))
        assertEquals(0, ContinueOffer.missing(ContinueOffer.GEMS + 50))
    }
}
