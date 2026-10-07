package com.example.chaturbateclient.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackUrlTest {

    @Test
    fun acceptsHttpsOnKnownStreamHosts() {
        val urls = listOf(
            "https://chaturbate.com/hls/stream.m3u8",
            "https://edge5.stream.highwebmedia.com/live/abc.m3u8",
            "https://b-hls-12.mmcdn.com/hls/xyz.m3u8"
        )
        urls.forEach { assertEquals(it, allowedPlaybackUrl(it)) }
    }

    @Test
    fun rejectsCleartextHttp() {
        assertNull(allowedPlaybackUrl("http://edge5.stream.highwebmedia.com/live/abc.m3u8"))
    }

    @Test
    fun rejectsUnknownHosts() {
        assertNull(allowedPlaybackUrl("https://evil.example.com/stream.m3u8"))
        // A look-alike suffix must not pass: nothighwebmedia.com is a different host.
        assertNull(allowedPlaybackUrl("https://nothighwebmedia.com/stream.m3u8"))
    }

    @Test
    fun rejectsUnparseableOrEmptyValues() {
        assertNull(allowedPlaybackUrl(null))
        assertNull(allowedPlaybackUrl("   "))
        assertNull(allowedPlaybackUrl("not a url"))
    }

    @Test
    fun keepsSignedQueryTokenIntact() {
        val url = "https://edge5.stream.highwebmedia.com/live/abc.m3u8?token=abc123&expires=99"
        assertEquals(url, allowedPlaybackUrl(url))
    }
}
