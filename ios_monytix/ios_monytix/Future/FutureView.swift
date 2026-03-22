//
//  FutureView.swift
//  ios_monytix
//
//  Financial Future (forecast) screen. Premium redesign killer screen.
//

import Combine
import SwiftUI

struct FutureView: View {
    @StateObject private var viewModel = FutureViewModel()
    var onUploadStatement: () -> Void = {}

    var body: some View {
        Group {
            if viewModel.isLoading && !viewModel.hasData {
                futureSkeleton
            } else if let err = viewModel.errorMessage, !viewModel.hasData {
                ZStack {
                    MonytixTheme.bg.ignoresSafeArea()
                    futureErrorState(message: err, onRetry: { viewModel.refresh() })
                }
            } else if !viewModel.hasData {
                ZStack {
                    MonytixTheme.bg.ignoresSafeArea()
                    emptyState
                }
            } else {
                ScrollView {
                    VStack(alignment: .leading, spacing: MonytixSpace.md) {
                        futureHero(message: viewModel.heroMessage, isRisk: viewModel.heroIsRisk)
                        Text(viewModel.confidenceLabel)
                            .font(.system(size: 13))
                            .foregroundStyle(MonytixTheme.text2)
                        forecastChartPlaceholder
                        if let label = viewModel.riskStripLabel {
                            riskStripCard(label: label, severity: viewModel.riskStripSeverity)
                        }
                        if let savings = viewModel.savingsOpportunity {
                            savingsCard(savings)
                        }
                        if !viewModel.recommendations.isEmpty {
                            Text("Recommendations")
                                .font(.system(size: 17, weight: .semibold))
                                .foregroundStyle(MonytixTheme.text1)
                            ForEach(viewModel.recommendations) { rec in
                                recommendationCard(rec)
                            }
                        }
                    }
                    .padding(.horizontal, MonytixSpace.lg)
                    .padding(.vertical, MonytixSpace.md)
                }
                .background(MonytixTheme.bg.ignoresSafeArea())
            }
        }
    }

    private var futureSkeleton: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: MonytixSpace.md) {
                Text("Building your forecast…")
                    .font(.system(size: 14))
                    .foregroundStyle(MonytixTheme.text2)
                RoundedRectangle(cornerRadius: 6)
                    .fill(MonytixTheme.stroke.opacity(0.4))
                    .frame(width: 180, height: 24)
                    .shimmer()
                RoundedRectangle(cornerRadius: 6)
                    .fill(MonytixTheme.stroke.opacity(0.4))
                    .frame(width: 140, height: 16)
                    .shimmer()
                RoundedRectangle(cornerRadius: MonytixShape.smallRadius)
                    .fill(MonytixTheme.stroke.opacity(0.4))
                    .frame(height: 160)
                    .shimmer()
                SkeletonCard()
                SkeletonCard()
            }
            .padding(.horizontal, MonytixSpace.lg)
            .padding(.vertical, MonytixSpace.md)
        }
        .background(MonytixTheme.bg.ignoresSafeArea())
    }

    private var emptyState: some View {
        VStack(spacing: MonytixSpace.lg) {
            Text("Your financial future")
                .font(.system(size: 17, weight: .semibold))
                .foregroundStyle(MonytixTheme.text1)
            Text("We need a bit more history to project cash flow.")
                .font(.system(size: 14))
                .foregroundStyle(MonytixTheme.text2)
                .multilineTextAlignment(.center)
            Button("Upload statement", action: onUploadStatement)
                .buttonStyle(.borderedProminent)
                .tint(MonytixTheme.cyan1)
        }
        .padding(MonytixSpace.xl)
    }

    private func futureErrorState(message: String, onRetry: @escaping () -> Void) -> some View {
        VStack(spacing: MonytixSpace.lg) {
            Text(message)
                .font(.system(size: 14))
                .foregroundStyle(MonytixTheme.text2)
                .multilineTextAlignment(.center)
            Button("Retry", action: onRetry)
                .buttonStyle(.borderedProminent)
                .tint(MonytixTheme.cyan1)
        }
        .padding(MonytixSpace.xl)
    }

    private func futureHero(message: String, isRisk: Bool) -> some View {
        let color = isRisk ? MonytixTheme.warn : MonytixTheme.cyan1
        return Text(message)
            .font(.system(size: 20, weight: .bold))
            .foregroundStyle(color)
    }

    private var forecastChartPlaceholder: some View {
        VStack(alignment: .leading, spacing: 8) {
            ForecastLineChartView(points: viewModel.projectionPoints, lowPointIndex: viewModel.lowPointDayIndex)
                .frame(height: 180)
                .background(MonytixTheme.surface)
                .clipShape(RoundedRectangle(cornerRadius: MonytixShape.smallRadius))
            HStack {
                Text("Today")
                    .font(.caption)
                    .foregroundStyle(MonytixTheme.text2)
                Spacer()
                Text("Day 15")
                    .font(.caption)
                    .foregroundStyle(MonytixTheme.text2)
                Spacer()
                Text("Day 30")
                    .font(.caption)
                    .foregroundStyle(MonytixTheme.text2)
            }
        }
    }

    private func riskStripCard(label: String, severity: String) -> some View {
        let tint: Color = severity == "warning" ? MonytixTheme.warn : (severity == "danger" ? MonytixTheme.danger : MonytixTheme.success)
        let displayLabel = label.hasPrefix("⚠") ? label : "⚠ \(label)"
        return Text(displayLabel)
            .font(.system(size: 14, weight: .medium))
            .foregroundStyle(tint)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(MonytixSpace.md)
            .background(tint.opacity(0.15))
            .clipShape(RoundedRectangle(cornerRadius: MonytixShape.smallRadius))
    }

    private func savingsCard(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 14))
            .foregroundStyle(MonytixTheme.text2)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(MonytixSpace.md)
            .background(MonytixTheme.surface2)
            .clipShape(RoundedRectangle(cornerRadius: MonytixShape.smallRadius))
    }

    private func recommendationCard(_ rec: FutureRecommendation) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(rec.title)
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(MonytixTheme.text1)
            Text(rec.body)
                .font(.system(size: 13))
                .foregroundStyle(MonytixTheme.text2)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(MonytixSpace.md)
        .background(MonytixTheme.surface)
        .clipShape(RoundedRectangle(cornerRadius: MonytixShape.smallRadius))
    }
}

