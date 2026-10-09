package com.korkoor.pardos.domain.shop

import com.korkoor.pardos.domain.economy.Economy

/**
 * Qué se le enseña al jugador en la tarjeta de ofertas del menú. Siempre algo verdadero y útil para él:
 * lo que ya tiene (VIP, pack inicial) no se vuelve a ofrecer, no hay cuentas atrás falsas y la tarjeta rota entre varias
 * opciones en cada visita para no cansar.
 */
enum class PromoKind { STARTER_PACK, VIP, DAILY_OFFER, FREE_GEMS }

object MenuPromo {
    /** El pack inicial se ofrece cuando ya conoce el juego, no en los primeros niveles. */
    const val STARTER_MIN_LEVEL = 4
    /** El VIP (sin anuncios) se ofrece cuando ya ha visto algunos. */
    const val VIP_MIN_LEVEL = 15

    data class State(
        val vip: Boolean,
        val starterClaimed: Boolean,
        val campaignLevel: Int,
        val freeGemsLeft: Int,
        /** Cuántas veces se ha abierto el menú hoy: sirve para rotar la tarjeta. */
        val visit: Int
    )

    fun eligible(s: State): List<PromoKind> = buildList {
        if (!s.starterClaimed && s.campaignLevel >= STARTER_MIN_LEVEL) add(PromoKind.STARTER_PACK)
        if (!s.vip && s.campaignLevel >= VIP_MIN_LEVEL) add(PromoKind.VIP)
        add(PromoKind.DAILY_OFFER)
        if (!s.vip && s.freeGemsLeft > 0) add(PromoKind.FREE_GEMS)
    }

    fun pick(s: State): PromoKind {
        val list = eligible(s)
        return list[s.visit.mod(list.size)]
    }

    /** Lo que vale el pack inicial en gemas "de tienda" (para decir cuánto se ahorra, sin inventar cifras). */
    val starterGems: Int get() = Economy.STARTER_GEMS
}
