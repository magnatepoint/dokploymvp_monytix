//
//  AuthView.swift
//  ios
//
//  Auth screen - matches APK AuthScreen with phone OTP, email, Apple, Google
//

import SwiftUI
import AuthenticationServices
import CryptoKit

struct AuthView: View {
    @Environment(AuthViewModel.self) private var authViewModel

    var body: some View {
        Group {
            switch authViewModel.authStep {
            case .login:
                LoginContentView()
            case .otp:
                OtpView(
                    phone: authViewModel.phoneForOtp,
                    otp: authViewModel.otp,
                    onOtpChange: { authViewModel.setOtp($0) },
                    resendSecondsLeft: authViewModel.resendSecondsLeft,
                    onVerify: { Task { await authViewModel.verifyOtp() } },
                    onResend: { authViewModel.resendOtp() }
                )
            }
        }
        .onAppear {
            let screenName = authViewModel.authStep == .otp ? "otp" : "auth"
            AnalyticsHelper.logScreenView(screenName: screenName)
        }
        .onChange(of: authViewModel.authStep) { _, newStep in
            let screenName = newStep == .otp ? "otp" : "auth"
            AnalyticsHelper.logScreenView(screenName: screenName)
        }
    }
}

private struct LoginContentView: View {
    @Environment(AuthViewModel.self) private var authViewModel
    @State private var phone = ""
    @State private var showEmailOption = false
    @State private var email = ""
    @State private var password = ""
    @State private var confirmPassword = ""
    @State private var isSignUpMode = false
    @State private var showResetPassword = false
    @State private var currentNonce: String?

