//
//  SpendSenseView.swift
//  ios_monytix
//
//  SpendSense screen: tabs Categories, Transactions, Insights. Mirrors APK SpendSenseScreen.
//

import SwiftUI
import UniformTypeIdentifiers

enum SpendSenseTab: String, CaseIterable {
    case categories = "Categories"
    case transactions = "Transactions"
    case insights = "Insights"
}

struct SpendSenseView: View {
    @StateObject private var viewModel = SpendSenseViewModel()
    @State private var selectedTab: SpendSenseTab = .categories
    @State private var showAddTransaction = false
    @State private var showFileImporter = false
    @State private var showUploadConfirmSheet = false
    @State private var pendingUploadData: Data?
    @State private var pendingUploadFilename: String?
    @State private var uploadMessage: String?
    @State private var isUploading = false

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                welcomeBanner
                tabBar
                if viewModel.isLoading && viewModel.kpis == nil && viewModel.transactions.isEmpty {
                    Spacer()
                    MonytixRingLoader(size: 44, lineWidth: 4)
                    Spacer()
                } else {
                    tabContent
                }
            }
            .background(MonytixTheme.bg.ignoresSafeArea())
            .navigationTitle("SpendSense")
            .navigationBarTitleDisplayMode(.inline)
            .toolbarBackground(MonytixTheme.bg, for: .navigationBar)
            .toolbarColorScheme(.dark, for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    HStack(spacing: 8) {
                        Button {
                            showFileImporter = true
                        } label: {
                            Image(systemName: "square.and.arrow.up")
                        }
                        .disabled(isUploading)
                        Button {
                            showAddTransaction = true
                        } label: {
                            Image(systemName: "plus.circle")
                        }
                    }
                }
            }
            .sheet(isPresented: $showAddTransaction) {
                AddTransactionSheet(isPresented: $showAddTransaction) {
                    viewModel.loadTransactions(page: 1, append: false)
                    viewModel.loadKpis(month: viewModel.selectedMonth)
                    viewModel.loadDebitCreditSummary()
                    viewModel.loadInsights()
                }
            }
            .fileImporter(
                isPresented: $showFileImporter,
                allowedContentTypes: [.pdf, .commaSeparatedText, .plainText, .data],
                allowsMultipleSelection: false
            ) { result in
                handleFileImport(result: result)
            }
            .sheet(isPresented: $showUploadConfirmSheet) {
                if let data = pendingUploadData, let name = pendingUploadFilename {
                    UploadConfirmSheet(
                        isPresented: $showUploadConfirmSheet,
                        filename: name,
                        isUploading: isUploading,
                        onUpload: { password in
                            performUpload(fileData: data, filename: name, password: password)
                        },
                        onCancel: {
                            pendingUploadData = nil
                            pendingUploadFilename = nil
                            showUploadConfirmSheet = false
                        }
                    )
                }
            }
            .overlay(alignment: .top) {
                if let msg = uploadMessage {
                    Text(msg)
                        .font(.caption)
                        .foregroundStyle(MonytixTheme.text1)
                        .padding(.horizontal, MonytixSpace.md)
                        .padding(.vertical, MonytixSpace.sm)
                        .background(MonytixTheme.surface2)
                        .clipShape(RoundedRectangle(cornerRadius: MonytixShape.smallRadius))
                        .padding(.top, MonytixSpace.md)
                        .onAppear {
                            DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) {
                                uploadMessage = nil
                            }
                        }
                }
            }
            .refreshable {
                await viewModel.refreshAsync()
            }
            .onAppear {
                viewModel.loadKpis(month: viewModel.selectedMonth)
                viewModel.loadTransactions(page: 1, append: false)
                viewModel.loadInsights()
                viewModel.loadDebitCreditSummary()
            }
        }
    }

    private func handleFileImport(result: Result<[URL], Error>) {
        guard case .success(let urls) = result, let url = urls.first else {
            uploadMessage = "No file selected"
            return
        }
        Task { @MainActor in
            guard url.startAccessingSecurityScopedResource() else {
                uploadMessage = "Could not access file"
                return
            }
            defer { url.stopAccessingSecurityScopedResource() }
            guard let data = try? Data(contentsOf: url) else {
                uploadMessage = "Could not read file"
                return
            }
            pendingUploadData = data
            pendingUploadFilename = url.lastPathComponent
            showUploadConfirmSheet = true
        }
    }

    private func performUpload(fileData: Data, filename: String, password: String?) {
        isUploading = true
        uploadMessage = nil
        Task { @MainActor in
            defer {
                isUploading = false
                pendingUploadData = nil
                pendingUploadFilename = nil
                showUploadConfirmSheet = false
            }
            guard let token = await AuthManager.shared.getIdToken() else {
                uploadMessage = "Please sign in again"
                return
            }
            let uploadResult = await BackendApi.uploadStatement(accessToken: token, fileData: fileData, filename: filename, password: password)
            switch uploadResult {
            case .success(let batch):
                viewModel.loadTransactions(page: 1, append: false)
                viewModel.loadKpis(month: viewModel.selectedMonth)
                viewModel.loadDebitCreditSummary()
                viewModel.loadInsights()
                uploadMessage = "Upload started • \(batch.status)"
            case .failure(let e):
                uploadMessage = e.localizedDescription
            }
        }
    }

    private var welcomeBanner: some View {
        let name = (viewModel.userEmail?.split(separator: "@").first).map(String.init) ?? "User"
        return VStack(alignment: .leading, spacing: 4) {
            Text("SpendSense")
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(MonytixTheme.text1)
            Text("Welcome, \(name) • Track spending & insights")
                .font(.system(size: 13, weight: .medium))
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
                ForEach(SpendSenseTab.allCases, id: \.self) { tab in
                    let selected = tab == selectedTab
                    Button {
                        selectedTab = tab
                    } label: {
                        Text(tab.rawValue)
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
        case .categories:
            SpendSenseCategoriesTab(viewModel: viewModel)
        case .transactions:
            SpendSenseTransactionsTab(viewModel: viewModel)
        case .insights:
            SpendSenseInsightsTab(viewModel: viewModel)
        }
    }
}

// MARK: - Categories Tab

private struct SpendSenseCategoriesTab: View {
    @ObservedObject var viewModel: SpendSenseViewModel

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: MonytixSpace.lg) {
                if let err = viewModel.errorMessage {
                    Text(err)
                        .font(.caption)
                        .foregroundStyle(MonytixTheme.danger)
                        .lineLimit(3)
                }
                smartSummaryCard
                financialHealthCard
                categoryBarsSection
            }
            .padding(MonytixSpace.md)
            .padding(.bottom, MonytixSpace.xl)
        }
    }

    private var smartSummaryCard: some View {
        let total = viewModel.totalSpend()
        let mom = viewModel.spendingMomPct()
        let highest = viewModel.highestCategory()
        return VStack(alignment: .leading, spacing: 8) {
            if total <= 0 {
                Text("No transactions yet")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundStyle(MonytixTheme.text1)
                Text("Upload a statement or add a transaction to get started.")
                    .font(.system(size: 13))
                    .foregroundStyle(MonytixTheme.text2)
            } else {
                Text(spendSenseFormatCurrency(total))
                    .font(.system(size: 26, weight: .bold))
                    .foregroundStyle(MonytixTheme.danger)
                if let mom = mom {
                    Text("\(mom >= 0 ? "↑" : "↓") \(Int(abs(mom)))% vs last month")
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(mom >= 0 ? MonytixTheme.danger : MonytixTheme.success)
                }
                if let (name, pct) = highest {
                    Text("Highest spend: \(name) (\(pct)%)")
                        .font(.system(size: 12))
                        .foregroundStyle(MonytixTheme.text2)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(MonytixSpace.lg)
        .background(MonytixTheme.surface)
        .clipShape(RoundedRectangle(cornerRadius: MonytixShape.cardRadius))
    }

    private var financialHealthCard: some View {
        let score = viewModel.financialHealthScore()
        let level = viewModel.riskLevel(score: score)
        return VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("Financial health")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(MonytixTheme.text1)
                Spacer()
                Text("\(score)")
                    .font(.system(size: 24, weight: .bold))
                    .foregroundStyle(MonytixTheme.cyan1)
            }
            Text("Risk: \(level)")
                .font(.system(size: 12))
                .foregroundStyle(MonytixTheme.text2)
            GeometryReader { g in
                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 6)
                        .fill(MonytixTheme.stroke.opacity(0.5))
                        .frame(height: 10)
                    RoundedRectangle(cornerRadius: 6)
                        .fill(MonytixTheme.cyan1)
                        .frame(width: g.size.width * CGFloat(score) / 100, height: 10)
                }
            }
            .frame(height: 10)
        }
        .padding(MonytixSpace.md)
        .background(MonytixTheme.surface)
        .clipShape(RoundedRectangle(cornerRadius: MonytixShape.cardRadius))
    }

    private var categoryBarsSection: some View {
        let breakdown = viewModel.categoryBreakdownForChart()
        let total = breakdown.map(\.amount).reduce(0, +)
        let deltaByCat = viewModel.deltaByCategory()
        return VStack(alignment: .leading, spacing: MonytixSpace.sm) {
            Text("Spending by category")
                .font(.system(size: 18, weight: .bold))
                .foregroundStyle(MonytixTheme.text1)
            if breakdown.isEmpty {
                Text("As you add data, we'll surface categories and insights here.")
                    .font(.system(size: 14))
                    .foregroundStyle(MonytixTheme.text2)
            } else {
                let top6 = Array(breakdown.prefix(6))
                let maxAmount = top6.map(\.amount).max() ?? 1
                ForEach(Array(top6.enumerated()), id: \.offset) { _, item in
                    let sharePct = total > 0 ? Int(item.amount / total * 100) : 0
                    let delta = item.categoryCode.flatMap { deltaByCat[$0] }
                    VStack(alignment: .leading, spacing: 4) {
                        HStack {
                            Text(item.categoryName ?? item.categoryCode ?? "Unknown")
                                .font(.system(size: 14, weight: .medium))
                                .foregroundStyle(MonytixTheme.text1)
                            Spacer()
                            Text(spendSenseFormatCurrency(item.amount))
                                .font(.system(size: 14, weight: .semibold))
                                .foregroundStyle(MonytixTheme.text1)
                        }
                        HStack(spacing: 8) {
                            GeometryReader { g in
                                ZStack(alignment: .leading) {
                                    RoundedRectangle(cornerRadius: 4)
                                        .fill(MonytixTheme.stroke.opacity(0.4))
                                        .frame(height: 8)
                                    RoundedRectangle(cornerRadius: 4)
                                        .fill(MonytixTheme.cyan1)
                                        .frame(width: maxAmount > 0 ? g.size.width * CGFloat(item.amount / maxAmount) : 0, height: 8)
                                }
                            }
                            .frame(height: 8)
                            Text("\(sharePct)%")
                                .font(.system(size: 11))
                                .foregroundStyle(MonytixTheme.text2)
                        }
                        if let delta = delta {
                            Text("\(delta >= 0 ? "↑" : "↓") \(Int(abs(delta)))% vs last month")
                                .font(.system(size: 11))
                                .foregroundStyle(MonytixTheme.text2)
                        }
                    }
                    .padding(MonytixSpace.sm)
                    .background(MonytixTheme.surface2)
                    .clipShape(RoundedRectangle(cornerRadius: MonytixShape.smallRadius))
                }
            }
        }
    }
}

