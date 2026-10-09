package com.korkoor.pardos.data.local

import android.content.Context
import android.util.Log
import com.korkoor.pardos.domain.collection.CollectibleCatalog
import com.korkoor.pardos.domain.collection.TradeOffer
import com.korkoor.pardos.domain.collection.TradeRules
import org.json.JSONArray
import org.json.JSONObject

/** Una propuesta que me hicieron: [give] es lo que recibiría y [want] lo que tendría que entregar. */
data class TradeInbox(val id: String, val fromUid: String, val fromName: String, val give: String, val want: String, val createdAt: Long) {
    val expired: Boolean get() = System.currentTimeMillis() - createdAt > TradeRules.EXPIRE_DAYS * 86_400_000L
}

/** Una propuesta mía que sigue esperando respuesta. */
data class TradeOutgoing(val id: String, val toUid: String, val toName: String, val give: String, val want: String, val cost: Int, val createdAt: Long)

/**
 * Intercambio de cartas repetidas con amigos.
 *
 * Al proponer: se aparta tu copia repetida y se gastan las fichas (así no se puede ofrecer dos veces lo mismo). La propuesta viaja por
 * Firestore (`trades/{id}`). Quien la recibe entrega la suya y se la queda; tú la recibes la próxima vez que abras el Álbum.
 * Si la rechazan, la cancelas o caduca (7 días), recuperas la carta y las fichas.
 */
