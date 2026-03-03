package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.di.ServiceLocator
import com.example.myapplication.presentation.screens.DeviceInfoScreen
import com.example.myapplication.presentation.theme.MyApplicationTheme
import com.example.myapplication.presentation.viewmodels.MainViewModel
import com.example.myapplication.presentation.viewmodels.UiEffect

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                EsperDeviceManagerApp()
            }
        }
    }
}

@Composable
fun EsperDeviceManagerApp() {
    val context = androidx.compose.ui.platform.LocalContext.current

    // Create ViewModel with dependency injection
    val viewModel: MainViewModel = viewModel {
        val deviceRepository = ServiceLocator.provideDeviceRepository(context)
        MainViewModel(deviceRepository)
    }

    // Collect UI state
    val uiState by viewModel.uiState.collectAsState()

    // Handle UI effects (would typically use LaunchedEffect for snackbars, navigation, etc.)
    LaunchedEffect(Unit) {
        viewModel.uiEffects.collect { effect ->
            when (effect) {
                is UiEffect.ShowSnackbar -> {
                    // Handle snackbar display
                    // In a real app, this would show a snackbar
                    println("Snackbar: ${effect.message}")
                }
                is UiEffect.ShowError -> {
                    // Handle error effects
                    println("Error: ${effect.error.message}")
                }
                is UiEffect.NavigateToSettings -> {
                    // Handle navigation to settings
                    println("Navigate to settings")
                }
            }
        }
    }

    DeviceInfoScreen(
        uiState = uiState,
        onEvent = viewModel::handleEvent,
        modifier = Modifier.fillMaxSize()
    )
}