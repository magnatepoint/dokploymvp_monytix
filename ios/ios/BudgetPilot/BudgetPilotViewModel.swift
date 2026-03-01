//
//  BudgetPilotViewModel.swift
//  ios
//
//  BudgetPilot ViewModel - matches APK BudgetPilotViewModel
//

import Foundation

@MainActor
@Observable
class BudgetPilotViewModel {
    var budgetState: BudgetStateResponse?
    var variance: [BudgetVariance] = []
    var recommendations: [BudgetRecommendation] = []
    var selectedMonth: String
    var isLoading = false
    var error: String?

    private let api = BackendApi()

    init() {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM"
        selectedMonth = f.string(from: Date())
    }

    func loadData(accessToken: String) async {
        isLoading = true
        error = nil
        async let stateTask: () = fetchState(token: accessToken)
        async let varianceTask: () = fetchVariance(token: accessToken)
        async let recsTask: () = fetchRecommendations(token: accessToken)
        _ = await (stateTask, varianceTask, recsTask)
        isLoading = false
    }

    private func fetchState(token: String) async {
        do {
            budgetState = try await api.getBudgetState(accessToken: token, month: selectedMonth)
        } catch {
            self.error = error.localizedDescription
        }
    }

    private func fetchVariance(token: String) async {
        do {
            let resp = try await api.getBudgetVariance(accessToken: token, month: selectedMonth)
            variance = resp.variance ?? []
        } catch {
            self.error = error.localizedDescription
        }
    }

    private func fetchRecommendations(token: String) async {
        do {
            let resp = try await api.getBudgetRecommendations(accessToken: token, month: selectedMonth)
            recommendations = resp.recommendations ?? []
        } catch {
            // Non-fatal
        }
    }

    func refresh(accessToken: String) async {
        await loadData(accessToken: accessToken)
    }
}
