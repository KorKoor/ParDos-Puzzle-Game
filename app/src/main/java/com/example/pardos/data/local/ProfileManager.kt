package com.korkoor.pardos.data.local

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.korkoor.pardos.domain.model.UserProfile

class ProfileManager(private val context: Context) {
    companion object {
        private const val TAG = "ProfileManager"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences("pardos_profile", Context.MODE_PRIVATE)
    private val db: FirebaseFirestore? by lazy {
        try {
            val app = FirebaseApp.initializeApp(context) ?: FirebaseApp.getApps(context).firstOrNull()
            if (app == null) {
                Log.w(TAG, "Firebase no configurado. Se desactiva sincronizacion en la nube.")
                null
            } else {
                FirebaseFirestore.getInstance(app).also {
                    Log.d(TAG, "FirebaseFirestore inicializado correctamente.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo inicializar FirebaseFirestore: ${e.message}")
            null
        }
    }

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
        val firestore = db
        if (firestore == null) {
            Log.w(TAG, "Sync omitido: Firebase no esta disponible en este build.")
            return
        }

        val profile = getProfile()
        if (profile.playerLevel == 1 && profile.currentXp == 0 && profile.name == "Jugador Zen") {
            Log.d(TAG, "Perfil inicial detectado. No se sube para evitar sobreescritura.")
            return
        }

        firestore.collection("users").document(profile.uid)
            .set(profile)
            .addOnSuccessListener {
                Log.d(TAG, "Sincronizado en Firebase correctamente.")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Fallo sincronizando en Firebase: ${e.message}")
            }
    }

    fun addFriendByCode(friendUid: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        if (friendUid.isBlank()) {
            onError("Código vacío.")
            return
        }

        if (friendUid == deviceId) {
            onError("¡No puedes agregarte a ti mismo!")
            return
        }

        val firestore = db
        if (firestore == null) {
            onError("Funciones sociales no disponibles: Firebase no configurado.")
            Log.w(TAG, "addFriendByCode cancelado: Firebase no disponible.")
            return
        }

        firestore.collection("users").document(friendUid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val friendName = document.getString("name") ?: "Jugador"
                    val friendsSet = prefs.getStringSet("friends_list", emptySet())?.toMutableSet() ?: mutableSetOf()

                    if (friendsSet.contains(friendUid)) {
                        onError("Ya lo tienes en tu lista.")
                    } else {
                        friendsSet.add(friendUid)
                        prefs.edit().putStringSet("friends_list", friendsSet).apply()

                        val currentProfile = getProfile()
                        val updatedProfile = currentProfile.copy(friendsUids = friendsSet.toList())
                        saveProfile(updatedProfile)

                        unlockSocialBadge()

                        if (friendsSet.size >= 3) {
                            if (!prefs.getBoolean("badge_influencer_unlocked", false)) {
                                prefs.edit().putBoolean("badge_influencer_unlocked", true).apply()
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
        val firestore = db
        if (firestore == null) {
            Log.w(TAG, "getFriendsProfiles: Firebase no disponible. Regresando lista vacia.")
            onComplete(emptyList())
            return
        }

        val currentFriends = getProfile().friendsUids
        if (currentFriends.isEmpty()) {
            onComplete(emptyList())
            return
        }

        val chunks = currentFriends.chunked(10)
        val allFriends = mutableListOf<UserProfile>()
        var completedChunks = 0

        for (chunk in chunks) {
            firestore.collection("users").whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk).get()
                .addOnSuccessListener { snapshot ->
                    val friendsInChunk = snapshot.documents.mapNotNull { doc ->
                        try {
                            UserProfile(
                                uid = doc.id,
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
        val firestore = db
        if (firestore == null) {
            Log.w(TAG, "syncFromFirebase omitido: Firebase no disponible.")
            onResult(null)
            return
        }

        firestore.collection("users").document(deviceId)
            .get(com.google.firebase.firestore.Source.SERVER)
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    Log.d(TAG, "Datos encontrados en la nube para el ID: $deviceId")

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
                    Log.d(TAG, "El documento no existe en la nube.")
                    onResult(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error descargando de Firestore: ${e.message}")
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

