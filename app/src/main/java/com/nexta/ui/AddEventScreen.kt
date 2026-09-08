@file:OptIn(ExperimentalMaterial3Api::class)

package com.nexta.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexta.data.model.Event
import com.nexta.data.model.EventType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

@Composable
fun AddEventScreen(
    onBack: () -> Unit,
    onSave: (Event) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(EventType.CLASS_OFFLINE) }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var startTime by remember { mutableStateOf("08:00") }
    var endTime by remember { mutableStateOf("09:00") }
    var location by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun save() {
        val trimmedTitle = title.trim()
        val trimmedLocation = location.trim()
        val trimmedNote = note.trim()

        if (trimmedTitle.isBlank()) {
            errorMessage = "Vui lòng nhập tên sự kiện."
            return
        }

        val startDateTime: LocalDateTime
        val endDateTime: LocalDateTime

        try {
            startDateTime = LocalDateTime.of(
                LocalDate.parse(date.trim()),
                LocalTime.parse(startTime.trim())
            )
            endDateTime = LocalDateTime.of(
                LocalDate.parse(date.trim()),
                LocalTime.parse(endTime.trim())
            )
        } catch (_: Exception) {
            errorMessage = "Ngày hoặc giờ không hợp lệ. Ví dụ: 2026-09-08 và 08:30."
            return
        }

        if (!endDateTime.isAfter(startDateTime)) {
            errorMessage = "Giờ kết thúc phải sau giờ bắt đầu."
            return
        }

        onSave(
            Event(
                id = UUID.randomUUID().toString(),
                title = trimmedTitle,
                type = type,
                startDateTime = startDateTime,
                endDateTime = endDateTime,
                location = trimmedLocation,
                note = trimmedNote,
                priority = priority
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thêm sự kiện") },
                navigationIcon = {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text("Quay lại")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    errorMessage = null
                },
                label = { Text("Tên sự kiện") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            EventTypeField(
                value = type,
                onValueChange = { type = it }
            )

            OutlinedTextField(
                value = date,
                onValueChange = {
                    date = it
                    errorMessage = null
                },
                label = { Text("Ngày (yyyy-MM-dd)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = startTime,
                    onValueChange = {
                        startTime = it
                        errorMessage = null
                    },
                    label = { Text("Bắt đầu (HH:mm)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = endTime,
                    onValueChange = {
                        endTime = it
                        errorMessage = null
                    },
                    label = { Text("Kết thúc (HH:mm)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Địa điểm") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Ghi chú") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Text(
                text = "Mức độ ưu tiên",
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PriorityButton(
                    value = 0,
                    label = "Bình thường",
                    selected = priority,
                    modifier = Modifier.weight(1f),
                    onClick = { priority = 0 }
                )
                PriorityButton(
                    value = 1,
                    label = "Quan trọng",
                    selected = priority,
                    modifier = Modifier.weight(1f),
                    onClick = { priority = 1 }
                )
                PriorityButton(
                    value = 2,
                    label = "Rất quan trọng",
                    selected = priority,
                    modifier = Modifier.weight(1f),
                    onClick = { priority = 2 }
                )
            }

            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = ::save,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Lưu sự kiện")
            }
        }
    }
}

@Composable
private fun EventTypeField(
    value: EventType,
    onValueChange: (EventType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = value.toDisplayName(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Loại sự kiện") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            EventType.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.toDisplayName()) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun PriorityButton(
    value: Int,
    label: String,
    selected: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    if (value == selected) {
        Button(onClick = onClick, modifier = modifier) {
            Text(label)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) {
            Text(label)
        }
    }
}

private fun EventType.toDisplayName(): String = when (this) {
    EventType.CLASS_OFFLINE -> "Học trực tiếp"
    EventType.CLASS_ONLINE -> "Học trực tuyến"
    EventType.TASK -> "Công việc"
    EventType.OTHER -> "Khác"
}
