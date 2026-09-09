package com.nexta.data.model

data class AlarmSettings(
    val enabled: Boolean = true,
    val leadTimeMinutes: Int = 15,
    val repeatEnabled: Boolean = true,
    val repeatIntervalMinutes: Int = 5,
    val maxRepeats: Int = 3,
    val acknowledged: Boolean = false
)
