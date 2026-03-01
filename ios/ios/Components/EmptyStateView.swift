//
//  EmptyStateView.swift
//  ios
//
//  Empty state component - matches APK EmptyState
//

import SwiftUI

struct EmptyStateView: View {
    let title: String
    let subtitle: String
    var onRetry: (() -> Void)?

    var body: some View {
        VStack(spacing: 16) {
            Text(title)
                .font(MonytixTypography.titleLarge)
                .foregroundColor(MonytixColors.textPrimary)
                .multilineTextAlignment(.center)

            Text(subtitle)
                .font(MonytixTypography.bodyMedium)
                .foregroundColor(MonytixColors.textSecondary)
                .multilineTextAlignment(.center)

            if let onRetry {
                Button("Retry", action: onRetry)
                    .buttonStyle(.borderedProminent)
                    .tint(MonytixColors.accentPrimary)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(32)
    }
}

#Preview {
    EmptyStateView(
        title: "No data",
        subtitle: "Upload to get started",
        onRetry: {}
    )
}
