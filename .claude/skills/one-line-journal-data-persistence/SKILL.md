---
name: one-line-journal-data-persistence
description: >
  Design, implement, review, and test production-ready data persistence,
  backup, authentication, synchronization, and recovery for the One Line
  Journal Android app. Use when working on Room persistence, guest/local
  storage, Android Auto Backup, Google Sign-In, Firebase Authentication,
  cloud journal storage, offline sync, guest-to-account migration, reinstall
  recovery, new-device recovery, conflict resolution, sign-out behavior,
  database migrations, or related persistence QA. Preserve the app's
  local-first behavior and existing journal functionality.
---

# One Line Journal — Data Persistence & Recovery Skill

## Purpose

This skill governs how to design, implement, review, and test data persistence
and recovery for the One Line Journal Android application.

The primary product requirement is:

> A user's journal must not be unnecessarily lost, while the app remains fully
> usable without requiring an account.

The architecture must be **local-first**.

Room is the app's local working database. Authenticated cloud storage provides
durable recovery and cross-device synchronization. Android Auto Backup is an
additional OS-level recovery mechanism, particularly useful for guest users,
but must never be represented as a guaranteed substitute for cloud persistence.

---

## When to activate

Activate this skill when the user asks to:

- add or change journal backup/recovery
- decide where journal data should persist
- implement Google Sign-In for the journal
- add Firebase Authentication or cloud journal storage
- synchronize Room with a cloud backend
- support offline-first journal behavior
- recover data after uninstall/reinstall
- recover data on a new device
- migrate guest journal data into an authenticated account
- handle sign-out/account switching
- implement or review Room migrations related to journal persistence
- test persistence, synchronization, backup, restore, or recovery
- review the production readiness of the journal's data architecture
- diagnose journal data loss or synchronization bugs

Do not activate merely for unrelated UI, styling, typography, or generic Android
questions.

---

# 1. Core architecture

Use this conceptual architecture unless inspection of the existing project
demonstrates a strong reason to adapt it:

    Compose UI
        ↓
    ViewModel
        ↓
    Repository
        ↓
    Room DB
        ↓
    SyncManager
        ↓
    Cloud datastore
        ↑
    Authentication

Room is the immediate source for UI reads.

Cloud synchronization must not be implemented directly inside Compose UI.

The UI must not contain Firebase/Firestore synchronization logic.

The Repository should provide the application's data-access abstraction.

The SyncManager should own synchronization state, retries, conflict handling,
and cloud/local reconciliation.

---

# 2. Inspect before modifying

Before changing code, inspect the existing project.

Identify:

1. Current Room entities and DAOs.
2. Room database version and migrations.
3. Repository classes.
4. ViewModels.
5. Compose screens and state flow.
6. DataStore/preferences implementation.
7. Existing backup configuration.
8. Gradle/Kotlin/Compose versions.
9. Existing Firebase dependencies and configuration.
10. Existing authentication implementation, if any.
11. Existing journal/streak/favorite logic.
12. Existing tests.

Do not replace working architecture merely for stylistic reasons.

Prefer incremental, minimal changes.

Do not invent project files or APIs that have not been inspected.

---

# 3. User states

Support two primary product states.

## State A — Guest

The user has not authenticated with One Line Journal.

The app must:

- work without an account
- work offline
- store journal data in Room
- retain existing journal functionality
- not force sign-in
- optionally use Android Auto Backup for supported recovery flows

The user should understand that guest data is primarily stored locally.

Recommended UX wording:

"Your journal is stored on this device."

Offer an unobtrusive path such as:

"Protect your journal"

with:

"Sign in with Google to keep your journal recoverable across devices."

Do not use fear-based messaging or repeatedly interrupt the user with login prompts.

---

## State B — Authenticated

The user signs in with Google.

Conceptual flow:

    Google Sign-In
          ↓
    Firebase Authentication
          ↓
       user UID
          ↓
    Cloud journal data
          ↕
       SyncManager
          ↕
        Room DB

The user should continue to have full offline functionality.

Cloud persistence provides durable recovery and cross-device support.

---

# 4. Room remains the local working database

Journal UI reads should normally come from Room.

Use observable Room data, such as Flow, where appropriate.

Writes should first persist locally so the UI remains responsive and the app
works offline.

Recommended write flow:

    User saves entry
        ↓
    Room transaction
        ↓
    Mark record pending synchronization
        ↓
    UI updates immediately
        ↓
    SyncManager synchronizes when possible

Do not make a network request a prerequisite for saving a journal entry.

---

# 5. Journal data model

Preserve the existing one-entry-per-day rule.

A journal entry should contain at minimum:

- journalDate
- text
- isFavorite
- createdAt
- updatedAt

Synchronization metadata may include:

- syncStatus
- lastSyncedAt
- version
- deviceId

