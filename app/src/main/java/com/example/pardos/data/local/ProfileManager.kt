package com.korkoor.pardos.data.local

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import android.util.Log
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.korkoor.pardos.domain.model.UserProfile

class ProfileManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("pardos_profile", Context.MODE_PRIVATE)
    private val db = FirebaseFirestore.getInstance()

    // 🔥 EL SECRETO DE LA PERSISTENCIA
    // Genera un ID único para el dispositivo que sobrevive a las reinstalaciones
    private val deviceId: String by lazy {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "dispositivo_desconocido"
    }

    fun getProfile(): UserProfile {
        // 1. Leemos el String largo de los récords (o un valor por defecto con separadores si está vacío)
        val pinnedCsv = prefs.getString("pinned_records_csv", "|||") ?: "|||"

        // 2. Lo convertimos de nuevo en una lista de 3 elementos
        // Limit = 3 asegura que siempre obtengamos los 3 slots aunque estén vacíos
        val pinnedList = pinnedCsv.split("|||", limit = 3).map { it.trim() }

        return UserProfile(
            uid = deviceId,
            name = prefs.getString("user_name", "Jugador Zen") ?: "Jugador Zen",
            avatarId = prefs.getInt("avatar_id", 1),
            playerLevel = prefs.getInt("player_level", 1),
            currentCampaignLevel = prefs.getInt("current_campaign_level", 1),
            currentXp = prefs.getInt("current_xp", 0),
            xpToNextLevel = prefs.getInt("xp_to_next", 100),
            currentStreak = prefs.getInt("current_streak", 0),
            bestStreak = prefs.getInt("best_streak", 0),
            lastPlayDate = prefs.getLong("last_play_date", 0L),
            friendsUids = prefs.getStringSet("friends_list", emptySet())?.toList() ?: emptyList(),
            unlockedBadges = prefs.getStringSet("unlocked_badges", emptySet())?.toList() ?: emptyList(),

            // 🔥 LA NUEVA PIEZA:
            pinnedRecords = pinnedList
        )
    }

    fun saveProfile(profile: UserProfile) {
        prefs.edit().apply {
            putString("user_name", profile.name)
            putInt("avatar_id", profile.avatarId)
            putInt("player_level", profile.playerLevel)
            putInt("current_campaign_level", profile.currentCampaignLevel)
            putInt("current_xp", profile.currentXp)
            putInt("xp_to_next", profile.xpToNextLevel)
            putInt("current_streak", profile.currentStreak)
            putInt("best_streak", profile.bestStreak)
            putLong("last_play_date", profile.lastPlayDate)

            // Guardamos los amigos e insignias (Sets)
            putStringSet("friends_list", profile.friendsUids.toSet())
            putStringSet("unlocked_badges", profile.unlockedBadges.toSet())

            // 🔥 LA NUEVA PIEZA: Vitrina de Récords
            // Convertimos la lista ["Record1", "Record2", ""] -> "Record1|||Record2|||"
            putString("pinned_records_csv", profile.pinnedRecords.joinToString("|||"))

            apply()
        }
        // Sincronización inmediata con Firestore para que otros vean tus récords fijados
        syncToFirebase()
    }

    fun unlockSocialBadge() {
        if (!prefs.getBoolean("badge_social_unlocked", false)) {
            prefs.edit().putBoolean("badge_social_unlocked", true).apply()
            syncToFirebase() // Sincronizamos el perfil completo con la nueva insignia
        }
    }

    fun addXpForLevelVictory(starsEarned: Int) {
        val profile = getProfile()
        val xpGained = 15 + (starsEarned * 10)

        var newXp = profile.currentXp + xpGained
        var newLevel = profile.playerLevel
        var nextLevelLimit = profile.xpToNextLevel

        while (newXp >= nextLevelLimit) {
            newXp -= nextLevelLimit
            newLevel++
            nextLevelLimit += 50
        }

        val updatedProfile = profile.copy(
            playerLevel = newLevel,
            currentXp = newXp,
            xpToNextLevel = nextLevelLimit
        )
        saveProfile(updatedProfile)
    }

    fun pinRecordToSlot(slotIndex: Int, recordText: String) {
        // 1. Obtenemos el perfil actual completo
        val currentProfile = getProfile()

        // 2. Creamos una copia editable de los récords fijados
        val newPinned = currentProfile.pinnedRecords.toMutableList()

        // 3. Verificamos que el slot sea válido (0, 1 o 2)
        if (slotIndex in 0..2) {
            // Actualizamos el texto en la posición elegida
            newPinned[slotIndex] = recordText

            // 4. Creamos el nuevo objeto de perfil con la lista actualizada
            val updatedProfile = currentProfile.copy(pinnedRecords = newPinned)

            // 5. ¡La magia ocurre aquí!
            // saveProfile se encarga de convertir la lista a String "|||"
            // y de subirla a Firebase automáticamente.
            saveProfile(updatedProfile)
        }
    }

  fun checkAndUpdateStreak() {
        val currentDay = (System.currentTimeMillis() / (1000 * 60 * 60 * 24)).toInt()
        val lastPlayDay = prefs.getInt("last_play_day", 0)

        var currentStreak = prefs.getInt("current_streak", 0)
        var bestStreak = prefs.getInt("best_streak", 0)
        var changed = false

        if (lastPlayDay == 0) {
            currentStreak = 1
            bestStreak = 1
            changed = true
        } else if (currentDay - lastPlayDay == 1) {
            currentStreak += 1
            if (currentStreak > bestStreak) bestStreak = currentStreak
            changed = true
        } else if (currentDay - lastPlayDay > 1) {
            currentStreak = 1
            changed = true
        }

        if (changed) {
            prefs.edit().apply {
                putInt("current_streak", currentStreak)
                putInt("best_streak", bestStreak)
                putInt("last_play_day", currentDay)
                apply()
            }
            syncToFirebase()
        }
    }

    private fun syncToFirebase() {
        val profile = getProfile()

        // 🛡️ ESCUDO: Si el perfil es nivel 1, tiene 0 XP y se llama "Jugador Zen",
        // es muy probable que sea un perfil recién creado tras reinstalar.
        // NO lo subimos para no borrar lo que ya existe en la nube.
        if (profile.playerLevel == 1 && profile.currentXp == 0 && profile.name == "Jugador Zen") {
            Log.d("ProfileManager", "⚠️ Perfil inicial detectado. No se subirá a la nube para evitar sobreescritura.")
            return
        }

        db.collection("users").document(profile.uid)
            .set(profile)
            .addOnSuccessListener {
                Log.d("ProfileManager", "✅ Sincronizado correctamente.")
            }
    }

    fun addFriendByCode(friendUid: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        // Validación inmediata
        if (friendUid.isBlank()) {
            onError("Código vacío.")
            return
        }

        if (friendUid == deviceId) {
            onError("¡No puedes agregarte a ti mismo!")
            return
        }

        db.collection("users").document(friendUid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val friendName = document.getString("name") ?: "Jugador"
                    val friendsSet = prefs.getStringSet("friends_list", emptySet())?.toMutableSet() ?: mutableSetOf()

                    if (friendsSet.contains(friendUid)) {
                        onError("Ya lo tienes en tu lista.")
                    } else {
                        friendsSet.add(friendUid)
                        prefs.edit().putStringSet("friends_list", friendsSet).apply()

                        // Reflejamos los cambios en el modelo de datos y sincronizamos
                        val currentProfile = getProfile()
                        val updatedProfile = currentProfile.copy(friendsUids = friendsSet.toList())
                        saveProfile(updatedProfile)

                        // 🥇 Insignia Social (1 amigo)
                        unlockSocialBadge()

                        // 🔥 NUEVO: Insignia Influencer Zen (3 amigos) 🔥
                        if (friendsSet.size >= 3) {
                            if (!prefs.getBoolean("badge_influencer_unlocked", false)) {
                                prefs.edit().putBoolean("badge_influencer_unlocked", true).apply()

                                // Volvemos a guardar el perfil para que Firebase detecte
                                // que la insignia se desbloqueó (por si acaso la usas en la nube)
                                saveProfile(getProfile())
                            }
                        }

                        onSuccess(friendName)
                    }
                } else {
                    onError("Código no encontrado.")
                }
            }
            .addOnFailureListener { e ->
                onError("Error de red: ${e.localizedMessage}")
            }
    }

    fun updateCampaignLevel(newLevel: Int) {
        val currentProfile = getProfile()
        // Solo actualizamos si el nuevo nivel es mayor al que ya teníamos (para no retroceder)
        if (newLevel > currentProfile.currentCampaignLevel) {
            val updatedProfile = currentProfile.copy(currentCampaignLevel = newLevel)
            saveProfile(updatedProfile) // Esto ya llama a syncToFirebase() internamente
        }
    }
    // DENTRO DE ProfileManager.kt

    // Dentro de tu archivo ProfileManager.kt

    fun getFriendsProfiles(onComplete: (List<UserProfile>) -> Unit) {
        val currentFriends = getProfile().friendsUids
        if (currentFriends.isEmpty()) {
            onComplete(emptyList())
            return
        }

        // 🔥 CUIDADO: Firestore no permite buscar más de 10 amigos de golpe con 'whereIn'.
        // Si un jugador tiene 15 amigos, la app crashearía. Por eso lo dividimos en bloques (chunks) de 10.
        val chunks = currentFriends.chunked(10)
        val allFriends = mutableListOf<UserProfile>()
        var completedChunks = 0

        for (chunk in chunks) {
            // Buscamos directamente por el ID del documento, es más rápido y seguro
            db.collection("users").whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk).get()
                .addOnSuccessListener { snapshot ->
                    val friendsInChunk = snapshot.documents.mapNotNull { doc ->
                        try {
                            UserProfile(
                                uid = doc.id, // Tomamos el ID directo del documento
                                name = doc.getString("name") ?: "Jugador Zen",
                                avatarId = doc.getLong("avatarId")?.toInt() ?: 1,
                                playerLevel = doc.getLong("playerLevel")?.toInt() ?: 1,
                                currentCampaignLevel = doc.getLong("currentCampaignLevel")?.toInt() ?: 1,
                                currentXp = doc.getLong("currentXp")?.toInt() ?: 0,
                                xpToNextLevel = doc.getLong("xpToNextLevel")?.toInt() ?: 100,
                                currentStreak = doc.getLong("currentStreak")?.toInt() ?: 0,
                                bestStreak = doc.getLong("bestStreak")?.toInt() ?: 0,
                                lastPlayDate = doc.getLong("lastPlayDate") ?: 0L,
                                friendsUids = (doc.get("friendsUids") as? List<String>) ?: emptyList(),
                                unlockedBadges = (doc.get("unlockedBadges") as? List<String>) ?: emptyList(),
                                pinnedRecords = (doc.get("pinnedRecords") as? List<String>) ?: listOf("", "", "")
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }

                    allFriends.addAll(friendsInChunk)
                    completedChunks++

                    // Cuando terminemos de buscar todos los bloques, devolvemos la lista
                    if (completedChunks == chunks.size) {
                        onComplete(allFriends)
                    }
                }
                .addOnFailureListener {
                    completedChunks++
                    if (completedChunks == chunks.size) {
                        onComplete(allFriends)
                    }
                }
        }
    }

    fun syncFromFirebase(onResult: (UserProfile?) -> Unit) {
        // 🔥 IMPORTANTE: Source.SERVER obliga a Firebase a ignorar la caché local
        // y descargar los datos reales de la nube.
        db.collection("users").document(deviceId)
            .get(com.google.firebase.firestore.Source.SERVER)
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    Log.d("ProfileManager", "☁️ Datos encontrados en la nube para el ID: $deviceId")

                    // Extraemos manualmente para evitar errores de parseo
                    val cloudProfile = UserProfile(
                        uid = document.getString("uid") ?: deviceId,
                        name = document.getString("name") ?: "Jugador Zen",
                        avatarId = document.getLong("avatarId")?.toInt() ?: 1,
                        playerLevel = document.getLong("playerLevel")?.toInt() ?: 1,
                        currentCampaignLevel = document.getLong("currentCampaignLevel")?.toInt() ?: 1,
                        currentXp = document.getLong("currentXp")?.toInt() ?: 0,
                        xpToNextLevel = document.getLong("xpToNextLevel")?.toInt() ?: 100,
                        currentStreak = document.getLong("currentStreak")?.toInt() ?: 0,
                        bestStreak = document.getLong("bestStreak")?.toInt() ?: 0,
                        lastPlayDate = document.getLong("lastPlayDate") ?: 0L,
                        friendsUids = (document.get("friendsUids") as? List<String>) ?: emptyList(),
                        unlockedBadges = (document.get("unlockedBadges") as? List<String>) ?: emptyList(),
                        pinnedRecords = (document.get("pinnedRecords") as? List<String>) ?: listOf("", "", "")
                    )

                    onResult(cloudProfile)
                } else {
                    Log.d("ProfileManager", "☁️ El documento no existe en la nube.")
                    onResult(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e("ProfileManager", "❌ Error descargando de Firestore: ${e.message}")
                onResult(null)
            }
    }
    fun migrateLegacyProgressIfNeeded(legacyCampaignLevel: Int) {
        val prefs = context.getSharedPreferences("pardos_profile", android.content.Context.MODE_PRIVATE)
        val hasMigrated = prefs.getBoolean("migrated_v1_to_v2", false)

        // Solo migramos si NO lo hemos hecho antes y si es nivel 2 o superior
        if (!hasMigrated && legacyCampaignLevel > 1) {
            val currentProfile = getProfile()

            // --- MATEMÁTICAS DE CONVERSIÓN ---
            // Asumimos que ganó todos los niveles anteriores con 3 estrellas.
            // Si tienes una constante de XP por estrella (ej. 10 XP), ajusta esto:
            val xpPerStar = 10
            val estimatedTotalXp = (legacyCampaignLevel - 1) * 3 * xpPerStar

            // Calculamos qué nivel de jugador le corresponde por esa XP
            // (Esto depende de tu fórmula de XP, aquí un ejemplo básico donde cada nivel pide 100 XP)
            var newPlayerLevel = 1
            var remainingXp = estimatedTotalXp
            var xpForNext = 100

            while (remainingXp >= xpForNext) {
                remainingXp -= xpForNext
                newPlayerLevel++
                xpForNext = newPlayerLevel * 100 // Escala de dificultad de nivel
            }

            // Actualizamos el perfil con su gloria pasada
            val migratedProfile = currentProfile.copy(
                name = "Veterano Zen", // Un apodo especial para los jugadores antiguos
                playerLevel = newPlayerLevel,
                currentXp = remainingXp,
                xpToNextLevel = xpForNext,
                currentCampaignLevel = legacyCampaignLevel
            )

            saveProfile(migratedProfile) // Esto lo guarda local y lo sube a Firebase

            // Marcamos que ya se migró para no volver a regalarle XP
            prefs.edit().putBoolean("migrated_v1_to_v2", true).apply()
            // También marcamos que el setup ya está "completo" para que no le pida crear perfil desde cero
            prefs.edit().putBoolean("is_profile_setup_complete", true).apply()

            android.util.Log.d("ProfileManager", "🏆 Jugador veterano migrado al nivel $newPlayerLevel con $estimatedTotalXp XP total.")
        }
    }
}