//
//  ProfileView.swift
//  ios
//
//  Profile - matches APK ProfileScreen
//

import SwiftUI

struct ProfileView: View {
    @Environment(AuthViewModel.self) private var authViewModel
    @State private var viewModel = ProfileViewModel()
    @State private var showLogoutConfirm = false
    @State private var showDeleteAccount = false
    @State private var showDeleteDataConfirm = false
    @State private var showDeleteDataSuccess = false

    private var profileUserId: String {
        let id = authViewModel.user?.id ?? viewModel.userId ?? ""
        return id.isEmpty ? "—" : String(id.prefix(8)) + "..."
    }

    var body: some View {
        NavigationStack {
            PremiumGradientBackground {
                ScrollView {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Manage your account and preferences")
                            .font(MonytixTypography.bodyMedium)
                            .foregroundColor(MonytixColors.textSecondary)
                            .padding(.bottom, 8)

                        ProfileSectionCard(title: "Account", icon: "👤") {
                            ProfileInfoRow(label: "Email", value: authViewModel.user?.email ?? viewModel.userEmail ?? "—")
                            ProfileInfoRow(label: "User ID", value: profileUserId)
                        }

                        ProfileSectionCard(title: "Preferences", icon: "⚙️") {
                            ProfilePrefRow(icon: "bell.fill", title: "Notifications", subtitle: "Manage notification preferences",
                                onClick: { AnalyticsHelper.logEvent("notification_toggled") })
                            ProfilePrefRow(icon: "gearshape.fill", title: "Currency", subtitle: "INR (Indian Rupee)", onClick: { AnalyticsHelper.logEvent("currency_changed") })
                            ProfilePrefRow(icon: "gearshape.fill", title: "Theme", subtitle: "Dark", onClick: {})
                        }

                        ProfileSectionCard(title: "Data Management", icon: "💾") {
                            Button {
                                showDeleteDataConfirm = true
                            } label: {
                                HStack {
                                    Text("🗑️")
                                        .font(MonytixTypography.titleLarge)
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text("Delete All Data")
                                            .font(MonytixTypography.bodyLarge)
                                            .fontWeight(.semibold)
                                            .foregroundColor(MonytixColors.error)
                                        Text("Permanently delete all your data")
                                            .font(MonytixTypography.bodySmall)
                                            .foregroundColor(MonytixColors.textSecondary)
                                    }
                                    Spacer()
                                    if viewModel.isDeletingData {
                                        ProgressView()
                                            .tint(MonytixColors.textPrimary)
                                    }
                                }
                                .padding(.vertical, 8)
                            }
                            .buttonStyle(.plain)
                            .disabled(viewModel.isDeletingData)
                        }

                        ProfileSectionCard(title: "About", icon: "ℹ️") {
                            ProfileInfoRow(label: "App Version", value: "1.0.0")
                    ProfileInfoRow(label: "Build", value: "1")
                        }

                        Text("Account")
                            .font(MonytixTypography.labelLarge)
                            .foregroundColor(MonytixColors.textSecondary)
                            .padding(.top, 16)

                        ProfileMenuItem(icon: "rectangle.portrait.and.arrow.right", title: "Sign Out", onClick: { showLogoutConfirm = true })

                        ProfileMenuItem(icon: "square.and.arrow.down", title: "Export Data", onClick: {
                            AnalyticsHelper.logEvent("export_data")
                            Task {
                                await viewModel.exportData()
                            }
                        })

                        ProfileMenuItem(icon: "person.slash", title: "Deactivate", onClick: {
                            AnalyticsHelper.logEvent("deactivate_account")
                            viewModel.error = "Contact support to deactivate"
                        })

                        Text("Danger Zone")
                            .font(MonytixTypography.labelLarge)
                            .foregroundColor(MonytixColors.textSecondary)
                            .padding(.top, 16)

                        ProfileMenuItem(icon: "trash", title: "Delete Account", onClick: { showDeleteAccount = true }, isDestructive: true)

                        if let err = viewModel.error {
                            Text(err)
                                .font(MonytixTypography.bodySmall)
                                .foregroundColor(MonytixColors.error)
                                .padding(8)
                        }
                    }
                    .padding(16)
                }
            }
            .navigationTitle("Profile")
            .navigationBarTitleDisplayMode(.inline)
            .toolbarColorScheme(.dark, for: .navigationBar)
            .toolbarBackground(MonytixColors.surfaceDark, for: .navigationBar)
            .task {
                viewModel.loadUser(from: (authViewModel.user?.email, authViewModel.user?.id))
            }
            .onAppear {
                AnalyticsHelper.logScreenView(screenName: "profile")
            }
            .alert("Sign Out", isPresented: $showLogoutConfirm) {
                Button("Cancel", role: .cancel) { showLogoutConfirm = false }
                Button("Sign Out", role: .destructive) {
                    AnalyticsHelper.logEvent("logout")
                    Task { await authViewModel.signOut() }
                    showLogoutConfirm = false
                }
            } message: {
                Text("Are you sure you want to sign out?")
            }
            .alert("Delete Account", isPresented: $showDeleteAccount) {
                Button("Cancel", role: .cancel) { showDeleteAccount = false }
                Button("Delete", role: .destructive) {
                    AnalyticsHelper.logEvent("delete_account")
                    Task { await authViewModel.signOut() }
                    showDeleteAccount = false
                }
            } message: {
                Text("This will permanently delete your account. This action cannot be undone.")
            }
            .alert("Delete All Data", isPresented: $showDeleteDataConfirm) {
                Button("Cancel", role: .cancel) { showDeleteDataConfirm = false }
                Button("Delete", role: .destructive) {
                    AnalyticsHelper.logEvent("delete_all_data")
                    Task {
                        if let token = await authViewModel.getAccessToken() {
                            await viewModel.deleteAllData(accessToken: token)
                            if viewModel.deleteDataSuccess {
                                showDeleteDataSuccess = true
                            }
                        }
                        showDeleteDataConfirm = false
                    }
                }
            } message: {
                Text("This will permanently delete all your transaction data, goals, budgets, and moments. This action cannot be undone.")
            }
            .alert("Data Deleted", isPresented: $showDeleteDataSuccess) {
                Button("OK") {
                    showDeleteDataSuccess = false
                    viewModel.clearDeleteSuccess()
                }
            } message: {
                Text("All your data has been successfully deleted.")
            }
        }
    }
}