Only add metadata that is actually required by the implementation.

The logical identity of an authenticated journal entry should be based on:

    userId + journalDate

For guest users, use an appropriate local installation identity if needed.

Do not change the existing product rule:

- one entry per calendar day
- today's entry is editable
- previous dates remain locked
- Save creates a new entry
- Update modifies today's existing entry
- streaks remain correctly calculated

---

# 6. Guest data persistence

For guest users:

    Room
      ↓
    Android Auto Backup where supported

Android Auto Backup should be treated as an OS-level safety net.

Do not tell users that guest data is guaranteed to survive every uninstall,
device replacement, backup configuration, or restore scenario.

The implementation must explicitly document the limitations of Android backup.

Back up appropriate user-generated data.

Do not back up:

- authentication tokens
- secrets
- API keys
- temporary cache
- network cache
- transient synchronization state
- unnecessary generated files

Review the project's existing backup configuration before modifying it.

Avoid conflicting backup mechanisms.

---

# 7. Authenticated cloud persistence

Use Google Sign-In with Firebase Authentication when Firebase is appropriate
for the existing project.

Use a cloud datastore such as Cloud Firestore unless inspection of the project
shows a strong technical reason to use another backend.

Recommended conceptual structure:

    users/{userId}/journals/{journalDate}

Journal documents should contain only necessary user data, such as:

- date
- text
- isFavorite
- createdAt
- updatedAt

Do not store authentication credentials in journal documents.

Do not store unnecessary analytics or device information alongside journal text.

---

# 8. Security

Cloud authorization must be based on the authenticated Firebase UID.

Security rules must ensure:

    authenticated UID == requested journal owner UID

A client-supplied userId must never be treated as sufficient authorization.

A user must never be able to read or write another user's journal.

Do not store passwords.

Do not log journal text in production logs.

Do not send journal text as analytics events.

Do not include journal text in crash reports.

---

# 9. Guest → authenticated migration

This is a critical workflow.

If a guest has existing local journal entries and then signs in:

1. Authenticate the user.
2. Obtain the authenticated UID.
3. Read existing local Room data.
4. Identify existing cloud records, if any.
5. Reconcile local and cloud data.
6. Upload/migrate local records as appropriate.
7. Preserve:
   - journal dates
   - text
   - favorite state
   - createdAt
   - updatedAt
8. Avoid duplicate entries.
9. Mark successfully synchronized records as synced.
10. Only mark migration complete after successful synchronization.
11. If synchronization fails, retain local data and retry later.

Never create an empty cloud account while silently abandoning the user's
existing local journal.

Never delete local journal data simply because authentication succeeded.

The migration must be idempotent. Re-running it must not create duplicate data.

---

# 10. Synchronization

The SyncManager should support:

- pending local changes
- upload
- download
- retry
- connectivity recovery
- duplicate prevention
- conflict resolution
- synchronization state

Recommended model:

    Room
      ↕
    SyncManager
      ↕
    Cloud

Room should remain the local UI source.

Cloud changes should be reconciled into Room.

Avoid synchronization loops.

A successfully synchronized record should not continuously trigger another
identical synchronization operation.

---

# 11. Offline behavior

The application must remain fully functional offline.

Test at minimum:

- create entry while offline
- edit today's entry while offline
- favorite/unfavorite while offline
- close/reopen while offline
- restart device while offline
- reconnect
- synchronize pending changes
- retry after synchronization failure

No journal entry should be lost merely because the device temporarily has no
network connectivity.

---

# 12. Conflict resolution

Keep conflict handling intentionally simple.

Use:

    Last-write-wins based on updatedAt

Rules:

- Never overwrite a newer local change with an older cloud version.
- Never overwrite a newer cloud change with an older local version.
- Prevent synchronization loops.
- Ensure timestamp handling is consistent.
- Make conflict behavior deterministic.

Do not introduce CRDTs or complex merge algorithms unless the product
requirements genuinely require them.

---

# 13. Reinstall recovery

## Guest user

Expected conceptual flow:

    Uninstall
        ↓
    Room may be removed
        ↓
    Android restore, if applicable
        ↓
    App starts
        ↓
    Restored local data becomes available

The implementation must not assume that Android Auto Backup will restore data
in every possible scenario.

## Authenticated user

Expected flow:

    Uninstall
        ↓
    Reinstall
        ↓
    Authenticate with Google
        ↓
    Obtain UID
        ↓
    Download cloud journal
        ↓
    Populate Room
        ↓
    Recalculate derived state
        ↓
    Display journal

Cloud data must remain the durable recovery mechanism for authenticated users.

---

# 14. New-device recovery

Test this complete scenario:

Device A:

1. Install app.
2. Create journal entries.
3. Sign in.
4. Synchronize.
5. Verify cloud data.

