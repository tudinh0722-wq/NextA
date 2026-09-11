# NextA Current Task

## Status
IN PROGRESS

## Current Objective
Refine the main planner into a polished Samsung Calendar-inspired mobile experience, then implement recurrence and AI-assisted bulk-import work without breaking the existing event flow.

## Current State
- Flutter on `flutter-v2` is the active product implementation; Android/Kotlin remains legacy reference.
- Runtime on Samsung S23 is stable; month↔week collapse/expand and directional month/week navigation have been verified by the user.
- Main planner uses month-first hierarchy, Material 3 semantic colors, selected-day agenda, countdown column, search, event editing, month/week gestures, and contextual add-event action.
- Horizontal month navigation uses a real directional page transition with old/new month surfaces coexisting during the animation.
- Week-view horizontal navigation advances by whole weeks rather than whole months.
- Countdown refresh is aligned to minute boundaries and continues refreshing once per minute while the planner is active.
- Countdown formatting keeps hour precision for multi-day durations (`xd yh`) instead of dropping the hour remainder.
- Agenda countdowns are now enclosed in a priority-colored surface, reusing the same Material 3 semantic severity hierarchy as the editor: primary / tertiary / error.
- The Add Event flow opens a full-height Samsung Calendar-inspired editor from the selected-day FAB, with today as the initial selected day.
- The editor uses explicit start/end date-time fields, title priority color, location, reminder, reminder-repeat settings, recurrence, note, and a floating `Thoát | Lưu` pill.
- Reminder configuration uses a centered popup with direct numeric entry. Defaults are 10 minutes before, then 2 additional reminders at 5-minute intervals if unacknowledged.
- Reminder and repeat rows visibly expose their tap affordance with trailing chevrons.
- The editor header is `Sự kiện | AI Import`, with safe-area handling and extra top spacing so the segment does not sit under the phone status bar/camera cutout.
- New recurring events are expanded into concrete occurrences sharing a `recurrenceId`, including their reminder configuration.
- `AI Import` is reserved for the AI-assisted bulk-import workflow; integration is still pending.
- Platform-specific widget/lock-screen work remains isolated from shared planner semantics.

## Important Boundaries
- Events remain concrete dated occurrences.
- Recurrence series share `recurrenceId`; recurrence metadata is carried on concrete occurrences rather than introducing a separate rule table.
- Reminder repeat count means additional reminders after the initial reminder; actual acknowledgement/alarm delivery remains a separate runtime concern.
- Home Widget and Focus/Lock Screen stay on `RemoteViews`; no Glance.
- Core schedule semantics stay independent from UI surfaces.
- No per-event/per-second countdown timers.
- Lock-screen availability is host/OEM dependent.
- Samsung-inspired UI means interaction hierarchy and visual language, not a Samsung-only implementation.

## Next Action
1. Verify the latest agenda countdown surfaces, multi-day countdown format, and event-editor top inset visually on Samsung S23.
2. Implement series-aware edit/delete with explicit one-event vs entire-series behavior.
3. Replace the `AI Import` placeholder with the AI-assisted bulk-import workflow and preview/validation.
4. Re-run Home Widget / Focus-Lock verification after recurrence/data-model integration.
5. Perform broader cross-device responsive verification after the planner behavior stabilizes.
