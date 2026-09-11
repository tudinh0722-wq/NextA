import '../domain/event.dart';

/// Expands one concrete event into concrete dated occurrences.
///
/// The first occurrence is the supplied event. Every generated occurrence
/// keeps the same series id and recurrence metadata; no separate recurrence
/// entity is required by the planner layer.
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
    nextStart = _nextStart(nextStart, rule);
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
      ),
    );
    index++;
  }

  return occurrences;
}

DateTime _nextStart(DateTime current, RecurrenceRule rule) {
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
      final targetMonth = DateTime(current.year, current.month + rule.interval, 1);
      final day = current.day;
      final lastDay = DateTime(targetMonth.year, targetMonth.month + 1, 0).day;
      return DateTime(
        targetMonth.year,
        targetMonth.month,
        day.clamp(1, lastDay),
        current.hour,
        current.minute,
        current.second,
        current.millisecond,
        current.microsecond,
      );
    case RecurrenceFrequency.none:
      return current;
  }
}
