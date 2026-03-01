//
//  TermsConditionsView.swift
//  ios
//
//  Terms of Service - matches APK reference design
//

import SwiftUI

struct TermsConditionsView: View {
    let onAccept: () -> Void
    @State private var accepted = false

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            VStack(spacing: 0) {
                HStack {
                    Spacer()
                    Text("Terms of Service")
                        .font(MonytixTypography.headlineMedium)
                        .fontWeight(.semibold)
                        .foregroundColor(.white)
                    Spacer()
                }
                .padding()
                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        Text("Last Updated: June 12, 2024")
                            .font(MonytixTypography.bodyMedium)
                            .foregroundColor(.white.opacity(0.6))
                        VStack(alignment: .leading, spacing: 12) {
                            Text("1. Acceptance of Terms")
                                .font(MonytixTypography.titleMedium)
                                .fontWeight(.semibold)
                                .foregroundColor(MonytixColors.accentPrimary)
                            Text("By accessing or using the Monytix AI financial services platform, you agree to be bound by these Terms of Service. If you do not agree to all of these terms, do not use our services.")
                                .foregroundColor(.white.opacity(0.9))
                            Text("2. AI Financial Insights")
                                .font(MonytixTypography.titleMedium)
                                .fontWeight(.semibold)
                                .foregroundColor(MonytixColors.accentPrimary)
                            Text("Monytix utilizes advanced artificial intelligence to provide financial suggestions. These insights are for informational purposes only and do not constitute professional financial advice. Always consult with a licensed professional before making significant investment decisions. For more information on how we handle your financial data please refer to our Data.")
                                .foregroundColor(.white.opacity(0.9))
                        }
                        .padding(20)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(Color(hex: 0x1A1A1D))
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                        HStack(alignment: .top) {
                            Toggle("", isOn: $accepted)
                                .labelsHidden()
                                .tint(MonytixColors.accentPrimary)
                            Text("I have read and agree to the Terms of Service and Privacy Policy.")
                                .font(MonytixTypography.bodyMedium)
                                .foregroundColor(.white.opacity(0.9))
                        }
                        Button(action: onAccept) {
                            HStack {
                                Text("I Agree")
                                    .fontWeight(.semibold)
                                Spacer()
                                Image(systemName: "checkmark")
                            }
                            .frame(maxWidth: .infinity)
                            .frame(height: 52)
                            .background(accepted ? MonytixColors.accentPrimary : MonytixColors.accentPrimary.opacity(0.3))
                            .foregroundColor(.white)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                        }
                        .disabled(!accepted)
                    }
                    .padding(24)
                }
            }
        }
    }
}
