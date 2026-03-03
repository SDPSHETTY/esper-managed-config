package com.example.myapplication.data.models

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.Assert.*

class DeviceInfoTest {

    @Test
    fun `DeviceInfo serializes to JSON correctly`() {
        val deviceInfo = createSampleDeviceInfo()

        val json = Json.encodeToString(deviceInfo)

        assertTrue(json.contains("serialNumber"))
        assertTrue(json.contains("uuid"))
        assertTrue(json.contains("currentIpAddress"))
        assertTrue(json.contains("osVersion"))
    }

    @Test
    fun `DeviceInfo deserializes from JSON correctly`() {
        val deviceInfo = createSampleDeviceInfo()
        val json = Json.encodeToString(deviceInfo)

        val deserialized = Json.decodeFromString<DeviceInfo>(json)

        assertEquals(deviceInfo, deserialized)
    }

    @Test
    fun `DeviceInfo handles null values correctly`() {
        val deviceInfo = DeviceInfo(
            serialNumber = null,
            uuid = null,
            imei1 = null,
            imei2 = null,
            deviceName = null,
            macAddress = null,
            currentIpAddress = null,
            osVersion = "14",
            androidVersion = "API 34",
            manufacturer = "Google",
            model = "Pixel 8",
            timestamp = System.currentTimeMillis(),
            appVersion = "1.0.0"
        )

        val json = Json.encodeToString(deviceInfo)
        val deserialized = Json.decodeFromString<DeviceInfo>(json)

        assertEquals(deviceInfo, deserialized)
        assertNull(deserialized.serialNumber)
        assertNull(deserialized.uuid)
    }

    @Test
    fun `DeviceSyncRequest generates unique request IDs`() {
        val deviceInfo = createSampleDeviceInfo()

        val request1 = DeviceSyncRequest(deviceInfo)
        val request2 = DeviceSyncRequest(deviceInfo)

        assertNotEquals(request1.requestId, request2.requestId)
        assertTrue(request1.requestId.startsWith("req_"))
        assertTrue(request2.requestId.startsWith("req_"))
    }

    @Test
    fun `DeviceSyncRequest serializes correctly`() {
        val deviceInfo = createSampleDeviceInfo()
        val request = DeviceSyncRequest(deviceInfo)

        val json = Json.encodeToString(request)
        val deserialized = Json.decodeFromString<DeviceSyncRequest>(json)

        assertEquals(request.deviceInfo, deserialized.deviceInfo)
        assertEquals(request.requestId, deserialized.requestId)
    }

    @Test
    fun `DeviceSyncResponse serializes correctly`() {
        val response = DeviceSyncResponse(
            success = true,
            message = "Device synchronized successfully",
            requestId = "req_123456789_1234",
            timestamp = System.currentTimeMillis(),
            deviceGroup = "office-devices"
        )

        val json = Json.encodeToString(response)
        val deserialized = Json.decodeFromString<DeviceSyncResponse>(json)

        assertEquals(response, deserialized)
    }

    @Test
    fun `BackendStatus enum has correct values`() {
        assertEquals(4, BackendStatus.values().size)
        assertTrue(BackendStatus.values().contains(BackendStatus.Unknown))
        assertTrue(BackendStatus.values().contains(BackendStatus.Connected))
        assertTrue(BackendStatus.values().contains(BackendStatus.Failed))
        assertTrue(BackendStatus.values().contains(BackendStatus.Syncing))
    }

    private fun createSampleDeviceInfo() = DeviceInfo(
        serialNumber = "SN123456789",
        uuid = "550e8400-e29b-41d4-a716-446655440000",
        imei1 = "123456789012345",
        imei2 = "543210987654321",
        deviceName = "Test Device",
        macAddress = "00:11:22:33:44:55",
        currentIpAddress = "192.168.1.100",
        osVersion = "14",
        androidVersion = "API 34",
        manufacturer = "Google",
        model = "Pixel 8",
        timestamp = 1640995200000L, // Fixed timestamp for testing
        appVersion = "1.0.0 (1)"
    )
}