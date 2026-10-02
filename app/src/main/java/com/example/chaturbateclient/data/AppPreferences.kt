package com.example.chaturbateclient.data

import android.content.Context

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
}
