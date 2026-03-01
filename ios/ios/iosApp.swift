//
//  iosApp.swift
//  ios
//
//  Created by santosh on 24/02/26.
//

import SwiftUI
import FirebaseCore

@main
struct iosApp: App {
    init() {
        FirebaseApp.configure()
    }

    @State private var authViewModel = AuthViewModel()
    
    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(authViewModel)
        }
    }
}
