# NextA Current Task

## Purpose of This File
This file is the implementation hand-off for the next coding agent/AI. It describes **what to do next, what is already true, and which product/architecture decisions must not be accidentally undone**.

For broader architecture and technology context, read `docs/architecture.md`. For the rationale/history of important decisions, read `docs/changelog.md`. `AGENTS.md` defines the repository-level working rules. Source code is the final authority for what is actually implemented.

**Do not use an old phase/status document or chat history as the implementation source of truth.** Re-check the current code before changing behavior.

## Status
IN PROGRESS

## Current Objective
Finish and verify the Home Widget implementation against the same countdown semantics as the App Screen, then validate the Focus/Lock Screen strategy on real Android devices.

The immediate implementation target is the **Home Widget countdown refresh/horizon and final 4x2 presentation**.

## Verified Current State
- Core Event model, Room persistence, repository, Hilt, sample seeding, and `MainViewModel` are implemented.
- Add Event form, validation, persistence, and long-press delete are implemented.
- Main Screen has day-by-day paging, an independent seven-day week pager, synchronization between them, centered date/week navigation, and Material 3 date picker jumps.
- Main Screen shows event status and countdowns; countdown changes to days at 24 hours and is intended to be relevant only within the next 14 days.
- Home Widget uses a dedicated `AppWidgetProvider` with traditional `RemoteViews`, a 4x2 layout, current/next selection, fallback to the next two events, and refresh scheduling.
- A separate Focus provider exists with a countdown-oriented presentation, but that does **not** prove Android/OEM Lock Screen placement is available.
- `ScheduleState` is implemented with `PAST`, `IN_PROGRESS`, and `UPCOMING`.
- `NEXTA_PROJECT_CONTEXT.md` has been retired. Its useful context was moved into repository documentation rather than keeping a stale second source of truth.

## Decision Context — Read Before Coding
These are not arbitrary implementation preferences. They are product/architecture decisions made to keep NextA maintainable across Android versions, launchers, OEMs, and future surfaces. **Do not reverse them just to make the immediate task easier.**

### 1. One product model, multiple presentation surfaces
NextA has separate surfaces because they serve different jobs:

```text
Shared Event + Schedule Semantics
              ↓
    ┌─────────┼───────────┐
    ↓         ↓           ↓
 App Screen  Home Widget  Focus/Lock
                              ↓
                       OS/OEM capability
                              ↓
                    Notification fallback
```

- App Screen = detailed weekly/daily planning.
- Home Widget = quick Today Schedule glance.
- Focus/Lock Screen = Now & Next with a large countdown.
- Notification = cross-device fallback when a dedicated Lock Screen surface is unavailable.

They should share **semantics**, not necessarily identical layouts or implementation.

### 2. Keep core logic platform-independent
Event state and countdown rules belong in shared/domain-level logic, not inside a widget, Compose screen, launcher-specific branch, or OEM branch.

Do not solve a UI problem by duplicating countdown/state logic in another surface.

### 3. Capability-oriented, not brand-oriented
Do not add core logic such as:

```kotlin
if (Build.MANUFACTURER == "samsung") { ... }
```

Prefer capability concepts such as:

```text
supportsHomeWidget
supportsLockScreenSurface
supportsNotifications
supportsDynamicColor
supportsExactAlarm
```

If an OEM-specific integration is genuinely required, isolate it behind a platform/capability adapter and only add it after verifying that the platform actually exposes the needed capability.

### 4. Lock Screen support is not a universal Android AppWidget capability
The existence of a working Home Screen `AppWidgetProvider` does not mean the same widget can be placed on the Lock Screen on every Android device.

Do not invent or assume a universal permission that makes this possible. A permission can grant access to a capability exposed by the OS/OEM; it cannot create a capability the OS/OEM does not expose.

Therefore, Focus/Lock Screen work must be verified on real representative devices before designing an OEM adapter. Notifications remain the robust fallback.

### 5. Material/system semantics over hard-coded device colors
Compose should use Material 3 semantic tokens. Restricted surfaces such as `RemoteViews` may require different implementation details, but the visual intent should remain consistent.

Do not introduce arbitrary hard-coded background colors merely to force visual parity across devices.

### 6. Home Widget deliberately uses RemoteViews
The Home Widget uses traditional Android `RemoteViews` rather than Jetpack Glance because launcher compatibility is currently more important than adopting a newer abstraction.

A previous decorative/nested-card 4x2 implementation caused launcher installation/inflation problems. Therefore:
- keep the widget hierarchy simple;
- add visual complexity incrementally;
- verify actual installation/rendering after layout changes;
- do not switch to Glance unless explicitly requested.

### 7. Countdown policy is a product decision
The countdown is deliberately bounded because a far-future countdown has little glance value and creates unnecessary refresh work.

Rules:
- Countdown is relevant only within the next 14 days.
- Below 24 hours: show hours/minutes.
- At 24 hours or more: show days.
- Do not create one timer per event or a per-second timer.
- Recalculate from event timestamps when a surface renders/refreshes.
- Avoid minute-level refresh work when the next relevant event is outside the 14-day countdown horizon.

The App Screen and Home Widget must follow the same semantic rule even though their rendering is different.

### 8. Event model is intentionally concrete
`Event` represents a concrete dated occurrence:

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

Do not reintroduce the old recurrence-oriented fields such as `occurrences`, `startTime`, or `endTime` without an explicit architecture redesign.

