package com.nexta.domain

import com.nexta.data.model.Event
import com.nexta.data.model.ScheduleResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ScheduleStateEngineTest {

    @Test
    fun `calculateResult - no events`() {
        val now = LocalDateTime.of(2023, 10, 27, 10, 0)
        val result = ScheduleStateEngine.calculateResult(emptyList(), now)
        assertNull(result.current)
        assertNull(result.next)
    }

    @Test
    fun `calculateResult - current event exists`() {
        val today = LocalDate.of(2023, 10, 27)
        val now = LocalDateTime.of(today, LocalTime.of(10, 30))
        
        val event = Event(
            id = "1",
            title = "Class",
            location = "Room 101",
            occurrences = listOf(today),
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(11, 0),
            notifyBeforeMinutes = 10,
            note = ""
        )
        
        val result = ScheduleStateEngine.calculateResult(listOf(event), now)
        assertEquals(event, result.current)
    }

    @Test
    fun `calculateResult - next event exists`() {
        val today = LocalDate.of(2023, 10, 27)
        val now = LocalDateTime.of(today, LocalTime.of(9, 0))
        
        val event = Event(
            id = "1",
            title = "Class",
            location = "Room 101",
            occurrences = listOf(today),
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(11, 0),
            notifyBeforeMinutes = 10,
            note = ""
        )
        
        val result = ScheduleStateEngine.calculateResult(listOf(event), now)
        assertNull(result.current)
        assertEquals(event, result.next)
    }

    @Test
    fun `calculateResult - pick smallest endTime for current`() {
        val today = LocalDate.of(2023, 10, 27)
        val now = LocalDateTime.of(today, LocalTime.of(10, 30))
        
        val event1 = Event(
            id = "1",
            title = "Class 1",
            location = "Room 101",
            occurrences = listOf(today),
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(12, 0),
            notifyBeforeMinutes = 10,
            note = ""
        )
        
        val event2 = Event(
            id = "2",
            title = "Class 2",
            location = "Room 102",
            occurrences = listOf(today),
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(11, 0),
            notifyBeforeMinutes = 10,
            note = ""
        )
        
        val result = ScheduleStateEngine.calculateResult(listOf(event1, event2), now)
        assertEquals(event2, result.current)
    }

    @Test
    fun `calculateResult - boundary cases`() {
        val today = LocalDate.of(2023, 10, 27)
        val event = Event(
            id = "1",
            title = "Class",
            location = "Room 101",
            occurrences = listOf(today),
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(11, 0),
            notifyBeforeMinutes = 10,
            note = ""
        )

        // exactly at startTime
        val atStart = LocalDateTime.of(today, LocalTime.of(10, 0))
        assertEquals(event, ScheduleStateEngine.calculateResult(listOf(event), atStart).current)

        // exactly at endTime
        val atEnd = LocalDateTime.of(today, LocalTime.of(11, 0))
        assertNull(ScheduleStateEngine.calculateResult(listOf(event), atEnd).current)
    }

    @Test
    fun `calculateResult - current and next`() {
        val today = LocalDate.of(2023, 10, 27)
        val now = LocalDateTime.of(today, LocalTime.of(10, 30))
        
        val current = Event(
            id = "1",
            title = "Current",
            location = "Room 101",
            occurrences = listOf(today),
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(11, 0),
            notifyBeforeMinutes = 10,
            note = ""
        )
        
        val next = Event(
            id = "2",
            title = "Next",
            location = "Room 102",
            occurrences = listOf(today),
            startTime = LocalTime.of(12, 0),
            endTime = LocalTime.of(13, 0),
            notifyBeforeMinutes = 10,
            note = ""
        )
        
        val result = ScheduleStateEngine.calculateResult(listOf(current, next), now)
        assertEquals(current, result.current)
        assertEquals(next, result.next)
    }
}
