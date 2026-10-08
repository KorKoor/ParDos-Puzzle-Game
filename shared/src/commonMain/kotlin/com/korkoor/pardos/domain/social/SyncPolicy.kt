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
    const val CLOUD_CHECK_MS = 24L * 60 * 60_000L

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
}
