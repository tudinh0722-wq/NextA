import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../application/recurrence_policy.dart';
import '../../domain/event.dart';

class EventEditorResult {
  const EventEditorResult({required this.events, required this.deleted});

  final List<NextAEvent> events;
  final bool deleted;

  NextAEvent? get event => events.isEmpty ? null : events.first;
}

Future<EventEditorResult?> showEventEditor(
  BuildContext context, {
  NextAEvent? event,
  required DateTime selectedDay,
}) async {
  if (event != null) {
    final action = await showModalBottomSheet<String>(
      context: context,
      showDragHandle: true,
      builder: (sheetContext) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            ListTile(
              leading: const Icon(Icons.edit_outlined),
              title: const Text('Chỉnh sửa sự kiện'),
              onTap: () => Navigator.pop(sheetContext, 'edit'),
            ),
            ListTile(
              leading: const Icon(Icons.delete_outline),
              title: const Text('Xóa sự kiện'),
              onTap: () => Navigator.pop(sheetContext, 'delete'),
            ),
            const SizedBox(height: 8),
          ],
        ),
      ),
    );

    if (!context.mounted) return null;

    if (action == 'delete') {
      final confirmed = await showDialog<bool>(
        context: context,
        builder: (dialogContext) => AlertDialog(
          title: const Text('Xóa sự kiện?'),
          content: Text('Xóa “${event.title}” khỏi lịch?'),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(dialogContext, false),
              child: const Text('Hủy'),
            ),
            FilledButton(
              onPressed: () => Navigator.pop(dialogContext, true),
              child: const Text('Xóa'),
            ),
          ],
        ),
      );

      if (!context.mounted) return null;

      return confirmed == true
          ? const EventEditorResult(events: [], deleted: true)
          : null;
    }

    if (action != 'edit') return null;
  }

  if (!context.mounted) return null;

  return showModalBottomSheet<EventEditorResult>(
    context: context,
    isScrollControlled: true,
    backgroundColor: Colors.transparent,
    useSafeArea: false,
    builder: (sheetContext) => _EventEditorSheet(
      event: event,
      selectedDay: selectedDay,
    ),
  );
}

class _EventEditorSheet extends StatefulWidget {
  const _EventEditorSheet({required this.event, required this.selectedDay});

  final NextAEvent? event;
  final DateTime selectedDay;

  @override
  State<_EventEditorSheet> createState() => _EventEditorSheetState();
}

class _EventEditorSheetState extends State<_EventEditorSheet> {
  late final TextEditingController _title;
  late final TextEditingController _location;
  late final TextEditingController _note;
  late EventType _type;
  late int _priority;
  late DateTime _start;
  late DateTime _end;
  late RecurrenceRule _recurrenceRule;
  int _reminderMinutes = 10;
  bool _bulkImportTab = false;

  @override
  void initState() {
    super.initState();
    final event = widget.event;
    _title = TextEditingController(text: event?.title ?? '');
    _location = TextEditingController(text: event?.location ?? '');
    _note = TextEditingController(text: event?.note ?? '');
    _type = event?.type ?? EventType.classEvent;
    _priority = event?.priority ?? 0;
    _start = event?.start ?? DateTime(
      widget.selectedDay.year,
      widget.selectedDay.month,
      widget.selectedDay.day,
      8,
    );
    _end = event?.end ?? _start.add(const Duration(hours: 1));
    _recurrenceRule = event?.recurrenceRule ?? const RecurrenceRule(
      frequency: RecurrenceFrequency.none,
    );
  }

  @override
  void dispose() {
    _title.dispose();
    _location.dispose();
    _note.dispose();
    super.dispose();
  }

