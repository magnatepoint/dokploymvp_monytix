//
//  MolyConsoleViewModel.swift
//  ios_monytix
//
//  Home dashboard state and data. Mirrors APK HomeViewModel: KPIs, accounts, insights, goals, transactions; backend REST + optional WebSocket refresh.
//

import Combine
import Foundation
import SwiftUI

// MARK: - Console UI Models

struct ConsoleGoal: Identifiable {
    let id: String
    let name: String
    let targetAmount: Double
    let savedAmount: Double
    let targetDate: String?
    let isActive: Bool
}

struct ConsoleCategorySpending: Identifiable {
    let id = UUID()
    let category: String
    let amount: Double
    let percentage: Double
    let transactionCount: Int
}

struct AiInsight: Identifiable {
    let id: String
    let title: String
    let message: String
    let type: String
    let confidence: Float?
}

struct TodayIntelligenceItem: Identifiable {
    let id = UUID()
    let label: String
    let type: String
}

/// First-fold state for command center home.
struct HealthState {
    let score: Int
    let trend: String
    let subtext: String
}

struct RiskState {
    let label: String
    let reason: String
}

struct NextAction {
    let type: String
    let label: String
    let payload: String?
}

// MARK: - ViewModel

final class MolyConsoleViewModel: ObservableObject {
    @Published private(set) var kpis: KpiResponse?
    @Published private(set) var accounts: [AccountItemResponse] = []
    @Published private(set) var insights: InsightsResponse?
    @Published private(set) var goalsProgress: [GoalProgressItem] = []
    @Published private(set) var recentTransactions: [TransactionRecordResponse] = []
    @Published private(set) var topInsightsFromApi: [AiInsight]? = nil
    @Published private(set) var userEmail: String?
    @Published private(set) var isLoading = false
    @Published var errorMessage: String?
    @Published private(set) var backendStatus: String?
    @Published private(set) var backendError: String?

    private var loadTask: Task<Void, Never>?

    func loadDashboard() {
        loadTask?.cancel()
        loadTask = Task { @MainActor in
            isLoading = true
            errorMessage = nil
            defer { isLoading = false }

            guard let token = await AuthManager.shared.getIdToken() else {
                errorMessage = "Not signed in."
                return
            }

            let calendar = Calendar.current
            let now = Date()
            guard let startOfMonth = calendar.date(from: calendar.dateComponents([.year, .month], from: now)),
                  let nextMonth = calendar.date(byAdding: .month, value: 1, to: startOfMonth),
                  let endOfMonth = calendar.date(byAdding: .day, value: -1, to: nextMonth) else { return }
            let formatter = DateFormatter()
            formatter.dateFormat = "yyyy-MM-dd"
            let startDate = formatter.string(from: startOfMonth)
            let endDate = formatter.string(from: endOfMonth)

            async let kpisResult = BackendApi.getKpis(accessToken: token)
            async let accountsResult = BackendApi.getAccounts(accessToken: token)
            async let insightsResult = BackendApi.getInsights(accessToken: token, startDate: startDate, endDate: endDate)
            async let goalsResult = BackendApi.getGoalsProgress(accessToken: token)
            async let sessionResult = BackendApi.getSession(accessToken: token)
            async let transactionsResult = BackendApi.getTransactions(accessToken: token, limit: 5, offset: 0)
            async let topInsightsResult = BackendApi.getTopInsights(accessToken: token, limit: 5)

            let (kpisRes, accountsRes, insightsRes, goalsRes, sessionRes, txnsRes, topRes) = await (
                kpisResult, accountsResult, insightsResult, goalsResult, sessionResult, transactionsResult, topInsightsResult
            )

            kpis = (try? kpisRes.get())
            accounts = (try? accountsRes.get())?.accounts ?? []
            insights = try? insightsRes.get()
            goalsProgress = (try? goalsRes.get())?.goals ?? []
            userEmail = (try? sessionRes.get())?.email
            recentTransactions = (try? txnsRes.get())?.transactions ?? []
            if let top = try? topRes.get() {
                topInsightsFromApi = top.insights.map { i in
                    AiInsight(id: i.id, title: i.title, message: i.message, type: i.type, confidence: i.confidence.map { Float($0) })
                }
            } else {
                topInsightsFromApi = nil
            }

            if case .failure(let err) = kpisRes {
                errorMessage = err.localizedDescription
            }
        }
    }

    func refresh() {
        loadDashboard()
        // Defer health check to avoid extra concurrent connections and nw_connection noise
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { [weak self] in
            self?.checkBackend()
        }
    }

    /// Call from SwiftUI refreshable { await viewModel.refreshAsync() }
    func refreshAsync() async {
        await loadDashboardAsync()
        try? await Task.sleep(nanoseconds: 300_000_000) // 0.3s
        checkBackend()
    }

