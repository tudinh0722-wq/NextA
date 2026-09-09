@file:OptIn(ExperimentalMaterial3Api::class)
package com.nexta.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexta.data.model.Event
import com.nexta.domain.ScheduleState
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM")
private val countdownRed = Color(0xFFD32F2F)
private val importantOrange = Color(0xFFFF9800)
private val veryImportantRed = Color(0xFFF44336)
private val selectedDayBlue = Color(0xFF81D4FA)
private const val DAYS_BEFORE_TODAY = 1825
private const val DAYS_AFTER_TODAY = 1825
private const val TOTAL_DAY_PAGES = DAYS_BEFORE_TODAY + DAYS_AFTER_TODAY + 1
private const val WEEKS_BEFORE_TODAY = 260
private const val WEEKS_AFTER_TODAY = 260
private const val TOTAL_WEEK_PAGES = WEEKS_BEFORE_TODAY + WEEKS_AFTER_TODAY + 1
private const val COUNTDOWN_DAYS_LIMIT = 14L

@Composable
fun MainScreen(events: List<Event>, message: String? = null, onAddEvent: (LocalDate) -> Unit = {}, onEditEvent: (Event) -> Unit = {}, onDeleteEvent: (Event) -> Unit = {}) {
    var selectedEvent by remember { mutableStateOf<Event?>(null) }
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val today = now.toLocalDate(); val initialDayPage = DAYS_BEFORE_TODAY; val initialWeekPage = WEEKS_BEFORE_TODAY
    val dayPagerState = rememberPagerState(initialPage = initialDayPage, pageCount = { TOTAL_DAY_PAGES }); val weekPagerState = rememberPagerState(initialPage = initialWeekPage, pageCount = { TOTAL_WEEK_PAGES })
    val currentDate = today.plusDays((dayPagerState.currentPage - initialDayPage).toLong()); val currentWeekStart = currentDate.with(DayOfWeek.MONDAY); val todayWeekStart = today.with(DayOfWeek.MONDAY)
    val currentWeekOffset = ChronoUnit.WEEKS.between(todayWeekStart, currentWeekStart).toInt(); val scope = rememberCoroutineScope()
    val minSelectableDate = today.minusDays(DAYS_BEFORE_TODAY.toLong()); val maxSelectableDate = today.plusDays(DAYS_AFTER_TODAY.toLong())
    LaunchedEffect(Unit) { while (true) { now = LocalDateTime.now(); delay(30_000) } }
    LaunchedEffect(currentWeekOffset) { val target = (initialWeekPage + currentWeekOffset).coerceIn(0, TOTAL_WEEK_PAGES - 1); if (weekPagerState.currentPage != target) weekPagerState.animateScrollToPage(target) }
    LaunchedEffect(weekPagerState.currentPage) { val weekOffset = weekPagerState.currentPage - initialWeekPage; if (weekOffset != currentWeekOffset) { val weekdayOffset = currentDate.dayOfWeek.value - DayOfWeek.MONDAY.value; val targetDate = todayWeekStart.plusWeeks(weekOffset.toLong()).plusDays(weekdayOffset.toLong()); dayPagerState.animateScrollToPage((initialDayPage + ChronoUnit.DAYS.between(today, targetDate).toInt()).coerceIn(0, TOTAL_DAY_PAGES - 1)) } }
    Scaffold(
        topBar = { TopAppBar(title = { Column { Text("NextA", style = MaterialTheme.typography.titleLarge); Text("Lịch tuần", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } }) },
        bottomBar = { Surface(color = MaterialTheme.colorScheme.surface) { AddEventCard(date = currentDate, onClick = { onAddEvent(currentDate) }, modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 20.dp)) } }
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) { Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) { Text("${currentWeekStart.format(dateFormatter)} — ${currentWeekStart.plusDays(6).format(dateFormatter)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("${currentDate.format(dateFormatter)} • Chọn ngày", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) } }
            WeekDayIndicator(weekPagerState, initialWeekPage, currentDate, today, events)
            HorizontalPager(state = dayPagerState, modifier = Modifier.fillMaxWidth().weight(1f)) { page ->
                val date = today.plusDays((page - initialDayPage).toLong())
                val dayEvents = events.filter { date in it.startDateTime.toLocalDate()..it.endDateTime.toLocalDate() }.sortedBy { it.startDateTime }
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) { DayHeader(date, date == today); dayEvents.forEach { event -> EventCard(event, event.scheduleState(now), now) { selectedEvent = event } }; message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }; Spacer(Modifier.height(16.dp)) }
            }
        }
    }
    if (showDatePicker) { val datePickerState = rememberDatePickerState(initialSelectedDateMillis = currentDate.toEpochMillis(), selectableDates = object : SelectableDates { override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis.toLocalDate() in minSelectableDate..maxSelectableDate }); DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = { TextButton(onClick = { datePickerState.selectedDateMillis?.let { millis -> val selectedDate = millis.toLocalDate(); scope.launch { dayPagerState.animateScrollToPage((initialDayPage + today.until(selectedDate, ChronoUnit.DAYS).toInt()).coerceIn(0, TOTAL_DAY_PAGES - 1)) } }; showDatePicker = false }) { Text("Chọn") } }, dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Hủy") } }) { DatePicker(state = datePickerState) } }
    selectedEvent?.let { event -> AlertDialog(onDismissRequest = { selectedEvent = null }, title = { Text(event.title) }, text = { Text("Bạn muốn làm gì với sự kiện này?") }, confirmButton = { TextButton(onClick = { selectedEvent = null; onEditEvent(event) }) { Text("Sửa") } }, dismissButton = { Row { TextButton(onClick = { selectedEvent = null }) { Text("Hủy") }; TextButton(onClick = { selectedEvent = null; onDeleteEvent(event) }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Xóa") } } }) }
}

@Composable
private fun WeekDayIndicator(weekPagerState: PagerState, initialWeekPage: Int, currentDate: LocalDate, today: LocalDate, events: List<Event>) {
    HorizontalPager(state = weekPagerState, modifier = Modifier.fillMaxWidth()) { page ->
        val weekStart = today.with(DayOfWeek.MONDAY).plusWeeks((page - initialWeekPage).toLong())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
            (0..6).forEach { offset ->
                val date = weekStart.plusDays(offset.toLong())
                val isCurrent = date == currentDate
                val isToday = date == today
                val dayEvents = events.filter { date in it.startDateTime.toLocalDate()..it.endDateTime.toLocalDate() }
                val priority = dayEvents.maxPriority()
                val eventColor = when (priority) { 2 -> veryImportantRed; 1 -> importantOrange; else -> MaterialTheme.colorScheme.primary }
                Column(Modifier.width(44.dp).padding(horizontal = 2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(dayLabel(date.dayOfWeek), style = MaterialTheme.typography.labelMedium, fontWeight = if (isCurrent || isToday) FontWeight.Bold else FontWeight.Normal, color = when { isCurrent -> MaterialTheme.colorScheme.onSurface; isToday -> MaterialTheme.colorScheme.primary; else -> MaterialTheme.colorScheme.onSurfaceVariant })
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(34.dp)) {
                        if (isCurrent) Surface(Modifier.fillMaxSize(), shape = CircleShape, color = Color.Transparent, border = BorderStroke(2.dp, selectedDayBlue)) {}
                        Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = if (isCurrent || isToday) FontWeight.Bold else FontWeight.Normal, color = if (priority != 0) eventColor else if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    }
                    Surface(Modifier.fillMaxWidth().height(if (dayEvents.isNotEmpty()) 4.dp else 2.dp), shape = MaterialTheme.shapes.small, color = if (dayEvents.isNotEmpty()) eventColor else MaterialTheme.colorScheme.surfaceVariant) {}
                }
            }
        }
    }
}

private fun List<Event>.maxPriority(): Int = maxOfOrNull { it.priority.coerceIn(0, 2) } ?: 0

@Composable private fun DayHeader(date: LocalDate, isToday: Boolean) { Column { Text(if (isToday) "HÔM NAY" else "", style = MaterialTheme.typography.labelLarge, color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold); Text("${vietnameseWeekday(date.dayOfWeek)}, ${date.format(dateFormatter)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) } }
@Composable private fun AddEventCard(date: LocalDate, onClick: () -> Unit, modifier: Modifier = Modifier) { Card(onClick = onClick, modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) { Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) { Text("+", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Light); Column(Modifier.weight(1f)) { Text("Thêm sự kiện", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer); Text("Cho ${vietnameseWeekday(date.dayOfWeek).lowercase()}, ${date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .78f)) } } } }
@Composable private fun EventCard(event: Event, state: ScheduleState, now: LocalDateTime, onLongClick: () -> Unit) {
    val active = state == ScheduleState.IN_PROGRESS
    val containerColor = when { active -> MaterialTheme.colorScheme.primaryContainer; state == ScheduleState.PAST -> MaterialTheme.colorScheme.surfaceContainerLow; else -> MaterialTheme.colorScheme.surfaceContainer }
    val priorityBorder = when (event.priority) { 2 -> veryImportantRed; 1 -> importantOrange; else -> null }
    Card(Modifier.fillMaxWidth().combinedClickable(onClick = {}, onLongClick = onLongClick), colors = CardDefaults.cardColors(containerColor = containerColor), border = priorityBorder?.let { BorderStroke(1.dp, it) }) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(Modifier.width(58.dp)) { Text(event.startDateTime.format(timeFormatter), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, fontSize = 15.sp); Text(event.endDateTime.format(timeFormatter), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Column(Modifier.weight(1f)) { Text(event.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 21.sp, maxLines = 2); Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(countdownText(event, state, now), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = countdownRed, fontSize = 13.sp); Spacer(Modifier.weight(1f)); StatusDot(state, active) }; if (event.location.isNotBlank()) Text(event.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 1); if (event.note.isNotBlank()) Text(event.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .72f), fontSize = 11.sp, maxLines = 2) }
        }
    }
}
@Composable private fun StatusDot(state: ScheduleState, active: Boolean) { val label = when (state) { ScheduleState.PAST -> "ĐÃ XONG"; ScheduleState.IN_PROGRESS -> "ĐANG HỌC"; ScheduleState.UPCOMING -> "SẮP TỚI" }; Surface(shape = MaterialTheme.shapes.small, color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest) { Text(label, Modifier.padding(horizontal = 7.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp) } }
private fun countdownText(event: Event, state: ScheduleState, now: LocalDateTime) = when (state) { ScheduleState.IN_PROGRESS -> "Kết thúc sau ${formatDuration(Duration.between(now, event.endDateTime))}"; ScheduleState.UPCOMING -> { val duration = Duration.between(now, event.startDateTime); if (duration.toDays() > COUNTDOWN_DAYS_LIMIT) "" else "Bắt đầu sau ${formatDuration(duration)}" }; ScheduleState.PAST -> "Đã kết thúc" }
private fun formatDuration(duration: Duration): String { val totalMinutes = duration.toMinutes().coerceAtLeast(0); val days = duration.toDays(); val hours = totalMinutes / 60; val minutes = totalMinutes % 60; return when { days >= 1 -> "$days ngày"; hours > 0 && minutes > 0 -> "${hours}h ${minutes}p"; hours > 0 -> "${hours}h"; minutes > 0 -> "${minutes}p"; else -> "<1p" } }
private fun dayLabel(dayOfWeek: DayOfWeek) = when (dayOfWeek) { DayOfWeek.MONDAY -> "T2"; DayOfWeek.TUESDAY -> "T3"; DayOfWeek.WEDNESDAY -> "T4"; DayOfWeek.THURSDAY -> "T5"; DayOfWeek.FRIDAY -> "T6"; DayOfWeek.SATURDAY -> "T7"; DayOfWeek.SUNDAY -> "CN" }
private fun vietnameseWeekday(dayOfWeek: DayOfWeek) = when (dayOfWeek) { DayOfWeek.MONDAY -> "Thứ Hai"; DayOfWeek.TUESDAY -> "Thứ Ba"; DayOfWeek.WEDNESDAY -> "Thứ Tư"; DayOfWeek.THURSDAY -> "Thứ Năm"; DayOfWeek.FRIDAY -> "Thứ Sáu"; DayOfWeek.SATURDAY -> "Thứ Bảy"; DayOfWeek.SUNDAY -> "Chủ Nhật" }
private fun LocalDate.toEpochMillis() = atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
private fun Long.toLocalDate() = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
private fun Event.scheduleState(now: LocalDateTime) = when { now < startDateTime -> ScheduleState.UPCOMING; now >= endDateTime -> ScheduleState.PAST; else -> ScheduleState.IN_PROGRESS }
