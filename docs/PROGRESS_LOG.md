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
| Fast-resume for torrents (save libtorrent resume data every 60 s, on finish and on pause; use it when the torrent is added again) | WRITTEN, TESTS WRITTEN (file store only). The libtorrent calls were read from the jar's signatures and have never run. Stored .torrent, priority and resume files are deleted when the user removes the torrent (written) |
| If-Range on resumed ranged requests (changed file makes the engine restart clean) | WRITTEN, TESTS WRITTEN. Restart path unverified until a failure-injection test runs |
| Smart download (spec 3.4: a configurable speed-management system covering parallel connections, network changes, scheduling and recovery; must be tested, not assumed) | NOT STARTED. My next action: design it from that list and test it |
| Proxy support (spec phase 6), per-connection profiles, multi-threading mode | NOT STARTED. My next action |

### Queue and service
| Item | State |
|---|---|
| Max simultaneous downloads, queue policy | BUILT+TESTED |
| Newest-first start order (start priority only; not list insertion order) | WRITTEN, TESTS WRITTEN |
| After finishing value row (`after_finish`: do nothing, or exit = stop the service). Exit fires only after a successful completion with nothing running, nothing QUEUED and no torrent being seeded; failures never trigger it; it does not run the clear-notifications-on-Exit setting. Reference shows value "exit"; its other options are not observed, so only these two exist. `AfterFinishPolicy`, 3 tests written, not run | WRITTEN, TESTS WRITTEN, UNBUILT |
| Quick add, two independent switches as in the reference captures (UI_PARITY_PLAN, Automation notes): `quick_add_links` "add without opening the editor" (default OFF) and dependent `quick_add_start` "start after adding" (default OFF, disabled while the parent is off). Traced in code: parent on, child off stores the item as PAUSED (scheduler `nextToStart` and `processQueue` only pick QUEUED), so it does not start until the user resumes it or uses Resume all. It is not "queued and waiting". Both on: stored QUEUED and started when the queue rules allow. Policy extracted to `QuickAddPolicy`; 6 tests written, not run (runtime behaviour unverified). Applies to links opened or shared from other apps; scheme (http/https only), folder check and secrets handling still run before any enqueue; a folder problem opens the editor dialog. Not applied to clipboard prefill, rotation or sniffer picks. Gap: the reference "add downloads" row is a value row (to bottom of list); ours is a switch, so not claimed as a match. Security note: when on, any app can queue a download without a prompt | WRITTEN, TESTS WRITTEN, UNBUILT |
| Side panel (Category, Status, Time, Sorting; one open at a time; counts when non-zero; clear funnel while filtered; status subtitle in the top bar) opened from a new toolbar action, as a second drawer on the other edge. `FilterPanel`, ListState panel methods, 2 more ListState tests written, not run. Gaps: rows have no icons yet, toolbar action uses an existing placeholder icon, the old drawer entries still exist, panel selection of Status uses Stopped = paused (inferred), unrendered | WRITTEN, TESTS WRITTEN (logic), UNBUILT, UNRENDERED |
| Torrents category means "came from a torrent" (rows keep their own type; count overlaps), observed in the video. `ListLogic.inCategory`, 1 test written, not run | WRITTEN, TEST WRITTEN, UNBUILT |
| Time filter (today, yesterday, week, month, year; nested counts) and Order sort, now used by the side panel; 4 tests written, not run | WRITTEN (logic), TESTS WRITTEN, UNBUILT |
| List display order: add new downloads to bottom (`list_add_to_bottom`, default off = top). Display order only. Applied at list creation, on return from Settings, and when the time sort is picked again; 2 tests written, not run | WRITTEN, TESTS WRITTEN, UNBUILT |
| Queue wait reasons in the list | WRITTEN |
| Battery threshold with hysteresis | WRITTEN, TESTS WRITTEN |
| Scheduler (start/stop times) | WRITTEN |
| Start service after phone boot | NOT BUILT. Android 15+ does not allow it for this service type |
| Add to bottom/top of list, after-finishing action | NOT STARTED |
| Deleting a list item while it runs | WRITTEN (before: only the row was removed and the transfer kept running). Whether Delete should also remove partial files is not decided |

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
| Clear finished-download notifications on Exit (`clear_notifs_on_exit`, default off, in backup spec) | WRITTEN, UNBUILT. Triggered only by the Exit action, not by swiping the app away |
| Vibration for intercept and total finish, per-event sound choice | NOT STARTED |

