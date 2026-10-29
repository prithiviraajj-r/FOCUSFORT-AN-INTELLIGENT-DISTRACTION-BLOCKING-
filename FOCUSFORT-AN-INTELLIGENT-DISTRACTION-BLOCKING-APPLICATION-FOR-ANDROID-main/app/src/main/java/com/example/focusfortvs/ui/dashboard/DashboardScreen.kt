@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.focusfortvs.ui.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.focusfortvs.data.local.dao.ModeFocusRow
import com.example.focusfortvs.data.local.entity.SessionEntity
import com.example.focusfortvs.data.repository.ChartBarEntry
import com.example.focusfortvs.data.repository.ChartRange
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// â”€â”€â”€ Palette â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
private val NavyDark   = Color(0xFF0B2545)
private val NavyMid    = Color(0xFF1565C0)
private val Teal       = Color(0xFF00BCD4)
private val Green      = Color(0xFF22C55E)
private val Amber      = Color(0xFFF59E0B)
private val Purple     = Color(0xFF8B5CF6)
private val SurfaceBg  = Color(0xFFF1F5F9)
private val CardBg     = Color.White
private val TextPri    = Color(0xFF0F172A)
private val TextSec    = Color(0xFF64748B)

private val modeColor = mapOf(
    "Student"       to Teal,
    "Normal"        to Green,
    "Office"        to NavyMid,
    "Digital Detox" to Color(0xFFEF4444),
)
private val modeEmoji = mapOf(
    "Student"       to "🎓",
    "Normal"        to "🧘",
    "Office"        to "💼",
    "Digital Detox" to "🔒",
)

// â”€â”€â”€ Screen â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
fun DashboardScreen(
    onBack: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    // Detail bottom sheet
    if (state.detailSheetBucketMillis != null) {
        SessionDetailSheet(
            bucketMillis    = state.detailSheetBucketMillis!!,
            range           = state.selectedRange,
            sessions        = state.detailSessions,
            isLoading       = state.isDetailLoading,
            onDismiss       = { viewModel.dismissDetail() }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBg)
    ) {
        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = NavyMid, strokeWidth = 3.dp
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = WindowInsets.navigationBars.asPaddingValues().let { navPadding ->
                    PaddingValues(bottom = navPadding.calculateBottomPadding() + 24.dp)
                }
            ) {
                // â”€â”€ Hero header â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                item { DashboardHeader(todayMinutes = state.todayFocusMinutes, onBack = onBack) }

                // â”€â”€ Top 5 stat cards (2+2+1) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                item {
                    Spacer(Modifier.height(20.dp))
                    HeroStatsGrid(
                        totalMinutes    = state.totalFocusMinutes,
                        streak          = state.currentStreak,
                        sessions        = state.completedSessions,
                        verifications   = state.studyVerifications,
                    )
                }

                // â”€â”€ Interactive chart â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                item {
                    Spacer(Modifier.height(22.dp))
                    ChartCard(
                        selectedRange  = state.selectedRange,
                        chartData      = state.chartData,
                        isChartLoading = state.isChartLoading,
                        onRangeChange  = { viewModel.onRangeSelected(it) },
                        onBarTapped    = { viewModel.onBarTapped(it) }
                    )
                }

                // â”€â”€ Mode breakdown â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                if (state.modeBreakdown.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(22.dp))
                        ModeBreakdownCard(rows = state.modeBreakdown)
                    }
                }

                // â”€â”€ Motivational footer â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                item {
                    Spacer(Modifier.height(22.dp))
                    MotivationalFooter(
                        streak       = state.currentStreak,
                        totalMinutes = state.totalFocusMinutes,
                    )
                }
            }
        }
    }
}

// â”€â”€â”€ Header â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun DashboardHeader(todayMinutes: Long, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(NavyDark, NavyMid)))
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 28.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back",
                tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.padding(top = 52.dp)) {
            Text("Focus Dashboard", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold,
                color = Color.White, letterSpacing = 0.3.sp)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Timer, contentDescription = null,
                    tint = Teal, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(5.dp))
                Text(
                    "Today: ${formatMins(todayMinutes)} focused",
                    fontSize = 14.sp, color = Color.White.copy(alpha = 0.80f)
                )
            }
        }
    }
}

