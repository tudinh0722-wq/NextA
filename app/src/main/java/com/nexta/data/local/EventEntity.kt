package com.nexta.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val type: String,
    val startDateTime: String,
    val endDateTime: String,
    val location: String,
    val note: String,
    val priority: Int
)
