# NextA — Project Context

> This document is the compact handoff for Claude, Codex, Gemini, ChatGPT, or another AI joining the project. It records what has been decided, what is already implemented, what is currently being fixed, and why the project is moving in its current direction.

## 1. Product

NextA is a planner/calendar app. The original Android implementation on `main` is the reference implementation. The project is being reimplemented in Flutter on `flutter-v2` so the product can target Android and iOS from one UI architecture.

The goal is not to clone Samsung Calendar's source code or proprietary assets. The goal is to reproduce the useful interaction patterns and visual qualities with NextA's own code, data model and branding.

## 2. Why Flutter

The original Android implementation proved the interaction and visual direction, but Android-only UI does not solve the longer-term requirement of supporting multiple device families and iOS.

Flutter was selected for the planner UI because:
- one shared UI implementation can serve Android and iOS;
- custom calendar interaction and animation can be controlled precisely;
- Material 3 provides a strong semantic color/theme foundation;
- platform-specific integrations can later be isolated behind platform boundaries.

Do not move the whole planner back to Android-specific widgets merely to solve an isolated visual problem.

## 3. Repository structure

```text
NextA/
├── main/                         # original Android app; reference only
├── flutter/                      # active Flutter project
│   ├── lib/
│   │   ├── domain/
│   │   │   └── event.dart
│   │   ├── application/
│   │   │   └── countdown_policy.dart
│   │   └── presentation/
│   │       ├── planner_screen.dart
│   │       └── widgets/
│   │           ├── planner_calendar.dart
│   │           └── event_editor_sheet.dart
│   └── pubspec.yaml
├── AGENTS.md
└── docs/
```

## 4. Domain model

`flutter/lib/domain/event.dart` defines `NextAEvent` and `EventType`. Event types include class event, exam, assignment, meeting, personal and other. Events contain id, title, type, start/end, optional location/note, priority and optional recurrence id. The domain layer must not depend on Flutter UI.

## 5. Countdown

`flutter/lib/application/countdown_policy.dart` owns the basic countdown policy.

Current behavior:
- horizon: 14 days;
- past events do not get a countdown;
- events farther than the horizon do not get a countdown;
- >= 1 day uses days;
- shorter durations use hours/minutes;
- an ongoing event shows remaining time until its end.

Countdown is important product information, not secondary metadata.

## 6. Calendar UI decisions

The calendar is deliberately compact and Samsung-like in information density, but implemented independently.

### Header
- hamburger/menu on the left;
- centered dynamic month label such as `TH9` / `TH10`;
- search action;
- Today action showing the current day number.

### Month grid
- Monday is the first column.
- Labels: `T.2 T.3 T.4 T.5 T.6 T.7 CN`.
- Sunday uses the theme error color.
- Month uses a compact 5-row grid.
- Outside-month days remain visible and visually subordinate.
- Day cells have very light semantic pastel surfaces.
- Event colors tint the cell subtly.
- Event indicators are short horizontal bars stacked vertically.
- Date number sits toward the top; lower space is reserved for event bars.
- Selected date has a thicker outline around the entire cell.

### Agenda
No generic Flutter Card/ListTile styling is desired. Each event is conceptually two columns: title/location/note on the left and start/end plus countdown on the right. For ongoing events the countdown label is `Kết thúc sau`.

Do not make useful text pale gray simply to create hierarchy. Hierarchy should come from typography, spacing, grouping and position.

### FAB
The add action is a compact near-white pill with subtle elevation/border and a contextual label such as `Thêm vào 11 Th9` plus an add icon.

## 7. Interaction model

Implemented:
- tap date;
- month horizontal swipe;
- agenda horizontal swipe to change selected day;
- vertical swipe up: month → selected/current week;
- vertical swipe down: week → month;
- Today action;
- event tap;
- long press to add;
- event edit/delete;
- search.

Horizontal month navigation preserves the selected day as sensibly as possible when moving between months, clamping the day to the target month's maximum day.

## 8. Motion / animation direction

The target feel is a lightweight physical page transition:
- directional slide matching the swipe;
- subtle scale;
- restrained fade;
- easing with a little physical weight;
- old and new month coexist during the transition.

Directional month transition is now implemented with `AnimationController`/`AnimatedBuilder` and explicit transforms. The transition is frame-synchronized and does not use `Future.delayed`.

Blur is optional and should only be retained if visual testing shows it improves the intended page-like transition.

## 9. Recent implementation history

