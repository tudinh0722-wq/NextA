# NextA Current Task

## Status
IN PROGRESS

## Current Objective
Verify the newly standardized Phase A/B/C implementation on Android, especially Home Widget rendering and the end-to-end bulk import flow.

## Current State
- Phase A: manual event creation, validation, concrete Event persistence, daily/weekly planner, shared countdown semantics.
- Phase B: long-press event actions now offer **Sửa** and **Xóa**; editing preserves the existing event id. Home Widget remains traditional 4x2 `RemoteViews`.
- Home Widget timeline now shows large red start/end times; the progress line and small endpoint dot appear only while the event is in progress. The countdown is split into two lines for readability and refreshes on minute boundaries and event transitions.
- Widget text uses system theme semantic colors; the widget background remains the only translucent/mica-style surface. Red is reserved for the two time markers and active progress.
- Phase C: external-AI bulk import is implemented without an AI/OCR dependency. Users can copy the prompt, use their own AI with an image, paste the generated `NGÀY:/TÊN:/BẮT ĐẦU:/KẾT THÚC:/ĐỊA ĐIỂM:/GHI CHÚ:` text into NextA, validate it, preview it, then insert all valid events.
- Parser treats `NGÀY:` as the event boundary and validates required fields, date/time ordering, and the existing 47/30/30 character limits.

## Product Flow
```text
Phase A: create + plan
        ↓
Phase B: edit/delete + widget glance
        ↓
Phase C: external AI image → NextA text format → parse → validate → preview → bulk insert
```

## Important Boundaries
- Event remains a concrete dated occurrence. Do not add recurrence fields without an explicit architecture redesign.
- Home Widget stays on `RemoteViews`; no Glance.
- Core schedule semantics stay independent from UI surfaces.
- No per-event/per-second countdown timers.
- Do not claim launcher/device support until tested on real devices.

## Next Action
1. Build/run the Android app and fix any compile or runtime issues.
2. Test long-press → Sửa/Xóa on real event cards.
3. Test Phase C with a real timetable-image-to-text output, including malformed and over-length rows.
4. Test Home Widget 4x2 on target launchers, including current/upcoming transitions and dynamic theme changes.
5. Then finish duplicate/conflict detection and inline correction for Phase C.
