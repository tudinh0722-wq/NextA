import 'package:dynamic_color/dynamic_color.dart';
import 'package:flutter/material.dart';

import 'data/event_database.dart';
import 'domain/event.dart';
import 'presentation/planner_screen.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final database = await EventDatabase.open();
  var events = await database.getAll();
  if (events.isEmpty) {
    events = demoEvents;
    await database.replaceAll(events);
  }
  runApp(NextAApp(database: database, initialEvents: events));
}

class NextAApp extends StatefulWidget {
  const NextAApp({super.key, required this.database, required this.initialEvents});

  final EventDatabase database;
  final List<NextAEvent> initialEvents;

  @override
  State<NextAApp> createState() => _NextAAppState();
}

class _NextAAppState extends State<NextAApp> {
  ThemeMode _themeMode = ThemeMode.system;
  Color _seedColor = const Color(0xFF1A73E8);

  ThemeData _theme(ColorScheme scheme) => ThemeData(
        useMaterial3: true,
        colorScheme: scheme,
        scaffoldBackgroundColor: scheme.surface,
        fontFamily: 'Roboto',
      );

  @override
  Widget build(BuildContext context) {
    return DynamicColorBuilder(
      builder: (lightDynamic, darkDynamic) {
        final light = lightDynamic ?? ColorScheme.fromSeed(seedColor: _seedColor);
        final dark = darkDynamic ?? ColorScheme.fromSeed(
          seedColor: _seedColor,
          brightness: Brightness.dark,
        );
        return MaterialApp(
          title: 'NextA',
          debugShowCheckedModeBanner: false,
          theme: _theme(light),
          darkTheme: _theme(dark),
          themeMode: _themeMode,
          home: PlannerScreen(
            events: widget.initialEvents,
            database: widget.database,
            themeMode: _themeMode,
            seedColor: _seedColor,
            onThemeChanged: (mode) => setState(() => _themeMode = mode),
            onSeedColorChanged: (color) => setState(() => _seedColor = color),
          ),
        );
      },
    );
  }
}

final List<NextAEvent> demoEvents = [
  NextAEvent(id: 'math', title: 'Giải tích', type: EventType.classEvent, start: DateTime(2026, 9, 10, 7, 30), end: DateTime(2026, 9, 10, 9), location: 'P. A204'),
  NextAEvent(id: 'database', title: 'Cơ sở dữ liệu', type: EventType.classEvent, start: DateTime(2026, 9, 10, 9, 15), end: DateTime(2026, 9, 10, 11), location: 'P. B302', priority: 1),
  NextAEvent(id: 'assignment', title: 'Nộp bài lập trình', type: EventType.assignment, start: DateTime(2026, 9, 10, 23), end: DateTime(2026, 9, 10, 23, 30), priority: 2),
  NextAEvent(id: 'english', title: 'English presentation', type: EventType.classEvent, start: DateTime(2026, 9, 11, 8), end: DateTime(2026, 9, 11, 9, 30)),
  NextAEvent(id: 'exam', title: 'Kiểm tra giữa kỳ', type: EventType.exam, start: DateTime(2026, 9, 14, 13, 30), end: DateTime(2026, 9, 14, 15), location: 'Hội trường A', priority: 2),
  NextAEvent(id: 'meeting', title: 'Họp nhóm NextA', type: EventType.meeting, start: DateTime(2026, 9, 16, 18, 30), end: DateTime(2026, 9, 16, 19, 30)),
  NextAEvent(id: 'personal', title: 'Tập gym', type: EventType.personal, start: DateTime(2026, 9, 18, 17), end: DateTime(2026, 9, 18, 18)),
];
