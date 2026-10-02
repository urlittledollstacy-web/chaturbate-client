package com.example.chaturbateclient.data

data class Room(
    val username: String,
    val viewers: Int,
    val category: String,
    val isLive: Boolean = true
)
