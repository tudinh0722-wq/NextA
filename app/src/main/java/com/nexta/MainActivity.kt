package com.nexta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.nexta.data.repository.EventRepository
import com.nexta.data.sample.SampleDataSeeder
import com.nexta.ui.AddEventScreen
import com.nexta.ui.MainScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var repository: EventRepository

    private var showAddEvent by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            SampleDataSeeder.seedIfEmpty(repository)
        }

        setContent {
            val events by repository
                .getAllEvents()
                .collectAsState(initial = emptyList())

            if (showAddEvent) {
                AddEventScreen(
                    onBack = {
                        showAddEvent = false
                    },
                    onSave = { event ->
                        lifecycleScope.launch {
                            repository.saveEvent(event)
                            showAddEvent = false
                        }
                    }
                )
            } else {
                MainScreen(
                    events = events,
                    onAddEvent = {
                        showAddEvent = true
                    }
                )
            }
        }
    }
}
