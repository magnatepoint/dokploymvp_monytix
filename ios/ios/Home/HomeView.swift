//
//  HomeView.swift
//  ios
//
//  MolyConsole - matches APK HomeScreen
//

import SwiftUI
import Charts

struct HomeView: View {
    @Environment(AuthViewModel.self) private var authViewModel
    @State private var viewModel = HomeViewModel()
    @State private var selectedTab: ConsoleTab = .overview

    var body: some View {
        NavigationStack {
            ZStack {
                LinearGradient(
                    colors: [MonytixColors.backgroundGradientTop, MonytixColors.backgroundGradientBottom],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .ignoresSafeArea()

                VStack(spacing: 0) {
                    WelcomeBanner(username: authViewModel.user?.email ?? viewModel.userEmail ?? "User")
                    TabBar(selectedTab: $selectedTab)

                    if viewModel.isLoading && viewModel.kpis == nil {
                        Spacer()
                        ProgressView()
                            .tint(MonytixColors.accentPrimary)
                        Spacer()
                    } else {
                        ScrollView {
                            tabContent
                        }
                    }
                }
            }
            .navigationTitle("MolyConsole")
            .navigationBarTitleDisplayMode(.inline)
            .toolbarColorScheme(.dark, for: .navigationBar)
            .toolbarBackground(MonytixColors.surfaceDark, for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    HStack(spacing: 8) {
                        Button {
                            // TODO: Launch document picker for statement upload
                        } label: {
                            Image(systemName: "square.and.arrow.up")
                                .foregroundColor(MonytixColors.textPrimary)
                        }
                        Button {
                            // TODO: Navigate to add transaction
                        } label: {
                            Image(systemName: "plus")
                                .foregroundColor(MonytixColors.textPrimary)
                        }
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
            }
            .task(id: authViewModel.user?.id) {
                if let token = await authViewModel.getAccessToken() {
                    viewModel.userEmail = authViewModel.user?.email
                    await viewModel.loadDashboard(accessToken: token)
                }
            }
            .onAppear {
                AnalyticsHelper.logScreenView(screenName: "home")
            }
        }
    }

    @ViewBuilder
    private var tabContent: some View {
        switch selectedTab {
        case .overview:
            OverviewTab(viewModel: viewModel, authViewModel: authViewModel)
        case .accounts:
            AccountsTab(accounts: viewModel.accounts) {
                Task {
                    if let token = await authViewModel.getAccessToken() {
                        await viewModel.refresh(accessToken: token)
                    }
                }
            }
        case .spending:
            SpendingTab(viewModel: viewModel)
        case .goals:
            GoalsTab(viewModel: viewModel)
        case .aiInsight:
            AIInsightTab(viewModel: viewModel)
        }
    }
}

private enum ConsoleTab: String, CaseIterable {
    case overview
    case accounts
    case spending
    case goals
    case aiInsight

    var label: String {
        switch self {
        case .overview: return "Overview"
        case .accounts: return "Accounts"
        case .spending: return "Spending"
        case .goals: return "Goals"
        case .aiInsight: return "AI Insight"
        }
    }

    var icon: String {
        switch self {
        case .overview: return "chart.bar.fill"
        case .accounts: return "creditcard.fill"
        case .spending: return "indianrupeesign"
        case .goals: return "flag.fill"
        case .aiInsight: return "sparkles"
        }
    }
}

private struct WelcomeBanner: View {
    let username: String

    var body: some View {
        let displayName = username.split(separator: "@").first.map(String.init) ?? "User"
        VStack(alignment: .leading, spacing: 4) {
            Text("AI Financial Command Center")
                .font(MonytixTypography.titleMedium)
                .fontWeight(.semibold)
                .foregroundColor(MonytixColors.textPrimary.opacity(0.95))
            Text("Welcome back, \(displayName) • Your financial cockpit")
                .font(MonytixTypography.bodySmall)
                .foregroundColor(MonytixColors.textSecondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(20)
        .background(MonytixColors.surfaceElevated.opacity(0.5))
    }
}

private struct TabBar: View {
    @Binding var selectedTab: ConsoleTab

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(ConsoleTab.allCases, id: \.rawValue) { tab in
                    Button {
                        selectedTab = tab
                    } label: {
                        HStack(spacing: 4) {
                            Image(systemName: tab.icon)
                                .font(.caption2)
                            Text(tab.label)
                                .font(MonytixTypography.labelMedium)
                        }
                        .foregroundColor(selectedTab == tab ? MonytixColors.textPrimary : MonytixColors.textSecondary)
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

private struct OverviewTab: View {
    let viewModel: HomeViewModel
    let authViewModel: AuthViewModel

    private var hasAnyData: Bool {
        viewModel.kpis != nil || !viewModel.accounts.isEmpty || !viewModel.goalsProgress.isEmpty || !viewModel.recentTransactions.isEmpty
    }

    var body: some View {
        Group {
            if let err = viewModel.backendError, !hasAnyData {
                VStack(spacing: 16) {
                    Text("Could not load data")
                        .font(MonytixTypography.titleMedium)
                        .fontWeight(.bold)
                        .foregroundColor(MonytixColors.textPrimary)
                    Text(err)
                        .font(MonytixTypography.bodySmall)
                        .foregroundColor(MonytixColors.error)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal)
                }
                .padding(.top, 40)
            } else if !hasAnyData {
                EmptyStateView(title: "No overview data available", subtitle: "Upload statements to see your financial overview")
                    .padding(.top, 40)
            } else {
                LazyVStack(alignment: .leading, spacing: 16) {
                    if let err = viewModel.backendError, hasAnyData {
                        Text("Some data could not be loaded. \(err)")
                            .font(MonytixTypography.labelSmall)
                            .foregroundColor(MonytixColors.warning)
                            .padding(12)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(MonytixColors.warning.opacity(0.15))
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                    }
                    HeroBalanceCard(
                        netWorth: viewModel.totalNetWorth(),
                        trendPct: viewModel.netWorthTrendPct(),
                        sparklineData: viewModel.sparklineData()
                    )

                    if !viewModel.todayIntelligence().isEmpty {
                        TodaysIntelligenceCard(items: viewModel.todayIntelligence())
                    }

                    HStack(spacing: 8) {
                        DynamicChip(
                            label: viewModel.isCashFlowPositive() ? "Cash Flow Positive" : "Cash Flow Negative",
                            isPositive: viewModel.isCashFlowPositive()
                        )
                        if viewModel.goalsAtRiskCount() > 0 {
                            DynamicChip(label: "\(viewModel.goalsAtRiskCount()) Goals At Risk", isPositive: false)
                        }
                        let spikes = viewModel.spendingSpikeCount()
                        if spikes > 0 {
                            DynamicChip(label: "\(spikes) Spending Spike\(spikes > 1 ? "s" : "")", isPositive: false)
                        }
                    }

                    let goals = viewModel.transformGoals()
                    let kpis = viewModel.kpis
                    let monthName = monthNameFromNow()
                    let thisMonthSpending = kpis?.totalDebitsAmount ?? (kpis?.needsAmount ?? 0) + (kpis?.wantsAmount ?? 0)
                    let income = kpis?.incomeAmount ?? 0
                    let savingsRate = income > 0 ? ((income - thisMonthSpending) / income * 100) : 0

                    HStack(spacing: 12) {
                        SummaryCard(title: "\(monthName) Spending", value: formatCurrency(thisMonthSpending), color: MonytixColors.chartRed)
                        SavingsRateRingCard(savingsRate: savingsRate)
                        SummaryCard(title: "Active Goals", value: "\(goals.count)", color: MonytixColors.accentPrimary)
                    }

                    if !viewModel.recentTransactions.isEmpty {
                        Text("Recent Transactions")
                            .font(MonytixTypography.titleLarge)
                            .fontWeight(.bold)
                            .foregroundColor(MonytixColors.textPrimary)
                        ForEach(viewModel.recentTransactions.prefix(5), id: \.txnId) { txn in
                            RecentTransactionRow(transaction: txn)
                        }
                    }

                    let categories = viewModel.transformCategorySpending()
                    if !categories.isEmpty {
                        Text("Spending by Category")
                            .font(MonytixTypography.titleLarge)
                            .fontWeight(.bold)
                            .foregroundColor(MonytixColors.textPrimary)
                        GlassCardView {
                            VStack(spacing: 8) {
                                ForEach(Array(categories.prefix(5).enumerated()), id: \.offset) { _, cat in
                                    CategoryBarRow(category: cat.category, amount: cat.amount, fraction: categories.isEmpty ? 0 : cat.amount / (categories.map(\.amount).max() ?? 1))
                                }
                            }
                        }
                    }
                }
                .padding(16)
            }
        }
    }

    private func monthNameFromNow() -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "MMMM"
        return formatter.string(from: Date())
    }
}

private struct HeroBalanceCard: View {
    let netWorth: Double
    let trendPct: Double
    let sparklineData: [Float]

    var body: some View {
        GlassCardView {
            VStack(alignment: .leading, spacing: 4) {
                Text("Net Worth")
                    .font(MonytixTypography.bodySmall)
                    .foregroundColor(MonytixColors.textSecondary)
                HStack(alignment: .bottom) {
                    Text(formatCurrency(netWorth))
                        .font(MonytixTypography.headlineLarge)
                        .fontWeight(.bold)
                        .foregroundColor(MonytixColors.textPrimary)
                    Spacer()
                    HStack(spacing: 4) {
                        Text(trendPct >= 0 ? "↑" : "↓")
                            .foregroundColor(trendPct >= 0 ? MonytixColors.success : MonytixColors.chartRed)
                        Text("\(Int(abs(trendPct)))% this month")
                            .font(MonytixTypography.labelSmall)
                            .foregroundColor(MonytixColors.textSecondary)
                    }
                }
                if !sparklineData.isEmpty {
                    MiniSparkline(data: sparklineData)
                        .frame(height: 32)
                        .padding(.top, 8)
                }
            }
        }
        .overlay(
            RoundedRectangle(cornerRadius: MonytixLayout.cardRadius)
                .stroke(MonytixColors.accentPrimary.opacity(0.2), lineWidth: 1.5)
        )
    }
}

private struct TodaysIntelligenceCard: View {
    let items: [TodayIntelligenceItem]

    var body: some View {
        GlassCardView {
            VStack(alignment: .leading, spacing: 8) {
                Text("TODAY")
                    .font(MonytixTypography.labelSmall)
                    .fontWeight(.semibold)
                    .foregroundColor(MonytixColors.accentPrimary)
                HStack(spacing: 8) {
                    ForEach(items.indices, id: \.self) { i in
                        Text("• \(items[i].label)")
                            .font(MonytixTypography.bodySmall)
                            .foregroundColor(colorForType(items[i].type))
                    }
                }
            }
        }
        .overlay(
            RoundedRectangle(cornerRadius: MonytixLayout.cardRadius)
                .stroke(MonytixColors.accentPrimary.opacity(0.2), lineWidth: 1)
        )
    }

    private func colorForType(_ type: String) -> Color {
        switch type {
        case "positive": return MonytixColors.success
        case "risk": return MonytixColors.warning
        case "spike", "large": return MonytixColors.chartRed
        default: return MonytixColors.textPrimary
        }
    }
}

private struct DynamicChip: View {
    let label: String
    let isPositive: Bool

    var body: some View {
        Text(label)
            .font(MonytixTypography.labelSmall)
            .foregroundColor(isPositive ? MonytixColors.success : MonytixColors.chartRed)
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background((isPositive ? MonytixColors.success : MonytixColors.chartRed).opacity(0.2))
            .clipShape(RoundedRectangle(cornerRadius: 8))
    }
}

private struct SummaryCard: View {
    let title: String
    let value: String
    let color: Color

    var body: some View {
        GlassCardView {
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(MonytixTypography.bodySmall)
                    .foregroundColor(MonytixColors.textSecondary)
                Text(value)
                    .font(MonytixTypography.titleLarge)
                    .fontWeight(.bold)
                    .foregroundColor(color)
            }
        }
    }
}

private struct SavingsRateRingCard: View {
    let savingsRate: Double

    var body: some View {
        GlassCardView {
            VStack(spacing: 4) {
                ZStack {
                    Circle()
                        .stroke(MonytixColors.textSecondary.opacity(0.2), lineWidth: 5)
                    Circle()
                        .trim(from: 0, to: min(max(savingsRate / 100, 0), 1))
                        .stroke(savingsRate >= 0 ? MonytixColors.accentPrimary : MonytixColors.chartRed, lineWidth: 5)
                        .rotationEffect(.degrees(-90))
                    Text("\(Int(savingsRate))%")
                        .font(MonytixTypography.titleMedium)
                        .fontWeight(.bold)
                        .foregroundColor(savingsRate >= 0 ? MonytixColors.accentPrimary : MonytixColors.chartRed)
                }
                .frame(width: 64, height: 64)
                Text("Savings Rate")
                    .font(MonytixTypography.labelSmall)
                    .foregroundColor(MonytixColors.textSecondary)
            }
        }
    }
}

private struct CategoryBarRow: View {
    let category: String
    let amount: Double
    let fraction: Double

    var body: some View {
        HStack {
            Text(category)
                .font(MonytixTypography.bodySmall)
                .foregroundColor(MonytixColors.textPrimary)
                .frame(width: 100, alignment: .leading)
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 4)
                        .fill(MonytixColors.textSecondary.opacity(0.3))
                    RoundedRectangle(cornerRadius: 4)
                        .fill(MonytixColors.accentPrimary)
                        .frame(width: geo.size.width * CGFloat(min(max(fraction, 0), 1)))
                }
            }
            .frame(height: 8)
            .padding(.horizontal, 8)
            Text(formatCurrency(amount))
                .font(MonytixTypography.labelMedium)
                .foregroundColor(MonytixColors.textPrimary)
                .frame(width: 60, alignment: .trailing)
        }
        .padding(.vertical, 4)
    }
}

private struct RecentTransactionRow: View {
    let transaction: TransactionRecordResponse

