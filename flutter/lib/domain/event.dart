enum EventType {
  classEvent,
  exam,
  assignment,
  meeting,
  personal,
  other,
}

class NextAEvent {
  const NextAEvent({
    required this.id,
    required this.title,
    required this.type,
    required this.start,
    required this.end,
    this.location,
    this.note,
    this.priority = 0,
    this.recurrenceId,
    this.recurrenceRule,
    this.reminderMinutes = 10,
    this.reminderRepeatCount = 2,
    this.reminderRepeatIntervalMinutes = 5,
  });

  final String id;
  final String title;
  final EventType type;
  final DateTime start;
  final DateTime end;
  final String? location;
  final String? note;
  final int priority;

  /// All concrete occurrences belonging to one series share this id.
  final String? recurrenceId;

  /// Optional rule metadata carried by concrete occurrences so a series can
  /// be regenerated or edited without introducing a separate rule table.
  final RecurrenceRule? recurrenceRule;

  /// Minutes before the event when the first reminder is raised.
  /// A value of 0 disables the reminder.
  final int reminderMinutes;

  /// Number of additional reminders after the first reminder, while the
  /// reminder remains unacknowledged.
  final int reminderRepeatCount;

  /// Minutes between repeated reminders.
  final int reminderRepeatIntervalMinutes;
}

class RecurrenceRule {
  const RecurrenceRule({
    required this.frequency,
    this.interval = 1,
    this.until,
  }) : assert(interval > 0);

  final RecurrenceFrequency frequency;
  final int interval;
  final DateTime? until;
}

enum RecurrenceFrequency {
  none,
  daily,
  weekly,
  weekdays,
  monthly,
}