class TradeManager(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("pardos_trades", Context.MODE_PRIVATE)
    private val collection = CollectionManager(appContext)
    private val profiles = ProfileManager(appContext)

    private val db get() = profiles.cloudDatabase()
    private val uid: String? get() = profiles.signedInUid()

    /** ¿Se puede intercambiar? Hace falta sesión (los amigos solo existen con cuenta). */
    val available: Boolean get() = uid != null && db != null

    // ---------------- Propuestas propias (guardadas en el teléfono) ----------------

    fun outgoing(): List<TradeOutgoing> = try {
        val arr = JSONArray(prefs.getString(KEY_OUT, "[]"))
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            TradeOutgoing(o.getString("id"), o.optString("to"), o.optString("toName"), o.getString("give"), o.getString("want"), o.optInt("cost", 1), o.optLong("at"))
        }
    } catch (e: Exception) { emptyList() }

    private fun saveOutgoing(list: List<TradeOutgoing>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().put("id", it.id).put("to", it.toUid).put("toName", it.toName).put("give", it.give).put("want", it.want).put("cost", it.cost).put("at", it.createdAt))
        }
        prefs.edit().putString(KEY_OUT, arr.toString()).apply()
    }

    private fun refund(o: TradeOutgoing) {
        collection.receive(o.give)
        collection.addTokens(o.cost)
        saveOutgoing(outgoing().filter { it.id != o.id })
    }

    private fun message(check: TradeRules.Check): String = when (check) {
        TradeRules.Check.Ok -> ""
        TradeRules.Check.SameRarityOnly -> "Solo se puede cambiar una carta por otra de la misma rareza."
        TradeRules.Check.NoSpareToGive -> "Necesitas una copia repetida de esa carta."
        TradeRules.Check.NotEnoughTokens -> "Te faltan fichas de intercambio."
        TradeRules.Check.Unknown -> "Esa carta no existe."
        TradeRules.Check.SamePiece -> "Elige dos cartas distintas."
        TradeRules.Check.TooManyPending -> "Ya tienes ${TradeRules.MAX_PENDING_OUT} propuestas esperando respuesta."
    }

    // ---------------- Proponer ----------------

    /** [onResult] recibe null si salió bien o el mensaje del problema. */
    fun propose(friendUid: String, friendName: String, offer: TradeOffer, onResult: (String?) -> Unit) {
        val me = uid
        val firestore = db
        if (me == null || firestore == null) { onResult("Inicia sesión con tu cuenta de Google para intercambiar."); return }
        val check = TradeRules.canPropose(collection.copies.value, collection.tokens.value, offer, outgoing().size)
        if (check != TradeRules.Check.Ok) { onResult(message(check)); return }
        val give = CollectibleCatalog.byId(offer.give) ?: return onResult(message(TradeRules.Check.Unknown))
        val cost = TradeRules.cost(give.rarity)

        // Primero se aparta y se cobra (para que no se pueda ofrecer dos veces); si falla la red, se devuelve
        if (!collection.escrow(offer.give)) { onResult(message(TradeRules.Check.NoSpareToGive)); return }
        if (!collection.spendTokens(cost)) { collection.receive(offer.give); onResult(message(TradeRules.Check.NotEnoughTokens)); return }

        val now = System.currentTimeMillis()
        val doc = firestore.collection("trades").document()
        val myName = profiles.getProfile().name
        val data = hashMapOf<String, Any>(
            "from" to me, "fromName" to myName, "to" to friendUid, "give" to offer.give, "want" to offer.want,
            "status" to "pending", "createdAt" to now
        )
        doc.set(data)
            .addOnSuccessListener {
                saveOutgoing(outgoing() + TradeOutgoing(doc.id, friendUid, friendName, offer.give, offer.want, cost, now))
                com.korkoor.pardos.audio.GameAudio.play(com.korkoor.pardos.audio.Sfx.TRADE_SEND)
                onResult(null)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "No se pudo crear la propuesta: ${e.message}")
                collection.receive(offer.give); collection.addTokens(cost)
                onResult("No se pudo enviar la propuesta. Revisa tu conexión e inténtalo otra vez.")
            }
    }

    // ---------------- Recibir ----------------

    fun fetchIncoming(onDone: (List<TradeInbox>) -> Unit) {
        val me = uid
        val firestore = db
        if (me == null || firestore == null) { onDone(emptyList()); return }
        firestore.collection("trades").whereEqualTo("to", me).whereEqualTo("status", "pending").limit(20).get()
            .addOnSuccessListener { snap ->
                onDone(snap.documents.mapNotNull { d ->
                    val give = d.getString("give") ?: return@mapNotNull null
                    val want = d.getString("want") ?: return@mapNotNull null
                    if (CollectibleCatalog.byId(give) == null || CollectibleCatalog.byId(want) == null) return@mapNotNull null
                    TradeInbox(d.id, d.getString("from") ?: "", d.getString("fromName") ?: "Un amigo", give, want, d.getLong("createdAt") ?: 0L)
                        .takeIf { !it.expired }
                }.sortedByDescending { it.createdAt })
            }
            .addOnFailureListener { Log.w(TAG, "No se pudieron leer las propuestas: ${it.message}"); onDone(emptyList()) }
    }

    /** Acepta: entregas tu carta repetida y te quedas la que te ofrecen. */
    fun accept(inbox: TradeInbox, onResult: (String?) -> Unit) {
        val firestore = db ?: return onResult("Sin conexión con la nube.")
        if (inbox.expired) { onResult("Esta propuesta caducó."); return }
        if (!TradeRules.canAccept(collection.copies.value, TradeOffer(inbox.give, inbox.want))) { onResult("Necesitas una copia repetida de ${CollectibleCatalog.byId(inbox.want)?.nameEs ?: "esa carta"}."); return }
        if (!collection.escrow(inbox.want)) { onResult("Necesitas una copia repetida de esa carta."); return }
        firestore.collection("trades").document(inbox.id)
            .update(mapOf("status" to "accepted", "resolvedAt" to System.currentTimeMillis()))
            .addOnSuccessListener { collection.receive(inbox.give); com.korkoor.pardos.audio.GameAudio.play(com.korkoor.pardos.audio.Sfx.TRADE_DONE); onResult(null) }
            .addOnFailureListener { e ->
                Log.w(TAG, "No se pudo aceptar: ${e.message}")
                collection.receive(inbox.want)
                onResult("No se pudo completar el intercambio. Puede que ya no esté disponible.")
            }
    }

    fun decline(inbox: TradeInbox, onResult: (Boolean) -> Unit) {
        val firestore = db ?: return onResult(false)
        firestore.collection("trades").document(inbox.id)
            .update(mapOf("status" to "declined", "resolvedAt" to System.currentTimeMillis()))
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    // ---------------- Resolver las mías ----------------

    /**
     * Mira cómo van mis propuestas: si me aceptaron recibo la carta; si las rechazaron o caducaron, recupero la mía y las fichas.
     * [onDone] recibe los avisos para enseñar al jugador.
     */
    fun syncOutgoing(onDone: (List<String>) -> Unit) {
        val firestore = db
        val pending = outgoing()
        if (firestore == null || pending.isEmpty()) { onDone(emptyList()); return }
        val notes = mutableListOf<String>()
        var left = pending.size
        fun finish() { if (--left == 0) onDone(notes) }
        for (o in pending) {
            firestore.collection("trades").document(o.id).get()
                .addOnSuccessListener { d ->
                    val status = d.getString("status")
                    val giveName = CollectibleCatalog.byId(o.give)?.nameEs ?: o.give
                    val wantName = CollectibleCatalog.byId(o.want)?.nameEs ?: o.want
                    when {
                        !d.exists() -> { refund(o); notes += "La propuesta a ${o.toName} ya no existe: recuperaste $giveName." }
                        status == "accepted" -> {
                            collection.receive(o.want)
                            saveOutgoing(outgoing().filter { it.id != o.id })
                            com.korkoor.pardos.audio.GameAudio.play(com.korkoor.pardos.audio.Sfx.TRADE_DONE)
                            notes += "¡${o.toName} aceptó! Cambiaste $giveName por $wantName."
                        }
                        status == "declined" || status == "cancelled" -> { refund(o); notes += "${o.toName} no aceptó: recuperaste $giveName y tus fichas." }
                        System.currentTimeMillis() - o.createdAt > TradeRules.EXPIRE_DAYS * 86_400_000L -> {
                            // caducó: se cancela en la nube y se devuelve
                            d.reference.update(mapOf("status" to "cancelled", "resolvedAt" to System.currentTimeMillis()))
                                .addOnSuccessListener { refund(o) }
                            notes += "La propuesta a ${o.toName} caducó: recuperaste $giveName y tus fichas."
                        }
                    }
                    finish()
                }
                .addOnFailureListener { finish() }
        }
    }

    /** Cancela una propuesta mía que sigue esperando. */
    fun cancel(o: TradeOutgoing, onResult: (Boolean) -> Unit) {
        val firestore = db ?: return onResult(false)
        firestore.collection("trades").document(o.id).update(mapOf("status" to "cancelled", "resolvedAt" to System.currentTimeMillis()))
            .addOnSuccessListener { refund(o); onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    private companion object {
        const val TAG = "TradeManager"
        const val KEY_OUT = "outgoing"
    }
}
