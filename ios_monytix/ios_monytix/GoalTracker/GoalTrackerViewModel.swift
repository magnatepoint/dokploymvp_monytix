//
//  GoalTrackerViewModel.swift
//  ios_monytix
//
//  Mirrors APK GoalTrackerViewModel: goals list + progress + CRUD and helpers.
//

import Combine
import Foundation
import SwiftUI

enum CreateGoalResult: Equatable {
    case success
    case failure(String)
}

final class GoalTrackerViewModel: ObservableObject {
    @Published private(set) var goals: [GoalResponse] = []
    @Published private(set) var progress: [GoalProgressItem] = []
    @Published private(set) var userEmail: String?
    @Published private(set) var isLoading = false
    @Published var errorMessage: String?
    @Published var createGoalResult: CreateGoalResult?
    @Published var selectedFilter: String?
    @Published private(set) var lastSyncTime: Date?

    init() {
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

    func loadData() {
        Task { @MainActor in
            guard let token = await AuthManager.shared.getIdToken() else { return }
            isLoading = true
            errorMessage = nil
            defer { isLoading = false }

            async let goalsResult = BackendApi.getUserGoals(accessToken: token)
            async let progressResult = BackendApi.getGoalsProgress(accessToken: token)
            let (gRes, pRes) = await (goalsResult, progressResult)

            goals = (try? gRes.get()) ?? []
            progress = (try? pRes.get())?.goals ?? []
            if case .failure(let err) = gRes {
                errorMessage = err.localizedDescription
            } else if case .failure(let err) = pRes {
                errorMessage = err.localizedDescription
            }
            lastSyncTime = Date()
        }
    }

    func refresh() {
        loadData()
    }

    func setFilter(_ filter: String?) {
        selectedFilter = filter
    }

    func clearError() {
        errorMessage = nil
    }

    func clearCreateGoalResult() {
        createGoalResult = nil
    }

    func createGoal(
        goalCategory: String,
        goalName: String,
        estimatedCost: Double,
        targetDate: String?,
        currentSavings: Double,
        goalType: String? = nil,
        importance: Int? = nil
    ) {
        Task { @MainActor in
            guard let token = await AuthManager.shared.getIdToken() else {
                createGoalResult = .failure("Please sign in to add goals")
                return
            }
            isLoading = true
            errorMessage = nil
            defer { isLoading = false }

            let result = await BackendApi.createGoal(
                accessToken: token,
                goalCategory: goalCategory,
                goalName: goalName,
                estimatedCost: estimatedCost,
                targetDate: targetDate,
                currentSavings: currentSavings,
                goalType: goalType,
                importance: importance
            )
            switch result {
            case .success:
                createGoalResult = .success
                loadData()
            case .failure(let err):
                let msg = err.localizedDescription
                errorMessage = msg
                createGoalResult = .failure(msg)
            }
        }
    }

    func updateGoal(
        goalId: String,
        estimatedCost: Double? = nil,
        targetDate: String? = nil,
        currentSavings: Double? = nil,
        goalType: String? = nil,
        importance: Int? = nil
    ) {
        Task { @MainActor in
            guard let token = await AuthManager.shared.getIdToken() else { return }
            isLoading = true
            errorMessage = nil
            defer { isLoading = false }

            let result = await BackendApi.updateGoal(
                accessToken: token,
                goalId: goalId,
                estimatedCost: estimatedCost,
                targetDate: targetDate,
                currentSavings: currentSavings,
                goalType: goalType,
                importance: importance
            )
            switch result {
            case .success:
                loadData()
            case .failure(let err):
                errorMessage = err.localizedDescription
            }
        }
    }

    func deleteGoal(goalId: String) {
        Task { @MainActor in
            guard let token = await AuthManager.shared.getIdToken() else { return }
            isLoading = true
            errorMessage = nil
            defer { isLoading = false }

            let result = await BackendApi.deleteGoal(accessToken: token, goalId: goalId)
            switch result {
            case .success:
                loadData()
            case .failure(let err):
                errorMessage = err.localizedDescription
            }
        }
    }

    // MARK: - Derived helpers (mirror APK)

    struct GoalHealthSummary {
        let score: Int
        let onTrack: Int
        let atRisk: Int
    }

    func goalHealthSummary() -> GoalHealthSummary {
        let active = goals.filter { $0.status.lowercased() == "active" }
        if active.isEmpty { return GoalHealthSummary(score: 0, onTrack: 0, atRisk: 0) }
        var onTrack = 0
        var atRisk = 0
        for g in active {
            let prog = progress.first { $0.goalId == g.goalId }
            let pct = prog?.progressPct ?? 0
            let daysLeft = prog?.daysToTarget ?? 999
            let monthlyReq = prog?.monthlyRequired ?? 0
            switch (pct, daysLeft, monthlyReq) {
            case (let p, _, _) where p >= 100:
                onTrack += 1
            case (let p, let d, _) where d <= 30 && p < 50:
                atRisk += 1
            case (_, _, let m) where m > 15_000:
                atRisk += 1
            default:
                onTrack += 1
            }
        }
        let n = max(active.count, 1)
        let score = Int((Double(onTrack) / Double(n)) * 60 + (1 - Double(atRisk) / Double(n)) * 40)
            .clamped(to: 0...100)
        return GoalHealthSummary(score: score, onTrack: onTrack, atRisk: atRisk)
    }

    func upcomingDeadlines() -> [(GoalResponse, GoalProgressItem?)] {
        let active = goals.filter { $0.status.lowercased() == "active" }
        return active
            .map { g in (g, progress.first { $0.goalId == g.goalId }) }
            .filter { (_, prog) in
                let d = prog?.daysToTarget ?? 999
                return (1...365).contains(d)
            }
            .sorted { ($0.1?.daysToTarget ?? 999) < ($1.1?.daysToTarget ?? 999) }
            .prefix(5)
            .map { $0 }
    }

    func goalEmoji(_ goal: GoalResponse) -> String {
        let name = goal.goalName
        if name.localizedCaseInsensitiveContains("Retirement") { return "🎯" }
        if name.localizedCaseInsensitiveContains("Home") || name.localizedCaseInsensitiveContains("Down Payment") { return "🏠" }
        if name.localizedCaseInsensitiveContains("Credit") || name.localizedCaseInsensitiveContains("Debt") || name.localizedCaseInsensitiveContains("Paydown") { return "💳" }
        if name.localizedCaseInsensitiveContains("Emergency") { return "🛡️" }
        if name.localizedCaseInsensitiveContains("Education") { return "📚" }
        if name.localizedCaseInsensitiveContains("Travel") || name.localizedCaseInsensitiveContains("Vacation") { return "✈️" }
        return "🎯"
    }

    func goalEmotionalLabel(_ goal: GoalResponse) -> String {
        let name = goal.goalName
        if name.localizedCaseInsensitiveContains("Retirement") { return "Long-term freedom" }
        if name.localizedCaseInsensitiveContains("Home") || name.localizedCaseInsensitiveContains("Down Payment") { return "Dream Home Fund" }
        if name.localizedCaseInsensitiveContains("Credit") || name.localizedCaseInsensitiveContains("Debt") || name.localizedCaseInsensitiveContains("Paydown") { return "Debt Freedom" }
        if name.localizedCaseInsensitiveContains("Emergency") { return "Safety Net" }
        if name.localizedCaseInsensitiveContains("Education") { return "Future Ready" }
        if name.localizedCaseInsensitiveContains("Travel") || name.localizedCaseInsensitiveContains("Vacation") { return "Adventure Fund" }
        return "Your goal"
    }
}

private extension Comparable {
    func clamped(to range: ClosedRange<Self>) -> Self {
        min(max(self, range.lowerBound), range.upperBound)
    }
}

