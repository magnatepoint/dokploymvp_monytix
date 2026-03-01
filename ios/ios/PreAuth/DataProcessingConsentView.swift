//
//  DataProcessingConsentView.swift
//  ios
//
//  Privacy & Data Consent - matches APK reference design
//

import SwiftUI

struct DataProcessingConsentView: View {
    let onAccept: () -> Void
    @State private var personalizedMarketing = false

    var body: some View {
        ZStack {
            Color(hex: 0x0D0D0F).ignoresSafeArea()
            VStack(spacing: 0) {
                HStack {
                    Spacer()
                    Text("Privacy & Data Consent")
                        .font(MonytixTypography.headlineMedium)
                        .fontWeight(.semibold)
                        .foregroundColor(.white)
                    Spacer()
                }
                .padding()
                ScrollView {
                    VStack(alignment: .leading, spacing: 24) {
                        Text("Your Privacy Matters.")
                            .font(MonytixTypography.headlineMedium)
                            .fontWeight(.bold)
                            .foregroundColor(.white)
                        Text("At Monytix AI, we value your trust. Learn how we use your data to power your financial future.")
                            .font(MonytixTypography.bodyLarge)
                            .foregroundColor(.white.opacity(0.7))
                        PrivacyCard(
                            icon: "sparkles",
                            title: "AI Personalization",
                            description: "We use your transaction history to provide tailored spending insights and AI-driven wealth advice."
                        )
                        PrivacyCard(
                            icon: "shield.checkered",
                            title: "Advanced Security",
                            description: "Your data is encrypted with bank-grade standards and used to detect fraudulent activities instantly."
                        )
                        PrivacyCard(
                            icon: "checkmark.circle",
                            title: "Regulatory Compliance",
                            description: "We adhere to global financial regulations ensuring your data is handled with legal transparency."
                        )
                        HStack {
                            Text("Personalized Marketing")
                                .font(MonytixTypography.bodyLarge)
                                .foregroundColor(.white)
                            Spacer()
                            Toggle("", isOn: $personalizedMarketing)
                                .labelsHidden()
                                .tint(MonytixColors.accentPrimary)
                        }
                        Button(action: onAccept) {
                            HStack {
                                Text("Proceed")
                                    .fontWeight(.semibold)
                                Spacer()
                                Image(systemName: "arrow.right")
                            }
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
}

private struct PrivacyCard: View {
    let icon: String
    let title: String
    let description: String

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Image(systemName: icon)
                    .font(.system(size: 24))
                    .foregroundColor(MonytixColors.accentPrimary)
                Text(title)
                    .font(MonytixTypography.titleMedium)
                    .fontWeight(.semibold)
                    .foregroundColor(.white)
            }
            Text(description)
                .font(MonytixTypography.bodyMedium)
                .foregroundColor(.white.opacity(0.8))
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(hex: 0x1A1A1D))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}
