package com.example.monytix.auth

import android.content.Context
import android.content.SharedPreferences

private const val PREFS_NAME = "biometric_prefs"
private const val KEY_BIOMETRIC_DECIDED = "biometric_decided"  // true = enabled or skipped

object BiometricPreferences {

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun setDecided(context: Context) {
        getPrefs(context).edit().putBoolean(KEY_BIOMETRIC_DECIDED, true).apply()
    }

    fun hasDecided(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_BIOMETRIC_DECIDED, false)
}
