# NextA Architecture

## Application
- Android application namespace: `com.nexta`
- Legacy package `com.example.nexta` exists in the repository but is not the active application path.
- UI uses Jetpack Compose + Material 3.
- Java/Kotlin target is Java 17.

## Layers

```text
Compose UI
    ↓
ViewModel
    ↓
Repository
    ↓
Room DAO
    ↓
Room Database
```

### UI
`com.nexta.ui`
- `MainScreen.kt`: weekly planner and selected-day event list.
- `AddEventScreen.kt`: event creation form.
- `MainViewModel.kt`: exposes event state and save/delete operations.
- `MainUiState.kt`: loading/success/error state.

### Domain
`com.nexta.domain`
- `ScheduleState`: PAST / IN_PROGRESS / UPCOMING.
- `ScheduleStateEngine`: calculates current and next events.

### Data
`com.nexta.data.model`
- `Event` is the domain-facing event model.
- `EventType` defines event categories.
- `ScheduleResult` contains current/next event results.

`com.nexta.data.repository`
- `EventRepository` maps Room entities to domain models and persists events.

`com.nexta.data.local`
- Room `AppDatabase`, `EventDao`, and `EventEntity`.
- Database version is currently 2.
- Current database builder uses destructive migration fallback.

### Dependency Injection
- Hilt is used for application and ViewModel injection.
- `DatabaseModule` provides Room database/DAO.
- `WidgetEntryPoint` exposes `EventRepository` to widget providers.

## Three UI Surfaces

### 1. App Screen
Weekly Planner. It is optimized for navigating a week and inspecting one selected day.

### 2. Home Widget
Today Schedule. It is optimized for quick glance information: current/upcoming events and countdown. It currently uses traditional Android `RemoteViews`.

Relevant files:
- `NextAWidgetProvider.kt`
- `nexta_widget.xml`
- `nexta_widget_info.xml`

The Home Widget currently targets 4x2 sizing. Launcher compatibility is a priority; avoid unnecessary RemoteViews layout complexity.

### 3. Focus / Lock Screen
Focus / Now & Next. It is intentionally a different UI with a large countdown and glanceable current/next information.

Relevant files:
- `NextAFocusWidgetProvider.kt`
- `nexta_focus_widget.xml`
- `nexta_focus_widget_info.xml`

## Widget Refresh Model
Both widget providers read events through the Hilt `WidgetEntryPoint`, calculate the current/next event from `LocalDateTime.now()`, update `RemoteViews`, and schedule refreshes around minute ticks/event boundaries using `AlarmManager`.

## Important Constraints
- Do not introduce Jetpack Glance unless explicitly requested.
- Do not merge the three UI surfaces into one UI design.
- Do not create a second data/persistence architecture.
- Reuse `EventRepository` rather than accessing Room directly from UI.
- `Event.priority` is already part of the model/entity and is available for weighting.
- Keep the active package under `com.nexta`.
