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
import java.util.UUID

private const val MAX_TITLE_LENGTH = 47
private const val MAX_LOCATION_LENGTH = 30
private const val MAX_NOTE_LENGTH = 30

@Composable
fun AddEventScreen(onBack: () -> Unit, onSave: (Event) -> Unit, initialEvent: Event? = null, onBulkImport: () -> Unit = {}) {
    var title by remember(initialEvent?.id) { mutableStateOf(initialEvent?.title.orEmpty()) }
    var type by remember(initialEvent?.id) { mutableStateOf(initialEvent?.type ?: EventType.CLASS_OFFLINE) }
    var date by remember(initialEvent?.id) { mutableStateOf(initialEvent?.startDateTime?.toLocalDate()?.toString() ?: LocalDate.now().toString()) }
    var startTime by remember(initialEvent?.id) { mutableStateOf(initialEvent?.startDateTime?.toLocalTime()?.toString()?.take(5) ?: "08:00") }
    var endTime by remember(initialEvent?.id) { mutableStateOf(initialEvent?.endDateTime?.toLocalTime()?.toString()?.take(5) ?: "09:00") }
    var location by remember(initialEvent?.id) { mutableStateOf(initialEvent?.location.orEmpty()) }
    var note by remember(initialEvent?.id) { mutableStateOf(initialEvent?.note.orEmpty()) }
    var priority by remember(initialEvent?.id) { mutableStateOf(initialEvent?.priority ?: 0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    fun save() {
        val trimmedTitle = title.trim(); if (trimmedTitle.isBlank()) { errorMessage = "Vui lòng nhập tên sự kiện."; return }
        val startDateTime: LocalDateTime; val endDateTime: LocalDateTime
        try { val d = LocalDate.parse(date.trim()); startDateTime = LocalDateTime.of(d, LocalTime.parse(startTime.trim())); endDateTime = LocalDateTime.of(d, LocalTime.parse(endTime.trim())) } catch (_: Exception) { errorMessage = "Ngày hoặc giờ không hợp lệ. Ví dụ: 2026-09-08 và 08:30."; return }
        if (!endDateTime.isAfter(startDateTime)) { errorMessage = "Giờ kết thúc phải sau giờ bắt đầu."; return }
        onSave(Event(initialEvent?.id ?: UUID.randomUUID().toString(), trimmedTitle, type, startDateTime, endDateTime, location.trim(), note.trim(), priority))
    }
    Scaffold(topBar = { TopAppBar(title = { Text(if (initialEvent == null) "Thêm sự kiện" else "Sửa sự kiện") }, navigationIcon = { TextButton(onClick = onBack) { Text("Quay lại") } }) }) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(title, { title = it.take(MAX_TITLE_LENGTH); errorMessage = null }, label = { Text("Tên sự kiện") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            EventTypeField(type) { type = it }
            OutlinedTextField(date, { date = it; errorMessage = null }, label = { Text("Ngày (yyyy-MM-dd)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { OutlinedTextField(startTime, { startTime = it; errorMessage = null }, label = { Text("Bắt đầu (HH:mm)") }, modifier = Modifier.weight(1f), singleLine = true); OutlinedTextField(endTime, { endTime = it; errorMessage = null }, label = { Text("Kết thúc (HH:mm)") }, modifier = Modifier.weight(1f), singleLine = true) }
            OutlinedTextField(location, { location = it.take(MAX_LOCATION_LENGTH) }, label = { Text("Địa điểm") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(note, { note = it.take(MAX_NOTE_LENGTH) }, label = { Text("Ghi chú") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            Text("Mức độ ưu tiên", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { PriorityButton(0, "Bình thường", priority, Modifier.weight(1f)) { priority = 0 }; PriorityButton(1, "Quan trọng", priority, Modifier.weight(1f)) { priority = 1 }; PriorityButton(2, "Rất quan trọng", priority, Modifier.weight(1f)) { priority = 2 } }
            errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = ::save, modifier = Modifier.fillMaxWidth()) { Text(if (initialEvent == null) "Lưu sự kiện" else "Lưu thay đổi") }
            if (initialEvent == null) OutlinedButton(onClick = onBulkImport, modifier = Modifier.fillMaxWidth()) { Text("Nhập nhiều sự kiện từ AI") }
        }
    }
}

@Composable private fun EventTypeField(value: EventType, onValueChange: (EventType) -> Unit) { var expanded by remember { mutableStateOf(false) }; ExposedDropdownMenuBox(expanded, { expanded = !expanded }) { OutlinedTextField(value.toDisplayName(), {}, readOnly = true, label = { Text("Loại sự kiện") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.fillMaxWidth().menuAnchor()); DropdownMenu(expanded, { expanded = false }) { EventType.entries.forEach { option -> DropdownMenuItem(text = { Text(option.toDisplayName()) }, onClick = { onValueChange(option); expanded = false }) } } } }
@Composable private fun PriorityButton(value: Int, label: String, selected: Int, modifier: Modifier, onClick: () -> Unit) { if (value == selected) Button(onClick, modifier) { Text(label) } else OutlinedButton(onClick, modifier) { Text(label) } }
private fun EventType.toDisplayName() = when (this) { EventType.CLASS_OFFLINE -> "Học trực tiếp"; EventType.CLASS_ONLINE -> "Học trực tuyến"; EventType.TASK -> "Công việc"; EventType.OTHER -> "Khác" }
