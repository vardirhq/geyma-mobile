# Building Geyma Mobile

## Requirements

- JDK 17
- Android SDK with API 35
- Android build tools compatible with Gradle 8.14.3

The app targets Android 15 (`targetSdk 35`) and supports Android 8.0 and newer
(`minSdk 26`).

## Commands

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
```

The debug APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Continuous integration

`.github/workflows/android.yml` builds and tests the project on pushes and pull
requests, then uploads the APK as the `geyma-mobile-debug` artifact.

The rolling development release is published at:

<https://github.com/vardirhq/geyma-mobile/releases/tag/dev>

Debug builds use the runner's temporary signing key. Android may require an older
development build to be uninstalled before installing a newer one.

Stable releases are published by `.github/workflows/release.yml` when a
`v*.*.*` tag is pushed. The workflow validates the tag against `versionName`,
runs tests and lint, builds a release APK, signs it with repository secrets, and
attaches the APK plus a SHA-256 checksum to the GitHub Release.

See [Releasing Geyma Mobile](releasing.md) for the release checklist and required
signing secrets.

## Architecture at a glance

- `theme/` — the eight desktop-compatible skins and Material 3 integration
- `files/` — filesystem operations, classification, OCR, archives, and viewers
- `data/` — Room entities/DAOs plus DataStore preferences
- `ui/` — Compose screens and shared components

`FsRepository` is the single mutation boundary. File operations and their journal,
trash, star, set, revisit, note, seal, and OCR updates must remain one coherent action.

Database migrations are additive. Never replace them with a destructive migration:
the journal surviving application updates is part of Geyma's core promise.
