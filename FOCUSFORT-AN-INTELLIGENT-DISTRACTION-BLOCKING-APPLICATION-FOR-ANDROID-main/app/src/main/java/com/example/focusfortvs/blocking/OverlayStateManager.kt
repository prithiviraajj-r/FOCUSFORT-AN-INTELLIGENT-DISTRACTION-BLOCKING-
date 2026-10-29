package com.example.focusfortvs.blocking

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed class BlockEvent {
    data class Show(val packageName: String, val mode: String, val appLabel: String) : BlockEvent()
    object Hide : BlockEvent()
}

object OverlayStateManager {
    private val _events = MutableSharedFlow<BlockEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<BlockEvent> = _events.asSharedFlow()

    fun showOverlay(packageName: String, mode: String, appLabel: String) {
        _events.tryEmit(BlockEvent.Show(packageName, mode, appLabel))
    }

    fun hideOverlay() {
        _events.tryEmit(BlockEvent.Hide)
    }
}
