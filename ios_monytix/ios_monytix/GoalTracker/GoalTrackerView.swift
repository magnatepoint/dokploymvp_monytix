//
//  GoalTrackerView.swift
//  ios_monytix
//
//  Mirrors APK GoalTrackerScreen: Overview / Goals / AI Insights tabs + goal detail + CRUD.
//

import SwiftUI

private enum GtTab: String, CaseIterable {
    case overview = "Overview"
    case goals = "Goals"
    case aiInsights = "AI Insights"

    var icon: String {
        switch self {
        case .overview: return "chart.bar.fill"
        case .goals: return "target"
        case .aiInsights: return "sparkles"
        }
    }
}

struct GoalTrackerView: View {
    @StateObject private var viewModel = GoalTrackerViewModel()
    @State private var selectedTab: GtTab = .overview
    @State private var showAddGoal = false
    @State private var selectedGoal: GoalResponse?
    @State private var showEditGoal: GoalResponse?
    @State private var showDeleteGoal: GoalResponse?

    var body: some View {
        NavigationStack {
            ZStack {
                MonytixTheme.bg.ignoresSafeArea()

                VStack(spacing: 0) {
                    welcomeBanner
                    lastUpdatedLine
                    tabBar
                    tabContent
                }
            }
            .navigationTitle(selectedGoal?.goalName ?? "GoalTracker")
            .navigationBarTitleDisplayMode(.inline)
            .toolbarBackground(MonytixTheme.bg, for: .navigationBar)
            .toolbarColorScheme(.dark, for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    HStack(spacing: 10) {
                        Button {
                            showAddGoal = true
                        } label: {
                            Image(systemName: "plus.circle")
                        }
                        Button {
                            viewModel.refresh()
                        } label: {
                            Image(systemName: "arrow.clockwise")
                        }
                    }
                }
            }
            .sheet(isPresented: $showAddGoal) {
                AddGoalSheet(viewModel: viewModel, isPresented: $showAddGoal)
                    .presentationDetents([.large])
            }
            .sheet(item: $showEditGoal) { goal in
                EditGoalSheet(viewModel: viewModel, goal: goal)
                    .presentationDetents([.large])
            }
            .alert("Delete goal?", isPresented: Binding(get: {
                showDeleteGoal != nil
            }, set: { v in
                if !v { showDeleteGoal = nil }
            })) {
                Button("Cancel", role: .cancel) { showDeleteGoal = nil }
                Button("Delete", role: .destructive) {
                    if let g = showDeleteGoal {
                        viewModel.deleteGoal(goalId: g.goalId)
                    }
                    showDeleteGoal = nil
                }
            } message: {
                Text(showDeleteGoal?.goalName ?? "")
            }
            .navigationDestination(item: $selectedGoal) { goal in
                GoalDetailView(
                    goal: goal,
                    progress: viewModel.progress.first { $0.goalId == goal.goalId },
                    viewModel: viewModel,
                    onEdit: { showEditGoal = goal },
                    onDelete: { showDeleteGoal = goal }
                )
            }
            .refreshable {
                viewModel.refresh()
            }
        }
    }

    private var welcomeBanner: some View {
        let displayName = (viewModel.userEmail?.split(separator: "@").first).map(String.init) ?? "User"
        return VStack(alignment: .leading, spacing: 4) {
            Text("Track your financial goals.")
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(MonytixTheme.text1)
            Text("Set targets, monitor progress, and achieve more. Welcome back, \(displayName)!")
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(MonytixTheme.text2)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, MonytixSpace.md)
        .padding(.vertical, MonytixSpace.md)
        .background(MonytixTheme.surface.opacity(0.7))
    }

    @ViewBuilder
    private var lastUpdatedLine: some View {
        if let t = viewModel.lastSyncTime {
            let mins = Int(Date().timeIntervalSince(t) / 60.0)
            let label: String = {
                if mins < 1 { return "Just now" }
                if mins == 1 { return "1 min ago" }
                if mins < 60 { return "\(mins) mins ago" }
                return "\(mins / 60) hrs ago"
            }()
            Text("Last updated: \(label) • From transactions sync")
                .font(.system(size: 11, weight: .medium))
                .foregroundStyle(MonytixTheme.text3)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, MonytixSpace.md)
                .padding(.vertical, 6)
        }
    }

    private var tabBar: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(GtTab.allCases, id: \.self) { tab in
                    let selected = tab == selectedTab
                    Button {
                        selectedTab = tab
                    } label: {
                        HStack(spacing: 6) {
                            Image(systemName: tab.icon)
                                .font(.system(size: 12, weight: .semibold))
                            Text(tab.rawValue)
                                .font(.system(size: 12, weight: selected ? .semibold : .medium))
                        }
                        .foregroundStyle(selected ? MonytixTheme.bg : MonytixTheme.text2)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 10)
                        .background(
                            RoundedRectangle(cornerRadius: 12)
                                .fill(selected ? MonytixTheme.cyan1 : Color.clear)
                                .background(
                                    RoundedRectangle(cornerRadius: 12)
                                        .fill(selected ? Color.clear : MonytixTheme.surface)
                                )
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
        if viewModel.isLoading && viewModel.goals.isEmpty {
            Spacer()
            MonytixRingLoader(size: 44, lineWidth: 4)
            Spacer()
        } else if let err = viewModel.errorMessage, viewModel.goals.isEmpty {
            VStack(spacing: 10) {
                Text("Unable to Load Data")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(MonytixTheme.text1)
                Text(err)
                    .font(.system(size: 13))
                    .foregroundStyle(MonytixTheme.text2)
                    .multilineTextAlignment(.center)
                Button("Retry") { viewModel.loadData() }
                    .buttonStyle(.borderedProminent)
                    .tint(MonytixTheme.cyan1)
            }
            .padding(.top, 40)
            .padding(.horizontal, MonytixSpace.lg)
            Spacer()
        } else {
            switch selectedTab {
            case .overview:
                GoalTrackerOverviewTab(viewModel: viewModel) { g in
                    selectedGoal = g
                }
            case .goals:
                GoalTrackerGoalsTab(viewModel: viewModel) { g in
                    selectedGoal = g
                }
            case .aiInsights:
                GoalTrackerAIInsightsTab(viewModel: viewModel)
            }
        }
    }
}

