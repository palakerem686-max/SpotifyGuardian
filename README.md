# Spotify Guardian (prototype 0.1.0)

A small Android Studio project for Android 8+ (intended to test on OPPO A16 / Android 11).

## What it does
- Runs a foreground service with an ongoing notification.
- Uses Android's user-granted Notification Listener access to inspect active media sessions locally.
- If Spotify was playing and its media session disappears for at least 20 seconds, shows a notification that opens Spotify when tapped.
- Does not send data anywhere.

## Important limitations
- This app cannot prevent ColorOS/Oppo's privileged `kill_process_when_silent` / `o-kill:59` decision.
- Android does not reliably permit a background app to launch another app's activity automatically. Recovery is therefore user-tap based.
- It cannot reliably distinguish every intentional stop from a process kill; a missing media session is only a signal.
- The app itself may also be stopped by ColorOS. Keep its ongoing notification enabled and exempt it from battery optimization if the phone offers that option.

## Build in Android Studio
1. Install Android Studio on a computer and its Android SDK platform 35 / Build Tools.
2. Open this folder (`SpotifyGuardian`) as an existing project.
3. Let Gradle sync and build the debug APK (`Build > Build Bundle(s) / APK(s) > Build APK(s)`).
4. Copy the generated APK from `app/build/outputs/apk/debug/app-debug.apk` to the phone and install it. You may need to allow installation from that file manager.
5. Open Spotify Guardian. Tap `1. Medya/bildirim erişimini aç`, find Spotify Guardian in the settings list and enable access.
6. Return to the app and tap `2. Koruyucuyu başlat`.
7. Leave the persistent notification enabled. Test by playing Spotify and switching to YouTube Shorts.

This source project has not been compiled or device-tested in this environment; it is a prototype, not a ready-to-install APK.
