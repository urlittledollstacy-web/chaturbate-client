package com.example.chaturbateclient.data

import org.junit.Assert.assertEquals
import org.junit.Test

class RoomSearchTest {
    private val rooms = listOf(
        ApiRoom("Alice", 10, "Live", "", gender = "female", tags = listOf("gaming"), subject = "Gaming"),
        ApiRoom("Bob", 20, "Live", "", gender = "masculine", tags = listOf("music"), subject = "Music"),
        ApiRoom("CoupleX", 30, "Live", "", gender = "couple", tags = listOf("gaming"), subject = "Chat")
    )

    @Test fun searchesUsernameSubjectGenderAndTags() {
        assertEquals(listOf("Alice"), searchRooms(rooms, "alice").map { it.username })
        assertEquals(listOf("Alice", "CoupleX"), searchRooms(rooms, "gaming").map { it.username })
        assertEquals(listOf("Bob"), searchRooms(rooms, "masculine").map { it.username })
    }

    @Test fun blankQueryReturnsLoadedDataset() {
        assertEquals(rooms, searchRooms(rooms, " "))
    }
}
