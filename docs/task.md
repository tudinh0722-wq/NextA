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
- Recurrence domain metadata and a pure concrete-occurrence expansion policy are now implemented.
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
1. Add recurrence controls to the event editor using the new domain/application recurrence model.
2. Implement series-aware edit/delete with explicit one-event vs entire-series behavior.
3. Integrate bulk import into the planned Add Event flow and add recurrence/import tests.
4. Re-run Home Widget / Focus-Lock verification after recurrence/data-model integration.
5. Perform broader cross-device responsive verification after the planner behavior stabilizes.
