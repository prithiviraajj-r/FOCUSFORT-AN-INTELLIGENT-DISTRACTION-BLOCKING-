package com.example.focusfortvs.ui.student

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.focusfortvs.data.network.QuizResponse
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentWorkspaceScreen(
    onQuizPassed: () -> Unit,
    viewModel: StudentWorkspaceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) viewModel.processImage(bitmap)
    }

    // Bottom sheet state for OCR result feedback
    var showResultSheet by remember { mutableStateOf(false) }
    var sheetContent by remember { mutableStateOf<WorkspaceUiState?>(null) }

    // Trigger bottom sheet on TopicMismatch or Error
    LaunchedEffect(uiState) {
        if (uiState is WorkspaceUiState.TopicMismatch || uiState is WorkspaceUiState.Error) {
            sheetContent = uiState
            showResultSheet = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)) // Ultra-light grey
    ) {
        when (val state = uiState) {
            is WorkspaceUiState.Initial -> InitialCaptureView(
                onCapture = { takePictureLauncher.launch(null) }
            )

            is WorkspaceUiState.Loading -> LoadingView()

            is WorkspaceUiState.TopicMismatch, is WorkspaceUiState.Error -> {
                // Show the initial capture view in background while bottom sheet is visible
                InitialCaptureView(
                    onCapture = { takePictureLauncher.launch(null) },
                    dimmed = true
                )
            }

            is WorkspaceUiState.QuizReady -> {
                QuizUI(
                    quizResponse = state.quiz,
                    aiRelevancePoints = state.aiRelevancePoints,
                    onSubmit = { correct, total, relevance, timeSeconds ->
                        viewModel.submitQuiz(correct, total, relevance, timeSeconds)
                    }
                )
            }

            is WorkspaceUiState.Approved -> {
                ScoreBreakdownUI(score = state.score, passed = true, onDone = onQuizPassed)
            }

            is WorkspaceUiState.Rejected -> {
                ScoreBreakdownUI(score = state.score, passed = false, attemptsLeft = state.attemptsLeft) {
                    viewModel.resetState()
                }
            }

            is WorkspaceUiState.LockedOut -> LockedOutView()
        }
    }

    // ── Bottom Sheet for TopicMismatch / Error results ────────────────────────
    if (showResultSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showResultSheet = false
                viewModel.resetState()
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            when (val content = sheetContent) {
                is WorkspaceUiState.TopicMismatch -> TopicMismatchSheet(
                    attemptsLeft = content.attemptsLeft,
                    onRetry = {
                        showResultSheet = false
                        viewModel.resetState()
                        takePictureLauncher.launch(null)
                    },
                    onDismiss = {
                        showResultSheet = false
                        viewModel.resetState()
                    }
                )
                is WorkspaceUiState.Error -> ErrorSheet(
                    message = content.message,
                    attemptsLeft = content.attemptsLeft,
                    onRetry = {
                        showResultSheet = false
                        viewModel.resetState()
                        takePictureLauncher.launch(null)
                    },
                    onDismiss = {
                        showResultSheet = false
                        viewModel.resetState()
                    }
                )
                else -> {}
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Initial Capture View
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun InitialCaptureView(onCapture: () -> Unit, dimmed: Boolean = false) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(if (dimmed) Modifier.background(Color.Black.copy(alpha = 0.5f)) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0EA5E9).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = null,
                    tint = Color(0xFF0EA5E9),
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Capture Study Material",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Take a photo of your notes or textbook. Our AI will verify it matches your topic and generate a quiz.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onCapture,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(56.dp)
                    .scale(scale),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
                interactionSource = interactionSource
            ) {
                Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Take Photo", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Loading View
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LoadingView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color(0xFF0EA5E9), modifier = Modifier.size(56.dp))
            Spacer(modifier = Modifier.height(24.dp))
            Text("Analyzing your study material...", style = MaterialTheme.typography.bodyLarge, color = Color(0xFF64748B))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Locked Out View
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LockedOutView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(40.dp)) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(72.dp))
            Spacer(modifier = Modifier.height(24.dp))
            Text("Full Lockout Active", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(12.dp))
            Text("You have used all 3 attempts. All apps are locked until your session timer ends.", style = MaterialTheme.typography.bodyLarge, color = Color(0xFF64748B), textAlign = TextAlign.Center)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bottom Sheet: Topic Mismatch
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TopicMismatchSheet(
    attemptsLeft: Int,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Handle pill
        Box(modifier = Modifier.size(width = 40.dp, height = 4.dp).clip(CircleShape).background(Color(0xFFE2E8F0)))

        Spacer(modifier = Modifier.height(8.dp))

        Text("📚 Nice try — go study!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), textAlign = TextAlign.Center)
        Text("Your photo doesn't match your study topic. Please actually study and try again.", style = MaterialTheme.typography.bodyLarge, color = Color(0xFF64748B), textAlign = TextAlign.Center)

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.1f)),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f))
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("⚠️", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Text("$attemptsLeft attempt(s) remaining", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color(0xFFEF4444))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9))
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Retry with New Photo", fontWeight = FontWeight.Bold, color = Color.White)
        }

        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Dismiss", color = Color(0xFF64748B))
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bottom Sheet: Error
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ErrorSheet(
    message: String,
    attemptsLeft: Int,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(modifier = Modifier.size(width = 40.dp, height = 4.dp).clip(CircleShape).background(Color(0xFFE2E8F0)))
        Spacer(modifier = Modifier.height(8.dp))

        Text("Something went wrong", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
        Text(message, style = MaterialTheme.typography.bodyLarge, color = Color(0xFF64748B), textAlign = TextAlign.Center)

        if (attemptsLeft > 0) {
            Text("$attemptsLeft attempt(s) remaining", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFF59E0B), fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9))
        ) {
            Text("Try Again", fontWeight = FontWeight.Bold, color = Color.White)
        }

        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Dismiss", color = Color(0xFF64748B))
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Score Breakdown UI
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ScoreBreakdownUI(
    score: FocusComplianceScore,
    passed: Boolean,
    attemptsLeft: Int = 0,
    onDone: () -> Unit,
) {
    val headerColor = if (passed) Color(0xFF22C55E) else Color(0xFFEF4444)
    val headerText = if (passed) "🎉 Session Approved!" else "❌ Not Approved"
    val subText = if (passed) "Great study session! All apps are now unblocked."
                  else "Score below 70. Keep studying! ($attemptsLeft attempt(s) remaining)"

    var animationTriggered by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        animationTriggered = true
    }

    val animatedScore by animateIntAsState(
        targetValue = if (animationTriggered) score.total else 0,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing)
    )
    val scale by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0.5f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)),
        contentPadding = PaddingValues(
            start = 24.dp, end = 24.dp,
            top = 24.dp,
            bottom = 32.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Status banner card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (passed) Color(0xFF22C55E).copy(alpha = 0.12f)
                                     else Color(0xFFEF4444).copy(alpha = 0.10f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (passed) Color(0xFF22C55E).copy(alpha = 0.4f) else Color(0xFFEF4444).copy(alpha = 0.4f)
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = headerText,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = headerColor,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = subText,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }
        item {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(160.dp)
                    .scale(scale)
                    .background(
                        if (passed) Color(0xFF22C55E).copy(alpha = 0.1f) else Color(0xFFEF4444).copy(alpha = 0.1f),
                        RoundedCornerShape(80.dp)
                    )
            ) {
                CircularProgressIndicator(
                    progress = { animatedScore / 100f },
                    modifier = Modifier.fillMaxSize(),
                    color = if (passed) Color(0xFF22C55E) else Color(0xFFEF4444),
                    trackColor = Color(0xFFE2E8F0),
                    strokeWidth = 8.dp,
                    strokeCap = StrokeCap.Round
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$animatedScore", fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                    Text("/100", fontSize = 14.sp, color = Color(0xFF64748B))
                }
            }
        }
        item { ScoreRow("📝 Quiz Accuracy", score.accuracyScore, 40, animationTriggered) }
        item { ScoreRow("📌 Topic Relevance", score.relevanceScore, 20, animationTriggered) }
        item { ScoreRow("🧠 Behavior", score.behaviorScore, 20, animationTriggered) }
        item { ScoreRow("⚡ Speed", score.speedScore, 20, animationTriggered) }
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (passed) Color(0xFF22C55E) else Color(0xFF0EA5E9)
                )
            ) {
                Text(
                    if (passed) "Return Home" else "Study More & Retry",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun ScoreRow(label: String, score: Int, outOf: Int, triggered: Boolean) {
    val animatedValue by animateIntAsState(
        targetValue = if (triggered) score else 0,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
    )
    val animatedProgress by animateFloatAsState(
        targetValue = if (triggered) score.toFloat() / outOf else 0f,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
    )

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
            Text(
                "$animatedValue / $outOf",
                fontWeight = FontWeight.Bold,
                color = if (score >= outOf * 0.5) Color(0xFF22C55E) else Color(0xFFEF4444)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
            color = if (score >= outOf * 0.5) Color(0xFF22C55E) else Color(0xFFEF4444),
            trackColor = Color(0xFFE2E8F0),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Quiz UI
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun QuizUI(
    quizResponse: QuizResponse,
    aiRelevancePoints: Int,
    onSubmit: (correctAnswers: Int, totalQuestions: Int, aiRelevancePoints: Int, timeSeconds: Long) -> Unit
) {
    val selectedAnswers = remember { androidx.compose.runtime.mutableStateMapOf<Int, Int>() }

    var elapsedSeconds by remember { mutableLongStateOf(0L) }
    var stopwatchRunning by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while (stopwatchRunning) {
            delay(1000L)
            elapsedSeconds++
        }
    }

    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val timeDisplay = "%02d:%02d".format(minutes, seconds)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        // Stopwatch header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("⏱ Time", style = MaterialTheme.typography.labelMedium, color = Color(0xFF64748B))
                Text(
                    text = timeDisplay,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (elapsedSeconds > 30) Color(0xFFEF4444) else Color(0xFF0EA5E9)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Pop Quiz", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(quizResponse.questions) { qIndex, question ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "${qIndex + 1}. ${question.question}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        question.options.forEachIndexed { optIndex, optionText ->
                            val isSelected = selectedAnswers[qIndex] == optIndex
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) Color(0xFF0EA5E9).copy(alpha = 0.08f)
                                        else Color.Transparent
                                    )
                                    .selectable(
                                        selected = isSelected,
                                        onClick = { selectedAnswers[qIndex] = optIndex }
                                    )
                                    .padding(vertical = 8.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = Color(0xFF0EA5E9),
                                        unselectedColor = Color(0xFF94A3B8)
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = optionText,
                                    color = if (isSelected) Color(0xFF0F172A) else Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                stopwatchRunning = false
                var correctCount = 0
                quizResponse.questions.forEachIndexed { index, question ->
                    if (selectedAnswers[index] == question.correctAnswerIndex) correctCount++
                }
                onSubmit(correctCount, quizResponse.questions.size, aiRelevancePoints, elapsedSeconds)
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
            enabled = selectedAnswers.size == quizResponse.questions.size
        ) {
            Text(
                "Submit Answers",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp).navigationBarsPadding())
    }
}
