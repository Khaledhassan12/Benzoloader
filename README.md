<div align="center">

# 📥 Benzoloader

**Android download manager in Kotlin** · package `khaled.benzo` · minSdk 26 · target/compileSdk 36

HTTP/HTTPS multi-connection downloads · resume · BitTorrent · in-app browser with media detection · English + Arabic

</div>

> Independent implementation of the behaviour the owner wants. It contains no code from, and has no access to the
> internals of, any other download manager.

## 🚦 Read this first: verification state

| Icon | Meaning |
|---|---|
| ✅ | Unit-tested: JVM tests passed under Gradle (local mock HTTP server, no phone). |
| 🔨 | Compiled, no test covers it. |
| ✍️ | Code written but **not compiled or run yet** (written while builds were paused on the `refactor` branch). |
| 📱 | Device-tested. **Nothing in this project has been run on a device or emulator.** |
| 📝 | Planned / not implemented. |

Every status is also written in words, so nothing depends on the icons displaying. Last full build and test run: 40 of 40 unit tests passed at commit `fd59931`. Later commits are ✍️ until a final build.
No speed or reliability measurements exist; this document makes no performance claim.
Per-feature detail: [docs/FEATURE_STATUS.md](docs/FEATURE_STATUS.md). Coverage against the original brief: [docs/SPEC_COVERAGE.md](docs/SPEC_COVERAGE.md).

## ✨ Features

| | Feature | State (in words) |
|---|---|---|
| 🌐 | Single connection and up to 16 parallel parts, minimum part size 512 KB | tested (engine); min part size not built |
| 🛡️ | Range validation (HTTP 206 + `Content-Range` start); servers without Range fall back to one connection | tested |
| 🔁 | Retry with exponential backoff; a short body is never marked complete | tested |
| ⏯️ | Pause/resume from saved chunk progress | tested (engine); not run on a device |
| 🔍 | Resume check: ETag (strong), Last-Modified and length compared with start values; mismatch restarts clean | not built |
| ✔️ | Final size verification before "completed" | tested |
| 🚦 | Speed limit (token bucket), live from Settings; fix for limits below one read | tested (basic limiter); fix and live apply not built |
| 📂 | Safe file names, free-space check, editor user-agent/referer/cookies sent | tested (names, headers); space check compiled only |
| 🗂️ | Queue rules: max concurrent, Wi-Fi only, battery threshold, stop on unplug, schedule window (crosses midnight) | tested (policy); service wiring compiled only |
| 🧲 | Torrent: `.torrent` or magnet, file selection, same queue as other downloads | compiled; later edits not built; never run |
| 🌍 | In-app browser (single page, no tabs/history yet), media sniffer, site manager, per-site profiles | compiled only |
| ⚙️ | Settings: threads, concurrency, Wi-Fi only, battery, unplug, speed limit, schedule, sounds | newest items not built |
| 🔔 | Foreground notification, completion sound | compiled only |
| 🔄 | Boot / schedule recovery: "tap to resume" notification, resume on app open, exact alarm when permitted | not built; not run on a device |
| 🗃️ | Sort new downloads into Video / Music / Documents / Archives / Programs / Torrents folders (Settings switch, off by default; skipped when the user picked another folder) | not built |
| ⏸️ | Pause (keep progress) instead of Failed when a download fails for good (Settings) | not built |
| 🧬 | Unique file names (`name (1).ext`) when a file or queued download already exists | not built |
| 🔂 | Retries per part on errors, 0 to 10 (Settings) | not built |
| 📝 | HLS download and merge, seeding limits, DHT/port settings | planned, not implemented |

## 🗺️ Project layout

```
Benzoloader/
├── build.gradle.kts        plugin versions (AGP, Kotlin, KSP)
├── settings.gradle.kts     repositories
├── gradle/, gradlew        Gradle wrapper
├── docs/FEATURE_STATUS.md  per-feature implemented / tested table
└── app/
    ├── build.gradle.kts    dependencies, ABIs, packaging
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── res/        layouts, menus, drawables, strings (en, ar), raw
        │   └── java/khaled/benzo/
        │       ├── BenzoApp.kt
        │       ├── core/
        │       │   ├── catalog/   CatalogPolicy
        │       │   ├── download/  DownloadEngine, ChunkWorker,
        │       │   │              TokenBucketLimiter, SpeedTracker,
        │       │   │              ResumePolicy
        │       │   ├── queue/     QueuePolicy
        │       │   ├── torrent/   TorrentEngine, TorrentMapping
        │       │   └── sniffer/   MediaSniffer, DetectedMedia
        │       ├── service/       DownloadService, DeviceConditions,
        │       │                  ScheduleManager, NotificationHelper,
        │       │                  WakeLockManager
        │       ├── receiver/      boot, schedule, notification actions
        │       ├── data/
        │       │   ├── db/        Room entities and DAOs
        │       │   ├── model/     DownloadItem, FileCategory, TorrentItem...
        │       │   └── repository/ DownloadRepository, SettingsRepository
        │       ├── provider/      BenzoFileProvider
        │       └── ui/            main, editor, browser, torrent, site,
        │                          settings, props, common
        └── test/                  QueuePolicyTest, DownloadEngineTest,
                                   TorrentMappingTest, ResumePolicyTest,
                                   CatalogPolicyTest, FileNamingTest
```

