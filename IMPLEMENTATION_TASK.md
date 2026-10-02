# CodeRabbit Implementation Task

This PR is the implementation workspace for the completed CodeRabbit audit.

Use the full audit findings already produced on PR #3 as the requirements. Implement the actionable findings across the entire repository, not merely review this file.

Priority:
1. Diagnose and instrument HTTP 400 room-list failures without guessing undocumented API behavior.
2. Fix discovery/search architecture and clearly separate live-room discovery from profile/offline lookup.
3. Fix pagination, partial failures, cancellation, concurrency and stale/partial cache handling.
4. Replace the single SharedPreferences JSON catalogue with appropriate persistent storage.
5. Introduce repository + ViewModel + StateFlow separation.
6. Fix player lifecycle, playback errors, quality handling, fullscreen/navigation, autoplay/data-saver integration.
7. Fix navigation/favorites, performance, privacy/backup and validation.
8. Add regression tests and CI validation.
9. Update documentation.

Do not modify PR #3. Do not invent undocumented Chaturbate API behavior. Preserve working functionality. Run the full build/tests before considering the implementation complete.

This file is a task marker only; the implementation changes belong in this PR.