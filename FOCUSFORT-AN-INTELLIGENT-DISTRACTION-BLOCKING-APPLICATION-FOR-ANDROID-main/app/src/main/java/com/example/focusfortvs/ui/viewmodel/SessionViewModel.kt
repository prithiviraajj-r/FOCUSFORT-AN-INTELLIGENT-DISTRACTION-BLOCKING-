package com.example.focusfortvs.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.focusfortvs.blocking.FocusSessionService
import com.example.focusfortvs.data.network.AzureOpenAIRepository
import com.example.focusfortvs.data.network.TopicValidationResult
import com.example.focusfortvs.data.repository.ProfileRepository
import com.example.focusfortvs.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Represents the AI-verification state of the study topic title */
sealed class TopicVerificationUiState {
    /** Initial — user hasn't typed yet or has just cleared the field */
    object Idle : TopicVerificationUiState()
    /** The AI call is in progress */
    object Verifying : TopicVerificationUiState()
    /** AI confirmed this is a legitimate study topic */
    object Verified : TopicVerificationUiState()
    /** AI says this is NOT a valid study topic */
    data class Invalid(val reason: String) : TopicVerificationUiState()
}

data class SessionUiState(
    val currentMode: String = "Student",
    val activeSessionMode: String? = null,
    val isSessionActive: Boolean = false,
    val activeSinceMillis: Long? = null,
    val activeDurationMinutes: Int? = null,
    val selectedDurationMinutes: Int? = 15,
    val studyTopic: String = "",
    /** Tracks AI verification result for the study topic */
    val topicVerification: TopicVerificationUiState = TopicVerificationUiState.Idle,
)

@HiltViewModel
class SessionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val profileRepository: ProfileRepository,
    private val sessionRepository: SessionRepository,
    private val azureRepo: AzureOpenAIRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState

    init {
        viewModelScope.launch {
            combine(
                profileRepository.observeProfile(),
                sessionRepository.observeActiveSession(),
            ) { profile, activeSession ->
                _uiState.value.copy(
                    currentMode = profile?.currentMode ?: "Student",
                    activeSessionMode = activeSession?.mode,
                    isSessionActive = activeSession != null,
                    activeSinceMillis = activeSession?.startTimeMillis,
                    activeDurationMinutes = activeSession?.durationMinutes,
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun setDuration(minutes: Int?) {
        _uiState.value = _uiState.value.copy(selectedDurationMinutes = minutes)
    }

    /**
     * Updates the study topic text and resets topic verification to Idle,
     * forcing the user to re-verify whenever they change the text.
     */
    fun setStudyTopic(topic: String) {
        _uiState.value = _uiState.value.copy(
            studyTopic = topic,
            topicVerification = TopicVerificationUiState.Idle
        )
    }

    /**
     * Calls the AI to verify if the entered topic is a legitimate study subject.
     * Updates [SessionUiState.topicVerification] with the result.
     */
    fun verifyTopic() {
        val topic = _uiState.value.studyTopic.trim()
        if (topic.isBlank()) return
        _uiState.value = _uiState.value.copy(topicVerification = TopicVerificationUiState.Verifying)
        viewModelScope.launch {
            val result = azureRepo.validateStudyTopic(topic)
            _uiState.value = _uiState.value.copy(
                topicVerification = when (result) {
                    is TopicValidationResult.Valid -> TopicVerificationUiState.Verified
                    is TopicValidationResult.Invalid -> TopicVerificationUiState.Invalid(result.reason)
                }
            )
        }
    }

    /** Clears the study topic and resets verification state */
    fun resetTopicVerification() {
        _uiState.value = _uiState.value.copy(
            studyTopic = "",
            topicVerification = TopicVerificationUiState.Idle
        )
    }

    fun startSession() {
        viewModelScope.launch {
            val state = _uiState.value
            Log.d("FocusFortDebug", "Starting session for mode=${state.currentMode} duration=${state.selectedDurationMinutes} topic=${state.studyTopic}")
            sessionRepository.startSession(state.currentMode, state.selectedDurationMinutes, state.studyTopic)
            val intent = Intent(context, FocusSessionService::class.java)
            ContextCompat.startForegroundService(context, intent)
            Log.d("FocusFortDebug", "FocusSessionService started")
        }
    }

    fun stopSession() {
        viewModelScope.launch {
            Log.d("FocusFortDebug", "Stopping session")
            sessionRepository.stopActiveSession()
            context.stopService(Intent(context, FocusSessionService::class.java))
        }
    }
}
