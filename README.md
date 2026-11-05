# My DayCal

**Minimalist daily timeline, journal & 20-year calendar.**

[![Android Build & APK Release](https://img.shields.io/badge/GitHub_Actions-APK_Release_Pipeline-F59E0B?logo=githubactions&logoColor=white)](.github/workflows/release-apk.yml)
[![Platform](https://img.shields.io/badge/Platform-Android_(API_24%2B)-14B8A6?logo=android&logoColor=white)](#)
[![UI](https://img.shields.io/badge/UI-Jetpack_Compose_(Material_3)-3B82F6?logo=jetpackcompose&logoColor=white)](#)
[![Storage](https://img.shields.io/badge/Storage-Local_Room_SQLite_%7C_Google_Cloud_Firestore-F43F5E)](#storage-modes)

---

## Overview

**My DayCal** is a fast, distraction-free daily timeline planner, personal reflection journal, and multi-year horizon calendar for Android. It focuses your attention on **one single day at a time**, while keeping an automatic searchable archive of all your past days and a **20-year calendar** (10 years in the past to 10 years in the future) so you can inspect history or schedule tasks years ahead.

You can use **My DayCal** in two ways right from the minimal start screen:
1. **Continue on Local Storage (No Account Required)** — 100% offline, private on-device SQLite storage powered by Android Jetpack Room (`my_daycal_local.db`).
2. **Sign in with Google (Free Cloud Sync)** — Real-time synchronization across your Android phone and companion web clients using free Google Firebase services (Google Sign-In + Cloud Firestore).

---

## Credits, Inspiration & Borrowed Concepts

This project's core timeline interaction model and minimalist philosophy are inspired by **[Adiseny/daytile](https://github.com/Adiseny/daytile)**:

- **Original Reference Project**: [`https://github.com/Adiseny/daytile`](https://github.com/Adiseny/daytile) by [@Adiseny](https://github.com/Adiseny)
- **What Was Borrowed / Adapted from `Adiseny/daytile`**:
  - **24-Hour Time-Block Tile Concept**: Representing daily tasks as visual, color-coded time tiles along a 24-hour axis (`00:00` – `23:59`) with start-minute and duration semantics.
  - **Tactile In-Place Tile Adjustments**: Quick duration resizing (`-15m` / `+15m`) and time-slot shifting (`-30m` / `+30m`) directly on the timeline without complex multi-screen forms.
  - **Privacy & Offline-First Ethos**: Providing a zero-account **Local Storage Mode** where all user data stays strictly on the device, honoring Daytile's commitment to local privacy.
- **How `My DayCal` Extends the Concept**:
  - **Daily Reflection & Journal**: Adds a per-day reflection journal with a 5-level Energy & Focus Pulse rating, daily highlight headline, and freeform notes.
  - **Past Days Archive**: Automatically aggregates all past days, completed tiles, and reflections in reverse chronological order with full-text search and category filters.
  - **20-Year Multi-Year Horizon (±10 Years)**: Adds both a 12-month full-year matrix and a detailed monthly calendar spanning 10 years into the past and 10 years into the future.
  - **Optional Google Cloud Sync**: Adds opt-in Google Sign-In and hardened Cloud Firestore sync so users who want multi-device access (Android + Web) can sync using free Google infrastructure.

---

## Key Features

### 1. Focused Single-Day View (`Day View`)
- Displays **only one active day** at a time to eliminate visual overwhelm.
- **7-Day Quick Ribbon** + previous/next day buttons + one-tap **Today** jump chip.
- Toggle between **Focused Tile Stream** and **Full 24-Hour Axis Grid** (tap any empty hour slot to create a tile at that exact hour).
- **Daily Reflection & Journal** section embedded directly beneath the day's timeline.

### 2. Past Days Archive (`Past Archive`)
- Reverse-chronological archive of every logged day, completed task tile, and journal entry.
- Instant search across dates, task titles, notes, and journal reflections.
- Filter by **Past & Today**, **All Logged Days**, **Future Planned**, or **With Journal**.

### 3. 20-Year Multi-Year Calendar (`20-Year Calendar`)
- Navigate **10 years in the past to 10 years in the future** with one-tap `-10Y`, `-1Y`, `This Year`, `+1Y`, and `+10Y` buttons or the scrollable year ribbon.
- Toggle between the **12-Month Full-Year Overview Matrix** and the **Interactive Month Detail Grid**.
- Tap any date in the 20-year span to preview its schedule, open its Day View, or schedule a future task.

### 4. Dual Storage Modes (`Local` & `Synced`)
- **Local Storage Mode**: Uses Android Room (`LocalDaytileDatabase`) with reactive Kotlin `Flow` queries. No login or internet required.
- **Google Cloud Sync Mode**: Uses Jetpack `CredentialManager` (`GetSignInWithGoogleOption`) and Cloud Firestore (`/users/{userId}/tasks` and `/users/{userId}/journals`) protected by zero-trust security rules (`firestore.rules`).

---

## Downloading the APK

Pre-built Android APKs (`My-DayCal-release.apk` and `My-DayCal-debug.apk`) are automatically built and published on the [**GitHub Releases**](../../releases) page by the repository's GitHub Actions workflow.

1. Go to the [**Releases**](../../releases) section of this repository.
2. Download `My-DayCal-release.apk` (or `My-DayCal-debug.apk`) from the latest release.
3. Open the downloaded `.apk` file on your Android device (Android 7.0 / API 24 or newer) to install.

---

## Automated APK Release Workflow

This repository includes an automated GitHub Actions pipeline at [`.github/workflows/release-apk.yml`](.github/workflows/release-apk.yml):

- **Triggers**:
  - Pushing a version tag (`v*.*.*`, e.g., `git tag v1.1.0 && git push origin v1.1.0`).
  - Pushing commits to `main` or `master` when `app/build.gradle.kts` (`versionName` / `versionCode`) changes, or when the commit message includes `[release]` or `major:`.
  - Manual trigger via **Actions → Build & Release Android APK → Run workflow**.
- **What It Does**:
  - Compiles both Debug and Release APKs with Gradle.
  - Uploads the APKs as workflow build artifacts.
  - Automatically creates or updates a **GitHub Release** with downloadable `.apk` files attached.

---

## Project Structure & Documentation

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for detailed architecture documentation, data models, security rules, and testing instructions.

```text
├── .github/workflows/release-apk.yml   # CI/CD workflow for building & releasing APKs
├── app/
│   ├── build.gradle.kts                # App configuration, dependencies & versionCode/versionName
│   └── src/
│       ├── main/java/com/example/
│       │   ├── MainActivity.kt         # Root navigation, storage mode switcher & tile editor sheet
│       │   ├── data/
│       │   │   ├── local/              # Room SQLite database (LocalDaytileDatabase, Entities, DAO)
│       │   │   ├── model/              # Domain models (DayTask, DayJournal, TaskCategory)
│       │   │   └── repository/         # PlannerDataSource, LocalPlannerDataSource & DaytileRepository
│       │   ├── ui/
│       │   │   ├── auth/               # Minimal start screen (Google Sign-In + Local Storage option)
│       │   │   ├── screens/            # DayTimelineScreen, ArchiveScreen, MultiYearCalendarScreen
│       │   │   ├── theme/              # Material 3 theme, palettes & bundled Google Fonts
│       │   │   └── viewmodel/          # DaytileViewModel with O(1) date indexing
│       │   └── util/                   # DateUtils (20-year horizon calendar math & formatting)
│       └── test/java/com/example/      # Robolectric unit & Firestore emulator rule tests
├── docs/ARCHITECTURE.md                # Detailed technical & architectural documentation
├── firebase-blueprint.json             # Firestore schema blueprint
├── firestore.rules                     # Hardened zero-trust Firestore security rules
└── firestore.test.js                   # Node.js security rules unit test suite
```
