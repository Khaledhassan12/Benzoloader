# Benzo (بينزو) — Development Working Notes & Gap Analysis

**Package Name:** `khaled.benzo`  
**Target SDK:** 36 | **Min SDK:** 26  
**Reference Benchmark:** ADM 14.0.43 (`com.dv.adm`) & `Benzo_Architectural_Blueprint.md`

---

## 1. Initial Architecture Audit & Codebase Gaps

### Core Build & Dependencies
- **KSP / Room Compatibility:** Resolved KSP2 issue by upgrading Room to `2.8.5`.
- **ABIs & Native Libraries:** `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64` included in `app/build.gradle.kts`.

### Component Gaps & Defect Inventory
1. **Application Class (`BenzoApp.kt`)**: Missing notification channel registration.
2. **Download Engine (`DownloadEngine.kt` & `ChunkWorker.kt`)**:
   - Must limit max concurrent active downloads to 5.
   - Must support up to 16 HTTP range chunks per file.
   - Sparse file pre-allocation via `RandomAccessFile` / NIO `FileChannel`.
   - 16 KB buffer size per chunk write.
   - Progress persistence every 1 second or 512 KB written.
   - SHA-256 & length verification upon completion.
   - Moving average speed calculation over 2 seconds (`SpeedTracker.kt`).
   - Token bucket bandwidth throttling (`TokenBucketLimiter.kt`).
3. **BitTorrent Engine (`TorrentEngine.kt`)**:
   - `jlibtorrent` integration for Magnet links and `.torrent` files.
   - Ephemeral session metadata fetching.
   - File selection and piece priorities: `0` (ignore), `1` (normal), `7` (top priority/sequential).
   - DHT, PEX, LSD support.
4. **Media Sniffer (`MediaSniffer.kt`)**:
   - DOM video/audio element inspection via JavaScript injection.
   - HLS `.m3u8` master playlist resolution and MPEG-DASH `.mpd` parsing.
5. **Foreground Service & Lifecycle (`DownloadService.kt` & `WakeLockManager.kt`)**:
   - `FOREGROUND_SERVICE_DATA_SYNC` type.
   - Partial WakeLock management.
   - Battery threshold protection (stop downloads if battery <= user setting).
   - Dynamic notification progress bars.
6. **Receivers**:
   - `DownloadStartReceiver`, `DownloadStopReceiver`, `AppExitReceiver`, `ScheduleReceiver`, `SystemEventReceiver`.
7. **Presentation & ViewBinding**:
   - Activity signatures must be `onCreate(savedInstanceState: Bundle?)`.
   - Wiring ViewModels to DAOs and engines cleanly using StateFlow.
8. **Localization**:
   - Complete English (`values/strings.xml`) and Arabic (`values-ar/strings.xml`).
9. **Verification**:
   - Unit tests covering chunk downloading, resume, checksum, token bucket, and torrent priorities.
   - Real HTTP download and resume verification on a physical/emulated Android device via ADB.
