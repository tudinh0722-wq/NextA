# NextA Changelog

## 2026-09-11 — Countdown and priority visual refinement

Decisions:
- Countdown refreshes on minute boundaries instead of relying only on unrelated planner rebuilds.
- Countdown values at or above one day now retain hour precision, for example `2d 5h`, instead of collapsing to whole days.
- Agenda countdowns are enclosed in a compact priority-colored surface so the severity signal is visible beyond the editor title dot.
- Priority colors continue to use Material 3 semantic roles: primary for normal, tertiary for important, and error for highest priority, giving the intended blue/orange/red visual hierarchy on the default palette.
- Event editor now uses the modal route's safe-area handling plus additional top spacing so `Sự kiện | AI Import` cannot sit under the status bar/camera cutout.

## 2026-09-11 — Event editor UX refinement

Decisions:
- Compact the `Sự kiện | AI Import` header to reduce vertical footprint and keep the editor safer around camera cutouts and Dynamic Island-style insets.
- Rename the bulk-import placeholder from `Nhắc nhở` to `AI Import` so its purpose is immediately understandable.
- Add trailing chevrons to reminder/repeat/recurrence rows so editable rows visibly communicate that they are tappable.
- Make the reminder popup scrollable so numeric inputs remain usable when the software keyboard reduces available height.

## 2026-09-11 — Reminder configuration in Flutter event editor

Decisions:
- Replaced preset reminder choices with a centered popup containing direct numeric entry.
- Default reminder is 10 minutes before the event.
- Added a separate reminder-repeat row directly below the reminder row.
- Default repeat behavior is 2 additional reminders, 5 minutes apart, while the reminder remains unacknowledged.
- Added reminder configuration fields to concrete events so recurrence expansion preserves them.
- Actual acknowledgement and alarm scheduling remain a separate runtime/persistence concern.

## 2026-09-11 — Samsung-style Flutter event editor

Decisions:
- Replaced the generic event dialog with a full-height Samsung Calendar-inspired Add/Edit Event surface opened from the planner add-event pill.
- The editor defaults to the planner's selected day, which is today when the planner first opens.
- Removed the All-day control; events use explicit start date/time and end date/time fields.
- Added a compact priority color selector beside the title using Material 3 semantic colors.
- Added location, reminder, recurrence, and note rows with the same restrained list/divider hierarchy as the reference screen.
- Added a floating `Thoát | Lưu` action pill at the bottom.
- Added the `Sự kiện | Nhắc nhở` header; the second segment is reserved for the planned bulk-import workflow.
- Connected new recurring-event creation to the concrete-occurrence recurrence policy.
- Kept reminder selection as UI state for now; persistent alarm wiring remains a separate concern.

## 2026-09-11 — Recurrence foundation

Decisions:
- Added recurrence metadata to the concrete `NextAEvent` model.
- A recurrence series is identified by a shared `recurrenceId`; no separate recurrence-rule table was introduced.
- Added a pure application policy that expands one concrete seed event into concrete dated occurrences.
- Supported recurrence frequencies are none, daily, weekly, weekdays, and monthly with an optional interval and end date.
- Monthly recurrence stays anchored to the original day, clamping only when the target month is shorter (for example Jan 31 → Feb 28 → Mar 31).
- Added unit coverage for daily, weekly, and month-end recurrence expansion.

## 2026-09-11 — Flutter planner runtime verification + directional month transition

Decisions:
- Samsung S23 runtime verification confirms the latest month/week collapse and expand behavior is stable.
- Horizontal month navigation now keeps old and new month surfaces alive during a real directional page transition.
- Swipe direction controls page direction; motion combines slide with restrained scale/fade.
- Week-view horizontal navigation advances by whole weeks rather than whole months.
- Transition uses Flutter animation primitives and does not use delayed rebuilds or timing hacks.

## 2026-09-10 — Flutter planner UI implementation

Decisions:
- Rebuilt the Flutter planner as a Samsung Calendar visual/interaction reference implementation rather than a generic Material calendar.
- Header uses dynamic `TH{month}` and a Today calendar affordance showing the current day number; neither is hard-coded.
- Month grid starts on Monday, keeps Sunday semantically red, fades adjacent-month dates, and renders compact per-day event bars.
- Selected date drives the agenda header and the floating `Thêm vào {day} Th{month}` action label.
- Agenda uses a time column, event color marker, title/details, dividers, priority indicator, and optional NextA countdown instead of Material event cards.
- Added tap selection, long-press add, month navigation, Today navigation, month/week collapse, event tap, and search entry points.
- UI colors are derived from Material 3 `ColorScheme`; Samsung source code/assets/branding are not used.

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
