//
//  WeekdayHeatmap.swift
//  ios_monytix
//
//  Created by santosh on 06/03/26.
//

import SwiftUI

/// AI-grade heatmap showing spending patterns across weeks and weekdays
struct WeekdayHeatmap: View {
    // 5 rows (weeks) × 7 cols (days). Value = spend amount.
    let weeks: [[Double]]
    let onTap: ((Int, Int, Double) -> Void)? // (weekIndex, dayIndex, value)

    private let cols = Array(repeating: GridItem(.flexible(), spacing: 8), count: 7)
    private let dayLabels = ["M", "T", "W", "T", "F", "S", "S"]

    var body: some View {
        let all = weeks.flatMap { $0 }
        let maxVal = max(all.max() ?? 1, 1)

        VStack(alignment: .leading, spacing: 12) {
            // Header
            HStack {
                Text("Spending patterns")
                    .font(MonytixChartType.title)
                    .foregroundStyle(MonytixTheme.text1)
                Spacer()
                Text("Mon → Sun")
                    .font(MonytixChartType.hint)
                    .foregroundStyle(MonytixTheme.text3)
            }
            
            // Day labels
            HStack(spacing: 8) {
                ForEach(dayLabels, id: \.self) { label in
                    Text(label)
                        .font(.system(size: 10, weight: .medium))
                        .foregroundStyle(MonytixTheme.text3)
                        .frame(maxWidth: .infinity)
                }
            }

            // Heatmap grid
            LazyVGrid(columns: cols, spacing: 8) {
                ForEach(0..<weeks.count, id: \.self) { w in
                    ForEach(0..<7, id: \.self) { d in
                        let v = weeks[w][d]
                        let t = CGFloat(v / maxVal) // 0..1 intensity
                        
                        RoundedRectangle(cornerRadius: 10)
                            .fill(colorForIntensity(t))
                            .frame(height: 28)
                            .overlay(
                                RoundedRectangle(cornerRadius: 10)
                                    .stroke(MonytixTheme.stroke.opacity(0.5), lineWidth: 1)
                            )
                            .onTapGesture {
                                onTap?(w, d, v)
                            }
                    }
                }
            }

            // Footer hint
            Text("Tap a day to see transactions")
                .font(MonytixChartType.hint)
                .foregroundStyle(MonytixTheme.text3)
        }
        .padding(MonytixSpace.md)
        .background(MonytixTheme.surface)
        .clipShape(RoundedRectangle(cornerRadius: MonytixShape.cardRadius))
        .overlay(
            RoundedRectangle(cornerRadius: MonytixShape.cardRadius)
                .stroke(MonytixTheme.stroke.opacity(0.6), lineWidth: 1)
        )
    }

    private func colorForIntensity(_ t: CGFloat) -> Color {
        MonytixChart.heatmapColor(intensity: t)
    }
}

// MARK: - Preview

#Preview {
    ZStack {
        MonytixTheme.bg.ignoresSafeArea()
        
        ScrollView {
            VStack(spacing: 20) {
                WeekdayHeatmap(
                    weeks: [
                        [1200, 0, 340, 800, 120, 4500, 1800],
                        [900, 110, 600, 0, 250, 3200, 900],
                        [0, 400, 700, 300, 120, 2800, 1400],
                        [800, 0, 900, 350, 0, 2100, 500],
                        [0, 0, 0, 0, 0, 0, 0]
                    ],
                    onTap: { week, day, value in
                        print("Week \(week + 1), Day \(day + 1): ₹\(value)")
                    }
                )
                .enter(delay: 0.1)
            }
            .padding()
        }
    }
}
