import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../domain/event.dart';

class EventEditorResult {
  const EventEditorResult({required this.event, required this.deleted});

  final NextAEvent? event;
  final bool deleted;
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
          ? const EventEditorResult(event: null, deleted: true)
          : null;
    }

    if (action != 'edit') return null;
  }

  if (!context.mounted) return null;

  return showDialog<EventEditorResult>(
    context: context,
    builder: (dialogContext) => _EventEditorDialog(
      event: event,
      selectedDay: selectedDay,
    ),
  );
}

class _EventEditorDialog extends StatefulWidget {
  const _EventEditorDialog({required this.event, required this.selectedDay});

  final NextAEvent? event;
  final DateTime selectedDay;

  @override
  State<_EventEditorDialog> createState() => _EventEditorDialogState();
}

class _EventEditorDialogState extends State<_EventEditorDialog> {
  late final TextEditingController _title;
  late final TextEditingController _location;
  late final TextEditingController _note;
  late EventType _type;
  late int _priority;
  late DateTime _start;
  late DateTime _end;

  @override
  void initState() {
    super.initState();
    final event = widget.event;
    _title = TextEditingController(text: event?.title ?? '');
    _location = TextEditingController(text: event?.location ?? '');
    _note = TextEditingController(text: event?.note ?? '');
    _type = event?.type ?? EventType.classEvent;
    _priority = event?.priority ?? 0;
    _start = event?.start ?? DateTime(widget.selectedDay.year, widget.selectedDay.month, widget.selectedDay.day, 8);
    _end = event?.end ?? _start.add(const Duration(hours: 1));
  }

  @override
  void dispose() {
    _title.dispose();
    _location.dispose();
    _note.dispose();
    super.dispose();
  }

  Future<void> _pickStart() async {
    final time = await showTimePicker(
      context: context,
      initialTime: TimeOfDay.fromDateTime(_start),
    );
    if (time == null) return;
    setState(() {
      _start = DateTime(_start.year, _start.month, _start.day, time.hour, time.minute);
      if (!_end.isAfter(_start)) _end = _start.add(const Duration(hours: 1));
    });
  }

  Future<void> _pickEnd() async {
    final time = await showTimePicker(
      context: context,
      initialTime: TimeOfDay.fromDateTime(_end),
    );
    if (time == null) return;
    setState(() {
      _end = DateTime(_end.year, _end.month, _end.day, time.hour, time.minute);
    });
  }

  void _save() {
    final title = _title.text.trim();
    if (title.isEmpty) return;
    if (!_end.isAfter(_start)) _end = _start.add(const Duration(hours: 1));

    final old = widget.event;
    Navigator.pop(
      context,
      EventEditorResult(
        deleted: false,
        event: NextAEvent(
          id: old?.id ?? DateTime.now().microsecondsSinceEpoch.toString(),
          title: title,
          type: _type,
          start: _start,
          end: _end,
          location: _location.text.trim().isEmpty ? null : _location.text.trim(),
          note: _note.text.trim().isEmpty ? null : _note.text.trim(),
          priority: _priority,
          recurrenceId: old?.recurrenceId,
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final timeFormat = DateFormat.Hm();
    return AlertDialog(
      title: Text(widget.event == null ? 'Thêm sự kiện' : 'Chỉnh sửa sự kiện'),
      content: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: _title,
              autofocus: true,
              decoration: const InputDecoration(labelText: 'Tên sự kiện'),
            ),
            TextField(
              controller: _location,
              decoration: const InputDecoration(labelText: 'Địa điểm'),
            ),
            TextField(
              controller: _note,
              maxLines: 2,
              decoration: const InputDecoration(labelText: 'Ghi chú'),
            ),
            const SizedBox(height: 8),
            DropdownButtonFormField<EventType>(
              initialValue: _type,
              decoration: const InputDecoration(labelText: 'Loại'),
              items: EventType.values
                  .map((type) => DropdownMenuItem(
                        value: type,
                        child: Text(eventTypeName(type)),
                      ))
                  .toList(),
              onChanged: (value) => setState(() => _type = value ?? _type),
            ),
            SwitchListTile.adaptive(
              contentPadding: EdgeInsets.zero,
              title: const Text('Ưu tiên'),
              value: _priority > 0,
              onChanged: (value) => setState(() => _priority = value ? 1 : 0),
            ),
            ListTile(
              contentPadding: EdgeInsets.zero,
              title: Text('Bắt đầu  ${timeFormat.format(_start)}'),
              onTap: _pickStart,
            ),
            ListTile(
              contentPadding: EdgeInsets.zero,
              title: Text('Kết thúc  ${timeFormat.format(_end)}'),
              onTap: _pickEnd,
            ),
          ],
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: const Text('Hủy'),
        ),
        FilledButton(
          onPressed: _save,
          child: Text(widget.event == null ? 'Thêm' : 'Lưu'),
        ),
      ],
    );
  }
}

String eventTypeName(EventType type) => switch (type) {
      EventType.classEvent => 'Môn học',
      EventType.exam => 'Thi / kiểm tra',
      EventType.assignment => 'Bài tập',
      EventType.meeting => 'Cuộc họp',
      EventType.personal => 'Cá nhân',
      EventType.other => 'Khác',
    };
