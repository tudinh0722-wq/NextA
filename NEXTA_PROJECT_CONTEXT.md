# NextA — Project Context

> **Purpose:** Source of truth for developers and AI coding agents taking over the NextA project.
>
> This document describes the current product direction, architecture, completed work, remaining work, and constraints.  
> When historical ideas conflict with this document, **this document represents the current decision**.

---

# 1. Project Overview

**NextA** is an Android local-first schedule assistant focused on one simple question:

> **“What do I need to do now, and how long until then?”**

The initial use case is a student's class schedule.

The problem being solved is not lack of access to timetable information. The problem is **friction**.

A school application may require login, biometric authentication, navigation through multiple screens, etc. just to answer something simple such as:

- What class do I have now?
- What is next?
- How long until it starts?
- How long until the current class ends?
- Where is it?

NextA puts this information directly on the Android home screen through a widget.

The widget is therefore a **core product feature**, not merely an optional UI.

---

# 2. Core Product Philosophy

NextA should provide useful information with minimal interaction.

The ideal interaction is:

```text
Look at home screen
        ↓
Immediately understand current/next schedule
```

The product should avoid becoming a full school-management application.

The MVP should remain small and focused.

---

# 3. Fundamental Domain Concept: Event

The most important architectural decision is:

> **The fundamental unit of the system is a concrete, dated Event.**

An Event represents **one actual occurrence at a specific date and time**.

Example:

```text
Thiết kế phần mềm
2026-09-07 09:40 → 2026-09-07 11:25
302 - A9 - Cơ sở 1 - Khu A
```

This is one Event.

---

## 3.1 Do NOT use recurrence as the core Event model

The system must NOT fundamentally represent schedules as:

```text
Monday
Period 1
Odd weeks
Theory weeks
Practice weeks
```

or:

```text
day_of_week + recurring_rule
```

The following complexities should ultimately be normalized into concrete Events:

- theory weeks
- practice weeks
- alternating weeks
- off weeks
- make-up classes
- changed rooms
- holidays
- special schedule changes

For example:

```text
TKB / import / OCR
        ↓
Concrete Events
        ↓
Room
        ↓
Repository
        ↓
ScheduleStateEngine
        ↓
Widget
```

The timetable is a **source of Events**, not the core domain model.

---

# 4. Current Architecture

Target architecture:

```text
                    Event
                      ↓
                 Repository
                      ↓
             ScheduleStateEngine
                      ↓
                   Widget
```

More broadly:

```text
Data source
    ↓
Event
    ↓
Room / Repository
    ↓
Domain logic
    ↓
Widget UI
```

The architecture intentionally separates:

- data persistence
- domain/state logic
- presentation

The Widget should not contain the core schedule decision logic.

---

# 5. Technology

Current Android stack:

- Kotlin
- Android
- Jetpack Compose for the basic application UI
- Room for local persistence
- Jetpack Glance for the home-screen widget

The MVP is local-first.

There is currently no requirement for:

- backend
- cloud sync
- account system
- login
- GPS
- OCR/AI import
- smartwatch integration
- notifications

These are future possibilities, not current MVP requirements.

---

# 6. Event Model

Current Event model represents a single occurrence.

Conceptually:

```kotlin
Event(
    id,
    title,
    type,
    startDateTime,
    endDateTime,
    location,
    note,
    priority
)
```

Required core properties:

```text
id
title
type
startDateTime
endDateTime
location
note
```

Priority is also part of the current product direction.

---

## 6.1 Event Type

Existing Event types:

```text
CLASS_OFFLINE
CLASS_ONLINE
TASK
OTHER
```

`Event.type` uses the existing `EventType`.

---

## 6.2 Date/Time

Events use concrete date/time values:

```text
startDateTime
endDateTime
```

using Kotlin date/time types such as `LocalDateTime`.

Do not revert to:

```text
occurrences: List<LocalDate>
startTime: LocalTime
endTime: LocalTime
```

That old design represented a recurring rule and has intentionally been removed.

---

# 7. Event Priority

Events can have an importance/priority level.

Current intended levels:

```text
0 = normal
1 = important
2 = very important
```

Widget behavior:

- priority `0` → no background highlight
- priority `1` → yellow background
- priority `2` → red background

Priority belongs to the **Event**.

It is NOT a separate day-level database object.

For the 7-day overview:

```text
dayPriority = highest priority of Events on that day
```

If a day has multiple Events, the highest priority determines the day's highlight.

