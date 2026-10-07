package com.example.chaturbateclient.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomCacheCodecTest {
    private val room = ApiRoom(
        username = "Alice",
        viewers = 42,
        category = "Gaming",
        imageUrl = "https://example.test/alice.jpg",
        gender = "f",
        location = "Berlin",
        country = "DE",
        spokenLanguages = "English,German",
        tags = listOf("gaming", "music")
    )

    @Test
    fun roundTripsRoomFields() {
        val decoded = RoomCacheCodec.decode(RoomCacheCodec.encode(listOf(room)))
        assertEquals(1, decoded.size)
        assertEquals(room, decoded.first())
    }

    @Test
    fun rejectsNullOrBlank() {
        assertEquals(emptyList<ApiRoom>(), RoomCacheCodec.decode(null))
        assertEquals(emptyList<ApiRoom>(), RoomCacheCodec.decode(""))
    }

    @Test
    fun rejectsMalformedJson() {
        assertEquals(emptyList<ApiRoom>(), RoomCacheCodec.decode("{not json"))
    }

    @Test
    fun rejectsUnknownSchemaVersion() {
        val stale = """{"schemaVersion":1,"rooms":[{"username":"Bob"}]}"""
        assertEquals(emptyList<ApiRoom>(), RoomCacheCodec.decode(stale))
    }

    @Test
    fun skipsEntriesWithoutUsername() {
        val payload = """{"schemaVersion":$ROOM_CACHE_SCHEMA_VERSION,"rooms":[{"viewers":1}]}"""
        assertTrue(RoomCacheCodec.decode(payload).isEmpty())
    }
}
