package com.example.monytix.preauth

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.monytix.BuildConfig
import com.example.monytix.analytics.AnalyticsHelper
import com.example.monytix.data.BackendApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class PreAuthStep {
    data object Splash : PreAuthStep()
    data object UpdateRequired : PreAuthStep()
    data object DeviceVerification : PreAuthStep()
    data object Onboarding : PreAuthStep()
    data object TermsConditions : PreAuthStep()
    data object PrivacyPolicy : PreAuthStep()
    data object DataProcessingConsent : PreAuthStep()
    data object PermissionExplainer : PreAuthStep()
    data object Auth : PreAuthStep()
}

data class PreAuthUiState(
    val step: PreAuthStep = PreAuthStep.Splash,
    val isLoading: Boolean = true,
    val forceUpdate: Boolean = false,
    val appStoreUrl: String = "",
    val securityWarning: Boolean = false,
)

class PreAuthViewModel(
    private val application: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(PreAuthUiState())
    val uiState: StateFlow<PreAuthUiState> = _uiState.asStateFlow()

    init {
        runSplashChecks()
    }

    private fun runSplashChecks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            delay(500) // Minimum splash display time
            val configResult = withTimeoutOrNull(5_000L) { BackendApi.getConfig() } ?: run {
                // Timeout or failure: proceed with defaults (no force update)
                Result.success(
                    com.example.monytix.data.ConfigResponse(
                        min_version_code = 1,
                        app_store_url = "https://play.google.com/store/apps/details?id=com.example.monytix",
                        feature_flags = emptyMap(),
                        maintenance_mode = false
                    )
                )
            }
            val updateRequired = configResult.getOrNull()?.let { config ->
                BuildConfig.VERSION_CODE < config.min_version_code
            } ?: false
            val appStoreUrl = configResult.getOrNull()?.app_store_url ?: ""
            val securityOk = !isDeviceCompromised()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    forceUpdate = updateRequired,
                    appStoreUrl = appStoreUrl,
                    securityWarning = !securityOk
                )
            }
            advanceToNextStep(updateRequired, securityOk)
        }
    }

    private suspend fun advanceToNextStep(updateRequired: Boolean, securityOk: Boolean) {
        if (updateRequired) {
            _uiState.update { it.copy(step = PreAuthStep.UpdateRequired) }
            return
        }
        if (!securityOk) {
            _uiState.update { it.copy(step = PreAuthStep.DeviceVerification) }
            return
        }
        val onboardingDone = PreAuthPreferences.onboardingComplete(application).first()
        if (!onboardingDone) {
            _uiState.update { it.copy(step = PreAuthStep.Onboarding) }
            return
        }
        val termsAccepted = PreAuthPreferences.termsAccepted(application).first()
        if (!termsAccepted) {
            _uiState.update { it.copy(step = PreAuthStep.TermsConditions) }
            return
        }
        val privacyViewed = PreAuthPreferences.privacyViewed(application).first()
        if (!privacyViewed) {
            _uiState.update { it.copy(step = PreAuthStep.PrivacyPolicy) }
            return
        }
        val dataConsent = PreAuthPreferences.dataConsent(application).first()
        if (!dataConsent) {
            _uiState.update { it.copy(step = PreAuthStep.DataProcessingConsent) }
            return
        }
        val permissionsExplained = PreAuthPreferences.permissionsExplained(application).first()
        if (!permissionsExplained) {
            _uiState.update { it.copy(step = PreAuthStep.PermissionExplainer) }
            return
        }
        _uiState.update { it.copy(step = PreAuthStep.Auth) }
    }

    private fun isDeviceCompromised(): Boolean {
        return try {
            val buildTags = android.os.Build.TAGS
            buildTags != null && buildTags.contains("test-keys")
        } catch (_: Exception) {
            false
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            AnalyticsHelper.logEvent("onboarding_complete")
            PreAuthPreferences.setOnboardingComplete(application, true)
            _uiState.update { it.copy(step = PreAuthStep.TermsConditions) }
        }
    }

    fun acceptTerms() {
        viewModelScope.launch {
            AnalyticsHelper.logEvent("terms_accepted")
            PreAuthPreferences.setTermsAccepted(application, true)
            _uiState.update { it.copy(step = PreAuthStep.PrivacyPolicy) }
        }
    }

    fun completePrivacyPolicy() {
        viewModelScope.launch {
            AnalyticsHelper.logEvent("privacy_continue")
            PreAuthPreferences.setPrivacyViewed(application, true)
            _uiState.update { it.copy(step = PreAuthStep.DataProcessingConsent) }
        }
    }

    fun acceptDataConsent() {
        viewModelScope.launch {
            AnalyticsHelper.logEvent("data_consent_accepted")
            PreAuthPreferences.setDataConsent(application, true)
            _uiState.update { it.copy(step = PreAuthStep.PermissionExplainer) }
        }
    }

    fun completePermissionExplainer() {
        viewModelScope.launch {
            AnalyticsHelper.logEvent("permission_continue")
            PreAuthPreferences.setPermissionsExplained(application, true)
            _uiState.update { it.copy(step = PreAuthStep.Auth) }
        }
    }

    fun skipOnboarding() = completeOnboarding()

    fun completeDeviceVerification() {
        viewModelScope.launch {
            AnalyticsHelper.logEvent("device_verification_continue")
            advanceToNextStep(updateRequired = false, securityOk = true)
        }
    }

    fun goToAuth() {
        viewModelScope.launch {
            AnalyticsHelper.logEvent("login_clicked")
            PreAuthPreferences.setOnboardingComplete(application, true)
            PreAuthPreferences.setTermsAccepted(application, true)
            PreAuthPreferences.setPrivacyViewed(application, true)
            PreAuthPreferences.setDataConsent(application, true)
            PreAuthPreferences.setPermissionsExplained(application, true)
            _uiState.update { it.copy(step = PreAuthStep.Auth) }
        }
    }

    /** Go to the previous step in the pre-auth flow. No-op for Splash, UpdateRequired, DeviceVerification, Onboarding. */
    fun goBack() {
        val previous = when (_uiState.value.step) {
            PreAuthStep.Auth -> PreAuthStep.PermissionExplainer
            PreAuthStep.PermissionExplainer -> PreAuthStep.DataProcessingConsent
            PreAuthStep.DataProcessingConsent -> PreAuthStep.PrivacyPolicy
            PreAuthStep.PrivacyPolicy -> PreAuthStep.TermsConditions
            PreAuthStep.TermsConditions -> PreAuthStep.Onboarding
            else -> return
        }
        _uiState.update { it.copy(step = previous) }
    }
}
