import 'package:flutter/material.dart';

import 'domain/event.dart';
import 'presentation/planner_screen.dart';

void main() {
  runApp(const NextAApp());
}

class NextAApp extends StatelessWidget {
  const NextAApp({super.key});

  @override
  Widget build(BuildContext context) {
    final colorScheme = ColorScheme.fromSeed(seedColor: const Color(0xFF1A73E8));
    return MaterialApp(
      title: 'NextA',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: colorScheme,
        scaffoldBackgroundColor: colorScheme.surface,
        fontFamily: 'Roboto',
      ),
      home: PlannerScreen(events: demoEvents),
    );
  }
}

final List<NextAEvent> demoEvents = [
  NextAEvent(
    id: 'math',
    title: 'Giải tích',
    type: EventType.classEvent,
    start: DateTime(2026, 9, 10, 7, 30),
    end: DateTime(2026, 9, 10, 9, 0),
    location: 'P. A204',
  ),
  NextAEvent(
    id: 'database',
    title: 'Cơ sở dữ liệu',
    type: EventType.classEvent,
    start: DateTime(2026, 9, 10, 9, 15),
    end: DateTime(2026, 9, 10, 11, 0),
    location: 'P. B302',
    priority: 1,
  ),
  NextAEvent(
    id: 'assignment',
    title: 'Nộp bài lập trình',
    type: EventType.assignment,
    start: DateTime(2026, 9, 10, 23, 0),
    end: DateTime(2026, 9, 10, 23, 30),
    priority: 2,
  ),
  NextAEvent(
    id: 'english',
    title: 'English presentation',
    type: EventType.classEvent,
    start: DateTime(2026, 9, 11, 8, 0),
    end: DateTime(2026, 9, 11, 9, 30),
  ),
  NextAEvent(
    id: 'exam',
    title: 'Kiểm tra giữa kỳ',
    type: EventType.exam,
    start: DateTime(2026, 9, 14, 13, 30),
    end: DateTime(2026, 9, 14, 15, 0),
    location: 'Hội trường A',
    priority: 2,
  ),
  NextAEvent(
    id: 'meeting',
    title: 'Họp nhóm NextA',
    type: EventType.meeting,
    start: DateTime(2026, 9, 16, 18, 30),
    end: DateTime(2026, 9, 16, 19, 30),
  ),
  NextAEvent(
    id: 'personal',
    title: 'Tập gym',
    type: EventType.personal,
    start: DateTime(2026, 9, 18, 17, 0),
    end: DateTime(2026, 9, 18, 18, 0),
  ),
];
