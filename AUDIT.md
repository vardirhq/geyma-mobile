# Geyma Mobile — Audit

_Date: 2026-07-12 · Scope: full codebase (main sources, DAOs, manifest, resource
configs, CI, tests). Reviewed by inspection (no Android SDK in the audit
environment)._

Native Kotlin + Jetpack Compose file manager (`dev.madsens.geyma`, ~2,600 LOC).
Architecture is clean: a single `FsRepository` funnels every mutation through the
journal/stars/sets/trash/seen ledgers; pure logic (`insights/`, `PathUtils`,
`FileKind`, `Skins`) is separated from Android and unit-tested (38 tests); Room
migrations are non-destructive.

Findings are ordered by severity. Items marked **[FIXED]** were addressed in the
same change that added this file.

## High

### 1. Path traversal in share-intake — arbitrary write to external storage **[FIXED]**
`ui/share/ShareIntake.kt`

The destination filename came from another app's `OpenableColumns.DISPLAY_NAME`
(or `uri.lastPathSegment`) and was used directly in `File(inbox, name)`.
`PathUtils.uniqueChildName` never strips separators, so a sharing app could set
`DISPLAY_NAME` to `../../evil.apk` and, since Geyma holds
`MANAGE_EXTERNAL_STORAGE`, write the shared bytes anywhere in external storage.

**Fix:** sanitize the incoming name to its last path segment and reject
`.`/`..`/blank before building the destination.

## Medium

### 2. `allowBackup=true` uploaded the journal — contradicted the privacy promise **[FIXED]**
`AndroidManifest.xml`, `res/xml/backup_rules.xml`, `res/xml/data_extraction_rules.xml`

The README and in-app copy promise *"nothing leaves the device except the file
you choose to share."* But `allowBackup="true"` with backup rules that
`<include domain="database" .../>` meant the Room journal (all file paths,
arrival provenance, timeline) was uploaded to Google cloud backup and copied on
device transfer.

**Fix:** set `android:allowBackup="false"` to match the stated behavior.

### 3. Unescaped `LIKE` wildcards corrupt rebase/remove for real filenames **[FIXED]**
`data/Daos.kt`, `files/FsRepository.kt`, `files/PathUtils.kt`

Path queries used `path LIKE :base || '/%'` with no `ESCAPE`. `%` and `_` are
legal filename characters, so `removeTree("a_b")` also matched `axb`, and
`rebasePaths` after moving `50%off/` rewrote unrelated rows — silent corruption
of the ledgers the app exists to protect.

**Fix:** added `PathUtils.escapeLike`, escape every `LIKE` operand, and append
`ESCAPE '\'`. The `=` and `substr(...)` uses keep the raw path; only the `LIKE`
operand is escaped (via a separate bound parameter where the same path feeds
both).

### 4. Arrival reconcile has a race and no debounce _(not fixed — noted)_
`files/StorageWatcher.kt`, `files/FsRepository.kt`

Every `FileObserver` event launches an unsynchronized `reconcileArrivals` over
all watched folders. Concurrent runs both snapshot `seen.allPaths()` and can each
log the same `ARRIVED` event (journal insert isn't dedup'd, unlike the `SeenFile`
insert). A burst of downloads also triggers a scan storm. Recommended fix:
debounce and guard reconcile with a `Mutex`.

## Low / polish _(not fixed — noted)_

- **Journal never pruned.** `EventDao.prune()` / `count()` are dead code; the
  `events` table grows unbounded. Call `prune(now - Nmonths)` on launch or drop
  the methods.
- **Black splash on light skins.** `res/values/themes.xml` hardcodes
  `windowBackground` to black.
- **Find treats `%`/`_` as wildcards** — same root cause as #3, user-facing
  search. (`escapeLike` is now applied here too as part of the #3 fix.)
- **Continuity import** reads the whole bundle into memory with no size/record
  limit (`Continuity.kt`).
- **Trash size wrong for folders** — recorded as 0 B (`FsRepository.moveToTrash`).
- **FileProvider `/storage` root-path** is broader than needed (`file_paths.xml`).
- **`FileKind.ofExtension` defaults unknown extensions to `TEXT`** → `text/plain`.
- **Distribution:** `MANAGE_EXTERNAL_STORAGE` blocks Google Play unless the app
  qualifies under the file-manager exception (fine for sideload).
- **CI:** `dev-release.yml` runs `git push --delete origin dev` on every publish.

## Solid

Accent color round-trips correctly; move/copy guard against moving a folder into
its own subtree and fall back to copy+delete across volumes; Room migrations are
additive; pure logic is well-tested.
