package com.korkoor.pardos.data.local

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.korkoor.pardos.domain.logic.RemoteChallenge
import com.korkoor.pardos.domain.logic.RemoteDuel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

data class TournamentSnapshot(
    val week: String,
    val seed: Long,
    val roundId: String,
    val alreadyPlayed: Boolean,
    val communityPoints: Long,
    val communityGoal: Long,
    val leaderboard: List<TournamentEntry>
)

data class TournamentEntry(val uid: String, val name: String, val score: Int)

/** Spark-compatible social data. Firestore stores replays for transparency; scores are client-reported. */
class TournamentRepository {
    private val db get() = FirebaseFirestore.getInstance()
    private val auth get() = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser

    fun load(onResult: (Result<TournamentSnapshot>) -> Unit) {
        val uid = auth?.uid
        if (uid == null) { onResult(Result.failure(IllegalStateException("Inicia sesión para competir."))); return }
        val week = weekKey()
        val tournament = db.collection("weekly_tournaments").document(week)
        val tasks = listOf(
            tournament.collection("scores").orderBy("score", com.google.firebase.firestore.Query.Direction.DESCENDING).limit(20).get(),
            db.collection("community_challenges").document(week).get(),
            tournament.collection("runs").document(uid).get(),
            tournament.collection("scores").document(uid).get()
        )
        Tasks.whenAllSuccess<Any>(tasks).addOnSuccessListener { values ->
            runCatching {
                val scores = values[0] as com.google.firebase.firestore.QuerySnapshot
                val community = values[1] as com.google.firebase.firestore.DocumentSnapshot
                val run = values[2] as com.google.firebase.firestore.DocumentSnapshot
                val ownScore = values[3] as com.google.firebase.firestore.DocumentSnapshot
                val entries = scores.documents.map { doc ->
                    TournamentEntry(doc.id, doc.getString("name") ?: "Jugador", (doc.getLong("score") ?: 0L).toInt())
                }
                TournamentSnapshot(
                    week = week,
                    seed = seedForWeek(week),
                    roundId = run.getString("roundId").orEmpty(),
                    alreadyPlayed = ownScore.exists(),
                    communityPoints = community.getLong("points") ?: 0L,
                    communityGoal = community.getLong("goal") ?: COMMUNITY_GOAL,
                    leaderboard = entries
                )
            }.onSuccess { onResult(Result.success(it)) }.onFailure { onResult(Result.failure(it)) }
        }.addOnFailureListener { onResult(Result.failure(it)) }
    }

    /** Creates or resumes this account's one weekly round; no server-side code is needed. */
    fun start(onResult: (Result<Pair<Long, String>>) -> Unit) {
        val uid = auth?.uid
        if (uid == null) { onResult(Result.failure(IllegalStateException("Inicia sesión para competir."))); return }
        val week = weekKey()
        val tournament = db.collection("weekly_tournaments").document(week)
        val scoreDoc = tournament.collection("scores").document(uid)
        val runDoc = tournament.collection("runs").document(uid)
        val newId = UUID.randomUUID().toString()
        db.runTransaction { tx ->
            if (tx.get(scoreDoc).exists()) throw IllegalStateException("Ya participaste esta semana.")
            val existing = tx.get(runDoc)
            if (existing.exists()) existing.getString("roundId") ?: newId
            else {
                tx.set(runDoc, mapOf("roundId" to newId, "startedAt" to FieldValue.serverTimestamp(), "submitted" to false))
                newId
            }
        }.addOnSuccessListener { onResult(Result.success(seedForWeek(week) to it)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    /** Stores the replay and result in one Firestore transaction so the shared total cannot drift. */
    fun submit(roundId: String, actions: List<String>, score: Int, name: String, onResult: (Result<Pair<Int, Long>>) -> Unit) {
        val uid = auth?.uid
        if (uid == null) { onResult(Result.failure(IllegalStateException("Inicia sesión para competir."))); return }
        if (actions.size > 200 || score < 0 || score > 10_000_000) {
            onResult(Result.failure(IllegalArgumentException("La partida excede los límites del torneo."))); return
        }
        val week = weekKey()
        val tournament = db.collection("weekly_tournaments").document(week)
        val runDoc = tournament.collection("runs").document(uid)
        val scoreDoc = tournament.collection("scores").document(uid)
        val communityDoc = db.collection("community_challenges").document(week)
        val contributionDoc = communityDoc.collection("contributors").document(uid)
        db.runTransaction { tx ->
            val run = tx.get(runDoc)
            val previousScore = tx.get(scoreDoc)
            val community = tx.get(communityDoc)
            val previousContribution = tx.get(contributionDoc)
            if (!run.exists() || run.getString("roundId") != roundId || run.getBoolean("submitted") == true) {
                throw IllegalStateException("Esta ronda ya se usó o no es válida.")
            }
            if (previousScore.exists()) throw IllegalStateException("Ya participaste esta semana.")
            val points = (community.getLong("points") ?: 0L) + score
            tx.update(runDoc, mapOf("submitted" to true, "score" to score, "actions" to actions))
            tx.set(scoreDoc, mapOf(
                "uid" to uid, "week" to week, "score" to score, "name" to name.take(30),
                "actions" to actions, "createdAt" to FieldValue.serverTimestamp()
            ))
            if (!previousContribution.exists()) {
                tx.set(contributionDoc, mapOf("uid" to uid, "week" to week, "score" to score))
                if (community.exists()) tx.update(communityDoc, "points", points)
                else tx.set(communityDoc, mapOf("week" to week, "goal" to COMMUNITY_GOAL, "points" to points))
            }
            points
        }.addOnSuccessListener { onResult(Result.success(score to it)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    fun sendFriendChallenge(friendUid: String, fromName: String, challenge: RemoteChallenge, onResult: (Result<Unit>) -> Unit) {
        val uid = auth?.uid
        if (uid == null || uid == friendUid) { onResult(Result.failure(IllegalStateException("Inicia sesión y elige una amistad."))); return }
        val id = "$uid~$friendUid"
        db.collection("friend_challenges").document(id).set(mapOf(
            "from" to uid, "fromName" to fromName.take(30), "to" to friendUid,
            "code" to RemoteDuel.encode(challenge), "status" to "pending", "createdAt" to System.currentTimeMillis()
        )).addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    companion object {
        const val COMMUNITY_GOAL = 100_000L

        /** Monday in UTC keeps every player on the same weekly board. */
        fun weekKey(): String {
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            val day = calendar.get(Calendar.DAY_OF_WEEK)
            val daysSinceMonday = (day + 5) % 7
            calendar.add(Calendar.DAY_OF_MONTH, -daysSinceMonday)
            return SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(calendar.time)
        }

        private fun seedForWeek(week: String): Long {
            var hash = 0x811c9dc5.toInt()
            week.forEach { hash = (hash xor it.code) * 0x01000193 }
            return hash.toLong() and 0xFFFF_FFFFL
        }
    }
}
