package com.example.focusfortvs.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "blocked_apps",
    primaryKeys = ["packageName", "mode"]
)
data class BlockedAppEntity(
    val packageName: String,
    val mode: String,
    val appLabel: String = "",
    val isEnabled: Boolean = true,
)
