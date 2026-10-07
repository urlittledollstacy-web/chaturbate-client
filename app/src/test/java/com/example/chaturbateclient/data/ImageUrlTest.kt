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
}
