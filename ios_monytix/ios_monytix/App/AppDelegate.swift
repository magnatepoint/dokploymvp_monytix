//
//  AppDelegate.swift
//  ios_monytix
//
//  Configures Google Sign-In from Firebase (same as APK). Forwards OAuth URL to GIDSignIn.
//

import UIKit
import FirebaseCore
import GoogleSignIn

@objc
class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        // Defer GID config to first use (AuthManager) so launch stays fast.
        return true
    }

    func application(
        _ app: UIApplication,
        open url: URL,
        options: [UIApplication.OpenURLOptionsKey: Any] = [:]
    ) -> Bool {
        return GIDSignIn.sharedInstance.handle(url)
    }
}
