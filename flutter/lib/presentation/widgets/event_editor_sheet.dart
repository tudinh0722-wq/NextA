import 'package:flutter/material.dart';

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
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          ListTile(leading: const Icon(Icons.edit_outlined), title: const Text('Chỉnh sửa sự kiện'), onTap: () => Navigator.pop(sheetContext, 'edit')),
          ListTile(leading: const Icon(Icons.delete_outline), title: const Text('Xóa sự kiện'), onTap: () => Navigator.pop(sheetContext, 'delete')),
          const SizedBox(height: 8),
        ]),
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
            TextButton(onPressed: () => Navigator.pop(dialogContext, false), child: const Text('Hủy')),
            FilledButton(onPressed: () => Navigator.pop(dialogContext, true), child: const Text('Xóa')),
          ],
        ),
      );
      if (!context.mounted) return null;
      return confirmed == true ? const EventEditorResult(events: [], deleted: true) : null;
    }
    if (action != 'edit') return null;
  }
  if (!context.mounted) return null;
  return showModalBottomSheet<EventEditorResult>(
    context: context,
    isScrollControlled: true,
    backgroundColor: Colors.transparent,
    useSafeArea: true,
    builder: (_) => _EventEditorSheet(event: event, selectedDay: selectedDay),
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
  late DateTime _start;
  late DateTime _end;
  late int _priority;
  late RecurrenceRule _recurrenceRule;
  int _reminderMinutes = 10;
  int _reminderRepeatCount = 2;
  int _reminderRepeatIntervalMinutes = 5;
  bool _bulkImportTab = false;

  @override
  void initState() {
    super.initState();
    final event = widget.event;
    _title = TextEditingController(text: event?.title ?? '');
    _location = TextEditingController(text: event?.location ?? '');
    _note = TextEditingController(text: event?.note ?? '');
    _start = event?.start ?? DateTime(widget.selectedDay.year, widget.selectedDay.month, widget.selectedDay.day, 8);
    _end = event?.end ?? _start.add(const Duration(hours: 1));
    _priority = event?.priority ?? 0;
    _recurrenceRule = event?.recurrenceRule ?? const RecurrenceRule(frequency: RecurrenceFrequency.none);
    _reminderMinutes = event?.reminderMinutes ?? 10;
    _reminderRepeatCount = event?.reminderRepeatCount ?? 2;
    _reminderRepeatIntervalMinutes = event?.reminderRepeatIntervalMinutes ?? 5;
  }

  @override
  void dispose() {
    _title.dispose();
    _location.dispose();
    _note.dispose();
    super.dispose();
  }

  Future<void> _pickDate(bool start) async {
    final current = start ? _start : _end;
    final date = await showDatePicker(context: context, initialDate: current, firstDate: DateTime(2020), lastDate: DateTime(2100));
    if (date == null) return;
    setState(() {
      final value = DateTime(date.year, date.month, date.day, current.hour, current.minute);
      if (start) {
        _start = value;
        if (!_end.isAfter(_start)) _end = _start.add(const Duration(hours: 1));
      } else {
        _end = value;
      }
    });
  }

  Future<void> _pickTime(bool start) async {
    final current = start ? _start : _end;
    final time = await showTimePicker(context: context, initialTime: TimeOfDay.fromDateTime(current));
    if (time == null) return;
    setState(() {
      final value = DateTime(current.year, current.month, current.day, time.hour, time.minute);
      if (start) {
        _start = value;
        if (!_end.isAfter(_start)) _end = _start.add(const Duration(hours: 1));
      } else {
        _end = value;
      }
    });
  }

  Future<void> _pickReminder() async {
    final result = await showDialog<_ReminderSettings>(
      context: context,
      builder: (_) => _ReminderDialog(minutes: _reminderMinutes, repeatCount: _reminderRepeatCount, repeatIntervalMinutes: _reminderRepeatIntervalMinutes),
    );
    if (result == null) return;
    setState(() {
      _reminderMinutes = result.minutes;
      _reminderRepeatCount = result.repeatCount;
      _reminderRepeatIntervalMinutes = result.repeatIntervalMinutes;
    });
  }

  Future<void> _pickRecurrence() async {
    final value = await showDialog<RecurrenceFrequency>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Lặp lại'),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              for (final frequency in RecurrenceFrequency.values)
                ListTile(
                  contentPadding: EdgeInsets.zero,
                  leading: Icon(_recurrenceIcon(frequency)),
                  title: Text(_recurrenceName(frequency)),
                  trailing: frequency == _recurrenceRule.frequency ? const Icon(Icons.check_rounded) : null,
                  onTap: () => Navigator.pop(dialogContext, frequency),
                ),
            ],
          ),
        ),
      ),
    );
    if (value != null) setState(() => _recurrenceRule = RecurrenceRule(frequency: value));
  }

  Future<void> _pickPriority() async {
    final value = await showModalBottomSheet<int>(
      context: context,
      showDragHandle: true,
      builder: (sheetContext) => SafeArea(
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          for (final priority in [0, 1, 2])
            ListTile(
              leading: _PriorityDot(priority: priority),
              title: Text(_priorityName(priority)),
              trailing: priority == _priority ? const Icon(Icons.check_rounded) : null,
              onTap: () => Navigator.pop(sheetContext, priority),
            ),
          const SizedBox(height: 8),
        ]),
      ),
    );
    if (value != null) setState(() => _priority = value);
  }

  void _save() {
    final title = _title.text.trim();
    if (title.isEmpty) return;
    if (!_end.isAfter(_start)) _end = _start.add(const Duration(hours: 1));
    final old = widget.event;
    final id = old?.id ?? DateTime.now().microsecondsSinceEpoch.toString();
    final recurring = _recurrenceRule.frequency != RecurrenceFrequency.none;
    final event = NextAEvent(
      id: id,
      title: title,
      type: old?.type ?? EventType.classEvent,
      start: _start,
      end: _end,
      location: _location.text.trim().isEmpty ? null : _location.text.trim(),
      note: _note.text.trim().isEmpty ? null : _note.text.trim(),
      priority: _priority,
      recurrenceId: recurring ? (old?.recurrenceId ?? id) : old?.recurrenceId,
      recurrenceRule: recurring ? _recurrenceRule : null,
      reminderMinutes: _reminderMinutes,
      reminderRepeatCount: _reminderRepeatCount,
      reminderRepeatIntervalMinutes: _reminderRepeatIntervalMinutes,
    );
    final events = old == null && recurring ? generateOccurrences(event, rule: _recurrenceRule) : [event];
    Navigator.pop(context, EventEditorResult(events: events, deleted: false));
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Material(
      color: scheme.surfaceContainerLowest,
      child: SafeArea(
        top: true,
        bottom: false,
        child: Column(children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(20, 14, 20, 14),
            child: _EditorTabs(selectedBulk: _bulkImportTab, onChanged: (value) => setState(() => _bulkImportTab = value)),
          ),
          Expanded(
            child: _bulkImportTab
                ? const _BulkImportSlot()
                : _EventContent(
                    title: _title,
                    location: _location,
                    note: _note,
                    start: _start,
                    end: _end,
                    priority: _priority,
                    reminderMinutes: _reminderMinutes,
                    reminderRepeatCount: _reminderRepeatCount,
                    reminderRepeatIntervalMinutes: _reminderRepeatIntervalMinutes,
                    recurrence: _recurrenceRule.frequency,
                    onPriority: _pickPriority,
                    onStartDate: () => _pickDate(true),
                    onStartTime: () => _pickTime(true),
                    onEndDate: () => _pickDate(false),
                    onEndTime: () => _pickTime(false),
                    onReminder: _pickReminder,
                    onRecurrence: _pickRecurrence,
                    onCancel: () => Navigator.pop(context),
                    onSave: _save,
                  ),
          ),
        ]),
      ),
    );
  }
}

