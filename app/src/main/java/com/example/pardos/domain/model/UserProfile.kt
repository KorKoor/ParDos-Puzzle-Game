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
    val pinnedRecords: List<String> = listOf("", "", ""), // 🔥 NUEVO: Espacio para 3 récords
    // Ranking semanal entre amigos: estrellas ganadas en la semana `weekId`
    val weeklyStars: Int = 0,
    val weekId: Int = 0,
    // Código corto para que te agreguen (solo cuentas con sesión)
    val friendCode: String = "",
    // Banner de perfil (fondo de tu tarjeta); 1 = el gratuito
    val bannerId: Int = 1,
    // Prestigio (lo que ven tus amigos): puntos, título que luces, Platino, piso de la torre y piezas del álbum
    val prestige: Int = 0,
    val titleId: String = "default",
    val platinum: Boolean = false,
    val towerBest: Int = 0,
    val pieces: Int = 0,
    // Álbum: piezas exhibidas en la vitrina, las que tienes y las que te sobran (en bits, ver AlbumBits) para intercambiar
    val showcase: List<String> = emptyList(),
    val albumBits: String = "",
    val spareBits: String = ""
) {
    // Constructor vacío requerido por Firestore para leer los datos
    constructor() : this("", "Jugador Zen", 1, 1, 1, 0, 100, 0, 0, 0L, emptyList(), emptyList())
}