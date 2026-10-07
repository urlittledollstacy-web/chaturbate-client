package com.example.chaturbateclient.data

import java.net.URI

// Hosts Chaturbate serves live streams from: the site itself plus its streaming CDNs.
private val ALLOWED_PLAYBACK_HOSTS = listOf(
    "chaturbate.com",
    "highwebmedia.com",
    "mmcdn.com"
)

/**
 * Returns [raw] only when it is an https URL on a known Chaturbate streaming host.
 *
 * The room API hands back the playback address, so a tampered or unexpected response
 * could otherwise point the player at an untrusted origin. Only the scheme and host
 * are checked; the path and query carry the signed stream token and pass through
 * unchanged.
 */
fun allowedPlaybackUrl(raw: String?): String? {
    val value = raw?.trim().orEmpty()
    if (value.isEmpty()) return null

    val uri = runCatching { URI(value) }.getOrNull() ?: return null
    if (!uri.scheme.equals("https", ignoreCase = true)) return null

    val host = uri.host?.lowercase() ?: return null
    val allowed = ALLOWED_PLAYBACK_HOSTS.any { host == it || host.endsWith(".$it") }
    return value.takeIf { allowed }
}
