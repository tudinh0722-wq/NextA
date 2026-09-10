@file:OptIn(ExperimentalMaterial3Api::class)

package com.nexta.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexta.R
import com.nexta.data.model.Event
import com.nexta.data.model.EventType
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.delay

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val viLocale = Locale("vi", "VN")

internal data class CalendarState(
    val selectedDate: LocalDate,
    val currentMonth: YearMonth,
    val monthExpanded: Boolean,
    val showSearch: Boolean,
    val todayDate: LocalDate
) {
    companion object {
        fun initial(date: LocalDate): CalendarState = CalendarState(
            selectedDate = date,
            currentMonth = YearMonth.from(date),
            monthExpanded = true,
            showSearch = false,
            todayDate = LocalDate.now()
        )
    }

    fun selectDate(date: LocalDate): CalendarState = copy(
        selectedDate = date,
        currentMonth = YearMonth.from(date)
    )

    fun moveHorizontal(delta: Int): CalendarState {
        return if (monthExpanded) {
            copy(
                currentMonth = currentMonth.plusMonths(delta.toLong()),
                selectedDate = selectedDate.plusMonths(delta.toLong())
            )
        } else {
            val date = selectedDate.plusWeeks(delta.toLong())
            copy(selectedDate = date, currentMonth = YearMonth.from(date))
        }
    }

    fun goToToday(): CalendarState = copy(
        selectedDate = todayDate,
        currentMonth = YearMonth.from(todayDate),
        monthExpanded = true
    )
}

@Composable
internal fun rememberCalendarToday(): LocalDate {
    var today by remember { mutableStateOf(LocalDate.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            val now = LocalDateTime.now()
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
            delay(Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1_000L))
            today = LocalDate.now()
        }
    }
    return today
}

