@file:OptIn(ExperimentalMaterial3Api::class)

package com.nexta.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.nexta.data.model.AlarmSettings
import com.nexta.data.model.Event
import com.nexta.data.model.EventType
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("vi", "VN"))
private enum class CalendarMode { MONTH, WEEK, DAY }

@Composable
fun MainScreen(
    events: List<Event>,
    message: String? = null,
    initialDate: LocalDate = LocalDate.now(),
    onAddEvent: (LocalDate) -> Unit = {},
    onEditEvent: (Event) -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onDeleteEvent: (Event) -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onQuickAdd: (String, LocalTime, LocalDate, Int) -> Unit = { _, _, _, _ -> },
    @Suppress("UNUSED_PARAMETER") onSaveFullEvent: (Event, AlarmSettings) -> Unit = { _, _ -> },
    @Suppress("UNUSED_PARAMETER") onBulkImport: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    var selectedDate by remember { mutableStateOf(initialDate) }
    var currentMonth by remember { mutableStateOf(YearMonth.from(initialDate)) }
    var mode by remember { mutableStateOf(CalendarMode.MONTH) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) { message?.let { snackbarHostState.showSnackbar(it) } }

    fun move(delta: Long) {
        when (mode) {
            CalendarMode.MONTH -> {
                currentMonth = currentMonth.plusMonths(delta)
                selectedDate = selectedDate.plusMonths(delta)
            }
            CalendarMode.WEEK -> selectedDate = selectedDate.plusWeeks(delta)
            CalendarMode.DAY -> selectedDate = selectedDate.plusDays(delta)
        }
    }

    fun today() {
        selectedDate = LocalDate.now()
        currentMonth = YearMonth.from(selectedDate)
    }

    Scaffold(
        topBar = {
            CalendarTopBar(
                mode = mode,
                date = selectedDate,
                month = currentMonth,
                onPrevious = { move(-1) },
                onNext = { move(1) },
                onToday = ::today,
                onSettings = onOpenSettings
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxSize()) {
                ModeTabs(mode) { mode = it }

                var dragTotal by remember { mutableFloatStateOf(0f) }
                CalendarSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(mode, currentMonth, selectedDate) {
                            detectHorizontalDragGestures(
                                onDragStart = { dragTotal = 0f },
                                onHorizontalDrag = { _, amount -> dragTotal += amount },
                                onDragEnd = {
                                    if (dragTotal > 80f) move(-1)
                                    else if (dragTotal < -80f) move(1)
                                }
                            )
                        },
                    mode = mode,
                    month = currentMonth,
                    selectedDate = selectedDate,
                    events = events,
                    onDateSelected = {
                        selectedDate = it
                        currentMonth = YearMonth.from(it)
                    }
                )

                Agenda(
                    date = selectedDate,
                    events = events.filter { it.startDateTime.toLocalDate() == selectedDate },
                    onEventClick = onEditEvent
                )
            }

            FloatingActionButton(
                onClick = { onAddEvent(selectedDate) },
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(painterResource(R.drawable.ic_add), "Thêm sự kiện")
            }
        }
    }
}

