# One_Line_Journal
A Journal app for shy people !!

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
