package com.example.monytix.system

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.monytix.analytics.AnalyticsHelper
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

    LaunchedEffect(state) {
        when (state) {
            AppBlockUiState.Maintenance -> AnalyticsHelper.logScreenView("maintenance")
            AppBlockUiState.InternetError -> AnalyticsHelper.logScreenView("internet_error")
            AppBlockUiState.ServerDown -> AnalyticsHelper.logScreenView("server_down")
            AppBlockUiState.Ready -> { /* MainContent screens track themselves */ }
        }
    }

    when (state) {
        AppBlockUiState.Maintenance -> MaintenanceModeScreen()
        AppBlockUiState.InternetError -> InternetErrorScreen(onRetry = {
            AnalyticsHelper.logEvent("retry_clicked")
            viewModel.retry()
        })
        AppBlockUiState.ServerDown -> ServerDownScreen(
            onRetry = {
                AnalyticsHelper.logEvent("retry_clicked")
                viewModel.retry()
            },
            onContinueAnyway = {
                AnalyticsHelper.logEvent("continue_anyway_clicked")
                viewModel.continueAnyway()
            }
        )
        AppBlockUiState.Ready -> content()
    }
}
