package com.nexta.data.model

import java.time.LocalDate
import java.time.LocalTime
import java.time.LocalDateTime

data class Event(
    val id: String,
    val title: String,
    val location: String,
    val occurrences: List<LocalDate>,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val notifyBeforeMinutes: Int,
    val note: String
)

fun Event.nextOccurrenceAfter(now: LocalDateTime): LocalDateTime? {
    return occurrences.asSequence()
        .map { it.atTime(startTime) }
        .filter { it > now }
        .minOrNull()
}