// MARK: - Overview tab

private struct GoalTrackerOverviewTab: View {
    @ObservedObject var viewModel: GoalTrackerViewModel
    let onGoalTap: (GoalResponse) -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: MonytixSpace.lg) {
                if let err = viewModel.errorMessage, !viewModel.goals.isEmpty {
                    Text(err)
                        .font(.caption)
                        .foregroundStyle(MonytixTheme.warn)
                        .lineLimit(3)
                }

                if viewModel.goals.isEmpty {
                    noGoalsEmptyState
                } else {
                    healthCard
                    if !viewModel.upcomingDeadlines().isEmpty {
                        Text("Upcoming Deadlines")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundStyle(MonytixTheme.warn)
                        ForEach(viewModel.upcomingDeadlines(), id: \.0.goalId) { (goal, prog) in
                            deadlineCard(goal: goal, progress: prog)
                                .onTapGesture { onGoalTap(goal) }
                        }
                    }
                    Text("Active Goals")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundStyle(MonytixTheme.text1)
                    let activeGoals = viewModel.goals.filter { $0.status.lowercased() == "active" }
                    if activeGoals.isEmpty {
                        Text("No Active Goals")
                            .font(.system(size: 14, weight: .medium))
                            .foregroundStyle(MonytixTheme.text2)
                    } else {
                        ForEach(activeGoals, id: \.goalId) { g in
                            goalCard(goal: g, progress: viewModel.progress.first { $0.goalId == g.goalId })
                                .onTapGesture { onGoalTap(g) }
                        }
                    }
                }
            }
            .padding(MonytixSpace.md)
            .padding(.bottom, MonytixSpace.xl)
        }
    }

    private var noGoalsEmptyState: some View {
        VStack(spacing: MonytixSpace.md) {
            Text("🎯")
                .font(.system(size: 44))
            Text("Turn intentions into goals")
                .font(.system(size: 18, weight: .semibold))
                .foregroundStyle(MonytixTheme.text1)
            Text("Set a target and we'll track progress and suggest a plan.")
                .font(.system(size: 14))
                .foregroundStyle(MonytixTheme.text2)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.top, 40)
    }

    private var healthCard: some View {
        let h = viewModel.goalHealthSummary()
        return VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("Goal health")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(MonytixTheme.text1)
                Spacer()
                Text("\(h.score)")
                    .font(.system(size: 24, weight: .bold))
                    .foregroundStyle(MonytixTheme.cyan1)
            }
            Text("\(h.onTrack) on track • \(h.atRisk) at risk")
                .font(.system(size: 12))
                .foregroundStyle(MonytixTheme.text2)
            GeometryReader { g in
                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 6)
                        .fill(MonytixTheme.stroke.opacity(0.4))
                        .frame(height: 10)
                    RoundedRectangle(cornerRadius: 6)
                        .fill(MonytixTheme.cyan1)
                        .frame(width: g.size.width * CGFloat(h.score) / 100, height: 10)
                }
            }
            .frame(height: 10)
        }
        .padding(MonytixSpace.md)
        .background(MonytixTheme.surface)
        .clipShape(RoundedRectangle(cornerRadius: MonytixShape.cardRadius))
    }

    private func deadlineCard(goal: GoalResponse, progress: GoalProgressItem?) -> some View {
        let days = progress?.daysToTarget ?? 0
        let label = days > 0 ? "\(days) days" : "—"
        return HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text("\(viewModel.goalEmoji(goal)) \(goal.goalName)")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(MonytixTheme.text1)
                Text("Target: \(goal.targetDate ?? "—")")
                    .font(.system(size: 11))
                    .foregroundStyle(MonytixTheme.text2)
            }
            Spacer()
            Text(label)
                .font(.system(size: 12, weight: .bold))
                .foregroundStyle(MonytixTheme.warn)
        }
        .padding(MonytixSpace.sm)
        .background(MonytixTheme.surface2)
        .clipShape(RoundedRectangle(cornerRadius: MonytixShape.smallRadius))
    }

    private func goalCard(goal: GoalResponse, progress: GoalProgressItem?) -> some View {
        let pct = Int(progress?.progressPct ?? 0)
        let saved = goal.currentSavings
        let target = goal.estimatedCost
        let remaining = max(0, target - saved)
        return VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("\(viewModel.goalEmoji(goal)) \(goal.goalName)")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundStyle(MonytixTheme.text1)
                Spacer()
                Text("\(pct)%")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundStyle(MonytixTheme.cyan1)
            }
            Text(viewModel.goalEmotionalLabel(goal))
                .font(.system(size: 12))
                .foregroundStyle(MonytixTheme.text2)
            GeometryReader { g in
                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 6)
                        .fill(MonytixTheme.stroke.opacity(0.35))
                        .frame(height: 10)
                    RoundedRectangle(cornerRadius: 6)
                        .fill(MonytixTheme.gradientPrimary)
                        .frame(width: g.size.width * CGFloat(min(max(pct, 0), 100)) / 100, height: 10)
                }
            }
            .frame(height: 10)
            HStack {
                Text("Saved \(formatINR(saved))")
                    .font(.system(size: 11))
                    .foregroundStyle(MonytixTheme.text2)
                Spacer()
                Text("Remaining \(formatINR(remaining))")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(MonytixTheme.warn)
            }
        }
        .padding(MonytixSpace.md)
        .background(MonytixTheme.surface)
        .clipShape(RoundedRectangle(cornerRadius: MonytixShape.cardRadius))
    }
}

