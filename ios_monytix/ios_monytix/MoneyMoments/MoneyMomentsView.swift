//
//  MoneyMomentsView.swift
//  ios_monytix
//
//  Mirrors APK MoneyMomentsScreen: Nudges, Habits, AI Insights tabs.
//

import SwiftUI

enum MoneyMomentsTab: String, CaseIterable {
    case nudges = "Nudges"
    case habits = "Habits"
    case aiInsights = "AI Insights"

    var icon: String {
        switch self {
        case .nudges: return "bell.fill"
        case .habits: return "arrow.triangle.2.circlepath"
        case .aiInsights: return "lightbulb.fill"
        }
    }
}

struct MoneyMomentsView: View {
    @StateObject private var viewModel = MoneyMomentsViewModel()
    @State private var selectedTab: MoneyMomentsTab = .nudges

    var body: some View {
        NavigationStack {
            ZStack {
                MonytixTheme.bg.ignoresSafeArea()
                VStack(spacing: 0) {
                    welcomeBanner
                    tabBar
                    tabContent
                }
            }
            .navigationTitle("MoneyMoments")
            .navigationBarTitleDisplayMode(.inline)
            .toolbarBackground(MonytixTheme.bg, for: .navigationBar)
            .toolbarColorScheme(.dark, for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        viewModel.loadData()
                    } label: {
                        Image(systemName: "arrow.clockwise")
                            .foregroundStyle(MonytixTheme.text1)
                    }
                    .disabled(viewModel.isMomentsLoading || viewModel.isNudgesLoading)
                }
            }
            .refreshable {
                viewModel.loadData()
            }
            .alert("MoneyMoments", isPresented: Binding(
                get: { viewModel.actionError != nil },
                set: { if !$0 { viewModel.clearActionError() } }
            )) {
                Button("OK") { viewModel.clearActionError() }
            } message: {
                if let msg = viewModel.actionError { Text(msg) }
            }
        }
    }

    private var welcomeBanner: some View {
        let name = (viewModel.userEmail?.split(separator: "@").first).map(String.init) ?? "User"
        return VStack(alignment: .leading, spacing: 4) {
            Text("Gentle reminders for smarter habits.")
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(MonytixTheme.text1)
            Text("Smart nudges and personalized prompts. Welcome back, \(name)!")
                .font(.system(size: 13, weight: .regular))
                .foregroundStyle(MonytixTheme.text2)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, MonytixSpace.md)
        .padding(.vertical, MonytixSpace.sm)
        .background(MonytixTheme.surface.opacity(0.6))
    }

    private var tabBar: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: MonytixSpace.sm) {
                ForEach(MoneyMomentsTab.allCases, id: \.self) { tab in
                    let selected = tab == selectedTab
                    Button {
                        selectedTab = tab
                    } label: {
                        Label(tab.rawValue, systemImage: tab.icon)
                            .font(.system(size: 14, weight: selected ? .semibold : .medium))
                            .foregroundStyle(selected ? MonytixTheme.bg : MonytixTheme.text2)
                            .padding(.horizontal, MonytixSpace.md)
                            .padding(.vertical, MonytixSpace.sm)
                            .background(
                                RoundedRectangle(cornerRadius: MonytixShape.smallRadius)
                                    .fill(selected ? MonytixTheme.cyan1 : MonytixTheme.surface)
                            )
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, MonytixSpace.md)
            .padding(.vertical, MonytixSpace.sm)
        }
    }

    @ViewBuilder
    private var tabContent: some View {
        switch selectedTab {
        case .nudges:
            NudgesTabContent(viewModel: viewModel)
        case .habits:
            HabitsTabContent(viewModel: viewModel)
        case .aiInsights:
            AIInsightsTabContent(viewModel: viewModel)
        }
    }
}

// MARK: - Nudges Tab

private struct NudgesTabContent: View {
    @ObservedObject var viewModel: MoneyMomentsViewModel

