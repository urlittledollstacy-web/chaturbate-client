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

### Upstream feed access (verified)

The room-list feed (`/api/ts/roomlist/room-list/`) is an internal AJAX endpoint. It
returns JSON only when the request carries `X-Requested-With: XMLHttpRequest`. Without
that header it answers `HTTP 302` to `/?next=...` and serves HTML, regardless of the
requested gender or parameters — which is easy to mistake for a login wall. The client
sends the required header and reads each room's thumbnail from the feed's `img` field
(with `image_url` / `thumb_url` as fallbacks). Thumbnails are served publicly from
`thumb.live.mmcdn.com` and need no session.

The playback source (`/api/chatvideocontext/{username}/`) is also an AJAX endpoint, but
it additionally answers `403 age-gate-required` to unauthenticated clients, so direct
playback still requires a signed-in, age-verified session.

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
