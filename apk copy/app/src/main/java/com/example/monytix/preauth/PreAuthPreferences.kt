package com.example.monytix.preauth

import android.app.Application
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

private const val PREFS_NAME = "preauth_prefs"
private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
private const val KEY_TERMS_ACCEPTED = "terms_accepted"
private const val KEY_PRIVACY_VIEWED = "privacy_viewed"
private const val KEY_DATA_CONSENT = "data_consent"
private const val KEY_PERMISSIONS_EXPLAINED = "permissions_explained"

object PreAuthPreferences {

    private fun getPrefs(application: Application) =
        application.getSharedPreferences(PREFS_NAME, Application.MODE_PRIVATE)

    fun setOnboardingComplete(application: Application, value: Boolean) {
        getPrefs(application).edit().putBoolean(KEY_ONBOARDING_COMPLETE, value).apply()
    }

    fun onboardingComplete(application: Application): Flow<Boolean> =
        flowOf(getPrefs(application).getBoolean(KEY_ONBOARDING_COMPLETE, false))

    fun setTermsAccepted(application: Application, value: Boolean) {
        getPrefs(application).edit().putBoolean(KEY_TERMS_ACCEPTED, value).apply()
    }

    fun termsAccepted(application: Application): Flow<Boolean> =
        flowOf(getPrefs(application).getBoolean(KEY_TERMS_ACCEPTED, false))

    fun setPrivacyViewed(application: Application, value: Boolean) {
        getPrefs(application).edit().putBoolean(KEY_PRIVACY_VIEWED, value).apply()
    }

    fun privacyViewed(application: Application): Flow<Boolean> =
        flowOf(getPrefs(application).getBoolean(KEY_PRIVACY_VIEWED, false))

    fun setDataConsent(application: Application, value: Boolean) {
        getPrefs(application).edit().putBoolean(KEY_DATA_CONSENT, value).apply()
    }

    fun dataConsent(application: Application): Flow<Boolean> =
        flowOf(getPrefs(application).getBoolean(KEY_DATA_CONSENT, false))

    fun setPermissionsExplained(application: Application, value: Boolean) {
        getPrefs(application).edit().putBoolean(KEY_PERMISSIONS_EXPLAINED, value).apply()
    }

    fun permissionsExplained(application: Application): Flow<Boolean> =
        flowOf(getPrefs(application).getBoolean(KEY_PERMISSIONS_EXPLAINED, false))
}