    var body: some View {
        Group {
            if viewModel.isNudgesLoading && viewModel.nudges.isEmpty {
                Spacer()
                MonytixRingLoader(size: 44, lineWidth: 4)
                Spacer()
            } else if viewModel.nudgesError != nil && viewModel.nudges.isEmpty {
                emptyState(title: "Unable to Load Nudges", subtitle: viewModel.nudgesError ?? "", onRetry: { viewModel.loadData() })
            } else {
                nudgesScroll
            }
        }
    }

    private var nudgesScroll: some View {
        let metrics = viewModel.computeProgressMetrics()
        return ScrollView {
            LazyVStack(alignment: .leading, spacing: MonytixSpace.md) {
                Text("Your Progress")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(MonytixTheme.text1)
                HStack(spacing: 12) {
                    MetricCard(label: "Streak", value: "\(metrics.streak) days", color: MonytixTheme.danger)
                    MetricCard(label: "Nudges", value: "\(metrics.nudgesCount)", color: MonytixTheme.info)
                }
                HStack(spacing: 12) {
                    MetricCard(label: "Habits", value: "\(metrics.habitsCount)", color: MonytixTheme.success)
                    MetricCard(label: "Saved", value: mmFormatCurrency(metrics.savedAmount), color: MonytixTheme.cyan2)
                }
                Text("Active Nudges")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(MonytixTheme.text1)
                if viewModel.nudges.isEmpty {
                    nudgesEmptyState
                } else {
                    ForEach(viewModel.nudges) { nudge in
                        NudgeCardView(nudge: nudge, viewModel: viewModel)
                    }
                }
                Spacer(minLength: 24)
            }
            .padding(MonytixSpace.md)
        }
    }

    private var nudgesEmptyState: some View {
        VStack(spacing: MonytixSpace.md) {
            Text("🔔")
                .font(.system(size: 48))
            Text("Personalized nudges will appear here")
                .font(.system(size: 16, weight: .semibold))
                .foregroundStyle(MonytixTheme.text1)
            Text("Add data in SpendSense and we'll suggest nudges when rules match your behavior.")
                .font(.system(size: 14, weight: .regular))
                .foregroundStyle(MonytixTheme.text2)
                .multilineTextAlignment(.center)
            if let err = viewModel.actionError {
                Text(err)
                    .font(.system(size: 12, weight: .regular))
                    .foregroundStyle(MonytixTheme.danger)
            }
            Button {
                viewModel.evaluateAndDeliverNudges()
            } label: {
                if viewModel.isEvaluating || viewModel.isComputing {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: MonytixTheme.onAccent))
                    Text("Processing...")
                } else {
                    Text("Evaluate & Deliver Nudges")
                }
            }
            .font(.system(size: 14, weight: .semibold))
            .foregroundStyle(MonytixTheme.onAccent)
            .frame(maxWidth: .infinity)
            .padding(.vertical, MonytixSpace.sm)
            .background(MonytixTheme.cyan1, in: RoundedRectangle(cornerRadius: MonytixShape.buttonRadius))
            .disabled(viewModel.isEvaluating || viewModel.isComputing)
        }
        .padding(MonytixSpace.lg)
    }
}

// MARK: - Habits Tab

private struct HabitsTabContent: View {
    @ObservedObject var viewModel: MoneyMomentsViewModel

    var body: some View {
        Group {
            if viewModel.isMomentsLoading && viewModel.moments.isEmpty {
                Spacer()
                MonytixRingLoader(size: 44, lineWidth: 4)
                Spacer()
            } else if viewModel.momentsError != nil && viewModel.moments.isEmpty {
                emptyState(title: "Unable to Load Habits", subtitle: viewModel.momentsError ?? "", onRetry: { viewModel.loadData() })
            } else if viewModel.moments.isEmpty {
                habitsEmptyState
            } else {
                habitsScroll
            }
        }
    }

