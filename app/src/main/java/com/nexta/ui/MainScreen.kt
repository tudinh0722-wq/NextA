@file:OptIn(ExperimentalMaterial3Api::class)

package com.nexta.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexta.data.model.Event
import com.nexta.data.model.ScheduleResult

@Composable
fun MainScreen(
    events: List<Event>,
    scheduleResult: ScheduleResult = ScheduleResult(null, null),
    message: String? = null,
    onAddEvent: () -> Unit = {},
    onDeleteEvent: (Event) -> Unit = {}
) {
    var eventToDelete by remember { mutableStateOf<Event?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NextA") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Lịch của bạn",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "Hôm nay và các sự kiện sắp tới",
                style = MaterialTheme.typography.bodyMedium
            )

            message?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onAddEvent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ Thêm sự kiện")
            }

            scheduleResult.current?.let { event ->
                ScheduleHighlight("ĐANG DIỄN RA", event)
            }

            scheduleResult.next?.let { event ->
                ScheduleHighlight("TIẾP THEO", event)
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (events.isEmpty()) {
                Text(
                    text = "Chưa có sự kiện nào.",
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                events.forEach { event ->
                    EventCard(
                        event = event,
                        onLongClick = { eventToDelete = event }
                    )
                }
            }
        }
    }

    eventToDelete?.let { event ->
        AlertDialog(
            onDismissRequest = { eventToDelete = null },
            title = { Text("Xóa sự kiện?") },
            text = { Text("Bạn có chắc muốn xóa \"${event.title}\" không?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteEvent(event)
                        eventToDelete = null
                    }
                ) {
                    Text("Xóa")
                }
            },
            dismissButton = {
                TextButton(onClick = { eventToDelete = null }) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
private fun ScheduleHighlight(
    label: String,
    event: Event
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "${event.startDateTime} → ${event.endDateTime}",
                style = MaterialTheme.typography.bodyMedium
            )
            if (event.location.isNotBlank()) {
                Text(
                    text = event.location,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun EventCard(
    event: Event,
    onLongClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick
            )
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = event.title,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "${event.startDateTime} → ${event.endDateTime}",
            style = MaterialTheme.typography.bodyMedium
        )
        if (event.location.isNotBlank()) {
            Text(
                text = event.location,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text(
            text = "Nhấn giữ để xóa",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}
