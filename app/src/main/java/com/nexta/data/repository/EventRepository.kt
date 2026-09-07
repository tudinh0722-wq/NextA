package com.nexta.data.repository

import com.nexta.data.local.EventDao
import com.nexta.data.local.EventEntity
import com.nexta.data.model.Event
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepository @Inject constructor(
    private val eventDao: EventDao,
    private val gson: Gson
) {
    fun getAllEvents(): Flow<List<Event>> {
        return eventDao.getAll().map { entities ->
            entities.map { it.toDomain(gson) }
        }
    }

    suspend fun getEventById(id: String): Event? {
        return eventDao.getById(id)?.toDomain(gson)
    }

    suspend fun saveEvent(event: Event) {
        eventDao.insert(event.toEntity(gson))
    }

    suspend fun deleteEvent(id: String) {
        eventDao.delete(id)
    }
}

fun EventEntity.toDomain(gson: Gson): Event {
    val type = object : TypeToken<List<LocalDate>>() {}.type
    val occurrences: List<LocalDate> = gson.fromJson(occurrencesJson, type)
    return Event(
        id = id,
        title = title,
        location = location,
        occurrences = occurrences,
        startTime = LocalTime.parse(startTime),
        endTime = LocalTime.parse(endTime),
        notifyBeforeMinutes = notifyBeforeMinutes,
        note = note
    )
}

fun Event.toEntity(gson: Gson): EventEntity {
    return EventEntity(
        id = id,
        title = title,
        location = location,
        occurrencesJson = gson.toJson(occurrences),
        startTime = startTime.toString(),
        endTime = endTime.toString(),
        notifyBeforeMinutes = notifyBeforeMinutes,
        note = note
    )
}
