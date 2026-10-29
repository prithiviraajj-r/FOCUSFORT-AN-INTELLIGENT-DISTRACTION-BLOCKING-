package com.example.focusfortvs.data.local.entity

data class ProfilePreferences(
    val unlockDurationMinutes: Int = 15,
    val officeWorkStartHour: Int = 9,
    val officeWorkEndHour: Int = 17,
    val emergencyPassesPerDay: Int = 3,
)
