# NextA — Project Context

> **Source of truth:** This document describes the code and configuration that currently exist on GitHub `main`.
>
> It is intentionally based on the repository as it exists now, not on previous plans, agent-generated notes, or unfinished local changes.
>
> **Rule for future work:** Do not assume a feature is implemented because it is described here as a goal. Verify the actual source files first.

---

# 1. Current Repository State

Repository: `tudinh0722-wq/NextA`

Main Android package currently used by the active code:

```text
com.nexta
```

There is also an older package under:

```text
com.example.nexta
```

The old package is still present in the repository and should be treated as legacy until it is explicitly removed after verification.

The repository currently contains an Android application using Kotlin, Jetpack Compose, Room, Hilt, KSP, and the Android Gradle Plugin.

The current GitHub `main` state is build-oriented and contains the basic local Event data layer plus a simple Compose screen. The home-screen widget is **not implemented yet**: the only file currently under the widget package is an empty `ScheduleWidgetWorker.kt`.

---

# 2. Product Direction

NextA is intended to become a simple schedule assistant for Android.

The core idea is to work with concrete calendar events and eventually make the most useful current/upcoming schedule information easy to access.

For now, keep the implementation small. Do not add backend, accounts, cloud sync, GPS, OCR/AI, smartwatch features, notifications, or other large features unless explicitly requested.

---

# 3. Technology Currently in the Repository

The current Gradle configuration contains:

- Android Gradle Plugin `9.3.2`
- Kotlin `2.2.10`
- KSP `2.2.10-2.0.2`
- Compose BOM `2026.02.01`
- Room `2.8.4`
- Hilt `2.59.2`
- Android `compileSdk = 37`
- Android `targetSdk = 37`
- `minSdk = 26`
- Java source/target compatibility `17`

The app module applies these plugins:

```text
com.android.application
org.jetbrains.kotlin.plugin.compose
com.google.devtools.ksp
com.google.dagger.hilt.android
```

The project also contains this Gradle property:

```properties
android.disallowKotlinSourceSets=false
```

This is part of the current working Gradle configuration and should not be removed casually.

---

# 4. Current Project Structure

Relevant active package structure on GitHub:

```text
app/src/main/java/com/nexta
│
├── MainActivity.kt
├── MainApplication.kt
│
├── data
│   ├── local
│   │   ├── AppDatabase.kt
│   │   ├── EventDao.kt
│   │   └── EventEntity.kt
│   │
│   ├── model
│   │   ├── Event.kt
│   │   ├── EventType.kt
│   │   └── ScheduleResult.kt
│   │
│   ├── repository
│   │   └── EventRepository.kt
│   │
│   └── sample
│       ├── SampleDataSeeder.kt
│       └── SampleEvents.kt
│
├── di
│   └── DatabaseModule.kt
│
├── domain
│   ├── ScheduleState.kt
│   └── ScheduleStateEngine.kt
│
└── ui
    ├── MainScreen.kt
    └── widget
        └── ScheduleWidgetWorker.kt
```

Important current-state detail:

```text
ScheduleState.kt             → exists but is empty
ScheduleWidgetWorker.kt      → exists but is empty
AddEventScreen.kt             → does not currently exist on GitHub main
MainViewModel.kt              → does not currently exist on GitHub main
```

There is also a legacy source tree:

```text
app/src/main/java/com/example/nexta
```

containing the older application code/theme files.

---

# 5. Application Entry Point

`MainApplication.kt` currently uses Hilt:

```kotlin
@HiltAndroidApp
class MainApplication : Application()
```

The Android manifest registers it as the application class.

`MainActivity.kt` is annotated with:

```kotlin
@AndroidEntryPoint
```

and injects `EventRepository`.

The activity currently:

1. Seeds sample data if the database is empty.
2. Collects all Events from the repository.
3. Passes the Events to `MainScreen`.

There is currently **no actual Add Event navigation in `MainActivity.kt` on GitHub main**.

---

# 6. Event Domain Model

The active `Event` model is a concrete, dated event:

