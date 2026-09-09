# Current Task

## Status
IN PROGRESS

## Current Objective
Refine the App Screen and Home Widget while establishing a cross-device presentation strategy so NextA is not tied to one Android version, launcher, OEM, or UI implementation.

## Current Change
- App day planner keeps day-by-day horizontal paging for the main schedule.
- The seven-day strip is its own horizontally swipable week pager; swiping it moves the main schedule by seven days while preserving the selected weekday.
- The date/week header is centered and opens the Material 3 date picker for long-range jumps.
- The seven-day strip has no filled selected-day border or large selection box.
- Countdown is useful only within the next 14 days. For durations of 24 hours or more, show days instead of hours/minutes.
- Home Widget keeps exactly two event slots: current + next, or next two when there is no current event.
- Widget hierarchy remains status -> title -> time -> countdown -> location -> note, with countdown visually emphasized.
- Widget avoids unnecessary minute-level refresh work when the next relevant event is beyond the countdown horizon.
- App Screen, Home Widget, and Focus/Lock Screen are intentionally separate presentation surfaces.
- Cross-device strategy now treats platform/OEM differences as capabilities and adapters rather than core business logic.

## Navigation Model
1. Main schedule swipe — one day at a time.
2. Week strip swipe — one week at a time, preserving weekday.
3. Date picker — direct jump across the supported multi-year range.
4. Today — remains the initial starting position.

## Surface / Platform Model
```text
NextA Core Event + Countdown Logic
                ↓
      ┌─────────┼─────────┐
      ↓         ↓         ↓
   App UI   Home Widget  Focus/Lock
                          ↓
                    Platform capability
                          ↓
                 Notification fallback
```

Do not put OEM-specific conditions into the core event/countdown logic. A dedicated Lock Screen surface is optional and device/OS dependent; a user permission cannot manufacture a capability that the OS/OEM does not expose.

## Constraints
- Preserve the existing theme/color system and prefer semantic Material/system colors over hard-coded device-specific colors.
- Keep the Home Widget launcher-compatible; avoid decorative nested `RemoteViews` that previously broke 4x2 installation.
- Do not restore filled selected-day boxes or event-count labels.
- Do not introduce Jetpack Glance unless explicitly requested.
- Do not create per-event/per-second timers for countdowns.

## Verification
Build and run in the user's Android environment. Check:
- Main screen opens on today.
- Main schedule swipe changes one day.
- Week strip swipe changes one week.
- Week strip and main schedule stay synchronized.
- Date picker jumps to the correct day.
- Countdown uses hours/minutes below 24h, days from 24h through 14 days, and is hidden beyond 14 days.
- Widget shows current + next, or next two when there is no current event.
- Widget remains readable and installable in 4x2.
- Focus/Lock Screen behavior is verified separately on each target platform/device; do not infer support from another OEM.
- Notification fallback is used where a dedicated Lock Screen surface is unavailable and the product explicitly requires Lock Screen visibility.

## Next Action
Finish Home Widget visual refinement and target-device verification, then validate the Focus/Lock Screen capability on representative devices before adding OEM-specific adapters.
