package com.example.chaturbateclient.data

/**
 * Search/discovery scope exposed by the app.
 *
 * The current upstream contract supports best-effort live-room feeds only.
 * It does not establish a verified global profile/offline-account search API.
 */
enum class DiscoveryScope(
    val label: String,
    val description: String
) {
    LOADED_LIVE_ROOMS(
        label = "Loaded live rooms",
        description = "Search is limited to live-room records currently loaded into the local catalogue."
    )
}

fun DiscoveryState.scope(): DiscoveryScope = DiscoveryScope.LOADED_LIVE_ROOMS
