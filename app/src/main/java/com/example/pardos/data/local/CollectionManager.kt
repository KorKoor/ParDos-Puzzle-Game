package com.korkoor.pardos.data.local

import android.content.Context
import android.content.SharedPreferences
import com.korkoor.pardos.domain.collection.ChestResult
import com.korkoor.pardos.domain.collection.ChestRules
import com.korkoor.pardos.domain.collection.ChestType
import com.korkoor.pardos.domain.collection.Collectible
import com.korkoor.pardos.domain.collection.CollectibleCatalog
import com.korkoor.pardos.domain.collection.CraftRules
import com.korkoor.pardos.domain.collection.FoilRules
import com.korkoor.pardos.domain.collection.ShardShop
import com.korkoor.pardos.domain.collection.PityState
import com.korkoor.pardos.domain.collection.Series
import com.korkoor.pardos.domain.shop.Price
import com.korkoor.pardos.domain.shop.TileSkin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Álbum de coleccionables: piezas, esencia (de repetidas), cofres sin abrir y recompensas.
 * Los StateFlow son compartidos entre instancias para que la UI siempre vea el valor actual.
 */
class CollectionManager(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = appContext.getSharedPreferences("pardos_collection", Context.MODE_PRIVATE)
    private val economy = EconomyManager(appContext)

    init {
        if (!loaded) {
            _owned.value = prefs.getStringSet(KEY_OWNED, emptySet())?.toSet() ?: emptySet()
            _shards.value = prefs.getInt(KEY_SHARDS, 0)
            _chests.value = ChestType.entries.associateWith { prefs.getInt("chest_${it.name}", 0) }
            _claimedSeries.value = prefs.getStringSet(KEY_CLAIMED_SERIES, emptySet())?.toSet() ?: emptySet()
            _albumClaimed.value = prefs.getBoolean(KEY_ALBUM_CLAIMED, false)
            _foil.value = prefs.getStringSet(KEY_FOIL, emptySet())?.toSet() ?: emptySet()
            loaded = true
        }
    }

    val owned: StateFlow<Set<String>> = _owned.asStateFlow()
    val shards: StateFlow<Int> = _shards.asStateFlow()
    val chests: StateFlow<Map<ChestType, Int>> = _chests.asStateFlow()
    val claimedSeries: StateFlow<Set<String>> = _claimedSeries.asStateFlow()
    val albumClaimed: StateFlow<Boolean> = _albumClaimed.asStateFlow()
    /** Piezas convertidas en Brillantes. */
    val foil: StateFlow<Set<String>> = _foil.asStateFlow()

    private fun pity() = PityState(prefs.getInt(KEY_PITY_EPIC, 0), prefs.getInt(KEY_PITY_LEGENDARY, 0))

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

    /** Abre un cofre del inventario. Aplica las piezas, la esencia y la garantía. */
    fun openChest(type: ChestType): ChestResult? {
        if (chestCount(type) <= 0) return null
        val result = ChestRules.open(type, _owned.value, pity())

        val newOwned = _owned.value + result.drops.filter { it.isNew }.map { it.collectible.id }
        val newShards = _shards.value + result.drops.sumOf { it.shards }
        val remaining = (chestCount(type) - 1).coerceAtLeast(0)

        _owned.value = newOwned
        _shards.value = newShards
        _chests.value = _chests.value.toMutableMap().also { it[type] = remaining }
        prefs.edit()
            .putStringSet(KEY_OWNED, newOwned)
            .putInt(KEY_SHARDS, newShards)
            .putInt("chest_${type.name}", remaining)
            .putInt(KEY_PITY_EPIC, result.pity.sinceEpic)
            .putInt(KEY_PITY_LEGENDARY, result.pity.sinceLegendary)
            .apply()
        return result
    }

    /** Crea una pieza que te falta gastando esencia. */
    fun craft(c: Collectible): Boolean {
        if (!CraftRules.canCraft(c, _owned.value, _shards.value)) return false
        val newOwned = _owned.value + c.id
        val newShards = _shards.value - CraftRules.cost(c)
        _owned.value = newOwned
        _shards.value = newShards
        prefs.edit().putStringSet(KEY_OWNED, newOwned).putInt(KEY_SHARDS, newShards).apply()
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
        const val KEY_SHARDS = "shards"
        const val KEY_PITY_EPIC = "pity_epic"
        const val KEY_PITY_LEGENDARY = "pity_legendary"
        const val KEY_CLAIMED_SERIES = "claimed_series"
        const val KEY_ALBUM_CLAIMED = "album_claimed"
        const val KEY_FOIL = "foil"
        const val KEY_SHARD_DAY = "shard_buy_day"
        const val KEY_SHARD_COUNT = "shard_buy_count"
        const val ALBUM_GEMS = 50

        val _owned = MutableStateFlow<Set<String>>(emptySet())
        val _shards = MutableStateFlow(0)
        val _chests = MutableStateFlow<Map<ChestType, Int>>(ChestType.entries.associateWith { 0 })
        val _claimedSeries = MutableStateFlow<Set<String>>(emptySet())
        val _albumClaimed = MutableStateFlow(false)
        val _foil = MutableStateFlow<Set<String>>(emptySet())
        var loaded = false
    }
}
