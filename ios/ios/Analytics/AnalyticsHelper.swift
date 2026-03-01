//
//  AnalyticsHelper.swift
//  ios
//
//  Central analytics helper for Firebase Analytics.
//  Tracks screen views and user events across the app.
//

import Foundation
import FirebaseAnalytics

enum AnalyticsHelper {
    
    static func logScreenView(screenName: String, screenClass: String? = nil) {
        var params: [String: Any] = [
            "screen_name": screenName
        ]
        if let screenClass = screenClass {
            params["screen_class"] = screenClass
        }
        Analytics.logEvent("screen_view", parameters: params)
    }
    
    static func logEvent(_ eventName: String, parameters: [String: Any]? = nil) {
        Analytics.logEvent(eventName, parameters: parameters)
    }
}
