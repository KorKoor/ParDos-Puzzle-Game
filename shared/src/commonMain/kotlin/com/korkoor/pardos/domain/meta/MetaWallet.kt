package com.korkoor.pardos.domain.meta

import com.korkoor.pardos.domain.collection.ChestType
import com.korkoor.pardos.domain.economy.Economy
import com.korkoor.pardos.domain.retention.SeasonReward
import com.korkoor.pardos.domain.shop.Avatars
import com.korkoor.pardos.domain.shop.Banners
import com.korkoor.pardos.domain.shop.CoinBoost
import com.korkoor.pardos.domain.shop.CoinShop
import com.korkoor.pardos.domain.shop.MergeFx
import com.korkoor.pardos.domain.shop.MergeFxInventory
import com.korkoor.pardos.domain.shop.SkinInventory
import com.korkoor.pardos.domain.shop.TileSkin
import com.korkoor.pardos.domain.shop.VipPerks

/** Monedas, gemas, consumibles y cosméticos del jugador. Las mismas reglas que EconomyManager de Android. */
internal class Wallet(private val s: MetaStore) {
    var coins: Int
        get() = s.int("coins")
        private set(v) = s.setInt("coins", v)
    var gems: Int
        get() = s.int("gems")
        private set(v) = s.setInt("gems", v)
    val freezes: Int get() = s.int("freezes")
    val undos: Int get() = s.int("undos")
    val extraTimes: Int get() = s.int("extra_times")
    val vip: Boolean get() = s.bool("vip")
    fun setVip(value: Boolean) { s.setBool("vip", value) }
    val starterClaimed: Boolean get() = s.bool("starter_claimed")
    val boostWins: Int get() = s.int("boost_wins")

    fun addCoins(n: Int) { if (n > 0) coins += n }
    fun addGems(n: Int) { if (n > 0) gems += n }
    fun addFreezes(n: Int) { if (n > 0) s.setInt("freezes", (freezes + n).coerceAtMost(Economy.MAX_STREAK_FREEZES)) }
    fun setFreezes(n: Int) { s.setInt("freezes", n.coerceAtLeast(0)) }
    fun addUndos(n: Int) { if (n > 0) s.setInt("undos", undos + n) }
    fun addExtraTimes(n: Int) { if (n > 0) s.setInt("extra_times", extraTimes + n) }

    fun spendCoins(n: Int): Boolean {
        if (n < 0 || coins < n) return false
        coins -= n
        return true
    }

    fun spendGems(n: Int): Boolean {
        if (n < 0 || gems < n) return false
        gems -= n
        return true
    }

    fun useUndo(): Boolean {
        if (undos <= 0) return false
        s.setInt("undos", undos - 1)
        return true
    }

    fun useExtraTime(): Boolean {
        if (extraTimes <= 0) return false
        s.setInt("extra_times", extraTimes - 1)
        return true
    }

    // ---------------- skins ----------------

    val ownedSkins: Set<String>
        get() = if (s.has("skins_owned")) s.strSet("skins_owned") else setOf(TileSkin.DEFAULT.id)
    val equippedSkin: TileSkin get() = TileSkin.fromId(s.str("skin_eq", TileSkin.DEFAULT.id))

    private fun skinInventory() = SkinInventory(ownedSkins, equippedSkin.id)

    fun grantSkin(skin: TileSkin) {
        s.setStrSet("skins_owned", skinInventory().grant(skin).owned)
    }

    fun buySkin(skin: TileSkin, discount: Int): SkinInventory.Purchase {
        val r = skinInventory().buy(skin, coins, gems, discount)
        if (r is SkinInventory.Purchase.Ok) {
            coins = r.coinsLeft
            gems = r.gemsLeft
            s.setStrSet("skins_owned", r.inventory.owned)
        }
        return r
    }

    fun equipSkin(skin: TileSkin): Boolean {
        val inv = skinInventory().equip(skin)
        if (inv.equipped != skin.id) return false
        s.setStr("skin_eq", skin.id)
        return true
    }

    // ---------------- efectos de fusión ----------------

    val ownedFx: Set<String>
        get() = if (s.has("fx_owned")) s.strSet("fx_owned") else setOf(MergeFx.DEFAULT.id)
    val equippedFx: MergeFx get() = MergeFx.fromId(s.str("fx_eq", MergeFx.DEFAULT.id))

    private fun fxInventory() = MergeFxInventory(ownedFx, equippedFx.id)

    fun grantFx(fx: MergeFx) { s.setStrSet("fx_owned", fxInventory().grant(fx).owned) }

