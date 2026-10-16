package com.korkoor.pardos.domain.logic

/** Tablero recomendado de la semana: una lista elegida a mano que rota cada semana (sin servidor, sin premios; es solo para probar algo distinto). */
data class WeeklyBoardPick(val name: String, val config: CustomLevelConfig)

object WeeklyBoard {
    private val picks = listOf(
        WeeklyBoardPick("Clásico apretado", CustomLevelConfig(3, 128, false, "Normal")),
        WeeklyBoardPick("Amplio y tranquilo", CustomLevelConfig(5, 256, true, "Zen")),
        WeeklyBoardPick("Reto sin ayudas", CustomLevelConfig(4, 256, false, "Pro")),
        WeeklyBoardPick("Maratón", CustomLevelConfig(6, 512, true, "Normal")),
        WeeklyBoardPick("Mini veloz", CustomLevelConfig(3, 64, true, "Pro")),
        WeeklyBoardPick("Equilibrio", CustomLevelConfig(4, 128, true, "Normal")),
        WeeklyBoardPick("Zen grande", CustomLevelConfig(6, 256, true, "Zen")),
        WeeklyBoardPick("Solo habilidad", CustomLevelConfig(5, 512, false, "Pro")),
    )

    /** Número de semana (la semana empieza el lunes) para un día contado desde 1970. */
    fun weekOf(epochDay: Long): Long = (epochDay + 3) / 7

    fun forWeek(week: Long): WeeklyBoardPick = picks[(((week % picks.size) + picks.size) % picks.size).toInt()]

    fun forDay(epochDay: Long): WeeklyBoardPick = forWeek(weekOf(epochDay))

    internal val all: List<WeeklyBoardPick> get() = picks
}
