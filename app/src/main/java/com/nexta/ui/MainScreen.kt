@file:OptIn(ExperimentalMaterial3Api::class)
package com.nexta.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun MainScreen(
    events: List<Event>,
    message: String? = null,
    initialDate: LocalDate = LocalDate.now(),
    @Suppress("UNUSED_PARAMETER") onAddEvent: (LocalDate) -> Unit = {},
    onEditEvent: (Event) -> Unit = {},
    onDeleteEvent: (Event) -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onQuickAdd: (String, LocalTime, LocalDate, Int) -> Unit = { _, _, _, _ -> },
    onSaveFullEvent: (Event, AlarmSettings) -> Unit = { _, _ -> },
    onBulkImport: () -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onOpenSettings: () -> Unit = {}
) {
    var selectedTabIndex by remember { mutableIntStateOf(1) }
    var selectedDate by remember { mutableStateOf(initialDate) }
    var currentMonth by remember { mutableStateOf(YearMonth.from(initialDate)) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            if (selectedTabIndex == 1) {
                SamsungTopBar(
                    currentMonth = currentMonth,
                    onMenuClick = { selectedTabIndex = 3 },
                    onTodayClick = {
                        selectedDate = LocalDate.now()
                        currentMonth = YearMonth.from(selectedDate)
                    }
                )
            } else {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = when(selectedTabIndex) {
                                0 -> "Trang chủ"
                                2 -> "Thêm sự kiện"
                                3 -> "Cài đặt"
                                else -> "NextA"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF7F7F7)
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when (selectedTabIndex) {
                0 -> HomeTab(events, LocalDate.now())
                1 -> CalendarTab(
                    currentMonth = currentMonth,
                    selectedDate = selectedDate,
                    events = events,
                    onDateSelected = { selectedDate = it },
                    onMonthChange = { currentMonth = it },
                    onEditEvent = onEditEvent,
                    onDeleteEvent = onDeleteEvent,
                    onAddClick = { selectedTabIndex = 2 }
                )
                2 -> AddEventScreen(onBack = { selectedTabIndex = 1 }, onSave = onSaveFullEvent, initialDate = selectedDate, onBulkImport = onBulkImport)
                3 -> SettingsScreen(onBack = { selectedTabIndex = 1 })
            }
            
            FloatingBottomNavBar(
                selectedTabIndex = selectedTabIndex,
                onTabSelected = { selectedTabIndex = it },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun CalendarTab(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    events: List<Event>,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChange: (YearMonth) -> Unit,
    onEditEvent: (Event) -> Unit,
    onDeleteEvent: (Event) -> Unit,
    onAddClick: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                shadowElevation = 2.dp
            ) {
                MonthlyCalendar(
                    currentMonth = currentMonth,
                    selectedDate = selectedDate,
                    events = events,
                    onDateSelected = onDateSelected,
                    onMonthChange = onMonthChange
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            AgendaSection(
                date = selectedDate,
                events = events.filter { it.startDateTime.toLocalDate() == selectedDate },
                onEventClick = onEditEvent,
                onDeleteEvent = onDeleteEvent
            )
        }
        
        SamsungPillFAB(
            date = selectedDate,
            onClick = onAddClick,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)
        )
    }
}

@Composable
private fun HomeTab(events: List<Event>, today: LocalDate) {
    val now = LocalDateTime.now()
    val upcomingEvent = events.filter { it.startDateTime > now }.minByOrNull { it.startDateTime }
    val todayEventsCount = events.count { it.startDateTime.toLocalDate() == today }
    
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column { 
            Text("Chào ngày mới!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.Black)
            Text("Hôm nay bạn có $todayEventsCount sự kiện.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray) 
        }
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))) {
            Column(Modifier.padding(20.dp)) {
                Text("SẮP DIỄN RA", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Gray)
                Spacer(Modifier.height(12.dp))
                if (upcomingEvent != null) { 
                    Text(upcomingEvent.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                    Text("${upcomingEvent.startDateTime.format(timeFormatter)} - ${upcomingEvent.location}", style = MaterialTheme.typography.bodyMedium, color = Color.Black.copy(alpha = 0.7f)) 
                } else {
                    Text("Không có sự kiện sắp tới", style = MaterialTheme.typography.titleMedium, color = Color.Black)
                }
            }
        }
    }
}

@Composable
private fun SamsungTopBar(
    currentMonth: YearMonth,
    onMenuClick: () -> Unit,
    onTodayClick: () -> Unit
) {
    TopAppBar(
        title = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(text = "TH${currentMonth.monthValue}", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(painterResource(R.drawable.ic_settings), contentDescription = "Menu")
            }
        },
        actions = {
            IconButton(onClick = {}) { Icon(painterResource(R.drawable.ic_search), contentDescription = "Search") }
            IconButton(onClick = onTodayClick) {
                Box(modifier = Modifier.size(26.dp).border(1.5.dp, Color.Black, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                    Text(text = LocalDate.now().dayOfMonth.toString(), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
    )
}

@Composable
private fun MonthlyCalendar(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    events: List<Event>,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChange: (YearMonth) -> Unit
) {
    Column(Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth()) {
            listOf("T.2", "T.3", "T.4", "T.5", "T.6", "T.7", "CN").forEach { day ->
                Text(text = day, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = if (day == "CN") Color.Red else Color.Gray)
            }
        }
        Spacer(Modifier.height(8.dp))
        val firstDayOfMonth = currentMonth.atDay(1); val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value; val daysInMonth = currentMonth.lengthOfMonth(); val prevMonth = currentMonth.minusMonths(1); val daysInPrevMonth = prevMonth.lengthOfMonth()
        val totalCells = 42; val days = buildList { for (i in (firstDayOfWeek - 1) downTo 1) { add(prevMonth.atDay(daysInPrevMonth - i + 1) to false) }; for (i in 1..daysInMonth) { add(currentMonth.atDay(i) to true) }; val remaining = totalCells - size; val nextMonth = currentMonth.plusMonths(1); for (i in 1..remaining) { add(nextMonth.atDay(i) to false) } }
        days.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { (date, isCurrentMonth) ->
                    CalendarDay(date = date, isCurrentMonth = isCurrentMonth, isSelected = date == selectedDate, events = events.filter { it.startDateTime.toLocalDate() == date }, onClick = { onDateSelected(date) })
                }
            }
        }
    }
}

@Composable
private fun RowScope.CalendarDay(date: LocalDate, isCurrentMonth: Boolean, isSelected: Boolean, events: List<Event>, onClick: () -> Unit) {
    val isToday = date == LocalDate.now()
    Column(modifier = Modifier.weight(1f).aspectRatio(0.9f).clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(32.dp).background(color = if (isSelected) Color(0xFFE3F2FD) else Color.Transparent, shape = RoundedCornerShape(8.dp)).border(width = if (isToday && !isSelected) 1.dp else 0.dp, color = if (isToday) Color(0xFF2196F3) else Color.Transparent, shape = RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
            Text(text = date.dayOfMonth.toString(), color = when { isSelected -> Color.Black; !isCurrentMonth -> Color.LightGray; date.dayOfWeek.value == 7 -> Color.Red; else -> Color.Black }, fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal, fontSize = 15.sp)
        }
        Spacer(Modifier.height(2.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            events.take(3).forEach { event ->
                val color = when (event.type) { EventType.CLASS_OFFLINE -> Color(0xFF4CAF50); EventType.CLASS_ONLINE -> Color(0xFF2196F3); EventType.TASK -> Color(0xFFFF9800); else -> Color(0xFF9C27B0) }
                Box(Modifier.size(4.dp).background(color, RoundedCornerShape(2.dp)))
            }
        }
    }
}

@Composable
private fun AgendaSection(date: LocalDate, events: List<Event>, onEventClick: (Event) -> Unit, onDeleteEvent: (Event) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = date.dayOfMonth.toString(), fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Column { Text(text = "T.${if(date.dayOfWeek.value == 7) "CN" else date.dayOfWeek.value + 1}", style = MaterialTheme.typography.bodySmall, color = Color.Gray); Text(text = "ÂL 29 Th7", style = MaterialTheme.typography.bodySmall, color = Color.LightGray) }
            Spacer(Modifier.weight(1f)); Icon(painter = painterResource(R.drawable.ic_settings), contentDescription = null, modifier = Modifier.size(24.dp), tint = Color.LightGray)
        }
        Spacer(Modifier.height(16.dp))
        if (events.isEmpty()) { Text(text = "Không có sự kiện", modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), textAlign = TextAlign.Center, color = Color.Gray) }
        else { LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) { items(events) { event -> AgendaItem(event, onClick = { onEventClick(event) }) } } }
    }
}

