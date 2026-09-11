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

Future<EventEditorResult?> showEventEditor(BuildContext context, {NextAEvent? event, required DateTime selectedDay}) async {
  if (event != null) {
    final action = await showModalBottomSheet<String>(context: context, showDragHandle: true, builder: (sheetContext) => SafeArea(child: Column(mainAxisSize: MainAxisSize.min, children: [
      ListTile(leading: const Icon(Icons.edit_outlined), title: const Text('Chỉnh sửa sự kiện'), onTap: () => Navigator.pop(sheetContext, 'edit')),
      ListTile(leading: const Icon(Icons.delete_outline), title: const Text('Xóa sự kiện'), onTap: () => Navigator.pop(sheetContext, 'delete')),
      const SizedBox(height: 8),
    ])));
    if (!context.mounted) return null;
    if (action == 'delete') {
      final confirmed = await showDialog<bool>(context: context, builder: (dialogContext) => AlertDialog(title: const Text('Xóa sự kiện?'), content: Text('Xóa “${event.title}” khỏi lịch?'), actions: [
        TextButton(onPressed: () => Navigator.pop(dialogContext, false), child: const Text('Hủy')), FilledButton(onPressed: () => Navigator.pop(dialogContext, true), child: const Text('Xóa')),
      ]));
      if (!context.mounted) return null;
      return confirmed == true ? const EventEditorResult(events: [], deleted: true) : null;
    }
    if (action != 'edit') return null;
  }
  return showModalBottomSheet<EventEditorResult>(context: context, isScrollControlled: true, backgroundColor: Colors.transparent, useSafeArea: true, builder: (_) => _EventEditorSheet(event: event, selectedDay: selectedDay));
}

class _EventEditorSheet extends StatefulWidget {
  const _EventEditorSheet({required this.event, required this.selectedDay});
  final NextAEvent? event; final DateTime selectedDay;
  @override State<_EventEditorSheet> createState() => _EventEditorSheetState();
}

class _EventEditorSheetState extends State<_EventEditorSheet> {
  late final TextEditingController _title, _location, _note;
  late DateTime _start, _end;
  late int _priority;
  late RecurrenceRule _recurrenceRule;
  int _reminderMinutes = 10, _reminderRepeatCount = 2, _reminderRepeatIntervalMinutes = 5;
  bool _bulkImportTab = false;

  @override
  void initState() {
    super.initState(); final e = widget.event;
    _title = TextEditingController(text: e?.title ?? ''); _location = TextEditingController(text: e?.location ?? ''); _note = TextEditingController(text: e?.note ?? '');
    _start = e?.start ?? DateTime(widget.selectedDay.year, widget.selectedDay.month, widget.selectedDay.day, 8); _end = e?.end ?? _start.add(const Duration(hours: 1));
    _priority = e?.priority ?? 0; _recurrenceRule = e?.recurrenceRule ?? const RecurrenceRule(frequency: RecurrenceFrequency.none);
    _reminderMinutes = e?.reminderMinutes ?? 10; _reminderRepeatCount = e?.reminderRepeatCount ?? 2; _reminderRepeatIntervalMinutes = e?.reminderRepeatIntervalMinutes ?? 5;
  }
  @override void dispose() { _title.dispose(); _location.dispose(); _note.dispose(); super.dispose(); }

  Future<void> _pickDate(bool start) async {
    final current = start ? _start : _end;
    final date = await showDatePicker(context: context, initialDate: current, firstDate: DateTime(2020), lastDate: DateTime(2100)); if (date == null) return;
    setState(() { final value = DateTime(date.year, date.month, date.day, current.hour, current.minute); if (start) { _start = value; if (!_end.isAfter(_start)) _end = _start.add(const Duration(hours: 1)); } else { _end = value; } });
  }

