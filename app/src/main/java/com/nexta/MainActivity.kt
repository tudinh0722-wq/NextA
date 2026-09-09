package com.nexta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import com.example.nexta.ui.theme.NextaTheme
import com.nexta.data.model.Event
import com.nexta.ui.AddEventScreen
import com.nexta.ui.BulkImportScreen
import com.nexta.ui.MainScreen
import com.nexta.ui.MainUiState
import com.nexta.ui.MainViewModel
import com.nexta.widget.NextAFocusWidgetProvider
import com.nexta.widget.NextAWidgetProvider
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private var showAddEvent by mutableStateOf(false)
    private var editingEvent by mutableStateOf<Event?>(null)
    private var showBulkImport by mutableStateOf(false)
    private var addEventDate by mutableStateOf<LocalDate?>(null)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NextaTheme {
                val uiState by viewModel.uiState.collectAsState(); val saveMessage by viewModel.saveMessage.collectAsState()
                when {
                    showBulkImport -> BulkImportScreen(onBack = { showBulkImport = false }, onImport = { events -> viewModel.importEvents(events) { refreshWidgets(); showBulkImport = false } })
                    showAddEvent || editingEvent != null -> AddEventScreen(onBack = { showAddEvent = false; editingEvent = null; addEventDate = null }, initialEvent = editingEvent, initialDate = addEventDate, onBulkImport = { showAddEvent = false; showBulkImport = true }, onSave = { event -> viewModel.saveEvent(event) { refreshWidgets(); showAddEvent = false; editingEvent = null; addEventDate = null } })
                    else -> when (val state = uiState) {
                        MainUiState.Loading -> MainScreen(emptyList(), "Đang tải lịch...", onAddEvent = { date -> showAddEvent = true; addEventDate = date })
                        is MainUiState.Success -> MainScreen(state.events, saveMessage, onAddEvent = { date -> showAddEvent = true; addEventDate = date }, onEditEvent = { editingEvent = it }, onDeleteEvent = { viewModel.deleteEvent(it) { refreshWidgets() } })
                        is MainUiState.Error -> MainScreen(emptyList(), state.message, onAddEvent = { date -> showAddEvent = true; addEventDate = date })
                    }
                }
            }
        }
    }
    private fun refreshWidgets() { NextAWidgetProvider.requestUpdate(this); NextAFocusWidgetProvider.requestUpdate(this) }
}