```kotlin
data class Event(
    val id: String,
    val title: String,
    val type: EventType,
    val startDateTime: LocalDateTime,
    val endDateTime: LocalDateTime,
    val location: String,
    val note: String,
    val priority: Int = 0
)
```

This is the current model actually present in the repository.

An Event therefore represents one occurrence with a concrete start and end date/time.

Do not reintroduce the old recurrence-oriented fields such as:

```text
occurrences
startTime
endTime
```

unless the architecture is deliberately redesigned and all dependent code is updated together.

---

# 7. Event Types

`EventType` currently contains exactly:

```text
CLASS_OFFLINE
CLASS_ONLINE
TASK
OTHER
```

---

# 8. Event Priority

`Event` currently contains:

```kotlin
val priority: Int = 0
```

No additional priority system is currently implemented in the UI or widget code.

For future work, keep priority attached to the Event rather than creating a separate day-level database model unless there is an explicit architectural reason to change this.

---

# 9. Local Persistence

Room is currently configured with one entity:

```text
EventEntity
```

The database is:

```kotlin
@Database(
    entities = [EventEntity::class],
    version = 2,
    exportSchema = false
)
```

`EventEntity` currently stores:

```text
id
 title
type
startDateTime
endDateTime
location
note
priority
```

The date/time values are stored as `String` values in Room.

The repository converts them using:

```kotlin
LocalDateTime.parse(...)
```

and writes them using:

```kotlin
LocalDateTime.toString()
```

`EventType` is persisted using its enum name and reconstructed with `EventType.valueOf(...)`.

---

# 10. EventDao

`EventDao` currently provides:

```text
getAll(): Flow<List<EventEntity>>
getById(id): EventEntity?
insert(event)
update(event)
delete(id)
```

`insert` uses:

```text
OnConflictStrategy.REPLACE
```

This is the current persistence API. Do not assume more repository/database operations exist without checking the source.

---

# 11. EventRepository

`EventRepository` is a Hilt-injected singleton.

Current public operations are:

```text
getAllEvents()
getEventById(id)
saveEvent(event)
deleteEvent(id)
```

It maps:

```text
EventEntity ↔ Event
```

The repository is currently the application-facing abstraction over the Room DAO.

---

# 12. Sample Data

Sample data is separated from `MainActivity` into:

```text
app/src/main/java/com/nexta/data/sample/SampleEvents.kt
app/src/main/java/com/nexta/data/sample/SampleDataSeeder.kt
```

`SampleDataSeeder.seedIfEmpty(repository)` checks whether the repository already contains Events and inserts the sample Events only when the database is empty.

The sample set currently contains these concrete Events:

```text
Thiết kế phần mềm
CLASS_OFFLINE
2026-09-07 09:40 → 2026-09-07 11:25
302 - A9 - Cơ sở 1 - Khu A

Tiếng Anh Công nghệ thông tin 1
CLASS_OFFLINE
2026-09-07 12:30 → 2026-09-07 14:10
308 - A9 - Cơ sở 1 - Khu A

Phát triển ứng dụng thương mại điện tử
CLASS_OFFLINE
2026-09-08 15:10 → 2026-09-08 17:45
402 - A9 - Cơ sở 1 - Khu A

Cơ sở dữ liệu
CLASS_OFFLINE
2026-09-09 08:45 → 2026-09-09 10:30
205 - A9 - Cơ sở 1 - Khu A

Tiếng Anh Công nghệ thông tin 1
CLASS_OFFLINE
2026-09-10 12:30 → 2026-09-10 14:10
308 - A9 - Cơ sở 1 - Khu A

Kiểm thử phần mềm
CLASS_ONLINE
2026-09-12 07:00 → 2026-09-12 09:35
Khu A_PH Online 05 - Khu A_Online - Cơ sở 1 - Khu A

Thiết kế phần mềm
CLASS_OFFLINE
2026-09-14 09:40 → 2026-09-14 11:25
302 - A9 - Cơ sở 1 - Khu A
```

These are test/development data, not a production import system.

---

# 13. Current Main UI

