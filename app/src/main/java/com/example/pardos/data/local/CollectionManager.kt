package com.korkoor.pardos.data.local

import android.content.Context
import android.content.SharedPreferences
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
import com.korkoor.pardos.domain.collection.SellRules
import com.korkoor.pardos.domain.collection.Series
import com.korkoor.pardos.domain.collection.SeriesPackRules
import com.korkoor.pardos.domain.collection.ShardShop
import com.korkoor.pardos.domain.collection.Showcase
import com.korkoor.pardos.domain.collection.TokenRules
import com.korkoor.pardos.domain.shop.Price
import com.korkoor.pardos.domain.shop.TileSkin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

/**
 * Álbum de coleccionables: copias de cada pieza, esencia, fichas de intercambio, cofres sin abrir, vitrina y recompensas.
 * Los StateFlow son compartidos entre instancias para que la UI siempre vea el valor actual.
 */
class CollectionManager(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = appContext.getSharedPreferences("pardos_collection", Context.MODE_PRIVATE)
    private val economy = EconomyManager(appContext)

    init {
        if (!loaded) {
            val legacy = prefs.getStringSet(KEY_OWNED, emptySet())?.toSet() ?: emptySet()
            var copies = Copies.decode(prefs.getString(KEY_COPIES, null))
            // Migración: la lista antigua ("la tienes") pasa a una copia por pieza
            legacy.filter { CollectibleCatalog.byId(it) != null && (copies[it] ?: 0) < 1 }.forEach { copies = copies + (it to 1) }
            _copies.value = copies
            _owned.value = Copies.owned(copies)
            _shards.value = prefs.getInt(KEY_SHARDS, 0)
            _tokens.value = prefs.getInt(KEY_TOKENS, 0)
            _chests.value = ChestType.entries.associateWith { prefs.getInt("chest_${it.name}", 0) }
            _claimedSeries.value = prefs.getStringSet(KEY_CLAIMED_SERIES, emptySet())?.toSet() ?: emptySet()
            _albumClaimed.value = prefs.getBoolean(KEY_ALBUM_CLAIMED, false)
            _foil.value = prefs.getStringSet(KEY_FOIL, emptySet())?.toSet() ?: emptySet()
            _showcase.value = (prefs.getString(KEY_SHOWCASE, "") ?: "").split(',').filter { it.isNotBlank() }
            _showcaseSlots.value = prefs.getInt(KEY_SHOWCASE_SLOTS, Showcase.BASE_SLOTS).coerceIn(Showcase.BASE_SLOTS, Showcase.MAX_SLOTS)
            loaded = true
        }
    }

    /** Copias de cada pieza (1 = solo la del álbum; más = repetidas). */
    val copies: StateFlow<Map<String, Int>> = _copies.asStateFlow()
    /** Piezas que tienes (al menos una copia). */
    val owned: StateFlow<Set<String>> = _owned.asStateFlow()
    val shards: StateFlow<Int> = _shards.asStateFlow()
    /** Fichas de intercambio. */
    val tokens: StateFlow<Int> = _tokens.asStateFlow()
    val chests: StateFlow<Map<ChestType, Int>> = _chests.asStateFlow()
    val claimedSeries: StateFlow<Set<String>> = _claimedSeries.asStateFlow()
    val albumClaimed: StateFlow<Boolean> = _albumClaimed.asStateFlow()
    /** Piezas convertidas en Brillantes. */
    val foil: StateFlow<Set<String>> = _foil.asStateFlow()
    /** Piezas exhibidas en tu perfil (en orden). */
    val showcase: StateFlow<List<String>> = _showcase.asStateFlow()
    val showcaseSlots: StateFlow<Int> = _showcaseSlots.asStateFlow()

    private fun pity() = PityState(prefs.getInt(KEY_PITY_EPIC, 0), prefs.getInt(KEY_PITY_LEGENDARY, 0))

    private fun saveCopies(copies: Map<String, Int>) {
        _copies.value = copies
        _owned.value = Copies.owned(copies)
        // Se mantiene la lista antigua por si se vuelve a una versión anterior
        prefs.edit().putString(KEY_COPIES, Copies.encode(copies)).putStringSet(KEY_OWNED, _owned.value).apply()
        // Si ya no tienes una pieza exhibida, se quita de la vitrina
        val clean = Showcase.sanitize(_showcase.value, _owned.value, _showcaseSlots.value)
        if (clean != _showcase.value) setShowcase(clean)
        // tu perfil dice qué cartas tienes y cuáles te sobran (para que tus amigos sepan qué cambiar)
        ProfileManager(appContext).requestSync()
    }

    // ============================ Cofres ============================

    fun addChests(type: ChestType, amount: Int) {
        if (amount <= 0) return
        val updated = _chests.value.toMutableMap().also { it[type] = (it[type] ?: 0) + amount }
        _chests.value = updated
        prefs.edit().putInt("chest_${type.name}", updated[type] ?: 0).apply()
    }

    fun chestCount(type: ChestType): Int = _chests.value[type] ?: 0
    val totalChests: Int get() = _chests.value.values.sum()

    /** Compra un cofre y lo deja en el inventario (sin abrirlo). Devuelve false si no alcanza el saldo. */
    fun buyChest(type: ChestType, price: Price): Boolean {
        if (economy.coins.value < price.coins || economy.gems.value < price.gems) return false
        if (price.coins > 0 && !economy.spendCoins(price.coins)) return false
        if (price.gems > 0 && !economy.spendGems(price.gems)) {
            economy.addCoins(price.coins) // revierte si falló la segunda parte
            return false
        }
        addChests(type, 1)
        return true
    }

    /** Abre un cofre del inventario. Las repetidas se guardan como copias. */
    fun openChest(type: ChestType): ChestResult? {
        if (chestCount(type) <= 0) return null
        val result = ChestRules.open(
            type, _owned.value, pity(), Random.Default,
            extraCardChance = PerkRules.extraCardChance(_owned.value, _foil.value)
        )
        applyDrops(result.drops)
        val remaining = (chestCount(type) - 1).coerceAtLeast(0)
        _chests.value = _chests.value.toMutableMap().also { it[type] = remaining }
        prefs.edit()
            .putInt("chest_${type.name}", remaining)
            .putInt(KEY_PITY_EPIC, result.pity.sinceEpic)
            .putInt(KEY_PITY_LEGENDARY, result.pity.sinceLegendary)
            .apply()
        return result
    }

    private fun applyDrops(drops: List<Drop>) {
        var c = _copies.value
        drops.forEach { c = Copies.add(c, it.collectible.id) }
        saveCopies(c)
    }

    // ============================ Sobres de serie ============================

    /** Sobre de una serie: 3 cartas de esa serie, con al menos una nueva mientras falte alguna. */
    fun openSeriesPack(series: Series, withGems: Boolean): List<Drop>? {
        if (!SeriesPackRules.canOpen(series, _owned.value)) return null
        val paid = if (withGems) economy.spendGems(SeriesPackRules.GEMS) else economy.spendCoins(SeriesPackRules.COINS)
        if (!paid) return null
        val drops = SeriesPackRules.open(series, _owned.value)
        applyDrops(drops)
        return drops
    }

    // ============================ Repetidas: vender, reciclar ============================

    fun spare(id: String): Int = Copies.spare(_copies.value, id)

    /** Vende copias repetidas por monedas (con la mejora de venta). Devuelve las monedas, o null si no se puede. */
    fun sell(id: String, qty: Int = 1): Int? {
        val c = CollectibleCatalog.byId(id) ?: return null
        if (!SellRules.canSell(_copies.value, id, qty)) return null
        val coins = SellRules.value(c, PerkRules.tenths(PerkKind.SELL, _owned.value, _foil.value)) * qty
        saveCopies(Copies.add(_copies.value, id, -qty))
        economy.addCoins(coins)
        return coins
    }

    /** Vende de golpe todas las repetidas hasta cierta rareza. Devuelve (copias vendidas, monedas). */
    fun sellAllUpTo(maxRarity: com.korkoor.pardos.domain.collection.Rarity): Pair<Int, Int> {
        var n = 0
        var total = 0
        SellRules.bulk(_copies.value, maxRarity).forEach { (c, qty) ->
            sell(c.id, qty)?.let { total += it; n += qty }
        }
        return n to total
    }

    /** Recicla copias repetidas en esencia. */
    fun recycle(id: String, qty: Int = 1): Int? {
        val c = CollectibleCatalog.byId(id) ?: return null
        if (!SellRules.canSell(_copies.value, id, qty)) return null
        val shards = SellRules.recycleValue(c) * qty
        saveCopies(Copies.add(_copies.value, id, -qty))
        _shards.value = _shards.value + shards
        prefs.edit().putInt(KEY_SHARDS, _shards.value).apply()
        return shards
    }

    // ============================ Crear y mejorar ============================

    /** Crea una pieza que te falta gastando esencia. */
    fun craft(c: Collectible): Boolean {
        if (!CraftRules.canCraft(c, _owned.value, _shards.value)) return false
        val newShards = _shards.value - CraftRules.cost(c)
        _shards.value = newShards
        prefs.edit().putInt(KEY_SHARDS, newShards).apply()
        saveCopies(Copies.add(_copies.value, c.id))
        return true
    }

    /** Convierte una pieza en Brillante gastando esencia. */
    fun upgradeFoil(c: Collectible): Boolean {
        if (!FoilRules.canUpgrade(c, _owned.value, _foil.value, _shards.value)) return false
        val newFoil = _foil.value + c.id
        val newShards = _shards.value - FoilRules.cost(c)
        _foil.value = newFoil
        _shards.value = newShards
        prefs.edit().putStringSet(KEY_FOIL, newFoil).putInt(KEY_SHARDS, newShards).apply()
        return true
    }

    /** Packs de esencia comprados hoy (tope diario). */
    fun shardPacksToday(today: Int = LocalDay.today()): Int =
        if (prefs.getInt(KEY_SHARD_DAY, -1) == today) prefs.getInt(KEY_SHARD_COUNT, 0) else 0

    /** Cambia gemas por un pack de esencia. Devuelve false si no alcanza o se llegó al tope del día. */
    fun buyShardPack(today: Int = LocalDay.today()): Boolean {
        val bought = shardPacksToday(today)
        if (!ShardShop.canBuy(economy.gems.value, bought)) return false
        if (!economy.spendGems(ShardShop.GEMS_PER_PACK)) return false
        val newShards = _shards.value + ShardShop.SHARDS_PER_PACK
        _shards.value = newShards
        prefs.edit().putInt(KEY_SHARDS, newShards).putInt(KEY_SHARD_DAY, today).putInt(KEY_SHARD_COUNT, bought + 1).apply()
        return true
    }

    // ============================ Fichas de intercambio ============================

    fun addTokens(n: Int) {
        if (n <= 0) return
        _tokens.value = (_tokens.value + n).coerceAtMost(TokenRules.MAX_STOCK)
        prefs.edit().putInt(KEY_TOKENS, _tokens.value).apply()
    }

    /** Gasta fichas (false si no alcanzan). */
    fun spendTokens(n: Int): Boolean {
        if (n <= 0) return true
        if (_tokens.value < n) return false
        _tokens.value -= n
        prefs.edit().putInt(KEY_TOKENS, _tokens.value).apply()
        return true
    }

    fun tokensBoughtToday(today: Int = LocalDay.today()): Int =
        if (prefs.getInt(KEY_TOKEN_DAY, -1) == today) prefs.getInt(KEY_TOKEN_COUNT, 0) else 0

    /** Cambia gemas por una ficha (tope diario). */
    fun buyToken(today: Int = LocalDay.today()): Boolean {
        val bought = tokensBoughtToday(today)
        if (!TokenRules.canBuy(economy.gems.value, bought, _tokens.value)) return false
        if (!economy.spendGems(TokenRules.GEMS_PER_TOKEN)) return false
        addTokens(1)
        prefs.edit().putInt(KEY_TOKEN_DAY, today).putInt(KEY_TOKEN_COUNT, bought + 1).apply()
        return true
    }

    /**
     * Ficha de cada día: 1 por entrar, más una extra cuando la mejora de las piezas la reparte (se acumula la probabilidad
     * y al llegar a 100 % cae una ficha, así la suerte es justa). Devuelve las fichas entregadas.
     */
    fun onDailyCheckIn(today: Int = LocalDay.today()): Int {
        if (prefs.getInt(KEY_TOKEN_CHECKIN, -1) == today) return 0
        var granted = TokenRules.DAILY_LOGIN
        val acc = prefs.getFloat(KEY_TOKEN_ACC, 0f) + PerkRules.dailyTokenChance(_owned.value, _foil.value).toFloat()
        var left = acc
        if (left >= 1f) { granted += 1; left -= 1f }
        prefs.edit().putInt(KEY_TOKEN_CHECKIN, today).putFloat(KEY_TOKEN_ACC, left).apply()
        addTokens(granted)
        return granted
    }

    // ============================ Intercambios: entregar y recibir piezas ============================

    /** Aparta una copia repetida para una propuesta (se devuelve si se rechaza o caduca). */
    fun escrow(id: String): Boolean {
        if (Copies.spare(_copies.value, id) < 1) return false
        saveCopies(Copies.add(_copies.value, id, -1))
        return true
    }

    /** Recibe una pieza (intercambio aceptado o devolución de una propuesta). */
    fun receive(id: String) {
        if (CollectibleCatalog.byId(id) == null) return
        saveCopies(Copies.add(_copies.value, id))
    }

    // ============================ Vitrina del perfil ============================

    fun setShowcase(ids: List<String>) {
        val clean = Showcase.sanitize(ids, _owned.value, _showcaseSlots.value)
        _showcase.value = clean
        prefs.edit().putString(KEY_SHOWCASE, clean.joinToString(",")).apply()
        // el perfil se vuelve a publicar para que tus amigos lo vean
        ProfileManager(appContext).requestSync()
    }

    /** Abre el siguiente hueco de vitrina con gemas. */
    fun unlockShowcaseSlot(): Boolean {
        val next = _showcaseSlots.value + 1
        if (next > Showcase.MAX_SLOTS) return false
        if (!economy.spendGems(Showcase.unlockCost(next))) return false
        _showcaseSlots.value = next
        prefs.edit().putInt(KEY_SHOWCASE_SLOTS, next).apply()
        return true
    }

    // ============================ Recompensas de series y del álbum ============================

    fun isSeriesClaimable(s: Series): Boolean =
        s.id !in _claimedSeries.value && CollectibleCatalog.isSeriesComplete(s, _owned.value)

    /** Entrega la recompensa de una serie completa (una sola vez). Devuelve (monedas, gemas). */
    fun claimSeries(s: Series): Pair<Int, Int>? {
        if (!isSeriesClaimable(s)) return null
        val updated = _claimedSeries.value + s.id
        _claimedSeries.value = updated
        prefs.edit().putStringSet(KEY_CLAIMED_SERIES, updated).apply()
        economy.addCoins(s.rewardCoins)
        economy.addGems(s.rewardGems)
        return s.rewardCoins to s.rewardGems
    }

    val isAlbumClaimable: Boolean
        get() = !_albumClaimed.value && CollectibleCatalog.isAlbumComplete(_owned.value)

    /** Álbum completo: skin exclusiva Oro Real + gemas. */
    fun claimAlbum(): Boolean {
        if (!isAlbumClaimable) return false
        _albumClaimed.value = true
        prefs.edit().putBoolean(KEY_ALBUM_CLAIMED, true).apply()
        economy.grantSkin(TileSkin.GOLD)
        economy.addGems(ALBUM_GEMS)
        return true
    }

    private companion object {
        const val KEY_OWNED = "owned"
        const val KEY_COPIES = "copies"
        const val KEY_SHARDS = "shards"
        const val KEY_TOKENS = "trade_tokens"
        const val KEY_TOKEN_DAY = "token_buy_day"
        const val KEY_TOKEN_COUNT = "token_buy_count"
        const val KEY_TOKEN_CHECKIN = "token_checkin_day"
        const val KEY_TOKEN_ACC = "token_luck_acc"
        const val KEY_SHOWCASE = "showcase"
        const val KEY_SHOWCASE_SLOTS = "showcase_slots"
        const val KEY_PITY_EPIC = "pity_epic"
        const val KEY_PITY_LEGENDARY = "pity_legendary"
        const val KEY_CLAIMED_SERIES = "claimed_series"
        const val KEY_ALBUM_CLAIMED = "album_claimed"
        const val KEY_FOIL = "foil"
        const val KEY_SHARD_DAY = "shard_buy_day"
        const val KEY_SHARD_COUNT = "shard_buy_count"
        const val ALBUM_GEMS = 50

        val _copies = MutableStateFlow<Map<String, Int>>(emptyMap())
        val _owned = MutableStateFlow<Set<String>>(emptySet())
        val _shards = MutableStateFlow(0)
        val _tokens = MutableStateFlow(0)
        val _chests = MutableStateFlow<Map<ChestType, Int>>(ChestType.entries.associateWith { 0 })
        val _claimedSeries = MutableStateFlow<Set<String>>(emptySet())
        val _albumClaimed = MutableStateFlow(false)
        val _foil = MutableStateFlow<Set<String>>(emptySet())
        val _showcase = MutableStateFlow<List<String>>(emptyList())
        val _showcaseSlots = MutableStateFlow(Showcase.BASE_SLOTS)
        var loaded = false
    }
}