  Future<void> _pickTime(bool start) async {
    final current = start ? _start : _end; DateTime picked = current;
    await showModalBottomSheet<void>(context: context, showDragHandle: true, backgroundColor: Theme.of(context).colorScheme.surface, builder: (sheetContext) => SafeArea(child: SizedBox(height: 300, child: Column(children: [
      Padding(padding: const EdgeInsets.fromLTRB(24, 4, 24, 4), child: Row(children: [const Expanded(child: Text('Chọn thời gian', style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700))), TextButton(onPressed: () => Navigator.pop(sheetContext), child: const Text('Xong'))])),
      Expanded(child: CupertinoDatePicker(mode: CupertinoDatePickerMode.time, use24hFormat: false, initialDateTime: current, onDateTimeChanged: (value) => picked = DateTime(current.year, current.month, current.day, value.hour, value.minute))),
    ])));
    if (!mounted) return;
    setState(() { if (start) { _start = picked; if (!_end.isAfter(_start)) _end = _start.add(const Duration(hours: 1)); } else { _end = picked; } });
  }

  Future<void> _pickReminder() async {
    final result = await showDialog<_ReminderSettings>(context: context, builder: (_) => _ReminderDialog(minutes: _reminderMinutes, repeatCount: _reminderRepeatCount, repeatIntervalMinutes: _reminderRepeatIntervalMinutes));
    if (result == null) return;
    setState(() { _reminderMinutes = result.enabled ? result.minutes : 0; _reminderRepeatCount = result.enabled ? result.repeatCount : 0; _reminderRepeatIntervalMinutes = result.enabled ? result.repeatIntervalMinutes : 5; });
  }

  Future<void> _pickRecurrence() async {
    final value = await showDialog<RecurrenceFrequency>(context: context, builder: (dialogContext) => AlertDialog(title: const Text('Lặp lại'), content: SingleChildScrollView(child: Column(mainAxisSize: MainAxisSize.min, children: [
      for (final frequency in RecurrenceFrequency.values) ListTile(contentPadding: EdgeInsets.zero, leading: Icon(_recurrenceIcon(frequency)), title: Text(_recurrenceName(frequency)), trailing: frequency == _recurrenceRule.frequency ? const Icon(Icons.check_rounded) : null, onTap: () => Navigator.pop(dialogContext, frequency)),
    ]))));
    if (value != null) setState(() => _recurrenceRule = RecurrenceRule(frequency: value));
  }

  Future<void> _pickPriority() async {
    final value = await showModalBottomSheet<int>(context: context, showDragHandle: true, builder: (sheetContext) => SafeArea(child: Column(mainAxisSize: MainAxisSize.min, children: [
      for (final priority in [0, 1, 2]) ListTile(leading: _PriorityDot(priority: priority), title: Text(_priorityName(priority)), trailing: priority == _priority ? const Icon(Icons.check_rounded) : null, onTap: () => Navigator.pop(sheetContext, priority)), const SizedBox(height: 8),
    ])));
    if (value != null) setState(() => _priority = value);
  }

  void _save() {
    final title = _title.text.trim(); if (title.isEmpty) return; if (!_end.isAfter(_start)) _end = _start.add(const Duration(hours: 1));
    final old = widget.event, id = old?.id ?? DateTime.now().microsecondsSinceEpoch.toString(), recurring = _recurrenceRule.frequency != RecurrenceFrequency.none;
    final event = NextAEvent(id: id, title: title, type: old?.type ?? EventType.classEvent, start: _start, end: _end, location: _location.text.trim().isEmpty ? null : _location.text.trim(), note: _note.text.trim().isEmpty ? null : _note.text.trim(), priority: _priority, recurrenceId: recurring ? (old?.recurrenceId ?? id) : old?.recurrenceId, recurrenceRule: recurring ? _recurrenceRule : null, reminderMinutes: _reminderMinutes, reminderRepeatCount: _reminderRepeatCount, reminderRepeatIntervalMinutes: _reminderRepeatIntervalMinutes);
    final events = old == null && recurring ? generateOccurrences(event, rule: _recurrenceRule) : [event]; Navigator.pop(context, EventEditorResult(events: events, deleted: false));
  }

