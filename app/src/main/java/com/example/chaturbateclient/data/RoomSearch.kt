package com.example.chaturbateclient.data

/**
 * Local search over the currently loaded live-room catalogue.
 *
 * This is deliberately not a global/profile search: it can only match rooms that
 * are already loaded. A blank query returns the loaded catalogue unchanged.
 */
fun filterRooms(rooms: List<ApiRoom>, query: String): List<ApiRoom> {
    val q = query.trim()
    if (q.isBlank()) return rooms
    return rooms.filter { room ->
        room.username.contains(q, ignoreCase = true) ||
            room.category.contains(q, ignoreCase = true) ||
            room.subject.contains(q, ignoreCase = true) ||
            room.gender.contains(q, ignoreCase = true) ||
            room.location.contains(q, ignoreCase = true) ||
            room.country.contains(q, ignoreCase = true) ||
            room.spokenLanguages.contains(q, ignoreCase = true) ||
            room.tags.any { it.contains(q, ignoreCase = true) }
    }
}
