# My DayCal — Senior Developer Review & Upgrade Plan

This plan synthesizes a complete architectural and UI/UX audit of **My DayCal** into a focused set of high-impact upgrades: a **Warm Ivory Light Theme** with an instant top-bar toggle, **week-bar swipe gestures**, **overlap conflict badges**, **undoable task deletion**, **automatic reminder rescheduling on inline time shifts**, **centralized semantic theme tokens**, and **removal of dead Daily Journal database code**.

---

## User Review & Confirmed Decisions

> [!IMPORTANT]
> All product and technical choices below were confirmed during our interactive review and will be implemented upon approval:

- **Confirmed Decision 1 — Light Theme Support**: Add a **Warm Ivory Light Theme** (`#FAF8F5` paper canvas, `#FFFFFF` elevated cards, `#D97706` solar amber accents) with a dedicated theme toggle in the top header bar and persistent preference storage.
- **Confirmed Decision 2 — High-Impact Planning Features**:
  1. **Overlap Conflict Badge**: Automatically detect when two time blocks collide on the same day and display a high-contrast `OVERLAP` warning badge on affected cards.
  2. **Horizontal Swipe on Week Scrubber**: Swipe left or right on the `CW` + `MON`–`SAT` week bar to step forward or backward by 7 days (`±1 week`).
  3. **Undo Snackbar on Delete**: Deleting a time block from either the inline speed-action bar or the edit sheet presents a Snackbar with an **Undo** action that restores the task and its scheduled alarm.
- **Confirmed Decision 3 — Codebase & Reliability Cleanups**:
  1. **Prune Dead Daily Journal Persistence**: Remove unused `DayJournal`, `LocalJournalEntity`, and journal DAO/repository methods left over from the retired reflection feature.
  2. **Centralize Hardcoded Colors**: Replace private file-level hex constants across screens with adaptive `MaterialTheme.colorScheme` tokens so every component responds seamlessly to Dark and Light themes.
  3. **Reschedule Reminders on Quick Time Shifts**: Ensure tapping `◀ -30m`, `+30m ▶`, `-15m len`, or `+15m len` on an expanded task card automatically updates its `AlarmManager` notification trigger.

---

## 1. Senior Developer Audit: What to Add, Remove & Why Light Theme Matters

### What We Are Adding (High-Signal Time Blocking)
- **Schedule Overlap Detection (`OVERLAP` Badge)**: Time-blocking apps lose trust when two blocks silently occupy the same window (e.g., `09:00–10:30` and `10:00–11:00`). Detecting interval intersections (`a.start < b.end && b.start < a.end`) and surfacing a subtle coral `OVERLAP` pill gives immediate visual feedback without blocking rapid entry.
- **Week Scrubber Horizontal Swipe (`±7 Days`)**: Jumping to next week currently requires opening the modal calendar picker. Adding horizontal drag detection directly on the week scrubber bar makes multi-week planning effortless.
- **Undoable Task Deletion**: Accidental taps on the inline trash icon shouldn't permanently destroy a block. Caching the last deleted task in the ViewModel and offering a 1-tap **Undo** action in the Snackbar prevents data loss.
- **Automatic Reminder Rescheduling on Inline Shifts**: Previously, shifting a task by `±30m` updated the database record but left the `AlarmManager` broadcast scheduled for the old time. Connecting inline schedule adjustments to the notification scheduler closes this reliability bug.

### What We Are Removing (Codebase Hygiene)
- **Orphaned Daily Journal Layer**: Although the Daily Reflection UI was removed earlier, the SQLite table (`local_journals`), domain model (`DayJournal`), and Firestore collection methods (`observeJournals`, `saveJournal`) remained in the data layer. Removing them reduces schema surface area and keeps the repository focused strictly on time tiles.
- **Duplicated File-Level Color Constants**: Screen composables previously declared private `Color(0xFF...)` values that bypassed `MaterialTheme.colorScheme`. Consolidating these into semantic theme tokens eliminates duplication and unlocks full Light/Dark theme reactivity.

