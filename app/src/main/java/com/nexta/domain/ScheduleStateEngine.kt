package com.nexta.domain

import com.nexta.data.model.Event
import com.nexta.data.model.ScheduleResult
import java.time.LocalDateTime

object ScheduleStateEngine {

    fun calculateResult(
        events: List<Event>,
        now: LocalDateTime
    ): ScheduleResult {

        val current = events
            .filter { event ->
                !now.isBefore(event.startDateTime) &&
                    now.isBefore(event.endDateTime)
            }
            .minByOrNull { it.endDateTime }

        val next = events
            .filter { event ->
                event.startDateTime.isAfter(now)
            }
            .minByOrNull { it.startDateTime }

        return ScheduleResult(
            current = current,
            next = next
        )
    }
}
