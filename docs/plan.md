# NextA Development Plan

> **`flutter-v2` is the active product branch. `flutter/` is the implementation. `app/` is legacy Android/Kotlin reference only.**
> See `docs/CLAUDE_FLUTTER_V2.md` for the full AI implementation contract.

## Flutter planner
- [x] Core event model and countdown policy
- [x] Month-first planner UI
- [x] Month/week collapse and expand
- [x] Event selection/edit/delete/search
- [x] Material 3 semantic theming
- [x] Samsung S23 runtime verification of collapse/expand
- [x] Directional horizontal month/week navigation
- [x] SQLite event persistence
- [x] Database-backed search across title/location/note
- [x] Samsung-inspired Add Event reference layout
- [x] Add Event priority, input limits, reminder and recurrence interaction refinement
- [x] Shared priority color persistence across editor, agenda, and calendar
- [x] Calendar quick-tap day selection and long-press Add Event shortcut
- [x] Non-interactive empty agenda state
- [x] Compact and balanced Add Event FABs
- [x] Calendar marker layout: vertical equal-width stack, maximum two bars
- [x] Agenda event-time/countdown block dimensions and typography
- [ ] Normalize Editor leading icon slots and shared UI spacing
- [ ] Remove obsolete/duplicated presentation code
- [ ] Motion polish and cross-device responsive verification

> **UI lock:** Calendar and Agenda are complete screens. Do not modify their visual presentation unless the user explicitly requests it. General cleanup must preserve the locked baseline.

## Event/data roadmap
- [x] Recurrence domain metadata and concrete-occurrence expansion policy
- [x] Recurrence controls in event editor
- [x] Finite recurrence defaults and compact end-branch layout
- [x] Reminder numeric configuration and repeat metadata in event editor
- [ ] Series-aware edit/delete verification and edge cases
- [ ] Integrate bulk import into Add Event flow
- [x] Recurrence policy unit tests
- [ ] Bulk-import integration tests

## Platform surfaces
- [ ] Final Android widget/launcher verification
- [ ] Focus/Lock Screen real-device capability verification
- [ ] Broader iOS/widget platform adapters

## Release-readiness
- [ ] Cross-device responsive pass
- [ ] Regression pass for persistence/search/recurrence
- [ ] Regression pass for Home Widget / Focus-Lock
- [ ] Final dead-code/import/UI cleanup
- [ ] Release candidate verification

## Working rule
Keep shared event/schedule semantics independent from UI surfaces. Implement product work in `flutter-v2` only. Do not modify the legacy Android implementation for Flutter work. Do not claim device support or test results without actual verification.
