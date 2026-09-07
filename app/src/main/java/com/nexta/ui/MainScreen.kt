package com.nexta.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexta.data.model.Event
import com.nexta.domain.ScheduleStateEngine
import java.time.Duration
import java.time.LocalDateTime

@Composable
fun MainScreen(events: List<Event>) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }

    // Update time every minute
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000 * 60)
            now = LocalDateTime.now()
        }
    }

    val result = ScheduleStateEngine.calculateResult(events, now)

    Column(modifier = Modifier.padding(16.dp)) {
        val currentText = result.current?.let {
            val minutesLeft = Duration.between(now, it.endTime.atDate(now.toLocalDate())).toMinutes()
            "CURRENT: ${it.title} còn $minutesLeft phút"
        } ?: "CURRENT: —"

        val nextText = result.next?.let {
            val nextStart = it.occurrences.asSequence()
                .map { date -> date.atTime(it.startTime) }
                .filter { start -> start.isAfter(now) }
                .minOrNull()
            
            if (nextStart != null) {
                val minutesWait = Duration.between(now, nextStart).toMinutes()
                "NEXT: ${it.title} sau $minutesWait phút"
            } else "NEXT: —"
        } ?: "NEXT: —"

        Text(text = currentText)
        Text(text = nextText)
    }
}
