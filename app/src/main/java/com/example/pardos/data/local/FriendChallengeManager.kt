package com.korkoor.pardos.data.local

import android.content.Context
import com.google.firebase.firestore.ListenerRegistration
import com.korkoor.pardos.domain.logic.RemoteChallenge
import com.korkoor.pardos.domain.logic.RemoteDuel

data class FriendChallengeInvite(val id: String, val fromName: String, val challenge: RemoteChallenge)

/** Invitaciones de duelo privadas entre cuentas con sesión; los códigos siguen usando el motor ya existente. */
class FriendChallengeManager(context: Context) {
    private val profiles = ProfileManager(context.applicationContext)
    private val db get() = profiles.cloudDatabase()
    private val uid get() = profiles.signedInUid()

    fun send(friendUid: String, fromName: String, challenge: RemoteChallenge, onDone: (String?) -> Unit) {
        val currentUid = uid
        if (currentUid == null || db == null || friendUid == currentUid) {
            onDone("Inicia sesión y agrega a esa persona como amistad para enviarle el reto.")
            return
        }
        TournamentRepository().sendFriendChallenge(friendUid, fromName, challenge) { result ->
            onDone(result.exceptionOrNull()?.localizedMessage?.take(120))
        }
    }

    fun listenInbox(onChanged: (List<FriendChallengeInvite>) -> Unit): ListenerRegistration? {
        val currentUid = uid ?: return null
        val database = db ?: return null
        return database.collection(COLLECTION)
            .whereEqualTo("to", currentUid)
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val invites = snapshot.documents
                    .filter { it.getString("status") == "pending" }
                    .sortedByDescending { it.getLong("createdAt") ?: 0L }
                    .take(20)
                    .mapNotNull { document ->
                        val challenge = document.getString("code")?.let(RemoteDuel::decode) ?: return@mapNotNull null
                        FriendChallengeInvite(document.id, document.getString("fromName").orEmpty(), challenge)
                    }
                onChanged(invites)
            }
    }

    fun markAccepted(inviteId: String, onDone: (Boolean) -> Unit = {}) {
        val currentUid = uid
        val database = db
        if (currentUid == null || database == null) { onDone(false); return }
        database.collection(COLLECTION).document(inviteId)
            .delete().addOnSuccessListener { onDone(true) }.addOnFailureListener { onDone(false) }
    }

    companion object { const val COLLECTION = "friend_challenges" }
}
