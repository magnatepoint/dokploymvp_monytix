//
//  RankedBars.swift
//  ios_monytix
//
//  Created by santosh on 06/03/26.
//

import SwiftUI

/// AI-grade ranked bars showing category spending with delta comparison
struct RankedBars: View {
    struct Row: Identifiable {
        let id = UUID()
        let name: String
        let amount: Double
        let percent: Double // 0..1
        let delta: Double   // vs last month, -0.2..+0.2 etc
        let icon: String?   // SF Symbol name
        
        init(name: String, amount: Double, percent: Double, delta: Double, icon: String? = nil) {
            self.name = name
            self.amount = amount
            self.percent = percent
            self.delta = delta
            self.icon = icon
        }
    }

    let rows: [Row]
    let onTap: ((Row) -> Void)?

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            // Header
            HStack {
                Text("Top categories")
                    .font(MonytixChartType.title)
                    .foregroundStyle(MonytixTheme.text1)
                Spacer()
                Button {
                    // View all action
                } label: {
                    Text("View all")
                        .font(MonytixChartType.label)
                        .foregroundStyle(MonytixTheme.cyan1)
                }
            }

            // Ranked rows
            ForEach(Array(rows.enumerated()), id: \.element.id) { index, row in
                Button {
                    onTap?(row)
                } label: {
                    VStack(alignment: .leading, spacing: 8) {
                        // Name and amount
                        HStack {
                            HStack(spacing: 8) {
                                if let icon = row.icon {
                                    Image(systemName: icon)
                                        .font(.system(size: 14, weight: .semibold))
                                        .foregroundStyle(MonytixChart.categoryColor(at: index))
                                }
                                Text(row.name)
                                    .foregroundStyle(MonytixTheme.text1)
                                    .font(.system(size: 14, weight: .semibold))
                            }
                            Spacer()
                            Text(formatINR(row.amount))
                                .foregroundStyle(MonytixTheme.text2)
                                .font(.system(size: 13, weight: .medium))
                        }

                        // Progress bar
                        GeometryReader { geo in
                            let w = geo.size.width
                            ZStack(alignment: .leading) {
                                Capsule()
                                    .fill(MonytixTheme.stroke.opacity(0.4))
                                    .frame(height: 8)
                                
                                Capsule()
                                    .fill(
                                        LinearGradient(
                                            colors: [
                                                MonytixChart.categoryColor(at: index),
                                                MonytixChart.categoryColor(at: index).opacity(0.7)
                                            ],
                                            startPoint: .leading,
                                            endPoint: .trailing
                                        )
                                    )
                                    .frame(width: max(12, w * row.percent), height: 8)
                            }
                        }
                        .frame(height: 8)

                        // Percent and delta
                        HStack {
                            Text("\(Int(row.percent * 100))%")
                                .foregroundStyle(MonytixTheme.text3)
                                .font(.system(size: 12))
                            Spacer()
                            HStack(spacing: 4) {
                                Image(systemName: row.delta >= 0 ? "arrow.up.right" : "arrow.down.right")
                                    .font(.system(size: 10, weight: .bold))
                                Text(deltaText(row.delta))
                                    .font(.system(size: 12, weight: .medium))
                            }
                            .foregroundStyle(row.delta >= 0 ? MonytixTheme.warn : MonytixTheme.success)
                        }
                    }
                    .padding(14)
                    .background(MonytixTheme.surface2)
                    .clipShape(RoundedRectangle(cornerRadius: 18))
                    .overlay(
                        RoundedRectangle(cornerRadius: 18)
                            .stroke(MonytixTheme.stroke.opacity(0.55), lineWidth: 1)
                    )
                }
                .buttonStyle(.plain)
                .pressScale()
                .enter(delay: Double(index) * 0.05)
            }
        }
        .padding(MonytixSpace.md)
        .background(MonytixTheme.surface)
        .clipShape(RoundedRectangle(cornerRadius: MonytixShape.cardRadius))
        .overlay(
            RoundedRectangle(cornerRadius: MonytixShape.cardRadius)
                .stroke(MonytixTheme.stroke.opacity(0.6), lineWidth: 1)
        )
    }

    private func deltaText(_ d: Double) -> String {
        let pct = Int(abs(d) * 100)
        return "\(pct)% vs last month"
    }

    private func formatINR(_ v: Double) -> String {
        let f = NumberFormatter()
        f.numberStyle = .currency
        f.currencyCode = "INR"
        f.maximumFractionDigits = 0
        return f.string(from: NSNumber(value: v)) ?? "₹0"
    }
}

// MARK: - Preview

#Preview {
    ZStack {
        MonytixTheme.bg.ignoresSafeArea()
        
        ScrollView {
            VStack(spacing: 20) {
                RankedBars(
                    rows: [
                        .init(name: "Food & Dining", amount: 12500, percent: 0.85, delta: 0.15, icon: "fork.knife"),
                        .init(name: "Transportation", amount: 8200, percent: 0.62, delta: -0.08, icon: "car.fill"),
                        .init(name: "Shopping", amount: 6800, percent: 0.48, delta: 0.22, icon: "bag.fill"),
                        .init(name: "Entertainment", amount: 4500, percent: 0.32, delta: -0.12, icon: "tv.fill"),
                        .init(name: "Bills & Utilities", amount: 3200, percent: 0.25, delta: 0.05, icon: "bolt.fill")
                    ],
                    onTap: { row in
                    }
                )
                .enter(delay: 0.1)
            }
            .padding()
        }
    }
}
