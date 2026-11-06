# My DayCal — Cross-Platform Web Sync & GitHub APK Google Sign-In Plan

Enable seamless real-time synchronization between a **GitHub Pages Companion Web App** and the **Android Phone App** via shared Google Sign-In and Cloud Firestore, while fixing the signing certificate and Firebase OAuth configuration in the GitHub Actions APK pipeline so Google Sign-In works reliably on downloaded APKs.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> All core product and technical preferences have been confirmed:
> - **Web Access & Real-Time Sync**: Build a full-featured **Companion Web App** hosted on **GitHub Pages** that connects to the same Firebase project and named Cloud Firestore database as the Android app.
> - **GitHub APK Google Sign-In Fix**: Auto-configure the provisioned Firebase OAuth client and align the release APK signing certificate (`SHA-1`) in the GitHub Actions workflow so downloaded APKs authenticate with Google out of the box.
> - **Web App Feature Scope**: Include **Timeline & 24-Hour Views** with real-time sync, the **New / Edit Task Modal** with all 5 categories, and the **Obsidian Kinetic Dark / Warm Ivory Light Theme Toggle**.

> [!NOTE]
> **One-Time Firebase Authorized Domain Step for GitHub Pages**:
> Once GitHub Pages publishes your web app at `https://<your-github-username>.github.io/<repo-name>/`, add `<your-github-username>.github.io` under **Firebase Console → Authentication → Settings → Authorized domains** so Google's OAuth popup permits sign-in from your GitHub Pages domain.

---

## 1. Overview & Core Concept

- **What It Does**:
  1. **Fixes Android APK Google Sign-In from GitHub Releases**: Ensures both Release and Debug APKs built by GitHub Actions contain the provisioned Firebase OAuth configuration and are signed with the exact `SHA-1` certificate registered with Google Identity Services (`11:B0:35:38:20:19:2F:9F:AF:CC:0F:3F:0D:A9:54:B5:E7:0F:BC:43`).
  2. **Delivers a Companion Web App with Instant Android Sync**: Provides a responsive web application deployed automatically via GitHub Pages. Signing in with the same Google account on the web and on your Android phone syncs every task creation, edit, time shift, and completion in real time.
- **Target Audience / Persona**: Users who plan and review their day on a laptop or desktop browser during work blocks and want their schedule immediately reflected on their Android phone and Android home screen widget on the go.
- **Key Value**: True multi-device continuity without third-party subscription fees, backed by your existing zero-trust Cloud Firestore security rules.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Installing & Signing In on the GitHub APK (Android)**:
   - User downloads `My-DayCal-v1.4-release.apk` from GitHub Releases and taps **Sign in with Google**.
   - Android Credential Manager verifies the APK's `SHA-1` signature against the registered Firebase Android OAuth client, completes Google Sign-In, and loads the user's cloud timeline.
2. **Accessing the Companion Web App (Desktop / Mobile Browser)**:
   - User opens the GitHub Pages URL (also linked from the repository header and in-app Cloud Sync dialog).
   - User clicks **Sign in with Google** (or **Continue Offline** for browser-local storage).
   - Once signed in, the web app attaches a live Firestore `onSnapshot` listener to `/users/{uid}/tasks` in the named database (`ai-studio-android-daytile-bb9d7cd1-7486-4dce-987f-d18abf26cbde`).
3. **Cross-Device Real-Time Planning**:
   - User switches between the **7-Day Sequential Week Strip (`SUN`–`SAT`)** with animated `CW` badge, **Timeline View**, and **24-Hour Grid View**.
   - Adding, editing, completing, or quick-shifting (`-30m`, `+30m`, `-15m`, `+15m`) a time block on the web immediately updates the Android app and passes all `firestore.rules` schema validations.

### Visual Identity & Theme
- **Aesthetic Direction**: Shares the exact **Obsidian Kinetic** (Dark) and **Warm Ivory** (Light) design language with the Android app.
- **Color Palette & Mood**:
  - *Obsidian Kinetic Dark*: Deep obsidian canvas (`#0D0F12`), elevated card containers (`#15181E` / `#1C2026`), warm amber primary (`#F5A623`), and high-contrast category accents (Amber Focus, Sky Work, Mint Personal, Coral Health, Purple Break).
  - *Warm Ivory Light*: Warm alabaster background (`#FAF7F2`), crisp ivory cards (`#FFFFFF`), deep amber primary (`#C87A00`), and slate typography (`#181A1F`).