## Current Issues / Gaps
1. Home Widget countdown refresh/horizon still needs to match the App Screen's 14-day policy consistently.
2. Home Widget visual hierarchy needs final refinement while preserving simple `RemoteViews`/4x2 compatibility.
3. Home Widget must be tested for actual 4x2 installation and rendering on target launchers/devices.
4. Focus/Lock Screen behavior must be verified on representative devices; do not assume an AppWidget provider can appear on the Lock Screen.
5. Notification fallback should be defined/implemented only after confirming which dedicated Lock Screen surfaces are actually available on target devices.
6. Event editing is not implemented yet.
7. The current widget refresh path should be checked for unnecessary work and for races caused by refreshing immediately after asynchronous event writes.

## Immediate Implementation Task — Home Widget
Work directly from the current source code. Do not redesign the data layer.

### Required behavior
1. Inspect `NextAWidgetProvider` and the widget XML/layout before editing.
2. Keep the existing two-slot rule:
   - if an event is currently in progress: show **current + next**;
   - if no event is in progress: show the **next two upcoming events**.
3. Preserve the information hierarchy:
   - status
   - title
   - start/end time
   - countdown
   - location
   - note
4. Make countdown formatting match the shared product policy:
   - `< 24h` → hours/minutes;
   - `>= 24h` and `<= 14 days` → days;
   - beyond 14 days → no countdown.
5. Align refresh scheduling with that horizon:
   - do not keep unnecessary minute-level refreshes for events beyond the 14-day countdown horizon;
   - still refresh at meaningful boundaries such as an event starting/ending or entering the supported countdown horizon;
   - avoid per-second/per-event timers.
6. Keep the widget compatible with `RemoteViews` and the existing 4x2 target.
7. Do not introduce Glance, a second data source, or duplicated domain semantics.
8. Check whether widget refresh after add/delete can race the asynchronous repository write. If it does, fix the sequencing at the appropriate layer rather than adding arbitrary delays.

### Acceptance criteria
The Home Widget task is complete only when:

- A current event renders with the next event in the second slot.
- With no current event, the two nearest upcoming events render.
- Empty slots do not show stale data from a previous update.
- Countdown formatting follows the exact 24h/14-day rules.
- An event outside the 14-day countdown horizon does not cause pointless minute-by-minute countdown refresh work.
- Event start/end transitions cause the widget to select/update the correct current/next events.
- Location and note remain secondary information and do not break the compact layout.
- The widget remains installable and readable at 4x2 on the target launcher(s).
- No per-second timer or per-event timer is introduced.
- Existing Event/Room/Repository architecture remains intact.

## Navigation Model
1. Main schedule swipe — one day at a time.
2. Week strip swipe — one week at a time while preserving the selected weekday.
3. Centered date/week header — open Material 3 date picker for direct jumps.
4. Today — initial starting position.

These navigation decisions exist because the main schedule and week strip have different navigation granularity: daily inspection versus weekly planning. Do not collapse them into one gesture model without a deliberate product decision.

## Surface / Platform Model
```text
NextA Event + Schedule Semantics
                ↓
      ┌─────────┼────────────┐
      ↓         ↓            ↓
   App UI   Home Widget   Focus/Lock
                              ↓
                       OS/OEM capability
                              ↓
                    Notification fallback
```

Core logic must not contain OEM/brand checks. Platform-specific behavior belongs behind capability detection/adapters. A permission can grant access to an OS-exposed capability; it cannot create a Lock Screen surface that the OS/OEM does not provide.

## Constraints
- Preserve the existing Material/system semantic color approach; do not hard-code device-specific background colors.
- Keep Home Widget launcher-compatible and simple.
- Use `RemoteViews` for the Home Widget; do not introduce Jetpack Glance unless explicitly requested.
- Do not create per-event/per-second countdown timers.
- Keep countdown computation independent from individual presentation surfaces.
- Do not reintroduce the old recurrence-oriented Event fields.
- Do not create a second data/persistence architecture.
- Do not merge App Screen, Home Widget, and Focus/Lock Screen into one UI implementation.
- Do not mark Lock Screen support as complete merely because a Focus `AppWidgetProvider` exists.

## Verification
Build and run in the Android environment. Check:
- App opens on today.
- Main schedule swipe changes exactly one day.
- Week strip swipe changes exactly one week and preserves weekday selection.
- Date picker jumps to the correct date.
- Event status transitions correctly.
- Countdown uses hours/minutes below 24h, days from 24h through 14 days, and is hidden beyond 14 days where the surface supports hiding.
- Home Widget shows current + next, or next two when there is no current event.
- Home Widget clears unused slots instead of leaving stale RemoteViews content.
- Home Widget remains readable and installable at 4x2.
- Home Widget refreshes at meaningful event/time boundaries without unnecessary long-horizon work.
- Add/delete followed by widget refresh shows the newly persisted state, not a stale pre-write state.
- Focus provider behavior is tested separately from actual Lock Screen availability.
- Notification fallback is validated on devices where a dedicated Lock Screen surface is unavailable.

## Documentation Rule for Future AI Agents
Whenever a significant product or architecture decision is made, record **both the decision and the reason** in repository docs:

- `docs/architecture.md` → stable architecture/constraints that future implementation must follow.
- `docs/changelog.md` → dated decision history and rationale that explains why the current approach was chosen.
- `docs/task.md` → only the context needed to safely execute the current task.

Do not create another giant context dump. The goal is a small set of maintained sources of truth that explain both **what the project is** and **why important decisions were made**.

## Next Action After Home Widget
After the Home Widget passes its acceptance criteria:
1. Validate the Focus provider independently.
2. Test actual Lock Screen surface availability on representative Android/OEM devices.
3. Only if a real platform-specific capability is confirmed, design an isolated capability/OEM adapter.
4. Otherwise, implement/validate notification-based Lock Screen fallback.
5. Then return to remaining product gaps such as Event editing and final UI polish.
