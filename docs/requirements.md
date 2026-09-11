# NextA Requirements

## Product
NextA is a student-focused personal schedule management Android app.

## App Screen — Weekly Planner
- Show a 7-day strip for the current week.
- Default selected day is today when the screen opens.
- User can select another day.
- Show only events belonging to the selected day below the day strip.
- Day background/load should reflect total event weight for that day.
- Event priority: `0` normal, `1` important, `2` very important.
- Provide a circular `+` floating action button at the bottom-right.
- Long press an event to delete it.
- Show event start/end time, title, location, note, and current status where useful.

## Event Management
- The primary Add Event action opens a full-height Samsung Calendar-inspired event editor for the currently selected day; the default selected day is today when the planner opens.
- The editor has a two-segment header: `Sự kiện` and `Nhắc nhở`. The `Nhắc nhở` surface is reserved for NextA's bulk-import workflow.
- User can create an event with title, type, date, start/end time, location, note, and priority.
- The editor does not expose an `All day` option; every event uses an explicit start date/time and end date/time.
- A compact color dot beside the title represents event priority and opens the priority selector.
- Reminder configuration is edited in a centered popup with numeric fields rather than fixed preset choices.
- Reminder default is 10 minutes before the event.
- If the reminder is not acknowledged, it repeats 2 additional times by default, with 5 minutes between repeats.
- The user can enter the reminder offset, repeat count, and repeat interval directly. Repeat count means additional reminders after the first reminder.
- The reminder repeat row is shown directly below the main reminder row and summarizes the current repeat configuration.
- Reminder persistence/runtime alarm delivery remains a separate alarm concern; the event model carries the configuration so it can be persisted and scheduled later.
- The editor provides recurrence choices: none, daily, weekly, weekdays, and monthly.
- New recurring events are expanded into concrete dated occurrences sharing a `recurrenceId`.
- End time must be after start time.
- Persist events locally through the existing Room repository layer.
- Keep the existing concrete Event model based on `startDateTime` and `endDateTime` unless an explicit architecture change is approved.

## Countdown
- Countdown is derived from event timestamps; it must not depend on a particular UI surface.
- For an active event, countdown is to its end time.
- For an upcoming event, countdown is to its start time.
- Show countdown only within the next 14 days.
- Below 24 hours, show hours/minutes.
- From 24 hours through 14 days, show days rather than hours/minutes.
- Do not create one timer per event. Prefer shared time evaluation and boundary-based refreshes.

## Home Widget — Today Schedule
- Separate UI from the app screen.
- Target size is 4x2.
- Use traditional Android `RemoteViews`.
- Show up to two relevant events: current + next, or next two when there is no current event.
- Primary information: event title, start/end time, and countdown.
- Location and note are secondary information.
- Refresh approximately at useful minute/event boundaries rather than every second.
- Prefer launcher-compatible/simple layouts over decorative complexity.

## Focus / Lock Screen Surface
- Separate UI from both the App Screen and Home Widget.
- Focus on Now/Next and a large countdown.
- Optimize for glanceability.
- Treat Lock Screen support as capability-dependent. Do not assume that an Android Home Screen `AppWidget` can appear on every device's Lock Screen.
- Do not require a hypothetical universal Lock Screen widget permission.
- If a dedicated Lock Screen API is unavailable, use a standard Android notification/Lock Screen notification as the fallback where appropriate.

## Cross-Device Compatibility
- NextA must target multiple Android devices and OEMs rather than a single device/UI version.
- Same product semantics should be preserved while presentation adapts to platform capabilities.
- Prefer capability detection over device-brand checks.
- OEM-specific integrations are optional adapters, not core product logic.
- Platform limitations must not force Samsung/Pixel/Xiaomi/etc. conditions into the event/countdown engine.

## Design System
- Prefer Material 3 / dynamic system colors where supported.
- Avoid hard-coded device-specific colors when a semantic Material/system token can be used.
- Restricted surfaces such as `RemoteViews` may implement the same visual hierarchy differently because of platform limitations.

## Current Scope
Requirements describe intended behavior. Implementation status belongs in `docs/task.md` and `docs/plan.md`. Verify actual source and target-device behavior before marking a capability complete.
