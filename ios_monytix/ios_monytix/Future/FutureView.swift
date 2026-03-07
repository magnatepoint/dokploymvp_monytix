//
//  FutureView.swift
//  ios_monytix
//
//  Financial Future (forecast) screen. Premium redesign killer screen.
//

import SwiftUI

struct FutureView: View {
    @StateObject private var viewModel = FutureViewModel()
    var onUploadStatement: () -> Void = {}

    var body: some View {
        Group {
            if viewModel.isLoading && !viewModel.hasData {
                futureSkeleton
            } else if !viewModel.hasData {
                ZStack {
                    MonytixTheme.bg.ignoresSafeArea()
                    emptyState
                }
            } else {
                ScrollView {
                    VStack(alignment: .leading, spacing: MonytixSpace.md) {
                        Text("Financial Future")
                            .font(.system(size: 22, weight: .bold))
                            .foregroundStyle(MonytixTheme.text1)
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

    private var forecastChartPlaceholder: some View {
        VStack(alignment: .leading, spacing: 8) {
            ForecastLineChartView(points: viewModel.projectionPoints)
                .frame(height: 160)
                .background(MonytixTheme.surface)
                .clipShape(RoundedRectangle(cornerRadius: MonytixShape.smallRadius))
        }
    }

    private func riskStripCard(label: String, severity: String) -> some View {
        let tint: Color = severity == "warning" ? MonytixTheme.warn : (severity == "danger" ? MonytixTheme.danger : MonytixTheme.success)
        return Text(label)
            .font(.system(size: 13, weight: .medium))
            .foregroundStyle(tint)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(MonytixSpace.sm)
            .background(tint.opacity(0.15))
            .clipShape(RoundedRectangle(cornerRadius: 12))
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
