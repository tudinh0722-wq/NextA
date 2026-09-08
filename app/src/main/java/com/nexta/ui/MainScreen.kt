@file:OptIn(ExperimentalMaterial3Api::class)

package com.nexta.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexta.data.model.Event
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
    var selectedDate by remember(today) { mutableStateOf(today) }
    val selectedEvents = eventsByDay[selectedDate].orEmpty().sortedBy { it.startDateTime }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("NextA", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Lịch tuần",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddEvent,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Text("+", style = MaterialTheme.typography.headlineSmall)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "${weekStart.format(dateFormatter)} — ${weekStart.plusDays(6).format(dateFormatter)}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Chọn một ngày để xem lịch",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            WeeklyDayStrip(
                weekDays = weekDays,
                eventsByDay = eventsByDay,
                today = today,
                selectedDate = selectedDate,
                onDaySelected = { selectedDate = it }
            )

            SelectedDayHeader(
                date = selectedDate,
                eventCount = selectedEvents.size,
                isToday = selectedDate == today
            )

            if (selectedEvents.isEmpty()) {
                EmptyDayState(isToday = selectedDate == today)
            } else {
                selectedEvents.forEach { event ->
                    EventCard(
                        event = event,
                        state = event.scheduleState(now),
                        onLongClick = { eventToDelete = event }
                    )
                }
            }

            message?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(72.dp))
        }
    }

    eventToDelete?.let { event ->
        AlertDialog(
            onDismissRequest = { eventToDelete = null },
            title = { Text("Xóa sự kiện?") },
            text = { Text("Bạn có chắc muốn xóa \"${event.title}\" không?") },
            confirmButton = {
                TextButton(onClick = { onDeleteEvent(event); eventToDelete = null }) {
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
private fun WeeklyDayStrip(
    weekDays: List<LocalDate>,
    eventsByDay: Map<LocalDate, List<Event>>,
    today: LocalDate,
    selectedDate: LocalDate,
    onDaySelected: (LocalDate) -> Unit
) {
    val maxWeight = weekDays.maxOfOrNull { date ->
        dayWeight(eventsByDay[date].orEmpty())
    }?.coerceAtLeast(1) ?: 1

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        weekDays.forEach { date ->
            val events = eventsByDay[date].orEmpty()
            DayCell(
                date = date,
                eventWeight = dayWeight(events),
                maxWeight = maxWeight,
                eventCount = events.size,
                isToday = date == today,
                isSelected = date == selectedDate,
                onClick = { onDaySelected(date) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    eventWeight: Int,
    maxWeight: Int,
    eventCount: Int,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
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
    val load = if (eventWeight == 0) 0f else (eventWeight.toFloat() / maxWeight).coerceIn(0f, 1f)
    val baseColor = MaterialTheme.colorScheme.primaryContainer
    val background = when {
        isSelected -> MaterialTheme.colorScheme.primary
        eventWeight == 0 -> MaterialTheme.colorScheme.surfaceContainerLow
        else -> baseColor.copy(alpha = 0.35f + load * 0.55f)
    }
    val contentColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = modifier
            .height(76.dp)
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(onClick = onClick, onLongClick = null),
        color = background,
        border = if (isToday && !isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                dayLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Text(
                date.dayOfMonth.toString().padStart(2, '0'),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            if (eventCount > 0) {
                Text(
                    "$eventCount lịch",
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.78f),
                    maxLines = 1
                )
            } else {
                Text(
                    "—",
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.55f)
                )
            }
        }
    }
}

@Composable
private fun SelectedDayHeader(
    date: LocalDate,
    eventCount: Int,
    isToday: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                if (isToday) "HÔM NAY" else "LỊCH NGÀY",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${vietnameseWeekday(date.dayOfWeek)} · ${date.format(dateFormatter)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            "$eventCount lịch",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyDayState(isToday: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                if (isToday) "Hôm nay chưa có lịch" else "Ngày này chưa có lịch",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Nhấn + để thêm sự kiện",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EventCard(
    event: Event,
    state: ScheduleState,
    onLongClick: () -> Unit
) {
    val active = state == ScheduleState.IN_PROGRESS
    val containerColor = when {
        active -> MaterialTheme.colorScheme.primaryContainer
        state == ScheduleState.PAST -> MaterialTheme.colorScheme.surfaceContainerLow
        else -> MaterialTheme.colorScheme.surfaceContainer
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.width(64.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    event.startDateTime.format(timeFormatter),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    event.endDateTime.format(timeFormatter),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    event.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (event.location.isNotBlank()) {
                    Text(
                        event.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                if (event.note.isNotBlank()) {
                    Text(
                        event.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }

            StatusDot(state = state, active = active)
        }
    }
}

@Composable
private fun StatusDot(state: ScheduleState, active: Boolean) {
    val label = when (state) {
        ScheduleState.PAST -> "ĐÃ XONG"
        ScheduleState.IN_PROGRESS -> "ĐANG HỌC"
        ScheduleState.UPCOMING -> "SẮP TỚI"
    }
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp
        )
    }
}

private fun dayWeight(events: List<Event>): Int =
    events.sumOf { it.priority.coerceAtLeast(0) + 1 }

private fun vietnameseWeekday(dayOfWeek: DayOfWeek): String = when (dayOfWeek) {
    DayOfWeek.MONDAY -> "Thứ Hai"
    DayOfWeek.TUESDAY -> "Thứ Ba"
    DayOfWeek.WEDNESDAY -> "Thứ Tư"
    DayOfWeek.THURSDAY -> "Thứ Năm"
    DayOfWeek.FRIDAY -> "Thứ Sáu"
    DayOfWeek.SATURDAY -> "Thứ Bảy"
    DayOfWeek.SUNDAY -> "Chủ Nhật"
}

private fun Event.scheduleState(now: LocalDateTime): ScheduleState = when {
    now < startDateTime -> ScheduleState.UPCOMING
    now >= endDateTime -> ScheduleState.PAST
    else -> ScheduleState.IN_PROGRESS
}
