# Screenshot and visual asset guide

The README hero is marketing artwork. Product screenshots should always come from a
real Geyma build so the repository never advertises UI that does not exist.

## Recommended screenshot set

Capture these five views with representative, non-personal sample files:

1. **Home** — recent activity, arrivals, and the main feature shortcuts.
2. **Files** — a folder in grid view with several file kinds visible.
3. **Dossier** — provenance, note, working sets, and timeline for one sample file.
4. **Almanac** — populated 14-day activity, busiest folders, and handled files.
5. **Themes** — the same screen in Parchment, Obsidian, and one expressive skin.

Optional secondary captures: Find with an OCR result, Sweep before confirmation,
Echoes with a duplicate group, and a ZIP archive opened as a folder.

## Capture rules

- Use the current development APK and verify its version in Settings › About.
- Use a modern phone viewport without display scaling or font-size overrides.
- Populate a dedicated sample storage tree; never expose real filenames, contacts,
  account names, notifications, or location data.
- Keep system bars consistent across the set.
- Capture PNG at native resolution. Do not JPEG-compress UI screenshots.
- Do not add device frames to the source screenshots. Marketing composites can add
  frames later while the originals remain reusable.

## Repository layout

```text
docs/assets/
├── geyma-mobile-hero.png
└── screenshots/
    ├── home.png
    ├── files.png
    ├── dossier.png
    ├── almanac.png
    └── themes.png
```

Once the real captures exist, add a compact three-image gallery near the top of the
README and link it to the feature guide. Keep the remaining screenshots in this guide
or a dedicated product tour so the README stays persuasive rather than exhaustive.
