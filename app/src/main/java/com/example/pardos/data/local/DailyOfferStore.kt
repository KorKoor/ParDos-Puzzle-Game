package com.korkoor.pardos.data.local

import android.content.Context
import com.korkoor.pardos.domain.shop.DailyOffer
import com.korkoor.pardos.domain.shop.DailyOffers
import com.korkoor.pardos.domain.shop.OfferCodec

/**
 * La oferta del día se fija la primera vez que se pide cada día y se guarda: así la tienda y el menú enseñan siempre la misma, y
 * comprar la skin no hace aparecer otra con descuento a mitad del día (que sería una cadena de descuentos).
 */
object DailyOfferStore {
    private const val PREFS = "pardos_offers"
    private const val K_DAY = "offer_day"
    private const val K_ITEM = "offer_item"

    fun offerFor(context: Context, day: Int, ownedSkinIds: Set<String>): DailyOffer {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getInt(K_DAY, -1) == day) {
            OfferCodec.decode(prefs.getString(K_ITEM, null))?.let { return DailyOffer(day, it, DailyOffers.forDay(day).discountPercent) }
        }
        val picked = DailyOffers.forDayAvoiding(day, ownedSkinIds)
        prefs.edit().putInt(K_DAY, day).putString(K_ITEM, OfferCodec.encode(picked.item)).apply()
        return picked
    }
}