## 📚 Libraries and what they do

| Library | Version | Role in this project |
|---|---|---|
| Kotlin + Coroutines | 2.2.10 / 1.8.1 | Language, async I/O, cancellation of downloads |
| OkHttp | 4.12.0 | HTTP/HTTPS client for the download engine (`logging-interceptor` is declared as well) |
| Room (+ KSP compiler) | 2.8.5 | Local database for downloads, chunk progress, torrents, sites (a bookmarks table exists but is unused) |
| jlibtorrent (FrostWire) | 1.2.0.18 | Java binding of libtorrent for the torrent engine |
| `jlibtorrent-android-{arm64,arm,x86,x86_64}` | 1.2.0.18 | Native `.so` per ABI, same version as the Java API |
| AndroidX (core, appcompat, lifecycle, activity, fragment, recyclerview, constraintlayout, drawerlayout, swiperefreshlayout, documentfile) | see `app/build.gradle.kts` | UI framework, ViewModels, lists, folder access |
| Material Components | 1.12.0 | Sliders, switches, toolbar, theming |
| JUnit, MockWebServer, kotlinx-coroutines-test | 4.13.2 / 4.12.0 / 1.8.1 | Unit tests against a local HTTP server |

## 🏗️ Architecture

```
UI (Activities, ViewModels)
  |  writes rows
  v
Room database (downloads, chunks, torrents)
  ^                     
  |  reads queue, writes progress
DownloadService  (1 second tick)
  |  reads: DeviceConditions (network, battery), Settings, schedule
  v
QueuePolicy  -> decides which rows may run now
  |
  +--> HTTP row ----> DownloadEngine + ChunkWorker
  |                   (OkHttp, Range, retry, limiter, resume checks)
  |
  +--> torrent row -> TorrentEngine
                      (jlibtorrent session, real stats)
```

- The UI writes a row and starts `DownloadService`. Every second the service reads network, battery and schedule,
  asks `QueuePolicy` what may run, and starts or requeues tasks.
- A torrent is a normal row (`isTorrent`, info hash). The `.torrent` file and file priorities are stored in the app
  files directory, so the same queue rules apply to it.
- `ScheduleManager` sets an alarm at the next schedule edge (exact when the exact-alarm permission is granted, otherwise inexact); `ScheduleReceiver` wakes the service.
- Android 15+ rules (checked against developer.android.com): a `dataSync` foreground service cannot be started from a
  `BOOT_COMPLETED` receiver for apps targeting 35+, and it is limited to 6 hours per 24 hours. The boot receiver
  therefore re-arms the alarm and posts a "tap to resume" notification when downloads are waiting. Opening the app starts the service (an allowed route). The service handles the 6-hour timeout by requeueing downloads and stopping. ✍️ 📱

## 🛠️ Build and run

Requirements: JDK 17, Android SDK platform 36, Gradle through the wrapper.

```
./gradlew :app:assembleDebug          # APK in app/build/outputs/apk/debug/ (about 43 MB with 4 native ABIs)
./gradlew :app:testDebugUnitTest      # JVM unit tests
```

Set `sdk.dir` in `local.properties` or `ANDROID_HOME`.

## ⚠️ Known limitations

- No device or emulator run yet. The native torrent library has only been checked statically (all 3168 Java native
  methods have matching symbols in the arm64 library); it has not been loaded.
- Start, stop and exit receivers exist in code but are not registered in the manifest.
- After a boot, downloads continue after the user taps the "resume" notification or opens the app (✍️, depends on notification permission). Whether the schedule alarm may start the service on its own is unverified; if refused, the same notification appears.
- Cookies and headers are stored unencrypted.
- Unknown-size downloads that drop mid-stream fail safely instead of resuming.
- `res/raw` contains audio plus ad/billing/OMSDK files of unknown origin. They are untouched; licensing needs a
  decision before any release. A stale `libjlibtorrent-2.0.12.9.so` remains in `app/src/main/jniLibs` but is excluded
  from the APK.
- Backup/import/export, overlay window and widget are not verified.

## 🤝 Contributing

Work on a branch and open a pull request. `main` is the original state; current development is on `refactor`.
