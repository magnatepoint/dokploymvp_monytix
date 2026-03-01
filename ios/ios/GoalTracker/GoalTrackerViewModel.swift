//
//  GoalTrackerViewModel.swift
//  ios
//
//  GoalTracker ViewModel - matches APK GoalTrackerViewModel
//

import Foundation

@MainActor
@Observable
class GoalTrackerViewModel {
    var goals: [GoalResponse] = []
    var goalsProgress: [GoalProgressItem] = []
    var isLoading = false
    var error: String?

    private let api = BackendApi()

    func loadData(accessToken: String) async {
        isLoading = true
        error = nil
        async let goalsTask: () = fetchGoals(token: accessToken)
        async let progressTask: () = fetchProgress(token: accessToken)
        _ = await (goalsTask, progressTask)
        isLoading = false
    }

    private func fetchGoals(token: String) async {
        do {
            goals = try await api.getUserGoals(accessToken: token)
        } catch {
            self.error = error.localizedDescription
        }
    }

    private func fetchProgress(token: String) async {
        do {
            let resp = try await api.getGoalsProgress(accessToken: token)
            goalsProgress = resp.goals
        } catch {
            self.error = error.localizedDescription
        }
    }

    func refresh(accessToken: String) async {
        await loadData(accessToken: accessToken)
    }

    func createGoal(
        accessToken: String,
        name: String,
        category: String = "savings",
        estimatedCost: Double,
        targetDate: String? = nil,
        currentSavings: Double = 0
    ) async {
        do {
            _ = try await api.createGoal(
                accessToken: accessToken,
                goalCategory: category,
                goalName: name,
                estimatedCost: estimatedCost,
                targetDate: targetDate,
                currentSavings: currentSavings
            )
            await loadData(accessToken: accessToken)
        } catch {
            self.error = error.localizedDescription
        }
    }

    func deleteGoal(accessToken: String, goalId: String) async {
        do {
            try await api.deleteGoal(accessToken: accessToken, goalId: goalId)
            await loadData(accessToken: accessToken)
        } catch {
            self.error = error.localizedDescription
        }
    }
}
