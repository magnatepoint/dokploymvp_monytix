//
//  GoalTrackerView.swift
//  ios
//
//  GoalTracker - matches APK GoalTrackerScreen
//

import SwiftUI

struct GoalTrackerView: View {
    @Environment(AuthViewModel.self) private var authViewModel
    @State private var viewModel = GoalTrackerViewModel()
    @State private var selectedTab: GtTab = .overview
    @State private var showAddGoal = false
    @State private var selectedGoal: GoalResponse?

    var body: some View {
        NavigationStack {
            PremiumGradientBackground {
                VStack(spacing: 0) {
                    TabBar(selectedTab: $selectedTab)

                    if viewModel.isLoading && viewModel.goals.isEmpty && viewModel.goalsProgress.isEmpty {
                        Spacer()
                        ProgressView()
                            .tint(MonytixColors.accentPrimary)
                        Spacer()
                    } else {
                        ScrollView {
                            switch selectedTab {
                            case .overview:
                                GoalsOverviewTab(viewModel: viewModel)
                            case .goals:
                                GoalsListTab(viewModel: viewModel, onSelectGoal: { selectedGoal = $0 })
                            }
                        }
                    }
                }
            }
            .navigationTitle("GoalTracker")
            .navigationBarTitleDisplayMode(.inline)
            .toolbarColorScheme(.dark, for: .navigationBar)
            .toolbarBackground(MonytixColors.surfaceDark, for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    HStack {
                        Button {
                            Task {
                                if let token = await authViewModel.getAccessToken() {
                                    await viewModel.refresh(accessToken: token)
                                }
                            }
                        } label: {
                            Image(systemName: "arrow.clockwise")
                                .foregroundColor(MonytixColors.textPrimary)
                        }
                        Button {
                            showAddGoal = true
                        } label: {
                            Image(systemName: "plus")
                                .foregroundColor(MonytixColors.textPrimary)
                        }
                    }
                }
            }
            .task {
                if let token = await authViewModel.getAccessToken() {
                    await viewModel.loadData(accessToken: token)
                }
            }
            .onAppear {
                AnalyticsHelper.logScreenView(screenName: "goal_tracker")
            }
            .sheet(isPresented: $showAddGoal) {
                AddGoalSheet(viewModel: viewModel, authViewModel: authViewModel) {
                    showAddGoal = false
                }
            }
            .navigationDestination(item: $selectedGoal) { goal in
                GoalDetailView(goal: goal, viewModel: viewModel, authViewModel: authViewModel)
            }
        }
    }
}

private enum GtTab: String, CaseIterable {
    case overview
    case goals

    var label: String {
        switch self {
        case .overview: return "Overview"
        case .goals: return "Goals"
        }
    }
}

private struct TabBar: View {
    @Binding var selectedTab: GtTab

    var body: some View {
        HStack(spacing: 0) {
            ForEach(GtTab.allCases, id: \.rawValue) { tab in
                Button {
                    selectedTab = tab
                } label: {
                    Text(tab.label)
                        .font(MonytixTypography.labelMedium)
                        .foregroundColor(selectedTab == tab ? MonytixColors.accentPrimary : MonytixColors.textSecondary)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 12)
                }
                .buttonStyle(.plain)
            }
        }
        .background(MonytixColors.surfaceElevated.opacity(0.5))
    }
}

private struct GoalsOverviewTab: View {
    let viewModel: GoalTrackerViewModel

    var body: some View {
        let progress = viewModel.goalsProgress
        LazyVStack(alignment: .leading, spacing: 16) {
            Text("Your Goals")
                .font(MonytixTypography.titleLarge)
                .fontWeight(.bold)
                .foregroundColor(MonytixColors.textPrimary)
            if progress.isEmpty {
                EmptyStateView(title: "No goals yet", subtitle: "Create your first financial goal to get started")
                    .padding(.top, 20)
            } else {
                ForEach(progress, id: \.goalId) { g in
                    GoalProgressCard(item: g)
                }
            }
        }
        .padding(16)
    }
}

private struct GoalProgressCard: View {
    let item: GoalProgressItem

    var body: some View {
        let color: Color = item.progressPct >= 80 ? MonytixColors.accentPrimary : (item.progressPct >= 40 ? MonytixColors.accentPrimary : MonytixColors.warning)
        GlassCardView {
            VStack(alignment: .leading, spacing: 8) {
                Text(item.goalName)
                    .font(MonytixTypography.titleMedium)
                    .fontWeight(.bold)
                    .foregroundColor(MonytixColors.textPrimary)
                Text("\(Int(item.progressPct))% complete")
                    .font(MonytixTypography.bodySmall)
                    .foregroundColor(color)
                GeometryReader { geo in
                    ZStack(alignment: .leading) {
                        RoundedRectangle(cornerRadius: 6)
                            .fill(MonytixColors.textSecondary.opacity(0.3))
                        RoundedRectangle(cornerRadius: 6)
                            .fill(color)
                            .frame(width: geo.size.width * CGFloat(min(item.progressPct / 100, 1)))
                    }
                }
                .frame(height: 12)
                HStack {
                    VStack(alignment: .leading) {
                        Text("Saved")
                            .font(MonytixTypography.labelSmall)
                            .foregroundColor(MonytixColors.textSecondary)
                        Text(formatCurrency(item.currentSavingsClose))
                            .font(MonytixTypography.bodyMedium)
                            .fontWeight(.semibold)
                            .foregroundColor(MonytixColors.textPrimary)
                    }
                    Spacer()
                    VStack(alignment: .trailing) {
                        Text("Remaining")
                            .font(MonytixTypography.labelSmall)
                            .foregroundColor(MonytixColors.textSecondary)
                        Text(formatCurrency(item.remainingAmount))
                            .font(MonytixTypography.bodyMedium)
                            .fontWeight(.semibold)
                            .foregroundColor(MonytixColors.textPrimary)
                    }
                }
            }
        }
        .overlay(
            RoundedRectangle(cornerRadius: MonytixLayout.cardRadius)
                .stroke(color.opacity(0.3), lineWidth: 1)
        )
    }
}