  Future<void> _pickDate({required bool start}) async {
    final initial = start ? _start : _end;
    final date = await showDatePicker(
      context: context,
      initialDate: initial,
      firstDate: DateTime(2020),
      lastDate: DateTime(2100),
      locale: const Locale('vi'),
    );
    if (date == null) return;

    setState(() {
      if (start) {
        _start = DateTime(
          date.year,
          date.month,
          date.day,
          _start.hour,
          _start.minute,
        );
        if (!_end.isAfter(_start)) {
          _end = _start.add(const Duration(hours: 1));
        }
      } else {
        _end = DateTime(
          date.year,
          date.month,
          date.day,
          _end.hour,
          _end.minute,
        );
      }
    });
  }

  Future<void> _pickTime({required bool start}) async {
    final initial = TimeOfDay.fromDateTime(start ? _start : _end);
    final time = await showTimePicker(
      context: context,
      initialTime: initial,
    );
    if (time == null) return;

    setState(() {
      if (start) {
        _start = DateTime(
          _start.year,
          _start.month,
          _start.day,
          time.hour,
          time.minute,
        );
        if (!_end.isAfter(_start)) {
          _end = _start.add(const Duration(hours: 1));
        }
      } else {
        _end = DateTime(
          _end.year,
          _end.month,
          _end.day,
          time.hour,
          time.minute,
        );
      }
    });
  }

