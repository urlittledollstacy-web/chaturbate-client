package com.example.chaturbateclient.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.min

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
    val subject: String = category,
    val observedAtMs: Long = System.currentTimeMillis()
)

data class PlaybackSource(val roomStatus: String, val hlsUrl: String?)

data class CatalogFetchResult(
    val rooms: List<ApiRoom>,
    val metadata: CatalogMetadata,
    val error: String? = null,
    val lastDiagnostic: NetworkDiagnostic? = null
)

private data class FeedPage(
    val gender: String,
    val offset: Int,
    val totalCount: Int?,
    val rooms: List<ApiRoom>
)

object ChaturbateApi {
    private const val BASE = "https://chaturbate.com"
    private const val ROOM_PAGE_LIMIT = 90
    private const val MAX_PAGES_PER_FEED = 300
    private const val MAX_TOTAL_ROOMS = 20_000
    private const val MAX_ERROR_BODY = 1_200
    private const val CONNECT_TIMEOUT_MS = 12_000
    private const val READ_TIMEOUT_MS = 12_000
    private const val MAX_RETRIES = 2
    private const val MAX_CONCURRENT_REQUESTS = 3
    private val ROOM_GENDERS = listOf("f", "m", "c", "s")

    @Volatile
    private var lastDiagnostic: NetworkDiagnostic? = null

    fun latestDiagnostic(): NetworkDiagnostic? = lastDiagnostic

    suspend fun fetchInitialCatalog(): CatalogFetchResult = withContext(Dispatchers.IO) {
        val results = fetchFirstPages()
        val rooms = results.flatMap { it.rooms }.deduplicate().take(MAX_TOTAL_ROOMS)
        val successful = results.count { it.totalCount != null }
        val failed = ROOM_GENDERS.size - successful
        val diagnostic = lastDiagnostic
        CatalogFetchResult(
            rooms = rooms,
            metadata = CatalogMetadata(
                fetchedAtMs = System.currentTimeMillis(),
                complete = false,
                successfulFeeds = successful,
                totalFeeds = ROOM_GENDERS.size,
                successfulPages = results.size,
                failedPages = failed,
                diagnostics = diagnostic?.safeSummary()
            ),
            error = if (failed > 0) "Some discovery feeds failed; showing the successful feeds." else null,
            lastDiagnostic = diagnostic
        )
    }

    suspend fun fetchCompleteCatalog(): CatalogFetchResult = withContext(Dispatchers.IO) {
        val results = mutableListOf<FeedPage>()
        var failures = 0

        for (gender in ROOM_GENDERS) {
            ensureActive()
            val first = try {
                fetchPage(gender, 0)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                failures++
                null
            } ?: continue

            results += first
            val total = first.totalCount ?: run {
                failures++
                continue
            }
            val pageCount = min(
                MAX_PAGES_PER_FEED,
                ((total + ROOM_PAGE_LIMIT - 1) / ROOM_PAGE_LIMIT).coerceAtLeast(1)
            )
            if (pageCount <= 1) continue

            for (batch in (1 until pageCount).chunked(MAX_CONCURRENT_REQUESTS)) {
                ensureActive()
                val batchResults = coroutineScope {
                    batch.map { page ->
                        async(Dispatchers.IO) {
                            runCatching { fetchPage(gender, page) }.getOrNull()
                        }
                    }.awaitAll()
                }
                batchResults.forEach { page ->
                    if (page == null) failures++ else results += page
                }
                delay(120)
            }
        }

        val rooms = results.flatMap { it.rooms }.deduplicate().take(MAX_TOTAL_ROOMS)
        val successfulFeeds = results.map { it.gender }.toSet().size
        val complete = failures == 0 && successfulFeeds == ROOM_GENDERS.size
        val diagnostic = lastDiagnostic

        CatalogFetchResult(
            rooms = rooms,
            metadata = CatalogMetadata(
                fetchedAtMs = System.currentTimeMillis(),
                complete = complete,
                successfulFeeds = successfulFeeds,
                totalFeeds = ROOM_GENDERS.size,
                successfulPages = results.size,
                failedPages = failures,
                diagnostics = diagnostic?.safeSummary()
            ),
            error = if (complete) null else "Catalogue coverage is partial; some feeds or pages failed.",
            lastDiagnostic = diagnostic
        )
    }

    private suspend fun fetchFirstPages(): List<FeedPage> = coroutineScope {
        ROOM_GENDERS.map { gender ->
            async(Dispatchers.IO) {
                runCatching { fetchPage(gender, 0) }.getOrNull()
            }
        }.awaitAll().filterNotNull()
    }

