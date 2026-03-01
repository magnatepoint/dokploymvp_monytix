//
//  MonytixLayout.swift
//  ios
//
//  Monytix layout constants - matches APK Dimen.kt
//

import SwiftUI

enum MonytixLayout {
    // MARK: - Spacing
    static let spacingXXS: CGFloat = 4
    static let spacingXS: CGFloat = 8
    static let spacingS: CGFloat = 12
    static let spacingM: CGFloat = 16
    static let spacingL: CGFloat = 24
    static let spacingXL: CGFloat = 32
    static let spacingXXL: CGFloat = 48
    
    // MARK: - Padding
    static let paddingXS: CGFloat = 8
    static let paddingS: CGFloat = 12
    static let paddingM: CGFloat = 16
    static let paddingL: CGFloat = 24
    static let paddingXL: CGFloat = 32
    
    // MARK: - Corner Radius
    static let cornerRadiusXS: CGFloat = 4
    static let cornerRadiusS: CGFloat = 8
    static let cornerRadiusM: CGFloat = 12
    static let cornerRadiusL: CGFloat = 16
    static let cornerRadiusXL: CGFloat = 24
    static let cornerRadiusXXL: CGFloat = 32
    static let cardRadius: CGFloat = 16 // Alias for card corner radius
    
    // MARK: - Border Width
    static let borderThin: CGFloat = 1
    static let borderMedium: CGFloat = 2
    static let borderThick: CGFloat = 3
    
    // MARK: - Icon Sizes
    static let iconXS: CGFloat = 16
    static let iconS: CGFloat = 20
    static let iconM: CGFloat = 24
    static let iconL: CGFloat = 32
    static let iconXL: CGFloat = 48
    
    // MARK: - Button Heights
    static let buttonHeightS: CGFloat = 36
    static let buttonHeightM: CGFloat = 44
    static let buttonHeightL: CGFloat = 52
    
    // MARK: - Input Heights
    static let inputHeightS: CGFloat = 40
    static let inputHeightM: CGFloat = 48
    static let inputHeightL: CGFloat = 56
    
    // MARK: - Card Sizes
    static let cardMinHeight: CGFloat = 100
    static let cardMaxWidth: CGFloat = 600
    static let cardPadding: CGFloat = 16 // Standard padding inside cards
}
