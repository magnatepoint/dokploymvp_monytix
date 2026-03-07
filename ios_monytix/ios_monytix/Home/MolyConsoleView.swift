//
//  MolyConsoleView.swift
//  ios_monytix
//
//  Home as command center: single scroll with Health → Risk → CTA → Goals strip → Forecast strip → Insights → Recent. Mirrors APK HomeScreen.
//

import SwiftUI
import UniformTypeIdentifiers

struct MolyConsoleView: View {
    @StateObject private var viewModel = MolyConsoleViewModel()
    var onNavigateToFuture: () -> Void = {}
    var onNavigateToGoals: () -> Void = {}
    var onNavigateToSpendSense: () -> Void = {}
    var requestUploadOnHome: Binding<Bool> = .constant(false)
    @State private var showAddTransaction = false
    @State private var showAssistant = false
    @State private var showFileImporter = false
    @State private var showUploadConfirmSheet = false
    @State private var pendingUploadData: Data?
    @State private var pendingUploadFilename: String?
    @State private var uploadMessage: String?
    @State private var isUploading = false

    var body: some View {
        NavigationStack {
            ZStack {
                MonytixTheme.bg.ignoresSafeArea()

                VStack(spacing: 0) {
                    welcomeBanner
                    if viewModel.isLoading && viewModel.kpis == nil {
                        commandCenterSkeleton
                    } else {
                        commandCenterContent
                    }
                }
            }
            .navigationTitle("MolyConsole")
            .navigationBarTitleDisplayMode(.inline)
            .toolbarBackground(MonytixTheme.bg, for: .navigationBar)
            .toolbarColorScheme(.dark, for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button {
                        AuthManager.shared.signOut()
                    } label: {
                        Image(systemName: "rectangle.portrait.and.arrow.right")
                    }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    HStack(spacing: 8) {
                        Button {
                            showFileImporter = true
                        } label: {
                            Image(systemName: "square.and.arrow.up")
                        }
                        .disabled(isUploading)
                        Button {
                            showAddTransaction = true
                        } label: {
                            Image(systemName: "plus.circle")
                        }
                        Button {
                            viewModel.refresh()
                        } label: {
                            Image(systemName: "arrow.clockwise")
                                .rotationEffect(.degrees(viewModel.isLoading ? 360 : 0))
                                .animation(viewModel.isLoading ? .linear(duration: 1).repeatForever(autoreverses: false) : .default, value: viewModel.isLoading)
                        }
                        Button {
                            showAssistant = true
                        } label: {
                            Image(systemName: "lightbulb")
                        }
                    }
                }
            }
            .sheet(isPresented: $showAssistant) {
                AssistantSheetView(isPresented: $showAssistant)
            }
            .sheet(isPresented: $showAddTransaction) {
                AddTransactionSheet(isPresented: $showAddTransaction) {
                    viewModel.refresh()
                }
            }
            .fileImporter(
                isPresented: $showFileImporter,
                allowedContentTypes: [.pdf, .commaSeparatedText, .plainText, .data],
                allowsMultipleSelection: false
            ) { result in
                handleFileImport(result: result)
            }
            .sheet(isPresented: $showUploadConfirmSheet) {
                if let data = pendingUploadData, let name = pendingUploadFilename {
                    UploadConfirmSheet(
                        isPresented: $showUploadConfirmSheet,
                        filename: name,
                        isUploading: isUploading,
                        onUpload: { password in
                            performUpload(fileData: data, filename: name, password: password)
                        },
                        onCancel: {
                            pendingUploadData = nil
                            pendingUploadFilename = nil
                            showUploadConfirmSheet = false
                        }
                    )
                }
            }
            .overlay(alignment: .top) {
                if let msg = uploadMessage {
                    Text(msg)
                        .font(.caption)
                        .foregroundStyle(MonytixTheme.text1)
                        .padding(.horizontal, MonytixSpace.md)
                        .padding(.vertical, MonytixSpace.sm)
                        .background(MonytixTheme.surface2)
                        .clipShape(RoundedRectangle(cornerRadius: MonytixShape.smallRadius))
                        .padding(.top, MonytixSpace.md)
                        .onAppear {
                            DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) {
                                uploadMessage = nil
                            }
                        }
                }
            }
            .refreshable {
                await viewModel.refreshAsync()
            }
            .onChange(of: requestUploadOnHome.wrappedValue) { _, new in
                if new {
                    showFileImporter = true
                    requestUploadOnHome.wrappedValue = false
                }
            }
            .onAppear {
                viewModel.loadDashboard()
                viewModel.connectRealtime()
                // Defer health check to reduce concurrent connections (and nw_connection log noise)
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.6) {
                    viewModel.checkBackend()
                }
            }
            .onDisappear {
                viewModel.disconnectRealtime()
            }
        }
    }

    private func handleFileImport(result: Result<[URL], Error>) {
        guard case .success(let urls) = result, let url = urls.first else {
            uploadMessage = "No file selected"
            return
        }
        Task { @MainActor in
            guard url.startAccessingSecurityScopedResource() else {
                uploadMessage = "Could not access file"
                return
            }
            defer { url.stopAccessingSecurityScopedResource() }
            guard let data = try? Data(contentsOf: url) else {
                uploadMessage = "Could not read file"
                return
            }
            pendingUploadData = data
            pendingUploadFilename = url.lastPathComponent
            showUploadConfirmSheet = true
        }
    }

    private func performUpload(fileData: Data, filename: String, password: String?) {
        isUploading = true
        uploadMessage = nil
        Task { @MainActor in
            defer {
                isUploading = false
                pendingUploadData = nil
                pendingUploadFilename = nil
                showUploadConfirmSheet = false
            }
            guard let token = await AuthManager.shared.getIdToken() else {
                uploadMessage = "Please sign in again"
                return
            }
            let uploadResult = await BackendApi.uploadStatement(accessToken: token, fileData: fileData, filename: filename, password: password)
            switch uploadResult {
            case .success(let batch):
                viewModel.refresh()
                uploadMessage = "Upload started • \(batch.status)"
            case .failure(let e):
                uploadMessage = e.localizedDescription
            }
        }
    }

    private var welcomeBanner: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("AI Financial Command Center")
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(MonytixTheme.text1)
            let name = (viewModel.userEmail?.split(separator: "@").first).map(String.init) ?? "User"
            Text("Welcome back, \(name) • Your financial cockpit")
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(MonytixTheme.text2)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(MonytixSpace.md)
        .background(MonytixTheme.surface.opacity(0.6))
    }

    // MARK: - Command center (single scroll)

    @ViewBuilder
    private var commandCenterContent: some View {
        ScrollView {
            if viewModel.hasNoTransactionData() {
                emptyStateCommandCenter
                    .padding(MonytixSpace.lg)
                    .padding(.top, MonytixSpace.xl)
            } else {
                LazyVStack(alignment: .leading, spacing: MonytixSpace.md) {
                    HealthCard(health: viewModel.healthState())
                        .enter(delay: 0)
                    RiskCard(risk: viewModel.riskState())
                        .enter(delay: 0.05)
                    CtaCard(
                        nextAction: viewModel.nextAction(),
                        onUpload: { showFileImporter = true },
                        onNavigateToFuture: onNavigateToFuture,
                        onNavigateToGoals: onNavigateToGoals,
                        onNavigateToSpendSense: onNavigateToSpendSense
                    )
                    .enter(delay: 0.1)
                    if !viewModel.transformGoals().isEmpty {
                        GoalPulseRow(goals: viewModel.transformGoals(), onSeeAll: onNavigateToGoals)
                            .enter(delay: 0.15)
                    }
                    ForecastStrip(onSeeFuture: onNavigateToFuture)
                        .enter(delay: 0.2)
                    let topInsights = viewModel.topInsightsForCommandCenter()
                    if !topInsights.isEmpty {
                        Text("Insights")
                            .font(.system(size: 17, weight: .semibold))
                            .foregroundStyle(MonytixTheme.text1)
                            .enter(delay: 0.22)
                        ForEach(Array(topInsights.enumerated()), id: \.element.id) { index, insight in
                            InsightCardCompact(title: insight.title, message: insight.message) {
                                onNavigateToSpendSense()
                            }
                            .enter(delay: 0.25 + Double(index) * 0.03)
                        }
                    }
                    if !viewModel.recentTransactions.isEmpty {
                        Text("Recent Transactions")
                            .font(.system(size: 17, weight: .semibold))
                            .foregroundStyle(MonytixTheme.text1)
                            .enter(delay: 0.32)
                        ForEach(Array(viewModel.recentTransactions.prefix(5).enumerated()), id: \.element.txnId) { i, t in
                            recentTxnRowCommandCenter(t)
                                .enter(delay: 0.35 + Double(i) * 0.02)
                        }
                    }
                }
                .padding(.horizontal, MonytixSpace.lg)
                .padding(.vertical, MonytixSpace.md)
                .padding(.bottom, MonytixSpace.xl)
            }
            if let err = viewModel.errorMessage {
                Text(err)
                    .font(.caption)
                    .foregroundStyle(MonytixTheme.danger)
                    .padding(.horizontal, MonytixSpace.lg)
            }
        }
    }

    private var emptyStateCommandCenter: some View {
        VStack(spacing: MonytixSpace.lg) {
            Text("Your command center is ready")
                .font(.system(size: 18, weight: .semibold))
                .foregroundStyle(MonytixTheme.text1)
            Text("Connect an account or upload a statement to see your financial picture.")
                .font(.system(size: 14))
                .foregroundStyle(MonytixTheme.text2)
                .multilineTextAlignment(.center)
            Button("Upload statement") { showFileImporter = true }
                .buttonStyle(.borderedProminent)
                .tint(MonytixTheme.cyan1)
            Button("Add transaction") { showAddTransaction = true }
                .buttonStyle(.bordered)
                .tint(MonytixTheme.cyan1)
        }
        .frame(maxWidth: .infinity)
    }

    private var commandCenterSkeleton: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: MonytixSpace.md) {
                SkeletonCard()
                SkeletonCard()
                SkeletonCard()
                RoundedRectangle(cornerRadius: MonytixShape.smallRadius)
                    .fill(MonytixTheme.stroke.opacity(0.4))
                    .frame(height: 52)
                    .shimmer()
                RoundedRectangle(cornerRadius: MonytixShape.smallRadius)
                    .fill(MonytixTheme.stroke.opacity(0.4))
                    .frame(height: 44)
                    .shimmer()
                RoundedRectangle(cornerRadius: 6)
                    .fill(MonytixTheme.stroke.opacity(0.4))
                    .frame(width: 80, height: 18)
                    .shimmer()
                SkeletonCard()
                SkeletonCard()
                SkeletonCard()
            }
            .padding(.horizontal, MonytixSpace.lg)
            .padding(.vertical, MonytixSpace.md)
        }
    }

    private func recentTxnRowCommandCenter(_ t: TransactionRecordResponse) -> some View {
        let isDebit = t.direction.lowercased() == "debit"
        return HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(t.merchant ?? t.category ?? "Transaction")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(MonytixTheme.text1)
                Text(t.txnDate)
                    .font(.system(size: 11, weight: .medium))
                    .foregroundStyle(MonytixTheme.text2)
            }
            Spacer()
            Text("\(isDebit ? "-" : "+")\(formatCurrency(abs(t.amount)))")
                .font(.system(size: 14, weight: .bold))
                .foregroundStyle(isDebit ? MonytixTheme.danger : MonytixTheme.success)
        }
        .padding(MonytixSpace.sm)
        .background(MonytixTheme.surface)
        .clipShape(RoundedRectangle(cornerRadius: MonytixShape.smallRadius))
    }
}

