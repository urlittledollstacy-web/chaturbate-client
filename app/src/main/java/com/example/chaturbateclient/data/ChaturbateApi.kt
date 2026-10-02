package com.example.chaturbateclient.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ApiRoom(
    val username: String,
    val viewers: Int,
    val category: String,
    val imageUrl: String,
    val gender: String = "",
    val location: String = "",
    val country: String = "",
    val spokenLanguages: String = "",
    val tags: List<String> = emptyList()
)

data class PlaybackSource(val roomStatus: String, val hlsUrl: String?)

object ChaturbateApi {
    private const val BASE = "https://chaturbate.com"
    private const val ROOM_PAGE_LIMIT = 90

    // These are the four gender/category feeds exposed by the current Chaturbate room list.
    private val ROOM_GENDERS = listOf("f", "m", "c", "s")

    fun fetchOnlineRooms(): List<ApiRoom> {
        val rooms = LinkedHashMap<String, ApiRoom>()
        var successfulCategories = 0

        for (gender in ROOM_GENDERS) {
            runCatching {
                fetchAllGenderRooms(gender)
            }.onSuccess { genderRooms ->
                successfulCategories++
                genderRooms.forEach { room ->
                    rooms.putIfAbsent(room.username.lowercase(), room)
                }
            }
        }

        if (successfulCategories == 0) {
            throw IllegalStateException("Chaturbate room list requests all failed.")
        }

        return rooms.values.toList()
    }

    private fun fetchAllGenderRooms(gender: String): List<ApiRoom> {
        val rooms = ArrayList<ApiRoom>()
        var offset = 0
        var totalCount: Int? = null

        while (true) {
            val url = URL(
                BASE + "/api/ts/roomlist/room-list/" +
                    "?enable_recommendations=true" +
                    "&genders=" + encode(gender) +
                    "&limit=" + ROOM_PAGE_LIMIT +
                    "&offset=" + offset
            )

            val root = JSONObject(request(url))
            val page = parseRooms(root)
            if (totalCount == null) {
                totalCount = root.optInt("total_count", page.size)
            }

            if (page.isEmpty()) {
                break
            }

            rooms.addAll(page)
            offset += page.size

            if (offset >= (totalCount ?: offset)) {
                break
            }
        }

        return rooms
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

    private fun parseRooms(root: JSONObject): List<ApiRoom> {
        val results = root.optJSONArray("rooms") ?: return emptyList()

        return buildList {
            for (i in 0 until results.length()) {
                val item = results.optJSONObject(i) ?: continue
                val username = item.optString("username").trim()
                if (username.isEmpty()) continue

                val gender = item.optString("gender").trim()
                val subject = item.optString("room_subject").trim()
                val location = item.optString("location").trim()
                val country = item.optString("country").trim()
                val spokenLanguages = item.optString("spoken_languages").trim()
                val tags = parseTags(item)

                add(
                    ApiRoom(
                        username = username,
                        viewers = item.optInt(
                            "num_users",
                            item.optInt("num_viewers", 0)
                        ),
                        category = subject.ifBlank {
                            gender.ifBlank { "Live" }
                        },
                        imageUrl = item.optString("image_url"),
                        gender = gender,
                        location = location,
                        country = country,
                        spokenLanguages = spokenLanguages,
                        tags = tags
                    )
                )
            }
        }
    }

    private fun parseTags(item: JSONObject): List<String> {
        val array = item.optJSONArray("tags") ?: return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val tag = array.optString(i).trim()
                if (tag.isNotEmpty()) add(tag)
            }
        }
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
            if (code !in 200..299) {
                throw IllegalStateException("Chaturbate API returned HTTP " + code)
            }

            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun encode(value: String): String =
        java.net.URLEncoder.encode(value, Charsets.UTF_8.name())
}
