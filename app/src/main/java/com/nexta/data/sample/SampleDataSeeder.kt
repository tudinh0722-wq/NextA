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

        if (existingEvents.isNotEmpty()) {
            return
        }

        SampleEvents.events.forEach { event ->
            repository.saveEvent(event)
        }
    }
}