package com.korkoor.pardos

import com.korkoor.pardos.domain.collection.Rarity
import com.korkoor.pardos.domain.collection.ShardShop
import com.korkoor.pardos.domain.collection.SeriesPackRules
import com.korkoor.pardos.domain.collection.Showcase
import com.korkoor.pardos.domain.collection.TokenRules
import com.korkoor.pardos.domain.economy.Economy
import com.korkoor.pardos.domain.shop.AvatarSource
import com.korkoor.pardos.domain.shop.Avatars
import com.korkoor.pardos.domain.shop.Banners
import com.korkoor.pardos.domain.shop.CoinShop
import com.korkoor.pardos.domain.shop.GemPacks
import com.korkoor.pardos.domain.shop.MergeFx
import com.korkoor.pardos.domain.shop.ShopCatalog
import com.korkoor.pardos.domain.shop.TileSkin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Cuánto "cuesta" de verdad cada cosa: precio dividido entre lo que entra en un día típico ([EconomyModel]).
 * Si alguien sube un premio o baja un precio y el juego se vuelve barato (o imposible), una de estas pruebas se queja.
 * Las cifras y el porqué están en docs/ECONOMIA.md.
 */
class EconomyBalanceTest {
    private val casual = EconomyModel.casual()
    private val ads = EconomyModel.withAds()

    private fun coinDays(price: Int) = price / casual.coins
    private fun gemDays(price: Int, daily: EconomyModel.Daily = casual) = price / daily.gems

    /** Dólares que cuestan [gems] gemas comprándolas en el pack más barato por gema (el más grande) — el mejor caso para el jugador. */
    private fun bestCaseDollars(gems: Int): Double {
        val best = GemPacks.packs.maxByOrNull { it.gems * 1.0 / it.usdCents }!!
        return gems * best.usdCents / 100.0 / best.gems
    }

    // ----------------------------------------------------------------- ingresos

    @Test fun dailyIncomeStaysInTheTargetBand() {
        assertTrue(casual.coins in 650.0..1_000.0, "monedas al día (casual): ${casual.coins}")
        assertTrue(casual.gems in 3.0..8.0, "gemas al día (casual): ${casual.gems}")
        assertTrue(ads.coins <= casual.coins * 1.8, "con anuncios no debe pasar de 1,8x las monedas: ${ads.coins} vs ${casual.coins}")
        assertTrue(ads.gems in 10.0..20.0, "gemas al día con anuncios: ${ads.gems}")
    }

    // ----------------------------------------------------------------- monedas

    @Test fun firstCoinPurchaseIsWithinReachOfTheFirstDays() {
        val cheapest = TileSkin.entries.filter { it.coinPrice > 0 }.minOf { it.coinPrice }
        assertTrue(coinDays(cheapest) in 0.8..1.6, "primera skin: ${coinDays(cheapest)} días")
    }

    @Test fun coinSkinsTakeDaysNotHours() {
        TileSkin.entries.filter { it.coinPrice > 0 }.forEach {
            assertTrue(coinDays(it.coinPrice) in 0.8..6.0, "${it.id}: ${coinDays(it.coinPrice)} días")
        }
        // las más caras (épicas por monedas) nunca bajan de 3 días: son metas, no impulsos
        TileSkin.entries.filter { it.coinPrice > 0 && it.rarity >= com.korkoor.pardos.domain.shop.SkinRarity.EPIC }.forEach {
            assertTrue(coinDays(it.coinPrice) >= 3.0, "${it.id}: ${coinDays(it.coinPrice)} días")
        }
    }

    @Test fun avatarsAndBannersFormALadder() {
        val shop = Avatars.shop.filter { it.source == AvatarSource.SHOP }
        assertTrue(coinDays(shop.minOf { it.coinPrice }) >= 1.0, "el avatar más barato debe costar al menos un día")
        val top = shop.maxOf { it.coinPrice }
        assertTrue(coinDays(top) in 6.0..12.0, "el avatar más caro: ${coinDays(top)} días")
        val topBanner = Banners.shop.maxOf { it.coinPrice }
        assertTrue(coinDays(topBanner) in 2.0..5.0, "el banner más caro por monedas: ${coinDays(topBanner)} días")
    }

