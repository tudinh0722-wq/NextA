@file:OptIn(ExperimentalMaterial3Api::class)

package com.nexta.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexta.data.model.Event
import com.nexta.data.model.ScheduleResult
import com.nexta.domain.ScheduleState
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM")

@Composable
fun MainScreen(
    events: List<Event>,
    scheduleResult: ScheduleResult = ScheduleResult(null, null),
    message: String? = null,
    onAddEvent: () -> Unit = {},
    onDeleteEvent: (Event) -> Unit = {}
) {
    var eventToDelete by remember { mutableStateOf<Event?>(null) }
    val now = LocalDateTime.now()
    val today = now.toLocalDate()
    val weekStart = today.with(DayOfWeek.MONDAY)
    val weekDays = (0..6).map { weekStart.plusDays(it.toLong()) }
    val eventsByDay = events.groupBy { it.startDateTime.toLocalDate() }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("NextA") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text("TUẦN NÀY", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(
                        "${weekStart.format(dateFormatter)} — ${weekStart.plusDays(6).format(dateFormatter)}",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
                Text("${events.size} mục", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            message?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }

            scheduleResult.current?.let { event ->
                FocusStrip("LIVE", event, MaterialTheme.colorScheme.primaryContainer)
            } ?: scheduleResult.next?.let { event ->
                FocusStrip("NEXT", event, MaterialTheme.colorScheme.secondaryContainer)
            }

            WeeklyBoard(
                weekDays = weekDays,
                eventsByDay = eventsByDay,
                today = today,
                now = now,
                onLongClick = { eventToDelete = it }
            )

            Button(
                onClick = onAddEvent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ Thêm vào tuần")
            }

            val notes = events
                .filter { it.note.isNotBlank() }
                .sortedBy { it.startDateTime }
                .take(6)

            if (notes.isNotEmpty()) {
                Text("GHI CHÚ / VIỆC CẦN NHỚ", style = MaterialTheme.typography.titleMedium)
                notes.forEach { event ->
                    NoteRow(event)
                }
            }
        }
    }

    eventToDelete?.let { event ->
        AlertDialog(
            onDismissRequest = { eventToDelete = null },
            title = { Text("Xóa sự kiện?") },
            text = { Text("Bạn có chắc muốn xóa \"${event.title}\" không?") },
            confirmButton = {
                TextButton(onClick = { onDeleteEvent(event); eventToDelete = null }) { Text("Xóa") }
            },
            dismissButton = {
                TextButton(onClick = { eventToDelete = null }) { Text("Hủy") }
            }
        )
    }
}

@Composable
private fun WeeklyBoard(
    weekDays: List<LocalDate>,
    eventsByDay: Map<LocalDate, List<Event>>,
    today: LocalDate,
    now: LocalDateTime,
    onLongClick: (Event) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        weekDays.forEach { date ->
            DayColumn(
                date = date,
                events = eventsByDay[date].orEmpty().sortedBy { it.startDateTime },
                isToday = date == today,
                now = now,
                onLongClick = onLongClick
            )
        }
    }
}

@Composable
private fun DayColumn(
    date: LocalDate,
    events: List<Event>,
    isToday: Boolean,
    now: LocalDateTime,
    onLongClick: (Event) -> Unit
) {
    val dayLabel = when (date.dayOfWeek) {
        DayOfWeek.MONDAY -> "T2"
        DayOfWeek.TUESDAY -> "T3"
        DayOfWeek.WEDNESDAY -> "T4"
        DayOfWeek.THURSDAY -> "T5"
        DayOfWeek.FRIDAY -> "T6"
        DayOfWeek.SATURDAY -> "T7"
        DayOfWeek.SUNDAY -> "CN"
    }

    Card(
        modifier = Modifier.width(142.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(dayLabel, style = MaterialTheme.typography.titleMedium, color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(6.dp))
                Text(date.format(dateFormatter), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (events.isEmpty()) {
                Text("Trống", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                events.forEach { event ->
                    WeekEvent(
                        event = event,
                        state = event.scheduleState(now),
                        onLongClick = { onLongClick(event) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekEvent(
    event: Event,
    state: ScheduleState,
    onLongClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onLongClick),
        shape = MaterialTheme.shapes.medium,
        color = if (state == ScheduleState.IN_PROGRESS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Column(modifier = Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                "${event.startDateTime.format(timeFormatter)}–${event.endDateTime.format(timeFormatter)}",
                style = MaterialTheme.typography.labelSmall,
                color = if (state == ScheduleState.IN_PROGRESS) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                event.title,
                maxLines = 3,
                style = MaterialTheme.typography.bodyMedium,
                color = if (state == ScheduleState.IN_PROGRESS) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
            if (event.location.isNotBlank()) {
                Text(event.location, maxLines = 1, style = MaterialTheme.typography.labelSmall, color = if (state == ScheduleState.IN_PROGRESS) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun FocusStrip(label: String, event: Event, color: androidx.compose.ui.graphics.Color) {
    Card(colors = CardDefaults.cardColors(containerColor = color), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(event.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text("${event.startDateTime.format(timeFormatter)} – ${event.endDateTime.format(timeFormatter)}", style = MaterialTheme.typography.bodySmall)
            }
            if (event.location.isNotBlank()) Text(event.location, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

@Composable
private fun NoteRow(event: Event) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("○", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(event.note, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                Text("${event.title} · ${event.startDateTime.format(timeFormatter)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun Event.scheduleState(now: LocalDateTime): ScheduleState = when {
    now < startDateTime -> ScheduleState.UPCOMING
    now >= endDateTime -> ScheduleState.PAST
    else -> ScheduleState.IN_PROGRESS
}