// MARK: - Command center cards

private struct HealthCard: View {
    let health: HealthState
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Financial health")
                .font(.system(size: 12, weight: .medium))
                .foregroundStyle(MonytixTheme.text2)
            HStack {
                Text("\(health.score)")
                    .font(.system(size: 28, weight: .bold))
                    .foregroundStyle(MonytixTheme.cyan1)
                Spacer()
                Text(health.trend == "up" ? "↗" : (health.trend == "down" ? "↘" : "→"))
                    .font(.system(size: 16, weight: .medium))
                    .foregroundStyle(MonytixTheme.text2)
            }
            Text(health.subtext)
                .font(.system(size: 12, weight: .regular))
                .foregroundStyle(MonytixTheme.text2)
        }
        .padding(MonytixSpace.lg)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(MonytixTheme.surface)
        .clipShape(RoundedRectangle(cornerRadius: MonytixShape.cardRadius))
    }
}

private struct RiskCard: View {
    let risk: RiskState
    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(risk.label)
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(MonytixTheme.text1)
            Text(risk.reason)
                .font(.system(size: 13, weight: .regular))
                .foregroundStyle(MonytixTheme.text2)
        }
        .padding(MonytixSpace.md)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(MonytixTheme.surface2.opacity(0.6))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}

private struct CtaCard: View {
    let nextAction: NextAction
    let onUpload: () -> Void
    let onNavigateToFuture: () -> Void
    let onNavigateToGoals: () -> Void
    let onNavigateToSpendSense: () -> Void
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(nextAction.label)
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(MonytixTheme.text1)
            Button(ctaButtonLabel) {
                switch nextAction.type {
                case "upload": onUpload()
                case "insights": onNavigateToSpendSense()
                case "goal": onNavigateToGoals()
                case "forecast": onNavigateToFuture()
                default: onUpload()
                }
            }
            .buttonStyle(.borderedProminent)
            .tint(MonytixTheme.cyan1)
        }
        .padding(MonytixSpace.md)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(MonytixTheme.cyan1.opacity(0.15))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(MonytixTheme.cyan1.opacity(0.4), lineWidth: 1)
        )
    }
    private var ctaButtonLabel: String {
        switch nextAction.type {
        case "upload": return "Upload statement"
        case "insights": return "Review insights"
        case "goal": return "View goals"
        case "forecast": return "See forecast"
        default: return nextAction.label
        }
    }
}

