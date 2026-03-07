//
//  FutureViewModel.swift
//  ios_monytix
//
//  Financial Future (forecast) screen. Loads from GET /v1/forecast; falls back to mock on failure.
//

import Foundation
import SwiftUI

struct FutureRecommendation: Identifiable {
    let id: String
    let title: String
    let body: String
    let ctaLabel: String?
}

@MainActor
final class FutureViewModel: ObservableObject {
    @Published private(set) var confidenceLabel = "Based on last 90 days"
    @Published private(set) var projectionPoints: [(CGFloat, CGFloat)] = []
    @Published private(set) var riskStripLabel: String?
    @Published private(set) var riskStripSeverity = "neutral"
    @Published private(set) var savingsOpportunity: String?
    @Published private(set) var recommendations: [FutureRecommendation] = []
    @Published private(set) var isLoading = false
    @Published private(set) var hasData = false

    init() {
        loadForecast()
    }

    func loadForecast() {
        isLoading = true
        Task {
            guard let token = await AuthManager.shared.getIdToken() else {
                loadMockData()
                return
            }
            switch await BackendApi.getForecast(accessToken: token) {
            case .success(let r):
                let points = r.projectionPoints.compactMap { list -> (CGFloat, CGFloat)? in
                    guard list.count >= 2 else { return nil }
                    return (CGFloat(list[0]), CGFloat(list[1]))
                }
                confidenceLabel = r.confidenceLabel.isEmpty ? "Based on this month's cash flow" : r.confidenceLabel
                projectionPoints = points
                riskStripLabel = r.riskStripLabel
                riskStripSeverity = r.riskStripSeverity ?? "neutral"
                savingsOpportunity = r.savingsOpportunity
                recommendations = r.recommendations.enumerated().map { i, rec in
                    FutureRecommendation(id: "\(i)", title: rec.title, body: rec.body, ctaLabel: nil)
                }
                hasData = !points.isEmpty
            case .failure:
                loadMockData()
            }
            isLoading = false
        }
    }

    private func loadMockData() {
        isLoading = true
        projectionPoints = (0...13).map { i in
            let x = CGFloat(i) / 13
            let y = 0.4 + 0.5 * (1 - Double(x)) + Double(i % 3) * 0.05
            return (x, CGFloat(min(1, max(0.2, y))))
        }
        riskStripLabel = "Low cash risk: Days 8–10"
        riskStripSeverity = "warning"
        savingsOpportunity = "You could save ₹3,200 by trimming dining 10%."
        recommendations = [
            FutureRecommendation(
                id: "1",
                title: "Delay non-essential spend until payday",
                body: "Your projected balance dips mid-month. Consider moving discretionary spend to after payday.",
                ctaLabel: nil
            ),
            FutureRecommendation(
                id: "2",
                title: "Top up Emergency goal by ₹2,000",
                body: "You're ahead of pace this month. Putting ₹2,000 into your Emergency goal keeps you on track.",
                ctaLabel: nil
            )
        ]
        hasData = true
        isLoading = false
    }

    func refresh() {
        loadForecast()
    }
}