// MARK: - Transactions Tab

private struct SpendSenseTransactionsTab: View {
    @ObservedObject var viewModel: SpendSenseViewModel

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: MonytixSpace.md) {
                if let err = viewModel.errorMessage {
                    Text(err)
                        .font(.caption)
                        .foregroundStyle(MonytixTheme.danger)
                        .lineLimit(3)
                }
                if let sum = viewModel.debitCreditSummary {
                    HStack(spacing: MonytixSpace.md) {
                        summaryChip(title: "Debits", value: spendSenseFormatCurrency(sum.debitTotal), count: sum.debitCount, color: MonytixTheme.danger)
                        summaryChip(title: "Credits", value: spendSenseFormatCurrency(sum.creditTotal), count: sum.creditCount, color: MonytixTheme.success)
                    }
                }
                Text("Transactions (\(viewModel.transactionsTotal))")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundStyle(MonytixTheme.text1)
                if viewModel.transactions.isEmpty {
                    Text("No transactions yet. Upload a statement or add a transaction to get started.")
                        .font(.system(size: 14))
                        .foregroundStyle(MonytixTheme.text2)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, MonytixSpace.xl)
                } else {
                    ForEach(viewModel.transactions, id: \.txnId) { t in
                        transactionRow(t)
                    }
                }
            }
            .padding(MonytixSpace.md)
            .padding(.bottom, MonytixSpace.xl)
        }
    }

    private func summaryChip(title: String, value: String, count: Int, color: Color) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .font(.system(size: 11, weight: .medium))
                .foregroundStyle(MonytixTheme.text2)
            Text(value)
                .font(.system(size: 16, weight: .bold))
                .foregroundStyle(color)
            Text("\(count) txns")
                .font(.system(size: 11))
                .foregroundStyle(MonytixTheme.text2)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(MonytixSpace.md)
        .background(MonytixTheme.surface)
        .clipShape(RoundedRectangle(cornerRadius: MonytixShape.smallRadius))
    }

    private func transactionRow(_ t: TransactionRecordResponse) -> some View {
        let isDebit = t.direction.lowercased() == "debit"
        return HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(t.merchant ?? t.category ?? "Transaction")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(MonytixTheme.text1)
                Text(t.txnDate)
                    .font(.system(size: 11))
                    .foregroundStyle(MonytixTheme.text2)
            }
            Spacer()
            Text("\(isDebit ? "-" : "+")\(spendSenseFormatCurrency(abs(t.amount)))")
                .font(.system(size: 14, weight: .bold))
                .foregroundStyle(isDebit ? MonytixTheme.danger : MonytixTheme.success)
        }
        .padding(MonytixSpace.sm)
        .background(MonytixTheme.surface)
        .clipShape(RoundedRectangle(cornerRadius: MonytixShape.smallRadius))
    }
}

