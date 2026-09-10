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
  });

  final String id;
  final String title;
  final EventType type;
  final DateTime start;
  final DateTime end;
  final String? location;
  final String? note;
  final int priority;
  final String? recurrenceId;
}
