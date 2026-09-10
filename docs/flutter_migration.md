# NextA Flutter v2 Migration Boundary

## Existing Android project

`main` is frozen as the Android reference implementation. It is not the long-term parallel product.

Reference material to preserve:
- `docs/requirements.md` — product behavior.
- `docs/architecture.md` — existing domain/data/surface decisions.
- `docs/plan.md` — roadmap and unfinished work.
- `docs/changelog.md` — historical decisions and fixes.
- Android source — only when it clarifies behavior or a native platform implementation.

## What is migrated conceptually

- Event and schedule semantics.
- Countdown rules.
- Planner interactions and information hierarchy.
- Event CRUD behavior.
- Search behavior.
- AI bulk-import direction.
- Recurrence direction.
- Widget/notification product behavior.
- Cross-device capability strategy.

## What is rewritten

- Compose UI -> Flutter widgets.
- Android ViewModel/state plumbing -> Dart application state/use cases.
- Android Room implementation -> Flutter-compatible local persistence behind repository contracts.
- Android navigation -> Flutter navigation.
- Android-only application services -> platform adapters.

## What remains native

Native platform code is intentionally retained for OS surfaces that require it. Flutter communicates through small, capability-oriented platform interfaces instead of embedding Android/iOS decisions throughout the application.

## First milestone

The first milestone is a clean Flutter planner that can:
- load local events,
- show the Samsung Calendar-inspired planner hierarchy,
- navigate month/week and selected days,
- show the selected-day agenda,
- create/edit/delete events,
- calculate shared countdown state,
- respect Material/system theme semantics.

Do not start with widgets, recurrence, or AI import before the core Flutter architecture is stable.
