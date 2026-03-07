package com.example.monytix.preauth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import com.example.monytix.R
import com.example.monytix.analytics.AnalyticsHelper
import com.example.monytix.auth.AuthScreen
import com.example.monytix.ui.MonytixSpinner
import com.example.monytix.auth.AuthViewModel
import com.example.monytix.security.DeviceVerificationScreen

@Composable
fun PreAuthScreen(
    preAuthViewModel: PreAuthViewModel,
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by preAuthViewModel.uiState.collectAsState()
    var showExitConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    BackHandler(enabled = true) {
        if (showExitConfirm) {
            showExitConfirm = false
            return@BackHandler
        }
        when (uiState.step) {
            PreAuthStep.Auth,
            PreAuthStep.Splash,
            PreAuthStep.UpdateRequired,
            PreAuthStep.DeviceVerification,
            PreAuthStep.Onboarding -> showExitConfirm = true
            else -> preAuthViewModel.goBack()
        }
    }
    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title = { Text(stringResource(R.string.exit_confirm_title)) },
            confirmButton = {
                Button(onClick = { activity?.finish() }) {
                    Text(stringResource(R.string.exit_confirm_exit))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirm = false }) {
                    Text(stringResource(R.string.exit_confirm_cancel))
                }
            }
        )
    }

    LaunchedEffect(uiState.step) {
        val screenName = when (uiState.step) {
            PreAuthStep.Splash -> "splash"
            PreAuthStep.UpdateRequired -> "update_required"
            PreAuthStep.DeviceVerification -> "device_verification"
            PreAuthStep.Onboarding -> "onboarding"
            PreAuthStep.TermsConditions -> "terms_conditions"
            PreAuthStep.PrivacyPolicy -> "privacy_policy"
            PreAuthStep.DataProcessingConsent -> "data_consent"
            PreAuthStep.PermissionExplainer -> "permission_explainer"
            PreAuthStep.Auth -> "auth"
        }
        AnalyticsHelper.logScreenView(screenName)
    }

    when (uiState.step) {
        PreAuthStep.Splash -> SplashContent(isLoading = uiState.isLoading)
        PreAuthStep.UpdateRequired -> UpdateRequiredScreen(
            appStoreUrl = uiState.appStoreUrl.ifEmpty {
                "https://play.google.com/store/apps/details?id=com.example.monytix"
            }
        )
        PreAuthStep.DeviceVerification -> DeviceVerificationScreen(
            onContinue = { preAuthViewModel.completeDeviceVerification() },
            onContactSupport = { /* TODO: Open support URL or email */ }
        )
        PreAuthStep.Onboarding -> OnboardingScreen(
            onComplete = { preAuthViewModel.completeOnboarding() },
            onLogin = { preAuthViewModel.goToAuth() }
        )
        PreAuthStep.TermsConditions -> TermsConditionsScreen(
            onAccept = { preAuthViewModel.acceptTerms() }
        )
        PreAuthStep.PrivacyPolicy -> PrivacyPolicyScreen(
            onContinue = { preAuthViewModel.completePrivacyPolicy() }
        )
        PreAuthStep.DataProcessingConsent -> DataProcessingConsentScreen(
            onAccept = { preAuthViewModel.acceptDataConsent() }
        )
        PreAuthStep.PermissionExplainer -> PermissionExplainerScreen(
            onContinue = { preAuthViewModel.completePermissionExplainer() }
        )
        PreAuthStep.Auth -> AuthScreen(viewModel = authViewModel)
    }
}

@Composable
private fun SplashContent(
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = "MONYTIX Logo",
            modifier = Modifier
                .size(width = 100.dp, height = 30.dp),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.splash_monytix),
            style = MaterialTheme.typography.headlineLarge,
            color = colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.splash_ai_intelligence),
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        androidx.compose.material3.LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = colorScheme.primary,
            trackColor = colorScheme.onBackground.copy(alpha = 0.2f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.splash_securing),
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onBackground.copy(alpha = 0.6f)
        )
        if (isLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            MonytixSpinner(size = 32.dp, stroke = 6.dp)
        }
    }
}
