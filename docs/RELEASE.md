# GitHub Actions release setup

The workflow `.github/workflows/android.yml` runs verification (native tests, Gradle unit tests, lint, debug APK) on pushes, pull requests, and manual dispatch. A tag beginning with `v` additionally enables the signed APK release job. Debug and release APKs are kept as GitHub Actions artifacts for the configured retention period; tagged release also publishes APK + SHA-256 file to GitHub Releases.

## Signing secrets

Create a dedicated Android signing key on a trusted machine and keep an encrypted offline backup. Never commit the keystore or passwords. Example key generation (choose and securely record the passwords; key renewal can break update compatibility):

```sh
keytool -genkeypair -v \
  -keystore adamant-release.jks \
  -alias adamant-release \
  -keyalg RSA -keysize 3072 -validity 10000
```

Create GitHub repository Actions secrets:

- `KEYSTORE_BASE64`: base64-encoded keystore contents.
- `KEYSTORE_PASSWORD`: store password.
- `KEY_ALIAS`: key alias.
- `KEY_PASSWORD`: key password.

Linux base64 encoding without line breaks:

```sh
base64 -w 0 adamant-release.jks
```

On macOS, use `base64 < adamant-release.jks | tr -d '\n'`. Paste only the output into `KEYSTORE_BASE64`; never paste the keystore or secret values into chat, issues, or logs.

The tag release job validates all four secrets before decoding to the runner temporary directory, passes values to Gradle as environment variables, sets the visible version name from the tag and the monotonically increasing Android `versionCode` from the workflow run number, verifies the APK signature with `apksigner`, and generates `SHA256SUMS.txt`. Missing signing secrets intentionally fail a tagged release instead of publishing an unsigned APK. Normal debug CI does not require signing secrets.

`GRADLE_ENCRYPTION_KEY` is **not required**: the workflow uses the Gradle Actions local dependency/cache support and does not configure a remote encrypted Gradle configuration cache.

## Release checklist

1. Confirm verification CI is green on the exact commit.
2. Review `docs/BENCHMARK.md`; do not add claims without attached run CSV/log.
3. Add release notes and verify the app version/tag.
4. Confirm the four signing secrets exist in repository Actions settings.
5. Create a version tag according to the repository's release policy; do not tag an unreviewed commit.
6. Confirm the `release` job succeeds, signature verification passes, and checksum matches the downloaded APK.
7. Keep the signing key and its backup protected; restrict who can create release tags. Consider GitHub environment protection and immutable action SHA pinning for a production repository.

This workflow is not a Play Store publishing pipeline: it does not upload to Google Play, create an AAB, configure Play App Signing, or claim production certification.
