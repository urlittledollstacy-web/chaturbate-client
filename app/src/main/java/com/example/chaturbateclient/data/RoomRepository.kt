package com.example.chaturbateclient.data

interface RoomRepository {
    suspend fun discover(): List<Room>
}

class DemoRoomRepository : RoomRepository {
    override suspend fun discover(): List<Room> = listOf(
        Room("Live room", 0, "Waiting for API"),
        Room("Featured room", 0, "Waiting for API"),
        Room("Popular room", 0, "Waiting for API")
    )
}
