package com.example.chaturbateclient.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DiscoveryViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = AppPreferences(application)
    private val repository = RoomRepository(application)

    private val _state = MutableStateFlow(DiscoveryState())
    val state: StateFlow<DiscoveryState> = _state.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _searchResults = MutableStateFlow<List<ApiRoom>>(emptyList())
    val searchResults: StateFlow<List<ApiRoom>> = _searchResults.asStateFlow()

    private var refreshJob: Job? = null
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            repository.migrateLegacyCache(preferences)
            val cached = repository.cachedState()
            _state.value = cached
            recomputeSearch()
            if (cached.rooms.isEmpty()) refresh()
            else if (cached.metadata?.complete != true) completeRefresh()
        }
    }

    fun setQuery(value: String) {
        _query.value = value
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(250)
            recomputeSearch()
        }
    }

    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            val previous = _state.value
            _state.value = previous.copy(refreshing = true, error = null)
            val initial = runCatching { repository.refreshInitial() }.getOrElse {
                previous.copy(error = it.message ?: "Unable to refresh live rooms.")
            }
            _state.value = initial.copy(refreshing = true)
            recomputeSearch()
            completeRefresh()
        }
    }

    private fun completeRefresh() {
        if (refreshJob?.isActive == true && refreshJob != coroutineContext[Job]) return
        viewModelScope.launch {
            val result = runCatching { repository.refreshComplete() }.getOrElse {
                _state.value.copy(refreshing = false, error = it.message ?: "Catalogue refresh failed.")
            }
            _state.value = result.copy(refreshing = false)
            recomputeSearch()
        }
    }

    private suspend fun recomputeSearch() {
        val rooms = _state.value.rooms
        val q = _query.value
        _searchResults.value = withContext(Dispatchers.Default) { searchRooms(rooms, q) }
    }

    fun latestDiagnostic(): NetworkDiagnostic? = _state.value.lastDiagnostic
}
