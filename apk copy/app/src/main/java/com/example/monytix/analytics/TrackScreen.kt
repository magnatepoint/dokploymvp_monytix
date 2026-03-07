package com.example.monytix.analytics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/**
 * Wraps content and logs a screen view when the composable is first composed.
 */
@Composable
fun TrackScreen(
    screenName: String,
    screenClass: String? = null,
    content: @Composable () -> Unit
) {
    LaunchedEffect(screenName) {
        AnalyticsHelper.logScreenView(screenName, screenClass)
    }
    content()
}
