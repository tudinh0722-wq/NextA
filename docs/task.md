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
- Main planner UI now follows a Samsung Calendar-inspired month-first hierarchy: centered `THx` month title, hamburger menu, search, dynamic Today affordance, month grid, selected-day agenda, and bottom add-event pill.
- Today button follows the actual local calendar date and refreshes at the next local midnight while the screen remains open.
- Bottom add-event action uses the currently selected date and a semantic add icon instead of a hard-coded `10 Th9` and text `+`.
- Month title uses `CenterAlignedTopAppBar` so it stays visually centered between navigation and actions.
- Month view supports tap selection, long-press add-event, horizontal month navigation, and vertical collapse to a compact week strip; downward swipe expands it again.
- Search is functional against event title, location, and note.
- Debug builds seed the supplied September 2026 class schedule when Room is empty, with 2 randomly selected important events (`priority = 1`) and 3 randomly selected very important events (`priority = 2`). The incomplete 07/09 event without start/end time is intentionally excluded rather than inventing a time. Demo alarms are disabled.
- Planner UI was refactored into a thin `MainScreen` orchestration layer plus `CalendarComponents` and `SearchDialog`, separating calendar rendering/gesture/state concerns from screen wiring.
- Calendar date events are grouped by date before rendering to avoid repeatedly filtering the full event list for every calendar cell.
- Event accents now use Material 3 semantic colors rather than fixed pastel RGB values.
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
1. Pull and build the latest UI/demo/refactor changes; fix any compile/resource issues.
2. Verify month selection, search, horizontal month swipe, and vertical month↔week collapse on a real device.
3. Verify Today and bottom add-event dates update correctly across date changes.
4. Verify long-press date opens event creation with that date.
5. Implement the Implementation Plan's DB v4 recurrence model using concrete occurrences and shared `recurrenceId`.
6. Add recurrence controls to `AddEventScreen` and series-aware edit/delete behavior to `MainScreen`.
7. Integrate Bulk Import into the planned two-tab Add Event experience.
8. Add unit tests for recurrence generation and the v3→v4 migration.
9. Re-run Home Widget / Focus-Lock verification after data-model changes.
