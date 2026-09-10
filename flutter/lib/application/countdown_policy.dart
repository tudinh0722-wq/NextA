import '../domain/event.dart';

class CountdownPolicy {
  const CountdownPolicy({this.horizon = const Duration(days: 14)});

  final Duration horizon;

  Duration? remaining(NextAEvent event, DateTime now) {
    final delta = event.start.difference(now);
    if (delta.isNegative || delta > horizon) return null;
    return delta;
  }

  String format(Duration remaining) {
    if (remaining.inDays >= 1) return '${remaining.inDays}d';
    final hours = remaining.inHours;
    final minutes = remaining.inMinutes.remainder(60);
    return '${hours}h ${minutes}m';
  }
}
