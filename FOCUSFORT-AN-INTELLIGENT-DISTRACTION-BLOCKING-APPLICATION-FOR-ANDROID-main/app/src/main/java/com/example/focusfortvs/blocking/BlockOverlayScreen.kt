package com.example.focusfortvs.blocking

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.content.pm.PackageManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import kotlin.math.min
import com.example.focusfortvs.data.repository.ProfileRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@EntryPoint
@InstallIn(SingletonComponent::class)
interface OverlayEntryPoint {
    fun profileRepository(): ProfileRepository
    fun azureOpenAIRepository(): com.example.focusfortvs.data.network.AzureOpenAIRepository
    fun sessionRepository(): com.example.focusfortvs.data.repository.SessionRepository
    fun normalModeRepository(): com.example.focusfortvs.data.repository.NormalModeRepository
    fun appWhitelistManager(): com.example.focusfortvs.blocking.AppWhitelistManager
}

@Composable
fun BlockOverlayScreen(
    mode: String,
    packageName: String,
    appLabel: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val entryPoint = remember {
        EntryPointAccessors.fromApplication(context.applicationContext, OverlayEntryPoint::class.java)
    }
    val profileRepository = entryPoint.profileRepository()
    val azureRepository = entryPoint.azureOpenAIRepository()
    val normalModeRepository = entryPoint.normalModeRepository()
    val sessionRepository = entryPoint.sessionRepository()
    val appWhitelistManager = entryPoint.appWhitelistManager()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0A192F) // Deep Navy corporate theme
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🛡️ FocusFort",
                fontSize = 20.sp,
                color = Color(0xFF00E5FF),
                letterSpacing = 2.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Access Restricted",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Mode: $mode",
                fontSize = 16.sp,
                color = Color(0xFFB0BEC5),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = appLabel,
                    fontSize = 20.sp,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }
            Spacer(modifier = Modifier.height(40.dp))

            when (mode) {
                "Student" -> {
                    var isLockedOut by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        val prefs = context.getSharedPreferences("focusfort_state", android.content.Context.MODE_PRIVATE)
                        isLockedOut = prefs.getBoolean("is_locked_out", false)
                    }
                    if (isLockedOut) {
                        StudentLockoutUI()
                    } else {
                        StudentModeUI(onDismiss)
                    }
                }
                "Normal" -> NormalModeUI(normalModeRepository, sessionRepository, appWhitelistManager, appLabel, packageName, onDismiss)
                "Office" -> OfficeModeUI(profileRepository, appLabel, packageName, onDismiss)
                "Digital Detox" -> DetoxModeUI(onDismiss)
                else -> NormalModeUI(normalModeRepository, sessionRepository, appWhitelistManager, appLabel, packageName, onDismiss)
            }
            
            // Global timer across all modes except those with strict internal logic
            if (mode != "Normal" && mode != "Digital Detox") {
                Spacer(modifier = Modifier.height(32.dp))
                Divider(color = Color.DarkGray, modifier = Modifier.fillMaxWidth(0.8f))
                Spacer(modifier = Modifier.height(16.dp))
                GlobalUnlockTimer(onDismiss)
            }
        }
    }
}

