package com.nexta.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexta.data.model.Event
import java.time.format.DateTimeFormatter

@Composable
internal fun SearchDialog(
    events: List<Event>,
    onDismiss: () -> Unit,
    onEventClick: (Event) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val matches = events
        .filter {
            query.isBlank() ||
                it.title.contains(query, true) ||
                it.location.contains(query, true) ||
                it.note.contains(query, true)
        }
        .take(20)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tìm kiếm") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    label = { Text("Sự kiện, địa điểm...") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                matches.forEach { event ->
                    TextButton(
                        onClick = { onEventClick(event) },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Column(Modifier.fillMaxWidth()) {
                            Text(event.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                event.startDateTime.format(DateTimeFormatter.ofPattern("dd/MM HH:mm")),
                                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Đóng") } }
    )
}
