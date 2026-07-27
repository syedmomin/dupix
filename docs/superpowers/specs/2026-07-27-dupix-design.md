# Dupix — Duplicate File Cleaner — Design Spec

**Date:** 2026-07-27
**Status:** Approved for v1 implementation

## 1. Product

Dupix finds and removes **exact** duplicate files (photos, videos, audio, documents,
APKs) and surfaces large files, so users recover storage. No misleading "RAM boost /
speed up phone" claims — Play-policy safe. Positioning: *"Find and remove duplicate
photos, videos, documents, and large files to recover storage space."*

## 2. v1 Scope

- **Categories:** Photos, Videos, Audio, Documents (PDF/DOCX/PPTX/TXT), APKs, Large Files.
- **Detection:** Exact duplicates only (byte-identical). Similar/blurry photos = v2.
- **Monetization:** None in v1. (Ads/subscription deferred.)
- **Min SDK:** Android 11 (API 30). Target: latest stable.

## 3. Architecture

MVVM + light clean layering, single Gradle module (`:app`).

```
UI (Jetpack Compose screens + ViewModels)
        v
Domain (use cases: Scan, FindDuplicates, Delete, StorageInfo)
        v
Data (repositories) -> MediaStore/SAF scanners + Room cache + FileHasher
```

- **DI:** Hilt
- **Async:** Coroutines + Flow (scan progress streamed as Flow)
- **Persistence:** Room caches file metadata + computed hashes keyed by
  (path, size, lastModified) so re-scans skip unchanged files.
- **Long scans:** Foreground `ScanService` with progress notification.

## 4. Duplicate Detection Engine (core)

Staged pipeline to avoid hashing everything:

1. **Enumerate** — MediaStore for images/video/audio; SAF `DocumentFile` tree for
   documents/APKs/arbitrary roots.
2. **Bucket by size** — only equal byte-sizes can collide; drop unique sizes.
3. **Partial hash** — SHA-256 of first + last 64 KB for same-size files; drop non-colliders.
4. **Full hash** — SHA-256 of full file only for remaining collisions → confirmed dupes.
5. **Group + keep-best** — pick a file to keep (largest resolution / newest / largest),
   mark the rest deletable.

Correctness is paramount: a false-positive = user data loss. The pipeline is pure Kotlin
and unit-tested with temp files.

## 5. Deletion (Android 11+)

- **Media (images/video/audio):** batched `MediaStore.createDeleteRequest()` → one system
  confirm dialog.
- **Documents / APKs / non-media:** SAF `ACTION_OPEN_DOCUMENT_TREE` grant, delete via
  `DocumentFile`; persist granted tree URI permission.
- **No** `MANAGE_EXTERNAL_STORAGE` (Play rejection risk).
- Two-step confirm; show reclaimed space after delete.

## 6. Screens

Splash -> Home Dashboard (storage used / dupes found / recoverable / Scan Now) ->
Scan Progress -> Results (per-category summary) -> Group detail (reused per category) ->
Large Files -> Settings.

## 7. Testing

- **Unit:** hash pipeline + grouping + keep-best selection (pure Kotlin, temp files).
- **Instrumented:** Room DAO; MediaStore/SAF scanners.
- **Manual matrix:** delete flows on Android 11/12/13/14.

## 8. Out of Scope (v1)

Similar/blurry photo detection, WhatsApp/Telegram cleanup, empty-folder/cache cleanup,
AI grouping, scheduled cleanup, cloud analysis, SD-card scan, ads, billing.
