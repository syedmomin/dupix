# Releasing Dupix (GitHub Actions)

Two workflows live in `.github/workflows/`:

| Workflow | Trigger | Output |
|----------|---------|--------|
| `ci.yml` | push / PR to `main` (or `master`), or manual | runs unit tests, builds **debug APK**, uploads it as a build artifact |
| `release.yml` | pushing a tag `v*` (e.g. `v1.0.0`), or manual | builds **release AAB + APK**, attaches both to a GitHub Release |

## Debug builds (no setup needed)

Every push/PR runs `ci.yml`. Download the APK from the run's **Artifacts** section
(`dupix-debug-apk`). Debug APKs are signed with the auto-generated debug key — installable
on any device, but **not** valid for Play upload.

## Release builds

### 1. Create an upload keystore (once)

```bash
keytool -genkey -v -keystore dupix-upload.keystore \
  -alias dupix -keyalg RSA -keysize 2048 -validity 10000
```

Keep this file and its passwords safe — losing them means you can't update the app on Play
(unless you use Play App Signing key reset).

### 2. Add repository secrets

In GitHub: **Settings → Secrets and variables → Actions → New repository secret**.

| Secret | Value |
|--------|-------|
| `KEYSTORE_BASE64` | `base64 -w0 dupix-upload.keystore` (macOS: `base64 -i dupix-upload.keystore`) |
| `KEYSTORE_PASSWORD` | the keystore password |
| `KEY_ALIAS` | `dupix` (the alias you chose) |
| `KEY_PASSWORD` | the key password |

If these secrets are absent, `release.yml` still runs but produces an **unsigned** release
(useful for testing the pipeline; not uploadable to Play).

### 3. Tag and push

```bash
git tag v1.0.0
git push origin v1.0.0
```

The workflow builds `app-release.aab` + `app-release.apk` and publishes a GitHub Release
with both files and auto-generated notes.

### 4. Upload to Play

Upload the **`.aab`** to the Play Console (Play requires App Bundles for new apps). The
`.apk` is there for sideloading/testing.

## Notes

- Runners use JDK 17, Gradle 8.9, Android SDK platform 35 + build-tools 35.0.0.
- The Gradle wrapper JAR isn't committed; CI uses the Gradle installed by
  `gradle/actions/setup-gradle`. Locally, Android Studio generates the wrapper on first
  open (or run `gradle wrapper`).
