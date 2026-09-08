# NextA Requirements

## Product
NextA is a student-focused personal schedule management Android app.

## App Screen — Weekly Planner
- Show a 7-day strip for the current week.
- Default selected day is today when the screen opens.
- User can select another day.
- Show only events belonging to the selected day below the day strip.
- Day background/load should reflect the total event weight for that day.
- Events expose a priority value: 0 normal, 1 important, 2 very important.
- Provide a circular `+` floating action button at the bottom-right.
- Long press an event to delete it.
- Show event start/end time, title, location, note, and current status where useful.

## Event Management
- User can create an event with title, type, date, start/end time, location, note, and priority.
- End time must be after start time.
- Persist events locally.

## Home Widget — Today Schedule
- This is a separate UI from the app screen.
- Target size is 4x2.
- Show up to two relevant events, prioritizing the currently active event and then upcoming event(s).
- Primary information: event title, start/end time, and countdown.
- If an event is active, countdown is to its end time.
- If an event is upcoming, countdown is to its start time.
- Location and note are secondary information.
- Countdown should refresh approximately every minute and at event boundaries.
- Prefer launcher-compatible/simple `RemoteViews` layouts over decorative complexity.

## Focus / Lock Screen Surface
- This is intentionally different from both the App Screen and Home Widget.
- Focus on Now/Next and a large countdown.
- Keep the information hierarchy optimized for glanceability.

## Current Scope
The project is still under active development. Requirements above describe the intended behavior; implementation status belongs in `docs/task.md` and `docs/plan.md`.