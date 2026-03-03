//
//  iosApp.swift
//  ios
//
//  Created by santosh on 24/02/26.
//

import SwiftUI
import FirebaseCore
import GoogleSignIn

@main
struct iosApp: App {
    init() {
        // Only configure Firebase if GoogleService-Info.plist is in the app bundle.
        // Add the plist from Firebase Console (Project Settings → Your apps → Add iOS app) and add the file to the Xcode project.
        if Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil {
            FirebaseApp.configure()
        }
    }

    @State private var authViewModel = AuthViewModel()
    
    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(authViewModel)
                .onOpenURL { url in
                    GIDSignIn.sharedInstance.handle(url)
                }
        }
    }
}
