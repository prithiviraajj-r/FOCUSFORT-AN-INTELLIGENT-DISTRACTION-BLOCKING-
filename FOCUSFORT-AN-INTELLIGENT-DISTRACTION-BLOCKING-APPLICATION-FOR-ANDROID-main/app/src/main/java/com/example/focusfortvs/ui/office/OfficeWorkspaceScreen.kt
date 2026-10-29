package com.example.focusfortvs.ui.office

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay

// ── Color palette ────────────────────────────────────────────────────────────
private val BgDeep       = Color(0xFF0D0F14)
private val BgCard       = Color(0xFF161B27)
private val AccentOrange = Color(0xFFFF6B35)
private val AccentGold   = Color(0xFFFFB74D)
private val AccentGreen  = Color(0xFF4CAF50)
private val AccentRed    = Color(0xFFEF5350)
private val TextPrimary  = Color(0xFFF1F5FB)
private val TextSecond   = Color(0xFF8A9BB8)
private val BorderColor  = Color(0xFF2A3450)

@Composable
fun OfficeWorkspaceScreen(
    appName: String,
    packageName: String,
    onGoHome: () -> Unit,
    viewModel: OfficeWorkspaceViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Initialize with passed args
    LaunchedEffect(appName, packageName) {
        viewModel.initialize(appName, packageName)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
    ) {
        AnimatedContent(
            targetState = uiState,
            transitionSpec = { fadeIn() togetherWith fadeOut() }
        ) { state ->
            when (state) {
                is OfficeWorkspaceUiState.Idle -> {
                    IdleScreen(
                        state = state,
                        packageName = packageName,
                        onSubmit = { reason, minutes ->
                            viewModel.submitRequest(appName, packageName, reason, minutes)
                        },
                        onClose = onGoHome,
                    )
                }

                is OfficeWorkspaceUiState.Evaluating -> EvaluatingScreen()

                is OfficeWorkspaceUiState.Approved -> {
                    ApprovedScreen(
                        state = state,
                        onLaunchApp = {
                            // Launch the whitelisted app
                            val launch = context.packageManager
                                .getLaunchIntentForPackage(state.packageName)
                                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            if (launch != null) context.startActivity(launch)
                            onGoHome()
                        }
                    )
                }

                is OfficeWorkspaceUiState.Rejected -> {
                    RejectedScreen(
                        nudgeMessage = state.nudgeMessage,
                        onBackToWork = onGoHome,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Idle — main input screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun IdleScreen(
    state: OfficeWorkspaceUiState.Idle,
    packageName: String,
    onSubmit: (reason: String, minutes: Int) -> Unit,
    onClose: () -> Unit,
) {
    var reason by remember { mutableStateOf("") }
    var selectedMinutes by remember { mutableIntStateOf(1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(Modifier.height(16.dp))

        // ── Header ────────────────────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(AccentOrange.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .border(1.dp, AccentOrange.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = "${state.appName} Access Request",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
                Text(
                    text = "Work Mode Active",
                    fontSize = 13.sp,
                    color = AccentOrange,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        // Divider
        HorizontalDivider(color = BorderColor)

        if (state.sessionDisabled) {
            DisabledCard()
        } else {
            // ── Reason input ──────────────────────────────────────────────────
            SectionLabel("Why do you need ${state.appName} right now?")
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                placeholder = {
                    Text(
                        "State your work requirement or emergency reason...",
                        color = TextSecond,
                        fontSize = 14.sp
                    )
                },
                minLines = 3,
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentOrange,
                    unfocusedBorderColor = BorderColor,
                    focusedContainerColor = BgCard,
                    unfocusedContainerColor = BgCard,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = AccentOrange,
                ),
                shape = RoundedCornerShape(12.dp),
            )

            // ── Duration selector ─────────────────────────────────────────────
            SectionLabel("Temporary access duration")
            DurationSelectorCard(
                selectedMinutes = selectedMinutes,
                maxMinutes = state.maxMinutes,
                onSelect = { selectedMinutes = it },
            )

            // ── AI notice ─────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1F2E), RoundedCornerShape(10.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = AccentGold, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Your reason is evaluated by AI. Frivolous or recreational requests will be rejected.",
                    fontSize = 12.sp,
                    color = TextSecond,
                    lineHeight = 18.sp,
                )
            }

            // ── Action buttons ────────────────────────────────────────────────
            Button(
                onClick = { onSubmit(reason, selectedMinutes) },
                enabled = reason.trim().length >= 10,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentOrange,
                    disabledContainerColor = Color(0xFF2A3450),
                    contentColor = Color.White,
                    disabledContentColor = TextSecond,
                ),
            ) {
                Text("Submit for AI Evaluation", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            OutlinedButton(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentRed.copy(alpha = 0.5f)),
            ) {
                Text("Close App", fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DurationSelectorCard(selectedMinutes: Int, maxMinutes: Int, onSelect: (Int) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Duration", color = TextSecond, fontSize = 13.sp)
                Text(
                    "$selectedMinutes min${if (selectedMinutes > 1) "s" else ""}",
                    color = AccentOrange,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                )
            }
            Spacer(Modifier.height(8.dp))
            Slider(
                value = selectedMinutes.toFloat(),
                onValueChange = { onSelect(it.toInt()) },
                valueRange = 1f..maxMinutes.toFloat(),
                steps = (maxMinutes - 2).coerceAtLeast(0),
                colors = SliderDefaults.colors(
                    thumbColor = AccentOrange,
                    activeTrackColor = AccentOrange,
                    inactiveTrackColor = BorderColor,
                ),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("1 min", color = TextSecond, fontSize = 12.sp)
                Text("$maxMinutes mins (max)", color = TextSecond, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = TextSecond,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

@Composable
private fun DisabledCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1215), RoundedCornerShape(12.dp))
            .border(1.dp, AccentRed.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = AccentRed, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(12.dp))
            Text("Insufficient Time Remaining", color = AccentRed, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "Your work session has less than 1 minute remaining. Access requests are disabled.",
                color = TextSecond,
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Evaluating screen — AI spinner
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EvaluatingScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(
            color = AccentOrange,
            strokeWidth = 3.dp,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.height(24.dp))
        Text("AI is evaluating your request...", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text("This usually takes a few seconds.", color = TextSecond, fontSize = 14.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Approved screen — 2-second countdown
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ApprovedScreen(state: OfficeWorkspaceUiState.Approved, onLaunchApp: () -> Unit) {
    var countdown by remember { mutableIntStateOf(2) }

    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
        onLaunchApp()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Glow ring around check icon
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    Brush.radialGradient(listOf(AccentGreen.copy(alpha = 0.25f), Color.Transparent)),
                    RoundedCornerShape(60.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = AccentGreen,
                modifier = Modifier.size(72.dp),
            )
        }
        Spacer(Modifier.height(28.dp))
        Text("Access Approved!", color = AccentGreen, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(10.dp))
        Text(
            "${state.appName} is unlocked for ${state.grantedMinutes} minute${if (state.grantedMinutes > 1) "s" else ""}.",
            color = TextPrimary,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "The overlay will re-engage automatically when time expires.",
            color = TextSecond,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(40.dp))
        // Countdown circle
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(BgCard, RoundedCornerShape(36.dp))
                .border(2.dp, AccentGreen, RoundedCornerShape(36.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("$countdown", color = AccentGreen, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        Text("Launching ${state.appName}...", color = TextSecond, fontSize = 13.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Rejected screen — nudge message
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RejectedScreen(nudgeMessage: String, onBackToWork: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Warning glow
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    Brush.radialGradient(listOf(AccentRed.copy(alpha = 0.2f), Color.Transparent)),
                    RoundedCornerShape(60.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text("🚫", fontSize = 56.sp)
        }
        Spacer(Modifier.height(28.dp))
        Text("Request Denied", color = AccentRed, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(16.dp))

        // Nudge card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1215)),
            border = androidx.compose.foundation.BorderStroke(1.dp, AccentRed.copy(alpha = 0.35f)),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("AI Verdict:", color = AccentRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = nudgeMessage,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    textAlign = TextAlign.Start,
                )
            }
        }

        Spacer(Modifier.height(40.dp))
        Button(
            onClick = onBackToWork,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
        ) {
            Text("Back to Work 💪", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
        }
    }
}