- stable 5-row month calendar;
- stacked event bars;
- clearer selected date and date typography;
- removed fake lunar text from the selected-date header;
- moved countdown into a dedicated right-hand column;
- improved note spacing;
- thicker/clearer add FAB;
- reduced calendar row height for compactness;
- theme seed selection wired after planner refactor;
- event editor guarded against async dialog context issues;
- collapse made reversible from the agenda/week view;
- selected-cell border made thicker and applied around the whole cell;
- calendar animation duration parameter was added;
- RenderFlex overflow during month→week collapse was addressed with viewport/clip-style behavior;
- Samsung S23 runtime verification confirmed collapse/expand stability;
- directional horizontal month transition implemented with coexisting old/new month surfaces.

## 10. Current RenderFlex lesson

The problematic pattern was effectively:

```text
AnimatedContainer(height: 299 → 83)
    └── _MonthGrid (natural height ~299)
```

During animation, Flutter gave `_MonthGrid` a constraint around `83px` while the month grid still needed its full layout. This produced the yellow/black `RenderFlex` overflow.

The correct mental model is:

```text
fixed/natural calendar content
        ↓
animated viewport / clip
        ↓
visible month or week area
```

Do not reintroduce a layout that asks a full 5-row month grid to physically fit inside the collapsed week height.

## 11. Theme and color

Use Material 3 `ColorScheme` semantics instead of hard-coded text colors wherever practical. Dynamic color is supported as a direction for Android-capable devices, with a seed/fallback theme for platforms where dynamic system colors are unavailable.

> Pastel is for surfaces, never a substitute for text hierarchy.

Important information must remain high-contrast in both light and dark themes.

## 12. Platform-specific future work

The original Android work explored lock-screen/widget presentation. This is a platform integration problem, not a reason to make the entire planner Android-specific.

Future architecture should isolate:
- Android home-screen widgets;
- Android lock-screen surfaces where platform APIs/permissions allow;
- iOS widgets/Live Activities or equivalent capabilities;
- platform permission/configuration differences.

The shared event model and planner UI should remain platform-neutral.

## 13. Current state / next work

### Done
- Flutter project and core structure established.
- Domain event model established.
- Countdown policy established.
- Planner calendar/month grid implemented.
- Agenda and event editing flow implemented.
- Material 3 theme direction established.
- Month/week gestures established.
- Selected cell styling refined.
- Collapse overflow issue identified and addressed.
- Samsung S23 runtime verification completed for collapse/expand.
- Directional horizontal month transition implemented.

### Current
- Verify directional month transition visually on Samsung S23.
- Watch for layout/hit-test exceptions during repeated or rapid month swipes.

### Next
1. Verify directional month transition on S23 and tune if necessary.
2. Continue cross-device responsive behavior.
3. Expand platform-specific widgets/lock-screen integrations behind platform boundaries.
4. Implement recurrence/data-model work when scheduled.
5. Complete persistence/production data flows and broader CRUD as the app moves toward release.

## 14. Why these choices

### Flutter instead of Android-only UI
Because the product needs Android + iOS and multiple device families.

### Material 3 colors instead of hard-coded colors
Because semantic color roles adapt better to light/dark/dynamic themes and device palettes.

### Strong text + pastel surfaces instead of gray metadata
Because calendar information must remain readable and hierarchy should come from multiple visual dimensions, not reduced contrast.

### Stacked event bars instead of side-by-side indicators
Because narrow calendar cells need to preserve a vertical event hierarchy without unreadable horizontal compression.

### Dedicated countdown column
Because countdown is a primary planning signal and deserves stable alignment independent of title length.

### Viewport/clip animation instead of shrinking the month grid
Because the month grid has a real content height. Collapsing should reveal less of it, not force its internal layout to become physically smaller than its intended geometry.

### Real animation instead of delays
Because UI motion should be frame-synchronized and interruptible rather than simulated by waiting.

### Directional month page transition
Because month navigation represents movement through adjacent pages; keeping both months alive during the transition preserves spatial continuity and makes swipe direction meaningful.

### Platform adapters instead of platform-specific planner code
Because Android/iOS widgets and lock-screen surfaces differ, while event semantics should remain shared.

## 15. Rules for future AI agents

- Read `AGENTS.md` and this file before substantial changes.
- Work on `flutter-v2` unless explicitly instructed otherwise.
- Never modify `main` for Flutter work.
- Preserve the original Android app as reference.
- Do not blindly copy Samsung proprietary assets/code.
- Inspect the existing widget tree before adding dependencies.
- Fix root layout/constraint problems rather than masking their visual symptoms.
- Keep animations real and composable; avoid timing hacks.
- Keep text contrast strong.
- Prefer Material semantic colors and existing Flutter primitives.
- Make one logical change at a time and keep commits focused.
- After UI changes, run `flutter analyze` and then run the app on a real/emulated device when possible.
- Never claim a runtime fix is verified unless it has actually been run.
- Update this document when a major architectural or product decision changes.
