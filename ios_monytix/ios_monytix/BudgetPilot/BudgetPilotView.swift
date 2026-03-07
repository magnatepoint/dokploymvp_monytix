//
//  BudgetPilotView.swift
//  ios_monytix
//
//  Mirrors APK BudgetPilotScreen: Smart Budget Engine, recommendations, commit plan, apply adjustment.
//

import SwiftUI

struct BudgetPilotView: View {
    @StateObject private var viewModel = BudgetPilotViewModel()
    @State private var showAddBudget = false
    @State private var showPlanPreview: BudgetRecommendation?
    @State private var dismissedSuggestion = false
    @State private var monthPickerExpanded = false
    var onOpenSpendSense: (() -> Void)?

    private var isZeroState: Bool {
        !hasRealData(variance: viewModel.variance, budgetState: viewModel.budgetState)
    }

    var body: some View {
        NavigationStack {
            ZStack {
                MonytixTheme.bg.ignoresSafeArea()
                if viewModel.isLoadingState && isZeroState {
                    VStack {
                        Spacer()
                        MonytixRingLoader(size: 44, lineWidth: 4)
                        Spacer()
                    }
                } else if isZeroState {
                    zeroStateCard
                } else {
                    dataStateContent
                }
            }
            .navigationTitle("BudgetPilot")
            .navigationBarTitleDisplayMode(.inline)
            .toolbarBackground(MonytixTheme.bg, for: .navigationBar)
            .toolbarColorScheme(.dark, for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .principal) {
                    VStack(spacing: 2) {
                        Text("BudgetPilot")
                            .font(.system(size: 18, weight: .semibold))
                            .foregroundStyle(MonytixTheme.text1)
                        Button {
                            monthPickerExpanded = true
                        } label: {
                            HStack(spacing: 4) {
                                Text(formatMonthLabel(viewModel.selectedMonth))
                                    .font(.system(size: 12, weight: .medium))
                                    .foregroundStyle(MonytixTheme.text2)
                                Image(systemName: "chevron.down")
                                    .font(.system(size: 10, weight: .medium))
                                    .foregroundStyle(MonytixTheme.text2)
                            }
                        }
                    }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        dismissedSuggestion = false
                        viewModel.refresh()
                    } label: {
                        Image(systemName: "arrow.clockwise")
                            .foregroundStyle(MonytixTheme.text1)
                    }
                }
            }
            .refreshable {
                viewModel.refresh()
            }
            .confirmationDialog("Select month", isPresented: $monthPickerExpanded, titleVisibility: .visible) {
                ForEach(budgetMonthOptions(), id: \.self) { month in
                    Button(formatMonthLabel(month)) {
                        viewModel.setMonth(month)
                        monthPickerExpanded = false
                    }
                }
                Button("Cancel", role: .cancel) {
                    monthPickerExpanded = false
                }
            }
            .sheet(isPresented: $showAddBudget) {
                addBudgetSheet
            }
            .sheet(item: $showPlanPreview) { plan in
                planPreviewSheet(plan: plan)
            }
            .alert("BudgetPilot", isPresented: Binding(
                get: { viewModel.errorMessage != nil },
                set: { if !$0 { viewModel.clearError() } }
            )) {
                Button("OK") { viewModel.clearError() }
            } message: {
                if let msg = viewModel.errorMessage {
                    Text(msg)
                }
            }
        }
    }

    private var zeroStateCard: some View {
        VStack {
            Spacer()
            VStack(spacing: MonytixSpace.md) {
                Text("Your financial engine is ready.")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundStyle(MonytixTheme.text1)
                    .multilineTextAlignment(.center)
                Text("Add data in SpendSense or set a budget plan to see your allocation here.")
                    .font(.system(size: 14, weight: .regular))
                    .foregroundStyle(MonytixTheme.text2)
                    .multilineTextAlignment(.center)
                Button {
                    onOpenSpendSense?()
                } label: {
                    Text("Open SpendSense")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(MonytixTheme.onAccent)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, MonytixSpace.sm)
                        .background(MonytixTheme.cyan1, in: RoundedRectangle(cornerRadius: MonytixShape.buttonRadius))
                }
                .padding(.top, MonytixSpace.sm)
            }
            .padding(MonytixSpace.lg)
            .background(MonytixTheme.cyan1.opacity(0.12), in: RoundedRectangle(cornerRadius: MonytixShape.cardRadius))
            .padding(.horizontal, MonytixSpace.lg)
            Spacer()
        }
    }

    private var dataStateContent: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 12) {
                autopilotStatusCard
                requiredAllocationCard
                if viewModel.autopilotSuggestion != nil && hasRealData(variance: viewModel.variance, budgetState: viewModel.budgetState) && !dismissedSuggestion {
                    optimizationSuggestionCard
                }
                adaptivePlanCard
                Text("Explore Standard Frameworks")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(MonytixTheme.text2)
                frameworkChipsRow
                Button("See all plans") {
                    showAddBudget = true
                }
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(MonytixTheme.cyan1)
                .frame(maxWidth: .infinity)
                Spacer(minLength: 24)
            }
            .padding(.horizontal, MonytixSpace.md)
            .padding(.top, MonytixSpace.sm)
        }
    }

    private var autopilotStatusCard: some View {
        let income = viewModel.variance?.incomeAmt ?? viewModel.budgetState?.incomeAmt ?? 0
        let actual = viewModel.budgetState?.actual
        let spendV = (viewModel.variance?.needsAmt ?? 0) + (viewModel.variance?.wantsAmt ?? 0)
        let spendA = actual.map { ($0.needsAmt ?? 0) + ($0.wantsAmt ?? 0) } ?? 0
        let spend = spendV + spendA
        let savings = viewModel.variance?.assetsAmt ?? actual?.savingsAmt ?? 0
        let savingsPct = income > 0 ? (savings / income) * 100 : 0
        let needsPct = (actual?.needsPct ?? 0) / 100
        let wantsPct = (actual?.wantsPct ?? 0) / 100
        let savingsPctActual = (actual?.savingsPct ?? 0) / 100
        let hasData = income >= 100 || spend >= 100 || savings >= 100
        let needsLabel = Int(actual?.needsPct ?? 0)
        let wantsLabel = Int(actual?.wantsPct ?? 0)
        return autopilotStatusCardContent(
            income: income,
            spend: spend,
            savings: savings,
            savingsPct: savingsPct,
            needsPct: needsPct,
            wantsPct: wantsPct,
            savingsPctActual: savingsPctActual,
            hasData: hasData,
            needsLabel: needsLabel,
            wantsLabel: wantsLabel,
            lastUpdatedAt: viewModel.lastUpdatedAt
        )
    }

    @ViewBuilder
    private func autopilotStatusCardContent(
        income: Double,
        spend: Double,
        savings: Double,
        savingsPct: Double,
        needsPct: Double,
        wantsPct: Double,
        savingsPctActual: Double,
        hasData: Bool,
        needsLabel: Int,
        wantsLabel: Int,
        lastUpdatedAt: String?
    ) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("Smart Budget Engine")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(MonytixTheme.text1)
                Spacer()
                Text("Status: \(viewModel.autopilotStatus)")
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(MonytixTheme.text2)
            }
            Text("Your allocation is dynamically adjusted based on goals and spending.")
                .font(.system(size: 13, weight: .regular))
                .foregroundStyle(MonytixTheme.text2)
            if hasData {
                Text("Income: \(formatCurrency(income)) • Spend: \(formatCurrency(spend)) • Savings: \(formatCurrency(savings)) (\(Int(savingsPct))%)")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(MonytixTheme.text1)
                allocationBar(needsPct: needsPct, wantsPct: wantsPct, savingsPct: savingsPctActual)
                Text("Needs: \(needsLabel)% | Wants: \(wantsLabel)% | Savings: \(Int(savingsPct))%")
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(MonytixTheme.text2)
            } else {
                Text("Status: Waiting for data")
                    .font(.system(size: 13, weight: .regular))
                    .foregroundStyle(MonytixTheme.text2)
            }
            if let last = lastUpdatedAt, !last.isEmpty {
                Text("Last recalculated: \(formatLastUpdated(last))")
                    .font(.system(size: 11, weight: .regular))
                    .foregroundStyle(MonytixTheme.text3)
            }
        }
        .padding(MonytixSpace.md)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(MonytixTheme.cyan1.opacity(0.12), in: RoundedRectangle(cornerRadius: MonytixShape.cardRadius))
    }

    private var requiredAllocationCard: some View {
        let goalRequired = viewModel.goalRequiredSavingsRate
        let actualRate = viewModel.budgetState?.actual.flatMap { a in
            let inc = viewModel.budgetState?.incomeAmt ?? 0
            return inc > 0 ? (a.savingsAmt ?? 0) / inc * 100 : 0
        } ?? 0
        let gap = actualRate - goalRequired
        let hasGoals = !viewModel.goalsProgressList.isEmpty || goalRequired > 0

        return VStack(alignment: .leading, spacing: 6) {
            if hasGoals {
                Text("Required Savings Rate: \(Int(goalRequired))%")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(MonytixTheme.text1)
                Text("You're currently saving: \(Int(actualRate))%")
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(MonytixTheme.text2)
                Text("Gap: \(gap >= 0 ? "+" : "")\(Int(gap))%")
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(gap >= 0 ? MonytixTheme.success : MonytixTheme.warn)
                if !viewModel.goalsProgressList.isEmpty {
                    ForEach(viewModel.goalsProgressList) { g in
                        Text("\(g.goalName): \(formatCurrency(g.monthlyRequired))/month")
                            .font(.system(size: 12, weight: .regular))
                            .foregroundStyle(MonytixTheme.text2)
                    }
                }
            } else {
                Text("Set goals in GoalTracker to see required allocation")
                    .font(.system(size: 13, weight: .regular))
                    .foregroundStyle(MonytixTheme.text3)
            }
        }
        .padding(MonytixSpace.md)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(MonytixTheme.surface, in: RoundedRectangle(cornerRadius: MonytixShape.cardRadius))
    }

    private var optimizationSuggestionCard: some View {
        guard let suggestion = viewModel.autopilotSuggestion else { return AnyView(EmptyView()) }
        let income = viewModel.variance?.incomeAmt ?? 0
        let amount = income > 0 ? max(100, income * suggestion.pct / 100) : 0
        let currentWants = income > 0 ? (viewModel.variance?.wantsAmt ?? 0) / income * 100 : 0
        let currentSavings = income > 0 ? (viewModel.variance?.assetsAmt ?? 0) / income * 100 : 0
        let targetWants = max(0, currentWants - suggestion.pct)
        let targetSavings = currentSavings + suggestion.pct

        return AnyView(
            VStack(alignment: .leading, spacing: 8) {
                Text("Suggested Adjustment")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(MonytixTheme.text1)
                Text("To stay on track, reduce \(suggestion.shiftFrom.capitalized) by \(formatCurrency(amount)) and increase \(suggestion.shiftTo.capitalized) by \(formatCurrency(amount)).")
                    .font(.system(size: 13, weight: .regular))
                    .foregroundStyle(MonytixTheme.text2)
                if income > 0 && suggestion.shiftFrom == "wants" && suggestion.shiftTo == "savings" {
                    let needsPct = (viewModel.variance?.needsAmt ?? 0) / income
                    allocationBar(needsPct: needsPct, wantsPct: currentWants / 100, savingsPct: currentSavings / 100)
                    Text("Target")
                        .font(.system(size: 11, weight: .medium))
                        .foregroundStyle(MonytixTheme.text3)
                    allocationBar(needsPct: (viewModel.variance?.needsAmt ?? 0) / income, wantsPct: targetWants / 100, savingsPct: targetSavings / 100)
                }
                HStack(spacing: 8) {
                    Button {
                        viewModel.applyAdjustment(shiftFrom: suggestion.shiftFrom, shiftTo: suggestion.shiftTo, pct: suggestion.pct)
                    } label: {
                        if viewModel.isApplyingAdjustment {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: MonytixTheme.onAccent))
                        }
                        Text(viewModel.isApplyingAdjustment ? "Applying…" : "Apply")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundStyle(MonytixTheme.onAccent)
                    }
                    .disabled(viewModel.isApplyingAdjustment)
                    .padding(.horizontal, MonytixSpace.md)
                    .padding(.vertical, MonytixSpace.sm)
                    .background(MonytixTheme.cyan1, in: RoundedRectangle(cornerRadius: MonytixShape.smallRadius))
                    Button("Dismiss") {
                        dismissedSuggestion = true
                    }
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(MonytixTheme.text2)
                }
            }
            .padding(MonytixSpace.md)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(MonytixTheme.warn.opacity(0.15), in: RoundedRectangle(cornerRadius: MonytixShape.cardRadius))
        )
    }

    private var adaptivePlanCard: some View {
        let committed = viewModel.committedBudget
        let state = viewModel.budgetState
        let variance = viewModel.variance
        let hasData = hasRealData(variance: variance, budgetState: state)
        let actual = state?.actual
        let (needsPct, wantsPct, savingsPct): (Int, Int, Int) = {
            if let c = committed {
                return (
                    Int((c.allocNeedsPct ?? 0) * 100),
                    Int((c.allocWantsPct ?? 0) * 100),
                    Int((c.allocAssetsPct ?? 0) * 100)
                )
            }
            if let a = actual, hasData {
                return (Int(a.needsPct ?? 0), Int(a.wantsPct ?? 0), Int(a.savingsPct ?? 0))
            }
            let top = viewModel.recommendations.max(by: { ($0.score ?? 0) < ($1.score ?? 0) }) ?? defaultBudgetPlans().first!
            return (
                Int((top.needsBudgetPct ?? 0.55) * 100),
                Int((top.wantsBudgetPct ?? 0.20) * 100),
                Int((top.savingsBudgetPct ?? 0.25) * 100)
            )
        }()
        let monthLabel = formatMonthLabel(viewModel.selectedMonth)

        return VStack(alignment: .leading, spacing: 6) {
            Text("Your Adaptive Plan (Generated for \(monthLabel))")
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(MonytixTheme.text1)
            Text("Needs: \(needsPct)% | Wants: \(wantsPct)% | Savings: \(savingsPct)%")
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(MonytixTheme.text2)
            if !viewModel.adaptivePlanReason.isEmpty {
                Text("Reason:")
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(MonytixTheme.text2)
                ForEach(viewModel.adaptivePlanReason, id: \.self) { reason in
                    Text("• \(reason)")
                        .font(.system(size: 12, weight: .regular))
                        .foregroundStyle(MonytixTheme.text2)
                }
            } else if !hasData {
                Text("Based on your goals and spending, we recommend the allocation above.")
                    .font(.system(size: 12, weight: .regular))
                    .foregroundStyle(MonytixTheme.text2)
            }
            if let last = viewModel.lastUpdatedAt, !last.isEmpty {
                Text("Last updated: \(formatLastUpdated(last))")
                    .font(.system(size: 11, weight: .regular))
                    .foregroundStyle(MonytixTheme.text3)
            }
        }
        .padding(MonytixSpace.md)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(MonytixTheme.surface, in: RoundedRectangle(cornerRadius: MonytixShape.cardRadius))
    }

    private var frameworkChipsRow: some View {
        let plans = Array(defaultBudgetPlans().prefix(3))
        return HStack(spacing: 8) {
            ForEach(plans) { plan in
                frameworkChip(plan: plan, isCommitted: viewModel.committedBudget?.planCode == plan.planCode) {
                    showPlanPreview = plan
                }
            }
        }
    }

    private func frameworkChip(plan: BudgetRecommendation, isCommitted: Bool, onClick: @escaping () -> Void) -> some View {
        let needsPct = Int((plan.needsBudgetPct ?? 0.55) * 100)
        let wantsPct = Int((plan.wantsBudgetPct ?? 0.20) * 100)
        let savingsPct = Int((plan.savingsBudgetPct ?? 0.25) * 100)
        return Button(action: onClick) {
            VStack(alignment: .leading, spacing: 4) {
                Text(plan.name ?? plan.planCode ?? "")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(MonytixTheme.text1)
                Text("\(needsPct)/\(wantsPct)/\(savingsPct)")
                    .font(.system(size: 11, weight: .regular))
                    .foregroundStyle(MonytixTheme.text2)
            }
            .padding(12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                isCommitted ? MonytixTheme.cyan1.opacity(0.2) : MonytixTheme.surface,
                in: RoundedRectangle(cornerRadius: MonytixShape.smallRadius)
            )
        }
        .buttonStyle(.plain)
    }

    private var addBudgetSheet: some View {
        NavigationStack {
            List {
                ForEach(viewModel.recommendations.isEmpty ? defaultBudgetPlans() : viewModel.recommendations) { rec in
                    let isCommitted = viewModel.committedBudget?.planCode == rec.planCode
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(rec.name ?? rec.planCode ?? "")
                                .font(.system(size: 16, weight: .bold))
                                .foregroundStyle(MonytixTheme.text1)
                            if let reason = rec.recommendationReason, !reason.isEmpty {
                                Text(reason)
                                    .font(.system(size: 13, weight: .regular))
                                    .foregroundStyle(MonytixTheme.text2)
                            }
                        }
                        Spacer()
                        if isCommitted {
                            Text("Committed")
                                .font(.system(size: 12, weight: .medium))
                                .foregroundStyle(MonytixTheme.success)
                        } else if !viewModel.isCommitting {
                            Button("Commit") {
                                guard let code = rec.planCode else { return }
                                viewModel.commitBudget(planCode: code)
                                showAddBudget = false
                            }
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundStyle(MonytixTheme.onAccent)
                            .padding(.horizontal, MonytixSpace.sm)
                            .padding(.vertical, 6)
                            .background(MonytixTheme.cyan1, in: RoundedRectangle(cornerRadius: MonytixShape.smallRadius))
                        }
                    }
                    .padding(.vertical, 4)
                }
            }
            .scrollContentBackground(.hidden)
            .background(MonytixTheme.bg)
            .navigationTitle("Add Budget")
            .navigationBarTitleDisplayMode(.inline)
            .toolbarBackground(MonytixTheme.bg, for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        showAddBudget = false
                    }
                    .foregroundStyle(MonytixTheme.cyan1)
                }
            }
        }
    }

    private func planPreviewSheet(plan: BudgetRecommendation) -> some View {
        let sampleIncome = 50_000.0
        let needsPct = Int((plan.needsBudgetPct ?? 0.55) * 100)
        let wantsPct = Int((plan.wantsBudgetPct ?? 0.20) * 100)
        let savingsPct = Int((plan.savingsBudgetPct ?? 0.25) * 100)
        let needsAmt = sampleIncome * Double(needsPct) / 100
        let wantsAmt = sampleIncome * Double(wantsPct) / 100
        let savingsAmt = sampleIncome * Double(savingsPct) / 100

        return VStack(spacing: MonytixSpace.md) {
            Text("What this would look like with \(formatCurrency(sampleIncome)) income")
                .font(.system(size: 16, weight: .bold))
                .foregroundStyle(MonytixTheme.text1)
                .multilineTextAlignment(.center)
            Text(plan.name ?? plan.planCode ?? "")
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(MonytixTheme.text2)
            VStack(alignment: .leading, spacing: 4) {
                Text("Needs: \(formatCurrency(needsAmt)) (\(needsPct)%)")
                Text("Wants: \(formatCurrency(wantsAmt)) (\(wantsPct)%)")
                Text("Savings: \(formatCurrency(savingsAmt)) (\(savingsPct)%)")
            }
            .font(.system(size: 13, weight: .regular))
            .foregroundStyle(MonytixTheme.text1)
            Button {
                if let code = plan.planCode {
                    viewModel.commitBudget(planCode: code)
                }
                showPlanPreview = nil
            } label: {
                if viewModel.isCommitting && viewModel.committingPlanCode == plan.planCode {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: MonytixTheme.onAccent))
                    Text("Activating…")
                } else {
                    Text("Start this plan")
                }
            }
            .font(.system(size: 16, weight: .semibold))
            .foregroundStyle(MonytixTheme.onAccent)
            .frame(maxWidth: .infinity)
            .padding(.vertical, MonytixSpace.sm)
            .background(MonytixTheme.cyan1, in: RoundedRectangle(cornerRadius: MonytixShape.buttonRadius))
            .disabled(viewModel.isCommitting)
            Button("Cancel") {
                showPlanPreview = nil
            }
            .font(.system(size: 14, weight: .medium))
            .foregroundStyle(MonytixTheme.text2)
        }
        .padding(MonytixSpace.lg)
        .frame(maxWidth: .infinity)
        .background(MonytixTheme.surface)
        .presentationDetents([.medium])
    }
}

