@file:OptIn(ExperimentalMaterial3Api::class)
package com.nexta.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
fun AddEventScreen(onBack: () -> Unit, onSave: (Event) -> Unit, initialEvent: Event? = null, initialDate: LocalDate? = null, onBulkImport: () -> Unit = {}) {
    var title by remember(initialEvent?.id) { mutableStateOf(initialEvent?.title.orEmpty()) }
    var type by remember(initialEvent?.id) { mutableStateOf(initialEvent?.type ?: EventType.CLASS_OFFLINE) }
    var startDate by remember(initialEvent?.id, initialDate) { mutableStateOf(initialEvent?.startDateTime?.toLocalDate() ?: initialDate ?: LocalDate.now()) }
    var endDate by remember(initialEvent?.id, initialDate) { mutableStateOf(initialEvent?.endDateTime?.toLocalDate() ?: initialDate ?: LocalDate.now()) }
    var startTime by remember(initialEvent?.id) { mutableStateOf(initialEvent?.startDateTime?.toLocalTime()?.toString()?.take(5) ?: "08:00") }
    var endTime by remember(initialEvent?.id) { mutableStateOf(initialEvent?.endDateTime?.toLocalTime()?.toString()?.take(5) ?: "09:00") }
    var location by remember(initialEvent?.id) { mutableStateOf(initialEvent?.location.orEmpty()) }
    var note by remember(initialEvent?.id) { mutableStateOf(initialEvent?.note.orEmpty()) }
    var priority by remember(initialEvent?.id) { mutableStateOf(initialEvent?.priority ?: 0) }
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
        onSave(Event(initialEvent?.id ?: UUID.randomUUID().toString(), trimmedTitle, type, startDateTime, endDateTime, location.trim(), note.trim(), priority))
    }

    Scaffold(topBar = { TopAppBar(title = { Text(if (initialEvent == null) "Thêm sự kiện" else "Sửa sự kiện") }, navigationIcon = { TextButton(onClick = onBack) { Text("Quay lại") } }) }) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextField(title, { title = it.take(MAX_TITLE_LENGTH); errorMessage = null }, label = { Text("Tên sự kiện") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            EventTypeField(type) { type = it }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateField("Ngày bắt đầu", startDate, Modifier.weight(1.2f)) { showStartDatePicker = true }
                TimeField("Bắt đầu", startTime, Modifier.weight(.8f)) { startTime = it; errorMessage = null }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateField("Ngày kết thúc", endDate, Modifier.weight(1.2f)) { showEndDatePicker = true }
                TimeField("Kết thúc", endTime, Modifier.weight(.8f)) { endTime = it; errorMessage = null }
            }
            TextField(location, { location = it.take(MAX_LOCATION_LENGTH) }, label = { Text("Địa điểm") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            TextField(note, { note = it.take(MAX_NOTE_LENGTH) }, label = { Text("Ghi chú") }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 2)
            Text("Mức độ ưu tiên", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) { PriorityButton(0, "Bình thường", priority, Modifier.weight(1f)) { priority = 0 }; PriorityButton(1, "Quan trọng", priority, Modifier.weight(1f)) { priority = 1 }; PriorityButton(2, "Rất quan trọng", priority, Modifier.weight(1f)) { priority = 2 } }
            errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Button(onClick = ::save, modifier = Modifier.fillMaxWidth()) { Text(if (initialEvent == null) "Lưu sự kiện" else "Lưu thay đổi") }
            if (initialEvent == null) OutlinedButton(onClick = onBulkImport, modifier = Modifier.fillMaxWidth()) { Text("Nhập nhiều sự kiện từ AI") }
            Spacer(Modifier.height(4.dp))
        }
    }

    if (showStartDatePicker) DatePickerDialogFor(startDate, { showStartDatePicker = false }) { selected -> startDate = selected; if (endDate.isBefore(selected)) endDate = selected; errorMessage = null }
    if (showEndDatePicker) DatePickerDialogFor(endDate, { showEndDatePicker = false }) { selected -> if (selected.isBefore(startDate)) errorMessage = "Ngày kết thúc không được trước ngày bắt đầu." else { endDate = selected; errorMessage = null } }
}

@Composable private fun DateField(label: String, value: LocalDate, modifier: Modifier, onClick: () -> Unit) { TextField(value = value.format(displayDateFormatter), onValueChange = {}, readOnly = true, label = { Text(label) }, modifier = modifier, singleLine = true, trailingIcon = { TextButton(onClick = onClick) { Text("Chọn") } }) }
@Composable private fun TimeField(label: String, value: String, modifier: Modifier, onValueChange: (String) -> Unit) { TextField(value = value, onValueChange = onValueChange, label = { Text(label) }, modifier = modifier, singleLine = true) }

@Composable private fun DatePickerDialogFor(value: LocalDate, onDismiss: () -> Unit, onSelected: (LocalDate) -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = value.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli())
    DatePickerDialog(onDismissRequest = onDismiss, confirmButton = { TextButton(onClick = { state.selectedDateMillis?.let { onSelected(java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate()); onDismiss() } }) { Text("Chọn") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }) { DatePicker(state = state) }
}

@Composable private fun EventTypeField(value: EventType, onValueChange: (EventType) -> Unit) { var expanded by remember { mutableStateOf(false) }; ExposedDropdownMenuBox(expanded, { expanded = !expanded }) { TextField(value.toDisplayName(), {}, readOnly = true, label = { Text("Loại sự kiện") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.fillMaxWidth().menuAnchor()); DropdownMenu(expanded, { expanded = false }) { EventType.entries.forEach { option -> DropdownMenuItem(text = { Text(option.toDisplayName()) }, onClick = { onValueChange(option); expanded = false }) } } } }
@Composable private fun PriorityButton(value: Int, label: String, selected: Int, modifier: Modifier, onClick: () -> Unit) { if (value == selected) Button(onClick, modifier) { Text(label) } else OutlinedButton(onClick, modifier) { Text(label) } }
private fun EventType.toDisplayName() = when (this) { EventType.CLASS_OFFLINE -> "Học trực tiếp"; EventType.CLASS_ONLINE -> "Học trực tuyến"; EventType.TASK -> "Công việc"; EventType.OTHER -> "Khác" }