`MainScreen.kt` is currently a simple Compose screen.

It receives:

```kotlin
fun MainScreen(
    events: List<Event>,
    onAddEvent: () -> Unit = {}
)
```

The screen currently has:

- Material 3 `Scaffold`
- `TopAppBar` with title `NextA`
- title text `Lịch của bạn`
- subtitle `Hôm nay và các sự kiện sắp tới`
- `+ Thêm sự kiện` button
- empty-state text when there are no Events
- up to the first 5 Events rendered as simple text cards

Each Event card currently shows:

```text
title
startDateTime → endDateTime
location (when non-blank)
```

Important:

> The `onAddEvent` callback exists in `MainScreen`, but the actual Add Event screen/navigation is **not present on GitHub main yet**.

Therefore the Add Event button currently has no implemented flow when `MainScreen` is called with its default callback.

---

# 14. ScheduleStateEngine — Actual Current State

`ScheduleStateEngine.kt` currently exposes:

```kotlin
fun calculateResult(
    events: List<Event>,
    now: LocalDateTime
): ScheduleResult
```

It calculates:

```text
current
next
```

Current logic:

- `current` = an Event where `startDateTime <= now < endDateTime`, selecting the one with the earliest `endDateTime`.
- `next` = an Event whose `startDateTime` is after `now`, selecting the earliest `startDateTime`.

It returns:

```kotlin
ScheduleResult(
    current = current,
    next = next
)
```

The engine does not currently return an explicit `UPCOMING`, `IN_PROGRESS`, or `NO_MORE` state.

---

# 15. ScheduleState.kt — Actual Current State

The file exists:

```text
app/src/main/java/com/nexta/domain/ScheduleState.kt
```

but it is currently empty.

Therefore the following must **not** be described as implemented:

```text
UPCOMING
IN_PROGRESS
NO_MORE
```

They can be future design goals, but they are not currently represented by the code in this file.

---

# 16. Widget — Actual Current State

The widget package exists:

```text
app/src/main/java/com/nexta/ui/widget
```

but the only current file is:

```text
ScheduleWidgetWorker.kt
```

and that file is empty.

There is currently no verified Jetpack Glance widget implementation on GitHub main.

There is therefore currently no implemented:

- seven-day widget header
- current/next widget cards
- widget countdown
- priority day highlighting
- widget refresh logic
- widget receiver/provider
- widget configuration metadata

These are future work, not completed work.

---

# 17. Manual Event Creation — Current State

The repository currently has the beginning of the UI contract:

```text
MainScreen
    ↓
onAddEvent callback
```

However, there is currently no `AddEventScreen.kt` on GitHub main and no navigation/state implementation in `MainActivity.kt`.

Therefore manual Event creation is **not implemented yet**.

The intended future minimum form can be:

```text
Title
Type
Date
Start time
End time
Location
Note
Priority
```

The created object should be the existing `Event` model, saved through `EventRepository.saveEvent(...)`.

Do not create a separate manual-event data model.

---

# 18. Legacy Code

The repository still contains the older package:

```text
app/src/main/java/com/example/nexta
```

including:

```text
MainActivity.kt
ui/theme/Color.kt
ui/theme/Theme.kt
ui/theme/Type.kt
```

This creates a split between the legacy package and the active `com.nexta` package.

Do not blindly delete the legacy package. First verify whether any build configuration, manifest entry, imports, tests, or resources still depend on it.

---

# 19. Current Build/Architecture Summary

The code currently forms this working path:

```text
SampleEvents
     ↓
SampleDataSeeder
     ↓
EventRepository
     ↓
Room / EventDao
     ↓
Flow<List<Event>>
     ↓
MainActivity
     ↓
MainScreen
```

The repository also contains the beginnings of a domain layer:

```text
Event
  ↓
ScheduleStateEngine
  ↓
ScheduleResult
```

But the domain result is not yet connected to the main UI.

The widget path is not implemented yet.

---

# 20. What Is Actually Done

Based only on the current GitHub `main` source:

### DONE

