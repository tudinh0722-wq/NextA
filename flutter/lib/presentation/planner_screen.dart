import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../application/countdown_policy.dart';
import '../domain/event.dart';

class PlannerScreen extends StatefulWidget {
  const PlannerScreen({super.key, required this.events});

  final List<NextAEvent> events;

  @override
  State<PlannerScreen> createState() => _PlannerScreenState();
}

class _PlannerScreenState extends State<PlannerScreen> {
  late DateTime _selectedDate;
  late DateTime _visibleMonth;
  bool _calendarExpanded = true;
  final CountdownPolicy _countdownPolicy = const CountdownPolicy();

  @override
  void initState() {
    super.initState();
    final today = DateTime.now();
    _selectedDate = DateTime(today.year, today.month, today.day);
    _visibleMonth = DateTime(today.year, today.month);
  }

  DateTime _monthShift(DateTime month, int delta) =>
      DateTime(month.year, month.month + delta);

  void _changeMonth(int delta) {
    final target = _monthShift(_visibleMonth, delta);
    final maxDay = DateTime(target.year, target.month + 1, 0).day;
    final day = _selectedDate.day.clamp(1, maxDay);
    setState(() {
      _visibleMonth = target;
      _selectedDate = DateTime(target.year, target.month, day);
    });
  }

  void _goToday() {
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    setState(() {
      _selectedDate = today;
      _visibleMonth = DateTime(today.year, today.month);
    });
  }

  List<DateTime> _calendarDays() {
    final first = DateTime(_visibleMonth.year, _visibleMonth.month, 1);
    // Monday = 1 ... Sunday = 7. The month view intentionally uses five rows;
    // the final row contains only the first days of the following month when needed.
    final leading = first.weekday - DateTime.monday;
    final firstCell = first.subtract(Duration(days: leading));
    final rowCount = _calendarExpanded ? 5 : 1;
    return List.generate(
      rowCount * 7,
      (index) => DateTime(firstCell.year, firstCell.month, firstCell.day + index),
    );
  }

  List<DateTime> _weekDays() {
    final monday = _selectedDate.subtract(
      Duration(days: _selectedDate.weekday - DateTime.monday),
    );
    return List.generate(7, (index) =>
        DateTime(monday.year, monday.month, monday.day + index));
  }

  List<NextAEvent> _eventsFor(DateTime date) => widget.events.where((event) {
        final local = event.start;
        return local.year == date.year &&
            local.month == date.month &&
            local.day == date.day;
      }).toList()
    ..sort((a, b) => a.start.compareTo(b.start));

  bool _sameDay(DateTime a, DateTime b) =>
      a.year == b.year && a.month == b.month && a.day == b.day;

  String _monthLabel(DateTime month) => 'TH${month.month}';

  String _selectedHeader(DateTime date) {
    const weekdays = ['T.2', 'T.3', 'T.4', 'T.5', 'T.6', 'T.7', 'CN'];
    return '${date.day}   ${weekdays[date.weekday - 1]}';
  }