// MARK: - Insights Tab

private struct SpendSenseInsightsTab: View {
    @ObservedObject var viewModel: SpendSenseViewModel

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: MonytixSpace.lg) {
                if let err = viewModel.errorMessage {
                    Text(err)
                        .font(.caption)
                        .foregroundStyle(MonytixTheme.danger)
                        .lineLimit(3)
                }
                insightBanner
                if let trends = viewModel.insights?.spendingTrends, !trends.isEmpty {
                    Text("Spending trends")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundStyle(MonytixTheme.text1)
                    ForEach(Array(trends.suffix(5).enumerated()), id: \.offset) { _, item in
                        HStack {
                            Text(item.period ?? "—")
                                .font(.system(size: 14, weight: .medium))
                                .foregroundStyle(MonytixTheme.text1)
                            Spacer()
                            if let net = item.net {
                                Text(spendSenseFormatCurrency(net))
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundStyle(net >= 0 ? MonytixTheme.success : MonytixTheme.danger)
                            }
                        }
                        .padding(MonytixSpace.sm)
                        .background(MonytixTheme.surface)
                        .clipShape(RoundedRectangle(cornerRadius: MonytixShape.smallRadius))
                    }
                }
                if let ts = viewModel.insights?.timeSeries, !ts.isEmpty {
                    Text("Time series")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundStyle(MonytixTheme.text1)
                    SpendSenseSparklineView(values: ts.map { $0.value })
                        .frame(height: 60)
                }
            }
            .padding(MonytixSpace.md)
            .padding(.bottom, MonytixSpace.xl)
        }
    }

    private var insightBanner: some View {
        let score = viewModel.financialHealthScore()
        let level = viewModel.riskLevel(score: score)
        return VStack(alignment: .leading, spacing: 8) {
            Text("Your spending at a glance")
                .font(.system(size: 16, weight: .bold))
                .foregroundStyle(MonytixTheme.text1)
            Text("Health score \(score) • \(level) risk. \(viewModel.insights?.categoryBreakdown?.isEmpty == false ? "Review categories below for details." : "Upload more data for richer insights.")")
                .font(.system(size: 13))
                .foregroundStyle(MonytixTheme.text2)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(MonytixSpace.md)
        .background(MonytixTheme.surface)
        .clipShape(RoundedRectangle(cornerRadius: MonytixShape.cardRadius))
    }
}

