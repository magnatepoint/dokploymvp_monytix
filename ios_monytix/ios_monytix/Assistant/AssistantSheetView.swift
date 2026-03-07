//
//  AssistantSheetView.swift
//  ios_monytix
//
//  Ask MONYTIX: prompt chips; calls POST /v1/assistant/ask with loading state.
//

import SwiftUI

private let promptChips = [
    "Can I afford this?",
    "Will I run short?",
    "Why did spending increase?",
    "How do I save faster?",
    "What should I reduce this week?"
]

private func mockAnswer(for prompt: String) -> String {
    if prompt.contains("afford") {
        return "Based on your current cash flow and goals, you're on track. Check the Future tab for a 14-day projection. For big purchases, we recommend keeping 3 months of expenses as buffer."
    }
    if prompt.contains("run short") {
        return "Your forecast shows a dip around days 8–10. Consider delaying non-essential spend until after payday, or top up your Emergency goal. See the Financial Future tab for details."
    }
    return "Your finances look on track. Use the Future tab for projections and Goals for targets. If you have a specific question, try one of the prompts above."
}

struct AssistantSheetView: View {
    @Binding var isPresented: Bool
    @State private var answer: String?
    @State private var isLoading = false

    var body: some View {
        NavigationStack {
            ZStack {
                MonytixTheme.bg.ignoresSafeArea()
                ScrollView {
                    VStack(alignment: .leading, spacing: MonytixSpace.md) {
                        Text("Quick prompts or type your question.")
                            .font(.system(size: 13))
                            .foregroundStyle(MonytixTheme.text2)
                        Text("Suggestions")
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundStyle(MonytixTheme.cyan1)
                        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: MonytixSpace.sm) {
                            ForEach(promptChips, id: \.self) { chip in
                                Button {
                                    if isLoading { return }
                                    answer = nil
                                    isLoading = true
                                    Task {
                                        guard let token = await AuthManager.shared.getIdToken() else {
                                            answer = mockAnswer(for: chip)
                                            isLoading = false
                                            return
                                        }
                                        switch await BackendApi.postAssistantAsk(accessToken: token, prompt: chip) {
                                        case .success(let r):
                                            answer = r.answer
                                        case .failure:
                                            answer = mockAnswer(for: chip)
                                        }
                                        isLoading = false
                                    }
                                } label: {
                                    Text(chip)
                                        .font(.system(size: 12))
                                        .foregroundStyle(MonytixTheme.text1)
                                        .multilineTextAlignment(.leading)
                                        .frame(maxWidth: .infinity, alignment: .leading)
                                        .padding(MonytixSpace.sm)
                                }
                                .buttonStyle(.plain)
                                .background(MonytixTheme.cyan1.opacity(0.12))
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                            }
                        }
                        if isLoading {
                            HStack(spacing: 8) {
                                ProgressView()
                                    .scaleEffect(0.9)
                                Text("Asking MONYTIX…")
                                    .font(.system(size: 13))
                                    .foregroundStyle(MonytixTheme.text2)
                            }
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, MonytixSpace.sm)
                        }
                        if let answer = answer {
                            Text("MONYTIX")
                                .font(.system(size: 11, weight: .semibold))
                                .foregroundStyle(MonytixTheme.cyan1)
                            Text(answer)
                                .font(.system(size: 14))
                                .foregroundStyle(MonytixTheme.text1)
                                .frame(maxWidth: .infinity, alignment: .leading)
                        }
                    }
                    .padding(MonytixSpace.lg)
                }
            }
            .navigationTitle("Ask MONYTIX")
            .navigationBarTitleDisplayMode(.inline)
            .toolbarBackground(MonytixTheme.bg, for: .navigationBar)
            .toolbarColorScheme(.dark, for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Done") { isPresented = false }
                        .foregroundStyle(MonytixTheme.cyan1)
                }
            }
        }
    }
}
