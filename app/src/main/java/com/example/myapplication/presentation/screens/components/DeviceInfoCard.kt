package com.example.myapplication.presentation.screens.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.models.DeviceInfo
import com.example.myapplication.data.repository.DiagnosticInfo

/**
 * Card displaying device information with Material3 design.
 */
@Composable
fun DeviceInfoCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            content()
        }
    }
}

/**
 * Displays Esper managed configuration values.
 */
@Composable
fun EsperConfigCard(
    deviceInfo: DeviceInfo,
    modifier: Modifier = Modifier
) {
    DeviceInfoCard(
        title = "Esper Managed Configuration",
        icon = Icons.Filled.Settings, // Using Settings as Security is not available
        modifier = modifier,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoRow("Serial Number", deviceInfo.serialNumber ?: "Not available")
                InfoRow("UUID", deviceInfo.uuid ?: "Not available")
                InfoRow("Device Name", deviceInfo.deviceName ?: "Not available")
                InfoRow("MAC Address", deviceInfo.macAddress ?: "Not available")
                InfoRow("IMEI 1", deviceInfo.imei1 ?: "Not available")
                InfoRow("IMEI 2", deviceInfo.imei2 ?: "Not available")
            }
        }
    )
}

/**
 * Displays runtime device metadata.
 */
@Composable
fun DeviceMetadataCard(
    deviceInfo: DeviceInfo,
    modifier: Modifier = Modifier
) {
    DeviceInfoCard(
        title = "Device Information",
        icon = Icons.Filled.Phone,
        modifier = modifier,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoRow("Manufacturer", deviceInfo.manufacturer)
                InfoRow("Model", deviceInfo.model)
                InfoRow("OS Version", deviceInfo.osVersion)
                InfoRow("Android Version", deviceInfo.androidVersion)
                InfoRow("Current IP", deviceInfo.currentIpAddress ?: "Not available")
                InfoRow("App Version", deviceInfo.appVersion)
            }
        }
    )
}

/**
 * Displays diagnostic and system information.
 */
@Composable
fun DiagnosticCard(
    diagnosticInfo: DiagnosticInfo,
    modifier: Modifier = Modifier
) {
    DeviceInfoCard(
        title = "System Diagnostics",
        icon = Icons.Filled.Settings,
        modifier = modifier,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusRow(
                    label = "Managed Config Available",
                    isPositive = diagnosticInfo.isManagedConfigAvailable
                )
                StatusRow(
                    label = "Has Config Data",
                    isPositive = diagnosticInfo.hasManagedConfigData
                )
                StatusRow(
                    label = "Network Connected",
                    isPositive = diagnosticInfo.isNetworkConnected
                )
                InfoRow("Network Type", diagnosticInfo.networkType)
                InfoRow("Restrictions Count", diagnosticInfo.restrictionsCount.toString())
            }
        }
    )
}

/**
 * Displays sync status and last sync time.
 */
@Composable
fun SyncStatusCard(
    backendStatus: com.example.myapplication.data.models.BackendStatus,
    lastSyncTime: Long?,
    syncProgress: com.example.myapplication.presentation.viewmodels.SyncProgress,
    modifier: Modifier = Modifier
) {
    DeviceInfoCard(
        title = "Backend Sync Status",
        icon = Icons.Filled.Settings, // Using Settings as Sync is not available
        modifier = modifier,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BackendStatusIndicator(status = backendStatus)
                    Text(
                        text = backendStatus.name,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (syncProgress != com.example.myapplication.presentation.viewmodels.SyncProgress.Idle) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = getSyncProgressText(syncProgress),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                lastSyncTime?.let {
                    InfoRow("Last Sync", formatTimestamp(it))
                }
            }
        }
    )
}

/**
 * Simple row for displaying key-value information.
 */
@Composable
private fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1.5f)
        )
    }
}

/**
 * Row for displaying boolean status with colored indicator.
 */
@Composable
private fun StatusRow(
    label: String,
    isPositive: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1.5f)
        ) {
            Icon(
                imageVector = if (isPositive) Icons.Filled.CheckCircle else Icons.Filled.Close,
                contentDescription = null,
                tint = if (isPositive) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = if (isPositive) "Yes" else "No",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Colored indicator for backend connection status.
 */
@Composable
private fun BackendStatusIndicator(
    status: com.example.myapplication.data.models.BackendStatus,
    modifier: Modifier = Modifier
) {
    val (color, icon) = when (status) {
        com.example.myapplication.data.models.BackendStatus.Connected ->
            MaterialTheme.colorScheme.primary to Icons.Filled.CheckCircle
        com.example.myapplication.data.models.BackendStatus.Failed ->
            MaterialTheme.colorScheme.error to Icons.Filled.Close
        com.example.myapplication.data.models.BackendStatus.Syncing ->
            MaterialTheme.colorScheme.tertiary to Icons.Filled.Settings
        com.example.myapplication.data.models.BackendStatus.Unknown ->
            MaterialTheme.colorScheme.onSurfaceVariant to Icons.Filled.Settings
    }

    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = color,
        modifier = modifier.size(16.dp)
    )
}

private fun getSyncProgressText(progress: com.example.myapplication.presentation.viewmodels.SyncProgress): String {
    return when (progress) {
        com.example.myapplication.presentation.viewmodels.SyncProgress.Idle -> ""
        com.example.myapplication.presentation.viewmodels.SyncProgress.CollectingDeviceInfo -> "Collecting device info..."
        com.example.myapplication.presentation.viewmodels.SyncProgress.ContactingBackend -> "Contacting backend..."
        com.example.myapplication.presentation.viewmodels.SyncProgress.SyncingData -> "Syncing data..."
        com.example.myapplication.presentation.viewmodels.SyncProgress.Completed -> "Completed"
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diffInMinutes = (now - timestamp) / (1000 * 60)

    return when {
        diffInMinutes < 1 -> "Just now"
        diffInMinutes < 60 -> "${diffInMinutes}m ago"
        diffInMinutes < 1440 -> "${diffInMinutes / 60}h ago"
        else -> "${diffInMinutes / 1440}d ago"
    }
}