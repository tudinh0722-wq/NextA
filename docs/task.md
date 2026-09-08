# Current Task

## Status
IN PROGRESS

## Current Objective
Refine the App Screen daily planner navigation and event-card information hierarchy.

## Current Change
Commit `15c927f5d92d4361eb2d29da768283336e633b17` keeps the seven-day visual context and day-by-day swipe interaction while extending the pager to a practical long-range window.

- The app opens on today.
- Users swipe one day at a time, preserving the existing daily-planner UX.
- The pager supports 365 days before today and 365 days after today.
- The seven-day indicator follows the currently viewed date/week.
- The week range header updates as the user swipes across weeks.
- No large filled selected-day box and no `1 lịch / 2 lịch` count labels are restored.
- Event cards keep the current hierarchy: title strongest, start/end time equal size, countdown red and bold, location/note secondary, status more visible.

## Long-Range Navigation Decision
For the current product stage, a one-year window in each direction is preferred over an effectively unbounded pager. This covers the user's realistic academic planning horizon of roughly six to twelve months while avoiding an unnecessarily huge navigation range.

A future release may add a direct date/calendar picker for jumping to dates outside this window instead of making users swipe hundreds of pages.

## Relevant File
- `app/src/main/java/com/nexta/ui/MainScreen.kt`

## Constraints
- App Screen remains a separate UI surface from Home Widget and Focus/Lock Screen.
- Preserve the existing project architecture.
- Keep the existing theme/color system; this task does not change the app theme.
- Swipe is the primary day navigation interaction.
- Countdown refreshes periodically and uses event start/end times.

## Verification
Build and run the app in the user's Android environment. Check:
- App opens on today.
- Swiping one day at a time works across the current week and long-range dates.
- The seven-day indicator updates to the week containing the viewed date.
- Event title is visibly strongest.
- Start and end times have the same size.
- Countdown is red, bold, visible, and changes over time.
- `SẮP TỚI` is clearly readable.
- Location/note remain secondary.
- Long press still opens delete confirmation.