// â”€â”€â”€ Hero Stats Grid â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun HeroStatsGrid(
    totalMinutes: Long,
    streak: Int,
    sessions: Int,
    verifications: Int,
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Row 1: two big cards
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BigStatCard(
                modifier  = Modifier.weight(1f),
                icon      = Icons.Filled.Timer,
                iconColor = Teal,
                value     = formatMins(totalMinutes),
                label     = "Total Focus Time",
                sub       = "all time"
            )
            BigStatCard(
                modifier  = Modifier.weight(1f),
                icon      = Icons.Filled.LocalFireDepartment,
                iconColor = Amber,
                value     = "${streak}d",
                label     = "Current Streak",
                sub       = if (streak == 0) "start today!" else "keep it going ðŸ”¥"
            )
        }
        // Row 2: two smaller cards
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SmallStatCard(
                modifier  = Modifier.weight(1f),
                icon      = Icons.Filled.EmojiEvents,
                iconColor = Purple,
                value     = "$sessions",
                label     = "Sessions Done"
            )
            SmallStatCard(
                modifier  = Modifier.weight(1f),
                icon      = Icons.Filled.School,
                iconColor = Green,
                value     = "$verifications",
                label     = "Study Verified"
            )
        }
    }
}

@Composable
private fun BigStatCard(
    modifier: Modifier,
    icon: ImageVector,
    iconColor: Color,
    value: String,
    label: String,
    sub: String,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp)) }
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = TextPri)
            Column {
                Text(label, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = TextPri)
                Text(sub, fontSize = 11.sp, color = TextSec)
            }
        }
    }
}

@Composable
private fun SmallStatCard(
    modifier: Modifier,
    icon: ImageVector,
    iconColor: Color,
    value: String,
    label: String,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp)) }
            Column {
                Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = TextPri)
                Text(label, fontSize = 10.sp, color = TextSec, maxLines = 1)
            }
        }
    }
}

// â”€â”€â”€ Interactive Chart Card â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun ChartCard(
    selectedRange: ChartRange,
    chartData: List<ChartBarEntry>,
    isChartLoading: Boolean,
    onRangeChange: (ChartRange) -> Unit,
    onBarTapped: (Long) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Title + range toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Focus Over Time", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPri)
                    Text("Minutes per period", fontSize = 11.sp, color = TextSec)
                }
                RangeToggle(selected = selectedRange, onSelect = onRangeChange)
            }

            Spacer(Modifier.height(20.dp))

            // Chart body
            if (isChartLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NavyMid, strokeWidth = 2.dp, modifier = Modifier.size(32.dp))
                }
            } else if (chartData.isEmpty() || chartData.all { it.minutes == 0 }) {
                EmptyChartState()
            } else {
                VicoBarChart(
                    data       = chartData,
                    range      = selectedRange,
                    onBarTap   = onBarTapped
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "â†‘ Tap a bar to see session details",
                    fontSize = 10.sp, color = TextSec,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun RangeToggle(selected: ChartRange, onSelect: (ChartRange) -> Unit) {
    val options = listOf(
        ChartRange.DAILY    to "Day",
        ChartRange.MONTHLY  to "Month",
        ChartRange.YEARLY   to "Year",
        ChartRange.ALL_TIME to "All",
    )
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFF1F5F9))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEach { (range, label) ->
            val isSelected = selected == range
            val bg by animateColorAsState(
                targetValue = if (isSelected) NavyDark else Color.Transparent,
                animationSpec = tween(200)
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else TextSec,
                animationSpec = tween(200)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(bg)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelect(range) }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = textColor)
            }
        }
    }
}

