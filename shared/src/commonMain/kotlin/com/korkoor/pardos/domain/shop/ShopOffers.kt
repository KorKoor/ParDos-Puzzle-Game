package com.korkoor.pardos.domain.shop

import com.korkoor.pardos.domain.collection.ChestType
import com.korkoor.pardos.domain.economy.Economy
import kotlin.math.ceil
import kotlin.random.Random

/** Precio en monedas y/o gemas (uno de los dos suele ser 0). */
data class Price(val coins: Int = 0, val gems: Int = 0) {
    fun discounted(percent: Int): Price {
        val f = (100 - percent.coerceIn(0, 90)) / 100.0
        return Price(ceil(coins * f).toInt(), ceil(gems * f).toInt())
    }
}

object ShopPrices {
    fun skin(skin: TileSkin) = Price(skin.coinPrice, skin.gemPrice)

    /** Un cofre se puede pagar con monedas o con gemas según su tipo (el épico solo con gemas). */
    fun chestCoins(type: ChestType): Price? = when (type) {
        ChestType.COMMON -> Price(coins = Economy.COMMON_CHEST_COINS)
        ChestType.RARE -> Price(coins = Economy.RARE_CHEST_COINS)
        ChestType.EPIC -> null
    }

    fun chestGems(type: ChestType): Price? = when (type) {
        ChestType.COMMON -> null
        ChestType.RARE -> Price(gems = Economy.RARE_CHEST_GEMS)
        ChestType.EPIC -> Price(gems = Economy.EPIC_CHEST_GEMS)
    }
}

sealed interface OfferItem {
    data class SkinOffer(val skin: TileSkin) : OfferItem
    data class ChestOffer(val type: ChestType) : OfferItem
}

data class DailyOffer(val day: Int, val item: OfferItem, val discountPercent: Int)

/** Oferta del día: igual para todos, cambia cada día local y se puede calcular sin servidor. */
object DailyOffers {
    private val skins = TileSkin.entries.filter { !it.isFree && !it.exclusive }

    fun forDay(day: Int): DailyOffer {
        val rng = Random(day.toLong() * 104_729L + 7L)
        val discount = Economy.dailyOfferDiscounts[rng.nextInt(Economy.dailyOfferDiscounts.size)]
        // Cada tercer día la oferta es un cofre; el resto, una skin
        val item: OfferItem = if (day % 3 == 0) {
            OfferItem.ChestOffer(if (rng.nextInt(4) == 0) ChestType.RARE else ChestType.COMMON)
        } else {
            OfferItem.SkinOffer(skins[rng.nextInt(skins.size)])
        }
        return DailyOffer(day, item, discount)
    }

    /**
     * Igual que [forDay], pero sin ofrecer una skin que el jugador ya tiene: se elige otra sin repetirse (la misma todo el día,
     * porque la semilla solo depende del día). Si ya las tiene todas, la oferta es un cofre raro.
     */
    fun forDayAvoiding(day: Int, ownedSkinIds: Set<String>): DailyOffer {
        val base = forDay(day)
        val item = base.item
        if (item !is OfferItem.SkinOffer || item.skin.id !in ownedSkinIds) return base
        val candidates = skins.filter { it.id !in ownedSkinIds }
        if (candidates.isEmpty()) return DailyOffer(day, OfferItem.ChestOffer(ChestType.RARE), base.discountPercent)
        val rng = Random(day.toLong() * 104_729L + 13L)
        return DailyOffer(day, OfferItem.SkinOffer(candidates[rng.nextInt(candidates.size)]), base.discountPercent)
    }
}
