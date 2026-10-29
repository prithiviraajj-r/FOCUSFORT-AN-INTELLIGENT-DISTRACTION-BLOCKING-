package com.example.focusfortvs.ui.student

import android.graphics.Bitmap
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.focusfortvs.data.network.AzureOpenAIRepository
import com.example.focusfortvs.data.network.QuizResponse
import com.example.focusfortvs.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import android.content.Intent
import com.example.focusfortvs.blocking.FocusSessionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@HiltViewModel
class StudentWorkspaceViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val azureRepo: AzureOpenAIRepository,
    private val sessionRepo: SessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<WorkspaceUiState>(WorkspaceUiState.Initial)
    val uiState: StateFlow<WorkspaceUiState> = _uiState

    /**
     * Called from BlockOverlayScreen when the 15-second intent timer expires
     * without the student clicking "I have studied".
     */
    fun recordBehaviorPenalty() {
        viewModelScope.launch {
            sessionRepo.incrementBehaviorPenalty()
        }
    }

    fun processImage(bitmap: Bitmap) {
        _uiState.value = WorkspaceUiState.Loading
        viewModelScope.launch {
            try {
                val session = sessionRepo.getActiveSession()
                if (session?.isLockedOut == true) {
                    _uiState.value = WorkspaceUiState.LockedOut
                    return@launch
                }

                val studyTopic = session?.studyTopic?.takeIf { it.isNotBlank() } ?: "general study material"

                // Compress bitmap to Base64
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                val base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

                // Step 1: Relevance validation
                val isRelevant = azureRepo.validateImageRelevance(base64Image, studyTopic)
                if (!isRelevant) {
                    val attemptsLeft = sessionRepo.consumeAttempt()
                    _uiState.value = if (attemptsLeft <= 0) {
                        WorkspaceUiState.LockedOut
                    } else {
                        WorkspaceUiState.TopicMismatch(attemptsLeft)
                    }
                    return@launch
                }

                // Step 2: Topic-grounded quiz generation
                val quizResponse = azureRepo.generateQuizFromImage(base64Image, studyTopic)
                if (quizResponse != null && quizResponse.questions.isNotEmpty()) {
                    // AI gives full relevance credit (20 pts) once image passes validation
                    _uiState.value = WorkspaceUiState.QuizReady(quizResponse, aiRelevancePoints = 20)
                } else {
                    val attemptsLeft = sessionRepo.consumeAttempt()
                    _uiState.value = if (attemptsLeft <= 0) {
                        WorkspaceUiState.LockedOut
                    } else {
                        WorkspaceUiState.Error("Failed to generate quiz. Please try again with a clearer photo.", attemptsLeft)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = WorkspaceUiState.Error("An error occurred: ${e.message}", attemptsLeft = 0)
            }
        }
    }

    /**
     * Submits a quiz using the full 4-factor scoring engine.
     *
     * @param correctAnswers   Number of correct answers selected
     * @param totalQuestions   Total number of questions in the quiz
     * @param aiRelevancePoints AI-awarded relevance score (0-20)
     * @param quizTimeSeconds  Elapsed time from quiz render to Submit click
     */
    fun submitQuiz(
        correctAnswers: Int,
        totalQuestions: Int,
        aiRelevancePoints: Int,
        quizTimeSeconds: Long,
    ) {
        viewModelScope.launch {
            val session = sessionRepo.getActiveSession()
            val behaviorPenalty = session?.behaviorPenalty ?: 0

            val score = ScoringEngine.totalScore(
                correctAnswers = correctAnswers,
                totalQuestions = totalQuestions,
                aiRelevancePoints = aiRelevancePoints,
                behaviorPenalty = behaviorPenalty,
                quizCompletionTimeInSeconds = quizTimeSeconds,
            )

            if (score.passed) {
                sessionRepo.resetAttempts()
                sessionRepo.stopActiveSession()
                context.stopService(Intent(context, FocusSessionService::class.java))
                _uiState.value = WorkspaceUiState.Approved(score)
            } else {
                val attemptsLeft = sessionRepo.consumeAttempt()
                _uiState.value = if (attemptsLeft <= 0) {
                    WorkspaceUiState.LockedOut
                } else {
                    WorkspaceUiState.Rejected(score, attemptsLeft)
                }
            }
        }
    }

    fun resetState() {
        viewModelScope.launch {
            val session = sessionRepo.getActiveSession()
            _uiState.value = if (session?.isLockedOut == true) {
                WorkspaceUiState.LockedOut
            } else {
                WorkspaceUiState.Initial
            }
        }
    }
}

sealed class WorkspaceUiState {
    object Initial : WorkspaceUiState()
    object Loading : WorkspaceUiState()
    data class QuizReady(val quiz: QuizResponse, val aiRelevancePoints: Int) : WorkspaceUiState()
    data class Approved(val score: FocusComplianceScore) : WorkspaceUiState()
    data class Rejected(val score: FocusComplianceScore, val attemptsLeft: Int) : WorkspaceUiState()
    data class TopicMismatch(val attemptsLeft: Int) : WorkspaceUiState()
    object LockedOut : WorkspaceUiState()
    data class Error(val message: String, val attemptsLeft: Int) : WorkspaceUiState()
}