@Composable
private fun CalendarTopBar(
    mode: CalendarMode,
    date: LocalDate,
    month: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onSettings: () -> Unit
) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onSettings) {
                Icon(painterResource(R.drawable.ic_settings), "Cài đặt")
            }
        },
        title = {
            Column {
                Text(
                    when (mode) {
                        CalendarMode.MONTH -> month.format(monthFormatter)
                        CalendarMode.WEEK -> "Tuần của ${date.dayOfMonth}/${date.monthValue}"
                        CalendarMode.DAY -> date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("vi", "VN"))
                    }.replaceFirstChar { it.uppercase(Locale("vi", "VN")) },
                    fontWeight = FontWeight.Bold
                )
                if (mode == CalendarMode.DAY) {
                    Text(
                        date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onPrevious) { Text("‹", fontSize = 32.sp) }
            IconButton(onClick = onNext) { Text("›", fontSize = 32.sp) }
            IconButton(onClick = onToday) {
                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(LocalDate.now().dayOfMonth.toString(), fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
    )
}

@Composable
private fun ModeTabs(mode: CalendarMode, onSelected: (CalendarMode) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CalendarMode.values().forEach { item ->
            val selected = item == mode
            Surface(
                onClick = { onSelected(item) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp),
                color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Text(
                    when (item) { CalendarMode.MONTH -> "Tháng"; CalendarMode.WEEK -> "Tuần"; CalendarMode.DAY -> "Ngày" },
                    Modifier.padding(vertical = 9.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun CalendarSurface(
    modifier: Modifier,
    mode: CalendarMode,
    month: YearMonth,
    selectedDate: LocalDate,
    events: List<Event>,
    onDateSelected: (LocalDate) -> Unit
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
    ) {
        when (mode) {
            CalendarMode.MONTH -> MonthGrid(month, selectedDate, events, onDateSelected)
            CalendarMode.WEEK -> WeekStrip(selectedDate, events, onDateSelected)
            CalendarMode.DAY -> DayHeader(selectedDate, events, onDateSelected)
        }
    }
}

@Composable
private fun MonthGrid(month: YearMonth, selected: LocalDate, events: List<Event>, onSelect: (LocalDate) -> Unit) {
    Column(Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth()) {
            listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN").forEachIndexed { i, label ->
                Text(label, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium,
                    color = if (i == 6) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(6.dp))
        val offset = month.atDay(1).dayOfWeek.value - 1
        val previous = month.minusMonths(1)
        val next = month.plusMonths(1)
        val days = buildList {
            repeat(offset) { i -> add(previous.atDay(previous.lengthOfMonth() - offset + i + 1) to false) }
            for (d in 1..month.lengthOfMonth()) add(month.atDay(d) to true)
            var d = 1
            while (size < 42) add(next.atDay(d++) to false)
        }
        days.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { (date, inMonth) ->
                    CalendarDay(date, inMonth, date == selected, events.filter { it.startDateTime.toLocalDate() == date }, onSelect)
                }
            }
        }
    }
}

@Composable
private fun RowScope.CalendarDay(date: LocalDate, inMonth: Boolean, selected: Boolean, events: List<Event>, onSelect: (LocalDate) -> Unit) {
    val today = date == LocalDate.now()
    val weight = events.sumOf { it.priority + 1 }.coerceAtMost(5)
    val load = if (weight == 0) Color.Transparent else MaterialTheme.colorScheme.primary.copy(alpha = (0.025f + weight * 0.025f).coerceAtMost(0.15f))
    Column(
        Modifier.weight(1f).aspectRatio(.92f).clickable { onSelect(date) }.padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(34.dp)
                .background(if (selected) MaterialTheme.colorScheme.primaryContainer else load, CircleShape)
                .then(if (today && !selected) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Text(
                date.dayOfMonth.toString(),
                color = when { !inMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(.4f); date.dayOfWeek.value == 7 -> MaterialTheme.colorScheme.error; else -> MaterialTheme.colorScheme.onSurface },
                fontWeight = if (today || selected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            events.take(3).forEach { Box(Modifier.size(4.dp).background(eventColor(it), CircleShape)) }
        }
    }
}

@Composable
private fun WeekStrip(selected: LocalDate, events: List<Event>, onSelect: (LocalDate) -> Unit) {
    val start = selected.minusDays((selected.dayOfWeek.value - 1).toLong())
    Column(Modifier.padding(horizontal = 10.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth()) {
            (0..6).forEach { i ->
                val date = start.plusDays(i.toLong())
                WeekDay(date, date == selected, events.count { it.startDateTime.toLocalDate() == date }, onSelect)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (0..6).forEach { i ->
                val count = events.count { it.startDateTime.toLocalDate() == start.plusDays(i.toLong()) }
                Box(Modifier.weight(1f).height((8 + count.coerceAtMost(6) * 5).dp).background(MaterialTheme.colorScheme.primary.copy(if (count == 0) .06f else .16f), RoundedCornerShape(4.dp)))
            }
        }
    }
}

@Composable
private fun RowScope.WeekDay(date: LocalDate, selected: Boolean, count: Int, onSelect: (LocalDate) -> Unit) {
    Column(Modifier.weight(1f).clickable { onSelect(date) }, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("vi", "VN")), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Box(Modifier.size(36.dp).background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape), contentAlignment = Alignment.Center) {
            Text(date.dayOfMonth.toString(), color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
        }
        Spacer(Modifier.height(3.dp))
        if (count > 0) Box(Modifier.size(4.dp).background(MaterialTheme.colorScheme.primary, CircleShape)) else Spacer(Modifier.size(4.dp))
    }
}

@Composable
private fun DayHeader(date: LocalDate, events: List<Event>, onSelect: (LocalDate) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("‹", fontSize = 32.sp, modifier = Modifier.clickable { onSelect(date.minusDays(1)) })
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Text("${events.size} sự kiện", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("›", fontSize = 32.sp, modifier = Modifier.clickable { onSelect(date.plusDays(1)) })
    }
}

@Composable
private fun Agenda(date: LocalDate, events: List<Event>, onEventClick: (Event) -> Unit) {
    Column(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 18.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("vi", "VN")), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(12.dp))
            Text("${events.size} sự kiện", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(10.dp))
        if (events.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { Text("Lịch trống", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(events, key = { it.id }) { event -> EventRow(event) { onEventClick(event) } }
            }
        }
    }
}

@Composable
private fun EventRow(event: Event, onClick: () -> Unit) {
    Surface(onClick = onClick, color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(52.dp), horizontalAlignment = Alignment.End) {
                Text(event.startDateTime.format(timeFormatter), fontWeight = FontWeight.Bold)
                Text(event.endDateTime.format(timeFormatter), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(12.dp))
            Box(Modifier.width(4.dp).height(42.dp).background(eventColor(event), RoundedCornerShape(2.dp)))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(event.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val secondary = listOf(event.location, event.note).filter { it.isNotBlank() }.joinToString(" • ")
                if (secondary.isNotBlank()) Text(secondary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun eventColor(event: Event): Color = when (event.type) {
    EventType.CLASS_OFFLINE -> MaterialTheme.colorScheme.tertiary
    EventType.CLASS_ONLINE -> MaterialTheme.colorScheme.primary
    EventType.TASK -> MaterialTheme.colorScheme.secondary
    else -> MaterialTheme.colorScheme.error
}
