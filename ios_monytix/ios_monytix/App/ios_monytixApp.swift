//
//  ios_monytixApp.swift
//  ios_monytix
//
//  Created by santosh on 06/03/26.
//

import SwiftUI
import FirebaseCore
import GoogleSignIn

@main
struct ios_monytixApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    init() {
        FirebaseApp.configure()
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                .preferredColorScheme(.dark)
                .onOpenURL { url in
                    GIDSignIn.sharedInstance.handle(url)
                }
        }
    }
}

/// Same logic as APK: show Auth when not signed in, main content (bottom tabs: Home + SpendSense) when signed in.
private struct RootView: View {
    @ObservedObject private var auth = AuthManager.shared

    var body: some View {
        if auth.isSignedIn {
            MainTabView()
        } else {
            AuthScreen()
        }
    }
}
