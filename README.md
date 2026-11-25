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

Last full build and test run: 40 of 40 unit tests passed at commit `fd59931`. Later commits are ✍️ until a final build.
No speed or reliability measurements exist; this document makes no performance claim.
Per-feature detail: [docs/FEATURE_STATUS.md](docs/FEATURE_STATUS.md).

## ✨ Features

| | Feature | State |
|---|---|---|
| 🌐 | Single connection and up to 16 parallel parts, minimum 512 KB per part | ✅ engine (min part size ✍️) |
| 🛡️ | Range validation (HTTP 206 + `Content-Range` start); servers without Range fall back to one connection | ✅ |
| 🔁 | Retry with exponential backoff; a short body is never marked complete | ✅ |
| ⏯️ | Pause/resume from saved chunk progress | ✅ engine · 📱 not tested |
| 🔍 | Resume check: ETag (strong) / Last-Modified / length compared with start values; mismatch restarts clean | ✍️ |
| ✔️ | Final size verification before "completed" | ✅ |
| 🚦 | Speed limit (token bucket), live from Settings; fix for limits below one read | ✅ limiter basics · ✍️ fix and live apply |
| 📂 | Safe file names (no escape from target folder), free-space check, editor user-agent/referer/cookies sent | ✅ names/headers · 🔨 space check |
| 🗂️ | Queue rules: max concurrent, Wi-Fi only, battery threshold, stop on unplug, schedule window (crosses midnight) | ✅ policy · 🔨 service wiring |
| 🧲 | Torrent: `.torrent` or magnet, file selection, runs through the same queue | 🔨 engine/service wired · ✍️ later edits · 📱 never run |
| 🌍 | In-app browser, media sniffer, bookmarks, site manager, per-site profiles | 🔨 |
| ⚙️ | Settings: threads, concurrency, Wi-Fi only, battery, unplug, speed limit, schedule, sounds | ✍️ newest items |
| 🔔 | Foreground notification, completion sound | 🔨 |
| 📝 | Sort-into-folders cataloging, error limit / pause after error, HLS download and merge, seeding limits, DHT/port settings | planned |

## 🗺️ Project layout

```
Benzoloader/
├── build.gradle.kts            plugin versions (Android Gradle Plugin, Kotlin, KSP)
├── settings.gradle.kts         repositories (Google, Maven Central, jlibtorrent repo)
├── gradle/ gradlew             Gradle wrapper
├── docs/FEATURE_STATUS.md      per-feature implemented / tested table
└── app/
    ├── build.gradle.kts        dependencies, ABIs, packaging
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── java/khaled/benzo/
        │   │   ├── BenzoApp.kt               Application class
        │   │   ├── core/
        │   │   │   ├── download/             DownloadEngine · ChunkWorker · TokenBucketLimiter · SpeedTracker · ResumePolicy
        │   │   │   ├── queue/                QueuePolicy (what may run now)
        │   │   │   ├── torrent/              TorrentEngine (jlibtorrent) · TorrentMapping
        │   │   │   └── sniffer/              MediaSniffer · DetectedMedia
        │   │   ├── service/                  DownloadService · DeviceConditions · ScheduleManager · NotificationHelper · WakeLockManager
        │   │   ├── receiver/                 boot, schedule and notification-action receivers
        │   │   ├── data/
        │   │   │   ├── db/                   Room: downloads, chunks, torrents, bookmarks, sites
        │   │   │   ├── model/                DownloadItem · DownloadStatus · FileCategory · TorrentItem · SiteProfile
        │   │   │   └── repository/           DownloadRepository · SettingsRepository (SharedPreferences)
        │   │   ├── provider/                 BenzoFileProvider
        │   │   └── ui/                       main · editor · browser · torrent · site · settings · props · common
        │   └── res/                          layouts, menus, drawables, strings (en, ar), raw sounds
        └── test/                             QueuePolicyTest · DownloadEngineTest · TorrentMappingTest · ResumePolicyTest
```

## 📚 Libraries and what they do

