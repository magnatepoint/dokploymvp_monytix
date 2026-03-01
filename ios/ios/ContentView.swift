//
//  ContentView.swift
//  ios
//
//  Root view - PreAuth flow when not logged in, MainContent when authenticated.
//  Matches APK flow: PreAuthScreen -> AuthScreen -> PostAuthGate -> MainContent
//

import SwiftUI

struct ContentView: View {
    @Environment(AuthViewModel.self) private var authViewModel

    var body: some View {
        Group {
            if authViewModel.user != nil {
                MainContentView()
            } else {
                PreAuthView()
            }
        }
    }
}

#Preview {
    ContentView()
        .environment(AuthViewModel())
}