// MARK: - Goals tab

private struct GoalTrackerGoalsTab: View {
    @ObservedObject var viewModel: GoalTrackerViewModel
    let onGoalTap: (GoalResponse) -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: MonytixSpace.md) {
                filterChips
                let goals = filteredGoals()
                if goals.isEmpty {
                    Text("No goals for this filter.")
                        .font(.system(size: 14))
                        .foregroundStyle(MonytixTheme.text2)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 40)
                } else {
                    ForEach(goals, id: \.goalId) { g in
                        goalRow(g)
                            .onTapGesture { onGoalTap(g) }
                    }
                }
            }
            .padding(MonytixSpace.md)
            .padding(.bottom, MonytixSpace.xl)
        }
    }

    private func filteredGoals() -> [GoalResponse] {
        switch viewModel.selectedFilter?.lowercased() {
        case "active":
            return viewModel.goals.filter { $0.status.lowercased() == "active" }
        case "completed":
            return viewModel.goals.filter { $0.status.lowercased() == "completed" }
        default:
            return viewModel.goals
        }
    }

    private var filterChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                chip(title: "All", selected: viewModel.selectedFilter == nil) { viewModel.setFilter(nil) }
                chip(title: "Active", selected: viewModel.selectedFilter == "active") { viewModel.setFilter("active") }
                chip(title: "Completed", selected: viewModel.selectedFilter == "completed") { viewModel.setFilter("completed") }
            }
        }
    }

    private func chip(title: String, selected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 12, weight: selected ? .semibold : .medium))
                .foregroundStyle(selected ? MonytixTheme.bg : MonytixTheme.text2)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(
                    RoundedRectangle(cornerRadius: 12)
                        .fill(selected ? MonytixTheme.cyan1 : MonytixTheme.surface)
                )
        }
        .buttonStyle(.plain)
    }

    private func goalRow(_ g: GoalResponse) -> some View {
        let pct = Int(viewModel.progress.first { $0.goalId == g.goalId }?.progressPct ?? 0)
        return HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text("\(viewModel.goalEmoji(g)) \(g.goalName)")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(MonytixTheme.text1)
                Text(g.status.capitalized)
                    .font(.system(size: 11))
                    .foregroundStyle(MonytixTheme.text2)
            }
            Spacer()
            Text("\(pct)%")
                .font(.system(size: 12, weight: .bold))
                .foregroundStyle(MonytixTheme.cyan1)
        }
        .padding(MonytixSpace.sm)
        .background(MonytixTheme.surface)
        .clipShape(RoundedRectangle(cornerRadius: MonytixShape.smallRadius))
    }
}