    private suspend fun fetchPage(gender: String, page: Int): FeedPage {
        val offset = page * ROOM_PAGE_LIMIT
        val root = JSONObject(requestJson(roomListUrl(gender, offset), "room-list"))
        val rooms = parseRooms(root)
        val total = if (root.has("total_count")) root.optInt("total_count", -1).takeIf { it >= 0 } else null
        return FeedPage(gender, offset, total, rooms)
    }

    suspend fun fetchPlaybackSource(username: String): PlaybackSource = withContext(Dispatchers.IO) {
        val clean = username.trim().lowercase()
        require(clean.matches(Regex("[a-z0-9_]{1,40}"))) { "Invalid room username." }
        val root = JSONObject(requestJson(URL("$BASE/api/chatvideocontext/$clean/"), "playback-source"))
        PlaybackSource(
            roomStatus = root.optString("room_status", "unknown"),
            hlsUrl = root.optString("hls_source").takeIf { it.isNotBlank() && isAllowedUri(it) }
        )
    }

    private fun requestJson(url: URL, operation: String): String {
        var last: Throwable? = null
        repeat(MAX_RETRIES + 1) { attempt ->
            try {
                return requestJsonOnce(url, operation)
            } catch (e: CancellationException) {
                throw e
            } catch (e: ChaturbateNetworkException) {
                last = e
                if (e.diagnostic.status == 400 || e.diagnostic.status in 401..499) throw e
            } catch (e: Exception) {
                last = e
            }
            if (attempt < MAX_RETRIES) Thread.sleep(250L * (attempt + 1))
        }
        throw last ?: IllegalStateException("Network request failed.")
    }

    private fun requestJsonOnce(url: URL, operation: String): String {
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "ChaturbateClient/0.1 Android")
            setRequestProperty("Referer", "$BASE/")
        }
        try {
            val code = connection.responseCode
            val contentType = connection.contentType
            val location = connection.getHeaderField("Location")
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText().take(MAX_ERROR_BODY) }.orEmpty()
            if (code in 300..399) throw failure(operation, url, code, contentType, location, body, "Server redirected the request.")
            if (code !in 200..299) throw failure(operation, url, code, contentType, location, body, "Chaturbate returned HTTP $code.")
            if (contentType?.contains("json", ignoreCase = true) != true) {
                throw failure(operation, url, code, contentType, location, body, "Expected JSON but received a different response.")
            }
            return body
        } finally {
            connection.disconnect()
        }
    }

    private fun failure(
        operation: String,
        url: URL,
        status: Int,
        contentType: String?,
        location: String?,
        body: String,
        message: String
    ): ChaturbateNetworkException {
        val diagnostic = NetworkDiagnostic(
            operation = operation,
            url = sanitizeUrl(url),
            status = status,
            contentType = contentType,
            location = location,
            excerpt = sanitizeDiagnosticExcerpt(body)
        )
        lastDiagnostic = diagnostic
        return ChaturbateNetworkException(message, diagnostic)
    }

    private fun parseRooms(root: JSONObject): List<ApiRoom> {
        val results = root.optJSONArray("rooms")
            ?: throw IllegalStateException("Room-list response has no rooms array.")
        return buildList {
            for (i in 0 until results.length()) {
                val item = results.optJSONObject(i) ?: continue
                val username = item.optString("username").trim()
                if (username.isEmpty()) continue
                val gender = item.optString("gender").trim()
                val subject = item.optString("room_subject").trim()
                add(ApiRoom(
                    username = username,
                    viewers = item.optInt("num_users", item.optInt("num_viewers", 0)).coerceAtLeast(0),
                    category = subject.ifBlank { "Live" },
                    subject = subject,
                    imageUrl = item.optString("image_url").trim(),
                    gender = gender,
                    location = item.optString("location").trim(),
                    country = item.optString("country").trim(),
                    spokenLanguages = item.optString("spoken_languages").trim(),
                    tags = parseTags(item),
                    observedAtMs = System.currentTimeMillis()
                ))
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

    private fun List<ApiRoom>.deduplicate(): List<ApiRoom> {
        val unique = LinkedHashMap<String, ApiRoom>()
        forEach { unique.putIfAbsent(it.username.lowercase(), it) }
        return unique.values.toList()
    }

    private fun roomListUrl(gender: String, offset: Int): URL =
        URL(
            BASE + "/api/ts/roomlist/room-list/?enable_recommendations=true" +
                "&genders=" + URLEncoder.encode(gender, Charsets.UTF_8.name()) +
                "&limit=" + ROOM_PAGE_LIMIT + "&offset=" + offset
        )

    private fun sanitizeUrl(url: URL): String =
        URL(url.protocol, url.host, url.port, url.path).toString()

    private fun isAllowedUri(value: String): Boolean =
        runCatching {
            val uri = java.net.URI(value)
            uri.scheme.equals("https", true) && !uri.host.isNullOrBlank()
        }.getOrDefault(false)
}
