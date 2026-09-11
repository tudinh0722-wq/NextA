# NextA Changelog

## 2026-09-11 — SQLite persistence and database-backed search

Decisions:
- Added a Flutter SQLite database (`nexta.db`) as the local source of truth for concrete planner events.
- Database schema stores event content, priority, recurrence metadata, and reminder configuration without introducing a separate recurrence-rule table.
- Demo events are seeded only when the database is empty.
- Create/edit/delete event flows now persist to SQLite.
- Top-bar search now queries SQLite across title, location, and note and lets the user jump to the selected event date.
- Added `sqflite` and `path` dependencies for the Flutter persistence layer.

## 2026-09-11 — Agenda and Today visual refinement

Decisions:
- Today control is now a compact cushion-shaped day number beside search; the calendar icon was removed.
- Removed the redundant `!` priority marker beside agenda event times.
- Countdown remains enclosed in a priority-colored surface and keeps `xd yh` precision for multi-day durations.

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
- Added the `Sự kiện | AI Import` header; the second segment is reserved for the planned bulk-import workflow.
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

## Product Boundaries
1. App Screen — Weekly Planner / calendar navigation.
2. Home Widget — Today Schedule.
3. Focus/Lock Screen — Now & Next with large countdown.
4. Notification — cross-device fallback when appropriate.

Do not merge their visual responsibilities.
