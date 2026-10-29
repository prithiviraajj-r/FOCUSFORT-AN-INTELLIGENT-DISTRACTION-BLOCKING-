package com.example.focusfortvs.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.focusfortvs.R
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────────────────
// Generic Mode Splash — dedicated full-screen route for non-Student modes.
//
// Shows a white animated loading screen for a minimum of 2 s, then calls
// onFinished(). NavHost transitions to AppPicker with a 500 ms fadeIn.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GenericModeSplash(mode: String, onFinished: () -> Unit) {

    // ── Lottie ────────────────────────────────────────────────────────────────
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.focus_splash)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations  = LottieConstants.IterateForever,
        isPlaying   = true,
        speed       = 1f,
    )

    // ── Drive the lifecycle ───────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        delay(2_000L)      // minimum visible time
        onFinished()       // NavHost then fades-in app_picker (500 ms)
    }

    val tagline = when (mode) {
        "Digital Detox" -> "Unplug.\nRecharge your mind."
        "Office" -> "Deep work.\nZero distractions."
        else -> "Stay focused.\nAchieve your goals."
    }

    // ── Full white screen ─────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp)
        ) {
            // ── Lottie animation ─────────────────────────────────────────────
            LottieAnimation(
                composition = composition,
                progress    = { progress },
                modifier    = Modifier.size(200.dp)
            )

            Spacer(Modifier.height(28.dp))

            // ── Title ────────────────────────────────────────────────────────
            Text(
                text          = "$mode Mode",
                fontSize      = 30.sp,
                fontWeight    = FontWeight.ExtraBold,
                color         = Color(0xFF0B2545),
                letterSpacing = 0.5.sp
            )

            Spacer(Modifier.height(10.dp))

            // ── Tagline ──────────────────────────────────────────────────────
            Text(
                text       = tagline,
                fontSize   = 15.sp,
                color      = Color(0xFF546E7A),
                textAlign  = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(44.dp))

            // ── Pulsing loading dots ─────────────────────────────────────────
            SplashLoadingDots()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SplashLoadingDots() {
    val animatables = List(3) { remember { Animatable(0.25f) } }

    animatables.forEachIndexed { i, anim ->
        LaunchedEffect(i) {
            delay(i * 180L)
            while (true) {
                anim.animateTo(1f,    tween(380))
                anim.animateTo(0.25f, tween(380))
            }
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        animatables.forEach { a ->
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .alpha(a.value)
                    .background(Color(0xFF1565C0), shape = CircleShape)
            )
        }
    }
}
