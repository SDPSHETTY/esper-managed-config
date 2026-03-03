package com.example.myapplication.di

import android.content.Context
import com.example.myapplication.data.NetworkManager
import com.example.myapplication.data.RestrictionsReader
import com.example.myapplication.data.api.EsperApiService
import com.example.myapplication.data.repository.DeviceRepository
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Simple ServiceLocator pattern for dependency injection.
 * Provides singleton instances of key application components.
 * This can be easily migrated to Hilt if needed in the future.
 */
object ServiceLocator {

    // Configuration
    private const val BASE_URL = "https://api.example.com/" // TODO: Replace with actual backend URL
    private const val NETWORK_TIMEOUT = 30L // seconds

    // Lazy-initialized singletons
    private var deviceRepository: DeviceRepository? = null
    private var apiService: EsperApiService? = null
    private var okHttpClient: OkHttpClient? = null
    private var restrictionsReader: RestrictionsReader? = null
    private var networkManager: NetworkManager? = null

    // JSON serialization configuration
    private val json by lazy {
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
    }

    /**
     * Provides the DeviceRepository instance.
     */
    fun provideDeviceRepository(context: Context): DeviceRepository {
        return deviceRepository ?: synchronized(this) {
            deviceRepository ?: createDeviceRepository(context).also { deviceRepository = it }
        }
    }

    /**
     * Provides the EsperApiService instance.
     */
    fun provideApiService(): EsperApiService {
        return apiService ?: synchronized(this) {
            apiService ?: createApiService().also { apiService = it }
        }
    }

    /**
     * Provides the RestrictionsReader instance.
     */
    fun provideRestrictionsReader(context: Context): RestrictionsReader {
        return restrictionsReader ?: synchronized(this) {
            restrictionsReader ?: RestrictionsReader(context.applicationContext)
                .also { restrictionsReader = it }
        }
    }

    /**
     * Provides the NetworkManager instance.
     */
    fun provideNetworkManager(context: Context): NetworkManager {
        return networkManager ?: synchronized(this) {
            networkManager ?: NetworkManager(context.applicationContext)
                .also { networkManager = it }
        }
    }

    /**
     * Creates the DeviceRepository with all its dependencies.
     */
    private fun createDeviceRepository(context: Context): DeviceRepository {
        val appContext = context.applicationContext
        return DeviceRepository(
            context = appContext,
            restrictionsReader = provideRestrictionsReader(appContext),
            networkManager = provideNetworkManager(appContext),
            apiService = provideApiService()
        )
    }

    /**
     * Creates the EsperApiService with Retrofit.
     */
    private fun createApiService(): EsperApiService {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(provideOkHttpClient())
            .addConverterFactory(
                json.asConverterFactory("application/json".toMediaType())
            )
            .build()

        return retrofit.create(EsperApiService::class.java)
    }

    /**
     * Provides the OkHttpClient with logging and timeouts.
     */
    private fun provideOkHttpClient(): OkHttpClient {
        return okHttpClient ?: synchronized(this) {
            okHttpClient ?: createOkHttpClient().also { okHttpClient = it }
        }
    }

    /**
     * Creates the OkHttpClient with appropriate configuration.
     */
    private fun createOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(NETWORK_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(NETWORK_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(NETWORK_TIMEOUT, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Clears all cached instances. Useful for testing.
     */
    fun clearInstances() {
        deviceRepository = null
        apiService = null
        okHttpClient = null
        restrictionsReader = null
        networkManager = null
    }

    /**
     * Updates the backend base URL. Useful for testing or configuration changes.
     */
    fun updateBaseUrl(newBaseUrl: String) {
        // Clear API service to force recreation with new URL
        apiService = null
        okHttpClient = null
    }
}