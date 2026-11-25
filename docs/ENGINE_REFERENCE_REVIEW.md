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

## Licensing consequence (a decision for Khaled, not for me)
Benzoloader has no LICENSE file in the repository. Until he picks how it will be distributed, GPL code (aria2, Gopeed, LibreTorrent, Maglev) must not be copied or linked. Apache-2.0 (Ketch) allows reuse with notice, but adopting it is an engine replacement, which is a separate decision. The report itself is not legal advice and neither is this.

## How the current code compares with the report's HTTP requirements (read from code, static)
- 206 and Content-Range checked, and a mismatching start is a permanent error (ChunkWorker).
- Validators (strong ETag, Last-Modified, length) saved and compared before resume (ResumePolicy); only a positive mismatch restarts.
- Unknown length: single chunk path exists (length -1).
- Final length verification before completing (DownloadEngine, "Verification failed").
- Chunks persisted in Room (ChunkEntity) with byte positions.
- Not present: If-Range on resume requests; reconciling saved chunk positions against the actual file size at recovery; checksum verification when a trusted checksum is offered. These are candidates for the next HTTP slice.

## Torrent side
Torrents use jlibtorrent 1.2.0.18 (libtorrent) with a torrent-specific runner, separate from the HTTP chunk table, as the report advises. Open: resume data after app restart, tracker and port settings, stopped-seeding label.

## Engine decision status
No engine switch. The report itself says to choose from measured prototypes. The prototypes and benchmarks need builds and devices, so they wait for the final stage. The decision documents the report asks for (HTTP, torrent, native feasibility, licensing) are not written yet; this file is only the source check.
