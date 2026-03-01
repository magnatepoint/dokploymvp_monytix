//
//  DeviceVerificationView.swift
//  ios
//
//  Device Verification - matches APK reference design
//

import SwiftUI

struct DeviceVerificationView: View {
    let onContinue: () -> Void
    let onContactSupport: () -> Void

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            VStack(spacing: 24) {
                Image(systemName: "touchid")
                    .font(.system(size: 64))
                    .foregroundColor(MonytixColors.accentPrimary)
                    .frame(width: 120, height: 120)
                    .overlay(
                        Circle()
                            .stroke(MonytixColors.accentPrimary.opacity(0.5), lineWidth: 2)
                    )
                Text("Device Verification")
                    .font(MonytixTypography.headlineMedium)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
                (Text("Verifying your device for secure access to ")
                    .foregroundColor(.white.opacity(0.8)) +
                 Text("Monytix AI")
                    .foregroundColor(MonytixColors.accentPrimary))
                    .font(MonytixTypography.bodyLarge)
                    .multilineTextAlignment(.center)
                Button(action: onContinue) {
                    Text("Verify Now")
                        .fontWeight(.semibold)
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .background(MonytixColors.accentPrimary)
                        .foregroundColor(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                .padding(.horizontal, 24)
                Button(action: onContactSupport) {
                    Text("Trouble verifying?")
                        .foregroundColor(MonytixColors.accentSecondary)
                }
                Spacer()
                HStack(spacing: 8) {
                    Image(systemName: "lock")
                        .font(.system(size: 12))
                        .foregroundColor(.white.opacity(0.6))
                    Text("END-TO-END ENCRYPTION")
                        .font(MonytixTypography.labelSmall)
                        .foregroundColor(.white.opacity(0.6))
                }
            }
            .padding(24)
        }
    }
}
