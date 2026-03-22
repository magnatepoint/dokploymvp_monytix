//
//  GuidanceView.swift
//  ios_monytix
//
//  AI hub: entry to Assistant + placeholder tip cards.
//

import SwiftUI

struct GuidanceView: View {
    @State private var showAssistant = false

    var body: some View {
        NavigationStack {
            ZStack {
                MonytixTheme.bg.ignoresSafeArea()
                ScrollView {
                    VStack(alignment: .leading, spacing: MonytixSpace.lg) {
                        Text("Tips and recommendations to improve your finances.")
                            .font(.system(size: 14))
                            .foregroundStyle(MonytixTheme.text2)

                        Button {
                            showAssistant = true
                        } label: {
                            HStack {
                                Image(systemName: "lightbulb.fill")
                                    .foregroundStyle(MonytixTheme.cyan1)
                                Text("Ask MONYTIX")
                                    .font(.system(size: 16, weight: .semibold))
                                    .foregroundStyle(MonytixTheme.text1)
                                Spacer()
                                Image(systemName: "chevron.right")
                                    .foregroundStyle(MonytixTheme.text2)
                            }
                            .padding(MonytixSpace.md)
                            .background(MonytixTheme.surface)
                            .clipShape(RoundedRectangle(cornerRadius: MonytixShape.mediumRadius))
                        }
                        .buttonStyle(.plain)

                        Text("Suggestions")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundStyle(MonytixTheme.text1)
                        VStack(spacing: MonytixSpace.sm) {
                            tipCard(
                                title: "Review your top spending category",
                                subtitle: "See where your money went this month."
                            )
                            tipCard(
                                title: "Check your forecast for the month",
                                subtitle: "Stay ahead with your financial future."
                            )
                            tipCard(
                                title: "Update your budget targets",
                                subtitle: "Keep State in sync with your goals."
                            )
                        }
                    }
                    .padding(MonytixSpace.md)
                }
            }
            .navigationTitle("Guidance")
            .navigationBarTitleDisplayMode(.inline)
            .toolbarBackground(MonytixTheme.bg, for: .navigationBar)
            .toolbarColorScheme(.dark, for: .navigationBar)
            .sheet(isPresented: $showAssistant) {
                AssistantSheetView(isPresented: $showAssistant)
            }
        }
    }

    private func tipCard(title: String, subtitle: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(MonytixTheme.text1)
            Text(subtitle)
                .font(.system(size: 13))
                .foregroundStyle(MonytixTheme.text2)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(MonytixSpace.md)
        .background(MonytixTheme.surface)
        .clipShape(RoundedRectangle(cornerRadius: MonytixShape.mediumRadius))
    }
}

#Preview {
    GuidanceView()
}
