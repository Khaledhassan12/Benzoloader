# Benzoloader

Android download manager written in Kotlin (package `khaled.benzo`). It downloads HTTP/HTTPS files with several
parallel connections, resumes interrupted downloads, runs BitTorrent downloads, and has an in-app browser that can
detect downloadable media. The UI is in English and Arabic.

This README describes what the code does and how far it has been checked. Read the **Status** section before relying
on any feature.

> The project is a from-scratch implementation of behaviour the owner wants in a download manager. It does not contain
> code from, and has no access to the internals of, any other download manager.

## Status (honest summary)

| Level | What it means here |
|---|---|
| Unit-tested | JVM tests run under Gradle, using a local mock HTTP server. No phone involved. |
| Compiled | Builds, but no test covers it. |
| Code present | Written, not compiled or run in the latest changes. |
| Device-tested | Run on a real phone or emulator. **Nothing in this project has been device-tested yet.** |

See [docs/FEATURE_STATUS.md](docs/FEATURE_STATUS.md) for the per-feature table. Some commits on the `refactor` branch
were written while building was paused, so a full build and test pass is still required at the end of that work.
No speed or reliability measurements exist yet; do not read any performance claim into this document.

## Features

HTTP/HTTPS engine (`core/download`)
- Single connection or up to 16 parallel parts (parts are never smaller than 512 KB).
- Range responses are validated (HTTP 206 and the `Content-Range` start). A server that ignores Range gets a single
  connection from scratch instead of a corrupted file.
- Retries with exponential backoff, resuming from the last byte written. A short body is never marked complete.
- Resume from saved chunk progress. On resume the server's ETag / Last-Modified / length are compared with those saved
  at start; a positive mismatch restarts the download clean. If the check cannot be made, progress is kept.
- Final size verification before a download is marked completed.
- Speed limit (token bucket, applies live from Settings).
- Safe file names (cannot escape the target folder) and a free-space check.
- User agent, referer and cookies from the editor are sent with requests.

Queue and rules (`core/queue`, `service`)
- Foreground service with a queue and notifications.
- Max concurrent downloads, Wi-Fi only, battery threshold, stop when unplugged, and a daily schedule window (can cross
  midnight). Running downloads return to the queue when a rule blocks them and start again when it clears.

Torrent (`core/torrent`)
- Real engine on jlibtorrent 1.2.0.18 (libtorrent): open `.torrent` files or magnet links, choose files, run through the
  same queue as other downloads. Native libraries for arm64-v8a, armeabi-v7a, x86 and x86_64 come from the matching
  `jlibtorrent-android-*` artifacts.
- Not done: seeding limits, DHT/port settings, per-peer UI, resume-data files.

App
- Download list with categories, add/edit screen, in-app browser with media sniffer, bookmarks, site manager,
  settings (threads, concurrency, Wi-Fi only, battery, unplug, speed limit, schedule, sounds).

## Architecture

```
app/src/main/java/khaled/benzo/
  core/download   DownloadEngine, ChunkWorker, TokenBucketLimiter, ResumePolicy (validators, chunk planning)
  core/queue      QueuePolicy: which downloads may run now
  core/torrent    TorrentEngine (jlibtorrent), TorrentMapping (pure helpers)
  core/sniffer    media link detection for the browser
  service         DownloadService, DeviceConditions, ScheduleManager, notifications, wake lock
  receiver        boot, schedule and notification-action receivers
  data            Room database (downloads, chunks, torrents, bookmarks, sites), repositories, SharedPreferences settings
  ui              main list, editor, browser, torrent editor, site manager, settings, properties
```

Flow: the UI inserts a row in Room and starts `DownloadService`. Every second the service reads device conditions,
asks `QueuePolicy` what may run, and starts or requeues tasks. HTTP rows go to `DownloadEngine`; torrent rows go to
`TorrentEngine`. Progress is written back to the row.

## Build and run

Requirements: JDK 17, Android SDK platform 36 (compileSdk and targetSdk 36, minSdk 26). Gradle is provided by the wrapper.

```
./gradlew :app:assembleDebug          # debug APK in app/build/outputs/apk/debug/
./gradlew :app:testDebugUnitTest      # JVM unit tests
```

Set `sdk.dir` in `local.properties` or `ANDROID_HOME` before building. The debug APK is about 43 MB because of the
four torrent native libraries.

## Known limitations

- Never run on a device or emulator. Foreground-service start from the boot receiver on newer Android versions is not
  verified and may need a change.
- Download start, stop and exit receivers exist in the code but are not registered in the manifest.
- Cookies and headers are stored unencrypted.
- No HLS download or segment merge. No sort-into-folders cataloging (categories only filter the list).
- The `res/raw` audio and ad/billing/OMSDK files have unknown origin and are left untouched. Their licensing needs a
  decision before any release.
- Backup/import/export, overlay window and widget are not verified.

## Contributing

Work on a branch and open a pull request. `main` holds the original project state; development is on `refactor`.
