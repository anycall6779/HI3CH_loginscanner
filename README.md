# HONKAI QR — HI3CH Login Scanner

轻量级 / 경량 Android 로그인·QR 도우미. **Development preview, not a production release.**

- Bilibili China channel authentication and PC QR approval flow.
- Encrypted multi-account profiles, local aliases, camera/image QR decoding, minimal local history.
- High-resolution camera input, autofocus, multi-region decoding and optional zoom.
- No in-app GitHub repository link. Developer blog: https://blog.naver.com/rkdrlgh22

## Release status

Source is public. 0.7.0-beta removes all user-log dispatch fixtures and fetches fresh configuration from the original HonkaiScanner service after explicit consent. This is **not direct official dispatch retrieval**. APK publication remains pending clean-install authentication and actual PC B服 login verification. No account data or signing keys are included.

The external service receives the selected game version and fixed protocol compatibility headers, not account IDs, tokens, QR tickets or real device IDs. It sees connection IP/time. It is a single external dependency: outages or incompatible/incorrect responses stop approval, with no fixture fallback. Encrypted config is structurally checked, not independently decrypted/authenticated. See [provider details](DISPATCH.ko.md).

See [Korean guide](README.ko.md), [features](FEATURES.ko.md), [privacy](PRIVACY.ko.md) and [release blockers](RELEASE-BLOCKERS.ko.md). Do not advertise other upstream channels as implemented.

## Build

Requires JDK 17, Android SDK Platform 34 and Build Tools 36.0.0. Run `./build.ps1` in PowerShell. Dependencies are version-pinned and SHA-256 checked. No Gradle installation is required. For maintainer signing use `-SigningKeystore KEYSTORE -SigningAlias honkaiqr` with `HONKAIQR_STORE_PASS` set locally. Never commit/upload the key or password. Release package: `io.anycall6779.honkaiqr`; existing development app data is not imported.

## Attribution

Reference implementation: HonkaiScanner/bh3_login_simulation-memories (MIT). ZXing core 3.5.3 (Apache-2.0). Required notices are retained in `licenses/` and the app assets. This is an unofficial independent implementation, not a vendor product.