class _ReminderSettings {
  const _ReminderSettings({required this.minutes, required this.repeatCount, required this.repeatIntervalMinutes});
  final int minutes;
  final int repeatCount;
  final int repeatIntervalMinutes;
}

class _ReminderDialog extends StatefulWidget {
  const _ReminderDialog({required this.minutes, required this.repeatCount, required this.repeatIntervalMinutes});
  final int minutes;
  final int repeatCount;
  final int repeatIntervalMinutes;
  @override
  State<_ReminderDialog> createState() => _ReminderDialogState();
}

class _ReminderDialogState extends State<_ReminderDialog> {
  late final TextEditingController _minutes;
  late final TextEditingController _repeatCount;
  late final TextEditingController _interval;

  @override
  void initState() {
    super.initState();
    _minutes = TextEditingController(text: widget.minutes.toString());
    _repeatCount = TextEditingController(text: widget.repeatCount.toString());
    _interval = TextEditingController(text: widget.repeatIntervalMinutes.toString());
  }

  @override
  void dispose() {
    _minutes.dispose();
    _repeatCount.dispose();
    _interval.dispose();
    super.dispose();
  }

  int _value(TextEditingController controller, {int fallback = 0, int min = 0, int max = 9999}) {
    final parsed = int.tryParse(controller.text.trim()) ?? fallback;
    return parsed.clamp(min, max);
  }

