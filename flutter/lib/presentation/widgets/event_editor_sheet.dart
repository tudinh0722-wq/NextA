import 'package:flutter/cupertino.dart';
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
      builder: (c) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            ListTile(
              leading: const Icon(Icons.edit_outlined),
              title: const Text('Chỉnh sửa sự kiện'),
              onTap: () => Navigator.pop(c, 'edit'),
            ),
            ListTile(
              leading: const Icon(Icons.delete_outline),
              title: const Text('Xóa sự kiện'),
              onTap: () => Navigator.pop(c, 'delete'),
            ),
            const SizedBox(height: 8),
          ],
        ),
      ),
    );
    if (!context.mounted) return null;
    if (action == 'delete') {
      final ok = await showDialog<bool>(
        context: context,
        builder: (c) => AlertDialog(
          title: const Text('Xóa sự kiện?'),
          content: Text('Xóa “${event.title}” khỏi lịch?'),
          actions: [
            TextButton(onPressed: () => Navigator.pop(c, false), child: const Text('Hủy')),
            FilledButton(onPressed: () => Navigator.pop(c, true), child: const Text('Xóa')),
          ],
        ),
      );
      return ok == true
          ? const EventEditorResult(events: [], deleted: true)
          : null;
    }
    if (action != 'edit') return null;
  }

  return showModalBottomSheet<EventEditorResult>(
    context: context,
    isScrollControlled: true,
    useSafeArea: true,
    backgroundColor: Colors.transparent,
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
    final e = widget.event;
    _title = TextEditingController(text: e?.title ?? '');
    _location = TextEditingController(text: e?.location ?? '');
    _note = TextEditingController(text: e?.note ?? '');
    _start = e?.start ?? DateTime(widget.selectedDay.year, widget.selectedDay.month, widget.selectedDay.day, 8);
    _end = e?.end ?? _start.add(const Duration(hours: 1));
    _priority = e?.priority ?? 0;
    _recurrenceRule = e?.recurrenceRule ?? const RecurrenceRule(frequency: RecurrenceFrequency.none);
    _reminderMinutes = e?.reminderMinutes ?? 10;
    _reminderRepeatCount = e?.reminderRepeatCount ?? 2;
    _reminderRepeatIntervalMinutes = e?.reminderRepeatIntervalMinutes ?? 5;
  }

  @override
  void dispose() {
    _title.dispose();
    _location.dispose();
    _note.dispose();
    super.dispose();
  }

  Future<void> _pickDate(bool isStart) async {
    final current = isStart ? _start : _end;
    final date = await showDatePicker(
      context: context,
      initialDate: current,
      firstDate: DateTime(2020),
      lastDate: DateTime(2100),
    );
    if (date == null) return;
    setState(() {
      final value = DateTime(date.year, date.month, date.day, current.hour, current.minute);
      if (isStart) {
        _start = value;
        if (!_end.isAfter(_start)) _end = _start.add(const Duration(hours: 1));
      } else {
        _end = value;
      }
    });
  }

  Future<void> _pickTime(bool isStart) async {
    final current = isStart ? _start : _end;
    DateTime picked = current;
    await showModalBottomSheet<void>(
      context: context,
      showDragHandle: true,
      backgroundColor: Theme.of(context).colorScheme.surface,
      builder: (c) => SafeArea(
        child: SizedBox(
          height: 300,
          child: Column(
            children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(24, 4, 24, 4),
                child: Row(
                  children: [
                    const Expanded(child: Text('Chọn thời gian', style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700))),
                    TextButton(onPressed: () => Navigator.pop(c), child: const Text('Xong')),
                  ],
                ),
              ),
              Expanded(
                child: CupertinoDatePicker(
                  mode: CupertinoDatePickerMode.time,
                  use24hFormat: false,
                  initialDateTime: current,
                  onDateTimeChanged: (v) {
                    picked = DateTime(current.year, current.month, current.day, v.hour, v.minute);
                  },
                ),
              ),
            ],
          ),
        ),
      ),
    );
    if (!mounted) return;
    setState(() {
      if (isStart) {
        _start = picked;
        if (!_end.isAfter(_start)) _end = _start.add(const Duration(hours: 1));
      } else {
        _end = picked;
      }
    });
  }

  Future<void> _pickReminder() async {
    final result = await showDialog<_ReminderSettings>(
      context: context,
      builder: (_) => _ReminderDialog(
        minutes: _reminderMinutes,
        repeatCount: _reminderRepeatCount,
        interval: _reminderRepeatIntervalMinutes,
      ),
    );
    if (result == null) return;
    setState(() {
      _reminderMinutes = result.enabled ? result.minutes : 0;
      _reminderRepeatCount = result.enabled ? result.repeatCount : 0;
      _reminderRepeatIntervalMinutes = result.enabled ? result.interval : 5;
    });
  }

  Future<void> _pickRecurrence() async {
    final result = await showDialog<RecurrenceRule>(
      context: context,
      builder: (_) => _RecurrenceDialog(initial: _recurrenceRule, start: _start),
    );
    if (result != null) setState(() => _recurrenceRule = result);
  }

  Future<void> _pickPriority() async {
    final value = await showModalBottomSheet<int>(
      context: context,
      showDragHandle: true,
      builder: (c) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            for (final p in [0, 1, 2])
              ListTile(
                leading: _PriorityDot(priority: p),
                title: Text(_priorityName(p)),
                trailing: p == _priority ? const Icon(Icons.check_rounded) : null,
                onTap: () => Navigator.pop(c, p),
              ),
            const SizedBox(height: 8),
          ],
        ),
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
      recurrenceId: recurring ? (old?.recurrenceId ?? id) : null,
      recurrenceRule: recurring ? _recurrenceRule : null,
      reminderMinutes: _reminderMinutes,
      reminderRepeatCount: _reminderRepeatCount,
      reminderRepeatIntervalMinutes: _reminderRepeatIntervalMinutes,
    );
    final events = old == null && recurring
        ? generateOccurrences(event, rule: _recurrenceRule)
        : [event];
    Navigator.pop(context, EventEditorResult(events: events, deleted: false));
  }

  @override
  Widget build(BuildContext context) {
    final s = Theme.of(context).colorScheme;
    return Material(
      color: s.surfaceContainerLowest,
      child: SafeArea(
        top: true,
        bottom: false,
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(20, 14, 20, 14),
              child: _EditorTabs(
                selectedBulk: _bulkImportTab,
                onChanged: (v) => setState(() => _bulkImportTab = v),
              ),
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
                      recurrence: _recurrenceRule,
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
          ],
        ),
      ),
    );
  }
}

