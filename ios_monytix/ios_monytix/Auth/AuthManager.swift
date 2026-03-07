//
//  AuthManager.swift
//  ios_monytix
//
//  Central auth state and sign-in/sign-up. Same logic as APK: Firebase for email/password + Google;
//  backend GET /auth/session validates token. Use idToken for all API calls.
//

import Combine
import Foundation
import SwiftUI
import UIKit
import FirebaseCore
import FirebaseAuth
import GoogleSignIn

final class AuthManager: ObservableObject {
    static let shared = AuthManager()

    @Published private(set) var isSignedIn = false
    @Published var errorMessage: String?
    @Published var isLoading = false

    private var authStateHandle: AuthStateDidChangeListenerHandle?

    private init() {
        authStateHandle = Auth.auth().addStateDidChangeListener { [weak self] _, user in
            DispatchQueue.main.async {
                self?.isSignedIn = user != nil
            }
        }
    }

    deinit {
        if let handle = authStateHandle {
            Auth.auth().removeStateDidChangeListener(handle)
        }
    }

    var currentUser: FirebaseAuth.User? { Auth.auth().currentUser }

    /// Firebase ID token for backend API (Authorization: Bearer token). Same as APK FirebaseAuthManager.getIdToken().
    func getIdToken(forceRefresh: Bool = false) async -> String? {
        guard let user = Auth.auth().currentUser else { return nil }
        return try? await user.getIDToken(forcingRefresh: forceRefresh)
    }

    /// Sign in with email/password. Same as APK AuthViewModel.signIn().
    func signIn(email: String, password: String) async {
        await MainActor.run { isLoading = true; errorMessage = nil }
        defer { Task { @MainActor in isLoading = false } }
        do {
            _ = try await Auth.auth().signIn(withEmail: email, password: password)
        } catch {
            await MainActor.run { errorMessage = error.localizedDescription }
        }
    }

    /// Sign up with email/password. Same as APK AuthViewModel.signUp().
    func signUp(email: String, password: String) async {
        await MainActor.run { isLoading = true; errorMessage = nil }
        defer { Task { @MainActor in isLoading = false } }
        do {
            _ = try await Auth.auth().createUser(withEmail: email, password: password)
        } catch {
            await MainActor.run { errorMessage = error.localizedDescription }
        }
    }

    /// Sign in with Google. Same as APK AuthViewModel.signInWithGoogle().
    func signInWithGoogle() async {
        await MainActor.run { isLoading = true; errorMessage = nil }
        defer { Task { @MainActor in isLoading = false } }

        // Ensure Google Sign-In is configured (from Firebase / GoogleService-Info.plist)
        if GIDSignIn.sharedInstance.configuration == nil {
            if let clientID = FirebaseApp.app()?.options.clientID {
                GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)
            } else {
                await MainActor.run { errorMessage = "Google Sign-In not configured. Add GoogleService-Info.plist and run the app (not Preview)." }
                return
            }
        }
        guard GIDSignIn.sharedInstance.configuration != nil else {
            await MainActor.run { errorMessage = "Google Sign-In not configured." }
            return
        }

        guard let rootVC = topViewController else {
            await MainActor.run { errorMessage = "Could not present sign-in. Run the app on a device or simulator (not Preview)." }
            return
        }

        do {
            let result = try await GIDSignIn.sharedInstance.signIn(withPresenting: rootVC)
            guard let idToken = result.user.idToken?.tokenString else {
                await MainActor.run { errorMessage = "Google Sign-In: no ID token." }
                return
            }
            let credential = GoogleAuthProvider.credential(
                withIDToken: idToken,
                accessToken: result.user.accessToken.tokenString
            )
            _ = try await Auth.auth().signIn(with: credential)
        } catch {
            await MainActor.run { errorMessage = error.localizedDescription }
        }
    }

    /// Sign in with Apple. Uses Firebase OAuthProvider.appleCredential(withIDToken:rawNonce:fullName:).
    func signInWithApple(idToken: String, rawNonce: String, fullName: PersonNameComponents? = nil) async {
        await MainActor.run { isLoading = true; errorMessage = nil }
        defer { Task { @MainActor in isLoading = false } }
        do {
            let credential = OAuthProvider.appleCredential(
                withIDToken: idToken,
                rawNonce: rawNonce,
                fullName: fullName
            )
            _ = try await Auth.auth().signIn(with: credential)
        } catch {
            await MainActor.run { errorMessage = error.localizedDescription }
        }
    }

    func signOut() {
        errorMessage = nil
        GIDSignIn.sharedInstance.signOut()
        try? Auth.auth().signOut()
    }

    func clearError() {
        errorMessage = nil
    }

    /// Validate current token with backend (same as APK BackendApi.getSession). Use after sign-in to confirm backend accepts token.
    func validateSession() async -> Result<SessionResponse, BackendApiError> {
        guard let token = await getIdToken(forceRefresh: false) else {
            return .failure(.httpStatus(401, "Not signed in"))
        }
        return await BackendApi.getSession(accessToken: token)
    }

    private var topViewController: UIViewController? {
        guard let windowScene = UIApplication.shared.connectedScenes
            .compactMap({ $0 as? UIWindowScene })
            .first(where: { $0.activationState == .foregroundActive }),
              let window = windowScene.windows.first(where: { $0.isKeyWindow }) ?? windowScene.windows.first,
              let root = window.rootViewController
        else { return nil }
        return top(from: root)
    }

    private func top(from vc: UIViewController) -> UIViewController {
        if let presented = vc.presentedViewController { return top(from: presented) }
        if let nav = vc as? UINavigationController, let visible = nav.visibleViewController { return top(from: visible) }
        if let tab = vc as? UITabBarController, let selected = tab.selectedViewController { return top(from: selected) }
        return vc
    }
}
