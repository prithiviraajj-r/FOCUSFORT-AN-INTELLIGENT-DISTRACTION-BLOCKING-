package com.example.focusfortvs.ui.normal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.focusfortvs.data.repository.NormalModeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class NormalWorkspacePhase {
    data class Friction(val targetString: String, val userInput: String = "", val isError: Boolean = false) : NormalWorkspacePhase()
    object Complete : NormalWorkspacePhase()
}

@HiltViewModel
class NormalWorkspaceViewModel @Inject constructor(
    private val normalModeRepo: NormalModeRepository
) : ViewModel() {

    private val _phase = MutableStateFlow<NormalWorkspacePhase>(
        NormalWorkspacePhase.Friction("") // initialized properly later
    )
    val phase: StateFlow<NormalWorkspacePhase> = _phase

    private var targetPackageName: String = ""

    fun initialize(packageName: String) {
        targetPackageName = packageName
        val usageCount = normalModeRepo.getUsageCount(packageName)
        val stringLength = 6 + (usageCount * 2)
        _phase.value = NormalWorkspacePhase.Friction(generateRandomString(stringLength))
    }

    fun onInputChanged(newInput: String) {
        val currentPhase = _phase.value as? NormalWorkspacePhase.Friction ?: return
        _phase.value = currentPhase.copy(userInput = newInput, isError = false)
    }

    fun verifyInput() {
        val currentPhase = _phase.value as? NormalWorkspacePhase.Friction ?: return
        if (currentPhase.userInput == currentPhase.targetString) {
            normalModeRepo.startCooldown(targetPackageName)
            _phase.value = NormalWorkspacePhase.Complete
        } else {
            _phase.value = currentPhase.copy(isError = true)
        }
    }

    private fun generateRandomString(length: Int): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9') + listOf('!', '@', '#', '$', '%', '&', '*')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }
}
