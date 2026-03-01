//
//  AuthViewModel.swift
//  ios
//
//  Auth via Firebase - Phone OTP, Email, Apple, Google. Matches APK AuthViewModel.
//

import Foundation
import FirebaseAuth

enum AuthStep {
    case login
    case otp
}

@MainActor
@Observable
class AuthViewModel {
    var user: User?
    var accessToken: String?
    var isLoading = false
    var errorMessage: String?
    var authStep: AuthStep = .login
    var phoneForOtp = ""
    var otp = ""
    var resendSecondsLeft = 0
    var verificationId: String?

    private let tokenKey = "auth_token"
    private let userKey = "user_data"

    init() {
        Task {
            await checkSession()
        }
    }

    // Check if user has an active session from Firebase
    func checkSession() async {
        if let firebaseUser = Auth.auth().currentUser {
            await applyFirebaseUser(firebaseUser)
        } else {
            // Try stored token (e.g. from previous session)
            guard let token = UserDefaults.standard.string(forKey: tokenKey),
                  let userData = UserDefaults.standard.data(forKey: userKey),
                  let storedUser = try? JSONDecoder().decode(User.self, from: userData) else {
                return
            }
            self.user = storedUser
            self.accessToken = token
        }
    }

    private func applyFirebaseUser(_ firebaseUser: FirebaseAuth.User) async {
        let token = try? await firebaseUser.getIDToken()
        let appUser = User(
            id: firebaseUser.uid,
            email: firebaseUser.email ?? "",
            createdAt: ISO8601DateFormatter().string(from: firebaseUser.metadata.creationDate ?? Date())
        )
        self.user = appUser
        self.accessToken = token
        saveSession()
    }

    // Sign up with email and password
    func signUp(email: String, password: String) async {
        isLoading = true
        errorMessage = nil
        do {
            let result = try await Auth.auth().createUser(withEmail: email, password: password)
            AnalyticsHelper.logEvent("sign_up_email", parameters: ["method": "email"])
            await applyFirebaseUser(result.user)
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    // Sign in with email and password
    func signIn(email: String, password: String) async {
        isLoading = true
        errorMessage = nil
        do {
            let result = try await Auth.auth().signIn(withEmail: email, password: password)
            AnalyticsHelper.logEvent("sign_in_email", parameters: ["method": "email"])
            await applyFirebaseUser(result.user)
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    // Sign out
    func signOut() async {
        isLoading = true
        errorMessage = nil
        do {
            try Auth.auth().signOut()
            clearSession()
        } catch {
            clearSession()
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    // Reset password
    func resetPassword(email: String) async {
        isLoading = true
        errorMessage = nil
        do {
            try await Auth.auth().sendPasswordReset(withEmail: email)
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    /// Returns the current session's ID token for API calls
    func getAccessToken() async -> String? {
        if let token = accessToken { return token }
        guard let firebaseUser = Auth.auth().currentUser else { return nil }
        let token = try? await firebaseUser.getIDToken()
        accessToken = token
        return token
    }

    // Sign in with Apple (native) - create Firebase credential from Apple ID token
    func signInWithApple(idToken: String, nonce: String) async {
        isLoading = true
        errorMessage = nil
        do {
            let credential = OAuthProvider.appleCredential(withIDToken: idToken, rawNonce: nonce, fullName: nil)
            let result = try await Auth.auth().signIn(with: credential)
            await applyFirebaseUser(result.user)
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    // Sign in with Google - requires Google Sign-In SDK or custom OAuth flow
    func signInWithGoogle() async {
        AnalyticsHelper.logEvent("sign_in_google", parameters: ["method": "google"])
        isLoading = true
        errorMessage = nil
        // Firebase Google Sign-In on iOS typically uses GIDSignIn. For now, show message.
        errorMessage = "Google Sign-In: Add FirebaseAuth with Google Sign-In. See Firebase docs."
        isLoading = false
    }

    // Phone OTP - Send OTP (requires AuthUIDelegate for reCAPTCHA; pass nil for testing)
    func sendPhoneOtp(phone: String) async {
        AnalyticsHelper.logEvent("send_otp")
        isLoading = true
        errorMessage = nil
        let normalized = phone.hasPrefix("+") ? phone : "+91\(phone)"
        await withCheckedContinuation { (continuation: CheckedContinuation<Void, Never>) in
            PhoneAuthProvider.provider().verifyPhoneNumber(normalized, uiDelegate: nil) { [weak self] verificationID, error in
                Task { @MainActor in
                    if let error = error {
                        self?.errorMessage = error.localizedDescription
                    } else if let vid = verificationID {
                        self?.verificationId = vid
                        self?.authStep = .otp
                        self?.phoneForOtp = normalized
                        self?.otp = ""
                        self?.resendSecondsLeft = 60
                        self?.startResendTimer()
                    }
                    self?.isLoading = false
                    continuation.resume()
                }
            }
        }
    }

    func setOtp(_ value: String) {
        otp = value
    }

    func verifyOtp() async {
        AnalyticsHelper.logEvent("verify_otp")
        guard otp.count == 6, let vid = verificationId else { return }
        isLoading = true
        errorMessage = nil
        do {
            let credential = PhoneAuthProvider.provider().credential(withVerificationID: vid, verificationCode: otp)
            let result = try await Auth.auth().signIn(with: credential)
            await applyFirebaseUser(result.user)
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    func resendOtp() {
        AnalyticsHelper.logEvent("resend_otp")
        Task {
            await sendPhoneOtp(phone: phoneForOtp.replacingOccurrences(of: "+91", with: ""))
        }
    }

    private func startResendTimer() {
        Task {
            for seconds in (1..<60).reversed() {
                try? await Task.sleep(nanoseconds: 1_000_000_000)
                resendSecondsLeft = seconds
            }
            resendSecondsLeft = 0
        }
    }

    func clearError() {
        errorMessage = nil
    }

    // MARK: - Private

    private func saveSession() {
        guard let token = accessToken,
              let user = user,
              let userData = try? JSONEncoder().encode(user) else {
            return
        }
        UserDefaults.standard.set(token, forKey: tokenKey)
        UserDefaults.standard.set(userData, forKey: userKey)
    }

    private func clearSession() {
        self.user = nil
        self.accessToken = nil
        UserDefaults.standard.removeObject(forKey: tokenKey)
        UserDefaults.standard.removeObject(forKey: userKey)
    }
}