// MARK: - AI Insights tab (simple)

private struct GoalTrackerAIInsightsTab: View {
    @ObservedObject var viewModel: GoalTrackerViewModel

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: MonytixSpace.md) {
                Text("AI Insights")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(MonytixTheme.text1)

                let health = viewModel.goalHealthSummary()
                insightCard(
                    title: "Goal health score: \(health.score)",
                    message: health.atRisk > 0 ? "You have \(health.atRisk) goal(s) at risk. Consider increasing monthly contribution or extending timelines." : "All active goals look on track. Keep your current pace.",
                    tint: health.atRisk > 0 ? MonytixTheme.warn : MonytixTheme.success
                )

                let deadlines = viewModel.upcomingDeadlines()
                if let first = deadlines.first {
                    insightCard(
                        title: "Next deadline: \(first.0.goalName)",
                        message: "Target date \(first.0.targetDate ?? "—"). Check your monthly required amount and close the gap early.",
                        tint: MonytixTheme.cyan1
                    )
                }
            }
            .padding(MonytixSpace.md)
            .padding(.bottom, MonytixSpace.xl)
        }
    }

    private func insightCard(title: String, message: String, tint: Color) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title)
                .font(.system(size: 14, weight: .bold))
                .foregroundStyle(tint)
            Text(message)
                .font(.system(size: 13))
                .foregroundStyle(MonytixTheme.text1)
        }
        .padding(MonytixSpace.md)
        .background(MonytixTheme.surface)
        .clipShape(RoundedRectangle(cornerRadius: MonytixShape.cardRadius))
        .overlay(
            RoundedRectangle(cornerRadius: MonytixShape.cardRadius)
                .stroke(tint.opacity(0.35), lineWidth: 1)
        )
    }
}

// MARK: - Detail + Sheets

private struct GoalDetailView: View {
    let goal: GoalResponse
    let progress: GoalProgressItem?
    @ObservedObject var viewModel: GoalTrackerViewModel
    let onEdit: () -> Void
    let onDelete: () -> Void

