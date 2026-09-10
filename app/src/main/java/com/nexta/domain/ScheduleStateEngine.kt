package com.nexta.domain

import com.nexta.data.model.Event
import com.nexta.data.model.ScheduleResult
import java.time.LocalDateTime

object ScheduleStateEngine {
    /**
     * Calculates the current and next events based on the provided list of events and the current time.
     * The current event is defined as an event that has started but not yet ended.
     * If multiple events overlap, the one that ends soonest is selected.
     * The next event is defined as the first event that starts after the current time.
     */
    fun calculateResult(events: List<Event>, now: LocalDateTime): ScheduleResult {
        val current = events.filter { it.startDateTime <= now && now < it.endDateTime }
            .minByOrNull { it.endDateTime }

        val next = events.filter { it.startDateTime > now }
            .minByOrNull { it.startDateTime }

        return ScheduleResult(current, next)
    }
}