    @Test fun chestsAndConsumablesAreWorthTheirPrice() {
        assertTrue(coinDays(Economy.COMMON_CHEST_COINS) in 0.8..1.5, "cofre común: ${coinDays(Economy.COMMON_CHEST_COINS)} días")
        assertTrue(coinDays(Economy.RARE_CHEST_COINS) in 2.5..4.0, "cofre raro: ${coinDays(Economy.RARE_CHEST_COINS)} días")
        assertTrue(coinDays(Economy.STREAK_FREEZE_PRICE_COINS) in 0.6..1.2, "escudo de racha: ${coinDays(Economy.STREAK_FREEZE_PRICE_COINS)} días")
        assertTrue(Economy.UNDO_PRICE_COINS * 5 < casual.coins, "5 deshacer no deben costar más que un día")
        // un cofre raro con monedas debe salir más caro que lo que valen sus cartas vendidas (si no, vender sería un negocio)
        val soldValue = Rarity.entries.sumOf { it.sellCoins } / 4.0 * 3
        assertTrue(Economy.RARE_CHEST_COINS > soldValue * 4, "vender las cartas de un cofre no debe pagar el cofre")
    }

    // ----------------------------------------------------------------- gemas

    @Test fun premiumGemItemsNeedWeeksOfFreePlayOrRealMoney() {
        val epicSkins = TileSkin.entries.filter { it.gemPrice > 0 && it.rarity == com.korkoor.pardos.domain.shop.SkinRarity.EPIC }
        val legendSkins = TileSkin.entries.filter { it.gemPrice > 0 && it.rarity == com.korkoor.pardos.domain.shop.SkinRarity.LEGENDARY }
        epicSkins.forEach {
            assertTrue(gemDays(it.gemPrice) >= 14.0, "${it.id} (jugando sin anuncios): ${gemDays(it.gemPrice)} días")
            assertTrue(gemDays(it.gemPrice, ads) >= 6.0, "${it.id} (con anuncios): ${gemDays(it.gemPrice, ads)} días")
        }
        legendSkins.forEach { assertTrue(gemDays(it.gemPrice, ads) >= 12.0, "${it.id} (con anuncios): ${gemDays(it.gemPrice, ads)} días") }
        assertTrue(gemDays(Economy.RARE_CHEST_GEMS) >= 5.0, "cofre raro con gemas: ${gemDays(Economy.RARE_CHEST_GEMS)} días")
        assertTrue(gemDays(Economy.EPIC_CHEST_GEMS) >= 14.0, "cofre épico con gemas: ${gemDays(Economy.EPIC_CHEST_GEMS)} días")
    }

    @Test fun anyGemCosmeticCostsAtLeastADollarFiftyEvenInTheBiggestPack() {
        val prices = TileSkin.entries.filter { it.gemPrice > 0 }.map { it.gemPrice to it.id } +
            Banners.shop.filter { it.gemPrice > 0 }.map { it.gemPrice to "banner ${it.id}" } +
            MergeFx.entries.filter { it.gemPrice > 0 && it.buyable }.map { it.gemPrice to "fx ${it.id}" }
        prices.forEach { (gems, name) -> assertTrue(bestCaseDollars(gems) >= 1.5, "$name: $gems gemas = \$${bestCaseDollars(gems)}") }
    }

    @Test fun theFirstPurchaseHookDoesNotBuyAWholeEpicSkin() {
        val tiny = ShopCatalog.byId(ShopCatalog.GEMS_TINY)!!
        val firstTiny = GemPacks.gemsForPurchase(tiny, firstTime = true)
        val cheapestEpicGemSkin = TileSkin.entries.filter { it.gemPrice > 0 }.minOf { it.gemPrice }
        assertTrue(firstTiny < cheapestEpicGemSkin, "el primer pack de \$0.99 ($firstTiny gemas) no debe comprar una skin premium ($cheapestEpicGemSkin)")
    }

