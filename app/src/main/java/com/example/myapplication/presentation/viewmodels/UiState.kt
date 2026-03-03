package com.example.myapplication.presentation.viewmodels

import com.example.myapplication.data.models.BackendStatus
import com.example.myapplication.data.models.DeviceInfo
import com.example.myapplication.data.repository.DiagnosticInfo

/**
 * UI state for the main device information screen.
 * Represents all the data needed to render the UI and its current state.
 */
data class UiState(
    val deviceInfo: DeviceInfo? = null,
    val diagnosticInfo: DiagnosticInfo? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val lastSyncTime: Long? = null,
    val error: UiError? = null,
    val backendStatus: BackendStatus = BackendStatus.Unknown,
    val syncProgress: SyncProgress = SyncProgress.Idle
) {
    /**
     * True if device info has been loaded at least once.
     */
    val hasData: Boolean get() = deviceInfo != null

    /**
     * True if there's currently any ongoing operation.
     */
    val isBusy: Boolean get() = isLoading || isRefreshing || syncProgress != SyncProgress.Idle

    /**
     * True if the app is in an error state that blocks normal operation.
     */
    val hasBlockingError: Boolean get() = error?.isBlocking == true
}

/**
 * Represents different types of UI errors with appropriate user messages.
 */
data class UiError(
    val message: String,
    val cause: Throwable? = null,
    val isBlocking: Boolean = false,
    val canRetry: Boolean = true
) {
    companion object {
        fun deviceDataCollection(cause: Throwable): UiError = UiError(
            message = "Failed to collect device information. Please check device permissions.",
            cause = cause,
            isBlocking = false,
            canRetry = true
        )

        fun networkError(cause: Throwable): UiError = UiError(
            message = "Network connection failed. Please check your internet connection.",
            cause = cause,
            isBlocking = false,
            canRetry = true
        )

        fun backendSync(cause: Throwable): UiError = UiError(
            message = "Failed to sync with backend service.",
            cause = cause,
            isBlocking = false,
            canRetry = true
        )

        fun permissions(): UiError = UiError(
            message = "Required permissions not granted. Some device information may be unavailable.",
            isBlocking = false,
            canRetry = false
        )

        fun managedConfig(): UiError = UiError(
            message = "Managed configuration not available. This app requires Esper device management.",
            isBlocking = true,
            canRetry = false
        )
    }
}

/**
 * Represents the progress of sync operations.
 */
enum class SyncProgress {
    Idle,
    CollectingDeviceInfo,
    ContactingBackend,
    SyncingData,
    Completed
}

/**
 * UI events that can be triggered by user actions.
 */
sealed class UiEvent {
    object Refresh : UiEvent()
    object RetryLastOperation : UiEvent()
    object DismissError : UiEvent()
    object CheckBackendHealth : UiEvent()
}

/**
 * One-time UI effects that should be consumed by the UI.
 */
sealed class UiEffect {
    data class ShowSnackbar(val message: String) : UiEffect()
    data class ShowError(val error: UiError) : UiEffect()
    object NavigateToSettings : UiEffect()
}