  void _save() {
    Navigator.pop(
      context,
      _ReminderSettings(
        minutes: _value(_minutes, min: 0, max: 10080),
        repeatCount: _value(_repeatCount, fallback: 2, min: 0, max: 20),
        repeatIntervalMinutes: _value(_interval, fallback: 5, min: 1, max: 1440),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return AlertDialog(
      scrollable: true,
      title: const Text('Báo trước'),
      content: Column(mainAxisSize: MainAxisSize.min, children: [
        _NumberField(controller: _minutes, label: 'Báo trước (phút)', autofocus: true),
        const SizedBox(height: 14),
        _NumberField(controller: _repeatCount, label: 'Lặp lại nếu chưa xác nhận (lần)'),
        const SizedBox(height: 14),
        _NumberField(controller: _interval, label: 'Khoảng cách giữa 2 lần (phút)'),
        const SizedBox(height: 8),
        Align(alignment: Alignment.centerLeft, child: Text('Mặc định: báo trước 10 phút, lặp thêm 2 lần, mỗi lần cách 5 phút.', style: TextStyle(fontSize: 12, color: scheme.onSurfaceVariant))),
      ]),
      actions: [
        TextButton(onPressed: () => Navigator.pop(context), child: const Text('Hủy')),
        FilledButton(onPressed: _save, child: const Text('Lưu')),
      ],
    );
  }
}

class _NumberField extends StatelessWidget {
  const _NumberField({required this.controller, required this.label, this.autofocus = false});
  final TextEditingController controller;
  final String label;
  final bool autofocus;
  @override
  Widget build(BuildContext context) => TextField(
        controller: controller,
        autofocus: autofocus,
        keyboardType: const TextInputType.numberWithOptions(decimal: false),
        textInputAction: TextInputAction.next,
        decoration: InputDecoration(labelText: label),
      );
}

class _EventContent extends StatelessWidget {
  const _EventContent({
    required this.title,
    required this.location,
    required this.note,
    required this.start,
    required this.end,
    required this.priority,
    required this.reminderMinutes,
    required this.reminderRepeatCount,
    required this.reminderRepeatIntervalMinutes,
    required this.recurrence,
    required this.onPriority,
    required this.onStartDate,
    required this.onStartTime,
    required this.onEndDate,
    required this.onEndTime,
    required this.onReminder,
    required this.onRecurrence,
    required this.onCancel,
    required this.onSave,
  });
  final TextEditingController title;
  final TextEditingController location;
  final TextEditingController note;
  final DateTime start;
  final DateTime end;
  final int priority;
  final int reminderMinutes;
  final int reminderRepeatCount;
  final int reminderRepeatIntervalMinutes;
  final RecurrenceFrequency recurrence;
  final VoidCallback onPriority;
  final VoidCallback onStartDate;
  final VoidCallback onStartTime;
  final VoidCallback onEndDate;
  final VoidCallback onEndTime;
  final VoidCallback onReminder;
  final VoidCallback onRecurrence;
  final VoidCallback onCancel;
  final VoidCallback onSave;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final bottomInset = MediaQuery.viewInsetsOf(context).bottom;
    return Container(
      width: double.infinity,
      decoration: BoxDecoration(color: scheme.surface, borderRadius: const BorderRadius.vertical(top: Radius.circular(42))),
      child: Column(children: [
        Expanded(
          child: SingleChildScrollView(
            keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
            padding: EdgeInsets.fromLTRB(28, 26, 28, 110 + bottomInset),
            child: Column(children: [
              _TitleRow(controller: title, priority: priority, onPriority: onPriority),
              const _Divider(),
              const SizedBox(height: 28),
              _DateTimeRange(start: start, end: end, onStartDate: onStartDate, onStartTime: onStartTime, onEndDate: onEndDate, onEndTime: onEndTime),
              const _Divider(),
              _InputRow(icon: Icons.location_on_outlined, controller: location, hint: 'Vị trí'),
              const _Divider(),
              _ActionRow(icon: Icons.notifications_none_outlined, title: reminderMinutes == 0 ? 'Không báo trước' : 'Trước $reminderMinutes phút', onTap: onReminder),
              const _Divider(),
              _ActionRow(icon: Icons.notification_important_outlined, title: reminderMinutes == 0 ? 'Không lặp thông báo' : 'Lặp $reminderRepeatCount lần · mỗi $reminderRepeatIntervalMinutes phút', onTap: onReminder, muted: reminderMinutes == 0),
              const _Divider(),
              _ActionRow(icon: Icons.repeat_rounded, title: _recurrenceName(recurrence), onTap: onRecurrence, muted: recurrence == RecurrenceFrequency.none),
              const _Divider(),
              _InputRow(icon: Icons.notes_outlined, controller: note, hint: 'Ghi chú', maxLines: 3),
            ]),
          ),
        ),
        _SaveBar(onCancel: onCancel, onSave: onSave),
      ]),
    );
  }
}

class _EditorTabs extends StatelessWidget {
  const _EditorTabs({required this.selectedBulk, required this.onChanged});
  final bool selectedBulk;
  final ValueChanged<bool> onChanged;
  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Container(
      height: 48,
      padding: const EdgeInsets.all(2),
      decoration: BoxDecoration(color: scheme.surfaceContainerHighest, borderRadius: BorderRadius.circular(26)),
      child: Row(children: [
        Expanded(child: _tab(context, 'Sự kiện', !selectedBulk, () => onChanged(false))),
        Expanded(child: _tab(context, 'AI Import', selectedBulk, () => onChanged(true))),
      ]),
    );
  }
  Widget _tab(BuildContext context, String text, bool selected, VoidCallback onTap) {
    final scheme = Theme.of(context).colorScheme;
    return Material(
      color: selected ? scheme.onSurface.withValues(alpha: 0.60) : Colors.transparent,
      shape: const StadiumBorder(),
      child: InkWell(
        onTap: onTap,
        customBorder: const StadiumBorder(),
        child: Center(child: Text(text, style: TextStyle(fontSize: 16, fontWeight: selected ? FontWeight.w700 : FontWeight.w400, color: selected ? scheme.surface : scheme.onSurfaceVariant))),
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
      decoration: BoxDecoration(color: scheme.surface, borderRadius: const BorderRadius.vertical(top: Radius.circular(42))),
      alignment: Alignment.topCenter,
      padding: const EdgeInsets.fromLTRB(28, 42, 28, 28),
      child: Column(children: [
        Icon(Icons.auto_awesome_motion_outlined, size: 42, color: scheme.primary),
        const SizedBox(height: 14),
        Text('AI Import', style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w700)),
        const SizedBox(height: 8),
        Text('Nhập lịch hàng loạt với sự hỗ trợ của AI.', textAlign: TextAlign.center, style: TextStyle(color: scheme.onSurfaceVariant)),
      ]),
    );
  }
}

