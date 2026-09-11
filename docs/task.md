# NextA Current Task

## Status
IN PROGRESS

## Product Source of Truth
- **Active implementation:** `flutter/` on branch `flutter-v2`.
- **Legacy reference only:** `app/` Android/Kotlin. Do not modify it for Flutter feature work.
- `docs/CLAUDE_FLUTTER_V2.md` is the implementation contract for external AI agents.

## Current Objective
Preserve the completed planner UI baseline and finish the remaining product work: series-aware recurrence operations, AI-assisted bulk import, platform-surface verification, and broader responsive verification.

## Completed Foundation
- Flutter month-first planner with month/week collapse and expansion.
- Directional month navigation and whole-week navigation.
- Selected-day agenda, event create/edit/delete, and SQLite persistence.
- SQLite-backed search across title, location, and note.
- Minute-aligned countdown refresh with `xd yh` multi-day formatting.
- Samsung Calendar-inspired Add Event editor.
- Explicit start/end date-time; no All-day mode.
- Shared persisted event priority with pastel blue/orange/red palette.
- Reminder configuration: default 10 minutes before, plus 2 additional reminders at 5-minute intervals.
- Finite recurrence: daily/weekly/monthly UI, count or until end, default 10 occurrences, hard safety cap.
- Recurrence occurrences share `recurrenceId`; no separate recurrence-rule table.
- Calendar quick tap selects a day; long press opens Add Event for that exact day.
- Empty agenda is informational only.
- Home Widget / Focus-Lock architecture remains isolated from planner semantics.

## Completed UI Baseline — DO NOT MODIFY WITHOUT AN EXPLICIT USER REQUEST
- **Calendar screen UI is complete and locked.** Event markers are vertical, limited to two bars per day, equal width, and priority changes color only.
- **Agenda screen UI is complete and locked.** Start/end time and countdown use the standardized right-side time block; time is prominent and centered, and countdown label/value are centered.
- Calendar and Agenda interaction behavior is part of the completed baseline: calendar quick tap selects a day, long press opens Add Event for that exact day, and empty agenda remains informational only.
- Do not redesign, resize, restyle, or refactor the Calendar or Agenda presentation unless the user explicitly requests a change to those screens.
- Do not treat future general UI cleanup as permission to alter these locked screens.

## Current UI Normalization
- Add Event editor icon alignment and shared presentation cleanup remain separate from the locked Calendar/Agenda baseline.
- Consolidate repeated UI dimensions, typography, icon slots, gutters, radii, event-time sizing, and countdown dimensions only where this does not change the locked Calendar or Agenda appearance.
- Remove dead widgets, unused callbacks/imports, duplicated calculations, and obsolete UI code while preserving behavior; locked-screen presentation must remain unchanged.

## Verified / User-Reported
- Samsung S23 runtime verification has passed for month/week collapse/expand and directional navigation.
- Recurrence finite generation and deletion flows have previously been runtime-tested by the user.
- The latest Calendar/Agenda UI baseline is treated as complete by product decision; no further visual changes are planned unless explicitly requested.

## Remaining Work
1. Preserve the locked Calendar and Agenda UI baseline; only fix regressions if the user explicitly requests or a functional bug requires it.
2. Finish/verify series-aware recurrence edit/delete behavior.
3. Replace `AI Import` placeholder with AI-assisted bulk import, preview, validation, and persistence.
4. Add bulk-import integration tests.
5. Re-verify Home Widget / Focus-Lock after data integration.
6. Broader cross-device responsive verification outside the locked UI baseline.
7. Final cleanup and release-readiness pass.

## Working Boundaries
- Do all Flutter product work on `flutter-v2`.
- Treat `app/` as historical/technical reference only.
- Core schedule semantics remain independent from UI surfaces.
- SQLite is Flutter's current local source of truth; Room is legacy Android reference only.
- Home Widget uses `RemoteViews`; do not introduce Glance unless explicitly requested.
- Do not claim tests/device behavior unless actually run.
- **Locked UI rule:** Calendar and Agenda are finished screens. No visual modification without an explicit user request.
