package com.korkoor.pardos.domain.meta

import com.korkoor.pardos.domain.collection.CollectibleCatalog
import com.korkoor.pardos.domain.collection.Series
import com.korkoor.pardos.domain.economy.Economy
import com.korkoor.pardos.domain.retention.SeasonPass
import com.korkoor.pardos.domain.retention.SeasonReward
import com.korkoor.pardos.domain.shop.Avatars
import com.korkoor.pardos.domain.shop.Banners
import com.korkoor.pardos.domain.shop.EventSkins
import com.korkoor.pardos.domain.shop.HiddenSkins
import com.korkoor.pardos.domain.shop.MergeFx
import com.korkoor.pardos.domain.shop.TileSkin

/** Catálogos que no cambian durante la sesión: Swift los pide una vez y los guarda. */
internal object MetaCatalogs {

    private fun argbList(list: List<Long>?): Any? = list?.map { rgb(it) }

    fun skins(): String = arr(TileSkin.entries.map { skin -> skinObject(skin, skin.style) })

    /** Una skin como objeto JSON; [style] permite enseñar una vista previa de Studio sin guardarla. */
    fun skinObject(skin: TileSkin, style: com.korkoor.pardos.domain.shop.SkinStyle, idOverride: String? = null): Raw {
        val st = style
        return robj(
            "id" to (idOverride ?: skin.id), "name" to skin.displayName, "rarity" to skin.rarity.name, "source" to skin.source.name,
            "coin" to skin.coinPrice, "gem" to skin.gemPrice, "exclusive" to skin.exclusive, "free" to skin.isFree,
            "finish" to st.finish.name, "palette" to argbList(st.tilePalette),
            "darkText" to rgb(st.darkText), "lightText" to rgb(st.lightText), "lightFrom" to st.lightTextFromPower,
            "bgTop" to st.bgTop?.let { rgb(it) }, "bgBottom" to st.bgBottom?.let { rgb(it) },
            "ink" to st.ink?.let { rgb(it) }, "accent" to st.accent?.let { rgb(it) }, "surface" to st.surface?.let { rgb(it) },
            "particles" to st.particles.name, "particleTint" to rgb(st.particleTint),
            "hint" to if (skin.source == com.korkoor.pardos.domain.shop.SkinSource.HIDDEN) HiddenSkins.hint(skin) else null,
            "event" to EventSkins.eventFor(skin)?.let { EventSkins.eventName(it) }
        )
    }

    fun fx(): String = arr(MergeFx.entries.map {
        robj(
            "id" to it.id, "name" to it.displayName, "blurb" to it.blurb, "rarity" to it.rarity.name,
            "coin" to it.coinPrice, "gem" to it.gemPrice, "source" to it.source.name, "buyable" to it.buyable
        )
    })

    fun avatars(): String = arr(Avatars.all.map {
        robj(
            "id" to it.id, "name" to it.name, "source" to it.source.name, "coin" to it.coinPrice,
            "animal" to it.animal?.name, "accessory" to it.accessory.name, "variant" to it.variant.name,
            "scene" to it.scene.name, "frame" to it.frame.name, "rarity" to it.rarity.name,
            "rank" to it.unlockRank?.name, "halloween" to it.isHalloween
        )
    })

    fun banners(): String = arr(Banners.all.map {
        robj(
            "id" to it.id, "name" to it.name, "source" to it.source.name, "pattern" to it.pattern.name,
            "top" to rgb(it.top), "bottom" to rgb(it.bottom), "accent" to rgb(it.accent), "ink" to rgb(it.ink),
            "coin" to it.coinPrice, "gem" to it.gemPrice, "rarity" to it.rarity.name, "rank" to it.unlockRank?.name,
            "dark" to it.isDark, "halloween" to it.isHalloween
        )
    })

