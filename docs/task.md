# NextA Current Task

## Status
IN PROGRESS

## Product Source of Truth
- **Active implementation:** `flutter/` on branch `flutter-v2`.
- **Legacy reference only:** `app/` Android/Kotlin. Do not modify it for Flutter feature work.
- `docs/CLAUDE_FLUTTER_V2.md` is the implementation contract for external AI agents.

## Current Objective
Stabilize and normalize the Flutter planner UI, then finish the remaining product work: series-aware recurrence operations, AI-assisted bulk import, platform-surface verification, and broader responsive verification.

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

## Current UI Normalization
- Calendar markers must be **vertical**: maximum two bars, equal width, stacked on the Y axis. Priority changes color only.
- Agenda start/end time must be prominent, centered, and use the same standardized right-side width as the countdown surface.
- Countdown label/value must be centered.
- Editor `Địa chỉ` and `Ghi chú` leading icons must align with the other editor icons; avoid the default 48px `prefixIcon` inset when it causes drift.
- Consolidate repeated UI dimensions, typography, icon slots, gutters, radii, and event-time sizing into reusable constants/components.
- Remove dead widgets, unused callbacks/imports, duplicated calculations, and obsolete UI code while preserving behavior.

## Verified / User-Reported
- Samsung S23 runtime verification has passed for month/week collapse/expand and directional navigation.
- Recurrence finite generation and deletion flows have previously been runtime-tested by the user.

## Remaining Work
1. Finish UI normalization/refactor for Calendar, Agenda, and Event Editor.
2. Verify the normalized UI on Samsung S23.
3. Finish/verify series-aware recurrence edit/delete behavior.
4. Replace `AI Import` placeholder with AI-assisted bulk import, preview, validation, and persistence.
5. Add bulk-import integration tests.
6. Re-verify Home Widget / Focus-Lock after data integration.
7. Broader cross-device responsive verification.
8. Final cleanup and release-readiness pass.

## Working Boundaries
- Do all Flutter product work on `flutter-v2`.
- Treat `app/` as historical/technical reference only.
- Core schedule semantics remain independent from UI surfaces.
- SQLite is Flutter's current local source of truth; Room is legacy Android reference only.
- Home Widget uses `RemoteViews`; do not introduce Glance unless explicitly requested.
- Do not claim tests/device behavior unless actually run.
