package com.example.focusfortvs.data.repository

import com.example.focusfortvs.data.local.dao.ModeFocusRow
import com.example.focusfortvs.data.local.dao.SessionDao
import com.example.focusfortvs.data.local.dao.TimeBucketRow
import com.example.focusfortvs.data.local.entity.SessionEntity
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/** Time-range toggle options for the chart. */
enum class ChartRange {
    DAILY,    // last 30 days, one bar per day
    MONTHLY,  // last 12 months, one bar per ~30-day period
    YEARLY,   // all time, one bar per ~365-day period
    ALL_TIME  // same as YEARLY but with all data
}

@Singleton
class DashboardRepository @Inject constructor(
    private val sessionDao: SessionDao,
) {
    // ─── Hero stats ───────────────────────────────────────────────────────────

    suspend fun getTotalFocusMinutes(): Long =
        sessionDao.getTotalFocusMillis() / 60_000L

    suspend fun getTodayFocusMinutes(): Long {
        val (start, end) = dayBounds(System.currentTimeMillis())
        // Sum only sessions that overlap today
        val sessions = sessionDao.getSessionsForDay(start, end)
        return sessions.sumOf { s ->
            val sEnd = s.endTimeMillis ?: s.startTimeMillis
            (minOf(sEnd, end) - maxOf(s.startTimeMillis, start)).coerceAtLeast(0L)
        } / 60_000L
    }

    suspend fun getCompletedSessionCount(): Int =
        sessionDao.getCompletedSessionCount()

    suspend fun getStudyVerificationsCount(): Int =
        sessionDao.getStudyVerificationsCount()

    suspend fun getModeBreakdown(): List<ModeFocusRow> =
        sessionDao.getFocusByMode()

    suspend fun getCurrentStreak(): Int {
        val dayBuckets = sessionDao.getDistinctSessionDays()
        if (dayBuckets.isEmpty()) return 0
        val todayBucket = dayBounds(System.currentTimeMillis()).first
        val yesterdayBucket = todayBucket - 86_400_000L
        val lastBucket = dayBuckets.last()
        if (lastBucket != todayBucket && lastBucket != yesterdayBucket) return 0
        var streak = 0
        var expected = lastBucket
        for (bucket in dayBuckets.reversed()) {
            if (bucket == expected) { streak++; expected -= 86_400_000L } else break
        }
        return streak
    }

    // ─── Time-series chart data ───────────────────────────────────────────────

    /**
     * Returns chart bar data for [range].
     * Each entry contains (bucketMillis, minutes).
     * Buckets with zero sessions are filled in so the chart has a continuous axis.
     */
    suspend fun getChartData(range: ChartRange): List<ChartBarEntry> {
        val nowMillis = System.currentTimeMillis()
        return when (range) {
            ChartRange.DAILY -> {
                val from = nowMillis - 29 * 86_400_000L
                val raw = sessionDao.getDailyMinutes(dayBounds(from).first)
                fillDailyGaps(raw, from, nowMillis)
            }
            ChartRange.MONTHLY -> {
                val from = nowMillis - 364 * 86_400_000L
                val raw = sessionDao.getMonthlyMinutes(dayBounds(from).first)
                fillMonthlyGaps(raw, from, nowMillis)
            }
            ChartRange.YEARLY, ChartRange.ALL_TIME -> {
                val raw = sessionDao.getYearlyMinutes()
                raw.map { ChartBarEntry(it.bucketMillis, it.minutes) }
            }
        }
    }

    // ─── Drill-down detail ────────────────────────────────────────────────────

    /** Sessions for a specific calendar day (for the bar-tap detail sheet). */
    suspend fun getSessionsForDay(dayStartMillis: Long): List<SessionEntity> {
        val end = dayStartMillis + 86_400_000L
        return sessionDao.getSessionsForDay(dayStartMillis, end)
    }

    /** Sessions for an approximate 30-day month bucket. */
    suspend fun getSessionsForMonth(monthStartMillis: Long): List<SessionEntity> {
        val end = monthStartMillis + 30L * 86_400_000L
        return sessionDao.getSessionsForMonth(monthStartMillis, end)
    }

    // ─── Gap-filling helpers ──────────────────────────────────────────────────

    private fun fillDailyGaps(
        raw: List<TimeBucketRow>,
        fromMillis: Long,
        toMillis: Long,
    ): List<ChartBarEntry> {
        val map = raw.associate { it.bucketMillis to it.minutes }
        val result = mutableListOf<ChartBarEntry>()
        var cursor = dayBounds(fromMillis).first
        val todayStart = dayBounds(toMillis).first
        while (cursor <= todayStart) {
            result.add(ChartBarEntry(cursor, map[cursor] ?: 0))
            cursor += 86_400_000L
        }
        return result
    }

    private fun fillMonthlyGaps(
        raw: List<TimeBucketRow>,
        fromMillis: Long,
        toMillis: Long,
    ): List<ChartBarEntry> {
        val map = raw.associate { it.bucketMillis to it.minutes }
        val result = mutableListOf<ChartBarEntry>()
        // 30-day buckets, same as the SQL
        val bucketMs = 30L * 86_400_000L
        val fromBucket = (dayBounds(fromMillis).first / bucketMs) * bucketMs
        val toBucket   = (dayBounds(toMillis).first  / bucketMs) * bucketMs
        var cursor = fromBucket
        while (cursor <= toBucket) {
            result.add(ChartBarEntry(cursor, map[cursor] ?: 0))
            cursor += bucketMs
        }
        return result
    }

    // ─── Date helpers ─────────────────────────────────────────────────────────

    private fun dayBounds(refMillis: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply { timeInMillis = refMillis }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        return start to (start + 86_400_000L)
    }
}

data class ChartBarEntry(
    /** Milliseconds of the bucket start (midnight for daily, month-start for monthly, etc.) */
    val bucketMillis: Long,
    val minutes: Int,
)