    var body: some View {
        PremiumGradientBackground {
            ScrollView {
                VStack(spacing: 24) {
                    VStack(spacing: 16) {
                        Image("Logo")
                            .resizable()
                            .scaledToFit()
                            .frame(maxWidth: 200, maxHeight: 80)
                        Text("Welcome")
                            .font(MonytixTypography.bodyLarge)
                            .foregroundColor(MonytixColors.textPrimary.opacity(0.7))
                    }
                    .padding(.top, 40)

                    if let errorMessage = authViewModel.errorMessage {
                        Text(errorMessage)
                            .font(MonytixTypography.bodyMedium)
                            .foregroundColor(MonytixColors.error)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal)
                    }

                    // Phone + Send OTP (primary - matches APK)
                    TextField("+91 98765 43210", text: $phone)
                        .keyboardType(.phonePad)
                        .padding()
                        .foregroundColor(MonytixColors.textPrimary)
                        .background(MonytixColors.surfaceElevated)
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(MonytixColors.textPrimary.opacity(0.3), lineWidth: 1)
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 12))

                    Button {
                        Task { await authViewModel.sendPhoneOtp(phone: phone) }
                    } label: {
                        if authViewModel.isLoading {
                            ProgressView().tint(.white)
                        } else {
                            Text("Send OTP").fontWeight(.medium)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(phone.count >= 10 ? MonytixColors.accentPrimary : MonytixColors.textMuted)
                    .foregroundStyle(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .disabled(phone.count < 10 || authViewModel.isLoading)

                    Text("or")
                        .font(MonytixTypography.bodyMedium)
                        .foregroundColor(MonytixColors.textSecondary.opacity(0.5))

                    // Sign in with Google
                    Button {
                        Task { await authViewModel.signInWithGoogle() }
                    } label: {
                        HStack {
                            Image(systemName: "globe")
                            Text("Continue with Google").fontWeight(.medium)
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .foregroundColor(MonytixColors.textPrimary)
                        .background(MonytixColors.surfaceElevated)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                    .buttonStyle(.plain)
                    .disabled(authViewModel.isLoading)

                    Text("If the login page doesn't load, check your network or try without VPN.")
                        .font(MonytixTypography.labelSmall)
                        .foregroundColor(MonytixColors.textSecondary.opacity(0.5))

                    // Sign in with Apple (required by App Store when offering Google)
                    SignInWithAppleButton(.signIn) { request in
                        let nonce = randomNonceString()
                        currentNonce = nonce
                        request.requestedScopes = [.email, .fullName]
                        request.nonce = sha256(nonce)
                    } onCompletion: { result in
                        Task { await handleAppleSignIn(result) }
                    }
                    .signInWithAppleButtonStyle(.white)
                    .frame(height: 50)
                    .frame(maxWidth: 375)
                    .frame(maxWidth: .infinity)
                    .clipShape(RoundedRectangle(cornerRadius: 12))

                    Button {
                        authViewModel.clearError()
                        showEmailOption.toggle()
                    } label: {
                        Text("Email + Password")
                            .font(MonytixTypography.bodyMedium)
                            .foregroundColor(MonytixColors.textPrimary.opacity(0.8))
                    }

                    if showEmailOption {
                        emailPasswordSection
                    }

                    Spacer(minLength: 24)
                }
                .padding(24)
            }
            .sheet(isPresented: $showResetPassword) {
                ResetPasswordView()
            }
        }
    }

    @ViewBuilder
    private var emailPasswordSection: some View {
        VStack(spacing: 16) {
            TextField("Email", text: $email)
                .textContentType(.emailAddress)
                .keyboardType(.emailAddress)
                .padding()
                .foregroundColor(MonytixColors.textPrimary)
                .background(MonytixColors.surfaceElevated)
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(MonytixColors.textPrimary.opacity(0.3), lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: 12))

            SecureField("Password", text: $password)
                .textContentType(isSignUpMode ? .newPassword : .password)
                .padding()
                .foregroundColor(MonytixColors.textPrimary)
                .background(MonytixColors.surfaceElevated)
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(MonytixColors.textPrimary.opacity(0.3), lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: 12))

            if isSignUpMode {
                SecureField("Confirm Password", text: $confirmPassword)
                    .textContentType(.newPassword)
                    .padding()
                    .foregroundColor(MonytixColors.textPrimary)
                    .background(MonytixColors.surfaceElevated)
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(MonytixColors.textPrimary.opacity(0.3), lineWidth: 1)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            }

            Button {
                Task {
                    if isSignUpMode {
                        await authViewModel.signUp(email: email, password: password)
                    } else {
                        await authViewModel.signIn(email: email, password: password)
                    }
                }
            } label: {
                if authViewModel.isLoading {
                    ProgressView().tint(.white)
                } else {
                    Text(isSignUpMode ? "Sign Up" : "Sign In").fontWeight(.medium)
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: 52)
            .background(emailPasswordValid ? MonytixColors.accentPrimary : MonytixColors.textMuted)
            .foregroundStyle(.white)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .disabled(!emailPasswordValid || authViewModel.isLoading)

            if !isSignUpMode {
                Button { showResetPassword = true } label: {
                    Text("Forgot Password?")
                        .font(MonytixTypography.bodyMedium)
                        .foregroundColor(MonytixColors.accentPrimary)
                }
            }

            Button {
                authViewModel.clearError()
                isSignUpMode.toggle()
                email = ""
                password = ""
                confirmPassword = ""
            } label: {
                Text(isSignUpMode ? "Already have an account? Sign In" : "Don't have an account? Sign Up")
                    .font(MonytixTypography.bodyMedium)
                    .foregroundColor(MonytixColors.textPrimary.opacity(0.8))
            }
        }
    }

    private var emailPasswordValid: Bool {
        guard !email.isEmpty, email.contains("@"), password.count >= 6 else { return false }
        if isSignUpMode { return password == confirmPassword }
        return true
    }

    private func handleAppleSignIn(_ result: Result<ASAuthorization, Error>) async {
        switch result {
        case .success(let authorization):
            if let cred = authorization.credential as? ASAuthorizationAppleIDCredential,
               let tokenData = cred.identityToken,
               let tokenString = String(data: tokenData, encoding: .utf8),
               let nonce = currentNonce {
                await authViewModel.signInWithApple(idToken: tokenString, nonce: nonce)
            } else {
                authViewModel.errorMessage = "Failed to get Apple credentials"
            }
        case .failure(let error):
            authViewModel.errorMessage = error.localizedDescription
        }
    }

    private func randomNonceString(length: Int = 32) -> String {
        precondition(length > 0)
        var randomBytes = [UInt8](repeating: 0, count: length)
        _ = SecRandomCopyBytes(kSecRandomDefault, randomBytes.count, &randomBytes)
        let charset: [Character] = Array("0123456789ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvwxyz-._")
        return String(randomBytes.map { charset[Int($0) % charset.count] })
    }

    private func sha256(_ input: String) -> String {
        let inputData = Data(input.utf8)
        let hashedData = SHA256.hash(data: inputData)
        return hashedData.compactMap { String(format: "%02x", $0) }.joined()
    }
}

private struct OtpView: View {
    let phone: String
    let otp: String
    let onOtpChange: (String) -> Void
    let resendSecondsLeft: Int
    let onVerify: () -> Void
    let onResend: () -> Void

