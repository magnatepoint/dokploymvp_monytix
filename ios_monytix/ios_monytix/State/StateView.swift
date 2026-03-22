//
//  StateView.swift
//  ios_monytix
//
//  Container: Budget | Goals tabs (State pillar).
//

import SwiftUI

struct StateView: View {
    @State private var selectedTab = 0
    /// When Budget asks to open SpendSense, switch main tab to Memory.
    var onNavigateToMemory: (() -> Void)? = nil

    var body: some View {
        VStack(spacing: 0) {
            Picker("", selection: $selectedTab) {
                Text("Budget").tag(0)
                Text("Goals").tag(1)
            }
            .pickerStyle(.segmented)
            .padding(.horizontal, MonytixSpace.md)
            .padding(.vertical, MonytixSpace.sm)
            .background(MonytixTheme.surface)

            Group {
                if selectedTab == 0 {
                    BudgetPilotView(onOpenSpendSense: onNavigateToMemory)
                } else {
                    GoalTrackerView()
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .background(MonytixTheme.bg.ignoresSafeArea())
    }
}

#Preview {
    StateView()
}
