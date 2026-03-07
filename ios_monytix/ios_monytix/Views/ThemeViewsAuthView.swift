//
//  AuthView.swift
//  ios_monytix
//
//  Created by santosh on 06/03/26.
//

import SwiftUI
import AuthenticationServices

// MARK: - Auth View (Sign In / Sign Up Toggle)

struct AuthView: View {
    @State private var isSignIn = true
    @State private var email = ""
    @State private var password = ""
    @State private var confirmPassword = ""
    @State private var fullName = ""
    @State private var showError = false
    @State private var errorMessage = ""
    @State private var isLoading = false
    
    var body: some View {
        ZStack {
            // Deep navy background
            MonytixTheme.bg.ignoresSafeArea()
            
            ScrollView {
                VStack(spacing: MonytixSpace.xl) {
                    // Logo & Title
                    logoSection
                        .enter(delay: 0.1)
                    
                    // Toggle Sign In / Sign Up
                    authToggle
                        .enter(delay: 0.2)
                    
                    // Form Fields
                    formFields
                        .enter(delay: 0.3)
                    
                    // Error Banner
                    if showError {
                        MonytixAlertBanner(
                            type: .error,
                            message: errorMessage,
                            onDismiss: { showError = false }
                        )
                        .enter(delay: 0.0)
                    }
                    
                    // Sign In with Apple
                    signInWithAppleButton
                        .enter(delay: 0.4)
                    
                    // Sign In with Google
                    signInWithGoogleButton
                        .enter(delay: 0.42)
                    
                    // OR Divider
                    orDivider
                        .enter(delay: 0.45)
                    
                    // Email Sign In/Up Button
                    emailAuthButton
                        .enter(delay: 0.5)
                    
                    // Footer
                    footerSection
                        .enter(delay: 0.6)
                }
                .padding(MonytixSpace.lg)
            }
            
            // Loading Overlay
            if isLoading {
                MonytixLoadingOverlay(message: isSignIn ? "Signing in..." : "Creating account...")
            }
        }
    }
    
    // MARK: - Logo Section
    
    private var logoSection: some View {
        VStack(spacing: MonytixSpace.md) {
            // AI-powered logo with glow
            ZStack {
                Circle()
                    .fill(MonytixTheme.gradientPrimary)
                    .frame(width: 100, height: 100)
                    .glowPulse()
                
                Image(systemName: "chart.line.uptrend.xyaxis")
                    .font(.system(size: 40, weight: .bold))
                    .foregroundStyle(MonytixTheme.onAccent)
            }
            
            VStack(spacing: 8) {
                HStack(spacing: 8) {
                    Text("Monytix")
                        .font(.system(size: 36, weight: .bold))
                        .foregroundStyle(MonytixTheme.text1)
                    
                    Text("AI")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundStyle(MonytixTheme.onAccent)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(MonytixTheme.gradientPrimary)
                        .clipShape(Capsule())
                        .glowShadow()
                }
                
                Text(isSignIn ? "Welcome back!" : "Let's get started")
                    .font(.system(size: 16, weight: .medium))
                    .foregroundStyle(MonytixTheme.text2)
            }
        }
        .padding(.top, MonytixSpace.xl)
    }
    
    // MARK: - Auth Toggle
    