### Does the App Need a Light Theme?
- **Yes — for Daytime & Outdoor Ergonomics**: While **Obsidian Kinetic** (`#0C0E14` / `#111319`) is ideal for low-glare indoor focus and OLED battery savings, dark-only interfaces suffer from halation (astigmatism blur) and poor legibility under direct sunlight or bright office lighting.
- **Warm Ivory Daylight Palette**: Rather than a sterile `#FFFFFF` clinical white, **My DayCal** will pair its signature **Obsidian Kinetic** dark mode with a **Warm Ivory Editorial** light mode (`#FAF8F5` warm paper background, `#F2ECE4` low container, `#FFFFFF` cards, `#1C1917` deep ink typography, and `#D97706` / `#F59E0B` amber accents) controllable via a 1-tap sun/moon icon in the top bar.

---

## 2. User Experience & Visual Design

### Visual Identity & Dual-Theme Token Mapping

| Semantic Role | Obsidian Kinetic (Dark) | Warm Ivory (Light) | Usage Across Screens |
| :--- | :--- | :--- | :--- |
| `background` / `surface` | `#111319` | `#FAF8F5` | Root scaffold, top bar, launch screen canvas |
| `surfaceContainerLowest` | `#0C0E14` | `#F3EFEA` | `CW` badge, unallocated gap bars, empty hour slots |
| `surfaceContainerLow` | `#191B22` | `#EFECE6` | Unselected week chips, progress summary container, bottom sheet |
| `surfaceContainer` | `#1E1F26` | `#FFFFFF` | Resting task cards, `TIME` and `TASK & DETAILS` cards |
| `surfaceContainerHigh` | `#282A30` | `#E6E1D8` | Expanded task cards, dial boxes, unselected pills |
| `surfaceContainerHighest` | `#33343B` | `#DCD5C9` | Progress track, category badge fill, borders |
| `primary` / `primaryContainer` | `#FFC174` / `#F59E0B` | `#B45309` / `#F59E0B` | Active `Timeline/24h` pill, selected day chip, `+` FAB, `+ Add` CTA |
| `onSurface` / `onSurfaceVariant` | `#E2E2EB` / `#D8C3AD` | `#1C1917` / `#57534E` | Task titles, tabular timestamps, notes, section headers |
| `error` (Overlap / Delete) | `#FFB4AB` / `#F43F5E` | `#DC2626` | `OVERLAP` warning badge and delete actions |

### Key User Flows
1. **Instant Theme Switching**:
   - A compact `34dp` circular Sun/Moon button in the top bar toggles between **Obsidian Dark** and **Warm Ivory Light**, persisting the choice in `SharedPreferences` (`use_light_theme_mode`).
2. **Week Scrubber Navigation**:
   - Swiping horizontally left on the `CW` + `MON`–`SAT` row advances the selected date by `+7 days` (next calendar week); swiping right steps back by `-7 days` (previous calendar week).
   - Tapping `CW` or tapping the already-selected day chip continues to open the Calendar Date Picker modal.
3. **Overlap Detection**:
   - Whenever two or more tasks on the selected date have overlapping time ranges (`startMinute < other.endMinute && other.startMinute < endMinute`), an `OVERLAP` badge renders next to the category pill on both Timeline and 24h cards so the user can tap to expand and shift the block by `±30m`.
4. **Undoable Deletion**:
   - Deleting a task cancels its alarm, removes it from storage, and displays a Snackbar (`"Task deleted"` with an **Undo** button). Tapping **Undo** re-inserts the task and restores its `AlarmManager` reminder.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1 — Persistent Manual Theme Toggle (Defaulting to Dark)**
  - *Chosen Approach*: Default to the signature **Obsidian Kinetic** dark theme while providing a 1-tap Sun/Moon toggle in the top bar that persists across launches.
  - *Why*: Preserves the app's core dark-first identity while giving users immediate control when stepping into bright daylight without forcing them into Android system settings.
- **Decision 2 — Non-Blocking Overlap Badges Instead of Hard Save Errors**
  - *Chosen Approach*: Allow saving overlapping tasks and highlight them with a visual `OVERLAP` pill on the timeline.
  - *Why*: Blocking the "Add" button with modal error dialogs slows down rapid capture; visual conflict badges let users log tasks quickly and resolve overlaps inline using the `◀ -30m` / `+30m ▶` chips.
- **Decision 3 — Room Schema Bump with Destructive Fallback on Local Cache**
  - *Chosen Approach*: Remove `LocalJournalEntity` from `LocalDaytileDatabase` and increment the Room version with `fallbackToDestructiveMigration` (or clean table drop).
  - *Why*: Simplifies the local SQLite database to a single `local_tasks` entity with zero unused tables.

