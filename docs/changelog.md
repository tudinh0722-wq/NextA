# NextA Changelog

## 2026-09-11 — Flutter planner verification and directional month transition

- Samsung S23 runtime verification confirmed the latest month/week collapse and expand behavior is stable.
- Directional horizontal month navigation is the next planner motion refinement: old and new month surfaces must coexist during the transition, with direction matching the swipe.
- Keep the transition frame-synchronized and implemented with Flutter animation primitives; do not use delayed rebuilds or timing hacks.

## 2026-09-10 — Flutter planner UI implementation

Decisions:
- Rebuilt the Flutter planner as a Samsung Calendar visual/interaction reference implementation rather than a generic Material calendar.
- Header uses dynamic `TH{month}` and a Today calendar affordance showing the current day number; neither is hard-coded.
- Month grid starts on Monday, keeps Sunday semantically red, fades adjacent-month dates, and renders compact per-day event bars.
- Selected date drives the agenda header and the floating `Thêm vào {day} Th{month}` action label.
- Agenda uses a time column, event color marker, title/details, dividers, priority indicator, and optional NextA countdown instead of Material event cards.
- Added tap selection, long-press add, month navigation, Today navigation, month/week collapse, event tap, and search entry points.
- UI colors are derived from Material 3 `ColorScheme`; Samsung source code/assets/branding are not used.
