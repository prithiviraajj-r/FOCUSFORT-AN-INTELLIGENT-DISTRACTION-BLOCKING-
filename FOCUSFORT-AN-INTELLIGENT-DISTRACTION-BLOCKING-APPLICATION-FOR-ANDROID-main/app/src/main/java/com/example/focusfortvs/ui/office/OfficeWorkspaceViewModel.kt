package com.example.focusfortvs.ui.office

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.focusfortvs.blocking.AppWhitelistManager
import com.example.focusfortvs.data.network.AzureOpenAIRepository
import com.example.focusfortvs.data.network.OfficeEvaluationResult
import com.example.focusfortvs.data.repository.OfficeAccessRepository
import com.example.focusfortvs.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// UI State Machine
// ─────────────────────────────────────────────────────────────────────────────

sealed class OfficeWorkspaceUiState {
    /** Workspace is ready for input. */
    data class Idle(
        val appName: String,
        val maxMinutes: Int,        // min(10, remainingSessionMinutes - 1)
        val sessionDisabled: Boolean = false,  // true if remaining <= 1 min
    ) : OfficeWorkspaceUiState()

    /** AI is evaluating. Show spinner. */
    object Evaluating : OfficeWorkspaceUiState()

    /** AI approved. Count down, then launch app. */
    data class Approved(
        val packageName: String,
        val appName: String,
        val grantedMinutes: Int,
        val countdownSeconds: Int = 2,
    ) : OfficeWorkspaceUiState()

    /** AI or local checks rejected the request. */
    data class Rejected(val nudgeMessage: String) : OfficeWorkspaceUiState()
}

// ─────────────────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class OfficeWorkspaceViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val azureRepo: AzureOpenAIRepository,
    private val accessRepo: OfficeAccessRepository,
    private val whitelistManager: AppWhitelistManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow<OfficeWorkspaceUiState>(
        OfficeWorkspaceUiState.Idle(appName = "", maxMinutes = 5)
    )
    val uiState: StateFlow<OfficeWorkspaceUiState> = _uiState

    fun initialize(appName: String, packageName: String) {
        viewModelScope.launch {
            val session = sessionRepository.getActiveSession()
            val remainingMinutes = session?.let { s ->
                if (s.durationMinutes == null) return@let 60 // unlimited session
                val elapsed = (System.currentTimeMillis() - s.startTimeMillis) / 60_000L
                (s.durationMinutes - elapsed).toInt().coerceAtLeast(0)
            } ?: 60

            val disabled = remainingMinutes <= 1
            val maxMinutes = minOf(10, remainingMinutes - 1).coerceAtLeast(1)

            _uiState.value = OfficeWorkspaceUiState.Idle(
                appName = appName,
                maxMinutes = maxMinutes,
                sessionDisabled = disabled,
            )
        }
    }

    fun submitRequest(appName: String, packageName: String, reason: String, selectedMinutes: Int) {
        viewModelScope.launch {
            // ── Anti-Abuse: Hourly quota ──────────────────────────────────────
            if (accessRepo.approvedCountInLastHour() >= OfficeAccessRepository.HOURLY_HARD_LIMIT) {
                _uiState.value = OfficeWorkspaceUiState.Rejected(
                    "Hourly emergency quota reached. You've used all ${ OfficeAccessRepository.HOURLY_HARD_LIMIT } approved accesses this hour. Stay focused!"
                )
                return@launch
            }

            // ── Anti-Abuse: Duplicate justification ───────────────────────────
            val last = accessRepo.lastApprovedReason()
            if (last != null && last.trim().equals(reason.trim(), ignoreCase = true)) {
                _uiState.value = OfficeWorkspaceUiState.Rejected(
                    "Duplicate justification detected. You used the exact same reason last time. Try harder. 🧐"
                )
                return@launch
            }

            // ── AI Evaluation ─────────────────────────────────────────────────
            _uiState.value = OfficeWorkspaceUiState.Evaluating

            when (val result = azureRepo.evaluateOfficeRequest(appName, reason)) {
                is OfficeEvaluationResult.Approved -> {
                    accessRepo.recordApprovedRequest(reason)
                    whitelistManager.grant(packageName, selectedMinutes)
                    _uiState.value = OfficeWorkspaceUiState.Approved(
                        packageName = packageName,
                        appName = appName,
                        grantedMinutes = selectedMinutes,
                    )
                }
                is OfficeEvaluationResult.Rejected -> {
                    _uiState.value = OfficeWorkspaceUiState.Rejected(result.nudgeMessage)
                }
            }
        }
    }

    fun resetToIdle(appName: String, packageName: String) {
        initialize(appName, packageName)
    }
}