private struct ProfileSectionCard<Content: View>: View {
    let title: String
    let icon: String
    @ViewBuilder let content: () -> Content

    var body: some View {
        GlassCardView {
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Text(icon)
                        .font(MonytixTypography.titleMedium)
                    Text(title)
                        .font(MonytixTypography.titleMedium)
                        .fontWeight(.bold)
                        .foregroundColor(MonytixColors.textPrimary)
                }
                content()
            }
        }
    }
}

private struct ProfileInfoRow: View {
    let label: String
    let value: String

    var body: some View {
        HStack {
            Text(label)
                .font(MonytixTypography.bodyMedium)
                .foregroundColor(MonytixColors.textSecondary)
            Spacer()
            Text(value)
                .font(MonytixTypography.bodyMedium)
                .foregroundColor(MonytixColors.textPrimary)
        }
        .padding(.vertical, 4)
    }
}

private struct ProfilePrefRow: View {
    let icon: String
    let title: String
    let subtitle: String
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            HStack {
                Image(systemName: icon)
                    .foregroundColor(MonytixColors.textSecondary)
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(MonytixTypography.bodyMedium)
                        .foregroundColor(MonytixColors.textPrimary)
                    Text(subtitle)
                        .font(MonytixTypography.bodySmall)
                        .foregroundColor(MonytixColors.textSecondary)
                }
                Spacer()
            }
            .padding(.vertical, 8)
        }
        .buttonStyle(.plain)
    }
}

private struct ProfileMenuItem: View {
    let icon: String
    let title: String
    let onClick: () -> Void
    var isDestructive = false

    var body: some View {
        Button(action: onClick) {
            HStack {
                Image(systemName: icon)
                    .foregroundColor(isDestructive ? MonytixColors.error : MonytixColors.textPrimary)
                Text(title)
                    .font(MonytixTypography.bodyLarge)
                    .foregroundColor(isDestructive ? MonytixColors.error : MonytixColors.textPrimary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(16)
        }
        .buttonStyle(.plain)
    }
}

#Preview {
    ProfileView()
        .environment(AuthViewModel())
}
