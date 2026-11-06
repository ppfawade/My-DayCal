# My DayCal — Architecture & Technical Documentation

## 1. Attribution & Design Lineage

**My DayCal** draws inspiration from **[Adiseny/daytile](https://github.com/Adiseny/daytile)**, an open-source, privacy-focused offline daily timeline planner for Android.

| Aspect | `Adiseny/daytile` | `My DayCal` |
| :--- | :--- | :--- |
| **Core Metaphor** | 24-hour visual timeline with draggable/resizable task tiles | Focused Timeline stream + proportional 24-hour grid with inline `-30m/+30m` shift and `-15m/+15m` duration controls |
| **Account Requirement** | Strictly offline, zero accounts | Dual-mode: **Offline Local Storage (Room SQLite)** without an account, OR optional **Google Cloud Sync (Firestore)** |
| **Week & Date Navigation** | Daily navigation | Continuous **7-day sequential week ribbon (`SUN`–`SAT`)** with Sunday start-of-week animated `CW` badge + **20-Year Calendar Picker** (±10 years) |
| **Time Selection** | Standard inputs | Interactive **12-hour vertical looping dial** (`01`–`12`) + **5-minute increment looping dial** (`00`–`55`) |
| **Scheduling Safeguards** | Visual blocks | Automatic **`OVERLAP` conflict detection**, **Undoable deletion Snackbar**, and **`AlarmManager` notification reminders** |
| **Theme System** | Dark UI | Dual-theme semantic tokens: **Obsidian Kinetic Dark** & **Warm Ivory Light** with live top-bar toggle |
| **UI Stack** | Android native | Kotlin + Jetpack Compose (Material 3) with locally bundled Google Fonts (`Space Grotesk`, `Plus Jakarta Sans`, `JetBrains Mono`) |

---

## 2. Dual-Storage Architecture (`PlannerDataSource`)

To support both account-free local usage and real-time multi-device cloud synchronization without duplicating UI or ViewModel logic, `DaytileViewModel` depends on the `PlannerDataSource` interface (`app/src/main/java/com/example/data/repository/PlannerDataSource.kt`):

```text
                   ┌──────────────────────────────┐
                   │       DaytileViewModel       │
                   │ (O(1) Date Index & Overlaps) │
                   └──────────────┬───────────────┘
                                  │
                   ┌──────────────▼───────────────┐
                   │      PlannerDataSource       │
                   └──────┬────────────────┬──────┘
                          │                │
         ┌────────────────▼───┐        ┌───▼────────────────────┐
         │LocalPlannerDataSrc │        │  CloudPlannerDataSrc   │
         │  (Offline Mode)    │        │ (Google Sign-In Mode)  │
         └────────┬───────────┘        └───┬────────────────────┘
                  │                        │
         ┌────────▼───────────┐        ┌───▼────────────────────┐
         │LocalDaytileDatabase│        │   DaytileRepository    │
         │ (Room SQLite v2)   │        │   (Cloud Firestore)    │
         └────────────────────┘        └────────────────────────┘
```

### Offline Local Mode (`LocalDaytileDatabase`)
- Stored in `my_daycal_local.db` (schema v2) via AndroidX Room (`LocalTaskEntity` in `local_tasks`).
- Requires zero network requests and zero account credentials.
- Users can switch from Local Mode to Google Cloud Sync at any time from the top app bar.

### Cloud Sync Mode (`DaytileRepository`)
- Uses **Google Sign-In** via Jetpack `CredentialManager` (`GetSignInWithGoogleOption`).
- Connects to the named Firestore Enterprise database (`R.string.firestore_database_id`).
- Stores per-user tasks under isolated paths:
  - `/users/{userId}/tasks/{taskId}` (`DayTask`)
- Enforces zero-trust ABAC validation rules in `firestore.rules`.

---

## 3. UI & Interaction Architecture

### 3.1 Sequential Week Slider & Dynamic `CW` Calculation (`DateUtils.kt` & `DayTimelineScreen.kt`)
- **Continuous 7-Day Sequence**: `DateUtils.getSequentialDays(anchorDateKey, radiusDays = 180)` generates a chronological sequence of all 7 days of the week (`SUN`, `MON`, `TUE`, `WED`, `THU`, `FRI`, `SAT`) rendered inside a `LazyRow` with `rememberSnapFlingBehavior`.
- **Sunday as Start of Week**: `DateUtils.getCalendarWeekNumber(dateKey)` sets `Calendar.SUNDAY` as `firstDayOfWeek`.
- **Reactive `CW` Badge**: The `CW` badge observes `dayStripState.firstVisibleItemIndex` and `selectedDateKey`. While swiping, `CW` updates dynamically from the first visible day; when a **Sunday** chip is selected, the ribbon smoothly animates (`animateScrollToItem`) so Sunday aligns as the first visible date of the new week, and the `CW` badge animates with a vertical slide + fade transition.

### 3.2 Proportional 24-Hour Timeline & Overlap Detection
- **Proportional Multi-Hour Spanning (`TwentyFourHourTimelineView`)**: In `24h` mode, each hour slot (`00:00`–`23:00`) calculates the proportional height of tasks starting in that hour (`(durationMinutes / 60f) * 60.dp`) and suppresses redundant empty-slot rows for subsequent hours covered by a multi-hour task.
- **Overlap Conflict Detection (`DaytileViewModel.computeOverlappingTaskIds`)**: Tasks on the selected date are sorted by `startMinute` on `Dispatchers.Default`. Any two tasks where `taskA.endMinute > taskB.startMinute` are added to `overlappingTaskIds: Set<String>`, rendering an `OVERLAP` warning badge on the card.
- **Undoable Deletion & Alarm Sync**: Deleting a task stores it in `recentlyDeletedTask` and cancels its alarm. Triggering `restoreDeletedTask()` via the Snackbar re-inserts the task and re-registers its `AlarmManager` notification via `TaskReminderScheduler`. Inline time shifts (`±30m`) also invoke `TaskReminderScheduler.scheduleTaskReminder` automatically.

### 3.3 Dual Theme Token System (`Theme.kt`)
- **Obsidian Kinetic Dark Theme**: Deep obsidian surfaces (`#0D0F12`, `#15181E`, `#1C2026`) with warm amber primary (`#F5A623`) and gold accents (`#F8B84E`).
- **Warm Ivory Light Theme**: Daylight-optimized warm alabaster surfaces (`#FAF7F2`, `#F2ECE1`, `#FFFFFF`) with deep amber primary (`#C87A00`) and crisp slate typography (`#181A1F`).

### 3.4 Week Agenda App Widget Architecture (`com.example.widget`)
- **`DayCalWidgetProvider`**: Android `AppWidgetProvider` handling widget updates, deep-link navigation to `MainActivity`, and header action triggers (Quick Add `+`, Refresh).
- **`DayCalWidgetService` & `DayCalWidgetViewsFactory`**: `RemoteViewsService` powering a smooth, scrollable `ListView` of week tasks.
- **`DayCalWidgetDataManager`**: Unified data layer reading synchronous Room SQLite records and cached Google Cloud tasks, chronologically sorting the 7-day week sequence with active/upcoming status, date badges, and category accent strips.
- **Freeform Resizability**: Android AppWidgetProviderInfo configured with `resizeMode="horizontal|vertical"` and adaptive constraints supporting common home screen widget sizes (2x2, 3x2, 4x2, 4x3, 4x4).

---

## 4. High-Performance 20-Year Calendar & O(1) Date Indexing

- **Date Representation**: All dates across the 20-year horizon (`currentYear - 10` through `currentYear + 10`) use canonical `YYYY-MM-DD` strings (`dateKey`).
- **Lexicographical Ordering**: Because `YYYY-MM-DD` strings sort identically to chronological dates, comparing dates requires zero `Date` object allocations.
- **Background Indexing**: `DaytileViewModel` indexes the reactive task stream on `Dispatchers.Default`, pre-building `Map<String, List<DayTask>>` and `Set<String>` of active dates. Switching between any day in the sequential strip or 20-year calendar performs an $O(1)$ map lookup on the UI thread.

---

## 5. Testing & Verification

### Tier 1: JavaScript Security Rules Tests
Validates `firestore.rules` against the local Firestore emulator:
```bash
FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js
```

### Tier 2: Robolectric JVM & Emulator Integration Tests
Validates Kotlin model serialization, repository operations, cross-user isolation, overlap conflict detection, and calendar utilities:
```bash
gradle :app:testDebugUnitTest
```

---

## 6. GitHub Actions APK Release Pipeline

The workflow in `.github/workflows/release-apk.yml` automates APK generation and GitHub Releases:

1. **Automatic Release on Push & Version Tags**: Pushing to `main`/`master` or pushing a tag matching `v*.*` (e.g., `v1.4`) builds `My-DayCal-<tag>-release.apk` and `My-DayCal-<tag>-debug.apk` and publishes/updates the GitHub Release automatically.
2. **Manual Dispatch**: Can also be triggered on demand from the GitHub Actions tab with a custom release tag.

---

## 7. Disclaimer — AI-Assisted Development

This project—spanning the Android Jetpack Compose UI, MVVM state management, Room & Cloud Firestore persistence layers, security rules, automated tests, and documentation—was generated and iteratively refined using AI-assisted software development tools.

All code is provided **"AS IS"** without warranty of any kind. Please review the implementation, dependencies, and security configurations thoroughly before using or redistributing in production environments.