class _TitleRow extends StatelessWidget {
  const _TitleRow({required this.controller, required this.priority, required this.onPriority});
  final TextEditingController controller;
  final int priority;
  final VoidCallback onPriority;
  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Row(children: [
      Icon(Icons.emoji_emotions_outlined, size: 30, color: scheme.onSurfaceVariant),
      const SizedBox(width: 16),
      Expanded(child: TextField(controller: controller, autofocus: true, style: const TextStyle(fontSize: 30), decoration: const InputDecoration(hintText: 'Tựa đề', hintStyle: TextStyle(fontSize: 30), border: InputBorder.none, isCollapsed: true))),
      const SizedBox(width: 12),
      InkWell(onTap: onPriority, borderRadius: BorderRadius.circular(22), child: Padding(padding: const EdgeInsets.all(8), child: _PriorityDot(priority: priority, size: 30))),
    ]);
  }
}

class _PriorityDot extends StatelessWidget {
  const _PriorityDot({required this.priority, this.size = 22});
  final int priority;
  final double size;
  @override
  Widget build(BuildContext context) => Container(width: size, height: size, decoration: BoxDecoration(shape: BoxShape.circle, color: _priorityColor(context, priority)));
}

class _DateTimeRange extends StatelessWidget {
  const _DateTimeRange({required this.start, required this.end, required this.onStartDate, required this.onStartTime, required this.onEndDate, required this.onEndTime});
  final DateTime start;
  final DateTime end;
  final VoidCallback onStartDate;
  final VoidCallback onStartTime;
  final VoidCallback onEndDate;
  final VoidCallback onEndTime;
  @override
  Widget build(BuildContext context) => Row(children: [
    Expanded(child: _DateTimeColumn(date: _dateLabel(start), time: _timeLabel(start), onDate: onStartDate, onTime: onStartTime)),
    Padding(padding: const EdgeInsets.symmetric(horizontal: 8), child: Icon(Icons.arrow_forward_rounded, size: 34, color: Theme.of(context).colorScheme.onSurfaceVariant)),
    Expanded(child: _DateTimeColumn(date: _dateLabel(end), time: _timeLabel(end), onDate: onEndDate, onTime: onEndTime)),
  ]);
}

