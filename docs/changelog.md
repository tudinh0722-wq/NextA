# NextA Changelog

This file records recent implementation decisions that may not be obvious from source code. Keep it concise; archive older entries when it becomes large.

## 2026-09-09 — Cross-Device Surface Strategy and Context Consolidation

The former `NEXTA_PROJECT_CONTEXT.md` is being retired as a separate context dump. Its important architectural, technology, product, and development constraints are now maintained in the normal repository documentation so future AI agents do not need a second source of truth.

Decisions:
- NextA core event/countdown logic must be independent from presentation surfaces, Android versions, launchers, and OEMs.
- App Screen, Home Widget, Focus/Lock Screen, and notification fallback are separate presentation surfaces.
- Platform differences are modeled as capabilities/adapters rather than brand checks in core logic.
- Android Lock Screen support must not be assumed from the existence of a standard Home Screen `AppWidget`.
- A user permission can enable an exposed capability but cannot create an OS/OEM capability that does not exist.
- Notifications are the cross-device fallback for Lock Screen visibility when a dedicated Lock Screen surface is unavailable and the product requires it.
- Material/system semantic colors should be preferred over hard-coded device-specific colors.
- The existing concrete `Event` model, Room repository boundary, Hilt setup, Java 17 target, Android SDK/toolchain versions, and RemoteViews constraint remain part of the architecture unless explicitly changed.

## 2026-09-08 — Countdown Horizon and Home Widget Refinement

Updated `MainScreen`, `NextAWidgetProvider`, and `nexta_widget.xml`.

Decisions:
- Countdown is shown only for events within the next 14 days.
- From 24 hours onward, countdown uses days rather than hours/minutes.
- Main schedule continues to refresh one shared `now` value every 30 seconds instead of creating one timer per event.
- Home Widget still presents exactly two events: current + next, or next two when there is no current event.
- Widget hierarchy remains status -> title -> time -> countdown -> location -> note.
- Widget avoids minute-level alarm refreshes when the next relevant event is more than 14 days away.

## 2026-09-08 — Centered Week Strip and Dual-Level Swipe Navigation

Decisions:
- The schedule remains a day pager: swiping the main schedule moves exactly one day.
- The seven-day strip is its own horizontal pager: swiping the strip moves exactly one week.
- Week-strip navigation preserves the currently selected weekday.
- Day and week pagers synchronize automatically.
- The week strip is centered and intentionally minimal: no border and no large filled selected-day container.
- The week/date header is centered and remains the entry point to the Material 3 date picker.
- The five-year-before/after range and direct date picker remain available for long-range planning.

## 2026-09-08 — App Screen Daily Planner Refinement

Decisions:
- The app opens on today.
- The seven-day indicator is lightweight rather than a large filled selected-day control.
- Event cards use explicit hierarchy: title strongest, start/end time prominent, countdown accent/bold, location secondary, note lowest emphasis.
- Existing long-press deletion and circular `+` FAB are preserved.

## 2026-09-08 — AI Context Bootstrap

Added repository-level AI context so different coding agents can work from the same source of truth instead of relying on chat history.

The repository documentation is now the source of truth: `AGENTS.md` plus the focused files under `docs/`.

## Product Boundaries
NextA intentionally has separate surfaces:
1. App Screen — Weekly Planner.
2. Home Widget — Today Schedule.
3. Focus/Lock Screen — Now & Next with large countdown.
4. Notification — cross-device fallback when appropriate.

Do not merge their visual responsibilities.

## Home Widget 4x2 Compatibility
The Home Widget uses traditional `RemoteViews`. A more decorative nested-card layout previously caused the launcher to report that the 4x2 widget could not be added. When modifying the Home Widget, prioritize launcher inflation compatibility and incrementally add visual complexity only after installation remains reliable.
