package com.korkoor.pardos.data.local

import android.content.Context
import com.korkoor.pardos.domain.collection.ChestType
import com.korkoor.pardos.domain.shop.OfferContents
import com.korkoor.pardos.domain.shop.StruggleHelp
import com.korkoor.pardos.domain.shop.TimedOffer
import java.util.TimeZone

/**
 * Compras de las ofertas de tiempo limitado y de la ayuda por nivel atascado. Todo se paga con gemas y se entrega al momento;
 * cada ventana de oferta se compra una sola vez (queda guardado para que cerrar y abrir la app no la vuelva a ofrecer).
 */
class TimedOfferStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("pardos_offers", Context.MODE_PRIVATE)
    private val economy = EconomyManager(appContext)

    fun isBought(offer: TimedOffer): Boolean = prefs.getBoolean("bought_${offer.windowId}", false)

    /** Milisegundos que faltan para que termine la ventana (fin del último día, hora local). 0 si ya pasó. */
    fun remainingMs(offer: TimedOffer, now: Long = System.currentTimeMillis()): Long {
        val end = (offer.lastDay + 1) * 86_400_000L - TimeZone.getDefault().getOffset(now) - LocalDay.debugOffsetDays * 86_400_000L
        return (end - now).coerceAtLeast(0L)
    }

    /** Compra la oferta: gasta las gemas y entrega todo. Devuelve false si ya la compró o no le alcanzan las gemas. */
    fun buy(offer: TimedOffer, today: Int = LocalDay.today()): Boolean {
        if (!offer.activeOn(today.toLong()) || isBought(offer)) return false
        if (!economy.spendGems(offer.priceGems)) return false
        grant(offer.contents)
        prefs.edit().putBoolean("bought_${offer.windowId}", true).apply()
        return true
    }

    private fun grant(c: OfferContents) {
        if (c.undos > 0) economy.addUndos(c.undos)
        if (c.extraTimes > 0) economy.addExtraTimes(c.extraTimes)
        if (c.freezes > 0) economy.addStreakFreezes(c.freezes.coerceAtMost((com.korkoor.pardos.domain.economy.Economy.MAX_STREAK_FREEZES - economy.streakFreezes.value).coerceAtLeast(0)))
        if (c.rareChests > 0) CollectionManager(appContext).addChests(ChestType.RARE, c.rareChests)
    }

    // ---------------------------------------------------------------- ayuda por nivel atascado

    fun helpOfferedForLevel(level: Int): Boolean = prefs.getBoolean("help_level_$level", false)

    fun helpOfferedToday(today: Int = LocalDay.today()): Int = if (prefs.getInt("help_day", -1) == today) prefs.getInt("help_count", 0) else 0

    fun shouldOfferHelp(level: Int, consecutiveLosses: Int, today: Int = LocalDay.today()): Boolean =
        StruggleHelp.shouldOffer(consecutiveLosses, helpOfferedForLevel(level), helpOfferedToday(today))

    /** Se anota que se enseñó la ayuda (una vez por nivel y con tope diario), la compre o no. */
    fun markHelpOffered(level: Int, today: Int = LocalDay.today()) {
        prefs.edit().putBoolean("help_level_$level", true).putInt("help_day", today).putInt("help_count", helpOfferedToday(today) + 1).apply()
    }

    fun buyHelp(): Boolean {
        if (!economy.spendGems(StruggleHelp.priceGems)) return false
        grant(StruggleHelp.contents)
        return true
    }
}
