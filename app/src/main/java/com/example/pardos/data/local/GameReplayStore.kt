package com.korkoor.pardos.data.local

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ReplayTile(val row: Int, val col: Int, val value: Int)
data class ReplayFrame(val move: Int, val direction: String, val score: Int, val tiles: List<ReplayTile>)
data class SavedGameReplay(val boardSize: Int, val target: Int, val finalScore: Int, val frames: List<ReplayFrame>, val savedAt: Long)

/** Guarda las últimas partidas localmente para poder recorrer sus tableros jugada a jugada. */
class GameReplayStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("pardos_replays", Context.MODE_PRIVATE)

    fun save(replay: SavedGameReplay) {
        val history = load().toMutableList()
        history.removeAll { it.savedAt == replay.savedAt }
        history.add(0, replay.copy(frames = replay.frames.takeLast(MAX_FRAMES)))
        prefs.edit().putString(KEY_HISTORY, JSONArray().apply {
            history.take(MAX_REPLAYS).forEach { put(encodeReplay(it)) }
        }.toString()).apply()
    }

    fun latest(): SavedGameReplay? = load().firstOrNull()

    private fun load(): List<SavedGameReplay> = try {
        val array = JSONArray(prefs.getString(KEY_HISTORY, "[]"))
        (0 until array.length()).mapNotNull { index -> decodeReplay(array.optJSONObject(index)) }
    } catch (_: Exception) { emptyList() }

    private fun encodeReplay(replay: SavedGameReplay) = JSONObject().apply {
        put("size", replay.boardSize)
        put("target", replay.target)
        put("score", replay.finalScore)
        put("savedAt", replay.savedAt)
        put("frames", JSONArray().apply {
            replay.frames.forEach { frame ->
                put(JSONObject().apply {
                    put("move", frame.move)
                    put("direction", frame.direction)
                    put("score", frame.score)
                    put("tiles", JSONArray().apply {
                        frame.tiles.forEach { tile ->
                            put(JSONArray().put(tile.row).put(tile.col).put(tile.value))
                        }
                    })
                })
            }
        })
    }

    private fun decodeReplay(json: JSONObject?): SavedGameReplay? {
        return try {
            if (json == null) return null
            val size = json.getInt("size")
            val framesJson = json.getJSONArray("frames")
            val frames = (0 until framesJson.length()).mapNotNull { i ->
                val frame = framesJson.optJSONObject(i) ?: return@mapNotNull null
                val tilesJson = frame.getJSONArray("tiles")
                val tiles = (0 until tilesJson.length()).mapNotNull { tileIndex ->
                    val tile = tilesJson.optJSONArray(tileIndex) ?: return@mapNotNull null
                    if (tile.length() < 3) return@mapNotNull null
                    ReplayTile(tile.getInt(0), tile.getInt(1), tile.getInt(2))
                }
                ReplayFrame(frame.getInt("move"), frame.getString("direction"), frame.getInt("score"), tiles)
            }
            if (size !in 3..6 || frames.isEmpty()) return null
            SavedGameReplay(size, json.getInt("target"), json.getInt("score"), frames, json.getLong("savedAt"))
        } catch (_: Exception) { null }
    }

    private companion object {
        const val KEY_HISTORY = "history_json"
        const val MAX_REPLAYS = 10
        const val MAX_FRAMES = 400
    }
}
