# Storage design (proposal, nothing implemented)

Distribution stated by the owner (25 Nov 2026): GitHub APK now, Google Play later (no developer account yet).

## Verified constraints (official pages fetched 25 Nov 2026)
- File API without a storage permission reaches only app-specific folders and media attributed to the app; other paths throw FileNotFoundException (developer.android.com/training/data-storage/shared/media).
- App-specific external folders (getExternalFilesDir) need no permission (developer.android.com/training/data-storage).
- Downloads and other documents: Storage Access Framework is the documented route; MediaStore.Downloads can be written by the app without permission on Android 10+ for files it creates (same pages; developer.android.com/about/versions/11/privacy/storage).
- Targeting 11+: WRITE_EXTERNAL_STORAGE adds nothing; READ_EXTERNAL_STORAGE is read-only; shared-storage read/write needs all-files access (Android 11 storage page).
- ACTION_OPEN_DOCUMENT_TREE cannot grant the Download folder or the storage root (target 11+).
- Android 10 devices: requestLegacyExternalStorage is the documented opt-out for apps that are written for the old model; this project does not set it, and the flag is ignored when targeting 11+ (Android 11 storage page).
- All-files access: declare MANAGE_EXTERNAL_STORAGE, send the user to ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION, check Environment.isExternalStorageManager() (developer.android.com/training/data-storage/manage-all-files).
- Google Play reviews that permission with a declaration form; a download manager is not among the listed permitted uses; an exception needs a justification (support.google.com/googleplay/android-developer/answer/10467955). Outcome of a review is not knowable from the documents.

## Current state
Declared: MANAGE_EXTERNAL_STORAGE (no limit), READ up to API 32, WRITE up to API 29. Never requested at run time. Downloads and the folder picker use java.io.File paths; libtorrent writes by path. Device behaviour is unverified.

## Recommended design: one storage layer, two build variants
1. Storage layer (all variants). A small `StorageTarget` abstraction decides where bytes go: (a) app-specific external folder (no permission, default for a fresh install of the Play variant), (b) a direct path folder the app can verify it can write (probe at choose time and at start), (c) later: MediaStore.Downloads for finished files. Engines receive a writable File or file descriptor from it; they never assume a public path works. A failed probe gives a clear error and offers the app-specific folder instead of a silent failure.
2. GitHub APK variant: keeps path-based folders. Adds an opt-in, explained request for all-files access (the flow in the manage-all-files page), re-checked on return, with a plain message if denied. Needs the owner's approval before it is written.
3. Play variant: a Gradle product flavor whose manifest does not declare MANAGE_EXTERNAL_STORAGE. Uses (a) and (c), plus SAF folder grants for other folders (not Download or the root). Only if he wants public Downloads without the special access.
4. Torrents: libtorrent keeps writing by path into the app-specific folder (or a verified direct folder in the GitHub variant); on completion an optional move into MediaStore.Downloads or the chosen folder, with the torrent re-pointed or removed first. Seeding from a moved copy is not supported without re-pointing.

## Effort and risk (my estimate, not measured)
- Layer plus probe plus clear errors: small to medium, mostly pure logic, testable on the JVM.
- GitHub all-files request flow: small. Needs a device to verify.
- Play variant (file descriptors for the segmented HTTP engine, MediaStore completion step, SAF picker replacing the File picker, settings backup path semantics): large. Needs a device for every step.
- Android 10: either set requestLegacyExternalStorage for API 29 devices or let the layer fall back to the app-specific folder. Not decided.
- Both variants share the settings file; the download folder is already applied only when it is writable.

## Decisions remaining for the owner
- Approve the all-files request flow for the GitHub variant (not approved yet).
- When Play comes: apply for the special access with a justification, or use the reduced storage mode.
- Android 10 behaviour (legacy flag or app-specific fallback).

