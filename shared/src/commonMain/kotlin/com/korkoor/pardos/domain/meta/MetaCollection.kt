package com.korkoor.pardos.domain.meta

import com.korkoor.pardos.domain.collection.ChestResult
import com.korkoor.pardos.domain.collection.ChestRules
import com.korkoor.pardos.domain.collection.ChestType
import com.korkoor.pardos.domain.collection.Collectible
import com.korkoor.pardos.domain.collection.CollectibleCatalog
import com.korkoor.pardos.domain.collection.Copies
import com.korkoor.pardos.domain.collection.CraftRules
import com.korkoor.pardos.domain.collection.Drop
import com.korkoor.pardos.domain.collection.FoilRules
import com.korkoor.pardos.domain.collection.PerkKind
import com.korkoor.pardos.domain.collection.PerkRules
import com.korkoor.pardos.domain.collection.PityState
import com.korkoor.pardos.domain.collection.Rarity
import com.korkoor.pardos.domain.collection.SellRules
import com.korkoor.pardos.domain.collection.Series
import com.korkoor.pardos.domain.collection.SeriesPackRules
import com.korkoor.pardos.domain.collection.ShardShop
import com.korkoor.pardos.domain.collection.Showcase
import com.korkoor.pardos.domain.collection.TokenRules
import com.korkoor.pardos.domain.shop.Price
import com.korkoor.pardos.domain.shop.TileSkin
import kotlin.random.Random

/** Álbum de coleccionables: copias, esencia, fichas, cofres sin abrir, brillantes, vitrina y premios. Igual que CollectionManager. */
internal class CollectionOps(private val s: MetaStore, private val wallet: Wallet) {

    val copies: Map<String, Int> get() = Copies.decode(s.str("copies"))
    val owned: Set<String> get() = Copies.owned(copies)
    val foil: Set<String> get() = s.strSet("foil")
    val shards: Int get() = s.int("shards")
    val tokens: Int get() = s.int("tokens")
    val showcase: List<String> get() = s.str("showcase").split(',').filter { it.isNotBlank() }
    val showcaseSlots: Int get() = s.int("showcase_slots", Showcase.BASE_SLOTS).coerceIn(Showcase.BASE_SLOTS, Showcase.MAX_SLOTS)
    val claimedSeries: Set<String> get() = s.strSet("series_claimed")
    val albumClaimed: Boolean get() = s.bool("album_claimed")

    private fun saveCopies(c: Map<String, Int>) {
        s.setStr("copies", Copies.encode(c))
        val clean = Showcase.sanitize(showcase, Copies.owned(c), showcaseSlots)
        if (clean != showcase) s.setStr("showcase", clean.joinToString(","))
    }

    // ---------------- cofres ----------------

    fun chestCount(t: ChestType): Int = s.int("chest_${t.name}")
    val totalChests: Int get() = ChestType.entries.sumOf { chestCount(it) }

    fun addChests(t: ChestType, n: Int) { if (n > 0) s.setInt("chest_${t.name}", chestCount(t) + n) }

    fun buyChest(t: ChestType, price: Price): Boolean {
        if (wallet.coins < price.coins || wallet.gems < price.gems) return false
        if (price.coins > 0 && !wallet.spendCoins(price.coins)) return false
        if (price.gems > 0 && !wallet.spendGems(price.gems)) { wallet.addCoins(price.coins); return false }
        addChests(t, 1)
        return true
    }

    private fun pity() = PityState(s.int("pity_epic"), s.int("pity_legendary"))

    /** Abre un cofre del inventario. Las repetidas se guardan como copias. */
    fun openChest(t: ChestType, random: Random): ChestResult? {
        if (chestCount(t) <= 0) return null
        val result = ChestRules.open(t, owned, pity(), random, extraCardChance = PerkRules.extraCardChance(owned, foil))
        applyDrops(result.drops)
        s.setInt("chest_${t.name}", (chestCount(t) - 1).coerceAtLeast(0))
        s.setInt("pity_epic", result.pity.sinceEpic)
        s.setInt("pity_legendary", result.pity.sinceLegendary)
        return result
    }

    private fun applyDrops(drops: List<Drop>) {
        var c = copies
        drops.forEach { c = Copies.add(c, it.collectible.id) }
        saveCopies(c)
    }

    fun openSeriesPack(series: Series, withGems: Boolean, random: Random): List<Drop>? {
        if (!SeriesPackRules.canOpen(series, owned)) return null
        val paid = if (withGems) wallet.spendGems(SeriesPackRules.GEMS) else wallet.spendCoins(SeriesPackRules.COINS)
        if (!paid) return null
        val drops = SeriesPackRules.open(series, owned, random)
        applyDrops(drops)
        return drops
    }

    // ---------------- repetidas ----------------

