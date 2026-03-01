//
//  OnboardingView.swift
//  ios
//
//  Onboarding slides - matches APK reference design
//

import SwiftUI

struct OnboardingView: View {
    let onComplete: () -> Void
    let onLogin: () -> Void

    @State private var currentPage = 0

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [Color(hex: 0x0D0D0F), Color(hex: 0x08080A)],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()
            VStack(spacing: 0) {
                TabView(selection: $currentPage) {
                    Slide1Content().tag(0)
                    Slide2Content().tag(1)
                    Slide3Content().tag(2)
                }
                .tabViewStyle(.page(indexDisplayMode: .never))
                .indexViewStyle(.page(backgroundDisplayMode: .never))
                VStack(spacing: 24) {
                    HStack(spacing: 8) {
                        ForEach(0..<3, id: \.self) { index in
                            if index == currentPage {
                                Capsule()
                                    .fill(MonytixColors.accentPrimary)
                                    .frame(width: 24, height: 4)
                            } else {
                                Circle()
                                    .fill(Color.white.opacity(0.3))
                                    .frame(width: 8, height: 8)
                            }
                        }
                    }
                    if currentPage < 2 {
                        Button {
                            withAnimation { currentPage += 1 }
                        } label: {
                            HStack {
                                Text("Continue with Mobile")
                                    .fontWeight(.semibold)
                                Spacer()
                                Image(systemName: "arrow.right")
                            }
                            .frame(maxWidth: .infinity)
                            .frame(height: 52)
                            .background(MonytixColors.accentPrimary)
                            .foregroundColor(.white)
                            .clipShape(RoundedRectangle(cornerRadius: 24))
                        }
                    } else {
                        Button(action: onComplete) {
                            Text("GET STARTED")
                                .fontWeight(.semibold)
                                .frame(maxWidth: .infinity)
                                .frame(height: 52)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 12)
                                        .stroke(MonytixColors.accentPrimary, lineWidth: 2)
                                )
                                .foregroundColor(.white)
                        }
                    }
                    HStack {
                        Text(currentPage < 2 ? "Existing user? " : "Already have an account? ")
                            .foregroundColor(.white.opacity(0.8))
                        Button(action: onLogin) {
                            Text("Login")
                                .fontWeight(.semibold)
                                .foregroundColor(MonytixColors.accentPrimary)
                        }
                    }
                }
                .padding(24)
            }
        }
    }
}

private struct Slide1Content: View {
    var body: some View {
        VStack(spacing: 24) {
            Text("₹")
                .font(.system(size: 72))
                .foregroundColor(.white)
            HStack(spacing: 8) {
                GlassChip(icon: "chart.line.uptrend.xyaxis", label: "+12.4%")
                GlassChip(icon: "sparkles", label: "AI CREDIT SCORE\n₹ 782")
            }
            VStack(spacing: 4) {
                (Text("Smart Wealth for ")
                    .foregroundColor(.white) +
                 Text("Bharat")
                    .foregroundColor(MonytixColors.accentPrimary))
                    .font(MonytixTypography.headlineLarge)
                    .fontWeight(.bold)
                Text("AI-powered financial planning built specifically for the Indian market.")
                    .font(MonytixTypography.bodyLarge)
                    .foregroundColor(.white.opacity(0.8))
                    .multilineTextAlignment(.center)
            }
        }
        .padding(32)
    }
}

private struct Slide2Content: View {
    var body: some View {
        VStack(spacing: 24) {
            Text("₹")
                .font(.system(size: 72))
                .foregroundColor(.white.opacity(0.9))
            (Text("AI-powered insights &\n")
                .foregroundColor(.white) +
             Text("future ")
                .foregroundColor(MonytixColors.accentPrimary) +
             Text("predictions")
                .foregroundColor(MonytixColors.accentSecondary))
                .font(MonytixTypography.headlineMedium)
                .fontWeight(.bold)
                .multilineTextAlignment(.center)
        }
        .padding(32)
    }
}

private struct Slide3Content: View {
    var body: some View {
        VStack(spacing: 24) {
            HStack {
                Spacer()
                HStack(spacing: 8) {
                    Circle()
                        .fill(Color(hex: 0x34D399))
                        .frame(width: 8, height: 8)
                    Text("ENCRYPTED")
                        .font(MonytixTypography.labelMedium)
                        .foregroundColor(.white)
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(Color.white.opacity(0.08))
                .clipShape(RoundedRectangle(cornerRadius: 20))
            }
            Image(systemName: "shield.checkered")
                .font(.system(size: 80))
                .foregroundColor(MonytixColors.accentPrimary)
            Text("256-BIT AES")
                .font(MonytixTypography.labelSmall)
                .foregroundColor(.white.opacity(0.6))
            (Text("Bank-grade ")
                .foregroundColor(.white) +
             Text("Encryption & ")
                .foregroundColor(MonytixColors.accentPrimary) +
             Text("Privacy-first design")
                .foregroundColor(.white))
                .font(MonytixTypography.headlineMedium)
                .fontWeight(.bold)
                .multilineTextAlignment(.center)
            Text("Your financial data is protected by the same security standards used by world-leading banks.")
                .font(MonytixTypography.bodyLarge)
                .foregroundColor(.white.opacity(0.8))
                .multilineTextAlignment(.center)
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 2)
                        .fill(Color.white.opacity(0.2))
                        .frame(height: 4)
                    RoundedRectangle(cornerRadius: 2)
                        .fill(MonytixColors.accentPrimary)
                        .frame(width: geo.size.width * 0.6, height: 4)
                }
            }
            .frame(height: 4)
        }
        .padding(32)
    }
}

private struct GlassChip: View {
    let icon: String
    let label: String

    var body: some View {
        VStack(spacing: 4) {
            Image(systemName: icon)
                .font(.system(size: 24))
                .foregroundColor(MonytixColors.accentPrimary)
            Text(label)
                .font(MonytixTypography.labelSmall)
                .foregroundColor(.white)
        }
        .padding(12)
        .background(Color.white.opacity(0.08))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}
