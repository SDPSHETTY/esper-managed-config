package com.example.myapplication.data

import android.content.Context
import android.content.RestrictionsManager
import android.os.Bundle
import com.example.myapplication.utils.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Handles reading Esper Managed Configuration values using Android's RestrictionsManager.
 * These values are injected by Esper during device provisioning and contain
 * authoritative device identifiers.
 */
class RestrictionsReader(private val context: Context) {

    private val restrictionsManager by lazy {
        context.getSystemService(Context.RESTRICTIONS_SERVICE) as? RestrictionsManager
    }

    /**
     * Reads all managed configuration values asynchronously.
     * Returns null values for keys that are not configured or accessible.
     */
    suspend fun readManagedConfiguration(): Result<ManagedConfigData> = withContext(Dispatchers.IO) {
        try {
            val restrictions = restrictionsManager?.applicationRestrictions ?: Bundle.EMPTY

            val configData = ManagedConfigData(
                serialNumber = restrictions.getString("serialNumber"),
                uuid = restrictions.getString("uuid"),
                imei1 = restrictions.getString("imei1"),
                imei2 = restrictions.getString("imei2"),
                deviceName = restrictions.getString("deviceName"),
                macAddress = restrictions.getString("macAddress")
            )

            Result.success(configData)
        } catch (exception: Exception) {
            Result.error(
                Exception("Failed to read managed configuration", exception)
            )
        }
    }

    /**
     * Checks if managed configuration is available and readable.
     */
    fun isManagedConfigurationAvailable(): Boolean {
        return restrictionsManager != null
    }

    /**
     * Gets raw application restrictions bundle for debugging.
     */
    suspend fun getRawRestrictions(): Result<Bundle> = withContext(Dispatchers.IO) {
        try {
            val restrictions = restrictionsManager?.applicationRestrictions ?: Bundle.EMPTY
            Result.success(restrictions)
        } catch (exception: Exception) {
            Result.error(
                Exception("Failed to get raw restrictions", exception)
            )
        }
    }
}

/**
 * Data class for managed configuration values from Esper.
 * These are authoritative device identifiers injected during provisioning.
 */
data class ManagedConfigData(
    val serialNumber: String?,
    val uuid: String?,
    val imei1: String?,
    val imei2: String?,
    val deviceName: String?,
    val macAddress: String?
) {
    /**
     * Returns true if at least one managed configuration value is present.
     */
    fun hasAnyData(): Boolean {
        return listOfNotNull(serialNumber, uuid, imei1, imei2, deviceName, macAddress).isNotEmpty()
    }

    /**
     * Returns a map of non-null configuration values for logging/debugging.
     */
    fun toDebugMap(): Map<String, String> {
        return mapOf(
            "serialNumber" to (serialNumber ?: "null"),
            "uuid" to (uuid ?: "null"),
            "imei1" to (imei1 ?: "null"),
            "imei2" to (imei2 ?: "null"),
            "deviceName" to (deviceName ?: "null"),
            "macAddress" to (macAddress ?: "null")
        )
    }
}