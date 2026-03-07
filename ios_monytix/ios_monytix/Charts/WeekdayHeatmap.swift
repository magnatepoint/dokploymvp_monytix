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
    /// Unique labels so ForEach(id: \.self) is well-defined (no duplicate T/S).
    private let dayLabels = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"]

    private struct CellId: Hashable {
        let w: Int
        let d: Int
    }

    var body: some View {
        let all = weeks.flatMap { $0 }
        let maxVal = max(all.max() ?? 1, 1)
        let cellIds: [CellId] = (0..<weeks.count).flatMap { w in (0..<7).map { d in CellId(w: w, d: d) } }

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
            
            // Day labels (unique strings for ForEach id)
            HStack(spacing: 8) {
                ForEach(dayLabels, id: \.self) { label in
                    Text(label)
                        .font(.system(size: 10, weight: .medium))
                        .foregroundStyle(MonytixTheme.text3)
                        .frame(maxWidth: .infinity)
                }
            }

            // Heatmap grid (one ForEach with unique (w,d) to avoid LazyVGrid duplicate ID warnings)
            LazyVGrid(columns: cols, spacing: 8) {
                ForEach(cellIds, id: \.self) { cell in
                    let w = cell.w, d = cell.d
                    let v = weeks[w][d]
                    let t = CGFloat(v / maxVal)
                    cellView(w: w, d: d, v: v, t: t)
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

    private func cellView(w: Int, d: Int, v: Double, t: CGFloat) -> some View {
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
                    }
                )
                .enter(delay: 0.1)
            }
            .padding()
        }
    }
}
