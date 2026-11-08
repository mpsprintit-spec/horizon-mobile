# Horizon Mobile

A native Android application for **one canonical Horizon Brain**.

- Unique Android application ID: `com.valtheron.horizon.mobile`
- Visible name: **Horizon Mobile**
- Chat screen uses Horizon's numeric observation bridge; it does not pretend that
  numeric answer metadata is natural-language text.
- Brain screen embeds the user's previously approved neural topology monitor in a
  local WebView; start the Python Canvas adapter in Termux at `http://127.0.0.1:8897`.
- Memory screen reads bounded canonical inspection counts.
- No account login and no external AI provider is required.
- Horizon Brain remains the sole owner of weights, memories, checkpoints and learning.
- No code changes in the core Horizon repository.

## Local services (on the same Android device)

1. Start the existing Horizon Go runtime with its bridge on `127.0.0.1:8765`.
2. Start the previously delivered Python neural-graph adapter with `python server.py`.
3. Open Horizon Mobile: Chat, Brain, Memory, Settings.

The Android app talks only to local loopback addresses. The Bridge bearer token,
if used, can be entered in the app's Settings page. Never expose the local bridge
to the public internet without proper authentication and transport security.

### Build

Open the repository using Android Studio with a compatible Android SDK and
Gradle 9.4.1. Run `./gradlew :app:assembleDebug` after ensuring the
Gradle wrapper jar is installed. This branch has **not** yet been verified with
an Android SDK build on a physical device.

### License and origin

Android/Compose project setup is adapted from the GPL-3.0 licensed
[GPTMobile](https://github.com/Taewan-P/gpt_mobile) project. The user-facing
brand and operating mode are independently designed for Horizon. Source
attribution and the GPL-3.0 license are preserved in NOTICE.md and LICENSE.
