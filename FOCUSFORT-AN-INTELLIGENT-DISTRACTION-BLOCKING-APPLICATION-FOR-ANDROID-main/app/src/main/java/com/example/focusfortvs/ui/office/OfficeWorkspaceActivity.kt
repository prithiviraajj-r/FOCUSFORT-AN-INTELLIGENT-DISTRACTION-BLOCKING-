package com.example.focusfortvs.ui.office

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.focusfortvs.ui.theme.FocusFortTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Full-screen Activity that hosts the Office Mode workspace.
 * Launched by OfficeModeUI in BlockOverlayScreen when the user taps
 * "Is it an Emergency? (Request Access)".
 *
 * Receives:
 *   EXTRA_PACKAGE_NAME  — target blocked app package
 *   EXTRA_APP_LABEL     — human-readable app name (e.g. "WhatsApp")
 */
@AndroidEntryPoint
class OfficeWorkspaceActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty()
        val appLabel    = intent.getStringExtra(EXTRA_APP_LABEL).orEmpty()

        setContent {
            FocusFortTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    OfficeWorkspaceScreen(
                        appName     = appLabel.ifBlank { "App" },
                        packageName = packageName,
                        onGoHome    = {
                            // Return to home screen
                            startActivity(
                                Intent(Intent.ACTION_MAIN).apply {
                                    addCategory(Intent.CATEGORY_HOME)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                            )
                            finish()
                        },
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "office_package_name"
        const val EXTRA_APP_LABEL    = "office_app_label"
    }
}
