# One Line Journal — Android App

## Project overview

A minimalist Android journaling app where users write one entry per day (max 120 characters). Built with Kotlin, Jetpack Compose, Room, and Google Drive backup.

**Package:** `com.onelinejournal`  
**Min SDK:** 24 (Android 7.0) | **Target SDK:** 36  
**Language:** Kotlin | **UI:** Jetpack Compose + Material3

## Build & run

```bash
# Debug build
./gradlew assembleDebug

# Release build (requires keystore.properties — see keystore.properties.example)
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug
```

## Project structure

```
app/src/main/java/com/onelinejournal/
├── MainActivity.kt              # Entry point — sets up Google Sign-In, ViewModel
├── ReminderReceiver.kt          # BroadcastReceiver for daily reminder notifications
├── ReminderScheduler.kt         # Schedules AlarmManager-based reminders
├── auth/
│   └── GoogleAccountSession.kt  # Persists Google sign-in email & access token
├── backup/
│   ├── DriveAppDataClient.kt    # OkHttp calls to Google Drive appDataFolder
│   ├── JournalBackupJson.kt     # JSON serialization for backup file
│   └── JournalBackupRepository.kt # pull-merge-push logic; merges by updatedAt
├── data/
│   ├── JournalEntry.kt          # Room entity: date (PK), content, updatedAt, isFavorite
│   ├── JournalEntryDao.kt       # Room DAO
│   ├── JournalDatabase.kt       # RoomDatabase singleton
│   └── JournalRepository.kt    # Data layer abstraction over DAO
└── ui/
    ├── JournalApp.kt            # NavHost with bottom navigation (Home/History/Favorites/Settings)
    ├── JournalViewModel.kt      # Single ViewModel for all screens; StateFlow-based UI state
    ├── HomeScreen.kt            # Today's entry input + streak counter
    ├── HistoryScreen.kt         # All entries by month
    ├── SettingsScreen.kt        # Theme, font, reminder, Google backup controls
    ├── ShareCard.kt             # Shareable image card from an entry
    ├── ThemeColorMenu.kt        # Theme swatch row + font card row (JournalFontPicker)
    ├── StreakFlame.kt           # Animated streak fire on Home
    └── theme/
        ├── AccentTheme.kt       # Enum of color themes (Green, Blue, Purple, etc.)
        ├── Color.kt             # Token definitions
        ├── Theme.kt             # MaterialTheme wrapper; respects AccentTheme
        └── Type.kt              # Typography definitions
```

## Key design decisions

- **One entry per day:** `JournalEntry.date` (format `yyyy-MM-dd`) is the primary key; saving overwrites the day.
- **120-character limit:** Enforced in `JournalViewModel` (`MAX_ENTRY_LENGTH = 120`).
- **Backup strategy:** Pull-merge-push via Google Drive `appDataFolder`. Conflict resolution uses `updatedAt` (latest wins). Backup is automatic — a push fires 1.5 s after any save/toggle (`PUSH_DEBOUNCE_MS`), on app start, and on sign-in. The always-visible `Backup now` button was removed; Settings now shows a `Retry backup` button only after a failed sync (`BackupSyncState.Error`), since failed pushes are not auto-retried (see Known gaps).
- **Account isolation:** `journal_owner_email` in `SharedPreferences` records which account local entries belong to. Signing in as a *different* account calls `pullReplace` (cloud wins, local discarded) instead of `pullMergePush`, so one account's entries are never uploaded into another's Drive. Guest → first sign-in still merges, which is the intended migration path. `DriveAppDataClient.cachedFileId` must be reset on account change — the file id is per-account and resolves to an unreadable file under a new token.
- **Streak:** Calculated in ViewModel from the sorted entry list; counts consecutive days ending today or yesterday.
- **Theming:** `AccentTheme` enum drives Material3 color scheme; persisted in `SharedPreferences`.
- **Fonts:** `JournalFont` enum (Sans/Serif/Mono/Casual/Condensed); persists across Home, History, Favorites, and the share card image. `toFontFamily()` in `HomeScreen.kt` is `internal` — shared across all screens. Condensed uses `DeviceFontFamilyName("sans-serif-condensed")` — no bundled font files needed.
- **Theme colors:** 8 options in `AccentTheme` enum (Amber was removed as a theme; `WarningAmber` in `Color.kt` is the non-selectable warning tint used by the streak glow and character counter). `ThemeColorMenu` is a 4-column grid of labelled swatches; `JournalFontPicker` (same file) is a 2-column grid of font preview cards. A persisted `Amber` theme name falls back to Green via `AccentTheme.fromName`.
- **Login & name:** Settings has a `Login` card (replaces "Google backup"). A name (`user_name` in `SharedPreferences`, max 20 chars) is required for both `Login with name` (local-only guest) and `Login with Google` (backup as before). Name drives the Home greeting (`Good morning, <name>`, by hour) and the editor title (`<name>'s Journal`, fallback `My Journal`). Logging out of a name-only session clears the name; signing out of Google keeps it.
- **Streak flame:** `StreakFlame.kt` draws a canvas flame that grows, warms and gains side flames as the streak rises (full strength at 30 days). It uses a slow `infiniteTransition` flicker — a deliberate exception to the UI skill's no-looping rule.
- **Editor box:** `JournalEditorCard` sizes the text field to 3-6 lines from the text size so a full 120-char entry shows without inner scrolling.
- **Settings order:** Appearance (theme, font, sample, text size), Reminder, then Login last — compact enough to show without scrolling.

