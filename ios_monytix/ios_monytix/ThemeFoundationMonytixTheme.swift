//
//  MonytixTheme.swift
//  ios_monytix
//
//  Created by santosh on 06/03/26.
//

import SwiftUI

// MARK: - Core Theme Tokens

enum MonytixTheme {
    // MARK: Backgrounds
    static let bg        = Color(hex: "#070A12")
    static let surface   = Color(hex: "#0D1220")
    static let surface2  = Color(hex: "#111A2E")
    static let stroke    = Color(hex: "#23304A")

    // MARK: Text
    static let text1     = Color(hex: "#EAF0FF")
    static let text2     = Color(hex: "#9AA7C0")
    static let text3     = Color(hex: "#6E7A98")
    static let onAccent  = Color(hex: "#0B1220")

    // MARK: Brand (AI Cyan)
    static let cyan1     = Color(hex: "#00D4FF")
    static let cyan2     = Color(hex: "#00A3FF")
    static let cyanGlow  = Color(hex: "#00D4FF").opacity(0.18)
    static let accentBlue = Color(hex: "#2B3FFF")
    static let highlightCyan = Color(hex: "#5BE7FF")

    // MARK: Status
    static let success   = Color(hex: "#2ED573")
    static let warn      = Color(hex: "#FF9F43")
    static let danger    = Color(hex: "#FF4D4F")
    static let info      = Color(hex: "#00C2FF")
    
    // MARK: Chart Palette
    enum Chart {
        static let cyan   = Color(hex: "#00D4FF")
        static let blue   = Color(hex: "#1E90FF")
        static let purple = Color(hex: "#6C5CE7")
        static let orange = Color(hex: "#FF7A00")
        static let green  = Color(hex: "#2ED573")
        static let yellow = Color(hex: "#F7C948")
        static let red    = Color(hex: "#FF4D4F")
    }
}

// MARK: - Gradients

extension MonytixTheme {
    static let gradientPrimary = LinearGradient(
        colors: [cyan1, cyan2],
        startPoint: .leading,
        endPoint: .trailing
    )

    static let gradientGlow = RadialGradient(
        colors: [cyan1.opacity(0.22), .clear],
        center: .center,
        startRadius: 10,
        endRadius: 220
    )
    
    static let gradientCard = LinearGradient(
        colors: [surface, surface2],
        startPoint: .topLeading,
        endPoint: .bottomTrailing
    )
}

// MARK: - Shape Tokens

enum MonytixShape {
    static let cardRadius: CGFloat = 24
    static let inputRadius: CGFloat = 18
    static let buttonRadius: CGFloat = 20
    static let smallRadius: CGFloat = 16
    static let modalRadius: CGFloat = 24
}

// MARK: - Spacing Tokens

enum MonytixSpace {
    static let xs: CGFloat = 6
    static let sm: CGFloat = 10
    static let md: CGFloat = 16
    static let lg: CGFloat = 24
    static let xl: CGFloat = 32
    static let xxl: CGFloat = 40
}

// MARK: - Color Extension (Hex Support)

extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let r = Double((int >> 16) & 0xFF) / 255
        let g = Double((int >> 8) & 0xFF) / 255
        let b = Double(int & 0xFF) / 255
        self.init(.sRGB, red: r, green: g, blue: b, opacity: 1)
    }
}

// MARK: - Legacy Support (for backwards compatibility)

extension Color {
    // Backgrounds
    static let appBackground = MonytixTheme.bg
    static let surface = MonytixTheme.surface
    static let elevated = MonytixTheme.surface2

    // Text
    static let textPrimary = MonytixTheme.text1
    static let textSecondary = MonytixTheme.text2

    // Brand
    static let primaryCyan = MonytixTheme.cyan1
    static let secondaryCyan = MonytixTheme.cyan2

    // States
    static let success = MonytixTheme.success
    static let warning = MonytixTheme.warn
    static let error = MonytixTheme.danger
}
