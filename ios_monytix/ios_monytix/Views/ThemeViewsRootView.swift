//
//  RootView.swift
//  ios_monytix
//
//  Created by santosh on 06/03/26.
//

import SwiftUI

/// Root view that handles authentication state
struct RootView: View {
    @StateObject private var authManager = AuthManager()
    
    var body: some View {
        Group {
            if authManager.isAuthenticated {
                ContentView()
                    .transition(.asymmetric(
                        insertion: .move(edge: .trailing).combined(with: .opacity),
                        removal: .move(edge: .leading).combined(with: .opacity)
                    ))
            } else {
                AuthView()
                    .transition(.asymmetric(
                        insertion: .move(edge: .leading).combined(with: .opacity),
                        removal: .move(edge: .trailing).combined(with: .opacity)
                    ))
            }
        }
        .animation(MonytixMotion.spring, value: authManager.isAuthenticated)
        .environmentObject(authManager)
    }
}

// MARK: - Auth Manager

class AuthManager: ObservableObject {
    @Published var isAuthenticated = false
    @Published var currentUser: User?
    
    init() {
        // Check if user is already logged in
        checkAuthStatus()
    }
    
    func checkAuthStatus() {
        // Check UserDefaults, Keychain, or your auth provider
        // For demo purposes, we'll start as not authenticated
        isAuthenticated = false
    }
    
    func signIn(email: String, password: String) async throws {
        // Implement your sign-in logic here
        // For demo:
        try await Task.sleep(nanoseconds: 1_000_000_000)
        
        await MainActor.run {
            currentUser = User(
                id: UUID().uuidString,
                email: email,
                fullName: "Demo User"
            )
            isAuthenticated = true
        }
    }
    
    func signUp(email: String, password: String, fullName: String) async throws {
        // Implement your sign-up logic here
        // For demo:
        try await Task.sleep(nanoseconds: 1_000_000_000)
        
        await MainActor.run {
            currentUser = User(
                id: UUID().uuidString,
                email: email,
                fullName: fullName
            )
            isAuthenticated = true
        }
    }
    
    func signInWithApple(userID: String, email: String?, fullName: String?) async throws {
        // Implement Sign in with Apple logic
        // For demo:
        try await Task.sleep(nanoseconds: 500_000_000)
        
        await MainActor.run {
            currentUser = User(
                id: userID,
                email: email ?? "user@privaterelay.appleid.com",
                fullName: fullName ?? "Apple User"
            )
            isAuthenticated = true
        }
    }
    
    func signOut() {
        currentUser = nil
        isAuthenticated = false
    }
}

// MARK: - User Model

struct User: Identifiable, Codable {
    let id: String
    let email: String
    let fullName: String
}

// MARK: - Preview

#Preview {
    RootView()
}