## Secrets & signing

Copy `keystore.properties.example` → `keystore.properties` and fill in:

```properties
RELEASE_STORE_FILE=path/to/keystore.jks
RELEASE_STORE_PASSWORD=...
RELEASE_KEY_ALIAS=...
RELEASE_KEY_PASSWORD=...
GOOGLE_WEB_CLIENT_ID=...apps.googleusercontent.com
```

`GOOGLE_WEB_CLIENT_ID` can also be set in `local.properties` or as an environment variable. It becomes the `default_web_client_id` string resource used by Google Sign-In.

## Google Sign-In setup

Requires **two** OAuth clients in the same Google Cloud project. Mixing them up is the main failure mode:

| Client type | Purpose | Referenced in code? |
|---|---|---|
| **Web application** | The "server client ID" passed to `GetSignInWithGoogleOption.Builder()`. Goes in `local.properties` as `GOOGLE_WEB_CLIENT_ID`. | Yes — as `default_web_client_id` |
| **Android** | Validates that a request claiming to be `com.onelinejournal`, signed with the registered key, is genuine. | No — server-side only |

Android client needs package `com.onelinejournal` plus the SHA-1 of the signing key. For debug builds, read it from the APK itself rather than guessing at a keystore:

```bash
apksigner verify --print-certs app/build/outputs/apk/debug/app-debug.apk
```

Symptoms and causes:

- **`[28444] Developer console is not set up correctly`** — an *Android* client ID was put in `local.properties` instead of the Web one, or no Android client exists, or the SHA-1 / package does not match the installed APK.
- **"has not completed the Google verification process"** — `drive.appdata` is a sensitive scope, so while the project is unverified only listed **test users** may sign in. Add the account under OAuth consent screen → Audience → Test users; being the project owner does not exempt you. Publishing status must be *Testing*, not unverified *In production*. The "unverified app" warning screen is expected and permanent — proceed via Advanced.

Changing the Android client or test users needs no rebuild. Changing `GOOGLE_WEB_CLIENT_ID` does, since it is compiled into the APK. Verify it landed:

```bash
cat app/build/generated/res/resValues/debug/values/gradleResValues.xml
```

## Known gaps

- **No sync retry.** A failed push is caught in `JournalViewModel.runBackup` and dropped; nothing retries until app restart or `Backup now`. Entries stay safe in Room, but can sit un-uploaded. Proper fix is a `syncStatus` column plus WorkManager.
- **Backup errors are never logged.** `runBackup` swallows exceptions into UI state only, so sync failures cannot be diagnosed from logcat. Log the failure reason — but never journal text.
- **No tests.** There are no `test`/`androidTest` source sets at all.

## Permissions

- `INTERNET` — Google Drive backup
- `POST_NOTIFICATIONS` — daily reminder notifications (Android 13+)

## Screens

| Route | Screen | Purpose |
|---|---|---|
| `home` | HomeScreen | Write today's entry, see streak |
| `history` | HistoryScreen | Browse all past entries |
| `favorites` | FavoritesScreen | Entries marked as favorite |
| `settings` | SettingsScreen | Theme, font size, reminder time, Google Drive backup |

## Dependencies (notable)

| Library | Use |
|---|---|
| Room 2.6.1 | Local SQLite journal storage |
| Jetpack Compose BOM 2024.09.03 | UI |
| Navigation Compose 2.8.2 | Screen routing |
| Credentials / GoogleId 1.1.1 | Google Sign-In |
| OkHttp 4.12.0 | Drive API calls |
| Coroutines (Play Services) 1.9.0 | `await()` on Tasks |

## Build environment

- **JDK:** Azul Zulu 17 (`C:/Program Files/Zulu/zulu-17`) — pinned via `org.gradle.java.home` in `gradle.properties`. JDK 26 cannot build this project (Kotlin's `JavaVersion.parse` rejects the 4-part `26.0.2.1` version string).
- **Android SDK:** `C:/Users/sunny/AppData/Local/Android/Sdk` — path set in `local.properties` (not committed). Installed via Google Android CLI (`winget install Google.AndroidCLI`).
- **Kotlin:** 2.0.20 | **KSP:** 2.0.20-1.0.24 | **AGP:** 8.13.2 | **Gradle:** 8.13

## Conventions

- All UI state flows through `JournalUiState` in `JournalViewModel`.
- No Hilt/DI framework — dependencies are manually wired in `MainActivity`.
- KSP is used for Room annotation processing (not kapt).
- `isCoreLibraryDesugaringEnabled = true` for `java.time` APIs on API < 26.
