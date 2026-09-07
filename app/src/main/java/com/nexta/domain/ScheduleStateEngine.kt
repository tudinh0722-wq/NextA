package com.nexta.domain

import com.nexta.data.model.Event
import com.nexta.data.model.ScheduleResult
import java.time.LocalDateTime

object ScheduleStateEngine {
    fun calculateResult(events: List<Event>, now: LocalDateTime): ScheduleResult {
        val today = now.toLocalDate()
        
        val current = events.filter { event ->
            event.occurrences.contains(today) &&
                    !now.toLocalTime().isBefore(event.startTime) &&
                    now.toLocalTime().isBefore(event.endTime)
        }.minByOrNull { it.endTime }

        val next = events.asSequence()
            .flatMap { event ->
                event.occurrences.map { date ->
                    date.atTime(event.startTime)
                }
            }
            .filter { it.isAfter(now) }
            .minByOrNull { it }
            ?.let { nextStart ->
                events.find { event ->
                    event.occurrences.contains(nextStart.toLocalDate()) &&
                            event.startTime == nextStart.toLocalTime()
                }
            }

        return ScheduleResult(current, next)
    }
}
