# Geyma Mobile

The Android companion to [Geyma](https://github.com/MadsenDev/geyma-file-manager) — a file
manager that remembers. The name is Old Norse for *"to keep, to guard."*

Native Kotlin + Jetpack Compose, sharing the desktop app's design language and core ideas:

- **Memory journal** — every move, rename, star, trash and restore is recorded locally.
  Each file has an activity timeline (up to 200 events), and the Timeline tab shows
  disk-wide history grouped by day.
- **Arrival provenance** — Geyma watches the folders where files land on a phone
  (downloads, screenshots, camera, messaging media) and journals each new file as it
  *arrives*, so every stray PDF and forwarded image gets a birth certificate — even the
  ones Geyma didn't create. Detected both live and by reconciling on launch.
- **Find** — journal-wide search that looks through the *past*, not just the current
  tree: a file you had last week still turns up after it was moved, renamed, or trashed,
  and the result tells you where it went.
- **Sweep** — cleanup ranked by neglect instead of size: files that arrived and were
  never opened, oldest first. Because trash remembers origins, a sweep is fully
  reversible — restore any of it with one tap.
- **Ghost trails** — folders show faint dashed markers where files recently departed,
  with where they went and when.
- **Working sets** — playlist-like collections of file *references* that stay in sync
  through moves and renames. Geyma registers as a share *target* (share files from any
  app straight into a set) and a set shares back out as a multi-file bundle.
- **Continuity** — export your stars, sets and timeline as a portable `.geyma` bundle to
  hand off to the desktop app or another phone, and merge one back in. No account, no
  server: nothing leaves the device except the file you choose to share.
- **Recoverable trash** — app-managed trash that remembers each file's origin for
  one-tap restore.
- **The 8 desktop skins** — Parchment, Obsidian, Phosphor, Nord, Amber, Plasma,
  Synthwave and Paper, ported token-for-token, with the same override system
  (accent, radius, tile style, mono icons, backdrop pattern, font).

Plus the standard file-manager toolkit: browse internal storage and SD cards,
breadcrumbs, list/grid views, sort and filter, hidden files, multi-select with
copy/move/rename, image and video thumbnails, open/share via other apps.

Everything stays on-device: the journal, sets, the seen-file ledger and preferences
live in a local Room database and DataStore. No network access, no telemetry — the
one thing that ever leaves is a continuity bundle you explicitly share.

## Building

```bash
./gradlew assembleDebug        # debug APK at app/build/outputs/apk/debug/
./gradlew testDebugUnitTest    # JVM unit tests (path utils, kinds, theme resolution)
./gradlew lintDebug
```

Requires JDK 17 and the Android SDK (compileSdk 35). CI builds the debug APK on every
push (`.github/workflows/android.yml`) and uploads it as the `geyma-mobile-debug`
artifact.

## Storage access

Geyma is a full file manager, so on Android 11+ it asks for the "All files access"
special permission (`MANAGE_EXTERNAL_STORAGE`); on Android 8–10 it uses the legacy
read/write permissions. Trash lives in `.geyma/trash` on primary storage.

## Status

Early v0.1 — core browsing, journal, ghosts, sets, trash, skins, plus the mobile-first
additions above (arrival provenance, Find, Sweep, share-sheet sets, continuity bundles).
Not yet ported from desktop: archive preview/extraction, batch rename, rule-based (smart)
sets, SFTP/SMB, local AI features.
