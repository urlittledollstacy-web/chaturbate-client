package com.example.chaturbateclient.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Bumped whenever the persisted catalogue shape changes. */
const val ROOM_CACHE_SCHEMA_VERSION = 2

/**
 * Pure encode/decode for the room-cache payload.
 *
 * Kept free of Android types so it can be unit tested on the JVM. Any malformed
 * or stale payload decodes to an empty list rather than throwing.
 */
object RoomCacheCodec {
    private const val KEY_SCHEMA = "schemaVersion"
    private const val KEY_ROOMS = "rooms"

    fun encode(rooms: List<ApiRoom>): String {
        val array = JSONArray()
        rooms.forEach { room ->
            array.put(
                JSONObject().apply {
                    put("username", room.username)
                    put("viewers", room.viewers)
                    put("category", room.category)
                    put("imageUrl", room.imageUrl)
                    put("gender", room.gender)
                    put("location", room.location)
                    put("country", room.country)
                    put("spokenLanguages", room.spokenLanguages)
                    put("tags", JSONArray(room.tags))
                }
            )
        }
        return JSONObject().apply {
            put(KEY_SCHEMA, ROOM_CACHE_SCHEMA_VERSION)
            put("complete", false)
            put("savedAtMs", System.currentTimeMillis())
            put(KEY_ROOMS, array)
        }.toString()
    }

    fun decode(raw: String?): List<ApiRoom> {
        if (raw.isNullOrBlank()) return emptyList()
        val root = runCatching { JSONObject(raw) }.getOrNull() ?: return emptyList()
        if (root.optInt(KEY_SCHEMA, 0) != ROOM_CACHE_SCHEMA_VERSION) return emptyList()
        val array = root.optJSONArray(KEY_ROOMS) ?: return emptyList()
        return runCatching {
            buildList {
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
                            tags = decodeTags(item.optJSONArray("tags")),
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun decodeTags(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                array.optString(i).trim().takeIf(String::isNotBlank)?.let(::add)
            }
        }
    }
}

interface RoomCacheStore {
    fun load(): List<ApiRoom>
    fun save(rooms: List<ApiRoom>)
}

class JsonRoomCacheStore(context: Context) : RoomCacheStore {
    private val prefs = context.applicationContext
        .getSharedPreferences("client_preferences", Context.MODE_PRIVATE)

    override fun load(): List<ApiRoom> = RoomCacheCodec.decode(prefs.getString(KEY_CACHE, null))

    override fun save(rooms: List<ApiRoom>) {
        prefs.edit().putString(KEY_CACHE, RoomCacheCodec.encode(rooms)).apply()
    }

    private companion object {
        const val KEY_CACHE = "room_cache_v2"
    }
}
