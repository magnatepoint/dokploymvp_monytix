//
//  SpendSenseViewModel.swift
//  ios_monytix
//
//  SpendSense state and data. Mirrors APK SpendSenseViewModel: KPIs, categories, transactions, insights, filters.
//

import Combine
import Foundation
import SwiftUI

// MARK: - Transaction filters (mirrors APK TransactionFilters)

struct SpendSenseTransactionFilters {
    var categoryCode: String?
    var subcategoryCode: String?
    var channel: String?
    var direction: String?
    var bankCode: String?
    var startDate: String?
    var endDate: String?
}

// MARK: - Debit/Credit summary (mirrors APK DebitCreditSummary)

struct DebitCreditSummary {
    let debitTotal: Double
    let creditTotal: Double
    let debitCount: Int
    let creditCount: Int
}

// MARK: - ViewModel

final class SpendSenseViewModel: ObservableObject {
    @Published private(set) var kpis: KpiResponse?
    @Published private(set) var availableMonths: [String] = []
    @Published var selectedMonth: String?
    @Published private(set) var transactions: [TransactionRecordResponse] = []
    @Published private(set) var transactionsTotal: Int = 0
    @Published private(set) var insights: InsightsResponse?
    @Published private(set) var categories: [CategoryResponse] = []
    @Published private(set) var subcategories: [SubcategoryResponse] = []
    @Published private(set) var channels: [String] = []
    @Published private(set) var accounts: [AccountItemResponse] = []
    @Published private(set) var userEmail: String?
    @Published private(set) var isLoading = false
    @Published var errorMessage: String?
    @Published var searchQuery: String = ""
    @Published var transactionFilters: SpendSenseTransactionFilters = SpendSenseTransactionFilters()
    @Published private(set) var debitCreditSummary: DebitCreditSummary?

    private var loadTask: Task<Void, Never>?

    init() {
        loadSession()
        loadCategories()
        loadChannels()
        loadAccounts()
    }

    func loadSession() {
        Task { @MainActor in
            guard let token = await AuthManager.shared.getIdToken() else { return }
            let result = await BackendApi.getSession(accessToken: token)
            if case .success(let session) = result {
                userEmail = session.email
            }
        }
    }

    func loadCategories() {
        Task { @MainActor in
            guard let token = await AuthManager.shared.getIdToken() else { return }
            let result = await BackendApi.getCategories(accessToken: token)
            if case .success(let list) = result {
                categories = list
            }
        }
    }

    func loadChannels() {
        Task { @MainActor in
            guard let token = await AuthManager.shared.getIdToken() else { return }
            let result = await BackendApi.getChannels(accessToken: token)
            if case .success(let list) = result {
                channels = list
            }
        }
    }

    func loadAccounts() {
        Task { @MainActor in
            guard let token = await AuthManager.shared.getIdToken() else { return }
            let result = await BackendApi.getAccounts(accessToken: token)
            if case .success(let resp) = result {
                accounts = resp.accounts
            }
        }
    }

    func loadKpis(month: String? = nil) {
        loadTask?.cancel()
        loadTask = Task { @MainActor in
            isLoading = true
            errorMessage = nil
            defer { isLoading = false }
            guard let token = await AuthManager.shared.getIdToken() else {
                errorMessage = "Not signed in."
                return
            }
            async let kpisResult = BackendApi.getKpis(accessToken: token, month: month)
            async let monthsResult = BackendApi.getAvailableMonths(accessToken: token)
            let (kpisRes, monthsRes) = await (kpisResult, monthsResult)
            if let kpis = try? kpisRes.get() {
                self.kpis = kpis
                selectedMonth = month ?? selectedMonth
            }
            if let monthsResp = try? monthsRes.get() {
                availableMonths = monthsResp.data
            }
            if case .failure(let err) = kpisRes {
                errorMessage = err.localizedDescription
            }
        }
    }

