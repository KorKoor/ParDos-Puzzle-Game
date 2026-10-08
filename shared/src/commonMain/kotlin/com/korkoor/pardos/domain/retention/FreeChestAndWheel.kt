package com.korkoor.pardos.domain.retention

import com.korkoor.pardos.domain.collection.ChestType
import com.korkoor.pardos.domain.economy.Economy
import kotlin.math.ceil
import kotlin.random.Random

/**
 * Cofre gratis por tiempo: cada pocas horas hay un cofre esperando. Es el motivo para abrir
 * la app varias veces al día. Se puede saltar la espera viendo un anuncio o con gemas.
 */
object FreeChest {
    const val COOLDOWN_MS: Long = Economy.FREE_CHEST_COOLDOWN_MS

    /** Milisegundos que faltan para el siguiente cofre. Si el reloj se atrasó, no regala nada. */
    fun remainingMs(lastClaimMs: Long, nowMs: Long): Long {
        if (lastClaimMs <= 0L) return 0L
        val elapsed = nowMs - lastClaimMs
        if (elapsed < 0L) return COOLDOWN_MS
        return (COOLDOWN_MS - elapsed).coerceAtLeast(0L)
    }

    fun isReady(lastClaimMs: Long, nowMs: Long): Boolean = remainingMs(lastClaimMs, nowMs) == 0L

    /** Cada quinto cofre gratis es raro; los demás, comunes. [claimedBefore] = cofres ya reclamados. */
    fun typeFor(claimedBefore: Int): ChestType =
        if ((claimedBefore + 1) % Economy.FREE_CHEST_RARE_EVERY == 0) ChestType.RARE else ChestType.COMMON

    /** Gemas para saltar la espera: 1 por cada media hora que falte (mínimo 1, máximo 8). */
    fun skipCostGems(remainingMs: Long): Int {
        if (remainingMs <= 0L) return 0
        return ceil(remainingMs / (30.0 * 60_000.0)).toInt().coerceIn(1, Economy.FREE_CHEST_SKIP_MAX_GEMS)
    }
}

enum class WheelKind { COINS, GEMS, CHEST, SEASON_POINTS }

data class WheelSlice(val kind: WheelKind, val amount: Int, val chest: ChestType? = null, val weight: Int) {
    val label: String
        get() = when (kind) {
            WheelKind.COINS -> "$amount"
            WheelKind.GEMS -> "$amount"
            WheelKind.CHEST -> when (chest) { ChestType.EPIC -> "Cofre épico"; ChestType.RARE -> "Cofre raro"; else -> "Cofre" }
            WheelKind.SEASON_POINTS -> "+$amount pts"
        }

    /** Valor aproximado en monedas, para balancear. */
    val coinValue: Int
        get() = when (kind) {
            WheelKind.COINS -> amount
            WheelKind.GEMS -> amount * Economy.GEM_COINS_VALUE
            WheelKind.CHEST -> when (chest) { ChestType.EPIC -> Economy.EPIC_CHEST_GEMS * Economy.GEM_COINS_VALUE; ChestType.RARE -> Economy.RARE_CHEST_COINS; else -> Economy.COMMON_CHEST_COINS }
            WheelKind.SEASON_POINTS -> amount * 2
        }
}

/** Ruleta diaria: un giro gratis al día y giros extra viendo anuncios. */
object DailyWheel {
    val slices: List<WheelSlice> = listOf(
        WheelSlice(WheelKind.COINS, 60, weight = 26),
        WheelSlice(WheelKind.GEMS, 3, weight = 14),
        WheelSlice(WheelKind.COINS, 120, weight = 20),
        WheelSlice(WheelKind.CHEST, 1, ChestType.COMMON, weight = 16),
        WheelSlice(WheelKind.COINS, 300, weight = 9),
        WheelSlice(WheelKind.SEASON_POINTS, 50, weight = 9),
        WheelSlice(WheelKind.GEMS, 10, weight = 3),
        WheelSlice(WheelKind.CHEST, 1, ChestType.RARE, weight = 3)
    )

    private val totalWeight = slices.sumOf { it.weight }

    /** Índice de la casilla ganadora, con probabilidad proporcional a su peso. */
    fun pick(random: Random): Int {
        var roll = random.nextInt(totalWeight)
        slices.forEachIndexed { i, s ->
            if (roll < s.weight) return i
            roll -= s.weight
        }
        return slices.lastIndex
    }

    /** Valor medio de un giro, en monedas. */
    fun expectedCoinValue(): Double = slices.sumOf { it.coinValue.toDouble() * it.weight } / totalWeight

    data class Allowance(val freeLeft: Int, val adLeft: Int) {
        val canSpin: Boolean get() = freeLeft > 0 || adLeft > 0
    }

    fun allowance(freeUsedToday: Int, adUsedToday: Int) = Allowance(
        freeLeft = (Economy.WHEEL_FREE_SPINS - freeUsedToday).coerceAtLeast(0),
        adLeft = (Economy.WHEEL_AD_SPINS - adUsedToday).coerceAtLeast(0)
    )
}
