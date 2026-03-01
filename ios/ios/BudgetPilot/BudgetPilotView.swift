//
//  BudgetPilotView.swift
//  ios
//
//  BudgetPilot - matches APK BudgetPilotScreen
//

import SwiftUI

struct BudgetPilotView: View {
    @Environment(AuthViewModel.self) private var authViewModel
    @State private var viewModel = BudgetPilotViewModel()

    var body: some View {
        NavigationStack {
            PremiumGradientBackground {
                Group {
                    if viewModel.isLoading && viewModel.budgetState == nil {
                        VStack {
                            Spacer()
                            ProgressView()
                                .tint(MonytixColors.accentPrimary)
                            Spacer()
                        }
                    } else {
                        ScrollView {
                            LazyVStack(alignment: .leading, spacing: 16) {
                                Text("Budget for \(formatMonthLabel(viewModel.selectedMonth))")
                                    .font(MonytixTypography.titleMedium)
                                    .foregroundColor(MonytixColors.textSecondary)

                                if let state = viewModel.budgetState {
                                    if let plan = state.committedPlan {
                                        GlassCardView {
                                            VStack(alignment: .leading, spacing: 8) {
                                                Text("Committed Plan")
                                                    .font(MonytixTypography.titleSmall)
                                                    .fontWeight(.bold)
                                                    .foregroundColor(MonytixColors.textPrimary)
                                                HStack {
                                                    BudgetRow(label: "Needs", value: plan.needs)
                                                    Spacer()
                                                    BudgetRow(label: "Wants", value: plan.wants)
                                                    Spacer()
                                                    BudgetRow(label: "Savings", value: plan.savings)
                                                }
                                            }
                                        }
                                    }
                                    if let actual = state.actual {
                                        GlassCardView {
                                            VStack(alignment: .leading, spacing: 8) {
                                                Text("Actual")
                                                    .font(MonytixTypography.titleSmall)
                                                    .fontWeight(.bold)
                                                    .foregroundColor(MonytixColors.textPrimary)
                                                HStack {
                                                    BudgetRow(label: "Needs", value: actual.needs)
                                                    Spacer()
                                                    BudgetRow(label: "Wants", value: actual.wants)
                                                    Spacer()
                                                    BudgetRow(label: "Savings", value: actual.savings)
                                                }
                                            }
                                        }
                                    }
                                }

                                if !viewModel.variance.isEmpty {
                                    Text("Variance")
                                        .font(MonytixTypography.titleLarge)
                                        .fontWeight(.bold)
                                        .foregroundColor(MonytixColors.textPrimary)
                                    ForEach(viewModel.variance, id: \.category) { v in
                                        VarianceCard(variance: v)
                                    }
                                }

                                if viewModel.budgetState == nil && viewModel.variance.isEmpty {
                                    EmptyStateView(
                                        title: "No budget data",
                                        subtitle: "Set up your budget to track spending",
                                        onRetry: {
                                            Task {
                                                if let token = await authViewModel.getAccessToken() {
                                                    await viewModel.refresh(accessToken: token)
                                                }
                                            }
                                        }
                                    )
                                    .padding(.top, 40)
                                }
                            }
                            .padding(16)
                        }
                    }
                }
            }
            .navigationTitle("BudgetPilot")
            .navigationBarTitleDisplayMode(.inline)
            .toolbarColorScheme(.dark, for: .navigationBar)
            .toolbarBackground(MonytixColors.surfaceDark, for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
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
                }
            }
            .task {
                if let token = await authViewModel.getAccessToken() {
                    await viewModel.loadData(accessToken: token)
                }
            }
            .onAppear {
                AnalyticsHelper.logScreenView(screenName: "budget_pilot")
            }
        }
    }
}

private struct BudgetRow: View {
    let label: String
    let value: Double?

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(label)
                .font(MonytixTypography.labelSmall)
                .foregroundColor(MonytixColors.textSecondary)
            Text(formatCurrency(value ?? 0))
                .font(MonytixTypography.bodyMedium)
                .fontWeight(.semibold)
                .foregroundColor(MonytixColors.textPrimary)
        }
    }
}

private struct VarianceCard: View {
    let variance: BudgetVariance

    var body: some View {
        let v = variance.variance ?? 0
        let color: Color = v > 0 ? MonytixColors.chartRed : MonytixColors.success
        GlassCardView {
            HStack {
                Text(variance.category ?? "Unknown")
                    .font(MonytixTypography.titleSmall)
                    .foregroundColor(MonytixColors.textPrimary)
                Spacer()
                VStack(alignment: .trailing, spacing: 2) {
                    Text("Planned: \(formatCurrency(variance.planned ?? 0))")
                        .font(MonytixTypography.labelSmall)
                        .foregroundColor(MonytixColors.textSecondary)
                    Text("Actual: \(formatCurrency(variance.actual ?? 0))")
                        .font(MonytixTypography.labelSmall)
                        .foregroundColor(MonytixColors.textSecondary)
                    Text("\(v >= 0 ? "+" : "")\(formatCurrency(v))")
                        .font(MonytixTypography.bodyMedium)
                        .fontWeight(.bold)
                        .foregroundColor(color)
                }
            }
        }
    }
}

#Preview {
    BudgetPilotView()
        .environment(AuthViewModel())
}
