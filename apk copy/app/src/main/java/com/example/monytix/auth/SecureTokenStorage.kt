package com.example.monytix.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Stores auth token and (optionally) credentials for biometric re-login.
 * Uses EncryptedSharedPreferences so tokens are not readable on rooted devices.
 */
object SecureTokenStorage {

    private const val PREFS_NAME = "secret_shared_prefs"
    private const val KEY_AUTH_TOKEN = "auth_token"
    private const val KEY_EMAIL = "auth_email"
    private const val KEY_PASSWORD = "auth_password"

    private fun getPrefs(context: Context) = runCatching {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }.getOrNull()

    fun saveAfterLogin(context: Context, token: String, email: String, password: String) {
        getPrefs(context)?.edit()?.apply {
            putString(KEY_AUTH_TOKEN, token)
            putString(KEY_EMAIL, email)
            putString(KEY_PASSWORD, password)
            apply()
        }
    }

    fun getStoredToken(context: Context): String? =
        getPrefs(context)?.getString(KEY_AUTH_TOKEN, null)

    fun getStoredCredentials(context: Context): Pair<String, String>? {
        val prefs = getPrefs(context) ?: return null
        val email = prefs.getString(KEY_EMAIL, null) ?: return null
        val password = prefs.getString(KEY_PASSWORD, null) ?: return null
        return if (email.isNotBlank() && password.isNotBlank()) email to password else null
    }

    fun hasStoredCredentials(context: Context): Boolean =
        getStoredCredentials(context) != null

    fun clear(context: Context) {
        getPrefs(context)?.edit()?.clear()?.apply()
    }
}
