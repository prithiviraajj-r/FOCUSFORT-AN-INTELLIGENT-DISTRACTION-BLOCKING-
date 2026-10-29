package com.example.focusfortvs.ui.pin

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

// ─── Palette ─────────────────────────────────────────────────────────────────
private val Navy    = Color(0xFF0B2545)
private val NavyMid = Color(0xFF1565C0)
private val Teal    = Color(0xFF00BCD4)
private val ErrorRed = Color(0xFFEF4444)

// ─────────────────────────────────────────────────────────────────────────────
// PinEntryDialog
// Shown when the user attempts a guarded action (stop session / switch mode).
// ─────────────────────────────────────────────────────────────────────────────

/**
 * @param title     Dialog heading, e.g. "Enter PIN to stop session"
 * @param onSuccess Called when the entered PIN matches. Dismiss is handled internally.
 * @param onDismiss Called when the user cancels.
 * @param verifyPin Function that returns true if the entered PIN is correct.
 */
@Composable
fun PinEntryDialog(
    title: String,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit,
    verifyPin: (String) -> Boolean,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            tonalElevation = 8.dp,
            shadowElevation = 24.dp
        ) {
            PinKeypad(
                title      = title,
                subtitle   = "Enter your 4-digit guardian PIN",
                onSuccess  = { _ -> onSuccess() },
                onCancel   = onDismiss,
                verifyPin  = verifyPin,
                isSetup    = false,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PinSetupScreen
// Full-screen flow: set PIN → confirm PIN → done.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * @param onPinSaved Called with the confirmed 4-digit PIN string once setup completes.
 */
@Composable
fun PinSetupScreen(onPinSaved: (String) -> Unit) {
    var step        by remember { mutableStateOf(0) } // 0 = enter, 1 = confirm
    var firstPin    by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Navy, NavyMid))),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            tonalElevation = 8.dp
        ) {
            if (step == 0) {
                PinKeypad(
                    title     = "Set Guardian PIN",
                    subtitle  = "Choose a 4-digit PIN to protect your focus sessions",
                    isSetup   = true,
                    onSuccess = { pin ->
                        firstPin = pin
                        step = 1
                    },
                    onCancel  = null, // can't skip setup
                    verifyPin = { true } // no verification in setup step 1
                )
            } else {
                PinKeypad(
                    title     = "Confirm PIN",
                    subtitle  = "Re-enter your PIN to confirm",
                    isSetup   = true,
                    onSuccess = { pin ->
                        if (pin == firstPin) {
                            onPinSaved(pin)
                        }
                    },
                    onCancel  = { step = 0 }, // go back to re-enter
                    verifyPin = { pin -> pin == firstPin },
                    wrongPinMessage = "PINs don't match — try again"
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PinChangeDialog
// Shown from ModeSelect to let user change PIN after verifying old one.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 3-step dialog: verify old PIN → enter new PIN → confirm new PIN.
 * @param verifyOldPin    Function that checks if the entered old PIN is correct.
 * @param onNewPinSaved   Called with the new confirmed PIN string.
 * @param onDismiss       Called when user cancels at any step.
 */
@Composable
fun PinChangeDialog(
    verifyOldPin: (String) -> Boolean,
    onNewPinSaved: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var step     by remember { mutableStateOf(0) } // 0=verify old, 1=new pin, 2=confirm new
    var newPin   by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f).wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            tonalElevation = 8.dp,
            shadowElevation = 24.dp
        ) {
            when (step) {
                0 -> PinKeypad(
                    title      = "Verify Current PIN",
                    subtitle   = "Enter your existing guardian PIN",
                    isSetup    = false,
                    verifyPin  = verifyOldPin,
                    onSuccess  = { _ -> step = 1 },
                    onCancel   = onDismiss,
                )
                1 -> PinKeypad(
                    title      = "New PIN",
                    subtitle   = "Choose your new 4-digit PIN",
                    isSetup    = true,
                    verifyPin  = { true },
                    onSuccess  = { pin -> newPin = pin; step = 2 },
                    onCancel   = onDismiss,
                )
                2 -> PinKeypad(
                    title      = "Confirm New PIN",
                    subtitle   = "Re-enter your new PIN",
                    isSetup    = true,
                    verifyPin  = { pin -> pin == newPin },
                    onSuccess  = { _ -> onNewPinSaved(newPin) },
                    onCancel   = { step = 1 },
                    wrongPinMessage = "PINs don't match — try again"
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PinKeypad — shared internal composable
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PinKeypad(
    title: String,
    subtitle: String,
    isSetup: Boolean,
    verifyPin: (String) -> Boolean,
    onSuccess: (String) -> Unit,
    onCancel: (() -> Unit)?,
    wrongPinMessage: String = "Incorrect PIN — try again",
) {
    var input      by remember { mutableStateOf("") }
    var shakeError by remember { mutableStateOf(false) }
    var errorMsg   by remember { mutableStateOf<String?>(null) }
    val scope      = rememberCoroutineScope()
    val haptic     = LocalHapticFeedback.current

    // Shake animation offset
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(shakeError) {
        if (shakeError) {
            shakeOffset.animateTo(0f, keyframes {
                durationMillis = 400
                -16f at 50
                 16f at 100
                -12f at 150
                 12f at 200
                 -8f at 250
                  8f at 300
                  0f at 400
            })
            shakeError = false
        }
    }

    Column(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("🛡️", fontSize = 36.sp)
            Text(
                title, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp,
                color = Navy, textAlign = TextAlign.Center
            )
            Text(
                subtitle, fontSize = 13.sp,
                color = Color(0xFF64748B), textAlign = TextAlign.Center
            )
        }

        // ── PIN dot indicators ────────────────────────────────────────────────
        Row(
            modifier = Modifier.offset(x = shakeOffset.value.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(4) { i ->
                val filled = i < input.length
                val dotColor by animateColorAsState(
                    targetValue = when {
                        errorMsg != null -> ErrorRed
                        filled           -> Navy
                        else             -> Color(0xFFE2E8F0)
                    }
                )
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                        .border(
                            width = 2.dp,
                            color = if (filled) Color.Transparent else Color(0xFFCBD5E1),
                            shape = CircleShape
                        )
                )
            }
        }

        // ── Error message ─────────────────────────────────────────────────────
        if (errorMsg != null) {
            Text(
                errorMsg!!, color = ErrorRed,
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }

        // ── Numeric keypad ────────────────────────────────────────────────────
        val keys = listOf("1","2","3","4","5","6","7","8","9","","0","⌫")
        val cols = 3

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            keys.chunked(cols).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { key ->
                        Box(modifier = Modifier.weight(1f)) {
                            when (key) {
                                "" -> { /* empty cell — placeholder for layout */ }
                                "⌫" -> KeyButton(
                                    label = key,
                                    isIcon = true,
                                    onClick = {
                                        if (input.isNotEmpty()) {
                                            input = input.dropLast(1)
                                            errorMsg = null
                                        }
                                    }
                                )
                                else -> KeyButton(
                                    label = key,
                                    onClick = {
                                        if (input.length < 4) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            input += key
                                            errorMsg = null

                                            // Auto-submit when 4 digits entered
                                            if (input.length == 4) {
                                                if (isSetup || verifyPin(input)) {
                                                    onSuccess(input)
                                                    input = ""
                                                } else {
                                                    // Wrong PIN
                                                    errorMsg = wrongPinMessage
                                                    scope.launch { shakeError = true }
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    input = ""
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Cancel button ─────────────────────────────────────────────────────
        if (onCancel != null) {
            TextButton(onClick = onCancel) {
                Text(
                    if (isSetup) "← Back" else "Cancel",
                    color = Color(0xFF64748B),
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun KeyButton(label: String, isIcon: Boolean = false, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF1F5F9))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isIcon) {
            Icon(
                Icons.AutoMirrored.Filled.Backspace,
                contentDescription = "Delete",
                tint = Color(0xFF64748B),
                modifier = Modifier.size(22.dp)
            )
        } else {
            Text(
                label, fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Navy
            )
        }
    }
}
