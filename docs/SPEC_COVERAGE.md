# Spec coverage (branch `refactor`)

Spec = the owner's reconstruction brief (Nov 25). Evidence = `adm-evidence-inventory.md` (screens, pasted reports, manifest; reference material only, nothing about internals).
State words: **done-tested** (JVM tests passed), **done-compiled** (built, no test), **written-unbuilt** (code present, not compiled since builds were paused), **partial**, **missing**, **blocked**, **deferred**. Nothing has run on a device.

| Spec | Item | State | Notes |
|---|---|---|---|
| 3.1 | Several simultaneous downloads | done-compiled | max concurrent in QueuePolicy (tested) and service |
| 3.2 | Multithreaded engine, Range, resume | done-tested | up to 16 parts; validators written-unbuilt |
| 3.3 | Speed control | done-tested (basic), written-unbuilt (fix, live apply, slider) | |
| 3.4 | Smart download | missing | ADM's meaning is unknown from the material; deferred until defined |
| 3.5 | Large files | partial | Long offsets tested arithmetically, no multi-GB run |
| 4 | Lifecycle states | partial | QUEUED, CONNECTING, RUNNING, PAUSED, COMPLETED, ERROR exist; RETRYING, VERIFYING, CANCELED are not separate states |
| 5 | Background service | done-compiled | boot/Android 15 handling written-unbuilt |
| 6 | Queue and scheduler | done-tested (policy), written-unbuilt (UI) | |
| 7 | Network management | partial | Wi-Fi only done; proxy and per-connection profiles missing |
| 8.1 | Open links through Android | done-compiled | VIEW http/https, magnet, .torrent filters; the huge per-extension pattern list is not reproduced |
| 8.2 | Share integration | done-compiled | SEND text/plain |
| 8.3 | Clipboard | partial | read in the editor (foreground only, as the spec requires) |
| 8.4 | Manual URL entry | done-compiled | |
| 9.1 | Categories | written-unbuilt | the category function had a bug (all files became Documents); fixed. Images and "Other" categories are missing |
| 9.2 | Sorting (order, size, name, time, category, year) | missing | next step |
| 9.3 | Filters (all, failed, completed, active, queued, paused) | partial | drawer has all, downloading, completed, queued; failed and paused missing |
| 9.4 | Storage | partial | free-space check and safe names done; duplicate names written-unbuilt; SAF, MediaStore, SD-card picker missing |
| 10 | Browser, tabs, UA, media detection | done-compiled | HLS/manifest handling missing; behaviour unverified |
| 11 | Torrent | written-unbuilt + compiled | real engine (compiled at fd59931), service run, editor, cataloging; missing: sequential, seeding limits, share ratio, trackers list, DHT/port, resume data |
| 12.1 | Downloading settings | partial | destination, concurrent, threads, speed, Wi-Fi, schedule, battery, retries, pause-after-error; missing: auto-resume toggle, proxy, create-empty-file toggle, after-finishing action, user-agent presets |
| 12.2 | Notifications settings | partial | sound, vibrate; missing: pop-up, lock screen, priority, command buttons |
| 12.3 | Scheduler | written-unbuilt | start/stop times, window crossing midnight; no per-day, no network condition per schedule |
| 12.4 | Automation | missing | auto-add without editor, auto-open pages in browser, auto-stop low battery restore |
| 12.5 | Cataloging | written-unbuilt | per-type folders, off by default; per-category folder names and extension lists are not configurable; no "images" or "other" |
| 12.6 | Top window overlay | missing | needs overlay permission; permission declared, no code |
| 12.7 | Interface | partial | dark mode key exists; widget, single list, swap menus, colored icons missing |
| 12.8 | Browser settings | partial | bookmarks and history exist; options panel items unverified |
| 12.9 | Data (backup, import/export txt) | missing | next steps after sort |
| 13 | Profiles and site manager | done-compiled | cookies/headers stored unencrypted: see 20 |
| 14 | Notifications | done-compiled | progress, speed, completion; command buttons missing |
| 15 | UI reconstruction | partial | no pixel match was attempted; reference screens only partly covered by evidence (open gaps F1 to F9 in the inventory) |
| 17 | Database | partial | downloads, chunks, torrents, bookmarks, sites exist; no history table, no headers table, no migrations |
| 18 | Reliability | done-tested (engine) | mock server tests cover Range, no Range, interruption, truncation; not tested: redirects, expired links, process kill, storage failure, network change |
| 19 | Performance | partial | buffered writes, throttled progress; no measurements |
| 20 | Security | partial | path traversal and file names done; cookies and headers in plaintext (missing: Keystore encryption); no URL scheme validation beyond http(s) filters |
| 21 | Tests | partial | 40 passed at fd59931; more written-unbuilt; no instrumented tests |
| (reports) | QA framework from Report 1 | deferred | used as a checklist; CI workflow needs Workflows write permission on the token (untested) |

## Deliberately deferred or blocked
- Smart download, proxy, profiles by connection type: ADM behaviour is not in the supplied material. Needs a decision or more evidence.
- `res/raw` third-party looking files: untouched until origin and licence are settled.
- Device behaviour of everything: blocked until a phone or emulator run exists. No such run is available in this environment.
- Speed benchmark: needs a measured run; none done.

## Next concrete steps (static work, in order)
1. Sorting and the missing filters (failed, paused) in the main list: pure comparator and filter functions with tests, then wire into MainViewModel.
2. Export and import of download lists as text (pure parser and formatter with tests, then two menu actions).
3. Encrypt cookies and headers at rest (Android Keystore, AES-GCM) with a pure wrapper interface so the logic is testable.
4. Images and "Other" categories, configurable folder names.
5. Auto-resume toggle and automation settings that map to existing service behaviour.
