package com.example.chaturbateclient.data

fun searchRooms(rooms: List<ApiRoom>, query: String): List<ApiRoom> {
    val q = query.trim()
    if (q.isBlank()) return rooms
    return rooms.filter { room ->
        room.username.contains(q, true) ||
            room.subject.contains(q, true) ||
            room.gender.contains(q, true) ||
            room.location.contains(q, true) ||
            room.country.contains(q, true) ||
            room.spokenLanguages.contains(q, true) ||
            room.tags.any { it.contains(q, true) }
    }
}