    var body: some View {
        let isDebit = transaction.direction.lowercased() == "debit"
        GlassCardView {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(transaction.merchant ?? transaction.category ?? "Transaction")
                        .font(MonytixTypography.titleSmall)
                        .foregroundColor(MonytixColors.textPrimary)
                    Text(transaction.txnDate)
                        .font(MonytixTypography.labelSmall)
                        .foregroundColor(MonytixColors.textSecondary)
                }
                Spacer()
                Text("\(isDebit ? "-" : "+")\(formatCurrency(abs(transaction.amount)))")
                    .font(MonytixTypography.titleSmall)
                    .fontWeight(.bold)
                    .foregroundColor(isDebit ? MonytixColors.chartRed : MonytixColors.success)
            }
        }
    }
}

private struct MiniSparkline: View {
    let data: [Float]

    var body: some View {
        if data.count < 2 {
            EmptyView()
        } else {
            Chart(Array(data.enumerated()), id: \.offset) { item in
                LineMark(
                    x: .value("i", item.offset),
                    y: .value("v", item.element)
                )
                .foregroundStyle(MonytixColors.accentPrimary)
                .lineStyle(StrokeStyle(lineWidth: 2))
            }
            .chartYScale(domain: (data.min() ?? 0)...(data.max() ?? 1))
        }
    }
}