    private var monthlyRequired: Double { progress?.monthlyRequired ?? 0 }
    private var currentAvg: Double { progress?.monthlyAvgContribution ?? 0 }
    private var progressPct: Double { progress?.progressPct ?? 0 }
    private var gap: Double { max(0, monthlyRequired - currentAvg) }
    private var isOnTrack: Bool { gap <= 0 || progressPct >= 95 }
    private var riskState: String {
        if progressPct >= 95 { return "complete" }
        if isOnTrack { return "on_track" }
        if progressPct >= 40 { return "behind" }
        return "at_risk"
    }
    private var projectedDate: String {
        progress?.projectedCompletionDate
            ?? (isOnTrack ? goal.targetDate : nil)
            ?? "—"
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: MonytixSpace.lg) {
                riskStateStrip
                heroCard
                planCard
            }
            .padding(MonytixSpace.md)
            .padding(.bottom, MonytixSpace.xl)
        }
        .background(MonytixTheme.bg.ignoresSafeArea())
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Menu {
                    Button("Edit") { onEdit() }
                    Button("Delete", role: .destructive) { onDelete() }
                } label: {
                    Image(systemName: "ellipsis.circle")
                }
            }
        }
    }

    private var riskStateStrip: some View {
        let (label, color) = riskStateLabelAndColor
        return Text(label)
            .font(.system(size: 14, weight: .semibold))
            .foregroundStyle(color)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(MonytixSpace.sm + 2)
            .background(color.opacity(0.15))
            .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private var riskStateLabelAndColor: (String, Color) {
        switch riskState {
        case "complete": return ("Complete", MonytixTheme.success)
        case "on_track": return ("On track", MonytixTheme.success)
        case "behind": return ("Behind pace", MonytixTheme.warn)
        default: return ("At risk", MonytixTheme.danger)
        }
    }

    private var heroCard: some View {
        let pct = Int(progressPct)
        let remaining = (progress?.remainingAmount ?? (goal.estimatedCost - goal.currentSavings)).clamped(to: 0...Double.greatestFiniteMagnitude)
        return VStack(alignment: .leading, spacing: 10) {
            Text("\(viewModel.goalEmoji(goal)) \(goal.goalName)")
                .font(.system(size: 18, weight: .bold))
                .foregroundStyle(MonytixTheme.text1)
            Text(viewModel.goalEmotionalLabel(goal))
                .font(.system(size: 12, weight: .medium))
                .foregroundStyle(MonytixTheme.cyan1)

            Text("Progress \(pct)%")
                .font(.system(size: 13))
                .foregroundStyle(MonytixTheme.text2)

            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(formatINR(goal.currentSavings))
                        .font(.system(size: 16, weight: .bold))
                        .foregroundStyle(MonytixTheme.text1)
                    Text("saved")
                        .font(.system(size: 11))
                        .foregroundStyle(MonytixTheme.text2)
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 2) {
                    Text(formatINR(remaining))
                        .font(.system(size: 16, weight: .bold))
                        .foregroundStyle(MonytixTheme.warn)
                    Text("remaining")
                        .font(.system(size: 11))
                        .foregroundStyle(MonytixTheme.text2)
                }
            }
            if let pace = progress?.paceDescription, !pace.isEmpty {
                Text("🕒 \(pace)")
                    .font(.system(size: 12))
                    .foregroundStyle(MonytixTheme.text2)
            }
        }
        .padding(MonytixSpace.lg)
        .background(MonytixTheme.surface)
        .clipShape(RoundedRectangle(cornerRadius: MonytixShape.cardRadius))
        .overlay(
            RoundedRectangle(cornerRadius: MonytixShape.cardRadius)
                .stroke(MonytixTheme.cyan1.opacity(0.25), lineWidth: 1)
        )
    }

    private var planCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("🧠 Your Current Plan")
                .font(.system(size: 15, weight: .bold))
                .foregroundStyle(MonytixTheme.text1)
            // One AI plan line
            Text(aiPlanLine)
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(MonytixTheme.cyan1)
            if let target = goal.targetDate {
                Text("Required to finish by \(target):")
                    .font(.system(size: 13))
                    .foregroundStyle(MonytixTheme.text2)
                Text("\(formatINR(monthlyRequired))/month")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundStyle(MonytixTheme.text1)
                Text("Current pace: \(formatINR(currentAvg))/month")
                    .font(.system(size: 12))
                    .foregroundStyle(MonytixTheme.text2)
                Text("Projected completion: \(projectedDate)")
                    .font(.system(size: 12))
                    .foregroundStyle(MonytixTheme.text2)
                if gap > 0 {
                    Text("Gap: \(formatINR(gap))/month short")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(MonytixTheme.warn)
                }
            } else {
                Text("Current pace: \(formatINR(currentAvg))/month")
                    .font(.system(size: 12))
                    .foregroundStyle(MonytixTheme.text2)
                Text("Projected completion: \(projectedDate)")
                    .font(.system(size: 12))
                    .foregroundStyle(MonytixTheme.text2)
                Text("Set a target date to unlock required monthly.")
                    .font(.system(size: 13))
                    .foregroundStyle(MonytixTheme.text2)
            }
        }
        .padding(MonytixSpace.lg)
        .background(MonytixTheme.surface)
        .clipShape(RoundedRectangle(cornerRadius: MonytixShape.cardRadius))
    }

    private var aiPlanLine: String {
        if isOnTrack && progressPct < 95 {
            return "AI plan: Save \(formatINR(monthlyRequired))/month to hit target by \(goal.targetDate ?? "deadline")."
        }
        if gap > 0 {
            return "AI plan: Top up by \(formatINR(gap))/month to stay on track."
        }
        return "AI plan: You're on track. Keep current pace."
    }
}

