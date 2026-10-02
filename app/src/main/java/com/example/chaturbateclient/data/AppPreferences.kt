package com.example.chaturbateclient.data

import android.content.Context
import org.json.JSONArray

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

    /** Legacy-only migration path. The catalogue is now stored in SQLite. */
    fun loadLegacyRoomCache(): List<ApiRoom> {
        val raw = prefs.getString("room_cache", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val tagsArray = item.optJSONArray("tags")
                    val tags = buildList {
                        if (tagsArray != null) {
                            for (j in 0 until tagsArray.length()) {
                                tagsArray.optString(j).trim().takeIf(String::isNotBlank)?.let(::add)
                            }
                        }
                    }
                    val username = item.optString("username").trim()
                    if (username.isNotEmpty()) add(
                        ApiRoom(
                            username = username,
                            viewers = item.optInt("viewers", 0).coerceAtLeast(0),
                            category = item.optString("category", "Live"),
                            subject = item.optString("category", ""),
                            imageUrl = item.optString("imageUrl", ""),
                            gender = item.optString("gender", ""),
                            location = item.optString("location", ""),
                            country = item.optString("country", ""),
                            spokenLanguages = item.optString("spokenLanguages", ""),
                            tags = tags
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun clearLegacyRoomCache() {
        prefs.edit().remove("room_cache").apply()
    }
}
