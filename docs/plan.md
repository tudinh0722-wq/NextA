# NextA Development Plan

> `flutter-v2` is the active product branch. Android/Kotlin is legacy reference only.

## Flutter planner
- [x] Core event model and countdown policy
- [x] Month-first planner UI
- [x] Month/week collapse and expand
- [x] Event selection/edit/delete/search
- [x] Material 3 semantic theming
- [x] Samsung S23 runtime verification of collapse/expand
- [x] Directional horizontal month/week navigation
- [ ] Motion polish and cross-device responsive verification

## Event/data roadmap
- [x] Recurrence domain metadata and concrete-occurrence expansion policy
- [ ] Recurrence controls in event editor
- [ ] Series-aware edit/delete
- [ ] Integrate bulk import into Add Event flow
- [x] Recurrence policy unit tests
- [ ] Bulk-import integration tests

## Platform surfaces
- [ ] Final Android widget/launcher verification
- [ ] Focus/Lock Screen real-device capability verification
- [ ] Broader iOS/widget platform adapters

## Working rule
Keep shared event/schedule semantics independent from UI surfaces. Do not modify the legacy Android implementation for Flutter work. Do not claim device support without actual verification.
