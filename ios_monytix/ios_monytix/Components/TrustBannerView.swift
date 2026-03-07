//
//  TrustBannerView.swift
//  ios_monytix
//
//  Trust signal block for fintech flows: login, upload, onboarding.
//  Premium, calm copy to reinforce encryption and user control. Mirrors APK TrustBanner.
//

import SwiftUI

struct TrustBannerView: View {
    let headline: String
    let bodyText: String
    var icon: String? = nil
    var learnMoreLabel: String? = nil
    var onLearnMore: (() -> Void)? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            if let icon = icon {
                Image(systemName: icon)
                    .font(.system(size: 18, weight: .medium))
                    .foregroundStyle(MonytixTheme.cyan1.opacity(0.9))
            }
            Text(headline)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(MonytixTheme.text2)
            Text(bodyText)
                .font(.system(size: 12, weight: .regular))
                .foregroundStyle(MonytixTheme.text2.opacity(0.9))
            if let label = learnMoreLabel, let action = onLearnMore {
                Button(label, action: action)
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(MonytixTheme.cyan1)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(MonytixSpace.md)
    }
}

// MARK: - Preset banners (match APK strings)

struct TrustBannerEncryption: View {
    var body: some View {
        TrustBannerView(
            headline: "Your data is encrypted",
            bodyText: "Encrypted in transit and at rest. We never store your bank password."
        )
    }
}

struct TrustBannerUpload: View {
    var body: some View {
        TrustBannerView(
            headline: "Secure upload",
            bodyText: "Files are encrypted and processed securely. We don't share your data."
        )
    }
}

struct TrustBannerReadOnly: View {
    var body: some View {
        TrustBannerView(
            headline: "Read-only analysis",
            bodyText: "We can't move money or change your account. Analysis only."
        )
    }
}