@Composable
internal fun CalendarTopBar(
    month: YearMonth,
    todayDate: LocalDate,
    onMenu: () -> Unit,
    onSearch: () -> Unit,
    onToday: () -> Unit
) {
    CenterAlignedTopAppBar(
        navigationIcon = {
            IconButton(onClick = onMenu) {
                Icon(painterResource(R.drawable.ic_menu), contentDescription = "Menu")
            }
        },
        title = {
            Text("TH${month.monthValue}", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        },
        actions = {
            IconButton(onClick = onSearch) {
                Icon(painterResource(R.drawable.ic_search), contentDescription = "Tìm kiếm")
            }
            IconButton(onClick = onToday) {
                Surface(
                    modifier = Modifier.size(34.dp),
                    shape = RoundedCornerShape(7.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(todayDate.dayOfMonth.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
    )
}

@Composable
internal fun CalendarSurface(
    modifier: Modifier,
    expanded: Boolean,
    month: YearMonth,
    selectedDate: LocalDate,
    events: List<Event>,
    onDateSelected: (LocalDate) -> Unit,
    onHorizontalSwipe: (Int) -> Unit,
    onVerticalSwipe: (Boolean) -> Unit,
    onLongPress: (LocalDate) -> Unit
) {
    Surface(
        modifier = modifier.pointerInput(expanded, month, selectedDate) {
            var totalX = 0f
            var totalY = 0f
            detectDragGestures(
                onDragStart = { totalX = 0f; totalY = 0f },
                onDrag = { _, amount -> totalX += amount.x; totalY += amount.y },
                onDragEnd = {
                    when {
                        kotlin.math.abs(totalX) > kotlin.math.abs(totalY) && kotlin.math.abs(totalX) > 80f ->
                            onHorizontalSwipe(if (totalX < 0f) 1 else -1)
                        kotlin.math.abs(totalY) > 80f -> onVerticalSwipe(totalY > 0f)
                    }
                }
            )
        },
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
    ) {
        if (expanded) MonthGrid(month, selectedDate, events, onDateSelected, onLongPress)
        else WeekStrip(selectedDate, events, onDateSelected, onLongPress)
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    selected: LocalDate,
    events: List<Event>,
    onSelect: (LocalDate) -> Unit,
    onLongPress: (LocalDate) -> Unit
) {
    val eventsByDate = remember(events) { events.groupBy { it.startDateTime.toLocalDate() } }
    Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
            listOf("T.2", "T.3", "T.4", "T.5", "T.6", "T.7", "CN").forEachIndexed { index, label ->
                Text(
                    label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (index == 6) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        val firstOffset = month.atDay(1).dayOfWeek.value - 1
        val previous = month.minusMonths(1)
        val next = month.plusMonths(1)
        val days = buildList {
            repeat(firstOffset) { index ->
                add(previous.atDay(previous.lengthOfMonth() - firstOffset + index + 1) to false)
            }
            for (day in 1..month.lengthOfMonth()) add(month.atDay(day) to true)
            var nextDay = 1
            while (size < 42) add(next.atDay(nextDay++) to false)
        }
        days.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { (date, inMonth) ->
                    CalendarDay(date, inMonth, date == selected, eventsByDate[date].orEmpty(), onSelect, onLongPress)
                }
            }
        }
    }
}

@Composable
private fun RowScope.CalendarDay(
    date: LocalDate,
    inMonth: Boolean,
    selected: Boolean,
    events: List<Event>,
    onSelect: (LocalDate) -> Unit,
    onLongPress: (LocalDate) -> Unit
) {
    val isSunday = date.dayOfWeek.value == 7
    val isToday = date == LocalDate.now()
    Column(
        modifier = Modifier
            .weight(1f)
            .height(54.dp)
            .padding(horizontal = 1.dp)
            .pointerInput(date) {
                detectTapGestures(
                    onTap = { onSelect(date) },
                    onLongPress = { onLongPress(date) }
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(34.dp)
                .height(30.dp)
                .then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(9.dp)) else Modifier)
                .then(if (isToday && !selected) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(9.dp)) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Text(
                date.dayOfMonth.toString(),
                color = when {
                    !inMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .38f)
                    isSunday -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                },
                fontWeight = if (selected || isToday) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp
            )
        }
        Spacer(Modifier.height(2.dp))
        Column(Modifier.fillMaxWidth().padding(horizontal = 2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            events.take(2).forEach { event ->
                Box(Modifier.fillMaxWidth().height(4.dp).background(eventColor(event), RoundedCornerShape(2.dp)))
            }
        }
    }
}

@Composable
private fun WeekStrip(
    selected: LocalDate,
    events: List<Event>,
    onSelect: (LocalDate) -> Unit,
    onLongPress: (LocalDate) -> Unit
) {
    val start = selected.minusDays((selected.dayOfWeek.value - 1).toLong())
    val eventsByDate = remember(events) { events.groupBy { it.startDateTime.toLocalDate() } }
    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp)) {
        (0..6).forEach { index ->
            val date = start.plusDays(index.toLong())
            CalendarDay(date, true, date == selected, eventsByDate[date].orEmpty(), onSelect, onLongPress)
        }
    }
}

@Composable
internal fun Agenda(date: LocalDate, events: List<Event>, onEventClick: (Event) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp)) {
        Text("${date.dayOfMonth} T.${date.monthValue}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            date.dayOfWeek.getDisplayName(TextStyle.FULL, viLocale).replaceFirstChar { it.uppercase(viLocale) },
            style = MaterialTheme.typography.labelLarge,
            color = if (date.dayOfWeek.value == 7) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        if (events.isEmpty()) {
            Text("Không có lịch trình", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(events, key = { it.id }) { event -> SamsungEventCard(event, onEventClick) }
            }
        }
    }
}

@Composable
private fun SamsungEventCard(event: Event, onClick: (Event) -> Unit) {
    Surface(
        onClick = { onClick(event) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(eventColor(event)))
            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(event.startDateTime.format(timeFormatter), Modifier.width(52.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(10.dp))
                    Text(event.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(5.dp))
                Row {
                    Spacer(Modifier.width(62.dp))
                    Text(
                        "${event.startDateTime.format(DateTimeFormatter.ofPattern("hh:mm a"))} - ${event.endDateTime.format(DateTimeFormatter.ofPattern("hh:mm a"))}" + if (event.location.isNotBlank()) "  ${event.location}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
internal fun AddEventBar(selectedDate: LocalDate, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(painterResource(R.drawable.ic_add), contentDescription = "Thêm sự kiện", modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text("Thêm vào ${selectedDate.dayOfMonth} Th${selectedDate.monthValue}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun eventColor(event: Event): Color = when (event.type) {
    EventType.CLASS_OFFLINE -> MaterialTheme.colorScheme.tertiary
    EventType.CLASS_ONLINE -> MaterialTheme.colorScheme.secondary
    EventType.TASK -> MaterialTheme.colorScheme.primary
    EventType.OTHER -> MaterialTheme.colorScheme.primary
}
