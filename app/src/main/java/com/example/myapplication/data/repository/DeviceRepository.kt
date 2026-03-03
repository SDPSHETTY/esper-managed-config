package com.example.myapplication.data.repository

import android.content.Context
import android.content.pm.PackageManager
import com.example.myapplication.data.NetworkManager
import com.example.myapplication.data.RestrictionsReader
import com.example.myapplication.data.api.EsperApiService
import com.example.myapplication.data.models.BackendStatus
import com.example.myapplication.data.models.DeviceInfo
import com.example.myapplication.data.models.DeviceSyncRequest
import com.example.myapplication.data.models.DeviceSyncResponse
import com.example.myapplication.utils.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

/**
 * Repository that orchestrates device data collection from multiple sources:
 * - Esper Managed Configuration (via RestrictionsReader)
 * - Runtime device metadata (via NetworkManager)
 * - Backend communication (via EsperApiService)
 */
class DeviceRepository(
    private val context: Context,
    private val restrictionsReader: RestrictionsReader,
    private val networkManager: NetworkManager,
    private val apiService: EsperApiService
) {

    /**
     * Collects comprehensive device information from all available sources.
     * Combines Esper managed configuration with runtime device metadata.
     */
    suspend fun collectDeviceInfo(): Result<DeviceInfo> = withContext(Dispatchers.IO) {
        try {
            // Read managed configuration (authoritative identifiers)
            val managedConfigResult = restrictionsReader.readManagedConfiguration()
            val managedConfig = managedConfigResult.getOrNull()

            // Collect runtime device metadata
            val metadataResult = networkManager.collectDeviceMetadata()
            val metadata = metadataResult.getOrThrow()

            // Get current IP address
            val ipResult = networkManager.getCurrentIpAddress()
            val currentIp = ipResult.getOrNull()

            // Get IMEI information (may be null due to permissions)
            val imeiResult = networkManager.getDeviceIMEI()
            val imeiInfo = imeiResult.getOrNull()

            // Get app version
            val appVersion = getAppVersion()

            val deviceInfo = DeviceInfo(
                // From Esper managed configuration
                serialNumber = managedConfig?.serialNumber,
                uuid = managedConfig?.uuid,
                imei1 = managedConfig?.imei1 ?: imeiInfo?.imei1,
                imei2 = managedConfig?.imei2 ?: imeiInfo?.imei2,
                deviceName = managedConfig?.deviceName,
                macAddress = managedConfig?.macAddress,

                // From runtime APIs
                currentIpAddress = currentIp,
                osVersion = metadata.osVersion,
                androidVersion = metadata.androidVersion,
                manufacturer = metadata.manufacturer,
                model = metadata.model,

                // Metadata
                timestamp = System.currentTimeMillis(),
                appVersion = appVersion
            )

            Result.success(deviceInfo)
        } catch (exception: Exception) {
            Result.error(Exception("Failed to collect device information", exception))
        }
    }

    /**
     * Syncs device information with the backend service.
     * The backend will process this data and make policy decisions.
     */
    suspend fun syncWithBackend(deviceInfo: DeviceInfo): Result<DeviceSyncResponse> = withContext(Dispatchers.IO) {
        try {
            if (!networkManager.isConnectedToInternet()) {
                return@withContext Result.error(Exception("No internet connection available"))
            }

            val request = DeviceSyncRequest(deviceInfo = deviceInfo)
            val response = apiService.syncDeviceData(request)

            if (response.isSuccessful) {
                val syncResponse = response.body()
                if (syncResponse != null) {
                    Result.success(syncResponse)
                } else {
                    Result.error(Exception("Empty response from backend"))
                }
            } else {
                Result.error(Exception("Backend sync failed: ${response.code()} ${response.message()}"))
            }
        } catch (exception: Exception) {
            Result.error(Exception("Failed to sync with backend", exception))
        }
    }

    /**
     * Checks backend connectivity and health.
     */
    suspend fun checkBackendHealth(): Result<BackendStatus> = withContext(Dispatchers.IO) {
        try {
            if (!networkManager.isConnectedToInternet()) {
                return@withContext Result.success(BackendStatus.Failed)
            }

            val response = apiService.healthCheck()
            val status = if (response.isSuccessful && response.body()?.status == "ok") {
                BackendStatus.Connected
            } else {
                BackendStatus.Failed
            }

            Result.success(status)
        } catch (exception: Exception) {
            Result.success(BackendStatus.Failed)
        }
    }

    /**
     * Gets diagnostic information about managed configuration availability.
     */
    suspend fun getDiagnosticInfo(): Result<DiagnosticInfo> = withContext(Dispatchers.IO) {
        try {
            val isManagedConfigAvailable = restrictionsReader.isManagedConfigurationAvailable()
            val rawRestrictionsResult = restrictionsReader.getRawRestrictions()
            val restrictionsBundle = rawRestrictionsResult.getOrNull()

            val managedConfigResult = restrictionsReader.readManagedConfiguration()
            val managedConfig = managedConfigResult.getOrNull()

            val isConnected = networkManager.isConnectedToInternet()
            val networkType = networkManager.getNetworkType()

            val diagnosticInfo = DiagnosticInfo(
                isManagedConfigAvailable = isManagedConfigAvailable,
                hasManagedConfigData = managedConfig?.hasAnyData() == true,
                isNetworkConnected = isConnected,
                networkType = networkType,
                restrictionsCount = restrictionsBundle?.keySet()?.size ?: 0,
                appVersion = getAppVersion()
            )

            Result.success(diagnosticInfo)
        } catch (exception: Exception) {
            Result.error(Exception("Failed to collect diagnostic information", exception))
        }
    }

    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            "${packageInfo.versionName} ($versionCode)"
        } catch (e: PackageManager.NameNotFoundException) {
            "Unknown"
        }
    }
}

/**
 * Diagnostic information for troubleshooting and debugging.
 */
data class DiagnosticInfo(
    val isManagedConfigAvailable: Boolean,
    val hasManagedConfigData: Boolean,
    val isNetworkConnected: Boolean,
    val networkType: String,
    val restrictionsCount: Int,
    val appVersion: String
)