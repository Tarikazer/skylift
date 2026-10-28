package com.example.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.BackupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isExporting: Boolean = false,
    val isRestoring: Boolean = false,
    val isSeeding: Boolean = false,
    val message: String? = null,
    val lastExportedFilePath: String? = null,
    val pendingExportPayload: String? = null
)

class SettingsViewModel(
    private val backupRepository: BackupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun prepareExport(onReady: (jsonPayload: String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, message = null)
            val summary = backupRepository.generateBackupJson()
            _uiState.value = _uiState.value.copy(
                isExporting = false,
                pendingExportPayload = summary.jsonPayload
            )
            onReady(summary.jsonPayload)
        }
    }

    fun exportToUri(uri: Uri) {
        val payload = _uiState.value.pendingExportPayload ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true)
            val success = backupRepository.saveBackupToUri(uri, payload)
            _uiState.value = _uiState.value.copy(
                isExporting = false,
                message = if (success) "Backup exported successfully!" else "Failed saving backup."
            )
        }
    }

    fun exportToInternalStorage() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true)
            val summary = backupRepository.generateBackupJson()
            val file = backupRepository.saveBackupToInternalStorage(summary.jsonPayload)
            _uiState.value = _uiState.value.copy(
                isExporting = false,
                lastExportedFilePath = file?.absolutePath,
                message = if (file != null) "Saved to local storage: ${file.name}" else "Export failed."
            )
        }
    }

    fun restoreFromUri(uri: Uri, clearExisting: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRestoring = true)
            val result = backupRepository.restoreFromUri(uri, clearExisting)
            _uiState.value = _uiState.value.copy(
                isRestoring = false,
                message = if (result.success) {
                    "Restored ${result.workoutsRestored} workouts, ${result.exercisesRestored} exercises, ${result.bodyWeightLogsRestored} weight logs!"
                } else {
                    "Restore error: ${result.errorMessage ?: "Unknown error"}"
                }
            )
        }
    }

    fun loadSampleData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSeeding = true)
            backupRepository.seedSampleData()
            _uiState.value = _uiState.value.copy(
                isSeeding = false,
                message = "Starter workout routines & bodyweight data loaded!"
            )
        }
    }

    fun loadUserProgram() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSeeding = true)
            val created = backupRepository.loadCustomUserProgram()
            _uiState.value = _uiState.value.copy(
                isSeeding = false,
                message = if (created > 0) {
                    "Your weekly program ($created routines) loaded and assigned to your weekly split!"
                } else {
                    "Your weekly program routines are already in your routines list."
                }
            )
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}

class SettingsViewModelFactory(
    private val backupRepository: BackupRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SettingsViewModel(backupRepository) as T
    }
}
