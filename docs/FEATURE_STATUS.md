# Benzoloader feature status

Status of each feature from the project specification. Only what was run is marked "tested".
Last updated on branch feature/engine-hardening. Tests: `./gradlew :app:testDebugUnitTest` (JVM, local mock HTTP server).

| Area | Implemented | Tested | Notes |
|---|---|---|---|
| Single-connection download | yes | yes (byte-exact SHA-256) | |
| Multi-connection (up to 16 parts) | yes | yes (8 and 16 parts) | |
| Range validation (206 + Content-Range start) | yes | yes | HTTP 200 reply to a range request no longer written at an offset |
| Server without Range support | yes (falls back to one connection) | yes | |
| Retry with backoff on dropped connections | yes (4 retries) | yes | |
| Truncated body never marked complete | yes | yes | plus final size verification |
| Pause / resume from persisted progress | engine yes | yes (engine level) | service now persists chunk progress; not yet tested on a device |
| Speed limit applied to real transfer | yes | yes | token bucket; 1 s burst at start |
| Files > 2 GB (Long offsets) | yes | partly (Long arithmetic only) | no multi-GB transfer was run |
| Free-space check before download | yes | no | |
| Foreground service, queue, notifications | existing code | no | |
| Scheduler, Wi-Fi only, battery stop | existing code | no | |
| Browser, tabs, history, bookmarks, sniffer | existing code | no | |
| Torrent engine (jlibtorrent 1.2.0.18) | code present, compiled, APK packages matching native libs for 4 ABIs; pure helpers unit-tested (8 tests) | no | Original TorrentEngine was a stub returning fabricated numbers; replaced with a real session-backed engine. Editor loads real metadata. DownloadService now runs torrents (download row with isTorrent, engine stats mirrored each second, pause/requeue/cancel hooks); compiled only, never run. Never run on a device; no speed measurements. |
| Editor user-agent / referer / cookie reach requests | yes (service now passes them) | yes (engine receives headers) | not encrypted at rest yet |
| Folder savePath + safe file names | yes | yes | Editor stores a folder; engine used it as a file path before |
| Backup / import / export | not verified | no | |
| Top window overlay, widget | not verified | no | |

Added on refactor, written while builds were paused (NOT compiled, NOT run): ETag/Last-Modified/length check on resume, minimum chunk size, speed-limit fix for limits below the read size, live speed limit, Settings screen for speed limit and schedule, unplug toggle bound to the right setting. Also not compiled: cataloging into type folders (CatalogPolicy, off by default), retry-count setting, boot/schedule resume notification, FileCategory.fromExtension fix (it was given whole file names and returned DOCUMENT for everything). Unit tests for ResumePolicy, ChunkPlanner, CatalogPolicy are written but not run.

Known gaps: secure storage for cookies/headers,
instrumented (device) tests, UI match against the reference screens.
