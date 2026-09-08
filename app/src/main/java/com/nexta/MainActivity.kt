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
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var showAddEvent by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val uiState by viewModel.uiState.collectAsState()
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
        // Focus/lock-screen widget still owns its refresh logic.
        // The home widget is currently in diagnostic mode and does not need
        // to be refreshed from the activity.
        NextAFocusWidgetProvider.requestUpdate(this)
    }
}
