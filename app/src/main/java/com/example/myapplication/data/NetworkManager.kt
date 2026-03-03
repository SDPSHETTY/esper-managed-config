package com.example.myapplication.data

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.TelephonyManager
import com.example.myapplication.utils.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Collections

/**
 * Handles network-related device information collection including IP addresses,
 * device metadata, and runtime system information.
 */
class NetworkManager(private val context: Context) {

    private val connectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    private val wifiManager by lazy {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    private val telephonyManager by lazy {
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    }

    /**
     * Gets the current IP address of the device by examining all network interfaces.
     * Returns the first non-loopback, IPv4 address found.
     */
    suspend fun getCurrentIpAddress(): Result<String?> = withContext(Dispatchers.IO) {
        try {
            val networkInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces())

            for (networkInterface in networkInterfaces) {
                val addresses = Collections.list(networkInterface.inetAddresses)

                for (address in addresses) {
                    if (!address.isLoopbackAddress && address is InetAddress) {
                        val hostAddress = address.hostAddress

                        // Return first IPv4 address (avoid IPv6 for simplicity)
                        if (hostAddress?.contains(':') == false) {
                            return@withContext Result.success(hostAddress)
                        }
                    }
                }
            }

            Result.success(null) // No IP address found
        } catch (exception: Exception) {
            Result.error(Exception("Failed to get current IP address", exception))
        }
    }

    /**
     * Checks if the device is currently connected to the internet.
     */
    fun isConnectedToInternet(): Boolean {
        return try {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (exception: Exception) {
            false
        }
    }

    /**
     * Gets network connection type (WiFi, Cellular, etc.)
     */
    fun getNetworkType(): String {
        return try {
            val network = connectivityManager.activeNetwork ?: return "None"
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "Unknown"

            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                else -> "Unknown"
            }
        } catch (exception: Exception) {
            "Error"
        }
    }

    /**
     * Collects comprehensive runtime device metadata.
     */
    @SuppressLint("HardwareIds")
    suspend fun collectDeviceMetadata(): Result<DeviceMetadata> = withContext(Dispatchers.IO) {
        try {
            val metadata = DeviceMetadata(
                osVersion = Build.VERSION.RELEASE,
                androidVersion = "API ${Build.VERSION.SDK_INT}",
                manufacturer = Build.MANUFACTURER,
                model = Build.MODEL,
                board = Build.BOARD,
                device = Build.DEVICE,
                product = Build.PRODUCT,
                hardware = Build.HARDWARE,
                networkType = getNetworkType(),
                isConnected = isConnectedToInternet()
            )

            Result.success(metadata)
        } catch (exception: Exception) {
            Result.error(Exception("Failed to collect device metadata", exception))
        }
    }

    /**
     * Gets device IMEI information (requires READ_PHONE_STATE permission).
     * Returns null if permission is not granted or on devices without telephony.
     */
    @SuppressLint("HardwareIds", "MissingPermission")
    suspend fun getDeviceIMEI(): Result<IMEIInfo?> = withContext(Dispatchers.IO) {
        try {
            if (!hasTelephonyFeature()) {
                return@withContext Result.success(null)
            }

            // Note: These methods require READ_PHONE_STATE permission
            // and may return null on newer Android versions due to privacy restrictions
            val imei1 = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try { telephonyManager.getImei(0) } catch (e: Exception) { null }
            } else {
                try { @Suppress("DEPRECATION") telephonyManager.deviceId } catch (e: Exception) { null }
            }

            val imei2 = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try { telephonyManager.getImei(1) } catch (e: Exception) { null }
            } else {
                null
            }

            val imeiInfo = IMEIInfo(imei1 = imei1, imei2 = imei2)
            Result.success(imeiInfo)
        } catch (exception: Exception) {
            // Return null instead of error for permission-related issues
            Result.success(null)
        }
    }

    private fun hasTelephonyFeature(): Boolean {
        return context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_TELEPHONY)
    }
}

/**
 * Runtime device metadata collected via Android APIs.
 */
data class DeviceMetadata(
    val osVersion: String,
    val androidVersion: String,
    val manufacturer: String,
    val model: String,
    val board: String,
    val device: String,
    val product: String,
    val hardware: String,
    val networkType: String,
    val isConnected: Boolean
)

/**
 * IMEI information (may be null due to permissions or device capabilities).
 */
data class IMEIInfo(
    val imei1: String?,
    val imei2: String?
)