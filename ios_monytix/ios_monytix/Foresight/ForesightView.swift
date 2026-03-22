//
//  ForesightView.swift
//  ios_monytix
//
//  Container: Nudges | Habits | Future tabs (Foresight pillar).
//

import SwiftUI

struct ForesightView: View {
    @State private var selectedTab = 0
    /// When Future tab requests upload, main tab can switch to Home and show upload.
    var onUploadStatementFromFuture: () -> Void = {}

    var body: some View {
        VStack(spacing: 0) {
            Picker("", selection: $selectedTab) {
                Text("Nudges").tag(0)
                Text("Habits").tag(1)
                Text("Future").tag(2)
            }
            .pickerStyle(.segmented)
            .padding(.horizontal, MonytixSpace.md)
            .padding(.vertical, MonytixSpace.sm)
            .background(MonytixTheme.surface)

            Group {
                if selectedTab == 0 {
                    MoneyMomentsView(embedTab: .nudges)
                } else if selectedTab == 1 {
                    MoneyMomentsView(embedTab: .habits)
                } else {
                    FutureView(onUploadStatement: onUploadStatementFromFuture)
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .background(MonytixTheme.bg.ignoresSafeArea())
    }
}

#Preview {
    ForesightView()
}
