package com.example.chaturbateclient

import com.example.chaturbateclient.data.ApiRoom
import com.example.chaturbateclient.data.RoomCacheStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setMainDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    private class FakeCacheStore(private var rooms: List<ApiRoom> = emptyList()) : RoomCacheStore {
        var saved: List<ApiRoom>? = null
        override fun load(): List<ApiRoom> = rooms
        override fun save(rooms: List<ApiRoom>) {
            saved = rooms
            this.rooms = rooms
        }
    }

    private fun viewModel(
        cache: RoomCacheStore = FakeCacheStore(),
        fetch: suspend () -> List<ApiRoom>
    ) = HomeViewModel(cache, fetch, dispatcher)

    private fun room(name: String) = ApiRoom(username = name, viewers = 1, category = "f", imageUrl = "")

    @Test
    fun loadsAndCachesRoomsOnStart() = runTest(dispatcher) {
        val cache = FakeCacheStore()
        val model = viewModel(cache) { listOf(room("alice"), room("bob")) }

        advanceUntilIdle()

        val state = model.uiState.value
        assertEquals(listOf("alice", "bob"), state.rooms.map { it.username })
        assertFalse(state.loading)
        assertEquals(listOf("alice", "bob"), cache.saved?.map { it.username })
    }

    @Test
    fun showsCachedRoomsImmediatelyThenReplacesThem() = runTest(dispatcher) {
        val cache = FakeCacheStore(listOf(room("cached")))
        val model = viewModel(cache) { listOf(room("fresh")) }

        advanceUntilIdle()

        assertEquals(listOf("fresh"), model.uiState.value.rooms.map { it.username })
    }

    @Test
    fun aFailedRefreshKeepsExistingRoomsAndNoError() = runTest(dispatcher) {
        val cache = FakeCacheStore(listOf(room("cached")))
        val model = viewModel(cache) { throw IllegalStateException("boom") }

        advanceUntilIdle()

        val state = model.uiState.value
        assertEquals(listOf("cached"), state.rooms.map { it.username })
        assertFalse(state.loading)
        assertEquals(null, state.error)
    }

    @Test
    fun aFailedRefreshWithNoRoomsSurfacesTheError() = runTest(dispatcher) {
        val model = viewModel { throw IllegalStateException("boom") }

        advanceUntilIdle()

        val state = model.uiState.value
        assertTrue(state.rooms.isEmpty())
        assertEquals("boom", state.error)
    }

    @Test
    fun anEmptyRefreshDoesNotReplaceCachedRooms() = runTest(dispatcher) {
        val cache = FakeCacheStore(listOf(room("cached")))
        val model = viewModel(cache) { emptyList() }

        advanceUntilIdle()

        assertEquals(listOf("cached"), model.uiState.value.rooms.map { it.username })
    }
}
