# NextA Changelog

This file records recent implementation decisions that may not be obvious from source code. Keep it concise; archive older entries when it becomes large.

## 2026-09-08 — App Screen Daily Planner Refinement

Updated `MainScreen` in commit `df00c4bd826388a3ea6c3a98155a2e925c4119b3`.

Decisions:
- Day navigation is now horizontal swipe/paging across the seven days.
- The app still opens on today.
- The seven-day indicator is informational/lightweight; it no longer uses large filled selected-day boxes.
- Removed `1 lịch / 2 lịch` labels from the day strip and removed the selected-day count from the header.
- Event cards now have explicit text hierarchy: title strongest, start time prominent, countdown accent/bold, end time smaller, location secondary, note lowest emphasis.
- Countdown is calculated from the current time to the event start or end depending on event state and refreshes every 30 seconds.
- Existing long-press deletion and circular `+` FAB are preserved.
- This task does not change the app theme or the separation between the three UI surfaces.

## 2026-09-08 — AI Context Bootstrap

Added repository-level AI context so different coding agents can work from the same source of truth instead of relying on chat history.

Files added:
- `AGENTS.md`
- `docs/requirements.md`
- `docs/architecture.md`
- `docs/plan.md`
- `docs/task.md`
- `docs/changelog.md`

Bootstrap commits:
- `47fbf498225d4335ff46d55ca5591eec03e1dad9` — AI instructions
- `9af42857d6935f613de7204d09e84b5a18b82ede` — requirements
- `971b15bdb67ab922e4062c86ce2b455bd5892da6` — architecture
- `356241a0c8ff098b1e4821a5aba7684d4481bf8b` — development plan
- `c15e734fe9863399226ca8149dbe16345e7a9f81` — current task context

## Recent Product/Implementation Decisions

### Weekly Planner
`MainScreen` was originally a weekly planner with seven days and day selection. The current interaction has been refined to horizontal paging/swiping between days. The app opens on today. The seven-day indicator remains lightweight rather than acting as a large selected-day control.

### Three Separate UI Surfaces
NextA intentionally has three different surfaces:
1. App Screen — Weekly Planner.
2. Home Widget — Today Schedule.
3. Focus/Lock Screen — Now & Next with large countdown.

Do not merge their visual responsibilities.

### Home Widget 4x2 Compatibility
The Home Widget uses traditional `RemoteViews`. A more decorative nested-card layout previously caused the launcher to report that the 4x2 widget could not be added. Commit `9168bed6afc134aebc04e7015e3de7578f27abdb` simplified the layout again.

When modifying the Home Widget, prioritize launcher inflation compatibility and incrementally add visual complexity only after installation remains reliable.

### Existing Stack
- Kotlin + Jetpack Compose/Material 3 components for the app UI.
- Room for local events.
- Hilt for dependency injection.
- Traditional `RemoteViews` for widgets.
- Java 17.
- Active package: `com.nexta`.

## Documentation Maintenance
`task.md` is short-lived operational context. `requirements.md` describes product behavior. `architecture.md` describes stable technical boundaries. `plan.md` tracks roadmap status. `changelog.md` records important decisions and reasons.