// MARK: - Forecast line chart (normalized points 0–1, path animation)

private struct ForecastLineChartView: View {
    let points: [(CGFloat, CGFloat)]
    var lowPointIndex: Int? = nil
    @State private var drawProgress: CGFloat = 0

    var body: some View {
        GeometryReader { g in
            let w = g.size.width
            let h = g.size.height
            let padding: CGFloat = 8
            let chartW = w - 2 * padding
            let chartH = h - 2 * padding
            if points.count >= 2 {
                let xStep = chartW / CGFloat(points.count - 1)
                let linePath = linePath(padding: padding, chartW: chartW, chartH: chartH, xStep: xStep)
                let fillPath = fillPath(padding: padding, chartW: chartW, chartH: chartH, xStep: xStep)
                ZStack(alignment: .topLeading) {
                    // Today marker (vertical line at x=0)
                    Rectangle()
                        .fill(MonytixTheme.text2.opacity(0.5))
                        .frame(width: 1, height: chartH)
                        .offset(x: padding - 0.5, y: padding)
                    // Low point marker (dot + optional label)
                    if let lowIdx = lowPointIndex, lowIdx >= 0, lowIdx < points.count {
                        let lowX = padding + CGFloat(lowIdx) * xStep
                        let lowY = padding + (1 - points[lowIdx].1) * chartH
                        Circle()
                            .fill(MonytixTheme.warn)
                            .frame(width: 10, height: 10)
                            .position(x: lowX, y: lowY)
                        Text("Low")
                            .font(.system(size: 10, weight: .medium))
                            .foregroundStyle(MonytixTheme.warn)
                            .position(x: lowX, y: lowY - 12)
                    }
                    // Fill under line (masked by drawProgress for reveal)
                    fillPath
                        .fill(MonytixTheme.cyan1.opacity(0.2))
                        .mask(
                            HStack(spacing: 0) {
                                Rectangle()
                                    .frame(width: max(0, padding + chartW * drawProgress), height: h)
                                Spacer(minLength: 0)
                            }
                        )
                    // Line (trimmed path animation)
                    linePath
                        .trim(from: 0, to: drawProgress)
                        .stroke(MonytixTheme.cyan1, lineWidth: 2)
                }
                .onAppear {
                    withAnimation(.easeOut(duration: 1.0)) { drawProgress = 1 }
                }
                .onChange(of: points.count) {
                    drawProgress = 0
                    withAnimation(.easeOut(duration: 1.0)) { drawProgress = 1 }
                }
            } else {
                Text("Projected cash")
                    .font(.caption)
                    .foregroundStyle(MonytixTheme.text2)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
    }

    private func linePath(padding: CGFloat, chartW: CGFloat, chartH: CGFloat, xStep: CGFloat) -> Path {
        var path = Path()
        path.move(to: CGPoint(x: padding, y: padding + (1 - points[0].1) * chartH))
        for i in 1..<points.count {
            path.addLine(to: CGPoint(x: padding + CGFloat(i) * xStep, y: padding + (1 - points[i].1) * chartH))
        }
        return path
    }

    private func fillPath(padding: CGFloat, chartW: CGFloat, chartH: CGFloat, xStep: CGFloat) -> Path {
        var path = Path()
        path.move(to: CGPoint(x: padding, y: padding + (1 - points[0].1) * chartH))
        for i in 1..<points.count {
            path.addLine(to: CGPoint(x: padding + CGFloat(i) * xStep, y: padding + (1 - points[i].1) * chartH))
        }
        path.addLine(to: CGPoint(x: padding + CGFloat(points.count - 1) * xStep, y: padding + chartH))
        path.addLine(to: CGPoint(x: padding, y: padding + chartH))
        path.closeSubpath()
        return path
    }
}