- Android application exists.
- Active package `com.nexta` exists.
- Hilt application setup exists.
- Room database exists.
- `EventEntity` exists with concrete date/time fields.
- `EventDao` exists.
- `EventRepository` exists.
- Concrete `Event` model exists.
- `EventType` exists.
- `ScheduleResult` exists.
- `ScheduleStateEngine` exists with current/next calculation.
- Sample Event data is separated into the sample package.
- Sample data seeding exists.
- Basic Compose main screen exists.
- Main screen displays Events from the repository.
- Main screen contains an Add Event button/callback contract.

### PARTIAL

- Schedule/domain logic exists, but explicit schedule states are not implemented.
- Manual Event creation UI has only the button/callback entry point; the form and navigation are missing.
- Widget package exists, but there is no working widget implementation.
- Project contains both active and legacy package trees.

### NOT IMPLEMENTED / FUTURE

- `UPCOMING` / `IN_PROGRESS` / `NO_MORE` enum/state model.
- Countdown presentation.
- Seven-day widget overview.
- Widget priority highlighting.
- Glance widget provider/receiver/configuration.
- Widget refresh/update mechanism.
- Add Event form.
- Add Event navigation.
- Edit Event UI.
- Production timetable import.
- OCR/AI import.
- Backend/cloud synchronization.

---

# 21. Development Rules for Future AI Agents

This section is intentionally strict.

## Rule 1 — Trust code over old documentation

Before modifying anything, inspect the current repository.

This file describes the current GitHub state, but source code remains the final authority for implementation details.

## Rule 2 — Do not resurrect old recurrence code

Use concrete Events with:

```text
startDateTime
endDateTime
```

Do not reintroduce old `occurrences` / recurring-rule fields merely because they existed in an earlier version.

## Rule 3 — Do not claim a feature is done without source evidence

For example:

```text
ScheduleState.kt exists
```

does NOT mean explicit schedule states are implemented when the file is empty.

Likewise:

```text
ScheduleWidgetWorker.kt exists
```

does NOT mean the widget exists when the file is empty.

## Rule 4 — Keep changes narrow

When implementing a feature, modify only the files necessary for that feature.

Do not allow an AI agent to rewrite unrelated architecture, package names, Gradle configuration, or data models without a clear reason.

## Rule 5 — Preserve the existing Event model

The current domain object is:

```kotlin
Event(
    id,
    title,
    type,
    startDateTime,
    endDateTime,
    location,
    note,
    priority
)
```

Future sources should produce this same Event type.

## Rule 6 — Verify build after structural changes

After changing Gradle, Room, Hilt, KSP, package names, or major Kotlin files, run a real build and fix compile errors before continuing to the next feature.

## Rule 7 — Do not use the empty widget worker as if it were working code

The widget must be implemented deliberately from scratch when that phase begins.

## Rule 8 — Do not silently modify the project direction

New ideas may be proposed, but they must not be treated as current requirements until explicitly accepted.

---

# 22. Recommended Next Development Order

Based on the current repository, a sensible sequence is:

```text
1. Stabilize / verify current com.nexta project
        ↓
2. Implement manual Add Event form
        ↓
3. Connect Add Event form → EventRepository.saveEvent()
        ↓
4. Improve ScheduleStateEngine / explicit state model
        ↓
5. Add tests for schedule boundaries
        ↓
6. Implement Jetpack Glance widget
        ↓
7. Connect widget to repository/domain logic
        ↓
8. Add countdown and widget refresh behavior
        ↓
9. Clean up legacy com.example.nexta code after dependency audit
```

Do not jump to OCR, AI import, backend, or other large features before the local Event → Repository → Domain → Widget path is stable.

---

# 23. Important Current-State Warning

This document deliberately does **not** preserve the previous project-context claims such as:

```text
"Phase 1 DONE"
"Phase 2 DONE"
"Phase 3 DONE"
"Widget implemented"
"Manual Add Event implemented"
```

unless the current GitHub source actually supports those claims.

The purpose of this rewrite is to establish a clean baseline from the repository that exists now.
