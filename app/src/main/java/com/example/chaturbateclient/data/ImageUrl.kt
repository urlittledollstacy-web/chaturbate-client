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
