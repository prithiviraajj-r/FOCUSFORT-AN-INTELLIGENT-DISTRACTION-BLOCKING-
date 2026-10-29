package com.example.focusfortvs.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: Long = 1,
    val displayName: String,
    val currentMode: String,
    val createdAtMillis: Long,
    val unlockDurationMinutes: Int = 15,
    val officeWorkStartHour: Int = 9,
    val officeWorkEndHour: Int = 17,
    val emergencyPassesPerDay: Int = 3,
)
