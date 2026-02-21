package com.korkoor.pardos.domain.model

import androidx.annotation.Keep

@Keep
data class UserProfile(
    val uid: String = "", // El ID único de Firebase Auth
    val name: String = "Jugador Zen",
    val avatarId: Int = 1, // Un número del 1 al 10
    val playerLevel: Int = 1, // Nivel del perfil (basado en XP)
    val currentCampaignLevel: Int = 1, // En qué nivel va de los 2,400
    val currentXp: Int = 0,
    val xpToNextLevel: Int = 100,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val lastPlayDate: Long = 0L,
    val friendsUids: List<String> = emptyList(), // Lista de IDs de sus amigos
    val unlockedBadges: List<String> = emptyList(),
    val pinnedRecords: List<String> = listOf("", "", "") // 🔥 NUEVO: Espacio para 3 récords
) {
    // Constructor vacío requerido por Firestore para leer los datos
    constructor() : this("", "Jugador Zen", 1, 1, 1, 0, 100, 0, 0, 0L, emptyList(), emptyList())
}