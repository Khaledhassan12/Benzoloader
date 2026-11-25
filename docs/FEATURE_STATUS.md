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
| Browser (single WebView, sniffer) | compiled at fd59931 | no | tabs, history, bookmarks UI are not implemented; bookmark table has no caller |
| Torrent engine (jlibtorrent 1.2.0.18) | code present, compiled, APK packages matching native libs for 4 ABIs; pure helpers unit-tested (8 tests) | no | Original TorrentEngine was a stub returning fabricated numbers; replaced with a real session-backed engine. Editor loads real metadata. DownloadService now runs torrents (download row with isTorrent, engine stats mirrored each second, pause/requeue/cancel hooks); compiled only, never run. Never run on a device; no speed measurements. |
| Editor user-agent / referer / cookie reach requests | yes (service now passes them) | yes (engine receives headers) | not encrypted at rest yet |
| Folder savePath + safe file names | yes | yes | Editor stores a folder; engine used it as a file path before |
| Backup / import / export | not verified | no | |
| Top window overlay, widget | not verified | no | |

Added on refactor, written while builds were paused (NOT compiled, NOT run): ETag/Last-Modified/length check on resume, minimum chunk size, speed-limit fix for limits below the read size, live speed limit, Settings screen for speed limit and schedule, unplug toggle bound to the right setting. Also not compiled: cataloging into type folders (CatalogPolicy, off by default), retry-count setting, boot/schedule resume notification, FileCategory.fromExtension fix (it was given whole file names and returned DOCUMENT for everything). Unit tests for ResumePolicy, ChunkPlanner, CatalogPolicy are written but not run.

Known gaps: secure storage for cookies/headers,
instrumented (device) tests, UI match against the reference screens.

| Encryption of cookies, headers, site passwords at rest | written, not compiled | pure part unit-tested? no: SecretsTest and SecretsMigrationTest written, not run | Keystore path never run on a device; allowBackup is true: a restored database has sealed values without their key and shows the re-enter status |

## Torrent input validation (written, not built, tests written, not run)
- BencodeValidator: iterative structural check (depth 64, 500k items, string lengths inside the buffer, integer form, dictionary keys, no trailing data). It replaces the old first/last byte check, which was only a precheck.
- Content URIs served by this app's own providers are refused. Files are limited to shared storage.
- libtorrent still parses afterwards (TorrentInfo.bdecode in a try/catch). Its own internal limits were not verified: the load_torrent_limits class is not in the jar's Java API.

## Permissions review (android/skills permissions-security, read to section 6 of the file; the remainder not read)
- Found: POST_NOTIFICATIONS was declared but never requested at runtime, so on Android 13+ the progress notification would be hidden. Added NotificationPermissionFlow (check, rationale dialog, request, settings offer after a permanent denial; state read each time, once per fresh launch) with a pure NotificationPermissionPolicy and 5 tests. Written, not built, not run, never shown on a device. Arabic and English strings added.
- Already fine: provider, service and the three internal receivers are not exported; PendingIntents are immutable; exact alarm is checked with canScheduleExactAlarms before use; the exported boot receiver checks the action.
- Declared but no code uses them (decision for Khaled, not changed): SYSTEM_ALERT_WINDOW, MANAGE_EXTERNAL_STORAGE, REQUEST_INSTALL_PACKAGES. Sensitive permissions that are unused are over-privilege and have Play policy review implications. Also usesCleartextTraffic=true (http downloads are a feature, so kept).

## Advertising audit (no ads anywhere is a product rule from Khaled)
Checked statically: source, Gradle files, proguard, AndroidManifest, the merged manifest of the last build (fd59931 era), and the resolved Gradle dependency cache. Result: no ad SDK dependency, no ad class or call, no ads or Play Services entries in the merged manifest, no AD_ID permission. No ads are shown.
Not an integration but unexplained: res/raw holds ad-technology-named files (applovin consent flow json, omsdk and omid js, Play Billing registration files) that nothing references. They came with the original repo and their origin and licence are unsettled, so they are left in place, not deleted. They add size only. Decision for later: remove them once their origin is settled. Limit: dependency resolution after fd59931 was not re-run (no build), but no dependency was added since.

## Auto-resume (written, not built; 6 policy tests and 6 Retry-After tests written, not run)
Setting "auto_resume", OFF by default (opt-in, so existing installs keep their behaviour; the ADM screenshot showing it on is not a proven default). When on, an HTTP download that fails with a failure that may pass is queued again after 30 s, 60 s, 120 s, at most 3 automatic attempts per service process. The delay is a coroutine job, and the requeue only happens if the row still has the status the failure gave it, so a user pause, restart or delete wins.
Retry classification: 408, 429 and 5xx are transient (the chunk worker retries them with backoff and honours Retry-After in seconds or as an HTTP date, never shorter than the server asked; waits up to 60 s inside the running download, and a longer ask stops that download with the hint kept; auto-resume then waits the full server time, or does not retry if it is over 15 minutes. A date is measured against the response Date header when present, else the phone clock. Pause cancels any wait at once). Other 4xx (401, 403, 404, 410, 416) are permanent. A server that ignores Range already falls back to one connection in the engine; if that also fails, auto-resume does not retry. Low storage is not retried.
Limits: torrents are not covered. With "pause after error" on, auto-resume is off (a failure-pause cannot be told apart from a user pause). Attempt counts are in memory and reset when the process dies (reboot, kill). No device test.
Retry-After trace: DownloadEngine runs the parts with async/awaitAll, so the first part to fail decides the reported exception and the other parts are cancelled. That exception carries the hint, so it is kept. Not covered by an engine-level test (the unit tests are on RetryAfter and AutoResumePolicy only).

## DownloadService split (written, not built)
DownloadService (378 to about 313 lines) keeps the queue loop, intents, notification and wake lock. Moved out: HttpTaskRunner (engine call, chunk and progress persistence), TorrentTaskRunner (libtorrent session loop; a cancelled job is now rethrown instead of being able to look finished), AutoResumeScheduler (attempt counts and the delayed requeue). No behaviour was intended to change except that cancelled-torrent fix. Not compiled, and the service has no unit tests (it needs Android), so this is checked by reading only.

## Static diff read since fd59931 (not a compile)
Done by script and by reading: every khaled.benzo import in the 84 changed files resolves to a declared name; English and Arabic strings have the same keys; the R.string references resolve; FileCategory aliases (AUDIO, DOCUMENTS, ARCHIVES, PROGRAMS) exist in the companion. New tests hand-checked against their code: BencodeValidator (3 wrong expected strings found and fixed: each was missing a closing "e" or had an extra one), ListState, ImportPlanner, FolderNames, TorrentSourcePolicy. Not yet hand-checked: Secrets, SecretsMigration, CredentialRepair, ListTransfer, CatalogPolicy, RequestHeaders, ResumePolicy, FileNaming tests. Torrent loading catches Exception (covers SecurityException); native errors such as UnsatisfiedLinkError are not caught on purpose.
