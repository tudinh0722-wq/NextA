# NextA — Decision Log

## Cross-platform Flutter
**Decision:** Build the new planner in Flutter.

**Why:** The product is intended for Android and iOS and multiple device families. The original Android implementation remains a behavioral and visual reference, not the long-term shared UI architecture.

## Preserve `main`
**Decision:** Never modify/delete the original Android implementation as part of Flutter work.

**Why:** It is a useful reference for behavior, historical decisions and platform-specific ideas.

## Material 3 semantic colors
**Decision:** Prefer Material 3 `ColorScheme` roles and dynamic color/fallback seed themes.

**Why:** Semantic colors adapt to light/dark/device themes better than a hard-coded palette and keep meaning separate from exact RGB values.

## Pastel surfaces, strong text
**Decision:** Pastel colors are used mainly for calendar surfaces/event tinting; important text stays high contrast.

**Why:** Reducing text contrast is a poor substitute for hierarchy. Typography, spacing and position communicate importance without harming readability.

## Stacked event indicators
**Decision:** Event bars are stacked vertically inside day cells.

**Why:** Calendar cells are narrow. Vertical stacking preserves multiple event signals without squeezing them side-by-side.

## Dedicated countdown column
**Decision:** Agenda countdown/time occupies a stable right-hand column.

**Why:** Countdown is a primary planning signal and should remain visually scannable regardless of title/location length.

## Viewport-based collapse
**Decision:** Month→week collapse should reveal/clip the calendar viewport rather than resize the whole 5-row month grid into week height.

**Why:** The grid has intentional cell geometry. Forcing it into ~83px creates impossible constraints and RenderFlex overflow.

## Real animation primitives
**Decision:** Use Flutter animation primitives rather than delayed rebuilds.

**Why:** Real animations are frame-synchronized, composable and interruptible. `Future.delayed` only makes a state change happen later; it does not create meaningful motion.

## Page-like month navigation
**Decision:** Horizontal month changes should eventually use a directional page transition.

**Why:** An instant rebuild feels mechanical. A directional slide with subtle scale/fade better communicates spatial continuity and matches the intended polished calendar feel.

## Blur is optional
**Decision:** Do not assume blur is required for the page effect.

**Why:** Apparent softness during platform transitions can come from compositing, alpha, scale and motion itself. Blur adds cost and can make text/UI muddy. Test it before keeping it.

## Platform adapters
**Decision:** Android/iOS widgets, lock-screen surfaces and similar OS features should be isolated from shared planner/domain code.

**Why:** Platform capabilities differ, while event semantics and planner behavior should remain reusable.
