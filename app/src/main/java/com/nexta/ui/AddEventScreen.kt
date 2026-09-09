@file:OptIn(ExperimentalMaterial3Api::class)
package com.nexta.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexta.alarm.AlarmSettings
import com.nexta.data.model.Event
import com.nexta.data.model.EventType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

private const val MAX_TITLE_LENGTH = 47
private const val MAX_LOCATION_LENGTH = 30
private const val MAX_NOTE_LENGTH = 30
private val displayDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val importantOrange = Color(0xFFFF9800)
private val veryImportantRed = Color(0xFFF44336)

@Composable
fun AddEventScreen(
    onBack: () -> Unit,
    onSave: (Event, AlarmSettings) -> Unit,
    initialEvent: Event? = null,
    initialDate: LocalDate? = null,
    initialAlarmSettings: AlarmSettings? = null,
    onBulkImport: () -> Unit = {}
) {
    BackHandler(enabled = true, onBack = onBack)
    var title by remember(initialEvent?.id) { mutableStateOf(initialEvent?.title.orEmpty()) }
    var type by remember(initialEvent?.id) { mutableStateOf(initialEvent?.type ?: EventType.CLASS_OFFLINE) }
    var startDate by remember(initialEvent?.id, initialDate) { mutableStateOf(initialEvent?.startDateTime?.toLocalDate() ?: initialDate ?: LocalDate.now()) }
    var endDate by remember(initialEvent?.id, initialDate) { mutableStateOf(initialEvent?.endDateTime?.toLocalDate() ?: initialDate ?: LocalDate.now()) }
    var startTime by remember(initialEvent?.id) { mutableStateOf(initialEvent?.startDateTime?.toLocalTime()?.toString()?.take(5) ?: "08:00") }
    var endTime by remember(initialEvent?.id) { mutableStateOf(initialEvent?.endDateTime?.toLocalTime()?.toString()?.take(5) ?: "09:00") }
    var location by remember(initialEvent?.id) { mutableStateOf(initialEvent?.location.orEmpty()) }
    var note by remember(initialEvent?.id) { mutableStateOf(initialEvent?.note.orEmpty()) }
    var priority by remember(initialEvent?.id) { mutableStateOf(initialEvent?.priority?.takeIf { it > 0 }) }
    var alarmEnabled by remember(initialEvent?.id) { mutableStateOf(initialAlarmSettings?.enabled ?: true) }
    var leadTime by remember(initialEvent?.id) { mutableIntStateOf(initialAlarmSettings?.leadTimeMinutes ?: 15) }
    var repeatEnabled by remember(initialEvent?.id) { mutableStateOf(initialAlarmSettings?.repeatEnabled ?: true) }
    var repeatInterval by remember(initialEvent?.id) { mutableIntStateOf(initialAlarmSettings?.repeatIntervalMinutes ?: 5) }
    var maxRepeats by remember(initialEvent?.id) { mutableIntStateOf(initialAlarmSettings?.maxRepeats ?: 3) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    fun save() {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isBlank()) { errorMessage = "Vui lòng nhập tên sự kiện."; return }
        val startDateTime: LocalDateTime
        val endDateTime: LocalDateTime
        try {
            startDateTime = LocalDateTime.of(startDate, LocalTime.parse(startTime.trim()))
            endDateTime = LocalDateTime.of(endDate, LocalTime.parse(endTime.trim()))
        } catch (_: Exception) { errorMessage = "Ngày hoặc giờ không hợp lệ."; return }
        if (!endDateTime.isAfter(startDateTime)) { errorMessage = "Thời điểm kết thúc phải sau thời điểm bắt đầu."; return }
        val event = Event(
            initialEvent?.id ?: UUID.randomUUID().toString(), trimmedTitle, type,
            startDateTime, endDateTime, location.trim(), note.trim(), priority ?: 0
        )
        val alarm = AlarmSettings(
            alarmEnabled, leadTime, repeatEnabled, repeatInterval, maxRepeats,
            event.title, startDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(), event.note
        )
        onSave(event, alarm)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (initialEvent == null) "Thêm sự kiện" else "Sửa sự kiện") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Quay lại") } }
            )
        }
    ) { innerPadding ->
        Column(
            Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            CompactTextField(title, { title = it.take(MAX_TITLE_LENGTH); errorMessage = null }, "Tên sự kiện", Modifier.fillMaxWidth())

            // Theo đúng bố cục: nhãn "Loại sự kiện" bên trái, lựa chọn bên phải.
            EventTypeField(type) { type = it }

            DateTimeRow("Ngày - giờ bắt đầu", startDate, startTime, Modifier.fillMaxWidth(), { showStartDatePicker = true }, { startTime = it; errorMessage = null })
            DateTimeRow("Ngày - giờ kết thúc", endDate, endTime, Modifier.fillMaxWidth(), { showEndDatePicker = true }, { endTime = it; errorMessage = null })
            CompactTextField(location, { location = it.take(MAX_LOCATION_LENGTH) }, "Địa điểm", Modifier.fillMaxWidth())
            CompactTextField(note, { note = it.take(MAX_NOTE_LENGTH) }, "Ghi chú", Modifier.fillMaxWidth())

            Text("Nhắc trước", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth().height(38.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (alarmEnabled) "Có báo" else "Không báo", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                if (alarmEnabled) AlarmChoice("${leadTime} phút", listOf(5, 10, 15, 30, 60), leadTime) { leadTime = it }
                Switch(checked = alarmEnabled, onCheckedChange = { alarmEnabled = it }, modifier = Modifier.padding(start = 6.dp))
            }
            if (alarmEnabled) {
                Row(Modifier.fillMaxWidth().height(36.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Lặp nếu chưa xác nhận", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    if (repeatEnabled) {
                        AlarmChoice("Mỗi $repeatInterval phút", listOf(5, 10, 15), repeatInterval) { repeatInterval = it }
                        Spacer(Modifier.width(4.dp))
                        AlarmChoice("$maxRepeats lần", listOf(1, 2, 3, 4), maxRepeats) { maxRepeats = it }
                    }
                    Switch(checked = repeatEnabled, onCheckedChange = { repeatEnabled = it }, modifier = Modifier.padding(start = 6.dp))
                }
            }

            Text("Mức độ ưu tiên", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth().height(36.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PriorityButton(null, "Bình thường", priority, Modifier.weight(1f)) { priority = null }
                PriorityButton(1, "Quan trọng", priority, Modifier.weight(1f), importantOrange) { priority = 1 }
                PriorityButton(2, "Rất quan trọng", priority, Modifier.weight(1.2f), veryImportantRed) { priority = 2 }
            }

            errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, maxLines = 1) }

            // Hai hành động chính tách thành hai dòng, không chen ngang nhau.
            Button(onClick = ::save, modifier = Modifier.fillMaxWidth().height(44.dp)) {
                Text(if (initialEvent == null) "Lưu sự kiện" else "Lưu thay đổi")
            }
            if (initialEvent == null) {
                OutlinedButton(onClick = onBulkImport, modifier = Modifier.fillMaxWidth().height(44.dp)) {
                    Text("Thêm nhiều (hỗ trợ bởi AI)")
                }
            }
        }
    }

    if (showStartDatePicker) DatePickerDialogFor(startDate, { showStartDatePicker = false }) { selected -> startDate = selected; if (endDate.isBefore(selected)) endDate = selected; errorMessage = null }
    if (showEndDatePicker) DatePickerDialogFor(endDate, { showEndDatePicker = false }) { selected -> if (selected.isBefore(startDate)) errorMessage = "Ngày kết thúc không được trước ngày bắt đầu." else { endDate = selected; errorMessage = null } }
}

