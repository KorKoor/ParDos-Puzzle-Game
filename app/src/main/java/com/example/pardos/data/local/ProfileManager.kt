package com.korkoor.pardos.data.local

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.korkoor.pardos.domain.model.UserProfile
import com.korkoor.pardos.domain.social.SyncPolicy

class ProfileManager(private val context: Context) {
    companion object {
        private const val TAG = "ProfileManager"

        // Estado compartido (ProfileManager se crea muchas veces; la cola de subida y la caché son únicas por proceso)
        private val handler = android.os.Handler(android.os.Looper.getMainLooper())
        private var pendingUpload: Runnable? = null
        private var friendCache: MutableMap<String, FriendEntry>? = null
        private var uploading = false
        private var lastForcedMs = 0L
        private const val FORCE_MIN_GAP_MS = 60_000L
    }

    /** Copia guardada de un amigo: cuándo se leyó, de qué colección (p = players, u = users, x = no existe) y su perfil. */
    private class FriendEntry(val at: Long, val src: String, val profile: UserProfile?)

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
            friendCode = if (signedUid != null) ensureFriendCode() else "",
            bannerId = prefs.getInt("banner_id", 1),
            prestige = PrestigeManager.cachedScore(context),
            titleId = PrestigeManager.cachedTitleId(context),
            platinum = PrestigeManager.cachedPlatinum(context),
            towerBest = PrestigeManager.cachedTower(context),
            pieces = PrestigeManager.cachedPieces(context)
        )
    }

    /** Pide subir el perfil (con la espera de siempre): lo usan el prestigio y los títulos cuando cambian. */
    fun requestSync() = syncToFirebase()

    fun saveProfile(profile: UserProfile) {
        prefs.edit().apply {
            putString("user_name", profile.name)
            putInt("avatar_id", profile.avatarId)
            putInt("banner_id", profile.bannerId)
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
        // Racha perdida por poco: se guarda una oferta para recuperarla hoy (ver StreakRepair)
        if (result.change == com.korkoor.pardos.domain.rewards.StreakChange.RESET &&
            com.korkoor.pardos.domain.rewards.StreakRepair.canOffer(before, LocalDay.today())
        ) {
            prefs.edit()
                .putInt("repair_day", LocalDay.today())
                .putInt("repair_streak", before.streak)
                .putInt("repair_best", before.best)
                .apply()
        }
        if (result.change != com.korkoor.pardos.domain.rewards.StreakChange.NONE) {
            prefs.edit().apply {
                putInt("current_streak", result.state.streak)
                putInt("best_streak", result.state.best)
                putInt("last_play_day", result.state.lastDay)
                apply()
            }
            economy.setStreakFreezes(result.freezesLeft)
            syncToFirebase()
            // 🎁 Hito de racha (3, 7, 14, 30, 60, 100 días): monedas, gemas y cofre
            RewardsManager(context).claimStreakMilestone(result.state.streak)
        }
        return result.change
    }

    // ---------------- Recuperar racha ----------------

    /** Racha perdida que aún se puede recuperar hoy, o null si no hay oferta. */
    fun pendingStreakRepair(): Int? =
        if (prefs.getInt("repair_day", -1) == LocalDay.today()) prefs.getInt("repair_streak", 0).takeIf { it > 0 } else null

    fun streakRepairGemCost(): Int = com.korkoor.pardos.domain.rewards.StreakRepair.gemCost(pendingStreakRepair() ?: 0)

    fun streakRepairAdAvailable(): Boolean =
        com.korkoor.pardos.domain.rewards.StreakRepair.adAvailable(prefs.getInt("repair_ad_day", 0), LocalDay.today())

    /** Recupera la racha pagando gemas o tras ver un anuncio (el anuncio lo muestra la UI antes de llamar). */
    fun repairStreak(withGems: Boolean): Boolean {
        val lost = pendingStreakRepair() ?: return false
        val today = LocalDay.today()
        if (withGems) {
            if (!EconomyManager(context).spendGems(streakRepairGemCost())) return false
        } else {
            if (!streakRepairAdAvailable()) return false
            prefs.edit().putInt("repair_ad_day", today).apply()
        }
        val fixed = com.korkoor.pardos.domain.rewards.StreakRepair.repaired(
            com.korkoor.pardos.domain.rewards.StreakState(lost, prefs.getInt("repair_best", lost), today), today
        )
        prefs.edit()
            .putInt("current_streak", fixed.streak)
            .putInt("best_streak", fixed.best)
            .putInt("last_play_day", fixed.lastDay)
            .remove("repair_day")
            .apply()
        syncToFirebase()
        RewardsManager(context).claimStreakMilestone(fixed.streak)
        return true
    }

    /** El jugador deja ir la racha: la oferta desaparece. */
    fun declineStreakRepair() {
        prefs.edit().remove("repair_day").apply()
    }

    /**
     * Pide subir el perfil. NO sube al instante: junta los cambios (ganar un nivel toca XP, campaña, racha…) y sube una
     * sola vez tras una pausa, o al salir de la app (ver [flushPendingSync]). Así una sesión cuesta ~1 escritura, no decenas.
     */
    private fun syncToFirebase() {
        prefs.edit().putBoolean("sync_dirty", true).apply()
        pendingUpload?.let { handler.removeCallbacks(it) }
        val task = Runnable { uploadIfNeeded() }
        pendingUpload = task
        handler.postDelayed(task, SyncPolicy.UPLOAD_DEBOUNCE_MS)
    }

    /** Se llama al pasar la app a segundo plano: sube lo pendiente (si cambió algo) antes de que el sistema la cierre. */
    fun flushPendingSync() {
        pendingUpload?.let { handler.removeCallbacks(it) }
        pendingUpload = null
        uploadIfNeeded()
    }

    private fun profileHash(p: UserProfile): Int = SyncPolicy.contentHash(
        listOf(
            p.uid, p.name, p.avatarId, p.playerLevel, p.currentCampaignLevel, p.currentXp, p.xpToNextLevel,
            p.currentStreak, p.bestStreak, p.friendsUids, p.unlockedBadges, p.pinnedRecords, p.weeklyStars, p.weekId, p.friendCode, p.bannerId,
            p.prestige, p.titleId, p.platinum, p.towerBest, p.pieces
        )
    )

    private fun isOnline(): Boolean = try {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        caps != null && caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } catch (e: Exception) { true }

    private fun uploadIfNeeded() {
        val now = System.currentTimeMillis()
        val profile = getProfile()
        val changed = profileHash(profile) != prefs.getInt("sync_hash", 0)
        val last = prefs.getLong("sync_last_ms", 0L)
        val dirty = prefs.getBoolean("sync_dirty", false)
        if (!dirty || !changed) {
            if (dirty) prefs.edit().putBoolean("sync_dirty", false).apply()
            return
        }
        // Sin red no se encola nada: el SDK de Firestore guardaría cada escritura y las enviaría TODAS al volver la conexión.
        // El perfil sigue marcado como pendiente y se sube al abrir/cerrar la app con conexión.
        if (uploading || !isOnline()) return
        // Tras un fallo (reglas que rechazan, servidor caído) se espera cada vez más en vez de insistir cada minuto
        val failures = prefs.getInt("sync_failures", 0)
        val backoffLeft = SyncPolicy.uploadBackoffMs(failures) - (now - prefs.getLong("sync_fail_ms", 0L))
        if (backoffLeft > 0) return
        if (!SyncPolicy.shouldUpload(dirty, changed, last, now)) {
            // demasiado pronto: se reintenta cuando se cumpla la separación mínima
            val wait = SyncPolicy.waitBeforeUpload(last, now).coerceAtLeast(5_000L)
            pendingUpload?.let { handler.removeCallbacks(it) }
            val task = Runnable { uploadIfNeeded() }
            pendingUpload = task
            handler.postDelayed(task, wait)
            return
        }
        uploadNow(profile)
    }

    private fun uploadNow(profile: UserProfile) {
        val firestore = db
        if (firestore == null) {
            Log.w(TAG, "Sync omitido: Firebase no esta disponible en este build.")
            return
        }

        if (profile.playerLevel == 1 && profile.currentXp == 0 && profile.name == "Jugador Zen") {
            Log.d(TAG, "Perfil inicial detectado. No se sube para evitar sobreescritura.")
            return
        }

        val collection = profilesCollection
        val hash = profileHash(profile)
        uploading = true
        firestore.collection(collection).document(profile.uid)
            .set(profile)
            .addOnSuccessListener {
                uploading = false
                prefs.edit().putInt("sync_hash", hash).putLong("sync_last_ms", System.currentTimeMillis())
                    .putBoolean("sync_dirty", false).putInt("sync_failures", 0).apply()
                Log.d(TAG, "Sincronizado en Firebase correctamente ($collection).")
            }
            .addOnFailureListener { e ->
                uploading = false
                Log.e(TAG, "Fallo sincronizando en Firebase ($collection): ${e.message}")
                prefs.edit().putInt("sync_failures", prefs.getInt("sync_failures", 0) + 1)
                    .putLong("sync_fail_ms", System.currentTimeMillis()).apply()
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
        // Antes de gastar una lectura: tope de amigos y límite de búsquedas por hora
        if (SyncPolicy.friendLimitReached(getProfile().friendsUids.size)) {
            onError("Llegaste al máximo de ${SyncPolicy.MAX_FRIENDS} amigos.")
            return
        }
        val nowMs = System.currentTimeMillis()
        val attempts = (prefs.getString("lookup_times", "") ?: "").split(",").mapNotNull { it.toLongOrNull() }
        if (!SyncPolicy.canLookupFriend(attempts, nowMs)) {
            onError("Demasiados intentos. Espera un momento e inténtalo de nuevo.")
            return
        }
        prefs.edit().putString("lookup_times", (attempts.filter { nowMs - it in 0 until 3_600_000L } + nowMs).joinToString(",")).apply()
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

    /**
     * Perfiles de los amigos. Cada amigo leído cuesta una lectura, así que cada uno se guarda EN EL DISCO con su propia
     * vida útil (quien juega hoy se renueva a los 15 min; quien lleva semanas sin jugar, una vez al día) y recuerda en qué
     * colección vive (no se vuelve a preguntar en `users` por quien ya está en `players`). Solo se leen los vencidos;
     * abrir Perfil, Amigos y el Mapa seguidos, o reiniciar la app, no gasta nada. [force] (botón de actualizar) salta la caché,
     * con un mínimo de 1 minuto entre refrescos forzados.
     */
    fun getFriendsProfiles(force: Boolean = false, onComplete: (List<UserProfile>) -> Unit) {
        val firestore = db
        if (firestore == null) {
            Log.w(TAG, "getFriendsProfiles: Firebase no disponible. Regresando lista vacia.")
            onComplete(emptyList())
            return
        }

        val currentFriends = getProfile().friendsUids.distinct()
        if (currentFriends.isEmpty()) {
            onComplete(emptyList())
            return
        }

        val now = System.currentTimeMillis()
        val reallyForce = force && now - lastForcedMs >= FORCE_MIN_GAP_MS
        if (reallyForce) lastForcedMs = now
        val cache = loadFriendCache()
        fun assemble(): List<UserProfile> = currentFriends.mapNotNull { cache[it]?.profile }

        val stale = SyncPolicy.friendsToFetch(
            currentFriends,
            cache.mapValues { it.value.at },
            cache.mapValues { it.value.profile?.lastPlayDate ?: 0L },
            now, reallyForce
        )
        if (stale.isEmpty() || !isOnline()) {
            Log.d(TAG, "getFriendsProfiles: caché (0 lecturas)")
            onComplete(assemble())
            return
        }

        val signed = signedUid != null
        // Con sesión: lo conocido en `users` va directo allí; el resto, primero `players`
        val v2Ids = if (signed) stale.filter { cache[it]?.src != "u" } else emptyList()
        val legacyKnown = if (signed) stale.filter { cache[it]?.src == "u" } else stale

        readCollection(firestore, "players", v2Ids) { v2, v2Ok ->
            val v2Found = v2.map { it.uid }.toSet()
            val legacyIds = (legacyKnown + (v2Ids - v2Found)).distinct()
            readCollection(firestore, "users", legacyIds) { legacy, legacyOk ->
                val legacyFound = legacy.map { it.uid }.toSet()
                val t = System.currentTimeMillis()
                v2.forEach { cache[it.uid] = FriendEntry(t, "p", it) }
                legacy.forEach { cache[it.uid] = FriendEntry(t, "u", it) }
                // Quien no está en ninguna colección se recuerda como "no existe" (no se vuelve a pedir en 24 h)
                for (id in stale) {
                    if (id in v2Found || id in legacyFound) continue
                    val askedAll = id in legacyOk && (!signed || id in v2Ok || cache[id]?.src == "u")
                    if (askedAll) cache[id] = FriendEntry(t, "x", null)
                }
                saveFriendCache(cache, currentFriends)
                onComplete(assemble())
            }
        }
    }

    // Lee una colección por trozos de 10 (límite de whereIn). Devuelve lo encontrado y los ids de los trozos que SÍ respondieron.
    private fun readCollection(
        firestore: FirebaseFirestore, collection: String, ids: List<String>,
        done: (List<UserProfile>, Set<String>) -> Unit
    ) {
        if (ids.isEmpty()) { done(emptyList(), emptySet()); return }
        val chunks = ids.chunked(10)
        val found = mutableListOf<UserProfile>()
        val ok = mutableSetOf<String>()
        var finished = 0
        for (chunk in chunks) {
            firestore.collection(collection)
                .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                .limit(10)
                .get()
                .addOnSuccessListener { snap ->
                    found += snap.documents.mapNotNull { parseProfile(it) }
                    ok += chunk
                }
                .addOnCompleteListener { if (++finished == chunks.size) done(found, ok) }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseProfile(doc: com.google.firebase.firestore.DocumentSnapshot): UserProfile? = try {
        UserProfile(
            uid = doc.id,
            name = doc.getString("name") ?: "Jugador Zen",
            avatarId = doc.getLong("avatarId")?.toInt() ?: 1,
            bannerId = doc.getLong("bannerId")?.toInt() ?: 1,
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
            friendCode = doc.getString("friendCode") ?: "",
            prestige = doc.getLong("prestige")?.toInt() ?: 0,
            titleId = doc.getString("titleId") ?: "default",
            platinum = doc.getBoolean("platinum") ?: false,
            towerBest = doc.getLong("towerBest")?.toInt() ?: 0,
            pieces = doc.getLong("pieces")?.toInt() ?: 0
        )
    } catch (e: Exception) { null }

    // ---- Caché de amigos en disco ----
    private val socialPrefs: SharedPreferences by lazy { context.getSharedPreferences("pardos_social_cache", Context.MODE_PRIVATE) }

    private fun loadFriendCache(): MutableMap<String, FriendEntry> {
        friendCache?.let { return it }
        val map = mutableMapOf<String, FriendEntry>()
        try {
            val arr = org.json.JSONArray(socialPrefs.getString("friends", "[]"))
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                map[o.getString("id")] = FriendEntry(o.getLong("at"), o.getString("src"), o.optJSONObject("p")?.let { profileFromJson(it) })
            }
        } catch (e: Exception) {
            Log.w(TAG, "Caché de amigos ilegible, se descarta: ${e.message}")
        }
        friendCache = map
        return map
    }

    private fun saveFriendCache(map: MutableMap<String, FriendEntry>, keep: List<String>) {
        map.keys.retainAll(keep.toSet())
        try {
            val arr = org.json.JSONArray()
            for ((id, e) in map) {
                val o = org.json.JSONObject().put("id", id).put("at", e.at).put("src", e.src)
                e.profile?.let { o.put("p", profileToJson(it)) }
                arr.put(o)
            }
            socialPrefs.edit().putString("friends", arr.toString()).apply()
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo guardar la caché de amigos: ${e.message}")
        }
    }

    private fun profileToJson(p: UserProfile): org.json.JSONObject = org.json.JSONObject()
        .put("uid", p.uid).put("name", p.name).put("avatarId", p.avatarId).put("bannerId", p.bannerId)
        .put("playerLevel", p.playerLevel).put("currentCampaignLevel", p.currentCampaignLevel)
        .put("currentXp", p.currentXp).put("xpToNextLevel", p.xpToNextLevel)
        .put("currentStreak", p.currentStreak).put("bestStreak", p.bestStreak).put("lastPlayDate", p.lastPlayDate)
        .put("friendsUids", org.json.JSONArray(p.friendsUids)).put("unlockedBadges", org.json.JSONArray(p.unlockedBadges))
        .put("pinnedRecords", org.json.JSONArray(p.pinnedRecords))
        .put("weeklyStars", p.weeklyStars).put("weekId", p.weekId).put("friendCode", p.friendCode)
        .put("prestige", p.prestige).put("titleId", p.titleId).put("platinum", p.platinum).put("towerBest", p.towerBest).put("pieces", p.pieces)

    private fun profileFromJson(o: org.json.JSONObject): UserProfile {
        fun list(key: String): List<String> = o.optJSONArray(key)?.let { a -> List(a.length()) { a.optString(it) } } ?: emptyList()
        return UserProfile(
            uid = o.optString("uid"), name = o.optString("name", "Jugador Zen"),
            avatarId = o.optInt("avatarId", 1), bannerId = o.optInt("bannerId", 1),
            playerLevel = o.optInt("playerLevel", 1), currentCampaignLevel = o.optInt("currentCampaignLevel", 1),
            currentXp = o.optInt("currentXp", 0), xpToNextLevel = o.optInt("xpToNextLevel", 100),
            currentStreak = o.optInt("currentStreak", 0), bestStreak = o.optInt("bestStreak", 0),
            lastPlayDate = o.optLong("lastPlayDate", 0L),
            friendsUids = list("friendsUids"), unlockedBadges = list("unlockedBadges"),
            pinnedRecords = list("pinnedRecords").ifEmpty { listOf("", "", "") },
            weeklyStars = o.optInt("weeklyStars", 0), weekId = o.optInt("weekId", 0), friendCode = o.optString("friendCode", ""),
            prestige = o.optInt("prestige", 0), titleId = o.optString("titleId", "default"), platinum = o.optBoolean("platinum", false),
            towerBest = o.optInt("towerBest", 0), pieces = o.optInt("pieces", 0)
        )
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
                            bannerId = doc.getLong("bannerId")?.toInt() ?: local.bannerId,
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

    /** ¿Vale la pena consultar la nube al arrancar? Solo en un perfil recién instalado o si pasó más de un día. */
    fun shouldCheckCloud(): Boolean {
        val p = getProfile()
        val fresh = p.playerLevel == 1 && p.currentXp == 0 && p.name == "Jugador Zen"
        return SyncPolicy.shouldCheckCloud(fresh, prefs.getLong("cloud_check_ms", 0L), System.currentTimeMillis())
    }

    fun syncFromFirebase(onResult: (UserProfile?) -> Unit) {
        val firestore = db
        if (firestore == null) {
            Log.w(TAG, "syncFromFirebase omitido: Firebase no disponible.")
            onResult(null)
            return
        }
        prefs.edit().putLong("cloud_check_ms", System.currentTimeMillis()).apply()

        firestore.collection(profilesCollection).document(cloudId)
            .get(com.google.firebase.firestore.Source.SERVER)
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    Log.d(TAG, "Datos encontrados en la nube para el ID: $deviceId")

                    val cloudProfile = UserProfile(
                        uid = document.getString("uid") ?: deviceId,
                        name = document.getString("name") ?: "Jugador Zen",
                        avatarId = document.getLong("avatarId")?.toInt() ?: 1,
                        bannerId = document.getLong("bannerId")?.toInt() ?: 1,
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