    private func loadDashboardAsync() async {
        guard let token = await AuthManager.shared.getIdToken() else {
            await MainActor.run { errorMessage = "Not signed in." }
            return
        }
        await MainActor.run { isLoading = true; errorMessage = nil }
        defer { Task { @MainActor in isLoading = false } }

        let calendar = Calendar.current
        let now = Date()
        guard let startOfMonth = calendar.date(from: calendar.dateComponents([.year, .month], from: now)),
              let nextMonth = calendar.date(byAdding: .month, value: 1, to: startOfMonth),
              let endOfMonth = calendar.date(byAdding: .day, value: -1, to: nextMonth) else { return }
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        let startDate = formatter.string(from: startOfMonth)
        let endDate = formatter.string(from: endOfMonth)

        async let kpisResult = BackendApi.getKpis(accessToken: token)
        async let accountsResult = BackendApi.getAccounts(accessToken: token)
        async let insightsResult = BackendApi.getInsights(accessToken: token, startDate: startDate, endDate: endDate)
        async let goalsResult = BackendApi.getGoalsProgress(accessToken: token)
        async let sessionResult = BackendApi.getSession(accessToken: token)
        async let transactionsResult = BackendApi.getTransactions(accessToken: token, limit: 5, offset: 0)

        let (kpisRes, accountsRes, insightsRes, goalsRes, sessionRes, txnsRes) = await (
            kpisResult, accountsResult, insightsResult, goalsResult, sessionResult, transactionsResult
        )

        await MainActor.run {
            kpis = (try? kpisRes.get())
            accounts = (try? accountsRes.get())?.accounts ?? []
            insights = try? insightsRes.get()
            goalsProgress = (try? goalsRes.get())?.goals ?? []
            userEmail = (try? sessionRes.get())?.email
            recentTransactions = (try? txnsRes.get())?.transactions ?? []
            if case .failure(let err) = kpisRes {
                errorMessage = err.localizedDescription
            }
        }
    }

    func checkBackend() {
        Task { @MainActor in
            let result = await BackendApi.healthCheck()
            switch result {
            case .success:
                backendStatus = "Connected"
                backendError = nil
            case .failure(let err):
                backendStatus = nil
                backendError = err.localizedDescription
            }
        }
    }

    func onRealtimeMessage() {
        Task { @MainActor in
            refresh()
        }
    }

    /// Call when MolyConsole appears: connect WebSocket and set callback to refresh.
    func connectRealtime() {
        Task {
            guard let token = await AuthManager.shared.getIdToken() else { return }
            WebSocketService.shared.setOnMessage { [weak self] in
                self?.onRealtimeMessage()
            }
            WebSocketService.shared.connect(idToken: token)
        }
    }

    /// Call when MolyConsole disappears: disconnect WebSocket.
    func disconnectRealtime() {
        WebSocketService.shared.disconnect()
    }

    // MARK: - Transforms (mirror APK)

    func transformGoals() -> [ConsoleGoal] {
        goalsProgress.map { g in
            let target = (g.currentSavingsClose ?? 0) + (g.remainingAmount ?? 0)
            return ConsoleGoal(
                id: g.goalId,
                name: g.goalName,
                targetAmount: target > 0 ? target : (g.currentSavingsClose ?? 0),
                savedAmount: g.currentSavingsClose ?? 0,
                targetDate: g.projectedCompletionDate,
                isActive: true
            )
        }
    }

    func transformCategorySpending() -> [ConsoleCategorySpending] {
        guard let breakdown = insights?.categoryBreakdown else { return [] }
        return breakdown.map { c in
            ConsoleCategorySpending(
                category: c.categoryName ?? c.categoryCode ?? "",
                amount: c.amount,
                percentage: c.percentage,
                transactionCount: c.transactionCount ?? 0
            )
        }
    }

    func totalNetWorth() -> Double {
        accounts.map(\.balance).reduce(0, +)
    }

    func netWorthTrendPct() -> Double {
        let trends = insights?.spendingTrends ?? []
        if trends.count < 2 { return kpis?.topCategories?.first?.deltaPct ?? 0 }
        let latest = trends.last!.net ?? 0
        let previous = trends.dropLast().last!.net ?? 0
        if previous == 0 { return 0 }
        return ((latest - previous) / abs(previous)) * 100
    }

    func spendingVsLastMonthPct() -> Double? {
        let trends = insights?.spendingTrends ?? []
        guard trends.count >= 2 else { return nil }
        let latest = trends.last!.expenses ?? 0
        let prev = trends.dropLast().last!.expenses ?? 0
        if prev == 0 { return nil }
        return ((latest - prev) / prev) * 100
    }

