package com.example.chaturbateclient.data

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/**
 * Exercises the real HTTP handling in [ChaturbateApi] against a real socket
 * server: pagination around the page limit, redirect / non-JSON / error
 * detection, and playback parsing. Only the network endpoint is swapped out;
 * the production [java.net.HttpURLConnection] path runs unchanged.
 */
class ChaturbateApiRequestsTest {

    private lateinit var server: MockWebServer
    private val requestedGenders = CopyOnWriteArrayList<String>()
    private val requestedOffsets = CopyOnWriteArrayList<Int>()

    @Before
    fun startServer() {
        server = MockWebServer()
        server.start()
        ChaturbateApi.baseOverride = server.url("/").toString().trimEnd('/')
    }

    @After
    fun stopServer() {
        server.shutdown()
        ChaturbateApi.baseOverride = null
    }

    private fun enqueue(fn: (RecordedRequest) -> MockResponse) {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = fn(request)
        }
    }

    private fun roomsJson(usernames: List<String>, totalCount: Int = usernames.size): String {
        val rooms = usernames.joinToString(",") {
            """{"username":"$it","num_users":5,"gender":"f","room_subject":"Chat","image_url":"https://x/$it.jpg","tags":["a"]}"""
        }
        return """{"total_count":$totalCount,"rooms":[$rooms]}"""
    }

    private fun json(body: String) = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(body)

    @Test
    fun initialRoomsRequestsEveryGenderAndDeduplicates() = runBlocking {
        enqueue { request ->
            val gender = request.requestUrl!!.queryParameter("genders").orEmpty()
            requestedGenders.add(gender)
            // "shared" appears in every gender feed; dedup must keep one copy.
            json(roomsJson(listOf("room_$gender", "shared")))
        }

        val result = ChaturbateApi.fetchInitialRooms()

        assertEquals(listOf("c", "f", "m", "s"), requestedGenders.sorted())
        assertEquals(
            setOf("room_c", "room_f", "room_m", "room_s", "shared"),
            result.map { it.username }.toSet()
        )
        assertEquals(result.size, result.map { it.username.lowercase() }.distinct().size)
    }

    @Test
    fun allOnlineRoomsPagesThroughOffsets() = runBlocking {
        enqueue { request ->
            val offset = request.requestUrl!!.queryParameter("offset")!!.toInt()
            requestedOffsets.add(offset)
            val count = minOf(90, 200 - offset)
            json(roomsJson(List(count) { "u$offset-$it" }, totalCount = 200))
        }

        val result = ChaturbateApi.fetchAllOnlineRooms()

        // Four gender feeds are paged in parallel; each requests offsets 0/90/180.
        assertEquals(listOf(0, 90, 180), requestedOffsets.distinct().sorted())
        assertEquals(12, requestedOffsets.size)
        assertEquals(200, result.size)
    }

    @Test
    fun redirectIsReportedAsSessionRequired() = runBlocking {
        enqueue {
            MockResponse()
                .setResponseCode(302)
                .setHeader("Location", "/?next=/api/ts/roomlist/room-list/")
        }

        try {
            ChaturbateApi.fetchInitialRooms()
            fail("Expected session-required failure")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("signed-in session"))
            assertTrue(e.message!!.contains("302"))
        }
    }

    @Test
    fun nonJsonBodyIsRejected() = runBlocking {
        enqueue {
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/html")
                .setBody("<html>login</html>")
        }

        try {
            ChaturbateApi.fetchInitialRooms()
            fail("Expected content-type failure")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("Expected JSON"))
        }
    }

    @Test
    fun serverErrorIsReportedWithCode() = runBlocking {
        enqueue { MockResponse().setResponseCode(500) }

        try {
            ChaturbateApi.fetchInitialRooms()
            fail("Expected HTTP 500 failure")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("HTTP 500"))
        }
    }

    @Test
    fun playbackSourceParsesStatusAndHls() = runBlocking {
        enqueue { json("""{"room_status":"public","hls_source":"https://x/alice.m3u8"}""") }

        val source = ChaturbateApi.fetchPlaybackSource("  Alice ")
        assertEquals("public", source.roomStatus)
        assertEquals("https://x/alice.m3u8", source.hlsUrl)

        assertEquals("/api/chatvideocontext/alice/", server.takeRequest().path)
    }

    @Test
    fun playbackSourceOmitsBlankHlsUrl() = runBlocking {
        enqueue { json("""{"room_status":"offline"}""") }

        val source = ChaturbateApi.fetchPlaybackSource("bob")
        assertEquals("offline", source.roomStatus)
        assertNull(source.hlsUrl)
    }
}
