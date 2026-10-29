package com.example.focusfortvs.ui.student

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.focusfortvs.system.hasOverlayPermission
import com.example.focusfortvs.system.hasUsageAccess
import com.example.focusfortvs.ui.viewmodel.ProfileViewModel
import com.example.focusfortvs.ui.viewmodel.SessionViewModel
import com.example.focusfortvs.ui.viewmodel.TopicVerificationUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentSetupScreen(
    onBack: () -> Unit = {},
    onSessionStarted: () -> Unit,
    profileViewModel: ProfileViewModel = hiltViewModel(),
    sessionViewModel: SessionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var usageAccessEnabled by remember { mutableStateOf(hasUsageAccess(context)) }
    var overlayGranted by remember { mutableStateOf(hasOverlayPermission(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                usageAccessEnabled = hasUsageAccess(context)
                overlayGranted = hasOverlayPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var usePreviousApps by remember { mutableStateOf(true) }
    var selectedPackages by remember { mutableStateOf<Set<String>>(emptySet()) }
    var previousAppCount by remember { mutableIntStateOf(0) }

    val uiState by sessionViewModel.uiState.collectAsState()
    val studyTopic = uiState.studyTopic

    val packageManager = context.packageManager

    // Loaded on IO thread to avoid blocking first composition → eliminates stutter
    var installedApps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }

    LaunchedEffect(Unit) {
        val apps = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            packageManager.queryIntentActivities(launcherIntent, 0)
                .mapNotNull { resolveInfo -> resolveInfo.toAppEntry(packageManager, context.packageName) }
                .distinctBy { it.packageName }
                .sortedBy { it.appLabel.lowercase() }
        }
        installedApps = apps
    }

    LaunchedEffect(Unit) {
        profileViewModel.setCurrentMode("Student")
        val blockedList = profileViewModel.getBlockedAppsForMode("Student")
        selectedPackages = blockedList.map { it.packageName }.toSet()
        previousAppCount = blockedList.size
    }

    val topicVerification = uiState.topicVerification
    val isTopicVerified = topicVerification is TopicVerificationUiState.Verified
    val canStart = isTopicVerified && usageAccessEnabled && overlayGranted

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Student Mode", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        Text("Configure your study session", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF0F172A)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { paddingValues ->
        // IMPORTANT: The entire screen is one root LazyColumn to prevent nested scroll jank
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Study Topic Input ──────────────────────────────────────────────
            item {
                SectionCard(title = "What will you study?") {
                    when (topicVerification) {

                        // ─── IDLE / INVALID: show editable text field ────────
                        is TopicVerificationUiState.Idle,
                        is TopicVerificationUiState.Invalid -> {
                            val isInvalid = topicVerification is TopicVerificationUiState.Invalid
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = studyTopic,
                                    onValueChange = { sessionViewModel.setStudyTopic(it) },
                                    placeholder = {
                                        Text(
                                            "e.g. Biology – Cell Division, Python OOP",
                                            color = Color(0xFF8892B0)
                                        )
                                    },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    isError = isInvalid,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF0EA5E9),
                                        unfocusedBorderColor = if (isInvalid) Color(0xFFEF4444) else Color(0xFFE2E8F0),
                                        errorBorderColor = Color(0xFFEF4444),
                                        cursorColor = Color(0xFF0EA5E9),
                                        focusedTextColor = Color(0xFF0F172A),
                                        unfocusedTextColor = Color(0xFF0F172A)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                // Show AI rejection reason
                                if (isInvalid) {
                                    val reason = (topicVerification as TopicVerificationUiState.Invalid).reason
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFFFFEBEE))
                                            .padding(horizontal = 12.dp, vertical = 10.dp)
                                    ) {
                                        Text("❌", fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = reason,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFB71C1C),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                // Verify button
                                Button(
                                    onClick = { sessionViewModel.verifyTopic() },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    enabled = studyTopic.isNotBlank(),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF0B2545),
                                        disabledContainerColor = Color(0xFFE2E8F0)
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.VerifiedUser,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isInvalid) "Re-verify Topic" else "Verify Topic with AI",
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (studyTopic.isNotBlank()) Color.White else Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }

                        // ─── VERIFYING: spinner state ────────────────────────
                        is TopicVerificationUiState.Verifying -> {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFF0EA5E9),
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "AI is verifying your topic…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF64748B),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "\"$studyTopic\"",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F172A),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // ─── VERIFIED: locked chip with edit option ──────────
                        is TopicVerificationUiState.Verified -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(Color(0xFF22C55E).copy(alpha = 0.10f), Color(0xFF16A34A).copy(alpha = 0.08f))
                                        )
                                    )
                                    .border(1.5.dp, Color(0xFF22C55E).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF22C55E).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF22C55E),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Topic Verified ✓",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF16A34A)
                                    )
                                    Text(
                                        text = studyTopic,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                                IconButton(
                                    onClick = { sessionViewModel.resetTopicVerification() },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = "Change topic",
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Study Duration Picker (locked until topic verified) ────────────
            if (isTopicVerified) {
                item {
                    SectionCard(title = "Study Duration") {
                        val durations = listOf(5, 15, 30, 45, 60)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(durations.size) { index ->
                                val mins = durations[index]
                                val selected = uiState.selectedDurationMinutes == mins
                                FilterChip(
                                    selected = selected,
                                    onClick = { sessionViewModel.setDuration(mins) },
                                    label = { Text("$mins min", color = if (selected) Color.White else Color(0xFF64748B)) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF0EA5E9),
                                        containerColor = Color.White
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selected,
                                        borderColor = Color(0xFFE2E8F0),
                                        selectedBorderColor = Color(0xFF0EA5E9)
                                    )
                                )
                            }
                            item {
                                val selected = uiState.selectedDurationMinutes == null
                                FilterChip(
                                    selected = selected,
                                    onClick = { sessionViewModel.setDuration(null) },
                                    label = { Text("No limit", color = if (selected) Color.White else Color(0xFF64748B)) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF0EA5E9),
                                        containerColor = Color.White
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selected,
                                        borderColor = Color(0xFFE2E8F0),
                                        selectedBorderColor = Color(0xFF0EA5E9)
                                    )
                                )
                            }
                        }
                    }
                }
            } else {
                item {
                    LockedSectionPlaceholder(
                        emoji = "⏱️",
                        label = "Study Duration",
                        hint = "Verify your topic above to unlock"
                    )
                }
            }

            // ── App Selection Toggle (locked until topic verified) ─────────────
            if (isTopicVerified) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Use previous app list",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    if (usePreviousApps) "Blocking $previousAppCount saved apps"
                                    else "Select new apps below",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF64748B),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            Switch(
                                checked = usePreviousApps,
                                onCheckedChange = { usePreviousApps = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF0EA5E9),
                                    uncheckedThumbColor = Color(0xFF94A3B8),
                                    uncheckedTrackColor = Color(0xFFE2E8F0),
                                    uncheckedBorderColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
            } else {
                item {
                    LockedSectionPlaceholder(
                        emoji = "📱",
                        label = "App Blocking",
                        hint = "Verify your topic above to unlock"
                    )
                }
            }

            // ── App list (only shown when topic verified AND usePreviousApps = false) ──
            if (isTopicVerified && !usePreviousApps) {
                item {
                    Text(
                        "Select Apps to Block",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                items(installedApps, key = { it.packageName }) { app ->
                    val checked = selectedPackages.contains(app.packageName)
                    StudentAppRowItem(
                        app = app,
                        checked = checked,
                        onToggle = {
                            val isChecked = !checked
                            val next = if (isChecked) selectedPackages + app.packageName
                                       else selectedPackages - app.packageName
                            selectedPackages = next
                            scope.launch {
                                if (isChecked) {
                                    profileViewModel.saveBlockedAppSelection("Student", app.packageName, app.appLabel)
                                    previousAppCount++
                                } else {
                                    profileViewModel.removeBlockedAppSelection("Student", app.packageName)
                                    previousAppCount--
                                }
                            }
                        }
                    )
                }
            }

            // ── Start Session / Permission Buttons ─────────────────────────────
            item {
                Spacer(modifier = Modifier.height(8.dp))

                when {
                    uiState.isSessionActive -> {
                        Button(
                            onClick = { sessionViewModel.stopSession() },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                        ) {
                            Text("Stop Active Session", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    !usageAccessEnabled -> {
                        PermissionWarningButton(
                            label = "Enable Usage Access (Required)",
                            onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        )
                    }

                    !overlayGranted -> {
                        PermissionWarningButton(
                            label = "Enable Display Over Other Apps (Required)",
                            onClick = {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        android.net.Uri.parse("package:${context.packageName}")
                                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        )
                    }

                    else -> {
                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()
                        val scale by animateFloatAsState(
                            targetValue = if (isPressed) 0.96f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                        )
                        Button(
                            onClick = {
                                sessionViewModel.startSession()
                                onSessionStarted()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .scale(scale),
                            shape = RoundedCornerShape(28.dp),
                            enabled = canStart,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0EA5E9),
                                disabledContainerColor = Color(0xFFE2E8F0)
                            ),
                            interactionSource = interactionSource
                        ) {
                            Text(
                                text = when {
                                    !isTopicVerified -> "🔒 Verify your topic first"
                                    else -> "Start Focus Session"
                                },
                                fontWeight = FontWeight.Bold,
                                color = if (canStart) Color.White else Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp).navigationBarsPadding())
            }
        }
    }
}


@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF64748B)
            )
            content()
        }
    }
}