    fun sell(id: String, qty: Int): Int? {
        val c = CollectibleCatalog.byId(id) ?: return null
        if (!SellRules.canSell(copies, id, qty)) return null
        val coins = SellRules.value(c, PerkRules.tenths(PerkKind.SELL, owned, foil)) * qty
        saveCopies(Copies.add(copies, id, -qty))
        wallet.addCoins(coins)
        return coins
    }

    fun sellAllUpTo(maxRarity: Rarity): Pair<Int, Int> {
        var n = 0
        var total = 0
        SellRules.bulk(copies, maxRarity).forEach { (c, qty) -> sell(c.id, qty)?.let { total += it; n += qty } }
        return n to total
    }

    fun recycle(id: String, qty: Int): Int? {
        val c = CollectibleCatalog.byId(id) ?: return null
        if (!SellRules.canSell(copies, id, qty)) return null
        val gained = SellRules.recycleValue(c) * qty
        saveCopies(Copies.add(copies, id, -qty))
        s.setInt("shards", shards + gained)
        return gained
    }

    fun craft(c: Collectible): Boolean {
        if (!CraftRules.canCraft(c, owned, shards)) return false
        s.setInt("shards", shards - CraftRules.cost(c))
        saveCopies(Copies.add(copies, c.id))
        return true
    }

    fun upgradeFoil(c: Collectible): Boolean {
        if (!FoilRules.canUpgrade(c, owned, foil, shards)) return false
        s.setInt("shards", shards - FoilRules.cost(c))
        s.setStrSet("foil", foil + c.id)
        return true
    }

    fun shardPacksToday(today: Int): Int = if (s.int("shard_day", -1) == today) s.int("shard_count") else 0

    fun buyShardPack(today: Int): Boolean {
        val bought = shardPacksToday(today)
        if (!ShardShop.canBuy(wallet.gems, bought)) return false
        if (!wallet.spendGems(ShardShop.GEMS_PER_PACK)) return false
        s.setInt("shards", shards + ShardShop.SHARDS_PER_PACK)
        s.setInt("shard_day", today)
        s.setInt("shard_count", bought + 1)
        return true
    }

    // ---------------- fichas de intercambio ----------------

    fun addTokens(n: Int) { if (n > 0) s.setInt("tokens", (tokens + n).coerceAtMost(TokenRules.MAX_STOCK)) }

    fun tokensBoughtToday(today: Int): Int = if (s.int("token_day", -1) == today) s.int("token_count") else 0

    fun buyToken(today: Int): Boolean {
        val bought = tokensBoughtToday(today)
        if (!TokenRules.canBuy(wallet.gems, bought, tokens)) return false
        if (!wallet.spendGems(TokenRules.GEMS_PER_TOKEN)) return false
        addTokens(1)
        s.setInt("token_day", today)
        s.setInt("token_count", bought + 1)
        return true
    }

    /** Ficha diaria por entrar (más una extra cuando la mejora de las piezas la reparte). */
    fun onDailyCheckIn(today: Int): Int {
        if (s.int("token_checkin", -1) == today) return 0
        var granted = TokenRules.DAILY_LOGIN
        var acc = s.double("token_acc") + PerkRules.dailyTokenChance(owned, foil)
        if (acc >= 1.0) { granted += 1; acc -= 1.0 }
        s.setInt("token_checkin", today)
        s.setDouble("token_acc", acc)
        addTokens(granted)
        return granted
    }

    // ---------------- vitrina ----------------

    fun setShowcase(ids: List<String>) {
        s.setStr("showcase", Showcase.sanitize(ids, owned, showcaseSlots).joinToString(","))
    }

    fun unlockShowcaseSlot(): Boolean {
        val next = showcaseSlots + 1
        if (next > Showcase.MAX_SLOTS) return false
        if (!wallet.spendGems(Showcase.unlockCost(next))) return false
        s.setInt("showcase_slots", next)
        return true
    }

    // ---------------- series y álbum completos ----------------

    fun isSeriesClaimable(series: Series): Boolean =
        series.id !in claimedSeries && CollectibleCatalog.isSeriesComplete(series, owned)

    fun claimSeries(series: Series): Pair<Int, Int>? {
        if (!isSeriesClaimable(series)) return null
        s.addToStrSet("series_claimed", series.id)
        wallet.addCoins(series.rewardCoins)
        wallet.addGems(series.rewardGems)
        return series.rewardCoins to series.rewardGems
    }

    val isAlbumClaimable: Boolean get() = !albumClaimed && CollectibleCatalog.isAlbumComplete(owned)

    fun claimAlbum(): Boolean {
        if (!isAlbumClaimable) return false
        s.setBool("album_claimed", true)
        wallet.grantSkin(TileSkin.GOLD)
        wallet.addGems(ALBUM_GEMS)
        return true
    }

    companion object {
        const val ALBUM_GEMS = 50
    }
}
