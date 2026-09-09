package com.nexta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import com.nexta.ui.AddEventScreen
import com.nexta.ui.MainScreen
import com.nexta.ui.MainUiState
import com.nexta.ui.MainViewModel
import com.nexta.widget.NextAFocusWidgetProvider
import com.nexta.widget.NextAWidgetProvider
import com.example.nexta.ui.theme.NextaTheme
import dagger.hilt.android.AndroidEntryPoint
import com.nexta.data.model.Event

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private var showAddEvent by mutableStateOf(false)
    private var editingEvent by mutableStateOf<Event?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NextaTheme {
                val uiState by viewModel.uiState.collectAsState()
                val saveMessage by viewModel.saveMessage.collectAsState()
                if (showAddEvent || editingEvent != null) {
                    AddEventScreen(
                        onBack = { showAddEvent = false; editingEvent = null },
                        initialEvent = editingEvent,
                        onSave = { event ->
                            viewModel.saveEvent(event)
                            refreshWidgets()
                            showAddEvent = false
                            editingEvent = null
                        }
                    )
                } else when (val state = uiState) {
                    MainUiState.Loading -> MainScreen(events = emptyList(), message = "Đang tải lịch...", onAddEvent = { showAddEvent = true })
                    is MainUiState.Success -> MainScreen(
                        events = state.events,
                        message = saveMessage,
                        onAddEvent = { showAddEvent = true },
                        onEditEvent = { event -> editingEvent = event },
                        onDeleteEvent = { event -> viewModel.deleteEvent(event); refreshWidgets() }
                    )
                    is MainUiState.Error -> MainScreen(events = emptyList(), message = state.message, onAddEvent = { showAddEvent = true })
                }
            }
        }
    }
    private fun refreshWidgets() { NextAWidgetProvider.requestUpdate(this); NextAFocusWidgetProvider.requestUpdate(this) }
}