### Browser
| Item | State |
|---|---|
| WebView, media detection | BUILT |
| Tabs (in memory), history, bookmarks dialogs | WRITTEN, TESTS WRITTEN (tab model) |
| Standalone bookmarks and history screens, desktop mode, clear cache | NOT STARTED |
| Interface: Swap side menus | New switch `swap_side_menus` (default OFF, in backup and reset through the settings map). When on, MainActivity sets the navigation drawer to the end edge and the filter panel to the start edge at screen creation; the toolbar button, nav list button, drawer close and filter action use the chosen edge (the default toggle opens START only, so its click is overridden). Now re-applied in onResume (idempotent, closes open drawers first), so it takes effect on return from Settings; the summary says so. Still unverified at runtime: DrawerLayout accepting a runtime gravity change plus requestLayout (written from the API contract, not run); if it does not re-lay out, a recreate() on change is the fallback. Gaps: no unit test (gravity wiring; instrumented check planned for the final stage), RTL behaviour of START/END with Arabic unverified, not rendered. Other Interface switches (single list, coloured icons, top bar at bottom) still not built. Bottom action bar: NOT a regression. Our `bottom_bar` (Step 2b, Khaled 16:23, default ON, in backup and reset) is the closest match to the reference "Bottom action bar for list" row; the reference capture shows it OFF as a real-user example, our approved default stays ON. INFERRED, not proven, that they are the same control. Real remaining gap: reference also has "Bottom action bar in left menu" (on), "Buttons in menu" value row and "Swap buttons Exit and Browser" switch for the side-panel bottom bar; ours has fixed three buttons there with no options. WRITTEN, UNBUILT |
| Left-menu video part 1 (20:03) | Reviewed (see UI_PARITY_PLAN, "Left-side menu video, part 1"): the left menu is the quick-settings panel, Settings > Downloading and Torrent row order/dialogs recorded as OBSERVED vs INFERRED, eye icon confirmed as a per-row visibility editor. Nothing built from it yet; part 2 may follow | REVIEWED, NOT BUILT |
| Left-menu video part 2 (20:08) | Reviewed (UI_PARITY_PLAN, "Left-side menu video, part 2"): panel Notifications/Options/Interface/Tools/App sections, Notifications, Automation, Cataloging, Scheduler, Interface colour rows and picker, Site manager and Batch loading dialogs, quick-settings tile actions. Batch loading is not in our code. Nothing built from it yet | REVIEWED, NOT BUILT |
| Download-list video (20:18) | Reviewed (UI_PARITY_PLAN, "Download-list interaction video"): selection toolbar, selection overflow (Send, Open, Restart/Start/Stop, torrent actions, Select all/Invert/Interval, To top/To bottom/Up/Down), Properties, per-item Editor, new-download editor, delete confirm. Manual reorder is evidence for a stored order column. Nothing built from it yet | REVIEWED, NOT BUILT |
| User-agent presets (from left-menu video part 1) | Default, mobile Chrome, desktop Chrome, Firefox, Safari, Custom as a single-choice dialog; Custom opens the old text dialog. Stored value stays one plain string (backup format unchanged); `UserAgentPresets.indexOf` maps a stored string to a preset or Custom. Preset strings are generic browser formats I wrote (not the reference list: it also had Internet Explorer and Opera 9.80, deliberately not added). 4 unit tests written, not run. Not rendered. WRITTEN, TESTS WRITTEN, UNBUILT |
| Reference videos document (20:40) | New docs/REFERENCE_VIDEOS.md: all 5 videos, observed vs inferred vs not seen, cross-video concepts, per-area implementation status, update rule. Video 5 (full settings: hub, Scheduler, Top window, Interface, Browser, Data) newly reviewed; Top window and Data screen are not built | DOCS |
| Videos doc, video 5 pass 1 (20:42) | Row-by-row t=0-120 s of full-settings video: Downloading rows/values, Proxy dialogs, per-profile values, thread and speed dialogs. Pause after error is seconds; Multi-Threading value row; remaining 120-630 s pending | DOCS |
| Videos doc, video 5 pass 2 (20:45) | Row-by-row t=120-264 s: Torrent, hub, Notifications, start of Automation incl. dependency of Auto-start-after-adding on Auto-add (design check needed vs two-switch quick-add). 264-630 s pending | DOCS |
| Videos doc, video 5 pass 3 (20:47) | Row-by-row t=264-360 s: rest of Automation (thresholds, tile actions), Cataloging, Scheduler, Top window incl. Width row. 360-630 s pending | DOCS |
| Videos doc, video 5 pass 4 (20:50) | Row-by-row t=360-456 s: Top window dialogs/ranges, full Interface order. Corrects two earlier claims (bottom-bar wording; Individual-settings gate not supported by frames). 456-630 s pending | DOCS |
| Videos doc, video 5 pass 5 (20:53) | Row-by-row t=456-625 s: Browser rows/dependencies/dialogs, Data screen dialogs and file suffixes. Whole video now covered at 6 s spacing; unresolved items and scope questions (credential export, browser ad blocking) listed as open | DOCS |
| Docs correction (20:55) | Quick-add: code already implements the dependent child (startNow = skip && startAfter); my 20:41 report wrongly said independent. Video-5 coverage wording narrowed to 6 s spacing | DOCS |
| Videos doc, video 6 browser (20:52) | 258 s browser video reviewed at 5 s spacing: Tabs panel, close dialog, Options with eye editor, page menu, bookmarks/history selection lists, badges, main-list panel. Nothing built for these | DOCS |
| Videos doc, video 7 torrent flow (21:00) | 200 s reviewed at 4 s spacing: file picker, torrent Editor, file selection dialog, run/checking states, torrent menu entries, speed graph. Docs only | DOCS |
| Pause-after-error design (21:00) | Reference shows a seconds value row (7 sec.); its meaning (delay before retry vs wait between attempts) is NOT shown in any frame, so no code change: ours stays boolean PAUSED vs ERROR (ErrorPolicy, AutoResumePolicy backoff 30/60/120 s unchanged). Stored key pause_after_error stays Bool for backup compatibility. Revisit only if a frame or Khaled settles the meaning | DESIGN, UNRESOLVED |
| Static review pass 1 (21:10) | Script checks over fd59931..HEAD, no compile: all 142 changed res XML parse; strings/plurals/arrays identical in values and values-ar; every R.string/color/dimen/id/layout/drawable/menu used in Kotlin and every @-reference in XML resolves in app resources (hits left were android.R platform/appcompat/cronet names). This does NOT check types, nullability, view-binding names or runtime; those wait for the one CI build | STATIC ONLY |
| Scope clarified (21:10) | All video/screenshot content is in scope; Block Ads and Data export sections move from unrequested to tracked-NOT BUILT; credential export needs a safe design. Doc has a Scope rule section | DOCS |
| Top window semantics (21:22) | User-stated: transparent progress overlay above all windows, only when enabled and configured and a download is actually running (torrent or other), completion sound and vibration. Screenshot shows the strip. Doc section added; NOT BUILT, in scope, runtime details await videos | DOCS |
| Top window overlay engine (21:25) | core/overlay/TopWindowSpec.kt (show rule: enabled AND permission AND running; text from the six statistics; percent; opacity clamp 10..100), service/TopWindowOverlay.kt (TYPE_APPLICATION_OVERLAY strip, not touchable, clip-drawn progress, gravity and offsets), 13 settings in SettingsRepository (enabled default OFF, others the observed values), SYSTEM_ALERT_WINDOW in the manifest, DownloadService updates or hides it on each progress tick and on destroy, TopWindowSpecTest (6 tests, written, not run). NOT done: the Top window settings screen, the permission request flow, completion sound/vibration tie-in, backup of the new keys, Arabic strings. With several downloads the strip shows the lowest-id running item (reference behaviour UNCLEAR) | WRITTEN-UNBUILT |
| Sound/vibration decoupled from Top window (21:25) | User 21:21: sound and vibration are automatic download-event feedback, not tied to Top window; names add.mp3, error.mp3, Finished.mp3 supplied. Code check: TopWindowOverlay and TopWindowSpec play no sound/vibration, DownloadService completion sound/vibration is not gated on topWindowEnabled. Existing raw add/done/error/finish untouched; add and finish are not played by code | DOCS + CHECK |
| Top window settings screen (21:27) | ui/settings/TopWindowSettingsActivity.kt built in code: switch (asks for the draw-over-apps permission when missing), statistics checkbox dialog (6), three colour rows, opacity, width, line height, text size, horizontal and vertical position, two offsets; rows dim while the switch is off; hub gets a Top window row between Scheduler and Interface; manifest entry; EN and AR strings (independently worded); own icon. Differences from the reference, open: colours are typed as hex (no square picker), pixel sliders use screen size as max, row wording is ours. Not done: backup of the new keys, rendering. WRITTEN-UNBUILT | WRITTEN-UNBUILT |
| Video 5 dense re-review part 1 (21:42) | 1 s and 3 s frames resolved: Profile radio dialog (mobile internet / internet via Wi-Fi), profile values and OFF state, two distinct Incoming connections rows, Encryption mode dialog and its dimming effect, torrent Individual settings gating Cataloging and Folder. Reverse order, Reset settings, Cataloging extension list, Buttons in menu still open | DOCS |
| Video 5 dense re-review part 2 (21:48) | Browser screen at 3 s: Block Ads dims Auto-update hosts and Host sources, host list dialog behaviour, Open radio, clear dialogs and counts, Swap buttons dependency on left-menu bottom bar. Reverse order dependency still unresolved | DOCS |
| Video 5 dense re-review part 3 (21:52) | Data screen at 2 s: Reset settings confirmation, export-list checkbox dialog (6 entries, OK counts), folder browser dialog for export/import, saved toast. Remaining open: Cataloging extension list, Buttons in menu, Interface individual settings | DOCS |
| Reset settings diff check (21:41) | Code already confirms before reset (MainActivity.confirmResetSettings, resetToDefaults). Reference dialog is a short question with Cancel/OK; ours is a long explanatory message with Reset. OPEN DIFFERENCE, not changed yet | DOCS |
| Export selection logic (21:55) | core/data/ExportSelection.kt (6 entries, OK enable rule, count, credentials excluded from writable set) + 4 tests written, not run. Dialog UI and DB wiring NOT done | WRITTEN-UNBUILT |
| Reset settings dialog (22:02) | Dialog message shortened to the reference's short question with Cancel/OK (EN+AR). Trade-off: the old text warned that saved tabs clear and suggested exporting first; that warning is gone. reset_settings_do string now unused. WRITTEN-UNBUILT | WRITTEN-UNBUILT |
| Reset disclosure restored (22:05) | Per Main: reference-style short question plus a concise consequence line (saved tabs and the default download folder setting are reset; downloads, history, bookmarks, logins kept). Wording difference is safety-driven, tracked. Actual scope per code: backupValues keys + saved tabs. Top window keys are NOT in backupValues so Reset leaves them; OPEN gap | WRITTEN-UNBUILT |
| Top window in backup/reset (22:22) | 13 top_window_* keys added to SettingsBackup.specs and SettingsRepository.backupValues, so settings export/import and Reset include them (pref key names equal backup keys). Existing everySpecIsExportable test covers the new specs (written, not run). Import of enabled=true does not grant the overlay permission | WRITTEN-UNBUILT |
| Bookmarks/history transfer format (22:44) | core/data/PageListTransfer.kt: pure format+parse (URL, title, time; web links only; duplicates dropped) + 3 tests written, not run. DAO wiring (export reads getAllOnce/recent, import insert) and the dialog NOT done yet | WRITTEN-UNBUILT |
| Bookmarks/history DAO wiring (11:05pm) | MainViewModel: exportBookmarksText, exportHistoryText (read DAOs), importBookmarksText (skips URLs already saved). History import not added (no insert-with-time reviewed yet). No UI calls them yet; dialog and file flow still to do | WRITTEN-UNBUILT |
| Export bundle format (11:25pm) | core/data/ExportBundle.kt: multi-section export file (join/split, marker lines, plain list files are not mistaken for bundles) + 4 tests written, not run. Needed so a bookmarks section is never imported as downloads. UI dialog not wired yet. Site manager and authorization sections intentionally NOT offered (no safe design) | WRITTEN-UNBUILT |
| Secrets export design (11:30pm) | docs/EXPORT_SECRETS_DESIGN.md (what is stored, options A passphrase-sealed / B no secrets / C disabled) + core/security/PassphraseEnvelope.kt + 5 tests written, not run. Not wired. Needs user choice | WRITTEN-UNBUILT |
| Export dialog + bundle import wired (11:45pm) | MainActivity.showExportDialog: checkbox dialog with 4 real entries (queue, finished(count), bookmarks, history), OK dimmed until ticked and shows a count, then file picker writes an ExportBundle. importAny: plain lists as before; bundles import queue/finished as paused downloads and bookmarks; history import NOT built. Differences from reference: 4 of 6 entries (site manager, authorization pending user choice), system file picker instead of the in-app folder browser, plain-text bundle format (reference file format UNKNOWN), a finished entry imports as paused downloads. EN+AR strings. WRITTEN-UNBUILT, unrendered | WRITTEN-UNBUILT |
| Finished round-trip fix + history import (11:50pm) | Per Main: finished entries no longer re-import as paused jobs. core/data/FinishedTransfer.kt (records: URL, name, folder, bytes, completedAt, category) + 3 tests written, not run; VM importFinishedText restores COMPLETED records, deduped by URL+folder+name, never queues, never touches files. History import added (dedupe URL+time). OPEN: folder paths are device-specific so a restored record may point to a missing file; behaviour of the list for a completed record with no file UNVERIFIED. Not full restoration (site manager/authorization absent, queue status/progress not kept: queue entries come back paused). WRITTEN-UNBUILT | WRITTEN-UNBUILT |
| Torrent encryption mode engine (12:08am) | TorrentEncryption (prefer/require/disable), setting torrent_encryption (0..2, default prefer) in repository, backup specs and DownloadService -> TorrentEngine.applyNetwork sets in/out enc policy (pe_forced/pe_enabled/pe_disabled). jlibtorrent API names read from the 1.2.0.18 class constants, setInteger parameter types NOT compiled: UNVERIFIED until the CI build. 3 tests written, not run. NO settings-screen row yet (radio dialog + dimming of Incoming/Outgoing rows still to do); Incoming/Outgoing connection rows themselves not built | WRITTEN-UNBUILT |
| Tab race | Traced one stale case: a page finish from the tab just left. Tab record was already guarded; history recording now sits in the same guard. Acceptance extracted to `PageEventPolicy.acceptFinish` with 3 unit tests (written, not run). This covers the traced case only; other stale events are not ruled out. An instrumented regression test (switch tabs during a slow load) is planned for the final build stage. Runtime behaviour unverified. WRITTEN, TESTS WRITTEN, UNBUILT |
| Browser ad blocking | NOT STARTED, undecided. The owner ruled out advertising inside the app (nothing found in the audited source); browser ad blocking is a different, optional feature and has not been requested |

