import 'package:flutter/material.dart';

import '../../domain/event.dart';

class PlannerCalendar extends StatelessWidget {
  const PlannerCalendar({
    super.key,
    required this.month,
    required this.selected,
    required this.expanded,
    required this.eventsFor,
    required this.onSelect,
    this.animationDuration = const Duration(milliseconds: 420),
  });

  final DateTime month;
  final DateTime selected;
  final bool expanded;
  final List<NextAEvent> Function(DateTime) eventsFor;
  final ValueChanged<DateTime> onSelect;
  final Duration animationDuration;

  List<DateTime> get _monthDays {
    final first = DateTime(month.year, month.month, 1);
    final start = first.subtract(Duration(days: first.weekday - 1));
    return List.generate(35, (index) => start.add(Duration(days: index)));
  }

  List<DateTime> get _weekDays {
    final start = selected.subtract(Duration(days: selected.weekday - 1));
    return List.generate(7, (index) => start.add(Duration(days: index)));
  }

  @override
  Widget build(BuildContext context) {
    final targetHeight = expanded ? 299.0 : 83.0;
    return AnimatedContainer(
      duration: animationDuration,
      curve: Curves.easeOutCubic,
      height: targetHeight,
      clipBehavior: Clip.hardEdge,
      decoration: const BoxDecoration(),
      child: expanded
          ? _MonthGrid(
              days: _monthDays,
              month: month,
              selected: selected,
              eventsFor: eventsFor,
              onSelect: onSelect,
            )
          : _WeekGrid(
              days: _weekDays,
              selected: selected,
              eventsFor: eventsFor,
              onSelect: onSelect,
            ),
    );
  }
}

class _MonthGrid extends StatelessWidget {
  const _MonthGrid({
    required this.days,
    required this.month,
    required this.selected,
    required this.eventsFor,
    required this.onSelect,
  });

  final List<DateTime> days;
  final DateTime month;
  final DateTime selected;
  final List<NextAEvent> Function(DateTime) eventsFor;
  final ValueChanged<DateTime> onSelect;

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        const WeekdayHeader(),
        SizedBox(
          height: 274,
          child: GridView.builder(
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 2),
            itemCount: days.length,
            gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
              crossAxisCount: 7,
              mainAxisExtent: 54,
            ),
            itemBuilder: (context, index) {
              final day = days[index];
              final inMonth = day.year == month.year && day.month == month.month;
              return CalendarDayCell(
                date: day,
                inMonth: inMonth,
                selected: _sameDay(day, selected),
                events: eventsFor(day),
                onTap: () => onSelect(day),
              );
            },
          ),
        ),
      ],
    );
  }
}

class _WeekGrid extends StatelessWidget {
  const _WeekGrid({
    required this.days,
    required this.selected,
    required this.eventsFor,
    required this.onSelect,
  });

  final List<DateTime> days;
  final DateTime selected;
  final List<NextAEvent> Function(DateTime) eventsFor;
  final ValueChanged<DateTime> onSelect;

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        const WeekdayHeader(),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 2),
          child: SizedBox(
            height: 54,
            child: Row(
              children: days
                  .map(
                    (day) => Expanded(
                      child: CalendarDayCell(
                        date: day,
                        inMonth: true,
                        selected: _sameDay(day, selected),
                        events: eventsFor(day),
                        onTap: () => onSelect(day),
                      ),
                    ),
                  )
                  .toList(),
            ),
          ),
        ),
      ],
    );
  }
}

class WeekdayHeader extends StatelessWidget {
  const WeekdayHeader({super.key});

  static const labels = ['T.2', 'T.3', 'T.4', 'T.5', 'T.6', 'T.7', 'CN'];

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 7),
      child: Row(
        children: List.generate(
          7,
          (index) => Expanded(
            child: SizedBox(
              height: 25,
              child: Center(
                child: Text(
                  labels[index],
                  style: TextStyle(
                    fontSize: 11,
                    fontWeight: FontWeight.w600,
                    color: index == 6 ? scheme.error : scheme.onSurface,
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class CalendarDayCell extends StatelessWidget {
  const CalendarDayCell({
    super.key,
    required this.date,
    required this.inMonth,
    required this.selected,
    required this.events,
    required this.onTap,
  });

  final DateTime date;
  final bool inMonth;
  final bool selected;
  final List<NextAEvent> events;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return GestureDetector(
      onTap: onTap,
      child: Container(
        margin: const EdgeInsets.all(1.5),
        padding: const EdgeInsets.fromLTRB(3, 4, 3, 3),
        decoration: BoxDecoration(
          color: calendarTileColor(context, events, inMonth),
          borderRadius: BorderRadius.circular(8),
          border: selected
              ? Border.all(color: scheme.primary, width: 2)
              : null,
        ),
        child: Column(
          children: [
            SizedBox(
              height: 23,
              child: Align(
                alignment: Alignment.topCenter,
                child: Text('${date.day}', style: _dateStyle(scheme)),
              ),
            ),
            const Spacer(),
            ...events.take(3).map(
                  (event) => Padding(
                    padding: const EdgeInsets.symmetric(vertical: 1),
                    child: Container(
                      height: 4,
                      decoration: BoxDecoration(
                        color: eventColor(context, event),
                        borderRadius: BorderRadius.circular(3),
                      ),
                    ),
                  ),
                ),
          ],
        ),
      ),
    );
  }

  TextStyle _dateStyle(ColorScheme scheme) => TextStyle(
        fontSize: selected ? 12 : 13,
        fontWeight: selected ? FontWeight.w800 : FontWeight.w600,
        color: !inMonth
            ? scheme.onSurface.withValues(alpha: 0.42)
            : date.weekday == DateTime.sunday
                ? scheme.error
                : scheme.onSurface,
      );
}

bool _sameDay(DateTime a, DateTime b) =>
    a.year == b.year && a.month == b.month && a.day == b.day;

Color calendarTileColor(
  BuildContext context,
  List<NextAEvent> events,
  bool inMonth,
) {
  final scheme = Theme.of(context).colorScheme;
  if (events.isEmpty) {
    return scheme.surfaceContainerLow.withValues(alpha: inMonth ? 0.55 : 0.20);
  }

  var color = scheme.surface;
  for (final event in events.take(3)) {
    color = Color.lerp(color, eventColor(context, event), 0.075) ?? color;
  }
  return color;
}

Color eventColor(BuildContext context, NextAEvent event) {
  final scheme = Theme.of(context).colorScheme;
  return switch (event.type) {
    EventType.classEvent => scheme.primary,
    EventType.exam => scheme.error,
    EventType.assignment => scheme.tertiary,
    EventType.meeting => scheme.secondary,
    EventType.personal => scheme.primaryContainer,
    EventType.other => scheme.outline,
  };
}