@Composable
private fun AgendaItem(event: Event, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        Text(text = event.startDateTime.format(timeFormatter), fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.width(54.dp))
        Spacer(Modifier.width(12.dp)); val color = when (event.type) { EventType.CLASS_OFFLINE -> Color(0xFF4CAF50); EventType.CLASS_ONLINE -> Color(0xFF2196F3); EventType.TASK -> Color(0xFFFF9800); else -> Color(0xFF9C27B0) }
        Box(Modifier.width(4.dp).height(36.dp).background(color, RoundedCornerShape(2.dp))); Spacer(Modifier.width(16.dp))
        Column { Text(text = event.title, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(text = "${event.startDateTime.format(timeFormatter)} - ${event.endDateTime.format(timeFormatter)}  ${event.location}", style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    }
}

@Composable
private fun SamsungPillFAB(date: LocalDate, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(onClick = onClick, shape = RoundedCornerShape(28.dp), color = Color.White, shadowElevation = 4.dp, modifier = modifier.height(54.dp).width(200.dp).border(0.5.dp, Color.LightGray, RoundedCornerShape(28.dp))) {
        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Thêm vào ${date.dayOfMonth} Th${date.monthValue}", color = Color.Black, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.width(12.dp)); Icon(painterResource(R.drawable.ic_add), contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun FloatingBottomNavBar(selectedTabIndex: Int, onTabSelected: (Int) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.height(56.dp).fillMaxWidth(0.85f)) {
        Surface(Modifier.fillMaxSize(), shape = RoundedCornerShape(28.dp), color = Color.White.copy(alpha = 0.95f), shadowElevation = 8.dp, tonalElevation = 2.dp) {
            Row(Modifier.fillMaxSize().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                BottomNavItem(R.drawable.ic_home, selectedTabIndex == 0, { onTabSelected(0) })
                BottomNavItem(R.drawable.ic_calendar, selectedTabIndex == 1, { onTabSelected(1) })
                BottomNavItem(R.drawable.ic_add, selectedTabIndex == 2, { onTabSelected(2) })
                BottomNavItem(R.drawable.ic_settings, selectedTabIndex == 3, { onTabSelected(3) })
            }
        }
    }
}

@Composable
private fun BottomNavItem(iconRes: Int, isSelected: Boolean, onClick: () -> Unit) {
    Surface(shape = CircleShape, color = if (isSelected) Color(0xFFFFF9C4) else Color.Transparent, modifier = Modifier.size(40.dp).clickable { onClick() }) {
        Box(contentAlignment = Alignment.Center) { Icon(painterResource(iconRes), null, Modifier.size(22.dp), tint = Color.Black) }
    }
}
