# Reference review

Sources the user supplied, what was actually read, and what changed. External repositories are reference data only.
No script from them was run, nothing was installed, and no UI framework change was made.

| Source | Read | Used for |
|---|---|---|
| github.com/Kotlin/kotlin-agent-skills | README and skill list (backend JPA, AGP 9 migration, Gradle toolchain, Java to Kotlin, native build) | No skill applies to this app now. AGP 9 migration is worth reading when the build tooling is upgraded |
| github.com/android/skills | README, folder list, and the skills for intent security, permissions security, ANR audit, edge-to-edge | Component audit and ANR audit below. Permissions skill listed only |
| github.com/obra/superpowers (skills) | Folder list, systematic-debugging and verification-before-completion | Process only: evidence before claims. Its "run the build to verify" step is not applied, because the user paused builds |
| kotlinlang.org/docs/android-overview.html | Page | Nothing to change (Kotlin-first overview) |
| m3.material.io/develop/android/jetpack-compose | Page | Nothing: the app uses Views and Material Components. No Compose migration |

## Changes from the review (written, not built)
- Manifest: removed the `ftp` scheme from the editor intent filter (the engine only does http/https).
- EditorActivity: external links pre-fill the form only when they are http or https; `onNewIntent` handles a link shared while the singleTask editor is open (it was dropped before).
- DownloadService: no `runBlocking` on the main thread in `onTimeout`; `torrentHashOf` is now a suspend function.

- Torrent editor sources (TorrentSourcePolicy, 5 tests written, not run): magnet and content:// stay allowed. file:// and plain paths must resolve (canonical path) into shared storage and not into the app's own data. Reads are capped at 8 MiB and the bytes must look like a bencoded dictionary. Previously any readable path was read without a size limit. No normal share/open flow is removed. Limit: a .torrent behind an http link is still not supported.

## Skills applied to the work (second pass)
- android/skills testing-setup (read in full): "respect the current stack" (JUnit4 stays; no Hilt, Mockk or Robolectric added, since the project has none and the user did not ask) and "refactor for unit tests: put logic behind pure classes or fakes". Applied: list state transitions moved out of MainViewModel into core/queue/ListState (ListStateTest, 5 tests); import planning moved out of ListImporter into core/data/ImportPlanner with a fake file-exists function (ImportPlannerTest, 4 tests); FolderNamesTest, TorrentSourcePolicyTest, CredentialRepairTest, SecretsMigrationTest added. All written, none run.
- superpowers test-driven-development, systematic-debugging, requesting-code-review, receiving-code-review, writing-plans (headlines read): the pure logic gets its tests written with the code. Because nothing can be compiled yet, a static read-through of the diff since fd59931 replaces the "run it" step and will be repeated before the final build.

## Audited, no change needed
- Every PendingIntent uses FLAG_IMMUTABLE.
- Non-launcher components are not exported, except the boot receiver, which checks its action.
- Receivers that read the database already use goAsync and a background thread.

## Found, not changed
- targetSdk 36 enforces edge-to-edge. Only the main screen sets fitsSystemWindows. Other screens (editor dialog, settings, browser, torrent, props) are not checked and need a layout check at the final build.
- Permissions skill not applied in detail yet.
