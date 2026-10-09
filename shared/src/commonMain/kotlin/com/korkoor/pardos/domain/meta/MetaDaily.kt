package com.korkoor.pardos.domain.meta

import com.korkoor.pardos.domain.collection.ChestType
import com.korkoor.pardos.domain.retention.HappyHour
import com.korkoor.pardos.domain.retention.LoginCalendar

/** Resultado de cobrar una casilla del calendario: el premio y, si completó un hito del mes, el cofre extra. */
internal class CalendarGrant(val day: Int, val prize: LoginCalendar.Prize, val bonusChest: String?)

/**
 * Calendario de conexión del mes. Estado: mes en curso (día desde 1970 en que empieza), casillas cobradas (una máscara de bits,
 * bit 0 = día 1), recuperaciones gratis usadas y meses completos. Si el reloj del teléfono retrocede a un mes anterior, el calendario
 * se queda quieto (no se puede cobrar dos veces cambiando la fecha).
 */
internal class CalendarOps(
    private val s: MetaStore,
    private val wallet: Wallet,
    private val col: CollectionOps,
    private val clock: Clock
) {
    /** Pone el mes al día. Devuelve false si el reloj está en un mes anterior al guardado. */
    private fun sync(): Boolean {
        val start = LoginCalendar.monthStart(clock.today)
        val saved = s.int("cal_month", -1)
        if (saved == start) return true
        if (saved != -1 && start < saved) return false
        if (saved != -1) {
            val old = LoginCalendar.civilFromDays(saved)
            if (claimedCount() >= LoginCalendar.daysInMonth(old.year, old.month)) s.addInt("cal_full_months", 1)
        }
        s.setInt("cal_month", start)
        s.setInt("cal_mask", 0)
        s.setInt("cal_free", 0)
        return true
    }

    private fun bit(day: Int): Int = 1 shl (day - 1)

    private fun isClaimed(day: Int): Boolean = (s.int("cal_mask") and bit(day)) != 0

    fun claimedCount(): Int {
        var m = s.int("cal_mask")
        var n = 0
        while (m != 0) {
            n += m and 1
            m = m ushr 1
        }
        return n
    }

    fun fullMonths(): Int = s.int("cal_full_months")

    fun claimable(): Boolean = sync() && !isClaimed(LoginCalendar.civilFromDays(clock.today).day)

    fun todayClaimed(): Boolean = sync() && isClaimed(LoginCalendar.civilFromDays(clock.today).day)

    fun recoverCost(): Int = if (s.int("cal_free") < LoginCalendar.FREE_RECOVERIES) 0 else LoginCalendar.RECOVER_GEMS

    fun claimToday(): CalendarGrant? {
        if (!sync()) return null
        val c = LoginCalendar.civilFromDays(clock.today)
        if (isClaimed(c.day)) return null
        return grant(c.day, c.year, c.month)
    }

    fun recover(day: Int): CalendarGrant? {
        if (!sync()) return null
        val c = LoginCalendar.civilFromDays(clock.today)
        if (!LoginCalendar.canRecover(day, c.day) || isClaimed(day)) return null
        val cost = recoverCost()
        if (cost > 0 && !wallet.spendGems(cost)) return null
        if (cost == 0) s.addInt("cal_free", 1)
        return grant(day, c.year, c.month)
    }

    private fun grant(day: Int, year: Int, month: Int): CalendarGrant {
        s.setInt("cal_mask", s.int("cal_mask") or bit(day))
        val prize = LoginCalendar.prizeFor(day, LoginCalendar.daysInMonth(year, month))
        wallet.addCoins(prize.coins)
        wallet.addGems(prize.gems)
        addChest(prize.chest)
        val bonus = LoginCalendar.MONTH_BONUS[claimedCount()]
        addChest(bonus)
        return CalendarGrant(day, prize, bonus)
    }

    private fun addChest(name: String?) {
        if (name == null) return
        val type = ChestType.entries.firstOrNull { it.name == name } ?: return
        col.addChests(type, 1)
    }

    fun json(): Raw {
        if (!sync()) return Raw(obj("frozen" to true, "cells" to emptyList<Any>()))
        val c = LoginCalendar.civilFromDays(clock.today)
        val dim = LoginCalendar.daysInMonth(c.year, c.month)
        val cells = (1..dim).map { d ->
            val prize = LoginCalendar.prizeFor(d, dim)
            val status = when {
                isClaimed(d) -> if (d == c.day) "todayDone" else "claimed"
                d == c.day -> "today"
                d < c.day -> if (LoginCalendar.canRecover(d, c.day)) "recoverable" else "missed"
                else -> "future"
            }
            robj("day" to d, "status" to status, "coins" to prize.coins, "gems" to prize.gems, "chest" to prize.chest, "big" to prize.big)
        }
        val nextBonus = LoginCalendar.MONTH_BONUS.keys.sorted().firstOrNull { it > claimedCount() }
        return Raw(obj(
            "frozen" to false, "year" to c.year, "month" to c.month, "daysInMonth" to dim, "today" to c.day,
            "claimed" to claimedCount(), "claimable" to claimable(), "recoverCost" to recoverCost(),
            "freeLeft" to (LoginCalendar.FREE_RECOVERIES - s.int("cal_free")).coerceAtLeast(0),
            "nextBonusAt" to nextBonus, "nextBonusChest" to nextBonus?.let { LoginCalendar.MONTH_BONUS[it] },
            "fullMonths" to fullMonths(), "cells" to cells
        ))
    }
}

/** Hora feliz: aplica el bono a las monedas de una victoria y recuerda cuántas monedas extra se llevan hoy. */
internal class HappyHourOps(private val s: MetaStore, private val clock: Clock) {
    fun state(): HappyHour.State = HappyHour.stateAt(clock.today, clock.minute)

    fun isActive(): Boolean = state().phase == HappyHour.Phase.ACTIVE

    /** Devuelve las monedas con el bono (si es hora feliz) y apunta lo ganado de más. */
    fun apply(coins: Int): Int {
        val total = HappyHour.applyCoins(coins, clock.today, clock.minute)
        val extra = total - coins
        if (extra > 0) {
            if (s.int("hh_day", -1) != clock.today) {
                s.setInt("hh_day", clock.today)
                s.setInt("hh_extra", 0)
            }
            s.addInt("hh_extra", extra)
        }
        return total
    }

    fun extraToday(): Int = if (s.int("hh_day", -1) == clock.today) s.int("hh_extra") else 0

    fun json(): Raw {
        val st = state()
        return Raw(obj(
            "phase" to st.phase.name, "startMin" to st.window.startMin, "endMin" to st.window.endMin,
            "minutes" to st.minutes, "extraPct" to HappyHour.EXTRA_COINS_PCT, "extraToday" to extraToday()
        ))
    }
}
