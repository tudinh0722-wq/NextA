@file:OptIn(ExperimentalMaterial3Api::class)

package com.nexta.ui

import androidx.compose.foundation.combinedClickable
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val vietnameseLocale = Locale("vi", "VN")
private val dayFormatter = DateTimeFormatter.ofPattern("EEEE", vietnameseLocale)
private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", vietnameseLocale)
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NextA") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Lịch của bạn",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "Theo dõi lịch học và sự kiện sắp tới",
                style = MaterialTheme.typography.bodyMedium
            )

            message?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Button(
                onClick = onAddEvent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ Thêm sự kiện")
            }

            scheduleResult.current?.let { event ->
                ScheduleHighlight("ĐANG DIỄN RA", event)
            }

            scheduleResult.next?.let { event ->
                ScheduleHighlight("TIẾP THEO", event)
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (events.isEmpty()) {
                Text(
                    text = "Chưa có sự kiện nào.",
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                events
                    .sortedBy { it.startDateTime }
                    .groupBy { it.startDateTime.toLocalDate() }
                    .forEach { (date, dayEvents) ->
                        DaySection(
                            date = date,
                            events = dayEvents,
                            now = now,
                            onLongClick = { eventToDelete = it }
                        )
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
                TextButton(
                    onClick = {
                        onDeleteEvent(event)
                        eventToDelete = null
                    }
                ) {
                    Text("Xóa")
                }
            },
            dismissButton = {
                TextButton(onClick = { eventToDelete = null }) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
private fun DaySection(
    date: LocalDate,
    events: List<Event>,
    now: LocalDateTime,
    onLongClick: (Event) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = date.format(dayFormatter).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = date.format(dateFormatter),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        events.forEach { event ->
            EventCard(
                event = event,
                state = event.scheduleState(now),
                onLongClick = { onLongClick(event) }
            )
        }
    }
}

@Composable
private fun ScheduleHighlight(
    label: String,
    event: Event
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = formatTimeRange(event),
                style = MaterialTheme.typography.bodyMedium
            )
            if (event.location.isNotBlank()) {
                Text(
                    text = event.location,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun EventCard(
    event: Event,
    state: ScheduleState,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick
            )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTimeRange(event),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.width(8.dp))
                StatusChip(state)
            }

            Text(
                text = event.title,
                style = MaterialTheme.typography.bodyLarge
            )

            if (event.location.isNotBlank()) {
                Text(
                    text = event.location,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (event.note.isNotBlank()) {
                Text(
                    text = event.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatusChip(state: ScheduleState) {
    val label = when (state) {
        ScheduleState.PAST -> "ĐÃ XONG"
        ScheduleState.IN_PROGRESS -> "ĐANG DIỄN RA"
        ScheduleState.UPCOMING -> "SẮP TỚI"
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

private fun Event.scheduleState(now: LocalDateTime): ScheduleState = when {
    now < startDateTime -> ScheduleState.UPCOMING
    now >= endDateTime -> ScheduleState.PAST
    else -> ScheduleState.IN_PROGRESS
}

private fun formatTimeRange(event: Event): String =
    "${event.startDateTime.format(timeFormatter)} – ${event.endDateTime.format(timeFormatter)}"
