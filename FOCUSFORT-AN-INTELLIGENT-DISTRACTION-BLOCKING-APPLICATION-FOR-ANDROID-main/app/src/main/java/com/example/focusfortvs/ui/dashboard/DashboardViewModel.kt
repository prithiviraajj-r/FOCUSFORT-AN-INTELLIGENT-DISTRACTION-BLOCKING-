package com.example.focusfortvs.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.focusfortvs.data.local.dao.ModeFocusRow
import com.example.focusfortvs.data.local.entity.SessionEntity
import com.example.focusfortvs.data.repository.ChartBarEntry
import com.example.focusfortvs.data.repository.ChartRange
import com.example.focusfortvs.data.repository.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    // ── Hero stats ─────────────────────────────────────────────────────────────
    val totalFocusMinutes: Long = 0,
    val todayFocusMinutes: Long = 0,
    val currentStreak: Int = 0,
    val completedSessions: Int = 0,
    val studyVerifications: Int = 0,
    // ── Mode breakdown ─────────────────────────────────────────────────────────
    val modeBreakdown: List<ModeFocusRow> = emptyList(),
    // ── Chart ──────────────────────────────────────────────────────────────────
    val selectedRange: ChartRange = ChartRange.DAILY,
    val chartData: List<ChartBarEntry> = emptyList(),
    val isChartLoading: Boolean = false,
    // ── Detail sheet (bar tap) ─────────────────────────────────────────────────
    val detailSheetBucketMillis: Long? = null,
    val detailSessions: List<SessionEntity> = emptyList(),
    val isDetailLoading: Boolean = false,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repo: DashboardRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState

    init { load() }

    fun refresh() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val totalD       = async { repo.getTotalFocusMinutes() }
            val todayD       = async { repo.getTodayFocusMinutes() }
            val streakD      = async { repo.getCurrentStreak() }
            val countD       = async { repo.getCompletedSessionCount() }
            val verifyD      = async { repo.getStudyVerificationsCount() }
            val modeD        = async { repo.getModeBreakdown() }
            val chartD       = async { repo.getChartData(_uiState.value.selectedRange) }

            _uiState.value = DashboardUiState(
                isLoading            = false,
                totalFocusMinutes    = totalD.await(),
                todayFocusMinutes    = todayD.await(),
                currentStreak        = streakD.await(),
                completedSessions    = countD.await(),
                studyVerifications   = verifyD.await(),
                modeBreakdown        = modeD.await(),
                selectedRange        = _uiState.value.selectedRange,
                chartData            = chartD.await(),
                isChartLoading       = false,
            )
        }
    }

    /** Called when user taps a chart range toggle button. */
    fun onRangeSelected(range: ChartRange) {
        if (_uiState.value.selectedRange == range) return
        _uiState.value = _uiState.value.copy(
            selectedRange  = range,
            isChartLoading = true,
            chartData      = emptyList()
        )
        viewModelScope.launch {
            val data = repo.getChartData(range)
            _uiState.value = _uiState.value.copy(
                chartData      = data,
                isChartLoading = false
            )
        }
    }

    /**
     * Called when user taps a bar.
     * [bucketMillis] is the start of the bucket for that bar.
     */
    fun onBarTapped(bucketMillis: Long) {
        _uiState.value = _uiState.value.copy(
            detailSheetBucketMillis = bucketMillis,
            isDetailLoading         = true,
            detailSessions          = emptyList()
        )
        viewModelScope.launch {
            val sessions = when (_uiState.value.selectedRange) {
                ChartRange.DAILY              -> repo.getSessionsForDay(bucketMillis)
                ChartRange.MONTHLY, ChartRange.YEARLY, ChartRange.ALL_TIME ->
                    repo.getSessionsForMonth(bucketMillis)
            }
            _uiState.value = _uiState.value.copy(
                detailSessions  = sessions,
                isDetailLoading = false
            )
        }
    }

    /** Dismiss the detail bottom sheet. */
    fun dismissDetail() {
        _uiState.value = _uiState.value.copy(
            detailSheetBucketMillis = null,
            detailSessions          = emptyList()
        )
    }
}
