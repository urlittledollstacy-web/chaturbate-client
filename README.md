# Chaturbate Client

A mobile-first Android client experiment focused on a lightweight UI, OLED-black rendering, and a replaceable streaming/data layer.

## Current status

This repository is the initial Android scaffold.

- Kotlin + Jetpack Compose
- Android API 26+
- OLED-first dark UI
- Home / Favorites / Settings navigation
- Search UI placeholder
- Room model and mock discovery data
- Internet permission ready for the future API layer

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

> The current UI intentionally uses placeholder room data. No credentials or private API tokens belong in the repository.
