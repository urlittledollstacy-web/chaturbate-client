package com.example.chaturbateclient.player

interface PlayerRepository {
    suspend fun resolve(username: String): Result<PlayerSource>
}

/**
 * Temporary implementation while the supported Chaturbate viewer source is
 * being verified. It deliberately does not scrape or invent stream URLs.
 */
class ChaturbatePlayerRepository : PlayerRepository {
    override suspend fun resolve(username: String): Result<PlayerSource> =
        Result.failure(
            UnsupportedOperationException(
                "Viewer playback source is not configured yet."
            )
        )
}
