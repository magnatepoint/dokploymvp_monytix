//
//  PreAuthViewModel.swift
//  ios
//
//  Pre-auth flow - matches APK PreAuthViewModel
//

import Foundation

enum PreAuthStep {
    case splash
    case updateRequired
    case deviceVerification
    case onboarding
    case termsConditions
    case privacyPolicy
    case dataProcessingConsent
    case permissionExplainer
    case auth
}

@MainActor
@Observable
class PreAuthViewModel {
    var step: PreAuthStep = .splash
    var isLoading = true
    var forceUpdate = false
    var appStoreUrl = "https://apps.apple.com/app/monytix"
    var securityWarning = false

    private let api = BackendApi()

    init() {
        Task { await runSplashChecks() }
    }

    func runSplashChecks() async {
        isLoading = true
        try? await Task.sleep(nanoseconds: 500_000_000) // 500ms min splash

        do {
            let config = try await api.getConfig()
            let versionCode = Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "1"
            let currentVersion = Int(versionCode) ?? 1
            let minRequired = config.minVersionCode ?? 1
            forceUpdate = currentVersion < minRequired
            appStoreUrl = config.appStoreUrl ?? "https://apps.apple.com/app/monytix"
        } catch {
            // Timeout or failure: proceed with defaults
            forceUpdate = false
            appStoreUrl = "https://apps.apple.com/app/monytix"
        }

        // iOS: no test-keys / root detection like Android; skip device verification
        let securityOk = true
        isLoading = false
        securityWarning = !securityOk
        await advanceToNextStep(updateRequired: forceUpdate, securityOk: securityOk)
    }

    private func advanceToNextStep(updateRequired: Bool, securityOk: Bool) async {
        if updateRequired {
            step = .updateRequired
            return
        }
        if !securityOk {
            step = .deviceVerification
            return
        }
        if !PreAuthPreferences.onboardingComplete {
            step = .onboarding
            return
        }
        if !PreAuthPreferences.termsAccepted {
            step = .termsConditions
            return
        }
        if !PreAuthPreferences.privacyViewed {
            step = .privacyPolicy
            return
        }
        if !PreAuthPreferences.dataConsent {
            step = .dataProcessingConsent
            return
        }
        if !PreAuthPreferences.permissionsExplained {
            step = .permissionExplainer
            return
        }
        step = .auth
    }

    func completeOnboarding() {
        AnalyticsHelper.logEvent("onboarding_complete")
        PreAuthPreferences.onboardingComplete = true
        step = .termsConditions
    }

    func acceptTerms() {
        AnalyticsHelper.logEvent("terms_accepted")
        PreAuthPreferences.termsAccepted = true
        step = .privacyPolicy
    }

    func completePrivacyPolicy() {
        AnalyticsHelper.logEvent("privacy_continue")
        PreAuthPreferences.privacyViewed = true
        step = .dataProcessingConsent
    }

    func acceptDataConsent() {
        AnalyticsHelper.logEvent("data_consent_accepted")
        PreAuthPreferences.dataConsent = true
        step = .permissionExplainer
    }

    func completePermissionExplainer() {
        AnalyticsHelper.logEvent("permission_continue")
        PreAuthPreferences.permissionsExplained = true
        step = .auth
    }

    func completeDeviceVerification() {
        AnalyticsHelper.logEvent("device_verification_continue")
        Task {
            await advanceToNextStep(updateRequired: false, securityOk: true)
        }
    }

    func goToAuth() {
        AnalyticsHelper.logEvent("login_clicked")
        PreAuthPreferences.setAllComplete()
        step = .auth
    }
}
