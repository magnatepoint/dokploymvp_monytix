//
//  ProfileViewModel.swift
//  ios
//
//  Profile ViewModel - matches APK ProfileViewModel
//

import Foundation

@MainActor
@Observable
class ProfileViewModel {
    var userEmail: String?
    var userId: String?
    var isExporting = false
    var exportSuccess = false
    var isDeletingData = false
    var deleteDataSuccess = false
    var error: String?

    private let api = BackendApi()

    func loadUser(from session: (email: String?, id: String?)?) {
        userEmail = session?.email
        userId = session?.id ?? nil
    }

    func deleteAllData(accessToken: String) async {
        isDeletingData = true
        error = nil
        do {
            _ = try await api.deleteAllData(accessToken: accessToken)
            isDeletingData = false
            deleteDataSuccess = true
        } catch {
            isDeletingData = false
            self.error = error.localizedDescription
        }
    }

    func clearDeleteSuccess() {
        deleteDataSuccess = false
    }

    func exportData() async {
        isExporting = true
        error = nil
        do {
            try await Task.sleep(nanoseconds: 500_000_000)
            isExporting = false
            exportSuccess = true
        } catch {
            isExporting = false
            self.error = error.localizedDescription
        }
    }
}