    private var habitsEmptyState: some View {
        VStack(spacing: MonytixSpace.md) {
            Spacer()
            Text("📊")
                .font(.system(size: 56))
            Text("Spending habits will show here")
                .font(.system(size: 18, weight: .bold))
                .foregroundStyle(MonytixTheme.text1)
            Text("Habits are derived from your moments. Run a compute to start tracking patterns.")
                .font(.system(size: 14, weight: .regular))
                .foregroundStyle(MonytixTheme.text2)
                .multilineTextAlignment(.center)
            if let err = viewModel.actionError {
                Text(err)
                    .font(.system(size: 12))
                    .foregroundStyle(MonytixTheme.danger)
            }
            Button {
                viewModel.computeMoments()
            } label: {
                if viewModel.isComputing {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: MonytixTheme.onAccent))
                    Text("Computing...")
                } else {
                    Text("Compute Moments for Past 12 Months")
                }
            }
            .font(.system(size: 14, weight: .semibold))
            .foregroundStyle(MonytixTheme.onAccent)
            .frame(maxWidth: .infinity)
            .padding(.vertical, MonytixSpace.sm)
            .background(MonytixTheme.cyan1, in: RoundedRectangle(cornerRadius: MonytixShape.buttonRadius))
            .disabled(viewModel.isComputing)
            Spacer()
        }
        .padding(MonytixSpace.xl)
    }

    private var habitsScroll: some View {
        let behaviorScore = viewModel.moments.isEmpty ? 0 : min(100, max(0, Int(viewModel.moments.compactMap { $0.confidence }.reduce(0, +) / Double(max(1, viewModel.moments.count)) * 100)))
        return ScrollView {
            LazyVStack(alignment: .leading, spacing: 12) {
                Text("Habits")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(MonytixTheme.text1)
                VStack(alignment: .leading, spacing: 4) {
                    Text("Behavior score")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(MonytixTheme.text2)
                    Text("\(behaviorScore) / 100")
                        .font(.system(size: 22, weight: .bold))
                        .foregroundStyle(MonytixTheme.cyan1)
                    Text("Based on \(viewModel.moments.count) moments")
                        .font(.system(size: 12, weight: .regular))
                        .foregroundStyle(MonytixTheme.text2)
                }
                .padding(MonytixSpace.md)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(MonytixTheme.surface, in: RoundedRectangle(cornerRadius: MonytixShape.cardRadius))
                ForEach(Array(viewModel.moments.enumerated()), id: \.offset) { index, moment in
                    let display = habitCardDisplay(for: moment)
                    MoneyMomentCardView(moment: moment, allMoments: viewModel.moments, display: display)
                }
                Spacer(minLength: 24)
            }
            .padding(MonytixSpace.md)
        }
    }
}

// MARK: - AI Insights Tab

private struct AIInsightsTabContent: View {
    @ObservedObject var viewModel: MoneyMomentsViewModel

    private var insights: [MMAiInsight] {
        var result: [MMAiInsight] = []
        let highConf = viewModel.moments.filter { ($0.confidence ?? 0) >= 0.7 }
        if !highConf.isEmpty {
            result.append(MMAiInsight(id: "1", type: "progress", message: "Great progress! You have \(highConf.count) well-established spending patterns.", icon: "🏆"))
        }
        if let m = viewModel.moments.first {
            let label = (m.label ?? "").lowercased()
            let habit = (m.habitId ?? "").replacingOccurrences(of: "_", with: " ")
            result.append(MMAiInsight(id: "2", type: "suggestion", message: "Based on your \(label), consider reviewing your \(habit).", icon: "💡"))
        }
        if !viewModel.nudges.isEmpty {
            result.append(MMAiInsight(id: "3", type: "milestone", message: "You've received \(viewModel.nudges.count) personalized recommendations. Keep up the great work!", icon: "🎯"))
        }
        if result.isEmpty {
            result.append(MMAiInsight(id: "0", type: "suggestion", message: "AI insights will appear here based on your spending patterns and habits.", icon: "✨"))
        }
        return result
    }

