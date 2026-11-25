# Dependency freshness review (research only; no version was changed)

Date of check: Nov 25 2026. Sources fetched live: repo1.maven.org maven-metadata.xml (okhttp, mockwebserver, kotlinx-coroutines-android, junit, jlibtorrent, jlibtorrent-android-arm64, material), developer.android.com/jetpack/androidx/versions/stable-channel and the release pages for core, appcompat, lifecycle, activity, fragment (searched for compileSdk/minSdk notes only, not read in full), github.com/frostwire/frostwire-jlibtorrent releases and README (the 2.1.2.1 release notes were read in part). dl.google.com metadata could not be fetched, so AndroidX numbers come from the stable-channel page.

| Library | In project | Latest stable seen | Kind of change | Notes |
|---|---|---|---|---|
| jlibtorrent (+4 ABI artifacts) | 1.2.0.18 | 2.1.2.1 (GitHub release 13 Nov; Central metadata still shows 1.2.0.18 as latest) | MAJOR: libtorrent 1.2 to 2.1 | Java classes and natives must move together. Replaces the 4 ABI jars with one AAR `com.frostwire:jlibtorrent-android` (minSdk 26, matches ours). Published at dl.frostwire.com/maven and GitHub Packages, not Central. The release notes list API changes (for example adding from TorrentInfo, alerts). TorrentEngine and its mapping need a compile to port. Recommend as its own step after the first green build. |
| OkHttp + mockwebserver | 4.12.0 | 5.5.1 | MAJOR | 5.x keeps the okhttp3 package, but has source and binary changes and a different mockwebserver artifact layout. Needs a compile. |
| kotlinx-coroutines | 1.8.1 | 1.11.0 | minor line | Likely compatible with Kotlin 2.2; not verified. |
| junit | 4.13.2 | 4.13.2 | none | Already current. |
| Room | 2.8.5 | 2.8.5 | none | Already current. |
| Core/core-ktx | 1.13.1 | 1.19.1 | several minors | The core release page mentions a compileSdk change from 36 to 36.1 in some release between ours and 1.19.1. Our compileSdk is 36. Could not confirm which version needs what. |
| AppCompat | 1.7.0 | 1.8.0 | minor | |
| Lifecycle | 2.8.6 | 2.11.1 | minors | Compose libraries moved to compileSdk 37 (needs AGP 9.2+). We do not use Compose, so this may not apply to us, unverified. |
| Activity / Fragment | 1.9.3 / 1.8.4 | 1.12.4 / 1.9.1 | minors | AndroidX default minSdk is now 23; ours is 26. |
| ConstraintLayout | 2.1.4 | 2.2.2 | minor | |
| RecyclerView | 1.3.2 | 1.4.0 | minor | |
| SwipeRefreshLayout / DocumentFile / DrawerLayout | 1.1.0 / 1.0.1 / 1.2.0 | 1.2.0 / 1.1.0 / 1.2.0 | minor | |
| Material Components | 1.12.0 | 1.14.0 (Central metadata) | minor | Theme and style behaviour changes are possible; needs a visual check. |

Decision: nothing changed. Raising these blind could force a compileSdk change and break the first build, and without a build I cannot tell which break is mine. Proposed order at the build phase: (1) first green build on the current set, (2) AndroidX + Material + coroutines bump with compileSdk checked against each release note, (3) OkHttp 5, (4) jlibtorrent 2.1.2.1 migration as its own change with the torrent tests. Items 3 and 4 are the load-bearing tradeoffs: behaviour risk for downloads and torrents.

## Follow-up: compileSdk and tool requirements (read from official pages, no build)
- AGP 9.5 requires Gradle 9.6.0 (developer.android.com/build/releases/about-agp, table). The project already uses AGP 9.5.0 with Gradle 9.6.0, so the pair matches the page.
- AGP page lists the minimum AGP for API 36.1 as 8.13.0, so AGP 9.5.0 supports compileSdk 36.1. The SDK platform 36.1 itself must be installed where the build runs; the build environment I used has the platform for 36, and I have not checked whether 36.1 is present.
- androidx.core: the 1.18.0 release (11 Mar 2026) notes say its own compileSdk changed from 36 to 36.1. Whether that is enforced as a minimum for apps through library metadata is NOT confirmed by the page. Treat core 1.18 and later as needing compileSdk 36.1 until a build shows otherwise.
- androidx.recyclerview 1.4.0: "will only compile against API 35 or higher" (page). Our compileSdk 36 is fine.
- activity/lifecycle: the compileSdk 37 notes are for their Compose libraries, which we do not use. No statement found that activity-ktx or lifecycle-*-ktx need more than 36. Activity 1.12 needs lifecycle-runtime 2.10 or later (page, for lint with AGP 9.5), which the staged bump covers.
- constraintlayout, fragment, appcompat, material: no compileSdk statement found on the pages I searched. Unconfirmed, not a guarantee.
Staged plan stays: first green build, then bump AndroidX in one commit and set compileSdk 36.1 only if the build or the dependency metadata asks for it.
