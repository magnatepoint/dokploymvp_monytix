//
//  UpdateRequiredView.swift
//  ios
//
//  Time for an Upgrade - matches APK reference design
//

import SwiftUI

struct UpdateRequiredView: View {
    let appStoreUrl: String

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            VStack(spacing: 24) {
                Image(systemName: "arrow.up")
                    .font(.system(size: 40))
                    .foregroundColor(.white)
                    .frame(width: 80, height: 80)
                    .background(MonytixColors.accentPrimary)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                Text("Time for an Upgrade")
                    .font(MonytixTypography.headlineMedium)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
                Text("We've introduced some powerful new AI features to help you manage your finances even better. Update now to continue using Monytix safely and efficiently.")
                    .font(MonytixTypography.bodyLarge)
                    .foregroundColor(.white.opacity(0.8))
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 24)
                Button {
                    AnalyticsHelper.logEvent("update_now_clicked")
                    if let url = URL(string: appStoreUrl) {
                        UIApplication.shared.open(url)
                    }
                } label: {
                    Text("Update Now")
                        .fontWeight(.semibold)
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .background(MonytixColors.accentPrimary)
                        .foregroundColor(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                .padding(.horizontal, 24)
                Button(action: {}) {
                    Text("Not Now")
                        .foregroundColor(.white)
                }
            }
        }
    }
}
