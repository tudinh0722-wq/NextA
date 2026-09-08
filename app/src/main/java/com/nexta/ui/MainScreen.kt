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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexta.data.model.Event
import com.nexta.domain.ScheduleState
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.launch

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM")
private val countdownRed = Color(0xFFD32F2F)

private const val DAYS_BEFORE_TODAY = 1825
private const val DAYS_AFTER_TODAY = 1825
private const val TOTAL_DAY_PAGES = DAYS_BEFORE_TODAY + DAYS_AFTER_TODAY + 1
private const val WEEKS_BEFORE_TODAY = 260
private const val WEEKS_AFTER_TODAY = 260
private const val TOTAL_WEEK_PAGES = WEEKS_BEFORE_TODAY + WEEKS_AFTER_TODAY + 1

@Composable
fun MainScreen(
    events: List<Event>,
    message: String? = null,
    onAddEvent: () -> Unit = {},
    onDeleteEvent: (Event) -> Unit = {}
) {
    var eventToDelete by remember { mutableStateOf<Event?>(null) }
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val today = now.toLocalDate()
    val initialDayPage = DAYS_BEFORE_TODAY
    val initialWeekPage = WEEKS_BEFORE_TODAY
    val dayPagerState = rememberPagerState(
        initialPage = initialDayPage,
        pageCount = { TOTAL_DAY_PAGES }
    )
    val weekPagerState = rememberPagerState(
        initialPage = initialWeekPage,
        pageCount = { TOTAL_WEEK_PAGES }
    )
    val currentDate = today.plusDays((dayPagerState.currentPage - initialDayPage).toLong())
    val currentWeekStart = currentDate.with(DayOfWeek.MONDAY)
    val todayWeekStart = today.with(DayOfWeek.MONDAY)
    val currentWeekOffset = ChronoUnit.WEEKS.between(todayWeekStart, currentWeekStart).toInt()
    val eventsByDay = remember(events) { events.groupBy { it.startDateTime.toLocalDate() } }
    val coroutineScope = rememberCoroutineScope()
    val minSelectableDate = today.minusDays(DAYS_BEFORE_TODAY.toLong())
    val maxSelectableDate = today.plusDays(DAYS_AFTER_TODAY.toLong())

    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            kotlinx.coroutines.delay(30_000)
        }
    }

    // Day pager drives the week strip when the user swipes the schedule.
    LaunchedEffect(currentWeekOffset) {
        val targetPage = (initialWeekPage + currentWeekOffset).coerceIn(0, TOTAL_WEEK_PAGES - 1)
        if (weekPagerState.currentPage != targetPage) {
            weekPagerState.animateScrollToPage(targetPage)
        }
    }

    // Swiping the week strip moves the schedule by a whole week,
    // preserving the currently selected weekday.
    LaunchedEffect(weekPagerState.currentPage) {
        val weekOffset = weekPagerState.currentPage - initialWeekPage
        if (weekOffset != currentWeekOffset) {
            val weekdayOffset = currentDate.dayOfWeek.value - DayOfWeek.MONDAY.value
            val targetDate = todayWeekStart.plusWeeks(weekOffset.toLong()).plusDays(weekdayOffset.toLong())
            val targetDayPage = initialDayPage + ChronoUnit.DAYS.between(today, targetDate).toInt()
            dayPagerState.animateScrollToPage(targetDayPage.coerceIn(0, TOTAL_DAY_PAGES - 1))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("NextA", style = MaterialTheme.typography.titleLarge)
                        Text("Lịch tuần", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddEvent,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) { Text("+", style = MaterialTheme.typography.headlineSmall) }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        "${currentWeekStart.format(dateFormatter)} — ${currentWeekStart.plusDays(6).format(dateFormatter)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${currentDate.format(dateFormatter)} • Chọn ngày",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            WeekDayIndicator(
                weekPagerState = weekPagerState,
                initialWeekPage = initialWeekPage,
                currentDate = currentDate,
                today = today
            )

            HorizontalPager(
                state = dayPagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                val date = today.plusDays((page - initialDayPage).toLong())
                val dayEvents = eventsByDay[date].orEmpty().sortedBy { it.startDateTime }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DayHeader(date = date, isToday = date == today)
                    if (dayEvents.isEmpty()) {
                        EmptyDayState(isToday = date == today)
                    } else {
                        dayEvents.forEach { event ->
                            EventCard(event = event, state = event.scheduleState(now), now = now) {
                                eventToDelete = event
                            }
                        }
                    }
                    message?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(72.dp))
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = currentDate.toEpochMillis(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val date = utcTimeMillis.toLocalDate()
                    return date in minSelectableDate..maxSelectableDate
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selectedDate = millis.toLocalDate()
                        val page = initialDayPage + today.until(selectedDate, ChronoUnit.DAYS).toInt()
                        coroutineScope.launch {
                            dayPagerState.animateScrollToPage(page.coerceIn(0, TOTAL_DAY_PAGES - 1))
                        }
                    }
                    showDatePicker = false
                }) { Text("Chọn") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Hủy") } }
        ) { DatePicker(state = datePickerState) }
    }

    eventToDelete?.let { event ->
        AlertDialog(
            onDismissRequest = { eventToDelete = null },
            title = { Text("Xóa sự kiện?") },
            text = { Text("Bạn có chắc muốn xóa \"${event.title}\" không?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteEvent(event)
                    eventToDelete = null
                }) { Text("Xóa") }
            },
            dismissButton = { TextButton(onClick = { eventToDelete = null }) { Text("Hủy") } }
        )
    }
}

