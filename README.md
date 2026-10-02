# Chaturbate Client

A native Android client experiment focused on fast live-room discovery, OLED-black UI, local catalogue search, and native Media3 playback.

## Current architecture

```
Compose UI
  ↓
DiscoveryViewModel / StateFlow
  ↓
RoomRepository
  ├── Chaturbate discovery transport
  └── SQLite catalogue
```

Small user preferences (autoplay, data saver, preferred quality, favorites) remain in SharedPreferences. The potentially large room catalogue is stored transactionally in SQLite.

## Discovery and search scope

The app performs best-effort live-room discovery using the currently observed Chaturbate web room-list endpoint. That endpoint is **not treated as an official public viewer API contract**.

The app:
- loads initial feed pages quickly;
- attempts bounded background pagination;
- deduplicates rooms by username;
- preserves the last usable catalogue when a refresh is partial;
- records feed/page coverage and diagnostics;
- searches the **loaded live-room catalogue** locally by username, subject, gender, location, country, language, and tags.

A local search match is not proof that an account exists globally, and a missing match is not proof that an account is offline or nonexistent.

Global Chaturbate-wide profile search and offline-profile indexing are **not implemented** because no verified supported viewer API contract was established for them. The app does not invent a profile endpoint as a fallback.

Room subjects are modeled separately from category taxonomy. The current feed fields are treated as observed metadata, not as a guaranteed Chaturbate category system.

## HTTP diagnostics

Network failures capture a sanitized diagnostic containing the operation, status, content type, redirect location without query parameters, and a bounded response excerpt. Diagnostics are user-visible only through the Settings screen.

Credentials, cookies, authorization values, signed playback URLs, and unrestricted response bodies are not exported as routine diagnostics.

HTTP 400 is **not considered fixed by diagnostics**. The diagnostic exists so a real failing response can be investigated without guessing which undocumented parameter caused it.

## Playback

Playback uses Media3 when a playable HTTPS HLS source is returned by the current source-resolution path.

The player now:
- respects autoplay;
- applies data-saver and preferred-quality constraints;
- reports Media3 playback errors;
- releases the player with its listeners;
- pauses when the Activity leaves the foreground;
- supports fullscreen and system Back handling.

Resolution selections are track constraints. They do not guarantee that a specific rendition exists upstream.

The current playback source endpoint is undocumented/private web behavior and may change. No guarantee of long-term compatibility is made.

Background playback and Picture-in-Picture are not advertised as supported features.

## Privacy and storage

The catalogue is local application data and is not exported through Android backup. Favorites and preferences remain local as well.

No API credentials or private tokens belong in the repository.

## Validation

Run locally:

```bash
gradle test --no-daemon
gradle lintDebug --no-daemon
gradle assembleDebug --no-daemon
```

CI runs unit tests, Android lint, and the debug build.

## Known upstream limitations

The room-list and playback-source paths are based on observed web behavior rather than a verified public viewer SDK/API. Redirects, HTML responses, HTTP errors, schema changes, authentication requirements, or parameter changes are therefore handled as failures instead of being silently interpreted.

The app intentionally does not claim exhaustive global room coverage unless every configured feed/page request succeeds. Even a complete successful snapshot is best-effort because a live catalogue can change while it is being paginated.
