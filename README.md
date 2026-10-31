# One Line Journal

A calm, minimalist journal for shy people: write **one line a day** (up to 120
characters, or a full diary page with page view on) and watch your streak grow. Built with Kotlin, Jetpack Compose and
Room. Journaling works fully offline and without an account; signing in just
adds an automatic Google Drive backup.

**Package:** `com.onelinejournal` · **Min SDK:** 24 (Android 7.0) · **Target SDK:** 36

## Features

### Home
- Time-of-day greeting with your name ("Good morning, Sunny") and today's date.
- A streak card with an animated flame that grows, warms and gains side flames as
  your streak lengthens (full strength at 30 days).
- Your journal card (`<name>'s Journal`) with a live character counter that turns
  amber, then red and gives a gentle shake as you near the limit. The box grows
  with your chosen text size so a full entry fits without scrolling.
- Save/Update with a press animation and haptic feedback, and a favourite heart.
- A month calendar: written days use your accent colour, missed days are soft red,
  today is ringed. Tap a day with an entry to jump to it in History; tap an empty
  past day to see a "No entry" note.
- Unsaved text is kept as a draft on your device, so it comes back if the app is
  closed before you save. Drafts are not backed up.

### Page view (optional)
Turn it on in Settings to show Home as a diary page: lined paper, no character
limit, and the calendar in a pop-up. Swipe right to read earlier days. Past days
are read-only. In History and Favorites, tap an entry to open it on its own screen.

If you turn page view off, a longer entry stays whole. It can be shortened by hand
but can't be saved until it fits in 120 characters.

### History and Favorites
- Entries grouped by month under sticky headers, newest first, with readable dates
  ("Wed, Oct 7").
- Favourite entries (animated heart) and a Favorites tab.
- Share any entry as an image card, rendered in your chosen font.

### Settings
- **Appearance:** 8 accent themes (Green by default), 5 fonts (Sans, Serif, Mono,
  Casual, Condensed) with live previews, a sample line, and a text-size slider.
- **Page view:** switch Home between the one-liner and the diary page.
- **Reminder:** set or turn off the daily reminder time.
- **App lock:** require fingerprint, face or phone PIN to open the app (off by default).
- **Login:** see below.

### Reminders
A daily notification at the time you choose. Tapping it opens the app. On Android
12 and newer, the app may ask you to allow exact alarms so it arrives on time.

### App lock
Turn it on in Settings to ask for your fingerprint, face or phone PIN when opening
the app. It locks on a fresh start and as soon as the app leaves the screen. The
exceptions are screens the app opens itself, such as Google sign-in and the share
sheet, unless you stay away for more than 2 minutes. While it is on, screenshots and the recent-apps preview are
blocked so your entries stay private. If you later remove your phone's screen
lock, the app lock switches itself off so you are never locked out.

### Login and backup
A name is required to log in, and it is used for the greeting and journal title.

- **Login with name:** a local-only profile. Your journal stays on the device.
- **Login with Google:** the same, plus automatic backup to your Google Drive.

With Google, backup is automatic: it runs on sign-in, on app start, and shortly
after every save or favourite change. Entries are merged by last-updated time, so
newer edits win. If a sync fails, it is retried automatically in the background
(waiting for a connection, with growing delays), and a **Retry backup** button
appears for a manual try. If the backup file can't be read, it is kept under another
name and a new backup is saved from your device.

Signing in as a *different* Google account asks first. If you agree, the local
journal is replaced by that account's backup, and anything not already backed up is
deleted. If you cancel, that account is signed out and your journal stays. Either
way, one account's entries are never uploaded to another's Drive.

## Privacy

Entries live in a local Room database on your device. The only data sent anywhere
is the backup file in your own hidden Google Drive App Data folder (scope
`drive.appdata`), and only if you log in with Google. There are no analytics or
ads. See [Privacy_Policy.txt](Privacy_Policy.txt).

## Known limitations

- If Google asks you to sign in again, the background retry can't fix that; sign in
  from Settings.
- Daily reminders stop after the phone restarts, until the app is next opened.
- Only backup parsing and sync have automated tests.
- While the Google project is unverified, only listed test users can log in with
  Google. Everyone else sees an "Account Blocked" screen; login with name still works.

## Design decisions

- **Entries are permanent.** There is no editing or deleting of past entries, on
  purpose: a journal as an honest record, and a reason to write what you mean. The
  Login card says so. If people ask for it, edit/delete can be added behind a
  confirmation (it would need the backup to track deletions, because it currently
  merges).

## Build and run

Requires **JDK 17**. It is pinned in `gradle.properties`
(`org.gradle.java.home`); newer JDKs such as 26 can't build this project. Put
your Android SDK path in `local.properties` (`sdk.dir=...`).

```powershell
.\gradlew.bat assembleDebug     # build a debug APK
.\gradlew.bat installDebug      # install on a connected device or emulator
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

Run the JVM tests with `.\gradlew.bat testDebugUnitTest`. Don't run
`connectedAndroidTest` on a phone with a real journal: it uninstalls the app
afterwards, which deletes the data.

## Google Drive backup setup

Backup needs a one-time OAuth setup. Until it is done, "Login with Google" shows a
configuration error (login with name works regardless).

Configure OAuth once in [Google Cloud Console](https://console.cloud.google.com/):

1. Create or select a project and enable the **Google Drive API**.
2. Configure the OAuth consent screen (External). Add scopes `email`, `profile`,
   and `https://www.googleapis.com/auth/drive.appdata`.
3. Create an OAuth client of type **Web application**. Copy the client ID.
4. Create an OAuth client of type **Android**:
   - Package name: `com.onelinejournal`
   - SHA-1 for **debug** and **release** keystores (see below)
5. Put the **Web** client ID in `local.properties` (gitignored):

```properties
GOOGLE_WEB_CLIENT_ID=1234567890-abc.apps.googleusercontent.com
```

Print the debug SHA-1:

```powershell
& 'C:\Program Files\Java\jdk-17\bin\keytool.exe' -list -v -alias androiddebugkey -keystore "$env:USERPROFILE\.android\debug.keystore" -storepass android -keypass android
```

Use the same `keytool -list -v` command on `release-key.jks` for the Play SHA-1.

Until `GOOGLE_WEB_CLIENT_ID` is set, Sign in shows a configuration error.

## Google Play release

This project builds an Android App Bundle, which Google Play requires for new
apps. The release configuration targets Android 16 / API 36 and supports
Android 7.0 / API 24 and newer.

Create a release keystore once:

```powershell
& 'C:\Program Files\Java\jdk-17\bin\keytool.exe' -genkeypair -v -keystore release-key.jks -alias release -keyalg RSA -keysize 2048 -validity 10000
```

Copy `keystore.properties.example` to `keystore.properties` and replace the
password values. `keystore.properties` and keystore files are ignored by Git.

Raise `versionCode` and `versionName` in `app/build.gradle.kts` for each upload,
because Play rejects a version code it has already seen. Build the Play Store bundle:

```powershell
.\gradlew.bat clean bundleRelease
```

Upload the signed bundle from:

```text
app/build/outputs/bundle/release/app-release.aab
```
