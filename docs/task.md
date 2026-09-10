# NextA Current Task

## Status
IN PROGRESS

## Current Objective
Refine the main planner into a Samsung Calendar-inspired mobile experience, then implement the planned recurrence and bulk-import work without breaking the existing Room-backed event/alarm flow.

## Current State
- Phase A: manual event creation, validation, concrete Event persistence, daily/weekly planner, shared countdown semantics.
- Phase B: long-press edit/delete, Home Widget, Focus/Lock Screen surface, and notification fallback.
- Phase C: external-AI bulk import with `NGÀY:/TÊN:/BẮT ĐẦU:/KẾT THÚC:/ĐỊA ĐIỂM:/GHI CHÚ:` contract, validation, preview, and bulk Room insert.
- Countdown refreshes on minute boundaries rather than polling every 30 seconds.
- Event and alarm persistence is standardized on Room. `events` stores event content; `event_alarms` stores reminder settings/state with a foreign-key cascade.
- Event + alarm writes are transactional. `AlarmManager` is runtime scheduling only; it is rebuilt from Room after boot/time/timezone/exact-alarm-permission changes.
- Existing legacy `nexta_alarms` SharedPreferences data has a one-time migration path into Room.
- Main planner UI now follows a Samsung Calendar-inspired month-first hierarchy: hamburger menu, `THx` month title, search, Today affordance, month grid, selected-day agenda, and bottom add-event pill.
- Month view supports tap selection, long-press add-event, horizontal month navigation, and vertical collapse to a compact week strip; downward swipe expands it again.
- Search is functional against event title, location, and note.
- Event creation still flows through `MainActivity` and `AddEventScreen`; the bottom add action calls the existing `onAddEvent(selectedDate)` callback.
- Home Widget and Focus/Lock Screen remain separate presentation surfaces.

## Important Boundaries
- Event remains a concrete dated occurrence until the explicit recurrence architecture from the Implementation Plan is implemented.
- The Implementation Plan uses Concrete Occurrences with shared `recurrenceId`; do not substitute a separate recurrence-rule table without an explicit decision.
- Home Widget and Focus/Lock Screen stay on `RemoteViews`; no Glance.
- Core schedule semantics stay independent from UI surfaces.
- No per-event/per-second countdown timers.
- Lock-screen availability is host/OEM dependent; do not claim universal support until tested on real devices.
- Samsung-inspired UI means interaction hierarchy and visual language, not a Samsung-only implementation.

## Next Action
1. Pull and build the latest UI changes; fix any compile/resource issues.
2. Verify month selection, search, horizontal month swipe, and vertical month↔week collapse on a real device.
3. Verify long-press date opens event creation with that date.
4. Implement the Implementation Plan's DB v4 recurrence model using concrete occurrences and shared `recurrenceId`.
5. Add recurrence controls to `AddEventScreen` and series-aware edit/delete behavior to `MainScreen`.
6. Integrate Bulk Import into the planned two-tab Add Event experience.
7. Add unit tests for recurrence generation and the v3→v4 migration.
8. Re-run Home Widget / Focus-Lock verification after data-model changes.
