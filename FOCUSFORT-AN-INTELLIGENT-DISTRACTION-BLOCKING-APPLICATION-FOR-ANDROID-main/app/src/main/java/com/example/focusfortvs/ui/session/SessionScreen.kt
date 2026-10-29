package com.example.focusfortvs.ui.session

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.focusfortvs.security.PinRepository
import com.example.focusfortvs.system.hasOverlayPermission
import com.example.focusfortvs.system.hasUsageAccess
import com.example.focusfortvs.ui.pin.PinEntryDialog
import com.example.focusfortvs.ui.viewmodel.SessionViewModel

@Composable
fun SessionScreen(
    viewModel: SessionViewModel = hiltViewModel(),
) {
    val uiState       by viewModel.uiState.collectAsState()
    val context       = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val pinRepo = remember { PinRepository(context) }

    // Re-check permissions on every resume
    var usageAccessEnabled by remember { mutableStateOf(hasUsageAccess(context)) }
    var overlayGranted     by remember { mutableStateOf(hasOverlayPermission(context)) }

    // PIN guard dialog state
    var showStopPinDialog by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                usageAccessEnabled = hasUsageAccess(context)
                overlayGranted     = hasOverlayPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // ── PIN Entry Dialog ──────────────────────────────────────────────────────
    if (showStopPinDialog) {
        PinEntryDialog(
            title      = "Stop Session?",
            onSuccess  = {
                showStopPinDialog = false
                viewModel.stopSession()
            },
            onDismiss  = { showStopPinDialog = false },
            verifyPin  = { pin -> pinRepo.verifyPin(pin) }
        )
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text  = "Focus Session",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)
            )
            Text(
                text  = "Start a session in your current mode, then open a distracting app to trigger the lock screen.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )

            // ── Pulsing status card ───────────────────────────────────────────
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue   = 1f,
                targetValue    = 1.05f,
                animationSpec  = infiniteRepeatable(
                    animation   = tween(1000),
                    repeatMode  = RepeatMode.Reverse
                ),
                label = "pulseScale"
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(if (uiState.isSessionActive) pulseScale else 1f),
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.isSessionActive)
                        Color(0xFF00E5FF).copy(alpha = 0.1f)
                    else
                        MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (uiState.isSessionActive) 8.dp else 2.dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text  = "Selected mode: ${uiState.currentMode}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )

                    if (uiState.isSessionActive) {
                        Box(
                            modifier = Modifier
                                .padding(16.dp)
                                .size(80.dp)
                                .background(Color(0xFF0A192F), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🛡️", style = MaterialTheme.typography.headlineLarge)
                        }
                    }

                    Text(
                        text = when {
                            uiState.isSessionActive ->
                                "Session is active for ${uiState.activeSessionMode ?: uiState.currentMode}. Blocking is ON."
                            !usageAccessEnabled ->
                                "⚠️ Usage Access is OFF. Tap 'Enable Usage Access' below."
                            !overlayGranted ->
                                "⚠️ Display over other apps permission is required."
                            else ->
                                "Session is inactive. Blocking will begin after you tap Start session."
                        },
                        style     = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color     = if (uiState.isSessionActive) Color(0xFF0A192F)
                                    else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // ── Duration picker (only when inactive) ─────────────────────────
            if (!uiState.isSessionActive) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Session Duration", style = MaterialTheme.typography.titleMedium)
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            val durations = listOf(5, 15, 30, 45, 60)
                            items(durations.size) { index ->
                                val mins = durations[index]
                                FilterChip(
                                    selected = uiState.selectedDurationMinutes == mins,
                                    onClick  = { viewModel.setDuration(mins) },
                                    label    = { Text("$mins min") }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = uiState.selectedDurationMinutes == null,
                                    onClick  = { viewModel.setDuration(null) },
                                    label    = { Text("No limit") }
                                )
                            }
                        }
                    }
                }
            }

            // ── Action button ─────────────────────────────────────────────────
            if (uiState.isSessionActive) {
                // 🔒 STOP SESSION — PIN guarded
                Button(
                    onClick  = { showStopPinDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("🔒  Stop Session — PIN Required")
                }
            } else {
                when {
                    !usageAccessEnabled -> Button(
                        onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Enable Usage Access") }

                    !overlayGranted -> Button(
                        onClick = {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    android.net.Uri.parse("package:${context.packageName}")
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Enable Display Over Other Apps") }

                    else -> Button(
                        onClick  = viewModel::startSession,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Start Session") }
                }
            }

            Text(
                text  = "Leave this screen and open one of the apps you marked as distracting.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}