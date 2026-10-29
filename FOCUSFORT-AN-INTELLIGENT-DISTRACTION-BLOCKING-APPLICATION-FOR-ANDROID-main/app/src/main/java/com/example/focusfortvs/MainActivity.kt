package com.example.focusfortvs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.focusfortvs.ui.navigation.FocusFortDestination
import com.example.focusfortvs.ui.navigation.FocusFortNavHost
import com.example.focusfortvs.ui.theme.FocusFortTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        var keepSplash = true
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { keepSplash }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = getSharedPreferences("focusfort_prefs", android.content.Context.MODE_PRIVATE)
        val isFirstLaunch = prefs.getBoolean("is_first_launch", true)
        val openWorkspace = intent.getBooleanExtra("OPEN_WORKSPACE", false)

        // Hold native splash screen very briefly to avoid flicker
        lifecycleScope.launch {
            delay(100)
            keepSplash = false
        }

        val startDestination = when {
            openWorkspace -> "student_workspace"
            else -> FocusFortDestination.Splash.route
        }

        setContent {
            FocusFortTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FocusFortNavHost(startDestination = startDestination)
                }
            }
        }
    }
}