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
import com.nexta.data.model.AlarmSettings
import com.nexta.data.model.Event
import com.nexta.data.model.EventType
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.delay

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val viLocale = Locale("vi", "VN")

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
    var monthExpanded by remember { mutableStateOf(true) }
    var showSearch by remember { mutableStateOf(false) }
    var todayDate by remember { mutableStateOf(LocalDate.now()) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        while (true) {
            val now = LocalDateTime.now()
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
            delay(Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1_000L))
            todayDate = LocalDate.now()
        }
    }

    LaunchedEffect(message) { message?.let { snackbarHostState.showSnackbar(it) } }

    fun moveHorizontal(delta: Int) {
        if (monthExpanded) {
            currentMonth = currentMonth.plusMonths(delta.toLong())
            selectedDate = selectedDate.plusMonths(delta.toLong())
        } else {
            selectedDate = selectedDate.plusWeeks(delta.toLong())
            currentMonth = YearMonth.from(selectedDate)
        }
    }

    fun today() {
        selectedDate = todayDate
        currentMonth = YearMonth.from(todayDate)
        monthExpanded = true
    }

    Scaffold(
        topBar = {
            CalendarTopBar(
                month = currentMonth,
                todayDate = todayDate,
                onMenu = onOpenSettings,
                onSearch = { showSearch = true },
                onToday = ::today
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxSize()) {
                CalendarSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(monthExpanded, currentMonth, selectedDate) {
                            var totalX = 0f
                            var totalY = 0f
                            detectDragGestures(
                                onDragStart = { totalX = 0f; totalY = 0f },
                                onDrag = { _, amount -> totalX += amount.x; totalY += amount.y },
                                onDragEnd = {
                                    when {
                                        kotlin.math.abs(totalX) > kotlin.math.abs(totalY) && kotlin.math.abs(totalX) > 80f ->
                                            moveHorizontal(if (totalX < 0f) 1 else -1)
                                        kotlin.math.abs(totalY) > 80f -> monthExpanded = totalY > 0f
                                    }
                                }
                            )
                        },
                    expanded = monthExpanded,
                    month = currentMonth,
                    selectedDate = selectedDate,
                    events = events,
                    onDateSelected = {
                        selectedDate = it
                        currentMonth = YearMonth.from(it)
                    },
                    onLongPress = onAddEvent
                )

                Agenda(
                    date = selectedDate,
                    events = events.filter { it.startDateTime.toLocalDate() == selectedDate },
                    onEventClick = onEditEvent
                )
            }

            Surface(
                onClick = { onAddEvent(selectedDate) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp, start = 20.dp, end = 20.dp)
                    .fillMaxWidth(),
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
                    Icon(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = "Thêm sự kiện",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Thêm vào ${selectedDate.dayOfMonth} Th${selectedDate.monthValue}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    if (showSearch) {
        SearchDialog(
            events = events,
            onDismiss = { showSearch = false },
            onEventClick = {
                showSearch = false
                selectedDate = it.startDateTime.toLocalDate()
                currentMonth = YearMonth.from(selectedDate)
                onEditEvent(it)
            }
        )
    }
}

@Composable
private fun CalendarTopBar(
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
            Text(
                text = "TH${month.monthValue}",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
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
private fun CalendarSurface(
    modifier: Modifier,
    expanded: Boolean,
    month: YearMonth,
    selectedDate: LocalDate,
    events: List<Event>,
    onDateSelected: (LocalDate) -> Unit,
    onLongPress: (LocalDate) -> Unit
) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)) {
        if (expanded) MonthGrid(month, selectedDate, events, onDateSelected, onLongPress)
        else WeekStrip(selectedDate, events, onDateSelected, onLongPress)
    }
}

@Composable
private fun MonthGrid(month: YearMonth, selected: LocalDate, events: List<Event>, onSelect: (LocalDate) -> Unit, onLongPress: (LocalDate) -> Unit) {
    Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
            listOf("T.2", "T.3", "T.4", "T.5", "T.6", "T.7", "CN").forEachIndexed { index, label ->
                Text(label, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium, color = if (index == 6) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        val firstOffset = month.atDay(1).dayOfWeek.value - 1
        val previous = month.minusMonths(1)
        val next = month.plusMonths(1)
        val days = buildList {
            repeat(firstOffset) { index -> add(previous.atDay(previous.lengthOfMonth() - firstOffset + index + 1) to false) }
            for (day in 1..month.lengthOfMonth()) add(month.atDay(day) to true)
            var nextDay = 1
            while (size < 42) add(next.atDay(nextDay++) to false)
        }
        days.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { (date, inMonth) -> CalendarDay(date, inMonth, date == selected, events.filter { it.startDateTime.toLocalDate() == date }, onSelect, onLongPress) }
            }
        }
    }
}

