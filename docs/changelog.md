# NextA Changelog

## 2026-09-10 — Samsung Calendar-inspired planner UI

Decisions:
- Refactored the main planner screen around the interaction hierarchy of Samsung Calendar while keeping NextA's own product semantics and implementation boundaries.
- Added Month / Week / Day presentation modes in the app planner.
- Month view keeps the selected day and event dots; day load is represented using `Event.priority` as a subtle semantic-color background weight.
- Week view provides a compact seven-day strip and visual event-load indicators.
- Day view provides a focused date header and shares the selected-day agenda below.
- Horizontal swipes navigate month/week/day depending on the active mode.
- The primary add-event action remains connected to the existing `MainActivity` event flow.
- Event rows retain start/end time, title, location, and note where available, with event-type colors mapped to Material 3 semantic colors.
- Removed device-specific hard-coded calendar colors from the planner surface.
- Home Widget and Focus/Lock Screen remain separate presentation surfaces.

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
