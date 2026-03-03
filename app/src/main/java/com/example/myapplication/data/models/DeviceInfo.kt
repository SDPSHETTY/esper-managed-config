package com.example.myapplication.data.models

import kotlinx.serialization.Serializable

/**
 * Data class representing comprehensive device information collected from
 * Esper Managed Configuration and runtime Android APIs.
 */
@Serializable
data class DeviceInfo(
    // Esper managed configuration values (authoritative device identifiers)
    val serialNumber: String?,
    val uuid: String?,
    val imei1: String?,
    val imei2: String?,
    val deviceName: String?,
    val macAddress: String?,

    // Runtime device attributes (collected via Android APIs)
    val currentIpAddress: String?,
    val osVersion: String,
    val androidVersion: String,
    val manufacturer: String,
    val model: String,

    // Metadata
    val timestamp: Long,
    val appVersion: String
)

/**
 * Backend status indicator for sync operations.
 */
enum class BackendStatus {
    Unknown,
    Connected,
    Failed,
    Syncing
}

/**
 * Device sync request model for backend communication.
 */
@Serializable
data class DeviceSyncRequest(
    val deviceInfo: DeviceInfo,
    val requestId: String = generateRequestId()
) {
    companion object {
        private fun generateRequestId(): String {
            return "req_${System.currentTimeMillis()}_${(1000..9999).random()}"
        }
    }
}

/**
 * Backend response model for device sync operations.
 */
@Serializable
data class DeviceSyncResponse(
    val success: Boolean,
    val message: String,
    val requestId: String,
    val timestamp: Long,
    val deviceGroup: String? = null
)