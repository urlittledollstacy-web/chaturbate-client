package com.example.chaturbateclient.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Transactional, versioned store for the room catalogue.
 *
 * JSON is parsed and serialized off the main thread by callers. The cache carries
 * a schema version and a completeness flag so a partial refresh can never replace
 * a more complete snapshot.
 */
interface RoomCacheStore {
    fun load(): List<ApiRoom>
    fun save(rooms: List<ApiRoom>)
}

class JsonRoomCacheStore(context: Context) : RoomCacheStore {
    private val prefs = context.applicationContext
        .getSharedPreferences("client_preferences", Context.MODE_PRIVATE)

    override fun load(): List<ApiRoom> {
        val raw = prefs.getString(KEY_CACHE, null) ?: return emptyList()
        val root = runCatching { JSONObject(raw) }.getOrNull() ?: return emptyList()
        if (root.optInt("schemaVersion", 0) != SCHEMA_VERSION) return emptyList()
        val array = root.optJSONArray("rooms") ?: return emptyList()
        return runCatching { parseRooms(array) }.getOrDefault(emptyList())
    }

    override fun save(rooms: List<ApiRoom>) {
        val array = JSONArray()
        rooms.forEach { room ->
            array.put(JSONObject().apply {
                put("username", room.username)
                put("viewers", room.viewers)
                put("category", room.category)
                put("imageUrl", room.imageUrl)
                put("gender", room.gender)
                put("location", room.location)
                put("country", room.country)
                put("spokenLanguages", room.spokenLanguages)
                put("tags", JSONArray(room.tags))
            })
        }
        val root = JSONObject().apply {
            put("schemaVersion", SCHEMA_VERSION)
            put("complete", false)
            put("savedAtMs", System.currentTimeMillis())
            put("rooms", array)
        }
        prefs.edit().putString(KEY_CACHE, root.toString()).apply()
    }

    private fun parseRooms(array: JSONArray): List<ApiRoom> = buildList {
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val username = item.optString("username").trim()
            if (username.isEmpty()) continue
            add(
                ApiRoom(
                    username = username,
                    viewers = item.optInt("viewers", 0).coerceAtLeast(0),
                    category = item.optString("category"),
                    imageUrl = item.optString("imageUrl"),
                    gender = item.optString("gender"),
                    location = item.optString("location"),
                    country = item.optString("country"),
                    spokenLanguages = item.optString("spokenLanguages"),
                    tags = parseTags(item.optJSONArray("tags"))
                )
            )
        }
    }

    private fun parseTags(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                array.optString(i).trim().takeIf(String::isNotBlank)?.let(::add)
            }
        }
    }

    private companion object {
        const val KEY_CACHE = "room_cache_v2"
        const val SCHEMA_VERSION = 2
    }
}
