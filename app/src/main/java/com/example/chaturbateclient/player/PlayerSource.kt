package com.example.chaturbateclient.player

/**
 * Playback source abstraction.
 *
 * The discovery layer must never depend on a specific player implementation.
 * This lets us use an official embed first and move to Media3/HLS if a
 * supported direct viewer source is established.
 */
sealed interface PlayerSource {
    data class Hls(
        val url: String,
        val title: String? = null
    ) : PlayerSource

    data class Embed(
        val url: String,
        val title: String? = null
    ) : PlayerSource
}
