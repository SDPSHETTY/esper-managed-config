package com.example.myapplication.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.models.BackendStatus
import com.example.myapplication.presentation.screens.components.*
import com.example.myapplication.presentation.viewmodels.*

/**
 * Main screen displaying device information and sync status.
 * Implements Material3 design with pull-to-refresh and error handling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceInfoScreen(
    uiState: UiState,
    onEvent: (UiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle UI effects
    LaunchedEffect(Unit) {
        // This would collect UI effects in a real implementation
        // For now, we'll handle them in the parent component
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            DeviceInfoTopBar(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { onEvent(UiEvent.Refresh) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                // Loading state
                uiState.isLoading -> {
                    LoadingView()
                }

                // Error state (blocking)
                uiState.hasBlockingError -> {
                    ErrorView(
                        error = uiState.error!!,
                        onRetry = { onEvent(UiEvent.RetryLastOperation) },
                        onDismiss = { onEvent(UiEvent.DismissError) }
                    )
                }

                // Content state
                uiState.hasData -> {
                    DeviceInfoContent(
                        uiState = uiState,
                        listState = listState,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Empty state
                else -> {
                    EmptyView()
                }
            }

            // Non-blocking error overlay
            if (uiState.error != null && !uiState.hasBlockingError) {
                ErrorBanner(
                    error = uiState.error,
                    onDismiss = { onEvent(UiEvent.DismissError) },
                    onRetry = if (uiState.error.canRetry) {
                        { onEvent(UiEvent.RetryLastOperation) }
                    } else null,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceInfoTopBar(
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    TopAppBar(
        title = {
            Text("Esper Device Manager")
        },
        actions = {
            IconButton(
                onClick = onRefresh,
                enabled = !isRefreshing
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh"
                    )
                }
            }
        }
    )
}

@Composable
private fun DeviceInfoContent(
    uiState: UiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Sync Status Card
        item {
            SyncStatusCard(
                backendStatus = uiState.backendStatus,
                lastSyncTime = uiState.lastSyncTime,
                syncProgress = uiState.syncProgress
            )
        }

        // Esper Managed Configuration Card
        uiState.deviceInfo?.let { deviceInfo ->
            item {
                EsperConfigCard(deviceInfo = deviceInfo)
            }
        }

        // Device Metadata Card
        uiState.deviceInfo?.let { deviceInfo ->
            item {
                DeviceMetadataCard(deviceInfo = deviceInfo)
            }
        }

        // Diagnostic Information Card
        uiState.diagnosticInfo?.let { diagnosticInfo ->
            item {
                DiagnosticCard(diagnosticInfo = diagnosticInfo)
            }
        }

        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun LoadingView(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = "Loading device information...",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ErrorView(
    error: UiError,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Error",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )

                Text(
                    text = error.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (error.canRetry) {
                        Button(onClick = onRetry) {
                            Text("Retry")
                        }
                    }

                    if (!error.isBlocking) {
                        TextButton(onClick = onDismiss) {
                            Text("Dismiss")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorBanner(
    error: UiError,
    onDismiss: () -> Unit,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = error.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (onRetry != null) {
                    TextButton(onClick = onRetry) {
                        Text(
                            text = "Retry",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                TextButton(onClick = onDismiss) {
                    Text(
                        text = "Dismiss",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyView(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "No device information available",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Pull down to refresh or check your device configuration",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}