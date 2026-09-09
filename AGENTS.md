# NextA AI Instructions

## Before modifying code

Read the smallest relevant set of project context files:
1. `AGENTS.md`
2. `docs/task.md`
3. `docs/architecture.md` when architecture or shared data flow is relevant
4. `docs/requirements.md` when product behavior is relevant
5. `docs/changelog.md` when the task touches an area with prior fixes/decisions
6. `docs/plan.md` when roadmap status is relevant

Do not rely on chat history or a separate project-context dump. The repository documentation is the maintained source of project decisions; source code remains the final authority for implementation details.

## Project rules

- Active package/application namespace: `com.nexta`.
- `com.example.nexta` is legacy; do not extend it.
- Preserve the existing layered architecture unless the task explicitly changes architecture.
- Keep core Event/countdown logic independent from UI surfaces, Android versions, launchers, and OEMs.
- The App Screen, Home Widget, Focus/Lock Screen, and notification fallback are intentionally different presentation surfaces.
- Home Widget uses Android `RemoteViews`; do not introduce Jetpack Glance unless explicitly requested.
- Prefer capability-oriented platform decisions over device-brand checks.
- OEM-specific integrations must be isolated adapters and must never become core business logic.
- A Lock Screen capability cannot be assumed merely because a Home Screen widget exists; verify actual OS/OEM behavior.
- Do not assume a user permission can enable a platform capability that the OS/OEM does not expose.
- `Event.priority` already exists and is used by the planner for day weighting.
- Room is the local persistence layer; avoid schema changes unless required.
- Do not create per-event/per-second countdown timers; use shared time evaluation and sensible refresh boundaries.
- Prefer semantic Material/system colors over hard-coded device-specific colors where the platform allows it.
- Prefer the smallest safe change and do not rewrite unrelated code.
- Do not claim a build/test or device behavior passes unless it was actually run and verified.

## After completing a task

- Update `docs/task.md` with the new current state and next action.
- Update `docs/changelog.md` when a meaningful change, bug fix, or implementation decision was made.
- Update `docs/plan.md` when a roadmap item changes status.
- Update `docs/architecture.md` when architecture or technology boundaries change.
- Update `docs/requirements.md` when product requirements change.

Keep documentation concise. The repository is the source of truth; chat history is not required context.