@Composable
private fun VicoBarChart(
    data: List<ChartBarEntry>,
    range: ChartRange,
    onBarTap: (Long) -> Unit,
) {
    val maxMinutes = data.maxOf { it.minutes }.coerceAtLeast(1).toFloat()

    // Animate bars growing up on first draw / data change
    val progress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(700, easing = FastOutSlowInEasing))
    }
    val anim by progress.asState()

    // Track which bar is highlighted (selected)
    var selectedIdx by remember(data) { mutableStateOf<Int?>(null) }

    // Decide how many axis labels to show
    val maxLabels = when (range) {
        ChartRange.DAILY   -> 7
        ChartRange.MONTHLY -> 6
        else               -> 5
    }
    val step = (data.size / maxLabels).coerceAtLeast(1)

    // We need pixel measurements â€” use a BoxWithConstraints
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        val totalWidthPx = constraints.maxWidth.toFloat()
        val totalHeightPx = constraints.maxHeight.toFloat()
        val labelAreaPx  = 28f  // px reserved at bottom for x-labels
        val chartHeightPx = totalHeightPx - labelAreaPx

        val barCount = data.size
        val barWidthPx = (totalWidthPx / barCount) * 0.55f

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(data) {
                    detectTapGestures { offset ->
                        val barSlotW = totalWidthPx / barCount
                        val idx = (offset.x / barSlotW).toInt().coerceIn(data.indices)
                        selectedIdx = idx
                        onBarTap(data[idx].bucketMillis)
                    }
                }
        ) {
            data.forEachIndexed { idx, entry ->
                val slotW  = totalWidthPx / barCount
                val centerX = slotW * idx + slotW / 2f
                val barLeft = centerX - barWidthPx / 2f
                val fraction = (entry.minutes.toFloat() / maxMinutes) * anim
                val barHeightPx = (chartHeightPx * 0.85f * fraction).coerceAtLeast(if (entry.minutes > 0) 4f else 0f)
                val barTop = chartHeightPx - barHeightPx

                val isSelected = selectedIdx == idx
                val isToday    = range == ChartRange.DAILY && idx == data.lastIndex

                val barColor = when {
                    isSelected -> Amber
                    isToday    -> Teal
                    else       -> Color(0xFF90CAF9)
                }

                // Selection glow ring
                if (isSelected && barHeightPx > 0) {
                    drawRoundRect(
                        color  = Amber.copy(alpha = 0.2f),
                        topLeft = Offset(barLeft - 4f, barTop - 4f),
                        size   = Size(barWidthPx + 8f, barHeightPx + 8f),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                }

                // Bar fill
                if (barHeightPx > 0) {
                    drawRoundRect(
                        brush  = Brush.verticalGradient(
                            colors = listOf(barColor, barColor.copy(alpha = 0.6f)),
                            startY = barTop,
                            endY   = chartHeightPx
                        ),
                        topLeft = Offset(barLeft, barTop),
                        size   = Size(barWidthPx, barHeightPx),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                }
            }
        }

        // X-axis labels (drawn in Compose, not Canvas, for text rendering quality)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            data.forEachIndexed { idx, entry ->
                val showLabel = idx % step == 0 || idx == data.lastIndex
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (showLabel) {
                        Text(
                            text = formatBucketLabel(entry.bucketMillis, range),
                            fontSize = 9.sp,
                            color = if (idx == data.lastIndex && range == ChartRange.DAILY) Teal else TextSec,
                            fontWeight = if (idx == data.lastIndex && range == ChartRange.DAILY)
                                FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

// Fallback if Vico API changes â€” plain Compose bars
@Composable
private fun EmptyChartState() {
    Box(
        modifier = Modifier.fillMaxWidth().height(160.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("ðŸ“Š", fontSize = 40.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "No focus sessions yet.\nStart a session and come back!",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSec, textAlign = TextAlign.Center
            )
        }
    }
}

// â”€â”€â”€ Mode Breakdown Card â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun ModeBreakdownCard(rows: List<ModeFocusRow>) {
    val totalMillis = rows.sumOf { it.totalMillis }.coerceAtLeast(1L).toFloat()

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("How You Focus", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPri)
            Text("Time split across modes", fontSize = 11.sp, color = TextSec)
            Spacer(Modifier.height(12.dp))

            rows.forEach { row ->
                val fraction = row.totalMillis / totalMillis
                val pct = (fraction * 100).toInt()
                val color = modeColor[row.mode] ?: Teal
                val emoji = modeEmoji[row.mode] ?: "âš¡"
                val minutes = row.totalMillis / 60_000L

                val animFraction = remember { androidx.compose.animation.core.Animatable(0f) }
                LaunchedEffect(fraction) {
                    animFraction.snapTo(0f)
                    animFraction.animateTo(fraction,
                        animationSpec = tween(700, easing = androidx.compose.animation.core.FastOutSlowInEasing))
                }
                val animF by animFraction.asState()

                Column(modifier = Modifier.padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(emoji, fontSize = 16.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Default)
                        Spacer(Modifier.width(8.dp))
                        Text(row.mode, style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold, color = TextPri, modifier = Modifier.weight(1f))
                        Text("${formatMins(minutes)} Â· $pct%", fontSize = 11.sp, color = TextSec)
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth().height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(color.copy(alpha = 0.12f))
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(animF).fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(Brush.horizontalGradient(listOf(color.copy(0.7f), color)))
                        )
                    }
                }
            }
        }
    }
}

