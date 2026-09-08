package com.nexta.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexta.data.model.Event
import com.nexta.data.repository.EventRepository
import com.nexta.data.sample.SampleDataSeeder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
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
        .map<MainUiState> { events ->
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