// MARK: - Sparkline (local)

private struct SpendSenseSparklineView: View {
    let values: [Double]
    var body: some View {
        GeometryReader { g in
            let w = g.size.width
            let h = g.size.height
            let pts = values.map { Double($0) }
            if pts.count >= 2 {
                let minVal = pts.min() ?? 0
                let maxVal = pts.max() ?? 1
                let range = maxVal - minVal > 0 ? maxVal - minVal : 1
                let step = w / CGFloat(pts.count - 1)
                Path { path in
                    for i in 0..<(pts.count - 1) {
                        let x1 = CGFloat(i) * step
                        let y1 = h - CGFloat((pts[i] - minVal) / range) * (h - 4) - 2
                        let x2 = CGFloat(i + 1) * step
                        let y2 = h - CGFloat((pts[i + 1] - minVal) / range) * (h - 4) - 2
                        path.move(to: CGPoint(x: x1, y: max(2, min(h - 2, y1))))
                        path.addLine(to: CGPoint(x: x2, y: max(2, min(h - 2, y2))))
                    }
                }
                .stroke(MonytixTheme.cyan1, lineWidth: 2)
            }
        }
    }
}

// MARK: - Helpers

private func spendSenseFormatCurrency(_ amount: Double) -> String {
    let absAmount = abs(amount)
    let formatter = NumberFormatter()
    formatter.numberStyle = .decimal
    formatter.maximumFractionDigits = 0
    let formatted = formatter.string(from: NSNumber(value: absAmount)) ?? "\(Int(absAmount))"
    return amount < 0 ? "-₹\(formatted)" : "₹\(formatted)"
}

#Preview {
    SpendSenseView()
}
