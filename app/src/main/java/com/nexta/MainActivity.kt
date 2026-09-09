package com.nexta

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.core.app.ActivityCompat
import com.example.nexta.ui.theme.NextaTheme
import com.nexta.alarm.AlarmScheduler
import com.nexta.alarm.AlarmSettings
import com.nexta.alarm.AlarmSettingsStore
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
    private val alarmScheduler by lazy { AlarmScheduler(this) }
    private val alarmStore by lazy { AlarmSettingsStore(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NextaTheme {
                val uiState by viewModel.uiState.collectAsState()
                val saveMessage by viewModel.saveMessage.collectAsState()
                when {
                    showBulkImport -> BulkImportScreen(
                        onBack = { showBulkImport = false },
                        onImport = { events ->
                            viewModel.importEvents(events) {
                                events.forEach { scheduleAlarm(it, defaultAlarm(it)) }
                                refreshWidgets()
                                showBulkImport = false
                            }
                        }
                    )
                    showAddEvent || editingEvent != null -> AddEventScreen(
                        onBack = { showAddEvent = false; editingEvent = null; addEventDate = null },
                        initialEvent = editingEvent,
                        initialDate = addEventDate,
                        initialAlarmSettings = editingEvent?.let { alarmStore.get(it.id) },
                        onBulkImport = { showAddEvent = false; showBulkImport = true },
                        onSave = { event, alarm ->
                            viewModel.saveEvent(event) {
                                scheduleAlarm(event, alarm)
                                refreshWidgets()
                                showAddEvent = false
                                editingEvent = null
                                addEventDate = null
                            }
                        }
                    )
                    else -> when (val state = uiState) {
                        MainUiState.Loading -> MainScreen(emptyList(), "Đang tải lịch...", onAddEvent = { date -> showAddEvent = true; addEventDate = date })
                        is MainUiState.Success -> MainScreen(
                            state.events,
                            saveMessage,
                            onAddEvent = { date -> showAddEvent = true; addEventDate = date },
                            onEditEvent = { editingEvent = it },
                            onDeleteEvent = { event ->
                                alarmScheduler.cancel(event.id)
                                viewModel.deleteEvent(event) { refreshWidgets() }
                            }
                        )
                        is MainUiState.Error -> MainScreen(emptyList(), state.message, onAddEvent = { date -> showAddEvent = true; addEventDate = date })
                    }
                }
            }
        }
    }

    private fun scheduleAlarm(event: Event, settings: AlarmSettings) {
        if (!settings.enabled) {
            alarmScheduler.schedule(event, settings)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName")))
            }
        }
        alarmScheduler.schedule(event, settings)
    }

    private fun defaultAlarm(event: Event) = AlarmSettings(
        enabled = true,
        leadTimeMinutes = 15,
        repeatEnabled = true,
        repeatIntervalMinutes = 5,
        maxRepeats = 3,
        title = event.title,
        startMillis = event.startDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
        note = event.note
    )

    private fun refreshWidgets() {
        NextAWidgetProvider.requestUpdate(this)
        NextAFocusWidgetProvider.requestUpdate(this)
    }

    companion object { private const val REQUEST_NOTIFICATIONS = 7001 }
}
