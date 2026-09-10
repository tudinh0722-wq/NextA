# NextA Changelog

## 2026-09-10 — Planner UI refactor

Decisions:
- Split the Samsung Calendar-inspired planner into a thin `MainScreen` orchestration layer and dedicated calendar/search UI components.
- Centralized calendar interaction state in `CalendarState` so month/week navigation, selected date, Today, and search visibility are not mixed with rendering code.
- Isolated `CalendarSurface`, `Agenda`, `AddEventBar`, and `SearchDialog` as reusable presentation components.
- Grouped events by date before rendering calendar cells to avoid repeatedly filtering the full event list for every cell.
- Kept the existing `MainActivity` → `MainScreen` callback contract and Room-backed data flow unchanged.
- Centralized the local-midnight Today refresh in the calendar UI layer.
- Replaced fixed event RGB accents with Material 3 semantic colors.

## 2026-09-10 — Samsung Calendar-inspired planner UI

Decisions:
- Refactored the main planner screen around the interaction hierarchy of Samsung Calendar while keeping NextA's own product semantics and implementation boundaries.
- Replaced the earlier mode-tab presentation with a Samsung Calendar-style month-first surface: hamburger menu, compact month title, search, Today affordance, selected-day agenda, and bottom add-event pill.
- Added horizontal swipe navigation between months and vertical swipe collapse/expand between month and week views.
- Added tap selection and long-press add-event gestures on calendar dates.
- Added functional event search from the top bar.
- Month view uses rounded-rectangle selected-day emphasis, Sunday red typography, and compact event bars.
- Event cards retain start/end time, title, location, and event-type accent colors.
- The primary add-event action remains connected to the existing `MainActivity` event flow.
- Home Widget and Focus/Lock Screen remain separate presentation surfaces.
- Added valid vector toolbar icons after removing invalid/empty drawable XML resources.

## 2026-09-09 — Event + Alarm persistence standardized on Room

Decisions:
- Room is now the single source of truth for both event data and reminder configuration/state.
- `events` and `event_alarms` are linked one-to-one by `eventId` with foreign-key cascade delete/update.
- Event content and alarm settings are saved atomically through a Room transaction.
- Alarm persistence no longer duplicates event title, note, or start time.
- `AlarmManager` is treated only as the runtime scheduling adapter; reboot/timezone/exact-alarm-permission recovery rebuilds schedules from Room.
- ACK state is persisted in `event_alarms.acknowledged`, then scheduled repeats are cancelled.
- Room database version 2 → 3 uses an explicit migration; destructive migration fallback was removed.
- Existing `nexta_alarms` SharedPreferences data is migrated once into Room before the legacy store is cleared.
- Removed the old test-alarm cleanup path together with the former SharedPreferences alarm store.

## 2026-09-09 — Standardized Phase A/B/C and Phase C bulk import

Decisions:
- Phase A is the concrete event planner: create, validate, persist, and plan events.
- Phase B is event management and glance surfaces: long-press edit/delete plus Home Widget refinement.
- Phase C is external-AI-assisted bulk import. NextA does not perform OCR/AI; it owns the text contract, parser, validation, preview, and persistence.
- `NGÀY:` is the stable record boundary/key in the V1 import format.
- Bulk import never writes before validation/preview; malformed rows are rejected instead of guessed.
- Existing title/location/note limits remain 47/30/30 characters.
- `Event` remains a concrete dated occurrence; recurrence is deferred until an explicit data-model redesign.

## Product Boundaries
1. App Screen — Weekly Planner / calendar navigation.
2. Home Widget — Today Schedule.
3. Focus/Lock Screen — Now & Next with large countdown.
4. Notification — cross-device fallback when appropriate.

Do not merge their visual responsibilities.
