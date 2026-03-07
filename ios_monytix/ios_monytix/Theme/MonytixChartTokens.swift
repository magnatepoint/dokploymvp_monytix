//
//  MonytixChartTokens.swift
//  ios_monytix
//
//  Created by santosh on 06/03/26.
//

import SwiftUI

// MARK: - AI-Grade Chart Tokens

enum MonytixChart {
    // MARK: Heatmap Colors
    static let heatLow  = MonytixTheme.cyan1.opacity(0.10)
    static let heatMid  = MonytixTheme.cyan1.opacity(0.22)
    static let heatHigh = MonytixTheme.cyan1.opacity(0.38)

    // MARK: Line Chart
    static let line     = MonytixTheme.cyan1
    static let line2    = Color(hex: "#2B3FFF") // secondary line
    static let marker   = MonytixTheme.cyan1.opacity(0.9)
    static let fill     = MonytixTheme.cyan1.opacity(0.15)

    // MARK: Grid & Axes
    static let grid     = MonytixTheme.stroke.opacity(0.6)
    static let axisText = MonytixTheme.text3
    static let axisLine = MonytixTheme.stroke.opacity(0.4)
}

// MARK: - Chart Typography

enum MonytixChartType {
    static let title = Font.system(size: 16, weight: .semibold)
    static let value = Font.system(size: 22, weight: .bold)
    static let label = Font.system(size: 12, weight: .medium)
    static let hint  = Font.system(size: 12, weight: .regular)
    static let largeValue = Font.system(size: 28, weight: .bold)
}

// MARK: - Chart Utilities

extension MonytixChart {
    /// Get a color from the chart palette by index
    static func categoryColor(at index: Int) -> Color {
        let colors: [Color] = [
            MonytixTheme.Chart.cyan,
            MonytixTheme.Chart.blue,
            MonytixTheme.Chart.purple,
            MonytixTheme.Chart.orange,
            MonytixTheme.Chart.green,
            MonytixTheme.Chart.yellow,
            MonytixTheme.Chart.red
        ]
        return colors[index % colors.count]
    }
    
    /// Get heatmap color based on intensity (0...1)
    static func heatmapColor(intensity: CGFloat) -> Color {
        switch intensity {
        case 0..<0.2:
            return heatLow
        case 0.2..<0.55:
            return heatMid
        default:
            return heatHigh
        }
    }
    
    /// Get gradient for positive/negative values
    static func deltaGradient(isPositive: Bool) -> LinearGradient {
        LinearGradient(
            colors: isPositive 
                ? [MonytixTheme.success.opacity(0.6), MonytixTheme.success]
                : [MonytixTheme.danger.opacity(0.6), MonytixTheme.danger],
            startPoint: .leading,
            endPoint: .trailing
        )
    }
}