private struct GoalsListTab: View {
    let viewModel: GoalTrackerViewModel
    let onSelectGoal: (GoalResponse) -> Void

    var body: some View {
        LazyVStack(alignment: .leading, spacing: 12) {
            if viewModel.goals.isEmpty {
                EmptyStateView(title: "No goals", subtitle: "Tap + to add your first goal")
                    .padding(.top, 40)
            } else {
                ForEach(viewModel.goals, id: \.goalId) { goal in
                    Button {
                        onSelectGoal(goal)
                    } label: {
                        GlassCardView {
                            HStack {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(goal.goalName)
                                        .font(MonytixTypography.titleMedium)
                                        .fontWeight(.bold)
                                        .foregroundColor(MonytixColors.textPrimary)
                                    Text("Target: \(formatCurrency(goal.estimatedCost))")
                                        .font(MonytixTypography.bodySmall)
                                        .foregroundColor(MonytixColors.textSecondary)
                                }
                                Spacer()
                                Image(systemName: "chevron.right")
                                    .foregroundColor(MonytixColors.textSecondary)
                            }
                        }
                    }
                    .buttonStyle(.plain)
                }
            }
        }
        .padding(16)
    }
}

private struct AddGoalSheet: View {
    let viewModel: GoalTrackerViewModel
    let authViewModel: AuthViewModel
    let onDismiss: () -> Void

    @State private var name = ""
    @State private var targetAmount = ""
    @State private var currentSavings = ""

    var body: some View {
        NavigationStack {
            Form {
                TextField("Goal name", text: $name)
                TextField("Target amount (₹)", text: $targetAmount)
                    .keyboardType(.decimalPad)
                TextField("Current savings (₹)", text: $currentSavings)
                    .keyboardType(.decimalPad)
            }
            .scrollContentBackground(.hidden)
            .background(MonytixColors.surfaceDark)
            .navigationTitle("Add Goal")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { onDismiss() }
                        .foregroundColor(MonytixColors.accentPrimary)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        Task {
                            if let token = await authViewModel.getAccessToken(),
                               let amount = Double(targetAmount),
                               !name.isEmpty {
                                await viewModel.createGoal(
                                    accessToken: token,
                                    name: name,
                                    estimatedCost: amount,
                                    currentSavings: Double(currentSavings) ?? 0
                                )
                                onDismiss()
                            }
                        }
                    }
                    .foregroundColor(MonytixColors.accentPrimary)
                    .disabled(name.isEmpty || Double(targetAmount) == nil)
                }
            }
        }
    }
}

struct GoalDetailView: View {
    let goal: GoalResponse
    let viewModel: GoalTrackerViewModel
    let authViewModel: AuthViewModel

    var body: some View {
        let progress = viewModel.goalsProgress.first { $0.goalId == goal.goalId }
        let progressPct = progress?.progressPct ?? (goal.estimatedCost > 0 ? goal.currentSavings / goal.estimatedCost * 100 : 0)
        let color: Color = progressPct >= 80 ? MonytixColors.accentPrimary : (progressPct >= 40 ? MonytixColors.accentPrimary : MonytixColors.warning)

        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                GlassCardView {
                    VStack(alignment: .leading, spacing: 12) {
                        Text(goal.goalName)
                            .font(MonytixTypography.headlineSmall)
                            .fontWeight(.bold)
                            .foregroundColor(MonytixColors.textPrimary)
                        Text("\(Int(progressPct))% complete")
                            .font(MonytixTypography.bodyMedium)
                            .foregroundColor(color)
                        GeometryReader { geo in
                            ZStack(alignment: .leading) {
                                RoundedRectangle(cornerRadius: 6)
                                    .fill(MonytixColors.textSecondary.opacity(0.3))
                                RoundedRectangle(cornerRadius: 6)
                                    .fill(color)
                                    .frame(width: geo.size.width * CGFloat(min(progressPct / 100, 1)))
                            }
                        }
                        .frame(height: 12)
                        HStack {
                            VStack(alignment: .leading) {
                                Text("Saved")
                                Text(formatCurrency(goal.currentSavings))
                            }
                            Spacer()
                            VStack(alignment: .trailing) {
                                Text("Target")
                                Text(formatCurrency(goal.estimatedCost))
                            }
                        }
                        .font(MonytixTypography.bodyMedium)
                        .foregroundColor(MonytixColors.textPrimary)
                    }
                }
            }
            .padding(16)
        }
        .background(MonytixColors.backgroundGradientTop)
        .navigationTitle(goal.goalName)
        .navigationBarTitleDisplayMode(.inline)
    }
}

extension GoalResponse: Identifiable {
    var id: String { goalId }
}

#Preview {
    GoalTrackerView()
        .environment(AuthViewModel())
}
