package com.example.chaturbateclient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chaturbateclient.data.ApiRoom
import com.example.chaturbateclient.data.ChaturbateApi
import com.example.chaturbateclient.data.RoomCacheStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Everything the Home feed needs to render, owned outside the composable. */
data class HomeUiState(
    val rooms: List<ApiRoom> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

/**
 * Owns catalogue loading so it survives screen rebuilds: returning to Home or
 * rotating the device shows the loaded rooms again instead of fetching from
 * scratch. The fetch is injected so the ViewModel can be tested without a network.
 */
class HomeViewModel(
    private val cacheStore: RoomCacheStore,
    private val fetchRooms: suspend () -> List<ApiRoom> = { ChaturbateApi.fetchAllOnlineRooms() },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            // Cache reads/writes stay off the main thread.
            val cached = withContext(ioDispatcher) { cacheStore.load() }
            if (cached.isNotEmpty()) _uiState.value = _uiState.value.copy(rooms = cached)
            // Always flag loading so a refresh (not just the first load) shows its indicator.
            _uiState.value = _uiState.value.copy(loading = true, error = null)

            // Best-effort refresh; never let a smaller/empty result replace a larger good cache.
            runCatching {
                withContext(ioDispatcher) { fetchRooms() }
            }.onSuccess { fresh ->
                if (fresh.isNotEmpty()) {
                    withContext(ioDispatcher) { cacheStore.save(fresh) }
                    _uiState.value = _uiState.value.copy(rooms = fresh, loading = false)
                } else if (_uiState.value.rooms.isEmpty()) {
                    _uiState.value = _uiState.value.copy(loading = false, error = "No live rooms returned.")
                } else {
                    _uiState.value = _uiState.value.copy(loading = false)
                }
            }.onFailure {
                if (it is CancellationException) throw it
                val message = it.message ?: "Unable to load live rooms."
                _uiState.value = if (_uiState.value.rooms.isEmpty()) {
                    _uiState.value.copy(loading = false, error = message)
                } else {
                    _uiState.value.copy(loading = false)
                }
            }
        }
    }
}