## Implemented slice (written, not built, not run; 25 Nov 2026 15:41)
- core/storage/FolderCheck (pure, 9 tests written not run) + AndroidFolderFs (probe file created and removed at once) + ui/common/FolderMessages.
- Editor: before a download is added, the chosen folder is checked. On failure a dialog explains the problem and offers Cancel, Add anyway, or "Use the app folder". The app folder is only ever an explicit choice; the dialog states that uninstalling removes files saved there and that other apps and Downloads do not show them. Nothing is moved and no saved destination is changed silently.
- Service: before a download starts, the same check runs on its saved folder. If it fails, the item goes to Error with the reason, the folder is untouched, and no automatic retry loop starts.
- Android 10: requestLegacyExternalStorage is NOT added. The documentation fetched says the flag is ignored once an app targets 11+ and describes it as a compatibility opt-out for older-model apps; the supported route at target 36 is to stay on scoped storage and use the checks above. Not verified on an Android 10 device.
- Not done: a way to edit the folder of an existing item that went to Error (today: fix access and resume, or delete and add again); Play-variant storage; the GitHub all-files request flow (held, awaiting the owner's answer).

## Optional all-files access request (owner approved 25 Nov 2026 15:41, GitHub variant; written, not built, not run)
- Approval source: the owner's reply "ايوا ده مهم جدا، موافق طبعا" to the question about an optional all-files request for the GitHub variant, with the choice to decline.
- Where it appears: (1) in the folder problem dialog as the first option, only on Android 11+, when not granted and not declined before; (2) Settings button "Files access: allowed / not allowed" (Android 11+ only), always available.
- Flow: explanation dialog (why, optional, can be changed later, what the app writes, what declining means) -> "Continue" opens the documented system screen (ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION); "Not now" is remembered and stops the dialog offer, not the Settings button. The app never grants anything itself. State is re-read with Environment.isExternalStorageManager() each time (never cached). Pure AllFilesAccessPolicy with 5 tests written, not run.
- Variant separation (written, not built; the Gradle flavor setup has never been compiled): two product flavors, `github` and `play`. MANAGE_EXTERNAL_STORAGE is declared only in src/github/AndroidManifest.xml; the main manifest no longer has it. `AllFilesAccess` has two implementations with the same API: the real one in src/github, and in src/play a stub that is never applicable, never granted-check-relevant, opens nothing and offers nothing (Settings hides its button, the editor dialog has no option). Policy tests stay in main and run for both. To verify at the first build: the merged manifest of the play variant must not contain MANAGE_EXTERNAL_STORAGE, and the github one must. Build commands changed: assembleGithubDebug, assemblePlayDebug, testGithubDebugUnitTest.
- Play variant storage gaps (honest): it has no way to write into the public Downloads folder or other shared folders on Android 11+ (no all-files access, no SAF or MediaStore writer yet). The folder check will report an error for such folders, and the explicit app-folder choice is the only working destination there until the MediaStore.Downloads and SAF work (Phase 2 above) is done. Not implemented.




## Android 8 to 10 (API 26 to 29), from the documentation per device API (written, not built, not run)
Source: developer.android.com/training/data-storage/use-cases (fetched 25 Nov 2026).
- Android 9 or lower device: the page says to request WRITE_EXTERNAL_STORAGE following the runtime-permission guidance, then write. Implemented as LegacyStoragePermissionFlow (check, rationale, request, settings after permanent denial; the grant is read from the system each time) with LegacyStoragePolicy (6 tests written, not run). Offered as the first option of the folder problem dialog on API 26 to 29 when the permission is missing, and it continues the add after a grant. It is a normal permission, so it applies to both distributions.
- Android 10 device: the page says an app that targets Android 10 should set requestLegacyExternalStorage to true to keep the old behavior, and that the system ignores the flag on Android 11 devices after an app targets 11. It does not say what a target-36 app gets on an Android 10 device. I added the flag (harmless on 11+, where the page says it is ignored) and kept the probe and the clear errors, so a different real behavior shows up as a clear message instead of a silent failure. Unverified on an Android 10 device.
- Android 11+ devices: unchanged (scoped storage, the Android 11 rules above).
