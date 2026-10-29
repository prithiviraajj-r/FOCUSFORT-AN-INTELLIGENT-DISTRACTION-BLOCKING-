package com.example.focusfortvs.ui.detox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.focusfortvs.data.network.AppAdvisoryResult
import com.example.focusfortvs.data.network.AzureOpenAIRepository
import com.example.focusfortvs.data.repository.BlockedAppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AdvisoryState {
    object Loading : AdvisoryState()
    data class Success(val results: List<AppAdvisoryResult>) : AdvisoryState()
    data class Error(val message: String) : AdvisoryState()
}

@HiltViewModel
class DetoxAdvisoryViewModel @Inject constructor(
    private val blockedAppRepository: BlockedAppRepository,
    private val openAIRepository: AzureOpenAIRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AdvisoryState>(AdvisoryState.Loading)
    val uiState: StateFlow<AdvisoryState> = _uiState

    fun loadAndEvaluate() {
        viewModelScope.launch {
            _uiState.value = AdvisoryState.Loading
            
            // 1. Get the essential apps selected by the user for "Digital Detox"
            val selectedApps = blockedAppRepository.getForMode("Digital Detox")
            if (selectedApps.isEmpty()) {
                // If they selected nothing, just return success with empty list
                _uiState.value = AdvisoryState.Success(emptyList())
                return@launch
            }

            val appNames = selectedApps.map { it.appLabel }
            
            // 2. Call OpenAI to evaluate
            val results = openAIRepository.evaluateEssentialApps(appNames)
            
            if (results.isEmpty()) {
                _uiState.value = AdvisoryState.Error("Failed to connect to the advisor. Please check your internet connection or try again.")
            } else {
                _uiState.value = AdvisoryState.Success(results)
            }
        }
    }

    fun unselectApp(appName: String) {
        viewModelScope.launch {
            // Remove from database
            val currentSelected = blockedAppRepository.getForMode("Digital Detox")
            val appToRemove = currentSelected.find { it.appLabel.equals(appName, ignoreCase = true) }
            
            if (appToRemove != null) {
                blockedAppRepository.removeSelection("Digital Detox", appToRemove.packageName)
            }
            
            // Update UI state
            val currentState = _uiState.value
            if (currentState is AdvisoryState.Success) {
                val newList = currentState.results.filter { !it.appName.equals(appName, ignoreCase = true) }
                _uiState.value = AdvisoryState.Success(newList)
            }
        }
    }
}
