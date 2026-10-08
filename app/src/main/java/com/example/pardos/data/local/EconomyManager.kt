package com.korkoor.pardos.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.korkoor.pardos.domain.shop.SkinInventory
import com.korkoor.pardos.domain.shop.TileSkin
import java.util.TimeZone

/** Día local (días desde epoch en la zona horaria del jugador). Evita romper rachas por UTC. */
object LocalDay {
    fun today(now: Long = System.currentTimeMillis()): Int =
        ((now + TimeZone.getDefault().getOffset(now)) / 86_400_000L).toInt()
}

/**
 * Monedas (se ganan jugando), gemas (premium, también se compran) y escudos de racha.
 * Los StateFlow son compartidos entre instancias para que la UI siempre vea el valor actual.
 */
class EconomyManager(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("pardos_economy", Context.MODE_PRIVATE)

    init {
        if (!loaded) {
            _coins.value = prefs.getInt(KEY_COINS, 0)
            _gems.value = prefs.getInt(KEY_GEMS, 0)
            _freezes.value = prefs.getInt(KEY_FREEZES, 0)
            _vip.value = prefs.getBoolean(KEY_VIP, false)
            _ownedSkins.value = prefs.getStringSet(KEY_OWNED_SKINS, null)?.toSet() ?: setOf(TileSkin.DEFAULT.id)
            _equippedSkin.value = TileSkin.fromId(prefs.getString(KEY_EQUIPPED_SKIN, null))
            loaded = true
        }
    }

    val coins: StateFlow<Int> = _coins.asStateFlow()
    val gems: StateFlow<Int> = _gems.asStateFlow()
    val streakFreezes: StateFlow<Int> = _freezes.asStateFlow()
    /** VIP (compra única): los poderes que piden anuncio se usan gratis. */
    val isVip: StateFlow<Boolean> = _vip.asStateFlow()

    // --- Skins de fichas ---
    val ownedSkins: StateFlow<Set<String>> = _ownedSkins.asStateFlow()
    val equippedSkin: StateFlow<TileSkin> = _equippedSkin.asStateFlow()

    private fun skinInventory() = SkinInventory(_ownedSkins.value, _equippedSkin.value.id)

    /** Compra una skin con monedas o gemas según su precio. Devuelve el resultado para mostrar el motivo. */
    fun buySkin(skin: TileSkin): SkinInventory.Purchase {
        val result = skinInventory().buy(skin, _coins.value, _gems.value)
        if (result is SkinInventory.Purchase.Ok) {
            _coins.value = result.coinsLeft
            _gems.value = result.gemsLeft
            _ownedSkins.value = result.inventory.owned
            prefs.edit()
                .putInt(KEY_COINS, _coins.value)
                .putInt(KEY_GEMS, _gems.value)
                .putStringSet(KEY_OWNED_SKINS, _ownedSkins.value)
                .apply()
        }
        return result
    }

    fun equipSkin(skin: TileSkin) {
        val inv = skinInventory().equip(skin)
        _equippedSkin.value = TileSkin.fromId(inv.equipped)
        prefs.edit().putString(KEY_EQUIPPED_SKIN, inv.equipped).apply()
    }

    fun setVip(value: Boolean) {
        _vip.value = value
        prefs.edit().putBoolean(KEY_VIP, value).apply()
    }

    /** Compra un escudo de racha con monedas. Devuelve false si no se puede. */
    fun buyStreakFreeze(): Boolean {
        if (!com.korkoor.pardos.domain.shop.CoinShop.canBuyStreakFreeze(_coins.value, _freezes.value)) return false
        if (!spendCoins(com.korkoor.pardos.domain.shop.CoinShop.STREAK_FREEZE_PRICE_COINS)) return false
        addStreakFreezes(1)
        return true
    }

    fun isChapterChestClaimed(chapter: Int): Boolean = prefs.getBoolean("chapter_chest_$chapter", false)

    /** Abre el cofre de un capítulo una sola vez y entrega el premio. Devuelve null si ya estaba reclamado. */
    fun claimChapterChest(chapter: Int): com.korkoor.pardos.domain.rewards.Reward? {
        if (isChapterChestClaimed(chapter)) return null
        val reward = com.korkoor.pardos.domain.rewards.ChapterRewards.forChapter(chapter)
        prefs.edit().putBoolean("chapter_chest_$chapter", true).apply()
        addCoins(reward.coins)
        addGems(reward.gems)
        return reward
    }

    // --- Pack inicial (compra real, una sola vez) ---
    fun isStarterClaimed(): Boolean = prefs.getBoolean("starter_claimed", false)

    /** Entrega gemas y la skin Sakura. Los cofres se añaden desde la colección. */
    fun claimStarterPack() {
        if (isStarterClaimed()) return
        prefs.edit().putBoolean("starter_claimed", true).apply()
        addGems(com.korkoor.pardos.domain.economy.Economy.STARTER_GEMS)
        grantSkin(TileSkin.SAKURA)
        com.korkoor.pardos.data.local.CollectionManager(appContext).addChests(
            com.korkoor.pardos.domain.collection.ChestType.RARE,
            com.korkoor.pardos.domain.economy.Economy.STARTER_RARE_CHESTS
        )
    }

    /** Concede una skin sin cobrar (recompensas, packs). */
    fun grantSkin(skin: TileSkin) {
        val inv = skinInventory().grant(skin)
        _ownedSkins.value = inv.owned
        prefs.edit().putStringSet(KEY_OWNED_SKINS, inv.owned).apply()
    }

    /** Cambia gemas por monedas. */
    fun exchangeGems(gems: Int): Boolean {
        if (!spendGems(gems)) return false
        addCoins(com.korkoor.pardos.domain.shop.CoinShop.coinsForGems(gems))
        return true
    }

    fun addCoins(amount: Int) {
        if (amount <= 0) return
        _coins.value += amount
        prefs.edit().putInt(KEY_COINS, _coins.value).apply()
    }

    fun addGems(amount: Int) {
        if (amount <= 0) return
        _gems.value += amount
        prefs.edit().putInt(KEY_GEMS, _gems.value).apply()
    }

    fun addStreakFreezes(amount: Int) {
        if (amount <= 0) return
        _freezes.value += amount
        prefs.edit().putInt(KEY_FREEZES, _freezes.value).apply()
    }

    fun setStreakFreezes(value: Int) {
        _freezes.value = value.coerceAtLeast(0)
        prefs.edit().putInt(KEY_FREEZES, _freezes.value).apply()
    }

    /** Gasta monedas. Devuelve false (sin cambios) si no alcanzan. */
    fun spendCoins(amount: Int): Boolean {
        if (amount < 0 || _coins.value < amount) return false
        _coins.value -= amount
        prefs.edit().putInt(KEY_COINS, _coins.value).apply()
        return true
    }

    fun spendGems(amount: Int): Boolean {
        if (amount < 0 || _gems.value < amount) return false
        _gems.value -= amount
        prefs.edit().putInt(KEY_GEMS, _gems.value).apply()
        return true
    }

    private companion object {
        const val KEY_COINS = "coins"
        const val KEY_GEMS = "gems"
        const val KEY_FREEZES = "streak_freezes"
        const val KEY_VIP = "vip"
        const val KEY_OWNED_SKINS = "owned_skins"
        const val KEY_EQUIPPED_SKIN = "equipped_skin"
        val _coins = MutableStateFlow(0)
        val _gems = MutableStateFlow(0)
        val _freezes = MutableStateFlow(0)
        val _vip = MutableStateFlow(false)
        val _ownedSkins = MutableStateFlow(setOf(TileSkin.DEFAULT.id))
        val _equippedSkin = MutableStateFlow(TileSkin.DEFAULT)
        var loaded = false
    }
}
