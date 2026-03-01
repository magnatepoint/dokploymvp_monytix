package com.example.monytix.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Central analytics helper for Firebase Analytics.
 * Tracks screen views and user events across the app.
 */
object AnalyticsHelper {

    private var analytics: FirebaseAnalytics? = null

    fun init(context: Context) {
        analytics = FirebaseAnalytics.getInstance(context)
    }

    private fun getAnalytics(): FirebaseAnalytics =
        analytics ?: throw IllegalStateException("AnalyticsHelper.init() must be called in Application.onCreate()")

    fun logScreenView(screenName: String, screenClass: String? = null) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            screenClass?.let { putString(FirebaseAnalytics.Param.SCREEN_CLASS, it) }
        }
        getAnalytics().logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    fun logEvent(eventName: String, params: Map<String, Any>? = null) {
        val bundle = params?.let { mapToBundle(it) }
        getAnalytics().logEvent(eventName, bundle)
    }

    fun logSignIn(method: String) {
        val bundle = Bundle().apply { putString(FirebaseAnalytics.Param.METHOD, method) }
        getAnalytics().logEvent(FirebaseAnalytics.Event.LOGIN, bundle)
    }

    fun logSignUp(method: String) {
        val bundle = Bundle().apply { putString(FirebaseAnalytics.Param.METHOD, method) }
        getAnalytics().logEvent(FirebaseAnalytics.Event.SIGN_UP, bundle)
    }

    fun logSelectContent(contentType: String, itemId: String? = null, itemName: String? = null) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.CONTENT_TYPE, contentType)
            itemId?.let { putString(FirebaseAnalytics.Param.ITEM_ID, it) }
            itemName?.let { putString(FirebaseAnalytics.Param.ITEM_NAME, it) }
        }
        getAnalytics().logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
    }

    fun logButtonTap(buttonId: String, screen: String? = null) {
        logEvent("button_tap", buildMap {
            put("button_id", buttonId)
            screen?.let { put("screen", it) }
        })
    }

    private fun mapToBundle(map: Map<String, Any>): Bundle {
        val bundle = Bundle()
        map.forEach { (key, value) ->
            when (value) {
                is String -> bundle.putString(key, value)
                is Int -> bundle.putInt(key, value)
                is Long -> bundle.putLong(key, value)
                is Double -> bundle.putDouble(key, value)
                is Boolean -> bundle.putBoolean(key, value)
                else -> bundle.putString(key, value.toString())
            }
        }
        return bundle
    }
}
