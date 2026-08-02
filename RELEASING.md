# Releasing Dupix to Google Play

All builds run in **GitHub Actions** — no local Java or Android Studio needed.
Package name: `dev.bit.dupix`.

## Step 1 — Create your upload keystore (once)

Actions tab → **Generate Upload Keystore** → *Run workflow*. Enter:
- **Key alias** — e.g. `dupix-upload`
- **Keystore password** and **Key password** — pick strong values and **write them
  down**; they cannot be recovered.

When it finishes, open the run → **Artifacts** → download **`dupix-upload-keystore`**.
It contains `upload.jks` and `upload.jks.base64`.

## Step 2 — Add the four repo secrets

Settings → *Secrets and variables* → *Actions* → **New repository secret**:

| Secret | Value |
|--------|-------|
| `KEYSTORE_BASE64` | full contents of `upload.jks.base64` |
| `KEYSTORE_PASSWORD` | the keystore password you chose |
| `KEY_ALIAS` | the alias you chose (e.g. `dupix-upload`) |
| `KEY_PASSWORD` | the key password you chose |

## Step 3 — Build the signed AAB

Actions tab → **Play Store AAB** → *Run workflow* (pick `internal` / `draft` for a
first run). Download the **`dupix-release-aab`** artifact — that's your
`app-release.aab`.

## Step 4 — Upload to Play Console

Create the app in [Play Console](https://play.google.com/console) → upload the
`.aab`. Keep **Play App Signing** enabled (default): Google holds the final signing
key; your keystore above is only the *upload* key.

> The very first bundle for a brand-new app must be uploaded **manually** here.
> After that, set the optional `PLAY_SERVICE_ACCOUNT_JSON` secret and the
> **Play Store AAB** workflow can publish updates automatically.

## Version numbers

`versionCode`/`versionName` are set from CI env vars (`app/build.gradle.kts`).
The workflow derives `versionCode = run_number + 1000` automatically, so every run
produces a higher code and Play never rejects a duplicate. Local builds fall back
to `versionCode = 1` / `versionName = "1.0"`.
