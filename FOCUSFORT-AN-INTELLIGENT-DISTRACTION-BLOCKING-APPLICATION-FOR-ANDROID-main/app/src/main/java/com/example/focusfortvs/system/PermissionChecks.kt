package com.example.focusfortvs.system

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.content.ComponentName

fun hasUsageAccess(context: Context): Boolean {
    val appOpsManager = context.getSystemService(AppOpsManager::class.java) ?: return false
    val packageInfo = context.packageManager.getApplicationInfo(context.packageName, 0)
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        @Suppress("DEPRECATION")
        appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, packageInfo.uid, context.packageName)
    } else {
        AppOpsManager.MODE_ERRORED
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

fun hasOverlayPermission(context: Context): Boolean = Settings.canDrawOverlays(context)