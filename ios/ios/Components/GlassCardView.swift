//
//  GlassCardView.swift
//  ios
//
//  Glass card component - matches APK GlassCard styling
//

import SwiftUI

struct GlassCardView<Content: View>: View {
    let content: () -> Content

    init(@ViewBuilder content: @escaping () -> Content) {
        self.content = content
    }

    var body: some View {
        content()
            .padding(MonytixLayout.cardPadding)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(MonytixColors.glassCard)
            .overlay(
                RoundedRectangle(cornerRadius: MonytixLayout.cardRadius)
                    .stroke(MonytixColors.textPrimary.opacity(0.12), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: MonytixLayout.cardRadius))
    }
}

#Preview {
    GlassCardView {
        Text("Card content")
            .foregroundColor(MonytixColors.textPrimary)
    }
}
