package com.korkoor.pardos

import com.korkoor.pardos.domain.shop.ColorRamp
import com.korkoor.pardos.domain.shop.SkinInventory
import com.korkoor.pardos.domain.shop.TileSkin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TileSkinsTest {
    @Test fun idsAreUniqueAndDefaultIsFree() {
        val ids = TileSkin.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(TileSkin.DEFAULT.isFree)
        assertEquals(TileSkin.DEFAULT, TileSkin.fromId("no-existe"))
        assertEquals(TileSkin.WOOD, TileSkin.fromId("wood"))
    }

    @Test fun thereAreAtLeastTwelveSkins() {
        assertTrue(TileSkin.entries.size >= 12)
    }

    @Test fun everyPaletteHasTwelveDistinctToneSteps() {
        TileSkin.entries.forEach { skin ->
            val p = skin.style.tilePalette ?: return@forEach
            assertEquals(12, p.size, skin.id)
            // Las fichas no deben verse todas iguales: tonos 2 y 4096+ distintos
            assertNotEquals(p.first(), p.last(), skin.id)
        }
    }

    @Test fun skinsThatChangeTheThemeDefineAllThemeColors() {
        TileSkin.entries.filter { it.style.changesTheme }.forEach {
            val s = it.style
            assertTrue(s.ink != null && s.accent != null && s.surface != null, "${it.id} le faltan colores de tema")
        }
    }

    @Test fun exclusiveSkinsAreNeverPurchasable() {
        val gold = TileSkin.GOLD
        assertTrue(gold.exclusive)
        assertIs<SkinInventory.Purchase.NotPurchasable>(SkinInventory().buy(gold, coins = 999_999, gems = 999_999))
        // pero se puede conceder y equipar
        val granted = SkinInventory().grant(gold)
        assertTrue(granted.owns(gold))
        assertEquals("gold", granted.equip(gold).equipped)
    }

    @Test fun freeSkinIsAlwaysOwned() {
        assertTrue(SkinInventory(owned = emptySet()).owns(TileSkin.JELLY))
    }

    @Test fun buyingSpendsCoinsAndUnlocks() {
        val r = SkinInventory().buy(TileSkin.FLAT, coins = TileSkin.FLAT.coinPrice + 50, gems = 0)
        assertIs<SkinInventory.Purchase.Ok>(r)
        assertEquals(50, r.coinsLeft)
        assertTrue(r.inventory.owns(TileSkin.FLAT))
    }

    @Test fun discountReducesTheCost() {
        val price = TileSkin.WOOD.coinPrice
        val r = SkinInventory().buy(TileSkin.WOOD, coins = price, gems = 0, discountPercent = 25)
        assertIs<SkinInventory.Purchase.Ok>(r)
        assertEquals(price - kotlin.math.ceil(price * 0.75).toInt(), r.coinsLeft)
    }

    @Test fun cannotBuyWithoutEnoughFunds() {
        assertIs<SkinInventory.Purchase.NotEnoughCoins>(SkinInventory().buy(TileSkin.WOOD, coins = TileSkin.WOOD.coinPrice - 1, gems = 99))
        assertIs<SkinInventory.Purchase.NotEnoughGems>(SkinInventory().buy(TileSkin.NEON, coins = 99_999, gems = TileSkin.NEON.gemPrice - 1))
    }

    @Test fun cannotBuyTwice() {
        val owned = SkinInventory(owned = setOf("jelly", "glass"))
        assertIs<SkinInventory.Purchase.AlreadyOwned>(owned.buy(TileSkin.GLASS, coins = 99_999, gems = 0))
    }

    @Test fun equipOnlyOwnedSkins() {
        val inv = SkinInventory()
        assertEquals("jelly", inv.equip(TileSkin.WOOD).equipped)
        val withWood = SkinInventory(owned = setOf("jelly", "wood"))
        assertEquals("wood", withWood.equip(TileSkin.WOOD).equipped)
    }

    @Test fun colorRampInterpolatesEndpointsAndMiddle() {
        val r = ColorRamp.ramp(3, 0xFF000000, 0xFFFFFFFF)
        assertEquals(0xFF000000, r[0])
        assertEquals(0xFFFFFFFF, r[2])
        assertEquals(0xFF7F7F7F, r[1])
    }

    @Test fun colorRampWithManyAnchorsHitsEveryAnchor() {
        val r = ColorRamp.ramp(5, 0xFFFF0000, 0xFF00FF00, 0xFF0000FF)
        assertEquals(0xFFFF0000, r[0])
        assertEquals(0xFF00FF00, r[2])
        assertEquals(0xFF0000FF, r[4])
    }
}
