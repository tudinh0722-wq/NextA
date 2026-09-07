package com.nexta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.nexta.data.model.Event
import com.nexta.data.repository.EventRepository
import com.nexta.ui.MainScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var repository: EventRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MainScope().launch {
            val currentEvents = repository.getAllEvents().first()
            if (currentEvents.isEmpty()) {
                val now = LocalDateTime.now()
                val dummyEvents = listOf(
                    Event(
                        id = UUID.randomUUID().toString(),
                        title = "Giải tích",
                        location = "Room 302, Building A",
                        occurrences = listOf(now.toLocalDate()),
                        startTime = now.toLocalTime().plusMinutes(30),
                        endTime = now.toLocalTime().plusMinutes(120),
                        notifyBeforeMinutes = 10,
                        note = ""
                    ),
                    Event(
                        id = UUID.randomUUID().toString(),
                        title = "Lập trình",
                        location = "Room 101, Building B",
                        occurrences = listOf(now.toLocalDate().plusDays(1)),
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(10, 0),
                        notifyBeforeMinutes = 10,
                        note = ""
                    )
                )
                dummyEvents.forEach { repository.saveEvent(it) }
            }
        }

        setContent {
            val events by repository.getAllEvents().collectAsState(initial = emptyList())
            MainScreen(events = events)
        }
    }
}
