//
//  SpendSenseViewModel.swift
//  ios
//
//  SpendSense ViewModel - matches APK SpendSenseViewModel
//

import Foundation

@MainActor
@Observable
class SpendSenseViewModel {
    var transactions: [TransactionRecordResponse] = []
    var transactionsTotal = 0
    var categoryBreakdown: [CategoryBreakdownItem] = []
    var availableMonths: [String] = []
    var selectedMonth: String?
    var isLoading = false
    var error: String?

    private let api = BackendApi()

    func loadData(accessToken: String) async {
        isLoading = true
        error = nil
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM"
        selectedMonth = formatter.string(from: Date())
        let startDate = "\(selectedMonth!)-01"
        let endDate = endOfMonth(for: selectedMonth!)

        async let txnsTask: () = fetchTransactions(token: accessToken)
        async let insightsTask: () = fetchInsights(token: accessToken, start: startDate, end: endDate)
        async let monthsTask: () = fetchAvailableMonths(token: accessToken)

        _ = await (txnsTask, insightsTask, monthsTask)
        isLoading = false
    }

    private func fetchTransactions(token: String) async {
        do {
            let resp = try await api.getTransactions(accessToken: token, limit: 50)
            transactions = resp.transactions
            transactionsTotal = resp.total ?? resp.transactions.count
        } catch {
            self.error = error.localizedDescription
        }
    }

    private func fetchInsights(token: String, start: String, end: String) async {
        do {
            let resp = try await api.getInsights(accessToken: token, startDate: start, endDate: end)
            categoryBreakdown = resp.categoryBreakdown ?? []
        } catch {
            self.error = error.localizedDescription
        }
    }

    private func fetchAvailableMonths(token: String) async {
        do {
            let resp = try await api.getAvailableMonths(accessToken: token)
            availableMonths = resp.data ?? []
        } catch {
            // Non-fatal
        }
    }

    private func endOfMonth(for yearMonth: String) -> String {
        let parts = yearMonth.split(separator: "-")
        guard parts.count >= 2, let year = Int(parts[0]), let month = Int(parts[1]) else {
            return "\(yearMonth)-28"
        }
        var comp = DateComponents()
        comp.year = year
        comp.month = month + 1
        comp.day = 0
        if let lastDay = Calendar.current.date(from: comp) {
            let f = DateFormatter()
            f.dateFormat = "yyyy-MM-dd"
            return f.string(from: lastDay)
        }
        return "\(yearMonth)-28"
    }

    func refresh(accessToken: String) async {
        await loadData(accessToken: accessToken)
    }
}
