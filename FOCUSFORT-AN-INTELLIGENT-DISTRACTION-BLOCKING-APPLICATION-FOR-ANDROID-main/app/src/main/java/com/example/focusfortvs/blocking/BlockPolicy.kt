package com.example.focusfortvs.blocking

import android.util.Log
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

import javax.inject.Inject

interface BlockPolicy {
    fun shouldBlock(packageName: String, sessionState: SessionState, now: Instant): Boolean
}

data class SessionState(
    val mode: String,
    val isActive: Boolean,
    val blockedPackages: Set<String>,
    val startedAt: Instant? = null,
    val focusDurationMinutes: Int = 15,
    val officeStartHour: Int = 9,
    val officeEndHour: Int = 17,
    val emergencyPassesRemaining: Int = 0,
)

class NormalPolicy @Inject constructor() : BlockPolicy {
    override fun shouldBlock(packageName: String, sessionState: SessionState, now: Instant): Boolean {
        return sessionState.isActive && normalizePackageName(packageName) in sessionState.blockedPackages
    }
}

class OfficePolicy @Inject constructor() : BlockPolicy {
    override fun shouldBlock(packageName: String, sessionState: SessionState, now: Instant): Boolean {
        if (!sessionState.isActive || normalizePackageName(packageName) !in sessionState.blockedPackages) return false

        val localTime = LocalTime.ofInstant(now, ZoneId.systemDefault())
        val start = LocalTime.of(sessionState.officeStartHour, 0)
        val end = LocalTime.of(sessionState.officeEndHour, 0)
        return !localTime.isBefore(start) && localTime.isBefore(end)
    }
}

class StudentPolicy @Inject constructor() : BlockPolicy {
    override fun shouldBlock(packageName: String, sessionState: SessionState, now: Instant): Boolean {
        val normalizedPackageName = normalizePackageName(packageName)
        Log.d("FocusFortDebug", "StudentPolicy checking package $normalizedPackageName against blocked list: ${sessionState.blockedPackages}")
        if (!sessionState.isActive || normalizedPackageName !in sessionState.blockedPackages) return false

        val startedAt = sessionState.startedAt ?: return true
        val unlockTime = startedAt.plus(Duration.ofMinutes(sessionState.focusDurationMinutes.toLong()))
        return now.isBefore(unlockTime)
    }
}

class DetoxPolicy @Inject constructor() : BlockPolicy {
    override fun shouldBlock(packageName: String, sessionState: SessionState, now: Instant): Boolean {
        return sessionState.isActive && normalizePackageName(packageName) in sessionState.blockedPackages
    }
}

internal fun normalizeModeForStorage(mode: String): String = mode.trim()

internal fun normalizePackageName(packageName: String): String = packageName.trim()