// â”€â”€â”€ Motivational Footer â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun MotivationalFooter(streak: Int, totalMinutes: Long) {
    val message = when {
        totalMinutes == 0L     -> "Your focus journey starts today. ðŸš€"
        streak >= 30           -> "30-day streak! You're unstoppable. 🏆"
        streak >= 7            -> "One full week of focus. You're building a habit! ðŸ”¥"
        streak >= 3            -> "3 days strong. Momentum is building! 🔥"
        totalMinutes >= 1000   -> "${totalMinutes / 60}+ hours of deep work. Impressive! 💼"
        else                   -> "Every minute counts. Keep showing up! 🎯"
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(NavyDark, NavyMid)))
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null,
                    tint = Amber, modifier = Modifier.size(28.dp))
                Text(
                    message, color = Color.White,
                    fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    lineHeight = 20.sp, modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// â”€â”€â”€ Detail Bottom Sheet â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun SessionDetailSheet(
    bucketMillis: Long,
    range: ChartRange,
    sessions: List<SessionEntity>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardBg,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
        ) {
            // Sheet header
            Text(
                text = formatBucketHeader(bucketMillis, range),
                fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = TextPri
            )

            if (!isLoading && sessions.isNotEmpty()) {
                val totalMs = sessions.sumOf {
                    (it.endTimeMillis ?: it.startTimeMillis) - it.startTimeMillis
                }
                Text(
                    "Total: ${formatMins(totalMs / 60_000L)}",
                    fontSize = 13.sp, color = NavyMid, fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(16.dp))

            if (isLoading) {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NavyMid, strokeWidth = 2.dp, modifier = Modifier.size(30.dp))
                }
            } else if (sessions.isEmpty()) {
                Text("No sessions recorded for this period.",
                    color = TextSec, modifier = Modifier.padding(vertical = 24.dp))
            } else {
                sessions.forEach { session ->
                    DetailSessionRow(session)
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DetailSessionRow(session: SessionEntity) {
    val color = modeColor[session.mode] ?: Teal
    val emoji = modeEmoji[session.mode] ?: "âš¡"
    val durationMs = (session.endTimeMillis ?: session.startTimeMillis) - session.startTimeMillis
    val durationMin = durationMs / 60_000L
    val timeLabel = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(session.startTimeMillis))

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(emoji, fontSize = 20.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Default)
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(session.mode, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPri)
                if (session.isLockedOut) {
                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFFFEBEE)) {
                        Text("locked", fontSize = 9.sp, color = Color(0xFFEF4444),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                    }
                }
            }
            if (session.studyTopic.isNotBlank()) {
                Text(session.studyTopic, fontSize = 11.sp, color = TextSec,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(timeLabel, fontSize = 10.sp, color = TextSec.copy(alpha = 0.7f))
        }
        Surface(shape = RoundedCornerShape(20.dp), color = color.copy(alpha = 0.10f)) {
            Text("${durationMin}m", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                color = color, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
        }
    }
}

// â”€â”€â”€ Helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

private fun formatMins(minutes: Long): String = when {
    minutes <= 0   -> "0m"
    minutes < 60   -> "${minutes}m"
    minutes % 60 == 0L -> "${minutes / 60}h"
    else           -> "${minutes / 60}h ${minutes % 60}m"
}

private fun formatBucketLabel(millis: Long, range: ChartRange): String {
    val cal = Calendar.getInstance().apply { timeInMillis = millis }
    return when (range) {
        ChartRange.DAILY    -> SimpleDateFormat("d/M", Locale.getDefault()).format(Date(millis))
        ChartRange.MONTHLY  -> SimpleDateFormat("MMM", Locale.getDefault()).format(Date(millis))
        ChartRange.YEARLY, ChartRange.ALL_TIME ->
            "${cal.get(Calendar.YEAR)}"
    }
}

private fun formatBucketHeader(millis: Long, range: ChartRange): String = when (range) {
    ChartRange.DAILY    -> SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date(millis))
    ChartRange.MONTHLY  -> SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(millis))
    ChartRange.YEARLY, ChartRange.ALL_TIME ->
        Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.YEAR).toString()
}

