package com.example.myapplication.data.api

import com.example.myapplication.data.models.DeviceSyncRequest
import com.example.myapplication.data.models.DeviceSyncResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Retrofit service interface for communicating with the backend service.
 * The backend is responsible for:
 * - Receiving device metadata and identity information
 * - Making policy decisions about device group placement
 * - Invoking Esper APIs for device management
 */
interface EsperApiService {

    /**
     * Reports device information to the backend service.
     * The backend will process this data and potentially move the device
     * to appropriate Esper device groups based on business logic.
     */
    @POST("api/v1/device/sync")
    suspend fun syncDeviceData(@Body request: DeviceSyncRequest): Response<DeviceSyncResponse>

    /**
     * Health check endpoint to verify backend connectivity.
     */
    @GET("api/v1/health")
    suspend fun healthCheck(): Response<HealthResponse>

    /**
     * Gets the current configuration/status for this device from the backend.
     */
    @GET("api/v1/device/status")
    suspend fun getDeviceStatus(): Response<DeviceStatusResponse>
}

/**
 * Health check response model.
 */
data class HealthResponse(
    val status: String,
    val timestamp: Long,
    val version: String
)

/**
 * Device status response from backend.
 */
data class DeviceStatusResponse(
    val deviceId: String,
    val currentGroup: String?,
    val targetGroup: String?,
    val lastSyncTime: Long,
    val nextSyncTime: Long?,
    val status: String
)