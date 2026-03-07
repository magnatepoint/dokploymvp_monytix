//
//  BackendConfig.swift
//  ios_monytix
//
//  Backend base URL (same idea as APK BuildConfig.BACKEND_URL). Override via Info.plist or use default.
//

import Foundation

enum BackendConfig {
    /// Primary backend base URL (no trailing slash). Fallback matches APK backupBaseUrl.
    static var baseURL: String {
        (Bundle.main.object(forInfoDictionaryKey: "BACKEND_URL") as? String)?
            .trimmingCharacters(in: CharacterSet(charactersIn: "/")) ?? "https://backend.monytix.ai"
    }

    static var backupBaseURL: String { "https://backend.monytix.ai" }
}
