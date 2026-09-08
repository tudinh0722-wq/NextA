# Current Task

## Status
IN PROGRESS

## Current Objective
Refine the App Screen daily planner UI and event-card information hierarchy.

## Current Change
Commit `032f456d9bff87f2e5fdcd8e9bd127a7f242f3c1` refines `MainScreen` event-card emphasis:
- Start and end times now use the same 15sp size for a clearer time block.
- Time colors are stronger: start time uses on-surface and end time uses on-surface-variant.
- Countdown is now consistently red for fast visual recognition.
- Countdown is slightly larger and remains bold.
- The `SẮP TỚI` status label is increased to 10sp with more padding.
- Existing swipe-between-days navigation, no event-count labels, and no filled selected-day box are preserved.

## Event Card Hierarchy
1. Event title — largest/strongest.
2. Start/end time — same size, visually prominent as a time block.
3. Countdown — red and bold for quick recognition.
4. Location — secondary.
5. Note — smallest/lowest emphasis.
6. Status — compact but clearly readable supporting label.

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
- Start and end times are visibly the same size.
- Time block is more prominent than before.
- Countdown is clearly red and bold.
- `SẮP TỚI` is easier to read.
- App opens on today and swiping left/right changes the day.
- No selected-day filled box or event-count labels appear.
- Long press still opens delete confirmation.