Device B:

1. Install app.
2. Sign in using the same Google account.
3. Obtain the same authenticated UID.
4. Download journal data.
5. Populate Room.
6. Verify:
   - history
   - dates
   - text
   - favorites
   - streak calculation

Do not rely solely on Android Auto Backup for authenticated-user recovery.

---

# 15. Sign-out

Before sign-out:

1. Attempt to synchronize pending changes.
2. If synchronization fails, clearly communicate the state.
3. Do not silently delete journal data.

After sign-out:

- preserve local data unless there is an explicit product reason not to
- prevent accidental exposure between accounts
- clear or isolate account-specific synchronization state
- ensure the next authenticated user cannot see the previous user's cloud data

If multiple accounts are supported, account isolation must be explicit.

---

# 16. Database migrations

Never use destructive Room migration for production journal data without an
explicit, justified recovery strategy.

When schema changes:

1. Increment database version.
2. Provide a proper Room migration.
3. Preserve existing journal entries.
4. Test migration using a production-like database.
5. Test both fresh installs and upgrades.

Before modifying an entity, inspect existing production behavior and constraints.

---

# 17. Existing functionality must not regress

Preserve:

- one entry per day
- today's editability
- previous-day locking
- manual Save
- Update behavior
- streak calculation
- favorites
- history
- theme preferences
- existing Compose UI
- existing navigation

Do not rewrite working features unnecessarily.

---

# 18. Required test matrix

## Local database

- create entry
- update entry
- favorite/unfavorite
- retrieve history
- one-entry-per-day constraint
- streak calculation
- Room migration

## Guest

- fresh install
- app restart
- process death
- device restart
- offline use
- backup/restore where testable

## Authentication

- successful Google authentication
- authentication cancellation
- authentication failure
- guest-to-account migration
- repeated migration
- existing cloud data
- migration failure/retry

## Authenticated

- offline write
- reconnect
- upload
- download
- reinstall
- new device
- sign-out
- sign-in again

## Conflict

- local newer than cloud
- cloud newer than local
- equal timestamps
- duplicate upload
- interrupted synchronization
- repeated synchronization

---

# 19. Required implementation report

After making changes, report:

## Architecture

Explain the final:

    UI → ViewModel → Repository → Room → SyncManager → Cloud

architecture.

## Files changed

List every created or modified file.

## Database

Describe:

- entity changes
- database version
- migrations

## Authentication

Describe:

- Google Sign-In
- Firebase Authentication
- UID handling

## Cloud

Describe:

- database structure
- synchronization
- security rules

## Backup

Describe:

- Android Auto Backup configuration
- included data
- excluded data
- limitations

## Recovery matrix

Provide a table:

| Scenario | Guest | Authenticated |
|---|---|---|
| Normal use | Room | Room + cloud sync |
| Offline | Room | Room + pending sync |
| App restart | Room | Room |
| Reinstall | Android restore if available | Cloud recovery |
| New device | Android restore if available | Cloud recovery |
| Device lost | Potentially unrecoverable | Cloud recovery |
| Cross-device | No | Yes |

## Tests

List automated and manual tests implemented.

## Risks

Explicitly identify remaining risks, assumptions, and platform limitations.

---

# 20. Engineering principles

Follow these principles throughout implementation:

1. **Local-first.**
2. **Never block journaling on network availability.**
3. **Never discard existing user journal data during authentication.**
4. **Never use destructive migrations for journal data without a recovery plan.**
5. **Cloud recovery is the durable mechanism for authenticated users.**
6. **Android Auto Backup is a safety net, not a guaranteed cloud-sync system.**
7. **Authentication and authorization are different concerns.**
8. **Cloud authorization must be enforced server-side.**
9. **Synchronization must be idempotent.**
10. **Conflict handling must be deterministic.**
11. **Journal content is private user data and must not appear in logs or analytics.**
12. **Inspect the existing code before choosing implementation details.**
13. **Prefer minimal architectural changes over unnecessary rewrites.**
14. **Test recovery scenarios, not just CRUD behavior.**

---

# 21. Definition of done

The implementation is complete only when:

- Guest users can use the app without authentication.
- Guest journal data persists locally.
- Android backup configuration is reviewed and appropriately configured.
- Google authentication works.
- Authenticated journal data can synchronize to the cloud.
- Guest data can migrate safely into an authenticated account.
- Offline writes work.
- Synchronization retries work.
- Conflict resolution is deterministic.
- Authenticated users can recover data after reinstall.
- Authenticated users can recover data on a new device.
- Sign-out does not silently destroy journal data.
- Room migrations preserve existing entries.
- Firebase/cloud security rules isolate users.
- Journal content is not exposed through logs/analytics.
- Recovery scenarios have automated/manual test coverage.
- Existing One Line Journal functionality continues to work.
