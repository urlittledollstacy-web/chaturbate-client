package com.example.chaturbateclient.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkDiagnosticsTest {
    @Test fun redactsSensitiveQueryValues() {
        val value = sanitizeDiagnosticExcerpt("token=secret cookie=abc csrf=xyz")
        assertTrue(value!!.contains("token=[redacted]"))
        assertFalse(value.contains("secret"))
        assertFalse(value.contains("abc"))
        assertFalse(value.contains("xyz"))
    }

    @Test fun summaryOmitsUrlQuery() {
        val diagnostic = NetworkDiagnostic("room-list", "https://chaturbate.com/path", 400, "text/html", "https://chaturbate.com/?token=secret", "bad")
        val summary = diagnostic.safeSummary()
        assertFalse(summary.contains("token=secret"))
        assertTrue(summary.contains("HTTP"))
    }
}