    func loadTransactions(page: Int = 1, append: Bool = false) {
        let limit = 25
        let offset = (page - 1) * limit
        Task { @MainActor in
            guard let token = await AuthManager.shared.getIdToken() else { return }
            isLoading = true
            errorMessage = nil
            defer { isLoading = false }
            let f = transactionFilters
            let result = await BackendApi.getTransactions(
                accessToken: token,
                limit: limit,
                offset: offset,
                search: searchQuery.isEmpty ? nil : searchQuery,
                categoryCode: f.categoryCode,
                subcategoryCode: f.subcategoryCode,
                channel: f.channel,
                direction: f.direction,
                bankCode: f.bankCode,
                startDate: f.startDate,
                endDate: f.endDate
            )
            switch result {
            case .success(let resp):
                transactions = append ? transactions + resp.transactions : resp.transactions
                transactionsTotal = resp.total ?? resp.transactions.count
            case .failure(let err):
                errorMessage = err.localizedDescription
            }
        }
    }

    func loadInsights(startDate: String? = nil, endDate: String? = nil) {
        Task { @MainActor in
            guard let token = await AuthManager.shared.getIdToken() else { return }
            isLoading = true
            errorMessage = nil
            defer { isLoading = false }
            let result = await BackendApi.getInsights(accessToken: token, startDate: startDate, endDate: endDate)
            switch result {
            case .success(let ins):
                insights = ins
            case .failure(let err):
                errorMessage = err.localizedDescription
            }
        }
    }

    func loadSubcategories(categoryCode: String?) {
        Task { @MainActor in
            guard let token = await AuthManager.shared.getIdToken() else { return }
            let result = await BackendApi.getSubcategories(accessToken: token, categoryCode: categoryCode)
            if case .success(let list) = result {
                subcategories = list
            }
        }
    }

    func loadDebitCreditSummary() {
        Task { @MainActor in
            guard let token = await AuthManager.shared.getIdToken() else { return }
            let f = transactionFilters
            let result = await BackendApi.getTransactionsSummary(
                accessToken: token,
                startDate: f.startDate,
                endDate: f.endDate,
                direction: f.direction,
                categoryCode: f.categoryCode,
                subcategoryCode: f.subcategoryCode,
                channel: f.channel,
                bankCode: f.bankCode,
                search: searchQuery.isEmpty ? nil : searchQuery
            )
            if case .success(let resp) = result {
                debitCreditSummary = DebitCreditSummary(
                    debitTotal: resp.debitTotal ?? 0,
                    creditTotal: resp.creditTotal ?? 0,
                    debitCount: resp.debitCount ?? 0,
                    creditCount: resp.creditCount ?? 0
                )
            } else {
                debitCreditSummary = nil
            }
        }
    }

    func setSelectedMonth(_ month: String?) {
        selectedMonth = month
        loadKpis(month: month)
    }

    func setTransactionFilters(_ filters: SpendSenseTransactionFilters) {
        transactionFilters = filters
        loadTransactions(page: 1, append: false)
        loadDebitCreditSummary()
    }

    func applyFiltersAndLoad() {
        loadTransactions(page: 1, append: false)
        loadDebitCreditSummary()
    }

    func refresh() {
        loadKpis(month: selectedMonth)
        loadTransactions(page: 1, append: false)
        loadInsights()
        loadDebitCreditSummary()
    }

