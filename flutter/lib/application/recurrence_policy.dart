import '../domain/event.dart';

/// Expands one concrete event into concrete dated occurrences.
///
/// The first occurrence is the supplied event. Every generated occurrence
/// keeps the same series id, recurrence metadata, and reminder configuration.
List<NextAEvent> generateOccurrences(
  NextAEvent seed, {
  required RecurrenceRule rule,
  int maxOccurrences = 366,
}) {
  if (rule.frequency == RecurrenceFrequency.none || maxOccurrences <= 1) {
    return [seed];
  }

  final recurrenceId = seed.recurrenceId ?? seed.id;
  final occurrences = <NextAEvent>[seed];
  var nextStart = seed.start;
  var index = 1;

  while (occurrences.length < maxOccurrences) {
    nextStart = _nextStart(seed.start, nextStart, index, rule);
    if (rule.until != null && nextStart.isAfter(rule.until!)) break;

    final duration = seed.end.difference(seed.start);
    occurrences.add(
      NextAEvent(
        id: '${seed.id}-$index',
        title: seed.title,
        type: seed.type,
        start: nextStart,
        end: nextStart.add(duration),
        location: seed.location,
        note: seed.note,
        priority: seed.priority,
        recurrenceId: recurrenceId,
        recurrenceRule: rule,
        reminderMinutes: seed.reminderMinutes,
        reminderRepeatCount: seed.reminderRepeatCount,
        reminderRepeatIntervalMinutes: seed.reminderRepeatIntervalMinutes,
      ),
    );
    index++;
  }

  return occurrences;
}

DateTime _nextStart(
  DateTime seedStart,
  DateTime current,
  int index,
  RecurrenceRule rule,
) {
  switch (rule.frequency) {
    case RecurrenceFrequency.daily:
      return current.add(Duration(days: rule.interval));
    case RecurrenceFrequency.weekly:
      return current.add(Duration(days: 7 * rule.interval));
    case RecurrenceFrequency.weekdays:
      var candidate = current;
      var remaining = rule.interval;
      while (remaining > 0) {
        candidate = candidate.add(const Duration(days: 1));
        if (candidate.weekday <= DateTime.friday) remaining--;
      }
      return candidate;
    case RecurrenceFrequency.monthly:
      final targetMonth = DateTime(
        seedStart.year,
        seedStart.month + (rule.interval * index),
        1,
      );
      final lastDay = DateTime(targetMonth.year, targetMonth.month + 1, 0).day;
      return DateTime(
        targetMonth.year,
        targetMonth.month,
        seedStart.day.clamp(1, lastDay),
        seedStart.hour,
        seedStart.minute,
        seedStart.second,
        seedStart.millisecond,
        seedStart.microsecond,
      );
    case RecurrenceFrequency.none:
      return current;
  }
}