  Future<void> _pickReminder() async {
    final selected = await showModalBottomSheet<int?>(
      context: context,
      showDragHandle: true,
      builder: (sheetContext) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            for (final value in [0, 5, 10, 15, 30, 60])
              ListTile(
                leading: Icon(
                  value == 0 ? Icons.notifications_off_outlined : Icons.notifications_none_outlined,
                ),
                title: Text(value == 0 ? 'Không báo trước' : 'Trước $value phút'),
                trailing: value == _reminderMinutes ? const Icon(Icons.check_rounded) : null,
                onTap: () => Navigator.pop(sheetContext, value),
              ),
            const SizedBox(height: 8),
          ],
        ),
      ),
    );
    if (selected != null) setState(() => _reminderMinutes = selected);
  }

  Future<void> _pickRecurrence() async {
    final selected = await showModalBottomSheet<RecurrenceFrequency>(
      context: context,
      showDragHandle: true,
      builder: (sheetContext) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            for (final frequency in RecurrenceFrequency.values)
              ListTile(
                leading: Icon(_recurrenceIcon(frequency)),
                title: Text(_recurrenceName(frequency)),
                trailing: frequency == _recurrenceRule.frequency
                    ? const Icon(Icons.check_rounded)
                    : null,
                onTap: () => Navigator.pop(sheetContext, frequency),
              ),
            const SizedBox(height: 8),
          ],
        ),
      ),
    );
    if (selected == null) return;
    setState(() {
      _recurrenceRule = RecurrenceRule(frequency: selected);
    });
  }

  Future<void> _pickPriority() async {
    final selected = await showModalBottomSheet<int>(
      context: context,
      showDragHandle: true,
      builder: (sheetContext) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            _PriorityChoice(priority: 0, selected: _priority, onTap: () => Navigator.pop(sheetContext, 0)),
            _PriorityChoice(priority: 1, selected: _priority, onTap: () => Navigator.pop(sheetContext, 1)),
            _PriorityChoice(priority: 2, selected: _priority, onTap: () => Navigator.pop(sheetContext, 2)),
            const SizedBox(height: 8),
          ],
        ),
      ),
    );
    if (selected != null) setState(() => _priority = selected);
  }

  void _save() {
    final title = _title.text.trim();
    if (title.isEmpty) return;
    if (!_end.isAfter(_start)) {
      setState(() => _end = _start.add(const Duration(hours: 1)));
    }

    final old = widget.event;
    final id = old?.id ?? DateTime.now().microsecondsSinceEpoch.toString();
    final recurring = _recurrenceRule.frequency != RecurrenceFrequency.none;
    final event = NextAEvent(
      id: id,
      title: title,
      type: _type,
      start: _start,
      end: _end,
      location: _location.text.trim().isEmpty ? null : _location.text.trim(),
      note: _note.text.trim().isEmpty ? null : _note.text.trim(),
      priority: _priority,
      recurrenceId: recurring ? (old?.recurrenceId ?? id) : old?.recurrenceId,
      recurrenceRule: recurring ? _recurrenceRule : null,
    );

    final occurrences = old == null && recurring
        ? generateOccurrences(event, rule: _recurrenceRule)
        : [event];

    Navigator.pop(
      context,
      EventEditorResult(events: occurrences, deleted: false),
    );
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final dateFormat = DateFormat('EEE, dd/MM', 'vi');
    final timeFormat = DateFormat('hh:mm a');

    return Material(
      color: scheme.surfaceContainerLowest,
      child: SafeArea(
        top: true,
        bottom: false,
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(24, 8, 24, 34),
              child: _EditorTabs(
                bulkImportSelected: _bulkImportTab,
                onChanged: (bulk) {
                  setState(() => _bulkImportTab = bulk);
                },
              ),
            ),
            Expanded(
              child: _bulkImportTab
                  ? const _BulkImportSlot()
                  : Container(
                      width: double.infinity,
                      decoration: BoxDecoration(
                        color: scheme.surface,
                        borderRadius: const BorderRadius.vertical(top: Radius.circular(42)),
                      ),
                      child: Column(
                        children: [
                          Expanded(
                            child: SingleChildScrollView(
                              padding: const EdgeInsets.fromLTRB(28, 26, 28, 120),
                              child: Column(
                                children: [
                                  _TitleRow(
                                    controller: _title,
                                    priority: _priority,
                                    onPriorityTap: _pickPriority,
                                  ),
                                  const _EditorDivider(),
                                  _EditorSwitchRow(
                                    icon: Icons.schedule_outlined,
                                    title: 'Cả ngày',
                                    value: false,
                                    onChanged: (_) {},
                                    enabled: false,
                                  ),
                                  const SizedBox(height: 18),
                                  _DateTimeRange(
                                    start: _start,
                                    end: _end,
                                    dateFormat: dateFormat,
                                    timeFormat: timeFormat,
                                    onStartDate: () => _pickDate(start: true),
                                    onStartTime: () => _pickTime(start: true),
                                    onEndDate: () => _pickDate(start: false),
                                    onEndTime: () => _pickTime(start: false),
                                  ),
                                  const _EditorDivider(),
                                  _EditorInputRow(
                                    icon: Icons.location_on_outlined,
                                    controller: _location,
                                    hint: 'Vị trí',
                                  ),
                                  const _EditorDivider(),
                                  _EditorActionRow(
                                    icon: Icons.notifications_none_outlined,
                                    title: _reminderMinutes == 0 ? 'Không báo trước' : 'Trước $_reminderMinutes phút',
                                    onTap: _pickReminder,
                                  ),
                                  const _EditorDivider(),
                                  _EditorActionRow(
                                    icon: Icons.repeat_rounded,
                                    title: _recurrenceName(_recurrenceRule.frequency),
                                    onTap: _pickRecurrence,
                                    muted: _recurrenceRule.frequency == RecurrenceFrequency.none,
                                  ),
                                  const _EditorDivider(),
                                  _EditorInputRow(
                                    icon: Icons.notes_outlined,
                                    controller: _note,
                                    hint: 'Ghi chú',
                                    maxLines: 3,
                                  ),
                                  const _EditorDivider(),
                                  _EditorActionRow(
                                    icon: Icons.category_outlined,
                                    title: eventTypeName(_type),
                                    onTap: () async {
                                      final selected = await showModalBottomSheet<EventType>(
                                        context: context,
                                        showDragHandle: true,
                                        builder: (sheetContext) => SafeArea(
                                          child: Column(
                                            mainAxisSize: MainAxisSize.min,
                                            children: [
                                              for (final type in EventType.values)
                                                ListTile(
                                                  leading: const Icon(Icons.event_outlined),
                                                  title: Text(eventTypeName(type)),
                                                  trailing: type == _type ? const Icon(Icons.check_rounded) : null,
                                                  onTap: () => Navigator.pop(sheetContext, type),
                                                ),
                                            ],
                                          ),
                                        ),
                                      );
                                      if (selected != null) setState(() => _type = selected);
                                    },
                                  ),
                                ],
                              ),
                            ),
                          ),
                          _SaveBar(
                            onCancel: () => Navigator.pop(context),
                            onSave: _save,
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
}

class _EditorTabs extends StatelessWidget {
  const _EditorTabs({required this.bulkImportSelected, required this.onChanged});

  final bool bulkImportSelected;
  final ValueChanged<bool> onChanged;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Container(
      height: 64,
      decoration: BoxDecoration(
        color: scheme.surfaceContainerHighest,
        borderRadius: BorderRadius.circular(34),
      ),
      padding: const EdgeInsets.all(3),
      child: Row(
        children: [
          Expanded(child: _tab(context, 'Sự kiện', !bulkImportSelected, () => onChanged(false))),
          Expanded(child: _tab(context, 'Nhắc nhở', bulkImportSelected, () => onChanged(true))),
        ],
      ),
    );
  }

  Widget _tab(BuildContext context, String label, bool selected, VoidCallback onTap) {
    final scheme = Theme.of(context).colorScheme;
    return Material(
      color: selected ? scheme.onSurface.withValues(alpha: 0.60) : Colors.transparent,
      shape: const StadiumBorder(),
      child: InkWell(
        onTap: onTap,
        customBorder: const StadiumBorder(),
        child: Center(
          child: Text(
            label,
            style: TextStyle(
              fontSize: 18,
              fontWeight: selected ? FontWeight.w700 : FontWeight.w400,
              color: selected ? scheme.surface : scheme.onSurfaceVariant,
            ),
          ),
        ),
      ),
    );
  }
}

class _BulkImportSlot extends StatelessWidget {
  const _BulkImportSlot();

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Container(
      width: double.infinity,
      decoration: BoxDecoration(
        color: scheme.surface,
        borderRadius: const BorderRadius.vertical(top: Radius.circular(42)),
      ),
      alignment: Alignment.topCenter,
      padding: const EdgeInsets.fromLTRB(28, 42, 28, 28),
      child: Column(
        children: [
          Icon(Icons.auto_awesome_motion_outlined, size: 42, color: scheme.primary),
          const SizedBox(height: 14),
          Text(
            'Nhắc nhở',
            style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w700),
          ),
          const SizedBox(height: 8),
          Text(
            'Khu vực này dành cho tính năng bulk import lịch.',
            textAlign: TextAlign.center,
            style: TextStyle(color: scheme.onSurfaceVariant),
          ),
        ],
      ),
    );
  }
}

class _TitleRow extends StatelessWidget {
  const _TitleRow({required this.controller, required this.priority, required this.onPriorityTap});

  final TextEditingController controller;
  final int priority;
  final VoidCallback onPriorityTap;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Row(
      children: [
        Icon(Icons.emoji_emotions_outlined, size: 30, color: scheme.onSurfaceVariant),
        const SizedBox(width: 16),
        Expanded(
          child: TextField(
            controller: controller,
            autofocus: true,
            style: const TextStyle(fontSize: 30, fontWeight: FontWeight.w400),
            decoration: const InputDecoration(
              hintText: 'Tựa đề',
              hintStyle: TextStyle(fontSize: 30),
              border: InputBorder.none,
              isCollapsed: true,
            ),
          ),
        ),
        const SizedBox(width: 12),
        Semantics(
          button: true,
          label: 'Mức độ sự kiện',
          child: InkWell(
            onTap: onPriorityTap,
            borderRadius: BorderRadius.circular(22),
            child: Padding(
              padding: const EdgeInsets.all(8),
              child: Container(
                width: 30,
                height: 30,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: _priorityColor(context, priority),
                ),
              ),
            ),
          ),
        ),
      ],
    );
  }
}

class _PriorityChoice extends StatelessWidget {
  const _PriorityChoice({required this.priority, required this.selected, required this.onTap});

  final int priority;
  final int selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return ListTile(
      leading: Container(
        width: 22,
        height: 22,
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          color: _priorityColor(context, priority),
        ),
      ),
      title: Text(_priorityName(priority)),
      trailing: priority == selected ? const Icon(Icons.check_rounded) : null,
      onTap: onTap,
    );
  }
}

