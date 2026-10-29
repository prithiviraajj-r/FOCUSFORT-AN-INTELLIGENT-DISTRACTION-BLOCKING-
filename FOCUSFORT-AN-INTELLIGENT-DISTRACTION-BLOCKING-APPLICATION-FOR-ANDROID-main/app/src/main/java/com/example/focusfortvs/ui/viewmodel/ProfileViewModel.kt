package com.example.focusfortvs.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.focusfortvs.data.local.entity.BlockedAppEntity
import com.example.focusfortvs.data.repository.BlockedAppRepository
import com.example.focusfortvs.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: ProfileRepository,
    private val blockedAppRepository: BlockedAppRepository,
) : ViewModel() {
    private val _currentMode = MutableStateFlow("Student")
    val currentMode: StateFlow<String> = _currentMode

    fun setCurrentMode(mode: String) {
        viewModelScope.launch {
            repository.setCurrentMode(mode)
            _currentMode.value = mode
        }
    }

    suspend fun saveBlockedAppSelection(mode: String, packageName: String, appLabel: String) {
        blockedAppRepository.saveSelection(mode, packageName, appLabel)
    }

    suspend fun removeBlockedAppSelection(mode: String, packageName: String) {
        blockedAppRepository.removeSelection(mode, packageName)
    }

    suspend fun getBlockedAppsForMode(mode: String): List<BlockedAppEntity> {
        return blockedAppRepository.getForMode(mode)
    }
}
