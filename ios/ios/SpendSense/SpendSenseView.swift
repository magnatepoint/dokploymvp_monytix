//
//  SpendSenseView.swift
//  ios
//
//  SpendSense - matches APK SpendSenseScreen
//

import SwiftUI
import Charts

struct SpendSenseView: View {
    @Environment(AuthViewModel.self) private var authViewModel
    @State private var viewModel = SpendSenseViewModel()
    @State private var selectedTab: SpendSenseTab = .categories

    var body: some View {
        NavigationStack {
            PremiumGradientBackground {
                VStack(spacing: 0) {
                    TabBar(selectedTab: $selectedTab)

                    if viewModel.isLoading && viewModel.transactions.isEmpty && viewModel.categoryBreakdown.isEmpty {
                        Spacer()
                        ProgressView()
                            .tint(MonytixColors.accentPrimary)
                        Spacer()
                    } else {
                        ScrollView {
                            switch selectedTab {
                            case .categories:
                                CategoriesTab(viewModel: viewModel)
                            case .transactions:
                                TransactionsTab(viewModel: viewModel)
                            }
                        }
                    }
                }
            }
            .navigationTitle("SpendSense")
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
                AnalyticsHelper.logScreenView(screenName: "spendsense")
            }
        }
    }
}

private enum SpendSenseTab: String, CaseIterable {
    case categories
    case transactions

    var label: String {
        switch self {
        case .categories: return "Categories"
        case .transactions: return "Transactions"
        }
    }
}

private struct TabBar: View {
    @Binding var selectedTab: SpendSenseTab

    var body: some View {
        HStack(spacing: 0) {
            ForEach(SpendSenseTab.allCases, id: \.rawValue) { tab in
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

private struct CategoriesTab: View {
    let viewModel: SpendSenseViewModel

    var body: some View {
        Group {
            if viewModel.categoryBreakdown.isEmpty {
                EmptyStateView(title: "No category data", subtitle: "Upload statements to see spending by category")
                    .padding(.top, 40)
            } else {
                let total = viewModel.categoryBreakdown.reduce(0) { $0 + $1.amount }
                LazyVStack(alignment: .leading, spacing: 16) {
                    if total > 0 {
                        Chart(Array(viewModel.categoryBreakdown.enumerated()), id: \.offset) { _, item in
                            SectorMark(
                                angle: .value("Amount", item.amount),
                                innerRadius: .ratio(0.5),
                                angularInset: 2
                            )
                            .foregroundStyle(by: .value("Category", item.categoryName ?? item.categoryCode ?? ""))
                        }
                        .frame(height: 200)
                        .chartLegend(position: .bottom)

                    Text("Total: \(formatCurrency(total))")
                        .font(MonytixTypography.titleMedium)
                        .fontWeight(.bold)
                        .foregroundColor(MonytixColors.textPrimary)
                    }

                    Text("By Category")
                        .font(MonytixTypography.titleLarge)
                        .fontWeight(.bold)
                        .foregroundColor(MonytixColors.textPrimary)

                    ForEach(Array(viewModel.categoryBreakdown.enumerated()), id: \.offset) { _, cat in
                        GlassCardView {
                            HStack {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(cat.categoryName ?? cat.categoryCode ?? "Unknown")
                                        .font(MonytixTypography.titleSmall)
                                        .foregroundColor(MonytixColors.textPrimary)
                                    Text("\(cat.transactionCount ?? 0) transactions • \(Int(cat.percentage))%")
                                        .font(MonytixTypography.labelSmall)
                                        .foregroundColor(MonytixColors.textSecondary)
                                }
                                Spacer()
                                Text(formatCurrency(cat.amount))
                                    .font(MonytixTypography.titleMedium)
                                    .fontWeight(.bold)
                                    .foregroundColor(MonytixColors.chartRed)
                            }
                        }
                    }
                }
                .padding(16)
            }
        }
    }
}

private struct TransactionsTab: View {
    let viewModel: SpendSenseViewModel

    var body: some View {
        Group {
            if viewModel.transactions.isEmpty {
                EmptyStateView(title: "No transactions", subtitle: "Upload statements or add transactions manually")
                    .padding(.top, 40)
            } else {
                LazyVStack(alignment: .leading, spacing: 12) {
                    Text("\(viewModel.transactionsTotal) transactions")
                        .font(MonytixTypography.labelMedium)
                        .foregroundColor(MonytixColors.textSecondary)

                    ForEach(viewModel.transactions, id: \.txnId) { txn in
                        TransactionRow(transaction: txn)
                    }
                }
                .padding(16)
            }
        }
    }
}

private struct TransactionRow: View {
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

#Preview {
    SpendSenseView()
        .environment(AuthViewModel())
}