| Library | Version | Role in this project |
|---|---|---|
| Kotlin + Coroutines | 2.2.10 / 1.8.1 | Language, async I/O, cancellation of downloads |
| OkHttp | 4.12.0 | HTTP/HTTPS client for the download engine (`logging-interceptor` is declared as well) |
| Room (+ KSP compiler) | 2.8.5 | Local database for downloads, chunk progress, torrents, bookmarks, sites |
| jlibtorrent (FrostWire) | 1.2.0.18 | Java binding of libtorrent for the torrent engine |
| `jlibtorrent-android-{arm64,arm,x86,x86_64}` | 1.2.0.18 | Native `.so` per ABI, same version as the Java API |
| AndroidX (core, appcompat, lifecycle, activity, fragment, recyclerview, constraintlayout, drawerlayout, swiperefreshlayout, documentfile) | see `app/build.gradle.kts` | UI framework, ViewModels, lists, folder access |
| Material Components | 1.12.0 | Sliders, switches, toolbar, theming |
| JUnit, MockWebServer, kotlinx-coroutines-test | 4.13.2 / 4.12.0 / 1.8.1 | Unit tests against a local HTTP server |

## 🏗️ Architecture

```
 UI (Activities + ViewModels)
        │ insert / update rows                       Settings (SharedPreferences)
        ▼                                                   │
   Room database ◄──────────── progress, status ───────┐    │
        ▲                                              │    ▼
        │ read queue                          ┌────────┴────────────────────┐
        └──────────────────────────────────── │  DownloadService (1 s tick) │
                                              │  DeviceConditions ──► QueuePolicy
                                              └───────┬─────────────┬───────┘
                                       HTTP rows      │             │     torrent rows
                                                      ▼             ▼
                                       DownloadEngine + ChunkWorker   TorrentEngine
                                       (OkHttp, Range, retry,         (jlibtorrent session,
                                        limiter, validators)           real stats)
```

- The UI writes a row and starts `DownloadService`. Every second the service reads network, battery and schedule,
  asks `QueuePolicy` what may run, and starts or requeues tasks.
- A torrent is a normal row (`isTorrent`, info hash). The `.torrent` file and file priorities are stored in the app
  files directory, so the same queue rules apply to it.
- `ScheduleManager` sets an inexact alarm at the next schedule edge; `ScheduleReceiver` wakes the service.
- Android 15+ rules (checked against developer.android.com): a `dataSync` foreground service cannot be started from a
  `BOOT_COMPLETED` receiver for apps targeting 35+, and it is limited to 6 hours per 24 hours. The boot receiver
  therefore only re-arms the alarm, and the service handles the timeout by requeueing downloads and stopping. ✍️ 📱

## 🛠️ Build and run

Requirements: JDK 17, Android SDK platform 36, Gradle through the wrapper.

```
./gradlew :app:assembleDebug          # APK in app/build/outputs/apk/debug/ (about 43 MB with 4 native ABIs)
./gradlew :app:testDebugUnitTest      # JVM unit tests
```

Set `sdk.dir` in `local.properties` or `ANDROID_HOME`.

## ⚠️ Known limitations

- 📱 No device or emulator run yet. The native torrent library has only been checked statically (all 3168 Java native
  methods have matching symbols in the arm64 library); it has not been loaded.
- Start, stop and exit receivers exist in code but are not registered in the manifest.
- After a boot, downloads continue only when the app is opened or a schedule alarm manages to start the service.
- Cookies and headers are stored unencrypted.
- Unknown-size downloads that drop mid-stream fail safely instead of resuming.
- `res/raw` contains audio plus ad/billing/OMSDK files of unknown origin. They are untouched; licensing needs a
  decision before any release. A stale `libjlibtorrent-2.0.12.9.so` remains in `app/src/main/jniLibs` but is excluded
  from the APK.
- Backup/import/export, overlay window and widget are not verified.

## 🤝 Contributing

Work on a branch and open a pull request. `main` is the original state; current development is on `refactor`.
