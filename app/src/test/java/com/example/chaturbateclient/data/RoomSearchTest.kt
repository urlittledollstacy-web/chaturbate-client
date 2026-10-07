package com.example.chaturbateclient.data

import org.junit.Assert.assertEquals
import org.junit.Test

class RoomSearchTest {
    private val rooms = listOf(
        ApiRoom("Alice", 10, "Gaming", "", gender = "f", location = "Berlin", country = "DE", tags = listOf("speedrun")),
        ApiRoom("Bob", 20, "Music", "", gender = "m", spokenLanguages = "English", tags = listOf("guitar")),
        ApiRoom("Carol", 30, "Chat", "", country = "FR", tags = emptyList())
    )

    @Test
    fun blankQueryReturnsWholeCatalogue() {
        assertEquals(rooms, filterRooms(rooms, "   "))
    }

    @Test
    fun matchesCaseInsensitiveUsername() {
        assertEquals(listOf("Alice"), filterRooms(rooms, "aLiCe").map { it.username })
    }

    @Test
    fun matchesTagsCategoryCountryAndLanguages() {
        assertEquals(listOf("Alice"), filterRooms(rooms, "speedrun").map { it.username })
        assertEquals(listOf("Bob"), filterRooms(rooms, "music").map { it.username })
        assertEquals(listOf("Carol"), filterRooms(rooms, "fr").map { it.username })
        assertEquals(listOf("Bob"), filterRooms(rooms, "english").map { it.username })
    }

    @Test
    fun noMatchReturnsEmptyNotEverything() {
        assertEquals(emptyList<ApiRoom>(), filterRooms(rooms, "zzz"))
    }
}
