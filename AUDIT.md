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

### 4. Arrival reconcile has a race and no debounce **[FIXED]**
`files/StorageWatcher.kt`

Every `FileObserver` event launched an unsynchronized `reconcileArrivals` over
all watched folders. Concurrent runs both snapshot `seen.allPaths()` and could
each log the same `ARRIVED` event (journal insert isn't dedup'd, unlike the
`SeenFile` insert). A burst of downloads also triggered a scan storm.

**Fix:** reconcile now runs under a `Mutex` so only one scan proceeds at a time,
and live `FileObserver` events are coalesced through a 750 ms debounce (which
also lets a file finish writing) instead of firing a scan per event.

## Low / polish

- **Journal never pruned. [FIXED]** The `events` table grew unbounded.
  `FsRepository.pruneJournal()` now runs once per launch (from
  `GeymaApp.onCreate`) and trims entries older than 24 months — a deliberately
  generous window, well beyond what the timeline or almanac surface, so Geyma
  still "remembers." The unused `EventDao.count()` was removed.
- **Black splash on light skins. [FIXED]** `MainActivity.onCreate` now paints
  the window in the saved skin's background color (via a `ColorDrawable`) before
  Compose lays out, so a light skin no longer flashes the manifest's black
  window background on launch.
- **Continuity import unbounded read. [FIXED]** `Continuity.import` now streams
  the bundle through a 64 MB cap and fails fast if exceeded, instead of reading
  an arbitrarily large (possibly hostile) file fully into memory.
- **Trash size wrong for folders. [FIXED]** `moveToTrash` recorded 0 B for
  directories; it now sums the tree so the trash screen shows a real size.
- **CI. [FIXED]** `dev-release.yml` no longer runs a raw
  `git push --delete origin dev` (which would also delete a branch named `dev`);
  it uses `gh release delete dev --cleanup-tag` to remove the release and its
  tag together.
- **Find treats `%`/`_` as wildcards** — same root cause as #3, user-facing
  search. (`escapeLike` is now applied here too as part of the #3 fix.)
- **FileProvider `/storage` root-path** _(left as-is — intentional)_. Geyma holds
  all-files access and shares/opens files on any mounted volume (SD cards and
  USB live under `/storage/<volume-id>` with runtime-assigned ids), so the
  `/storage` root-path is what multi-volume sharing needs; narrowing it would
  break sharing off secondary volumes (`file_paths.xml`).
- **`FileKind.ofExtension` defaults unknown extensions to `TEXT`** _(left as-is)_.
  The desktop's kind palette has no "unknown" bucket, so an unclassified file
  falls back to `TEXT` to stay within it; `mimeOf` still returns `*/*` (not
  `text/plain`) for unknown extensions, so opening is unaffected.
- **Distribution:** `MANAGE_EXTERNAL_STORAGE` blocks Google Play unless the app
  qualifies under the file-manager exception (fine for sideload) — unchanged, a
  deliberate product tradeoff.

## Solid

Accent color round-trips correctly; move/copy guard against moving a folder into
its own subtree and fall back to copy+delete across volumes; Room migrations are
additive; pure logic is well-tested.
