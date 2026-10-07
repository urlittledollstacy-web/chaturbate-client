package com.example.chaturbateclient.data

/**
 * Resolves a room image URL for display, or null when there is nothing usable.
 *
 * The room feed can hand back protocol-relative URLs ("//cdn...") or cleartext
 * ("http://..."). Android blocks cleartext traffic by default, so http is
 * upgraded to https to give the image a chance; protocol-relative URLs get the
 * https scheme prefixed. Anything that cannot become an https URL returns null
 * so the UI can show an explicit placeholder instead of a silent grey box.
 */
fun normalizedImageUrl(raw: String?): String? {
    val url = raw?.trim().orEmpty()
    return when {
        url.isEmpty() -> null
        url.startsWith("https://") -> url
        url.startsWith("//") -> "https:$url"
        url.startsWith("http://") -> "https://" + url.removePrefix("http://")
        else -> null
    }
}

/** Counts the shape of the raw image URLs in a loaded catalogue, for diagnostics. */
data class ImageUrlStats(
    val total: Int,
    val blank: Int,
    val https: Int,
    val cleartextHttp: Int,
    val protocolRelative: Int,
    val other: Int
) {
    val usable: Int get() = https + cleartextHttp + protocolRelative
}

fun summarizeImageUrls(rooms: List<ApiRoom>): ImageUrlStats {
    var blank = 0
    var https = 0
    var http = 0
    var protocolRelative = 0
    var other = 0
    rooms.forEach { room ->
        val url = room.imageUrl.trim()
        when {
            url.isEmpty() -> blank++
            url.startsWith("https://") -> https++
            url.startsWith("http://") -> http++
            url.startsWith("//") -> protocolRelative++
            else -> other++
        }
    }
    return ImageUrlStats(rooms.size, blank, https, http, protocolRelative, other)
}
