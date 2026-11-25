# Build configuration review (static, nothing built or changed)

Sources read (developer.android.com/build): gradle-build-overview, android-build-structure, tool-and-library-dependencies, gradle-dependency-resolution, version-upgrade-strategies (first two thirds in full, the Dependency Guard part only skimmed), jdks (through the Gradle JDK section; the Java language level sections were only skimmed). developer.android.com/reference is an API index and was not read as a whole.

Project facts (from the files): AGP 9.5.0, Kotlin 2.2.10, KSP 2.3.6, Gradle wrapper 9.6.0, compileSdk 36, targetSdk 36, minSdk 26, Java 17 source/target and jvmTarget 17, Room 2.8.5, OkHttp 4.12.0. Last compile (fd59931) used this exact setup and passed 40/40, so the combination resolved and built at that time.

Findings (none changed; each is a decision for later):
1. No Java toolchain is declared. The docs recommend declaring one. Today the JDK that runs Gradle compiles the code (JDK 17 in my environment). Adding a toolchain can trigger a JDK download, so I left it.
2. No version catalog. Versions are inline in app/build.gradle.kts and the root file. Works, but versions are scattered. Optional refactor.
3. Dependencies are pinned, not dynamic. Good. There is no dependency verification file and no lockfile. The docs recommend verification, especially because two non-standard Maven repos (frostwire cloudsmith and raw.githubusercontent) and jitpack are declared. Adding verification metadata needs a Gradle run, so it is deferred.
4. KSP 2.3.6 does not follow the old "Kotlin version prefix" pattern the page describes. It compiled with Kotlin 2.2.10 at fd59931, so I take it as the newer independent KSP versioning, but this page alone does not prove that.
5. gradle.properties has AGP 9 opt-outs (android.builtInKotlin=false, android.newDsl=false, enableJetifier=true, and others). They worked at fd59931, but they are transitional and are likely to stop working in later AGP versions. Not touched.
6. Libraries are older than the SDK they target (AndroidX core 1.13.1, appcompat 1.7.0, material 1.12.0, lifecycle 2.8.6). The upgrade page says to upgrade AndroidX when the SDK changes. Moving them is a risk change that needs a build and tests, so it waits for the build phase and Khaled's decision.
7. targetSdk 36: behaviour changes (edge-to-edge, predictive back and others) are the reason the layout checks stay on the list.
8. minSdk 26 is above every library minimum I know of, but I did not check each one.
9. logging-interceptor is declared and not used. Candidate to remove later.

## Verification capabilities at the final stage (checked 25 Nov, no build run)
- Sandbox: 1 CPU, about 1 GB RAM, no /dev/kvm. An Android emulator cannot run here with acceleration, so no on-device run or screenshot from this sandbox. Its toolchain (JDK 17, Gradle 8.9, Android SDK) can compile and run JVM unit tests when the build stage opens.
- GitHub Actions: the emulator-runner action (github.com/ReactiveCircus/android-emulator-runner) documents hardware-accelerated emulators on Linux runners with KVM enabled, and the GitHub runners page calls nested virtualization experimental and unsupported. So a CI job can plausibly boot an emulator, run instrumented tests and capture screenshots as artifacts. Not tried; an emulator on the CI runner is not the owner's phone, and feasibility for this repository's visibility and runner type is unverified.
- Not available: Paparazzi/Roborazzi style render tests are not in the project; adding them is a dependency decision for the build stage.
