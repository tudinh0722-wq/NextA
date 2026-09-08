# NextA AI Instructions

## Before modifying code

Read the smallest relevant set of project context files:
1. `AGENTS.md`
2. `docs/task.md`
3. `docs/architecture.md` when architecture or shared data flow is relevant
4. `docs/requirements.md` when product behavior is relevant
5. `docs/changelog.md` when the task touches an area with prior fixes/decisions

Do not read every document for every task if it is not relevant.

## Project rules

- Active package/application namespace: `com.nexta`.
- `com.example.nexta` is legacy; do not extend it.
- Preserve the existing layered architecture unless the task explicitly changes architecture.
- The app, Home Widget, and Focus/Lock Screen surface are intentionally different UIs.
- Home Widget currently uses Android `RemoteViews`; do not introduce Jetpack Glance unless explicitly requested.
- Prefer the smallest safe change and do not rewrite unrelated code.
- `Event.priority` already exists and is used by the weekly planner for day weighting.
- Room is the local persistence layer; avoid schema changes unless required.
- Do not claim a build/test passes unless it was actually run and verified.

## After completing a task

- Update `docs/task.md` with the new current state and next action.
- Update `docs/changelog.md` when a meaningful change, bug fix, or implementation decision was made.
- Update `docs/plan.md` when a roadmap item changes status.
- Update `docs/architecture.md` only when architecture or technology boundaries change.
- Update `docs/requirements.md` only when product requirements change.

Keep documentation concise. The repository is the source of truth; chat history is not required context.