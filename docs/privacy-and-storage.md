# Privacy and storage access

Geyma is a local-first file manager. Its product model does not depend on an account,
remote database, telemetry service, or advertising network.

## What stays on the device

Geyma stores the following locally:

- file journal events and historical paths
- stars and working-set references
- recoverable-trash records
- the seen-file ledger used for arrival detection
- revisits, notes, and sealed paths
- text recognized inside eligible images
- appearance and file-browser preferences

Structured records live in a Room database. Preferences live in Android DataStore.
The recoverable trash directory is `.geyma/trash` on primary storage.

## Network behavior

Geyma does not require a backend and does not upload files or metadata. OCR uses the
bundled on-device Latin text-recognition model. HTML previews disable JavaScript,
file access, and network access.

Data leaves the app only when you deliberately use an Android share action or export
a `.geyma` continuity bundle.

## Why broad storage access is required

Geyma is a full file manager rather than a picker for one application folder. On
Android 11 and newer it therefore requests the system's **All files access** special
permission (`MANAGE_EXTERNAL_STORAGE`). On Android 8–10 it uses the legacy read and
write storage permissions.

The permission enables Geyma to:

- browse internal storage and removable cards
- move, rename, copy, trash, and restore selected files
- detect arrivals in common landing folders
- keep journal references synchronized when paths change

Android presents the Android 11+ permission in a system settings screen. If access is
not granted, Geyma cannot operate as a full file manager.

## Exports and continuity bundles

Continuity bundles are explicit, portable exports. Review where you save or share a
bundle: it contains the Geyma history and organization data selected for continuity,
even though it does not upload itself anywhere.
