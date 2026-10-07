# Chaturbate Client

A mobile-first Android client experiment focused on a lightweight UI, OLED-black rendering, and a replaceable streaming/data layer.

## Current status

This repository is an early Android client.

- Kotlin + Jetpack Compose
- Android API 26+
- OLED-first dark UI
- Home / Favorites / Settings navigation
- Room discovery against Chaturbate's room-list feed, cached locally (versioned JSON)
- Room model and best-effort live discovery

## Discovery and search scope

Search filters the live-room records currently loaded into the local catalogue. It is
not a global profile or offline-account search. A room that is not loaded cannot be
found, and "no match" does not mean an account does not exist.

### Upstream limitation (verified)

The room-list feed (`/api/ts/roomlist/room-list/`) and the playback source
(`/api/chatvideocontext/{username}/`) are **session-gated web endpoints, not a
supported public API**. Probed unauthenticated, both answer with `HTTP 302` to
`/?next=...` and then serve HTML, regardless of the requested gender or parameters.
The client now detects the redirect/non-JSON response and reports that a signed-in
session is required instead of treating it as an empty catalogue. Discover therefore
shows no live rooms until a supported discovery interface is configured.

## Architecture direction

The app is intentionally split conceptually into:

1. **Discovery/data layer** — official Chaturbate-supported APIs where available.
2. **Room/player layer** — the supported cam/embed or playback mechanism.
3. **Native UI** — lightweight Compose screens, favorites, settings, and player controls.

The project should not depend on scraping the entire Chaturbate website unless a specific piece of functionality cannot be implemented through a supported interface.

## Planned investigation

Before implementing direct playback, verify:

- official room discovery endpoints and terms
- supported cam embedding
- available stream quality variants
- player protocol and adaptive bitrate behavior
- chat availability
- authentication requirements
- Android playback and Picture-in-Picture options

## Build

Open the repository in Android Studio with a current JDK 17 installation and let Gradle sync.
A Gradle wrapper is included, so a command-line build works without a local Gradle install:

```bash
./gradlew testDebugUnitTest   # unit tests
./gradlew lintDebug           # Android lint
./gradlew assembleDebug       # debug APK
```

> No credentials or private API tokens belong in the repository.
