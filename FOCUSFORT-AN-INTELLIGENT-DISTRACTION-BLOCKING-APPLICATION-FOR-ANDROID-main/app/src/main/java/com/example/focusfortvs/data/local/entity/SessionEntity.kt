package com.example.focusfortvs.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mode: String,
    val durationMinutes: Int? = null,
    val startTimeMillis: Long,
    val endTimeMillis: Long? = null,
    val isActive: Boolean,
    val studyTopic: String = "",
    val attemptsUsed: Int = 0,
    val isLockedOut: Boolean = false,
    val behaviorPenalty: Int = 0,   // incremented each time user lingers >15s on overlay
)