private struct GoalPulseRow: View {
    let goals: [ConsoleGoal]
    let onSeeAll: () -> Void
    var body: some View {
        let totalToGo = goals.map { max(0, $0.targetAmount - $0.savedAmount) }.reduce(0, +)
        Button(action: onSeeAll) {
            HStack {
                Text("\(goals.count) goals · \(formatCurrency(totalToGo)) to go this month")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(MonytixTheme.text1)
                Spacer()
                Text("See all")
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(MonytixTheme.cyan1)
            }
            .padding(MonytixSpace.md)
        }
        .buttonStyle(.plain)
        .frame(maxWidth: .infinity)
        .background(MonytixTheme.surface)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

private struct ForecastStrip: View {
    let onSeeFuture: () -> Void
    var body: some View {
        Button(action: onSeeFuture) {
            HStack {
                Text("Next 30 days")
                    .font(.system(size: 15, weight: .medium))
                    .foregroundStyle(MonytixTheme.text1)
                Spacer()
                Text("See forecast →")
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(MonytixTheme.cyan1)
            }
            .padding(MonytixSpace.md)
        }
        .buttonStyle(.plain)
        .frame(maxWidth: .infinity)
        .background(MonytixTheme.surface)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

private struct InsightCardCompact: View {
    let title: String
    let message: String
    let onClick: () -> Void
    var body: some View {
        Button(action: onClick) {
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(MonytixTheme.cyan1)
                Text(message)
                    .font(.system(size: 12, weight: .regular))
                    .foregroundStyle(MonytixTheme.text2)
                    .lineLimit(2)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(MonytixSpace.sm)
        }
        .buttonStyle(.plain)
        .background(MonytixTheme.surface)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

// MARK: - Helpers

private func formatCurrency(_ amount: Double) -> String {
    let absAmount = abs(amount)
    let formatter = NumberFormatter()
    formatter.numberStyle = .decimal
    formatter.maximumFractionDigits = 0
    let formatted = formatter.string(from: NSNumber(value: absAmount)) ?? "\(Int(absAmount))"
    return amount < 0 ? "-₹\(formatted)" : "₹\(formatted)"
}

#Preview {
    MolyConsoleView()
}
