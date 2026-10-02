package com.example.chaturbateclient.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DiscoveryState(
    val rooms: List<ApiRoom> = emptyList(),
    val metadata: CatalogMetadata? = null,
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val error: String? = null,
    val lastDiagnostic: NetworkDiagnostic? = null
) {
    val isPartial: Boolean get() = metadata?.complete == false
}

class RoomRepository(context: Context) {
    private val database = RoomCatalogDatabase(context.applicationContext)

    suspend fun cachedState(): DiscoveryState = withContext(Dispatchers.IO) {
        DiscoveryState(database.readRooms(), database.readMetadata())
    }

    suspend fun migrateLegacyCache(preferences: AppPreferences) = withContext(Dispatchers.IO) {
        if (database.readRooms().isNotEmpty()) return@withContext
        val legacy = preferences.loadLegacyRoomCache()
        if (legacy.isNotEmpty()) {
            database.replaceSnapshot(
                legacy,
                CatalogMetadata(
                    fetchedAtMs = System.currentTimeMillis(),
                    complete = false,
                    successfulFeeds = 0,
                    totalFeeds = 0,
                    successfulPages = 0,
                    failedPages = 0,
                    diagnostics = "Migrated legacy cache; coverage is unknown."
                )
            )
            preferences.clearLegacyRoomCache()
        }
    }

    suspend fun refreshInitial(): DiscoveryState = withContext(Dispatchers.IO) {
        val result = ChaturbateApi.fetchInitialCatalog()
        val old = database.readRooms()
        if (result.rooms.isNotEmpty()) {
            database.replaceSnapshot(result.rooms, result.metadata)
        }
        DiscoveryState(
            rooms = if (result.rooms.isNotEmpty()) result.rooms else old,
            metadata = if (result.rooms.isNotEmpty()) result.metadata else database.readMetadata(),
            error = result.error,
            lastDiagnostic = result.lastDiagnostic
        )
    }

    suspend fun refreshComplete(): DiscoveryState = withContext(Dispatchers.IO) {
        val result = ChaturbateApi.fetchCompleteCatalog()
        val old = database.readRooms()
        // Never replace a known-good catalogue with an unverified partial snapshot.
        if (result.rooms.isNotEmpty() && result.metadata.complete) {
            database.replaceSnapshot(result.rooms, result.metadata)
        }
        val cached = database.readRooms()
        DiscoveryState(
            rooms = if (cached.isNotEmpty()) cached else old,
            metadata = database.readMetadata(),
            error = result.error,
            lastDiagnostic = result.lastDiagnostic
        )
    }
}
