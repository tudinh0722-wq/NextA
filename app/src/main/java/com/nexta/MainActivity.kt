package com.nexta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.nexta.data.repository.EventRepository
import com.nexta.data.sample.SampleDataSeeder
import com.nexta.ui.MainScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var repository: EventRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MainScope().launch {
            SampleDataSeeder.seedIfEmpty(repository)
        }

        setContent {
            val events by repository
                .getAllEvents()
                .collectAsState(initial = emptyList())

            MainScreen(
                events = events
            )
        }
    }
}