    var body: some View {
        Group {
            if viewModel.isMomentsLoading && viewModel.isNudgesLoading {
                Spacer()
                MonytixRingLoader(size: 44, lineWidth: 4)
                Spacer()
            } else {
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 12) {
                        Text("Recent Insights")
                            .font(.system(size: 18, weight: .bold))
                            .foregroundStyle(MonytixTheme.text1)
                        ForEach(insights, id: \.id) { insight in
                            HStack(alignment: .top, spacing: 8) {
                                Text(insight.icon)
                                    .font(.system(size: 24))
                                Text(insight.message)
                                    .font(.system(size: 14, weight: .regular))
                                    .foregroundStyle(MonytixTheme.text1)
                            }
                            .padding(MonytixSpace.md)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(MonytixTheme.surface, in: RoundedRectangle(cornerRadius: MonytixShape.cardRadius))
                        }
                        Spacer(minLength: 24)
                    }
                    .padding(MonytixSpace.md)
                }
            }
        }
    }
}

private struct MMAiInsight {
    let id: String
    let type: String
    let message: String
    let icon: String
}

// MARK: - Shared components

private struct MetricCard: View {
    let label: String
    let value: String
    let color: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.system(size: 12, weight: .regular))
                .foregroundStyle(MonytixTheme.text2)
            Text(value)
                .font(.system(size: 18, weight: .bold))
                .foregroundStyle(color)
        }
        .padding(MonytixSpace.md)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(MonytixTheme.surface, in: RoundedRectangle(cornerRadius: MonytixShape.smallRadius))
    }
}

private struct NudgeCardView: View {
    let nudge: Nudge
    @ObservedObject var viewModel: MoneyMomentsViewModel

    var body: some View {
        let title = nudge.title ?? nudge.titleTemplate ?? "Nudge"
        let bodyText = nudge.body ?? nudge.bodyTemplate
        let sentPrefix = (nudge.sentAt ?? "").prefix(10)
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("✨ \(nudge.ruleName ?? "")")
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(MonytixTheme.cyan1)
                Spacer()
                Text(String(sentPrefix))
                    .font(.system(size: 11, weight: .regular))
                    .foregroundStyle(MonytixTheme.text3)
            }
            Text(title)
                .font(.system(size: 15, weight: .bold))
                .foregroundStyle(MonytixTheme.text1)
            if let b = bodyText, !b.isEmpty {
                Text(b)
                    .font(.system(size: 13, weight: .regular))
                    .foregroundStyle(MonytixTheme.text2)
                    .lineLimit(4)
            }
            if let cta = nudge.ctaText, !cta.isEmpty {
                Button {
                    viewModel.logNudgeInteraction(deliveryId: nudge.deliveryId ?? "", eventType: "click")
                } label: {
                    Text(cta)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(MonytixTheme.onAccent)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8)
                }
                .background(MonytixTheme.cyan1, in: RoundedRectangle(cornerRadius: MonytixShape.smallRadius))
            }
        }
        .padding(MonytixSpace.md)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(MonytixTheme.surface, in: RoundedRectangle(cornerRadius: MonytixShape.cardRadius))
        .onAppear {
            if let id = nudge.deliveryId { viewModel.logNudgeInteraction(deliveryId: id, eventType: "view") }
        }
    }
}

// MARK: - Habit card display (from APK toHabitCardDisplay)

private enum HabitHealth {
    case healthy
    case moderate
    case needsAttention
}

private struct HabitCardDisplay {
    let health: HabitHealth
    let conversationalTitle: String
    let idealOrBenchmark: String?
    let progressBarRatio: CGFloat?
    let progressLabel: String?
    let insightLine: String
    let actionLine: String
    let icon: String
}

