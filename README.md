# My DayCal

**Minimalist daily timeline & reflection journal.**

[![Android Build & APK Release](https://img.shields.io/badge/GitHub_Actions-APK_Release_Pipeline-F59E0B?logo=githubactions&logoColor=white)](.github/workflows/release-apk.yml)
[![Platform](https://img.shields.io/badge/Platform-Android_(API_24%2B)-14B8A6?logo=android&logoColor=white)](#)
[![UI](https://img.shields.io/badge/UI-Jetpack_Compose_(Material_3)-3B82F6?logo=jetpackcompose&logoColor=white)](#)
[![Storage](https://img.shields.io/badge/Storage-Local_Room_SQLite_%7C_Google_Cloud_Firestore-F43F5E)](#storage-modes)

---

## Overview

**My DayCal** is a fast, distraction-free daily timeline planner and personal reflection journal for Android. It focuses your attention on **one single day at a time** with a clean top-bar date picker (`MMM dd`), top-bar `Focus` / `24h` view toggle, compact horizontal-scrolling tile creation, and local notification reminders.

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
  - **Unified Single-Screen Timeline**: Top-center `MMM dd` calendar picker and top-right `Focus` / `24h` view switcher with zero bottom-tab clutter.
  - **Compact Horizontal Tile Creator**: Streamlined bottom sheet where Category (`Focus`, `Work`, `Personal`, `Health`, `Break` with fixed color indicators), Start Time, Duration, and Reminder options scroll horizontally.
  - **Task Reminders & Notification Channels**: Native Android notification channels (`daycal_task_reminders` & `daycal_daily_journal`) with `AlarmManager` scheduling.
  - **Daily Reflection & Journal**: Per-day reflection journal with a 5-level Energy & Focus Pulse rating, daily highlight headline, and freeform notes.
  - **Optional Google Cloud Sync**: Opt-in Google Sign-In and hardened Cloud Firestore sync so users who want multi-device access can sync using free Google infrastructure.

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
