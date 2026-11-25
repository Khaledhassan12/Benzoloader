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

## Audited, no change needed
- Every PendingIntent uses FLAG_IMMUTABLE.
- Non-launcher components are not exported, except the boot receiver, which checks its action.
- Receivers that read the database already use goAsync and a background thread.

## Found, not changed
- targetSdk 36 enforces edge-to-edge. Only the main screen sets fitsSystemWindows. Other screens (editor dialog, settings, browser, torrent, props) are not checked and need a layout check at the final build.
- The torrent editor accepts file:// and content:// from other apps. It only reads the file as a torrent. Not changed, since it needs a decision on which sources to allow.
- Permissions skill not applied in detail yet.