If all Events on that day are normal:

```text
no background color
```

---

# 8. Persistence

Room is already set up.

Relevant components include:

```text
AppDatabase
EventDao
EventEntity
EventRepository
```

`EventEntity` has been migrated conceptually from the old recurrence-oriented schema to concrete Event fields.

Current persistence direction:

```text
one concrete Event occurrence
        ↓
one persisted record
```

`startDateTime` and `endDateTime` are stored as ISO-8601 strings.

`EventType` is stored as a String using its enum name.

Repository maps:

```text
EventEntity ↔ Event
```

The old `occurrencesJson` field and Gson-based occurrence handling were removed.

---

# 9. Database Migration Note

The EventEntity schema has changed.

Existing app data on a previously installed development build may cause a Room schema mismatch/crash unless:

- the app is reinstalled, or
- a proper Room migration is added.

For development, a clean reinstall is acceptable unless migration becomes necessary.

Do not introduce unnecessary migration complexity during unrelated tasks.

---

# 10. ScheduleStateEngine

`ScheduleStateEngine` is the core business-logic component.

Its purpose is to answer:

> Given a set of concrete Events and the current time, what should the user see?

The state model contains:

```text
UPCOMING
IN_PROGRESS
NO_MORE
```

---

## 10.1 UPCOMING

Condition:

```text
now < event.startDateTime
```

The relevant event is the earliest applicable future Event.

Countdown target:

```text
event.startDateTime
```

Widget wording:

```text
Còn X phút nữa vào tiết
```

---

## 10.2 IN_PROGRESS

Condition:

```text
event.startDateTime <= now < event.endDateTime
```

Countdown target:

```text
event.endDateTime
```

Widget wording:

```text
Còn X phút nữa ra tiết
```

---

## 10.3 NO_MORE

`NO_MORE` does **not** mean:

> “There are no more Events today.”

The intended meaning is:

> **There are no Events remaining from the current time onward.**

Therefore:

```text
Today has ended
        ↓
Tomorrow has an Event
        ↓
NOT NO_MORE
        ↓
Tomorrow's Event is next
```

Only return `NO_MORE` when there are genuinely no future Events available.

---

## 10.4 Time Boundaries

Example:

```text
Event:
09:40 → 11:25
```

Expected:

```text
09:39 → UPCOMING
09:40 → IN_PROGRESS
10:30 → IN_PROGRESS
11:24 → IN_PROGRESS
11:25 → event finished
```

If another Event starts exactly at 11:25:

```text
11:25 → second Event is IN_PROGRESS
```

Boundary behavior must remain precise.

---

## 10.5 Unsorted Input

The state engine must not assume Events are already sorted.

It should correctly determine:

- current Event
- earliest upcoming Event
- next Events

from an arbitrary Event list.

---

# 11. Widget UX

The Widget is the primary product surface.

The current intended layout has **two major sections**.

---

## 11.1 Top: Seven-Day Headline

Show only the next seven weekdays.

Example:

```text
T2   T3   T4   T5   T6   T7   CN
```

Do **NOT** show date numbers in this headline.

The user does not care about seeing:

```text
07 08 09 10 ...
```

The purpose is simply to give a quick visual overview of the next seven days.

---

## 11.2 Day Highlighting

Each weekday cell represents that day's Events.

The background color is determined by the highest Event priority on that day.

Example:

```text
T2   T3   T4   T5   T6   T7   CN
     🔴   🟡
```

Conceptually:

```text
normal Event       → no background
important Event    → yellow
very important     → red
```

Today must remain visually distinguishable from other days.

Priority highlighting must not make it impossible to identify today.

---

# 12. Widget: Current and Next Events

Below the seven-day headline, show only the **next 2–3 Events**.

Do not dump the entire timetable into the widget.

The list may contain:

- current Event
- later Event today
- Event tomorrow
- Event several days later

Each Event should show at least:

```text
title
time/date when useful
location
```

If an Event is currently in progress, it should remain visually prominent.

---

# 13. Countdown

Countdown is derived from a target timestamp.

Never treat countdown as an independent persistent timer.

Concept:

```text
countdown = targetDateTime - currentDateTime
```

For UPCOMING:

```text
target = startDateTime
```

For IN_PROGRESS:

```text
target = endDateTime
```

The system must not use a continuously running per-second background loop.

---

# 14. Manual Event Creation

A critical part of the product that should be implemented is **manual Event creation**.