    func projectedMonthEndSpending() -> Double? {
        guard let k = kpis else { return nil }
        let spent = k.totalDebitsAmount ?? (k.needsAmount ?? 0) + (k.wantsAmount ?? 0)
        if spent <= 0 { return nil }
        let day = Calendar.current.component(.day, from: Date())
        let daysInMonth = Calendar.current.range(of: .day, in: .month, for: Date())?.count ?? 30
        return (spent / Double(max(1, day))) * Double(daysInMonth)
    }

    func sparklineData() -> [Float] {
        if let ts = insights?.timeSeries, !ts.isEmpty {
            return ts.map { Float($0.value) }
        }
        let trends = insights?.spendingTrends ?? []
        return trends.map { Float($0.net ?? 0) }
    }

    func isCashFlowPositive() -> Bool {
        guard let k = kpis else { return false }
        let income = k.incomeAmount ?? 0
        let expenses = k.totalDebitsAmount ?? (k.needsAmount ?? 0) + (k.wantsAmount ?? 0)
        return income >= expenses
    }

    func goalsAtRiskCount() -> Int {
        transformGoals().filter { g in
            let progress = g.targetAmount > 0 ? (g.savedAmount / g.targetAmount) * 100 : 0
            return progress < 40
        }.count
    }

    func spendingSpikeCount() -> Int {
        let k = kpis
        let breakdown = insights?.categoryBreakdown ?? []
        let topCats = k?.topCategories ?? []
        var spikes = 0
        for kpi in topCats {
            guard let cat = breakdown.first(where: { $0.categoryCode == kpi.categoryCode }) else { continue }
            let avg = cat.avgTransaction ?? 0
            let spend = kpi.spendAmount ?? 0
            if avg > 0 && spend > avg * 1.5 { spikes += 1 }
        }
        return spikes
    }

    func todayIntelligence() -> [TodayIntelligenceItem] {
        var items: [TodayIntelligenceItem] = []
        if isCashFlowPositive() {
            items.append(TodayIntelligenceItem(label: "Cash flow positive", type: "positive"))
        }
        let atRisk = goalsAtRiskCount()
        if atRisk > 0 {
            items.append(TodayIntelligenceItem(label: "\(atRisk) goal\(atRisk > 1 ? "s" : "") at risk", type: "risk"))
        }
        let spikes = spendingSpikeCount()
        if spikes > 0 {
            items.append(TodayIntelligenceItem(label: "\(spikes) spending spike\(spikes > 1 ? "s" : "")", type: "spike"))
        }
        let recurringCount = insights?.recurringTransactions?.count ?? 0
        if recurringCount > 0 {
            items.append(TodayIntelligenceItem(label: "\(recurringCount) recurring payments", type: "recurring"))
        }
        let largeDebits = recentTransactions.filter { ($0.direction.lowercased() == "debit") && abs($0.amount) >= 10000 }.count
        if largeDebits > 0 {
            items.append(TodayIntelligenceItem(label: "\(largeDebits) large debit\(largeDebits > 1 ? "s" : "")", type: "large"))
        }
        return Array(items.prefix(4))
    }

    /// Top 3 insights for command center: from API if available, else from generateAiInsights().
    func topInsightsForCommandCenter() -> [AiInsight] {
        if let api = topInsightsFromApi, !api.isEmpty {
            return Array(api.prefix(3))
        }
        return Array(generateAiInsights().prefix(3))
    }

    func hasNoTransactionData() -> Bool {
        guard let k = kpis else { return true }
        let hasIncome = (k.incomeAmount ?? 0) > 0
        let hasNeeds = (k.needsAmount ?? 0) > 0
        let hasWants = (k.wantsAmount ?? 0) > 0
        let hasAssets = (k.assetsAmount ?? 0) > 0
        let hasCategories = !(k.topCategories ?? []).isEmpty
        return !hasIncome && !hasNeeds && !hasWants && !hasAssets && !hasCategories
    }

