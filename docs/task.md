# Current Task

## Status
IN PROGRESS

## Current Objective
Refine the App Screen daily planner navigation for both nearby day-by-day use and long-range academic planning.

## Current Change
Commit `703c455281c6f3dbd1b4c795b5122d14d2c20418` refines planner navigation:
- Keep day-by-day horizontal swipe in the schedule: one horizontal swipe moves one day.
- Make the seven-day strip a separate horizontal pager: one horizontal swipe on the strip moves one whole week.
- Keep the selected weekday when moving week-to-week from the strip.
- Keep the strip visually lightweight: centered, no bordered/filled selected-day box, selected day uses typography + a small dot.
- Center the week/date header.
- Keep the Material 3 date picker for direct long-range navigation.
- Preserve the five-year-before/after planning range.
- Preserve event-card hierarchy, red countdown, long-press delete, and circular `+` FAB.

## Navigation Model
1. Schedule area — swipe left/right one day at a time.
2. Week strip — swipe left/right one week at a time.
3. Long-range dates — tap the centered date/week header and choose a date directly.
4. Today — remains the initial starting position.

## Constraints
- App Screen remains a separate UI surface from Home Widget and Focus/Lock Screen.
- Preserve the existing project architecture and theme/color system.
- Do not restore filled selected-day boxes or event-count labels.
- Avoid decorative borders around the week selector.
- Do not make users swipe hundreds of days to reach a distant event.

## Verification
Build and run the app in the user's Android environment. Check:
- App opens on today.
- Swiping the schedule changes one day at a time.
- Swiping the week strip changes one week at a time and preserves weekday.
- Day and week navigation stay synchronized.
- Week/date header is centered and opens the date picker.
- Date picker allows navigation across multiple years.
- Selecting a date jumps to the correct day/week.
- Event cards and countdown remain unchanged.
- Long press still opens delete confirmation.
