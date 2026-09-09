# NextA Changelog

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

## 2026-09-09 — Home Widget timeline hierarchy

Decisions:
- Removed colored status-card backgrounds from the two event slots.
- Start/end times are the dominant timeline information and remain red.
- Progress line and endpoint dot exist only for the currently active event.
- Endpoint dot is intentionally small so the time labels get more horizontal room.
- Countdown uses two lines (`Bắt đầu sau` / `Kết thúc sau` then the value) and refreshes on minute boundaries and meaningful event transitions.
- Non-red widget text uses system semantic theme colors; the translucent/mica treatment is limited to the overall widget background.

## 2026-09-09 — Cross-Device Surface Strategy and Context Consolidation

The former `NEXTA_PROJECT_CONTEXT.md` is retired as a separate context dump. Important architectural, technology, product, and development constraints live in repository documentation.

- Core event/countdown logic is independent from presentation surfaces, Android versions, launchers, and OEMs.
- App Screen, Home Widget, Focus/Lock Screen, and notification fallback are separate presentation surfaces.
- Platform differences are modeled as capabilities/adapters rather than brand checks.
- Android Lock Screen support must not be assumed from the existence of a Home Screen AppWidget.
- Material/system semantic colors are preferred over hard-coded device-specific colors.

## Product Boundaries
1. App Screen — Weekly Planner.
2. Home Widget — Today Schedule.
3. Focus/Lock Screen — Now & Next with large countdown.
4. Notification — cross-device fallback when appropriate.

Do not merge their visual responsibilities.
