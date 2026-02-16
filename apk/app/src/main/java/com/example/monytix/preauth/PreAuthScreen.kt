package com.example.monytix.preauth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.monytix.R
import com.example.monytix.auth.AuthScreen
import com.example.monytix.auth.AuthViewModel
import com.example.monytix.security.DeviceVerificationScreen

@Composable
fun PreAuthScreen(
    preAuthViewModel: PreAuthViewModel,
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by preAuthViewModel.uiState.collectAsState()

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
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.splash_logo),
            contentDescription = "MONYTIX Logo",
            modifier = Modifier.size(160.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.splash_tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        }
    }
}
