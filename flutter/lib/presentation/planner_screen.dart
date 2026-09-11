import 'package:flutter/material.dart';

import '../application/countdown_policy.dart';
import '../domain/event.dart';
import 'widgets/event_editor_sheet.dart';
import 'widgets/planner_agenda.dart';
import 'widgets/planner_calendar.dart';

class PlannerScreen extends StatefulWidget {
  const PlannerScreen({
    super.key,
    required this.events,
    this.themeMode = ThemeMode.system,
    this.seedColor = const Color(0xFF1A73E8),
    this.onThemeChanged,
    this.onSeedColorChanged,
  });

  final List<NextAEvent> events;
  final ThemeMode themeMode;
  final Color seedColor;
  final ValueChanged<ThemeMode>? onThemeChanged;
  final ValueChanged<Color>? onSeedColorChanged;

  @override
  State<PlannerScreen> createState() => _PlannerScreenState();
}

class _PlannerScreenState extends State<PlannerScreen> {
  late DateTime _selected;
  late DateTime _month;
  late List<NextAEvent> _events;
  bool _expanded = true;
  final _countdownPolicy = const CountdownPolicy();

  static const _pageAnimationDuration = Duration(milliseconds: 420);

  @override
  void initState() {
    super.initState();
    final now = DateTime.now();
    _selected = DateTime(now.year, now.month, now.day);
    _month = DateTime(now.year, now.month);
    _events = List.of(widget.events);
  }

  List<NextAEvent> _eventsFor(DateTime day) {
    final result = _events.where((event) => event.start.year == day.year && event.start.month == day.month && event.start.day == day.day).toList();
    result.sort((a, b) => a.start.compareTo(b.start));
    return result;
  }

  void _selectDay(DateTime day) {
    setState(() {
      _selected = DateTime(day.year, day.month, day.day);
      _month = DateTime(day.year, day.month);
    });
  }

  void _shiftDay(int delta) => _selectDay(_selected.add(Duration(days: delta)));

  void _shiftMonth(int delta) {
    final target = DateTime(_month.year, _month.month + delta);
    final maxDay = DateTime(target.year, target.month + 1, 0).day;
    setState(() {
      _month = target;
      _selected = DateTime(target.year, target.month, _selected.day.clamp(1, maxDay));
    });
  }

  void _shiftWeek(int delta) => _selectDay(_selected.add(Duration(days: 7 * delta)));

  void _handleVerticalSwipe(DragEndDetails details) {
    final velocity = details.primaryVelocity ?? 0;
    if (velocity.abs() < 220) return;
    if (velocity < 0 && _expanded) {
      setState(() => _expanded = false);
    } else if (velocity > 0 && !_expanded) {
      setState(() => _expanded = true);
    }
  }

  Future<void> _editEvent(NextAEvent? event) async {
    final result = await showEventEditor(context, event: event, selectedDay: _selected);
    if (!mounted || result == null) return;
    setState(() {
      if (result.deleted && event != null) {
        _events.removeWhere((item) => item.id == event.id);
        return;
      }
      final updated = result.event;
      if (updated == null) return;
      if (event == null) {
        _events.add(updated);
      } else {
        final index = _events.indexWhere((item) => item.id == event.id);
        if (index >= 0) _events[index] = updated;
      }
    });
  }

