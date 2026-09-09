# NextA Development Plan

> Status is based on the current `main` source tree. Source code is the final authority.

## Phase A — Core + Manual Event Planner
- [x] Android app foundation, Hilt, Room, repository
- [x] Concrete Event model and shared schedule semantics
- [x] Sample data seeding
- [x] Add event form and validation
- [x] Main day/week planner with Material 3 navigation
- [x] Current/upcoming/past states and countdown policy

## Phase B — Event Management + Surfaces
- [x] Long-press event actions
- [x] Edit event flow preserving event identity
- [x] Delete event flow with confirmation
- [x] Home Widget 4x2 RemoteViews
- [x] Widget current + next / next two selection
- [x] Widget minute countdown refresh only while relevant
- [x] Widget progress only for the active event
- [x] Material/system dynamic colors for widget text
- [x] Mica-style translucent widget background
- [x] Focus/Lock Screen widget provider and compact Now/Next surface
- [ ] Final device/launcher verification
- [ ] Focus/Lock Screen real-device capability verification
- [x] Notification fallback surface

> Phase B deliberately keeps `Event` concrete. Recurrence is not introduced into the data model until there is an explicit architecture decision; this avoids mixing recurring rules with concrete occurrences.

## Phase C — AI-Assisted Bulk Import
- [x] Stable `NEXTA_V1` text contract concept using `NGÀY:` as the record boundary/key
- [x] Copyable prompt for external AI/OCR tools
- [x] Paste/import screen
- [x] Parser tolerant of markdown fences and Vietnamese key accents
- [x] Required-field and length validation
- [x] Preview valid/invalid rows before persistence
- [x] Bulk Room insert
- [x] No AI/OCR dependency inside NextA
- [ ] Duplicate/conflict detection
- [ ] Inline correction of invalid rows
- [ ] Versioned import contract and migration strategy

## Product Surface Phases
```text
Phase A: create + plan
        ↓
Phase B: manage + glance
        ↓
Phase C: external AI → text → validate → preview → database
```

## Deferred
- [ ] Recurrence rule model, only after explicit architecture redesign
- [ ] Cloud/backend synchronization
- [ ] Large platform integrations

## Working Rule
Keep the app's core Event/schedule semantics independent from UI surfaces. Home Widget and Focus/Lock Screen remain `RemoteViews`; do not introduce Glance unless explicitly requested. Do not claim device/launcher support without real-device verification.
