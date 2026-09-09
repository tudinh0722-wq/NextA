# NextA Architecture

## Application
- Android application namespace: `com.nexta`.
- Kotlin + Jetpack Compose + Material 3.
- Java/Kotlin target: Java 17.
- Current toolchain: AGP `9.3.2`, Kotlin `2.2.10`, KSP `2.2.10-2.0.2`, Compose BOM `2026.02.01`, Room `2.8.4`, Hilt `2.59.2`, `compileSdk/targetSdk = 37`, `minSdk = 26`.

## Layers

```text
Compose UI / Platform Surfaces
          ↓
       ViewModel
          ↓
      Repository
          ↓
       Room DAO
          ↓
     Room Database
```

Core Event/countdown semantics stay independent from UI surfaces, Android versions, launchers, and OEMs.

## Data and Persistence

`com.nexta.data.model`
- `Event` is the concrete dated occurrence model.
- `EventType` defines event categories.
- `AlarmSettings` contains only reminder configuration/state; it does not duplicate Event title, note, or start time.

`com.nexta.data.local`
- `AppDatabase` is the single local persistence root.
- `events` stores event content.
- `event_alarms` stores the optional one-to-one reminder configuration for an event.
- `EventAlarmEntity.eventId` is a foreign key to `events.id` with `CASCADE` delete/update.
- Room database version is `3`.
- `MIGRATION_2_3` creates the alarm table without destructive fallback.
- Date/time values are persisted as strings and mapped through `LocalDateTime`.

`com.nexta.data.repository`
- `EventRepository` owns atomic event + alarm writes through `EventScheduleDao`.
- `AlarmRepository` reads/updates alarm state and resolves its related Event from Room.
- `LegacyAlarmMigrator` performs a one-time migration from the old `nexta_alarms` SharedPreferences JSON store into Room, then clears the legacy store.

### Event + Alarm write contract

```text
Create/Edit Event
      ↓
EventRepository.saveEvent(event, alarm)
      ↓
Room transaction
 ├── events
 └── event_alarms
```

Deleting an event removes its alarm row through the Room foreign-key cascade. The AlarmManager is not a second source of truth; it is only the runtime scheduling layer.

### Alarm runtime contract

```text
Room = source of truth
        ↓
AlarmScheduler = AlarmManager adapter
        ↓
AlarmReceiver = read Event + AlarmSettings from Room
        ↓
TTS / alarm tone / notification
```

ACK updates `event_alarms.acknowledged` in Room and cancels scheduled repeat PendingIntents. Boot, time changes, timezone changes, and exact-alarm permission changes rebuild runtime alarms from Room.

## Dependency Injection
- Hilt provides Room database/DAOs and repositories.
- Platform receivers use Hilt injection for Room-backed repositories.

## Presentation Surfaces

NextA intentionally has different presentation surfaces. They share data/domain semantics but are not required to share implementation or pixel-perfect UI.

### 1. App Screen
Weekly/daily planner optimized for navigation and detailed inspection.

### 2. Home Widget
Today Schedule optimized for glanceable current/upcoming events and countdown.
- Traditional Android `RemoteViews`.
- Target size 4x2.
- Launcher compatibility is a priority.
- Do not introduce Jetpack Glance unless explicitly requested.

### 3. Focus / Lock Screen Surface
Now & Next optimized for a large countdown and glanceable information.

An Android AppWidget must not be assumed to work on the Lock Screen across Android versions and OEMs. Lock-screen availability is a platform capability, not a universal permission.

### 4. Notification / Lock-Screen Fallback
Notifications are a first-class cross-device fallback when a dedicated Lock Screen surface is unavailable.

## Cross-Device Strategy

> Same product semantics, adaptive presentation.

Use capability-oriented decisions such as:

```text
supportsHomeWidget
supportsLockScreenSurface
supportsNotifications
supportsDynamicColor
supportsExactAlarm
```

over device-brand checks. OEM-specific integrations must stay isolated adapters.

## Countdown / Refresh Model
- No per-event/per-second countdown timers.
- App countdown refreshes on useful minute boundaries and event transitions.
- Widget providers calculate current/next state from event timestamps and refresh only at useful boundaries.
- Countdown is shown only within the supported 14-day horizon; durations of 24 hours or more use days.

## Important Constraints
- Room is the only persistent source of Event and alarm state.
- Do not reintroduce a second alarm store in SharedPreferences/DataStore.
- Do not duplicate Event title/note/start time inside alarm persistence.
- Do not merge App Screen, Home Widget, and Focus/Lock Screen into one UI.
- Do not introduce Glance unless explicitly requested.
- Do not claim device/launcher support without real-device verification.
