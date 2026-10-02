package com.example.chaturbateclient.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DiscoveryScopeTest {
    @Test
    fun scopeIsExplicitlyLoadedLiveRooms() {
        assertEquals(DiscoveryScope.LOADED_LIVE_ROOMS, DiscoveryState().scope())
        assertEquals("Loaded live rooms", DiscoveryState().scope().label)
    }
}