---

## 4. Technical Architecture & Data Strategy

### Architecture & Component Diagram

```
┌───────────────────────────────────────────────────────────────────────────┐
│                         MainActivity & Theme Layer                        │
│  ┌──────────────────────┐   ┌───────────────────────┐   ┌──────────────┐  │
│  │ DaytileTheme         │   │ DayCalTopBar          │   │ TaskEditor   │  │
│  │ (Obsidian Dark /     │   │ • Local/Sync Badge    │   │ BottomSheet  │  │
│  │  Warm Ivory Light)   │   │ • Theme Toggle (☀/☾)  │   │ • Loop Dials │  │
│  └──────────┬───────────┘   │ • Timeline | 24h Pill │   └──────┬───────┘  │
└─────────────┼───────────────┴───────────┬───────────┴──────────┼──────────┘
              │                           │                      │
              ▼                           ▼                      ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                           DayTimelineScreen                               │
│  ┌─────────────────────────────────────────────────────────────────────┐  │
│  │ SingleDayHeaderSection                                              │  │
│  │ • CW + MON..SAT Week Bar (Tap CW/Selected -> DatePicker,            │  │
│  │   Horizontal Swipe -> ±7 Days) + Amber Progress Card                │  │
│  └─────────────────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────┐  ┌───────────────────────────────┐  │
│  │ Timeline Stream Mode             │  │ 24h Spanned Grid Mode         │  │
│  │ • DayTaskTileCard + OVERLAP Pill │  │ • Proportional Duration Span  │  │
│  │ • Pulsing NOW Line               │  │ • Covered Hour Absorption     │  │
│  │ • + Schedule Gap Indicators      │  │ • Inline Speed-Action Bar     │  │
│  └──────────────────────────────────┘  └───────────────────────────────┘  │
└─────────────────────────────────────────┬─────────────────────────────────┘
                                          │
                                          ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                    DaytileViewModel & Reminder Manager                    │
│  • Computes overlappingTaskIds: Set<String> per day                       │
│  • Caches lastDeletedTask for Snackbar Undo restoration                   │
│  • Syncs AlarmManager reminders on Create, Update, Shift, Delete & Undo   │
└───────────────────┬───────────────────────────────────┬───────────────────┘
                    │                                   │
                    ▼                                   ▼
      ┌───────────────────────────┐       ┌───────────────────────────┐
      │ LocalPlannerDataSource    │       │ CloudPlannerDataSource    │
      │ • Room SQLite (Tasks only)│       │ • Firestore /users/{uid}/ │
      └───────────────────────────┘       └───────────────────────────┘
```

### Interactive Component & State Mapping
1. **Overlap Calculation (`DaytileUiState.overlappingTaskIds`)**:
   - In `DaytileViewModel`, as `selectedDayTasks` is derived, compute `overlappingTaskIds: Set<String>` by checking all pairs `(a, b)` where `a.id != b.id && a.startMinute < b.endMinute && b.startMinute < a.endMinute`.
   - Pass `hasOverlap = task.id in uiState.overlappingTaskIds` to `DayTaskTileCard` so both Timeline and 24h views display the `OVERLAP` badge automatically.
2. **Week Scrubber Swipe (`pointerInput` horizontal drag)**:
   - Attach `Modifier.pointerInput(selectedDateKey) { detectHorizontalDragGestures(...) }` to the week scrubber row in `SingleDayHeaderSection`. When total horizontal drag exceeds `±60.dp`, trigger `onStepWeek(if (dragAmount < 0) 7 else -7)`.
3. **Undoable Deletion & Alarm Sync**:
   - `DaytileViewModel.deleteTask(task)` stores `lastDeletedTask = task` and emits a signal to display a Snackbar with actionLabel `"Undo"`.
   - Tapping **Undo** calls `viewModel.undoDeleteTask()`, which re-saves the task and invokes `ReminderNotificationManager.scheduleTaskReminder(context, restoredTask)`.
   - Calling `onAdjustTaskSchedule` (`±30m` start or `±15m` duration) now also calls `ReminderNotificationManager.scheduleTaskReminder(context, adjustedTask)` so alarms stay in sync with quick shifts.
