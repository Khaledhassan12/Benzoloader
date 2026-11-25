# Progress log (branch `refactor`)

Live status of the work on this branch. Updated with every pushed batch. `main` is untouched. No pull request.

**Head of branch when this entry was written:** see the first row of "Batch history" (the log's own commit comes right after it).
**Last commit that was compiled and unit-tested:** `fd59931` (40 of 40 tests passed). Every commit after it is written code only.
**Build gate:** no build, no Gradle, no compile tests until the final stage. Then CI builds an installable APK as an artifact.

## How to read the states
| State | Meaning |
|---|---|
| BUILT+TESTED | Compiled and its JVM tests passed (only up to `fd59931`) |
| WRITTEN | Code is in the repo. Not compiled, not run. May contain compile errors |
| TESTS WRITTEN | Unit tests exist but have not been run |
| NOT RENDERED | Layout written; never drawn on a screen. No visual match is claimed |
| NOT DEVICE-TESTED | Never run on a phone. Runtime behaviour is unverified |
| PARTIAL | Some of the feature exists; the gap is named |
| NOT STARTED | Nothing written |
| OPEN | Needs a decision, a device, or a build before it can move |

Screenshot review means looking at the reference pictures. It is not a rendered comparison.

## Status by area
### Downloading engine
| Item | State |
|---|---|
| Multi-part HTTP download with Range, resume, validators | BUILT+TESTED (core), later changes WRITTEN |
| Chunk positions flushed to disk regularly | WRITTEN, TESTS WRITTEN. Durability is unverified until failure-injection tests |
| Minimum part size setting (128 KB to 4 MB, default 512 KB) | WRITTEN, TESTS WRITTEN |
| Default user agent setting (fallback only) | WRITTEN, TESTS WRITTEN |
| Auto-resume after transient errors (opt-in), Retry-After handling | WRITTEN, TESTS WRITTEN |
| Fast-resume for torrents | NOT STARTED |
| If-Range on resumed ranged requests (changed file makes the engine restart clean) | WRITTEN, TESTS WRITTEN. Restart path unverified until a failure-injection test runs |
| Smart download (spec 3.4: a configurable speed-management system covering parallel connections, network changes, scheduling and recovery; must be tested, not assumed) | NOT STARTED. My next action: design it from that list and test it |
| Proxy support (spec phase 6), per-connection profiles, multi-threading mode | NOT STARTED. My next action |

### Queue and service
| Item | State |
|---|---|
| Max simultaneous downloads, queue policy | BUILT+TESTED |
| Newest-first start order (start priority only; not list insertion order) | WRITTEN, TESTS WRITTEN |
| Queue wait reasons in the list | WRITTEN |
| Battery threshold with hysteresis | WRITTEN, TESTS WRITTEN |
| Scheduler (start/stop times) | WRITTEN |
| Start service after phone boot | NOT BUILT. Android 15+ does not allow it for this service type |
| Add to bottom/top of list, after-finishing action | NOT STARTED |

### Torrent
| Item | State |
|---|---|
| Engine, editor, service run, cataloging | BUILT (at `fd59931`), later changes WRITTEN |
| Sequential, seed-after-finish, seed ratio and time, DHT, connection limit | WRITTEN, TESTS WRITTEN |
| Fixed listen port, anonymous mode | WRITTEN, TESTS WRITTEN |
| Tracker edit dialog (Properties screen), running session only | WRITTEN, TESTS WRITTEN. Not remembered after restart |
| Random port range, NAT-PMP/UPnP/LSD/uTP, encryption mode, per-torrent limits | NOT STARTED |

### Storage
| Item | State |
|---|---|
| Folder validation before start | WRITTEN |
| Optional all-files access request (GitHub variant only) | WRITTEN |
| `github` / `play` build variants | WRITTEN |
| Play storage writer | NOT STARTED. Needs a build-variant gate before any Play use |

### Notifications
| Item | State |
|---|---|
| Progress, speed, completion notifications | BUILT (before `fd59931`) |
| Lock-screen visibility, Pause all button | WRITTEN |
| Vibration on success (existing completion switch, default on) and separate vibration on error (default off), with durations | WRITTEN, TESTS WRITTEN. The success switch did nothing before; it now vibrates |
| Vibration for intercept and total finish, per-event sound choice, clear on close | NOT STARTED |

### Browser
| Item | State |
|---|---|
| WebView, media detection | BUILT |
| Tabs (in memory), history, bookmarks dialogs | WRITTEN, TESTS WRITTEN (tab model) |
| Standalone bookmarks and history screens, desktop mode, clear cache | NOT STARTED |
| Tab race | OPEN: needs a fix and a test. My next action |
| Browser ad blocking | NOT STARTED, undecided. The owner ruled out advertising inside the app (nothing found in the audited source); browser ad blocking is a different, optional feature and has not been requested |

### Interface
| Item | State |
|---|---|
| Settings hub with category screens | WRITTEN, NOT RENDERED |
| Bottom navigation bar (Queue, Finished, badges), switchable in Interface | WRITTEN, NOT RENDERED |
| Theme: device colours (Material You) plus four presets | WRITTEN, NOT RENDERED. PARTIAL: no custom colour picker |
| Manual light / dark / follow-system choice (Interface) | WRITTEN, NOT RENDERED. Default follows the system, as before. The old stored `dark_mode` key stays unread |
| Green value rows for 8 slider settings | WRITTEN, NOT RENDERED |
| Cataloging: switch, one row per category, category screens | WRITTEN, NOT RENDERED. PARTIAL: extensions and folder per category are display only |
| Dependent rows fade with their switch | WRITTEN, NOT RENDERED |
| Pause-after-error and clear-finished as value rows | NOT STARTED (they are still switches) |
| Top window overlay, Data screen, side panel with graph | NOT STARTED |
| Main list | PARTIAL reference: the empty Queue shell capture was supplied and its bottom bar is written. No populated-row captures were supplied, so row layout is not matched |
| Editor and properties screens | NOT CHECKED. No captures of them were supplied |

### Data and security
| Item | State |
|---|---|
| Settings export, import and reset (settings only) | WRITTEN, TESTS WRITTEN |
| Cookies, headers and site passwords sealed with Keystore-backed AES-GCM | WRITTEN, TESTS WRITTEN. Device behaviour unverified |
| `allowBackup` | Left as it was (true). Backup policy not decided; I must propose one with the tradeoff |
| Secrets inside URLs | NOT STARTED. Audit and handling to be designed by me |
| Old ad-named files and mp3s in `res/raw` | Origin unknown, not touched. Provenance check is pending |

### Release
| Item | State |
|---|---|
| Dependency upgrades (staged plan) | NOT STARTED. Plan in `DEPENDENCY_REVIEW.md` |
| CI build and APK artifact | NOT STARTED (final stage). Signing and artifact retention to be reported at handoff |
| Arabic wording review by a native speaker | NOT DONE. Strings are my own wording; no reviewer arranged yet |

## Batch history (newest first)
| Commit | Time (25 Nov, Cairo) | Change | State |
|---|---|---|---|
| `c9d73af` | 17:34 | Manual light / dark / follow-system choice | WRITTEN, NOT RENDERED |
| `2260b7b` | 17:30 | Disabling a container row now disables its child rows (Cataloging category rows) | WRITTEN, NOT RENDERED |
| `8d66a24` | 17:29 | If-Range on resumed requests | WRITTEN, TESTS WRITTEN |
| `2b5c204` | 17:25 | Tracker edit dialog on the Properties screen | WRITTEN |
| `176eebb` | 17:25 | Tracker policy and engine accessors | WRITTEN, TESTS WRITTEN |
| `0485df4` | 17:24 | Notes: notification and vibration API review | docs |
| `931463b` | 17:24 | Error vibration is its own switch, default off | WRITTEN |
| `5c83c48` | 17:23 | Vibration wired to success and error | WRITTEN, TESTS WRITTEN |
| `0de0cad` | 17:22 | Minimum part size and default user agent | WRITTEN, TESTS WRITTEN |
| `b940a94` | 17:20 | Cataloging category rows and screens | WRITTEN, NOT RENDERED |
| `9cfc01b` | 17:19 | Green value rows with slider dialogs; fixed a missing import in my own code | WRITTEN, NOT RENDERED |
| `8efc25d` | 17:17 | Dependent rows fade; newest-first start order | WRITTEN |
| `9ee3d0d` | 17:10 | Fixed torrent port; anonymous mode | WRITTEN |
| `c9e0546`, `3bd191d` | 16:29 to 16:30 | Theme with device colours and presets | WRITTEN |
| `45c6c45`, `1860a86` | 16:25 to 16:26 | Bottom bar, Queue filter, badges | WRITTEN |
| `b9af743`, `42ffce7` | 16:13 to 16:15 | Settings hub, first palette | WRITTEN |
| `c926ef7`, `d5ca7c6` | 16:09 to 16:10 | Chunk flushing, magnet metadata saved | WRITTEN (durability unverified) |
| `1e6a6a2` | 16:07 | Engine research notes (no engine change) | docs |
| earlier | 14:11 to 15:55 | Service split, auto-resume, Retry-After, permissions flow, storage variants, reset, seeding, browser tabs and history, settings export, and other items | see `git log` and `SPEC_COVERAGE.md` |

## Known risks right now
1. Nothing after `fd59931` has been compiled. The first CI build may show compile errors in the written code. They will be fixed then.
2. No screen has been rendered. Layout parity with the reference captures is unverified.
3. HTTP and torrent durability claims are unverified until tests that inject failures are run.
4. Success vibration is newly active for installs that never chose it.

## Remaining scope, in order of work
Implementation comes first. The one build happens at the end, after all of this.
1. HTTP integrity: failure-injection tests written for resume, flush and If-Range paths.
2. Torrent: fast-resume and restart restore, random-port range, NAT-PMP/UPnP/LSD/uTP, encryption mode, per-torrent limits, tracker persistence.
3. Storage: Play-variant storage writer behind a build-variant gate; editing the folder of an Error item.
4. Network: smart download, proxy, per-connection profiles, multi-threading mode, user-agent presets.
5. Browser: tab race fix, standalone bookmarks and history screens, desktop mode, pop-up tabs, clear cache, address-bar position.
6. Interface: custom colour picker, value rows for pause-after-error and clear-finished, Top window, Data screen, side panel with graph, remaining Interface rows.
7. Notifications: intercept and total-finish vibration, per-event sound choice, clear on close.
8. Automation: add to bottom or top of list, after-finishing action, auto-open pages, auto-add without editor.
9. Security and data: backup policy proposal, secrets in URLs, provenance check of `res/raw` files, access-test cleanup.
10. Whole-diff static review of all written code.
11. Final stage (verification options researched in `BUILD_REVIEW.md`; this sandbox has no emulator, a CI emulator job is plausible but untried): dependency and library upgrades (staged plan), edge-to-edge, Arabic review, then one CI build, fix its errors, then run the tests and publish the APK as an artifact (signing and retention reported at handoff).

Related files: `SPEC_COVERAGE.md` (item by item), `FEATURE_STATUS.md`, `UI_PARITY_PLAN.md`, `ENGINE_REFERENCE_REVIEW.md`, `STORAGE_DESIGN.md`, `DEPENDENCY_REVIEW.md`.
