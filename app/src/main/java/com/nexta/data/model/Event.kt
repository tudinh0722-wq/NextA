package com.nexta.data.model

import java.time.LocalDateTime

data class Event(
    val id: String,
    val title: String,
    val type: EventType,
    val startDateTime: LocalDateTime,
    val endDateTime: LocalDateTime,
    val location: String,
    val note: String,
    val priority: Int = 0
)
