//
//  MonytixColors.swift
//  ios
//
//  Monytix color palette - matches APK MonytixColors.kt
//

import SwiftUI

enum MonytixColors {
    // MARK: - Primary Brand Colors
    static let accentPrimary = Color(hex: 0x14B8A6)
    static let accentSecondary = Color(hex: 0xA78BFA)

    // MARK: - Background Colors
    static let backgroundGradientTop = Color(hex: 0x0D0D0F)
    static let backgroundGradientBottom = Color(hex: 0x08080A)
    static let backgroundCard = Color(hex: 0x0D0D0F)
    static let backgroundSurface = Color(hex: 0x161618)
    static let surfaceDark = Color(hex: 0x0D0D0F)
    static let surfaceElevated = Color(hex: 0x161618)

    // MARK: - Text Colors
    static let textPrimary = Color(hex: 0xE5E7EB)
    static let textSecondary = Color(hex: 0x9CA3AF)
    static let textTertiary = Color.white.opacity(0.5)
    static let textDisabled = Color.white.opacity(0.3)
    static let textMuted = Color(hex: 0x6B7280)

    // MARK: - Semantic Colors
    static let success = Color(hex: 0x34D399)
    static let error = Color(hex: 0xF472B6)
    static let warning = Color(hex: 0xFBBF24)
    static let info = Color(hex: 0x38BDF8)

    // MARK: - UI Element Colors
    static let divider = Color.white.opacity(0.1)
    static let border = Color.white.opacity(0.2)
    static let overlay = Color.black.opacity(0.5)

    // MARK: - Button Colors
    static let buttonPrimary = accentPrimary
    static let buttonSecondary = Color.white.opacity(0.1)
    static let buttonDisabled = Color.white.opacity(0.05)

    // MARK: - Input Colors
    static let inputBackground = Color.white.opacity(0.05)
    static let inputBorder = Color.white.opacity(0.2)
    static let inputFocused = accentPrimary
    static let inputError = error

    // MARK: - Chart/Graph Colors (matches APK)
    static let chartPrimary = accentPrimary
    static let chartSecondary = accentSecondary
    static let chartTertiary = Color(hex: 0xFB923C)
    static let chartQuaternary = Color(hex: 0x34D399)
    static let chartRed = Color(hex: 0xF472B6)
    static let chartBlue = Color(hex: 0x38BDF8)
    static let chartOrange = Color(hex: 0xFB923C)
    static let chartGreen = Color(hex: 0x34D399)
    static let chartPurple = Color(hex: 0xA78BFA)

    // MARK: - Glass Effect Colors
    static let glassCard = Color(hex: 0xFFFFFF, opacity: 8.0 / 255.0)
}
