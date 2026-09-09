# NextA Current Task

## Status
IN PROGRESS

## Current Objective
Build and verify the standardized event + alarm persistence flow on Android, then continue real-device surface verification and Phase C conflict handling.

## Current State
- Phase A: manual event creation, validation, concrete Event persistence, daily/weekly planner, shared countdown semantics.
- Phase B: long-press edit/delete, Home Widget, Focus/Lock Screen surface, and notification fallback.
- Phase C: external-AI bulk import with `NGÀY:/TÊN:/BẮT ĐẦU:/KẾT THÚC:/ĐỊA ĐIỂM:/GHI CHÚ:` contract, validation, preview, and bulk Room insert.
- Countdown now refreshes on minute boundaries rather than polling every 30 seconds.
- Event and alarm persistence is standardized on Room. `events` stores event content; `event_alarms` stores reminder settings/state with a foreign-key cascade.
- Event + alarm writes are transactional. `AlarmManager` is runtime scheduling only; it is rebuilt from Room after boot/time/timezone/exact-alarm-permission changes.
- Existing legacy `nexta_alarms` SharedPreferences data has a one-time migration path into Room.
- The old test-alarm cleanup path and SharedPreferences alarm store are removed.

## Important Boundaries
- Event remains a concrete dated occurrence. Do not add recurrence fields without an explicit architecture redesign.
- Home Widget and Focus/Lock Screen stay on `RemoteViews`; no Glance.
- Core schedule semantics stay independent from UI surfaces.
- No per-event/per-second countdown timers.
- Lock-screen availability is host/OEM dependent; do not claim universal support until tested on real devices.
- Do not claim launcher/device support until tested on real devices.

## Next Action
1. Build/run the Android app and fix any compile or migration issues.
2. Verify existing events survive Room 2 → 3 migration and legacy alarm settings migrate correctly.
3. Test create/edit/delete event and confirm alarm settings follow the event lifecycle.
4. Test alarm + TTS + ACK + repeat after the Room migration.
5. Test reboot/timezone/exact-alarm-permission rescheduling.
6. Continue Home Widget / Focus-Lock real-device verification.
7. Finish duplicate/conflict detection and inline correction for Phase C.