  @override
  Widget build(BuildContext context) {
    final days = _calendarDays();

    return Scaffold(
      body: SafeArea(
        child: GestureDetector(
          onHorizontalDragEnd: (details) {
            final velocity = details.primaryVelocity ?? 0;
            if (velocity.abs() < 200) return;
            _changeMonth(velocity < 0 ? 1 : -1);
          },
          child: Column(
            children: [
              _TopBar(
                monthLabel: _monthLabel(_visibleMonth),
                todayDay: DateTime.now().day,
                onToday: _goToday,
                onSearch: () => _showSearch(context),
                onMenu: () => ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('Menu')),
                ),
              ),
              AnimatedSize(
                duration: const Duration(milliseconds: 180),
                curve: Curves.easeOut,
                child: _calendarExpanded
                    ? _MonthCalendar(
                        days: days,
                        visibleMonth: _visibleMonth,
                        selectedDate: _selectedDate,
                        eventsFor: _eventsFor,
                        sameDay: _sameDay,
                        onSelect: (date) => setState(() => _selectedDate = date),
                        onLongPress: (date) => _createEvent(context, date),
                      )
                    : _WeekCalendar(
                        days: _weekDays(),
                        selectedDate: _selectedDate,
                        eventsFor: _eventsFor,
                        sameDay: _sameDay,
                        onSelect: (date) => setState(() => _selectedDate = date),
                      ),
              ),
              _CalendarToggle(
                expanded: _calendarExpanded,
                onTap: () => setState(() => _calendarExpanded = !_calendarExpanded),
              ),
              const Divider(height: 1),
              Expanded(
                child: Stack(
                  children: [
                    _Agenda(
                      date: _selectedDate,
                      header: _selectedHeader(_selectedDate),
                      events: _eventsFor(_selectedDate),
                      countdownPolicy: _countdownPolicy,
                      onEventTap: (event) => _showEvent(context, event),
                      onEmptyTap: () => _createEvent(context, _selectedDate),
                    ),
                    Positioned(
                      left: 20,
                      right: 20,
                      bottom: 16,
                      child: _AddEventPill(
                        label: 'Thêm vào ${_selectedDate.day} Th${_selectedDate.month}',
                        onTap: () => _createEvent(context, _selectedDate),
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  void _createEvent(BuildContext context, DateTime date) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text('Tạo sự kiện ngày ${date.day}/${date.month}')),
    );
  }

  void _showEvent(BuildContext context, NextAEvent event) {
    final time = '${DateFormat.Hm().format(event.start)} – ${DateFormat.Hm().format(event.end)}';
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text('${event.title} · $time')),
    );
  }

  void _showSearch(BuildContext context) {
    showDialog<void>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Tìm kiếm'),
        content: TextField(
          autofocus: true,
          decoration: const InputDecoration(hintText: 'Tên sự kiện'),
          onSubmitted: (_) => Navigator.pop(context),
        ),
      ),
    );
  }
}

class _TopBar extends StatelessWidget {
  const _TopBar({
    required this.monthLabel,
    required this.todayDay,
    required this.onToday,
    required this.onSearch,
    required this.onMenu,
  });

  final String monthLabel;
  final int todayDay;
  final VoidCallback onToday;
  final VoidCallback onSearch;
  final VoidCallback onMenu;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 64,
      child: Row(
        children: [
          IconButton(
            tooltip: 'Menu',
            onPressed: onMenu,
            icon: const Icon(Icons.menu_rounded),
          ),
          const Spacer(),
          Text(
            monthLabel,
            style: const TextStyle(fontSize: 21, fontWeight: FontWeight.w700),
          ),
          const Spacer(),
          IconButton(
            tooltip: 'Tìm kiếm',
            onPressed: onSearch,
            icon: const Icon(Icons.search_rounded),
          ),
          Padding(
            padding: const EdgeInsets.only(right: 8),
            child: InkWell(
              onTap: onToday,
              borderRadius: BorderRadius.circular(12),
              child: SizedBox(
                width: 38,
                height: 38,
                child: Stack(
                  alignment: Alignment.center,
                  children: [
                    const Icon(Icons.calendar_today_outlined, size: 27),
                    Padding(
                      padding: const EdgeInsets.only(top: 3),
                      child: Text(
                        '$todayDay',
                        style: const TextStyle(fontSize: 11, fontWeight: FontWeight.w700),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _MonthCalendar extends StatelessWidget {
  const _MonthCalendar({
    required this.days,
    required this.visibleMonth,
    required this.selectedDate,
    required this.eventsFor,
    required this.sameDay,
    required this.onSelect,
    required this.onLongPress,
  });

  final List<DateTime> days;
  final DateTime visibleMonth;
  final DateTime selectedDate;
  final List<NextAEvent> Function(DateTime) eventsFor;
  final bool Function(DateTime, DateTime) sameDay;
  final ValueChanged<DateTime> onSelect;
  final ValueChanged<DateTime> onLongPress;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Column(
      children: [
        const _WeekdayRow(),
        GridView.builder(
          padding: const EdgeInsets.symmetric(horizontal: 8),
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          itemCount: days.length,
          gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: 7,
            mainAxisExtent: 54,
          ),
          itemBuilder: (context, index) {
            final date = days[index];
            final inMonth = date.month == visibleMonth.month;
            final selected = sameDay(date, selectedDate);
            final events = eventsFor(date);
            final sunday = date.weekday == DateTime.sunday;
            return GestureDetector(
              onTap: () => onSelect(date),
              onLongPress: () => onLongPress(date),
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 2),
                child: Column(
                  children: [
                    const SizedBox(height: 1),
                    AnimatedContainer(
                      duration: const Duration(milliseconds: 120),
                      width: 38,
                      height: 38,
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        border: selected
                            ? Border.all(color: scheme.onSurface, width: 2)
                            : null,
                      ),
                      alignment: Alignment.center,
                      child: Text(
                        '${date.day}',
                        style: TextStyle(
                          fontSize: 14,
                          fontWeight: selected ? FontWeight.w700 : FontWeight.w500,
                          color: !inMonth
                              ? scheme.onSurface.withValues(alpha: .28)
                              : sunday
                                  ? scheme.error
                                  : scheme.onSurface,
                        ),
                      ),
                    ),
                    const SizedBox(height: 1),
                    SizedBox(
                      height: 11,
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.start,
                        children: events.take(2).map((event) {
                          return Container(
                            width: double.infinity,
                            height: 3,
                            margin: const EdgeInsets.symmetric(horizontal: 2, vertical: 1),
                            decoration: BoxDecoration(
                              color: _eventColor(context, event),
                              borderRadius: BorderRadius.circular(2),
                            ),
                          );
                        }).toList(),
                      ),
                    ),
                  ],
                ),
              ),
            );
          },
        ),
      ],
    );
  }
}

class _WeekCalendar extends StatelessWidget {
  const _WeekCalendar({
    required this.days,
    required this.selectedDate,
    required this.eventsFor,
    required this.sameDay,
    required this.onSelect,
  });

  final List<DateTime> days;
  final DateTime selectedDate;
  final List<NextAEvent> Function(DateTime) eventsFor;
  final bool Function(DateTime, DateTime) sameDay;
  final ValueChanged<DateTime> onSelect;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Column(
      children: [
        const _WeekdayRow(),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 8),
          child: Row(
            children: days.map((date) {
              final selected = sameDay(date, selectedDate);
              final sunday = date.weekday == DateTime.sunday;
              final events = eventsFor(date);
              return Expanded(
                child: GestureDetector(
                  onTap: () => onSelect(date),
                  child: SizedBox(
                    height: 57,
                    child: Column(
                      children: [
                        Container(
                          width: 38,
                          height: 38,
                          decoration: BoxDecoration(
                            shape: BoxShape.circle,
                            border: selected
                                ? Border.all(color: scheme.onSurface, width: 2)
                                : null,
                          ),
                          alignment: Alignment.center,
                          child: Text(
                            '${date.day}',
                            style: TextStyle(
                              fontSize: 14,
                              fontWeight: selected ? FontWeight.w700 : FontWeight.w500,
                              color: sunday ? scheme.error : scheme.onSurface,
                            ),
                          ),
                        ),
                        const SizedBox(height: 2),
                        SizedBox(
                          height: 10,
                          child: Column(
                            children: events.take(2).map((event) {
                              return Container(
                                width: double.infinity,
                                height: 3,
                                margin: const EdgeInsets.symmetric(horizontal: 2, vertical: 1),
                                decoration: BoxDecoration(
                                  color: _eventColor(context, event),
                                  borderRadius: BorderRadius.circular(2),
                                ),
                              );
                            }).toList(),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              );
            }).toList(),
          ),
        ),
      ],
    );
  }
}

class _WeekdayRow extends StatelessWidget {
  const _WeekdayRow();

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    const labels = ['T.2', 'T.3', 'T.4', 'T.5', 'T.6', 'T.7', 'CN'];
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 8),
      child: Row(
        children: List.generate(7, (index) {
          return Expanded(
            child: SizedBox(
              height: 26,
              child: Center(
                child: Text(
                  labels[index],
                  style: TextStyle(
                    fontSize: 11,
                    fontWeight: FontWeight.w600,
                    color: index == 6 ? scheme.error : scheme.onSurfaceVariant,
                  ),
                ),
              ),
            ),
          );
        }),
      ),
    );
  }
}

class _CalendarToggle extends StatelessWidget {
  const _CalendarToggle({required this.expanded, required this.onTap});

  final bool expanded;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 22,
      child: IconButton(
        padding: EdgeInsets.zero,
        constraints: const BoxConstraints(minWidth: 44, minHeight: 22),
        onPressed: onTap,
        icon: Icon(
          expanded ? Icons.keyboard_arrow_up_rounded : Icons.keyboard_arrow_down_rounded,
          size: 20,
        ),
      ),
    );
  }
}

class _Agenda extends StatelessWidget {
  const _Agenda({
    required this.date,
    required this.header,
    required this.events,
    required this.countdownPolicy,
    required this.onEventTap,
    required this.onEmptyTap,
  });

  final DateTime date;
  final String header;
  final List<NextAEvent> events;
  final CountdownPolicy countdownPolicy;
  final ValueChanged<NextAEvent> onEventTap;
  final VoidCallback onEmptyTap;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 0, 16, 90),
      children: [
        SizedBox(
          height: 46,
          child: Row(
            children: [
              Text(
                header,
                style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600),
              ),
              const Spacer(),
              Icon(Icons.more_horiz_rounded, color: scheme.onSurfaceVariant),
            ],
          ),
        ),
        if (events.isEmpty)
          InkWell(
            onTap: onEmptyTap,
            child: Padding(
              padding: const EdgeInsets.only(top: 28),
              child: Column(
                children: [
                  Icon(Icons.event_available_outlined, size: 34, color: scheme.onSurfaceVariant),
                  const SizedBox(height: 10),
                  Text('Không có sự kiện', style: TextStyle(color: scheme.onSurfaceVariant)),
                ],
              ),
            ),
          )
        else
          ...events.map((event) => _AgendaEventRow(
                event: event,
                countdown: countdownPolicy.remaining(event, DateTime.now()),
                countdownPolicy: countdownPolicy,
                onTap: () => onEventTap(event),
              )),
      ],
    );
  }
}