    var body: some View {
        PremiumGradientBackground {
            VStack(spacing: 24) {
                Text("Enter verification code")
                    .font(MonytixTypography.headlineMedium)
                    .fontWeight(.medium)
                    .foregroundColor(MonytixColors.textPrimary)
                Text("Code sent to \(phone)")
                    .font(MonytixTypography.bodyMedium)
                    .foregroundColor(MonytixColors.textSecondary)
                TextField("000000", text: Binding(
                    get: { otp },
                    set: { new in
                        if new.count <= 6, new.allSatisfy(\.isNumber) {
                            onOtpChange(new)
                        }
                    }
                ))
                .keyboardType(.numberPad)
                .multilineTextAlignment(.center)
                .font(.title2)
                .padding()
                .foregroundColor(MonytixColors.textPrimary)
                .background(MonytixColors.surfaceElevated)
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(MonytixColors.textPrimary.opacity(0.3), lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: 12))

                if resendSecondsLeft > 0 {
                    Text("Resend in \(resendSecondsLeft)s")
                        .font(MonytixTypography.bodySmall)
                        .foregroundColor(MonytixColors.textSecondary)
                } else {
                    Button(action: onResend) {
                        Text("Resend")
                            .foregroundColor(MonytixColors.textPrimary.opacity(0.9))
                    }
                }

                Button(action: onVerify) {
                    Text("Verify")
                        .fontWeight(.medium)
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .background(otp.count == 6 ? MonytixColors.accentPrimary : MonytixColors.textMuted)
                        .foregroundColor(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                .disabled(otp.count != 6)
            }
            .padding(32)
        }
    }
}

struct ResetPasswordView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(AuthViewModel.self) private var authViewModel
    @State private var email = ""
    @State private var showSuccess = false

    var body: some View {
        NavigationStack {
            PremiumGradientBackground {
                VStack(spacing: 24) {
                    Image(systemName: "envelope.circle.fill")
                        .font(.system(size: 60))
                        .foregroundStyle(MonytixColors.accentPrimary)

                    Text("Reset Password")
                        .font(MonytixTypography.titleLarge)
                        .fontWeight(.bold)
                        .foregroundColor(MonytixColors.textPrimary)

                    Text("Enter your email address and we'll send you instructions to reset your password.")
                        .font(MonytixTypography.bodyMedium)
                        .foregroundColor(MonytixColors.textSecondary)
                        .multilineTextAlignment(.center)

                    TextField("Email", text: $email)
                        .textContentType(.emailAddress)
                        .keyboardType(.emailAddress)
                        .padding()
                        .foregroundColor(MonytixColors.textPrimary)
                        .background(MonytixColors.surfaceElevated)
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(MonytixColors.textPrimary.opacity(0.3), lineWidth: 1)
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 12))

                    if let errorMessage = authViewModel.errorMessage {
                        Text(errorMessage)
                            .font(MonytixTypography.bodyMedium)
                            .foregroundColor(MonytixColors.error)
                            .multilineTextAlignment(.center)
                    }

                    Button {
                        Task {
                            await authViewModel.resetPassword(email: email)
                            if authViewModel.errorMessage == nil {
                                showSuccess = true
                            }
                        }
                    } label: {
                        if authViewModel.isLoading {
                            ProgressView().tint(.white)
                        } else {
                            Text("Send Reset Link").fontWeight(.semibold)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(email.contains("@") ? MonytixColors.accentPrimary : MonytixColors.textMuted)
                    .foregroundStyle(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .disabled(!email.contains("@") || authViewModel.isLoading)

                    Spacer()
                }
                .padding(24)
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Cancel") { dismiss() }
                            .foregroundColor(MonytixColors.accentPrimary)
                    }
                }
                .alert("Check Your Email", isPresented: $showSuccess) {
                    Button("OK") { dismiss() }
                } message: {
                    Text("We've sent password reset instructions to \(email)")
                }
            }
        }
    }
}

#Preview {
    AuthView()
        .environment(AuthViewModel())
}