    @Test fun gemSinksOutweighTheFreeSupply() {
        // la serie de cartas y los atajos de esencia/fichas se pagan con gemas: no deben ser regalo
        assertTrue(SeriesPackRules.GEMS >= 60, "sobre de serie")
        assertTrue(ShardShop.GEMS_PER_PACK >= 35, "esencia")
        assertTrue(TokenRules.GEMS_PER_TOKEN >= 18, "fichas")
        assertEquals(990, (4..9).sumOf { Showcase.unlockCost(it) }, "vitrina completa")
        assertTrue(Economy.REVIVE_PRICE_GEMS in 12..20, "segunda oportunidad")
        assertTrue(Economy.COIN_BOOST_PRICE_GEMS >= 25, "impulso de monedas")
        assertTrue(Economy.SEASON_TIER_SKIP_GEMS >= 50, "saltar nivel del pase")
    }

    @Test fun exchangingGemsNeverBeatsBuyingWithCoins() {
        // 1 gema = 40 monedas: la skin de monedas más cara no puede salir por menos gemas que el precio de un cofre raro
        val dearestCoinSkin = TileSkin.entries.maxOf { it.coinPrice }
        val inGems = dearestCoinSkin / CoinShop.COINS_PER_GEM
        assertTrue(inGems >= Economy.RARE_CHEST_GEMS, "$dearestCoinSkin monedas = $inGems gemas")
    }

    // ----------------------------------------------------------------- informe (para docs/ECONOMIA.md)

    @Test fun printReport() {
        if ((System.getenv("ECO_REPORT") ?: "") != "1") return
        fun f(d: Double) = ((d * 10).toInt() / 10.0).toString()
        println("ECO casual coins/day=${f(casual.coins)} gems/day=${f(casual.gems)}")
        println("ECO ads    coins/day=${f(ads.coins)} gems/day=${f(ads.gems)}")
        println("ECO coinsPerWin=${f(EconomyModel.coinsPerWin())}")
        TileSkin.entries.filter { it.coinPrice > 0 }.sortedBy { it.coinPrice }.forEach { println("ECO skin ${it.id} coins=${it.coinPrice} days=${f(coinDays(it.coinPrice))}") }
        TileSkin.entries.filter { it.gemPrice > 0 }.sortedBy { it.gemPrice }.forEach { println("ECO skin ${it.id} gems=${it.gemPrice} days=${f(gemDays(it.gemPrice))} daysAds=${f(gemDays(it.gemPrice, ads))} usd=${f(bestCaseDollars(it.gemPrice))}") }
        listOf(
            "avatar basic" to Economy.AVATAR_PRICE_BASIC, "avatar legendary" to Economy.AVATAR_PRICE_LEGENDARY,
            "banner basic" to Economy.BANNER_PRICE_BASIC, "banner prime" to Economy.BANNER_PRICE_PRIME,
            "chest common" to Economy.COMMON_CHEST_COINS, "chest rare" to Economy.RARE_CHEST_COINS, "freeze" to Economy.STREAK_FREEZE_PRICE_COINS
        ).forEach { (n, p) -> println("ECO coins $n=$p days=${f(coinDays(p))}") }
        listOf("chest rare" to Economy.RARE_CHEST_GEMS, "chest epic" to Economy.EPIC_CHEST_GEMS, "revive" to Economy.REVIVE_PRICE_GEMS, "boost" to Economy.COIN_BOOST_PRICE_GEMS)
            .forEach { (n, p) -> println("ECO gems $n=$p days=${f(gemDays(p))} daysAds=${f(gemDays(p, ads))}") }
    }
}
