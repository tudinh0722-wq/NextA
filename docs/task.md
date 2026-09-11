# NextA Current Task

## Status
IN PROGRESS

## Current Objective
Refine the main planner into a polished Samsung Calendar-inspired mobile experience, then complete local event persistence/search and continue recurrence + AI-assisted bulk-import work.

## Current State
- Flutter on `flutter-v2` is the active product implementation; Android/Kotlin remains legacy reference.
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
- The editor uses explicit start/end date-time fields, title priority color, `Địa điểm`, `Ghi chú`, reminder, reminder-repeat settings, recurrence, and a floating `Thoát | Lưu` bar.
- Start/end time selection uses a draggable Cupertino wheel picker for a 3D cylinder-style interaction.
- `Ghi chú` is placed immediately below `Địa điểm` so the two text fields remain together.
- Reminder configuration uses a centered popup with label/value/unit rows and an explicit on/off switch.
- Reminder defaults are 10 minutes before, then 2 additional reminders at 5-minute intervals if unacknowledged.
- Reminder and repeat rows expose their tap affordance with trailing chevrons.
- Recurrence selection is a centered dialog with a mandatory finite end condition. For recurring events, `Sau N lần` is the default end mode (20 occurrences) and `Đến ngày` is the alternative.
- Recurrence generation stops at the selected count or end date, with an additional hard safety cap.
- Recurrence persistence stores end mode, count, and until date in SQLite.
- New recurring events are expanded into concrete occurrences sharing a `recurrenceId`, including their reminder configuration.
- Deleting a recurring occurrence supports `Chỉ sự kiện này`, `Sự kiện này và các sự kiện sau`, or `Tất cả sự kiện trong chuỗi`.
- Flutter now persists concrete events in a local SQLite database (`nexta.db`) and seeds the database with demo events only when it is empty.
- Event create/edit/delete writes are connected to SQLite persistence.
- Search queries SQLite across title, location, and note and lets the user jump directly to a matching event's date.
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
1. Verify the latest recurrence UI, bounded generation, SQLite migration, and series deletion on Samsung S23.
2. Replace the `AI Import` placeholder with the AI-assisted bulk-import workflow and preview/validation.
3. Re-run Home Widget / Focus-Lock verification after recurrence/data-model integration.
4. Perform broader cross-device responsive verification after the planner behavior stabilizes.