class _DateTimeColumn extends StatelessWidget {
  const _DateTimeColumn({required this.date, required this.time, required this.onDate, required this.onTime});
  final String date;
  final String time;
  final VoidCallback onDate;
  final VoidCallback onTime;
  @override
  Widget build(BuildContext context) => Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
    InkWell(onTap: onDate, child: Padding(padding: const EdgeInsets.symmetric(vertical: 5), child: Text(date, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w600)))),
    InkWell(onTap: onTime, child: Padding(padding: const EdgeInsets.symmetric(vertical: 5), child: Text(time, style: const TextStyle(fontSize: 20)))),
  ]);
}

class _InputRow extends StatelessWidget {
  const _InputRow({required this.icon, required this.controller, required this.hint, this.maxLines = 1});
  final IconData icon;
  final TextEditingController controller;
  final String hint;
  final int maxLines;
  @override
  Widget build(BuildContext context) => Row(
    crossAxisAlignment: maxLines > 1 ? CrossAxisAlignment.start : CrossAxisAlignment.center,
    children: [
      Padding(padding: EdgeInsets.only(top: maxLines > 1 ? 10 : 0), child: Icon(icon, size: 30)),
      const SizedBox(width: 20),
      Expanded(child: TextField(controller: controller, maxLines: maxLines, style: const TextStyle(fontSize: 20), decoration: InputDecoration(hintText: hint, hintStyle: TextStyle(color: Theme.of(context).colorScheme.onSurfaceVariant, fontSize: 20), border: InputBorder.none, contentPadding: const EdgeInsets.symmetric(vertical: 16)))),
    ],
  );
}

class _ActionRow extends StatelessWidget {
  const _ActionRow({required this.icon, required this.title, required this.onTap, this.muted = false});
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
        child: Row(children: [
          Icon(icon, size: 30, color: muted ? scheme.onSurfaceVariant : scheme.onSurface),
          const SizedBox(width: 20),
          Expanded(child: Text(title, style: TextStyle(fontSize: 20, color: muted ? scheme.onSurfaceVariant : scheme.onSurface))),
          const SizedBox(width: 12),
          Icon(Icons.chevron_right_rounded, size: 24, color: scheme.onSurfaceVariant),
        ]),
      ),
    );
  }
}

class _Divider extends StatelessWidget {
  const _Divider();
  @override
  Widget build(BuildContext context) => Divider(height: 1, color: Theme.of(context).colorScheme.outlineVariant.withValues(alpha: 0.65));
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
        child: SizedBox(height: 62, child: Row(children: [
          Expanded(child: InkWell(onTap: onCancel, customBorder: const StadiumBorder(), child: const Center(child: Text('Thoát', style: TextStyle(fontSize: 19, fontWeight: FontWeight.w700))))),
          Container(width: 1, height: 28, color: scheme.outlineVariant),
          Expanded(child: InkWell(onTap: onSave, customBorder: const StadiumBorder(), child: const Center(child: Text('Lưu', style: TextStyle(fontSize: 19, fontWeight: FontWeight.w700))))),
        ])),
      ),
    );
  }
}

String _dateLabel(DateTime date) {
  const weekdays = ['T.2', 'T.3', 'T.4', 'T.5', 'T.6', 'T.7', 'CN'];
  return '${weekdays[date.weekday - 1]}, ${date.day.toString().padLeft(2, '0')}/${date.month.toString().padLeft(2, '0')}';
}

String _timeLabel(DateTime date) {
  final hour = date.hour % 12 == 0 ? 12 : date.hour % 12;
  final minute = date.minute.toString().padLeft(2, '0');
  final period = date.hour >= 12 ? 'PM' : 'AM';
  return '${hour.toString().padLeft(2, '0')}:$minute $period';
}

Color _priorityColor(BuildContext context, int priority) {
  final scheme = Theme.of(context).colorScheme;
  return switch (priority) { 2 => scheme.error, 1 => scheme.tertiary, _ => scheme.primary };
}

String _priorityName(int priority) => switch (priority) { 2 => 'Rất quan trọng', 1 => 'Quan trọng', _ => 'Bình thường' };

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
