# Engine reference review (research only, no engine change)

Source: the engineering report Khaled sent on 25 Nov 2026 (WhatsApp, 16:06). Checked on 25 Nov against the original pages below. This is research. Nothing here adopts or replaces an engine, and no code was copied from any of these projects. No benchmark was run; no performance numbers exist.

## Claims checked against the originals

| Project | Report says | Original page says | URL |
|---|---|---|---|
| Ketch | Apache-2.0, Kotlin Multiplatform, Maven Central, not 1.0 yet, pure-Kotlin torrent engine | Confirmed: "License: Apache License 2.0", "has not reached 1.0 yet", Maven Central group com.linroid.ketch (core, ktor), Android 8.0+, torrent engine "written in pure Kotlin" (v1, v2, hybrid) | https://github.com/linroid/Ketch |
| Gopeed | "verify the license" | GPL-3.0 (repository page). 2.0.0 is in public beta | https://github.com/GopeedLab/gopeed |
| aria2 | GPL-2.0-or-later | Repository page and COPYING: GPL version 2. The "or later" part is NOT confirmed: I could not read a source header | https://github.com/aria2/aria2 |
| LibreTorrent | GPL-3.0-or-later, libtorrent4j | Confirmed ("either version 3 ... or any later version"; "Based on libtorrent4j") | https://github.com/proninyaroslav/libretorrent |
| Maglev | GPL-3.0, Rust engine, modified librqbit | GPL-3.0; includes a modified copy of librqbit (Apache-2.0) | https://github.com/Kaos599/Maglev |
| jlibtorrent (our torrent binding) | not in the report | MIT | https://github.com/frostwire/frostwire-jlibtorrent |

Useful facts from the originals:
- LibreTorrent's README says the Google Play build has no all-files permission and points to F-Droid or the direct APK. It matches our GitHub/Play storage split (docs/STORAGE_DESIGN.md).
- Ketch's torrent page documents restart behaviour: it saves seeding state about once a minute and when stopping, remembers which tasks seed, and keeps showing the old progress while it re-checks files. It also says a seeding-only Android app is not kept in the foreground. Source: https://github.com/linroid/Ketch/blob/main/docs/torrent.md . This is a design reference for our open "torrent resume after app restart" item. I read documentation only, not Ketch code.

## Licensing: a review gate, not a conclusion
Scope of this file: README and documentation pages only. It is not a code-level evaluation and not a completed five-engine comparison. The GPL licenses listed above carry obligations on copying, linking and distribution that need a proper review against how Benzoloader will be distributed. The repository currently has no LICENSE file, which leaves that question open; it is not a legal finding either way. Until the review is done and Khaled decides, the rule in force is: no third-party code reuse and no engine replacement. aria2's "or later" wording stays unresolved until checked against its authoritative license grant text. Ketch is Apache-2.0 per its repository page; adopting it would still be an engine replacement and a separate decision. Nothing here is legal advice.

## How the current code compares with the report's HTTP requirements (read from code, static)
- 206 and Content-Range checked, and a mismatching start is a permanent error (ChunkWorker).
- Validators (strong ETag, Last-Modified, length) saved and compared before resume (ResumePolicy); only a positive mismatch restarts.
- Unknown length: single chunk path exists (length -1).
- Final length verification before completing (DownloadEngine, "Verification failed").
- Chunks persisted in Room (ChunkEntity) with byte positions.
- Added (written, unbuilt, UNVERIFIED): chunk workers call FileChannel.force every 8 MB and when a chunk stops, and the database now saves the last flushed position of each chunk (DownloadChunk.durableByte) instead of the live one. Intended effect: saved progress is never ahead of a successful flush. NOT proven: that force() makes the bytes durable on every device and file system, that a recovered file has no hole (the file is preallocated with setLength, so its length says nothing about which ranges are complete), or that process kill is safe in every ordering. Recovery does not reconcile the saved chunks against the file contents, and there are no failure-injection tests yet. Treat all durability claims as unverified until such tests run.
- Added (written, unbuilt): a magnet torrent keeps its metadata as a .torrent file once the session has it. This is NOT fast-resume.
- Not present: If-Range on resume requests (the validator check before resume exists); checksum verification when a trusted checksum is offered; libtorrent fast-resume data (jlibtorrent has TorrentHandle.saveResumeData and SaveResumeDataAlert, and libtorrent.write_resume_data_buf; not wired, so a restarted torrent re-checks its files).

## Torrent side
Torrents use jlibtorrent 1.2.0.18 (libtorrent) with a torrent-specific runner, separate from the HTTP chunk table, as the report advises. Open: resume data after app restart, tracker and port settings, stopped-seeding label.

## Engine decision status
No engine switch. The report itself says to choose from measured prototypes. The prototypes and benchmarks need builds and devices, so they wait for the final stage. The decision documents the report asks for (HTTP, torrent, native feasibility, licensing) are not written yet; this file is only the source check.

## Fast-resume implementation notes (written 25 Nov, unbuilt, never run)
- API used, read with javap from jlibtorrent 1.2.0.18: TorrentHandle.needSaveResumeData(), saveResumeData(flags) with FLUSH_DISK_CACHE | SAVE_INFO_DICT; SaveResumeDataAlert.params(); swig add_torrent_params.write_resume_data_buf / read_resume_data; SessionManager.swig().async_add_torrent(params); AlertListener on AlertType.SAVE_RESUME_DATA.
- Stored in filesDir/torrents/<infohash>.fastresume through ResumeStore (temp file then rename; empty or over 4 MB means no data).
- The saved paused flag is cleared on restore so that a user start works. The save path is set again from the item.
- Unverified: whether the restored state skips the re-check on a real device, and whether the alert arrives before a quick stop. Needs a device or emulator run.
