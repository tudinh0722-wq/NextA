# NextA — Project Context

> Flutter on `flutter-v2` is the active product implementation. The original Android implementation is legacy reference only.

## Current verified state

- Main planner UI is implemented in Flutter with month-first hierarchy, selected-day agenda, countdown, search, event editing, and Material 3 semantic theming.
- Samsung S23 runtime verification confirms the latest month/week collapse and expand behavior is stable.
- Horizontal month navigation is being refined into a real directional page transition: old and new month surfaces coexist during the animation and movement follows the swipe direction.
- Collapse remains a viewport/clip problem; do not force the natural 5-row month grid into the compact week height.

## Architecture

```text
presentation → application → domain → infrastructure/platform adapters
```

Shared event/countdown semantics remain platform-neutral. Android/iOS widgets and lock-screen surfaces remain platform adapters.

## Animation decisions

- Use Flutter animation primitives such as `AnimationController`, `AnimatedBuilder`, `Tween`, `SlideTransition`, `ScaleTransition`, `FadeTransition`, `ClipRect`, or equivalent.
- Horizontal month navigation is a page-transition problem and should keep old/new month content alive during the transition.
- Do not use `Future.delayed` to fake motion.
- Blur is optional and must not be added unless visual testing proves it improves the result.
