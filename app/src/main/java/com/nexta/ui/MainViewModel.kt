package com.nexta.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexta.data.model.AlarmSettings
import com.nexta.data.model.Event
import com.nexta.data.model.EventType
import com.nexta.data.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject
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

    fun saveEvent(event: Event, alarm: AlarmSettings, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.upsertEvent(event, alarm)
                _saveMessage.value = "Đã lưu sự kiện."
                onSaved()
            } catch (t: Throwable) {
                _saveMessage.value = "Lưu sự kiện thất bại: ${t.message ?: "lỗi không xác định"}"
            }
        }
    }

    fun importEvents(events: List<Event>, alarms: List<AlarmSettings>, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.upsertEvents(events, alarms)
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

    /** Demo-only data: insert once when the local database is empty. */
    fun seedDemoEventsIfEmpty() {
        viewModelScope.launch {
            if (repository.getAllEvents().first().isNotEmpty()) return@launch

            val raw = listOf(
                Triple(LocalDate.of(2026, 9, 7), "Tiếng Anh Công nghệ thông tin 1", Pair("12:30", "14:10")),
                Triple(LocalDate.of(2026, 9, 8), "Phát triển ứng dụng thương mại điện tử", Pair("15:10", "17:45")),
                Triple(LocalDate.of(2026, 9, 9), "Cơ sở dữ liệu", Pair("08:45", "10:30")),
                Triple(LocalDate.of(2026, 9, 10), "Tiếng Anh Công nghệ thông tin 1", Pair("12:30", "14:10")),
                Triple(LocalDate.of(2026, 9, 12), "Kiểm thử phần mềm", Pair("07:00", "09:35")),
                Triple(LocalDate.of(2026, 9, 14), "Thiết kế phần mềm", Pair("09:40", "11:25")),
                Triple(LocalDate.of(2026, 9, 14), "Tiếng Anh Công nghệ thông tin 1", Pair("12:30", "14:10")),
                Triple(LocalDate.of(2026, 9, 15), "Phát triển ứng dụng thương mại điện tử", Pair("15:10", "17:45")),
                Triple(LocalDate.of(2026, 9, 16), "Cơ sở dữ liệu", Pair("08:45", "10:30")),
                Triple(LocalDate.of(2026, 9, 17), "Tiếng Anh Công nghệ thông tin 1", Pair("12:30", "14:10")),
                Triple(LocalDate.of(2026, 9, 19), "Kiểm thử phần mềm", Pair("07:00", "09:35"))
            )

            val details = listOf(
                "308 - A9 - Cơ sở 1 - Khu A" to "Buổi chiều - 7,8 Nguyễn Thị Thảo",
                "402 - A9 - Cơ sở 1 - Khu A" to "Buổi chiều - 10,11,12 Đỗ Ngọc Sơn",
                "205 - A9 - Cơ sở 1 - Khu A" to "Buổi sáng - 3,4 Nguyễn Quang Đại",
                "308 - A9 - Cơ sở 1 - Khu A" to "GV: Bùi Phương Thảo",
                "Khu A_PH Online 05 - Khu A_Online" to "GV: Nguyễn Ngọc Khải",
                "302 - A9 - Cơ sở 1 - Khu A" to "GV: Nguyễn Thị Nhung",
                "308 - A9 - Cơ sở 1 - Khu A" to "GV: Bùi Phương Thảo",
                "402 - A9 - Cơ sở 1 - Khu A" to "GV: Đỗ Ngọc Sơn",
                "205 - A9 - Cơ sở 1 - Khu A" to "GV: Nguyễn Quang Đại",
                "308 - A9 - Cơ sở 1 - Khu A" to "GV: Bùi Phương Thảo",
                "Khu A_PH Online 05 - Khu A_Online" to "GV: Nguyễn Ngọc Khải"
            )

            val priorityIndexes = (raw.indices).shuffled().take(5)
            val veryImportant = priorityIndexes.take(3).toSet()
            val important = priorityIndexes.drop(3).toSet()

            val events = raw.mapIndexed { index, (date, title, times) ->
                Event(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    type = EventType.CLASS_OFFLINE,
                    startDateTime = LocalDateTime.of(date, LocalTime.parse(times.first)),
                    endDateTime = LocalDateTime.of(date, LocalTime.parse(times.second)),
                    location = details[index].first,
                    note = details[index].second,
                    priority = when {
                        index in veryImportant -> 2
                        index in important -> 1
                        else -> 0
                    }
                )
            }
            repository.upsertEvents(events, List(events.size) { AlarmSettings(enabled = false) })
        }
    }

    fun clearSaveMessage() {
        _saveMessage.value = null
    }
}
