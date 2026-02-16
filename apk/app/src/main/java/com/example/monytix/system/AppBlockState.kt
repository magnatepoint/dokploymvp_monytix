package com.example.monytix.system

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
/**
 * Wraps post-auth content and shows block screens when:
 * - Maintenance mode (from config)
 * - No internet (optional, requires connectivity check)
 * - Server down (when health check fails)
 */
@Composable
fun AppBlockState(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val viewModel: AppBlockViewModel = viewModel(
        factory = AppBlockViewModelFactory(context.applicationContext)
    )
    val state by viewModel.uiState.collectAsState()

    when (state) {
        AppBlockUiState.Maintenance -> MaintenanceModeScreen()
        AppBlockUiState.InternetError -> InternetErrorScreen(onRetry = { viewModel.retry() })
        AppBlockUiState.ServerDown -> ServerDownScreen(
            onRetry = { viewModel.retry() },
            onContinueAnyway = { viewModel.continueAnyway() }
        )
        AppBlockUiState.Ready -> content()
    }
}
