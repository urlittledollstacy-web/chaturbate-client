package com.example.chaturbateclient.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope


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

    suspend fun fetchInitialRooms(): List<ApiRoom> = coroutineScope {
        ROOM_GENDERS.map { gender ->
            async(Dispatchers.IO) {
                runCatching { fetchGenderPage(gender, 0) }.getOrNull()
            }
        }.awaitAll().filterNotNull().flatten().deduplicate()
    }

    suspend fun fetchAllOnlineRooms(): List<ApiRoom> = coroutineScope {
        val firstPages = ROOM_GENDERS.map { gender ->
            async(Dispatchers.IO) {
                runCatching { fetchGenderPageWithCount(gender) }.getOrNull()
            }
        }.awaitAll().filterNotNull()

        firstPages.map { page ->
            async(Dispatchers.IO) { fetchRemainingPages(page.gender, page.totalCount, page.rooms) }
        }.awaitAll().flatten().deduplicate()
    }

    private fun fetchGenderPage(gender: String, page: Int): List<ApiRoom> {
        val offset = page * ROOM_PAGE_LIMIT
        val url = roomListUrl(gender, offset)
        return parseRooms(JSONObject(request(url)))
    }

    private data class GenderPage(
        val gender: String,
        val totalCount: Int,
        val rooms: List<ApiRoom>
    )

    private fun fetchGenderPageWithCount(gender: String): GenderPage {
        val url = roomListUrl(gender, 0)
        val root = JSONObject(request(url))
        return GenderPage(
            gender = gender,
            totalCount = root.optInt("total_count", 0),
            rooms = parseRooms(root)
        )
    }

    private suspend fun fetchRemainingPages(
        gender: String,
        totalCount: Int,
        firstPage: List<ApiRoom>
    ): List<ApiRoom> = coroutineScope {
        if (totalCount <= firstPage.size) return@coroutineScope firstPage

        val pageCount = (totalCount + ROOM_PAGE_LIMIT - 1) / ROOM_PAGE_LIMIT
        val remaining = (1 until pageCount).map { page ->
            async(Dispatchers.IO) { fetchGenderPage(gender, page) }
        }.awaitAll().flatten()

        firstPage + remaining
    }

    private fun roomListUrl(gender: String, offset: Int): URL =
        URL(
            BASE + "/api/ts/roomlist/room-list/" +
                "?enable_recommendations=true" +
                "&genders=" + encode(gender) +
                "&limit=" + ROOM_PAGE_LIMIT +
                "&offset=" + offset
        )

    private fun List<ApiRoom>.deduplicate(): List<ApiRoom> {
        val unique = LinkedHashMap<String, ApiRoom>()
        forEach { unique.putIfAbsent(it.username.lowercase(), it) }
        return unique.values.toList()
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
            // The room-list and playback endpoints are session-gated. Without a
            // session they answer with a 302 to the site's login page; do not hide
            // that behind a generic error by silently following the redirect.
            instanceFollowRedirects = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "ChaturbateClient/0.1 Android")
            setRequestProperty("Referer", BASE + "/")
        }

        try {
            val code = connection.responseCode
            val contentType = connection.contentType
            if (code in 300..399) {
                throw IllegalStateException(
                    "Chaturbate requires a signed-in session for this data (HTTP $code). " +
                        "No supported public discovery API is configured."
                )
            }
            if (code !in 200..299) {
                throw IllegalStateException("Chaturbate API returned HTTP $code")
            }
            if (contentType?.contains("json", ignoreCase = true) != true) {
                throw IllegalStateException(
                    "Expected JSON but received ${contentType ?: "an unknown content type"}."
                )
            }

            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun encode(value: String): String =
        java.net.URLEncoder.encode(value, Charsets.UTF_8.name())
}