  @override
  Widget build(BuildContext context) {
    final s = Theme.of(context).colorScheme;
    return Material(color: s.surfaceContainerLowest, child: SafeArea(top: true, bottom: false, child: Column(children: [
      Padding(padding: const EdgeInsets.fromLTRB(20, 14, 20, 14), child: _EditorTabs(selectedBulk: _bulkImportTab, onChanged: (v) => setState(() => _bulkImportTab = v))),
      Expanded(child: _bulkImportTab ? const _BulkImportSlot() : _EventContent(title: _title, location: _location, note: _note, start: _start, end: _end, priority: _priority, reminderMinutes: _reminderMinutes, reminderRepeatCount: _reminderRepeatCount, reminderRepeatIntervalMinutes: _reminderRepeatIntervalMinutes, recurrence: _recurrenceRule.frequency, onPriority: _pickPriority, onStartDate: () => _pickDate(true), onStartTime: () => _pickTime(true), onEndDate: () => _pickDate(false), onEndTime: () => _pickTime(false), onReminder: _pickReminder, onRecurrence: _pickRecurrence, onCancel: () => Navigator.pop(context), onSave: _save)),
    ])));
  }
}

class _ReminderSettings { const _ReminderSettings({required this.enabled, required this.minutes, required this.repeatCount, required this.repeatIntervalMinutes}); final bool enabled; final int minutes, repeatCount, repeatIntervalMinutes; }
class _ReminderDialog extends StatefulWidget { const _ReminderDialog({required this.minutes, required this.repeatCount, required this.repeatIntervalMinutes}); final int minutes, repeatCount, repeatIntervalMinutes; @override State<_ReminderDialog> createState() => _ReminderDialogState(); }
class _ReminderDialogState extends State<_ReminderDialog> {
  late final TextEditingController _minutes, _repeatCount, _interval; late bool _enabled;
  @override void initState() { super.initState(); _enabled = widget.minutes > 0; _minutes = TextEditingController(text: (widget.minutes > 0 ? widget.minutes : 10).toString()); _repeatCount = TextEditingController(text: (widget.repeatCount > 0 ? widget.repeatCount : 2).toString()); _interval = TextEditingController(text: widget.repeatIntervalMinutes.toString()); }
  @override void dispose() { _minutes.dispose(); _repeatCount.dispose(); _interval.dispose(); super.dispose(); }
  int _value(TextEditingController c, {int fallback = 0, int min = 0, int max = 9999}) => (int.tryParse(c.text.trim()) ?? fallback).clamp(min, max);
  void _save() => Navigator.pop(context, _ReminderSettings(enabled: _enabled, minutes: _value(_minutes, fallback: 10, min: 1, max: 10080), repeatCount: _value(_repeatCount, fallback: 2, min: 0, max: 20), repeatIntervalMinutes: _value(_interval, fallback: 5, min: 1, max: 1440)));
  @override Widget build(BuildContext context) => AlertDialog(scrollable: true, title: const Text('Báo trước'), content: Column(mainAxisSize: MainAxisSize.min, children: [
    SwitchListTile(contentPadding: EdgeInsets.zero, title: const Text('Báo trước'), value: _enabled, onChanged: (v) => setState(() => _enabled = v)),
    const SizedBox(height: 4),
    _NumberSetting(label: 'Báo trước', suffix: 'phút', controller: _minutes, enabled: _enabled, autofocus: _enabled),
    const SizedBox(height: 12), _NumberSetting(label: 'Lặp lại nếu chưa xác nhận', suffix: 'lần', controller: _repeatCount, enabled: _enabled),
    const SizedBox(height: 12), _NumberSetting(label: 'Khoảng cách giữa 2 lần', suffix: 'phút', controller: _interval, enabled: _enabled),
  ]), actions: [TextButton(onPressed: () => Navigator.pop(context), child: const Text('Hủy')), FilledButton(onPressed: _save, child: const Text('Lưu'))]);
}
class _NumberSetting extends StatelessWidget { const _NumberSetting({required this.label, required this.suffix, required this.controller, required this.enabled, this.autofocus = false}); final String label, suffix; final TextEditingController controller; final bool enabled, autofocus; @override Widget build(BuildContext context) => Row(children: [Expanded(child: Text(label, style: TextStyle(color: enabled ? null : Theme.of(context).colorScheme.onSurfaceVariant))), const SizedBox(width: 12), SizedBox(width: 64, child: TextField(controller: controller, autofocus: autofocus, enabled: enabled, textAlign: TextAlign.center, keyboardType: const TextInputType.numberWithOptions(decimal: false), decoration: const InputDecoration(isDense: true))), const SizedBox(width: 8), SizedBox(width: 42, child: Text(suffix))]); }

