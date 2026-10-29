package com.example.focusfortvs.data.repository

import com.example.focusfortvs.data.local.dao.SessionDao
import com.example.focusfortvs.data.local.entity.SessionEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val sessionDao: SessionDao,
    @ApplicationContext private val context: android.content.Context,
    private val normalModeRepository: NormalModeRepository,
) {
    fun observeActiveSession(): Flow<SessionEntity?> = sessionDao.observeActiveSession()

    suspend fun getActiveSession(): SessionEntity? = sessionDao.getActiveSession()

    suspend fun startSession(mode: String, durationMinutes: Int?, studyTopic: String = "", startedAtMillis: Long = System.currentTimeMillis()) {
        sessionDao.endActiveSession(startedAtMillis)
        sessionDao.insert(
            SessionEntity(
                mode = mode.trim(),
                durationMinutes = durationMinutes,
                startTimeMillis = startedAtMillis,
                endTimeMillis = null,
                isActive = true,
                studyTopic = studyTopic.trim(),
                attemptsUsed = 0,
                isLockedOut = false,
            ),
        )
        // Reset lockout flag for new session
        context.getSharedPreferences("focusfort_state", android.content.Context.MODE_PRIVATE)
            .edit().putBoolean("is_locked_out", false).apply()
            
        // Reset normal mode quotas and hard locks
        normalModeRepository.clearSessionData()
    }

    suspend fun stopActiveSession(endTimeMillis: Long = System.currentTimeMillis()) {
        sessionDao.endActiveSession(endTimeMillis)
    }

    /**
     * Consumes one attempt from the current session.
     * If 3 attempts have been used, the session is locked out.
     * Returns the number of remaining attempts (0 means fully locked out).
     */
    suspend fun consumeAttempt(): Int {
        val session = sessionDao.getActiveSession() ?: return 0
        val newCount = session.attemptsUsed + 1
        sessionDao.updateAttemptsUsed(newCount)
        if (newCount >= 3) {
            sessionDao.setLockedOut()
            // Write lockout flag to SharedPreferences so the overlay can read it
            context.getSharedPreferences("focusfort_state", android.content.Context.MODE_PRIVATE)
                .edit().putBoolean("is_locked_out", true).apply()
            return 0
        }
        return 3 - newCount
    }

    /**
     * Resets the attempt counter after a successful quiz pass (approved unlock).
     */
    suspend fun resetAttempts() {
        sessionDao.updateAttemptsUsed(0)
        // Clear lockout flag
        context.getSharedPreferences("focusfort_state", android.content.Context.MODE_PRIVATE)
            .edit().putBoolean("is_locked_out", false).apply()
    }

    /**
     * Called when a student stays on the intent overlay for >15 seconds without responding.
     * Penalises the behavior score.
     */
    suspend fun incrementBehaviorPenalty() {
        sessionDao.incrementBehaviorPenalty()
    }
}