class _RecurrenceDialog extends StatefulWidget {
  const _RecurrenceDialog({required this.initial, required this.start});
  final RecurrenceRule initial;
  final DateTime start;

  @override
  State<_RecurrenceDialog> createState() => _RecurrenceDialogState();
}

class _RecurrenceDialogState extends State<_RecurrenceDialog> {
  late RecurrenceFrequency _frequency;
  late RecurrenceEndMode _endMode;
  late TextEditingController _count;
  late DateTime _until;

  @override
  void initState() {
    super.initState();
    _frequency = widget.initial.frequency;
    _endMode = widget.initial.endMode;
    _count = TextEditingController(text: (widget.initial.count ?? 20).toString());
    _until = widget.initial.until ?? widget.start.add(const Duration(days: 90));
    if (_until.isBefore(widget.start)) _until = widget.start;
  }

  @override
  void dispose() {
    _count.dispose();
    super.dispose();
  }

  String _name(RecurrenceFrequency f) => switch (f) {
        RecurrenceFrequency.none => 'Không lặp lại',
        RecurrenceFrequency.daily => 'Hàng ngày',
        RecurrenceFrequency.weekly => 'Hàng tuần',
        RecurrenceFrequency.weekdays => 'Thứ 2 – Thứ 6',
        RecurrenceFrequency.monthly => 'Hàng tháng',
      };

  Future<void> _pickUntil() async {
    final picked = await showDatePicker(
      context: context,
      initialDate: _until,
      firstDate: widget.start,
      lastDate: DateTime(2100),
    );
    if (picked != null) {
      setState(() => _until = DateTime(picked.year, picked.month, picked.day, 23, 59, 59));
    }
  }