class _EditorDivider extends StatelessWidget {
  const _EditorDivider();

  @override
  Widget build(BuildContext context) => Divider(
        height: 1,
        color: Theme.of(context).colorScheme.outlineVariant.withValues(alpha: 0.65),
      );
}

class _EditorSwitchRow extends StatelessWidget {
  const _EditorSwitchRow({
    required this.icon,
    required this.title,
    required this.value,
    required this.onChanged,
    this.enabled = true,
  });

  final IconData icon;
  final String title;
  final bool value;
  final ValueChanged<bool> onChanged;
  final bool enabled;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Icon(icon, size: 30, color: Theme.of(context).colorScheme.onSurfaceVariant),
        const SizedBox(width: 20),
        Expanded(child: Text(title, style: const TextStyle(fontSize: 22))),
        Switch.adaptive(value: value, onChanged: enabled ? onChanged : null),
      ],
    );
  }
}

class _DateTimeRange extends StatelessWidget {
  const _DateTimeRange({
    required this.start,
    required this.end,
    required this.dateFormat,
    required this.timeFormat,
    required this.onStartDate,
    required this.onStartTime,
    required this.onEndDate,
    required this.onEndTime,
  });

  final DateTime start;
  final DateTime end;
  final DateFormat dateFormat;
  final DateFormat timeFormat;
  final VoidCallback onStartDate;
  final VoidCallback onStartTime;
  final VoidCallback onEndDate;
  final VoidCallback onEndTime;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: _DateTimeColumn(
            date: dateFormat.format(start),
            time: timeFormat.format(start),
            onDate: onStartDate,
            onTime: onStartTime,
          ),
        ),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 8),
          child: Icon(Icons.arrow_forward_rounded, size: 34, color: Theme.of(context).colorScheme.onSurfaceVariant),
        ),
        Expanded(
          child: _DateTimeColumn(
            date: dateFormat.format(end),
            time: timeFormat.format(end),
            onDate: onEndDate,
            onTime: onEndTime,
          ),
        ),
      ],
    );
  }
}

