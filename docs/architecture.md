# NextA Architecture

## Application
- Android application namespace: `com.nexta`.
- Legacy package `com.example.nexta` exists but is not the active application path; remove it only after dependency/build verification.
- Kotlin + Jetpack Compose + Material 3.
- Java/Kotlin target: Java 17.
- Current Android build configuration uses AGP `9.3.2`, Kotlin `2.2.10`, KSP `2.2.10-2.0.2`, Compose BOM `2026.02.01`, Room `2.8.4`, Hilt `2.59.2`, `compileSdk/targetSdk = 37`, `minSdk = 26`.
- Keep the existing `android.disallowKotlinSourceSets=false` Gradle property unless there is a verified reason to change it.

## Layers

```text
Compose UI / Platform Surfaces
          ↓
       ViewModel
          ↓
        Domain
          ↓
      Repository
          ↓
       Room DAO
          ↓
     Room Database
```

The core event/countdown logic must remain independent from any specific UI surface, Android version, launcher, OEM, or platform.

### Data
`com.nexta.data.model`
- `Event` is the domain-facing event model.
- `EventType` defines event categories.
- `ScheduleResult` contains current/next event results.

Current `Event` shape:

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

Events are concrete dated occurrences. Do not reintroduce the old recurrence-oriented fields such as `occurrences`, `startTime`, or `endTime` without an explicit architecture redesign.

`com.nexta.data.local`
- Room `AppDatabase`, `EventDao`, and `EventEntity`.
- Database version is currently 2.
- Date/time values are persisted as strings and mapped through `LocalDateTime`.

`com.nexta.data.repository`
- `EventRepository` is the application-facing abstraction over Room.
- Reuse it instead of accessing Room directly from UI or platform surfaces.

### Domain
`com.nexta.domain`
- `ScheduleStateEngine` calculates current and next events.
- Explicit schedule-state modeling may evolve, but UI surfaces must consume shared domain semantics rather than duplicating countdown logic.

### Dependency Injection
- Hilt is used for application/ViewModel injection.
- `DatabaseModule` provides Room database/DAO.
- Widget/platform providers may access `EventRepository` through the existing DI boundary.

## Presentation Surfaces

NextA intentionally has different presentation surfaces. They share data/domain semantics but are not required to share implementation or pixel-perfect UI.

### 1. App Screen
Weekly/daily planner optimized for navigation and detailed inspection.

### 2. Home Widget
Today Schedule optimized for glanceable current/upcoming events and countdown.

- Uses traditional Android `RemoteViews`.
- Target size is 4x2.
- Launcher compatibility is a priority.
- Do not introduce Jetpack Glance unless explicitly requested.
- Avoid unnecessary RemoteViews layout complexity.

### 3. Focus / Lock Screen Surface
Now & Next optimized for a large countdown and glanceable information.

Important: an Android `AppWidget` must not be assumed to work on the Lock Screen across Android versions and OEMs. Lock-screen availability is a platform capability, not a universal permission that NextA can force on.

### 4. Notification / Lock-Screen Fallback
Notifications are a first-class cross-device fallback for important countdown information when a dedicated Lock Screen surface is unavailable.

Do not update a notification every second merely to simulate a timer. Recalculate countdown from the event timestamp when the surface is rendered/refreshed and use sensible refresh boundaries.

## Cross-Device / Platform Strategy

The product goal is:

> Same product semantics, adaptive presentation.

It is **not** pixel-perfect identical UI on every device.

Architecture must be capability-oriented rather than brand-oriented. Prefer questions such as:

```text
supportsHomeWidget
supportsLockScreenSurface
supportsNotifications
supportsDynamicColor
supportsExactAlarm
```

over hard-coded checks such as:

```kotlin
if (Build.MANUFACTURER == "samsung") { ... }
```

OEM-specific code is allowed only when a real platform-specific capability exists and should be isolated behind a presentation/capability adapter. Never put Samsung/Pixel/Xiaomi/etc. conditions into the core event or countdown engine.

Platform examples:

```text
Android Home Screen      → AppWidget / RemoteViews
Android Lock Screen      → only where OS/OEM supports it
Android fallback         → Notification / Lock Screen notification
Samsung-specific surface → isolated adapter when officially available
iOS future surface       → WidgetKit / ActivityKit-style adapter if NextA expands to iOS
```

A user permission can enable a capability exposed by the OS, but cannot create a capability that the OS/OEM does not expose. Never design around an assumed universal `LOCK_SCREEN_WIDGET` permission.

## Widget Refresh Model
Widget providers calculate current/next state from the shared event/domain logic, update `RemoteViews`, and schedule refreshes around useful time boundaries. Avoid per-event/per-second timers. The existing countdown policy is:

- Show countdown only within the next 14 days.
- For durations of 24 hours or more, show days rather than hours/minutes.
- Reuse a shared `now` value where possible.
- Avoid minute-level refresh work when the next relevant event is farther away than the supported countdown horizon.

## Design System Boundary
Use shared semantic design tokens rather than hard-coded device-specific colors.

Compose app UI can use Material 3 directly. Restricted surfaces such as `RemoteViews` may require different implementation details, but should preserve the same semantic hierarchy and design intent.

Do not force the same implementation onto all surfaces.

## Important Constraints
- Do not create a second data/persistence architecture.
- Keep the active package under `com.nexta`.
- Do not merge the App Screen, Home Widget, and Focus/Lock Screen into one UI.
- Do not introduce Jetpack Glance unless explicitly requested.
- Keep platform/OEM work isolated from core domain logic.
- Prefer capability detection over device-brand detection.
- Verify actual platform behavior before claiming support.
