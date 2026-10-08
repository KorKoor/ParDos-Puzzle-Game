package com.korkoor.pardos.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.korkoor.pardos.domain.shop.SkinInventory
import com.korkoor.pardos.domain.shop.StudioConfig
import com.korkoor.pardos.domain.shop.StudioSkin
import com.korkoor.pardos.domain.shop.TileSkin
import java.util.TimeZone

/** Día local (días desde epoch en la zona horaria del jugador). Evita romper rachas por UTC. */
object LocalDay {
    /** Solo pruebas (builds de depuración): adelanta el calendario para ver fiestas sin esperar. En memoria, nunca se guarda. */
    @Volatile var debugOffsetDays: Int = 0

    fun today(now: Long = System.currentTimeMillis()): Int =
        ((now + TimeZone.getDefault().getOffset(now)) / 86_400_000L).toInt() + debugOffsetDays
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
            _boostWins.value = prefs.getInt(KEY_BOOST_WINS, 0)
            _ownedFx.value = prefs.getStringSet(KEY_OWNED_FX, null)?.toSet() ?: setOf(com.korkoor.pardos.domain.shop.MergeFx.DEFAULT.id)
            _equippedFx.value = com.korkoor.pardos.domain.shop.MergeFx.fromId(prefs.getString(KEY_EQUIPPED_FX, null))
            _undosFlow.value = prefs.getInt(KEY_UNDOS, 0)
            _ownedAvatars.value = (prefs.getStringSet(KEY_OWNED_AVATARS, null) ?: emptySet()).mapNotNull { it.toIntOrNull() }.toSet()
            _ownedBanners.value = (prefs.getStringSet(KEY_OWNED_BANNERS, null) ?: emptySet()).mapNotNull { it.toIntOrNull() }.toSet()
            _extraTimesFlow.value = prefs.getInt(KEY_EXTRA_TIMES, 0)
            _ownedSkins.value = prefs.getStringSet(KEY_OWNED_SKINS, null)?.toSet() ?: setOf(TileSkin.DEFAULT.id)
            _equippedSkin.value = TileSkin.fromId(prefs.getString(KEY_EQUIPPED_SKIN, null))
            _studioConfig.value = StudioConfig.decode(prefs.getString(KEY_STUDIO_CONFIG, null))
            StudioSkin.config = _studioConfig.value
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
    fun buySkin(skin: TileSkin, discountPercent: Int = 0): SkinInventory.Purchase {
        val result = skinInventory().buy(skin, _coins.value, _gems.value, discountPercent)
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

    // --- Studio: la skin de pago que el jugador diseña ---
    /** Configuración actual del editor (se puede probar sin comprar; solo equipar requiere la compra). */
    val studioConfig: StateFlow<StudioConfig> = _studioConfig.asStateFlow()

    fun isStudioOwned(): Boolean = TileSkin.STUDIO.id in _ownedSkins.value

    /** Guarda el diseño. La skin Studio activa lo refleja al instante. */
    fun saveStudioConfig(config: StudioConfig) {
        _studioConfig.value = config
        StudioSkin.config = config
        prefs.edit().putString(KEY_STUDIO_CONFIG, config.encode()).apply()
    }

    /** Compra real de Studio: la skin queda en el inventario. */
    fun unlockStudio() = grantSkin(TileSkin.STUDIO)

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

    // --- Consumibles (se compran con monedas y se gastan en partida) ---
    val undos: StateFlow<Int> get() = _undosFlow

    fun addUndos(n: Int) { _undosFlow.value += n.coerceAtLeast(0); prefs.edit().putInt(KEY_UNDOS, _undosFlow.value).apply() }

    fun buyUndos(pack: Int = 3): Boolean {
        val price = com.korkoor.pardos.domain.economy.Economy.UNDO_PRICE_COINS * pack
        if (!spendCoins(price)) return false
        addUndos(pack)
        return true
    }

    /** Gasta un "Deshacer". Devuelve false si no quedan. */
    fun useUndo(): Boolean {
        if (_undosFlow.value <= 0) return false
        _undosFlow.value -= 1
        prefs.edit().putInt(KEY_UNDOS, _undosFlow.value).apply()
        return true
    }

    // --- Banners de perfil (los gratuitos son de todos) ---
    val ownedBanners: StateFlow<Set<Int>> get() = _ownedBanners

    fun grantBanner(id: Int) {
        if (id <= 0 || id in _ownedBanners.value) return
        _ownedBanners.value = _ownedBanners.value + id
        prefs.edit().putStringSet(KEY_OWNED_BANNERS, _ownedBanners.value.map { it.toString() }.toSet()).apply()
    }

    /** Compra un banner con monedas o gemas según su precio. */
    fun buyBanner(id: Int): com.korkoor.pardos.domain.shop.Banners.Purchase {
        val result = com.korkoor.pardos.domain.shop.Banners.buy(id, _ownedBanners.value, _coins.value, _gems.value)
        if (result is com.korkoor.pardos.domain.shop.Banners.Purchase.Ok) {
            _coins.value = result.coinsLeft
            _gems.value = result.gemsLeft
            prefs.edit().putInt(KEY_COINS, _coins.value).putInt(KEY_GEMS, _gems.value).apply()
            grantBanner(id)
        }
        return result
    }

    // --- Avatares comprados / ganados (los clásicos 1..10 son de todos) ---
    val ownedAvatars: StateFlow<Set<Int>> get() = _ownedAvatars

    fun grantAvatar(id: Int) {
        if (id <= 0 || id in _ownedAvatars.value) return
        _ownedAvatars.value = _ownedAvatars.value + id
        prefs.edit().putStringSet(KEY_OWNED_AVATARS, _ownedAvatars.value.map { it.toString() }.toSet()).apply()
    }

    /** Compra un avatar con monedas. Devuelve el resultado para mostrar el motivo si falla. */
    fun buyAvatar(id: Int): com.korkoor.pardos.domain.shop.Avatars.Purchase {
        val result = com.korkoor.pardos.domain.shop.Avatars.buy(id, _ownedAvatars.value, _coins.value)
        if (result is com.korkoor.pardos.domain.shop.Avatars.Purchase.Ok) {
            _coins.value = result.coinsLeft
            prefs.edit().putInt(KEY_COINS, _coins.value).apply()
            grantAvatar(id)
        }
        return result
    }

    // "Tiempo extra": segundos de regalo en modos con reloj
    val extraTimes: StateFlow<Int> get() = _extraTimesFlow

    fun addExtraTimes(n: Int) { _extraTimesFlow.value += n.coerceAtLeast(0); prefs.edit().putInt(KEY_EXTRA_TIMES, _extraTimesFlow.value).apply() }

    fun buyExtraTimes(pack: Int = 3): Boolean {
        if (!spendCoins(com.korkoor.pardos.domain.economy.Economy.EXTRA_TIME_PRICE_COINS * pack)) return false
        addExtraTimes(pack)
        return true
    }

    fun useExtraTime(): Boolean {
        if (_extraTimesFlow.value <= 0) return false
        _extraTimesFlow.value -= 1
        prefs.edit().putInt(KEY_EXTRA_TIMES, _extraTimesFlow.value).apply()
        return true
    }

    // --- Pack inicial (compra real, una sola vez) ---
    fun isStarterClaimed(): Boolean = prefs.getBoolean("starter_claimed", false)

    /** Entrega gemas y la skin Sakura. Los cofres se añaden desde la colección. */
    fun claimStarterPack() {
        if (isStarterClaimed()) return
        prefs.edit().putBoolean("starter_claimed", true).apply()
        addGems(com.korkoor.pardos.domain.economy.Economy.STARTER_GEMS)
        grantSkin(TileSkin.SAKURA)
        grantFx(com.korkoor.pardos.domain.shop.MergeFx.HEARTS)
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

    // --- Efectos de fusión (cosméticos) ---
    val ownedFx: StateFlow<Set<String>> get() = _ownedFx
    val equippedFx: StateFlow<com.korkoor.pardos.domain.shop.MergeFx> get() = _equippedFx

    private fun fxInventory() = com.korkoor.pardos.domain.shop.MergeFxInventory(_ownedFx.value, _equippedFx.value.id)

    fun buyFx(fx: com.korkoor.pardos.domain.shop.MergeFx, discountPercent: Int = 0): com.korkoor.pardos.domain.shop.MergeFxInventory.Purchase {
        val result = fxInventory().buy(fx, _coins.value, _gems.value, discountPercent)
        if (result is com.korkoor.pardos.domain.shop.MergeFxInventory.Purchase.Ok) {
            _coins.value = result.coinsLeft
            _gems.value = result.gemsLeft
            _ownedFx.value = result.inventory.owned
            prefs.edit().putInt(KEY_COINS, _coins.value).putInt(KEY_GEMS, _gems.value).putStringSet(KEY_OWNED_FX, _ownedFx.value).apply()
        }
        return result
    }

    fun grantFx(fx: com.korkoor.pardos.domain.shop.MergeFx) {
        _ownedFx.value = fxInventory().grant(fx).owned
        prefs.edit().putStringSet(KEY_OWNED_FX, _ownedFx.value).apply()
    }

    fun equipFx(fx: com.korkoor.pardos.domain.shop.MergeFx) {
        val inv = fxInventory().equip(fx)
        _equippedFx.value = com.korkoor.pardos.domain.shop.MergeFx.fromId(inv.equipped)
        prefs.edit().putString(KEY_EQUIPPED_FX, inv.equipped).apply()
    }

    // --- Impulso de monedas (se compra con gemas) ---
    val coinBoostWins: StateFlow<Int> get() = _boostWins

    fun buyCoinBoost(): Boolean {
        val left = _boostWins.value
        if (!com.korkoor.pardos.domain.shop.CoinBoost.canBuy(_gems.value, left)) return false
        if (!spendGems(com.korkoor.pardos.domain.shop.CoinBoost.PRICE_GEMS)) return false
        _boostWins.value = com.korkoor.pardos.domain.shop.CoinBoost.addWins(left)
        prefs.edit().putInt(KEY_BOOST_WINS, _boostWins.value).apply()
        return true
    }

    /**
     * Monedas finales de una victoria: aplica el extra del VIP y, si hay impulso activo, lo aplica y gasta una victoria.
     * Se llama una sola vez por victoria.
     */
    fun applyWinBonuses(coins: Int): Int {
        var total = com.korkoor.pardos.domain.shop.VipPerks.coinsWithBonus(coins, _vip.value)
        val wins = _boostWins.value
        if (wins > 0 && coins > 0) {
            total = com.korkoor.pardos.domain.shop.CoinBoost.apply(total, wins)
            _boostWins.value = wins - 1
            prefs.edit().putInt(KEY_BOOST_WINS, _boostWins.value).apply()
        }
        return total
    }

    // --- VIP: gemas diarias ---
    fun canClaimVipDaily(today: Int = LocalDay.today()): Boolean =
        com.korkoor.pardos.domain.shop.VipPerks.canClaimDaily(_vip.value, prefs.getInt(KEY_VIP_DAY, Int.MIN_VALUE), today)

    /** Entrega las gemas diarias del VIP (una vez por día local). Devuelve cuántas, o 0 si no tocaba. */
    fun claimVipDaily(today: Int = LocalDay.today()): Int {
        if (!canClaimVipDaily(today)) return 0
        prefs.edit().putInt(KEY_VIP_DAY, today).apply()
        addGems(com.korkoor.pardos.domain.shop.VipPerks.DAILY_GEMS)
        return com.korkoor.pardos.domain.shop.VipPerks.DAILY_GEMS
    }

    // --- Primera compra de cada pack de gemas: doble ---
    fun isFirstPurchase(productId: String): Boolean = !prefs.getBoolean("purchased_$productId", false)
    fun markPurchased(productId: String) { prefs.edit().putBoolean("purchased_$productId", true).apply() }

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
        const val KEY_OWNED_FX = "owned_fx"
        const val KEY_EQUIPPED_FX = "equipped_fx"
        const val KEY_BOOST_WINS = "boost_wins"
        const val KEY_VIP_DAY = "vip_daily_day"
        const val KEY_UNDOS = "undos"
        const val KEY_OWNED_AVATARS = "owned_avatars"
        const val KEY_OWNED_BANNERS = "owned_banners"
        const val KEY_EXTRA_TIMES = "extra_times"
        const val KEY_OWNED_SKINS = "owned_skins"
        const val KEY_EQUIPPED_SKIN = "equipped_skin"
        const val KEY_STUDIO_CONFIG = "studio_config"
        val _coins = MutableStateFlow(0)
        val _gems = MutableStateFlow(0)
        val _freezes = MutableStateFlow(0)
        val _vip = MutableStateFlow(false)
        val _ownedFx = MutableStateFlow(setOf(com.korkoor.pardos.domain.shop.MergeFx.DEFAULT.id))
        val _equippedFx = MutableStateFlow(com.korkoor.pardos.domain.shop.MergeFx.DEFAULT)
        val _boostWins = MutableStateFlow(0)
        val _undosFlow = MutableStateFlow(0)
        val _ownedAvatars = MutableStateFlow<Set<Int>>(emptySet())
        val _ownedBanners = MutableStateFlow<Set<Int>>(emptySet())
        val _extraTimesFlow = MutableStateFlow(0)
        val _ownedSkins = MutableStateFlow(setOf(TileSkin.DEFAULT.id))
        val _equippedSkin = MutableStateFlow(TileSkin.DEFAULT)
        val _studioConfig = MutableStateFlow(StudioConfig())
        var loaded = false
    }
}
