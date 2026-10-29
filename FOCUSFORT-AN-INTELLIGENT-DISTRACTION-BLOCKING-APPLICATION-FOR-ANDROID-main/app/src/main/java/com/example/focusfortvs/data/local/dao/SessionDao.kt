package com.example.focusfortvs.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.focusfortvs.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    // ─── Core session operations ──────────────────────────────────────────────

    @Query("SELECT * FROM sessions WHERE isActive = 1 ORDER BY startTimeMillis DESC LIMIT 1")
    fun observeActiveSession(): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions WHERE isActive = 1 ORDER BY startTimeMillis DESC LIMIT 1")
    suspend fun getActiveSession(): SessionEntity?

    @Insert
    suspend fun insert(session: SessionEntity): Long

    @Query("UPDATE sessions SET isActive = 0, endTimeMillis = :endTimeMillis WHERE isActive = 1")
    suspend fun endActiveSession(endTimeMillis: Long)

    @Query("UPDATE sessions SET attemptsUsed = :attemptsUsed WHERE isActive = 1")
    suspend fun updateAttemptsUsed(attemptsUsed: Int)

    @Query("UPDATE sessions SET isLockedOut = 1 WHERE isActive = 1")
    suspend fun setLockedOut()

    @Query("UPDATE sessions SET behaviorPenalty = behaviorPenalty + 1 WHERE isActive = 1")
    suspend fun incrementBehaviorPenalty()

    // ─── Hero / top-level stats ───────────────────────────────────────────────

    /** All-time total focus milliseconds (completed sessions only). */
    @Query("""
        SELECT COALESCE(SUM(endTimeMillis - startTimeMillis), 0)
        FROM sessions
        WHERE isActive = 0 AND endTimeMillis IS NOT NULL
    """)
    suspend fun getTotalFocusMillis(): Long

    /** Count of all completed sessions. */
    @Query("SELECT COUNT(*) FROM sessions WHERE isActive = 0 AND endTimeMillis IS NOT NULL")
    suspend fun getCompletedSessionCount(): Int

    /**
     * Count of Student-mode sessions that completed successfully
     * (not locked out) = proxy for "study verifications passed".
     */
    @Query("""
        SELECT COUNT(*) FROM sessions
        WHERE isActive = 0
          AND endTimeMillis IS NOT NULL
          AND mode = 'Student'
          AND isLockedOut = 0
    """)
    suspend fun getStudyVerificationsCount(): Int

    /** All distinct calendar-day buckets (midnight ms) with sessions — for streak. */
    @Query("""
        SELECT DISTINCT (startTimeMillis / 86400000) * 86400000 AS dayBucket
        FROM sessions
        WHERE isActive = 0 AND endTimeMillis IS NOT NULL
        ORDER BY dayBucket ASC
    """)
    suspend fun getDistinctSessionDays(): List<Long>

    // ─── Mode breakdown ───────────────────────────────────────────────────────

    @Query("""
        SELECT mode, COALESCE(SUM(endTimeMillis - startTimeMillis), 0) AS totalMillis
        FROM sessions
        WHERE isActive = 0 AND endTimeMillis IS NOT NULL
        GROUP BY mode
        ORDER BY totalMillis DESC
    """)
    suspend fun getFocusByMode(): List<ModeFocusRow>

    // ─── Time-series chart data ───────────────────────────────────────────────

    /**
     * Daily aggregation: total focus minutes per day.
     * [fromMillis] is the oldest day-start to include.
     * Returns rows sorted oldest → newest.
     */
    @Query("""
        SELECT
            (startTimeMillis / 86400000) * 86400000  AS bucketMillis,
            CAST(COALESCE(SUM(
                MIN(COALESCE(endTimeMillis, startTimeMillis), (startTimeMillis / 86400000 + 1) * 86400000)
                - MAX(startTimeMillis, (startTimeMillis / 86400000) * 86400000)
            ), 0) / 60000 AS INTEGER) AS minutes
        FROM sessions
        WHERE isActive = 0
          AND endTimeMillis IS NOT NULL
          AND startTimeMillis >= :fromMillis
        GROUP BY (startTimeMillis / 86400000)
        ORDER BY bucketMillis ASC
    """)
    suspend fun getDailyMinutes(fromMillis: Long): List<TimeBucketRow>

    /**
     * Monthly aggregation: total focus minutes per calendar month.
     * Uses integer division to bucket by (year*12 + month).
     * Returns rows sorted oldest → newest.
     */
    @Query("""
        SELECT
            (startTimeMillis / 86400000 / 30) * 30 * 86400000  AS bucketMillis,
            CAST(COALESCE(SUM(endTimeMillis - startTimeMillis), 0) / 60000 AS INTEGER) AS minutes
        FROM sessions
        WHERE isActive = 0
          AND endTimeMillis IS NOT NULL
          AND startTimeMillis >= :fromMillis
        GROUP BY (startTimeMillis / 86400000 / 30)
        ORDER BY bucketMillis ASC
    """)
    suspend fun getMonthlyMinutes(fromMillis: Long): List<TimeBucketRow>

    /**
     * Yearly aggregation: total focus minutes per approximate year (365 days).
     */
    @Query("""
        SELECT
            (startTimeMillis / 86400000 / 365) * 365 * 86400000 AS bucketMillis,
            CAST(COALESCE(SUM(endTimeMillis - startTimeMillis), 0) / 60000 AS INTEGER) AS minutes
        FROM sessions
        WHERE isActive = 0 AND endTimeMillis IS NOT NULL
        GROUP BY (startTimeMillis / 86400000 / 365)
        ORDER BY bucketMillis ASC
    """)
    suspend fun getYearlyMinutes(): List<TimeBucketRow>

    // ─── Detail drill-down (for bar tap) ──────────────────────────────────────

    /**
     * All completed sessions that started within [dayStartMillis, dayEndMillis).
     * Used to populate the day-detail bottom sheet.
     */
    @Query("""
        SELECT * FROM sessions
        WHERE isActive = 0
          AND endTimeMillis IS NOT NULL
          AND startTimeMillis >= :dayStartMillis
          AND startTimeMillis < :dayEndMillis
        ORDER BY startTimeMillis ASC
    """)
    suspend fun getSessionsForDay(dayStartMillis: Long, dayEndMillis: Long): List<SessionEntity>

    /**
     * All completed sessions for a given month window.
     */
    @Query("""
        SELECT * FROM sessions
        WHERE isActive = 0
          AND endTimeMillis IS NOT NULL
          AND startTimeMillis >= :monthStartMillis
          AND startTimeMillis < :monthEndMillis
        ORDER BY startTimeMillis ASC
    """)
    suspend fun getSessionsForMonth(monthStartMillis: Long, monthEndMillis: Long): List<SessionEntity>
}

/** Room projection for mode-based focus aggregation */
data class ModeFocusRow(
    val mode: String,
    val totalMillis: Long,
)

/** Room projection for time-series chart bars */
data class TimeBucketRow(
    val bucketMillis: Long,
    val minutes: Int,
)