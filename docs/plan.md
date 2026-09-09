# NextA Development Plan

## Phase 1 — Core Data & App Foundation
- [x] Room database
- [x] Event entity/model/DAO/repository
- [x] Hilt dependency injection
- [x] Main ViewModel state flow
- [x] Sample data seeding

## Phase 2 — Event Management
- [x] Add event form
- [x] Event validation
- [x] Long-press delete
- [x] Persist changes

## Phase 3 — App Screen
- [x] 7-day weekly strip
- [x] Default to today
- [x] Select a day
- [x] Show only selected-day events
- [x] Day background weighted by event priority/load
- [x] Circular `+` FAB

## Phase 4 — Home Widget
- [x] Separate Home Widget provider
- [x] 4x2 widget configuration
- [x] Current + upcoming event selection logic
- [x] Countdown logic
- [x] Minute/event-boundary refresh scheduling
- [ ] Finalize visual hierarchy
- [ ] Verify launcher installation/rendering on target device

## Phase 5 — Focus / Lock Screen
- [x] Separate Focus surface/provider foundation
- [x] Large countdown hierarchy
- [x] Current/next event logic
- [ ] Verify actual lock-screen/keyguard behavior on target devices
- [ ] Final visual polish
- [ ] Define notification fallback when dedicated Lock Screen surface is unavailable

## Phase 6 — Cross-Device Platform Strategy
- [x] Separate core countdown/event semantics from presentation surfaces
- [x] Document capability-oriented architecture
- [x] Document OEM/platform-specific adapter boundary
- [ ] Define a small runtime capability model where needed
- [ ] Verify Home Widget behavior across representative Android launchers/OEMs
- [ ] Verify Focus/Lock Screen behavior across representative devices
- [ ] Add platform-specific integrations only where an official capability exists

## Working Rule
Keep implementation status here high-level. Put the exact current problem and next action in `docs/task.md`. Do not mark platform support complete without testing the actual target device/surface.
