package com.korkoor.pardos

import com.korkoor.pardos.domain.shop.CoinShop
import com.korkoor.pardos.domain.shop.ProductKind
import com.korkoor.pardos.domain.shop.ShopCatalog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ShopCatalogTest {
    @Test fun productIdsAreUnique() {
        val ids = ShopCatalog.products.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test fun biggerGemPacksGiveBetterValue() {
        val packs = ShopCatalog.products.filter { it.kind == ProductKind.GEMS }.sortedBy { it.gems }
        assertTrue(packs.size >= 3)
        assertTrue(packs.all { it.gems > 0 })
    }

    @Test fun lookupById() {
        assertNotNull(ShopCatalog.byId(ShopCatalog.VIP_FOREVER))
        assertEquals(null, ShopCatalog.byId("nope"))
    }

    @Test fun streakFreezeRules() {
        assertTrue(CoinShop.canBuyStreakFreeze(coins = 200, owned = 0))
        assertFalse(CoinShop.canBuyStreakFreeze(coins = 199, owned = 0))
        assertFalse(CoinShop.canBuyStreakFreeze(coins = 9999, owned = CoinShop.MAX_STREAK_FREEZES))
    }

    @Test fun gemExchange() {
        assertEquals(200, CoinShop.coinsForGems(5))
        assertEquals(0, CoinShop.coinsForGems(-3))
    }
}