The system needs a way for the user to create an Event directly.

At minimum there should be an:

```text
+ Add Event
```

button/action in the application UI.

The basic flow:

```text
User taps Add Event
        ↓
Event form
        ↓
User enters data
        ↓
Validate
        ↓
Create Event
        ↓
Save to Room
        ↓
Repository
        ↓
Widget sees new Event
```

The manual-created Event must use the **same Event model** as imported/generated Events.

There should NOT be a separate manual-event data model.

---

## 14.1 Minimum Manual Event Form

The form should eventually support:

```text
Title
Type
Date
Start time
End time
Location
Note
Priority
```

Priority options:

```text
Normal
Important
Very important
```

The form does not need to be complicated.

---

# 15. Future Event Sources

Different input mechanisms should all eventually produce the same Event objects.

Potential future flow:

```text
Manual input ───────┐
                    │
TKB import ─────────┼──→ Event
                    │
OCR / AI ───────────┘
```

This is an important architectural advantage of the Event-based model.

OCR/AI does not need to understand the widget.

It only needs to produce valid concrete Events.

---

# 16. Sample Events

The current development/test data includes:

```text
Thiết kế phần mềm
CLASS_OFFLINE
2026-09-07 09:40
2026-09-07 11:25
302 - A9 - Cơ sở 1 - Khu A

Tiếng Anh Công nghệ thông tin 1
CLASS_OFFLINE
2026-09-07 12:30
2026-09-07 14:10
308 - A9 - Cơ sở 1 - Khu A

Phát triển ứng dụng thương mại điện tử
CLASS_OFFLINE
2026-09-08 15:10
2026-09-08 17:45
402 - A9 - Cơ sở 1 - Khu A

Cơ sở dữ liệu
CLASS_OFFLINE
2026-09-09 08:45
2026-09-09 10:30
205 - A9 - Cơ sở 1 - Khu A

Tiếng Anh Công nghệ thông tin 1
CLASS_OFFLINE
2026-09-10 12:30
2026-09-10 14:10
308 - A9 - Cơ sở 1 - Khu A

Kiểm thử phần mềm
CLASS_ONLINE
2026-09-12 07:00
2026-09-12 09:35
Khu A_PH Online 05 - Khu A_Online - Cơ sở 1 - Khu A

Thiết kế phần mềm
CLASS_OFFLINE
2026-09-14 09:40
2026-09-14 11:25
302 - A9 - Cơ sở 1 - Khu A
```

These are **concrete Events**, not recurrence rules.

---

# 17. Current Code Structure

Current project structure is approximately:

```text
com.nexta
│
├── MainActivity.kt
├── MainApplication.kt
│
├── data
│   ├── local
│   │   ├── AppDatabase.kt
│   │   ├── EventDao.kt
│   │   └── EventEntity.kt
│   │
│   ├── model
│   │   ├── Event.kt
│   │   ├── EventType.kt
│   │   └── ScheduleResult.kt
│   │
│   └── repository
│       └── EventRepository.kt
│
├── domain
│   └── ScheduleStateEngine.kt
│
├── di
│   └── DatabaseModule.kt
│
└── ui
    ├── MainScreen.kt
    └── MainViewModel.kt
```

Glance widget code has been introduced during the widget phase.

The exact widget file structure may evolve, but the architecture should remain:

```text
Event
 ↓
Repository
 ↓
ScheduleStateEngine
 ↓
Glance Widget
```

---

# 18. Completed Development Phases

## PHASE 1 — Audit

Status:

```text
DONE
```

The repository was inspected and the following were identified:

- Event used recurrence-like `occurrences`
- Room stored occurrences as JSON
- State Engine lacked explicit states
- Widget did not exist

---

## PHASE 2 — Event Model

Status:

```text
DONE
```

Completed:

- Event converted to concrete occurrence model
- Added `type`
- Added `startDateTime`
- Added `endDateTime`
- Removed `occurrences`
- Updated EventEntity
- Updated Repository
- Removed old Gson occurrence handling
- Updated existing UI/dummy data

Build:

```text
:app:assembleDebug
SUCCESS
```

---

## PHASE 3 — State Engine

Status:

```text
DONE
```

Completed:

- explicit schedule states
- correct time-boundary handling
- current/upcoming event logic
- multiple-event handling
- unsorted event handling
- focused unit tests

---

## PHASE 4 — Android Widget

Status:

```text
DONE / IMPLEMENTED
```

