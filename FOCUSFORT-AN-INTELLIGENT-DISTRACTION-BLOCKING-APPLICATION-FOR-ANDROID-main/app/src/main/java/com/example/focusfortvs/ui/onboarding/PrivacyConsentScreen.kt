package com.example.focusfortvs.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.TextStyle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PrivacyConsentScreen(onAccepted: () -> Unit) {
    val uriHandler = LocalUriHandler.current

    val listState = rememberLazyListState()
    var isChecked by remember { mutableStateOf(false) }

    val hasScrolledToBottom by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            totalItems == 0 || lastVisible >= totalItems - 2
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFF0B2545), Color(0xFF1565C0)))
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 28.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Privacy Policy & Terms",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Please read and accept before continuing",
                        fontSize = 13.sp,
                        color = Color(0xFFB0BEC5),
                        textAlign = TextAlign.Center
                    )
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    PolicySection(
                        title = "1. Introduction",
                        body = "Welcome to Focus Fort, developed by Madhan. We are committed to protecting your privacy. This Privacy Policy explains how the App collects, uses, and safeguards your information.\n\nBy using Focus Fort, you agree to the terms of this policy."
                    )
                }
                item {
                    PolicySection(
                        title = "2. Data We Collect",
                        body = "Focus Fort collects the following data to provide its core features:\n\n- App Usage Statistics: We use Android's UsageStatsManager to detect which app is in the foreground in real-time. This data is never recorded.\n\n- Focus Session Data: Start times, end times, and session durations are stored locally on your device only.\n\n- Study Topics & Blocked Apps: Stored locally on your device.\n\n- Security PIN: A 4-digit PIN stored as a secure SHA-256 hash, saved locally only."
                    )
                }
                item {
                    PolicySection(
                        title = "3. How We Use Your Data",
                        body = "All data collected by Focus Fort is used solely to provide the app's features:\n\n- App usage data is processed in real-time to enforce blocking rules. It is never logged or uploaded.\n\n- Session history is stored locally to power your Dashboard.\n\n- No personal data is ever uploaded to any server or shared with third parties.\n\n- AI features send only your study topic text to a secure Azure OpenAI endpoint to generate quiz questions. No personal information is sent."
                    )
                }
                item {
                    PolicySection(
                        title = "4. Permissions We Request",
                        body = "Focus Fort requires the following permissions:\n\n- Usage Access: Used exclusively to detect the foreground app and enforce blocking rules. Data is processed in real-time and never stored.\n\n- Display Over Other Apps: Used to show the block screen on top of distracting apps.\n\n- Post Notifications: Used only for focus session reminders.\n\n- Foreground Service: Used to keep the blocking service active during a session."
                    )
                }
                item {
                    PolicySection(
                        title = "5. Data Storage & Security",
                        body = "All your data (sessions, blocked apps, settings, PIN hash) is stored locally on your device. This data is never transmitted to our servers.\n\nUninstalling the app will permanently delete all locally stored data."
                    )
                }
                item {
                    PolicySection(
                        title = "6. Third-Party Services",
                        body = "Focus Fort uses Azure OpenAI (by Microsoft) only for the AI study verification feature. When you use this feature, your study topic text is sent to Azure OpenAI to generate quiz questions. Please refer to Microsoft's Privacy Policy for details."
                    )
                }
                item {
                    PolicySection(
                        title = "7. Children's Privacy",
                        body = "Focus Fort is not directed toward children under the age of 13. We do not knowingly collect personal information from children under 13."
                    )
                }
                item {
                    PolicySection(
                        title = "8. Changes to This Policy",
                        body = "We may update this Privacy Policy from time to time. Any changes will be reflected in an updated version of this document within the app."
                    )
                }
                item {
                    PolicySection(
                        title = "9. Contact Us",
                        body = "If you have any questions about this Privacy Policy, please contact us at:\n\nfocusfort.support@gmail.com\n\nBy continuing, you acknowledge that you have read, understood, and agree to this Privacy Policy and Terms of Service."
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
            }

            AnimatedVisibility(visible = !hasScrolledToBottom) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(listOf(Color.Transparent, Color(0xFFEBF1F5)))
                        )
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Scroll down to read all terms",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 12.dp,
                color = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .navigationBarsPadding()
                ) {
                    if (hasScrolledToBottom) {
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(400)) + slideInVertically(
                                initialOffsetY = { it / 2 },
                                animationSpec = tween(400)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isChecked) Color(0xFFEFF6FF) else Color(0xFFF1F5F9))
                                    .border(
                                        width = 1.dp,
                                        color = if (isChecked) Color(0xFF3B82F6) else Color(0xFFCBD5E1),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { isChecked = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFF1565C0),
                                        uncheckedColor = Color(0xFF94A3B8)
                                    )
                                )
                                Spacer(Modifier.width(10.dp))
                                val annotatedString = buildAnnotatedString {
                                    append("I have read and agree to the ")
                                    pushStringAnnotation(tag = "URL", annotation = "https://madhan-kumar2005.github.io/private_policy/")
                                    withStyle(SpanStyle(color = Color(0xFF1565C0), fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline)) {
                                        append("Privacy Policy")
                                    }
                                    pop()
                                    append(" and ")
                                    pushStringAnnotation(tag = "URL", annotation = "https://madhan-kumar2005.github.io/private_policy/")
                                    withStyle(SpanStyle(color = Color(0xFF1565C0), fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline)) {
                                        append("Terms of Service")
                                    }
                                    pop()
                                }
                                ClickableText(
                                    text = annotatedString,
                                    style = TextStyle(fontSize = 13.sp, color = Color(0xFF334155), lineHeight = 18.sp),
                                    onClick = { offset ->
                                        annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                            .firstOrNull()?.let { annotation ->
                                                uriHandler.openUri(annotation.item)
                                            }
                                    }
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = false, onCheckedChange = null, enabled = false)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Scroll down to read the full policy first",
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = onAccepted,
                        enabled = isChecked,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(27.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1565C0),
                            disabledContainerColor = Color(0xFFCBD5E1)
                        )
                    ) {
                        if (isChecked) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            text = if (isChecked) "Accept & Continue" else "Please Read & Accept Above",
                            fontWeight = FontWeight.Bold,
                            color = if (isChecked) Color.White else Color(0xFF94A3B8),
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PolicySection(title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0B2545))
            Spacer(Modifier.height(8.dp))
            Text(text = body, fontSize = 13.sp, color = Color(0xFF475569), lineHeight = 20.sp)
        }
    }
}
