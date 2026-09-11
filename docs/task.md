# NextA Current Task

## Status
IN PROGRESS

## Current Objective
Refine the main planner into a polished Samsung Calendar-inspired mobile experience, then complete local event persistence/search and continue recurrence + AI-assisted bulk-import work.

## Current State
- Flutter on `flutter-v2` is the active product implementation; Android/Kotlin remains legacy reference only.
- Runtime on Samsung S23 is stable; month↔week collapse/expand and directional month/week navigation have been verified by the user.
- Main planner uses month-first hierarchy, Material 3 semantic colors, selected-day agenda, countdown column, search, event editing, month/week gestures, and contextual add-event action.
- Horizontal month navigation uses a real directional page transition with old/new month surfaces coexisting during the animation.
- Week-view horizontal navigation advances by whole weeks rather than whole months.
- Countdown refresh is aligned to minute boundaries and continues refreshing once per minute while the planner is active.
- Countdown formatting keeps hour precision for multi-day durations (`xd yh`) instead of dropping the hour remainder.
- Agenda countdowns are enclosed in a priority-colored surface, reusing the same Material 3 semantic severity hierarchy as the editor: primary / tertiary / error.
- The agenda no longer shows the redundant `!` priority icon beside the time.
- Agenda start/end time is visually stronger than secondary metadata.
- The Today control is an outline-only day number beside search, without a filled background.
- The Add Event flow opens a full-height Samsung Calendar-inspired editor from the selected-day FAB, with today as the initial selected day.
- The editor now follows the supplied reference hierarchy: title with a compact priority dot, start→end date/time columns, address, note, reminder, reminder-repeat, and recurrence.
- Title input is limited to 47 characters; address and note inputs are limited to 30 characters.
- Priority is selected inline from the small title dot only. The selector expands horizontally to three pastel choices: blue, orange, red. No priority names are shown in the editor.
- All-day has no control in the event editor; events use explicit start/end date-time fields.
- Start/end time selection uses a draggable Cupertino wheel picker for a 3D cylinder-style interaction.
- Start/end date-time columns no longer show calendar/clock icons; the date and time text themselves are tappable.
- `Ghi chú` is immediately below `Địa chỉ`, with no horizontal divider crossing the note field.
- Reminder is split visually into `Báo trước`, `Báo lại`, and `Lặp lại` rows.
- Bottom `Thoát | Lưu` is a single floating pill with subtle surface contrast and shadow rather than two visually separated buttons.
- The top editor switch is a rounded pill with `Thêm sự kiện` and `AI Import` content.
- Reminder configuration uses a centered popup with label/value/unit rows and an explicit on/off switch.
- Reminder defaults are 10 minutes before, then 2 additional reminders at 5-minute intervals if unacknowledged.
- Reminder and repeat rows expose their tap affordance with trailing chevrons.
- Recurrence selection is a centered radio dialog with `Không lặp lại`, `Hàng ngày`, `Hàng tuần`, and `Hàng tháng`.
- Selecting a recurring frequency expands a compact indented end section directly beneath it with two radio branches: `Kết thúc sau X lần lặp` and `Đến ngày X`.
- The default recurrence count is 10. The end-date preview is derived from the date of the 10th occurrence, so the two end choices stay consistent with the selected frequency.
- Both recurrence end branches are finite: count mode stores `count`, while date mode stores `until`. The recurrence engine remains bounded without introducing a new recurrence table.
- Recurrence generation stops at the selected end boundary, with an additional hard safety cap.
- Recurrence persistence stores end mode, count, and until date in SQLite.
- New recurring events are expanded into concrete occurrences sharing a `recurrenceId`, including their reminder configuration.
- Deleting a recurring occurrence supports `Chỉ sự kiện này`, `Sự kiện này và các sự kiện sau`, or `Tất cả sự kiện trong chuỗi`.
- Flutter now persists concrete events in a local SQLite database (`nexta.db`) and seeds the database with demo events only when it is empty.
- Event create/edit/delete writes are connected to SQLite persistence.
- Search queries SQLite across title, location, and note and lets the user jump directly to a matching event's date.
- A quick tap on a calendar day selects/navigates to that day; a long press opens the Add Event shortcut preselected to that exact day.
- The empty agenda state `Không có sự kiện` is informational only and no longer opens the Add Event editor.
- `AI Import` is reserved for the AI-assisted bulk-import workflow; integration is still pending.
- Platform-specific widget/lock-screen work remains isolated from shared planner semantics.

## Important Boundaries
- Events remain concrete dated occurrences.
- Recurrence series share `recurrenceId`; recurrence metadata is carried on concrete occurrences rather than introducing a separate rule table.
- Every recurring series is finite: count or until is required; the recurrence engine also enforces a hard safety cap.
- Reminder repeat count means additional reminders after the initial reminder; actual acknowledgement/alarm delivery remains a separate runtime concern.
- A reminder-disabled event is represented by `reminderMinutes = 0`; repeat settings are inactive while disabled.
- Home Widget and Focus/Lock Screen stay on `RemoteViews`; no Glance.
- Core schedule semantics stay independent from UI surfaces.
- No per-event/per-second countdown timers.
- Lock-screen availability is host/OEM dependent.
- Samsung-inspired UI means interaction hierarchy and visual language, not a Samsung-only implementation.

## Next Action
1. Verify the latest Add Event visual layout and recurrence interaction on Samsung S23.
2. Verify calendar tap vs long-press behavior and the non-interactive empty agenda state on Samsung S23.
3. Verify bounded recurrence generation and series deletion on Samsung S23.
4. Replace the `AI Import` placeholder with the AI-assisted bulk-import workflow and preview/validation.
5. Re-run Home Widget / Focus-Lock verification after recurrence/data-model integration.
6. Perform broader cross-device responsive verification after the planner behavior stabilizes.
