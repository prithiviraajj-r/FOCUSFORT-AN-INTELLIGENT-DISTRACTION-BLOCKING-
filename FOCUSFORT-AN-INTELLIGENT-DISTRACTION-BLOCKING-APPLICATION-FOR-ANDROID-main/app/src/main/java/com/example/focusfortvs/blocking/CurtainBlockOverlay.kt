package com.example.focusfortvs.blocking

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

class CurtainBlockOverlay(
    private val context: Context,
    private val windowManager: WindowManager,
) {
    private val TAG = "FocusFortDebug"
    private var isAttached = false
    private var currentPackageName by mutableStateOf("")
    private var currentMode by mutableStateOf("")
    private var currentAppLabel by mutableStateOf("")

    // A fresh LifecycleOwner is created each time the overlay is shown
    // to avoid the "cannot move to CREATED from DESTROYED" crash.
    private var overlayLifecycleOwner: OverlayLifecycleOwner? = null
    private var composeView: ComposeView? = null

    fun show(mode: String, packageName: String, appLabel: String) {
        currentMode = mode
        currentPackageName = packageName
        currentAppLabel = appLabel
        if (!isAttached) {
            attach()
        }
    }

    fun update(mode: String, packageName: String, appLabel: String) {
        currentMode = mode
        currentPackageName = packageName
        currentAppLabel = appLabel
    }

    fun hide() {
        if (!isAttached) return
        detach()
    }

    fun isShowing(): Boolean = isAttached

    private fun buildComposeView(lifecycleOwner: OverlayLifecycleOwner): ComposeView {
        return ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent {
                BlockOverlayScreen(
                    mode = currentMode,
                    packageName = currentPackageName,
                    appLabel = currentAppLabel,
                    onDismiss = { hide() }
                )
            }
        }
    }

    private fun attach() {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        )

        try {
            val owner = OverlayLifecycleOwner()
            owner.start()
            val cv = buildComposeView(owner)
            windowManager.addView(cv, params)
            overlayLifecycleOwner = owner
            composeView = cv
            isAttached = true
            Log.d(TAG, "Overlay attached to WindowManager")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach overlay", e)
        }
    }

    private fun detach() {
        if (!isAttached) return
        try {
            composeView?.let { windowManager.removeViewImmediate(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove overlay view", e)
        }
        try {
            overlayLifecycleOwner?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop lifecycle owner", e)
        }
        composeView = null
        overlayLifecycleOwner = null
        isAttached = false
        Log.d(TAG, "Overlay detached")
    }
}

class OverlayLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController: SavedStateRegistryController = SavedStateRegistryController.create(this)
    private val store: ViewModelStore = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    fun start() {
        savedStateRegistryController.performRestore(Bundle())
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun stop() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }
}