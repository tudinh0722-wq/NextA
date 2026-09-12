import 'dart:async';

import 'package:flutter_local_notifications/flutter_local_notifications.dart';

import '../domain/event.dart';
import 'tts_service.dart';

// ── Notification payload key ──────────────────────────────────────────────────
// Payload format stored in the notification so TtsService can speak when
// the user taps the notification or the app is in foreground.
//
// "eventId|slotIndex|minutesBefore|title|note"
// Fields are pipe-separated; note may be empty.
// ─────────────────────────────────────────────────────────────────────────────

/// Schedules local notification alarms for [NextAEvent] with a
/// TTS-first → notification → TTS-last pattern.
///
/// Alarm flow per slot:
///   1. App-foreground listener (or notification tap) triggers TTS pre-announcement.
///   2. Notification appears with event details.
///   3. On tap / foreground receive → TTS post-announcement ("Nhắc lại").
///
/// Because Flutter TTS cannot be invoked from a true background isolate,
/// the TTS is triggered in two places:
///   a. [_onForegroundNotification] — app is visible when alarm fires.
///   b. [_onNotificationTap] — user taps the notification to open app.
///
/// For a fully background TTS (when the screen is off), a native
/// Android Service / BroadcastReceiver is required and is documented in
/// README as a future native extension.
///
/// Android AndroidManifest.xml additions required (manual — see README):
///   <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>
///   <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM"/>
///   <uses-permission android:name="android.permission.USE_EXACT_ALARM"/>
///   <uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
class AlarmScheduler {
  AlarmScheduler._(this._plugin, this._tts);

  final FlutterLocalNotificationsPlugin _plugin;
  final TtsService _tts;

  static AlarmScheduler? _instance;

  // Foreground notification callback — set once during init.
  static void Function(NotificationResponse)? _foregroundHandler;

  // ── init ──────────────────────────────────────────────────────────────────

  static Future<AlarmScheduler> init(TtsService tts) async {
    if (_instance != null) return _instance!;

    final plugin = FlutterLocalNotificationsPlugin();

    // Callback wired to both foreground and background tap paths.
    void onResponse(NotificationResponse r) => _handleResponse(tts, r);

    const androidInit = AndroidInitializationSettings('@mipmap/ic_launcher');
    const darwinInit = DarwinInitializationSettings(
      requestAlertPermission: true,
      requestBadgePermission: false,
      requestSoundPermission: true,
    );
    await plugin.initialize(
      const InitializationSettings(
          android: androidInit, iOS: darwinInit, macOS: darwinInit),
      onDidReceiveNotificationResponse: onResponse,
      onDidReceiveBackgroundNotificationResponse: _backgroundTap,
    );

    // Android permission requests.
    await plugin
        .resolvePlatformSpecificImplementation<
            AndroidFlutterLocalNotificationsPlugin>()
        ?.requestNotificationsPermission();
    await plugin
        .resolvePlatformSpecificImplementation<
            AndroidFlutterLocalNotificationsPlugin>()
        ?.requestExactAlarmsPermission();

    _instance = AlarmScheduler._(plugin, tts);
    return _instance!;
  }

  // ── Notification response handling ────────────────────────────────────────

  /// Called when the notification fires while the app is in the foreground,
  /// or when the user taps a notification to bring the app forward.
  static Future<void> _handleResponse(
      TtsService tts, NotificationResponse r) async {
    final parts = (r.payload ?? '').split('|');
    if (parts.length < 5) return;

    final slotIndex = int.tryParse(parts[1]) ?? 0;
    final minutesBefore = int.tryParse(parts[2]) ?? 0;
    final title = parts[3];
    final note = parts[4].isNotEmpty ? parts[4] : null;

    // TTS pre-announcement.
    final preText = TtsService.buildAnnouncement(
      title: title,
      note: note,
      minutesBefore: minutesBefore,
      isRepeat: slotIndex > 0,
    );
    await tts.speak(preText);

    // Brief pause, then TTS post-announcement.
    await Future<void>.delayed(const Duration(milliseconds: 600));
    final postText = TtsService.buildAnnouncement(
      title: title,
      note: note,
      isRepeat: true,
    );
    // Only read the post announcement if it differs from pre (slot 0 already
    // said the countdown; slot > 0 just needs the repeat label).
    if (slotIndex == 0) {
      await tts.speak(postText);
    }
  }

