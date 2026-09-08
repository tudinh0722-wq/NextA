# Current Task

## Status
IN PROGRESS

## Current Objective
Refine the App Screen daily planner navigation for both nearby day-by-day use and long-range academic planning.

## Current Change
Commit `9d96de49f3134c5eb3192e7df20e6208b02d8193` adds long-range date navigation:
- Keep day-by-day horizontal swipe as the primary interaction.
- Expand the day pager to five years before and five years after today.
- Add a Material 3 date picker opened from the week/date header.
- Selecting a date jumps directly to that day instead of requiring hundreds of swipes.
- Keep the seven-day indicator focused on the week containing the viewed day.
- Keep today as the initial page when opening the app.
- Preserve the event-card hierarchy, red countdown, long-press delete, and circular `+` FAB.

## Navigation Model
1. Nearby dates — swipe left/right one day at a time.
2. Long-range dates — tap the date/week header and choose a date directly.
3. Today — remains the initial starting position and can be reached directly from the date picker.
4. Supported planning range — approximately five years before/after today.

## Constraints
- App Screen remains a separate UI surface from Home Widget and Focus/Lock Screen.
- Preserve the existing project architecture.
- Keep the existing theme/color system.
- Do not restore filled selected-day boxes or event-count labels.
- Do not make users swipe hundreds of days to reach a distant event.

## Verification
Build and run the app in the user's Android environment. Check:
- App opens on today.
- Swiping changes one day at a time.
- Header opens the date picker.
- Date picker allows navigation across multiple years.
- Selecting a date jumps to the correct day/week.
- Event cards and countdown remain unchanged.
- Long press still opens delete confirmation.
