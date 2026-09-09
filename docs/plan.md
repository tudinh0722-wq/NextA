# NextA Development Plan

> **Status is based on the current `main` source tree, not on the retired project-context document.**
> Source code is the final authority for implementation status.

## Phase 1 — Core Data & App Foundation
- [x] Android app foundation
- [x] Hilt dependency injection
- [x] Room database
- [x] Event entity/model/DAO/repository
- [x] Sample data seeding
- [x] MainViewModel state flow
- [x] Current/next schedule calculation

## Phase 2 — Event Management
- [x] Add event form
- [x] Event validation
- [x] Persist new events
- [x] Long-press delete flow
- [x] Persist deletion
- [ ] Edit event flow

## Phase 3 — App Screen / Weekly Planner
- [x] Seven-day horizontal week strip
- [x] One-day-at-a-time main schedule paging
- [x] Default to today
- [x] Select a weekday within the displayed week
- [x] Keep week strip and main day pager synchronized
- [x] Show only selected-day events
- [x] Centered week/date header
- [x] Material 3 date picker for long-range jumps
- [x] Minimal selected-day indicator without a filled selection box
- [x] Current/upcoming/past event presentation
- [x] Countdown presentation with 24h display switch
- [ ] Final visual polish
- [ ] Verify the full interaction flow on target devices

## Phase 4 — Home Widget
- [x] Dedicated `AppWidgetProvider`
- [x] 4x2 RemoteViews layout/provider path
- [x] Current + next event selection
- [x] Next two events when there is no current event
- [x] Event time/location/note presentation
- [x] Countdown presentation
- [x] Event-boundary refresh scheduling
- [x] Minute refresh while an active countdown is relevant
- [ ] Apply the same 14-day countdown horizon policy as the app
- [ ] Finalize visual hierarchy
- [ ] Verify launcher installation/rendering on target devices

## Phase 5 — Focus / Lock Screen Surface
- [x] Separate Focus provider/presentation implementation exists
- [x] Current/next event data path exists
- [x] Large countdown-oriented presentation foundation exists
- [ ] Verify whether the target OS/OEM exposes a usable Lock Screen surface
- [ ] Integrate only through an official OS/OEM capability where available
- [ ] Define and implement notification fallback when dedicated Lock Screen presentation is unavailable
- [ ] Final visual polish

## Phase 6 — Cross-Device Platform Strategy
- [x] Separate event/countdown semantics from presentation surfaces
- [x] Treat platform/OEM differences as capabilities/adapters rather than core business logic
- [x] Keep Home Widget on RemoteViews for launcher compatibility
- [ ] Define a small runtime capability model where it provides real value
- [ ] Verify Home Widget behavior across representative Android launchers/OEMs
- [ ] Verify Focus/Lock Screen behavior across representative devices
- [ ] Add platform-specific adapters only for documented official capabilities

## Deferred Product Work
- [ ] Event editing
- [ ] Production timetable import
- [ ] OCR/AI import
- [ ] Backend/cloud synchronization
- [ ] Other large platform integrations not yet required

## Working Rule
Keep this file at high-level phase/status level. Keep exact current problems and the immediate next action in `docs/task.md`. Never mark platform or device support complete without testing the actual target surface/device.