private func habitCardDisplay(for moment: MoneyMoment) -> HabitCardDisplay {
    let id = (moment.habitId ?? "").lowercased()
    let value = moment.value ?? 0
    let confidence = moment.confidence ?? 0
    let label = moment.label ?? ""
    let insightText = moment.insightText ?? ""
    let icon: String
    if id.contains("burn_rate") || id.contains("spend_to_income") { icon = "📈" }
    else if id.contains("micro") || id.contains("cash") { icon = "ℹ️" }
    else { icon = "⚠️" }

    if id.contains("burn_rate") || id.contains("early") {
        let ratio = CGFloat(min(1, max(0, value)))
        let health: HabitHealth = value > 0.75 ? .needsAttention : (value > 0.6 ? .moderate : .healthy)
        return HabitCardDisplay(
            health: health,
            conversationalTitle: "You spend early in the month",
            idealOrBenchmark: "Ideal: < 60%",
            progressBarRatio: ratio,
            progressLabel: "\(Int(value * 100))% by Day 15",
            insightLine: insightText.isEmpty ? "You are spending most of your monthly budget within the first half of the month." : insightText,
            actionLine: "Try limiting first-week spending to 30% of your budget.",
            icon: icon
        )
    }
    if id.contains("cash") || id.contains("cash_spend") {
        let health: HabitHealth = value <= 0.2 ? .healthy : (value <= 0.5 ? .moderate : .needsAttention)
        return HabitCardDisplay(
            health: health,
            conversationalTitle: "Cash vs digital mix",
            idealOrBenchmark: nil,
            progressBarRatio: CGFloat(min(1, max(0, value))),
            progressLabel: "Digital \(Int(value * 100))% | Cash \(100 - Int(value * 100))%",
            insightLine: insightText.isEmpty ? "You rely \(value >= 0.8 ? "fully" : "mostly") on digital payments." : insightText,
            actionLine: "Track small cash spends if any to improve accuracy.",
            icon: icon
        )
    }
    if id.contains("micro") {
        let health: HabitHealth = confidence >= 0.7 ? .healthy : (confidence >= 0.5 ? .moderate : .needsAttention)
        return HabitCardDisplay(
            health: health,
            conversationalTitle: "Micro-spending pattern",
            idealOrBenchmark: nil,
            progressBarRatio: nil,
            progressLabel: nil,
            insightLine: insightText.isEmpty ? "Frequent small transactions can silently increase monthly spend." : insightText,
            actionLine: "Set micro-spend alert above ₹3,000/month.",
            icon: icon
        )
    }
    let health: HabitHealth = confidence >= 0.7 ? .healthy : (confidence >= 0.5 ? .moderate : .needsAttention)
    let title = label.isEmpty ? (moment.habitId ?? "").replacingOccurrences(of: "_", with: " ").capitalized : label
    return HabitCardDisplay(
        health: health,
        conversationalTitle: title,
        idealOrBenchmark: nil,
        progressBarRatio: nil,
        progressLabel: nil,
        insightLine: insightText.isEmpty ? "Review this pattern for insights." : insightText,
        actionLine: "Review this pattern in SpendSense.",
        icon: icon
    )
}

private struct MoneyMomentCardView: View {
    let moment: MoneyMoment
    let allMoments: [MoneyMoment]
    let display: HabitCardDisplay
    @State private var expanded = false

    private var healthColor: Color {
        switch display.health {
        case .healthy: return MonytixTheme.success
        case .moderate: return MonytixTheme.warn
        case .needsAttention: return MonytixTheme.danger
        }
    }

