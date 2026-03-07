//
//  BudgetPilotViewModel.swift
//  ios_monytix
//
//  Mirrors APK BudgetPilotViewModel: budget state, recommendations, commit, apply adjustment, recalculate.
//

import Combine
import Foundation
import SwiftUI

struct GoalProgressForBudget: Identifiable {
    let id: String
    let goalName: String
    let monthlyRequired: Double
}

struct AutopilotSuggestion {
    let shiftFrom: String
    let shiftTo: String
    let pct: Double
    let message: String
}

struct BudgetDeviation {
    let needs: Double
    let wants: Double
    let savings: Double
}

final class BudgetPilotViewModel: ObservableObject {
    @Published private(set) var recommendations: [BudgetRecommendation] = []
    @Published private(set) var committedBudget: CommittedBudget?
    @Published private(set) var variance: BudgetVariance?
    @Published private(set) var deviation: BudgetDeviation?
    @Published private(set) var autopilotSuggestion: AutopilotSuggestion?
    @Published private(set) var budgetState: BudgetStateResponse?
    @Published private(set) var lastUpdatedAt: String?
    @Published var selectedMonth: String = ""
    @Published private(set) var goalsCount: Int = 0
    @Published private(set) var goalsProgressList: [GoalProgressForBudget] = []
    @Published private(set) var goalRequiredMonthlyTotal: Double = 0
    @Published private(set) var goalRequiredSavingsRate: Double = 0
    @Published private(set) var autopilotStatus: String = "Waiting for data"
    @Published private(set) var adaptivePlanReason: [String] = []
    @Published private(set) var userEmail: String?
    @Published private(set) var isLoadingState = false
    @Published private(set) var isCommitting = false
    @Published private(set) var committingPlanCode: String?
    @Published private(set) var isApplyingAdjustment = false
    @Published private(set) var isRecalculating = false
    @Published var errorMessage: String?

