# NextA Architecture Notes

## Direction

NextA is moving from the original Android implementation to a Flutter cross-platform implementation. `main` remains the Android reference; `flutter-v2` is the active product branch.

## Layers

```text
UI / interaction
    ↓
presentation
    ↓
application policies / use cases
    ↓
domain models
    ↓
infrastructure / persistence / platform adapters
```

### Domain
Contains concepts such as events and event types. It should remain framework-independent.

### Application
Contains behavior/policies that should not be encoded directly into widgets, e.g. countdown horizon and formatting rules.

### Presentation
Contains planner screens, calendar widgets, editor sheets, theme and interaction state. Keep widgets focused and avoid turning `PlannerScreen` into a data/persistence layer.

### Infrastructure
Persistence and platform integrations belong here or behind explicit adapters. Android/iOS widget and lock-screen APIs should not leak into the shared domain model.

## Calendar rendering model

The month calendar is a fixed logical 5-row grid. Its cells have intentional geometry for date numbers and stacked event indicators.

Month/week collapse is a **viewport problem**, not a request to resize every child to the week height. Animation should therefore clip/reveal the existing calendar geometry rather than force a 5-row grid into an 83px constraint.

Horizontal month navigation is a separate **page transition** problem. The eventual implementation should keep old and new months alive during the transition so the user sees directional motion rather than an instant rebuild.

## State

Planner state currently includes at least:
- displayed month;
- selected date;
- expanded/collapsed calendar state;
- event collection/filtering relevant to the visible dates.

When state changes, preserve the distinction between:
- changing the selected date;
- changing the visible month;
- changing the calendar viewport (month/week).

That distinction is important for predictable gestures and animation.

## Animation principles

Use Flutter animation primitives (`AnimatedSwitcher`, `PageView`, `AnimationController`, `Tween`, `CurvedAnimation`, `ClipRect`, `SizeTransition`, etc.) according to the interaction.

Avoid:
- `Future.delayed` used only to make a transition appear slower;
- replacing a complete calendar with a new widget without a transition when the interaction implies navigation;
- adding blur just because a reference UI appears soft during motion.

The target is responsive, physical-feeling motion that can be interrupted or naturally completed.

## Dependency principle

Do not add a package for a small effect that Flutter can already implement reliably. Add a dependency when it provides meaningful cross-platform capability, maintenance value, or platform integration that would otherwise require substantial duplicated code.
