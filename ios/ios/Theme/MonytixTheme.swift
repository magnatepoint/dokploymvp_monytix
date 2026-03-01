//
//  MonytixTheme.swift
//  ios
//
//  Monytix theme - gradient background, preferred color scheme
//

import SwiftUI

struct MonytixThemeModifier: ViewModifier {
    func body(content: Content) -> some View {
        content
            .preferredColorScheme(.dark)
            .background(
                LinearGradient(
                    colors: [
                        MonytixColors.backgroundGradientTop,
                        MonytixColors.backgroundGradientBottom
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .ignoresSafeArea()
            )
    }
}

extension View {
    func monytixTheme() -> some View {
        modifier(MonytixThemeModifier())
    }
}

/// Full-screen gradient background for content that needs it
struct PremiumGradientBackground<Content: View>: View {
    let content: () -> Content

    init(@ViewBuilder content: @escaping () -> Content) {
        self.content = content
    }

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [
                    MonytixColors.backgroundGradientTop,
                    MonytixColors.backgroundGradientBottom
                ],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()

            content()
        }
        .preferredColorScheme(.dark)
    }
}
