//
//  AuthScreen.swift
//  ios_monytix
//
//  Login / registration screen. Same flow as APK AuthScreen: email, password, sign in/sign up toggle, Apple, Google.
//

import AuthenticationServices
import CryptoKit
import SwiftUI

struct AuthScreen: View {
    @ObservedObject private var auth = AuthManager.shared
    @State private var isLogin = true
    @State private var email = ""
    @State private var password = ""
    @State private var confirmPassword = ""
    @State private var currentNonce: String?
    @FocusState private var focusedField: Field?

    private enum Field: Hashable {
        case email, password, confirmPassword
    }

    private var primaryTitle: String { isLogin ? "Sign in" : "Create account" }
    private var canSubmit: Bool {
        let validEmail = !email.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        let validPassword = password.count >= 6
        let match = !isLogin ? (password == confirmPassword) : true
        return validEmail && validPassword && match
    }

    var body: some View {
        ZStack {
            MonytixTheme.bg.ignoresSafeArea()

            ScrollView {
                VStack(spacing: MonytixSpace.lg) {
                    header
                        .enter(delay: 0)
                    formCard
                        .enter(delay: 0.08)
                    primaryButton
                        .enter(delay: 0.12)
                    toggleModeButton
                    orDivider
                    appleButton
                        .enter(delay: 0.14)
                    googleButton
                        .enter(delay: 0.16)
                    TrustBannerEncryption()
                        .enter(delay: 0.18)
                }
                .padding(.horizontal, MonytixSpace.lg)
                .padding(.top, 40)
                .padding(.bottom, MonytixSpace.xl)
            }
            .scrollDismissesKeyboard(.interactively)
        }
        .onTapGesture { focusedField = nil }
    }

    private var header: some View {
        VStack(spacing: MonytixSpace.sm) {
            Image(systemName: "chart.line.uptrend.xyaxis")
                .font(.system(size: 48))
                .foregroundStyle(MonytixTheme.cyan1)
            Text("Monytix")
                .font(.system(size: 28, weight: .bold))
                .foregroundStyle(MonytixTheme.text1)
            Text("Your AI-powered financial insights")
                .font(.system(size: 15, weight: .medium))
                .foregroundStyle(MonytixTheme.text2)
        }
        .padding(.bottom, MonytixSpace.sm)
    }

    private var formCard: some View {
        VStack(alignment: .leading, spacing: MonytixSpace.md) {
            emailField
            passwordField
            if !isLogin { confirmPasswordField }

            if let message = auth.errorMessage {
                Text(message)
                    .font(.caption)
                    .foregroundStyle(MonytixTheme.danger)
            }
        }
        .padding(MonytixSpace.lg)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(MonytixTheme.surface)
        .clipShape(RoundedRectangle(cornerRadius: MonytixShape.cardRadius))
        .overlay(
            RoundedRectangle(cornerRadius: MonytixShape.cardRadius)
                .stroke(MonytixTheme.stroke.opacity(0.6), lineWidth: 1)
        )
    }

    private var emailField: some View {
        VStack(alignment: .leading, spacing: MonytixSpace.xs) {
            Text("Email")
                .font(.subheadline.weight(.medium))
                .foregroundStyle(MonytixTheme.text2)
            TextField("your@email.com", text: $email)
                .textContentType(.emailAddress)
                .keyboardType(.emailAddress)
                .autocapitalization(.none)
                .autocorrectionDisabled()
                .focused($focusedField, equals: .email)
                .foregroundStyle(MonytixTheme.text1)
                .padding(MonytixSpace.sm + 4)
                .background(MonytixTheme.surface2)
                .clipShape(RoundedRectangle(cornerRadius: MonytixShape.inputRadius))
        }
    }

    private var passwordField: some View {
        VStack(alignment: .leading, spacing: MonytixSpace.xs) {
            Text("Password")
                .font(.subheadline.weight(.medium))
                .foregroundStyle(MonytixTheme.text2)
            SecureField(isLogin ? "Password" : "Min 6 characters", text: $password)
                .textContentType(isLogin ? .password : .newPassword)
                .focused($focusedField, equals: .password)
                .foregroundStyle(MonytixTheme.text1)
                .padding(MonytixSpace.sm + 4)
                .background(MonytixTheme.surface2)
                .clipShape(RoundedRectangle(cornerRadius: MonytixShape.inputRadius))
        }
    }

    private var confirmPasswordField: some View {
        VStack(alignment: .leading, spacing: MonytixSpace.xs) {
            Text("Confirm password")
                .font(.subheadline.weight(.medium))
                .foregroundStyle(MonytixTheme.text2)
            SecureField("Repeat password", text: $confirmPassword)
                .textContentType(.newPassword)
                .focused($focusedField, equals: .confirmPassword)
                .foregroundStyle(MonytixTheme.text1)
                .padding(MonytixSpace.sm + 4)
                .background(MonytixTheme.surface2)
                .clipShape(RoundedRectangle(cornerRadius: MonytixShape.inputRadius))
            if !confirmPassword.isEmpty && password != confirmPassword {
                Text("Passwords don't match")
                    .font(.caption)
                    .foregroundStyle(MonytixTheme.danger)
            }
        }
    }