  String _selectedHeader() {
    const weekdays = ['T.2', 'T.3', 'T.4', 'T.5', 'T.6', 'T.7', 'CN'];
    return '${_selected.day}   ${weekdays[_selected.weekday - 1]}';
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Column(
          children: [
            PlannerTopBar(
              monthLabel: 'TH${_month.month}',
              today: DateTime.now().day,
              onMenu: () => _showThemeMenu(context),
              onSearch: () => _showSearch(context),
              onToday: () => _selectDay(DateTime.now()),
            ),
            Expanded(
              child: GestureDetector(
                behavior: HitTestBehavior.translucent,
                onVerticalDragEnd: _handleVerticalSwipe,
                child: Column(
                  children: [
                    GestureDetector(
                      behavior: HitTestBehavior.opaque,
                      onHorizontalDragEnd: (details) {
                        final velocity = details.primaryVelocity ?? 0;
                        if (velocity.abs() > 200) {
                          if (_expanded) {
                            _shiftMonth(velocity < 0 ? 1 : -1);
                          } else {
                            _shiftWeek(velocity < 0 ? 1 : -1);
                          }
                        }
                      },
                      child: PlannerCalendar(
                        month: _month,
                        selected: _selected,
                        expanded: _expanded,
                        eventsFor: _eventsFor,
                        onSelect: _selectDay,
                        animationDuration: _pageAnimationDuration,
                      ),
                    ),
                    Expanded(
                      child: GestureDetector(
                        behavior: HitTestBehavior.opaque,
                        onHorizontalDragEnd: (details) {
                          final velocity = details.primaryVelocity ?? 0;
                          if (velocity.abs() > 200) {
                            _shiftDay(velocity < 0 ? 1 : -1);
                          }
                        },
                        child: Stack(
                          children: [
                            PlannerAgenda(
                              header: _selectedHeader(),
                              events: _eventsFor(_selected),
                              policy: _countdownPolicy,
                              onEventTap: _editEvent,
                              onEmptyTap: () => _editEvent(null),
                            ),
                            Positioned(
                              left: 28,
                              right: 28,
                              bottom: 14,
                              child: PlannerFab(
                                label: 'Thêm vào ${_selected.day} Th${_selected.month}',
                                onTap: () => _editEvent(null),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _showSearch(BuildContext context) async {
    await showDialog<void>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Tìm kiếm'),
        content: TextField(
          autofocus: true,
          onSubmitted: (_) => Navigator.pop(dialogContext),
          decoration: const InputDecoration(hintText: 'Tên sự kiện'),
        ),
      ),
    );
  }

  Future<void> _showThemeMenu(BuildContext context) async {
    await showModalBottomSheet<void>(
      context: context,
      showDragHandle: true,
      builder: (sheetContext) => SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(16, 4, 16, 20),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text('Giao diện', style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700)),
              const SizedBox(height: 8),
              SegmentedButton<ThemeMode>(
                segments: const [
                  ButtonSegment(value: ThemeMode.system, label: Text('Hệ thống')),
                  ButtonSegment(value: ThemeMode.light, label: Text('Sáng')),
                  ButtonSegment(value: ThemeMode.dark, label: Text('Tối')),
                ],
                selected: {widget.themeMode},
                onSelectionChanged: (selection) => widget.onThemeChanged?.call(selection.first),
              ),
              const SizedBox(height: 18),
              const Text('Màu chủ đề', style: TextStyle(fontWeight: FontWeight.w600)),
              const SizedBox(height: 10),
              Wrap(
                spacing: 10,
                children: const [
                  Color(0xFF1A73E8), Color(0xFF6750A4), Color(0xFF006A6A), Color(0xFF8E4A2F), Color(0xFF7A4E00),
                ].map((color) => _SeedColorButton(color: color, onSelected: widget.onSeedColorChanged)).toList(),
              ),
              const SizedBox(height: 10),
              const Text('Trên Android có màu động, NextA ưu tiên màu hệ thống theo Material 3.'),
            ],
          ),
        ),
      ),
    );
  }
}

class _SeedColorButton extends StatelessWidget {
  const _SeedColorButton({required this.color, required this.onSelected});
  final Color color;
  final ValueChanged<Color>? onSelected;
  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () {
        onSelected?.call(color);
        Navigator.pop(context);
      },
      child: CircleAvatar(radius: 17, backgroundColor: color),
    );
  }
}

class PlannerTopBar extends StatelessWidget {
  const PlannerTopBar({super.key, required this.monthLabel, required this.today, required this.onMenu, required this.onSearch, required this.onToday});
  final String monthLabel;
  final int today;
  final VoidCallback onMenu;
  final VoidCallback onSearch;
  final VoidCallback onToday;
  @override
  Widget build(BuildContext context) {
    return SizedBox(
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
              borderRadius: BorderRadius.circular(20),
              child: SizedBox(
                width: 38,
                height: 38,
                child: Stack(
                  alignment: Alignment.center,
                  children: [
                    const Icon(Icons.calendar_today_outlined, size: 27),
                    Padding(
                      padding: const EdgeInsets.only(top: 5),
                      child: Text('$today', style: const TextStyle(fontSize: 10, fontWeight: FontWeight.w800)),
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

class PlannerFab extends StatelessWidget {
  const PlannerFab({super.key, required this.label, required this.onTap});
  final String label;
  final VoidCallback onTap;
  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Material(
      elevation: 2,
      shadowColor: scheme.shadow.withValues(alpha: 0.18),
      color: scheme.surfaceContainerHighest,
      shape: const StadiumBorder(),
      child: InkWell(
        onTap: onTap,
        customBorder: const StadiumBorder(),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(Icons.add_rounded, size: 20),
              const SizedBox(width: 8),
              Text(label, style: const TextStyle(fontWeight: FontWeight.w700)),
            ],
          ),
        ),
      ),
    );
  }
}
