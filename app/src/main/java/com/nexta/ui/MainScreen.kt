package com.nexta.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexta.data.model.AlarmSettings
import com.nexta.data.model.Event
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

@Composable
fun MainScreen(
    events: List<Event>,
    message: String? = null,
    initialDate: LocalDate = LocalDate.now(),
    onAddEvent: (LocalDate) -> Unit = {},
    onEditEvent: (Event) -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onDeleteEvent: (Event) -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onQuickAdd: (String, LocalTime, LocalDate, Int) -> Unit = { _, _, _, _ -> },
    @Suppress("UNUSED_PARAMETER") onSaveFullEvent: (Event, AlarmSettings) -> Unit = { _, _ -> },
    @Suppress("UNUSED_PARAMETER") onBulkImport: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    var state by remember(initialDate) { mutableStateOf(CalendarState.initial(initialDate)) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            CalendarTopBar(
                month = state.currentMonth,
                todayDate = state.todayDate,
                onMenu = onOpenSettings,
                onSearch = { state = state.copy(showSearch = true) },
                onToday = { state = state.goToToday() }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(Modifier.fillMaxSize()) {
                CalendarSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    expanded = state.monthExpanded,
                    month = state.currentMonth,
                    selectedDate = state.selectedDate,
                    events = events,
                    onDateSelected = { date ->
                        state = state.selectDate(date)
                    },
                    onHorizontalSwipe = { direction ->
                        state = state.moveHorizontal(direction)
                    },
                    onVerticalSwipe = { expanded ->
                        state = state.copy(monthExpanded = expanded)
                    },
                    onLongPress = onAddEvent
                )

                Agenda(
                    date = state.selectedDate,
                    events = events.filter { it.startDateTime.toLocalDate() == state.selectedDate },
                    onEventClick = onEditEvent
                )
            }

            AddEventBar(
                selectedDate = state.selectedDate,
                onClick = { onAddEvent(state.selectedDate) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp, start = 20.dp, end = 20.dp)
            )
        }
    }

    if (state.showSearch) {
        SearchDialog(
            events = events,
            onDismiss = { state = state.copy(showSearch = false) },
            onEventClick = { event ->
                state = state.copy(
                    showSearch = false,
                    selectedDate = event.startDateTime.toLocalDate(),
                    currentMonth = YearMonth.from(event.startDateTime.toLocalDate())
                )
                onEditEvent(event)
            }
        )
    }
}
