# NextA Changelog

This file records recent implementation decisions that may not be obvious from source code. Keep it concise; archive older entries when it becomes large.

## 2026-09-08 — Countdown Horizon and Home Widget Refinement

Updated `MainScreen`, `NextAWidgetProvider`, and `nexta_widget.xml`.

Decisions:
- Countdown is shown only for events within the next 14 days.
- From 24 hours onward, countdown uses days rather than hours/minutes.
- Main schedule continues to refresh one shared `now` value every 30 seconds instead of creating one timer per event.
- Home Widget still presents exactly two events: current + next, or next two when there is no current event.
- Widget hierarchy remains status -> title -> time -> countdown -> location -> note.
- Widget countdown is red and more visually prominent.
- Widget avoids minute-level alarm refreshes when the next relevant event is more than 14 days away.

## 2026-09-08 — Centered Week Strip and Dual-Level Swipe Navigation

Updated `MainScreen` in commit `703c455281c6f3dbd1b4c795b5122d14d2c20418`.

Decisions:
- The schedule remains a day pager: swiping the main schedule moves exactly one day.
- The seven-day strip is now its own horizontal pager: swiping the strip moves exactly one week.
- Week-strip navigation preserves the currently selected weekday.
- Day and week pagers synchronize automatically.
- The week strip is centered and intentionally minimal: no border and no large filled selected-day container.
- The week/date header is centered and remains the entry point to the Material 3 date picker.
- The five-year-before/after range and direct date picker remain available for long-range planning.

## 2026-09-08 — Long-Range Daily Planner Navigation

Updated `MainScreen` in commit `15c927f5d92d4361eb2d29da768283336e633b17`.

Decisions:
- Keep the App Screen as a day-by-day horizontal swipe experience rather than changing the pager to week-at-a-time navigation.
- Keep the seven-day indicator as the user's local week context.
- Keep the week range header synchronized with the viewed date.

## 2026-09-08 — App Screen Daily Planner Refinement

Updated `MainScreen` in commit `df00c4bd826388a3ea6c3a98155a2e925c4119b3`.

Decisions:
- The app opens on today.
- The seven-day indicator is lightweight rather than a large filled selected-day control.
- Removed `1 lịch / 2 lịch` labels.
- Event cards use explicit hierarchy: title strongest, start/end time prominent, countdown accent/bold, location secondary, note lowest emphasis.
- Existing long-press deletion and circular `+` FAB are preserved.

## 2026-09-08 — AI Context Bootstrap

Added repository-level AI context so different coding agents can work from the same source of truth instead of relying on chat history.

Files added:
- `AGENTS.md`
- `docs/requirements.md`
- `docs/architecture.md`
- `docs/plan.md`
- `docs/task.md`
- `docs/changelog.md`

Bootstrap commits:
- `47fbf498225d4335ff46d55ca5591eec03e1dad9` — AI instructions
- `9af42857d6935f613de7204d09e84b5a18b82ede` — requirements
- `971b15bdb67ab922e4062c86ce2b455bd5892da6` — architecture
- `356241a0c8ff098b1e4821a5aba7684d4481bf8b` — development plan
- `c15e734fe9863399226ca8149dbe16345e7a9f81` — current task context

## Product Boundaries
NextA intentionally has three different surfaces:
1. App Screen — Weekly Planner.
2. Home Widget — Today Schedule.
3. Focus/Lock Screen — Now & Next with large countdown.

Do not merge their visual responsibilities.

## Home Widget 4x2 Compatibility
The Home Widget uses traditional `RemoteViews`. A more decorative nested-card layout previously caused the launcher to report that the 4x2 widget could not be added. Commit `9168bed6afc134aebc04e7015e3de7578f27abdb` simplified the layout again.

When modifying the Home Widget, prioritize launcher inflation compatibility and incrementally add visual complexity only after installation remains reliable.