private struct AccountsTab: View {
    let accounts: [AccountItemResponse]
    let onRetry: () -> Void

    var body: some View {
        Group {
            if accounts.isEmpty {
                EmptyStateView(title: "No Accounts", subtitle: "Upload bank statements to see your accounts", onRetry: onRetry)
                    .padding(.top, 40)
            } else {
                let total = accounts.reduce(0) { $0 + $1.balance }
                LazyVStack(alignment: .leading, spacing: 12) {
                    Text("Total Portfolio")
                        .font(MonytixTypography.titleMedium)
                        .fontWeight(.semibold)
                        .foregroundColor(MonytixColors.textSecondary)
                    Text(formatCurrency(total))
                        .font(MonytixTypography.headlineLarge)
                        .fontWeight(.bold)
                        .foregroundColor(total >= 0 ? MonytixColors.success : MonytixColors.chartRed)
                    Text("Your Accounts")
                        .font(MonytixTypography.titleLarge)
                        .fontWeight(.bold)
                        .foregroundColor(MonytixColors.textPrimary)
                    ForEach(accounts, id: \.id) { account in
                        AccountCard(account: account, maxBalance: accounts.map { abs($0.balance) }.max() ?? 1)
                    }
                }
                .padding(16)
            }
        }
    }
}

private struct AccountCard: View {
    let account: AccountItemResponse
    let maxBalance: Double

