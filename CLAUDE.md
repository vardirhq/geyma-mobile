# CLAUDE.md

Guidance for Claude Code (claude.ai/code) working in this repository.

## What this is

Geyma Mobile is the Android companion to the [Geyma desktop file manager](https://github.com/MadsenDev/geyma-file-manager)
("to keep, to guard" — a file manager that *remembers*). It is a native Kotlin +
Jetpack Compose app that carries over the desktop app's identity and pillars:
a memory journal, ghost trails, working sets, recoverable trash, and the eight
desktop skins. Everything is on-device; there is no network access or telemetry.

When porting or extending a feature, treat the desktop repo as the source of
truth for behavior and visual design. Keep names, tokens, and the event
vocabulary aligned with it rather than inventing new ones (e.g. the skin tokens
in `theme/Skins.kt` are a line-for-line port of the desktop's `src/theme/skins.ts`).

## Build & test commands

```bash
./gradlew testDebugUnitTest    # JVM unit tests (pure logic: paths, kinds, theme)
./gradlew assembleDebug        # debug APK -> app/build/outputs/apk/debug/app-debug.apk
./gradlew lintDebug            # Android lint (non-fatal; abortOnError=false)
```

- JDK 17 and `compileSdk`/`targetSdk` 35, `minSdk` 26.
- The Gradle wrapper is pinned to 8.14.3. When (re)generating it, the sandbox
  cannot validate the distribution URL, so use
  `gradle wrapper --gradle-version 8.14.3 --distribution-type bin --no-validate-url`
  and generate it in a throwaway directory (a stray `settings.gradle.kts` in the
  project root confuses the standalone `gradle` binary), then copy the four
  wrapper files in.

### Verifying in this sandbox

**The Android SDK is not installed and cannot be fetched** — the agent proxy
blocks `dl.google.com` (403 on CONNECT). Maven Central and `maven.google.com`
(dependency resolution) *are* reachable, but there is no `sdkmanager`, no
`local.properties`, and no emulator. Therefore:

- **Do not** try to run `assembleDebug` / `testDebugUnitTest` locally expecting
  them to succeed — they need the SDK.
- **CI is the build-of-record.** Push the branch and confirm the GitHub Actions
  runs go green (`.github/workflows/android.yml`). Read failures with the
  `mcp__github__*` Actions tools and iterate.
- You *can* sanity-check pure-Kotlin logic by reading it; keep genuinely
  build-independent logic in files free of Android imports (see below) so the
  JVM unit tests cover it.

## Architecture

Package root: `dev.madsens.geyma` (app id `dev.madsens.geyma`, matching desktop).

- **`theme/`** — `Skins.kt` holds the 8 skins + override/resolution logic as
  pure Kotlin (no Android imports → unit-testable). `GeymaTheme.kt` maps a
  `ResolvedTheme` onto Material 3 `ColorScheme`/`Typography`, exposes
  `LocalTheme`, and draws the dot/grid backdrop. System bar icon contrast is set
  in `MainActivity` and must follow the skin's light/dark mode, **not** the OS
  dark-mode setting.
- **`files/`** — `PathUtils` and `FileKind` are pure Kotlin (unit-tested).
  `FsRepository` is the single choke point for every mutation: it performs the
  filesystem op *and* writes the journal/stars/trash/set-refs so they stay
  consistent. New file operations go through it, and every mutation must log a
  `FileEvent` (with `prevPath` for rename/move/trash) — that is what powers
  timelines and ghost trails.
- **`data/`** — Room (`GeymaDb`) for journal events, stars, trash registry,
  working sets, the seen-files ledger, and revisits; DataStore (`Prefs`) for
  appearance + view preferences. Path rebasing after a move/rename is done in
  SQL across every path-bearing table (events/stars/set_items/seen_files/
  revisits). SQL `LIKE` operands are escaped via `PathUtils.escapeLike` +
  `ESCAPE '\'` so `%`/`_` in filenames don't act as wildcards.
- **Schema migrations must be real and additive, never destructive.** The
  journal surviving app updates is the whole point of "an app that remembers,"
  so bump the `GeymaDb` version and add a `Migration` (see `MIGRATION_1_2`,
  `MIGRATION_2_3`) rather than falling back to `fallbackToDestructiveMigration`.
- **`ui/`** — Compose screens plus shared `components/`. `GeymaRoot` owns the
  bottom-nav shell (Home, Files, Timeline, Sets) and hosts everything else —
  Trash, Settings, and the mobile-original surfaces (`sweep/` for arrivals never
  opened, `almanac/` history digest, `dossier/` per-file detail, `echoes/`
  revisit reminders, `finder/` journal search, `share/` inbound share-target,
  `viewer/` in-app file preview) — as full-screen destinations reached from
  within screens, not as tabs.
- **`viewer/`** — tapping a file opens it *inside* Geyma when a built-in viewer
  fits (images with a swipe-through gallery, video/audio via platform players,
  PDFs via the framework `PdfRenderer`, text/code), otherwise it falls back to
  the system chooser. `files/Viewable.kt` (`InAppViewer.kindFor`) is the pure,
  unit-tested routing decision; `GeymaRoot.openEntry` is the single call site
  that picks viewer-vs-external. Viewing records an `opened` event just like an
  external open, so it still feeds the journal, sweep ledger and dossiers.

### Conventions

- Prefer the shared theme tokens (`LocalTheme.current`) over hard-coded colors,
  and `geymaShape()` over ad-hoc corner radii, so overrides propagate everywhere.
- Keep pure logic out of Android-dependent files so unit tests can reach it. Add
  a JVM test in `app/src/test/` for any new pure logic (path math, classification,
  theme resolution).
- Match the desktop's event action strings (`EventActions`) and skin token names.

## Dev APK releases

`.github/workflows/dev-release.yml` builds the debug APK and publishes it to a
rolling `dev` pre-release for on-device testing:
https://github.com/vardirhq/geyma-mobile/releases/tag/dev

It triggers on `workflow_dispatch` **and** on pushes that touch the workflow file
itself — because `workflow_dispatch` isn't registered until the file reaches the
default branch. To cut a fresh dev build from a feature branch, include an edit
to `dev-release.yml` in the push. Debug builds are signed with the runner's
throwaway key, so a new build may require uninstalling the previous one.

A release-signed APK would need a keystore + secrets from the maintainer; that
workflow does not exist yet.

## Git / branch protocol

- Develop on the designated feature branch; commit with clear messages; push with
  `git push -u origin <branch>`.
- If this branch's PR has already merged, treat new work as fresh: restart the
  branch from the latest default branch (`git fetch origin main && git checkout -B
  <branch> origin/main`) rather than stacking onto merged history.
- Only open a PR when the user explicitly asks. Do not push to other branches
  without permission.

## Not yet ported from desktop

Archive preview/extraction, batch rename, rule-based (smart) working sets,
SFTP/SMB remotes, and the local AI (Ollama) features. (Some mobile-original
features have no desktop equivalent — sweep, almanac, echoes/revisits, the
share-target inbox — so "port from desktop" and "matches desktop" don't apply
to those; keep them internally consistent instead.)
