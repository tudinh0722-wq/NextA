@file:OptIn(ExperimentalMaterial3Api::class)

package com.nexta.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexta.data.model.Event

@Composable
fun MainScreen(
    events: List<Event>,
    onAddEvent: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("NextA")
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = 16.dp,
                    vertical = 16.dp
                ),
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

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onAddEvent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ Thêm sự kiện")
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (events.isEmpty()) {
                Text(
                    text = "Chưa có sự kiện nào.",
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                events
                    .sortedBy { it.startDateTime }
                    .forEach { event ->
                        EventCard(event)
                    }
            }
        }
    }
}

@Composable
private fun EventCard(
    event: Event
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
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
    }
}