class _DateTimeColumn extends StatelessWidget {
  const _DateTimeColumn({required this.date, required this.time, required this.onDate, required this.onTime});

  final String date;
  final String time;
  final VoidCallback onDate;
  final VoidCallback onTime;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        InkWell(
          onTap: onDate,
          borderRadius: BorderRadius.circular(8),
          child: Padding(
            padding: const EdgeInsets.symmetric(vertical: 6),
            child: Text(date, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w600)),
          ),
        ),
        InkWell(
          onTap: onTime,
          borderRadius: BorderRadius.circular(8),
          child: Padding(
            padding: const EdgeInsets.symmetric(vertical: 6),
            child: Text(time, style: const TextStyle(fontSize: 20)),
          ),
        ),
      ],
    );
  }
}

class _EditorInputRow extends StatelessWidget {
  const _EditorInputRow({required this.icon, required this.controller, required this.hint, this.maxLines = 1});

  final IconData icon;
  final TextEditingController controller;
  final String hint;
  final int maxLines;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Row(
      crossAxisAlignment: maxLines > 1 ? CrossAxisAlignment.start : CrossAxisAlignment.center,
      children: [
        Padding(
          padding: EdgeInsets.only(top: maxLines > 1 ? 10 : 0),
          child: Icon(icon, size: 30, color: scheme.onSurface),
        ),
        const SizedBox(width: 20),
        Expanded(
          child: TextField(
            controller: controller,
            maxLines: maxLines,
            style: const TextStyle(fontSize: 20),
            decoration: InputDecoration(
              hintText: hint,
              hintStyle: TextStyle(color: scheme.onSurfaceVariant, fontSize: 20),
              border: InputBorder.none,
              contentPadding: const EdgeInsets.symmetric(vertical: 16),
            ),
          ),
        ),
      ],
    );
  }
}

