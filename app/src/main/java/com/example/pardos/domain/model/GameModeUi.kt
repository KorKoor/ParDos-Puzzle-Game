package com.korkoor.pardos.domain.model

import androidx.compose.ui.graphics.Color
import com.korkoor.pardos.R

// Propiedades de UI de GameMode (el enum vive en :shared y no conoce Android).

val GameMode.nameResId: Int
    get() = when (this) {
        GameMode.CLASICO -> R.string.mode_classic
        GameMode.DESAFIO -> R.string.mode_challenge
        GameMode.ZEN -> R.string.mode_zen
        GameMode.RAPIDO -> R.string.mode_fast
        GameMode.TABLAS -> R.string.mode_tables
        GameMode.CARRERA -> R.string.mode_race
        GameMode.CUSTOM -> R.string.mode_custom
    }

val GameMode.descriptionResId: Int
    get() = when (this) {
        GameMode.CLASICO -> R.string.mode_classic_desc
        GameMode.DESAFIO -> R.string.mode_challenge_desc
        GameMode.ZEN -> R.string.mode_zen_desc
        GameMode.RAPIDO -> R.string.mode_fast_desc
        GameMode.TABLAS -> R.string.mode_tables_desc
        GameMode.CARRERA -> R.string.mode_race_desc
        GameMode.CUSTOM -> R.string.mode_custom_desc
    }

val GameMode.color: Color
    get() = when (this) {
        GameMode.CLASICO -> Color(0xFF81B29A)
        GameMode.DESAFIO -> Color(0xFFE07A5F)
        GameMode.ZEN -> Color(0xFF6C63FF)
        GameMode.RAPIDO -> Color(0xFFF4A261)
        GameMode.TABLAS -> Color(0xFF3D405B)
        GameMode.CARRERA -> Color(0xFFE0A93B)
        GameMode.CUSTOM -> Color(0xFF2A9D8F)
    }
