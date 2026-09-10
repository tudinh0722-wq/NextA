# NextA Flutter v2 Architecture

## 1. Direction

NextA v2 is a Flutter-first cross-platform application targeting Android and iOS.

The existing Android implementation remains the reference implementation on `main`. This branch is the migration boundary; Android-specific implementation details are not copied into Flutter unless they express a product requirement or a platform capability that still needs a native adapter.

## 2. Core rule

Product semantics must not depend on Android, iOS, launcher, OEM, or widget technology.

The core model and use cases own:
- Event semantics and concrete dated occurrences.
- Event types, priority, location, note, start/end time.
- Recurrence generation when recurrence is introduced.
- Countdown semantics and shared refresh boundaries.
- Relevant-event selection for presentation surfaces.
- Import/AI parsing contracts and validation rules.

Platform adapters own capabilities that Flutter cannot provide consistently by itself:
- Android Home Screen widgets using RemoteViews.
- Android alarms and background scheduling.
- Android Lock Screen / Focus surface where the OS and device support it.
- Android notification fallback.
- iOS WidgetKit.
- iOS notifications and background scheduling.
- Any future OS-specific integrations.

## 3. Layering

```text
Flutter UI
  -> Application
      -> Domain
      -> Repositories
          -> Local Data

Platform adapters are reached through capability-oriented interfaces.

Android / iOS native code
  -> Platform adapters
      -> OS APIs
```

### Presentation

Flutter widgets/screens:
- Planner / Calendar.
- Event creation and editing.
- Search.
- AI bulk import.
- Settings.
- Shared design system and theme.

Presentation owns transient UI state, not persistence or platform scheduling.

### Application

Use cases and coordinators:
- Observe events.
- Create/update/delete events.
- Generate recurrence occurrences.
- Select current/next/relevant events.
- Evaluate countdown display state.
- Import and validate AI-generated event data.
- Coordinate platform refresh requests.

### Domain

Pure Dart models and policies. Domain code must be testable without Flutter bindings or Android/iOS APIs.

### Data

Repository implementations and local persistence. The local database is the source of truth for event data. Do not create a second event store for widgets or alarms.

## 4. Event model

Keep the existing product concept of a concrete event occurrence. Recurring schedules should generate concrete occurrences rather than turning every UI surface into a recurrence engine.

Planned recurrence support uses a nullable recurrence identifier/rule association and concrete generated instances. The exact schema should be finalized before persistence implementation.

## 5. Countdown policy

Countdown calculation is shared and platform-independent:
- Horizon: 14 days.
- Durations of 24 hours or more are displayed in days.
- Do not create one timer per event or per second.
- Refresh at meaningful time boundaries and when the visible event set changes.

The same policy feeds the app, widgets, lock-screen surfaces, and notifications.

## 6. Presentation surfaces

These remain intentionally separate:

1. Main Flutter app: full interaction and calendar navigation.
2. Android Home Screen widget: compact RemoteViews presentation, current + next relevant event.
3. Android Lock Screen / Focus surface: capability-dependent Now + Next and large countdown.
4. Android notification fallback: used when a richer surface is unavailable.
5. iOS WidgetKit: iOS-specific compact presentation.
6. iOS notifications: iOS fallback/background reminder surface.

A platform capability must never be inferred from the existence of another capability. For example, an Android Home Screen widget does not imply Lock Screen support.

## 7. UI direction

The planner should preserve the current product direction: Samsung Calendar-inspired information hierarchy, clean/minimal presentation, Material 3 semantics where appropriate, dynamic/system colors, selected-date agenda, month/week navigation, search, and fast event creation.

Flutter should reproduce the behavior and visual hierarchy, not mechanically port Compose components.

## 8. Dependency rules

- Domain must not import Flutter.
- Application may depend on domain abstractions but not Android/iOS APIs.
- Data implements repository contracts.
- Platform adapters implement capability contracts.
- UI consumes application state and use cases.
- Avoid direct database access from widgets/screens.
- Avoid direct platform API calls from domain/application logic.

## 9. Migration strategy

Build the Flutter app from the inside out:

1. Architecture and contracts.
2. Domain models/policies.
3. Local persistence and repositories.
4. Planner/calendar UI.
5. Event CRUD.
6. Search.
7. AI bulk import.
8. Recurrence.
9. Android platform adapters.
10. iOS platform adapters.
11. Device-specific verification.

Do not maintain two complete app implementations indefinitely. Once Flutter reaches feature parity, the old Android app is reference/history rather than a parallel product.
