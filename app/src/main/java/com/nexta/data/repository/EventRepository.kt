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
class EventRepository @Inject constructor(private val eventDao: EventDao) {
    fun getAllEvents(): Flow<List<Event>> = eventDao.getAll().map { entities -> entities.map { it.toDomain() } }
    suspend fun getEventById(id: String): Event? = eventDao.getById(id)?.toDomain()
    suspend fun saveEvent(event: Event) = eventDao.insert(event.toEntity())
    suspend fun saveEvents(events: List<Event>) = eventDao.insertAll(events.map { it.toEntity() })
    suspend fun deleteEvent(id: String) = eventDao.delete(id)
}

private fun EventEntity.toDomain() = Event(id, title, EventType.valueOf(type), LocalDateTime.parse(startDateTime), LocalDateTime.parse(endDateTime), location, note, priority)
private fun Event.toEntity() = EventEntity(id, title, type.name, startDateTime.toString(), endDateTime.toString(), location, note, priority)
