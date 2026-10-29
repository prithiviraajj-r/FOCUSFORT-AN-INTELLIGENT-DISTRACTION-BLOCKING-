package com.example.focusfortvs.data.repository

import javax.inject.Inject
import javax.inject.Singleton

data class OfficeAccessRequest(
    val timestampMillis: Long,
    val reason: String,
)

@Singleton
class OfficeAccessRepository @Inject constructor() {

    private val _requests = mutableListOf<OfficeAccessRequest>()

    fun recordApprovedRequest(reason: String) {
        _requests.add(OfficeAccessRequest(System.currentTimeMillis(), reason.trim()))
    }

    fun approvedCountInLastHour(): Int {
        val cutoff = System.currentTimeMillis() - 60 * 60 * 1000L
        return _requests.count { it.timestampMillis >= cutoff }
    }

    fun lastApprovedReason(): String? = _requests.lastOrNull()?.reason

    fun clear() = _requests.clear()

    companion object {
        const val HOURLY_HARD_LIMIT = 5
    }
}