    private var primaryButton: some View {
        Button {
            submit()
        } label: {
            HStack {
                if auth.isLoading {
                    MonytixRingLoader(size: 28, lineWidth: 3)
                } else {
                    Text(primaryTitle)
                        .fontWeight(.semibold)
                }
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, MonytixSpace.md)
            .background(canSubmit ? MonytixTheme.cyan1 : MonytixTheme.cyan1.opacity(0.5))
            .foregroundStyle(MonytixTheme.onAccent)
            .clipShape(RoundedRectangle(cornerRadius: MonytixShape.buttonRadius))
        }
        .disabled(!canSubmit || auth.isLoading)
        .animation(MonytixMotion.easeOut, value: canSubmit)
    }

    private var toggleModeButton: some View {
        Button {
            auth.clearError()
            isLogin.toggle()
        } label: {
            Text(isLogin ? "New to Monytix? Create account" : "Already have an account? Sign in")
                .font(.system(size: 15, weight: .medium))
                .foregroundStyle(MonytixTheme.text2)
        }
    }

    private var orDivider: some View {
        HStack(spacing: MonytixSpace.sm) {
            Rectangle()
                .fill(MonytixTheme.stroke.opacity(0.6))
                .frame(height: 1)
            Text("OR")
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(MonytixTheme.text2)
            Rectangle()
                .fill(MonytixTheme.stroke.opacity(0.6))
                .frame(height: 1)
        }
    }

    private var appleButton: some View {
        SignInWithAppleButton(
            onRequest: { request in
                let nonce = randomNonceString()
                currentNonce = nonce
                request.requestedScopes = [.fullName, .email]
                request.nonce = sha256(nonce)
            },
            onCompletion: { result in
                handleSignInWithApple(result)
            }
        )
        .signInWithAppleButtonStyle(.white)
        .frame(maxWidth: 375)
        .frame(height: 50)
        .clipShape(RoundedRectangle(cornerRadius: MonytixShape.buttonRadius))
        .overlay(
            RoundedRectangle(cornerRadius: MonytixShape.buttonRadius)
                .stroke(MonytixTheme.stroke.opacity(0.6), lineWidth: 1)
        )
        .disabled(auth.isLoading)
    }

    private var googleButton: some View {
        Button {
            Task { await auth.signInWithGoogle() }
        } label: {
            HStack(spacing: MonytixSpace.sm) {
                Image(systemName: "g.circle.fill")
                    .font(.title2)
                Text("Continue with Google")
                    .fontWeight(.medium)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, MonytixSpace.md)
            .background(MonytixTheme.surface)
            .foregroundStyle(MonytixTheme.text1)
            .clipShape(RoundedRectangle(cornerRadius: MonytixShape.buttonRadius))
            .overlay(
                RoundedRectangle(cornerRadius: MonytixShape.buttonRadius)
                    .stroke(MonytixTheme.stroke.opacity(0.6), lineWidth: 1)
            )
        }
        .disabled(auth.isLoading)
    }

    private func submit() {
        focusedField = nil
        auth.clearError()
        guard canSubmit else { return }
        let e = email.trimmingCharacters(in: .whitespacesAndNewlines)
        if isLogin {
            Task { await auth.signIn(email: e, password: password) }
        } else {
            Task { await auth.signUp(email: e, password: password) }
        }
    }

    private func handleSignInWithApple(_ result: Result<ASAuthorization, Error>) {
        auth.clearError()
        switch result {
        case .success(let authorization):
            guard let credential = authorization.credential as? ASAuthorizationAppleIDCredential,
                  let rawNonce = currentNonce,
                  let idTokenData = credential.identityToken,
                  let idTokenString = String(data: idTokenData, encoding: .utf8)
            else {
                auth.errorMessage = "Sign in with Apple: missing token or nonce."
                return
            }
            Task {
                await auth.signInWithApple(
                    idToken: idTokenString,
                    rawNonce: rawNonce,
                    fullName: credential.fullName
                )
            }
        case .failure(let error):
            let nsError = error as NSError
            if nsError.code != ASAuthorizationError.canceled.rawValue {
                auth.errorMessage = error.localizedDescription
            }
        }
    }

    private func randomNonceString(length: Int = 32) -> String {
        precondition(length > 0)
        var randomBytes = [UInt8](repeating: 0, count: length)
        let errorCode = SecRandomCopyBytes(kSecRandomDefault, randomBytes.count, &randomBytes)
        if errorCode != errSecSuccess {
            return UUID().uuidString.replacingOccurrences(of: "-", with: "")
        }
        let charset: [Character] = Array("0123456789ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvwxyz-._")
        return String(randomBytes.map { charset[Int($0) % charset.count] })
    }

    private func sha256(_ input: String) -> String {
        let data = Data(input.utf8)
        let hash = SHA256.hash(data: data)
        return hash.compactMap { String(format: "%02x", $0) }.joined()
    }
}

#Preview {
    AuthScreen()
}
