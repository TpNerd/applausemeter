# Refactor / Upgrade Plan (Android -> Latest Kotlin + Modern Android)

## Checklist

- [x] Upgrade Gradle wrapper to latest stable compatible with latest AGP
- [x] Install/configure JDK 17 for Gradle/AGP (ensure `javac` available)
- [x] Upgrade Android Gradle Plugin (AGP) to latest stable
- [x] Upgrade Kotlin to latest stable and enable `org.jetbrains.kotlin.android`
- [x] Remove Kotlin BOM / stdlib excludes leftover from Java-only setup (or reconfigure properly)
- [x] Remove deprecated `package=` from AndroidManifest.xml (use Gradle `namespace`)
- [x] Increase minSdk to satisfy latest AndroidX dependencies
- [x] Align source folder/package structure cleanly (remove legacy `audiocapture` sources)
- [x] Migrate Java sources to Kotlin (`MainActivity.java` -> `MainActivity.kt`, tests as needed)
- [x] Modernize concurrency: replace `Timer`/`TimerTask` with coroutines + lifecycle-aware scopes
- [x] Modernize permissions: migrate to Activity Result API (`RequestPermission`)
- [x] Modernize UI wiring: remove repeated `findViewById` where reasonable (ViewBinding)
- [x] Update dependencies to latest stable (androidx core/appcompat, test libs)
- [x] Verify builds: `./gradlew assembleDebug` and unit tests
- [x] Clean up warnings/lints and ensure project builds reliably