class _EventContent extends StatelessWidget {
  const _EventContent({required this.title, required this.location, required this.note, required this.start, required this.end, required this.priority, required this.reminderMinutes, required this.reminderRepeatCount, required this.reminderRepeatIntervalMinutes, required this.recurrence, required this.onPriority, required this.onStartDate, required this.onStartTime, required this.onEndDate, required this.onEndTime, required this.onReminder, required this.onRecurrence, required this.onCancel, required this.onSave});
  final TextEditingController title, location, note; final DateTime start, end; final int priority, reminderMinutes, reminderRepeatCount, reminderRepeatIntervalMinutes; final RecurrenceFrequency recurrence; final VoidCallback onPriority, onStartDate, onStartTime, onEndDate, onEndTime, onReminder, onRecurrence, onCancel, onSave;
  @override Widget build(BuildContext context) { final s = Theme.of(context).colorScheme; final bottomInset = MediaQuery.viewInsetsOf(context).bottom; return Container(width: double.infinity, decoration: BoxDecoration(color: s.surface, borderRadius: const BorderRadius.vertical(top: Radius.circular(42))), child: Column(children: [
    Expanded(child: SingleChildScrollView(keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag, padding: EdgeInsets.fromLTRB(28, 26, 28, 110 + bottomInset), child: Column(children: [
      _TitleRow(controller: title, priority: priority, onPriority: onPriority), const _Divider(), const SizedBox(height: 28),
      _DateTimeRange(start: start, end: end, onStartDate: onStartDate, onStartTime: onStartTime, onEndDate: onEndDate, onEndTime: onEndTime), const _Divider(),
      _InputRow(icon: Icons.location_on_outlined, controller: location, hint: 'Địa điểm'), const _Divider(),
      _InputRow(icon: Icons.notes_outlined, controller: note, hint: 'Ghi chú', maxLines: 3), const _Divider(),
      _ActionRow(icon: Icons.notifications_none_outlined, title: reminderMinutes == 0 ? 'Không báo trước' : 'Trước $reminderMinutes phút', onTap: onReminder), const _Divider(),
      _ActionRow(icon: Icons.notification_important_outlined, title: reminderMinutes == 0 ? 'Không lặp thông báo' : 'Lặp $reminderRepeatCount lần · mỗi $reminderRepeatIntervalMinutes phút', onTap: onReminder, muted: reminderMinutes == 0), const _Divider(),
      _ActionRow(icon: Icons.repeat_rounded, title: _recurrenceName(recurrence), onTap: onRecurrence, muted: recurrence == RecurrenceFrequency.none),
    ]))), _SaveBar(onCancel: onCancel, onSave: onSave),
  ])); }
}

