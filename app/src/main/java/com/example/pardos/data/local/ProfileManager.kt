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

    // ---- Identidad en la nube ----
    // Con sesión de Google: perfil seguro en `players/{uid}`. Sin sesión: el perfil legacy `users/{ANDROID_ID}`.
    private val signedUid: String?
        get() = try {
            if (FirebaseApp.getApps(context).isEmpty()) null else com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        } catch (e: Exception) { null }

    /** ¿La cuenta actual usa el almacenamiento seguro v2? */
    val isCloudAccount: Boolean get() = signedUid != null
    private val cloudId: String get() = signedUid ?: deviceId
    private val profilesCollection: String get() = if (signedUid != null) "players" else "users"

    /** Código de amigo propio; se genera una vez por instalación y viaja con el perfil. */
    private fun ensureFriendCode(): String {
        val existing = prefs.getString("friend_code", null)
        if (!existing.isNullOrBlank()) return existing
        val code = com.korkoor.pardos.domain.social.FriendCode.generate()
        prefs.edit().putString("friend_code", code).apply()
        return code
    }

    private fun currentWeekId(): Int =
        com.korkoor.pardos.domain.social.WeekCalendar.weekId(LocalDay.today())

    /** Estrellas de ESTA semana; si la semana guardada es otra, empieza de cero. */
    private fun currentWeeklyStars(): Int =
        if (prefs.getInt("weekly_week", 0) == currentWeekId()) prefs.getInt("weekly_stars", 0) else 0

    fun getProfile(): UserProfile {
        // 1. Leemos el String largo de los récords (o un valor por defecto con separadores si está vacío)
        val pinnedCsv = prefs.getString("pinned_records_csv", "|||") ?: "|||"

        // 2. Lo convertimos de nuevo en una lista de 3 elementos
        // Limit = 3 asegura que siempre obtengamos los 3 slots aunque estén vacíos
        val pinnedList = pinnedCsv.split("|||", limit = 3).map { it.trim() }

        return UserProfile(
            uid = cloudId,
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
            pinnedRecords = pinnedList,
            weeklyStars = currentWeeklyStars(),
            weekId = currentWeekId(),
            friendCode = if (signedUid != null) ensureFriendCode() else ""
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
            putInt("weekly_stars", profile.weeklyStars)
            putInt("weekly_week", profile.weekId)

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
        val today = LocalDay.today()
        val xpGained = com.korkoor.pardos.domain.events.EventCalendar.apply(
            15 + (starsEarned * 10),
            com.korkoor.pardos.domain.events.EventCalendar.xpMultiplier(today)
        )

        var newXp = profile.currentXp + xpGained
        var newLevel = profile.playerLevel
        var nextLevelLimit = profile.xpToNextLevel

        while (newXp >= nextLevelLimit) {
            newXp -= nextLevelLimit
            newLevel++
            nextLevelLimit += 50
        }

        val week = currentWeekId()
        val starsSoFar = if (profile.weekId == week) profile.weeklyStars else 0

        val updatedProfile = profile.copy(
            playerLevel = newLevel,
            currentXp = newXp,
            xpToNextLevel = nextLevelLimit,
            weeklyStars = starsSoFar + starsEarned.coerceAtLeast(0) * com.korkoor.pardos.domain.events.EventCalendar.starMultiplier(today),
            weekId = week
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

    /** Actualiza la racha al abrir el juego, usando el día LOCAL del jugador. */
    fun checkAndUpdateStreak(): com.korkoor.pardos.domain.rewards.StreakChange {
        val economy = EconomyManager(context)
        val before = com.korkoor.pardos.domain.rewards.StreakState(
            streak = prefs.getInt("current_streak", 0),
            best = prefs.getInt("best_streak", 0),
            lastDay = prefs.getInt("last_play_day", 0)
        )
        val result = com.korkoor.pardos.domain.rewards.StreakCalculator.onOpen(
            state = before,
            today = LocalDay.today(),
            freezes = economy.streakFreezes.value
        )
        if (result.change != com.korkoor.pardos.domain.rewards.StreakChange.NONE) {
            prefs.edit().apply {
                putInt("current_streak", result.state.streak)
                putInt("best_streak", result.state.best)
                putInt("last_play_day", result.state.lastDay)
                apply()
            }
            economy.setStreakFreezes(result.freezesLeft)
            syncToFirebase()
        }
        return result.change
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

        val collection = profilesCollection
        firestore.collection(collection).document(profile.uid)
            .set(profile)
            .addOnSuccessListener {
                Log.d(TAG, "Sincronizado en Firebase correctamente ($collection).")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Fallo sincronizando en Firebase ($collection): ${e.message}")
                // Mientras las reglas v2 no estén publicadas, `players` rechaza la escritura:
                // seguimos guardando en el perfil legacy para no perder la copia en la nube.
                if (collection == "players") {
                    firestore.collection("users").document(deviceId).set(profile.copy(uid = deviceId))
                }
            }
    }

    /**
     * Agrega un amigo por código. Acepta el ID completo o solo el prefijo de 8 caracteres
     * que se muestra en pantalla (antes solo funcionaba el ID completo, y era confuso).
     */
    fun addFriendByCode(friendUid: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        // Cuenta con sesión: se busca por el código corto de amigo en `players`
        if (signedUid != null) {
            addFriendByFriendCode(friendUid, onSuccess, onError)
            return
        }
        val code = friendUid.trim().lowercase()
        if (code.length < 6) {
            onError("El código es demasiado corto.")
            return
        }

        val firestore = db
        if (firestore == null) {
            onError("Funciones sociales no disponibles: Firebase no configurado.")
            Log.w(TAG, "addFriendByCode cancelado: Firebase no disponible.")
            return
        }

        fun finish(friendId: String, friendName: String) {
            if (friendId == deviceId) {
                onError("¡No puedes agregarte a ti mismo!")
                return
            }
            val friendsSet = prefs.getStringSet("friends_list", emptySet())?.toMutableSet() ?: mutableSetOf()
            if (friendsSet.contains(friendId)) {
                onError("Ya lo tienes en tu lista.")
                return
            }
            friendsSet.add(friendId)
            prefs.edit().putStringSet("friends_list", friendsSet).apply()
            saveProfile(getProfile().copy(friendsUids = friendsSet.toList()))
            unlockSocialBadge()
            if (friendsSet.size >= 3 && !prefs.getBoolean("badge_influencer_unlocked", false)) {
                prefs.edit().putBoolean("badge_influencer_unlocked", true).apply()
                saveProfile(getProfile())
            }
            onSuccess(friendName)
        }

        if (code.length >= 16) {
            firestore.collection("users").document(code).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) finish(doc.id, doc.getString("name") ?: "Jugador")
                    else onError("Código no encontrado.")
                }
                .addOnFailureListener { e -> onError("Error de red: ${e.localizedMessage}") }
        } else {
            // Búsqueda por prefijo del ID de documento
            val id = com.google.firebase.firestore.FieldPath.documentId()
            firestore.collection("users")
                .whereGreaterThanOrEqualTo(id, code)
                .whereLessThanOrEqualTo(id, code + "\uf8ff")
                .limit(1)
                .get()
                .addOnSuccessListener { snap ->
                    val doc = snap.documents.firstOrNull()
                    if (doc != null) finish(doc.id, doc.getString("name") ?: "Jugador")
                    else onError("Código no encontrado.")
                }
                .addOnFailureListener { e -> onError("Error de red: ${e.localizedMessage}") }
        }
    }

    private fun addFriendByFriendCode(input: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val code = com.korkoor.pardos.domain.social.FriendCode.normalize(input)
        if (!com.korkoor.pardos.domain.social.FriendCode.isValid(code)) {
            onError("El código debe tener ${com.korkoor.pardos.domain.social.FriendCode.LENGTH} caracteres.")
            return
        }
        val firestore = db ?: run { onError("Funciones sociales no disponibles."); return }
        firestore.collection("players").whereEqualTo("friendCode", code).limit(1).get()
            .addOnSuccessListener { snap ->
                val doc = snap.documents.firstOrNull()
                if (doc == null) { onError("Código no encontrado."); return@addOnSuccessListener }
                if (doc.id == cloudId) { onError("¡No puedes agregarte a ti mismo!"); return@addOnSuccessListener }
                val friends = prefs.getStringSet("friends_list", emptySet())?.toMutableSet() ?: mutableSetOf()
                if (!friends.add(doc.id)) { onError("Ya lo tienes en tu lista."); return@addOnSuccessListener }
                prefs.edit().putStringSet("friends_list", friends).apply()
                saveProfile(getProfile().copy(friendsUids = friends.toList()))
                unlockSocialBadge()
                if (friends.size >= 3 && !prefs.getBoolean("badge_influencer_unlocked", false)) {
                    prefs.edit().putBoolean("badge_influencer_unlocked", true).apply()
                    saveProfile(getProfile())
                }
                onSuccess(doc.getString("name") ?: "Jugador")
            }
            .addOnFailureListener { e -> onError("Error de red: ${e.localizedMessage}") }
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

        fun parse(doc: com.google.firebase.firestore.DocumentSnapshot): UserProfile? = try {
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
                pinnedRecords = (doc.get("pinnedRecords") as? List<String>) ?: listOf("", "", ""),
                weeklyStars = doc.getLong("weeklyStars")?.toInt() ?: 0,
                weekId = doc.getLong("weekId")?.toInt() ?: 0,
                friendCode = doc.getString("friendCode") ?: ""
            )
        } catch (e: Exception) { null }

        // Lee una colección por trozos de 10 (límite de whereIn). Si falla, devuelve lo que haya.
        fun readCollection(collection: String, ids: List<String>, done: (List<UserProfile>) -> Unit) {
            if (ids.isEmpty()) { done(emptyList()); return }
            val chunks = ids.chunked(10)
            val found = mutableListOf<UserProfile>()
            var finished = 0
            for (chunk in chunks) {
                firestore.collection(collection)
                    .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk).get()
                    .addOnSuccessListener { snap -> found += snap.documents.mapNotNull { parse(it) } }
                    .addOnCompleteListener { if (++finished == chunks.size) done(found) }
            }
        }

        if (signedUid != null) {
            // Primero `players` (v2); los ids que no estén allí se buscan en el perfil legacy
            readCollection("players", currentFriends) { v2 ->
                val missing = currentFriends - v2.map { it.uid }.toSet()
                readCollection("users", missing) { legacy -> onComplete(v2 + legacy) }
            }
        } else {
            readCollection("users", currentFriends, onComplete)
        }
    }

    /**
     * Tras iniciar sesión: si la cuenta ya tiene un perfil en la nube (otro dispositivo, reinstalación)
     * se restaura el progreso más avanzado; si es nueva, se sube el perfil local.
     */
    fun syncAfterSignIn(onDone: (restored: Boolean) -> Unit) {
        val firestore = db
        val uid = signedUid
        if (firestore == null || uid == null) { onDone(false); return }

        firestore.collection("players").document(uid).get()
            .addOnSuccessListener { doc ->
                val local = getProfile()
                if (doc.exists()) {
                    val cloudLevel = doc.getLong("playerLevel")?.toInt() ?: 1
                    val cloudCampaign = doc.getLong("currentCampaignLevel")?.toInt() ?: 1
                    val cloudIsAhead = cloudLevel > local.playerLevel ||
                        (cloudLevel == local.playerLevel && cloudCampaign > local.currentCampaignLevel)
                    if (cloudIsAhead) {
                        val restored = local.copy(
                            name = doc.getString("name") ?: local.name,
                            avatarId = doc.getLong("avatarId")?.toInt() ?: local.avatarId,
                            playerLevel = cloudLevel,
                            currentCampaignLevel = cloudCampaign,
                            currentXp = doc.getLong("currentXp")?.toInt() ?: local.currentXp,
                            xpToNextLevel = doc.getLong("xpToNextLevel")?.toInt() ?: local.xpToNextLevel,
                            friendsUids = ((doc.get("friendsUids") as? List<String>) ?: emptyList()).union(local.friendsUids).toList(),
                            unlockedBadges = (doc.get("unlockedBadges") as? List<String>) ?: local.unlockedBadges
                        )
                        saveProfile(restored)
                        onDone(true)
                        return@addOnSuccessListener
                    }
                }
                // Cuenta nueva (o el local va más avanzado): se sube el perfil local
                saveProfile(local)
                onDone(false)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "syncAfterSignIn: ${e.message}")
                onDone(false)
            }
    }

    fun syncFromFirebase(onResult: (UserProfile?) -> Unit) {
        val firestore = db
        if (firestore == null) {
            Log.w(TAG, "syncFromFirebase omitido: Firebase no disponible.")
            onResult(null)
            return
        }

        firestore.collection(profilesCollection).document(cloudId)
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

