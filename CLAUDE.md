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
    ├── ThemeColorMenu.kt        # Accent theme picker dropdown
    └── theme/
        ├── AccentTheme.kt       # Enum of color themes (Green, Blue, Purple, etc.)
        ├── Color.kt             # Token definitions
        ├── Theme.kt             # MaterialTheme wrapper; respects AccentTheme
        └── Type.kt              # Typography definitions
```

## Key design decisions

- **One entry per day:** `JournalEntry.date` (format `yyyy-MM-dd`) is the primary key; saving overwrites the day.
- **120-character limit:** Enforced in `JournalViewModel` (`MAX_ENTRY_LENGTH = 120`).
- **Backup strategy:** Pull-merge-push via Google Drive `appDataFolder`. Conflict resolution uses `updatedAt` timestamp (latest wins). Auto-push triggers 1.5 s after any save/toggle (`PUSH_DEBOUNCE_MS`).
- **Streak:** Calculated in ViewModel from the sorted entry list; counts consecutive days ending today or yesterday.
- **Theming:** `AccentTheme` enum drives Material3 color scheme; persisted in `SharedPreferences`.
- **Fonts:** `JournalFont` enum (Sans/Serif/Mono/Casual/Condensed); persists across Home, History, Favorites, and the share card image. `toFontFamily()` in `HomeScreen.kt` is `internal` — shared across all screens. Condensed uses `DeviceFontFamilyName("sans-serif-condensed")` — no bundled font files needed.
- **Theme colors:** 9 options in `AccentTheme` enum. `ThemeColorMenu` uses `FlowRow` so colors wrap on smaller screens.

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
