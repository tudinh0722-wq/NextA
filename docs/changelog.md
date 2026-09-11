# NextA Changelog

## 2026-09-11 — Add Event reference UI refinement

Decisions:
- Rebuilt the Add Event content hierarchy to follow the supplied Samsung Calendar reference more closely: title, compact start→end date/time columns, address, note, reminder, reminder-repeat, and recurrence.
- Title input is limited to 47 characters.
- Priority is selected only from the small color dot beside the title; the editor does not show a separate priority row or label.
- Priority uses a fixed pastel blue / orange / red palette for normal / important / very important.
- Removed the All-day control from the event editor.
- Location and note are presented as a compact text-input pair without horizontal divider lines crossing through the note field.
- Bottom `Thoát | Lưu` is a single floating pill with a subtle surface contrast and shadow rather than two visually separated buttons.
- The top editor switch remains a rounded pill with `Thêm sự kiện` and `AI Import` content.

## 2026-09-11 — Reminder and event-input refinement

Decisions:
- `Ghi chú` is placed immediately below `Địa điểm` in the event editor.
- Location is labeled `Địa điểm`.
- Start/end time selection uses a draggable Cupertino wheel picker for the requested 3D cylinder-style interaction.
- Reminder fields are compact single-line label/value/unit controls instead of full-width fields stacked vertically.
- Removed the reminder popup's explanatory default sentence.
- Added an explicit `Báo trước` switch so events can have no reminder; disabled reminders persist as `reminderMinutes = 0` and display `Không báo trước`.
- Recurrence remains a centered dialog rather than a bottom sheet.

## 2026-09-11 — Dialog and editor keyboard-safety refinement

Decisions:
- Search now uses a bounded custom dialog layout with an expanded result list so the software keyboard cannot cause `Bottom overflowed` errors.
- Recurrence selection is now a centered dialog instead of a bottom sheet, matching the reminder editor interaction.
- The event editor keeps the note field reachable by adding the current keyboard inset to the scrollable content's bottom padding.
- Agenda start/end time is promoted to a stronger 14sp/800 visual treatment and uses `onSurface` for clearer hierarchy.
- Removed the redundant priority `!` icon from agenda event rows.
- Today control is an outline-only cushion/stadium shape around the day number, with no filled background or calendar icon.

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