  /// Top-level function required by flutter_local_notifications for background
  /// notification responses.  Cannot capture closures — TTS is not available
  /// in the background isolate without native bridging; log for now.
  @pragma('vm:entry-point')
  static void _backgroundTap(NotificationResponse r) {
    // Background TTS requires a native Android Service.
    // The notification itself carries all event details — no action needed here
    // until the native extension is implemented.
  }

  // ── Public API ────────────────────────────────────────────────────────────

  /// Schedule all alarm slots for [event].  Idempotent — cancels first.
  Future<void> scheduleEvent(NextAEvent event) async {
    if (event.reminderMinutes <= 0) return;
    await cancelEvent(event.id);

    final firstAlarm =
        event.start.subtract(Duration(minutes: event.reminderMinutes));
    if (firstAlarm.isBefore(DateTime.now())) return;

    // Slot 0 = first alarm; slots 1..repeatCount = follow-ups.
    final totalSlots = 1 + event.reminderRepeatCount.clamp(0, 10);
    for (var slot = 0; slot < totalSlots; slot++) {
      final alarmTime = firstAlarm.add(
        Duration(minutes: slot * event.reminderRepeatIntervalMinutes),
      );
      if (alarmTime.isBefore(DateTime.now())) continue;

      final minutesBefore =
          event.reminderMinutes - slot * event.reminderRepeatIntervalMinutes;
      final payload =
          '${event.id}|$slot|$minutesBefore|${event.title}|${event.note ?? ''}';

      await _plugin.zonedSchedule(
        _notifId(event.id, slot),
        event.title,
        _notifBody(event, slot),
        _toTZDateTime(alarmTime),
        _details(event.priority),
        androidScheduleMode: AndroidScheduleMode.exactAllowWhileIdle,
        payload: payload,
      );
    }
  }

  /// Cancel all alarm slots for [eventId].
  Future<void> cancelEvent(String eventId) async {
    const maxSlots = 11;
    for (var slot = 0; slot < maxSlots; slot++) {
      await _plugin.cancel(_notifId(eventId, slot));
    }
  }

  /// Schedule all future events.
  Future<void> scheduleAll(List<NextAEvent> events) async {
    for (final e in events) {
      await scheduleEvent(e);
    }
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  int _notifId(String eventId, int slot) =>
      ('$eventId:$slot').hashCode.abs() & 0x7FFFFFFF;

  String _notifBody(NextAEvent event, int slot) {
    final minutesBefore =
        event.reminderMinutes - slot * event.reminderRepeatIntervalMinutes;
    final parts = <String>[];
    if (event.location != null && event.location!.isNotEmpty) {
      parts.add(event.location!);
    }
    if (event.note != null && event.note!.isNotEmpty) {
      parts.add(event.note!);
    }
    final timeStr =
        '${event.start.hour.toString().padLeft(2, '0')}:${event.start.minute.toString().padLeft(2, '0')}';
    final reminder = minutesBefore > 0
        ? (minutesBefore < 60
            ? '$minutesBefore phút nữa'
            : '${minutesBefore ~/ 60}h${minutesBefore % 60 > 0 ? '${minutesBefore % 60}p' : ''} nữa')
        : 'Đang diễn ra';
    return '${parts.join(' · ')} · $timeStr ($reminder)';
  }

  NotificationDetails _details(int priority) {
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
        channelDescription:
            'Thông báo nhắc nhở trước khi sự kiện bắt đầu',
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

  /// Convert local [DateTime] to the type expected by [zonedSchedule].
  /// Uses plain DateTime — the plugin falls back to device local time
  /// when the timezone package is not configured.
  dynamic _toTZDateTime(DateTime dt) => dt;
}
