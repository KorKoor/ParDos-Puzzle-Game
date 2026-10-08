package com.korkoor.pardos.domain.social

/**
 * Reglas para gastar lo mínimo de Firestore (plan gratis: ~20 000 escrituras y ~50 000 lecturas al día).
 * Todo es lógica pura para poder probarla y reutilizarla en iOS; la app solo ejecuta lo que decide.
 *
 * Idea: el perfil vive en el teléfono; la nube es copia de seguridad y vitrina para los amigos.
 *  - Las escrituras se JUNTAN (varias victorias seguidas = 1 subida) y se saltan si nada cambió.
 *  - Las lecturas de amigos se guardan en caché unos minutos.
 *  - La nube solo se consulta al arrancar si hay motivo (reinstalación o hace más de un día).
 */
object SyncPolicy {
    /** Espera tras el último cambio antes de subir: junta ráfagas de cambios (ganar, subir de nivel, racha…). */
    const val UPLOAD_DEBOUNCE_MS = 45_000L
    /** Separación mínima entre dos subidas, pase lo que pase. */
    const val MIN_UPLOAD_GAP_MS = 60_000L
    /** Vida de la caché de perfiles de amigos. */
    const val FRIENDS_CACHE_MS = 15L * 60_000L
    /** Cada cuánto se vuelve a mirar la nube al arrancar (si el perfil local ya tiene progreso). */
    const val CLOUD_CHECK_MS = 3L * 24 * 60 * 60_000L
    /** Máximo de amigos: acota las lecturas de la lista (cada amigo = 1 lectura al refrescar). */
    const val MAX_FRIENDS = 100
    /** Búsquedas de código de amigo permitidas por hora (frena el abuso y las lecturas inútiles). */
    const val MAX_LOOKUPS_PER_HOUR = 12
    const val MIN_LOOKUP_GAP_MS = 1_500L
    private const val HOUR_MS = 60L * 60_000L
    private const val DAY_MS = 24L * HOUR_MS

    /** ¿Toca subir el perfil ahora? */
    fun shouldUpload(dirty: Boolean, contentChanged: Boolean, lastUploadMs: Long, nowMs: Long): Boolean {
        if (!dirty || !contentChanged) return false
        return lastUploadMs <= 0L || nowMs - lastUploadMs >= MIN_UPLOAD_GAP_MS
    }

    /** Milisegundos que faltan para poder subir otra vez (0 = ya se puede). */
    fun waitBeforeUpload(lastUploadMs: Long, nowMs: Long): Long =
        if (lastUploadMs <= 0L) 0L else (MIN_UPLOAD_GAP_MS - (nowMs - lastUploadMs)).coerceAtLeast(0L)

    /**
     * ¿Sirve la caché de amigos? Debe ser reciente y de la MISMA lista de amigos
     * (si agregaste o quitaste a alguien, hay que volver a leer).
     */
    fun friendsCacheValid(cachedIds: Set<String>?, currentIds: Set<String>, cachedAtMs: Long, nowMs: Long, force: Boolean): Boolean {
        if (force || cachedIds == null) return false
        if (cachedIds != currentIds) return false
        return nowMs - cachedAtMs in 0 until FRIENDS_CACHE_MS
    }

    /**
     * ¿Hay que leer el perfil de la nube al arrancar?
     * Sí si es un perfil recién instalado (puede haber progreso guardado) o hace más de un día que no se mira.
     */
    fun shouldCheckCloud(isFreshProfile: Boolean, lastCheckMs: Long, nowMs: Long): Boolean {
        if (isFreshProfile) return true
        return lastCheckMs <= 0L || nowMs - lastCheckMs >= CLOUD_CHECK_MS
    }

    /** Huella simple del contenido a subir: si no cambia, no se sube. */
    fun contentHash(fields: List<Any?>): Int = fields.joinToString("|") { it?.toString() ?: "∅" }.hashCode()

    /** Estimación de escrituras diarias de un jugador activo con [sessions] sesiones: una subida por sesión como máximo. */
    fun estimatedDailyWrites(sessions: Int): Int = sessions.coerceAtLeast(0)

    /**
     * Vida de la copia de UN amigo según cuánto lleva sin jugar: quien juega hoy cambia a cada rato; quien no ha vuelto
     * en semanas casi nunca cambia, así que se vuelve a leer a lo sumo una vez al día.
     */
    fun friendTtlMs(lastPlayMs: Long, nowMs: Long): Long {
        if (lastPlayMs <= 0L) return DAY_MS
        val idle = nowMs - lastPlayMs
        return when {
            idle < 3 * DAY_MS -> FRIENDS_CACHE_MS
            idle < 14 * DAY_MS -> 6 * HOUR_MS
            else -> DAY_MS
        }
    }

    /** Amigos que hay que leer de verdad: los que no están guardados o ya vencieron. [force] lee a todos. */
    fun friendsToFetch(current: List<String>, fetchedAt: Map<String, Long>, lastPlay: Map<String, Long>, nowMs: Long, force: Boolean): List<String> {
        if (force) return current
        return current.filter { id ->
            val at = fetchedAt[id] ?: return@filter true
            val age = nowMs - at
            age < 0 || age >= friendTtlMs(lastPlay[id] ?: 0L, nowMs)
        }
    }

    /** Espera tras [failures] subidas fallidas seguidas: 1, 2, 4… minutos hasta 6 horas (no se insiste contra una regla que rechaza). */
    fun uploadBackoffMs(failures: Int): Long {
        if (failures <= 0) return 0L
        val shift = (failures - 1).coerceAtMost(9)
        return (MIN_UPLOAD_GAP_MS shl shift).coerceAtMost(6 * HOUR_MS)
    }

    /** ¿Se permite otra búsqueda de código de amigo? [attemptsMs] = instantes de las búsquedas anteriores. */
    fun canLookupFriend(attemptsMs: List<Long>, nowMs: Long): Boolean {
        val recent = attemptsMs.filter { nowMs - it in 0 until HOUR_MS }
        if (recent.size >= MAX_LOOKUPS_PER_HOUR) return false
        val last = recent.maxOrNull() ?: return true
        return nowMs - last >= MIN_LOOKUP_GAP_MS
    }

    fun friendLimitReached(count: Int): Boolean = count >= MAX_FRIENDS
}
