//
//  PreAuthPreferences.swift
//  ios
//
//  Pre-auth consent and onboarding state - matches APK PreAuthPreferences
//

import Foundation

enum PreAuthPreferences {
    private static let defaults = UserDefaults.standard
    private static let keyOnboardingComplete = "preauth_onboarding_complete"
    private static let keyTermsAccepted = "preauth_terms_accepted"
    private static let keyPrivacyViewed = "preauth_privacy_viewed"
    private static let keyDataConsent = "preauth_data_consent"
    private static let keyPermissionsExplained = "preauth_permissions_explained"

    static var onboardingComplete: Bool {
        get { defaults.bool(forKey: keyOnboardingComplete) }
        set { defaults.set(newValue, forKey: keyOnboardingComplete) }
    }

    static var termsAccepted: Bool {
        get { defaults.bool(forKey: keyTermsAccepted) }
        set { defaults.set(newValue, forKey: keyTermsAccepted) }
    }

    static var privacyViewed: Bool {
        get { defaults.bool(forKey: keyPrivacyViewed) }
        set { defaults.set(newValue, forKey: keyPrivacyViewed) }
    }

    static var dataConsent: Bool {
        get { defaults.bool(forKey: keyDataConsent) }
        set { defaults.set(newValue, forKey: keyDataConsent) }
    }

    static var permissionsExplained: Bool {
        get { defaults.bool(forKey: keyPermissionsExplained) }
        set { defaults.set(newValue, forKey: keyPermissionsExplained) }
    }

    static func setAllComplete() {
        onboardingComplete = true
        termsAccepted = true
        privacyViewed = true
        dataConsent = true
        permissionsExplained = true
    }
}