    private var authToggle: some View {
        HStack(spacing: 0) {
            // Sign In Tab
            Button {
                withAnimation(MonytixMotion.spring) {
                    isSignIn = true
                    clearFields()
                }
                HapticManager.light()
            } label: {
                Text("Sign In")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(isSignIn ? MonytixTheme.onAccent : MonytixTheme.text2)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(
                        isSignIn ? AnyShapeStyle(MonytixTheme.gradientPrimary) : AnyShapeStyle(Color.clear)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 16))
            }
            
            // Sign Up Tab
            Button {
                withAnimation(MonytixMotion.spring) {
                    isSignIn = false
                    clearFields()
                }
                HapticManager.light()
            } label: {
                Text("Sign Up")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(!isSignIn ? MonytixTheme.onAccent : MonytixTheme.text2)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(
                        !isSignIn ? AnyShapeStyle(MonytixTheme.gradientPrimary) : AnyShapeStyle(Color.clear)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 16))
            }
        }
        .padding(4)
        .background(MonytixTheme.surface)
        .clipShape(RoundedRectangle(cornerRadius: 18))
        .overlay(
            RoundedRectangle(cornerRadius: 18)
                .stroke(MonytixTheme.stroke.opacity(0.6), lineWidth: 1)
        )
    }
    
    // MARK: - Form Fields
    
    private var formFields: some View {
        VStack(spacing: MonytixSpace.md) {
            // Full Name (Sign Up only)
            if !isSignIn {
                MonytixTextField(
                    "Full Name",
                    text: $fullName,
                    placeholder: "John Doe",
                    icon: "person.fill"
                )
                .transition(.asymmetric(
                    insertion: .move(edge: .top).combined(with: .opacity),
                    removal: .move(edge: .top).combined(with: .opacity)
                ))
            }
            
            // Email
            MonytixTextField(
                "Email",
                text: $email,
                placeholder: "you@example.com",
                icon: "envelope.fill"
            )
            .textContentType(.emailAddress)
            .keyboardType(.emailAddress)
            .autocapitalization(.none)
            
            // Password
            MonytixSecureField(
                "Password",
                text: $password,
                placeholder: "Enter password",
                icon: "lock.fill"
            )
            
            // Confirm Password (Sign Up only)
            if !isSignIn {
                MonytixSecureField(
                    "Confirm Password",
                    text: $confirmPassword,
                    placeholder: "Confirm password",
                    icon: "lock.fill"
                )
                .transition(.asymmetric(
                    insertion: .move(edge: .top).combined(with: .opacity),
                    removal: .move(edge: .top).combined(with: .opacity)
                ))
            }
            
            // Forgot Password (Sign In only)
            if isSignIn {
                HStack {
                    Spacer()
                    Button {
                        // Handle forgot password
                    } label: {
                        Text("Forgot Password?")
                            .font(.system(size: 13, weight: .medium))
                            .foregroundStyle(MonytixTheme.cyan1)
                    }
                }
                .transition(.opacity)
            }
        }
    }
    
    // MARK: - Sign In with Apple Button
    
    private var signInWithAppleButton: some View {
        SignInWithAppleButton(
            onRequest: { request in
                request.requestedScopes = [.fullName, .email]
            },
            onCompletion: { result in
                handleSignInWithAppleCompletion(result)
            }
        )
        .signInWithAppleButtonStyle(.white)
        .frame(maxWidth: 375)
        .frame(height: 56)
        .clipShape(RoundedRectangle(cornerRadius: MonytixShape.buttonRadius))
        .overlay(
            RoundedRectangle(cornerRadius: MonytixShape.buttonRadius)
                .stroke(MonytixTheme.stroke.opacity(0.6), lineWidth: 1)
        )
    }
    
    // MARK: - Sign In with Google Button
    
    private var signInWithGoogleButton: some View {
        Button {
            Task {
                await AuthManager.shared.signInWithGoogle()
            }
        } label: {
            HStack(spacing: 8) {
                Image(systemName: "g.circle.fill")
                    .font(.system(size: 20))
                Text("Sign in with Google")
                    .font(.system(size: 16, weight: .semibold))
            }
            .frame(maxWidth: .infinity)
            .frame(height: 56)
            .foregroundStyle(MonytixTheme.text1)
            .background(MonytixTheme.surface2)
            .clipShape(RoundedRectangle(cornerRadius: MonytixShape.buttonRadius))
            .overlay(
                RoundedRectangle(cornerRadius: MonytixShape.buttonRadius)
                    .stroke(MonytixTheme.stroke.opacity(0.6), lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
        .disabled(AuthManager.shared.isLoading)
        .hapticFeedback()
    }
    
    // MARK: - OR Divider
    
    private var orDivider: some View {
        HStack(spacing: 16) {
            Rectangle()
                .fill(MonytixTheme.stroke.opacity(0.6))
                .frame(height: 1)
            
            Text("OR")
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(MonytixTheme.text3)
            
            Rectangle()
                .fill(MonytixTheme.stroke.opacity(0.6))
                .frame(height: 1)
        }
    }
    
    // MARK: - Email Auth Button
    
    private var emailAuthButton: some View {
        Button {
            handleEmailAuth()
        } label: {
            HStack(spacing: 8) {
                Image(systemName: "envelope.fill")
                Text("Continue with Email")
            }
        }
        .buttonStyle(MonytixSecondaryButton())
        .hapticFeedback()
    }
    
    // MARK: - Footer Section
    
    private var footerSection: some View {
        VStack(spacing: MonytixSpace.sm) {
            if !isSignIn {
                Text("By signing up, you agree to our")
                    .font(.system(size: 12, weight: .regular))
                    .foregroundStyle(MonytixTheme.text3)
                +
                Text(" Terms & Privacy Policy")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(MonytixTheme.cyan1)
            }
            
            // Security Badge
            HStack(spacing: 6) {
                Image(systemName: "lock.shield.fill")
                    .font(.system(size: 14))
                    .foregroundStyle(MonytixTheme.success)
                
                Text("Bank-level security encryption")
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(MonytixTheme.text3)
            }
            .padding(.vertical, 8)
            .padding(.horizontal, 12)
            .background(MonytixTheme.success.opacity(0.1))
            .clipShape(Capsule())
            .overlay(
                Capsule()
                    .stroke(MonytixTheme.success.opacity(0.2), lineWidth: 1)
            )
        }
        .padding(.top, MonytixSpace.md)
    }
    
    // MARK: - Helper Methods
    
    private func clearFields() {
        email = ""
        password = ""
        confirmPassword = ""
        fullName = ""
        showError = false
    }
    
    private func handleEmailAuth() {
        // Validation
        guard !email.isEmpty else {
            showErrorMessage("Please enter your email")
            return
        }
        
        guard !password.isEmpty else {
            showErrorMessage("Please enter your password")
            return
        }
        
        if !isSignIn {
            guard !fullName.isEmpty else {
                showErrorMessage("Please enter your full name")
                return
            }
            
            guard password == confirmPassword else {
                showErrorMessage("Passwords don't match")
                return
            }
            
            guard password.count >= 6 else {
                showErrorMessage("Password must be at least 6 characters")
                return
            }
        }
        
        // Theme preview: no real API call; use AuthScreen for real sign-in.
        isLoading = true
        HapticManager.medium()
        isLoading = false
        HapticManager.success()
    }
    
    private func handleSignInWithAppleCompletion(_ result: Result<ASAuthorization, Error>) {
        switch result {
        case .success(let authorization):
            if let _ = authorization.credential as? ASAuthorizationAppleIDCredential {
                isLoading = true
                HapticManager.medium()
                isLoading = false
                HapticManager.success()
            }
            
        case .failure(let error):
            let nsError = error as NSError
            if nsError.code != 1001 { // Ignore user cancellation
                showErrorMessage("Sign in with Apple failed. Please try again.")
            }
        }
    }
    
    private func showErrorMessage(_ message: String) {
        errorMessage = message
        showError = true
        HapticManager.error()
    }
}

// MARK: - Secure Text Field Component

struct MonytixSecureField: View {
    let title: String
    let placeholder: String
    @Binding var text: String
    let icon: String?
    @State private var isSecure = true
    
    init(_ title: String, text: Binding<String>, placeholder: String = "", icon: String? = nil) {
        self.title = title
        self._text = text
        self.placeholder = placeholder
        self.icon = icon
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(MonytixTheme.text2)
            
            HStack(spacing: 12) {
                if let icon = icon {
                    Image(systemName: icon)
                        .font(.system(size: 16, weight: .medium))
                        .foregroundStyle(MonytixTheme.text3)
                }
                
                if isSecure {
                    SecureField(placeholder, text: $text)
                        .font(.system(size: 16, weight: .medium))
                        .foregroundStyle(MonytixTheme.text1)
                        .tint(MonytixTheme.cyan1)
                } else {
                    TextField(placeholder, text: $text)
                        .font(.system(size: 16, weight: .medium))
                        .foregroundStyle(MonytixTheme.text1)
                        .tint(MonytixTheme.cyan1)
                }
                
                Button {
                    isSecure.toggle()
                    HapticManager.light()
                } label: {
                    Image(systemName: isSecure ? "eye.slash.fill" : "eye.fill")
                        .font(.system(size: 16, weight: .medium))
                        .foregroundStyle(MonytixTheme.text3)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .background(MonytixTheme.surface2)
            .clipShape(RoundedRectangle(cornerRadius: MonytixShape.inputRadius))
            .overlay(
                RoundedRectangle(cornerRadius: MonytixShape.inputRadius)
                    .stroke(MonytixTheme.stroke.opacity(0.6), lineWidth: 1)
            )
        }
    }
}

// MARK: - Preview

#Preview {
    AuthView()
}
