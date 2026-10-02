package com.example.chaturbateclient.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PaginationTest {
    @Test fun createsBoundedOffsets() {
        assertEquals(listOf(0, 90, 180), pageOffsets(181, 90, 10))
    }

    @Test fun capsPageCount() {
        assertEquals(listOf(0, 90), pageOffsets(1000, 90, 2))
    }

    @Test fun emptyCatalogueHasNoPages() {
        assertEquals(emptyList<Int>(), pageOffsets(0, 90, 10))
    }
}
