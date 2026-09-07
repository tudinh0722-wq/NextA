package com.nexta.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val location: String,
    val occurrencesJson: String,
    val startTime: String,
    val endTime: String,
    val notifyBeforeMinutes: Int,
    val note: String
)
