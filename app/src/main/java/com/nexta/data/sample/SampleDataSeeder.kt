package com.nexta.data.sample

import com.nexta.data.repository.EventRepository
import kotlinx.coroutines.flow.first

object SampleDataSeeder {

    suspend fun seedIfEmpty(
        repository: EventRepository
    ) {
        val existingEvents = repository
            .getAllEvents()
            .first()

        if (existingEvents.isEmpty()) {
            SampleEvents.events.forEach { event ->
                repository.saveEvent(event)
            }
            return
        }

        // Keep existing sample events usable when the app was already installed
        // before the widget test notes were added.
        val notesBySampleId = mapOf(
            "sample-002" to "hoàn thành writing trước 9h",
            "sample-004" to "mang theo máy tính",
            "sample-005" to "hoàn thành writing trước 9h"
        )
        existingEvents
            .filter { it.id in notesBySampleId && it.note.isBlank() }
            .forEach { event ->
                repository.saveEvent(
                    event.copy(note = notesBySampleId.getValue(event.id))
                )
            }
    }
}