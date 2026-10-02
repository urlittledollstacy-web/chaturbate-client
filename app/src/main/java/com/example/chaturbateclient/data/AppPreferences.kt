package com.example.chaturbateclient.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class AppPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("client_preferences", Context.MODE_PRIVATE)
    var autoPlay: Boolean
        get() = prefs.getBoolean("auto_play", true)
        set(value) = prefs.edit().putBoolean("auto_play", value).apply()
    var dataSaver: Boolean
        get() = prefs.getBoolean("data_saver", false)
        set(value) = prefs.edit().putBoolean("data_saver", value).apply()
    var preferredQuality: String
        get() = prefs.getString("preferred_quality", "Auto") ?: "Auto"
        set(value) = prefs.edit().putString("preferred_quality", value).apply()

    fun loadFavorites(): Set<String> =
        prefs.getStringSet("favorites", emptySet())?.toSet() ?: emptySet()

    fun saveFavorites(values: Set<String>) =
        prefs.edit().putStringSet("favorites", values).apply()

    fun loadRoomCache(): List<ApiRoom> {
        val raw = prefs.getString("room_cache", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    val tags = item.optJSONArray("tags")?.let { tagsArray ->
                        buildList {
                            for (j in 0 until tagsArray.length()) add(tagsArray.optString(j))
                        }
                    } ?: emptyList()

                    add(
                        ApiRoom(
                            username = item.optString("username"),
                            viewers = item.optInt("viewers"),
                            category = item.optString("category"),
                            imageUrl = item.optString("imageUrl"),
                            gender = item.optString("gender"),
                            location = item.optString("location"),
                            country = item.optString("country"),
                            spokenLanguages = item.optString("spokenLanguages"),
                            tags = tags
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveRoomCache(rooms: List<ApiRoom>) {
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
        prefs.edit().putString("room_cache", array.toString()).apply()
    }
}
