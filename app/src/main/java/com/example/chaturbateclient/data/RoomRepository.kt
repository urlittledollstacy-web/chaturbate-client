package com.example.chaturbateclient.data

/**
 * App-facing discovery contract.
 *
 * Keep Chaturbate credentials and affiliate configuration outside the Android
 * client. A future server-side adapter can implement this interface.
 */
interface RoomRepository {
    suspend fun discover(): List<Room>
    suspend fun search(query: String): List<Room>
}

class DemoRoomRepository : RoomRepository {
    private val rooms = listOf(
        Room("Room preview", 0, "API pending"),
        Room("Room preview 2", 0, "API pending"),
        Room("Room preview 3", 0, "API pending")
    )

    override suspend fun discover(): List<Room> = rooms

    override suspend fun search(query: String): List<Room> =
        if (query.isBlank()) rooms
        else rooms.filter { it.username.contains(query, ignoreCase = true) }
}