    var body: some View {
        let isNegative = account.balance < 0
        let fraction = maxBalance > 0 ? min(abs(account.balance) / maxBalance, 1) : 0
        GlassCardView {
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Text(emojiForType(account.accountType ?? ""))
                        .font(.title2)
                    VStack(alignment: .leading, spacing: 2) {
                        HStack {
                            Text(account.bankName ?? "Account")
                                .font(MonytixTypography.titleMedium)
                                .fontWeight(.bold)
                                .foregroundColor(MonytixColors.textPrimary)
                            if isNegative {
                                Text("Overdraft Risk")
                                    .font(MonytixTypography.labelSmall)
                                    .foregroundColor(MonytixColors.chartRed)
                            }
                        }
                        Text("\(account.accountType ?? "")\(account.accountNumber.map { " • \($0)" } ?? "")")
                            .font(MonytixTypography.bodySmall)
                            .foregroundColor(MonytixColors.textSecondary)
                    }
                    Spacer()
                    Text(formatCurrency(account.balance))
                        .font(MonytixTypography.titleMedium)
                        .fontWeight(.bold)
                        .foregroundColor(isNegative ? MonytixColors.chartRed : MonytixColors.textPrimary)
                }
                GeometryReader { geo in
                    ZStack(alignment: .leading) {
                        RoundedRectangle(cornerRadius: 2)
                            .fill(MonytixColors.textSecondary.opacity(0.2))
                        RoundedRectangle(cornerRadius: 2)
                            .fill(isNegative ? MonytixColors.chartRed : MonytixColors.accentPrimary)
                            .frame(width: geo.size.width * fraction)
                    }
                }
                .frame(height: 4)
            }
        }
    }

    private func emojiForType(_ type: String) -> String {
        switch type.uppercased() {
        case "SAVINGS": return "🏦"
        case "CHECKING": return "💳"
        case "INVESTMENT": return "📈"
        case "CREDIT": return "💳"
        default: return "💳"
        }
    }

}

