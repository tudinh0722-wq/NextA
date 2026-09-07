package com.nexta.data.repository

import com.nexta.data.local.EventDao
import com.nexta.data.local.EventEntity
import com.nexta.data.model.Event
import com.nexta.data.model.EventType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepository @Inject constructor(
    private val eventDao: EventDao
) {

    fun getAllEvents(): Flow<List<Event>> {
        return eventDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getEventById(id: String): Event? {
        return eventDao.getById(id)?.toDomain()
    }

    suspend fun saveEvent(event: Event) {
        eventDao.insert(event.toEntity())
    }

    suspend fun deleteEvent(id: String) {
        eventDao.delete(id)
    }
}

private fun EventEntity.toDomain(): Event {
    return Event(
        id = id,
        title = title,
        type = EventType.valueOf(type),
        startDateTime = LocalDateTime.parse(startDateTime),
        endDateTime = LocalDateTime.parse(endDateTime),
        location = location,
        note = note,
        priority = priority
    )
}

private fun Event.toEntity(): EventEntity {
    return EventEntity(
        id = id,
        title = title,
        type = type.name,
        startDateTime = startDateTime.toString(),
        endDateTime = endDateTime.toString(),
        location = location,
        note = note,
        priority = priority
    )
}
