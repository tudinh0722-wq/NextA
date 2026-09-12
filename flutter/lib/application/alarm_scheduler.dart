import 'dart:async';

import 'package:android_intent_plus/android_intent.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';

import '../domain/event.dart';
import 'tts_service.dart';

// Payload format: "eventId|slotIndex|minutesBefore|title|note"
// Pipe-separated; note may be empty.

/// Schedules local notification alarms for [NextAEvent].
///
/// Alarm flow per slot:
///   1. Notification fires (device sound/vibration).
///   2. If app is foreground or user taps notification → TTS announces.
///   3. On repeat slots → TTS says “Nhắc lại + title + note”.
///
/// Android AndroidManifest.xml permissions required (manual — see README):
///   RECEIVE_BOOT_COMPLETED, SCHEDULE_EXACT_ALARM,
///   USE_EXACT_ALARM, POST_NOTIFICATIONS
class AlarmScheduler {
  AlarmScheduler._(this._plugin, this._tts);

  final FlutterLocalNotificationsPlugin _plugin;
  final TtsService _tts;

  static AlarmScheduler? _instance;

  // ── init ──────────────────────────────────────────────────────────────

  static Future<AlarmScheduler> init(TtsService tts) async {
    if (_instance != null) return _instance!;

    final plugin = FlutterLocalNotificationsPlugin();

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

    _instance = AlarmScheduler._(plugin, tts);
    // Request permissions asynchronously to avoid blocking app startup
    unawaited(_instance!._checkAndRequestPermissions());
    
    return _instance!;
  }

  Future<void> _checkAndRequestPermissions() async {
    // Chờ một chút để app hoàn tất việc vẽ khung hình đầu tiên trước khi yêu cầu quyền
    await Future<void>.delayed(const Duration(seconds: 1));

    final androidImpl = _plugin
        .resolvePlatformSpecificImplementation<
            AndroidFlutterLocalNotificationsPlugin>();
    
    await androidImpl?.requestNotificationsPermission();
    
    final hasExact = await androidImpl?.canScheduleExactNotifications() ?? false;
    if (!hasExact) {
      const intent = AndroidIntent(
        action: 'android.settings.REQUEST_SCHEDULE_EXACT_ALARM',
      );
      await intent.launch();
    }
  }

  // ── Notification response ───────────────────────────────────────────────

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

    // Post-announcement (repeat reminder) only for slot 0.
    if (slotIndex == 0) {
      await Future<void>.delayed(const Duration(milliseconds: 800));
      final postText =
          TtsService.buildAnnouncement(title: title, note: note, isRepeat: true);
      await tts.speak(postText);
    }
  }

  @pragma('vm:entry-point')
  static void _backgroundTap(NotificationResponse r) {
    // Background TTS requires a native Android Service — future extension.
  }

  // ── Public API ────────────────────────────────────────────────────────────

  Future<void> scheduleEvent(NextAEvent event) async {
    if (event.reminderMinutes <= 0) return;
    await cancelEvent(event.id);

    final firstAlarm =
        event.start.subtract(Duration(minutes: event.reminderMinutes));
    if (firstAlarm.isBefore(DateTime.now())) return;

    final totalSlots = 1 + event.reminderRepeatCount.clamp(0, 10);
    for (var slot = 0; slot < totalSlots; slot++) {
      final alarmTime = firstAlarm
          .add(Duration(minutes: slot * event.reminderRepeatIntervalMinutes));
      if (alarmTime.isBefore(DateTime.now())) continue;

      final minutesBefore =
          event.reminderMinutes - slot * event.reminderRepeatIntervalMinutes;
      final payload =
          '${event.id}|$slot|$minutesBefore|${event.title}|${event.note ?? ''}';

      await _plugin.zonedSchedule(
        _notifId(event.id, slot),
        event.title,
        _notifBody(event, slot),
        // flutter_local_notifications uses TZDateTime when timezone package is
        // configured. Passing a plain DateTime works via the local-time fallback
        // when using AndroidScheduleMode.exactAllowWhileIdle on Android.
        // ignore: deprecated_member_use
        alarmTime as dynamic,
        _details(event.priority),
        androidScheduleMode: AndroidScheduleMode.exactAllowWhileIdle,
        uiLocalNotificationDateInterpretation:
            UILocalNotificationDateInterpretation.absoluteTime,
        payload: payload,
      );
    }
  }

  Future<void> cancelEvent(String eventId) async {
    const maxSlots = 11;
    for (var slot = 0; slot < maxSlots; slot++) {
      await _plugin.cancel(_notifId(eventId, slot));
    }
  }

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
}
