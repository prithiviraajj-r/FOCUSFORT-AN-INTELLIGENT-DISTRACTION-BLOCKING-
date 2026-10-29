package com.example.focusfortvs.ui.normal

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun NormalWorkspaceScreen(
    appName: String,
    packageName: String,
    onFinish: () -> Unit,
    viewModel: NormalWorkspaceViewModel = hiltViewModel()
) {
    val phase by viewModel.phase.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.initialize(packageName)
    }

    LaunchedEffect(phase) {
        if (phase is NormalWorkspacePhase.Complete) {
            onFinish()
        }
    }

    // Force clear clipboard to prevent pasted text even outside the text field boundaries
    val clipboardManager = LocalClipboardManager.current
    LaunchedEffect(Unit) {
        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(""))
    }

    when (val currentPhase = phase) {
        is NormalWorkspacePhase.Friction -> {
            FrictionScreen(
                appName = appName,
                phase = currentPhase,
                onInputChanged = viewModel::onInputChanged,
                onSubmit = viewModel::verifyInput
            )
        }
        is NormalWorkspacePhase.Complete -> {
            // Empty, finishing
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrictionScreen(
    appName: String,
    phase: NormalWorkspacePhase.Friction,
    onInputChanged: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Unlocking $appName",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Type the exact text below to prove you are paying attention.",
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Surface(
                color = Color(0xFF1E1E1E),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = phase.targetString,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 4.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Disable text selection completely
            val customTextSelectionColors = TextSelectionColors(
                handleColor = Color.Transparent,
                backgroundColor = Color.Transparent
            )

            CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
                OutlinedTextField(
                    value = phase.userInput,
                    onValueChange = onInputChanged,
                    textStyle = LocalTextStyle.current.copy(
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 20.sp,
                        letterSpacing = 2.sp
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF1E1E1E),
                        unfocusedContainerColor = Color(0xFF1E1E1E),
                        focusedBorderColor = if (phase.isError) Color.Red else Color(0xFF03A9F4),
                        unfocusedBorderColor = if (phase.isError) Color.Red else Color.Gray,
                        cursorColor = Color(0xFF03A9F4)
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Done,
                        autoCorrect = false
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (phase.isError) {
                Text(
                    text = "Incorrect match. Case and symbols matter.",
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03A9F4))
            ) {
                Text("Verify & Continue", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