@Composable
private fun CompactTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier) {
    var focused by remember { mutableStateOf(false) }
    val lineColor = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Column(modifier.onFocusChanged { focused = it.isFocused }) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            modifier = Modifier.fillMaxWidth().height(if (focused) 44.dp else 40.dp),
            decorationBox = { innerTextField ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                    if (value.isBlank()) Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
                    innerTextField()
                }
            }
        )
        Box(Modifier.fillMaxWidth().height(if (focused) 2.dp else 1.dp).background(lineColor))
    }
}

@Composable
private fun EventTypeField(value: EventType, onValueChange: (EventType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).fillMaxHeight().clickable { expanded = true }, contentAlignment = Alignment.CenterStart) {
                Text("Loại sự kiện", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(Modifier.weight(1f).fillMaxHeight().clickable { expanded = true }, contentAlignment = Alignment.CenterEnd) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(value.toDisplayName(), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    Text("⌄", modifier = Modifier.padding(start = 6.dp), fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            EventType.entries.forEach { option ->
                DropdownMenuItem(text = { Text(option.toDisplayName()) }, onClick = { onValueChange(option); expanded = false })
            }
        }
    }
}

@Composable
private fun DateTimeRow(label: String, date: LocalDate, time: String, modifier: Modifier, onDateClick: () -> Unit, onTimeChange: (String) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val lineColor = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Column(modifier.onFocusChanged { focused = it.isFocused }) {
        Row(Modifier.fillMaxWidth().height(if (focused) 44.dp else 40.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.35f))
            Text(date.format(displayDateFormatter), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.clickable(onClick = onDateClick))
            Text("  •  ", color = MaterialTheme.colorScheme.outline)
            BasicTextField(
                value = time,
                onValueChange = onTimeChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.width(54.dp)
            )
        }
        Box(Modifier.fillMaxWidth().height(if (focused) 2.dp else 1.dp).background(lineColor))
    }
}

@Composable
private fun AlarmChoice(label: String, options: List<Int>, selected: Int, onSelected: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp), modifier = Modifier.height(34.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(if (options == listOf(1, 2, 3, 4)) "$option lần" else "$option phút") }, onClick = { onSelected(option); expanded = false })
            }
        }
    }
}

@Composable
private fun DatePickerDialogFor(value: LocalDate, onDismiss: () -> Unit, onSelected: (LocalDate) -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = value.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { state.selectedDateMillis?.let { onSelected(java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate()); onDismiss() } }) { Text("Chọn") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    ) { DatePicker(state = state) }
}

@Composable
private fun PriorityButton(value: Int?, label: String, selected: Int?, modifier: Modifier, accent: Color? = null, onClick: () -> Unit) {
    val isSelected = value == selected
    val color = accent ?: MaterialTheme.colorScheme.primary
    val container = if (isSelected) color.copy(alpha = .14f) else Color.Transparent
    val content = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = modifier.fillMaxHeight().clickable(onClick = onClick),
        shape = RoundedCornerShape(9.dp),
        color = container,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) color else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = content, maxLines = 1, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
        }
    }
}

private fun EventType.toDisplayName() = when (this) {
    EventType.CLASS_OFFLINE -> "Học trực tiếp"
    EventType.CLASS_ONLINE -> "Học trực tuyến"
    EventType.TASK -> "Công việc"
    EventType.OTHER -> "Khác"
}
