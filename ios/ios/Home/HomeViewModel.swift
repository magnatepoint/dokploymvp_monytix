//
//  HomeViewModel.swift
//  ios
//
//  MolyConsole ViewModel - matches APK HomeViewModel
//

import Foundation

@MainActor
@Observable
class HomeViewModel {
    var kpis: KpiResponse?
    var accounts: [AccountItemResponse] = []
    var insights: InsightsResponse?
    var goalsProgress: [GoalProgressItem] = []
    var recentTransactions: [TransactionRecordResponse] = []
    var userEmail: String?
    var isLoading = false
    var backendError: String?

    private let api = BackendApi()

    func loadDashboard(accessToken: String) async {
        isLoading = true
        backendError = nil

        let calendar = Calendar.current
        let now = Date()
        guard let startOfMonth = calendar.date(from: calendar.dateComponents([.year, .month], from: now)),
              let endOfMonth = calendar.date(byAdding: DateComponents(month: 1, day: -1), to: startOfMonth) else {
            isLoading = false
            return
        }
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        let startDate = formatter.string(from: startOfMonth)
        let endDate = formatter.string(from: endOfMonth)

        async let kpisTask: () = fetchKpis(token: accessToken)
        async let accountsTask: () = fetchAccounts(token: accessToken)
        async let insightsTask: () = fetchInsights(token: accessToken, start: startDate, end: endDate)
        async let goalsTask: () = fetchGoals(token: accessToken)
        async let txnsTask: () = fetchTransactions(token: accessToken)

        _ = await (kpisTask, accountsTask, insightsTask, goalsTask, txnsTask)
        isLoading = false
    }

    private func fetchKpis(token: String) async {
        do {
            kpis = try await api.getKpis(accessToken: token)
        } catch {
            backendError = error.localizedDescription
        }
    }

    private func fetchAccounts(token: String) async {
        do {
            let resp = try await api.getAccounts(accessToken: token)
            accounts = resp.accounts
        } catch {
            backendError = error.localizedDescription
        }
    }

    private func fetchInsights(token: String, start: String, end: String) async {
        do {
            insights = try await api.getInsights(accessToken: token, startDate: start, endDate: end)
        } catch {
            backendError = error.localizedDescription
        }
    }

    private func fetchGoals(token: String) async {
        do {
            let resp = try await api.getGoalsProgress(accessToken: token)
            goalsProgress = resp.goals
        } catch {
            backendError = error.localizedDescription
        }
    }

    private func fetchTransactions(token: String) async {
        do {
            let resp = try await api.getTransactions(accessToken: token, limit: 5)
            recentTransactions = resp.transactions
        } catch {
            backendError = error.localizedDescription
        }
    }

