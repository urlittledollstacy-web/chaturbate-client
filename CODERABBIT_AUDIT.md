# CodeRabbit Full Codebase Audit

This file exists only to trigger a repository-wide CodeRabbit PR review. Do not merge this branch.

Please review the ENTIRE repository, not only this file or the diff.

## Project goal

Native Android Chaturbate client:
- Fast live-room discovery.
- Complete available live-room catalogue rather than only the first API page.
- Search across the actual available live-room dataset.
- Eventually offline profile/profile lookup.
- Eventually categories, tags, genders, regions and discovery filters.
- Native live playback with Media3/HLS.
- Stream quality selection.
- Responsive OLED-friendly UI.
- Persistent room cache for fast subsequent launches.

## Required audit

### P0/P1: Search
Determine exactly why search is incomplete/unreliable:
- actual dataset loaded
- whether complete catalogue is really loaded
- whether the room-list endpoint supports query/search
- whether our query parameter is valid
- whether room subjects are incorrectly treated as categories
- tag parsing
- offline profiles
- whether local search is fundamentally insufficient
- missing profile/search endpoints
- pagination
- replacement vs merge behavior
- stale/incomplete cache

Do not assume downloading thousands of live rooms equals global Chaturbate search. Explain exactly what current search can and cannot find.

### P0/P1: Room catalogue
Audit pagination, total_count, page size, category/gender parameters, deduplication, partial failures, concurrency, race conditions, memory, cancellation, lifecycle, timeouts, rate limiting, and whether fetching thousands of rooms is appropriate.

### P0/P1: HTTP 400
Trace the exact request that can produce HTTP 400. Identify invalid parameters, validity of all four gender values, required parameters, concurrency issues, and whether the endpoint is undocumented/private. Do not guess.

### P1/P2: Cache
Audit SharedPreferences JSON caching for thousands of rooms: size, serialization, startup performance, stale data, invalidation, atomicity, corruption handling, and whether Room/SQLite/DataStore is more appropriate.

### P1/P2: Player
Audit Media3/HLS source retrieval, offline handling, track selection/quality, autoplay, data saver, errors, lifecycle/release, fullscreen/orientation, background/PiP, buffering/retry, and headers/cookies/CSRF requirements.

### P1/P2: Data model
Audit ApiRoom for live rooms, profiles, categories, tags, regions, languages, location, viewer counts, thumbnails, and offline users. Identify missing fields.

### P1/P2: Architecture
Audit the current Compose/API architecture. Recommend (do not implement) repository, ViewModel, StateFlow, cache, pagination, search, profile, and player layers.

### P2/P3: Android performance
Check main-thread blocking, coroutines, recomposition, leaks, lifecycle, JSON parsing, network volume, image loading, battery/network usage.

### P0/P1/P2: Security/reliability
Check secrets, credentials, HTTP/TLS, URL construction, input validation, WebView risks, sensitive logging, dependency risks, and undocumented/private API assumptions.

## Output requirements

Separate findings into:
- P0 — fundamentally blocks intended functionality
- P1 — major functional bug
- P2 — significant architecture/performance issue
- P3 — minor issue/improvement

For every finding provide:
1. File
2. Exact code/location
3. Problem
4. Root cause
5. Real-world consequence
6. Recommended fix
7. Whether the fix requires API architecture changes

DO NOT modify code as part of this audit.

Focus especially on the ROOT CAUSES of SEARCH and HTTP 400.

Also identify every important assumption based on undocumented/private Chaturbate endpoints or behavior.

The purpose of this PR is diagnosis only.