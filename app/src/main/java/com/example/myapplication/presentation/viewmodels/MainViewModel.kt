package com.example.myapplication.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.models.BackendStatus
import com.example.myapplication.data.repository.DeviceRepository
import com.example.myapplication.utils.Result
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Main ViewModel for the device information screen.
 * Handles reactive state management, automatic data collection, and periodic backend syncing.
 */
class MainViewModel(
    private val deviceRepository: DeviceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _uiEffects = MutableSharedFlow<UiEffect>()
    val uiEffects: SharedFlow<UiEffect> = _uiEffects.asSharedFlow()

    private var syncJob: Job? = null
    private var periodicSyncJob: Job? = null

    // Configuration
    private val periodicSyncInterval = 300_000L // 5 minutes in milliseconds

    init {
        // Start initial data collection on ViewModel creation
        collectInitialData()

        // Start periodic sync
        startPeriodicSync()
    }

    /**
     * Handles UI events from the user interface.
     */
    fun handleEvent(event: UiEvent) {
        when (event) {
            is UiEvent.Refresh -> refreshData()
            is UiEvent.RetryLastOperation -> retryLastOperation()
            is UiEvent.DismissError -> dismissError()
            is UiEvent.CheckBackendHealth -> checkBackendHealth()
        }
    }

    /**
     * Refreshes all data (device info and backend sync).
     */
    private fun refreshData() {
        if (_uiState.value.isBusy) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)

            try {
                // Collect fresh device information
                collectDeviceData()

                // Try to sync with backend
                syncWithBackend()

                showEffect(UiEffect.ShowSnackbar("Data refreshed successfully"))
            } catch (e: Exception) {
                handleError(UiError.deviceDataCollection(e))
            } finally {
                _uiState.value = _uiState.value.copy(isRefreshing = false)
            }
        }
    }

    /**
     * Retries the last failed operation.
     */
    private fun retryLastOperation() {
        val currentError = _uiState.value.error
        if (currentError?.canRetry == true) {
            when {
                _uiState.value.deviceInfo == null -> collectInitialData()
                _uiState.value.backendStatus == BackendStatus.Failed -> syncWithBackend()
                else -> refreshData()
            }
        }
    }

    /**
     * Dismisses the current error state.
     */
    private fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Checks backend health status.
     */
    private fun checkBackendHealth() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(syncProgress = SyncProgress.ContactingBackend)

            when (val result = deviceRepository.checkBackendHealth()) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        backendStatus = result.data,
                        syncProgress = SyncProgress.Idle
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        backendStatus = BackendStatus.Failed,
                        syncProgress = SyncProgress.Idle
                    )
                }
            }
        }
    }

    /**
     * Collects initial device data on app start.
     */
    private fun collectInitialData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                syncProgress = SyncProgress.CollectingDeviceInfo
            )

            try {
                // Collect device information and diagnostic data in parallel
                collectDeviceData()
                collectDiagnosticData()

                // Check if managed configuration is available
                val diagnosticInfo = _uiState.value.diagnosticInfo
                if (diagnosticInfo?.isManagedConfigAvailable == false) {
                    handleError(UiError.managedConfig())
                } else if (diagnosticInfo?.hasManagedConfigData == false) {
                    showEffect(UiEffect.ShowSnackbar("No managed configuration data found"))
                }

                // Try initial backend sync
                syncWithBackend()

            } catch (e: Exception) {
                handleError(UiError.deviceDataCollection(e))
            } finally {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    syncProgress = SyncProgress.Idle
                )
            }
        }
    }

    /**
     * Collects device information from repository.
     */
    private suspend fun collectDeviceData() {
        when (val result = deviceRepository.collectDeviceInfo()) {
            is Result.Success -> {
                _uiState.value = _uiState.value.copy(deviceInfo = result.data)
            }
            is Result.Error -> {
                throw result.exception
            }
        }
    }

    /**
     * Collects diagnostic information.
     */
    private suspend fun collectDiagnosticData() {
        when (val result = deviceRepository.getDiagnosticInfo()) {
            is Result.Success -> {
                _uiState.value = _uiState.value.copy(diagnosticInfo = result.data)
            }
            is Result.Error -> {
                // Diagnostic info failure is not critical
                showEffect(UiEffect.ShowSnackbar("Failed to collect diagnostic information"))
            }
        }
    }

    /**
     * Syncs device data with the backend service.
     */
    private fun syncWithBackend() {
        val deviceInfo = _uiState.value.deviceInfo ?: return

        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                backendStatus = BackendStatus.Syncing,
                syncProgress = SyncProgress.SyncingData
            )

            when (val result = deviceRepository.syncWithBackend(deviceInfo)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        backendStatus = BackendStatus.Connected,
                        lastSyncTime = System.currentTimeMillis(),
                        syncProgress = SyncProgress.Completed
                    )

                    // Show success message if device group was assigned
                    result.data.deviceGroup?.let { group ->
                        showEffect(UiEffect.ShowSnackbar("Device assigned to group: $group"))
                    }

                    delay(1000) // Show completed state briefly
                    _uiState.value = _uiState.value.copy(syncProgress = SyncProgress.Idle)
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        backendStatus = BackendStatus.Failed,
                        syncProgress = SyncProgress.Idle
                    )
                    handleError(UiError.backendSync(result.exception))
                }
            }
        }
    }

    /**
     * Starts periodic background syncing with the backend.
     */
    private fun startPeriodicSync() {
        periodicSyncJob = viewModelScope.launch {
            while (true) {
                delay(periodicSyncInterval)

                // Only sync if we have device info and aren't already busy
                if (_uiState.value.deviceInfo != null && !_uiState.value.isBusy) {
                    try {
                        syncWithBackend()
                    } catch (e: Exception) {
                        // Periodic sync failures are logged but don't show errors to user
                        println("Periodic sync failed: ${e.message}")
                    }
                }
            }
        }
    }

    /**
     * Handles errors by updating UI state and showing appropriate effects.
     */
    private fun handleError(error: UiError) {
        _uiState.value = _uiState.value.copy(error = error)
        viewModelScope.launch {
            _uiEffects.emit(UiEffect.ShowError(error))
        }
    }

    /**
     * Shows a UI effect (snackbar, navigation, etc.).
     */
    private fun showEffect(effect: UiEffect) {
        viewModelScope.launch {
            _uiEffects.emit(effect)
        }
    }

    override fun onCleared() {
        super.onCleared()
        syncJob?.cancel()
        periodicSyncJob?.cancel()
    }
}