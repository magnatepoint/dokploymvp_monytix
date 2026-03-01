//
//  MoneyMomentsView.swift
//  ios
//
//  MoneyMoments - matches APK MoneyMomentsScreen
//

import SwiftUI

struct MoneyMomentsView: View {
    @Environment(AuthViewModel.self) private var authViewModel
    @State private var viewModel = MoneyMomentsViewModel()
    @State private var selectedTab: MmTab = .nudges

    var body: some View {
        NavigationStack {
            PremiumGradientBackground {
                VStack(spacing: 0) {
                    WelcomeBanner(username: authViewModel.user?.email ?? viewModel.userEmail)
                    TabBar(selectedTab: $selectedTab)

                    if viewModel.isMomentsLoading && viewModel.isNudgesLoading && viewModel.nudges.isEmpty && viewModel.moments.isEmpty {
                        Spacer()
                        ProgressView()
                            .tint(MonytixColors.accentPrimary)
                        Spacer()
                    } else {
                        ScrollView {
                            switch selectedTab {
                            case .nudges:
                                NudgesTab(viewModel: viewModel)
                            case .habits:
                                HabitsTab(viewModel: viewModel)
                            case .aiInsights:
                                AIInsightsTab(viewModel: viewModel)
                            }
                        }
                    }
                }
            }
            .navigationTitle("MoneyMoments")
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
                    viewModel.userEmail = authViewModel.user?.email
                    await viewModel.loadData(accessToken: token)
                }
            }
            .onAppear {
                AnalyticsHelper.logScreenView(screenName: "money_moments")
            }
        }
    }
}

private enum MmTab: String, CaseIterable {
    case nudges
    case habits
    case aiInsights

    var label: String {
        switch self {
        case .nudges: return "Nudges"
        case .habits: return "Habits"
        case .aiInsights: return "AI Insights"
        }
    }
}

private struct WelcomeBanner: View {
    let username: String?

    var body: some View {
        let base = username?.split(separator: "@").first.map(String.init) ?? "User"
        let displayName = base.isEmpty ? "User" : base.prefix(1).uppercased() + base.dropFirst()
        VStack(alignment: .leading, spacing: 4) {
            Text("Hi, \(displayName)")
                .font(MonytixTypography.titleMedium)
                .fontWeight(.semibold)
                .foregroundColor(MonytixColors.textPrimary)
            Text("Your financial nudges and insights")
                .font(MonytixTypography.bodySmall)
                .foregroundColor(MonytixColors.textSecondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(20)
        .background(MonytixColors.surfaceElevated.opacity(0.5))
    }
}

private struct TabBar: View {
    @Binding var selectedTab: MmTab

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(MmTab.allCases, id: \.rawValue) { tab in
                    Button {
                        selectedTab = tab
                    } label: {
                        Text(tab.label)
                            .font(MonytixTypography.labelMedium)
                            .foregroundColor(selectedTab == tab ? MonytixColors.accentPrimary : MonytixColors.textSecondary)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(selectedTab == tab ? MonytixColors.accentPrimary.opacity(0.25) : Color.clear)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
        }
    }
}

private struct NudgesTab: View {
    let viewModel: MoneyMomentsViewModel

    var body: some View {
        Group {
            if viewModel.nudges.isEmpty {
                EmptyStateView(title: "No nudges yet", subtitle: "Your personalized financial nudges will appear here")
                    .padding(.top, 40)
            } else {
                LazyVStack(alignment: .leading, spacing: 12) {
                    ForEach(Array(viewModel.nudges.enumerated()), id: \.offset) { _, nudge in
                        NudgeCard(nudge: nudge)
                    }
                }
                .padding(16)
            }
        }
    }
}

private struct NudgeCard: View {
    let nudge: Nudge

    var body: some View {
        GlassCardView {
            VStack(alignment: .leading, spacing: 8) {
                Text(nudge.title ?? nudge.ruleName ?? "Nudge")
                    .font(MonytixTypography.titleSmall)
                    .fontWeight(.bold)
                    .foregroundColor(MonytixColors.accentSecondary)
                Text(nudge.body ?? "")
                    .font(MonytixTypography.bodyMedium)
                    .foregroundColor(MonytixColors.textPrimary)
            }
        }
        .overlay(
            RoundedRectangle(cornerRadius: MonytixLayout.cardRadius)
                .stroke(MonytixColors.accentSecondary.opacity(0.3), lineWidth: 1)
        )
    }
}

private struct HabitsTab: View {
    let viewModel: MoneyMomentsViewModel

    var body: some View {
        Group {
            if viewModel.moments.isEmpty {
                EmptyStateView(title: "No habits yet", subtitle: "Your financial habits will be tracked here")
                    .padding(.top, 40)
            } else {
                LazyVStack(alignment: .leading, spacing: 12) {
                    Text("Habits")
                        .font(MonytixTypography.titleLarge)
                        .fontWeight(.bold)
                        .foregroundColor(MonytixColors.textPrimary)
                    ForEach(Array(viewModel.moments.enumerated()), id: \.offset) { _, moment in
                        MomentCard(moment: moment)
                    }
                }
                .padding(16)
            }
        }
    }
}

private struct MomentCard: View {
    let moment: MoneyMoment

    var body: some View {
        GlassCardView {
            VStack(alignment: .leading, spacing: 8) {
                Text(moment.label ?? "Moment")
                    .font(MonytixTypography.titleSmall)
                    .fontWeight(.bold)
                    .foregroundColor(MonytixColors.textPrimary)
                Text(moment.insightText ?? "")
                    .font(MonytixTypography.bodyMedium)
                    .foregroundColor(MonytixColors.textSecondary)
            }
        }
    }
}

private struct AIInsightsTab: View {
    let viewModel: MoneyMomentsViewModel

    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "sparkles")
                .font(.system(size: 48))
                .foregroundColor(MonytixColors.accentSecondary.opacity(0.6))
            Text("AI Insights")
                .font(MonytixTypography.titleMedium)
                .fontWeight(.semibold)
                .foregroundColor(MonytixColors.textPrimary)
            Text("Personalized insights based on your spending patterns will appear here.")
                .font(MonytixTypography.bodyMedium)
                .foregroundColor(MonytixColors.textSecondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(32)
    }
}

#Preview {
    MoneyMomentsView()
        .environment(AuthViewModel())
}
