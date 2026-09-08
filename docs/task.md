# Current Task

## Status
IN PROGRESS

## Current Objective
Stabilize and finish the Home Widget 4x2 UI while preserving launcher compatibility.

## Current Problem
The Home Widget has previously failed to install with a launcher message equivalent to `Không thể thêm tiện ích` after RemoteViews layouts became too complex.

## Last Relevant Commit
`9168bed6afc134aebc04e7015e3de7578f27abdb`

The latest Home Widget layout was simplified after an earlier nested-card layout caused 4x2 installation failure.

## Relevant Files
- `app/src/main/res/layout/nexta_widget.xml`
- `app/src/main/res/xml/nexta_widget_info.xml`
- `app/src/main/java/com/nexta/widget/NextAWidgetProvider.kt`
- `app/src/main/res/drawable/nexta_widget_background.xml`

## Current Home Widget Constraints
- Keep target size 4x2.
- Use traditional Android `RemoteViews`.
- Do not introduce Glance.
- Keep current event/upcoming event behavior and countdown logic unless the task explicitly changes it.
- Prefer simple launcher-compatible hierarchy over decorative nested cards.
- If installation fails again, first reduce the layout to a known-good minimal RemoteViews structure, then add hierarchy incrementally.

## Current App Screen
The App Screen has already been changed to a 7-day weekly planner:
- Monday–Sunday strip.
- Defaults to today.
- Selecting a day filters the event list to that day.
- Day background reflects event load/priority weight.
- Circular `+` FAB at bottom-right.
- Long press on an event opens delete confirmation.

## Other UI Surface
The Focus widget is intentionally separate from the Home Widget and App Screen. It uses a large countdown and current/next event hierarchy.

## Next Action
For the next implementation task, inspect the current widget layout/provider and make the smallest safe change required by the user's request. Build/test on the user's Android environment before declaring the widget stable.

## Documentation Rule
After a task/commit:
- Update this file with the current problem/status/next action.
- Update `docs/changelog.md` for meaningful changes or decisions.
- Update `docs/plan.md` only when roadmap status changes.
- Update `docs/architecture.md` only when architecture boundaries change.
- Update `docs/requirements.md` only when product requirements change.