# One_Line_Journal
A Journal app for shy people !!

## Google Drive backup

Journaling works without an account. Optional Google Sign-In stores a JSON
snapshot of entries in that account’s hidden Drive **App Data** folder. Signing
in on a new install with the same account restores and merges the journal.

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

Build the Play Store bundle:

```powershell
.\gradlew.bat clean bundleRelease
```

Upload the signed bundle from:

```text
app/build/outputs/bundle/release/app-release.aab
```