    static func currentMonthString() -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM"
        return formatter.string(from: Date())
    }

    init() {
        selectedMonth = BudgetPilotViewModel.currentMonthString()
        loadSession()
        loadData()
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

    func setMonth(_ month: String) {
        selectedMonth = month
        loadData()
    }

    func loadData() {
        Task { @MainActor in
            guard let token = await AuthManager.shared.getIdToken() else { return }
            let monthParam = selectedMonth.isEmpty ? nil : selectedMonth
            isLoadingState = true
            errorMessage = nil
            defer { isLoadingState = false }

            async let stateResult = BackendApi.getBudgetState(accessToken: token, month: monthParam)
            async let goalsProgressResult = BackendApi.getGoalsProgress(accessToken: token)
            let (stateRes, goalsRes) = await (stateResult, goalsProgressResult)

            let goalsProgress = (try? goalsRes.get())?.goals ?? []
            goalsCount = goalsProgress.count
            let goalRequiredTotal = goalsProgress.compactMap { $0.monthlyRequired }.reduce(0, +)
            goalRequiredMonthlyTotal = goalRequiredTotal
            goalsProgressList = goalsProgress.compactMap { g in
                guard let req = g.monthlyRequired, req > 0 else { return nil }
                return GoalProgressForBudget(id: g.goalId, goalName: g.goalName, monthlyRequired: req)
            }

            switch stateRes {
            case .success(let state):
                applyBudgetStateSuccess(state, goalRequiredTotal: goalRequiredTotal)
            case .failure(let err):
                errorMessage = BackendApiErrorMessage(err)
            }
        }
    }

    private func applyBudgetStateSuccess(_ state: BudgetStateResponse, goalRequiredTotal: Double) {
        budgetState = state
        lastUpdatedAt = state.lastUpdatedAt

        let committedPlan = state.committedPlan
        if let cp = committedPlan, let target = cp.target {
            let needs = (target["needs"] ?? 0) / 100.0
            let wants = (target["wants"] ?? 0) / 100.0
            let savings = (target["savings"] ?? 0) / 100.0
            committedBudget = CommittedBudget(
                userId: nil,
                month: state.month,
                planCode: cp.planId,
                allocNeedsPct: needs,
                allocWantsPct: wants,
                allocAssetsPct: savings,
                notes: nil,
                committedAt: nil,
                goalAllocations: nil
            )
        } else {
            committedBudget = nil
        }

        let income = state.incomeAmt ?? 0
        let actual = state.actual
        variance = computeVariance(state: state, committedPlan: committedPlan, income: income, actual: actual)

        if let d = state.deviation {
            deviation = BudgetDeviation(
                needs: d.needs ?? 0,
                wants: d.wants ?? 0,
                savings: d.savings ?? 0
            )
        } else {
            deviation = nil
        }

        autopilotSuggestion = computeAutopilotSuggestion(deviation: deviation)

        recommendations = (state.plans ?? []).map { p in
            BudgetRecommendation(
                planCode: p.planId,
                name: p.name,
                description: nil,
                needsBudgetPct: nil,
                wantsBudgetPct: nil,
                savingsBudgetPct: nil,
                score: p.score,
                recommendationReason: p.reason
            )
        }

        let incomeAmt = state.incomeAmt ?? 0
        let savingsAmt = actual?.savingsAmt ?? 0
        let totalSpend = (actual?.needsAmt ?? 0) + (actual?.wantsAmt ?? 0) + savingsAmt
        autopilotStatus = computeAutopilotStatus(
            incomeAmt: incomeAmt,
            totalSpend: totalSpend,
            hasSuggestion: autopilotSuggestion != nil,
            deviation: deviation
        )

        adaptivePlanReason = buildAdaptivePlanReason(
            deviation: deviation,
            goalImpact: state.goalImpact ?? [],
            topPlanReason: recommendations.first?.recommendationReason,
            actual: actual,
            income: incomeAmt
        )
    }

    private func computeVariance(
        state: BudgetStateResponse,
        committedPlan: BudgetStateCommittedPlan?,
        income: Double,
        actual: BudgetStateActual?
    ) -> BudgetVariance? {
        if let cp = committedPlan, let target = cp.target, income > 0 {
            let targetNeeds = (target["needs"] ?? 0) / 100.0 * income
            let targetWants = (target["wants"] ?? 0) / 100.0 * income
            let targetSavings = (target["savings"] ?? 0) / 100.0 * income
            let needsAmt = actual?.needsAmt ?? 0
            let wantsAmt = actual?.wantsAmt ?? 0
            let savingsAmt = actual?.savingsAmt ?? 0
            return BudgetVariance(
                userId: nil,
                month: state.month,
                incomeAmt: income,
                needsAmt: needsAmt,
                plannedNeedsAmt: targetNeeds,
                varianceNeedsAmt: needsAmt - targetNeeds,
                wantsAmt: wantsAmt,
                plannedWantsAmt: targetWants,
                varianceWantsAmt: wantsAmt - targetWants,
                assetsAmt: savingsAmt,
                plannedAssetsAmt: targetSavings,
                varianceAssetsAmt: savingsAmt - targetSavings,
                computedAt: nil
            )
        }
        guard let a = actual else { return nil }
        let total = (a.needsAmt ?? 0) + (a.wantsAmt ?? 0) + (a.savingsAmt ?? 0)
        guard income > 0 || total > 0 else { return nil }
        let effectiveIncome = income > 0 ? income : total
        let needsAmt = a.needsAmt ?? 0
        let wantsAmt = a.wantsAmt ?? 0
        let savingsAmt = a.savingsAmt ?? 0
        return BudgetVariance(
            userId: nil,
            month: state.month,
            incomeAmt: effectiveIncome,
            needsAmt: needsAmt,
            plannedNeedsAmt: 0,
            varianceNeedsAmt: 0,
            wantsAmt: wantsAmt,
            plannedWantsAmt: 0,
            varianceWantsAmt: 0,
            assetsAmt: savingsAmt,
            plannedAssetsAmt: 0,
            varianceAssetsAmt: 0,
            computedAt: nil
        )
    }

    private func computeAutopilotSuggestion(deviation: BudgetDeviation?) -> AutopilotSuggestion? {
        guard let d = deviation else { return nil }
        if d.savings < -5 && d.wants > 5 {
            let pct = min(5.0, min(-d.savings / 2, d.wants))
            return AutopilotSuggestion(
                shiftFrom: "wants",
                shiftTo: "savings",
                pct: pct,
                message: "Move \(Int(pct))% from wants to savings"
            )
        }
        if d.savings < -5 {
            let pct = min(5.0, -d.savings / 2)
            return AutopilotSuggestion(
                shiftFrom: "wants",
                shiftTo: "savings",
                pct: pct,
                message: "Move \(Int(pct))% from wants to savings"
            )
        }
        return nil
    }

    private func computeAutopilotStatus(
        incomeAmt: Double,
        totalSpend: Double,
        hasSuggestion: Bool,
        deviation: BudgetDeviation?
    ) -> String {
        if incomeAmt < 100 && totalSpend < 100 { return "Waiting for data" }
        if hasSuggestion { return "Adjusting" }
        if let d = deviation, (d.savings < -5 || d.needs > 5 || d.wants > 5) { return "Adjusting" }
        return "On Track"
    }

    private func buildAdaptivePlanReason(
        deviation: BudgetDeviation?,
        goalImpact: [BudgetGoalImpact],
        topPlanReason: String?,
        actual: BudgetStateActual?,
        income: Double
    ) -> [String] {
        var reasons: [String] = []
        if let reason = topPlanReason?.trimmingCharacters(in: .whitespacesAndNewlines), !reason.isEmpty {
            reasons.append(reason)
        }
        let totalPlanned = goalImpact.compactMap { $0.plannedAmount }.reduce(0, +)
        if totalPlanned > 0 && income > 0 && (totalPlanned / income) > 0.15 {
            reasons.append("High long-term goal load")
        }
        let needsPct = actual?.needsPct ?? 0
        if needsPct > 55 {
            reasons.append("Above-average EMI commitments")
        }
        if let d = deviation, (abs(d.needs) > 5 || abs(d.wants) > 5 || abs(d.savings) > 5) {
            if !reasons.contains(where: { $0.contains("volatility") }) {
                reasons.append("Moderate income volatility")
            }
        }
        return Array(Set(reasons))
    }

    func recalculate() {
        Task { @MainActor in
            guard let token = await AuthManager.shared.getIdToken() else { return }
            isRecalculating = true
            defer { isRecalculating = false }
            let monthParam = selectedMonth.isEmpty ? nil : selectedMonth
            let result = await BackendApi.recalculateBudget(accessToken: token, month: monthParam)
            switch result {
            case .success:
                loadData()
            case .failure(let err):
                errorMessage = BackendApiErrorMessage(err)
            }
        }
    }

    func commitBudget(planCode: String) {
        Task { @MainActor in
            guard let token = await AuthManager.shared.getIdToken() else {
                errorMessage = "Not signed in. Please sign in again."
                return
            }
            isCommitting = true
            committingPlanCode = planCode
            let result = await BackendApi.commitBudget(accessToken: token, planCode: planCode, month: selectedMonth.isEmpty ? nil : selectedMonth)
            isCommitting = false
            committingPlanCode = nil
            switch result {
            case .success(let budget):
                committedBudget = budget
                loadData()
            case .failure(let err):
                errorMessage = BackendApiErrorMessage(err)
            }
        }
    }

    func refresh() {
        loadData()
    }

    func applyAdjustment(shiftFrom: String, shiftTo: String, pct: Double) {
        Task { @MainActor in
            guard let token = await AuthManager.shared.getIdToken() else { return }
            isApplyingAdjustment = true
            defer { isApplyingAdjustment = false }
            let monthParam = selectedMonth.isEmpty ? nil : selectedMonth
            let result = await BackendApi.applyBudgetAdjustment(
                accessToken: token,
                shiftFrom: shiftFrom,
                shiftTo: shiftTo,
                pct: pct,
                month: monthParam
            )
            switch result {
            case .success(let resp):
                if resp.status == "applied" {
                    loadData()
                } else if resp.status == "rejected" {
                    errorMessage = resp.reason ?? "Adjustment rejected"
                } else {
                    errorMessage = "Unknown response"
                }
            case .failure(let err):
                errorMessage = BackendApiErrorMessage(err)
            }
        }
    }

    func clearError() {
        errorMessage = nil
    }
}

private func BackendApiErrorMessage(_ err: BackendApiError) -> String {
    switch err {
    case .invalidURL:
        return "Invalid URL"
    case .httpStatus(_, let body):
        return body ?? "Request failed"
    case .decoding(let e):
        return "Data error: \(e.localizedDescription)"
    case .network(let e):
        return e.localizedDescription
    }
}
