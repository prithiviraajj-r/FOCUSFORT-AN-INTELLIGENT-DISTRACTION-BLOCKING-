package com.example.focusfortvs.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "unlock_events")
data class UnlockEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long?,
    val mode: String,
    val reason: String,
    val timestampMillis: Long,
    val durationMinutes: Int? = null,
)
