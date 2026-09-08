# Current Task

## Status
IN PROGRESS

## Current Objective
Refine the App Screen and Home Widget so navigation and countdown information stay useful without unnecessary visual or background work.

## Current Change
- App day planner keeps day-by-day horizontal paging for the main schedule.
- The seven-day strip is its own horizontally swipable week pager; swiping it moves the main schedule by seven days while preserving the selected weekday.
- The date/week header is centered and opens the Material 3 date picker for long-range jumps.
- The seven-day strip has no filled selected-day border or large selection box.
- Countdown is useful only within the next 14 days. For durations of 24 hours or more, show days instead of hours/minutes.
- Home Widget keeps exactly two event slots: current event first, then the next upcoming event; when there is no current event, show the next two upcoming events.
- Widget hierarchy remains status -> title -> time -> countdown -> location -> note, with countdown visually emphasized in red.
- Widget countdown is hidden for events more than 14 days away, and minute-level refresh is avoided while the next relevant event is farther away.

## Navigation Model
1. Main schedule swipe — one day at a time.
2. Week strip swipe — one week at a time, preserving weekday.
3. Date picker — direct jump across the supported multi-year range.
4. Today — remains the initial starting position.

## Constraints
- App Screen, Home Widget, and Focus/Lock Screen remain separate UI surfaces.
- Preserve the existing theme/color system.
- Keep the home widget launcher-compatible; avoid decorative nested RemoteViews that previously broke 4x2 installation.
- Do not restore filled selected-day boxes or event-count labels.

## Verification
Build and run in the user's Android environment. Check:
- Main screen opens on today.
- Main schedule swipe changes one day.
- Week strip swipe changes one week.
- Week strip and main schedule stay synchronized.
- Date picker jumps to the correct day.
- Countdown uses hours/minutes below 24h, days from 24h through 14 days, and is hidden beyond 14 days.
- Widget shows current + next, or next two when there is no current event.
- Widget countdown is red and hierarchy remains readable in 4x2.