@Composable
fun GlobalUnlockTimer(onDismiss: () -> Unit) {
    var timeLeft by remember { mutableIntStateOf(30) }

    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            delay(1000L)
            timeLeft--
        }
    }

    Text(
        text = "Reflective Delay",
        fontSize = 16.sp,
        color = Color(0xFFB0BEC5)
    )
    Spacer(modifier = Modifier.height(12.dp))
    Button(
        onClick = onDismiss,
        enabled = timeLeft == 0,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF00E5FF),
            contentColor = Color(0xFF0A192F),
            disabledContainerColor = Color(0xFF1E293B),
            disabledContentColor = Color.Gray
        ),
        modifier = Modifier.fillMaxWidth(0.8f).height(56.dp),
        shape = RoundedCornerShape(28.dp)
    ) {
        Text(
            text = if (timeLeft > 0) "Emergency Unlock in ${timeLeft}s" else "Unlock App",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun NormalModeUI(
    normalModeRepository: com.example.focusfortvs.data.repository.NormalModeRepository,
    sessionRepository: com.example.focusfortvs.data.repository.SessionRepository,
    whitelistManager: com.example.focusfortvs.blocking.AppWhitelistManager,
    appLabel: String,
    packageName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isHardLocked by remember { mutableStateOf(false) }
    var remainingLockSeconds by remember { mutableLongStateOf(0L) }
    var remainingCooldownSeconds by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        remainingCooldownSeconds = normalModeRepository.getCooldownRemainingSeconds(packageName)
        if (remainingCooldownSeconds > 0) {
            while (remainingCooldownSeconds > 0) {
                delay(1000L)
                remainingCooldownSeconds--
            }
            
            // Cooldown complete
            val usageCount = normalModeRepository.incrementUsage(packageName)
            if (usageCount >= 5) {
                val session = sessionRepository.getActiveSession()
                val remainingSessionMillis = if (session != null && session.durationMinutes != null) {
                    val endMillis = session.startTimeMillis + (session.durationMinutes * 60 * 1000L)
                    val remaining = endMillis - System.currentTimeMillis()
                    if (remaining > 0) remaining else 0L
                } else {
                    Long.MAX_VALUE
                }
                val lockDurationMillis = min(10 * 60 * 1000L, remainingSessionMillis)
                normalModeRepository.applyHardLock(packageName, System.currentTimeMillis() + lockDurationMillis)
            }
            whitelistManager.grant(packageName, 3)
            onDismiss()
        } else {
            isHardLocked = normalModeRepository.isHardLocked(packageName)
            if (isHardLocked) {
                remainingLockSeconds = normalModeRepository.getRemainingHardLockSeconds(packageName)
                while (remainingLockSeconds > 0) {
                    delay(1000L)
                    remainingLockSeconds--
                }
                isHardLocked = false
            }
        }
    }

    if (remainingCooldownSeconds > 0) {
        val isVibrant = remainingCooldownSeconds > 15
        val targetColor = if (isVibrant) Color(0xFF4CAF50) else Color(0xFF424242)
        val animatedBgColor by animateColorAsState(
            targetValue = targetColor,
            animationSpec = tween(durationMillis = 2000), label = "bg_color"
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(animatedBgColor, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Preparing $appLabel",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = remainingCooldownSeconds.toString(),
                    color = Color.White,
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Breathe. Let the craving pass.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
        }
    } else if (isHardLocked) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Control your dopamine.",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Red,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "You've exhausted your attempts for $appLabel.",
                color = Color.LightGray,
                fontSize = 16.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Penalty Lock: ${remainingLockSeconds / 60}m ${remainingLockSeconds % 60}s remaining",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(32.dp))
            OutlinedButton(
                onClick = {
                    val startMain = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(startMain)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE57373)),
            ) {
                Text("✖ Close App")
            }
        }
    } else {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Normal Mode Active.",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Do you want a 3-minute session?",
                color = Color.LightGray,
                fontSize = 16.sp,
            )
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val intent = Intent(context, com.example.focusfortvs.ui.normal.NormalWorkspaceActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        putExtra("EXTRA_PACKAGE_NAME", packageName)
                        putExtra("EXTRA_APP_NAME", appLabel)
                    }
                    context.startActivity(intent)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03A9F4)),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text("Yes", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    val startMain = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(startMain)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE57373)),
            ) {
                Text("✖ Close App")
            }
        }
    }
}

@Composable
fun OfficeModeUI(profileRepository: ProfileRepository, appLabel: String, packageName: String, onDismiss: () -> Unit) {
    val context = LocalContext.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "💼 Work Mode Active. Stay focused.",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "You tried to open $appLabel during your work session.",
            color = Color.LightGray,
            fontSize = 14.sp,
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Emergency request — opens workspace
        Button(
            onClick = {
                val intent = Intent(context, com.example.focusfortvs.ui.office.OfficeWorkspaceActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra(com.example.focusfortvs.ui.office.OfficeWorkspaceActivity.EXTRA_PACKAGE_NAME, packageName)
                    putExtra(com.example.focusfortvs.ui.office.OfficeWorkspaceActivity.EXTRA_APP_LABEL, appLabel)
                }
                context.startActivity(intent)
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B35)),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
        ) {
            Text("🚨 Is it an Emergency? (Request Access)", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Close App — go home
        OutlinedButton(
            onClick = {
                val startMain = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(startMain)
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE57373)),
        ) {
            Text("✖ Close App")
        }
    }
}


@Composable
fun DetoxModeUI(onDismiss: () -> Unit) {
    val context = LocalContext.current
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Digital Detox Active",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Strict mode is active. You are only allowed to use your designated essential apps. There are no bypasses.",
            color = Color.LightGray,
            fontSize = 16.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedButton(
            onClick = {
                val startMain = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(startMain)
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE57373)),
        ) {
            Text("✖ Go to Home Screen")
        }
    }
}

@Composable
fun StudentModeUI(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 15-second hidden penalty timer — if student doesn't click within 15s, record a behavior penalty
    var penaltyRecorded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(15_000L)
        if (!penaltyRecorded) {
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext, OverlayEntryPoint::class.java
            )
            coroutineScope.launch {
                entryPoint.sessionRepository().incrementBehaviorPenalty()
            }
            penaltyRecorded = true
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "⏰ Focus Session Active",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "You're trying to open a distraction app. Prove you've been studying to get a break.",
            color = Color.LightGray,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))

        // "I have studied" — proceed to workspace (penalty-free if within 15s)
        Button(
            onClick = {
                penaltyRecorded = true // cancel penalty timer
                val intent = Intent(context, com.example.focusfortvs.MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("OPEN_WORKSPACE", true)
                }
                context.startActivity(intent)
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
        ) {
            Text("✅ I Have Studied", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // "Close App" — dismiss overlay and return home
        OutlinedButton(
            onClick = {
                penaltyRecorded = true
                val startMain = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(startMain)
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE57373))
        ) {
            Text("✖ Close App")
        }
    }
}

@Composable
fun StudentLockoutUI() {
    Text(
        text = "🔒 Full Lockout Active",
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFFE53935)
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "You have exhausted all 3 attempts for this blocked app.\nAll apps are locked until your session timer ends.",
        color = Color.LightGray,
        fontSize = 16.sp,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = "Go back to studying!",
        color = Color(0xFFFFB74D),
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold
    )
}
