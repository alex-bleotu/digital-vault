# Digital Vault

An Android app for self-discipline. It blocks the apps and feeds you choose, and keeps the rules in place with a password that only you know.

## Features

- Block whole apps, or only specific parts of them, such as Instagram Reels and Explore, TikTok, YouTube Shorts and Chrome incognito tabs.
- Block domains with a local DNS filter that runs through Android's VPN service.
- Protect the rules with a vault password. Changing a rule or uninstalling the app asks for it first.
- Show a block screen over a blocked app instead of leaving it open.
- Recover after a reboot or a killed process with a boot receiver and a watchdog worker.
- Update itself from the latest GitHub release.

The accessibility service only watches the apps you choose to block. It does not read your messages or log your activity.

## Install

Download the APK from the [latest release](https://github.com/alex-bleotu/digital-vault/releases/latest) and open it on an Android 8.0 (API 26) or newer device. The setup flow asks for the permissions it needs: accessibility, overlay, VPN, notifications and device admin.

## Build

You need JDK 21 and the Android SDK.

```
./gradlew :app:assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug`.

## Release

Pushing a tag that starts with `v` runs `.github/workflows/release.yml`. It builds a signed release APK and attaches it to a GitHub release. The workflow needs these repository secrets: `RELEASE_KEYSTORE_BASE64`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS` and `RELEASE_KEY_PASSWORD`.

## Built with

Kotlin, Jetpack Compose with Material 3, DataStore, Coil and the Android accessibility, VPN and device admin APIs.