  void _save() {
    if (_frequency == RecurrenceFrequency.none) {
      Navigator.pop(context, const RecurrenceRule(frequency: RecurrenceFrequency.none));
      return;
    }
    final count = (int.tryParse(_count.text.trim()) ?? 20).clamp(1, 366).toInt();
    Navigator.pop(
      context,
      RecurrenceRule(
        frequency: _frequency,
        endMode: _endMode,
        count: _endMode == RecurrenceEndMode.count ? count : null,
        until: _endMode == RecurrenceEndMode.until ? _until : null,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      scrollable: true,
      title: const Text('Lặp lại'),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          DropdownButtonFormField<RecurrenceFrequency>(
            initialValue: _frequency,
            decoration: const InputDecoration(labelText: 'Lặp lại'),
            items: RecurrenceFrequency.values
                .map((f) => DropdownMenuItem(value: f, child: Text(_name(f))))
                .toList(),
            onChanged: (v) => setState(() => _frequency = v ?? _frequency),
          ),
          if (_frequency != RecurrenceFrequency.none) ...[
            const SizedBox(height: 16),
            const Align(
              alignment: Alignment.centerLeft,
              child: Text('Kết thúc lặp', style: TextStyle(fontWeight: FontWeight.w700)),
            ),
            RadioListTile<RecurrenceEndMode>(
              contentPadding: EdgeInsets.zero,
              value: RecurrenceEndMode.count,
              groupValue: _endMode,
              onChanged: (v) => setState(() => _endMode = v!),
              title: Row(
                children: [
                  const Text('Sau'),
                  const SizedBox(width: 12),
                  SizedBox(
                    width: 72,
                    child: TextField(
                      controller: _count,
                      enabled: _endMode == RecurrenceEndMode.count,
                      keyboardType: TextInputType.number,
                      textAlign: TextAlign.center,
                      decoration: const InputDecoration(isDense: true),
                    ),
                  ),
                  const SizedBox(width: 8),
                  const Text('lần'),
                ],
              ),
            ),
            RadioListTile<RecurrenceEndMode>(
              contentPadding: EdgeInsets.zero,
              value: RecurrenceEndMode.until,
              groupValue: _endMode,
              onChanged: (v) => setState(() => _endMode = v!),
              title: Row(
                children: [
                  const Text('Đến ngày'),
                  const Spacer(),
                  TextButton(
                    onPressed: _endMode == RecurrenceEndMode.until ? _pickUntil : null,
                    child: Text('${_until.day.toString().padLeft(2, '0')}/${_until.month.toString().padLeft(2, '0')}/${_until.year}'),
                  ),
                ],
              ),
            ),
          ],
        ],
      ),
      actions: [
        TextButton(onPressed: () => Navigator.pop(context), child: const Text('Hủy')),
        FilledButton(onPressed: _save, child: const Text('Lưu')),
      ],
    );
  }
}

class _ReminderSettings {
  const _ReminderSettings({required this.enabled, required this.minutes, required this.repeatCount, required this.interval});
  final bool enabled;
  final int minutes;
  final int repeatCount;
  final int interval;
}

class _ReminderDialog extends StatefulWidget {
  const _ReminderDialog({required this.minutes, required this.repeatCount, required this.interval});
  final int minutes;
  final int repeatCount;
  final int interval;

  @override
  State<_ReminderDialog> createState() => _ReminderDialogState();
}

class _ReminderDialogState extends State<_ReminderDialog> {
  late final TextEditingController _minutes;
  late final TextEditingController _repeatCount;
  late final TextEditingController _interval;
  late bool _enabled;

  @override
  void initState() {
    super.initState();
    _enabled = widget.minutes > 0;
    _minutes = TextEditingController(text: (widget.minutes > 0 ? widget.minutes : 10).toString());
    _repeatCount = TextEditingController(text: (widget.repeatCount > 0 ? widget.repeatCount : 2).toString());
    _interval = TextEditingController(text: (widget.interval > 0 ? widget.interval : 5).toString());
  }

  @override
  void dispose() {
    _minutes.dispose();
    _repeatCount.dispose();
    _interval.dispose();
    super.dispose();
  }

  int _value(TextEditingController c, int fallback, int min, int max) =>
      (int.tryParse(c.text.trim()) ?? fallback).clamp(min, max).toInt();

