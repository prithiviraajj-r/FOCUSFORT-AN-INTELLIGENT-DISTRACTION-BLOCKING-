package com.example.focusfortvs.ui.navigation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.focusfortvs.R
import com.example.focusfortvs.security.PinRepository
import com.example.focusfortvs.system.hasOverlayPermission
import com.example.focusfortvs.system.hasUsageAccess
import com.example.focusfortvs.ui.dashboard.DashboardScreen
import com.example.focusfortvs.ui.detox.DetoxAdvisoryScreen
import com.example.focusfortvs.ui.pin.PinChangeDialog
import com.example.focusfortvs.ui.pin.PinEntryDialog
import com.example.focusfortvs.ui.pin.PinSetupScreen
import com.example.focusfortvs.ui.session.SessionScreen
import com.example.focusfortvs.ui.splash.CinematicSplashScreen
import com.example.focusfortvs.ui.splash.GenericModeSplash
import com.example.focusfortvs.ui.student.StudentModeSplash
import com.example.focusfortvs.ui.student.StudentSetupScreen
import com.example.focusfortvs.ui.student.StudentWorkspaceScreen
import com.example.focusfortvs.ui.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// Navigation Host
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
fun FocusFortNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = FocusFortDestination.Welcome.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            fadeIn(animationSpec = tween(280)) + slideInHorizontally(
                initialOffsetX = { it / 4 },
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(200)) + slideOutHorizontally(
                targetOffsetX = { -it / 6 },
                animationSpec = tween(200)
            )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(280)) + slideInHorizontally(
                initialOffsetX = { -it / 4 },
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
            )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(200)) + slideOutHorizontally(
                targetOffsetX = { it / 4 },
                animationSpec = tween(200)
            )
        }
    ) {
        // Cinematic Splash Screen
        composable(FocusFortDestination.Splash.route) {
            val context = LocalContext.current
            val prefs = context.getSharedPreferences("focusfort_prefs", android.content.Context.MODE_PRIVATE)
            val isFirstLaunch = prefs.getBoolean("is_first_launch", true)
            
            CinematicSplashScreen(
                onSplashFinished = {
                    val nextRoute = if (isFirstLaunch) FocusFortDestination.Welcome.route else FocusFortDestination.ModeSelect.route
                    navController.navigate(nextRoute) {
                        popUpTo(FocusFortDestination.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // Welcome / onboarding highlights (first-launch only)
        composable(FocusFortDestination.Welcome.route) {
            val context = LocalContext.current
            WelcomeScreen(
                onGetStarted = {
                    navController.navigate(FocusFortDestination.Privacy.route) {
                        popUpTo(FocusFortDestination.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        // Privacy Policy & Terms consent gate (new users must accept before onboarding)
        composable(FocusFortDestination.Privacy.route) {
            com.example.focusfortvs.ui.onboarding.PrivacyConsentScreen(
                onAccepted = {
                    navController.navigate(FocusFortDestination.Onboarding.route) {
                        popUpTo(FocusFortDestination.Privacy.route) { inclusive = true }
                    }
                }
            )
        }

        // Permissions onboarding
        composable(FocusFortDestination.Onboarding.route) {
            val context = LocalContext.current
            OnboardingFlow(
                onFinished = {
                    // After permissions, go to PIN setup
                    navController.navigate(FocusFortDestination.PinSetup.route) {
                        popUpTo(FocusFortDestination.Welcome.route) { inclusive = true }
                    }
                },
            )
        }

        // First-launch PIN setup
        composable(FocusFortDestination.PinSetup.route) {
            val context = LocalContext.current
            val pinRepo = remember { PinRepository(context) }
            PinSetupScreen(
                onPinSaved = { pin ->
                    pinRepo.savePin(pin)
                    val prefs = context.getSharedPreferences("focusfort_prefs", android.content.Context.MODE_PRIVATE)
                    prefs.edit().putBoolean("is_first_launch", false).apply()
                    navController.navigate(FocusFortDestination.ModeSelect.route) {
                        popUpTo(FocusFortDestination.PinSetup.route) { inclusive = true }
                    }
                }
            )
        }

        // Mode selection dashboard
        composable(FocusFortDestination.ModeSelect.route) {
            ModeSelectScreen(
                onModeChosen = { mode ->
                    if (mode == "Student") {
                        navController.navigate("student_splash")
                    } else {
                        navController.navigate("generic_splash/${Uri.encode(mode)}")
                    }
                },
                onDashboard = {
                    navController.navigate(FocusFortDestination.Stats.route)
                },
            )
        }

        // Student mode splash â€” plays while setup screen loads in background
        composable(
            route = "student_splash",
            exitTransition = { fadeOut(animationSpec = tween(400)) }
        ) {
            StudentModeSplash(
                onFinished = {
                    navController.navigate("student_setup") {
                        popUpTo("student_splash") { inclusive = true }
                    }
                }
            )
        }

        // Student setup â€” fades in so content is visible before it fully renders
        composable(
            route = "student_setup",
            enterTransition = { fadeIn(animationSpec = tween(500)) },
            popEnterTransition = { fadeIn(animationSpec = tween(500)) }
        ) {
            StudentSetupScreen(
                onBack = { navController.navigateUp() },
                onSessionStarted = {
                    navController.navigate("student_workspace") {
                        popUpTo("student_setup") { inclusive = true }
                    }
                }
            )
        }

        // Student workspace
        composable("student_workspace") {
            StudentWorkspaceScreen(
                onQuizPassed = {
                    navController.navigate(FocusFortDestination.ModeSelect.route) {
                        popUpTo(0)
                    }
                }
            )
        }

        // Generic mode splash
        composable(
            route = "generic_splash/{mode}",
            arguments = listOf(navArgument("mode") { type = NavType.StringType }),
            exitTransition = { fadeOut(animationSpec = tween(400)) }
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode").orEmpty()
            GenericModeSplash(
                mode = mode,
                onFinished = {
                    navController.navigate("${FocusFortDestination.AppPicker.route}/${Uri.encode(mode)}") {
                        popUpTo("generic_splash/{mode}") { inclusive = true }
                    }
                }
            )
        }

        // App picker (for all non-student modes)
        composable(
            route = "${FocusFortDestination.AppPicker.route}/{mode}",
            arguments = listOf(navArgument("mode") { type = NavType.StringType }),
            enterTransition = { fadeIn(animationSpec = tween(500)) },
            popEnterTransition = { fadeIn(animationSpec = tween(500)) }
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode").orEmpty()
            AppPickerScreen(
                selectedMode = mode,
                onBack = { navController.navigateUp() },
                onFinished = {
                    if (mode == "Digital Detox") {
                        navController.navigate("detox_advisory")
                    } else {
                        navController.navigate(FocusFortDestination.Session.route)
                    }
                },
            )
        }

        // Detox advisory
        composable("detox_advisory") {
            DetoxAdvisoryScreen(
                onNext = { navController.navigate(FocusFortDestination.Session.route) }
            )
        }

        // Active session
        composable(FocusFortDestination.Session.route) {
            SessionScreen()
        }

        // Dashboard / Stats
        composable(FocusFortDestination.Stats.route) {
            DashboardScreen(
                onBack = { navController.navigateUp() }
            )
        }
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// Welcome Screen
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

private data class FeatureHighlight(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val accentColor: Color,
)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun WelcomeScreen(onGetStarted: () -> Unit) {
    val features = listOf(
        FeatureHighlight(
            icon = Icons.Filled.Lock,
            title = "Four Powerful Modes",
            description = "Student, Normal, Office, and Digital Detox â€” each designed for a different level of focus.",
            accentColor = Color(0xFF0EA5E9)
        ),
        FeatureHighlight(
            icon = Icons.Filled.Face,
            title = "AI-Powered Verification",
            description = "Student mode uses AI to verify your study material and generate a personalized quiz before you get a break.",
            accentColor = Color(0xFF22C55E)
        ),
        FeatureHighlight(
            icon = Icons.Filled.Security,
            title = "Instant App Blocking",
            description = "FocusFort watches your screen in real-time and instantly covers any distracting app you open.",
            accentColor = Color(0xFF8B5CF6)
        ),
        FeatureHighlight(
            icon = Icons.Filled.CheckCircle,
            title = "You're in Control",
            description = "Choose which apps to block, set study durations, and earn your break time â€” no tricks, just focus.",
            accentColor = Color(0xFFF59E0B)
        ),
    )

    val pagerState = rememberPagerState(pageCount = { features.size })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)) // Ultra-light grey
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(64.dp))

            // Logo
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "FocusFort Logo",
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .padding(16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "FocusFort",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A)
            )
            Text(
                text = "Your digital discipline companion",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Feature pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 32.dp),
                pageSpacing = 16.dp
            ) { page ->
                FeatureCard(feature = features[page])
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Pager dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                repeat(features.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    val width by animateFloatAsState(
                        targetValue = if (isSelected) 24f else 8f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                    )
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(width.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color(0xFF0EA5E9) else Color(0xFFCBD5E1)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Get Started button
            Button(
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9))
            ) {
                Text(
                    text = "Get Started",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun FeatureCard(feature: FeatureHighlight) {
    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = feature.accentColor.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(feature.accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = feature.title,
                    tint = feature.accentColor,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = feature.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = feature.description,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
            )
        }
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// Permissions Onboarding Flow
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

private enum class OnboardingStep(val index: Int) {
    UsageAccess(0),
    Overlay(1),
    Notifications(2),
    Done(3),
}

@Composable
private fun OnboardingFlow(
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    var step by remember { mutableStateOf(OnboardingStep.UsageAccess) }
    var usageAccessGranted by remember { mutableStateOf(false) }
    var overlayGranted by remember { mutableStateOf(false) }
    var notificationsGranted by remember { mutableStateOf(Build.VERSION.SDK_INT < 33) }

    val totalSteps = OnboardingStep.entries.size - 1 // exclude "Done" from progress

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationsGranted = granted || Build.VERSION.SDK_INT < 33
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                usageAccessGranted = hasUsageAccess(context)
                overlayGranted = hasOverlayPermission(context)
                notificationsGranted = Build.VERSION.SDK_INT < 33 ||
                    NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val openUsageAccessSettings = {
        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    val openOverlaySettings = {
        context.startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:${context.packageName}".toUri(),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)) // Ultra-light grey
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Header
            Text(
                text = "Set Up Permissions",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Text(
                text = "FocusFort needs a few permissions to work its magic.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Progress indicator
            val progressFraction = when (step) {
                OnboardingStep.UsageAccess -> 0.33f
                OnboardingStep.Overlay -> 0.66f
                OnboardingStep.Notifications -> 1f
                OnboardingStep.Done -> 1f
            }
            val animatedProgress by animateFloatAsState(
                targetValue = progressFraction,
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
            )
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(6.dp)
                    .clip(CircleShape),
                color = Color(0xFF0EA5E9),
                trackColor = Color(0xFFE2E8F0),
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Animated content swap between steps
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    val forward = targetState.index > initialState.index
                    val enter = slideInHorizontally(
                        initialOffsetX = { if (forward) it else -it },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                    ) + fadeIn(tween(200))
                    val exit = slideOutHorizontally(
                        targetOffsetX = { if (forward) -it / 2 else it / 2 },
                        animationSpec = tween(200)
                    ) + fadeOut(tween(150))
                    enter togetherWith exit
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 24.dp)
            ) { currentStep ->
                when (currentStep) {
                    OnboardingStep.UsageAccess -> PermissionCard(
                        icon = Icons.Filled.Visibility,
                        accentColor = Color(0xFF0EA5E9),
                        title = "Usage Access",
                        body = "Allows FocusFort to detect which app you open, so it can block distractions in real-time.",
                        isGranted = usageAccessGranted,
                        grantedNote = "Detection engine is armed and ready.",
                        deniedNote = "Without this, blocked apps cannot be detected.",
                        primaryLabel = if (usageAccessGranted) "Continue ➡️" else "Open Settings",
                        onPrimary = {
                            if (usageAccessGranted) step = OnboardingStep.Overlay
                            else openUsageAccessSettings()
                        },
                        skipLabel = if (usageAccessGranted) null else "Skip for now",
                        onSkip = { step = OnboardingStep.Overlay }
                    )

                    OnboardingStep.Overlay -> PermissionCard(
                        icon = Icons.Filled.Security,
                        accentColor = Color(0xFF8B5CF6),
                        title = "Display Over Apps",
                        body = "Required to show the block screen on top of any distracting app you try to open.",
                        isGranted = overlayGranted,
                        grantedNote = "Block screen is ready to deploy.",
                        deniedNote = "Without this, the block screen cannot appear.",
                        primaryLabel = if (overlayGranted) "Continue ➡️" else "Open Settings",
                        onPrimary = {
                            if (overlayGranted) step = OnboardingStep.Notifications
                            else openOverlaySettings()
                        },
                        skipLabel = if (overlayGranted) null else "Skip for now",
                        onSkip = { step = OnboardingStep.Notifications }
                    )

                    OnboardingStep.Notifications -> PermissionCard(
                        icon = Icons.Filled.Notifications,
                        accentColor = Color(0xFFF59E0B),
                        title = "Notifications",
                        body = "Shows a persistent status notification while your focus session is active.",
                        isGranted = notificationsGranted,
                        grantedNote = "Session status notifications enabled.",
                        deniedNote = "Session status may not appear in the notification bar.",
                        primaryLabel = if (notificationsGranted || Build.VERSION.SDK_INT < 33) "All Done â†’" else "Allow",
                        onPrimary = {
                            if (notificationsGranted || Build.VERSION.SDK_INT < 33) {
                                step = OnboardingStep.Done
                            } else {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        skipLabel = if (notificationsGranted) null else "Skip",
                        onSkip = { step = OnboardingStep.Done }
                    )

                    OnboardingStep.Done -> {
                        // All done â€” auto-navigate
                        LaunchedEffect(Unit) { onFinished() }
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF22C55E),
                                    modifier = Modifier.size(72.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("You're ready!", style = MaterialTheme.typography.headlineSmall, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp).navigationBarsPadding())
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    accentColor: Color,
    title: String,
    body: String,
    isGranted: Boolean,
    grantedNote: String,
    deniedNote: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    skipLabel: String? = null,
    onSkip: (() -> Unit)? = null,
) {
    val borderColor by animateColorAsState(
        targetValue = if (isGranted) Color(0xFF22C55E).copy(alpha = 0.5f) else Color(0xFFE2E8F0),
        animationSpec = tween(400)
    )
    val buttonContainerColor by animateColorAsState(
        targetValue = if (isGranted) Color(0xFF22C55E) else accentColor,
        animationSpec = tween(400)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(32.dp))
            }

            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            // Body
            Text(text = body, style = MaterialTheme.typography.bodyLarge, color = Color(0xFF64748B))

            // Status note
            Row(verticalAlignment = Alignment.CenterVertically) {
                val noteColor = if (isGranted) Color(0xFF22C55E) else Color(0xFF64748B)
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(noteColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isGranted) grantedNote else deniedNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = noteColor,
                    fontWeight = if (isGranted) FontWeight.SemiBold else FontWeight.Normal
                )
            }

            // Primary action button
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.96f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            )
            Button(
                onClick = onPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .scale(scale),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = buttonContainerColor),
                interactionSource = interactionSource
            ) {
                AnimatedContent(targetState = isGranted) { granted ->
                    if (granted) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = primaryLabel, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    } else {
                        Text(text = primaryLabel, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            // Skip link
            if (skipLabel != null && onSkip != null) {
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = skipLabel, color = Color(0xFF94A3B8))
                }
            }
        }
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// Mode Selection Dashboard
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

private data class ModeOption(
    val key: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val gradientStart: Color,
    val gradientEnd: Color,
    val icon: ImageVector,
    val emoji: String,
)

@Composable
private fun ModeSelectScreen(
    onModeChosen: (String) -> Unit,
    onDashboard: () -> Unit = {},
) {
    val scope      = rememberCoroutineScope()
    val context    = LocalContext.current
    val viewModel: ProfileViewModel = hiltViewModel()
    val sessionViewModel: com.example.focusfortvs.ui.viewmodel.SessionViewModel = hiltViewModel()
    val sessionState by sessionViewModel.uiState.collectAsState()
    val pinRepo    = remember { PinRepository(context) }

    // PIN dialog states
    var pendingMode         by remember { mutableStateOf<String?>(null) }  // mode waiting for PIN
    var showChangePinDialog by remember { mutableStateOf(false) }

    // Guard: if session active + user taps a mode, require PIN first
    if (pendingMode != null) {
        PinEntryDialog(
            title     = "Switch Mode?",
            onSuccess = {
                val mode = pendingMode!!
                pendingMode = null
                scope.launch {
                    viewModel.setCurrentMode(mode)
                    onModeChosen(mode)
                }
            },
            onDismiss = { pendingMode = null },
            verifyPin = { pin -> pinRepo.verifyPin(pin) }
        )
    }

    // Change PIN dialog
    if (showChangePinDialog) {
        PinChangeDialog(
            verifyOldPin  = { pin -> pinRepo.verifyPin(pin) },
            onNewPinSaved = { newPin ->
                pinRepo.savePin(newPin)
                showChangePinDialog = false
            },
            onDismiss = { showChangePinDialog = false }
        )
    }

    val selectMode: (String) -> Unit = { mode ->
        if (sessionState.isSessionActive) {
            // Session is running â€” require PIN to switch
            pendingMode = mode
        } else {
            scope.launch {
                viewModel.setCurrentMode(mode)
                onModeChosen(mode)
            }
        }
    }

    val modes = listOf(
        ModeOption(
            key = "Student",
            title = "Student Mode",
            subtitle = "AI-powered study sessions",
            description = "Earn your breaks by passing AI-generated quizzes",
            gradientStart = Color(0xFF00BCD4),
            gradientEnd   = Color(0xFF006064),
            icon = Icons.Filled.Face,
            emoji = "🎓"
        ),
        ModeOption(
            key = "Normal",
            title = "Normal Mode",
            subtitle = "Mindful browsing",
            description = "Pause and reflect before opening distracting apps",
            gradientStart = Color(0xFF43A047),
            gradientEnd   = Color(0xFF1B5E20),
            icon = Icons.Filled.CheckCircle,
            emoji = "🧘"
        ),
        ModeOption(
            key = "Office",
            title = "Office Mode",
            subtitle = "Professional productivity",
            description = "AI-judged emergency passes keep you accountable",
            gradientStart = Color(0xFF1E88E5),
            gradientEnd   = Color(0xFF0D47A1),
            icon = Icons.Filled.Build,
            emoji = "💼"
        ),
        ModeOption(
            key = "Digital Detox",
            title = "Digital Detox",
            subtitle = "Total disconnection",
            description = "Zero distractions. Maximum clarity. Full lock.",
            gradientStart = Color(0xFFE53935),
            gradientEnd   = Color(0xFF880E4F),
            icon = Icons.Filled.Lock,
            emoji = "🔒"
        ),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // â”€â”€ Hero Header â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF0B2545), Color(0xFF1565C0))
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 28.dp)
            ) {
                // Right side: Dashboard + Change PIN icons
                Row(
                    modifier = Modifier.align(Alignment.TopEnd),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Change PIN
                    IconButton(
                        onClick = { showChangePinDialog = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Change PIN",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    // Dashboard
                    IconButton(
                        onClick = onDashboard,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Visibility,
                            contentDescription = "Dashboard",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(modifier = Modifier.padding(top = 44.dp)) {
                    Text(
                        text = "Choose Your Mode",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 0.3.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "How do you want to protect your focus today?",
                        fontSize = 14.sp,
                        color = Color(0xFFB0BEC5),
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            // â”€â”€ Mode Cards â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .navigationBarsPadding(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 20.dp, vertical = 20.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                modes.forEachIndexed { index, option ->
                    item(key = option.key) {
                        val visible = remember { Animatable(0f) }
                        val slideY  = remember { Animatable(60f) }
                        LaunchedEffect(Unit) {
                            kotlinx.coroutines.delay(index * 80L)
                            visible.animateTo(1f, tween(350))
                            slideY.animateTo(0f, spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow))
                        }

                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()
                        val scale by animateFloatAsState(
                            targetValue = if (isPressed) 0.96f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(scale)
                                .alpha(visible.value)
                                .clickable(interactionSource = interactionSource, indication = null) {
                                    selectMode(option.key)
                                }
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(option.gradientStart, option.gradientEnd)
                                    )
                                )
                                .padding(horizontal = 20.dp, vertical = 20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Icon bubble
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.White.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = option.emoji,
                                        fontSize = 28.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Default
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = option.title,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = option.subtitle,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = option.description,
                                        fontSize = 13.sp,
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.Normal
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Chevron
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ChevronRight,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// App Picker Screen
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppPickerScreen(
    selectedMode: String,
    onBack: () -> Unit,
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profileViewModel: ProfileViewModel = hiltViewModel()

    val packageManager = context.packageManager
    val currentModeLabel = selectedMode.ifBlank { "Normal" }
    val isDetox = currentModeLabel == "Digital Detox"

    // Loaded on IO thread â€“ prevents main-thread block and eliminates stutter
    var installedApps by remember(currentModeLabel) { mutableStateOf<List<AppEntry>>(emptyList()) }

    LaunchedEffect(currentModeLabel) {
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

    var selectedPackages by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(currentModeLabel) {
        selectedPackages = profileViewModel.getBlockedAppsForMode(currentModeLabel).map { it.packageName }.toSet()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isDetox) "Essential Apps" else "Block These Apps",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = if (isDetox) "Only these will be allowed" else "$currentModeLabel Mode",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Instruction banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = if (isDetox)
                        "Select the apps you NEED. Everything else will be blocked."
                    else
                        "Select the apps you want blocked during this session.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF64748B)
                )
            }

            if (installedApps.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("No apps found.", color = Color(0xFF64748B))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp, top = 12.dp, end = 16.dp, bottom = 120.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(installedApps, key = { it.packageName }) { app ->
                        val checked = selectedPackages.contains(app.packageName)
                        AppRowItem(
                            app = app,
                            checked = checked,
                            accentColor = if (isDetox) Color(0xFF0EA5E9) else Color(0xFFEF4444),
                            onToggle = {
                                val isChecked = !checked
                                val next = if (isChecked) selectedPackages + app.packageName
                                           else selectedPackages - app.packageName
                                selectedPackages = next
                                scope.launch {
                                    if (isChecked) profileViewModel.saveBlockedAppSelection(currentModeLabel, app.packageName, app.appLabel)
                                    else profileViewModel.removeBlockedAppSelection(currentModeLabel, app.packageName)
                                }
                            }
                        )
                    }
                }
            }
        }

        // Floating Continue button
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp, start = 24.dp, end = 24.dp)
                .navigationBarsPadding(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Button(
                onClick = onFinished,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDetox) Color(0xFF0EA5E9) else Color(0xFF0F172A)
                )
            ) {
                Text(
                    text = if (selectedPackages.isEmpty()) "Continue Without Selection" else "Start Session (${selectedPackages.size} selected)",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun AppRowItem(
    app: AppEntry,
    checked: Boolean,
    accentColor: Color,
    onToggle: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val bgColor by animateColorAsState(
        targetValue = if (checked) accentColor.copy(alpha = 0.08f) else Color.White,
        animationSpec = tween(200)
    )
    val borderColor by animateColorAsState(
        targetValue = if (checked) accentColor.copy(alpha = 0.5f) else Color(0xFFE8ECF0),
        animationSpec = tween(200)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onToggle() }
    ) {
        // Colored left accent bar when selected
        if (checked) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .matchParentSize()
                    .clip(RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
                    .background(accentColor)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // App icon
            Image(
                painter = BitmapPainter(app.iconBitmap.asImageBitmap()),
                contentDescription = null,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
            Spacer(modifier = Modifier.width(14.dp))
            // App name â€” FIXED: dark text visible on white background
            Text(
                text = app.appLabel,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal,
                color = if (checked) accentColor else Color(0xFF1E293B),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = checked,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = accentColor,
                    uncheckedThumbColor = Color(0xFFB0BEC5),
                    uncheckedTrackColor = Color(0xFFE8ECF0),
                    uncheckedBorderColor = Color(0xFFCDD5DD)
                )
            )
        }
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// Data helpers
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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
    return AppEntry(
        packageName = packageName,
        appLabel = label,
        iconBitmap = icon,
    )
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


