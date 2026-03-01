//
//  GlassCardView.swift
//  ios
//
//  Glass morphism card component
//

import SwiftUI

/// A card view with glass morphism effect
struct GlassCardView<Content: View>: View {
    let content: () -> Content
    
    init(@ViewBuilder content: @escaping () -> Content) {
        self.content = content
    }
    
    var body: some View {
        content()
            .padding(MonytixLayout.cardPadding)
            .background(MonytixColors.glassCard)
            .clipShape(RoundedRectangle(cornerRadius: MonytixLayout.cardRadius))
            .overlay(
                RoundedRectangle(cornerRadius: MonytixLayout.cardRadius)
                    .stroke(MonytixColors.border, lineWidth: MonytixLayout.borderThin)
            )
    }
}

#Preview {
    ZStack {
        Color.black.ignoresSafeArea()
        GlassCardView {
            VStack(alignment: .leading, spacing: 8) {
                Text("Card Title")
                    .font(MonytixTypography.titleMedium)
                    .foregroundColor(.white)
                Text("Card content goes here")
                    .font(MonytixTypography.bodyMedium)
                    .foregroundColor(.white.opacity(0.7))
            }
        }
        .padding()
    }
}