// MARK: - Helpers

private func hasRealData(variance: BudgetVariance?, budgetState: BudgetStateResponse?) -> Bool {
    let income = variance?.incomeAmt ?? budgetState?.incomeAmt ?? 0
    let spendFromVariance: Double? = variance.map { ($0.needsAmt ?? 0) + ($0.wantsAmt ?? 0) }
    let spendFromActual: Double? = budgetState?.actual.map {
        ($0.needsAmt ?? 0) + ($0.wantsAmt ?? 0) + ($0.savingsAmt ?? 0)
    }
    let spend = spendFromVariance ?? spendFromActual ?? 0
    return income >= 100 || spend >= 100
}

private func budgetMonthOptions() -> [String] {
    let cal = Calendar.current
    return (0..<24).compactMap { i in
        cal.date(byAdding: .month, value: -i, to: Date()).map { d in
            let formatter = DateFormatter()
            formatter.dateFormat = "yyyy-MM"
            return formatter.string(from: d)
        }
    }.compactMap { $0 }
}

private func formatMonthLabel(_ month: String) -> String {
    guard !month.isEmpty else { return "" }
    let parts = month.split(separator: "-")
    guard parts.count >= 2,
          let m = Int(parts[1]),
          (1...12).contains(m) else { return month }
    let months = ["", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"]
    let y = String(parts[0].prefix(4))
    return "\(months[m]) \(y)"
}

private func formatCurrency(_ amount: Double) -> String {
    let absAmount = abs(amount)
    let formatter = NumberFormatter()
    formatter.numberStyle = .decimal
    formatter.maximumFractionDigits = 0
    let formatted = formatter.string(from: NSNumber(value: absAmount)) ?? "\(Int(absAmount))"
    return amount < 0 ? "-₹\(formatted)" : "₹\(formatted)"
}

private func formatLastUpdated(_ iso: String?) -> String {
    guard let iso = iso, !iso.isEmpty else { return "" }
    guard let date = ISO8601DateFormatter().date(from: iso) else { return "" }
    let secs = Date().timeIntervalSince(date)
    if secs < 60 { return "\(Int(secs)) seconds ago" }
    if secs < 3600 { return "\(Int(secs / 60))m ago" }
    return "\(Int(secs / 3600))h ago"
}

private func allocationBar(needsPct: Double, wantsPct: Double, savingsPct: Double) -> some View {
    let total = max(0.001, needsPct + wantsPct + savingsPct)
    let n = (needsPct / total).clamped(to: 0...1)
    let w = (wantsPct / total).clamped(to: 0...1)
    let s = (savingsPct / total).clamped(to: 0...1)
    return GeometryReader { geo in
        let width = geo.size.width
        HStack(spacing: 0) {
            if n > 0.005 {
                Rectangle()
                    .fill(MonytixTheme.Chart.blue)
                    .frame(width: width * n, height: 8)
            }
            if w > 0.005 {
                Rectangle()
                    .fill(MonytixTheme.Chart.orange)
                    .frame(width: width * w, height: 8)
            }
            if s > 0.005 {
                Rectangle()
                    .fill(MonytixTheme.Chart.green)
                    .frame(width: width * s, height: 8)
            }
        }
        .frame(height: 8)
        .clipShape(RoundedRectangle(cornerRadius: 10))
    }
    .frame(height: 8)
    .background(MonytixTheme.surface2, in: RoundedRectangle(cornerRadius: 10))
}

extension Comparable {
    fileprivate func clamped(to range: ClosedRange<Self>) -> Self {
        min(max(self, range.lowerBound), range.upperBound)
    }
}

private func defaultBudgetPlans() -> [BudgetRecommendation] {
    var plans: [BudgetRecommendation] = []
    plans.append(BudgetRecommendation(planCode: "LEAN_BASICS", name: "Lean Basics", description: nil, needsBudgetPct: 0.55, wantsBudgetPct: 0.20, savingsBudgetPct: 0.25, score: 0.5, recommendationReason: "Best for low-income stability"))
    plans.append(BudgetRecommendation(planCode: "BAL_50_30_20", name: "Balanced 50/30/20", description: nil, needsBudgetPct: 0.50, wantsBudgetPct: 0.30, savingsBudgetPct: 0.20, score: 0.5, recommendationReason: "Best for steady income"))
    plans.append(BudgetRecommendation(planCode: "EMERGENCY_FIRST", name: "Emergency Priority", description: nil, needsBudgetPct: 0.50, wantsBudgetPct: 0.20, savingsBudgetPct: 0.30, score: 0.5, recommendationReason: "Boost savings"))
    plans.append(BudgetRecommendation(planCode: "DEBT_FIRST", name: "Debt First", description: nil, needsBudgetPct: 0.55, wantsBudgetPct: 0.20, savingsBudgetPct: 0.25, score: 0.5, recommendationReason: "Aggressive debt payoff"))
    plans.append(BudgetRecommendation(planCode: "GOAL_PRIORITY", name: "Top 3 Goals", description: nil, needsBudgetPct: 0.50, wantsBudgetPct: 0.20, savingsBudgetPct: 0.30, score: 0.5, recommendationReason: "Assets tilt to goals"))
    return plans
}

#Preview {
    BudgetPilotView()
}
