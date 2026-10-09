package com.korkoor.pardos

import com.korkoor.pardos.domain.meta.MetaSession
import com.korkoor.pardos.domain.shop.IosStore
import com.korkoor.pardos.domain.shop.ShopCatalog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IosStoreTest {
    @Test fun everyPriceIsAnAppStorePricePoint() {
        (IosStore.gemPacks.map { it.usdCents } + IosStore.specials.map { it.usdCents }).forEach {
            assertTrue(it in IosStore.applePricePoints, "$it no es un escalón de precio de Apple")
        }
    }

    @Test fun biggerPacksAlwaysGiveMoreGemsPerDollar() {
        val packs = IosStore.gemPacks
        assertEquals(packs.sortedBy { it.usdCents }, packs, "van de menos a más caro")
        packs.zipWithNext().forEach { (a, b) ->
            val ra = a.gems.toDouble() / a.usdCents
            val rb = b.gems.toDouble() / b.usdCents
            assertTrue(rb > ra, "${b.id} debe rendir más por dólar que ${a.id}")
        }
    }

    @Test fun idsAreUniqueAndAndroidIdsAreKept() {
        val ids = IosStore.gemPacks.map { it.id } + IosStore.specials.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        listOf(ShopCatalog.GEMS_TINY, ShopCatalog.GEMS_SMALL, ShopCatalog.GEMS_MEDIUM, ShopCatalog.GEMS_LARGE, ShopCatalog.GEMS_HUGE).forEach {
            assertTrue(it in ids, "falta $it (mismo ID que en Google Play)")
        }
    }

    @Test fun priceTextIsPlainDollars() {
        assertEquals("$0.99", IosStore.priceText(99))
        assertEquals("$4.99", IosStore.priceText(499))
        assertEquals("$100.00", IosStore.priceText(10000))
    }

    @Test fun bestValueIsOneOfThePacksAndTheBonusGrows() {
        assertTrue(IosStore.gemPack(IosStore.BEST_VALUE_ID) != null)
        assertEquals(0, IosStore.bonusPercent(IosStore.gemPacks.first()))
        assertTrue(IosStore.bonusPercent(IosStore.gemPacks.last()) > 50)
    }

    @Test fun testPurchasesPayOnceWhereTheyShould() {
        val day = 20_800
        val m = MetaSession()
        m.tick(day, day * 86_400_000L, 600)
        val first = jsonObject(m.testBuyProduct("gems_pocket"))
        assertEquals(540, first.int("gems"), "primera compra de un pack: doble")
        assertEquals(270, jsonObject(m.testBuyProduct("gems_pocket")).int("gems"))
        assertEquals(true, jsonObject(m.testBuyProduct("starter_pack"))["ok"])
        assertEquals(false, jsonObject(m.testBuyProduct("starter_pack"))["ok"])
        assertEquals(true, jsonObject(m.testBuyProduct("vip_forever"))["ok"])
        assertEquals(false, jsonObject(m.testBuyProduct("vip_forever"))["ok"])
        assertEquals(true, jsonObject(m.testBuyProduct("season_pass"))["ok"])
        assertEquals(false, jsonObject(m.testBuyProduct("piggy_break"))["ok"], "la hucha vacía no se rompe")
        assertEquals(false, jsonObject(m.testBuyProduct("nada"))["ok"])
        val store = jsonObject(m.storeProducts())
        assertEquals(7, store.list("packs").size)
        assertEquals(4, store.list("specials").size)
    }
}
