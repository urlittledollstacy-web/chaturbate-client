package com.example.chaturbateclient.data

sealed interface ProfileLookupResult {
    data object Unsupported : ProfileLookupResult
}

interface ProfileLookupRepository {
    suspend fun lookup(username: String): ProfileLookupResult
}

/**
 * No verified public viewer/profile search contract was established.
 * Keeping this explicit prevents local live-room search from being mistaken for global profile lookup.
 */
class UnsupportedProfileLookupRepository : ProfileLookupRepository {
    override suspend fun lookup(username: String): ProfileLookupResult =
        ProfileLookupResult.Unsupported
}
