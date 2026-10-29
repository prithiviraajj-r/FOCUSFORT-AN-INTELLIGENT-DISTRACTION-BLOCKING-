package com.example.focusfortvs.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focusfortvs.R
import kotlinx.coroutines.delay

@Composable
fun CinematicSplashScreen(onSplashFinished: () -> Unit) {
    // Animation values
    val logoScale   = remember { Animatable(0.3f) }
    val logoAlpha   = remember { Animatable(0f) }
    val textAlpha   = remember { Animatable(0f) }
    val taglineAlpha = remember { Animatable(0f) }
    val bgAlpha     = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // 1. Fade in background
        bgAlpha.animateTo(1f, animationSpec = tween(300))

        // 2. Logo springs in with impact
        logoAlpha.animateTo(1f, animationSpec = tween(200))
        logoScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.55f,
                stiffness = Spring.StiffnessMediumLow
            )
        )

        // 3. Title fades up
        delay(120)
        textAlpha.animateTo(1f, animationSpec = tween(400, easing = FastOutSlowInEasing))

        // 4. Tagline fades in
        delay(150)
        taglineAlpha.animateTo(1f, animationSpec = tween(400, easing = FastOutSlowInEasing))

        // 5. Hold, then navigate
        delay(1200)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(bgAlpha.value)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0B2545), // Dark navy top
                        Color(0xFF0D3268), // Deep royal blue bottom
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo with spring scale
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value)
                    .clip(RoundedCornerShape(32.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.focusfort_logo),
                    contentDescription = "FocusFort Logo",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // App name
            Text(
                text = "FocusFort",
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 1.sp,
                modifier = Modifier.alpha(textAlpha.value)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline
            Text(
                text = "Your digital discipline companion",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF90CAF9),
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(taglineAlpha.value)
            )
        }
    }
}
