package com.example.chaturbateclient.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImageUrlTest {
    @Test
    fun keepsHttpsUnchanged() {
        assertEquals("https://cdn.test/a.jpg", normalizedImageUrl("https://cdn.test/a.jpg"))
    }

    @Test
    fun upgradesCleartextToHttps() {
        assertEquals("https://cdn.test/a.jpg", normalizedImageUrl("http://cdn.test/a.jpg"))
    }

    @Test
    fun prefixesProtocolRelative() {
        assertEquals("https://cdn.test/a.jpg", normalizedImageUrl("//cdn.test/a.jpg"))
    }

    @Test
    fun trimsWhitespace() {
        assertEquals("https://cdn.test/a.jpg", normalizedImageUrl("  https://cdn.test/a.jpg  "))
    }

    @Test
    fun rejectsNullBlankAndJunk() {
        assertNull(normalizedImageUrl(null))
        assertNull(normalizedImageUrl(""))
        assertNull(normalizedImageUrl("   "))
        assertNull(normalizedImageUrl("cdn.test/a.jpg"))
        assertNull(normalizedImageUrl("ftp://cdn.test/a.jpg"))
    }

    @Test
    fun summarizesUrlShapes() {
        val rooms = listOf(
            room("a", "https://cdn.test/a.jpg"),
            room("b", "http://cdn.test/b.jpg"),
            room("c", "//cdn.test/c.jpg"),
            room("d", ""),
            room("e", "not-a-url")
        )
        val stats = summarizeImageUrls(rooms)
        assertEquals(5, stats.total)
        assertEquals(1, stats.https)
        assertEquals(1, stats.cleartextHttp)
        assertEquals(1, stats.protocolRelative)
        assertEquals(1, stats.blank)
        assertEquals(1, stats.other)
        assertEquals(3, stats.usable)
    }

    private fun room(name: String, imageUrl: String) = ApiRoom(
        username = name,
        viewers = 0,
        category = "Live",
        imageUrl = imageUrl
    )
}
