# NextA Current Task

## Status
IN PROGRESS

## Current Objective
Refine the main planner into a polished Samsung Calendar-inspired mobile experience, then implement the planned recurrence and bulk-import work without breaking the existing event flow.

## Current State
- Flutter on `flutter-v2` is the active product implementation; Android/Kotlin remains legacy reference.
- Runtime on Samsung S23 is stable; month↔week collapse/expand has been verified.
- Main planner uses month-first hierarchy, Material 3 semantic colors, selected-day agenda, countdown column, search, event editing, and contextual add-event action.
- Horizontal month navigation now uses a real directional page transition with old/new month surfaces coexisting during the animation.
- Month transition uses slide + subtle scale/fade with frame-synchronized Flutter animation primitives.
- Event creation/edit/delete, search, month/week gestures and countdown policy are implemented.
- Platform-specific widget/lock-screen work remains isolated from shared planner semantics.

## Important Boundaries
- Event remains a concrete dated occurrence until the explicit recurrence architecture is implemented.
- Recurrence should use concrete occurrences with shared `recurrenceId`; do not introduce a separate recurrence-rule table without an explicit decision.
- Home Widget and Focus/Lock Screen stay on `RemoteViews`; no Glance.
- Core schedule semantics stay independent from UI surfaces.
- No per-event/per-second countdown timers.
- Lock-screen availability is host/OEM dependent.
- Samsung-inspired UI means interaction hierarchy and visual language, not a Samsung-only implementation.

## Next Action
1. Verify directional month transition on S23: left/right direction, no layout exceptions, selected-date continuity, and repeated swipes.
2. Tune motion only if device testing shows issues; keep real Flutter animation primitives and avoid timing hacks.
3. Implement the recurrence architecture using concrete occurrences with shared `recurrenceId`.
4. Add recurrence controls and series-aware edit/delete.
5. Integrate bulk import into the planned Add Event flow and add recurrence/import tests.
6. Re-run Home Widget / Focus-Lock verification after data-model changes.