@Composable
private fun RowScope.CalendarDay(date: LocalDate, inMonth: Boolean, selected: Boolean, events: List<Event>, onSelect: (LocalDate) -> Unit, onLongPress: (LocalDate) -> Unit) {
    val isSunday = date.dayOfWeek.value == 7
    val isToday = date == LocalDate.now()
    Column(modifier = Modifier.weight(1f).height(54.dp).padding(horizontal = 1.dp).pointerInput(date) { detectTapGestures(onTap = { onSelect(date) }, onLongPress = { onLongPress(date) }) }, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.width(34.dp).height(30.dp).then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(9.dp)) else Modifier).then(if (isToday && !selected) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(9.dp)) else Modifier), contentAlignment = Alignment.Center) {
            Text(date.dayOfMonth.toString(), color = when { !inMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .38f); isSunday -> MaterialTheme.colorScheme.error; else -> MaterialTheme.colorScheme.onSurface }, fontWeight = if (selected || isToday) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp)
        }
        Spacer(Modifier.height(2.dp))
        Column(Modifier.fillMaxWidth().padding(horizontal = 2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            events.take(2).forEach { event -> Box(Modifier.fillMaxWidth().height(4.dp).background(eventColor(event), RoundedCornerShape(2.dp))) }
        }
    }
}

@Composable
private fun WeekStrip(selected: LocalDate, events: List<Event>, onSelect: (LocalDate) -> Unit, onLongPress: (LocalDate) -> Unit) {
    val start = selected.minusDays((selected.dayOfWeek.value - 1).toLong())
    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp)) {
        (0..6).forEach { index ->
            val date = start.plusDays(index.toLong())
            CalendarDay(date, true, date == selected, events.filter { it.startDateTime.toLocalDate() == date }, onSelect, onLongPress)
        }
    }
}

@Composable
private fun Agenda(date: LocalDate, events: List<Event>, onEventClick: (Event) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp)) {
        Text("${date.dayOfMonth} T.${date.monthValue}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(date.dayOfWeek.getDisplayName(TextStyle.FULL, viLocale).replaceFirstChar { it.uppercase(viLocale) }, style = MaterialTheme.typography.labelLarge, color = if (date.dayOfWeek.value == 7) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        if (events.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { Text("Không có lịch trình", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { items(events, key = { it.id }) { event -> SamsungEventCard(event, onEventClick) } }
        }
    }
}

@Composable
private fun SamsungEventCard(event: Event, onClick: (Event) -> Unit) {
    Surface(onClick = { onClick(event) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
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
                    Text("${event.startDateTime.format(DateTimeFormatter.ofPattern("hh:mm a"))} - ${event.endDateTime.format(DateTimeFormatter.ofPattern("hh:mm a"))}" + if (event.location.isNotBlank()) "  ${event.location}" else "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun SearchDialog(events: List<Event>, onDismiss: () -> Unit, onEventClick: (Event) -> Unit) {
    var query by remember { mutableStateOf("") }
    val matches = events.filter { query.isBlank() || it.title.contains(query, true) || it.location.contains(query, true) || it.note.contains(query, true) }.take(20)
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Tìm kiếm") }, text = {
        Column {
            OutlinedTextField(value = query, onValueChange = { query = it }, singleLine = true, label = { Text("Sự kiện, địa điểm...") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            matches.forEach { event ->
                TextButton(onClick = { onEventClick(event) }, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 8.dp)) {
                    Column(Modifier.fillMaxWidth()) {
                        Text(event.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(event.startDateTime.format(DateTimeFormatter.ofPattern("dd/MM HH:mm")), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Đóng") } })
}

@Composable
private fun eventColor(event: Event): Color = when (event.type) {
    EventType.CLASS_OFFLINE -> Color(0xFF7BC8A4)
    EventType.CLASS_ONLINE -> Color(0xFFF0A6C8)
    EventType.TASK -> Color(0xFF8FC8F4)
    EventType.OTHER -> MaterialTheme.colorScheme.primary
}
