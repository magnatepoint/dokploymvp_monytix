//
//  PermissionExplainerView.swift
//  ios
//
//  Security & Experience - dark theme (matches app design system)
//

import SwiftUI

struct PermissionExplainerView: View {
    let onContinue: () -> Void

    var body: some View {
        ZStack {
            MonytixColors.surfaceDark.ignoresSafeArea()
            ScrollView {
                VStack(alignment: .leading, spacing: 24) {
                    Image(systemName: "shield.checkered")
                        .font(.system(size: 48))
                        .foregroundColor(MonytixColors.accentPrimary)
                        .padding(.bottom, 8)
                    Text("Security & Experience")
                        .font(MonytixTypography.headlineMedium)
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                    Text("To provide AI-powered insights and secure your account, Monytix needs the following permissions.")
                        .font(MonytixTypography.bodyLarge)
                        .foregroundColor(.white.opacity(0.8))
                    PermissionCard(
                        icon: "bubble.left.and.bubble.right",
                        title: "Transaction Tracking",
                        description: "Monytix AI reads transaction SMS to automatically categorize your spending and update your budget in real-time."
                    )
                    PermissionCard(
                        icon: "person.2",
                        title: "Split Bills",
                        description: "Easily find friends to split dinners or shared expenses without typing phone numbers manually."
                    )
                    PermissionCard(
                        icon: "location",
                        title: "Enhanced Security",
                        description: "We use your location to prevent fraudulent transactions and secure your account."
                    )
                    Button(action: onContinue) {
                        Text("Allow Permissions")
                            .fontWeight(.semibold)
                            .frame(maxWidth: .infinity)
                            .frame(height: 52)
                            .background(MonytixColors.accentPrimary)
                            .foregroundColor(.white)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                    Button(action: onContinue) {
                        Text("I'll do this later")
                            .foregroundColor(.white.opacity(0.7))
                    }
                    .frame(maxWidth: .infinity)
                }
                .padding(24)
            }
        }
    }
}

private struct PermissionCard: View {
    let icon: String
    let title: String
    let description: String

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 24))
                .foregroundColor(MonytixColors.accentPrimary)
                .frame(width: 44, height: 44)
                .background(MonytixColors.accentPrimary.opacity(0.2))
                .clipShape(RoundedRectangle(cornerRadius: 10))
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(MonytixTypography.titleMedium)
                    .fontWeight(.semibold)
                    .foregroundColor(.white)
                Text(description)
                    .font(MonytixTypography.bodyMedium)
                    .foregroundColor(.white.opacity(0.8))
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(MonytixColors.surfaceElevated)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}