    fun buyFx(fx: MergeFx, discount: Int): MergeFxInventory.Purchase {
        val r = fxInventory().buy(fx, coins, gems, discount)
        if (r is MergeFxInventory.Purchase.Ok) {
            coins = r.coinsLeft
            gems = r.gemsLeft
            s.setStrSet("fx_owned", r.inventory.owned)
        }
        return r
    }

    fun equipFx(fx: MergeFx): Boolean {
        val inv = fxInventory().equip(fx)
        if (inv.equipped != fx.id) return false
        s.setStr("fx_eq", fx.id)
        return true
    }

    // ---------------- avatares y banners ----------------

    val ownedAvatars: Set<Int> get() = s.intSet("avatars_owned")
    val ownedBanners: Set<Int> get() = s.intSet("banners_owned")
    var avatarId: Int
        get() = s.int("avatar", Avatars.DEFAULT_ID)
        set(v) = s.setInt("avatar", v)
    var bannerId: Int
        get() = s.int("banner", 1)
        set(v) = s.setInt("banner", v)

    fun grantAvatar(id: Int) { if (id > 0) s.addToIntSet("avatars_owned", id) }
    fun grantBanner(id: Int) { if (id > 0) s.addToIntSet("banners_owned", id) }

    fun buyAvatar(id: Int): Avatars.Purchase {
        val r = Avatars.buy(id, ownedAvatars, coins)
        if (r is Avatars.Purchase.Ok) {
            coins = r.coinsLeft
            grantAvatar(id)
        }
        return r
    }

    fun buyBanner(id: Int): Banners.Purchase {
        val r = Banners.buy(id, ownedBanners, coins, gems)
        if (r is Banners.Purchase.Ok) {
            coins = r.coinsLeft
            gems = r.gemsLeft
            grantBanner(id)
        }
        return r
    }

    // ---------------- consumibles y extras ----------------

    fun buyUndos(pack: Int = 3): Boolean {
        if (!spendCoins(Economy.UNDO_PRICE_COINS * pack)) return false
        addUndos(pack)
        return true
    }

    fun buyExtraTimes(pack: Int = 3): Boolean {
        if (!spendCoins(Economy.EXTRA_TIME_PRICE_COINS * pack)) return false
        addExtraTimes(pack)
        return true
    }

    fun buyFreeze(): Boolean {
        if (!CoinShop.canBuyStreakFreeze(coins, freezes)) return false
        if (!spendCoins(CoinShop.STREAK_FREEZE_PRICE_COINS)) return false
        addFreezes(1)
        return true
    }

    fun buyCoinBoost(): Boolean {
        if (!CoinBoost.canBuy(gems, boostWins)) return false
        if (!spendGems(CoinBoost.PRICE_GEMS)) return false
        s.setInt("boost_wins", CoinBoost.addWins(boostWins))
        return true
    }

    fun exchangeGems(g: Int): Boolean {
        if (g <= 0 || !spendGems(g)) return false
        addCoins(CoinShop.coinsForGems(g))
        return true
    }

    /** Monedas finales de una victoria: aplica el extra del VIP y el impulso (gasta una victoria del impulso). */
    fun applyWinBonuses(base: Int): Int {
        var total = VipPerks.coinsWithBonus(base, vip)
        val wins = boostWins
        if (wins > 0 && base > 0) {
            total = CoinBoost.apply(total, wins)
            s.setInt("boost_wins", wins - 1)
        }
        return total
    }

    fun claimVipDaily(today: Int): Int {
        if (!VipPerks.canClaimDaily(vip, s.int("vip_day", Int.MIN_VALUE), today)) return 0
        s.setInt("vip_day", today)
        addGems(VipPerks.DAILY_GEMS)
        return VipPerks.DAILY_GEMS
    }

    // ---------------- premios compuestos ----------------

    fun grant(r: SeasonReward, collection: CollectionOps) {
        addCoins(r.coins)
        addGems(r.gems)
        r.chest?.let { collection.addChests(it, 1) }
        addFreezes(r.freezes)
        addUndos(r.undos)
        r.skin?.let { grantSkin(it) }
        if (r.avatar != 0) grantAvatar(r.avatar)
        if (r.banner != 0) grantBanner(r.banner)
        r.fx?.let { grantFx(it) }
        if (r.tokens > 0) collection.addTokens(r.tokens)
    }

    fun claimStarterPack(collection: CollectionOps): Boolean {
        if (s.bool("starter_claimed")) return false
        s.setBool("starter_claimed", true)
        addGems(Economy.STARTER_GEMS)
        grantSkin(TileSkin.SAKURA)
        grantFx(MergeFx.HEARTS)
        collection.addChests(ChestType.RARE, Economy.STARTER_RARE_CHESTS)
        return true
    }
}