The widget was implemented using Jetpack Glance.

It is intended to display:

- UPCOMING
- IN_PROGRESS
- NO_MORE
- event title
- location
- countdown

The supplied sample Events are used for development/testing.

---

## PHASE 5 — Current Direction

The original Phase 5 focused only on countdown/update scheduling.

The product direction was subsequently expanded.

Current Phase 5 direction is:

```text
7-day weekday overview
        +
Event priority highlighting
        +
next 2–3 Events
        +
future-day next Event handling
```

The old assumption:

```text
No more Events today = NO_MORE
```

has been explicitly rejected.

The correct behavior is:

```text
No more Events today
        ↓
search future Events
        ↓
show next Event
```

---

# 19. Immediate Remaining Work

The next practical development steps are:

### 1. Finish/verify the new widget overview

Verify:

- seven weekdays displayed
- no date numbers
- today distinguishable
- priority colors work
- next 2–3 Events displayed
- tomorrow/future Events appear when today is finished

### 2. Countdown/update mechanism

Ensure the widget updates correctly at meaningful schedule boundaries.

Avoid per-second background loops.

### 3. Manual Event creation

Add:

```text
+ Add Event
```

and the minimum Event form.

### 4. Verify complete data flow

The desired end-to-end flow is:

```text
Manual Event
      ↓
Room
      ↓
Repository
      ↓
ScheduleStateEngine
      ↓
Widget
```

Once this works, the MVP has a complete usable lifecycle.

---

# 20. Explicit MVP Non-Goals

Do NOT add these unless explicitly requested:

```text
OCR
AI timetable extraction
GPS
automatic travel estimation
notifications
smartwatch
cloud sync
backend
authentication
accounts
social features
iOS version
complex recurring schedule engine
full calendar management
school-system integration
```

The existence of these ideas does not mean they belong in the current implementation.

---

# 21. AI Coding Agent Rules

AI agents are implementation tools, not product decision-makers.

The project owner defines:

```text
WHAT
WHY
PRODUCT RULES
SCOPE
ACCEPTANCE CRITERIA
```

The coding agent decides:

```text
HOW
Kotlin implementation
Android APIs
Glance implementation
Room implementation
```

Agents must not invent new product scope.

---

## 21.1 Phase Discipline

Each task should be narrowly scoped.

Recommended execution pattern:

```text
Implement one phase
        ↓
Build/test once
        ↓
If failure:
    one targeted fix
        ↓
Build/test once more
        ↓
STOP
```

Do NOT allow:

```text
fix
→ build
→ fix
→ build
→ refactor
→ fix
→ build
→ ...
```

indefinitely.

After each phase, the agent should report:

```text
Files changed
What changed
Build/test result
Remaining issues
```

Then STOP and wait for the next instruction.

---

# 22. Important Architectural Rules

These decisions should not be changed casually.

### Rule 1

**Concrete Event is the core domain object.**

### Rule 2

Do not turn Event back into a recurrence-rule model.

### Rule 3

TKB is an input/source of Events.

### Rule 4

The State Engine owns schedule-state decisions.

### Rule 5

The Widget displays state; it should not reinvent business logic.

### Rule 6

Manual Events and imported Events use the same Event model.

### Rule 7

Priority belongs to Event.

### Rule 8

Day highlighting is derived from the highest Event priority for that day.

### Rule 9

`NO_MORE` means no Events remain from now onward, not merely no Events today.

### Rule 10

Countdown must be derived from target timestamps.

### Rule 11

Do not use a per-second background loop.

### Rule 12

Keep the MVP small.

---

# 23. Product Mental Model

The simplest way to understand NextA is:

```text
              EVENTS
                 │
                 ▼
        "What is happening
          around me now?"
                 │
                 ▼
        SCHEDULE STATE
                 │
       ┌─────────┼─────────┐
       ▼         ▼         ▼
   UPCOMING  IN_PROGRESS NO_MORE
       │         │
       └────┬────┘
            ▼
        WIDGET
            │
            ▼
 "What do I need to do
    and when?"
```

The 7-day overview adds context:

```text
7 weekdays
     ↓
priority / important days
     ↓
current or next Event
     ↓
2–3 upcoming Events
```

The product should remain **ambient, fast, and glanceable**.

---

# 24. One-Sentence Project Definition

> **NextA is a local-first Android schedule widget that turns concrete dated Events into an immediate, glanceable answer to “what do I need to do now, and how long until then?”**