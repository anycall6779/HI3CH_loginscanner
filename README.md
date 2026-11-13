# HONKAI QR — HI3CH Login Scanner

轻量级 / 경량 Android 로그인·QR 도우미. **Development preview, not a production release.**

- Bilibili China channel authentication and PC QR approval flow.
- Encrypted multi-account profiles, local aliases, camera/image QR decoding, minimal local history.
- High-resolution camera input, autofocus, multi-region decoding and optional zoom.
## Release status

Source is public. APK releases are intentionally withheld until independent official dispatch retrieval and a clean end-to-end login are verified. User-derived dispatch fixtures, account files, logs and signing keys are **not included**. The current source builds, but PC approval stops without a verified dispatch provider.

See [Korean guide](README.ko.md), [features](FEATURES.ko.md), [privacy](PRIVACY.ko.md) and [release blockers](RELEASE-BLOCKERS.ko.md). Do not advertise other upstream channels as implemented.

## Build

Requires JDK 17, Android SDK Platform 34 and Build Tools 36.0.0. Run `./build.ps1` in PowerShell. Dependencies are version-pinned and SHA-256 checked. No Gradle installation is required.

## Attribution

Reference implementation: HonkaiScanner/bh3_login_simulation-memories (MIT). ZXing core 3.5.3 (Apache-2.0). Required notices are retained in `licenses/` and the app assets. This is an unofficial independent implementation, not a vendor product.