### Interface
| Item | State |
|---|---|
| Settings hub with category screens | WRITTEN, NOT RENDERED |
| Bottom navigation bar (Queue, Finished, badges), switchable in Interface | WRITTEN, NOT RENDERED. Reconciled 20:01: badges exist (badgeQueue, badgeFinished in activity_main.xml, filled in MainActivity from MainViewModel counts over all downloads, hidden at zero, drawable back_badge, commit 1860a86). A stale "NOT done: badges" note in UI_PARITY_PLAN Step 2b caused a wrong later claim; corrected, no regression found in code |
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
| `fdf6b4d` | 18:06 | Failure-injection tests for resume: If-Range header, changed file restarts clean | TESTS WRITTEN |
| `5ef01a9` | 18:05 | List Delete stops the running transfer first; removed torrents lose their stored files | WRITTEN, TESTS WRITTEN |
| `69e6ead` | 17:50 | Torrent fast-resume (resume data saved and reused) | WRITTEN, TESTS WRITTEN (store) |
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
6. Interface (new captures reviewed 19:16, see UI_PARITY_PLAN): side panel with Category/Status/Time/Sorting sections and counts, custom colour picker, value rows for pause-after-error and clear-finished, Top window, Data screen, side panel with graph, remaining Interface rows.
7. Notifications: intercept and total-finish vibration, per-event sound choice (clear on Exit written).
8. Automation: add to bottom or top of list (display order written; start order was already separate), after-finishing action (written, exit option only), auto-open pages (auto-add without editor written).
9. Security and data: backup policy proposal, secrets in URLs, provenance check of `res/raw` files, access-test cleanup.
10. Whole-diff static review of all written code.
11. Final stage (verification options researched in `BUILD_REVIEW.md`; this sandbox has no emulator, a CI emulator job is plausible but untried): dependency and library upgrades (staged plan), edge-to-edge, Arabic review, then one CI build, fix its errors, then run the tests and publish the APK as an artifact (signing and retention reported at handoff).

Related files: `SPEC_COVERAGE.md` (item by item), `FEATURE_STATUS.md`, `UI_PARITY_PLAN.md`, `ENGINE_REFERENCE_REVIEW.md`, `STORAGE_DESIGN.md`, `DEPENDENCY_REVIEW.md`.
