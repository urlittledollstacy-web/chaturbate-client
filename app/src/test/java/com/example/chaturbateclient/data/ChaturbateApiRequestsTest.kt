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

        assertEquals(listOf("c", "f", "m", "t"), requestedGenders.sorted())
        assertEquals(
            setOf("room_c", "room_f", "room_m", "room_t", "shared"),
            result.map { it.username }.toSet()
        )
        assertEquals(result.size, result.map { it.username.lowercase() }.distinct().size)
    }

    @Test
    fun parsesImgFieldUsedByCurrentFeed() = runBlocking {
        enqueue {
            json(
                """{"total_count":1,"rooms":[{"username":"solo","num_users":9,"gender":"f","img":"https://thumb.test/solo.jpg"}]}"""
            )
        }

        val result = ChaturbateApi.fetchInitialRooms(listOf("f"))

        assertEquals("https://thumb.test/solo.jpg", result.single().imageUrl)
    }

    @Test
    fun sendsAjaxHeaderRequiredForJson() = runBlocking {
        val seen = CopyOnWriteArrayList<String>()
        enqueue { request ->
            seen.add(request.getHeader("X-Requested-With").orEmpty())
            json(roomsJson(listOf("r")))
        }

        ChaturbateApi.fetchInitialRooms(listOf("f"))

        assertTrue(seen.isNotEmpty())
        assertTrue(seen.all { it == "XMLHttpRequest" })
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
    fun refreshCapsPagingAtThreePagesPerGender() = runBlocking {
        enqueue { request ->
            val gender = request.requestUrl!!.queryParameter("genders").orEmpty()
            val offset = request.requestUrl!!.queryParameter("offset")!!.toInt()
            requestedOffsets.add(offset)
            // A huge catalogue: without the cap this would page through ~80 offsets.
            json(roomsJson(List(90) { "${gender}u$offset-$it" }, totalCount = 7000))
        }

        val result = ChaturbateApi.fetchAllOnlineRooms()

        // 4 genders x 3 pages = 12 requests, offsets 0/90/180 only.
        assertEquals(listOf(0, 90, 180), requestedOffsets.distinct().sorted())
        assertEquals(12, requestedOffsets.size)
        assertEquals(4 * 3 * 90, result.size)
    }

    @Test
    fun initialRoomsDropsFailedGenderAndKeepsOthers() = runBlocking {
        enqueue { request ->
            val gender = request.requestUrl!!.queryParameter("genders").orEmpty()
            if (gender == "m") {
                MockResponse().setResponseCode(500)
            } else {
                json(roomsJson(listOf("room_$gender")))
            }
        }

        val result = ChaturbateApi.fetchInitialRooms()

        // The failing "m" feed drops out; the other three still load.
        assertEquals(setOf("room_c", "room_f", "room_t"), result.map { it.username }.toSet())
    }

    @Test
    fun allOnlineRoomsKeepsSuccessfulFeedsWhenOneFails() = runBlocking {
        enqueue { request ->
            val gender = request.requestUrl!!.queryParameter("genders").orEmpty()
            if (gender == "c") {
                MockResponse().setResponseCode(500)
            } else {
                json(roomsJson(listOf("${gender}1", "${gender}2")))
            }
        }

        val result = ChaturbateApi.fetchAllOnlineRooms()

        assertEquals(
            setOf("f1", "f2", "m1", "m2", "t1", "t2"),
            result.map { it.username }.toSet()
        )
    }

    @Test
    fun customGenderListIsHonoured() = runBlocking {
        enqueue { json(roomsJson(listOf("only"))) }

        val result = ChaturbateApi.fetchInitialRooms(listOf("f"))

        assertEquals(listOf("only"), result.map { it.username })
        assertEquals("f", server.takeRequest().requestUrl!!.queryParameter("genders"))
    }

    @Test
    fun redirectIsReportedAsMisconfiguredRequest() = runBlocking {
        enqueue {
            MockResponse()
                .setResponseCode(302)
                .setHeader("Location", "/?next=/api/ts/roomlist/room-list/")
        }

        try {
            ChaturbateApi.fetchInitialRooms()
            fail("Expected redirect failure")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("AJAX"))
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
