package com.example.focusfortvs.blocking

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.getSystemService
import com.example.focusfortvs.MainActivity
import com.example.focusfortvs.R
import com.example.focusfortvs.system.hasOverlayPermission
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay

import com.example.focusfortvs.data.repository.SessionRepository
import com.example.focusfortvs.data.repository.BlockedAppRepository
import javax.inject.Inject

@AndroidEntryPoint
class FocusSessionService : Service() {

    @Inject lateinit var sessionRepository: SessionRepository
    @Inject lateinit var blockedAppRepository: BlockedAppRepository
    @Inject lateinit var appWhitelistManager: AppWhitelistManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var overlayController: CurtainBlockOverlay? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "FocusSessionService onCreate")
        createNotificationChannel()
        startAsForeground()
        // Create overlay on the main thread
        mainHandler.post {
            try {
                val wm = getSystemService<WindowManager>()
                if (wm != null) {
                    overlayController = CurtainBlockOverlay(this, wm)
                    Log.d(TAG, "CurtainBlockOverlay created successfully")
                } else {
                    Log.e(TAG, "WindowManager is null!")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create overlay", e)
            }
        }
        
        startPollingLoop()
    }

    private fun startPollingLoop() {
        serviceScope.launch {
            val usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
            
            var lastKnownForegroundApp: String? = null

            while (isActive) {
                val activeSession = sessionRepository.getActiveSession()
                
                if (activeSession != null) {
                    // Check if timer expired
                    if (activeSession.durationMinutes != null) {
                        val endTime = activeSession.startTimeMillis + (activeSession.durationMinutes * 60 * 1000L)
                        if (System.currentTimeMillis() >= endTime) {
                            Log.d(TAG, "Session timer expired!")
                            sessionRepository.stopActiveSession()
                            mainHandler.post { hideOverlay() }
                            stopSelf()
                            return@launch
                        }
                    }

                    val mode = activeSession.mode.trim()
                    // Retrieve blocked list for blacklist modes
                    // For Digital Detox, this is actually the essential apps list
                    val blockedList = blockedAppRepository.getForMode(mode)
                        .filter { it.isEnabled }
                        .map { it.packageName.trim() }

                    val endTime = System.currentTimeMillis()
                    val startTime = endTime - 10000 // look back 10 seconds

                    val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
                    val event = android.app.usage.UsageEvents.Event()
                    while (usageEvents.hasNextEvent()) {
                        usageEvents.getNextEvent(event)
                        if (event.eventType == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND || 
                            event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED) {
                            lastKnownForegroundApp = event.packageName
                        }
                    }

                    if (lastKnownForegroundApp != null) {
                        val foregroundApp = lastKnownForegroundApp!!
                        
                        if (mode == "Digital Detox") {
                            val defaultLauncher = getDefaultLauncherPackage()
                            val isAllowed = blockedList.any { it.equals(foregroundApp, ignoreCase = true) } || 
                                          foregroundApp == packageName || 
                                          foregroundApp == defaultLauncher

                            if (isAllowed) {
                                mainHandler.post { hideOverlay() }
                            } else {
                                val appLabel = resolveAppLabel(foregroundApp)
                                mainHandler.post { showOverlay(foregroundApp, mode, appLabel) }
                            }
                        } else {
                            if (foregroundApp == packageName) {
                                mainHandler.post { hideOverlay() }
                            } else if (appWhitelistManager.isWhitelisted(foregroundApp)) {
                                // Temporary whitelist bypass for Office/Normal mode
                                mainHandler.post { hideOverlay() }
                            } else {
                                val isBlocked = blockedList.any { it.equals(foregroundApp, ignoreCase = true) }
                                if (isBlocked) {
                                    val appLabel = resolveAppLabel(foregroundApp)
                                    mainHandler.post { showOverlay(foregroundApp, mode, appLabel) }
                                } else {
                                    mainHandler.post { hideOverlay() }
                                }
                            }
                        }
                    }
                }
                delay(1000)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand action=${intent?.action}")

        when (intent?.action) {
            ACTION_SHOW_BLOCK -> {
                val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty()
                val mode = intent.getStringExtra(EXTRA_MODE).orEmpty()
                val appLabel = intent.getStringExtra(EXTRA_APP_LABEL).orEmpty()
                Log.d(TAG, "SHOW_BLOCK: pkg=$packageName mode=$mode")
                mainHandler.post { showOverlay(packageName, mode, appLabel) }
            }
            ACTION_HIDE_BLOCK -> {
                Log.d(TAG, "HIDE_BLOCK")
                mainHandler.post { hideOverlay() }
            }
            // Legacy compat
            ACTION_UPDATE_OVERLAY -> {
                val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty()
                val isBlocked = intent.getBooleanExtra(EXTRA_IS_BLOCKED, false)
                val mode = intent.getStringExtra(EXTRA_MODE).orEmpty()
                val appLabel = resolveAppLabel(packageName)
                Log.d(TAG, "UPDATE_OVERLAY: pkg=$packageName isBlocked=$isBlocked mode=$mode")
                if (isBlocked) {
                    mainHandler.post { showOverlay(packageName, mode, appLabel) }
                } else {
                    mainHandler.post { hideOverlay() }
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "FocusSessionService onDestroy")
        mainHandler.post { hideOverlay() }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun showOverlay(packageName: String, mode: String, appLabel: String) {
        if (!hasOverlayPermission(this)) {
            Log.w(TAG, "No overlay permission — cannot show block screen")
            return
        }
        try {
            val oc = overlayController
            if (oc == null) {
                Log.w(TAG, "overlayController is null, recreating")
                val wm = getSystemService<WindowManager>() ?: return
                overlayController = CurtainBlockOverlay(this, wm)
            }
            if (overlayController?.isShowing() == true) {
                overlayController?.update(mode, packageName, appLabel)
                Log.d(TAG, "Overlay updated")
            } else {
                overlayController?.show(mode, packageName, appLabel)
                Log.d(TAG, "Overlay shown")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show overlay", e)
        }
    }

    private fun hideOverlay() {
        try {
            if (overlayController?.isShowing() == true) {
                overlayController?.hide()
                Log.d(TAG, "Overlay hidden")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to hide overlay", e)
        }
    }

    private fun resolveAppLabel(packageName: String): String {
        return try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(packageName, 0)
            ).toString()
        } catch (_: Exception) { packageName }
    }

    private fun getDefaultLauncherPackage(): String {
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
        val resolveInfo = packageManager.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName ?: ""
    }

    private fun startAsForeground() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("FocusFort Active")
            .setContentText("Blocking distractions via Usage Access")
            .setOngoing(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
            .build()

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService<NotificationManager>() ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "FocusFort Session",
            NotificationManager.IMPORTANCE_LOW,
        )
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "FocusFortDebug"
        private const val CHANNEL_ID = "focusfort_session"
        private const val NOTIFICATION_ID = 401

        const val ACTION_SHOW_BLOCK = "com.example.focusfortvs.ACTION_SHOW_BLOCK"
        const val ACTION_HIDE_BLOCK = "com.example.focusfortvs.ACTION_HIDE_BLOCK"
        // Legacy — keep for compat
        const val ACTION_UPDATE_OVERLAY = "com.example.focusfortvs.ACTION_UPDATE_OVERLAY"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_IS_BLOCKED = "extra_is_blocked"
        const val EXTRA_MODE = "extra_mode"
        const val EXTRA_APP_LABEL = "extra_app_label"
    }
}