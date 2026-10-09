package com.korkoor.pardos

import com.korkoor.pardos.domain.collection.AlbumHints
import com.korkoor.pardos.domain.collection.ChestRules
import com.korkoor.pardos.domain.collection.ChestType
import com.korkoor.pardos.domain.collection.CollectibleCatalog
import com.korkoor.pardos.domain.collection.Copies
import com.korkoor.pardos.domain.collection.PerkKind
import com.korkoor.pardos.domain.collection.PerkRules
import com.korkoor.pardos.domain.collection.PityState
import com.korkoor.pardos.domain.collection.Rarity
import com.korkoor.pardos.domain.collection.SellRules
import com.korkoor.pardos.domain.collection.Series
import com.korkoor.pardos.domain.collection.SeriesPackRules
import com.korkoor.pardos.domain.collection.Showcase
import com.korkoor.pardos.domain.collection.TokenRules
import com.korkoor.pardos.domain.collection.TradeOffer
import com.korkoor.pardos.domain.collection.TradeRules
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AlbumEconomyTest {
    private val all = CollectibleCatalog.all

    // ---------- Contenido ----------
    @Test
    fun anExtraCardForAnAdIsASingleBonusRollOfTheSameChest() {
        val d = ChestRules.extraCard(ChestType.COMMON, emptySet(), emptySet(), Random(3))
        assertTrue(d.bonus && d.isNew)
        // con todo el álbum, la extra es repetida y vale su esencia
        val all = com.korkoor.pardos.domain.collection.CollectibleCatalog.all.map { it.id }.toSet()
        val dup = ChestRules.extraCard(ChestType.EPIC, all, emptySet(), Random(3))
        assertTrue(!dup.isNew && dup.shards == dup.collectible.rarity.shardValue)
        // las probabilidades son las del cofre: el común casi nunca da legendarias
        val legendaries = (0 until 2000).count { ChestRules.extraCard(ChestType.COMMON, emptySet(), emptySet(), Random(it)).collectible.rarity == com.korkoor.pardos.domain.collection.Rarity.LEGENDARY }
        assertTrue(legendaries < 20, "legendarias en 2000 cofres comunes: $legendaries")
    }

    @Test fun everyPieceIsUniqueAndFullyDescribed() {
        assertEquals(320, all.size)
        val dupGlyphs = all.groupBy { it.glyph }.filterValues { it.size > 1 }.mapValues { e -> e.value.map { it.id } }
        assertEquals(emptyMap(), dupGlyphs, "emoji repetidos")
        val dupNames = all.groupBy { it.nameEs }.filterValues { it.size > 1 }.mapValues { e -> e.value.map { it.id } }
        assertEquals(emptyMap(), dupNames, "nombres repetidos")
        all.forEach {
            assertTrue(it.nameEs.isNotBlank() && it.nameEn.isNotBlank(), it.id)
            assertTrue(it.descEs.length in 12..120, "${it.id}: descripcion ${it.descEs.length}")
            assertTrue(it.glyph.isNotBlank() && !it.glyph.contains(' ') && it.glyph.length in 1..14, "${it.id}: emoji raro '${it.glyph}'")
        }
        Series.entries.forEach { assertTrue(it.glyph.isNotBlank() && it.top != it.bottom, it.id) }
    }

    @Test fun rewardsGrowWithTheSeriesSize() {
        Series.entries.forEach { assertTrue(it.rewardCoins >= 400 && it.rewardGems >= 5, it.id) }
        assertTrue(Series.entries.sumOf { it.rewardCoins } in 15_000..30_000)
    }

    // ---------- Copias ----------
    @Test fun copiesKeepTheFirstOneAndExposeTheSpares() {
        var c = Copies.fromOwnedSet(setOf("garden_1", "sky_2"))
        assertEquals(0, Copies.spare(c, "garden_1"))
        c = Copies.add(c, "garden_1", 2)
        assertEquals(3, Copies.count(c, "garden_1"))
        assertEquals(2, Copies.spare(c, "garden_1"))
        assertEquals(mapOf("garden_1" to 2), Copies.spares(c))
        assertEquals(2, Copies.totalSpares(c))
        assertEquals(setOf("garden_1", "sky_2"), Copies.owned(c))
        assertEquals(c, Copies.decode(Copies.encode(c)))
        assertEquals(emptyMap(), Copies.decode(null))
        assertEquals(emptyMap(), Copies.decode("garbage,x:y,a:0"))
    }

    // ---------- Venta ----------
    @Test fun sellingNeverTouchesTheFirstCopyAndPaysByRarity() {
        val c = mapOf("garden_1" to 1, "garden_8" to 3)
        assertFalse(SellRules.canSell(c, "garden_1"))
        assertTrue(SellRules.canSell(c, "garden_8", 2))
        assertFalse(SellRules.canSell(c, "garden_8", 3))
        val vals = Rarity.entries.map { r -> SellRules.value(CollectibleCatalog.ofRarity(r).first()) }
        assertEquals(vals, vals.sorted())
        val leg = CollectibleCatalog.byId("garden_8")!!
        assertEquals(leg.rarity.sellCoins, SellRules.value(leg))
        assertEquals((leg.rarity.sellCoins * 1.2).toInt(), SellRules.value(leg, sellTenths = 200))
        assertEquals(1, SellRules.bulk(c, Rarity.LEGENDARY).size)
        assertEquals(0, SellRules.bulk(c, Rarity.EPIC).size)
    }

    // ---------- Mejoras ----------
    @Test fun onlyEpicAndLegendaryPiecesGivePerks() {
        all.forEach { assertEquals(it.rarity.ordinal >= Rarity.EPIC.ordinal, it.perk != null, it.id) }
        val g = CollectibleCatalog.inSeries(Series.GARDEN)
        assertEquals(PerkKind.COINS, g.first { it.rarity == Rarity.EPIC }.perk!!.kind)
        assertEquals(g.first { it.rarity == Rarity.EPIC }.perk!!.tenths * 2, g.first { it.rarity == Rarity.LEGENDARY }.perk!!.tenths)
    }

    @Test fun perksAddUpFoilDoublesThemAndCapsHold() {
        val epic = CollectibleCatalog.inSeries(Series.GARDEN).first { it.rarity == Rarity.EPIC }
        val one = setOf(epic.id)
        assertEquals(epic.perk!!.tenths, PerkRules.tenths(PerkKind.COINS, one))
        assertEquals(epic.perk!!.tenths * 2, PerkRules.tenths(PerkKind.COINS, one, one))
        val everything = all.map { it.id }.toSet()
        PerkKind.entries.forEach { assertTrue(PerkRules.tenths(it, everything, everything) <= PerkRules.cap(it)) }
        assertEquals(1.0, PerkRules.xpMultiplier(emptySet()), 1e-9)
        assertTrue(PerkRules.xpMultiplier(everything) in 1.05..1.25)
        assertTrue(PerkRules.extraCardChance(everything) in 0.05..0.25)
        assertTrue(PerkRules.dailyTokenChance(everything) in 0.2..0.5)
        assertEquals(6, Series.entries.count { it.perk == PerkKind.LUCK })
        PerkKind.entries.forEach { k -> assertTrue(Series.entries.count { it.perk == k } >= 6, k.name) }
    }

    // ---------- Cofres ----------
    @Test fun luckyChestsCanGiveAnExtraCard() {
        val res = ChestRules.open(ChestType.COMMON, emptySet(), PityState(), Random(1), extraCardChance = 1.0)
        assertEquals(ChestType.COMMON.cards + 1, res.drops.size)
        assertTrue(res.drops.last().bonus)
        assertEquals(1, res.drops.count { it.bonus })
        val plain = ChestRules.open(ChestType.COMMON, emptySet(), PityState(), Random(1))
        assertEquals(ChestType.COMMON.cards, plain.drops.size)
        assertTrue(plain.drops.none { it.bonus })
    }

    @Test fun repeatsAreCommonLateInTheAlbum() {
        val owned = all.map { it.id }.toSet()
        val dups = (0 until 200).sumOf { s -> ChestRules.open(ChestType.RARE, owned, PityState(), Random(s)).drops.count { !it.isNew } }
        assertEquals(600, dups, "con el álbum lleno todo son repetidas")
        val mid = all.take(160).map { it.id }.toSet()
        val some = (0 until 400).sumOf { s -> ChestRules.open(ChestType.RARE, mid, PityState(), Random(s)).drops.count { !it.isNew } }
        assertTrue(some in 100..900, "repetidas a medio álbum: $some")
    }

    // ---------- Intercambio ----------
    @Test fun proposalsNeedASpareCopyTheSameRarityAndTokens() {
        val copies = mapOf("garden_1" to 2, "sky_1" to 1, "tea_1" to 2, "garden_5" to 2)
        val ok = TradeOffer("garden_1", "sky_1")
        assertEquals(TradeRules.Check.Ok, TradeRules.canPropose(copies, 1, ok))
        assertEquals(TradeRules.Check.NotEnoughTokens, TradeRules.canPropose(copies, 0, ok))
        assertEquals(TradeRules.Check.NoSpareToGive, TradeRules.canPropose(copies, 5, TradeOffer("sky_1", "garden_1")))
        assertEquals(TradeRules.Check.SameRarityOnly, TradeRules.canPropose(copies, 5, TradeOffer("garden_1", "garden_5")))
        assertEquals(TradeRules.Check.SamePiece, TradeRules.canPropose(copies, 5, TradeOffer("garden_1", "garden_1")))
        assertEquals(TradeRules.Check.Unknown, TradeRules.canPropose(copies, 5, TradeOffer("nope", "sky_1")))
        assertEquals(TradeRules.Check.TooManyPending, TradeRules.canPropose(copies, 5, ok, pendingOut = TradeRules.MAX_PENDING_OUT))
        assertEquals(1, TradeRules.cost(Rarity.COMMON))
        assertTrue(TradeRules.cost(Rarity.LEGENDARY) > TradeRules.cost(Rarity.EPIC))
    }

    @Test fun recipientMustHaveASpareOfWhatIsWanted() {
        val offer = TradeOffer("garden_1", "sky_1")
        assertTrue(TradeRules.canAccept(mapOf("sky_1" to 2), offer))
        assertFalse(TradeRules.canAccept(mapOf("sky_1" to 1), offer))
        assertFalse(TradeRules.canAccept(emptyMap(), offer))
    }

    @Test fun suggestionsOnlyHelpBothSides() {
        val mine = mapOf("garden_1" to 3, "garden_2" to 2, "sky_1" to 1, "tea_8" to 2)
        val theirSpares = setOf("sky_2", "tea_1", "garden_8", "sky_1")
        val theirOwned = setOf("sky_2", "tea_1", "garden_8", "sky_1", "garden_2")
        val s = TradeRules.suggestions(mine, theirSpares, theirOwned)
        assertTrue(s.isNotEmpty())
        s.forEach {
            assertEquals(it.give.rarity, it.want.rarity)
            assertTrue(it.want.id in theirSpares && it.want.id !in Copies.owned(mine))
            assertTrue(Copies.spare(mine, it.give.id) > 0 && it.give.id !in theirOwned)
        }
        // la legendaria que te falta (garden_8) se ofrece primero... pero solo si puedes dar otra legendaria: tea_8
        assertEquals("garden_8", s.first().want.id)
        assertEquals("tea_8", s.first().give.id)
        assertEquals(s.size, s.map { it.give.id }.toSet().size, "no se ofrece dos veces la misma pieza")
    }

    @Test fun tokensHaveAPriceAndAStockLimit() {
        assertTrue(TokenRules.canBuy(TokenRules.GEMS_PER_TOKEN, 0, 0))
        assertFalse(TokenRules.canBuy(TokenRules.GEMS_PER_TOKEN - 1, 0, 0))
        assertFalse(TokenRules.canBuy(999, TokenRules.MAX_BOUGHT_PER_DAY, 0))
        assertFalse(TokenRules.canBuy(999, 0, TokenRules.MAX_STOCK))
    }

    // ---------- Sobre de serie ----------
    @Test fun seriesPacksAlwaysBringSomethingNewWhileSomethingIsMissing() {
        val series = Series.TEA
        val items = CollectibleCatalog.inSeries(series)
        for (seed in 0 until 300) {
            val owned = items.take(1 + seed % 8).map { it.id }.toSet()
            val drops = SeriesPackRules.open(series, owned, Random(seed))
            assertEquals(SeriesPackRules.CARDS, drops.size)
            assertTrue(drops.all { it.collectible.series == series })
            assertTrue(drops.any { it.isNew }, "seed $seed")
            assertEquals(drops.size, drops.map { it.collectible.id }.toSet().size)
        }
        assertFalse(SeriesPackRules.canOpen(series, items.map { it.id }.toSet()))
        assertTrue(SeriesPackRules.canOpen(series, emptySet()))
    }

    // ---------- Vitrina y pistas ----------
    @Test fun showcaseOnlyKeepsOwnedUniquePiecesWithinTheSlots() {
        val owned = setOf("garden_1", "garden_2", "sky_1", "tea_1", "tea_2")
        val got = Showcase.sanitize(listOf("garden_1", "garden_1", "zzz", "sky_1", "tea_1", "tea_2", "garden_2", "sky_2"), owned, Showcase.BASE_SLOTS)
        assertEquals(listOf("garden_1", "sky_1", "tea_1"), got)
        assertEquals(5, Showcase.sanitize(listOf("garden_1", "garden_2", "sky_1", "tea_1", "tea_2"), owned, 5).size)
        assertEquals(0, Showcase.unlockCost(2))
        val costs = (4..Showcase.MAX_SLOTS).map { Showcase.unlockCost(it) }
        assertEquals(costs, costs.sorted())
        assertTrue(costs.all { it > 0 })
    }

    @Test fun hintsPointToTheSeriesClosestToCompletion() {
        val sky = CollectibleCatalog.inSeries(Series.SKY).map { it.id }
        val tea = CollectibleCatalog.inSeries(Series.TEA).map { it.id }
        val owned = (sky.take(9) + tea.take(4) + "garden_1").toSet()
        val hints = AlbumHints.closest(owned)
        assertEquals(Series.SKY, hints.first().series)
        assertEquals(1, hints.first().missing.size)
        assertEquals(10, hints.first().total)
        assertTrue(hints.map { it.series }.containsAll(listOf(Series.TEA, Series.GARDEN)))
        assertNull(hints.firstOrNull { it.series == Series.FARM }, "series sin ninguna pieza no son pista")
        assertNotNull(hints.firstOrNull())
    }
}

class AlbumBitsTest {
    @Test fun roundTripKeepsExactlyTheSamePieces() {
        val some = com.korkoor.pardos.domain.collection.CollectibleCatalog.all.filterIndexed { i, _ -> i % 3 == 0 || i == 319 }.map { it.id }.toSet()
        val enc = com.korkoor.pardos.domain.collection.AlbumBits.encode(some)
        assertEquals(80, enc.length, "320 piezas = 80 letras hex")
        assertEquals(some, com.korkoor.pardos.domain.collection.AlbumBits.decode(enc))
        assertEquals(emptySet(), com.korkoor.pardos.domain.collection.AlbumBits.decode(""))
        assertEquals(emptySet(), com.korkoor.pardos.domain.collection.AlbumBits.decode(null))
        assertEquals(emptySet(), com.korkoor.pardos.domain.collection.AlbumBits.decode("zzzz"))
        val all = com.korkoor.pardos.domain.collection.CollectibleCatalog.all.map { it.id }.toSet()
        assertEquals(all, com.korkoor.pardos.domain.collection.AlbumBits.decode(com.korkoor.pardos.domain.collection.AlbumBits.encode(all)))
    }
}
