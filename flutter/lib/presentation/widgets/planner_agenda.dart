import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../application/countdown_policy.dart';
import '../../domain/event.dart';
import 'planner_calendar.dart';

class PlannerAgenda extends StatelessWidget {
  const PlannerAgenda({
    super.key,
    required this.header,
    required this.events,
    required this.policy,
    required this.onEventTap,
    required this.onEmptyTap,
  });

  final String header;
  final List<NextAEvent> events;
  final CountdownPolicy policy;
  final ValueChanged<NextAEvent> onEventTap;
  final VoidCallback onEmptyTap;

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 0, 16, 82),
      children: [
        SizedBox(
          height: 42,
          child: Row(
            children: [
              Text(
                header,
                style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w700),
              ),
              const Spacer(),
              const Icon(Icons.more_horiz_rounded, size: 22),
            ],
          ),
        ),
        if (events.isEmpty)
          InkWell(
            onTap: onEmptyTap,
            borderRadius: BorderRadius.circular(16),
            child: Padding(
              padding: const EdgeInsets.only(top: 28),
              child: Column(
                children: [
                  Icon(
                    Icons.event_available_outlined,
                    size: 34,
                    color: Theme.of(context).colorScheme.primary,
                  ),
                  const SizedBox(height: 10),
                  const Text('Không có sự kiện'),
                ],
              ),
            ),
          )
        else
          ...events.map((event) => PlannerEventRow(
                event: event,
                policy: policy,
                onTap: () => onEventTap(event),
              )),
      ],
    );
  }
}

class PlannerEventRow extends StatelessWidget {
  const PlannerEventRow({
    super.key,
    required this.event,
    required this.policy,
    required this.onTap,
  });

  final NextAEvent event;
  final CountdownPolicy policy;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final now = DateTime.now();
    final ongoing = !now.isBefore(event.start) && now.isBefore(event.end);
    final remaining = ongoing ? event.end.difference(now) : policy.remaining(event, now);

    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(12),
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 9),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              width: 3,
              height: 48,
              margin: const EdgeInsets.only(right: 12, top: 3),
              decoration: BoxDecoration(
                color: eventColor(context, event),
                borderRadius: BorderRadius.circular(3),
              ),
            ),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    event.title,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w700),
                  ),
                  if (event.location != null)
                    Padding(
                      padding: const EdgeInsets.only(top: 3),
                      child: Text(event.location!),
                    ),
                  if (event.note != null && event.note!.trim().isNotEmpty)
                    Padding(
                      padding: const EdgeInsets.only(top: 3),
                      child: Text(
                        event.note!,
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                ],
              ),
            ),
            const SizedBox(width: 10),
            SizedBox(
              width: 106,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  Text(
                    '${DateFormat.Hm().format(event.start)} – ${DateFormat.Hm().format(event.end)}',
                    textAlign: TextAlign.end,
                    style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w700),
                  ),
                  if (remaining != null) ...[
                    const SizedBox(height: 5),
                    Text(
                      ongoing ? 'Kết thúc sau' : 'Bắt đầu sau',
                      style: TextStyle(
                        fontSize: 10,
                        fontWeight: FontWeight.w600,
                        color: scheme.primary,
                      ),
                    ),
                    Text(
                      policy.format(remaining),
                      style: TextStyle(
                        fontSize: 13,
                        fontWeight: FontWeight.w800,
                        color: scheme.primary,
                      ),
                    ),
                  ],
                ],
              ),
            ),
            if (event.priority > 0)
              Padding(
                padding: const EdgeInsets.only(left: 5, top: 3),
                child: Icon(
                  Icons.priority_high_rounded,
                  size: 16,
                  color: scheme.error,
                ),
              ),
          ],
        ),
      ),
    );
  }
}