private struct AddGoalSheet: View {
    @ObservedObject var viewModel: GoalTrackerViewModel
    @Binding var isPresented: Bool

    @State private var goalCategory = "general"
    @State private var goalName = ""
    @State private var estimatedCost = ""
    @State private var currentSavings = ""
    @State private var targetDate = ""

    var body: some View {
        NavigationStack {
            ZStack {
                MonytixTheme.bg.ignoresSafeArea()
                Form {
                    Section("Goal") {
                        TextField("Category (e.g. travel)", text: $goalCategory)
                        TextField("Name", text: $goalName)
                    }
                    Section("Amounts") {
                        TextField("Estimated cost", text: $estimatedCost)
                            .keyboardType(.decimalPad)
                        TextField("Current savings", text: $currentSavings)
                            .keyboardType(.decimalPad)
                    }
                    Section("Target") {
                        TextField("Target date (YYYY-MM-DD)", text: $targetDate)
                            .textInputAutocapitalization(.never)
                    }
                }
                .scrollContentBackground(.hidden)
            }
            .navigationTitle("Add Goal")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Cancel") { isPresented = false }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Save") {
                        let est = Double(estimatedCost) ?? 0
                        let saved = Double(currentSavings) ?? 0
                        viewModel.createGoal(
                            goalCategory: goalCategory,
                            goalName: goalName,
                            estimatedCost: est,
                            targetDate: targetDate.isEmpty ? nil : targetDate,
                            currentSavings: saved
                        )
                    }
                    .disabled(goalName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || (Double(estimatedCost) ?? 0) <= 0)
                }
            }
            .onChange(of: viewModel.createGoalResult) { _, r in
                guard let r else { return }
                switch r {
                case .success:
                    viewModel.clearCreateGoalResult()
                    isPresented = false
                case .failure:
                    viewModel.clearCreateGoalResult()
                }
            }
        }
    }
}

private struct EditGoalSheet: View {
    @ObservedObject var viewModel: GoalTrackerViewModel
    let goal: GoalResponse

    @Environment(\.dismiss) private var dismiss
    @State private var estimatedCost: String
    @State private var currentSavings: String
    @State private var targetDate: String

    init(viewModel: GoalTrackerViewModel, goal: GoalResponse) {
        self.viewModel = viewModel
        self.goal = goal
        _estimatedCost = State(initialValue: String(goal.estimatedCost))
        _currentSavings = State(initialValue: String(goal.currentSavings))
        _targetDate = State(initialValue: goal.targetDate ?? "")
    }

    var body: some View {
        NavigationStack {
            ZStack {
                MonytixTheme.bg.ignoresSafeArea()
                Form {
                    Section("Amounts") {
                        TextField("Estimated cost", text: $estimatedCost)
                            .keyboardType(.decimalPad)
                        TextField("Current savings", text: $currentSavings)
                            .keyboardType(.decimalPad)
                    }
                    Section("Target") {
                        TextField("Target date (YYYY-MM-DD)", text: $targetDate)
                            .textInputAutocapitalization(.never)
                    }
                }
                .scrollContentBackground(.hidden)
            }
            .navigationTitle("Edit Goal")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Save") {
                        viewModel.updateGoal(
                            goalId: goal.goalId,
                            estimatedCost: Double(estimatedCost),
                            targetDate: targetDate.isEmpty ? nil : targetDate,
                            currentSavings: Double(currentSavings)
                        )
                        dismiss()
                    }
                }
            }
        }
    }
}

private func formatINR(_ amount: Double) -> String {
    let absAmount = abs(amount)
    let formatter = NumberFormatter()
    formatter.numberStyle = .decimal
    formatter.maximumFractionDigits = 0
    let formatted = formatter.string(from: NSNumber(value: absAmount)) ?? "\(Int(absAmount))"
    return amount < 0 ? "-₹\(formatted)" : "₹\(formatted)"
}

private extension Double {
    func clamped(to range: ClosedRange<Double>) -> Double {
        min(max(self, range.lowerBound), range.upperBound)
    }
}

#Preview {
    GoalTrackerView()
}

