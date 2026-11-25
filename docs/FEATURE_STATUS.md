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
| Torrent (jlibtorrent) | existing code | no | only arm64 native lib is bundled |
| Site profiles / headers / cookies | existing code | no | service passes empty headers to the engine today |
| Backup / import / export | not verified | no | |
| Top window overlay, widget | not verified | no | |

Known gaps: file-name sanitising on the Editor path, secure storage for cookies/headers, ETag/size change detection on resume,
instrumented (device) tests, UI match against the reference screens.
