package com.korkoor.pardos.domain.achievements

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import com.korkoor.pardos.domain.model.GameMode
import com.korkoor.pardos.R

object gameAchievements {
    // Lista masiva de logros para el juego (80+ achievements)
    val all: List<Achievement> = listOf(
        // ========== LOGROS DE INICIO Y PROGRESIÓN ==========
        Achievement(
            id = "first_win",
            titleResId = R.string.ach_first_win_title,
            descriptionResId = R.string.ach_first_win_desc,
            icon = Icons.Filled.EmojiEvents,
            color = Color(0xFF81B29A),
            condition = { it.isLevelCompleted && it.currentLevel == 1 }
        ),
        Achievement(
            id = "getting_warmed_up",
            titleResId = R.string.ach_warmup_title,
            descriptionResId = R.string.ach_warmup_desc,
            icon = Icons.Default.LocalFireDepartment,
            color = Color(0xFFE07A5F),
            condition = { it.isLevelCompleted && it.currentLevel == 2 }
        ),
        Achievement(
            id = "level_5",
            titleResId = R.string.ach_level_5_title,
            descriptionResId = R.string.ach_level_5_desc,
            icon = Icons.Default.TrendingUp,
            color = Color(0xFFF2CC8F),
            condition = { it.isLevelCompleted && it.currentLevel == 4 }
        ),
        Achievement(
            id = "level_10",
            titleResId = R.string.ach_level_10_title,
            descriptionResId = R.string.ach_level_10_desc,
            icon = Icons.Default.Star,
            color = Color(0xFFC0C0C0),
            condition = { it.isLevelCompleted && it.currentLevel == 9 }
        ),
        Achievement(
            id = "level_15",
            titleResId = R.string.ach_level_15_title,
            descriptionResId = R.string.ach_level_15_desc,
            icon = Icons.Default.PersonalVideo,
            color = Color(0xFFC0C0C0),
            condition = { it.isLevelCompleted && it.currentLevel == 14 }
        ),
        Achievement(
            id = "level_20",
            titleResId = R.string.ach_level_20_title,
            descriptionResId = R.string.ach_level_20_desc,
            icon = Icons.Default.Stars,
            color = Color(0xFFFFD700),
            condition = { it.isLevelCompleted && it.currentLevel == 19 }
        ),
        Achievement(
            id = "level_25",
            titleResId = R.string.ach_level_25_title,
            descriptionResId = R.string.ach_level_25_desc,
            icon = Icons.Default.Grade,
            color = Color(0xFFFFD700),
            condition = { it.isLevelCompleted && it.currentLevel == 24 }
        ),
        Achievement(
            id = "level_30",
            titleResId = R.string.ach_level_30_title,
            descriptionResId = R.string.ach_level_30_desc,
            icon = Icons.Default.Stadium,
            color = Color(0xFFFFD700),
            condition = { it.isLevelCompleted && it.currentLevel == 29 }
        ),
        Achievement(
            id = "level_40",
            titleResId = R.string.ach_level_40_title,
            descriptionResId = R.string.ach_level_40_desc,
            icon = Icons.Default.School,
            color = Color(0xFF9C27B0),
            condition = { it.isLevelCompleted && it.currentLevel == 39 }
        ),
        Achievement(
            id = "level_50",
            titleResId = R.string.ach_level_50_title,
            descriptionResId = R.string.ach_level_50_desc,
            icon = Icons.Default.WorkspacePremium,
            color = Color(0xFF9C27B0),
            condition = { it.isLevelCompleted && it.currentLevel == 49 }
        ),
        Achievement(
            id = "level_75",
            titleResId = R.string.ach_level_75_title,
            descriptionResId = R.string.ach_level_75_desc,
            icon = Icons.Default.AutoAwesome,
            color = Color(0xFFFF6B6B),
            condition = { it.isLevelCompleted && it.currentLevel == 74 }
        ),
        Achievement(
            id = "level_100",
            titleResId = R.string.ach_level_100_title,
            descriptionResId = R.string.ach_level_100_desc,
            icon = Icons.Default.Celebration,
            color = Color(0xFFFF1744),
            condition = { it.isLevelCompleted && it.currentLevel == 99 }
        ),

        // ========== LOGROS DE FICHAS ==========
        Achievement(
            id = "tile_16",
            titleResId = R.string.ach_tile_16_title,
            descriptionResId = R.string.ach_tile_16_desc,
            icon = Icons.Default.Looks,
            color = Color(0xFFF59563),
            condition = { it.tiles.any { tile -> tile.value >= 16 } }
        ),
        Achievement(
            id = "tile_32",
            titleResId = R.string.ach_tile_32_title,
            descriptionResId = R.string.ach_tile_32_desc,
            icon = Icons.Default.Filter1,
            color = Color(0xFFF67C5F),
            condition = { it.tiles.any { tile -> tile.value >= 32 } }
        ),
        Achievement(
            id = "tile_64",
            titleResId = R.string.ach_tile_64_title,
            descriptionResId = R.string.ach_tile_64_desc,
            icon = Icons.Default.Filter2,
            color = Color(0xFFF65E3B),
            condition = { it.tiles.any { tile -> tile.value >= 64 } }
        ),
        Achievement(
            id = "tile_128",
            titleResId = R.string.ach_tile_128_title,
            descriptionResId = R.string.ach_tile_128_desc,
            icon = Icons.Default.Filter3,
            color = Color(0xFFEDCF72),
            condition = { it.tiles.any { tile -> tile.value >= 128 } }
        ),
        Achievement(
            id = "tile_256",
            titleResId = R.string.ach_tile_256_title,
            descriptionResId = R.string.ach_tile_256_desc,
            icon = Icons.Default.Filter4,
            color = Color(0xFFEDCC61),
            condition = { it.tiles.any { tile -> tile.value >= 256 } }
        ),
        Achievement(
            id = "tile_512",
            titleResId = R.string.ach_tile_512_title,
            descriptionResId = R.string.ach_tile_512_desc,
            icon = Icons.Default.Filter5,
            color = Color(0xFFEDC850),
            condition = { it.tiles.any { tile -> tile.value >= 512 } }
        ),
        Achievement(
            id = "tile_1024",
            titleResId = R.string.ach_tile_1024_title,
            descriptionResId = R.string.ach_tile_1024_desc,
            icon = Icons.Default.Filter6,
            color = Color(0xFFEDC53F),
            condition = { it.tiles.any { tile -> tile.value >= 1024 } }
        ),
        Achievement(
            id = "tile_2048",
            titleResId = R.string.ach_tile_2048_title,
            descriptionResId = R.string.ach_tile_2048_desc,
            icon = Icons.Default.Verified,
            color = Color(0xFFEDC22E),
            condition = { it.tiles.any { tile -> tile.value >= 2048 } }
        ),
        Achievement(
            id = "tile_4096",
            titleResId = R.string.ach_tile_4096_title,
            descriptionResId = R.string.ach_tile_4096_desc,
            icon = Icons.Default.Rocket,
            color = Color(0xFF9C27B0),
            condition = { it.tiles.any { tile -> tile.value >= 4096 } }
        ),
        Achievement(
            id = "tile_8192",
            titleResId = R.string.ach_tile_8192_title,
            descriptionResId = R.string.ach_tile_8192_desc,
            icon = Icons.Default.FlightTakeoff,
            color = Color(0xFFFF1744),
            condition = { it.tiles.any { tile -> tile.value >= 8192 } }
        ),

        // ========== LOGROS DE VELOCIDAD ==========
        Achievement(
            id = "speedrun",
            titleResId = R.string.ach_speedrun_title,
            descriptionResId = R.string.ach_speedrun_desc,
            icon = Icons.Default.FlashOn,
            color = Color(0xFFE07A5F),
            condition = { it.isLevelCompleted && it.moveCount < 50 }
        ),
        Achievement(
            id = "speed_40",
            titleResId = R.string.ach_speed_40_title,
            descriptionResId = R.string.ach_speed_40_desc,
            icon = Icons.Default.Speed,
            color = Color(0xFFE07A5F),
            condition = { it.isLevelCompleted && it.moveCount < 40 }
        ),
        Achievement(
            id = "speed_30",
            titleResId = R.string.ach_speed_30_title,
            descriptionResId = R.string.ach_speed_30_desc,
            icon = Icons.Default.Air,
            color = Color(0xFFFF9800),
            condition = { it.isLevelCompleted && it.moveCount < 30 }
        ),
        Achievement(
            id = "speed_20",
            titleResId = R.string.ach_speed_20_title,
            descriptionResId = R.string.ach_speed_20_desc,
            icon = Icons.Default.Flight,
            color = Color(0xFFFFD700),
            condition = { it.isLevelCompleted && it.moveCount < 20 }
        ),
        Achievement(
            id = "speed_15",
            titleResId = R.string.ach_speed_15_title,
            descriptionResId = R.string.ach_speed_15_desc,
            icon = Icons.Default.CheckCircle,
            color = Color(0xFF9C27B0),
            condition = { it.isLevelCompleted && it.moveCount < 15 }
        ),
        Achievement(
            id = "speed_10",
            titleResId = R.string.ach_speed_10_title,
            descriptionResId = R.string.ach_speed_10_desc,
            icon = Icons.Default.Diamond,
            color = Color(0xFFFF6B6B),
            condition = { it.isLevelCompleted && it.moveCount < 10 }
        ),

        // ========== LOGROS DE TIEMPO ==========
        Achievement(
            id = "time_60",
            titleResId = R.string.ach_time_60_title,
            descriptionResId = R.string.ach_time_60_desc,
            icon = Icons.Default.Timer,
            color = Color(0xFFFFD700),
            condition = { it.isLevelCompleted && it.elapsedTime < 60 }
        ),
        Achievement(
            id = "time_45",
            titleResId = R.string.ach_time_45_title,
            descriptionResId = R.string.ach_time_45_desc,
            icon = Icons.Default.TimerOff,
            color = Color(0xFFFFD700),
            condition = { it.isLevelCompleted && it.elapsedTime < 45 }
        ),
        Achievement(
            id = "time_30",
            titleResId = R.string.ach_time_30_title,
            descriptionResId = R.string.ach_time_30_desc,
            icon = Icons.Default.Alarm,
            color = Color(0xFFFF9800),
            condition = { it.isLevelCompleted && it.elapsedTime < 30 }
        ),
        Achievement(
            id = "time_15",
            titleResId = R.string.ach_time_15_title,
            descriptionResId = R.string.ach_time_15_desc,
            icon = Icons.Filled.Bolt,
            color = Color(0xFFFF6B6B),
            condition = { it.isLevelCompleted && it.elapsedTime < 15 }
        ),

        // ========== LOGROS DE RESISTENCIA ==========
        Achievement(
            id = "century",
            titleResId = R.string.ach_century_title,
            descriptionResId = R.string.ach_century_desc,
            icon = Icons.Default.FitnessCenter,
            color = Color(0xFF3D405B),
            condition = { it.moveCount >= 100 }
        ),
        Achievement(
            id = "moves_150",
            titleResId = R.string.ach_moves_150_title,
            descriptionResId = R.string.ach_moves_150_desc,
            icon = Icons.Default.DirectionsRun,
            color = Color(0xFF3D405B),
            condition = { it.moveCount >= 150 }
        ),
        Achievement(
            id = "moves_200",
            titleResId = R.string.ach_moves_200_title,
            descriptionResId = R.string.ach_moves_200_desc,
            icon = Icons.Default.SportsScore,
            color = Color(0xFF6C63FF),
            condition = { it.moveCount >= 200 }
        ),
        Achievement(
            id = "moves_300",
            titleResId = R.string.ach_moves_300_title,
            descriptionResId = R.string.ach_moves_300_desc,
            icon = Icons.Default.SportsMartialArts,
            color = Color(0xFF9C27B0),
            condition = { it.moveCount >= 300 }
        ),
        Achievement(
            id = "moves_500",
            titleResId = R.string.ach_moves_500_title,
            descriptionResId = R.string.ach_moves_500_desc,
            icon = Icons.Default.LocalFireDepartment,
            color = Color(0xFFFF6B6B),
            condition = { it.moveCount >= 500 }
        ),
        Achievement(
            id = "time_5min",
            titleResId = R.string.ach_time_5min_title,
            descriptionResId = R.string.ach_time_5min_desc,
            icon = Icons.Default.AccessTime,
            color = Color(0xFF81B29A),
            condition = { it.elapsedTime >= 300 }
        ),
        Achievement(
            id = "time_10min",
            titleResId = R.string.ach_time_10min_title,
            descriptionResId = R.string.ach_time_10min_desc,
            icon = Icons.Default.Schedule,
            color = Color(0xFF6C63FF),
            condition = { it.elapsedTime >= 600 }
        ),
        Achievement(
            id = "time_20min",
            titleResId = R.string.ach_time_20min_title,
            descriptionResId = R.string.ach_time_20min_desc,
            icon = Icons.Default.MoreTime,
            color = Color(0xFF9C27B0),
            condition = { it.elapsedTime >= 1200 }
        ),
        Achievement(
            id = "time_30min",
            titleResId = R.string.ach_time_30min_title,
            descriptionResId = R.string.ach_time_30min_desc,
            icon = Icons.Default.HourglassFull,
            color = Color(0xFFFF6B6B),
            condition = { it.elapsedTime >= 1800 }
        ),

        // ========== LOGROS DE PUNTUACIÓN ==========
        Achievement(
            id = "score_500",
            titleResId = R.string.ach_score_500_title,
            descriptionResId = R.string.ach_score_500_desc,
            icon = Icons.Default.Score,
            color = Color(0xFF81B29A),
            condition = { it.score >= 500 }
        ),
        Achievement(
            id = "score_1k",
            titleResId = R.string.ach_score_1k_title,
            descriptionResId = R.string.ach_score_1k_desc,
            icon = Icons.Default.MonetizationOn,
            color = Color(0xFF81B29A),
            condition = { it.score >= 1000 }
        ),
        Achievement(
            id = "score_2500",
            titleResId = R.string.ach_score_2500_title,
            descriptionResId = R.string.ach_score_2500_desc,
            icon = Icons.Default.TrendingUp,
            color = Color(0xFFC0C0C0),
            condition = { it.score >= 2500 }
        ),
        Achievement(
            id = "score_5k",
            titleResId = R.string.ach_score_5k_title,
            descriptionResId = R.string.ach_score_5k_desc,
            icon = Icons.Default.Paid,
            color = Color(0xFFC0C0C0),
            condition = { it.score >= 5000 }
        ),
        Achievement(
            id = "score_7500",
            titleResId = R.string.ach_score_7500_title,
            descriptionResId = R.string.ach_score_7500_desc,
            icon = Icons.Default.AttachMoney,
            color = Color(0xFFFFD700),
            condition = { it.score >= 7500 }
        ),
        Achievement(
            id = "score_10k",
            titleResId = R.string.ach_score_10k_title,
            descriptionResId = R.string.ach_score_10k_desc,
            icon = Icons.Default.Grade,
            color = Color(0xFFFFD700),
            condition = { it.score >= 10000 }
        ),
        Achievement(
            id = "score_15k",
            titleResId = R.string.ach_score_15k_title,
            descriptionResId = R.string.ach_score_15k_desc,
            icon = Icons.Default.StarRate,
            color = Color(0xFF9C27B0),
            condition = { it.score >= 15000 }
        ),
        Achievement(
            id = "score_25k",
            titleResId = R.string.ach_score_25k_title,
            descriptionResId = R.string.ach_score_25k_desc,
            icon = Icons.Default.Stars,
            color = Color(0xFF9C27B0),
            condition = { it.score >= 25000 }
        ),
        Achievement(
            id = "score_50k",
            titleResId = R.string.ach_score_50k_title,
            descriptionResId = R.string.ach_score_50k_desc,
            icon = Icons.Default.AutoAwesome,
            color = Color(0xFFFF6B6B),
            condition = { it.score >= 50000 }
        ),
        Achievement(
            id = "score_100k",
            titleResId = R.string.ach_score_100k_title,
            descriptionResId = R.string.ach_score_100k_desc,
            icon = Icons.Default.Celebration,
            color = Color(0xFFFF1744),
            condition = { it.score >= 100000 }
        ),

        // ========== LOGROS DE COMBOS ==========
        Achievement(
            id = "combo_3",
            titleResId = R.string.ach_combo_3_title,
            descriptionResId = R.string.ach_combo_3_desc,
            icon = Icons.Default.Whatshot,
            color = Color(0xFFFF9800),
            condition = { it.combo >= 3 }
        ),
        Achievement(
            id = "combo_5",
            titleResId = R.string.ach_combo_5_title,
            descriptionResId = R.string.ach_combo_5_desc,
            icon = Icons.Default.LocalFireDepartment,
            color = Color(0xFFFF5722),
            condition = { it.combo >= 5 }
        ),
        Achievement(
            id = "combo_7",
            titleResId = R.string.ach_combo_7_title,
            descriptionResId = R.string.ach_combo_7_desc,
            icon = Icons.Default.Fireplace,
            color = Color(0xFFE91E63),
            condition = { it.combo >= 7 }
        ),
        Achievement(
            id = "combo_10",
            titleResId = R.string.ach_combo_10_title,
            descriptionResId = R.string.ach_combo_10_desc,
            icon = Icons.Default.Flare,
            color = Color(0xFFFFD700),
            condition = { it.combo >= 10 }
        ),
        Achievement(
            id = "combo_15",
            titleResId = R.string.ach_combo_15_title,
            descriptionResId = R.string.ach_combo_15_desc,
            icon = Icons.Default.Bolt,
            color = Color(0xFF9C27B0),
            condition = { it.combo >= 15 }
        ),
        Achievement(
            id = "combo_20",
            titleResId = R.string.ach_combo_20_title,
            descriptionResId = R.string.ach_combo_20_desc,
            icon = Icons.Default.FlashOn,
            color = Color(0xFFFF6B6B),
            condition = { it.combo >= 20 }
        ),
        Achievement(
            id = "combo_30",
            titleResId = R.string.ach_combo_30_title,
            descriptionResId = R.string.ach_combo_30_desc,
            icon = Icons.Default.AutoAwesome,
            color = Color(0xFFFF1744),
            condition = { it.combo >= 30 }
        ),

        // ========== LOGROS DE ESTRATEGIA ==========
        Achievement(
            id = "full_board",
            titleResId = R.string.ach_full_board_title,
            descriptionResId = R.string.ach_full_board_desc,
            icon = Icons.Default.GridOn,
            color = Color(0xFF6C63FF),
            condition = { it.emptySpaces == 0 && it.hasMovesAvailable }
        ),
        Achievement(
            id = "minimal_tiles",
            titleResId = R.string.ach_minimal_tiles_title,
            descriptionResId = R.string.ach_minimal_tiles_desc,
            icon = Icons.Default.Apps,
            color = Color(0xFF607D8B),
            condition = { it.isLevelCompleted && it.tiles.size < 5 }
        ),
        Achievement(
            id = "clutch_victory",
            titleResId = R.string.ach_clutch_victory_title,
            descriptionResId = R.string.ach_clutch_victory_desc,
            icon = Icons.Default.HealthAndSafety,
            color = Color(0xFFFF5722),
            condition = { it.isLevelCompleted && it.emptySpaces == 1 }
        ),
        Achievement(
            id = "close_call",
            titleResId = R.string.ach_close_call_title,
            descriptionResId = R.string.ach_close_call_desc,
            icon = Icons.Default.Warning,
            color = Color(0xFFFF9800),
            condition = { it.isLevelCompleted && it.emptySpaces <= 2 }
        ),
        Achievement(
            id = "efficiency_king",
            titleResId = R.string.ach_efficiency_king_title,
            descriptionResId = R.string.ach_efficiency_king_desc,
            icon = Icons.Default.CheckCircleOutline,
            color = Color(0xFF9C27B0),
            condition = { it.isLevelCompleted && it.moveCount <= it.currentLevel * 3 }
        ),

        // ========== LOGROS POR MODO DE JUEGO ==========
        Achievement(
            id = "classic_beginner",
            titleResId = R.string.ach_classic_beginner_title,
            descriptionResId = R.string.ach_classic_beginner_desc,
            icon = Icons.Default.School,
            color = Color(0xFF81B29A),
            condition = { it.gameMode == GameMode.CLASICO && it.isLevelCompleted && it.currentLevel == 4 }
        ),
        Achievement(
            id = "classic_intermediate",
            titleResId = R.string.ach_classic_intermediate_title,
            descriptionResId = R.string.ach_classic_intermediate_desc,
            icon = Icons.Filled.EmojiEvents,
            color = Color(0xFF81B29A),
            condition = { it.gameMode == GameMode.CLASICO && it.isLevelCompleted && it.currentLevel == 9 }
        ),
        Achievement(
            id = "classic_master",
            titleResId = R.string.ach_classic_master_title,
            descriptionResId = R.string.ach_classic_master_desc,
            icon = Icons.Default.WorkspacePremium,
            color = Color(0xFF81B29A),
            condition = { it.gameMode == GameMode.CLASICO && it.isLevelCompleted && it.currentLevel == 19 }
        ),
        Achievement(
            id = "challenge_completed",
            titleResId = R.string.ach_challenge_completed_title,
            descriptionResId = R.string.ach_challenge_completed_desc,
            icon = Icons.Default.Flag,
            color = Color(0xFFE07A5F),
            condition = { it.gameMode == GameMode.DESAFIO && it.isLevelCompleted }
        ),
        Achievement(
            id = "challenge_level_5",
            titleResId = R.string.ach_challenge_level_5_title,
            descriptionResId = R.string.ach_challenge_level_5_desc,
            icon = Icons.Default.MilitaryTech,
            color = Color(0xFFE07A5F),
            condition = { it.gameMode == GameMode.DESAFIO && it.isLevelCompleted && it.currentLevel == 4 }
        ),
        Achievement(
            id = "zen_master",
            titleResId = R.string.ach_zen_master_title,
            descriptionResId = R.string.ach_zen_master_desc,
            icon = Icons.Default.Spa,
            color = Color(0xFF6C63FF),
            condition = { it.gameMode == GameMode.ZEN && it.isLevelCompleted && it.currentLevel == 4 }
        ),
        Achievement(
            id = "zen_enlightened",
            titleResId = R.string.ach_zen_enlightened_title,
            descriptionResId = R.string.ach_zen_enlightened_desc,
            icon = Icons.Default.EmojiObjects,
            color = Color(0xFF6C63FF),
            condition = { it.gameMode == GameMode.ZEN && it.isLevelCompleted && it.currentLevel == 9 }
        ),
        Achievement(
            id = "rapid_fire",
            titleResId = R.string.ach_rapid_fire_title,
            descriptionResId = R.string.ach_rapid_fire_desc,
            icon = Icons.Default.Bolt,
            color = Color(0xFFF4A261),
            condition = { it.gameMode == GameMode.RAPIDO && it.isLevelCompleted && it.currentLevel == 3 }
        ),
        Achievement(
            id = "speed_demon",
            titleResId = R.string.ach_speed_demon_title,
            descriptionResId = R.string.ach_speed_demon_desc,
            icon = Icons.Default.RocketLaunch,
            color = Color(0xFFF4A261),
            condition = { it.gameMode == GameMode.RAPIDO && it.isLevelCompleted && it.currentLevel == 9 }
        ),

        // ========== LOGROS ESPECIALES Y SECRETOS ==========
        Achievement(
            id = "lucky_seven",
            titleResId = R.string.ach_lucky_seven_title,
            descriptionResId = R.string.ach_lucky_seven_desc,
            icon = Icons.Default.Casino,
            color = Color(0xFF4CAF50),
            condition = { it.currentLevel == 7 && it.isLevelCompleted }
        ),
        Achievement(
            id = "thirteen",
            titleResId = R.string.ach_thirteen_title,
            descriptionResId = R.string.ach_thirteen_desc,
            icon = Icons.Default.Psychology,
            color = Color(0xFF212121),
            condition = { it.isLevelCompleted && it.currentLevel == 12 }
        ),
        Achievement(
            id = "double_double",
            titleResId = R.string.ach_double_double_title,
            descriptionResId = R.string.ach_double_double_desc,
            icon = Icons.Default.ContentCopy,
            color = Color(0xFF3F51B5),
            condition = {
                it.tiles.groupBy { tile -> tile.value }
                    .count { group -> group.value.size >= 2 } >= 2
            }
        ),
        Achievement(
            id = "quad_squad",
            titleResId = R.string.ach_quad_squad_title,
            descriptionResId = R.string.ach_quad_squad_desc,
            icon = Icons.Default.Collections,
            color = Color(0xFF9C27B0),
            condition = {
                it.tiles.groupBy { tile -> tile.value }.any { group -> group.value.size >= 4 }
            }
        ),
        Achievement(
            id = "corner_king",
            titleResId = R.string.ach_corner_king_title,
            descriptionResId = R.string.ach_corner_king_desc,
            icon = Icons.Default.CropSquare,
            color = Color(0xFF795548),
            condition = {
                val corners = listOf(
                    0 to 0,
                    0 to (it.boardSize - 1),
                    (it.boardSize - 1) to 0,
                    (it.boardSize - 1) to (it.boardSize - 1)
                )
                corners.all { corner ->
                    it.tiles.any { tile -> tile.row == corner.first && tile.col == corner.second }
                }
            }
        ),
        Achievement(
            id = "power_of_two",
            titleResId = R.string.ach_power_of_two_title,
            descriptionResId = R.string.ach_power_of_two_desc,
            icon = Icons.Default.Functions,
            color = Color(0xFF00BCD4),
            condition = {
                it.tiles.isNotEmpty() && it.tiles.all { tile ->
                    val v = tile.value
                    v > 0 && (v and (v - 1)) == 0
                }
            }
        ),
        Achievement(
            id = "symmetry",
            titleResId = R.string.ach_symmetry_title,
            descriptionResId = R.string.ach_symmetry_desc,
            icon = Icons.Default.Balance,
            color = Color(0xFF009688),
            condition = {
                it.tiles.all { tile ->
                    val mirror = it.tiles.find { t ->
                        t.col == tile.col && t.row == (it.boardSize - 1 - tile.row)
                    }
                    mirror?.value == tile.value
                }
            }
        ),
        Achievement(
            id = "center_piece",
            titleResId = R.string.ach_center_piece_title,
            descriptionResId = R.string.ach_center_piece_desc,
            icon = Icons.Default.CenterFocusStrong,
            color = Color(0xFFFF6B6B),
            condition = {
                val maxTile = it.tiles.maxByOrNull { tile -> tile.value }
                val center = it.boardSize / 2
                maxTile?.let { tile ->
                    (tile.row == center || tile.row == center - 1) &&
                            (tile.col == center || tile.col == center - 1)
                } ?: false
            }
        ),
        Achievement(
            id = "edge_lord",
            titleResId = R.string.ach_edge_lord_title,
            descriptionResId = R.string.ach_edge_lord_desc,
            icon = Icons.Default.BorderOuter,
            color = Color(0xFF607D8B),
            condition = {
                it.tiles.isNotEmpty() && it.tiles.all { tile ->
                    tile.row == 0 || tile.row == it.boardSize - 1 ||
                            tile.col == 0 || tile.col == it.boardSize - 1
                }
            }
        ),
        Achievement(
            id = "fibonacci",
            titleResId = R.string.ach_fibonacci_title,
            descriptionResId = R.string.ach_fibonacci_desc,
            icon = Icons.Default.Analytics,
            color = Color(0xFFFFEB3B),
            condition = {
                val fib = listOf(2, 8, 32, 128, 512, 2048)
                it.tiles.any { tile -> tile.value in fib }
            }
        )
    )
}