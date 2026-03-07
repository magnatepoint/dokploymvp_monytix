//
//  MoneyMomentsViewModel.swift
//  ios_monytix
//
//  Mirrors APK MoneyMomentsViewModel: moments, nudges, compute, evaluate & deliver, log interaction.
//

import Combine
import Foundation
import SwiftUI

struct ProgressMetrics {
    let streak: Int
    let nudgesCount: Int
    let habitsCount: Int
    let savedAmount: Double
}

final class MoneyMomentsViewModel: ObservableObject {
    @Published private(set) var moments: [MoneyMoment] = []
    @Published private(set) var nudges: [Nudge] = []
    @Published private(set) var userEmail: String?
    @Published private(set) var isMomentsLoading = false
    @Published private(set) var isNudgesLoading = false
    @Published var momentsError: String?
    @Published var nudgesError: String?
    @Published private(set) var isEvaluating = false
    @Published private(set) var isComputing = false
    @Published var actionError: String?

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
            isMomentsLoading = true
            isNudgesLoading = true
            momentsError = nil
            nudgesError = nil

            async let momentsResult = BackendApi.getMoments(accessToken: token, month: nil, allMonths: false)
            async let nudgesResult = BackendApi.getNudges(accessToken: token, limit: 20)
            let (momRes, nudgRes) = await (momentsResult, nudgesResult)

            switch momRes {
            case .success(let r): moments = r.moments ?? []
            case .failure(.decoding): moments = [] // lenient: show empty instead of data error
            case .failure(let e): moments = []; momentsError = mmErrorMessage(e)
            }
            switch nudgRes {
            case .success(let r): nudges = r.nudges ?? []
            case .failure(.decoding): nudges = [] // lenient: show empty instead of data error
            case .failure(let e): nudges = []; nudgesError = mmErrorMessage(e)
            }
            isMomentsLoading = false
            isNudgesLoading = false
        }
    }

    func computeProgressMetrics() -> ProgressMetrics {
        let monthsArray = Array(Array(Set(moments.compactMap { $0.month })).sorted().reversed())
        var streak = 0
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM"
        let currentMonth = formatter.string(from: Date())
        if let first = monthsArray.first, first == currentMonth {
            streak = 1
            var prev = first
            for i in 1..<monthsArray.count {
                let curr = monthsArray[i]
                if let (py, pm) = parseMonth(prev), let (cy, cm) = parseMonth(curr) {
                    let diff = (py - cy) * 12 + (pm - cm)
                    if diff == 1 { streak += 1; prev = curr } else { break }
                } else { break }
            }
        }
        let habitsCount = Set(moments.compactMap { $0.habitId }).count
        let savedAmount = moments
            .filter { ($0.habitId ?? "").lowercased().contains("savings") || ($0.habitId ?? "").lowercased().contains("assets") }
            .compactMap { $0.value }
            .filter { $0 > 0 }
            .reduce(0, +)
        return ProgressMetrics(
            streak: streak,
            nudgesCount: nudges.count,
            habitsCount: habitsCount,
            savedAmount: savedAmount
        )
    }

    private func parseMonth(_ s: String) -> (Int, Int)? {
        let parts = s.split(separator: "-").compactMap { Int($0) }
        guard parts.count >= 2 else { return nil }
        return (parts[0], parts[1])
    }

    func evaluateAndDeliverNudges() {
        Task { @MainActor in
            guard let token = await AuthManager.shared.getIdToken() else { return }
            isEvaluating = true
            actionError = nil
            defer { isEvaluating = false }

            let sigRes = await BackendApi.computeSignal(accessToken: token, asOfDate: nil)
            if case .failure(let e) = sigRes {
                actionError = mmErrorMessage(e)
                return
            }
            let evalRes = await BackendApi.evaluateNudges(accessToken: token, asOfDate: nil)
            if case .failure(let e) = evalRes {
                actionError = mmErrorMessage(e)
                return
            }

            isComputing = true
            defer { isComputing = false }
            let procRes = await BackendApi.processNudges(accessToken: token, limit: 10)
            if case .failure(let e) = procRes {
                actionError = mmErrorMessage(e)
                return
            }
            loadData()
        }
    }

    func computeMoments() {
        Task { @MainActor in
            guard let token = await AuthManager.shared.getIdToken() else { return }
            isComputing = true
            actionError = nil
            defer { isComputing = false }

            let cal = Calendar.current
            var successCount = 0
            for i in 0..<12 {
                guard let date = cal.date(byAdding: .month, value: -i, to: Date()) else { continue }
                let comps = cal.dateComponents([.year, .month], from: date)
                let monthStr = String(format: "%04d-%02d", comps.year ?? 0, comps.month ?? 0)
                let result = await BackendApi.computeMoments(accessToken: token, targetMonth: monthStr)
                if case .success = result { successCount += 1 }
            }
            if successCount > 0 {
                loadData()
            } else {
                actionError = "Failed to compute moments. Upload transaction data first."
            }
        }
    }

    func logNudgeInteraction(deliveryId: String, eventType: String) {
        Task {
            guard let token = await AuthManager.shared.getIdToken() else { return }
            _ = await BackendApi.logNudgeInteraction(accessToken: token, deliveryId: deliveryId, eventType: eventType, metadata: nil)
        }
    }

    func clearActionError() {
        actionError = nil
    }
}

private func mmErrorMessage(_ err: BackendApiError) -> String {
    switch err {
    case .invalidURL: return "Invalid URL"
    case .httpStatus(_, let body): return body ?? "Request failed"
    case .decoding(let e): return "Data error: \(e.localizedDescription)"
    case .network(let e): return e.localizedDescription
    }
}
