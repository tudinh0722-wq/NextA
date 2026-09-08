package com.nexta.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexta.data.model.Event
import com.nexta.data.model.ScheduleResult
import com.nexta.data.repository.EventRepository
import com.nexta.data.sample.SampleDataSeeder
import com.nexta.domain.ScheduleStateEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: EventRepository
) : ViewModel() {

    private val _saveMessage = MutableStateFlow<String?>(null)
    val saveMessage: StateFlow<String?> = _saveMessage.asStateFlow()

    val uiState: StateFlow<MainUiState> = repository
        .getAllEvents()
        .map { events: List<Event> ->
            MainUiState.Success(events.sortedBy { it.startDateTime })
        }
        .catch { throwable ->
            emit(
                MainUiState.Error(
                    throwable.message ?: "Không thể tải danh sách sự kiện."
                )
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MainUiState.Loading
        )

    private val now: Flow<LocalDateTime> = flow {
        while (true) {
            emit(LocalDateTime.now())
            delay(30_000)
        }
    }

    val scheduleResult: StateFlow<ScheduleResult> = combine(
        uiState,
        now
    ) { state, currentTime ->
        when (state) {
            is MainUiState.Success -> {
                ScheduleStateEngine.calculateResult(state.events, currentTime)
            }
            else -> ScheduleResult(current = null, next = null)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ScheduleResult(current = null, next = null)
    )

    init {
        viewModelScope.launch {
            try {
                SampleDataSeeder.seedIfEmpty(repository)
            } catch (throwable: Throwable) {
                _saveMessage.value =
                    "Không thể tải dữ liệu mẫu: ${throwable.message ?: "lỗi không xác định"}"
            }
        }
    }

    fun addEvent(event: Event) {
        viewModelScope.launch {
            try {
                repository.saveEvent(event)
                _saveMessage.value = "Đã lưu sự kiện."
            } catch (throwable: Throwable) {
                _saveMessage.value =
                    "Lưu sự kiện thất bại: ${throwable.message ?: "lỗi không xác định"}"
            }
        }
    }

    fun clearSaveMessage() {
        _saveMessage.value = null
    }
}