    private var healthText: String {
        switch display.health {
        case .healthy: return "Healthy"
        case .moderate: return "Moderate"
        case .needsAttention: return "Needs attention"
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .center) {
                Text(display.icon)
                    .font(.system(size: 22))
                Text(display.conversationalTitle)
                    .font(.system(size: 15, weight: .bold))
                    .foregroundStyle(MonytixTheme.text1)
                    .lineLimit(2)
                Spacer()
                Text(healthText)
                    .font(.system(size: 11, weight: .medium))
                    .foregroundStyle(healthColor)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(healthColor.opacity(0.2), in: RoundedRectangle(cornerRadius: 8))
            }
            if let ratio = display.progressBarRatio, let progLabel = display.progressLabel {
                GeometryReader { g in
                    ZStack(alignment: .leading) {
                        RoundedRectangle(cornerRadius: 4)
                            .fill(MonytixTheme.stroke)
                            .frame(height: 8)
                        RoundedRectangle(cornerRadius: 4)
                            .fill(MonytixTheme.cyan1)
                            .frame(width: g.size.width * min(1, max(0, ratio)), height: 8)
                    }
                }
                .frame(height: 8)
                HStack {
                    Text(progLabel)
                        .font(.system(size: 11, weight: .regular))
                        .foregroundStyle(MonytixTheme.text2)
                    Spacer()
                    if let ideal = display.idealOrBenchmark {
                        Text(ideal)
                            .font(.system(size: 11, weight: .regular))
                            .foregroundStyle(MonytixTheme.text3)
                    }
                }
            }
            Text(display.insightLine)
                .font(.system(size: 13, weight: .regular))
                .foregroundStyle(MonytixTheme.text2)
            Text(display.actionLine)
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(MonytixTheme.cyan1)
            Button {
                expanded.toggle()
            } label: {
                HStack {
                    Text("View details")
                        .font(.system(size: 11, weight: .regular))
                        .foregroundStyle(MonytixTheme.text3)
                    Image(systemName: expanded ? "chevron.up" : "chevron.down")
                        .font(.system(size: 12))
                        .foregroundStyle(MonytixTheme.text3)
                }
            }
            if expanded {
                let sameHabit = allMoments
                    .filter { $0.habitId == moment.habitId }
                    .sorted { ($0.month ?? "") > ($1.month ?? "") }
                    .prefix(3)
                VStack(alignment: .leading, spacing: 4) {
                    Text("Last \(sameHabit.count) months")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(MonytixTheme.text2)
                    ForEach(Array(sameHabit.enumerated()), id: \.offset) { _, m in
                        let v: String = {
                            let h = (m.habitId ?? "").lowercased()
                            if h.contains("ratio") || h.contains("share") { return "\(Int((m.value ?? 0) * 100))%" }
                            if h.contains("count") { return "\(Int(m.value ?? 0))" }
                            return mmFormatCurrency(m.value ?? 0)
                        }()
                        HStack {
                            Text(m.month ?? "")
                                .font(.system(size: 12, weight: .regular))
                                .foregroundStyle(MonytixTheme.text2)
                            Spacer()
                            Text(v)
                                .font(.system(size: 12, weight: .medium))
                                .foregroundStyle(MonytixTheme.cyan1)
                        }
                    }
                }
            }
        }
        .padding(MonytixSpace.md)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(MonytixTheme.surface, in: RoundedRectangle(cornerRadius: MonytixShape.cardRadius))
        .onTapGesture {
            expanded.toggle()
        }
    }
}

private func emptyState(title: String, subtitle: String, onRetry: @escaping () -> Void) -> some View {
    VStack(spacing: MonytixSpace.md) {
        Spacer()
        Text(title)
            .font(.system(size: 18, weight: .bold))
            .foregroundStyle(MonytixTheme.text1)
        Text(subtitle)
            .font(.system(size: 14, weight: .regular))
            .foregroundStyle(MonytixTheme.text2)
            .multilineTextAlignment(.center)
        Button("Retry", action: onRetry)
            .font(.system(size: 14, weight: .semibold))
            .foregroundStyle(MonytixTheme.onAccent)
            .padding(.horizontal, MonytixSpace.lg)
            .padding(.vertical, MonytixSpace.sm)
            .background(MonytixTheme.cyan1, in: RoundedRectangle(cornerRadius: MonytixShape.buttonRadius))
        Spacer()
    }
    .padding(MonytixSpace.xl)
}

private func mmFormatCurrency(_ amount: Double) -> String {
    let absAmount = abs(amount)
    let formatter = NumberFormatter()
    formatter.numberStyle = .decimal
    formatter.maximumFractionDigits = 0
    let formatted = formatter.string(from: NSNumber(value: absAmount)) ?? "\(Int(absAmount))"
    return amount < 0 ? "-₹\(formatted)" : "₹\(formatted)"
}

#Preview {
    MoneyMomentsView()
}
