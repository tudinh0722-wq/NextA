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
    final now = DateTime.now();
    _selectedDate = DateTime(now.year, now.month, now.day);
    _visibleMonth = DateTime(now.year, now.month);
  }

  DateTime _monthShift(DateTime month, int delta) => DateTime(month.year, month.month + delta);

  void _changeMonth(int delta) {
    final target = _monthShift(_visibleMonth, delta);
    final maxDay = DateTime(target.year, target.month + 1, 0).day;
    setState(() {
      _visibleMonth = target;
      _selectedDate = DateTime(target.year, target.month, _selectedDate.day.clamp(1, maxDay));
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
    final firstCell = first.subtract(Duration(days: first.weekday - DateTime.monday));
    return List.generate(35, (i) => DateTime(firstCell.year, firstCell.month, firstCell.day + i));
  }

  List<DateTime> _weekDays() {
    final monday = _selectedDate.subtract(Duration(days: _selectedDate.weekday - DateTime.monday));
    return List.generate(7, (i) => DateTime(monday.year, monday.month, monday.day + i));
  }

  List<NextAEvent> _eventsFor(DateTime date) => widget.events.where((event) {
        final start = event.start;
        return start.year == date.year && start.month == date.month && start.day == date.day;
      }).toList()..sort((a, b) => a.start.compareTo(b.start));

  bool _sameDay(DateTime a, DateTime b) => a.year == b.year && a.month == b.month && a.day == b.day;

  String _selectedHeader(DateTime date) {
    const weekdays = ['T.2', 'T.3', 'T.4', 'T.5', 'T.6', 'T.7', 'CN'];
    return '${date.day}   ${weekdays[date.weekday - 1]}';
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: GestureDetector(
          onHorizontalDragEnd: (details) {
            final velocity = details.primaryVelocity ?? 0;
            if (velocity.abs() >= 200) _changeMonth(velocity < 0 ? 1 : -1);
          },
          onVerticalDragEnd: (details) {
            final velocity = details.primaryVelocity ?? 0;
            if (velocity.abs() < 250) return;
            if (velocity < 0 && !_calendarExpanded) {
              setState(() => _calendarExpanded = true);
            } else if (velocity > 0 && _calendarExpanded) {
              setState(() => _calendarExpanded = false);
            }
          },
          child: Column(
            children: [
              _TopBar(
                monthLabel: 'TH${_visibleMonth.month}',
                todayDay: DateTime.now().day,
                onToday: _goToday,
                onSearch: () => _showSearch(context),
                onMenu: () => ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Menu'))),
              ),
              AnimatedSize(
                duration: const Duration(milliseconds: 180),
                curve: Curves.easeOut,
                child: _calendarExpanded
                    ? _MonthCalendar(
                        days: _calendarDays(),
                        visibleMonth: _visibleMonth,
                        selectedDate: _selectedDate,
                        eventsFor: _eventsFor,
                        sameDay: _sameDay,
                        onSelect: (date) => setState(() {
                          _selectedDate = date;
                          _visibleMonth = DateTime(date.year, date.month);
                        }),
                        onLongPress: (date) => _createEvent(context, date),
                      )
                    : _WeekCalendar(
                        days: _weekDays(),
                        selectedDate: _selectedDate,
                        eventsFor: _eventsFor,
                        sameDay: _sameDay,
                        onSelect: (date) => setState(() {
                          _selectedDate = date;
                          _visibleMonth = DateTime(date.year, date.month);
                        }),
                      ),
              ),
              Expanded(
                child: Stack(
                  children: [
                    _Agenda(
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
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Tạo sự kiện ngày ${date.day}/${date.month}')));
  }

  void _showEvent(BuildContext context, NextAEvent event) {
    final time = '${DateFormat.Hm().format(event.start)} – ${DateFormat.Hm().format(event.end)}';
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('${event.title} · $time')));
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
  const _TopBar({required this.monthLabel, required this.todayDay, required this.onToday, required this.onSearch, required this.onMenu});
  final String monthLabel;
  final int todayDay;
  final VoidCallback onToday;
  final VoidCallback onSearch;
  final VoidCallback onMenu;

  @override
  Widget build(BuildContext context) => SizedBox(
        height: 64,
        child: Row(
          children: [
            IconButton(onPressed: onMenu, icon: const Icon(Icons.menu_rounded)),
            const Spacer(),
            Text(monthLabel, style: const TextStyle(fontSize: 21, fontWeight: FontWeight.w700)),
            const Spacer(),
            IconButton(onPressed: onSearch, icon: const Icon(Icons.search_rounded)),
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
                        child: Text('$todayDay', style: const TextStyle(fontSize: 11, fontWeight: FontWeight.w700)),
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

class _MonthCalendar extends StatelessWidget {
  const _MonthCalendar({required this.days, required this.visibleMonth, required this.selectedDate, required this.eventsFor, required this.sameDay, required this.onSelect, required this.onLongPress});
  final List<DateTime> days;
  final DateTime visibleMonth;
  final DateTime selectedDate;
  final List<NextAEvent> Function(DateTime) eventsFor;
  final bool Function(DateTime, DateTime) sameDay;
  final ValueChanged<DateTime> onSelect;
  final ValueChanged<DateTime> onLongPress;

  @override
  Widget build(BuildContext context) => Column(
        children: [
          const _WeekdayRow(),
          GridView.builder(
            padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 2),
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            itemCount: days.length,
            gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(crossAxisCount: 7, mainAxisExtent: 54),
            itemBuilder: (context, index) {
              final date = days[index];
              return GestureDetector(
                onTap: () => onSelect(date),
                onLongPress: () => onLongPress(date),
                child: _CalendarDayTile(
                  date: date,
                  inMonth: date.month == visibleMonth.month && date.year == visibleMonth.year,
                  selected: sameDay(date, selectedDate),
                  sunday: date.weekday == DateTime.sunday,
                  events: eventsFor(date),
                ),
              );
            },
          ),
        ],
      );
}

class _CalendarDayTile extends StatelessWidget {
  const _CalendarDayTile({required this.date, required this.inMonth, required this.selected, required this.sunday, required this.events});
  final DateTime date;
  final bool inMonth;
  final bool selected;
  final bool sunday;
  final List<NextAEvent> events;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final surface = _calendarTileColor(context, events, inMonth);
    return AnimatedContainer(
      duration: const Duration(milliseconds: 120),
      margin: const EdgeInsets.all(1.5),
      padding: const EdgeInsets.fromLTRB(3, 4, 3, 3),
      decoration: BoxDecoration(
        color: surface,
        borderRadius: BorderRadius.circular(8),
        border: selected ? Border.all(color: scheme.onSurface, width: 1.2) : null,
      ),
      child: Column(
        children: [
          SizedBox(
            height: 23,
            child: Align(
              alignment: Alignment.topCenter,
              child: selected
                  ? Container(
                      width: 25,
                      height: 25,
                      alignment: Alignment.center,
                      decoration: BoxDecoration(shape: BoxShape.circle, border: Border.all(color: scheme.onSurface, width: 1.15)),
                      child: Text('${date.day}', style: _dateStyle(scheme, inMonth, sunday, true)),
                    )
                  : Text('${date.day}', style: _dateStyle(scheme, inMonth, sunday, false)),
            ),
          ),
          const Spacer(),
          ...events.take(3).map(
                (event) => Padding(
                  padding: const EdgeInsets.symmetric(vertical: 1),
                  child: Container(
                    height: 4,
                    decoration: BoxDecoration(color: _eventColor(context, event), borderRadius: BorderRadius.circular(3)),
                  ),
                ),
              ),
        ],
      ),
    );
  }

  TextStyle _dateStyle(ColorScheme scheme, bool inMonth, bool sunday, bool selected) => TextStyle(
        fontSize: selected ? 12 : 13,
        height: 1.05,
        fontWeight: selected ? FontWeight.w700 : FontWeight.w600,
        color: !inMonth ? scheme.onSurface.withValues(alpha: .42) : sunday ? scheme.error : scheme.onSurface,
      );
}

class _WeekCalendar extends StatelessWidget {
  const _WeekCalendar({required this.days, required this.selectedDate, required this.eventsFor, required this.sameDay, required this.onSelect});
  final List<DateTime> days;
  final DateTime selectedDate;
  final List<NextAEvent> Function(DateTime) eventsFor;
  final bool Function(DateTime, DateTime) sameDay;
  final ValueChanged<DateTime> onSelect;

  @override
  Widget build(BuildContext context) => Column(
        children: [
          const _WeekdayRow(),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 2),
            child: Row(
              children: days.map((date) => Expanded(
                    child: GestureDetector(
                      onTap: () => onSelect(date),
                      child: _CalendarDayTile(
                        date: date,
                        inMonth: true,
                        selected: sameDay(date, selectedDate),
                        sunday: date.weekday == DateTime.sunday,
                        events: eventsFor(date),
                      ),
                    ),
                  )).toList(),
            ),
          ),
        ],
      );
}

class _WeekdayRow extends StatelessWidget {
  const _WeekdayRow();
  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    const labels = ['T.2', 'T.3', 'T.4', 'T.5', 'T.6', 'T.7', 'CN'];
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 7),
      child: Row(
        children: List.generate(7, (i) => Expanded(
              child: SizedBox(
                height: 25,
                child: Center(
                  child: Text(labels[i], style: TextStyle(fontSize: 11, fontWeight: FontWeight.w600, color: i == 6 ? scheme.error : scheme.onSurface)),
                ),
              ),
            )),
      ),
    );
  }
}

class _Agenda extends StatelessWidget {
  const _Agenda({required this.header, required this.events, required this.countdownPolicy, required this.onEventTap, required this.onEmptyTap});
  final String header;
  final List<NextAEvent> events;
  final CountdownPolicy countdownPolicy;
  final ValueChanged<NextAEvent> onEventTap;
  final VoidCallback onEmptyTap;

  @override
  Widget build(BuildContext context) => ListView(
        padding: const EdgeInsets.fromLTRB(16, 0, 16, 94),
        children: [
          SizedBox(
            height: 42,
            child: Row(
              children: [
                Text(header, style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w700)),
                const Spacer(),
                const Icon(Icons.more_horiz_rounded, size: 22),
              ],
            ),
          ),
          if (events.isEmpty)
            InkWell(
              onTap: onEmptyTap,
              child: const Padding(
                padding: EdgeInsets.only(top: 28),
                child: Column(children: [Icon(Icons.event_available_outlined, size: 34), SizedBox(height: 10), Text('Không có sự kiện')]),
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

class _AgendaEventRow extends StatelessWidget {
  const _AgendaEventRow({required this.event, required this.countdown, required this.countdownPolicy, required this.onTap});
  final NextAEvent event;
  final Duration? countdown;
  final CountdownPolicy countdownPolicy;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final marker = _eventColor(context, event);
    final time = '${DateFormat.Hm().format(event.start)} – ${DateFormat.Hm().format(event.end)}';
    final isOngoing = !DateTime.now().isBefore(event.start) && DateTime.now().isBefore(event.end);

    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 9),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(width: 3, height: 58, margin: const EdgeInsets.only(top: 3, right: 12), decoration: BoxDecoration(color: marker, borderRadius: BorderRadius.circular(3))),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(event.title, maxLines: 2, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w700)),
                  if (event.location != null && event.location!.trim().isNotEmpty) ...[
                    const SizedBox(height: 3),
                    Text(event.location!, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w500)),
                  ],
                  if (event.note != null && event.note!.trim().isNotEmpty) ...[
                    const SizedBox(height: 3),
                    Text(event.note!, maxLines: 2, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w400)),
                  ],
                ],
              ),
            ),
            const SizedBox(width: 12),
            SizedBox(
              width: 102,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  Text(time, maxLines: 1, softWrap: false, style: const TextStyle(fontSize: 11, fontWeight: FontWeight.w700)),
                  if (countdown != null) ...[
                    const SizedBox(height: 6),
                    Text(isOngoing ? 'Kết thúc sau' : 'Bắt đầu sau', style: TextStyle(fontSize: 11, fontWeight: FontWeight.w600, color: scheme.primary)),
                    const SizedBox(height: 1),
                    Text(countdownPolicy.format(countdown!), style: TextStyle(fontSize: 16, fontWeight: FontWeight.w800, color: scheme.primary)),
                  ],
                ],
              ),
            ),
            if (event.priority > 0) ...[
              const SizedBox(width: 5),
              Padding(padding: const EdgeInsets.only(top: 3), child: Icon(Icons.star_rounded, size: 15, color: scheme.tertiary)),
            ],
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
      shadowColor: scheme.shadow.withValues(alpha: .16),
      borderRadius: BorderRadius.circular(26),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(26),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 16),
          child: Row(
            mainAxisSize: MainAxisSize.max,
            children: [
              Expanded(child: Text(label, textAlign: TextAlign.center, style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w700))),
              const Icon(Icons.add_rounded, size: 23),
            ],
          ),
        ),
      ),
    );
  }
}

Color _calendarTileColor(BuildContext context, List<NextAEvent> events, bool inMonth) {
  final scheme = Theme.of(context).colorScheme;
  if (!inMonth || events.isEmpty) return scheme.surfaceContainerLow.withValues(alpha: inMonth ? .6 : .28);
  var color = scheme.surface;
  for (final event in events.take(3)) {
    color = Color.lerp(color, _eventColor(context, event), .075) ?? color;
  }
  return color;
}

Color _eventColor(BuildContext context, NextAEvent event) {
  final scheme = Theme.of(context).colorScheme;
  return switch (event.type) {
    EventType.exam => scheme.error,
    EventType.assignment => scheme.tertiary,
    EventType.meeting => scheme.secondary,
    EventType.personal => scheme.primary,
    EventType.classEvent => scheme.primary,
    EventType.other => scheme.outline,
  };
}
