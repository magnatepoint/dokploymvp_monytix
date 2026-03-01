//
//  PrivacyPolicyView.swift
//  ios
//
//  Privacy Policy - matches APK PrivacyPolicyScreen
//

import SwiftUI

struct PrivacyPolicyView: View {
    let onContinue: () -> Void

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text("Privacy Policy")
                        .font(MonytixTypography.headlineMedium)
                        .fontWeight(.semibold)
                        .foregroundColor(.white)
                    Text("We do not read your personal messages.")
                        .font(MonytixTypography.bodyLarge)
                        .fontWeight(.medium)
                        .foregroundColor(.white.opacity(0.9))
                    Text("We do not sell your financial data.")
                        .font(MonytixTypography.bodyLarge)
                        .fontWeight(.medium)
                        .foregroundColor(.white.opacity(0.9))
                    Text("Your data is encrypted at rest and in transit.")
                        .font(MonytixTypography.bodyLarge)
                        .fontWeight(.medium)
                        .foregroundColor(.white.opacity(0.9))
                    Button(action: onContinue) {
                        Text("Continue")
                            .fontWeight(.semibold)
                            .frame(maxWidth: .infinity)
                            .frame(height: 52)
                            .background(MonytixColors.accentPrimary)
                            .foregroundColor(.white)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                }
                .padding(24)
            }
        }
    }
}