class _EditorTabs extends StatelessWidget { const _EditorTabs({required this.selectedBulk, required this.onChanged}); final bool selectedBulk; final ValueChanged<bool> onChanged; @override Widget build(BuildContext context) { final s = Theme.of(context).colorScheme; return Container(height: 48, padding: const EdgeInsets.all(2), decoration: BoxDecoration(color: s.surfaceContainerHighest, borderRadius: BorderRadius.circular(26)), child: Row(children: [Expanded(child: _tab(context, 'Sự kiện', !selectedBulk, () => onChanged(false))), Expanded(child: _tab(context, 'AI Import', selectedBulk, () => onChanged(true)))])); } Widget _tab(BuildContext context, String text, bool selected, VoidCallback onTap) { final s = Theme.of(context).colorScheme; return Material(color: selected ? s.onSurface.withValues(alpha: .60) : Colors.transparent, shape: const StadiumBorder(), child: InkWell(onTap: onTap, customBorder: const StadiumBorder(), child: Center(child: Text(text, style: TextStyle(fontSize: 16, fontWeight: selected ? FontWeight.w700 : FontWeight.w400, color: selected ? s.surface : s.onSurfaceVariant))))); } }
class _BulkImportSlot extends StatelessWidget { const _BulkImportSlot(); @override Widget build(BuildContext context) { final s = Theme.of(context).colorScheme; return Container(width: double.infinity, decoration: BoxDecoration(color: s.surface, borderRadius: const BorderRadius.vertical(top: Radius.circular(42))), alignment: Alignment.topCenter, padding: const EdgeInsets.fromLTRB(28, 42, 28, 28), child: Column(children: [Icon(Icons.auto_awesome_motion_outlined, size: 42, color: s.primary), const SizedBox(height: 14), Text('AI Import', style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w700)), const SizedBox(height: 8), Text('Nhập lịch hàng loạt với sự hỗ trợ của AI.', textAlign: TextAlign.center, style: TextStyle(color: s.onSurfaceVariant))])); } }
class _TitleRow extends StatelessWidget { const _TitleRow({required this.controller, required this.priority, required this.onPriority}); final TextEditingController controller; final int priority; final VoidCallback onPriority; @override Widget build(BuildContext context) => Row(children: [Icon(Icons.emoji_emotions_outlined, size: 30, color: Theme.of(context).colorScheme.onSurfaceVariant), const SizedBox(width: 16), Expanded(child: TextField(controller: controller, autofocus: true, style: const TextStyle(fontSize: 30), decoration: const InputDecoration(hintText: 'Tựa đề', hintStyle: TextStyle(fontSize: 30), border: InputBorder.none, isCollapsed: true))), const SizedBox(width: 12), InkWell(onTap: onPriority, borderRadius: BorderRadius.circular(22), child: Padding(padding: const EdgeInsets.all(8), child: _PriorityDot(priority: priority, size: 30)))]); }
class _PriorityDot extends StatelessWidget { const _PriorityDot({required this.priority, this.size = 22}); final int priority; final double size; @override Widget build(BuildContext context) => Container(width: size, height: size, decoration: BoxDecoration(shape: BoxShape.circle, color: _priorityColor(context, priority))); }
class _DateTimeRange extends StatelessWidget { const _DateTimeRange({required this.start, required this.end, required this.onStartDate, required this.onStartTime, required this.onEndDate, required this.onEndTime}); final DateTime start, end; final VoidCallback onStartDate, onStartTime, onEndDate, onEndTime; @override Widget build(BuildContext context) => Row(children: [Expanded(child: _DateTimeColumn(date: _dateLabel(start), time: _timeLabel(start), onDate: onStartDate, onTime: onStartTime)), Padding(padding: const EdgeInsets.symmetric(horizontal: 8), child: Icon(Icons.arrow_forward_rounded, size: 34, color: Theme.of(context).colorScheme.onSurfaceVariant)), Expanded(child: _DateTimeColumn(date: _dateLabel(end), time: _timeLabel(end), onDate: onEndDate, onTime: onEndTime))]); }
class _DateTimeColumn extends StatelessWidget { const _DateTimeColumn({required this.date, required this.time, required this.onDate, required this.onTime}); final String date, time; final VoidCallback onDate, onTime; @override Widget build(BuildContext context) => Column(crossAxisAlignment: CrossAxisAlignment.start, children: [InkWell(onTap: onDate, child: Padding(padding: const EdgeInsets.symmetric(vertical: 5), child: Text(date, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w600)))), InkWell(onTap: onTime, child: Padding(padding: const EdgeInsets.symmetric(vertical: 5), child: Text(time, style: const TextStyle(fontSize: 20)))]); }
class _InputRow extends StatelessWidget { const _InputRow({required this.icon, required this.controller, required this.hint, this.maxLines = 1}); final IconData icon; final TextEditingController controller; final String hint; final int maxLines; @override Widget build(BuildContext context) => Row(crossAxisAlignment: maxLines > 1 ? CrossAxisAlignment.start : CrossAxisAlignment.center, children: [Padding(padding: EdgeInsets.only(top: maxLines > 1 ? 10 : 0), child: Icon(icon, size: 30)), const SizedBox(width: 20), Expanded(child: TextField(controller: controller, maxLines: maxLines, style: const TextStyle(fontSize: 20), decoration: InputDecoration(hintText: hint, hintStyle: TextStyle(color: Theme.of(context).colorScheme.onSurfaceVariant, fontSize: 20), border: InputBorder.none, contentPadding: const EdgeInsets.symmetric(vertical: 16))))]); }
class _ActionRow extends StatelessWidget { const _ActionRow({required this.icon, required this.title, required this.onTap, this.muted = false}); final IconData icon; final String title; final VoidCallback onTap; final bool muted; @override Widget build(BuildContext context) { final s = Theme.of(context).colorScheme; return InkWell(onTap: onTap, child: Padding(padding: const EdgeInsets.symmetric(vertical: 16), child: Row(children: [Icon(icon, size: 30, color: muted ? s.onSurfaceVariant : s.onSurface), const SizedBox(width: 20), Expanded(child: Text(title, style: TextStyle(fontSize: 20, color: muted ? s.onSurfaceVariant : s.onSurface))), const SizedBox(width: 12), Icon(Icons.chevron_right_rounded, size: 24, color: s.onSurfaceVariant)]))); } }
class _Divider extends StatelessWidget { const _Divider(); @override Widget build(BuildContext context) => Divider(height: 1, color: Theme.of(context).colorScheme.outlineVariant.withValues(alpha: .65)); }
class _SaveBar extends StatelessWidget { const _SaveBar({required this.onCancel, required this.onSave}); final VoidCallback onCancel, onSave; @override Widget build(BuildContext context) { final s = Theme.of(context).colorScheme; return Padding(padding: const EdgeInsets.fromLTRB(28, 8, 28, 18), child: Material(elevation: 2, shadowColor: s.shadow.withValues(alpha: .18), color: s.surfaceContainerLowest, shape: const StadiumBorder(), child: SizedBox(height: 62, child: Row(children: [Expanded(child: InkWell(onTap: onCancel, customBorder: const StadiumBorder(), child: const Center(child: Text('Thoát', style: TextStyle(fontSize: 19, fontWeight: FontWeight.w700))))), Container(width: 1, height: 28, color: s.outlineVariant), Expanded(child: InkWell(onTap: onSave, customBorder: const StadiumBorder(), child: const Center(child: Text('Lưu', style: TextStyle(fontSize: 19, fontWeight: FontWeight.w700)))))])))); } }
String _dateLabel(DateTime d) { const w = ['T.2','T.3','T.4','T.5','T.6','T.7','CN']; return '${w[d.weekday - 1]}, ${d.day.toString().padLeft(2,'0')}/${d.month.toString().padLeft(2,'0')}'; }
String _timeLabel(DateTime d) { final h = d.hour % 12 == 0 ? 12 : d.hour % 12; return '${h.toString().padLeft(2,'0')}:${d.minute.toString().padLeft(2,'0')} ${d.hour >= 12 ? 'PM' : 'AM'}'; }
Color _priorityColor(BuildContext c, int p) { final s = Theme.of(c).colorScheme; return switch (p) { 2 => s.error, 1 => s.tertiary, _ => s.primary }; }
String _priorityName(int p) => switch (p) { 2 => 'Rất quan trọng', 1 => 'Quan trọng', _ => 'Bình thường' };
String _recurrenceName(RecurrenceFrequency f) => switch (f) { RecurrenceFrequency.none => 'Không lặp lại', RecurrenceFrequency.daily => 'Hàng ngày', RecurrenceFrequency.weekly => 'Hàng tuần', RecurrenceFrequency.weekdays => 'Thứ 2 – Thứ 6', RecurrenceFrequency.monthly => 'Hàng tháng' };
IconData _recurrenceIcon(RecurrenceFrequency f) => switch (f) { RecurrenceFrequency.none => Icons.repeat_rounded, RecurrenceFrequency.daily => Icons.today_outlined, RecurrenceFrequency.weekly => Icons.view_week_outlined, RecurrenceFrequency.weekdays => Icons.work_outline_rounded, RecurrenceFrequency.monthly => Icons.calendar_month_outlined };
