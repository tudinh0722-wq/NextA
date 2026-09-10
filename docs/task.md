# NextA Current Task

## Status
IN PROGRESS

## Current Objective
Build and verify the standardized event + alarm persistence flow on Android, then continue real-device surface verification and Phase C conflict handling.

## Current State
- Phase A: manual event creation, validation, concrete Event persistence, daily/weekly planner, shared countdown semantics.
- Phase B: long-press edit/delete, Home Widget, Focus/Lock Screen surface, and notification fallback.
- Phase C: external-AI bulk import with `NGÀY:/TÊN:/BẮT ĐẦU:/KẾT THÚC:/ĐỊA ĐIỂM:/GHI CHÚ:` contract, validation, preview, and bulk Room insert.
- Countdown refreshes on minute boundaries rather than polling every 30 seconds.
- Event and alarm persistence is standardized on Room. `events` stores event content; `event_alarms` stores reminder settings/state with a foreign-key cascade.
- Event + alarm writes are transactional. `AlarmManager` is runtime scheduling only; it is rebuilt from Room after boot/time/timezone/exact-alarm-permission changes.
- Existing legacy `nexta_alarms` SharedPreferences data has a one-time migration path into Room.
- Main planner UI was refactored toward a Samsung Calendar-inspired information hierarchy: month/week/day modes, selected-day agenda, today affordance, swipe navigation, priority-based day load, and semantic Material 3 colors.
- Event creation still flows through `MainActivity` and `AddEventScreen`; the new planner FAB calls the existing `onAddEvent(selectedDate)` callback.
- Home Widget and Focus/Lock Screen remain separate presentation surfaces.

## Important Boundaries
- Event remains a concrete dated occurrence. Do not add recurrence fields without an explicit architecture redesign.
- Home Widget and Focus/Lock Screen stay on `RemoteViews`; no Glance.
- Core schedule semantics stay independent from UI surfaces.
- No per-event/per-second countdown timers.
- Lock-screen availability is host/OEM dependent; do not claim universal support until tested on real devices.
- Do not claim launcher/device support until tested on real devices.
- Samsung-inspired UI means interaction hierarchy and visual language, not a pixel-for-pixel copy or Samsung-only implementation.

## Next Action
1. Build/run the Android app and fix any compile or layout issues introduced by the planner UI refactor.
2. Verify month/week/day swipe navigation and selected-day event filtering on a real device.
3. Test create/edit/delete event and confirm alarm settings follow the event lifecycle.
4. Test alarm + TTS + ACK + repeat after the Room migration.
5. Test reboot/timezone/exact-alarm-permission rescheduling.
6. Continue Home Widget / Focus-Lock real-device verification.
7. Finish duplicate/conflict detection and inline correction for Phase C.
