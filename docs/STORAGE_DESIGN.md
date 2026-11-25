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
