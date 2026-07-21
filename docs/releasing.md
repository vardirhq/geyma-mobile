# Releasing Geyma Mobile

Stable releases are published from version tags and produce a signed release APK
attached to a GitHub Release.

## Release checklist

1. Update `versionCode` and `versionName` in `app/build.gradle.kts`.
2. Merge the release commit to `main`.
3. Create and push a tag that exactly matches the version name:

```bash
git tag v0.3.1
git push origin v0.3.1
```

The `Stable APK release` workflow validates that the tag matches
`versionName`, runs unit tests and lint, builds the release APK, signs it, uploads
the APK plus a SHA-256 checksum as workflow artifacts, and publishes the GitHub
Release.

## Required repository secrets

Configure these GitHub Actions secrets before publishing stable releases:

| Secret | Value |
| --- | --- |
| `ANDROID_SIGNING_KEY_BASE64` | Base64-encoded release keystore file |
| `ANDROID_KEY_ALIAS` | Key alias inside the keystore |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password |
| `ANDROID_KEY_PASSWORD` | Key password |

Create the base64 value from a local keystore with:

```bash
base64 -w 0 geyma-release.keystore
```

On macOS, use:

```bash
base64 -i geyma-release.keystore | tr -d '\n'
```

Keep the keystore itself out of the repository. If the keystore is lost, Android
will treat future builds as a different app signature.

## Development builds

The existing `Dev APK release` workflow publishes a rolling debug-signed
pre-release at the `dev` tag. Use that for quick testing. Use version tags for
stable APKs that should keep the same installable signature across releases.