class _EditorActionRow extends StatelessWidget {
  const _EditorActionRow({required this.icon, required this.title, required this.onTap, this.muted = false});

  final IconData icon;
  final String title;
  final VoidCallback onTap;
  final bool muted;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 16),
        child: Row(
          children: [
            Icon(icon, size: 30, color: muted ? scheme.onSurfaceVariant : scheme.onSurface),
            const SizedBox(width: 20),
            Expanded(
              child: Text(
                title,
                style: TextStyle(
                  fontSize: 20,
                  color: muted ? scheme.onSurfaceVariant : scheme.onSurface,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _SaveBar extends StatelessWidget {
  const _SaveBar({required this.onCancel, required this.onSave});

  final VoidCallback onCancel;
  final VoidCallback onSave;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Padding(
      padding: const EdgeInsets.fromLTRB(28, 8, 28, 18),
      child: Material(
        elevation: 2,
        shadowColor: scheme.shadow.withValues(alpha: 0.18),
        color: scheme.surfaceContainerLowest,
        shape: const StadiumBorder(),
        child: SizedBox(
          height: 62,
          child: Row(
            children: [
              Expanded(
                child: InkWell(
                  onTap: onCancel,
                  customBorder: const StadiumBorder(),
                  child: const Center(
                    child: Text('Thoát', style: TextStyle(fontSize: 19, fontWeight: FontWeight.w700)),
                  ),
                ),
              ),
              Container(
                width: 1,
                height: 28,
                color: scheme.outlineVariant,
              ),
              Expanded(
                child: InkWell(
                  onTap: onSave,
                  customBorder: const StadiumBorder(),
                  child: const Center(
                    child: Text('Lưu', style: TextStyle(fontSize: 19, fontWeight: FontWeight.w700)),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

Color _priorityColor(BuildContext context, int priority) {
  final scheme = Theme.of(context).colorScheme;
  return switch (priority) {
    2 => scheme.error,
    1 => scheme.tertiary,
    _ => scheme.primary,
  };
}

String _priorityName(int priority) => switch (priority) {
      2 => 'Rất quan trọng',
      1 => 'Quan trọng',
      _ => 'Bình thường',
    };

String _recurrenceName(RecurrenceFrequency frequency) => switch (frequency) {
      RecurrenceFrequency.none => 'Không lặp lại',
      RecurrenceFrequency.daily => 'Hàng ngày',
      RecurrenceFrequency.weekly => 'Hàng tuần',
      RecurrenceFrequency.weekdays => 'Thứ 2 – Thứ 6',
      RecurrenceFrequency.monthly => 'Hàng tháng',
    };

IconData _recurrenceIcon(RecurrenceFrequency frequency) => switch (frequency) {
      RecurrenceFrequency.none => Icons.repeat_rounded,
      RecurrenceFrequency.daily => Icons.today_outlined,
      RecurrenceFrequency.weekly => Icons.view_week_outlined,
      RecurrenceFrequency.weekdays => Icons.work_outline_rounded,
      RecurrenceFrequency.monthly => Icons.calendar_month_outlined,
    };

String eventTypeName(EventType type) => switch (type) {
      EventType.classEvent => 'Môn học',
      EventType.exam => 'Thi / kiểm tra',
      EventType.assignment => 'Bài tập',
      EventType.meeting => 'Cuộc họp',
      EventType.personal => 'Cá nhân',
      EventType.other => 'Khác',
    };