/**
 * A muted placeholder card shown when a section is locked behind topic verification.
 */
@Composable
private fun LockedSectionPlaceholder(emoji: String, label: String, hint: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(emoji, fontSize = 22.sp)
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFCBD5E1)
                )
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCBD5E1)
                )
            }
        }
    }
}

@Composable
private fun PermissionWarningButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
    ) {
        Text(text = label, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun StudentAppRowItem(
    app: AppEntry,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable { onToggle() },
        colors = CardDefaults.cardColors(
            containerColor = if (checked) Color(0xFF0EA5E9).copy(alpha = 0.05f) else Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (checked) Color(0xFF0EA5E9).copy(alpha = 0.3f) else Color(0xFFE2E8F0)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = BitmapPainter(app.iconBitmap.asImageBitmap()),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = app.appLabel,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal,
                color = Color(0xFF0F172A),
                modifier = Modifier.weight(1f)
            )
            
            val circleColor by animateColorAsState(
                targetValue = if (checked) Color(0xFF0EA5E9) else Color.Transparent,
                animationSpec = tween(200)
            )
            val iconTint by animateColorAsState(
                targetValue = if (checked) Color.White else Color(0xFFCBD5E1),
                animationSpec = tween(200)
            )
            val circleBorder = if (checked) Color.Transparent else Color(0xFFCBD5E1)

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(circleColor)
                    .border(1.dp, circleBorder, androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (checked) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Selected",
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Data helpers (duplicated intentionally – scoped to this file)
// ─────────────────────────────────────────────────────────────────────────────

private data class AppEntry(
    val packageName: String,
    val appLabel: String,
    val iconBitmap: Bitmap,
)

private fun ResolveInfo.toAppEntry(
    packageManager: PackageManager,
    currentPackageName: String,
): AppEntry? {
    val activityInfo = activityInfo ?: return null
    val packageName = activityInfo.packageName ?: return null
    if (packageName == currentPackageName) return null
    val label = loadLabel(packageManager)?.toString().orEmpty()
    if (label.isBlank()) return null
    val icon = loadIcon(packageManager)?.toBitmap() ?: return null
    return AppEntry(packageName = packageName, appLabel = label, iconBitmap = icon)
}

private fun Drawable.toBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(
        intrinsicWidth.takeIf { it > 0 } ?: 48,
        intrinsicHeight.takeIf { it > 0 } ?: 48,
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap
}
