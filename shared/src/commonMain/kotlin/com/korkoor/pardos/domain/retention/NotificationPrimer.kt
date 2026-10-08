package com.korkoor.pardos.domain.retention

/**
 * Cuándo preguntar si quiere recibir avisos. Pedir el permiso nada más abrir la app, sin contexto, hace que casi todos digan
 * que no; preguntar después de la primera victoria, explicando qué se le avisará, hace que acepte mucha más gente.
 * Se insiste poco: como mucho [MAX_ASKS] veces, separadas [GAP_DAYS] días, y nunca a quien ya aceptó o los apagó en Ajustes.
 */
object NotificationPrimer {
    const val MAX_ASKS = 3
    const val GAP_DAYS = 3

    fun shouldAsk(
        permissionNeeded: Boolean,
        granted: Boolean,
        enabledInSettings: Boolean,
        hasWonAGame: Boolean,
        lastAskDay: Int?,
        askCount: Int,
        today: Int
    ): Boolean {
        if (!permissionNeeded || granted || !enabledInSettings) return false
        if (!hasWonAGame) return false
        if (askCount >= MAX_ASKS) return false
        return lastAskDay == null || today - lastAskDay >= GAP_DAYS
    }
}