- **Typography & Hierarchy**: Clean geometric sans-serif headings paired with tabular numerals (`font-variant-numeric: tabular-nums`) for all time ranges (`08:30 - 10:00`), durations, and `CW` counters.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Aligning Release Keystore `SHA-1` in GitHub Actions**
  - *Chosen Approach*: Convert the repository's `debug.keystore.base64` key entry into the `upload` alias of `my-upload-key.jks` during the GitHub Actions build, and supply the provisioned Firebase configuration when `GOOGLE_SERVICES_JSON` is not overridden.
  - *Why*: Google Sign-In on Android strictly validates the `(package_name, certificate_hash)` pair against the Firebase project's OAuth client. Generating a random key on each CI run guaranteed an OAuth signature mismatch (`GetCredentialCancellationException`). Using the registered certificate ensures Google Sign-In works immediately on every GitHub Release APK.
- **Decision 2: Zero-Build Modular Web App with Automated GitHub Pages Deployment**
  - *Chosen Approach*: Implement the companion web app using the official Firebase v11 Modular ES Modules SDK (`firebase-app`, `firebase-auth`, `firebase-firestore`) and deploy it via an automated GitHub Actions Pages workflow alongside the Android project.
  - *Why*: Keeps the Android Gradle build completely untouched while giving the repository an instant, zero-maintenance web deployment that shares the exact same Firestore database and security rules.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Architecture & Cross-Platform Sync Diagram

```text
  ┌─────────────────────────────────┐       ┌─────────────────────────────────┐
  │    Android App (My DayCal)      │       │   Companion Web App (Browser)   │
  │  • Jetpack Compose + Widget     │       │  • Timeline & 24h Views         │
  │  • CredentialManager Google ID  │       │  • Firebase Auth Google Popup   │
  │  • Signed with Registered SHA-1 │       │  • Dark / Warm Ivory Light Mode │
  └───────────────┬─────────────────┘       └─────────────────┬───────────────┘
                  │                                           │
                  │  Real-Time Snapshot Listener & Writes     │
                  └────────────────────┬──────────────────────┘
                                       ▼
        ┌──────────────────────────────────────────────────────────────┐
        │             Firebase Auth & Cloud Firestore                  │
        │  Project: gen-lang-client-0504309294                         │
        │  Database: ai-studio-android-daytile-bb9d7cd1-7486-4dce...   │
        │  Collection: /users/{userId}/tasks/{taskId}                  │
        │   Enforced by Zero-Trust ABAC Security Rules (firestore.rules)│
        └──────────────────────────────────────────────────────────────┘
```

### Data Model & Security Rule Compliance on Web
Every write from the Companion Web App strictly conforms to `isValidDayTask` in `firestore.rules`:
- **Required Fields**: `userId` (matching `auth.currentUser.uid`), `dateKey` (`YYYY-MM-DD`), `title` (`1..200` chars), `notes` (`0..2000` chars), `startMinute` (`0..1439` integer), `durationMinutes` (`15..720` integer), `category` (`focus`, `work`, `personal`, `health`, `break`), `colorIndex` (`0..5` integer), `reminderMinutes` (`-1..1440` integer), `completed` (`boolean`), `createdAt` (`serverTimestamp()`), and `updatedAt` (`serverTimestamp()`).
- **Update Invariants**: Preserves existing `userId` and `createdAt` timestamps on task edits, quick time shifts, and completion toggles so Firestore security rules validate every mutation.

### Interactive Component & State Mapping
- **Auth & Storage Switcher**: Supports Google Sign-In (`GoogleAuthProvider`) with automatic session persistence and an offline `localStorage` mode (`Continue Offline`) so the web app works with or without an account.
- **Sequential 7-Day Week Bar & Date Picker**: Computes Sunday-based Calendar Week (`CW`) numbers, highlights `TODAY` and days with scheduled tasks, and supports jumping to any date.
- **Timeline & 24-Hour Views**: Computes daily completion progress (`tasks • scheduled time • % DONE`), detects colliding tasks (`OVERLAP` badge), renders proportional multi-hour cards in `24h` mode, and provides inline `-30m / +30m / -15m / +15m` controls.
- **New / Edit Task Modal**: Interactive 12-hour and 5-minute time selectors, `AM`/`PM` toggle, duration presets (`15m` to `4h`), category selector pills, and Undo toast on deletion.