class _AgendaEventRow extends StatelessWidget {
  const _AgendaEventRow({
    required this.event,
    required this.countdown,
    required this.countdownPolicy,
    required this.onTap,
  });

  final NextAEvent event;
  final Duration? countdown;
  final CountdownPolicy countdownPolicy;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final marker = _eventColor(context, event);
    final time = '${DateFormat.Hm().format(event.start)} – ${DateFormat.Hm().format(event.end)}';
    return InkWell(
      onTap: onTap,
      child: Container(
        constraints: const BoxConstraints(minHeight: 82),
        decoration: BoxDecoration(
          border: Border(bottom: BorderSide(color: scheme.outlineVariant.withValues(alpha: .65))),
        ),
        padding: const EdgeInsets.symmetric(vertical: 10),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            SizedBox(
              width: 54,
              child: Padding(
                padding: const EdgeInsets.only(top: 2),
                child: Text(
                  DateFormat.Hm().format(event.start),
                  style: TextStyle(fontSize: 12, color: scheme.onSurfaceVariant),
                ),
              ),
            ),
            Container(
              width: 3,
              height: 56,
              margin: const EdgeInsets.only(right: 12),
              decoration: BoxDecoration(color: marker, borderRadius: BorderRadius.circular(3)),
            ),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    event.title,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
                  ),
                  const SizedBox(height: 3),
                  Text(
                    [time, if (event.location != null) event.location!].join(' · '),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(fontSize: 12, color: scheme.onSurfaceVariant),
                  ),
                  if (event.note != null && event.note!.trim().isNotEmpty) ...[
                    const SizedBox(height: 3),
                    Text(
                      event.note!,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(fontSize: 12, color: scheme.onSurfaceVariant),
                    ),
                  ],
                ],
              ),
            ),
            if (countdown != null)
              Padding(
                padding: const EdgeInsets.only(left: 10, top: 2),
                child: SizedBox(
                  width: 68,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.end,
                    children: [
                      Text(
                        'Bắt đầu sau',
                        textAlign: TextAlign.right,
                        style: TextStyle(
                          fontSize: 10,
                          color: scheme.onSurfaceVariant,
                          fontWeight: FontWeight.w500,
                        ),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        countdownPolicy.format(countdown!),
                        textAlign: TextAlign.right,
                        style: TextStyle(
                          fontSize: 12,
                          color: scheme.primary,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            if (event.priority > 0)
              Padding(
                padding: const EdgeInsets.only(left: 6, top: 3),
                child: Icon(Icons.star_rounded, size: 16, color: scheme.tertiary),
              ),
          ],
        ),
      ),
    );
  }
}

class _AddEventPill extends StatelessWidget {
  const _AddEventPill({required this.label, required this.onTap});

  final String label;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Material(
      elevation: 3,
      color: scheme.surface,
      borderRadius: BorderRadius.circular(24),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(24),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 14),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Expanded(
                child: Text(
                  label,
                  textAlign: TextAlign.center,
                  style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w600),
                ),
              ),
              const Icon(Icons.add_rounded, size: 22),
            ],
          ),
        ),
      ),
    );
  }
}

Color _eventColor(BuildContext context, NextAEvent event) {
  final scheme = Theme.of(context).colorScheme;
  return switch (event.type) {
    EventType.exam => scheme.error,
    EventType.assignment => scheme.tertiary,
    EventType.meeting => scheme.secondary,
    EventType.personal => scheme.primary,
    EventType.classEvent => scheme.primaryContainer,
    EventType.other => scheme.outline,
  };
}
