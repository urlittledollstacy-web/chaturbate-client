package com.example.chaturbateclient.data

import com.example.chaturbateclient.data.ChaturbateApi.deduplicate
import org.junit.Assert.assertEquals
import org.junit.Test

class ChaturbateApiTest {
    @Test
    fun deduplicateKeepsFirstOccurrenceCaseInsensitively() {
        val rooms = listOf(
            ApiRoom("Alice", 10, "f", ""),
            ApiRoom("alice", 99, "f", ""),
            ApiRoom("Bob", 5, "m", "")
        )
        val result = rooms.deduplicate()
        assertEquals(listOf("Alice", "Bob"), result.map { it.username })
        assertEquals(10, result.first().viewers)
    }
}
