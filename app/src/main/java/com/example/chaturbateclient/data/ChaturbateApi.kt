package com.example.chaturbateclient.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ApiRoom(val username: String, val viewers: Int, val category: String, val imageUrl: String)
data class PlaybackSource(val roomStatus: String, val hlsUrl: String?)

object ChaturbateApi {
    private const val BASE = "https://chaturbate.com"

    fun fetchOnlineRooms(webmasterId: String, limit: Int = 100): List<ApiRoom> {
        require(webmasterId.isNotBlank()) { "A Chaturbate Webmaster ID is required for the official online-room feed." }
        val url = URL(BASE + "/api/public/affiliates/onlinerooms/?wm=" + encode(webmasterId) + "&client_ip=request_ip&limit=" + limit)
        val root = JSONObject(request(url))
        val results = root.optJSONArray("results") ?: return emptyList()
        return buildList {
            for (i in 0 until results.length()) {
                val item = results.optJSONObject(i) ?: continue
                val username = item.optString("username").trim()
                if (username.isEmpty()) continue
                add(ApiRoom(
                    username = username,
                    viewers = item.optInt("num_users", 0),
                    category = item.optString("room_subject").ifBlank { item.optString("gender").ifBlank { "Live" } },
                    imageUrl = item.optString("image_url")
                ))
            }
        }
    }

    fun fetchPlaybackSource(username: String): PlaybackSource {
        val clean = username.trim().lowercase()
        require(clean.isNotEmpty()) { "Username is empty." }
        val url = URL(BASE + "/api/chatvideocontext/" + encode(clean) + "/")
        val root = JSONObject(request(url))
        return PlaybackSource(
            roomStatus = root.optString("room_status", "offline"),
            hlsUrl = root.optString("hls_source").takeIf { it.isNotBlank() }
        )
    }

    private fun request(url: URL): String {
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "ChaturbateClient/0.1 Android")
            setRequestProperty("Referer", BASE + "/")
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) throw IllegalStateException("Chaturbate API returned HTTP " + code)
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally { connection.disconnect() }
    }

    private fun encode(value: String): String = java.net.URLEncoder.encode(value, Charsets.UTF_8.name())
}
