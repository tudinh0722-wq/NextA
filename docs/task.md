# NextA Current Task

## Status
IN PROGRESS

## Current Objective
Refine the main planner into a polished Samsung Calendar-inspired mobile experience, then implement recurrence and bulk-import work without breaking the existing event flow.

## Current State
- Flutter on `flutter-v2` is the active product implementation; Android/Kotlin remains legacy reference.
- Runtime on Samsung S23 is stable; month↔week collapse/expand and directional month/week navigation have been verified.
- Main planner uses month-first hierarchy, Material 3 semantic colors, selected-day agenda, countdown column, search, event editing, and contextual add-event action.
- Horizontal month navigation uses a real directional page transition with old/new month surfaces coexisting during the animation.
- Week-view horizontal navigation advances by whole weeks rather than whole months.
- Event creation/edit/delete, search, month/week gestures and countdown policy are implemented.
- The Add Event flow now opens a full-height Samsung Calendar-inspired editor from the selected-day FAB, with today as the initial selected day.
- The editor uses explicit start/end date-time fields, title priority color, location, reminder, recurrence, note, and a floating `Thoát | Lưu` pill.
- New recurring events are expanded into concrete occurrences sharing a `recurrenceId`.
- The top `Nhắc nhở` segment is reserved as the bulk-import surface; bulk-import integration is still pending.
- Platform-specific widget/lock-screen work remains isolated from shared planner semantics.

## Important Boundaries
- Events remain concrete dated occurrences.
- Recurrence series share `recurrenceId`; recurrence metadata is carried on concrete occurrences rather than introducing a separate rule table.
- Home Widget and Focus/Lock Screen stay on `RemoteViews`; no Glance.
- Core schedule semantics stay independent from UI surfaces.
- No per-event/per-second countdown timers.
- Lock-screen availability is host/OEM dependent.
- Samsung-inspired UI means interaction hierarchy and visual language, not a Samsung-only implementation.

## Next Action
1. Verify the new event editor visually and interactively on Samsung S23, especially date/time pickers, reminder, recurrence and `Thoát | Lưu`.
2. Implement series-aware edit/delete with explicit one-event vs entire-series behavior.
3. Replace the `Nhắc nhở` placeholder with the bulk-import workflow and preview/validation.
4. Re-run Home Widget / Focus-Lock verification after recurrence/data-model integration.
5. Perform broader cross-device responsive verification after the planner behavior stabilizes.
