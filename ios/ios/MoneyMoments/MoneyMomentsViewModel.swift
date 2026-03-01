//
//  MoneyMomentsViewModel.swift
//  ios
//
//  MoneyMoments ViewModel - matches APK MoneyMomentsViewModel
//

import Foundation

@MainActor
@Observable
class MoneyMomentsViewModel {
    var moments: [MoneyMoment] = []
    var nudges: [Nudge] = []
    var userEmail: String?
    var isMomentsLoading = false
    var isNudgesLoading = false
    var error: String?

    private let api = BackendApi()

    func loadData(accessToken: String) async {
        isMomentsLoading = true
        isNudgesLoading = true
        error = nil
        async let momentsTask: () = fetchMoments(token: accessToken)
        async let nudgesTask: () = fetchNudges(token: accessToken)
        _ = await (momentsTask, nudgesTask)
        isMomentsLoading = false
        isNudgesLoading = false
    }

    private func fetchMoments(token: String) async {
        do {
            let resp = try await api.getMoments(accessToken: token)
            moments = resp.moments ?? []
        } catch {
            self.error = error.localizedDescription
        }
    }

    private func fetchNudges(token: String) async {
        do {
            let resp = try await api.getNudges(accessToken: token)
            nudges = resp.nudges ?? []
        } catch {
            self.error = error.localizedDescription
        }
    }

    func refresh(accessToken: String) async {
        await loadData(accessToken: accessToken)
    }
}
