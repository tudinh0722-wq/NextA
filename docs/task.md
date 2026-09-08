# Current Task

## Status
IN PROGRESS

## Current Objective
Refine the App Screen daily planner UI and event-card information hierarchy.

## Current Change
Commit `df00c4bd826388a3ea6c3a98155a2e925c4119b3` changes `MainScreen` to:
- Use horizontal swipe/paging between the seven days.
- Keep today as the initial page when opening the app.
- Remove day-card selection backgrounds and the `1 lịch / 2 lịch` count labels.
- Keep a lightweight seven-day indicator without filled selection boxes.
- Add a live countdown to each event card.
- Make event title the strongest text hierarchy.
- Make start time prominent and end time smaller.
- Put countdown below the title with accent emphasis.
- Keep location and note visually secondary.
- Preserve long-press delete and the circular `+` FAB.

## Event Card Hierarchy
1. Event title — largest/strongest.
2. Start time — prominent.
3. Countdown — accent and bold, directly below title.
4. End time — smaller than start time.
5. Location — secondary.
6. Note — smallest/lowest emphasis.
7. Status — compact supporting label.

## Relevant File
- `app/src/main/java/com/nexta/ui/MainScreen.kt`

## Constraints
- App Screen remains a separate UI surface from Home Widget and Focus/Lock Screen.
- Preserve the existing project architecture.
- Keep the existing theme/color system; this task does not change the app theme.
- Swipe is the primary day navigation interaction.
- Do not restore the previous filled selected-day boxes or event-count labels.
- Countdown refreshes periodically and uses event start/end times.

## Verification
Build and run the app in the user's Android environment. Check:
- App opens on today.
- Swiping left/right changes the day.
- No selected-day filled box appears.
- Event title is visibly larger than times.
- End time is visibly smaller than start time.
- Countdown is visible and changes over time.
- Location/note remain secondary.
- Long press still opens delete confirmation.
