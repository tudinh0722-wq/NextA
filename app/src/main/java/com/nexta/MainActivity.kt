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
import androidx.lifecycle.lifecycleScope
import com.nexta.alarm.AlarmScheduler
import com.nexta.data.model.AlarmSettings
import com.nexta.data.model.Event
import com.nexta.data.repository.AlarmRepository
import com.nexta.data.repository.LegacyAlarmMigrator
import com.nexta.ui.AddEventScreen
import com.nexta.ui.BulkImportScreen
import com.nexta.ui.MainScreen
import com.nexta.ui.MainUiState
import com.nexta.ui.MainViewModel
import com.nexta.ui.SettingsScreen
import com.nexta.ui.theme.NextaTheme
import com.nexta.widget.NextAFocusWidgetProvider
import com.nexta.widget.NextAWidgetProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    @javax.inject.Inject lateinit var alarmRepository: AlarmRepository
    @javax.inject.Inject lateinit var legacyAlarmMigrator: LegacyAlarmMigrator

    private var showAddEvent by mutableStateOf(false)
    private var editingEvent by mutableStateOf<Event?>(null)
    private var editingAlarmSettings by mutableStateOf<AlarmSettings?>(null)
    private var showBulkImport by mutableStateOf(false)
    private var showSettings by mutableStateOf(false)
    private var addEventDate by mutableStateOf<java.time.LocalDate?>(null)
    private val alarmScheduler by lazy { AlarmScheduler(this, alarmRepository) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch { legacyAlarmMigrator.migrate() }
        if (BuildConfig.DEBUG) viewModel.seedDemoEventsIfEmpty()
        setContent {
            NextaTheme {
                val uiState by viewModel.uiState.collectAsState()
                val saveMessage by viewModel.saveMessage.collectAsState()

                when {
                    showSettings -> SettingsScreen(onBack = { showSettings = false })
                    showBulkImport -> BulkImportScreen(
                        onBack = { showBulkImport = false },
                        onImport = { events, alarm ->
                            viewModel.importEvents(events, events.map { alarm.copy(acknowledged = false) }) {
                                events.forEach { scheduleAlarm(it, alarm) }
                                refreshWidgets()
                                showBulkImport = false
                            }
                        }
                    )
                    showAddEvent || editingEvent != null -> AddEventScreen(
                        onBack = {
                            showAddEvent = false
                            editingEvent = null
                            editingAlarmSettings = null
                            addEventDate = null
                        },
                        initialEvent = editingEvent,
                        initialDate = addEventDate,
                        initialAlarmSettings = editingAlarmSettings,
                        onBulkImport = {
                            showAddEvent = false
                            editingEvent = null
                            showBulkImport = true
                        },
                        onSave = { event, alarm ->
                            viewModel.saveEvent(event, alarm) {
                                scheduleAlarm(event, alarm)
                                refreshWidgets()
                                showAddEvent = false
                                editingEvent = null
                                editingAlarmSettings = null
                                addEventDate = null
                            }
                        }
                    )
                    else -> {
                        val events = (uiState as? MainUiState.Success)?.events ?: emptyList()
                        MainScreen(
                            events = events,
                            message = saveMessage,
                            onAddEvent = { date ->
                                addEventDate = date
                                showAddEvent = true
                            },
                            onEditEvent = { event -> openEditEvent(event) },
                            onDeleteEvent = { event ->
                                alarmScheduler.cancel(event.id)
                                viewModel.deleteEvent(event) { refreshWidgets() }
                            },
                            onOpenSettings = { showSettings = true },
                            onBulkImport = { showBulkImport = true }
                        )
                    }
                }
            }
        }
    }

    private fun openEditEvent(event: Event) {
        lifecycleScope.launch {
            editingAlarmSettings = alarmRepository.getAlarm(event.id) ?: defaultAlarm(event)
            editingEvent = event
        }
    }

    private fun scheduleAlarm(event: Event, settings: AlarmSettings) {
        if (!settings.enabled) {
            alarmScheduler.cancel(event.id)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 7001)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName")))
            }
        }
        alarmScheduler.schedule(event, settings)
    }

    private fun defaultAlarm(@Suppress("UNUSED_PARAMETER") event: Event) = AlarmSettings(enabled = true, leadTimeMinutes = 15, repeatEnabled = true, repeatIntervalMinutes = 5, maxRepeats = 3)

    private fun refreshWidgets() {
        NextAWidgetProvider.requestUpdate(this)
        NextAFocusWidgetProvider.requestUpdate(this)
    }
}
