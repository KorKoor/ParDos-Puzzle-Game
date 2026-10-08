package com.korkoor.pardos.domain.retention

/** Una novedad que se muestra una sola vez a quien actualiza la app. [icon] es una clave que la app traduce a un dibujo. */
data class WhatsNewItem(val icon: String, val title: String, val body: String)

/**
 * Diálogo de "Novedades" tras una actualización. Se muestra una vez por versión y solo a quien ya jugaba:
 * un jugador nuevo no necesita saber qué cambió.
 */
object WhatsNew {
    /** Código de versión a partir del cual existen estas novedades. */
    const val SINCE_VERSION = 16
    const val MAX_ITEMS = 5

    val items: List<WhatsNewItem> = listOf(
        WhatsNewItem("map", "Un mapa con mundos", "12 capítulos con su propio cielo, paisaje y monumento. Tu avatar marca dónde vas."),
        WhatsNewItem("duel", "Retos a distancia", "Juega 60 s, comparte tu código y tus amigos juegan el mismo tablero. Sin cuentas ni esperas."),
        WhatsNewItem("chest", "Cofres, avatares y banners", "Cofres dibujados, animalitos para tu perfil y banners. Cómpralos con monedas o gánalos en el pase."),
        WhatsNewItem("league", "Ligas y fiestas", "Sube de Bronce a Diamante cada semana y consigue skins exclusivas en Halloween, Navidad y más."),
        WhatsNewItem("settings", "Más control", "Nuevos ajustes de sonido, vibración y avisos. Ahora puedes silenciar solo lo que quieras.")
    ).also { check(it.size <= MAX_ITEMS) }

    /**
     * ¿Mostrar las novedades?
     * [lastSeenVersion] = última versión cuyas novedades ya viste (0 = ninguna); [isFreshInstall] = instalación nueva.
     */
    fun shouldShow(lastSeenVersion: Int, currentVersion: Int, isFreshInstall: Boolean): Boolean {
        if (isFreshInstall) return false
        if (currentVersion < SINCE_VERSION) return false
        return lastSeenVersion < SINCE_VERSION
    }
}
