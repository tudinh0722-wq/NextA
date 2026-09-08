package com.nexta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nexta.ui.AddEventScreen
import com.nexta.ui.MainScreen
import com.nexta.ui.MainUiState
import com.nexta.ui.MainViewModel
import com.nexta.widget.NextAFocusWidgetProvider
import com.nexta.widget.NextAWidgetProvider
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var showAddEvent by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val scheduleResult by viewModel.scheduleResult.collectAsState()
            val saveMessage by viewModel.saveMessage.collectAsState()

            if (showAddEvent) {
                AddEventScreen(
                    onBack = { showAddEvent = false },
                    onSave = { event ->
                        viewModel.addEvent(event)
                        refreshWidgets()
                        showAddEvent = false
                    }
                )
            } else {
                when (val state = uiState) {
                    MainUiState.Loading -> MainScreen(
                        events = emptyList(),
                        message = "Đang tải lịch...",
                        onAddEvent = { showAddEvent = true }
                    )

                    is MainUiState.Success -> MainScreen(
                        events = state.events,
                        scheduleResult = scheduleResult,
                        message = saveMessage,
                        onAddEvent = { showAddEvent = true },
                        onDeleteEvent = { event ->
                            viewModel.deleteEvent(event)
                            refreshWidgets()
                        }
                    )

                    is MainUiState.Error -> MainScreen(
                        events = emptyList(),
                        message = state.message,
                        onAddEvent = { showAddEvent = true }
                    )
                }
            }
        }
    }

    private fun refreshWidgets() {
        NextAWidgetProvider.requestUpdate(this)
        NextAFocusWidgetProvider.requestUpdate(this)
    }
}
