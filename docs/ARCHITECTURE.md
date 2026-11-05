# My DayCal — Architecture & Technical Documentation

## 1. Attribution & Design Lineage

**My DayCal** draws inspiration from **[Adiseny/daytile](https://github.com/Adiseny/daytile)**, an open-source, privacy-focused offline daily timeline planner for Android.

| Aspect | `Adiseny/daytile` | `My DayCal` |
| :--- | :--- | :--- |
| **Core Metaphor** | 24-hour visual timeline with draggable/resizable task tiles | 24-hour axis + focused tile stream with tactile `-30m/+30m` shift and `-15m/+15m` duration controls |
| **Account Requirement** | Strictly offline, zero accounts | Dual-mode: **Offline Local Storage (Room SQLite)** without an account, OR optional **Google Cloud Sync (Firestore)** |
| **Date Horizon** | Daily planning | Single-Day focus + **Past Days Archive** + **20-Year Multi-Year Calendar** (±10 years) |
| **Journaling** | Task blocks | Task tiles + per-day **Reflection Journal** & 5-level Energy/Mood Pulse |
| **UI Stack** | Android native | Kotlin + Jetpack Compose (Material 3) with locally bundled Google Fonts (`Space Grotesk`, `Plus Jakarta Sans`, `JetBrains Mono`) |

---

## 2. Dual-Storage Architecture (`PlannerDataSource`)

To support both account-free local usage and real-time multi-device cloud synchronization without duplicating UI or ViewModel logic, `DaytileViewModel` depends on the `PlannerDataSource` interface (`app/src/main/java/com/example/data/repository/PlannerDataSource.kt`):

```text
                   ┌──────────────────────────┐
                   │    DaytileViewModel      │
                   │  (O(1) Date-Key Index)   │
                   └────────────┬─────────────┘
                                │
                   ┌────────────▼─────────────┐
                   │    PlannerDataSource     │
                   └─────┬──────────────┬─────┘
                         │              │
        ┌────────────────▼───┐      ┌───▼────────────────────┐
        │LocalPlannerDataSrc │      │  CloudPlannerDataSrc   │
        │  (Offline Mode)    │      │ (Google Sign-In Mode)  │
        └────────┬───────────┘      └───┬────────────────────┘
                 │                      │
        ┌────────▼───────────┐      ┌───▼────────────────────┐
        │LocalDaytileDatabase│      │   DaytileRepository    │
        │   (Room SQLite)    │      │   (Cloud Firestore)    │
        └────────────────────┘      └────────────────────────┘
```

### Offline Local Mode (`LocalDaytileDatabase`)
- Stored in `my_daycal_local.db` via AndroidX Room (`LocalTaskEntity` in `local_tasks` and `LocalJournalEntity` in `local_journals`).
- Requires zero network requests and zero account credentials.
- Users can switch from Local Mode to Google Cloud Sync at any time from the top app bar.

### Cloud Sync Mode (`DaytileRepository`)
- Uses **Google Sign-In** via Jetpack `CredentialManager` (`GetSignInWithGoogleOption`).
- Connects to the named Firestore Enterprise database (`R.string.firestore_database_id`).
- Stores data under per-user isolated paths:
  - `/users/{userId}/tasks/{taskId}` (`DayTask`)
  - `/users/{userId}/journals/{dateKey}` (`DayJournal`)
- Enforces zero-trust ABAC validation rules in `firestore.rules`.

---

## 3. High-Performance 20-Year Calendar & O(1) Date Indexing

- **Date Representation**: All dates across the 20-year horizon (`currentYear - 10` through `currentYear + 10`) use canonical `YYYY-MM-DD` strings (`dateKey`).
- **Lexicographical Ordering**: Because `YYYY-MM-DD` strings sort identically to chronological dates, filtering past days (`dateKey < todayDateKey`), today (`dateKey == todayDateKey`), and future planned days (`dateKey > todayDateKey`) requires zero `Date` object allocations.
- **Background Indexing**: `DaytileViewModel` combines the reactive task and journal streams on `Dispatchers.Default`, pre-building `Map<String, List<DayTask>>` and `Map<String, DayJournal>`. Switching between any day or year in the 20-year calendar performs an $O(1)$ map lookup on the UI thread.

---

## 4. Testing & Verification

### Tier 1: JavaScript Security Rules Tests
Validates `firestore.rules` against the local Firestore emulator:
```bash
FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js
```

### Tier 2: Robolectric JVM & Emulator Integration Tests
Validates Kotlin model serialization, repository operations, cross-user isolation, and 20-year calendar math:
```bash
gradle :app:testDebugUnitTest
```

---

## 5. GitHub Actions APK Release Pipeline

The workflow in `.github/workflows/release-apk.yml` automates APK generation and GitHub Releases:

1. **Automatic Release on Version Tags**: Push any tag matching `v*` (e.g., `v1.0.0`, `v2.0.0`) to create a versioned GitHub Release with `My-DayCal-vX.Y.Z-release.apk` and `My-DayCal-vX.Y.Z-debug.apk`.
2. **Automatic Release on Major Main Branch Changes**: Pushing to `main` or `master` when `app/build.gradle.kts` changes (or with `[release]` / `major:` in the commit message, or via manual `workflow_dispatch`) builds the APK and publishes/updates the GitHub Release automatically.