    /// Call from SwiftUI .refreshable { await viewModel.refreshAsync() }
    func refreshAsync() async {
        guard let token = await AuthManager.shared.getIdToken() else { return }
        await MainActor.run { isLoading = true; errorMessage = nil }
        defer { Task { @MainActor in isLoading = false } }
        async let kpisRes = BackendApi.getKpis(accessToken: token, month: selectedMonth)
        async let monthsRes = BackendApi.getAvailableMonths(accessToken: token)
        async let txnsRes = BackendApi.getTransactions(
            accessToken: token,
            limit: 25,
            offset: 0,
            search: searchQuery.isEmpty ? nil : searchQuery,
            categoryCode: transactionFilters.categoryCode,
            subcategoryCode: transactionFilters.subcategoryCode,
            channel: transactionFilters.channel,
            direction: transactionFilters.direction,
            bankCode: transactionFilters.bankCode,
            startDate: transactionFilters.startDate,
            endDate: transactionFilters.endDate
        )
        async let insightsRes = BackendApi.getInsights(accessToken: token)
        async let summaryRes = BackendApi.getTransactionsSummary(
            accessToken: token,
            startDate: transactionFilters.startDate,
            endDate: transactionFilters.endDate,
            direction: transactionFilters.direction,
            categoryCode: transactionFilters.categoryCode,
            subcategoryCode: transactionFilters.subcategoryCode,
            channel: transactionFilters.channel,
            bankCode: transactionFilters.bankCode,
            search: searchQuery.isEmpty ? nil : searchQuery
        )
        let (kpisR, monthsR, txnsR, insightsR, summaryR) = await (kpisRes, monthsRes, txnsRes, insightsRes, summaryRes)
        await MainActor.run {
            if let k = try? kpisR.get() { kpis = k }
            if let m = try? monthsR.get() { availableMonths = m.data }
            if let t = try? txnsR.get() {
                transactions = t.transactions
                transactionsTotal = t.total ?? t.transactions.count
            }
            if let i = try? insightsR.get() { insights = i }
            if let s = try? summaryR.get() {
                debitCreditSummary = DebitCreditSummary(
                    debitTotal: s.debitTotal ?? 0,
                    creditTotal: s.creditTotal ?? 0,
                    debitCount: s.debitCount ?? 0,
                    creditCount: s.creditCount ?? 0
                )
            }
            if case .failure(let e) = kpisR { errorMessage = e.localizedDescription }
        }
    }

    func clearError() {
        errorMessage = nil
    }

    // MARK: - Derived (Categories tab)

    /// Total spend for current KPIs (total_debits or needs + wants).
    func totalSpend() -> Double {
        guard let k = kpis else { return 0 }
        return k.totalDebitsAmount ?? (k.needsAmount ?? 0) + (k.wantsAmount ?? 0)
    }

    /// Month-over-month change % for spending (from top_categories or best_month if available). Simplified.
    func spendingMomPct() -> Double? {
        guard let top = kpis?.topCategories?.first else { return nil }
        return top.deltaPct
    }

    /// Highest spending category name and share %.
    func highestCategory() -> (name: String, pct: Int)? {
        guard let top = kpis?.topCategories?.first else { return nil }
        let name = top.categoryName ?? top.categoryCode ?? "Unknown"
        let total = totalSpend()
        let pct = total > 0 ? Int((top.spendAmount ?? 0) / total * 100) : 0
        return (name, pct)
    }

    /// Category breakdown from insights for chart (mirrors APK category_breakdown).
    func categoryBreakdownForChart() -> [CategoryBreakdownItem] {
        insights?.categoryBreakdown ?? []
    }

    /// Delta % by category code from KPIs top_categories.
    func deltaByCategory() -> [String: Double] {
        guard let top = kpis?.topCategories else { return [:] }
        var map: [String: Double] = [:]
        for t in top {
            if let code = t.categoryCode, let delta = t.deltaPct {
                map[code] = delta
            }
        }
        return map
    }

    /// Financial health score 0–100 (simplified).
    func financialHealthScore() -> Int {
        guard let k = kpis else { return 50 }
        let income = k.incomeAmount ?? 0
        let needs = k.needsAmount ?? 0
        let wants = k.wantsAmount ?? 0
        let expenses = needs + wants
        let savingsRate = income > 0 ? (income - expenses) / income * 100 : 0
        let wantsRatio = expenses > 0 ? wants / expenses : 0
        let score = savingsRate * 0.5 + (1 - wantsRatio) * 25 + min(income / 50_000, 1.0) * 25
        return Int(score.clamped(to: 0...100))
    }

    /// Risk level label from score.
    func riskLevel(score: Int) -> String {
        switch score {
        case 80...: return "Low"
        case 60..<80: return "Moderate"
        case 40..<60: return "Elevated"
        default: return "High"
        }
    }
}

private extension Comparable {
    func clamped(to range: ClosedRange<Self>) -> Self {
        min(max(self, range.lowerBound), range.upperBound)
    }
}