    func generateAiInsights() -> [AiInsight] {
        var list: [AiInsight] = []
        let k = kpis
        let goals = transformGoals()
        let income = k?.incomeAmount ?? 0
        let expenses = k?.totalDebitsAmount ?? (k?.needsAmount ?? 0) + (k?.wantsAmount ?? 0)
        let breakdown = insights?.categoryBreakdown ?? []

        let emiCat = breakdown.first { ($0.categoryName ?? "").lowercased().contains("emi") || ($0.categoryName ?? "").lowercased().contains("loan") }
        if let emi = emiCat, income > 0 {
            let emiPct = (emi.amount / income) * 100
            if emiPct >= 30 {
                list.append(AiInsight(id: "risk_emi", title: "Risk", message: "EMIs consume \(Int(emiPct))% of your income. Consider refinancing or prepayment.", type: "risk", confidence: 0.9))
            }
        }

        let surplus = income - expenses
        if surplus > 5000 && income > 0 {
            let safeSip = Int(surplus * 0.3)
            list.append(AiInsight(id: "opt_sip", title: "Optimization", message: "You can increase SIP by ₹\(safeSip) safely.", type: "optimization", confidence: 0.85))
        }

        if let top = k?.topCategories?.first, (top.spendAmount ?? 0) > 50000 {
            let name = top.categoryName ?? top.categoryCode ?? "spending"
            list.append(AiInsight(id: "1", title: "Risk", message: "Your spending on \(name.lowercased()) is high. Consider setting a limit.", type: "risk", confidence: 0.8))
        }

        if let g = goals.first {
            let progress = g.targetAmount > 0 ? g.savedAmount / g.targetAmount : 0
            if progress > 0.8 {
                list.append(AiInsight(id: "2", title: "Good News", message: "You're on track to reach your \(g.name.lowercased()) goal soon.", type: "goal_progress", confidence: 0.9))
            }
        }

        let foodCat = breakdown.first { ($0.categoryName ?? "").lowercased().contains("food") || ($0.categoryName ?? "").lowercased().contains("dining") }
        if let food = foodCat, food.percentage > 25 {
            let save = Int(food.amount * 0.15)
            list.append(AiInsight(id: "3", title: "Budget Tip", message: "You're spending \(Int(food.percentage))% on \(food.categoryName ?? "food"). Meal planning could save ₹\(save).", type: "budget_tip", confidence: 0.8))
        }

        if (k?.assetsAmount ?? 0) > 1_000_000 {
            list.append(AiInsight(id: "4", title: "Optimization", message: "Your portfolio shows strong growth. Consider increasing SIP contributions.", type: "optimization", confidence: 0.85))
        }

        if list.isEmpty {
            list.append(AiInsight(id: "0", title: "On Track", message: "Your finances look on track this month. Keep monitoring to stay ahead.", type: "on_track", confidence: 0.7))
        }
        return list
    }

    func healthState() -> HealthState {
        if hasNoTransactionData() {
            return HealthState(score: 0, trend: "neutral", subtext: "Add data to see your score")
        }
        let income = kpis?.incomeAmount ?? 0
        let expenses = kpis?.totalDebitsAmount ?? 0
        let goals = transformGoals()
        let surplus = income - expenses
        var score = 50
        if income > 0 {
            let savingsRate = Int((surplus / income) * 100)
            score = min(100, max(0, 50 + savingsRate / 2))
        }
        let atRisk = goalsAtRiskCount()
        if atRisk > 0 { score = max(0, score - atRisk * 5) }
        let trend: String
        if surplus > 0 && atRisk == 0 { trend = "up" }
        else if surplus < 0 || atRisk > 0 { trend = "down" }
        else { trend = "neutral" }
        return HealthState(score: score, trend: trend, subtext: "Based on cash, goals, and spending")
    }

    func riskState() -> RiskState {
        if hasNoTransactionData() {
            return RiskState(label: "Add your data", reason: "Upload a statement to see risk and opportunities.")
        }
        if !isCashFlowPositive() {
            return RiskState(label: "Spending ahead of income", reason: "This month's outgo exceeds income. Review expenses.")
        }
        let atRisk = goalsAtRiskCount()
        if atRisk > 0 {
            return RiskState(label: "\(atRisk) goal\(atRisk > 1 ? "s" : "") at risk", reason: "Consider topping up to stay on track.")
        }
        return RiskState(label: "You're on track", reason: "Cash flow is positive and goals are in good shape.")
    }

    func nextAction() -> NextAction {
        if hasNoTransactionData() {
            return NextAction(type: "upload", label: "Upload this week's statement", payload: nil)
        }
        let insights = generateAiInsights()
        if insights.count >= 2 {
            return NextAction(type: "insights", label: "Review \(insights.count) insights", payload: nil)
        }
        let goals = transformGoals()
        if goals.contains(where: { g in g.targetAmount > 0 && (g.savedAmount / g.targetAmount) < 0.5 }) {
            return NextAction(type: "goal", label: "Top up a goal", payload: nil)
        }
        return NextAction(type: "forecast", label: "See your financial future", payload: nil)
    }
}

// MARK: - BackendApiError + localizedDescription

extension BackendApiError {
    var localizedDescription: String {
        switch self {
        case .invalidURL: return "Invalid URL"
        case .httpStatus(let code, let body):
            let text = body ?? "HTTP \(code)"
            if text.lowercased().contains("<!doctype") || text.lowercased().contains("<html") || text.contains("</") {
                return "Service unavailable (\(code)). Please try again later."
            }
            if text.count > 300 { return String(text.prefix(300)) + "…" }
            return text
        case .decoding(let e): return e.localizedDescription
        case .network(let e): return e.localizedDescription
        }
    }
}
