package com.nexta.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexta.data.model.Event
import com.nexta.data.model.ScheduleResult
import com.nexta.data.repository.EventRepository
import com.nexta.domain.ScheduleStateEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(private val repository: EventRepository) : ViewModel() {
    private val _saveMessage = MutableStateFlow<String?>(null)
    val saveMessage: StateFlow<String?> = _saveMessage.asStateFlow()
    val uiState: StateFlow<MainUiState> = repository.getAllEvents()
        .map { MainUiState.Success(it.sortedBy { event -> event.startDateTime }) as MainUiState }
        .catch { emit(MainUiState.Error(it.message ?: "Không thể tải danh sách sự kiện.")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState.Loading)

    private val now: Flow<LocalDateTime> = flow {
        while (true) {
            emit(LocalDateTime.now())
            delay(30_000)
        }
    }

    val scheduleResult: StateFlow<ScheduleResult> = combine(uiState, now) { state, currentTime ->
        if (state is MainUiState.Success) {
            ScheduleStateEngine.calculateResult(state.events, currentTime)
        } else {
            ScheduleResult(null, null)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScheduleResult(null, null))

    fun saveEvent(event: Event, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.saveEvent(event)
                _saveMessage.value = "Đã lưu sự kiện."
                onSaved()
            } catch (t: Throwable) {
                _saveMessage.value = "Lưu sự kiện thất bại: ${t.message ?: "lỗi không xác định"}"
            }
        }
    }

    fun addEvent(event: Event) = saveEvent(event)

    fun importEvents(events: List<Event>, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.saveEvents(events)
                _saveMessage.value = "Đã thêm ${events.size} sự kiện."
                onSaved()
            } catch (t: Throwable) {
                _saveMessage.value = "Nhập sự kiện thất bại: ${t.message ?: "lỗi không xác định"}"
            }
        }
    }

    fun deleteEvent(event: Event, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.deleteEvent(event.id)
                _saveMessage.value = "Đã xóa sự kiện."
                onDeleted()
            } catch (t: Throwable) {
                _saveMessage.value = "Xóa sự kiện thất bại: ${t.message ?: "lỗi không xác định"}"
            }
        }
    }

    fun clearSaveMessage() {
        _saveMessage.value = null
    }
}
