package com.korkoor.pardos

import com.korkoor.pardos.domain.shop.SkinInventory
import com.korkoor.pardos.domain.shop.TileSkin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TileSkinsTest {
    @Test fun idsAreUniqueAndDefaultIsFree() {
        val ids = TileSkin.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(TileSkin.DEFAULT.isFree)
        assertEquals(TileSkin.DEFAULT, TileSkin.fromId("no-existe"))
        assertEquals(TileSkin.WOOD, TileSkin.fromId("wood"))
    }

    @Test fun freeSkinIsAlwaysOwned() {
        assertTrue(SkinInventory(owned = emptySet()).owns(TileSkin.JELLY))
    }

    @Test fun buyingSpendsCoinsAndUnlocks() {
        val r = SkinInventory().buy(TileSkin.FLAT, coins = 200, gems = 0)
        assertIs<SkinInventory.Purchase.Ok>(r)
        assertEquals(50, r.coinsLeft)
        assertTrue(r.inventory.owns(TileSkin.FLAT))
    }

    @Test fun cannotBuyWithoutEnoughFunds() {
        assertIs<SkinInventory.Purchase.NotEnoughCoins>(SkinInventory().buy(TileSkin.WOOD, coins = 299, gems = 99))
        assertIs<SkinInventory.Purchase.NotEnoughGems>(SkinInventory().buy(TileSkin.NEON, coins = 9999, gems = 7))
    }

    @Test fun cannotBuyTwice() {
        val owned = SkinInventory(owned = setOf("jelly", "glass"))
        assertIs<SkinInventory.Purchase.AlreadyOwned>(owned.buy(TileSkin.GLASS, coins = 9999, gems = 0))
    }

    @Test fun equipOnlyOwnedSkins() {
        val inv = SkinInventory()
        assertEquals("jelly", inv.equip(TileSkin.WOOD).equipped)
        val withWood = SkinInventory(owned = setOf("jelly", "wood"))
        assertEquals("wood", withWood.equip(TileSkin.WOOD).equipped)
    }
}
