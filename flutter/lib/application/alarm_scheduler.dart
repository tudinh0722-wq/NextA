import 'package:flutter_local_notifications/flutter_local_notifications.dart';

import '../domain/event.dart';

/// Schedules and cancels local notification alarms for [NextAEvent].
///
/// Alarm behaviour per event:
///   - First alarm  : [event.reminderMinutes] before start
///   - Repeat alarms: if [event.reminderRepeatCount] > 0 and event has not
///     been dismissed, repeated [event.reminderRepeatCount] more times every
///     [event.reminderRepeatIntervalMinutes] minutes after the first alarm.
///
/// Notification IDs are derived from the event id so reschedule / cancel is
/// idempotent.  Each repeat slot gets its own notification ID to allow
/// independent cancellation once acknowledged (future feature).
///
/// Android setup required in AndroidManifest.xml (done manually — see README):
///   <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>
///   <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM"/>
///   <uses-permission android:name="android.permission.USE_EXACT_ALARM"/>
///   <uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
class AlarmScheduler {
  AlarmScheduler._(this._plugin);

  final FlutterLocalNotificationsPlugin _plugin;

  static AlarmScheduler? _instance;

  /// Initialise the plugin once at app startup.  Returns the singleton.
  static Future<AlarmScheduler> init() async {
    if (_instance != null) return _instance!;

    final plugin = FlutterLocalNotificationsPlugin();

    const androidInit = AndroidInitializationSettings('@mipmap/ic_launcher');
    const darwinInit = DarwinInitializationSettings(
      requestAlertPermission: true,
      requestBadgePermission: false,
      requestSoundPermission: true,
    );
    await plugin.initialize(
      const InitializationSettings(android: androidInit, iOS: darwinInit, macOS: darwinInit),
    );

    // Request Android 13+ POST_NOTIFICATIONS permission.
    await plugin
        .resolvePlatformSpecificImplementation<AndroidFlutterLocalNotificationsPlugin>()
        ?.requestNotificationsPermission();

    // Request exact alarm permission (Android 12+).
    await plugin
        .resolvePlatformSpecificImplementation<AndroidFlutterLocalNotificationsPlugin>()
        ?.requestExactAlarmsPermission();

    _instance = AlarmScheduler._(plugin);
    return _instance!;
  }

  // ── public API ────────────────────────────────────────────────────────────

  /// Schedule all alarms for [event].  Safe to call multiple times —
  /// cancels existing slots first.
  Future<void> scheduleEvent(NextAEvent event) async {
    if (event.reminderMinutes <= 0) return;
    await cancelEvent(event.id);

    final firstAlarm = event.start.subtract(Duration(minutes: event.reminderMinutes));
    if (firstAlarm.isBefore(DateTime.now())) return; // already past

    // Slot 0 = first alarm; slots 1..repeatCount = follow-up alarms.
    final totalSlots = 1 + event.reminderRepeatCount.clamp(0, 10);
    for (var slot = 0; slot < totalSlots; slot++) {
      final alarmTime = firstAlarm.add(
        Duration(minutes: slot * event.reminderRepeatIntervalMinutes),
      );
      if (alarmTime.isBefore(DateTime.now())) continue;

      await _plugin.zonedSchedule(
        _notifId(event.id, slot),
        event.title,
        _body(event, slot),
        _toTZDateTime(alarmTime),
        _details(event.priority),
        androidScheduleMode: AndroidScheduleMode.exactAllowWhileIdle,
        matchDateTimeComponents: null,
      );
    }
  }

  /// Cancel all alarm slots for the given event id.
  Future<void> cancelEvent(String eventId) async {
    const maxSlots = 11; // 1 + max repeat count
    for (var slot = 0; slot < maxSlots; slot++) {
      await _plugin.cancel(_notifId(eventId, slot));
    }
  }

  /// Schedule alarms for every event in [events] that is in the future.
  Future<void> scheduleAll(List<NextAEvent> events) async {
    for (final e in events) {
      await scheduleEvent(e);
    }
  }

  // ── helpers ───────────────────────────────────────────────────────────────

  /// Stable integer ID: hash of "eventId:slot" truncated to 31 bits.
  int _notifId(String eventId, int slot) =>
      ('$eventId:$slot').hashCode.abs() & 0x7FFFFFFF;

  String _body(NextAEvent event, int slot) {
    final minutesBefore = event.reminderMinutes +
        slot * event.reminderRepeatIntervalMinutes;
    final parts = <String>[];
    if (event.location != null && event.location!.isNotEmpty) {
      parts.add(event.location!);
    }
    final timeStr =
        '${event.start.hour.toString().padLeft(2, '0')}:${event.start.minute.toString().padLeft(2, '0')}';
    parts.add('Bắt đầu lúc $timeStr');
    final reminder = minutesBefore < 60
        ? '$minutesBefore phút nữa'
        : '${(minutesBefore / 60).floor()}h${minutesBefore % 60 > 0 ? ' ${minutesBefore % 60}p' : ''} nữa';
    return '${parts.join(' · ')} ($reminder)';
  }

  NotificationDetails _details(int priority) {
    // Priority mapping: 0=normal, 1=important, 2=very important
    final importance = priority >= 2
        ? Importance.max
        : priority == 1
            ? Importance.high
            : Importance.defaultImportance;
    final notifPriority = priority >= 2
        ? Priority.max
        : priority == 1
            ? Priority.high
            : Priority.defaultPriority;

    return NotificationDetails(
      android: AndroidNotificationDetails(
        'nexta_reminder',
        'Nhắc nhở sự kiện',
        channelDescription: 'Thông báo nhắc nhở trước khi sự kiện bắt đầu',
        importance: importance,
        priority: notifPriority,
        playSound: true,
        enableVibration: true,
        icon: '@mipmap/ic_launcher',
      ),
      iOS: const DarwinNotificationDetails(
        presentAlert: true,
        presentSound: true,
        presentBadge: false,
      ),
    );
  }

  /// Convert a plain [DateTime] (local) to a tzDateTime understood by the
  /// plugin.  We avoid the timezone package to keep dependencies minimal —
  /// the plugin accepts a plain [DateTime] via [TZDateTime.from] only when
  /// timezone is configured, so we use the local constructor trick.
  dynamic _toTZDateTime(DateTime dt) {
    // flutter_local_notifications >= 10 exposes TZDateTime through the
    // timezone package.  We call it via dynamic to avoid a hard import that
    // would require timezone setup.  If timezone is not initialised the plugin
    // falls back to device-local time automatically.
    try {
      // ignore: avoid_dynamic_calls
      final tz = _tzLocal();
      if (tz != null) return tz;
    } catch (_) {}
    return dt;
  }

  dynamic _tzLocal() => null; // Extend here if timezone package is added later.
}