    func transformGoals() -> [ConsoleGoal] {
        goalsProgress.map { g in
            ConsoleGoal(
                id: g.goalId,
                name: g.goalName,
                targetAmount: g.currentSavingsClose + g.remainingAmount,
                savedAmount: g.currentSavingsClose,
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
        accounts.reduce(0) { $0 + $1.balance }
    }

    func netWorthTrendPct() -> Double {
        guard let trends = insights?.spendingTrends, trends.count >= 2 else {
            return 0
        }
        let latest = trends.last!.net
        let previous = trends[trends.count - 2].net
        return previous != 0 ? ((latest - previous) / abs(previous) * 100) : 0
    }

    func sparklineData() -> [Float] {
        if let ts = insights?.timeSeries, !ts.isEmpty {
            return ts.map { Float($0.value) }
        }
        guard let trends = insights?.spendingTrends else { return [] }
        return trends.map { Float($0.net) }
    }

    func isCashFlowPositive() -> Bool {
        guard let k = kpis else { return false }
        let income = k.incomeAmount ?? 0
        let expenses = k.totalDebitsAmount ?? (k.needsAmount ?? 0) + (k.wantsAmount ?? 0)
        return income >= expenses
    }

    func goalsAtRiskCount() -> Int {
        transformGoals().filter { g in
            let progress = g.targetAmount > 0 ? g.savedAmount / g.targetAmount * 100 : 0
            return progress < 40
        }.count
    }

    func hasNoTransactionData() -> Bool {
        guard let k = kpis else { return true }
        let income = k.incomeAmount ?? 0
        let needs = k.needsAmount ?? 0
        let wants = k.wantsAmount ?? 0
        let assets = k.assetsAmount ?? 0
        let hasCategories = (k.topCategories?.isEmpty ?? true) == false
        return income == 0 && needs == 0 && wants == 0 && assets == 0 && !hasCategories
    }

    func generateAiInsights() -> [AiInsight] {
        guard let k = kpis else { return [] }
        let goals = transformGoals()
        let income = k.incomeAmount ?? 0
        let expenses = k.totalDebitsAmount ?? (k.needsAmount ?? 0) + (k.wantsAmount ?? 0)
        var list: [AiInsight] = []

        let surplus = income - expenses
        if surplus > 5000 && income > 0 {
            let safeSip = Int(surplus * 0.3)
            list.append(AiInsight(id: "opt_sip", title: "Optimization", message: "You can increase SIP by \(formatCurrency(Double(safeSip))) safely.", type: "optimization", confidence: 0.85))
        }
        if let top = goals.first, top.targetAmount > 0, top.savedAmount / top.targetAmount > 0.8 {
            list.append(AiInsight(id: "goal", title: "Good News", message: "You're on track to reach your \(top.name.lowercased()) goal soon.", type: "goal_progress", confidence: 0.9))
        }
        if list.isEmpty {
            list.append(AiInsight(id: "0", title: "On Track", message: "Your finances look on track this month. Keep monitoring to stay ahead.", type: "on_track", confidence: 0.7))
        }
        return list
    }

    func spendingVsLastMonthPct() -> Double? {
        guard let trends = insights?.spendingTrends, trends.count >= 2 else { return nil }
        let latest = trends.last!.expenses
        let prev = trends[trends.count - 2].expenses
        return prev != 0 ? ((latest - prev) / prev * 100) : nil
    }

    func projectedMonthEndSpending() -> Double? {
        guard let k = kpis else { return nil }
        let spent = k.totalDebitsAmount ?? (k.needsAmount ?? 0) + (k.wantsAmount ?? 0)
        guard spent > 0 else { return nil }
        let calendar = Calendar.current
        let now = Date()
        let dayOfMonth = Double(calendar.component(.day, from: now))
        let daysInMonth = Double(calendar.range(of: .day, in: .month, for: now)?.count ?? 30)
        return (spent / max(dayOfMonth, 1)) * daysInMonth
    }

    func spendingSpikeCount() -> Int {
        guard let insights = insights, let kpis = kpis else { return 0 }
        let topCats = kpis.topCategories ?? []
        let breakdown = insights.categoryBreakdown ?? []
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
        if isCashFlowPositive() { items.append(TodayIntelligenceItem(label: "Cash flow positive", type: "positive")) }
        let atRisk = goalsAtRiskCount()
        if atRisk > 0 { items.append(TodayIntelligenceItem(label: "\(atRisk) goal\(atRisk > 1 ? "s" : "") at risk", type: "risk")) }
        let spikes = spendingSpikeCount()
        if spikes > 0 { items.append(TodayIntelligenceItem(label: "\(spikes) spending spike\(spikes > 1 ? "s" : "")", type: "spike")) }
        let largeDebits = recentTransactions.filter { $0.direction.lowercased() == "debit" && abs($0.amount) >= 10000 }.count
        if largeDebits > 0 { items.append(TodayIntelligenceItem(label: "\(largeDebits) large debit\(largeDebits > 1 ? "s" : "")", type: "large")) }
        return Array(items.prefix(4))
    }

    func refresh(accessToken: String) async {
        await loadDashboard(accessToken: accessToken)
    }
}

struct ConsoleGoal {
    let id: String
    let name: String
    let targetAmount: Double
    let savedAmount: Double
    let targetDate: String?
    let isActive: Bool
}

struct ConsoleCategorySpending {
    let category: String
    let amount: Double
    let percentage: Double
    let transactionCount: Int
}

struct AiInsight {
    let id: String
    let title: String
    let message: String
    let type: String
    let confidence: Float?
}

struct TodayIntelligenceItem {
    let label: String
    let type: String
}
