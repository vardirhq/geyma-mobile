<p align="center">
  <img src="docs/assets/geyma-mobile-hero.png" alt="Geyma Mobile — the file manager that remembers" width="100%" />
</p>

<p align="center">
  <a href="https://github.com/vardirhq/geyma-mobile/actions/workflows/android.yml"><img alt="Android build" src="https://img.shields.io/github/actions/workflow/status/vardirhq/geyma-mobile/android.yml?branch=main&style=flat-square&label=build" /></a>
  <img alt="Android 8 or newer" src="https://img.shields.io/badge/Android-8%2B-2C6E49?style=flat-square&logo=android&logoColor=white" />
  <img alt="Kotlin and Jetpack Compose" src="https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7A4B8C?style=flat-square&logo=kotlin&logoColor=white" />
  <img alt="Private and local-first" src="https://img.shields.io/badge/privacy-local--first-B4562E?style=flat-square" />
</p>

Geyma is an Android file manager with a memory. It remembers when files arrive, where
they move, what they were called, and whether you ever returned to them—so your file
system becomes a useful history instead of an unexplained pile of folders.

It is native, local-first, and deliberately account-free. Your journal, search index,
working sets, notes, and preferences stay on your device.

## More than a file browser

| Remember | Rediscover | Clean up safely |
| --- | --- | --- |
| Every arrival, move, rename, open, star, trash, and restore becomes part of a file's story. | Search the journal—including text found inside images—and find files even after they moved. | Surface neglected files and duplicate echoes, then sweep them into recoverable trash. |

### A biography for every file

Open a **Dossier** to see where a file came from, everywhere it has been, how often it
was opened, the sets that reference it, your pinned note, and its complete timeline.

### History that stays useful

The **Timeline** shows activity across storage. **Ghost trails** explain where recently
moved files went. **Almanac** turns the journal into a two-week view of arrivals,
neglect, busy folders, and frequently handled files.

### Organization without duplication

**Working sets** behave like playlists: they reference files without copying them and
follow those files through moves and renames. Share files from another Android app
directly into a set, or pack a set for offline use.

## Built for the device in your hand

- Browse internal storage and SD cards in list or grid view
- Preview images, GIFs, SVGs, video, audio, PDFs, Markdown, HTML, text, and code
- Browse ZIP-compatible archives and extract them safely
- Find duplicates by content with **Echoes**
- Resurface forgotten files later with **Revisit**
- Protect important paths with **Seals**
- Carry history between devices with portable `.geyma` continuity bundles
- Choose from eight skins shared with the desktop Geyma app

[Explore every feature →](docs/features.md)

## Privacy by design

Geyma has no account, telemetry, advertising, or cloud service. Image text recognition
runs on-device. Nothing leaves your phone unless you explicitly share a file or export
a continuity bundle.

Because it is a full file manager, Geyma requires broad storage access. The permission
is used to browse and manage the files you choose—not to upload or profile them.

[Read the privacy and storage model →](docs/privacy-and-storage.md)

## Try the development build

Geyma Mobile is under active development. The rolling `dev` release contains the
latest debug APK for hands-on testing:

**[Download the latest development APK](https://github.com/vardirhq/geyma-mobile/releases/tag/dev)**

Debug builds use an ephemeral signing key, so Android may require you to uninstall an
older development build before installing a newer one.

## Documentation

- [Feature guide](docs/features.md)
- [Privacy and storage access](docs/privacy-and-storage.md)
- [Build and development guide](docs/building.md)
- [Screenshot and visual asset guide](docs/visual-assets.md)
- [Desktop Geyma](https://github.com/MadsenDev/geyma-file-manager)

## Project status

Current app version: **0.3.1**. Core browsing, journaling, search, cleanup, dossiers,
working sets, continuity, in-app previews, and ZIP browsing are implemented. Batch
rename, smart sets, remote storage, and desktop-local AI features are not yet ported.

Geyma means *“to keep, to guard”* in Old Norse.
