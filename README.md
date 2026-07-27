# Dupix — Duplicate File Cleaner

Find and remove duplicate photos, videos, documents, and large files to recover storage
space. Android, Kotlin, Jetpack Compose, Material 3.

## Status

v1 source scaffold. Detects **exact** (byte-identical) duplicates across Photos, Videos,
Audio, Documents, and APKs, plus a Large Files list. No ads, no analytics.

## Requirements

- Android Studio (Ladybug or newer) — bundles the JDK 17, Gradle, and Android SDK.
- Min SDK 30 (Android 11), target SDK 35.

## Build & run

1. Open this folder in Android Studio (`File → Open`).
2. Let it sync Gradle (it will download the wrapper + dependencies on first run).
3. Run the `app` configuration on a device/emulator (Android 11+).

CLI (once a local SDK is configured via `local.properties`):

```
./gradlew :app:assembleDebug        # build the APK
./gradlew :app:testDebugUnitTest    # run the duplicate-engine unit tests
```

> Note: this repo was scaffolded in an environment without a JDK/Android SDK, so it has
> not been compiled here. Build once in Android Studio; if the Gradle wrapper JAR is
> missing, Studio regenerates it automatically (or run `gradle wrapper`).

## Architecture

MVVM + light clean layering, single `:app` module.

```
ui/            Compose screens + ScanViewModel + navigation + theme
domain/        models, ScanManager, engine (Hasher, FileHasher, DuplicateFinder)
data/          repositories, MediaStore/SAF scanners, Room hash cache, CachingHasher
service/       ScanService (foreground scan + progress notification)
di/            Hilt module
```

### Duplicate engine (the core)

Staged so we hash as little as possible (`domain/engine/DuplicateFinder.kt`):

1. Bucket by exact byte size.
2. Partial SHA-256 (head + tail) within each size bucket.
3. Full SHA-256 within each partial-hash bucket → confirmed duplicates.
4. Keep-best selection (keeps the oldest copy) → the rest are deletable.

Hashes are cached in Room keyed by `(path, size, lastModified)`, so unchanged files are
never re-hashed across scans.

### Deletion (Android 11+)

- Media (photos/videos/audio): batched `MediaStore.createDeleteRequest()` — one system
  confirmation dialog.
- Documents / APKs / non-media: SAF (`ACTION_OPEN_DOCUMENT_TREE`) + `DocumentsContract`.
- No `MANAGE_EXTERNAL_STORAGE` (Play-policy safe).

## Tests

`app/src/test/.../DuplicateFinderTest.kt` covers grouping, false-positive avoidance,
size bucketing, keep-best, empty-file handling, and reclaimable-bytes math — pure JVM,
no Android I/O.

## CI / Releases

GitHub Actions build the app automatically:

- **`ci.yml`** — on every push/PR: runs unit tests + builds a debug APK (downloadable
  from the run's artifacts).
- **`release.yml`** — on a `v*` tag: builds a signed release **AAB + APK** and publishes
  a GitHub Release.

Setup (signing secrets, keystore, tagging) is documented in
[`docs/RELEASE.md`](docs/RELEASE.md).

## Design spec

`docs/superpowers/specs/2026-07-27-dupix-design.md`
