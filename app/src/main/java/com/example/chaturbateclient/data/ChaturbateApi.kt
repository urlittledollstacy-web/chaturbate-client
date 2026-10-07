package com.example.chaturbateclient.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CancellationException
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
    val tags: List<String> = emptyList(),
    val subject: String = ""
)

data class PlaybackSource(val roomStatus: String, val hlsUrl: String?)

object ChaturbateApi {
    private const val BASE = "https://chaturbate.com"
    private const val ROOM_PAGE_LIMIT = 90

    // Refresh fetches a bounded slice of each category instead of paging through
    // the whole catalogue (thousands of rooms => hundreds of requests per refresh).
    // 3 pages per category is ~270 rooms each, ~1,080 total, in ~12 requests.
    private const val MAX_PAGES_PER_GENDER = 3

    // Test seam: points the client at a local server so request handling
    // (redirects, content-type, pagination) can be exercised end to end.
    internal var baseOverride: String? = null

    private fun base(): String = baseOverride ?: BASE

    // The four gender/category feeds exposed by the current Chaturbate room list.
    val DEFAULT_GENDERS = listOf("f", "m", "c", "t")

    suspend fun fetchInitialRooms(genders: List<String> = DEFAULT_GENDERS): List<ApiRoom> = coroutineScope {
        val attempts = genders.map { gender ->
            async(Dispatchers.IO) { runCatching { fetchGenderPage(gender, 0) } }
        }.awaitAll()
        val rooms = attempts.mapNotNull { it.getOrNull() }.flatten().deduplicate()
        if (rooms.isEmpty()) {
            // Surface the real reason (e.g. session required) instead of hiding
            // every failure behind an empty catalogue.
            attempts.firstNotNullOfOrNull { it.exceptionOrNull() }?.let { throw it }
        }
        rooms
    }

    suspend fun fetchAllOnlineRooms(genders: List<String> = DEFAULT_GENDERS): List<ApiRoom> = coroutineScope {
        val firstAttempts = genders.map { gender ->
            async(Dispatchers.IO) { runCatching { fetchGenderPageWithCount(gender) } }
        }.awaitAll()
        val firstPages = firstAttempts.mapNotNull { it.getOrNull() }

        if (firstPages.isEmpty()) {
            firstAttempts.firstNotNullOfOrNull { it.exceptionOrNull() }?.let { throw it }
            return@coroutineScope emptyList()
        }

        // A failed gender feed drops out; the feeds that succeeded still return.
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

        // Cap the fan-out: never request more than MAX_PAGES_PER_GENDER pages.
        val pageCount = minOf(
            (totalCount + ROOM_PAGE_LIMIT - 1) / ROOM_PAGE_LIMIT,
            MAX_PAGES_PER_GENDER
        )
        // Each later page fails on its own: a single transient error must not cancel
        // its siblings and discard pages that already loaded. Cancellation still
        // propagates so leaving the screen stops the remaining requests.
        val remaining = (1 until pageCount).map { page ->
            async(Dispatchers.IO) {
                runCatching { fetchGenderPage(gender, page) }
                    .onFailure { if (it is CancellationException) throw it }
            }
        }.awaitAll().mapNotNull { it.getOrNull() }.flatten()

        firstPage + remaining
    }

    private fun roomListUrl(gender: String, offset: Int): URL =
        URL(
            base() + "/api/ts/roomlist/room-list/" +
                "?enable_recommendations=true" +
                "&genders=" + encode(gender) +
                "&limit=" + ROOM_PAGE_LIMIT +
                "&offset=" + offset
        )

    internal fun List<ApiRoom>.deduplicate(): List<ApiRoom> {
        val unique = LinkedHashMap<String, ApiRoom>()
        forEach { unique.putIfAbsent(it.username.lowercase(), it) }
        return unique.values.toList()
    }

    fun fetchPlaybackSource(username: String): PlaybackSource {
        val clean = username.trim().lowercase()
        require(clean.isNotEmpty()) { "Username is empty." }
        val url = URL(base() + "/api/chatvideocontext/" + encode(clean) + "/")
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
                        ).coerceAtLeast(0),
                        category = gender.ifBlank { "Live" },
                        subject = subject,
                        imageUrl = firstNonBlank(
                            item.optString("img"),
                            item.optString("image_url"),
                            item.optString("image_url_360x270"),
                            item.optString("thumb_url")
                        ),
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
            // The room-list endpoint only answers JSON when asked as an AJAX call.
            // Without X-Requested-With it replies 302 to /?next=... and then HTML.
            instanceFollowRedirects = false
            setRequestProperty("Accept", "application/json, text/plain, */*")
            setRequestProperty("User-Agent", "ChaturbateClient/0.1 Android")
            setRequestProperty("X-Requested-With", "XMLHttpRequest")
            setRequestProperty("Referer", base() + "/")
        }

        try {
            val code = connection.responseCode
            val contentType = connection.contentType
            if (code in 300..399) {
                throw IllegalStateException(
                    "Chaturbate returned a redirect (HTTP $code) instead of JSON. " +
                        "The room-list endpoint expects an AJAX request " +
                        "(X-Requested-With: XMLHttpRequest)."
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

    private fun firstNonBlank(vararg values: String): String =
        values.firstOrNull { it.isNotBlank() }?.trim().orEmpty()

    private fun encode(value: String): String =
        java.net.URLEncoder.encode(value, Charsets.UTF_8.name())
}
