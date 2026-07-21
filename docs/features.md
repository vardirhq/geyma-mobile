# Geyma Mobile feature guide

Geyma is built around a simple idea: file management should preserve context instead
of discarding it. The ordinary file browser is still there, but a local journal makes
each action useful later.

## Journal and provenance

Geyma records file arrivals, moves, renames, opens, stars, trash operations, and
restores. A file keeps its history as its path changes.

- **Arrival provenance** watches common landing folders such as Downloads, camera,
  screenshots, and messaging media. New files are detected live and reconciled when
  Geyma starts.
- **Timeline** groups storage-wide activity by day.
- **Ghost trails** leave a faint marker where a file recently departed, including its
  destination and when it moved.
- **Why is this here?** uses the recorded context to explain a file's current place.

The journal retains up to 200 events per file.

## Dossiers, notes, and revisits

A **Dossier** is a biography for one file. It brings together:

- its current path and arrival source
- previous names and locations
- open count and recent activity
- working-set membership
- a pinned note
- its complete journal timeline

**Revisit** schedules a file to return to the Home screen later. The reminder follows
the file if it moves or is renamed.

**Seals** guard important paths from rename, move, or trash operations. A folder
containing a sealed descendant is protected from destructive actions too.

## Find and on-device OCR

Find searches the journal rather than only the current directory tree. Historical
paths and names remain useful after a file changes.

Geyma can also recognize text inside eligible images. The bundled ML Kit model runs
on-device, stores its searchable index locally, and returns a matching snippet with
the result.

## Sweep and recoverable trash

**Sweep** ranks files by neglect: arrivals that were never opened, oldest first. Files
selected for cleanup move to Geyma's recoverable trash, which records their original
locations for one-tap restore.

**Echoes** detects byte-for-byte duplicates. It keeps the oldest copy in a group and
can sweep redundant copies into the same recoverable trash.

## Working sets

Working sets are playlist-like collections of references. A file does not need to be
duplicated to appear in several sets, and references are rebased when it moves or is
renamed.

- Share files from any Android app into Geyma's inbox and add them to a set.
- Share a set back out as a multi-file bundle.
- Pack a set for offline use.

## Almanac

Almanac summarizes the last 14 days of journal activity:

- files arrived, opened, and neglected
- daily activity sparkline
- busiest folders
- most-handled files

Every item remains actionable: open its dossier or browse to its folder.

## Viewing and archives

Geyma opens supported files without leaving the app:

- images, animated GIFs, and SVGs
- video and audio
- PDF documents
- Markdown and locked-down HTML
- text and source code

ZIP-compatible containers—including ZIP, JAR, and GYSET—can be browsed like folders.
Single entries can be previewed without extraction. **Extract all** validates paths
against ZIP-slip attacks and journals the resulting files.

Unsupported formats fall back to Android's application chooser.

## Continuity

A portable `.geyma` bundle can export stars, working sets, and timeline history. A
bundle can be merged into another phone or handed to desktop Geyma without creating
an account or involving a server.

## Appearance

The eight desktop skins are available on Android using the same design tokens:

- Parchment
- Obsidian
- Phosphor
- Nord
- Amber
- Plasma
- Synthwave
- Paper

Accent, typeface, corner radius, tile style, icon treatment, and backdrop pattern can
be overridden independently.

## Conventional file management

The memory layer sits on top of a normal file-management toolkit:

- internal storage and SD-card roots
- breadcrumbs and folder navigation
- list and grid layouts
- sorting, filtering, and hidden files
- multi-select
- copy, move, rename, share, and trash
- image and video thumbnails

## Current limitations

The following desktop features are not yet available on mobile:

- batch rename
- rule-based smart sets
- SFTP and SMB remotes
- local Ollama features

Non-ZIP archive formats such as TAR, GZ, 7Z, and RAR currently open through an
external compatible app.
