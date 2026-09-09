package com.nexta.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "event_alarms",
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["eventId"], unique = true)]
)
data class EventAlarmEntity(
    @PrimaryKey
    val eventId: String,
    val enabled: Boolean,
    val leadTimeMinutes: Int,
    val repeatEnabled: Boolean,
    val repeatIntervalMinutes: Int,
    val maxRepeats: Int,
    val acknowledged: Boolean
)