private struct SpendingTab: View {
    let viewModel: HomeViewModel

    var body: some View {
        let categories = viewModel.transformCategorySpending()
        let total = categories.reduce(0) { $0 + $1.amount }
        let vsLastMonth = viewModel.spendingVsLastMonthPct()
        let projected = viewModel.projectedMonthEndSpending()
        let sparklineData = viewModel.sparklineData()
        LazyVStack(alignment: .leading, spacing: 12) {
            GlassCardView {
                VStack(alignment: .leading, spacing: 4) {
                    Text("This Month's Spending")
                        .font(MonytixTypography.bodyMedium)
                        .foregroundColor(MonytixColors.textSecondary)
                    HStack(alignment: .bottom) {
                        Text(formatCurrency(total))
                            .font(MonytixTypography.headlineMedium)
                            .fontWeight(.bold)
                            .foregroundColor(MonytixColors.chartRed)
                        if let pct = vsLastMonth {
                            Text("\(pct >= 0 ? "↑" : "↓") \(Int(abs(pct)))% vs last month")
                                .font(MonytixTypography.labelSmall)
                                .foregroundColor(pct >= 0 ? MonytixColors.chartRed : MonytixColors.success)
                        }
                    }
                    if let proj = projected {
                        Text("Projected: \(formatCurrency(proj)) by month end")
                            .font(MonytixTypography.labelSmall)
                            .foregroundColor(MonytixColors.textSecondary)
                    }
                    if !sparklineData.isEmpty {
                        MiniSparkline(data: sparklineData)
                            .frame(height: 32)
                            .padding(.top, 8)
                    }
                }
            }
            if categories.isEmpty {
                EmptyStateView(title: "No Spending Data", subtitle: "Upload statements to see your spending breakdown")
                    .padding(.top, 20)
            } else {
                Text("Spending by Category")
                    .font(MonytixTypography.titleMedium)
                    .fontWeight(.bold)
                    .foregroundColor(MonytixColors.textPrimary)
                ForEach(categories, id: \.category) { cat in
                    GlassCardView {
                        VStack(alignment: .leading, spacing: 8) {
                            HStack {
                                Text(cat.category)
                                    .font(MonytixTypography.titleSmall)
                                    .foregroundColor(MonytixColors.textPrimary)
                                Spacer()
                                Text(formatCurrency(cat.amount))
                                    .font(MonytixTypography.titleSmall)
                                    .fontWeight(.bold)
                                    .foregroundColor(MonytixColors.textPrimary)
                            }
                            GeometryReader { geo in
                                ZStack(alignment: .leading) {
                                    RoundedRectangle(cornerRadius: 4)
                                        .fill(MonytixColors.textSecondary.opacity(0.3))
                                    RoundedRectangle(cornerRadius: 4)
                                        .fill(MonytixColors.accentPrimary)
                                        .frame(width: geo.size.width * CGFloat(cat.percentage / 100))
                                }
                            }
                            .frame(height: 8)
                            Text("\(cat.transactionCount) transactions")
                                .font(MonytixTypography.labelSmall)
                                .foregroundColor(MonytixColors.textSecondary)
                        }
                    }
                }
            }
        }
        .padding(16)
    }
}