  @override
  Widget build(BuildContext context) => AlertDialog(
        scrollable: true,
        title: const Text('Báo trước'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            SwitchListTile(
              contentPadding: EdgeInsets.zero,
              title: const Text('Báo trước'),
              value: _enabled,
              onChanged: (v) => setState(() => _enabled = v),
            ),
            const SizedBox(height: 4),
            _NumberSetting(label: 'Báo trước', suffix: 'phút', controller: _minutes, enabled: _enabled, autofocus: _enabled),
            const SizedBox(height: 12),
            _NumberSetting(label: 'Lặp lại nếu chưa xác nhận', suffix: 'lần', controller: _repeatCount, enabled: _enabled),
            const SizedBox(height: 12),
            _NumberSetting(label: 'Khoảng cách giữa 2 lần', suffix: 'phút', controller: _interval, enabled: _enabled),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('Hủy')),
          FilledButton(
            onPressed: () => Navigator.pop(
              context,
              _ReminderSettings(
                enabled: _enabled,
                minutes: _value(_minutes, 10, 1, 10080),
                repeatCount: _value(_repeatCount, 2, 0, 20),
                interval: _value(_interval, 5, 1, 1440),
              ),
            ),
            child: const Text('Lưu'),
          ),
        ],
      );
}

class _NumberSetting extends StatelessWidget {
  const _NumberSetting({required this.label, required this.suffix, required this.controller, required this.enabled, this.autofocus = false});
  final String label;
  final String suffix;
  final TextEditingController controller;
  final bool enabled;
  final bool autofocus;

  @override
  Widget build(BuildContext context) {
    final s = Theme.of(context).colorScheme;
    return Row(
      children: [
        Expanded(child: Text(label, style: TextStyle(color: enabled ? null : s.onSurfaceVariant))),
        const SizedBox(width: 12),
        SizedBox(
          width: 64,
          child: TextField(
            controller: controller,
            autofocus: autofocus,
            enabled: enabled,
            textAlign: TextAlign.center,
            keyboardType: const TextInputType.numberWithOptions(decimal: false),
            decoration: const InputDecoration(isDense: true),
          ),
        ),
        const SizedBox(width: 8),
        SizedBox(width: 42, child: Text(suffix)),
      ],
    );
  }
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
  final RecurrenceRule recurrence;
  final VoidCallback onPriority;
  final VoidCallback onStartDate;
  final VoidCallback onStartTime;
  final VoidCallback onEndDate;
  final VoidCallback onEndTime;
  final VoidCallback onReminder;
  final VoidCallback onRecurrence;
  final VoidCallback onCancel;
  final VoidCallback onSave;

  String _date(DateTime d) => '${d.day.toString().padLeft(2, '0')}/${d.month.toString().padLeft(2, '0')}/${d.year}';
  String _time(DateTime d) => '${d.hour.toString().padLeft(2, '0')}:${d.minute.toString().padLeft(2, '0')}';

  String _recurrenceLabel() => switch (recurrence.frequency) {
        RecurrenceFrequency.none => 'Không lặp lại',
        RecurrenceFrequency.daily => 'Hàng ngày',
        RecurrenceFrequency.weekly => 'Hàng tuần',
        RecurrenceFrequency.weekdays => 'Thứ 2 – Thứ 6',
        RecurrenceFrequency.monthly => 'Hàng tháng',
      };

