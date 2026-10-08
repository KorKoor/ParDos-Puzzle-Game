package com.korkoor.pardos.domain.model

enum class MissionType {
    PLAY_GAMES,         // Jugar N partidas (ganes o pierdas)
    WIN_LEVELS,         // Ganar N niveles de campaña
    MERGE_PAIRS,        // Hacer N combinaciones en total
    REACH_BLOCK,        // Lograr crear una ficha de valor N (ej. 32, 64)
    EARN_STARS,         // Ganar N estrellas en total hoy
    WIN_UNDER_TIME,     // Ganar un nivel en menos de N segundos
    WIN_NO_POWERUPS     // Ganar un nivel sin usar ayudas
}

data class DailyMission(
    val id: Int,
    val type: MissionType,
    val description: String,
    val targetValue: Int,       // Lo que hay que alcanzar
    val xpReward: Int,          // Recompensa
    var currentProgress: Int = 0,
    var isCompleted: Boolean = false
)

object MissionPool {
    // Aquí tienes una base sólida de casi 30 misiones variadas.
    // El sistema rotará entre ellas.
    val allMissions = listOf(
        // --- DE JUEGO Y COMPLETADO ---
        DailyMission(1, MissionType.PLAY_GAMES, "Juega 3 partidas", 3, 20),
        DailyMission(2, MissionType.PLAY_GAMES, "Juega 5 partidas", 5, 40),
        DailyMission(3, MissionType.WIN_LEVELS, "Completa 1 nivel", 1, 15),
        DailyMission(4, MissionType.WIN_LEVELS, "Completa 3 niveles", 3, 50),
        DailyMission(5, MissionType.WIN_LEVELS, "Supera 5 niveles", 5, 100),

        // --- DE COMBINACIÓN (MERGE PARES) ---
        DailyMission(6, MissionType.MERGE_PAIRS, "Combina 50 pares", 50, 30),
        DailyMission(7, MissionType.MERGE_PAIRS, "Combina 100 pares", 100, 60),
        DailyMission(8, MissionType.MERGE_PAIRS, "Combina 250 pares", 250, 100),

        // --- DE CREACIÓN DE FICHAS ---
        DailyMission(9, MissionType.REACH_BLOCK, "Crea una ficha de 16", 16, 15),
        DailyMission(10, MissionType.REACH_BLOCK, "Crea una ficha de 32", 32, 30),
        DailyMission(11, MissionType.REACH_BLOCK, "Crea una ficha de 64", 64, 50),
        DailyMission(12, MissionType.REACH_BLOCK, "Llega al bloque 128", 128, 100),

        // --- DE ESTRELLAS (CALIDAD DE JUEGO) ---
        DailyMission(13, MissionType.EARN_STARS, "Gana 3 estrellas hoy", 3, 25),
        DailyMission(14, MissionType.EARN_STARS, "Gana 6 estrellas hoy", 6, 50),
        DailyMission(15, MissionType.EARN_STARS, "Consigue 10 estrellas", 10, 80),

        // --- DE TIEMPO (RETO VELOZ) - Target en segundos ---
        DailyMission(16, MissionType.WIN_UNDER_TIME, "Gana en menos de 3 min", 180, 40),
        DailyMission(17, MissionType.WIN_UNDER_TIME, "Gana en menos de 2 min", 120, 60),
        DailyMission(18, MissionType.WIN_UNDER_TIME, "Gana en menos de 1 min", 60, 150), // ¡Extremo!

        // --- DE MAESTRÍA (SIN AYUDAS) ---
        DailyMission(19, MissionType.WIN_NO_POWERUPS, "Gana sin usar ayudas", 1, 50),
        DailyMission(20, MissionType.WIN_NO_POWERUPS, "Gana 2 veces sin ayudas", 2, 100)
    )
}