@Composable
private fun WeekDayIndicator(
    weekPagerState: androidx.compose.foundation.pager.PagerState,
    initialWeekPage: Int,
    currentDate: LocalDate,
    today: LocalDate
) {
    HorizontalPager(
        state = weekPagerState,
        modifier = Modifier.fillMaxWidth()
    ) { page ->
        val weekStart = today.with(DayOfWeek.MONDAY)
            .plusWeeks((page - initialWeekPage).toLong())
        val weekDays = remember(weekStart) { (0..6).map { weekStart.plusDays(it.toLong()) } }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom
        ) {
            weekDays.forEach { date ->
                val isCurrent = date == currentDate
                val isToday = date == today
                Column(
                    modifier = Modifier
                        .width(44.dp)
                        .padding(horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        dayLabel(date.dayOfWeek),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isCurrent || isToday) FontWeight.Bold else FontWeight.Normal,
                        color = when {
                            isCurrent -> MaterialTheme.colorScheme.onSurface
                            isToday -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Text(
                        date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isCurrent || isToday) FontWeight.Bold else FontWeight.Normal,
                        color = when {
                            isCurrent -> MaterialTheme.colorScheme.onSurface
                            isToday -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Surface(
                        modifier = Modifier.size(if (isCurrent) 4.dp else 2.dp),
                        shape = MaterialTheme.shapes.small,
                        color = when {
                            isCurrent || isToday -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {}
                }
            }
        }
    }
}

@Composable
private fun DayHeader(date: LocalDate, isToday: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            if (isToday) "HÔM NAY" else vietnameseWeekday(date.dayOfWeek),
            style = MaterialTheme.typography.labelLarge,
            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        Text(
            "${vietnameseWeekday(date.dayOfWeek)}, ${date.format(dateFormatter)}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
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
            Text("Nhấn + để thêm sự kiện", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EventCard(event: Event, state: ScheduleState, now: LocalDateTime, onLongClick: () -> Unit) {
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
                .padding(horizontal = 14.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.width(58.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    event.startDateTime.format(timeFormatter),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    event.endDateTime.format(timeFormatter),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
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
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    lineHeight = 21.sp,
                    maxLines = 2
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        countdownText(event, state, now),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = countdownRed,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.weight(1f))
                    StatusDot(state = state, active = active)
                }
                if (event.location.isNotBlank()) {
                    Text(
                        event.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
                if (event.note.isNotBlank()) {
                    Text(
                        event.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        maxLines = 2
                    )
                }
            }
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
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
    }
}

private fun countdownText(event: Event, state: ScheduleState, now: LocalDateTime): String = when (state) {
    ScheduleState.IN_PROGRESS -> "Kết thúc sau ${formatDuration(Duration.between(now, event.endDateTime))}"
    ScheduleState.UPCOMING -> "Bắt đầu sau ${formatDuration(Duration.between(now, event.startDateTime))}"
    ScheduleState.PAST -> "Đã kết thúc"
}

private fun formatDuration(duration: Duration): String {
    val totalMinutes = duration.toMinutes().coerceAtLeast(0)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}p"
        hours > 0 -> "${hours}h"
        minutes > 0 -> "${minutes}p"
        else -> "<1p"
    }
}

private fun dayLabel(dayOfWeek: DayOfWeek): String = when (dayOfWeek) {
    DayOfWeek.MONDAY -> "T2"
    DayOfWeek.TUESDAY -> "T3"
    DayOfWeek.WEDNESDAY -> "T4"
    DayOfWeek.THURSDAY -> "T5"
    DayOfWeek.FRIDAY -> "T6"
    DayOfWeek.SATURDAY -> "T7"
    DayOfWeek.SUNDAY -> "CN"
}

private fun vietnameseWeekday(dayOfWeek: DayOfWeek): String = when (dayOfWeek) {
    DayOfWeek.MONDAY -> "Thứ Hai"
    DayOfWeek.TUESDAY -> "Thứ Ba"
    DayOfWeek.WEDNESDAY -> "Thứ Tư"
    DayOfWeek.THURSDAY -> "Thứ Năm"
    DayOfWeek.FRIDAY -> "Thứ Sáu"
    DayOfWeek.SATURDAY -> "Thứ Bảy"
    DayOfWeek.SUNDAY -> "Chủ Nhật"
}

private fun LocalDate.toEpochMillis(): Long = atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

private fun Event.scheduleState(now: LocalDateTime): ScheduleState = when {
    now < startDateTime -> ScheduleState.UPCOMING
    now >= endDateTime -> ScheduleState.PAST
    else -> ScheduleState.IN_PROGRESS
}
