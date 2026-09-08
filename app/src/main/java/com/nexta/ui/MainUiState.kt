package com.nexta.ui

import com.nexta.data.model.Event

sealed interface MainUiState {
    data object Loading : MainUiState

    data class Success(
        val events: List<Event>
    ) : MainUiState

    data class Error(
        val message: String
    ) : MainUiState
}
