@file:OptIn(ExperimentalMaterial3Api::class)
package com.nexta.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
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
    var priority by remember(initialEvent?.id) { mutableStateOf(initialEvent?.priority ?: 0) }
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
        if (startDate == endDate && endTime <= startTime) { errorMessage = "Thời điểm kết thúc phải sau thời điểm bắt đầu."; return }
        if (endDate.isBefore(startDate)) { errorMessage = "Ngày kết thúc không được trước ngày bắt đầu."; return }
        val startDateTime: LocalDateTime
        val endDateTime: LocalDateTime
        try {
            startDateTime = LocalDateTime.of(startDate, LocalTime.parse(startTime.trim()))
            endDateTime = LocalDateTime.of(endDate, LocalTime.parse(endTime.trim()))
        } catch (_: Exception) { errorMessage = "Ngày hoặc giờ không hợp lệ."; return }
        if (!endDateTime.isAfter(startDateTime)) { errorMessage = "Thời điểm kết thúc phải sau thời điểm bắt đầu."; return }
        val event = Event(initialEvent?.id ?: UUID.randomUUID().toString(), trimmedTitle, type, startDateTime, endDateTime, location.trim(), note.trim(), priority)
        val alarm = AlarmSettings(alarmEnabled, leadTime, repeatEnabled, repeatInterval, maxRepeats, event.title, startDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(), event.note)
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
            Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            CompactTextField(title, { title = it.take(MAX_TITLE_LENGTH); errorMessage = null }, "Tên sự kiện", Modifier.fillMaxWidth())
            EventTypeField(type) { type = it }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DateField("Ngày bắt đầu", startDate, Modifier.weight(1.1f)) { showStartDatePicker = true }
                TimeField("Bắt đầu", startTime, Modifier.weight(.75f)) { startTime = it; errorMessage = null }
                TimeField("Kết thúc", endTime, Modifier.weight(.75f)) { endTime = it; errorMessage = null }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DateField("Ngày kết thúc", endDate, Modifier.weight(1.1f)) { showEndDatePicker = true }
                CompactTextField(location, { location = it.take(MAX_LOCATION_LENGTH) }, "Địa điểm", Modifier.weight(1.5f))
            }
            CompactTextField(note, { note = it.take(MAX_NOTE_LENGTH) }, "Ghi chú", Modifier.fillMaxWidth())

            Text("Nhắc trước", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(if (alarmEnabled) "Có báo" else "Không báo", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                if (alarmEnabled) AlarmChoice("${leadTime} phút", listOf(5, 10, 15, 30, 60), leadTime) { leadTime = it }
                Switch(checked = alarmEnabled, onCheckedChange = { alarmEnabled = it }, modifier = Modifier.height(36.dp))
            }
            if (alarmEnabled) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Lặp", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    if (repeatEnabled) {
                        AlarmChoice("Mỗi $repeatInterval phút", listOf(5, 10, 15), repeatInterval) { repeatInterval = it }
                        AlarmChoice("$maxRepeats lần", listOf(1, 2, 3, 4), maxRepeats) { maxRepeats = it }
                    }
                    Switch(checked = repeatEnabled, onCheckedChange = { repeatEnabled = it }, modifier = Modifier.height(36.dp))
                }
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Ưu tiên", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(.55f))
                PriorityButton(0, "Bình thường", priority, Modifier.weight(1f)) { priority = 0 }
                PriorityButton(1, "Quan trọng", priority, Modifier.weight(1f)) { priority = 1 }
                PriorityButton(2, "Rất quan trọng", priority, Modifier.weight(1f)) { priority = 2 }
            }

            errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, maxLines = 1) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = ::save, modifier = Modifier.weight(1f)) { Text(if (initialEvent == null) "Lưu sự kiện" else "Lưu thay đổi") }
                if (initialEvent == null) OutlinedButton(onClick = onBulkImport, modifier = Modifier.weight(1f)) { Text("Nhập nhiều") }
            }
        }
    }

    if (showStartDatePicker) DatePickerDialogFor(startDate, { showStartDatePicker = false }) { selected -> startDate = selected; if (endDate.isBefore(selected)) endDate = selected; errorMessage = null }
    if (showEndDatePicker) DatePickerDialogFor(endDate, { showEndDatePicker = false }) { selected -> if (selected.isBefore(startDate)) errorMessage = "Ngày kết thúc không được trước ngày bắt đầu." else { endDate = selected; errorMessage = null } }
}

@Composable private fun CompactTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier) {
    var focused by remember { mutableStateOf(false) }
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.onFocusChanged { focused = it.isFocused },
        singleLine = true,
        minLines = 1,
        maxLines = 1,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = if (focused) 10.dp else 5.dp)
    )
}

@Composable private fun AlarmChoice(label: String, options: List<Int>, selected: Int, onSelected: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, contentPadding = PaddingValues(horizontal = 9.dp, vertical = 0.dp)) { Text(label, style = MaterialTheme.typography.labelSmall) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option -> DropdownMenuItem(text = { Text("$option phút") }, onClick = { onSelected(option); expanded = false }) }
        }
    }
}

@Composable private fun DateField(label: String, value: LocalDate, modifier: Modifier, onClick: () -> Unit) {
    TextField(value = value.format(displayDateFormatter), onValueChange = {}, readOnly = true, label = { Text(label) }, modifier = modifier, singleLine = true, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp), trailingIcon = { TextButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 4.dp)) { Text("Chọn", style = MaterialTheme.typography.labelSmall) } })
}

@Composable private fun TimeField(label: String, value: String, modifier: Modifier, onValueChange: (String) -> Unit) {
    TextField(value = value, onValueChange = onValueChange, label = { Text(label) }, modifier = modifier, singleLine = true, contentPadding = PaddingValues(horizontal = 9.dp, vertical = 5.dp))
}

@Composable private fun DatePickerDialogFor(value: LocalDate, onDismiss: () -> Unit, onSelected: (LocalDate) -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = value.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli())
    DatePickerDialog(onDismissRequest = onDismiss, confirmButton = { TextButton(onClick = { state.selectedDateMillis?.let { onSelected(java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate()); onDismiss() } }) { Text("Chọn") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }) { DatePicker(state = state) }
}

@Composable private fun EventTypeField(value: EventType, onValueChange: (EventType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded, { expanded = !expanded }) {
        TextField(value.toDisplayName(), {}, readOnly = true, label = { Text("Loại sự kiện") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.fillMaxWidth().menuAnchor(), singleLine = true, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp))
        DropdownMenu(expanded, { expanded = false }) { EventType.entries.forEach { option -> DropdownMenuItem(text = { Text(option.toDisplayName()) }, onClick = { onValueChange(option); expanded = false }) } }
    }
}

@Composable private fun PriorityButton(value: Int, label: String, selected: Int, modifier: Modifier, onClick: () -> Unit) {
    if (value == selected) Button(onClick, modifier, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) { Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1) }
    else OutlinedButton(onClick, modifier, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) { Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1) }
}

private fun EventType.toDisplayName() = when (this) {
    EventType.CLASS_OFFLINE -> "Học trực tiếp"
    EventType.CLASS_ONLINE -> "Học trực tuyến"
    EventType.TASK -> "Công việc"
    EventType.OTHER -> "Khác"
}