  @override
  Widget build(BuildContext context) {
    final s = Theme.of(context).colorScheme;
    final endText = recurrence.frequency == RecurrenceFrequency.none
        ? ''
        : recurrence.endMode == RecurrenceEndMode.count
            ? 'Tối đa ${recurrence.count ?? 20} lần'
            : 'Đến ${_date(recurrence.until ?? DateTime.now())}';

    return Column(
      children: [
        Expanded(
          child: SingleChildScrollView(
            padding: EdgeInsets.fromLTRB(20, 0, 20, 20 + MediaQuery.viewInsetsOf(context).bottom),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Expanded(
                      child: TextField(
                        controller: title,
                        autofocus: true,
                        decoration: const InputDecoration(
                          hintText: 'Tên sự kiện',
                          border: InputBorder.none,
                          hintStyle: TextStyle(fontSize: 22),
                        ),
                      ),
                    ),
                    const SizedBox(width: 8),
                    IconButton(onPressed: onPriority, icon: _PriorityDot(priority: priority)),
                  ],
                ),
                const Divider(),
                _DateTimeRow(icon: Icons.play_arrow_rounded, label: 'Bắt đầu', value: '${_date(start)}  ${_time(start)}', onDate: onStartDate, onTime: onStartTime),
                _DateTimeRow(icon: Icons.stop_rounded, label: 'Kết thúc', value: '${_date(end)}  ${_time(end)}', onDate: onEndDate, onTime: onEndTime),
                const SizedBox(height: 8),
                TextField(controller: location, decoration: const InputDecoration(prefixIcon: Icon(Icons.location_on_outlined), hintText: 'Địa điểm')),
                const SizedBox(height: 8),
                TextField(controller: note, maxLines: 3, decoration: const InputDecoration(prefixIcon: Icon(Icons.notes_outlined), hintText: 'Ghi chú')),
                const SizedBox(height: 8),
                ListTile(contentPadding: EdgeInsets.zero, leading: const Icon(Icons.notifications_none_rounded), title: Text(reminderMinutes > 0 ? 'Báo trước $reminderMinutes phút' : 'Không nhắc nhở'), trailing: const Icon(Icons.chevron_right_rounded), onTap: onReminder),
                ListTile(contentPadding: EdgeInsets.zero, leading: const Icon(Icons.repeat_rounded), title: const Text('Lặp lại'), subtitle: Text(_recurrenceLabel()), trailing: const Icon(Icons.chevron_right_rounded), onTap: onRecurrence),
                if (endText.isNotEmpty)
                  Padding(padding: const EdgeInsets.only(left: 56, bottom: 8), child: Text(endText, style: TextStyle(color: s.onSurfaceVariant))),
              ],
            ),
          ),
        ),
        Material(
          color: s.surfaceContainer,
          child: SafeArea(
            top: false,
            child: Padding(
              padding: const EdgeInsets.fromLTRB(20, 10, 20, 10),
              child: Row(
                children: [
                  Expanded(child: OutlinedButton(onPressed: onCancel, child: const Text('Thoát'))),
                  const SizedBox(width: 12),
                  Expanded(child: FilledButton(onPressed: onSave, child: const Text('Lưu'))),
                ],
              ),
            ),
          ),
        ),
      ],
    );
  }
}

class _DateTimeRow extends StatelessWidget {
  const _DateTimeRow({required this.icon, required this.label, required this.value, required this.onDate, required this.onTime});
  final IconData icon;
  final String label;
  final String value;
  final VoidCallback onDate;
  final VoidCallback onTime;

  @override
  Widget build(BuildContext context) => ListTile(
        contentPadding: EdgeInsets.zero,
        leading: Icon(icon),
        title: Text(label),
        subtitle: Text(value, style: const TextStyle(fontWeight: FontWeight.w700)),
        trailing: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            IconButton(onPressed: onDate, icon: const Icon(Icons.calendar_today_outlined)),
            IconButton(onPressed: onTime, icon: const Icon(Icons.schedule_outlined)),
          ],
        ),
      );
}

class _EditorTabs extends StatelessWidget {
  const _EditorTabs({required this.selectedBulk, required this.onChanged});
  final bool selectedBulk;
  final ValueChanged<bool> onChanged;

  @override
  Widget build(BuildContext context) => SegmentedButton<bool>(
        segments: const [
          ButtonSegment(value: false, label: Text('Sự kiện')),
          ButtonSegment(value: true, label: Text('AI Import')),
        ],
        selected: {selectedBulk},
        onSelectionChanged: (s) => onChanged(s.first),
      );
}

class _BulkImportSlot extends StatelessWidget {
  const _BulkImportSlot();

  @override
  Widget build(BuildContext context) => Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Text(
            'AI Import sẽ hỗ trợ nhập nhiều sự kiện từ nội dung lịch.',
            textAlign: TextAlign.center,
            style: TextStyle(color: Theme.of(context).colorScheme.onSurfaceVariant),
          ),
        ),
      );
}

class _PriorityDot extends StatelessWidget {
  const _PriorityDot({required this.priority});
  final int priority;

  @override
  Widget build(BuildContext context) {
    final s = Theme.of(context).colorScheme;
    final color = priority == 2 ? s.error : priority == 1 ? s.tertiary : s.primary;
    return Container(width: 18, height: 18, decoration: BoxDecoration(color: color, shape: BoxShape.circle));
  }
}

String _priorityName(int p) => p == 2 ? 'Cao' : p == 1 ? 'Trung bình' : 'Thường';
