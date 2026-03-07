package com.example.monytix.quicktour

import android.content.Context
import android.content.SharedPreferences

private const val PREFS_NAME = "quick_tour_prefs"
private const val KEY_TOUR_COMPLETED = "quick_tour_completed"

object QuickTourPreferences {

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun setCompleted(context: Context) {
        getPrefs(context).edit().putBoolean(KEY_TOUR_COMPLETED, true).apply()
    }

    fun hasCompleted(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_TOUR_COMPLETED, false)
}
