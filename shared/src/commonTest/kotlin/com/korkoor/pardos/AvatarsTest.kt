package com.korkoor.pardos

import com.korkoor.pardos.domain.economy.Economy
import com.korkoor.pardos.domain.retention.SeasonPass
import com.korkoor.pardos.domain.shop.Avatars
import com.korkoor.pardos.domain.shop.AvatarSource
import com.korkoor.pardos.domain.shop.AvatarFrame
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AvatarsTest {
    @Test fun idsAreUniqueAndClassicsAreFree() {
        val ids = Avatars.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertEquals((1..10).toList(), Avatars.classics.map { it.id })
        assertTrue(Avatars.classics.all { it.source == AvatarSource.FREE && it.coinPrice == 0 })
        assertTrue(Avatars.all.all { it.id >= 1 })
    }

    @Test fun everyDrawnAvatarHasAnAnimal() {
        Avatars.all.filter { it.id > Avatars.CLASSIC_COUNT }.forEach { assertTrue(it.animal != null, it.name) }
        Avatars.classics.forEach { assertTrue(it.animal == null) }
    }

    @Test fun shopAvatarsHaveSaneIncreasingPrices() {
        assertTrue(Avatars.shop.size >= 60, "tienda: ${Avatars.shop.size}")
        Avatars.shop.forEach { assertTrue(it.coinPrice in Economy.AVATAR_PRICE_BASIC..Economy.AVATAR_PRICE_LEGENDARY, it.name) }
        // el catálogo viene ordenado de barato a caro
        assertEquals(Avatars.shop.map { it.coinPrice }, Avatars.shop.map { it.coinPrice }.sorted())
        assertTrue(Avatars.shop.first().coinPrice < Avatars.shop.last().coinPrice)
    }

    @Test fun seasonAvatarsAreNeverSold() {
        assertEquals(10, Avatars.seasonal.size)
        Avatars.seasonal.forEach {
            assertEquals(AvatarSource.SEASON, it.source)
            assertEquals(0, it.coinPrice)
            assertIs<Avatars.Purchase.NotPurchasable>(Avatars.buy(it.id, emptySet(), coins = 999_999))
        }
    }

    @Test fun ownershipAndPurchases() {
        assertTrue(Avatars.isOwned(3, emptySet()))
        assertFalse(Avatars.isOwned(11, emptySet()))
        assertTrue(Avatars.isOwned(11, setOf(11)))
        val zorro = Avatars.byId(11)
        assertIs<Avatars.Purchase.NotEnoughCoins>(Avatars.buy(11, emptySet(), zorro.coinPrice - 1))
        val ok = Avatars.buy(11, emptySet(), zorro.coinPrice + 40)
        assertIs<Avatars.Purchase.Ok>(ok)
        assertEquals(40, ok.coinsLeft)
        assertIs<Avatars.Purchase.AlreadyOwned>(Avatars.buy(11, setOf(11), 99_999))
        assertIs<Avatars.Purchase.AlreadyOwned>(Avatars.buy(2, emptySet(), 99_999))
        assertIs<Avatars.Purchase.NotPurchasable>(Avatars.buy(9999, emptySet(), 99_999))
        assertEquals(1, Avatars.byId(9999).id, "id inexistente → avatar por defecto")
    }

    @Test fun seasonRotationCoversAllAvatarsOverFiveMonths() {
        val seen = (0 until 5).flatMap { listOf(Avatars.seasonFree(it).id, Avatars.seasonPremium(it).id) }.toSet()
        assertEquals(Avatars.seasonal.map { it.id }.toSet(), seen)
        // gratis y premium de un mismo mes son distintos, y se repite cada 3 meses
        for (s in 0 until 10) assertTrue(Avatars.seasonFree(s).id != Avatars.seasonPremium(s).id)
        assertEquals(Avatars.seasonFree(1).id, Avatars.seasonFree(6).id)
        // funciona con ids de temporada reales (año*12+mes) y negativos
        assertTrue(Avatars.exists(Avatars.seasonFree(24321).id))
        assertTrue(Avatars.exists(Avatars.seasonPremium(-5).id))
    }

    @Test fun seasonPassGivesOneAvatarPerTrack() {
        val season = 24321
        val freeTiers = (1..SeasonPass.TIERS).filter { SeasonPass.freeReward(it, season).avatar != 0 }
        val premiumTiers = (1..SeasonPass.TIERS).filter { SeasonPass.premiumReward(it, season).avatar != 0 }
        assertEquals((listOf(SeasonPass.FREE_AVATAR_TIER) + SeasonPass.FREE_EXTRA_AVATAR_TIERS).sorted(), freeTiers)
        assertEquals((listOf(SeasonPass.PREMIUM_AVATAR_TIER) + SeasonPass.PREMIUM_EXTRA_AVATAR_TIERS).sorted(), premiumTiers)
        // todos los que reparte una temporada existen, son del pase y no se repiten
        val all = SeasonPass.avatarsOf(season)
        assertEquals(all.size, all.toSet().size)
        all.forEach { assertEquals(AvatarSource.SEASON, Avatars.byId(it).source) }
        assertEquals(Avatars.seasonFree(season).id, SeasonPass.freeReward(SeasonPass.FREE_AVATAR_TIER, season).avatar)
        assertEquals(Avatars.seasonPremium(season).id, SeasonPass.premiumReward(SeasonPass.PREMIUM_AVATAR_TIER, season).avatar)
        assertFalse(SeasonPass.freeReward(SeasonPass.FREE_AVATAR_TIER, season).isEmpty)
    }

    @Test fun framesGrowWithValueAndSeasonAvatarsGetThePrism() {
        assertEquals(com.korkoor.pardos.domain.shop.AvatarFrame.NONE, Avatars.byId(3).frame)
        assertEquals(com.korkoor.pardos.domain.shop.AvatarFrame.PRISM, Avatars.seasonal.first().frame)
        assertTrue(Avatars.byId(37).frame.ordinal > Avatars.byId(30).frame.ordinal)
        val frames = Avatars.shop.map { it.frame.ordinal }
        assertEquals(frames, frames.sorted(), "más caro = marco mejor")
    }

    @Test fun passExtrasRotateOverThreeMonthsAndNeverRepeatInAMonth() {
        val seen = (0 until 3).flatMap { s -> (0 until 5).map { Avatars.seasonExtra(s, it).id } }.toSet()
        assertEquals(Avatars.passExtras.map { it.id }.toSet(), seen)
        for (s in 0 until 9) assertEquals(5, (0 until 5).map { Avatars.seasonExtra(s, it).id }.toSet().size)
        assertTrue(Avatars.exists(Avatars.seasonExtra(24321, 4).id))
        assertTrue(Avatars.exists(Avatars.seasonExtra(-11, 0).id))
        Avatars.passExtras.forEach { assertIs<Avatars.Purchase.NotPurchasable>(Avatars.buy(it.id, emptySet(), 999_999)) }
    }

    @Test fun prestigeAvatarsComeFromRankAndPlatinum() {
        val ranks = com.korkoor.pardos.domain.prestige.PrestigeRank.entries
        assertEquals(0, Avatars.prestigeUnlocked(ranks.first(), platinum = false).size)
        assertEquals(1, Avatars.prestigeUnlocked(com.korkoor.pardos.domain.prestige.PrestigeRank.APPRENTICE, platinum = false).size)
        assertEquals(7, Avatars.prestigeUnlocked(ranks.last(), platinum = false).size)
        assertEquals(8, Avatars.prestigeUnlocked(ranks.last(), platinum = true).size)
        Avatars.prestige.forEach {
            assertEquals(AvatarSource.PRESTIGE, it.source)
            assertEquals(AvatarFrame.MYTHIC, it.frame)
            assertIs<Avatars.Purchase.NotPurchasable>(Avatars.buy(it.id, emptySet(), 999_999))
        }
    }

    @Test fun everyAvatarIsDrawableAndTheCatalogIsBig() {
        assertTrue(Avatars.all.size >= 100, "avatares: ${Avatars.all.size}")
        // dos avatares nunca son idénticos a la vista
        val looks = Avatars.all.filter { it.animal != null }.map { listOf(it.animal, it.accessory, it.variant, it.scene) }
        val dup = Avatars.all.filter { it.animal != null }.groupBy { listOf(it.animal, it.accessory, it.variant, it.scene) }.values.filter { it.size > 1 }
        assertEquals(emptyList(), dup.map { g -> g.map { it.name } }, "avatares repetidos")
    }
}
