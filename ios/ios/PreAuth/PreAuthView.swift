//
//  PreAuthView.swift
//  ios
//
//  Pre-auth flow root - matches APK PreAuthScreen
//

import SwiftUI

struct PreAuthView: View {
    @State private var preAuthViewModel = PreAuthViewModel()
    @Environment(AuthViewModel.self) private var authViewModel

    var body: some View {
        Group {
            switch preAuthViewModel.step {
            case .splash:
                SplashContentView(isLoading: preAuthViewModel.isLoading)
            case .updateRequired:
                UpdateRequiredView(appStoreUrl: preAuthViewModel.appStoreUrl)
            case .deviceVerification:
                DeviceVerificationView(
                    onContinue: { preAuthViewModel.completeDeviceVerification() },
                    onContactSupport: { /* TODO: Open support URL */ }
                )
            case .onboarding:
                OnboardingView(
                    onComplete: { preAuthViewModel.completeOnboarding() },
                    onLogin: { preAuthViewModel.goToAuth() }
                )
            case .termsConditions:
                TermsConditionsView(onAccept: { preAuthViewModel.acceptTerms() })
            case .privacyPolicy:
                PrivacyPolicyView(onContinue: { preAuthViewModel.completePrivacyPolicy() })
            case .dataProcessingConsent:
                DataProcessingConsentView(onAccept: { preAuthViewModel.acceptDataConsent() })
            case .permissionExplainer:
                PermissionExplainerView(onContinue: { preAuthViewModel.completePermissionExplainer() })
            case .auth:
                AuthView()
            }
        }
        .onAppear {
            let screenName: String
            switch preAuthViewModel.step {
            case .splash: screenName = "splash"
            case .updateRequired: screenName = "update_required"
            case .deviceVerification: screenName = "device_verification"
            case .onboarding: screenName = "onboarding"
            case .termsConditions: screenName = "terms_conditions"
            case .privacyPolicy: screenName = "privacy_policy"
            case .dataProcessingConsent: screenName = "data_consent"
            case .permissionExplainer: screenName = "permission_explainer"
            case .auth: screenName = "auth"
            }
            AnalyticsHelper.logScreenView(screenName: screenName)
        }
        .onChange(of: preAuthViewModel.step) { _, newStep in
            let screenName: String
            switch newStep {
            case .splash: screenName = "splash"
            case .updateRequired: screenName = "update_required"
            case .deviceVerification: screenName = "device_verification"
            case .onboarding: screenName = "onboarding"
            case .termsConditions: screenName = "terms_conditions"
            case .privacyPolicy: screenName = "privacy_policy"
            case .dataProcessingConsent: screenName = "data_consent"
            case .permissionExplainer: screenName = "permission_explainer"
            case .auth: screenName = "auth"
            }
            AnalyticsHelper.logScreenView(screenName: screenName)
        }
    }
}

private struct SplashContentView: View {
    let isLoading: Bool

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            VStack(spacing: 24) {
                Image("Logo")
                    .resizable()
                    .scaledToFit()
                    .frame(maxWidth: 200, maxHeight: 80)
                Text("MONYTIX")
                    .font(MonytixTypography.headlineLarge)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
                Text("AI INTELLIGENCE")
                    .font(MonytixTypography.bodyMedium)
                    .foregroundColor(.white.opacity(0.6))
                GeometryReader { geo in
                    ZStack(alignment: .leading) {
                        RoundedRectangle(cornerRadius: 2)
                            .fill(Color.white.opacity(0.2))
                            .frame(height: 4)
                        RoundedRectangle(cornerRadius: 2)
                            .fill(MonytixColors.accentPrimary)
                            .frame(width: geo.size.width * 0.5, height: 4)
                    }
                }
                .frame(height: 4)
                Text("SECURING CONNECTION...")
                    .font(MonytixTypography.bodySmall)
                    .foregroundColor(.white.opacity(0.6))
                if isLoading {
                    ProgressView()
                        .tint(MonytixColors.accentPrimary)
                        .scaleEffect(0.8)
                }
            }
            .padding(24)
        }
    }
}

#Preview {
    PreAuthView()
        .environment(AuthViewModel())
}