private struct GoalsTab: View {
    let viewModel: HomeViewModel

    var body: some View {
        let goals = viewModel.transformGoals()
        Group {
            if goals.isEmpty {
                EmptyStateView(title: "No Goals", subtitle: "Create your first financial goal to get started")
                    .padding(.top, 40)
            } else {
                LazyVStack(alignment: .leading, spacing: 12) {
                    Text("Your Goals")
                        .font(MonytixTypography.titleLarge)
                        .fontWeight(.bold)
                        .foregroundColor(MonytixColors.textPrimary)
                    ForEach(goals, id: \.id) { goal in
                        GoalCard(goal: goal)
                    }
                }
                .padding(16)
            }
        }
    }
}

private struct GoalCard: View {
    let goal: ConsoleGoal

    var body: some View {
        let progress = goal.targetAmount > 0 ? (goal.savedAmount / goal.targetAmount * 100) : 0
        let remaining = max(goal.targetAmount - goal.savedAmount, 0)
        let pctLeft = 100 - progress
        let color: Color = progress >= 80 ? MonytixColors.accentPrimary : (progress >= 40 ? MonytixColors.accentPrimary : MonytixColors.warning)
        GlassCardView {
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Text(goal.name)
                        .font(MonytixTypography.titleMedium)
                        .fontWeight(.bold)
                        .foregroundColor(MonytixColors.textPrimary)
                    if goal.isActive {
                        Text("Active")
                            .font(MonytixTypography.labelSmall)
                            .foregroundColor(MonytixColors.success)
                    }
                }
                Text(pctLeft > 0 ? "\(Int(pctLeft))% left to finish" : "Complete")
                    .font(MonytixTypography.bodySmall)
                    .foregroundColor(color)
                HStack {
                    Text("Progress")
                        .font(MonytixTypography.bodySmall)
                        .foregroundColor(MonytixColors.textSecondary)
                    Spacer()
                    Text("\(Int(progress))%")
                        .font(MonytixTypography.bodySmall)
                        .fontWeight(.semibold)
                        .foregroundColor(color)
                }
                GeometryReader { geo in
                    ZStack(alignment: .leading) {
                        RoundedRectangle(cornerRadius: 6)
                            .fill(MonytixColors.textSecondary.opacity(0.3))
                        RoundedRectangle(cornerRadius: 6)
                            .fill(color)
                            .frame(width: geo.size.width * CGFloat(progress / 100))
                    }
                }
                .frame(height: 12)
                HStack {
                    VStack {
                        Text("Saved")
                            .font(MonytixTypography.labelSmall)
                            .foregroundColor(MonytixColors.textSecondary)
                        Text(formatCurrency(goal.savedAmount))
                            .font(MonytixTypography.bodyMedium)
                            .fontWeight(.semibold)
                            .foregroundColor(MonytixColors.textPrimary)
                    }
                    Spacer()
                    VStack {
                        Text("Target")
                            .font(MonytixTypography.labelSmall)
                            .foregroundColor(MonytixColors.textSecondary)
                        Text(formatCurrency(goal.targetAmount))
                            .font(MonytixTypography.bodyMedium)
                            .fontWeight(.semibold)
                            .foregroundColor(MonytixColors.textPrimary)
                    }
                    Spacer()
                    VStack {
                        Text("Remaining")
                            .font(MonytixTypography.labelSmall)
                            .foregroundColor(MonytixColors.textSecondary)
                        Text(formatCurrency(remaining))
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

private struct AIInsightTab: View {
    let viewModel: HomeViewModel

    var body: some View {
        let insights = viewModel.generateAiInsights()
        Group {
            if insights.isEmpty {
                VStack(spacing: 16) {
                    Image(systemName: "sparkles")
                        .font(.system(size: 48))
                        .foregroundColor(MonytixColors.accentSecondary.opacity(0.6))
                    Text("AI is analyzing your patterns")
                        .font(MonytixTypography.titleMedium)
                        .fontWeight(.semibold)
                        .foregroundColor(MonytixColors.textPrimary)
                    Text("Check back soon for personalized insights.")
                        .font(MonytixTypography.bodyMedium)
                        .foregroundColor(MonytixColors.textSecondary)
                }
                .frame(maxWidth: .infinity)
                .padding(32)
            } else {
                LazyVStack(alignment: .leading, spacing: 12) {
                    Text("AI Insights")
                        .font(MonytixTypography.titleLarge)
                        .fontWeight(.bold)
                        .foregroundColor(MonytixColors.textPrimary)
                    ForEach(insights, id: \.id) { insight in
                        AIInsightCard(insight: insight)
                    }
                }
                .padding(16)
            }
        }
    }
}

private struct AIInsightCard: View {
    let insight: AiInsight

    var body: some View {
        let color: Color = {
            switch insight.type {
            case "optimization", "goal_progress": return MonytixColors.success
            case "risk", "budget_tip": return MonytixColors.warning
            case "pattern": return MonytixColors.info
            default: return MonytixColors.accentSecondary
            }
        }()
        GlassCardView {
            HStack(alignment: .top, spacing: 16) {
                Text(emojiForType(insight.type))
                    .font(.title2)
                VStack(alignment: .leading, spacing: 4) {
                    Text(insight.title)
                        .font(MonytixTypography.titleMedium)
                        .fontWeight(.bold)
                        .foregroundColor(color)
                    Text(insight.message)
                        .font(MonytixTypography.bodyMedium)
                        .foregroundColor(MonytixColors.textPrimary.opacity(0.9))
                    if let conf = insight.confidence, conf > 0.8 {
                        Text("\(Int(conf * 100))% confidence")
                            .font(MonytixTypography.labelSmall)
                            .foregroundColor(MonytixColors.textSecondary)
                    }
                }
            }
        }
        .overlay(
            RoundedRectangle(cornerRadius: MonytixLayout.cardRadius)
                .stroke(color.opacity(0.4), lineWidth: 1)
        )
    }

    private func emojiForType(_ type: String) -> String {
        switch type {
        case "optimization", "goal_progress": return "🟢"
        case "risk", "budget_tip": return "🟡"
        case "pattern": return "🔵"
        default: return "✨"
        }
    }
}

#Preview {
    HomeView()
        .environment(AuthViewModel())
}
