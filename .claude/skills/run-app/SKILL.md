---
name: run-app
description: Build and install One Line Journal on a connected Android phone or emulator. Use when asked to run, launch, install, or deploy the app, or to confirm a change works on a real device. Covers the JDK 17 pin, Android SDK paths, and adb install.
---

# Running One Line Journal

Native Android app (Kotlin + Compose). There is no dev server and no hot reload — you build an APK and install it on a device.

## Hard requirements (already configured — do not "fix" these)

**JDK 17, not the system JDK.** The machine's default is JDK `26.0.2.1`, whose 4-part version string crashes Kotlin's `JavaVersion.parse` with `IllegalArgumentException: 26.0.2.1` during `:app:compileDebugKotlin`. Azul Zulu 17 is pinned in `gradle.properties`:

```properties
org.gradle.java.home=C:/Program Files/Zulu/zulu-17
```

If you see that `IllegalArgumentException`, the pin was lost — restore it rather than upgrading Kotlin. Upgrading Kotlin does **not** fix it (tried 2.2.0; same crash).

**Android SDK path** lives in `local.properties` (gitignored, so absent on a fresh clone):

```properties
sdk.dir=C\:/Users/sunny/AppData/Local/Android/Sdk
```

## Tool paths

These are winget install locations and are not on `PATH` in a fresh shell:

```bash
ADB="C:/Users/sunny/AppData/Local/Microsoft/WinGet/Packages/Google.PlatformTools_Microsoft.Winget.Source_8wekyb3d8bbwe/platform-tools/adb.exe"
ANDROID_CLI="C:/Users/sunny/AppData/Local/Microsoft/WinGet/Packages/Google.AndroidCLI_Microsoft.Winget.Source_8wekyb3d8bbwe/android.exe"
```

## Build

```bash
./gradlew assembleDebug
```

APK lands at `app/build/outputs/apk/debug/app-debug.apk`. First build is ~2.5 min; incremental ~30 s.

## Install and launch

Check for a device first — this returns an empty list if the phone isn't connected or USB debugging is off:

```bash
"$ADB" devices
```

If empty, the user must: enable Developer Options (Settings → About Phone → tap Build Number 7x), turn on **USB Debugging**, plug in via USB, and tap **Allow** on the phone. Ask them to do this; you cannot do it for them.

With a device attached:

```bash
./gradlew installDebug                           # build + install in one step
"$ADB" shell am start -n com.onelinejournal/.MainActivity
```

## Driving the app

Screenshot to confirm a change actually rendered — a blank or unchanged frame means it didn't:

```bash
"$ADB" exec-out screencap -p > /tmp/screen.png
```

Navigate with the bottom bar: Home / History / Favorites / Settings. Font and theme changes are made in **Settings** and should be verified on **History**, **Favorites**, and a shared card — those are the screens that have regressed before.

## If the SDK is missing entirely

`sdkmanager` is not installed; use the newer Android CLI instead:

```bash
"$ANDROID_CLI" sdk install "platforms;android-36"
"$ANDROID_CLI" sdk install "build-tools;36.0.0"
"$ANDROID_CLI" info     # prints the resolved SDK path
```
