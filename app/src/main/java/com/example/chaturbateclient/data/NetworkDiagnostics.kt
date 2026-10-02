package com.example.chaturbateclient.data

import java.net.URI

data class NetworkDiagnostic(
    val operation: String,
    val url: String,
    val status: Int?,
    val contentType: String?,
    val location: String?,
    val excerpt: String?,
    val timestampMs: Long = System.currentTimeMillis()
) {
    fun safeSummary(): String = buildString {
        append(operation)
        append(" — ")
        append(status?.let { "HTTP $it" } ?: "transport error")
        append("\n")
        append(url)
        contentType?.let { append("\nContent-Type: ").append(it) }
        location?.let { append("\nRedirect: ").append(sanitizeLocation(it)) }
        excerpt?.let { append("\nResponse: ").append(it) }
    }

    private fun sanitizeLocation(value: String): String =
        runCatching {
            val uri = URI(value)
            URI(uri.scheme, uri.authority, uri.path, null, null).toString()
        }.getOrElse { value.substringBefore('?').take(300) }
}

class ChaturbateNetworkException(
    message: String,
    val diagnostic: NetworkDiagnostic,
    cause: Throwable? = null
) : Exception(message, cause)

fun sanitizeDiagnosticExcerpt(value: String?): String? {
    if (value.isNullOrBlank()) return null
    return value
        .replace(Regex("(?i)(cookie|authorization|token|csrf|signature|expires)=?[^&\\s]+"), "$1=[redacted]")
        .replace(Regex("(?i)Bearer\\s+[^\\s]+"), "Bearer [redacted]")
        .take(1200)
}