    fun album(): String {
        val series = Series.entries.map {
            robj(
                "id" to it.id, "name" to it.nameEs, "glyph" to it.glyph, "top" to rgb(it.top), "bottom" to rgb(it.bottom),
                "accent" to rgb(it.accent), "motif" to it.motif.name, "perk" to it.perk.name, "perkLabel" to it.perk.labelEs,
                "coins" to it.rewardCoins, "gems" to it.rewardGems
            )
        }
        val pieces = CollectibleCatalog.all.map { c ->
            robj(
                "id" to c.id, "series" to c.series.id, "rarity" to c.rarity.name, "glyph" to c.glyph, "name" to c.nameEs,
                "desc" to c.descEs, "n" to c.number,
                "perk" to c.perk?.let { it.label(false) }, "foilPerk" to c.perk?.let { it.label(true) },
                "craft" to c.rarity.craftCost, "sell" to c.rarity.sellCoins, "foilCost" to c.rarity.craftCost * 2
            )
        }
        return obj("series" to series, "pieces" to pieces)
    }

    private fun rewardJson(r: SeasonReward): Raw = Raw(obj(
        "coins" to r.coins, "gems" to r.gems, "chest" to r.chest?.name, "freezes" to r.freezes, "undos" to r.undos,
        "skin" to r.skin?.id, "avatar" to r.avatar, "banner" to r.banner, "fx" to r.fx?.id, "tokens" to r.tokens
    ))

    /** Los 30 niveles del pase de la temporada [seasonId], con la vía gratis y la premium. */
    fun seasonTiers(seasonId: Int): String = arr((1..SeasonPass.TIERS).map { t ->
        robj("tier" to t, "free" to rewardJson(SeasonPass.freeReward(t, seasonId)), "premium" to rewardJson(SeasonPass.premiumReward(t, seasonId)))
    })

    fun economy(): String = obj(
        "undoPrice" to Economy.UNDO_PRICE_COINS, "revivePrice" to Economy.REVIVE_PRICE_GEMS, "powerPrice" to Economy.MANUAL_POWER_PRICE_COINS, "powerCooldownMs" to Economy.POWER_COOLDOWN_MS, "extraTimeSeconds" to Economy.EXTRA_TIME_SECONDS, "extraTimePrice" to Economy.EXTRA_TIME_PRICE_COINS,
        "freezePrice" to Economy.STREAK_FREEZE_PRICE_COINS, "maxFreezes" to Economy.MAX_STREAK_FREEZES,
        "commonChestCoins" to Economy.COMMON_CHEST_COINS, "rareChestCoins" to Economy.RARE_CHEST_COINS,
        "rareChestGems" to Economy.RARE_CHEST_GEMS, "epicChestGems" to Economy.EPIC_CHEST_GEMS,
        "boostPrice" to Economy.COIN_BOOST_PRICE_GEMS, "boostWins" to Economy.COIN_BOOST_WINS, "boostPercent" to Economy.COIN_BOOST_PERCENT,
        "coinsPerGem" to com.korkoor.pardos.domain.shop.CoinShop.COINS_PER_GEM,
        "seasonTiers" to SeasonPass.TIERS, "pointsPerTier" to SeasonPass.POINTS_PER_TIER,
        "eventSkinGems" to EventSkins.GEM_PRICE, "eventWins" to EventSkins.WINS_REQUIRED,
        "shardPackShards" to com.korkoor.pardos.domain.collection.ShardShop.SHARDS_PER_PACK,
        "shardPackGems" to com.korkoor.pardos.domain.collection.ShardShop.GEMS_PER_PACK,
        "shardPacksPerDay" to com.korkoor.pardos.domain.collection.ShardShop.MAX_PACKS_PER_DAY,
        "tokenGems" to com.korkoor.pardos.domain.collection.TokenRules.GEMS_PER_TOKEN,
        "tokensPerDay" to com.korkoor.pardos.domain.collection.TokenRules.MAX_BOUGHT_PER_DAY,
        "seriesPackCoins" to com.korkoor.pardos.domain.collection.SeriesPackRules.COINS,
        "seriesPackGems" to com.korkoor.pardos.domain.collection.SeriesPackRules.GEMS,
        "seriesPackCards" to com.korkoor.pardos.domain.collection.SeriesPackRules.CARDS,
        "albumGems" to com.korkoor.pardos.domain.meta.CollectionOps.ALBUM_GEMS
    )
}
