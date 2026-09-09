# Current Task

## Status
IN PROGRESS

## Current Objective
Finish the current App Screen/Home Widget work and verify the separate Focus/Lock Screen surface against real Android device capabilities.

## Verified Current State
- Core Event model, Room persistence, repository, Hilt, sample seeding, and `MainViewModel` are implemented.
- Add Event form, validation, persistence, and long-press delete are implemented.
- Main Screen has day-by-day paging, an independent seven-day week pager, synchronization between them, centered date/week navigation, and Material 3 date picker jumps.
- Main Screen shows event status and countdowns; countdown switches to days at 24 hours and is intended to be relevant only within the next 14 days.
- Home Widget uses a dedicated `AppWidgetProvider` with `RemoteViews`, 4x2 layout, current/next selection, fallback to the next two events, and refresh scheduling.
- A separate Focus provider exists with a countdown-oriented presentation, but that does not prove that Android/OEM Lock Screen placement is available.
- `ScheduleState` is now implemented with `PAST`, `IN_PROGRESS`, and `UPCOMING`.

## Current Issues / Gaps
1. Home Widget countdown refresh/horizon still needs to match the app's 14-day policy consistently.
2. Home Widget visual hierarchy needs final refinement while keeping the layout simple and launcher-compatible.
3. Home Widget must be tested for actual 4x2 installation/rendering on target launchers/devices.
4. Focus/Lock Screen behavior must be verified on representative devices; do not assume an AppWidget provider can appear on Lock Screen.
5. Notification fallback should be defined only after confirming which dedicated Lock Screen surfaces are actually available on the target devices.
6. Event editing is not implemented yet.

## Navigation Model
1. Main schedule swipe — one day at a time.
2. Week strip swipe — one week at a time while preserving the selected weekday.
3. Centered date/week header — open Material 3 date picker for direct jumps.
4. Today — initial starting position.

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

## Verification
Build and run in the Android environment. Check:
- App opens on today.
- Main schedule swipe changes exactly one day.
- Week strip swipe changes exactly one week and preserves weekday selection.
- Date picker jumps to the correct date.
- Event status transitions correctly.
- Countdown uses hours/minutes below 24h, days from 24h through 14 days, and is hidden beyond 14 days where the surface supports hiding.
- Home Widget shows current + next, or next two when there is no current event.
- Home Widget remains readable and installable at 4x2.
- Home Widget refreshes at meaningful event/time boundaries without unnecessary long-horizon work.
- Focus provider behavior is tested separately from actual Lock Screen availability.
- Notification fallback is validated on devices where a dedicated Lock Screen surface is unavailable.

## Next Action
Bring the Home Widget countdown horizon and visual hierarchy into final consistency with the App Screen, then perform target-device verification. After that, validate Focus/Lock Screen capabilities before considering any OEM-specific adapter.
