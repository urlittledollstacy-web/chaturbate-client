package com.example.chaturbateclient.player

import com.example.chaturbateclient.data.ChaturbateApi

interface PlayerRepository {
    suspend fun resolve(username: String): Result<PlayerSource>
}

class ChaturbatePlayerRepository : PlayerRepository {
    override suspend fun resolve(username: String): Result<PlayerSource> = runCatching {
        val result = ChaturbateApi.fetchPlaybackSource(username)
        val hls = result.hlsUrl ?: throw IllegalStateException(
            "No playable HTTPS HLS source was returned for this room."
        )
        PlayerSource.Hls(hls, username)
    }
}
